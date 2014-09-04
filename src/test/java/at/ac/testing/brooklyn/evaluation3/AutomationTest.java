package at.ac.testing.brooklyn.evaluation3;

import java.util.Map;

import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.brooklyn.utils.LocallyResizableEntity;
import at.ac.testing.mocks.ActionType;
import at.ac.testing.mocks.SensorReading;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicNotificationSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.AutoScalerPolicyTest;
import brooklyn.test.TestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestCluster;

import com.google.common.collect.ImmutableMap;

public class AutomationTest {

	public static long TIMEOUT_MS = 1000;
	public static long SHORT_WAIT_MS = 250;
	public static long OVERHEAD_DURATION_MS = 500;
	public static long EARLY_RETURN_MS = 10;

	static public final AttributeSensor<Integer> MY_ATTRIBUTE = Sensors
			.newIntegerSensor("autoscaler.test.intAttrib");

	protected AutoScalerPolicy policy;
	protected TestCluster cluster;
	protected LocallyResizableEntity resizable;
	protected TestApplication app;

	ThresholdBasedPolicy dataMock;

	@BeforeClass
	public static void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 9;// We limit the search space by
		// default, so we can keep it simple
	}

	@BeforeMethod(alwaysRun = true)
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize",
			"minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void before(int lowerBound, int upperBound, int minSize,
			int maxSize, long minPeriodBetweenExecs,
			long resizeUpStabilizationDelay, long resizeDownStabilizationDelay) {
		// Setup the application
		app = ApplicationBuilder.newManagedApp(TestApplication.class);
		cluster = app.createAndManageChild(EntitySpec.create(TestCluster.class)
				.configure(TestCluster.INITIAL_SIZE, 1));

		resizable = new LocallyResizableEntity(cluster, cluster);
		Entities.manage(resizable);
		// Setup the policy (SUT)
		policy = AutoScalerPolicy.builder().metric(MY_ATTRIBUTE)
				//
				.metricRange(lowerBound, upperBound)
				//
				.minPoolSize(minSize).maxPoolSize(maxSize)
				//
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				//
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();

		resizable.addPolicy(policy);
		// Configure the Declarative Data Mock(s)
		dataMock = new ThresholdBasedPolicy(lowerBound, upperBound);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		if (policy != null) {
			policy.destroy();
		}
		if (app != null) {
			Entities.destroyAll(app.getManagementContext());
		}
		cluster = null;
		resizable = null;
		policy = null;
	}

	@Test(dataProvider = "allTransitions")
	public void testResizeWithMetricSensor(int startSize, int targetSize) {
		resizable.resize(startSize);
		SensorReading sr = dataMock.resize(startSize, targetSize);

		resizable.setAttribute(MY_ATTRIBUTE,
				dataMock.resize(startSize, targetSize).getMetric());

		TestUtils
				.executeUntilSucceeds(ImmutableMap.of("timeout",
						Long.valueOf(TIMEOUT_MS)), AutoScalerPolicyTest
						.currentSizeAsserter(resizable, targetSize));

	}

	private BasicNotificationSensor<Map> getSensor(SensorReading sr,
			int startSize, int targetSize) {
		if (startSize == targetSize) {
			return sr.getPoolSensor(ActionType.STAY);
		} else if (startSize < targetSize) {
			return sr.getPoolSensor(ActionType.SCALE_UP);
		} else {
			return sr.getPoolSensor(ActionType.SCALE_DOWN);
		}
	}

	/**
	 * This data provider generates all the possible transition between stated
	 * in minSize and maxSize. See http://www.lysergicjava.com/?p=165 for a
	 * better solution.
	 * 
	 * @return
	 */
	@DataProvider
	public Object[][] allTransitions(ITestContext context) {
		Integer minSize = Integer.parseInt(context.getCurrentXmlTest()
				.getParameter("minSize"));
		Integer maxSize = Integer.parseInt(context.getCurrentXmlTest()
				.getParameter("maxSize"));
		int n = (maxSize - minSize) + 1;
		Object[][] result = new Object[n * n][];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				result[n * i + j] = new Object[] { minSize + i, minSize + j };
			}
		}
		return result;
	}
}
