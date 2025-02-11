package prerna.reactor.database.upload.modifications;

import prerna.auth.utils.SecurityEngineUtils;
import prerna.auth.utils.SecurityQueryUtils;
import prerna.engine.api.IDatabaseEngine;
import prerna.engine.api.IEngineModifier;
import prerna.engine.impl.modifications.EngineModificationFactory;
import prerna.reactor.AbstractReactor;
import prerna.reactor.database.metaeditor.properties.EditOwlPropertyDataTypeReactor;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Utility;

public class EditDatabasePropertyDataTypeReactor extends AbstractReactor {

	public EditDatabasePropertyDataTypeReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.DATABASE.getKey(),
				ReactorKeysEnum.CONCEPT.getKey(),
				ReactorKeysEnum.COLUMN.getKey(), 
				ReactorKeysEnum.DATA_TYPE.getKey(),
		};
		this.keyRequired = new int[]{1, 1, 1, 1};
	}
	
	@Override
	public NounMetadata execute() {
		this.organizeKeys();
		
		String databaseId = this.keyValue.get(this.keysToGet[0]);
		// we may have the alias
		databaseId = SecurityQueryUtils.testUserEngineIdForAlias(this.insight.getUser(), databaseId);
		if(!SecurityEngineUtils.userCanEditEngine(this.insight.getUser(), databaseId)) {
			throw new IllegalArgumentException("Database" + databaseId + " does not exist or user does not have access to database");
		}
		
		String table = this.keyValue.get(this.keysToGet[1]);
		String newColumn = this.keyValue.get(this.keysToGet[2]);
		String newColType = this.keyValue.get(this.keysToGet[3]);
		
		IDatabaseEngine database = Utility.getDatabase(databaseId);
		
		// keep track of existing data type in case something goes wrong
		
		// update the owl for any database
		// we will just update the 
		// the additional data types + description + new column type
		// are handled and used by this reactor
		EditOwlPropertyDataTypeReactor owlUpdater = new EditOwlPropertyDataTypeReactor();
		owlUpdater.setInsight(this.insight);
		owlUpdater.setNounStore(this.store);
		owlUpdater.execute();

		IEngineModifier modifier = EngineModificationFactory.getEngineModifier(database);
		if(modifier == null) {
			throw new IllegalArgumentException("This type of data modification has not been implemented for this database type");
		}
		try {
			modifier.editProperty(table, newColumn, newColType);
		} catch (Exception e) {
			// an error occurred here, so we need to revert our change from the OWL
//			try {
//				EditOwlPropertyDataTypeReactor owlRemover = new EditOwlPropertyDataTypeReactor();
//				owlRemover.setInsight(this.insight);
//				owlRemover.setNounStore(this.store);
//				owlRemover.execute();
//			} catch(Exception e2) {
//				classLogger.error(Constants.STACKTRACE, e2);
//			}
			
			throw new IllegalArgumentException("Error occurred to alter the table. Error returned from driver: " + e.getMessage(), e);
		}
		
		NounMetadata noun = new NounMetadata(true, PixelDataType.BOOLEAN);
		noun.addAdditionalReturn(NounMetadata.getSuccessNounMessage("Successfully modified data type of property"));
		return noun;
	}
}
