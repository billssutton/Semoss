package prerna.engine.impl.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import prerna.engine.impl.model.responses.IModelEngineResponseHandler;
import prerna.engine.impl.model.responses.IModelEngineResponseStreamHandler;
import prerna.sablecc2.comm.JobManager;
import prerna.security.AbstractHttpHelper;
import prerna.util.Constants;

public abstract class RESTModelEngine extends AbstractModelEngine {

	private static final Logger classLogger = LogManager.getLogger(RESTModelEngine.class);
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
	}
	
	protected IModelEngineResponseHandler postRequestStringBody(String url, Map<String, String> headersMap, String body, ContentType contentType, String keyStore, String keyStorePass, String keyPass, boolean isStream, Class<? extends IModelEngineResponseHandler> responseType, String insightId) {
		CloseableHttpClient httpClient = null;
	    CloseableHttpResponse response = null;
	    try {
	        httpClient = AbstractHttpHelper.getCustomClient(null, keyStore, keyStorePass, keyPass);
	        HttpPost httpPost = new HttpPost(url);
	        if (headersMap != null && !headersMap.isEmpty()) {
	            for (String key : headersMap.keySet()) {
	                httpPost.addHeader(key, headersMap.get(key));
	            }
	        }
	        if (body != null && !body.isEmpty()) {
	            httpPost.setEntity(new StringEntity(body, contentType));
	        }
	        response = httpClient.execute(httpPost);

	        int statusCode = response.getStatusLine().getStatusCode();
	        if (statusCode >= 200 && statusCode < 300) {
	            HttpEntity entity = response.getEntity();
	            if (!isStream) {
	                // Handle regular response
	                String responseData = entity != null ? EntityUtils.toString(entity) : null;
	                IModelEngineResponseHandler responseObject = new Gson().fromJson(responseData, responseType);
	                return responseObject;
	            } else {
	                // Handle streaming response
	                if (entity != null) {
	                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
	                        String line;
	                        StringBuilder responseAssimilator = new StringBuilder();
	                        IModelEngineResponseHandler responseObject = responseType.newInstance();
	                        
	                        while ((line = reader.readLine()) != null) {
	                            if (line.contains("data: [DONE]")) {
	                                break;
	                            }
	                            
	                            if (line.startsWith("data: ")) {
	                                // Extract JSON part
	                                String jsonPart = line.substring("data: ".length());
	                                IModelEngineResponseStreamHandler partialObject = new Gson().fromJson(jsonPart, responseObject.getStreamHandlerClass());
	                                Object partial = partialObject.getPartialResponse();
	                                
	                                if (partial != null) {
	                                	responseObject.appendStream(partialObject);
		                                JobManager.getManager().addPartialOut(insightId, partial+"");
		                                responseAssimilator.append(partial);
	                                }
	                            }
	                        }
	                        responseObject.setResponse(responseAssimilator.toString());
	                        return responseObject;
	                    } catch (Exception e) {
	            	        classLogger.error(Constants.STACKTRACE, e);
	            	        throw new IllegalArgumentException("There was an error processing the response from " + url);
	            	    }
	                }
	            }
	        } else {
	        	// try to send back the error from the server
	            String errorResponse = EntityUtils.toString(response.getEntity());
	            throw new IllegalArgumentException("Connected to " + url + " but received error = " + errorResponse);
	        }
	    } catch (IOException e) {
	        classLogger.error(Constants.STACKTRACE, e);
	        throw new IllegalArgumentException("Could not connect to URL at " + url);
	    } finally {
	        try {
	            if (response != null) {
	                response.close();
	            }
	            if (httpClient != null) {
	                httpClient.close();
	            }
	        } catch (IOException e) {
	            classLogger.error("Error while closing resources", e);
	        }
	    }
	    return null; // In case of unexpected flow
	}
}
