package com.partatoes.digitalfrontier.grid.regime;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.RecruiterEntity;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.core.CoreManager;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.Optional;

/**
 * The regime as a society: its rules ({@link RegimeRules}, reloaded with datapacks), how big it is and wants to be,
 * conscripting programs, and how deaths in a grid move alignments and sentiments.
 */
public final class Regime implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RULES_FILE = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid/regime.json");
    // How recently a dying mob must have hit a program for its killer to count as the program's defender
    private static final int DEFENDED_WITHIN = 100;
    private static final double BRUTALITY_RADIUS = 16;

    private static volatile RegimeRules rules = RegimeRules.DEFAULT;

    public static RegimeRules rules() {
        return rules;
    }

    public static void register() {
        // Made on the server thread as each grid loads, so the F3 screen can read it from the client thread
        ServerLevelEvents.LOAD.register((server, world) -> {
            if (GridArrival.generator(world) != null) {
                RegimeState.of(world);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel world && GridArrival.generator(world) != null) {
                onDeath(world, entity, source.getEntity());
            }
        });
    }

    @Nullable
    private static GridChunkGenerator generator(Level world) {
        return world instanceof ServerLevel serverWorld ? GridArrival.generator(serverWorld) : null;
    }

    /** A player's alignment in the grid they're in; 0 outside grids. */
    public static float alignment(Entity player) {
        return generator(player.level()) != null ? RegimeState.of((ServerLevel) player.level()).alignment(player.getUUID()) : 0;
    }

    public static void shiftAlignment(Player player, float delta) {
        if (generator(player.level()) != null && delta != 0) {
            RegimeState.of((ServerLevel) player.level()).shiftAlignment(player.getUUID(), delta);
        }
    }

    /** How many lasting members the regime wants: none once every stronghold has fallen. */
    public static int ideal(ServerLevel grid) {
        GridChunkGenerator generator = GridArrival.generator(grid);
        if (generator == null) {
            return 0;
        }
        int standing = (int) StrongholdManager.strongholds(grid).stream().filter(view -> view.stronghold().alive()).count();
        int ideal = Conscription.ideal(standing, rules.recruiting().idealPerStronghold(), generator.layout().settings().regime());
        // With its core derezzed, it wants half as many
        return CoreManager.defeated(grid) ? ideal / 2 : ideal;
    }

    public static int population(ServerLevel grid) {
        return RegimeState.of(grid).roster();
    }

    /** One recruitment attempt on a program, at a method's base chance; true if it joined. */
    public static boolean recruit(ServerLevel grid, ProgramEntity program, double base) {
        if (program.job() != null) {
            return false;
        }
        double chance = Conscription.chance(base, program.sentiment(), population(grid), ideal(grid));
        return program.getRandom().nextDouble() < chance && conscript(grid, program);
    }

    /** The program joins the regime: it becomes an enforcer, for good, and goes on the roster. */
    public static boolean conscript(ServerLevel grid, ProgramEntity program) {
        EnforcerEntity enforcer = program.convertTo(ModEntities.ENFORCER, ConversionParams.single(program, false, false), converted -> {
            converted.setConscripted(true);
            converted.setPersistenceRequired();
        });
        if (enforcer == null) {
            return false;
        }
        RegimeState.of(grid).changeRoster(1);
        grid.sendParticles(ParticleTypes.ANGRY_VILLAGER, enforcer.getX(), enforcer.getEyeY(), enforcer.getZ(), 4, 0.3, 0.3, 0.3, 0);
        grid.playSound(null, enforcer.getX(), enforcer.getY(), enforcer.getZ(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 0.8f, 1.4f);
        return true;
    }

    private static void onDeath(ServerLevel world, LivingEntity entity, @Nullable Entity killer) {
        RegimeRules.Alignment alignment = rules.alignment();
        Player player = killer instanceof Player p ? p : null;
        switch (entity) {
            case EnforcerEntity enforcer -> {
                if (enforcer.conscripted()) {
                    RegimeState.of(world).changeRoster(-1);
                }
                if (player != null) {
                    shiftAlignment(player, alignment.killEnforcer());
                }
            }
            case RecruiterEntity recruiter -> {
                if (recruiter.rostered()) {
                    RegimeState.of(world).changeRoster(-1);
                }
                if (player != null) {
                    shiftAlignment(player, alignment.killRecruiter());
                }
            }
            case LieutenantEntity lieutenant -> {
                if (player != null) {
                    shiftAlignment(player, alignment.defeatLieutenant());
                }
            }
            case ProgramEntity program -> {
                if (player != null) {
                    switch (program.stance()) {
                        case REBEL, RESISTANCE -> shiftAlignment(player, alignment.killRebel());
                        case LOYAL -> shiftAlignment(player, alignment.killLoyal());
                        default -> {
                        }
                    }
                }
                // Programs that see the regime kill one of their own turn against it
                if (killer instanceof EnforcerEntity) {
                    world.getEntitiesOfClass(ProgramEntity.class, entity.getBoundingBox().inflate(BRUTALITY_RADIUS), witness -> witness != entity)
                            .forEach(witness -> witness.shiftSentiment(rules.sentiment().brutality()));
                }
            }
            default -> {
            }
        }

        // Whoever kills what was attacking a program has defended it
        if (entity.getLastHurtMob() instanceof ProgramEntity victim && victim.isAlive()
                && entity.tickCount - entity.getLastHurtMobTimestamp() < DEFENDED_WITHIN) {
            if (player != null) {
                victim.actedOnByPlayer(player, rules.sentiment().defended());
            } else if (killer instanceof EnforcerEntity && entity instanceof CorruptedProgramEntity) {
                victim.shiftSentiment(rules.sentiment().protection());
            }
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid_regime");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(RULES_FILE);
        RegimeRules loaded = RegimeRules.DEFAULT;
        if (resource.isPresent()) {
            try (Reader reader = resource.get().openAsReader()) {
                loaded = RegimeRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Invalid grid regime rules: {}", error))
                        .orElse(RegimeRules.DEFAULT);
            } catch (Exception e) {
                DigitalFrontier.LOGGER.error("Failed to read grid regime rules", e);
            }
        }
        rules = loaded;
    }
}
