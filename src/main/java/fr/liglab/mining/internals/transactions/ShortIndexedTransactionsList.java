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

public final class ShortIndexedTransactionsList extends IndexedTransactionsList {

	public static boolean compatible(Counters c) {
		return c.getMaxFrequent() <= Short.MAX_VALUE;
	}

	public static int getMaxTransId(Counters c) {
		return c.distinctTransactionsCount - 1;
	}

	private short[] concatenated;

	public ShortIndexedTransactionsList(Counters c) {
		this(c.distinctTransactionLengthSum, c.distinctTransactionsCount);
	}

	public ShortIndexedTransactionsList(int transactionsLength, int nbTransactions) {
		super(nbTransactions);
		this.concatenated = new short[transactionsLength];
	}

	@Override
	public IndexedReusableIterator getIterator() {
		return new TransIter();
	}

	@Override
	void writeItem(int item) {
		if (item > Short.MAX_VALUE) {
			throw new IllegalArgumentException(item + " too big for a short");
		}
		this.concatenated[this.writeIndex] = (short) item;
		this.writeIndex++;
	}

	@Override
	public TransactionsList clone() {
		ShortIndexedTransactionsList o = (ShortIndexedTransactionsList) super.clone();
		o.concatenated = Arrays.copyOf(this.concatenated, this.concatenated.length);
		return o;
	}

	private final class TransIter extends BasicTransIter {

		@Override
		boolean isNextPosValid() {
			return concatenated[this.nextPos] != -1;
		}

		@Override
		void removePosVal() {
			concatenated[this.pos] = -1;
		}

		@Override
		int getPosVal() {
			return concatenated[this.pos];
		}

	}

}
