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

import gazcreations.borkler.blocks.BorklerTileEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * A custom class representing a "Crafting Recipe" for a Borkler fluid fuel.
 * 
 * @author gazotti
 *
 */
public class BorklerFuel implements IRecipe<BorklerTileEntity> {

	protected final Fluid fluid;
	protected final int burnTime;
	private final ResourceLocation id;
	public static final IRecipeType<BorklerFuel> TYPE = IRecipeType.register("borkler:borkler_fuel");

	public BorklerFuel(Fluid fluid, int burnTime) {
		this(fluid, burnTime, new ResourceLocation("borkler:" + fluid.getRegistryName().toString() + "_fuel"));
	}

	public BorklerFuel(Fluid fluid, int burnTime, ResourceLocation id) {
		this.fluid = fluid;
		this.burnTime = burnTime;
		this.id = id;
	}

	/**
	 * Searches the available fluid fuels to check the burn time for one mB of a
	 * given fluid. <br>
	 * Doubles as a check to see if such fluid is a valid fuel.
	 * 
	 * @param fluid The fluid to check
	 * @param world The world whose RecipeManager will be used to check the recipes
	 * @return The burn time in ticks/mB; 0 if the fluid is not fuel
	 */
	public static int getBurnTime(Fluid fluid, World world) {
		for (BorklerFuel rec : world.getRecipeManager().getRecipesForType(BorklerFuel.TYPE)) {
			if (rec.fluid.isEquivalentTo(fluid))
				return rec.burnTime;
		}
		return 0;
	}

	@Override
	public boolean matches(BorklerTileEntity inv, World worldIn) {
		return inv.getFluidInTank(1).getFluid().isEquivalentTo(fluid);
	}

	@Override
	public ItemStack getCraftingResult(BorklerTileEntity inv) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canFit(int width, int height) {
		return false;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return BorklerFuelSerializer.BORKLER_FUEL;
	}

	@Override
	public IRecipeType<?> getType() {
		return TYPE;
	}
}
