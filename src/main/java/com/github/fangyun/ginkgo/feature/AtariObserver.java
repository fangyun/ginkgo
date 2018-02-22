package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.FIRST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.LAST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.util.ShortList;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 为每一种棋色追踪目前在打吃的所有链. */
public final class AtariObserver implements BoardObserver {
	private static final long serialVersionUID = 7102010754419074015L;

	private final Board board;

	private final ShortSet[] chainsInAtari;

	private final CoordinateSystem coords;

	public AtariObserver(Board board) {
		this.board = board;
		coords = board.getCoordinateSystem();
		board.addObserver(this);
		chainsInAtari = new ShortSet[] { new ShortSet(coords.getFirstPointBeyondBoard()),
				new ShortSet(coords.getFirstPointBeyondBoard()) };
	}

	@Override
	public void clear() {
		chainsInAtari[BLACK.index()].clear();
		chainsInAtari[WHITE.index()].clear();
	}

	@Override
	public void copyDataFrom(BoardObserver that) {
		final AtariObserver original = (AtariObserver) that;
		chainsInAtari[BLACK.index()].copyDataFrom(original.chainsInAtari[BLACK.index()]);
		chainsInAtari[WHITE.index()].copyDataFrom(original.chainsInAtari[WHITE.index()]);
	}

	/** 返回打吃中给定颜色的所有链的id. */
	public ShortSet getChainsInAtari(StoneColor color) {
		return chainsInAtari[color.index()];
	}

	/**
	 * 移除打吃列表中的任何链或不再是链或不再在打吃中.
	 */
	private void removeInvalidChains(StoneColor color) {
		final int index = color.index();
		final ShortSet chains = chainsInAtari[index];
		for (int i = 0; i < chains.size(); i++) {
			final short p = chains.get(i);
			if (board.getColorAt(p) == VACANT || board.getChainRoot(p) != p || board.getLiberties(p).size() > 1) {
				chains.remove(p);
				i--;
			}
		}
	}

	@Override
	public void update(StoneColor color, short location, ShortList capturedStones) {
		if (location != PASS) {
			removeInvalidChains(color);
			removeInvalidChains(color.opposite());
			if (board.getLiberties(location).size() == 1) {
				chainsInAtari[color.index()].add(board.getChainRoot(location));
			}
			final short[] neighbors = coords.getNeighbors(location);
			for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
				final short n = neighbors[i];
				if (board.getColorAt(n) == color.opposite() && board.getLiberties(n).size() == 1) {
					chainsInAtari[color.opposite().index()].add(board.getChainRoot(n));
				}
			}
		}
	}
}
