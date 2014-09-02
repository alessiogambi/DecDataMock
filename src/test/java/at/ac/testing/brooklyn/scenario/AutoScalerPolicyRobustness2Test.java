package at.ac.testing.brooklyn.scenario;

import static at.ac.testing.brooklyn.utils.ScenarioElement.concurrentEvents;
import static at.ac.testing.brooklyn.utils.ScenarioElement.monitoringBlip;
import static at.ac.testing.brooklyn.utils.ScenarioElement.resize;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleOverMax;
import static at.ac.testing.brooklyn.utils.ScenarioElement.scaleUnderMin;
import static at.ac.testing.brooklyn.utils.ScenarioElement.sequence;
import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.brooklyn.utils.DataMockLoadGenerator;
import at.ac.testing.brooklyn.utils.ScenarioAtom;
import at.ac.testing.brooklyn.utils.ScenarioElement;
import at.ac.testing.brooklyn.utils.ScenarioHandler;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.collections.MutableMap;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This class contains only the test cases that depends on time related
 * configurations.
 */
public class AutoScalerPolicyRobustness2Test extends
		at.ac.testing.brooklyn.original.AutoScalerPolicyRobustness2Test {

	@BeforeClass(alwaysRun = true)
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@AfterMethod(alwaysRun = true)
	public void forceGC() {
		System.gc();
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testConcurrentShrinkShrink(long minPeriodBetweenExecs,
			long resizeUpStabilizationDelay, long resizeDownStabilizationDelay)
			throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				//
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(4 * 10)
				.metricUpperBound(4 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						concurrentEvents(
								ScenarioAtom.resize(4, 3).withSensorPool(),
								ScenarioAtom.resize(4, 1).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testConcurrentGrowGrow(long minPeriodBetweenExecs,
			long resizeUpStabilizationDelay, long resizeDownStabilizationDelay)
			throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				//
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(2 * 10)
				.metricUpperBound(2 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						concurrentEvents(
								// TODO Is this necessary here the
								// withSensorPool on the Atom ?
								ScenarioAtom.resize(2, 3).withSensorPool(),
								ScenarioAtom.resize(2, 5).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testConcurrentGrowShrink(long minPeriodBetweenExecs,
			long resizeUpStabilizationDelay, long resizeDownStabilizationDelay)
			throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				//
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(2 * 10)
				.metricUpperBound(2 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						concurrentEvents(
								ScenarioAtom.resize(2, 5).withSensorPool(),
								ScenarioAtom.resize(2, 1).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testConcurrentShrinkGrow(long minPeriodBetweenExecs,
			long resizeUpStabilizationDelay, long resizeDownStabilizationDelay)
			throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				//
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(4 * 10)
				.metricUpperBound(4 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						concurrentEvents(
								ScenarioAtom.resize(4, 1).withSensorPool(),
								ScenarioAtom.resize(4, 5).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

	// COMMENT: Second/Millisecond SAT limitation!
	// TODO Different semantic or wrong test case definition ? the two messages
	// have different values for UB and LB !
	// We cannot do that with ScenarioHandler but only with basic objects
	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testResizeUpStabilizationDelayIgnoresBlip(
			long minPeriodBetweenExecs, long resizeUpStabilizationDelay,
			long resizeDownStabilizationDelay) throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay).build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(1 * 10)
				.metricUpperBound(1 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				//
				.startWith(
						monitoringBlip(
								ScenarioAtom.resize(1, 3).withSensorPool(),
								ScenarioAtom.stay(1).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		Assert.assertEquals(resizable.getCurrentSize(), (Integer) 1);
		assertSucceedsContinually(MutableMap.of("duration", 2000L),
				new Runnable() {
					@Override
					public void run() {
						Assert.assertEquals(resizable.sizes,
								ImmutableList.of(1));
					}
				});
	}

	// COMMENT Tweak ! => Nota che per avere 3 azioni non concorrenti e
	// contando i secondi, questo numero deve essere abbastanza grande
	// Altrimenti devi abbassare quel numero ma tenere i Millisecondi !
	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testResizeUpStabilizationDelayTakesMaxSustainedDesired(
			long minPeriodBetweenExecs, long resizeUpStabilizationDelay,
			long resizeDownStabilizationDelay) throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(1 * 10)
				.metricUpperBound(1 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				//
				.startWith(
						ScenarioElement.sustainedActions(
								ScenarioAtom.resize(1, 4).withSensorPool(),
								ScenarioAtom.resize(1, 2).withSensorPool(),
								ScenarioAtom.resize(1, 4).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		// Will grow to only the max sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		mockedDataGeneration.execute();
		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		// Wait for it to reach size 2, and confirm take expected time
		// TODO This is time sensitive, and sometimes fails in CI with size=4 if
		// we wait for currentSize==2 (presumably GC kicking in?)
		// Therefore do strong assertion of currentSize==2 later, so can write
		// out times if it goes wrong.
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						Assert.assertTrue(resizable.getCurrentSize() >= 2,
								"currentSize=" + resizable.getCurrentSize());
					}
				});
		Assert.assertEquals(
				resizable.getCurrentSize(),
				(Integer) 2,
				stopwatch.elapsed(TimeUnit.MILLISECONDS)
						+ "ms after first emission; "
						+ (stopwatch.elapsed(TimeUnit.MILLISECONDS) - postSleepTime)
						+ "ms after last");

		long timeToResizeTo2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		Assert.assertTrue(timeToResizeTo2 >= resizeUpStabilizationDelay
				- EARLY_RETURN_MS
				&& timeToResizeTo2 <= resizeUpStabilizationDelay
						+ OVERHEAD_DURATION_MS + minPeriodBetweenExecs, "Resizing to 2: time="
				+ timeToResizeTo2 + "; resizeUpStabilizationDelay="
				+ resizeUpStabilizationDelay);

		// Will then grow to 4 $resizeUpStabilizationDelay milliseconds after
		// that emission
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 4));
		long timeToResizeTo4 = stopwatch.elapsed(TimeUnit.MILLISECONDS)
				- postSleepTime;

		// TODO Note that the final time depends on the minPeriodBetweenExecs as
		// well
		// So either we remove it from the parameters or we update the oracle !
		Assert.assertTrue(timeToResizeTo4 >= resizeUpStabilizationDelay
				- EARLY_RETURN_MS
				&& timeToResizeTo4 <= resizeUpStabilizationDelay
						+ OVERHEAD_DURATION_MS + minPeriodBetweenExecs,
				"Resizing to 4: timeToResizeTo4=" + timeToResizeTo4
						+ "; timeToResizeTo2=" + timeToResizeTo2
						+ "; resizeUpStabilizationDelay="
						+ resizeUpStabilizationDelay);
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testResizeUpStabilizationDelayResizesAfterDelay(
			long minPeriodBetweenExecs, final long resizeUpStabilizationDelay,
			long resizeDownStabilizationDelay) {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay).build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(1 * 10)
				.metricUpperBound(1 * 20)
				//
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		final DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						sequence(ScenarioAtom.resize(1, 4),
								ScenarioAtom.resize(1, 4)).withSensorPool()
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		// After suitable delay, grows to desired
		final long emitTime = System.currentTimeMillis();
		mockedDataGeneration.unfold();

		final AtomicInteger emitCount = new AtomicInteger(0);

		executeUntilSucceeds(MutableMap.of("timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						if (System.currentTimeMillis() - emitTime > (2 + emitCount
								.get()) * resizeUpStabilizationDelay) {
							// first one may not have been received, in a
							// registration
							// race
							mockedDataGeneration.unfold();
							emitCount.incrementAndGet();
						}
						Assert.assertEquals(resizable.getCurrentSize(),
								(Integer) 4);
					}
				});

		long resizeDelay = System.currentTimeMillis() - emitTime;
		Assert.assertTrue(
				resizeDelay >= (resizeUpStabilizationDelay - EARLY_RETURN_MS),
				"resizeDelay=" + resizeDelay);
	}

	// COMMENT This cannot be easily dealt with SECONDS and MILLISECONDS !
	// TODO Check that it will skyp if less than 3/4 seconds

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testResizeDownStabilizationDelayIgnoresBlip(
			long minPeriodBetweenExecs, long resizeUpStabilizationDelay,
			long resizeDownStabilizationDelay) throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(1 * 10)
				.metricUpperBound(1 * 20)
				//
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						monitoringBlip(ScenarioAtom.resize(2, 1),
								ScenarioAtom.stay(2)).withSensorPool()
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();
		// but
		// 2
		// is
		// still
		// adequate

		Assert.assertEquals(resizable.getCurrentSize(), (Integer) 2);
		assertSucceedsContinually(MutableMap.of("duration", 2000L),
				new Runnable() {
					public void run() {
						Assert.assertEquals(resizable.sizes,
								ImmutableList.of(2));
					}
				});
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testResizeDownStabilizationDelayTakesMinSustainedDesired(
			long minPeriodBetweenExecs, long resizeUpStabilizationDelay,
			long resizeDownStabilizationDelay) throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resize(3);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(3 * 10)
				.metricUpperBound(3 * 20)
				//
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				//
				.startWith(
						ScenarioElement.sustainedActions(
								ScenarioAtom.resize(3, 1).withSensorPool(),
								ScenarioAtom.resize(3, 2).withSensorPool(),
								ScenarioAtom.resize(3, 1).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		// Will shrink to only the min sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		mockedDataGeneration.execute();

		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		// Wait for it to reach size 2, and confirm take expected time
		// TODO This is time sensitive, and sometimes fails in CI with size=1 if
		// we wait for currentSize==2 (presumably GC kicking in?)
		// Therefore do strong assertion of currentSize==2 later, so can write
		// out times if it goes wrong.
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						Assert.assertTrue(resizable.getCurrentSize() <= 2,
								"currentSize=" + resizable.getCurrentSize());
					}
				});
		Assert.assertEquals(
				resizable.getCurrentSize(),
				(Integer) 2,
				stopwatch.elapsed(TimeUnit.MILLISECONDS)
						+ "ms after first emission; "
						+ (stopwatch.elapsed(TimeUnit.MILLISECONDS) - postSleepTime)
						+ "ms after last");

		long timeToResizeTo2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		Assert.assertTrue(timeToResizeTo2 >= resizeDownStabilizationDelay
				- EARLY_RETURN_MS
				&& timeToResizeTo2 <= resizeDownStabilizationDelay + minPeriodBetweenExecs
						+ OVERHEAD_DURATION_MS, "Resizing to 2: time="
				+ timeToResizeTo2 + "; resizeDownStabilizationDelay="
				+ resizeDownStabilizationDelay);

		// Will then shrink to 1 $resizeUpStabilizationDelay milliseconds after
		// that emission
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
		long timeToResizeTo1 = stopwatch.elapsed(TimeUnit.MILLISECONDS)
				- postSleepTime;

		Assert.assertTrue(timeToResizeTo1 >= resizeDownStabilizationDelay
				- EARLY_RETURN_MS
				&& timeToResizeTo1 <= resizeDownStabilizationDelay
						+ OVERHEAD_DURATION_MS + minPeriodBetweenExecs,
				"Resizing to 1: timeToResizeTo1=" + timeToResizeTo1
						+ "; timeToResizeTo2=" + timeToResizeTo2
						+ "; resizeDownStabilizationDelay="
						+ resizeDownStabilizationDelay);
	}

	@Test(groups = "Integration")
	@Parameters({ "minPeriodBetweenExecs", "resizeUpStabilizationDelay",
			"resizeDownStabilizationDelay" })
	public void testResizeDownStabilizationDelayResizesAfterDelay(
			long minPeriodBetweenExecs, long resizeUpStabilizationDelay,
			final long resizeDownStabilizationDelay) throws Exception {
		//
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder()
				.minPeriodBetweenExecs(minPeriodBetweenExecs)
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)//
				.build();
		resizable.addPolicy(policy);
		//
		//
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(2 * 10)
				.metricUpperBound(2 * 20)
				//
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		final DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						sequence(ScenarioAtom.resize(2, 1),
								ScenarioAtom.resize(2, 1)).withSensorPool()
								.doNotAddOtherElements()).build();

		resizable.addEnricher(mockedDataGeneration);

		// After suitable delay, grows to desired
		final long emitTime = System.currentTimeMillis();
		// final Map<String, Object> needJust1 = (new ThresholdBasedPolicy(2 *
		// 10, 2 * 20)).resize(2, 1).getSensorReading();
		// resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, needJust1);
		mockedDataGeneration.unfold();
		// would
		// shrink
		// to
		// 1
		final AtomicInteger emitCount = new AtomicInteger(0);

		executeUntilSucceeds(MutableMap.of("timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						if (System.currentTimeMillis() - emitTime > (2 + emitCount
								.get()) * resizeDownStabilizationDelay) {
							// first one may not have been received, in a
							// registration
							// race
							mockedDataGeneration.unfold();
							emitCount.incrementAndGet();
						}
						Assert.assertEquals(resizable.getCurrentSize(),
								(Integer) 1);
					}
				});

		long resizeDelay = System.currentTimeMillis() - emitTime;
		Assert.assertTrue(
				resizeDelay >= (resizeDownStabilizationDelay - EARLY_RETURN_MS),
				"resizeDelay=" + resizeDelay);
	}
}
