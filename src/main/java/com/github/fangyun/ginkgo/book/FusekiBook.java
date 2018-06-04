package com.github.fangyun.ginkgo.book;

import static com.github.fangyun.ginkgo.experiment.Logging.log;

import java.io.InputStream;
import java.io.ObjectInputStream;

import com.github.fangyun.ginkgo.core.Board;
import com.github.fangyun.ginkgo.core.CoordinateSystem;

/**
 * 产生着子来自强大的棋手棋谱.
 * 
 * @see FusekiBookBuilder
 */
public final class FusekiBook implements OpeningBook {

	/** 布局的棋谱. */
	private SmallHashMap book;

	/** 在这之后，不要再去看这本棋谱了. */
	private int maxMoves;

	public FusekiBook() {
		this("books");
	}

	/** 得到哈希Map从文件中. */
	public FusekiBook(String directory) {
		final InputStream is = getClass().getResourceAsStream("/books/ginkgo_fuseki19.data");
		log("开始读公开棋谱");
		try (ObjectInputStream in = new ObjectInputStream(is)) {
			maxMoves = (Integer) in.readObject();
			book = (SmallHashMap) in.readObject();
			log("读完公开棋谱");
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public short nextMove(Board board) {
		final long fancyHash = board.getFancyHash();
		if (board.getTurn() < maxMoves) {
			if (book.containsKey(fancyHash)) {
				final short move = book.get(fancyHash);
				if (board.isLegal(move)) {
					return move;
				}
			}
		}
		return CoordinateSystem.NO_POINT;
	}
}
