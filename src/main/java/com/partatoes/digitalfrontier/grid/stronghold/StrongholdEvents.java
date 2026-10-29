package com.partatoes.digitalfrontier.grid.stronghold;

import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.GridMemory;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Turns what players do into {@link GridEvent}s for the strongholds' respawn triggers. */
public final class StrongholdEvents {
    private StrongholdEvents() {
    }

    public static void register() {
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            if (GridArrival.generator(destination) != null) {
                // Arriving through a laser records the terminal only after this, so a first visit still looks new
                boolean newPlayer = player.getAttachedOrCreate(GridMemory.TYPE).discovered(destination.dimension().identifier()).isEmpty();
                StrongholdManager.fire(destination, new GridEvent.GridEntered(newPlayer));
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel world)) {
                return;
            }
            if (entity instanceof ServerPlayer) {
                StrongholdManager.fire(world, new GridEvent.PlayerDerezzed(entity.blockPosition()));
            } else if (entity instanceof ProgramEntity && source.getEntity() instanceof Player) {
                StrongholdManager.fire(world, new GridEvent.ProgramDerezzed(entity.blockPosition()));
            }
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel serverWorld) {
                StrongholdManager.fire(serverWorld, new GridEvent.BlockBroken(state, pos));
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!(world instanceof ServerLevel serverWorld) || stack.isEmpty()) {
                return InteractionResult.PASS;
            }
            StrongholdManager.Fired fired = StrongholdManager.fire(serverWorld, new GridEvent.ItemUsed(stack.copy(), player.blockPosition()));
            if (fired.consume()) {
                stack.consume(1, player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    /** From {@code ItemStackMixin}: crafting in a grid counts there; crafting in the real world counts in every grid. */
    public static void onItemCrafted(ItemStack stack, Level world, Player player) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        GridEvent event = new GridEvent.ItemCrafted(stack.copy());
        if (GridArrival.generator(serverWorld) != null) {
            StrongholdManager.fire(serverWorld, event);
        } else {
            StrongholdManager.fireEverywhere(serverWorld.getServer(), event);
        }
    }

    /** From {@code BlockItemMixin}, after a block was placed successfully. */
    public static void onBlockPlaced(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld) {
            StrongholdManager.fire(serverWorld, new GridEvent.BlockPlaced(world.getBlockState(pos), pos));
        }
    }
}
