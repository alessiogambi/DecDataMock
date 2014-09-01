package at.ac.testing.brooklyn.mixed;

import static at.ac.testing.brooklyn.utils.ScenarioElement.concurrentEvents;
import static at.ac.testing.brooklyn.utils.ScenarioElement.monitoringBlip;
import static at.ac.testing.brooklyn.utils.ScenarioElement.resize;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleOverMax;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleUnderMin;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.brooklyn.utils.DataMockLoadGenerator;
import at.ac.testing.brooklyn.utils.ScenarioAtom;
import at.ac.testing.brooklyn.utils.ScenarioElement;
import at.ac.testing.brooklyn.utils.ScenarioHandler;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.collections.MutableMap;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This class re implement the relevant tests of the AutoScalerPolicyTest class
 * in a literal way: we substitute all the occurrences of hardcoded test input
 * data with a call to the data mock generator
 */
public class AutoScalerPolicyTest extends at.ac.testing.brooklyn.original.AutoScalerPolicyTest {

	@BeforeClass(alwaysRun = true)
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@AfterMethod(alwaysRun = true)
	public void forceGC() {
		System.gc();
	}

	@Test
	public void testShrinkColdPool() throws Exception {
		resizable.resize(4);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, (new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 3).getSensorReading());

		// expect pool to shrink to 3 (i.e. maximum to have >= 40 per container)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 3));
	}

	@Test
	public void testShrinkColdPoolRoundsUpDesiredNumberOfContainers() throws Exception {
		resizable.resize(4);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, (new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 1).getSensorReading());

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 1));
	}

	@Test
	public void testGrowHotPool() throws Exception {
		resizable.resize(2);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR, (new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 3).getSensorReading());

		// expect pool to grow to 3 (i.e. minimum to have <= 80 per container)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 3));
	}

	@Test
	public void testNeverShrinkBelowMinimum() throws Exception {
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder().minPoolSize(2).build();
		resizable.addPolicy(policy);

		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(4 * 10).metricUpperBound(4 * 20)//
				.minPoolSize(2).withTimeUnit(TimeUnit.MILLISECONDS).build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0).from(4).startWith(scaleUnderMin().withSensorPool().doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		// expect pool to shrink only to the minimum
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 2));
	}

	@Test
	public void testNeverGrowAboveMaximmum() throws Exception {
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder().maxPoolSize(5).build();
		resizable.addPolicy(policy);

		resizable.resize(4);
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(4 * 10).metricUpperBound(4 * 20)//
				.maxPoolSize(5).withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0).from(4).startWith(scaleOverMax().withSensorPool().doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		// expect pool to grow only to the maximum
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 5));
	}

	@Test
	public void testNeverGrowColdPool() throws Exception {
		resizable.resize(2);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, (new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 50).getSensorReading());

		Thread.sleep(SHORT_WAIT_MS);
		assertEquals(resizable.getCurrentSize(), (Integer) 2);
	}

	@Test
	public void testNeverShrinkHotPool() throws Exception {
		resizable.resizeSleepTime = 0;
		resizable.resize(2);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR, (new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 0).getSensorReading());

		Thread.sleep(SHORT_WAIT_MS);
		assertEquals(resizable.getCurrentSize(), (Integer) 2);
	}

	@Test(groups = "Integration")
	public void testConcurrentShrinkShrink() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(4 * 10).metricUpperBound(4 * 20)//
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(concurrentEvents(ScenarioAtom.resize(4, 3).withSensorPool(), ScenarioAtom.resize(4, 1).withSensorPool()).doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 1));
	}

	// TODO First resize can be simply scaleUp()
	// TODO No need of simultaneous, should work also concurrent
	@Test(groups = "Integration")
	public void testConcurrentGrowGrow() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(2 * 10).metricUpperBound(2 * 20)//
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0).startWith(concurrentEvents(
		// TODO Is this necessary here the
		// withSensorPool on the Atom ?
				ScenarioAtom.resize(2, 3).withSensorPool(), ScenarioAtom.resize(2, 5).withSensorPool()).doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 5));
	}

	@Test(groups = "Integration")
	public void testConcurrentGrowShrink() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(2 * 10).metricUpperBound(2 * 20)//
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(concurrentEvents(ScenarioAtom.resize(2, 5).withSensorPool(), ScenarioAtom.resize(2, 1).withSensorPool()).doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 1));
	}

	@Test(groups = "Integration")
	public void testConcurrentShrinkGrow() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(4 * 10).metricUpperBound(4 * 20)//
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(concurrentEvents(ScenarioAtom.resize(4, 1).withSensorPool(), ScenarioAtom.resize(4, 5).withSensorPool()).doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 5));
	}

	// TODO Different semantic or wrong test case definition ? the two messages
	// have different values for UB and LB !
	// We cannot do that with ScenarioHandler but only with basic objects
	@Test(groups = "Integration")
	public void testResizeUpStabilizationDelayIgnoresBlip() throws Exception {
		// XXX Second/Millisecond problems !
		long resizeUpStabilizationDelay = 2000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder().resizeUpStabilizationDelay(resizeUpStabilizationDelay).minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(1 * 10).metricUpperBound(1 * 20)
		//
				.resizeUpStabilizationDelay((int) TimeUnit.MILLISECONDS.toSeconds(resizeUpStabilizationDelay)).minPeriodBetweenExecs((int) TimeUnit.MILLISECONDS.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		// DataMockLoadGenerator mockedDataGeneration =
		// scenarioHandler.at(0).from(1).startWith(monitoringBlip()).build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
		//
				.startWith(monitoringBlip(ScenarioAtom.resize(1, 3).withSensorPool(), ScenarioAtom.stay(1).withSensorPool()).doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		assertEquals(resizable.getCurrentSize(), (Integer) 1);
		assertSucceedsContinually(MutableMap.of("duration", 2000L), new Runnable() {
			@Override
			public void run() {
				assertEquals(resizable.sizes, ImmutableList.of(1));
			}
		});

	}

	@Test(groups = "Integration")
	public void testResizeUpStabilizationDelayTakesMaxSustainedDesired() throws Exception {
		// FIXME Tweak ! => Nota che per avere 3 azioni non concorrenti e
		// contando i secondi, questo numero deve essere abbastanza grande
		// Altrimenti devi abbassare quel numero ma tenere i Millisecondi !
		long resizeUpStabilizationDelay = 4000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder().resizeUpStabilizationDelay(resizeUpStabilizationDelay).minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(1 * 10).metricUpperBound(1 * 20)
		//
				.resizeUpStabilizationDelay((int) TimeUnit.MILLISECONDS.toSeconds(resizeUpStabilizationDelay)).minPeriodBetweenExecs((int) TimeUnit.MILLISECONDS.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				//
				.startWith(
						ScenarioElement.sustainedActions(ScenarioAtom.resize(1, 4).withSensorPool(), ScenarioAtom.resize(1, 2).withSensorPool(), ScenarioAtom.resize(1, 4).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		// Will grow to only the max sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		// XXX This comment is the unclear is not 4 the MAX value instead of 2 ?
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		mockedDataGeneration.execute();
		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		// Wait for it to reach size 2, and confirm take expected time
		// TODO This is time sensitive, and sometimes fails in CI with size=4 if
		// we wait for currentSize==2 (presumably GC kicking in?)
		// Therefore do strong assertion of currentSize==2 later, so can write
		// out times if it goes wrong.
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS), new Runnable() {
			public void run() {
				assertTrue(resizable.getCurrentSize() >= 2, "currentSize=" + resizable.getCurrentSize());
			}
		});
		assertEquals(resizable.getCurrentSize(), (Integer) 2, stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms after first emission; " + (stopwatch.elapsed(TimeUnit.MILLISECONDS) - postSleepTime)
				+ "ms after last");

		long timeToResizeTo2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		assertTrue(timeToResizeTo2 >= resizeUpStabilizationDelay - EARLY_RETURN_MS && timeToResizeTo2 <= resizeUpStabilizationDelay + OVERHEAD_DURATION_MS, "Resizing to 2: time=" + timeToResizeTo2
				+ "; resizeUpStabilizationDelay=" + resizeUpStabilizationDelay);

		// Will then grow to 4 $resizeUpStabilizationDelay milliseconds after
		// that emission
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 4));
		long timeToResizeTo4 = stopwatch.elapsed(TimeUnit.MILLISECONDS) - postSleepTime;

		System.out.println("\n\tHistory of SIZES: " + resizable.sizes + "\n");

		assertTrue(timeToResizeTo4 >= resizeUpStabilizationDelay - EARLY_RETURN_MS && timeToResizeTo4 <= resizeUpStabilizationDelay + OVERHEAD_DURATION_MS, "Resizing to 4: timeToResizeTo4="
				+ timeToResizeTo4 + "; timeToResizeTo2=" + timeToResizeTo2 + "; resizeUpStabilizationDelay=" + resizeUpStabilizationDelay);
	}

	@Test(groups = "Integration")
	public void testResizeUpStabilizationDelayResizesAfterDelay() {
		final long resizeUpStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder().resizeUpStabilizationDelay(resizeUpStabilizationDelay).minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		// After suitable delay, grows to desired
		final long emitTime = System.currentTimeMillis();
		final Map<String, Object> need4 = (new ThresholdBasedPolicy(1 * 10, 1 * 20)).resize(1, 4).getSensorReading();
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR, need4); // would
																			// grow
																			// to
																			// 4
		final AtomicInteger emitCount = new AtomicInteger(0);

		executeUntilSucceeds(MutableMap.of("timeout", TIMEOUT_MS), new Runnable() {
			public void run() {
				if (System.currentTimeMillis() - emitTime > (2 + emitCount.get()) * resizeUpStabilizationDelay) {
					// first one may not have been received, in a
					// registration
					// race
					resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR, need4);
					emitCount.incrementAndGet();
				}
				assertEquals(resizable.getCurrentSize(), (Integer) 4);
			}
		});

		long resizeDelay = System.currentTimeMillis() - emitTime;
		assertTrue(resizeDelay >= (resizeUpStabilizationDelay - EARLY_RETURN_MS), "resizeDelay=" + resizeDelay);
	}

	@Test(groups = "Integration")
	public void testResizeDownStabilizationDelayIgnoresBlip() throws Exception {
		// XXX This cannot be easily dealt with SECONDS and MILLISECONDS !
		// Change to 2
		long resizeDownStabilizationDelay = 2000L;
		// long resizeDownStabilizationDelay = 1;
		// XXX Control Period of 0 is not working ? !
		long minPeriodBetweenExecs = 0;

		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder().resizeDownStabilizationDelay(resizeDownStabilizationDelay).minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(1 * 10).metricUpperBound(1 * 20)
		//
				.resizeDownStabilizationDelay((int) TimeUnit.MILLISECONDS.toSeconds(resizeDownStabilizationDelay)).minPeriodBetweenExecs((int) TimeUnit.MILLISECONDS.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		// DataMockLoadGenerator mockedDataGeneration =
		// scenarioHandler.at(0).from(2).startWith(monitoringBlip()).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
		//
				.startWith(monitoringBlip(ScenarioAtom.resize(2, 1).withSensorPool().withSensorMetric(), ScenarioAtom.stay(1).withSensorPool().withSensorMetric()).doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		assertEquals(resizable.getCurrentSize(), (Integer) 2);
		assertSucceedsContinually(MutableMap.of("duration", 2000L), new Runnable() {
			public void run() {
				assertEquals(resizable.sizes, ImmutableList.of(2));
			}
		});
	}

	@Test(groups = "Integration")
	public void testResizeDownStabilizationDelayTakesMinSustainedDesired() throws Exception {
		// FIXME Tweak ! => Nota che per avere 3 azioni non concorrenti e
		// contando i secondi, questo numero deve essere abbastanza grande
		// Altrimenti devi abbassare quel numero ma tenere i Millisecondi !

		long resizeDownStabilizationDelay = 4000L;
		long minPeriodBetweenExecs = 0;
		policy.suspend();
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder().resizeDownStabilizationDelay(resizeDownStabilizationDelay).minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(3);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder().metricLowerBound(3 * 10).metricUpperBound(3 * 20)
		//
				.resizeDownStabilizationDelay((int) TimeUnit.MILLISECONDS.toSeconds(resizeDownStabilizationDelay)).minPeriodBetweenExecs((int) TimeUnit.MILLISECONDS.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				//
				.startWith(
						ScenarioElement.sustainedActions(ScenarioAtom.resize(3, 1).withSensorPool(), ScenarioAtom.resize(3, 2).withSensorPool(), ScenarioAtom.resize(3, 1).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		// Will shrink to only the min sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		// XXX This comment is unclear, why 2 should be the minimum and not 1?
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		mockedDataGeneration.execute();
		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		// Wait for it to reach size 2, and confirm take expected time
		// TODO This is time sensitive, and sometimes fails in CI with size=1 if
		// we wait for currentSize==2 (presumably GC kicking in?)
		// Therefore do strong assertion of currentSize==2 later, so can write
		// out times if it goes wrong.
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS), new Runnable() {
			public void run() {
				assertTrue(resizable.getCurrentSize() <= 2, "currentSize=" + resizable.getCurrentSize());
			}
		});
		assertEquals(resizable.getCurrentSize(), (Integer) 2, stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms after first emission; " + (stopwatch.elapsed(TimeUnit.MILLISECONDS) - postSleepTime)
				+ "ms after last");

		long timeToResizeTo2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		assertTrue(timeToResizeTo2 >= resizeDownStabilizationDelay - EARLY_RETURN_MS && timeToResizeTo2 <= resizeDownStabilizationDelay + OVERHEAD_DURATION_MS, "Resizing to 2: time="
				+ timeToResizeTo2 + "; resizeDownStabilizationDelay=" + resizeDownStabilizationDelay);

		// Will then shrink to 1 $resizeUpStabilizationDelay milliseconds after
		// that emission
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS), currentSizeAsserter(resizable, 1));
		long timeToResizeTo1 = stopwatch.elapsed(TimeUnit.MILLISECONDS) - postSleepTime;

		assertTrue(timeToResizeTo1 >= resizeDownStabilizationDelay - EARLY_RETURN_MS && timeToResizeTo1 <= resizeDownStabilizationDelay + OVERHEAD_DURATION_MS, "Resizing to 1: timeToResizeTo1="
				+ timeToResizeTo1 + "; timeToResizeTo2=" + timeToResizeTo2 + "; resizeDownStabilizationDelay=" + resizeDownStabilizationDelay);
	}

	// FIXME Better do this with message precomputation !
	@Test(groups = "Integration")
	public void testResizeDownStabilizationDelayResizesAfterDelay() throws Exception {
		final long resizeDownStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder().resizeDownStabilizationDelay(resizeDownStabilizationDelay).minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(2);

		// After suitable delay, grows to desired
		final long emitTime = System.currentTimeMillis();
		final Map<String, Object> needJust1 = (new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 1).getSensorReading();
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, needJust1);
		// would
		// shrink
		// to
		// 1
		final AtomicInteger emitCount = new AtomicInteger(0);

		executeUntilSucceeds(MutableMap.of("timeout", TIMEOUT_MS), new Runnable() {
			public void run() {
				if (System.currentTimeMillis() - emitTime > (2 + emitCount.get()) * resizeDownStabilizationDelay) {
					// first one may not have been received, in a registration
					// race
					resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, needJust1); // would
																							// shrink
																							// to
																							// 1
					emitCount.incrementAndGet();
				}
				assertEquals(resizable.getCurrentSize(), (Integer) 1);
			}
		});

		long resizeDelay = System.currentTimeMillis() - emitTime;
		assertTrue(resizeDelay >= (resizeDownStabilizationDelay - EARLY_RETURN_MS), "resizeDelay=" + resizeDelay);
	}
}
