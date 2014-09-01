package at.ac.testing.brooklyn.original;

import static brooklyn.policy.autoscaling.AutoScalerPolicyTest.currentSizeAsserter;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.basic.BasicNotificationSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.MaxPoolSizeReachedEvent;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestCluster;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * 
 * This class contains only the test cases relevant for elasticity. We remove
 * from the original test suite only those test cases that checked the on line
 * reconfiguration of the policy object but not its inner functionality.
 * 
 * We use this class as baseline for the comparison
 * 
 * @author alessiogambi
 * 
 */
public class AutoScalerPolicyMetricRobustnessTest {
	public static final long TIMEOUT_MS = 10000;
	public static final long SHORT_WAIT_MS = 250;

	static public final AttributeSensor<Integer> MY_ATTRIBUTE = Sensors
			.newIntegerSensor("autoscaler.test.intAttrib");
	protected TestApplication app;
	protected TestCluster tc;

	@BeforeMethod(alwaysRun = true)
	public void before() {
		app = ApplicationBuilder.newManagedApp(TestApplication.class);
		tc = app.createAndManageChild(EntitySpec.create(TestCluster.class)
				.configure("initialSize", 1));
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		if (app != null) {
			Entities.destroyAll(app.getManagementContext());
		}
		System.gc();
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testIncrementsSizeIffUpperBoundExceeded(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(1);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);

		tc.setAttribute(MY_ATTRIBUTE, upperBound);
		assertSucceedsContinually(ImmutableMap.of("timeout", SHORT_WAIT_MS),
				currentSizeAsserter(tc, 1));

		tc.setAttribute(MY_ATTRIBUTE, (upperBound + 1));
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 2));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testDecrementsSizeIffLowerBoundExceeded(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(2);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);

		tc.setAttribute(MY_ATTRIBUTE, lowerBound);
		assertSucceedsContinually(ImmutableMap.of("timeout", SHORT_WAIT_MS),
				currentSizeAsserter(tc, 2));

		tc.setAttribute(MY_ATTRIBUTE, (lowerBound - 1));
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 1));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testIncrementsSizeInProportionToMetric(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);

		// workload 200 so requires doubling size to 10 to handle: (200*5)/100 =
		// 10
		tc.setAttribute(MY_ATTRIBUTE, 200);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 10));

		// workload 5, requires 1 entity: (10*110)/100 = 11
		tc.setAttribute(MY_ATTRIBUTE, 110);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 11));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testDecrementsSizeInProportionToMetric(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);

		// workload can be handled by 4 servers, within its valid range:
		// (49*5)/50 = 4.9
		tc.setAttribute(MY_ATTRIBUTE, 49);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 4));

		// workload can be handled by 4 servers, within its valid range:
		// (25*4)/50 = 2
		tc.setAttribute(MY_ATTRIBUTE, 25);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 2));

		tc.setAttribute(MY_ATTRIBUTE, 0);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 0));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testObeysMinAndMaxSize(int lowerBound, int upperBound,
			int minSize, int maxSize) throws Exception {
		tc.resize(4);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).minPoolSize(minSize)
				.maxPoolSize(maxSize).build();
		tc.addPolicy(policy);

		// Decreases to min-size only
		tc.setAttribute(MY_ATTRIBUTE, 0);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, minSize));

		// Increases to max-size only
		tc.setAttribute(MY_ATTRIBUTE, 100000);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, maxSize));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testWarnsWhenMaxCapReached(int lowerBound, int upperBound,
			int minSize, final int maxSize) throws Exception {
		final List<MaxPoolSizeReachedEvent> maxReachedEvents = Lists
				.newCopyOnWriteArrayList();
		tc.resize(1);

		BasicNotificationSensor<MaxPoolSizeReachedEvent> maxSizeReachedSensor = AutoScalerPolicy.DEFAULT_MAX_SIZE_REACHED_SENSOR;

		app.subscribe(tc, maxSizeReachedSensor,
				new SensorEventListener<MaxPoolSizeReachedEvent>() {
					@Override
					public void onEvent(
							SensorEvent<MaxPoolSizeReachedEvent> event) {
						maxReachedEvents.add(event.getValue());
					}
				});

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).maxPoolSize(maxSize)
				.maxSizeReachedSensor(maxSizeReachedSensor).build();
		tc.addPolicy(policy);

		// workload can be handled by 6 servers, so no need to notify: 6 <=
		// (100*6)/50
		tc.setAttribute(MY_ATTRIBUTE, 600);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, maxSize));
		assertTrue(maxReachedEvents.isEmpty());

		// Increases to above max capacity: would require (100000*6)/100 = 6000
		tc.setAttribute(MY_ATTRIBUTE, 100000);

		// Assert our listener gets notified (once)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						assertEquals(maxReachedEvents.size(), 1);
						assertEquals(maxReachedEvents.get(0).getMaxAllowed(),
								maxSize);
						assertEquals(maxReachedEvents.get(0)
								.getCurrentPoolSize(), maxSize);
						assertEquals(maxReachedEvents.get(0)
								.getCurrentUnbounded(), 6000);
						assertEquals(maxReachedEvents.get(0).getMaxUnbounded(),
								6000);
						assertEquals(maxReachedEvents.get(0).getTimeWindow(), 0);
					}
				});
		assertSucceedsContinually(new Runnable() {
			@Override
			public void run() {
				assertEquals(maxReachedEvents.size(), 1);
			}
		});
		currentSizeAsserter(tc, maxSize).run();
	}

}
