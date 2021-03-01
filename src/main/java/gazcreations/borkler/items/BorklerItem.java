package gazcreations.borkler.items;

import gazcreations.borkler.Index;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;

public class BorklerItem extends BlockItem {

	public BorklerItem() {
		super(Index.Blocks.BORKLERBLOCK,
				new Properties().group(ItemGroup.REDSTONE).maxStackSize(64));
		this.setRegistryName("borkler","steam_boiler");
	}

}
