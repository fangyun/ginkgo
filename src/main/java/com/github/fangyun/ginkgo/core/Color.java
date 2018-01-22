package com.github.fangyun.ginkgo.core;

/** 子类看有效颜色. */
public interface Color {

	/** 返回颜色在图示中对应的字符. */
	public char toChar();

	/**
	 * 返回用在数组下标的索引值，对每种颜色唯一的值.
	 */
	public int index();

}
