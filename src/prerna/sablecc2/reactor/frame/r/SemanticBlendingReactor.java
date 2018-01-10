package prerna.sablecc2.reactor.frame.r;

import org.apache.log4j.Logger;
import org.rosuda.REngine.Rserve.RConnection;
import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.OwlTemporalEngineMeta;
import prerna.ds.r.RDataTable;
import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.util.Constants;
import prerna.util.Utility;

public class SemanticBlendingReactor extends AbstractRFrameReactor {

	/**
	 * This reactor runs the semantic blending routine on a given column and flushes the results out as a table
	 * The inputs to the reactor are: 
	 * 1) the columns
	 * 2) the number of results to be displayed (defaults to 3 if none is entered)
	 * 3) the number of random values to use in the routine (defaults to 20 if none is entered)
	 * 4) boolean indicator if we want to create an r data table, otherwise just return table of results; true indicates widget is used; defaults to false
	 * 5) name for r data table, if one is to be created
	 */
	
	private static final String CLASS_NAME = SemanticBlendingReactor.class.getName();
	
	// keys used to retrieve user input
	// determine whether using semantic blending or widget
	// default to false
	private static final String GENERATE_FRAME = "genFrame";
	private static final String FRAME_NAME = "frameName";
	
	public SemanticBlendingReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.COLUMNS.getKey(), ReactorKeysEnum.NUM_DISPLAY.getKey(), ReactorKeysEnum.RANDOM_VALS.getKey(), GENERATE_FRAME, FRAME_NAME};
	}

	@Override
	public NounMetadata execute() {
		Logger logger = getLogger(CLASS_NAME);
		// initialize the rJavaTranslator
		init();
		
		// need to make sure that the WikidataR package is installed before running this method
		String hasPackage = this.rJavaTranslator.getString("as.character(\"WikidataR\" %in% rownames(installed.packages()))");
		if (!hasPackage.equalsIgnoreCase("true")) {
			throw new IllegalArgumentException("The WikidataR package is NOT installed");
		}
				
		// get frame
		ITableDataFrame frame = getFrame();
		
		// we have an input to indicate whether semantic blending or widget is being used
		// we generate an r data frame for the widget
		// for widget, rDataTableIndicator is true
		boolean generateFrameIndicator = getGenerateFrameIndicator();
		
		// get other inputs
		// the first input is the columns
		String[] columns = getColumns();
		
		// get the number of results to display
		String numDisplayString = getNumResults();
				
		// get the number of random values to use in the routine
		String randomValsString = getNumRandomVals();
		
		// build a query struct so that we can query and limit the number of values being passed into the method
		// this will also keep track of the columns
		QueryStruct2 qs = new QueryStruct2();
		qs.setLimit(((Number) Double.parseDouble(randomValsString)).longValue());
		for (int i = 0; i < columns.length; i++) {
			qs.addSelector(new QueryColumnSelector(columns[i]));
		}
		
		//create an r data frame (in r) using this querystruct and get the name of the variable
		String dfName = rJavaTranslator.generateRDataTableVariable(frame, qs);
		logger.info("Done generating random subset");
		
		// this will define the column numbers that we are selecting from our frame to run through the routine
		// the r routine uses column numbers rather than names
		StringBuilder colSelectSb = new StringBuilder("c(");

		// format: c(1,2)
		for (int i = 0; i < columns.length; i++) {
			colSelectSb.append((i + 1) + ",");
		}
		
		//remove the last comma and add an end parentheses
		int remove = colSelectSb.length() - 1;
		String colSelectString = colSelectSb.substring(0, remove) + ")";	

		// construct a new dataframe to hold the results of the r script
		String df2 = "PredictionTable" + Utility.getRandomString(10);
		
		// determine the path and source the script
		String baseRScriptPath = getBaseFolder() + "\\" + Constants.R_BASE_FOLDER + "\\"+ "AnalyticsRoutineScripts";
		String rScriptPath = (baseRScriptPath + "\\" + "master_concept.r").replace("\\", "/");
		String sourceScript = "source(\"" + rScriptPath + "\");";
		
		// run the function
		// function script: PredictionTable<- concept_mgr(frame,c(1,2),1,20);
		String rFunctionScript = df2 + " <- concept_mgr(" + dfName + "," + colSelectString + "," + numDisplayString + "," + randomValsString + ");";
		// results should be in a data frame
		String dataTableScript = df2 + " <- as.data.table(" + df2 + ");";
		// run all of the above r scripts
		logger.info("Running semantic blending script");
	    logger.info("This process may take a few minutes depending on the type of data and internet speed");
		this.rJavaTranslator.runR(sourceScript + rFunctionScript + dataTableScript);
		
		// if we are running semantic blending
		if (!generateFrameIndicator) {
			// these are the column names for the results
			String[] colNames = {"Predicted_Concept", "Prob", "URL"};
			return new NounMetadata(this.rJavaTranslator.flushObjectAsTable(df2, colNames), PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.WIKI_LOGICAL_NAMES);
		} else {
			//we are not running semantic blending; we are running the widget
			// need to make a new r table to store this info so we can later query it
			RDataTable resultsTable = null;
			if (retrieveVariable(this.rJavaTranslator.R_CONN) != null && retrieveVariable(this.rJavaTranslator.R_PORT) != null) {
				resultsTable = new RDataTable(df2, (RConnection) retrieveVariable(this.rJavaTranslator.R_CONN), (String) retrieveVariable(this.rJavaTranslator.R_PORT));
			} else {
				// if we dont have a current r session
				// but when we create the table it makes one
				// store those variables so we end up using that
				resultsTable = new RDataTable(df2);
				if (resultsTable.getConnection() != null && resultsTable.getPort() != null) {
					storeVariable(this.rJavaTranslator.R_CONN, new NounMetadata(resultsTable.getConnection(), PixelDataType.R_CONNECTION));
					storeVariable(this.rJavaTranslator.R_PORT, new NounMetadata(resultsTable.getPort(), PixelDataType.CONST_STRING));
				}
			}
			// create the new frame meta
			OwlTemporalEngineMeta metaData = resultsTable.getMetaData();
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
			NounMetadata frameNoun = new NounMetadata(resultsTable, PixelDataType.FRAME);
			this.storeVariable(getRDataTableName(), frameNoun);
			return frameNoun;
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	///////////////////////// GET PIXEL INPUT ////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	
	private String[] getColumns() {
		GenRowStruct columnGrs = this.store.getNoun(ReactorKeysEnum.COLUMNS.getKey());
		if (columnGrs.size() > 0) {
			String[] columns = new String[columnGrs.size()];
			for (int i = 0; i < columnGrs.size(); i++) {
				String column = columnGrs.get(i).toString();
				columns[i] = column;
			}
			return columns;
		}
		throw new IllegalArgumentException("Need to define column to run semantic blending on");
	}
	
	private String getNumResults() {
		GenRowStruct numDisplayGrs = this.store.getNoun(ReactorKeysEnum.NUM_DISPLAY.getKey());
		if (numDisplayGrs != null) {
			if (numDisplayGrs.size() > 0) {
				return numDisplayGrs.get(0).toString();
			}
		}
		//default to 3
		return "3";
	}
	
	private String getNumRandomVals() {
		GenRowStruct randomValsGrs = this.store.getNoun(ReactorKeysEnum.RANDOM_VALS.getKey());
		if (randomValsGrs != null) {
			if (randomValsGrs.size() > 0) {
				return randomValsGrs.get(0).toString();
			}
		}
		// default to 20
		return "20";
	}
	
	private boolean getGenerateFrameIndicator() {
		// see if we are using semantic blending or widget
		// true indicates to use widget
		// default to false (semantic blending)
		GenRowStruct rGrs = this.store.getNoun(GENERATE_FRAME);
		if (rGrs != null) {
			if (rGrs.size() > 0) {
				return (Boolean)rGrs.get(0);
			}
		}
		return false;
	}
	
	private String getRDataTableName() {
		// only get the RDataFrame name if we have determined that we would like to create an RDataFrame
		GenRowStruct nameGrs = this.store.getNoun(FRAME_NAME);
		if (nameGrs != null) {
			if (nameGrs.size() > 0) {
				return nameGrs.get(0).toString();
			}
		}
		// default to "predictionFrame"
		return "predictionFrame";
	}
	
	///////////////////////// KEYS /////////////////////////////////////

	@Override
	protected String getDescriptionForKey(String key) {
		if (key.equals(GENERATE_FRAME)) {
			return "Boolean indicator of whether an RDataFrame should be created - defaults to false";
		} if (key.equals(FRAME_NAME)) {
			return "The name for the RDataFrame, if one is to be created";
		} else {
			return super.getDescriptionForKey(key);
		}
	}
}