package gazcreations.borkler.container;

import gazcreations.borkler.Index;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class BorklerContainer extends Container {

	private IInventory borklerInventory;

	public BorklerContainer(int id, PlayerInventory playerInv) {
		this(id, playerInv, new Inventory(1));
	}

	/**
	 * Initializes the Borkler Container with its internal inventory, as well as the
	 * player's.
	 * 
	 * @param id
	 * @param playerInv
	 * @param Ideally,  a BorklerTileEntity
	 */
	public BorklerContainer(int id, PlayerInventory playerInv, IInventory inventory) {
		super(Index.BORKLER_CONTAINER_TYPE, id);
		this.borklerInventory = inventory;
		borklerInventory.openInventory(playerInv.player);
		addSlot(new Slot(inventory, 0, 28, 27) {
			@Override
			public void onSlotChanged() {
				super.onSlotChanged();
				gazcreations.borkler.Borkler.LOGGER
						.debug("The Borkler Slot has changed. Content: " + this.getStack().toString());
			}

			@Override
			public boolean isItemValid(ItemStack stack) {
				return inventory.isItemValidForSlot(this.getSlotIndex(), stack);
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
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		this.putStackInSlot(index, itemstack);
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

}
