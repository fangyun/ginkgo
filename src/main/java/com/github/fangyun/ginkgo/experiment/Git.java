package com.github.fangyun.ginkgo.experiment;

import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;
import java.io.IOException;
import java.util.Scanner;

/** 包含静态方法来检查git仓库的状态. */
public final class Git {

	/**
	 * 返回当前git提交字符串, 或者如果得不到清晰的git状态，则返回空串.如果运行在Windows机器上，则可能不能查看当前git提交字符串.
	 */
	public static String getGitCommit() {
		try {
			try (Scanner s = new Scanner(new ProcessBuilder("git", "--git-dir=" + GINKGO_ROOT + ".git",
					"--work-tree=" + GINKGO_ROOT, "status", "-s").start().getInputStream())) {
				if (s.hasNextLine()) {
					return "";
				}
			}
			try (Scanner s = new Scanner(new ProcessBuilder("git", "--git-dir=" + GINKGO_ROOT + ".git",
					"--work-tree=" + GINKGO_ROOT, "log", "--pretty=format:'%H'", "-n", "1").start().getInputStream())) {
				if (s.hasNextLine()) {
					final String commit = s.nextLine();
					// substring移除单引号
					return commit.substring(1, commit.length() - 1);
				}
			}
		} catch (final IOException e) {
			return "未知 (git未安装)";
		}
		return "";
	}
}
