package com.github.fangyun.ginkgo.mcts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个带有许多部分的复杂结构. 能够通过序列话复制. 这主要被用来拷贝Board和相关的BoardObservers等到每一个McRunnable.
 *
 */
public final class CopiableStructure implements Serializable {
	private static final long serialVersionUID = 820278532295333637L;
	private final List<Serializable> contents;

	public CopiableStructure() {
		this.contents = new ArrayList<>();
	}

	/** 添加一项目到此CopiableStructure. */
	public CopiableStructure add(Serializable item) {
		contents.add(item);
		return this;
	}

	/**
	 * 返回此CopiableStructure的深度拷贝.
	 *
	 * 采纳自： http://www.javaworld.com/article/2077578/learn-java/java-tip-
	 * 76--an-alternative-to-the-deep-copy-technique.html.
	 */
	public CopiableStructure copy() {
		try {
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(this);
			oos.flush();
			final ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
			final ObjectInputStream ois = new ObjectInputStream(bin);
			final CopiableStructure result = (CopiableStructure) ois.readObject();
			oos.close();
			ois.close();
			return result;
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		throw new IllegalStateException("不可达");
	}

	/**
	 * 返回在此CopiableStructure中指定类的对象. 如果它是一份拷贝，先调用copy()再调用get()在此拷贝上，这步骤是至关重要.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(Class<T> c) {
		for (final Serializable obj : contents) {
			if (c.isInstance(obj)) {
				return (T) obj;
			}
		}
		throw new IllegalArgumentException("无此对象在CopiableStructure中");
	}

}
