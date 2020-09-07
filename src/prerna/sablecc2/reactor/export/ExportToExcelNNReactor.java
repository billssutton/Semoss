package prerna.sablecc2.reactor.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import prerna.algorithm.api.SemossDataType;
import prerna.date.SemossDate;
import prerna.engine.api.IHeadersDataRow;
import prerna.om.InsightPanel;
import prerna.om.InsightSheet;
import prerna.om.ThreadStore;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.om.task.BasicIteratorTask;
import prerna.sablecc2.om.task.ITask;
import prerna.sablecc2.om.task.options.TaskOptions;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.ChromeDriverUtility;
import prerna.util.Utility;


// export to excel non-native is the NN
public class ExportToExcelNNReactor extends AbstractReactor {
	
	public static final String exportTemplate = "EXCEL_EXPORT_TEMPLATE";

	public ExportToExcelNNReactor() {
		this.keysToGet = new String[] {ReactorKeysEnum.FILE_NAME.getKey(), ReactorKeysEnum.FILE_PATH.getKey(), ReactorKeysEnum.USE_PANEL.getKey()};
	}

	@Override
	public NounMetadata execute() {
		
		// get the number of sheets
		// export each sheet using the insight definition
		// Open excel
		// embed each of the sheet
		
		
		NounMetadata retNoun = null;
		organizeKeys();
		String insightFolder = this.insight.getInsightFolder();
		String fileName = null;
		
		if(keyValue.containsKey(ReactorKeysEnum.FILE_PATH.getKey()))
			insightFolder = (String)keyValue.get(ReactorKeysEnum.FILE_PATH.getKey());
		if(keyValue.containsKey(ReactorKeysEnum.FILE_NAME.getKey()))
			fileName = (String)keyValue.get(ReactorKeysEnum.FILE_NAME.getKey());
		
		String baseUrl = this.insight.getBaseURL();
		String sessionId = ThreadStore.getSessionId();
		String imageUrl = this.insight.getLiveURL();
		boolean panel = false;
		
		if(keyValue.containsKey(ReactorKeysEnum.USE_PANEL.getKey()))
		{
			String panelUse= (String)keyValue.get(ReactorKeysEnum.USE_PANEL.getKey());
			panel = panelUse.equalsIgnoreCase("yes") || panelUse.equalsIgnoreCase("true");
		}
		
		
		// see if someone has pushed a template file into insight
		String template = insight.getProperty(exportTemplate);
		// open a workbook
		
	   Map <String, InsightSheet> allSheets = insight.getInsightSheets();
	   Map <String, InsightPanel> allPanels = insight.getInsightPanels();
	   
	   // sort out the panels by sheet
	   // only get the pivot panels
	   Map <String, InsightPanel> pivotPanelsBySheet = new HashMap<String, InsightPanel>();
	   Iterator <InsightPanel> allPanelsIterator = allPanels.values().iterator();
	   while(allPanelsIterator.hasNext())
	   {
		   InsightPanel thisPanel = allPanelsIterator.next();
		   TaskOptions options = thisPanel.getTaskOptions();
		   String chartLayout = options.getLayout(thisPanel.getPanelId());
		   
		   System.err.println("Layout is set to " + chartLayout);
		   

		   if(chartLayout.equalsIgnoreCase("PivotTable"))
			   pivotPanelsBySheet.put(thisPanel.getSheetId(), thisPanel);
		   
	   }
	   
	   Iterator <String> keys = allSheets.keySet().iterator();
	   if(panel)
		   keys = allPanels.keySet().iterator();
	   
	   List <String> processedSheetPanel = new ArrayList<String>();
	   
	   try {
		   Workbook wb = new XSSFWorkbook();
			if(template != null)
				wb = new XSSFWorkbook(template);
			
		   while(keys.hasNext())
		   {
			   String thisKey = keys.next();
			   String sheetAppender = "";
			   String panelAppender = "";
			   String sheetLabel = "";
			   String sheetKey = "";
			   
			   if(panel)
			   {			   
				   InsightPanel thisPanel = allPanels.get(thisKey);
				   panelAppender = "&panel=" + thisKey;
				   
				   InsightSheet thisSheet = allSheets.get(thisPanel.getSheetId());
				   sheetAppender = "&sheet=" + thisSheet.getSheetId();
				   sheetKey = thisSheet.getSheetId();
				   sheetLabel = thisSheet.getSheetLabel();
				   if(sheetLabel == null)
					   sheetLabel = "Sheet" + (Integer.parseInt(sheetKey) + 1); 
				   sheetLabel = sheetLabel + " Panel - " + thisKey;
			   }
			   else
			   {
				   InsightSheet thisSheet = allSheets.get(thisKey);

				   sheetAppender = "&sheet=" + thisKey;
				   sheetKey = thisKey;				   
				   sheetLabel = thisSheet.getSheetLabel();
				   if(sheetLabel == null)
					   sheetLabel = "Sheet" + (Integer.parseInt(thisKey) + 1); 
			   }
			   
			   Sheet sheet = null;
			   
			   if(template != null)
				   sheet = wb.cloneSheet(wb.getSheetIndex("Template"));
			   else
				   sheet = wb.createSheet(sheetLabel);
			   
			   if(!pivotPanelsBySheet.containsKey(thisKey))
			   {
				   // now capture the image and fill it
					String prefixName = Utility.getRandomString(8);
					String exportName = AbstractExportTxtReactor.getExportFileName(prefixName, "png");
					String fileLocation = insightFolder + DIR_SEPARATOR + exportName;
				 
					// download this file
					ChromeDriverUtility.captureImage(baseUrl, imageUrl + sheetAppender + panelAppender, fileLocation, sessionId, 800, 600, true);
					// write this to the sheet now
					
					//1920 x 936
				   //FileInputStream obtains input bytes from the image file
				   InputStream inputStream = new FileInputStream(fileLocation);
				   //Get the contents of an InputStream as a byte[].
				   byte[] bytes = IOUtils.toByteArray(inputStream);
				   //Adds a picture to the workbook
				   int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
				   //close the input stream
				   inputStream.close();
				   
				   FileUtils.forceDelete(new File(fileLocation));
	
				   //Returns an object that handles instantiating concrete classes
				   CreationHelper helper = wb.getCreationHelper();
				   //Creates the top-level drawing patriarch.
				   Drawing drawing = sheet.createDrawingPatriarch();
	
				   //Create an anchor that is attached to the worksheet
				   ClientAnchor anchor = helper.createClientAnchor();
	
				   //create an anchor with upper left cell _and_ bottom right cell
				   anchor.setCol1(1); //Column B
				   anchor.setRow1(2); //Row 3
				   anchor.setCol2(2); //Column C // doesnt matter
				   anchor.setRow2(4); //Row 4
	
				   //Creates a picture
				   Picture pict = drawing.createPicture(anchor, pictureIdx);
				   pict.resize();
	
				   //Reset the image to the original size
				   //pict.resize(); //don't do that. Let the anchor resize the image!
				   //Create the Cell B3
				   Cell cell = sheet.createRow(2).createCell(1);
			   }
			   else
			   {
				   // this is the gen pivot logic
				   // need to write the data
				   // and then generate the pivot from it
				   
				   // get the task options
				   // make the frame
				   // call the genXLPivot
				   InsightPanel pivotPanel = pivotPanelsBySheet.get(thisKey);
				   TaskOptions taskOptions= pivotPanel.getTaskOptions();
				   ITask task = new BasicIteratorTask(pivotPanel.getLastQs());
				   task.setTaskOptions(taskOptions);
				   
				   // I dont know if this can deal with older excel formats ?
				   Map <String, Object> columnMap = writeData((XSSFWorkbook)wb, task, (XSSFSheet)sheet);
				   
				   // get other data now
				   Map <String, Object> pivotMakerOptions = taskOptions.getAlignmentMap(pivotPanel.getPanelId());
				   // get the rows
				   List <String> rows = (List <String>)pivotMakerOptions.get("rows");
				   List <String> columns = (List <String>)pivotMakerOptions.get("columns");
				   // calculations is being kept directly in task options so going to pick from there
				   List <String> values = (List <String>)taskOptions.getOptions().get("values");

				   List <String> newValues = new Vector<String>();
				   List <String> functions = new Vector<String>();
				   // now generate the pivot
				   // need to parse values and functions separately
					for(int valIndex = 0;valIndex < values.size();valIndex++)
					{
						Map<String, String> valueMap = new HashMap<String, String>();
						String curValue = values.get(valIndex);

						// get the operator and selector
						//String [] composite = curValue.split("(");
						String operator = curValue.substring(0, curValue.indexOf("(")).trim();
						String operand = curValue.substring(curValue.indexOf("(") + 1, curValue.length()-1).trim();
						newValues.add(operand);
						functions.add(operator);
					}
					
					genXLPivot((XSSFSheet)sheet, rows, columns, newValues, functions, columnMap);
				   
			   }
		   }
		   
		   // remove the template sheet when you finally save it
		   // it is no longer needed
		   if(template != null)
			   wb.removeSheetAt(wb.getSheetIndex(wb.getSheet("Template")));

		   makeParamSheet(wb);
		   String prefixName = fileName;
		   String exportName = AbstractExportTxtReactor.getExportFileName(prefixName, "xlsx");
		   String fileLocation = insightFolder + DIR_SEPARATOR + exportName;
		   FileOutputStream fileOut = null;
		   fileOut = new FileOutputStream(fileLocation);
		   wb.write(fileOut);
		   fileOut.close();
		   retNoun = new NounMetadata(exportName, PixelDataType.CONST_STRING, PixelOperationType.FILE_DOWNLOAD);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		// now that the file location has been set
		// create the excel file there
		return retNoun;
	}
	
	private void makeParamSheet(Workbook wb)
	{
		// get to the insight
		// pick the frame
		// print the filter
		// create a new sheet call it parameters / audit trail
		// print it one by one
		Sheet paramSheet = wb.createSheet();
		wb.setSheetName(wb.getSheetIndex(paramSheet), "Parameters Audit");
		Row row = paramSheet.createRow(2);
		row.createCell(2).setCellValue("Parameter Name");
		row.createCell(3).setCellValue("Parameter Value(s)");
		
		
		Map <String, StringBuffer> colParams = this.insight.getCurFrame().printFilters(null, true);
		Iterator <String> columns = colParams.keySet().iterator();
		
		// print out all the parameters
		for(int rowIndex = 3;columns.hasNext();rowIndex++)
		{
			row = paramSheet.createRow(rowIndex);
			String column = columns.next();
			String values = colParams.get(column).toString();
			row.createCell(2).setCellValue(column);
			row.createCell(3).setCellValue(values);
		}
	}
	
	private Map<String, Object> writeData(XSSFWorkbook workbook, ITask task, XSSFSheet sheet) {
		CreationHelper createHelper = workbook.getCreationHelper();
		// freeze the first row
		sheet.createFreezePane(0, 1);

		int i = 0;
		int size = 0;
		// create typesArr as an array for faster searching
		String[] headers = null;
		SemossDataType[] typesArr = null;

		// style dates
		CellStyle dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
		// style timestamps
		CellStyle timeStampCellStyle = workbook.createCellStyle();
		timeStampCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy HH:mm:ss"));

		// the excel data row
		// why is there an end row ?
		Row excelRow = null;
		int excelColStart = 0;
		int curSheetCol = i + excelColStart;
		int endRow = 0;
		int excelRowCounter = 0;

		// we need to iterate and write the headers during the first time
		if (task.hasNext()) {
			IHeadersDataRow row = task.next();
			List<Map<String, Object>> headerInfo = task.getHeaderInfo();

			// create the header row
			Row headerRow = null;
			if (excelRowCounter < endRow) {
				headerRow = sheet.getRow(excelRowCounter++);
			} else {
				headerRow = sheet.createRow(excelRowCounter++);
			}
			// create a Font for styling header cells
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			// create a CellStyle with the font
			CellStyle headerCellStyle = workbook.createCellStyle();
			headerCellStyle.setFont(headerFont);
			headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
			headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			// generate the header row
			// and define constants used throughout like size, and types
			i = 0;
			headers = row.getHeaders();
			size = headers.length;
			typesArr = new SemossDataType[size];
			for (; i < size; i++) {
				curSheetCol = i + excelColStart;
				Cell cell = headerRow.createCell(curSheetCol);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerCellStyle);

				if (headerInfo.get(i).containsKey("type")) {
					typesArr[i] = SemossDataType.convertStringToDataType(headerInfo.get(i).get("type").toString());
				} else {
					typesArr[i] = SemossDataType.STRING;
				}
			}

			// generate the data row
			// ah it is creating the data row first time ok
			if (excelRowCounter < endRow) {
				excelRow = sheet.getRow(excelRowCounter++);
			} else {
				excelRow = sheet.createRow(excelRowCounter++);

			}
			Object[] dataRow = row.getValues();
			i = 0;
			for (; i < size; i++) {
				curSheetCol = i + excelColStart;
				Cell cell = excelRow.createCell(curSheetCol);
				Object value = dataRow[i];
				if (value == null || value.toString().length() == 0) {
					cell.setCellValue("");
				} else {
					if (typesArr[i] == SemossDataType.STRING) {
						cell.setCellValue(value + "");
					} else if (typesArr[i] == SemossDataType.INT || typesArr[i] == SemossDataType.DOUBLE) {
						cell.setCellValue(((Number) value).doubleValue());
					} else if (typesArr[i] == SemossDataType.DATE) {
						cell.setCellValue(((SemossDate) value).getDate());
						cell.setCellStyle(dateCellStyle);
					} else if (typesArr[i] == SemossDataType.TIMESTAMP) {
						cell.setCellValue(((SemossDate) value).getDate());
						cell.setCellStyle(timeStampCellStyle);
					} else if (typesArr[i] == SemossDataType.BOOLEAN) {
						cell.setCellValue((boolean) value);
					} else {
						cell.setCellValue(value + "");
					}
				}
			}
		}

		// now iterate through all the data
		while (task.hasNext()) {
			if (excelRowCounter < endRow) {
				excelRow = sheet.getRow(excelRowCounter++);
			} else {
				excelRow = sheet.createRow(excelRowCounter++);
			}
			IHeadersDataRow row = task.next();
			Object[] dataRow = row.getValues();
			i = 0;
			for (; i < size; i++) {
				curSheetCol = i + excelColStart;
				Cell cell = excelRow.createCell(curSheetCol);
				Object value = dataRow[i];
				if (value == null || value.toString().length() == 0) {
					cell.setCellValue("");
				} else {
					if (typesArr[i] == SemossDataType.STRING) {
						cell.setCellValue(value + "");
					} else if (typesArr[i] == SemossDataType.INT || typesArr[i] == SemossDataType.DOUBLE) {
						cell.setCellValue(((Number) value).doubleValue());
					} else if (typesArr[i] == SemossDataType.DATE) {
						cell.setCellValue(((SemossDate) value).getDate());
						cell.setCellStyle(dateCellStyle);
					} else if (typesArr[i] == SemossDataType.TIMESTAMP) {
						cell.setCellValue(((SemossDate) value).getDate());
						cell.setCellStyle(timeStampCellStyle);
					} else if (typesArr[i] == SemossDataType.BOOLEAN) {
						cell.setCellValue((boolean) value);
					} else {
						cell.setCellValue(value + "");
					}
				}
			}
		}
		Map<String, Object> columnMap = new HashMap<>();

		// Update col and row bounds for sheet
		int endCol = curSheetCol;
		columnMap.put("colIndex", endCol + 1);
		if (excelRowCounter > endRow) {
			columnMap.put("rowIndex", excelRowCounter);
		}

		List<String> headerList = Arrays.asList(headers);

		if (headers != null && headers.length > 0) {
				columnMap.put("startRow", 0);
				columnMap.put("endRow", excelRowCounter - 1);
				// find header index in list
				columnMap.put("startCol", 0);
				columnMap.put("endCol", excelColStart + (headers.length - 1));
		}
		
		return columnMap;
	}
	
	
	// now generate the excel pivot
	public void genXLPivot(XSSFSheet srcSheet, List <String> rows, List <String> columns, List <String> values, List <String> functions, Map <String, Object> columnMap)
	{
		// will never be the case
		if(values.size() != functions.size())
			return; // bye bye
		// xl pivot method here
		// assume data has been recorded on sheet1
		
		//Workbook wb = new XSSFWorkbook();

		// find the upper limits and lower limits
		/*
		int firstRow = 	srcSheet.getFirstRowNum();
		int firstCol = srcSheet.getRow(firstRow).getFirstCellNum();

		
		int lastRow = srcSheet.getLastRowNum();
		int lastCol = srcSheet.getRow(lastRow).getLastCellNum();
		*/
		
		int firstRow = (Integer)columnMap.get("startRow");
		int firstCol = (Integer)columnMap.get("startCol");
		int lastRow = (Integer)columnMap.get("endRow");
		int lastCol = (Integer)columnMap.get("endCol");
		
		// add an outline to the source sheet
		// and collapse it
		srcSheet.groupColumn(firstCol, lastCol);
		srcSheet.setColumnGroupCollapsed(firstCol, true);
		
		// impute the headers
		// I need to do this since everything uses cardinality
		Row headerRow = srcSheet.getRow(firstRow);
		List <String> xlHeaders = new Vector<String>();
		
		for(int cellIndex = firstCol; cellIndex <= lastCol;cellIndex++)
			xlHeaders.add(headerRow.getCell(cellIndex).toString());
		
		// compose the area to use
		AreaReference pivSource = new AreaReference(new CellReference(firstRow,firstCol), new CellReference(lastRow, lastCol), SpreadsheetVersion.EXCEL2007);
		//XSSFSheet pivSheet = (XSSFSheet) wb.createSheet();

		String colName = CellReference.convertNumToColString(lastCol+2);
		
		XSSFPivotTable pivotTable = srcSheet.createPivotTable(pivSource, new CellReference((firstRow+2),(lastCol+2)));
	    pivotTable.getCTPivotTableDefinition().getPivotTableStyleInfo().setShowColHeaders(true);
	    pivotTable.getCTPivotTableDefinition().getPivotTableStyleInfo().setShowRowHeaders(true);

		// and now we start adding the rows and columns
		for(int rowIndex = 0;rowIndex < rows.size();rowIndex++)
		{
			String rowHeader = rows.get(rowIndex);
			int xlHeaderIndex = xlHeaders.indexOf(rowHeader);
			
			// set the first header
			if(rowIndex == 0)
				pivotTable.getCTPivotTableDefinition().setRowHeaderCaption(rowHeader);
			
		    pivotTable.addRowLabel(xlHeaderIndex);
		    pivotTable.getCTPivotTableDefinition().getPivotFields().getPivotFieldArray(xlHeaderIndex).setOutline(false);
		}
		
		
		for(int valIndex = 0;valIndex < values.size();valIndex++)
		{
			String value = values.get(valIndex);
			String function = functions.get(valIndex);

			DataConsolidateFunction xlFun = convertToExcelFunction(function);
			int xlHeaderIndex = xlHeaders.indexOf(value);
			
			pivotTable.addColumnLabel(xlFun, xlHeaderIndex);
		}
		
		// adding columns 
		for(int colIndex = 0;colIndex < columns.size();colIndex++)
		{
			String column = columns.get(colIndex);
			int xlHeaderIndex = xlHeaders.indexOf(column);			
		    pivotTable.getCTPivotTableDefinition().getPivotFields().getPivotFieldArray(xlHeaderIndex).setAxis(
		            org.openxmlformats.schemas.spreadsheetml.x2006.main.STAxis.AXIS_COL);
		    pivotTable.getCTPivotTableDefinition().getPivotFields().getPivotFieldArray(xlHeaderIndex).addNewItems();
		    pivotTable.getCTPivotTableDefinition().getPivotFields().getPivotFieldArray(xlHeaderIndex).getItems().addNewItem().setT(
		            org.openxmlformats.schemas.spreadsheetml.x2006.main.STItemType.DEFAULT);
		    pivotTable.getCTPivotTableDefinition().addNewColFields().addNewField().setX(xlHeaderIndex);
		}

//		try {
//		FileOutputStream fileOut = null;
//		   fileOut = new FileOutputStream("c:/users/pkapaleeswaran/workspacej3/temp/myTFile.xlsx");
//		   wb.write(fileOut);
//		   fileOut.close();
//	} catch (FileNotFoundException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	} catch (IOException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}

		
	}
	
	
	public DataConsolidateFunction convertToExcelFunction(String functionName)
	{
		DataConsolidateFunction retFunction = null;
		
		switch(functionName.toUpperCase())
		{
			case "SUM":
				retFunction = DataConsolidateFunction.SUM;
				break;
			case "COUNT":
				retFunction = DataConsolidateFunction.COUNT;
				break;
			case "MIN":
				retFunction = DataConsolidateFunction.MIN;
				break;
			case "MAX":
				retFunction = DataConsolidateFunction.MAX;
				break;
			case "MEDIAN": // cheating here
				retFunction = DataConsolidateFunction.AVERAGE;
				break;
			case "STDDEV": // need to see the actual name
				retFunction = DataConsolidateFunction.STD_DEV;
				break;
			case "AVERAGE":
				retFunction = DataConsolidateFunction.AVERAGE;
				break;
			case "MEAN":
				retFunction = DataConsolidateFunction.AVERAGE;
				break;
			default:
				retFunction = DataConsolidateFunction.SUM;
				break;		
		}
		return retFunction;
		
	}
	

	
	
	
}
