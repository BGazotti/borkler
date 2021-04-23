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
 * @author gazotti
 *
 */
public class ClientProxy implements Supplier<DistExecutor.SafeRunnable> {

	@SuppressWarnings("unchecked")
	private void doClientStuff(final FMLCommonSetupEvent evt) {
		Borkler.LOGGER.info("Registering Screens!");
		ScreenManager.registerFactory((ContainerType<BorklerContainer>) Index.BORKLER_CONTAINER_TYPE,
				BorklerScreen::new);

	}

	@Override
	public SafeRunnable get() {
		return new SafeRunnable() {
			private static final long serialVersionUID = 1L;

			@Override
			public void run() {
				FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientProxy.this::doClientStuff);
			}
		};
	}

}
