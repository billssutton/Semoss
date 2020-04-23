package prerna.util.git.reactors;

import java.util.List;
import java.util.Vector;

import prerna.auth.AccessToken;
import prerna.auth.AuthProvider;
import prerna.auth.User;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.AssetUtility;
import prerna.util.git.GitRepoUtils;

public class CommitAssetReactor extends AbstractReactor {

	public CommitAssetReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.FILE_PATH.getKey(), ReactorKeysEnum.COMMENT_KEY.getKey(),
				ReactorKeysEnum.SPACE.getKey() };
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		User user = this.insight.getUser();
		String author = null;
		String email = null;
		// check if user is logged in
		if (AbstractSecurityUtils.securityEnabled()) {
			if (AbstractSecurityUtils.anonymousUsersEnabled() && user.isAnonymous()) {
				throwAnonymousUserError();
			}
			// Get the user's email
			AccessToken accessToken = user.getAccessToken(user.getPrimaryLogin());
			email = accessToken.getEmail();
			author = accessToken.getUsername();
		}
		String filePath = this.keyValue.get(this.keysToGet[0]);
		String comment = this.keyValue.get(this.keysToGet[1]);
		String space = this.keyValue.get(this.keysToGet[2]);
		if(space == null || space.trim().isEmpty() || space.equals(AssetUtility.INSIGHT_SPACE_KEY)) {
			// if we are in the insight space
			// it must be a saved insight
			if(!this.insight.isSavedInsight()) {
				return NounMetadata.getWarningNounMessage("Unable to commit file. All files will be commited once the insight is saved.");
			}
		}
		
		String assetFolder = AssetUtility.getAssetVersionBasePath(this.insight, space);
		String relativePath = AssetUtility.getAssetRelativePath(this.insight, space);
		// add file to git
		List<String> files = new Vector<>();
		files.add(relativePath + DIR_SEPARATOR + filePath);		
		GitRepoUtils.addSpecificFiles(assetFolder, files);
		
		// commit it
		GitRepoUtils.commitAddedFiles(assetFolder, comment, author, email);
		if (AssetUtility.USER_SPACE_KEY.equalsIgnoreCase(space)) {
			if (AbstractSecurityUtils.securityEnabled()) {
				if (AbstractSecurityUtils.anonymousUsersEnabled() && user.isAnonymous()) {
					throwAnonymousUserError();
				}
				AuthProvider provider = user.getPrimaryLogin();
				String appId = user.getAssetEngineId(provider);
				if(appId!=null && !(appId.isEmpty())) {
					ClusterUtil.reactorPushApp(appId);
				}
			}
		} else {
			//if space is null or it is in the insight, push using insight id to get engine
			if(space == null || space.trim().isEmpty() || space.equals(AssetUtility.INSIGHT_SPACE_KEY)) {
				ClusterUtil.reactorPushApp(this.insight.getEngineId());

			} else {
				//this is an app asset. Space is the appID
			ClusterUtil.reactorPushApp(space);
			}
		}

		return NounMetadata.getSuccessNounMessage("Success!");

	}
}
