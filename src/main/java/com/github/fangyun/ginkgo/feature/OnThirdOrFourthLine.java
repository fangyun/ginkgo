package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.MAX_POSSIBLE_BOARD_WIDTH;
import static java.lang.Math.min;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/** True如果p在第三或第四线上. */
public final class OnThirdOrFourthLine implements Predicate {
	private static final long serialVersionUID = -4096254800178638761L;

	/** 各种棋盘宽度的实例. */
	private static final OnThirdOrFourthLine[] INSTANCES = new OnThirdOrFourthLine[MAX_POSSIBLE_BOARD_WIDTH + 1];

	/** 根据棋盘的宽度返回唯一的OnThirdOrFourthLine实例. */
	public static OnThirdOrFourthLine forWidth(int width) {
		final CoordinateSystem coords = CoordinateSystem.forWidth(width);
		if (INSTANCES[width] == null) {
			INSTANCES[width] = new OnThirdOrFourthLine(coords);
		}
		return INSTANCES[width];
	}

	/**
	 * 返回点p的线(1-基)从棋盘边缘
	 */
	private static int line(short p, CoordinateSystem coords) {
		int r = coords.row(p);
		r = min(r, coords.getWidth() - r - 1);
		int c = coords.column(p);
		c = min(c, coords.getWidth() - c - 1);
		return 1 + min(r, c);
	}

	/** True 如果点在第三或第四线上. */
	private final boolean[] bits;

	private final int width;

	private OnThirdOrFourthLine(CoordinateSystem coords) {
		width = coords.getWidth();
		bits = new boolean[coords.getFirstPointBeyondBoard()];
		for (final short p : coords.getAllPointsOnBoard()) {
			final int line = line(p, coords);
			if (line == 3 | line == 4) {
				bits[p] = true;
			}
		}
	}

	@Override
	public boolean at(short p) {
		return bits[p];
	}

	/**
	 * 使用这样的串行化，就像在CopiableStructure结构中所使用的那样，不会产生冗余的OnThirdOrFourthLine对象.
	 *
	 * @see com.github.fangyun.ginkgo.mcts.CopiableStructure
	 */
	private Object readResolve() {
		return forWidth(width);
	}
}
