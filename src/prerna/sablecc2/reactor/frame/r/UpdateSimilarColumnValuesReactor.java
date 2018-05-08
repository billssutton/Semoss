package prerna.sablecc2.reactor.frame.r;

import java.util.List;
import java.util.Map;

import prerna.algorithm.api.SemossDataType;
import prerna.ds.OwlTemporalEngineMeta;
import prerna.ds.r.RDataTable;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Utility;

public class UpdateSimilarColumnValuesReactor extends AbstractRFrameReactor {

	public static final String MATCHES = "matches";
	public static final String MATCHES_TABLE = "matchesTable";

	public UpdateSimilarColumnValuesReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.COLUMN.getKey(), MATCHES_TABLE, MATCHES };
	}

	@Override
	public NounMetadata execute() {
		init();
		organizeKeys();
		String column = this.keyValue.get(this.keysToGet[0]);
		String matchesTable = this.keyValue.get(this.keysToGet[1]);

		// check if packages are installed
		String[] packages = { "stringdist", "data.table" };
		this.rJavaTranslator.checkPackages(packages);

		// get single column input
		String linkFrame = "link" + Utility.getRandomString(5);
		RDataTable frame = (RDataTable) getFrame();
		String frameName = frame.getTableName();
		String col1 = matchesTable + "col1";
		this.rJavaTranslator.runR(col1 + "<- as.character(" + frameName + "$" + column + ");");

		// get data type and account for numerics

		// iterate matches and create the link frame
		List<String> allMatches = getInputList(MATCHES);
		// add all matches
		if (allMatches != null && !(allMatches.isEmpty())) {
			StringBuilder col1Builder = new StringBuilder();
			StringBuilder col2Builder = new StringBuilder();
			StringBuilder col3Builder = new StringBuilder();
			for (int i = 0; i < allMatches.size(); i++) {
				if (i != 0) {
					col1Builder.append(",");
					col2Builder.append(",");
					col3Builder.append(",");
				}
				String match = (String) allMatches.get(i);
				String[] matchList = match.split(" == ");
				if (matchList.length > 2) {
					throw new IllegalArgumentException("match seperator didnt work");
				}
				String column1 = matchList[0];
				String column2 = matchList[1];
				col1Builder.append("\"" + column1 + "\"");
				col2Builder.append("\"" + column2 + "\"");
				col3Builder.append("1");
			}
			// add all matches provided
			String script = linkFrame + " <- data.table(\"col1\"=c(" + col1Builder + "), \"col2\"=c(" + col2Builder
					+ ")); ";
			this.rJavaTranslator.runR(script);
		}
		// make linkframe unique
		this.rJavaTranslator.runR(linkFrame + " <- unique(" + linkFrame + ");");

		// get current frame data type
		boolean convertJoinColFromNum = false;
		OwlTemporalEngineMeta metaData = this.getFrame().getMetaData();
		Map<String, SemossDataType> typeMap = metaData.getHeaderToTypeMap();
		SemossDataType dataType = typeMap.get(column);
		if (dataType == SemossDataType.DOUBLE || dataType == SemossDataType.INT) {
			convertJoinColFromNum = true;
		}

		// call the curate script
		String resultFrame = Utility.getRandomString(8);
		this.rJavaTranslator.runR(resultFrame + "<- curate(" + col1 + "," + linkFrame + ");");

		// delete column from frame
		this.rJavaTranslator.runR(frameName + " <- " + frameName + "[,-c(\"" + column + "\")]");

		// make resultFrame a DT and put column header back
		this.rJavaTranslator.runR(resultFrame + " <- as.data.table(" + resultFrame + ");" + "names(" + resultFrame + ")<-\"" + column + "\";");

		// rbind add new dataframe to frame
		this.rJavaTranslator.runR(frameName + " <- cbind(" + frameName + "," + resultFrame + ");");

		// return data type to original state
		if (convertJoinColFromNum) {
			this.rJavaTranslator.runR(frameName + "$" + column + " <- as.numeric(as.character(" + frameName + "$" + column + "));");
		}

		NounMetadata retNoun = new NounMetadata(frame, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE);

		return retNoun;
	}

	private List<String> getInputList(String key) {
		// see if defined as individual key
		GenRowStruct columnGrs = this.store.getNoun(key);
		if (columnGrs != null) {
			if (columnGrs.size() > 0) {
				List<String> values = columnGrs.getAllStrValues();
				return values;
			}
		}
		// else, we assume it is values in the curRow
		List<String> values = this.curRow.getAllStrValues();
		return values;
	}
}
