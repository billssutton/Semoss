package prerna.sablecc.expressions.r.builder;

import java.util.List;
import java.util.Vector;

public class RColumnSelector implements IRExpressionSelector {

	private String columnName;
	
	public RColumnSelector(String columnName) {
		this.columnName = columnName;
	}
	
	@Override
	public List<String> getTableColumns() {
		List<String> tableColumns = new Vector<String>();
		tableColumns.add(columnName);
		return tableColumns;
	}
	
	@Override
	public String toString() {
		return columnName;
	}

	@Override
	public String getName() {
		return columnName;
	}

}
