package com.drillgon.example.render.vbo;

public class Vertex {
	
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