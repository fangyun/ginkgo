package com.github.fangyun.ginkgo.util;

import static java.util.Arrays.fill;

import java.io.Serializable;

/**
 * 相当于一组布尔值，但要小得多。当插入、删除、清除和搜索是唯一的操作时，可以方便地表示一个集合;关键是小，空间是最重要的。
 * 如果有必要快速计算集合的大小或遍历元素，或者如果集合非常稀疏，则ShortSet可能更可取
 *
 * @see ShortSet
 */
public final class BitVector implements Serializable {
	private static final long serialVersionUID = -4550969625407960212L;
	/** 位块本身，64位块. */
	private final long[] data;

	/** 元素必须是[0，capacity). */
	public BitVector(int capacity) {
		int longs = capacity / 64;
		if (longs * 64 < capacity) {
			longs++;
		}
		data = new long[longs];
	}

	/** 从这个集合中删除所有元素. */
	public void clear() {
		fill(data, 0L);
	}

	/** 如果i在这个集合中，返回true. */
	public boolean get(int i) {
		return (data[i / 64] & 1L << i % 64) != 0;
	}

	/** 设置i是否在这个集合中. */
	public void set(int i, boolean value) {
		if (value) {
			data[i / 64] |= 1L << i % 64;
		} else {
			data[i / 64] &= ~(1L << i % 64);
		}
	}

}
