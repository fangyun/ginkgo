package com.github.fangyun.ginkgo.feature;

import java.io.Serializable;

import com.github.fangyun.ginkgo.mcts.SearchNode;

/** 给新搜索节点提供启发式偏置. */
public interface Rater extends Serializable {

	/** 用偏置更新节点的所有子节点. */
	public void updateNode(SearchNode node);

}
