package prerna.util.git.gitlab;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import prerna.reactor.AbstractReactor;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;

public class GitlabPullLastSuccessfulJobArtifactReactor extends AbstractReactor {

	public GitlabPullLastSuccessfulJobArtifactReactor() {
		this.keysToGet = new String[] {
				ReactorKeysEnum.HOST.getKey(), ReactorKeysEnum.GITLAB_PROJECT_ID.getKey(),
				ReactorKeysEnum.GITLAB_BRANCH_NAME.getKey(), ReactorKeysEnum.GITLAB_JOB_NAME.getKey(),
				ReactorKeysEnum.GITLAB_PRIVATE_TOKEN.getKey(), 
				ReactorKeysEnum.USE_APPLICATION_CERT.getKey()
			};
	}
	
	@Override
	public NounMetadata execute() {
		organizeKeys();
		String host = this.keyValue.get(ReactorKeysEnum.HOST.getKey());
		String gitProjectId = this.keyValue.get(ReactorKeysEnum.GITLAB_PROJECT_ID.getKey());
		String gitBranch = this.keyValue.get(ReactorKeysEnum.GITLAB_BRANCH_NAME.getKey());
		String gitJobName = this.keyValue.get(ReactorKeysEnum.GITLAB_JOB_NAME.getKey());
		String gitPrivateToken = this.keyValue.get(ReactorKeysEnum.GITLAB_PRIVATE_TOKEN.getKey());
		Boolean useApplicationCert = Boolean.parseBoolean(this.keyValue.get(ReactorKeysEnum.USE_APPLICATION_CERT.getKey()) + "");
		
		String saveFilePath = this.insight.getInsightFolder();
		File artifact = GitlabUtility.pullLastSuccessfulJobArtifact(host, gitProjectId, gitBranch, gitJobName, 
				null, gitPrivateToken, useApplicationCert, saveFilePath, null);
		
		String artifactFilePath = artifact.getAbsolutePath();
		String artifactFileName = FilenameUtils.getName(artifactFilePath);
		return new NounMetadata(artifactFileName, PixelDataType.CONST_STRING);
	}

	@Override
	public String getReactorDescription() {
		return "This reactor pulls the latest successful artifact for a GitLab project. "
				+ "Specifically, its pulls the last successful pipeline execution for a specific branch and for a specific job name "
				+ "as the pipeline might produce multiple artifacts based on the stages";
	}
	
}
