package fr.liglab.jlcm.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import fr.liglab.jlcm.PLCM;
import fr.liglab.jlcm.internals.ExplorationStep;
import fr.liglab.jlcm.io.AllFISConverter;
import fr.liglab.jlcm.io.PatternsCollector;

public class PlcmTest {
	
	private void minerInvocation(int minsup, String path, PatternsCollector collector) {
		ExplorationStep init = new ExplorationStep(minsup, path);
		PLCM algo = new PLCM(collector, 1);
		algo.lcm(init);
		collector.close();
	}
	
	@Test
	public void test() {
		minerInvocation(2, FileReaderTest.PATH_MICRO, FileReaderTest.getMicroReaderPatterns());
		minerInvocation(2, FileReaderTest.PATH_GLOBAL_CLOSURE, FileReaderTest.getGlobalClosurePatterns());
		minerInvocation(2, FileReaderTest.PATH_FAKE_GLOBAL_CLOSURE, FileReaderTest.getFakeGlobalClosurePatterns());
		minerInvocation(4, FileReaderTest.PATH_50_RETAIL, FileReaderTest.get50RetailPatterns());
		minerInvocation(6, FileReaderTest.PATH_TEST_UNFILTERING, FileReaderTest.getTestUnfilteringPatterns());
	}
	
	

}
