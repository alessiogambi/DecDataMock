package at.ac.testing.mocks;

import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import polyglot.ext.pbnj.tologic.LogMap;

public class ScheduleTest {

	private static final Logger LOG = LoggerFactory
			.getLogger("at.ac.testing.mocks.Test");

	Schedule schedule;
	ScheduleAsserter asserter;

	@BeforeClass
	public static void setupSolver() {
		LogMap.SolverOpt_IntBitWidth = 8;
	}

	@BeforeMethod
	public void configureSchedule() {
		schedule = new Schedule();
		asserter = new ScheduleAsserter(schedule);
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
		Assert.assertTrue("Failed Input Size",
				schedule.inputActions.length == targetStates.length);
		for (TimedAction target : targetStates) {
			Assert.assertTrue("Failed Input Element " + target,
					asserter.containsAsInput(target));
			Assert.assertTrue("Failed Action Element " + target,
					asserter.containsAsActions(target));
		}
	}
}
