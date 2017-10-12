package prerna.sablecc2.reactor.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.Rserve.RConnection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import au.com.bytecode.opencsv.CSVReader;
import prerna.algorithm.api.IMetaData;
import prerna.algorithm.learning.matching.DomainValues;
import prerna.cache.ICache;
import prerna.ds.OwlTemporalEngineMeta;
import prerna.ds.QueryStruct;
import prerna.ds.TinkerFrame;
import prerna.ds.h2.H2Frame;
import prerna.ds.nativeframe.NativeFrame;
import prerna.ds.r.RDataTable;
import prerna.engine.api.IEngine;
import prerna.engine.api.IHeadersDataRow;
import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.poi.main.HeadersException;
import prerna.poi.main.helper.CSVFileHelper;
import prerna.poi.main.helper.XLFileHelper;
import prerna.query.querystruct.CsvQueryStruct;
import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.task.ConstantDataTask;
import prerna.sablecc2.reactor.imports.H2Importer;
import prerna.sablecc2.reactor.imports.ImportUtility;
import prerna.sablecc2.reactor.imports.RImporter;
import prerna.util.ArrayUtilityMethods;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

public abstract class AbstractBaseRClass extends AbstractJavaReactorBaseClass {

	public static final String R_CONN = "R_CONN";
	public static final String R_PORT = "R_PORT";
	public static final String R_ENGINE = "R_ENGINE";
	public static final String R_GRAQH_FOLDERS = "R_GRAQH_FOLDERS";

	private static long counter = 0;

	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	/////////////////// Abstract R Methods /////////////////////

	/*
	 * These are the methods that we cannot make generic between the
	 * implementations of R
	 */

	protected abstract Object startR();

	protected abstract Object eval(String script);

	protected abstract void runR(String script);

	protected abstract void runR(String script, boolean outputResult);

	protected abstract String getWd();

	protected abstract void endR();

	// graph specific abstract methods
	protected abstract void colorClusters(String clusterName);

	protected abstract void key();

	protected abstract void synchronizeXY(String rVarName);

	// table specific abstract methods

	protected abstract int getNumRows(String frameName);

	protected abstract String getColType(String frameName, String colName, boolean print);

	protected abstract String[] getColTypes(String frameName, boolean print);

	protected abstract String[] getColNames(String frameName, boolean print);

	protected abstract Object[][] getColumnCount(String frameName, String colName);

	protected abstract Object[][] getColumnCount(String frameName, String colName, boolean top);

	protected abstract Object[][] getDescriptiveStats(String frameName, String colName);

	protected abstract Object[][] getHistogram(String frameName, String column, int numBreaks);

	protected abstract void performSplitColumn(String frameName, String[] columnNames, String separator, String direction, boolean dropColumn, boolean frameReplace);

	protected abstract void performJoinColumns(String frameName, String newColumnName, String separator, String[] columns);

	protected abstract Map<String, Object> flushObjectAsTable(String framename, String[] colNames);
	
	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	//////////////////////// R Methods /////////////////////////

	/**
	 * Shift the dataframe into R with a default name
	 */
	public void synchronizeToR() {
		java.lang.System.setSecurityManager(curManager);
		if(dataframe instanceof TinkerFrame) {
			synchronizeGraphToR();
		} else if (dataframe instanceof H2Frame) {
			synchronizeGridToR();
		}
	}

	private static String getDefaultName() {
		// TODO: need to check variable names
		// make sure default name won't override
		return "df_" + counter++;
	}

	/**
	 * Shift the dataframe into R
	 * 
	 * @param rVarName
	 */
	public void synchronizeToR(String rVarName) {
		java.lang.System.setSecurityManager(curManager);
		if (dataframe instanceof TinkerFrame) {
			synchronizeGraphToR(rVarName);
		} else if (dataframe instanceof H2Frame) {
			synchronizeGridToR(rVarName);
		}
	}

	public void synchronizeFromR() {
		if (dataframe instanceof TinkerFrame) {
			String graphName = (String) retrieveVariable("GRAPH_NAME");
			synchronizeGraphFromR(graphName);
		} else if (dataframe instanceof H2Frame) {
			String frameName = (String) retrieveVariable("GRID_NAME");
			synchronizeGridFromR(frameName, true);
		}
	}

	/**
	 * Install a R package
	 * 
	 * @param packageName
	 */
	protected void installR(String packageName) {
		startR();
		this.logger.info("Starting to install package " + packageName + "... ");
		eval("install.packages('" + packageName + "', repos='http://cran.us.r-project.org');");
		this.logger.info("Succesfully installed package " + packageName);
		System.out.println("Succesfully installed package " + packageName);
	}

	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	////////////////////// H2 R Methods ////////////////////////

	protected void synchronizeGridToR() {
		String defaultName = getDefaultName();
		synchronizeGridToR(defaultName);
	}

	/**
	 * Synchronize the grid to R
	 * 
	 * @param frameName
	 */
	protected void synchronizeGridToR(String rVarName) {
		synchronizeGridToR(rVarName, null);
	}

	/**
	 * Synchronize the grid to R
	 * 
	 * @param frameName
	 * @param cols
	 */
	private void synchronizeGridToR(String frameName, String cols) {
		long start = java.lang.System.currentTimeMillis();
		logger.info("Synchronizing H2Frame to R data.table...");
		H2Frame gridFrame = (H2Frame) dataframe;
		String tableName = gridFrame.getBuilder().getTableName();
		String url = gridFrame.getBuilder().connectFrame();
		url = url.replace("\\", "/");

		// note : do not use * since R will not preserve the column order
		StringBuilder selectors = new StringBuilder();
		String[] colSelectors = gridFrame.getColumnHeaders();
		for (int selectIndex = 0; selectIndex < colSelectors.length; selectIndex++) {
			//TODO: lots of assumptions around a single table
			//TODO: lots of assumptions around a single table
			//TODO: lots of assumptions around a single table
			String colSelector = colSelectors[selectIndex];
			if(colSelector.contains("__")) {
				colSelector = colSelector.split("__")[1];
				selectors.append(colSelector);
				colSelectors[selectIndex] = colSelector;
			} else {
				selectors.append(colSelector);
			}
			if (selectIndex + 1 < colSelectors.length) {
				selectors.append(", ");
			}
		}
		
		startR();

		// Don't sync via RJDBC if OS is Mac because we'll write to CSV and load into data.table to avoid rJava setup
		String OS = java.lang.System.getProperty("os.name").toLowerCase();
		if(OS.contains("mac")) {
			String outputLocation = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER).replace("\\", "/") + java.lang.System.getProperty("file.separator") + "R" 
					+ java.lang.System.getProperty("file.separator") + "Temp" + java.lang.System.getProperty("file.separator") + "output.csv";
			gridFrame.execQuery("CALL CSVWRITE('" + outputLocation + "', 'SELECT * FROM " + gridFrame.getTableName() + "', 'charset=UTF-8 fieldSeparator=, fieldDelimiter=');");
			eval("file <- '" + outputLocation + "';");
			eval(frameName + " <- read.csv(file);");
			File f = new File(outputLocation);
			f.delete();
		} else {
			initiateDriver(url, "sa");
			eval(frameName + " <-as.data.table(unclass(dbGetQuery(conn,'SELECT " + selectors + " FROM " + tableName + "')));");
		}
		eval("setDT(" + frameName + ")");

		// modify the headers to be what they used to be because the query
		// return everything in
		// all upper case which may not be accurate
		String[] currHeaders = getColNames(frameName, false);
		renameColumn(frameName, currHeaders, colSelectors, false);
		storeVariable("GRID_NAME", new NounMetadata(frameName, PixelDataType.CONST_STRING));
		System.out.println("Completed synchronization as " + frameName);
		
		long end = java.lang.System.currentTimeMillis();
		logger.info("Done synchroizing to R data.table...");
		logger.debug("Time to finish synchronizing to R data.table " + (end-start) + "ms");
	}

	/**
	 * Synchronize current H2Frame into a R Data Table Frame
	 * 
	 * @param rVarName
	 */
	protected void synchronizeGridToRDataTable(String rVarName) {
		// defualt will be to replace the existing frame on the insight
		// with the new R Data Table we are about to make
		synchronizeGridToRDataTable(rVarName, true);
	}
	
	protected RDataTable synchronizeGridToRDataTable(String rVarName, boolean replaceDefaultInsightFrame) {
		if(rVarName == null || rVarName.isEmpty()) {
			rVarName = getDefaultName();
		}
		// if there is a current r serve session
		// use that for the frame so we have all the other variables
		RDataTable table = null;
		if (retrieveVariable(R_CONN) != null && retrieveVariable(R_PORT) != null) {
			table = new RDataTable(rVarName, (RConnection) retrieveVariable(R_CONN), (String) retrieveVariable(R_PORT));
		} else {
			// if we dont have a current r session
			// but when we create the table it makes one
			// store those variables so we end up using that
			table = new RDataTable(rVarName);
			if (table.getConnection() != null && table.getPort() != null) {
				storeVariable(R_CONN, new NounMetadata(table.getConnection(), PixelDataType.R_CONNECTION));
				storeVariable(R_PORT, new NounMetadata(table.getPort(), PixelDataType.CONST_STRING));
			}
		}
		table.setUserId(dataframe.getUserId());

		if (dataframe instanceof H2Frame) {
			H2Frame gridFrame = (H2Frame) dataframe;
			String tableName = gridFrame.getBuilder().getTableName();
			String url = gridFrame.getBuilder().connectFrame();
			url = url.replace("\\", "/");
			initiateDriver(url, "sa");
			synchronizeGridToR(rVarName, null);
			
			// now that we have created the frame
			// we need to set the metadata for the frame
			OwlTemporalEngineMeta newMeta = gridFrame.getMetaData().copy();
			newMeta.modifyVertexName(tableName, rVarName);
			table.setMetaData(newMeta);
		
		} else if(dataframe  instanceof RDataTable){
			// ughhh... why are you calling this?
			// i will just change the r var name
			table.executeRScript(rVarName + " <- " + ((RDataTable) dataframe).getTableVarName());
			table.setTableVarName(rVarName);
			table.setMetaData(dataframe.getMetaData());
			// also, dont forget to update the metadata
			table.getMetaData().modifyVertexName(((RDataTable) dataframe).getTableVarName(), rVarName);
		
		} else if(dataframe instanceof NativeFrame) {
			Iterator<IHeadersDataRow> it = dataframe.iterator();
			RImporter importer = new RImporter(table, dataframe.getMetaData().getFlatTableQs(), it);
			importer.insertData();
			
		} else {
			throw new IllegalArgumentException("Frame must be of type H2");
		}


		// now we return the data
		this.nounMetaOutput.add(new NounMetadata(table, PixelDataType.FRAME, PixelOperationType.FRAME));
		if(replaceDefaultInsightFrame) {
			this.insight.setDataMaker(table);
		}
		return table;
	}


	/**
	 * Create a H2Frame from an existing R data table
	 */
	protected void synchronizeGridFromR() {
		String frameName = (String) retrieveVariable("GRID_NAME");
		synchronizeGridFromR(frameName, true);
	}

	/**
	 * Synchronize a R data table into a H2Frame
	 * 
	 * @param frameName
	 * @param overrideExistingTable
	 */
	protected void synchronizeGridFromR(String frameName, boolean overrideExistingTable) {
		// get the necessary information from the r frame
		// to be able to add the data correctly

		// get the names and types
		String[] colNames = getColNames(frameName, false);
		// since R has less restrictions than we do regarding header names
		// we will clean the header names to match what the cleaning would be
		// when we load
		// in a file
		// note: the clean routine will only do something if the metadata has
		// changed
		// otherwise, the headers would already be good to go
		List<String> cleanColNames = new Vector<String>();
		HeadersException headerException = HeadersException.getInstance();
		for (int i = 0; i < colNames.length; i++) {
			String cleanHeader = headerException.recursivelyFixHeaders(colNames[i], cleanColNames);
			cleanColNames.add(cleanHeader);
		}
		colNames = cleanColNames.toArray(new String[]{});
		String[] colTypes = getColTypes(frameName, false);

		// generate the QS
		// set the column names and types
		CsvQueryStruct qs = new CsvQueryStruct();
		qs.setSelectorsAndTypes(colNames, colTypes);
		
		/*
		 * logic to determine where we are adding this data... 1) First, make
		 * sure the existing frame is a grid -> If it is not a grid, we already
		 * know we need to make a new h2frame 2) Second, if it is a grid, check
		 * the meta data and see if it has changed -> if it has changed, we need
		 * to make a new h2frame 3) Regardless of #2 -> user can decide what
		 * they want to create a new frame even if the meta data hasn't changed
		 */

		H2Frame frameToUse = null;
		boolean frameIsH2 = false;
		String schemaName = null;
		String tableName = null;
		boolean determineNewFrameNeeded = false;
		boolean syncExistingRMetadata = false;
		OwlTemporalEngineMeta newMeta = null;
		
		// if we dont even have a h2frame currently, make a new one
		if (!(dataframe instanceof H2Frame)) {
			determineNewFrameNeeded = true;
			if(dataframe instanceof RDataTable && ((RDataTable) dataframe).getTableVarName().equals(frameName)) {
				syncExistingRMetadata = true;
			}
		} else {
			frameIsH2 = true;
			schemaName = ((H2Frame) dataframe).getSchema();
			tableName = ((H2Frame) dataframe).getTableName();

			// if we do have an h2frame, look at headers to figure
			// out if the metadata has changed

			String[] currHeaders = dataframe.getColumnHeaders();

			if (colNames.length != currHeaders.length) {
				determineNewFrameNeeded = true;
			} else {
				for (String currHeader : currHeaders) {
					if (!ArrayUtilityMethods.arrayContainsValueIgnoreCase(colNames, currHeader)) {
						determineNewFrameNeeded = true;
					}
				}
			}
		}

		if (!overrideExistingTable || determineNewFrameNeeded) {
			frameToUse = new H2Frame();
			tableName = frameToUse.getTableName();

			// set the correct schema in the new frame
			// drop the existing table
			if (frameIsH2) {
				frameToUse.setUserId(schemaName);
				((H2Frame) dataframe).dropTable();
			} else {
				// this is set when we set the original dataframe
				// within the reactor
				frameToUse.setUserId(this.insight.getUserId());
			}

			// if we can use the existing metadata, use it
			if(syncExistingRMetadata) {
				newMeta = this.dataframe.getMetaData().copy();
				newMeta.modifyVertexName(frameName, frameToUse.getTableName());
			} 
//			else {
//				// create a prim key one
//				Map<String, Set<String>> edgeHash = TinkerMetaHelper.createPrimKeyEdgeHash(colNames);
//				frameToUse.mergeEdgeHash(edgeHash, dataTypeMapStr);
//			}
		} else if (overrideExistingTable && frameIsH2) {
			frameToUse = ((H2Frame) dataframe);

			// can only enter here when we are overriding the existing H2Frame
			// drop any index if altering the existing frame
			Set<String> columnIndices = frameToUse.getColumnsWithIndexes();
			if (columnIndices != null) {
				for (String colName : columnIndices) {
					frameToUse.removeColumnIndex(colName);
				}
			}

			// drop all existing data
			frameToUse.deleteAllRows();
		}

		// we will make a temp file
		String tempFileLocation = DIHelper.getInstance().getProperty(Constants.INSIGHT_CACHE_DIR) + "\\" + DIHelper.getInstance().getProperty(Constants.CSV_INSIGHT_CACHE_FOLDER);
		tempFileLocation += "\\" + Utility.getRandomString(10) + ".csv";
		tempFileLocation = tempFileLocation.replace("\\", "/");
		eval("fwrite(" + frameName + ", file='" + tempFileLocation + "')");

		// iterate through file and insert values
		qs.setCsvFilePath(tempFileLocation);
		H2Importer importer = new H2Importer(frameToUse, qs);
		if(syncExistingRMetadata) {
			importer.insertData(newMeta);
		} else {
			// importer will create the necessary meta information 
			importer.insertData();
			
		}
		
//		// keep track of in-mem vs on-disk frames
//		int limitSizeInt = RdbmsFrameUtility.getLimitSize();
//		if (dataIterator.numberRowsOverLimit(limitSizeInt)) {
//			frameToUse.convertToOnDiskFrame(null);
//		}
//
//		// now that we know if we are adding to disk vs mem
//		// iterate through and add all the data
//		frameToUse.addRowsViaIterator(dataIterator, dataTypeMap);
//		dataIterator.deleteFile();

		System.out.println("Table Synchronized as " + tableName);
		// override frame references & table name reference
		if(overrideExistingTable) {
			this.nounMetaOutput.add(new NounMetadata(frameToUse, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE));
			this.insight.setDataMaker(frameToUse);
		} else {
			this.nounMetaOutput.add(new NounMetadata(frameToUse, PixelDataType.FRAME));
		}
	}

	/**
	 * Synchronize a R data table into a H2Frame
	 * 
	 * @param frameName
	 * @param overrideExistingTable
	 */
	protected void synchronizeGridFromRDataTable(String frameName) {
		synchronizeGridFromR(frameName, true);
	}

	protected void initiateDriver(String url, String username) {
		String driver = "org.h2.Driver";
		String jarLocation = "";
		if (retrieveVariable("H2DRIVER_PATH") == null) {
			String workingDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER).replace("\\", "/");
			;
			String jar = "h2-1.4.185.jar"; // TODO: create an enum of available
			// drivers and the necessary jar for
			// each
			jarLocation = workingDir + "/RDFGraphLib/" + jar;
		} else {
			jarLocation = (String) retrieveVariable("H2DRIVER_PATH");
		}
		logger.info("Loading driver.. " + jarLocation);
		// line of R script that connects to H2Frame
		String script = "drv <- JDBC('" + driver + "', '" + jarLocation + "', identifier.quote='`');"
				+ "conn <- dbConnect(drv, '" + url + "', '" + username + "', '')"; 
		runR(script);
	}

	/**
	 * Synchronize a CSV File into an R Data Table
	 * 
	 * @param fileName
	 * @param frameName
	 */
	protected void synchronizeCSVToR(String fileName, String frameName) {
		eval(frameName + " <- fread(\"" + fileName + "\")");
		System.out.println("Completed synchronization of CSV " + fileName);
	}

	protected String getColType(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		return getColType(frameName, colName, true);
	}

	protected String getColType(String frameName, String colName) {
		return getColType(frameName, colName, true);
	}

	protected String[] getColTypes(String frameName) {
		return getColTypes(frameName, true);
	}

	protected String[] getColNames(String frameName) {
		return getColNames(frameName, true);
	}

	/**
	 * Get the column count of a given column
	 * 
	 * @param column
	 */
	protected Object[][] getColumnCount(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		return getColumnCount(frameName, colName);
	}

	/**
	 * Get the column count of a given column
	 * 
	 * @param column
	 */
	protected Object[][] getDescriptiveStats(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		return getDescriptiveStats(frameName, colName);
	}

	public void getHistogram(String frameName, String column) {
		getHistogram(frameName, column, 0);
	}

	/**
	 * Add an empty column to later insert new values
	 * 
	 * @param newColName
	 */
	protected void addEmptyColumn(String newColName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		addEmptyColumn(frameName, newColName);
	}

	protected void addEmptyColumn(String frameName, String newColName) {
		String script = frameName + "$" + newColName + " <- \"\" ";
		eval(script);
		System.out.println("Successfully added column = " + newColName);
		if (checkRTableModified(frameName)) {
			OwlTemporalEngineMeta metaData = this.dataframe.getMetaData();
			metaData.addProperty(frameName, frameName + "__" + newColName);
			metaData.setAliasToProperty(frameName + "__" + newColName, newColName);
			metaData.setDataTypeToProperty(frameName + "__" + newColName, "STRING");
			this.dataframe.syncHeaders();
		}
	}

	/**
	 * Add an empty column to later insert new values
	 * 
	 * @param newColName
	 */
	protected void changeColumnType(String colName, String newType) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		changeColumnType(frameName, colName, newType);
	}

	protected void changeColumnType(String frameName, String colName, String newType) {
		changeColumnType(frameName, colName, newType, "%Y/%m/%d");
	}

	protected void changeColumnType(String frameName, String colName, String newType, String dateFormat) {
		String script = null;
		if (newType.equalsIgnoreCase("string")) {
			script = frameName + " <- " + frameName + "[, " + colName + " := as.character(" + colName + ")]";
			eval(script);
		} else if (newType.equalsIgnoreCase("factor")) {
			script = frameName + " <- " + frameName + "[, " + colName + " := as.factor(" + colName + ")]";
			eval(script);
		} else if (newType.equalsIgnoreCase("number")) {
			script = frameName + " <- " + frameName + "[, " + colName + " := as.numeric(" + colName + ")]";
			eval(script);
		} else if (newType.equalsIgnoreCase("date")) {
			// we have a different script to run if it is a str to date
			// conversion
			// or a date to new date format conversion
			String type = getColType(frameName, colName, false);
			String tempTable = Utility.getRandomString(6);
			if (type.equalsIgnoreCase("date")) {
				String formatString = ", format = '" + dateFormat + "'";
				script = tempTable + " <- format(" + frameName + "$" + colName + formatString + ")";
				eval(script);
				script = frameName + "$" + colName + " <- " + "as.Date(" + tempTable + formatString + ")";
				eval(script);
			} else {
				script = tempTable + " <- as.Date(" + frameName + "$" + colName + ", format='" + dateFormat + "')";
				eval(script);
				script = frameName + "$" + colName + " <- " + tempTable;
				eval(script);
			}
			// perform variable cleanup
			eval("rm(" + tempTable + ");");
			eval("gc();");
		}
		System.out.println("Successfully changed data type for column = " + colName);
		if (checkRTableModified(frameName)) {
			this.dataframe.getMetaData().modifyDataTypeToProperty(frameName + "__" + colName, frameName, newType);
		}
	}

	/**
	 * Drop a column within the table
	 * 
	 * @param colName
	 */
	protected void dropRColumn(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		dropRColumn(frameName, colName);
	}

	protected void dropRColumn(String frameName, String colName) {
		if (checkRTableModified(frameName)) {
			// this R method will do the same evaluation
			// but it will also drop it from the metadata
			this.dataframe.removeColumn(colName);
			this.nounMetaOutput.add(new NounMetadata(this.dataframe, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE));
		} else {
			eval(frameName + "[," + colName + ":=NULL]");
		}
		System.out.println("Successfully removed column = " + colName);
	}
	
	protected void dropRColumn(String frameName, Object[] colName) {
		if (checkRTableModified(frameName)) {
			// this R method will do the same evaluation
			// but it will also drop it from the metadata
			for(Object col : colName) {
				this.dataframe.removeColumn(col.toString());
			}
			this.nounMetaOutput.add(new NounMetadata(this.dataframe, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE));
		} else {
			for(Object col : colName) {
				eval(frameName + "[," + col + ":=NULL]");
			}
		}
		System.out.println("Successfully removed column = " + colName);
	}

	/**
	 * Drop rows based on a comparator for a set of values
	 * 
	 * @param colName
	 * @param comparator
	 * @param values
	 */
	protected void dropRowsWhereColumnContainsValue(String colName, String comparator, Object values) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		dropRowsWhereColumnContainsValue(frameName, colName, comparator, values);
	}

	/**
	 * Filter out rows based on values in a given column
	 * 
	 * @param frameName
	 * @param colName
	 * @param comparator
	 * @param values
	 */
	protected void dropRowsWhereColumnContainsValue(String frameName, String colName, String comparator, Object values) {
		// to account for misunderstandings between = and == for normal users
		if (comparator.trim().equals("=")) {
			comparator = " == ";
		}
		String frameExpression = frameName + "$" + colName;
		// determine the correct comparison to drop values from the frame
		// .... this is a bunch of casting...
		// also note that the string NULL is special to remove values that are
		// undefined within the frame
		StringBuilder script = new StringBuilder(frameName).append(" <- ").append(frameName).append("[!( ");
		String dataType = getColType(frameName, colName, false);

		// accommodate for factors cause they are annoying
		if (dataType.equals("factor")) {
			changeColumnType(frameName, colName, "STRING");
			dataType = "character";
		}

		if (values instanceof Object[]) {
			Object[] arr = (Object[]) values;
			Object val = arr[0];
			if (dataType.equalsIgnoreCase("character")) {
				if (val.toString().equalsIgnoreCase("NULL") || val.toString().equalsIgnoreCase("NA")) {
					script.append("is.na(").append(frameExpression).append(") ");
				} else {
					if(comparator.equals("like")) {
						script.append("like(").append(frameExpression).append(",").append("\"").append(val).append("\")");
					} else {
						script.append(frameExpression).append(comparator).append("\"").append(val).append("\"");
					}
				}
			} else {
				script.append(comparator).append(val);
			}
			for (int i = 1; i < arr.length; i++) {
				val = arr[i];
				if (dataType.equalsIgnoreCase("character")) {
					if (val.toString().equalsIgnoreCase("NULL") || val.toString().equalsIgnoreCase("NA")) {
						script.append(" | is.na(").append(frameExpression).append(") ");
					} else {
						if(comparator.equals("like")) {
							script.append(" | ").append("like(").append(frameExpression).append(",").append("\"").append(val).append("\")");
						} else {
							script.append(" | ").append(frameExpression).append(comparator).append("\"").append(val).append("\"");						}
					}
				} else {
					script.append(" | ").append(frameExpression).append(comparator).append(val);
				}
			}
		} else if (values instanceof Double[]) {
			Double[] arr = (Double[]) values;
			Double val = arr[0];
			script.append(frameExpression).append(comparator).append(val);
			for (int i = 1; i < arr.length; i++) {
				val = arr[i];
				script.append(" | ").append(frameExpression).append(comparator).append(val);
			}
		} else if (values instanceof Integer[]) {
			Integer[] arr = (Integer[]) values;
			Integer val = arr[0];
			script.append(frameExpression).append(comparator).append(val);
			for (int i = 1; i < arr.length; i++) {
				val = arr[i];
				script.append(" | ").append(frameExpression).append(comparator).append(val);
			}
		} else if (values instanceof double[]) {
			double[] arr = (double[]) values;
			double val = arr[0];
			script.append(frameExpression).append(comparator).append(val);
			for (int i = 1; i < arr.length; i++) {
				val = arr[i];
				script.append(" | ").append(frameExpression).append(comparator).append(val);
			}
		} else if (values instanceof int[]) {
			int[] arr = (int[]) values;
			int val = arr[0];
			script.append(frameExpression).append(comparator).append(val);
			for (int i = 1; i < arr.length; i++) {
				val = arr[i];
				script.append(" | ").append(frameExpression).append(comparator).append(val);
			}
		} else {
			if (dataType.equalsIgnoreCase("character")) {
				if (values.toString().equalsIgnoreCase("NULL") || values.toString().equalsIgnoreCase("NA")) {
					script.append("is.na(").append(frameExpression).append(") ");
				} else {
					if(comparator.equals("like")) {
						script.append("like(").append(frameExpression).append(",").append("\"").append(values).append("\")");
					} else {
						script.append(frameExpression).append(comparator).append("\"").append(values).append("\"");
					}
				}
			} else {
				script.append(frameExpression).append(comparator).append(values);
			}
		}
		script.append("),]");
		eval(script.toString());
		System.out.println("Script ran = " + script.toString() + "\nSuccessfully removed rows");
		checkRTableModified(frameName);
	}

	protected void dropRowsWhereColumnContainsValue(String colName, String comparator, int value) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		dropRowsWhereColumnContainsValue(frameName, colName, comparator, value);
	}

	protected void dropRowsWhereColumnContainsValue(String frameName, String colName, String comparator, int value) {
		// to account for misunderstandings between = and == for normal users
		if (comparator.trim().equals("=")) {
			comparator = " == ";
		}
		String frameExpression = frameName + "$" + colName;
		StringBuilder script = new StringBuilder(frameName).append("<-").append(frameName).append("[!(")
				.append(frameExpression).append(comparator).append(value).append("),]");
		eval(script.toString());
		System.out.println("Script ran = " + script.toString() + "\nSuccessfully removed rows");
		checkRTableModified(frameName);
	}

	protected void dropRowsWhereColumnContainsValue(String colName, String comparator, double value) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		dropRowsWhereColumnContainsValue(frameName, colName, comparator, value);
	}

	protected void dropRowsWhereColumnContainsValue(String frameName, String colName, String comparator, double value) {
		// to account for misunderstandings between = and == for normal users
		if (comparator.trim().equals("=")) {
			comparator = " == ";
		}
		String frameExpression = frameName + "$" + colName;
		StringBuilder script = new StringBuilder(frameName).append("<-").append(frameName).append("[!(")
				.append(frameExpression).append(comparator).append(value).append("),]");
		eval(script.toString());
		System.out.println("Script ran = " + script.toString() + "\nSuccessfully removed rows");
		checkRTableModified(frameName);
	}

	/**
	 * Create a new column by counting the presence of a string within another
	 * column
	 * 
	 * @param newColName
	 * @param countColName
	 * @param strToCount
	 */
	protected void insertStrCountColumn(String newColName, String countColName, String strToCount) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		insertStrCountColumn(frameName, newColName, countColName, strToCount);
	}

	protected void insertStrCountColumn(String newColName, String countColName, int valToCount) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		insertStrCountColumn(frameName, newColName, countColName, valToCount + "");
	}

	protected void insertStrCountColumn(String newColName, String countColName, double valToCount) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		insertStrCountColumn(frameName, newColName, countColName, valToCount + "");
	}

	protected void insertStrCountColumn(String frameName, String newColName, String countColName, int valToCount) {
		insertStrCountColumn(frameName, newColName, countColName, valToCount + "");
	}

	protected void insertStrCountColumn(String frameName, String newColName, String countColName, double valToCount) {
		insertStrCountColumn(frameName, newColName, countColName, valToCount + "");
	}

	protected void insertStrCountColumn(String frameName, String newColName, String countColName, Object strToCount) {
		// dt$new <- str_count(dt$oldCol, "strToFind");
		String script = frameName + "$" + newColName + " <- str_count(" + frameName + "$" + countColName + ", \""
				+ strToCount + "\")";
		eval(script);
		System.out.println("Added new column = " + newColName);
		if (checkRTableModified(frameName)) {
			OwlTemporalEngineMeta metaData = this.dataframe.getMetaData();
			metaData.addProperty(frameName, frameName + "__" + newColName);
			metaData.setAliasToProperty(frameName + "__" + newColName, newColName);
			metaData.setDataTypeToProperty(frameName + "__" + newColName, "NUMBER");
			this.dataframe.syncHeaders();
		}
	}

	/**
	 * Turn a string to lower case
	 * 
	 * @param colName
	 */
	protected void toLowerCase(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		toLowerCase(frameName, colName);
	}

	protected void toLowerCase(String frameName, String colName) {
		String script = frameName + "$" + colName + " <- tolower(" + frameName + "$" + colName + ")";
		eval(script);
		checkRTableModified(frameName);
	}
	
	protected void toLowerCase(String frameName, Object[] colNames) {
		for(Object colName : colNames) {
			String script = frameName + "$" + colName + " <- tolower(" + frameName + "$" + colName + ")";
			eval(script);
		}
		checkRTableModified(frameName);
	}

	/**
	 * Turn a string to lower case
	 * 
	 * @param colName
	 */
	protected void toUpperCase(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		toUpperCase(frameName, colName);
	}

	protected void toUpperCase(String frameName, String colName) {
		String script = frameName + "$" + colName + " <- toupper(" + frameName + "$" + colName + ")";
		eval(script);
		checkRTableModified(frameName);
	}
	
	protected void toUpperCase(String frameName, Object[] colNames) {
		for(Object colName : colNames) {
			String script = frameName + "$" + colName + " <- toupper(" + frameName + "$" + colName + ")";
			eval(script);
		}
		checkRTableModified(frameName);
	}

	/**
	 * Turn a string to lower case
	 * 
	 * @param colName
	 */
	protected void trim(String colName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		trim(frameName, colName);
	}

	protected void trim(String frameName, String colName) {
		String script = frameName + "$" + colName + " <- str_trim(" + frameName + "$" + colName + ")";
		eval(script);
		checkRTableModified(frameName);
	}
	
	protected void trim(String frameName, Object[] colNames) {
		for(Object colName : colNames) {
			String script = frameName + "$" + colName + " <- str_trim(" + frameName + "$" + colName + ")";
			eval(script);
		}
		checkRTableModified(frameName);
	}

	/**
	 * Replace a column value with a new value
	 * 
	 * @param columnName
	 * @param curValue
	 * @param newValue
	 */
	protected void replaceColumnValue(String columnName, String curValue, String newValue) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		replaceColumnValue(frameName, columnName, curValue, newValue);
	}

	protected void replaceColumnValue(String frameName, String columnName, String curValue, String newValue) {
		// replace the column value for a particular column
		// dt[PY == "hello", PY := "D"] replaces a column conditionally based on
		// the value
		// need to get the type of this
		try {
			String condition = " ,";
			String dataType = getColType(columnName);
			String quote = "";
			if (dataType.contains("character")) {
				quote = "\"";
			} else if (dataType.equals("factor")) {
				changeColumnType(frameName, columnName, "STRING");
				quote = "\"";
			}
			if (curValue.equalsIgnoreCase("null") || curValue.equalsIgnoreCase("NA")) {
				condition = "is.na(" + columnName + ") , ";
			} else {
				condition = columnName + " == " + quote + curValue + quote + ", ";
			}
			String script = frameName + "[" + condition + columnName + " := " + quote + newValue + quote + "]";
			eval(script);
			System.out.println("Done replacing value = \"" + curValue + "\" with new value = \"" + newValue + "\"");
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkRTableModified(frameName);
	}

	/**
	 * 
	 * @param frameName
	 * @param columnName
	 * @param curValue
	 * @param newValue
	 */
	protected void updateRowValuesWhereColumnContainsValue(String updateColName, Object updateColValue,
			String conditionalColName, String comparator, Object conditionalColValue) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		updateRowValuesWhereColumnContainsValue(frameName, updateColName, updateColValue, conditionalColName,
				comparator, conditionalColValue);
	}

	protected void updateRowValuesWhereColumnContainsValue(String updateColName, Object updateColValue,
			String conditionalColName, String comparator, double conditionalColValue) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		updateRowValuesWhereColumnContainsValue(frameName, updateColValue, conditionalColName, comparator,
				conditionalColValue + "");
	}

	protected void updateRowValuesWhereColumnContainsValue(String updateColName, Object updateColValue,
			String conditionalColName, String comparator, int conditionalColValue) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		updateRowValuesWhereColumnContainsValue(frameName, updateColValue, conditionalColName, comparator,
				conditionalColValue + "");
	}

	protected void updateRowValuesWhereColumnContainsValue(String frameName, String updateColName,
			Object updateColValue, String conditionalColName, String comparator, double conditionalColValue) {
		updateRowValuesWhereColumnContainsValue(frameName, updateColName, updateColValue, conditionalColName,
				comparator, conditionalColValue + "");
	}

	protected void updateRowValuesWhereColumnContainsValue(String frameName, String updateColName,
			Object updateColValue, String conditionalColName, String comparator, int conditionalColValue) {
		updateRowValuesWhereColumnContainsValue(frameName, updateColName, updateColValue, conditionalColName,
				comparator, conditionalColValue + "");
	}

	protected void updateRowValuesWhereColumnContainsValue(String frameName, String updateColName,
			Object updateColValue, String conditionalColName, String comparator, Object conditionalColValue) {
		// update values based on other columns
		// dt$updateColName[dt$conditionalColName == "conditionalColValue] <-
		// updateColValue
		// need to get the types of this
		try {
			if (comparator.trim().equals("=")) {
				comparator = "==";
			}
			comparator = " " + comparator + " ";

			String updateDataType = getColType(updateColName);
			String updateQuote = "";
			if (updateDataType.contains("character") || updateDataType.contains("factor")) {
				updateQuote = "\"";
			}

			String conditionColDataType = getColType(conditionalColName);
			String conditionColQuote = "";
			if (conditionColDataType.contains("character")) {
				conditionColQuote = "\"";
			}

			String script = frameName + "$" + updateColName + "[" + frameName + "$" + conditionalColName + comparator
					+ conditionColQuote + conditionalColValue + conditionColQuote + "] <- " + updateQuote
					+ updateColValue + updateQuote;
			eval(script);
			System.out.println("Done updating column " + updateColName + " where " + conditionalColName + comparator
					+ conditionalColValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkRTableModified(frameName);
	}

	/**
	 * Regex replace a column value with a new value
	 * 
	 * @param columnName
	 * @param curValue
	 * @param newValue
	 */
	protected void regexReplaceColumnValue(String columnName, String regex, String newValue) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		regexReplaceColumnValue(frameName, columnName, regex, newValue);
	}

	protected void regexReplaceColumnValue(String frameName, String columnName, String regex, String newValue) {
		// replace the column value for a particular column
		// dt$Title = gsub("regex", "newValue", dt$Title)
		// need to get the type of this
		try {
			String colScript = frameName + "$" + columnName;
			String script = colScript + " = ";
			String dataType = getColType(columnName);
			String quote = "";
			if (dataType.contains("character") || dataType.contains("factor")) {
				quote = "\"";
			}
			script += "gsub(" + quote + regex + quote + "," + quote + newValue + quote + ", " + colScript + ")";
			eval(script);
			System.out.println(
					"Done replacing value with regex = \"" + regex + "\" with new value = \"" + newValue + "\"");
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkRTableModified(frameName);
	}

	protected void splitColumn(String[] columnNames, String separator) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		splitColumn(frameName, columnNames, separator, "wide", false, true);
	}

	protected void splitColumn(String frameName, String[] columnNames, String separator) {
		splitColumn(frameName, columnNames, separator, "wide", false, true);
	}

	protected void splitColumn(String frameName, String[] columnNames, String separator, String direction) {
		splitColumn(frameName, columnNames, separator, direction, false, true);
	}

	protected void splitColumn(String frameName, String[] columnNames, String separator, String direction, boolean dropColumn, boolean frameReplace) {
		performSplitColumn(frameName, columnNames, separator, direction, false, true);
		if (checkRTableModified(frameName)) {
			recreateMetadata(frameName);
		}
	}

	protected void joinColumns(String newColumnName, String separator, String[] columns) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		joinColumns(frameName, newColumnName, separator, columns);
	}

	protected void joinColumns(String frameName, String newColumnName, String separator, String[] columns) {
		performJoinColumns(frameName, newColumnName, separator, columns);
		if (checkRTableModified(frameName)) {
			recreateMetadata(frameName);
		}
	}

	protected void transpose() {
		String frameName = (String) retrieveVariable("GRID_NAME");
		transpose(frameName);
	}

	protected void transpose(String frameName) {
		String script = frameName + " <- " + frameName + "[, data.table(t(.SD), keep.rownames=TRUE)]";
		System.out.println("Running script : " + script);
		eval(script);
		System.out.println("Successfully transposed data table into existing frame");
		if (checkRTableModified(frameName)) {
			recreateMetadata(frameName);
		}
	}

	protected void transpose(String frameName, String transposeFrameName) {
		String script = transposeFrameName + " <- " + frameName + "[, data.table(t(.SD), keep.rownames=TRUE)]";
		System.out.println("Running script : " + script);
		eval(script);
		System.out.println("Successfully transposed data table into new frame " + transposeFrameName);
		if (checkRTableModified(frameName)) {
			recreateMetadata(frameName);
		}
	}

	protected void unpivot() {
		String frameName = (String) retrieveVariable("GRID_NAME");
		unpivot(frameName, new String[]{}, true);
	}

	protected void unpivot(String frameName, String[] columnsToUnPivot, boolean replace) {
		// makes the columns and converts them into rows
		// melt(dat, id.vars = "FactorB", measure.vars = c("Group1", "Group2"))
		startR();
		String concatString = "";
		String tempName = Utility.getRandomString(8);

		int numColsToUnPivot = columnsToUnPivot.length;
		if(numColsToUnPivot > 0) {
			concatString = ", measure.vars = c(";
			for (int colIndex = 0; colIndex < numColsToUnPivot; colIndex++) {
				concatString = concatString + "\"" + columnsToUnPivot[colIndex] + "\"";
				if (colIndex + 1 < numColsToUnPivot)
					concatString = concatString + ", ";
			}
			concatString = concatString + ")";
		}
		
		String script = tempName + "<- melt(" + frameName + concatString + ");";
		// run the first script to unpivot into the temp frame
		eval(script);
		// if we are to replace the existing frame
		if (replace) {
			script = frameName + " <- " + tempName;
			eval(script);
			if (checkRTableModified(frameName)) {
				recreateMetadata(frameName);
			}
		}
		System.out.println("Done unpivoting...");
	}

	protected void pivot(String columnToPivot, String valueToPivot, String[] columnsToKeep) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		pivot(frameName, true, columnToPivot, valueToPivot, columnsToKeep, null);
	}

	protected void pivot(String frameName, boolean replace, String columnToPivot, String valueToPivot, String[] columnsToKeep) {
		pivot(frameName, true, columnToPivot, valueToPivot, columnsToKeep, null);
	}

	protected void pivot(String frameName, boolean replace, String columnToPivot, String valueToPivot, String[] columnsToKeep, String aggregateFunction) {
		// makes the columns and converts them into rows
		// dcast(molten, formula = subject~ variable)
		// I need columns to keep and columns to pivot
		startR();
		String newFrame = Utility.getRandomString(8);

		String keepString = "";
		int numColsToKeep = columnsToKeep.length;
		if (numColsToKeep > 0) {
			// with the portion of code to ignore if the user passes in the 
			// col to pivot or value to pivot in the selected columns
			// we need to account for this so we dont end the keepString with " + "
			keepString = ", formula = ";
			for (int colIndex = 0; colIndex < numColsToKeep; colIndex++) {
				String newKeepString = columnsToKeep[colIndex];
				if(newKeepString.equals(columnToPivot) || newKeepString.equals(valueToPivot)) {
					continue;
				}
				keepString = keepString + newKeepString;
				if (colIndex + 1 < numColsToKeep) {
					keepString = keepString + " + ";
				}
			}

			// with the portion of code to ignore if the user passes in the 
			// col to pivot or value to pivot in the selected columns
			// we need to account for this so we dont end the keepString with " + "
			if(keepString.endsWith(" + ")) {
				keepString = keepString.substring(0, keepString.length() - 3);
			}
			keepString = keepString + " ~ " + columnToPivot + ", value.var=\"" + valueToPivot + "\"";
		}

		String aggregateString = "";
		if (aggregateFunction != null && aggregateFunction.length() > 0) {
			aggregateString = ", fun.aggregate = " + aggregateFunction + " , na.rm = TRUE";
		}
		String script = newFrame + " <- dcast(" + frameName + keepString + aggregateString + ");";
		eval(script);
		script = newFrame + " <- as.data.table(" + newFrame + ");";
		eval(script);
		if (replace) {
			script = frameName + " <- " + newFrame;
			eval(script);
			if (checkRTableModified(frameName)) {
				recreateMetadata(frameName);
			}
		}
		System.out.println("Done pivoting...");
	}

	protected void recreateMetadata(String frameName) {
		// recreate a new frame and set the frame name
		String[] colNames = getColNames(frameName, false);
		String[] colTypes = getColTypes(frameName, false);

		RDataTable newTable = null;
		if (retrieveVariable(R_CONN) != null && retrieveVariable(R_PORT) != null) {
			newTable = new RDataTable(frameName, (RConnection) retrieveVariable(R_CONN), (String) retrieveVariable(R_PORT));
		} else {
			newTable = new RDataTable(frameName);
		}
		ImportUtility.parseColumnsAndTypesToFlatTable(newTable, colNames, colTypes, frameName);
		this.nounMetaOutput.add(new NounMetadata(newTable, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE));
		this.insight.setDataMaker(newTable);
	}

	protected void renameColumn(String curColName, String newColName) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		renameColumn(frameName, curColName, newColName);
	}

	protected void renameColumn(String frameName, String curColName, String newColName) {
		String validNewHeader = getCleanNewHeader(frameName, newColName);
		String script = "names(" + frameName + ")[names(" + frameName + ") == \"" + curColName + "\"] = \"" + validNewHeader + "\"";
		System.out.println("Running script : " + script);
		eval(script);
		System.out.println("Successfully modified name = " + curColName + " to now be " + validNewHeader);
		if (checkRTableModified(frameName)) {
			// FE passes the column name
			// but meta will still be table __ column
			this.dataframe.getMetaData().modifyPropertyName(frameName + "__" + curColName, frameName, frameName + "__" + validNewHeader);
			this.dataframe.syncHeaders();
		}
	}
	
	protected void renameColumn(String[] oldNames, String[] newColNames) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		renameColumn(frameName, oldNames, newColNames);
	}

	protected void renameColumn(String frameName, String[] oldNames, String[] newColNames) {
		renameColumn(frameName, oldNames, newColNames, true);
	}

	protected void renameColumn(String frameName, String[] oldNames, String[] newNames, boolean print) {
		int size = oldNames.length;
		if (size != newNames.length) {
			throw new IllegalArgumentException("Names arrays do not match in length");
		}
		StringBuilder oldC = new StringBuilder("c(");
		int i = 0;
		oldC.append("'").append(oldNames[i]).append("'");
		i++;
		for (; i < size; i++) {
			oldC.append(", '").append(oldNames[i]).append("'");
		}
		oldC.append(")");

		StringBuilder newC = new StringBuilder("c(");
		i = 0;
		newC.append("'").append(newNames[i]).append("'");
		i++;
		for (; i < size; i++) {
			newC.append(", '").append(newNames[i]).append("'");
		}
		newC.append(")");

		String script = "setnames(" + frameName + ", old = " + oldC + ", new = " + newC + ")";
		eval(script);

		if (print) {
			System.out.println("Running script : " + script);
			System.out.println("Successfully modified old names = " + Arrays.toString(oldNames) + " to new names " + Arrays.toString(newNames));
		}
		if (checkRTableModified(frameName)) {
			// FE passes the column name
			// but meta will still be table __ column
			for (i = 0; i < size; i++) {
				this.dataframe.getMetaData().modifyPropertyName(frameName + "__" + oldNames[i], frameName, frameName + "__" + newNames[i]);
			}
			this.dataframe.syncHeaders();
		}
	}

	private String getCleanNewHeader(String frameName, String newColName) {
		// make the new column name valid
		HeadersException headerChecker = HeadersException.getInstance();
		String[] currentColumnNames = getColNames(frameName);
		String validNewHeader = headerChecker.recursivelyFixHeaders(newColName, currentColumnNames);
		return validNewHeader;
	}

	/**
	 * Modify the specific cell value in the data frame
	 * 
	 * @param colName
	 * @param rowNum
	 * @param newVal
	 */
	protected void modifyCellValues(String colName, int rowNum, Object newVal) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		modifyCellValues(frameName, colName, rowNum, newVal);
	}

	protected void modifyCellValues(String colName, int rowNum, int newVal) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		modifyCellValues(frameName, colName, rowNum, newVal);
	}

	protected void modifyCellValues(String colName, int rowNum, double newVal) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		modifyCellValues(frameName, colName, rowNum, newVal);
	}

	protected void modifyCellValues(String frameName, String colName, int rowNum, Object newVal) {
		String type = getColType(frameName, colName, false);
		if (type.contains("character")) {
			newVal = "\"" + newVal + "\"";
		}
		String script = frameName + "[" + rowNum + "]$" + colName + " <- " + newVal;
		System.out.println("Running script " + script);
		eval(script);
		checkRTableModified(frameName);
	}

	protected void modifyCellValues(String frameName, String colName, int rowNum, int newVal) {
		String type = getColType(frameName, colName, false);
		String value = newVal + "";
		if (type.contains("character")) {
			value = "\"" + newVal + "\"";
		}
		String script = frameName + "[" + rowNum + "]$" + colName + " <- " + value;
		System.out.println("Running script " + script);
		eval(script);
		checkRTableModified(frameName);
	}

	protected void modifyCellValues(String frameName, String colName, int rowNum, double newVal) {
		String type = getColType(frameName, colName, false);
		String value = newVal + "";
		if (type.contains("character")) {
			value = "\"" + newVal + "\"";
		}
		String script = frameName + "[" + rowNum + "]$" + colName + " <- " + value;
		System.out.println("Running script " + script);
		eval(script);
		checkRTableModified(frameName);
	}

	/**
	 * If we order the data, we need to maintain that structure within the
	 * entire grid If we are to actually be able to replace values based on
	 * index
	 * 
	 * @param colName
	 * @param orderDirection
	 */
	protected void sortData(String colName, String orderDirection) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		sortData(frameName, colName, orderDirection);
	}

	protected void sortData(String frameName, String colName, String orderDirection) {
		String script = null;
		if (orderDirection == null || orderDirection.equalsIgnoreCase("asc")) {
			script = frameName + " <- " + frameName + "[order(rank(" + colName + "))]";
		} else if (orderDirection.equalsIgnoreCase("desc")) {
			script = frameName + " <- " + frameName + "[order(-rank(" + colName + "))]";
		}
		System.out.println("Running script " + script);
		eval(script);
		checkRTableModified(frameName);
	}
	
	protected void removeDuplicateRows(String frameName) {
		String script = frameName + " <- unique(" + frameName + ")";
		System.out.println("Running script " + script);
		eval(script);
		checkRTableModified(frameName);
	}

	/**
	 * Insert data at a given index into the frame
	 * 
	 * @param index
	 * @param values
	 */
	protected void insertDataAtIndex(int index, Object[] values) {
		String frameName = (String) retrieveVariable("GRID_NAME");
		insertDataAtIndex(frameName, index, values);
	}

	protected void insertDataAtIndex(String frameName, int index, Object[] values) {
		// we create a string with the correct types of the values array
		// and then we use that to do the rbindlist
		// if we use a conventional vector with c
		// it will require all the same types

		String[] names = getColNames(frameName, false);
		String[] types = getColTypes(frameName, false);

		String listName = Utility.getRandomString(6);
		StringBuilder listScript = new StringBuilder(listName).append(" <- list(");
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				listScript.append(", ");
			}
			listScript.append(names[i]).append("=");
			if (types[i].equalsIgnoreCase("character")) {
				listScript.append("\"").append(values[i]).append("\"");
			} else {
				listScript.append(values[i]);
			}
		}
		listScript.append(")");
		eval(listScript.toString());

		String script = null;
		int totalRows = getNumRows(frameName);
		if (index == 1) {
			script = frameName + " <- rbindlist(list( " + listName + ", " + frameName + " ))";
		} else if (index == (totalRows + 1)) {
			script = frameName + " <- rbindlist(list( " + frameName + ", " + listName + " ))";
		} else {
			// ugh... somewhere in the middle
			script = frameName + " <- rbindlist(list(" + frameName + "[1:" + (index - 1) + ",] , " + listName + " , "
					+ frameName + "[" + index + ":" + totalRows + ",] ))";
		}
		eval(script);
		System.out.println("Running script :\n" + listScript + "\n" + script);

		checkRTableModified(frameName);
	}

	protected boolean checkRTableModified(String frameName) {
		if (this.dataframe instanceof RDataTable) {
			String tableVarName = ((RDataTable) this.dataframe).getTableVarName();
			if (frameName.equals(tableVarName)) {
				this.dataframe.updateDataId();
				this.nounMetaOutput.add(new NounMetadata(this.dataframe, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE, PixelOperationType.FRAME_HEADERS_CHANGE));
				return true;
			}
		}
		return false;
	}

	protected Map<String, Object> getBarChartInfo(String label, String value, Object[][] dataValues) {
		// create the weird object the FE needs to paint a bar chart
		ConstantDataTask task = new ConstantDataTask();
		task.setId("TEMP_ID");
		Map<String, Object> returnData = new Hashtable<String, Object>();
		returnData.put("values", dataValues);
		returnData.put("headers", new String[]{label, value});
		task.setOutputObject(returnData);
		
		List<Map<String, Object>> vizHeaders = new Vector<Map<String, Object>>();
		Map<String, Object> labelMap = new Hashtable<String, Object>();
		labelMap.put("header", label);
		labelMap.put("derived", true);
		Map<String, Object> frequencyMap = new Hashtable<String, Object>();
		frequencyMap.put("header", value);
		frequencyMap.put("derived", true);
		
		vizHeaders.add(labelMap);
		vizHeaders.add(frequencyMap);

		task.setHeaderInfo(vizHeaders);
		
		return task.collect(0, true);
	}

	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	//////////////////// Tinker R Methods //////////////////////

	protected void synchronizeGraphToR() {
		String defaultName = getDefaultName();
		synchronizeGraphToR(defaultName);
	}

	protected void synchronizeGraphToR(String rVarName) {
		String baseFolder = getBaseFolder();
		String randomDir = Utility.getRandomString(22);
		String wd = baseFolder + "/" + randomDir;
		synchronizeGraphToR(rVarName, wd);
	}

	private void synchronizeGraphToR(String graphName, String wd) {
		java.io.File file = new File(wd);
		String curWd = null;
		try {
			logger.info("Trying to start R.. ");
			startR();
			logger.info("Successfully started R");

			// get the current directory
			// we need to switch out of this to write the graph file
			// but want to go back to this original one
			curWd = getWd();

			// create this directory
			file.mkdir();
			String fileName = writeGraph(wd);

			wd = wd.replace("\\", "/");

			// set the working directory
			eval("setwd(\"" + wd + "\")");
			// load the library
			Object ret = eval("library(\"igraph\");");
			if (ret == null) {
				ICache.deleteFolder(wd);
				throw new ClassNotFoundException("Package igraph could not be found!");
			}
			String loadGraphScript = graphName + "<- read_graph(\"" + fileName + "\", \"graphml\");";
			java.lang.System.out.println(" Load !! " + loadGraphScript);
			// load the graph
			eval(loadGraphScript);

			System.out.println("Successfully synchronized, your graph is now available as " + graphName);
			// store the graph name for future use
			storeVariable("GRAPH_NAME", new NounMetadata(graphName, PixelDataType.CONST_STRING));

			// store the directories used for the iGraph
			List<String> graphLocs = new Vector<String>();
			if (retrieveVariable(R_GRAQH_FOLDERS) != null) {
				graphLocs = (List<String>) retrieveVariable(R_GRAQH_FOLDERS);
			}
			graphLocs.add(wd);
			storeVariable(R_GRAQH_FOLDERS, new NounMetadata(graphLocs, PixelDataType.CONST_STRING));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(
					"ERROR ::: Could not convert TinkerFrame into iGraph.\nPlease make sure iGraph package is installed.");
		} finally {
			// reset back to the original wd
			if (curWd != null) {
				eval("setwd(\"" + curWd + "\")");
			}
		}
		java.lang.System.setSecurityManager(reactorManager);
	}

	/**
	 * Synchronize graph from iGraph
	 * 
	 * @param graphName
	 */
	private void synchronizeGraphFromR(String graphName) {
		System.out.println("ERROR ::: Have not implemented synchronizeGraphFromR yet...");

		// get the attributes
		// and then synchronize all the different properties
		// vertex_attr_names
		// String names = "";
		// RConnection con = (RConnection)startR();
		//
		// // get all the attributes first
		// try {
		// String [] strings = con.eval("vertex_attr_names(" + graphName +
		// ")").asStrings();
		// // the question is do I get everything here and set tinker
		// // or for each get it and so I dont look up tinker many times ?!
		//
		// // now I need to get each of this string and then synchronize
		// } catch (RserveException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (REXPMismatchException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	/**
	 * Remove all nodes of a specific type and with a specific value
	 * 
	 * @param type
	 * @param data
	 */
	protected void removeNode(String type, String data) {
		java.lang.System.setSecurityManager(curManager);
		if (dataframe instanceof TinkerFrame) {
			List<Object> removeList = new Vector<Object>();
			removeList.add(data);
			((TinkerFrame) dataframe).remove(type, removeList);
			String output = "Removed nodes for  " + data + " with values " + removeList;
			System.out.println(output);
			dataframe.updateDataId();
			removeNodeFromR(type, removeList);
		}
		java.lang.System.setSecurityManager(reactorManager);
	}

	/**
	 * Delete nodes from R iGraph
	 * 
	 * @param type
	 * @param nodeList
	 */
	protected void removeNodeFromR(String type, List<Object> nodeList) {
		String graphName = (String) retrieveVariable("GRAPH_NAME");
		if (graphName == null) {
			// we will not have a graph name if the graph has not been
			// synchronized to R
			return;
		}
		for (int nodeIndex = 0; nodeIndex < nodeList.size(); nodeIndex++) {
			String name = type + ":" + nodeList.get(nodeIndex);
			try {
				java.lang.System.out.println("Deleting node = " + name);
				// eval is abstract and is determined by the specific R
				// implementation
				eval(graphName + " <- delete_vertices(" + graphName + ", V(" + graphName + ")[vertex_attr(" + graphName
						+ ", \"" + TinkerFrame.TINKER_ID + "\") == \"" + name + "\"])");
			} catch (Exception ex) {
				java.lang.System.out.println("ERROR ::: Could not delete node = " + name);
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Perform clusters routine on iGraph
	 */
	protected void clusterInfo() {
		String clusters = "clusters";
		clusterInfo(clusters);
	}

	/**
	 * Perform clusters routine on iGraph
	 */
	protected void clusterInfo(String clusterRoutine) {
		String graphName = (String) retrieveVariable("GRAPH_NAME");
		if (graphName == null) {
			System.out.println("ERROR ::: No graph has been synchronized to R");
			return;
		}
		startR();
		try {
			// set the clusters
			storeVariable("CLUSTER_NAME", new NounMetadata("clus", PixelDataType.CONST_STRING));
			eval("clus <- " + clusterRoutine + "(" + graphName + ")");
			System.out.println("\n No. Of Components :");
			runR("clus$no");
			System.out.println("\n Component Sizes :");
			runR("clus$csize");
			colorClusters();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Perform cluster_walktrap routine on iGraph
	 */
	protected void walkInfo() {
		String graphName = (String) retrieveVariable("GRAPH_NAME");

		Rengine retEngine = (Rengine) startR();
		String clusters = "Component Information  \n";
		try {
			// set the clusters
			storeVariable("CLUSTER_NAME",  new NounMetadata("clus", PixelDataType.CONST_STRING));
			retEngine.eval("clus <- cluster_walktrap(" + graphName + ", membership=TRUE)");
			clusters = clusters + "Completed Walktrap";
			System.out.println(clusters);
			colorClusters();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Color tinker nodes based on iGrpah values
	 */
	protected void colorClusters() {
		String clusterName = (String) retrieveVariable("CLUSTER_NAME");
		colorClusters(clusterName);
	}

	/**
	 * Serialize the TinkerGraph in GraphML format
	 * 
	 * @param directory
	 * @return
	 */
	public String writeGraph(String directory) {
		String absoluteFileName = null;
		if (dataframe instanceof TinkerFrame) {
			final Graph graph = ((TinkerFrame) dataframe).g;
			absoluteFileName = "output" + java.lang.System.currentTimeMillis() + ".xml";
			String fileName = directory + "/" + absoluteFileName;
			OutputStream os = null;
			try {
				os = new FileOutputStream(fileName);
				graph.io(IoCore.graphml()).writer().normalize(true).create().writeGraph(os, graph);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				try {
					if (os != null) {
						os.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return absoluteFileName;
	}

	/**
	 * Run a layout in iGraph and store back into tinker objects Possible
	 * values: Fruchterman - layout_with_fr KK - layout_with_kk sugiyama -
	 * layout_with_sugiyama layout_as_tree layout_as_star layout.auto
	 * http://igraph.org/r/doc/layout_with_fr.html
	 * 
	 * @param layout
	 */
	public void doLayout(String layout) {
		String graphName = (String) retrieveVariable("GRAPH_NAME");
		// the color is saved as color
		try {
			eval("xy_layout <- " + layout + "(" + graphName + ")");
			synchronizeXY("xy_layout");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////   X-Ray Methods ///////////////////////
	/**
	 * Used for X-ray matching results
	 * 
	 * @param json
	 *            [ { "Source_Column": "db", "Source_Database": "col",
	 *            "Target_Column": "col", "Target_Database": "db" }, {...} ]
	 */
	public String addLogicalNames(String json) {
		Gson gson = new Gson();
		Type type = new TypeToken<List<HashMap<String, String>>>() {
		}.getType();
		List<HashMap<String, String>> values = gson.fromJson(json, type);
		boolean all = false;
		if (values != null) {
			all = true;
			for (HashMap<String, String> row : values) {
				String sourceDB = row.get("Source_Database");
				String sourceColumn = row.get("Source_Column");
				String targetDB = row.get("Target_Database");
				String targetColumn = row.get("Target_Column");
				IEngine sourceEngine = Utility.getEngine(sourceDB);
				IEngine targetEngine = Utility.getEngine(targetDB);
				if (sourceEngine != null && targetEngine != null) {
					boolean sourceSuccess = MasterDatabaseUtility.addLogicalName(sourceDB, sourceColumn, targetColumn);
					if (!sourceSuccess) {
						logger.info("Unable to extend data for Soure Database :" + sourceDB + " using " + sourceColumn
								+ " and " + targetColumn);
					}
					boolean targetSuccess = MasterDatabaseUtility.addLogicalName(targetDB, targetColumn, sourceColumn);
					if (!targetSuccess) {
						logger.info("Unable to extend data for Soure Database :" + targetDB + " using " + targetColumn
								+ " and " + sourceColumn);
					}
					all = all && sourceSuccess && targetSuccess;
				}
			}

		}
		if (all) {
			this.nounMetaOutput.add(new NounMetadata("", PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));
		}
		else {
			throw new IllegalArgumentException("Unable to merge databases check logger");
		}
		return "";
	}
	
	public String runXrayCompatibility(String configFileJson)
			throws SQLException, JsonParseException, JsonMappingException, IOException {

		// runs the full xray compatibility from the new UI
		HashMap<String, Object> config = new ObjectMapper().readValue(configFileJson, HashMap.class);
		HashMap<String, Object> parameters = (HashMap<String, Object>) config.get("parameters");

		//output folder for instance data to be written to
		String outputFolder = getBaseFolder() + "\\R\\XrayCompatibility\\Temp\\MatchingRepository";
		outputFolder = outputFolder.replace("\\", "/");

		
		//output folder for semantic data to be compared
		String semanticOutputFolder = getBaseFolder() + "\\R\\XrayCompatibility\\Temp\\SemanticRepository";
		semanticOutputFolder = semanticOutputFolder.replace("\\", "/");
		// clean output folder
		try {
			FileUtils.cleanDirectory(new File(outputFolder));
			FileUtils.cleanDirectory(new File(semanticOutputFolder));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		boolean semanticComparison = false;
		Boolean semanticParam = (Boolean) parameters.get("semanticMode");
		if (semanticParam != null) {
			semanticComparison = semanticParam.booleanValue();
		}
		
		boolean dataComparison = false;
		Boolean dataParam = (Boolean) parameters.get("dataMode");
		if (dataParam != null) {
			dataComparison = dataParam.booleanValue();
		}
		
		if((!semanticComparison && !dataComparison)) {
			dataComparison = true;
		}
		
		//Write text files to run xray from different sources
		// outputFolder/database;table;column.txt
		ArrayList<Object> connectors = (ArrayList<Object>) config.get("connectors");
		for (int i = 0; i < connectors.size(); i++) {
			HashMap<String, Object> connection = (HashMap<String, Object>) connectors.get(i);
			String connectorType = (String) connection.get("connectorType");
			HashMap<String, Object> connectorData = (HashMap<String, Object>) connection.get("connectorData");
			HashMap<String, Object> dataSelection = (HashMap<String, Object>) connection.get("dataSelection");
			if (connectorType.toUpperCase().equals("LOCAL")) {
				String engineName = (String) connectorData.get("engineName");
				IEngine engine = Utility.getEngine(engineName);
				for (String table : dataSelection.keySet()) {
					HashMap<String, Object> allColumns = (HashMap<String, Object>) dataSelection.get(table);
					for (String column : allColumns.keySet()) {
						Boolean selectedValue = (Boolean) allColumns.get(column);
						if (selectedValue) {
							if (table.equals(column)) {
								String fileName = engineName + ";" + table + ";";
								String testFilePath = outputFolder + "\\" + fileName + ".txt";
								testFilePath = testFilePath.replace("\\", "/");
								String uri = DomainValues.getConceptURI(table, engine, true);
								List<Object> instances;
								if (engine.getEngineType().equals(IEngine.ENGINE_TYPE.SESAME)) {
									instances = DomainValues.retrieveCleanConceptValues(uri, engine);
								} else {
									instances = DomainValues.retrieveCleanConceptValues(table, engine);
								}
								encodeInstances(instances, dataComparison, testFilePath, semanticComparison, semanticOutputFolder);
							} else {
								String fileName = engineName + ";" + table + ";" + column;
								String testFilePath = outputFolder + "\\" + fileName + ".txt";
								testFilePath = testFilePath.replace("\\", "/");
								String minHashFilePath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"
										+ Constants.R_ANALYTICS_SCRIPTS_FOLDER + "\\" + "encode_instances.r";
								minHashFilePath = minHashFilePath.replace("\\", "/");
								String conceptUri = DomainValues.getConceptURI(table, engine, true);
								String propUri = DomainValues.getPropertyURI(column, table, engine, false);
								List<Object> instances;
								if (engine.getEngineType().equals(IEngine.ENGINE_TYPE.SESAME)) {
									instances = DomainValues.retrieveCleanPropertyValues(conceptUri, propUri, engine);
								} else {
									instances = DomainValues.retrieveCleanPropertyValues(conceptUri, propUri, engine);
								}
								encodeInstances(instances, dataComparison, testFilePath, semanticComparison, semanticOutputFolder);
							}
						}

					}
				}
			} else if (connectorType.toUpperCase().equals("EXTERNAL")) {
				// process if jdbc connection
				String connectionUrl = (String) connectorData.get("connectionString");
				String port = (String) connectorData.get("port");
				String host = (String) connectorData.get("host");
				String schema = (String) connectorData.get("schema");
				String username = (String) connectorData.get("userName");
				String password = (String) connectorData.get("password");
				String newDBName = (String) connectorData.get("databaseName");
				String type = (String) connectorData.get("type");
				Connection con = buildConnection(type, host, port, username, password, schema);

				for (String table : dataSelection.keySet()) {
					HashMap<String, Object> allColumns = (HashMap<String, Object>) dataSelection.get(table);
					for (String column : allColumns.keySet()) {
						Boolean selectedValue = (Boolean) allColumns.get(column);
						if (selectedValue) {
							// build sql query - write only unique values
							StringBuilder sb = new StringBuilder();
							sb.append("SELECT DISTINCT ");
							sb.append(column);
							sb.append(" FROM ");
							sb.append(table);
							sb.append(";");
							String query = sb.toString();

							// execute query against db
							Statement stmt = null;
							try {
								stmt = con.createStatement();
								ResultSet rs = stmt.executeQuery(query);
								String fileName = newDBName + ";" + table + ";" + column;
								String testFilePath = outputFolder + "\\" + fileName + ".txt";
								testFilePath = testFilePath.replace("\\", "/");
								
								//get instances
								List<Object> instances = new ArrayList<Object>();
								try {
									while (rs.next()) {
										Object value = rs.getString(1);
										String row = "";
										if (value != null) {
											row = ((String) value).replaceAll("\"", "\\\"");
										}
										instances.add(row.toString());
									}
								} catch (SQLException e) {
									e.printStackTrace();
								}
								
								//encode instances
								encodeInstances(instances, dataComparison, testFilePath, semanticComparison, semanticOutputFolder);
								stmt.close();
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					}
				}
				con.close();
			} else if (connectorType.toUpperCase().equals("FILE")) {
				// process csv file reading
				String filePath = (String) connectorData.get("filePath");
				String extension = FilenameUtils.getExtension(filePath);
				if (extension.equals("csv") || extension.equals("txt")) {
					String[] csvFileName = filePath.split("\\\\");
					String fileName = csvFileName[csvFileName.length - 1].replace(".csv", "");
					// read csv into string[]
					char delimiter = ','; // TODO get from user
					CSVReader csv;
					if (delimiter == '\t') {
						csv = new CSVReader(new FileReader(new File(filePath)));
					} else {
						csv = new CSVReader(new FileReader(new File(filePath)));
					}
					List<String[]> rowData = csv.readAll(); // get all rows
					String[] headers = rowData.get(0);
					List<String> selectedCols = new ArrayList<String>();
					for (String col : dataSelection.keySet()) {
						// make a list of selected columns
						HashMap<String, Object> colInfo = (HashMap<String, Object>) dataSelection.get(col);
						for (String cols : colInfo.keySet()) {
							if ((Boolean) colInfo.get(cols) == true) {
								selectedCols.add(cols);
							}
						}
					}

					// iterate through selected columns and only grab those
					// instances where the indices match
					for (String col : selectedCols) {
						// find the index of the selected column in the header array
						int index = -1;
						for (String header : headers) {
							if (header.toUpperCase().equals(col.toUpperCase())) {
								index = Arrays.asList(headers).indexOf(header);
							}
						}

						// get instance values
						if (index != -1) {
							HashSet<Object> instances = new HashSet<Object>();
							for (int j = 0; j < rowData.size(); j++) {
								if (j == 1) {
									continue;
								}
								else {
									instances.add(rowData.get(j)[index]);
								}
							}
							String testFilePath = outputFolder + "\\" + fileName + ";" + col + ".txt";
							testFilePath = testFilePath.replace("\\", "/");
							encodeInstances(instances, dataComparison, testFilePath, semanticComparison, semanticOutputFolder);
						}
					}

				} else if (extension.equals("xls") || extension.equals("xlsx")) {
					XLFileHelper xl = new XLFileHelper();
					xl.parse(filePath);
					String sheetName = (String) connectorData.get("worksheet");

					// put all row data into a List<String[]>
					List<String[]> rowData = new ArrayList<String[]>();
					String[] row = null;
					while ((row = xl.getNextRow(sheetName)) != null) {
						rowData.add(row);
					}
					String[] headers = xl.getHeaders(sheetName);
					List<String> selectedCols = new ArrayList<String>();
					for (String col : dataSelection.keySet()) {
						// make a list of selected columns
						HashMap<String, Object> colInfo = (HashMap<String, Object>) dataSelection.get(col);
						for (String cols : colInfo.keySet()) {
							if ((Boolean) colInfo.get(cols) == true) {
								selectedCols.add(cols);
							}
						}
					}
					for (String col : selectedCols) {
						// find the index of the selected column in the header array
						int index = -1;
						for (String header : headers) {
							if (header.toUpperCase().equals(col.toUpperCase())) {
								index = Arrays.asList(headers).indexOf(header);
							}
						}

						// get instance values
						if (index != -1) {
							HashSet<Object> instances = new HashSet<Object>();
							for (int j = 0; j < rowData.size(); j++) {
								instances.add(rowData.get(j)[index]);
							}
							String testFilePath = outputFolder + "\\" + sheetName + ";" + col + ".txt";
							testFilePath = testFilePath.replace("\\", "/");
							encodeInstances(instances, dataComparison, testFilePath, semanticComparison, semanticOutputFolder);
						}
					}
				}
			}
		}

		String baseMatchingFolder = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\" + "XrayCompatibility";
		baseMatchingFolder = baseMatchingFolder.replace("\\", "/");

		// Semoss/R/Matching/Temp/rdbms
		String outputXrayDataFolder = baseMatchingFolder + "\\" + Constants.R_TEMP_FOLDER + "\\rdbms";
		outputXrayDataFolder = outputXrayDataFolder.replace("\\", "/");

		int nMinhash;
		int nBands;
		int instancesThreshold = 1;
		double similarityThreshold = -1;
		double candidateThreshold = -1;
		String matchingSameDBR = "FALSE";
		boolean matchingSameDB = false;
		if (parameters != null) {
			Object sim = parameters.get("similarity");
			Double similarity = null;
			if (sim instanceof Integer) {
				similarity = (double) ((Integer) sim).intValue();
			} else {
				similarity = (Double) sim;
			}
			if (similarity != null) {
				similarityThreshold = similarity.doubleValue();
			}
			Object cand = parameters.get("candidate");
			Double candidate = null;
			if (cand instanceof Integer) {
				candidate = (double) ((Integer) cand).intValue();
			} else {
				candidate = (Double) cand;
			}
			if (candidate != null) {
				candidateThreshold = candidate.doubleValue();
			}
			Boolean matchDB = (Boolean) parameters.get("matchSameDb");
			if (matchDB != null) {
				matchingSameDB = matchDB.booleanValue();
			}
		}
		
		if (similarityThreshold < 0 || similarityThreshold > 1) {
			similarityThreshold = 0.01;
		}

		if (candidateThreshold < 0 || candidateThreshold > 1) {
			candidateThreshold = 0.01;
		}

		// check if user wants to compare columns from the same database
		if (matchingSameDB) {
			matchingSameDBR = "TRUE";
		}

		// set other parameters
		if (candidateThreshold <= 0.03) {
			nMinhash = 3640;
			nBands = 1820;
		} else if (candidateThreshold <= 0.02) {
			nMinhash = 8620;
			nBands = 4310;
		} else if (candidateThreshold <= 0.01) {
			nMinhash = 34480;
			nBands = 17240;
		} else if (candidateThreshold <= 0.05) {
			nMinhash = 1340;
			nBands = 670;
		} else if (candidateThreshold <= 0.1) {
			nMinhash = 400;
			nBands = 200;
		} else if (candidateThreshold <= 0.2) {
			nMinhash = 200;
			nBands = 100;
		} else if (candidateThreshold <= 0.4) {
			nMinhash = 210;
			nBands = 70;
		} else if (candidateThreshold <= 0.5) {
			nMinhash = 200;
			nBands = 50;
		} else {
			nMinhash = 200;
			nBands = 40;
		}

		// Parameters for R script
		String rFrameName = "this.dt.name.is.reserved.for.semantic.matching";

		// Grab the utility script
		String utilityScriptPath = baseMatchingFolder + "\\" + "matching.R";
		utilityScriptPath = utilityScriptPath.replace("\\", "/");

		// Create R Script to run xray
		StringBuilder rsb = new StringBuilder();
		rsb.append("library(textreuse);");

		// Source the LSH function from the utility script
		rsb.append("source(\"" + utilityScriptPath + "\");");
		rsb.append(rFrameName + " <- data.frame();");

		// Run locality sensitive hashing to generate matches
		if(dataComparison) {
			rsb.append(rFrameName + " <- " + Constants.R_LSH_MATCHING_FUN + "(\"" + outputFolder + "\", " + nMinhash
					+ ", " + nBands + ", " + similarityThreshold + ", " + instancesThreshold + ", \""
					+ DomainValues.ENGINE_CONCEPT_PROPERTY_DELIMETER + "\", " + matchingSameDBR + ", \""
					+ outputXrayDataFolder + "\");");
		}
		runR(rsb.toString());
		
		//run xray on semantic folder
		if(semanticComparison) {
			rsb = new StringBuilder();
			String semanticComparisonFrame = "semantic.xray.df";
			rsb.append("library(textreuse);");

			// Source the LSH function from the utility script
			rsb.append("source(\"" + utilityScriptPath + "\");");
			rsb.append(semanticComparisonFrame + " <- data.frame();");
			String semanticOutput = baseMatchingFolder + "\\" + Constants.R_TEMP_FOLDER + "\\semantic";
			semanticOutput = semanticOutput.replace("\\", "/");
			rsb.append(semanticComparisonFrame + " <- " + Constants.R_LSH_MATCHING_FUN + "(\"" + semanticOutputFolder
					+ "\", " + nMinhash + ", " + nBands + ", " + similarityThreshold + ", " + instancesThreshold
					+ ", \"" + DomainValues.ENGINE_CONCEPT_PROPERTY_DELIMETER + "\", " + matchingSameDBR + ", \""
					+ semanticOutput + "\");");
			
			//join data xray df with semantic xray df if dataComparison frame was created
			if (dataComparison) {
				String mergeRScriptPath = baseMatchingFolder + "\\merge.r";
				mergeRScriptPath = mergeRScriptPath.replace("\\", "/");
				rsb.append("source(\"" + mergeRScriptPath + "\");");
				rsb.append(rFrameName + " <- xray_merge(" + rFrameName + ", " + semanticComparisonFrame + ");");
			} else {
				rsb.append(rFrameName + " <-" + semanticComparisonFrame + ";");
			}
			runR(rsb.toString());
		}

		// Synchronize from R
		NounMetadata noun = new NounMetadata(rFrameName, PixelDataType.CONST_STRING, PixelOperationType.CODE_EXECUTION);
		storeVariable("GRID_NAME", noun);
		synchronizeFromR();

		// TODO save to local master?

		// // Persist the data into a database
		// String matchingDbName = "MatchingRDBMSDatabase";
		// IEngine engine = Utility.getEngine(matchingDbName);
		//
		// // Only add to the engine if it is null
		// // TODO gracefully refresh the entire db
		// if (engine == null) {
		// MatchingDB db = new MatchingDB(getBaseFolder());
		// // creates rdf and rdbms dbs
		// // TODO specify dbType if desired
		// String matchingDBType = ImportOptions.DB_TYPE.RDBMS.toString();
		// db.saveDB(matchingDBType);
		//
		// }
		// this.hasReturnData = true;
		return "";
		// return null;
	}
	
	public String getXrayConfigList() throws JsonGenerationException, JsonMappingException, IOException {
		HashMap<String, Object> configMap = MasterDatabaseUtility.getXrayConfigList();
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String xRayConfigList = ow.writeValueAsString(configMap);
		this.nounMetaOutput.add(new NounMetadata(xRayConfigList, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));
		return xRayConfigList;
	}
	
	public String getXrayConfigFile(String configFileID) throws JsonGenerationException, JsonMappingException, IOException {
		String configFile = MasterDatabaseUtility.getXrayConfigFile(configFileID);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		this.nounMetaOutput.add(new NounMetadata(configFile, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));
		return configFile;
	}
	
	public String getSchemaForLocal(String engineName)
			throws JsonGenerationException, JsonMappingException, IOException {
		IEngine engine = Utility.getEngine(engineName);
		List<String> concepts = DomainValues.getConceptList(engine);
		QueryStruct qs = engine.getDatabaseQueryStruct();
		Map<String, Map<String, List>> relations = qs.getRelations();
		// get relations
		Map<String, List<String>> relationshipMap = new HashMap<String, List<String>>();
		//structure is Title = {inner.join={Genre, Studio, Nominated}}
		
		
		
		for(String concept : concepts) {
			concept = DomainValues.determineCleanConceptName(concept, engine);
			if(concept.equals("Concept")) {
				continue;
			}
			//check if concept is in the relationship hashmap, if not just add an empty list 
			List<String> conceptRelations = new ArrayList<String>();
			for (String key : relations.keySet()) {
				if (concept.equalsIgnoreCase(key)) {
					conceptRelations = relations.get(key).get("inner.join"); //TODO check if this changes 
				} 
			}
			relationshipMap.put(concept, conceptRelations);
			
		}
		

		// tablename: [{name, type}]
		HashMap<String, ArrayList<HashMap>> tableDetails = new HashMap<String, ArrayList<HashMap>>();

		for (String conceptURI : concepts) {
			String cleanConcept = DomainValues.determineCleanConceptName(conceptURI, engine);
			//ignore default concept value
			if (cleanConcept.equals("Concept")) {
				continue;
			}
			ArrayList<HashMap> allCols = new ArrayList<HashMap>();
			HashMap<String, String> colInfo = new HashMap<String, String>();
			colInfo.put("name", cleanConcept);
			String dataType = engine.getDataTypes(conceptURI);
			if(dataType != null) {
				dataType = IMetaData.convertToDataTypeEnum(dataType).toString();
			}
			else {
				dataType = IMetaData.DATA_TYPES.STRING.toString();
			}
			colInfo.put("type", dataType);
			allCols.add(colInfo);
			List<String> properties = DomainValues.getPropertyList(engine, conceptURI);
			for (String prop : properties) {
				String cleanProp = DomainValues.determineCleanPropertyName(prop, engine);
				HashMap<String, String> propInfo = new HashMap<String, String>();
				propInfo.put("name", cleanProp);
				dataType = engine.getDataTypes(prop);
				if(dataType != null) {
					dataType = IMetaData.convertToDataTypeEnum(dataType).toString();
				}
				else {
					dataType = IMetaData.DATA_TYPES.STRING.toString();
				}
				dataType = IMetaData.convertToDataTypeEnum(dataType).toString();
				propInfo.put("type", dataType);
				allCols.add(propInfo);
			}
			tableDetails.put(cleanConcept, allCols);
		}
		
		HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put("databaseName", engine.getEngineName());
		ret.put("tables", tableDetails);
		ret.put("relationships", relationshipMap);
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String schema = ow.writeValueAsString(ret);
		this.nounMetaOutput.add(new NounMetadata(schema, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));
		return schema;
	}
	
	public String getSchemaForXL(String filePath, String sheetName)
			throws JsonGenerationException, JsonMappingException, IOException {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		XLFileHelper helper = new XLFileHelper();
		helper.parse(filePath);
		ret.put("databaseName", FilenameUtils.getName(filePath).replace(".", "_"));

		// store the suggested data types
		Map<String, Map<String, String>> dataTypes = new Hashtable<String, Map<String, String>>();
		Map<String, String> sheetDataMap = new LinkedHashMap<String, String>();
		String[] columnHeaders = helper.getHeaders(sheetName);
		String[] predicatedDataTypes = helper.predictRowTypes(sheetName);

		HashMap<String, List<String>> relationshipMap = new HashMap<String, List<String>>();
		for (String concept : columnHeaders) {
			relationshipMap.put(concept, new ArrayList<String>());
		}

		ret.put("relationships", relationshipMap);

		dataTypes.put(sheetName, sheetDataMap);

		HashMap<String, HashMap> tableDetails = new HashMap<String, HashMap>();
		for (int i = 0; i < columnHeaders.length; i++) {
			HashMap<String, String> colDetails = new HashMap<String, String>();
			colDetails.put("name", columnHeaders[i]);
			String dataType = Utility.getCleanDataType(predicatedDataTypes[i]);
			colDetails.put("type", dataType);
			tableDetails.put(columnHeaders[i], colDetails);
		}

		ret.put("tables", tableDetails);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String schema = ow.writeValueAsString(ret);

		this.nounMetaOutput.add(new NounMetadata(schema, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));
		return schema;

	}

	
	
	public String getSchemaForCSV(String filePath, String delimiter) throws JsonGenerationException, JsonMappingException, IOException {
		CSVFileHelper cv = new CSVFileHelper();
		cv.setDelimiter(delimiter.charAt(0));
		cv.parse(filePath);
		String[] headers = cv.getAllCSVHeaders();
		String[] types = cv.predictTypes();
		
		HashMap<String, Object> ret = new HashMap<String, Object>();
		//generate db name
		String[] parts = filePath.split("\\\\");
		String dbName = parts[parts.length-1].replace(".", "_");
		// C:\\..\\file.csv -> file_csv
		ret.put("databaseName", dbName);
		
		//construct empty relationship map (assuming flat table)
		HashMap<String, List<String>> relationshipMap = new HashMap<String, List<String>>();
		for(String concept : headers) {
			relationshipMap.put(concept, new ArrayList<String>()); //return empty list for FE
		}
		
		ret.put("relationships", relationshipMap);
		
		//add column details
		//since it's a flat table we don't need to worry about concept/property relationships
		HashMap<String, HashMap> tableDetails = new HashMap<String, HashMap>();
		for(int i = 0; i < headers.length; i++) {
			HashMap<String, String> colDetails = new HashMap<String, String>();
			colDetails.put("name", headers[i]);
			String dataType = IMetaData.convertToDataTypeEnum(types[i]).toString();
			colDetails.put("type", dataType);
			tableDetails.put(headers[i], colDetails);
		}
		
		ret.put("tables", tableDetails);
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String schema = ow.writeValueAsString(ret);
		this.nounMetaOutput.add(new NounMetadata(schema, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));

		return schema;
		
	}

	private void encodeInstances(List<Object> instances, boolean dataMode, String filePath, boolean semanticComparison, String semanticFolder) {
		if (instances.size() > 1) {
			String minHashFilePath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"
					+ Constants.R_ANALYTICS_SCRIPTS_FOLDER + "\\" + "encode_instances.r";
			minHashFilePath = minHashFilePath.replace("\\", "/");

			String predictColumnFilePath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"
					+ Constants.R_ANALYTICS_SCRIPTS_FOLDER + "\\" + "master_concept.r";
			predictColumnFilePath = predictColumnFilePath.replace("\\", "/");
			StringBuilder rsb = new StringBuilder();
			rsb.append("library(textreuse);");
			rsb.append("source(" + "\"" + minHashFilePath + "\"" + ");");
			rsb.append("source(" + "\"" + predictColumnFilePath + "\"" + ");");

			// construct R dataframe
			String dfName = "df.xray";
			rsb.append(dfName + "<-data.frame(instances=character(), stringsAsFactors = FALSE);");
			for (int j = 0; j < instances.size(); j++) {
				rsb.append(dfName + "[" + (j + 1) + ",1" + "]");
				rsb.append("<-");
				if (instances.get(j) == null) {
					rsb.append("\"" + "" + "\"");
				} else {
					rsb.append("\"" + instances.get(j).toString() + "\"");
				}
				rsb.append(";");

			}
			String writeFrameResultsToFile = "";
			if(semanticComparison) {
				String semanticResults = "semantic.results.df";
				String colSelectString = "1";
				int numDisplay = 3;
				int randomVals = 20;
				
				rsb.append(semanticResults + "<- concept_xray(" + dfName +","+ colSelectString + "," + numDisplay + "," + randomVals + ");");
				String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
				writeFrameResultsToFile = "fileConn <- file(\"" + semanticFolder + "/" + fileName + "\");";
				writeFrameResultsToFile += "writeLines("+semanticResults +"$Predicted_Concept, fileConn);";
				writeFrameResultsToFile += "close(fileConn);";
			}
			rsb.append(writeFrameResultsToFile);
			if (dataMode) {
				rsb.append("encode_instances(" + dfName + "," + "\"" + filePath + "\"" + ");");
			}
			runR(rsb.toString());
		}
	}
	
	/**
	 * Used to encode instances from csv, excel, external rdbms
	 * @param filePath
	 * @param instances
	 */
	private void encodeInstances(HashSet<Object> instances, boolean dataMode, String filePath, boolean semanticComparison, String semanticFolder ) {
		if (instances.size() > 1) {
			String minHashFilePath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"
					+ Constants.R_ANALYTICS_SCRIPTS_FOLDER + "\\" + "encode_instances.r";
			minHashFilePath = minHashFilePath.replace("\\", "/");

			StringBuilder rsb = new StringBuilder();
			rsb.append("library(textreuse);");
			rsb.append("source(" + "\"" + minHashFilePath + "\"" + ");");

			// construct R dataframe
			String dfName = "df.xray";
			rsb.append(dfName + "<-data.frame(instances=character(), stringsAsFactors = FALSE);");
			int j = 0;
			for (Object value : instances) {
				rsb.append(dfName + "[" + (j + 1) + ",1" + "]");
				rsb.append("<-");
				if (value == null) {
					rsb.append("\"" + "" + "\"");
				} else {
					rsb.append("\"" + value.toString() + "\"");
				}
				rsb.append(";");
				j++;

			}
			//run predict column headers write output to folder
			String writeFrameResultsToFile = "";
			if(semanticComparison) {
				String semanticResults = "semantic.results.df";
				String colSelectString = "1";
				int numDisplay = 3;
				int randomVals = 20;
				
				rsb.append(semanticResults + "<- concept_xray(" + dfName +","+ colSelectString + "," + numDisplay + "," + randomVals + ");");
				String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
				writeFrameResultsToFile = "fileConn <- file(\"" + semanticFolder + "/" + fileName + "\");";
				writeFrameResultsToFile += "writeLines("+semanticResults +"$Predicted_Concept, fileConn);";
				writeFrameResultsToFile += "close(fileConn);";
			}
			rsb.append(writeFrameResultsToFile);
			if(dataMode) {
			rsb.append("encode_instances(" + dfName + "," + "\"" + filePath + "\"" + ");");
			}
			runR(rsb.toString());
		}
	}
	
	public String getSchemaForExternal(String type, String host, String port, String username, String password, String schema) throws SQLException  {
		Connection con = null;
		String schemaJSON = "";
		try {
			con = buildConnection(type, host, port, username, password, schema);
			String url = "";

			HashMap<String, ArrayList<HashMap>> tableDetails = new HashMap<String, ArrayList<HashMap>>(); // tablename:
			// [colDetails]
			HashMap<String, ArrayList<HashMap>> relations = new HashMap<String, ArrayList<HashMap>>(); // sub_table:
			// [(obj_table,
			// fromCol,
			// toCol)]

			DatabaseMetaData meta = con.getMetaData();
			ResultSet tables = meta.getTables(null, null, null, new String[] { "TABLE" });
			while (tables.next()) {
				ArrayList<String> primaryKeys = new ArrayList<String>();
				HashMap<String, Object> colDetails = new HashMap<String, Object>(); // name:
				// ,
				// type:
				// ,
				// isPK:
				ArrayList<HashMap> allCols = new ArrayList<HashMap>();
				HashMap<String, String> fkDetails = new HashMap<String, String>();
				ArrayList<HashMap> allRels = new ArrayList<HashMap>();

				String table = tables.getString("table_name");
				System.out.println("Table: " + table);
				ResultSet keys = meta.getPrimaryKeys(null, null, table);
				while (keys.next()) {
					primaryKeys.add(keys.getString("column_name"));

					System.out.println(keys.getString("table_name") + ": " + keys.getString("column_name") + " added.");
				}

				System.out.println("COLUMNS " + primaryKeys);
				keys = meta.getColumns(null, null, table, null);
				while (keys.next()) {
					colDetails = new HashMap<String, Object>();
					colDetails.put("name", keys.getString("column_name"));
					colDetails.put("type", keys.getString("type_name"));
					if (primaryKeys.contains(keys.getString("column_name"))) {
						colDetails.put("isPK", true);
					} else {
						colDetails.put("isPK", false);
					}
					allCols.add(colDetails);

					System.out.println(
							"\t" + keys.getString("column_name") + " (" + keys.getString("type_name") + ") added.");
				}
				tableDetails.put(table, allCols);

				System.out.println("FOREIGN KEYS");
				keys = meta.getExportedKeys(null, null, table);
				while (keys.next()) {
					fkDetails = new HashMap<String, String>();
					fkDetails.put("fromCol", keys.getString("PKCOLUMN_NAME"));
					fkDetails.put("toTable", keys.getString("FKTABLE_NAME"));
					fkDetails.put("toCol", keys.getString("FKCOLUMN_NAME"));
					allRels.add(fkDetails);

					System.out.println(keys.getString("PKTABLE_NAME") + ": " + keys.getString("PKCOLUMN_NAME") + " -> "
							+ keys.getString("FKTABLE_NAME") + ": " + keys.getString("FKCOLUMN_NAME") + " added.");
				}
				relations.put(table, allRels);
			}
			HashMap<String, Object> ret = new HashMap<String, Object>();
			ret.put("databaseName", con.getCatalog());
			ret.put("tables", tableDetails);
			ret.put("relationships", relations);
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			schemaJSON = ow.writeValueAsString(ret);
			this.nounMetaOutput.add(new NounMetadata(schemaJSON, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.CODE_EXECUTION));

			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return schemaJSON;
        
  }

	private Connection buildConnection(String type, String host, String port, String username, String password,
			String schema) throws SQLException, JsonGenerationException, JsonMappingException, IOException {
        Connection con = null;
        String url = "";

		try {
            if (type.equals("MYSQL")) {
                   Class.forName("com.mysql.jdbc.Driver");
                  // Connection URL format:
                  // jdbc:mysql://<hostname>[:port]/<DBname>?user=username&password=pw
                  url = "jdbc:mysql://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }
                  con = DriverManager.getConnection(url + "?user=" + username + "&password=" + new String(password));
            } else if (type.equals("Oracle")) {
                   Class.forName("oracle.jdbc.driver.OracleDriver");

                  // Connection URL format:
                  // jdbc:oracle:thin:@<hostname>[:port]/<service or sid>[-schema
                  // name]
                  url = "jdbc:oracle:thin:@HOST:PORT:SERVICE".replace("HOST", host).replace("SERVICE", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));
            } else if (type.equals("SQL_Server")) {
                   Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

                  // Connection URL format:
                  // jdbc:sqlserver://<hostname>[:port];databaseName=<DBname>
                  url = "jdbc:sqlserver://HOST:PORT;databaseName=SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));
            } else if (type.equals("DB2")) {
                   Class.forName("com.ibm.db2.jcc.DB2Driver");
                  
                  // Connection URL format:
                  // jdbc:db2://<hostname>[:port]/<databasename>
                  url = "jdbc:db2://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("ASTER_DB")) {
                   Class.forName("com.asterdata.ncluster.jdbc.core.NClusterJDBCDriver");
                  url = "jdbc:ncluster://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("SAP_HANA")) {
                   Class.forName("com.sap.db.jdbc.Driver");
                  url = "jdbc:sap://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));
            } else if (type.equals("MARIA_DB")) {
                   Class.forName("org.mariadb.jdbc.Driver");
                  url = "jdbc:mariadb://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("H2_DB")) {
                  Class.forName("org.h2.Driver");
                  //Local db
                  if(host.contains("C:")) {
                      url = "jdbc:h2:HOST/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);

                  } else {
                  url = "jdbc:h2:tcp://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  }
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }
                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("TERADATA")) {
                   Class.forName("com.teradata.jdbc.TeraDriver");
                  url = "jdbc:teradata://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("POSTGRES")) {
                   Class.forName("org.postgresql.Driver");
                  url = "jdbc:postgresql://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("DERBY")) {
                   Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                  url = "jdbc:derby://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("CASSANDRA")) {
                   Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
                  url = "jdbc:cassandra://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            } else if (type.equals("IMPALA")) {
                   Class.forName("com.cloudera.impala.jdbc3.Driver");
                  url = "jdbc:impala://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));
            } else if (type.equals("PHOENIX")) {
                   Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
                  url = "jdbc:phoenix:HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
                  if (port != null && !port.isEmpty()) {
                         url = url.replace(":PORT", ":" + port);
                  } else {
                         url = url.replace(":PORT", "");
                  }

                  con = DriverManager.getConnection(url, username, new String(password));

            }

     } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println(">>>>>DRIVER NOT FOUND. PLEASE ENSURE YOU HAVE ACCESS TO JDBC DRIVER");
     }
		return con;
	}

	protected void runSemanticBlending(String column, String numDisplay, String randomVals) {
		// move frame to r so we can run alg on it
		String colName = column;
		if (column.contains("__")) {
			colName = column.split("__")[1];
		} else if(dataframe.getTableName() != null) {
			column = dataframe.getTableName() + "__" + column;
		} 

		QueryStruct2 qs = new QueryStruct2();
		qs.setLimit( ((Number)Double.parseDouble(randomVals)).longValue() ); 
		QueryColumnSelector selector = new QueryColumnSelector(column);  
		qs.addSelector(selector);

		Iterator<IHeadersDataRow> it = (Iterator<IHeadersDataRow>) this.dataframe.query(qs); 
		//construct r frame
		String dfName = "frameSubset" + Utility.getRandomString(10);
		StringBuilder rsb = new StringBuilder();
		StringBuilder rsb2 = new StringBuilder(); 
		List<Object[]> instanceList = new ArrayList<Object[]>();
		while(it.hasNext()) {
			Object[] values = it.next().getRawValues();
			instanceList.add(values);
		}   

		String colNameString = colName + "= character()"; 
		for (int j = 0; j < instanceList.size(); j++) {
			rsb.append(dfName + "[" + (j+1) + ", 1]"); 
			rsb.append("<-");
			if (instanceList.get(j) == null) {
				rsb.append("\"" + "" + "\""); 
			} else {
				rsb.append("\"" + instanceList.get(j)[0].toString().replaceAll("_", " ") + "\"");
			}
			rsb.append(";"); 
		}

		rsb2.append(dfName + "<-data.frame("+ colNameString + ", stringsAsFactors = FALSE);");
		int colSelect = 1;

		String df2 = "PredictionTable" + Utility.getRandomString(10);
		String baseRScriptPath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"+ "AnalyticsRoutineScripts";
		String rScriptPath = (baseRScriptPath + "\\" + "master_concept.r").replace("\\", "/");

		// run r commands to get output
		runR("source(\"" + rScriptPath + "\");");
		runR(rsb2.toString() + rsb.toString());
		runR(df2 + " <- concept_mgr(" + dfName + "," + colSelect + "," + numDisplay + "," + randomVals + ")");
		runR(df2 + " <- as.data.table(" + df2 + ")");

		String[] colNames = {"Predicted_Concept", "Prob"};
		this.nounMetaOutput.add(new NounMetadata(flushObjectAsTable(df2, colNames), PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.WIKI_LOGICAL_NAMES));
	}

	public void predictColumnHeader(String[] column, String numDisplay, String randomVals) {
		QueryStruct2 qs = new QueryStruct2();
		qs.setLimit( ((Number)Double.parseDouble(randomVals)).longValue() ); 
		for (int i = 0; i < column.length; i++) {
			QueryColumnSelector selector  = new QueryColumnSelector(column[i]);
			qs.addSelector(selector);
		}

		Iterator<IHeadersDataRow> it = (Iterator<IHeadersDataRow>) this.dataframe.query(qs);
		//construct r dataframe
		String dfName = "frameSubset" + Utility.getRandomString(10);
		StringBuilder rsb = new StringBuilder();
		StringBuilder rsb2 = new StringBuilder();

		List<Object[]> instanceList = new ArrayList<Object[]>();
		while (it.hasNext()) {
			Object[] values = it.next().getRawValues();
			instanceList.add(values);
		}

		String colNameString = ""; 
		for (int i = 0; i < column.length; i++) {
			colNameString += column[i].split("__")[1];
			colNameString += "= character()" + ",";
			for (int j = 0; j < instanceList.size(); j++) {
				rsb.append(dfName + "[" + (j + 1) + "," + (i + 1) + "]");
				rsb.append("<-");
				if (instanceList.get(j) == null) {
					rsb.append("\"" + "" + "\"");
				} else {

					rsb.append("\"" + instanceList.get(j)[i].toString().replaceAll("_", " ") + "\"");
				}
				rsb.append(";");
			}
		}

		colNameString = colNameString.substring(0, colNameString.length() - 1); 
		rsb2.append(dfName + "<-data.frame("+ colNameString + ", stringsAsFactors = FALSE);");

		String colSelectString = ",c(";

		for (int i = 0; i < column.length; i++) {
			colSelectString += (i + 1) + ",";
		}

		int remove = colSelectString.length() - 1;
		colSelectString = colSelectString.substring(0, remove);
		colSelectString += ")";

		String df2 = "PredictionTable";
		String baseRScriptPath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"+ "AnalyticsRoutineScripts";
		String rScriptPath = (baseRScriptPath + "\\" + "master_concept.r").replace("\\", "/");

		// run r commands to get output
		runR("source(\"" + rScriptPath + "\");");
		//runR(colSelectString.substring(1, colSelectString.length()));
		System.out.println(df2 + "<- concept_mgr(" + dfName + colSelectString + "," + numDisplay + "," + randomVals + ")");
		runR(rsb2.toString() + rsb.toString());
		//runR(rsb2.toString());
		runR(df2 + "<- concept_mgr(" + dfName + colSelectString + "," + numDisplay + "," + randomVals + ")");
		runR(df2 + " <- as.data.table(" + df2 + ")");

		// need to make a new r table to store this info so we can later query it
		RDataTable table = null;
		if (retrieveVariable(R_CONN) != null && retrieveVariable(R_PORT) != null) {
			table = new RDataTable(df2, (RConnection) retrieveVariable(R_CONN), (String) retrieveVariable(R_PORT));
		} else {
			// if we dont have a current r session
			// but when we create the table it makes one
			// store those variables so we end up using that
			table = new RDataTable(df2);
			if (table.getConnection() != null && table.getPort() != null) {
				storeVariable(R_CONN, new NounMetadata(table.getConnection(), PixelDataType.R_CONNECTION));
				storeVariable(R_PORT, new NounMetadata(table.getPort(), PixelDataType.CONST_STRING));
			}
		}
		// create the new frame meta
		OwlTemporalEngineMeta metaData = table.getMetaData();
		metaData.addVertex(df2);
		metaData.setPrimKeyToVertex(df2, true);
		metaData.addProperty(df2, "Original_Column");
		metaData.setAliasToProperty("Original_Column", "Original_Column");
		metaData.setDataTypeToProperty("Original_Column", "STRING");
		metaData.addProperty(df2, "Predicted_Concept");
		metaData.setAliasToProperty("Predicted_Concept", "Predicted_Concept");
		metaData.setDataTypeToProperty("Predicted_Concept", "STRING");
		metaData.addProperty(df2, "URL");
		metaData.setAliasToProperty("URL", "URL");
		metaData.setDataTypeToProperty("URL", "STRING");
		metaData.addProperty(df2, "Prob");
		metaData.setAliasToProperty("Prob", "Prob");
		metaData.setDataTypeToProperty("Prob", "NUMBER");

		// store the r variable
		NounMetadata frameNoun = new NounMetadata(table, PixelDataType.FRAME);
		this.storeVariable("predictionFrame", frameNoun);
		this.nounMetaOutput.add(frameNoun);
	}

}
