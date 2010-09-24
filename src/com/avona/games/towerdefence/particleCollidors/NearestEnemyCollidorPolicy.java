package com.avona.games.towerdefence.particleCollidors;

import java.util.List;

import com.avona.games.towerdefence.Enemy;
import com.avona.games.towerdefence.Particle;

public class NearestEnemyCollidorPolicy implements ParticleCollidorPolicy {

	@Override
	public void collideParticleWithEnemies(final Particle p,
			final List<Enemy> enemies, final float dt) {
		for (Enemy e : enemies) {
			if (e.isDead()) {
				continue;
			}

			if (p.collidedWith(e, dt)) {
				p.attack(e);

				if (p.isDead()) {
					// Particle exploded, don't use it any more.
					return;
				}
				// Note: The enemy might be dead at this point.
			}
		}
	}
}