package prerna.util.ga;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import prerna.om.Insight;
import prerna.query.querystruct.QueryStruct2;

public interface IGoogleAnalytics {

	/**
	 * Track a pixel of a certain type
	 * @param thisExpression
	 * @param thisType
	 */
	void track(String thisExpression, String thisType);
	
	/**
	 * Track a pixel of a certain type relative to the previous pixel executed
	 * @param thisExpression
	 * @param thisType
	 * @param prevExpression
	 * @param prevType
	 */
	void track(String thisExpression, String thisType, String prevExpression, String prevType);
	
	/**
	 * Track a pixel of a certain type relative to the previous pixel executed when we know the user id
	 * @param thisExpression
	 * @param thisType
	 * @param prevExpression
	 * @param prevType
	 */
	void track(String thisExpression, String thisType, String prevExpression, String prevType, String userId);
	
	/**
	 * Executes the tracking for an analytical routine
	 * @param in
	 * @param routine
	 */
	void trackAnalyticsPixel(Insight in, String routine);

	/**
	 * Execute the tracking of a data import or data merge
	 * @param in
	 * @param selectors
	 */
	void trackDataImport(Insight in, QueryStruct2 qs);
	
	/**
	 * Track running and saving an existing insight
	 * @param in
	 * @param type
	 * @param engineName
	 * @param rdbmsId
	 * @param insightName
	 */
	void trackInsightExecution(Insight in, String type, String engineName, String rdbmsId, String insightName);

	/**
	 * Track an excel upload into a database
	 * @param tableName
	 * @param files
	 * @param headerDataTypes
	 */
	void trackExcelUpload(String tableName, String fileName,
			List<Map<String, Map<String, String[]>>> headerDataTypes);

	/**
	 * Track a csv upload into a database
	 * @param files
	 * @param dbName
	 * @param headerDataTypes
	 */
	void trackCsvUpload(String files, String dbName, List<Map<String, String[]>> headerDataTypes);
	
	/**
	 * Track drag and drop 
	 * @param in
	 * @param headers
	 * @param FileName
	 */
	void trackDragAndDrop(Insight in, List<String> headers, String FileName);

	/**
	 * Executes tracking of visualizations
	 * @param mapOptions
	 * @param in
	 * @throws SQLException
	 */

	void trackViz(Map<String, Object> taskOptions, Insight in, QueryStruct2 qs);
	
}
