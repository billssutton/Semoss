package prerna.auth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import prerna.ds.util.RdbmsQueryBuilder;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.api.IRawSelectWrapper;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

public abstract class AbstractSecurityUtils {

	static RDBMSNativeEngine securityDb;
	
	/**
	 * Only used for static references
	 */
	AbstractSecurityUtils() {
		
	}
	
	public static void loadSecurityDatabase() {
		securityDb = (RDBMSNativeEngine) Utility.getEngine(Constants.SECURITY_DB);
		initialize();
		// TODO: testing code!!!!
		// TODO: testing code!!!!
		// TODO: testing code!!!!
		// TODO: testing code!!!!
		// TODO: testing code!!!!
		// TODO: testing code!!!!
		// TODO: testing code!!!!
		// TODO: testing code!!!!
//		String deleteQuery = "DELETE FROM ENGINE WHERE 1=1";
//		securityDb.removeData(deleteQuery);
//		deleteQuery = "DELETE FROM INSIGHT WHERE 1=1";
//		securityDb.removeData(deleteQuery);
//		deleteQuery = "DELETE FROM ENGINEPERMISSION WHERE 1=1";
//		securityDb.removeData(deleteQuery);
//		deleteQuery = "DELETE FROM ENGINEMETA WHERE 1=1";
//		securityDb.removeData(deleteQuery);
	}

	/**
	 * Does this engine name already exist
	 * @param appName
	 * @return
	 */
	@Deprecated
	//TODO: needs to account for a user having the app name already
	public static boolean containsEngine(String appName) {
		if(ignoreEngine(appName)) {
			// dont add local master or security db to security db
			return true;
		}
		String query = "SELECT ENGINEID FROM ENGINE WHERE ENGINENAME='" + appName + "'";
		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
		try {
			if(wrapper.hasNext()) {
				return true;
			} else {
				return false;
			}
		} finally {
			wrapper.cleanUp();
		}
	}
	
	public static boolean containsEngineId(String appId) {
		if(ignoreEngine(appId)) {
			// dont add local master or security db to security db
			return true;
		}
		String query = "SELECT ENGINEID FROM ENGINE WHERE ENGINEID='" + appId + "'";
		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
		try {
			if(wrapper.hasNext()) {
				return true;
			} else {
				return false;
			}
		} finally {
			wrapper.cleanUp();
		}
	}
	
	static boolean ignoreEngine(String appId) {
		if(appId.equals(Constants.LOCAL_MASTER_DB_NAME) || appId.equals(Constants.SECURITY_DB)) {
			// dont add local master or security db to security db
			return true;
		}
		return false;
	}
	
	public static void initialize() {
		String[] colNames = null;
		String[] types = null;
		Object[] defaultValues = null;
		/*
		 * Currently used
		 */
		
		// ENGINE
		colNames = new String[] { "enginename", "engineid", "global", "type", "cost" };
		types = new String[] { "varchar(255)", "varchar(255)", "boolean", "varchar(255)", "varchar(255)" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("ENGINE", colNames, types));

		// ENGINEMETA
		colNames = new String[] { "engineid", "key", "value" };
		types = new String[] { "varchar(255)", "varchar(255)", "varchar(255)" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("ENGINEMETA", colNames, types));

		// ENGINEPERMISSION
		colNames = new String[] { "userid", "permission", "engineid", "visibility" };
		types = new String[] { "varchar(255)", "integer", "varchar(255)", "boolean" };
		defaultValues = new Object[]{null, null, null, null, true};
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreateWithDefault("ENGINEPERMISSION", colNames, types, defaultValues));

		// INSIGHT
		colNames = new String[] { "engineid", "insightid", "insightname", "global", "executioncount", "createdon", "lastmodifiedon", "layout" };
		types = new String[] { "varchar(255)", "varchar(255)", "varchar(255)", "boolean", "bigint", "timestamp", "timestamp", "varchar(255)" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("INSIGHT", colNames, types));

		// USERINSIGHTPERMISSION
		colNames = new String[] { "userid", "engineid", "insightid", "permission" };
		types = new String[] { "varchar(255)", "varchar(255)", "varchar(255)", "integer" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("USERINSIGHTPERMISSION", colNames, types));

		// PERMISSION
		colNames = new String[] { "id", "name" };
		types = new String[] { "integer", "varchar(255)" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("PERMISSION", colNames, types));
		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, "select count(*) from permission");
		if(wrapper.hasNext()) {
			if( ((Number) wrapper.next().getValues()[0]).intValue() <= 3) {
				securityDb.insertData(RdbmsQueryBuilder.makeInsert("PERMISSION", colNames, types, new Object[]{1, "OWNER"}));
				securityDb.insertData(RdbmsQueryBuilder.makeInsert("PERMISSION", colNames, types, new Object[]{2, "EDIT"}));
				securityDb.insertData(RdbmsQueryBuilder.makeInsert("PERMISSION", colNames, types, new Object[]{3, "READ_ONLY"}));
			}
		}
		
		// USER
		colNames = new String[] { "name", "email", "type", "admin", "id", "password", "salt", "username" };
		types = new String[] { "varchar(255)", "varchar(255)", "varchar(255)", "boolean", "varchar(255)", "varchar(255)", "varchar(255)", "varchar(255)" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("USER", colNames, types));
		
		// USERGROUP
		colNames = new String[] { "groupid", "name", "owner" };
		types = new String[] { "identity", "varchar(255)", "varchar(255)" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("USERGROUP", colNames, types));
		
		// GROUPMEMBERS
		colNames = new String[] {"groupmembersid", "groupid", "userid"};
		types = new String[] {"identity", "integer", "varchar(255)"};
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("GROUPMEMBERS", colNames, types));
		
		// ENGINEGROUPMEMBERVISIBILITY
		colNames = new String[] { "id", "groupenginepermissionid", "groupmembersid", "visibility" };
		types = new String[] { "identity", "integer", "integer", "boolean" };
		defaultValues = new Object[]{null, null, null, true};
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreateWithDefault("ENGINEGROUPMEMBERVISIBILITY", colNames, types, defaultValues));

		// GROUPENGINEPERMISSION
		colNames = new String[] {"groupenginepermissionid", "groupid", "permission", "engine"};
		types = new String[] {"identity", "integer", "integer", "varchar(255)"};
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("GROUPENGINEPERMISSION", colNames, types));

		// FOREIGN KEYS FOR CASCASDE DELETE
		wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, "select count(*) from INFORMATION_SCHEMA.CONSTRAINTS where constraint_name='FK_GROUPENGINEPERMISSION'");
		if(wrapper.hasNext()) {
			if( ((Number) wrapper.next().getValues()[0]).intValue() == 0) {
				securityDb.insertData("ALTER TABLE ENGINEGROUPMEMBERVISIBILITY ADD CONSTRAINT FK_GROUPENGINEPERMISSION FOREIGN KEY (GROUPENGINEPERMISSIONID) REFERENCES GROUPENGINEPERMISSION(GROUPENGINEPERMISSIONID) ON DELETE CASCADE;");
			}
		}
		wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, "select count(*) from INFORMATION_SCHEMA.CONSTRAINTS where constraint_name='FK_GROUPMEMBERSID'");
		if(wrapper.hasNext()) {
			if( ((Number) wrapper.next().getValues()[0]).intValue() == 0) {
				securityDb.insertData("ALTER TABLE ENGINEGROUPMEMBERVISIBILITY ADD CONSTRAINT FK_GROUPMEMBERSID FOREIGN KEY (GROUPMEMBERSID) REFERENCES GROUPMEMBERS (GROUPMEMBERSID) ON DELETE CASCADE;");
			}
		}
		
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		
		/*
		 * Tables accounted for that we are not using yet...
		 */
		
		// ACCESSREQUEST
		colNames = new String[] { "id", "submittedby", "submittedto", "engine", "permission" };
		types = new String[] { "integer", "varchar(255)", "varchar(255)", "integer", "integer" };
		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("ACCESSREQUEST", colNames, types));

//		// GROUPINSIGHTPERMISSION
//		colNames = new String[] { "groupid", "engineid", "insightid" };
//		types = new String[] { "integer", "integer", "varchar(255)" };
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("GROUPINSIGHTPERMISSION", colNames, types));

//		// INSIGHTEXECUTION
//		colNames = new String[] { "user", "database", "insight", "count", "lastexecuted", "session" };
//		types = new String[] { "varchar(255)", "varchar(255)", "varchar(255)", "integer", "date", "varchar(255)" };
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("INSIGHTEXECUTION", colNames, types));

//		// SEED
//		colNames = new String[] { "id", "name", "databaseid", "tablename", "columnname", "rlsvalue", "rlsjavacode", "owner" };
//		types = new String[] { "integer", "varchar(255)", "integer", "varchar(255)", "varchar(255)", "varchar(255)", "clob", "varchar(255)" };
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("SEED", colNames, types));

//		// USERSEEDPERMISSION
//		colNames = new String[] { "userid", "seedid" };
//		types = new String[] { "varchar(255)", "integer" };
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("USERSEEDPERMISSION", colNames, types));
		
//		// GROUPSEEDPERMISSION
//		colNames = new String[] { "groupid", "seedid" };
//		types = new String[] { "integer", "integer" };
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("GROUPSEEDPERMISSION", colNames, types));
	}
	
	/**
	 * Get default image for insight
	 * @param appId
	 * @param insightId
	 * @return
	 */
	public static File getStockImage(String appId, String insightId) {
		String imageDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER) + "/images/stock/";
		String query = "SELECT LAYOUT FROM INSIGHT WHERE INSIGHT.ENGINEID='" + appId + "' AND INSIGHT.INSIGHTID='" + insightId + "'";
		String layout = null;
		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
		try {
			while(wrapper.hasNext()) {
				layout = wrapper.next().getValues()[0].toString();
			} 
		} finally {
			wrapper.cleanUp();
		}
		
		if(layout == null) {
			return null;
		}
		
		if(layout.equals("area")) {
			return new File(imageDir + "area.png");
		} else if(layout.equals("column")) {
			return new File(imageDir + "bar.png");
		} else if(layout.equals("boxwhisker")) {
			return new File(imageDir + "boxwhisker.png");
		} else if(layout.equals("bubble")) {
			return new File(imageDir + "bubble.png");
		} else if(layout.equals("choropleth")) {
			return new File(imageDir + "choropleth.png");
		} else if(layout.equals("cloud")) {
			return new File(imageDir + "cloud.png");
		} else if(layout.equals("cluster")) {
			return new File(imageDir + "cluster.png");
		} else if(layout.equals("dendrogram")) {
			return new File(imageDir + "dendrogram-echarts.png");
		} else if(layout.equals("funnel")) {
			return new File(imageDir + "funnel.png");
		} else if(layout.equals("gauge")) {
			return new File(imageDir + "gauge.png");
		} else if(layout.equals("graph")) {
			return new File(imageDir + "graph.png");
		} else if(layout.equals("grid")) {
			return new File(imageDir + "grid.png");
		} else if(layout.equals("heatmap")) {
			return new File(imageDir + "heatmap.png");
		} else if(layout.equals("infographic")) {
			return new File(imageDir + "infographic.png");
		} else if(layout.equals("line")) {
			return new File(imageDir + "line.png");
		} else if(layout.equals("map")) {
			return new File(imageDir + "map.png");
		} else if(layout.equals("pack")) {
			return new File(imageDir + "pack.png");
		} else if(layout.equals("parallelcoordinates")) {
			return new File(imageDir + "parallel-coordinates.png");
		} else if(layout.equals("pie")) {
			return new File(imageDir + "pie.png");
		} else if(layout.equals("polar")) {
			return new File(imageDir + "polar-bar.png");
		} else if(layout.equals("radar")) {
			return new File(imageDir + "radar.png");
		} else if(layout.equals("sankey")) {
			return new File(imageDir + "sankey.png");
		} else if(layout.equals("scatter")) {
			return new File(imageDir + "scatter.png");
		} else if(layout.equals("scatterplotmatrix")) {
			return new File(imageDir + "scatter-matrix.png");
		} else if(layout.equals("singleaxiscluster")) {
			return new File(imageDir + "single-axis.png");
		} else if(layout.equals("sunburst")) {
			return new File(imageDir + "sunburst.png");
		} else if(layout.equals("text-widget")) {
			return new File(imageDir + "text-widget.png");
		} else if(layout.equals("treemap")) {
			return new File(imageDir + "treemap.png");
		} else {
			return new File(imageDir + "color-logo.png");
		}
	}
	
	/**
	 * Need to escape single quotes for sql queries
	 * @param s
	 * @return
	 */
	protected static String escapeForSQLStatement(String s) {
		return s.replaceAll("'", "''");
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	
	
	/*
	 * Utility methods
	 */
	
	/**
	 * Utility method to flush result set into list
	 * Assumes single return at index 0
	 * @param wrapper
	 * @return
	 */
	static List<String> flushToListString(IRawSelectWrapper wrapper) {
		List<String> values = new Vector<String>();
		while(wrapper.hasNext()) {
			values.add(wrapper.next().getValues()[0].toString());
		}
		return values;
	}
	
	/**
	 * Utility method to flush result set into set
	 * Assumes single return at index 0
	 * @param wrapper
	 * @return
	 */
	static Set<String> flushToSetString(IRawSelectWrapper wrapper, boolean order) {
		Set<String> values = null;
		if(order) {
			values = new TreeSet<String>();
		} else {
			values = new HashSet<String>();
		}
		while(wrapper.hasNext()) {
			values.add(wrapper.next().getValues()[0].toString());
		}
		return values;
	}
	
	static List<String[]> flushRsToListOfStrArray(IRawSelectWrapper wrapper) {
		List<String[]> ret = new ArrayList<String[]>();
		while(wrapper.hasNext()) {
			IHeadersDataRow headerRow = wrapper.next();
			Object[] values = headerRow.getValues();
			int len = values.length;
			String[] strVals = new String[len];
			for(int i = 0; i < len; i++) {
				strVals[i] = values[i] + "";
			}
			ret.add(strVals);
		}
		return ret;
	}
	
	static List<Object[]> flushRsToMatrix(IRawSelectWrapper wrapper) {
		List<Object[]> ret = new ArrayList<Object[]>();
		while(wrapper.hasNext()) {
			ret.add(wrapper.next().getValues());
		}
		return ret;
	}
	
	static List<Map<String, Object>> flushRsToMap(IRawSelectWrapper wrapper) {
		List<Map<String, Object>> result = new Vector<Map<String, Object>>();
		while(wrapper.hasNext()) {
			IHeadersDataRow headerRow = wrapper.next();
			String[] headers = headerRow.getHeaders();
			Object[] values = headerRow.getValues();
			Map<String, Object> map = new HashMap<String, Object>();
			for(int i = 0; i < headers.length; i++) {
				map.put(headers[i], values[i]);
			}
			result.add(map);
		}
		return result;
	}
	
	static String createFilter(String... filterValues) {
		StringBuilder b = new StringBuilder();
		boolean hasData = false;
		if(filterValues.length > 0) {
			hasData = true;
			b.append(" IN (");
			b.append("'").append(filterValues[0]).append("'");
			for(int i = 1; i < filterValues.length; i++) {
				b.append(", '").append(filterValues[i]).append("'");
			}
		}
		if(hasData) {
			b.append(")");
		}
		return b.toString();
	}
	
	static String createFilter(List<String> filterValues) {
		StringBuilder b = new StringBuilder();
		boolean hasData = false;
		if(filterValues.size() > 0) {
			hasData = true;
			b.append(" IN (");
			b.append("'").append(filterValues.get(0)).append("'");
			for(int i = 1; i < filterValues.size(); i++) {
				b.append(", '").append(filterValues.get(i)).append("'");
			}
		}
		if(hasData) {
			b.append(")");
		}
		return b.toString();
	}
	
	static String createFilter(String firstValue, String... filterValues) {
		StringBuilder b = new StringBuilder();
		b.append(" IN (");
		b.append("'").append(firstValue).append("'");
		for(int i = 0; i < filterValues.length; i++) {
			b.append(", '").append(filterValues[i]).append("'");
		}
		b.append(")");
		return b.toString();
	}
}
