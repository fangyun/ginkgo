package com.github.fangyun.ginkgo.feature;

/** True 如果提供给构造函数的两个谓词都是正确的. */
public final class Conjunction implements Predicate {
	private static final long serialVersionUID = -8504123259430263943L;

	private final Predicate a;

	private final Predicate b;

	public Conjunction(Predicate a, Predicate b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public boolean at(short p) {
		return a.at(p) && b.at(p);
	}
}
