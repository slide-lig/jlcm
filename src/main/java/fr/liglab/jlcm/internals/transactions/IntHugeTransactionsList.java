package fr.liglab.jlcm.internals.transactions;

import java.util.Arrays;
import java.util.Iterator;

import fr.liglab.jlcm.internals.Counters;
import it.unimi.dsi.fastutil.ints.IntBigArrays;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class IntHugeTransactionsList implements TransactionsList {
	
	private int[][] concatenated;
	private int size = 0;
	
	private int[] indexSegmentsAndFreqs;

	private int transId = -1;
	private int writeSegment = 0;
	private int writeIndex = 0;
	
	public static int getMaxTransId(Counters c) {
		return c.distinctTransactionsCount - 1;
	}
	
	public IntHugeTransactionsList(Counters c) {
		this(c.distinctTransactionLengthSum, c.distinctTransactionsCount);
	}

	public IntHugeTransactionsList(long transactionsLength, int nbTransactions) {
		this.indexSegmentsAndFreqs = new int[nbTransactions * 3];
		Arrays.fill(this.indexSegmentsAndFreqs, -1);
		this.concatenated = IntBigArrays.newBigArray(transactionsLength);
	}
	
	/**
	 * These datasets are never compressed, given their size this would not 
	 * be amortized
	 */
	@Override
	public void compress(int coreItem) {
	}
	
	@Override
	public int size() {
		return this.size;
	}
	
	@Override
	public TransactionsList clone() {
		try {
			IntHugeTransactionsList o = (IntHugeTransactionsList) super.clone();
			o.indexSegmentsAndFreqs = Arrays.copyOf(this.indexSegmentsAndFreqs, this.indexSegmentsAndFreqs.length);
			o.concatenated = IntBigArrays.copy(this.concatenated);
			o.size = this.size;
			return o;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void startWriting() {
		this.transId = -1;
		this.writeSegment = 0;
		this.writeIndex = 0;
	}

	@Override
	public int beginTransaction(int support) {
		this.transId++;
		int startPos = this.transId * 3;
		indexSegmentsAndFreqs[startPos] = this.writeSegment;
		indexSegmentsAndFreqs[startPos + 1] = this.writeIndex;
		indexSegmentsAndFreqs[startPos + 2] = support;
		if (support != 0) {
			size++;
		}
		return this.transId;
	}
	
	@Override
	public void addItem(int item) {
		concatenated[this.writeSegment][this.writeIndex] = item;
		this.writeIndex++;
		if (this.writeIndex == concatenated[this.writeSegment].length) {
			this.writeSegment++;
			this.writeIndex = 0;
		}
	}

		
	
	
	
	
	@Override
	public Iterator<IterableTransaction> iterator() {
		return new Iter();
	}
	
	final private class Iter implements Iterator<IterableTransaction> {
		private int nextTid = -1;
		
		public Iter() {
			this.findNext();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public IterableTransaction next() {
			final int tid = this.nextTid;
			this.findNext();
			return new IterableTransaction() {
				private TIter iter = new TIter();

				@Override
				public TransactionIterator iterator() {
					iter.setTransaction(tid);
					return iter;
				}
			};
		}

		private void findNext() {
			while (true) {
				this.nextTid++;
				int nextPosStart = this.nextTid * 3;
				if (nextPosStart >= indexSegmentsAndFreqs.length) {
					this.nextTid = -1;
					return;
				}
				if (indexSegmentsAndFreqs[nextPosStart + 2] > 0) {
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return this.nextTid != -1;
		}
	}
	
	
	
	
	
	

	@Override
	public TransactionIterator getIterator() {
		return new TIter();
	}
	
	private final class TIter implements TransactionIterator {
		
		private int lastSegment;
		private int maxIndex;
		
		private int currentTsupport;
		
		private int segment;
		private int index;

		@Override
		public int next() {
			int next = concatenated[this.segment][this.index];
			this.index++;
			if (this.index == concatenated[this.segment].length) {
				this.segment++;
				this.index = 0;
			}
			return next;
		}

		@Override
		public boolean hasNext() {
			
			return this.segment < this.lastSegment || (this.segment == this.lastSegment && this.index < this.maxIndex);
		}

		@Override
		public int getTransactionSupport() {
			return this.currentTsupport;
		}

		@Override
		public void setTransaction(int transaction) {
			int startPos = transaction * 3;
			this.segment = indexSegmentsAndFreqs[startPos];
			this.index = indexSegmentsAndFreqs[startPos + 1];
			this.currentTsupport = indexSegmentsAndFreqs[startPos + 2];
			
			startPos += 3;
			if (startPos == indexSegmentsAndFreqs.length) {
				this.lastSegment = concatenated.length - 1;
				this.maxIndex = concatenated[this.lastSegment].length;
			} else {
				this.lastSegment = indexSegmentsAndFreqs[startPos];
				this.maxIndex = indexSegmentsAndFreqs[startPos + 1];
			}
		}

		@Override
		public void setTransactionSupport(int s) {
			throw new NotImplementedException();
		}
		
		@Override
		public void remove() {
			throw new NotImplementedException();
		}
	}
}
