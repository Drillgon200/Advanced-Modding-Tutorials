package com.drillgon.example.render.vbo;

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
