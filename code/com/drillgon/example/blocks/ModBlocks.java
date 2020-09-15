package com.drillgon.example.blocks;

import java.util.ArrayList;
import java.util.List;

import com.drillgon.example.blocks.generic.BlockTestRender;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ModBlocks {

	//Yes, I know this is bad practice. I don't care, because it makes creating new items easier.
	public static final List<Block> ALL_BLOCKS = new ArrayList<>();
	
	public static final Block test_render = new BlockTestRender(Material.IRON, "test_render");
	
	//I also know I shouldn't be doing this in preInit.
	public static void preInit(){
		for(Block block : ALL_BLOCKS){
			ForgeRegistries.BLOCKS.register(block);
		}
	}
}
