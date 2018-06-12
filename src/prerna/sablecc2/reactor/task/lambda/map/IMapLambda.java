package prerna.sablecc2.reactor.task.lambda.map;

import java.util.List;
import java.util.Map;

import prerna.auth.User;
import prerna.engine.api.IHeadersDataRow;

public interface IMapLambda {

	/**
	 * Process one row and output the row again
	 * Cannot modify the headers
	 * @param row
	 * @return
	 */
	IHeadersDataRow process(IHeadersDataRow row);
	
	/**
	 * Modify the header information if necessary for the new transformation
	 * @return
	 */
	List<Map<String, Object>> getModifiedHeaderInfo();
	
	/**
	 * Initialize the transformation by defining the columns being used
	 * @param headerInfo
	 * @param columns
	 */
	void init(List<Map<String, Object>> headerInfo, List<String> columns);
	
	/**
	 * Set the user within the transformation
	 */
	void setUser(User user);
}
