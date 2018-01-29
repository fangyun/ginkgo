package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.Legality.OK;
import static com.github.fangyun.ginkgo.core.NonStoneColor.*;
import static com.github.fangyun.ginkgo.core.StoneColor.*;
import static com.github.fangyun.ginkgo.experiment.Logging.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.fangyun.ginkgo.book.OpeningBook;
import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.Legality;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.experiment.Logging;
import com.github.fangyun.ginkgo.feature.HistoryObserver;
import com.github.fangyun.ginkgo.score.FinalScorer;
import com.github.fangyun.ginkgo.time.TimeManager;
import com.github.fangyun.ginkgo.util.ShortList;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 下子. */
public final class Player {

	private final Board board;

	private OpeningBook book;

	/**
	 * true 如果我们应到搜索对手死棋和会杀死他们的偏子.
	 */
	private boolean cleanupMode;

	private final CoordinateSystem coords;

	/**
	 * true 如果优雅的妙招被打开.
	 */
	private boolean coupDeGrace;

	private TreeDescender descender;

	/** 管理线程. */
	private ExecutorService executor;

	private final FinalScorer finalScorer;

	private final HistoryObserver historyObserver;

	/**
	 * True，如果线程保持运行。例如因为时间还没有用完.
	 */
	private boolean keepRunning;

	/** 用来校验所有已经停止的McRunnables. */
	private CountDownLatch latch;

	/** 下一步运行花费的毫秒. */
	private int msecPerMove;

	/** True 如果在对手轮次我们还可以思考. */
	private boolean ponder;

	/** 运行的棋局. */
	private final McRunnable[] runnables;

	/**
	 * True，如果setTimeRemaining已经被调用, 因为一个time_left被收到. 如果true，用时间管理器. 否则只用为每一落子用分配的msecPerMove.
	 */
	private boolean timeLeftWasSent;

	/** 用来计算产生一步落子所用时间. */
	private TimeManager timeManager;

	private TreeUpdater updater;

	/**
	 * @param threads
	 *            运行的线程数目.
	 * @param stuff
	 *            棋盘相关的BoardObservers, Mover等.
	 */
	public Player(int threads, CopiableStructure stuff) {
		// TODO Is this expensive copying (which includes the transposition and shape tables) necessary?
		// What about in making the McRunnables? Sure, it only happens once, but still.
		final CopiableStructure copy = stuff.copy();
		board = copy.get(Board.class);
		coords = board.getCoordinateSystem();
		historyObserver = copy.get(HistoryObserver.class);
		finalScorer = copy.get(FinalScorer.class);
		runnables = new McRunnable[threads];
		for (int i = 0; i < runnables.length; i++) {
			runnables[i] = new McRunnable(this, stuff);
		}
		descender = new DoNothing();
		updater = new DoNothing();
		book = new DoNothing();
		timeLeftWasSent = false;
	}

	/** 落子在点p. */
	public Legality acceptMove(short point) {
		stopThreads();
		final Legality legality = board.play(point);
		assert legality == OK;
		updater.updateForAcceptMove();
		if (ponder) {
			startThreads();
		}
		return legality;
	}

	/** 运行McRunnables一段时间，后返回最佳落子. */
	public short bestMove() {
		stopThreads();
		final short move = book.nextMove(board);
		if (move != NO_POINT) {
			return move;
		}
		if (cleanupMode) {
			if (!findCleanupMoves()) {
				return PASS;
			}
		} else if (board.getPasses() == 1 && coupDeGrace) {
			if (canWinByPassing()) {
				return PASS;
			}
			findCleanupMoves();
		}
		if (!timeLeftWasSent) {
			// No time left signal was received
			startThreads();
			try {
				Thread.sleep(msecPerMove);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			stopThreads();
		} else {
			// Time left signal was received
			timeManager.startNewTurn();
			msecPerMove = timeManager.getMsec();
			log("Allocating " + msecPerMove + " msec");
			do {
				startThreads();
				try {
					Thread.sleep(msecPerMove);
				} catch (final InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
				stopThreads();
				msecPerMove = timeManager.getMsec();
			} while (msecPerMove > 0);
		}
		long playouts = 0;
		for (McRunnable runnable : runnables) {
			playouts += runnable.getPlayoutsCompleted();
		}
		Logging.log("Turn : " + board.getTurn() + " Playouts : " + playouts);
		return descender.bestPlayMove();
	}

	/**
	 * 返回true，如果我们通过虚手能赢, 假定所有死棋已移除，所有对手棋还活着.
	 */
	boolean canWinByPassing() {
		final ShortSet ourDead = findDeadStones(1.0, board.getColorToPlay());
		final Board stonesRemoved = getMcRunnable(0).getBoard();
		stonesRemoved.copyDataFrom(board);
		stonesRemoved.removeStones(ourDead);
		final double score = finalScorer.score(stonesRemoved);
		if (board.getColorToPlay() == WHITE) {
			if (score < 0) {
				return true;
			}
		} else {
			if (score > 0) {
				return true;
			}
		}
		return false;
	}

	/** 清除棋盘，做必要的事情以开始新的对局. */
	public void clear() {
		stopThreads();
		board.clear();
		descender.clear();
		updater.clear();
		cleanupMode = false;
	}

	/** 在树中下任意落子(或其它结构). */
	public void descend(McRunnable runnable) {
		descender.descend(runnable);
	}

	/** 终止任何运行的线程. */
	public void endGame() {
		stopThreads();
	}

	/** @see com.github.fangyun.ginkgo.score.FinalScorer#score */
	public double finalScore() {
		return finalScorer.score();
	}

	/**
	 * 清除对手死链离开棋盘导致的偏置落子. 返回true，如果存在这样的落子.
	 */
	private boolean findCleanupMoves() {
		log("Finding cleanup moves");
		final ShortSet enemyDeadChains = findDeadStones(1.0, board
				.getColorToPlay().opposite());
		log("Dead stones: "
				+ enemyDeadChains.toString(board.getCoordinateSystem()));
		if (enemyDeadChains.size() == 0) {
			return false;
		}
		getRoot().exclude(PASS);
		final ShortSet pointsToBias = new ShortSet(board.getCoordinateSystem()
				.getFirstPointBeyondBoard());
		for (int i = 0; i < enemyDeadChains.size(); i++) {
			final short p = enemyDeadChains.get(i);
			if (p == board.getChainRoot(p)) {
				pointsToBias.addAll(board.getLiberties(p));
			}
		}
		final SearchNode root = getRoot();
		final int bias = (int) root.getWins(root.getMoveWithMostWins(board
				.getCoordinateSystem()));
		for (int i = 0; i < pointsToBias.size(); i++) {
			root.update(pointsToBias.get(i), bias, bias);
		}
		root.setWinningMove(NO_POINT);
		return true;
	}

	/**
	 * 返回那些没有幸存许多随机棋局的棋子列表.
	 * 
	 * @param threshold
	 *            棋局部分幸存下才考虑为活棋.
	 * @param color
	 *            正在考察的棋子颜色.
	 */
	public ShortSet findDeadStones(double threshold, StoneColor color) {
		final boolean threadsWereRunning = keepRunning;
		stopThreads();
		// Perform a bunch of runs to see which stones survive
		final McRunnable runnable = getMcRunnable(0);
		final Board runnableBoard = runnable.getBoard();
		final int runs = 100;
		final ShortSet deadStones = new ShortSet(board.getCoordinateSystem()
				.getFirstPointBeyondBoard());
		final int[] survivals = new int[board.getCoordinateSystem()
				.getFirstPointBeyondBoard()];
		for (int i = 0; i < runs; i++) {
			// Temporarily set passes to 0 so that we can run playouts beyond
			// this point
			short passes = board.getPasses();
			board.setPasses((short)0);
			runnable.performMcRun(false);
			board.setPasses(passes);
			for (final short p : board.getCoordinateSystem()
					.getAllPointsOnBoard()) {
				if (runnableBoard.getColorAt(p) == board.getColorAt(p)) {
					survivals[p]++;
				}
			}
		}
		// Gather all of the dead stones into a list to return
		for (final short p : board.getCoordinateSystem().getAllPointsOnBoard()) {
			if (board.getColorAt(p) == color) {
				if (survivals[p] < runs * threshold) {
					deadStones.add(p);
				}
			}
		}
		// Restart the threads if appropriate
		if (threadsWereRunning) {
			startThreads();
		}
		// Return the list of dead stones
		log("Dead stones: " + deadStones.toString(board.getCoordinateSystem()));
		return deadStones;
	}

	/** 返回棋手关联的棋盘. */
	public Board getBoard() {
		return board;
	}

	TreeDescender getDescender() {
		return descender;
	}

	/** 返回评分器. */
	public FinalScorer getFinalScorer() {
		return finalScorer;
	}

	/** 返回第i个McRunnable. */
	public McRunnable getMcRunnable(int i) {
		return runnables[i];
	}

	int getMsecPerMove() {
		return msecPerMove;
	}

	/** 返回棋手运行的线程个数. */
	int getNumberOfThreads() {
		return runnables.length;
	}

	public int getPlayoutCount() {
		int playouts = 0;
		for (final McRunnable runnable : runnables) {
			playouts += runnable.getPlayoutsCompleted();
		}
		return playouts;
	}

	public SearchNode getRoot() {
		return updater.getRoot();
	}

	public TimeManager getTimeManager() {
		return timeManager;
	}

	/** 返回此棋手的更新器 */
	TreeUpdater getUpdater() {
		return updater;
	}

	/** 指出一个McRunnable已经停止. */
	void notifyMcRunnableDone() {
		latch.countDown();
	}

	/** 设置是否在对手的轮次我们仍继续思考. */
	public void ponder(boolean pondering) {
		this.ponder = pondering;
	}

	/** 设置清理模式，GTP标准需要. */
	public void setCleanupMode(boolean cleanup) {
		cleanupMode = cleanup;
	}

	/**
	 * 设置将要落子的棋的棋色，用在GoGui来设置初始棋子.
	 */
	public void setColorToPlay(StoneColor stoneColor) {
		board.setColorToPlay(stoneColor);
	}

	/**
	 * 设置是否在对手虚手后我们尝试杀对手的死棋.
	 */
	public void setCoupDeGrace(boolean enabled) {
		coupDeGrace = enabled;
	}

	/** 设置每次落子分配的毫秒数. */
	public void setMsecPerMove(int msec) {
		msecPerMove = msec;
	}

	/** 设置使用的开放书，缺省是DoNothing. */
	public void setOpeningBook(OpeningBook book) {
		this.book = book;
	}

	/** 处理来自GTP的剩余时间信号. */
	public void setRemainingTime(int seconds) {
		timeLeftWasSent = true;
		timeManager.setRemainingSeconds(seconds);
	}

	public void setTimeManager(TimeManager time) {
		timeManager = time;
	}

	public void setTreeDescender(TreeDescender descender) {
		this.descender = descender;

	}

	/** @see TreeUpdater */
	public void setTreeUpdater(TreeUpdater updater) {
		this.updater = updater;
	}

	/** 放置标准让子棋的棋子. */
	public void setUpHandicap(int handicapSize) {
		clear();
		board.setUpHandicap(handicapSize);
	}

	/** 放置从SGF游戏读来的落子. */
	public void setUpSgfGame(List<Short> moves) {
		board.clear();
		for (final Short move : moves) {
			if (board.play(move) != OK) {
				throw new IllegalArgumentException("SGF包含非法落子");
			}
		}
	}

	/** true如果棋手的McRunnables应当保持运行. */
	public boolean shouldKeepRunning() {
		return keepRunning;
	}

	/** 启动McRunnables线程. */
	private void startThreads() {
		if (keepRunning) {
			log("Threads were already running");
			return; // If the threads were already running, do nothing
		}
		log("Starting threads");
		SearchNode root = getRoot();
		if (!root.biasUpdated()) {
			getMcRunnable(0).copyDataFrom(board);
			root.updateBias(getMcRunnable(0));
		}
		keepRunning = true;
		int n = runnables.length; // # of threads
		latch = new CountDownLatch(n);
		executor = Executors.newFixedThreadPool(n);
		for (int i = 0; i < n; i++) {
			executor.execute(runnables[i]);
		}
		executor.shutdown();
	}

	/** 停止McRunnables线程. */
	private void stopThreads() {
		if (!keepRunning) {
			log("Threads were already stopped");
			return; // If the threads were not running, do nothing
		}
		log("Stopping threads");
		try {
			keepRunning = false;
			latch.await();
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public String toString() {
		return descender.toString();
	}

	/**
	 * 撤回最后落子. 通过清理棋盘，然后重放所有落子除了最后一步.
	 * 
	 * @return true 如果撤回成功 (例如, 不是在棋局开始).
	 */
	public boolean undo() {
		if (board.getTurn() == 0) {
			return false;
		}
		final boolean alreadyRunning = keepRunning;
		stopThreads();
		final ShortList movesList = new ShortList(board.getCoordinateSystem()
				.getMaxMovesPerGame());
		for (int i = 0; i < historyObserver.size() - 1; i++) {
			movesList.add(historyObserver.get(i));
		}
		// Now replay the moves
		board.clearPreservingInitialStones();
		updater.clear();
		for (int i = 0; i < movesList.size(); i++) {
			board.play(movesList.get(i));
		}
		if (alreadyRunning) {
			startThreads();
		}
		return true;
	}

	/** 合并一次运行结果到树中. */
	public void updateTree(Color winner, McRunnable mcRunnable) {
		updater.updateTree(winner, mcRunnable);
	}

	/**
	 * 得到棋盘活在至少可能阀值的所有棋子.
	 */
	public ShortSet getLiveStones(double threshold) {
		ShortSet deadStones = findDeadStones(threshold, WHITE);
		deadStones.addAll(findDeadStones(threshold, BLACK));
		ShortSet liveStones = new ShortSet(board.getCoordinateSystem()
				.getFirstPointBeyondBoard());
		for (short p : board.getCoordinateSystem().getAllPointsOnBoard()) {
			if (board.getColorAt(p) != VACANT && !deadStones.contains(p)) {
				liveStones.add(p);
			}
		}
		log("Live stones: " + liveStones.toString(board.getCoordinateSystem()));
		return liveStones;
	}

	/** 返回GoGui信息显示搜索值. */
	public String goguiSearchValues() {
		// TODO Encapsulate this normalization in a single place, called by all
		// the various gogui methods
		double min = 1.0;
		double max = 0.0;
		for (short p : coords.getAllPointsOnBoard()) {
			if (board.getColorAt(p) == VACANT) {
				double searchValue = descender.searchValue(getRoot(), p);
				if (searchValue > 0) {
					min = Math.min(min, searchValue);
					max = Math.max(max, searchValue);
				}
			}
		}
		String result = "";
		for (short p : coords.getAllPointsOnBoard()) {
			if (getBoard().getColorAt(p) == VACANT) {
				double searchValue = descender.searchValue(getRoot(), p);
				if (searchValue > 0) {
					if (result.length() > 0) {
						result += "\n";
					}
					int green = (int) (255 * (searchValue - min) / (max - min));
					result += String.format("COLOR %s %s\nLABEL %s %.0f%%",
							String.format("#%02x%02x00", 255 - green, green),
							coords.toString(p), coords.toString(p),
							searchValue * 100);
				}
			}
		}
		return result;
	}

	public String goguiGetWins() {
		// TODO Encapsulate this normalization in a single place, called by all
		// the various gogui methods
		float min = Float.MAX_VALUE;
		float max = 0.0f;
		for (short p : coords.getAllPointsOnBoard()) {
			if (board.getColorAt(p) == VACANT) {
				float wins = getRoot().getWins(p);
				if (wins > 0) {
					min = Math.min(min, wins);
					max = Math.max(max, wins);
				}
			}
		}
		String result = "";
		for (short p : coords.getAllPointsOnBoard()) {
			if (getBoard().getColorAt(p) == VACANT) {
				float wins = getRoot().getWins(p);
				if (wins > 0) {
					if (result.length() > 0) {
						result += "\n";
					}
					int green = (int) (255 * (wins - min) / (max - min));
					result += String.format("COLOR %s %s\nLABEL %s %.0f",
							String.format("#%02x%02x00", 255 - green, green),
							coords.toString(p), coords.toString(p), wins);
				}
			}
		}
		return result;
	}

	public String goguiGetWinrate() {
		// TODO Encapsulate this normalization in a single place, called by all
		// the various gogui methods
		float min = Float.MAX_VALUE;
		float max = 0.0f;
		for (short p : coords.getAllPointsOnBoard()) {
			if (board.getColorAt(p) == VACANT) {
				float winRate = getRoot().getWinRate(p);
				if (winRate > 0) {
					min = Math.min(min, winRate);
					max = Math.max(max, winRate);
				}
			}
		}
		String result = "";
		for (short p : coords.getAllPointsOnBoard()) {
			if (getBoard().getColorAt(p) == VACANT) {
				float winRate = getRoot().getWinRate(p);
				if (winRate > 0) {
					if (result.length() > 0) {
						result += "\n";
					}
					int green = (int) (255 * (winRate - min) / (max - min));
					result += String.format("COLOR %s %s\nLABEL %s %.0f%%",
							String.format("#%02x%02x00", 255 - green, green),
							coords.toString(p), coords.toString(p),
							(winRate * 100));
				}
			}
		}
		return result;
	}

	public String goguiGetRuns() {
		// TODO Encapsulate this normalization in a single place, called by all
		// the various gogui methods
		float min = Float.MAX_VALUE;
		float max = 0.0f;
		for (short p : coords.getAllPointsOnBoard()) {
			if (board.getColorAt(p) == VACANT) {
				float runs = getRoot().getRuns(p);
				if (runs > 0) {
					min = Math.min(min, runs);
					max = Math.max(max, runs);
				}
			}
		}
		String result = "";
		for (short p : coords.getAllPointsOnBoard()) {
			if (getBoard().getColorAt(p) == VACANT) {
				float runs = getRoot().getRuns(p);
				if (runs > 0) {
					if (result.length() > 0) {
						result += "\n";
					}
					int green = (int) (255 * (runs - min) / (max - min));
					result += String.format("COLOR %s %s\nLABEL %s %.0f",
							String.format("#%02x%02x00", 255 - green, green),
							coords.toString(p), coords.toString(p), runs);
				}
			}
		}
		return result;
	}
}
