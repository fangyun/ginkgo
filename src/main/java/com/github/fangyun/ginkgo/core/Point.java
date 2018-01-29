package com.github.fangyun.ginkgo.core;

import static com.github.fangyun.ginkgo.core.NonStoneColor.*;
import static com.github.fangyun.ginkgo.core.StoneColor.*;

import java.io.Serializable;

import com.github.fangyun.ginkgo.util.*;

/**
 * 保有单点信息的类. 此类从棋盘中分离出来，使得该类更简单.因为是单点（而不是大数组）扇入扇出缓存，因此也可能更快。此类的域直接可以访问.
 * 
 * 注意，在Ginkgo的其它地方，点不是作为参数传入，而是使用原始的shorts.
 * 
 * @see com.github.fangyun.ginkgo.core.Board
 * @see com.github.fangyun.ginkgo.core.CoordinateSystem
 */
final class Point implements Serializable {
	private static final long serialVersionUID = 6073055955878061022L;

	/** 每个域最高占据的比特位数. */
	static final int FIELD_SIZE = 3;

	/** 和域一样宽度的组. */
	static final int MASK = (1 << FIELD_SIZE) - 1;

	/** 一个点能拥有的最大的邻居数量 */
	static final int MAX_NEIGHBORS = 4;

	/** 每个域被转变的比特位数. */
	static final int[] SHIFT = { 0 * FIELD_SIZE, 1 * FIELD_SIZE,
			2 * FIELD_SIZE };

	/**
	 * 此点链的标识("root"位置存储在此链中). 空白点的chainId是该点拥有的位置.
	 */
	short chainId;

	/**
	 * 此点的下一个的指针，链接点成为链.
	 */
	short chainNextPoint;

	/** 点的颜色. */
	Color color;

	/** 点的下标. */
	final short index;

	/** 点是链的root，此点的气. */
	final ShortSet liberties;

	/**
	 * 存储黑白空邻居的个数，每个计数使用3比特位来存储.
	 */
	int neighborCounts;

	/**
	 * 加黑白邻居个数，减去空邻居个数.
	 */
	static final int EDGE_INCREMENT = (1 << SHIFT[BLACK.index()])
			+ (1 << SHIFT[WHITE.index()]) - (1 << SHIFT[VACANT.index()]);

	/** 四个空邻居的点的计数. */
	public static final int FOUR_VACANT_NEIGHBORS = MAX_NEIGHBORS << SHIFT[VACANT
			.index()];

	/** 掩码指出每个颜色的最大邻居数. */
	static final int[] MAX_COLOR_MASK = {
			(MAX_NEIGHBORS << SHIFT[BLACK.index()]),
			(MAX_NEIGHBORS << SHIFT[WHITE.index()]) };

	/**
	 * 增加黑白原色到这种颜色并删除空邻居. 反之，减去一个棋子，增加空邻居.
	 */
	static final int[] NEIGHBOR_INCREMENT = {
			(1 << SHIFT[BLACK.index()]) - (1 << SHIFT[VACANT.index()]),
			(1 << SHIFT[WHITE.index()]) - (1 << SHIFT[VACANT.index()]) };

	Point(CoordinateSystem coords, short index) {
		this.index = index;
		if (coords.isOnBoard(index)) {
			short n = coords.getFirstPointBeyondBoard();
			liberties = new ShortSet(n);
		} else {
			liberties = null;
			color = OFF_BOARD;
		}
	}

	/** 增加落子到链中. */
	void addToChain(Point chain) {
		chainNextPoint = chain.chainNextPoint;
		chain.chainNextPoint = index;
		chainId = chain.index;
	}

	/**
	 * 创建单子链.
	 * 
	 * @param directLiberties
	 *            直接围绕此点的气.
	 */
	void becomeOneStoneChain(ShortSet directLiberties) {
		chainId = index;
		chainNextPoint = index;
		liberties.copyDataFrom(directLiberties);
	}

	/**
	 * 返回点到初始状态.仅在刚落子的点使用.
	 */
	void clear() {
		liberties.clear();
		color = VACANT;
		chainId = index;
		neighborCounts = FOUR_VACANT_NEIGHBORS;
	}

	/** 拷贝数据. */
	void copyDataFrom(Point that) {
		chainId = that.chainId;
		chainNextPoint = that.chainNextPoint;
		color = that.color;
		liberties.copyDataFrom(that.liberties);
		neighborCounts = that.neighborCounts;
	}

	/** 返回点的颜色c的邻居数. */
	public int getNeighborCount(Color c) {
		return (neighborCounts >> SHIFT[c.index()]) & MASK;
	}

	/**
	 * 如果点有颜色c的最大可能邻居数目，则返回true.
	 */
	public boolean hasMaxNeighborsForColor(Color c) {
		return (neighborCounts & MAX_COLOR_MASK[c.index()]) == MAX_COLOR_MASK[c
				.index()];
	}

	/**
	 * 如果点处于打吃状态，返回true. 假定此点是链的root.
	 */
	boolean isInAtari() {
		assert chainId == index;
		return liberties.size() == 1;
	}

}
