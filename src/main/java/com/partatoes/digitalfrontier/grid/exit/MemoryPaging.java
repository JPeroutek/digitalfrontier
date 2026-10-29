package com.partatoes.digitalfrontier.grid.exit;

import com.mojang.serialization.Codec;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.GridMemory;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jetbrains.annotations.Nullable;

/**
 * Memory Paging and Core Dump. A player derezzed in a grid while carrying one respawns at the terminal they last
 * arrived at, instead of in the real world; one item is used up. With Memory Paging their other items drop as
 * usual; a Core Dump also keeps their inventory and experience, like the keepInventory game rule.
 *
 * <p>The respawn position itself is swapped in by {@code ServerPlayerEntityMixin}, so the player respawns straight
 * into the grid. Keeping the inventory is done by {@code PlayerEntityMixin} (no drops) and {@link #copyKeptInventory}.
 */
public final class MemoryPaging {
    /** Set at death, used to pick the respawn position. Saved, so quitting on the death screen doesn't lose it. */
    private static final AttachmentType<TerminalRef> PENDING = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "memory_paging_pending"),
            builder -> builder.persistent(TerminalRef.CODEC).copyOnDeath());
    /** Set at death when a Core Dump was used. Only read from the dead player, so it isn't copied to the new one. */
    private static final AttachmentType<Boolean> KEEP_INVENTORY = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "core_dump_keep_inventory"),
            builder -> builder.persistent(Codec.BOOL));

    private MemoryPaging() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                pageOut(player);
            }
            return true;
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                copyKeptInventory(oldPlayer, newPlayer);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                pageIn(oldPlayer, newPlayer);
            }
        });
    }

    /** True while a player who used a Core Dump is dying: nothing they carry should drop. */
    public static boolean keepsInventory(Player player) {
        return Boolean.TRUE.equals(player.getAttached(KEEP_INVENTORY));
    }

    /** The terminal a dead player should respawn at, or null to respawn as usual. */
    @Nullable
    public static TeleportTransition respawnTarget(ServerPlayer player) {
        TerminalRef target = player.getAttached(PENDING);
        if (target == null || !player.isDeadOrDying()) {
            return null;
        }
        return GridArrival.arrivalTarget(player.level().getServer(), target, player.getYRot(), player.getXRot()).orElse(null);
    }

    private static void pageOut(ServerPlayer player) {
        if (!ModDimensions.isGrid(player.level()) || holdsDeathProtector(player)) {
            return;
        }
        // A Core Dump does everything Memory Paging does and more, so it's used first
        ItemStack coreDump = find(player, ModItems.CORE_DUMP);
        ItemStack used = coreDump.isEmpty() ? find(player, ModItems.MEMORY_PAGING) : coreDump;
        if (used.isEmpty()) {
            return;
        }
        used.shrink(1);
        if (!coreDump.isEmpty()) {
            player.setAttached(KEEP_INVENTORY, true);
        }
        Identifier grid = player.level().dimension().identifier();
        TerminalRef target = player.getAttachedOrCreate(GridMemory.TYPE).lastArrival()
                .filter(terminal -> terminal.grid().equals(grid))
                .orElse(new TerminalRef(grid, GridLayout.MAIN_ARRIVAL_ID));
        player.setAttached(PENDING, target);
        StrongholdManager.fire(player.level(), new GridEvent.MemoryPaged());
    }

    // What the keepInventory game rule copies; vanilla skips it for a death without that rule
    private static void copyKeptInventory(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        if (!keepsInventory(oldPlayer)) {
            return;
        }
        newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
        newPlayer.experienceLevel = oldPlayer.experienceLevel;
        newPlayer.totalExperience = oldPlayer.totalExperience;
        newPlayer.experienceProgress = oldPlayer.experienceProgress;
        newPlayer.setScore(oldPlayer.getScore());
    }

    private static void pageIn(ServerPlayer oldPlayer, ServerPlayer player) {
        TerminalRef target = player.removeAttached(PENDING);
        if (target == null) {
            return;
        }
        if (player.level().dimension().identifier().equals(target.grid())) {
            String key = keepsInventory(oldPlayer) ? "message.digitalfrontier.core_dumped" : "message.digitalfrontier.memory_paged";
            GridArrival.recordArrival(player, target,
                    Component.translatable(key, GridArrival.terminalName(target.terminal())).withStyle(ChatFormatting.AQUA));
        } else {
            // The grid or terminal couldn't be found when the player respawned, so they respawned as usual
            player.displayClientMessage(Component.translatable("message.digitalfrontier.terminal_unreachable").withStyle(ChatFormatting.RED), true);
        }
    }

    // Fabric asks before a totem gets its chance; leave the items alone if a totem will save the player
    private static boolean holdsDeathProtector(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).has(DataComponents.DEATH_PROTECTION)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack find(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
