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

import prerna.ui.components.playsheets.GridPlaySheet;

public class DHMSMIntegrationSavingsPerFiscalYearBySitePlaySheet extends GridPlaySheet {

	@Override
	public void createData() {
		DHMSMIntegrationSavingsPerFiscalYearProcessor processor = new DHMSMIntegrationSavingsPerFiscalYearProcessor();
		processor.runSupportQueries();
		if(query.equalsIgnoreCase("None")) {
			processor.runMainQuery("");
		} else {
			processor.runMainQuery(query);
		}
		processor.generateSavingsData();
		processor.processSiteData();
		list = processor.getList();
		names = processor.getNames();
	}
	
}

