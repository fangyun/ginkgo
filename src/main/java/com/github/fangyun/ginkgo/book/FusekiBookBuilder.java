package com.github.fangyun.ginkgo.book;

import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;
import static java.util.Arrays.sort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.sgf.SgfParser;

import com.github.fangyun.ginkgo.book.SmallHashMap;

/**
 * 构建布局棋谱从(可能嵌套)SGF文件目录. 首先处理数据产生初略棋谱, 然后处理创建为FusekiBook的最终棋谱.
 */
public final class FusekiBookBuilder {

	/**
	 * 在和低于这个响应数, 存储响应为列表. 高于此，存储为响应的频率(着子为索引).
	 */
	public static final int MEDIUM_ARRAY_LIMIT = 50;

	public static void main(String[] args) {
		final FusekiBookBuilder builder = new FusekiBookBuilder(20, 50, "books", true);
		// Directory below contains SGF
		builder.processFiles(new File("/Network/Servers/maccsserver.lclark.edu/Users/mdreyer/Desktop/KGS Files/"));
		builder.writeRawBook();
		// To only build final book from raw book, comment two lines above
		builder.buildFinalBook();
	}

	/**
	 * 映射棋盘哈希值到short数组. 或者中型数组(响应棋盘的着子列表)或者长数组(响应棋盘的多少次每个着子的计数).
	 */
	private BigHashMap<short[]> bigMap;

	/** 为每个旋转与反射. */
	private final Board[] boards;

	private final CoordinateSystem coords;

	/**
	 * 当着子至少这多次时，此着子才被存储在最终的map中.
	 */
	private final int countThreshold;

	/** 写出到输出文件中的对象. */
	private final SmallHashMap finalMap;

	/** 棋局中在和超过此深度的着子被忽略. */
	private final int maxMoves;

	/** 目录存储粗略和最终的棋谱. */
	private final String objectFilePath;

	/**
	 * 映射棋盘哈希值到响应. 一旦有第二次响应，则采用bigMap.
	 */
	private final SmallHashMap smallMap;

	/** 如果true，通过打印消息到标准输出指出进度. */
	private final boolean verbose;

	public FusekiBookBuilder(int maxMoves, int countThreshold, String directoryName, boolean verbose) {
		// 如果countThreshold为1，我们就必须在buildFinalBook中查找smallMap。
		assert countThreshold > 1;
		smallMap = new SmallHashMap();
		bigMap = new BigHashMap<>();
		finalMap = new SmallHashMap();
		this.maxMoves = maxMoves;
		this.countThreshold = countThreshold;
		boards = new Board[8];
		coords = CoordinateSystem.forWidth(19);
		for (int i = 0; i < boards.length; i++) {
			boards[i] = new Board(coords.getWidth());
		}
		objectFilePath = GINKGO_ROOT + directoryName;
		new File(objectFilePath).mkdir();
		this.verbose = verbose;
	}

	/** 从粗略棋谱构建最终棋谱. */
	@SuppressWarnings("unchecked")
	void buildFinalBook() {
		try {
			try (ObjectInputStream in = new ObjectInputStream(
					new FileInputStream(objectFilePath + File.separator + "rawfuseki19.data"))) {
				bigMap = (BigHashMap<short[]>) in.readObject();
			}
			try (ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(objectFilePath + File.separator + "fuseki19.data"))) {
				findHighestCounts();
				out.writeObject(maxMoves);
				out.writeObject(finalMap);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * 查找大约等于countThreshold的最常见着子，否则，返回NO_POINT.
	 */
	private short findHighest(short[] counts) {
		short winner = CoordinateSystem.NO_POINT;
		if (counts.length <= MEDIUM_ARRAY_LIMIT) {
			sort(counts);
			final short[] frequency = new short[coords.getFirstPointBeyondBoard()];
			short mostFrequent = 0;
			for (short p = 0; p < counts.length; p++) {
				frequency[counts[p]]++;
				if (frequency[counts[p]] >= mostFrequent) {
					mostFrequent = frequency[counts[p]];
					if (mostFrequent >= countThreshold) {
						winner = counts[p];
					}
				}
			}
		} else {
			for (final short p : coords.getAllPointsOnBoard()) {
				if (counts[p] >= countThreshold && counts[p] >= counts[winner]) {
					winner = p;
				}
			}
		}
		return winner;
	}

	/**
	 * 对每一个棋盘配置查找最流行的下一步，存储在finalMap中.
	 */
	private void findHighestCounts() {
		for (final long boardHash : bigMap.getKeys()) {
			final short[] moves = bigMap.get(boardHash);
			// This null check is necessary -- see BigHashMap.getKeys()
			if (moves != null) {
				final short move = findHighest(moves);
				if (move != CoordinateSystem.NO_POINT) {
					finalMap.put(boardHash, move);
				}
			}
		}
	}

	/**
	 * 分析文件，修改smallMap and bigMap. 如果文件是目录，递归分析.
	 */
	void processFiles(File file) {
		if (file.isDirectory()) {
			if (verbose) {
				System.out.println("Analyzing files in " + file.getName());
			}
			for (final File tempFile : file.listFiles()) {
				processFiles(tempFile);
			}
		} else if (file.getPath().endsWith(".sgf")) {
			final SgfParser parser = new SgfParser(coords, true);
			final List<List<Short>> games = parser.parseGamesFromFile(file, maxMoves);
			processGames(games);
		}
	}

	/** 处理棋局中的着子，更新bigMap和smallMap. */
	private void processGame(List<Short> game) {
		final short[] transformations = new short[8];
		for (final short move : game) {
			transformations[0] = move;
			transformations[1] = rotate90(move);
			transformations[2] = rotate90(transformations[1]);
			transformations[3] = rotate90(transformations[2]);
			transformations[4] = reflect(move);
			transformations[5] = rotate90(transformations[4]);
			transformations[6] = rotate90(transformations[5]);
			transformations[7] = rotate90(transformations[6]);
			for (int i = 0; i < transformations.length; i++) {
				processMove(transformations[i], boards[i].getFancyHash());
				boards[i].play(transformations[i]);
			}
		}
	}

	/** 对所有指定的棋局更新smallMap和bigMap. */
	private void processGames(List<List<Short>> games) {
		for (final List<Short> game : games) {
			for (final Board board : boards) {
				board.clear();
			}
			processGame(game);
		}
	}

	/**
	 * 分析着子作为fancyHash的响应, 更新bigMap和smallMap.
	 */
	private void processMove(short move, long fancyHash) {
		if (bigMap.containsKey(fancyHash)) {
			// bigMap中的条目要么是一个着子列表(中型)，要么是一个计数的着子索引数组。
			final short[] array = bigMap.get(fancyHash);
			if (array.length < MEDIUM_ARRAY_LIMIT) {
				// 它是中等的，但是有空间让它变大
				final short[] temp = new short[array.length + 1];
				for (int i = 0; i < array.length; i++) {
					temp[i] = array[i];
				}
				temp[temp.length - 1] = move;
				bigMap.put(fancyHash, temp);
			} else if (array.length == MEDIUM_ARRAY_LIMIT) {
				// 它达到了中等大小的极限;把它转换成大
				final short[] temp = new short[coords.getFirstPointBeyondBoard()];
				for (int i = 0; i < array.length; i++) {
					temp[array[i]]++;
				}
				temp[move]++;
				bigMap.put(fancyHash, temp);
			} else {
				// 它已经大;只是增加一个计数
				array[move]++;
				if (array[move] < 0) {
					array[move] = Short.MAX_VALUE;
				}
			}
		} else if (smallMap.containsKey(fancyHash)) {
			// 我们以前见过这个哈希;变小到中。
			final short[] temp = new short[2];
			temp[0] = smallMap.get(fancyHash);
			temp[1] = move;
			bigMap.put(fancyHash, temp);
		} else {
			// 我们第一次看到这个散列;添加到smallmap
			smallMap.put(fancyHash, move);
		}
	}

	/** 返回着子在线c=r的镜像点. */
	public short reflect(short move) {
		final int row = coords.row(move);
		final int col = coords.column(move);
		final int r2 = coords.getWidth() - 1 - col;
		final int c2 = coords.getWidth() - 1 - row;
		final short p = coords.at(r2, c2);
		return p;
	}

	/** 返回着子反时针旋转90度的点. */
	public short rotate90(short move) {
		final int row = coords.row(move);
		final int col = coords.column(move);
		final int r2 = coords.getWidth() - 1 - col;
		final int c2 = row;
		final short p = coords.at(r2, c2);
		return p;
	}

	/** 写粗略棋谱到文件中. */
	public void writeRawBook() {
		final File directory = new File(objectFilePath + File.separator + "rawfuseki19.data");
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(directory))) {
			out.writeObject(bigMap);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
