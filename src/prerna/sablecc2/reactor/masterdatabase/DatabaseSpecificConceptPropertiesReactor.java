package prerna.sablecc2.reactor.masterdatabase;

import java.util.List;

import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;

public class DatabaseSpecificConceptPropertiesReactor extends AbstractReactor {

	public DatabaseSpecificConceptPropertiesReactor() {
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
		engineId = MasterDatabaseUtility.testEngineIdIfAlias(engineId);

		List<String> conceptProperties = MasterDatabaseUtility.getSpecificConceptPropertiesRDBMS(conceptLogicals, engineId);
		return new NounMetadata(conceptProperties, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.DATABASE_CONCEPT_PROPERTIES);
	}

}
