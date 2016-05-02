package prerna.ds.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import prerna.ds.QueryStruct;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.impl.rdf.HeadersDataRow;
import prerna.poi.main.helper.CSVFileHelper;
import prerna.util.Utility;

public class FileIterator implements Iterator<IHeadersDataRow>{

	CSVFileHelper helper;
	String[] headers;
	String[] types;
	String[] nextRow;
	
	Map<String, Set<Object>> filters;
	Map<String, String> dataTypeMap;
	
	public FileIterator(String fileLoc, QueryStruct qs, Map<String, String> dataTypeMap) {
		
		this.helper = new CSVFileHelper();
		filters = new HashMap<String, Set<Object>>();
		helper.setDelimiter(",".charAt(0));
		helper.parse(fileLoc);
		
		if(dataTypeMap != null && !dataTypeMap.isEmpty()) {
			this.dataTypeMap = dataTypeMap;
			headers = dataTypeMap.keySet().toArray(new String[]{});
			headers = helper.orderHeadersToGet(headers);
			
			types = new String[headers.length];
			for(int j = 0; j < headers.length; j++) {
				types[j] = dataTypeMap.get(headers[j]);
			}
			
			helper.parseColumns(headers);
			helper.getNextRow(); // next row is a header
		} else {
			this.dataTypeMap = new HashMap<String, String>();
			String[] allHeaders = helper.getHeaders();
			types = helper.predictTypes();
			for(int i = 0; i < types.length; i++) {
				this.dataTypeMap.put(allHeaders[i], types[i]);
			}
		}
		
		setSelectors(qs.getSelectors());
		setFilters(qs.andfilters);
		
		headers = helper.getHeaders();
//		headers = helper.orderHeadersToGet(headers);
		types = new String[headers.length];
		for(int i = 0; i < types.length; i++) {
			types[i] = this.dataTypeMap.get(headers[i]);
		}
		getNextRow();
//		nextRow = helper.getNextRow();
	}
	
	@Override
	public boolean hasNext() {
		return nextRow != null;
	}

	@Override
	public IHeadersDataRow next() {
		String[] row = nextRow;
		getNextRow();

		String[] cleanRow = new String[row.length];
		for(int i = 0; i < row.length; i++) {
			cleanRow[i] = Utility.cleanString(row[i], true, true, false);
		}
		IHeadersDataRow nextData = new HeadersDataRow(this.headers, cleanRow, cleanRow);
		return nextData;
	}
	
	public String[] getHeaders() {
		return this.headers;
	}
	
	public String[] getTypes() {
		return this.types;
	}
	
	public void getNextRow() {
		String[] row = helper.getNextRow();
		String[] newRow = null;
		while(newRow == null && (row != null)) {
			for(int i = 0; i < row.length; i++) {
				Set<Object> nextSet = filters.get(headers[i]);
				if(nextSet != null && nextSet.contains(row[i])) {
					newRow = row;
					break;
				}
			}
			if(newRow == null) {
				newRow = helper.getNextRow();
			}
		}
		
		this.nextRow = newRow;
	}
	
	private void setFilters(Hashtable<String, Hashtable<String, Vector>> andfilters) {
		for(String column : andfilters.keySet()) {
			Hashtable<String, Vector> filterValues = andfilters.get(column);
			Set<Object> values = new HashSet<>();
			for(String comparator : filterValues.keySet()) {
				List vals = filterValues.get(comparator);
				values.addAll(vals);
			}
			
			this.filters.put(column, values);
		}
	}
	
	private void setSelectors(Hashtable<String, Vector<String>> selectorSet) {
		String[] selectors = selectorSet.keySet().toArray(new String[]{});
		this.helper.parseColumns(selectors);
	}
}
