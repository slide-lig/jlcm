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


package fr.liglab.mining.io;

import java.util.Arrays;

/**
 * a PatternsCollector decorator : it will sort items in transactions before
 * transmitting them to the enclosed PatternsCollector
 */
public class PatternSortCollector implements PatternsCollector {

	protected final PatternsCollector decorated;

	public PatternSortCollector(PatternsCollector wrapped) {
		this.decorated = wrapped;
	}

	public void collect(final int support, final int[] pattern) {
		int[] sorted = Arrays.copyOf(pattern, pattern.length);
		Arrays.sort(sorted);
		this.decorated.collect(support, sorted);
	}

	public long close() {
		return this.decorated.close();
	}

	public int getAveragePatternLength() {
		return this.decorated.getAveragePatternLength();
	}

}
