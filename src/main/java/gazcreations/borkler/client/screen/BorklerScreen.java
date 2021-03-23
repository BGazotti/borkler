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

package gazcreations.borkler.client.screen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import gazcreations.borkler.container.BorklerContainer;
import gazcreations.borkler.entities.BorklerTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.gui.GuiUtils;

/**
 * A GUI for the player to interact with the Steam Boiler. The possibilities are
 * endless!
 * 
 * @author gazotti
 *
 */
@OnlyIn(Dist.CLIENT)
public class BorklerScreen extends ContainerScreen<BorklerContainer> implements IHasContainer<BorklerContainer> {

	private static final ResourceLocation guiTexture = new ResourceLocation("borkler", "textures/gui/steam_boiler.png");
	private static final ResourceLocation overlayTexture = new ResourceLocation("borkler",
			"textures/gui/boiler_overlay.png");
	// private List<Pair<FluidStack, Integer>> fluids;
	private TankSimulator[] tanks;
	private BorklerTileEntity ent;

	/**
	 * A little screen constructor, containing the back-end container and the
	 * player's inventory.
	 * 
	 * @param screenContainer
	 * @param inv
	 * @param titleIn
	 */
	public BorklerScreen(BorklerContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
		super(screenContainer, inv, titleIn);
		this.xSize = 184;
		this.ySize = 151;
		ent = screenContainer.getTileEntity();
		initTanks(Minecraft.getInstance().getMainWindow().getWidth(),
				Minecraft.getInstance().getMainWindow().getHeight());
		passEvents = false;
	}

	private void initTanks(int width, int height) {
		this.tanks = new TankSimulator[3];
		for (int i = 0; i < tanks.length; i++) {
			/*
			 * These magic numbers came up while I was configuring the screen for an
			 * arbitrary size. Think of them as... correction factors. Yup, that's it.
			 */
			tanks[i] = new TankSimulator(i, (width / 2) - 5 + (width / 427) * (i * 32), (height / 2) - 65);
		}
	}

	/**
	 * A much needed override to resize tanks when the game window is resized.
	 */
	public void init(Minecraft minecraft, int width, int height) {
		super.init(minecraft, width, height);
		this.initTanks(width, height);
	}

	/**
	 * Will draw default tooltips for items and for the fluids in the tanks.
	 */
	@Override
	protected void renderHoveredTooltip(MatrixStack matrixStack, int x, int y) {
		if (this.minecraft.player.inventory.getItemStack().isEmpty() && this.hoveredSlot != null
				&& this.hoveredSlot.getHasStack()) {
			this.renderTooltip(matrixStack, this.hoveredSlot.getStack(), x, y);
		} else {
			for (int i = 0; i < tanks.length; i++) {
				TankSimulator tank = tanks[i];
				if (tank.isMouseOver(x, y)) {
					List<ITextComponent> text = new ArrayList<>(3);
					if (!tank.getFluid().isEmpty()) {
						text.add(tank.getFluid().getDisplayName());
						text.add(new StringTextComponent(String.valueOf(tank.getFluid().getAmount()) + " mB"));
					} else
						text.add(new StringTextComponent("Empty"));
					// Not sure if this needs to be called, but still, documentation suggests it
					GuiUtils.preItemToolTip(new ItemStack(() -> Items.ACACIA_BOAT, 1));
					GuiUtils.drawHoveringText(matrixStack, text, x, y, width, height, 53, font);
					GuiUtils.postItemToolTip();
				}
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
		this.font.func_243248_b(matrixStack, this.title, 8.0F, 6.0F, 4210752);
		this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(), 8.0F, (float) (this.ySize - 96 + 2),
				4210752);
	}

	/**
	 * Will draw a tank with its fluid and level on the screen.
	 * 
	 * @param tank        the TankSimulator to draw with liquid and overlay
	 * @param doubleScale Whether to use a doubly-graduated scale for the tank. Used
	 *                    for steam.
	 */
	private void drawTank(MatrixStack matrixStack, TankSimulator tank, boolean doubleScale) {
		if (tank.getFluid().isEmpty())
			return; // will not draw an empty tank
		tank.setFluidTexture();
		/*
		 * Let's isolate our variables for a prettier method call.
		 * 
		 * Tank render size is calculated based on the amount of fluid present. We will
		 * subtract 2 pixels from that result to respect the tank's borders (cosmetic
		 * effect).
		 */
		int ySize = (int) Math.ceil(tank.sizeY * tank.getFilledFraction()) - 2;
		/*
		 * The fluid's initial y position is calculated using the size of the containing
		 * area and just how much fluid there's in it, minus one pixel for correction.
		 */
		int yPos = tank.posY + tank.sizeY - ySize - 1;

		RenderSystem.color4f(tank.fluidColor.getRed() / 255f, tank.fluidColor.getGreen() / 255f,
				tank.fluidColor.getBlue() / 255f, tank.fluidColor.getAlpha() / 255f);
		RenderSystem.enableBlend();
		getMinecraft().getTextureManager().bindTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
		/*
		 * Again, we will subtract two pixels from the tank's X size and dislocate it to
		 * the right.
		 */
		blit(matrixStack, tank.posX + 1, yPos, 0, tank.sizeX - 2, ySize, tank.cachedFluidSprite);
		RenderSystem.disableBlend();
		RenderSystem.color4f(1, 1, 1, 1);
		getMinecraft().getTextureManager().bindTexture(overlayTexture);
		/*
		 * This offset tells the texture manager where to look for the tank overlay in
		 * the gui texture file.
		 */
		int yOffset = 0;
		if (!doubleScale)
			yOffset = 49;
		blit(matrixStack, tank.posX, tank.posY, 0, yOffset, tank.sizeX, tank.sizeY);

	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		for (int i = 0; i < 3; i++) {
			drawTank(matrixStack, tanks[i], i == 2);
		}
		this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bindTexture(guiTexture);
		blit(matrixStack, (this.width - this.xSize) / 2, (this.height - this.ySize) / 2, 0, 0, 256, 256);
	}

	class TankSimulator implements IGuiEventListener {
		private int index;
		private TextureAtlasSprite cachedFluidSprite;
		private Color fluidColor;
		private int posX;
		private int posY;
		private int sizeX;
		private int sizeY;

		/**
		 * 
		 * @param index The corresponding index (0-2) to the BorklerTileEntity. Will be
		 *              used to retrieve fluid and capacity.
		 * @param posX  This tank's left boundary.
		 * @param posY  This tank's upper boundary.
		 */
		public TankSimulator(int index, int posX, int posY) {
			this.index = index;
			this.posX = posX;
			this.posY = posY;
			this.sizeX = 18;
			this.sizeY = 49;
			setFluidTexture();
			BorklerScreen.this.addListener(this); // this is done so that the tanks can be resized

		}

		/**
		 * Gets whether the mouse is currently over this TankSimulator's area.
		 */
		public boolean isMouseOver(double mouseX, double mouseY) {
			return mouseX >= this.posX && mouseX < this.posX + this.sizeX && mouseY >= this.posY
					&& mouseY < this.posY + this.sizeY;
		}

		/**
		 * Gets the texture and color for this TankSimulator's fluid.
		 */
		private void setFluidTexture() {
			if (getFluid().isEmpty()) {
				cachedFluidSprite = null;
				fluidColor = new Color(0);
				return;
			}
			if (cachedFluidSprite == null) {
				Texture txtr = Minecraft.getInstance().getTextureManager()
						.getTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
				if (txtr instanceof AtlasTexture) {
					cachedFluidSprite = ((AtlasTexture) txtr)
							.getSprite(getFluid().getFluid().getAttributes().getStillTexture());
				}
				fluidColor = new Color(getFluid().getFluid().getAttributes().getColor());
			}
		}

		/**
		 * @return a number, between 0 and 1, representing the proportion of fluid to
		 *         capacity for this tank.
		 */
		private double getFilledFraction() {
			try {
				return getFluid().getAmount() / (double) getCapacity();
			} catch (ArithmeticException singularity) {
				// yeah i see ya trying to divide by zero. Oh wait, that's me.
				return 0;
			}
		}

		int getCapacity() {
			return ent.getTankCapacity(index);
		}

		FluidStack getFluid() {
			return ent.getFluidInTank(index);
		}
	}

}
