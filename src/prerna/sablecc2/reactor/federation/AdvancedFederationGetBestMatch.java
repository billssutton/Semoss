package prerna.sablecc2.reactor.federation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import prerna.algorithm.api.SemossDataType;
import prerna.ds.r.RDataTable;
import prerna.engine.api.IEngine;
import prerna.engine.api.IRawSelectWrapper;
import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.frame.r.AbstractRFrameReactor;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class AdvancedFederationGetBestMatch extends AbstractRFrameReactor {
	public static final String FRAME_COLUMN = "frameCol";	
	
	public AdvancedFederationGetBestMatch() {
		this.keysToGet = new String[] {ReactorKeysEnum.APP.getKey(), ReactorKeysEnum.CONCEPT.getKey(), ReactorKeysEnum.COLUMN.getKey(), FRAME_COLUMN};
	}

	@Override
	public NounMetadata execute() {
		init();
		organizeKeys();
		String baseFolder = DIHelper.getInstance().getProperty("BaseFolder");
		// 4 column results df with matches, distance, and combined column
		final String matchesFrame = Utility.getRandomString(8) + "adFed";
		// 1 column df of all data in frame join column
		final String rCol1 = matchesFrame + "col1";
		// 1 column df of all data in the incoming join column
		final String rCol2 = matchesFrame + "col2";

		// check if packages are installed
		String[] packages = { "stringdist", "data.table" };
		this.rJavaTranslator.checkPackages(packages);

		// for the first iteration we have to build the inputs, second iteration
		// we already have them
		String newDb = this.keyValue.get(this.keysToGet[0]);
		String newTable = this.keyValue.get(this.keysToGet[1]);
		String newCol = this.keyValue.get(this.keysToGet[2]);
		String frameCol = this.keyValue.get(this.keysToGet[3]);

		// accept input info, generate matches table
		IEngine newColEngine = Utility.getEngine(newDb);
		RDataTable frame = (RDataTable) getFrame();
		String frameName = frame.getTableName();
		String rTable1 = rCol1 + " <- as.character(" + frameName + "$" + frameCol + ");";

		// create script to generate col2 from table to be joined
		QueryStruct2 qs = new QueryStruct2();
		qs.setEngine(newColEngine);
		QueryColumnSelector selector = new QueryColumnSelector();
		selector.setTable(newTable);
		selector.setColumn(newCol);
		qs.addSelector(selector);

		IRawSelectWrapper it2 = WrapperManager.getInstance().getRawWrapper(newColEngine, qs);
		
		Map typesMap = new HashMap<String, SemossDataType>();
		String conceptDataType = MasterDatabaseUtility.getBasicDataType(newDb, newCol, newTable);
		SemossDataType semossType = SemossDataType.convertStringToDataType(conceptDataType);
		typesMap.put(newCol, semossType);
		String newFileLoc = DIHelper.getInstance().getProperty(Constants.INSIGHT_CACHE_DIR) + "/"
				+ Utility.getRandomString(6) + ".tsv";
		File newFile = Utility.writeResultToFile(newFileLoc, it2, typesMap, "\t");
		String loadFileRScript = rCol2 + " <- fread(\"" + newFile.getAbsolutePath().replace("\\", "/")
				+ "\", sep=\"\t\");";
		this.rJavaTranslator.runR(loadFileRScript);
		this.rJavaTranslator.runR(rCol2 + " <- as.character(" + rCol2 + "$" + newCol + ")");
		newFile.delete();
		
		// execute the scripts
		this.rJavaTranslator.executeR(rTable1);

		// generate script based on what george wants - empty list of selected
		String bestMatchScript = "source(\"" + baseFolder + "\\R\\Recommendations\\advanced_federation_blend.r\") ; "
				+ matchesFrame + " <- best_match(" + rCol1 + "," + rCol2 + ");";
		bestMatchScript = bestMatchScript.replace("\\", "/");

		this.rJavaTranslator.runR(bestMatchScript);

		// add a unique combined col1 == col2, remove extra columns,
		String combineScript = matchesFrame + "$distance <- as.numeric(" + matchesFrame + "$dist);" + matchesFrame
				+ "<-" + matchesFrame + "[,c(\"col1\",\"col2\",\"distance\")]; " + matchesFrame + "<-" + matchesFrame
				+ "[order(unique(" + matchesFrame + ")$distance),] ;";

		this.rJavaTranslator.runR(combineScript + matchesFrame + " <- as.data.table(" + matchesFrame + ");");

		// remove all garbage 
		this.rJavaTranslator.runR("rm(" + rCol1 + "," + rCol2 + ")");
		
		RDataTable returnTable = createFrameFromVaraible(matchesFrame);
		NounMetadata retNoun = new NounMetadata(returnTable, PixelDataType.FRAME);
		
		// get count of exact matches
		String exactMatchCount = this.rJavaTranslator.getString("as.character(nrow(" + matchesFrame + "[" + matchesFrame + "$distance == 0,]))");
		if (exactMatchCount != null){
			int val = Integer.parseInt(exactMatchCount);
			retNoun.addAdditionalReturn(new NounMetadata(val, PixelDataType.CONST_INT));
		} else{
			throw new IllegalArgumentException("No matches found.");
		}
		
		this.insight.getVarStore().put(matchesFrame, retNoun);
		return retNoun;
	}

	///////////////////////// KEYS /////////////////////////////////////

	@Override
	protected String getDescriptionForKey(String key) {
		if (key.equals(FRAME_COLUMN)) {
			return "The column from the existing frame to join on.";
		} else {
			return super.getDescriptionForKey(key);
		}
	}
}
