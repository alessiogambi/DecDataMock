package at.ac.testing.brooklyn.literal;

import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import brooklyn.entity.Entity;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.ResizeOperator;
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

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 3)
						.getSensorReading());
		// would cause pool to shrink to 3

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 1)
						.getSensorReading());
		// now expect pool to shrink to 1

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
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 3)
						.getSensorReading());
		// would cause pool to grow to 3

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 5)
						.getSensorReading());
		// now expect pool to grow to 5

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

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 5)
						.getSensorReading());
		// would cause pool to grow to 5

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 1)
						.getSensorReading());
		// now expect pool to shrink to 1

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

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 1)
						.getSensorReading());
		// would cause pool to shrink to 1

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 5)
						.getSensorReading());
		// now expect pool to grow to 5

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

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

		// Ignores temporary blip
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(1 * 10, 1 * 20)).resize(1, 4)
						.getSensorReading());
		// would
		// grow
		// to
		// 4
		Thread.sleep(resizeUpStabilizationDelay - OVERHEAD_DURATION_MS);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_OK_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(1, 1)
						.getSensorReading());
		// but
		// 1
		// is
		// still
		// adequate

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

		// Will grow to only the max sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(1 * 10, 1 * 20)).resize(1, 4)
						.getSensorReading());
		// would
		// grow
		// to
		// 4
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(1 * 10, 1 * 20)).resize(1, 2)
						.getSensorReading());
		// would
		// grow
		// to
		// 2
		Thread.sleep(resizeUpStabilizationDelay - OVERHEAD_DURATION_MS);

		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(1 * 10, 1 * 20)).resize(1, 4)
						.getSensorReading());
		// would
		// grow
		// to
		// 4

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

		// After suitable delay, grows to desired
		final long emitTime = System.currentTimeMillis();
		final Map<String, Object> need4 = (new ThresholdBasedPolicy(1 * 10,
				1 * 20)).resize(1, 4).getSensorReading();
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR, need4); // would
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
							resizable.emit(
									AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
									need4);
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
		// Ignores temporary blip
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 1)
						.getSensorReading());
		// would
		// shrink
		// to
		// 1
		Thread.sleep(resizeDownStabilizationDelay - OVERHEAD_DURATION_MS);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_OK_SENSOR,
				(new ThresholdBasedPolicy(1 * 10, 1 * 20)).resize(2, 2)
						.getSensorReading());
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

		// Will shrink to only the min sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(3 * 10, 3 * 20)).resize(3, 1)
						.getSensorReading());
		// would
		// shrink
		// to
		// 1
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(3 * 10, 3 * 20)).resize(3, 2)
						.getSensorReading());
		// would
		// shrink
		// to
		// 2
		Thread.sleep(resizeDownStabilizationDelay - OVERHEAD_DURATION_MS);

		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(3 * 10, 3 * 20)).resize(3, 1)
						.getSensorReading());
		// would
		// shrink
		// to
		// 1

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

		// TODO Note that the final time depends on the minPeriodBetweenExecs as
		// well
		// So either we remove it from the parameters or we update the oracle !
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

		// After suitable delay, grows to desired
		final long emitTime = System.currentTimeMillis();
		final Map<String, Object> needJust1 = (new ThresholdBasedPolicy(2 * 10,
				2 * 20)).resize(2, 1).getSensorReading();
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, needJust1);
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
							resizable.emit(
									AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
									needJust1); // would
												// shrink
												// to
												// 1
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
