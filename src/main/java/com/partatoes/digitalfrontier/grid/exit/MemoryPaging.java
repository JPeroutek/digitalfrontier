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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.TeleportTarget;
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
            Identifier.of(DigitalFrontier.MOD_ID, "memory_paging_pending"),
            builder -> builder.persistent(TerminalRef.CODEC).copyOnDeath());
    /** Set at death when a Core Dump was used. Only read from the dead player, so it isn't copied to the new one. */
    private static final AttachmentType<Boolean> KEEP_INVENTORY = AttachmentRegistry.create(
            Identifier.of(DigitalFrontier.MOD_ID, "core_dump_keep_inventory"),
            builder -> builder.persistent(Codec.BOOL));

    private MemoryPaging() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
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
    public static boolean keepsInventory(PlayerEntity player) {
        return Boolean.TRUE.equals(player.getAttached(KEEP_INVENTORY));
    }

    /** The terminal a dead player should respawn at, or null to respawn as usual. */
    @Nullable
    public static TeleportTarget respawnTarget(ServerPlayerEntity player) {
        TerminalRef target = player.getAttached(PENDING);
        if (target == null || !player.isDead()) {
            return null;
        }
        return GridArrival.arrivalTarget(player.getServer(), target, player.getYaw(), player.getPitch()).orElse(null);
    }

    private static void pageOut(ServerPlayerEntity player) {
        if (!ModDimensions.isGrid(player.getServerWorld()) || holdsDeathProtector(player)) {
            return;
        }
        // A Core Dump does everything Memory Paging does and more, so it's used first
        ItemStack coreDump = find(player, ModItems.CORE_DUMP);
        ItemStack used = coreDump.isEmpty() ? find(player, ModItems.MEMORY_PAGING) : coreDump;
        if (used.isEmpty()) {
            return;
        }
        used.decrement(1);
        if (!coreDump.isEmpty()) {
            player.setAttached(KEEP_INVENTORY, true);
        }
        Identifier grid = player.getServerWorld().getRegistryKey().getValue();
        TerminalRef target = player.getAttachedOrCreate(GridMemory.TYPE).lastArrival()
                .filter(terminal -> terminal.grid().equals(grid))
                .orElse(new TerminalRef(grid, GridLayout.MAIN_ARRIVAL_ID));
        player.setAttached(PENDING, target);
        StrongholdManager.fire(player.getServerWorld(), new GridEvent.MemoryPaged());
    }

    // What the keepInventory game rule copies; vanilla skips it for a death without that rule
    private static void copyKeptInventory(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        if (!keepsInventory(oldPlayer)) {
            return;
        }
        newPlayer.getInventory().clone(oldPlayer.getInventory());
        newPlayer.experienceLevel = oldPlayer.experienceLevel;
        newPlayer.totalExperience = oldPlayer.totalExperience;
        newPlayer.experienceProgress = oldPlayer.experienceProgress;
        newPlayer.setScore(oldPlayer.getScore());
    }

    private static void pageIn(ServerPlayerEntity oldPlayer, ServerPlayerEntity player) {
        TerminalRef target = player.removeAttached(PENDING);
        if (target == null) {
            return;
        }
        if (player.getServerWorld().getRegistryKey().getValue().equals(target.grid())) {
            String key = keepsInventory(oldPlayer) ? "message.digitalfrontier.core_dumped" : "message.digitalfrontier.memory_paged";
            GridArrival.recordArrival(player, target,
                    Text.translatable(key, GridArrival.terminalName(target.terminal())).formatted(Formatting.AQUA));
        } else {
            // The grid or terminal couldn't be found when the player respawned, so they respawned as usual
            player.sendMessage(Text.translatable("message.digitalfrontier.terminal_unreachable").formatted(Formatting.RED), true);
        }
    }

    // Fabric asks before a totem gets its chance; leave the items alone if a totem will save the player
    private static boolean holdsDeathProtector(ServerPlayerEntity player) {
        for (Hand hand : Hand.values()) {
            if (player.getStackInHand(hand).contains(DataComponentTypes.DEATH_PROTECTION)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack find(ServerPlayerEntity player, Item item) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
