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
package gazcreations.borkler.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.Items;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraftforge.common.Tags.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * This class is basically a {@link HashMap}<Fluid, Integer>, representing a
 * list of fuel types and their respective burn times (in ticks per mB).
 * 
 * @author gazotti
 *
 */
public class BorklerFluidList extends HashMap<Fluid, Integer> implements ITag<Fluid> {

	private static final long serialVersionUID = 194008857968480530L;

	public static BorklerFluidList getDefault() {
		BorklerFluidList def = new BorklerFluidList();
		for (Fluid fluid : FluidTags.LAVA.getAllElements())
			def.put(fluid, 20);
		return def;
	}

	protected BorklerFluidList() {
		super(3);
	}

	public BorklerFluidList(Map<? extends Fluid, ? extends Integer> map) {
		super(map);
	}

	public static void encode(@Nonnull BorklerFluidList data, @Nonnull PacketBuffer packet) {
		for (Entry<Fluid, Integer> fluid : data.entrySet()) {
			packet.writeFluidStack(new FluidStack(fluid.getKey(), 1));
			packet.writeInt(fluid.getValue());
		}
	}

	@Nonnull
	public static BorklerFluidList decode(@Nonnull PacketBuffer packet) {
		BorklerFluidList fluids = new BorklerFluidList();
		while (packet.isReadable()) {
			fluids.put(packet.readFluidStack().getFluid(), packet.readInt());
		}
		return fluids;
	}

	@Override
	public boolean contains(Fluid element) {
		return this.containsKey(element);
	}

	@Override
	public List<Fluid> getAllElements() {
		return new java.util.ArrayList<>(this.keySet());
	}
}
