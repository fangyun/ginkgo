package com.github.fangyun.ginkgo.feature;

/** True 如果提供给构造函数的至少一个谓词是正确的. */
@SuppressWarnings("serial")
public final class Disjunction implements Predicate {

	private final Predicate a;

	private final Predicate b;

	public Disjunction(Predicate a, Predicate b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public boolean at(short p) {
		return a.at(p) || b.at(p);
	}
}
