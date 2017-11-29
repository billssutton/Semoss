package prerna.ds.r;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import prerna.algorithm.api.ITableDataFrame;
import prerna.algorithm.api.SemossDataType;
import prerna.ds.shared.AbstractTableDataFrame;
import prerna.engine.api.IHeadersDataRow;
import prerna.query.interpreters.RInterpreter2;
import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.QueryStructConverter;
import prerna.sablecc.PKQLEnum;
import prerna.sablecc.PKQLEnum.PKQLReactor;
import prerna.ui.components.playsheets.datamakers.DataMakerComponent;
import prerna.util.Constants;
import prerna.util.DIHelper;

public class RDataTable extends AbstractTableDataFrame {

	public static final String DATA_MAKER_NAME = "RDataTable";
	
	private AbstractRBuilder builder;
	
	public RDataTable() {
		this(null);
	}
	
	public RDataTable(String rTableVarName) {
		String useJriStr = DIHelper.getInstance().getProperty(Constants.R_CONNECTION_JRI);
		boolean useJri = false;
		if(useJriStr != null) {
			useJri = Boolean.valueOf(useJriStr);
		}
		try {
			if(useJri) {
				this.builder = new RBuilderJRI(rTableVarName);
			} else {
				this.builder = new RBuilder(rTableVarName);
			}
		} catch (RserveException e) {
			e.printStackTrace();
			closeConnection();
			throw new IllegalStateException("Could not create valid connection to R. "
					+ "Please make sure R is installed properly and running on machine.");
		}
	}
	
	public RDataTable(String rTableVarName, RConnection retCon, String port) {
		try {
			this.builder = new RBuilder(rTableVarName, retCon, port);
		} catch (RserveException e) {
			e.printStackTrace();
			closeConnection();
			throw new IllegalStateException("Could not create valid connection to R. "
					+ "Please make sure R is installed properly and running on machine.");
		}
	}
	
	public RConnection getConnection() {
		return this.builder.getConnection();
	}
	
	public String getPort() {
		return this.builder.getPort();
	}
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
		this.builder.setLogger(logger);
	}
	
	public void closeConnection() {
		// now we only hold 1 connection
		// do not do this...
//		if(this.builder.getConnection() != null) {
//			try {
//				this.builder.getConnection().shutdown();
//			} catch (RserveException e) {
//				logger.info("R Connection is already closed...");
//			}
//		}
	}
	
	public String getFilterString() {
		//TODO:
		//TODO:
		return "";
	}
	
	public void addRowsViaIterator(Iterator<IHeadersDataRow> it) {
		// we really need another way to get the data types....
		Map<String, SemossDataType> rawDataTypeMap = this.metaData.getHeaderToTypeMap();
		
		// TODO: this is annoying, need to get the frame on the same page as the meta
		Map<String, SemossDataType> dataTypeMap = new HashMap<String, SemossDataType>();
		for(String rawHeader : rawDataTypeMap.keySet()) {
			dataTypeMap.put(rawHeader.split("__")[1], rawDataTypeMap.get(rawHeader));
		}
		this.addRowsViaIterator(it, this.getTableName(), dataTypeMap);
		syncHeaders();
	}
	
	public void addRowsViaIterator(Iterator<IHeadersDataRow> it, String tableName, Map<String, SemossDataType> dataTypeMap) {
		this.builder.createTableViaIterator(tableName, it, dataTypeMap);
		syncHeaders();
	}
	
	public Object[] getDataRow(String rScript, String[] headerOrdering) {
		return this.builder.getDataRow(rScript, headerOrdering);
	}
	
	public List<Object[]> getBulkDataRow(String rScript, String[] headerOrdering) {
		return this.builder.getBulkDataRow(rScript, headerOrdering);
	}
	
	public Object getScalarValue(String rScript) {
		return this.builder.getScalarReturn(rScript);
	}
	
	public void executeRScript(String rScript) {
		//Validate user input won't break R and crash JVM
		RregexValidator reg = new RregexValidator();
		reg.Validate(rScript);
		
		this.builder.executeR(rScript);
	}
	
	public String[] getColumnNames() {
		return this.builder.getColumnNames();
	}
	
	public String[] getColumnTypes() {
		return this.builder.getColumnTypes();
	}
	
	public String[] getColumnNames(String varName) {
		return this.builder.getColumnNames(varName);
	}
	
	public String[] getColumnTypes(String varName) {
		return this.builder.getColumnTypes(varName);
	}
	
	@Override
	public Iterator<IHeadersDataRow> query(String query) {
		return new RIterator2(builder, query);
	}

	@Override
	public Iterator<IHeadersDataRow> query(QueryStruct2 qs) {
		qs = QueryStructConverter.getPhysicalQs(qs, this.metaData);
		RInterpreter2 interp = new RInterpreter2();
		interp.setQueryStruct(qs);
		interp.setDataTableName(this.getTableName());
		interp.setColDataTypes(this.metaData.getHeaderToTypeMap());
		interp.setLogger(this.logger);
		String query = interp.composeQuery();
		RIterator2 it = new RIterator2(this.builder, query, qs);
		return it;
	}
	
	@Override
	public Iterator<List<Object[]>> scaledUniqueIterator(String columnName, List<String> attributeUniqueHeaderName) {
		int numSelectors = attributeUniqueHeaderName.size();
		List<SemossDataType> dataTypes = new Vector<SemossDataType>();
		Double[] max = new Double[numSelectors];
		Double[] min = new Double[numSelectors];
		
		for (int i = 0; i < numSelectors; i++) {
			String uniqueHeader = this.metaData.getUniqueNameFromAlias(attributeUniqueHeaderName.get(i));
			if(uniqueHeader == null) {
				uniqueHeader = attributeUniqueHeaderName.get(i);
			}
			SemossDataType dataType = this.metaData.getHeaderTypeAsEnum(uniqueHeader);
			dataTypes.add(dataType);
			if(dataType == SemossDataType.NUMBER) {
				max[i] = getMax(uniqueHeader);
				min[i] = getMin(uniqueHeader);
			}
		}

		RScaledUniqueFrameIterator iterator = new RScaledUniqueFrameIterator(this, this.builder, columnName, max, min, dataTypes, attributeUniqueHeaderName);
		return iterator;
	}
	
	@Override
	public void removeColumn(String columnHeader) {
		String tableName = this.builder.getTableName();
		this.builder.evalR(tableName + "[," + columnHeader + ":=NULL]");
		this.metaData.dropProperty(tableName + "__" + columnHeader, tableName);
		syncHeaders();
	}
	
	@Override
	public boolean isEmpty() {
		return this.builder.isEmpty();
	}
	
	public String getTableName() {
		return this.builder.getTableName();
	}
	
	public void setTableName(String tableVarName) {
		this.builder.setTableName(tableVarName);
	}
	
	public int getNumRows(String varName) {
		return this.builder.getNumRows(varName);
	}
	
	@Override
	public Double[] getColumnAsNumeric(String columnHeader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void save(String fileName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ITableDataFrame open(String fileName, String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDataMakerName() {
		return DATA_MAKER_NAME;
	}
	
	@Override
	protected Boolean calculateIsUnqiueColumn(String columnName) {
		// we override this method because it is faster to get the unique count
		// using the below syntax which works for only a single column
		// than it is using the syntax in the interpreter
		
		String tableName = getTableName();
		String[] cleanCols = new String[1];
		if(columnName.contains("__")) {
			cleanCols[0] = columnName.split("__")[1];
		} else {
			cleanCols[0] = columnName;
		}
		
		long start = System.currentTimeMillis();
		String rQuery = tableName + "[, " + RSyntaxHelper.createStringRColVec(cleanCols) + "]"; 
		int val1 = getNumRows(rQuery);
		long end = System.currentTimeMillis();
		logger.info("R duplicates query1 time = " + (end-start) + "ms");
		
		start = System.currentTimeMillis();
		String distinctQuery = "unique(" + tableName + "[, " + RSyntaxHelper.createStringRColVec(cleanCols) + "])"; 
		int val2 = getNumRows(distinctQuery);
		end = System.currentTimeMillis();
		logger.info("R duplicates query2 time = " + (end-start) + "ms");
		
		boolean isUnique = (long) val1 == (long) val2;
		return isUnique;
	}
	
	// generates a row id and binds it
	public void generateRowIdWithName()
	{
		this.builder.genRowId(getTableName(), "PRIM_KEY_PLACEHOLDER");
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * Deprecated DataMakerComponent stuff
	 */
	
	@Override
	@Deprecated
	public Map<String, String> getScriptReactors() {
		Map<String, String> reactorNames = super.getScriptReactors();
		reactorNames.put(PKQLEnum.IMPORT_DATA, "prerna.sablecc.RImportDataReactor");
		reactorNames.put(PKQLEnum.DATA_FRAME_DUPLICATES, "prerna.sablecc.RDuplicatesReactor");

		reactorNames.put(PKQLEnum.EXPR_TERM, "prerna.sablecc.ExprReactor");
		reactorNames.put(PKQLEnum.EXPR_SCRIPT, "prerna.sablecc.ExprReactor");
		reactorNames.put(PKQLReactor.MATH_FUN.toString(),"prerna.sablecc.MathReactor");
		reactorNames.put(PKQLEnum.COL_CSV, "prerna.sablecc.ColCsvReactor"); // it almost feels like I need a way to tell when to do this and when not but let me see
		reactorNames.put(PKQLEnum.ROW_CSV, "prerna.sablecc.RowCsvReactor");
		reactorNames.put(PKQLEnum.PASTED_DATA, "prerna.sablecc.PastedDataReactor");
		reactorNames.put(PKQLEnum.WHERE, "prerna.sablecc.ColWhereReactor");
		reactorNames.put(PKQLEnum.REL_DEF, "prerna.sablecc.RelReactor");
		reactorNames.put(PKQLEnum.COL_ADD, "prerna.sablecc.ColAddReactor");
		reactorNames.put(PKQLEnum.REMOVE_DATA, "prerna.sablecc.RemoveDataReactor");
		reactorNames.put(PKQLEnum.FILTER_DATA, "prerna.sablecc.ColFilterReactor");
		reactorNames.put(PKQLEnum.UNFILTER_DATA, "prerna.sablecc.ColUnfilterReactor");
		reactorNames.put(PKQLEnum.DATA_FRAME, "prerna.sablecc.DataFrameReactor");
		reactorNames.put(PKQLEnum.DASHBOARD_JOIN, "prerna.sablecc.DashboardJoinReactor");
		reactorNames.put(PKQLEnum.OPEN_DATA, "prerna.sablecc.OpenDataReactor");
		reactorNames.put(PKQLEnum.DATA_TYPE, "prerna.sablecc.DataTypeReactor");
		reactorNames.put(PKQLEnum.DATA_CONNECT, "prerna.sablecc.DataConnectReactor");
		reactorNames.put(PKQLEnum.JAVA_OP, "prerna.sablecc.JavaReactorWrapper");
		reactorNames.put(PKQLEnum.NETWORK_CONNECT, "prerna.sablecc.ConnectReactor");
		reactorNames.put(PKQLEnum.NETWORK_DISCONNECT, "prerna.sablecc.DisConnectReactor");

		reactorNames.put(PKQLEnum.VIZ, "prerna.sablecc.RVizReactor");

		reactorNames.put(PKQLEnum.SUM, "prerna.sablecc.expressions.r.RSumReactor");
		reactorNames.put(PKQLEnum.MAX, "prerna.sablecc.expressions.r.RMaxReactor");
		reactorNames.put(PKQLEnum.MIN, "prerna.sablecc.expressions.r.RMinReactor");
		reactorNames.put(PKQLEnum.AVERAGE, "prerna.sablecc.expressions.r.RAverageReactor");
		reactorNames.put(PKQLEnum.STANDARD_DEVIATION, "prerna.sablecc.expressions.r.RStandardDeviationReactor");
		reactorNames.put(PKQLEnum.MEDIAN, "prerna.sablecc.expressions.r.RMedianReactor");
		reactorNames.put(PKQLEnum.COUNT, "prerna.sablecc.expressions.r.RCountReactor");
		reactorNames.put(PKQLEnum.COUNT_DISTINCT, "prerna.sablecc.expressions.r.RUniqueCountReactor");

		reactorNames.put(PKQLEnum.QUERY_API, "prerna.sablecc.QueryApiReactor");
		reactorNames.put(PKQLEnum.CSV_API, "prerna.sablecc.RCsvApiReactor");
		reactorNames.put(PKQLEnum.EXCEL_API, "prerna.sablecc.RExcelApiReactor");
		reactorNames.put(PKQLEnum.WEB_API, "prerna.sablecc.WebApiReactor");

		return reactorNames;
	}
	
	@Override
	@Deprecated
	public void processDataMakerComponent(DataMakerComponent component) {
		// we have only had RDataTable since PKQL was introduced
		// lets not try to expand this to cover the old stuff
		// assuming only pkql is used
		long startTime = System.currentTimeMillis();
		logger.info("Processing Component..................................");
		processPostTransformations(component, component.getPostTrans());
		long endTime = System.currentTimeMillis();
		logger.info("Component Processed: " + (endTime - startTime) + " ms");		
	}
	
	@Override
	@Deprecated
	public void removeRelationship(String[] columns, Object[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	@Deprecated
	public void addRow(Object[] cleanCells, String[] headers) {
		// TODO Auto-generated method stub
		
	}
	
	
}
