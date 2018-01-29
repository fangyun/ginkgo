package com.github.fangyun.ginkgo.book;

import com.github.fangyun.ginkgo.core.Board;

public interface OpeningBook {

	/** 返回给定棋盘状态的存储的响应. */
	public short nextMove(Board board);

}
