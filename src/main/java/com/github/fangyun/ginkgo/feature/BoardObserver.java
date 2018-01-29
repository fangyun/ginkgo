package com.github.fangyun.ginkgo.feature;

import java.io.Serializable;

import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.util.ShortList;

/** 当棋盘发生改变时，该接口的对象被通知. */
public interface BoardObserver extends Serializable {

	/** 在棋盘落子一步后更新观察者. */
	public void update(StoneColor color, short location, ShortList capturedStones);

	/** 对空棋盘复位数据结构到合适状态. */
	public void clear();

	/**
	 * 从另一观察者中拷贝数据，这里假定和目标观察者同一类型. (我们不能用泛型，因为在同一列表中有多种观察者.)
	 */
	public void copyDataFrom(BoardObserver that);

}
