package com.github.fangyun.ginkgo.experiment;

import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;
import static com.github.fangyun.ginkgo.experiment.SystemConfiguration.SYSTEM;
import static java.io.File.separator;
import static java.lang.Double.parseDouble;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/** 保存实验依赖的设置，例如：每个主机的游戏数. */
enum ExperimentConfiguration {

	/** 单例名. */
	EXPERIMENT;

	/** 用于所有条件下的命令行参数. */
	final String always;

	/**
	 * 用于Ginkgo命令行参数的Maps条件名.
	 */
	final Map<String, String> conditions;

	/**
	 * 每种颜色与Ginkgo对弈的棋局数. 棋局总数=2 * <主机数目> * gamesPerHost * gamesPerColor.
	 */
	final int gamesPerColor;

	/** 每种条件下棋局总数. */
	final int gamesPerCondition;

	/**
	 * 在每个主机上同时运行的对弈数目. 此数目应当不大于每个主机上CPU核的个数. 如果Ginkgo采用多线程方式运行，此数目应当更小.
	 */
	final int gamesPerHost;

	/** 此试验运行GNUGo的完整命令. */
	final String gnugo;

	/** 保留棋盘尺寸，贴目和游戏时间. */
	final Rules rules;

	/** 从config/experiment.properties读配置. */
	private ExperimentConfiguration() {
		final Properties properties = new Properties();
		try {
			properties.load(
					new FileInputStream(GINKGO_ROOT + separator + "config" + separator + "experiment.properties"));
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		gamesPerHost = Integer.getInteger("gamesPerHost");
		gamesPerCondition = Integer.getInteger("gamesPerCondition");
		gamesPerColor = gamesPerCondition / (2 * SYSTEM.hosts.size() * gamesPerHost);
		if (2 * SYSTEM.hosts.size() * gamesPerHost * gamesPerColor != gamesPerCondition) {
			throw new IllegalArgumentException(
					"gamesPerCondition 必须等于 2 * <主机数> * <gamesPerHost>");
		}
		final int boardSize = Integer.getInteger("boardSize");
		final double komi = parseDouble(properties.getProperty("komi"));
		rules = new Rules(boardSize, komi, Integer.getInteger("time"));
		gnugo = SYSTEM.gnugoHome + " --boardsize " + boardSize
				+ " --mode gtp --quiet --chinese-rules --capture-all-dead --positional-superko --komi " + komi;
		System.out.println("Gnugo是： " + gnugo);
		always = properties.getProperty("always");
		conditions = new TreeMap<>();
		for (final String s : properties.stringPropertyNames()) {
			if (s.startsWith("condition")) {
				conditions.put(s, (String) properties.get(s));
			}
		}
		System.out.println(conditions.size() + " conditions: ");
		for (final String name : conditions.keySet()) {
			System.out.println(name + ": " + conditions.get(name));
		}
	}

}
