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


package fr.liglab.mining.internals;

import fr.liglab.mining.internals.Dataset.TransactionsIterable;
import fr.liglab.mining.internals.Selector.WrongFirstParentException;
import fr.liglab.mining.io.FileReader;
import fr.liglab.mining.util.ItemsetsFactory;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Represents an LCM recursion step. Its also acts as a Dataset factory.
 */
public final class ExplorationStep implements Cloneable {

	public static boolean verbose = false;
	public static boolean ultraVerbose = false;

	public final static String KEY_VIEW_SUPPORT_THRESHOLD = "toplcm.threshold.view";
	public final static String KEY_LONG_TRANSACTIONS_THRESHOLD = "toplcm.threshold.long";

	/**
	 * @see longTransactionsMode
	 */
	static int LONG_TRANSACTION_MODE_THRESHOLD = Integer.parseInt(System.getProperty(KEY_LONG_TRANSACTIONS_THRESHOLD,
			"2000"));

	/**
	 * When projecting on a item having a support count above
	 * VIEW_SUPPORT_THRESHOLD%, projection will be a DatasetView
	 */
	static double VIEW_SUPPORT_THRESHOLD = Double.parseDouble(System.getProperty(KEY_VIEW_SUPPORT_THRESHOLD, "0.15"));

	/**
	 * When set to true we stick to a complete LCMv2 implementation, with
	 * predictive prefix-preservation tests and compressions at all steps.
	 * Setting this to false is better when mining top-k-per-item patterns.
	 */
	public static boolean LCM_STYLE = false;

	/**
	 * closure of parent's pattern UNION extension
	 */
	public final int[] pattern;

	/**
	 * Extension item that led to this recursion step. Already included in
	 * "pattern".
	 */
	public final int core_item;

	public final Dataset dataset;

	public final Counters counters;

	/**
	 * Selectors chain - may be null when empty
	 */
	protected Selector selectChain;

	protected final FrequentsIterator candidates;

	/**
	 * When an extension fails first-parent test, it ends up in this map. Keys
	 * are non-first-parent items associated to their actual first parent.
	 */
	private final TIntIntHashMap failedFPTests;

	private final boolean predictiveFPTestMode;

	/**
	 * Start exploration on a dataset contained in a file.
	 * 
	 * @param minimumSupport
	 * @param path
	 *            to an input file in ASCII format. Each line should be a
	 *            transaction containing space-separated item IDs.
	 */
	public ExplorationStep(int minimumSupport, String path) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		this.predictiveFPTestMode = false;

		FileReader reader = new FileReader(path);
		this.counters = new Counters(minimumSupport, reader);
		reader.close(this.counters.renaming);

		this.pattern = this.counters.closure;

		this.dataset = new Dataset(this.counters, reader);

		this.candidates = this.counters.getExtensionsIterator();

		this.failedFPTests = new TIntIntHashMap();
	}

	private ExplorationStep(int[] pattern, int core_item, Dataset dataset, Counters counters, Selector selectChain,
			FrequentsIterator candidates, TIntIntHashMap failedFPTests, boolean predictiveFPTestMode) {
		super();
		this.pattern = pattern;
		this.core_item = core_item;
		this.dataset = dataset;
		this.counters = counters;
		this.selectChain = selectChain;
		this.candidates = candidates;
		this.failedFPTests = failedFPTests;
		this.predictiveFPTestMode = predictiveFPTestMode;
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

					Counters candidateCounts = new Counters(this.counters.minSupport, support.iterator(), candidate,
							this.dataset.getIgnoredItems(), this.counters.maxFrequent);

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
			this.predictiveFPTestMode = false;
		} else {
			this.failedFPTests = new TIntIntHashMap();

			if (parent.selectChain == null) {
				this.selectChain = null;
			} else {
				this.selectChain = parent.selectChain.copy();
			}

			// ! \\ From here, order is important

			if (parent.predictiveFPTestMode) {
				this.predictiveFPTestMode = true;
			} else {
				final int averageLen = candidateCounts.distinctTransactionLengthSum
						/ candidateCounts.distinctTransactionsCount;

				this.predictiveFPTestMode = LCM_STYLE || averageLen > LONG_TRANSACTION_MODE_THRESHOLD;
				if (this.predictiveFPTestMode) {
					this.selectChain = new FirstParentTest(this.selectChain);
				}
			}

			// indeed, instantiateDataset is influenced by longTransactionsMode
			this.dataset = instanciateDataset(parent, support);

			// and intanciateDataset may choose to trigger some renaming in
			// counters
			this.candidates = this.counters.getExtensionsIterator();

		}
	}

	private Dataset instanciateDataset(ExplorationStep parent, TransactionsIterable support) {
		final double supportRate = this.counters.distinctTransactionsCount
				/ (double) parent.dataset.getStoredTransactionsCount();

		if (!this.predictiveFPTestMode && (supportRate) > VIEW_SUPPORT_THRESHOLD) {
			return new DatasetView(parent.dataset, this.counters, support, this.core_item);
		} else {
			final int[] renaming = this.counters.compressRenaming(parent.counters.getReverseRenaming());

			TransactionsRenamingDecorator filtered = new TransactionsRenamingDecorator(support.iterator(), renaming);

			final int tidsLimit = this.predictiveFPTestMode ? Integer.MAX_VALUE : this.counters.getMaxCandidate() + 1;
			try {
				Dataset dataset = new Dataset(this.counters, filtered, tidsLimit);
				if (LCM_STYLE) {
					dataset.compress(this.core_item);
				}

				return dataset;
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("WAT core_item = " + this.core_item);
				e.printStackTrace();
				System.exit(1);
			}

			return null;
		}
	}

	public int getFailedFPTest(final int item) {
		synchronized (this.failedFPTests) {
			return this.failedFPTests.get(item);
		}
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

	public int getCatchedWrongFirstParentCount() {
		if (this.failedFPTests == null) {
			return 0;
		} else {
			return this.failedFPTests.size();
		}
	}

	public ExplorationStep copy() {
		return new ExplorationStep(pattern, core_item, dataset.clone(), counters.clone(), selectChain, candidates,
				failedFPTests, predictiveFPTestMode);
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
