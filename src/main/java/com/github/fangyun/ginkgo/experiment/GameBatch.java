package com.github.fangyun.ginkgo.experiment;

import static com.github.fangyun.ginkgo.experiment.ExperimentConfiguration.EXPERIMENT;
import static com.github.fangyun.ginkgo.experiment.SystemConfiguration.SYSTEM;
import static java.io.File.separator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 在一台计算机上运行一系列的实验性的棋局。 */
public final class GameBatch implements Runnable {

	/**
	 * @param args
	 *            - 第0参数是主机名. 如果有第1参数，是结果目录。如果没有指定，则在系统结果目录上创建一个新目录。
	 */
	public static void main(String[] args) {
		assert args.length >= 1;
		String results;
		if (args.length >= 2) {
			results = args[1];
		} else {
			results = SYSTEM.resultsDirectory + timeStamp(true) + separator;
		}
		new File(results).mkdir();
		try {
			for (int i = 0; i < EXPERIMENT.gamesPerHost; i++) {
				new Thread(new GameBatch(i, args[0], results)).start();
			}
		} catch (final Throwable e) {
			e.printStackTrace(System.out);
			System.exit(1);
		}
	}

	/**
	 * 当前日期与时间的字符串表示.
	 *
	 * @param nest
	 *            如果true，使用文件分隔符代替横线分隔年、月、日期、时间.
	 */
	public static String timeStamp(boolean nest) {
		String punctuation;
		if (nest) {
			punctuation = File.separator;
		} else {
			punctuation = "-";
		}
		return new SimpleDateFormat("yyyy" + punctuation + "MM" + punctuation + "dd" + punctuation + "HH:mm:ss.SSS")
				.format(new Date());
	}

	/** 批次数（用作文件名的一部分）. */
	private final int batchNumber;

	/**
	 * 主机名的第一部分(用作文件名的一部分).
	 */
	private final String host;

	private final String resultsDirectory;

	public GameBatch(int batchNumber, String hostname, String resultsDirectory) {
		this.batchNumber = batchNumber;
		if (hostname.contains(".")) {
			host = hostname.substring(0, hostname.indexOf('.'));
		} else {
			host = hostname;
		}
		this.resultsDirectory = resultsDirectory;
	}

	@Override
	public void run() {
		System.out.println("Running batch " + batchNumber + " on " + host);
		for (final String conditionName : EXPERIMENT.conditions.keySet()) {
			final String condition = EXPERIMENT.conditions.get(conditionName);
			System.out.println("批次 " + batchNumber + " 在 " + host + " " + conditionName + ": " + condition);
			final String ginkgo = SYSTEM.java + " -cp " + SYSTEM.ginkgoClassPath + " -ea -Xmx" + SYSTEM.megabytes
					+ "M com.github.fangyun.ginkgo.ui.Ginkgo " + "boardsize=" + EXPERIMENT.rules.boardWidth + " komi="
					+ EXPERIMENT.rules.komi + " memory=" + SYSTEM.megabytes + " " + EXPERIMENT.always + " " + condition;
			System.out.println("Ginkgo is: " + ginkgo);
			runGames(ginkgo, EXPERIMENT.gnugo);
			runGames(EXPERIMENT.gnugo, ginkgo);
		}
		System.out.println("完成运行批次 " + batchNumber + " 在 " + host);
	}

	/** 用指定黑白棋手来运行一系列的棋局. */
	public void runGames(String black, String white) {
		final int[] wins = new int[3];
		for (int i = 0; i < EXPERIMENT.gamesPerColor; i++) {
			final String outFile = resultsDirectory + host + "-b" + batchNumber + "-" + timeStamp(false) + ".sgf";
			final Game game = new Game(outFile, EXPERIMENT.rules, black, white);
			wins[game.play().index()]++;
		}
	}

}
