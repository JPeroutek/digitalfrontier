package com.partatoes.digitalfrontier.derez;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.custom.BoolEntity;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.RecruiterEntity;
import com.partatoes.digitalfrontier.entity.custom.SystemCoreEntity;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/**
 * The client's side of derezzing (see {@link Derez}): which style to show, which bodies derez, and shedding their
 * cubes as they die. The body itself is handled by {@code LivingEntityRendererDerezMixin}.
 */
public final class DerezClient {
    // The part of a body's cubes in its glow; the rest are its dark shell
    private static final float GLOW_SHARE = 0.7f;
    private static final int SHELL = 0x14191E;
    private static final int DEFAULT_GLOW = 0xBFF6FF;

    // Bodies whose statue has been raised, by entity id, with the game time it was: one statue per death
    private static final Map<Integer, Long> RAISED = new HashMap<>();

    private static DerezStyle style = DerezStyle.SWEEP;

    private DerezClient() {
    }

    public static void register() {
        ParticleGroupRegistry.register(DerezCubeGroup.TYPE, DerezCubeGroup::new);
        ClientTickEvents.END_LEVEL_TICK.register(DerezClient::tick);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "derez_skins"),
                (ResourceManagerReloadListener) manager -> DerezStatue.clearSkins());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("derez")
                        .executes(command -> {
                            command.getSource().sendFeedback(Component.translatable("commands.digitalfrontier.derez.style", name(style)));
                            return 1;
                        })
                        .then(ClientCommands.literal("style")
                                .then(ClientCommands.argument("style", StringArgumentType.word())
                                        .suggests((command, builder) -> {
                                            Arrays.stream(DerezStyle.values()).forEach(value -> builder.suggest(name(value)));
                                            return builder.buildFuture();
                                        })
                                        .executes(command -> {
                                            String wanted = StringArgumentType.getString(command, "style");
                                            for (DerezStyle value : DerezStyle.values()) {
                                                if (name(value).equals(wanted)) {
                                                    style = value;
                                                    command.getSource().sendFeedback(Component.translatable(
                                                            "commands.digitalfrontier.derez.style", name(value)));
                                                    return 1;
                                                }
                                            }
                                            command.getSource().sendError(Component.translatable("commands.digitalfrontier.derez.unknown", wanted));
                                            return 0;
                                        })))));
    }

    private static String name(DerezStyle value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    /** The style bodies derez in, chosen with {@code /derez style}. */
    public static DerezStyle style() {
        return style;
    }

    /** Whether this body derezzes when it dies. */
    public static boolean derezzes(LivingEntity entity) {
        boolean ownMob = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(DigitalFrontier.MOD_ID);
        return Derez.derezzes(ownMob, ModDimensions.isGrid(entity.level()),
                entity instanceof EnderDragon || entity instanceof WitherBoss);
    }

    /**
     * Whether a dying body's statue should be raised now: true once per death, while the style builds statues. Called
     * as its render state is extracted, so the statue is raised from the frame about to be drawn.
     */
    public static boolean raiseOnce(LivingEntity body) {
        if (!style.statue()) {
            return false;
        }
        return RAISED.putIfAbsent(body.getId(), body.level().getGameTime()) == null;
    }

    // Each tick, every dying body sheds its share of cubes
    private static void tick(ClientLevel level) {
        long now = level.getGameTime();
        RAISED.values().removeIf(raised -> now - raised > Derez.TICKS * 3 || raised > now);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.deathTime > 0 && living.deathTime <= Derez.TICKS && derezzes(living)) {
                shed(level, living);
            }
        }
    }

    private static void shed(ClientLevel level, LivingEntity body) {
        AABB box = body.getBoundingBox();
        int count = style.cubesThisTick(body.deathTime, Derez.budget(body.getBbWidth(), body.getBbHeight()));
        if (count == 0) {
            return;
        }
        float[] band = style.band(body.deathTime);
        int glow = glow(body);
        RandomSource random = level.getRandom();
        double centerX = (box.minX + box.maxX) / 2;
        double centerZ = (box.minZ + box.maxZ) / 2;
        // Bigger bodies come apart into bigger cubes
        float scale = Math.clamp(body.getBbHeight() / 1.8f, 0.6f, 2.5f);
        for (int i = 0; i < count; i++) {
            double x = box.minX + random.nextDouble() * box.getXsize();
            double y = box.minY + (band[0] + random.nextDouble() * (band[1] - band[0])) * box.getYsize();
            double z = box.minZ + random.nextDouble() * box.getZsize();
            double kick = style.kick() * (0.5 + random.nextDouble());
            double dx = x - centerX;
            double dz = z - centerZ;
            double length = Math.max(0.05, Math.sqrt(dx * dx + dz * dz));
            float size = (0.05f + random.nextFloat() * 0.07f) * scale;
            int color = random.nextFloat() < GLOW_SHARE ? glow : SHELL;
            Minecraft.getInstance().particleEngine.add(new DerezCubeParticle(level, x, y, z,
                    dx / length * kick, kick * 0.6 * random.nextDouble(), dz / length * kick, size, color));
        }
    }

    /** The color a body's circuits glow, which most of its cubes take. */
    public static int glow(LivingEntity body) {
        return switch (body) {
            case BoolEntity bool -> switch (bool.signal()) {
                case YES -> 0xFFF2A0;
                case NO -> 0xFF3030;
                default -> 0x4FE3FF;
            };
            case LieutenantEntity lieutenant -> lieutenant.redeemed() ? 0xE8FFFF : 0xFF3030;
            case CorruptedProgramEntity corrupted -> 0xFF8A1E;
            case EnforcerEntity enforcer -> 0xFF3030;
            case RecruiterEntity recruiter -> 0xFF3030;
            case SystemCoreEntity core -> 0xFF3030;
            case ProgramEntity program -> 0x4FE3FF;
            default -> DEFAULT_GLOW;
        };
    }
}
