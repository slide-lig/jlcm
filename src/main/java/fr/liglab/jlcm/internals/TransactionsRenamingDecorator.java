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


package fr.liglab.jlcm.internals;

import java.util.Iterator;

import fr.liglab.jlcm.internals.TransactionReader;

/**
 * Decorates a transactions iterator : transactions are filtered (and, maybe, rebased) 
 * as they're read.
 */
class TransactionsRenamingDecorator implements Iterator<TransactionReader> {

	protected final Iterator<TransactionReader> wrapped;
	protected final int[] rebasing;
	protected FilteredTransaction instance;

	/**
	 * @param filtered
	 * @param rebasing items having -1 as a value will be filtered
	 */
	public TransactionsRenamingDecorator(Iterator<TransactionReader> filtered, int[] rebasing) {
		this.wrapped = filtered;
		this.instance = null;
		this.rebasing = rebasing;
	}

	@Override
	public final boolean hasNext() {
		return this.wrapped.hasNext();
	}

	@Override
	public TransactionReader next() {
		if (this.instance == null) {
			this.instance = new FilteredTransaction(this.wrapped.next());
		} else {
			this.instance.reset(this.wrapped.next());
		}

		return this.instance;
	}

	@Override
	public final void remove() {
		this.wrapped.remove();
	}

	protected class FilteredTransaction implements TransactionReader {

		protected TransactionReader wrapped;
		protected int next;
		protected boolean hasNext;

		public FilteredTransaction(TransactionReader filtered) {
			this.reset(filtered);
		}

		public void reset(TransactionReader filtered) {
			this.wrapped = filtered;
			this.next = 0;

			this.findNext();
		}

		protected void findNext() {
			while (this.wrapped.hasNext()) {
				this.next = rebasing[this.wrapped.next()];
				if (this.next != -1 ) {
					this.hasNext = true;
					return;
				}
			}

			this.hasNext = false;
		}

		@Override
		public final int getTransactionSupport() {
			return this.wrapped.getTransactionSupport();
		}

		@Override
		public final int next() {
			final int value = this.next;
			this.findNext();
			return value;
		}

		@Override
		public final boolean hasNext() {
			return this.hasNext;
		}
	}
}
