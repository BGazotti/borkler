package gazcreations.borkler.items;

import gazcreations.borkler.blocks.SteamBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;

/**
 * This should not even exist. Yet it does.
 * <p> Ah, the paradoxes of our time. </p>
 * @author gazotti
 *
 */
@Deprecated
public class SteamItem extends BlockItem {

	public SteamItem() {
		super(new SteamBlock(), new Properties().group(ItemGroup.MISC));
		this.setRegistryName("borkler","steam_source");
	}
}
