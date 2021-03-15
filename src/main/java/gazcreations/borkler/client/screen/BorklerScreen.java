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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import gazcreations.borkler.container.BorklerContainer;
import gazcreations.borkler.network.BorklerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.network.NetworkEvent.Context;

/**
 * A GUI for the player to interact with the Steam Boiler. The possibilities are
 * endless!
 * 
 * @author gazotti
 *
 */
@OnlyIn(Dist.CLIENT)
public class BorklerScreen extends ContainerScreen<BorklerContainer> implements IHasContainer<BorklerContainer> {

	private List<Pair<FluidStack, Integer>> fluids; // TODO get these via packet
	private TankSimulator[] tanks;

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
		// TODO Auto-generated constructor stub
		this.xSize = 184;
		this.ySize = 151;
		fluids = screenContainer.getTanks();
		if (fluids == null) {
			this.fluids = new ArrayList<>(4);
			for (int i = 0; i < 3; i++)
				fluids.add(Pair.of(FluidStack.EMPTY, 0));
		}
		initTanks(Minecraft.getInstance().getMainWindow().getWidth(),
				Minecraft.getInstance().getMainWindow().getHeight());
		passEvents = false;
	}

	private void initTanks(int width, int height) {
		this.tanks = new TankSimulator[3];
		for (int i = 0; i < fluids.size(); i++) { // 208,55
			/**
			 * these magic numbers came up while I was configuring the screen for an
			 * arbitrary size
			 */
			tanks[i] = new TankSimulator(this, fluids.get(i).getKey(), fluids.get(i).getValue(),
					(width / 2) - 5 + (width / 427) * (i * 32), (height / 2) - 65);
		}
	}

	public void init(Minecraft minecraft, int width, int height) {
		super.init(minecraft, width, height);
		this.initTanks(width, height);
	}

	@Override
	protected void renderHoveredTooltip(MatrixStack matrixStack, int x, int y) {
		if (this.minecraft.player.inventory.getItemStack().isEmpty() && this.hoveredSlot != null
				&& this.hoveredSlot.getHasStack()) {
			this.renderTooltip(matrixStack, this.hoveredSlot.getStack(), x, y);
		} else {
			for (TankSimulator tank : tanks) {
				if (tank.isMouseOver(x, y)) {
					List<ITextComponent> text = new ArrayList<>(3);
					if (!tank.fluid.isEmpty()) {
						text.add(tank.fluid.getDisplayName());
						text.add(new StringTextComponent(String.valueOf(tank.fluid.getAmount()) + " mB"));
					}
					else
						text.add(new StringTextComponent("Empty"));
					GuiUtils.preItemToolTip(new ItemStack(() -> Items.ACACIA_BOAT, 1));
					GuiUtils.drawHoveringText(matrixStack, text, x, y, width, height, 30, font);
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

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		for (TankSimulator tank : tanks) {
			GuiUtils.drawContinuousTexturedBox(matrixStack, tank.posX, tank.posY, 0, 0, tank.sizeX, tank.sizeY, 16, 16,
					0, 0.9F);
		}
		this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager()
				.bindTexture(new ResourceLocation("borkler", "textures/gui/steam_boiler.png"));
		blit(matrixStack, (this.width - this.xSize) / 2, (this.height - this.ySize) / 2, 0, 0, 256, 256);
	}

	public void handleDataPacket(BorklerData data, Supplier<Context> ctx) {

	}

	class TankSimulator implements IGuiEventListener {
		private FluidStack fluid;
		private int capacity;
		private ResourceLocation fluidTexture;
		private int fluidColor;
		private BorklerScreen screen;
		private int posX;
		private int posY;
		private int sizeX;
		private int sizeY;

		/**
		 * Please do not supply null fluids to this tank. Use {@link FluidStack#EMPTY}.
		 * 
		 * @param screen
		 * @param fluid
		 * @param capacity
		 * @param posX
		 * @param posY
		 */
		public TankSimulator(BorklerScreen screen, @Nonnull FluidStack fluid, int capacity, int posX, int posY) {
			this.fluid = fluid;
			this.capacity = capacity;
			this.screen = screen;
			this.posX = posX;
			this.posY = posY;
			this.sizeX = 18;
			this.sizeY = 49;
			getFluidTexture();
			screen.addListener(this);

		}

		public boolean isMouseOver(double mouseX, double mouseY) {
			return mouseX >= this.posX && mouseX < this.posX + this.sizeX && mouseY >= this.posY
					&& mouseY < this.posY + this.sizeY;
		}

		/**
		 * Gets the texture and color for this TankSimulator's fluid.
		 */
		private void getFluidTexture() {
			fluidTexture = fluid.getFluid().getAttributes().getStillTexture();
			fluidColor = fluid.getFluid().getAttributes().getColor();
		}

		/**
		 * @return a number, between 0 and 1, representing the proportion of fluid to
		 *         capacity for this tank.
		 */
		private double getFilledFraction() {
			try {
				return fluid.getAmount() / capacity;
			} catch (ArithmeticException singularity) {
				// yeah i see ya trying to divide by zero. Oh wait, that's me.
				return 0;
			}
		}
	}

}
