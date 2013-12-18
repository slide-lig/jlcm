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


package fr.liglab.mining.internals;

import fr.liglab.mining.internals.FrequentsIterator;

/**
 * Decorates a FrequentsIterator : all items from the wrapped iterator will be renamed according 
 * to the map provided at instantiation.
 * 
 * This decorator won't check that items fit in renaming array.
 */
public class FrequentsIteratorRenamer implements FrequentsIterator {
	
	private final int[] renaming;
	private final FrequentsIterator wrapped;
	
	public FrequentsIteratorRenamer(final FrequentsIterator decorated, final int[] itemsRenaming) {
		this.renaming = itemsRenaming;
		this.wrapped = decorated;
	}
	
	@Override
	public int next() {
		final int next = this.wrapped.next();
		if (next >= 0) {
			return this.renaming[next];
		} else {
			return -1;
		}
	}

	@Override
	public int peek() {
		final int next = this.wrapped.peek();
		if (next >= 0) {
			return this.renaming[next];
		} else {
			return -1;
		}
	}

	@Override
	public int last() {
		return this.renaming[this.wrapped.last()];
	}
}
