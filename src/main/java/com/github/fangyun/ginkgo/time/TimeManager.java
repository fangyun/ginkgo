package com.github.fangyun.ginkgo.time;

/** 管理棋手时间. */
public interface TimeManager {

	/**
	 * 再次要求下子前的毫秒数. 返回0如果此轮没有思考时间.
	 */
	public int getMsec();

	/** 设置此棋局对此棋手剩余时间. */
	public void setRemainingSeconds(int seconds);

	/** 设置开始新一轮. */
	public void startNewTurn();

}
