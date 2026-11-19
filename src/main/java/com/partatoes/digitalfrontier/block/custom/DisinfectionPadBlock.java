package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A Disinfection Pad: a corrupted program that steps onto one is cured, back to a clean program with no decay and a
 * fresh sentiment (see {@link ProgramEntity}). The cure releases what the corruption held, the
 * {@code digitalfrontier:entities/restored_program} loot table, and counts as a {@code program_restored} event for
 * lieutenant triggers. Nothing else is affected: not players, programs, enforcers or recruiters.
 */
public class DisinfectionPadBlock extends Block {
    public static final ResourceKey<LootTable> CURE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "entities/restored_program"));

    public DisinfectionPadBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (world instanceof ServerLevel serverWorld && entity instanceof CorruptedProgramEntity corrupted && corrupted.isAlive()) {
            cure(serverWorld, corrupted, pos);
        }
        super.stepOn(world, pos, state, entity);
    }

    private static void cure(ServerLevel world, CorruptedProgramEntity corrupted, BlockPos pad) {
        ProgramEntity program = corrupted.convertTo(ModEntities.PROGRAM, ConversionParams.single(corrupted, false, false),
                ProgramEntity::setPersistenceRequired);
        if (program == null) {
            return;
        }
        world.sendParticles(ParticleTypes.END_ROD, program.getX(), program.getY() + 1, program.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        world.playSound(null, pad, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 1.6f);
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.THIS_ENTITY, program)
                .withParameter(LootContextParams.ORIGIN, program.position())
                .create(LootContextParamSets.SELECTOR);
        world.getServer().reloadableRegistries().getLootTable(CURE_LOOT)
                .getRandomItems(params, stack -> program.spawnAtLocation(world, stack));
        StrongholdManager.fire(world, new GridEvent.ProgramRestored(pad));
    }
}
