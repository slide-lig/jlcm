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


package fr.liglab.mining.internals.transactions;

import java.util.Arrays;

import org.omg.CORBA.IntHolder;

import fr.liglab.mining.internals.Counters;

public class VIntIndexedTransactionsList extends IndexedTransactionsList {
	private byte[] concatenated;

	public static boolean compatible(Counters c) {
		return true;
	}

	public static int getMaxTransId(Counters c) {
		return c.distinctTransactionsCount - 1;
	}

	static private int getVIntSize(int val) {
		if (val < 0) {
			return 5;
		} else if (val < 0x00000080) {
			return 1;
		} else if (val < 0x00004000) {
			return 2;
		} else if (val < 0x00200000) {
			return 3;
		} else if (val < 0x10000000) {
			return 4;
		} else {
			return 5;
		}
	}

	public VIntIndexedTransactionsList(Counters c) {
		this(c.distinctTransactionsCounts, c.distinctTransactionsCount);
	}

	public VIntIndexedTransactionsList(int[] distinctItemFreq, int nbTransactions) {
		super(nbTransactions);
		int size = 0;
		for (int i = 0; i < distinctItemFreq.length; i++) {
			// add 1 because we use the value 0 for empty
			size += distinctItemFreq[i] * getVIntSize(i + 1);
		}
		this.concatenated = new byte[size];
	}

	private void eraseLastVal(int pos) {
		// there is at least 1 byte in a vint, and it is positive
		int erase = pos - 1;
		concatenated[erase] = 0;
		// all other bytes of the vint are negative, stop when we see a positive
		for (erase--; concatenated[erase] < 0; erase--) {
			concatenated[erase] = 0;
		}
	}

	private int readVInt(IntHolder pos) {
		byte b = this.concatenated[pos.value];
		pos.value++;
		if (b >= 0) {
			return b;
		} else {
			int res = (b & 0x7F);
			int shift = 7;
			while (true) {
				b = this.concatenated[pos.value];
				pos.value++;
				if (b > 0) {
					res = res | (b << shift);
					break;
				} else {
					res = res | ((b & 0x7F) << shift);
					shift += 7;
				}
			}
			return res;
		}
	}

	private void writeVInt(int val) {
		while (true) {
			if (val >= 0 && val < 0x00000080) {
				this.concatenated[this.writeIndex] = (byte) val;
				// System.out.println("encoding "
				// + String.format("%X", (byte) value));
				this.writeIndex++;
				break;
			} else {
				// System.out.println("encoding "
				// + String.format("%X", ((byte) value)) + " into "
				// + String.format("%X", (((byte) value) | 0x80)));
				this.concatenated[this.writeIndex] = (byte) (((byte) val) | 0x80);
				val = val >>> 7;
				this.writeIndex++;
			}
		}
	}

	@Override
	public IndexedReusableIterator getIterator() {
		return new TransIter();
	}

	@Override
	void writeItem(int item) {
		// O is for empty
		item++;
		this.writeVInt(item);
	}

	@Override
	public TransactionsList clone() {
		VIntIndexedTransactionsList o = (VIntIndexedTransactionsList) super.clone();
		o.concatenated = Arrays.copyOf(this.concatenated, this.concatenated.length);
		return o;
	}

	private class TransIter extends IndexedReusableIterator {

		private int val;
		private IntHolder pos;
		private int end;
		private int deleteIndex;

		public TransIter() {
		}

		@Override
		public void set(int begin, int end) {
			this.pos = new IntHolder(begin);
			this.end = end;
			this.findNext();
		}

		private void findNext() {
			while (true) {
				if (this.pos.value == this.end) {
					this.val = -1;
					return;
				}
				this.deleteIndex = this.pos.value;
				this.val = readVInt(this.pos);
				if (this.val != 0) {
					return;
				}
			}
		}

		@Override
		public int next() {
			// because we saved 0 for empty
			int res = this.val - 1;
			this.findNext();
			return res;
		}

		@Override
		public boolean hasNext() {
			return this.val != -1;
		}

		@Override
		public void remove() {
			eraseLastVal(this.deleteIndex);
		}

	}

}
