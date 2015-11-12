/*******************************************************************************
 * Copyright 2015 Defense Health Agency (DHA)
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
package prerna.ui.components.playsheets;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import prerna.util.Constants;
import prerna.util.DIHelper;

/**
 * The GridScatterPlaySheet class creates the panel and table for a scatter plot view of data from a SPARQL query.
 */
public class ScatterChartPlaySheet extends BrowserPlaySheet{
	int offset = 0;
	
	/**
	 * Constructor for GridScatterSheet.
	 */
	public ScatterChartPlaySheet() {
		super();
		this.setPreferredSize(new Dimension(800,600));
		String workingDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		//fileName  = "file://" + workingDir + "/html/MHS-RDFSemossCharts/app/singlechartgrid.html";
		fileName = "file://" + workingDir + "/html/MHS-RDFSemossCharts/app/scatterplot.html";
	}
	
	/**
	 * Method processQueryData.  Processes the data from the SPARQL query into an appropriate format for the specific play sheet.
	
	 * @return Hashtable Includes the data series, graph title, and the x- and y-axis titles.*/
	public void processQueryData()
	{
		String[] names = dataFrame.getColumnHeaders();
		String name = names[0];
		boolean hasType = true;

		if(dataFrame.isNumeric(names[1])) {
			hasType = false;
		} else {
			offset = 1;
		}
		
		Iterator<Object[]> it = dataFrame.iterator(true);
		ArrayList<Hashtable<String, Object>> allData = new ArrayList<Hashtable<String, Object>>();
		while(it.hasNext())
		{
			Hashtable<String, Object> elementHash = new Hashtable<String, Object>();
			Object[] listElement = it.next();
			
			if(hasType) {
				name = listElement[0].toString();
			}
			
			elementHash.put("series", name);
			elementHash.put("label", listElement[0+offset]);
			elementHash.put("x", listElement[1+offset]);
			if(listElement.length > 2 + offset)
				elementHash.put("y", listElement[2+offset]);
			if(listElement.length > 3 + offset)
				elementHash.put("z", listElement[3+offset]);
			if(offset == 0 && listElement.length > 4)
				elementHash.put("heat", listElement[4]);
			
			allData.add(elementHash);
		}
		Hashtable<String, Object> allHash = new Hashtable<String, Object>();
		allHash.put("dataSeries", allData);
		allHash.put("title",  names[1 + offset] + " vs " + names[2 + offset]);
		allHash.put("labelHeader", names[0 + offset]);
		allHash.put("xAxisTitle", names[1 + offset]);
		if(names.length > 2 + offset)
			allHash.put("yAxisTitle", names[2 + offset]);
		if(names.length > 3 + offset)
			allHash.put("zAxisTitle", names[3 + offset]);

		this.dataHash = allHash;
	}
	
	@Override
	public Hashtable<String, String> getDataTableAlign() {
		Hashtable<String, String> alignHash = new Hashtable<String, String>();
		String[] names = dataFrame.getColumnHeaders();
		
		if(offset != 0)
			alignHash.put("series", names[0]);
		alignHash.put("label", names[0 + offset]);
		alignHash.put("x", names[1 + offset]);
		if(names.length > 2 + offset)
			alignHash.put("y", names[2 + offset]);
		if(names.length > 3 + offset)
			alignHash.put("z", names[3 + offset]);
		return alignHash;
	}
	
}
