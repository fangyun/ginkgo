package com.github.fangyun.ginkgo.feature;

import static com.github.fangyun.ginkgo.core.CoordinateSystem.NO_POINT;
import static com.github.fangyun.ginkgo.core.CoordinateSystem.PASS;

import java.io.Serializable;
import java.util.Arrays;

import com.github.fangyun.ginkgo.core.Color;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/**
 * 带遗忘的最好回复表. 这不是线程安全的；我们只是忽略偶尔的更新丢失.
 */
public final class LgrfTable implements Serializable {
	private static final long serialVersionUID = 5216955850220022701L;

	/**
	 * 条目 [c][i] 是着子i对颜色c最好的回复(或者NO_POINT，如果没有).
	 */
	private final short[][] replies1;

	/**
	 * 条目 [c][i][j] 是着子i,j对颜色c最好的回复(或者NO_POINT，如果没有).
	 */
	private final short[][][] replies2;

	public LgrfTable(CoordinateSystem coords) {
		replies1 = new short[2][coords.getFirstPointBeyondBoard()];
		replies2 = new short[2][coords.getFirstPointBeyondBoard()][coords.getFirstPointBeyondBoard()];
	}

	public void clear() {
		for (final short[] array : replies1) {
			Arrays.fill(array, NO_POINT);
		}
		for (final short[][] array : replies2) {
			for (final short[] array2 : array) {
				Arrays.fill(array2, NO_POINT);
			}
		}
	}

	/**
	 * 返回上一着子对颜色c最好存储的回复.或者NO_POINT，如果没有
	 */
	public short getFirstLevelReply(Color color, short previousMove) {
		return replies1[color.index()][previousMove];
	}

	/**
	 * 返回上连续两次着子对颜色c最好存储的回复.或者NO_POINT，如果没有.
	 */
	public short getSecondLevelReply(Color color, short penultimateMove, short previousMove) {
		return replies2[color.index()][penultimateMove][previousMove];
	}

	public void update(Color colorToPlay, boolean playoutWon, short penultimateMove, short previousMove, short reply) {
		if (reply != PASS) {
			if (playoutWon) {
				replies1[colorToPlay.index()][previousMove] = reply;
				replies2[colorToPlay.index()][penultimateMove][previousMove] = reply;
			} else {
				if (replies1[colorToPlay.index()][previousMove] == reply) {
					replies1[colorToPlay.index()][previousMove] = NO_POINT;
				}
				if (replies2[colorToPlay.index()][penultimateMove][previousMove] == reply) {
					replies2[colorToPlay.index()][penultimateMove][previousMove] = NO_POINT;
				}
			}
		}
	}
}
