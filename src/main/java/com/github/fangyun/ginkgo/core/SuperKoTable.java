package com.github.fangyun.ginkgo.core;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 佐布里斯特哈希的集合，存储以前棋盘位置. 这是一个哈希表，但是没有java.util.HashSet的限制.
 * 只支持插入、搜索和拷贝.通过线性探测解决冲突.特殊值EMPTY总是被认为在表中.
 */
public final class SuperKoTable implements Serializable {
	private static final long serialVersionUID = -296222608624304037L;

	/**
	 * 特殊值表示在表中的空槽. 这个值也代表空棋盘，因此总是认为在表中.
	 */
	public static final long EMPTY = 0L;

	/**
	 * 位掩码保证哈希码为正数. Math.abs() 不能正常工作，因为 abs(Integer.minValue()) < 0.
	 */
	public static final int IGNORE_SIGN_BIT = 0x7fffffff;

	/** 哈希表数据. */
	private final long[] data;

	public SuperKoTable(CoordinateSystem coords) {
		data = new long[coords.getMaxMovesPerGame() * 2];
	}

	/** 增加一个key到表中. */
	public void add(long key) {
		if (key != EMPTY) {
			int slot = ((int) key & IGNORE_SIGN_BIT) % data.length;
			while (data[slot] != EMPTY) {
				if (data[slot] == key) {
					return;
				}
				slot = (slot + 1) % data.length;
			}
			data[slot] = key;
		}
	}

	/** 返回哈希表的槽数. */
	int capacity() {
		return data.length;
	}

	/** 从哈希表中删除所有元素. */
	public void clear() {
		Arrays.fill(data, EMPTY);
	}

	/** 返回true如果key在哈希表中. */
	public boolean contains(long key) {
		if (key == EMPTY) {
			return true;
		}
		int slot = ((int) key & IGNORE_SIGN_BIT) % data.length;
		while (data[slot] != EMPTY) {
			if (data[slot] == key) {
				return true;
			}
			slot = (slot + 1) % data.length;
		}
		return false;
	}

	/**
	 * 拷贝另一哈希表，没有创建对象的问题.
	 */
	public void copyDataFrom(SuperKoTable that) {
		System.arraycopy(that.data, 0, data, 0, data.length);
	}

}
