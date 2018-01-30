package com.github.fangyun.ginkgo.mcts;

import com.github.fangyun.ginkgo.core.CoordinateSystem;

public final class SimpleSearchNodeBuilder implements SearchNodeBuilder {

	private final CoordinateSystem coords;

	public SimpleSearchNodeBuilder(CoordinateSystem coords) {
		this.coords = coords;
	}

	@Override
	public SimpleSearchNode build() {
		return new SimpleSearchNode(coords);
	}

}
