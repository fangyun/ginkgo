package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.util.ShortList;

/**
 * 记忆在棋盘上落子的序列(不包括初始化的摆棋).
 */
public final class HistoryObserver implements BoardObserver {
	private static final long serialVersionUID = 3806133012873085011L;

	private final Board board;

	/** 落子序列. */
	private final ShortList history;

	public HistoryObserver(Board board) {
		this.board = board;
		final CoordinateSystem coords = board.getCoordinateSystem();
		history = new ShortList(coords.getMaxMovesPerGame());
		board.addObserver(this);
	}

	/**
	 * 产生一个HistoryObserver并不实际观察任何棋盘. 这是有用的对于拷贝数据从其它的HistoryObserver.
	 */
	public HistoryObserver(CoordinateSystem coords) {
		board = null;
		history = new ShortList(coords.getMaxMovesPerGame());
	}

	@Override
	public void clear() {
		history.clear();
	}

	@Override
	public void copyDataFrom(BoardObserver that) {
		final HistoryObserver original = (HistoryObserver) that;
		history.copyDataFrom(original.history);
	}

	/** 返回在时刻t的落子. 如果t < 0, 返回NO_POINT. */
	public short get(int t) {
		if (t < 0) {
			return NO_POINT;
		}
		return history.get(t);
	}

	public int size() {
		return history.size();
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		final CoordinateSystem coords = board.getCoordinateSystem();
		result.append("[");
		if (history.size() > 0) {
			result.append(coords.toString(history.get(0)));
			for (int t = 1; t < history.size(); t++) {
				result.append(", ");
				result.append(coords.toString(history.get(t)));
			}
		}
		result.append("]");
		return result.toString();
	}

	@Override
	public void update(StoneColor color, short location, ShortList capturedStones) {
		assert location == CoordinateSystem.PASS || board.getCoordinateSystem().isOnBoard(location);
		if (board.getTurn() > 0) {
			history.add(location);
		}
	}
}
