package com.github.fangyun.ginkgo.experiment;

import java.io.File;

import com.github.fangyun.ginkgo.ui.Ginkgo;

/** 定义常量用来找到属性文件. */
public class PropertyPaths {

	/**
	 * 包含bin和config的目录.
	 */
	public static final String GINKGO_ROOT = System.getProperty("ginkgo.root",
			Ginkgo.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "..") + File.separator;

	private PropertyPaths() {
	}

}
