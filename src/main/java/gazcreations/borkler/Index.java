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

package gazcreations.borkler;

import gazcreations.borkler.blocks.BorklerBlock;
import gazcreations.borkler.blocks.BorklerTileEntity;
import gazcreations.borkler.container.BorklerContainer;
import gazcreations.borkler.fluids.Steam;
import gazcreations.borkler.fluids.SteamBlock;
import gazcreations.borkler.items.BorklerItem;
import gazcreations.borkler.items.SteamItem;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.ObjectHolder;

/**
 * An Index class, containing static references to Borkler's blocks, fluids,
 * items, entities, containers, you name it.
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

	@SuppressWarnings("unchecked")
	private static <T extends Container> ContainerType<T> register(String name, IContainerFactory<T> containerFactory) {
		return (ContainerType<T>) new ContainerType<>(containerFactory).setRegistryName("borkler", name);
	}

	@ObjectHolder(value = "borkler:borklercontainer")
	public static final ContainerType<?> BORKLER_CONTAINER_TYPE = register("borklercontainer",
			((windowId, inv, data) -> {
				return new BorklerContainer(windowId, inv, data);
			}));
}
