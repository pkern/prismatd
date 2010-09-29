package com.avona.games.towerdefence;

public final class RGB {
	public float R;
	public float G;
	public float B;
	
	public RGB(float r, float g, float b) {
		R = r;
		G = g;
		B = b;
	}
	
	public void sub(RGB other, float cutoff) {
		R -= other.R;
		G -= other.G;
		B -= other.B;
		
		R = Math.max(R, cutoff);
		G = Math.max(G, cutoff);
		B = Math.max(B, cutoff);
	}
	
	public RGB normalized() {
		float f = 1.0f / (R + G + B);
		return new RGB(R*f, G*f, B*f);
	}
}
