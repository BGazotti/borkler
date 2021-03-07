/**
 *  Copyright 2021, B. Gazotti
 *
 *  This file is part of Borkler.
 *
 *  Borkler is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Borkler is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Borkler.  If not, see <https://www.gnu.org/licenses/>.
 */

package gazcreations.borkler.blocks;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import gazcreations.borkler.Index;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;

public class SteamBlock extends FlowingFluidBlock {

	public SteamBlock() {
		super(new Supplier<Steam>() {
			@Override
			public Steam get() {
				// TODO Auto-generated method stub
				return Index.Fluids.STEAMSOURCE;
			}
		}, AbstractBlock.Properties.create(Material.WATER, MaterialColor.LIGHT_GRAY).doesNotBlockMovement().hardnessAndResistance(0.0F).noDrops()
				.setLightLevel(new ToIntFunction<BlockState>() {

					@Override
					public int applyAsInt(BlockState value) {
						// TODO Auto-generated method stub
						return 7;
					}
				}));
		this.setRegistryName("borkler", "steam_source");
	}
}
