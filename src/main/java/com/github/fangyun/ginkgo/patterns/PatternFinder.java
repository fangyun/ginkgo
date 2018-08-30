package com.github.fangyun.ginkgo.patterns;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;
import com.github.fangyun.ginkgo.core.NonStoneColor;
import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;

public final class PatternFinder {

	/** 对敌方带2口气在POINT_HASHES的第一个下标. */
	public static final int ENEMY_2_LIBERTIES = 1;

	/** 对敌方带3或更多口气在POINT_HASHES的第一个下标. */
	public static final int ENEMY_3_OR_MORE_LIBERTIES = 2;

	/** 对打吃的敌方在POINT_HASHES的第一个下标. */
	public static final int ENEMY_IN_ATARI = 0;

	/** 对己方子带2口气在POINT_HASHES的第一个下标. */
	public static final int FRIENDLY_2_LIBERTIES = 1;

	/** 对己方子带3或更多口气在POINT_HASHES的第一个下标. */
	public static final int FRIENDLY_3_OR_MORE_LIBERTIES = 2;

	/** 对打吃的己方子在POINT_HASHES的第一个下标. */
	public static final int FRIENDLY_IN_ATARI = 0;

	/** 当最后落子是敌方，则增加第一个下标在POINT_HASHES. */
	public static final int LAST_MOVE_INCREASE = 3;

	/** 对棋盘外的点在POINT_HASHES的第一个下标. */
	public static final int OFF_BOARD = 9;

	/** 从一点粗略扩展开的偏移序列. */
	private static short[][] offsets;

	// TODO This (and the above) needs a better explanation. Maybe in package
	// documentation?
	/** 偏移的连续集合的大小. */
	private static int[] patternSizes;

	/** 第一个下标是点的条件，第二个下标是偏移量. */
	public static final long[][] POINT_HASHES = new long[10][39 * 39 - 1];

	static {
		// 查找所有可能的偏移
		ArrayList<short[]> unsortedOffsets = new ArrayList<>();
		for (short r = -19; r <= 19; r++) {
			for (short c = -19; c <= 19; c++) {
				if (r != 0 || c != 0) {
					unsortedOffsets.add(new short[] { r, c });
				}
			}
		}
		// 创建同心模式尺寸
		patternSizes = new int[180];
		offsets = new short[1520][];
		int sizeIndex = 1;
		int numberSorted = 0;
		int oldNumberSorted = numberSorted;
		for (double radius = 1.0; !unsortedOffsets.isEmpty(); radius += 0.01) {
			Iterator<short[]> iter = unsortedOffsets.iterator();
			while (iter.hasNext()) {
				short[] offset = iter.next();
				if (distanceTo(offset) <= radius) {
					offsets[numberSorted] = offset;
					iter.remove();
					numberSorted++;
				}
			}
			if (numberSorted > oldNumberSorted && (numberSorted - oldNumberSorted) % 4 == 0) {
				// 上面的第二个条件校验rounding错误，不创建非对称模式
				patternSizes[sizeIndex] = numberSorted;
				sizeIndex++;
				oldNumberSorted = numberSorted;
			}
		}
		// 创建Zobrist哈希
		MersenneTwisterFast random = new MersenneTwisterFast(0L);
		for (int i = 0; i < POINT_HASHES.length; i++) {
			for (int j = 0; j < POINT_HASHES[i].length; j++) {
				POINT_HASHES[i][j] = random.nextLong();
			}
		}
	}

	/**
	 * 返回从原始到偏移的欧氏距离.
	 */
	static double distanceTo(short[] offset) {
		double x = offset[0];
		double y = offset[1];
		return Math.sqrt(x * x + y * y);
	}

	static void generatePatternMap(Board board, HashMap<String, Float> map, HashMap<String, Long> hashMap,
			ShapeTable table, ArrayList<Short> stones, int minStoneCount, int maxStoneCount, int centerRow,
			int centerColumn, int patternRadius) {
		int topRow = centerRow - patternRadius;
		int bottomRow = centerRow + patternRadius;
		int leftColumn = centerColumn - patternRadius;
		int rightColumn = centerColumn + patternRadius;
		if (stones.size() >= minStoneCount) {
			board.clear();
			for (short p : stones) {
				board.play(p);
			}
			long hash = getHash(board, board.getCoordinateSystem().at(centerRow, centerColumn),
					(1 + (patternRadius * 2)) * (1 + (patternRadius * 2)) - 1, CoordinateSystem.NO_POINT);
			String pattern = getPatternString(board, topRow, bottomRow, leftColumn, rightColumn);
			map.put(pattern, table.getWinRate(hash));
			hashMap.put(pattern, hash);
		}
		if (stones.size() < maxStoneCount) {
			for (int row = topRow; row <= bottomRow; row++) {
				for (int column = leftColumn; column <= rightColumn; column++) {
					if (column == centerColumn && row == centerRow) {
						continue;
					}
					if (!board.getCoordinateSystem().isValidOneDimensionalCoordinate(row)
							|| !board.getCoordinateSystem().isValidOneDimensionalCoordinate(column)) {
						continue;
					}
					short p = board.getCoordinateSystem().at(row, column);
					if (!stones.contains(p)) {
						stones.add(p);
						generatePatternMap(board, map, hashMap, table, stones, minStoneCount, maxStoneCount, centerRow,
								centerColumn, patternRadius);
						stones.remove(new Short(p));
					}
				}
			}
		}
	}

	/**
	 * 得到围绕在一个点的模式的Zobrist哈希. 模式大小是这个模式-1(排除中心点).
	 */
	public static long getHash(Board board, short p, int minStones, short lastMove) {
		CoordinateSystem coords = board.getCoordinateSystem();
		long result = 0L;
		int row = coords.row(p);
		int column = coords.column(p);
		int stonesSeen = 0;
		// TODO Verify (in a test) that this finds distant patterns
		for (int i = 0; i < patternSizes.length - 1; i++) {
			for (int j = patternSizes[i]; j < patternSizes[i + 1]; j++) {
				int newRow = row + offsets[j][0];
				int newColumn = column + offsets[j][1];
				if (coords.isValidOneDimensionalCoordinate(newRow)
						&& coords.isValidOneDimensionalCoordinate(newColumn)) {
					short point = coords.at(newRow, newColumn);
					Color color = board.getColorAt(point);
					if (color == board.getColorToPlay()) {
						if (board.getLiberties(point).size() == 1) {
							result ^= POINT_HASHES[FRIENDLY_IN_ATARI][j];
						} else if (board.getLiberties(point).size() == 2) {
							result ^= POINT_HASHES[FRIENDLY_2_LIBERTIES][j];
						} else {
							result ^= POINT_HASHES[FRIENDLY_3_OR_MORE_LIBERTIES][j];
						}
						stonesSeen++;
					} else if (color == board.getColorToPlay().opposite()) {
						int lastMoveIncrease = lastMove == point ? LAST_MOVE_INCREASE : 0;
						if (board.getLiberties(point).size() == 1) {
							result ^= POINT_HASHES[ENEMY_IN_ATARI + lastMoveIncrease][j];
						} else if (board.getLiberties(point).size() == 2) {
							result ^= POINT_HASHES[ENEMY_2_LIBERTIES + lastMoveIncrease][j];
						} else {
							result ^= POINT_HASHES[ENEMY_3_OR_MORE_LIBERTIES + lastMoveIncrease][j];
						}
						stonesSeen++;
					}
				} else {
					result ^= POINT_HASHES[OFF_BOARD][j];
				}
			}
			if (stonesSeen >= minStones) {
				return result;
			}
		}
		return result;
	}

	/** 返回偏移数组。测试用. */
	static short[][] getOffsets() {
		return offsets;
	}

	/** 返回模式尺寸. 测试用. */
	static int[] getPatternSizes() {
		return patternSizes;
	}

	private static String getPatternString(Board board, int topRow, int bottomRow, int leftColumn, int rightColumn) {
		String pattern = "";
		for (int row = topRow; row <= bottomRow; row++) {
			for (int column = leftColumn; column <= rightColumn; column++) {
				if (!board.getCoordinateSystem().isValidOneDimensionalCoordinate(row)
						|| !board.getCoordinateSystem().isValidOneDimensionalCoordinate(column)) {
					pattern += NonStoneColor.OFF_BOARD.toChar();
				} else {
					Color pointColor = board.getColorAt(board.getCoordinateSystem().at(row, column));
					pattern += pointColor.toChar();
				}
			}
			pattern += "\n";
		}
		return pattern;
	}

	public static void main(String[] args) {
		HashMap<String, Float> map = new HashMap<>();
		HashMap<String, Long> hashMap = new HashMap<>();
		Board board = new Board(19);
		ShapeTable table = new ShapeTable(GINKGO_ROOT+File.separator+"patterns" + File.separator + "patterns9stones-SHAPE-sf999.data", 0.999f);
		int centerColumn = 11;
		int centerRow = 16;
		int patternRadius = 4;
		int minStoneCount = 4;
		int maxStoneCount = 4;
		ArrayList<Short> stones = new ArrayList<>();
		generatePatternMap(board, map, hashMap, table, stones, minStoneCount, maxStoneCount, centerRow, centerColumn,
				patternRadius);
		System.out.println(map.size());
		ArrayList<Entry<String, Float>> entries = new ArrayList<>(map.entrySet());
		Collections.sort(entries, new Comparator<Entry<String, Float>>() {
			@Override
			public int compare(Entry<String, Float> entry1, Entry<String, Float> entry2) {
				if (entry1.getValue() > entry2.getValue()) {
					return 1;
				} else if (entry1.getValue() < entry2.getValue()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		System.out.println("Bottom Twenty\n");
		for (int i = 0; i < 20; i++) {
			System.out.println(entries.get(i).getValue());
			System.out.println(hashMap.get(entries.get(i).getKey()));
			table.printIndividualWinRates(hashMap.get(entries.get(i).getKey()));
			System.out.println(entries.get(i).getKey());
		}
		System.out.println("Top Twenty\n");
		for (int i = entries.size() - 20; i < entries.size(); i++) {
			System.out.println(entries.get(i).getValue());
			System.out.println(hashMap.get(entries.get(i).getKey()));
			table.printIndividualWinRates(hashMap.get(entries.get(i).getKey()));
			System.out.println(entries.get(i).getKey());
		}
	}

}
