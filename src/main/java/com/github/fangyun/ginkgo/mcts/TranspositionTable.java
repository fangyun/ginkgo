package com.github.fangyun.ginkgo.mcts;

import static com.github.fangyun.ginkgo.core.SuperKoTable.IGNORE_SIGN_BIT;
import com.github.fangyun.ginkgo.core.CoordinateSystem;
import com.github.fangyun.ginkgo.experiment.Logging;
import com.github.fangyun.ginkgo.util.ListNode;
import com.github.fangyun.ginkgo.util.Pool;

/** 代表棋盘配置节点的哈希表. */
public final class TranspositionTable {

	private final CoordinateSystem coords;

	/** 列表节点用来构建SearchNodes的子列表. */
	private final Pool<ListNode<SearchNode>> listNodes;

	/** 哈希表自己. */
	private final SearchNode[] table;

	private int nodesInUse;

	public TranspositionTable(int megabytes, SearchNodeBuilder builder, CoordinateSystem coords) {
		final int size = megabytes * 1024 * 16 / Math.max(81, coords.getArea());
		table = new SearchNode[size];
		nodesInUse = 0;
		for (int i = 0; i < size; i++) {
			table[i] = builder.build();
		}
		listNodes = new Pool<>();
		for (int i = 0; i < 3 * size; i++) {
			listNodes.free(new ListNode<SearchNode>());
		}
		this.coords = coords;
	}

	/** 增加父节点的子节点. */
	void addChild(SearchNode parent, SearchNode child) {
		final ListNode<SearchNode> node = listNodes.allocate();
		node.setKey(child);
		node.setNext(parent.getChildren());
		parent.setChildren(node);
	}

	/**
	 * 慢 -- 测试用. 返回从根能到的节点个数.
	 */
	public int dagSize(SearchNode root) {
		final int result = markNodesReachableFrom(root);
		for (int i = 0; i < table.length; i++) {
			table[i].setMarked(false);
		}
		return result;
	}

	/**
	 * 返回哈希值关联的节点，或者为null如果没有这样的点.
	 */
	public synchronized SearchNode findIfPresent(long fancyHash) {
		final int start = ((int) fancyHash & IGNORE_SIGN_BIT) % table.length;
		int slot = start;
		do {
			final SearchNode n = table[slot];
			if (n.isInUse()) {
				if (n.getFancyHash() == fancyHash) {
					return n;
				}
			} else {
				return null;
			}
			slot = (slot + 1) % table.length;
		} while (slot != start);
		return null;
	}

	/**
	 * 返回在表中哈希值关联的节点. 如果没有，从池中分配并返回. 如果池中没有有效节点，则返回null.
	 */
	synchronized SearchNode findOrAllocate(long fancyHash) {
		final int start = ((int) fancyHash & IGNORE_SIGN_BIT) % table.length;
		int slot = start;
		do {
			final SearchNode n = table[slot];
			if (n.isInUse()) {
				if (n.getFancyHash() == fancyHash) {
					return n;
				}
			} else {
				n.clear(fancyHash, coords);
				nodesInUse++;
				return n;
			}
			slot = (slot + 1) % table.length;
		} while (slot != start);
		return null;
	}

	/** 返回表中节点的数目。测试用. */
	int getCapacity() {
		return table.length;
	}

	/**
	 * 标识从节点能到达的所有节点, 这些节点在调用sweep()方法后而幸存.
	 * 
	 * @return 被标识的节点数目.
	 */
	int markNodesReachableFrom(SearchNode root) {
		if (root == null || root.isMarked()) {
			return 0;
		}
		root.setMarked(true);
		int sum = 1;
		ListNode<SearchNode> child = root.getChildren();
		while (child != null) {
			sum += markNodesReachableFrom(child.getKey());
			child = child.getNext();
		}
		return sum;
	}

	/** @return 返回表节点当前在用的数目. */
	int getNodesInUse() {
		return nodesInUse;
	}

	/**
	 * 在{@link #markNodesReachableFrom(SearchNode)}后, 释放所有不用的SearchNodes
	 * (标识它们因为没有在用)和关联的ListNodes (返回它们到池中).
	 */
	void sweep() {
		Logging.log(
				"在用节点数 " + nodesInUse + "/" + table.length + " (" + (nodesInUse * 100) / table.length + "%)");
		for (int i = 0; i < table.length; i++) {
			final SearchNode node = table[i];
			if (node.isInUse()) {
				if (node.isMarked()) {
					node.setMarked(false);
				} else {
					ListNode<SearchNode> n = node.getChildren();
					while (n != null) {
						n = listNodes.free(n);
					}
					node.free();
					nodesInUse--;
				}
			}
		}
	}

}
