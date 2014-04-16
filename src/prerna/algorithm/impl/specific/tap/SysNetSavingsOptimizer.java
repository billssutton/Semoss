/*******************************************************************************
 * Copyright 2013 SEMOSS.ORG
 * 
 * This file is part of SEMOSS.
 * 
 * SEMOSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SEMOSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SEMOSS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package prerna.algorithm.impl.specific.tap;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.MultiStartUnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well1024a;
import org.apache.log4j.Logger;

import prerna.ui.components.specific.tap.SysOptGraphFunctions;
import prerna.ui.components.specific.tap.SysOptPlaySheet;
import prerna.util.Utility;

/**
 * This optimizer is used for implementation of the ROI (return on investment) function.
 */
public class SysNetSavingsOptimizer extends UnivariateSvcOptimizer{
	
	ResidualSystemOptFillData resFunc;
	String sysQuery, dataQuery, bluQuery;
	ArrayList<String> sysList, dataList, bluList;
	double dataExposeCost;
	double numMaintenanceSavings;
	public double budget=0.0, optNumYears = 0.0, netSavings = 0.0, roi=0.0;
	Logger logger = Logger.getLogger(getClass());
	boolean noErrors=true;
	public ArrayList<Double> cumSavingsList, breakEvenList;
	
	
	public void setQueries(String sysQuery, String dataQuery, String bluQuery)
	{
		this.sysQuery = sysQuery;
		this.dataQuery = dataQuery;
		this.bluQuery = bluQuery;
	}
	public void addCapBindings(ArrayList capBindings)
	{
		if(capBindings.size()==0)
			return;
		dataQuery += "BINDINGS ?Capability {";
		bluQuery += "BINDINGS ?Capability {";
		for(int i=0;i<capBindings.size();i++)
		{
			dataQuery+="(<http://health.mil/ontologies/Concept/Capability/"+(String)capBindings.get(i)+">)";
			bluQuery+="(<http://health.mil/ontologies/Concept/Capability/"+(String)capBindings.get(i)+">)";
		}
		dataQuery+="}";
		bluQuery+="}";
	}
	public void getData()
	{
		String engine = playSheet.engine.getEngineName();
		resFunc = new ResidualSystemOptFillData();
		resFunc.setMaxYears(maxYears);
		sysList = resFunc.runListQuery(engine,sysQuery);
		dataList = resFunc.runListQuery(engine,dataQuery);
		bluList = resFunc.runListQuery(engine,bluQuery);
		if(sysList.size()==0)
		{
			noErrors = false;
			return;

		}
		resFunc.setPlaySheet((SysOptPlaySheet)playSheet);
		resFunc.setSysList(sysList);
		resFunc.setDataList(dataList);
		resFunc.setBLUList(bluList);
		resFunc.fillDataStores();
	}
	
	public void getModernizedSysList()
	{
		playSheet.progressBar.setString("Determining Modernized List");
		ResidualSystemOptimizer sysOpt = new ResidualSystemOptimizer();
		sysOpt.setPlaySheet((SysOptPlaySheet)playSheet);
		sysOpt.setDataSet(sysList,dataList,bluList,resFunc.systemDataMatrix,resFunc.systemBLUMatrix,resFunc.systemCostOfDataMatrix,resFunc.systemCostOfMaintenance,resFunc.systemCostOfDB,resFunc.systemNumOfSites,resFunc.dataSORSystemExists,resFunc.bluProviderExists);
		noErrors = sysOpt.runOpt();

		this.dataExposeCost = sysOpt.numTransformationTotal; //total cost to expose all data for all systems at all sites
		this.numMaintenanceSavings =sysOpt.numMaintenanceTotal;
	}
	
	public void runOpt()
	{
		getData();
		if(noErrors == false)
		{
			playSheet.progressBar.setVisible(false);
			Utility.showError("No systems exist for this selection. Please revise your query or choose different system types.");
			return;
		}
		getModernizedSysList();
		if(noErrors == false)
		{
			playSheet.progressBar.setVisible(false);
			Utility.showError("All systems must be kept to maintain same functionality.");
			return;
		}
		
        progressBar = playSheet.progressBar;
        f.setConsoleArea(playSheet.consoleArea);
        f.setProgressBar(progressBar);
        f.setVariables(maxYears, hourlyCost, interfaceCost, serMainPerc, attRate, hireRate,infRate, disRate, scdLT, iniLC, scdLC);
        ((SysNetSavingsFunction)f).setSavingsVariables(numMaintenanceSavings, serMainPerc, dataExposeCost);
        ((SysNetSavingsFunction)f).createLinearInterpolation(iniLC,scdLC, scdLT, dataExposeCost, 0, maxYears);

        //budget in LOE
        UnivariateOptimizer optimizer = new BrentOptimizer(.001, .001);
        RandomGenerator rand = new Well1024a(500);
        MultiStartUnivariateOptimizer multiOpt = new MultiStartUnivariateOptimizer(optimizer, noOfPts, rand);
        UnivariateObjectiveFunction objF = new UnivariateObjectiveFunction(f);
        SearchInterval search = new SearchInterval(minBudget,maxBudget); //budget in LOE
        optimizer.getStartValue();
        MaxEval eval = new MaxEval(200);
        
        OptimizationData[] data = new OptimizationData[]{search, objF, GoalType.MAXIMIZE, eval};
        try {
            UnivariatePointValuePair pair = multiOpt.optimize(data);
            budget = pair.getPoint();
            optNumYears = ((SysNetSavingsFunction)f).calculateYear(budget);

            calculateSavingsAndROI();

           // netSavings = pair.getValue();
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
        } catch (TooManyEvaluationsException fee) {
        	noErrors = false;
        	playSheet.consoleArea.setText(playSheet.consoleArea.getText()+"\nError: "+fee);
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
            clearPlaysheet();
        }
        runOptIteration();
		
		
	}
	public void calculateSavingsAndROI()
	{
        SysNetSavingsFunction savingsF = new SysNetSavingsFunction();
        savingsF.setVariables(maxYears, hourlyCost, interfaceCost, serMainPerc, attRate, hireRate,infRate, disRate, scdLT, iniLC, scdLC);
        savingsF.setSavingsVariables(numMaintenanceSavings, serMainPerc, dataExposeCost);
        savingsF.createLinearInterpolation(iniLC,scdLC, scdLT, dataExposeCost, 0, maxYears);
        netSavings = savingsF.calculateRet(budget,optNumYears);
        
        SysROIFunction roiF = new SysROIFunction();
        roiF.setVariables(maxYears, hourlyCost, interfaceCost, serMainPerc, attRate, hireRate,infRate, disRate, scdLT, iniLC, scdLC);
        roiF.setSavingsVariables(numMaintenanceSavings, serMainPerc, dataExposeCost);
        roiF.createLinearInterpolation(iniLC,scdLC, scdLT, dataExposeCost, 0, maxYears);
        roi = roiF.calculateRet(budget,optNumYears);
	}
	
	/**
	 * Runs the appropriate optimization iteration.
	 */
	@Override
	public void optimize()
	{
        f = new SysNetSavingsFunction();
        runOpt();
        if(noErrors)
        {
			playSheet.consoleArea.setText(playSheet.consoleArea.getText()+"\nBudget: "+budget);
			playSheet.consoleArea.setText(playSheet.consoleArea.getText()+"\nNumber of Years to consolidate systems: "+optNumYears);
			playSheet.consoleArea.setText(playSheet.consoleArea.getText()+"\nGiven timespan to accumulate savings over: "+maxYears);
			playSheet.consoleArea.setText(playSheet.consoleArea.getText()+"\nMaximized net cumulative savings: "+netSavings);
        }
	}   
	

	/**
	 * Runs a specific iteration of the optimization.
	 */
	@Override
	public void runOptIteration()
	{
        displayResults(f.lin);
	}
	

	/**
	 * Displays the results from various optimization calculations. 
	 * These include profit, ROI, Recoup, and breakeven functions.
	 * @param lin 	Optimizer used for TAP-specific calculations. 
	 */
	@Override
	public void displayResults(ServiceOptimizer lin)
	{
		f.createLearningYearlyConstants((int)Math.ceil(optNumYears), scdLT, iniLC, scdLC);
		cumSavingsList = ((SysNetSavingsFunction)f).createCumulativeSavings(budget, optNumYears);
		breakEvenList = ((SysNetSavingsFunction)f).createBreakEven(budget, optNumYears);

		String netSavingsString = Utility.sciToDollar(netSavings);
		playSheet.savingLbl.setText(netSavingsString);
		String annualBudgetString = Utility.sciToDollar(budget);
		((SysOptPlaySheet)playSheet).annualBudgetLbl.setText(annualBudgetString); 
		double timeTransition = Utility.round(optNumYears,2);
		((SysOptPlaySheet)playSheet).timeTransitionLbl.setText(Double.toString(timeTransition)+" Years");
		double roiVal = Utility.round(roi, 5);
		playSheet.roiLbl.setText(Double.toString(roiVal)); 
		
		double breakEvenYear = 0.0;
		for(int i=0;i<breakEvenList.size();i++)
		{
			if(breakEvenList.get(i)<0)
				breakEvenYear = i+1;
		}
		if(breakEvenList.get(breakEvenList.size()-1)<0)
			playSheet.bkevenLbl.setText("Beyond Max Time");
		else
			playSheet.bkevenLbl.setText(Double.toString(breakEvenYear)+" Years");
		
		SysOptGraphFunctions graphF= new SysOptGraphFunctions();
		graphF.setOptimzer(this);
		Hashtable chartHash4 = graphF.createCumulativeSavings();
		Hashtable chartHash5 = graphF.createBreakevenGraph();
		Hashtable chartHash6 = graphF.createLearningCurve();

		playSheet.tab4.callIt(chartHash4);
		playSheet.tab5.callIt(chartHash5);
		playSheet.tab6.callIt(chartHash6);
		playSheet.tab4.setVisible(true);
		playSheet.tab5.setVisible(true);
		playSheet.tab6.setVisible(true);
	}
	
	
}
