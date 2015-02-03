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

import java.util.ArrayList;

import prerna.error.FileReaderException;
import prerna.ui.components.playsheets.GridPlaySheet;
import prerna.util.Utility;

public class DHMSMIntegrationSavingsBySystemFromWorksheetPerYearPlaySheet extends GridPlaySheet{
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
