package prerna.util.git.reactors;

import java.util.List;

import org.apache.logging.log4j.Logger;

import prerna.auth.AuthProvider;
import prerna.auth.User;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityQueryUtils;
import prerna.auth.utils.SecurityUpdateUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.execptions.SemossPixelException;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.sablecc2.reactor.app.upload.UploadUtilities;
import prerna.util.git.GitConsumer;

public class CopyAppRepo extends AbstractReactor {

	/**
	 * Clone an existing remote database and bring it into the 
	 * local semoss that is running for collaboration
	 */
	
	public CopyAppRepo() {
		super.keysToGet = new String[]{ReactorKeysEnum.DATABASE.getKey(), ReactorKeysEnum.REPOSITORY.getKey()};
	}
	
	@Override
	public NounMetadata execute() {
		organizeKeys();
		
		String localDatabaseName = this.keyValue.get(this.keysToGet[0]);
		if(localDatabaseName == null || localDatabaseName.isEmpty()) {
			throw new IllegalArgumentException("Need to define the local database name");
		}
		String repository = this.keyValue.get(this.keysToGet[1]);
		if(repository == null || repository.isEmpty()) {
			throw new IllegalArgumentException("Need to define a respository");
		}
		
		// check to see if the user is entering github.com and if so replace
		if(repository.contains("github.com"))
		{
			repository = repository.replace("http://github.com/","");
			repository = repository.replace("https://github.com/","");
		}
		Logger logger = getLogger(this.getClass().getName());
		logger.info("Downloading database located at " + repository);
		logger.info("Database will be named locally as " + localDatabaseName);

		
		// throw error if user is anonymous
		if(AbstractSecurityUtils.anonymousUsersEnabled() && this.insight.getUser().isAnonymous()) {
			throwAnonymousUserError();
		}

		// throw error is user doesn't have rights to publish new databases
		if(AbstractSecurityUtils.adminSetPublisher() && !SecurityQueryUtils.userIsPublisher(this.insight.getUser())) {
			throwUserNotPublisherError();
		}
		
		try {
			String databaseId = GitConsumer.makeDatabaseFromRemote(localDatabaseName, repository, logger);
			ClusterUtil.reactorPushDatabase(databaseId);
			User user = this.insight.getUser();
			if(user != null) {
				List<AuthProvider> logins = user.getLogins();
				for(AuthProvider ap : logins) {
					SecurityUpdateUtils.addDatabaseOwner(databaseId, user.getAccessToken(ap).getId());
				}
			}
			logger.info("Congratulations! Downloading your new database has been completed");
			return new NounMetadata(UploadUtilities.getDatabaseReturnData(user, databaseId), PixelDataType.MAP, PixelOperationType.MARKET_PLACE_ADDITION);
		} catch(Exception e) {
			SemossPixelException err = new SemossPixelException(NounMetadata.getErrorNounMessage(e.getMessage()));
			err.setContinueThreadOfExecution(false);
			throw err;
		}
	}
}
