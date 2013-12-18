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

import java.util.Calendar;

import fr.liglab.mining.internals.ExplorationStep;
import fr.liglab.mining.internals.ExplorationStep.Progress;

/**
 * This thread will give some information about the progression on stderr every
 * 5 minutes. When running on Hadoop it may also be used to poke the master node
 * every 5 minutes so it doesn't kill the task.
 * 
 * you MUST use setInitState before starting the thread you MAY use
 * setHadoopContext
 */
public class ProgressWatcherThread extends Thread {
	/**
	 * ping delay, in milliseconds
	 */
	private static final long PRINT_STATUS_EVERY = 5 * 60 * 1000;

	private ExplorationStep step;

	public void setInitState(ExplorationStep initial) {
		this.step = initial;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(PRINT_STATUS_EVERY);
				Progress progress = this.step.getProgression();
				System.err.format("%1$tY/%1$tm/%1$td %1$tk:%1$tM:%1$tS - root iterator state : %2$d/%3$d\n",
						Calendar.getInstance(), progress.current, progress.last);

			} catch (InterruptedException e) {
				return;
			}
		}
	}

}
