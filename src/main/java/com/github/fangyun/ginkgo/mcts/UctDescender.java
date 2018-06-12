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
	 * 返回为节点的UCT上限。这是UCB1-TUNED的策略，在Gelly的技术报告“Modification of UCT with Patterns in Monte-Carlo Go”中解释。这个公式在第5页的底部。
	 */
	@Override
	public float searchValue(SearchNode node, short move) {
		// 这里选择的变量名是为了与技术报告保持一致
		final double barX = node.getWinRate(move);
		if (barX < 0) { // 如果此着子被排除在外
			return NEGATIVE_INFINITY;
		}
		final double logParentRunCount = log(node.getTotalRuns());
		// 在论文中，term1是奖励平方的平均值;因为这里所有的奖励都是0或1，这就等于奖励的平均值，即，获胜的几率
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
