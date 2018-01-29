package com.github.fangyun.ginkgo.util;

/**
 * 能包含在一个池子中的东西.
 *
 * @see {@link Pool}
 */
public interface Poolable<T> {

	/** 返回下一个对象(例如，在池子中的自由列表). */
	public T getNext();

	/** 设置下一个对象(例如，在池子中的自由列表). */
	public void setNext(T next);

}
