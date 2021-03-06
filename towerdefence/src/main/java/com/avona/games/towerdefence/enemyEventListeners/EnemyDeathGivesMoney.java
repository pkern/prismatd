package com.avona.games.towerdefence.enemyEventListeners;

import com.avona.games.towerdefence.Game;
import com.avona.games.towerdefence.enemy.Enemy;

public class EnemyDeathGivesMoney implements EnemyEventListener {
	private static final long serialVersionUID = 1L;

	private Game game;

	public EnemyDeathGivesMoney(Game game) {
		this.game = game;
	}

	@Override
	public void onDeathEvent(Enemy e) {
		game.money += e.worth;
	}

	@Override
	public void onEscapeEvent(Enemy e) {
		// Nothing to do.
	}
}
