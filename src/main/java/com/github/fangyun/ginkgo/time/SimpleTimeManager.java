package com.github.fangyun.ginkgo.time;

/** 每次落子时，总是分配相同的时间. */
public final class SimpleTimeManager implements TimeManager {

	/** true，如果这个回合我们已经考虑过. */
	private boolean alreadyThought;

	private final int msecPerMove;

	public SimpleTimeManager(int msecPerMove) {
		this.msecPerMove = msecPerMove;
	}

	@Override
	public int getMsec() {
		if (alreadyThought) {
			return 0;
		}
		alreadyThought = true;
		return msecPerMove;
	}

	@Override
	public void setRemainingSeconds(int seconds) {
		// Does nothing
	}

	@Override
	public void startNewTurn() {
		alreadyThought = false;
	}
}
