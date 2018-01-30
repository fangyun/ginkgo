package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static java.lang.String.format;

import java.util.Arrays;

import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 包含RAVE信息. */
public final class RaveNode extends SimpleSearchNode {

	/** 在这个节点的每个子节点上运行RAVE的次数. */
	private final int[] raveRuns;

	/** 通过这个节点的子节点的RAVE赢率. */
	private final float[] raveWinRates;

	public RaveNode(CoordinateSystem coords) {
		super(coords);
		raveRuns = new int[coords.getFirstPointBeyondBoard()];
		raveWinRates = new float[coords.getFirstPointBeyondBoard()];
	}

	/** 为p增加了一个RAVE的失败. */
	public void addRaveLoss(short p) {
		addRaveRun(p, 0);
	}

	/**
	 * 为p添加一个RAVE的棋局.
	 * 
	 * @param w
	 *            这场棋局的获胜几率，通常是0或1赢.
	 */
	public void addRaveRun(int p, float w) {
		raveWinRates[p] = (w + raveWinRates[p] * raveRuns[p]) / (1 + raveRuns[p]);
		raveRuns[p]++;
	}

	/** 为p增加了一个RAVE的赢. */
	public void addRaveWin(short p) {
		addRaveRun(p, 1);
	}

	@Override
	public void clear(long fancyHash, CoordinateSystem coords) {
		super.clear(fancyHash, coords);
		Arrays.fill(raveRuns, 2);
		Arrays.fill(raveWinRates, 0.5f);
	}

	/** 返回通过落子p的RAVE的运行数量. */
	public int getRaveRuns(short p) {
		return raveRuns[p];
	}

	/** 返回落子p的RAVE胜率. */
	public float getRaveWinRate(short p) {
		return raveWinRates[p];
	}

	/** 返回通过落子p的RAVE的胜的次数. */
	public float getRaveWins(int p) {
		return raveWinRates[p] * raveRuns[p];
	}

	@Override
	public void recordPlayout(float winProportion, McRunnable runnable, int t) {
		final ShortSet playedPoints = runnable.getPlayedPoints();
		super.recordPlayout(winProportion, runnable, t);
		playedPoints.clear();
		// The remaining moves in the sequence are recorded for RAVE
		while (t < runnable.getTurn()) {
			short move = runnable.getHistoryObserver().get(t);
			if (move != PASS && !playedPoints.contains(move)) {
				assert runnable.getBoard().getCoordinateSystem().isOnBoard(move);
				playedPoints.addKnownAbsent(move);
				addRaveRun(move, winProportion);
			}
			t++;
			if (t >= runnable.getTurn()) {
				return;
			}
			move = runnable.getHistoryObserver().get(t);
			playedPoints.add(move);
			t++;
		}
	}

	/**
	 * 类似于public版本，但将更简单的部分作为参数，以简化测试.
	 */
	void recordPlayout(float winProportion, short[] moves, int t, int turn, ShortSet playedPoints) {
		assert t < turn;
		short move = moves[t];
		update(move, 1, winProportion);
		while (t < turn) {
			move = moves[t];
			if (move != PASS && !playedPoints.contains(move)) {
				playedPoints.addKnownAbsent(move);
				addRaveRun(move, winProportion);
			}
			t++;
			if (t >= turn) {
				return;
			}
			move = moves[t];
			playedPoints.add(move);
			t++;
		}
	}

	@Override
	public String toString(CoordinateSystem coords) {
		String result = "Total runs: " + super.getTotalRuns() + "\n";
		for (final short p : coords.getAllPointsOnBoard()) {
			if (super.getRuns(p) > 2) {
				result += toString(p, coords);
			}
		}
		if (super.getRuns(PASS) > 10) {
			result += toString(PASS, coords);
		}
		return result;
	}

	@Override
	String toString(short p, CoordinateSystem coords) {
		return format("%s: %7d/%7d (%1.4f) RAVE %d (%1.4f)\n", coords.toString(p), (int) getWins(p), super.getRuns(p),
				super.getWinRate(p), raveRuns[p], raveWinRates[p]);
	}
}
