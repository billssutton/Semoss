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
package prerna.ui.components.playsheets;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.util.Constants;
import prerna.util.DIHelper;

import com.google.gson.Gson;
import com.teamdev.jxbrowser.chromium.JSValue;

/**
 * The Play Sheet for Outside the Continental United States (OCONUS) geo-location data. Visualizes Latitude/Longitude
 * coordinates on a map of OCONUS.
 */
public class OCONUSMapPlaySheet extends BrowserPlaySheet {
	
	private static final Logger logger = LogManager.getLogger(OCONUSMapPlaySheet.class.getName());
	protected Hashtable allHash;
	protected HashSet data;
	
	/**
	 * Constructor for OCONUSMapPlaySheet.
	 */
	public OCONUSMapPlaySheet() {
		super();
		this.setPreferredSize(new Dimension(1600, 1150));
		String workingDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		fileName = "file://" + workingDir + "/html/MHS-RDFSemossCharts/app/worldmap.html";
	}
	
	/**
	 * Method processQueryData. Processes the data from the SPARQL query into an appropriate format for the specific play
	 * sheet.
	 * 
	 * @return Hashtable - A Hashtable of the queried Continental United States data to be converted into json format. The
	 *         data must be in the format lat, lon, size, and must include any relevant properties for the coordinate.
	 */
	public Hashtable processQueryData() {
		data = new HashSet();
		String[] var = getVariableArray();
		
		// Possibly filter out all US Facilities from the query?
		
		for (int i = 0; i < list.size(); i++) {
			LinkedHashMap elementHash = new LinkedHashMap();
			Object[] listElement = list.get(i);
			String colName;
			Double value;
			for (int j = 0; j < var.length; j++) {
				colName = var[j];
				elementHash.put("size", 1000000);
				if (listElement[j] instanceof String) {
					String text = (String) listElement[j];
					elementHash.put(colName, text);
				} else {
					value = (Double) listElement[j];
					elementHash.put(colName, value);
				}
				
			}
			data.add(elementHash);
		}
		
		allHash = new Hashtable();
		allHash.put("dataSeries", data);
		
		allHash.put("lat", var[1]);
		allHash.put("lon", var[2]);
		allHash.put("size", "size");
		allHash.put("locationName", var[0]);
		/*
		 * allHash.put("xAxisTitle", var[0]); allHash.put("yAxisTitle", var[1]); allHash.put("value", var[2]);
		 */
		
		return allHash;
	}
	
	public String[] getVariableArray() {
		return wrapper.getVariables();
	}
	
	@Override
	/**
	 * Method callIt.  Converts a given Hashtable to a Json and passes it to the browser.
	 * @param table Hashtable - the correctly formatted data from the SPARQL query results.
	 */
	public void callIt(final Hashtable table) {
		output = table;
		Gson gson = new Gson();
		logger.info("Converted gson");
		JSValue val = browser.executeJavaScriptAndReturnValue("start('" + gson.toJson(table) + "');");
		
		output.clear();
		allHash.clear();
		data.clear();
	}
	
}
