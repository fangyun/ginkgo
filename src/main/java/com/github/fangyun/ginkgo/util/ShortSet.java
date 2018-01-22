package com.github.fangyun.ginkgo.util;

import java.io.Serializable;

import com.github.fangyun.ginkgo.core.CoordinateSystem;

/**
 * 一个集合假定所有key在范围[0,n),
 * 从而实现常数级的插入、搜索、删除、清除和求大小。如果空间重要，或者如果集合相当稠密，则BitVector可能更合适。
 */
public final class ShortSet implements Serializable {
	private static final long serialVersionUID = -9052013795977331718L;

	/** data[i]是集合中第i个位置的元素. */
	private final short[] data;

	/** 当i被存储时，locations[i]指在数据data中的索引值. */
	private final short[] locations;

	/** 集合中元素个数. */
	private int size;

	/** 所有key必须是在[0, capacity). */
	public ShortSet(int capacity) {
		data = new short[capacity];
		locations = new short[capacity];
	}

	/**
	 * 增加key到集合中。该key可能已经存在或不存在于集合中.
	 */
	public void add(short key) {
		if (!contains(key)) {
			addKnownAbsent(key);
		}
	}

	/** 并集加入的集合到已存在的集合中. */
	public void addAll(ShortSet that) {
		for (short i = 0; i < that.size; i++) {
			add(that.get(i));
		}
	}

	/**
	 * 加入key,已知是不存在于集合中的。此操作快于add.
	 */
	public void addKnownAbsent(short key) {
		data[size] = key;
		locations[key] = (short) size;
		size++;
	}

	/** 从集合中删除所有元素. */
	public void clear() {
		size = 0;
	}

	/** 如果key在集合中则返回true. */
	public boolean contains(short key) {
		final int location = locations[key];
		return location < size & data[locations[key]] == key;
	}

	/**
	 * 复制指定集合到当前集合中，不会覆盖新建的对象.
	 */
	public void copyDataFrom(ShortSet that) {
		size = that.size;
		System.arraycopy(that.data, 0, data, 0, size);
		System.arraycopy(that.locations, 0, locations, 0, locations.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ShortSet that = (ShortSet) obj;
		if (that.data.length != data.length) {
			// If we have different universes, we're not equal.
			return false;
		}
		if (that.size != size) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!that.contains(data[i])) {
				return false;
			}
		}
		return true;
	}

	/** 返回集合中第i个元素. */
	public short get(int i) {
		return data[i];
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException("ShortSets不适合存储为哈希表");
	}

	/** 从集合中删除key，该key可能存在或不存在于集合中. */
	public void remove(short key) {
		if (contains(key)) {
			removeKnownPresent(key);
		}
	}

	/**
	 * 删除已知存在于集合中的key。此操作快于remove方法.
	 */
	public void removeKnownPresent(int key) {
		size--;
		final short location = locations[key];
		final short replacement = data[size];
		data[location] = replacement;
		locations[replacement] = location;
	}

	/** 返回在此集合中的元素个数. */
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		String result = "{";
		if (size > 0) {
			result += data[0];
			for (int i = 1; i < size; i++) {
				result += ", " + data[i];
			}
		}
		return result + "}";
	}

	/**
	 * 类似{@link#toString()}, 但是代替显示整数，而显示可读的点标签(例如.,"d3").
	 */
	public String toString(CoordinateSystem coords) {
		String result = size + ": {";
		if (size > 0) {
			result += coords.toString(data[0]);
			for (int i = 1; i < size; i++) {
				result += ", " + coords.toString(data[i]);
			}
		}
		return result + "}";
	}

}
