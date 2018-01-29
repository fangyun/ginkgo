package com.github.fangyun.ginkgo.mcts;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.util.ListNode;

/** 在搜索树的一个节点. */
public interface SearchNode {

	/**
	 * 返回可读的统计在最优落子后.
	 */
	public String bestWinCountReport(CoordinateSystem coords);

	/**
	 * 返回在最优落子后的赢率.
	 */
	public short bestWinRate(CoordinateSystem coords);

	/** 返回是否已经为这节点更新了偏置. */
	public boolean biasUpdated();

	/**
	 * 重置节点为在哈希值代表的棋盘状态下的新节点.
	 */
	public void clear(long fancyHash, CoordinateSystem coords);

	/**
	 * 返回以此节点为根的子树的可读形式，最深maxDepth.
	 */
	public String deepToString(Board board, TranspositionTable table, int maxDepth);

	/**
	 * 标志落子p（例如非法落子）为可怕的, 因此它不会再次尝试.
	 */
	public void exclude(short p);

	/** 标志节点为无用，直到下次被重置. */
	public void free();

	/** 返回这节点的子节点. */
	public ListNode<SearchNode> getChildren();

	/**
	 * 返回存在该节点的棋盘状态的Zobrist哈希值.
	 */
	public long getFancyHash();

	/** 返回从此点的最优落子. */
	public short getMoveWithMostWins(CoordinateSystem coords);

	/** 返回经过落子p运行的数目. */
	public int getRuns(short p);

	/** 返回经过此节点总计数目. */
	public int getTotalRuns();

	/**
	 * 如果导致赢，则返回从此节点的最近的落子，否则为NO_POINT.
	 */
	public short getWinningMove();

	/** 返回对于落子p通过此节点的赢率. */
	public float getWinRate(short p);

	/** 返回通过落子p的赢的数. */
	public float getWins(short p);

	/**
	 * 返回true对于那些落子通过另一个已经创建的节点.
	 */
	public boolean hasChild(short p);

	/**
	 * 返回true，如果此节点还没有体验任何棋局(不是初始化的偏置棋局).
	 */
	public boolean isFresh(CoordinateSystem coords);

	/**
	 * 返回true，如果节点在用(例如, 被重置自从最近被释放).
	 */
	public boolean isInUse();

	/**
	 * 返回true，如果节点被标识过。用于垃圾收集.
	 */
	public boolean isMarked();

	/**
	 * 返回从此节点的所有落子的总赢率.
	 */
	public float overallWinRate(CoordinateSystem coords);

	/**
	 * 增加数目为从一棋局的落子序列结果.
	 *
	 * 注: 因为此方法不是同步，对同一个节点的两个并发调用可能导致竟跑条件影响设置赢的落子域.
	 *
	 * @param winProportion
	 *            1.0 如果在这节点是个赢的棋局，0.0反之.
	 * @param runnable
	 *            McRunnable响应这次运行.
	 * @param t
	 *            首次落子的下标(从此节点开始).
	 */
	public void recordPlayout(float winProportion, McRunnable runnable, int t);

	/** 设置对这节点是否偏置已经被更新. */
	public void setBiasUpdated(boolean value);

	/** 设置此节点的子列表. */
	public void setChildren(ListNode<SearchNode> children);

	/** 表示落子p已经遍历过. */
	public void setHasChild(short p);

	/** 设置节点的标识，为垃圾收集用. */
	public void setMarked(boolean marked);

	/** 设置为此节点的赢的落子. */
	public void setWinningMove(short move);

	/** 设置此节点可读形式. */
	public String toString(CoordinateSystem coords);

	/**
	 * 更新点p的赢率, 通过增加n运行和指定的赢率. 同样更新总共运行和当前运行次数.
	 */
	public void update(short p, int n, float wins);

	/**
	 * 通过McRunnable建议提供额外的赢.
	 */
	public void updateBias(McRunnable runnable);

}