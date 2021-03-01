package gazcreations.borkler.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;

public class BorklerBlock extends Block {

	public BorklerBlock() {
		super(Properties.create(Material.PISTON).hardnessAndResistance(1.8f).sound(SoundType.STONE)
				.harvestTool(ToolType.PICKAXE).harvestLevel(0));
		this.setRegistryName("borkler","steam_boiler");
	}

}
