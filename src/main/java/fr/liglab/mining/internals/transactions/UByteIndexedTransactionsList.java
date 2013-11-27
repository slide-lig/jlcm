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

import fr.liglab.mining.internals.Counters;

public final class UByteIndexedTransactionsList extends IndexedTransactionsList {
	private byte[] concatenated;

	@SuppressWarnings("cast")
	public static boolean compatible(Counters c) {
		return c.getMaxFrequent() <= ((int) Byte.MAX_VALUE) - ((int) Byte.MIN_VALUE) - 1;
	}

	public static int getMaxTransId(Counters c) {
		return c.distinctTransactionsCount - 1;
	}

	public UByteIndexedTransactionsList(Counters c) {
		this(c.distinctTransactionLengthSum, c.distinctTransactionsCount);
	}

	public UByteIndexedTransactionsList(int transactionsLength, int nbTransactions) {
		super(nbTransactions);
		this.concatenated = new byte[transactionsLength];
	}

	@Override
	public IndexedReusableIterator getIterator() {
		return new TransIter();
	}

	@Override
	void writeItem(int item) {
		// Byte.MIN_VALUE is for empty
		if (item > Byte.MAX_VALUE) {
			item = -item + Byte.MAX_VALUE;
			if (item <= Byte.MIN_VALUE) {
				throw new IllegalArgumentException(item + " too big for a byte");
			}
		}
		this.concatenated[this.writeIndex] = (byte) item;
		this.writeIndex++;
	}

	@Override
	public TransactionsList clone() {
		UByteIndexedTransactionsList o = (UByteIndexedTransactionsList) super.clone();
		o.concatenated = Arrays.copyOf(this.concatenated, this.concatenated.length);
		return o;
	}

	private final class TransIter extends BasicTransIter {

		@Override
		boolean isNextPosValid() {
			return concatenated[this.nextPos] != Byte.MIN_VALUE;
		}

		@Override
		void removePosVal() {
			concatenated[this.pos] = Byte.MIN_VALUE;
		}

		@SuppressWarnings("cast")
		@Override
		int getPosVal() {
			if (concatenated[this.pos] >= 0) {
				return concatenated[this.pos];
			} else {
				return ((int) -concatenated[this.pos]) + ((int) Byte.MAX_VALUE);
			}
		}

	}

}
