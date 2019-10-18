package prerna.sablecc2.reactor.frame.r;

import java.util.List;
import java.util.Vector;

import prerna.ds.r.RDataTable;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.usertracking.AnalyticsTrackerHelper;
import prerna.util.usertracking.UserTrackerFactory;

public class RegexReplaceColumnValueReactor extends AbstractRFrameReactor {

	/**
	 * This reactor updates row values based on a regex
	 * It replaces all portions of the current cell value that is an exact match to the input value
	 * The inputs to the reactor are: 
	 * 1) the column to update
	 * 2) the regex to look for
	 * 3) value to replace the regex with 
	 */
	
	public RegexReplaceColumnValueReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.COLUMN.getKey(), ReactorKeysEnum.VALUE.getKey(), ReactorKeysEnum.NEW_VALUE.getKey() };
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		//initialize rJavaTranslator
		init();
		// get frame
		RDataTable frame = (RDataTable) getFrame();
		
		//get table name
		String table = frame.getName();
		
		// get inputs
		// first input is the column that we are updating
		List<String> columnNames = getColumns();

		//get regular expression
		String regex = this.keyValue.get(this.keysToGet[1]);
		if (regex == null) {
			regex = getRegex();
		}
		
		//get new value
		String newValue = this.keyValue.get(this.keysToGet[2]);
		if (newValue == null) {
			newValue = getNewValue();
		}
		
		// iterate through all passed columns
		StringBuilder script = new StringBuilder();
		for (String column : columnNames) {
			
			// define r script
			String colScript = table + "$" + column;
			script.append(colScript + " = ");
			String dataType = getColumnType(table, column);
			String quote = "";
			if (dataType.contains("character") || dataType.contains("factor")) {
				quote = "\"";
			}
			// script is of the form FRAME$Genre = gsub("-","M", FRAME$Genre)
			script.append("gsub(" + quote + regex + quote + "," + quote + newValue + quote + ", " + colScript + ");");
				
			// doing gsub on a numeric column changes the data type to a string
			// so change it back to numeric in r
			if(dataType.contains("numeric")) {
				script.append(table + "$" + column + " <- as.numeric(" + table + "$" + column + ");");
			} else if(dataType.contains("integer")) {
				script.append(table + "$" + column + " <- as.integer(" + table + "$" + column + ");");
			}
			
		}

		this.rJavaTranslator.runR(script.toString());

		// NEW TRACKING
		UserTrackerFactory.getInstance().trackAnalyticsWidget(
				this.insight, 
				frame, 
				"RegexReplaceColumnValue", 
				AnalyticsTrackerHelper.getHashInputs(this.store, this.keysToGet));
		
		return new NounMetadata(frame, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE);
	}
	
	private List<String> getColumns() {
		List<String> cols = new Vector<String>();

		GenRowStruct grs = this.store.getNoun(this.keysToGet[0]);
		if(grs != null && !grs.isEmpty()) {
			for(int i = 0; i < grs.size(); i++) {
				String column =grs.get(i).toString();
				if (column.contains("__")) {
					column = column.split("__")[1];
				}
				cols.add(column);
			}
			return cols;
		}
		
		return cols;
	}
	
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	///////////////////////// GET PIXEL INPUT ////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	
	private String getUpdateColumn() {
		GenRowStruct inputsGRS = this.getCurRow();
		if (inputsGRS != null && !inputsGRS.isEmpty()) {
			//first input is the column that we are updating
			NounMetadata input1 = inputsGRS.getNoun(0);
			String column = input1.getValue() + "";
			if (column.length() == 0) {
				throw new IllegalArgumentException("Need to defne the column to be updated");
			}
			return column;
		}	
		throw new IllegalArgumentException("Need to define the column to be updated");
	}
	
	private String getRegex() {
		//second input is the regex
		NounMetadata input2 = this.getCurRow().getNoun(1);
		String regex = input2.getValue() + ""; 
		return regex;
	}
	
	private String getNewValue() {
		//third input is the new value
		NounMetadata input3 = this.getCurRow().getNoun(2);
		String newValue = input3.getValue() + ""; 
		return newValue;
	}

}
