package at.ac.testing.mocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;

public class PoolTest {
	private static final Logger LOG = LoggerFactory.getLogger(PoolTest.class);

	@BeforeClass
	public static void setupSolver() {
		LogMap.SolverOpt_IntBitWidth = 9;
	}

	@Test
	public void testInit() throws Exception {
		Pool pool = new Pool(-1, -1);
		pool.init();
		System.out.println("PoolTest.testInit() " + pool);
	}

	@Test
	public void testInitWithValues() throws Exception {
		Pool pool = new Pool(-1, 10);
		pool.init();
		System.out.println("PoolTest.testInitWithValues()" + pool);

		pool = new Pool(5, -1);
		pool.init();
		System.out.println("PoolTest.testInitWithValues()" + pool);

		pool = new Pool(1, 100);
		pool.init();
		System.out.println("PoolTest.testInitWithValues()" + pool);
	}

	@Test
	public void testUnderMin() throws Exception {
		Pool pool = new Pool(2, 6);
		pool.init();
		System.out.println("PoolTest.testUnderMin() " + pool);
		TimedAction action = new TimedAction(-1, 5, 2, ActionType.SCALE_DOWN, SensorType.METRIC);
		pool.forceUnderMin(action);
		System.out.println("PoolTest.testOverMax() " + action);
	}

	@Test
	public void testOverMax() throws Exception {
		Pool pool = new Pool(2, 6);
		pool.init();
		System.out.println("PoolTest.testOverMax() " + pool);
		// TimedAction action = new TimedAction(-1, 2, 6, ActionType.SCALE_UP,
		// SensorType.METRIC);
		// pool.forceOverMax(action);
		// System.out.println("PoolTest.testOverMax() " + action);
	}
}
