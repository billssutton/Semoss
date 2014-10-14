package prerna.algorithm.impl.specific.tap;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class ResidualSystemTheatGarrOptimizer extends ResidualSystemOptimizer{
	
	int[] systemTheater;
	int[] systemGarrison;
	
	//Ap, Bq
	int[][] dataRegionSORSystemTheaterExists;
	int[][] dataRegionSORSystemGarrisonExists;
	int[][] bluRegionProviderTheaterExists;
	int[][] bluRegionProviderGarrisonExists;

	/**
	 * Gathers data set.
	 */
	public void setTheatGarrDataSet(int[] systemTheater,int[] systemGarrison,int[][] dataRegionSORSystemTheaterExists, int[][] dataRegionSORSystemGarrisonExists, int[][] bluRegionProviderTheaterExists, int[][] bluRegionProviderGarrisonExists) {
		this.systemTheater = systemTheater;
		this.systemGarrison = systemGarrison;
		this.dataRegionSORSystemTheaterExists=dataRegionSORSystemTheaterExists;
		this.dataRegionSORSystemGarrisonExists=dataRegionSORSystemGarrisonExists;
		this.bluRegionProviderTheaterExists=bluRegionProviderTheaterExists;
		this.bluRegionProviderGarrisonExists=bluRegionProviderGarrisonExists;
	}
	
	/**
	 * Sets constraints in the model.
	 */
	@Override
	public void setConstraints() {
		//makes building the model faster if it is done rows by row
		solver.setAddRowmode(true);	
		//adding constraints for data objects
		addRequiredSystemsConstraint(systemModernize);
		if(systemTheater!=null) {
			addConstraints(systemDataMatrix,systemRegionMatrix,systemTheater,dataRegionSORSystemTheaterExists);
			addConstraints(systemBLUMatrix,systemRegionMatrix,systemTheater,bluRegionProviderTheaterExists);
		}
		if(systemGarrison!=null) {
			addConstraints(systemDataMatrix,systemRegionMatrix,systemGarrison,dataRegionSORSystemGarrisonExists);
			addConstraints(systemBLUMatrix,systemRegionMatrix,systemGarrison,bluRegionProviderGarrisonExists);
		}
		//rowmode turned off
		solver.setAddRowmode(false);
	}
	
	private void addConstraints(int[][] systemProviderMatrix, int[][] systemRegionMatrix, int[] systemGT, int[][] constraintMatrix)
	{
		try{
			for(int dataInd=0;dataInd<systemProviderMatrix[0].length;dataInd++)
			{
				for(int regionInd=0;regionInd<systemRegionMatrix[0].length;regionInd++)
				{
					int[] colno = new int[systemProviderMatrix.length];
			        double[] row = new double[systemProviderMatrix.length];
		
			        for(int sysInd=0;sysInd<systemProviderMatrix.length;sysInd++)
			        {
			        	colno[sysInd] = sysInd+1;
			        	row[sysInd] = systemProviderMatrix[sysInd][dataInd]*systemRegionMatrix[sysInd][regionInd]*systemGT[sysInd];
			        }
			        if(constraintMatrix[dataInd][regionInd]>0)
			        	solver.addConstraintex(systemProviderMatrix.length, row, colno, LpSolve.GE, 1);
			        else
			        	solver.addConstraintex(systemProviderMatrix.length, row, colno, LpSolve.GE, 0);
				}
			}
		}catch (LpSolveException e){
			e.printStackTrace();
		}
	}
	
}
