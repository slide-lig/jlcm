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


package fr.liglab.jlcm.internals.transactions;

import java.util.Arrays;

import fr.liglab.jlcm.internals.Counters;

public final class UShortIndexedTransactionsList extends IndexedTransactionsList {
	char[] concatenated;

	public static boolean compatible(Counters c) {
		return c.distinctTransactionLengthSum < Integer.MAX_VALUE && 
				c.distinctTransactionsCount < (Integer.MAX_VALUE/2) && 
				c.getMaxFrequent() < Character.MAX_VALUE;
	}

	public static int getMaxTransId(Counters c) {
		return c.distinctTransactionsCount - 1;
	}

	public UShortIndexedTransactionsList(Counters c) {
		this((int) c.distinctTransactionLengthSum, c.distinctTransactionsCount);
	}

	public UShortIndexedTransactionsList(int transactionsLength, int nbTransactions) {
		super(nbTransactions);
		this.concatenated = new char[transactionsLength];
	}

	@Override
	public void addItem(int item) {
		// MAX_VALUE is for empty;
		if (item == Character.MAX_VALUE) {
			throw new IllegalArgumentException(item + " too big for a char");
		}
		this.concatenated[this.writeIndex] = (char) item;
		this.writeIndex++;
	}

	@Override
	public TransactionsList clone() {
		UShortIndexedTransactionsList o = (UShortIndexedTransactionsList) super.clone();
		o.concatenated = Arrays.copyOf(this.concatenated, this.concatenated.length);
		return o;
	}

	@Override
	public IndexedReusableIterator getIterator() {
		return new TransIter();
	}

	class TransIter extends IndexedReusableIterator {

		@Override
		boolean isNextPosValid() {
			return concatenated[this.nextPos] != Character.MAX_VALUE;
		}

		@Override
		void removePosVal() {
			concatenated[this.pos] = Character.MAX_VALUE;
		}

		@Override
		int getPosVal() {
			return concatenated[this.pos];
		}
	}
}
