package com.github.fangyun.ginkgo.book;

import static com.github.fangyun.ginkgo.core.SuperKoTable.IGNORE_SIGN_BIT;

import java.io.Serializable;

/**
 * 关联键(long, 棋盘配置的Zobrist哈希值)和值.
 * 底层数据结构是一个带线性探测的哈希表.
 * 
 * 模仿java.util.HashMap<Long, V>, 但不支持删除，明显更有效率.
 */
public final class BigHashMap<V> implements Serializable {
	private static final long serialVersionUID = 1872186022332727626L;

	private long[] keys;

	/** Number of keys currently in the map. */
	private int size;

	/** Values associated with keys. */
	private V[] values;

	@SuppressWarnings("unchecked")
	public BigHashMap() {
		keys = new long[1];
		values = (V[]) new Object[1];
	}

	/** True如果map包含key. */
	public boolean containsKey(long key) {
		int slot = ((int) key & IGNORE_SIGN_BIT) % keys.length;
		while (true) {
			if (keys[slot] == key && values[slot] != null) {
				return true;
			} else if (values[slot] == null) {
				return false;
			}
			slot = (slot + 1) % keys.length;
		}
	}

	/** 返回key所关联的value，或者null如果没有. */
	public V get(long key) {
		int slot = ((int) key & IGNORE_SIGN_BIT) % keys.length;
		while (true) {
			if (keys[slot] == key && values[slot] != null) {
				return values[slot];
			} else if (values[slot] == null) {
				return null;
			}
			slot = (slot + 1) % keys.length;
		}
	}

	/**
	 * 返回粗略的key数组，这可能包含许多false的条目，这些条目的值为null.
	 */
	public long[] getKeys() {
		return keys;
	}

	/**
	 * 关联key和value，如果map太满了，则伸展map.
	 */
	public void put(long key, V value) {
		if (!containsKey(key)) {
			size++;
			// The maximum load factor is 0.5
			if (size > keys.length / 2) {
				rehash();
			}
		}
		putAfterTableKnownLargeEnough(key, value);
	}

	/**
	 * 关联key和value. 不去检查这个map，假定map足够大.
	 */
	private void putAfterTableKnownLargeEnough(long key, V value) {
		int slot = ((int) key & IGNORE_SIGN_BIT) % keys.length;
		while (true) {
			if (keys[slot] == key || values[slot] == null) {
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
	@SuppressWarnings("unchecked")
	private void rehash() {
		final long[] oldKeys = keys;
		final V[] oldValues = values;
		keys = new long[keys.length * 2];
		values = (V[]) new Object[values.length * 2];
		for (int i = 0; i < oldKeys.length; i++) {
			if (oldValues[i] != null) {
				putAfterTableKnownLargeEnough(oldKeys[i], oldValues[i]);
			}
		}
	}

}
