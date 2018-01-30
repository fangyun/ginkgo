package com.github.fangyun.ginkgo.time;

import static java.lang.Math.max;
import com.github.fangyun.ginkgo.core.Board;

/** 根据棋局中剩余的落子数的估计来管理时间. */
public final class UniformTimeManager implements TimeManager {

	/** 用在getMsec的常量. */
	private static final double TIME_CONSTANT = 0.2;

	/** True如果这个回合我们已经考虑过. */
	private boolean alreadyThought;

	private final Board board;

	/** 整个棋局的剩余时间. */
	private int msecRemaining;

	public UniformTimeManager(Board board) {
		this.board = board;
	}

	@Override
	public int getMsec() {
		if (!alreadyThought) {
			final int movesLeft = max(10,
					(int) (board.getVacantPoints().size() * TIME_CONSTANT));
			alreadyThought = true;
			return max(1, msecRemaining / movesLeft);
		}
		return 0;
	}

	@Override
	public void setRemainingSeconds(int seconds) {
		// The subtraction ensures that we don't run out of time due to lag
		msecRemaining = max(1, (seconds - 10) * 1000);
	}

	@Override
	public void startNewTurn() {
		alreadyThought = false;
	}
}
