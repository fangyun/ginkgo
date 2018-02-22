package com.github.fangyun.ginkgo.ui;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.RESIGN;
import static com.github.fangyun.ginkgo.core.StoneColor.BLACK;
import static com.github.fangyun.ginkgo.core.StoneColor.WHITE;
import static com.github.fangyun.ginkgo.experiment.Git.getGitCommit;
import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;
import static java.io.File.separator;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Float.parseFloat;

import static com.github.fangyun.ginkgo.experiment.Logging.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.core.Legality;
import com.github.fangyun.ginkgo.core.StoneColor;
import com.github.fangyun.ginkgo.experiment.Logging;
import com.github.fangyun.ginkgo.mcts.Player;
import com.github.fangyun.ginkgo.mcts.PlayerBuilder;
import com.github.fangyun.ginkgo.sgf.SgfParser;
import com.github.fangyun.ginkgo.util.ShortSet;

/**
 * 运行GTP协议的前端主类。响应诸如"showboard"、"genmove black"命令.
 * <p>
 * 命令行参数采用 <code>feature=value</code>形式. 布尔参数可以简单使用 <code>feature</code>形式.
 * <dl>
 * <dt>biasdelay</dt>
 * <dd>启发与偏见.缺省800.</dd>
 * <dt>boardsize</dt>
 * <dd>棋盘宽度，缺省19.</dd>
 * <dt>book</dt>
 * <dd>是否Ginkgo从布局棋谱开始对弈。缺省true。</dd>
 * <dt>grace</dt>
 * <dd>是否采用优雅模式。当对手虚招时，Ginkgo试图清理棋盘上的对手的死棋，或者如果在当前棋盘局面上能赢的话，则同样虚招。缺省false.</dd>
 * <dt>gestation</dt>
 * <dd>下一棋子在子创建为这个棋子，需要运行的计数。缺省4.</dd>
 * <dt>komi</dt>
 * <dd>贴目数. 缺省7.5.</dd>
 * <dt>lgrf2</dt>
 * <dd>切换最近好的没有忘记的响应(级别2). 下棋中,
 * Ginkgo跟踪一个步棋的成功回应，或者是两步回应棋链，用以在以后的下棋中。缺省true.</dd>
 * <dt>log-file</dt>
 * <dd>切换日志记录，这会触发记录日志到特定的目录中。如果没有设置，将不会记录日志。.</dd>
 * <dt>memory</dt>
 * <dd>Ginkgo使用的内存的兆字节数，转换表以此而扩展。匹配JVM使用命令行分配的内存，例如-Xmx1024M. 缺省1024.
 * <dt>msec</dt>
 * <dd>Ginkgo决定一步棋的毫秒数。当使用时间管理是，则没有关系。缺省1000毫秒.</dd>
 * <dt>pondering</dt>
 * <dd>切换是否Ginkgo在对手下棋时依然思考。缺省false.</dd>
 * <dt>rave</dt>
 * <dd>切换快速行为价值评估。缺省true.</dd>
 * <dt>shape</dt>
 * <dd>切换对5x5模式使用形状建议.</dd>
 * <dt>threads</dt>
 * <dd>Ginkgo用来思考的线程数.缺省2.</dd>
 * <dt>time-management</dt>
 * <dd>设置Ginkgo的时间管理的类型。如果没有设置，Ginkgo将依赖msec.类型选项有：uniform (缺省)和exiting.</dd>
 * </dl>
 */
public final class Ginkgo {

	private static final String[] DEFAULT_GTP_COMMANDS = { "black", "boardsize", "clear_board", "final_score",
			"final_status_list", "fixed_handicap", "genmove", "genmove_black", "genmove_white",
			"gogui-analyze_commands", "gogui-get-wins", "gogui-search-values", "known_command", "kgs-game_over",
			"kgs-genmove_cleanup", "komi", "list_commands", "loadsgf", "name", "play", "playout_count",
			"protocol_version", "quit", "reg_genmove", "showboard", "time_left", "time_settings", "undo", "version",
			"white", };

	public static void main(String[] args) throws IOException {
		new Ginkgo(args).run();
	}

	/** True如果在电脑上运行测试收集程序 */
	private boolean cgtc;

	/** 当前命令的GTP的ID数 */
	private int commandId;

	/** 命令行参数. */
	private String commandLineArgs;

	/** 已知的GTP命令. */
	private final List<String> commands;

	/**
	 * 输入流.
	 */
	private final BufferedReader in;

	/** 输出流. */
	private final PrintStream out;

	/** 下棋的棋手. */
	private Player player;

	/** 棋手构建器. */
	private PlayerBuilder playerBuilder;

	/**
	 * @param inStream
	 *            驱动程序的输入流(通常为System.in)
	 * @param outStream
	 *            打印响应到输出流(通常为System.out)
	 */
	private Ginkgo(InputStream inStream, OutputStream outStream, String[] args) {
		in = new BufferedReader(new InputStreamReader(inStream));
		if (outStream instanceof PrintStream) {
			out = (PrintStream) outStream;
		} else {
			out = new PrintStream(outStream);
		}
		handleCommandLineArguments(args);
		commandLineArgs = "";
		for (final String arg : args) {
			commandLineArgs += arg + " ";
		}
		commands = new ArrayList<>();
		for (final String s : DEFAULT_GTP_COMMANDS) {
			commands.add(s);
		}
	}

	private Ginkgo(String[] args) {
		this(System.in, System.out, args);
	}

	/** 应答被处理的最新命令. */
	private void acknowledge() {
		acknowledge("");
	}

	/**
	 * 使用指定消息应答被处理的最新命令.
	 */
	private void acknowledge(String message) {
		String response;
		if (commandId >= 0) {
			response = "=" + commandId + " " + message;
		} else {
			response = "= " + message;
		}
		log("发送: " + response);
		out.println(response + "\n");
	}

	/** 指出最新的命令不能被处理. */
	private void error(String message) {
		String response;
		if (commandId >= 0) {
			response = "?" + commandId + " " + message;
		} else {
			response = "? " + message;
		}
		log("发送: " + response);
		out.println(response + "\n");
	}

	/**
	 * 处理一条GTP命令.
	 * 
	 * @return true 其它都返回,除了"quit"命令.
	 */
	private boolean handleCommand(String command) {
		log("收到: " + command);
		// Remove any comment
		final int commentStart = command.indexOf("#");
		if (commentStart >= 0) {
			command = command.substring(0, command.indexOf('#'));
		}
		// 解析字符串为可选的id数字, 命令，和参数
		final StringTokenizer arguments = new StringTokenizer(command);
		final String token1 = arguments.nextToken();
		try {
			commandId = Integer.parseInt(token1);
			command = arguments.nextToken().toLowerCase();
		} catch (final NumberFormatException exception) {
			commandId = -1;
			command = token1.toLowerCase();
		}
		// 调用更冗长的handleCommand方法
		return handleCommand(command, arguments);
	}

	/**
	 * handleCommand(String)的帮助方法.
	 * 
	 * @return true 除了"quit"命令，其它都返回.
	 * @param arguments
	 *            包含给命令的参数. 例如, 落子颜色
	 */
	private boolean handleCommand(String command, StringTokenizer arguments) {
		final CoordinateSystem coords = player.getBoard().getCoordinateSystem();
		if (command.equals("black") || command.equals("b") || command.equals("white") || command.equals("w")) {
			final short point = coords.at(arguments.nextToken());
			player.setColorToPlay(command.charAt(0) == 'b' ? BLACK : WHITE);
			if (player.acceptMove(point) == Legality.OK) {
				acknowledge();
			} else {
				error("非法落子");
			}
		} else if (command.equals("boardsize")) {
			final int width = Integer.parseInt(arguments.nextToken());
			if (width == coords.getWidth()) {
				player.clear();
				acknowledge();
			} else if (width >= 2 && width <= 19) {
				player = null; // 因此旧的转换表能再利用
				playerBuilder = playerBuilder.boardWidth(width);
				player = playerBuilder.build();
				acknowledge();
			} else {
				error("不可接受的棋盘大小");
			}
		} else if (command.equals("clear_board")) {
			player.clear();
			acknowledge();
		} else if (command.equals("final_score")) {
			final double score = player.finalScore();
			if (score > 0) {
				acknowledge("B+" + score);
			} else if (score < 0) {
				acknowledge("W+" + -score);
			} else {
				acknowledge("0");
			}
		} else if (command.equals("final_status_list")) {
			String status = arguments.nextToken();
			if (status.equals("dead")) {
				ShortSet deadStones = player.findDeadStones(0.75, WHITE);
				deadStones.addAll(player.findDeadStones(0.75, BLACK));
				acknowledge(produceVerticesString(deadStones));
			} else if (status.equals("alive")) {
				acknowledge(produceVerticesString(player.getLiveStones(0.75)));
			}
		} else if (command.equals("fixed_handicap")) {
			final int handicapSize = parseInt(arguments.nextToken());
			if (handicapSize >= 2 && handicapSize <= 9) {
				player.setUpHandicap(handicapSize);
				acknowledge();
			} else {
				error("无效的让子大小");
			}
		} else if (command.equals("genmove") || command.equals("genmove_black") || command.equals("genmove_white")
				|| command.equals("kgs-genmove_cleanup") || command.equals("reg_genmove")) {
			StoneColor color;
			if (command.equals("genmove") || command.equals("kgs-genmove_cleanup") || command.equals("reg_genmove")) {
				color = arguments.nextToken().toLowerCase().charAt(0) == 'b' ? BLACK : WHITE;
			} else {
				color = command.equals("genmove_black") ? BLACK : WHITE;
			}
			if (!cgtc) {
				assert color == player.getBoard().getColorToPlay();
			} else {
				player.getBoard().setColorToPlay(color);
			}
			if (command.equals("kgs-genmove_cleanup")) {
				player.setCleanupMode(true);
			}
			final short point = player.bestMove();
			if (point == RESIGN) {
				acknowledge("resign");
				player.clear(); // to stop threaded players
			} else {
				if (!command.equals("reg_genmove")) {
					player.acceptMove(point);
				}
				acknowledge(coords.toString(point));
			}
		} else if (command.equals("gogui-analyze_commands")) {
			acknowledge(
					"gfx/Perform bias/gogui-perform-bias\ngfx/Search values/gogui-search-values\ngfx/Get wins/gogui-get-wins\ngfx/Get runs/gogui-get-runs\ngfx/Get winrate/gogui-get-winrate\ngfx/Perform 1000 mcruns/perform-mcruns\n");
		} else if (command.equals("gogui-get-runs")) {
			acknowledge(player.goguiGetRuns());
		} else if (command.equals("gogui-get-winrate")) {
			acknowledge(player.goguiGetWinrate());
		} else if (command.equals("gogui-get-wins")) {
			acknowledge(player.goguiGetWins());
		} else if (command.equals("gogui-perform-bias")) {
			player.getMcRunnable(0).copyDataFrom(player.getBoard());
			player.getRoot().updateBias(player.getMcRunnable(0));
			acknowledge();
		} else if (command.equals("gogui-search-values")) {
			acknowledge(player.goguiSearchValues());
		} else if (command.equals("kgs-game_over")) {
			try (Scanner scanner = new Scanner(new File(GINKGO_ROOT + separator + "config" + separator + "quit.txt"))) {
				acknowledge();
				player.endGame(); // to stop threaded players
				if (scanner.nextLine().equals("true")) {
					return false;
				}
			} catch (final FileNotFoundException e) {
				// The file was not found, so we continue to play.
			}
		} else if (command.equals("known_command")) {
			acknowledge(commands.contains(arguments.nextToken()) ? "1" : "0");
		} else if (command.equals("komi")) {
			final double komi = parseDouble(arguments.nextToken());
			log("收到贴目命令");
			if (komi == player.getFinalScorer().getKomi()) {
				log("清理棋手");
				player.clear();
				log("完成清理棋手");
			} else {
				log("再建棋手");
				player = null;
				playerBuilder = playerBuilder.komi(komi);
				player = playerBuilder.build();
				log("完成再建棋手");
			}
			acknowledge();
		} else if (command.equals("list_commands")) {
			String response = "";
			for (final String s : commands) {
				response += s + "\n";
			}
			// 消除最后空白行
			response = response.substring(0, response.length() - 1);
			acknowledge(response);
		} else if (command.equals("loadsgf")) {
			final SgfParser parser = new SgfParser(player.getBoard().getCoordinateSystem(), false);
			if (cgtc) {
				player.clear();
				parser.sgfToBoard(arguments.nextToken(), player.getBoard());
			} else {
				player.setUpSgfGame(parser.parseGameFromFile(new File(arguments.nextToken())));
			}
			acknowledge();
		} else if (command.equals("name")) {
			acknowledge("Ginkgo");
		} else if (command.equals("showboard")) {
			String s = player.getBoard().toString();
			s = "\n" + s.substring(0, s.length() - 1);
			acknowledge(s);
		} else if (command.equals("perform-mcruns")) {
			for (int i = 0; i < 1000; i++) {
				player.getMcRunnable(0).performMcRun(true);
			}
			acknowledge();
		} else if (command.equals("play")) {
			// ggo和Goban发送"black f4"而不是"play black f4". 接受这样的命令.
			// 小写命令，因为GTP定义颜色为大小写不敏感.
			handleCommand(arguments.nextToken().toLowerCase(), arguments);
		} else if (command.equals("playout_count")) {
			acknowledge("落子计数: " + player.getPlayoutCount());
		} else if (command.equals("protocol_version")) {
			acknowledge("2");
		} else if (command.equals("quit")) {
			acknowledge();
			player.endGame(); // to stop threaded players
			return false;
		} else if (command.equals("time_left")) {
			arguments.nextToken(); // 扔掉颜色参数
			final int secondsLeft = parseInt(arguments.nextToken());
			player.setRemainingTime(secondsLeft);
			acknowledge();
		} else if (command.equals("time_settings")) {
			final int secondsLeft = parseInt(arguments.nextToken());
			player.setRemainingTime(secondsLeft);
			acknowledge();
		} else if (command.equals("undo")) {
			if (player.undo()) {
				acknowledge();
			} else {
				error("不能悔棋");
			}
		} else if (command.equals("version")) {
			String git;
			git = getGitCommit();
			if (git.isEmpty()) {
				git = "未知";
			}
			final String version = "Ginkgo Git提交: " + git + " 参数: " + commandLineArgs;
			acknowledge(version);
		} else {
			error("未知命令: " + command);
		}
		return true;
	}

	// TODO This needs documentation
	private String produceVerticesString(ShortSet deadStones) {
		String vertices = "";
		for (int i = 0; i < deadStones.size(); i++) {
			vertices += player.getBoard().getCoordinateSystem().toString(deadStones.get(i)) + " ";
		}
		return vertices;
	}

	/** 用命令行参数更新playerBuilder. */
	private void handleCommandLineArguments(String[] args) {
		playerBuilder = new PlayerBuilder();
		for (final String argument : args) {
			final int j = argument.indexOf('=');
			String left, right;
			if (j > 0) {
				left = argument.substring(0, j);
				right = argument.substring(j + 1);
			} else {
				left = argument;
				right = "true";
			}
			// 处理属性
			if (left.equals("biasdelay")) {
				playerBuilder.biasDelay(parseInt(right));
			} else if (left.equals("boardsize")) {
				playerBuilder.boardWidth(parseInt(right));
			} else if (left.equals("book")) {
				playerBuilder.openingBook(parseBoolean(right));
			} else if (left.equals("cgtc")) {
				cgtc = parseBoolean(right);
			} else if (left.equals("grace")) {
				playerBuilder.coupDeGrace(parseBoolean(right));
			} else if (left.equals("gestation")) {
				playerBuilder.gestation(parseInt(right));
			} else if (left.equals("komi")) {
				playerBuilder.komi(parseDouble(right));
			} else if (left.equals("lgrf2")) {
				playerBuilder.lgrf2(parseBoolean(right));
			} else if (left.equals("liveshape")) {
				playerBuilder.liveShape(parseBoolean(right));
			} else if (left.equals("log-file")) {
				Logging.setFilePath(right);
			} else if (left.equals("memory")) {
				playerBuilder.memorySize(parseInt(right));
			} else if (left.equals("msec")) {
				playerBuilder.msecPerMove(parseInt(right));
			} else if (left.equals("ponder")) {
				playerBuilder.ponder(parseBoolean(right));
			} else if (left.equals("rave")) {
				playerBuilder.rave(parseBoolean(right));
			} else if (left.equals("shape")) {
				playerBuilder.shape(parseBoolean(right));
			} else if (left.equals("shape-bias")) {
				playerBuilder.shapeBias(parseInt(right));
			} else if (left.equals("shape-minstones")) {
				playerBuilder.shapeMinStones(parseInt(right));
			} else if (left.equals("shape-scaling-factor")) {
				playerBuilder.shapeScalingFactor(parseFloat(right));
			} else if (left.equals("threads")) {
				playerBuilder.threads(parseInt(right));
			} else if (left.equals("time-management")) {
				playerBuilder.timeManagement(right);
			} else {
				throw new IllegalArgumentException("未知的命令行参数: " + left);
			}
		}
		player = playerBuilder.build();
	}

	/** 接受并处理GTP命令直到收到quit命令. */
	private void run() throws IOException {
		String input;
		do {
			input = "";
			while (input.equals("")) {
				input = in.readLine();
				if (input == null) {
					return;
				}
			}
		} while (handleCommand(input));
	}

}
