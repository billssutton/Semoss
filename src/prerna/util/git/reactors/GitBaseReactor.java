package prerna.util.git.reactors;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.kohsuke.github.GitHub;

import prerna.auth.AccessToken;
import prerna.auth.AuthProvider;
import prerna.auth.User2;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.git.GitAssetMaker;
import prerna.util.git.GitUtils;

public abstract class GitBaseReactor extends AbstractReactor {


	public String getToken() {

		
		User2 user = insight.getUser2();
		String oauth = null;
		AccessToken gitAccess = user.getAccessToken(AuthProvider.GIT.name());
		
		if(gitAccess == null)
		{
			Map<String, Object> retMap = new HashMap<String, Object>();
			retMap.put("type", "git");
			retMap.put("message", "Please login to your Git account");
			throwError(retMap);
		}

		return gitAccess.getAccess_token();
	}
}
