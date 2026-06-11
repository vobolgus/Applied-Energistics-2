package appeng.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.integration.modules.curios.CuriosSupport;

/**
 * Extension point used when AE2 is looking for ItemStacks in a player inventory. By default, AE2 only looks at the 36
 * usual slots of the player inventory (plus Curios slots, if present); register an additional source via
 * {@link #addStackSource} to make AE2 consider more stacks. AE2 will check after collection if they contain the item it
 * is searching.
 * <p>
 * This used to be an event posted on the NeoForge game bus; it is now an AE2-internal callback list so that it can be
 * shared across loaders.
 */
public final class SearchInventoryEvent {
    private static final List<BiConsumer<Player, List<ItemStack>>> SOURCES = new CopyOnWriteArrayList<>();

    static {
        // The 36 usual slots of the player inventory
        SOURCES.add((player, stacks) -> stacks.addAll(player.getInventory().getNonEquipmentItems()));
        // Curios slots, if Curios is present
        SOURCES.add((player, stacks) -> {
            var curios = CuriosSupport.get().getCuriosInventory(player);
            if (curios == null)
                return;
            for (int i = 0; i < curios.size(); i++) {
                stacks.add(curios.getStack(i));
            }
        });
    }

    private SearchInventoryEvent() {
    }

    /**
     * Registers an additional source of ItemStacks to consider when AE2 searches a player inventory.
     */
    public static void addStackSource(BiConsumer<Player, List<ItemStack>> source) {
        SOURCES.add(source);
    }

    public static List<ItemStack> getItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (var source : SOURCES) {
            source.accept(player, items);
        }
        return items;
    }
}
