package com.github.fangyun.ginkgo.sgf;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/** 包含用于写SGF文件的方法. */
public final class SgfWriter {

	/** 返回一个行或列的SGF字符串表示. */
	private static String rowOrColumnToStringSgf(int i) {
		return "" + "abcdefghijklmnopqrs".charAt(i);
	}

	/** 为p返回SGF坐标，包括“”为虚手. */
	public static String toSgf(short p, CoordinateSystem coords) {
		if (p == PASS) {
			return "";
		}
		return rowOrColumnToStringSgf(coords.row(p))
				+ rowOrColumnToStringSgf(coords.column(p));
	}

}
