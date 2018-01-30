package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.FIRST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.LAST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.util.ShortSet;

/**
 * 返回一组落子，允许通过逃跑、合并或吃子的组合来从打吃中逃脱。不能避免倒扑.
 */
public final class EscapeSuggester implements Suggester {
	private static final long serialVersionUID = 2046806007963681312L;

	private final AtariObserver atariObserver;

	private final int bias;

	private final Board board;
	
	private final CoordinateSystem coords;

	/**
	 * 当前棋手的所有落子的列表将允许一个组从打吃中逃脱
	 */
	private final ShortSet movesToEscape;

	/**
	 * 跟踪一个可能合并的链的气.
	 */
	private final ShortSet tempLiberties;
	
	public EscapeSuggester(Board board, AtariObserver atariObserver){
		this(board, atariObserver, 0);
	}

	public EscapeSuggester(Board board, AtariObserver atariObserver, int bias) {
		this.bias = bias;
		this.board = board;
		coords = board.getCoordinateSystem();
		this.atariObserver = atariObserver;
		final int n = coords.getFirstPointBeyondBoard();
		tempLiberties = new ShortSet(n);
		movesToEscape = new ShortSet(n);
	}

	/**
	 * 通过吃外部对手的棋子，发现可以从打吃中逃脱的落子。不能避免倒扑。任何这样的落子都被添加到movesToEscape.
	 *
	 * @param chain
	 *            在打吃中的己方链.
	 */
	private void escapeByCapturing(short chain) {
		final StoneColor enemy = board.getColorToPlay().opposite();
		short p = chain;
		do {
			final short[] neighbors = coords.getNeighbors(p);
			final ShortSet enemiesInAtari = atariObserver.getChainsInAtari(enemy);
			for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
				final short n = neighbors[i];
				final Color color = board.getColorAt(n);
				if (color == enemy) {
					if (enemiesInAtari.contains(board.getChainRoot(n))) {
						movesToEscape.add(board.getLiberties(n).get(0));
					}
				}
			}
			p = board.getChainNextPoint(p);
		} while (p != chain);
	}

	/**
	 * 通过与其他链的合并，发现逃脱打吃的落子. 任何这样的落子都被添加到movesToEscape.
	 *
	 * @param liberty
	 *            在打吃中的当前链的气.
	 */
	private void escapeByMerging(short liberty) {
		tempLiberties.clear();
		final short[] neighbors = coords.getNeighbors(liberty);
		for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
			final short n = neighbors[i];
			if (board.getColorAt(n) == VACANT) {
				tempLiberties.add(n);
			} else if (board.getColorAt(n) == board.getColorToPlay()) {
				final ShortSet neighborsLiberties = board.getLiberties(n);
				if (neighborsLiberties.size() > 1) {
					for (int j = 0; j < neighborsLiberties.size(); j++) {
						tempLiberties.add(neighborsLiberties.get(j));
						// 3 because there need to be 2 left not counting
						// liberty itself
						if (tempLiberties.size() == 3) {
							movesToEscape.add(liberty);
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public int getBias() {
		return bias;
	}

	@Override
	public ShortSet getMoves() {
		movesToEscape.clear();
		final StoneColor colorToPlay = board.getColorToPlay();
		final ShortSet chainsInAtari = atariObserver.getChainsInAtari(colorToPlay);
		for (int i = 0; i < chainsInAtari.size(); i++) {
			final short chain = chainsInAtari.get(i);
			final short p = board.getLiberties(chain).get(0);
			if (board.getNeighborsOfColor(p, VACANT) >= 2) {
				movesToEscape.add(p);
			} else if (board.getNeighborsOfColor(p, colorToPlay) > 0) {
				escapeByMerging(p);
			}
			escapeByCapturing(chain);
		}
		return movesToEscape;
	}
}
