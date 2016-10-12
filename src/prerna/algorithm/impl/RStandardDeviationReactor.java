package prerna.algorithm.impl;

public class RStandardDeviationReactor extends RBasicMathReactor {

	/*
	 * Only need to set the Math Routine
	 * Everything else is handled by inheritance
	 */
	
	public RStandardDeviationReactor() {
		this.setMathRoutine("sd");
	}

}