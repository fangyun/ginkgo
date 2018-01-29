package com.github.fangyun.ginkgo.util;

/**
 * 类型T的对象的池. 这允许手工内存管理，例如树节点.
 * <p>
 * 用对象注满池子，创建并置为可用.
 */
public final class Pool<T extends Poolable<T>> {

	/** 可用对象的链接列表. */
	private T free;

	/**
	 * 返回池中下一个可用对象，或null如果没有可用的. 同步方法避免两线程从池中拉出同一对象.
	 */
	public synchronized T allocate() {
		if (free == null) {
			return null;
		}
		final T result = free;
		free = free.getNext();
		return result;
	}

	/**
	 * 添加元素到池中. 即使元素以前不在池中，这也工作. 事实上，这是添加元素到池子中的首要方式.
	 *
	 * @return T为下一个有用的元素.
	 */
	public T free(T element) {
		final T result = element.getNext();
		element.setNext(free);
		free = element;
		return result;
	}

	/**
	 * 返回true如果没有元素在池子中.
	 */
	public boolean isEmpty() {
		return free == null;
	}

	/**
	 * 返回在池中的元素个数. 这方法耗时线性于池中元素个数.
	 */
	public int size() {
		int count = 0;
		T node = free;
		while (node != null) {
			count++;
			node = node.getNext();
		}
		return count;
	}
}
