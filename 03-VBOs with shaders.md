# Modern VBOs with shaders
The first VBO tutorial covered setting up a basic vbo designed to work with the fixed function pipeline. In the tutorial, we will set up a more modern version of a vbo that uses generic vertex attributes instead of the fixed client states. This will allow use to send arbitrary data as vertex attributes later. We will also put this VBO in a VAO (Vertex Array Object), which acts as a state container so we don't have to do all the setup commands every time we want to draw it, we just bind the vao and render it. This will also make it easier to specify what vertex attributes we want in a model loader instead of in the vbo class itself.

## Vao class
The vao class will be very similar to the vbo class. The data we will need for it is the vao id, the draw mode, the length of the vertices, and whether or not we should be drawing elements. The element buffer is for indices. The reason this is a thing is because many models have exactly the same vertex data specified more than once in places where primitives link with each other. To save space and memory, the solution is to have a bunch of vertices and indices that point to those vertices. The indices determine what is actually drawn. Here's a graphic example.
![image](https://user-images.githubusercontent.com/50186362/93248821-3c9dcc80-f745-11ea-9a8f-be482bc2df7c.png)
The element buffer is what stores the indices. I won't go over that right now, because we aren't using indices.
Finally, to draw the vao, we can simply bind and draw it. If elements are enabled, we use glDrawElements instead of glDrawArrays. The class should now look like this
```java
public class Vao {
	
	public int vaoId;
	public int drawMode;
	public int vertexCount;
	public boolean useElements;
	
	public Vao(int vao, int mode, int length, boolean b) {
		this.vaoId = vao;
		this.drawMode = mode;
		this.vertexCount = length;
		this.useElements = b;
	}
	
	public void draw(){
		GL30.glBindVertexArray(vaoId);
		if(useElements){
      //Unsigned int because usually elements are specified as unsigned integer values
			GL11.glDrawElements(drawMode, vertexCount, GL11.GL_UNSIGNED_INT, 0);
		} else {
			GL11.glDrawArrays(drawMode, 0, vertexCount);
		}
		GL30.glBindVertexArray(0);
	}
}
```

## Setting up the VBO and VAO
We're going to use the same vertices array from the first tutorial to set it up. First, create a class called Vao, that will be the class that stores all information necessary to rendering our VAO after it's set up. Just like the first vbo tutorial, we need to put all the vertex data into a pile of bytes so opengl can understand it.
```java
ByteBuffer data = GLAllocation.createDirectByteBuffer(vertices.length * Vertex.BYTES_PER_VERTEX);
		for(Vertex v : vertices){
			data.putFloat(v.x);
			data.putFloat(v.y);
			data.putFloat(v.z);
			data.putFloat(v.u);
			data.putFloat(v.v);
			//Normals don't need as much precision as tex coords or positions
			data.put((byte)((int)(v.normalX*127)&0xFF));
			data.put((byte)((int)(v.normalY*127)&0xFF));
			data.put((byte)((int)(v.normalZ*127)&0xFF));
			//Neither do colors
			data.put((byte)(v.r*255));
			data.put((byte)(v.g*255));
			data.put((byte)(v.b*255));
			data.put((byte)(v.a*255));
		}
		data.rewind();
```
Just like the first vbo, we're going to create a vbo, bind it, and send the data to it.
```java
int vbo = GL15.glGenBuffers();
GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
```
Now is where it differs. Since we want to use a vao to hold the state so we don't have to keep enabling attributes every time we want to render, we need to create a vao and bind it.
```java
int vao = GL30.glGenVertexArrays();
GL30.glBindVertexArray(vao);
```
Next, we have to tell it what attributes we are using. Instead of static attributes like in the last tutorial, however, we are going to use generic vertex attributes. We use glVertexAttribPointer to tell it how to interpret each attribute we want to send, and then enable each attribute index. Normal and color, stored as bytes, should be normalized to between 0 and 1 for use in the shader.
```java
//Position. Arguments: attribute 0, 3 numbers (will be accessed as a vec3 in the shader), should be interpreted as a float, not normalized, stride and offset same as last time
GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.BYTES_PER_VERTEX, 0);
//Enable vertex attribute 0
GL20.glEnableVertexAttribArray(0);
//Texcoords. Arguments: attribute 1, 2 numbers, interpreted as a float, not normalized, stride and offset same as last time
GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, Vertex.BYTES_PER_VERTEX, 12);
GL20.glEnableVertexAttribArray(1);
//Normal. Arguments: attribute 2, 3 numbers, should be interpreted as a signed byte, will be normalized, stride and offset same as last time
GL20.glVertexAttribPointer(2, 3, GL11.GL_BYTE, true, Vertex.BYTES_PER_VERTEX, 20);
GL20.glEnableVertexAttribArray(2);
//Color. Arguments: attribute 3, 4 numbers, interpreted as an unsigned byte, will be normalized, stride and offset same as last time
GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, Vertex.BYTES_PER_VERTEX, 23);
GL20.glEnableVertexAttribArray(3);
```
After that, we just unbind the vao and the vbo, and return a new instance of the vao class.
```java
GL30.glBindVertexArray(0);
GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
return new Vao(vao, GL11.GL_QUADS, vertices.length, false);
```
The whole method should look like this:
```java
public static Vao setupVertices(Vertex[] vertices){
		ByteBuffer data = GLAllocation.createDirectByteBuffer(vertices.length * Vertex.BYTES_PER_VERTEX);
		for(Vertex v : vertices){
			data.putFloat(v.x);
			data.putFloat(v.y);
			data.putFloat(v.z);
			data.putFloat(v.u);
			data.putFloat(v.v);
			//Normals don't need as much precision as tex coords or positions
			data.put((byte)((int)(v.normalX*127)&0xFF));
			data.put((byte)((int)(v.normalY*127)&0xFF));
			data.put((byte)((int)(v.normalZ*127)&0xFF));
			//Neither do colors
			data.put((byte)(v.r*255));
			data.put((byte)(v.g*255));
			data.put((byte)(v.b*255));
			data.put((byte)(v.a*255));
		}
		data.rewind();
		
		int vbo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		
		int vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);
		
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.BYTES_PER_VERTEX, 0);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, Vertex.BYTES_PER_VERTEX, 12);
		GL20.glEnableVertexAttribArray(1);
		GL20.glVertexAttribPointer(2, 3, GL11.GL_BYTE, true, Vertex.BYTES_PER_VERTEX, 20);
		GL20.glEnableVertexAttribArray(2);
		GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, Vertex.BYTES_PER_VERTEX, 23);
		GL20.glEnableVertexAttribArray(3);
		
		GL30.glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		return new Vao(vao, GL11.GL_QUADS, vertices.length, false);
	}
```
And we're done with the vao!

## Shader that can render the vao
Now, we need to create a shader that can use the data from this vao. Create two new shader files, a vertex and a fragment shader. I called them base_vao.vert and base_vao.frag. Since the code is almost the same as the basic shader from last tutorial, I'm just going to copy the vertex and fragment shaders from that. The vertex and fragment shaders should now look like this (same as last tutorial).
```glsl
#version 330 compatibility

out vec2 texCoord;
out vec2 lightCoord;
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
Now, to make it work with out new vao, we have to change the way it accesses vertex attributes from the fixed function build in way, to the must more controllable and more modern way. Vertex attributes are specified by in variables with the layout qualifier. We also specify the attribute location just to be extra verbose. We will specify a layout in variable for each vertex attribute we defined when we set up the vbo.
```glsl
layout (location = 0) in vec3 in_pos;
layout (location = 1) in vec2 in_texcoord;
layout (location = 2) in vec3 in_normal;
layout (location = 3) in vec4 in_color;
```
Now, we will replace everywhere where we accessed a built in vertex attribute with the new attributes. Only the vertex shader needs to change for this.
```glsl
#version 330 compatibility

layout (location = 0) in vec3 in_pos;
layout (location = 1) in vec2 in_texcoord;
layout (location = 2) in vec3 in_normal;
layout (location = 3) in vec4 in_color;

out vec2 texCoord;
out vec2 lightCoord;
out vec4 color;
out vec3 lighting;

void main(){
	gl_Position = gl_ModelViewProjectionMatrix * vec4(in_pos, 1);
	//0 and 1 are used for the p and q coordinates because p defaults to 0 and q defaults to 1
	texCoord = (gl_TextureMatrix[0] * vec4(in_texcoord, 0, 1)).st;
	lightCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
	color = in_color;
	
	vec3 totalLighting = vec3(gl_LightModel.ambient) * vec3(gl_FrontMaterial.emission);
	vec3 normal = (gl_NormalMatrix * in_normal).xyz;
	vec4 difftot = vec4(0.0F);
	
	for (int i = 0; i < gl_MaxLights; i ++){
			
		vec4 diff = gl_FrontLightProduct[i].diffuse * max(dot(normal,gl_LightSource[i].position.xyz), 0.0f);
		diff = clamp(diff, 0.0F, 1.0F);     
			
		difftot += diff;
	}
	lighting = clamp((difftot + gl_LightModel.ambient).rgb, 0.0F, 1.0F);
}
```
We are done! Time to load the shader and the new vbo, and test it out.
I load the shader with this code in my resource manager class.
```java
public static final Shader base_vao = ShaderManager.loadShader(new ResourceLocation(RefStrings.MODID, "shaders/base_vao"))
			.withUniforms(ShaderManager.LIGHTMAP);
```
**Important: For some reason minecraft seems to delete vaos when they are created before everything else is done loading. For this reason, you have to create them on the first client tick rather than in regular startup like everything else. I have not figured out why this is yet. To solve this, I subscribed to client tick event and initialized my vao on the first tick.**
```java
private boolean initialized = false;
	
	@SubscribeEvent
	public void clientTick(ClientTickEvent e){
		if(!initialized){
			initialized = true;
			Vertex bottomLeft =  new Vertex(-0.5F, -0.5F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
			Vertex bottomRight = new Vertex(0.5F, -0.5F, 0F, 1F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
			Vertex topLeft =     new Vertex(-0.5F, 0.5F, 0F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
			Vertex topRight =    new Vertex(0.5F, 0.5F, 0F, 1F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
			Vertex[] vertices = new Vertex[]{bottomLeft, bottomRight, topRight, topLeft};
			
			ResourceManager.test_vao = Vao.setupVertices(vertices);
		}
	}
```
The rendering code remains almost the same, except with the new shader and the vao instead of the vbo.
```java
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y + 4, z + 0.5);
		
		ResourceManager.base_vao.use();
		
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.duck_tex);
		ResourceManager.test_vao.draw();
		
		ResourceManager.base_vao.release();
		
		GL11.glPopMatrix();
```
And perfect! We now have our duck in the world again, this time using a more modern opengl approach!
![image](https://user-images.githubusercontent.com/50186362/93255967-7d024800-f74f-11ea-983b-095bae252ab9.png)
While rendering single quads is great and all, it has limited usefulness. Next tutorial, I will cover loading models from one of the more complex formats: FBX. It's a lot more difficult than an obj file, but forge already has an obj loader, and these are supposed to be more advanced tutorials after all.
