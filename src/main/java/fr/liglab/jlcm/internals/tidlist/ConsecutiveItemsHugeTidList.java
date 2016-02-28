package fr.liglab.jlcm.internals.tidlist;

import java.util.Arrays;

import fr.liglab.jlcm.internals.Counters;
import gnu.trove.iterator.TIntIterator;
import it.unimi.dsi.fastutil.ints.IntBigArrays;

public class ConsecutiveItemsHugeTidList implements TidList {
	
	private long[] indexAndFreqs;
	private int[][] tidLists;
	
	public ConsecutiveItemsHugeTidList(Counters counters, int highestTidList) {
		long startPos = 0;
		int top = Math.min(highestTidList, counters.getMaxFrequent() + 1);
		this.indexAndFreqs = new long[top * 2];
		for (int i = 0; i < top; i++) {
			int itemIndex = i << 1;
			int count = counters.distinctTransactionsCounts[i];
			if (count > 0) {
				this.indexAndFreqs[itemIndex] = startPos;
				startPos += count;
			} else {
				this.indexAndFreqs[itemIndex] = -1;
			}
		}
		this.tidLists = IntBigArrays.newBigArray(startPos);
	}

	@Override
	public void addTransaction(int item, int transaction) {
		int itemIndex = item << 1;
		if (itemIndex > this.indexAndFreqs.length || this.indexAndFreqs[itemIndex] == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		long start = this.indexAndFreqs[itemIndex];
		long index = this.indexAndFreqs[itemIndex + 1];
		IntBigArrays.set(tidLists, start + index, transaction);
		this.indexAndFreqs[itemIndex + 1]++;
	}

	@Override
	public TIntIterable getIterable(final int item) {
		return new TIntIterable() {

			@Override
			public TIntIterator iterator() {
				return get(item);
			}
		};
	}

	@Override
	public TIntIterator get(int item) {
		int itemIndex = item << 1;
		if (itemIndex > this.indexAndFreqs.length || this.indexAndFreqs[itemIndex] == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		final long startPos = this.indexAndFreqs[itemIndex];
		final long length = this.indexAndFreqs[itemIndex + 1];
		return new TidIterator(length, startPos);
	}

	private final class TidIterator implements TIntIterator {
		private long i = 0;
		private long end;
		
		public TidIterator(long length, long startPos) {
			this.i = startPos;
			this.end = startPos + length;
		}

		@Override
		public boolean hasNext() {
			return this.i < this.end;
		}
		
		@Override
		public int next() {
			return IntBigArrays.get(tidLists, this.i++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public TidList clone() {
		try {
			ConsecutiveItemsHugeTidList c = (ConsecutiveItemsHugeTidList) super.clone();
			c.indexAndFreqs = Arrays.copyOf(this.indexAndFreqs, this.indexAndFreqs.length);
			c.tidLists = IntBigArrays.copy(this.tidLists);
			return c;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
