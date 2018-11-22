package com.github.fangyun.ginkgo.move;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.Legality.OK;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Legality;
import com.github.fangyun.ginkgo.feature.Predicate;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;
import com.github.fangyun.ginkgo.util.ShortList;

/**
 * 做出一些满足某些谓词的随机落子.
 */
public final class PredicateMover implements Mover {
	private static final long serialVersionUID = -395681488790478014L;

	private final Board board;

	private final ShortList candidates;

	private final Predicate filter;

	/**
	 * @param filter
	 *            只考虑满足的过滤器.
	 */
	public PredicateMover(Board board, Predicate filter) {
		this.board = board;
		this.filter = filter;
		candidates = new ShortList(board.getCoordinateSystem().getArea());
	}
	
	@Override
	public short selectAndPlayOneMove(MersenneTwisterFast random, boolean fast) {
		candidates.clear();
		candidates.addAll(board.getVacantPoints());
		while (candidates.size() > 0) {
			final short p = candidates.removeRandom(random);
			if (board.getColorAt(p) == VACANT && filter.at(p)) {
				Legality legality = fast ? board.playFast(p) : board.play(p);
				if (legality == OK) {
					return p;
				}
			}	
		} 
		board.pass();
		return PASS;
	}

}
