package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.feature.AtariObserver;
import com.github.fangyun.ginkgo.feature.CaptureSuggester;
import com.github.fangyun.ginkgo.feature.Conjunction;
import com.github.fangyun.ginkgo.feature.Disjunction;
import com.github.fangyun.ginkgo.feature.EscapeSuggester;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.feature.LgrfSuggester;
import com.github.fangyun.ginkgo.feature.LgrfTable;
import com.github.fangyun.ginkgo.feature.NearAnotherStone;
import com.github.fangyun.ginkgo.feature.NotEyeLike;
import com.github.fangyun.ginkgo.feature.OnThirdOrFourthLine;
import com.github.fangyun.ginkgo.feature.PatternSuggester;
import com.github.fangyun.ginkgo.feature.Rater;
import com.github.fangyun.ginkgo.feature.ShapeRater;
import com.github.fangyun.ginkgo.feature.Predicate;
import com.github.fangyun.ginkgo.feature.StoneCountObserver;
import com.github.fangyun.ginkgo.feature.Suggester;
import com.github.fangyun.ginkgo.move.MoverFactory;
import com.github.fangyun.ginkgo.move.PredicateMover;
import com.github.fangyun.ginkgo.move.SuggesterMover;
import com.github.fangyun.ginkgo.patterns.ShapeTable;
import com.github.fangyun.ginkgo.score.ChineseFinalScorer;
import com.github.fangyun.ginkgo.score.ChinesePlayoutScorer;

/** 创建一些特定的、广泛使用的可使用的CopiableStructure的静态方法. */
public final class CopiableStructureFactory {

	/** 返回一个带有棋盘、记分器和落子计数器的结构. */
	public static CopiableStructure basicParts(int width, double komi) {
		final Board board = new Board(width);
		final ChinesePlayoutScorer scorer = new ChinesePlayoutScorer(board, komi);
		return new CopiableStructure().add(board).add(scorer).add(new StoneCountObserver(board, scorer))
				.add(new HistoryObserver(board)).add(new ChineseFinalScorer(board, komi));
	}

	/** 就像可行的，但是返回的结构在可能的时候吃子. */
	public static CopiableStructure capturer(int width) {
		final CopiableStructure base = basicParts(width, 7.5);
		final Board board = base.get(Board.class);
		final AtariObserver atariObserver = new AtariObserver(board);
		return base.add(MoverFactory.capturer(board, atariObserver));
	}

	/**
	 * 返回结构首先使用逃避建议器，用吃子建议器作为后备.
	 */
	public static CopiableStructure escapeCapturer(int width) {
		final CopiableStructure base = basicParts(width, 7.5);
		final Board board = base.get(Board.class);
		final AtariObserver atariObserver = new AtariObserver(board);
		base.add(new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board))));
		return base.add(MoverFactory.escapeCapturer(board, atariObserver));
	}

	/**
	 * 返回结构首先使用逃避建议器，再使用模式建议器，最后采用吃子建议器作为后备(像MoGo).
	 */
	public static CopiableStructure escapePatternCapture(int width) {
		final CopiableStructure base = basicParts(width, 7.5);
		final Board board = base.get(Board.class);
		final AtariObserver atariObserver = new AtariObserver(board);
		final HistoryObserver historyObserver = base.get(HistoryObserver.class);
		base.add(new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board))));
		return base.add(MoverFactory.escapePatternCapturer(board, atariObserver, historyObserver));
	}

	/**
	 * 返回结构就像simpleRandom，但只在第三或第四行或附近的其他棋位上落子.
	 */
	public static CopiableStructure feasible(int width) {
		final CopiableStructure base = basicParts(width, 7.5);
		final Board board = base.get(Board.class);
		base.add(new Suggester[0]);
		base.add(new int[0]);
		base.add(new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board))));
		return base.add(MoverFactory.feasible(board));
	}

	/** 类似于useWithBias，但包含LGRF2. */
	public static CopiableStructure lgrfWithBias(int width, double komi) {
		final CopiableStructure base = basicParts(width, komi);
		final Board board = base.get(Board.class);
		// Observers
		final AtariObserver atariObserver = new AtariObserver(board);
		final HistoryObserver historyObserver = base.get(HistoryObserver.class);
		// Filter
		Predicate filter = new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board)));
		base.add(filter);
		// LGRF
		final LgrfTable table = new LgrfTable(board.getCoordinateSystem());
		base.add(table);
		// This is added to the structure to that every LgrfSuggester can point
		// to
		final LgrfSuggester lgrf = new LgrfSuggester(board, historyObserver, table, filter);
		// This is added to the structure to that every LgrfSuggester can point
		// to
		// the same table. This is handled in the McRunnable constructor.
		base.add(lgrf);
		// Suggesters
		final EscapeSuggester escape = new EscapeSuggester(board, atariObserver, 20);
		final PatternSuggester patterns = new PatternSuggester(board, historyObserver, 20);
		final CaptureSuggester capture = new CaptureSuggester(board, atariObserver, 20);
		// Bias
		base.add(new Suggester[] { escape, patterns, capture });
		// Mover
		final SuggesterMover mover = new SuggesterMover(board, lgrf,
				new SuggesterMover(board, escape, new SuggesterMover(board, patterns,
						new SuggesterMover(board, capture, new PredicateMover(board, filter)))));
		return base.add(mover);
	}

	/** 返回结构随机落子除了像眼的点. */
	public static CopiableStructure simpleRandom(int width) {
		final CopiableStructure base = basicParts(width, 7.5);
		final Board board = base.get(Board.class);
		base.add(new NotEyeLike(board));
		return base.add(MoverFactory.simpleRandom(board));
	}

	/**
	 * 类似{@link#escapePatternCapture}, 但也更新偏置.
	 */
	public static CopiableStructure useWithBias(int width, double komi) {
		final CopiableStructure base = basicParts(width, komi);
		final Board board = base.get(Board.class);
		// Observers
		final AtariObserver atariObserver = new AtariObserver(board);
		final HistoryObserver historyObserver = base.get(HistoryObserver.class);
		// Suggesters
		final EscapeSuggester escape = new EscapeSuggester(board, atariObserver, 20);
		final PatternSuggester patterns = new PatternSuggester(board, historyObserver, 20);
		final CaptureSuggester capture = new CaptureSuggester(board, atariObserver, 20);
		// Bias
		base.add(new Suggester[] { escape, patterns, capture });
		// Mover
		final SuggesterMover mover = new SuggesterMover(board, escape,
				new SuggesterMover(board, patterns,
						new SuggesterMover(board, capture,
								new PredicateMover(board, new Conjunction(new NotEyeLike(board),
										new Disjunction(
												OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()),
												new NearAnotherStone(board)))))));
		// Filter
		base.add(new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board))));
		return base.add(mover);
	}

	public static CopiableStructure shape(int width, double komi, int shapeBias, int minStones,
			float shapeScalingFactor) {
		final CopiableStructure base = basicParts(width, komi);
		final Board board = base.get(Board.class);
		// Observers
		final AtariObserver atariObserver = new AtariObserver(board);
		final HistoryObserver historyObserver = base.get(HistoryObserver.class);
		// Filter
		Predicate filter = new Conjunction(new NotEyeLike(board), new Disjunction(
				OnThirdOrFourthLine.forWidth(board.getCoordinateSystem().getWidth()), new NearAnotherStone(board)));
		base.add(filter);
		// LGRF
		final LgrfTable table = new LgrfTable(board.getCoordinateSystem());
		base.add(table);
		final LgrfSuggester lgrf = new LgrfSuggester(board, historyObserver, table, filter);
		// This is added to the structure to that every LgrfSuggester can point
		// to
		// the same table. This is handled in the McRunnable constructor.
		base.add(lgrf);
		String sfString = Float.toString(shapeScalingFactor);
		sfString = sfString.substring(sfString.indexOf('.') + 1);
		// TODO The shape scaling factor (last parameter below) should not be
		// hard-coded
		final ShapeTable shapeTable = new ShapeTable(
				GINKGO_ROOT + "patterns/patterns" + minStones + "stones-SHAPE-sf" + sfString + ".data", 0.99f);
		// Suggesters
		final EscapeSuggester escape = new EscapeSuggester(board, atariObserver, 20);
		final PatternSuggester patterns = new PatternSuggester(board, historyObserver, 20);
		final CaptureSuggester capture = new CaptureSuggester(board, atariObserver, 20);
		// Shape
		final ShapeRater shape = new ShapeRater(board, historyObserver, shapeTable, shapeBias, minStones);
		base.add(shapeTable);
		base.add(shape);
		// Bias;
		base.add(new Suggester[] { escape, patterns, capture });
		// First argument is null because the ShapeTable needs to be
		// added to the ShapeRater on the outside, and this avoids resizing
		// the array; when using this copiable structure, add the ShapeRater
		// to the 0th slot of this array
		base.add(new Rater[] { null });
		// Mover
		final SuggesterMover mover = new SuggesterMover(board, lgrf,
				new SuggesterMover(board, escape, new SuggesterMover(board, patterns,
						new SuggesterMover(board, capture, new PredicateMover(board, filter)))));
		return base.add(mover);
	}

}
