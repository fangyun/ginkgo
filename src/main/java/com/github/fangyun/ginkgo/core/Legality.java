package com.github.fangyun.ginkgo.core;

/** 由Board.play返回，指出落子是否合法，如果不合法，指出为什么. */
public enum Legality {

	/**
	 * 虽然落子技术上合法，但会导致棋局太长.
	 */
	GAME_TOO_LONG,

	/** 违反简单劫或大劫. */
	KO_VIOLATION,

	/** 此点出现过. */
	OCCUPIED,

	/** 落子合法. */
	OK,

	/** 落子是自杀. */
	SUICIDE;

}
