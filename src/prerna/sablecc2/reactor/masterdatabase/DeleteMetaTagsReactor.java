package prerna.sablecc2.reactor.masterdatabase;

import java.util.ArrayList;
import java.util.List;

import prerna.nameserver.AddToMasterDB;
import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.util.Constants;

public class DeleteMetaTagsReactor extends  AbstractMetaDBReactor {

	@Override
	public NounMetadata execute() {
		String engineName = getEngine();
		String concept = getConcept();
		List<String> valuesToDelete = getValues();
		String oldTagList = MasterDatabaseUtility.getMetadataValue(engineName, concept, Constants.TAG);
		ArrayList<String> tags = new ArrayList<String>();
		// organize existing tags into list
		for (String tag : oldTagList.split(VALUE_DELIMITER)) {
			if (tag.length() > 0) {
				tags.add(tag);
			}
		}
		// delete tags from list
		for(String deleteTag: valuesToDelete) {
			int indexToRemove = tags.indexOf(deleteTag);
			if(indexToRemove >=0) {
			tags.remove(indexToRemove);
			}
		}
		// update list
		String newTagList = "";
		for(String newTag: tags) {
			newTagList += newTag + VALUE_DELIMITER;
		}
		AddToMasterDB master = new AddToMasterDB();
		boolean success = master.addMetadata(engineName, concept, Constants.TAG, newTagList);
		return new NounMetadata(success, PixelDataType.BOOLEAN, PixelOperationType.CODE_EXECUTION);
	}
	

}
