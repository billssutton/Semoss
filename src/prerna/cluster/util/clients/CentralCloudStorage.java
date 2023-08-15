package prerna.cluster.util.clients;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.auth.utils.SecurityEngineUtils;
import prerna.auth.utils.SecurityProjectUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.engine.api.IDatabase;
import prerna.engine.api.IDatabase.DATABASE_TYPE;
import prerna.engine.impl.AbstractDatabase;
import prerna.engine.impl.SmssUtilities;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.engine.impl.storage.AbstractRCloneStorageEngine;
import prerna.engine.impl.storage.AzureBlobStorageEngine;
import prerna.engine.impl.storage.GoogleCloudStorageEngine;
import prerna.engine.impl.storage.MinioStorageEngine;
import prerna.engine.impl.storage.S3StorageEngine;
import prerna.project.api.IProject;
import prerna.test.TestUtilityMethods;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.EngineSyncUtility;
import prerna.util.ProjectSyncUtility;
import prerna.util.SMSSWebWatcher;
import prerna.util.Utility;
import prerna.util.sql.RdbmsTypeEnum;

public class CentralCloudStorage implements ICloudClient {

	private static final Logger classLogger = LogManager.getLogger(CentralCloudStorage.class);

	public static final String DB_BLOB = "semoss-db";
	public static final String PROJECT_BLOB = "semoss-project";
	public static final String USER_BLOB = "semoss-user";
	public static final String DB_IMAGES_BLOB = "semoss-dbimagecontainer";
	public static final String PROJECT_IMAGES_BLOB = "semoss-projectimagecontainer";
	
	private static ICloudClient instance = null;
	private static AbstractRCloneStorageEngine storageEngine = null;
	
	private static final String FILE_SEPARATOR = java.nio.file.FileSystems.getDefault().getSeparator();
	private static final String SMSS_POSTFIX = "-smss";

	private static String DATABASE_FOLDER = null;
	private static String PROJECT_FOLDER = null;
	private static String USER_FOLDER = null;
	
	// these can change based on the cloud client type
	private static String DB_CONTAINER_PREFIX = "/" + DB_BLOB + "/";
	private static String PROJECT_CONTAINER_PREFIX = "/" + PROJECT_BLOB + "/";
	private static String USER_CONTAINER_PREFIX = "/" + USER_BLOB + "/";
	
	// these are all the legacy keys before we had engine
	// need to do some mapping between these ones and the ones used 
	// in the storage engine classes
	// will keep all of these indendted to identify that
//		public static String RCLONE_PATH = "RCLONE_PATH";
//	
//		// s3 prefix is used for minio as well
//		public static final String S3_REGION_KEY = "S3_REGION";
//		public static final String S3_BUCKET_KEY = "S3_BUCKET";
//		public static final String S3_ACCESS_KEY = "S3_ACCESS_KEY";
//		public static final String S3_SECRET_KEY = "S3_SECRET_KEY";
//		public static final String S3_ENDPOINT_KEY = "S3_ENDPOINT";
//		
//		// gcp keys
//		public static final String GCP_SERVICE_ACCOUNT_FILE_KEY = "GCP_SERVICE_ACCOUNT_FILE";
//		public static final String GCP_REGION_KEY = "GCP_REGION";
//		public static final String GCP_BUCKET_KEY = "GCP_BUCKET";
//		
//		// az keys
//		public static final String AZ_CONN_STRING = "AZ_CONN_STRING";
//		public static final String AZ_NAME = "AZ_NAME";
//		public static final String AZ_KEY = "AZ_KEY";
//		public static final String SAS_URL = "SAS_URL";
//		public static final String AZ_URI = "AZ_URI";
//		public static final String STORAGE = "STORAGE"; // says if this is local / cluster
//		public static final String KEY_HOME = "KEY_HOME"; // this is where the various keys are cycled
//	
//		protected String rclonePath = "rclone";
//		protected String rcloneConfigF = null;
	
	private CentralCloudStorage() throws Exception {
		buildStorageEngine();
	}
	
	public static ICloudClient getInstance() throws Exception {
		if(instance != null) {
			return instance;
		}
		
		if(instance == null) {
			synchronized (CentralCloudStorage.class) {
				if(instance != null) {
					return instance;
				}
				
				instance = new CentralCloudStorage();
				CentralCloudStorage.DATABASE_FOLDER = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER) + FILE_SEPARATOR + Constants.DB_FOLDER;
				CentralCloudStorage.PROJECT_FOLDER = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER) + FILE_SEPARATOR + Constants.PROJECT_FOLDER;
				CentralCloudStorage.USER_FOLDER = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER) + FILE_SEPARATOR + Constants.USER_FOLDER;
			}
		}
		
		return instance;
	}
	
	
	private static synchronized void buildStorageEngine() throws Exception {
		Properties props = new Properties();
		AppCloudClientProperties clientProps = new AppCloudClientProperties();
		if(ClusterUtil.STORAGE_PROVIDER == null || ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("AZURE")){
			
			storageEngine = new AzureBlobStorageEngine();
			propertiesMigratePut(props, AzureBlobStorageEngine.AZ_ACCOUNT_NAME, clientProps, AbstractClientBuilder.AZ_NAME);
			propertiesMigratePut(props, AzureBlobStorageEngine.AZ_PRIMARY_KEY, clientProps, AbstractClientBuilder.AZ_KEY);
			propertiesMigratePut(props, AzureBlobStorageEngine.AZ_CONN_STRING, clientProps, AbstractClientBuilder.AZ_CONN_STRING);
			
			// we have a different structure for AZ storage since it doesn't represent the blobs as folders
			CentralCloudStorage.DB_CONTAINER_PREFIX = "db-";
			CentralCloudStorage.PROJECT_CONTAINER_PREFIX = "project-";
			CentralCloudStorage.USER_CONTAINER_PREFIX = "user-";
			
		}
		else if(ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("AWS") ||
				ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("S3")){
			
			storageEngine = new S3StorageEngine();
			propertiesMigratePut(props, S3StorageEngine.S3_REGION_KEY, clientProps, AbstractClientBuilder.S3_REGION_KEY);
			propertiesMigratePut(props, S3StorageEngine.S3_BUCKET_KEY, clientProps, AbstractClientBuilder.S3_BUCKET_KEY);
			propertiesMigratePut(props, S3StorageEngine.S3_ACCESS_KEY, clientProps, AbstractClientBuilder.S3_ACCESS_KEY);
			propertiesMigratePut(props, S3StorageEngine.S3_SECRET_KEY, clientProps, AbstractClientBuilder.S3_SECRET_KEY);

		} 
		else if(ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("MINIO")){
			
			storageEngine = new MinioStorageEngine();
			propertiesMigratePut(props, MinioStorageEngine.MINIO_REGION_KEY, clientProps, AbstractClientBuilder.S3_REGION_KEY);
			propertiesMigratePut(props, MinioStorageEngine.MINIO_BUCKET_KEY, clientProps, AbstractClientBuilder.S3_BUCKET_KEY);
			propertiesMigratePut(props, MinioStorageEngine.MINIO_ACCESS_KEY, clientProps, AbstractClientBuilder.S3_ACCESS_KEY);
			propertiesMigratePut(props, MinioStorageEngine.MINIO_SECRET_KEY, clientProps, AbstractClientBuilder.S3_SECRET_KEY);
			propertiesMigratePut(props, MinioStorageEngine.MINIO_ENDPOINT_KEY, clientProps, AbstractClientBuilder.S3_ENDPOINT_KEY);

			if(!props.containsKey(MinioStorageEngine.MINIO_REGION_KEY)) {
				propertiesMigratePut(props, MinioStorageEngine.MINIO_REGION_KEY, clientProps, MinioStorageEngine.MINIO_REGION_KEY);
			}
			if(!props.containsKey(MinioStorageEngine.MINIO_BUCKET_KEY)) {
				propertiesMigratePut(props, MinioStorageEngine.MINIO_BUCKET_KEY, clientProps, MinioStorageEngine.MINIO_BUCKET_KEY);
			}
			if(!props.containsKey(MinioStorageEngine.MINIO_ACCESS_KEY)) {
				propertiesMigratePut(props, MinioStorageEngine.MINIO_ACCESS_KEY, clientProps, MinioStorageEngine.MINIO_ACCESS_KEY);
			}
			if(!props.containsKey(MinioStorageEngine.MINIO_SECRET_KEY)) {
				propertiesMigratePut(props, MinioStorageEngine.MINIO_SECRET_KEY, clientProps, MinioStorageEngine.MINIO_SECRET_KEY);
			}
			if(!props.containsKey(MinioStorageEngine.MINIO_ENDPOINT_KEY)) {
				propertiesMigratePut(props, MinioStorageEngine.MINIO_ENDPOINT_KEY, clientProps, MinioStorageEngine.MINIO_ENDPOINT_KEY);
			}
		} 
		else if(ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("GCS") ||
				ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("GCP") ||
				ClusterUtil.STORAGE_PROVIDER.equalsIgnoreCase("GOOGLE")){
			
			storageEngine = new GoogleCloudStorageEngine();
			propertiesMigratePut(props, GoogleCloudStorageEngine.GCS_REGION, clientProps, AbstractClientBuilder.GCP_REGION_KEY);
			propertiesMigratePut(props, GoogleCloudStorageEngine.GCS_SERVICE_ACCOUNT_FILE_KEY, clientProps, AbstractClientBuilder.GCP_SERVICE_ACCOUNT_FILE_KEY);
			propertiesMigratePut(props, GoogleCloudStorageEngine.GCS_BUCKET_KEY, clientProps, AbstractClientBuilder.GCP_BUCKET_KEY);
			
		}
		else {
			throw new IllegalArgumentException("You have specified an incorrect storage provider");
		}
		
		storageEngine.open(props);
	}
	
	/**
	 * 
	 * @param prop
	 * @param propKey
	 * @param clientProps
	 * @param oldKey
	 */
	private static void propertiesMigratePut(Properties prop, String propKey, AppCloudClientProperties clientProps, String oldKey) {
		if(clientProps.get(oldKey) != null) {
			prop.put(propKey, clientProps.get(oldKey));
		}
	}
	
	
	@Override
	public void pushDatabase(String databaseId) throws IOException, InterruptedException {
		IDatabase database = Utility.getDatabase(databaseId, false);
		if (database == null) {
			throw new IllegalArgumentException("Database not found...");
		}

		DATABASE_TYPE databaseType = database.getDatabaseType();

		// We need to push the folder alias__appId and the file alias__appId.smss
		String databaseName = null;
		if (databaseType == DATABASE_TYPE.APP){
			databaseName = database.getEngineName();
		} else{
			databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		}

		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		String localDatabaseFolder = DATABASE_FOLDER + FILE_SEPARATOR + aliasAndDatabaseId;
		String localSmssFileName = aliasAndDatabaseId + ".smss";
		String localSmssFilePath = DATABASE_FOLDER + FILE_SEPARATOR + localSmssFileName;

		String sharedRCloneConfig = null;

		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;
		String storageSmssFolder = DB_CONTAINER_PREFIX + databaseId + SMSS_POSTFIX;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to push database");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database "+ aliasAndDatabaseId + " is locked");
		try {
			DIHelper.getInstance().removeEngineProperty(databaseId);
			database.close();
			
			if(storageEngine.canReuseRcloneConfig()) {
				sharedRCloneConfig = storageEngine.createRCloneConfig();
			}
			storageEngine.syncLocalToStorage(localDatabaseFolder, storageDatabaseFolder, sharedRCloneConfig);
			storageEngine.copyToStorage(localSmssFilePath, storageSmssFolder, sharedRCloneConfig);
		} finally {
			try {
				// Re-open the database
				Utility.getDatabase(databaseId, false);
				if(sharedRCloneConfig != null) {
					storageEngine.deleteRcloneConfig(sharedRCloneConfig);
				}
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Database "+ aliasAndDatabaseId + " is unlocked");
		}
	}

	@Override
	public void pushLocalDatabaseFile(String databaseId, RdbmsTypeEnum dbType) throws Exception {
		if (dbType != RdbmsTypeEnum.SQLITE
				&& dbType != RdbmsTypeEnum.H2_DB) {
			throw new IllegalArgumentException("Unallowed database type. Must be either SQLITE or H2");
		}
		
		IDatabase database = Utility.getDatabase(databaseId, false);
		if (database == null) {
			throw new IllegalArgumentException("Database not found...");
		}
		
		// We need to push the folder alias__appId and the file alias__appId.smss
		String databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		String localDatabaseFolder = DATABASE_FOLDER + FILE_SEPARATOR + aliasAndDatabaseId;
		
		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;
		
		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to push db file");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database "+ aliasAndDatabaseId + " is locked");
		try {
			DIHelper.getInstance().removeEngineProperty(databaseId);
			database.close();

			classLogger.info("Pushing local database file from " + localDatabaseFolder + " to remote " + storageDatabaseFolder);
			List<String> dbFiles = null;
			if (dbType == RdbmsTypeEnum.SQLITE) {
				dbFiles = getSqlLiteFile(localDatabaseFolder);
			} else if (dbType == RdbmsTypeEnum.H2_DB) {
				dbFiles = getH2File(localDatabaseFolder);
			}
			for (String dbFileName : dbFiles) {
				storageEngine.copyToStorage(localDatabaseFolder+"/"+dbFileName, storageDatabaseFolder);
			}
		} finally {
			try {
				// Re-open the database
				Utility.getDatabase(databaseId, false);
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Database "+ aliasAndDatabaseId + " is unlocked");
		}
	}

	@Override
	public void pullDatabase(String databaseId) throws IOException, InterruptedException {
		pullDatabase(databaseId, false);
	}

	@Override
	public void pullDatabase(String databaseId, boolean databaseAlreadyLoaded) throws IOException, InterruptedException {
		IDatabase database = null;
		if (databaseAlreadyLoaded) {
			database = Utility.getDatabase(databaseId, false);
			if (database == null) {
				throw new IllegalArgumentException("Database not found...");
			}
		}

		// We need to push the folder alias__appId and the file alias__appId.smss
		String databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		String localDatabaseFolder = DATABASE_FOLDER + FILE_SEPARATOR + aliasAndDatabaseId;

		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;
		String storageSmssFolder = DB_CONTAINER_PREFIX + databaseId + SMSS_POSTFIX;

		String sharedRCloneConfig = null;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to push database");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database "+ aliasAndDatabaseId + " is locked");
		
		try {
			if(database != null) {
				DIHelper.getInstance().removeEngineProperty(databaseId);
				database.close();
			}
			if(storageEngine.canReuseRcloneConfig()) {
				sharedRCloneConfig = storageEngine.createRCloneConfig();
			}
			
			// List the smss directory to get the alias + app id
			List<String> results = storageEngine.list(storageSmssFolder, sharedRCloneConfig);
			boolean foundSmss = false;
			for (String result : results) {
				if (result.endsWith(".smss")) {
					foundSmss = true;
					break;
				}
			}
			if (!foundSmss) {
				classLogger.warn("Could not find smss file for database " + aliasAndDatabaseId + " in remote="+storageSmssFolder);
				try {
					classLogger.warn("Attemping to fix legacy database structure for database " + aliasAndDatabaseId);
					classLogger.warn("Attemping to fix legacy database structure for database " + aliasAndDatabaseId);
					classLogger.warn("Attemping to fix legacy database structure for database " + aliasAndDatabaseId);
					classLogger.warn("Attemping to fix legacy database structure for database " + aliasAndDatabaseId);
					fixLegacyDbStructure(databaseId);
				} catch(IOException | InterruptedException e) {
					classLogger.info(Constants.STACKTRACE, e);
					throw new IOException("Failed to pull database " + aliasAndDatabaseId);
				}
				
				// try again
				results = storageEngine.list(storageSmssFolder, sharedRCloneConfig);
				for (String result : results) {
					if (result.endsWith(".smss")) {
						foundSmss = true;
						break;
					}
				}
				
				if (!foundSmss) {
					classLogger.warn("Could not find smss file for database " + aliasAndDatabaseId);
					throw new IOException("Failed to pull database " + aliasAndDatabaseId);
				} else {
					// we just fixed the structure and this was pulled and synched up
					// can just return from here
					return;
				}
			}
			
			// Close the database so that we can pull without file lock issues
			if (databaseAlreadyLoaded) {
				DIHelper.getInstance().removeEngineProperty(databaseId);
				database.close();
			}

			// Make the app directory (if it doesn't already exist)
			File engineFolder = new File(Utility.normalizePath(localDatabaseFolder));
			engineFolder.mkdirs(); 

			// Pull the contents of the app folder before the smss
			classLogger.info("Pulling database from remote=" + Utility.cleanLogString(aliasAndDatabaseId) + " to target=" + engineFolder.getPath());
			storageEngine.syncStorageToLocal(storageDatabaseFolder, localDatabaseFolder, sharedRCloneConfig);
			classLogger.debug("Done pulling from remote=" + Utility.cleanLogString(aliasAndDatabaseId) + " to target=" + engineFolder.getPath());

			// Now pull the smss
			classLogger.info("Pulling smss from remote=" + Utility.cleanLogString(storageSmssFolder) + " to target=" + DATABASE_FOLDER);
			// THIS MUST BE COPY AND NOT SYNC TO AVOID DELETING EVERYTHING IN THE DB FOLDER
			storageEngine.copyToLocal(storageSmssFolder, DATABASE_FOLDER, sharedRCloneConfig);
			classLogger.debug("Done pulling from remote=" + Utility.cleanLogString(storageSmssFolder) + " to target=" + DATABASE_FOLDER);

			// Catalog the db if it is new
			if (!databaseAlreadyLoaded) {
				classLogger.info("Synchronizing the database metadata for " + aliasAndDatabaseId);
				SMSSWebWatcher.catalogDB(results.get(0), DATABASE_FOLDER);
			}
		} finally {
			try {
				// Re-open the database
				Utility.getDatabase(databaseId, false);
				if(sharedRCloneConfig != null) {
					storageEngine.deleteRcloneConfig(sharedRCloneConfig);
				}
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Database "+ aliasAndDatabaseId + " is unlocked");
		}
	}
	
	@Override
	public void pullLocalDatabaseFile(String databaseId, RdbmsTypeEnum rdbmsType) throws IOException, InterruptedException {
		if (rdbmsType != RdbmsTypeEnum.SQLITE
				&& rdbmsType != RdbmsTypeEnum.H2_DB) {
			throw new IllegalArgumentException("Unallowed database type. Must be either SQLITE or H2");
		}
		
		IDatabase database = Utility.getDatabase(databaseId, false);
		if (database == null) {
			throw new IllegalArgumentException("Database not found...");
		}
		String databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		String localDatabaseFolder = DATABASE_FOLDER + FILE_SEPARATOR + aliasAndDatabaseId;

		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;

		String sharedRCloneConfig = null;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to pull database file");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database "+ databaseId + " is locked");
		try {
			DIHelper.getInstance().removeEngineProperty(databaseId);
			database.close();
			
			if(storageEngine.canReuseRcloneConfig()) {
				sharedRCloneConfig = storageEngine.createRCloneConfig();
			}
			
			classLogger.info("Pulling database files for " + aliasAndDatabaseId + " from remote=" + databaseId);
			List<String> filesToPull = new ArrayList<>();
			List<String> cloudFiles = storageEngine.list(storageDatabaseFolder, sharedRCloneConfig);
			for(String cloudF : cloudFiles) {
				if(rdbmsType == RdbmsTypeEnum.SQLITE && cloudF.endsWith(".sqlite")) {
					filesToPull.add(cloudF);	
				} else if(rdbmsType == RdbmsTypeEnum.H2_DB && cloudF.endsWith(".mv.db")) {
					filesToPull.add(cloudF);	
				}
			}
			
			for(String fileToPull : filesToPull) {
				storageEngine.copyToLocal(storageDatabaseFolder+"/"+fileToPull, localDatabaseFolder, sharedRCloneConfig);
			}
		} finally {
			try {
				// Re-open the database
				Utility.getDatabase(databaseId, false);
				if(sharedRCloneConfig != null) {
					storageEngine.deleteRcloneConfig(sharedRCloneConfig);
				}
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Database "+ aliasAndDatabaseId + " is unlocked");
		}
	}

	@Override
	public void pushDatabaseSmss(String databaseId) throws Exception {
		// We need to push the file alias__appId.smss
		String databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		String localSmssFileName = SmssUtilities.getUniqueName(databaseName, databaseId) + ".smss";
		String localSmssFilePath = Utility.normalizePath(DATABASE_FOLDER + FILE_SEPARATOR + localSmssFileName);
		
		String storageSmssFolder = DB_CONTAINER_PREFIX + databaseId + SMSS_POSTFIX;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to push database");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database " + aliasAndDatabaseId + " is locked");
		try {
			storageEngine.copyToStorage(localSmssFilePath, storageSmssFolder);
		} finally {
			lock.unlock();
			classLogger.info("Database " + aliasAndDatabaseId + " is unlocked");
		}
	}

	@Override
	public void pushOwl(String databaseId) throws Exception {
		IDatabase database = Utility.getDatabase(databaseId, false);
		if (database == null) {
			throw new IllegalArgumentException("Database not found...");
		}
		
		// We need to push the file alias__appId.smss
		String databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		File localOwlF = SmssUtilities.getOwlFile(database.getSmssProp());
		String localOwlFile = localOwlF.getAbsolutePath();
		String localOwlPositionFile = localOwlF.getParent() + "/" + AbstractDatabase.OWL_POSITION_FILENAME;
		boolean hasPositionFile = new File(localOwlPositionFile).exists();
		
		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;

		String sharedRCloneConfig = null;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to push database owl and postions.json");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database " + aliasAndDatabaseId + " is locked");
		try {
			if(storageEngine.canReuseRcloneConfig()) {
				sharedRCloneConfig = storageEngine.createRCloneConfig();
			}
			//close the owl
			database.getBaseDataEngine().close();
			storageEngine.copyToStorage(localOwlFile, storageDatabaseFolder, sharedRCloneConfig);
			if(hasPositionFile) {
				storageEngine.copyToStorage(localOwlPositionFile, storageDatabaseFolder, sharedRCloneConfig);
			}
		} finally {
			try {
				database.setOWL(localOwlFile);
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			if(sharedRCloneConfig != null) {
				try {
					storageEngine.deleteRcloneConfig(sharedRCloneConfig);
				} catch(Exception e) {
					classLogger.error(Constants.STACKTRACE, e);
				}
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Database "+ aliasAndDatabaseId + " is unlocked");
		}
	}

	@Override
	public void pullOwl(String databaseId) throws Exception {
		IDatabase database = Utility.getDatabase(databaseId, false);
		if (database == null) {
			throw new IllegalArgumentException("Database not found...");
		}
		
		// We need to push the file alias__appId.smss
		String databaseName = SecurityEngineUtils.getEngineAliasForId(databaseId);
		String aliasAndDatabaseId = SmssUtilities.getUniqueName(databaseName, databaseId);
		String localDatabaseFolder = DATABASE_FOLDER + FILE_SEPARATOR + aliasAndDatabaseId;

		File localOwlF = SmssUtilities.getOwlFile(database.getSmssProp());
		String localOwlFile = localOwlF.getAbsolutePath();
		String owlFileName = localOwlF.getName();
		
		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;
		String storageDatabaseOwl = storageDatabaseFolder + "/" + owlFileName;
		String storageDatabaseOwlPosition = storageDatabaseFolder + "/" + AbstractDatabase.OWL_POSITION_FILENAME;
		
		String sharedRCloneConfig = null;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndDatabaseId + " to pull database owl and postions.json");
		ReentrantLock lock = EngineSyncUtility.getEngineLock(databaseId);
		lock.lock();
		classLogger.info("Database " + aliasAndDatabaseId + " is locked");
		try {
			if(storageEngine.canReuseRcloneConfig()) {
				sharedRCloneConfig = storageEngine.createRCloneConfig();
			}
			//close the owl
			database.getBaseDataEngine().close();
			storageEngine.copyToLocal(storageDatabaseOwl, localDatabaseFolder);
			storageEngine.copyToLocal(storageDatabaseOwlPosition, localDatabaseFolder);
		} finally {
			try {
				database.setOWL(localOwlFile);
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			if(sharedRCloneConfig != null) {
				try {
					storageEngine.deleteRcloneConfig(sharedRCloneConfig);
				} catch(Exception e) {
					classLogger.error(Constants.STACKTRACE, e);
				}
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Database "+ aliasAndDatabaseId + " is unlocked");
		}
	}

	@Override
	public void pullDatabaseImageFolder() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushDatabaseImageFolder() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deleteDatabase(String databaseId) throws IOException, InterruptedException {
		String sharedRCloneConfig = null;
		if(storageEngine.canReuseRcloneConfig()) {
			sharedRCloneConfig = storageEngine.createRCloneConfig();
		}
		String storageDatabaseFolder = DB_CONTAINER_PREFIX + databaseId;
		String storageSmssFolder = DB_CONTAINER_PREFIX + databaseId + SMSS_POSTFIX;

		storageEngine.deleteFolderFromStorage(storageDatabaseFolder, sharedRCloneConfig);
		storageEngine.deleteFolderFromStorage(storageSmssFolder, sharedRCloneConfig);
	}

	@Override
	public void pushProject(String projectId) throws IOException, InterruptedException {
		IProject project = Utility.getProject(projectId, false);
		if (project == null) {
			throw new IllegalArgumentException("Project not found...");
		}

		// We need to push the folder alias__appId and the file alias__appId.smss
		String alias = project.getProjectName();
		if(alias == null) {
			alias = SecurityProjectUtils.getProjectAliasForId(projectId);
		}

		String aliasAndProjectId = alias + "__" + projectId;
		String localProjectFolder = PROJECT_FOLDER + FILE_SEPARATOR + aliasAndProjectId;
		String localSmssFileName = aliasAndProjectId + ".smss";
		String localSmssFilePath = PROJECT_FOLDER + FILE_SEPARATOR + localSmssFileName;

		String sharedRCloneConfig = null;

		String storageProjectFolder = PROJECT_CONTAINER_PREFIX + projectId;
		String storageSmssFolder = DB_CONTAINER_PREFIX + projectId + SMSS_POSTFIX;

		// synchronize on the project id
		classLogger.info("Applying lock for " + aliasAndProjectId + " to push project");
		ReentrantLock lock = ProjectSyncUtility.getProjectLock(projectId);
		lock.lock();
		classLogger.info("Project "+ aliasAndProjectId + " is locked");
		try {
			DIHelper.getInstance().removeProjectProperty(projectId);
			project.close();
			
			if(storageEngine.canReuseRcloneConfig()) {
				sharedRCloneConfig = storageEngine.createRCloneConfig();
			}
			storageEngine.syncLocalToStorage(localProjectFolder, storageProjectFolder, sharedRCloneConfig);
			storageEngine.copyToStorage(localSmssFilePath, storageSmssFolder, sharedRCloneConfig);
		} finally {
			try {
				// Re-open the database
				Utility.getDatabase(projectId, false);
				if(sharedRCloneConfig != null) {
					storageEngine.deleteRcloneConfig(sharedRCloneConfig);
				}
			} catch(Exception e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			// always unlock regardless of errors
			lock.unlock();
			classLogger.info("Project "+ aliasAndProjectId + " is unlocked");
		}
	}
	
	@Override
	public void pullProject(String projectId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pullProject(String projectId, boolean projectAlreadyLoaded) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void pushProjectSmss(String projectId) throws Exception {
		// We need to push the file alias__appId.smss
		String projectName = SecurityProjectUtils.getProjectAliasForId(projectId);
		String aliasAndProjectId = SmssUtilities.getUniqueName(projectName, projectId);
		String localSmssFileName = SmssUtilities.getUniqueName(projectName, projectId) + ".smss";
		String localSmssFilePath = Utility.normalizePath(PROJECT_FOLDER + FILE_SEPARATOR + localSmssFileName);
		
		String storageSmssFolder = PROJECT_CONTAINER_PREFIX + projectId + SMSS_POSTFIX;

		// synchronize on the app id
		classLogger.info("Applying lock for " + aliasAndProjectId + " to push project smss");
		ReentrantLock lock = ProjectSyncUtility.getProjectLock(projectId);
		lock.lock();
		classLogger.info("Project " + aliasAndProjectId + " is locked");
		try {
			storageEngine.copyToStorage(localSmssFilePath, storageSmssFolder);
		} finally {
			lock.unlock();
			classLogger.info("Project " + aliasAndProjectId + " is unlocked");
		}
		
	}
	
	@Override
	public void pullProjectImageFolder() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushProjectImageFolder() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteProject(String projectId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pullInsightsDB(String projectId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushInsightDB(String projectId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushEngineFolder(String appId, String absolutePath, String remoteRelativePath)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pullEngineFolder(String appId, String absolutePath, String remoteRelativePath)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushProjectFolder(String projectId, String absolutePath, String remoteRelativePath)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pullProjectFolder(String projectId, String absolutePath, String remoteRelativePath)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushInsight(String projectId, String insightId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pullInsight(String projectId, String insightId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushInsightImage(String projectId, String insightId, String oldImageFileName, String newImageFileName)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pullUserAssetOrWorkspace(String projectId, boolean isAsset, boolean projectAlreadyLoaded)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushUserAssetOrWorkspace(String projectId, boolean isAsset) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fixLegacyDbStructure(String appId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fixLegacyImageStructure() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fixLegacyUserAssetStructure(String appId, boolean isAsset) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * 
	 * @param directory
	 * @return
	 */
	protected List<String> getSqlLiteFile(String directory) {
		File dir = new File(directory);
		List<String> sqlFiles = new ArrayList<>();
		//search dir for .sqlite files 
		for(File file : dir.listFiles()) {
			if (file.getName().endsWith((".sqlite"))) {
				sqlFiles.add(file.getName());
			}
		}
		if(sqlFiles.size() > 1){
			if(sqlFiles.contains("insights_database.sqlite")) {
				sqlFiles.remove("insights_database.sqlite");
			}
		}
		if(sqlFiles.size() > 1) {
			classLogger.warn("Found multiple sqlite files. Only expecting 1 for database");
			classLogger.warn("Found multiple sqlite files. Only expecting 1 for database");
			classLogger.warn("Found multiple sqlite files. Only expecting 1 for database");
			classLogger.warn("Found multiple sqlite files. Only expecting 1 for database");
		}
		return sqlFiles;
	}
	
	/**
	 * 
	 * @param directory
	 * @return
	 */
	protected List<String> getH2File(String directory) {
		File dir = new File(directory);
		List<String> sqlFiles = new ArrayList<>();
		//search dir for .sqlite files 
		for(File file : dir.listFiles()) {
			if (file.getName().endsWith((".mv.db"))) {
				sqlFiles.add(file.getName());
			}
		}
		if(sqlFiles.size() > 1){
			if(sqlFiles.contains("insights_database.mv.db")) {
				sqlFiles.remove("insights_database.mv.db");
			}
		}
		if(sqlFiles.size() > 1) {
			classLogger.warn("Found multiple h2 files. Only expecting 1 for database");
			classLogger.warn("Found multiple h2 files. Only expecting 1 for database");
			classLogger.warn("Found multiple h2 files. Only expecting 1 for database");
			classLogger.warn("Found multiple h2 files. Only expecting 1 for database");
		}
		return sqlFiles;
	}

	/**
	 * 
	 * @param project
	 * @param specificProjectFolder
	 * @return
	 */
	protected String getInsightDB(IProject project, String specificProjectFolder) {
		RdbmsTypeEnum insightDbType = project.getInsightDatabase().getDbType();
		String insightDbName = null;
		if (insightDbType == RdbmsTypeEnum.H2_DB) {
			insightDbName = "insights_database.mv.db";
		} else {
			insightDbName = "insights_database.sqlite";
		}
		File dir = new File(specificProjectFolder);
		for (File file : dir.listFiles()) {
			if (file.getName().equalsIgnoreCase(insightDbName)){
				return file.getName();
			}
		}
		throw new IllegalArgumentException("There is no insight database for project: " + project.getProjectName());
	}
	
	@Override
	public List<String> listAllBlobContainers() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteContainer(String containerId) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String createRcloneConfig() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////

	
	public static void main(String[] args) throws Exception {
		TestUtilityMethods.loadAll("C:/workspace/Semoss_Dev/RDF_Map.prop");
		Properties coreProp = DIHelper.getInstance().getCoreProp();
		coreProp.put("SEMOSS_STORAGE_PROVIDER", "MINIO");
		coreProp.put(MinioStorageEngine.MINIO_REGION_KEY, "us-east-1");
		coreProp.put(MinioStorageEngine.MINIO_ACCESS_KEY, "***REMOVED***");
		coreProp.put(MinioStorageEngine.MINIO_SECRET_KEY, "***REMOVED***");
		coreProp.put(MinioStorageEngine.MINIO_ENDPOINT_KEY, "http://localhost:9000");
		coreProp.put(Constants.ENGINE, "CENTRAL_STORAGE");

		{
			String baseFolder = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
			String engineProp = baseFolder + "\\db\\diabetes sanjay and sarji__56af9395-64fd-40a2-b68c-bbd6961336a5.smss";
			IDatabase sampleDb = new RDBMSNativeEngine();
			sampleDb.openDB(engineProp);
			DIHelper.getInstance().setEngineProperty("56af9395-64fd-40a2-b68c-bbd6961336a5", sampleDb);
		}
		
		
		ICloudClient centralStorage = CentralCloudStorage.getInstance();
		centralStorage.pushDatabase("56af9395-64fd-40a2-b68c-bbd6961336a5");
		centralStorage.pullDatabase("56af9395-64fd-40a2-b68c-bbd6961336a5", true);
		centralStorage.pullLocalDatabaseFile("56af9395-64fd-40a2-b68c-bbd6961336a5", RdbmsTypeEnum.H2_DB);
	}

}
