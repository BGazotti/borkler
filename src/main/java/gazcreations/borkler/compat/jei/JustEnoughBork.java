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
package gazcreations.borkler.compat.jei;

import gazcreations.borkler.Index;
import gazcreations.borkler.client.screen.BorklerScreen;
import gazcreations.borkler.recipes.BorklerFuel;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * This is an attempt at providing JEI support.<br>
 * 
 * A note to self and fellow curious readers: Everything here is running
 * clientside; we can use stuff like {@link Minecraft} and {@link BorklerScreen}
 * without fear of crashing.
 * 
 * @author gazotti
 *
 */
@JeiPlugin
public class JustEnoughBork implements IModPlugin {

	private static final ResourceLocation UID = new ResourceLocation("borkler", "justenoughbork");
	private BorklerFluidFuelCategory catFluidFuel;

	/**
	 * Nothing to see here, move along.
	 */
	public JustEnoughBork() {
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration reg) {
		// pretty self-explanatory.
		reg.addRecipeCategories(catFluidFuel = new BorklerFluidFuelCategory(reg.getJeiHelpers().getGuiHelper()));
	}

	@SuppressWarnings("resource")
	@Override
	public void registerRecipes(IRecipeRegistration reg) {
		// register all liquid fuel types

		reg.addRecipes(Minecraft.getInstance().player.getCommandSenderWorld().getRecipeManager()
				.getAllRecipesFor(BorklerFuel.TYPE), BorklerFluidFuelCategory.UID);

	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
		reg.addRecipeCatalyst(new ItemStack(() -> Index.Items.BORKLERITEM), BorklerFluidFuelCategory.UID,
				VanillaRecipeCategoryUid.FUEL);
	}

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

}
