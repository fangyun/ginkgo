package com.github.fangyun.ginkgo.book;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.SuperKoTable.IGNORE_SIGN_BIT;

import java.io.Serializable;

/**
 * 映射longs到shorts.
 *
 * 底层数据结构是一个带线性探测的哈希表.
 *
 * 模仿java.util.HashMap<Long, Short>, 但不支持删除，明显更有空间效率.

 */
public final class SmallHashMap implements Serializable {
	private static final long serialVersionUID = -592457889377229109L;

	/** 键. */
	private long[] keys;

	/** 当前在map中有多少键. */
	private int size;

	/** 值. */
	private short[] values;

	public SmallHashMap() {
		keys = new long[1];
		values = new short[1];
		values[0] = NO_POINT;
	}

	/** 返回true如果包含key. */
	public boolean containsKey(long key) {
		int slot = ((int) key & IGNORE_SIGN_BIT) % keys.length;
		while (true) {
			if (keys[slot] == key && values[slot] != NO_POINT) {
				return true;
			} else if (values[slot] == NO_POINT) {
				return false;
			}
			slot = (slot + 1) % keys.length;
		}
	}

	/** 包含key所关联的值. */
	public short get(long key) {
		int slot = ((int) key & IGNORE_SIGN_BIT) % keys.length;
		while (true) {
			if (keys[slot] == key && values[slot] != NO_POINT) {
				return values[slot];
			} else if (values[slot] == NO_POINT) {
				return NO_POINT;
			}
			slot = (slot + 1) % keys.length;
		}
	}

	/** 返回粗略的key数组. */
	long[] getKeys() {
		return keys;
	}

	/** 关联key和value，如果map太满了，则伸展map.*/
	public void put(long key, short value) {
		assert value != NO_POINT;
		size++;
		// The maximum load factor is 0.5
		if (size > keys.length / 2) {
			rehash();
		}
		putAfterTableKnownLargeEnough(key, value);
	}

	/**
	 * 关联key和value. 不去检查这个map，假定map足够大.
	 */
	private void putAfterTableKnownLargeEnough(long key, short value) {
		int slot = ((int) key & IGNORE_SIGN_BIT) % keys.length;
		while (true) {
			// If this map already has the value, it doesn't do anything.
			if (keys[slot] == key && values[slot] != NO_POINT) {
				return;
			} else if (values[slot] == NO_POINT) {
				keys[slot] = key;
				values[slot] = value;
				return;
			}
			slot = (slot + 1) % keys.length;
		}
	}

	/**
	 * 拷贝数据到两倍大小的表中，伸展map.
	 */
	private void rehash() {
		final long[] oldKeys = keys;
		final short[] oldValues = values;
		keys = new long[keys.length * 2];
		values = new short[values.length * 2];
		for (int i = 0; i < values.length; i++) {
			values[i] = NO_POINT;
		}
		for (int i = 0; i < oldKeys.length; i++) {
			if (oldValues[i] != NO_POINT) {
				putAfterTableKnownLargeEnough(oldKeys[i], oldValues[i]);
			}
		}
	}
}
