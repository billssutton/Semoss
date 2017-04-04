package prerna.sablecc2.reactor;

import java.util.List;
import java.util.Vector;

import prerna.algorithm.api.ITableDataFrame;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PkslDataTypes;

public class CreateFrame extends AbstractReactor {

	@Override
	public void In() {
		curNoun("all");
	}

	@Override
	public Object Out() {
		return parentReactor;
	}
	
	public Object execute() {
		// get the name of the frame type
		String frameType = this.curRow.get(0).toString();
		// use factory to generate the new table
		ITableDataFrame newFrame = FrameFactory.getFrame(frameType);
		// store it as the result and push it to the planner to override
		// any existing frame that was in use
		NounMetadata result = new NounMetadata(newFrame, PkslDataTypes.FRAME);
		planner.addProperty("FRAME", "FRAME", newFrame);
		return result;
	}

	@Override
	public void mergeUp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<NounMetadata> getOutputs() {
		List<NounMetadata> outputs = new Vector<NounMetadata>();
		NounMetadata output = new NounMetadata(this.signature, PkslDataTypes.FRAME);
		outputs.add(output);
		return outputs;
	}

	@Override
	public List<NounMetadata> getInputs() {
		// this can only have 1 single input
		// which will be the noun containing the string
		// for the specific frame type to use
		List<NounMetadata> inputs = new Vector<NounMetadata>();
		inputs.add(this.curRow.getNoun(0));
		return inputs;
	}

}
