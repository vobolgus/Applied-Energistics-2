package appeng.client.integration.rei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.ButtonArea;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;

import appeng.api.config.Actionable;
import appeng.api.config.CondenserOutput;
import appeng.api.features.P2PTunnelAttunementInternal;
import appeng.api.integrations.rei.IngredientConverters;
import appeng.client.AppEngClient;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.InscriberScreen;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.FacadeCreativeTab;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.ItemDefinition;
import appeng.core.localization.GuiText;
import appeng.core.localization.ItemModText;
import appeng.integration.abstraction.ItemListMod;
import appeng.integration.modules.itemlists.CompatLayerHelper;
import appeng.integration.modules.itemlists.ItemPredicates;
import appeng.integration.modules.rei.AttunementDisplay;
import appeng.integration.modules.rei.CategoryIds;
import appeng.integration.modules.rei.ChargerDisplay;
import appeng.integration.modules.rei.CondenserOutputDisplay;
import appeng.integration.modules.rei.EntropyRecipeDisplay;
import appeng.integration.modules.rei.InscriberRecipeDisplay;
import appeng.integration.modules.rei.ReiItemListModAdapter;
import appeng.integration.modules.rei.TransformRecipeWrapper;
import appeng.integration.modules.rei.transfer.EncodePatternTransferHandler;
import appeng.integration.modules.rei.transfer.UseCraftingRecipeTransfer;
import appeng.items.parts.FacadeItem;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.recipes.AERecipeTypes;

// was @me.shedaniel.rei.forge.REIPluginClient: NeoForge-only annotation, carried by the loader-overlay subclass
// appeng.client.integration.rei.NeoForgeReiClientPlugin; on Fabric this class is the rei_client entrypoint.
public class ReiClientPlugin implements REIClientPlugin {

    public static final Identifier TEXTURE = AppEng.makeId("textures/guis/jei.png");

    public ReiClientPlugin() {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        // was in ReiPlugin (common): REIRuntime is client-only, so the search-field adapter belongs here
        ItemListMod.setAdapter(new ReiItemListModAdapter());
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        AELog.info("Registering AE2 REI client categories");

        registry.add(new TransformCategory());
        registry.add(new CondenserCategory());
        registry.add(new InscriberRecipeCategory());
        registry.add(new AttunementCategory());
        registry.add(new ChargerCategory());
        registry.add(new EntropyRecipeCategory());

        registerWorkingStations(registry);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        registry.registerDraggableStackVisitor(new GhostIngredientHandler());
        registry.registerFocusedStack((screen, mouse) -> {
            if (screen instanceof AEBaseScreen<?> aeScreen) {
                var stack = aeScreen.getStackUnderMouse(mouse.x, mouse.y);
                if (stack != null) {
                    for (var converter : IngredientConverters.getConverters()) {
                        var entryStack = converter.getIngredientFromStack(stack.stack());
                        if (entryStack != null) {
                            return CompoundEventResult.interruptTrue(entryStack);
                        }
                    }
                }
            }

            return CompoundEventResult.pass();
        });
        registry.registerContainerClickArea(
                new Rectangle(82, 39, 26, 16),
                InscriberScreen.class,
                CategoryIds.INSCRIBER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        zones.register(AEBaseScreen.class, screen -> {
            return screen != null ? mapRects(screen.getExclusionZones()) : Collections.emptyList();
        });

    }

    private static List<Rectangle> mapRects(List<Rect2i> exclusionZones) {
        return exclusionZones.stream()
                .map(ez -> new Rectangle(ez.getX(), ez.getY(), ez.getWidth(), ez.getHeight()))
                .collect(Collectors.toList());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (AEConfig.instance().isEnableFacadeRecipesInRecipeViewer()) {
            registry.registerGlobalDisplayGenerator(new FacadeRegistryGenerator());
        }

        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        // was: registry.registerRecipeFiller(Class, RecipeType, Function) - removed in REI 21.x. Recipes are no
        // longer synced to the client since 1.21.2 either; displays are built from AE2's own recipe sync (the same
        // source the JEI integration uses) and registered with their RecipeHolder as origin so the transfer
        // handlers can recover the recipe via DisplayRegistry#getDisplayOrigin.
        addSyncedRecipes(registry, AERecipeTypes.INSCRIBER, InscriberRecipeDisplay::new);
        addSyncedRecipes(registry, AERecipeTypes.CHARGER, ChargerDisplay::new);
        addSyncedRecipes(registry, AERecipeTypes.TRANSFORM, TransformRecipeWrapper::new);
        addSyncedRecipes(registry, AERecipeTypes.ENTROPY, EntropyRecipeDisplay::new);
        // NOTE: the pre-26.1 special handling of StorageCellUpgradeRecipe (a registerRecipeFiller into the
        // crafting category) was dropped: vanilla 26.1 syncs crafting recipes to the client as RecipeDisplays,
        // which REI's builtin crafting plugin picks up natively (the JEI integration dropped it as well).

        registry.add(new CondenserOutputDisplay(CondenserOutput.MATTER_BALLS));
        registry.add(new CondenserOutputDisplay(CondenserOutput.SINGULARITY));

        registerDescriptions(registry);
    }

    private static <I extends RecipeInput, T extends Recipe<I>> void addSyncedRecipes(DisplayRegistry registry,
            RecipeType<T> recipeType, Function<RecipeHolder<T>, Display> displayFactory) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            // REI reloads its plugins when joining a world; outside of a world there are no synced recipes.
            return;
        }
        var recipes = AppEngClient.instance().getRecipeMapForType(level, recipeType);
        for (var holder : recipes.byType(recipeType)) {
            registry.add(displayFactory.apply(holder), holder);
        }
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        // Allow recipe transfer from REI to crafting and pattern terminal
        registry.register(new EncodePatternTransferHandler<>(PatternEncodingTermMenu.class));
        registry.register(new UseCraftingRecipeTransfer<>(CraftingTermMenu.class));
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(this::shouldEntryBeHidden);

        if (AEConfig.instance().isEnableFacadesInRecipeViewer()) {
            registry.addEntries(
                    EntryIngredients.ofItemStacks(FacadeCreativeTab.getDisplayItems()));
        }
    }

    @Override
    public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
        if (AEConfig.instance().isEnableFacadesInRecipeViewer()) {
            FacadeItem facadeItem = AEItems.FACADE.get();
            registry.group(AppEng.makeId("facades"), Component.translatable("itemGroup.ae2.facades"),
                    stack -> stack.getType() == VanillaEntryTypes.ITEM && stack.<ItemStack>castValue().is(facadeItem));
        }
    }

    private void registerWorkingStations(CategoryRegistry registry) {
        var condenser = AEBlocks.CONDENSER.stack();
        registry.addWorkstations(CategoryIds.CONDENSER, EntryStacks.of(condenser));

        var inscriber = AEBlocks.INSCRIBER.stack();
        registry.addWorkstations(CategoryIds.INSCRIBER, EntryStacks.of(inscriber));
        registry.setPlusButtonArea(CategoryIds.INSCRIBER, ButtonArea.defaultArea());

        var craftingTerminal = AEParts.CRAFTING_TERMINAL.stack();
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(craftingTerminal));

        var wirelessCraftingTerminal = chargeFully(AEItems.WIRELESS_CRAFTING_TERMINAL.stack());
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(wirelessCraftingTerminal));

        registry.addWorkstations(CategoryIds.CHARGER, EntryStacks.of(AEBlocks.CHARGER.stack()));
        registry.addWorkstations(CategoryIds.CHARGER, EntryStacks.of(AEBlocks.CRANK.stack()));

        var entropyManipulator = chargeFully(AEItems.ENTROPY_MANIPULATOR.stack());
        registry.addWorkstations(CategoryIds.ENTROPY_MANIPULATOR, EntryStacks.of(entropyManipulator));
    }

    private static ItemStack chargeFully(ItemStack stack) {
        if (stack.getItem() instanceof AEBasePoweredItem poweredItem) {
            poweredItem.injectAEPower(stack, poweredItem.getAEMaxPower(stack), Actionable.MODULATE);
        }
        return stack;
    }

    private void registerDescriptions(DisplayRegistry registry) {
        var all = EntryRegistry.getInstance().getEntryStacks().collect(EntryIngredient.collector());

        for (var entry : P2PTunnelAttunementInternal.getApiTunnels()) {
            var input = all.filter(
                    stack -> stack.getValue() instanceof ItemStack s && entry.stackPredicate().test(s));
            if (input.isEmpty()) {
                continue;
            }

            registry.add(new AttunementDisplay(
                    List.of(input),
                    List.of(EntryIngredient.of(EntryStacks.of(entry.tunnelType()))),
                    ItemModText.P2P_API_ATTUNEMENT.text(),
                    entry.description()));
        }

        for (var entry : P2PTunnelAttunementInternal.getTagTunnels().entrySet()) {
            var ingredient = Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(entry.getKey()));
            var entryIngredient = EntryIngredients.ofIngredient(ingredient);
            if (entryIngredient.isEmpty()) {
                continue;
            }

            registry.add(new AttunementDisplay(List.of(entryIngredient),
                    List.of(EntryIngredient.of(EntryStacks.of(entry.getValue()))),
                    ItemModText.P2P_TAG_ATTUNEMENT.text()));
        }

        addDescription(registry, AEItems.CERTUS_QUARTZ_CRYSTAL, GuiText.CertusQuartzObtain.getTranslationKey());

        if (AEConfig.instance().isSpawnPressesInMeteoritesEnabled()) {
            addDescription(registry, AEItems.LOGIC_PROCESSOR_PRESS, GuiText.inWorldCraftingPresses.getTranslationKey());
            addDescription(registry, AEItems.CALCULATION_PROCESSOR_PRESS,
                    GuiText.inWorldCraftingPresses.getTranslationKey());
            addDescription(registry, AEItems.ENGINEERING_PROCESSOR_PRESS,
                    GuiText.inWorldCraftingPresses.getTranslationKey());
            addDescription(registry, AEItems.SILICON_PRESS, GuiText.inWorldCraftingPresses.getTranslationKey());
        }

        addDescription(registry, AEBlocks.CRANK.item(), ItemModText.CRANK_DESCRIPTION.getTranslationKey());
    }

    private static void addDescription(DisplayRegistry registry, ItemDefinition<?> itemDefinition, String... message) {
        DefaultInformationDisplay info = DefaultInformationDisplay.createFromEntry(EntryStacks.of(itemDefinition),
                itemDefinition.stack().getHoverName());
        info.lines(Arrays.stream(message).map(Component::translatable).collect(Collectors.toList()));
        registry.add(info);
    }

    private boolean shouldEntryBeHidden(EntryStack<?> entryStack) {
        if (entryStack.getType() != VanillaEntryTypes.ITEM) {
            return false;
        }
        return ItemPredicates.shouldBeHidden(entryStack.castValue());
    }
}
