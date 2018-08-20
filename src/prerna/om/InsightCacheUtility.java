package prerna.om;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import prerna.cache.ICache;
import prerna.engine.impl.SmssUtilities;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.gson.GsonUtility;
import prerna.util.gson.InsightAdapter;

public class InsightCacheUtility {

	private static final String DIR_SEPARATOR = java.nio.file.FileSystems.getDefault().getSeparator();
	private static byte[] buffer = new byte[2048];

	public static final String INSIGHT_ZIP = "InsightZip.zip";
	public static final String MAIN_INSIGHT_JSON = "InsightCache.json";
	public static final String VIEW_JSON = "ViewData.json";

	private InsightCacheUtility() {
		
	}
	
	/**
	 * Main method to cache a full insight
	 * @param insight
	 * @throws IOException 
	 */
	public static File cacheInsight(Insight insight) throws IOException {
		String rdbmsId = insight.getRdbmsId();
		String engineId = insight.getEngineId();
		String engineName = insight.getEngineName();
		
		if(engineId == null || rdbmsId == null || engineName == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}
		
		String baseFolder = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		String folderDir = baseFolder + DIR_SEPARATOR + "db" + DIR_SEPARATOR + SmssUtilities.getUniqueName(engineName, engineId) 
				+ DIR_SEPARATOR + "version" + DIR_SEPARATOR + rdbmsId;
		if(!(new File(folderDir).exists())) {
			new File(folderDir).mkdirs();
		}
		File zipFile = new File(folderDir + DIR_SEPARATOR + INSIGHT_ZIP);

		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		try {
			fos = new FileOutputStream(zipFile.getAbsolutePath());
			zos = new ZipOutputStream(fos);
			
			InsightAdapter iAdapter = new InsightAdapter(zos);
			StringWriter writer = new StringWriter();
			JsonWriter jWriter = new JsonWriter(writer);
			iAdapter.write(jWriter, insight);
			
			String insightLoc = folderDir+ DIR_SEPARATOR + MAIN_INSIGHT_JSON;
			File insightFile = new File(insightLoc);
			try {
				FileUtils.writeStringToFile(insightFile, writer.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			addToZipFile(insightFile, zos);
		} catch(Exception e) {
			e.printStackTrace();
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
	 */
	public static Insight readInsightCache(File insightCacheZip) {
		ZipFile zip = null;
		ZipEntry entry = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			zip = new ZipFile(insightCacheZip);
			entry = zip.getEntry(MAIN_INSIGHT_JSON);
			is = zip.getInputStream(entry);
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
	        StringBuilder sb = new StringBuilder();
	        String line;
	        while ((line = br.readLine()) != null) {
	            sb.append(line);
	        }
	        
	        InsightAdapter iAdapter = new InsightAdapter(zip);
	        StringReader reader = new StringReader(sb.toString());
	        JsonReader jReader = new JsonReader(reader);
			Insight insight = iAdapter.read(jReader);
			return insight;
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeStream(br);
			closeStream(isr);
			closeStream(is);
			closeStream(zip);
		}
		
		return null;
	}
	
	/**
	 * Main method to read in a full insight
	 * @param insightPath
	 * @return
	 */
	public static Insight readInsightCache(String insightPath) {
		File insightFile = new File(insightPath);
		return readInsightCache(insightFile);
	}
	
	/**
	 * Get the view data for a cached insight
	 * @return
	 * @throws IOException 
	 */
	public static Map<String, Object> getCachedInsightViewData(Insight insight) throws IOException {
		String rdbmsId = insight.getRdbmsId();
		String engineId = insight.getEngineId();
		String engineName = insight.getEngineName();
		
		if(engineId == null || rdbmsId == null || engineName == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}
		
		String baseFolder = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		String zipFileLoc = baseFolder + DIR_SEPARATOR + "db" + DIR_SEPARATOR + SmssUtilities.getUniqueName(engineName, engineId) 
				+ DIR_SEPARATOR + "version" + DIR_SEPARATOR + rdbmsId + DIR_SEPARATOR + INSIGHT_ZIP;
		File zipFile = new File(zipFileLoc);
		if(!zipFile.exists()) {
			throw new IOException("Cannot find insight cache");
		}
		
		ZipEntry viewData = new ZipEntry(VIEW_JSON);
		ZipFile zip = null;
		InputStream zis = null;
		try {
			zip = new ZipFile(zipFileLoc);
			zis = zip.getInputStream(viewData);
			
			String jsonString = IOUtils.toString(zis); 
			Gson gson = GsonUtility.getDefaultGson();
			return gson.fromJson(jsonString, Map.class);
		} catch(Exception e) {
			e.printStackTrace();
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
		String baseFolder = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		String folderDir = baseFolder + DIR_SEPARATOR + "db" + DIR_SEPARATOR + SmssUtilities.getUniqueName(engineName, engineId) 
				+ DIR_SEPARATOR + "version" + DIR_SEPARATOR + rdbmsId;
		File folder = new File(folderDir); 
		if(!folder.exists()) {
			return;
		}
		
		List<String> extentions = new Vector<String>();
		extentions.add("*.gz");
		extentions.add("*.json");
		extentions.add("*.JSON");
		extentions.add("*.zip");
		extentions.add("*.owl");
		extentions.add("*.tg");
		extentions.add("*.rda");

		FileFilter fileFilter = new WildcardFileFilter(extentions);
		File[] cacheFiles = folder.listFiles(fileFilter);
		for(File f : cacheFiles) {
			ICache.deleteFile(f);
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
				e.printStackTrace();
			}
		}
	}
	
}
