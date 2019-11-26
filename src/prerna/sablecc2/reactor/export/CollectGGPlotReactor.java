package prerna.sablecc2.reactor.export;

import java.io.File;
import java.io.FileWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import prerna.algorithm.api.SemossDataType;
import prerna.engine.api.IRawSelectWrapper;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.om.task.BasicIteratorTask;
import prerna.sablecc2.om.task.ConstantDataTask;
import prerna.sablecc2.om.task.options.TaskOptions;
import prerna.sablecc2.reactor.frame.r.util.AbstractRJavaTranslator;
import prerna.sablecc2.reactor.task.TaskBuilderReactor;
import prerna.util.Utility;

public class CollectGGPlotReactor extends TaskBuilderReactor {

	/**
	 * This class is responsible for collecting data from a task and returning it
	 */

	private int limit = 0;
	
	public CollectGGPlotReactor() {
		this.keysToGet = new String[] { ReactorKeysEnum.GGPLOT.getKey()};
	}
	
	public NounMetadata execute() {
		
		organizeKeys();

		AbstractRJavaTranslator rJavaTranslator = this.insight.getRJavaTranslator(this.getLogger(this.getClass().getName()));
		rJavaTranslator.startR(); 
		
		String ggplotCommand = keyValue.get(keysToGet[0]) +"";
		
		this.task = getTask();
		
		// I neeed to get the basic iterator and then get types from there
		task.getMetaMap();
		SemossDataType[] sTypes = ((IRawSelectWrapper) ((BasicIteratorTask)(task)).getIterator()).getTypes();
		String[] headers = ((IRawSelectWrapper) ((BasicIteratorTask)(task)).getIterator()).getHeaders();
		
		Map<String, SemossDataType> typeMap = new HashMap<String, SemossDataType>();
		for(int i = 0; i < headers.length; i++) {
			typeMap.put(headers[i],sTypes[i]);
		}
		
		
		// I need to see how to get this to temp
		String fileName = Utility.getRandomString(6);
		String dir = insight.getUserFolder() + "/Temp";
		dir = dir.replaceAll("\\\\", "/");
		File tempDir = new File(dir);
		if(!tempDir.exists())
			tempDir.mkdir();
		String outputFile = dir + "/" + fileName + ".csv";
		Utility.writeResultToFile(outputFile, this.task, typeMap, ",");
		
		
		StringBuilder ggplotter = new StringBuilder("{library(\"ggplot2\");"); // library(\"RCurl\");");
		ggplotter.append(fileName + " <- fread(\"" + outputFile + "\");");
		// get the frame
		// get frame name
		String table = fileName;
		
		// run the ggplot with this frame as the data
		// we will refer to it as the plotterframe
		ggplotter.append("plotterframe <- " + fileName + ";");
		
		// now it is just running the ggplotter
		String plotString = Utility.getRandomString(6);
		ggplotter.append(plotString + " <- " + ggplotCommand + ";");
		
		// now save it
		// file name
		String ggsaveFile = Utility.getRandomString(6);
		
		
		ggplotter.append(ggsaveFile + " <- " + "paste(ROOT,\"/" + ggsaveFile + "\",\".jpeg\", sep=\"\");");
		
		ggplotter.append("ggsave(" + ggsaveFile + ");");

		ggplotter.append("}");
		
		// run the ggplotter command
		rJavaTranslator.runRAndReturnOutput(ggplotter.toString());

		// get file name
		String retFile = rJavaTranslator.runRAndReturnOutput(ggsaveFile);

		// also try to encode it
		/*String encode = "base64Encode(readBin(" + ggsaveFile + " , \"raw\", file.info(" + ggsaveFile + ")[1, \"size\"]), \"txt\")";
		
		String dataURI = rJavaTranslator.runRAndReturnOutput(encode);
		System.out.println(dataURI);
		*/
		String ggremove = "rm(" + ggsaveFile + ", txt);detach(\"package:ggplot2\", unload=FALSE);"; //detach(\"package:RCurl\", unload=FALSE)";
		
		// remove the variable
		rJavaTranslator.runRAndReturnOutput(ggsaveFile);

		// remove the csv
		new File(outputFile).delete();
		
		// Need to figure out if I am trying to delete the image and URI encode it at some point.. 
		
		ConstantDataTask cdt = new ConstantDataTask();
		// need to do all the sets
		cdt.setFormat("TABLE");
		
		// I need to create the options here
		Map optionMap = new HashMap<String, Object>();
		optionMap.put(keysToGet[0], ggplotCommand);
		
		TaskOptions options = new TaskOptions(optionMap);
		cdt.setTaskOptions(options);
		cdt.setHeaderInfo(task.getHeaderInfo());
		cdt.setSortInfo(task.getSortInfo());
		
		// set the output so it can give it
		cdt.setOutputData(retFile);
		// I dont think the filter information is required
		//cdt.setFilterInfo(task.getFilterInfo());
		// delete the pivot later
		return new NounMetadata(cdt, PixelDataType.FORMATTED_DATA_SET, PixelOperationType.TASK_DATA);
	}
	

	// keeping these methods for now.. I am not sure I require them
	
	@Override
	protected void buildTask() {
		// if the task was already passed in
		// we do not need to optimize/recreate the iterator
		if(this.task.isOptimized()) {
			this.task.optimizeQuery(this.limit);
		}
	}
	
	private TaskOptions genOrnamentTaskOptions() {
		if(this.subAdditionalReturn != null && this.subAdditionalReturn.size() == 1) {
			NounMetadata noun = this.subAdditionalReturn.get(0);
			if(noun.getNounType() == PixelDataType.ORNAMENT_MAP) {
				// we will use this map as task options
				TaskOptions options = new TaskOptions((Map<String, Object>) noun.getValue());
				options.setOrnament(true);
				return options;
			}
		}
		return null;
	}
	
	//returns how much do we need to collect
	private int getTotalToCollect() {
		// try the key
		GenRowStruct numGrs = store.getNoun(keysToGet[1]);
		if(numGrs != null && !numGrs.isEmpty()) {
			return ((Number) numGrs.get(0)).intValue();
		}
		
		// try the cur row
		List<Object> allNumericInputs = this.curRow.getAllNumericColumns();
		if(allNumericInputs != null && !allNumericInputs.isEmpty()) {
			return ((Number) allNumericInputs.get(0)).intValue();
		}
		
		// default to 500
		return 500;
	}
	
	@Override
	public List<NounMetadata> getOutputs() {
		List<NounMetadata> outputs = super.getOutputs();
		if(outputs != null && !outputs.isEmpty()) return outputs;
		
		outputs = new Vector<NounMetadata>();
		NounMetadata output = new NounMetadata(this.signature, PixelDataType.FORMATTED_DATA_SET, PixelOperationType.TASK_DATA);
		outputs.add(output);
		return outputs;
	}
	
	///////////////////////// KEYS /////////////////////////////////////

	@Override
	protected String getDescriptionForKey(String key) {
		if (key.equals(ReactorKeysEnum.LIMIT.getKey())) {
			return "The number to collect";
		} else {
			return super.getDescriptionForKey(key);
		}
	}
	

	// encoding
	// uses RCurl
	// txt <- base64Encode(readBin(tf1, "raw", file.info(tf1)[1, "size"]), "txt")
	// tf1 - is just the file location
	// https://stackoverflow.com/questions/33409363/convert-r-image-to-base-64/36707831?noredirect=1#comment61003382_36707831
	
	
}
