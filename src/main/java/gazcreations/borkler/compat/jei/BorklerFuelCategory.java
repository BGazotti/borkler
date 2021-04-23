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

import gazcreations.borkler.BorklerConfig;
import gazcreations.borkler.Index;
import gazcreations.borkler.client.screen.BorklerScreen;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

/**
 * An abstract Borkler Fuel category for JEI.
 * 
 * @author gazotti
 *
 */
public abstract class BorklerFuelCategory<T> implements IRecipeCategory<T> {

	protected final IGuiHelper guiHelper;
	protected static final ResourceLocation BG = new ResourceLocation("borkler", "textures/gui/steam_boiler_jei.png");
	protected IDrawable waterTank;
	protected IDrawable fuelTank;
	protected IDrawable steamTank;
	protected IDrawable text;
	
	public BorklerFuelCategory(IGuiHelper guiHelper) {
		this.guiHelper = guiHelper;
	}

	@Override
	public String getTitle() {
		return I18n.format("container.borkler.steam_boiler");
	}

	@Override
	public IDrawable getBackground() {
		return guiHelper.createDrawable(BG, 0, 0, 175, 62); // also the bounds for our GUI
	}

	@Override
	public IDrawable getIcon() {
		return guiHelper.createDrawableIngredient(new ItemStack(() -> Index.Items.BORKLERITEM, 1));
	}
	
	@Override
	public void setRecipe(IRecipeLayout layout, T recipe, IIngredients ingredients) {
		int waterCap = BorklerConfig.CONFIG.WATER_USE.get();
		int steamCap = (int) (waterCap * BorklerConfig.CONFIG.CONVERSION_RATE.get());
		waterTank = guiHelper.createDrawable(BorklerScreen.overlayTexture, 0, 49, 16, 48);
		layout.getFluidStacks().init(0, true, 83, 7, 17, 48, waterCap, false, waterTank);
		layout.getFluidStacks().set(0, new FluidStack(Fluids.WATER, waterCap));
		steamTank = guiHelper.createDrawable(BorklerScreen.overlayTexture, 0, 0, 16, 49);
		layout.getFluidStacks().init(2, false, 148, 7, 17, 48, steamCap, false, steamTank);
		layout.getFluidStacks().set(2, new FluidStack(Index.Fluids.STEAMSOURCE, steamCap));
	}
}
