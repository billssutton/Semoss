
package prerna.sablecc2.reactor.qs.source;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import prerna.auth.AccessToken;
import prerna.auth.AuthProvider;
import prerna.auth.User2;
import prerna.io.connector.IConnectorIOp;
import prerna.om.RemoteItem;
import prerna.poi.main.MetaModelCreator;
import prerna.poi.main.helper.CSVFileHelper;
import prerna.query.querystruct.CsvQueryStruct;
import prerna.query.querystruct.QueryStruct2;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.execptions.SemossPixelException;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.qs.AbstractQueryStructReactor;
import prerna.security.AbstractHttpHelper;
import prerna.util.BeanFiller;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class DropBoxFileRetrieverReactor extends AbstractQueryStructReactor{

	//private String[] keysToGet;

	public DropBoxFileRetrieverReactor() {
		this.keysToGet = new String[] { "name", "path" };
	}

	@Override
	protected QueryStruct2 createQueryStruct() {
		String fileName = this.curRow.get(0).toString();
		if (fileName == null || fileName.length() <= 0) {
			throw new IllegalArgumentException("Need to specify file name");
		}
		String dropboxPath = this.curRow.get(1).toString();
		if (dropboxPath == null || dropboxPath.length() <= 0) {
			throw new IllegalArgumentException("Need to specify file path");
		}

		//get access token
		String accessToken = null;
		User2 user = this.insight.getUser2();
		try{
			if(user==null){
				SemossPixelException exception = new SemossPixelException();
				exception.setContinueThreadOfExecution(false);
				Map<String, Object> retMap = new HashMap<String, Object>();
				retMap.put("type", "dropbox");
				retMap.put("message", "Please login to your DropBox account");
				exception.setAdditionalReturn(new NounMetadata(retMap, PixelDataType.ERROR, PixelOperationType.LOGGIN_REQUIRED_ERROR, PixelOperationType.ERROR));
				throw exception;
			}
			else if (user != null) {
				AccessToken msToken = user.getAccessToken(AuthProvider.DROPBOX.name());
				accessToken=msToken.getAccess_token();
			}
		}
		catch (Exception e) {
			SemossPixelException exception = new SemossPixelException();
			exception.setContinueThreadOfExecution(false);
			Map<String, Object> retMap = new HashMap<String, Object>();
			retMap.put("type", "dropbox");
			retMap.put("message", "Please login to your DropBox account");
			exception.setAdditionalReturn(new NounMetadata(retMap, PixelDataType.ERROR, PixelOperationType.LOGGIN_REQUIRED_ERROR, PixelOperationType.ERROR));
			throw exception;
		}

		//

		// lists the various files for this user
		// if the 
		// name of the object to return
		String objectName = "prerna.om.RemoteItem"; // it will fill this object and return the data
		String [] beanProps = {"name","id","url"}; // add is done when you have a list
		String jsonPattern = "[metadata.name,metadata.id,link]";

		// you fill what you want to send on the API call
		String url_str = "https://api.dropboxapi.com/2/files/get_temporary_link";
		Hashtable params = new Hashtable();
		params.put("path", dropboxPath);

		String output = AbstractHttpHelper.makePostCall(url_str, accessToken, params, true);

		// fill the bean with the return. This return will have a url to download the file from which is done below
		RemoteItem link = (RemoteItem) BeanFiller.fillFromJson(output, jsonPattern, beanProps, new RemoteItem());
		String filePath = DIHelper.getInstance().getProperty(Constants.INSIGHT_CACHE_DIR) + "\\"
				+ DIHelper.getInstance().getProperty(Constants.CSV_INSIGHT_CACHE_FOLDER);
		filePath += "\\" + Utility.getRandomString(10) + ".csv";
		filePath = filePath.replace("\\", "/");
		try {
			URL urlDownload = new URL(link.getUrl());
			File destination = new File(filePath);
			FileUtils.copyURLToFile(urlDownload, destination);
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// get datatypes
		CSVFileHelper helper = new CSVFileHelper();
		helper.setDelimiter(',');
		helper.parse(filePath);
		MetaModelCreator predictor = new MetaModelCreator(helper, null);
		Map<String, String> dataTypes = predictor.getDataTypeMap();
		CsvQueryStruct qs = new CsvQueryStruct();
		for (String key : dataTypes.keySet()) {
			qs.addSelector("DND", key);
		}
		helper.clear();
		qs.merge(this.qs);
		qs.setCsvFilePath(filePath);
		qs.setDelimiter(',');
		qs.setColumnTypes(dataTypes);
		return qs;


	}


}