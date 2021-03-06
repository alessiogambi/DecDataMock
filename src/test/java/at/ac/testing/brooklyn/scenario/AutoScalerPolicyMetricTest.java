package at.ac.testing.brooklyn.scenario;

import static at.ac.testing.brooklyn.utils.ScenarioElement.resize;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleOverMax;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleUnderMin;
import static at.ac.testing.brooklyn.utils.ScenarioElement.sequence;
import static at.ac.testing.brooklyn.utils.ScenarioElement.stay;
import static brooklyn.policy.autoscaling.AutoScalerPolicyTest.currentSizeAsserter;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.brooklyn.utils.DataMockLoadGenerator;
import at.ac.testing.brooklyn.utils.ScenarioAtom;
import at.ac.testing.brooklyn.utils.ScenarioHandler;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.basic.BasicNotificationSensor;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.MaxPoolSizeReachedEvent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class AutoScalerPolicyMetricTest extends
		at.ac.testing.brooklyn.original.AutoScalerPolicyMetricTest {

	@BeforeClass
	public static void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;// We limit the search space by
											// default, so we can keep it simple
	}

	// Non proprio la stess semantica, qui ci vorrebbe un predicato "speciale"
	// per i boundary checks
	@Test
	public void testIncrementsSizeIffUpperBoundExceeded() {
		tc.resize(1);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100).build();
		tc.addPolicy(policy);

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100)//
				.build();

		// Build the scenario for the test!
		// DataMockLoadGenerator mockedDataGeneration =
		// scenarioHandler.at(0).from(1).startWith(stay(1)).then(scaleUp(2)).build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(stay(1).withSensorMetric())
				.then(resize(1, 2).withSensorMetric()).build();

		tc.addEnricher(mockedDataGeneration);

		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 100);
		assertSucceedsContinually(ImmutableMap.of("timeout", SHORT_WAIT_MS),
				currentSizeAsserter(tc, 1));

		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 101);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 2));
	}

	// Non proprio la stess semantica, qui ci vorrebbe un predicato "speciale"
	@Test
	public void testDecrementsSizeIffLowerBoundExceeded() {
		tc.resize(2);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100).build();
		tc.addPolicy(policy);

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100)
				//
				.build();

		// Build the scenario for the test!
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(stay(2).withSensorMetric())
				.then(resize(2, 1).withSensorMetric()).build();
		tc.addEnricher(mockedDataGeneration);

		mockedDataGeneration.unfold(); // tc.setAttribute(MY_ATTRIBUTE, 50);
		assertSucceedsContinually(ImmutableMap.of("timeout", SHORT_WAIT_MS),
				currentSizeAsserter(tc, 2));

		mockedDataGeneration.unfold(); // tc.setAttribute(MY_ATTRIBUTE, 49);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 1));
	}

	@Test
	public void testIncrementsSizeInProportionToMetric() {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100).build();
		tc.addPolicy(policy);

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100)
				//
				.build();

		// Build the scenario for the test!
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(resize(5, 10).withSensorMetric())
				.then(resize(10, 11).withSensorMetric()).build();
		tc.addEnricher(mockedDataGeneration);

		// workload 200 so requires doubling size to 10 to handle: (200*5)/100 =
		// 10
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 200);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 10));

		// workload 5, requires 1 entity: (10*110)/100 = 11
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 110);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 11));
	}

	@Test
	public void testDecrementsSizeInProportionToMetric() {
		tc.resize(5);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100).build();
		tc.addPolicy(policy);

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						sequence(ScenarioAtom.resize(5, 4),
								ScenarioAtom.resize(4, 2),
								ScenarioAtom.resize(2, 0)).withSensorMetric()
								.doNotAddOtherElements()).build();
		tc.addEnricher(mockedDataGeneration);

		// workload can be handled by 4 servers, within its valid range:
		// (49*5)/50 = 4.9
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 49);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 4));

		// workload can be handled by 4 servers, within its valid range:
		// (25*4)/50 = 2
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 25);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 2));

		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 0);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 0));
	}

	@Test
	public void testObeysMinAndMaxSize() {
		tc.resize(4);

		AutoScalerPolicy policy = new AutoScalerPolicy.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100).minPoolSize(2).maxPoolSize(6).build();
		tc.addPolicy(policy);

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100)
				//
				.minPoolSize(2).maxPoolSize(6)
				//
				.build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.from(4).startWith(scaleUnderMin())
				.then(scaleOverMax().withSensorMetric()).build();
		tc.addEnricher(mockedDataGeneration);

		// Decreases to min-size only
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 0);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 2));

		// Increases to max-size only
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 100000);
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 6));
	}

	@Test
	public void testWarnsWhenMaxCapReached() {
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
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100)//
				.maxPoolSize(6)//
				.maxSizeReachedSensor(maxSizeReachedSensor).build();
		tc.addPolicy(policy);

		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metric(MY_ATTRIBUTE).metricLowerBound(50)
				.metricUpperBound(100)
				//
				.maxPoolSize(6)
				//
				.build();

		// DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
		// .startWith(resize(1, 6).withSensorMetric())
		// .then(scaleOverMax().withSensorMetric()).build();
		// TODO Use sequences to force the total number of elements
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						sequence(ScenarioAtom.resize(1, 6),
								ScenarioAtom.scaleOverMax()).withSensorMetric()
								.doNotAddOtherElements()).build();
		tc.addEnricher(mockedDataGeneration);

		// workload can be handled by 6 servers, so no need to notify: 6 <=
		// (100*6)/50
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 600);

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(tc, 6));
		assertTrue(maxReachedEvents.isEmpty());

		// Increases to above max capacity: would require (100000*6)/100 = 6000
		mockedDataGeneration.unfold();// tc.setAttribute(MY_ATTRIBUTE, 100000);

		// Assert our listener gets notified (once)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						assertEquals(maxReachedEvents.size(), 1);
						assertEquals(maxReachedEvents.get(0).getMaxAllowed(), 6);
						assertEquals(maxReachedEvents.get(0)
								.getCurrentPoolSize(), 6);

						// XXX Tweak: this requires to be able to get to the end
						// state of the the system (or at least to get to the
						// input!)
						// assertEquals(maxReachedEvents.get(0)
						// .getCurrentUnbounded(), 6000);
						// assertEquals(maxReachedEvents.get(0).getMaxUnbounded(),
						// 6000);
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