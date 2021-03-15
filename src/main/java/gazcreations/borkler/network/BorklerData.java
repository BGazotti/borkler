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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import gazcreations.borkler.container.BorklerContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.network.NetworkEvent.Context;

/**
 * A simplified class, containing the fluid and tier data for a given Steam
 * Boiler.
 * 
 * @author gazotti
 *
 */
public class BorklerData {

	@Nonnull
	private List<Pair<FluidStack, Integer>> tanks;
	private byte tier;

	public BorklerData(List<Pair<FluidStack, Integer>> tanks, byte tier) {
		this.tanks = tanks;
		this.tier = tier;
	}

	public List<Pair<FluidStack, Integer>> getTanks() {
		return tanks;
	}

	public void setTier(byte tier) {
		this.tier = tier;
	}
	
	public byte getTier() {
		return tier;
	}

	/**
	 * BorklerData is as follows: byte(tier), 3x Pair <fluidStack(fluids),
	 * int(capacity)> <br>
	 * The Pairs are written as separate FluidStack and Integer values.
	 * 
	 * @param data   BorklerData object to encode
	 * @param packet The PacketBuffer to write to
	 */
	public static void encode(BorklerData data, PacketBuffer packet) {
		packet.writeByte(data.getTier());
		for (Pair<FluidStack, Integer> p : data.getTanks()) {
			packet.writeFluidStack(p.getKey());
			packet.writeInt(p.getValue());
		}
	}

	/**
	 * Returns a BorklerData object from a provided PacketBuffer.
	 * 
	 * @param packet The buffer to read from
	 * @return a BorklerData object constructed from the PacketBuffer
	 */
	public static BorklerData decode(PacketBuffer packet) {
		ArrayList<Pair<FluidStack, Integer>> tanks = new ArrayList<>(4);
		byte tier = packet.readByte();
		while (packet.isReadable()) {
			tanks.add(Pair.of(packet.readFluidStack(), packet.readInt()));
		}
		return new BorklerData(tanks, tier);
	}

	/**
	 * This is where the magic happens.
	 * 
	 * @param data
	 * @param context
	 */
	public static void handlePacket(BorklerData data, Supplier<Context> contextSupplier) {
		Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Container container = Minecraft.getInstance().player.openContainer;
			gazcreations.borkler.Borkler.LOGGER.debug("player is " + Minecraft.getInstance().player.getScoreboardName());
			gazcreations.borkler.Borkler.LOGGER.debug("container is " + container.toString());
			if (container != null && container instanceof BorklerContainer) { //TODO for some reason the container is not a BorklerContainer?
				BorklerContainer bc = (BorklerContainer) container;
				bc.setTanks(data.tanks);
			}
		});
		context.setPacketHandled(true);
	}
}
