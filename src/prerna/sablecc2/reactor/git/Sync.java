package prerna.sablecc2.reactor.git;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.DIHelper;
import prerna.util.GitHelper;
import prerna.util.MosfetSyncHelper;

public class Sync extends AbstractReactor {

	public Sync() {
		this.keysToGet = new String[]{"app", "remoteApp", "username", "password", "dual"};
	}
	
	@Override
	public NounMetadata execute() {
		Logger logger = getLogger(this.getClass().getName());
		try {
			organizeKeys();
			
			logger.info("Starting the synchronization process");
			GitHelper helper = new GitHelper();
			String baseFolder = DIHelper.getInstance().getProperty("BaseFolder");
			//helper.synchronize(baseFolder + "/db/Mv2Git4", "Mv4", userName, password, true);

			logger.info("Initialized the base folder");
			String dbName = baseFolder + "/db/" + keyValue.get(keysToGet[0]);	
			
			// if nothing is sent it means it is dual
			boolean dual = false;
			if(keyValue.containsKey(keysToGet[4]) && keyValue.get(keysToGet[4]).equalsIgnoreCase("true")) {
				dual = true;
			}
			
			// if it is null or true dont worry
			logger.info("Synchronizing now");
			Hashtable <String, List<String>> filesChanged = helper.synchronize(dbName, keyValue.get(keysToGet[1]), keyValue.get(keysToGet[2]), keyValue.get(keysToGet[3]), dual);

			logger.info("Synchronize Complete");
			StringBuffer output = new StringBuffer("SUCCESS \r\n ");
			output.append("ADDED : ");
			if(filesChanged.containsKey("ADD")) {
				output.append(filesChanged.get("ADD").size());
			} else {
				output.append("0");
			}
			output.append(" , MODIFIED : ");
			if(filesChanged.containsKey("MOD")) {
				output.append(filesChanged.get("MOD").size());
			} else {
				output.append("0");
			}
			output.append(" , RENAMED : ");
			if(filesChanged.containsKey("REN")) {
				output.append(filesChanged.get("REN").size());
			} else {
				output.append("0");
			}
			output.append(" , DELETED : ");
			if(filesChanged.containsKey("DEL")) {
				output.append(filesChanged.get("DEL").size());
			} else {
				output.append("0");
			}

			logger.info("Indexing your changes");
			// will update solr and in the engine rdbms insights database
			MosfetSyncHelper.synchronizeInsightChanges(getMosfetFiles(filesChanged), logger);
			logger.info("Index complete");

			return new NounMetadata(output.toString(), PixelDataType.CONST_STRING, PixelOperationType.MARKET_PLACE);
		} catch (Exception e) {
			e.printStackTrace();
			logger.fatal(e.getMessage());
		}
		return null;
	}
	
	/**
	 * Need to get the appropriate files to perform indexing!
	 * @param filesChanged
	 * @return
	 */
	private Map<String, List<String>> getMosfetFiles(Map <String, List<String>> filesChanged) {
		Map<String, List<String>> mosfetFiles = new Hashtable<String, List<String>>();
		if(filesChanged.containsKey("ADD")) {
			List<String> files = getMosfetFiles(filesChanged.get("ADD"));
			if(!files.isEmpty()) {
				mosfetFiles.put("ADD", files);
			}
		}
		
		if(filesChanged.containsKey("MOD")) {
			List<String> files = getMosfetFiles(filesChanged.get("MOD"));
			if(!files.isEmpty()) {
				mosfetFiles.put("MOD", files);
			}		
		}
		
		if(filesChanged.containsKey("REN")) {
			List<String> files = getMosfetFiles(filesChanged.get("REN"));
			if(!files.isEmpty()) {
				mosfetFiles.put("REN", files);
			}		
		}
		
		if(filesChanged.containsKey("DEL")) {
			List<String> files = getMosfetFiles(filesChanged.get("DEL"));
			if(!files.isEmpty()) {
				mosfetFiles.put("DEL", files);
			}		
		}
		return mosfetFiles;
	}
	
	private List<String> getMosfetFiles(List<String> potentialFiles) {
		List<String> mosfetFiles = new Vector<String>();
		if(potentialFiles != null) {
			for(String f : potentialFiles) {
				if(f.endsWith(".mosfet")) {
					mosfetFiles.add(f);
				}
			}
		}
		return mosfetFiles;
	}

}
