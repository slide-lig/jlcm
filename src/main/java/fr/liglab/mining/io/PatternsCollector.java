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


package fr.liglab.mining.io;

import fr.liglab.mining.internals.ExplorationStep;


public interface PatternsCollector {
	public void collect(final ExplorationStep state);

	/**
	 * Call this once mining has terminated. Behavior of the collect method is
	 * undefined once close() has been called
	 * 
	 * @return outputted pattern count
	 */
	public long close();
	
	/**
	 * It is safer to get this value once close() has been called.
	 * @return average length among outputted patterns
	 */
	public int getAveragePatternLength();
}
