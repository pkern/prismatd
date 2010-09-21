package com.avona.games.towerdefence;

// TODO -- maybe use DelayedRunnable here...
public class RechargeTimer {
	public double time;
	public double timeRemaining;

	public RechargeTimer(double time) {
		this.time = time;
		this.timeRemaining = 0.0;
	}

	public RechargeTimer(RechargeTimer t) {
		this.time = t.time;
		this.timeRemaining = t.timeRemaining;
	}

	public boolean isReady() {
		return this.timeRemaining <= 0.0;
	}

	public void rearm() {
		this.timeRemaining = this.time;
	}

	public void step(double dt) {
		this.timeRemaining -= dt;
	}
}
