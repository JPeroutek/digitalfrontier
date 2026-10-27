package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

// Creative/debug tool: jumps to the default grid's main arrival terminal and back, skipping the Digitizing Laser
public class DimensionTeleporterItem extends Item {
    private static final int COOLDOWN_TICKS = 20;

    public DimensionTeleporterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        if (ModDimensions.isGrid(world)) {
            ServerWorld overworld = player.getServer().getOverworld();
            Vec3d targetPos = player.getWorldSpawnPos(overworld, overworld.getSpawnPos()).toBottomCenterPos();
            player.teleportTo(new TeleportTarget(overworld, targetPos, Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP));
        } else if (!GridArrival.teleport(player, new TerminalRef(ModDimensions.DEFAULT_GRID_ID, GridLayout.MAIN_ARRIVAL_ID))) {
            return ActionResult.FAIL;
        }

        player.getItemCooldownManager().set(player.getStackInHand(hand), COOLDOWN_TICKS);
        return ActionResult.SUCCESS;
    }
}
