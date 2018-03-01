package prerna.engine.impl.json;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import net.minidev.json.JSONArray;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.engine.impl.AbstractEngine;
import prerna.poi.main.RDBMSEngineCreationHelper;
import prerna.query.interpreters.IQueryInterpreter2;
import prerna.query.interpreters.JsonInterpreter;
import prerna.query.interpreters.SparqlInterpreter2;
import prerna.util.CSVToOwlMaker;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

public class JsonAPIEngine extends AbstractEngine {
	
	private static final Logger logger = LogManager.getLogger(AbstractEngine.class.getName());
	
	public static final String input_type = "input_type";
	public static final String input_method = "input_method";
	public static final String input_url = "input_url";
	public static final String input_params = "input_params";
	public static final String mandatory_input = "mandatory_input";
	public static final String output_type = "output_type";
	public static final String path_patterns = "path_patterns";
	public static final String concat = "concat";
	public static final String delim = "delim";

	
	Object document = null;

	
	public void openDB(String propFile) {
		try {
			baseFolder = DIHelper.getInstance().getProperty("BaseFolder");
			Hashtable <String, String> paramHash = new Hashtable <String, String>();
			paramHash.put("BaseFolder", baseFolder);
			if(getEngineName() != null)
				paramHash.put("engine", getEngineName());

			if(propFile != null) {
				setPropFile(propFile);
				logger.info("Opening DB - " + engineName);
				prop = Utility.loadProperties(propFile);
			}
			if(prop != null) {
				// load the rdbms insights db
				// nope nothing to load here

				// load the rdf owl db
				String owlFile = prop.getProperty(Constants.OWL);
				
				if (owlFile != null) {
					// need a check here to say if I am asking this to be remade or keep what it is
					if(owlFile.equalsIgnoreCase("REMAKE"))
					{
						// the process of remake will start here
						// see if the usefile is there
						if(prop.containsKey(USE_FILE))
						{
							String csvFile = prop.getProperty(DATA_FILE);	
							owlFile = csvFile.replace("data/", "") + ".OWL";
							//Map <String, String> paramHash = new Hashtable<String, String>();
							
							paramHash.put("BaseFolder", DIHelper.getInstance().getProperty("BaseFolder"));
							paramHash.put("ENGINE", getEngineName());
							csvFile = Utility.fillParam2(csvFile, paramHash);

							
							String fileName = Utility.getOriginalFileName(csvFile);
							// make the table name based on the fileName
							String cleanTableName = RDBMSEngineCreationHelper.cleanTableName(fileName).toUpperCase();
							owlFile = baseFolder + "/db/" + getEngineName() + "/" + cleanTableName + ".OWL";
							
							CSVToOwlMaker maker = new CSVToOwlMaker();
							maker.makeOwl(csvFile, owlFile, getEngineType());
							owlFile = "/db/" + getEngineName() + "/" + cleanTableName + ".OWL";
							
							if(prop.containsKey("REPLACE_OWL"))
								Utility.updateSMSSFile(propFile, Constants.OWL, owlFile);
						}
						else
							owlFile = null;
					}
					if(owlFile != null)
					{					
						owlFile = Utility.fillParam2(owlFile, paramHash);
						logger.info("Loading OWL: " + owlFile);
						setOWL(baseFolder + "/" + owlFile);
					}
				}
				// load properties object for db
				// not sure what this is doing
				// I changed this to public
				String genEngPropFile = prop.getProperty(Constants.ENGINE_PROPERTIES);
				if (genEngPropFile != null) {
					generalEngineProp = Utility.loadProperties(baseFolder + "/" + genEngPropFile);
				}
			}
			loadDocument();
			// seems like I may not need this
			// setOWL already does this
			//this.owlHelper = new MetaHelper(baseDataEngine, getEngineType(), this.engineName);
		} catch (RuntimeException e) {
			e.printStackTrace();
		} 
	}

	private void loadDocument()
	{
		try {
			if(prop.containsKey("input_type") && ((String)prop.get("input_type")).equalsIgnoreCase("file"))
				document = Configuration.defaultConfiguration().jsonProvider().parse(new FileInputStream(baseFolder + "/" + prop.getProperty("input_url")), "utf-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// inputparams@@@alias patterns=alias or just alias@@@metadata - I will not worry about metadata for now
	@Override
	public Object execQuery(String query) {
		// the exec query has to get a couple of different things
		// parameters for the input
		// selection for the output
		// output metadata - may not be needed
		// need to use a better delimiter
		
		// need some way to identify how to segregate into one vs. many requests
		
		String [] pathParts = query.split("@@@");
		String [] inputParams = null;
		String [] jsonPaths = null;
		Hashtable retHash = new Hashtable();
		
		Object curDoc = document;
		//JSONArray [] data = new JSONArray[jsonPaths.length];
		
		
		if(pathParts.length == 2)
		{
			inputParams = pathParts[0].split(";");		
			jsonPaths = pathParts[1].split(";");			
		}
		else
		{
			jsonPaths = pathParts[0].split(";");			
		}
		// which of the params are list params
		// hopefully there is only one
		// I have no idea how to deal with many at htis point
		// without it being a MESS of for loops
		Hashtable listParams = new Hashtable();
		// since I am hoping there is only one
		// I am keeping it here
		String listKey = null;
		String listValue = null;
		
		boolean array = false;
		for(int paramIndex = 0;inputParams != null && paramIndex < inputParams.length;paramIndex++)
		{
			String [] keyValue = inputParams[paramIndex].split("=");
			String key = keyValue[0];
			String value = keyValue[1];
			
			if(value.startsWith("ARRAY"))
			{
				array = true;
				listKey = key;
				listValue = value;
				listParams.put(key, value.replace("ARRAY", ""));
			}
		}
		
		
		// make the doc
		// get the content
		// if this is not a file based then we need to make the URL
		if(curDoc == null)
		{
			String inputData = "";
			// need to run the http to grab the URL etc. and then load the document
			
			// need to make a check to see if this one vs. many
			Hashtable inputParamHash = Utility.getParamTypeHash(prop.getProperty(input_url));
			Hashtable inputValHash = getParamHash(inputParams);
			
			// make the primary hash
			Hashtable finalValHash = fillParams(inputParamHash, inputValHash);
			
			// replace each value for the key and send it in
			if(array)
			{
				String [] multiValue = listValue.split("<>");
				for(int valIndex = 0;valIndex < multiValue.length;valIndex++)
				{
					finalValHash.put(listKey, multiValue[valIndex]);
					if(prop.getProperty(input_method).equalsIgnoreCase("GET"))
						inputData = doGet(finalValHash);
					else
						inputData = doPost(finalValHash);
					
				
					curDoc = Configuration.defaultConfiguration().jsonProvider().parse(inputData);
					
					// send the data to add to it
					retHash = getOutput(curDoc, jsonPaths, retHash);					
				}
			}
			else // this is not a list.. one time pull call it a day
			{
				if(prop.getProperty(input_method).equalsIgnoreCase("GET"))
					inputData = doGet(finalValHash);
				else
					inputData = doPost(finalValHash);
							
				curDoc = Configuration.defaultConfiguration().jsonProvider().parse(inputData);
				
				// send the data to add to it
				retHash = getOutput(curDoc, jsonPaths, retHash);
			}
			
		}
		
		else // it is a file
		{
			retHash = getOutput(curDoc, jsonPaths, retHash);
		}
		
		//System.out.println("Output..  " + data);
		
		
		//return the data
		return retHash;
	}
	
	private Hashtable getOutput(Object doc, String [] jsonPaths, Hashtable retHash)
	{
		JSONArray [] data = new JSONArray[jsonPaths.length];
		int numRows = 0;
		
		int totalRows = 0;
		
		
		String [] headers = new String[jsonPaths.length];
		
		JSONArray [] input = null;
		if(retHash.containsKey("DATA"))
			input = (JSONArray [])	retHash.get("DATA");
		
		if(retHash.containsKey("COUNT"))
			totalRows = (Integer)	retHash.get("COUNT");
		
		for(int pathIndex = 0;pathIndex < jsonPaths.length;pathIndex++)
		{
			
			String thisHeader = null;
			String thisPath = null;

			if(jsonPaths[pathIndex].equalsIgnoreCase("="))
			{
				String [] pathToks = jsonPaths[pathIndex].split("=");
				thisHeader = pathToks[0];
				thisPath = pathToks[1];
			}
			else
			{
				thisPath = prop.getProperty(jsonPaths[pathIndex]);
				thisHeader = jsonPaths[pathIndex];
			}
			
			data[pathIndex] = JsonPath.read(doc, thisPath);
			
			// add it to the current input
			if(input != null)
				input[pathIndex].addAll(data[pathIndex]);

			// set the headers
			headers[pathIndex] = thisHeader;
						
			// if we are starting at a new point
			// add the number of rows to comparator
			
			if(numRows == 0 || numRows > data[pathIndex].size())
				numRows = data[pathIndex].size();
			
			System.out.println(" >> " + data[pathIndex].toString());
			System.out.println("Length >> " + data[pathIndex].size());	
		}
		
		totalRows = totalRows + numRows;
		if(!retHash.containsKey("TYPES"))
			retHash.put("TYPES", getTypes(data));
		retHash.put("HEADERS", headers);
		retHash.put("DATA", data);
		retHash.put("COUNT", totalRows);

		System.out.println("Output..  " + data);
		
		return retHash;
	}
	
	private String [] getTypes(JSONArray [] data)
	{
		
		String [] types = new String[data.length];
		for(int dataIndex = 0;dataIndex < data.length;dataIndex++)
		{
			Object firstOne = data[dataIndex].get(0);
			if(firstOne instanceof Integer)
				types[dataIndex] = "int";
			if(firstOne instanceof JSONArray)
				types[dataIndex] = "String";
			if(firstOne instanceof Double)
				types[dataIndex] = "Double";
		}

		return types;
	}

	@Override
	public void insertData(String query) {
		// TODO Auto-generated method stub

	}

	@Override
	public ENGINE_TYPE getEngineType() {
		// TODO Auto-generated method stub
		return prerna.engine.api.IEngine.ENGINE_TYPE.JSON;
	}

	@Override
	public Vector<Object> getEntityOfType(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeData(String query) {
		// TODO Auto-generated method stub

	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub

	}

	@Override
	public Vector<Object> getCleanSelect(String query) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// get call
	private String doGet(Hashtable params)
	{
		String retString = null;
		
		try {
			String url = constructURL(params);
			
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpget = new HttpGet(url);
			
			ResponseHandler<String> handler = new BasicResponseHandler();
			CloseableHttpResponse response = httpclient.execute(httpget);
			
			retString = handler.handleResponse(response);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		return retString;
	}

	private String doPost(Hashtable params)
	{
		String retString = null;
		
		try {
			String url = prop.getProperty(input_url);
			
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost httppost = new HttpPost(url);
			
			List<NameValuePair> paramList = new ArrayList<NameValuePair>();
			Enumeration <String> keys = params.keys();
			
			while(keys.hasMoreElements())
			{
				String key = keys.nextElement();
				String value = (String)params.get(key);
				
				paramList.add(new BasicNameValuePair(key, value));
			}			
			
			// set entity
			httppost.setEntity(new UrlEncodedFormEntity(paramList));
			
			ResponseHandler<String> handler = new BasicResponseHandler();
			CloseableHttpResponse response = httpclient.execute(httppost);
			
			retString = handler.handleResponse(response);
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		return retString;
	}


	// composes the params
	private Hashtable getParamHash(String [] params)
	{
		String [] paramToks = params;
		Hashtable <String, String> paramHash = new Hashtable <String, String>();
		
		for(int paramIndex = 0;paramIndex < paramToks.length;paramIndex++)
		{
			String thisParam = paramToks[paramIndex];
			
			String [] thisParamToks = thisParam.split("=");
			
			paramHash.put(thisParamToks[0], thisParamToks[1]);
		}
		
		return paramHash;
	}
	
	private Hashtable fillParams(Hashtable inputParam, Hashtable valParam)
	{
		Enumeration <String> keys = inputParam.keys();
		
		// replace with the new values
		while(keys.hasMoreElements())
		{
			String key = keys.nextElement();
			String value = null;
			if(valParam.containsKey(key))
				value = (String)valParam.get(key);
			else if(prop.containsKey(key + "_DEFAULT"))
				value = (String)prop.getProperty(key + "_DEFAULT");
			//else
				// need a way to remove this
			inputParam.put(key, value);
		}
		
		return inputParam;
	}
	
	// take the URL 
	private String constructURL(Hashtable params)
	{
		// params are given as paramname=value;paramname2=value2
	
		// need to get hate URL
		String inputUrl = prop.getProperty(input_url);
		String mandatoryInputs = prop.getProperty(mandatory_input);
		
		
		// compose the URL
		String finalURL = Utility.fillParam2(inputUrl, params);
		
		return finalURL;
		
	}
	
	@Override
	public IQueryInterpreter2 getQueryInterpreter2(){
		return new JsonInterpreter(this);
	}

}
