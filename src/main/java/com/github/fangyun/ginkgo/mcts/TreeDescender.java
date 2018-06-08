package com.github.fangyun.ginkgo.mcts;


/**
 * 产生落子步骤在一次蒙特卡洛搜索树(或模拟结构).
 */
public interface TreeDescender {

	/**
	 * 返回最优落子从此时(相反在棋局中). 我们选择最大可能赢的落子.
	 */
	public short bestPlayMove();

	/** 清除到原始状态. */
	public void clear();

	/**
	 * 产生落子步骤在树的前端. 这些落子步骤被放在运行的棋盘上.
	 */
	public void descend(McRunnable runnable);

	/**
	 * 一个后代方法用于测试，采用整个棋局的运行的部分.
	 */
	public void fakeDescend(McRunnable runnable, short... moves);

	/** 返回一个节点必须有的运行轮次在运用偏置之前. */
	public int getBiasDelay();
	
	
	/**
	 * 返回这个落子的搜索值, 例如., 最赢率, UCT或RAVE.
	 */
	public float searchValue(SearchNode node, short move);

}
