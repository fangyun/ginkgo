package com.github.fangyun.ginkgo.feature;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 建议吃敌方棋子的着子. */
public final class CaptureSuggester implements Suggester {
	private static final long serialVersionUID = -7612549415894969828L;

	private final AtariObserver atari;

	private final int bias;

	private final Board board;

	/**
	 * 当前棋手的所有着子的列表将会吃到棋子
	 */
	private final ShortSet movesToCapture;

	public CaptureSuggester(Board board, AtariObserver atari) {
		this(board, atari, 0);
	}

	public CaptureSuggester(Board board, AtariObserver atari, int bias) {
		this.bias = bias;
		this.board = board;
		this.atari = atari;
		movesToCapture = new ShortSet(board.getCoordinateSystem()
				.getFirstPointBeyondBoard());
	}

	@Override
	public int getBias() {
		return bias;
	}

	@Override
	public ShortSet getMoves() {
		movesToCapture.clear();
		final ShortSet chainsInAtari = atari.getChainsInAtari(board.getColorToPlay()
				.opposite());
		for (int i = 0; i < chainsInAtari.size(); i++) {
			movesToCapture.add(board.getLiberties(chainsInAtari.get(i)).get(0));
		}
		return movesToCapture;
	}
}
