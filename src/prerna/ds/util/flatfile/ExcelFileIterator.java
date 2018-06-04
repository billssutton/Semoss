package prerna.ds.util.flatfile;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import prerna.algorithm.api.SemossDataType;
import prerna.date.SemossDate;
import prerna.engine.api.IHeadersDataRow;
import prerna.om.HeadersDataRow;
import prerna.poi.main.helper.FileHelperUtil;
import prerna.poi.main.helper.XLFileHelper;
import prerna.query.querystruct.ExcelQueryStruct;
import prerna.query.querystruct.filters.IQueryFilter;
import prerna.query.querystruct.filters.SimpleQueryFilter;
import prerna.query.querystruct.filters.SimpleQueryFilter.FILTER_TYPE;
import prerna.query.querystruct.selectors.IQuerySelector;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.ArrayUtilityMethods;
import prerna.util.Utility;

public class ExcelFileIterator extends AbstractFileIterator {

	private XLFileHelper helper;
	private ExcelQueryStruct qs;
	private int[] headerIndices;
	private String sheetToLoad;
	
	public ExcelFileIterator(ExcelQueryStruct qs) {
		this.qs = qs;
		this.fileLocation = qs.getFilePath();
		this.sheetToLoad = qs.getSheetName();
		this.helper = new XLFileHelper();
		this.helper.parse(qs.getFilePath());
		
		this.dataTypeMap = qs.getColumnTypes();
		this.newHeaders = qs.getNewHeaderNames();
		
		if(newHeaders != null && !newHeaders.isEmpty()) {
			Map<String, Map<String, String>> excelHeaderNames = new Hashtable<String, Map<String, String>>();
			excelHeaderNames.put(this.sheetToLoad, this.newHeaders);
			this.helper.modifyCleanedHeaders(excelHeaderNames);
		}
		
		setSelectors(qs.getSelectors());
		setFilters(qs.getExplicitFilters());
		
		// now that I have set the headers from the setSelectors
		this.headers = this.helper.getHeaders(this.sheetToLoad);
		this.additionalTypesMap = qs.getAdditionalTypes();
		
		if(this.dataTypeMap != null && !this.dataTypeMap.isEmpty()) {
			this.types = new SemossDataType[this.headers.length];
			this.additionalTypes = new String[this.headers.length];
			
			for (int index = 0; index < this.headers.length; index++) {
				this.types[index] = SemossDataType.convertStringToDataType(dataTypeMap.get(this.headers[index]));
				if(this.additionalTypesMap != null) {
					this.additionalTypes[index] = additionalTypesMap.get(this.headers[index]);
				}
			}
		}
		else {
			setUnknownTypes();
			setSelectors(qs.getSelectors());
			qs.setColumnTypes(this.dataTypeMap);
		}
		
		// need to grab the first row upon initialization 
		getNextRow();
	}
	
	private void setUnknownTypes() {
		Map[] predictionMaps = FileHelperUtil.generateDataTypeMapsFromPrediction(helper.getHeaders(this.sheetToLoad), helper.predictTypes(this.sheetToLoad, this.headerIndices));
		this.dataTypeMap = predictionMaps[0];
		this.additionalTypesMap = predictionMaps[1];
		
		// need to redo types to be only those in the selectors
		this.types = new SemossDataType[this.headers.length];
		this.additionalTypes = new String[this.headers.length];
		for(int i = 0; i < this.headers.length; i++) {
			this.types[i] = SemossDataType.convertStringToDataType(this.dataTypeMap.get(this.headers[i]));
			this.additionalTypes[i] = this.additionalTypesMap.get(this.headers[i]);
		}
	}
	
	@Override
	public IHeadersDataRow next() {
		Object[] row = nextRow;
		getNextRow();

		// couple of things to take care of here
//		Object[] cleanRow = cleanRow(row, types, additionalTypes);
		IHeadersDataRow nextData = new HeadersDataRow(this.headers, row, row);
		return nextData;
	}
	
//	protected Object[] cleanRow(Object[] row, SemossDataType[] types, String[] additionalTypes) {
//		Object[] cleanRow = new Object[row.length];
//		for(int i = 0; i < row.length; i++) {
//			SemossDataType type = types[i];
//			String val = row[i].toString().trim();
//			// try to get correct type
//			if(type == SemossDataType.INT) {
//				try {
//					//added to remove $ and , in data and then try parsing as Double
//					int mult = 1;
//					if(val.startsWith("(") || val.startsWith("-")) // this is a negativenumber
//						mult = -1;
//					val = val.replaceAll("[^0-9\\.E]", "");
//					cleanRow[i] = mult * Integer.parseInt(val.trim());
//				} catch(NumberFormatException ex) {
//					//do nothing
//					cleanRow[i] = null;
//				}
//			} else if(type == SemossDataType.DOUBLE) {
//				try {
//					//added to remove $ and , in data and then try parsing as Double
//					int mult = 1;
//					if(val.startsWith("(") || val.startsWith("-")) // this is a negativenumber
//						mult = -1;
//					val = val.replaceAll("[^0-9\\.E]", "");
//					cleanRow[i] = mult * Double.parseDouble(val.trim());
//				} catch(NumberFormatException ex) {
//					//do nothing
//					cleanRow[i] = null;
//				}
//			} else if(type == SemossDataType.DATE || type == SemossDataType.TIMESTAMP) {
//				String additionalTypeData = additionalTypes[i];
//				
//				// if we have additional data format for the date
//				// send the date object
//				Object date = null;
//				if(additionalTypeData != null) {
//					date = new SemossDate(val, additionalTypeData);
//				} else {
//					date = val;
//				}
//				cleanRow[i] = date;
//			} else {
//				cleanRow[i] = Utility.cleanString(val, true, true, false);
//			}
//		}
//		
//		return cleanRow;
//	}
	
	@Override
	public void getNextRow() {
		Object[] row = this.helper.getNextRow(this.sheetToLoad, this.headerIndices);
		
		if(filters == null || filters.isEmpty()) {
			this.nextRow = row;
			return;
		}
		
		Object[] newRow = null;
		while (newRow == null && (row != null)) {
			Set<String> allFilteredCols = this.filters.getAllFilteredColumns();
			//isValid checks if the row meets all of the given filters 
			boolean isValid = true;
			for (String col : allFilteredCols) {
				int rowIndex = Arrays.asList(headers).indexOf(col);
				// check valid index
				if (rowIndex >= 0) {
					//list of all filters on a given column
					List<SimpleQueryFilter> nextSet = this.filters.getAllSimpleQueryFiltersContainingColumn(col);
					for (SimpleQueryFilter filter : nextSet) {
						//get all filter information
						FILTER_TYPE filterType = filter.getFilterType();
						NounMetadata leftComp = filter.getLComparison();
						NounMetadata rightComp = filter.getRComparison();
						String comparator = filter.getComparator();
						if (filterType == FILTER_TYPE.COL_TO_COL) {
							//TODO
							//isValid = isValid && filterColToCol(leftComp, rightComp, row, comparator, rowIndex);
						} else if (filterType == FILTER_TYPE.COL_TO_VALUES) {
							// Genre = ['Action'] example
							isValid = isValid && filterColToValues(leftComp, rightComp, row, comparator, rowIndex);
						} else if (filterType == FILTER_TYPE.VALUES_TO_COL) {
							// here the left and rightcomps are reversed, so send them to the method in opposite order and reverse comparator
							// 50000 > MovieBudget gets sent as MovieBudget < 50000
							isValid = isValid && filterColToValues(rightComp, leftComp, row, IQueryFilter.getReverseNumericalComparator(comparator), rowIndex);
						}
					}
				}
			}

			if (isValid) {
				newRow = row;
				break;
			} else {
				newRow = null;
			}

			if (newRow == null) {
				row = this.helper.getNextRow(this.sheetToLoad, this.headerIndices);
			}

		}
		this.nextRow = newRow;
	}

	private void setSelectors(List<IQuerySelector> qsSelectors) {
		if(qsSelectors.isEmpty()) {
			return; // if no selectors, return everything
		}
		int numSelectors = qsSelectors.size();

		String[] selectors = new String[numSelectors];
		
		for(int i = 0; i < numSelectors; i++) {
			QueryColumnSelector newSelector = (QueryColumnSelector) qsSelectors.get(i);
			if(newSelector.getSelectorType() != IQuerySelector.SELECTOR_TYPE.COLUMN) {
				throw new IllegalArgumentException("Cannot perform math on a excel import");
			}
			selectors[i] = newSelector.getAlias();
		}
		
		String[] allHeaders = this.helper.getHeaders(this.sheetToLoad);
		if(allHeaders.length != selectors.length) {
			// order the selectors
			// all headers will be ordered
			String[] orderedSelectors = new String[selectors.length];
			int counter = 0;
			for(String header : allHeaders) {
				if(ArrayUtilityMethods.arrayContainsValue(selectors, header)) {
					orderedSelectors[counter] = header;
					counter++;
				}
			}
			
			this.headers = orderedSelectors;
			this.headerIndices = this.helper.getHeaderIndicies(this.sheetToLoad, orderedSelectors);
			this.helper.getNextRow(this.sheetToLoad, this.headerIndices); // after redoing the selectors, we need to skip the headers 
		} else {
			this.headers = allHeaders;
			this.headerIndices = new int[this.headers.length];
			for(int i = 0; i < this.headers.length; i++) {
				this.headerIndices[i] = i;
			}
		}
	}

	@Override
	public void resetHelper() {
		this.helper.reset();
	}

	@Override
	public void clearHelper() {
		this.helper.clear();
	}
	
	public XLFileHelper getHelper() {
		return this.helper;
	}
	
	public int getSheetIndex() {
		return this.helper.getSheetIndex(this.sheetToLoad);
	}
	
	public ExcelQueryStruct getQs() {
		return this.qs;
	}

	public void setQs(ExcelQueryStruct qs) {
		this.qs = qs;
	}
}
