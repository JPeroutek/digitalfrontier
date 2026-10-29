package com.partatoes.digitalfrontier.grid.stronghold;

import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.GridMemory;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Turns what players do into {@link GridEvent}s for the strongholds' respawn triggers. */
public final class StrongholdEvents {
    private StrongholdEvents() {
    }

    public static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (GridArrival.generator(destination) != null) {
                // Arriving through a laser records the terminal only after this, so a first visit still looks new
                boolean newPlayer = player.getAttachedOrCreate(GridMemory.TYPE).discovered(destination.getRegistryKey().getValue()).isEmpty();
                StrongholdManager.fire(destination, new GridEvent.GridEntered(newPlayer));
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
                return;
            }
            if (entity instanceof ServerPlayerEntity) {
                StrongholdManager.fire(world, new GridEvent.PlayerDerezzed(entity.getBlockPos()));
            } else if (entity instanceof ProgramEntity && source.getAttacker() instanceof PlayerEntity) {
                StrongholdManager.fire(world, new GridEvent.ProgramDerezzed(entity.getBlockPos()));
            }
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) {
                StrongholdManager.fire(serverWorld, new GridEvent.BlockBroken(state, pos));
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(world instanceof ServerWorld serverWorld) || stack.isEmpty()) {
                return ActionResult.PASS;
            }
            StrongholdManager.Fired fired = StrongholdManager.fire(serverWorld, new GridEvent.ItemUsed(stack.copy(), player.getBlockPos()));
            if (fired.consume()) {
                stack.decrementUnlessCreative(1, player);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    /** From {@code ItemStackMixin}: crafting in a grid counts there; crafting in the real world counts in every grid. */
    public static void onItemCrafted(ItemStack stack, World world, PlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) {
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
    public static void onBlockPlaced(World world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            StrongholdManager.fire(serverWorld, new GridEvent.BlockPlaced(world.getBlockState(pos), pos));
        }
    }
}
