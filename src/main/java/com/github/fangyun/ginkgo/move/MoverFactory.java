package com.github.fangyun.ginkgo.move;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.feature.AtariObserver;
import com.github.fangyun.ginkgo.feature.CaptureSuggester;
import com.github.fangyun.ginkgo.feature.Conjunction;
import com.github.fangyun.ginkgo.feature.Disjunction;
import com.github.fangyun.ginkgo.feature.EscapeSuggester;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.feature.NearAnotherStone;
import com.github.fangyun.ginkgo.feature.NotEyeLike;
import com.github.fangyun.ginkgo.feature.OnThirdOrFourthLine;
import com.github.fangyun.ginkgo.feature.PatternSuggester;
import com.github.fangyun.ginkgo.feature.Predicate;

/** 静态方法用来创建一些特定的广泛用的落子. */
public final class MoverFactory {

	/** 就像可行的，但在可能的时候吃子. */
	public static Mover capturer(Board board, AtariObserver atariObserver) {
		return new SuggesterMover(board, new CaptureSuggester(board, atariObserver), feasible(board));
	}

	/**
	 * 首先使用一种逃避建议器，用吃子建议器作为后备.
	 */
	public static Mover escapeCapturer(Board board, AtariObserver atariObserver) {
		return new SuggesterMover(board, new EscapeSuggester(board, atariObserver), capturer(board, atariObserver));
	}

	/** 首先使用逃避建议器，再使用模式建议器，最后采用吃子建议器作为后备(像MoGo). */
	public static Mover escapePatternCapturer(Board board, AtariObserver atariObserver,
			HistoryObserver historyObserver) {
		return new SuggesterMover(board, new EscapeSuggester(board, atariObserver), new SuggesterMover(board,
				new PatternSuggester(board, historyObserver), capturer(board, atariObserver)));
	}

	/**
	 * 就像simpleRandom，但只在第三或第四行或附近的其他棋位上落子.
	 */
	public static Mover feasible(Board board) {
		final Predicate f = new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board)));
		return new PredicateMover(board, f);
	}

	/** 落子除了像眼的点. */
	public static Mover simpleRandom(Board board) {
		return new PredicateMover(board, new NotEyeLike(board));
	}
}
