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


package fr.liglab.mining.internals;

import fr.liglab.mining.PLCM;
import fr.liglab.mining.PLCM.PLCMCounters;

/**
 * Main class for chained exploration filters, implemented as an immutable
 * chained list.
 */
public abstract class Selector {

	private final Selector next;

	/**
	 * @param extension in state's local base
	 * @param state
	 * @return false if, at the given state, trying to extend the current
	 *         pattern with the given extension is useless
	 * @throws WrongFirstParentException
	 */
	abstract protected boolean allowExploration(int extension, ExplorationStep state) throws WrongFirstParentException;

	/**
	 * @return an instance of the same selector for another recursion
	 */
	abstract protected Selector copy(Selector newNext);

	/**
	 * @return which enum value from TopLCMCounters will be used to count this Selector's rejections, 
	 * or null if we don't want to count rejections from the implementing class
	 */
	abstract protected PLCMCounters getCountersKey();

	public Selector() {
		this.next = null;
	}

	protected Selector(Selector follower) {
		this.next = follower;
	}

	/**
	 * This one handles chained calls
	 * 
	 * @param extension
	 * @param state
	 * @return false if, at the given state, trying to extend the current
	 *         pattern with the given extension is useless
	 * @throws WrongFirstParentException
	 */
	final boolean select(int extension, ExplorationStep state) throws WrongFirstParentException {
		if (this.allowExploration(extension, state)) {
			return (this.next == null || this.next.select(extension, state));
		} else {
			PLCMCounters key = this.getCountersKey();
			if (key != null) {
				((PLCM.PLCMThread) Thread.currentThread()).counters[key.ordinal()]++;
			}
			return false;
		}
	}

	/**
	 * Note: prepending should simply be done by passing a chain at first
	 * selector's instantiation. Appends the given selector at the end of
	 * current list, and returns new list's head
	 */
	final Selector append(Selector s) {
		if (this.next == null) {
			return this.copy(s);
		} else {
			return this.copy(this.next.append(s));
		}
	}

	/**
	 * @return a new Selector chain for a new recursion
	 */
	final Selector copy() {
		return this.append(null);
	}
	
	final protected Selector getNext() {
		return this.next;
	}

	/**
	 * Thrown when a Selector finds that an extension won't be the first parent
	 * of its closed pattern (FirstParentTest should be the only one concerned)
	 */
	public static class WrongFirstParentException extends Exception {
		private static final long serialVersionUID = 2969583589161047791L;

		public final int firstParent;
		public final int extension;

		/**
		 * @param extension
		 *            the tested extension
		 * @param foundFirstParent
		 *            a item found in closure > extension
		 */
		public WrongFirstParentException(int exploredExtension, int foundFirstParent) {
			this.firstParent = foundFirstParent;
			this.extension = exploredExtension;
		}
	}
}
