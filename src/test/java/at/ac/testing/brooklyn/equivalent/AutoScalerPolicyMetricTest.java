package at.ac.testing.brooklyn.equivalent;

import static brooklyn.policy.autoscaling.AutoScalerPolicyTest.currentSizeAsserter;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeClass;
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

public class AutoScalerPolicyMetricTest extends at.ac.testing.brooklyn.original.AutoScalerPolicyMetricTest {

	ThresholdBasedPolicy mockedPolicy;

	@BeforeClass
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@Test
	public void testIncrementsSizeIffUpperBoundExceeded() {
		tc.resize(1);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE).metricLowerBound(Integer.valueOf(50)).metricUpperBound(Integer.valueOf(100)).build();
		tc.addPolicy(policy);

		mockedPolicy = new ThresholdBasedPolicy(50, 100);

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(1, 1).getMetric());
		TestUtils.assertSucceedsContinually(ImmutableMap.of("timeout", Long.valueOf(SHORT_WAIT_MS)), AutoScalerPolicyTest.currentSizeAsserter(this.tc, 1));

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(1, 2).getMetric());
		TestUtils.executeUntilSucceeds(ImmutableMap.of("timeout", Long.valueOf(TIMEOUT_MS)), AutoScalerPolicyTest.currentSizeAsserter(this.tc, 2));
	}

	@Test
	public void testDecrementsSizeIffLowerBoundExceeded() {
		tc.resize(2);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE).metricLowerBound(50).metricUpperBound(100).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(50, 100);

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 2).getMetric());
		assertSucceedsContinually(ImmutableMap.of("timeout", SHORT_WAIT_MS), currentSizeAsserter(tc, 2));

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 1).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 1));
	}

	@Test
	public void testIncrementsSizeInProportionToMetric() {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE).metricLowerBound(50).metricUpperBound(100).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(50, 100);

		// workload 200 so requires doubling size to 10 to handle: (200*5)/100 =
		// 10
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(Integer.valueOf(5), 10).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 10));

		// workload 5, requires 1 entity: (10*110)/100 = 11
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(Integer.valueOf(10), 11).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 11));
	}

	@Test
	public void testDecrementsSizeInProportionToMetric() {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE).metricLowerBound(50).metricUpperBound(100).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(50, 100);

		// workload can be handled by 4 servers, within its valid range:
		// (49*5)/50 = 4.9
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(5, 4).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 4));

		// workload can be handled by 4 servers, within its valid range:
		// (25*4)/50 = 2
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(4, 2).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 2));

		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 0).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 0));
	}

	@Test
	public void testObeysMinAndMaxSize() {
		tc.resize(4);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE).metricLowerBound(50).metricUpperBound(100).minPoolSize(2).maxPoolSize(6).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(50, 100);

		// Decreases to min-size only
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(4, 0).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 2));

		// Increases to max-size only
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(2, 20).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 6));
	}

	@Test
	public void testWarnsWhenMaxCapReached() {
		final List<MaxPoolSizeReachedEvent> maxReachedEvents = Lists.newCopyOnWriteArrayList();
		tc.resize(1);

		BasicNotificationSensor<MaxPoolSizeReachedEvent> maxSizeReachedSensor = AutoScalerPolicy.DEFAULT_MAX_SIZE_REACHED_SENSOR;

		app.subscribe(tc, maxSizeReachedSensor, new SensorEventListener<MaxPoolSizeReachedEvent>() {
			@Override
			public void onEvent(SensorEvent<MaxPoolSizeReachedEvent> event) {
				maxReachedEvents.add(event.getValue());
			}
		});

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE).metricLowerBound(50).metricUpperBound(100).maxPoolSize(6).maxSizeReachedSensor(maxSizeReachedSensor).build();
		tc.addPolicy(policy);
		mockedPolicy = new ThresholdBasedPolicy(50, 100);

		// workload can be handled by 6 servers, so no need to notify: 6 <=
		// (100*6)/50
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(1, 6).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(tc, 6));
		assertTrue(maxReachedEvents.isEmpty());

		// Increases to above max capacity: would require (100000*6)/100 = 6000
		tc.setAttribute(MY_ATTRIBUTE, mockedPolicy.resize(6, 20).getMetric());

		// Assert our listener gets notified (once)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), new Runnable() {
			public void run() {
				assertEquals(maxReachedEvents.size(), 1);
				assertEquals(maxReachedEvents.get(0).getMaxAllowed(), 6);
				assertEquals(maxReachedEvents.get(0).getCurrentPoolSize(), 6);
				assertEquals(maxReachedEvents.get(0).getCurrentUnbounded(), 20, "Current Unbounded is not ok");
				assertEquals(maxReachedEvents.get(0).getMaxUnbounded(), 20, "MaxUnbounded is not ok");
				assertEquals(maxReachedEvents.get(0).getTimeWindow(), 0);
			}
		});
		assertSucceedsContinually(new Runnable() {
			@Override
			public void run() {
				assertEquals(maxReachedEvents.size(), 1);
			}
		});
		currentSizeAsserter(tc, 6).run();
	}

}