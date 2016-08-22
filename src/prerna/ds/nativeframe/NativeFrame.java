package prerna.ds.nativeframe;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.api.IMatcher;
import prerna.algorithm.api.IMetaData;
import prerna.algorithm.api.ITableDataFrame;
import prerna.algorithm.api.IMetaData.DATA_TYPES;
import prerna.ds.AbstractTableDataFrame;
import prerna.ds.TinkerMetaData;
import prerna.ds.H2.H2Builder;
import prerna.sablecc.PKQLEnum;
import prerna.ui.components.playsheets.datamakers.DataMakerComponent;

public class NativeFrame extends AbstractTableDataFrame {

	private static final Logger LOGGER = LogManager.getLogger(NativeFrame.class.getName());
	NativeFrameBuilder builder;

	public NativeFrame() {
		this.metaData = new TinkerMetaData();
		this.builder = new NativeFrameBuilder();
	}

	// added as a path to get connection url for current dataframe
	public NativeFrameBuilder getBuilder() {
		return this.builder;
	}

	public void setConnection(String engineName) {
		this.builder.setConnection(engineName);
		Connection connection = this.builder.getConnection();
		
		if (connection != null) {
			try {
				// working with Mairiadb
				Statement stmt = connection.createStatement();
				String query = "select * from director";
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
				 System.out.print(rs.toString());
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}



	@Override
	public Integer getUniqueInstanceCount(String columnHeader) {
		return null;
	}

	@Override
	public Double getMax(String columnHeader) {
		return null;
	}

	@Override
	public Double getMin(String columnHeader) {
		return null;
	}

	@Override
	public Iterator<Object[]> iterator(boolean getRawData) {
		return null;
	}

	@Override
	public Iterator<Object[]> iterator(boolean getRawData, Map<String, Object> options) {
		return null;
	}

	@Override
	public Iterator<List<Object[]>> scaledUniqueIterator(String columnHeader, boolean getRawData, Map<String, Object> options) {
		return null;
	}

	@Override
	public Iterator<Object> uniqueValueIterator(String columnHeader, boolean getRawData, boolean iterateAll) {
		return null;
	}

	@Override
	public Double[] getColumnAsNumeric(String columnHeader) {
		return null;
	}





	/**
	 * String columnHeader - the column on which to filter on filterValues - the
	 * values that will remain in the
	 */
	@Override
	public void filter(String columnHeader, List<Object> filterValues) {
		if (filterValues != null && filterValues.size() > 0) {
			this.metaData.setFiltered(columnHeader, true);
			builder.setFilters(columnHeader, filterValues, H2Builder.Comparator.EQUAL);
		}
	}
	
	@Override
	public void filter(String columnHeader, Map<String, List<Object>> filterValues) {
		if(columnHeader == null || filterValues == null) return;

		DATA_TYPES type = this.metaData.getDataType(columnHeader);
		boolean isOrdinal = type != null && (type == DATA_TYPES.DATE || type == DATA_TYPES.NUMBER);


		String[] comparators = filterValues.keySet().toArray(new String[]{});
		for(int i = 0; i < comparators.length; i++) {
			String comparator = comparators[i];
			boolean override = i == 0;
			List<Object> filters = filterValues.get(comparator);

			comparator = comparator.trim();
			if(comparator.equals("=")) {

				if(override) builder.setFilters(columnHeader, filters, H2Builder.Comparator.EQUAL);
				else builder.addFilters(columnHeader, filters, H2Builder.Comparator.EQUAL);

			} else if(comparator.equals("!=")) { 

				if(override) builder.setFilters(columnHeader, filters, H2Builder.Comparator.NOT_EQUAL);
				else builder.addFilters(columnHeader, filters, H2Builder.Comparator.NOT_EQUAL);

			} else if(comparator.equals("<")) {

				if(isOrdinal) {

					if(override) builder.setFilters(columnHeader, filters, H2Builder.Comparator.LESS_THAN);
					else builder.addFilters(columnHeader, filters, H2Builder.Comparator.LESS_THAN);

				} else {
					throw new IllegalArgumentException(columnHeader
							+ " is not a numeric column, cannot use operator "
							+ comparator);
				}

			} else if(comparator.equals(">")) {

				if(isOrdinal) {

					if(override) builder.setFilters(columnHeader, filters, H2Builder.Comparator.GREATER_THAN);
					else builder.addFilters(columnHeader, filters, H2Builder.Comparator.GREATER_THAN);

				} else {
					throw new IllegalArgumentException(columnHeader
							+ " is not a numeric column, cannot use operator "
							+ comparator);
				}

			} else if(comparator.equals("<=")) {
				if(isOrdinal) {

					if(override) builder.setFilters(columnHeader, filters, H2Builder.Comparator.LESS_THAN_EQUAL);
					else builder.addFilters(columnHeader, filters, H2Builder.Comparator.LESS_THAN_EQUAL);

				} else {
					throw new IllegalArgumentException(columnHeader
							+ " is not a numeric column, cannot use operator "
							+ comparator);
				}
			} else if(comparator.equals(">=")) {
				if(isOrdinal) {

					if(override) builder.setFilters(columnHeader, filters, H2Builder.Comparator.GREATER_THAN_EQUAL);
					else builder.addFilters(columnHeader, filters, H2Builder.Comparator.GREATER_THAN_EQUAL);

				} else {
					throw new IllegalArgumentException(columnHeader
							+ " is not a numeric column, cannot use operator "
							+ comparator);
				}
			} else {
				// comparator not recognized...do equal by default? or do
				// nothing? or throw error?
			}
			this.metaData.setFiltered(columnHeader, true);
		}
	}

	@Override
	public void unfilter(String columnHeader) {
		this.metaData.setFiltered(columnHeader, false);
		builder.removeFilter(columnHeader);
	}

	@Override
	public void unfilter() {
		builder.clearFilters();
	}

	@Override
	public Object[] getFilterModel() {
		List<String> selectors = this.getSelectors();
		int length = selectors.size();
		Map<String, List<Object>> filteredValues = new HashMap<String, List<Object>>(length);
		Map<String, List<Object>> visibleValues = new HashMap<String, List<Object>>(length);
		Map<String, Map<String, Double>> minMaxValues = new HashMap<String, Map<String, Double>>(length);
		Iterator<Object[]> iterator = this.iterator(true);

		// put instances into sets to remove duplicates
		Set<Object>[] columnSets = new HashSet[length];
		for (int i = 0; i < length; i++) {
			columnSets[i] = new HashSet<Object>();
		}
		while (iterator.hasNext()) {
			Object[] nextRow = iterator.next();
			for (int i = 0; i < length; i++) {
				columnSets[i].add(nextRow[i]);
			}
		}

		//TODO: is this the same as filteredValues object?
		Map<String, List<Object>> h2filteredValues = builder.getFilteredValues(getSelectors());

		for (int i = 0; i < length; i++) {
			// get filtered values
			String h2key = selectors.get(i);//H2Builder.cleanHeader(selectors.get(i));
			List<Object> values = h2filteredValues.get(h2key);
			if (values != null) {
				filteredValues.put(selectors.get(i), values);
			} else {
			filteredValues.put(selectors.get(i), new ArrayList<Object>());
			}

			// get unfiltered values
			ArrayList<Object> unfilteredList = new ArrayList<Object>(columnSets[i]);
			visibleValues.put(selectors.get(i), unfilteredList);

			// store data type for header
			// get min and max values for numerical columns
			// TODO: need to include date type
			if(this.metaData.getDataType(selectors.get(i)) == IMetaData.DATA_TYPES.NUMBER) {
				Map<String, Double> minMax = new HashMap<String, Double>();

				// sort unfiltered array to pull relative min and max of unfiltered data
				Object[] unfilteredArray = unfilteredList.toArray();
				Arrays.sort(unfilteredArray);
				double absMin = getMin(selectors.get(i));
				double absMax = getMax(selectors.get(i));
				if(!unfilteredList.isEmpty()) {
					minMax.put("min", (Double)unfilteredArray[0]);
					minMax.put("max", (Double)unfilteredArray[unfilteredArray.length-1]);
				}
				minMax.put("absMin", absMin);
				minMax.put("absMax", absMax);

				// calculate how large each step in the slider should be
				double difference = absMax - absMin;
				double step = 1;
				if(difference < 1) {
					double tenthPower = Math.floor(Math.log10(difference));
					if(tenthPower < 0) {
						// ex. if difference is 0.009, step should be 0.001
						step = Math.pow(10, tenthPower);
					} else {
						step = 0.1;
					}
				}
				minMax.put("step", step);

				minMaxValues.put(selectors.get(i), minMax);
			}
		}

		return new Object[] { visibleValues, filteredValues, minMaxValues };
	}


	@Override
	public String getDataMakerName() {
		return "NativeFrame";
	}

	@Override
	public Map<String, String> getScriptReactors() {
		Map<String, String> reactorNames = super.getScriptReactors();
		reactorNames.put(PKQLEnum.DATA_CONNECTDB, "prerna.sablecc.DataConnectDBReactor");
		reactorNames.put(PKQLEnum.DATA_FRAME, "prerna.sablecc.DataFrameReactor");
		reactorNames.put(PKQLEnum.API, "prerna.sablecc.NativeApiReactor");
		reactorNames.put(PKQLEnum.IMPORT_DATA, "prerna.sablecc.NativeImportDataReactor");

		return reactorNames;
	}
	
	public void createView(String selectQuery) {
		selectQuery = selectQuery.trim().toUpperCase();
		if(!selectQuery.startsWith("SELECT")) {
			throw new IllegalArgumentException("Query must be a 'SELECT' query");
		}
		String viewTable = this.builder.getNewTableName();
		selectQuery = "("+selectQuery+")";
		selectQuery = "CREATE OR REPLACE VIEW AS "+viewTable+selectQuery;
		builder.setView(viewTable);
	}
	
	/******************************* UNNECESSARY ON NATIVE FRAME FOR NOW BUT NEED TO OVERRIDE FOR NOW *************************************************/
	
	@Override
	public void processDataMakerComponent(DataMakerComponent component) {
	}
	
	@Override
	public void save(String fileName) {
	}

	@Override
	public ITableDataFrame open(String fileName, String userId) {
		return null;
	}

	@Override
	public void addRelationship(Map<String, Object> cleanRow, Map<String, Object> rawRow) {
	}

	@Override
	public void removeRelationship(Map<String, Object> cleanRow, Map<String, Object> rawRow) {
	}

	@Override
	public void addRelationship(Map<String, Object> rowCleanData, Map<String, Object> rowRawData, Map<String, Set<String>> edgeHash, Map<String, String> logicalToValMap) {
	}

	@Override
	public Map<String, Object[]> getFilterTransformationValues() {
		return null;
	}

	@Override
	public void removeColumn(String columnHeader) {
	}
	
	@Override
	public void addRow(Object[] rowCleanData, Object[] rowRawData) {
	}

	@Override
	public void addRow(Object[] cleanCells, Object[] rawCells, String[] headers) {
	}

	@Override
	public void addRelationship(String[] headers, Object[] values, Object[] rawValues, Map<Integer, Set<Integer>> cardinality, Map<String, String> logicalToValMap) {
	}

	@Override
	public void join(ITableDataFrame table, String colNameInTable, String colNameInJoiningTable, double confidenceThreshold, IMatcher routine) {
	}
}
