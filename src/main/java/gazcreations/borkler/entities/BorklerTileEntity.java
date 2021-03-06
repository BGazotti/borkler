package gazcreations.borkler.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import gazcreations.borkler.Borkler;
import gazcreations.borkler.Index;
import gazcreations.borkler.blocks.BorklerBlock;
import gazcreations.borkler.container.BorklerContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The Borkler Tile Entity. The heart of this mod, if you will.
 * <p>
 * This class handles all of the boiler's processes. Consuming fluids,
 * outputting fluids, burning stuff, etc.
 * 
 * @author gazotti
 *
 */
public class BorklerTileEntity extends LockableTileEntity implements ITickableTileEntity, IFluidHandler, IItemHandler {

	/**
	 * This contains a somewhat hardcoded set of stuff that the boiler is allowed to
	 * accept as fuel, based on their burn time. <br>
	 * Note that, due to a feature of MC/Forge, trying to run
	 * {@link ForgeHooks#getBurnTime(ItemStack)} before a
	 * {@link PlayerLoggedInEvent} has occurred will return an invalid value, such
	 * as 0 or -1, instead of the correct burn time in ticks for that
	 * {@link ItemStack}. As such, this Collection is
	 */
	private Collection<?> validFuels;

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
	private final Inventory solidFuel;
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
	 * Whether this boiler is currently operating. This is used mostly for rendering
	 * purposes.
	 */
	private boolean isActive;

	/**
	 * A Set containing up to 6 fluid connections for this boiler. This set is
	 * always populated by {@link BorklerTileEntity#updateFluidConnections()}.
	 */
	private Set<LazyOptional<IFluidHandler>> connections;

	/**
	 * A constructor. Populates the Borkler's tanks with empty FluidStacks,
	 * initializes its inventory and sets burnTime to zero.
	 * 
	 */
	public BorklerTileEntity(IBlockReader world) {
		super(Index.BORKLER_TE_TYPE);
		this.water = new FluidStack(Fluids.EMPTY, 0);
		this.fuel = new FluidStack(Fluids.EMPTY, 0);
		this.steam = new FluidStack(Index.Fluids.STEAMSOURCE, 0);
		this.solidFuel = new Inventory(1) {
			public void markDirty() {
				BorklerTileEntity.this.markDirty();
			}

			@Override
			public boolean isItemValidForSlot(int slot, ItemStack stack) {
				return stack.getItem() == Items.COAL;
			}
		};
		this.burnTime = 0;
		this.isActive = false;
		this.connections = Collections.emptySet();
		this.world = (World) world;
		// updateFluidConnections();
		Borkler.LOGGER.debug("BorklerTileEntity created at " + pos);
	}

	/**
	 * This constructor exists only for {@link Index} purposes, and should not be
	 * used under other circumstances. TileEntities must have a world to be bound
	 * to.
	 */
	public BorklerTileEntity() {
		this(null);
	}

	/**
	 * Pretty self-explanatory. Active means that this boiler is currently burning
	 * something.
	 * 
	 * @return whether the boiler is active or not
	 */
	public boolean isActive() {
		return isActive;
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

	/**
	 * Sets this Boiler's isActive field to true or false, signaling that there's
	 * been a change of state, and updates the corresponding {@link BorklerBlock} to
	 * match its state.
	 * 
	 * @param active
	 */
	private final void setActive(final boolean active) {
		if (this.isActive == active) {
			return; // nothing to do, nothing changed
		}
		this.isActive = active;
		this.world.setBlockState(pos,
				Index.Blocks.BORKLERBLOCK.getStateContainer().getBaseState().with(BorklerBlock.ACTIVE, active));
		this.updateContainingBlockInfo();
		markDirty();
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		/*
		 * if (isActuallyALiquid(stack)) { // TODO this looks like it will fuck shit up.
		 * ItemStack copy = ItemHandlerHelper.copyStackWithSize(stack,
		 * stack.getCount()); IFluidHandler o = (IFluidHandler)
		 * copy.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		 * fill(o.drain(o.getFluidInTank(0).getAmount(), FluidAction.EXECUTE),
		 * FluidAction.EXECUTE); return copy;
		 * 
		 * }
		 */
		if (!isItemValid(0, stack))
			return stack;
		ItemStack existing = this.solidFuel.getStackInSlot(0);

		int limit = getSlotLimit(0);
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
				gazcreations.borkler.Borkler.LOGGER.debug("Inventory is empty, creating a new one");
				solidFuel.setInventorySlotContents(0,
						reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
				gazcreations.borkler.Borkler.LOGGER.debug("New inv contents are:");
				for (int i = 0; i < solidFuel.getSizeInventory(); i++) {
					gazcreations.borkler.Borkler.LOGGER.debug(solidFuel.getStackInSlot(i).toString());

				}
			} else {
				gazcreations.borkler.Borkler.LOGGER
						.debug("Inventory contains items: " + solidFuel.getStackInSlot(0).toString());
				existing.grow(reachedLimit ? limit : stack.getCount());
				gazcreations.borkler.Borkler.LOGGER
						.debug("Inventory now contains items: " + solidFuel.getStackInSlot(0).toString());

			}
			markDirty();
		}

		return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
	}

	/**
	 * @return An empty item stack; items will not be removed from the Boiler. It is
	 *         THAT clingy.
	 */
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
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
		return solidFuel.isItemValidForSlot(slot, stack);// ForgeHooks.getBurnTime(stack) > 0;

	}

	/**
	 * @return 3. A tank for water (tank #0), a tank for liquid fuel(tank #1) and a
	 *         tank for steam(tank #2).
	 */
	@Override
	public int getTanks() {
		return 3;
	}

	/**
	 * @return A copy of the FluidStack in a given tank. FluidStack.EMPTY if the
	 *         tank is empty or if the specified tank is not valid.
	 */
	@Override
	public FluidStack getFluidInTank(int tank) {
		switch (tank) {
		case 0:
			return water.isEmpty() ? FluidStack.EMPTY : new FluidStack(this.water, this.water.getAmount());
		case 1:
			return fuel.isEmpty() ? FluidStack.EMPTY : new FluidStack(this.fuel, this.fuel.getAmount());
		case 2:
			return steam.isEmpty() ? FluidStack.EMPTY : new FluidStack(this.steam, this.steam.getAmount());
		}
		return FluidStack.EMPTY;
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
	 * A more generic alternative to {@link BorklerTileEntity#isFluidValid(Fluid)}.
	 * <br>
	 * Valid fluids are water and liquid fuel. Steam is valid as a contained fluid,
	 * but cannot be inserted from the outside.
	 * 
	 * @param fluid The fluid to check.
	 * @return
	 */
	public boolean isFluidValid(Fluid fluid) {
		return getTankForFluid(fluid) > 0;
	}

	/**
	 * Valid fluids are water and liquid fuel. Steam is valid as a contained fluid,
	 * but cannot be inserted from the outside.
	 * 
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
			return stack.getFluid().isEquivalentTo(Index.Fluids.STEAM)
					|| stack.getFluid().isEquivalentTo(Index.Fluids.STEAMSOURCE)
					|| stack.getFluid().isIn(FluidTags.getCollection().get(new ResourceLocation("forge:fluids/steam")));
		}
		return false;
	}

	/**
	 * Returns the proper tank (as a number) for whatever Fluid we got as a
	 * parameter. Will return either 0 if water, 1 if fuel, 2 if steam , or -1 if
	 * the Boiler cannot properly hold this fluid.
	 * 
	 * @param fluid The {@link Fluid} to check
	 * @return 0 if water, 1 if fuel, 2 if steam, or -1 if neither.
	 */
	public byte getTankForFluid(Fluid fluid) {
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

	/**
	 * For minecraft reasons, items such as the lava bucket will be treated as solid
	 * fuel, when, in fact, they're liquid. This function should redirect lava from
	 * buckets to an actual tank where it will be stored and burned on-demand.
	 * 
	 * @param stack
	 * @return true if this ItemStack has the FluidHandler Capability.
	 */
	private boolean isActuallyALiquid(ItemStack stack) {
		return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).isPresent();
	}

	/**
	 * Will return the proper tank for a tank number (0-2).
	 * 
	 * @param index The tank number, from 0 to 2
	 * @return one of the three FluidStacks representing water, fuel or steam, or
	 *         {@link FluidStack#EMPTY} if the specified tank number is invalid.
	 */
	private FluidStack getTank(byte index) {
		switch (index) {
		case 0:
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
		if (whereDoIPutThis == -1 || whereDoIPutThis == 2)
			// fluid not valid, won't fill
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
			switch (whereDoIPutThis) {
			case 0:
				this.water = selectedTank;
				break;
			case 1:
				this.fuel = selectedTank;
				break;
			default:
				return 0; // we should never get here, plenty of checks by now
			}
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

	/**
	 * This method runs a check on all sides of this TileEntity and populates this
	 * {@link BorklerTileEntity#connections} with a HashSet containing up to 6
	 * {@link IFluidHandler} instances. <br>
	 * The populated set is guaranteed not to be null and not to contain any null
	 * elements.
	 * 
	 */
	public void updateFluidConnections() {
		Set<LazyOptional<IFluidHandler>> consumers = new HashSet<LazyOptional<IFluidHandler>>(7) {
			private static final long serialVersionUID = 1L;

			public boolean add(LazyOptional<IFluidHandler> element) {
				if (element == null || !element.isPresent())
					return false;
				return super.add(element);
			}
		};
		gazcreations.borkler.Borkler.LOGGER
				.debug("Borkler @" + world + " ," + pos + " has been politely asked to update its fluid connections.");
		gazcreations.borkler.Borkler.LOGGER.debug("Current connections are: " + this.connections.toString());
		LazyOptional<IFluidHandler> cap = LazyOptional.empty();
		TileEntity te = null;
		// Trigger warning: the following section may require subsequent use of
		// eyebleach.
		// up
		if ((te = this.world.getTileEntity(getPos().offset(Direction.UP))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN);
			if (cap.isPresent())
				consumers.add(cap);
		}
		// down
		if ((te = this.world.getTileEntity(getPos().offset(Direction.DOWN))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.UP);
			if (cap.isPresent())
				consumers.add(cap);
		}
		// east
		if ((te = this.world.getTileEntity(getPos().offset(Direction.EAST))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.WEST);
			if (cap.isPresent())
				consumers.add(cap);
		}
		// west
		if ((te = this.world.getTileEntity(getPos().offset(Direction.WEST))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.EAST);
			if (cap.isPresent())
				consumers.add(cap);
		}
		// north
		if ((te = this.world.getTileEntity(getPos().offset(Direction.NORTH))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.SOUTH);
			if (cap.isPresent())
				consumers.add(cap);
		}
		// south
		if ((te = this.world.getTileEntity(getPos().offset(Direction.SOUTH))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.NORTH);
			if (cap.isPresent())
				consumers.add(cap);
		}
		gazcreations.borkler.Borkler.LOGGER.debug("Borkler @" + world + " ," + pos + "has updated its connections: " + consumers.toString());
		this.connections = consumers;
	}

	/**
	 * Will attempt to pull a compatible fluid (i.e. water or fuel) from a connected
	 * {@link IFluidHandler}.
	 * 
	 * @param source
	 */
	public void pullFluid(IFluidHandler source) {
		source.drain(new FluidStack(Fluids.WATER, getTankCapacity(0)), FluidAction.EXECUTE);
	}

	/**
	 * @return Either {@link FluidStack#EMPTY} or a FluidStack of steam.
	 */
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
		// TODO this method visibly conflicts with drain(int, FluidAction). FIX.
	}

	/**
	 * Will drain steam.
	 */
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
		return new TranslationTextComponent("container.borkler.steam_boiler");
	}

	/**
	 * Opens up a Container for interacting with this Boiler's inventory.
	 */
	@Override
	protected Container createMenu(int id, PlayerInventory player) {
		Borkler.LOGGER.debug("Borkler @ " + pos + "clicked. Opening GUI shortly." + "\n"
				+ "Inventory contents are as follows:\n" + solidFuel.getSizeInventory() + "slots;");
		for (int i = 0; i < solidFuel.getSizeInventory(); i++)
			Borkler.LOGGER.debug("Slot" + i + ":" + solidFuel.getStackInSlot(i).getItem().getRegistryName() + "x "
					+ solidFuel.getStackInSlot(i).getCount());
		return new BorklerContainer(id, player, this.solidFuel);
	}

	/**
	 * Credits to the folks at @TeamCofh for helping me make sense of this method.
	 */
	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		if (solidFuel.getStackInSlot(0).isEmpty())
			return ItemStack.EMPTY;
		ItemStack stack = solidFuel.decrStackSize(0, amount);
		markDirty();
		return stack;
	}

	/**
	 * Wrapper for {@link Inventory#getSizeInventory()}.
	 */
	@Override
	public int getSizeInventory() {
		return solidFuel.getSizeInventory();
	}

	/**
	 * Wrapper for this {@link Inventory#isEmpty()}.
	 */
	@Override
	public boolean isEmpty() {
		return (solidFuel == null || solidFuel.isEmpty() || solidFuel.getStackInSlot(0).isEmpty());
	}

	/**
	 * Will return true, even though it shouldn't.
	 */
	@Override
	public boolean isUsableByPlayer(PlayerEntity arg0) {
		return true;
	}

	/**
	 * Will just pass this on to its underlying inventory.
	 */
	@Override
	public ItemStack removeStackFromSlot(int arg0) {
		ItemStack stonks = solidFuel.removeStackFromSlot(0);
		if (!stonks.isEmpty())
			markDirty();
		return stonks;
	}

	/**
	 * Will check item for validity and pass it on to its underlying inventory.
	 */
	@Override
	public void setInventorySlotContents(int arg0, ItemStack arg1) {
		if (isItemValid(0, arg1)) {
			this.solidFuel.setInventorySlotContents(0, arg1);
			markDirty();
		}
	}

	/**
	 * Wrapper for this {@link Inventory#clear()}.
	 */
	@Override
	public void clear() {
		solidFuel.clear();
		markDirty();
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
		// the boiler will refuse to operate if it has no water.
		if (water.isEmpty()) {
			// Borkler.LOGGER.debug("Boiler @" + pos + " shutting down due to lack of
			// water");
			setActive(false);
			return;
		}
		if (steam.getAmount() == getTankCapacity(2)) {
			// Borkler.LOGGER.debug("Boiler @" + pos + " shutting down because its steam
			// tank is full");
			setActive(false);
			return;

		}
		if (burnTime > 0 && steam.getAmount() < getTankCapacity(2) && water.getAmount() > 0) {
			// so, we have water, the boiler is active, and the steam tank is not full. It's
			// boiling time, boyos.
			boil();

		} else {
			burnTime = 0;
			// OK, so we're not currently burning fuel. Is there anything to burn, though?
			if (solidFuel.isEmpty() && fuel.isEmpty()) {
				// there's no liquid or solid fuel present. Boiler will be turned off.
				setActive(false);
				return;
			}
			if (!solidFuel.isEmpty()) {
				// OK, so there's solid fuel in the boiler. We'll try to burn this.
				// Fuel has been checked for burnability before being placed in the inventory.
				// This will remove one piece of fuel from the inventory and power the boiler
				// for its burnTime ticks.
				// EDIT: just found out that this will not work because of minecraft things.
				// I've yet to find a way to fix it. TODO .
				// burnTime += decrStackSize(0, 1).getBurnTime();
				// Borkler.LOGGER.debug("Burn time increased: now " + burnTime);
				// setActive(true);
				// return;
			}
			if (!fuel.isEmpty()) {
				// ok, there is liquid fuel in the boiler. We'll try to burn this.
				if (fuel.getFluid().isEquivalentTo(Fluids.LAVA)) {
					// Determining burn time for lava is tricky. Let's use the lava bucket, or 1000
					// mB, as reference.
					int bitOFuel = Math.min(fuel.getAmount(), 5);
					/*
					 * Found the bug. Lava bucket has burn time of -1.
					 *
					 * This is because of MC/Forge not loading the default burn time values until a
					 * player is logged in. The below value is hardcoded to match the default burn
					 * time of lava, which is 20000 ticks for a bucket, so 20 ticks per mB.
					 */
					// int additionalBurnTime = bitOFuel / (new ItemStack(() -> Items.LAVA_BUCKET,
					// 1).getBurnTime());
					int additionalBurnTime = bitOFuel * 20;

					Borkler.LOGGER.debug("Consuming " + bitOFuel + " mB of fuel " + fuel.getFluid().getRegistryName()
							+ " for " + additionalBurnTime);
					fuel.shrink(bitOFuel);
					burnTime += additionalBurnTime;
					Borkler.LOGGER.debug("Burn time increased: now " + burnTime);
					setActive(true);
					return;
				}
			}

		}

	}

	/**
	 * This function tries to drain 25 mB of the water tank and add 25 mB of Steam
	 * to the output tank. It is run on every tick while the boiler is active, and
	 * decreases burnTime by one tick. <br>
	 * If less than 25 mB of water is present, it will boil whatever's left. <br>
	 * Any excess steam is discarded.
	 */
	private final void boil() {
		int amount = Math.min(this.water.getAmount(), 25);
		this.water.shrink(amount);
		this.steam.grow(amount);
		if (steam.getAmount() > getTankCapacity(2))
			steam.setAmount(getTankCapacity(2));
		burnTime--;
		markDirty();
	}

	/**
	 * The Boiler is like the Void. Many things go in, only steam gets out.
	 * 
	 * @return false
	 *
	 * @Override public boolean canExtractItem(int arg0, ItemStack arg1, Direction
	 *           arg2) { return false; }
	 * 
	 *           /**
	 * @return {@link BorklerTileEntity.isItemValid}(arg0, arg1, arg2)
	 *
	 * @Override public boolean canInsertItem(int arg0, ItemStack arg1, Direction
	 *           arg2) { return isItemValid(arg0, arg1); }
	 * 
	 *           /**
	 * @return an array containing a single element.
	 *
	 * @Override public int[] getSlotsForFace(Direction arg0) { return new int[] { 1
	 *           }; }
	 */

	/**
	 * Writes this TileEntity's data, such as its state and contents, to NBT tags,
	 * for persistence.
	 */
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		CompoundNBT stuff = super.write(nbt);
		stuff.putInt("steam", this.steam.getAmount());
		stuff.putInt("water", this.water.getAmount());
		stuff.putString("fuelType", fuel.getFluid().getRegistryName().getPath());
		stuff.putInt("fuelAmount", this.fuel.getAmount());
		// stuff.put("fuelInv", solidFuel.write());
		stuff.putString("solidFuelType", this.solidFuel.getStackInSlot(0).getItem().getRegistryName().getPath());
		stuff.putInt("solidFuelAmount", this.solidFuel.getStackInSlot(0).getCount());
		stuff.putBoolean("isActive", isActive);
		stuff.putInt("burnTime", this.burnTime);
		return super.write(stuff);
	}

	/**
	 * Populates this TileEntity's fields with values stored in an NBT.
	 */
	@Override
	public void read(BlockState state, CompoundNBT nbt) {
		super.read(state, nbt);
		steam = new FluidStack(Index.Fluids.STEAMSOURCE, nbt.getInt("steam"));
		water = new FluidStack(Fluids.WATER, nbt.getInt("water"));
		fuel = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(nbt.getString("fuelType"))),
				nbt.getInt("fuelAmount"));
		solidFuel.setInventorySlotContents(0,
				new ItemStack(
						() -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("solidFuelType"))),
						nbt.getInt("solidFuelAmount")));
		// new Inventory(1).read((ListNBT) nbt.get("fuelInv"));
		isActive = nbt.getBoolean("isActive");
		burnTime = nbt.getInt("burnTime");
	}

	/**
	 * Enables this TileEntity to interact with pipes and inventories in-world, for
	 * automation purposes.
	 */
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @javax.annotation.Nullable Direction side) {
		if (this.isRemoved())
			return super.getCapability(cap, side);
		/*
		 * if (cap ==
		 * net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) { if
		 * (cachedItemHandlerCapability == null) { cachedItemHandlerCapability =
		 * LazyOptional.of(() -> solidFuel); } if
		 * (cachedItemHandlerCapability.isPresent()) return
		 * cachedItemHandlerCapability.cast(); }
		 */
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			// if (cachedFluidHandlerCapability == null) {
			// cachedFluidHandlerCapability = LazyOptional.of(() -> steam);
			// }
			return LazyOptional.of(() -> this).cast();
		}
		return super.getCapability(cap, side);
	}
}
