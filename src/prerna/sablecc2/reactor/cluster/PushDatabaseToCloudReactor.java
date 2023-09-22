package prerna.sablecc2.reactor.cluster;

import prerna.auth.utils.SecurityEngineUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;

public class PushDatabaseToCloudReactor extends AbstractReactor {
	
	public PushDatabaseToCloudReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.DATABASE.getKey()};
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		String databaseId = this.keyValue.get(this.keysToGet[0]);
		
		if(databaseId == null || databaseId.isEmpty()) {
			throw new IllegalArgumentException("Must input an database id");
		}
		
		// make sure valid id for user
		if(!SecurityEngineUtils.userIsOwner(this.insight.getUser(), databaseId)) {
			// you dont have access
			throw new IllegalArgumentException("Database does not exist or user is not an owner to force pushing to cloud storage");
		}
		
		ClusterUtil.pushEngine(databaseId);
		return new NounMetadata(true, PixelDataType.BOOLEAN);
	}

}

