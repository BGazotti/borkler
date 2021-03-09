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

import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import gazcreations.borkler.entities.BorklerTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * The Boiler, ladies, gentlemen and everyone in between.
 * <p>
 * A single-block steam boiler. Easy to craft, easy to use. Just pipe in water
 * and some lava (or feed it some sort of solid fuel, works too) and BAM, ya got
 * steam. Just like that.
 * </p>
 * Technical stuff: this block implements {@link ILiquidContainer}, meaning you
 * can shift+click it with a bucket of water or liquid fuel and it will
 * appropriately fill up with whatever it is you're holding, as long as it can
 * use it. <br>
 * Turbines sold separately.
 * 
 * @author gazotti
 *
 */
public class BorklerBlock extends Block implements ILiquidContainer {

	/**
	 * This field implements {@link BlockState}s, enabling the boiler to have two
	 * separate sets of textures.
	 */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/**
	 * A Constructor. <br>
	 * Default values and properties for the Steam Boiler are similar to a piston's,
	 * but harder. It can be harvested without a tool, but it's easier to just use a
	 * pickaxe.
	 */
	public BorklerBlock() {
		super(Properties.create(Material.PISTON).hardnessAndResistance(2.0f).sound(SoundType.STONE)
				.harvestTool(ToolType.PICKAXE).harvestLevel(0).setLightLevel(new ToIntFunction<BlockState>() {
					@Override
					public int applyAsInt(BlockState value) {
						if (value.get(BorklerBlock.ACTIVE))
							return 13;
						else
							return 0;
					}
				}));
		this.setRegistryName("borkler", "steam_boiler");
		this.setDefaultState(stateContainer.getBaseState().with(ACTIVE, false));
	}

	/**
	 * This method is called when the block is right-clicked. It will open up the
	 * GUI, and that's pretty much it. <br>
	 * Fluid insertion logic via bucket is handled by their own items if you
	 * shift+click.
	 * 
	 * @param state
	 * @param worldIn
	 * @param pos
	 * @param player
	 * @param handIn
	 * @param hit
	 * @return
	 */
	@Override
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
			Hand handIn, BlockRayTraceResult hit) {
		if (!worldIn.isRemote) {
			// TODO implement GUI stuff
			INamedContainerProvider inamedcontainerprovider = this.getContainer(state, worldIn, pos);
			if (inamedcontainerprovider != null) {
				player.openContainer(inamedcontainerprovider);
			}

		}
		return ActionResultType.SUCCESS;
	}

	/**
	 * Gets the Container, which is essentially a way for interacting with this
	 * block's TileEntity inventory.
	 */
	@Override
	@Nullable
	public INamedContainerProvider getContainer(BlockState state, World world, BlockPos pos) {
		TileEntity tileentity = world.getTileEntity(pos);
		return tileentity instanceof INamedContainerProvider ? (INamedContainerProvider) tileentity : null;
	}

	/**
	 * @return true; see {@link BorklerTileEntity}
	 */
	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	/**
	 * Executes a quick check to see if this block's new neighbor is a
	 * {@link TileEntity}. If so, forces this Borkler's {@link BorklerTileEntity} to
	 * update its list of possible adjacent FluidHandlers, regarding direction. <br>
	 * See {@link BorklerTileEntity#updateFluidConnections()}.
	 */
	@Override
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {
		gazcreations.borkler.Borkler.LOGGER.debug("A Borkler has been notified of changes to its neighbors.");
		this.getTileEntity(world, pos).updateFluidConnections();
	}

	/**
	 * Returns a new {@link BorklerTileEntity}.
	 */
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BorklerTileEntity(world);
	}

	/**
	 * Adds the boolean property "active" to the set of possible variations of this
	 * block.
	 */
	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> stateContainer) {
		stateContainer.add(ACTIVE);
	}

	/**
	 * A wrapper for {@link World#getTileEntity(BlockPos)}, returning a proper
	 * instance of a {@link BorklerTileEntity}.
	 * 
	 * @param world
	 * @param pos   the position to query for a TileEntity
	 * @return
	 */
	private BorklerTileEntity getTileEntity(IBlockReader world, BlockPos pos) {
		TileEntity temp = world.getTileEntity(pos);
		if (temp instanceof BorklerTileEntity)
			return (BorklerTileEntity) temp;
		throw new RuntimeException(
				"What in the absolute fuck? A Boiler has another type of TileEntity that not its own!" + "\n"
						+ "Call Gaz!");
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BorklerTileEntity te = getTileEntity(worldIn, pos);
		if (stack.hasDisplayName()) {
			te.setCustomName(stack.getDisplayName());
		}
		te.updateFluidConnections();
	}

	/**
	 * @return see {@link BorklerTileEntity#isFluidValid(Fluid)}
	 */
	@Override
	public boolean canContainFluid(IBlockReader arg0, BlockPos arg1, BlockState arg2, Fluid arg3) {
		return getTileEntity(arg0, arg1).getTankForFluid(arg3) >= 0;
	}

	/**
	 * Will attempt to insert the fluid from a bucket into one of this Boiler's
	 * internal tanks. See {@link BorklerTileEntity#isFluidValid(Fluid)} for valid
	 * fluids. <br>
	 * Will only insert 1000 mB worth of fluid if such room is available.
	 * 
	 * @return whether the fluid was successfully inserted or not
	 */
	@Override
	public boolean receiveFluid(IWorld arg0, BlockPos arg1, BlockState arg2, FluidState arg3) {
		BorklerTileEntity tileEntity = getTileEntity(arg0, arg1);
		int tank = tileEntity.getTankForFluid(arg3.getFluid());
		if (tank < 0 || tank == 2)
			return false;
		if (tileEntity.getTankCapacity(tank) - tileEntity.getFluidInTank(tank).getAmount() < 1000)
			return false;
		if (tileEntity.fill(new FluidStack(arg3.getFluid(), 1000), FluidAction.SIMULATE) > 0) {
			return tileEntity.fill(new FluidStack(arg3.getFluid(), 1000), FluidAction.EXECUTE) > 0;
		}
		return false;
	}

	/**
	 * Overriden to drop items in the Boiler's inventory if it is destroyed.
	 */
	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
		super.onBlockHarvested(worldIn, pos, state, player);
		getTileEntity(worldIn, pos).remove();
		InventoryHelper.dropInventoryItems(worldIn, pos, getTileEntity(worldIn, pos));
	}

}
