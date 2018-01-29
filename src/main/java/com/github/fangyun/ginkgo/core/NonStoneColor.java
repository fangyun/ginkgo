package com.github.fangyun.ginkgo.core;

/** 不是棋子的某些东西的颜色. */
public enum NonStoneColor implements Color {

	/** 棋盘边缘（例如：哨兵点）的颜色. */
	OFF_BOARD('?', 3),

	/** 未下棋子点的颜色. */
	VACANT('.', 2);

	public static NonStoneColor forChar(char c) {
		if (c == OFF_BOARD.toChar()) {
			return OFF_BOARD;
		} else if (c == VACANT.toChar()) {
			return VACANT;
		}
		return null;
	}

	private char glyph;

	private int index;

	private NonStoneColor(char c, int index) {
		glyph = c;
		this.index = index;
	}

	@Override
	public int index() {
		return index;
	}

	@Override
	public char toChar() {
		return glyph;
	}
}
