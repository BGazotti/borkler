package gazcreations.borkler;

import gazcreations.borkler.blocks.BorklerBlock;
import gazcreations.borkler.blocks.Steam;
import gazcreations.borkler.blocks.SteamBlock;
import gazcreations.borkler.container.BorklerContainer;
import gazcreations.borkler.entities.BorklerTileEntity;
import gazcreations.borkler.items.BorklerItem;
import gazcreations.borkler.items.SteamItem;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

/**
 * An Index class, containing references to Borkler's blocks, fluids and items.
 * 
 * @author gazotti
 *
 */
@SuppressWarnings("deprecation")
public abstract class Index {

	public static final class Blocks {
		@ObjectHolder(value = "borkler:steam_boiler")
		public static final BorklerBlock BORKLERBLOCK = new BorklerBlock();
		@ObjectHolder(value = "borkler:steam_source")
		public static final SteamBlock STEAM = new SteamBlock();

	}

	public static final class Fluids {

		@ObjectHolder(value = "borkler:steam_flowing")
		public static final Steam.Flowing STEAM = new Steam.Flowing();
		@ObjectHolder(value = "borkler:steam_source")
		public static final Steam.Source STEAMSOURCE = new Steam.Source();
	}

	public static final class Items {
		@ObjectHolder(value = "borkler:steam_boiler")
		public static final BorklerItem BORKLERITEM = new BorklerItem();
		@ObjectHolder(value = "borkler:steam_source")
		public static final SteamItem STEAMITEM = new SteamItem();
	}

	@ObjectHolder(value = "borkler:borklertile")
	public static final TileEntityType<?> BORKLER_TE_TYPE = TileEntityType.Builder
			.create(BorklerTileEntity::new, Index.Blocks.BORKLERBLOCK).build(null)
			.setRegistryName("borkler", "borklertile");

	@ObjectHolder(value="borkler:borklertile")
	public static final ContainerType<?> BORKLER_CONTAINER_TYPE = new ContainerType<BorklerContainer>(
			BorklerContainer::new).setRegistryName("borkler", "borklertile");
}
