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


package fr.liglab.jlcm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import fr.liglab.jlcm.internals.ExplorationStep;
import fr.liglab.jlcm.io.PatternsCollector;
import fr.liglab.jlcm.util.ProgressWatcherThread;

/**
 * LCM implementation, based on UnoAUA04 :
 * "An Efficient Algorithm for Enumerating Closed Patterns in Transaction Databases"
 * by Takeaki Uno el. al.
 */
public class PLCM {
	final List<PLCMThread> threads;
	private ProgressWatcherThread progressWatch;
	protected static long chrono;

	private final PatternsCollector collector;

	private final long[] globalCounters;
	
	public PLCM(PatternsCollector patternsCollector, int nbThreads) {
		this(patternsCollector, nbThreads, null);
	}
	
	public PLCM(PatternsCollector patternsCollector, int nbThreads, ProgressWatcherThread watch) {
		if (nbThreads < 1) {
			throw new IllegalArgumentException("nbThreads has to be > 0, given " + nbThreads);
		}
		this.collector = patternsCollector;
		this.threads = new ArrayList<PLCMThread>(nbThreads);
		this.createThreads(nbThreads);
		this.globalCounters = new long[PLCMCounters.values().length];
		this.progressWatch = watch;
	}
	
	/**
	 * Initial invocation
	 */
	public final void lcm(final ExplorationStep initState) {
		if (initState.pattern.length > 0) {
			this.collector.collect(initState);
		}

		this.initializeAndStartThreads(initState);

		if (this.progressWatch != null) {
			this.progressWatch.setInitState(initState);
			this.progressWatch.start();
		}
		
		for (PLCMThread t : this.threads) {
			try {
				t.join();
				for (int i = 0; i < t.counters.length; i++) {
					this.globalCounters[i] += t.counters[i];
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (this.progressWatch != null) {
			this.progressWatch.interrupt();
		}
	}
	
	void createThreads(int nbThreads) {
		for (int i = 0; i < nbThreads; i++) {
			this.threads.add(new PLCMThread(i));
		}
	}

	public final void collect(ExplorationStep step) {
		this.collector.collect(step);
	}

	void initializeAndStartThreads(final ExplorationStep initState) {
		for (PLCMThread t : this.threads) {
			t.init(initState);
			t.start();
		}
	}

	public Map<PLCMCounters, Long> getCounters() {
		HashMap<PLCMCounters, Long> map = new HashMap<PLCMCounters, Long>();

		PLCMCounters[] counters = PLCMCounters.values();

		for (int i = 0; i < this.globalCounters.length; i++) {
			map.put(counters[i], this.globalCounters[i]);
		}

		return map;
	}

	public String toString(Map<String, Long> additionalCounters) {
		StringBuilder builder = new StringBuilder();

		builder.append("{\"name\":\"PLCM\", \"threads\":");
		builder.append(this.threads.size());

		PLCMCounters[] counters = PLCMCounters.values();

		for (int i = 0; i < this.globalCounters.length; i++) {
			PLCMCounters counter = counters[i];

			builder.append(", \"");
			builder.append(counter.toString());
			builder.append("\":");
			builder.append(this.globalCounters[i]);
		}

		if (additionalCounters != null) {
			for (Entry<String, Long> entry : additionalCounters.entrySet()) {
				builder.append(", \"");
				builder.append(entry.getKey());
				builder.append("\":");
				builder.append(entry.getValue());
			}
		}

		builder.append('}');

		return builder.toString();
	}

	@Override
	public String toString() {
		return this.toString(null);
	}

	ExplorationStep stealJob(PLCMThread thief) {
		// here we need to readlock because the owner thread can write
		for (PLCMThread victim : this.threads) {
			if (victim != thief) {
				ExplorationStep e = stealJob(thief, victim);
				if (e != null) {
					return e;
				}
			}
		}
		return null;
	}

	static ExplorationStep stealJob(PLCMThread thief, PLCMThread victim) {
		victim.lock.readLock().lock();
		for (int stealPos = 0; stealPos < victim.stackedJobs.size(); stealPos++) {
			ExplorationStep sj = victim.stackedJobs.get(stealPos);
			ExplorationStep next = sj.next();

			if (next != null) {
				thief.init(sj);
				victim.lock.readLock().unlock();
				return next;
			}
		}
		victim.lock.readLock().unlock();
		return null;
	}

	/**
	 * Some classes in EnumerationStep may declare counters here. see references
	 * to PLCMCounters.counters
	 */
	public enum PLCMCounters {
		ExplorationStepInstances, ExplorationStepCaughtWrongFirstParents, FirstParentTestRejections, TransactionsCompressions
	}

	public class PLCMThread extends Thread {
		public final long[] counters;
		final ReadWriteLock lock;
		final List<ExplorationStep> stackedJobs;
		protected final int id;

		public PLCMThread(final int id) {
			super("PLCMThread" + id);
			this.stackedJobs = new ArrayList<ExplorationStep>();
			this.id = id;
			this.lock = new ReentrantReadWriteLock();
			this.counters = new long[PLCMCounters.values().length];
		}

		void init(ExplorationStep initState) {
			this.lock.writeLock().lock();
			this.stackedJobs.add(initState);
			this.lock.writeLock().unlock();
		}

		@Override
		public long getId() {
			return this.id;
		}

		@Override
		public void run() {
			// no need to readlock, this thread is the only one that can do
			// writes
			boolean exit = false;
			while (!exit) {
				ExplorationStep sj = null;
				if (!this.stackedJobs.isEmpty()) {
					sj = this.stackedJobs.get(this.stackedJobs.size() - 1);

					ExplorationStep extended = sj.next();
					// iterator is finished, remove it from the stack
					if (extended == null) {
						this.lock.writeLock().lock();

						this.stackedJobs.remove(this.stackedJobs.size() - 1);
						this.counters[PLCMCounters.ExplorationStepInstances.ordinal()]++;
						this.counters[PLCMCounters.ExplorationStepCaughtWrongFirstParents.ordinal()] += sj
								.getCaughtWrongFirstParentCount();

						this.lock.writeLock().unlock();
					} else {
						this.lcm(extended);
					}

				} else { // our list was empty, we should steal from another
							// thread
					ExplorationStep stolj = stealJob(this);
					if (stolj == null) {
						exit = true;
					} else {
						lcm(stolj);
					}
				}
			}
		}

		private void lcm(ExplorationStep state) {
			collect(state);

			this.lock.writeLock().lock();
			this.stackedJobs.add(state);
			this.lock.writeLock().unlock();
		}
	}
}
