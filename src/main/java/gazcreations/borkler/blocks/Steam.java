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

import gazcreations.borkler.Index;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.fluids.FluidAttributes;

/**
 * Oh boy. Steam.
 * <p>
 * The core fluid of this mod. Still very much a work in progress, with a lot of
 * hardcoded magic numbers. Implementing this was a real pain. I hope other mod
 * developers have an easier time with it than I did.
 * </p>
 * <p>
 * Borkler Steam uses many default attributes from water, and (at the time of
 * writing) behaves bizarrely when in-world. I mean, it's not supposed to be
 * in-world, just in pipes, ME networks, tanks and the likes. But still, I'd
 * find the whole thing a lot less unsettling if steam behaved... in a
 * predictable fashion, at least. Here goes nothing.
 * </p>
 * 
 * @author gazotti
 *
 */
public abstract class Steam extends FlowingFluid {

	@Override
	/**
	 * Creates the Steam FluidAttributes. Many of them are identical to water, or
	 * random properties from the top of my head.
	 */
	protected FluidAttributes createAttributes() {
		return net.minecraftforge.fluids.FluidAttributes
				.builder(new ResourceLocation("block/water_still"), new ResourceLocation("block/water_flow"))
				.overlay(new ResourceLocation("block/water_overlay")).translationKey("block.borkler.steam_source")
				.color(0xFFD6EBEB).sound(SoundEvents.ITEM_BUCKET_FILL, SoundEvents.ITEM_BUCKET_EMPTY).density(-5)
				.temperature(373).gaseous().viscosity(500).build(Index.Fluids.STEAMSOURCE);
	}

	/**
	 * Are you seriously trying to put Steam in a bucket? Not going to happen.
	 * 
	 * @return Air. Yep.
	 */
	@Override
	public Item getFilledBucket() {
		return Items.AIR;
	}

	/**
	 * We're talking about cool non-pressurized steam. It won't displace anything.
	 * 
	 * @return false
	 */
	@Override
	protected boolean canDisplace(FluidState fluidState, IBlockReader blockReader, BlockPos pos, Fluid fluid,
			Direction direction) {
		return false;
	}

	/**
	 * Same rate as water.
	 * 
	 * @return 5, the default tick rate for water.
	 */
	@Override
	public int getTickRate(IWorldReader p_205569_1_) {
		return 5;
	}

	/**
	 * Steam is not resistant to explosions, and will dissipate if exposed to one.
	 * 
	 * @return 0
	 */
	@Override
	protected float getExplosionResistance() {
		return 0;
	}

	/**
	 * @return A reference to flowing steam.
	 */
	@Override
	public Fluid getFlowingFluid() {
		return Index.Fluids.STEAM;
	}

	/**
	 * @return A reference to a steam source block.
	 */
	@Override
	public Fluid getStillFluid() {
		return Index.Fluids.STEAMSOURCE;
	}

	/**
	 * @return false. What? The whole point of this mod is that you can't magically
	 *         produce steam out of steam. Or can you?
	 */
	@Override
	protected boolean canSourcesMultiply() {
		return false;
	}

	/**
	 * I should probably implement this method.
	 */
	@Override
	protected void beforeReplacingBlock(IWorld worldIn, BlockPos pos, BlockState state) {
		// TODO implement
	}

	/**
	 * @return 4. Water value.
	 */
	@Override
	protected int getSlopeFindDistance(IWorldReader worldIn) {
		return 4;
	}

	/**
	 * Ok, you got me. I do not know what this method does. But it's there, and
	 * perhaps the reason for the bizarre behaviour?
	 */
	@Override
	public BlockState getBlockState(FluidState state) {
		return Index.Blocks.STEAM.getDefaultState().with(FlowingFluidBlock.LEVEL,
				Integer.valueOf(getLevelFromState(state)));
	}

	/**
	 * @return 1. Water value.
	 */
	@Override
	public int getLevelDecreasePerBlock(IWorldReader worldIn) {
		return 1;
	}

	/**
	 * A Flowing Steam fluid.
	 * 
	 * @author gazotti
	 *
	 */
	public static class Flowing extends Steam {

		public Flowing() {
			this.setRegistryName("borkler", "steam_flowing");
			this.createAttributes();
		}

		@Override
		protected FluidAttributes createAttributes() {
			return net.minecraftforge.fluids.FluidAttributes
					.builder(new ResourceLocation("block/water_still"), new ResourceLocation("block/water_flow"))
					.overlay(new ResourceLocation("block/water_overlay")).translationKey("block.borkler.steam_source")
					.color(0xFFD6EBEB).sound(SoundEvents.ITEM_BUCKET_FILL, SoundEvents.ITEM_BUCKET_EMPTY).density(-5)
					.temperature(373).gaseous().viscosity(500).build(Index.Fluids.STEAM);
		}

		@Override
		protected void fillStateContainer(StateContainer.Builder<Fluid, FluidState> builder) {
			super.fillStateContainer(builder);
			builder.add(LEVEL_1_8);
		}

		@Override
		public int getLevel(FluidState state) {
			return state.get(LEVEL_1_8);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}

	/**
	 * A Steam source block.
	 * @author gazotti
	 *
	 */
	public static class Source extends Steam {

		public Source() {
			this.setRegistryName("borkler", "steam_source");
			createAttributes();
		}

		@Override
		public int getLevel(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}

}
