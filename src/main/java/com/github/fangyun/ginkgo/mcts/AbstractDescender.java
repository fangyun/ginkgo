package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.RESIGN;
import static com.github.fangyun.ginkgo.experiment.Logging.log;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;
import com.github.fangyun.ginkgo.util.ShortList;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 总是选择最佳胜率的落子，没有任何探索. */
public abstract class AbstractDescender implements TreeDescender {

	/** 如果我们的赢率低于这个，放弃. */
	public static final float RESIGN_PARAMETER = 0.1f;

	/**
	 * 除非有很多通过节点的运行，否则偏置不会被更新.
	 */
	private final int biasDelay;

	private final Board board;

	private final TranspositionTable table;

	public AbstractDescender(Board board, TranspositionTable table,
			int biasDelay) {
		this.board = board;
		this.table = table;
		this.biasDelay = biasDelay;
	}

	@Override
	public short bestPlayMove() {
		double mostWins = 1;
		short result = PASS;
		final ShortSet vacantPoints = board.getVacantPoints();
		final SearchNode root = getRoot();
		do {
			mostWins = root.getWins(PASS);
			// 如果在前一个循环中选择的移动是非法的(例如，因为它从来没有真正尝试过)，那么就把它扔掉
			if (result != PASS) {
				log("Rejected " + board.getCoordinateSystem().toString(result) + " as illegal");
				root.exclude(result);
				result = PASS;
			}
			for (int i = 0; i < vacantPoints.size(); i++) {
				final short move = vacantPoints.get(i);
				if (root.getWins(move) > mostWins) {
					mostWins = root.getWins(move);
					result = move;
				}
			}
		} while (result != PASS && !board.isLegal(result));
		// 考虑认输
		if (root.getWinRate(result) < RESIGN_PARAMETER) {
			return RESIGN;
		}
		log("Selected " + board.getCoordinateSystem().toString(result) + " with " + root.getWins(result) + " wins in " + root.getRuns(result) + " runs");
		return result;
	}

	/** 返回棋局从这里开始最佳的落子. */
	short bestSearchMove(SearchNode node, McRunnable runnable) {
		final Board runnableBoard = runnable.getBoard();
		final MersenneTwisterFast random = runnable.getRandom();
		short result = node.getWinningMove();
		if (result != NO_POINT && runnableBoard.isLegal(result)) {
			// 为了避免违反superko规则，需要进行isLegal()检查
			return result;
		}
		float bestSearchValue = searchValue(node, PASS);
		result = PASS;
		final ShortList candidates = runnable.getCandidates();
		candidates.clear();
		candidates.addAll(runnableBoard.getVacantPoints());
		while (candidates.size() > 0) {
			final short p = candidates.removeRandom(random);
			final float searchValue = searchValue(node, p);
			if (searchValue > bestSearchValue) {
				if (runnable.isFeasible(p) && runnableBoard.isLegal(p)) {
					bestSearchValue = searchValue;
					result = p;
				} else {
					node.exclude(p);
				}
			}
		} 
		return result;
	}

	@Override
	public void clear() {
		// Nothing to do; the TreeUpdater clears the table
	}

	/** 有些节点可能会更新它们的偏置. */
	@Override
	public void descend(McRunnable runnable) {
		SearchNode node = getRoot();
		assert node != null : "Fancy hash code: " + board.getFancyHash();
		while (runnable.getBoard().getPasses() < 2) {
			selectAndPlayMove(node, runnable);
			final SearchNode child = table.findIfPresent(runnable.getBoard()
					.getFancyHash());
			if (child == null) {
				return; // No child
			}
			if (child.getTotalRuns() > biasDelay && !child.biasUpdated()) {
				child.updateBias(runnable);
			}
			node = child;
		}
	}

	@Override
	public void fakeDescend(McRunnable runnable, short... moves) {
		runnable.copyDataFrom(board);
		final SearchNode node = getRoot();
		assert node != null : "Fancy hash code: " + board.getFancyHash();
		for (final short move : moves) {
			runnable.acceptMove(move);
			final SearchNode child = table.findIfPresent(runnable.getBoard()
					.getFancyHash());
			if (child == null) {
				return; // No child
			}
			if (child.getTotalRuns() > biasDelay && !child.biasUpdated()) {
				child.updateBias(runnable);
			}
		}
	}

	@Override
	public int getBiasDelay() {
		return biasDelay;
	}

	Board getBoard() {
		return board;
	}

	/** 返回根节点(如果需要，创建它). */
	SearchNode getRoot() {
		return table.findOrAllocate(board.getFancyHash());
	}

	TranspositionTable getTable() {
		return table;
	}


	/** 在搜索树中选择并下一个棋子. */
	short selectAndPlayMove(SearchNode node, McRunnable runnable) {
		final short move = bestSearchMove(node, runnable);
		runnable.acceptMove(move);
		return move;
	}

	@Override
	public String toString() {
		return getRoot().deepToString(board, table, 0);
	}

}
