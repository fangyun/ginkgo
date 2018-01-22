package com.github.fangyun.ginkgo.core;

/** 棋子的颜色. */
public enum StoneColor implements Color {

	BLACK('#', 0),

	WHITE('O', 1);

	// 设置对手棋的棋子颜色.
	static {
		BLACK.opposite = WHITE;
		WHITE.opposite = BLACK;
	}

	/**
	 * 返回图示字符对应的StoneColor. 返回null如果没有对应的StoneColor.
	 */
	public static StoneColor forChar(char c) {
		if (c == BLACK.toChar()) {
			return BLACK;
		} else if (c == WHITE.toChar()) {
			return WHITE;
		}
		return null;
	}

	private final char glyph;

	private final int index;

	private StoneColor opposite;

	private StoneColor(char c, int index) {
		glyph = c;
		this.index = index;
	}

	@Override
	public int index() {
		return index;
	}

	/** 返回对手棋子颜色. */
	public StoneColor opposite() {
		return opposite;
	}

	@Override
	public char toChar() {
		return glyph;
	}

}
