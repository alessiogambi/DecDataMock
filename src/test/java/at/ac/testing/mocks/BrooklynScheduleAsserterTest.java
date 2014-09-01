package at.ac.testing.mocks;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import polyglot.ext.pbnj.tologic.LogMap;

public class BrooklynScheduleAsserterTest {
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
	public void hasConcurrency() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(0, ActionType.SCALE_UP),
				new TimedAction(1, ActionType.SCALE_DOWN) };
		schedule.actions = targetStates;

		Assert.assertTrue("", asserter.hasConcurrency());
	}

	@Test
	public void hasMonitoringBlips() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(0, ActionType.SCALE_UP),
				new TimedAction(4, ActionType.STAY) };
		schedule.actions = targetStates;

		Assert.assertTrue("", asserter.hasMonitoringBlips());
	}

	@Test
	public void hasSustainedActions() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(0, ActionType.SCALE_UP),
				new TimedAction(4, ActionType.SCALE_UP) };
		schedule.actions = targetStates;

		Assert.assertTrue("", asserter.hasSustainedActions());
	}

	@Test
	public void hasNoSustainedActions() {
		TimedAction[] targetStates = new TimedAction[] {
				new TimedAction(0, ActionType.SCALE_UP),
				new TimedAction(4, ActionType.SCALE_DOWN) };
		schedule.actions = targetStates;

		Assert.assertFalse("", asserter.hasSustainedActions());
	}
}
