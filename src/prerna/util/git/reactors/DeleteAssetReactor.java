package prerna.util.git.reactors;

import java.util.List;
import java.util.Vector;

import org.h2.store.fs.FileUtils;

import prerna.auth.AccessToken;
import prerna.auth.User;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.AssetUtility;
import prerna.util.git.GitDestroyer;
import prerna.util.git.GitRepoUtils;

public class DeleteAssetReactor extends AbstractReactor {

	public DeleteAssetReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.FILE_PATH.getKey(), ReactorKeysEnum.COMMENT_KEY.getKey(),
				ReactorKeysEnum.SPACE.getKey() };
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		// check if user is logged in
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

		// get asset base folder
		String space = this.keyValue.get(this.keysToGet[2]);
		String assetFolder = AssetUtility.getAssetVersionBasePath(this.insight, space);
		// relative path is used for git if insight is saved
		String relativePath = "";
		if (this.insight.isSavedInsight()) {
			relativePath = AssetUtility.getAssetRelativePath(this.insight, space);
		}

		// get the file path to delete
		String fileName = keyValue.get(keysToGet[0]);
		String comment = this.keyValue.get(this.keysToGet[1]);

		List<String> files = new Vector<>();
		files.add(relativePath + "/" + fileName);
		// if security enables, you need proper permissions
		// this takes in the insight and does a user check that the user has access to perform the operations
		String baseFolder = AssetUtility.getAssetBasePath(this.insight, space, AbstractSecurityUtils.securityEnabled());
		FileUtils.delete(baseFolder + "/" + fileName);

		// commit it
		if (this.insight.isSavedInsight()) {
			GitDestroyer.removeSpecificFiles(assetFolder, true, files);
			GitRepoUtils.commitAddedFiles(assetFolder, comment, author, email);
			ClusterUtil.reactorPushApp(this.insight.getEngineId());
		}

		return NounMetadata.getSuccessNounMessage("Success!");
	}
}
