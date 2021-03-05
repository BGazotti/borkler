package gazcreations.borkler.blocks;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import gazcreations.borkler.Index;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;

public class SteamBlock extends FlowingFluidBlock {

	public SteamBlock() {
		super(new Supplier<Steam>() {
			@Override
			public Steam get() {
				// TODO Auto-generated method stub
				return Index.Fluids.STEAMSOURCE;
			}
		}, AbstractBlock.Properties.create(Material.WATER, MaterialColor.LIGHT_GRAY).doesNotBlockMovement().hardnessAndResistance(0.0F).noDrops()
				.setLightLevel(new ToIntFunction<BlockState>() {

					@Override
					public int applyAsInt(BlockState value) {
						// TODO Auto-generated method stub
						return 7;
					}
				}));
		this.setRegistryName("borkler", "steam_source");
	}
}
