package com.avona.games.towerdefence;

import java.util.List;

public class NearestEnemyPolicy implements EnemySelectionPolicy {

	@Override
	public Enemy findSuitableEnemy(Tower t, List<Enemy> enemies) {
		Enemy bestEnemy = null;
		float bestEnemyLocationSquaredDist = Float.MAX_VALUE;
		final float squaredRange = t.range * t.range;

		for (Enemy e : enemies) {
			final float newEnemyLocationSquaredDist = t.location
					.squaredDist(e.location);
			if (newEnemyLocationSquaredDist < squaredRange) {
				// shoot to nearest enemy
				if (bestEnemy == null
						|| bestEnemyLocationSquaredDist > newEnemyLocationSquaredDist) {
					bestEnemy = e;
					bestEnemyLocationSquaredDist = newEnemyLocationSquaredDist;
				}
			}
		}
		return bestEnemy;
	}
}
