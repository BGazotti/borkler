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

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

import gazcreations.borkler.blocks.BorklerTileEntity;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.common.registries.MekanismGases;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * This class acts as a wrapper for a {@link BorklerTileEntity}, implementing
 * Mekanism's {@link IGasHandler} interface.<br>
 * Basically, it provides an external {@link Capability}<IGasHandler> that a
 * BorklerTE can use to output steam in its Mekanism gaseous form. The "Liquid"
 * steam provided by Borkler is converted to Mekanism's gaseous Steam when
 * something tries to extract a gas from it.
 * 
 * @author gazotti
 *
 */
public class MekaBorkler implements IGasHandler {

	@CapabilityInject(IGasHandler.class)
	public static Capability<IGasHandler> GasHandlerCapability = null;

	private final BorklerTileEntity boiler;

	private Set<LazyOptional<IGasHandler>> gasConsumers;

	public MekaBorkler(@Nonnull BorklerTileEntity borkler) {
		assert borkler != null;
		boiler = borkler;
		gasConsumers = Collections.emptySet();
		updateGasConnections();
	}

	public void updateGasConnections() {
		if (boiler.getWorld().isRemote())
			return;
		Set<LazyOptional<IGasHandler>> consumers = new ObjectArraySet<LazyOptional<IGasHandler>>(7) {

			private static final long serialVersionUID = 1L;

			public boolean add(LazyOptional<IGasHandler> element) {
				if (element == null || !element.isPresent())
					return false;
				return super.add(element);
			}
		};
		gazcreations.borkler.Borkler.LOGGER.debug("Borkler @" + boiler.getWorld() + " ," + boiler.getPos()
				+ " has been politely asked to update its gas connections.");
		gazcreations.borkler.Borkler.LOGGER.debug("Current connections are: " + gasConsumers.toString());
		LazyOptional<IGasHandler> cap = null;
		TileEntity te = null;
		for (Direction d : Direction.values()) {
			if ((te = boiler.getWorld().getTileEntity(boiler.getPos().offset(d))) != null) {
				cap = te.getCapability(GasHandlerCapability, d.getOpposite());
				// if (cap.isPresent()) override of Set.add will prevent empty Optionals from
				// being added
				boiler.addWithListener(consumers, cap);
			}
		}
		gazcreations.borkler.Borkler.LOGGER.debug(
				"Borkler @" + boiler.getWorld() + " ," + boiler.getPos() + "has updated its connections: " + consumers.toString());
		this.gasConsumers = consumers;
	}

	public BorklerTileEntity getWrapped() {
		return boiler;
	}

	@Override
	public GasStack extractChemical(int tank, long maxDrain, Action action) {
		// TODO Auto-generated method stub
		if (tank != 2)
			return GasStack.EMPTY;
		long drained = maxDrain;
		// Okay. Since we're talking about a steam boiler, it only makes sense
		// that whatever we drain here is steam, right?
		// guys?
		if (boiler.getFluidInTank(2).getAmount() < drained) {
			drained = boiler.getFluidInTank(2).getAmount();
		}
		GasStack stack = MekanismGases.STEAM.getStack(drained);
		if (action.execute() && drained > 0) {
			boiler.drain((int) drained, FluidAction.EXECUTE);
			boiler.markDirty();
		}
		return stack;
	}

	@Override
	public GasStack getChemicalInTank(int arg0) {
		// TODO Auto-generated method stub
		if (arg0 == 2) {
			return MekanismGases.STEAM.getStack(boiler.getFluidInTank(arg0).getAmount());
		}
		return GasStack.EMPTY;
	}

	@Override
	public long getTankCapacity(int arg0) {
		return boiler.getTankCapacity(arg0);
	}

	@Override
	public int getTanks() {
		return boiler.getTanks();
	}

	@Override
	public GasStack insertChemical(int arg0, GasStack arg1, Action arg2) {
		// TODO add support for gas fuels?
		// as of right now, this method won't do anything, because you can't pipe steam
		// to a boiler.
		return arg1;
	}

	@Override
	public boolean isValid(int arg0, GasStack arg1) {
		// TODO Auto-generated method stub
		return arg0 == 2 && arg1.getRaw() == MekanismGases.STEAM.get();
	}

	@Override
	public void setChemicalInTank(int arg0, GasStack arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public GasStack getEmptyStack() {
		return GasStack.EMPTY;
	}

	public void autoOutputGas() {
		// TODO implement
	}

}
