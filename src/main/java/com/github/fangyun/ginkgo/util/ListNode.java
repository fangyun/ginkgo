package com.github.fangyun.ginkgo.util;

/**
 * 链接的列表节点. T是在节点中key的类型. 这是个简单容器包括key和next两个域.
 */
public final class ListNode<T> implements Poolable<ListNode<T>> {

	/** 存在节点中的键. */
	private T key;

	/** 下一个列表节点. */
	private ListNode<T> next;

	/** 返回存在节点中的键. */
	public T getKey() {
		return key;
	}

	/** 返回下一个列表节点. */
	@Override
	public ListNode<T> getNext() {
		return next;
	}

	/** 设置存储在列表节点中的键. */
	public void setKey(T key) {
		this.key = key;
	}

	/** 设置下一个列表节点. */
	@Override
	public void setNext(ListNode<T> next) {
		this.next = next;
	}

}
