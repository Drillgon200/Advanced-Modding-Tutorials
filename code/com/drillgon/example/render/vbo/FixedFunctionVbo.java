package com.drillgon.example.render.vbo;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import net.minecraft.client.renderer.GLAllocation;

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
