package com.avona.games.towerdefence.enemySelection;

import java.util.List;

import com.avona.games.towerdefence.RGB;
import com.avona.games.towerdefence.enemy.Enemy;
import com.avona.games.towerdefence.tower.Tower;

public class NearestEnemyWithColourPolicy implements EnemySelectionPolicy {
	private static final long serialVersionUID = 1L;

	private boolean withRed;
	private boolean withGreen;
	private boolean withBlue;

	public NearestEnemyWithColourPolicy(boolean withRed, boolean withGreen,
			boolean withBlue) {
		this.withRed = withRed;
		this.withGreen = withGreen;
		this.withBlue = withBlue;
	}

	final private boolean enemyHasOurColours(final Enemy e) {
		final RGB l = e.life;
		return (withRed && l.R > 0) || (withGreen && l.G > 0)
				|| (withBlue && l.B > 0);
	}

	@Override
	public Enemy findSuitableEnemy(Tower t, List<Enemy> enemies) {
		Enemy bestEnemy = null;
		float bestEnemyLocationSquaredDist = Float.MAX_VALUE;
		final float squaredRange = t.range * t.range;

		for (Enemy e : enemies) {
			// Skip enemies that don't contain the colours we can affect.
			if (!enemyHasOurColours(e))
				continue;
			final float newEnemyLocationSquaredDist = t.location
					.squaredDist(e.location);
			// Within range?
			if (newEnemyLocationSquaredDist < squaredRange) {
				// Shoot to nearest enemy.
				if (bestEnemyLocationSquaredDist > newEnemyLocationSquaredDist) {
					bestEnemy = e;
					bestEnemyLocationSquaredDist = newEnemyLocationSquaredDist;
				}
			}
		}
		return bestEnemy;
	}
}
