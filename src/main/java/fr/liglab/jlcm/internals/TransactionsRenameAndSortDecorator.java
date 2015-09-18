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

import java.util.Arrays;
import java.util.Iterator;

import fr.liglab.jlcm.internals.TransactionReader;

/**
 * Decorates a transactions iterator : transactions are filtered (and, maybe, rebased) 
 * as they're read.
 */
final class TransactionsRenameAndSortDecorator implements Iterator<TransactionReader> {

	protected final Iterator<TransactionReader> wrapped;
	protected final int[] rebasing;
	protected CopyReader currentReader = new CopyReader();
	protected CopyReader nextReader = new CopyReader();

	/**
	 * @param filtered
	 * @param rebasing items having -1 as a value will be filtered
	 */
	public TransactionsRenameAndSortDecorator(Iterator<TransactionReader> filtered, int[] rebasing) {
		this.wrapped = filtered;
		this.rebasing = rebasing;
		this.preRead();
	}
	
	private void preRead() {
		this.nextReader.findNext(this.wrapped, this.rebasing);
		if (!this.nextReader.hasNext()) {
			this.nextReader = null;
		}
	}
	
	@Override
	public boolean hasNext() {
		return this.nextReader != null;
	}

	@Override
	public TransactionReader next() {
		CopyReader tmp = this.nextReader;
		this.nextReader = this.currentReader;
		this.currentReader = tmp;
		this.preRead();
		return this.currentReader;
	}

	@Override
	public final void remove() {
		this.wrapped.remove();
	}
	
	private static class CopyReader implements TransactionReader {
		private int i;
		private int transactionSupport;
		private int length;
		protected int[] buffer = new int[1024];
		
		public CopyReader() {}
		
		void findNext(Iterator<TransactionReader> source, int[] rebasing) {
			this.i = 0;
			while(source.hasNext()) {
				int j = 0;
				TransactionReader transaction = source.next();
				while(transaction.hasNext()) {
					final int rebased = rebasing[transaction.next()];
					if (rebased >= 0) {
						this.buffer[j++] = rebased;
						if (j==this.buffer.length) {
							this.extendBuffer();
						}
					}
				}
				if (j>0) {
					this.length = j;
					Arrays.sort(this.buffer, 0, this.length);
					this.transactionSupport = transaction.getTransactionSupport();
					return;
				}
			}
			this.length = 0;
		}

		public int getTransactionSupport() {
			return this.transactionSupport;
		}
		public int next() {
			return this.buffer[this.i++];
		}
		public boolean hasNext() {
			return this.i < this.length;
		}
		
		private void extendBuffer() {
			int[] extended = new int[this.buffer.length*2];
			System.arraycopy(this.buffer, 0, extended, 0, this.buffer.length);
			this.buffer = extended;
		}
	}
}
