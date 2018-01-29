package com.github.fangyun.ginkgo.feature;

import java.io.Serializable;

/** 决定是否在棋盘上的独立点满足一定的谓词. */
public interface Predicate extends Serializable {

	/** 返回true如果点p满足此谓词. */
	public boolean at(short p);

}
