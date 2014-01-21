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


package fr.liglab.mining.tests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.liglab.mining.internals.ExplorationStep;
import fr.liglab.mining.io.PatternsCollector;

/**
 * Firstly, fill expected patterns with expectCollect()
 * Then invoke LCM with this collector
 * 
 * a RuntimeException is thrown :
 * - if an unexpected pattern appears
 * - if one (or more) expected pattern(s) hasn't been seen on close() invocation
 * 
 * You should also assertTrue(thisCollector.isEmpty()) at the end 
 */
public class StubPatternsCollector implements PatternsCollector {
	
	protected static Map<Integer, Set<Set<Integer>>> expected = new TreeMap<Integer, Set<Set<Integer>>>();
	protected long collected = 0;
	protected long collectedLength = 0;
	
	public void expectCollect(Integer support, Integer... patternItems) {
		Set<Set<Integer>> supportExpectation = null;
		
		if (expected.containsKey(support)){
			supportExpectation = expected.get(support);
		} else {
			supportExpectation = new HashSet<Set<Integer>>();
			expected.put(support, supportExpectation);
		}
		
		supportExpectation.add(new TreeSet<Integer>( Arrays.asList(patternItems)));
	}
	
	public boolean isEmpty() {
		return expected.isEmpty();
	}

	public void collect(final ExplorationStep state) {
		final int support = state.counters.transactionsCount;
		final int[] pattern = state.pattern;
		
		Set<Integer> p = new TreeSet<Integer>();
		for (int item : pattern) {
			p.add(item);
		}
		
		if (expected.containsKey(support)) {
			Set<Set<Integer>> expectations = expected.get(support);
			
			if (expectations.contains(p)) {
				expectations.remove(p);
				if (expectations.isEmpty()) {
					expected.remove(support);
				}
				this.collected++;
				this.collectedLength += pattern.length;
				return;
			}
		}
		
		throw new RuntimeException("Unexpected support/pattern : " + p.toString() + " , support=" + support);
	}

	public long close() {
		if (!isEmpty()) {
			StringBuilder builder = new StringBuilder();
			builder.append("Expected pattern(s) not found :\n");
			
			for (Integer support: expected.keySet()) {
				Set<Set<Integer>> supportExpectation = expected.get(support);
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
