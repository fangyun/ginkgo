package com.github.fangyun.ginkgo.patterns;

import java.io.*;
import java.util.Arrays;

/** 对模式哈希存赢率. */
public final class ShapeTable implements Serializable {
	private static final long serialVersionUID = -3915546434380921804L;

	private final float[][] winRateTables;

	private float scalingFactor;

	/** 创建空白型表，其中每个条目等于0.5. */
	public ShapeTable() {
		this(0.99f);
	}

	/** 创建ShapeTable，用指定文件填充数据. */
	public ShapeTable(String filePath, float scalingFactor) {
		float[][] fake = null;
		try (ObjectInputStream objectInputStream = new ObjectInputStream(
				new FileInputStream(filePath))) {
			fake = (float[][]) objectInputStream.readObject();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		winRateTables = fake;
		this.scalingFactor = scalingFactor;
	}

	/**
	 * 用特定的伸缩因子构建模式数据. 表创建，并填充0.5.
	 */
	public ShapeTable(float scalingFactor) {
		this.scalingFactor = scalingFactor;
		winRateTables = new float[3][2097152];
		for (float[] table : winRateTables) {
			Arrays.fill(table, 0.5f);
		}
	}

	public void getRates() {
		// TODO What is this specific filename doing here?
		try (PrintWriter writer = new PrintWriter(new File("test-books/patterns5x5.csv"))) {
			for (float winRate : winRateTables[0]) {
				writer.println(winRate + ",");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public float getScalingFactor(){
		return scalingFactor;
	}

	public double testGetRate(int index) {
		return winRateTables[1][index];
	}

	public float[][] getWinRateTables() {
		return winRateTables;
	}

	/** 更新表对给定的哈希用新的赢率. */
	public void update(long hash, boolean win) {
		for (int i = 0; i < 3; i++) {
			// TODO Get rid of this ridiculous magic number
			int index = (int) (hash >> (21 * i) & 2097151);
			winRateTables[i][index] = scalingFactor * winRateTables[i][index]
					+ (win ? (1.0f - scalingFactor) : 0);
		}
	}

	/** 返回给定模式的赢率. */
	public float getWinRate(long hash) {
		float result = 0;
		for (int i = 0; i < 3; i++) {
			int index = (int) (hash >> (21 * i) & 2097151);
			result += winRateTables[i][index];
		}
		return result / 3;
	}
	
	/** 打印存在表的每一节赢率. */
	public void printIndividualWinRates(long hash){
		for (int i = 0; i < 3; i++) {
			int index = (int) (hash >> (21 * i) & 2097151);
			System.out.println("Section " + i + ": " + winRateTables[i][index]);
		}
	}
}
