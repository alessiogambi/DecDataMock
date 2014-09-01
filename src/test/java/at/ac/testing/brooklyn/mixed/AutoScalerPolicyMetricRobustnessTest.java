package at.ac.testing.brooklyn.mixed;

import static at.ac.testing.brooklyn.utils.ScenarioElement.resize;
import static at.ac.testing.brooklyn.utils.ScenarioElement.sequence;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleOverMax;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleUnderMin;
import static brooklyn.policy.autoscaling.AutoScalerPolicyTest.currentSizeAsserter;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.brooklyn.utils.DataMockLoadGenerator;
import at.ac.testing.brooklyn.utils.ScenarioHandler;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.basic.BasicNotificationSensor;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.AutoScalerPolicyTest;
import brooklyn.policy.autoscaling.MaxPoolSizeReachedEvent;
import brooklyn.test.TestUtils;
import at.ac.testing.brooklyn.utils.ScenarioAtom;

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

	@BeforeClass
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	// COMMENT This does not event require the policy mock
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
		TestUtils.assertSucceedsContinually(
				ImmutableMap.of("timeout", Long.valueOf(SHORT_WAIT_MS)),
				AutoScalerPolicyTest.currentSizeAsserter(this.tc, 1));

		tc.setAttribute(MY_ATTRIBUTE, upperBound + 1);
		TestUtils.executeUntilSucceeds(
				ImmutableMap.of("timeout", Long.valueOf(TIMEOUT_MS)),
				AutoScalerPolicyTest.currentSizeAsserter(this.tc, 2));
	}

	// COMMENT This does not event require the policy mock
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

		tc.setAttribute(MY_ATTRIBUTE, lowerBound - 1);
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

		ThresholdBasedPolicy mockedPolicy = new ThresholdBasedPolicy(
				lowerBound, upperBound);

		// workload 200 so requires doubling size to 10 to handle: (200*5)/100 =
		// 10
		tc.setAttribute(MY_ATTRIBUTE,
				mockedPolicy.resize(Integer.valueOf(5), 10).getMetric());
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 10));

		// workload 5, requires 1 entity: (10*110)/100 = 11
		tc.setAttribute(MY_ATTRIBUTE,
				mockedPolicy.resize(Integer.valueOf(10), 11).getMetric());
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

		ThresholdBasedPolicy mockedPolicy = new ThresholdBasedPolicy(
				lowerBound, upperBound);

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

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).minPoolSize(minSize)
				.maxPoolSize(maxSize).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.from(4).startWith(scaleUnderMin())
				.then(scaleOverMax().withSensorMetric()).build();
		tc.addEnricher(mockedDataGeneration);

		// Decreases to min-size only
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 0);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, minSize));

		// Increases to max-size only
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 100000);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, maxSize));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound", "minSize", "maxSize" })
	public void testWarnsWhenMaxCapReached(int lowerBound, int upperBound,
			int minSize, final int maxSize) throws Exception {
		final List<MaxPoolSizeReachedEvent> maxReachedEvents = Lists
				.newCopyOnWriteArrayList();
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

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(lowerBound)
				.metricUpperBound(upperBound).maxPoolSize(maxSize).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						sequence(ScenarioAtom.resize(minSize, maxSize),
								ScenarioAtom.resize(maxSize, maxSize + 1))
								.withSensorMetric().doNotAddOtherElements())
				.build();
		tc.addEnricher(mockedDataGeneration);

		// workload can be handled by 6 servers, so no need to notify: 6 <=
		// (100*6)/50
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 600);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, maxSize));
		assertTrue(maxReachedEvents.isEmpty());

		// Increases to above max capacity: would require (100000*6)/100 = 6000
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 100000);

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
								.getCurrentUnbounded(), maxSize + 1);
						assertEquals(maxReachedEvents.get(0).getMaxUnbounded(),
								maxSize + 1);
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