# Display Lists and VBOs
Both display lists and VBOs (Vertex Buffer Objects) do roughly the same thing, storing a bunch of vertices in one large block so they can all be sent to the GPU at once, which is much better than sending each vertex one by one. Most of the time, display lists and vbos are stored in graphics memory on the GPU, which provides even more of a performance improvement, as you no longer have to send a large block of vertices to the GPU. Minecraft's Tessellator actually already uses a VBO, but the vertex data has to be added to it and sent to the GPU each time you want to draw something, which is slower. If your model is static (you never update the vertex data for the model), going for a display list or a vbo can improve the performance, especially if the model is quite large.
## Display Lists
Display lists are extremely simple to understand. They are also old opengl, and quite deprecated in modern opengl, but minecraft isn't modern opengl. All you have to do is make a new display list, render anything you want stored in it, and end the display list. Now, all the vertex data you rendered is stored in the list and can be rendered at any time by calling that list.

*Example*:
```java
//generates 1 list, using different numbers will result in that number of lists with sequential ids starting with the id returned by the function.
int displayList = GL11.glGenLists(1); 
//Creates the list. Compile means any vertex commands will be compiled into the list. The other option is GL_COMPILE_AND_EXECUTE, which isn't that useful to use since we want to draw it multiple times later, not right now.
GL11.glNewList(displayList, GL11.GL_COMPILE);
//Render anything you want in the list
some_model.render();
//Ends the list, it is no longer collecting vertex commands.
GL11.glEndList();
```
## VBOs
VBOs are a little more difficult, and often not neccessary, since a call list is just as fast, if not faster. However, VBOs are still useful for some situations, especially for use with shaders. VBOs allow you to specify vertex data with a whole lot more control than a call list, and you can even supply arbitrary data for shaders to access in VBOs. This is useful for things like GPU vertex skinning, and sending arbitrary particle data to the gpu for shader based particles. I will cover two VBO types here, one for use with regular minecraft and its fixed function opengl pipeline (this is also compatible with shader packs since it doesn't require you to use a shader yourself), and one for use with your own custom shaders.

### Creating a fixed function vbo
The first thing we're going to need when setting up a vbo is a few test vertices to see if it worked. I made this basic vertex data class to hold the vertex position, texture coordinates, normal, and color. If you don't use one of these attributes, it's perfectly fine to leave it out. Color is not often used anyway. I also put in the number of bytes each vertex is going to take up because we'll use that to figure out how much memory to allocate for the vbo later.
```java
public static class Vertex {

    //Pos, tex, normal, color
	  public static final int BYTES_PER_VERTEX = 3*4 + 2*4 + 3 + 4;

		public float x;
		public float y;
		public float z;
		public float u;
		public float v;
		public float normalX;
		public float normalY;
		public float normalZ;
		public float r;
		public float g;
		public float b;
		public float a;
		
		public Vertex(float x, float y, float z, float u, float v, float nX, float nY, float nZ, float r, float g, float b, float a) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.u = u;
			this.v = v;
			this.normalX = nX;
			this.normalY = nY;
			this.normalZ = nZ;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}
	}
  ```
For the test vertex array, we can just make a single quad for now. This vertex data defines 4 corners of a quad, each with the correct position, tex coord, and normal. They all use opaque white for the color, since we don't want to tint the quad any color.
```java
  Vertex bottomLeft =  new Vertex(-0.5F, -0.5F, 0F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex bottomRight = new Vertex(0.5F, -0.5F, 0F, 1F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex topLeft =     new Vertex(-0.5F, 0.5F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex topRight =    new Vertex(0.5F, 0.5F, 0F, 1F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex[] vertices = new Vertex[]{bottomLeft, bottomRight, topRight, topLeft};
 ```
Now, we actually need to put these vertices into a vbo. Since I'm making two different types of vbos, I made a vbo base class that has information that all vbos need. It contains the vbo id, which we use to actually bind and render the vertices, the draw mode, which controls how the vertices are used (could be things like triangles, quads, or lines), and the number of vertices, which is used to tell opengl how many vertices to draw from the vbo. This is helpful if you want to stick multiple models in the same vbo and render them separately, but we're not going to do that. I also put a draw function in because we need to be able to draw the vbo. This is the base class so far:
```java
public abstract class Vbo {

	public final int vboId;
	public final int drawMode;
	public final int numVertices;
	
	public Vbo(int vbo, int drawMode, int numVertices) {
		this.vboId = vbo;
		this.drawMode = drawMode;
		this.numVertices = numVertices;
	}
	
	public abstract void draw();
}
```
Next, we're going to make the actual class for the first type of vbo. I called it FixedFunctionVbo because it's designed to be used in the fixed function pipeline. In it, put a setupVbo function that takes an array of vertices. This method is going to transform those vertices into the vbo. First thing we need to do is put all those vertices in a format open gl can understand: a large block of bytes. We allocate a direct byte buffer with exactly the right number of bytes to hold the vbo data, then loop through each vertex and add all its data in order to the byte buffer. After we're done, rewind the buffer so it's ready to be sent to the gpu. The normals don't need as much precision as other components, so we can store each of those in a single byte. The best thing to do here would actually be packing the normal into a single 32 bit integer with 10 bits per normal value, but that's harder to wrap your head around. The normal values in the vertices are in the range -1 to 1, so to transform them to the -128 to 127 range of a signed byte, we multiply by 127. Colors are in the range 0 to 1, so to transform them to an unsigned byte ranging from 0 to 255, we multiply by 255.
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
Now that the data is all in place, we can create the vbo and send it the data. First, we tell opengl to create a new vbo and give us the id, then we bind the buffer to the array buffer (other buffers, like the element buffer, exist, but I'm not going to cover them here). After that, we can send our data to the buffer bound to the array buffer. We're done with creating the vbo, so now we can unbind the vbo (done by binding id 0) and create our renderable vbo object. Since we specified a quad in our vertex data, the draw mode will be quads. This object is what we return.
```java
//Generates a new buffer object
int vboId = GL15.glGenBuffers();
//Binds our newly generated buffer to the array buffer
GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
//Sends the data we put in the bytebuffer to the vbo. Static draw means we won't be updating this data very often, but we will be using it a lot. Other options are dynamic draw, which means the contents are modified a lot and used many times, and steam draw, which means the contents are modified once and used only a few times. This helps determine how the gpu allocates resources.
GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
//Unbind the buffer. Minecraft will throw an error if there is something already bound to the array buffer when it is tessellating.
GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

//Create our vbo holder class. It has the draw mode of quads, since our test data specified a quad. Most of the time, it will be triangles, since most models are triangulated.
FixedFunctionVbo vbo = new FixedFunctionVbo(vboId, GL11.GL_QUADS, vertices.length);
return vbo;
```
The whole class should look something like this now.
```java
public class FixedFunctionVbo extends Vbo {

	public FixedFunctionVbo(int vbo, int drawMode, int numVertices) {
		super(vbo, drawMode, numVertices);
	}

	public static FixedFunctionVbo setupVbo(Vertex[] vertices){
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
	  int vboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		FixedFunctionVbo vbo = new FixedFunctionVbo(vboId, GL11.GL_QUADS, vertices.length);
		return vbo;
	}

	@Override
	public void draw() {
	}
	
}
```
### Rendering a fixed function vbo
That's great that we have a vbo, but how do you actually render it? Unlike VAOs, this fixed function vbo needs all its attributes set up every time you render it. For that, we'll make a predraw and a postdraw method, for setting up the state needed to draw and cleaning up after. In the predraw method, we're going to bind the vbo to the array buffer, and enable all its attributes. All the pointer commands do is tell open gl where in the block of bytes we sent it each attribute is stored. The position of an attribute in the block of bytes is determined by stride*vertexId+offset. I made a badly drawn image to explain this better, shown below.
![image](https://user-images.githubusercontent.com/50186362/93141801-a95b8d00-f699-11ea-8161-c22249432d57.png)
```java
private void preDraw(){
    //Bind the buffer to the array buffer
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
    //Tells opengl that we specificed our positions as 3 numbers stored as 3 floats, with a stride of the size of a vertex and an offset of 0.
		GL11.glVertexPointer(3, GL11.GL_FLOAT, Vertex.BYTES_PER_VERTEX, 0);
    //Enables the position attribute
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
    //Tells opengl that the texcoords is 2 numbers, stored as floats, with an offset of 12 (position is the first 12 bytes)
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, Vertex.BYTES_PER_VERTEX, 12);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    //Tells opengl that the normal (which is always 3 numbers) is stored as signed bytes, with an offset of 20 (vertex bytes + the 8 texcoord bytes come first)
		GL11.glNormalPointer(GL11.GL_BYTE, Vertex.BYTES_PER_VERTEX, 20);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
    //Tells opengl that color is 4 numbers, stored as an unsigned byte, and has an offset of 23 (vertex bytes + texcoord bytes + the 3 normal bytes)
		GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, Vertex.BYTES_PER_VERTEX, 23);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
}
```
The post draw method just cleans up all of that by disabling all that states, and unbinding the vbo.
```java
private void postDraw(){
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
}
```
Actually drawing it is extremely simple compared to all that, simple call glDrawArrays and we're done!
```java
@Override
public void draw() {
		preDraw();
    //Draws the currently bound array buffer with the specified draw mode (quads in this case), from vertex 0 to the number of vertices (the whole model).
		GL11.glDrawArrays(drawMode, 0, numVertices);
		postDraw();
}
```
The whole class should now look like this.
```java
public class FixedFunctionVbo extends Vbo {

	public FixedFunctionVbo(int vbo, int drawMode, int numVertices) {
		super(vbo, drawMode, numVertices);
	}

	public static FixedFunctionVbo setupVbo(Vertex[] vertices){
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
		int vboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		FixedFunctionVbo vbo = new FixedFunctionVbo(vboId, GL11.GL_QUADS, vertices.length);
		return vbo;
	}

	private void preDraw(){
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL11.glVertexPointer(3, GL11.GL_FLOAT, Vertex.BYTES_PER_VERTEX, 0);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, Vertex.BYTES_PER_VERTEX, 12);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glNormalPointer(GL11.GL_BYTE, Vertex.BYTES_PER_VERTEX, 20);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, Vertex.BYTES_PER_VERTEX, 23);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
	}
	
	@Override
	public void draw() {
		preDraw();
		GL11.glDrawArrays(drawMode, 0, numVertices);
		postDraw();
	}
	
	private void postDraw(){
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}
	
}
```
In order to render the vbo, simply store the created vbo somewhere (I made a ResourceManager class to store things like this), then call render on it in something like a TESR. I used the ModelBakeEvent for loading the vbo, since it's a model and it sounds like it fits in the model baking event.
```java
@SubscribeEvent
	public void modelBaking(ModelBakeEvent e){
		Vertex bottomLeft =  new Vertex(-0.5F, -0.5F, 0F, 0F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex bottomRight = new Vertex(0.5F, -0.5F, 0F, 1F, 0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex topLeft =     new Vertex(-0.5F, 0.5F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex topRight =    new Vertex(0.5F, 0.5F, 0F, 1F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F);
		Vertex[] vertices = new Vertex[]{bottomLeft, bottomRight, topRight, topLeft};
		
		ResourceManager.vbo = FixedFunctionVbo.setupVbo(vertices);
}
```
After sticking this code in a test tile entity to render it...
```java
GL11.glPushMatrix();
GL11.glTranslated(x + 0.5, y + 4, z + 0.5);
		
OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.duck_tex);
ResourceManager.vbo.draw();
		
GL11.glPopMatrix();
```
Perfect! We now have a nice duck!
![image](https://user-images.githubusercontent.com/50186362/93146438-a8c7f400-f6a3-11ea-9f24-77787f72c1d8.png)
This is only a basic implementation, and if you were to use a vbo in an actual mod, you might want to do things like making it more extensible by giving it a vertex format to define what gets enabled or passing the draw mode to the setupVbo method. Minecraft's VertexFormats already have the code for enabling and disabling the attributes, so you would just call that instead.

Defining just a quad like this isn't that useful, and a display list would also work just fine for this model. The vbo really starts to become useful when you need to send your own data to the gpu per vertex and decide what to do with it in a shader. I will be using vbos with shaders for things like gpu skinning and single vertex particles.

I will cover vbo use with a shader after the shader loading tutorial.
