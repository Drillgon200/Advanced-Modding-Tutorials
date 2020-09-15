package com.drillgon.example.main;

import com.drillgon.example.render.vbo.Vbo;
import com.drillgon.example.shaders.Shader;
import com.drillgon.example.shaders.ShaderManager;

import net.minecraft.util.ResourceLocation;

public class ResourceManager {

	public static final ResourceLocation duck_tex = new ResourceLocation(RefStrings.MODID, "textures/misc/duck.png");
	
	public static Vbo vbo;
	
	public static final Shader test_shader = ShaderManager.loadShader(new ResourceLocation(RefStrings.MODID, "shaders/test_shader"))
			.withUniforms(ShaderManager.LIGHTMAP);

}
