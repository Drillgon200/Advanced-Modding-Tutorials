package com.drillgon.example.main;

import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.GLContext;

import com.drillgon.example.render.tileentity.RenderTestRender;
import com.drillgon.example.shaders.ShaderManager;
import com.drillgon.example.tileentity.TileEntityTestRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class ClientProxy extends ServerProxy {

	@Override
	public void preInit() {
		if(!Minecraft.getMinecraft().getFramebuffer().isStencilEnabled())
			Minecraft.getMinecraft().getFramebuffer().enableStencil();
		
		MinecraftForge.EVENT_BUS.register(new ModEventHandlerClient());
		
		if(!OpenGlHelper.shadersSupported) {
			MainRegistry.logger.log(Level.WARN, "GLSL shaders are not supported; not using shaders");
			ShaderManager.enableShaders = false;
		} else if(!GLContext.getCapabilities().OpenGL33) {
			MainRegistry.logger.log(Level.WARN, "OpenGL 3.3 is not supported; not using shaders");
			ShaderManager.enableShaders = false;
		}
		
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTestRender.class, new RenderTestRender());
	}
}
