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

import java.util.ArrayList;
import java.util.List;

import prerna.error.FileReaderException;
import prerna.ui.components.playsheets.GridPlaySheet;
import prerna.util.Utility;

public class DHMSMIntegrationSavingsBySystemFromWorksheetPerYearPlaySheet extends GridPlaySheet{
	
	List<Object[]> list;
	String[] names;
	
	@Override
	public List<Object[]> getTabularData() {
		return this.list;
	}
	
	@Override
	public String[] getColumnHeaders() {
		return this.names;
	}
	
	@Override
	public void createData() {
		DHMSMIntegrationSavingsPerYearFromWkSht reader = new DHMSMIntegrationSavingsPerYearFromWkSht();
		DHMSMIntegrationSavingsPerFiscalYearProcessor processor = new DHMSMIntegrationSavingsPerFiscalYearProcessor();
		
		boolean success = true;
		try {
			processor.runSupportQueries();
		} catch(NullPointerException ex) {
			Utility.showError(ex.getMessage());
			success = false;
		}
		
		if(success) {
			try {
				reader.read();
				ArrayList<String> systems = reader.getSystems();
				processor.runMainQueryFromWorksheetList(systems);
				processor.generateSavingsData();
				processor.processSystemData();
				list = processor.getSystemOutputList();
				names = processor.getSysNames();
			} catch (FileReaderException e) {
				Utility.showError(e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
