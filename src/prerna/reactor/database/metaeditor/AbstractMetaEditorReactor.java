package prerna.reactor.database.metaeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.vocabulary.OWL;
import org.apache.logging.log4j.Logger;
import org.semarglproject.vocab.RDFS;

import prerna.ds.r.RDataTable;
import prerna.ds.r.RSyntaxHelper;
import prerna.engine.api.IDatabaseEngine;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.impl.owl.AbstractOWLEngine;
import prerna.engine.impl.owl.WriteOWLEngine;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.query.querystruct.filters.SimpleQueryFilter;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.query.querystruct.selectors.QueryFunctionSelector;
import prerna.reactor.AbstractReactor;
import prerna.reactor.frame.r.util.IRJavaTranslator;
import prerna.reactor.imports.ImportUtility;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Utility;

public abstract class AbstractMetaEditorReactor extends AbstractReactor {

	protected static Set<String> literalPreds = new HashSet<String>();
	static {
		literalPreds.add(RDFS.LABEL);
		literalPreds.add(RDFS.DOMAIN);
		literalPreds.add(OWL.sameAs.toString());
		literalPreds.add(RDFS.COMMENT);
		literalPreds.add(AbstractOWLEngine.SEMOSS_URI_PREFIX + AbstractOWLEngine.DEFAULT_PROP_CLASS + "/UNIQUE");
		literalPreds.add(AbstractOWLEngine.CONCEPTUAL_RELATION_URI);
	}

	protected static final String CONCEPTUAL_NAME = "conceptual";
	protected static final String TABLES_FILTER = ReactorKeysEnum.TABLES.getKey();
	protected static final String STORE_VALUES_FRAME = "store";

	/**
	 * Get the base folder
	 * 
	 * @return
	 */
	protected String getBaseFolder() {
		String baseFolder = null;
		try {
			baseFolder = Utility.getBaseFolder();
		} catch (Exception ignored) {
			// logger.info("No BaseFolder detected... most likely running as test...");
		}
		return baseFolder;
	}

	/**
	 * Execute a remove statement based on if the object is a literal or not
	 * 
	 * @param headerRows
	 * @param owlEngine
	 */
	protected void executeRemoveQuery(IHeadersDataRow headerRows, WriteOWLEngine owlEngine) {
		Object[] raw = headerRows.getRawValues();
		String s = raw[0].toString();
		String p = raw[1].toString();
		String o = raw[2].toString();
		boolean isLiteral = objectIsLiteral(p);
		if (isLiteral) {
			owlEngine.removeFromBaseEngine(new Object[] { s, p, headerRows.getValues()[2], !isLiteral });
		} else {
			owlEngine.removeFromBaseEngine(new Object[] { s, p, o, !isLiteral });
		}
	}

	/**
	 * Get a list of tables to run certain routines
	 * 
	 * @return
	 */
	protected List<String> getTableFilters() {
		List<String> filters = new ArrayList<>();
		GenRowStruct grs = this.store.getNoun(TABLES_FILTER);
		if (grs != null && !grs.isEmpty()) {
			for (int i = 0; i < grs.size(); i++) {
				filters.add(grs.get(i).toString());
			}
		}

		if (filters.size() == 1) {
			throw new IllegalArgumentException("Must define at least 2 tables");
		}

		return filters;
	}

	/**
	 * Get an array of lists The first list contains the tables The second list
	 * contains the column But the first list table will repeat for each column so
	 * that they match based on index
	 */
	protected List<String>[] getTablesAndColumnsList(IDatabaseEngine database, List<String> tableFilters) {
		// store 2 lists
		// of all table names
		// and column names
		// matched by index
		List<String> tableNamesList = new ArrayList<>();
		List<String> columnNamesList = new ArrayList<>();

		List<String> concepts = database.getPhysicalConcepts();
		for (String cUri : concepts) {
			String tableName = Utility.getInstanceName(cUri);

			// if this is empty
			// no filters have been defined
			if (!tableFilters.isEmpty()) {
				// now if the table isn't included
				// ignore it
				if (!tableFilters.contains(tableName)) {
					continue;
				}
			}
			// grab all the properties
			List<String> properties = database.getPropertyUris4PhysicalUri(cUri);
			for (String pUri : properties) {
				tableNamesList.add(tableName);
				columnNamesList.add(Utility.getClassName(pUri));
			}
		}

		return new List[] { tableNamesList, columnNamesList };
	}

	/**
	 * Generate a query struct to query a single column ignoring empty values
	 * 
	 * @param qsName
	 * @param limit
	 * @return
	 */
	protected SelectQueryStruct getMostOccuringSingleColumnNonEmptyQs(String qsName, int limit) {
		SelectQueryStruct qs = new SelectQueryStruct();
		qs.addSelector(new QueryColumnSelector(qsName));
		QueryFunctionSelector cSelector = new QueryFunctionSelector();
		cSelector.addInnerSelector(new QueryColumnSelector(qsName));
		cSelector.setFunction("count");
		cSelector.setAlias("OccuranceCountForColumn");
		qs.addSelector(cSelector);
		// order
		qs.addOrderBy("OccuranceCountForColumn", "desc");
		// group
		qs.addGroupBy(new QueryColumnSelector(qsName));
		qs.setLimit(limit);

		{
			NounMetadata lComparison = new NounMetadata(new QueryColumnSelector(qsName), PixelDataType.COLUMN);
			NounMetadata rComparison = new NounMetadata(null, PixelDataType.NULL_VALUE);
			SimpleQueryFilter f = new SimpleQueryFilter(lComparison, "!=", rComparison);
			qs.addExplicitFilter(f);
		}
		{
			NounMetadata lComparison = new NounMetadata(new QueryColumnSelector(qsName), PixelDataType.COLUMN);
			NounMetadata rComparison = new NounMetadata("", PixelDataType.CONST_STRING);
			SimpleQueryFilter f = new SimpleQueryFilter(lComparison, "!=", rComparison);
			qs.addExplicitFilter(f);
		}

		return qs;
	}

	/**
	 * Get the frame we are using to store existing results
	 * 
	 * @return
	 */
	protected RDataTable getStore() {
		GenRowStruct grs = this.store.getNoun(STORE_VALUES_FRAME);
		if (grs != null && !grs.isEmpty()) {
			NounMetadata noun = grs.getNoun(0);
			if (noun.getNounType() == PixelDataType.FRAME) {
				return (RDataTable) noun.getValue();
			}
		}

		return null;
	}

	/**
	 * Generate an R vector for a constant value
	 * 
	 * @param value
	 * @param size
	 * @return
	 */
	protected String getRColumnOfSameValue(String value, int size) {
		if (size <= 0) {
			return "c()";
		}
		StringBuilder b = new StringBuilder("c(");
		b.append("\"").append(value).append("\"");
		for (int i = 1; i < size; i++) {
			b.append(",\"").append(value).append("\"");
		}
		b.append(")");
		return b.toString();
	}

	/**
	 * Store user input results based on the input values
	 * 
	 * @param logger
	 * @param startTList
	 * @param startCList
	 * @param endTList
	 * @param endCList
	 */
	protected void storeUserInputs(Logger logger, List<String> startTList, List<String> startCList,
			List<String> endTList, List<String> endCList, String action) {
		RDataTable storeFrame = getStore();
		boolean storeResults = (storeFrame != null);
		if (storeResults) {
			IRJavaTranslator rJavaTranslator = this.insight.getRJavaTranslator(logger);
			rJavaTranslator.startR();

			StringBuilder tableCreationBuilder = new StringBuilder();
			tableCreationBuilder.append("data.table(").append(RSyntaxHelper.createStringRColVec(startTList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(startCList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(endTList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(endCList)).append(",")
					.append(getRColumnOfSameValue(action, startTList.size())).append(");");

			execQueryStore(storeFrame, rJavaTranslator, tableCreationBuilder);
		}
	}

	/**
	 * Store user input results based on the input values
	 * 
	 * @param logger
	 * @param startTList
	 * @param startCList
	 * @param endTList
	 * @param endCList
	 */
	protected void storeUserInputs(Logger logger, List<String> startTList, List<String> startCList,
			List<String> endTList, List<String> endCList, List<String> actionList) {
		RDataTable storeFrame = getStore();
		boolean storeResults = (storeFrame != null);
		if (storeResults) {
			IRJavaTranslator rJavaTranslator = this.insight.getRJavaTranslator(logger);
			rJavaTranslator.startR();

			StringBuilder tableCreationBuilder = new StringBuilder();
			tableCreationBuilder.append("data.table(").append(RSyntaxHelper.createStringRColVec(startTList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(startCList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(endTList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(endCList)).append(",")
					.append(RSyntaxHelper.createStringRColVec(actionList)).append(");");

			execQueryStore(storeFrame, rJavaTranslator, tableCreationBuilder);
		}
	}

	/**
	 * Store the user entered edits
	 * 
	 * @param storeFrame
	 * @param rJavaTranslator
	 * @param newValuesBuilder
	 * @param randomVar
	 */
	private void execQueryStore(RDataTable storeFrame, IRJavaTranslator rJavaTranslator,
			StringBuilder newValuesBuilder) {
		String frameName = storeFrame.getName();
		if (storeFrame.isEmpty()) {
			// frame has not been set
			// we will override it
			rJavaTranslator.runR(frameName + "<-" + newValuesBuilder.toString());
			rJavaTranslator.runR("names(" + frameName + ")<-" + RSyntaxHelper.createStringRColVec(
					new String[] { "sourceTable", "sourceCol", "targetTable", "targetCol", "action" }));
			ImportUtility.parseTableColumnsAndTypesToFlatTable(storeFrame.getMetaData(),
					new String[] { "sourceTable", "sourceCol", "targetTable", "targetCol", "action" },
					new String[] { "STRING", "STRING", "STRING", "STRING", "STRING" }, frameName);
		} else {
			// note, string builder already ends with ";"
			// do a union
			// remove the random var
			String randomVar = "storeDataFrame_" + Utility.getRandomString(6);
			String rename = "names(" + randomVar + ")<-" + RSyntaxHelper.createStringRColVec(
					new String[] { "sourceTable", "sourceCol", "targetTable", "targetCol", "action" });
			rJavaTranslator.runR(randomVar + "<-" + newValuesBuilder.toString() + ";" + rename + ";" 
					+ storeFrame.getName() + "<-funion(" + frameName + "," + randomVar + ");rm(" + randomVar + ");");
		}
	}

	/**
	 * Removed previously stored values
	 * 
	 * @param resultsFrame
	 * @param logger
	 */
	protected void removeStoredValues(String resultsFrame, Object[] storeTypesToRemove, Logger logger) {
		RDataTable storeFrame = getStore();
		boolean storeResults = (storeFrame != null);
		if (storeResults) {
			logger.info("Removing previously mastered data from the results...");
			String storeFrameName = storeFrame.getName();
			String filter = RSyntaxHelper.createStringRColVec(storeTypesToRemove);

			String subsetVar = "subset_" + Utility.getRandomString(6);
//			String indexVar = "indices_" + Utility.getRandomString(6);

			IRJavaTranslator rJavaTranslator = this.insight.getRJavaTranslator(logger);
			rJavaTranslator.startR();

			String script = subsetVar + "<-" + storeFrameName + "[" + storeFrameName + "$action %in% " + filter + "];"
//					+ indexVar + "<- !("
//					+ "match(" + resultsFrame + "$sourceTable," + subsetVar + "$sourceTable) "
//					+ "& match(" + resultsFrame + "$sourceCol," + subsetVar + "$sourceCol) "
//					+ "& match(" + resultsFrame + "$targetTable," + subsetVar + "$targetTable) "
//					+ "& match(" + resultsFrame + "$targetCol," + subsetVar + "$targetCol));"
					+ resultsFrame + "<-" + resultsFrame + "[ !( " + resultsFrame + "$sourceTable != " + subsetVar
					+ "$sourceTable " + "& " + resultsFrame + "$sourceCol != " + subsetVar + "$sourceCol " + "& "
					+ resultsFrame + "$targetTable != " + subsetVar + "$targetTable " + "& " + resultsFrame
					+ "$targetCol != " + subsetVar + "$targetCol " + ") ];" + "gc(" + subsetVar + ");";//," + indexVar + ");";
			rJavaTranslator.runR(script);

			logger.info("Finsihed removing previously mastered data from the results");
		}
	}

	/**
	 * Determine if the predicate points to a literal
	 * 
	 * @param predicate
	 * @return
	 */
	protected boolean objectIsLiteral(String predicate) {
		return literalPreds.contains(predicate);
	}

	/**
	 * Get the top n most occurring values
	 * 
	 * @param results
	 * @return
	 */
	protected static List<String> getTopNResults(List<String> results, int n) {
		// get the frequency
		Map<String, Integer> freqMap = new HashMap<>();
		for (String value : results) {
			Integer freq = freqMap.get(value);
			freqMap.put(value, (freq == null) ? 1 : freq + 1);
		}

		// now loop through and store the top N
		boolean init = true;
		String minValue = null;
		int minFreq = 0;
		Map<String, Integer> topN = new HashMap<>(n);
		for (String value : freqMap.keySet()) {
			int freq = freqMap.get(value);
			if (topN.keySet().size() < n) {
				// we just add
				topN.put(value, freq);
				// keep track of lowest
				if (init) {
					minValue = value;
					minFreq = freq;
					init = false;
				} else {
					if (minFreq < freq) {
						// this is a new low...
						// even for you!
						minFreq = freq;
						minValue = value;
					}
				}
			} else {
				// we have the max # of positions filled
				// need to do substitutions
				if (freq > minFreq) {
					// occurred more times
					// going to sub you in
					topN.remove(minValue);
					topN.put(value, freq);

					// reset the values
					// need to determine what the new lowest min is
					String findNewMinValue = null;
					int findNewMinFreq = 0;
					for (String minV : topN.keySet()) {
						int minF = topN.get(minV);
						if (findNewMinFreq == 0 || minF < findNewMinFreq) {
							findNewMinValue = minV;
							findNewMinFreq = minF;
						}
					}
					minFreq = findNewMinFreq;
					minValue = findNewMinValue;
				} else if (freq == minFreq) {
					// let us compare to get the thing that has the least # of characters
					if (minValue != null && minValue.length() > value.length()) {
						// the new input has less words
						// lets go with that instead
						topN.remove(minValue);
						topN.put(value, freq);

						// reset the values
						// need to determine what the new lowest min is
						String findNewMinValue = null;
						int findNewMinFreq = 0;
						for (String minV : topN.keySet()) {
							int minF = topN.get(minV);
							if (findNewMinFreq == 0 || minF < findNewMinFreq) {
								findNewMinValue = minV;
								findNewMinFreq = minF;
							}
						}
						minFreq = findNewMinFreq;
						minValue = findNewMinValue;
					}
				}
			}
		}

		List<String> sortedTopN = topN.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(p -> p.getKey())
				.collect(Collectors.toList());

		return sortedTopN;
	}
}
