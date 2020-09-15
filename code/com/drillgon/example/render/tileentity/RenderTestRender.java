package com.drillgon.example.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.drillgon.example.main.ResourceManager;
import com.drillgon.example.tileentity.TileEntityTestRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderTestRender extends TileEntitySpecialRenderer<TileEntityTestRender> {

	@Override
	public boolean isGlobalRenderer(TileEntityTestRender te) {
		return true;
	}
	
	@Override
	public void render(TileEntityTestRender te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y + 4, z + 0.5);
		
		ResourceManager.test_shader.use();
		
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.duck_tex);
		ResourceManager.vbo.draw();
		
		ResourceManager.test_shader.release();
		
		GL11.glPopMatrix();
	}
}
