package gazcreations.borkler.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;

public class BorklerContainer extends Container {

	private IInventory inventory;

	public BorklerContainer(ContainerType<?> type, int id, PlayerInventory playerInv, IInventory inventory) {
		super(type, id);
		this.inventory = inventory;
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onContainerClosed(PlayerEntity playerIn) {
		super.onContainerClosed(playerIn);
		this.inventory.closeInventory(playerIn);
	}

}
