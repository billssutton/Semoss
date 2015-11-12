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
package prerna.ui.helpers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JDesktopPane;

import prerna.om.Insight;
import prerna.om.InsightStore;
import prerna.ui.components.api.IPlaySheet;
import prerna.ui.components.playsheets.AbstractPlaySheet;
import prerna.ui.components.playsheets.datamakers.DataMakerComponent;
import prerna.ui.components.playsheets.datamakers.IDataMaker;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

/**
 * This class handles everything involved with creating an insight
 * The creating of an insight currently varies significantly between web and thick
 * Web: use data maker and components to create data, then send to front end
 * Thick: use data maker and components to create data, then set in playsheet and call necessary methods there
 */
public class InsightCreateRunner implements Runnable{

	Insight insight= null;
	
	/**
	 * Constructor for PlaysheetCreateRunner.
	 * @param playSheet IPlaySheet
	 */
	public InsightCreateRunner(Insight insight)
	{
		this.insight = insight;
	}
	
	/**
	 * Method run.  Calls the create view method on the local play sheet.
	 */
	@Override
	public void run() {
		IDataMaker dm = createData();
		IPlaySheet playSheet = insight.getPlaySheet();
		playSheet.setDataMaker(dm);
		preparePlaySheet(playSheet, insight);

		if(!insight.getAppend()){
			playSheet.runAnalytics();
			playSheet.processQueryData();
			playSheet.createView();
		}
		else {
			playSheet.runAnalytics();
			playSheet.overlayView();
		}
		
	}
	
	/** 
	 * This creates the data needed for the insight
	 * Get the data maker from the insight
	 * Feed each component one by one into data maker
	 */
	private IDataMaker createData(){
		IDataMaker dm = insight.getDataMaker();
		
//		DataMakerComponent[] dmComps = insight.getDataMakerComponents();
		List<DataMakerComponent> dmComps = insight.getDataMakerComponents();
		insight.appendParamsToDataMakerComponents();
		for(DataMakerComponent dmComp : dmComps){
			dm.processDataMakerComponent(dmComp);
		}
		return dm;
	}
	
	public Map<String, Object> runWeb()
	{
		IDataMaker dm = createData();
		Map<String, String> tableDataAlign = insight.getDataTableAlign();
		
		if(tableDataAlign == null || tableDataAlign.isEmpty()) {
			IPlaySheet playSheet = insight.getPlaySheet();
			playSheet.setDataMaker(dm);
			tableDataAlign = (Map<String, String>) (((AbstractPlaySheet) playSheet).getDataTableAlign());
			insight.setDataTableAlign(tableDataAlign);
		}
		
		Map<String, Object> retHash = new HashMap<String, Object>();
		retHash.put("insight", this.insight);
		retHash.put("data", dm); // TODO: what should this be? getData()?
		return retHash;
	}
	
	private void preparePlaySheet(IPlaySheet playSheet, Insight insight){
		// SET THE DESKTOP PANE
		playSheet.setJDesktopPane((JDesktopPane) DIHelper.getInstance().getLocalProp(Constants.DESKTOP_PANE));
		
		// CREATE INSIGHT ID
//		QuestionPlaySheetStore.getInstance().idCount++;
//		String insightID = insight.getInsightID();
//		if(insightID == null){
//			insightID = "Custom";
//		}
//		insightID = QuestionPlaySheetStore.getInstance().getIDCount()+". "+ insightID;
//		playSheet.setQuestionID(insightID);
//		QuestionPlaySheetStore.getInstance().put(insightID,  playSheet);
		
		String insightID = InsightStore.getInstance().put(insight);
		playSheet.setQuestionID(insightID);
		
		// CREATE THE INSIGHT TITLE
		String playSheetTitle = "";
		Map<String, List<Object>> paramHash = insight.getParamHash();
		if(paramHash != null && !paramHash.isEmpty())
		{
			Iterator<String> enumKey = paramHash.keySet().iterator();
			while (enumKey.hasNext())
			{
				String key = (String) enumKey.next();
				List<Object> value = paramHash.get(key);
				for(int i = 0; i < value.size(); i++) {
					Object val = value.get(i);
					if(val instanceof String || val instanceof Double )
					{
						playSheetTitle = playSheetTitle + Utility.getInstanceName(value+"") + " - ";
					}
				}
			}
		}
		String name = insight.getInsightName();
		if (name == null){
			name = insightID;
		}
		System.out.println("Param Hash is " + paramHash);
		playSheetTitle = playSheetTitle+name.trim();
		playSheet.setTitle(playSheetTitle);
	}
}
