package appeng.client.integration.rei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;

import appeng.core.definitions.AEItems;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.rei.CategoryIds;
import appeng.integration.modules.rei.EntropyRecipeDisplay;
import appeng.items.tools.powered.EntropyManipulatorItem;

public class EntropyRecipeCategory implements DisplayCategory<EntropyRecipeDisplay> {
    private static final int PADDING = 5;
    private static final int BODY_TEXT_COLOR = 0x7E7E7E;

    @Override
    public CategoryIdentifier<? extends EntropyRecipeDisplay> getCategoryIdentifier() {
        return CategoryIds.ENTROPY_MANIPULATOR;
    }

    @Override
    public Renderer getIcon() {
        // was: a custom Renderer lambda blitting textures/item/entropy_manipulator.png. REI 26.1.819's Renderer now
        // takes its own compat.GuiGraphics (a GuiGraphicsExtractor subclass), so the texture icon is implementable
        // again — but the item entry renderer is visually equivalent, so it is kept deliberately (no functional gain).
        return EntryStacks.of(AEItems.ENTROPY_MANIPULATOR.stack());
    }

    @Override
    public Component getTitle() {
        return AEItems.ENTROPY_MANIPULATOR.stack().getHoverName();
    }

    @Override
    public List<Widget> setupDisplay(EntropyRecipeDisplay recipe, Rectangle bounds) {
        for (var ingredient : recipe.getConsumed()) {
            for (EntryStack<?> entryStack : ingredient) {
                // The pre-26.1 code additionally overlaid a "consumed" cross texture via a custom EntryRenderer.
                // EntryRenderer#render is implementable again on REI 26.1.819 (compat.GuiGraphics), BUT the original
                // blit sprite coordinates (0,0,0,52,16,16 with textureW/H=16) map to uWidth=0 under the 26.1 blit
                // signature — malformed/no-op — so the historic coords cannot be restored verbatim. Only the tooltip
                // marker remains. PRISM: to restore the cross, verify the sprite's (u,v,w,h) in jei.png in-world.
                entryStack.tooltip(ItemModText.CONSUMED.text().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }

        var mode = recipe.getRecipe().getMode();

        var widgets = new ArrayList<Widget>();
        widgets.add(Widgets.createRecipeBase(bounds));

        var centerX = bounds.getCenterX();
        var y = bounds.getY() + PADDING;

        var labelText = switch (mode) {
            case HEAT -> ItemModText.ENTROPY_MANIPULATOR_HEAT.text(EntropyManipulatorItem.ENERGY_PER_USE);
            case COOL -> ItemModText.ENTROPY_MANIPULATOR_COOL.text(EntropyManipulatorItem.ENERGY_PER_USE);
        };
        var interaction = switch (mode) {
            case HEAT -> ItemModText.RIGHT_CLICK.text();
            case COOL -> ItemModText.SHIFT_RIGHT_CLICK.text();
        };

        var modeLabel = Widgets.createLabel(new Point(centerX + 4, y + 2), labelText)
                .color(BODY_TEXT_COLOR)
                .noShadow()
                .centered();
        var modeLabelX = modeLabel.getBounds().x;
        widgets.add(modeLabel);
        var modeIcon = switch (mode) {
            case HEAT -> Widgets.createTexturedWidget(ReiClientPlugin.TEXTURE, modeLabelX - 9, y + 3, 0, 68, 6, 6);
            case COOL -> Widgets.createTexturedWidget(ReiClientPlugin.TEXTURE, modeLabelX - 9, y + 3, 6, 68, 6, 6);
        };
        widgets.add(modeIcon);

        widgets.add(Widgets.createArrow(new Point(centerX - 12, y + 14)));
        widgets.add(Widgets.createLabel(new Point(centerX, y + 38), interaction)
                .color(BODY_TEXT_COLOR).noShadow().centered());

        widgets.add(Widgets.createSlot(new Point(centerX - 34, y + 15)).entries(recipe.getInput()).markInput());

        int x = centerX + 20;

        // In-World Block or Fluid output
        for (var entries : recipe.getConsumed()) {
            widgets.add(Widgets.createSlot(new Point(x, y + 15)).entries(entries));
            x += 18;
        }
        for (var entries : recipe.getOutputEntries()) {
            widgets.add(Widgets.createSlot(new Point(x, y + 15)).entries(entries).markOutput());
            x += 18;
        }

        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 50 + 2 * PADDING;
    }

    @Override
    public int getDisplayWidth(EntropyRecipeDisplay display) {
        return 130 + 2 * PADDING;
    }
}
