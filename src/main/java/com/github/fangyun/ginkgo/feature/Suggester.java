package com.github.fangyun.ginkgo.feature;

import java.io.Serializable;

import com.github.fangyun.ginkgo.util.ShortSet;

/**
 * 建议落子有一定的属性.
 */
public interface Suggester extends Serializable {

	/**
	 * 返回建议的落子.
	 */
	public ShortSet getMoves();

	/** 返回建议器应当加到此落子建议上的偏置. */
	public int getBias();

}
