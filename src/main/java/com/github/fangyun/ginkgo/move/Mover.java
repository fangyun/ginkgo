package com.github.fangyun.ginkgo.move;

import java.io.Serializable;

import com.github.fangyun.ginkgo.thirdparty.MersenneTwisterFast;

/** 落子. */
public interface Mover extends Serializable {

	/**
	 * 选择并落一子.
	 *
	 * @param fast 如果true，采用playFast而不是play.
	 */
	public short selectAndPlayOneMove(MersenneTwisterFast random, boolean fast);

}