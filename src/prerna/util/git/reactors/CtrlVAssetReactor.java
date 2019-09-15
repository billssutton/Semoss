package prerna.util.git.reactors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import prerna.auth.User;
import prerna.om.CopyObject;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.AssetUtility;

public class CtrlVAssetReactor extends AbstractReactor {

	public CtrlVAssetReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.SPACE.getKey(), ReactorKeysEnum.FILE_PATH.getKey() };
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		User user = this.insight.getUser();
		String filePath = this.keyValue.get(this.keysToGet[1]);
		String space = this.keyValue.get(this.keysToGet[0]);

		String assetFolder = AssetUtility.getAssetVersionBasePath(this.insight, space);
		String relativePath = AssetUtility.getAssetRelativePath(this.insight, space);
		
		if(filePath != null)
			filePath =  DIR_SEPARATOR + filePath;
		else
			filePath = "";
		// file / folder to be moved
		String destSource = assetFolder + DIR_SEPARATOR + relativePath + filePath;
				
		// need to make sure the destination is valid
		File file = new File(destSource);
		if(!(file.exists() && file.isDirectory()))
			throw new IllegalArgumentException("Destination should be a directory : " + filePath);
		
		CopyObject copyObj = user.getCtrlC();
		String copySource = copyObj.source;
		boolean isDelete = copyObj.delete;
		
		
		if(copySource == null)
			throw new IllegalArgumentException("Nothing to copy, please copy something first ");

		File sfile = new File(copySource);
		boolean isSourceDir = sfile.exists() && sfile.isDirectory();

		String dirName = sfile.getName();
		
		destSource = destSource + DIR_SEPARATOR + dirName;
		file = new File(destSource);
		try {
			if(isSourceDir)
			{
				FileUtils.copyDirectory(sfile, file);
				if(isDelete)
					FileUtils.deleteDirectory(sfile);
			}
			else
			{
				FileUtils.copyFileToDirectory(sfile, file);
				if(isDelete)
					sfile.delete();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return NounMetadata.getSuccessNounMessage("Copied " + filePath);

	}
}
