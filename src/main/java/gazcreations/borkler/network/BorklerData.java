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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.network.NetworkEvent.Context;

/**
 * A simplified class, containing the fluid and position data for a given Steam
 * Boiler.<br>
 * Used in networking.
 * 
 * @author gazotti
 *
 */
public class BorklerData {

	@Nonnull
	private BlockPos pos;
	@Nonnull
	private List<Pair<FluidStack, Integer>> tanks;

	@Deprecated
	public BorklerData(BlockPos pos, List<Pair<FluidStack, Integer>> tanks) {
		this.pos = pos;
		this.tanks = tanks;
	}

	public List<Pair<FluidStack, Integer>> getTanks() {
		return tanks;
	}

	public void setPos(BlockPos pos) {
		this.pos = pos;
	}

	public BlockPos getPos() {
		return pos;
	}

	/**
	 * BorklerData is as follows: BlockPos, 3x (fluidStack, int(capacity)) <br>
	 * 
	 * @param data   BorklerData object to encode
	 * @param packet The PacketBuffer to write to
	 * @deprecated
	 */
	public static void encode(BorklerData data, PacketBuffer packet) {
		packet.writeBlockPos(data.getPos());
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
		BlockPos pos = packet.readBlockPos();
		while (packet.isReadable()) {
			tanks.add(Pair.of(packet.readFluidStack(), packet.readInt()));
		}
		return new BorklerData(pos, tanks);
	}

	public static void encodePos(BlockPos pos, PacketBuffer packet) {
		packet.writeBlockPos(pos);
	}

	public static CompoundNBT decodeToNBT(PacketBuffer packet) {
		return null;
	}

	// TODO the switch will be made. Need the full CompoundNBT, not just custom
	// tags.
	public static void encode(CompoundNBT data, PacketBuffer packet) {
		packet.writeCompoundTag(data);
	}

	/**
	 * This is where the magic happens. TODO implement. Magic is not happening yet.
	 * 
	 * @param data
	 * @param context
	 */
	@SuppressWarnings("resource")
	@Deprecated
	public static void handlePacket(BorklerData data, Supplier<Context> contextSupplier) {
		Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Container container = Minecraft.getInstance().player.openContainer;
			if (container != null && container instanceof BorklerContainer) {
				BorklerContainer bc = (BorklerContainer) container;
				bc.setTanks(data.tanks);
			}
		});
		context.setPacketHandled(true);
	}
}
