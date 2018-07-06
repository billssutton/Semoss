package prerna.query.querystruct;

public class ExcelQueryStruct extends AbstractFileQueryStruct {

	private String sheetName;
	private String sheetRange;
	
	public ExcelQueryStruct() {
		this.setQsType(QUERY_STRUCT_TYPE.EXCEL_FILE);
	}
	
	public String getSheetName() {
		return sheetName;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}
	
	public String getSheetRange() {
		return this.sheetRange;
	}

	public void setSheetRange(String sheetRange) {
		this.sheetRange = sheetRange;
	}
	
	@Override
	public SelectQueryStruct getNewBaseQueryStruct() {
		ExcelQueryStruct newQs = new ExcelQueryStruct();
		newQs.setQsType(this.getQsType());
		newQs.setFilePath(this.getFilePath());
		newQs.setColumnTypes(this.getColumnTypes());
		newQs.setSheetName(this.getSheetName());
		newQs.setSheetRange(this.getSheetRange());
		return newQs;
	}
}
