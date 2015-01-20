/*******************************************************************************
 * Copyright 2014 SEMOSS.ORG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package prerna.ui.components.specific.tap;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.swing.event.EventListenerList;
import javax.swing.event.InternalFrameEvent;

import org.apache.log4j.Logger;

import prerna.ui.components.playsheets.BrowserPlaySheet;
import prerna.ui.main.listener.specific.tap.SimilarityBarChartBrowserFunction;
import prerna.ui.main.listener.specific.tap.SysSimHealthGridListener;
import prerna.ui.main.listener.specific.tap.SimilarityRefreshBrowserFunction;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

import com.google.gson.Gson;


/**
 */
public class SysSimHeatMapSheet extends SimilarityHeatMapSheet{
	boolean createSystemBindings = true;
	String systemListBindings = "BINDINGS ?System ";
	
	/**
	 * Constructor for SysSimeHeatMapSheet.
	 */
	public SysSimHeatMapSheet() {
		super();
		this.setPreferredSize(new Dimension(800,600));
		setComparisonObjectTypes("System1", "System2");
	}

	/**
	 * Adds the health grid, refresh, and bar chart listeners when the navigation has finished.
	 */
	@Override
	public void registerFunctions()
	{
		super.registerFunctions();
		SysSimHealthGridListener healthGridCall = new SysSimHealthGridListener();
	   	browser.registerFunction("healthGrid",  healthGridCall);    	
	}
	
	@Override
	public void createView() {
		if (!(this.query).equals("NULL") || this.query.isEmpty()) {			
			if (list!=null && list.isEmpty()) {
				return;
			}
			super.createView();
		}
		else if ((this.query).equals("NULL")) {
			super.createView();
		}
	}
	
	@Override
	public void createData()
	{
		if (!(this.query).equals("NULL") || this.query.isEmpty()) {
			super.createData();
			if (list!=null && list.isEmpty()) {
				Utility.showError("Query returned no results.");
				return;
			}
		}
		SimilarityFunctions sdf = new SimilarityFunctions();
		addPanel();
		// this would be create the data
		Hashtable dataHash = new Hashtable();
//		Hashtable overallHash;
		//get list of systems first
		updateProgressBar("10%...Getting all systems for evaluation", 10);
		String defaultSystemsQuery = "SELECT DISTINCT ?System WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>}{?System ?UsedBy ?SystemUser}}";
		defaultSystemsQuery = addBindings(defaultSystemsQuery);
		comparisonObjectList = sdf.createComparisonObjectList(this.engine.getEngineName(), defaultSystemsQuery);
		sdf.setComparisonObjectList(comparisonObjectList);
		
		//first get databack from the 
		updateProgressBar("20%...Evaluating Data/BLU Score", 20);
		String dataQuery = "SELECT DISTINCT ?System ?Data ?CRM WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>}{?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;}{?Provide <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>;}{?Provide <http://semoss.org/ontologies/Relation/Contains/CRM> ?CRM;}{?System ?Provide ?Data .}{?System ?UsedBy ?SystemUser}}";
		dataQuery = addBindings(dataQuery);
		String bluQuery = "SELECT DISTINCT ?System ?BLU WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>}{?BLU <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/BusinessLogicUnit>;}{?System <http://semoss.org/ontologies/Relation/Provide> ?BLU }{?System ?UsedBy ?SystemUser}}";
		bluQuery = addBindings(bluQuery);
		Hashtable<String, Hashtable<String,Double>> dataBLUHash = sdf.getDataBLUDataSet(this.engine.getEngineName(), dataQuery, bluQuery, SimilarityFunctions.VALUE);
		dataHash = processHashForCharting(dataBLUHash);
		
		String theaterQuery = "SELECT DISTINCT ?System ?Theater WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>}{?System <http://semoss.org/ontologies/Relation/Contains/GarrisonTheater> ?Theater}{?System ?UsedBy ?SystemUser}}";
		theaterQuery = addBindings(theaterQuery);
		updateProgressBar("30%...Evaluating Deployment Score", 30);
		Hashtable theaterHash = sdf.stringCompareBinaryResultGetter(this.engine.getEngineName(), theaterQuery, "Theater", "Garrison", "Both");
		theaterHash = processHashForCharting(theaterHash);
		//dataHash = processOverallScore(dataHash, theaterHash);
		
		String dwQuery = "SELECT DISTINCT ?System ?Trans WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>}{?System <http://semoss.org/ontologies/Relation/Contains/Transactional> ?Trans}{?System ?UsedBy ?SystemUser}}";
		dwQuery = addBindings(dwQuery);
		updateProgressBar("40%...Evaluating System Transactional Score", 40);
		Hashtable dwHash = sdf.stringCompareBinaryResultGetter(this.engine.getEngineName(), dwQuery, "'Yes'", "'No'", "Both");
		dwHash = processHashForCharting(dwHash);
		//dataHash = processOverallScore(dataHash, dwHash);
		
		//BP
		String bpQuery ="SELECT DISTINCT ?System ?BusinessProcess WHERE { {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System> ;} {?BusinessProcess <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/BusinessProcess> ;} {?System <http://semoss.org/ontologies/Relation/Supports> ?BusinessProcess}{?System ?UsedBy ?SystemUser}}";
		bpQuery = addBindings(bpQuery);
		updateProgressBar("50%...Evaluating System Supporting Business Processes", 50);
		Hashtable bpHash = sdf.compareObjectParameterScore(this.engine.getEngineName(), bpQuery, SimilarityFunctions.VALUE);
		bpHash = processHashForCharting(bpHash);
		
		String actQuery ="SELECT DISTINCT ?System ?Activity WHERE { {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System> ;} {?Activity <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Activity> ;} {?System <http://semoss.org/ontologies/Relation/Supports> ?Activity}{?System ?UsedBy ?SystemUser}}";
		actQuery = addBindings(actQuery);
		updateProgressBar("55%...Evaluating System Supporting Activity", 55);
		Hashtable actHash = sdf.compareObjectParameterScore(this.engine.getEngineName(), actQuery, SimilarityFunctions.VALUE);
		actHash = processHashForCharting(actHash);
		
		String userQuery ="SELECT DISTINCT ?System ?Personnel WHERE { {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System> ;} {?Personnel <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Personnel> ;} {?System <http://semoss.org/ontologies/Relation/UsedBy> ?Personnel}{?System ?UsedBy ?SystemUser}}";
		userQuery = addBindings(userQuery);
		updateProgressBar("60%...Evaluating System Users", 60);
		Hashtable userHash = sdf.compareObjectParameterScore(this.engine.getEngineName(), userQuery, SimilarityFunctions.VALUE);
		userHash = processHashForCharting(userHash);
		
		String uiQuery ="SELECT DISTINCT ?System ?UserInterface WHERE { {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System> ;} {?UserInterface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInterface> ;} {?System <http://semoss.org/ontologies/Relation/Utilizes> ?UserInterface}{?System ?UsedBy ?SystemUser}}";
		uiQuery = addBindings(uiQuery);
		updateProgressBar("70%...Evaluating User Interface", 70);
		Hashtable uiHash = sdf.compareObjectParameterScore(this.engine.getEngineName(), uiQuery, SimilarityFunctions.VALUE);
		uiHash = processHashForCharting(uiHash);
		
		ArrayList<Hashtable> hashArray = new ArrayList<Hashtable>();
		//hashArray.add(bpHash);
		//hashArray.add(actHash);
		//hashArray.add(userHash);
		//dataHash = processOverallScoreByAverage(dataHash,hashArray);
		
		boolean allQueriesAreEmpty = true;
		updateProgressBar("80%...Creating Heat Map Visualization", 80);
		if (bpHash != null && !bpHash.isEmpty()) {
			paramDataHash.put("Business_Processes_Supported", bpHash);
			allQueriesAreEmpty = false;
		}
		if (actHash != null && !actHash.isEmpty()) {
			paramDataHash.put("Activities_Supported", actHash);
			allQueriesAreEmpty = false;
		}
		if (dataHash != null && !dataHash.isEmpty()) {
			paramDataHash.put("Data_and_Business_Logic_Supported", dataHash);
			allQueriesAreEmpty = false;
		}
		if (theaterHash != null && !theaterHash.isEmpty()) {
			paramDataHash.put("Deployment_(Theater/Garrison)",  theaterHash);
			allQueriesAreEmpty = false;
		}
		if (dwHash != null && !dwHash.isEmpty()) {
			paramDataHash.put("Transactional_(Yes/No)", dwHash);
			allQueriesAreEmpty = false;
		}
		if (userHash != null && !userHash.isEmpty()) {
			paramDataHash.put("User_Types", userHash);
			allQueriesAreEmpty = false;
		}
		if (uiHash != null && !uiHash.isEmpty()) {
			paramDataHash.put("User_Interface_Types_(PC/Mobile/etc.)", uiHash);
			allQueriesAreEmpty = false;
		}		
		
		if (allQueriesAreEmpty == true) {
			Utility.showError("System Similarity Heat Map returned no results.");
			return;
		}
		
		//allHash.put("dataSeries", testDataHash);
		allHash.put("title",  "System Similarity");
		allHash.put("xAxisTitle", "System1");
		allHash.put("yAxisTitle", "System2");
		allHash.put("value", "Score");
		allHash.put("sysDup", true);

	}
	
	public String addBindings(String sysSimQuery) {
		
		String defaultBindings = "BINDINGS ?SystemUser {(<http://health.mil/ontologies/Concept/SystemOwner/Central>)(<http://health.mil/ontologies/Concept/SystemUser/Army>)(<http://health.mil/ontologies/Concept/SystemUser/Navy>)(<http://health.mil/ontologies/Concept/SystemUser/Air_Force>)}";
		
		
		//If a query is not specifed, append the default SystemUser bindings
		if ((this.query).equals("NULL") || (this.query).equals("null") || (this.query).equals("Null") || this.query == null) {
			sysSimQuery = sysSimQuery + defaultBindings;
		}
		//if a query is specified, bind the system list to the system similarity query.
		else {
			//only create the bindings string once
			if (createSystemBindings == true) {
				String systemURIs = "{";
				for( int i = 0; i < list.size(); i++) {
					Object[] values = list.get(i);
					String system = "";
					for (Object systemResult : values) {
						system = "(<http://health.mil/ontologies/Concept/System/" + systemResult.toString() + ">)";
					}
					systemURIs = systemURIs + system;
				}
				systemURIs = systemURIs + "}";
				systemListBindings = systemListBindings + systemURIs;
				createSystemBindings = false;
			}
			sysSimQuery = sysSimQuery + systemListBindings;
		}
		
		return sysSimQuery;		
	}

}
