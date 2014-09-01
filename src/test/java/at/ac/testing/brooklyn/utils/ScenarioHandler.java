package at.ac.testing.brooklyn.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicNotificationSensor;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.collections.MutableMap;

// TODO This assume for simplicity of the proof of concept that there is only ThresholdBased Policy, Plain Schedule, and a Pool object
// In the future we can extend it to take specification objects like Policy, Schedule, Pool or something.
// Each of them is configured according to it own builder stuff

// FIXME Define a fluent API that works on ScenarioElements, and ScenarioStarters. Each of those Elements have a type (ScaleUp, Stay, Blip, Concurrent, etc)
// Elements are appended one after the other and result in "small chains" of TimedActions that once merged will retuirn the "entire" scenario.

/*
 * Something Like
 * 	ScenarioHandler.BUilder.startWith( NextScenarioElement ).AtTime(0).FromSize(1).then( NextScenarioElement ).withDelay().then( NextScenarioElement ).build();
 * 
 * 	build will go over the list of then(s) and call Scenario.init() and merge( current, scenario.init() ) by setting all the relevant stuff (startAtTime, endAt) etc.
 * One thing to note
 * 	then() implies that NextScenarioElement.targetSize==NextScenarioElement.startSize and that NextScenarioElement.startAt = NextScenarioElement.Element.endAt (duration+additionalDealys for actuationTime !)  
 * 
 * 
 *  Not sure where to put AtTime etc.
 *  
 *  
 */

public class ScenarioHandler implements ScenarioStarter {

	private static final Logger LOG = LoggerFactory
			.getLogger(ScenarioHandler.class);

	private ScenarioBuilderImpl builder = new ScenarioBuilderImpl();

	private List<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>> scenario;
	// private AttributeSensor<? extends Number> metric;
	// private TimeUnit timeUnit;
	// private Entity entityWithMetric;

	private Map<String, Object> configuration;

	// This encapsulates the logic to obtain the scenario elements
	private class ScenarioBuilderImpl implements ScenarioBuilder,
			AtFromScenarioStarter, AtScenarioStarter, FromScenarioStarter,
			NextScenarioElement {

		private int startAtTime = -1;
		private int startFromSize = -1;

		private List<ScenarioElement> elements;

		@Override
		public AtFromScenarioStarter at(int time) {
			this.startAtTime = time;
			return this;
		}

		@Override
		public AtFromScenarioStarter from(int size) {
			this.startFromSize = size;
			return this;
		}

		@Override
		public NextScenarioElement startWith(ScenarioElement scenarioElement) {
			elements = new ArrayList<ScenarioElement>();
			scenarioElement.startAt(startAtTime);
			scenarioElement.startFromSize(startFromSize);
			elements.add(scenarioElement);
			return this;
		}

		@Override
		public NextScenarioElement then(ScenarioElement scenarioElement) {
			elements.add(scenarioElement);
			return this;
		}

		// All the consecutive scenarios made of
		// single atoms must be merged into sequence()
		private List<ScenarioElement> preprocessElements() {
			List<ScenarioElement> preprocessedElements = new ArrayList<ScenarioElement>();
			List<ScenarioAtom> atomSequence = new ArrayList<ScenarioAtom>();

			for (ScenarioElement element : elements) {
				if (element.isSingleElement) {
					// Accumulate
					atomSequence.addAll(element.scenarioAtoms);
				} else {
					// Either "create new and copy" or "copy"
					if (atomSequence.size() > 0) {
						// Add the new one a
						preprocessedElements.add(ScenarioElement
								.sequence(atomSequence));
						// Reset the states
						atomSequence.clear();
					}
					// Copy the original one
					preprocessedElements.add(element);
				}
			}

			// Add the default one if no one was done before.
			// Maintain size and startTime !
			if (atomSequence.size() > 0) {
				// Add the new one a
				ScenarioElement firstSequence = ScenarioElement
						.sequence(atomSequence);
				if (startAtTime != -1) {
					firstSequence.startAt(startAtTime);
				}
				if (startFromSize != -1) {
					firstSequence.startFromSize(startFromSize);
				}
				preprocessedElements.add(firstSequence);
				// Reset the states
				atomSequence.clear();
			}
			return preprocessedElements;
		}

		/**
		 * Return an Enricher policy that can be linked to the target entity and
		 * that will generate monitoring data according to the specifications
		 * 
		 * @return
		 * 
		 */
		@Override
		public DataMockLoadGenerator build() {
			int previousEndTime = -1;
			int previousEndSize = -1;

			// Do some preprocessing
			List<ScenarioElement> preprocessedElements = preprocessElements();

			scenario = new ArrayList<AbstractMap.SimpleImmutableEntry<AbstractMap.SimpleImmutableEntry, Integer>>();

			// Then process them normally !
			for (ScenarioElement element : preprocessedElements) {
				// Fill up the instance with the generic configurations
				element.setConfiguration(configuration);

				// ScenarioStarter
				if (previousEndSize != -1) {
					element.startFromSize(previousEndSize);
				}
				if (previousEndTime != -1) {
					element.startAt(previousEndTime);
				}

				// Get the TimedActions !
				scenario.addAll(element.createTheScenario());

				// End State and End Time
				// TODO This is not yet accurate but for the proof of concept is
				// fine
				previousEndTime = (Integer) configuration.get("endTime");
				previousEndSize = (Integer) configuration.get("endSize");
			}

			Map<String, ?> props = MutableMap.<String, Object> builder()//
					// From the Outside if any
					.putIfNotNull("metric", configuration.get("metric"))//
					.putIfNotNull("timeUnit", configuration.get("timeUnit"))//
					.putIfNotNull("entityWithMetric",
							configuration.get("entityWithMetric"))//
					// Built here
					.putIfNotNull("scenario", scenario)//
					.build();
			return new DataMockLoadGenerator(props);
			//
			// System.out.println("\n The Configuration : " + configuration);
			//
			//
			// /*
			// * private AttributeSensor<? extends Number> metric; private
			// * BasicNotificationSensor<?> poolHotSensor; private
			// * BasicNotificationSensor<?> poolColdSensor; private
			// * BasicNotificationSensor<?> poolOkSensor;
			// */
			//
			//
			// // To solve initialization and integration efforts we create an
			// // EnricherSpec that we bind to the targetEntity
			// // and return an object to control the execution of the scenario
			// // from within the test case
			//
			// // ScenarioHandler handler = new ScenarioHandler(scenario);
			//
			// // Wire this with the target entity
			// // ((Entity)
			// //
			// configuration.get("targetEntity")).addEnricher(EnricherSpec.create(DataMockLoadGenerator.class)
			// // .configure(DataMockLoadGenerator.TARGET_SENSOR,
			// // (AttributeSensor) configuration.get("targetSensor"))
			// // .configure(DataMockLoadGenerator.TIME_UNIT, (TimeUnit)
			// //
			// configuration.get("timeUnit")).configure(DataMockLoadGenerator.SCENARIO,
			// // handler));

			//
			// } catch (java.lang.AssertionError e) {
			// // UNSAT
			// throw new
			// SkipException("Some conditions prevent the generation of the specified scenario !"
			// + e.getMessage());
			// }
			// }
			// }
		}
	}

	public ScenarioHandler(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	// This is to spin up the thing
	@Override
	public AtScenarioStarter at(int time) {
		return (AtScenarioStarter) builder.at(time);
	}

	public FromScenarioStarter from(int size) {
		return (FromScenarioStarter) builder.from(size);
	};

	/**
	 * Configure and build the Scenario Handler. TODO Maybe change the name to
	 * something else. It is meant to capture the general properties and
	 * configurations, while the scenario builder is to build the different
	 * pieces of workload/scenario. It also provides the default values !
	 * 
	 * @return
	 */
	public static Builder builder() {
		return new Builder();
	}

	// TODO Keep only the relevant options to show the integration
	public static class Builder {
		// If present this entity with metric is the source of the data,
		// so we need to publish to it instead of "entity" that is the
		// target entity of the policy and by default is also the source of the
		// data
		private Entity entityWithMetric;

		// For sure
		// Being not valid numbers this will result in the fall back to the SAT
		// to make them
		// valid !
		private Number metricUpperBound = -1;
		private Number metricLowerBound = -1;

		private int minPoolSize = -1;
		private int maxPoolSize = -1;// Integer.MAX_VALUE;

		private int minPeriodBetweenExecs = 100; // MSEC
		private int resizeUpStabilizationDelay = 0;
		private int resizeDownStabilizationDelay = 0;
		private int actuationTime = 0;
		private int firstActionAtTime = 0;

		private TimeUnit timeUnit = TimeUnit.MILLISECONDS; // Default value

		// Output values -

		// Really I have all the SENSORS then I must choose which one to publish
		// ?
		private AttributeSensor<? extends Number> metric;
		private BasicNotificationSensor<?> poolHotSensor = AutoScalerPolicy.DEFAULT_POOL_HOT_SENSOR;
		private BasicNotificationSensor<?> poolColdSensor = AutoScalerPolicy.DEFAULT_POOL_COLD_SENSOR;
		private BasicNotificationSensor<?> poolOkSensor = AutoScalerPolicy.DEFAULT_POOL_OK_SENSOR;

		public ScenarioHandler build() {
			Map<String, Object> configuration = MutableMap
					.<String, Object> builder()//
					// Output data/objects
					.putIfNotNull("metric", metric)//
					.putIfNotNull("poolHotSensor", poolHotSensor)//
					.putIfNotNull("poolColdSensor", poolColdSensor)//
					.putIfNotNull("poolOkSensor", poolOkSensor)//
					// Output
					.putIfNotNull("actuationTime", actuationTime)//
					.putIfNotNull("targetEntity", entityWithMetric)//
					.putIfNotNull("metricUpperBound", metricUpperBound)//
					.putIfNotNull("metricLowerBound", metricLowerBound)//
					.putIfNotNull("minPoolSize", minPoolSize)//
					.putIfNotNull("maxPoolSize", maxPoolSize)//
					.putIfNotNull("minPeriodBetweenExecs",
							minPeriodBetweenExecs)//
					.putIfNotNull("resizeUpStabilizationDelay",
							resizeUpStabilizationDelay)//
					.putIfNotNull("resizeDownStabilizationDelay",
							resizeDownStabilizationDelay)//
					.putIfNotNull("timeUnit", timeUnit)//
					//
					.build();
			return new ScenarioHandler(configuration);
		}

		public Builder entityWithMetric(Entity val) {
			this.entityWithMetric = val;
			return this;
		}

		public Builder metricLowerBound(Number val) {
			this.metricLowerBound = val;
			return this;
		}

		public Builder metricUpperBound(Number val) {
			this.metricUpperBound = val;
			return this;
		}

		public Builder metricRange(Number min, Number max) {
			metricLowerBound = checkNotNull(min);
			metricUpperBound = checkNotNull(max);
			return this;
		}

		public Builder minPoolSize(int val) {
			this.minPoolSize = val;
			return this;
		}

		public Builder maxPoolSize(int val) {
			this.maxPoolSize = val;
			return this;
		}

		public Builder sizeRange(int min, int max) {
			minPoolSize = min;
			maxPoolSize = max;
			return this;
		}

		public Builder minPeriodBetweenExecs(int val) {
			this.minPeriodBetweenExecs = val;
			return this;
		}

		public Builder withTimeUnit(TimeUnit timeUnit) {
			this.timeUnit = timeUnit;
			return this;
		}

		public Builder resizeUpStabilizationDelay(int val) {
			this.resizeUpStabilizationDelay = val;
			return this;
		}

		public Builder resizeDownStabilizationDelay(int val) {
			this.resizeDownStabilizationDelay = val;
			return this;
		}

		public Builder metric(AttributeSensor<? extends Number> val) {
			this.metric = val;
			return this;
		}

		public Builder poolHotSensor(BasicNotificationSensor<?> val) {
			this.poolHotSensor = val;
			return this;
		}

		public Builder poolColdSensor(BasicNotificationSensor<?> val) {
			this.poolColdSensor = val;
			return this;
		}

		public Builder poolOkSensor(BasicNotificationSensor<?> val) {
			this.poolOkSensor = val;
			return this;
		}

		public Builder startAt(int firstActionAtTime) {
			this.firstActionAtTime = firstActionAtTime;
			return this;
		}
	}

}
