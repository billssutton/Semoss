package prerna.util.sql;

import prerna.algorithm.api.ITableDataFrame;
import prerna.engine.api.IEngine;
import prerna.query.interpreters.IQueryInterpreter;
import prerna.query.interpreters.sql.HiveSqlInterpreter;

public class HiveQueryUtil  extends AnsiSqlQueryUtil {

	HiveQueryUtil() {
		super();
		setDbType(RdbmsTypeEnum.HIVE);
	}
	
	HiveQueryUtil(String connectionUrl, String username, String password) {
		super(connectionUrl, username, password);
		setDbType(RdbmsTypeEnum.HIVE);
	}
	
	HiveQueryUtil(RdbmsTypeEnum dbType, String hostname, String port, String schema, String username, String password) {
		super(dbType, hostname, port, schema, username, password);
	}
	
	@Override
	public IQueryInterpreter getInterpreter(IEngine engine) {
		return new HiveSqlInterpreter(engine);
	}

	@Override
	public IQueryInterpreter getInterpreter(ITableDataFrame frame) {
		return new HiveSqlInterpreter(frame);
	}
	
}
