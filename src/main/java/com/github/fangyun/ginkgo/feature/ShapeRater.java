package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.mcts.SearchNode;
import com.github.fangyun.ginkgo.patterns.PatternFinder;
import com.github.fangyun.ginkgo.patterns.ShapeTable;

/**
 * 这个类更新每个节点的子节点用基于SHAPE模式数据的偏置量.
 */
@SuppressWarnings("serial")
public class ShapeRater implements Rater {

	private final int bias;

	private final Board board;

	private final CoordinateSystem coords;

	private final HistoryObserver history;

	private final int minStones;

	private ShapeTable shapeTable;

	public ShapeRater(Board board, HistoryObserver history,
			ShapeTable shapeTable, int bias, int minStones) {
		this.bias = bias;
		this.board = board;
		this.history = history;
		this.coords = board.getCoordinateSystem();
		this.shapeTable = shapeTable;
		this.minStones = minStones;
	}

	public void setTable(ShapeTable table) {
		shapeTable = table;
	}

	@Override
	public void updateNode(SearchNode node) {
		for (short p : coords.getAllPointsOnBoard()) {
			if (board.getColorAt(p) == VACANT) {
				long hash = PatternFinder.getHash(board, p, minStones,
						history.get(board.getTurn() - 1));
				node.update(p, bias, (int) (bias * shapeTable.getWinRate(hash)));
			}
		}
	}
}
