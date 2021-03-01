package gazcreations.borkler;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Just a constructor. Nothing to see here, move along.
	 * <p>
	 * JK. Here we set listeners for the basic methods of the mod, and may do other
	 * stuff. WIP.
	 * </p>
	 */
	public Borkler() {
		// Register the setup method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		// Register the enqueueIMC method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		// Register the processIMC method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		// Register the doClientStuff method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
		ResourceLocation steam = new ResourceLocation("forge", "fluids/steam");
		FluidTags.createOptional(steam);
		LOGGER.debug(FluidTags.getAllTags().toString());
		LOGGER.debug(FluidTags.getCollection().getTagByID(steam).contains(Index.Fluids.STEAMSOURCE));
	}

	/**
	 * I suppose this would be where we get our tags and config settings?
	 * 
	 * @param event
	 */
	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		LOGGER.info("HELLO FROM PREINIT");
		LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		// do something that can only be done on the client
		//LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
	}

	private void enqueueIMC(final InterModEnqueueEvent event) {
		// some example code to dispatch IMC to another mod
		InterModComms.sendTo("borkler", "helloworld", () -> {
			LOGGER.info("Hello world from the MDK");
			return "Hello world";
		});
	}

	private void processIMC(final InterModProcessEvent event) {
		// some example code to receive and process InterModComms from other mods
		LOGGER.info("Got IMC {}",
				event.getIMCStream().map(m -> m.getMessageSupplier().get()).collect(Collectors.toList()));
	}

	// You can use SubscribeEvent and let the Event Bus discover methods to call
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
	 * fluids and items associated with Borkler. Their names are pretty self-explanatory.
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
	}
}