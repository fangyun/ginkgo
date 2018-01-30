package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.patterns.PatternFinder;
import com.github.fangyun.ginkgo.patterns.ShapeTable;

/** 在进行了一次棋局之后，更新形状表(和树). */
public class ShapeUpdater implements TreeUpdater {

	private final TreeUpdater updater;

	private final ShapeTable shapeTable;

	public ShapeUpdater(TreeUpdater updater, ShapeTable shapeTable) {
		this.updater = updater;
		this.shapeTable = shapeTable;
	}

	@Override
	public void clear() {
		updater.clear();
		// TODO What do we do here? Re-load the SHAPE tables from memory?
		// If so, some sort of "still clean" flag is probably in order.
	}

	@Override
	public int getGestation() {
		return updater.getGestation();
	}

	@Override
	public SearchNode getRoot() {
		return updater.getRoot();
	}

	@Override
	public void updateForAcceptMove() {
		updater.updateForAcceptMove();
	}

	@Override
	public void updateTree(Color winner, McRunnable runnable) {
		updater.updateTree(winner, runnable);
		if (winner != VACANT) {
			Board playerBoard = runnable.getPlayer().getBoard();
//			HistoryObserver history = new HistoryObserver(playerBoard.getCoordinateSystem());
			boolean win = winner == playerBoard.getColorToPlay();
			int turn = runnable.getTurn();
			Board board = runnable.getBoard();
			HistoryObserver history = new HistoryObserver(board.getCoordinateSystem());
			history.copyDataFrom(runnable.getHistoryObserver());
			board.copyDataFrom(playerBoard);
			int k = 0; // Don't gather data beyond a certain depth
			for (int t = playerBoard.getTurn(); t < turn; t++) {
				if (k == 20) {
					break;
				}
				k++;
				short p = history.get(t);
				// TODO Get rid of magic number 3
				long hash = PatternFinder.getHash(board, p, 3,
						history.get(t - 1));
				// TODO Make win a double or float, so we can incorporate
				// ties (winner == VACANT above).
				shapeTable.update(hash, win);
				// System.out.println("Playing " +
				// board.getCoordinateSystem().toString(p));
				board.play(p);
				win = !win;
			}
		}
	}

	public ShapeTable getTable() {
		return shapeTable;
	}

}
