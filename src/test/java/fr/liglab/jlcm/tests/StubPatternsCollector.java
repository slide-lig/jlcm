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

package fr.liglab.jlcm.tests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.liglab.jlcm.io.PatternsWriter;

/**
 * Firstly, fill expected patterns with expectCollect() Then invoke LCM with
 * this collector
 * 
 * a RuntimeException is thrown : - if an unexpected pattern appears - if one
 * (or more) expected pattern(s) hasn't been seen on close() invocation
 * 
 * You should also assertTrue(thisCollector.isEmpty()) at the end
 */
public class StubPatternsCollector extends PatternsWriter {

	protected Map<Integer, Set<Set<Integer>>> expected = new TreeMap<Integer, Set<Set<Integer>>>();
	protected long collected = 0;
	protected long collectedLength = 0;

	@Override
	public void collect(int support, int[] pattern, int length, int[] originalTransIds) {
		Set<Integer> p = new TreeSet<Integer>();
		for (int i = 0; i < length; i++) {
			p.add(pattern[i]);
		}

		if (this.expected.containsKey(support)) {
			Set<Set<Integer>> expectations = this.expected.get(support);

			if (expectations.contains(p)) {
				expectations.remove(p);
				if (expectations.isEmpty()) {
					this.expected.remove(support);
				}
				this.collected++;
				this.collectedLength += pattern.length;
				return;
			}
		}

		throw new RuntimeException("Unexpected support/pattern : " + p.toString() + " , support=" + support);
	}

	public void expectCollect(Integer support, Integer... patternItems) {
		Set<Set<Integer>> supportExpectation = null;

		if (this.expected.containsKey(support)) {
			supportExpectation = this.expected.get(support);
		} else {
			supportExpectation = new HashSet<Set<Integer>>();
			this.expected.put(support, supportExpectation);
		}

		supportExpectation.add(new TreeSet<Integer>(Arrays.asList(patternItems)));
	}

	public boolean isEmpty() {
		return this.expected.isEmpty();
	}

	public long close() {
		if (!isEmpty()) {
			StringBuilder builder = new StringBuilder();
			builder.append("Expected pattern(s) not found :\n");

			for (Integer support : this.expected.keySet()) {
				Set<Set<Integer>> supportExpectation = this.expected.get(support);
				for (Set<Integer> pattern : supportExpectation) {
					builder.append(pattern.toString());
					builder.append(", support = ");
					builder.append(support);
					builder.append("\n");
				}
			}

			throw new RuntimeException(builder.toString());
		}

		return this.collected;
	}

	public int getAveragePatternLength() {
		if (this.collected == 0) {
			return 0;
		} else {
			return (int) (this.collectedLength / this.collected);
		}
	}
}
