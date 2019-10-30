package prerna.sablecc2.reactor.masterdatabase;

import java.util.List;

import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityQueryUtils;
import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;

public class GetSpecificConceptPropertiesReactor extends AbstractReactor {

	public GetSpecificConceptPropertiesReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.CONCEPT.getKey(), ReactorKeysEnum.DATABASE.getKey()};
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		GenRowStruct conceptNamesGrs = this.store.getNoun(keysToGet[0]);
		if (conceptNamesGrs == null) {
			throw new IllegalArgumentException("Need to define the concepts to find relations");
		}
		String conceptLogicals = conceptNamesGrs.get(0).toString();

		// account for optional engine filter
		GenRowStruct engineFilterGrs = this.store.getNoun(keysToGet[1]);
		if (engineFilterGrs == null) {
			throw new IllegalArgumentException("Need to define the engine filter");
		}
		String engineId = engineFilterGrs.get(0).toString();

		if(AbstractSecurityUtils.securityEnabled()) {
			engineId = SecurityQueryUtils.testUserEngineIdForAlias(this.insight.getUser(), engineId);
			List<String> appFilters = SecurityQueryUtils.getFullUserEngineIds(this.insight.getUser());
			if(!appFilters.contains(engineId)) {
				throw new IllegalArgumentException("Databases " + engineId + " does not exist or user does not have access");
			}
		} else {
			engineId = MasterDatabaseUtility.testEngineIdIfAlias(engineId);
		}
		
		List<String> conceptProperties = MasterDatabaseUtility.getSpecificConceptProperties(conceptLogicals, engineId);
		return new NounMetadata(conceptProperties, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.DATABASE_CONCEPT_PROPERTIES);
	}

}
