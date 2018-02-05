package prerna.sablecc2.reactor.insights;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.h2.H2Frame;
import prerna.ds.r.RDataTable;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.VarStore;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.om.task.TaskStore;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.sablecc2.reactor.imports.FileMeta;

public class ClearInsightReactor extends AbstractReactor {

	private static final String CLASS_NAME = DropInsightReactor.class.getName();

	@Override
	public NounMetadata execute() {
		Logger logger = this.getLogger(CLASS_NAME);

		String insightId = this.insight.getInsightId();
		logger.info("Trying to clear insight " + insightId);

		// drop all the tasks that are currently running
		TaskStore taskStore = this.insight.getTaskStore();
		taskStore.clearAllTasks();
		logger.info("Successfully cleared all stored Tasks for the insight");

		// drop all the frame connections
		VarStore varStore = this.insight.getVarStore();
		Set<String> keys = varStore.getKeys();
		// find all the vars which are frames
		// and drop them
		for(String key : keys) {
			NounMetadata noun = varStore.get(key);
			PixelDataType nType = noun.getNounType();
			if(nType == PixelDataType.FRAME) {
				ITableDataFrame dm = (ITableDataFrame) noun.getValue();
				dm.setLogger(logger);
				//TODO: expose a delete on the frame to hide this crap
				// drop the existing tables/connections if present
				if(dm instanceof H2Frame) {
					H2Frame frame = (H2Frame)dm;
					frame.dropTable();
					if(!frame.isInMem()) {
						frame.dropOnDiskTemporalSchema();
					}
				} else if(dm instanceof RDataTable) {
					RDataTable frame = (RDataTable) dm;
					frame.closeConnection();
				}
			}
		}
		logger.info("Successfully removed all frames from insight");
		
		this.insight.getVarStore().clear();
		logger.info("Successfully removed all variables from varstore");

		// also delete any files that were used
		List<FileMeta> fileData = insight.getFilesUsedInInsight();
		if (fileData != null && !fileData.isEmpty()) {
			for (int fileIdx = 0; fileIdx < fileData.size(); fileIdx++) {
				FileMeta file = fileData.get(fileIdx);
				File f = new File(file.getFileLoc());
				f.delete();
				logger.info("Successfully deleted File used in insight " + file.getFileLoc());
			}
		}
		
		Map<String, String> fileExports = insight.getExportFiles();
		if (fileExports != null && !fileExports.isEmpty()) {
			for (String fileKey : fileExports.keySet()){
				File f = new File(fileExports.get(fileKey));
				f.delete();
				logger.info("Successfully deleted File used in insight " + f.getName());
			}
		}

		logger.info("Successfully cleared insight");
		return new NounMetadata(true, PixelDataType.BOOLEAN, PixelOperationType.CLEAR_INSIGHT);
	}
}
