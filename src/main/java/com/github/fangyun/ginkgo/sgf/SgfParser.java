package com.github.fangyun.ginkgo.sgf;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import static java.lang.Integer.MAX_VALUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/** 解析SGF文件. */
public final class SgfParser {
	public static void main(String[] args) {
		final SgfParser parser = new SgfParser(CoordinateSystem.forWidth(19), true);
		final List<List<Short>> games = parser
				.parseGamesFromFile(new File("src/main/resources/sgf-test-files/19/print1.sgf"), MAX_VALUE);
		for (final List<Short> game : games) {
			for (final Short move : game) {
				System.out.println(parser.coords.toString(move));
			}
		}
	}

	private final CoordinateSystem coords;

	private boolean breakOnFirstPass;

	public SgfParser(CoordinateSystem coords, boolean breakOnFirstPass) {
		this.coords = coords;
		this.breakOnFirstPass = breakOnFirstPass;
	}

	/**
	 * 从sgf文件构建棋盘. 使用placeInitialStone()处理AB和AW命令,使用play()处理B和W命令.
	 * 
	 * @param filepath sgf文件名.
	 * @param board    sgf文件覆盖的棋盘.
	 */
	public void sgfToBoard(String filepath, Board board) {
		if (!filepath.toLowerCase().endsWith(".sgf")) {
			System.err.println(filepath + " 不是sgf文件!");
		} else {
			File file = new File(filepath);
			sgfToBoard(file, board);
		}
	}

	/**
	 * 从sgf文件构建棋盘. 使用placeInitialStone()处理AB和AW命令,使用play()处理B和W命令.
	 * 
	 * @param file  sgf文件.
	 * @param board sgf文件覆盖的棋盘.
	 */
	public void sgfToBoard(File file, Board board) {
		board.clear();
		String input = "";
		try (Scanner s = new Scanner(file)) {
			while (s.hasNextLine()) {
				input += s.nextLine();
			}
			input = input.replace("W[]", "W[tt]");
			input = input.replace("B[]", "B[tt]");
			StringTokenizer stoken = new StringTokenizer(input, ")[];");
			int addStoneState = 0;
			// Ignores AE
			while (stoken.hasMoreTokens()) {
				String token = stoken.nextToken();
				if (token.equals("AW")) {
					addStoneState = 1;
					token = stoken.nextToken();
				} else if (token.equals("AB")) {
					addStoneState = 2;
					token = stoken.nextToken();
				} else if (token.equals("W") || token.equals("B")) {
					if (token.equals("W")) {
						board.setColorToPlay(WHITE);
					} else {
						board.setColorToPlay(BLACK);
					}
					addStoneState = 0;
					token = stoken.nextToken();
					if (token.equals("tt")) {
						board.play(PASS);
					} else {
						board.play(sgfToPoint(token));
					}
				}
				if (token.charAt(0) >= 'a') {
					if (addStoneState == 1) {
						board.placeInitialStone(WHITE, sgfToPoint(token));
					} else if (addStoneState == 2) {
						board.placeInitialStone(BLACK, sgfToPoint(token));
					}
				} else {
					addStoneState = 0;
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("文件没找到!");
			e.printStackTrace();
		}
	}

	/**
	 * 从StringTokenizer中解析落子.
	 * 
	 * @param maxBookDepth 查看多少落子，没有限制则使用Integer.MAX_VALUE;
	 * @return 落子的列表 (以shorts).
	 */
	private List<Short> parseGame(StringTokenizer stoken, int maxBookDepth) {
		final List<Short> game = new ArrayList<>();
		int turn = 0;
		while (turn <= maxBookDepth) {
			if (!stoken.hasMoreTokens()) {
				return game;
			}
			String token = stoken.nextToken();
			if (token.equals("HA")) {
				// 让子棋，丢弃.
				return null;
			} else if (token.equals("SZ")) {
				token = stoken.nextToken();
				final int intToken = Integer.parseInt(token);
				if (intToken != 19) {
					// 棋局不是正确尺寸，丢弃.
					return null;
				}
			} else if (token.equals("AB") || token.equals("AW")) {
				// 棋局已经落子，丢弃.
				return null;
			} else if (token.equals("PL")) {
				// 棋局换颜色落子，丢弃.
				return null;
			} else if (token.equals("B") || token.equals("W")) {
				token = stoken.nextToken();
				final short move = sgfToPoint(token);
				if (maxBookDepth != MAX_VALUE && move == PASS) {
					// 从棋谱中读到怪异的虚着，弃局
					return null;
				}
				if (breakOnFirstPass && move == PASS) {
					return game;
				}
				game.add(move);
				turn++;
			} else if (token.equals(")")) {
				return game;
			}
		}
		return game;
	}

	/**
	 * 解析棋局并返回它.读入棋局来响应GTP命令.
	 */
	public List<Short> parseGameFromFile(File file) {
		final List<List<Short>> games = parseGamesFromFile(file, MAX_VALUE);
		return games.get(0);
	}

	/**
	 * 从文件中读入所有棋局，并以列表返回.列表中每个元素是落子的列表(shorts).
	 * 
	 * @param maxBookDepth 查看每局棋的最大步数，或者Integer.MAX_VALUE表示无限制.
	 */
	public List<List<Short>> parseGamesFromFile(File file, int maxBookDepth) {
		final List<List<Short>> games = new ArrayList<>();
		String input = "";
		try (Scanner s = new Scanner(file)) {
			while (s.hasNextLine()) {
				input += s.nextLine();
			}
			input = input.replace("W[]", "W[tt]");
			input = input.replace("B[]", "B[tt]");
			final StringTokenizer stoken = new StringTokenizer(input, ")[];");
			while (stoken.hasMoreTokens()) {
				final String token = stoken.nextToken();
				if (token.equals("(")) {
					final List<Short> game = parseGame(stoken, maxBookDepth);
					if (game != null) {
						games.add(game);
					}
				}
			}
			return games;
		} catch (final FileNotFoundException e) {
			System.err.println("文件没找到!");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	/** 返回sgf字符串的点形式. */
	public short sgfToPoint(String label) {
		if (label.equals("tt")) {
			return PASS;
		}
		final int c = label.charAt(0) - 'a';
		final int r = label.charAt(1) - 'a';
		short result = coords.at(r, c);
		assert coords.isOnBoard(result);
		return result;
	}

}
