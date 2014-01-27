package fr.liglab.mining.tests;

import org.junit.Test;

import fr.liglab.mining.PLCM;
import fr.liglab.mining.internals.ExplorationStep;
import fr.liglab.mining.io.AllFISConverter;
import fr.liglab.mining.io.PatternsCollector;

public class AllFISConverterTest {

	@Test
	public void testConverter() {
		ExplorationStep init = new ExplorationStep(2, FileReaderTest.PATH_TEST_ALL_FIS);
		PatternsCollector collector = new AllFISConverter(FileReaderTest.getTestAllFISPatterns());
		PLCM algo = new PLCM(collector, 1);
		algo.lcm(init);
		collector.close();
	}

}
