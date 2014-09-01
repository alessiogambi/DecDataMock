package at.ac.testing.mocks;

import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;

public class BrooklynScheduleTest {

	private static final Logger LOG = LoggerFactory
			.getLogger("at.ac.testing.mocks.Test");

	BrooklynSchedule schedule;
	BrooklynScheduleAsserter asserter;

	private int stabilityPeriodUp = 5;

	private int stabilityPeriodDown = 5;

	private int controlPeriod = 3;

	@BeforeClass
	public static void setupSolver() {
		LogMap.SolverOpt_IntBitWidth = 10;
	}

	@BeforeMethod
	public void configureSchedule() {
		schedule = new BrooklynSchedule(stabilityPeriodUp, stabilityPeriodDown,
				controlPeriod);
		asserter = new BrooklynScheduleAsserter(schedule);
	}

	@Test
	public void testScheduleInitialization() {
		schedule.init();
		LOG.info(schedule.toString());
		Assert.assertTrue("Failed Size", schedule.size > 0);
		Assert.assertTrue("Failed Duration", schedule.duration > 0);

	}

	@Test
	public void testScheduleInitializationWithStates() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(-1, ActionType.SCALE_UP),
				new TimedAction(-1, ActionType.SCALE_DOWN) };
		schedule.init(targetStates);
		LOG.info(schedule.toString());
		Assert.assertTrue("Failed Size", schedule.size >= targetStates.length);
		Assert.assertTrue("Failed Duration", schedule.duration > 0);
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}

	}

	@Test
	public void testScheduleNoConcurrentInitialization() {

		schedule.initNoConcurrent();
		LOG.info(schedule.toString());
		Assert.assertFalse("Has Concurrency ", asserter.hasConcurrency());
	}

	@Test
	public void testScheduleNoConcurrentInitializationWithStates() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(-1, ActionType.SCALE_UP),
				new TimedAction(-1, ActionType.SCALE_DOWN) };
		schedule.initNoConcurrent(targetStates);
		LOG.info(schedule.toString());
		Assert.assertFalse("Has Concurrency ", asserter.hasConcurrency());
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}

	}

	@Test
	public void testScheduleConcurrentInitialization() {

		schedule.initConcurrent();
		LOG.info(schedule.toString());
		Assert.assertTrue("No Concurrency ", asserter.hasConcurrency());
	}

	@Test
	public void testScheduleConcurrentInitializationWithStates() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(-1, 4, 3), new TimedAction(-1, 4, 2),
				new TimedAction(-1, 4, 1) };
		schedule.size = 3;
		schedule.firstActionAt = 0;
		schedule.minStateSize = 0;
		schedule.maxStateSize = 25;
		schedule.controlPeriod = 100;
		schedule.initConcurrent(targetStates);
		LOG.info(schedule.toString());
		Assert.assertTrue("No Concurrency ", asserter.hasConcurrency());
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}

	}

	@Test
	public void testScheduleMonitoringBlipInitialization() {

		schedule.initMonitoringBlip();
		LOG.info(schedule.toString());
		Assert.assertTrue("No Monitoring Blip ", asserter.hasMonitoringBlips());
	}

	@Test
	public void testScheduleMonitoringBlipInitializationWithStates() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(-1, ActionType.SCALE_UP),
				new TimedAction(-1, ActionType.STAY) };
		schedule.initMonitoringBlip(targetStates);
		LOG.info(schedule.toString());
		Assert.assertTrue("No Monitoring Blip ", asserter.hasMonitoringBlips());
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}
	}

	@Test
	public void testScheduleSustainedActionsInitialization() {

		schedule.initSustainedActions();
		LOG.info(schedule.toString());
		Assert.assertTrue("No Sustained Actions",
				asserter.hasSustainedActions());
	}

	@Test
	public void testScheduleSustainedActionsInitializationWithStates() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(-1, 1, 4), new TimedAction(-1, 1, 2),
				new TimedAction(-1, 1, 4) };
		schedule.minStateSize = 0;
		schedule.maxStateSize = 25;
		schedule.controlPeriod = 0;
		schedule.stabilityPeriodUp = 4;
		schedule.stabilityPeriodDown = 0;
		
		schedule.size = targetStates.length;
		
		schedule.initSustainedActions(targetStates);
		LOG.info(schedule.toString());
		Assert.assertTrue("No Sustained Actions",
				asserter.hasSustainedActions());
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}
	}
}
