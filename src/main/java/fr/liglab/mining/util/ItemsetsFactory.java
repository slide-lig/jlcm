/*
	This file is part of jLCM - see https://github.com/martinkirch/jlcm/
	
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


package fr.liglab.mining.util;

import gnu.trove.list.array.TIntArrayList;

/**
 * Itemsets and patterns are represented by classic integer arrays
 * Aside static utility methods, instanciate it to create arrays without knowing their length beforehand
 */
public class ItemsetsFactory {
	
	// default constructor FTW
	
	protected TIntArrayList buffer = new TIntArrayList();
	protected int capacity = 50;
	
	/**
	 * If you're going big and have an estimation of future array's size...
	 */
	public void ensureCapacity(final int c) {
		buffer.ensureCapacity(c);
	}
	
	public void add(final int i) {
		buffer.add(i);
	}
	
	/**
	 * Resets the builder by the way
	 * @return an array containing latest items added.
	 */
	public int[] get() {
		if (capacity < buffer.size()) {
			capacity = buffer.size();
		}
		int[] res = buffer.toArray();
		buffer.clear(capacity);
		return res;
	}

	public boolean isEmpty() {
		return buffer.isEmpty();
	}
	
	/////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * @return a new array concatenating each of its arguments
	 */
	public static int[] extend(final int[] pattern, final int extension, final int[] closure) {
		if (closure == null) {
			return extend(pattern, extension);
		}
		
		int[] extended = new int[pattern.length + closure.length + 1];
		
		System.arraycopy(pattern, 0, extended, 0, pattern.length);
		extended[pattern.length] = extension;
		System.arraycopy(closure, 0, extended, pattern.length + 1, closure.length);

		return extended;
	}

	/**
	 * @return a new array concatenating each of its arguments with their contents renamed EXCEPT for 'pattern' !
	 */
	public static int[] extendRename(final int[] closure, final int extension, final int[] pattern, 
			final int[] renaming) {
		
		int[] extended = new int[pattern.length + closure.length + 1];
		
		for (int i = 0; i < closure.length; i++) {
			extended[i] = renaming[closure[i]];
		}
		
		extended[closure.length] = renaming[extension];
		System.arraycopy(pattern, 0, extended, closure.length + 1, pattern.length);
		
		return extended;
	}
	
	public static int[] extend(final int[] pattern, final int extension, final int[] closure, final int[] ignoreItems) {
		if (ignoreItems == null) {
			return extend(pattern, extension, closure);
		}
		
		int[] extended = new int[pattern.length + closure.length + 1 + ignoreItems.length];

		System.arraycopy(pattern, 0, extended, 0, pattern.length);
		extended[pattern.length] = extension;
		System.arraycopy(closure, 0, extended, pattern.length + 1, closure.length);
		System.arraycopy(ignoreItems, 0, extended, pattern.length + 1 + closure.length, ignoreItems.length);
		System.arraycopy(closure, 0, extended, pattern.length+1, closure.length);
		
		return extended;
	}

	/**
	 * @return a new array concatenating each of its arguments
	 */
	public static int[] extend(final int[] closure, final int extension) {
		int[] extended = new int[closure.length + 1];
		
		System.arraycopy(closure, 0, extended, 0, closure.length);
		extended[closure.length] = extension;
		
		return extended;
	}
}
