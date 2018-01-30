package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static java.lang.String.format;
import static java.util.Arrays.fill;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.feature.Rater;
import com.github.fangyun.ginkgo.feature.Suggester;
import com.github.fangyun.ginkgo.util.BitVector;
import com.github.fangyun.ginkgo.util.ListNode;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 在搜索树/转置表中的一个节点. */
public class SimpleSearchNode implements SearchNode {

	/**
	 * 当重置被调用时，一个虚手被赋予了这么多的运行，只有一个是胜利，不鼓励虚手除非所有其他的落子都是可怕的
	 */
	private static final int INITIAL_PASS_RUNS = 10;

	/** True 如果这个节点的偏置被设置了. */
	private boolean biasUpdated;

	/** 该节点的子节点. */
	private ListNode<SearchNode> children;

	/**
	 * 这个节点所代表的棋盘位置的Zobrist哈希值。这包含了简单的劫位置和棋子颜色。碰撞是如此罕见，以至于它们可以被忽略
	 */
	private long fancyHash;

	/**
	 * 对于那些与其他节点已经创建的位置相对应的位置是正确的。用于确定是否需要一个新子
	 */
	private final BitVector hasChild;

	/** 这个节点的每个子节点的运行次数. */
	private final int[] runs;

	/**
	 * 通过这个节点运行的总次数。对于不使用的节点，这个设置为-1.
	 */
	private int totalRuns;

	/** @see #getWinningMove() */
	private short winningMove;

	/** 通过这个节点的每个子节点的赢的次数. */
	private final float[] winRates;

	public SimpleSearchNode(CoordinateSystem coords) {
		runs = new int[coords.getFirstPointBeyondBoard()];
		winRates = new float[coords.getFirstPointBeyondBoard()];
		hasChild = new BitVector(coords.getFirstPointBeyondBoard());
		totalRuns = -1; // Indicates this node is not in use
	}

	@Override
	public String bestWinCountReport(CoordinateSystem coords) {
		final short best = getMoveWithMostWins(coords);
		return coords.toString(best) + " wins " + winRates[best] * runs[best] + "/" + runs[best] + " = "
				+ getWinRate(best);
	}

	@Override
	public short bestWinRate(CoordinateSystem coords) {
		short best = PASS;
		for (final short p : coords.getAllPointsOnBoard()) {
			if (getWinRate(p) >= getWinRate(best)) {
				best = p;
			}
		}
		return best;
	}

	@Override
	public boolean biasUpdated() {
		return biasUpdated;
	}

	@Override
	public void clear(long fancyHash, CoordinateSystem coords) {
		this.fancyHash = fancyHash;
		totalRuns = 2 * coords.getArea() + INITIAL_PASS_RUNS;
		fill(runs, 2);
		fill(winRates, 0.5f);
		hasChild.clear();
		// Make passing look very bad, so it will only be tried if all other
		// moves lose
		runs[PASS] = 10;
		winRates[PASS] = 1.0f / INITIAL_PASS_RUNS;
		children = null;
		winningMove = NO_POINT;
	}

	@Override
	public String deepToString(Board board, TranspositionTable table, int maxDepth) {
		return deepToString(board, table, maxDepth, 0);
	}

	/**
	 * 递归的辅助方法.
	 *
	 * @see #deepToString(Board, TranspositionTable, int)
	 */
	String deepToString(Board board, TranspositionTable table, int maxDepth, int depth) {
		final CoordinateSystem coords = board.getCoordinateSystem();
		if (maxDepth < depth) {
			return "";
		}
		String indent = "";
		for (int i = 0; i < depth; i++) {
			indent += "  ";
		}
		String result = indent + "Total runs: " + getTotalRuns() + "\n";
		final Board childBoard = new Board(coords.getWidth());
		for (final short p : coords.getAllPointsOnBoard()) {
			if (hasChild(p)) {
				result += indent + toString(p, coords);
				childBoard.copyDataFrom(board);
				childBoard.play(p);
				final SimpleSearchNode child = (SimpleSearchNode) table.findIfPresent(childBoard.getFancyHash());
				if (child != null) {
					result += child.deepToString(childBoard, table, maxDepth, depth + 1);
				}
			}
		}
		final short p = PASS;
		if (hasChild(p)) {
			result += indent + toString(p, coords);
			childBoard.copyDataFrom(board);
			childBoard.play(p);
			final SearchNode child = table.findIfPresent(childBoard.getFancyHash());
			if (child != null) {
				result += deepToString(childBoard, table, maxDepth, depth + 1);
			}
		}
		return result;
	}

	@Override
	public synchronized void exclude(short p) {
		winRates[p] = -1;
	}

	@Override
	public void free() {
		totalRuns = -1;
	}

	@Override
	public ListNode<SearchNode> getChildren() {
		return children;
	}

	@Override
	public long getFancyHash() {
		return fancyHash;
	}

	@Override
	public short getMoveWithMostWins(CoordinateSystem coords) {
		short best = PASS;
		for (final short p : coords.getAllPointsOnBoard()) {
			if (getWins(p) >= getWins(best)) {
				best = p;
			}
		}
		return best;
	}

	@Override
	public int getRuns(short p) {
		return runs[p];
	}

	@Override
	public int getTotalRuns() {
		return totalRuns;
	}

	@Override
	public short getWinningMove() {
		return winningMove;
	}

	@Override
	public float getWinRate(short p) {
		return winRates[p];
	}

	@Override
	public float getWins(short p) {
		return winRates[p] * runs[p];
	}

	@Override
	public boolean hasChild(short p) {
		return hasChild.get(p);
	}

	@Override
	public boolean isFresh(CoordinateSystem coords) {
		return totalRuns == 2 * coords.getArea() + INITIAL_PASS_RUNS;
	}

	@Override
	public boolean isInUse() {
		return totalRuns >= 0;
	}

	@Override
	public boolean isMarked() {
		return hasChild.get(NO_POINT);
	}

	@Override
	public float overallWinRate(CoordinateSystem coords) {
		int r = 0; // Runs
		int w = 0; // Wins
		for (final short p : coords.getAllPointsOnBoard()) {
			if (getWins(p) > 0) {
				w += getWins(p);
				r += getRuns(p);
			}
		}
		w += getWins(PASS);
		r += getRuns(PASS);
		return 1.0f * w / r;
	}

	@Override
	public void recordPlayout(float winProportion, McRunnable runnable, int t) {
		final int turn = runnable.getTurn();
		final HistoryObserver history = runnable.getHistoryObserver();
		assert t < turn : "t = " + t + " >= turn = " + turn;
		final short move = history.get(t);
		update(move, 1, winProportion);
		if (winProportion == 1) {
			winningMove = move;
		} else {
			winningMove = NO_POINT;
		}
	}

	/**
	 * 类似于public版本，但将更简单的部分作为参数，以简化测试.
	 */
	void recordPlayout(float winProportion, short[] moves, int t, int turn) {
		assert t < turn;
		final short move = moves[t];
		update(move, 1, winProportion);
		if (winProportion == 1) {
			winningMove = move;
		} else {
			winningMove = NO_POINT;
		}
	}

	@Override
	public void setBiasUpdated(boolean value) {
		biasUpdated = value;
	}

	@Override
	public void setChildren(ListNode<SearchNode> children) {
		this.children = children;
	}

	@Override
	public void setHasChild(short p) {
		hasChild.set(p, true);
	}

	@Override
	public void setMarked(boolean marked) {
		hasChild.set(NO_POINT, marked);
	}

	@Override
	public void setWinningMove(short move) {
		winningMove = move;
	}

	@Override
	public String toString(CoordinateSystem coords) {
		String result = "Total runs: " + totalRuns + "\n";
		for (final short p : coords.getAllPointsOnBoard()) {
			if (runs[p] > 2) {
				result += toString(p, coords);
			}
		}
		if (runs[PASS] > 10) {
			result += toString(PASS, coords);
		}
		return result;
	}

	String toString(short p, CoordinateSystem coords) {
		return format("%s: %7d/%7d (%1.4f)\n", coords.toString(p), (int) getWins(p), runs[p], winRates[p]);
	}

	@Override
	public synchronized void update(short p, int n, float wins) {
		if (winRates[p] > 0.0) {
			totalRuns += n;
			winRates[p] = (wins + winRates[p] * runs[p]) / (n + runs[p]);
			runs[p] += n;
		}
	}

	@Override
	public void updateBias(McRunnable runnable) {
		final Suggester[] suggesters = runnable.getSuggesters();
		for (int i = 0; i < suggesters.length; i++) {
			int bias = suggesters[i].getBias();
			final ShortSet moves = suggesters[i].getMoves();
			for (int j = 0; j < moves.size(); j++) {
				final short p = moves.get(j);
				update(p, bias, bias);
			}
		}
		Rater[] raters = runnable.getRaters();
		for (Rater rater : raters) {
			rater.updateNode(this);
		}
		setBiasUpdated(true);
	}
}
