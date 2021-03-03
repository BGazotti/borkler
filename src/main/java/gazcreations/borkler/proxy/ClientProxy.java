package gazcreations.borkler.proxy;

import java.util.function.Supplier;

import gazcreations.borkler.Borkler;
import gazcreations.borkler.Index;
import gazcreations.borkler.client.screen.BorklerScreen;
import gazcreations.borkler.container.BorklerContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.DistExecutor.SafeRunnable;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * I find this a tad non-elegant and perhaps even unnecessary (maybe as I learn
 * more about how Minecraft and Forge work I'll change my mind), but apparently
 * it's good practice to have a separate class to run client-only methods.
 * 
 * @author kili
 *
 */
public class ClientProxy implements Supplier<DistExecutor.SafeRunnable>{

	private void doClientStuff(final FMLCommonSetupEvent evt) {
		ScreenManager.registerFactory((ContainerType<BorklerContainer>) Index.BORKLER_CONTAINER_TYPE,
				BorklerScreen::new);
		Borkler.LOGGER.info("Registering Screens!");
	}

	@Override
	public  SafeRunnable get() {
		// TODO Auto-generated method stub
		return new SafeRunnable() {
			private static final long serialVersionUID = 1L;

			@Override
			public void run() {
				FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientProxy.this::doClientStuff);
			}
		};
	}

}
