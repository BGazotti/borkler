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

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

public abstract class BorklerConfig {

	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	/**
	 * 
	 */
	public static final ForgeConfigSpec.BooleanValue THIRSTY;

	/**
	 * 
	 */
	public static final ForgeConfigSpec.IntValue WATER_USE;

	/**
	 * 
	 */
	public static final ForgeConfigSpec.DoubleValue CONVERSION_RATE;

	static {
		BUILDER.push("options");
		THIRSTY = BUILDER.comment(
				"If true, the boiler will automatically attempt to pull water and fuel from connected suppliers.")
				.define("thirsty", true);
		WATER_USE = BUILDER.comment("How much water will be consumed by a powered boiler, in mB/tick.")
				.defineInRange("water_use", 25, 5, 500);
		CONVERSION_RATE = BUILDER.comment("The ratio of conversion of water into steam.")
				.defineInRange("conversion_rate", 1.0, 0.1, 10.0);
		BUILDER.pop();
		SPEC = BUILDER.build();
	}

	public static void sendConfigToClient() {
	}

	public static void setup() {
		Path configPath = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "borkler");
		// Create the config folder
		try {
			Files.createDirectory(configPath);
		} catch (FileAlreadyExistsException e) {
			// nice
		} catch (IOException e) {
			Borkler.LOGGER.error("An error has occurred while trying to create the Borkler config directory.", e);
		}
		try {
			Files.createFile(Paths.get(configPath.toAbsolutePath().toString(), "config.toml"));
		}
		catch (FileAlreadyExistsException e) {
			//nice
		}
		catch (IOException e) {
			Borkler.LOGGER.error("An error has occurred while trying to create the Borkler config file.", e);
		}
		ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, BorklerConfig.SPEC,
				"borkler/config.toml");
		
	}
}
