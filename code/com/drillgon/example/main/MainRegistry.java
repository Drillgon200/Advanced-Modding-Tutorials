package com.drillgon.example.main;

import org.apache.logging.log4j.Logger;

import com.drillgon.example.blocks.ModBlocks;
import com.drillgon.example.items.ModItems;
import com.drillgon.example.tileentity.TileEntityTestRender;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = RefStrings.MODID, version = RefStrings.VERSION, name = RefStrings.NAME)
public class MainRegistry {
	
	@SidedProxy(clientSide = RefStrings.CLIENTSIDE, serverSide = RefStrings.SERVERSIDE)
	public static ServerProxy proxy;

	@Mod.Instance(RefStrings.MODID)
	public static MainRegistry instance;
	
	public static Logger logger;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent evt){
		
		if(logger == null)
			logger = evt.getModLog();
		
		MinecraftForge.EVENT_BUS.register(new ModEventHandler());
		MinecraftForge.TERRAIN_GEN_BUS.register(new ModEventHandler());
		MinecraftForge.ORE_GEN_BUS.register(new ModEventHandler());
		ModItems.preInit();
		ModBlocks.preInit();
		proxy.preInit();
	}
	
	@EventHandler
	public void init(FMLInitializationEvent evt){
		proxy.init();
		GameRegistry.registerTileEntity(TileEntityTestRender.class, new ResourceLocation(RefStrings.MODID, "tileentity_test_render"));
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent evt){
		proxy.postInit();
	}
	
	@EventHandler
	public void serverStarting(FMLServerStartingEvent evt){
		
	}
}
