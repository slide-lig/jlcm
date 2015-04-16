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

package fr.liglab.jlcm.internals;

import java.util.Iterator;

import org.omg.CORBA.IntHolder;

import fr.liglab.jlcm.PLCM;
import fr.liglab.jlcm.PLCM.PLCMCounters;
import fr.liglab.jlcm.internals.tidlist.IntConsecutiveItemsConcatenatedTidList;
import fr.liglab.jlcm.internals.tidlist.TidList;
import fr.liglab.jlcm.internals.tidlist.TidList.TIntIterable;
import fr.liglab.jlcm.internals.tidlist.UShortConsecutiveItemsConcatenatedTidList;
import fr.liglab.jlcm.internals.transactions.IntIndexedTransactionsList;
import fr.liglab.jlcm.internals.transactions.TransactionIterator;
import fr.liglab.jlcm.internals.transactions.TransactionsList;
import fr.liglab.jlcm.internals.transactions.UShortIndexedTransactionsList;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

/**
 * Stores transactions and does occurrence delivery
 */
public class Dataset implements Cloneable {

	protected final TransactionsList transactions;

	/**
	 * Associates to a (frequent) item its array of occurrences indexes in
	 * "concatenated"
	 * Transactions are added in the same order in all occurrences-arrays.
	 */
	protected final TidList tidLists;

	protected Dataset(TransactionsList transactions, TidList occurrences) {
		this.transactions = transactions;
		this.tidLists = occurrences;
	}

	@Override
	protected Dataset clone() {
		return new Dataset(this.transactions.clone(), this.tidLists.clone());
	}

	Dataset(Counters counters, final Iterator<TransactionReader> transactions) {
		this(counters, transactions, Integer.MAX_VALUE);
	}

	/**
	 * @param counters
	 * @param transactions
	 *            assumed to be filtered according to counters
	 * @param tidListBound
	 *            - highest item (exclusive) which will have a tidList. set to
	 *            MAX_VALUE when using predictive pptest.
	 */
	Dataset(Counters counters, final Iterator<TransactionReader> transactions, int tidListBound) {

		int maxTransId;

		// if (UByteIndexedTransactionsList.compatible(counters)) {
		// this.transactions = new UByteIndexedTransactionsList(counters);
		// maxTransId = UByteIndexedTransactionsList.getMaxTransId(counters);
		// } else
		if (UShortIndexedTransactionsList.compatible(counters)) {
			this.transactions = new UShortIndexedTransactionsList(counters);
			maxTransId = UShortIndexedTransactionsList.getMaxTransId(counters);
		} else {
			this.transactions = new IntIndexedTransactionsList(counters);
			maxTransId = IntIndexedTransactionsList.getMaxTransId(counters);
		}

		// if (UByteConsecutiveItemsConcatenatedTidList.compatible(maxTransId))
		// {
		// this.tidLists = new
		// UByteConsecutiveItemsConcatenatedTidList(counters, tidListBound);
		// } else
		if (UShortConsecutiveItemsConcatenatedTidList.compatible(maxTransId)) {
			this.tidLists = new UShortConsecutiveItemsConcatenatedTidList(counters, tidListBound);
		} else {
			this.tidLists = new IntConsecutiveItemsConcatenatedTidList(counters, tidListBound);
		}

		this.transactions.startWriting();
		IntHolder originalId = new IntHolder();
		while (transactions.hasNext()) {
			TransactionReader transaction = transactions.next();
			if (transaction.getTransactionSupport() != 0 && transaction.hasNext()) {
				final TIntArrayList originalIds = transaction.getTransactionOriginalId(originalId);
				int transId;
				if (originalIds == null) {
					transId = this.transactions.beginTransaction(transaction.getTransactionSupport(), originalId.value);
				} else {
					transId = this.transactions.beginTransaction(transaction.getTransactionSupport(), originalIds);
				}

				while (transaction.hasNext()) {
					final int item = transaction.next();
					this.transactions.addItem(item);

					if (item < tidListBound) {
						this.tidLists.addTransaction(item, transId);
					}
				}
			}
		}
	}

	public void compress(int coreItem) {
		((PLCM.PLCMThread) Thread.currentThread()).counters[PLCMCounters.TransactionsCompressions.ordinal()]++;
		this.transactions.compress(coreItem);
	}

	/**
	 * @return how many transactions (ignoring their weight) are stored behind
	 *         this dataset
	 */
	int getStoredTransactionsCount() {
		return this.transactions.size();
	}

	public TransactionsIterable getSupport(int item) {
		return new TransactionsIterable(this.tidLists.getIterable(item));
	}

	public final class TransactionsIterable implements Iterable<TransactionReader> {
		final TIntIterable tids;

		public TransactionsIterable(TIntIterable tidList) {
			this.tids = tidList;
		}

		@Override
		public Iterator<TransactionReader> iterator() {
			return new TransactionsIterator(this.tids.iterator());
		}
	}

	protected final class TransactionsIterator implements Iterator<TransactionReader> {

		protected final TIntIterator it;
		private final TransactionIterator transIter;

		public TransactionsIterator(TIntIterator tids) {
			this.it = tids;
			this.transIter = transactions.getIterator();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TransactionReader next() {
			this.transIter.setTransaction(this.it.next());
			return this.transIter;
		}

		@Override
		public boolean hasNext() {
			return this.it.hasNext();
		}
	}

	@Override
	public String toString() {
		return this.transactions.toString();
	}
}
