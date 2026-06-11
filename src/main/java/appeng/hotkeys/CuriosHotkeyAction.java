package appeng.hotkeys;

import java.util.function.Predicate;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import appeng.api.features.HotkeyAction;
import appeng.integration.modules.curios.CuriosSupport;
import appeng.menu.locator.MenuLocators;

public record CuriosHotkeyAction(Predicate<ItemStack> locatable,
        InventoryHotkeyAction.Opener opener) implements HotkeyAction {

    public CuriosHotkeyAction(ItemLike item, InventoryHotkeyAction.Opener opener) {
        this((stack) -> stack.is(item.asItem()), opener);
    }

    @Override
    public boolean run(Player player) {
        var curios = CuriosSupport.get().getCuriosInventory(player);
        if (curios == null)
            return false;
        for (int i = 0; i < curios.size(); i++) {
            if (locatable.test(curios.getStack(i))) {
                if (opener.open(player, MenuLocators.forCurioSlot(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}
