package com.github.fangyun.ginkgo.core;

import static java.lang.Math.abs;

import java.io.Serializable;

import com.github.fangyun.ginkgo.mcts.CopiableStructure;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;

/**
 * 坐标系统用来转换短表示或其它表示位置方式. 没有公开的构造方法，而采用静态方法widthOf来获取正确实例.
 * <p>
 * 一个点体现为一个短表示. 这是一维数组表示的棋盘的一个索引值，数组在边缘带有哨兵点.
 * <p>
 * 遍历棋盘上所有点的标准方式：
 * 
 * <pre>
 * for (short p : getAllPointsOnBoard()) {
 * 	// Do something with p
 * }
 * </pre>
 *
 * 遍历点p的所有正交邻居的标准方式:
 *
 * <pre>
 * short[] neighbors = getNeighbors(p);
 * for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i &lt;= LAST_ORTHOGONAL_NEIGHBOR; i++) {
 * 	short n = neighbors[i];
 * 	// Do something with n, which might be an off-board point
 * }
 * </pre>
 *
 * 遍历斜线邻居，类似方式，除了使用DIAGONAL代替ORTHOGONAL.遍历两者，对邻居使用for-each循环.
 * <p>
 * 很少使用行列坐标，行从顶开始0基，列从左开始.
 */
public final class CoordinateSystem implements Serializable {
	private static final long serialVersionUID = 4776588534814612341L;

	/** 点的东边增加一点. */
	private static final short EAST = 1;

	/** 给getNeighbors返回的数组下标. */
	public static final int EAST_NEIGHBOR = 2;

	/**
	 * Ginkgo不支持更大的棋盘尺寸，这是个避免魔数的常量.
	 */
	public static final int MAX_POSSIBLE_BOARD_WIDTH = 19;

	/** 特定值表示没有点. */
	public static final short NO_POINT = 0;

	/** 给getNeighbors返回的数组下标. */
	public static final int NORTH_NEIGHBOR = 0;

	/** 给getNeighbors返回的数组下标. */
	public static final int NORTHEAST_NEIGHBOR = 5;

	/** 给getNeighbors返回的数组下标. */
	public static final int NORTHWEST_NEIGHBOR = 4;

	/** 特定值表示虚手. */
	public static final short PASS = 1;

	/** 特定值表示弃棋. */
	public static final short RESIGN = 2;

	/** 给getNeighbors返回的数组下标. */
	public static final int SOUTH_NEIGHBOR = 3;

	/** 给getNeighbors返回的数组下标. */
	public static final int SOUTHEAST_NEIGHBOR = 7;

	/** 给getNeighbors返回的数组下标. */
	public static final int SOUTHWEST_NEIGHBOR = 6;

	/** 给getNeighbors返回的数组下标. */
	public static final int WEST_NEIGHBOR = 1;

	/** 给getNeighbors返回的数组下标. */
	public static final int FIRST_DIAGONAL_NEIGHBOR = NORTHWEST_NEIGHBOR;

	/** 给getNeighbors返回的数组下标. */
	public static final int FIRST_ORTHOGONAL_NEIGHBOR = NORTH_NEIGHBOR;

	/** 各种棋盘尺寸的实例. */
	private static final CoordinateSystem[] INSTANCES = new CoordinateSystem[MAX_POSSIBLE_BOARD_WIDTH + 1];

	/** 给getNeighbors返回的数组下标. */
	public static final int LAST_DIAGONAL_NEIGHBOR = SOUTHEAST_NEIGHBOR;

	/** 给getNeighbors返回的数组下标. */
	public static final int LAST_ORTHOGONAL_NEIGHBOR = SOUTH_NEIGHBOR;

	/** 列c的字符串表示. */
	public static String columnToString(int column) {
		return "" + "ABCDEFGHJKLMNOPQRST".charAt(column);
	}

	/** 根据指定宽度返回唯一的CoordinateSystem. */
	public static CoordinateSystem forWidth(int width) {
		if (INSTANCES[width] == null) {
			INSTANCES[width] = new CoordinateSystem(width);
		}
		return INSTANCES[width];
	}

	/**
	 * @see #getAllPointsOnBoard()
	 */
	private final short[] allPointsOnBoard;

	/**
	 * @see #getMaxMovesPerGame()
	 */
	private final short maxMovesPerGame;

	/**
	 * @see #getNeighbors(short)
	 */
	private final short[][] neighbors;

	/** 点的南边增加一点. */
	private final short south;

	/** 棋盘宽度. */
	private final int width;

	/**
	 * 对Zobrist哈希的随机数,以颜色和点来索引. 最后行作为简单围棋点.
	 */
	private final long[][] zobristHashes;

	/** 其它类使用forWidth来获得实例. */
	private CoordinateSystem(int width) {
		this.width = width;
		south = (short) (width + 1);
		final short boardArea = (short) (width * width);
		allPointsOnBoard = new short[boardArea];
		for (int r = 0, i = 0; r < width; r++) {
			for (int c = 0; c < width; c++, i++) {
				allPointsOnBoard[i] = at(r, c);
			}
		}
		maxMovesPerGame = (short) (boardArea * 3);
		final int n = getFirstPointBeyondBoard();
		neighbors = new short[n][];
		zobristHashes = new long[2][n];
		final MersenneTwisterFast random = new MersenneTwisterFast(0L);
		for (final short p : allPointsOnBoard) {
			neighbors[p] = new short[] { (short) (p - south), (short) (p - EAST), (short) (p + EAST),
					(short) (p + south), (short) (p - south - EAST), (short) (p - south + EAST),
					(short) (p + south - EAST), (short) (p + south + EAST) }; //{N,W,E,S,WN,EN,WS,ES},上至下，左至右
			for (int i = 0; i < zobristHashes.length; i++) {
				zobristHashes[i][p] = random.nextLong();
			}
		}
	}

	/** 行r列c的short值形式. */
	public short at(int r, int c) {
		assert isValidOneDimensionalCoordinate(r) : "无效行: " + r;
		assert isValidOneDimensionalCoordinate(c) : "无效列: " + c;
		return (short) ((r + 1) * south + (c + 1) * EAST);
	}

	/**
	 * 返回使用标签描述的short值形式，例如可能的值有 "A5", "b3", or "PASS".
	 */
	public short at(String label) {
		label = label.toUpperCase();
		if (label.equals("PASS")) {
			return PASS;
		}
		if (label.equals("RESIGN")) {
			return RESIGN;
		}
		int r = Integer.parseInt(label.substring(1));
		r = width - r;
		int c;
		final char letter = label.charAt(0);
		if (letter <= 'H') {
			c = letter - 'A';
		} else {
			c = letter - 'B';
		}
		return at(r, c);
	}

	/** 返回点p的列. */
	public int column(short p) {
		return p % south - 1;
	}

	/**
	 * 返回在棋盘上的所有点，方便迭代.
	 */
	public short[] getAllPointsOnBoard() {
		return allPointsOnBoard;
	}

	/** 返回棋盘上点的数目. */
	public int getArea() {
		return width * width;
	}

	/**
	 * 返回超过棋盘的第一点的下标. 这是有用的，任意数组的尺寸对在棋盘上任何点有个入口.
	 */
	public short getFirstPointBeyondBoard() {
		return (short) (width * (south + EAST) + 1);
	}

	/**
	 * 返回超过棋盘外延的第一点下标(包括外延上的哨兵点）.这是有用的，任意数组的尺寸对在棋盘上任何点或哨兵有个入口.
	 */
	public short getFirstPointBeyondExtendedBoard() {
		return (short) ((width + 1) * (width + 2) + 1);
	}

	/** 返回落颜色color的棋子p的随机值. */
	long getHash(Color color, short p) {
		return zobristHashes[color.index()][p];
	}

	/**
	 * 返回每棋局最大落子数目. 很少有实际棋局下到这个数目, 但一次棋局（不检查大劫）可能落子到此.
	 * 通过这种方式来检查切断不寻常的落子要快于检查棋局中大劫.
	 */
	public short getMaxMovesPerGame() {
		return maxMovesPerGame;
	}

	/**
	 * 返回点p的8个方位的邻居. 如果点在边缘（角），一个（两个）邻居将是棋盘外点.棋盘外点的邻居未定义.
	 * <p>
	 *
	 * @see com.github.fangyun.ginkgo.core.CoordinateSystem
	 */
	public short[] getNeighbors(short p) {
		return neighbors[p];
	}

	/** 返回棋盘的尺寸(例如., 19). */
	public int getWidth() {
		return width;
	}

	/** 返回true如果p在棋盘上. */
	public boolean isOnBoard(short p) {
		return isValidOneDimensionalCoordinate(row(p)) && isValidOneDimensionalCoordinate(column(p));
	}

	/** 返回true如果c有效的行或列的下标. */
	public boolean isValidOneDimensionalCoordinate(int c) {
		return c >= 0 & c < width;
	}

	/** 返回p到q的曼哈顿距离. */
	public int manhattanDistance(short p, short q) {
		final int rowd = abs(row(p) - row(q));
		final int cold = abs(column(p) - column(q));
		return rowd + cold;
	}

	/**
	 * 用来序列化，被用在CopiableStructure, 不去创建冗余的CoordinateSystems对象.
	 *
	 * @see CopiableStructure
	 */
	private Object readResolve() {
		return forWidth(width);
	}

	/** 返回点p的行. */
	public int row(short p) {
		return p / south - 1;
	}

	/** 返回字符串形式的行r. */
	public String rowToString(int row) {
		return "" + (width - row);
	}

	/** 返回字符串形式的点p. */
	public String toString(short p) {
		if (p == PASS) {
			return "PASS";
		} else if (p == NO_POINT) {
			return "NO_POINT";
		} else if (p == RESIGN) {
			return "RESIGN";
		} else {
			return columnToString(column(p)) + rowToString(row(p));
		}
	}

}
