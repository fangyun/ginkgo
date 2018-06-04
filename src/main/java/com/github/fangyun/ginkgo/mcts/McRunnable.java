package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.Legality.OK;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import static com.github.fangyun.ginkgo.experiment.Logging.log;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.Legality;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.feature.LgrfSuggester;
import com.github.fangyun.ginkgo.feature.LgrfTable;
import com.github.fangyun.ginkgo.feature.Predicate;
import com.github.fangyun.ginkgo.feature.Rater;
import com.github.fangyun.ginkgo.feature.ShapeRater;
import com.github.fangyun.ginkgo.feature.StoneCountObserver;
import com.github.fangyun.ginkgo.feature.Suggester;
import com.github.fangyun.ginkgo.move.Mover;
import com.github.fangyun.ginkgo.patterns.ShapeTable;
import com.github.fangyun.ginkgo.score.ChinesePlayoutScorer;
import com.github.fangyun.ginkgo.score.PlayoutScorer;
import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;
import com.github.fangyun.ginkgo.util.ShortList;
import com.github.fangyun.ginkgo.util.ShortSet;

/**
 * 棋手使用此类在不同线程中运行多个蒙特卡洛运算.
 */
public final class McRunnable implements Runnable {

	/** McRunnable执行所在的棋盘. */
	private final Board board;

	private final ShortList candidates;

	private final CoordinateSystem coords;

	/** @see #getFancyHashes() */
	private final long[] fancyHashes;

	/** 没通过此过滤棋的着子不能下子. */
	private final Predicate filter;

	/** 跟踪着子. */
	private final HistoryObserver historyObserver;

	/** 着子计数为了快速结束棋局. */
	private final StoneCountObserver mercyObserver;

	/** 产生超出树的着子. */
	private final Mover mover;

	/**
	 * 被RaveNode.recordPlayout所用. 存在此而不是在RaveNode是为了避免创建百万的ShortSet.
	 */
	private final ShortSet playedPoints;

	/** 发起此McRunnable的棋手. */
	private final Player player;

	/** 已完成的棋局. */
	private long playoutsCompleted;

	/** 随机数发生器. */
	private final MersenneTwisterFast random;

	/** 判定棋局的胜方. */
	private final PlayoutScorer scorer;

	/** 一组建议器用来更新偏置量. */
	private Suggester[] suggesters;

	/** 一组赢率用来更新偏置量. */
	private Rater[] raters;

	public McRunnable(Player player, CopiableStructure stuff) {
		LgrfTable table = null;
		try {
			table = stuff.get(LgrfTable.class);
		} catch (final IllegalArgumentException e) {
			// If we get here, we're not using LGRF
		}
		final CopiableStructure copy = stuff.copy();
		board = copy.get(Board.class);
		coords = board.getCoordinateSystem();
		candidates = new ShortList(coords.getArea());
		ShapeTable shapeTable = null;
		ShapeRater shape = null;
		try {
			shapeTable = stuff.get(ShapeTable.class);
			shape = copy.get(ShapeRater.class);
			shape.setTable(shapeTable);
		} catch (final IllegalArgumentException e) {
			// If we get here, we're not using shape
		}
		suggesters = copy.get(Suggester[].class);
		try {
			raters = copy.get(Rater[].class);
		} catch (final IllegalArgumentException e) {
			raters = new Rater[0];
		}
		if (shape != null) {
			raters[0] = shape;
		}
		this.player = player;
		random = new MersenneTwisterFast();
		mover = copy.get(Mover.class);
		if (table != null) {
			final LgrfSuggester lgrf = copy.get(LgrfSuggester.class);
			lgrf.setTable(table);
		}
		scorer = copy.get(ChinesePlayoutScorer.class);
		mercyObserver = copy.get(StoneCountObserver.class);
		historyObserver = copy.get(HistoryObserver.class);
		filter = copy.get(Predicate.class);
		fancyHashes = new long[coords.getMaxMovesPerGame() + 1];
		playedPoints = new ShortSet(coords.getFirstPointBeyondBoard());
	}

	/**
	 * 接受一步棋.
	 *
	 * @see com.github.fangyun.ginkgo.core.Board#play(short)
	 */
	public void acceptMove(short p) {
		final Legality legality = board.play(p);
		assert legality == OK : "Legality " + legality + " for move " + coords.toString(p) + "\n" + board;
		// TODO Move the fancy hashes out to separate BoardObservers observing
		// board
		// (or just replay the moves, as in LiveShapeUpdater).
		fancyHashes[board.getTurn()] = board.getFancyHash();
	}

	/** 拷贝棋盘数据. */
	public void copyDataFrom(Board that) {
		board.copyDataFrom(that);
		fancyHashes[board.getTurn()] = board.getFancyHash();
	}

	/** 返回runnable关联的棋盘. */
	public Board getBoard() {
		return board;
	}

	/**
	 * 返回在此轮运算中每个访问过的搜索节点的哈希序列. 在实际棋盘轮次（包括）和McRunnable轮次(排除)是有效.
	 */
	public long[] getFancyHashes() {
		return fancyHashes;
	}

	public HistoryObserver getHistoryObserver() {
		return historyObserver;
	}

	/**
	 * @return 已有着子
	 */
	public ShortSet getPlayedPoints() {
		return playedPoints;
	}

	/** @return 本次runnable关联的棋手 */
	public Player getPlayer() {
		return player;
	}

	/** 返回本次runnable的完成的棋局数. */
	public long getPlayoutsCompleted() {
		return playoutsCompleted;
	}

	/** 返回本次runnable关联的随机数发生器. */
	public MersenneTwisterFast getRandom() {
		return random;
	}

	/** 返回一组建议器. */
	public Suggester[] getSuggesters() {
		return suggesters;
	}

	/** 返回在此runnable的棋盘上的手数. */
	public int getTurn() {
		return board.getTurn();
	}

	/** 返回true，如果点p通过了McRunnable的过滤器. */
	public boolean isFeasible(short p) {
		return filter.at(p);
	}

	/**
	 * 执行单次Monte Carlo运算然后合并到棋手的搜索树中. 棋手应当产生着子到已知树的前端，后返回. McRunnable执行实际超越树的棋局,
	 * 再调用棋手的incorporateRun方法.
	 *
	 * @return 胜方颜色，尽管只是为测试用.
	 */
	public Color performMcRun() {
		return performMcRun(true);
	}

	/**
	 * @param mercy
	 *            true，如果在一种棋色远多于另一种棋色时，我们放弃棋局.
	 */
	public Color performMcRun(boolean mercy) {
		copyDataFrom(player.getBoard());
		player.descend(this);
		Color winner;
		if (board.getPasses() == 2) {
			winner = scorer.winner();
		} else {
			winner = playout(mercy);
		}
		player.updateTree(winner, this);
		playoutsCompleted++;
		return winner;
	}

	/**
	 * 着子到棋局终止，返回胜方: BLACK, WHITE,或 (很少的平局或取消棋局因为达到最大着子数) VACANT.
	 * 
	 * @param mercy
	 *            true，如果在一种棋色远多于另一种棋色时，我们放弃棋局.
	 */
	public Color playout(boolean mercy) {
		// The first move is played normally, updating the fancy hashes
		if (board.getTurn() >= coords.getMaxMovesPerGame()) {
			// Playout ran out of moves, probably due to superko
			return VACANT;
		}
		if (board.getPasses() < 2) {
			selectAndPlayOneMove(false);
			fancyHashes[board.getTurn()] = board.getFancyHash();
		}
		if (board.getPasses() >= 2) {
			// Game ended
			return scorer.winner();
		}
		if (mercy) {
			final Color mercyWinner = mercyObserver.mercyWinner();
			if (mercyWinner != null) {
				// One player has far more stones on the board
				return mercyWinner;
			}
		}
		// All subsequent moves are played fast
		do {
			if (board.getTurn() >= coords.getMaxMovesPerGame()) {
				// Playout ran out of moves, probably due to superko
				return VACANT;
			}
			if (board.getPasses() < 2) {
				selectAndPlayOneMove(true);
			}
			if (board.getPasses() >= 2) {
				// Game ended
				return scorer.winner();
			}
			if (mercy) {
				final Color mercyWinner = mercyObserver.mercyWinner();
				if (mercyWinner != null) {
					// One player has far more stones on the board
					return mercyWinner;
				}
			}
		} while (true);
	}

	/**
	 * 执行计算然后合并结果到棋手的搜索树直到线程中断.
	 */
	@Override
	public void run() {
		playoutsCompleted = 0;
		while (getPlayer().shouldKeepRunning()) {
			performMcRun();
		}
		log("Playouts completed: " + playoutsCompleted);
		player.notifyMcRunnableDone();
	}

	/**
	 * @param fast
	 *            如果true，采用playFast代替play.
	 */
	private short selectAndPlayOneMove(boolean fast) {
		return mover.selectAndPlayOneMove(random, fast);
	}

	public Rater[] getRaters() {
		return raters;
	}

	/**
	 * 返回临时存储的候选着子，以备随机选择.
	 */
	public ShortList getCandidates() {
		return candidates;
	}

}
