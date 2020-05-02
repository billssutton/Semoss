package prerna.rdf.engine.wrappers;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.api.SemossDataType;
import prerna.date.SemossDate;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.api.IRawSelectWrapper;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.om.HeadersDataRow;
import prerna.query.parsers.PraseSqlQueryForCount;
import prerna.util.ConnectionUtils;

public class RawRDBMSSelectWrapper extends AbstractWrapper implements IRawSelectWrapper {

	private static final Logger logger = LogManager.getLogger(RawRDBMSSelectWrapper.class);

	private static final String STACKTRACE = "StackTrace: ";

	protected BasicDataSource dataSource = null;
	protected Connection conn = null;
	protected Statement stmt = null;
	protected ResultSet rs = null;
	protected boolean closedConnection = false;

	protected int numColumns = 0;
	protected int[] colTypes = null;
	protected SemossDataType[] types;

	protected IHeadersDataRow currRow = null;

	// this is used so we do not close the engine connection
	protected boolean useEngineConnection = false;

	// use this if we want to close the connection once the iterator is done
	protected boolean closeConnectionAfterExecution = false;
	
	@Override
	public void execute() throws Exception {
		try {
			Map<String, Object> map = (Map<String, Object>) engine.execQuery(query);
			this.stmt = (Statement) map.get(RDBMSNativeEngine.STATEMENT_OBJECT);
			Object connObj = map.get(RDBMSNativeEngine.CONNECTION_OBJECT);
			if(connObj == null){
				this.useEngineConnection = true;
				connObj = map.get(RDBMSNativeEngine.ENGINE_CONNECTION_OBJECT);
			}
			this.conn = (Connection) connObj;
			this.rs = (ResultSet) map.get(RDBMSNativeEngine.RESULTSET_OBJECT);
			this.dataSource = (BasicDataSource) map.get(RDBMSNativeEngine.DATASOURCE_POOLING_OBJECT);
			// go through and collect the metadata around the query
			setVariables();
		} catch (Exception e) {
			logger.error(STACKTRACE, e);
			if(this.useEngineConnection) {
				ConnectionUtils.closeAllConnections(null, rs, stmt);
			} else {
				ConnectionUtils.closeAllConnections(conn, rs, stmt);
			}
			throw e;
		}
	}

	@Override
	public IHeadersDataRow next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}

		if (currRow == null) {
			hasNext();
		}
		// grab the current row we have
		IHeadersDataRow retRow = currRow;
		// set the reference to null so we can get a new one 
		// on the next hasNext() call;
		currRow = null;
		// return the row
		return retRow;
	}

	@Override
	public boolean hasNext() {
		if(this.closedConnection) {
			return false;
		}
		try {
			// if it is null, try and get the next row
			// from the result set
			if(currRow == null) {
				currRow = getNextRow();
			}

			// if after attempting to get the next row it is 
			// still null, then there are no new returns within the rs
			if(currRow != null) {
				return true;
			}


		} catch (SQLException e) {
			logger.error(STACKTRACE, e);
		}

		return false;
	}

	private IHeadersDataRow getNextRow() throws SQLException {
		if(rs.next()) {
			Object[] row = new Object[numColumns];
			// iterate through all the columns to get the appropriate data types
			for(int colNum = 1; colNum <= numColumns; colNum++) {
				Object val = null;
				int type = colTypes[colNum-1];
				if(type == Types.INTEGER) {
					val = rs.getInt(colNum);
				} else if(type == Types.FLOAT || type == Types.DOUBLE || type == Types.NUMERIC || type == Types.DECIMAL || type == Types.BIGINT || type == Types.REAL) {
					val = rs.getDouble(colNum);
				} else if(type == Types.DATE) {
					try {
						Date dVal = rs.getDate(colNum);
						if(dVal == null) {
							val = null;
						} else {
							val = new SemossDate(dVal, "yyyy-MM-dd");
						}
					} catch(Exception e) {
						// some rdbms do not actually support dates
						// and just return a string
						// ex: SQLite
						try {
							String dateValStr = rs.getString(colNum);
							val = new SemossDate(dateValStr, "yyyy-MM-dd");
						} catch(Exception e2) {
							// out of luck...
							logger.error(STACKTRACE, e);
							logger.error(STACKTRACE, e2);
						}
					}
				} else if(type == Types.TIMESTAMP) {
					try {
						Timestamp dVal = rs.getTimestamp(colNum);
						if(dVal == null) {
							val = null;
						} else {
							val = new SemossDate(dVal.getTime(), true);
						}
					} catch(Exception e) {
						// some rdbms do not actually support dates
						// and just return a string
						// ex: SQLite
						try {
							String dateValStr = rs.getString(colNum);
							val = new SemossDate(dateValStr, "yyyy-MM-dd HH:mm:ss");
						} catch(Exception e2) {
							// out of luck...
							logger.error(STACKTRACE, e);
							logger.error(STACKTRACE, e2);
						}
					}
				} else if(type == Types.CLOB) {
					val = rs.getClob(colNum);
				} else if(type == Types.ARRAY) {
					Array arrVal = rs.getArray(colNum);
					if(arrVal != null) {
						val = arrVal.getArray();
					}
				} else if(type == Types.BOOLEAN || type == Types.BIT) {
					val = rs.getBoolean(colNum);
				}
				else {
					val = rs.getString(colNum);
				}
				
				// need to account for null values
				if(rs.wasNull()) {
					val = null;
				}
				
				row[colNum-1] = val;
			}
			
			// return the header row
			return new HeadersDataRow(headers, rawHeaders, row, row);
		} else {
			cleanUp();
		}

		// no more results
		// return null
		return null;
	}


	protected void setVariables(){
		try {
			// get the result set metadata
			ResultSetMetaData rsmd = rs.getMetaData();
			numColumns = rsmd.getColumnCount();

			// create the arrays to store the column types,
			// the physical variable names and the display variable names
			colTypes = new int[numColumns];
			types = new SemossDataType[numColumns];
			rawHeaders = new String[numColumns];
			headers = new String[numColumns];

			for(int colIndex = 1; colIndex <= numColumns; colIndex++) {
				rawHeaders[colIndex-1] = rsmd.getColumnName(colIndex);
				headers[colIndex-1] = rsmd.getColumnLabel(colIndex);
				colTypes[colIndex-1] = rsmd.getColumnType(colIndex);
				types[colIndex-1] = SemossDataType.convertStringToDataType(rsmd.getColumnTypeName(colIndex));
			}
		} catch (SQLException e) {
			logger.error(STACKTRACE, e);
		}
	}

	@Override
	public String[] getHeaders() {
		return headers;
	}

	@Override
	public SemossDataType[] getTypes() {
		return types;
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return this.rs.getMetaData();
	}
	
	public void setCloseConenctionAfterExecution(boolean closeConnectionAfterExecution) {
		this.closeConnectionAfterExecution = closeConnectionAfterExecution;
	}
	
	@Override
	public void cleanUp() {
		if(this.closedConnection) {
			return;
		}
		try {
			if(this.rs != null) {
				this.rs.close();
			}
		} catch (SQLException e) {
			logger.error(STACKTRACE, e);
		}
		try {
			if(this.stmt != null) {
				this.stmt.close();
			}
		} catch (SQLException e) {
			logger.error(STACKTRACE, e);
		}
		if(this.closeConnectionAfterExecution) {
			try {
				if(this.conn != null) {
					this.conn.close();
				}
			} catch (SQLException e) {
				logger.error(STACKTRACE, e);
			}
		}
		if(this.dataSource != null) {
			// if using a datasource
			// we need to close the connection
			// to give it back to the pool
			try {
				if(this.conn != null) {
					this.conn.close();
				}
			} catch (SQLException e) {
				logger.error(STACKTRACE, e);
			}
		}
		this.closedConnection = true;
	}
	
	@Override
	public long getNumRows() {
		if(this.numRows == 0) {
			PraseSqlQueryForCount parser = new PraseSqlQueryForCount();
			String query;
			try {
				query = parser.processQuery(this.query);
			} catch (Exception e) {
				logger.error(STACKTRACE, e);
				query = this.query;
			}
			query = "select count(*) from (" + query + ") t";
			Connection connection = null;
			Statement statement = null;
			ResultSet resultSet = null;
			try {
				if(this.dataSource != null) {
					connection = this.dataSource.getConnection();
					statement = connection.createStatement();
				} else {
					statement = this.conn.createStatement();
				}
				resultSet = statement.executeQuery(query);
				if(resultSet.next()) {
					this.numRows = resultSet.getLong(1);
				}
			} catch (SQLException e) {
				logger.error(STACKTRACE, e);
			} finally {
				if(resultSet != null) {
					try {
						resultSet.close();
					} catch (SQLException e) {
						logger.error(STACKTRACE, e);
					}
				}
				if(statement != null) {
					try {
						statement.close();
					} catch (SQLException e) {
						logger.error(STACKTRACE, e);
					}
				}
				if(this.dataSource != null) {
					try {
						if (connection != null) {
							connection.close();
						}
					} catch (SQLException e) {
						logger.error(STACKTRACE, e);
					}
				}
			}
		}
		return this.numRows;
	}
	
	@Override
	public long getNumRecords() {
		return getNumRows() * this.numColumns;
	}
	
	@Override
	public void reset() throws Exception {
		// close current stuff
		// but we shouldn't close the connection
		// so store whatever that boolean is as temp
		// and then reasign after we re-execute
		boolean temp = this.closeConnectionAfterExecution;
		this.closeConnectionAfterExecution = false;
		cleanUp();
		this.closeConnectionAfterExecution = temp;
		// execute again
		execute();
	}
	
	/**
	 * This method allows me to perform the execution of a query on a given connection
	 * without having to go through a formal RDBMSNativeEngine construct
	 * i.e. the naked engine ;)
	 * @param conn
	 * @param query
	 */
	public void directExecutionViaConnection(Connection conn, String query, boolean closeIfFail) {
		try {
			this.query = query;
			this.conn = conn;
			this.stmt = this.conn.createStatement();
			this.rs = this.stmt.executeQuery(query);
			setVariables();
		} catch(Exception e) {
			logger.error(STACKTRACE, e);
			if(closeIfFail) {
				ConnectionUtils.closeAllConnections(conn, rs, stmt);
			}
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	@Override
	public boolean flushable() {
		return false;
	}
	
	@Override
	public String flush() {
		return null;
	}
}
