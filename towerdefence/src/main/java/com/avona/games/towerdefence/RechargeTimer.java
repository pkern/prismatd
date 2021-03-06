package com.avona.games.towerdefence;

public class RechargeTimer extends TimedCode {
	private static final long serialVersionUID = 1L;

	private TimedCodeManager timedCodeManager;
	public float time;
	public boolean ready = true;

	public RechargeTimer(TimedCodeManager timedCodeManager, float time) {
		this.timedCodeManager = timedCodeManager;
		this.time = time;
	}

	public RechargeTimer(RechargeTimer t) {
		this.timedCodeManager = t.timedCodeManager;
		this.time = t.time;
	}

	@Override
	public RechargeTimer clone() {
		return new RechargeTimer(this);
	}

	public void rearm() {
		ready = false;
		timedCodeManager.addCode(time, this);
	}

	@Override
	public void execute() {
		ready = true;
	}
}
