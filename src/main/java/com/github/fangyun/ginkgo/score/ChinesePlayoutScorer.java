package com.github.fangyun.ginkgo.score;

import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/**
 * 用中国规则计分.
 */
public final class ChinesePlayoutScorer implements PlayoutScorer {
	private static final long serialVersionUID = -6053051581794964985L;

	private final Board board;

	/**
	 * 白棋得的贴目数. 为了速度而存为负数.
	 */
	private final double komi;

	public ChinesePlayoutScorer(Board board, double komi) {
		this.board = board;
		this.komi = -komi;
	}

	@Override
	public double getKomi() {
		return -komi;
	}

	@Override
	public double score() {
		final CoordinateSystem coords = board.getCoordinateSystem();
		double result = komi;
		for (final short p : coords.getAllPointsOnBoard()) {
			final Color color = board.getColorAt(p);
			if (color == BLACK) {
				result++;
			} else if (color == WHITE) {
				result--;
			} else {
				if (board.hasMaxNeighborsForColor(BLACK, p)) {
					result++;
				} else if (board.hasMaxNeighborsForColor(WHITE, p)) {
					result--;
				}
			}
		}
		return result;
	}

	@Override
	public Color winner() {
		final double score = score();
		if (score > 0) {
			return BLACK;
		} else if (score < 0) {
			return WHITE;
		}
		return VACANT;
	}
}
