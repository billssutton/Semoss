package prerna.sablecc2.reactor.database.upload.modifications;

import java.io.IOException;

import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityDatabaseUtils;
import prerna.auth.utils.SecurityQueryUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.engine.api.IEngine;
import prerna.engine.api.IEngineModifier;
import prerna.engine.api.impl.util.Owler;
import prerna.engine.impl.modifications.EngineModificationFactory;
import prerna.nameserver.utility.MasterDatabaseUtility;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.sablecc2.reactor.masterdatabase.SyncDatabaseWithLocalMasterReactor;
import prerna.util.EngineSyncUtility;
import prerna.util.Utility;

public class DatabaseRenameColumnReactor extends AbstractReactor {

	public DatabaseRenameColumnReactor() {
		this.keysToGet = new String[] { 
				ReactorKeysEnum.DATABASE.getKey(), 
				ReactorKeysEnum.CONCEPT.getKey(),
				ReactorKeysEnum.COLUMN.getKey(), 
				ReactorKeysEnum.NEW_VALUE.getKey()
				};
		this.keyRequired = new int[] { 1, 1, 1, 1 };
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		
		String databaseId = this.keyValue.get(this.keysToGet[0]);
		if(AbstractSecurityUtils.securityEnabled()) {
			databaseId = SecurityQueryUtils.testUserDatabaseIdForAlias(this.insight.getUser(), databaseId);
			if(!SecurityDatabaseUtils.userCanEditDatabase(this.insight.getUser(), databaseId)) {
				throw new IllegalArgumentException("Database" + databaseId + " does not exist or user does not have access to database");
			}
		} else {
			databaseId = MasterDatabaseUtility.testDatabaseIdIfAlias(databaseId);
			if(!MasterDatabaseUtility.getAllDatabaseIds().contains(databaseId)) {
				throw new IllegalArgumentException("Database " + databaseId + " does not exist");
			}
		}
		
		String table = this.keyValue.get(this.keysToGet[1]);
		String existingColumn = this.keyValue.get(this.keysToGet[2]);
		String newColumn = this.keyValue.get(this.keysToGet[3]);
		
		IEngine database = Utility.getEngine(databaseId);
		Owler owler = new Owler(database);
		boolean dbUpdate = false;
		IEngineModifier modifier = EngineModificationFactory.getEngineModifier(database);
		if (modifier == null) {
			throw new IllegalArgumentException("This type of data modification has not been implemented for this database type");
		}
		try {
			modifier.renameProperty(table, existingColumn, newColumn);
			dbUpdate = true;
		} catch (Exception e) {
			throw new IllegalArgumentException("Error occurred to alter the table. Error returned from driver: " + e.getMessage(), e);
		}
		
		// update owl
		try {
			owler.renameProp(databaseId, table, existingColumn, newColumn);
			owler.export();
			SyncDatabaseWithLocalMasterReactor syncWithLocal = new SyncDatabaseWithLocalMasterReactor();
			syncWithLocal.setInsight(this.insight);
			syncWithLocal.setNounStore(this.store);
			syncWithLocal.In();
			syncWithLocal.execute();
		} catch (IOException e) {
			NounMetadata noun = new NounMetadata(dbUpdate, PixelDataType.BOOLEAN);
			noun.addAdditionalReturn(getError("Error occurred savig the metadata file with the executed changes"));
			return noun;
		}
		EngineSyncUtility.clearEngineCache(databaseId);
		ClusterUtil.reactorPushOwl(databaseId);

		NounMetadata noun = new NounMetadata(dbUpdate, PixelDataType.BOOLEAN);
		noun.addAdditionalReturn(NounMetadata.getSuccessNounMessage("Successfully renamed the column"));
		return noun;
	}
}
