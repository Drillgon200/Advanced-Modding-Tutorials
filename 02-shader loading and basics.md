# Shader Loading and Basic Usage
Shaders are the core of the programmable graphics pipeline. They allow you to control how a model looks on a vertex and pixel level. Shaders are split into two parts, the vertex shader and the fragment shader (technically there are also geometry shaders and tessellation shaders, but that's beyond the scope of this tutorial). The vertex shader's primary job is to figure out where each vertex goes in screen space. This is usually done by multiplying the model space vertex with the ModelViewProjection matrix. The fragment shader's job is to figure out the color of each pixel rendered (technically it's not a pixel yet, it's a fragment, hence "fragment shader". It still needs to pass various tests, like the depth test, the stencil test, and the ownership test, before actually becoming a pixel on the screen). Shaders are extremely useful because of the high level of control they give you over exactly how your geometry looks.

**Warning! Using a shader on a model will not be compatible with shader packs and has the potential to break in unexpected ways when used with them! I would recommend adding a config option to enable shaders if possible, in case a user would rather use a shader pack than have that part of your mod's visuals work. However, shaders break with pretty much everything rendering related, including stuff like different blend modes, so it's not a bad thing to use shaders in your mod.**


## Loading shaders

The first thing we need to do is make a shader class and a shader manager class. This will be the thing actually used in our rendering code. Before creating the shader, you should know about uniforms. A uniform is a bit of data that you send to your shader while using it. Open GL has a number of built in uniforms you can use if you're in compatibility mode, such as the ModelViewProjection matrix and various lighting variables. Since we need to be compatible with minecraft's fixed function pipeline, we're going to use compatiblity mode. Uniforms can be sent to a shader with any of the uniform commands in GL20. Some data types that can be sent are integers, floats, vectors (up to vec4), and 4x4 matrices. These uniforms cannot be changed between draw calls. To make things easier, so we don't have to call the uniform commands everywhere we use the shader, we can tell the shader class what uniforms it has and make it assign them automatically when you use the shader. For that, it's easiest to have a uniform functional interface. It will have a single method that takes the shader id. The uniform interface should look like this:
```java
public interface Uniform {

	public void apply(int shader);
}
```
The shader class will keep a list of uniforms and a shader id. For readability, I put a method withUniforms because I think it looks slightly cleaner than having it in the constructor, but it's perfectly fine to put that as a constructor argument, too. The use method uses the shader and applies all shader uniforms. The release method stops using it for when you've already rendered the geometry you want with that shader. The getShaderId method is just there in case we need the id number somewhere. Since not every computer supports shaders, it's a good idea to put in a check to see if shaders are supported and opengl 3.3 (the version of shaders we'll be using) is supported. This isn't usually much of a problem though, since almost all computers nowadays support opengl 3.3, and minecraft requires opengl 4.4 support in its minimum spec.
```java
public class Shader {

	private int shader;
	private List<Uniform> uniforms = new ArrayList<>(2);
	
	public Shader(int shader) {
		this.shader = shader;
	}
	
	public Shader withUniforms(Uniform... uniforms){
		for(Uniform u : uniforms){
			this.uniforms.add(u);
		}
		return this;
	}
	
	public void use(){
  if(!ShaderManager.enableShaders)
			return;
		GL20.glUseProgram(shader);
		for(Uniform u : uniforms){
			u.apply(shader);
		}
	}
  
  public void release(){
		GL20.glUseProgram(0);
	}
	
	public int getShaderId(){
		return shader;
	}
}
```
Now that we have the basic classes set up, we can create the code to load and compile a shader from a file. I made a ShaderManager class to keep track of my shaders and store the code for loading them. For actually loading the shaders, I plan on putting my shader files in the package assets.modid.shaders. Now we can create the method that loads shaders. It will take a ResourceLocation that points to the shader file, and return a Shader object. First off, we should declare the variables that will store the vertex and fragment shader ids. We need to declare them now so that we can delete incomplete shader objects in case something goes wrong in the shader loading and throws an exception. Next, because shaders do give you errors if something bad happens, write a try catch block that deletes the shaders in its catch section, and prints the stack trace of the exception so you know what's happening if your shader doesn't load correctly. Inside the try block, we create a new program. The program is what we actually use when using the shader. After that, we can create the vertex shader. Use glCreateShader with GL_VERTEX_SHADER to create a new vertex shader object and store the id in your vertexShader variable. Now, to get the file from the disk, we need to read the file into a bytebuffer so opengl can understand it. Make a new method that will read a resource location into a bytebuffer. It will take a resourcelocation and return a bytebuffer. I set it up so I only need to pass one resource location with the file name, and have each shader have the same file names for the vertex and fragment shader with different descriptions, so I appent the extention "vert" to the resource location before passing it to the readFileToBuf method. In the readFileToBuf, we get an InputStream from minecraft's ResourceManager, and turn that into a byte array using IOUtils from the org.apache.commons.io package. Next, we create a byte buffer with the file's bytes' length and put the bytes into it. Finally, we rewind the bytebuffer so it's ready for reading and return it.
```java
private static ByteBuffer readFileToBuf(ResourceLocation file) throws IOException {
		InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(file).getInputStream();
		byte[] bytes = IOUtils.toByteArray(in);
		IOUtils.closeQuietly(in);
		ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
		buf.put(bytes);
		buf.rewind();
		return buf;
}
```
Using that method, we can use glShaderSource to give the shader its code. After that, we can compile it. Compiling it might fail, so we should error check it just to make sure. If getting the compile status is false, I log an error and throw a new exception, which is caught by the try catch block, printed, and the incomplete shaders are deleted.
```java
vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
GL20.glShaderSource(vertexShader, readFileToBuf(new ResourceLocation(file.getResourceDomain(), file.getResourcePath() + ".vert")));
GL20.glCompileShader(vertexShader);
if(GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
  MainRegistry.logger.error(GL20.glGetShaderInfoLog(vertexShader, GL20.GL_INFO_LOG_LENGTH));
  throw new RuntimeException("Error creating vertex shader: " + file);
}
```
This code is almost exactly repeated, except this time for the fragment shader.
```java
			fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
			GL20.glShaderSource(fragmentShader, readFileToBuf(new ResourceLocation(file.getResourceDomain(), file.getResourcePath() + ".frag")));
			GL20.glCompileShader(fragmentShader);
			if(GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				MainRegistry.logger.error(GL20.glGetShaderInfoLog(fragmentShader, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating fragment shader: " + file);
			}
```
After the shader files are loaded, we can attach them to the program with glAttachShader. Then, we can link the program. Link the program can also error, so we check and catch that, too.
```java
			GL20.glAttachShader(program, vertexShader);
			GL20.glAttachShader(program, fragmentShader);
			GL20.glLinkProgram(program);
			if(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				MainRegistry.logger.error(GL20.glGetProgramInfoLog(program, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating fragment shader: " + file);
			}
```
Now that the program is done, we don't need the vertex and fragment shader objects anymore, so we can just delete them. We are done loading now, so we can return a new Shader with the program's id. We can also put in a check to see if shaders are supported and load a nothing shader if they aren't. The whole class should now look like this:
```java
public class ShaderManager {

	public static Shader loadShader(ResourceLocation file) {
  if(!enableShaders)
			return new Shader(0);
		int vertexShader = 0;
		int fragmentShader = 0;
		try {
			int program = GL20.glCreateProgram();
			
			vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
			GL20.glShaderSource(vertexShader, readFileToBuf(new ResourceLocation(file.getResourceDomain(), file.getResourcePath() + ".vert")));
			GL20.glCompileShader(vertexShader);
			if(GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				MainRegistry.logger.error(GL20.glGetShaderInfoLog(vertexShader, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating vertex shader: " + file);
			}
			
			fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
			GL20.glShaderSource(fragmentShader, readFileToBuf(new ResourceLocation(file.getResourceDomain(), file.getResourcePath() + ".frag")));
			GL20.glCompileShader(fragmentShader);
			if(GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				MainRegistry.logger.error(GL20.glGetShaderInfoLog(fragmentShader, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating fragment shader: " + file);
			}
			
			GL20.glAttachShader(program, vertexShader);
			GL20.glAttachShader(program, fragmentShader);
			GL20.glLinkProgram(program);
			if(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				MainRegistry.logger.error(GL20.glGetProgramInfoLog(program, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating fragment shader: " + file);
			}
			
			GL20.glDeleteShader(vertexShader);
			GL20.glDeleteShader(fragmentShader);
			
			return new Shader(program);
		} catch(Exception x) {
			GL20.glDeleteShader(vertexShader);
			GL20.glDeleteShader(fragmentShader);
			x.printStackTrace();
		}
		return new Shader(0);
	}

	private static ByteBuffer readFileToBuf(ResourceLocation file) throws IOException {
		InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(file).getInputStream();
		byte[] bytes = IOUtils.toByteArray(in);
		IOUtils.closeQuietly(in);
		ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
		buf.put(bytes);
		buf.rewind();
		return buf;
	}
}
```
To actually check if shaders are enabled, you can check if both shaders are supported and opengl 3.3 is supported in pre init.
```java
if(!OpenGlHelper.shadersSupported) {
			MainRegistry.logger.log(Level.WARN, "GLSL shaders are not supported; not using shaders");
			ShaderManager.enableShaders = false;
		} else if(!GLContext.getCapabilities().OpenGL33) {
			MainRegistry.logger.log(Level.WARN, "OpenGL 3.3 is not supported; not using shaders");
			ShaderManager.enableShaders = false;
		}
```
The shader loading code is done, so it's time to actually make a basic shader. In the shader folder in your mod's assets, create two files, [shadername].vert and [shadername].frag. [shadername] can be anything. I will use test_shader. To edit shaders, I would recommend installing some kind of glsl syntax highlighting plugin (glsl is the opengl shading language. It's similar to C and it's what we write shaders in). For larger shaders, I recommend getting Notepad++ and installing the GLSL plugin for that.

## Writing a shader
GLSL shaders start with a #version definition. Since we're using opengl 3.3 shaders (this is so they can work easily with both fixed function and modern things), we'll be using #version 330 compatibility. The compatibility makes it so we can access all of minecraft's fixed function variables while using more modern shaders. After the #version, we can put a main method. This is where all the code that determines how the pixels looks and where vertices are goes. A shader with nothing in it should look like this.
```glsl
#version 330 compatibility

void main(){

}
```
The vertex shader only really needs to do one thing, which is to transform the input vertex. All we need to do for that is multiply the built in ModelViewProjection matrix with gl_Vertex and write it to gl_Position. All vertex shaders must write to gl_Position. Now, we also might want to pass data, like texture coordinates and normals, to the fragment shader and do stuff, like sampling a texture, there. This is done with in and out variables. We need to pass the texture coordinate so we can see our nice duck image, so let's use a built in attribute and do that. All out variables are automatically interpolated between vertices when you get them in the fragment shader. That way vertex attributes mix evenly throughout the triangle. Here is the vertex shader code so far.
```glsl
#version 330 compatibility

out vec2 texCoord;

void main(){
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
  //TexCoords can have 4 components: S, T, R, and Q. R is replaced by P in some places because it conflicts with R from RGBA. We only need to worry about S and T, since we're only sampling a normal 2d texture.
	texCoord = gl_MultiTexCoord0.st;
}
```
We're done with our basic vertex shader for now, so let's move on to the fragment shader. It will use the same blank shader as the vertex shader. We will specify an in variable to recieve the texture coords, and an out variable for the final color. In varaibles must be named the same thing as the out variables in the vertex shader. Now, we will specify a uniform sampler2D. This will have 0 by default, so we don't actually have to send it any uniform data. The value it holds corresponds to the texture unit it is sampling from. 0 is used for the active texture, while 1 is used for the lightmap. The others we can use for sending multiple textures to our shader. In the fragment's shader main method, we can sample the texture with the sampler and the texture coordinate passed from the vertex shader and make that our fragment color.
```glsl
#version 330 compatibility

in vec2 texCoord;
out vec4 FragColor;

uniform sampler2D texture;

void main(){
	FragColor = texture2D(texture, texCoord);
}
```
That's all that's needed for a basic shader, so let's test it on the duck from last tutorial. I used this code in my ResourceManager to load the shader.
```java
public static final Shader test_shader = ShaderManager.loadShader(new ResourceLocation(RefStrings.MODID, "shaders/test_shader"));
```
I then modified my rendering code for the duck in my test tile entity rendering to use the shader.
```java
GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y + 4, z + 0.5);
		
		ResourceManager.test_shader.use();
		
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.duck_tex);
		ResourceManager.vbo.draw();
		
		ResourceManager.test_shader.release();
		
		GL11.glPopMatrix();
```
And perfect, we have our duck in the world again.
![image](https://user-images.githubusercontent.com/50186362/93158683-7c22d500-f6c1-11ea-813f-279a2712b6bd.png)
## Lighting
This has no lighting though, and lighting is rather important. This is how you can re construct minecraft's basic lighting model in a shader. We can use vertex lighting to replicate it. First, we send both the lightmap tex coords and the color to the fragment shader as well. After that, we can use some simple lighting algorithms that would take too much time to explain here to calculate the lighting. If you want to learn more about that, look up the blinn phong lighting model and gouraud shading. Multiplying by the texture matrix is necessary because minecraft uses the texture matrix to scale values from 0 to 255 supplied in lightmap methods to the 0 to 1 range used in opengl. The vertex shader now looks like this.
```glsl
#version 330 compatibility

out vec2 texCoord;
out vec2 lightmap;
out vec4 color;
out vec3 lighting;

void main(){
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
	lightCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
	color = gl_Color;
	
	vec3 totalLighting = vec3(gl_LightModel.ambient) * vec3(gl_FrontMaterial.emission);
	vec3 normal = (gl_NormalMatrix * gl_Normal).xyz;
	vec4 difftot = vec4(0.0F);
	
	for (int i = 0; i < gl_MaxLights; i ++){
			
		vec4 diff = gl_FrontLightProduct[i].diffuse * max(dot(normal,gl_LightSource[i].position.xyz), 0.0f);
		diff = clamp(diff, 0.0F, 1.0F);     
			
		difftot += diff;
	}
	lighting = clamp((difftot + gl_LightModel.ambient).rgb, 0.0F, 1.0F);
}
```
In the fragment shader, we take in all the new out variables we declared in the vertex shader. Using these, we sample the light map and multiply all the lighting variables together. Now the fragment shader looks like this.
```glsl
#version 330 compatibility

in vec2 texCoord;
in vec2 lightCoord;
in vec4 color;
in vec3 lighting;
out vec4 FragColor;

uniform sampler2D texture;
uniform sampler2D lightmap;

void main(){
	vec4 col = color * texture2D(texture, texCoord) * texture2D(lightmap, lightCoord);
	FragColor = vec4(col.rgb * lighting, col.a);
}
```
Notice that there is a new uniform. Since this is the lightmap, it has to be 1, and is not 1 by default, so we need to send 1 to the shader on use. Time to use the uniform system. Since the lightmap is used a lot, I will make a single anonymous uniform and put it in ShaderManager.
```java
public static final Uniform LIGHTMAP = shader -> {
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "lightmap"), 1);
	};
  ```
Now, in ResourceManager where we create the shader, we can add the lightmap.
```java
public static final Shader test_shader = ShaderManager.loadShader(new ResourceLocation(RefStrings.MODID, "shaders/test_shader"))
			.withUniforms(ShaderManager.LIGHTMAP);
```
That's it for setting up basic shaders. I highly encourage you to experiment around with them. This is only basic shader setup, and they can do a lot more. One thing I would suggest you do is to build shaders in programs with node based shader editors like unity, as it's much easier to see what your shader is doing there, and try to port that shader to glsl for minecraft.
