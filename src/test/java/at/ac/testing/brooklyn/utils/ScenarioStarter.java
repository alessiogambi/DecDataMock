package at.ac.testing.brooklyn.utils;

public interface ScenarioStarter {
	AtScenarioStarter at(int time);

	FromScenarioStarter from(int size);
}
