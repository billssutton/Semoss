package prerna.sablecc2.reactor.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityDatabaseUtils;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;

public class GetDatabaseUserAccessRequestReactor extends AbstractReactor {
	
	public GetDatabaseUserAccessRequestReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.DATABASE.getKey()};
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		String databaseId = this.keyValue.get(this.keysToGet[0]);
		if(databaseId == null) {
			throw new IllegalArgumentException("Please define the database id.");
		}
		List<Map<String, Object>> requests = null;
		if(AbstractSecurityUtils.securityEnabled()) {
			requests = SecurityDatabaseUtils.getUserAccessRequestsByDatabase(databaseId);
		} else {
			requests = new ArrayList<>();
		}
		return new NounMetadata(requests, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.DATABASE_INFO);
	}
}
