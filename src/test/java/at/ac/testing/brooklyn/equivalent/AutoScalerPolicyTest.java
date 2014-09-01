package at.ac.testing.brooklyn.equivalent;

import static brooklyn.test.TestUtils.assertSucceedsContinually;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import brooklyn.entity.trait.Resizable;
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
public class AutoScalerPolicyTest extends
		at.ac.testing.brooklyn.original.AutoScalerPolicyTest {

	@BeforeClass(alwaysRun = true)
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@Test
	public void testShrinkColdPool() throws Exception {
		resizable.resize(4);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 3)
						.getSensorReading());

		// expect pool to shrink to 3 (i.e. maximum to have >= 40 per container)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 3));
	}

	@Test
	public void testShrinkColdPoolRoundsUpDesiredNumberOfContainers()
			throws Exception {
		resizable.resize(4);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 1)
						.getSensorReading());

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
	}

	@Test
	public void testGrowHotPool() throws Exception {
		resizable.resize(2);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 3)
						.getSensorReading());

		// expect pool to grow to 3 (i.e. minimum to have <= 80 per container)
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 3));
	}

	@Test
	public void testNeverShrinkBelowMinimum() throws Exception {
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder().minPoolSize(2).build();
		resizable.addPolicy(policy);

		resizable.resize(4);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 0)
						.getSensorReading());

		// expect pool to shrink only to the minimum
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 2));
	}

	@Test
	public void testNeverGrowAboveMaximmum() throws Exception {
		resizable.removePolicy(policy);
		policy = AutoScalerPolicy.builder().maxPoolSize(5).build();
		resizable.addPolicy(policy);

		resizable.resize(4);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(4 * 10, 4 * 20)).resize(4, 6)
						.getSensorReading());

		// expect pool to grow only to the maximum
		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

	@Test
	public void testNeverGrowColdPool() throws Exception {
		resizable.resize(2);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 50)
						.getSensorReading());

		Thread.sleep(SHORT_WAIT_MS);
		assertEquals(resizable.getCurrentSize(), (Integer) 2);
	}

	@Test
	public void testNeverShrinkHotPool() throws Exception {
		resizable.resizeSleepTime = 0;
		resizable.resize(2);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				(new ThresholdBasedPolicy(2 * 10, 2 * 20)).resize(2, 0)
						.getSensorReading());

		Thread.sleep(SHORT_WAIT_MS);
		assertEquals(resizable.getCurrentSize(), (Integer) 2);
	}

	@Test(groups = "Integration")
	public void testConcurrentShrinkShrink() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		// Precompute Data
		final Map<String, Object> resize_from_4_to_3 = (new ThresholdBasedPolicy(
				4 * 10, 4 * 20)).resize(4, 3).getSensorReading();
		final Map<String, Object> resize_from_4_to_1 = (new ThresholdBasedPolicy(
				4 * 10, 4 * 20)).resize(4, 1).getSensorReading();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_4_to_3);
		// would cause pool to shrink to 3

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_4_to_1);
		// now expect pool to shrink to 1

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
	}

	@Test(groups = "Integration")
	public void testConcurrentGrowGrow() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		// Precompute Data
		final Map<String, Object> resize_from_2_to_3 = (new ThresholdBasedPolicy(
				2 * 10, 2 * 20)).resize(2, 3).getSensorReading();
		final Map<String, Object> resize_from_2_to_5 = (new ThresholdBasedPolicy(
				2 * 10, 2 * 20)).resize(2, 5).getSensorReading();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_2_to_3);
		// would cause pool to grow to 3

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_2_to_5);
		// now expect pool to grow to 5

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

	@Test(groups = "Integration")
	public void testConcurrentGrowShrink() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(2);

		// Precompute Data
		final Map<String, Object> resize_from_2_to_5 = (new ThresholdBasedPolicy(
				2 * 10, 2 * 20)).resize(2, 5).getSensorReading();
		final Map<String, Object> resize_from_2_to_1 = (new ThresholdBasedPolicy(
				2 * 10, 2 * 20)).resize(2, 1).getSensorReading();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_2_to_5);
		// would cause pool to grow to 5

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_2_to_1);
		// now expect pool to shrink to 1

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 1));
	}

	@Test(groups = "Integration")
	public void testConcurrentShrinkGrow() throws Exception {
		resizable.resizeSleepTime = 250;
		resizable.resize(4);

		// Precompute Data
		final Map<String, Object> resize_from_4_to_1 = (new ThresholdBasedPolicy(
				4 * 10, 4 * 20)).resize(4, 1).getSensorReading();
		final Map<String, Object> resize_from_4_to_5 = (new ThresholdBasedPolicy(
				4 * 10, 4 * 20)).resize(4, 5).getSensorReading();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_4_to_1);
		// would cause pool to shrink to 1

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_4_to_5);
		// now expect pool to grow to 5

		executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
				currentSizeAsserter(resizable, 5));
	}

	@Test(groups = "Integration")
	public void testResizeUpStabilizationDelayIgnoresBlip() throws Exception {
		long resizeUpStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		// Precompute Messages
		final Map<String, Object> resize_from_1_to_4 = (new ThresholdBasedPolicy(
				1 * 10, 1 * 20)).resize(1, 4).getSensorReading();
		final Map<String, Object> stay_at_1 = (new ThresholdBasedPolicy(4 * 10,
				4 * 20)).resize(1, 1).getSensorReading();

		// Ignores temporary blip
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_1_to_4);
		// would
		// grow
		// to
		// 4
		Thread.sleep(resizeUpStabilizationDelay - OVERHEAD_DURATION_MS);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_OK_SENSOR, stay_at_1);
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
	public void testResizeUpStabilizationDelayTakesMaxSustainedDesired()
			throws Exception {
		long resizeUpStabilizationDelay = 1100L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(1);

		// Precompute the values
		final Map<String, Object> resize_from_1_to_4 = (new ThresholdBasedPolicy(
				1 * 10, 1 * 20)).resize(1, 4).getSensorReading();
		final Map<String, Object> resize_from_1_to_2 = (new ThresholdBasedPolicy(
				1 * 10, 1 * 20)).resize(1, 2).getSensorReading();

		// Will grow to only the max sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_1_to_4);
		// would
		// grow
		// to
		// 4
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_1_to_2);
		// would
		// grow
		// to
		// 2
		Thread.sleep(resizeUpStabilizationDelay - OVERHEAD_DURATION_MS);

		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
				resize_from_1_to_4);
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
	public void testResizeUpStabilizationDelayResizesAfterDelay() {
		final long resizeUpStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
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
	public void testResizeDownStabilizationDelayIgnoresBlip() throws Exception {
		long resizeStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
		resizable.resize(2);

		// Precompute the values
		final Map<String, Object> resize_from_2_to_1 = (new ThresholdBasedPolicy(
				2 * 10, 2 * 20)).resize(2, 1).getSensorReading();
		final Map<String, Object> stay_at_2 = (new ThresholdBasedPolicy(1 * 10,
				1 * 20)).resize(2, 2).getSensorReading();

		// Ignores temporary blip
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_2_to_1);
		// would
		// shrink
		// to
		// 1
		Thread.sleep(resizeStabilizationDelay - OVERHEAD_DURATION_MS);
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_OK_SENSOR, stay_at_2);
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
	public void testResizeDownStabilizationDelayTakesMinSustainedDesired()
			throws Exception {
		long resizeDownStabilizationDelay = 1100L;
		long minPeriodBetweenExecs = 0;
		policy.suspend();
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();

		resizable.addPolicy(policy);
		resizable.resize(3);

		// Precompute the values before starting measuring time !
		final Map<String, Object> resize_from_3_to_1 = (new ThresholdBasedPolicy(
				3 * 10, 3 * 20)).resize(3, 1).getSensorReading();
		final Map<String, Object> resize_from_3_to_2 = (new ThresholdBasedPolicy(
				3 * 10, 3 * 20)).resize(3, 2).getSensorReading();

		// Will shrink to only the min sustained in this time window
		// (i.e. to 2 within the first $resizeUpStabilizationDelay milliseconds)
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_3_to_1);
		// would
		// shrink
		// to
		// 1
		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_3_to_2);
		// would
		// shrink
		// to
		// 2
		Thread.sleep(resizeDownStabilizationDelay - OVERHEAD_DURATION_MS);

		long postSleepTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
				resize_from_3_to_1);
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
	public void testResizeDownStabilizationDelayResizesAfterDelay()
			throws Exception {
		final long resizeDownStabilizationDelay = 1000L;
		long minPeriodBetweenExecs = 0;
		resizable.removePolicy(policy);

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);
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

	protected static Map<String, Object> message(int currentSize,
			double currentWorkrate, double lowThreshold, double highThreshold) {
		return ImmutableMap.<String, Object> of(
				AutoScalerPolicy.POOL_CURRENT_SIZE_KEY, currentSize,
				AutoScalerPolicy.POOL_CURRENT_WORKRATE_KEY, currentWorkrate,
				AutoScalerPolicy.POOL_LOW_THRESHOLD_KEY, lowThreshold,
				AutoScalerPolicy.POOL_HIGH_THRESHOLD_KEY, highThreshold);
	}

	public static Runnable currentSizeAsserter(final Resizable resizable,
			final Integer desired) {
		return new Runnable() {
			public void run() {
				Assert.assertEquals(resizable.getCurrentSize(), desired);
			}
		};
	}

	public static void dumpThreadsEtc() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threads = threadMXBean.dumpAllThreads(true, true);
		for (ThreadInfo thread : threads) {
			System.out.println(thread.getThreadName() + " ("
					+ thread.getThreadState() + ")");
			for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
				System.out.println("\t" + stackTraceElement);
			}
		}

		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
		System.out.println("Memory:");
		System.out.println("\tHeap: used=" + heapMemoryUsage.getUsed()
				+ "; max=" + heapMemoryUsage.getMax() + "; init="
				+ heapMemoryUsage.getInit() + "; committed="
				+ heapMemoryUsage.getCommitted());
		System.out.println("\tNon-heap: used=" + nonHeapMemoryUsage.getUsed()
				+ "; max=" + nonHeapMemoryUsage.getMax() + "; init="
				+ nonHeapMemoryUsage.getInit() + "; committed="
				+ nonHeapMemoryUsage.getCommitted());

		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory
				.getOperatingSystemMXBean();
		System.out.println("OS:");
		System.out.println("\tsysLoadAvg="
				+ operatingSystemMXBean.getSystemLoadAverage()
				+ "; availableProcessors="
				+ operatingSystemMXBean.getAvailableProcessors() + "; arch="
				+ operatingSystemMXBean.getArch());
	}
}
