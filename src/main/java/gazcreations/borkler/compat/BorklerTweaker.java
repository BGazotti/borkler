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

package gazcreations.borkler.compat;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.managers.IRecipeManager;

import gazcreations.borkler.recipes.BorklerFuel;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;

/**
 * So, I can't possibly know what y'all will want to fuel this bad boy with.
 * Hopefully, CraftTweaker allows for quick, in-game editing of recipes, and why
 * not make our boiler extra steamy and versatile?
 * 
 * @author gazotti
 *
 */
@ZenRegister
@ZenCodeType.Name("mods.borkler.steam_boiler")
public class BorklerTweaker implements IRecipeManager {

	public BorklerTweaker() {
	}

	@Override
	public final IRecipeType<BorklerFuel> getRecipeType() {
		return BorklerFuel.TYPE;
	}

	@ZenCodeType.Method
	public final void addFuel(String recipeName, IFluidStack fluid, int burnTime) {
		ResourceLocation reciPath = new ResourceLocation("crafttweaker", fixRecipeName(recipeName));
		final BorklerFuel fuel = new BorklerFuel(fluid.getFluid().getInternal(), burnTime, reciPath);
		CraftTweakerAPI.apply(new ActionAddFluidRecipe(this, fuel));
		// BorklerTileEntity.addFuel(fluid.getFluid().getInternal(), burnTime);
	}

}
