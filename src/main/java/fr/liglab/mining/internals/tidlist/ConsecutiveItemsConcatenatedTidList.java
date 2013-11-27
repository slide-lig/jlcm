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


package fr.liglab.mining.internals.tidlist;

import java.util.Arrays;

import fr.liglab.mining.internals.Counters;
import gnu.trove.iterator.TIntIterator;

public abstract class ConsecutiveItemsConcatenatedTidList extends TidList {

	private int[] indexAndFreqs;

	public ConsecutiveItemsConcatenatedTidList(Counters c, int highestTidList) {
		this(c.distinctTransactionsCounts, highestTidList);
	}

	public ConsecutiveItemsConcatenatedTidList(final int[] lengths, int highestTidList) {
		int startPos = 0;
		int top = Math.min(highestTidList, lengths.length);
		this.indexAndFreqs = new int[top * 2];
		for (int i = 0; i < top; i++) {
			int itemIndex = i << 1;
			if (lengths[i] > 0) {
				this.indexAndFreqs[itemIndex] = startPos;
				startPos += lengths[i];
			} else {
				this.indexAndFreqs[itemIndex] = -1;
			}
		}
		this.allocateArray(startPos);
	}

	abstract void allocateArray(int size);

	@Override
	public TidList clone() {
		ConsecutiveItemsConcatenatedTidList o = (ConsecutiveItemsConcatenatedTidList) super.clone();
		o.indexAndFreqs = Arrays.copyOf(this.indexAndFreqs, this.indexAndFreqs.length);
		return o;
	}

	@Override
	public TIntIterator get(final int item) {
		int itemIndex = item << 1;
		if (itemIndex > this.indexAndFreqs.length || this.indexAndFreqs[itemIndex] == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		final int startPos = this.indexAndFreqs[itemIndex];
		final int length = this.indexAndFreqs[itemIndex + 1];
		return new TidIterator(length, startPos);
	}

	@Override
	public TIntIterable getIterable(final int item) {
		return new TIntIterable() {

			@Override
			public TIntIterator iterator() {
				return get(item);
			}
		};
	}

	@Override
	public void addTransaction(int item, int transaction) {
		int itemIndex = item << 1;
		if (itemIndex > this.indexAndFreqs.length || this.indexAndFreqs[itemIndex] == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		int start = this.indexAndFreqs[itemIndex];
		int index = this.indexAndFreqs[itemIndex + 1];
		this.write(start + index, transaction);
		this.indexAndFreqs[itemIndex + 1]++;
	}

	abstract void write(int position, int transaction);

	abstract int read(int position);

	private class TidIterator implements TIntIterator {
		private int index = 0;
		private int length;
		private int startPos;

		private TidIterator(int length, int startPos) {
			super();
			this.length = length;
			this.startPos = startPos;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNext() {
			return this.index < length;
		}

		@Override
		public int next() {
			int res = read(startPos + index);
			this.index++;
			return res;
		}
	}
}
