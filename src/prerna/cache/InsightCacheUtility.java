package prerna.cache;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronExpression;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import prerna.auth.utils.SecurityInsightUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.engine.impl.InsightAdministrator;
import prerna.engine.impl.SmssUtilities;
import prerna.io.connector.secrets.SecretsUtility;
import prerna.om.Insight;
import prerna.project.api.IProject;
import prerna.sablecc2.reactor.cluster.VersionReactor;
import prerna.util.AssetUtility;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.MosfetSyncHelper;
import prerna.util.Utility;
import prerna.util.gson.InsightAdapter;

public class InsightCacheUtility {

	private static final Logger logger = LogManager.getLogger(InsightCacheUtility.class);

	private static final String DIR_SEPARATOR = java.nio.file.FileSystems.getDefault().getSeparator();

	private static byte[] buffer = new byte[2048];

	public static final String INSIGHT_ZIP = "InsightZip.zip";
	public static final String MAIN_INSIGHT_JSON = "InsightCache.json";
	public static final String VIEW_JSON = "ViewData.json";
	
	public static final String VERSION_FILE = ".version";
	public static final String VERSION_HEADER = 
			"# This file is automatically generated by SEMOSS.\r\n" + 
			"# It is not intended for manual editing.\r\n";
	public static final String DATETIME_KEY = VersionReactor.DATETIME_KEY;
	public static final String VERSION_KEY = VersionReactor.VERSION_KEY;
	
	public static final String CACHE_FOLDER = ".cache";
	
	private InsightCacheUtility() {
		
	}
	
	public static String getInsightCacheFolderPath(Insight insight, Map<String, Object> parameters) {
		String rdbmsId = insight.getRdbmsId();
		String projectId = insight.getProjectId();
		String projectName = insight.getProjectName();
		return getInsightCacheFolderPath(projectId, projectName, rdbmsId, parameters);
	}
	
	public static String getInsightCacheFolderPath(String projectId, String projectName, String rdbmsId, Map<String, Object> parameters) {
		String folderDir = AssetUtility.getProjectVersionFolder(projectName, projectId) 
				+ DIR_SEPARATOR +  rdbmsId + DIR_SEPARATOR + CACHE_FOLDER;
		if(parameters != null && !parameters.isEmpty()) {
			MessageDigest messageDigest;
			try {
				messageDigest = MessageDigest.getInstance("SHA-256");
				TreeMap<String, Object> orderedParams = new TreeMap<>(parameters);
				for(String key : orderedParams.keySet()) {
					if(orderedParams.get(key) instanceof List) {
						Collections.sort((List) orderedParams.get(key));
					}
				}
				byte[] hash = messageDigest.digest(orderedParams.toString().getBytes());
				// convert bytes to hexadecimal
		        StringBuilder s = new StringBuilder();
		        for (byte b : hash) {
		            s.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
		        }
				
				folderDir = folderDir + DIR_SEPARATOR + s.toString();
			} catch (NoSuchAlgorithmException e) {
				logger.error(Constants.STACKTRACE, e);
			}
		}
		return folderDir;
	}
	
	/**
	 * Main method to cache a full insight
	 * @param insight
	 * @throws IOException 
	 */
	public static File cacheInsight(Insight insight, Set<String> varsToExclude, Map<String, Object> parameters) throws IOException {
		String rdbmsId = insight.getRdbmsId();
		String projectId = insight.getProjectId();
		String projectName = insight.getProjectName();

		if(projectId == null || rdbmsId == null || projectName == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}

		boolean encrypt = insight.isCacheEncrypt();
		Cipher cipher = null;
		if(encrypt) {
			cipher = SecretsUtility.generateCipherForInsight(rdbmsId, projectName, projectId);
		}

		String folderDir = getInsightCacheFolderPath(insight, parameters);
		String normalizedFolderDir = Utility.normalizePath(folderDir);
		if(!(new File(normalizedFolderDir).exists())) {
			new File(normalizedFolderDir).mkdirs();
		}
		File zipFile = new File(normalizedFolderDir + DIR_SEPARATOR + INSIGHT_ZIP);

		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		try {
			fos = new FileOutputStream(zipFile.getAbsolutePath());
			zos = new ZipOutputStream(fos);
			
			String insightLoc = normalizedFolderDir + DIR_SEPARATOR + MAIN_INSIGHT_JSON;
			File insightFile = new File(insightLoc);
			
			InsightAdapter iAdapter = new InsightAdapter(normalizedFolderDir, zos);
			iAdapter.setVarsToExclude(varsToExclude);
			iAdapter.setEncrypt(encrypt);
			iAdapter.setCipher(cipher);
			StringWriter writer = new StringWriter();
			JsonWriter jWriter = new JsonWriter(writer);
			iAdapter.write(jWriter, insight);
			
			try {
				if(encrypt) {
					FileUtils.writeByteArrayToFile(insightFile, cipher.doFinal(writer.toString().getBytes()));
				} else {
					FileUtils.writeStringToFile(insightFile, writer.toString());
				}
			} catch (IOException e) {
				logger.error(Constants.STACKTRACE, e);
			}
			addToZipFile(insightFile, zos);

			// also write a .version file so store when this cache was created
			String versionFileLoc = normalizedFolderDir + DIR_SEPARATOR + VERSION_FILE;
			File versionFile = writeInsightCacheVersion(versionFileLoc);
			addToZipFile(versionFile, zos);

			// update the metadata
			// TODO: how do we store this at the parameter level
			// TODO: how do we store this at the parameter level
			// TODO: how do we store this at the parameter level
			// TODO: how do we store this at the parameter level
			// TODO: how do we store this at the parameter level
			IProject project = Utility.getProject(projectId);
			LocalDateTime cachedOn = LocalDateTime.now();
			InsightAdministrator admin = new InsightAdministrator(project.getInsightDatabase());
			admin.updateInsightCachedOn(rdbmsId, cachedOn);
			SecurityInsightUtils.updateInsightCachedOn(projectId, rdbmsId, cachedOn);
			
			String mosfetPath = MosfetSyncHelper.getMosfetFileLocation(projectId, projectName, rdbmsId);
			File mosfet = new File(Utility.normalizePath(mosfetPath));
			if(mosfet.exists() && mosfet.isFile()) {
				MosfetSyncHelper.updateMosfitFileCachedOn(mosfet, cachedOn);
			}
		} catch(Exception e) {
			logger.error(Constants.STACKTRACE, e);
		} finally {
			closeStream(zos);
			closeStream(fos);
		}
		
		return zipFile;
	}
	
	/**
	 * 
	 * @param versionFileLoc
	 * @return
	 */
	public static File writeInsightCacheVersion(String versionFileLoc) {
		StringBuilder version = new StringBuilder(VERSION_HEADER);
		version.append(DATETIME_KEY).append("=").append(LocalDateTime.now()).append("\r\n");
		try {
			Map<String, String> versionMap = VersionReactor.getVersionMap(false);
			version.append(VERSION_KEY).append("=").append(versionMap.get(VERSION_KEY)).append("\r\n");
		} catch(Exception e) {
			logger.error(Constants.STACKTRACE, e);
		}
		
		File versionFile = new File(Utility.normalizePath(versionFileLoc));
		try {
			FileUtils.writeStringToFile(versionFile, version.toString());
		} catch (IOException e) {
			logger.error(Constants.STACKTRACE, e);
		}
		return versionFile;
	}
	
	/**
	 * Used to add a file to the insight zip
	 * @param file
	 * @param zos
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void addToZipFile(File file, ZipOutputStream zos) throws FileNotFoundException, IOException {
		ZipEntry zipEntry = new ZipEntry(file.getName());
		zos.putNextEntry(zipEntry);

		FileInputStream fis = null;
		try {
			int length;
			fis = new FileInputStream(file);
			while ((length = fis.read(buffer)) >= 0) {
				zos.write(buffer, 0, length);
			}
		} finally {
			closeStream(fis);
		}
	}
	
	/**
	 * Main method to read in a full insight
	 * @param insightDir
	 * @return
 	 * @throws IOException 
	 */
	public static Insight readInsightCache(Insight existingInsight, Map<String, Object> paramValues) throws IOException, RuntimeException {
		String insightZipLoc = InsightCacheUtility.getInsightCacheFolderPath(existingInsight, paramValues) + DIR_SEPARATOR + InsightCacheUtility.INSIGHT_ZIP;
		File insightZip = new File(Utility.normalizePath(insightZipLoc));
		if(!insightZip.exists()) {
			// just return null
			return null;
		}
		
		String versionFileLoc = InsightCacheUtility.getInsightCacheFolderPath(existingInsight, paramValues) + DIR_SEPARATOR + InsightCacheUtility.VERSION_FILE;
		File versionFile = new File(Utility.normalizePath(versionFileLoc));
		if(!versionFile.exists() || !versionFile.isFile()) {
			// delete the current cache in case it is not accurate
			InsightCacheUtility.deleteCache(existingInsight.getProjectId(), existingInsight.getProjectName(), 
					existingInsight.getRdbmsId(), paramValues, true);
			return null;
		}
		Properties vProp = Utility.loadProperties(versionFileLoc);
		String versionStr = vProp.getProperty(InsightCacheUtility.VERSION_KEY);
		String dateGenStr = vProp.getProperty(InsightCacheUtility.DATETIME_KEY);
		if(versionStr == null || (versionStr=versionStr.trim()).isEmpty()
			|| versionStr == null || (versionStr=versionStr.trim()).isEmpty()) {
			// delete the current cache in case it is not accurate
			InsightCacheUtility.deleteCache(existingInsight.getProjectId(), existingInsight.getProjectName(), 
					existingInsight.getRdbmsId(), paramValues, true);
			return null;
		}
		// check the version is accurate / the same
		if(!versionStr.equals(VersionReactor.getVersionMap(false).get(VersionReactor.VERSION_KEY))) {
			// different semoss version, delete the cache
			InsightCacheUtility.deleteCache(existingInsight.getProjectId(), existingInsight.getProjectName(), 
					existingInsight.getRdbmsId(), paramValues, true);
			return null;
		}
		
		LocalDateTime cachedDateTime = null;
		try {
			cachedDateTime = LocalDateTime.parse(dateGenStr);
		} catch(Exception e) {
			// someone has been manually touching the file and they should
			// write the version file again with todays date
			versionFile.delete();
			InsightCacheUtility.writeInsightCacheVersion(versionFileLoc);
			vProp = Utility.loadProperties(versionFileLoc);
			dateGenStr = vProp.getProperty(InsightCacheUtility.DATETIME_KEY);
			cachedDateTime = LocalDateTime.parse(dateGenStr);
		}
		
		// check cache doesn't have a time expiration
		int cacheMinutes = existingInsight.getCacheMinutes();
		if(cacheMinutes > 0) {
			if(cachedDateTime.plusMinutes(cacheMinutes).isBefore(LocalDateTime.now())) {
				InsightCacheUtility.deleteCache(existingInsight.getProjectId(), existingInsight.getProjectName(), 
						existingInsight.getRdbmsId(), paramValues, true);
				return null;
			}
		}
		
		// check cache doesn't have a set expiration
		String cacheCron = existingInsight.getCacheCron();
		if(cacheCron != null && !cacheCron.isEmpty()) {
			CronExpression expression;
			try {
				expression = new CronExpression(cacheCron);
				TimeZone tz = TimeZone.getTimeZone(Utility.getApplicationTimeZoneId());
				Date cachedDateObj = Date.from(cachedDateTime.atZone(tz.toZoneId()).toInstant());
				Date nextValidTimeAfter = expression.getNextValidTimeAfter(cachedDateObj);
				if(nextValidTimeAfter.before(cachedDateObj)) {
					InsightCacheUtility.deleteCache(existingInsight.getProjectId(), existingInsight.getProjectName(), 
							existingInsight.getRdbmsId(), paramValues, true);
					return null;
				}
			} catch (ParseException e) {
				// invalid cron... not sure if we should ever get to this point
				logger.error(Constants.STACKTRACE, e);
				return null;
			}
		}
		
		boolean encrypt = existingInsight.isCacheEncrypt();
		Cipher cipher = null;
		if(encrypt) {
			cipher = SecretsUtility.retrieveCipherForInsight(existingInsight);
		}
		
		ZipFile zip = null;
		ZipEntry entry = null;
		try {
			zip = new ZipFile(insightZip);
			entry = zip.getEntry(MAIN_INSIGHT_JSON);
			if(entry == null) {
				throw new IOException("Invalid zip format for cached insight");
			}
	        StringBuilder sb = new StringBuilder();
	        
	        if(cipher != null) {
		        try (BufferedReader br = new BufferedReader(new InputStreamReader(new CipherInputStream(zip.getInputStream(entry), cipher)))){
		        	String line;
			        while ((line = br.readLine()) != null) {
			            sb.append(line);
			        }
		        }
	        } else {
	        	try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)))) {
	        		String line;
			        while ((line = br.readLine()) != null) {
			            sb.append(line);
			        }
	        	}
	        }
	        
	        InsightAdapter iAdapter = new InsightAdapter(zip);
	        iAdapter.setUserContext(existingInsight);
	        iAdapter.setCipher(cipher);
	        StringReader reader = new StringReader(sb.toString());
	        JsonReader jReader = new JsonReader(reader);
			Insight insight = iAdapter.read(jReader);
			insight.setCachedDateTime(cachedDateTime);
			insight.setCacheEncrypt(encrypt);
			return insight;
		} catch(Exception e) {
			logger.error(Constants.STACKTRACE, e);
			throw e;
		} finally {
			closeStream(zip);
		}
	}
	
	/**
	 * Main method to read in a full insight
	 * @param insightPath
	 * @return
	 * @throws IOException 
	 */
	public static Insight readInsightCache(Insight existingInsight) throws IOException, JsonSyntaxException {
		return readInsightCache(existingInsight, null);
	}
	
	/**
	 * Get the view data for a cached insight
	 * @return
	 * @throws IOException 
	 */
	public static Map<String, Object> getCachedInsightViewData(Insight insight, Map<String, Object> parameters) throws IOException, JsonSyntaxException {
		String rdbmsId = insight.getRdbmsId();
		String projectId = insight.getProjectId();
		String projectName = insight.getProjectName();
		
		if(projectId == null || rdbmsId == null || projectName == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}
		
		boolean encrypt = insight.isCacheEncrypt();
		Cipher cipher = null;
		if(encrypt) {
			cipher = SecretsUtility.retrieveCipherForInsight(insight);
		}
		
		String zipFileLoc = Utility.normalizePath(getInsightCacheFolderPath(insight, parameters) + DIR_SEPARATOR + INSIGHT_ZIP);
		File zipFile = new File(zipFileLoc);
		
		if(!zipFile.exists()) {
			throw new IOException("Cannot find insight cache");
		}
		
		ZipFile zip = null;
		ZipEntry entry = null;
		try {
			zip = new ZipFile(zipFileLoc);
			entry = zip.getEntry(VIEW_JSON);
			if(entry == null) {
				throw new IOException("Invalid zip format for cached insight");
			}
	        StringBuilder sb = new StringBuilder();
	        
	        if(cipher != null) {
		        try (BufferedReader br = new BufferedReader(new InputStreamReader(new CipherInputStream(zip.getInputStream(entry), cipher)))){
		        	String line;
			        while ((line = br.readLine()) != null) {
			            sb.append(line);
			        }
		        }
	        } else {
	        	try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)))) {
	        		String line;
			        while ((line = br.readLine()) != null) {
			            sb.append(line);
			        }
	        	}
	        }
	        Gson gson = new Gson();
			return gson.fromJson(sb.toString(), Map.class);
		} catch(Exception e) {
			logger.error(Constants.STACKTRACE, e);
			throw e;
		} finally {
			closeStream(zip);
		}
	}
	
	/**
	 * Delete cached files for an insight
	 * @param projectId
	 * @param projectName
	 * @param rdbmsId
	 */
	public static void deleteCache(String projectId, String projectName, String rdbmsId, Map<String, Object> parameters, boolean pullCloud) {
		// this is false on save insight
		// because i do not want to pull when i save
		// but i do want to delete the cache in case i am saving 
		// from an existing insight as the .cache folder gets moved

		String folderDir = Utility.normalizePath(getInsightCacheFolderPath(projectId, projectName, rdbmsId, parameters));
		Path projectFolder = Paths.get(DIHelper.getInstance().getProperty(Constants.BASE_FOLDER) + DIR_SEPARATOR 
				+ Constants.PROJECT_FOLDER + DIR_SEPARATOR + SmssUtilities.getUniqueName(projectName, projectId));
		Path relative = projectFolder.relativize( Paths.get(folderDir));
		if(pullCloud) {
			ClusterUtil.reactorPullProjectFolder(projectId, folderDir, relative.toString());
		}

		File folder = new File(Utility.normalizePath(folderDir)); 
		if(!folder.exists()) {
			return;
		}
		
		File[] cacheFiles = folder.listFiles();
		for(File f : cacheFiles) {
			if(f.isDirectory()) {
				ICache.deleteFolder(f);
			} else {
				ICache.deleteFile(f);
			}
		}
		
		// update the metadata
		try {
			IProject project = Utility.getProject(projectId);
			LocalDateTime cachedOn = null;
			InsightAdministrator admin = new InsightAdministrator(project.getInsightDatabase());
			admin.updateInsightCachedOn(rdbmsId, cachedOn);
			SecurityInsightUtils.updateInsightCachedOn(projectId, rdbmsId, cachedOn);
			
			String mosfetPath = MosfetSyncHelper.getMosfetFileLocation(projectId, projectName, rdbmsId);
			File mosfet = new File(Utility.normalizePath(mosfetPath));
			if(mosfet.exists() && mosfet.isFile()) {
				MosfetSyncHelper.updateMosfitFileCachedOn(mosfet, cachedOn);
			}
		} catch (IOException e) {
			logger.error(Constants.STACKTRACE, e);
		}
		
		if(pullCloud) {
			ClusterUtil.reactorPushProjectFolder(projectId, folderDir, relative.toString());
		}
	}
	
	public static void unzipFile(ZipFile zip, String name, String path) throws FileNotFoundException {
		byte[] buffer = new byte[1024];
		File newFile = new File(Utility.normalizePath(path));
		FileOutputStream fos = null;
		ZipEntry zipE = new ZipEntry(name);
		InputStream zis = null;
		try {
			zis = zip.getInputStream(zipE);
			fos = new FileOutputStream(newFile);
			int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
		} catch(Exception e) {
			logger.error(Constants.STACKTRACE, e);
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error(Constants.STACKTRACE, e);
				}
			}
			if(zis != null) {
				try {
					zis.close();
				} catch (IOException e) {
					logger.error(Constants.STACKTRACE, e);
				}
			}
		}
	}
	
	/**
	 * Close a stream
	 * @param is
	 */
	private static void closeStream(Closeable is) {
		if(is != null) {
			try {
				is.close();
			} catch (IOException e) {
				logger.error(Constants.STACKTRACE, e);
			}
		}
	}
	
}
