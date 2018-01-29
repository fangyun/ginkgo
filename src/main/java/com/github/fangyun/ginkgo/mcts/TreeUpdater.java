package com.github.fangyun.ginkgo.mcts;

import com.github.fangyun.ginkgo.core.Color;

/** 根据一轮运行结果更新树. */
public interface TreeUpdater {

	/** 重置到最初状态. */
	public void clear();

	/** 返回在一个子节点被创建前所需的运行次数. */
	public int getGestation();

	/** 返回根节点. */
	public SearchNode getRoot();

	/**
	 * 接受一步棋后更新树 (e.g., 抛出不可到达的节点).
	 */
	public void updateForAcceptMove();

	/** 基于运行结果更新树. */
	public void updateTree(Color winner, McRunnable mcRunnable);

}
