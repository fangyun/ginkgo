package com.github.fangyun.ginkgo.mcts;

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import com.github.fangyun.ginkgo.core.Board;

/** 使用UCT. */
public final class UctDescender extends AbstractDescender {

	public UctDescender(Board board, TranspositionTable table, int biasDelay) {
		super(board, table, biasDelay);
	}

	/**
	 * 返回为节点的UCT上限。这是UCB1-TUNED的策略，在Gelly的技术报告中解释道，“在蒙特-卡罗的围棋中，对UCT的修改”。这个公式在第5页的底部。
	 */
	@Override
	public float searchValue(SearchNode node, short move) {
		// The variable names here are chosen for consistency with the tech
		// report
		final double barX = node.getWinRate(move);
		if (barX < 0) { // if the move has been excluded
			return NEGATIVE_INFINITY;
		}
		final double logParentRunCount = log(node.getTotalRuns());
		// In the paper, term1 is the mean of the SQUARES of the rewards; since
		// all rewards are 0 or 1 here, this is equivalent to the mean of the
		// rewards, i.e., the win rate.
		final double term1 = barX;
		final double term2 = -(barX * barX);
		final double term3 = sqrt(2 * logParentRunCount / node.getRuns(move));
		final double v = term1 + term2 + term3; // This equation is above Eq. 1
		assert v >= 0 : "Negative variability in UCT";
		final double factor1 = logParentRunCount / node.getRuns(move);
		final double factor2 = min(0.25, v);
		final double uncertainty = 0.4 * sqrt(factor1 * factor2);
		return (float) (uncertainty + barX);
	}

}
