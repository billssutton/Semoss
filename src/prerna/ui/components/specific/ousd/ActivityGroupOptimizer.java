package prerna.ui.components.specific.ousd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.impl.LPOptimizer;

public class ActivityGroupOptimizer extends LPOptimizer{

	protected static final Logger LOGGER = LogManager.getLogger(BLUSystemOptimizer.class.getName());

	String[] sysList;
	boolean limit;
	int year;
	int totalYears;
	int totalSystemCount;
	Map<String, Double> sysBudgets;
	Map<String, Integer> upstreamInterfaceCount = new HashMap<String, Integer>();
	Map<String, Integer> downstreamInterfaceCount = new HashMap<String, Integer>();
	Map<String, List<String>> retirementMap; //retirement type -> systems that have that retirementType
	Map<String, Double> riskScoreMap;
	int sysListSize;
	double interfaceSustainmentPercent;
	double[] results;
	double interfaceCost;
	double replacementPercentage;
	double treeMaxConstant;
	double budgetConstraint;
	List<String> keptSystems;
	List<String> enduringSystems = new ArrayList<String>();


	/**
	 * 
	 */
	public ActivityGroupOptimizer(){
		super();
	}

	/**
	 * Makes the new LpSolver and sets variables in model.
	 * Variables are: one for each system at every site,
	 * one for each system to say deployed at any site (kept or not),
	 * one for each centrally deployed system to say deployed at all sites (kept or not)
	 * Also declares all variables to be binary and gives a starting point for optimization.
	 */
	@Override
	public void setVariables() throws LpSolveException {

		//make the lp solver with enough variables
		solver = LpSolve.makeLp(0, sysListSize);

		System.out.println("TANGO::setting "+sysListSize+" variables");
		//one variable for each system. simply is the system needed or decommissioned
		for(int i=0; i<sysListSize; i++) {
			solver.setBinary(i + 1, true);
			solver.setColName(i+1, sysList[i]);
		}
	}

	/**
	 * Sets the objection function to minimize sustainment cost.
	 * Sums the sustainment cost for the local systems kept at sites + their central maintenance costs
	 * + the sustainment cost for the centrally hosted systems.
	 */
	@Override
	public void setObjFunction() throws LpSolveException{
		int[] colno = new int[sysListSize];
		double[] row = new double[sysListSize];
		int i =0;

		/**
		 * each row calculated below is one entry in the sum of the objective function
		 */
		for(String system: sysList){
			colno[i] = i + 1;
			row[i] = treeMaxConstant * (double) (1-riskScoreMap.get(system)) * (double) sysBudgets.get(system);
			i++;
		}

		solver.setObjFnex(sysListSize, row, colno);
		solver.setMaxim();
	}

	@Override
	public void setConstraints() throws LpSolveException{

		long startTime;
		long endTime;

		//adding constraints for data and blu at each site
		startTime = System.currentTimeMillis();
		addFunctionalityConstraints();
		endTime = System.currentTimeMillis();
		System.out.println("Time to run add functionality constraint " + (endTime - startTime) / 1000 );

	}

	private void addFunctionalityConstraints() {

		int[] colno = new int[sysListSize];
		double[] row = new double[sysListSize];

		for(int j=0; j<sysListSize; j++) {				

			colno[j] = j + 1;
			
			if(sysList[j].equals("BAM")){
				System.out.println("BAM");
			}
			row[j] = interfaceCost*(downstreamInterfaceCount.get(sysList[j]) - (interfaceSustainmentPercent * upstreamInterfaceCount.get(sysList[j])));
		}
		try {
			solver.addConstraintex(sysListSize, row, colno, LpSolve.LE, budgetConstraint);
		} catch (LpSolveException e) {
			e.printStackTrace();
		}
		
//		for(String granularBlu: granularBluMap.keySet()){
//			List<String> supportingSysList = granularBluMap.get(granularBlu);
//
//			int[] colno = new int[sysListSize];
//			double[] row = new double[sysListSize];
//			for(int j=0; j<sysListSize; j++) {
//
//				colno[j] = j + 1;
//				if(supportingSysList.contains(sysList[j])){
//					row[j] = 1;
//				}
//				else {
//					row[j] = 0;
//				}
//			}
//
//			try {
//				if(constraintCalculator(granularBlu, granularBluMap, false) >0){
//					solver.addConstraintex(sysListSize, row, colno, LpSolve.GE, constraintCalculator(granularBlu, granularBluMap, true));
//				}
//			} catch (LpSolveException e) {
//				e.printStackTrace();
//			}
//		}
//
//		for(String dataObj: dataMap.keySet()){
//			List<String> supportingSysList = dataMap.get(dataObj);
//
//			int[] colno = new int[sysListSize];
//			double[] row = new double[sysListSize];
//			for(int j=0; j<sysListSize; j++) {
//
//				colno[j] = j + 1;
//				if(supportingSysList.contains(sysList[j])){
//					row[j] = 1;
//				}
//				else {
//					row[j] = 0;
//				}
//			}
//
//			try {
//				solver.addConstraintex(sysListSize, row, colno, LpSolve.GE, constraintCalculator(dataObj, dataMap, true));
//			} catch (LpSolveException e) {
//				e.printStackTrace();
//			}
//		}

		System.out.println("ENDURING TOTAL:::"+enduringSystems.size());
		for(int j=0; j<sysListSize; j++){
			colno = new int[sysListSize];
			row = new double[sysListSize];

			if(enduringSystems.contains(sysList[j])){
				System.out.println("ENDURING::::::"+sysList[j]);
				colno[j] = j+1;
				row[j] = 1;
				try {
					solver.addConstraintex(sysListSize, row, colno, LpSolve.EQ, 0);
				} catch (LpSolveException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private int constraintCalculator(String constraintVariable, Map<String, List<String>> constraintVariables, boolean shouldLog){

		List<String> systems = constraintVariables.get(constraintVariable);
		int variableMax = systems.size();

		if(shouldLog){
			//			System.out.println("Constraint for "+constraintVariable+" has max value of "+variableMax);
		}

		int constraintValue = Math.max(variableMax, 1); 

		if(shouldLog){
			//			System.out.println("Determined value is of "+constraintVariable+" is "+constraintValue);
			//			System.out.println();
		}

		return constraintValue;
	}
	
	/**
	 * Executes the optimization.
	 */
	@Override
	public void execute(){

		super.execute();

		int index = 0;
		int nConstraints = solver.getNorigRows();

		this.results = new double[sysListSize];
		this.keptSystems = new ArrayList<String>();

		if(solved == LpSolve.OPTIMAL) {
			try {
				double objectiveVal = solver.getObjective();
				System.out.println("objective val::::::::::::::::::: " + objectiveVal);
				for(int i = 0; i < sysListSize; i++ ) {

					this.results[i] = solver.getVarPrimalresult(nConstraints + index + 1);
					System.out.println("System "+sysList[i]+" has result "+this.results[i]);
					if(this.results[i] == 0.0){
						this.keptSystems.add(this.sysList[i]);
					}
					index++;
				}
			} catch(LpSolveException e) {
				LOGGER.error("Unable to get solution. Take no action.");
			}

		} else {
			LOGGER.error("Solution is not optimal. Take no action.");
		}

	}

	/**
	 * @param sysNames
	 * @param budgets
	 * @param bluMap
	 * @param interfaceMap
	 * @param dataMap
	 * @param granularBLU
	 * @param retirementTypes
	 * @param year
	 * @param totalYears
	 * @param replacementPercent
	 */
	public void setSystemData(String[] sysNames, Map<String, Double> budgets, Map<String, List<String>> retirementMap, Map<String, Double> riskScoreMap, Map<String, Integer> upstreamInterfaceCount, Map<String, Integer> downstreamInterfaceCount){

		for(String system: sysNames){
			boolean contained =  false;
			for(String key: retirementMap.keySet()){
				if(retirementMap.get(key).contains(system)){
					contained = true;
				}
			}
			if(!contained){
				enduringSystems.add(system);
			}
		}

		this.sysListSize = sysNames.length;
		this.sysList = sysNames;
		this.sysBudgets = budgets;
		this.riskScoreMap = riskScoreMap;
		this.upstreamInterfaceCount = upstreamInterfaceCount;
		this.downstreamInterfaceCount = downstreamInterfaceCount;
	}

	/**
	 * @param year
	 * @param totalYears
	 * @param replacementPercent
	 */
	public void setOptimizationConstants(int totalSystemCount, boolean limit, double treeMax, double budgetConstraint, double interfaceSustainmentPercent, double interfaceCost){ 
		this.interfaceSustainmentPercent = interfaceSustainmentPercent;
		this.interfaceCost = interfaceCost;
		this.treeMaxConstant = treeMax;
		this.budgetConstraint = budgetConstraint;
		this.totalSystemCount = totalSystemCount;
		this.limit = limit;
	}

	public double[] getResults(){
		return this.results;
	}

	public List<String> getKeptSystems(){
		return this.keptSystems;
	}

}
