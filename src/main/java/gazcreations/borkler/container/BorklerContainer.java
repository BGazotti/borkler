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
			this.borklerInventory = ((BorklerTileEntity) playerInv.player.world.getTileEntity(pos)).getInventory();
			gazcreations.borkler.Borkler.LOGGER.debug("Workaround implemented.");
		} else
			this.borklerInventory = inventory;
		borklerInventory.openInventory(playerInv.player);
		addSlot(new Slot(borklerInventory, 0, 28, 27) {

			@Override
			public boolean isItemValid(ItemStack stack) {
				boolean valid = borklerInventory.isItemValidForSlot(this.getSlotIndex(), stack);
				return valid;
			}

			@Override
			public void putStack(ItemStack stack) {
				if (this.isItemValid(stack))
					super.putStack(stack);
			}
		});
		// 12,68 is the first player slot
		// 12,126 is the player's first hotbar slot
		int leftCol = 12;
		for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
			for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
				addSlot(new Slot(playerInv, playerInvCol + playerInvRow * 9 + 9, leftCol + playerInvCol * 18,
						151 - (4 - playerInvRow) * 18 - 10));
			}

		}
		for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			addSlot(new Slot(playerInv, hotbarSlot, leftCol + hotbarSlot * 18, 127));
		}
		if (pos != null) {
			this.tileEntityPos = pos;
			this.borklerTE = (BorklerTileEntity) playerInv.player.world.getTileEntity(tileEntityPos);
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
	public boolean canInteractWith(PlayerEntity playerIn) {
		return borklerInventory.isUsableByPlayer(playerIn);
	}

	@Override
	public void onContainerClosed(PlayerEntity playerIn) {
		super.onContainerClosed(playerIn);
		borklerInventory.closeInventory(playerIn);
	}

	@Override
	public void putStackInSlot(int index, ItemStack stack) {
		if (index == 0) { // Borkler Special Slot. Running extra checks.
			if (!getSlot(0).isItemValid(stack))
				return;
		}
		super.putStackInSlot(index, stack);
	}

	/**
	 * Handles shift+click logic.
	 */
	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (index < borklerInventory.getSizeInventory()) {
				if (!this.mergeItemStack(itemstack1, borklerInventory.getSizeInventory(), this.inventorySlots.size(),
						true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.mergeItemStack(itemstack1, 0, borklerInventory.getSizeInventory(), false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
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
}
