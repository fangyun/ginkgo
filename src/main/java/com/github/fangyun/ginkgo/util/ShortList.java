package com.github.fangyun.ginkgo.util;

import java.io.Serializable;

import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;

/**
 * 类似于java.util.ArrayList&lt;Short&gt;, 但是避免了使用封装器的各种限制问题.
 * 此类没有ArrayList安全，例如你能设置一个key超过数组的尺寸(尽管有个断言检查了这个). 这只是为了速度.
 * <p>
 * 方法addIfNotPresent()ShortList表现像个Set.
 * 如果keys来自于小的、有限的集合，那么ShortSet或者BitVector可能更有效率.
 */
public final class ShortList implements Serializable {
	private static final long serialVersionUID = -1827970670763813425L;

	/** 列表元素. */
	private final short[] data;

	/** 在列表中元素的个数，也是下一个有效空间的下标. */
	private int size;

	public ShortList(int capacity) {
		data = new short[capacity];
	}

	/**
	 * 在列表尾部增加一个key.
	 */
	public void add(short key) {
		data[size] = key;
		size++;
	}

	/** 如果key不存在，则在列表尾部增加该key. */
	public void addIfNotPresent(short key) {
		if (!contains(key)) {
			add(key);
		}
	}

	/** 返回列表能保持的元素的容量数. */
	public int capacity() {
		return data.length;
	}

	/** 从列表中删除所有元素. */
	public void clear() {
		size = 0;
	}

	/** 返回true如果列表包含key. */
	public boolean contains(short key) {
		for (int i = 0; i < size; i++) {
			if (data[i] == key) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 复制另一个列表到此列表中，不去创建新的对象.
	 */
	public void copyDataFrom(ShortList that) {
		size = that.size;
		System.arraycopy(that.data, 0, data, 0, size);
	}

	/** 返回列表的第i个元素. */
	public short get(int i) {
		assert i < size;
		return data[i];
	}

	/**
	 * 删除并返回列表中最后的元素.
	 *
	 * @throws ArrayIndexOutOfBoundsException
	 *             如果列表为空.
	 */
	public short removeLast() {
		size--;
		return data[size];
	}

	/**
	 * 删除并返回列表中随机元素. 列表的顺序不被维护.
	 */
	public short removeRandom(MersenneTwisterFast random) {
		int randomIndex = random.nextInt(size);
		short temp = data[randomIndex];
		size--;
		data[randomIndex] = data[size];
		return temp;
	}

	/** 设置列表中第i个元素. */
	public void set(int i, short key) {
		assert i < size;
		data[i] = key;
	}

	/** 返回列表大小. */
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		String result = "";
		if (size > 0) {
			result += data[0];
			for (int i = 1; i < size; i++) {
				result += ", " + data[i];
			}
		}
		return "(" + result.trim() + ")";
	}

	/**
	 * 返回列表中元素的可读棋盘坐标形式.
	 */
	public String toString(CoordinateSystem coords) {
		String result = "";
		if (size > 0) {
			result += coords.toString(data[0]);
			for (int i = 1; i < size; i++) {
				result += ", " + coords.toString(data[i]);
			}
		}
		return "(" + result + ")";
	}

	/** 增加在set中的元素到列表中. */
	public void addAll(ShortSet set) {
		for (int i = 0; i < set.size(); i++) {
			data[i] = set.get(i);
		}
		size = set.size();
	}
}
