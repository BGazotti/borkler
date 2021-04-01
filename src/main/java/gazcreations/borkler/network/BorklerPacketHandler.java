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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * A packet handler, written in ForgeDocs style, to handle packets.
 * 
 * @author gazotti
 *
 */
public class BorklerPacketHandler {

	private static int id = 0;
	private static final String PROTOCOL_VERSION = "1";
	public static SimpleChannel INSTANCE;

	private BorklerPacketHandler() {

	}

	/**
	 * Opens a network channel for Borkler and registers the packet types associated
	 * with the mod.
	 * 
	 * @param ev
	 */
	@SubscribeEvent
	public static final void registerChannel(FMLCommonSetupEvent ev) {
		INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation("borkler", "main"), () -> PROTOCOL_VERSION,
				PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
		INSTANCE.registerMessage(id, BorklerData.class, BorklerData::encode, BorklerData::decode,
				BorklerData::handlePacket);
		id++;
		/*
		 * INSTANCE.registerMessage(id, BorklerFluidList.class,
		 * BorklerFluidList::encode, BorklerFluidList::decode,
		 * BorklerTileEntity::updateValidFuelList); id++;
		 */
	}

	public static final void sendToPlayer(PlayerEntity player, Object message) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), message);
	}

	public static final void sendToChunk(Chunk chunk, Object message) {
		INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
	}

	public static final void sendToAll(Object message) {
		INSTANCE.send(PacketDistributor.ALL.noArg(), message);
	}
}
