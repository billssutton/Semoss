package prerna.sablecc2.reactor.export;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.export.graph.GraphExporterFactory;
import prerna.ds.export.graph.IGraphExporter;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;

public class CollectGraphReactor extends CollectReactor {

	/**
	 * This class is responsible for collecting all graph data from a frame
	 */
	
	private static final String FRAME_KEY = "frame";

	public NounMetadata execute() {
		this.task = getTask();

		ITableDataFrame frame = getFrame();
		IGraphExporter exporter = GraphExporterFactory.getExporter(frame);
		Map<String, Object> collectedData = new HashMap<String, Object>(7);
		collectedData.put("data", exporter.getData());
		collectedData.put("taskId", "TEMP_ID");
		collectedData.put("numCollected", "-1");
		
		Map<String, Object> formatMap = new HashMap<String, Object>();
		formatMap.put("type", "GRAPH");
		collectedData.put("format", formatMap);
		collectedData.put("taskOptions", this.task.getTaskOptions());
		collectedData.put("headerInfo", this.task.getHeaderInfo());
		collectedData.put("sortInfo", this.task.getSortInfo());
		
		NounMetadata result = new NounMetadata(collectedData, PixelDataType.FORMATTED_DATA_SET, PixelOperationType.TASK_DATA);
		return result;
	}
	
	private ITableDataFrame getFrame() {
		// try the key
		GenRowStruct fGrs = store.getNoun(FRAME_KEY);
		if(fGrs != null && !fGrs.isEmpty()) {
			return (ITableDataFrame) fGrs.get(0);
		}
		
		// try the cur row
		List<Object> allNumericInputs = this.curRow.getValuesOfType(PixelDataType.FRAME);
		if(allNumericInputs != null && !allNumericInputs.isEmpty()) {
			return (ITableDataFrame) allNumericInputs.get(0);
		}
		
		return (ITableDataFrame) this.insight.getDataMaker();
	}

	@Override
	public List<NounMetadata> getOutputs() {
		List<NounMetadata> outputs = super.getOutputs();
		if(outputs != null) return outputs;
		
		outputs = new Vector<NounMetadata>();
		NounMetadata output = new NounMetadata(this.signature, PixelDataType.FORMATTED_DATA_SET, PixelOperationType.TASK_DATA);
		outputs.add(output);
		return outputs;
	}
}
