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

public class UByteConsecutiveItemsConcatenatedTidList extends ConsecutiveItemsConcatenatedTidList {

	@SuppressWarnings("cast")
	public static boolean compatible(int maxTid) {
		return maxTid <= ((int) Byte.MAX_VALUE) - ((int) Byte.MIN_VALUE);
	}

	private byte[] array;

	@Override
	public TidList clone() {
		UByteConsecutiveItemsConcatenatedTidList o = (UByteConsecutiveItemsConcatenatedTidList) super.clone();
		o.array = Arrays.copyOf(this.array, this.array.length);
		return o;
	}

	@Override
	void allocateArray(int size) {
		this.array = new byte[size];
	}

	@Override
	void write(int position, int transaction) {
		if (transaction > Byte.MAX_VALUE) {
			transaction = -transaction + Byte.MAX_VALUE;
			if (transaction < Byte.MIN_VALUE) {
				throw new IllegalArgumentException(transaction + " too big for a byte");
			}
		}
		this.array[position] = (byte) transaction;
	}

	@SuppressWarnings("cast")
	@Override
	int read(int position) {
		if (this.array[position] >= 0) {
			return this.array[position];
		} else {
			return ((int) -this.array[position]) + ((int) Byte.MAX_VALUE);
		}
	}

	public UByteConsecutiveItemsConcatenatedTidList(Counters c, int highestItem) {
		super(c, highestItem);
	}

	public UByteConsecutiveItemsConcatenatedTidList(int[] lengths, int highestItem) {
		super(lengths, highestItem);
	}

}
