package prerna.engine.impl.r;

import java.sql.SQLException;
import java.util.HashMap;

import org.h2.tools.Server;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import prerna.util.Constants;
import prerna.util.DIHelper;

public class RRunner {
	
	private RConnection conn = null;
	private Server server = null;
	private String tableName = "";
	private String username = "";
	private boolean dataframeExists = false;
	private boolean scriptRanSuccessfully = false;
	
	/**
	 * Runs start method
	 * @param databaseMetaData Must contain "username" and "tableName" from H2Frame
	 * @throws RserveException Thrown when RConnection fails, usually because Rserve was not started locally
	 * @throws SQLException Thrown when Server could not be created
	 */
	public RRunner(HashMap<String, String> databaseMetaData) throws RserveException, SQLException {
		this.tableName = databaseMetaData.get("tableName");
		this.username = databaseMetaData.get("username");
		start();
	}
	
	/**
	 * Sets the RConnection and server to the values provided in method arguments, then runs start method
	 * @param databaseMetaData Must contain "username" and "tableName" from H2Frame
	 * @param conn
	 * @param server
	 * @throws RserveException Thrown when RConnection fails, usually because Rserve was not started locally
	 * @throws SQLException Thrown when Server could not be created
	 */
	public RRunner(HashMap<String, String> databaseMetaData, RConnection conn, Server server) throws RserveException, SQLException {
		this.tableName = databaseMetaData.get("tableName");
		this.username = databaseMetaData.get("username");		
		this.conn = conn;
		this.server = server;
		start();
	}
	
	/**
	 * Creates the RConnection and TCP Server if nonexistent, then starts the server and initializes the RJDBC connection
	 * @throws RserveException Thrown when RConnection fails, usually because Rserve was not started locally
	 * @throws SQLException Thrown when Server could not be created
	 */
	public String start() throws RserveException, SQLException {
		if (conn == null) {
			conn = new RConnection();
		} 
		if (server == null) {
			server = Server.createTcpServer("-tcpPort", "9999");
		}
		server.stop();
		server.start();
		return initializeRJDBCConnection();
	}
	
	/**
	 * Runs the script necessary to load the data from the H2Frame into R
	 * @param databaseMetaData Must contain "username" and "tableName" from H2Frame
	 */
	private String initializeRJDBCConnection() {
		String workingDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER).replace("\\", "/");;
		String library = "RJDBC";
		String driver = "org.h2.Driver";
		String jar = "h2-1.4.185.jar"; // TODO: create an enum of available drivers and the necessary jar for each
		String url = "jdbc:h2:" + server.getURL() + "/mem:test:LOG=0;CACHE_SIZE=65536;LOCK_MODE=1;UNDO_LOG=0";
		String script = "drv <- JDBC('" + driver + "', '" + workingDir + "/RDFGraphLib/" + jar + "', identifier.quote='`');" // line of R that loads database driver and jar
			+ "conn <- dbConnect(drv, '" + url + "', '" + username + "', '')"; // line of R script that connects to H2Frame
		try {
			loadPackage(library);
		} catch (RserveException e) {
			e.printStackTrace();
			conn.finalize();
			conn = null;
			return "Error: RJDBC package must be installed locally.";
		}
		
		return evaluateScript(script);
	}
	
	/**
	 * Loads all data from current dataframe into R dataframe
	 * @return <code>String</code> Success message
	 * @throws RserveException Thrown if Rserve is not running/crashed
	 */
	public String createDefaultDataframe() throws RserveException {
		String script = "dataframe <- dbReadTable(conn, '" + tableName + "'); ";
		String result = evaluateScript(script);
		dataframeExists = true;
		
		return result;
	}
	
	/**
	 * Loads R package
	 * @param library Name of package
	 * @throws RserveException Thrown if RServe isn't running/crashed or if library doesn't exist locally
	 */
	public void loadPackage(String library) throws RserveException {
		conn.voidEval("library(" + library + ")");
	}
	
	/**
	 * Executes R script(s) and returns String result from R
	 * @param script String of R script to be evaluated
	 * @return String R result
	 */
	public String evaluateScript(String script) {
		String result = null;
		String[] scripts = null;
		String semiColonRegex = ";(?=(?:[^'|\"]*('|\")[^'|\"]*('|\"))*[^'|\"]*$)"; // counts out pairs of quotes (single or double) in order to only obtain semicolons not between quotes
		scripts = script.trim().split(semiColonRegex);
		int i = 0;
		for(; i < scripts.length - 1; i++) {
			runScript(scripts[i].trim());
		}
		script = "paste(capture.output(print(" + scripts[i] + ")),collapse='\\n')";
		result = runScript(script);
		
		scriptRanSuccessfully = !Boolean.parseBoolean(result); // result="true" if the script errored and parses to false otherwise, thus we set scriptRanSuccessfully to opposite of result boolean
		if(!scriptRanSuccessfully) {
			result = runScript("geterrmessage()");
		}
		
		return result;
	}
	
	/**
	 * Runs script and returns String result from script. Will return true if there was an error.
	 * @param script
	 * @return String R result
	 */
	private String runScript(String script) {
		REXP r;
		String result = null;
		try {
			r = conn.eval(script);
			result = r.asString();
		} catch (RserveException e) {
			e.printStackTrace();
			result = "true";
		} catch (REXPMismatchException e) {
			System.out.println("R result could not be converted into a String");
		}
		return result;
	}
		
	/**
	 * Closes RConnection and TCP Server
	 */
	public void close() {
		conn.finalize();
		conn = null;
		server.stop();
		server = null;
		dataframeExists = false;
	}
	
	public void setDataframeExists(boolean dataframeExists) {
		this.dataframeExists = dataframeExists;
	}
	
	public boolean getDataframeExists() {
		return this.dataframeExists;
	}
	
	public boolean getScriptRanSuccessfully() {
		return this.scriptRanSuccessfully;
	}

}
