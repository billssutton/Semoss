/*******************************************************************************
 * Copyright 2015 SEMOSS.ORG
 *
 * If your use of this software does not include any GPLv2 components:
 * 	Licensed under the Apache License, Version 2.0 (the "License");
 * 	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 *
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 * ----------------------------------------------------------------------------
 * If your use of this software includes any GPLv2 components:
 * 	This program is free software; you can redistribute it and/or
 * 	modify it under the terms of the GNU General Public License
 * 	as published by the Free Software Foundation; either version 2
 * 	of the License, or (at your option) any later version.
 *
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *******************************************************************************/
package prerna.ui.components.specific.tap;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.engine.api.IEngine;
import prerna.engine.api.ISelectStatement;
import prerna.engine.api.ISelectWrapper;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.ui.components.GridFilterData;
import prerna.ui.components.GridTableModel;
import prerna.ui.components.NewScrollBarUI;
import prerna.ui.components.ParamComboBox;
import prerna.util.Constants;
import prerna.util.DIHelper;

/**
 * This class is a financial organizer.
 //TODO: Remove class? Not called in SEMOSS
 */
public class PfTapFinancialOrganizer {
	
	static final Logger logger = LogManager.getLogger(PfTapFinancialOrganizer.class.getName());

	ISelectWrapper wrapper;
	Hashtable<Integer, Integer> yearIdxTable = new Hashtable<Integer, Integer>();
	int totalColumns=12;
	int stringColumns=4;
	int doubleColumns=8;
	Double maintenanceRate = 0.18;
	Double hourlyRate = 200.0;
	ArrayList<Object[]> outputList = new ArrayList<Object[]>();
	
	/**
	 * Constructor for PfTapFinancialOrganizer.
	 */
	public PfTapFinancialOrganizer()
	{
		
	}
	
	/**
	 * Method preparePhaseTasks.
	 * @param tapPhaseLOEHash Hashtable
	 * @param tapPhaseDateHash Hashtable
	 * @param system String
	 */
	public void preparePhaseTasks(Hashtable tapPhaseLOEHash, Hashtable tapPhaseDateHash, String system)
	{
		String query = "SELECT (SAMPLE(?PfType) As ?Type) (SAMPLE(?prime) AS ?Prime) (SAMPLE(?PfPhase) AS ?Phase) (SAMPLE(?PfSubSubTaskType) AS ?SubSubTaskType) ?PfPhaseTask (SUM(COALESCE(?rate,0)) AS ?summedRate) WHERE {{?PfSubSubTaskType <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfSubSubTaskType> ;} {?PfType <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfType> ;} BIND(<http://semoss.org/ontologies/Relation/TypeOf> AS ?type) {?PfSubSubTaskType ?type ?PfType;}BIND(<http://semoss.org/ontologies/Relation/Has> AS ?has) {{?PfPhaseTask <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfPhaseTask> ;} {?PfSubSubTaskType ?has ?PfPhaseTask ;} {?PfOverheadItem <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfOverheadItem> ;} {?PfPhaseTask ?has ?PfOverheadItem ;}{?PfOverheadItem <http://semoss.org/ontologies/Relation/Contains/Rate> ?rate ;} {?has3 <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has> ;} {?PfPhase <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfPhase> ;} {?PfPhase ?has3 ?PfPhaseTask ;} {?has3 <http://semoss.org/ontologies/Relation/Contains/Prime> ?prime ;} } UNION { {?PfPhaseTask <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfPhaseTask> ;} {?PfSubSubTaskType ?has ?PfPhaseTask ;} {?has3 <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has> ;} {?PfPhase <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfPhase> ;} {?PfPhase ?has3 ?PfPhaseTask ;} {?has3 <http://semoss.org/ontologies/Relation/Contains/Prime> ?prime ;} FILTER(?prime = \"Prime\"). } } GROUP BY ?PfPhaseTask";
		ArrayList<Object[]> returnedList = retListFromQuery(query);
		
		int deployYear = (Integer) tapPhaseDateHash.get("Deploy");
		int omYear = deployYear +1;
		Hashtable amountHash = new Hashtable<String, Double>();
		Hashtable dateHash = new Hashtable<String, Integer>();
		Hashtable overheadRateHash = new Hashtable<String, Double>();
		String[] names = wrapper.getVariables();
		//save overhead rates for later
		for (int i = 0; i<returnedList.size(); i++){
			String phaseTask = (String) returnedList.get(i)[4];
			Double overheadRate = (Double) returnedList.get(i)[5];
			//Double overheadRate = Double.parseDouble(rateString);
			overheadRateHash.put(phaseTask, overheadRate);

		}
		for (int i = 0; i<returnedList.size(); i++){
			String type = returnedList.get(i)[0].toString();
			String primeString = returnedList.get(i)[1].toString();
			Object phase = returnedList.get(i)[2];
			String phaseTask = (String) returnedList.get(i)[4];
			Double overheadRate = (Double) returnedList.get(i)[5];
			boolean prime = true;
			if (primeString.equals("\"Not Prime\"")) prime = false;
			
			//Double overheadRate = Double.parseDouble(rateString);
			logger.info(phase);
			Double phaseRate = (Double) tapPhaseLOEHash.get(phase);
			int year = (Integer) tapPhaseDateHash.get(phase);
			
			double primeDouble =0;
			if(prime ==true) primeDouble = 1;
			
			double calculatedAmount = 0;
			
			if(type.equals("RDT&E")){
				calculatedAmount = (primeDouble + overheadRate)*phaseRate;
			}
			else if(type.equals("OM")){
				//need to get the overhead rate of the RDT&E side of the house
				String rdtePhaseTask= phaseTask.replace("%OM", "%RDT&E");
				Double rdteOverheadRate = (Double) overheadRateHash.get(rdtePhaseTask);
				year = omYear;
				if (!overheadRateHash.containsKey(rdtePhaseTask))
				{
					//this is only for the PMO item which does not have RDT&E, the rate change is divided by 3
					rdteOverheadRate=overheadRate/3;
					//this is only for the PMO item which starts as early as possible
					year = 2014;
				}
				//if it's prime, you need to take whole phase rate, if it's not prime, you're only taking the overhead rate of phase rate
				if (prime ==true)
				{
					calculatedAmount =  (phaseRate*primeDouble)*maintenanceRate*(1+overheadRate);
				}
				else
				{
					calculatedAmount =  (phaseRate*rdteOverheadRate)*maintenanceRate*(1+overheadRate);
				}
				
			}
			
			amountHash.put(phaseTask, calculatedAmount);
			dateHash.put(phaseTask, year);
		}
		logger.info(amountHash);
		logger.info(dateHash);
		ArrayList<Object[]> sysOutputList =processDataForGrid(amountHash, dateHash, system);
		outputList.addAll(sysOutputList);
		//createGrid(outputList);
	}
	
	/**
	 * Method processDataForGrid.
	 * @param amountHash Hashtable
	 * @param dateHash Hashtable
	 * @param system String
	
	 * @return ArrayList<Object[]> */
	public ArrayList<Object[]> processDataForGrid(Hashtable amountHash, Hashtable dateHash, String system)
	{
		ArrayList<Object[]> outputList = new ArrayList<Object[]>();
		Hashtable<String, String[]> outputStringHash = new Hashtable<String, String[]>();
		Hashtable<String, Double[]> outputDoubleHash = new Hashtable<String, Double[]>();
		yearIdxTable.put(2013, 0);
		yearIdxTable.put(2014, 1);
		yearIdxTable.put(2015, 2);
		yearIdxTable.put(2016, 3);
		yearIdxTable.put(2017, 4);
		yearIdxTable.put(2018, 5);
		yearIdxTable.put(2019, 6);
		String query = "SELECT DISTINCT ?PfCoreTask ?PfSubTask ?PfSubSubTask ?PfSubSubTaskType (COALESCE(?PfPhaseTask,\"\") AS ?pfPhaseTask)  WHERE {{?PfCoreTask <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfCoreTask> ;} {?includes <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Includes>;}{?PfSubTask <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfSubTask>;}{?includes2 <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Includes>;}{?PfSubSubTask <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfSubSubTask>;}{?consists <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consists>;}{?PfSubSubTaskType <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfSubSubTaskType>;}{?PfCoreTask ?includes ?PfSubTask}{?PfSubTask ?includes2 ?PfSubSubTask}{?PfSubSubTask ?consists ?PfSubSubTaskType}OPTIONAL{{?has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>;}{?PfPhaseTask <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/PfPhaseTask>;}{?PfSubSubTaskType ?has ?PfPhaseTask}}}";
		ArrayList<Object[]> returnedList = retListFromQuery(query);
		int pfPhaseTypeIdx = 4;
		int pfSubTaskTypeIdx = 3;
		int totalColumns = 12;
		for (int i = 0; i<returnedList.size(); i++)
		{
			String pfPhaseType = (String) returnedList.get(i)[pfPhaseTypeIdx];
			String pfSubTaskType = (String) returnedList.get(i)[pfSubTaskTypeIdx];
			boolean hasLOE = amountHash.containsKey(pfPhaseType);
			if (hasLOE)
			{
				Double loe = (Double)amountHash.get(pfPhaseType);
				int dateYear = (Integer)dateHash.get(pfPhaseType);
				int intYearIdx = yearIdxTable.get(dateYear);
				if (outputStringHash.containsKey(pfSubTaskType))
				{
					Double[] retDoubleList = outputDoubleHash.get(pfSubTaskType);
					if (retDoubleList[intYearIdx] == null)
					{
						retDoubleList[intYearIdx]=loe;
					}
					else
					{
						retDoubleList[intYearIdx]=retDoubleList[intYearIdx]+loe;
					}
					outputDoubleHash.put(pfSubTaskType, retDoubleList);
				}
				else
				{
					String[] addStringList = new String[stringColumns];
					Double[] addDoubleList = new Double[doubleColumns];
					
					addStringList[0]= (String) returnedList.get(i)[0];
					addStringList[1]= (String) returnedList.get(i)[1];
					addStringList[2]= (String) returnedList.get(i)[2];
					String[] splitString = pfSubTaskType.split("%");
					addStringList[3]= splitString[splitString.length-1];
					addDoubleList[intYearIdx]=loe;
					outputStringHash.put(pfSubTaskType, addStringList);
					outputDoubleHash.put(pfSubTaskType, addDoubleList);
				}
			}
			else
			{
				String[] addStringList = new String[stringColumns];
				Double[] addDoubleList = new Double[doubleColumns];
				
				addStringList[0]= (String) returnedList.get(i)[0];
				addStringList[1]= (String) returnedList.get(i)[1];
				addStringList[2]= (String) returnedList.get(i)[2];
				String[] splitString = pfSubTaskType.split("%");
				addStringList[3]= splitString[splitString.length-1];
				outputStringHash.put(pfSubTaskType, addStringList);
				outputDoubleHash.put(pfSubTaskType, addDoubleList);
			}
				
		}
		
		Iterator it = outputDoubleHash.keySet().iterator();
		
		while (it.hasNext())
		{
			Object[] totalArray = new String[totalColumns +1];
			totalArray[0] = system;
			String key = (String) it.next();
			Double[] doubleList = outputDoubleHash.get(key);
			String[] stringList = outputStringHash.get(key);
			if (key.contains("%OM"))
			{
				for (int i=0;i<doubleList.length-2;i++)
				{
					if (doubleList[i] !=null)
					{
						Double sustainLOE = doubleList[i];
						doubleList[i+1]= sustainLOE*1.018;
					}
				}
			}
			//final column for FY2015-2019
			Double totalLOE = 0.0;
			for (int i=doubleColumns-2; i>1;i--)
			{
				if (doubleList[i] !=null)
				{
					totalLOE = totalLOE +doubleList[i];
				}
			}
			doubleList[doubleList.length-1] = totalLOE;
			String[] doubleToStringArray = new String[doubleColumns];
			//change doubleArray to stringArray for display purposes, then add to overallArray
			//converting to dollar here
			for (int i=0;i<doubleList.length;i++)
			{
				if (doubleList[i] !=null)
				{
					Double totalDouble =doubleList[i]*hourlyRate;
					int finalInt = totalDouble.intValue();
					totalArray[i+stringColumns+1]=finalInt;
					//totalArray[i+stringColumns+1] = "$"+totalArray[i+stringColumns+1];
				}
			}
			//add stringArray to the overallArray, probably should have used ArrayList...
			for (int i=0;i<stringList.length;i++)
			{
				totalArray[i+1]=stringList[i];
			}
			
			outputList.add(totalArray);
		}
		
		return outputList;
	}
	
	
	/**
	 * Method createGrid.
	 */
	public void createGrid ()
	{
		String[] names = new String[totalColumns+1];
		//put in column names
		names[0]="System";
		names[4]="APPN";
		names[5]="FY13";
		names[6]="FY14";
		names[7]="FY15";
		names[8]="FY16";
		names[9]="FY17";
		names[10]="FY18";
		names[11]="FY19";
		names[12]="FYDP Total (FY15-FY19)";
		
		JComboBox reportType = (JComboBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_REPORT_TYPE_COMBO_BOX);
		String type = (String) reportType.getSelectedItem();
		String title = type;
		if(!type.contains("Generic")){
			ParamComboBox systemComboBox = (ParamComboBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_REPORT_COMBO_BOX);
			String system = (String) systemComboBox.getSelectedItem();
			title = system +"-"+title;
		}
		else title = "Generic-"+title;
		JCheckBox dataFedCheck = (JCheckBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_CHECK_BOX_DATA_FED);
		JCheckBox consumerCheck = (JCheckBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_CHECK_BOX_DATA_CONSUMER);
		JCheckBox bluProviderCheck = (JCheckBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_CHECK_BOX_BLU_PROVIDER);
		JCheckBox genericDataCheck = (JCheckBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_CHECK_BOX_DATA_GENERIC);
		JCheckBox genericBLUCheck = (JCheckBox) DIHelper.getInstance().getLocalProp(Constants.TRANSITION_CHECK_BOX_BLU_GENERIC);
		
		if(dataFedCheck.isSelected()&&dataFedCheck.isVisible()) title = title+"-Data Federation";
		if(bluProviderCheck.isSelected()&&bluProviderCheck.isVisible()) title = title+"-BLU Provider";
		if(consumerCheck.isSelected()&&consumerCheck.isVisible()) title = title+"-Consumer";
		if(genericDataCheck.isSelected()&&genericDataCheck.isVisible()) title = title+"-Data";
		if(genericBLUCheck.isSelected()&&genericBLUCheck.isVisible()) title = title+"-BLU";
		GridFilterData gfd = new GridFilterData();
		gfd.setColumnNames(names);
		gfd.setDataList(outputList);
		JTable table = new JTable();
		GridTableModel model = new GridTableModel(gfd);
		table.setModel(model);
		table.setAutoCreateRowSorter(true);
		JDesktopPane pane = (JDesktopPane)DIHelper.getInstance().getLocalProp(Constants.DESKTOP_PANE);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.getVerticalScrollBar().setUI(new NewScrollBarUI());
		scrollPane.setAutoscrolls(true);
		JInternalFrame financialTestSheet = new JInternalFrame();
		financialTestSheet.setContentPane(scrollPane);
		pane.add(financialTestSheet);
		financialTestSheet.setClosable(true);
		financialTestSheet.setMaximizable(true);
		financialTestSheet.setIconifiable(true);
		financialTestSheet.setTitle(title);
		financialTestSheet.setResizable(true);
		financialTestSheet.pack();
		financialTestSheet.setVisible(true);
		try {
			financialTestSheet.setSelected(false);
			financialTestSheet.setSelected(true);
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		table.getRowSorter().toggleSortOrder(3);
	}
	
	/**
	 * Method retListFromQuery.
	 * @param query String
	
	 * @return ArrayList<Object[]> */
	public ArrayList<Object[]> retListFromQuery (String query)
	{
		ArrayList <Object []> list = new ArrayList();
		JList repoList = (JList)DIHelper.getInstance().getLocalProp(Constants.REPO_LIST);
		// get the selected repository
		Object[] repo = (Object[])repoList.getSelectedValues();
		IEngine engine = (IEngine)DIHelper.getInstance().getLocalProp(repo[0]+"");
		
		wrapper = WrapperManager.getInstance().getSWrapper(engine, query);

		/*wrapper = new SesameJenaSelectWrapper();
		wrapper.setQuery(query);
		wrapper.setEngine(engine);
		wrapper.executeQuery();
		// get the bindings from it
		*/
		int count = 0;
		String[] names = wrapper.getVariables();
		// now get the bindings and generate the data
		try {
			while(wrapper.hasNext())
			{
				ISelectStatement sjss = wrapper.next();
				
				Object [] values = new Object[names.length];
				boolean filledData = true;
				for(int colIndex = 0;colIndex < names.length;colIndex++)
				{
					if(sjss.getVar(names[colIndex]) != null)
					{
						values[colIndex] = sjss.getVar(names[colIndex]);
					}
					else {
						filledData = false;
						break;
					}
				}
				if(filledData)
					list.add(count, values);
				count++;
			}
		} 
		catch (RuntimeException e) {
			logger.debug(e);
		}
		
		return list;
	}
	
}
