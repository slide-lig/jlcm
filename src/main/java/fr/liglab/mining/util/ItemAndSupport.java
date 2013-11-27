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


package fr.liglab.mining.util;

/**
 * A < B <=> A.support > B.support
 */
public class ItemAndSupport implements Comparable<ItemAndSupport> {
	
	public int item;
	public int support;
	
	public ItemAndSupport(int i, int s) {
		this.item = i;
		this.support = s;
	}

	/**
	 *  Returns a negative integer, zero, or a positive integer 
	 *  as this object's support is less than, equal to, or 
	 *  greater than the specified object's support. 
	 */
	public int compareTo(ItemAndSupport other) {
		if (other.support == this.support) {
			return this.item - other.item;
		} else {
			return other.support - this.support;
		}
	}

}
