package com.drillgon.example.main;

import com.drillgon.example.blocks.ModBlocks;
import com.drillgon.example.items.ModItems;
import com.drillgon.example.render.vbo.FixedFunctionVbo;
import com.drillgon.example.render.vbo.Vertex;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModEventHandlerClient {

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent e){
		for(Item item : ModItems.ALL_ITEMS) {
			registerModel(item, 0);
		}
		for(Block block : ModBlocks.ALL_BLOCKS) {
			registerBlockModel(block, 0);
		}
	}
	
	private void registerBlockModel(Block block, int meta) {
		registerModel(Item.getItemFromBlock(block), meta);
	}
	
	private void registerModel(Item item, int meta) {
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}
	
	@SubscribeEvent
	public void modelBaking(ModelBakeEvent e){
		Vertex bottomLeft =  new Vertex(-0.5F, -0.5F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex bottomRight = new Vertex(0.5F, -0.5F, 0F, 1F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex topLeft =     new Vertex(-0.5F, 0.5F, 0F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex topRight =    new Vertex(0.5F, 0.5F, 0F, 1F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex[] vertices = new Vertex[]{bottomLeft, bottomRight, topRight, topLeft};
		
		ResourceManager.vbo = FixedFunctionVbo.setupVbo(vertices);
	}
}
