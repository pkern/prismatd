package com.avona.games.towerdefence.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Vibrator;

import com.avona.games.towerdefence.Game;
import com.avona.games.towerdefence.PortableMainLoop;
import com.avona.games.towerdefence.gfx.PortableGraphicsEngine;
import com.avona.games.towerdefence.res.ResourceResolverRegistry;

public class MainLoop extends PortableMainLoop {
	private static final long serialVersionUID = 1L;

	public GLSurfaceView surfaceView;

	public MainLoop(Context context, Vibrator vibrator) {
		super();

		game = new Game(eventListener);
		initWithGame();

		ResourceResolverRegistry.setInstance(new AndroidResourceResolver(
				context.getResources()));

		final AndroidDisplay display = new AndroidDisplay(displayEventListener);
		ge = new PortableGraphicsEngine(display, game, mouse, layerHerder, this);
		displayEventListener.add(ge);

		setupInputActors();

		eventListener.listeners.add(new AndroidEventListener(vibrator));

		surfaceView = new InputForwardingGLSurfaceView(context, inputActor, display);
		surfaceView.setRenderer(new GameRenderProxy(this, display));
	}

	@Override
	public void exit() {
		// TODO Determine how an application properly exits in Android.
	}
}
