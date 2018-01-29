package com.github.fangyun.ginkgo.thirdparty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

/**
 * <h3>梅森旋转算法和梅森旋转快速算法</h3>
 * <p>
 * <b>版本 13</b>, 基于从<a href=
 * "http://www.math.keio.ac.jp/matumoto/emt.html">梅森旋转算法主页</a>找到的版本MT199937(99/10/29)
 * 使用Sean Luke在2004年10月的新2002/1/26初始化算法提高初始化
 * <p>
 * <b>MersenneTwister（梅森旋转算法）</b>是个嵌入子类替换java.util.Random.它被正确同步后能被用于多线程环境.
 * 在现代虚拟机，例如HotSpot, 它几乎比java.util.Random慢1/3
 * <p>
 * <b>MersenneTwisterFast（梅森旋转快速算法）</b> 不是java.util.Random子类.
 * 它拥有和Random类同样的公开方法, 然而它在算法上等价与MersenneTwister. MersenneTwisterFast有硬编码
 * 直接内联所有的方法, 并标识为final (这样是其中一个推论).
 * 进一步，所有方法不是同步的，所以同样的MersenneTwisterFast实例不能在多线程中共享.
 * 所有这些帮助MersenneTwisterFast达到两倍于MersenneTwister的速度.
 * java.util.Random大约比MersenneTwisterFast慢1/3.
 *
 * <h3>关于梅森旋转算法</h3>
 * <p>
 * 这是一个Java版本的C-程序MT19937: 整数版本. MT19937算法由Makoto Matsumoto和Takuji Nishimura创建,
 * 他们要求: "当你用它，发邮件给matumoto@math.keio.ac.jp，说明你的工作的合适的参考内容".
 * 指明这是一个他们算法的Java版本翻译.
 *
 * <p>
 * <b>参考. </b> Makato Matsumoto and Takuji Nishimura, "Mersenne Twister: A
 * 623-Dimensionally Equidistributed Uniform Pseudo-Random Number Generator",
 * <i>ACM Transactions on Modeling and. Computer Simulation,</i> Vol. 8, No. 1,
 * January 1998, pp 3--30.
 *
 * <h3>关于此版本</h3>
 *
 * <p>
 * <b>自从V12变更:</b> 增加clone()方法.
 *
 * <p>
 * <b>自从V11变更:</b> 增加stateEquals(...)方法. MersenneTwisterFast
 * 等于另一个MersenneTwisterFasts使用ID状态; 类似MersenneTwister等同于其它MersenneTwister使用ID状态.
 * 这不是equals(...)，因为后者需要对值的比较的不变性合同.
 *
 * <p>
 * <b>自从V10变更:</b>
 * 一个文档错误建议setSeed(int[])需要一个int[]数组624长.事实上数组能是任意非零长度.新版本也检查了这一事实.
 *
 * <p>
 * <b>自从V9变更:</b> 提供readState(stream)和writeState(stream)方法.
 *
 * <p>
 * <b>自从V8变更:</b> setSeed(int)仅仅使用种子的前28位;它应当有32位. 对于小数值的种子的行为是一致的.
 *
 * <p>
 * <b>自从V7变更:</b> 一个文档错误在MersenneTwisterFast
 * (而非MersenneTwister)，情况是nextDouble选择唯一数从全闭区间[0,1].
 * 它不应该这样，nextDouble合同在MersenneTwisterFast, MersenneTwister, and
 * java.util.Random中都是一致的，也就是说选择自半开区间[0,1).1.0不会返回. 同样类似合同存在于nextFloat.
 *
 * <p>
 * <b>自从V6变更:</b>许可证从LGPL到BSD. 和java.util.Random新的时间性能比较.
 * 最近版本的HotSpot帮助Random提高速度，比MersenneTwister快，但比MersenneTwisterFast慢
 * (应当如此，因为它使用了复杂度小些的算法，但是是同步的).
 * 
 * <p>
 * <b>自从V5变更:</b> 新的空参数的构造器，使得和java.util.Random一样。也就是，它一当前毫秒时间为种子.
 *
 * <p>
 * <b>自从V4变更:</b> 新的初始化算法. 参见 (see <a
 * href="http://www.math.keio.ac.jp/matumoto/MT2002/emt19937ar.html"</a>
 * http://www.math.keio.ac.jp/matumoto/MT2002/emt19937ar.html</a>)
 *
 * <p>
 * 1997七月，MersenneTwister代码基于Takuji Nishimura写的标准的MT19937 C/C++代码, 参考来自Topher
 * Cooper and Marc Rieffel建议. Michael Lecuyer于1999年1月翻译成Java, 原始代码版权(c) 1999归
 * Michael Lecuyer所有.
 *
 * <h3>Java注释</h3>
 * 
 * <p>
 * 这个实现修复了在Java 1.2的Random的缺陷, 这意味着类能用于早期版本的Java.参见 <a href=
 * "http://www.javasoft.com/products/jdk/1.2/docs/api/java/util/Random.html">
 * JDK 1.2 java.util.Random文档</a> 获取进一步的随机数产生合同约定. 另外有一个在JDK
 * java.util.Random.nextBytes()方法中未注明的缺陷，在此代码中也修复了.
 *
 * <p>
 * 就像java.util.Random,产生器接受long型种子并没有全部用上. java.util.Random用了48位.Mersenne
 * Twister用了32 bits (int型长度).因此它用在种子长度不超过int型范围时也更好.
 *
 * <p>
 * MersenneTwister能稳定用在JDK
 * 1.1.5或更高.更早的Java版本在java.util.Random有严重缺陷;已有MersenneTwisterFast
 * (而非MersenneTwister，也非java.util.Random)应当用在此环境中.
 *
 * <h3>License</h3>
 *
 * Copyright (c) 2003 by Sean Luke. <br>
 * Portions copyright (c) 1993 by Michael Lecuyer. <br>
 * All rights reserved. <br>
 *
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <li>Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <li>Neither the name of the copyright owners, their employers, nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @version 13
 */

// 注: 此类硬编码内联了所有方法，这使得其可读性差.事实上Mersenne
// Twister是很容易理解的代码: 如果你试图理解代码，我强烈建议先看MersenneTwister.java.
// -- Sean

public class MersenneTwisterFast implements Serializable, Cloneable {
	private static final long serialVersionUID = 7062457826036112059L;
	// Period parameters
	private static final int N = 624;
	private static final int M = 397;
	private static final int MATRIX_A = 0x9908b0df; // private static final *
													// constant vector a
	private static final int UPPER_MASK = 0x80000000; // most significant w-r
														// bits
	private static final int LOWER_MASK = 0x7fffffff; // least significant r
														// bits

	// Tempering parameters
	private static final int TEMPERING_MASK_B = 0x9d2c5680;
	private static final int TEMPERING_MASK_C = 0xefc60000;

	private int mt[]; // the array for the state vector
	private int mti; // mti==N+1 means mt[N] is not initialized
	private int mag01[];

	// a good initial seed (of int size, though stored in a long)
	// private static final long GOOD_SEED = 4357;

	private double __nextNextGaussian;
	private boolean __haveNextNextGaussian;

	/*
	 * We're overriding all internal data, to my knowledge, so this should be
	 * okay
	 */
	public Object clone() throws CloneNotSupportedException {
		MersenneTwisterFast f = (MersenneTwisterFast) (super.clone());
		f.mt = (int[]) (mt.clone());
		f.mag01 = (int[]) (mag01.clone());
		return f;
	}

	public boolean stateEquals(Object o) {
		if (o == this)
			return true;
		if (o == null || !(o instanceof MersenneTwisterFast))
			return false;
		MersenneTwisterFast other = (MersenneTwisterFast) o;
		if (mti != other.mti)
			return false;
		for (int x = 0; x < mag01.length; x++)
			if (mag01[x] != other.mag01[x])
				return false;
		for (int x = 0; x < mt.length; x++)
			if (mt[x] != other.mt[x])
				return false;
		return true;
	}

	/** Reads the entire state of the MersenneTwister RNG from the stream */
	public void readState(DataInputStream stream) throws IOException {
		int len = mt.length;
		for (int x = 0; x < len; x++)
			mt[x] = stream.readInt();

		len = mag01.length;
		for (int x = 0; x < len; x++)
			mag01[x] = stream.readInt();

		mti = stream.readInt();
		__nextNextGaussian = stream.readDouble();
		__haveNextNextGaussian = stream.readBoolean();
	}

	/** Writes the entire state of the MersenneTwister RNG to the stream */
	public void writeState(DataOutputStream stream) throws IOException {
		int len = mt.length;
		for (int x = 0; x < len; x++)
			stream.writeInt(mt[x]);

		len = mag01.length;
		for (int x = 0; x < len; x++)
			stream.writeInt(mag01[x]);

		stream.writeInt(mti);
		stream.writeDouble(__nextNextGaussian);
		stream.writeBoolean(__haveNextNextGaussian);
	}

	/**
	 * Constructor using the default seed.
	 */
	public MersenneTwisterFast() {
		this(System.currentTimeMillis());
	}

	/**
	 * Constructor using a given seed. Though you pass this seed in as a long,
	 * it's best to make sure it's actually an integer.
	 *
	 */
	public MersenneTwisterFast(final long seed) {
		setSeed(seed);
	}

	/**
	 * Constructor using an array of integers as seed. Your array must have a
	 * non-zero length. Only the first 624 integers in the array are used; if
	 * the array is shorter than this then integers are repeatedly used in a
	 * wrap-around fashion.
	 */
	public MersenneTwisterFast(final int[] array) {
		setSeed(array);
	}

	/**
	 * Initalize the pseudo random number generator. Don't pass in a long that's
	 * bigger than an int (Mersenne Twister only uses the first 32 bits for its
	 * seed).
	 */

	synchronized public void setSeed(final long seed) {
		// Due to a bug in java.util.Random clear up to 1.2, we're
		// doing our own Gaussian variable.
		__haveNextNextGaussian = false;

		mt = new int[N];

		mag01 = new int[2];
		mag01[0] = 0x0;
		mag01[1] = MATRIX_A;

		mt[0] = (int) (seed & 0xffffffff);
		for (mti = 1; mti < N; mti++) {
			mt[mti] = (1812433253 * (mt[mti - 1] ^ (mt[mti - 1] >>> 30)) + mti);
			/* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
			/* In the previous versions, MSBs of the seed affect */
			/* only MSBs of the array mt[]. */
			/* 2002/01/09 modified by Makoto Matsumoto */
			mt[mti] &= 0xffffffff;
			/* for >32 bit machines */
		}
	}

	/**
	 * Sets the seed of the MersenneTwister using an array of integers. Your
	 * array must have a non-zero length. Only the first 624 integers in the
	 * array are used; if the array is shorter than this then integers are
	 * repeatedly used in a wrap-around fashion.
	 */

	synchronized public void setSeed(final int[] array) {
		if (array.length == 0)
			throw new IllegalArgumentException("Array length must be greater than zero");
		int i, j, k;
		setSeed(19650218);
		i = 1;
		j = 0;
		k = (N > array.length ? N : array.length);
		for (; k != 0; k--) {
			mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * 1664525)) + array[j]
					+ j; /* non linear */
			mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
			i++;
			j++;
			if (i >= N) {
				mt[0] = mt[N - 1];
				i = 1;
			}
			if (j >= array.length)
				j = 0;
		}
		for (k = N - 1; k != 0; k--) {
			mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * 1566083941))
					- i; /* non linear */
			mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
			i++;
			if (i >= N) {
				mt[0] = mt[N - 1];
				i = 1;
			}
		}
		mt[0] = 0x80000000; /* MSB is 1; assuring non-zero initial array */
	}

	public final int nextInt() {
		int y;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return y;
	}

	public final short nextShort() {
		int y;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return (short) (y >>> 16);
	}

	public final char nextChar() {
		int y;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return (char) (y >>> 16);
	}

	public final boolean nextBoolean() {
		int y;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return (boolean) ((y >>> 31) != 0);
	}

	/**
	 * This generates a coin flip with a probability <tt>probability</tt> of
	 * returning true, else returning false. <tt>probability</tt> must be
	 * between 0.0 and 1.0, inclusive. Not as precise a random real event as
	 * nextBoolean(double), but twice as fast. To explicitly use this, remember
	 * you may need to cast to float first.
	 */

	public final boolean nextBoolean(final float probability) {
		int y;

		if (probability < 0.0f || probability > 1.0f)
			throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
		if (probability == 0.0f)
			return false; // fix half-open issues
		else if (probability == 1.0f)
			return true; // fix half-open issues
		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return (y >>> 8) / ((float) (1 << 24)) < probability;
	}

	/**
	 * This generates a coin flip with a probability <tt>probability</tt> of
	 * returning true, else returning false. <tt>probability</tt> must be
	 * between 0.0 and 1.0, inclusive.
	 */

	public final boolean nextBoolean(final double probability) {
		int y;
		int z;

		if (probability < 0.0 || probability > 1.0)
			throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
		if (probability == 0.0)
			return false; // fix half-open issues
		else if (probability == 1.0)
			return true; // fix half-open issues
		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
			for (; kk < N - 1; kk++) {
				z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
			z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

			mti = 0;
		}

		z = mt[mti++];
		z ^= z >>> 11; // TEMPERING_SHIFT_U(z)
		z ^= (z << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(z)
		z ^= (z << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(z)
		z ^= (z >>> 18); // TEMPERING_SHIFT_L(z)

		/* derived from nextDouble documentation in jdk 1.2 docs, see top */
		return ((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53) < probability;
	}

	public final byte nextByte() {
		int y;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return (byte) (y >>> 24);
	}

	public final void nextBytes(byte[] bytes) {
		int y;

		for (int x = 0; x < bytes.length; x++) {
			if (mti >= N) // generate N words at one time
			{
				int kk;
				final int[] mt = this.mt; // locals are slightly faster
				final int[] mag01 = this.mag01; // locals are slightly faster

				for (kk = 0; kk < N - M; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				for (; kk < N - 1; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

				mti = 0;
			}

			y = mt[mti++];
			y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

			bytes[x] = (byte) (y >>> 24);
		}
	}

	public final long nextLong() {
		int y;
		int z;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
			for (; kk < N - 1; kk++) {
				z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
			z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

			mti = 0;
		}

		z = mt[mti++];
		z ^= z >>> 11; // TEMPERING_SHIFT_U(z)
		z ^= (z << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(z)
		z ^= (z << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(z)
		z ^= (z >>> 18); // TEMPERING_SHIFT_L(z)

		return (((long) y) << 32) + (long) z;
	}

	/**
	 * Returns a long drawn uniformly from 0 to n-1. Suffice it to say, n must
	 * be > 0, or an IllegalArgumentException is raised.
	 */
	public final long nextLong(final long n) {
		if (n <= 0)
			throw new IllegalArgumentException("n must be > 0");

		long bits, val;
		do {
			int y;
			int z;

			if (mti >= N) // generate N words at one time
			{
				int kk;
				final int[] mt = this.mt; // locals are slightly faster
				final int[] mag01 = this.mag01; // locals are slightly faster

				for (kk = 0; kk < N - M; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				for (; kk < N - 1; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

				mti = 0;
			}

			y = mt[mti++];
			y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

			if (mti >= N) // generate N words at one time
			{
				int kk;
				final int[] mt = this.mt; // locals are slightly faster
				final int[] mag01 = this.mag01; // locals are slightly faster

				for (kk = 0; kk < N - M; kk++) {
					z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
				}
				for (; kk < N - 1; kk++) {
					z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
				}
				z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

				mti = 0;
			}

			z = mt[mti++];
			z ^= z >>> 11; // TEMPERING_SHIFT_U(z)
			z ^= (z << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(z)
			z ^= (z << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(z)
			z ^= (z >>> 18); // TEMPERING_SHIFT_L(z)

			bits = (((((long) y) << 32) + (long) z) >>> 1);
			val = bits % n;
		} while (bits - val + (n - 1) < 0);
		return val;
	}

	/**
	 * Returns a random double in the half-open range from [0.0,1.0). Thus 0.0
	 * is a valid result but 1.0 is not.
	 */
	public final double nextDouble() {
		int y;
		int z;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
			for (; kk < N - 1; kk++) {
				z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
			z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

			mti = 0;
		}

		z = mt[mti++];
		z ^= z >>> 11; // TEMPERING_SHIFT_U(z)
		z ^= (z << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(z)
		z ^= (z << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(z)
		z ^= (z >>> 18); // TEMPERING_SHIFT_L(z)

		/* derived from nextDouble documentation in jdk 1.2 docs, see top */
		return ((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53);
	}

	public final double nextGaussian() {
		if (__haveNextNextGaussian) {
			__haveNextNextGaussian = false;
			return __nextNextGaussian;
		} else {
			double v1, v2, s;
			do {
				int y;
				int z;
				int a;
				int b;

				if (mti >= N) // generate N words at one time
				{
					int kk;
					final int[] mt = this.mt; // locals are slightly faster
					final int[] mag01 = this.mag01; // locals are slightly
													// faster

					for (kk = 0; kk < N - M; kk++) {
						y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
					}
					for (; kk < N - 1; kk++) {
						y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
					}
					y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
					mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

					mti = 0;
				}

				y = mt[mti++];
				y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
				y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
				y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
				y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

				if (mti >= N) // generate N words at one time
				{
					int kk;
					final int[] mt = this.mt; // locals are slightly faster
					final int[] mag01 = this.mag01; // locals are slightly
													// faster

					for (kk = 0; kk < N - M; kk++) {
						z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
					}
					for (; kk < N - 1; kk++) {
						z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
					}
					z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
					mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

					mti = 0;
				}

				z = mt[mti++];
				z ^= z >>> 11; // TEMPERING_SHIFT_U(z)
				z ^= (z << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(z)
				z ^= (z << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(z)
				z ^= (z >>> 18); // TEMPERING_SHIFT_L(z)

				if (mti >= N) // generate N words at one time
				{
					int kk;
					final int[] mt = this.mt; // locals are slightly faster
					final int[] mag01 = this.mag01; // locals are slightly
													// faster

					for (kk = 0; kk < N - M; kk++) {
						a = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + M] ^ (a >>> 1) ^ mag01[a & 0x1];
					}
					for (; kk < N - 1; kk++) {
						a = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + (M - N)] ^ (a >>> 1) ^ mag01[a & 0x1];
					}
					a = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
					mt[N - 1] = mt[M - 1] ^ (a >>> 1) ^ mag01[a & 0x1];

					mti = 0;
				}

				a = mt[mti++];
				a ^= a >>> 11; // TEMPERING_SHIFT_U(a)
				a ^= (a << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(a)
				a ^= (a << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(a)
				a ^= (a >>> 18); // TEMPERING_SHIFT_L(a)

				if (mti >= N) // generate N words at one time
				{
					int kk;
					final int[] mt = this.mt; // locals are slightly faster
					final int[] mag01 = this.mag01; // locals are slightly
													// faster

					for (kk = 0; kk < N - M; kk++) {
						b = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + M] ^ (b >>> 1) ^ mag01[b & 0x1];
					}
					for (; kk < N - 1; kk++) {
						b = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
						mt[kk] = mt[kk + (M - N)] ^ (b >>> 1) ^ mag01[b & 0x1];
					}
					b = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
					mt[N - 1] = mt[M - 1] ^ (b >>> 1) ^ mag01[b & 0x1];

					mti = 0;
				}

				b = mt[mti++];
				b ^= b >>> 11; // TEMPERING_SHIFT_U(b)
				b ^= (b << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(b)
				b ^= (b << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(b)
				b ^= (b >>> 18); // TEMPERING_SHIFT_L(b)

				/*
				 * derived from nextDouble documentation in jdk 1.2 docs, see
				 * top
				 */
				v1 = 2 * (((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53)) - 1;
				v2 = 2 * (((((long) (a >>> 6)) << 27) + (b >>> 5)) / (double) (1L << 53)) - 1;
				s = v1 * v1 + v2 * v2;
			} while (s >= 1 || s == 0);
			double multiplier = /* Strict */Math.sqrt(-2 * /* Strict */Math.log(s) / s);
			__nextNextGaussian = v2 * multiplier;
			__haveNextNextGaussian = true;
			return v1 * multiplier;
		}
	}

	/**
	 * Returns a random float in the half-open range from [0.0f,1.0f). Thus 0.0f
	 * is a valid result but 1.0f is not.
	 */
	public final float nextFloat() {
		int y;

		if (mti >= N) // generate N words at one time
		{
			int kk;
			final int[] mt = this.mt; // locals are slightly faster
			final int[] mag01 = this.mag01; // locals are slightly faster

			for (kk = 0; kk < N - M; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (; kk < N - 1; kk++) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
				mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
		}

		y = mt[mti++];
		y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

		return (y >>> 8) / ((float) (1 << 24));
	}

	/**
	 * Returns an integer drawn uniformly from 0 to n-1. Suffice it to say, n
	 * must be > 0, or an IllegalArgumentException is raised.
	 */
	public final int nextInt(final int n) {
		if (n <= 0)
			throw new IllegalArgumentException("n must be > 0");

		if ((n & -n) == n) // i.e., n is a power of 2
		{
			int y;

			if (mti >= N) // generate N words at one time
			{
				int kk;
				final int[] mt = this.mt; // locals are slightly faster
				final int[] mag01 = this.mag01; // locals are slightly faster

				for (kk = 0; kk < N - M; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				for (; kk < N - 1; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

				mti = 0;
			}

			y = mt[mti++];
			y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

			return (int) ((n * (long) (y >>> 1)) >> 31);
		}

		int bits, val;
		do {
			int y;

			if (mti >= N) // generate N words at one time
			{
				int kk;
				final int[] mt = this.mt; // locals are slightly faster
				final int[] mag01 = this.mag01; // locals are slightly faster

				for (kk = 0; kk < N - M; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				for (; kk < N - 1; kk++) {
					y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
					mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
				y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

				mti = 0;
			}

			y = mt[mti++];
			y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

			bits = (y >>> 1);
			val = bits % n;
		} while (bits - val + (n - 1) < 0);
		return val;
	}

	/**
	 * Tests the code.
	 */
	public static void main(String args[]) {
		int j;

		MersenneTwisterFast r;

		// CORRECTNESS TEST
		// COMPARE WITH
		// http://www.math.keio.ac.jp/matumoto/CODES/MT2002/mt19937ar.out

		r = new MersenneTwisterFast(new int[] { 0x123, 0x234, 0x345, 0x456 });
		System.out.println("Output of MersenneTwisterFast with new (2002/1/26) seeding mechanism");
		for (j = 0; j < 1000; j++) {
			// first, convert the int from signed to "unsigned"
			long l = (long) r.nextInt();
			if (l < 0)
				l += 4294967296L; // max int value
			String s = String.valueOf(l);
			while (s.length() < 10)
				s = " " + s; // buffer
			System.out.print(s + " ");
			if (j % 5 == 4)
				System.out.println();
		}

		// SPEED TEST

		final long SEED = 4357;

		int xx;
		long ms;
		System.out.println("\nTime to test grabbing 100000000 ints");

		Random rr = new Random(SEED);
		xx = 0;
		ms = System.currentTimeMillis();
		for (j = 0; j < 100000000; j++)
			xx += rr.nextInt();
		System.out.println("java.util.Random: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx);

		r = new MersenneTwisterFast(SEED);
		ms = System.currentTimeMillis();
		xx = 0;
		for (j = 0; j < 100000000; j++)
			xx += r.nextInt();
		System.out.println(
				"Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx);

		// TEST TO COMPARE TYPE CONVERSION BETWEEN
		// MersenneTwisterFast.java AND MersenneTwister.java

		System.out.println("\nGrab the first 1000 booleans");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextBoolean() + " ");
			if (j % 8 == 7)
				System.out.println();
		}
		if (!(j % 8 == 7))
			System.out.println();

		System.out.println("\nGrab 1000 booleans of increasing probability using nextBoolean(double)");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextBoolean((double) (j / 999.0)) + " ");
			if (j % 8 == 7)
				System.out.println();
		}
		if (!(j % 8 == 7))
			System.out.println();

		System.out.println("\nGrab 1000 booleans of increasing probability using nextBoolean(float)");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextBoolean((float) (j / 999.0f)) + " ");
			if (j % 8 == 7)
				System.out.println();
		}
		if (!(j % 8 == 7))
			System.out.println();

		byte[] bytes = new byte[1000];
		System.out.println("\nGrab the first 1000 bytes using nextBytes");
		r = new MersenneTwisterFast(SEED);
		r.nextBytes(bytes);
		for (j = 0; j < 1000; j++) {
			System.out.print(bytes[j] + " ");
			if (j % 16 == 15)
				System.out.println();
		}
		if (!(j % 16 == 15))
			System.out.println();

		byte b;
		System.out.println("\nGrab the first 1000 bytes -- must be same as nextBytes");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print((b = r.nextByte()) + " ");
			if (b != bytes[j])
				System.out.print("BAD ");
			if (j % 16 == 15)
				System.out.println();
		}
		if (!(j % 16 == 15))
			System.out.println();

		System.out.println("\nGrab the first 1000 shorts");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextShort() + " ");
			if (j % 8 == 7)
				System.out.println();
		}
		if (!(j % 8 == 7))
			System.out.println();

		System.out.println("\nGrab the first 1000 ints");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextInt() + " ");
			if (j % 4 == 3)
				System.out.println();
		}
		if (!(j % 4 == 3))
			System.out.println();

		System.out.println("\nGrab the first 1000 ints of different sizes");
		r = new MersenneTwisterFast(SEED);
		int max = 1;
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextInt(max) + " ");
			max *= 2;
			if (max <= 0)
				max = 1;
			if (j % 4 == 3)
				System.out.println();
		}
		if (!(j % 4 == 3))
			System.out.println();

		System.out.println("\nGrab the first 1000 longs");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextLong() + " ");
			if (j % 3 == 2)
				System.out.println();
		}
		if (!(j % 3 == 2))
			System.out.println();

		System.out.println("\nGrab the first 1000 longs of different sizes");
		r = new MersenneTwisterFast(SEED);
		long max2 = 1;
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextLong(max2) + " ");
			max2 *= 2;
			if (max2 <= 0)
				max2 = 1;
			if (j % 4 == 3)
				System.out.println();
		}
		if (!(j % 4 == 3))
			System.out.println();

		System.out.println("\nGrab the first 1000 floats");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextFloat() + " ");
			if (j % 4 == 3)
				System.out.println();
		}
		if (!(j % 4 == 3))
			System.out.println();

		System.out.println("\nGrab the first 1000 doubles");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextDouble() + " ");
			if (j % 3 == 2)
				System.out.println();
		}
		if (!(j % 3 == 2))
			System.out.println();

		System.out.println("\nGrab the first 1000 gaussian doubles");
		r = new MersenneTwisterFast(SEED);
		for (j = 0; j < 1000; j++) {
			System.out.print(r.nextGaussian() + " ");
			if (j % 3 == 2)
				System.out.println();
		}
		if (!(j % 3 == 2))
			System.out.println();

	}
}
