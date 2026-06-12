package appeng.integration.modules.rei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

public class AttunementDisplay extends BasicDisplay {

    public AttunementDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Component... description) {
        super(inputs, outputs);

        for (var input : inputs) {
            for (var entry : input) {
                entry.tooltip(description);
            }
        }
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIds.ATTUNEMENT;
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
