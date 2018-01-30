package com.github.fangyun.ginkgo.time;

import static com.github.fangyun.ginkgo.thirdparty.Gaussian.Phi;
import static java.lang.Math.max;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.mcts.Player;
import com.github.fangyun.ginkgo.mcts.SearchNode;
import com.github.fangyun.ginkgo.util.ShortSet;

/**
 * 当他们确信自己找到了一个好的落子时，偶尔会早停下来. 多余的时间被滚动到下一个落子，直到它被使用.
 */
public final class ExitingTimeManager implements TimeManager {

	/** 每一轮分割的片数. */
	private static final int SLICE_COUNT = 3;

	/** 在时间管理公式中使用的常数C. */
	private static final double TIME_CONSTANT = 0.20;

	/**
	 * 返回情况A比B更好的置信度(从0.0到1.0).
	 */
	private static double confidence(float winrateA, double runsA, float winrateB, double runsB) {
		if (runsB == 0) {
			// There are no other moves to consider, so this must be best
			return 1.0;
		}
		final double z = (winrateA - winrateB)
				/ Math.sqrt(winrateA * (1 - winrateA) / runsA + winrateB * (1 - winrateB) / runsB);
		return Phi(z);
	}

	/** 用于确定剩余空点的数量. */
	private final Board board;

	private int msecPerSlice;

	/**剩下的时间留给我们的落子，以毫秒. */
	private int msecRemaining;

	/** 用于查找树的根. */
	private final Player player;

	/** 时间从以前轮来的时间（以毫秒）. */
	private int rollover;

	/** 这个轮次中剩下的时间片数. */
	private int slicesRemaining;

	public ExitingTimeManager(Player player) {
		this.player = player;
		this.board = player.getBoard();
	}

	/**
	 * 返回我们的置信度(从0.0到1.0)，最好的落子比其他的合法落子都有更高的赢率
	 */
	private double confidenceBestVsRest() {
		final SearchNode root = player.getRoot();
		final CoordinateSystem coords = player.getBoard().getCoordinateSystem();
		// win rate and runs of the best move
		final short bestMove = root.getMoveWithMostWins(coords);
		final float bestWinRate = root.getWinRate(bestMove);
		final int bestRuns = root.getRuns(bestMove);
		// runs and wins of the rest of the moves
		int restRuns = 0;
		int restWins = 0;
		final ShortSet vacant = board.getVacantPoints();
		for (int i = 0; i < vacant.size(); i++) {
			final short p = vacant.get(i);
			if (p != bestMove && root.getWinRate(p) > 0.0) {
				final float w = root.getWins(p);
				restWins += w;
				restRuns += root.getRuns(p);
			}
		}
		final float restWinRate = restWins / (float) restRuns;
		if (restWinRate <= 0) {
			return 0;
		}
		final double c = confidence(bestWinRate, bestRuns, restWinRate, restRuns);
		return c;
	}

	/** 设置要使用的时间片的数量和大小. */
	private void createSlices() {
		slicesRemaining = SLICE_COUNT;
		msecPerSlice = (getMsecPerMove() + rollover) / SLICE_COUNT;
	}

	@Override
	public int getMsec() {
		assert player.shouldKeepRunning() == false;
		if (slicesRemaining == 0) {
			rollover = 0;
			return 0;
		}
		if (slicesRemaining < SLICE_COUNT && confidenceBestVsRest() > 0.99) {
			rollover = slicesRemaining * msecPerSlice;
			return 0;
		}
		slicesRemaining--;
		return msecPerSlice;
	}

	/** 计算分配到下一落子的总时间. */
	private int getMsecPerMove() {
		final int movesLeft = max(10, (int) (board.getVacantPoints().size() * TIME_CONSTANT));
		return max(1, msecRemaining / movesLeft);
	}

	/** 返回到下一个回合的毫秒数. */
	int getRollover() {
		return rollover;
	}

	@Override
	public void setRemainingSeconds(int seconds) {
		msecRemaining = (seconds - 10) * 1000 - rollover / 1000;
		createSlices();
	}

	@Override
	public void startNewTurn() {
		// Does nothing; things are reset in setRemainingTime
	}

}
