package at.ac.testing.mocks;

import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Resizable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.policy.autoscaling.LocallyResizableEntity;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestCluster;

import com.google.common.collect.ImmutableMap;

public class ThresholdPolicyTest {

	private static final Logger LOG = LoggerFactory
			.getLogger("at.ac.testing.mocks.Test");

	private static final Logger LOG_TEST = LoggerFactory.getLogger("TEST");

	private static final Logger UNSAT_LOG = LoggerFactory.getLogger("UNSAT");

	private static final Logger WRONG_LOG = LoggerFactory.getLogger("WRONG");

	@BeforeClass
	public static void setupSolver() {
		LogMap.SolverOpt_IntBitWidth = 9;// 8 is too small, and I guess it
											// cannot generates enough model
											// elements
		// TODO We need to confirm the hyphothesis that increasing bit is ok !
		// AND ALSO CONSIDER THE COST/OVERHEAD !!!

		// LogMap.SolverOpt_IntBitWidth = 11 Is too expensive, circa 10 sec for
		// each invocation of the SAT !!

		/*
		 * 
		 * 11:44:35.437 [main] DEBUG TEST - policy.resize(19,11) 11:44:35.438
		 * [main] DEBUG TEST - policy.getCommonDivisors() [1, 2, 5, 10]
		 * 11:44:35.438 [main] DEBUG TEST - policy.validMultipliers() [1, 2]
		 * 11:44:35.438 [main] DEBUG TEST - ALL VALUES [3] 11:44:35.438 [main]
		 * DEBUG TEST - RESULT Sensor [Value: 4, Multiplier 2] --> 8
		 * 11:44:45.442 [main] INFO WRONG - [10,50] 19 --> 11, with 8 got 15
		 * 
		 * 11:44:53.542 [main] DEBUG TEST - policy.resize(19,13) 11:44:53.542
		 * [main] DEBUG TEST - policy.getCommonDivisors() [1, 2, 5, 10]
		 * 11:44:53.542 [main] DEBUG TEST - policy.validMultipliers() [1]
		 * 11:44:53.542 [main] DEBUG TEST - ALL VALUES [7] 11:44:53.542 [main]
		 * DEBUG TEST - RESULT Sensor [Value: 9, Multiplier 1] --> 9
		 * 11:45:03.545 [main] INFO WRONG - [10,50] 19 --> 13, with 9 got 17
		 * 
		 * 11:46:16.947 [main] DEBUG TEST - policy.resize(20,12) 11:46:16.947
		 * [main] DEBUG TEST - policy.getCommonDivisors() [1, 2, 5, 10]
		 * 11:46:16.947 [main] DEBUG TEST - policy.validMultipliers() [1, 2]
		 * 11:46:16.947 [main] DEBUG TEST - ALL VALUES [3] 11:46:16.947 [main]
		 * DEBUG TEST - RESULT Sensor [Value: 4, Multiplier 2] --> 8
		 * 11:46:26.948 [main] INFO WRONG - [10,50] 20 --> 12, with 8 got 16
		 * 
		 * 11:46:35.066 [main] DEBUG TEST - policy.resize(20,14) 11:46:35.066
		 * [main] DEBUG TEST - policy.getCommonDivisors() [1, 2, 5, 10]
		 * 11:46:35.066 [main] DEBUG TEST - policy.validMultipliers() [1]
		 * 11:46:35.066 [main] DEBUG TEST - ALL VALUES [7] 11:46:35.066 [main]
		 * DEBUG TEST - RESULT Sensor [Value: 8, Multiplier 1] --> 8
		 * 11:46:45.068 [main] INFO WRONG - [10,50] 20 --> 14, with 8 got 16
		 */
	}

	private static long TIMEOUT_MS = 10 * 1000;

	int low = 10;
	int high = 50;

	AutoScalerPolicy policy;

	static final AttributeSensor<Integer> MY_ATTRIBUTE = Sensors
			.newIntegerSensor("autoscaler.test.intAttrib");
	TestCluster cluster;
	LocallyResizableEntity resizable;
	TestApplication app;

	@BeforeMethod(alwaysRun = true)
	public void setUp() throws Exception {
		app = ApplicationBuilder.newManagedApp(TestApplication.class);
		cluster = app.createAndManageChild(EntitySpec.create(TestCluster.class)
				.configure(TestCluster.INITIAL_SIZE, 1));
		resizable = new LocallyResizableEntity(cluster, cluster);
		Entities.manage(resizable);
		policy = new AutoScalerPolicy.Builder().metric(MY_ATTRIBUTE)
				.metricLowerBound(low).metricUpperBound(high).build();
		// policy = new AutoScalerPolicy();
		resizable.addPolicy(policy);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		if (policy != null)
			policy.destroy();
		if (app != null)
			Entities.destroyAll(app.getManagementContext());
		cluster = null;
		resizable = null;
		policy = null;
	}

	static Map<String, Object> message(int currentSize, double currentWorkrate,
			double lowThreshold, double highThreshold) {
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
				assertEquals(resizable.getCurrentSize(), desired);
			}
		};
	}

	// @Test
	public void testIntValuesCanScaleItDown() throws Exception {
		// resizable.resize(21);
		// Both 1 and 0 failes because the "right" value should be > 0 < 1 (0.6)
		// And cannot be found with INTEGERS
		// resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, message(21,
		// 1, 10, 50));
		// resizable.emit(AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR, message(21,
		// 0, 10, 50));
		//
		// expect pool to shrink to 3 (i.e. maximum to have >= 40 per container)
		// executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
		// currentSizeAsserter(resizable, 1));

		low = 10;
		high = 20;
		this.policy.setMetricLowerBound(low);
		this.policy.setMetricUpperBound(high);

		for (int value : new int[] { 1 }) {
			resizable.resize(15);
			LOG.debug(" \n\n Publish " + value);
			resizable.emit(MY_ATTRIBUTE, value); // Scales to 18
			executeUntilSucceeds(ImmutableMap.of("timeout", TIMEOUT_MS),
					currentSizeAsserter(resizable, 1));
		}
		// resizable.emit(MY_ATTRIBUTE, 8); // Scales to 16

	}

	@Test
	public void performanceOptimization() {
		LogMap.SolverOpt_IntBitWidth = 9;

		low = 50;
		high = 100;
		int currentSize = 6;
		int targetSize = 20;
		ThresholdBasedPolicy policy = new ThresholdBasedPolicy(low, high);
		int multiplier = Collections.max(policy.validMultipliers(currentSize,
				targetSize));
		System.out
				.println("ThresholdPolicyTest.performanceOptimization() multiplier  "
						+ multiplier
						+ " "
						+ policy.validMultipliers(currentSize, targetSize));
		System.out
				.println("ThresholdPolicyTest.performanceOptimization() multiplier  "
						+ multiplier
						+ " "
						+ policy.validMultipliers(currentSize, targetSize));
	}

	@Test
	public void testScheduleInitializationOverHeadNonOverlapping() {
		double unsats = 0;
		double wrongs = 0;
		double total = 0;

		// Some Non overlapping regions that allows for some int value inside ;)
		// 10-14, 20-28, "30-42", 40-56
		low = 10;
		high = 20;
		ThresholdBasedPolicy policy = new ThresholdBasedPolicy(low, high);

		policy.MAX_INT = (int) Math.pow(2, LogMap.SolverOpt_IntBitWidth) / 2 - 1;

		this.policy.setMetricLowerBound(low);
		this.policy.setMetricUpperBound(high);

		// 3 => 1, 3 => 2 not working right !
		// for (int i = 1; i <= 20; i++) {
		for (int i : new int[] { 3 }) {
			// for (int j = 1; j <= 20; j++) {
			for (int j : new int[] { 3, 2, 1 }) {
				SensorReading sr = null;
				try {
					LOG.debug("policy.resize(" + i + "," + j + ")  ");
					sr = policy.resize(i, j);

					LOG.debug("policy.getAllResize(" + i + "," + j + ") "
							+ policy.getAllResize(i, j));
					LOG.debug("sr.multiplier " + sr.multiplier);
					int multiplier = sr.multiplier;
					for (Integer value : policy.getAllResize(i, j)) {
						resizable.resize(i);
						//
						// Summary
						// LOG.debug("policy.resize(" + i + "," + j + ") ");
						// LOG.debug("policy.getCommonDivisors() " +
						// policy.getCommonDivisors(low, high));
						// LOG.debug("policy.validMultipliers() " +
						// policy.validMultipliers(i, j));
						// LOG.debug("\t ALL VALUES " +
						// policy.getAllResize(i, j));
						// LOG.debug("\t RESULT " + sr + " --> " +
						// sr.getMetric());
						LOG.debug("\n\nUse " + (value * multiplier) + "\n\n");
						// resizable.emit(MY_ATTRIBUTE, (Integer)
						// sr.getMetric());
						// resizable.emit(MY_ATTRIBUTE, (value * multiplier));
						Map<String, Object> theMap = new HashMap<String, Object>();
						theMap.put(AutoScalerPolicy.POOL_CURRENT_SIZE_KEY, i);
						// TODO Note this one !!
						theMap.put(AutoScalerPolicy.POOL_CURRENT_WORKRATE_KEY,
								(double) (value * multiplier));
						theMap.put(AutoScalerPolicy.POOL_LOW_THRESHOLD_KEY,
								(double) low);
						theMap.put(AutoScalerPolicy.POOL_HIGH_THRESHOLD_KEY,
								(double) high);

						if (i == j) {
							resizable.emit(
									AutoScalerPolicy.DEFAULT_POOL_OK_SENSOR,
									theMap);
						} else if (i > j) {
							resizable.emit(
									AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR,
									theMap);
						} else {
							resizable.emit(
									AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR,
									theMap);
						}

						executeUntilSucceeds(
								ImmutableMap.of("timeout", TIMEOUT_MS),
								currentSizeAsserter(resizable, j));
						total++;
					}
				} catch (SkipException e) {
					LOG.info("MISSING PRECONDITIONS !" + e.getMessage());
					UNSAT_LOG.info("MISSING PRECONDITIONS : [{},{}] {} --> {}",
							new Object[] { low, high, i, j });
					total++;
					unsats++;
				} catch (Exception e) {
					Assert.fail("Exception", e);
				} catch (Throwable e) {
					if (e.getMessage().contains("UNSAT")) {
						UNSAT_LOG.info("[{},{}] {} --> {}", new Object[] { low,
								high, i, j });
						unsats++;
						total++;
					} else {
						WRONG_LOG.info("[{},{}] {} --> {}, with {} got {} ",
								new Object[] { low, high, i, j, sr.getMetric(),
										resizable.getCurrentSize() });
						wrongs++;
						total++;
					}
				}
			}
		}
		LOG.info("SUMMARY OF THE TEST: IntBitWidth = "
				+ LogMap.SolverOpt_IntBitWidth);
		LOG.info("\t Total {} - Ok {} - Unsat {} - Wrong {}", new Object[] {
				total, (total - wrongs - unsats), unsats, wrongs });
		LOG.info("\t Total {} - Ok {} - Unsat {} - Wrong {}", new Object[] {
				total / total, (total - wrongs - unsats) / total,
				unsats / total, wrongs / total });
		Assert.assertTrue(wrongs == 0, "There are wrong values !");
	}

	@Test
	public void testScheduleInitializationOverHeadOverlapping() {
		double unsats = 0;
		double wrongs = 0;
		double total = 0;
		// Overlapping all over (except 0)
		low = 10;
		high = 50;

		ThresholdBasedPolicy policy = new ThresholdBasedPolicy(low, high);

		policy.MAX_INT = (int) Math.pow(2, LogMap.SolverOpt_IntBitWidth) / 2 - 1;

		this.policy.setMetricLowerBound(low);
		this.policy.setMetricUpperBound(high);
		try {
			// for (int i = 0; i <= 20; i++) {
			for (int i : new int[] { 4 }) {
				// for (int j = 0; j <= 20; j++) {
				for (int j : new int[] { 8 }) {
					SensorReading sr = null;
					try {
						LOG.debug("policy.resize(" + i + "," + j + ") ");

						resizable.resize(i);

						sr = policy.resize(i, j);

						// Summary
						LOG.debug("policy.resize(" + i + "," + j + ") ");
						LOG.debug("policy.getCommonDivisors() "
								+ policy.getCommonDivisors(low, high));
						LOG.debug("policy.validMultipliers() "
								+ policy.validMultipliers(i, j));
						LOG.debug("\t ALL VALUES " + policy.getAllResize(i, j));
						LOG.debug("\t RESULT " + sr + " --> " + sr.getMetric());
						resizable.emit(MY_ATTRIBUTE, (Integer) sr.getMetric());
						executeUntilSucceeds(
								ImmutableMap.of("timeout", TIMEOUT_MS),
								currentSizeAsserter(resizable, j));
						total++;
					} catch (SkipException e) {
						LOG.info("MISSING PRECONDITIONS !" + e.getMessage());
						UNSAT_LOG.info(
								"MISSING PRECONDITIONS : [{},{}] {} --> {}",
								new Object[] { low, high, i, j });
						total++;
						unsats++;
					} catch (Exception e) {
						Assert.fail("Exception", e);
					} catch (Throwable e) {
						if (e.getMessage().contains("UNSAT")) {
							UNSAT_LOG.info("[{},{}] {} --> {}", new Object[] {
									low, high, i, j });
							total++;
							unsats++;
						} else {
							WRONG_LOG.info(
									"[{},{}] {} --> {}, with {} got {} ",
									new Object[] { low, high, i, j,
											sr.getMetric(),
											resizable.getCurrentSize() });
							total++;
							wrongs++;
						}
					}

				}
			}
		} finally {
			LOG.info("SUMMARY OF THE TEST: IntBitWidth = "
					+ LogMap.SolverOpt_IntBitWidth);
			LOG.info("\t Total {} - Ok {} - Unsat {} - Wrong {}", new Object[] {
					total, (total - wrongs - unsats), unsats, wrongs });
			LOG.info("\t Total {} - Ok {} - Unsat {} - Wrong {}", new Object[] {
					total / total, (total - wrongs - unsats) / total,
					unsats / total, wrongs / total });
		}
	}
}
