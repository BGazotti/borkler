package gazcreations.borkler.entities;

import gazcreations.borkler.Index;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * The Borkler Tile Entity. The heart of this mod, if you will.
 * <p>
 * This class handles all of the boiler's processes. Consuming fluids,
 * outputting fluids, burning stuff, etc.
 * 
 * @author gazotti
 *
 */
public class BorklerTileEntity extends LockableTileEntity implements ISidedInventory, IRecipeHolder,
		IRecipeHelperPopulator, ITickableTileEntity, IFluidHandler, IItemHandler {

	/**
	 * Hereby referred interchangeably to as tank #0.
	 */
	private FluidStack water;
	/**
	 * Hereby referred interchangeably to as tank #1.
	 */
	private FluidStack fuel;
	/**
	 * Hereby referred interchangeably to as tank #2. Output only.
	 */
	private FluidStack steam;
	/**
	 * A single inventory slot for up to 64 burnables.
	 */
	private Inventory solidFuel;
	/**
	 * This one is a bit trickier. A positive, non-zero burn time indicates that the
	 * boiler is active, consuming water and producing steam. A zero burn time
	 * indicates that the boiler is currently not active, not consuming water and
	 * not outputting steam. <br>
	 * Mind you that a positive burnTime does not reflect the amount of liquid or
	 * solid fuel left in the boiler's tank or inventory, since it may have just
	 * used up its last unit of fuel to remain powered for burnTime ticks.
	 */
	private int burnTime;

	/**
	 * Whether this boiler is currently operating. This is used in ticking
	 * calculations, modeling and other stuff.
	 */
	private boolean isActive;

	/**
	 * A constructor. Populates the Borkler's tanks with empty FluidStacks,
	 * initializes its inventory and sets burnTime to zero.
	 * 
	 * @param typeIn
	 */
	protected BorklerTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
		this.water = new FluidStack(Fluids.WATER, 0);
		this.fuel = new FluidStack(Fluids.EMPTY, 0);
		this.steam = new FluidStack(Index.Fluids.STEAM, 0);
		this.solidFuel = new Inventory(ItemStack.EMPTY);
		this.burnTime = 0;
		this.isActive = false;

	}

	/**
	 * @return 1, since it has a single slot for solid fuel.
	 */
	@Override
	public int getSlots() {
		return 1;
	}

	/**
	 * @return whatever ItemStack is currently held in its inventory.
	 * @param slot Not used, since there is only one slot.
	 */
	@Override
	public ItemStack getStackInSlot(int slot) {
		// return new ItemStack(() -> solidFuel.getItem(), solidFuel.getCount());
		return solidFuel.getStackInSlot(0);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		if (!isItemValid(slot, stack))
			return stack;
		ItemStack existing = this.solidFuel.getStackInSlot(0);

		int limit = getSlotLimit(slot);
		if (!existing.isEmpty()) {
			if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
				return stack;

			limit -= existing.getCount();
		}

		if (limit <= 0)
			return stack;

		boolean reachedLimit = stack.getCount() > limit;

		if (!simulate) {
			if (existing.isEmpty()) {
				solidFuel = new Inventory(reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
			} else {
				existing.grow(reachedLimit ? limit : stack.getCount());
			}
			markDirty();
		}

		return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param slot Not used, since the boiler only contains 1 inventory slot.
	 * @return 64 units of whatever it is you're burning.
	 */
	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	/**
	 * Checks whether the ItemStack is a suitable candidate for placement in the
	 * boiler inventory slot.
	 * 
	 * @param slot Not used.
	 * @return true if it burns.
	 */
	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return stack.getBurnTime() > 0;

	}

	/**
	 * @return 3. A tank for water (tank #0), a tank for liquid fuel(tank #1) and a
	 *         tank for steam(tank #2).
	 */
	@Override
	public int getTanks() {
		return 3;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		switch (tank) {
		// TODO check empty
		case 0:
			return new FluidStack(this.water, this.water.getAmount());
		case 1:
			return new FluidStack(this.fuel, this.fuel.getAmount());
		case 2:
			return new FluidStack(this.steam, this.steam.getAmount());
		}
		return new FluidStack(Fluids.EMPTY, 0);
	}

	/**
	 * @return Input tanks (#0 and #1) have a capacity of 4B. Output tank (Steam,
	 *         #2) has a capacity of 8B.
	 */
	@Override
	public int getTankCapacity(int tank) {
		switch (tank) {
		case 0:
			return 4000;
		case 1:
			return 4000;
		case 2:
			return 8000;
		}
		return -1;
	}

	/**
	 * @return Tank 0 can only hold water. Tank 1 can only hold liquid fuel. Tank 2
	 *         can only hold steam.
	 */
	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		switch (tank) {
		case 0:
			return stack.getFluid().isIn(FluidTags.WATER);
		case 1:
			// TODO add valid fuel types
			return stack.getFluid().isIn(FluidTags.LAVA) /* || acceptedFuels.contains(fluid) */;
		case 2:
			return stack.getFluid().isIn(FluidTags.getCollection().get(new ResourceLocation("forge:fluids/steam")));
		}
		return false;
	}

	/**
	 * Returns the proper tank (as a number) for whatever Fluid we got as a
	 * parameter. Will return either 0 if water, 1 if fuel, 2 if steam , or -1 if
	 * the Boiler cannot properly accomodate this fluid.
	 * 
	 * @param fluid The Fluid to check
	 * @return 0 if water, 1 if fuel, 2 if steam, or -1 if neither.
	 */
	private byte getTankForFluid(Fluid fluid) {
		if (fluid.isIn(FluidTags.WATER))
			return 0;
		if (fluid.isIn(FluidTags.LAVA) /* || acceptedFuels.contains(fluid) */) {
			return 1;
		}
		if (fluid.isIn(FluidTags.getCollection().get(new ResourceLocation("forge:fluids/steam")))) {
			return 2;
		}
		return -1;
	}

	private FluidStack getTank(byte index) {
		switch (index) {
		case 0: // water
			return this.water;
		case 1:
			return this.fuel;
		case 2:
			return this.steam;
		default:
			return FluidStack.EMPTY;
		}
	}

	/**
	 * Time to load up! Inserts a FluidStack into the boiler. Accepted fluids are
	 * water and liquid fuel (currently, only lava). Steam cannot be inserted into
	 * the boiler.
	 * 
	 * @param resource The FluidStack to insert
	 * @param action   Either FluidAction.SIMULATE or FluidAction.EXECUTE
	 * @return The amount of fluid transferred
	 *
	 */

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			// this means that you're not actually trying to put anything in the boiler.
			return 0;
		}
		byte whereDoIPutThis = getTankForFluid(resource.getFluid());
		if (whereDoIPutThis == -1)
			// fluid not valid
			return 0;
		if (action.simulate()) {
			if (getTank(whereDoIPutThis).isEmpty()) {
				// returns maximum amount of fluid that can fit into the tank
				return Math.min(getTankCapacity(whereDoIPutThis), resource.getAmount());
			} else {
				// returns maximum amount of fluid that can fit into the tank
				return Math.min(getTankCapacity(whereDoIPutThis) - getTank(whereDoIPutThis).getAmount(),
						resource.getAmount());
			}
		}
		// if we got this far, this is not a simulation. Time to fill'er up.
		FluidStack selectedTank = getTank(whereDoIPutThis);
		if (selectedTank.isEmpty()) {
			// initialize tank
			selectedTank = new FluidStack(resource, Math.min(getTankCapacity(whereDoIPutThis), resource.getAmount()));
			markDirty(); // this signals the game that stuff has changed
			return selectedTank.getAmount(); // this is how much fluid was inserted
		} else {
			if (!selectedTank.isFluidEqual(resource)) {
				// Fuel mixtures are not supported at this time. Water mixtures... still
				// thinking about that.
				return 0;
			}
			int remainingCapacity = getTankCapacity(whereDoIPutThis) - selectedTank.getAmount();
			if (resource.getAmount() < remainingCapacity) {
				// everything fits!
				selectedTank.grow(resource.getAmount());
				markDirty();
				return resource.getAmount();
			} else {
				// Tank is filled and there's fluid leftover
				selectedTank.setAmount(getTankCapacity(whereDoIPutThis));
				markDirty();
				return remainingCapacity;
			}
		}
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty())
			// nothing was drained
			return FluidStack.EMPTY;
		byte whereDidThisComeFrom = getTankForFluid(resource.getFluid());
		if (whereDidThisComeFrom == -1)
			// nothing was drained, fluid not valid
			return FluidStack.EMPTY;
		return drain(resource.getAmount(), action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		int drained = maxDrain;
		// Okay. Since we're talking about a steam boiler, it only makes sense
		// that whatever we drain here is steam, right?
		// guys?
		FluidStack selectedTank = steam;
		if (selectedTank.getAmount() < drained) {
			drained = selectedTank.getAmount();
		}
		FluidStack stack = new FluidStack(selectedTank, drained);
		if (action.execute() && drained > 0) {
			selectedTank.shrink(drained);
			markDirty();
		}
		return stack;
	}

	@Override
	protected ITextComponent getDefaultName() {
		// TODO Auto-generated method stub
		return ITextComponent.getTextComponentOrEmpty("Steam Boiler");
	}

	@Override
	protected Container createMenu(int id, PlayerInventory player) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Credits to the folks at @TeamCofh for helping me make sense of this method.
	 */
	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		if (solidFuel.getStackInSlot(0).isEmpty())
			return new ItemStack(() -> Items.AIR);
		ItemStack stack = solidFuel.decrStackSize(slot, amount);
		markDirty();
		return stack;
	}

	@Override
	public int getSizeInventory() {
		return solidFuel.getSizeInventory();
	}

	@Override
	public boolean isEmpty() {
		return (solidFuel == null || solidFuel.isEmpty() || solidFuel.getStackInSlot(0).isEmpty());
	}

	@Override
	public boolean isUsableByPlayer(PlayerEntity arg0) {
		return true;
	}

	@Override
	public ItemStack removeStackFromSlot(int arg0) {
		ItemStack stack = solidFuel.removeStackFromSlot(arg0);
		if (stack == null || stack.isEmpty())
			return stack;
		markDirty();
		return stack;
	}

	@Override
	public void setInventorySlotContents(int arg0, ItemStack arg1) {
		if (isItemValid(arg0, arg1)) {
			this.solidFuel.setInventorySlotContents(arg0, arg1);
			markDirty();
		}
	}

	@Override
	public void clear() {
		solidFuel.clear();
	}

	/**
	 * The clock is ticking.
	 * <p>
	 * So, this is where the magic happens. Every tick, the boiler will attempt to
	 * do its job, turning water into steam. This is possible if the boiler is
	 * currently powered and active.
	 */
	@Override
	public void tick() {
		// TODO Auto-generated method stub
		if (burnTime > 0) {
			// this means that the boiler is powered, and will attempt to turn water into
			// steam.
			if (water.isEmpty() || steam.getAmount() == getTankCapacity(2)) {
				// either there's no water or the steam tank is full. Boiler will not operate.
				// this will not use up fuel burn time, because I'm too nice.
				isActive = false;
				return;
			}
		}

	}

	@Override
	public void fillStackedContents(RecipeItemHelper arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public IRecipe<?> getRecipeUsed() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRecipeUsed(IRecipe<?> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canExtractItem(int arg0, ItemStack arg1, Direction arg2) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canInsertItem(int arg0, ItemStack arg1, Direction arg2) {
		// TODO implement
		if (!isItemValid(arg0, arg1))
			return false;
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction arg0) {
		return new int[] { 1 };
	}

}
