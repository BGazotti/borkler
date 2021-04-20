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
package gazcreations.borkler.recipes;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * A serializer for the {@link BorklerFuel} recipes.
 * 
 * @author gazotti
 *
 */
public class BorklerFuelSerializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
		implements IRecipeSerializer<BorklerFuel> {

	public static final BorklerFuelSerializer BORKLER_FUEL = (BorklerFuelSerializer) new BorklerFuelSerializer()
			.setRegistryName("borkler", "borkler_fuel");

	/**
	 * 
	 */
	private BorklerFuelSerializer() {
	}

	@Nullable
	@Override
	public BorklerFuel fromJson(ResourceLocation recipeId, JsonObject json) {
		try {
			String fluidName = json.get("fluid").getAsString();
			Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidName));
			int burnTime = json.get("burnTime").getAsInt();
			gazcreations.borkler.Borkler.LOGGER.debug("Loaded fuel recipe for " + fluidName);
			return new BorklerFuel(fluid, burnTime);
		} catch (Exception e) {
			gazcreations.borkler.Borkler.LOGGER
					.debug("Something went wrong while trying to deserialize a borkler json recipe: ", e);
			return null;
		}
	}

	@Override
	public BorklerFuel fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
		Fluid fluid = buffer.readFluidStack().getFluid();
		int burnTime = buffer.readInt();
		return new BorklerFuel(fluid, burnTime, recipeId);
	}

	@Override
	public void toNetwork(PacketBuffer buffer, BorklerFuel recipe) {
		buffer.writeFluidStack(new FluidStack(recipe.fluid, 1));
		buffer.writeInt(recipe.burnTime);
	}
}
