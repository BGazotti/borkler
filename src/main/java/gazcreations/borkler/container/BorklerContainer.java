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

	private IInventory inventory;

	public BorklerContainer(int id, PlayerInventory playerInv) {
		this(id, playerInv, new Inventory(1));
	}

	/**
	 * Initializes the Borkler Container with its internal inventory, as well as the
	 * player's.
	 * 
	 * @param id
	 * @param playerInv
	 * @param inventory
	 */
	public BorklerContainer(int id, PlayerInventory playerInv, IInventory inventory) {
		super(Index.BORKLER_CONTAINER_TYPE, id);
		this.inventory = inventory;
		super.addSlot(new Slot(inventory, 0, 28, 27));
		// 12,68 is the first player slot
		// 12,126 is the player's first hotbar slot
		int leftCol = 12;
		for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
			for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
				this.addSlot(new Slot(playerInv, playerInvCol + playerInvRow * 9 + 9, leftCol + playerInvCol * 18,
						151 - (4 - playerInvRow) * 18 - 10));
			}

		}
		for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			this.addSlot(new Slot(playerInv, hotbarSlot, leftCol + hotbarSlot * 18, 127));
		}

	}

	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return inventory.isUsableByPlayer(playerIn);
	}

	@Override
	public void onContainerClosed(PlayerEntity playerIn) {
		super.onContainerClosed(playerIn);
		this.inventory.closeInventory(playerIn);
	}

	
	/**
	 *TODO shift click is crashing and that might be it
	 */
	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
	      Slot slot = this.inventorySlots.get(index);
	      return slot != null ? slot.getStack() : ItemStack.EMPTY;
	   }

}
