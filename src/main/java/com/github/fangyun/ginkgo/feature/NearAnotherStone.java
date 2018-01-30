package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.MAX_POSSIBLE_BOARD_WIDTH;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/** True 如果p是“接近”另一棋子，例如，在一个大飞的落子中. */
public final class NearAnotherStone implements Predicate {
	private static final long serialVersionUID = 5221893898977649012L;

	/**
	 * 每个棋盘宽度的领域的值.
	 */
	private static final short[][][] NEIGHBORHOODS = new short[MAX_POSSIBLE_BOARD_WIDTH + 1][][];

	/** 相邻点的行和列偏移，向外扩展. */
	public static final short[][] OFFSETS = { { 0, -1 }, { 0, 1 }, { -1, 0 },
			{ 1, 0 }, { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 }, { -2, 0 },
			{ 2, 0 }, { 0, -2 }, { 0, 2 }, { -2, -1 }, { -2, 1 }, { -1, -2 },
			{ -1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 }, { 1, 2 }, { 2, 2 },
			{ 2, -2 }, { -2, 2 }, { -2, -2 }, { 3, 0 }, { -3, 0 }, { 0, -3 },
			{ 0, 3 }, { 3, 1 }, { 3, -1 }, { -1, -3 }, { 1, -3 }, { -3, -1 },
			{ -3, 1 }, { -1, 3 }, { 1, 3 } };

	/**
	 * 返回一个包含在一个大飞落子中的所有棋盘点坐标的数组.
	 */
	private static short[] findNeighborhood(short p, CoordinateSystem coords) {
		final int r = coords.row(p), c = coords.column(p);
		final short[] result = new short[OFFSETS.length];
		int count = 0;
		for (int i = 0; i < OFFSETS.length; i++) {
			final int rr = r + OFFSETS[i][0];
			final int cc = c + OFFSETS[i][1];
			if (coords.isValidOneDimensionalCoordinate(rr)
					&& coords.isValidOneDimensionalCoordinate(cc)) {
				result[count] = coords.at(rr, cc);
				count++;
			}
		}
		// Create a small array and copy the elements into it
		return java.util.Arrays.copyOf(result, count);
	}

	private final Board board;

	/**
	 * 大飞领域周围的点。第一个索引是指领域的定义.
	 */
	private final short[][] neighborhoods;

	public NearAnotherStone(Board board) {
		this.board = board;
		final CoordinateSystem coords = board.getCoordinateSystem();
		final int width = coords.getWidth();
		if (NEIGHBORHOODS[width] == null) {
			final short[] pointsOnBoard = coords.getAllPointsOnBoard();
			NEIGHBORHOODS[width] = new short[coords.getFirstPointBeyondBoard()][];
			for (final short p : pointsOnBoard) {
				NEIGHBORHOODS[width][p] = findNeighborhood(p, coords);
			}
		}
		neighborhoods = NEIGHBORHOODS[width];
	}

	@Override
	public boolean at(short p) {
		for (final short q : neighborhoods[p]) {
			if (board.getColorAt(q) != VACANT) {
				return true;
			}
		}
		return false;
	}

}
