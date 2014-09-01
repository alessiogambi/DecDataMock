package at.ac.testing.brooklyn.evaluation3;

import static at.ac.testing.brooklyn.utils.ScenarioElement.conflictingEvents;
import static brooklyn.test.TestUtils.executeUntilSucceeds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;
import at.ac.testing.brooklyn.utils.DataMockLoadGenerator;
import at.ac.testing.brooklyn.utils.LocallyResizableEntity;
import at.ac.testing.brooklyn.utils.ScenarioAtom;
import at.ac.testing.brooklyn.utils.ScenarioHandler;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestCluster;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.internal.TimeExtras;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

/**
 * This class implement some new test cases to show the cost-effectiveness of
 * the declarative data mocking approach.
 */
public class ConflictingEventsTest {

	static {
		TimeExtras.init();
	}

	public static long TIMEOUT_MS = 10 * 1000;
	public static long SHORT_WAIT_MS = 250;
	public static long OVERHEAD_DURATION_MS = 500;
	public static long EARLY_RETURN_MS = 10;

	protected AutoScalerPolicy policy;
	TestCluster cluster;
	protected LocallyResizableEntity resizable;
	TestApplication app;

	@BeforeMethod(alwaysRun = true)
	public void setUp() throws Exception {
		app = ApplicationBuilder.newManagedApp(TestApplication.class);
		cluster = app.createAndManageChild(EntitySpec.create(TestCluster.class)
				.configure(TestCluster.INITIAL_SIZE, 1));
		resizable = new LocallyResizableEntity(cluster, cluster);
		Entities.manage(resizable);
		policy = new AutoScalerPolicy();
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

	@BeforeClass(alwaysRun = true)
	public void setUpSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@AfterMethod(alwaysRun = true)
	public void forceGC() {
		System.gc();
	}

	/**
	 * We want to understand if the application resize in the presence of
	 * conflicting actions. Conflicting actions are similar to sustained
	 * actions, but have opposite scaling effects; therefore, conflicting
	 * actions are not concurrent nor simultaneous. In fact, if they would be
	 * so, they will cancel each other out as proven by the test cases
	 * implemented by testConcurrent* methods of
	 * {@link at.ac.testing.brooklyn.original.AutoScalerPolicyTest}
	 * 
	 * According to the specification every conflicting action will reset it's
	 * stabilityPeriod, so if we have a sequence of conflicting actions, we must
	 * check that the application does not resize until the last stabilityPeriod
	 * goes off
	 */
	@Test
	public void testConflictingGrowShrink() {
		resizable.removePolicy(policy);

		final long resizeUpStabilizationDelay = 2000L;
		final long resizeDownStabilizationDelay = 3000L;
		long minPeriodBetweenExecs = 0;

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);

		resizable.resizeSleepTime = 250;
		resizable.resize(3);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(2 * 10)
				.metricUpperBound(2 * 20)
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
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
						conflictingEvents(ScenarioAtom.resize(3, 5),
								ScenarioAtom.resize(3, 1)).withSensorPool()
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		mockedDataGeneration.execute();
		// At this point we publish all the data
		// and we need to check that the new
		// resize will take place more or less
		// after the stability period
		long endOfExecution = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		// Use the same pattern of the other test cases
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						assertTrue(resizable.getCurrentSize() < 3,
								"currentSize=" + resizable.getCurrentSize());
					}
				});
		assertEquals(resizable.getCurrentSize(), (Integer) 1,
				"Wrong Final STATE !");

		long delayForReconfiguration = stopwatch.elapsed(TimeUnit.MILLISECONDS)
				- endOfExecution;

		// The last operation was a ScaleDown
		assertTrue(delayForReconfiguration >= resizeDownStabilizationDelay
				- EARLY_RETURN_MS
				&& delayForReconfiguration <= resizeDownStabilizationDelay
						+ OVERHEAD_DURATION_MS,
				"Resizing to Final State: time=" + delayForReconfiguration
						+ "; resizeDownStabilizationDelay="
						+ resizeDownStabilizationDelay);

		// Check that only that reconfiguration took place !
		Assert.assertEquals(resizable.sizes, ImmutableList.of(3, 1));
	}

	@Test
	public void testConflictingShrinkGrow() {
		resizable.removePolicy(policy);

		final long resizeUpStabilizationDelay = 2000L;
		final long resizeDownStabilizationDelay = 3000L;
		long minPeriodBetweenExecs = 0;

		policy = AutoScalerPolicy.builder()
				.resizeDownStabilizationDelay(resizeDownStabilizationDelay)
				.resizeUpStabilizationDelay(resizeUpStabilizationDelay)
				.minPeriodBetweenExecs(minPeriodBetweenExecs).build();
		resizable.addPolicy(policy);

		resizable.resizeSleepTime = 250;
		resizable.resize(3);

		ScenarioHandler scenarioHandler = new ScenarioHandler.Builder()
				.metricLowerBound(2 * 10)
				.metricUpperBound(2 * 20)
				.resizeDownStabilizationDelay(
						(int) TimeUnit.MILLISECONDS
								.toSeconds(resizeDownStabilizationDelay))
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
						conflictingEvents(ScenarioAtom.resize(3, 1),
								ScenarioAtom.resize(3, 5)).withSensorPool()
								.doNotAddOtherElements()).build();
		resizable.addEnricher(mockedDataGeneration);

		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start();

		mockedDataGeneration.execute();
		// At this point we publish all the data
		// and we need to check that the new
		// resize will take place more or less
		// after the stability period
		long endOfExecution = stopwatch.elapsed(TimeUnit.MILLISECONDS);

		// Use the same pattern of the other test cases
		executeUntilSucceeds(MutableMap.of("period", 1, "timeout", TIMEOUT_MS),
				new Runnable() {
					public void run() {
						assertTrue(resizable.getCurrentSize() > 3,
								"currentSize=" + resizable.getCurrentSize());
					}
				});
		assertEquals(resizable.getCurrentSize(), (Integer) 5,
				"Wrong Final STATE !");

		long delayForReconfiguration = stopwatch.elapsed(TimeUnit.MILLISECONDS)
				- endOfExecution;

		// The last operation was a ScaleDown
		assertTrue(delayForReconfiguration >= resizeUpStabilizationDelay
				- EARLY_RETURN_MS
				&& delayForReconfiguration <= resizeUpStabilizationDelay
						+ OVERHEAD_DURATION_MS,
				"Resizing to Final State: time=" + delayForReconfiguration
						+ "; resizeUpStabilizationDelay="
						+ resizeUpStabilizationDelay);

		Assert.assertEquals(resizable.sizes, ImmutableList.of(3, 5));
	}
}
