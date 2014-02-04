package fr.liglab.jlcm.tests;

import org.junit.Test;

import fr.liglab.jlcm.PLCM;
import fr.liglab.jlcm.internals.ExplorationStep;
import fr.liglab.jlcm.io.AllFISConverter;
import fr.liglab.jlcm.io.PatternsCollector;

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
