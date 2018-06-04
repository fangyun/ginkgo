package com.github.fangyun.ginkgo.experiment;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** 一些记录日志的便利方法. */
public final class Logging {

	/** 被使用的logger，如果没有打开则为null */
	private static Logger logger = null;

	private static String previousTimeStamp = null;

	/**
	 * 用指定的级别记录一个消息.
	 *
	 * @see Logger#log(Level, String)
	 */
	public static synchronized void log(Level level, String message) {
		if (logger != null) {
			String stamp = GameBatch.timeStamp(false);
			logger.log(level, stamp + " thread "
					+ Thread.currentThread().getId() + " " + message);
			// 注意: 这不会造成包括午夜在内的长时间延误。
			if (previousTimeStamp != null && rawTime(stamp) - rawTime(previousTimeStamp) > 5) {
				logger.log(level, stamp + " thread "
						+ Thread.currentThread().getId() + " " + "LONG DELAY!");
			}
			previousTimeStamp = stamp;
		}
	}

	/**
	 * 在默认INFO级别上记录消息.
	 *
	 * @see Logger#log(Level, String)
	 */
	public static void log(String message) {
		log(Level.INFO, message);
	}

	/**
	 * 返回时间戳表示的从一天开始的分钟时间.
	 */
	public static int rawTime(String stamp) {
		int hour = Integer.parseInt(stamp.substring(11, 13));
		int minute = Integer.parseInt(stamp.substring(14, 16));
		return 60 * hour + minute;
	}

	/**
	 * 设置日志记录以显示在目录中一个时间戳的文件中。如果两个Ginkgo的实例在同一毫秒内启动，行为就没有意义了.
	 */
	public static void setFilePath(String directory) {
		logger = Logger.getLogger("ginkgo-default");
		new File(directory).mkdir();
		if(directory.endsWith(File.separator)) {
			directory += GameBatch.timeStamp(false) + ".log";
		}else {
			directory += File.separator + GameBatch.timeStamp(false) + ".log";
		}
		try {
			final FileHandler handler = new FileHandler(directory);
			handler.setFormatter(new Formatter() {
				@Override
				public String format(LogRecord record) {
					return record.getMessage() + "\n";
				}
			});
			logger.addHandler(handler);
			logger.setLevel(Level.ALL);
			logger.setUseParentHandlers(false);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
