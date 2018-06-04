package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 建议的最好回复存储在最近最好回复表中. */
public final class LgrfSuggester implements Suggester {
	private static final long serialVersionUID = -6166265907038238165L;

	private final int bias;

	private final Board board;

	private final HistoryObserver history;
	
	private final ShortSet moves;

	private final Predicate filter;
	
	/**
	 * 因为所有的McRunnables共享同一LgrfSuggesters，所以此表是瞬变的.
	 */
	private transient LgrfTable table;
	
	public LgrfSuggester(Board board, HistoryObserver history, LgrfTable table, Predicate filter){
		this(board, history, table, 0, filter);
	}

	public LgrfSuggester(Board board, HistoryObserver history, LgrfTable table, int bias, Predicate filter) {
		this.bias = bias;
		this.board = board;
		this.history = history;
		this.table = table;
		this.filter = filter;
		moves = new ShortSet(board.getCoordinateSystem()
				.getFirstPointBeyondBoard());
	}

	@Override
	public int getBias() {
		return bias;
	}

	@Override
	public ShortSet getMoves() {
		moves.clear();
		final short previousMove = history.get(board.getTurn() - 1);
		short reply = table.getSecondLevelReply(board.getColorToPlay(),
				history.get(board.getTurn() - 2), previousMove);
		if (reply != NO_POINT && board.getColorAt(reply) == VACANT && filter.at(reply)) {
			moves.add(reply);
		} else {
			reply = table.getFirstLevelReply(board.getColorToPlay(), previousMove);
			if (reply != NO_POINT && board.getColorAt(reply) == VACANT && filter.at(reply)) {
				moves.add(reply);
			}
		}
		return moves;
	}

	public void setTable(LgrfTable table) {
		this.table = table;
	}
}
