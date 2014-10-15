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

import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.omg.CORBA.IntHolder;

import fr.liglab.jlcm.util.ItemAndSupport;
import fr.liglab.jlcm.util.ItemsetsFactory;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * This class' constructor performs item counting over a transactions database,
 * and then gives access to various counters. It ignores items below the minimum
 * support. During recursions this allows the algorithm to choose what kind of
 * projections and filtering should be done, before instantiating the actual
 * projected dataset.
 * 
 * We're using arrays as Maps<Int,Int> , however they're not public as item
 * identifiers may have been renamed by dataset representation. By the way this
 * class is able to generate renamings (and applies them to itself by the way)
 * if you need to rename items in a future representation. You *MUST* handle
 * renaming after instantiation. See field reverseRenaming.
 */
public final class Counters implements Cloneable {

	/**
	 * Items occuring less than minSup times will be considered infrequent
	 */
	public final int minSupport;

	/**
	 * How many transactions are represented by the given dataset ?
	 */
	public final int transactionsCount;

	/**
	 * How many transactions have been counted (equals transactionsCount when
	 * all transactions have a weight of 1)
	 */
	public final int distinctTransactionsCount;

	/**
	 * Sum of given *filtered* transactions' lengths, ignoring their weight
	 */
	public final int distinctTransactionLengthSum;

	/**
	 * Support count, per item having a support count in [minSupport; 100% [
	 * Items having a support count below minSupport are considered infrequent,
	 * those at 100% belong to closure, for both supportCounts[i] = 0 - except
	 * if renaming happened, in which case such items no longer exists.
	 * 
	 * Indexes above maxFrequent should be considered valid.
	 */
	public final int[] supportCounts;

	/**
	 * For each item having a support count in [minSupport; 100% [ , gives how
	 * many distinct transactions contained this item. It's like supportCounts
	 * if all transactions have a weight equal to 1
	 * 
	 * Indexes above maxFrequent should be considered valid.
	 */
	public final int[] distinctTransactionsCounts;

	/**
	 * Items found to have a support count equal to transactionsCount (using IDs
	 * from given transactions) On renamed datasets you SHOULD NOT use
	 * getReverseRenaming to translate back these items, rather use parent's
	 * reverseRenaming (or none for the initial dataset)
	 */
	public final int[] closure;

	/**
	 * Counts how many items have a support count in [minSupport; 100% [
	 */
	public final int nbFrequents;

	/**
	 * Biggest item ID having a support count in [minSupport; 100% [
	 */
	protected int maxFrequent;

	/**
	 * This array allows another class to output the discovered closure using
	 * original items' IDs.
	 * 
	 * After instanciation this field *must* be set by one of these methods -
	 * reuseRenaming, the initial dataset's constructor (which also sets
	 * "renaming") - compressRenaming, useful when recompacting dataset in
	 * recursions
	 */
	protected int[] reverseRenaming;

	/**
	 * This field will be null EXCEPT if you're using the initial dataset's
	 * constructor (in which case it computes its absolute renaming by the way)
	 * OR if you called compressRenaming (in which case getRenaming will give
	 * back the same value)
	 * 
	 * It gives, for each original item ID, its new identifier. If it's negative
	 * it means the item should be filtered.
	 */
	protected int[] renaming = null;

	/**
	 * will be set to true if arrays have been compacted, ie. if supportCounts
	 * and distinctTransactionsCounts don't contain any zero.
	 */
	protected boolean compactedArrays = false;

	/**
	 * Exclusive index of the first item >= core_item in current base
	 */
	protected int maxCandidate;

	protected final int[] originalTidList;

	/**
	 * Does item counting over a projected dataset
	 * 
	 * @param minimumSupport
	 * @param transactions
	 *            extension's support
	 * @param extension
	 *            the item on which we're projecting - it won't appear in *any*
	 *            counter (not even 'closure')
	 * @param maxItem
	 *            biggest index among items to be found in "transactions"
	 */
	public Counters(int minimumSupport, Iterator<TransactionReader> transactions, int extension, final int maxItem) {

		this.renaming = null;
		this.minSupport = minimumSupport;
		this.supportCounts = new int[maxItem + 1];
		this.distinctTransactionsCounts = new int[maxItem + 1];

		// item support and transactions counting

		int weightsSum = 0;
		int transactionsCount = 0;

		TIntArrayList originalTransIds = new TIntArrayList();
		IntHolder originalId = new IntHolder();

		while (transactions.hasNext()) {
			TransactionReader transaction = transactions.next();
			int weight = transaction.getTransactionSupport();

			if (weight > 0) {
				final TIntArrayList originalIds = transaction.getTransactionOriginalId(originalId);
				if (originalIds == null) {
					originalTransIds.add(originalId.value);
				} else {
					originalTransIds.addAll(originalIds);
				}

				if (transaction.hasNext()) {
					weightsSum += weight;
					transactionsCount++;
				}

				while (transaction.hasNext()) {
					int item = transaction.next();
					if (item <= maxItem) {
						this.supportCounts[item] += weight;
						this.distinctTransactionsCounts[item]++;
					}
				}
			}
		}

		this.originalTidList = originalTransIds.toArray();

		this.transactionsCount = weightsSum;
		this.distinctTransactionsCount = transactionsCount;

		// ignored items
		this.supportCounts[extension] = 0;
		this.distinctTransactionsCounts[extension] = 0;
		this.maxCandidate = extension;

		// item filtering and final computations : some are infrequent, some
		// belong to closure

		ItemsetsFactory closureBuilder = new ItemsetsFactory();
		int remainingDistinctTransLengths = 0;
		int remainingFrequents = 0;
		int biggestItemID = 0;

		for (int i = 0; i < this.supportCounts.length; i++) {
			if (this.supportCounts[i] < minimumSupport) {
				this.supportCounts[i] = 0;
				this.distinctTransactionsCounts[i] = 0;
			} else if (this.supportCounts[i] == this.transactionsCount) {
				closureBuilder.add(i);
				this.supportCounts[i] = 0;
				this.distinctTransactionsCounts[i] = 0;
			} else {
				biggestItemID = Math.max(biggestItemID, i);
				remainingFrequents++;
				remainingDistinctTransLengths += this.distinctTransactionsCounts[i];
			}
		}

		this.closure = closureBuilder.get();
		this.distinctTransactionLengthSum = remainingDistinctTransLengths;
		this.nbFrequents = remainingFrequents;
		this.maxFrequent = biggestItemID;
	}

	/**
	 * Does item counting over an initial dataset : it will only ignore
	 * infrequent items, and it doesn't know what's biggest item ID. IT ALSO
	 * IGNORES TRANSACTIONS WEIGHTS ! (assuming it's 1 everywhere) /!\ It will
	 * perform an absolute renaming : items are renamed (and, likely,
	 * re-ordered) by decreasing support count. For instance 0 will be the most
	 * frequent item.
	 * 
	 * Indexes in arrays will refer items' new names, except for closure.
	 * 
	 * @param minimumSupport
	 * @param transactions
	 */
	Counters(int minimumSupport, Iterator<TransactionReader> transactions) {
		this.minSupport = minimumSupport;

		TIntIntHashMap supportsMap = new TIntIntHashMap();
		int biggestItemID = 0;

		// item support and transactions counting

		int transactionsCounter = 0;

		TIntArrayList originalTransIds = new TIntArrayList();
		IntHolder originalId = new IntHolder();

		while (transactions.hasNext()) {
			TransactionReader transaction = transactions.next();
			transactionsCounter++;

			final TIntArrayList originalIds = transaction.getTransactionOriginalId(originalId);
			if (originalIds == null) {
				originalTransIds.add(originalId.value);
			} else {
				originalTransIds.addAll(originalIds);
			}

			while (transaction.hasNext()) {
				int item = transaction.next();
				biggestItemID = Math.max(biggestItemID, item);
				supportsMap.adjustOrPutValue(item, 1, 1);
			}
		}

		this.originalTidList = originalTransIds.toArray();

		this.transactionsCount = transactionsCounter;
		this.distinctTransactionsCount = transactionsCounter;
		this.renaming = new int[biggestItemID + 1];
		Arrays.fill(this.renaming, -1);

		// item filtering and final computations : some are infrequent, some
		// belong to closure

		final PriorityQueue<ItemAndSupport> renamingHeap = new PriorityQueue<ItemAndSupport>();
		ItemsetsFactory closureBuilder = new ItemsetsFactory();

		TIntIntIterator iterator = supportsMap.iterator();

		while (iterator.hasNext()) {
			iterator.advance();
			final int item = iterator.key();
			final int supportCount = iterator.value();

			if (supportCount == this.transactionsCount) {
				closureBuilder.add(item);
			} else if (supportCount >= minimumSupport) {
				renamingHeap.add(new ItemAndSupport(item, supportCount));
			} // otherwise item is infrequent : its renaming is already -1, ciao
		}

		this.closure = closureBuilder.get();
		this.nbFrequents = renamingHeap.size();
		this.maxFrequent = this.nbFrequents - 1;
		this.maxCandidate = this.maxFrequent + 1;

		this.supportCounts = new int[this.nbFrequents];
		this.distinctTransactionsCounts = new int[this.nbFrequents];
		this.reverseRenaming = new int[this.nbFrequents];
		int remainingSupportsSum = 0;

		ItemAndSupport entry = renamingHeap.poll();
		int newItemID = 0;

		while (entry != null) {
			final int item = entry.item;
			final int support = entry.support;

			this.renaming[item] = newItemID;
			this.reverseRenaming[newItemID] = item;

			this.supportCounts[newItemID] = support;
			this.distinctTransactionsCounts[newItemID] = support;

			remainingSupportsSum += support;

			entry = renamingHeap.poll();
			newItemID++;
		}

		this.compactedArrays = true;
		this.distinctTransactionLengthSum = remainingSupportsSum;
	}

	private Counters(int minSupport, int transactionsCount, int distinctTransactionsCount,
			int distinctTransactionLengthSum, int[] supportCounts, int[] distinctTransactionsCounts, int[] closure,
			int nbFrequents, int maxFrequent, int[] reverseRenaming, int[] renaming, boolean compactedArrays,
			int maxCandidate, int[] originalTidList) {
		super();
		this.minSupport = minSupport;
		this.transactionsCount = transactionsCount;
		this.distinctTransactionsCount = distinctTransactionsCount;
		this.distinctTransactionLengthSum = distinctTransactionLengthSum;
		this.supportCounts = supportCounts;
		this.distinctTransactionsCounts = distinctTransactionsCounts;
		this.closure = closure;
		this.nbFrequents = nbFrequents;
		this.maxFrequent = maxFrequent;
		this.reverseRenaming = reverseRenaming;
		this.renaming = renaming;
		this.compactedArrays = compactedArrays;
		this.maxCandidate = maxCandidate;
		this.originalTidList = originalTidList;
	}

	@Override
	protected Counters clone() {
		return new Counters(minSupport, transactionsCount, distinctTransactionsCount, distinctTransactionLengthSum,
				Arrays.copyOf(supportCounts, supportCounts.length), Arrays.copyOf(distinctTransactionsCounts,
						distinctTransactionsCounts.length), Arrays.copyOf(closure, closure.length), nbFrequents,
				maxFrequent, Arrays.copyOf(reverseRenaming, reverseRenaming.length), Arrays.copyOf(renaming,
						renaming.length), compactedArrays, maxCandidate, Arrays.copyOf(originalTidList,
						originalTidList.length));
	}

	/**
	 * @return greatest frequent item's ID, which is also the greatest valid
	 *         index for arrays supportCounts and distinctTransactionsCounts
	 */
	public int getMaxFrequent() {
		return this.maxFrequent;
	}

	/**
	 * @return the renaming map from instantiation's base to current base
	 */
	public int[] getRenaming() {
		return this.renaming;
	}

	/**
	 * @return a translation from internal item indexes to dataset's original
	 *         indexes
	 */
	public int[] getReverseRenaming() {
		return this.reverseRenaming;
	}

	void reuseRenaming(int[] olderReverseRenaming) {
		this.reverseRenaming = olderReverseRenaming;
	}

	/**
	 * Will compress an older renaming, by removing infrequent items. Contained
	 * arrays (except closure) will refer new item IDs
	 * 
	 * @param olderReverseRenaming
	 *            reverseRenaming from the dataset that fed this Counter
	 * @return the translation from the old renaming to the compressed one
	 *         (gives -1 for removed items)
	 */
	public int[] compressRenaming(int[] olderReverseRenaming) {
		int[] renaming = new int[Math.max(olderReverseRenaming.length, this.supportCounts.length)];
		this.reverseRenaming = new int[this.nbFrequents];

		// we will always have newItemID <= item
		int newItemID = 0;
		int greatestBelowMaxCandidate = Integer.MIN_VALUE;

		for (int item = 0; item < this.supportCounts.length; item++) {
			if (this.supportCounts[item] > 0) {
				renaming[item] = newItemID;
				this.reverseRenaming[newItemID] = olderReverseRenaming[item];

				this.distinctTransactionsCounts[newItemID] = this.distinctTransactionsCounts[item];
				this.supportCounts[newItemID] = this.supportCounts[item];

				if (item < this.maxCandidate) {
					greatestBelowMaxCandidate = newItemID;
				}

				newItemID++;
			} else {
				renaming[item] = -1;
			}
		}

		this.maxCandidate = greatestBelowMaxCandidate + 1;
		Arrays.fill(renaming, this.supportCounts.length, renaming.length, -1);

		this.maxFrequent = newItemID - 1;
		this.compactedArrays = true;

		this.renaming = renaming;

		return renaming;
	}

	public int getMaxCandidate() {
		return this.maxCandidate;
	}

	/**
	 * Notice: enumerated item IDs are in local base, use this.reverseRenaming
	 * 
	 * @return a thread-safe iterator over frequent items (in ascending order)
	 */
	public ExtensionsIterator getExtensionsIterator() {
		return new ExtensionsIterator(this.maxCandidate);
	}

	/**
	 * Thread-safe iterator over frequent items (ie. those having a support
	 * count in [minSup, 100%[)
	 */
	public final class ExtensionsIterator {
		private final AtomicInteger index;
		private final int max;

		/**
		 * will provide an iterator on frequent items (in increasing order) in
		 * [0,to[
		 */
		public ExtensionsIterator(final int to) {
			this.index = new AtomicInteger(0);
			this.max = to;
		}

		/**
		 * @return -1 if iterator is finished
		 */
		public int next() {
			if (compactedArrays) {
				final int nextIndex = this.index.getAndIncrement();
				if (nextIndex < this.max) {
					return nextIndex;
				} else {
					return -1;
				}
			} else {
				while (true) {
					final int nextIndex = this.index.getAndIncrement();
					if (nextIndex < this.max) {
						if (supportCounts[nextIndex] > 0) {
							return nextIndex;
						}
					} else {
						return -1;
					}
				}
			}
		}

		public int peek() {
			return this.index.get();
		}

		public int last() {
			return this.max;
		}
	}
}
