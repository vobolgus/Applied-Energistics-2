package appeng.neoforge;

import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.access.ItemAccess;

import appeng.api.features.P2PTunnelAttunement;
import appeng.core.localization.GuiText;

/**
 * NeoForge capability-typed convenience overloads for {@link P2PTunnelAttunement}. These used to be the
 * {@code registerAttunementApi}/{@code registerItemAccessAttunementApi} methods of that class before the public API was
 * decoupled from loader types.
 */
public final class AENeoForgeP2PAttunement {
    private AENeoForgeP2PAttunement() {
    }

    /**
     * Registers AE2's own capability-based attunements. Called once after registration, right after
     * {@code InitP2PAttunements.init()} (which registers the loader-neutral tag attunements).
     */
    public static void init() {
        registerItemAccessAttunementApi(P2PTunnelAttunement.ENERGY_TUNNEL,
                Capabilities.Energy.ITEM,
                GuiText.P2PAttunementEnergy.text());
        registerItemAccessAttunementApi(P2PTunnelAttunement.FLUID_TUNNEL,
                Capabilities.Fluid.ITEM,
                GuiText.P2PAttunementFluid.text());
    }

    /**
     * Attunement based on the ability of getting a capability from the item.
     *
     * @param tunnelPart  The P2P-tunnel part item.
     * @param description Description for display in REI/JEI.
     */
    public static void registerAttunementApi(ItemLike tunnelPart, ItemCapability<?, Void> cap,
            Component description) {
        Objects.requireNonNull(cap, "cap");
        Predicate<ItemStack> test = stack -> stack.getCapability(cap) != null;
        P2PTunnelAttunement.registerAttunementApi(tunnelPart, cap, test, description);
    }

    /**
     * Attunement based on the ability of getting a capability from the item.
     *
     * @param tunnelPart  The P2P-tunnel part item.
     * @param description Description for display in REI/JEI.
     */
    public static void registerItemAccessAttunementApi(ItemLike tunnelPart,
            ItemCapability<?, ItemAccess> cap,
            Component description) {
        Objects.requireNonNull(cap, "cap");
        Predicate<ItemStack> test = stack -> stack.getCapability(cap, ItemAccess.forStack(stack)) != null;
        P2PTunnelAttunement.registerAttunementApi(tunnelPart, cap, test, description);
    }
}
