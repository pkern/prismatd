package com.avona.games.towerdefence.gfx;

import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.avona.games.towerdefence.Enemy;
import com.avona.games.towerdefence.Game;
import com.avona.games.towerdefence.Layer;
import com.avona.games.towerdefence.LayerHerder;
import com.avona.games.towerdefence.Mouse;
import com.avona.games.towerdefence.Particle;
import com.avona.games.towerdefence.PortableMainLoop;
import com.avona.games.towerdefence.TickRater;
import com.avona.games.towerdefence.TimeTrack;
import com.avona.games.towerdefence.Tower;
import com.avona.games.towerdefence.V2;

public abstract class PortableGraphicsEngine {

	public static final int DEFAULT_HEIGHT = 480;
	public static final int DEFAULT_WIDTH = 675;

	public V2 size = new V2();

	public Layer menuLayer;
	public Layer gameLayer;

	protected TimeTrack graphicsTime;
	protected TickRater graphicsTickRater;
	protected Game game;
	protected Mouse mouse;

	private FloatBuffer vertexBuffer;
	private FloatBuffer colorBuffer;

	public PortableGraphicsEngine(Game game, Mouse mouse,
			LayerHerder layerHerder, TimeTrack graphicsTime) {
		this.game = game;
		this.mouse = mouse;
		this.graphicsTime = graphicsTime;
		this.graphicsTickRater = new TickRater(graphicsTime);

		gameLayer = layerHerder
				.findLayerByName(PortableMainLoop.GAME_LAYER_NAME);
		menuLayer = layerHerder
				.findLayerByName(PortableMainLoop.MENU_LAYER_NAME);

		vertexBuffer = GeometryHelper.allocateFloatBuffer(102 * 2);
		colorBuffer = GeometryHelper.allocateFloatBuffer(102 * 4);
	}

	public abstract void prepareTransformationForLayer(Layer layer);

	public abstract void resetTransformation();

	protected abstract void prepareScreen();

	protected abstract void drawTriangleStrip(final int vertices,
			final FloatBuffer vertexBuffer, final FloatBuffer colorBuffer);

	protected abstract void drawLine(final int vertices,
			final FloatBuffer vertexBuffer, final FloatBuffer colorBuffer);

	protected abstract void drawTriangleFan(final int vertices,
			final FloatBuffer vertexBuffer, final FloatBuffer colorBuffer);

	protected abstract void drawTriangles(final int numTriangles,
			final FloatBuffer coordBuffer, final FloatBuffer colorBuffer,
			final CharBuffer indexBuffer);

	public abstract void drawText(final String text, final double x,
			final double y, final float colR, final float colG,
			final float colB, final float colA);

	public abstract V2 getTextBounds(final String text);

	public void render(float gameDelta, float graphicsDelta) {
		graphicsTickRater.updateTickRate();

		prepareScreen();

		prepareTransformationForLayer(gameLayer);
		renderWorld();

		for (Enemy e : game.enemies) {
			renderEnemy(e);
		}
		for (Tower t : game.towers) {
			renderTower(t);
		}
		for (Particle p : game.particles) {
			renderParticle(p);
		}
		if (game.selectedObject != null) {
			if (game.selectedObject instanceof Tower) {
				final Tower t = (Tower) game.selectedObject;
				drawCircle(t.location.x, t.location.y, t.range, 1.0f, 1.0f,
						1.0f, 1.0f);
			}
		}
		if (game.draggingTower && game.selectedBuildTower != null) {
			final V2 l = gameLayer.convertToVirtual(mouse.location);
			final Tower t = game.selectedBuildTower;
			drawCircle(l.x, l.y, t.range, 1.0f, 1.0f, 1.0f, 1.0f);
		}
		resetTransformation();

		prepareTransformationForLayer(menuLayer);
		renderMenu();
		resetTransformation();

		renderStats();
		renderMouse();
	}

	protected void renderWorld() {
		vertexBuffer.position(0);
		GeometryHelper.boxVerticesAsTriangleStrip(0.0f, 0.0f,
				gameLayer.virtualRegion.x, gameLayer.virtualRegion.y,
				vertexBuffer);
		vertexBuffer.position(0);

		colorBuffer.position(0);
		// Top right
		colorBuffer.put(new float[] { 0.00f, 0.00f, 0.00f, 1.0f });
		// Lower right
		colorBuffer.put(new float[] { 0.60f, 0.83f, 0.91f, 1.0f });
		// Top left
		colorBuffer.put(new float[] { 0.34f, 0.81f, 0.89f, 1.0f });
		// Lower left
		colorBuffer.put(new float[] { 0.37f, 0.84f, 0.92f, 1.0f });
		colorBuffer.position(0);

		drawTriangleStrip(4, vertexBuffer, colorBuffer);

		// Draw the waypoints... top first...
		assert (vertexBuffer.capacity() >= game.world.waypoints.size() * 2 * 4);
		assert (colorBuffer.capacity() >= game.world.waypoints.size() * 4);
		assert (game.world.waypoints.size() > 1);

		// Start the triangle strip by drawing two points at the first
		// waypoint. TODO: This assumes that it's always starting at
		// the top!
		vertexBuffer.position(0);
		colorBuffer.position(0);
		final ArrayList<V2> waypoints = game.world.waypoints;
		int vertices = 0;
		V2 wp = waypoints.get(0);

		int oldPosition = vertexBuffer.position();
		vertexBuffer.put(wp.x - WAYPOINT_SPACING);
		vertexBuffer.put(wp.y);

		for (int i = 1; i < waypoints.size(); ++i)
			putWaypointVertices(waypoints, vertexBuffer, i);

		wp = waypoints.get(waypoints.size() - 1);
		vertexBuffer.put(wp.x + WAYPOINT_SPACING);
		vertexBuffer.put(wp.y);

		for (int i = 1; i < (vertexBuffer.position() - oldPosition) / 2; ++i) {
			colorBuffer.put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
			++vertices;
		}

		vertexBuffer.position(0);
		colorBuffer.position(0);

		drawTriangleStrip(vertices, vertexBuffer, colorBuffer);
	}

	private void putWaypointVertices(final ArrayList<V2> waypoints,
			final FloatBuffer vertexBuffer, final int index) {
		V2 previousWP;
		final V2 currentWP = waypoints.get(index);
		V2 nextWP;

		try {
			previousWP = waypoints.get(index - 1);
		} catch (IndexOutOfBoundsException e) {
			// get first one instead
			previousWP = waypoints.get(0);
		}

		try {
			nextWP = waypoints.get(index + 1);
		} catch (IndexOutOfBoundsException e) {
			// get last one instead
			nextWP = waypoints.get(waypoints.size() - 1);
		}

		if (previousWP.x < currentWP.x) {
			/**
			 * <pre>
			 * *--------------- ...
			 * 
			 * ---------------- ...
			 * </pre>
			 */
			if (nextWP.y < currentWP.y) {
				/**
				 * <pre>
				 * *------------------2
				 *                  X |
				 * ---------------1/4 3
				 *                |   |
				 * </pre>
				 */
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 1x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 1y
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 2x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 3x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 3y
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 1x=4x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 1y=4y
			} else if (nextWP.y > currentWP.y) {
				/**
				 * <pre>
				 *                |  |
				 * *--------------2  3
				 *                 X | 
				 * ------------------1
				 * </pre>
				 */
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 1x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 1y
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 2x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 3x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 3y
			} else {
				/**
				 * <pre>
				 * *--------------2
				 *                X
				 * ---------------1
				 * </pre>
				 */
				vertexBuffer.put(currentWP.x); // 1x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 1y
				vertexBuffer.put(currentWP.x); // 2x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
			}
		} else if (previousWP.x > currentWP.x) {
			/**
			 * <pre>
			 * -----------------*
			 * 
			 * ------------------
			 * </pre>
			 */
			if (nextWP.y < currentWP.y) {
				/**
				 * <pre>
				 * 2-----------------*
				 * | X
				 * 4  3--------------1
				 * |  |
				 * </pre>
				 */
				vertexBuffer.put(previousWP.x); // 1x
				vertexBuffer.put(previousWP.y - WAYPOINT_SPACING); // 1y
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 2x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 3x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 3y
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 4x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 4y
			} else if (nextWP.y > currentWP.y) {
				/**
				 * <pre>
				 * |  |
				 * 3  2--------------*
				 * | X
				 * 1------------------
				 * </pre>
				 */
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 1x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 1y
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 2x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 3x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 3y
			} else {
				/**
				 * <pre>
				 * 2------------------*
				 * X
				 * 1-------------------
				 * </pre>
				 */
				vertexBuffer.put(currentWP.x); // 1x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 1y
				vertexBuffer.put(currentWP.x); // 2x
				vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
			}
		} else {
			if (previousWP.y > currentWP.y) {
				/**
				 * <pre>
				 * *   1
				 * |   |
				 * |   |
				 * | X | TODO: 2 X 3
				 * 2   3
				 * </pre>
				 */
				vertexBuffer.put(previousWP.x + WAYPOINT_SPACING); // 1x
				vertexBuffer.put(previousWP.y); // 1y
				vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 2x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 2y
				vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 3x
				vertexBuffer.put(currentWP.y - WAYPOINT_SPACING); // 3y
				if (nextWP.x > currentWP.x) {
					/**
					 * <pre>
					 * *   1
					 * |   |
					 * |   4
					 * | X 
					 * 2---3
					 * </pre>
					 */
					vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 4x
					vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 4y
				} else if (nextWP.x < currentWP.x) {
					/**
					 * <pre>
					 * *   1
					 * |   |
					 * 4   |
					 *   X |
					 * 2---3
					 * </pre>
					 */
					vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 4x
					vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 4y
				}

			} else if (previousWP.y <= currentWP.y) {// do both, x=x,y=y is
				// illegal anyway
				if (nextWP.x > currentWP.x) {
					/**
					 * <pre>
					 * 1---2
					 * | X 
					 * |   |
					 * |   |
					 * *   |
					 * </pre>
					 */
					vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 1x
					vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 1y
					vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 2x
					vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
				} else if (nextWP.x <= currentWP.x) {
					/**
					 * <pre>
					 * 2---1
					 *   X |
					 * |   |
					 * |   |
					 * *   |
					 * </pre>
					 */
					vertexBuffer.put(currentWP.x + WAYPOINT_SPACING); // 1x
					vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 1y
					vertexBuffer.put(currentWP.x - WAYPOINT_SPACING); // 2x
					vertexBuffer.put(currentWP.y + WAYPOINT_SPACING); // 2y
				} else {
					throw new RuntimeException("Should not be reached.");
				}
			}
		}
	}

	private float WAYPOINT_SPACING = 4.0f;

	protected void renderMenu() {
		vertexBuffer.position(0);
		GeometryHelper.boxVerticesAsTriangleStrip(0.0f, 0.0f,
				menuLayer.virtualRegion.x, menuLayer.virtualRegion.y,
				vertexBuffer);
		vertexBuffer.position(0);

		colorBuffer.position(0);
		// Top right
		colorBuffer.put(new float[] { 0.2314f, 0.4275f, 0.8980f, 1.0f });
		// Lower right
		colorBuffer.put(new float[] { 0.4627f, 0.6863f, 0.9372f, 1.0f });
		// Top left
		colorBuffer.put(new float[] { 0.2314f, 0.4275f, 0.8980f, 1.0f });
		// Lower left
		colorBuffer.put(new float[] { 0.4627f, 0.6863f, 0.9372f, 1.0f });
		colorBuffer.position(0);

		drawTriangleStrip(4, vertexBuffer, colorBuffer);

		vertexBuffer.position(0);
		colorBuffer.position(0);

		vertexBuffer.put(0.0f);
		vertexBuffer.put(menuLayer.virtualRegion.y);
		colorBuffer.put(new float[] { 0.0f, 0.0f, 0.0f, 1.0f });

		vertexBuffer.put(0.0f);
		vertexBuffer.put(0.0f);
		colorBuffer.put(new float[] { 0.0f, 0.0f, 0.0f, 1.0f });

		vertexBuffer.position(0);
		colorBuffer.position(0);

		drawLine(2, vertexBuffer, colorBuffer);
	}

	public void renderEnemy(final Enemy e) {
		if (e.isDead())
			return;
		
		 float lightness = (e.lifeR + e.lifeG + e.lifeB) * 1.0f / (e.lifeMaxR + e.lifeMaxG + e.lifeMaxB);
		final float colgesfac = 1.0f / (e.lifeR + e.lifeG + e.lifeB);

		lightness = 1;
		
		final float cr = e.lifeR * colgesfac * lightness;
		final float cg = e.lifeG * colgesfac * lightness;
		final float cb = e.lifeB * colgesfac * lightness;

		final float width = 12;
		final V2 location = e.location;

		vertexBuffer.position(0);
		GeometryHelper.boxVerticesAsTriangleStrip(location.x - width / 2,
				location.y - width / 2, width, width, vertexBuffer);
		vertexBuffer.position(0);

		colorBuffer.position(0);
		// Top right
		colorBuffer.put(new float[] { cr * 1.0f, cg * 0.9f, cb * 0.9f, 1.0f });
		// Bottom right
		colorBuffer.put(new float[] { cr * 0.8f, cg * 0.6f, cb * 0.6f, 1.0f });
		// Top left
		colorBuffer.put(new float[] { cr * 0.6f, cg * 0.8f, cb * 0.8f, 1.0f });
		// Bottom left
		colorBuffer.put(new float[] { cr * 0.9f, cg * 1.0f, cb * 1.0f, 1.0f });
		colorBuffer.position(0);

		drawTriangleStrip(4, vertexBuffer, colorBuffer);
	}

	public void renderStats() {
		String towerString = "";
		if (game.selectedObject != null) {
			if (game.selectedObject instanceof Tower) {
				final Tower t = (Tower) game.selectedObject;
				towerString = String.format("tower lvl %d | ", t.level);
			} else if (game.selectedObject instanceof Enemy) {
				final Enemy e = (Enemy) game.selectedObject;
				towerString = String.format("enemy lvl %d, health R%d G%d B%d  /  R%d G%d B%d | ",
						e.level, e.lifeR, e.lifeG, e.lifeB, 
						e.lifeMaxR, e.lifeMaxG, e.lifeMaxB);
			}
		}
		final String fpsString = String.format(
				"%swave %d | %d killed | %d escaped | $%d | fps %.2f",
				towerString, game.currentWave != null ? game.currentWave
						.getLevel() : -1, game.killed, game.escaped,
				game.money, graphicsTickRater.tickRate);
		final V2 bounds = getTextBounds(fpsString);
		final float width = bounds.x + 4;
		final float height = bounds.y + 2;

		vertexBuffer.position(0);
		GeometryHelper.boxVerticesAsTriangleStrip(0.0f, 0.0f, width, height,
				vertexBuffer);
		vertexBuffer.position(0);

		colorBuffer.position(0);
		GeometryHelper.boxColoursAsTriangleStrip(0.0f, 0.0f, 0.0f, 0.2f,
				colorBuffer);
		colorBuffer.position(0);

		drawTriangleStrip(4, vertexBuffer, colorBuffer);

		drawText(fpsString, 2, 4, 1.0f, 1.0f, 1.0f, 1.0f);
	}

	public void renderTower(final Tower t) {
		final float width = t.radius;
		final V2 location = t.location;
		
		//final float lightness = 1;
		//final float colgesfac = 1.0f / (t.strengthR + t.strengthG + t.strengthB);

		//final float cr = p.strengthR * colgesfac * lightness;
		//final float cg = p.strengthG * colgesfac * lightness;
		//final float cb = p.strengthB * colgesfac * lightness;
		
		float cr, cg, cb; cr = cg = cb = 0;
		
		if (t.colors == 0) {
			cr = 1;
		}
		else if (t.colors == 1) {
			cg = 1;
		} 
		else {
			cb = 1;
		}

		vertexBuffer.position(0);
		GeometryHelper.boxVerticesAsTriangleStrip(location.x - width / 2,
				location.y - width / 2, width, width, vertexBuffer);
		vertexBuffer.position(0);

		colorBuffer.position(0);
		// Top right
		colorBuffer.put(new float[] { cr * 1.0f, cg * 0.9f, cb * 0.9f, 1.0f });
		// Bottom right
		colorBuffer.put(new float[] { cr * 0.8f, cg * 0.6f, cb * 0.6f, 1.0f });
		// Top left
		colorBuffer.put(new float[] { cr * 0.6f, cg * 0.8f, cb * 0.8f, 1.0f });
		// Bottom left
		colorBuffer.put(new float[] { cr * 0.9f, cg * 1.0f, cb * 1.0f, 1.0f });
		colorBuffer.position(0);

		drawTriangleStrip(4, vertexBuffer, colorBuffer);
	}

	public void renderParticle(final Particle p) {
		if (p.isDead())
			return;
		
		final float lightness = 1;
		final float colgesfac = 1.0f / (p.strengthR + p.strengthG + p.strengthB);

		final float cr = p.strengthR * colgesfac * lightness;
		final float cg = p.strengthG * colgesfac * lightness;
		final float cb = p.strengthB * colgesfac * lightness;

		final float width = 10;
		final V2 location = p.location;

		vertexBuffer.position(0);
		GeometryHelper.boxVerticesAsTriangleStrip(location.x - width / 2,
				location.y - width / 2, width, width, vertexBuffer);
		vertexBuffer.position(0);

		colorBuffer.position(0);
		// Top right
		colorBuffer.put(new float[] { cr * 1.0f, cg * 0.9f, cb * 0.9f, 1.0f });
		// Bottom right
		colorBuffer.put(new float[] { cr * 0.8f, cg * 0.6f, cb * 0.6f, 1.0f });
		// Top left
		colorBuffer.put(new float[] { cr * 0.6f, cg * 0.8f, cb * 0.8f, 1.0f });
		// Bottom left
		colorBuffer.put(new float[] { cr * 0.9f, cg * 1.0f, cb * 1.0f, 1.0f });
		colorBuffer.position(0);

		drawTriangleStrip(4, vertexBuffer, colorBuffer);

	}

	public void renderMouse() {
		if (!mouse.onScreen)
			return;
		final V2 p = mouse.location;
		final float col = 0.5f + 0.3f * (float) Math.abs(Math
				.sin(4 * graphicsTime.clock));
		drawFilledCircle(p.x, p.y, mouse.radius, 1.0f, 1.0f, 1.0f, col);
	}

	protected void onReshapeScreen() {
		final float gameFieldPercentage = gameLayer.virtualRegion.x
				/ (gameLayer.virtualRegion.x + menuLayer.virtualRegion.x);

		final float gameRatio = gameLayer.virtualRegion.x
				/ gameLayer.virtualRegion.y;

		gameLayer.region.y = size.y;
		gameLayer.region.x = gameRatio * gameLayer.region.y;
		if (gameLayer.region.x / size.x > gameFieldPercentage) {
			// Too wide, screen width is the limit.
			gameLayer.region.x = size.x * gameFieldPercentage;
			gameLayer.region.y = gameLayer.region.x / gameRatio;
		}
		gameLayer.offset.x = 0;
		gameLayer.offset.y = (size.y - gameLayer.region.y) * 0.5f;

		final float menuRatio = menuLayer.virtualRegion.x
				/ menuLayer.virtualRegion.y;

		final V2 remainingSize = new V2(size.x - gameLayer.offset.x
				- gameLayer.region.x, size.y);

		menuLayer.region.y = remainingSize.y;
		menuLayer.region.x = menuRatio * menuLayer.region.y;
		if (menuLayer.region.x > remainingSize.x) {
			// Too wide, screen width is the limit.
			menuLayer.region.x = remainingSize.x;
			menuLayer.region.y = menuLayer.region.x / menuRatio;
		}

		gameLayer.offset.x += (remainingSize.x - menuLayer.region.x) * .5f;

		menuLayer.offset.y = gameLayer.offset.y;
		menuLayer.offset.x = gameLayer.offset.x + gameLayer.region.x;
	}

	public void drawCircle(final float x, final float y, final float radius,
			final float colR, final float colG, final float colB,
			final float colA, final int segments) {

		final double angleStep = 2 * Math.PI / segments;
		final float[] colors = new float[] { colR, colG, colB, colA };

		assert (vertexBuffer.capacity() >= (segments + 2) * 2);
		assert (colorBuffer.capacity() >= (segments + 2) * 4);

		for (int i = 0; i < segments; ++i) {
			final double angle = i * angleStep;
			vertexBuffer.put((float) (x + (Math.cos(angle) * radius)));
			vertexBuffer.put((float) (y + (Math.sin(angle) * radius)));
			colorBuffer.put(colors);
		}
		// Close the circle.
		vertexBuffer.put((float) (x + (Math.cos(angleStep) * radius)));
		vertexBuffer.put((float) (y + (Math.sin(angleStep) * radius)));
		colorBuffer.put(colors);
	}

	public void drawCircle(final float x, final float y, final float radius,
			final float colR, final float colG, final float colB,
			final float colA) {

		final int segments = 100;

		vertexBuffer.position(0);
		colorBuffer.position(0);

		drawCircle(x, y, radius, colR, colG, colB, colA, segments);

		vertexBuffer.position(0);
		colorBuffer.position(0);

		drawLine(101, vertexBuffer, colorBuffer);
	}

	public void drawFilledCircle(final float x, final float y,
			final float radius, final float colR, final float colG,
			final float colB, final float colA) {

		final int segments = 100;

		vertexBuffer.position(0);
		colorBuffer.position(0);

		assert (vertexBuffer.capacity() >= (segments + 2) * 2);
		assert (colorBuffer.capacity() >= (segments + 2) * 4);

		vertexBuffer.put((float) x);
		vertexBuffer.put((float) y);
		colorBuffer.put(new float[] { colR, colG, colB, colA });

		drawCircle(x, y, radius, colR, colG, colB, colA, segments);

		vertexBuffer.position(0);
		colorBuffer.position(0);

		drawTriangleFan(101, vertexBuffer, colorBuffer);
	}
}