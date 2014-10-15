/*
	This file is part of jLCM - see https://github.com/martinkirch/jlcm/
	
	Copyright 2013,2014 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, Sihem Amer-Yahia, Marie-Christine Rousset, Université Joseph Fourier and CNRS

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

package fr.liglab.jlcm.internals.transactions;

import fr.liglab.jlcm.internals.Counters;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;

import java.util.Arrays;
import java.util.Iterator;

import org.omg.CORBA.IntHolder;

/**
 * Stores transactions. Items in transactions are assumed to be sorted in
 * increasing order
 */
public abstract class TransactionsList implements Iterable<IterableTransaction>, Cloneable {

	int[] indexAndFreqs;
	private int size = 0;
	private int transId = -1;
	int writeIndex = 0;
	private int[] originalTransactionIds;
	private TIntObjectMap<TIntArrayList> multipleTransactionIds;

	public TransactionsList(Counters c) {
		this(c.distinctTransactionsCount);
	}

	public TransactionsList(int nbTransactions) {
		this.indexAndFreqs = new int[nbTransactions << 1];
		Arrays.fill(this.indexAndFreqs, -1);
		this.originalTransactionIds = new int[nbTransactions];
		Arrays.fill(this.originalTransactionIds, -1);
	}

	public TIntArrayList getOriginalId(int transId) {
		if (this.originalTransactionIds[transId] >= 0) {
			TIntArrayList a = new TIntArrayList(0);
			a.add(this.originalTransactionIds[transId]);
			return a;
		} else {
			return this.multipleTransactionIds.get(transId);
		}
	}

	public TIntArrayList getOriginalId(int transId, IntHolder h) {
		if (this.originalTransactionIds[transId] >= 0) {
			h.value = this.originalTransactionIds[transId];
			return null;
		} else {
			return this.multipleTransactionIds.get(transId);
		}
	}

	@Override
	public TransactionsList clone() {
		// TODO update clone with new fields
		try {
			TransactionsList o = (TransactionsList) super.clone();
			o.indexAndFreqs = Arrays.copyOf(this.indexAndFreqs, this.indexAndFreqs.length);
			return o;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return how many IterableTransaction are behind this object
	 */
	final public int size() {
		return this.size;
	}

	final public void startWriting() {
		this.transId = -1;
	}

	public abstract void addItem(int item);

	public int beginTransaction(int support, int originalTransactionId) {
		int newId = this.beginTransaction(support);
		this.originalTransactionIds[newId] = originalTransactionId;
		return newId;
	}

	public int beginTransaction(int support, TIntArrayList originalTransactionIds) {
		int newId = this.beginTransaction(support);
		this.originalTransactionIds[newId] = -1;
		if (this.multipleTransactionIds == null) {
			this.multipleTransactionIds = new TIntObjectHashMap<TIntArrayList>();
		}
		this.multipleTransactionIds.put(newId, new TIntArrayList(originalTransactionIds));
		return newId;
	}

	private int beginTransaction(int support) {
		this.transId++;
		int startPos = this.transId << 1;
		this.indexAndFreqs[startPos] = TransactionsList.this.writeIndex;
		this.indexAndFreqs[startPos + 1] = support;
		if (support != 0) {
			this.size++;
		}
		return this.transId;
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
	public Iterator<IterableTransaction> iterator() {
		return new Iter();
	}

	abstract public IndexedReusableIterator getIterator();

	final public TIntIterator getIdIterator() {
		return new IdIter();
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
					positionIterator(p, this.iter);
					return this.iter;
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

	public void compress(final int prefixEnd) {
		int[] sortList = new int[this.size()];
		TIntIterator idIter = this.getIdIterator();
		for (int i = 0; i < sortList.length; i++) {
			sortList[i] = idIter.next();
		}
		this.sort(sortList, 0, sortList.length, this.getIterator(), this.getIterator(), prefixEnd);
		this.trimAllOriginalTransIdsLists();
	}

	/**
	 * This is NOT a standard quicksort. Transactions with same prefix as the
	 * pivot are left out the rest of the sort because they have been merged in
	 * the pivot. Consequence: in the recursion, there is some space between
	 * left sublist and right sublist (besides the pivot itself).
	 * 
	 * @param array
	 * @param start
	 * @param end
	 * @param it1
	 * @param it2
	 * @param prefixEnd
	 */
	private void sort(final int[] array, final int start, final int end, final IndexedReusableIterator it1,
			final IndexedReusableIterator it2, int prefixEnd) {
		if (start >= end - 1) {
			// size 0 or 1
			return;
		} else if (end - start == 2) {
			it1.setTransaction(array[start]);
			it2.setTransaction(array[start + 1]);
			merge(it1, it2, prefixEnd);
		} else {
			// pick pivot at the middle and put it at the end
			int pivotPos = start + ((end - start) / 2);
			int pivotVal = array[pivotPos];
			array[pivotPos] = array[end - 1];
			array[end - 1] = pivotVal;
			int insertInf = start;
			int insertSup = end - 2;
			for (int i = start; i <= insertSup;) {
				it1.setTransaction(pivotVal);
				it2.setTransaction(array[i]);
				int comp = merge(it1, it2, prefixEnd);
				if (comp < 0) {
					int valI = array[i];
					array[insertInf] = valI;
					insertInf++;
					i++;
				} else if (comp > 0) {
					int valI = array[i];
					array[i] = array[insertSup];
					array[insertSup] = valI;
					insertSup--;
				} else {
					i++;
				}
			}
			array[end - 1] = array[insertSup + 1];
			// Arrays.fill(array, insertInf, insertSup + 2, -1);
			array[insertSup + 1] = pivotVal;
			sort(array, start, insertInf, it1, it2, prefixEnd);
			sort(array, insertSup + 2, end, it1, it2, prefixEnd);
		}
	}

	// only use with iterators from the same TransactionList (because of
	// original transaction ids merge)
	private int merge(IndexedReusableIterator t1, IndexedReusableIterator t2, final int prefixEnd) {
		if (!t1.hasNext()) {
			if (!t2.hasNext() || t2.next() > prefixEnd) {
				doMerge(t1, t2);
				return 0;
			} else {
				return -1;
			}
		} else if (!t2.hasNext()) {
			if (t1.next() > prefixEnd) {
				t1.remove();
				while (t1.hasNext()) {
					t1.remove();
				}
				doMerge(t1, t2);
				return 0;
			} else {
				return 1;
			}
		}
		int t1Item = t1.next();
		int t2Item = t2.next();
		while (true) {
			if (t1Item < prefixEnd) {
				if (t2Item < prefixEnd) {
					if (t1Item != t2Item) {
						return t1Item - t2Item;
					} else {
						if (t1.hasNext()) {
							t1Item = t1.next();
							if (t2.hasNext()) {
								t2Item = t2.next();
								continue;
							} else {
								if (t1Item < prefixEnd) {
									return 1;
								} else {
									t1.remove();
									while (t1.hasNext()) {
										t1Item = t1.next();
										t1.remove();
									}
									doMerge(t1, t2);
									return 0;
								}
							}
						} else {
							if (t2.hasNext()) {
								t2Item = t2.next();
								if (t2Item < prefixEnd) {
									return -1;
								} else {
									doMerge(t1, t2);
									return 0;
								}
							} else {
								doMerge(t1, t2);
								return 0;
							}
						}
					}
				} else {
					return -1;
				}
			} else {
				if (t2Item < prefixEnd) {
					return 1;
				} else {
					break;
				}
			}
		}
		while (true) {
			if (t1Item == t2Item) {
				if (t1.hasNext()) {
					if (t2.hasNext()) {
						t1Item = t1.next();
						t2Item = t2.next();
						continue;
					} else {
						while (t1.hasNext()) {
							t1Item = t1.next();
							t1.remove();
						}
						doMerge(t1, t2);
						return 0;
					}
				} else {
					doMerge(t1, t2);
					return 0;
				}
			} else {
				if (t1Item < t2Item) {
					t1.remove();
					if (t1.hasNext()) {
						t1Item = t1.next();
					} else {
						doMerge(t1, t2);
						return 0;
					}
				} else {
					if (t2.hasNext()) {
						t2Item = t2.next();
					} else {
						t1.remove();
						while (t1.hasNext()) {
							t1Item = t1.next();
							t1.remove();
						}
						doMerge(t1, t2);
						return 0;
					}
				}
			}
		}
	}

	private final void doMerge(IndexedReusableIterator t1, IndexedReusableIterator t2) {
		t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
		t2.setTransactionSupport(0);
		if (this.multipleTransactionIds == null) {
			this.multipleTransactionIds = new TIntObjectHashMap<TIntArrayList>();
		}
		TIntArrayList l = multipleTransactionIds.get(t1.transNum);
		if (l == null) {
			l = new TIntArrayList();
			originalTransactionIds[t1.transNum] = -1;
			multipleTransactionIds.put(t1.transNum, l);
		}
		TIntArrayList otherL = multipleTransactionIds.remove(t2.transNum);
		if (otherL == null) {
			l.add(originalTransactionIds[t2.transNum]);
		} else {
			l.addAll(otherL);
		}
	}

	private final void trimAllOriginalTransIdsLists() {
		if (this.multipleTransactionIds != null) {
			this.multipleTransactionIds.forEachValue(new TObjectProcedure<TIntArrayList>() {
				@Override
				public boolean execute(TIntArrayList l) {
					l.trimToSize();
					return true;
				}
			});
		}
	}

	abstract class IndexedReusableIterator implements TransactionIterator {
		private int transNum;
		int pos;
		int nextPos;
		private int end;
		private boolean first;

		final void set(int begin, int end) {
			this.nextPos = begin - 1;
			this.end = end;
			this.first = true;
		}

		@Override
		public final void setTransaction(int transaction) {
			this.transNum = transaction;
			positionIterator(transaction, this);
		}

		@Override
		public final int getTransactionSupport() {
			return getTransSupport(this.transNum);
		}

		@Override
		public TIntArrayList getTransactionOriginalId() {
			return getOriginalId(this.transNum);
		}

		@Override
		public TIntArrayList getTransactionOriginalId(IntHolder h) {
			return getOriginalId(this.transNum, h);
		}

		@Override
		public final void setTransactionSupport(int s) {
			setTransSupport(this.transNum, s);
		}

		private void findNext() {
			while (true) {
				this.nextPos++;
				if (this.nextPos == this.end) {
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
			if (this.first) {
				this.first = false;
				findNext();
			}
			return this.nextPos != -1;
		}

		@Override
		public void remove() {
			this.removePosVal();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.size() + " transactions\n[");
		boolean first = true;
		for (IterableTransaction trans : this) {
			TransactionIterator iter = trans.iterator();
			if (first) {
				first = false;
			} else {
				sb.append("\n");
			}
			sb.append(iter.getTransactionSupport() + " {");
			while (iter.hasNext()) {
				sb.append(iter.next() + ",");
			}
			sb.append("}");
		}
		sb.append("]");
		return sb.toString();
	}

	public static void main(String[] args) {
		int[] freqs = new int[Short.MAX_VALUE * 2 + 1];
		freqs[1] = 3;
		freqs[2] = 3;
		freqs[3] = 2;
		freqs[5] = 3;
		freqs[Short.MAX_VALUE * 2 - 2] = 1;
		freqs[Short.MAX_VALUE * 2 - 1] = 2;
		freqs[Short.MAX_VALUE * 2] = 2;
		// TransactionsList tl = new VIntConcatenatedTransactionsList(3, freqs);
		IntIndexedTransactionsList tl = new IntIndexedTransactionsList(16, 3);
		// TransactionsList tl = new ConcatenatedTransactionsList(16, 3);
		tl.startWriting();
		tl.beginTransaction(Short.MAX_VALUE + 3, 47);
		tl.addItem(1);
		tl.addItem(2);
		tl.addItem(3);
		tl.addItem(5);
		tl.addItem(Short.MAX_VALUE * 2 - 2);
		tl.addItem(Short.MAX_VALUE * 2);
		tl.beginTransaction(1, 28);
		tl.addItem(1);
		tl.addItem(2);
		tl.addItem(5);
		tl.addItem(Short.MAX_VALUE * 2 - 1);
		tl.beginTransaction(3, 8);
		tl.addItem(1);
		tl.addItem(2);
		tl.addItem(3);
		tl.addItem(5);
		tl.addItem(Short.MAX_VALUE * 2 - 1);
		tl.addItem(Short.MAX_VALUE * 2);
		System.out.println(tl);
		tl.compress(4);
		System.out.println(tl);
	}
}
