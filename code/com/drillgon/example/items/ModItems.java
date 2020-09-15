package com.drillgon.example.items;

import java.util.ArrayList;
import java.util.List;

import com.drillgon.example.blocks.ModBlocks;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ModItems {

	//Yes, I know this is bad practice. I don't care, because it makes creating new items easier.
	public static List<Item> ALL_ITEMS = new ArrayList<>();

	//I also know I shouldn't be doing this in preInit.
	public static void preInit(){
		for(Item item : ALL_ITEMS){
			ForgeRegistries.ITEMS.register(item);
		}
		for(Block block : ModBlocks.ALL_BLOCKS){
			ForgeRegistries.ITEMS.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
		}
	}
}
