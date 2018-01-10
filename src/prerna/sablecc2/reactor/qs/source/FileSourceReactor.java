package prerna.sablecc2.reactor.qs.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import prerna.query.querystruct.CsvQueryStruct;
import prerna.query.querystruct.ExcelQueryStruct;
import prerna.query.querystruct.QueryStruct2;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.reactor.qs.AbstractQueryStructReactor;
import prerna.util.ga.GATracker;

public class FileSourceReactor extends AbstractQueryStructReactor {

	//keys to get inputs from pixel command
	private static final String FILEPATH = ReactorKeysEnum.FILE_PATH.getKey();
	private static final String FILENAME = ReactorKeysEnum.FILE_NAME.getKey();
	private static final String SHEET_NAME = "sheetName";
	private static final String DATA_TYPES = ReactorKeysEnum.DATA_TYPE_MAP.getKey();
	private static final String DELIMITER = ReactorKeysEnum.DELIMITER.getKey();
	private static final String HEADER_NAMES = ReactorKeysEnum.NEW_HEADER_NAMES.getKey();

	/**
	 * FileRead args 
	 * 
	 * FILE=["filePath"]
	 * 
	 * for csv must add delim = ["delimiter"]
	 * for excel must add sheetName = ["sheetName"]
	 * 
	 * to set dataTypes 
	 *     dataTypesMap = [{"column", "type"}]
	 * to set newHeaders
	 *     newHeaders = [{"oldColumn", "newColumn"}] 
	 */
	
	public FileSourceReactor() {
		this.keysToGet = new String[]{FILEPATH, FILENAME, SHEET_NAME, DATA_TYPES, DELIMITER, HEADER_NAMES};
	}

	@Override
	protected QueryStruct2 createQueryStruct() {
		QueryStruct2 qs = null;

		// get inputs
		Map<String, String> dataTypes = getDataTypes(); 
		Map<String, String> headers = getHeaders(); 
		String fileLocation = getFileLocation();
		String fileExtension = "";

		// get file extension to determine qs
		boolean isExcel = false;
		if (fileLocation.contains(".")) {
			fileExtension = fileLocation.substring(fileLocation.indexOf('.'), fileLocation.length());
			if (fileExtension.equals(".xlsx")) {
				isExcel = true;
			}
		}
		if(isExcel) { // set excelQS
			// get excel inputs
			String sheetName = getSheetName();
			qs = new ExcelQueryStruct();
			((ExcelQueryStruct) qs).setExcelFilePath(fileLocation);
			((ExcelQueryStruct) qs).setSheetName(sheetName);
			((ExcelQueryStruct) qs).setColumnTypes(dataTypes);
			((ExcelQueryStruct) qs).setNewHeaderNames(headers);
		} else { // set csv qs
			char delimiter = getDelimiter();
			qs = new CsvQueryStruct();
			((CsvQueryStruct) qs).setCsvFilePath(fileLocation);
			((CsvQueryStruct) qs).setDelimiter(delimiter);
			((CsvQueryStruct) qs).setColumnTypes(dataTypes);
			((CsvQueryStruct) qs).setNewHeaderNames(headers);
		}
		qs.merge(this.qs);
		
		// Formatting and Tracking for Google Analytics
		String FileName = getFileName();
		List<String> heads = new ArrayList<String>(dataTypes.keySet());

		// track GA data
		GATracker.getInstance().trackDragAndDrop(this.insight, heads, FileName);

		return qs;
	}

	/**************************************************************************************************
	 ************************************* INPUT METHODS***********************************************
	 **************************************************************************************************/

	private Map<String, String> getHeaders() {
		GenRowStruct headersGRS = this.store.getNoun(HEADER_NAMES);
		Map<String, String> headers = null;
		NounMetadata dataNoun;
		if (headersGRS != null) {
			dataNoun = headersGRS.getNoun(0);
			headers = (Map<String, String>) dataNoun.getValue();
		}
		return headers;
	}

	private String getSheetName() {
		GenRowStruct sheetGRS = this.store.getNoun(SHEET_NAME);
		String sheetName = "";
		NounMetadata sheetNoun;
		if (sheetGRS != null) {
			sheetNoun = sheetGRS.getNoun(0);
			sheetName = (String) sheetNoun.getValue();
		} else {
			throw new IllegalArgumentException("Need to specify " + SHEET_NAME + "=[sheetName] in pixel command");
		}
		return sheetName;
	}

	private Map<String, String> getDataTypes() {
		GenRowStruct dataTypeGRS = this.store.getNoun(DATA_TYPES);
		Map<String, String> dataTypes = null;
		NounMetadata dataNoun;
		if (dataTypeGRS != null) {
			dataNoun = dataTypeGRS.getNoun(0);
			dataTypes = (Map<String, String>) dataNoun.getValue();
		}
		return dataTypes;

	}

	private char getDelimiter() {
		GenRowStruct delimGRS = this.store.getNoun(DELIMITER);
		String delimiter = "";
		char delim = ','; //default
		NounMetadata instanceIndexNoun;

		if (delimGRS != null) {
			instanceIndexNoun = delimGRS.getNoun(0);
			delimiter = (String) instanceIndexNoun.getValue();
		} else {
			throw new IllegalArgumentException("Need to specify " + DELIMITER + "=[delimiter] in pixel command");
		}

		//get char from input string
		if(delimiter.length() > 0) {
			delim = delimiter.charAt(0);
		}

		return delim;
	}

	private String getFileLocation() {
		GenRowStruct fileGRS = this.store.getNoun(FILEPATH);
		String fileLocation = "";
		NounMetadata fileNoun;
		if (fileGRS != null) {
			fileNoun = fileGRS.getNoun(0);
			fileLocation = (String) fileNoun.getValue();
		} else {
			throw new IllegalArgumentException("Need to specify " + FILEPATH + "=[filePath] in pixel command");
		}
		return fileLocation;
	}
	private String getFileName() {
		GenRowStruct fileGRS = this.store.getNoun(FILENAME);
		String fileName = "NoFileName";
		NounMetadata fileNoun;
		if (fileGRS != null) {
			fileNoun = fileGRS.getNoun(0);
			fileName = (String) fileNoun.getValue();
		}
		return fileName;
	}
	
	///////////////////////// KEYS /////////////////////////////////////

	@Override
	protected String getDescriptionForKey(String key) {
		if (key.equals(SHEET_NAME)) {
			return "The excel sheet name";
		} else {
			return super.getDescriptionForKey(key);
		}
	}
}