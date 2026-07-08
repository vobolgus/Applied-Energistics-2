/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.server.testplots;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import appeng.core.definitions.AEBlocks;
import appeng.hooks.SkyStoneBreakSpeed;
import appeng.server.testworld.PlotBuilder;

@TestPlotClass
public final class SkyStoneTestPlots {

    private SkyStoneTestPlots() {
    }

    /**
     * Regression test for {@link SkyStoneBreakSpeed}: a tool better than iron breaks sky stone
     * {@link SkyStoneBreakSpeed#SPEEDUP_FACTOR}× faster, while an iron (or worse) tool leaves it unchanged.
     * <p>
     * Loader-agnostic on purpose — it passes through NeoForge's {@code PlayerEvent.BreakSpeed} and Fabric's
     * {@code PlayerDestroySpeedMixin} alike, so it guards the whole feature from either loader.
     * <p>
     * The assertion is a <em>ratio</em> between sky stone and a plain-stone control mined with the same tool.
     * A diamond pickaxe reports the same tool speed on any {@code mineable/pickaxe} block, so every player-state
     * factor in {@code Player#getDestroySpeed} (on-ground, in-fluid, attribute modifiers) cancels between the two
     * calls and only the hook's multiplier survives — making the check deterministic regardless of where the mock
     * player happens to stand.
     */
    @TestPlot("sky_stone_break_speed")
    public static void skyStoneBreakSpeed(PlotBuilder plot) {
        plot.block(BlockPos.ZERO, AEBlocks.SKY_STONE_BLOCK);
        plot.test(helper -> {
            var skyStone = AEBlocks.SKY_STONE_BLOCK.block().defaultBlockState();
            var control = Blocks.STONE.defaultBlockState(); // same pickaxe tool speed, never sped up
            var player = helper.makeMockPlayer(GameType.SURVIVAL);

            // Tool better than iron (diamond speed 8 > iron 6): sky stone is sped up ×SPEEDUP_FACTOR.
            player.setItemSlot(EquipmentSlot.MAINHAND, Items.DIAMOND_PICKAXE.getDefaultInstance());
            float fastSky = player.getDestroySpeed(skyStone);
            float fastControl = player.getDestroySpeed(control);
            helper.assertTrue(fastControl > 0f, "control (stone) break speed should be positive");
            float ratio = fastSky / fastControl;
            helper.assertTrue(
                    Math.abs(ratio - SkyStoneBreakSpeed.SPEEDUP_FACTOR) < 0.01f,
                    "sky stone should break " + SkyStoneBreakSpeed.SPEEDUP_FACTOR
                            + "x faster than stone with a better-than-iron tool, but the ratio was " + ratio);

            // Iron tool (speed 6, not strictly greater than iron): no speedup, sky stone == stone.
            player.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_PICKAXE.getDefaultInstance());
            float ironSky = player.getDestroySpeed(skyStone);
            float ironControl = player.getDestroySpeed(control);
            helper.assertTrue(
                    Math.abs(ironSky - ironControl) < 0.01f,
                    "sky stone should NOT be sped up with an iron tool, but " + ironSky + " != " + ironControl);

            helper.succeed();
        });
    }
}
