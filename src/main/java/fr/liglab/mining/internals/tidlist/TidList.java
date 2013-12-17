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

public abstract class TidList implements Cloneable {

	private int[] indexAndFreqs;

	public TidList(Counters c, int highestTidList) {
		this(c.distinctTransactionsCounts, highestTidList);
	}

	public TidList(final int[] lengths, int highestTidList) {
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

	
	
	public TIntIterator get(final int item) {
		int itemIndex = item << 1;
		if (itemIndex > this.indexAndFreqs.length || this.indexAndFreqs[itemIndex] == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		final int startPos = this.indexAndFreqs[itemIndex];
		final int length = this.indexAndFreqs[itemIndex + 1];
		return new TidIterator(length, startPos);
	}

	public TIntIterable getIterable(final int item) {
		return new TIntIterable() {

			@Override
			public TIntIterator iterator() {
				return get(item);
			}
		};
	}
	
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

	
	
	
	public interface TIntIterable {
		public TIntIterator iterator();
	}

	private class TidIterator implements TIntIterator {
		private int index = 0;
		private int length;
		private int startPos;

		TidIterator(int length, int startPos) {
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
			return this.index < this.length;
		}

		@Override
		public int next() {
			int res = read(this.startPos + this.index);
			this.index++;
			return res;
		}
	}
	
	

	public String toString(int[] items) {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		for (int item : items) {
			TIntIterator iter = this.get(item);
			if (first) {
				first = false;
			} else {
				sb.append("\n");
			}
			sb.append(item + " {");
			while (iter.hasNext()) {
				sb.append(iter.next() + ",");
			}
			sb.append("}");
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public TidList clone() {
		try {
			TidList c = (TidList) super.clone();
			c.indexAndFreqs = Arrays.copyOf(this.indexAndFreqs, this.indexAndFreqs.length);
			return c;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		int[] items = new int[] { 1, 2, 3, 5, Short.MAX_VALUE * 2 - 2, Short.MAX_VALUE * 2 - 1, Short.MAX_VALUE * 2 };
		int[] freqs = new int[Short.MAX_VALUE * 2 + 1];
		freqs[1] = 3;
		freqs[2] = 3;
		freqs[3] = 2;
		freqs[5] = 3;
		freqs[Short.MAX_VALUE * 2 - 2] = 1;
		freqs[Short.MAX_VALUE * 2 - 1] = 2;
		freqs[Short.MAX_VALUE * 2] = 2;
		TidList tl = new IntConsecutiveItemsConcatenatedTidList(freqs, Short.MAX_VALUE * 2);
		tl.addTransaction(1, 1);
		tl.addTransaction(1, Short.MAX_VALUE * 2 - 2);
		tl.addTransaction(1, Short.MAX_VALUE * 2);
		tl.addTransaction(2, 1);
		tl.addTransaction(2, Short.MAX_VALUE * 2 - 2);
		tl.addTransaction(2, Short.MAX_VALUE * 2);
		tl.addTransaction(3, 1);
		tl.addTransaction(3, 12);
		tl.addTransaction(5, 1);
		tl.addTransaction(5, 58);
		tl.addTransaction(5, 57887);
		tl.addTransaction(Short.MAX_VALUE * 2 - 2, 4);
		tl.addTransaction(Short.MAX_VALUE * 2 - 1, 18);
		tl.addTransaction(Short.MAX_VALUE * 2 - 1, 27);
		tl.addTransaction(Short.MAX_VALUE * 2, 37);
		tl.addTransaction(Short.MAX_VALUE * 2, 78);
		System.out.println(tl.toString(items));
	}
}
