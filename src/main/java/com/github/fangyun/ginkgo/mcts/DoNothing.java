package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import com.github.fangyun.ginkgo.book.OpeningBook;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;

/** 提供各种接口实现，但不无任何操作. */
public final class DoNothing implements TreeDescender, TreeUpdater, OpeningBook {

	@Override
	public short bestPlayMove() {
		// Does nothing
		return -1;
	}

	@Override
	public void clear() {
		// Does nothing
	}

	@Override
	public void descend(McRunnable runnable) {
		// Does nothing
	}

	@Override
	public void fakeDescend(McRunnable runnable, short... moves) {
		// Does nothing
	}

	@Override
	public int getBiasDelay() {
		return 0;
	}

	@Override
	public int getGestation() {
		return 0;
	}

	@Override
	public SearchNode getRoot() {
		return null;
	}

	@Override
	public short nextMove(Board board) {
		return NO_POINT;
	}

	@Override
	public void updateForAcceptMove() {
		// Does nothing
	}

	@Override
	public void updateTree(Color winner, McRunnable mcRunnable) {
		// Does nothing
	}

	@Override
	public float searchValue(SearchNode node, short move) {
		return 0;
	}

}
