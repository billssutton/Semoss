package prerna.sablecc2.reactor.frame.r.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import prerna.auth.AccessToken;
import prerna.auth.AuthProvider;
import prerna.auth.User;
import prerna.engine.impl.r.RUserRserve;
import prerna.util.ArrayUtilityMethods;

public class RJavaUserRserveTranslator extends AbstractRJavaTranslator{
	
	RConnection rcon;
	
	@Override
	public void initREnv() {
		try {
			rcon.eval("if(!exists(\"" + this.env + "\")) {" + this.env  + "<- new.env();}");
		} catch (RserveException e) {
			throw new IllegalArgumentException("Failed to establish R environment.");
		}
	}

	@Override
	public void startR() {
		if (rConnIsDefined()) {
			logger.info("Using existing R connection for user " + getUserInfo());
			rcon = this.insight.getUser().getRConn();
		} else {
			logger.info("Establishing new R connection for user " + getUserInfo());
			establishNewRConnection();
		}
	}
	
	private String getUserInfo() {
		if (userIsDefined()) {
			List<String> userNames = new ArrayList<>();
			User user = this.insight.getUser();
			for (AuthProvider provider : user.getLogins()) {
				AccessToken token = user.getAccessToken(provider);
				userNames.add(token.getName());
			}
			return String.join(";", userNames);
		} else {
			return "anonymous";
		}
	}
	
	private boolean userIsDefined() {
		return this.insight != null && this.insight.getUser() != null;
	}
	
	private boolean rConnIsDefined() {
		return userIsDefined() && this.insight.getUser().getRConn() != null;
	}
	
	private void establishNewRConnection() {
		try {
			rcon = RUserRserve.createConnection();
			
			// load all the libraries
			// split stack shape
			rcon.eval("library(splitstackshape);");
			logger.info("Loaded packages splitstackshape");
			
			// data table
			rcon.eval("library(data.table);");
			logger.info("Loaded packages data.table");
			
			// reshape2
			rcon.eval("library(reshape2);");
			logger.info("Loaded packages reshape2");
			
			// stringr
			rcon.eval("library(stringr)");
			logger.info("Loaded packages stringr");
			
			// lubridate
			rcon.eval("library(lubridate);");
			logger.info("Loaded packages lubridate");
			
			// dplyr
			// TODO >>>timb: RUSER - uncomment this
			// rcon.eval("library(dplyr);");
			// logger.info("Loaded packages dplyr");
			
			// initialize the r environment
			initREnv();
			setMemoryLimit();
			
			// Set the R connection on the user object
			if (userIsDefined()) {
				this.insight.getUser().setRConn(rcon);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("ERROR ::: Could not find connection.\nPlease make sure Rserve is running and the following libraries are installed:\n"
							+ "1)splitstackshape\n 2)data.table\n 3)reshape2\n 4)stringr\n 5)lubridate\n 6)dplyr", e);
		}
	}

	private synchronized boolean heartBeat(){
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(new Callable<Boolean>() {
		    @Override
		    public Boolean call() throws Exception {
		        return isHealthy();
		    }
		});

		boolean isHealthy = false;
		try {
			isHealthy = future.get(3L, TimeUnit.SECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			executor.shutdownNow();
		    isHealthy = false;
		}

		return isHealthy;
	}
	private boolean isHealthy() {
		boolean isHealthy = false; // Healthy skepticism
		try {
			Object heartBeat = rcon.eval("1+2");
			if (heartBeat instanceof org.rosuda.REngine.REXP) {
				if (((org.rosuda.REngine.REXP) heartBeat).asDouble() == 3L) {
					logger.info("R health check passed");
					isHealthy = true;
				}
			}
		} catch (RserveException | REXPMismatchException e) {
			logger.warn("R health check failed", e);
		}
		return isHealthy;
	}
	
	private String handleRException(Exception e) {
		String message;
		if (isHealthy()) {
			logger.warn("Script failed but R is healthy", e);
			message = "R is working properly, but an error occurred running the script.";
		} else {
			logger.info("R health check failed; attempting to establish a new R connection", e);
			establishNewRConnection();
			message = "R was not working properly but has succesfully recovered; however, your data in R has been lost.";
		}
		return message;
	}
	
	@Override
	public Object executeR(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			if(heartBeat()){
			return rcon.eval(rScript);
			} 
			else{
				throw new RserveException(rcon, rScript);
			}
		} catch (RserveException e) {
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public void executeEmptyR(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			if(heartBeat()){
			 rcon.voidEval(rScript);
			} 
			else{
				throw new RserveException(rcon, rScript);
			}
		} catch (RserveException e) {
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public String getString(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			return rcon.eval(rScript).asString();
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public String[] getStringArray(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			return rcon.eval(rScript).asStrings();
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public int getInt(String rScript) {
		int number = 0;
		try {
			logger.info("Running R: " + rScript);
			number = rcon.eval(rScript).asInteger();
			return number;
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public int[] getIntArray(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			return rcon.eval(rScript).asIntegers();
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public double getDouble(String rScript) {
		double number = 0;
		try {
			logger.info("Running R: " + rScript);
			number = rcon.eval(rScript).asDouble();
			return number;
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public double[] getDoubleArray(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			return rcon.eval(rScript).asDoubles();
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public double[][] getDoubleMatrix(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			return rcon.eval(rScript).asDoubleMatrix();
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public boolean getBoolean(String rScript) {
		// 1 = TRUE, 0 = FALSE

		try {
			logger.info("Running R: " + rScript);
			REXP val = rcon.eval(rScript);
			if(val != null) {
				return (val.asInteger() == 1);
			}
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
		return false;

	}

	@Override
	public Object getFactor(String rScript) {
		try {
			logger.info("Running R: " + rScript);
			return rcon.eval(rScript).asFactor();
		} catch(RserveException | REXPMismatchException e){
			String message = handleRException(e);
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public void setConnection(RConnection connection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPort(String port) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endR() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopRProcess() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getHistogramBreaksAndCounts(String script) {
		try {
			double[] breaks;
			Map<String, Object> histJ = (Map<String, Object>) (rcon.eval(script).asNativeJavaObject());
			if (histJ.get("breaks") instanceof int[]){
				int[] breaksInt = (int[]) histJ.get("breaks");
				breaks = Arrays.stream(breaksInt).asDoubleStream().toArray();
			} else { 
			breaks = (double[]) histJ.get("breaks");
			}
			int[] counts = (int[]) histJ.get("counts");
			
			Map<String, Object> retMap = new HashMap<String, Object>();
			retMap.put("breaks", breaks);
			retMap.put("counts", counts);
			return retMap;
		} catch (RserveException e) {
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Map<String, Object> flushFrameAsTable(String framename, String[] colNames) {
List<Object[]> dataMatrix = new ArrayList<Object[]>();
		
		int numCols = colNames.length;
		for (int i = 0; i < numCols; i++) {
			String script = framename + "$" + colNames[i];
			REXP val = (REXP) executeR(script);

			if (val.isNumeric()) {
				// for a double array
				try {
					double[] rows = val.asDoubles();
					int numRows = rows.length;
					if (dataMatrix.isEmpty()) {
						initEmptyMatrix(dataMatrix, numRows, numCols);
					}
					for (int j = 0; j < numRows; j++) {
						dataMatrix.get(j)[i] = rows[j];
					}
					continue;
				} catch (REXPMismatchException rme) {
					rme.printStackTrace();
				}
				//in case values cannot be doubles
				try {
					int[] rows = val.asIntegers();
					int numRows = rows.length;
					if (dataMatrix.isEmpty()) {
						initEmptyMatrix(dataMatrix, numRows, numCols);
					}
					for (int j = 0; j < numRows; j++) {
						dataMatrix.get(j)[i] = rows[j];
					}
					continue;
				} catch (REXPMismatchException rme) {
					rme.printStackTrace();
				}
				//in case values cannot be put into an array
				//for an integer
				try {
					int row = val.asInteger();
					if (dataMatrix.isEmpty()) {
						initEmptyMatrix(dataMatrix, 1, numCols);
					}
					dataMatrix.get(0)[i] = row;
					continue;
				} catch (REXPMismatchException rme) {
					rme.printStackTrace();
				}

			} else {
				// for a string array
				try {
					String[] rows = val.asStrings();
					int numRows = rows.length;
					if (dataMatrix.isEmpty()) {
						initEmptyMatrix(dataMatrix, numRows, numCols);
					}
					for (int j = 0; j < numRows; j++) {
						dataMatrix.get(j)[i] = rows[j];
					}
					continue;
				} catch (REXPMismatchException rme) {
					rme.printStackTrace();
				}
				//for a string
				try {
					String row = val.asString();
					if (dataMatrix.isEmpty()) {
						initEmptyMatrix(dataMatrix, 1, numCols);
					}
					dataMatrix.get(0)[i] = row;
					continue;
				} catch (REXPMismatchException rme) {
					rme.printStackTrace();
				}
			}
		}
		
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("headers", colNames);
		retMap.put("data", dataMatrix);
		
		return retMap;
	}

	@Override
	public Object[] getDataRow(String rScript, String[] headerOrdering) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object[]> getBulkDataRow(String rScript, String[] headerOrdering) {
		REXP rs = (REXP) executeR(rScript);
		Object result = null;
		try {
			result = rs.asNativeJavaObject();
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		}
		if(result instanceof Map) {
			return processMapReturn((Map<String, Object>) result, headerOrdering);
		} else if(result instanceof List) {
			String[] returnNames = null;
			try {
				Object namesAttr = rs.getAttribute("names").asNativeJavaObject();
				if(namesAttr instanceof String[]) {
					returnNames = (String[]) namesAttr;
				} else {
					// assume it is single string
					returnNames = new String[]{namesAttr.toString()};
				}
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
			return processListReturn((List) result, headerOrdering, returnNames);
		} else {
			throw new IllegalArgumentException("Unknown data type returned from R");
		}
	}
	
	private List<Object[]> processMapReturn(Map<String, Object> result,  String[] headerOrdering) {
		List<Object[]> retArr = new Vector<Object[]>(500);
		int numColumns = headerOrdering.length;
		for(int idx = 0; idx < numColumns; idx++) {
			Object val = result.get(headerOrdering[idx]);

			if(val instanceof Object[]) {
				Object[] data = (Object[]) val;
				if(retArr.size() == 0) {
					for(int i = 0; i < data.length; i++) {
						Object[] values = new Object[numColumns];
						values[idx] = data[i];
						retArr.add(values);
					}
				} else {
					for(int i = 0; i < data.length; i++) {
						Object[] values = retArr.get(i);
						values[idx] = data[i];
					}
				}
			} else if(val instanceof double[]) {
				double[] data = (double[]) val;
				if(retArr.size() == 0) {
					for(int i = 0; i < data.length; i++) {
						Object[] values = new Object[numColumns];
						values[idx] = data[i];
						retArr.add(values);
					}
				} else {
					for(int i = 0; i < data.length; i++) {
						Object[] values = retArr.get(i);
						values[idx] = data[i];
					}
				}
			} else if(val instanceof int[]) {
				int[] data = (int[]) val;
				if(retArr.size() == 0) {
					for(int i = 0; i < data.length; i++) {
						Object[] values = new Object[numColumns];
						values[idx] = data[i];
						retArr.add(values);
					}
				} else {
					for(int i = 0; i < data.length; i++) {
						Object[] values = retArr.get(i);
						values[idx] = data[i];
					}
				}
			} else if (val instanceof String) {
				String data = (String) val;
				if (retArr.size() == 0) {
					Object[] values = new Object[numColumns];
					values[idx] = data;
					retArr.add(values);
				} else {
					Object[] values = retArr.get(0);
					values[idx] = data;
				}
			} else if (val instanceof Double) {
				Double data = (Double) val;
				if (retArr.size() == 0) {
					Object [] values = new Object[numColumns];
					values[idx] = data;
					retArr.add(values);
				} else {
					Object[] values = retArr.get(0);
					values [idx] = data;
				}	
			} else if (val instanceof Integer){
				Integer data = (Integer) val;
				if (retArr.size() == 0) {
					Object [] values = new Object [numColumns];
					values[idx] = data;
					retArr.add(values);
				} else {
					Object [] values = retArr.get(0);
					values [idx] = data;
				}
			} else {
				logger.info("ERROR ::: Could not identify the return type for this iterator!!!");
			}
		}
		return retArr;
	}
	
	private List<Object[]> processListReturn(List<Object[]> result, String[] headerOrdering, String[] returnNames) {
		List<Object[]> retArr = new Vector<Object[]>(500);

		// match the returns based on index
		int numHeaders = headerOrdering.length;
		int[] headerIndex = new int[numHeaders];
		for(int i = 0; i < numHeaders; i++) {
			headerIndex[i] = ArrayUtilityMethods.arrayContainsValueAtIndex(returnNames, headerOrdering[i]);
		}
		
		for(int i = 0; i < numHeaders; i++) {
			// grab the right column index
			int columnIndex = headerIndex[i];
			// each column comes back as an array
			// need to first initize my return matrix
			Object col = result.get(columnIndex);
			if(col instanceof Object[]) {
				Object[] columnResults = (Object[]) col;
				int numResults = columnResults.length;
				if(retArr.size() == 0) {
					for(int j = 0; j < numResults; j++) {
						Object[] values = new Object[numHeaders];
						values[i] = columnResults[j];
						retArr.add(values);
					}
				} else {
					for(int j = 0; j < numResults; j++) {
						Object[] values = retArr.get(j);
						values[i] = columnResults[j];
					}
				}
			} else if(col instanceof double[]) {
				double[] columnResults = (double[]) col;
				int numResults = columnResults.length;
				if(retArr.size() == 0) {
					for(int j = 0; j < numResults; j++) {
						Object[] values = new Object[numHeaders];
						values[i] = columnResults[j];
						retArr.add(values);
					}
				} else {
					for(int j = 0; j < numResults; j++) {
						Object[] values = retArr.get(j);
						values[i] = columnResults[j];
					}
				}
			} else if(col instanceof int[]) {
				int[] columnResults = (int[]) col;
				int numResults = columnResults.length;
				if(retArr.size() == 0) {
					for(int j = 0; j < numResults; j++) {
						Object[] values = new Object[numHeaders];
						values[i] = columnResults[j];
						retArr.add(values);
					}
				} else {
					for(int j = 0; j < numResults; j++) {
						Object[] values = retArr.get(j);
						values[i] = columnResults[j];
					}
				}
			}
		}
		
		return retArr;
	}
	
	

}
