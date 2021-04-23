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

import gazcreations.borkler.fluids.SteamBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;

/**
 * This should not even exist. Yet it does.
 * <p>
 * Ah, the paradoxes of our time.
 * </p>
 * 
 * @author gazotti
 *
 */
@Deprecated
public class SteamItem extends BlockItem {

	public SteamItem() {
		super(new SteamBlock(), new Properties().group(ItemGroup.MISC));
		this.setRegistryName("borkler", "steam_source");
	}
}
