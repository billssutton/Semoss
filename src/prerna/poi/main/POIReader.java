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
package prerna.poi.main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openrdf.sail.SailException;

import prerna.engine.api.IEngine;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

/**
 * Loading data into SEMOSS using Microsoft Excel Loading Sheet files
 */
public class POIReader extends AbstractFileReader {

	private static final Logger logger = LogManager.getLogger(POIReader.class.getName());

	/**
	 * Load data into SEMOSS into an existing database
	 * 
	 * @param engineName
	 *            String grabbed from the user interface specifying which database to add the data
	 * @param fileNames
	 *            Absolute paths of files the user wants to load into SEMOSS, paths are separated by ";"
	 * @param customBase
	 *            String grabbed from the user interface that is used as the URI base for all instances
	 * @param customMap
	 *            Absolute path that determines the location of the current db map file for the data
	 * @param owlFile
	 *            String automatically generated within SEMOSS to determine the location of the OWL file that is produced
	 * @throws EngineException
	 * @throws FileReaderException
	 * @throws FileWriterException
	 * @throws InvalidUploadFormatException
	 */
	public void importFileWithConnection(String engineName, String fileNames, String customBase, String customMap, String owlFile)
			throws FileNotFoundException, IOException {
		logger.setLevel(Level.ERROR);
		String[] files = prepareReader(fileNames, customBase, owlFile, engineName);
		openEngineWithConnection(engineName);

		// load map file for existing db
		if (!customMap.isEmpty()) {
			openProp(customMap);
		}
		for (String fileName : files) {
			importFile(fileName);
		}
		createBaseRelations();
		commitDB();
	}

	/**
	 * Loading data into SEMOSS to create a new database
	 * 
	 * @param dbName
	 *            String grabbed from the user interface that would be used as the name for the database
	 * @param fileNames
	 *            Absolute paths of files the user wants to load into SEMOSS, paths are separated by ";"
	 * @param customBase
	 *            String grabbed from the user interface that is used as the URI base for all instances
	 * @param customMap
	 *            Absolute path that determines the location of a custom map file for the data
	 * @param owlFile
	 *            String automatically generated within SEMOSS to determine the location of the OWL file that is produced
	 * @throws EngineException
	 * @throws FileReaderException
	 * @throws FileWriterException
	 * @throws InvalidUploadFormatException
	 */
	public void importFileWithOutConnection(String smssLocation, String engineName, String fileNames, String customBase, String customMap, String owlFile)
			throws FileNotFoundException, IOException {
		String[] files = prepareReader(fileNames, customBase, owlFile, smssLocation);
		try {
			openEngineWithoutConnection(engineName);
			// load map file for db if user wants to use specific URIs
			if (!customMap.isEmpty()) {
				openProp(customMap);
			}
			// if user selected a map, load just as before--using the prop file to discover Excel->URI translation
			for (String fileName : files) {
				importFile(fileName);
			}
			createBaseRelations();
		} finally {
			closeDB();
			closeOWL();
		}
	}

	/**
	 * Load subclassing information into the db and the owl file Requires the data to be in specific excel tab labeled "Subclass", with Parent nodes
	 * in the first column and child nodes in the second column
	 * 
	 * @param subclassSheet
	 *            Excel sheet with the subclassing information
	 * @throws EngineException
	 * @throws SailException
	 */
	private void createSubClassing(XSSFSheet subclassSheet) {
		//		try {
		//			if (!scOWL.isActive() || !scOWL.isOpen()) {
		//				scOWL.begin();
		//			}
		//		} catch (SailException e1) {
		//			e1.printStackTrace();
		//			throw new EngineException("Error opening connection to OWL file");
		//		}
		// URI for subclass
		String pred = Constants.SUBCLASS_URI;
		String semossNodeURI = semossURI + "/" + Constants.DEFAULT_NODE_CLASS;
		// check parent and child nodes in correct position
		XSSFRow row = subclassSheet.getRow(0);
		String parentNode = row.getCell(0).toString().trim().toLowerCase();
		String childNode = row.getCell(1).toString().trim().toLowerCase();
		// check to make sure parent column is in the correct column
		if (!parentNode.equals("parent")) {
			JFrame playPane = (JFrame) DIHelper.getInstance().getLocalProp(Constants.MAIN_FRAME);
			JOptionPane.showMessageDialog(playPane, "<html>Error with Subclass Sheet.<br>Error in parent node column.</html>");
		}
		// check to make sure child column is in the correct column
		if (!childNode.equals("child")) {
			JFrame playPane = (JFrame) DIHelper.getInstance().getLocalProp(Constants.MAIN_FRAME);
			JOptionPane.showMessageDialog(playPane, "<html>Error with Subclass Sheet.<br>Error in child node column.</html>");
		}
		// loop through and create all the triples for subclassing
		int lastRow = subclassSheet.getLastRowNum();
		for (int i = 1; i <= lastRow; i++) {
			row = subclassSheet.getRow(i);
			parentNode = semossNodeURI + "/" + Utility.cleanString(row.getCell(0).toString(), true);
			childNode = semossNodeURI + "/" + Utility.cleanString(row.getCell(1).toString(), true);
			// add triples to engine
			engine.doAction(IEngine.ACTION_TYPE.ADD_STATEMENT, new Object[]{childNode, pred, parentNode, true});
			engine.doAction(IEngine.ACTION_TYPE.ADD_STATEMENT, new Object[]{childNode, pred, semossNodeURI, true});
			engine.doAction(IEngine.ACTION_TYPE.ADD_STATEMENT, new Object[]{parentNode, pred, semossNodeURI, true});
			// add triples to OWL
			baseEngCreator.addToBaseEngine(new Object[]{childNode, pred, parentNode, true});
			baseEngCreator.addToBaseEngine(new Object[]{childNode, pred, semossNodeURI, true});
			baseEngCreator.addToBaseEngine(new Object[]{parentNode, pred, semossNodeURI, true});
		}
		engine.commit();
		baseEngCreator.commit();
	}

	/**
	 * Load the excel workbook, determine which sheets to load in workbook from the Loader tab
	 * @param fileName 					String containing the absolute path to the excel workbook to load
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void importFile(String fileName) throws FileNotFoundException, IOException {

		XSSFWorkbook workbook = null;
		FileInputStream poiReader = null;
		try {
			poiReader = new FileInputStream(fileName);
			workbook = new XSSFWorkbook(poiReader);
			// load the Loader tab to determine which sheets to load
			XSSFSheet lSheet = workbook.getSheet("Loader");
			if (lSheet == null) {
				throw new IOException("Could not find Loader Sheet in Excel file " + fileName);
			}
			// check if user is loading subclassing relationships
			XSSFSheet subclassSheet = workbook.getSheet("Subclass");
			if (subclassSheet != null) {
				createSubClassing(subclassSheet);
			}
			// determine number of sheets to load
			int lastRow = lSheet.getLastRowNum();
			// first sheet name in second row
			for (int rIndex = 1; rIndex <= lastRow; rIndex++) {
				XSSFRow row = lSheet.getRow(rIndex);
				// check to make sure cell is not null
				if (row != null) {
					XSSFCell sheetNameCell = row.getCell(0);
					XSSFCell sheetTypeCell = row.getCell(1);
					if (sheetNameCell != null && sheetTypeCell != null) {
						// get the name of the sheet
						String sheetToLoad = sheetNameCell.getStringCellValue();
						// determine the type of load
						String loadTypeName = sheetTypeCell.getStringCellValue();
						if (!sheetToLoad.isEmpty() && !loadTypeName.isEmpty()) {
							logger.debug("Cell Content is " + sheetToLoad);
							// this is a relationship
							if (loadTypeName.contains("Matrix")) {
								loadMatrixSheet(sheetToLoad, workbook);
								engine.commit();
							} else {
								loadSheet(sheetToLoad, workbook);
								engine.commit();
							}
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if(e.getMessage()!= null && !e.getMessage().isEmpty()) {
				throw new FileNotFoundException(e.getMessage());
			} else {
				throw new FileNotFoundException("Could not find Excel file located at " + fileName);
			}
		} catch (IOException e) {
			e.printStackTrace();
			if(e.getMessage()!= null && !e.getMessage().isEmpty()) {
				throw new IOException(e.getMessage());
			} else {
				throw new IOException("Could not read Excel file located at " + fileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(e.getMessage()!= null && !e.getMessage().isEmpty()) {
				throw new IOException(e.getMessage());
			} else {
				throw new IOException("File: " + fileName + " is not a valid Microsoft Excel (.xlsx, .xlsm) file");
			}
		} finally {
			if(poiReader != null) {
				try {
					poiReader.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new IOException("Could not close Excel file stream");
				}
			}
		}
	}

	/**
	 * Load specific sheet in workbook
	 * 
	 * @param sheetToLoad
	 *            String containing the name of the sheet to load
	 * @param workbook
	 *            XSSFWorkbook containing the sheet to load
	 * @throws IOException
	 */
	public void loadSheet(String sheetToLoad, XSSFWorkbook workbook) throws IOException {

		XSSFSheet lSheet = workbook.getSheet(sheetToLoad);
		if (lSheet == null) {
			throw new IOException("Could not find sheet " + sheetToLoad + " in workbook.");
		}
		logger.info("Loading Sheet: " + sheetToLoad);
		System.out.println(">>>>>>>>>>>>>>>>> " + sheetToLoad);
		int lastRow = lSheet.getLastRowNum() + 1;

		// Get the first row to get column names
		XSSFRow row = lSheet.getRow(0);

		// initialize variables
		String objectNode = "";
		String relName = "";
		Vector<String> propNames = new Vector<String>();

		// determine if relationship or property sheet
		String sheetType = row.getCell(0).getStringCellValue();
		String subjectNode = row.getCell(1).getStringCellValue();
		int currentColumn = 0;
		if (sheetType.equalsIgnoreCase("Relation")) {
			objectNode = row.getCell(2).getStringCellValue();
			// if relationship, properties start at column 2
			currentColumn++;
		}

		// determine property names for the relationship or node
		// colIndex starts at currentColumn+1 since if relationship, the object node name is in the second column
		int lastColumn = 0;
		for (int colIndex = currentColumn + 1; colIndex < row.getLastCellNum(); colIndex++) {
			// add property name to vector
			if (row.getCell(colIndex) != null) {
				propNames.addElement(row.getCell(colIndex).getStringCellValue());
				lastColumn = colIndex;
			}
		}
		logger.info("Number of Columns: " + (lastColumn + 1));

		// processing starts
		logger.info("Number of Rows: " + lastRow);
		for (int rowIndex = 1; rowIndex < lastRow; rowIndex++) {
			// first cell is the name of relationship
			XSSFRow nextRow = lSheet.getRow(rowIndex);

			if (nextRow == null) {
				continue;
			}

			// get the name of the relationship
			if (rowIndex == 1) {
				relName = nextRow.getCell(0).getStringCellValue();
			}

			// set the name of the subject instance node to be a string
			if (nextRow.getCell(1) != null && nextRow.getCell(1).getCellType() != XSSFCell.CELL_TYPE_BLANK) {
				nextRow.getCell(1).setCellType(XSSFCell.CELL_TYPE_STRING);
			}

			// to prevent errors when java thinks there is a row of data when the row is empty
			XSSFCell instanceSubjectNodeCell = nextRow.getCell(1);
			String instanceSubjectNode = "";
			if (instanceSubjectNodeCell != null && instanceSubjectNodeCell.getCellType() != XSSFCell.CELL_TYPE_BLANK
					&& !instanceSubjectNodeCell.toString().isEmpty()) {
				instanceSubjectNode = nextRow.getCell(1).getStringCellValue();
			} else {
				continue;
			}

			// get the name of the object instance node if relationship
			String instanceObjectNode = "";
			int startCol = 1;
			int offset = 1;
			if (sheetType.equalsIgnoreCase("Relation")) {
				if(nextRow.getCell(2) != null) {
					nextRow.getCell(2).setCellType(XSSFCell.CELL_TYPE_STRING);
					XSSFCell instanceObjectNodeCell = nextRow.getCell(2);
					if (instanceObjectNodeCell != null && instanceObjectNodeCell.getCellType() != XSSFCell.CELL_TYPE_BLANK
							&& !instanceObjectNodeCell.toString().isEmpty()) {
						instanceObjectNode = nextRow.getCell(2).getStringCellValue();
					} else {
						continue;
					}
				}
				startCol++;
				offset++;
			}

			Hashtable<String, Object> propHash = new Hashtable<String, Object>();
			// process properties
			for (int colIndex = (startCol + 1); colIndex < nextRow.getLastCellNum(); colIndex++) {
				if (propNames.size() <= (colIndex - offset)) {
					continue;
				}
				String propName = propNames.elementAt(colIndex - offset).toString();
				String propValue = "";
				if (nextRow.getCell(colIndex) == null || nextRow.getCell(colIndex).getCellType() == XSSFCell.CELL_TYPE_BLANK
						|| nextRow.getCell(colIndex).toString().isEmpty()) {
					continue;
				}
				if (nextRow.getCell(colIndex).getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
					if (DateUtil.isCellDateFormatted(nextRow.getCell(colIndex))) {
						Date date = (Date) nextRow.getCell(colIndex).getDateCellValue();
						propHash.put(propName, date);
					} else {
						Double dbl = new Double(nextRow.getCell(colIndex).getNumericCellValue());
						propHash.put(propName, dbl);
					}
				} else {
					nextRow.getCell(colIndex).setCellType(XSSFCell.CELL_TYPE_STRING);
					propValue = nextRow.getCell(colIndex).getStringCellValue();
					propHash.put(propName, propValue);
				}
			}

			if (sheetType.equalsIgnoreCase("Relation")) {
				// adjust indexing since first row in java starts at 0
				logger.info("Processing Relationship Sheet: " + sheetToLoad + ", Row: " + (rowIndex + 1));
				createRelationship(subjectNode, objectNode, instanceSubjectNode, instanceObjectNode, relName, propHash);
			} else {
				addNodeProperties(subjectNode, instanceSubjectNode, propHash);
			}
			if (rowIndex == (lastRow - 1)) {
				logger.info("Done processing: " + sheetToLoad);
			}
		}
	}

	/**
	 * Load excel sheet in matrix format
	 * 
	 * @param sheetToLoad
	 *            String containing the name of the excel sheet to load
	 * @param workbook
	 *            XSSFWorkbook containing the name of the excel workbook
	 * @throws EngineException
	 */
	public void loadMatrixSheet(String sheetToLoad, XSSFWorkbook workbook) {
		XSSFSheet lSheet = workbook.getSheet(sheetToLoad);
		int lastRow = lSheet.getLastRowNum();
		logger.info("Number of Rows: " + lastRow);

		// Get the first row to get column names
		XSSFRow row = lSheet.getRow(0);
		// initialize variables
		String objectNodeType = "";
		String relName = "";
		boolean propExists = false;

		String sheetType = row.getCell(0).getStringCellValue();
		// Get the string that contains the subject node type, object node type, and properties
		String nodeMap = row.getCell(1).getStringCellValue();

		// check to see if properties exist
		String propertyName = "";
		StringTokenizer tokenProperties = new StringTokenizer(nodeMap, "@");
		String triple = tokenProperties.nextToken();
		if (tokenProperties.hasMoreTokens()) {
			propertyName = tokenProperties.nextToken();
			propExists = true;
		}

		StringTokenizer tokenTriple = new StringTokenizer(triple, "_");
		String subjectNodeType = tokenTriple.nextToken();
		if (sheetType.equalsIgnoreCase("Relation")) {
			relName = tokenTriple.nextToken();
			objectNodeType = tokenTriple.nextToken();
		}

		// determine object instance names for the relationship
		ArrayList<String> objectInstanceArray = new ArrayList<String>();
		int lastColumn = 0;
		for (int colIndex = 2; colIndex < row.getLastCellNum(); colIndex++) {
			objectInstanceArray.add(row.getCell(colIndex).getStringCellValue());
			lastColumn = colIndex;
		}
		// fix number of columns due to data shift in excel sheet
		lastColumn--;
		logger.info("Number of Columns: " + lastColumn);

		try {
			// process all rows (contains subject instances) in the matrix
			for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
				// boolean to determine if a mapping exists
				boolean mapExists = false;
				XSSFRow nextRow = lSheet.getRow(rowIndex);
				// get the name subject instance
				String instanceSubjectName = nextRow.getCell(1).getStringCellValue();
				// see what relationships are mapped between subject instances and object instances
				for (int colIndex = 2; colIndex <= lastColumn; colIndex++) {
					String instanceObjectName = objectInstanceArray.get(colIndex - 2);
					Hashtable<String, Object> propHash = new Hashtable<String, Object>();
					// store value in cell between instance subject and object in current iteration of loop
					XSSFCell matrixContent = nextRow.getCell(colIndex);
					// if any value in cell, there should be a mapping
					if (matrixContent != null) {
						if (propExists) {
							if (matrixContent.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
								if (DateUtil.isCellDateFormatted(matrixContent)) {
									propHash.put(propertyName, (Date) matrixContent.getDateCellValue());
									mapExists = true;
								} else {
									propHash.put(propertyName, new Double(matrixContent.getNumericCellValue()));
									mapExists = true;
								}
							} else {
								// if not numeric, assume it is a string and check to make sure it is not empty
								if (!matrixContent.getStringCellValue().isEmpty()) {
									propHash.put(propertyName, matrixContent.getStringCellValue());
									mapExists = true;
								}
							}
						} else {
							mapExists = true;
						}
					}

					if (sheetType.equalsIgnoreCase("Relation") && mapExists) {
						logger.info("Processing" + sheetToLoad + " Row " + rowIndex + " Column " + colIndex);
						createRelationship(subjectNodeType, objectNodeType, instanceSubjectName, instanceObjectName, relName, propHash);
					} else {
						logger.info("Processing" + sheetToLoad + " Row " + rowIndex + " Column " + colIndex);
						addNodeProperties(subjectNodeType, instanceSubjectName, propHash);
					}
				}
				logger.info(instanceSubjectName);
			}
		} finally {
			logger.info("Done processing: " + sheetToLoad);
		}
	}

}
