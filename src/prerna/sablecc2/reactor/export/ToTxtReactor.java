package prerna.sablecc2.reactor.export;

import java.io.File;
import java.util.List;
import java.util.UUID;

import prerna.om.InsightFile;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Utility;

public class ToTxtReactor extends AbstractExportTxtReactor {

	private static final String CLASS_NAME = ToTxtReactor.class.getName();
	
	public ToTxtReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.TASK.getKey(), ReactorKeysEnum.DELIMITER.getKey(), 
				ReactorKeysEnum.FILE_NAME.getKey(), ReactorKeysEnum.FILE_PATH.getKey()};
	}
	
	@Override
	public NounMetadata execute() {
		organizeKeys();
		this.logger = getLogger(CLASS_NAME);
		this.task = getTask();
		// set to tab separated
		this.setDelimiter(getDelimiter());
		
		String downloadKey = UUID.randomUUID().toString();
		InsightFile insightFile = new InsightFile();
		insightFile.setFileKey(downloadKey);
		
		// get a random file name
		String prefixName =  Utility.normalizePath(this.keyValue.get(ReactorKeysEnum.FILE_NAME.getKey()));
		String exportName = AbstractExportTxtReactor.getExportFileName(prefixName, "txt");
		// grab file path to write the file
		this.fileLocation = this.keyValue.get(ReactorKeysEnum.FILE_PATH.getKey());
		// if the file location is not defined generate a random path and set
		// location so that the front end will download
		if (this.fileLocation == null) {
			String insightFolder = this.insight.getInsightFolder();
			File f = new File(insightFolder);
			if(!f.exists()) {
				f.mkdirs();
			}
			this.fileLocation = insightFolder + DIR_SEPARATOR + exportName;
			insightFile.setDeleteOnInsightClose(true);
		} else {
			insightFile.setDeleteOnInsightClose(false);
			this.fileLocation += DIR_SEPARATOR + exportName;
		}
		insightFile.setFilePath(this.fileLocation);
		buildTask();

		// store the insight file 
		// in the insight so the FE can download it
		// only from the given insight
		this.insight.addExportFile(downloadKey, insightFile);
		
		NounMetadata retNoun = new NounMetadata(downloadKey, PixelDataType.CONST_STRING, PixelOperationType.FILE_DOWNLOAD);
		retNoun.addAdditionalReturn(NounMetadata.getSuccessNounMessage("Successfully generated the txt file"));
		return retNoun;
	}
	
	private String getDelimiter() {
		GenRowStruct grs = store.getNoun(this.keysToGet[1]);
		if (grs != null && !grs.isEmpty()) {
			return grs.get(0).toString();
		}
		
		List<String> values = this.curRow.getAllStrValues();
		if(values != null && !values.isEmpty()) {
			return values.get(0);
		}
		
		throw new IllegalArgumentException("Must define the delimiter");
	}

}
