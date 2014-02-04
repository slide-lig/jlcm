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


package fr.liglab.jlcm.io;


public class StdOutCollector extends PatternsWriter {

	protected long collected = 0;
	protected long collectedLength = 0;
	
	@Override
	synchronized public void collect(int support, int[] pattern, int length) {
		System.out.print(Integer.toString(support) + "\t");
		
		boolean addSeparator = false;
		for (int i = 0; i < length; i++) {
			if (addSeparator) {
				System.out.print(' ');
			} else {
				addSeparator = true;
			}
			
			System.out.print(pattern[i]);
		}
		
		System.out.println("");
		
		this.collected++;
		this.collectedLength += pattern.length;
	}

	public long close() {
		return this.collected;
	}

	public int getAveragePatternLength() {
		if (this.collected == 0) {
			return 0;
		} else {
			return (int) (this.collectedLength / this.collected);
		}
	}
}
