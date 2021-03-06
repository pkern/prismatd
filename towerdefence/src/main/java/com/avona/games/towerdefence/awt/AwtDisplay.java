package com.avona.games.towerdefence.awt;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.JOptionPane;

import com.avona.games.towerdefence.Layer;
import com.avona.games.towerdefence.V2;
import com.avona.games.towerdefence.gfx.Display;
import com.avona.games.towerdefence.gfx.DisplayEventListener;
import com.avona.games.towerdefence.gfx.PortableGraphicsEngine;
import com.avona.games.towerdefence.gfx.Texture;
import com.avona.games.towerdefence.gfx.VertexArray;
import com.jogamp.opengl.util.awt.TextRenderer;


import static com.jogamp.opengl.GL2.*;

/**
 * The GraphicsEngine object currently incorporates all drawing operations. It
 * will iterate over all in-game objects and call (possibly overloaded) class
 * methods to perform the GL calls. It will not touch any in-game state, though.
 */
public class AwtDisplay implements Display, GLEventListener {
	public Frame frame;
	public GLCanvas canvas;
	private GL2 gl;
	private TextRenderer renderer;
	private V2 size = new V2();
	private DisplayEventListener eventListener;

	public AwtDisplay(DisplayEventListener eventListener) {
		this.eventListener = eventListener; 
		renderer = new TextRenderer(new Font("Deja Vu Sans", Font.PLAIN, 12),
				true, true);
		setupGlCanvas();
		setupFrame();
	}

	private void setupGlCanvas() {
		GLCapabilities capabilities = new GLCapabilities(GLProfile.getDefault());
		capabilities.setDoubleBuffered(true);

		canvas = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);
		canvas.setAutoSwapBufferMode(true);
	}

	private void setupFrame() {
		frame = new Frame("Towerdefence");
		frame.add(canvas);
		frame.setSize(PortableGraphicsEngine.DEFAULT_WIDTH,
				PortableGraphicsEngine.DEFAULT_HEIGHT);
		frame.setBackground(Color.WHITE);
		frame.setCursor(java.awt.Toolkit.getDefaultToolkit()
				.createCustomCursor(
						new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR),
						new java.awt.Point(0, 0), "NOCURSOR"));
		frame.setVisible(true);
	}
	
	@Override
	public V2 getTextBounds(final String text) {
		Rectangle2D bounds = renderer.getBounds(text);
		return new V2((float) bounds.getWidth(), (float) bounds.getHeight());
	}

	@Override
	public void drawText(final String text, final double x, final double y,
			final float colR, final float colG, final float colB,
			final float colA) {
		renderer.beginRendering((int) size.x, (int) size.y);
		renderer.setColor(colR, colG, colB, colA);
		renderer.draw(text, (int) x, (int) y);
		renderer.endRendering();
	}

	@Override
	public void drawText(Layer layer, final String text, final double x, final double y,
			final float colR, final float colG, final float colB,
			final float colA) {
		final V2 pos = layer.convertToPhysical(new V2((float)x, (float)y));
		drawText(text, pos.x, pos.y, colR, colG, colB, colA);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
	}

	/*
	@Override
	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
		// Not implemented by JOGL.
	}
	*/

	@Override
	public void init(GLAutoDrawable drawable) {
		// We have a fresh GL context, retrieve reference.
		gl = (GL2)canvas.getGL();

		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		eventListener.onNewScreenContext();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		throw new RuntimeException("added for jogl2. unneeded(?).");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		size = new V2(width, height);

		gl.glViewport(0, 0, (int) size.x, (int) size.y);
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, width, 0, height, -1, 1);
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();

		eventListener.onReshapeScreen();
	}

	@Override
	public void prepareScreen() {
		// Paint background, clearing previous drawings.
		gl.glColor3d(0.0, 0.0, 0.0);
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void prepareTransformationForLayer(Layer layer) {
		gl.glPushMatrix();
		gl.glTranslatef(layer.offset.x, layer.offset.y, 0);
		gl.glScalef(layer.region.x / layer.virtualRegion.x, layer.region.y
				/ layer.virtualRegion.y, 1);
	}

	@Override
	public void resetTransformation() {
		gl.glPopMatrix();
	}

	@Override
	public void drawVertexArray(final VertexArray array) {
		assert array.coordBuffer != null;
		gl.glEnableClientState(GL_VERTEX_ARRAY);
		array.coordBuffer.position(0);
		gl.glVertexPointer(2, GL_FLOAT, 0, array.coordBuffer);
		if (array.hasColour) {
			assert array.colourBuffer != null;
			gl.glEnableClientState(GL_COLOR_ARRAY);
			array.colourBuffer.position(0);
			gl.glColorPointer(4, GL_FLOAT, 0, array.colourBuffer);
		}
		if (array.hasTexture) {
			assert array.textureBuffer != null;
			assert array.texture != null;
			array.textureBuffer.position(0);
			gl.glEnable(GL_TEXTURE_2D);
			gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
			gl.glBindTexture(GL_TEXTURE_2D, array.texture.textureId);
			gl.glTexCoordPointer(2, GL_FLOAT, 0, array.textureBuffer);
		}

		if (array.mode == VertexArray.Mode.TRIANGLE_FAN) {
			gl.glDrawArrays(GL_TRIANGLE_FAN, 0, array.numCoords);
		} else if (array.mode == VertexArray.Mode.TRIANGLE_STRIP) {
			gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, array.numCoords);
		} else if (array.mode == VertexArray.Mode.TRIANGLES) {
			assert array.indexBuffer != null;
			array.indexBuffer.position(0);
			gl.glDrawElements(GL_TRIANGLES, array.numIndexes,
					GL_UNSIGNED_SHORT, array.indexBuffer);
		} else if (array.mode == VertexArray.Mode.LINE_STRIP) {
			gl.glDrawArrays(GL_LINE_STRIP, 0, array.numCoords);
		}

		if (array.hasTexture) {
			gl.glBindTexture(GL_TEXTURE_2D, 0);
			gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
			gl.glDisable(GL_TEXTURE_2D);
		}
		if (array.hasColour) {
			gl.glDisableClientState(GL_COLOR_ARRAY);
		}
		gl.glDisableClientState(GL_VERTEX_ARRAY);
	}

	@Override
	public Texture allocateTexture() {
		assert gl != null;

		Texture texture = new AwtTexture(gl);

		int[] textures = new int[1];
		gl.glGenTextures(1, textures, 0);
		texture.textureId = textures[0];

		assert gl.glGetError() == 0;
		gl.glBindTexture(GL_TEXTURE_2D, texture.textureId);

		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
				GL_NEAREST);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
				GL_LINEAR);

		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
				GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
				GL_CLAMP_TO_EDGE);

		gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

		assert gl.glGetError() == 0;
		gl.glBindTexture(GL_TEXTURE_2D, 0);

		return texture;
	}

	@Override
	public V2 getSize() {
		return size;
	}

	@Override
	public int userSelectsAString(String title, String message, String[] strings) {
		int x = JOptionPane.showOptionDialog(
				null,
				message,
				title,
				0,
				0, null, strings, null);

		if (x == JOptionPane.CLOSED_OPTION) x = -1;
		return x;
	}
}
