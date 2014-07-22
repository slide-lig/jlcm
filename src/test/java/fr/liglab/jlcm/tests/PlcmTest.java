package fr.liglab.jlcm.tests;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import fr.liglab.jlcm.PLCM;
import fr.liglab.jlcm.internals.ExplorationStep;
import fr.liglab.jlcm.internals.TransactionReader;
import fr.liglab.jlcm.io.AllFISConverter;
import fr.liglab.jlcm.io.FileReader;
import fr.liglab.jlcm.io.PatternsCollector;

public class PlcmTest {
	
	private void minerInvocation(int minsup, String path, PatternsCollector collector) {
		ExplorationStep init = new ExplorationStep(minsup, path);
		PLCM algo = new PLCM(collector, 1);
		algo.lcm(init);
		collector.close();
	}
	
	@Test
	public void testFiles() {
//		minerInvocation(2, FileReaderTest.PATH_MICRO, FileReaderTest.getMicroReaderPatterns());
//		minerInvocation(2, FileReaderTest.PATH_GLOBAL_CLOSURE, FileReaderTest.getGlobalClosurePatterns());
//		minerInvocation(2, FileReaderTest.PATH_FAKE_GLOBAL_CLOSURE, FileReaderTest.getFakeGlobalClosurePatterns());
		minerInvocation(4, FileReaderTest.PATH_50_RETAIL, FileReaderTest.get50RetailPatterns());
		//minerInvocation(6, FileReaderTest.PATH_TEST_UNFILTERING, FileReaderTest.getTestUnfilteringPatterns());
	}
	

	@Test
	public void testGenericInitState() {
		ExplorationStep init = new ExplorationStep(4, new StupidIterable(FileReaderTest.PATH_50_RETAIL));
		StubPatternsCollector collector = FileReaderTest.get50RetailPatterns();
		PLCM algo = new PLCM(collector, 1);
		algo.lcm(init);
		collector.close();
	}

	private static class StupidIterable implements Iterable<TransactionReader> {
		
		private final String input;
		
		public StupidIterable(String inputPath) {
			this.input = inputPath;
		}
		
		public Iterator<TransactionReader> iterator() {
			return new FileReader(this.input);
		}
	}

}
