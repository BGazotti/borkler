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

import static net.minecraftforge.fml.loading.LogMarkers.FORGEMOD;

import java.util.HashSet;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gazcreations.borkler.proxy.ClientProxy;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Welcome to Borkler!
 * <p>
 * This is an attempt at providing a simple steam boiler, capable of operating
 * on both liquid and solid fuel, for use with other mods that provide steam
 * turbines. Yes, I'm looking at you, @ZeroNoRyouki. Love your work.
 * </p>
 * <p>
 * I will try to keep this as well-documented as possible, to make it easier for
 * anyone reading my spaghetti code to point out what I'm doing wrong, as well
 * as suggest improvements or new features. And also because.
 * </p>
 * 
 * @author gazotti
 *
 */
@Mod("borkler")
public class Borkler {
	// Directly reference a log4j logger.
	public static final Logger LOGGER = LogManager.getLogger();

	public final IEventBus BUS;

	/**
	 * Just a constructor. Nothing to see here, move along.
	 * <p>
	 * JK. Here we set listeners for the basic methods of the mod, and may do other
	 * stuff. WIP.
	 * </p>
	 */
	public Borkler() {
		BUS = FMLJavaModLoadingContext.get().getModEventBus();
		// Register the setup method for modloading
		BUS.addListener(this::setup);
		// Register the enqueueIMC method for modloading
		BUS.addListener(this::enqueueIMC);
		// Register the processIMC method for modloading
		BUS.addListener(this::processIMC);
		// Registers methods for running on the client only
		DistExecutor.safeRunWhenOn(Dist.CLIENT, new ClientProxy());
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

	}

	/**
	 * This method sets up some more stuff, such as enforcing the creation of a tag
	 * for steam.
	 * 
	 * @param event
	 */
	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		ResourceLocation steam = new ResourceLocation("forge", "fluids/steam");
		HashSet<Supplier<Fluid>> steamTypes = new HashSet<Supplier<Fluid>>(2);
		steamTypes.add(() -> Index.Fluids.STEAM);
		steamTypes.add(() -> Index.Fluids.STEAMSOURCE);
		FluidTags.createOptional(steam, steamTypes);
		LOGGER.fatal(BorklerConfig.CONFIG.WATER_USE.get());
	}

	private void enqueueIMC(final InterModEnqueueEvent event) {
		// some example code to dispatch IMC to another mod
		// InterModComms.sendTo("borkler", "helloworld", () -> {
		// LOGGER.info("Hello world from the MDK");
		// return "Hello world";});

	}

	private void processIMC(final InterModProcessEvent event) {
		// some example code to receive and process InterModComms from other mods
		// LOGGER.info("Got IMC {}",
		// event.getIMCStream().map(m ->
		// m.getMessageSupplier().get()).collect(Collectors.toList()));
	}

	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		// do something when the server starts
		LOGGER.info("Fire up those turbines! This server is Borkler-powered.");
	}

	/**
	 * Fire up those registries!
	 * <p>
	 * A class containing methods that are called when Forge starts registering
	 * stuff. The methods in this class are responsible for registering blocks,
	 * fluids, items and entities associated with Borkler. Their names are pretty
	 * self-explanatory.
	 * </p>
	 * 
	 * @author gazotti
	 *
	 */
	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents {
		@SubscribeEvent
		public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
			LOGGER.info("Registering blocks!");
			blockRegistryEvent.getRegistry().registerAll(Index.Blocks.BORKLERBLOCK, Index.Blocks.STEAM);
		}

		@SubscribeEvent
		public static void onFluidsRegistry(final RegistryEvent.Register<Fluid> fluidRegistryEvent) {
			LOGGER.info("Registering fluids!");
			fluidRegistryEvent.getRegistry().registerAll(Index.Fluids.STEAM, Index.Fluids.STEAMSOURCE);
		}

		@SubscribeEvent
		public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
			LOGGER.info("Registering items!");
			itemRegistryEvent.getRegistry().registerAll(Index.Items.BORKLERITEM, Index.Items.STEAMITEM);
		}

		@SubscribeEvent
		public static void onEntitiesRegistry(final RegistryEvent.Register<TileEntityType<?>> event) {
			LOGGER.info("Registering TileEntities!");
			event.getRegistry().register(Index.BORKLER_TE_TYPE);
		}

		@SubscribeEvent
		public static void onContainersRegistry(final RegistryEvent.Register<ContainerType<?>> event) {
			LOGGER.info("Registering ContainerTypes!");
			event.getRegistry().register(Index.BORKLER_CONTAINER_TYPE);
		}
	}
}
