/*
	This file is part of jLCM
	
	Copyright 2013 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, Sihem Amer-Yahia, Marie-Christine Rousset, Université Joseph Fourier and CNRS

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0
	 
	or see the LICENSE.txt file joined with this program.

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/


package fr.liglab.mining.internals.transactions;

import java.util.Arrays;
import java.util.Iterator;

import fr.liglab.mining.internals.Counters;
import gnu.trove.iterator.TIntIterator;

public abstract class IndexedTransactionsList extends TransactionsList {

	private int[] indexAndFreqs;
	int writeIndex = 0;
	private int size = 0;

	public IndexedTransactionsList(Counters c) {
		this(c.distinctTransactionsCount);
	}

	public IndexedTransactionsList(int nbTransactions) {
		this.indexAndFreqs = new int[nbTransactions << 1];
		Arrays.fill(this.indexAndFreqs, -1);
	}

	@Override
	public Iterator<IterableTransaction> iterator() {
		return new Iter();
	}

	final void positionIterator(int transaction, IndexedReusableIterator iter) {
		int startPos = transaction << 1;
		if (startPos >= this.indexAndFreqs.length || this.indexAndFreqs[startPos] == -1) {
			throw new IllegalArgumentException("transaction " + transaction + " does not exist");
		} else {
			int endPos = startPos + 2;
			int end;
			if (endPos < this.indexAndFreqs.length) {
				end = this.indexAndFreqs[endPos];
				if (end == -1) {
					end = this.writeIndex;
				}
			} else {
				end = this.writeIndex;
			}
			iter.set(this.indexAndFreqs[startPos], end);
		}
	}

	abstract class IndexedReusableIterator implements ReusableTransactionIterator {
		private int transNum;

		abstract void set(int begin, int end);

		@Override
		public final void setTransaction(int transaction) {
			this.transNum = transaction;
			positionIterator(transaction, this);
		}

		@Override
		public final int getTransactionSupport() {
			return getTransSupport(transNum);
		}

		@Override
		public final void setTransactionSupport(int s) {
			setTransSupport(this.transNum, s);
		}
	}

	abstract class BasicTransIter extends IndexedReusableIterator {
		int pos;
		int nextPos;
		private int end;
		private boolean first;

		@Override
		final void set(int begin, int end) {
			this.nextPos = begin - 1;
			this.end = end;
			first = true;
		}

		private void findNext() {
			while (true) {
				this.nextPos++;
				if (nextPos == this.end) {
					this.nextPos = -1;
					return;
				}
				if (isNextPosValid()) {
					return;
				}
			}
		}

		abstract boolean isNextPosValid();

		abstract void removePosVal();

		abstract int getPosVal();

		@Override
		public int next() {
			this.pos = this.nextPos;
			this.findNext();
			return getPosVal();
		}

		@Override
		public boolean hasNext() {
			if (first) {
				first = false;
				findNext();
			}
			return this.nextPos != -1;
		}

		@Override
		public void remove() {
			this.removePosVal();
		}

	}

	final int getTransSupport(int trans) {
		int startPos = trans << 1;
		return this.indexAndFreqs[startPos + 1];
	}

	final void setTransSupport(int trans, int s) {
		int startPos = trans << 1;
		if (s != 0 && this.indexAndFreqs[startPos + 1] == 0) {
			this.size++;
		} else if (s == 0 && this.indexAndFreqs[startPos + 1] != 0) {
			this.size--;
		}
		this.indexAndFreqs[startPos + 1] = s;
	}

	@Override
	abstract public IndexedReusableIterator getIterator();

	@Override
	final public TIntIterator getIdIterator() {
		return new IdIter();
	}

	@Override
	final public TransactionsWriter getWriter() {
		return new Writer();
	}

	@Override
	final public int size() {
		return this.size;
	}

	@Override
	public TransactionsList clone() {
		IndexedTransactionsList o = (IndexedTransactionsList) super.clone();
		o.indexAndFreqs = Arrays.copyOf(this.indexAndFreqs, this.indexAndFreqs.length);
		return o;
	}

	abstract void writeItem(int item);

	final private class Writer implements TransactionsWriter {
		private int transId = -1;

		@Override
		public int beginTransaction(int support) {
			this.transId++;
			int startPos = this.transId << 1;
			indexAndFreqs[startPos] = writeIndex;
			indexAndFreqs[startPos + 1] = support;
			if (support != 0) {
				size++;
			}
			return this.transId;
		}

		@Override
		public void addItem(int item) {
			writeItem(item);
		}

		@Override
		public void endTransaction() {
		}

	}

	final private class Iter implements Iterator<IterableTransaction> {
		private int pos;
		private int nextPos = -1;

		public Iter() {
			this.findNext();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public IterableTransaction next() {
			this.pos = this.nextPos;
			this.findNext();
			final int p = this.pos;
			return new IterableTransaction() {
				private IndexedReusableIterator iter = getIterator();

				@Override
				public TransactionIterator iterator() {
					positionIterator(p, iter);
					return iter;
				}
			};
		}

		private void findNext() {
			while (true) {
				this.nextPos++;
				int nextPosStart = this.nextPos << 1;
				if (nextPosStart >= indexAndFreqs.length || indexAndFreqs[nextPosStart] == -1) {
					this.nextPos = -1;
					return;
				}
				if (indexAndFreqs[nextPosStart + 1] > 0) {
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return this.nextPos != -1;
		}
	}

	final private class IdIter implements TIntIterator {
		private int pos;
		private int nextPos = -1;

		public IdIter() {
			this.findNext();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int next() {
			this.pos = this.nextPos;
			this.findNext();
			return this.pos;
		}

		private void findNext() {
			while (true) {
				this.nextPos++;
				int nextPosStart = this.nextPos << 1;
				if (nextPosStart >= indexAndFreqs.length || indexAndFreqs[nextPosStart] == -1) {
					this.nextPos = -1;
					return;
				}
				if (indexAndFreqs[nextPosStart + 1] > 0) {
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return this.nextPos != -1;
		}
	}

}
