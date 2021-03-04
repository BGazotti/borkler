package gazcreations.borkler.blocks;

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
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class BorklerBlock extends Block implements ILiquidContainer {

	// private BorklerTileEntity tileEntity;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public BorklerBlock() {
		super(Properties.create(Material.PISTON).hardnessAndResistance(1.8f).sound(SoundType.STONE)
				.harvestTool(ToolType.PICKAXE).harvestLevel(0));
		this.setRegistryName("borkler", "steam_boiler");
		this.setDefaultState(stateContainer.getBaseState().with(ACTIVE, false));
	}

	/**
	 * This method is called when the block is right-clicked.
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

	@Override
	@Nullable
	public INamedContainerProvider getContainer(BlockState state, World world, BlockPos pos) {
		TileEntity tileentity = world.getTileEntity(pos);
		return tileentity instanceof INamedContainerProvider ? (INamedContainerProvider) tileentity : null;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BorklerTileEntity();
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> wut) {
		wut.add(ACTIVE);
	}

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
		// createTileEntity(state, worldIn);
		TileEntity tileentity = worldIn.getTileEntity(pos);

		if (tileentity instanceof BorklerTileEntity) {
			if (stack.hasDisplayName()) {
				((BorklerTileEntity) tileentity).setCustomName(stack.getDisplayName());
			}
			// this.tileEntity = (BorklerTileEntity) tileentity;
		} else {
			throw new RuntimeException("What in the absolute fuck? A Boiler has another type of TE that not its own!"
					+ "\n" + "Call Gaz!");
		}
	}

	@Override
	public boolean canContainFluid(IBlockReader arg0, BlockPos arg1, BlockState arg2, Fluid arg3) {
		return getTileEntity(arg0, arg1).getTankForFluid(arg3) >= 0;
	}

	@Override
	public boolean receiveFluid(IWorld arg0, BlockPos arg1, BlockState arg2, FluidState arg3) {
		// TODO Auto-generated method stub
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
}
