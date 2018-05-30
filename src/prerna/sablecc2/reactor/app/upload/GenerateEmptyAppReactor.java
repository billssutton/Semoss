package prerna.sablecc2.reactor.app.upload;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import prerna.engine.api.IEngine;
import prerna.engine.impl.app.AppEngine;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.solr.SolrIndexEngine;
import prerna.solr.SolrUtility;
import prerna.util.Constants;
import prerna.util.DIHelper;

public class GenerateEmptyAppReactor extends AbstractReactor {

	private static final String CLASS_NAME = GenerateEmptyAppReactor.class.getName();

	/*
	 * This class is used to construct an empty app
	 * This app contains no data (no data file or OWL)
	 * This app only contains insights
	 * The idea being that the insights are parameterized and can be applied to various data sources
	 */

	public GenerateEmptyAppReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.APP.getKey()};
	}

	@Override
	public NounMetadata execute() {
		Logger logger = getLogger(CLASS_NAME);
		this.organizeKeys();
		String appName = this.keyValue.get(this.keysToGet[0]);
		if(appName == null || appName.isEmpty()) {
			throw new IllegalArgumentException("Need to provide a name for the app");
		}
		// need to make sure the app is unique
		boolean containsApp = true;
		try {
			containsApp = SolrIndexEngine.getInstance().containsApp(appName);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		if(containsApp) {
			throw new IllegalArgumentException("App name already exists.  Please provide a unique app name");
		}
		// need to make sure we are not overriding something that already exists in the file system
		final String FILE_SEP = System.getProperty("file.separator");
		String baseFolder = DIHelper.getInstance().getProperty("BaseFolder");
		// need to make sure app name doesn't already exist
		String appLocation = baseFolder + FILE_SEP + "db" + FILE_SEP + appName;
		File appFolder = new File(appLocation);
		if(appFolder.exists()) {
			throw new IllegalArgumentException("Database folder already contains an app directory with the same name. Please delete the existing app folder or provide a unique app name");
		}

		logger.info("Done validating app");
		logger.info("Starting app creation");

		/*
		 * Things we need to do
		 * 1) make directory
		 * 2) make insights database
		 * 3) make special smss
		 * 4) load into solr
		 */

		logger.info("Start generating app folder");
		appFolder.mkdirs();
		logger.info("Done generating app folder");

		logger.info("Start generating insights database");
		IEngine insightDb = UploadUtilities.generateInsightsDatabase(appName);
		logger.info("Done generating insights database");

		// add to DIHelper so we dont auto load with the file watcher
		File tempSmss = null;
		logger.info("Start generating temp smss");
		try {
			tempSmss = UploadUtilities.createTemporaryAppSmss(appName);
			DIHelper.getInstance().getCoreProp().setProperty(appName + "_" + Constants.STORE, tempSmss.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
		logger.info("Done generating temp smss");

		logger.info("Start loading into solr");
		try {
			SolrUtility.addAppToSolr(appName);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		logger.info("Done loading into solr");

		AppEngine appEng = new AppEngine();
		appEng.setEngineId(appName);
		appEng.setInsightDatabase(insightDb);
		// only at end do we add to DIHelper
		DIHelper.getInstance().setLocalProperty(appName, appEng);
		String appNames = (String) DIHelper.getInstance().getLocalProp(Constants.ENGINES);
		appNames = appNames + ";" + appName;
		DIHelper.getInstance().setLocalProperty(Constants.ENGINES, appNames);
		
		// and rename .temp to .smss
		File smssFile = new File(tempSmss.getAbsolutePath().replace(".temp", ".smss"));
		try {
			FileUtils.copyFile(tempSmss, smssFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		tempSmss.delete();
		
		// update DIHelper & engine smss file location
		appEng.setPropFile(smssFile.getAbsolutePath());
		DIHelper.getInstance().getCoreProp().setProperty(appName + "_" + Constants.STORE, smssFile.getAbsolutePath());
		DIHelper.getInstance().setLocalProperty(appName, appEng);
		String engineNames = (String) DIHelper.getInstance().getLocalProp(Constants.ENGINES);
		engineNames = engineNames + ";" + appName;
		DIHelper.getInstance().setLocalProperty(Constants.ENGINES, engineNames);
		
		return new NounMetadata(true, PixelDataType.BOOLEAN);
	}
}
