package at.ac.testing.brooklyn.utils;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.testing.mocks.BrooklynSchedule;
import at.ac.testing.mocks.Pool;
import at.ac.testing.mocks.SensorReading;
import at.ac.testing.mocks.SensorType;
import at.ac.testing.mocks.ThresholdBasedPolicy;
import at.ac.testing.mocks.TimedAction;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicNotificationSensor;

// Those are implemented using SAT !!?!
public class ScenarioElement {

	private static final Logger LOG = LoggerFactory
			.getLogger(ScenarioElement.class);

	// TODO Additional Constraints and Configurations
	private boolean forceBlips = false;
	private boolean forceConcurrentActions = false;
	private boolean avoidConcurrentActions = false;
	private boolean forceSustainedActions = false;

	/**
	 * Added for Evaluation 3.2
	 */
	private boolean forceConflictingActions = false;

	protected List<ScenarioAtom> scenarioAtoms;

	private int startAt = -1;
	private int startFromSize = -1;

	private Map<String, Object> configuration;

	private ThresholdBasedPolicy policy;
	private BrooklynSchedule schedule;
	private Pool pool;

	public boolean exactSize;
	public boolean isSingleElement;

	private boolean useWrongSensorPool;

	/**
	 * Added for Evalution 3.2
	 */
	public static ScenarioElement conflictingEvents() {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceConflictingActions = true;
		return scenarioElement;
	}

	/**
	 * Added for Evalution 3.2
	 */
	public static ScenarioElement conflictingEvents(ScenarioAtom... atoms) {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceConflictingActions = true;
		scenarioElement.scenarioAtoms.addAll(Arrays.asList(atoms));
		return scenarioElement;
	}

	// Provide only the backbone structure and constraints
	public static ScenarioElement resize(int from, int to) {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.resize(from, to));
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement stay() {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.stay());
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement stay(int at) {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.stay(at));
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement scaleUp() {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.scaleUp());
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement scaleUp(int to) {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.scaleUp(to));
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement scaleDown() {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.scaleDown());
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement scaleDown(int to) {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.scaleDown(to));
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement scaleOverMax() {
		// Here specify all the properties of the scenario to build !
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.scaleOverMax());
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	public static ScenarioElement scaleUnderMin() {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.add(ScenarioAtom.scaleUnderMin());
		scenarioElement.isSingleElement = true;
		return scenarioElement;
	}

	// TODO Add here back again the avoid blips and such ?
	// Sequence does not protect you from generating blips if you provide the
	// elements yourself !
	public static ScenarioElement sequence() {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.avoidConcurrentActions = true;
		return scenarioElement;
	}

	public static ScenarioElement sequence(ScenarioAtom... atoms) {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.addAll(Arrays.asList(atoms));
		scenarioElement.avoidConcurrentActions = true;
		return scenarioElement;
	}

	public static ScenarioElement sequence(List<ScenarioAtom> atoms) {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.scenarioAtoms.addAll(atoms);
		scenarioElement.avoidConcurrentActions = true;
		return scenarioElement;
	}

	public static ScenarioElement concurrentEvents() {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceConcurrentActions = true;
		return scenarioElement;
	}

	public static ScenarioElement concurrentEvents(ScenarioAtom... atoms) {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceConcurrentActions = true;
		scenarioElement.scenarioAtoms.addAll(Arrays.asList(atoms));
		return scenarioElement;
	}

	public static ScenarioElement monitoringBlip() {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceBlips = true;
		return scenarioElement;
	}

	public static ScenarioElement monitoringBlip(ScenarioAtom... atoms) {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceBlips = true;
		scenarioElement.scenarioAtoms.addAll(Arrays.asList(atoms));
		return scenarioElement;
	}

	public static ScenarioElement sustainedActions() {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceSustainedActions = true;
		return scenarioElement;
	}

	public static ScenarioElement sustainedActions(ScenarioAtom... atoms) {
		ScenarioElement scenarioElement = new ScenarioElement();
		scenarioElement.forceSustainedActions = true;
		scenarioElement.scenarioAtoms.addAll(Arrays.asList(atoms));
		return scenarioElement;
	}

	// if There are no elements ?
	public ScenarioElement doNotAddOtherElements() {
		LOG.trace("Forcing exact size");
		this.exactSize = true;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * at.ac.testing.brooklyn.utils.ScenarioElementInterface#withSensorMetric()
	 */

	public ScenarioElement withSensorMetric() {
		LOG.trace("Forcing sensor Metric.");
		for (ScenarioAtom atom : scenarioAtoms) {
			atom.withSensorMetric();
			LOG.trace("Sensor Metric to " + atom.action);
		}
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * at.ac.testing.brooklyn.utils.ScenarioElementInterface#withSensorPool()
	 */

	public ScenarioElement withSensorPool() {
		LOG.trace("Forcing sensor Pool.");

		for (ScenarioAtom atom : scenarioAtoms) {
			if (atom.action.sensorType != null) {
				atom.withSensorPool();
				LOG.trace("Sensor Pool to " + atom.action);

			}
		}
		return this;
	}

	private ScenarioElement() {
		scenarioAtoms = new ArrayList<ScenarioAtom>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see at.ac.testing.brooklyn.utils.ScenarioElementInterface#startAt(int)
	 */

	public void startAt(int time) {
		this.startAt = time;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * at.ac.testing.brooklyn.utils.ScenarioElementInterface#startFromSize(int)
	 */

	public void startFromSize(int startFrom) {
		this.startFromSize = startFrom;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * at.ac.testing.brooklyn.utils.ScenarioElementInterface#setConfiguration
	 * (java.util.Map)
	 */

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * at.ac.testing.brooklyn.utils.ScenarioElementInterface#withWrongSensorPool
	 * ()
	 */

	public ScenarioElement withWrongSensorPool() {
		this.useWrongSensorPool = true;
		return this;
	}

	public List<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>> createTheScenario() {
		pool = new Pool((Integer) configuration.get("minPoolSize"),
				(Integer) configuration.get("maxPoolSize"));
		policy = new ThresholdBasedPolicy(
				(Integer) configuration.get("metricLowerBound"),
				(Integer) configuration.get("metricUpperBound"));
		// Initialize and possibly force valid configurations !

		// TODO Not sure this is the right way to do thatt
		pool.init();
		LOG.debug("ScenarioElement.getScenarioAtoms() POOL " + pool);

		policy.init();
		LOG.debug("ScenarioElement.getScenarioAtoms() POLICY " + policy);

		schedule = new BrooklynSchedule(
				(Integer) configuration.get("resizeUpStabilizationDelay"),
				(Integer) configuration.get("resizeDownStabilizationDelay"),
				(Integer) configuration.get("minPeriodBetweenExecs"));

		// Pass the min/max size. This is to avoid to pass along another spec
		// object
		schedule.minStateSize = pool.minSize;
		schedule.maxStateSize = pool.maxSize;

		// Here we have a list of atoms that we need to convert into an array of
		// input TimedActions to initialize the schedule
		List<TimedAction> inputActions = new ArrayList<TimedAction>();
		for (ScenarioAtom inputAtom : scenarioAtoms) {
			LOG.debug("ScenarioElement.getScenarioAtoms() Parsing Atom "
					+ inputAtom);
			if (inputAtom.overMax) {
				pool.forceOverMax(inputAtom.action);
			} else if (inputAtom.underMin) {
				pool.forceUnderMin(inputAtom.action);
			}
			LOG.debug("ScenarioElement.getScenarioAtoms() Adding TimedAction "
					+ inputAtom.action);
			inputActions.add(inputAtom.action);
		}

		// Force startAt and exactSize of the scenario, that is the number of
		// element comprising it

		if (startAt != -1) {
			LOG.debug("ScenarioElement.getScenarioAtoms() Forcing START AT ");
			schedule.firstActionAt = startAt;
		}

		// This will overwrite ?
		if (startFromSize != -1) {
			schedule.startFrom = startFromSize;
		}

		if (exactSize == true) {
			LOG.debug("ScenarioElement.getScenarioAtoms() Forcing Exact size = "
					+ inputActions.size());
			schedule.size = inputActions.size();

		}

		// Those can be only active one at time !
		if (forceConcurrentActions) {
			schedule.initConcurrent(inputActions.toArray(new TimedAction[0]));
		} else if (forceSustainedActions) {
			schedule.initSustainedActions(inputActions
					.toArray(new TimedAction[0]));
		} else if (forceBlips) {
			schedule.initMonitoringBlip(inputActions
					.toArray(new TimedAction[0]));
		} else if (avoidConcurrentActions) {
			schedule.initNoConcurrent(inputActions.toArray(new TimedAction[0]));
		}
		/**
		 * Added for Evalution 3.2
		 */
		else if (forceConflictingActions) {
			schedule.initConflicting(inputActions.toArray(new TimedAction[0]));
		}
		//
		else {
			schedule.init(inputActions.toArray(new TimedAction[0]));
		}

		LOG.debug(schedule.toString());

		List<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>> scenarioAtoms = new ArrayList<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>>();
		// Iterate over the array to get the SensorReading
		// TODO Improve with 1 call for all the objects if possible
		for (TimedAction atom : schedule.actions) {

			SensorReading sensorReading = policy.resize(atom.startSize,
					atom.targetSize);

			if (atom.sensorType == SensorType.METRIC) {
				scenarioAtoms.add(//
						new SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>(//
								new SimpleImmutableEntry<AttributeSensor<? extends Number>, Number>(//
										(AttributeSensor<? extends Number>) configuration
												.get("metric"),//
										sensorReading.getMetric()),//
								atom.time));

			} else if (!useWrongSensorPool) {
				scenarioAtoms
						.add(//
						new SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>(
								//
								new SimpleImmutableEntry<BasicNotificationSensor<?>, Object>(
										//
										sensorReading
												.getPoolSensor(
														//
														atom.type, //
														(BasicNotificationSensor<?>) configuration
																.get("poolHotSensor"),//
														(BasicNotificationSensor<?>) configuration
																.get("poolColdSensor"), //
														(BasicNotificationSensor<?>) configuration
																.get("poolOkSensor")), //
										sensorReading.getSensorReading()), //
								atom.time));
			} else if (useWrongSensorPool) {
				scenarioAtoms
						.add(//
						new SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>(
								//
								new SimpleImmutableEntry<BasicNotificationSensor<?>, Object>(
										//
										sensorReading
												.getWrongPoolSensor(
														//
														atom.type, //
														(BasicNotificationSensor<?>) configuration
																.get("poolHotSensor"),//
														(BasicNotificationSensor<?>) configuration
																.get("poolColdSensor"), //
														(BasicNotificationSensor<?>) configuration
																.get("poolOkSensor")), //
										sensorReading.getSensorReading()), //
								atom.time));
			}
			// TODO Note that this is not accurate !! We need to consider the
			// concurrency and stability period as well.
			// For the proof of concept is fine
			configuration.put("endTime", atom.time);
			configuration.put("endSize", atom.targetSize);
		}

		return scenarioAtoms;
	}

}
