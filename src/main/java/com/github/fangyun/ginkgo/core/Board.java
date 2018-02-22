package com.github.fangyun.ginkgo.core;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.FIRST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.LAST_ORTHOGONAL_NEIGHBOR;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.Legality.GAME_TOO_LONG;
import static com.github.fangyun.ginkgo.core.Legality.KO_VIOLATION;
import static com.github.fangyun.ginkgo.core.Legality.OCCUPIED;
import static com.github.fangyun.ginkgo.core.Legality.OK;
import static com.github.fangyun.ginkgo.core.Legality.SUICIDE;
import static com.github.fangyun.ginkgo.core.NonStoneColor.OFF_BOARD;
import static com.github.fangyun.ginkgo.core.NonStoneColor.VACANT;
import static com.github.fangyun.ginkgo.core.Point.EDGE_INCREMENT;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;

import java.io.Serializable;

import com.github.fangyun.ginkgo.feature.BoardObserver;
import com.github.fangyun.ginkgo.util.ShortList;
import com.github.fangyun.ginkgo.util.ShortSet;

/** 管理棋盘，检测合法下子等. */
public final class Board implements Serializable {
	private static final long serialVersionUID = -4434335541051930600L;

	/** 让子棋的位置. */
	private final static String[] HANDICAP_LOCATIONS = { "d4", "q16", "q4", "d16", "k10", "d10", "q10", "k4", "k16" };

	/** 被最近落子提走的棋子. */
	private final ShortList capturedStones;

	/** 下步棋的棋子颜色. */
	private StoneColor colorToPlay;

	/** 给定宽度的坐标系统. */
	private final CoordinateSystem coords;

	/**
	 * 最近落子后的对手的ID链，用于isSuicidal()和isSelfAtari()方法.
	 */
	private final ShortList enemyNeighboringChainIds;

	/** 最近落子后己方的ID链. */
	private final ShortList friendlyNeighboringChainIds;

	/**
	 * 当前棋盘位置的佐布里斯特哈希.
	 *
	 * @see #getHash()
	 */
	private long hash;

	/**
	 * 用于撤销落子，因此我们有一个初始化已下棋子的记录.
	 */
	private final ShortSet[] initialStones;

	/** 劫点. */
	private short koPoint;

	/** 刚落子的棋的直接气. */
	private final ShortSet lastPlayLiberties;

	/** 刚被吃子的邻居，用在removeStone(). */
	private final ShortList neighborsOfCapturedStone;

	/** 棋盘观察者. */
	private BoardObserver[] observers;

	/** 刚落子的连续虚手数. */
	private short passes;

	/** 在棋盘上的点(被哨兵点围绕). */
	private final Point[] points;

	/** 提子后的哈希值. */
	private long proposedHash;

	/**
	 * 为打劫校验而存储的所有棋盘以前未知的哈希表。此处哈希码不含简单劫点.
	 */
	private final SuperKoTable superKoTable;

	/** @see #getTurn() */
	private short turn;

	/** 空点集合. */
	private final ShortSet vacantPoints;

	public Board(int width) {
		coords = CoordinateSystem.forWidth(width);
		points = new Point[coords.getFirstPointBeyondExtendedBoard()];
		friendlyNeighboringChainIds = new ShortList(4);
		enemyNeighboringChainIds = new ShortList(4);
		capturedStones = new ShortList(coords.getArea());
		final int n = coords.getFirstPointBeyondBoard();
		lastPlayLiberties = new ShortSet(n);
		superKoTable = new SuperKoTable(coords);
		vacantPoints = new ShortSet(n);
		for (short p = 0; p < points.length; p++) {
			points[p] = new Point(coords, p);
		}
		neighborsOfCapturedStone = new ShortList(4);
		observers = new BoardObserver[0];
		initialStones = new ShortSet[] { new ShortSet(n), new ShortSet(n) };
		clear();
	}

	/**
	 * 增加此棋盘的一个观察者.
	 */
	public void addObserver(BoardObserver observer) {
		// 断言棋盘还没有任何操作
		assert hash == SuperKoTable.EMPTY;
		assert turn == 0;
		observers = java.util.Arrays.copyOf(observers, observers.length + 1);
		observers[observers.length - 1] = observer;
	}

	/**
	 * 处理空链毗邻当前落子点p，或者杀棋、或者减少气数.
	 */
	private void adjustEnemyNeighbors(short p) {
		capturedStones.clear();
		for (int i = 0; i < enemyNeighboringChainIds.size(); i++) {
			final short enemy = enemyNeighboringChainIds.get(i);
			if (points[enemy].isInAtari()) {
				short s = enemy;
				do {
					removeStone(s);
					s = points[s].chainNextPoint;
				} while (s != enemy);
			} else {
				points[enemy].liberties.removeKnownPresent(p);
			}
		}
	}

	/**
	 * 处理己方邻居毗邻刚落子点p，必要时合并棋串.
	 */
	private void adjustFriendlyNeighbors(short p) {
		if (friendlyNeighboringChainIds.size() == 0) {
			// 如果没有己方邻居，创建单子链
			points[p].becomeOneStoneChain(lastPlayLiberties);
		} else {
			short c = friendlyNeighboringChainIds.get(0);
			points[p].addToChain(points[c]);
			points[c].liberties.addAll(lastPlayLiberties);
			if (friendlyNeighboringChainIds.size() > 1) {
				// If there are several friendly neighbors, merge them
				for (int i = 1; i < friendlyNeighboringChainIds.size(); i++) {
					final short ally = friendlyNeighboringChainIds.get(i);
					if (points[c].liberties.size() >= points[ally].liberties.size()) {
						mergeChains(c, ally);
					} else {
						mergeChains(ally, c);
						c = ally;
					}

				}
			}
			points[c].liberties.removeKnownPresent(p);
		}
	}

	/**
	 * 设置棋盘为空白状态. 删除任何初始化的棋子，贴目设置为缺省值. 这粗略的等同于新建一个实例，但(a)这样更快，(b) 对棋盘的引用没有发生改变.
	 */
	public void clear() {
		colorToPlay = BLACK;
		hash = SuperKoTable.EMPTY;
		koPoint = NO_POINT;
		passes = 0;
		superKoTable.clear();
		turn = 0;
		vacantPoints.clear();
		for (final ShortSet stones : initialStones) {
			stones.clear();
		}
		for (final short p : coords.getAllPointsOnBoard()) {
			points[p].clear();
			vacantPoints.addKnownAbsent(p);
			int edgeCount = 0;
			final short[] neighbors = coords.getNeighbors(p);
			for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
				final short n = neighbors[i];
				if (!coords.isOnBoard(n)) {
					edgeCount++;
				}
			}
			points[p].neighborCounts += edgeCount * EDGE_INCREMENT;
		}
		for (final BoardObserver observer : observers) {
			observer.clear();
		}
	}

	public void clearPreservingInitialStones() {
		final ShortSet[] tempInitial = new ShortSet[] { new ShortSet(coords.getFirstPointBeyondBoard()),
				new ShortSet(coords.getFirstPointBeyondBoard()) };
		for (int i = 0; i < 2; i++) {
			tempInitial[i].addAll(initialStones[i]);
		}
		clear();
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < tempInitial[i].size(); j++) {
				placeInitialStone(i == 0 ? BLACK : WHITE, tempInitial[i].get(j));
			}
		}
	}

	/**
	 * 拷贝棋盘.
	 */
	public void copyDataFrom(Board that) {
		colorToPlay = that.colorToPlay;
		hash = that.hash;
		koPoint = that.koPoint;
		for (int i = 0; i < observers.length; i++) {
			observers[i].copyDataFrom(that.observers[i]);
		}
		passes = that.passes;
		for (final short p : coords.getAllPointsOnBoard()) {
			points[p].copyDataFrom(that.points[p]);
		}
		superKoTable.copyDataFrom(that.superKoTable);
		turn = that.turn;
		vacantPoints.copyDataFrom(that.vacantPoints);
	}

	/**
	 * 落子后更新数据结构.
	 *
	 * @param color
	 *            落子的颜色.
	 * @param p
	 *            落子的位置.
	 */
	private void finalizePlay(StoneColor color, short p) {
		final int lastVacantPointCount = vacantPoints.size();
		points[p].color = color;
		vacantPoints.remove(p);
		final boolean surrounded = points[p].hasMaxNeighborsForColor(color.opposite());
		final short[] neighbors = coords.getNeighbors(p);
		for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
			points[neighbors[i]].neighborCounts += Point.NEIGHBOR_INCREMENT[color.index()];
		}
		adjustFriendlyNeighbors(p);
		adjustEnemyNeighbors(p);
		if (lastVacantPointCount == vacantPoints.size() & surrounded) {
			koPoint = vacantPoints.get((short) (vacantPoints.size() - 1));
		} else {
			koPoint = NO_POINT;
		}
	}

	/** 返回此点在链中临近点. */
	public short getChainNextPoint(short p) {
		return points[p].chainNextPoint;
	}

	/** 返回包含点p的链的根. */
	public short getChainRoot(short p) {
		return points[p].chainId;
	}

	/** 返回点p的颜色. */
	public Color getColorAt(short p) {
		return points[p].color;
	}

	/** 返回下一步落子的棋的颜色. */
	public StoneColor getColorToPlay() {
		return colorToPlay;
	}

	/** 返回与棋盘关联的坐标系统CoordinateSystem. */
	public CoordinateSystem getCoordinateSystem() {
		return coords;
	}

	/**
	 * 返回当前棋盘位置的Zobrist哈希, 合并简单劫点和落子颜色。这用在转换表中.
	 */
	public long getFancyHash() {
		long result = hash;
		if (koPoint != NO_POINT) {
			result ^= coords.getHash(colorToPlay, koPoint);
		}
		if (colorToPlay == WHITE) {
			result = ~result;
		}
		// We don't believe we need to take the number of passes into account,
		// because we would never look at or store data in an end-of-game node.
		return result;
	}

	/**
	 * 返回当前棋盘位置的Zobrist哈希,这用在超级劫表中.
	 */
	public long getHash() {
		return hash;
	}

	/**
	 * 返回点p的气.
	 */
	public ShortSet getLiberties(short p) {
		assert coords.isOnBoard(p);
		assert points[p].color != VACANT;
		return points[points[p].chainId].liberties;
	}

	/**
	 * 返回点p的给定棋子颜色邻居数，棋盘外点认为是黑白都有.
	 */
	public int getNeighborsOfColor(short p, Color color) {
		return points[p].getNeighborCount(color);
	}

	/**
	 * 返回结束落子序列后的连续虚手数.
	 */
	public short getPasses() {
		return passes;
	}

	/**
	 * 返回当前落子手数 (1基).
	 */
	public int getTurn() {
		return turn;
	}

	/**
	 * 返回棋盘上空点的集合.
	 */
	public ShortSet getVacantPoints() {
		return vacantPoints;
	}

	/**
	 * 返回如果提子后的棋盘哈希值. 用于play()中检查超级劫.
	 *
	 * @param color
	 *            将要落子的棋子颜色.
	 * @param p
	 *            将要落子的位置.
	 */
	private long hashAfterRemovingCapturedStones(StoneColor color, short p) {
		long result = hash;
		result ^= coords.getHash(color, p);
		final StoneColor enemy = color.opposite();
		for (int i = 0; i < enemyNeighboringChainIds.size(); i++) {
			final short c = enemyNeighboringChainIds.get(i);
			if (points[c].isInAtari()) {
				short active = c;
				do {
					result ^= coords.getHash(enemy, active);
					active = points[active].chainNextPoint;
				} while (active != c);
			}
		}
		return result;
	}

	/**
	 * 如果p点棋子有最大可能邻居数，则返回true.
	 */
	public boolean hasMaxNeighborsForColor(StoneColor color, short p) {
		return points[p].hasMaxNeighborsForColor(color);
	}

	/**
	 * 如果落子p合法（在棋盘上或是虚招），返回true.
	 */
	public boolean isLegal(short p) {
		if (p == PASS) {
			return true;
		}
		return legality(colorToPlay, p) == OK;
	}

	/**
	 * 遍历p的邻居, 查找潜在的杀棋和棋串去合并新的落子. 作为副作用，加载域，friendlyNeighboringChainIds,
	 * enemyNeighboringChainIds, 和 lastPlayLiberties, 被finalizePlay所用.
	 *
	 * @return true如果落子在p将是自杀.
	 */
	private boolean isSuicidal(StoneColor color, short p) {
		friendlyNeighboringChainIds.clear();
		enemyNeighboringChainIds.clear();
		lastPlayLiberties.clear();
		boolean suicide = true;
		final short[] neighbors = coords.getNeighbors(p);
		for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
			final short n = neighbors[i];
			final Color neighborColor = points[n].color;
			if (neighborColor == VACANT) { // Vacant point
				lastPlayLiberties.add(n);
				suicide = false;
			} else if (neighborColor == color) { // Friendly neighbor
				final short chainId = points[n].chainId;
				friendlyNeighboringChainIds.addIfNotPresent(chainId);
				suicide &= points[chainId].isInAtari();
			} else if (neighborColor != OFF_BOARD) { // Enemy neighbor
				final short chainId = points[n].chainId;
				enemyNeighboringChainIds.addIfNotPresent(chainId);
				suicide &= !points[chainId].isInAtari();
			}
		}
		return suicide;
	}

	/**
	 * 返回落子p的合法性. 作为副作用, 加载域、friendlyNeighboringChainIds,
	 * enemyNeighboringChainIds和 lastPlayLiberties, 被finalizePlay所用.
	 */
	private Legality legality(StoneColor color, short p) {
		assert coords.isOnBoard(p);
		if (turn >= coords.getMaxMovesPerGame() - 2) {
			return GAME_TOO_LONG;
		}
		if (points[p].color != VACANT) {
			return OCCUPIED;
		}
		if (p == koPoint) {
			return KO_VIOLATION;
		}
		if (isSuicidal(color, p)) {
			return SUICIDE;
		}
		proposedHash = hashAfterRemovingCapturedStones(color, p);
		if (superKoTable.contains(proposedHash)) {
			return KO_VIOLATION;
		}
		return OK;
	}

	/**
	 * 相似与{@link#legality}, 但不检查已下子或违反超级劫.
	 */
	private Legality legalityFast(StoneColor color, short p) {
		assert coords.isOnBoard(p);
		if (turn >= coords.getMaxMovesPerGame() - 2) {
			return GAME_TOO_LONG;
		}
		if (p == koPoint) {
			return KO_VIOLATION;
		}
		if (isSuicidal(color, p)) {
			return SUICIDE;
		}
		return OK;
	}

	/**
	 * 从基点合并附加点链中. 每个参数都是将合并到链中的棋子.
	 *
	 * @param base
	 *            如果计算不太昂贵, 基点是两链中较大的一个.
	 */
	private void mergeChains(short base, short appendage) {
		points[base].liberties.addAll(points[appendage].liberties);
		int active = appendage;
		do {
			points[active].chainId = points[base].chainId;
			active = points[active].chainNextPoint;
		} while (active != appendage);
		final short temp = points[base].chainNextPoint;
		points[base].chainNextPoint = points[appendage].chainNextPoint;
		points[appendage].chainNextPoint = temp;
	}

	/** 通知观察者关于什么已经改变了. */
	private void notifyObservers(StoneColor color, short p) {
		for (final BoardObserver observer : observers) {
			observer.update(color, p, capturedStones);
		}
	}

	/** 虚手. */
	public void pass() {
		if (koPoint != NO_POINT) {
			koPoint = NO_POINT;
		}
		colorToPlay = colorToPlay.opposite();
		passes++;
		turn++;
		notifyObservers(colorToPlay.opposite(), PASS);
	}

	/** 布棋. */
	public void placeInitialStone(StoneColor color, short p) {
		// Initial stones will always be legal, but the legality method
		// also sets up some fields called by finalizePlay.
		legality(color, p);
		finalizePlay(color, p);
		initialStones[color.index()].add(p);
		hash = proposedHash;
		superKoTable.add(hash);
		// To ensure that the board is in a stable state, this must be done last
		notifyObservers(color, p);
	}

	/**
	 * 落子。如果不合法,则没有副作用. 返回落子的合法性.
	 */
	public Legality play(short p) {
		if (p == PASS) {
			pass();
			return OK;
		}
		final Legality result = legality(colorToPlay, p);
		if (result != OK) {
			return result;
		}
		finalizePlay(colorToPlay, p);
		colorToPlay = colorToPlay.opposite();
		passes = 0;
		turn++;
		hash = proposedHash;
		superKoTable.add(hash);
		//为了确保棋盘处于稳定状态，这调用必须是最后一步，且棋色的参数是被转换回的棋子颜色
		notifyObservers(colorToPlay.opposite(), p);
		return OK;
	}

	/**
	 * 使用可读字符串（例如"c4" or "pass"）的落子方便方法,
	 *
	 * @see #play(short)
	 */
	public Legality play(String move) {
		return play(coords.at(move));
	}

	/**
	 * 相似与落子，但假定p点在棋盘上且没出现过. 不维护哈希值，也不检查超级劫.
	 */
	public Legality playFast(short p) {
		final Legality result = legalityFast(colorToPlay, p);
		if (result != OK) {
			return result;
		}
		finalizePlay(colorToPlay, p);
		colorToPlay = colorToPlay.opposite();
		passes = 0;
		turn++;
		// To ensure that the board is in a stable state, this must be done last
		// The color argument is flipped back to the color of the stone played
		notifyObservers(colorToPlay.opposite(), p);
		return OK;
	}

	/** 提掉在点p的棋子. */
	private void removeStone(short p) {
		points[p].color = VACANT;
		vacantPoints.addKnownAbsent(p);
		neighborsOfCapturedStone.clear();
		final short[] neighbors = coords.getNeighbors(p);
		for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
			final short n = neighbors[i];
			points[n].neighborCounts -= Point.NEIGHBOR_INCREMENT[colorToPlay.opposite().index()];
			if (points[n].color == BLACK | points[n].color == WHITE) {
				neighborsOfCapturedStone.addIfNotPresent(points[n].chainId);
			}
		}
		for (int k = 0; k < neighborsOfCapturedStone.size(); k++) {
			final int c = neighborsOfCapturedStone.get(k);
			points[c].liberties.addKnownAbsent(p);
		}
		capturedStones.add(p);
	}

	/**
	 * 设置将要落子棋的颜色, 用在像GoGui中初始化棋子.
	 */
	public void setColorToPlay(StoneColor stoneColor) {
		colorToPlay = stoneColor;
	}

	/**
	 * 设置连续虚手的数目。不调整观察者. (这用在特殊的超越棋局的落子来决定棋子哪些还活着.)
	 */
	public void setPasses(short passes) {
		this.passes = passes;
	}

	public void setUpHandicap(int handicapSize) {
		clear();
		for (int i = 0; i < handicapSize; i++) {
			if ((handicapSize == 6 || handicapSize == 8) && i == 4) {
				i++;
				handicapSize++;
			}
			placeInitialStone(BLACK, coords.at(HANDICAP_LOCATIONS[i]));
		}
		setColorToPlay(WHITE);
	}

	/**
	 * 根据图示来布棋. 用来初始化棋局，不记录棋盘历史。下一子的颜色如同所指出.
	 *
	 * @throws IllegalArgumentException
	 *             图示有非法字符
	 */
	public void setUpProblem(String[] diagram, StoneColor colorToPlay) {
		assert diagram.length == coords.getWidth();
		clear();
		for (int r = 0; r < coords.getWidth(); r++) {
			assert diagram[r].length() == coords.getWidth();
			for (int c = 0; c < coords.getWidth(); c++) {
				final StoneColor color = StoneColor.forChar(diagram[r].charAt(c));
				if (color != null) {
					placeInitialStone(color, coords.at(r, c));
				} else if (NonStoneColor.forChar(diagram[r].charAt(c)) != VACANT) {
					throw new IllegalArgumentException();
				}
			}
		}
		this.colorToPlay = colorToPlay;
	}

	@Override
	public String toString() {
		String result = "";
		for (int r = 0; r < coords.getWidth(); r++) {
			for (int c = 0; c < coords.getWidth(); c++) {
				result += points[coords.at(r, c)].color.toChar();
			}
			result += "\n";
		}
		return result;
	}

	public void removeStones(ShortSet ourDead) {
		for (int i = 0; i < ourDead.size(); i++) {
			points[ourDead.get(i)].color = VACANT;
			vacantPoints.addKnownAbsent(ourDead.get(i));
		}
	}
}
