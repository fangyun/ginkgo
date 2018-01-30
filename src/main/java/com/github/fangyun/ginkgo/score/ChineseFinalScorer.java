package com.github.fangyun.ginkgo.score;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.FIRST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.LAST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.NonStoneColor.OFF_BOARD;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.util.ShortSet;

/**
 * 使用中国规则(区域得分)得分。假设棋盘上的所有棋子都是活的
 */
public final class ChineseFinalScorer implements FinalScorer {
	private static final long serialVersionUID = 2186265679766308790L;

	private final Board board;

	/**
	 * 目前正在探索的区域的棋色，如果没有发现附近的棋子，则是空的.
	 */
	private Color colorToScore;

	private final CoordinateSystem coords;

	/**
	 * 白色的贴目的数量。考虑效率，这是一个负数
	 */
	private final double komi;

	/**
	 * true，如果目前正在探索的领土是潜在有效的，也就是说。没有相邻的两种颜色的棋子.
	 */
	private boolean validTerritory;

	/**
	 * 在领域中使用递归深度优先搜索.
	 */
	private final ShortSet visitedPoints;

	public ChineseFinalScorer(Board board, double komi) {
		this.board = board;
		this.komi = -komi;
		coords = board.getCoordinateSystem();
		visitedPoints = new ShortSet(coords.getFirstPointBeyondBoard());
	}

	@Override
	public double getKomi() {
		return -komi;
	}

	@Override
	public double score() {
		return score(board);
	}

	/**
	 * 在p附近搜索相邻的区域，会返回该区域内空点的数量。还修改了visitedPoints、colorToScore和validTerritory
	 */
	private int searchNeighbors(short p, Board boardToScore) {
		int result = 1;
		final short[] neighbors = coords.getNeighbors(p);
		for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
			final short n = neighbors[i];
			final Color neighborColor = boardToScore.getColorAt(n);
			if (neighborColor == OFF_BOARD) {
				continue;
			}

			if (colorToScore == VACANT) {
				colorToScore = neighborColor;
			}
			if (neighborColor == VACANT) {
				if (!visitedPoints.contains(n)) {
					visitedPoints.add(n);
					result += searchNeighbors(n, boardToScore);
				}
			} else if (neighborColor == colorToScore) {
				continue;
			} else {
				validTerritory = false;
			}
		}
		return result;
	}

	@Override
	public Color winner() {
		final double score = score();
		if (score > 0) {
			return BLACK;
		} else if (score < 0) {
			return WHITE;
		}
		return VACANT;
	}

	@Override
	public double score(Board boardToScore) {
		double result = komi;
		visitedPoints.clear();
		for (final short p : coords.getAllPointsOnBoard()) {
			final Color color = boardToScore.getColorAt(p);
			if (color == BLACK) {
				result++;
			} else if (color == WHITE) {
				result--;
			}

		}
		final ShortSet vacantPoints = boardToScore.getVacantPoints();
		for (int i = 0; i < vacantPoints.size(); i++) {
			final short p = vacantPoints.get(i);
			if (visitedPoints.contains(p)) {
				continue;
			}
			colorToScore = VACANT;
			validTerritory = true;
			visitedPoints.add(p);
			final int territory = searchNeighbors(p, boardToScore);
			if (validTerritory) {
				if (colorToScore == WHITE) {
					result -= territory;
				} else {
					result += territory;
				}
			}
		}
		return result;
	}
}
