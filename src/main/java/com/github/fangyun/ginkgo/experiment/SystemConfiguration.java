package com.github.fangyun.ginkgo.experiment;

import static com.github.fangyun.ginkgo.experiment.PropertyPaths.GINKGO_ROOT;
import static java.io.File.separator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** 保有系统依赖的属性。例如classpath */
enum SystemConfiguration {

	/** 单例名称. */
	SYSTEM;

	/** 运行GNUGo的命令. */
	final String gnugoHome;

	/** 运行实验的主机列表. */
	final List<String> hosts;

	/** 运行Java的命令. */
	final String java;

	/** 分配给Ginkgo的兆字节内存. */
	final int megabytes;

	/** Ginkgo的classpath. */
	final String ginkgoClassPath;

	/** 存放结果文件的目录. */
	final String resultsDirectory;

	/** 从config/system.properties读取配置. */
	SystemConfiguration() {
		final Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(GINKGO_ROOT + separator + "config" + separator + "system.properties"));
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		ginkgoClassPath = properties.getProperty("ginkgoClassPath");
		megabytes = Integer.getInteger("megabytes");
		gnugoHome = properties.getProperty("gnugoHome");
		java = properties.getProperty("java");
		resultsDirectory = properties.getProperty("resultsDirectory");
		hosts = new ArrayList<>();
		for (final String s : properties.stringPropertyNames()) {
			if (s.startsWith("host")) {
				hosts.add((String) properties.get(s));
			}
		}
	}
}
