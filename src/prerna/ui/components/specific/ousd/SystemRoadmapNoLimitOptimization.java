package prerna.ui.components.specific.ousd;

import prerna.engine.api.IEngine;

public class SystemRoadmapNoLimitOptimization extends SystemRoadmapOptimizationGenerator{

	private boolean limit = false;
	
	@Override
	public void createTimeline(IEngine engine){
		roadmapEngine = engine;
		runOptimization(limit);
	}
}
