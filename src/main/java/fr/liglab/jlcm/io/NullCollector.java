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

package fr.liglab.jlcm.io;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import fr.liglab.jlcm.internals.ExplorationStep;

/**
 * The collector that doesn't care at all about outputting
 */
public final class NullCollector extends PatternsWriter {

	protected AtomicInteger collectedCount = new AtomicInteger(0);
	protected AtomicLong collectedLength = new AtomicLong(0);

	@Override
	public void collect(int support, int[] pattern, int length, int[] originalTransIds) {
		this.collectedCount.incrementAndGet();
		this.collectedLength.addAndGet(length);
	}

	@Override
	public void collect(final ExplorationStep state) {
		this.collectedCount.incrementAndGet();
		this.collectedLength.addAndGet(state.pattern.length);
	}

	@Override
	public long close() {
		return this.collectedCount.get();
	}

	public int getAveragePatternLength() {
		if (this.collectedCount.get() == 0) {
			return 0;
		} else {
			return (int) (this.collectedLength.get() / this.collectedCount.get());
		}
	}
}
