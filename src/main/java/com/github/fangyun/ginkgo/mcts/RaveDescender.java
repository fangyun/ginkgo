package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static java.lang.Float.NEGATIVE_INFINITY;
import com.github.fangyun.ginkgo.core.Board;

/** 使用快速行动价值估算的下降器. */
public final class RaveDescender extends AbstractDescender {

	/**
	 * 这对应于b^2/(0.5*0.5)在银的公式中。这个值越高，RAVE对它的关注就越少.
	 */
	private final float raveBias;

	public RaveDescender(Board board, TranspositionTable table, int biasDelay) {
		super(board, table, biasDelay);
		raveBias = 0.0009f;
	}

	/**
	 * 返回给定RAVE的权重（与直接的MC数据相反），给定c运行和rc RAVE运行.
	 */
	private float raveCoefficient(float c, float rc) {
		return rc / (rc + c + rc * c * raveBias);
	}

	/**
	 * 使用下面的公式:David Silver强化学习和基于计算机围棋的模拟搜索
	 * http://papersdb.cs.ualberta.ca/~papersdb/uploaded_files/
	 * 1029/paper_thesis.pdf equation 8.40 page 107//Updated
	 */
	@Override
	public float searchValue(SearchNode node, short move) {
		if (node.getWinRate(move) < 0.0f) {
			return NEGATIVE_INFINITY;
		}
		if (move == PASS) {
			return node.getWinRate(move);
		}
		final RaveNode raveNode = (RaveNode) node;
		final float c = raveNode.getRuns(move);
		final float r = raveNode.getWinRate(move);
		final float rc = raveNode.getRaveRuns(move);
		final float rr = raveNode.getRaveWinRate(move);
		final float coef = raveCoefficient(c, rc);
		return r * (1 - coef) + rr * coef;
	}

}
