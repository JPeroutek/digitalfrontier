package com.partatoes.digitalfrontier.grid.exit;

import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * The Portal's toll. A grid's Portal starts sealed: someone has to step into it holding an Escape Key, which is used
 * up, and from then on it's open to everyone, for good. The Portal Keeper beside it sells keys.
 */
public final class PortalGate {
    private PortalGate() {
    }

    /** Whether this player may pass the Portal at this position now, paying its toll if they can and it's due. */
    public static boolean pass(ServerPlayer player, ServerLevel grid, BlockPos pos) {
        String portal = portalAt(grid, pos);
        if (portal == null) {
            // A Portal block placed by hand, outside any Portal site: nothing to pay
            return true;
        }
        PortalState state = PortalState.of(grid);
        if (state.isUnlocked(portal)) {
            return true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ModItems.ESCAPE_KEY)) {
                held.consume(1, player);
                state.setUnlocked(portal, true);
                grid.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.2f);
                Component opened = Component.translatable("message.digitalfrontier.portal_opened", player.getDisplayName())
                        .withStyle(ChatFormatting.AQUA);
                grid.players().forEach(other -> other.sendSystemMessage(opened));
                return true;
            }
        }
        player.sendOverlayMessage(Component.translatable("message.digitalfrontier.portal_sealed").withStyle(ChatFormatting.RED));
        return false;
    }

    /** The Portal site whose column this is, or null if there's none. */
    @Nullable
    public static String portalAt(ServerLevel grid, BlockPos pos) {
        GridChunkGenerator generator = GridArrival.generator(grid);
        if (generator == null) {
            return null;
        }
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return generator.layout().sites().stream()
                .filter(site -> site.type() == Site.Type.PORTAL && site.distanceTo(chunkX, chunkZ) <= 2)
                .min(Comparator.comparingDouble(site -> site.distanceTo(chunkX, chunkZ)))
                .map(Site::id)
                .orElse(null);
    }
}
