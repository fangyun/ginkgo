package com.github.fangyun.ginkgo.score;

import java.io.Serializable;

import com.github.fangyun.ginkgo.core.Color;

/** 决定得分. */
public interface Scorer extends Serializable {

	/** 返回此得分使用的贴目. */
	public double getKomi();

	/**
	 * 返回当前棋盘状态下得分差值。正数对黑棋利好.
	 */
	public double score();

	/**
	 * 返回当前棋盘状态下的胜方棋色。可以为空如果是平局.
	 */
	public Color winner();

}
