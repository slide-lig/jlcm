/*
	This file is part of jLCM - see https://github.com/martinkirch/jlcm/
	
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


package fr.liglab.jlcm.io;

import fr.liglab.jlcm.internals.ExplorationStep;

/**
 * A PatternsCollector providing a more low-level method.
 * Implemented by classes that won't ever change the pattern.
 */
public abstract class PatternsWriter implements PatternsCollector {
	
	/**
	 * Record the sub-array pattern[0:length[
	 * @param support Pattern's support count
	 * @param pattern Pattern container
	 * @param length should be between 1 and pattern.length
	 */
	public abstract void collect(final int support, final int[] pattern, int length);
	
	public void collect(final ExplorationStep state) {
		this.collect(state.counters.transactionsCount, state.pattern, state.pattern.length);
	}
}
