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

import java.util.ArrayList;

import com.mojang.blaze3d.matrix.MatrixStack;

import gazcreations.borkler.BorklerConfig;
import gazcreations.borkler.Index;
import gazcreations.borkler.client.screen.BorklerScreen;
import gazcreations.borkler.recipes.BorklerFuel;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

/**
 * A class representing the JEI category for a fluid Borkler fuel.
 * 
 * @author gazotti
 *
 */
class BorklerFluidFuelCategory extends BorklerFuelCategory<BorklerFuel> {

	static final ResourceLocation UID = new ResourceLocation("borkler", "jei_borkler_fluid_fuel");

	final int waterCap = BorklerConfig.CONFIG.WATER_USE.get();
	final int steamCap = (int) (waterCap * BorklerConfig.CONFIG.CONVERSION_RATE.get());

	public BorklerFluidFuelCategory(IGuiHelper guiHelper) {
		super(guiHelper);
	}

	/**
	 * Returns a {@link java.util.List} of 2 @link FluidStack}s: the first is water,
	 * the second is the provided fuel. <br>
	 * For JEI prettiness purposes.
	 */
	private java.util.List<FluidStack> plusWater(FluidStack fuel) {
		ArrayList<FluidStack> list = new ArrayList<>(3);
		list.add(new FluidStack(Fluids.WATER, waterCap));
		list.add(fuel);
		return list;
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public Class<? extends BorklerFuel> getRecipeClass() {
		return BorklerFuel.class;
	}

	@Override
	public void setIngredients(BorklerFuel recipe, IIngredients ingredients) {
		ingredients.setInputs(VanillaTypes.FLUID, plusWater(recipe.getFluid()));
		ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(Index.Fluids.STEAMSOURCE, steamCap));
	}

	@Override
	public void setRecipe(IRecipeLayout layout, BorklerFuel recipe, IIngredients ingredients) {
		// TODO minor cosmetic fix if I come around to it
		waterTank = guiHelper.createDrawable(BorklerScreen.overlayTexture, 0, 49, 16, 48);
		layout.getFluidStacks().init(0, true, 83, 7, 17, 48, waterCap, false, waterTank);
		layout.getFluidStacks().set(0, new FluidStack(Fluids.WATER, waterCap));
		fuelTank = guiHelper.createDrawable(BorklerScreen.overlayTexture, 0, 49, 16, 48);
		layout.getFluidStacks().init(1, true, 115, 7, 17, 48, 4, false, fuelTank);
		layout.getFluidStacks().set(1, recipe.getFluid());
		steamTank = guiHelper.createDrawable(BorklerScreen.overlayTexture, 0, 0, 16, 49);
		layout.getFluidStacks().init(2, false, 148, 7, 17, 48, steamCap, false, steamTank);
		layout.getFluidStacks().set(2, new FluidStack(Index.Fluids.STEAMSOURCE, steamCap));
	}

	@SuppressWarnings("resource")
	@Override
	public void draw(BorklerFuel recipe, MatrixStack stack, double mouseX, double mouseY) {
		Minecraft.getInstance().fontRenderer.drawString(stack, recipe.getBurnTime() + " ticks", 5, 5, 0);
	}
}
