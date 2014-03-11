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
package prerna.ui.components.specific.tap;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;

import prerna.ui.components.playsheets.BrowserPlaySheet;
import prerna.ui.main.listener.specific.tap.CapabilityFactSheetListener;
import prerna.util.DIHelper;

import com.google.gson.Gson;
import com.teamdev.jxbrowser.events.NavigationEvent;
import com.teamdev.jxbrowser.events.NavigationFinishedEvent;
import com.teamdev.jxbrowser.events.NavigationListener;
import com.teamdev.jxbrowser.print.PrintAdapter;

/**
 * This class creates the capability fact sheet.
 */
public class CapabilityFactSheet extends BrowserPlaySheet{

	Hashtable allHash = new Hashtable();
	Hashtable capabilityHash = new Hashtable();
	//keys are processed capabilities and values are semoss stored capabilities
	public Hashtable capabilityProcessed = new Hashtable();
	
	CapabilityFactSheetListener singleCapFactSheetCall = new CapabilityFactSheetListener();
	
	/**
	 * Constructor for CapabilityFactSheet. Generates the landing page for the capability fact sheets.
	 */
	public CapabilityFactSheet() {
		super();
		this.setPreferredSize(new Dimension(800,600));
	}

	/**
	 * Processes all Sys Dupe queries and shows results in sysdupe.html format.
	 */
	@Override
	public void createView()
	{		
		String workingDir = System.getProperty("user.dir");
		
		singleCapFactSheetCall.setCapabilityFactSheet(this);
		//browser.navigate("file://" + workingDir + "/html/MHS-FactSheets/Capability Fact Sheet.html");
		//singleCapFactSheetCall.invoke(null);
		
		browser.addNavigationListener(new NavigationListener() {
    	    public void navigationStarted(NavigationEvent event) {
    	    	logger.info("event.getUrl() = " + event.getUrl());
    	    }

    	    public void navigationFinished(NavigationFinishedEvent event) {
   	    	browser.registerFunction("singleCapFactSheet",  singleCapFactSheetCall);
	   	    	File file = new File(DIHelper.getInstance().getProperty("BaseFolder") + "/html/MHS-FactSheets/export.json");
	   			if(file.exists()) {
	   				file.delete();
	   				browser.executeScript("window.location.reload()");
	   			}
    			callIt();
    	    }
    	});
	       
		browser.navigate("file://" + workingDir + "/html/MHS-FactSheets/index.html");
	}

	
	/**
	 * Method processQueryData.  Processes the data from the SPARQL query into an appropriate format for the specific play sheet.
	
	 * @return Hashtable Includes the data series.*/
	public Hashtable processQueryData()
	{
		addPanel();
		ArrayList dataArrayList = new ArrayList();
		String[] var = wrapper.getVariables(); 		
		for (int i=0; i<list.size(); i++)
		{	
			Object[] listElement = list.get(i);
		//	for (int j = 0; j < var.length; j++) 
		//	{	
					String text = (String) listElement[0];
					String source = (String) listElement[1];
					String processedText = text.replaceAll("\\[", "(").replaceAll("\\]", ")").replaceAll(",", "").replaceAll("&", "").replaceAll("\'","").replaceAll("�", "")+" ("+source+")";
					capabilityProcessed.put(processedText,text);
					dataArrayList.add(processedText);
		//	}			
		}

		capabilityHash.put("dataSeries", dataArrayList);
		
		return capabilityHash;
	}
	
	public Hashtable processNewCapability(String capability)
	{		
		CapabilityFactSheetPerformer performer = new CapabilityFactSheetPerformer();
		Hashtable<String,Object> dataSeries = new Hashtable<String,Object>();

		updateProgressBar("10%...Processing Capability Dupe", 10);
		Hashtable<String, Object> capabilityDupeSheetHash = performer.processCapabilityDupeSheet(capability);
		dataSeries.put("CapabilityDupeSheet", capabilityDupeSheetHash);
		
		updateProgressBar("25%...Processing Data Objects", 25);
		Hashtable<String, Object> dataSheet = performer.processDataSheetQueries(capability);
		dataSeries.put("DataSheet", dataSheet);
		
		updateProgressBar("30%...Processing Systems", 30);
		Hashtable<String, Object> systemSheet = performer.processSystemQueries(capability);
		dataSeries.put("SystemSheet", systemSheet);
		
		updateProgressBar("60%...Processing Tasks and BPs", 60);
		Hashtable<String, Object> taskAndBPSheetHash = performer.processTaskandBPQueries(capability);
		dataSeries.put("TaskAndBPSheet", taskAndBPSheetHash);
		
		updateProgressBar("65%...Processing Requirements and Standards", 65);
		Hashtable<String, Object> reqAndStandardSheet = performer.processRequirementsAndStandardsQueries(capability);
		dataSeries.put("ReqAndStandardSheet", reqAndStandardSheet);
		
		updateProgressBar("70%...Processing BLUs", 70);
		Hashtable<String, Object> bluSheet = performer.processBLUSheetQueries(capability);
		dataSeries.put("BLUSheet", bluSheet);
		
		updateProgressBar("75%...Processing FunctionalGaps", 75);
		Hashtable<String, Object> funtionalGapSheet = performer.processFunctionalGapSheetQueries(capability);
		dataSeries.put("FunctionalGapSheet", funtionalGapSheet);
		
		updateProgressBar("80%...Processing Capability Overview", 80);
		Hashtable<String, Object> firstSheetHash = performer.processFirstSheetQueries(capability);
		dataSeries.put("CapabilityOverviewSheet", firstSheetHash);

		allHash.put("dataSeries", dataSeries);
		allHash.put("capability", capability);

	//	callItAllHash();
		updateProgressBar("100%...Capability Fact Sheet Generation Complete", 100);
		return allHash;
	}
	
	public void callIt()
	{
		System.err.println(">>> callIt");
		Gson gson = new Gson();
//		browser.executeScript("capabilityList('" + gson.toJson(capabilityHash) + "');");
		String json = gson.toJson(capabilityHash);
		browser.executeScript("start('" + gson.toJson(capabilityHash) + "');");
		System.out.println(gson.toJson(capabilityHash));
	}
	
	public void callItAllHash()
	{
		System.err.println(">>> callItAllHash");
		Gson gson = new Gson();
//		browser.executeScript("capabilityData('" + gson.toJson(allHash) + "');");
		String workingDir = System.getProperty("user.dir");
		browser.navigate("file://" + workingDir + "/html/MHS-FactSheets/index.html#/cap");
		browser.executeScript("start('" + gson.toJson(allHash) + "');");
	}
	
}


