package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.FIRST_DIAGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.LAST_DIAGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.StoneColor;

/**
 * True，除非p是“像”棋色的眼，也就是说，被己方的棋子包围着，并且没有超过一个(在棋盘边缘零个)对角相邻的对手的棋子。在这种情况下落子几乎总是一个坏主意。点p被认为是空点。.
 */
public final class NotEyeLike implements Predicate {
	private static final long serialVersionUID = -7579484813160964564L;

	private final Board board;

	/**
	 * 在每一点上由于处于边缘而有效的对角邻居的数量。(这里是1边，在其他地方是0）
	 */
	private final int[] edgeEnemies;

	public NotEyeLike(Board board) {
		this.board = board;
		final CoordinateSystem coords = board.getCoordinateSystem();
		edgeEnemies = new int[coords.getFirstPointBeyondBoard()];
		for (final short p : coords.getAllPointsOnBoard()) {
			edgeEnemies[p] = 0;
			for (int i = FIRST_DIAGONAL_NEIGHBOR; i <= LAST_DIAGONAL_NEIGHBOR; i++) {
				final short n = coords.getNeighbors(p)[i];
				if (!coords.isOnBoard(n)) {
					edgeEnemies[p] = 1;
				}
			}
		}
	}

	@Override
	public boolean at(short p) {
		assert board.getColorAt(p) == VACANT;
		final StoneColor color = board.getColorToPlay();
		if (!board.hasMaxNeighborsForColor(color, p)) {
			return true;
		}
		int count = edgeEnemies[p];
		final StoneColor enemy = color.opposite();
		final short[] neighbors = board.getCoordinateSystem().getNeighbors(p);
		for (int i = FIRST_DIAGONAL_NEIGHBOR; i <= LAST_DIAGONAL_NEIGHBOR; i++) {
			if (board.getColorAt(neighbors[i]) == enemy) {
				count++;
				if (count >= 2) {
					return true;
				}
			}
		}
		return false;
	}
}
