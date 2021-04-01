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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gazcreations.borkler.BorklerConfig;
import gazcreations.borkler.Index;
import gazcreations.borkler.container.BorklerContainer;
import gazcreations.borkler.recipes.BorklerFuel;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The Borkler Tile Entity. The heart of this mod, if you will.
 * <p>
 * This class handles all of the boiler's processes - even though it shouldn't.
 * Consuming fluids, outputting fluids, burning stuff, etc.
 * 
 * @author gazotti
 *
 */
public class BorklerTileEntity extends LockableTileEntity implements ITickableTileEntity, IFluidHandler, IItemHandler {

	/**
	 * A set of valid {@link Fluid} types to use as fuel.
	 */
	// @Deprecated
	// private static BorklerFluidList validuels = BorklerFluidList.getDefault();

	/**
	 * @return A copy (in case you're tempted to alter its contents) of the map of
	 *         valid fuels and burn times.
	 */
	// public static BorklerFluidList getValidFuelTypes() {
	// BorklerFluidList copy = new BorklerFluidList(validuels);
	// gazcreations.borkler.Borkler.LOGGER.debug("getting valid fuel types: " +
	// copy);
	// return copy;
	// }

	// public static void addFuel(Fluid fuel, int burnTime) {
	// validuels.put(fuel, burnTime);
	// if (ServerLifecycleHooks.getCurrentServer() != null) {
	// for (PlayerEntity player :
	// ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
	// addFutureServerTask(player.world, () ->
	// BorklerPacketHandler.sendToPlayer(player, validuels), true);
	// }
	// }
	// }

	/**
	 * Called when a client receives a BorklerFluidList update from the server, on
	 * login or reload.
	 * 
	 * @param fluids
	 * @param context
	 */
	// public static void updateValidFuelList(BorklerFluidList fluids,
	// java.util.function.Supplier<Context> context) {
	// Context ctx = context.get();
	// if (ctx.getDirection() == NetworkDirection.LOGIN_TO_CLIENT
	// || ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
	// gazcreations.borkler.Borkler.LOGGER.debug("updateValidFuelList has been
	// called; list is " + fluids);
	// validuels = fluids;
	// }
	// ctx.setPacketHandled(true);
	// }

	/**
	 * The tier of this boiler. Currently unused. <br>
	 * Will be used as a multiplier for tank capacity and steam production,
	 * probably. TODO implement.
	 */
	private byte tier;

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
	 * Networking, baby.<br>
	 * The number of ticks since the server has sent this TE's data to the client.
	 * Data will be synchronized after a few of those, as to not lag the whole thing
	 * out by synchronizing every tick. A tradeoff, of course, but a welcome one.
	 */
	private byte ticksSinceLastClientUpdate;

	/**
	 * A true value means that this entity has changed since its last configuration.
	 * Used along with {@link BorklerTileEntity#ticksSinceLastClientUpdate} to
	 * determine whether the server should send data to the client.
	 */

	/**
	 * A Set containing up to 6 fluid connections for this boiler. This set is
	 * always populated by {@link BorklerTileEntity#updateFluidConnections()}. <br>
	 */
	private Set<LazyOptional<IFluidHandler>> fluidConnections;

	/**
	 * A cacheable value for this entity's Capability<IFluidHandler>.
	 */
	private LazyOptional<IFluidHandler> fluidHandlerCapability;

	/**
	 * A Set containing up to 6 item connections for this boiler.
	 */
	private Set<LazyOptional<IItemHandler>> itemConnections;

	/**
	 * A cacheable value for this entity's Capability<IItemHandler>.
	 */
	private LazyOptional<IItemHandler> itemHandlerCapability;

	/**
	 * A constructor. Populates the Borkler's tanks with empty FluidStacks,
	 * initializes its inventory and sets burnTime to zero.
	 * 
	 */
	public BorklerTileEntity(IBlockReader world) {
		super(Index.BORKLER_TE_TYPE);
		this.tier = 1;
		this.water = new FluidStack(Fluids.WATER, 0);
		this.fuel = new FluidStack(Fluids.EMPTY, 0);
		this.steam = new FluidStack(Index.Fluids.STEAMSOURCE, 0);
		this.solidFuel = new Inventory(1) {

			/**
			 * Calls this TileEntity's markDirty() method.
			 */
			@Override
			public void markDirty() {
				BorklerTileEntity.this.markDirty();
			}

			/**
			 * Checks the burn time of the itemstack to see if it can be used as fuel.
			 */
			@Override
			public boolean isItemValidForSlot(int slot, ItemStack stack) {
				List<Item> hardcoded = new ArrayList<>();
				hardcoded.add(Items.BUCKET);
				return hardcoded.contains(stack.getItem()) || stack.isEmpty() || ForgeHooks.getBurnTime(stack) > 0;
			}
		};
		this.burnTime = 0;
		this.isActive = false;
		this.fluidConnections = Collections.emptySet();
		this.itemConnections = Collections.emptySet();
		this.world = (World) world;
		if (world != null) { // index TEs will not run this
			addFutureServerTask(this.world, () -> updateFluidConnections(), false);
			addFutureServerTask(this.world, () -> updateItemConnections(), false);
		}
	}

	/**
	 * This constructor exists only for {@link Index} purposes, and should not be
	 * used under other circumstances. TileEntities must have a world to be bound
	 * to.
	 */
	public BorklerTileEntity() {
		this(null);
	}

	public IInventory getInventory() {
		return this.solidFuel;
	}

	@Override
	public void markDirty() {
		if (this.world != null) {
			if (this.ticksSinceLastClientUpdate > 1) {
				addFutureServerTask(world, () -> this.world.notifyBlockUpdate(getPos(),
						getWorld().getBlockState(getPos()), getWorld().getBlockState(getPos()), 3), true);
				this.ticksSinceLastClientUpdate = 0;
			}
		}
		super.markDirty();
	}

	/**
	 * @return The burn time, in ticks, for a given solid fuel, corrected by
	 *         {@link BorklerConfig#NERFACTOR}.
	 */
	private int nerfdBurnTime(ItemStack item) {
		int defaultBT = ForgeHooks.getBurnTime(item);
		return defaultBT > 0 ? Math.toIntExact(Math.round(Math.floor(defaultBT * BorklerConfig.CONFIG.NERFACTOR.get())))
				: -1;
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
		if (world != null)
			this.world.setBlockState(pos,
					Index.Blocks.BORKLERBLOCK.getStateContainer().getBaseState().with(BorklerBlock.ACTIVE, active));
		requestModelDataUpdate();
		this.updateContainingBlockInfo();
		markDirty();
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;

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
				solidFuel.setInventorySlotContents(0,
						reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
			} else {
				existing.grow(reachedLimit ? limit : stack.getCount());

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
		return solidFuel.isItemValidForSlot(0, stack);
	}

	/**
	 * See {@link BorklerTileEntity#isItemValid(int, ItemStack)}.
	 */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return solidFuel.isItemValidForSlot(0, stack);
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
	 * @return The FluidStack in a given tank. FluidStack.EMPTY if the tank is empty
	 *         or if the specified tank is not valid. <br>
	 *         Again, DO NOT MODIFY THIS FLUIDSTACK.
	 */
	@Override
	public FluidStack getFluidInTank(int tank) {
		switch (tank) {
		case 0:
			return water;
		case 1:
			return fuel;
		case 2:
			return steam;
		}
		return FluidStack.EMPTY;
	}

	/**
	 * @return Input tanks (#0 and #1) have a capacity of 4B. Output tank (Steam,
	 *         #2) has a capacity of 8B. TODO implement tiers.
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
	 * A more generic alternative to
	 * {@link BorklerTileEntity#isFluidValid(int, FluidStack)}. <br>
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
			// return validFuels.containsKey(stack.getFluid());
			return BorklerFuel.getBurnTime(stack.getFluid(), world) > 0;
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
		// if (validFuels.containsKey(fluid)) {
		if (BorklerFuel.getBurnTime(fluid, world) > 0) {
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
			gazcreations.borkler.Borkler.LOGGER
					.info("Fuel is " + fuel.getDisplayName().getString() + ": " + fuel.getAmount());
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
	 * Adds a {@link LazyOptional} to a {@link Set}, then adds a listener to that
	 * LazyOptional prompting it to be removed from the specified set upon
	 * invalidation.
	 * 
	 * @param <T>
	 * @param set
	 * @param element
	 * @return
	 */
	private <T> LazyOptional<T> addWithListener(Set<LazyOptional<T>> set, LazyOptional<T> element) {
		set.add(element);
		if (element == null) {
			return null;
		}
		element.addListener(new NonNullConsumer<LazyOptional<T>>() {
			@Override
			public void accept(LazyOptional<T> t) {
				gazcreations.borkler.Borkler.LOGGER
						.debug("A Borkler's supplier has been invalidated: " + t.toString() + "/" + element.toString());
				set.remove(element);
			}
		});
		return element;
	}

	public void updateItemConnections() {
		if (world.isRemote)
			return;
		Set<LazyOptional<IItemHandler>> consumers = new HashSet<LazyOptional<IItemHandler>>(7, 0.99f) {
			private static final long serialVersionUID = 1L;

			public boolean add(LazyOptional<IItemHandler> element) {
				if (element == null || !element.isPresent())
					return false;
				return super.add(element);
			}
		};
		gazcreations.borkler.Borkler.LOGGER
				.debug("Borkler @" + world + " ," + pos + " has been politely asked to update its item connections.");
		LazyOptional<IItemHandler> cap = null;
		TileEntity te = null;
		// Trigger warning: the following section may require subsequent use of
		// eyebleach.
		// up
		if ((te = this.world.getTileEntity(getPos().offset(Direction.UP))) != null) {
			cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN);
			// if (cap.isPresent()) override of Set.add will prevent empty Optionals from
			// being added
			addWithListener(consumers, cap);
		}
		// down
		if ((te = this.world.getTileEntity(getPos().offset(Direction.DOWN))) != null) {
			cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP);
			addWithListener(consumers, cap);
		}
		// east
		if ((te = this.world.getTileEntity(getPos().offset(Direction.EAST))) != null) {
			cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.WEST);
			addWithListener(consumers, cap);
		}
		// west
		if ((te = this.world.getTileEntity(getPos().offset(Direction.WEST))) != null) {
			cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.EAST);
			addWithListener(consumers, cap);
		}
		// north
		if ((te = this.world.getTileEntity(getPos().offset(Direction.NORTH))) != null) {
			cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.SOUTH);
			addWithListener(consumers, cap);
		}
		// south
		if ((te = this.world.getTileEntity(getPos().offset(Direction.SOUTH))) != null) {
			cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.NORTH);
			addWithListener(consumers, cap);
		}
		gazcreations.borkler.Borkler.LOGGER
				.debug("Borkler @" + world + " ," + pos + "has updated its item connections: " + consumers.toString());
		this.itemConnections = consumers;
	}

	/**
	 * This method runs a check on all sides of this TileEntity and populates this
	 * {@link BorklerTileEntity#fluidConnections} with a HashSet containing up to 6
	 * {@link IFluidHandler} instances. <br>
	 * The populated set is guaranteed not to be null and not to contain any null
	 * elements.
	 * 
	 */
	public void updateFluidConnections() {
		if (world.isRemote)
			return;
		Set<LazyOptional<IFluidHandler>> consumers = new HashSet<LazyOptional<IFluidHandler>>(7, 0.99f) {
			private static final long serialVersionUID = 1L;

			public boolean add(LazyOptional<IFluidHandler> element) {
				if (element == null || !element.isPresent())
					return false;
				return super.add(element);
			}
		};
		gazcreations.borkler.Borkler.LOGGER
				.debug("Borkler @" + world + " ," + pos + " has been politely asked to update its fluid connections.");
		gazcreations.borkler.Borkler.LOGGER.debug("Current connections are: " + this.fluidConnections.toString());
		LazyOptional<IFluidHandler> cap = null;
		TileEntity te = null;
		// Trigger warning: the following section may require subsequent use of
		// eyebleach.
		// up
		if ((te = this.world.getTileEntity(getPos().offset(Direction.UP))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.DOWN);
			// if (cap.isPresent()) override of Set.add will prevent empty Optionals from
			// being added
			addWithListener(consumers, cap);
		}
		// down
		if ((te = this.world.getTileEntity(getPos().offset(Direction.DOWN))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.UP);
			addWithListener(consumers, cap);
		}
		// east
		if ((te = this.world.getTileEntity(getPos().offset(Direction.EAST))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.WEST);
			addWithListener(consumers, cap);
		}
		// west
		if ((te = this.world.getTileEntity(getPos().offset(Direction.WEST))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.EAST);
			addWithListener(consumers, cap);
		}
		// north
		if ((te = this.world.getTileEntity(getPos().offset(Direction.NORTH))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.SOUTH);
			addWithListener(consumers, cap);
		}
		// south
		if ((te = this.world.getTileEntity(getPos().offset(Direction.SOUTH))) != null) {
			cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.NORTH);
			addWithListener(consumers, cap);
		}
		gazcreations.borkler.Borkler.LOGGER
				.debug("Borkler @" + world + " ," + pos + "has updated its connections: " + consumers.toString());
		this.fluidConnections = consumers;
	}

	/**
	 * This function will scour the connected {@link IFluidHandler}s to see which
	 * ones can accept steam, and appropriately adjust the steam output.<br>
	 * The Boiler will attempt to evenly distribute its steam to all consumers.
	 */
	private void distributeSteam() {
		Set<LazyOptional<IFluidHandler>> consumers = new HashSet<>(7, .99f);
		for (LazyOptional<IFluidHandler> dest : fluidConnections) {
			if (dest.isPresent()) {
				IFluidHandler handler = dest.orElse(null);
				if (handler != null) {
					int amount = handler.fill(steam, FluidAction.SIMULATE);
					if (amount > 0) {
						// oh look, we can output steam!
						addWithListener(consumers, dest);
					}
				}
			}
		}
		if (consumers.size() < 1)
			return; // nobody to send steam to =(
		int amount = this.steam.getAmount() / consumers.size();
		for (LazyOptional<IFluidHandler> dest : consumers) {
			if (dest.isPresent()) {
				pushSteam(dest.orElse(null), amount);
			}
		}
	}

	/**
	 * This method will attempt to output steam to a connected IFluidHandler.
	 * 
	 * @param destination
	 * @param steamAmount
	 */
	private void pushSteam(IFluidHandler destination, int steamAmount) {
		if (destination == null) // just checking
			return;
		int amount = destination.fill(new FluidStack(steam.getFluid(), steamAmount), FluidAction.SIMULATE);
		if (amount > 0) {
			// oh look, we can output steam!
			destination.fill(drain(Math.min(steamAmount, amount), FluidAction.EXECUTE), FluidAction.EXECUTE);
			// this.drain will invoke markDirty(), so we're good
		}

	}

	/**
	 * Will attempt to pull a compatible fluid (i.e. water or fuel) from a connected
	 * {@link IFluidHandler}.
	 * 
	 * @param source
	 */
	public void pullFluid(IFluidHandler source) {
		if (!BorklerConfig.CONFIG.THIRSTY.get())
			return;
		// a hard check for fluid validity
		boolean isValidSource = false;
		for (int i = 0; i < source.getTanks(); i++) {
			Fluid fluid = source.getFluidInTank(i).getFluid();
			if (fluid == Fluids.EMPTY)
				isValidSource = true; // in order for this to work with pipes, which drain on-demand, EMPTY has to be
										// a valid fluid
			byte tank = getTankForFluid(fluid);
			if (tank >= 0 && tank != 2)
				isValidSource = true;
		}
		if (!isValidSource)
			return;
		// try to pull water
		FluidStack toDrain = new FluidStack(Fluids.WATER, getTankCapacity(0) - water.getAmount());
		FluidStack drained = source.drain(toDrain, FluidAction.SIMULATE);
		if (drained.getFluid().isEquivalentTo(Fluids.WATER) && drained.getAmount() > 0) {
			if (water.isEmpty())
				water = new FluidStack(Fluids.WATER, 0);
			water.grow(source.drain(toDrain, FluidAction.EXECUTE).getAmount());
			markDirty();
			// water has been pulled!
		}
		// try to pull fuel
		if (!fuel.isEmpty()) {
			toDrain = new FluidStack(fuel.getFluid(), getTankCapacity(1) - fuel.getAmount());
			drained = source.drain(toDrain, FluidAction.SIMULATE);
			// extra check just in case
			if (!fuel.isFluidEqual(drained)) // the incoming fluid must be the same as the current fuel
				return;
			if (drained.getAmount() > 0) {
				fuel.grow(source.drain(toDrain, FluidAction.EXECUTE).getAmount());
				markDirty();
				// fuel has been pulled!
			}
		}
		if (fuel.isEmpty()) {
			// first we simulate filling the entire tank with fuel
			toDrain = source.drain(getTankCapacity(1), FluidAction.SIMULATE);
			if (!isFluidValid(1, toDrain)) {
				// whatever this is, it's not fuel
				return;
			}
			if (toDrain.getAmount() > 0) {
				fuel = new FluidStack(toDrain.getFluid(), 0);
				fuel.grow(source.drain(toDrain, FluidAction.EXECUTE).getAmount());
				markDirty();
				// fuel has been pulled!
			}
		}
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
		if (steam.getAmount() < drained) {
			drained = steam.getAmount();
		}
		FluidStack stack = new FluidStack(steam, drained);
		if (action.execute() && drained > 0) {
			steam.shrink(drained);
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
	public Container createMenu(int id, PlayerInventory playerInv, PlayerEntity player) {
		BorklerContainer menu = new BorklerContainer(id, playerInv, this.solidFuel, getPos());
		return menu;
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

	@Override
	public void onDataPacket(NetworkManager man, SUpdateTileEntityPacket s) {
		super.onDataPacket(man, s);
		if (world.isRemote) { // just checking we're clientside
			this.readCustomData(s.getNbtCompound());
		}
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(pos, 0, writeCustomData());
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
		if (world.isRemote()) // server side only.
			return;
		// first things first: if this boiler is set to auto-input fluids, it will try
		// to do so before anything else.
		if (BorklerConfig.CONFIG.THIRSTY.get()) {
			// will check its connections for a water supply
			for (LazyOptional<IFluidHandler> supplier : fluidConnections) {
				if (supplier.isPresent()) {
					// found a supplier!
					IFluidHandler f = supplier.orElse(null);
					if (f != null)
						pullFluid(f);
				}
			}
		}
		distributeSteam();
		mainMethod: {
			// the boiler will refuse to operate if it has no water.
			if (water.isEmpty()) {
				setActive(false);
				break mainMethod;
			}
			if (steam.getAmount() == getTankCapacity(2)) {
				// the boiler will refuse to operate if its steam tank is full.
				setActive(false);
				break mainMethod;

			}
			if (burnTime > 0) {
				// so, we have water, the boiler is active, and the steam tank is not full. It's
				// boiling time, boyos.
				setActive(true);
				boil();

			} else {
				burnTime = 0; // just in case
				// OK, so we're not currently burning fuel. Is there anything to burn, though?
				if (solidFuel.isEmpty() && fuel.isEmpty()) {
					// there's no liquid or solid fuel present. Boiler will be turned off.
					setActive(false);
					break mainMethod;
				}
				if (!solidFuel.isEmpty()) {
					// there's solid fuel in the burner. Let's try to burn this.
					// int additionalBurnTime = ForgeHooks.getBurnTime(getStackInSlot(0));
					int additionalBurnTime = nerfdBurnTime(getStackInSlot(0));
					if (additionalBurnTime > 0) {
						// burnTime += ForgeHooks.getBurnTime(decrStackSize(0, 1));
						burnTime += nerfdBurnTime(decrStackSize(0, 1));
						setActive(true);
						break mainMethod;
					}
				}
				if (!fuel.isEmpty()) {
					// ok, there is liquid fuel in the boiler. We'll try to burn this.
					int bitOFuel = Math.min(fuel.getAmount(), 5);
					// int addBurnTime = validFuels.getInt(fuel.getFluid()) * bitOFuel;
					int addBurnTime = BorklerFuel.getBurnTime(fuel.getFluid(), world) * bitOFuel;
					fuel.shrink(bitOFuel);
					burnTime += addBurnTime;
					setActive(true);
					break mainMethod;
					/*
					 * if (fuel.getFluid().isIn(FluidTags.LAVA)) { // Determining burn time for lava
					 * is tricky. Let's use the lava bucket, or 1000 // mB, as reference. int
					 * bitOFuel = Math.min(fuel.getAmount(), 5); int additionalBurnTime = bitOFuel *
					 * 20; fuel.shrink(bitOFuel); burnTime += additionalBurnTime; setActive(true);
					 * break mainMethod; } if
					 * (fuel.getFluid().isIn(net.minecraftforge.common.Tags.Fluids.MILK)) {
					 * 
					 * }
					 */
				}
			}
		} // mainMethod label ends here. Now we run our dirtyness check.
		ticksSinceLastClientUpdate++;
	}

	/**
	 * This function tries to drain a configurable amount of water from the water
	 * tank and add a corresponding amount of Steam, corrected by the
	 * 'conversion_rate' config setting, to the output tank. It is run on every tick
	 * while the boiler is active, and decreases burnTime by one tick. <br>
	 * If less than that configurable amount of water is present, it will boil
	 * whatever's left. <br>
	 * Any excess steam is discarded.
	 */
	private final void boil() {
		int amount = Math.min(this.water.getAmount(), BorklerConfig.CONFIG.WATER_USE.get());
		this.water.shrink(amount);
		this.steam.grow(Math.toIntExact(Math.round(amount * BorklerConfig.CONFIG.CONVERSION_RATE.get())));
		if (steam.getAmount() > getTankCapacity(2))
			steam.setAmount(getTankCapacity(2));
		burnTime--;
		markDirty();
	}

	/**
	 * Writes this BorklerTileEntity's specific data to the designated CompoundNBT,
	 * from {@link TileEntity#getTileData()}.
	 */
	private CompoundNBT writeCustomData() {
		CompoundNBT stuff = getTileData();
		stuff.putByte("tier", tier);
		stuff.putInt("steam", this.steam.getAmount());
		stuff.putInt("water", this.water.getAmount());
		stuff.putString("fuelType", fuel.getFluid().getRegistryName().toString());
		stuff.putInt("fuelAmount", this.fuel.getAmount());
		stuff.putString("solidFuelType", this.solidFuel.getStackInSlot(0).getItem().getRegistryName().toString());
		stuff.putInt("solidFuelAmount", this.solidFuel.getStackInSlot(0).getCount());
		stuff.putBoolean("isActive", isActive);
		stuff.putInt("burnTime", this.burnTime);
		return stuff;
	}

	private void readCustomData(CompoundNBT nbt) {
		if (nbt.isEmpty()) {
			gazcreations.borkler.Borkler.LOGGER.debug("Empty ForgeData tag means something went wrong while saving.");
			return;
		}
		tier = nbt.getByte("tier");
		steam = new FluidStack(Index.Fluids.STEAMSOURCE, nbt.getInt("steam"));
		water = new FluidStack(Fluids.WATER, nbt.getInt("water"));
		fuel = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(nbt.getString("fuelType"))),
				nbt.getInt("fuelAmount"));
		solidFuel.setInventorySlotContents(0,
				new ItemStack(
						() -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("solidFuelType"))),
						nbt.getInt("solidFuelAmount")));
		boolean act = nbt.getBoolean("isActive");
		setActive(act);
		burnTime = nbt.getInt("burnTime");
	}

	/**
	 * Writes this TileEntity's data, such as its state and contents, to NBT tags,
	 * for persistence.
	 */
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		writeCustomData();
		return super.write(nbt);
	}

	/**
	 * Populates this TileEntity's fields with values stored in an NBT.
	 */
	@Override
	public void read(BlockState state, CompoundNBT nbtTag) {
		super.read(state, nbtTag);
		readCustomData(nbtTag.getCompound("ForgeData"));
		if (this.world != null)
			updateFluidConnections();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (world != null && !world.isRemote()) {
			addFutureServerTask(world, () -> updateFluidConnections(), true);
			addFutureServerTask(world, () -> updateItemConnections(), true);
			addFutureServerTask(world, () -> this.updateContainingBlockInfo(), true);
		}
	}

	/**
	 * This function was copied from the IE code because running
	 * updateFluidConnections in onLoad would cause Minecraft to hang indefinitely.
	 * Let's see if this fixes it. EDIT: it does. Genius.
	 * 
	 * @author BluSunrize of Immersive Engineering.
	 * @param world
	 * @param task
	 * @param forceFuture
	 */
	public static void addFutureServerTask(World world, Runnable task, boolean forceFuture) {
		LogicalSide side = world.isRemote ? LogicalSide.CLIENT : LogicalSide.SERVER;
		ThreadTaskExecutor<? super TickDelayedTask> tmp = LogicalSidedProvider.WORKQUEUE.get(side);
		if (forceFuture) {
			int tick;
			if (world.isRemote)
				tick = 0;
			else
				tick = ((MinecraftServer) tmp).getTickCounter();
			tmp.enqueue(new TickDelayedTask(tick, task));
		} else
			tmp.deferTask(task);
	}

	/**
	 * Enables this TileEntity to interact with pipes and inventories in-world, for
	 * automation purposes.
	 */
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @javax.annotation.Nullable Direction side) {
		if (this.isRemoved())
			return LazyOptional.empty();
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			if (fluidHandlerCapability == null || !fluidHandlerCapability.isPresent()) {
				fluidHandlerCapability = LazyOptional.of(() -> this);
			}
			return fluidHandlerCapability.cast();
		}
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			if (itemHandlerCapability == null || !itemHandlerCapability.isPresent()) {
				itemHandlerCapability = LazyOptional.of(() -> this);
			}
			return itemHandlerCapability.cast();
		}
		return LazyOptional.empty();
	}

	/**
	 * Invalidates this entity's {@link BorklerTileEntity#fluidHandlerCapability}
	 * and {@link BorklerTileEntity#itemHandlerCapability}.
	 */
	@Override
	public void invalidateCaps() {
		if (fluidHandlerCapability != null) {
			fluidHandlerCapability.invalidate();
		}
		if (itemHandlerCapability != null) {
			itemHandlerCapability.invalidate();
		}
		super.invalidateCaps();
	}

	@Override
	public Container createMenu(int id, PlayerInventory player) {
		return this.createMenu(id, player, player.player);
	}

}
