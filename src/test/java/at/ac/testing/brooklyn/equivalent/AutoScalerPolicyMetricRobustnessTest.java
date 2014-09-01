package at.ac.testing.brooklyn.equivalent;

import static brooklyn.policy.autoscaling.AutoScalerPolicyTest.currentSizeAsserter;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.basic.BasicNotificationSensor;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.AutoScalerPolicyTest;
import brooklyn.policy.autoscaling.MaxPoolSizeReachedEvent;
import brooklyn.test.TestUtils;

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
public class AutoScalerPolicyMetricRobustnessTest extends
		at.ac.testing.brooklyn.original.AutoScalerPolicyMetricRobustnessTest {

	ThresholdBasedPolicy mockedPolicy;

	@BeforeClass
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	// COMMENT: This really does not require any threshold based mock object at
	// all !
	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testIncrementsSizeIffUpperBoundExceeded(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(1);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);

		mockedPolicy = new ThresholdBasedPolicy(lowerBound, upperBound);

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(1, 1).getMetric());
		TestUtils.assertSucceedsContinually(
				ImmutableMap.of("timeout", Long.valueOf(SHORT_WAIT_MS)),
				AutoScalerPolicyTest.currentSizeAsserter(this.tc, 1));

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(1, 2).getMetric());
		TestUtils.executeUntilSucceeds(
				ImmutableMap.of("timeout", Long.valueOf(TIMEOUT_MS)),
				AutoScalerPolicyTest.currentSizeAsserter(this.tc, 2));
	}

	// COMMENT: This really does not require any threshold based mock object at
	// all !
	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testDecrementsSizeIffLowerBoundExceeded(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(2);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(lowerBound, upperBound);

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 2).getMetric());
		assertSucceedsContinually(ImmutableMap.of("timeout", SHORT_WAIT_MS),
				currentSizeAsserter(tc, 2));

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 1).getMetric());
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
		mockedPolicy = new ThresholdBasedPolicy(lowerBound, upperBound);

		// workload 200 so requires doubling size to 10 to handle: (200*5)/100 =
		// 10
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(5, 10).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 10));

		// workload 5, requires 1 entity: (10*110)/100 = 11
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(10, 11).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 11));
	}

	// COMMENT: Pay attention to the state consistency here, the states are
	// correlated!
	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testDecrementsSizeInProportionToMetric(int lowerBound,
			int upperBound, int minSize, int maxSize) throws Exception {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(lowerBound, upperBound);

		// workload can be handled by 4 servers, within its valid range:
		// (49*5)/50 = 4.9
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(5, 4).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 4));

		// workload can be handled by 4 servers, within its valid range:
		// (25*4)/50 = 2
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(4, 2).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 2));

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 0).getMetric());
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
		mockedPolicy = new ThresholdBasedPolicy(lowerBound, upperBound);

		// Decreases to min-size only
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(4, minSize - 1)
				.getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, minSize));

		// Increases to max-size only
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(minSize, maxSize + 1)
				.getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, maxSize));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testWarnsWhenMaxCapReached(int lowerBound, int upperBound,
			int minSize, final int maxSize) throws Exception {
		final List<MaxPoolSizeReachedEvent> maxReachedEvents = Lists
				.newCopyOnWriteArrayList();

		// TODO Improve readability by introducing variables for initialState
		// and intended resize
		tc.resize(minSize);

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
		mockedPolicy = new ThresholdBasedPolicy(lowerBound, upperBound);

		// workload can be handled by 6 servers, so no need to notify: 6 <=
		// (100*6)/50
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(minSize, maxSize)
				.getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, maxSize));
		assertTrue(maxReachedEvents.isEmpty());

		// Increases to above max capacity: would require (100000*6)/100 = 6000
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(maxSize, maxSize + 1)
				.getMetric());

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
								.getCurrentUnbounded(), (maxSize + 1),
								"Current Unbounded is not ok");
						assertEquals(maxReachedEvents.get(0).getMaxUnbounded(),
								(maxSize + 1), "MaxUnbounded is not ok");
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
