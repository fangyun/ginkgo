package com.github.fangyun.ginkgo.experiment;

import static com.github.fangyun.ginkgo.core.Legality.OK;
import static com.github.fangyun.ginkgo.core.NonStoneColor.OFF_BOARD;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import static com.github.fangyun.ginkgo.experiment.Game.State.QUITTING;
import static com.github.fangyun.ginkgo.experiment.Game.State.REQUESTING_MOVE;
import static com.github.fangyun.ginkgo.experiment.Game.State.SENDING_MOVE;
import static com.github.fangyun.ginkgo.experiment.Game.State.SENDING_TIME_LEFT;
import static com.github.fangyun.ginkgo.sgf.SgfWriter.toSgf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Scanner;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.Legality;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.score.ChineseFinalScorer;
import com.github.fangyun.ginkgo.score.FinalScorer;

/** 允许两个独立的GTP程序对弈. */
final class Game {

	static enum State {
		QUITTING, REQUESTING_MOVE, SENDING_MOVE, SENDING_TIME_LEFT
	}

	public static void main(String[] args) {
		final String black = "java -ea -server -Xmx3072M -cp target/classes com.github.fangyun.ginkgo.ui.Ginkgo";
		final String white = black;
		final Rules rules = new Rules(9, 7.5, 600);
		new Game("target/test.sgf", rules, black, white).play();
	}

	/** 棋盘. */
	private final Board board;

	/** 结果文件名. */
	private final String filename;

	/** 打印到指定的文件名的文件中. */
	private PrintWriter out;

	/** 启动两个棋手的命令行. */
	private final String[] players;

	/** 运行对弈程序的进程. */
	private final Process[] programs;

	/** 棋局规则. */
	private final Rules rules;

	/** 计分棋局. */
	private final FinalScorer scorer;

	/** 棋局开始的系统时间（单位毫秒）. */
	private long startTime;

	/** 程序状态. @see #handleResponse */
	private State state;

	/**
	 * 棋手被要求着子的系统时间（毫秒）. 用来计算每位棋手的下棋用时.
	 */
	private long timeLastMoveWasRequested;

	/** 每位棋手的总计用时（毫秒）. */
	private final long[] timeUsed;

	/** 输出两个程序进程. */
	private final PrintWriter[] toPrograms;

	/** 胜方棋的颜色(BLACK或WHITE). */
	private Color winner;

	/**
	 * @param outputFilename
	 *            SGF的输出文件名.
	 * @param rules
	 *            棋局规则.
	 * @param black
	 *            启动黑棋方的命令.
	 * @param white
	 *            启动白棋方的命令.
	 */
	Game(String outputFilename, Rules rules, String black, String white) {
		this.filename = outputFilename;
		this.rules = rules;
		timeUsed = new long[2];
		programs = new Process[2];
		toPrograms = new PrintWriter[2];
		players = new String[] { black, white };
		board = new Board(rules.boardWidth);
		scorer = new ChineseFinalScorer(board, rules.komi);
		try {
			out = new PrintWriter(filename);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			out.flush();
			out.close();
			System.exit(1);
		}
		out.println("(;FF[4]CA[UTF-8]AP[Ginkgo1]KM[" + rules.komi + "]GM[1]RU[Chinese]SZ[" + rules.boardWidth + "]");
		out.println("PB[" + players[BLACK.index()] + "]");
		out.println("PW[" + players[WHITE.index()] + "]");
		out.flush();
	}

	/**
	 * 出现阻碍棋局继续的意外，告诉棋手关闭，之后失败退出.
	 */
	private void die(String line, Scanner s, String message) {
		System.err.println(hashCode() + "棋局将亡, 行号 " + line + ", 错误信息 " + message);
		endPrograms();
		out.println("输出文件 " + filename + ":");
		out.println(board);
		out.println(message);
		out.println(line);
		while (s.hasNextLine()) {
			out.println(s.nextLine());
		}
		out.flush();
		System.exit(1);
	}

	/**
	 * 给彼此棋手发出quit命令，因为进程将结束. 同时记录开始时间、结束时间到输出的SGF文件中.
	 */
	private void endPrograms() {
		out.println(";C[开始时间:" + new Date(startTime) + "]");
		out.println(";C[结束时间:" + new Date() + "]");
		out.println(")");
		out.flush();
		state = QUITTING;
		for (final StoneColor color : StoneColor.values()) {
			toPrograms[color.index()].println("quit");
			toPrograms[color.index()].flush();
		}
	}

	/**
	 * 当前下棋的棋子颜色.
	 */
	private StoneColor getColorToPlay() {
		return board.getColorToPlay();
	}

	/**
	 * 处理给定棋子颜色的棋手的响应行.
	 *
	 * 方法开始设置状态为正在处理的行为.例如，如果状态是REQUESTING_MOVE, 我们正在处理着子响应我们的请求.
	 *
	 * @param line
	 *            被处理的响应行.
	 * @param s
	 *            通过Scanner响应被到达. 这对于接受来自棋手的多行错误信息是有用的.
	 * @return true，如果响应一个quit命令.
	 */
	boolean handleResponse(String line, Scanner s) {
		System.err.println(hashCode() + " 棋局收到行 " + line);
		if (line.startsWith("=")) {
			if (state == REQUESTING_MOVE) {
				final String move = line.substring(line.indexOf(' ') + 1);
				System.err.println(hashCode() + " 棋局收到着子 " + move);
				if (!writeMoveToSgf(move)) {
					return false;
				}
				final Legality legality = board.play(move);
				if (legality != OK) {
					die(line, s, "非法着子: " + legality);
				}
				if (board.getPasses() == 2) {
					winner = scorer.winner();
					System.err.println(hashCode() + " 棋局的胜方为 " + winner);
					out.println(";RE[" + (winner == BLACK ? "B" : "W") + "+" + Math.abs(scorer.score()) + "]");
					out.println(";C[moves:" + board.getTurn() + "]");
					out.flush();
					endPrograms();
					return false;
				}
				sendToOtherPlayer(move);
				return false;
			} else if (state == SENDING_MOVE) {
				state = SENDING_TIME_LEFT;
				sendTime();
				return false;
			} else if (state == SENDING_TIME_LEFT) {
				state = REQUESTING_MOVE;
				sendMoveRequest();
				return false;
			} else { // Mode is QUITTING
				return true;
			}
		}
		die(line, s, "程序错误");
		return false;
	}

	/**
	 * 对弈并写SGF文件.
	 *
	 * @return 棋局的胜方.
	 */
	Color play() {
		winner = OFF_BOARD;
		board.clear();
		try {
			startPlayers();
			startTime = System.currentTimeMillis();
			if (rules.time > 0) {
				state = SENDING_TIME_LEFT;
				sendTime();
			} else {
				state = REQUESTING_MOVE;
				sendMoveRequest();
			}
			for (final StoneColor color : StoneColor.values()) {
				programs[color.index()].waitFor();
			}
			out.close();
		} catch (final InterruptedException e) { // Should never happen
			e.printStackTrace();
			System.exit(1);
		}
		if (winner == OFF_BOARD) {
			System.err.println("赢方离开了棋盘.\n" + board.toString()); // TODO
		}
		return winner;
	}

	/** 发送着子请求给棋手. */
	private void sendMoveRequest() {
		final StoneColor c = getColorToPlay();
		System.err.println(hashCode() + " 正在发送着子请求给棋色 " + c);
		toPrograms[c.index()].println("genmove " + c);
		toPrograms[c.index()].flush();
		timeLastMoveWasRequested = System.currentTimeMillis();
	}

	/** 发送剩余时间消息给棋手. */
	private void sendTime() {
		final StoneColor c = getColorToPlay();
		final int timeLeftForThisPlayer = rules.time - (int) (timeUsed[c.index()] / 1000);
		System.err.println(hashCode() + " 正在发送剩余时间(" + timeLeftForThisPlayer + " 秒)给 " + c);
		toPrograms[c.index()].println("time_left " + c + " " + timeLeftForThisPlayer + " 0");
		toPrograms[c.index()].flush();
	}

	/**
	 * 发送着子给其它棋手. 根据棋局的时间是否保持, 状态设置为SENDING_MOVE
	 * (因为会发送着子和剩余时间消息)或者SENDING_TIME_LEFT (因为只发送着子，并把剩余时间消息作为应答).
	 */
	private void sendToOtherPlayer(final String move) {
		System.err.println(hashCode() + " 正发送着子给其它棋手 (" + getColorToPlay() + ")");
		if (rules.time > 0) {
			state = SENDING_MOVE;
		} else {
			state = SENDING_TIME_LEFT;
		}
		// 注意棋子颜色：因为棋子颜色已经切换
		toPrograms[getColorToPlay().index()].println((getColorToPlay().opposite() + " " + move).toLowerCase());
		toPrograms[getColorToPlay().index()].flush();
	}

	/** 启动一个棋手运行(在不同进程). */
	private void startPlayers() {
		try {
			for (final StoneColor color : StoneColor.values()) {
				final int c = color.index();
				final ProcessBuilder builder = new ProcessBuilder("nohup", "bash", "-c", players[c], "&");
				builder.redirectErrorStream(true);
				programs[c] = builder.start();
				toPrograms[c] = new PrintWriter(programs[c].getOutputStream());
				new Thread(new PlayerListener(programs[c].getInputStream(), this)).start();
			}
		} catch (final IOException e) {
			System.err.println("启动下面两个进程之一失败:");
			System.err.println(players[BLACK.index()]);
			System.err.println(players[WHITE.index()]);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * 写着子到输出文件. 如果棋局因为放弃或运行超时而结束, 同样设置胜方, 结束程序和完成SGF文件.
	 *
	 * @return true 如果棋局在着子之后依然继续.
	 */
	private boolean writeMoveToSgf(final String coordinates) {
		String timeLeftIndicator = "";
		int timeLeftForThisPlayer = 0;
		if (rules.time > 0) {
			timeUsed[getColorToPlay().index()] += System.currentTimeMillis() - timeLastMoveWasRequested;
			timeLeftForThisPlayer = rules.time - (int) (timeUsed[getColorToPlay().index()] / 1000);
			timeLeftIndicator = (getColorToPlay() == BLACK ? "BL" : "WL") + "[" + timeLeftForThisPlayer + "]";
		}
		if (!coordinates.toLowerCase().equals("resign")) {
			final CoordinateSystem coords = board.getCoordinateSystem();
			out.println((getColorToPlay() == BLACK ? ";B" : ";W") + "[" + toSgf(coords.at(coordinates), coords) + "]"
					+ timeLeftIndicator);
			out.flush();
		}
		if (coordinates.toLowerCase().equals("resign")
		// || rules.time > 0
		// && timeLeftForThisPlayer <= 0
		) {
			winner = getColorToPlay().opposite();
			out.print(";RE[" + (winner == BLACK ? "B" : "W") + "+");
			// out.print(rules.time > 0 && timeLeftForThisPlayer <= 0 ? "Time"
			// : "Resign");
			out.print("放弃");
			out.println("]");
			out.println(";C[moves:" + board.getTurn() + "]");
			endPrograms();
			out.flush();
			return false;
		}
		return true;
	}
}
