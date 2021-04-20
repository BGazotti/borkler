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

package gazcreations.borkler.container;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import gazcreations.borkler.Index;
import gazcreations.borkler.blocks.BorklerTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

public class BorklerContainer extends Container {

	private IInventory borklerInventory;
	private List<Pair<FluidStack, Integer>> tanksWithCapacity;
	private BlockPos tileEntityPos; //
	private BorklerTileEntity borklerTE;

	/**
	 * This constructor is called on the client when the Boiler is right-clicked.
	 * 
	 * @param id
	 * @param playerInv
	 * @param data
	 */
	public BorklerContainer(int id, PlayerInventory playerInv, PacketBuffer data) {
		/*
		 * Note: this 'new Inventory(1)' is a dummy call. The called constructor will
		 * use the boiler's position (data.readBlockPos()) to retrieve the actual
		 * BorklerTileEntity's inventory.
		 */
		this(id, playerInv, new Inventory(1), data.readBlockPos());
	}

	/**
	 * Initializes the Borkler Container with its internal inventory, as well as the
	 * player's, and an associated Borkler's data. This constructor is called on the server.
	 * 
	 * @param id
	 * @param playerInv
	 * @param inventory an unused parameter. I might even remove it later.
	 * @param pos The boiler's position
	 */
	public BorklerContainer(int id, PlayerInventory playerInv, IInventory inventory,
			BlockPos pos) {
		super(Index.BORKLER_CONTAINER_TYPE, id);
		if (pos != null) {
			this.borklerInventory = ((BorklerTileEntity) playerInv.player.level.getBlockEntity(pos)).getInventory();
			gazcreations.borkler.Borkler.LOGGER.debug("Workaround implemented.");
		} else
			this.borklerInventory = inventory;
		borklerInventory.startOpen(playerInv.player);
		addSlot(new Slot(borklerInventory, 0, 24, 34) {

			@Override
			public boolean mayPlace(ItemStack stack) {
				boolean valid = borklerInventory.canPlaceItem(this.getSlotIndex(), stack);
				return valid;
			}

			@Override
			public void set(ItemStack stack) {
				if (this.mayPlace(stack))
					super.set(stack);
			}
		});
		// 137, 106 -> 133, 113: -4, +7
		// 12,68 is the first player slot
		// 12,126 is the player's first hotbar slot
		int leftCol = 8;
		for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
			for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
				addSlot(new Slot(playerInv, playerInvCol + playerInvRow * 9 + 9, leftCol + playerInvCol * 18,
						158 - (4 - playerInvRow) * 18 - 10));
			}

		}
		for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			addSlot(new Slot(playerInv, hotbarSlot, leftCol + hotbarSlot * 18, 134));
		}
		if (pos != null) {
			this.tileEntityPos = pos;
			this.borklerTE = (BorklerTileEntity) playerInv.player.level.getBlockEntity(tileEntityPos);
			if (borklerTE != null) {
				gazcreations.borkler.Borkler.LOGGER.debug(this.getClass() + ": BorklerTE found");
				this.tanksWithCapacity = new ArrayList<Pair<FluidStack, Integer>>(4);
				for (int i = 0; i <= 2; i++) {
					this.tanksWithCapacity.add(Pair.of(borklerTE.getFluidInTank(i), borklerTE.getTankCapacity(i)));
				}
			}
		}
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		if (index == 0) { // Borkler Special Slot. Running extra checks.
			if (!getSlot(0).mayPlace(stack))
				return;
		}
		super.setItem(index, stack);
	}

	/**
	 * Handles shift+click logic.
	 */
	@Override
	public ItemStack quickMoveStack(PlayerEntity playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < borklerInventory.getContainerSize()) {
				if (!this.moveItemStackTo(itemstack1, borklerInventory.getContainerSize(), this.slots.size(),
						true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemstack1, 0, borklerInventory.getContainerSize(), false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return itemstack;
	}

	public final List<Pair<FluidStack, Integer>> getTanks() {
		return this.tanksWithCapacity;
	}

	public void setTanks(List<Pair<FluidStack, Integer>> tanks) {
		this.tanksWithCapacity = tanks;
	}

	public BlockPos getTileEntityPos() {
		return tileEntityPos;
	}

	public void setTileEntityPos(BlockPos tileEntityPos) {
		this.tileEntityPos = tileEntityPos;
	}

	public BorklerTileEntity getTileEntity() {
		return this.borklerTE;
	}

	@Override
	public boolean stillValid(PlayerEntity p_75145_1_) {
		return this.borklerInventory.stillValid(p_75145_1_);
	}
}
