package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

// Placeholder way in and out of the Digital Frontier until there's a proper portal
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
        if (world.getRegistryKey() == ModDimensions.DIGITAL_FRONTIER_WORLD_KEY) {
            targetWorld = server.getOverworld();
            targetPos = player.getWorldSpawnPos(targetWorld, targetWorld.getSpawnPos()).toBottomCenterPos();
        } else {
            targetWorld = server.getWorld(ModDimensions.DIGITAL_FRONTIER_WORLD_KEY);
            if (targetWorld == null) {
                return ActionResult.FAIL;
            }
            targetPos = targetWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING, player.getBlockPos()).toBottomCenterPos();
        }

        player.teleportTo(new TeleportTarget(targetWorld, targetPos, Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP));
        player.getItemCooldownManager().set(player.getStackInHand(hand), COOLDOWN_TICKS);
        return ActionResult.SUCCESS;
    }
}
