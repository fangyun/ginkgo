package com.github.fangyun.ginkgo.experiment;

/** 保存不依赖棋手的规则, 例如：棋盘尺寸. */
final class Rules {

	final int boardWidth;

	final double komi;

	/** 单位秒. 如果没有时间限制，则为负数. */
	final int time;

	Rules(int boardSize, double komi, int time) {
		this.boardWidth = boardSize;
		this.komi = komi;
		this.time = time;
	}
}
