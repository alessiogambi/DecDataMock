package at.ac.testing.brooklyn.utils;

import at.ac.testing.mocks.ActionType;
import at.ac.testing.mocks.SensorType;
import at.ac.testing.mocks.TimedAction;

public class ScenarioAtom {

	boolean manualTo;
	boolean manualFrom;
	TimedAction action;
	boolean overMax;
	boolean underMin;
	
	public ScenarioAtom withSensorMetric(){
		this.action.sensorType = SensorType.METRIC;
		return this;
	}
	
	public ScenarioAtom withSensorPool(){
		this.action.sensorType = SensorType.POOL_SENSOR;
		return this;
	}

	// Provide only the backbone structure and constraints
	public static ScenarioAtom resize(int from, int to) {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.manualTo = true;
		ScenarioAtom.manualFrom = true;
		ScenarioAtom.action = new TimedAction(-1, from, to);
		return ScenarioAtom;
	}

	public static ScenarioAtom stay() {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.action = new TimedAction(-1, ActionType.STAY);
		return ScenarioAtom;
	}

	public static ScenarioAtom stay(int at) {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.manualTo = true;
		ScenarioAtom.manualFrom = true;
		ScenarioAtom.action = new TimedAction(-1, at, ActionType.STAY);
		return ScenarioAtom;
	}

	public static ScenarioAtom scaleUp() {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.action = new TimedAction(-1, ActionType.SCALE_UP);
		return ScenarioAtom;
	}

	public static ScenarioAtom scaleUp(int to) {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.manualTo = true;
		ScenarioAtom.action = new TimedAction(-1, ActionType.SCALE_UP, to);
		return ScenarioAtom;
	}

	public static ScenarioAtom scaleDown() {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.action = new TimedAction(-1, ActionType.SCALE_DOWN);
		return ScenarioAtom;
	}

	public static ScenarioAtom scaleDown(int to) {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.manualTo = true;
		ScenarioAtom.action = new TimedAction(-1, ActionType.SCALE_DOWN, to);
		return ScenarioAtom;
	}

	public static ScenarioAtom scaleOverMax() {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.overMax = true;
		ScenarioAtom.action = new TimedAction(-1, ActionType.SCALE_UP);
		return ScenarioAtom;
	}

	public static ScenarioAtom scaleUnderMin() {
		// Here specify all the properties of the scenario to build !
		ScenarioAtom ScenarioAtom = new ScenarioAtom();
		ScenarioAtom.underMin = true;
		ScenarioAtom.action = new TimedAction(-1, ActionType.SCALE_DOWN);
		return ScenarioAtom;
	}

}
