package prerna.sablecc2.reactor.app;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.solr.client.solrj.SolrServerException;

import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.solr.SolrIndexEngine;

public class SetAppTagsReactor extends AbstractReactor {
	
	public SetAppTagsReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.APP.getKey(), "tags"};
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		String appName = this.keyValue.get(this.keysToGet[0]);
		List<String> tags = getTags();
		
		Map<String, Object> fieldsToModify = new HashMap<String, Object>();
		fieldsToModify.put("app_tags", tags);
		try {
			SolrIndexEngine.getInstance().modifyApp(appName, fieldsToModify);
			return new NounMetadata(true, PixelDataType.BOOLEAN, PixelOperationType.APP_INFO);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | SolrServerException
				| IOException e1) {
			e1.printStackTrace();
		}

		return new NounMetadata(false, PixelDataType.BOOLEAN, PixelOperationType.APP_INFO);
	}
	
	public List<String> getTags() {
		List<String> tags = new Vector<String>();
		
		// see if added as key
		GenRowStruct grs = this.store.getNoun(this.keysToGet[1]);
		if(grs != null && !grs.isEmpty()) {
			int size = grs.size();
			for(int i = 0; i < size; i++) {
				tags.add(grs.get(i).toString());
			}
			return tags;
		}
		
		// start at index 1 and see if in cur row
		int size = this.curRow.size();
		for(int i = 2; i < size; i++) {
			tags.add(grs.get(i).toString());
		}
		return tags;
	}

}
