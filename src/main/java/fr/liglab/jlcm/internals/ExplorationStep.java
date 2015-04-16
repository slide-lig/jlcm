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
import java.util.Calendar;

import fr.liglab.jlcm.internals.Counters.ExtensionsIterator;
import fr.liglab.jlcm.internals.Dataset.TransactionsIterable;
import fr.liglab.jlcm.internals.Selector.WrongFirstParentException;
import fr.liglab.jlcm.io.FileReader;
import fr.liglab.jlcm.io.FileReaderWithTransId;
import fr.liglab.jlcm.util.ItemsetsFactory;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Represents an LCM recursion step. Its also acts as a Dataset factory.
 */
public final class ExplorationStep implements Cloneable {

	public static boolean verbose = false;
	public static boolean ultraVerbose = false;

	public final ExplorationStep parent;

	/**
	 * Initialized with the given/parent's threshold
	 * Set it to another value if you want to over/under-filter when calling next() 
	 */
	public int childrenThreshold;
	
	/**
	 * closure of parent's pattern UNION extension
	 */
	public final int[] pattern;

	/**
	 * Extension item that led to this recursion step (internal ID) Already
	 * included in "pattern".
	 */
	public final int core_item;

	public final Dataset dataset;

	public final Counters counters;

	/**
	 * Selectors chain - may be null when empty
	 */
	protected Selector selectChain;

	protected final ExtensionsIterator candidates;

	/**
	 * When an extension fails first-parent test, it ends up in this map. Keys
	 * are non-first-parent items associated to their actual first parent.
	 */
	private final TIntIntHashMap failedFPTests;

	/**
	 * Start exploration on a dataset contained in a file.
	 * 
	 * @param minimumSupport
	 * @param path
	 *            to an input file in ASCII format. Each line should be a
	 *            transaction containing space-separated item IDs.
	 */
	public ExplorationStep(int minimumSupport, String path) {
		this.parent = null;
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		this.childrenThreshold = minimumSupport;

		FileReader reader = new FileReader(path);
		this.counters = new Counters(minimumSupport, reader);
		reader.close(this.counters.renaming);

		this.pattern = this.counters.closure;

		this.dataset = new Dataset(this.counters, reader);

		this.candidates = this.counters.getExtensionsIterator();

		this.failedFPTests = new TIntIntHashMap();
	}

	public ExplorationStep(int minimumSupport, String transactionsFile, int[] ids) {
		this.parent = null;
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;

		FileReaderWithTransId reader = new FileReaderWithTransId(transactionsFile, ids);
		this.counters = new Counters(minimumSupport, reader);
		reader.close(this.counters.renaming);

		this.pattern = this.counters.closure;

		this.dataset = new Dataset(this.counters, reader);

		this.candidates = this.counters.getExtensionsIterator();

		this.failedFPTests = new TIntIntHashMap();
	}

	/**
	 * Start exploration on an abstract dataset, using an absolute frequency 
	 * threshold
	 */
	public ExplorationStep(int minimumSupport, Iterable<TransactionReader> source) {
		this.parent = null;
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		this.childrenThreshold = minimumSupport;
		this.counters = new Counters(minimumSupport, source.iterator());
		this.pattern = this.counters.closure;
		TransactionsRenameAndSortDecorator filtered = new TransactionsRenameAndSortDecorator(source.iterator(),
				this.counters.renaming);
		this.dataset = new Dataset(this.counters, filtered);
		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
	}

	/**
	 * Start exploration on an abstract dataset, using a relative frequency 
	 * threshold
	 */
	public ExplorationStep(double minimumSupport, Iterable<TransactionReader> source) {
		this.parent = null;
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		this.counters = new Counters(minimumSupport, source.iterator());
		this.childrenThreshold = this.counters.minSupport;
		this.pattern = this.counters.closure;
		TransactionsRenameAndSortDecorator filtered = new TransactionsRenameAndSortDecorator(source.iterator(), this.counters.renaming);
		this.dataset = new Dataset(this.counters, filtered);
		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
	}
	
	private ExplorationStep(ExplorationStep parent, int childrenThreshold, int[] pattern, int core_item, Dataset dataset, Counters counters, Selector selectChain,
			ExtensionsIterator candidates, TIntIntHashMap failedFPTests) {
		super();
		this.childrenThreshold = childrenThreshold;
		this.parent = parent;
		this.pattern = pattern;
		this.core_item = core_item;
		this.dataset = dataset;
		this.counters = counters;
		this.selectChain = selectChain;
		this.candidates = candidates;
		this.failedFPTests = failedFPTests;
	}

	/**
	 * Finds an extension for current pattern in current dataset and returns the
	 * corresponding ExplorationStep (extensions are enumerated by ascending
	 * item IDs - in internal rebasing) Returns null when all valid extensions
	 * have been generated
	 */
	public ExplorationStep next() {
		if (this.candidates == null) {
			return null;
		}

		while (true) {
			int candidate = this.candidates.next();

			if (candidate < 0) {
				return null;
			}

			try {
				if (this.selectChain == null || this.selectChain.select(candidate, this)) {
					TransactionsIterable support = this.dataset.getSupport(candidate);

					// System.out.println("extending "+Arrays.toString(this.pattern)+
					// " with "+
					// candidate+" ("+this.counters.getReverseRenaming()[candidate]+")");

					Counters candidateCounts = new Counters(this.childrenThreshold, support.iterator(),
							candidate, this.counters.maxFrequent);

					int greatest = Integer.MIN_VALUE;
					for (int i = 0; i < candidateCounts.closure.length; i++) {
						if (candidateCounts.closure[i] > greatest) {
							greatest = candidateCounts.closure[i];
						}
					}

					if (greatest > candidate) {
						throw new WrongFirstParentException(candidate, greatest);
					}

					// instanciateDataset may choose to compress renaming - if
					// not, at least it's set for now.
					candidateCounts.reuseRenaming(this.counters.reverseRenaming);

					return new ExplorationStep(this, candidate, candidateCounts, support);
				}
			} catch (WrongFirstParentException e) {
				addFailedFPTest(e.extension, e.firstParent);
			}
		}
	}

	/**
	 * Instantiate state for a valid extension.
	 * 
	 * @param parent
	 * @param extension
	 *            a first-parent extension from parent step
	 * @param candidateCounts
	 *            extension's counters from parent step
	 * @param support
	 *            previously-computed extension's support
	 */
	protected ExplorationStep(ExplorationStep parent, int extension, Counters candidateCounts,
			TransactionsIterable support) {
		
		this.childrenThreshold = candidateCounts.minSupport;
		this.parent = parent;
		this.core_item = extension;
		this.counters = candidateCounts;
		int[] reverseRenaming = parent.counters.reverseRenaming;

		if (verbose) {
			if (parent.pattern.length == 0 || ultraVerbose) {
				System.err
						.format("{\"time\":\"%1$tY/%1$tm/%1$td %1$tk:%1$tM:%1$tS\",\"thread\":%2$d,\"pattern\":%3$s,\"extension_internal\":%4$d,\"extension\":%5$d}\n",
								Calendar.getInstance(), Thread.currentThread().getId(),
								Arrays.toString(parent.pattern), extension, reverseRenaming[extension]);
			}
		}

		this.pattern = ItemsetsFactory
				.extendRename(candidateCounts.closure, extension, parent.pattern, reverseRenaming);

		if (this.counters.nbFrequents == 0 || this.counters.distinctTransactionsCount == 0) {
			this.candidates = null;
			this.failedFPTests = null;
			this.selectChain = null;
			this.dataset = null;
		} else {
			this.failedFPTests = new TIntIntHashMap();

			if (parent.selectChain == null) {
				this.selectChain = new FirstParentTest(null);
			} else {
				this.selectChain = parent.selectChain.copy();
			}

			this.dataset = instanciateDataset(parent.counters, support);
			this.candidates = this.counters.getExtensionsIterator();
		}
	}

	private Dataset instanciateDataset(Counters parentCounters, TransactionsIterable support) {

		final int[] renaming = this.counters.compressRenaming(parentCounters.getReverseRenaming());

		TransactionsRenamingDecorator filtered = new TransactionsRenamingDecorator(support.iterator(), renaming);

		try {
			Dataset instance = new Dataset(this.counters, filtered, Integer.MAX_VALUE);
			instance.compress(this.counters.maxCandidate);
			return instance;

		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("WAT core_item = " + this.core_item);
			e.printStackTrace();
			System.exit(1);
		}

		return null;
	}

	public int getFailedFPTest(final int item) {
		synchronized (this.failedFPTests) {
			return this.failedFPTests.get(item);
		}
	}

	public int[] getOriginalSupportIds() {
		return this.counters.originalTidList;
	}

	private void addFailedFPTest(final int item, final int firstParent) {
		synchronized (this.failedFPTests) {
			this.failedFPTests.put(item, firstParent);
		}
	}

	public void appendSelector(Selector s) {
		if (this.selectChain == null) {
			this.selectChain = s;
		} else {
			this.selectChain = this.selectChain.append(s);
		}
	}

	public int getCaughtWrongFirstParentCount() {
		if (this.failedFPTests == null) {
			return 0;
		}
		return this.failedFPTests.size();
	}

	public ExplorationStep copy() {
		return new ExplorationStep(parent, childrenThreshold, pattern, core_item, dataset.clone(), counters.clone(), selectChain, candidates, failedFPTests);
	}

	public Progress getProgression() {
		return new Progress();
	}

	public final class Progress {
		public final int current;
		public final int last;

		protected Progress() {
			this.current = candidates.peek();
			this.last = candidates.last();
		}
	}

}
