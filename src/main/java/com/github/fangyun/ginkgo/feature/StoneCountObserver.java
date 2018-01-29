package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.score.Scorer;
import com.github.fangyun.ginkgo.util.ShortList;

/** 跟踪每种棋色有多少棋子. */
public final class StoneCountObserver implements BoardObserver {
	private static final long serialVersionUID = 7828152837803078723L;

	private final int[] counts;

	/** 如果黑子-白子 >= 此数, 黑子赢. */
	private final int blackMercyThreshold;

	/** 如果黑子-白子 <= 此数, 白子赢. */
	private final int whiteMercyThreshold;

	public StoneCountObserver(Board board, Scorer scorer) {
		counts = new int[2];
		final double komi = scorer.getKomi();
		final int base = Math.max(board.getCoordinateSystem().getArea() / 6, (int) (2 * komi));
		blackMercyThreshold = base + (int) (Math.ceil(komi));
		whiteMercyThreshold = -base + (int) (Math.floor(komi));
		board.addObserver(this);
	}

	@Override
	public void clear() {
		counts[0] = 0;
		counts[1] = 0;
	}

	@Override
	public void copyDataFrom(BoardObserver that) {
		final StoneCountObserver original = (StoneCountObserver) that;
		counts[0] = original.counts[0];
		counts[1] = original.counts[1];
	}

	/** 返回棋色的棋子数. */
	public int getCount(StoneColor color) {
		return counts[color.index()];
	}

	/**
	 * 如果一种棋色远多于对手棋色，返回这中棋色。如果没有返回null.
	 */
	public StoneColor mercyWinner() {
		final int difference = counts[BLACK.index()] - counts[WHITE.index()];
		if (difference >= blackMercyThreshold) {
			return BLACK;
		} else if (difference <= whiteMercyThreshold) {
			return WHITE;
		}
		return null;
	}

	@Override
	public void update(StoneColor color, short location, ShortList capturedStones) {
		if (location != PASS) {
			counts[color.index()]++;
			counts[color.opposite().index()] -= capturedStones.size();
		}
	}
}
