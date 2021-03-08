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

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.tuple.Pair;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

public class BorklerConfig {

	public static final Path configPath = new File(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), "borkler.toml")
			.toPath();
	public static final BorklerConfig CONFIG;
	public static final ForgeConfigSpec SPEC;
	static {
		final Pair<BorklerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(BorklerConfig::new);
		CONFIG = specPair.getKey();
		SPEC = specPair.getValue();
		SPEC.setConfig((CommentedConfig.copy(FileConfig.of(configPath))));
		//SPEC.save();
		ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC,
				"borkler.toml");
		
	}
	//public final ForgeConfigSpec.Builder BUILDER;
	

	/**
	 * 
	 */
	public final ForgeConfigSpec.BooleanValue THIRSTY;

	/**
	 * 
	 */
	public final ForgeConfigSpec.IntValue WATER_USE;

	/**
	 * 
	 */
	public final ForgeConfigSpec.DoubleValue CONVERSION_RATE;

	protected BorklerConfig(ForgeConfigSpec.Builder builder) {
		Borkler.LOGGER.fatal(configPath);
		setup();
		//BUILDER = builder;
		// BUILDER.push("options");
		THIRSTY = builder.comment(
				"If true, the boiler will automatically attempt to pull water and fuel from connected suppliers.")
				.define("thirsty", true);
		WATER_USE = builder.comment("How much water will be consumed by a powered boiler, in mB/tick.")
				.defineInRange("water_use", 25, 5, 500);
		CONVERSION_RATE = builder.comment("The ratio of conversion of water into steam.")
				.defineInRange("conversion_rate", 1.0, 0.1, 10.0);
		// BUILDER.pop();
		//SPEC = builder.build();
		
	}

	/*
	 * You know what? Fuck this shit. Forge is refusing to create a default config
	 * file with that constructor. Instead of figuring out why and doing so
	 * appropriately, i'm just going to hardcode the hell out of this shit. Sue me.
	 */
	public static void createDefaultFile() throws Exception {
		final String content = "# If true, the boiler will automatically attempt to pull water and fuel from connected suppliers.\n"
				+ "thirsty = true\n" + "# How much water will be consumed by a powered boiler, in mB/tick.\n"
				+ "water_use = 25\n" + "# The ratio of conversion of water to steam.\n" + "conversion_ratio = 1.0\n";
		Files.write(configPath, content.getBytes(), StandardOpenOption.CREATE);

	}

	public static void sendConfigToClient() {
	}

	protected final void setup() {
		// Create the config folder
		try {
			if (!Files.exists(configPath))
				createDefaultFile();
		} catch (FileAlreadyExistsException e) {
			// nice
		} catch (Exception e) {
			Borkler.LOGGER.error("An error has occurred while trying to create the Borkler config file.", e);
		}

	}
}
