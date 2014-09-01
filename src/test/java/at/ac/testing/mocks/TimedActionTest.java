package at.ac.testing.mocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;

public class TimedActionTest {

	private static final Logger LOG = LoggerFactory.getLogger(TimedActionTest.class);

	@BeforeClass
	public static void setupSolver() {
		LogMap.SolverOpt_IntBitWidth = 9;
	}

	@Test
	public void validDefaultSensorTypeTest() {
		TimedAction t = new TimedAction(-1, ActionType.SCALE_UP);

		t.checkValidity();

		LOG.info("A valid TimedAction : " + t);

		Assert.assertEquals(t.sensorType, SensorType.POOL_SENSOR);
	}

	@Test
	public void maintainDefaultSensorTypeTest() {
		TimedAction t = new TimedAction(-1, ActionType.SCALE_UP);
		t.sensorType = SensorType.POOL_SENSOR;
		t.checkValidity();

		Assert.assertEquals(t.sensorType, SensorType.POOL_SENSOR);
	}

	@Test
	public void maintainSensorTypeTest() {
		TimedAction t = new TimedAction(-1, ActionType.SCALE_UP);
		t.sensorType = SensorType.METRIC;
		t.checkValidity();

		LOG.info("TimedAction : " + t);
		Assert.assertEquals(t.sensorType, SensorType.METRIC);
	}
	
	@Test
	public void maintainSensorTypeWithSizesTest() {
		TimedAction t = new TimedAction(-1, 1, 2);
		t.sensorType = SensorType.METRIC;
		t.checkValidity();

		LOG.info("TimedAction : " + t);
		Assert.assertEquals(t.sensorType, SensorType.METRIC);
	}
}
