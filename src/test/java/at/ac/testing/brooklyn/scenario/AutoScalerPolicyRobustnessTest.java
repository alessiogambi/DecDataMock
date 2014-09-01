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
 * This class re implement the relevant tests of the AutoScalerPolicyTest class
 * in a literal way: we substitute all the occurrences of hardcoded test input
 * data with a call to the data mock generator
 */
public class AutoScalerPolicyRobustnessTest extends
		at.ac.testing.brooklyn.original.AutoScalerPolicyRobustnessTest {

	@BeforeClass(alwaysRun = true)
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@AfterMethod(alwaysRun = true)
	public void forceGC() {
		System.gc();
	}

	@Test
	@Parameters({ "lowerBound", "upperBound" })
	public void testShrinkColdPool(int lowerBound, int upperBound)
			throws Exception {
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)//
				.withTimeUnit(TimeUnit.MILLISECONDS).build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						resize(4, 3).withSensorPool().doNotAddOtherElements())
				.build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		// expect pool to shrink to 3 (i.e. maximum to have >= 40 per container)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 3));
	}

	@Test
	@Parameters({ "lowerBoundResourceThrashing", "upperBoundResourceThrashing" })
	public void testShrinkColdPoolRoundsUpDesiredNumberOfContainers(
			int lowerBoundResourceThrashing, int upperBoundResourceThrashing)
			throws Exception {
		resizable.resize(4);
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBoundResourceThrashing)
				.metricUpperBound(upperBoundResourceThrashing)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						resize(4, 1).withSensorPool().doNotAddOtherElements())
				.build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound" })
	public void testGrowHotPool(int lowerBound, int upperBound)
			throws Exception {
		resizable.resize(2);
		// Specify the general configurations
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						resize(2, 3).withSensorPool().doNotAddOtherElements())
				.build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		// expect pool to grow to 3 (i.e. minimum to have <= 80 per container)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 3));
	}

	@Test
	@Parameters({ "minSize", "maxSize", "lowerBound", "upperBound" })
	public void testNeverShrinkBelowMinimum(int minSize, int maxSize,
			int lowerBound, int upperBound) throws Exception {
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder().minPoolSize(minSize).build();
		resizable.addPolicy(policy);

		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.minPoolSize(minSize).withTimeUnit(TimeUnit.MILLISECONDS)
				.build();
		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.from(4)
				.startWith(
						scaleUnderMin().withSensorPool()
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		// expect pool to shrink only to the minimum
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, minSize));
	}

	@Test
	@Parameters({ "minSize", "maxSize", "lowerBound", "upperBound" })
	public void testNeverGrowAboveMaximmum(int minSize, int maxSize,
			int lowerBound, int upperBound) throws Exception {
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder().minPoolSize(minSize)
				.maxPoolSize(maxSize).build();
		resizable.addPolicy(policy);

		resizable.resize(minSize);
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.maxPoolSize(maxSize).withTimeUnit(TimeUnit.MILLISECONDS)
				.build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.from(4)
				.startWith(
						scaleOverMax().withSensorPool().doNotAddOtherElements())
				.build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		// expect pool to grow only to the maximum
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, maxSize));
	}

	@Test
	@Parameters({ "lowerBound", "upperBound" })
	public void testNeverGrowColdPool(int lowerBound, int upperBound)
			throws Exception {

		resizable.resizeSleepTime = 0;
		resizable.resize(2);
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.startWith(resize(2, 5).withWrongSensorPool()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		Assert.assertEquals(resizable.getCurrentSize(), (Integer) 2);
	}

	@Test
	@Parameters({ "lowerBound", "upperBound" })
	public void testNeverShrinkHotPool(int lowerBound, int upperBound)
			throws Exception {
		resizable.resizeSleepTime = 0;
		resizable.resize(2);
		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler.at(0)
				.from(2).startWith(resize(2, 0).withWrongSensorPool()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		Assert.assertEquals(resizable.getCurrentSize(), (Integer) 2);
	}

	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testConcurrentShrinkShrink(int lowerBound, int upperBound)
			throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)//
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

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
	@Parameters({ "lowerBound", "upperBound" })
	public void testConcurrentGrowGrow(int lowerBound, int upperBound)
			throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
				.startWith(
						concurrentEvents(
								ScenarioAtom.resize(2, 3).withSensorPool(),
								ScenarioAtom.resize(2, 5).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testConcurrentGrowShrink(int lowerBound, int upperBound)
			throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

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
	@Parameters({ "lowerBound", "upperBound" })
	public void testConcurrentShrinkGrow(int lowerBound, int upperBound)
			throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound).metricUpperBound(upperBound)
				.withTimeUnit(TimeUnit.MILLISECONDS).build();

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

	// TODO Different semantic or wrong test case definition ? the two messages
	// have different values for UB and LB !
	// We cannot do that with ScenarioHandler but only with basic objects
	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testResizeUpStabilizationDelayIgnoresBlip(int lowerBound,
			int upperBound) throws Exception {

		// COMMENT Second/Millisecond problems !
		long resizeUpStabilizationDelay = 2000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound)
				.metricUpperBound(upperBound)
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
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

	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testResizeUpStabilizationDelayTakesMaxSustainedDesired(
			int lowerBound, int upperBound) throws Exception {

		// COMMENT Tweak ! => Nota che per avere 3 azioni non concorrenti e
		// contando i secondi, questo numero deve essere abbastanza grande
		// Altrimenti devi abbassare quel numero ma tenere i Millisecondi !
		long resizeUpStabilizationDelay = 4000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound)
				.metricUpperBound(upperBound)
				.resizeUpStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeUpStabilizationDelay))
				.minPeriodBetweenExecs(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(minPeriodBetweenExecs))//
				.withTimeUnit(TimeUnit.SECONDS).build();

		DataMockLoadGenerator mockedDataGeneration = scenarioHandler
				.at(0)
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
						+ OVERHEAD_DURATION_MS, "Resizing to 2: time="
				+ timeToResizeTo2 + "; resizeUpStabilizationDelay="
				+ resizeUpStabilizationDelay);

		// Will then grow to 4 $resizeUpStabilizationDelay milliseconds after
		// that emission
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 4));
		long timeToResizeTo4 = stopwatch.elapsed(TimeUnit.MILLISECONDS)
				- postSleepTime;

		Assert.assertTrue(timeToResizeTo4 >= resizeUpStabilizationDelay
				- EARLY_RETURN_MS
				&& timeToResizeTo4 <= resizeUpStabilizationDelay
						+ OVERHEAD_DURATION_MS,
				"Resizing to 4: timeToResizeTo4=" + timeToResizeTo4
						+ "; timeToResizeTo2=" + timeToResizeTo2
						+ "; resizeUpStabilizationDelay="
						+ resizeUpStabilizationDelay);
	}

	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testResizeUpStabilizationDelayResizesAfterDelay(int lowerBound,
			int upperBound) throws Exception {
		final long resizeUpStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound)
				.metricUpperBound(upperBound)
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
		// would
		// grow
		// to
		// 4
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
						assertEquals(resizable.getCurrentSize(), (Integer) 4);
					}
				});

		long resizeDelay = System.currentTimeMillis() - emitTime;
		assertTrue(
				resizeDelay >= (resizeUpStabilizationDelay - EARLY_RETURN_MS),
				"resizeDelay=" + resizeDelay);
	}

	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testResizeDownStabilizationDelayIgnoresBlip(int lowerBound,
			int upperBound) throws Exception {

		// COMMENT: This cannot be easily dealt with SECONDS and MILLISECONDS !
		// Change to 2
		long resizeDownStabilizationDelay = 2000L;
		// long resizeDownStabilizationDelay = 1;
		// XXX Control Period of 0 is not working ? !
		long minPeriodBetweenExecs = 0;

		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound)
				.metricUpperBound(upperBound)
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
						monitoringBlip(
								ScenarioAtom.resize(2, 1).withSensorPool(),
								ScenarioAtom.stay(2).withSensorPool())
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		mockedDataGeneration.execute();

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
	@Parameters({ "lowerBound", "upperBound" })
	public void testResizeDownStabilizationDelayTakesMinSustainedDesired(
			int lowerBound, int upperBound) throws Exception {

		// COMMENT Tweak ! => Nota che per avere 3 azioni non concorrenti e
		// contando i secondi, questo numero deve essere abbastanza grande
		// Altrimenti devi abbassare quel numero ma tenere i Millisecondi !

		long resizeDownStabilizationDelay = 4000L;
		long minPeriodBetweenExecs = 0;
		policy.suspend();
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(3);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound)
				.metricUpperBound(upperBound)
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
						ScenarioElement.sustainedActions(
								ScenarioAtom.resize(3, 1).withSensorPool(),
								ScenarioAtom.resize(3, 2).withSensorPool(),
								ScenarioAtom.resize(3, 1).withSensorPool())
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
				&& timeToResizeTo2 <= resizeDownStabilizationDelay
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
						+ OVERHEAD_DURATION_MS,
				"Resizing to 1: timeToResizeTo1=" + timeToResizeTo1
						+ "; timeToResizeTo2=" + timeToResizeTo2
						+ "; resizeDownStabilizationDelay="
						+ resizeDownStabilizationDelay);
	}

	@Test(groups = "Integration")
	@Parameters({ "lowerBound", "upperBound" })
	public void testResizeDownStabilizationDelayResizesAfterDelay(
			int lowerBound, int upperBound) throws Exception {
		final long resizeDownStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(2);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(lowerBound)
				.metricUpperBound(upperBound)
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
				//
				.startWith(
						resize(2, 1).withSensorPool().doNotAddOtherElements())
				.then(resize(2, 1).withSensorPool().doNotAddOtherElements())
				.build();
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
