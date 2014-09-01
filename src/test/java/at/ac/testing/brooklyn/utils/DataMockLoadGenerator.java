package at.ac.testing.brooklyn.utils;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import brooklyn.config.ConfigKey;
import brooklyn.enricher.basic.AbstractEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.event.Sensor;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.policy.autoscaling.AutoScalerPolicyMetricTest;
import brooklyn.util.collections.MutableMap;

import com.google.common.base.Stopwatch;

/**
 * This class represents an integration point between the Brooklyn framework and
 * the declarative data mocking part. The class receives at construction a
 * scenario to be executed, which is a timed trace of monitoring objects or
 * messages to be emitted at the given time during the test execution. There are
 * two basic modalities:
 * <ul>
 * <li>unfold : this allows the test execution to block until the next (single)
 * message is published</li>
 * <li>execute: this allows the test execution to block until all the scenario
 * develops
 * </ul>
 * 
 * The two modalities allow testers to define and check conditions over
 * transient states, or entire executions by possible means of trace checking.
 * 
 * <b>This class is not ThreadSafe</b>
 * 
 * TODO: Understand if it is better to link the enricher to the policy at
 * constructor, inside the test, or while calling the unfold/execute.
 * 
 * Pros/cons - At constructor : easy to setup, but easy to forget / hidden,
 * might be difficult to track down if something goes wrong - Inside the test:
 * explicit, but easy to forget to do, moreover is repetitive - but it part of
 * the setup - While calling the unfold: makes the Enricher general vs using
 * directly tc.emit/tc.setAttribute inside the test cases. - makes it possible
 * to publish multiple times the same data to different sources, or the
 * different data to different sources. but it is not clear if this is a real
 * requirement or just gold plating ! = In the future the target entity might
 * become part of the specification, in that case also the target is needed.
 */
public class DataMockLoadGenerator extends AbstractEnricher {
	private static final Logger LOG = LoggerFactory.getLogger(DataMockLoadGenerator.class);

	// Already included in the scenario !
	// public static final ConfigKey<Sensor> METRIC_SENSOR =
	// ConfigKeys.newConfigKey(new TypeToken<Sensor>() {}, "targetSensor");

	// TODO Specify a default, empty list of sensor reading !
	public static final ConfigKey<List<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>>> SCENARIO =
		(ConfigKey<List<SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>>>) ConfigKeys
			.newConfigKey(( (List) new ArrayList<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>>()).getClass(), "scenario", "", null);

	public static final ConfigKey<TimeUnit> TIME_UNIT = ConfigKeys.newConfigKey(TimeUnit.class, "timeUnit", "Default to second", TimeUnit.SECONDS);

	public static final ConfigKey<Entity> ENTITY_WITH_METRIC = BasicConfigKey.builder(Entity.class).name("entityWithMetric").build();

	// Transient, generated on the fly!
	private ScheduledExecutorService scheduledExecutor;
	private Iterator<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>> actionsToRun;
	private Stopwatch stopwatch;

	public DataMockLoadGenerator() {
		this(MutableMap.<String, Object> of());
	}

	// TODO Once a default value for scenario is set refactor and remove the
	// if/then/else
	public DataMockLoadGenerator(Map<String, ?> props) {
		// This is not really working !
		super(props);

		if (getRequiredConfig(SCENARIO) != null) {
			actionsToRun = getRequiredConfig(SCENARIO).iterator();
		} else {
			LOG.warn("Not scenario was provided to the monitoring data generation component. Empty iterator.");
			actionsToRun = (new ArrayList<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>>()).iterator();
		}
		stopwatch = Stopwatch.createUnstarted();
	}

	// FIXME Return an immutable copy or something
	public List<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>> getScenario() {
		return getRequiredConfig(SCENARIO);
	}

	/**
	 * Executes the next task, if any, and blocks until that is finished. If the
	 * sequence of monitoring data cannot be published according to the original
	 * schedule, that is, if an unfold comes too late, this method will generate
	 * a SkipException to skip the running test because since the monitoring
	 * data is produced at a different time, the intended temporal properties
	 * might be not guaranteed. This usually happens if the check implemented in
	 * the test case uses some form of object polling with a delay/timeout
	 * 
	 * TODO Must this wait for the actuation time before returing to the test ?
	 */
	public void unfoldStrict() {
		// If an entity with metric was defined, use that, otherwise default to
		// the target entity
		executedUnfold(true, (getConfig(ENTITY_WITH_METRIC) != null) ? (EntityLocal) getConfig(ENTITY_WITH_METRIC) : entity);
	}

	/**
	 * Similar to {@link DataMockLoadGenerator#unfoldStrict()} but it warns the
	 * user instead of raising the SkipException
	 * 
	 * TODO Must this wait for the actuation time before returning to the test ?
	 */
	public void unfold() {
		executedUnfold(false, (getConfig(ENTITY_WITH_METRIC) != null) ? (EntityLocal) getConfig(ENTITY_WITH_METRIC) : entity);
	}

	/**
	 * Similar to the one before, but force the data to be published to another
	 * entity local. This is to support
	 * {@link AutoScalerPolicyMetricTest#testSubscribesToMetricOnSpecifiedEntity()}
	 */
	public void unfoldStrict(Entity entity) {
		executedUnfold(true, (EntityLocal) entity);
	}

	/**
	 * Similar to the one before, but force the data to be published to another
	 * entity local. This is to support
	 * {@link AutoScalerPolicyMetricTest#testSubscribesToMetricOnSpecifiedEntity()}
	 */
	public void unfold(Entity entity) {
		executedUnfold(false, (EntityLocal) entity);
	}

	// Must be EntityLocal otherwise the emit method is NOT defined !
	private void executedUnfold(boolean strict, final EntityLocal sourceOfSensorData) {
		// get the first action in the scenario and execute it
		// blocks until that happen (included the delay to get there, and the
		// delay after that if any)
		// This accounts for example for the assumed actuation time ?
		// Will it fail if we already passed the right period ?
		// For example, if we block the execution long enough to break the
		// intended properties ?
		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		if (actionsToRun.hasNext()) {
			// TODO Check if there is enough time left !!
			// TODO TimeManagement and StopWatch or so !
			// TODO Refactor the cose !
			AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer> nextMonitoringValue = actionsToRun.next();
			final AbstractMap.SimpleImmutableEntry monitoringData = nextMonitoringValue.getKey();

			Integer delaySinceTheBeginningInAbstractTimeUnit = nextMonitoringValue.getValue();
			long actualDelay = -1;

			// Avoid the 0 delay at the beginning by starting the Stopwatch only
			// after computing the delay
			if (!stopwatch.isRunning()) {
				actualDelay = delaySinceTheBeginningInAbstractTimeUnit;
				stopwatch.start();
			} else {
				actualDelay = delaySinceTheBeginningInAbstractTimeUnit - stopwatch.elapsed(getConfig(TIME_UNIT));
			}

			if (actualDelay < 0) {
				if (strict) {
					LOG.warn("Late Unfold in strict mode {}", new Object[] { Math.abs(actualDelay) });
					// This is a failed/skipped test !
					throw new SkipException("Too much delay between consecutive UNFOLDS. The assumption on the workload might not be met, and the test will be skipped !");
				} else {
					LOG.warn("Too much delay between consecutive UNFOLDS {}." + "The assumptions on the workload might not be met!", new Object[] { Math.abs(actualDelay) });
				}
			}

			LOG.debug("Schedule {} to be published @{} which is in {} {} Sensor {} on Channel/Entity {}", new Object[] { monitoringData.getValue(), delaySinceTheBeginningInAbstractTimeUnit,
					actualDelay, getConfig(TIME_UNIT), monitoringData.getKey(), sourceOfSensorData });

			scheduledExecutor.schedule(new Runnable() {
				@Override
				public void run() {
					LOG.warn("Publish {} on {}", new Object[] { monitoringData, sourceOfSensorData });
					// Publish the monitoring value
					sourceOfSensorData.emit((Sensor) monitoringData.getKey(), monitoringData.getValue());
				}
			}, actualDelay, getConfig(TIME_UNIT));

			// Block until the execution completes
			try {
				scheduledExecutor.shutdown();
				scheduledExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} else {
			LOG.warn("No more monitoring data to publish!");
		}
	}

	/**
	 * Executes all the remaining tasks, if any, and blocks until all of them
	 * are finished
	 */
	public void execute() {
		this.execute((getConfig(ENTITY_WITH_METRIC) != null) ? (EntityLocal) getConfig(ENTITY_WITH_METRIC) : entity);
	}

	public void execute(final EntityLocal sourceOfSensorData) {
		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		while (actionsToRun.hasNext()) {
			AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer> nextMonitoringValue = actionsToRun.next();
			final AbstractMap.SimpleImmutableEntry monitoringData = nextMonitoringValue.getKey();
			Integer delaySinceTheBeginningInAbstractTimeUnit = nextMonitoringValue.getValue();
			long actualDelay = -1;

			// Avoid the 0 delay at the beginning by starting the Stopwatch only
			// after computing the delay
			if (!stopwatch.isRunning()) {
				actualDelay = delaySinceTheBeginningInAbstractTimeUnit;
				stopwatch.start();
			} else {
				actualDelay = delaySinceTheBeginningInAbstractTimeUnit - stopwatch.elapsed(getConfig(TIME_UNIT));
			}

			LOG.debug("Schedule {} to be published @{} which is in {} {} Sensor {} on Channel/Entity {}", new Object[] { monitoringData.getValue(), delaySinceTheBeginningInAbstractTimeUnit,
					actualDelay, getConfig(TIME_UNIT), monitoringData.getKey(), sourceOfSensorData });

			scheduledExecutor.schedule(new Runnable() {
				@Override
				public void run() {
					LOG.debug("Publish {}", new Object[] { monitoringData });
					// Publish the monitoring value
					// TODO Not sure this is the correct way to do that !
					sourceOfSensorData.emit((Sensor) monitoringData.getKey(), monitoringData.getValue());
				}
			}, actualDelay, getConfig(TIME_UNIT));
		}
		// Block until the execution completes
		try {
			scheduledExecutor.shutdown();
			scheduledExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void destroy() {
		scheduledExecutor.shutdownNow();
		super.destroy();
	}

}
