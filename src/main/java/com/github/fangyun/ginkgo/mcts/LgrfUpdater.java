package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.NonStoneColor.*;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.feature.LgrfTable;

/** 在一个棋局结束后更新LGRF表. (同样更新树.) */
public final class LgrfUpdater implements TreeUpdater {

	private final LgrfTable table;

	private final TreeUpdater updater;

	public LgrfUpdater(TreeUpdater updater, LgrfTable table) {
		this.updater = updater;
		this.table = table;
	}

	@Override
	public void clear() {
		table.clear();
		updater.clear();
	}

	@Override
	public int getGestation() {
		return updater.getGestation();
	}

	@Override
	public SearchNode getRoot() {
		return updater.getRoot();
	}

	/** 测试用. */
	LgrfTable getTable() {
		return table;
	}

	@Override
	public void updateForAcceptMove() {
		updater.updateForAcceptMove();
	}

	@Override
	public void updateTree(Color winner, McRunnable runnable) {
		updater.updateTree(winner, runnable);
		HistoryObserver history = runnable.getHistoryObserver();
		if (winner != VACANT) {
			Board playerBoard = runnable.getPlayer().getBoard();
			int turn = runnable.getTurn();
			boolean win = winner == playerBoard.getColorToPlay();
			StoneColor color = playerBoard.getColorToPlay();
			int t = playerBoard.getTurn();
			short penultimate = history.get(t - 2);
			short previous = history.get(t - 1);
			for (; t < turn; t++) {
				short reply = history.get(t);
				table.update(color, win, penultimate, previous, reply);
				win = !win;
				penultimate = previous;
				previous = reply;
				color = color.opposite();
			}
		}
	}

}
