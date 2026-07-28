/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.block.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.MolecularAssemblerMenu;
import appeng.menu.locator.MenuLocators;

public class MolecularAssemblerBlock extends AEBaseEntityBlock<MolecularAssemblerBlockEntity> {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public MolecularAssemblerBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, MolecularAssemblerBlockEntity be) {
        return currentState.setValue(POWERED, be.isPowered());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        var be = this.getBlockEntity(level, pos);
        if (be != null) {
            if (!level.isClientSide()) {
                MenuOpener.open(MolecularAssemblerMenu.TYPE, player,
                        MenuLocators.forBlockEntity(be));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


    /**
     * MoreCulling derives cull shapes via getOcclusionShape without re-checking canOcclude; with no
     * shape override this defaults to a full cube and every neighbouring face gets culled — players
     * see through the world at the block's recessed edges (live report 2026-07-28, same class as
     * the Create-ports sweep of 07-27). Saying explicitly what noOcclusion() already promises.
     * Public, not protected: MoreCulling's classTweaker widens the vanilla method and an override
     * may not narrow access. Zero vanilla behaviour change (vanilla consults this only when
     * canOcclude).
     */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(net.minecraft.world.level.block.state.BlockState state) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
}
