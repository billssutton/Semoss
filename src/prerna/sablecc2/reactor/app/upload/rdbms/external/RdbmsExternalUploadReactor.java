package prerna.sablecc2.reactor.app.upload.rdbms.external;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.openrdf.model.vocabulary.RDFS;

import com.google.common.io.Files;

import prerna.algorithm.api.SemossDataType;
import prerna.auth.AuthProvider;
import prerna.auth.User;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityAppUtils;
import prerna.auth.utils.SecurityQueryUtils;
import prerna.auth.utils.SecurityUpdateUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.engine.api.IEngine;
import prerna.engine.api.IEngine.ACTION_TYPE;
import prerna.engine.api.IRDBMSEngine;
import prerna.engine.api.impl.util.Owler;
import prerna.engine.impl.AbstractEngine;
import prerna.engine.impl.rdbms.ImpalaEngine;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.engine.impl.rdf.RDFFileSesameEngine;
import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.poi.main.RDBMSEngineCreationHelper;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.execptions.SemossPixelException;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.sablecc2.reactor.app.upload.UploadInputUtility;
import prerna.sablecc2.reactor.app.upload.UploadUtilities;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.EngineSyncUtility;
import prerna.util.Utility;
import prerna.util.git.GitRepoUtils;
import prerna.util.sql.AbstractSqlQueryUtil;
import prerna.util.sql.RdbmsTypeEnum;
import prerna.util.sql.SqlQueryUtilFactory;

public class RdbmsExternalUploadReactor extends AbstractReactor {

	private static final String DIR_SEPARATOR = java.nio.file.FileSystems.getDefault().getSeparator();
	private static final String CLASS_NAME = RdbmsExternalUploadReactor.class.getName();

	private static final String[] JDBC_CONSTANTS = {Constants.USE_CONNECTION_POOLING,
			Constants.POOL_MIN_SIZE,
			Constants.POOL_MAX_SIZE,
			Constants.CONNECTION_QUERY_TIMEOUT,
			Constants.FETCH_SIZE};
	
	// we need to define some variables that are stored at the class level
	// so that we can properly account for cleanup if errors occur
	protected transient Logger logger;
	protected transient String appId;
	protected transient String appName;
	protected transient IEngine engine;
	protected transient File appFolder;
	protected transient File tempSmss;
	protected transient File smssFile;

	protected transient boolean error = false;

	public RdbmsExternalUploadReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.CONNECTION_DETAILS.getKey(), UploadInputUtility.APP, 
				UploadInputUtility.METAMODEL, ReactorKeysEnum.EXISTING.getKey() };
	}

	@Override
	public NounMetadata execute() {
		this.logger = getLogger(this.getClass().getName());
		User user = null;
		boolean security = AbstractSecurityUtils.securityEnabled();
		if (security) {
			user = this.insight.getUser();
			if (user == null) {
				NounMetadata noun = new NounMetadata(
						"User must be signed into an account in order to create a database", PixelDataType.CONST_STRING,
						PixelOperationType.ERROR, PixelOperationType.LOGGIN_REQUIRED_ERROR);
				SemossPixelException err = new SemossPixelException(noun);
				err.setContinueThreadOfExecution(false);
				throw err;
			}

			if (AbstractSecurityUtils.anonymousUsersEnabled()) {
				if (this.insight.getUser().isAnonymous()) {
					throwAnonymousUserError();
				}
			}

			// throw error is user doesn't have rights to publish new apps
			if (AbstractSecurityUtils.adminSetPublisher() && !SecurityQueryUtils.userIsPublisher(this.insight.getUser())) {
				throwUserNotPublisherError();
			}
		}

		organizeKeys();
		String appId = this.keyValue.get(this.keysToGet[1]);
		String userPassedExisting = this.keyValue.get(this.keysToGet[3]);
		boolean existingApp = false;
		IRDBMSEngine nativeEngine = null;

		// make sure both fields exist
		if (appId != null && userPassedExisting != null) {
			existingApp = Boolean.parseBoolean(userPassedExisting);
			
			IEngine engine = Utility.getEngine(appId);
			if(engine instanceof IRDBMSEngine) {
				nativeEngine = (IRDBMSEngine) engine;
			} else {
				throw new IllegalArgumentException("Engine must be a valid JDBC engine");
			}
		}

		// if user enters existing=true and the app doesn't exist
		if (existingApp && (appId == null || nativeEngine == null)) {
			throw new IllegalArgumentException("App " + appId + " does not exist");
		}
		this.appName = UploadInputUtility.getAppNameOrId(this.store);

		if (existingApp) {
			if (security) {
				// check if input is alias since we are adding to existing
				appId = SecurityQueryUtils.testUserEngineIdForAlias(user, appId);
				if (!SecurityAppUtils.userCanEditEngine(user, appId)) {
					NounMetadata noun = new NounMetadata(
							"User does not have sufficient priviledges to create or update an app",
							PixelDataType.CONST_STRING, PixelOperationType.ERROR);
					SemossPixelException err = new SemossPixelException(noun);
					err.setContinueThreadOfExecution(false);
					throw err;
				}
			} else {
				// check if input is alias since we are adding to existing
				appId = MasterDatabaseUtility.testEngineIdIfAlias(appId);
				if (!MasterDatabaseUtility.getAllEngineIds().contains(appId)) {
					throw new IllegalArgumentException("Database " + appId + " does not exist");
				}
			}

			this.appId = appId;
			this.engine = Utility.getEngine(appId);
			try {
				this.logger.info("Updating existing app");
				updateExistingApp();
				this.logger.info("Done updating existing app");
			} catch (Exception e) {
				e.printStackTrace();
				this.error = true;
				if (e instanceof SemossPixelException) {
					throw (SemossPixelException) e;
				} else {
					NounMetadata noun = new NounMetadata(e.getMessage(), PixelDataType.CONST_STRING,
							PixelOperationType.ERROR);
					SemossPixelException err = new SemossPixelException(noun);
					err.setContinueThreadOfExecution(false);
					throw err;
				}
			}
		} else { // if app doesn't exist create new
			try {
				// make a new id
				this.appId = UUID.randomUUID().toString();
				// validate app
				this.logger.info("Start validating app");
				UploadUtilities.validateApp(user, this.appName, this.appId);
				this.logger.info("Done validating app");
				// create app folder
				this.logger.info("Start generating app folder");
				this.appFolder = UploadUtilities.generateAppFolder(this.appId, this.appName);
				this.logger.info("Complete");
				generateNewApp();
				// and rename .temp to .smss
				this.smssFile = new File(this.tempSmss.getAbsolutePath().replace(".temp", ".smss"));
				FileUtils.copyFile(this.tempSmss, this.smssFile);
				this.tempSmss.delete();
				this.engine.setPropFile(this.smssFile.getAbsolutePath());
				UploadUtilities.updateDIHelper(this.appId, this.appName, this.engine, this.smssFile);
				// sync metadata
				this.logger.info("Process app metadata to allow for traversing across apps");
				UploadUtilities.updateMetadata(this.appId);

				// adding all the git here
				// make a version folder if one doesn't exist
				String versionFolder = appFolder.getAbsolutePath() + "/version";
				File file = new File(versionFolder);
				if (!file.exists()) {
					file.mkdir();
				}
				// I will assume the directory is there now
				GitRepoUtils.init(versionFolder);
				this.logger.info("Complete");
			} catch (Exception e) {
				e.printStackTrace();
				this.error = true;
				if (e instanceof SemossPixelException) {
					throw (SemossPixelException) e;
				} else {
					NounMetadata noun = new NounMetadata(e.getMessage(), PixelDataType.CONST_STRING,
							PixelOperationType.ERROR);
					SemossPixelException err = new SemossPixelException(noun);
					err.setContinueThreadOfExecution(false);
					throw err;
				}
			} finally {
				if (this.error) {
					// need to delete everything...
					cleanUpCreateNewError();
				}
			}
		}

		// even if no security, just add user as engine owner
		if (user != null) {
			List<AuthProvider> logins = user.getLogins();
			for (AuthProvider ap : logins) {
				SecurityUpdateUtils.addEngineOwner(this.appId, user.getAccessToken(ap).getId());
			}
		}

		ClusterUtil.reactorPushApp(this.appId);

		Map<String, Object> retMap = UploadUtilities.getAppReturnData(this.insight.getUser(), this.appId);
		return new NounMetadata(retMap, PixelDataType.UPLOAD_RETURN_MAP, PixelOperationType.MARKET_PLACE_ADDITION);
	}
	
	private void generateNewApp() throws Exception {
		Logger logger = getLogger(CLASS_NAME);
		
		Map<String, Object> connectionDetails = getConDetails();
		if(connectionDetails != null) {
			String host = (String) connectionDetails.get(AbstractSqlQueryUtil.HOSTNAME);
			if(host != null) {
				String testUpdatedHost = this.insight.getAbsoluteInsightFolderPath(host);
				File f = new File(testUpdatedHost);
				if (f.exists()) {
					// move the file
					// and then update the host value
					String newLocation = this.appFolder.getAbsolutePath() + DIR_SEPARATOR
							+ FilenameUtils.getName(f.getAbsolutePath());
					try {
						Files.move(f, new File(newLocation));
					} catch (IOException e) {
						throw new IOException("Unable to relocate database to correct app folder");
					}
					host = newLocation;
					connectionDetails.put(AbstractSqlQueryUtil.HOSTNAME, host);
				}
			}
		}
		
		String driver = (String) connectionDetails.get(AbstractSqlQueryUtil.DRIVER_NAME);
		RdbmsTypeEnum driverEnum = RdbmsTypeEnum.getEnumFromString(driver);
		AbstractSqlQueryUtil queryUtil = SqlQueryUtilFactory.initialize(driverEnum);
		
		String connectionUrl = null;
		try {
			connectionUrl = queryUtil.buildConnectionString(connectionDetails);
		} catch (RuntimeException e) {
			throw new SemossPixelException(new NounMetadata("Unable to generation connection url with message " + e.getMessage(), PixelDataType.CONST_STRING, PixelOperationType.ERROR));
		}
		
		int stepCounter = 1;
		logger.info(stepCounter + ". Create metadata for database...");
		File owlFile = UploadUtilities.generateOwlFile(this.appId, this.appName);
		logger.info(stepCounter + ". Complete");
		stepCounter++;

		// the logical metamodel for the upload
		Map<String, Object> newMetamodel = UploadInputUtility.getMetamodel(this.store);
		if (newMetamodel == null) {
			throw new IllegalArgumentException("Must define the metamodel portions we are uploading");
		}
		Map<String, List<String>> nodesAndProps = (Map<String, List<String>>) newMetamodel.get(ExternalJdbcSchemaReactor.TABLES_KEY);
		List<Map<String, Object>> relationships = (List<Map<String, Object>>) newMetamodel.get(ExternalJdbcSchemaReactor.RELATIONS_KEY);
		logger.info(stepCounter + ". Create properties file for database...");
		// Create default RDBMS engine or Impala
		String engineClassName = RDBMSNativeEngine.class.getName();
		this.engine = new RDBMSNativeEngine();
		if (driverEnum == RdbmsTypeEnum.IMPALA) {
			engineClassName = ImpalaEngine.class.getName();
			engine = new ImpalaEngine();
		}
		
		Map<String, Object> jdbcPropertiesMap = validateJDBCProperties(connectionDetails);	

		this.tempSmss = UploadUtilities.createTemporaryExternalRdbmsSmss(this.appId, this.appName, owlFile,
				engineClassName, driverEnum, connectionUrl, connectionDetails, jdbcPropertiesMap);
		DIHelper.getInstance().getCoreProp().setProperty(this.appId + "_" + Constants.STORE, this.tempSmss.getAbsolutePath());
		logger.info(stepCounter + ". Complete");
		stepCounter++;

		logger.info(stepCounter + ". Create database store...");
		engine.setEngineId(this.appId);
		engine.setEngineName(this.appName);
		Properties prop = Utility.loadProperties(tempSmss.getAbsolutePath());
		prop.put("TEMP", "TRUE");
		((AbstractEngine) engine).setProp(prop);
		engine.openDB(null);
		if (!engine.isConnected()) {
			throw new IllegalArgumentException("Unable to connect to external database");
		}
		logger.info(stepCounter + ". Complete");
		stepCounter++;

		logger.info(stepCounter + ". Start generating engine metadata...");
		Owler owler = new Owler(owlFile.getAbsolutePath(), engine.getEngineType());
		// get the existing datatypes
		// table names -> column name, column type
		Set<String> cleanTables = new HashSet<String>();
		for (String t : nodesAndProps.keySet()) {
			cleanTables.add(t.split("\\.")[0]);
		}
		Map<String, Map<String, String>> existingRDBMSStructure = RDBMSEngineCreationHelper.getExistingRDBMSStructure(engine, cleanTables);
		// parse the nodes and get the prime keys and write to OWL
		Map<String, String> nodesAndPrimKeys = parseNodesAndProps(owler, nodesAndProps, existingRDBMSStructure);
		// parse the relationships and write to OWL
		parseRelationships(owler, relationships, existingRDBMSStructure, nodesAndPrimKeys);
		// commit and save the owl
		owler.commit();
		owler.export();
		engine.setOWL(owler.getOwlPath());
		logger.info(stepCounter + ". Complete");
		stepCounter++;

		logger.info(stepCounter + ". Start generating default app insights");
		RDBMSNativeEngine insightDatabase = UploadUtilities.generateInsightsDatabase(this.appId, this.appName);
		UploadUtilities.addExploreInstanceInsight(this.appId, this.appName, insightDatabase);
		UploadUtilities.addInsightUsageStats(this.appId, this.appName, insightDatabase);
		UploadUtilities.addGridDeltaInsight(this.appId, this.appName, insightDatabase);
		engine.setInsightDatabase(insightDatabase);
		// generate base insights
		RDBMSEngineCreationHelper.insertAllTablesAsInsights(engine, owler);
		logger.info(stepCounter + ". Complete");
		stepCounter++;

		logger.info(stepCounter + ". Process app metadata to allow for traversing across apps	");
		UploadUtilities.updateMetadata(this.appId);
		logger.info(stepCounter + ". Complete");
		stepCounter++;
	}
	
	/**
	 * Update the existing app 
	 * @throws Exception
	 */
	private void updateExistingApp() throws Exception {
		this.logger.info("Bringing in metamodel");
		Owler owler = new Owler(this.engine);
		Map<String, Map<String, SemossDataType>> existingMetamodel = UploadUtilities.getExistingMetamodel(owler);
		Map<String, Object> newMetamodel = UploadInputUtility.getMetamodel(this.store);
		if (newMetamodel == null) {
			throw new IllegalArgumentException("Must define the metamodel portions to change");
		}
		
		Map<String, List<String>> nodesAndProps = (Map<String, List<String>>) newMetamodel.get(ExternalJdbcSchemaReactor.TABLES_KEY);
		List<Map<String, Object>> relationships = (List<Map<String, Object>>) newMetamodel.get(ExternalJdbcSchemaReactor.RELATIONS_KEY);

		// separate table names from primary keys
		Set<String> cleanTables = new HashSet<String>();
		Map<String, String> nodesAndPrimKeys = new HashMap<String, String>();
		for (String t : nodesAndProps.keySet()) {
			cleanTables.add(t.split("\\.")[0]);
			nodesAndPrimKeys.put(t.split("\\.")[0], t.split("\\.")[1]);
		}

		Map<String, Map<String, String>> newRDBMSStructure = RDBMSEngineCreationHelper.getExistingRDBMSStructure(this.engine, cleanTables);
		boolean metamodelsEqual = existingMetamodel.equals(newRDBMSStructure);

		// clean up/remove spaces and dashes in new metamodel
		newRDBMSStructure.forEach((tName, columnNames) -> {
			Map<String, String> cleanedColumns = new HashMap<>();
			columnNames.forEach((newColumnName, newDataType) -> {
				String cleanedName = RDBMSEngineCreationHelper.cleanTableName(newColumnName);
				cleanedColumns.put(cleanedName, newDataType);
			});
			newRDBMSStructure.replace(tName, cleanedColumns);
		});

		if (!metamodelsEqual) {
			this.logger.info("Checking differences in metamodel to add");
			// loop through new tables and column names and add them in to existing metamodel
			newRDBMSStructure.forEach((newTableName, columnsFromNew) -> {
				if (!existingMetamodel.containsKey(newTableName)) {
					owler.addConcept(newTableName, null, null);
					try {
						owler.export();
					} catch (IOException e) {
						NounMetadata noun = new NounMetadata(
								"An error occured attempting to remove the desired concept",
								PixelDataType.CONST_STRING, PixelOperationType.ERROR);
						SemossPixelException err = new SemossPixelException(noun);
						err.setContinueThreadOfExecution(false);
						throw err;
					}
				}

				this.logger.info("Adding columns to OWL");
				columnsFromNew.forEach((newColumnName, newDataType) -> {
					owler.addProp(newTableName, newColumnName, newDataType, null, null);
				});

				this.logger.info("Parsing relationships and writing to OWL");
				parseRelationships(owler, relationships, newRDBMSStructure, nodesAndPrimKeys);
			});

			this.logger.info("Checking differences in metamodel to remove");
			Map<String, String> removedProperties = new HashMap<>();
			RDFFileSesameEngine owlEngine = this.engine.getBaseDataEngine();

			// loop through old tables and column names and remove them from existing metamodel
			existingMetamodel.forEach((existingTableName, columnsFromOld) -> {
				boolean tableRemoved = false;
				if (!newRDBMSStructure.containsKey(existingTableName)) {
					owler.removeConcept(this.appId, existingTableName, null);
					tableRemoved = true;
				}
				
				if (!tableRemoved) {
					Map<String, String> newColumnNames = newRDBMSStructure.get(existingTableName);
					columnsFromOld.forEach((existingColumnName, existingDataType) -> {
						if (!newColumnNames.containsKey(existingColumnName)) {
							// track removed properties
							removedProperties.put(existingTableName, existingColumnName);
							this.logger.info("removing relationships from owl");
							removeRelationships(removedProperties, owlEngine);
							this.logger.info("removing properties from owl");
							owler.removeProp(existingTableName, existingColumnName, existingDataType + "", null, null);
						}
					});
				}
			});

			this.logger.info("committing and saving OWL");
			owler.commit();
			this.logger.info("writing changes to OWL");
			owler.export();
			this.logger.info("deleting OWL position map");
			File owlF = this.engine.getOwlPositionFile();
			if(owlF.exists()) {
				owlF.delete();
			}
			
			// also clear caching that is stored for the app
			EngineSyncUtility.clearEngineCache(appId);
		}
	}

	private void removeRelationships(Map<String, String> removedProperties, RDFFileSesameEngine owlEngine) {
		List<String[]> fkRelationships = getPhysicalRelationships(owlEngine);

		for (String[] relations: fkRelationships) {
			String instanceName = Utility.getInstanceName(relations[2]);
			String[] tablesAndPrimaryKeys = instanceName.split("\\.");

			for (int i=0; i < tablesAndPrimaryKeys.length; i+=2) {
				String key = tablesAndPrimaryKeys[i], value = tablesAndPrimaryKeys[i+1], removedValue = removedProperties.get(key);

				if (removedValue != null && removedValue.equalsIgnoreCase(value)) {
					owlEngine.doAction(ACTION_TYPE.REMOVE_STATEMENT, new Object[] { relations[0], relations[2], relations[1], true });
					owlEngine.doAction(ACTION_TYPE.REMOVE_STATEMENT, new Object[] { relations[2], RDFS.SUBPROPERTYOF.toString(), "http://semoss.org/ontologies/Relation", true });
				}
			}
		}
	}

	private List<String[]> getPhysicalRelationships(IEngine engine) {
		String query = "SELECT DISTINCT ?start ?end ?rel WHERE { "
				+ "{?start <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://semoss.org/ontologies/Concept> }"
				+ "{?end <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://semoss.org/ontologies/Concept> }"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <http://semoss.org/ontologies/Relation>} " + "{?start ?rel ?end}"
				+ "Filter(?rel != <" + RDFS.SUBPROPERTYOF + ">)"
				+ "Filter(?rel != <http://semoss.org/ontologies/Relation>)" + "}";
		return Utility.getVectorArrayOfReturn(query, engine, true);
	}
	
	/**
	 * Delete all the corresponding files that are generated from the upload the
	 * failed
	 */
	private void cleanUpCreateNewError() {
		// TODO:clean up DIHelper!
		try {
			// close the DB so we can delete it
			if (this.engine != null) {
				engine.closeDB();
			}

			// delete the .temp file
			if (this.tempSmss != null && this.tempSmss.exists()) {
				FileUtils.forceDelete(this.tempSmss);
			}
			// delete the .smss file
			if (this.smssFile != null && this.smssFile.exists()) {
				FileUtils.forceDelete(this.smssFile);
			}
			// delete the engine folder and all its contents
			if (this.appFolder != null && this.appFolder.exists()) {
				File[] files = this.appFolder.listFiles();
				if (files != null) { // some JVMs return null for empty dirs
					for (File f : files) {
						FileUtils.forceDelete(f);
					}
				}
				FileUtils.forceDelete(this.appFolder);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Add the concepts and properties into the OWL
	 * 
	 * @param owler
	 * @param nodesAndProps
	 * @param dataTypes
	 * @return
	 */
	private Map<String, String> parseNodesAndProps(Owler owler, Map<String, List<String>> nodesAndProps, Map<String, Map<String, String>> dataTypes) {
		Map<String, String> nodesAndPrimKeys = new HashMap<String, String>(nodesAndProps.size());
		for (String node : nodesAndProps.keySet()) {
			String[] tableAndPrimaryKey = node.split("\\.");
			String nodeName = tableAndPrimaryKey[0];
			String primaryKey = tableAndPrimaryKey[1];
			nodesAndPrimKeys.put(nodeName, primaryKey);
			String cleanConceptTableName = RDBMSEngineCreationHelper.cleanTableName(nodeName);
			// add concepts
			owler.addConcept(cleanConceptTableName, null, null);
			owler.addProp(cleanConceptTableName, primaryKey, dataTypes.get(nodeName).get(primaryKey));
			// add concept properties
			for (String prop : nodesAndProps.get(node)) {
				if (!prop.equals(primaryKey)) {
					String cleanProp = RDBMSEngineCreationHelper.cleanTableName(prop);
					owler.addProp(cleanConceptTableName, cleanProp, dataTypes.get(nodeName).get(prop));
				}
			}
		}
		return nodesAndPrimKeys;
	}

	/**
	 * Add the relationships into the OWL
	 * 
	 * @param owler
	 * @param relationships
	 * @param dataTypes
	 * @param nodesAndPrimKeys
	 */
	private void parseRelationships(Owler owler, List<Map<String, Object>> relationships,
			Map<String, Map<String, String>> dataTypes, Map<String, String> nodesAndPrimKeys) {
		for (Map relation : relationships) {
			String subject = RDBMSEngineCreationHelper.cleanTableName(relation.get(Constants.FROM_TABLE).toString());
			String object = RDBMSEngineCreationHelper.cleanTableName(relation.get(Constants.TO_TABLE).toString());
			// TODO: check if this needs to be cleaned
			String[] joinColumns = relation.get(Constants.REL_NAME).toString().split("\\.");
			// predicate is: "fromTable.fromJoinCol.toTable.toJoinCol"
			String predicate = subject + "." + joinColumns[0] + "." + object + "." + joinColumns[1];
			owler.addRelation(subject, object, predicate);
		}
	}

	private Map<String, Object> getConDetails() {
		GenRowStruct grs = this.store.getNoun(ReactorKeysEnum.CONNECTION_DETAILS.getKey());
		if(grs != null && !grs.isEmpty()) {
			List<Object> mapInput = grs.getValuesOfType(PixelDataType.MAP);
			if(mapInput != null && !mapInput.isEmpty()) {
				return (Map<String, Object>) mapInput.get(0);
			}
		}
		
		List<Object> mapInput = grs.getValuesOfType(PixelDataType.MAP);
		if(mapInput != null && !mapInput.isEmpty()) {
			return (Map<String, Object>) mapInput.get(0);
		}
		
		return null;
	}

	/**
	 * Validates JDBC properties and returns a LinkedHash of the properties while removing said
	 * properties from connection details. 
	 * 
	 * @param connectionDetails
	 * @return jdbcProperties
	 */
	private Map<String, Object> validateJDBCProperties(Map<String, Object> connectionDetails) {
		// keep an ordered map for the jdbc properties
		Map<String, Object> jdbcProperties = new LinkedHashMap<String, Object>();
		int minPool = -1;
		int maxPool = -1;
		for (String key : JDBC_CONSTANTS) {
			if(connectionDetails.containsKey(key)) {
				Object jdbcVal = connectionDetails.remove(key);
				// ignore empty string inputs
				if(jdbcVal.toString().isEmpty()) {
					continue;
				}
				jdbcProperties.put(key, jdbcVal);
				if (key.equals(Constants.USE_CONNECTION_POOLING)) {
					// boolean check
					String strBool = jdbcVal.toString();
					if(!(strBool.equalsIgnoreCase("false") || strBool.equalsIgnoreCase("true"))) {
						throw new IllegalArgumentException("Parameter " + key + " is not a valid boolean value");
					}
				} else {
					// currently all other parameter inputs are integer values
					// make sure it is a valid integer or turn it to an integer
					int integerInput = -1;
					if(jdbcVal instanceof Number) {
						integerInput = ((Number) jdbcVal).intValue();
					} else {
						try {
							integerInput = Integer.parseInt(jdbcVal + "");
						} catch(NumberFormatException e) {
							throw new IllegalArgumentException("Parameter " + key + " is not a valid number");
						}
					}
					
					// perform the integer check
					if(integerInput < 0) {
						throw new IllegalArgumentException("Paramter " + key + " must be a numeric value greater than 0");
					}
					
					// assign so we can do a final check for min/max pool size
					if(key.equals(Constants.POOL_MIN_SIZE)) {
						minPool = integerInput;
					} else if(key.equals(Constants.POOL_MAX_SIZE)) {
						maxPool = integerInput;
					}
				}
			}
		}
		// after check pool min/max size
		if(minPool > 0 && maxPool >0) {
			if (minPool > maxPool) {
				throw new IllegalArgumentException("Max pool size must be greater than min pool size");
			}
		}
		
		return jdbcProperties;
	}
}
	

