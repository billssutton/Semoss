package prerna.util.sql;

import java.util.ArrayList;
import java.util.List;

public class OracleQueryUtil extends SQLQueryUtil {
	public static final String DATABASE_DRIVER = "oracle.jdbc.driver.OracleDriver";
	private String connectionBase = "jdbc:oracle:thin:@HOST:PORT/SERVICE";

	public OracleQueryUtil(){
		setDialect();
		super.setDefaultDbUserName("root");//
		super.setDefaultDbPassword("password");//
	}
	
	public OracleQueryUtil(String hostname, String port, String schema, String username, String password) {
		setDialect();
		connectionBase = connectionBase.replace("HOST", hostname).replace("SERVICE", schema);
		if(port != null && !port.isEmpty()) {
			connectionBase = connectionBase.replace(":PORT", ":" + port);
		} else {
			connectionBase = connectionBase.replace(":PORT", "");
		}
		super.setDefaultDbUserName(username);
		super.setDefaultDbPassword(password);
	}
	
	public OracleQueryUtil(String connectionURL, String username, String password) {
		setDialect();
		connectionBase = connectionURL;
		super.setDefaultDbUserName(username);
		super.setDefaultDbPassword(password);
	}
	
	private void setDialect() {
		super.setDialectAllTables(" SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES ");
		super.setDialectAllColumns(" SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ");
		super.setResultAllTablesTableName("TABLE_NAME");//
		super.setResultAllColumnsColumnName("COLUMN_NAME");
		super.setResultAllColumnsColumnType("DATA_TYPE");
		super.setDialectOuterJoinLeft(" LEFT OUTER JOIN ");
		super.setDialectOuterJoinRight(" RIGHT OUTER JOIN ");
	}

	@Override
	public SQLQueryUtil.DB_TYPE getDatabaseType(){
		return SQLQueryUtil.DB_TYPE.Oracle;
	}
	
	public String getDialectAllColumns(String tableName){
		return super.getDialectAllColumns() + "'" + tableName + "'" ;
	}
	
	@Override
	public String getDialectAllIndexesInDB(String schema){
		return super.getDialectAllIndexesInDB();
	}
	
	//jdbc:oracle:thin:@<hostname>[:port]/<service or sid>[-schema name]
	@Override
	public String getConnectionURL(String baseFolder,String dbname){
		return connectionBase + "-" + dbname;
	}
	
	@Override
	public String getTempConnectionURL(){
		return connectionBase;
	}

	@Override
	public String getDatabaseDriverClassName(){
		return DATABASE_DRIVER;
	}

	@Override
	public String getEngineNameFromConnectionURL(String connectionURL) {
		String splitConnectionURL[] = connectionURL.split("=");
		String engineName[] = splitConnectionURL[1].split(";");
		return engineName[0];
	}

	@Override
	public String getDefaultOuterJoins() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDialectIndexInfo(String indexName, String dbName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDialectFullOuterJoinQuery(boolean distinct,
			String selectors, List<String> rightJoinsArr,
			List<String> leftJoinsArr, List<String> joinsArr, String filters,
			int limit, String groupBy) {
		// TODO Auto-generated method stub
		return null;
	}
}
