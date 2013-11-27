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

import fr.liglab.mining.PLCM;
import fr.liglab.mining.PLCM.PLCMCounters;
import fr.liglab.mining.internals.tidlist.TidList;
import gnu.trove.iterator.TIntIterator;

/**
 * A stateless Selector that may throw WrongFirstParentException
 * 
 * Allows to perform first-parent test BEFORE performing item counting for a
 * candidate extension
 */
final class FirstParentTest extends Selector {

	@Override
	protected PLCMCounters getCountersKey() {
		return PLCMCounters.FirstParentTestRejections;
	}

	FirstParentTest() {
		super();
	}

	FirstParentTest(Selector follower) {
		super(follower);
	}

	@Override
	protected Selector copy(Selector newNext) {
		return new FirstParentTest(newNext);
	}

	private boolean isAincludedInB(final TIntIterator aIt, final TIntIterator bIt) {
		int tidA = 0;
		int tidB = 0;

		while (aIt.hasNext() && bIt.hasNext()) {
			tidA = aIt.next();
			tidB = bIt.next();

			while (tidB < tidA && bIt.hasNext()) {
				tidB = bIt.next();
			}

			if (tidB > tidA) {
				return false;
			}
		}

		return tidA == tidB && !aIt.hasNext();
	}

	/**
	 * returns true or throws a WrongFirstParentException
	 */
	@Override
	protected boolean allowExploration(int extension, ExplorationStep state) throws WrongFirstParentException {

		/**
		 * FIXME maybe fast prefix-preserving state as described by Uno et al.
		 * may work with DatasetView
		 */
		if (state.dataset instanceof DatasetView) {
			throw new IllegalArgumentException("FPtest can only be done on Dataset");
		}

		final int[] supportCounts = state.counters.supportCounts;
		final TidList occurrencesLists = state.dataset.tidLists;

		final int candidateSupport = supportCounts[extension];

		for (int i = state.counters.maxFrequent; i > extension; i--) {
			if (supportCounts[i] >= candidateSupport) {
				TIntIterator candidateOccurrences = occurrencesLists.get(extension);
				final TIntIterator iOccurrences = occurrencesLists.get(i);
				if (isAincludedInB(candidateOccurrences, iOccurrences)) {
					((PLCM.PLCMThread) Thread.currentThread()).counters[PLCMCounters.FirstParentTestRejections
							.ordinal()]++;
					throw new WrongFirstParentException(extension, i);
				}
			}
		}

		return true;
	}
}
