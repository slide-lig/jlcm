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

import gnu.trove.iterator.TIntIterator;

/**
 * Stores transactions. Items in transactions are assumed to be sorted in
 * increasing order
 */
public abstract class TransactionsList implements Iterable<IterableTransaction>, Cloneable {

	@Override
	public TransactionsList clone() {
		try {
			return (TransactionsList) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	abstract public ReusableTransactionIterator getIterator();

	abstract public TIntIterator getIdIterator();

	abstract public TransactionsWriter getWriter();

	/**
	 * @return how many IterableTransaction are behind this object
	 */
	abstract public int size();

	public void compress(final int prefixEnd) {
		int[] sortList = new int[this.size()];
		TIntIterator idIter = this.getIdIterator();
		for (int i = 0; i < sortList.length; i++) {
			sortList[i] = idIter.next();
		}
		sort(sortList, 0, sortList.length, this.getIterator(), this.getIterator(), prefixEnd);
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
	private static void sort(final int[] array, final int start, final int end, final ReusableTransactionIterator it1,
			final ReusableTransactionIterator it2, int prefixEnd) {
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

	static private int merge(TransactionIterator t1, TransactionIterator t2, final int prefixEnd) {
		if (!t1.hasNext()) {
			if (!t2.hasNext() || t2.next() > prefixEnd) {
				t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
				t2.setTransactionSupport(0);
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
				t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
				t2.setTransactionSupport(0);
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
									t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
									t2.setTransactionSupport(0);
									return 0;
								}
							}
						} else {
							if (t2.hasNext()) {
								t2Item = t2.next();
								if (t2Item < prefixEnd) {
									return -1;
								} else {
									t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
									t2.setTransactionSupport(0);
									return 0;
								}
							} else {
								t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
								t2.setTransactionSupport(0);
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
						t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
						t2.setTransactionSupport(0);
						return 0;
					}
				} else {
					t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
					t2.setTransactionSupport(0);
					return 0;
				}
			} else {
				if (t1Item < t2Item) {
					t1.remove();
					if (t1.hasNext()) {
						t1Item = t1.next();
					} else {
						t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
						t2.setTransactionSupport(0);
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
						t1.setTransactionSupport(t1.getTransactionSupport() + t2.getTransactionSupport());
						t2.setTransactionSupport(0);
						return 0;
					}
				}
			}
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
		TransactionsList tl = new IntIndexedTransactionsList(16, 3);
		// TransactionsList tl = new ConcatenatedTransactionsList(16, 3);
		TransactionsWriter w = tl.getWriter();
		w.beginTransaction(Short.MAX_VALUE + 3);
		w.addItem(1);
		w.addItem(2);
		w.addItem(3);
		w.addItem(5);
		w.addItem(Short.MAX_VALUE * 2 - 2);
		w.addItem(Short.MAX_VALUE * 2);
		w.endTransaction();
		w.beginTransaction(1);
		w.addItem(1);
		w.addItem(2);
		w.addItem(5);
		w.addItem(Short.MAX_VALUE * 2 - 1);
		w.endTransaction();
		w.beginTransaction(3);
		w.addItem(1);
		w.addItem(2);
		w.addItem(3);
		w.addItem(5);
		w.addItem(Short.MAX_VALUE * 2 - 1);
		w.addItem(Short.MAX_VALUE * 2);
		w.endTransaction();
		System.out.println(tl);
		tl.compress(4);
		System.out.println(tl);
	}
}
