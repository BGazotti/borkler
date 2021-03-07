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
