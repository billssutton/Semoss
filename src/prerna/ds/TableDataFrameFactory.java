package prerna.ds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import prerna.algorithm.api.IMetaData.DATA_TYPES;
import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.H2.H2Frame;
import prerna.ds.util.TinkerCastHelper;
import prerna.poi.main.helper.CSVFileHelper;
import prerna.poi.main.helper.XLFileHelper;
import prerna.util.Utility;

public class TableDataFrameFactory {

	private static final String CSV_FILE_KEY = "CSV";
	
	/**
	 * Method to generate a table data frame from a file
	 * @param dataStr
	 * @param delimeter
	 * @param dataTypeMap 
	 * @param mainConcept 
	 * @return
	 */
	public static ITableDataFrame generateDataFrameFromFile(String fileLoc, String delimeter, String dataFrameType, Map<String, Map<String, String>> dataTypeMap, Map<String, String> mainCol) {
		if(dataFrameType.equalsIgnoreCase("H2")) {
			if(fileLoc.endsWith(".xlsx") || fileLoc.endsWith(".xlsm")) {
				return generateH2FrameFromExcel(fileLoc, dataTypeMap, mainCol);
			} else {
				return generateH2FrameFromFile(fileLoc, delimeter, dataTypeMap.get(CSV_FILE_KEY), mainCol.get(CSV_FILE_KEY));
			}
		} else {
			if(fileLoc.endsWith(".xlsx") || fileLoc.endsWith(".xlsm")) {
				return generateTinkerFrameFromExcel(fileLoc, dataTypeMap, mainCol);
			} else {
				return generateTinkerFrameFromFile(fileLoc, delimeter, dataTypeMap.get(CSV_FILE_KEY), mainCol.get(CSV_FILE_KEY));
			}
		}
	}
	
	//////////////////////// START EXCEL LOADING //////////////////////////////////////
	
	private static TinkerFrame generateTinkerFrameFromExcel(String fileLoc, Map<String, Map<String, String>> dataTypeMap, Map<String, String> mainCols) {
		XLFileHelper helper = new XLFileHelper();
		helper.parse(fileLoc);
		String[] tables = helper.getTables();
		
		TinkerFrame tf = null;
		TinkerCastHelper caster = new TinkerCastHelper();

		int tableCounter = 0;
		for(int i = 0; i < tables.length; i++)
		{
			String primKeyHeader = null;
			String sheetName = tables[i];
			
			String [] headers = null;
			String [] types = null;
			
			if(dataTypeMap != null && !dataTypeMap.isEmpty()) {
				Map<String, String> sheetMap = dataTypeMap.get(sheetName);
				
				if(sheetMap == null || sheetMap.isEmpty()) {
					//not loading anything from this sheet
					continue;
				}
				
				headers = sheetMap.keySet().toArray(new String[]{});
				headers = helper.orderHeadersToGet(sheetName, headers);
				
				types = new String[headers.length];
				for(int j = 0; j < headers.length; j++) {
					types[j] = sheetMap.get(headers[j]);
				}
			} else {
				headers = helper.getHeaders(sheetName);
				types = helper.predictRowTypes(sheetName);			
			}
			
			String mainCol = mainCols.get(sheetName);
			if(tf == null) {
				tf = (TinkerFrame) createDataFrame(headers, "TinkerFrame", types, mainCol);
			} else {
				Map<String, Set<String>> newEdgeHash = null;
				if(mainCol != null) {
					newEdgeHash = createFlatEdgeHash(mainCol, headers);
				} else {
					newEdgeHash = new Hashtable<String, Set<String>>();
					// need to create a new prim_key vertex
					Set<String> values = new HashSet<String>();
					values.addAll(Arrays.asList(headers));
					primKeyHeader = TinkerFrame.PRIM_KEY + "_" + i;
					newEdgeHash.put(primKeyHeader, values);
				}
				
				Map<String, String> datatypes = new HashMap<>();
				for(int x = 0; x < headers.length; x++) {
					datatypes.put(headers[x], types[x]);
				}
				tf.mergeEdgeHash(newEdgeHash, datatypes);
//				tf.addMetaDataTypes(headers, types);
			}
			
			Object[] values = null;	
			String[] row = null;
			helper.getNextRow(sheetName); // first row is header
			while( ( row = helper.getNextRow(sheetName, headers) ) != null) {
				values = caster.castToTypes(row, types);
				Map<String, Object> cleanRow = new HashMap<>();
				Map<String, Object> rawRow = new HashMap<>();
				for(int j = 0; j < headers.length; j++) {
					
					String header = headers[j];
					Object value = values[j];
					String rawVal = "http://" + header + "/" + value;

					cleanRow.put(headers[j], values[j]);
					rawRow.put(header, rawVal);
				}
				if(tableCounter == 0) {
					if(mainCol == null) {
						tf.addRow(cleanRow, rawRow);
					} else {
						tf.addRelationship(cleanRow, rawRow);
					}
				} else {
					String primKeyVal = values.hashCode() + "";
					cleanRow.put(primKeyHeader, primKeyVal);
					rawRow.put(primKeyHeader, primKeyVal);
					
					tf.addRelationship(cleanRow, rawRow);
				}
			}
			tableCounter++;
		}
		
		return tf;
	}

	private static H2Frame generateH2FrameFromExcel(String fileLoc, Map<String, Map<String, String>> dataTypeMap, Map<String, String> mainCols) {
		
		XLFileHelper helper = new XLFileHelper();
		helper.parse(fileLoc);
		
		H2Frame dataFrame = null;
		String[] tables = helper.getTables();
		for(int i = 0; i < tables.length; i++)
		{
			String primKeyHeader = null;
			String table = tables[i];
			
			String [] headers = null;
			String [] types = null;
			
			if(dataTypeMap != null && !dataTypeMap.isEmpty()) {
				Map<String, String> sheetMap = dataTypeMap.get(table);
				
				if(sheetMap == null || sheetMap.isEmpty()) {
					//not loading anything from this sheet
					continue;
				}
				
				headers = sheetMap.keySet().toArray(new String[]{});
				headers = helper.orderHeadersToGet(table, headers);
				
				types = new String[headers.length];
				for(int j = 0; j < headers.length; j++) {
					types[j] = sheetMap.get(headers[j]);
				}
			} else {
				headers = helper.getHeaders(table);
				types = helper.predictRowTypes(table);			
			}
			
			String mainCol = mainCols.get(table);
			if(dataFrame == null) {
				dataFrame = (H2Frame) createDataFrame(headers, "H2", types, mainCol);
			} else {
				Map<String, Set<String>> newEdgeHash = null;
				if(mainCol != null) {
					newEdgeHash = createFlatEdgeHash(mainCol, headers);
				} else {
					newEdgeHash = new Hashtable<String, Set<String>>();
					// need to create a new prim_key vertex
					Set<String> values = new HashSet<String>();
					values.addAll(Arrays.asList(headers));
					primKeyHeader = TinkerFrame.PRIM_KEY + "_" + i;
					newEdgeHash.put(primKeyHeader, values);
				}
			}
			
			// unique names always match the headers when creating from csv/excel
//			String[] values = new String[headers.length];
//			for(int j = 0; j < headers.length; j++) {
//				values[j] = dataFrame.getValueForUniqueName(headers[j]);
//			}
			
			String tableName = null;
			if(mainCol != null) {
				tableName = mainCol;
			} else {
				tableName = dataFrame.getTableNameForUniqueColumn(headers[0]);
			}
			String [] cells = null;
			while((cells = helper.getNextRow(table, headers)) != null) {
				dataFrame.addRow(tableName, cells, headers, types);
			}
		}
		dataFrame.setRelations(helper.getRelations());
		
		return dataFrame;
	}

	//////////////////////// END EXCEL LOADING //////////////////////////////////////

	
	//////////////////////// START CSV LOADING //////////////////////////////////////
	
	private static H2Frame generateH2FrameFromFile(String fileLoc, String delimiter, Map<String, String> dataTypeMap, String mainCol) {
		CSVFileHelper helper = new CSVFileHelper();
		helper.setDelimiter(delimiter.charAt(0));
		helper.parse(fileLoc);
		
		String [] headers = null;
		String [] types = null;
		
		if(dataTypeMap != null && !dataTypeMap.isEmpty()) {
			headers = dataTypeMap.keySet().toArray(new String[]{});
			headers = helper.orderHeadersToGet(headers);
			
			types = new String[headers.length];
			for(int j = 0; j < headers.length; j++) {
				types[j] = dataTypeMap.get(headers[j]);
			}
			
			helper.parseColumns(headers);
			helper.getNextRow(); // next row is a header
		} else {
			headers = helper.getHeaders();
			types = helper.predictTypes();			
		}
		
		H2Frame dataFrame = (H2Frame) createDataFrame(headers, "H2", types, mainCol);
		
		// unique names always match the headers when creating from csv/excel
//		String[] values = new String[headers.length];
//		for(int i = 0; i < headers.length; i++) {
//			values[i] = dataFrame.getValueForUniqueName(headers[i]);
//		}
		
		String tableName = null;
		if(mainCol != null) {
			tableName = mainCol;
		} else {
			tableName = dataFrame.getTableNameForUniqueColumn(headers[0]);
		}
		
		String [] cells = null;
		while((cells = helper.getNextRow()) != null) {
			dataFrame.addRow(tableName, cells, headers, types);
		}
		return dataFrame;
	}
	
	private static TinkerFrame generateTinkerFrameFromFile(String fileLoc, String delimiter, Map<String, String> dataTypeMap, String mainCol) {
		CSVFileHelper helper = new CSVFileHelper();
		helper.setDelimiter(delimiter.charAt(0));
		helper.parse(fileLoc);
		
		String [] headers = null;
		String [] types = null;
		
		if(dataTypeMap != null && !dataTypeMap.isEmpty()) {
			headers = dataTypeMap.keySet().toArray(new String[]{});
			headers = helper.orderHeadersToGet(headers);
			
			types = new String[headers.length];
			for(int j = 0; j < headers.length; j++) {
				types[j] = dataTypeMap.get(headers[j]);
			}
			
			helper.parseColumns(headers);
			helper.getNextRow(); // next row is a header
		} else {
			headers = helper.getHeaders();
			types = helper.predictTypes();			
		}

		TinkerFrame dataFrame = (TinkerFrame) createDataFrame(headers, "TinkerFrame", types, mainCol);
		
		TinkerCastHelper caster = new TinkerCastHelper();
		String[] cells = null;
		Object[] values = null;
		while((cells = helper.getNextRow()) != null) {
		
			values = caster.castToTypes(cells, types);
			Map<String, Object> row = new HashMap<>();
			Map<String, Object> rawRow = new HashMap<>();
			for(int i = 0; i < headers.length; i++) {
				
				String header = headers[i];
				Object value = values[i];
				String rawVal = "http://" + header + "/" + value;

				row.put(headers[i], values[i]);
				rawRow.put(header, rawVal);
			}
			if(mainCol == null) {
				dataFrame.addRow(row, rawRow);
			} else {
				dataFrame.addRelationship(row, rawRow);
			}
		}
				
		return dataFrame;
	}
	
	//////////////////////// END CSV LOADING //////////////////////////////////////

	private static ITableDataFrame createDataFrame(String[] headers, String dataFrameType, String[] types, String mainConcept) {
		ITableDataFrame dataFrame = null;
		Map<String, Set<String>> edgeHash = null;
		if(dataFrameType.equalsIgnoreCase("H2")) {
			dataFrame = new H2Frame(headers);
		} else {
			dataFrame = new TinkerFrame(headers);
		}
		
		// user has defined an edge hash
		if(mainConcept != null) {
			edgeHash = createFlatEdgeHash(mainConcept, headers);
		} else {
			// no user defined edge hash, create prim key
			edgeHash = TinkerMetaHelper.createPrimKeyEdgeHash(headers);
		}
		Map<String, String> datatypes = new HashMap<>();
		for(int x = 0; x < headers.length; x++) {
			datatypes.put(headers[x], types[x]);
		}
		dataFrame.mergeEdgeHash(edgeHash, datatypes);
//		dataFrame.addMetaDataTypes(headers, types);
		return dataFrame;
	}
	
	private static Map<String, Set<String>> createFlatEdgeHash(String mainCol, String[] headers) {
		Map<String, Set<String>> edgeHash = new HashMap<String, Set<String>>();
		Set<String> edges = new LinkedHashSet<>();
		for(String header : headers) {
			if(!header.equals(mainCol)) {
				edges.add(header);
			}
		}
		edgeHash.put(mainCol, edges);
		return edgeHash;
	}
	
	public static H2Frame convertToH2Frame(ITableDataFrame table) {

		if(table instanceof H2Frame) return (H2Frame)table;
		
		if(table instanceof AbstractTableDataFrame) {
		
			AbstractTableDataFrame atable = (AbstractTableDataFrame)table;
			String[] headers = atable.getColumnHeaders();
			String[] types = new String[headers.length];
			for(int i = 0; i < headers.length; i++) {
				DATA_TYPES type = atable.metaData.getDataType(headers[i]);
				if(type.equals(DATA_TYPES.NUMBER)) {
					types[i] = "DOUBLE";
				} else if(type.equals(DATA_TYPES.DATE)) {
					types[i] = "DATE";
				} else {
					types[i] = "VARCHAR(800)";
				}
			}
			H2Frame dataFrame = (H2Frame) createDataFrame(headers, "H2", types, null);
			
					
			String tableName = dataFrame.getTableNameForUniqueColumn(headers[0]);
			
			Iterator<Object[]> iterator = atable.iterator(false);
			while(iterator.hasNext()) {
				Object[] row = iterator.next();
				String[] cells = new String[row.length];
				for(int i = 0; i < row.length; i++) {
					cells[i] = row[i].toString();
				}
				dataFrame.addRow(tableName, cells, headers, types);
			}
			
			return dataFrame;
		}
		return null;
	}
	
	/**
	 * 
	 * @param table 	table to convert to a tinker frame
	 * @return			converts an ITableDataFrame of type H2Frame to an ITableDataFrame of type TinkerFrame
	 * 
	 */
	public static TinkerFrame convertToTinkerFrame(ITableDataFrame table) {
		//		frame.addRelationship(ss.getHeaders(), ss.getValues(), ss.getRawValues(), cardinality, logicalToValue);
		if(table instanceof TinkerFrame) {
			return (TinkerFrame)table;
		} else if(table instanceof H2Frame) {

			TinkerFrame frame = null;

			H2Frame h2frame = (H2Frame)table;
			//get an iterator and skip duplicates
			Map<String, Object> options = new HashMap<>();
			options.put(TinkerFrame.DE_DUP, true);
			options.put(TinkerFrame.SELECTORS, h2frame.getSelectors());
			Iterator<Object[]> iterator = h2frame.iterator(false, options);

			String[] columnHeaders  = h2frame.getSelectors().toArray(new String[]{});
			Map<Integer, Set<Integer>> cardinality = Utility.getCardinalityOfValues(columnHeaders, h2frame.getEdgeHash());
			Map<String, String> uniqueToValue = h2frame.metaData.getAllUniqueNamesToValues();

			String[] types = new String[columnHeaders.length];
			int i = 0;
			for(String header : columnHeaders) {
				String headerType = h2frame.getDataType(header).toString();
				types[i] = headerType;
				i++;
			}

			//for each row add that relationship to tinker
			while(iterator.hasNext()) {
				Object[] row = iterator.next();
				if(cardinality == null || cardinality.isEmpty()) {
					frame = (TinkerFrame)createDataFrame(columnHeaders, "tinker", types, null);
					frame.addRow(row, row, columnHeaders);
				} else{
					frame = new TinkerFrame();
					frame.metaData = h2frame.metaData; //set the meta data for the new frame
					frame.addRelationship(columnHeaders, row, row, cardinality, uniqueToValue);
				}
			}
			return frame;
		}

		return null;
	}

}
