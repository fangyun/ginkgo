package com.github.fangyun.ginkgo.experiment;

import static com.github.fangyun.ginkgo.experiment.Logging.*;

import java.io.InputStream;
import java.util.Scanner;

/**
 * 侦听在另一个进程中运行的GTP棋手。每当该棋手发出一行输出时，该行就被传递给一个棋局实例.
 */
final class PlayerListener implements Runnable {

	/** 来自另一个程序的输入. */
	private final InputStream fromProgram;

	/** 正在对弈的棋局. */
	private final Game game;

	public PlayerListener(InputStream input, Game game) {
		this.fromProgram = input;
		this.game = game;
	}

	@Override
	public void run() {
		try (Scanner s = new Scanner(fromProgram)) {
			boolean finishedNormally = false;
			while (s.hasNextLine()) {
				//当出现多行错误消息时，s会传递进来，因此游戏可以将所有消息转储到输出文件。
				final String line = s.nextLine();
				System.err.println(game.hashCode() + " PlayerListener got line " + line);
				if (!line.isEmpty()) {
					finishedNormally = game.handleResponse(line, s);
				}
			}
			System.err.println(game.hashCode() + " PlayerListener did not get a line, finishedNormally = " + finishedNormally);
			if (!finishedNormally) {
				log("Program crashed");
				game.handleResponse("? program crashed", s);
			}
		}
	}

}
