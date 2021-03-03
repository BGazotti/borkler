package gazcreations.borkler.container;

import gazcreations.borkler.Index;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;

public class BorklerContainer extends Container {

	private IInventory inventory;

	public BorklerContainer(int id, PlayerInventory playerInv) {
		this(id, playerInv, new Inventory(1));
	}
	
	public BorklerContainer(int id, PlayerInventory playerInv, IInventory inventory) {
		super(Index.BORKLER_CONTAINER_TYPE, id);
		this.inventory = inventory;
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

}
