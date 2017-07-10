/*******************************************************************************
 * Copyright 2015 Defense Health Agency (DHA)
 *
 * If your use of this software does not include any GPLv2 components:
 * 	Licensed under the Apache License, Version 2.0 (the "License");
 * 	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 *
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 * ----------------------------------------------------------------------------
 * If your use of this software includes any GPLv2 components:
 * 	This program is free software; you can redistribute it and/or
 * 	modify it under the terms of the GNU General Public License
 * 	as published by the Free Software Foundation; either version 2
 * 	of the License, or (at your option) any later version.
 *
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *******************************************************************************/
package prerna.rdf.main; // TODO: move to prerna.poi.main

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.engine.api.IEngine;
import prerna.poi.main.AbstractEngineCreator;
import prerna.poi.main.RDBMSEngineCreationHelper;
import prerna.poi.main.helper.ImportOptions;
import prerna.util.DIHelper;
import prerna.util.sql.SQLQueryUtil;

public class ImportRDBMSProcessor extends AbstractEngineCreator {
	static final Logger logger = LogManager.getLogger(ImportRDBMSProcessor.class.getName());

	private String connectionURL;
	private String schema;
	private String baseFolder = DIHelper.getInstance().getProperty("BaseFolder");

	private final String MYSQL = "MySQL";
	private final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

	private final String ORACLE = "Oracle";
	private final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";

	private final String SQLSERVER = "SQL_Server";
	private final String SQLSERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

	private final String ASTER = "ASTER_DB";
	private final String ASTER_DRIVER = "com.asterdata.ncluster.jdbc.core.NClusterJDBCDriver";

	private final String SAP_HANA = "SAP_HANA";
	private final String SAP_HANA_DRIVER = "com.sap.db.jdbc.Driver";

	private final String DB2 = "DB2";
	private final String DB2_DRIVER = "com.ibm.db2.jcc.DB2Driver";

	private final String MARIADB = "MARIA_DB";
	private final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";

	private final String H2 = "H2_DB";
	private final String H2_DRIVER = "org.h2.Driver";

	private final String TERADATA = "TERADATA";
	private final String TERADATA_DRIVER = "com.teradata.jdbc.TeraDriver";

	private final String IMPALA = "IMPALA";
	private final String IMPALA_DRIVER = "com.cloudera.impala.jdbc3.Driver";

	private final String POSTGRES = "POSTGRES";
	private final String POSTGRES_DRIVER = "org.postgresql.Driver";

	private final String PHOENIX = "PHOENIX";
	private final String PHOENIX_DRIVER = "org.apache.phoenix.jdbc.PhoenixDriver";

	private final String DERBY = "DERBY";
	private final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	private final String CASSANDRA = "CASSANDRA";
	private final String CASSANDRA_DRIVER = "com.github.adejanovski.cassandra.jdbc.CassandraDriver";

	private Connection buildConnection(String type, String host, String port, String username, String password,
			String schema) throws SQLException {
		Connection con = null;
		String url = "";

		try {
			if (type.equals(this.MYSQL)) {
				Class.forName(this.MYSQL_DRIVER);

				if (connectionURL != null && !connectionURL.isEmpty()) {
					schema = connectionURL.substring(0, connectionURL.indexOf("?")).split("/")[3];
					this.schema = schema;
					return DriverManager.getConnection(connectionURL);
				}

				// Connection URL format:
				// jdbc:mysql://<hostname>[:port]/<DBname>?user=username&password=pw
				url = "jdbc:mysql://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url + "?user=" + username + "&password=" + new String(password));
			} else if (type.equals(this.ORACLE)) {
				Class.forName(this.ORACLE_DRIVER);

				if (connectionURL != null && !connectionURL.isEmpty()) {
					schema = connectionURL.substring(connectionURL.indexOf("/")).split("-")[0];
					this.schema = schema;
					return DriverManager.getConnection(connectionURL);
				}

				// Connection URL format:
				// jdbc:oracle:thin:@<hostname>[:port]/<service or sid>[-schema
				// name]
				url = "jdbc:oracle:thin:@HOST:PORT:SERVICE".replace("HOST", host).replace("SERVICE", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));
			} else if (type.equals(this.SQLSERVER)) {
				Class.forName(this.SQLSERVER_DRIVER);

				if (connectionURL != null && !connectionURL.isEmpty()) {
					schema = connectionURL.substring(connectionURL.indexOf("="));
					this.schema = schema;
					return DriverManager.getConnection(connectionURL);
				}

				// Connection URL format:
				// jdbc:sqlserver://<hostname>[:port];databaseName=<DBname>
				url = "jdbc:sqlserver://HOST:PORT;databaseName=SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));
			} else if (type.equals(this.DB2)) {
				Class.forName(this.DB2_DRIVER);
				if (connectionURL != null && !connectionURL.isEmpty()) {
					schema = connectionURL.split("/")[3];
					this.schema = schema;
					return DriverManager.getConnection(connectionURL);
				}

				// Connection URL format:
				// jdbc:db2://<hostname>[:port]/<databasename>
				url = "jdbc:db2://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.ASTER)) {
				Class.forName(this.ASTER_DRIVER);
				url = "jdbc:ncluster://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.SAP_HANA)) {
				Class.forName(this.SAP_HANA_DRIVER);
				url = "jdbc:sap://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));
			} else if (type.equals(this.MARIADB)) {
				Class.forName(this.MARIADB_DRIVER);
				url = "jdbc:mariadb://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.H2)) {
				Class.forName(this.H2_DRIVER);
				url = "jdbc:h2:tcp://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.TERADATA)) {
				Class.forName(this.TERADATA_DRIVER);
				url = "jdbc:teradata://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.POSTGRES)) {
				Class.forName(this.POSTGRES_DRIVER);
				url = "jdbc:postgresql://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.DERBY)) {
				Class.forName(this.DERBY_DRIVER);
				url = "jdbc:derby://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.CASSANDRA)) {
				Class.forName(this.CASSANDRA_DRIVER);
				url = "jdbc:cassandra://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			} else if (type.equals(this.IMPALA)) {
				Class.forName(this.IMPALA_DRIVER);
				url = "jdbc:impala://HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));
			} else if (type.equals(this.PHOENIX)) {
				Class.forName(this.PHOENIX_DRIVER);
				url = "jdbc:phoenix:HOST:PORT/SCHEMA".replace("HOST", host).replace("SCHEMA", schema);
				if (port != null && !port.isEmpty()) {
					url = url.replace(":PORT", ":" + port);
				} else {
					url = url.replace(":PORT", "");
				}

				this.schema = schema;
				con = DriverManager.getConnection(url, username, new String(password));

			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.out.println(">>>>>DRIVER NOT FOUND. PLEASE ENSURE YOU HAVE ACCESS TO JDBC DRIVER");
		}

		return con;
	}

	public HashMap<String, ArrayList<String>> getAllFields(String type, String host, String port, String username,
			String password, String schema) {
		Statement stmt;
		ResultSet rs;
		String query = "";
		HashMap<String, ArrayList<String>> allTablesAndColumns = new HashMap<String, ArrayList<String>>();

		try {
			Connection con = buildConnection(type, host, port, username, password, schema);

			if (type.equals(this.MYSQL)) {
				query = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='"
						+ this.schema + "' ORDER BY TABLE_NAME;";
			} else if (type.equals(this.ORACLE)) {
				query = "SELECT TABLE_NAME, COLUMN_NAME FROM USER_TAB_COLS ORDER BY TABLE_NAME";
			} else if (type.equals(this.SQLSERVER)) {
				query = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS";
			} else {
				return null;
			}

			stmt = con.createStatement();
			rs = stmt.executeQuery(query);

			while (rs.next()) {
				String tableName = "";
				String columnName = "";

				tableName = rs.getString("TABLE_NAME");
				columnName = rs.getString("COLUMN_NAME");

				if (allTablesAndColumns.get(tableName) == null) {
					allTablesAndColumns.put(tableName, new ArrayList<String>());
				}

				allTablesAndColumns.get(tableName).add(columnName);
			}

			System.out.println("All Tables and Columns: " + allTablesAndColumns);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return allTablesAndColumns;
	}

	public IEngine addNewRDBMS(ImportOptions options) throws IOException {
		SQLQueryUtil.DB_TYPE sqlType = options.getRDBMSDriverType();
		String host = options.getHost();
		String port = options.getPort();
		String schema = options.getSchema();
		String username = options.getUsername();
		String password = options.getPassword();
		String engineName = options.getDbName();
		HashMap<String, Object> externalMetamodel = options.getExternalMetamodel();
		queryUtil = SQLQueryUtil.initialize(sqlType, host, port, schema, username, password);
		prepEngineCreator(null, options.getOwlFileLocation(), options.getSMSSLocation());
		// openRdbmsEngineWithoutConnection(engineName);
		openRdbmsEngineWithConnection(schema, engineName);// added for testing
															// connect to
															// external wf
		HashMap<String, ArrayList<String>> nodesAndProps = (HashMap<String, ArrayList<String>>) externalMetamodel
				.get("nodes");
		ArrayList<String[]> relationships = (ArrayList<String[]>) externalMetamodel.get("relationships");
		Map<String, Map<String, String>> existingRDBMSStructure = RDBMSEngineCreationHelper
				.getExistingRDBMSStructure(engine, queryUtil);
		Map<String, String> nodesAndPrimKeys = new HashMap<String, String>(); // Uncleaned
																				// concepts
																				// and
																				// their
																				// primkeys

		nodesAndPrimKeys = parseNodesAndProps(nodesAndProps, existingRDBMSStructure);
		parseRelationships(relationships, existingRDBMSStructure, nodesAndPrimKeys);
		createBaseRelations(); // TODO: this should be moved into
								// ImportDataProcessor and removed from every
								// subclass of AbstractEngineCreator

		RDBMSEngineCreationHelper.insertAllTablesAsInsights(engine, queryUtil);

		return this.engine;
	}

	private HashMap<String, String> parseNodesAndProps(HashMap<String, ArrayList<String>> nodesAndProps,
			Map<String, Map<String, String>> dataTypes) {
		HashMap<String, String> nodesAndPrimKeys = new HashMap<String, String>(nodesAndProps.size());
		for (String node : nodesAndProps.keySet()) {
			String[] tableAndPrimaryKey = node.split("\\.");
			String nodeName = tableAndPrimaryKey[0];
			String primaryKey = tableAndPrimaryKey[1];
			nodesAndPrimKeys.put(nodeName, primaryKey);

			String cleanConceptTableName = RDBMSEngineCreationHelper.cleanTableName(nodeName);
			owler.addConcept(cleanConceptTableName, primaryKey, dataTypes.get(nodeName).get(primaryKey));
			for (String prop : nodesAndProps.get(node)) {
				if (!prop.equals(primaryKey)) {
					String cleanProp = RDBMSEngineCreationHelper.cleanTableName(prop);
					owler.addProp(cleanConceptTableName, primaryKey, cleanProp, dataTypes.get(nodeName).get(prop));
				}
			}
		}

		return nodesAndPrimKeys;
	}

	private void parseRelationships(ArrayList<String[]> relationships, Map<String, Map<String, String>> dataTypes,
			Map<String, String> nodesAndPrimKeys) {
		for (String[] relationship : relationships) {
			String subject = RDBMSEngineCreationHelper.cleanTableName(relationship[0]);
			String object = RDBMSEngineCreationHelper.cleanTableName(relationship[2]);
			String[] joinColumns = relationship[1].split("\\."); // TODO: check
																	// if this
																	// needs to
																	// be
																	// cleaned
			String predicate = subject + "." + joinColumns[0] + "." + object + "." + joinColumns[1]; // predicate
																										// is:
																										// "fromTable.fromJoinCol.toTable.toJoinCol"
			owler.addRelation(subject, nodesAndPrimKeys.get(subject), object, nodesAndPrimKeys.get(object), predicate);
		}
	}

	private boolean isValidConnection(Connection con) {
		boolean isValid = false;

		try {
			if (con.isValid(5)) {
				isValid = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return isValid;
	}

	public String checkConnectionParams(String type, String host, String port, String username, String password,
			String schema) {
		boolean success;
		try {
			success = isValidConnection(buildConnection(type, host, port, username, password, schema));
		} catch (SQLException e) {
			return e.getMessage();
		}

		return success + "";
	}

	public HashMap<String, Object> getSchemaDetails(String type, String host, String port, String username,
			String password, String schema) throws SQLException {
		Connection con = buildConnection(type, host, port, username, password, schema);
		HashMap<String, ArrayList<HashMap>> tableDetails = new HashMap<String, ArrayList<HashMap>>(); // tablename:
																										// [colDetails]
		HashMap<String, ArrayList<HashMap>> relations = new HashMap<String, ArrayList<HashMap>>(); // sub_table:
																									// [(obj_table,
																									// fromCol,
																									// toCol)]

		DatabaseMetaData meta = con.getMetaData();
		ResultSet tables = meta.getTables(null, null, null, new String[] { "TABLE" });
		while (tables.next()) {
			ArrayList<String> primaryKeys = new ArrayList<String>();
			HashMap<String, Object> colDetails = new HashMap<String, Object>(); // name:
																				// ,
																				// type:
																				// ,
																				// isPK:
			ArrayList<HashMap> allCols = new ArrayList<HashMap>();
			HashMap<String, String> fkDetails = new HashMap<String, String>();
			ArrayList<HashMap> allRels = new ArrayList<HashMap>();

			String table = tables.getString("table_name");
			System.out.println("Table: " + table);
			ResultSet keys = meta.getPrimaryKeys(null, null, table);
			while (keys.next()) {
				primaryKeys.add(keys.getString("column_name"));

				System.out.println(keys.getString("table_name") + ": " + keys.getString("column_name") + " added.");
			}

			System.out.println("COLUMNS " + primaryKeys);
			keys = meta.getColumns(null, null, table, null);
			while (keys.next()) {
				colDetails = new HashMap<String, Object>();
				colDetails.put("name", keys.getString("column_name"));
				colDetails.put("type", keys.getString("type_name"));
				if (primaryKeys.contains(keys.getString("column_name"))) {
					colDetails.put("isPK", true);
				} else {
					colDetails.put("isPK", false);
				}
				allCols.add(colDetails);

				System.out.println(
						"\t" + keys.getString("column_name") + " (" + keys.getString("type_name") + ") added.");
			}
			tableDetails.put(table, allCols);

			System.out.println("FOREIGN KEYS");
			keys = meta.getExportedKeys(null, null, table);
			while (keys.next()) {
				fkDetails = new HashMap<String, String>();
				fkDetails.put("fromCol", keys.getString("PKCOLUMN_NAME"));
				fkDetails.put("toTable", keys.getString("FKTABLE_NAME"));
				fkDetails.put("toCol", keys.getString("FKCOLUMN_NAME"));
				allRels.add(fkDetails);

				System.out.println(keys.getString("PKTABLE_NAME") + ": " + keys.getString("PKCOLUMN_NAME") + " -> "
						+ keys.getString("FKTABLE_NAME") + ": " + keys.getString("FKCOLUMN_NAME") + " added.");
			}
			relations.put(table, allRels);
		}

		HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put("tables", tableDetails);
		ret.put("relationships", relations);
		return ret;
	}

	public void setConnectionURL(String connectionURL) {
		this.connectionURL = connectionURL;
	}
}