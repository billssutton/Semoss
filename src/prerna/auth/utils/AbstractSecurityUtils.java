package prerna.auth.utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jodd.util.BCrypt;
import prerna.auth.AuthProvider;
import prerna.auth.User;
import prerna.ds.util.RdbmsQueryBuilder;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.api.IRawSelectWrapper;
import prerna.engine.impl.OwlSeparatePixelFromConceptual;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.query.querystruct.filters.SimpleQueryFilter;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.sablecc2.om.PixelDataType;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;
import prerna.util.sql.AbstractSqlQueryUtil;

public abstract class AbstractSecurityUtils {

	static RDBMSNativeEngine securityDb;
	static boolean securityEnabled = false;
	static boolean adminSetPublisher = false;
	static String ADMIN_ADDED_USER = "ADMIN_ADDED_USER";
	static boolean anonymousUsersEnabled = false;
	static boolean anonymousUsersUploadData = false;

	static Gson securityGson = new GsonBuilder().disableHtmlEscaping().create();
	
	/**
	 * Only used for static references
	 */
	AbstractSecurityUtils() {
		
	}
	
	public static void loadSecurityDatabase() throws SQLException, IOException {
		securityDb = (RDBMSNativeEngine) Utility.getEngine(Constants.SECURITY_DB);
		SecurityOwlCreator owlCreator = new SecurityOwlCreator(securityDb);
		if(owlCreator.needsRemake()) {
			owlCreator.remakeOwl();
		}
		// Update OWL
		OwlSeparatePixelFromConceptual.fixOwl(securityDb.getProp());
		
		initialize();
		
		Object security = DIHelper.getInstance().getLocalProp(Constants.SECURITY_ENABLED);
		if(security == null) {
			securityEnabled = false;
		} else {
			securityEnabled = (security instanceof Boolean && ((boolean) security) ) || (Boolean.parseBoolean(security.toString()));
		}
		
		Object anonymousUsers = DIHelper.getInstance().getLocalProp(Constants.ANONYMOUS_USER_ALLOWED);
		if(anonymousUsers == null) {
			anonymousUsersEnabled = false;
		} else {
			anonymousUsersEnabled = (anonymousUsers instanceof Boolean && ((boolean) anonymousUsers) ) || (Boolean.parseBoolean(anonymousUsers.toString()));
		}
		
		Object anonymousUsersData = DIHelper.getInstance().getLocalProp(Constants.ANONYMOUS_USER_UPLOAD_DATA);
		if(anonymousUsersData == null) {
			anonymousUsersUploadData = false;
		} else {
			anonymousUsersUploadData = (anonymousUsersData instanceof Boolean && ((boolean) anonymousUsersData) ) || (Boolean.parseBoolean(anonymousUsersData.toString()));
		}
		
		Object adminSetsPublisher = DIHelper.getInstance().getLocalProp(Constants.ADMIN_SET_PUBLISHER);
		if(adminSetsPublisher == null) {
			adminSetPublisher = false;
		} else {
			adminSetPublisher = (adminSetsPublisher instanceof Boolean && ((boolean) adminSetsPublisher) ) || (Boolean.parseBoolean(adminSetsPublisher.toString()));
		}
	}

	public static boolean securityEnabled() {
		return securityEnabled;
	}
	
	public static boolean anonymousUsersEnabled() {
		return securityEnabled && anonymousUsersEnabled;
	}
	
	public static boolean anonymousUserUploadData() {
		return anonymousUsersEnabled() && anonymousUsersUploadData;
	}
	
	public static boolean adminSetPublisher() {
		return securityEnabled && adminSetPublisher;
	}
	
	public static void initialize() throws SQLException {
		String schema = securityDb.getSchema();
		Connection conn = securityDb.getConnection();
		String[] colNames = null;
		String[] types = null;
		Object[] defaultValues = null;
		/*
		 * Currently used
		 */
		
		AbstractSqlQueryUtil queryUtil = securityDb.getQueryUtil();
		boolean allowIfExistsTable = queryUtil.allowsIfExistsTableSyntax();
		boolean allowIfExistsIndexs = queryUtil.allowIfExistsIndexSyntax();
		// ENGINE
		colNames = new String[] { "ENGINENAME", "ENGINEID", "GLOBAL", "TYPE", "COST" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "BOOLEAN", "VARCHAR(255)", "VARCHAR(255)" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("ENGINE", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "ENGINE", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("ENGINE", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("ENGINE_GLOBAL_INDEX", "ENGINE", "GLOBAL"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("ENGINE_ENGINENAME_INDEX", "ENGINE", "ENGINENAME"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("ENGINE_ENGINEID_INDEX", "ENGINE", "ENGINEID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "ENGINE_GLOBAL_INDEX", "ENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("ENGINE_GLOBAL_INDEX", "ENGINE", "GLOBAL"));
			}
			if(!queryUtil.indexExists(securityDb, "ENGINE_ENGINENAME_INDEX", "ENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("ENGINE_ENGINENAME_INDEX", "ENGINE", "ENGINENAME"));
			}
			if(!queryUtil.indexExists(securityDb, "ENGINE_ENGINEID_INDEX", "ENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("ENGINE_ENGINEID_INDEX", "ENGINE", "ENGINEID"));
			}
		}
		
		// ENGINEMETA
		// check if column exists
		// TEMPORARY CHECK! - not sure when added but todays date is 12/16 
		{
			List<String> allCols = queryUtil.getTableColumns(securityDb.getConnection(), "ENGINEMETA", schema);
			// this should return in all upper case
			if(!allCols.contains("METAORDER")) {
				if(allowIfExistsTable) {
					securityDb.insertData(queryUtil.dropTableIfExists("ENGINEMETA"));
				} else if(queryUtil.tableExists(conn, "ENGINEMETA", schema)) {
					securityDb.insertData(queryUtil.dropTable("ENGINEMETA"));
				}
			}
		}
		colNames = new String[] { "ENGINEID", "METAKEY", "METAVALUE", "METAORDER" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "CLOB", "INT" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("ENGINEMETA", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "ENGINEMETA", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("ENGINEMETA", colNames, types));
			}
		}
		
		// ENGINEPERMISSION
		colNames = new String[] { "USERID", "PERMISSION", "ENGINEID", "VISIBILITY" };
		types = new String[] { "VARCHAR(255)", "INT", "VARCHAR(255)", "boolean" };
		defaultValues = new Object[]{null, null, null, true};
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExistsWithDefaults("ENGINEPERMISSION", colNames, types, defaultValues));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "ENGINEPERMISSION", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("ENGINEPERMISSION", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("ENGINEPERMISSION_PERMISSION_INDEX", "ENGINEPERMISSION", "PERMISSION"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("ENGINEPERMISSION_VISIBILITY_INDEX", "ENGINEPERMISSION", "VISIBILITY"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("ENGINEPERMISSION_ENGINEID_INDEX", "ENGINEPERMISSION", "ENGINEID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "ENGINEPERMISSION_PERMISSION_INDEX", "ENGINEPERMISSION", schema)) {
				securityDb.insertData(queryUtil.createIndex("ENGINEPERMISSION_PERMISSION_INDEX", "ENGINEPERMISSION", "PERMISSION"));
			}
			if(!queryUtil.indexExists(securityDb, "ENGINEPERMISSION_VISIBILITY_INDEX", "ENGINEPERMISSION", schema)) {
				securityDb.insertData(queryUtil.createIndex("ENGINEPERMISSION_VISIBILITY_INDEX", "ENGINEPERMISSION", "VISIBILITY"));
			}
			if(!queryUtil.indexExists(securityDb, "ENGINEPERMISSION_ENGINEID_INDEX", "ENGINEPERMISSION", schema)) {
				securityDb.insertData(queryUtil.createIndex("ENGINEPERMISSION_ENGINEID_INDEX", "ENGINEPERMISSION", "ENGINEID"));
			}
		}

		// WORKSPACEENGINE
		colNames = new String[] {"TYPE", "USERID", "ENGINEID"};
		types = new String[] {"VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)"};
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("WORKSPACEENGINE", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "WORKSPACEENGINE", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("WORKSPACEENGINE", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("WORKSPACEENGINE_TYPE_INDEX", "WORKSPACEENGINE", "TYPE"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("WORKSPACEENGINE_USERID_INDEX", "WORKSPACEENGINE", "USERID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "WORKSPACEENGINE_TYPE_INDEX", "WORKSPACEENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("WORKSPACEENGINE_TYPE_INDEX", "WORKSPACEENGINE", "TYPE"));
			}
			if(!queryUtil.indexExists(securityDb, "WORKSPACEENGINE_USERID_INDEX", "WORKSPACEENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("WORKSPACEENGINE_USERID_INDEX", "WORKSPACEENGINE", "USERID"));
			}			
		}
		
		// ASSETENGINE
		colNames = new String[] {"TYPE", "USERID", "ENGINEID"};
		types = new String[] {"VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)"};
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("ASSETENGINE", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "ASSETENGINE", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("ASSETENGINE", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("ASSETENGINE_TYPE_INDEX", "ASSETENGINE", "TYPE"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("ASSETENGINE_USERID_INDEX", "ASSETENGINE", "USERID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "ASSETENGINE_TYPE_INDEX", "ASSETENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("ASSETENGINE_TYPE_INDEX", "ASSETENGINE", "TYPE"));
			}
			if(!queryUtil.indexExists(securityDb, "ASSETENGINE_USERID_INDEX", "ASSETENGINE", schema)) {
				securityDb.insertData(queryUtil.createIndex("ASSETENGINE_USERID_INDEX", "ASSETENGINE", "USERID"));
			}
		}
		
		// INSIGHT
		colNames = new String[] { "ENGINEID", "INSIGHTID", "INSIGHTNAME", "GLOBAL", "EXECUTIONCOUNT", "CREATEDON", "LASTMODIFIEDON", "LAYOUT", "CACHEABLE", "RECIPE" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "BOOLEAN", "BIGINT", "TIMESTAMP", "TIMESTAMP", "VARCHAR(255)", "BOOLEAN", "CLOB" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("INSIGHT", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "INSIGHT", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("INSIGHT", colNames, types));
			}
		}
		// INSIGHT RECIPE
		// check if column exists
		// TEMPORARY CHECK! - not sure when added but todays date is 12/16 
		{
			List<String> allCols = queryUtil.getTableColumns(securityDb.getConnection(), "INSIGHT", schema);
			// this should return in all upper case
			if(!allCols.contains("RECIPE")) {
				String addRecipeColumnSql = queryUtil.alterTableAddColumn("INSIGHT", "RECIPE", "CLOB");
				securityDb.insertData(addRecipeColumnSql);
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("INSIGHT_LASTMODIFIEDON_INDEX", "INSIGHT", "LASTMODIFIEDON"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("INSIGHT_GLOBAL_INDEX", "INSIGHT", "GLOBAL"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("INSIGHT_ENGINEID_INDEX", "INSIGHT", "ENGINEID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "INSIGHT_LASTMODIFIEDON_INDEX", "INSIGHT", schema)) {
				securityDb.insertData(queryUtil.createIndex("INSIGHT_LASTMODIFIEDON_INDEX", "INSIGHT", "LASTMODIFIEDON"));
			}
			if(!queryUtil.indexExists(securityDb, "INSIGHT_GLOBAL_INDEX", "INSIGHT", schema)) {
				securityDb.insertData(queryUtil.createIndex("INSIGHT_GLOBAL_INDEX", "INSIGHT", "GLOBAL"));
			}
			if(!queryUtil.indexExists(securityDb, "INSIGHT_ENGINEID_INDEX", "INSIGHT", schema)) {
				securityDb.insertData(queryUtil.createIndex("INSIGHT_ENGINEID_INDEX", "INSIGHT", "ENGINEID"));
			}
		}


		// USERINSIGHTPERMISSION
		colNames = new String[] { "USERID", "ENGINEID", "INSIGHTID", "PERMISSION" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "INTEGER" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("USERINSIGHTPERMISSION", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "USERINSIGHTPERMISSION", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("USERINSIGHTPERMISSION", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("USERINSIGHTPERMISSION_PERMISSION_INDEX", "USERINSIGHTPERMISSION", "PERMISSION"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("USERINSIGHTPERMISSION_ENGINEID_INDEX", "USERINSIGHTPERMISSION", "ENGINEID"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("USERINSIGHTPERMISSION_USERID_INDEX", "USERINSIGHTPERMISSION", "USERID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "USERINSIGHTPERMISSION_PERMISSION_INDEX", "USERINSIGHTPERMISSION", schema)) {
				securityDb.insertData(queryUtil.createIndex("USERINSIGHTPERMISSION_PERMISSION_INDEX", "USERINSIGHTPERMISSION", "PERMISSION"));
			}
			if(!queryUtil.indexExists(securityDb, "USERINSIGHTPERMISSION_ENGINEID_INDEX", "USERINSIGHTPERMISSION", schema)) {
				securityDb.insertData(queryUtil.createIndex("USERINSIGHTPERMISSION_ENGINEID_INDEX", "USERINSIGHTPERMISSION", "ENGINEID"));
			}
			if(!queryUtil.indexExists(securityDb, "USERINSIGHTPERMISSION_USERID_INDEX", "USERINSIGHTPERMISSION", schema)) {
				securityDb.insertData(queryUtil.createIndex("USERINSIGHTPERMISSION_USERID_INDEX", "USERINSIGHTPERMISSION", "USERID"));
			}
		}
		
		// INSIGHTMETA
		colNames = new String[] { "ENGINEID", "INSIGHTID", "METAKEY", "METAVALUE", "METAORDER" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "CLOB", "INT" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("INSIGHTMETA", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "INSIGHTMETA", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("INSIGHTMETA", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("INSIGHTMETA_ENGINEID_INDEX", "INSIGHT", "ENGINEID"));
			securityDb.insertData(queryUtil.createIndexIfNotExists("INSIGHTMETA_INSIGHTID_INDEX", "INSIGHT", "INSIGHTID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "INSIGHTMETA_ENGINEID_INDEX", "INSIGHT", schema)) {
				securityDb.insertData(queryUtil.createIndex("INSIGHTMETA_ENGINEID_INDEX", "INSIGHT", "ENGINEID"));
			}
			if(!queryUtil.indexExists(securityDb, "INSIGHTMETA_INSIGHTID_INDEX", "INSIGHT", schema)) {
				securityDb.insertData(queryUtil.createIndex("INSIGHTMETA_INSIGHTID_INDEX", "INSIGHT", "INSIGHTID"));
			}
		}

		// USER
		colNames = new String[] { "NAME", "EMAIL", "TYPE", "ID", "PASSWORD", "SALT", "USERNAME", "ADMIN", "PUBLISHER" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "BOOLEAN", "BOOLEAN" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("USER", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "USER", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("USER", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			securityDb.insertData(queryUtil.createIndexIfNotExists("USER_ID_INDEX", "USER", "ID"));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "USER_ID_INDEX", "USER", schema)) {
				securityDb.insertData(queryUtil.createIndex("USER_ID_INDEX", "USER", "ID"));
			}
		}
		
		// PERMISSION
		colNames = new String[] { "ID", "NAME" };
		types = new String[] { "INT", "VARCHAR(255)" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("PERMISSION", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "PERMISSION", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("PERMISSION", colNames, types));
			}
		}
		if(allowIfExistsIndexs) {
			List<String> iCols = new Vector<String>();
			iCols.add("ID");
			iCols.add("NAME");
			securityDb.insertData(queryUtil.createIndexIfNotExists("PERMISSION_ID_NAME_INDEX", "PERMISSION", iCols));
		} else {
			// see if index exists
			if(!queryUtil.indexExists(securityDb, "PERMISSION_ID_NAME_INDEX", "PERMISSION", schema)) {
				List<String> iCols = new Vector<String>();
				iCols.add("ID");
				iCols.add("NAME");
				securityDb.insertData(queryUtil.createIndex("PERMISSION_ID_NAME_INDEX", "PERMISSION", iCols));
			}
		}
		
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, "select count(*) from permission");
			if(wrapper.hasNext()) {
				int numrows = ((Number) wrapper.next().getValues()[0]).intValue();
				if(numrows > 3) {
					securityDb.removeData("DELETE FROM PERMISSION WHERE 1=1;");
					securityDb.insertData(queryUtil.insertIntoTable("PERMISSION", colNames, types, new Object[]{1, "OWNER"}));
					securityDb.insertData(queryUtil.insertIntoTable("PERMISSION", colNames, types, new Object[]{2, "EDIT"}));
					securityDb.insertData(queryUtil.insertIntoTable("PERMISSION", colNames, types, new Object[]{3, "READ_ONLY"}));
				} else if(numrows == 0) {
					securityDb.insertData(queryUtil.insertIntoTable("PERMISSION", colNames, types, new Object[]{1, "OWNER"}));
					securityDb.insertData(queryUtil.insertIntoTable("PERMISSION", colNames, types, new Object[]{2, "EDIT"}));
					securityDb.insertData(queryUtil.insertIntoTable("PERMISSION", colNames, types, new Object[]{3, "READ_ONLY"}));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}
		

		// ACCESSREQUEST
		colNames = new String[] { "ID", "SUBMITTEDBY", "ENGINE", "PERMISSION" };
		types = new String[] { "VARCHAR(255)", "VARCHAR(255)", "VARCHAR(255)", "INT" };
		if(allowIfExistsTable) {
			securityDb.insertData(queryUtil.createTableIfNotExists("ACCESSREQUEST", colNames, types));
		} else {
			// see if table exists
			if(!queryUtil.tableExists(conn, "ACCESSREQUEST", schema)) {
				// make the table
				securityDb.insertData(queryUtil.createTable("ACCESSREQUEST", colNames, types));
			}
		}
		
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		
		/*
		 * Tables accounted for that we are not using yet...
		 */
		
//		// USERGROUP
//		colNames = new String[] { "groupid", "name", "owner" };
//		types = new String[] { "int identity", "varchar(255)", "varchar(255)" };
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("USERGROUP", colNames, types));
//		
//		// GROUPMEMBERS
//		colNames = new String[] {"groupmembersid", "groupid", "userid"};
//		types = new String[] {"int identity", "integer", "varchar(255)"};
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("GROUPMEMBERS", colNames, types));
//		
//		// ENGINEGROUPMEMBERVISIBILITY
//		colNames = new String[] { "id", "groupenginepermissionid", "groupmembersid", "visibility" };
//		types = new String[] { "int identity", "integer", "integer", "boolean" };
//		defaultValues = new Object[]{null, null, null, true};
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreateWithDefault("ENGINEGROUPMEMBERVISIBILITY", colNames, types, defaultValues));
//
//		// GROUPENGINEPERMISSION
//		colNames = new String[] {"groupenginepermissionid", "groupid", "permission", "engine"};
//		types = new String[] {"int identity", "integer", "integer", "varchar(255)"};
//		securityDb.insertData(RdbmsQueryBuilder.makeOptionalCreate("GROUPENGINEPERMISSION", colNames, types));
//		
//		// FOREIGN KEYS FOR CASCASDE DELETE
//		wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, "select count(*) from INFORMATION_SCHEMA.CONSTRAINTS where constraint_name='FK_GROUPENGINEPERMISSION'");
//		if(wrapper.hasNext()) {
//			if( ((Number) wrapper.next().getValues()[0]).intValue() == 0) {
//				securityDb.insertData("ALTER TABLE ENGINEGROUPMEMBERVISIBILITY ADD CONSTRAINT FK_GROUPENGINEPERMISSION FOREIGN KEY (GROUPENGINEPERMISSIONID) REFERENCES GROUPENGINEPERMISSION(GROUPENGINEPERMISSIONID) ON DELETE CASCADE;");
//			}
//		}
//		wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, "select count(*) from INFORMATION_SCHEMA.CONSTRAINTS where constraint_name='FK_GROUPMEMBERSID'");
//		if(wrapper.hasNext()) {
//			if( ((Number) wrapper.next().getValues()[0]).intValue() == 0) {
//				securityDb.insertData("ALTER TABLE ENGINEGROUPMEMBERVISIBILITY ADD CONSTRAINT FK_GROUPMEMBERSID FOREIGN KEY (GROUPMEMBERSID) REFERENCES GROUPMEMBERS (GROUPMEMBERSID) ON DELETE CASCADE;");
//			}
//		}
//				
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
	 * Does this engine name already exist
	 * @param user
	 * @param appName
	 * @return
	 */
	public static boolean userContainsEngineName(User user, String appName) {
		if(ignoreEngine(appName)) {
			// dont add local master or security db to security db
			return true;
		}
//		String userFilters = getUserFilters(user);
//		String query = "SELECT * "
//				+ "FROM ENGINE "
//				+ "INNER JOIN ENGINEPERMISSION ON ENGINE.ENGINEID=ENGINEPERMISSION.ENGINEID "
//				+ "WHERE ENGINENAME='" + appName + "' AND PERMISSION IN (1,2) AND ENGINEPERMISSION.USERID IN " + userFilters;
//		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
		
		SelectQueryStruct qs = new SelectQueryStruct();
		qs.addSelector(new QueryColumnSelector("ENGINE__ENGINEID"));
		qs.addRelation("ENGINE", "ENGINEPERMISSION", "inner.join");
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("ENGINE__ENGINENAME", "==", appName));
		List<Integer> permissionValues = new Vector<Integer>(2);
		permissionValues.add(new Integer(1));
		permissionValues.add(new Integer(2));
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("ENGINEPERMISSION__PERMISSION", "==", permissionValues, PixelDataType.CONST_INT));
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("ENGINEPERMISSION__USERID", "==", getUserFiltersQs(user)));
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, qs);
			if(wrapper.hasNext()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}
		
		return false;
	}
	
	public static boolean containsEngineName(String appName) {
		if(ignoreEngine(appName)) {
			// dont add local master or security db to security db
			return true;
		}
//		String query = "SELECT ENGINEID FROM ENGINE WHERE ENGINENAME='" + appName + "'";
//		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);

		SelectQueryStruct qs = new SelectQueryStruct();
		qs.addSelector(new QueryColumnSelector("ENGINE__ENGINEID"));
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("ENGINE__ENGINENAME", "==", appName));
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, qs);
			if(wrapper.hasNext()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}
		
		return false;
	}
	
	public static boolean containsEngineId(String appId) {
		if(ignoreEngine(appId)) {
			// dont add local master or security db to security db
			return true;
		}
//		String query = "SELECT ENGINEID FROM ENGINE WHERE ENGINEID='" + appId + "'";
//		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
		
		SelectQueryStruct qs = new SelectQueryStruct();
		qs.addSelector(new QueryColumnSelector("ENGINE__ENGINEID"));
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("ENGINE__ENGINEID", "==", appId));
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, qs);
			if(wrapper.hasNext()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}
		
		return false;
	}
	
	static boolean ignoreEngine(String appId) {
		if(appId.equals(Constants.LOCAL_MASTER_DB_NAME) || appId.equals(Constants.SECURITY_DB) || appId.equals(Constants.SCHEDULER_DB) ) {
			// dont add local master or security db to security db
			return true;
		}
		// engine is an asset
		if(WorkspaceAssetUtils.isAssetApp(appId)) {
			return true;
		}
		// so that way all those Asset apps do not appear a bunch of times
		String smssFile = DIHelper.getInstance().getCoreProp().getProperty(appId + "_" + Constants.STORE);
		if(smssFile != null && new File(smssFile).exists()) {
			Properties prop = Utility.loadProperties(smssFile);
			return Boolean.parseBoolean(prop.get(Constants.IS_ASSET_APP) + "");
		}
		
		return false;
	}
	
	/**
	 * Get default image for insight
	 * @param appId
	 * @param insightId
	 * @return
	 */
	public static File getStockImage(String appId, String insightId) {
		String imageDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER) + "/images/stock/";
		String layout = null;

//		String query = "SELECT LAYOUT FROM INSIGHT WHERE INSIGHT.ENGINEID='" + appId + "' AND INSIGHT.INSIGHTID='" + insightId + "'";
//		IRawSelectWrapper wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
		
		SelectQueryStruct qs = new SelectQueryStruct();
		qs.addSelector(new QueryColumnSelector("INSIGHT__LAYOUT"));
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("INSIGHT__ENGINEID", "==", appId));
		qs.addExplicitFilter(SimpleQueryFilter.makeColToValFilter("INSIGHT__INSIGHTID", "==", insightId));
		
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, qs);
			if(wrapper.hasNext()) {
				layout = wrapper.next().getValues()[0].toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}
		
		if(layout == null) {
			return null;
		}
		
		if(layout.equalsIgnoreCase("area")) {
			return new File(imageDir + "area.png");
		} else if(layout.equalsIgnoreCase("column")) {
			return new File(imageDir + "bar.png");
		} else if(layout.equalsIgnoreCase("boxwhisker")) {
			return new File(imageDir + "boxwhisker.png");
		} else if(layout.equalsIgnoreCase("bubble")) {
			return new File(imageDir + "bubble.png");
		} else if(layout.equalsIgnoreCase("choropleth")) {
			return new File(imageDir + "choropleth.png");
		} else if(layout.equalsIgnoreCase("cloud")) {
			return new File(imageDir + "cloud.png");
		} else if(layout.equalsIgnoreCase("cluster")) {
			return new File(imageDir + "cluster.png");
		} else if(layout.equalsIgnoreCase("dendrogram")) {
			return new File(imageDir + "dendrogram-echarts.png");
		} else if(layout.equalsIgnoreCase("funnel")) {
			return new File(imageDir + "funnel.png");
		} else if(layout.equalsIgnoreCase("gauge")) {
			return new File(imageDir + "gauge.png");
		} else if(layout.equalsIgnoreCase("graph")) {
			return new File(imageDir + "graph.png");
		} else if(layout.equalsIgnoreCase("grid")) {
			return new File(imageDir + "grid.png");
		} else if(layout.equalsIgnoreCase("heatmap")) {
			return new File(imageDir + "heatmap.png");
		} else if(layout.equalsIgnoreCase("infographic")) {
			return new File(imageDir + "infographic.png");
		} else if(layout.equalsIgnoreCase("line")) {
			return new File(imageDir + "line.png");
		} else if(layout.equalsIgnoreCase("map")) {
			return new File(imageDir + "map.png");
		} else if(layout.equalsIgnoreCase("pack")) {
			return new File(imageDir + "pack.png");
		} else if(layout.equalsIgnoreCase("parallelcoordinates")) {
			return new File(imageDir + "parallel-coordinates.png");
		} else if(layout.equalsIgnoreCase("pie")) {
			return new File(imageDir + "pie.png");
		} else if(layout.equalsIgnoreCase("polar")) {
			return new File(imageDir + "polar-bar.png");
		} else if(layout.equalsIgnoreCase("radar")) {
			return new File(imageDir + "radar.png");
		} else if(layout.equalsIgnoreCase("sankey")) {
			return new File(imageDir + "sankey.png");
		} else if(layout.equalsIgnoreCase("scatter")) {
			return new File(imageDir + "scatter.png");
		} else if(layout.equalsIgnoreCase("scatterplotmatrix")) {
			return new File(imageDir + "scatter-matrix.png");
		} else if(layout.equalsIgnoreCase("singleaxiscluster")) {
			return new File(imageDir + "single-axis.png");
		} else if(layout.equalsIgnoreCase("sunburst")) {
			return new File(imageDir + "sunburst.png");
		} else if(layout.equalsIgnoreCase("text-widget")) {
			return new File(imageDir + "text-widget.png");
		} else if(layout.equalsIgnoreCase("treemap")) {
			return new File(imageDir + "treemap.png");
		} else {
			return new File(imageDir + "color-logo.png");
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	
	
	/*
	 * Utility methods
	 */
	
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
	
	static String createFilter(Collection<String> filterValues) {
		if(filterValues.isEmpty()) {
			return " IN () ";
		}
		StringBuilder b = new StringBuilder();
		boolean hasData = false;
		if(filterValues.size() > 0) {
			hasData = true;
			b.append(" IN (");
			Iterator<String> iterator = filterValues.iterator();
			b.append("'").append(RdbmsQueryBuilder.escapeForSQLStatement(iterator.next())).append("'");
			while(iterator.hasNext()) {
				b.append(", '").append(RdbmsQueryBuilder.escapeForSQLStatement(iterator.next())).append("'");
			}
		}
		if(hasData) {
			b.append(")");
		}
		return b.toString();
	}
	
	/**
	 * Get all ids from user object
	 * @param user
	 * @return
	 */
	static String getUserFilters(User user) {
		StringBuilder b = new StringBuilder();
		b.append("(");
		if(user != null) {
			List<AuthProvider> logins = user.getLogins();
			if(!logins.isEmpty()) {
				int numLogins = logins.size();
				b.append("'").append(RdbmsQueryBuilder.escapeForSQLStatement(user.getAccessToken(logins.get(0)).getId())).append("'");
				for(int i = 1; i < numLogins; i++) {
					b.append(", '").append(RdbmsQueryBuilder.escapeForSQLStatement(user.getAccessToken(logins.get(i)).getId())).append("'");
				}
			}
		}
		b.append(")");
		return b.toString();
	}
	
	/**
	 * Get a vector of the user ids
	 * @param user
	 * @return
	 */
	static Collection<String> getUserFiltersQs(User user) {
		List<String> filters = new Vector<String>();
		if(user != null) {
			List<AuthProvider> logins = user.getLogins();
			for(AuthProvider thisLogin : logins) {
				filters.add(user.getAccessToken(thisLogin).getId());
			}
		}
		
		return filters;
	}
	
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns a list of values given a query with one column/variable.
	 * @param query		Query to be executed
	 * @return			
	 */
	static List<Map<String, Object>> getSimpleQuery(String query) {
		List<Map<String, Object>> ret = new Vector<Map<String, Object>>();
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, query);
			while(wrapper.hasNext()) {
				IHeadersDataRow row = wrapper.next();
				String[] headers = row.getHeaders();
				Object[] values = row.getValues();
				Map<String, Object> rowData = new HashMap<String, Object>();
				for(int idx = 0; idx < headers.length; idx++){
					if(values[idx] == null) {
						rowData.put(headers[idx].toLowerCase(), "null");
					} else {
						rowData.put(headers[idx].toLowerCase(), values[idx]);
					}
				}
				ret.add(rowData);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}

		return ret;
	}
	
	/**
	 * Returns a list of values given a query with one column/variable.
	 * @param qs		Query Struct to be executed
	 * @return			
	 */
	static List<Map<String, Object>> getSimpleQuery(SelectQueryStruct qs) {
		List<Map<String, Object>> ret = new Vector<Map<String, Object>>();
		IRawSelectWrapper wrapper = null;
		try {
			wrapper = WrapperManager.getInstance().getRawWrapper(securityDb, qs);
			while(wrapper.hasNext()) {
				IHeadersDataRow row = wrapper.next();
				String[] headers = row.getHeaders();
				Object[] values = row.getValues();
				Map<String, Object> rowData = new HashMap<String, Object>();
				for(int idx = 0; idx < headers.length; idx++){
					if(values[idx] == null) {
						rowData.put(headers[idx].toLowerCase(), "null");
					} else {
						rowData.put(headers[idx].toLowerCase(), values[idx]);
					}
				}
				ret.add(rowData);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}

		return ret;
	}
	
	
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////

	public static String validEmail(String email){
		if(email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")){
			return  email + " is not a valid email address. ";
		}
		return "";
	}
	
	public static String validPassword(String password){
		if(password == null || password.isEmpty()) {
			return "Password cannot be empty.";
		}
		Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])(?=.{8,})");
        Matcher matcher = pattern.matcher(password);
		
		if(!matcher.lookingAt()){
			return "Password doesn't comply with the security policies.";
		}
		return "";
	}
	
	/**
	 * Current salt generation by BCrypt
	 * @return salt
	 */
	public static String generateSalt(){
		return BCrypt.gensalt();
	}

	/**
	 * Create the password hash based on the password and salt provided.
	 * @param password
	 * @param salt
	 * @return hash
	 */
	public static String hash(String password, String salt) {
		return BCrypt.hashpw(password, salt);
	}
}
