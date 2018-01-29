package com.github.fangyun.ginkgo.score;

import com.github.fangyun.ginkgo.core.Board;

/**
 * 决定棋终的得分.
 *
 * @see {@link PlayoutScorer}
 */
public interface FinalScorer extends Scorer {

	double score(Board stonesRemoved);
	
}