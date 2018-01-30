package com.github.fangyun.ginkgo.move;

import static com.github.fangyun.ginkgo.core.Legality.OK;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Legality;
import com.github.fangyun.ginkgo.feature.Suggester;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;
import com.github.fangyun.ginkgo.util.ShortList;

/** 这是一些建议器的建议落子. */
public final class SuggesterMover implements Mover {
	private static final long serialVersionUID = 1225709546835053009L;

	private final Board board;

	private final ShortList candidates;

	/** 如果建议器什么都不提，那就回到这个落子上. */
	private final Mover fallbackMover;

	private final Suggester suggester;

	/**
	 * @param fallbackMover
	 *           如果建议器什么都不提，那fallbackMover被要求落子.
	 */
	public SuggesterMover(Board board, Suggester suggester, Mover fallbackMover) {
		this.board = board;
		this.suggester = suggester;
		this.fallbackMover = fallbackMover;
		candidates = new ShortList(board.getCoordinateSystem().getArea());
	}

	@Override
	public short selectAndPlayOneMove(MersenneTwisterFast random, boolean fast) {
		candidates.clear();
		candidates.addAll(suggester.getMoves());
		while (candidates.size() > 0) {
			final short p = candidates.removeRandom(random);
			assert board.getColorAt(p) == VACANT;
			Legality legality = fast ? board.playFast(p) : board.play(p);
			if (legality == OK) {
				return p;
			}
		} 
		return fallbackMover.selectAndPlayOneMove(random, fast);
	}
}
