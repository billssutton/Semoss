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
package prerna.ui.components.specific.tap.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import prerna.engine.api.IEngine;
import prerna.engine.api.ISelectStatement;
import prerna.engine.api.ISelectWrapper;
import prerna.ui.main.listener.specific.tap.FormsSourceFilesConsolidationListener;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class FormsSiteICDDataProcessor extends BaseFormsICDDataProcessor {
	public static final Logger LOGGER = LogManager.getLogger(FormsSiteICDDataProcessor.class.getName());	
	public int SITE_COL_NUM = 4;
	
	public FormsSiteICDDataProcessor(){
		PROVIDER_COL_NUM = 5;
		CONSUMER_COL_NUM = 6;
		INTERFACE_NAME_COL_NUM = 8;
		PROTOCOL_COL_NUM = 10;
		FREQUENCY_COL_NUM = 11;
		FORMAT_COL_NUM = 12;
		ICD_DATA_OBJECT_STARTING_COL_NUM = 35;
		MY_QUERY = SYSTEM_INTERFACE_SITE_SPECIFIC_QUERY;
		INTERFACES_FILE = "\\ICD SiteSpecific.xlsm";
		
		ICD_FREQUENCY_STARTING_COL_NUM = 16;
		ICD_FREQUENCY_ENDING_COL_NUM = 23;
		ICD_PROTOCOL_STARTING_COL_NUM = 24;
		ICD_PROTOCOL_ENDING_COL_NUM = 42;
		ICD_FORMAT_STARTING_COL_NUM = 43;
		ICD_FORMAT_ENDING_COL_NUM = 60;
	}
	
	public int getICDColumnNumber(String name){
		if(name.equalsIgnoreCase("Site")){
			return SITE_COL_NUM;
		}
		else {
			return super.getICDColumnNumber(name);
		}
	}
	
	public void addInterfaceName(XSSFRow row, String key1){
		//LOGGER.info("*********** key1: " + key1);
		String[] strs = key1.split("%");
		//LOGGER.info("*********** strs 1: " + strs[0]);
		//LOGGER.info("*********** strs 2: " + strs[1]);
		XSSFCell cell = row.createCell(INTERFACE_NAME_COL_NUM);
		cell.setCellValue(strs[0]);
		
		XSSFCell cell1 = row.createCell(SITE_COL_NUM);
		cell1.setCellValue(strs[1]);
	}
}
