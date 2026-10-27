package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

// Creative/debug tool: jumps to the default grid's main arrival terminal and back, until the digitizing laser exists
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

        MinecraftServer server = player.getServer();
        ServerWorld targetWorld;
        Vec3d targetPos;
        if (ModDimensions.isGrid(world)) {
            targetWorld = server.getOverworld();
            targetPos = player.getWorldSpawnPos(targetWorld, targetWorld.getSpawnPos()).toBottomCenterPos();
        } else {
            targetWorld = server.getWorld(ModDimensions.DEFAULT_GRID_WORLD_KEY);
            if (targetWorld == null || !(targetWorld.getChunkManager().getChunkGenerator() instanceof GridChunkGenerator grid)) {
                return ActionResult.FAIL;
            }
            Site arrival = grid.layout().mainArrival();
            targetPos = new BlockPos(arrival.chunkX() * 16 + 8, GridLayout.FLOOR_Y + 1, arrival.chunkZ() * 16 + 8).toBottomCenterPos();
        }

        player.teleportTo(new TeleportTarget(targetWorld, targetPos, Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP));
        player.getItemCooldownManager().set(player.getStackInHand(hand), COOLDOWN_TICKS);
        return ActionResult.SUCCESS;
    }
}
