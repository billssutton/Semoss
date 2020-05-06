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
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import prerna.cluster.util.ClusterUtil;
import prerna.engine.impl.SmssUtilities;
import prerna.om.Insight;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;
import prerna.util.gson.InsightAdapter;

public class InsightCacheUtility {

	private static final Logger logger = LogManager.getLogger(InsightCacheUtility.class);

	private static final String STACKTRACE = "StackTrace: ";
	private static final String DIR_SEPARATOR = java.nio.file.FileSystems.getDefault().getSeparator();

	private static byte[] buffer = new byte[2048];

	public static final String INSIGHT_ZIP = "InsightZip.zip";
	public static final String MAIN_INSIGHT_JSON = "InsightCache.json";
	public static final String VIEW_JSON = "ViewData.json";

	public static final String CACHE_FOLDER = ".cache";
	
	private InsightCacheUtility() {
		
	}
	
	public static String getInsightCacheFolderPath(Insight insight) {
		String rdbmsId = insight.getRdbmsId();
		String engineId = insight.getEngineId();
		String engineName = insight.getEngineName();
		return getInsightCacheFolderPath(engineId, engineName, rdbmsId);
	}
	
	public static String getInsightCacheFolderPath(String engineId, String engineName, String rdbmsId) {
		String baseFolder = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		String folderDir = baseFolder + DIR_SEPARATOR + "db" + DIR_SEPARATOR + SmssUtilities.getUniqueName(engineName, engineId) 
				+ DIR_SEPARATOR + "version" + DIR_SEPARATOR + rdbmsId + DIR_SEPARATOR + CACHE_FOLDER;
		return folderDir;
	}
	
	/**
	 * Main method to cache a full insight
	 * @param insight
	 * @throws IOException 
	 */
	public static File cacheInsight(Insight insight, Set<String> varsToExclude) throws IOException {
		String rdbmsId = insight.getRdbmsId();
		String engineId = insight.getEngineId();
		String engineName = insight.getEngineName();
		
		if(engineId == null || rdbmsId == null || engineName == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}
		
		String folderDir = getInsightCacheFolderPath(insight);
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
			
			InsightAdapter iAdapter = new InsightAdapter(zos);
			iAdapter.setVarsToExclude(varsToExclude);
			StringWriter writer = new StringWriter();
			JsonWriter jWriter = new JsonWriter(writer);
			iAdapter.write(jWriter, insight);
			
			String insightLoc = normalizedFolderDir + DIR_SEPARATOR + MAIN_INSIGHT_JSON;
			File insightFile = new File(insightLoc);
			try {
				FileUtils.writeStringToFile(insightFile, writer.toString());
			} catch (IOException e) {
				logger.error(STACKTRACE, e);
			}
			addToZipFile(insightFile, zos);
		} catch(Exception e) {
			logger.error(STACKTRACE, e);
		} finally {
			closeStream(zos);
			closeStream(fos);
		}
		
		return zipFile;
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
	public static Insight readInsightCache(File insightCacheZip, Insight existingInsight) throws IOException, RuntimeException {
		ZipFile zip = null;
		ZipEntry entry = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			zip = new ZipFile(insightCacheZip);
			entry = zip.getEntry(MAIN_INSIGHT_JSON);
			if(entry == null) {
				throw new IOException("Invalid zip format for cached insight");
			}
			is = zip.getInputStream(entry);
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
	        StringBuilder sb = new StringBuilder();
	        String line;
	        while ((line = br.readLine()) != null) {
	            sb.append(line);
	        }
	        
	        InsightAdapter iAdapter = new InsightAdapter(zip);
	        iAdapter.setUserContext(existingInsight);
	        StringReader reader = new StringReader(sb.toString());
	        JsonReader jReader = new JsonReader(reader);
			Insight insight = iAdapter.read(jReader);
			return insight;
		} catch(Exception e) {
			logger.error(STACKTRACE, e);
			throw e;
		} finally {
			closeStream(br);
			closeStream(isr);
			closeStream(is);
			closeStream(zip);
		}
	}
	
	/**
	 * Main method to read in a full insight
	 * @param insightPath
	 * @return
	 * @throws IOException 
	 */
	public static Insight readInsightCache(String insightPath, Insight existingInsight) throws IOException, JsonSyntaxException {
		File insightFile = new File(insightPath);
		return readInsightCache(insightFile, existingInsight);
	}
	
	/**
	 * Get the view data for a cached insight
	 * @return
	 * @throws IOException 
	 */
	public static Map<String, Object> getCachedInsightViewData(Insight insight) throws IOException, JsonSyntaxException {
		String rdbmsId = insight.getRdbmsId();
		String engineId = insight.getEngineId();
		String engineName = insight.getEngineName();
		
		if(engineId == null || rdbmsId == null || engineName == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}
		
		String zipFileLoc = getInsightCacheFolderPath(insight) + DIR_SEPARATOR + INSIGHT_ZIP;
		String normalizedZipFileLoc = Utility.normalizePath(zipFileLoc);
		File zipFile = new File(normalizedZipFileLoc);

		if(!zipFile.exists()) {
			throw new IOException("Cannot find insight cache");
		}
		
		ZipEntry viewData = new ZipEntry(VIEW_JSON);
		ZipFile zip = null;
		InputStream zis = null;
		try {
			zip = new ZipFile(normalizedZipFileLoc);
			zis = zip.getInputStream(viewData);
			
			String jsonString = IOUtils.toString(zis, "UTF-8");
			
			//TODO:
			// this is here to try to delete old invalid caches
			if(!jsonString.contains("CACHED_PANEL")) {
				throw new IllegalArgumentException("Old format of cache. Must delete and re-create.");
			}
			
			Gson gson = new Gson();
			return gson.fromJson(jsonString, Map.class);
		} catch(Exception e) {
			logger.error("Error retrieving cache for " + rdbmsId);
			logger.error(STACKTRACE, e);
		} finally {
			if(zis != null) {
				zis.close();
			}
			if(zip != null) {
				zip.close();
			}
		}
		return null;
	}
	
	/**
	 * Delete cached files for an insight
	 * @param engineId
	 * @param engineName
	 * @param rdbmsId
	 */
	public static void deleteCache(String engineId, String engineName, String rdbmsId) {
		ClusterUtil.reactorPullApp(engineId);
		String folderDir = getInsightCacheFolderPath(engineId, engineName, rdbmsId);
		File folder = new File(folderDir); 
		if(!folder.exists()) {
			return;
		}
		
		File[] cacheFiles = folder.listFiles();
		for(File f : cacheFiles) {
			ICache.deleteFile(f);
		}
		ClusterUtil.reactorPushApp(engineId);
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
			logger.error(STACKTRACE, e);
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					logger.error(STACKTRACE, e);
				}
			}
			if(zis != null) {
				try {
					zis.close();
				} catch (IOException e) {
					logger.error(STACKTRACE, e);
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
				logger.error(STACKTRACE, e);
			}
		}
	}
	
}
