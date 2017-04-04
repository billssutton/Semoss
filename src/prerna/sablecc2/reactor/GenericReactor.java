package prerna.sablecc2.reactor;

import java.util.List;

import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.NounStore;
import prerna.sablecc2.om.PkslDataTypes;

public class GenericReactor extends AbstractReactor {

	public GenericReactor() {
		setName("Generic");
	}
	
	@Override
	public void In() {
		curNoun("all");
	}

	@Override
	public Object Out() {
		return parentReactor;
	}

	@Override
	public Object execute() {
		// THIS IS A SPECIAL CASE
		// when we execute, what we really want to do
		// is store this value into the parent
		// with the specific key that was used in the pksl
		// normal flow dictates that the parent's currow
		// is how this value should be added
		// but we need to override this for this special case

		// ... this is exactly what mergeUp does
		mergeUp();

		return null;
	}

	@Override
	public void mergeUp() {
		String key = (String)getProp("KEY");

		GenRowStruct allNouns = store.getNoun(NounStore.all);
		GenRowStruct thisStruct;
		if(store.getNoun(key) == null) {
			thisStruct = store.makeNoun(key);
		} else {
			thisStruct = store.getNoun(key);
		}

		int numNouns = allNouns.size();
		for(int nounIdx = 0; nounIdx < numNouns; nounIdx++) {
			Object noun = allNouns.get(nounIdx);
			PkslDataTypes nounType = allNouns.getMeta(nounIdx);
			if(noun instanceof String) {
				NounMetadata value = this.planner.getVariable((String)noun);
				if(value != null) {
					thisStruct.add(value);
				} else {
					thisStruct.add(noun, nounType);
				}
			} else {
				thisStruct.add(noun, nounType);
			}
		}

		// just add this to the parent
		parentReactor.getNounStore().addNoun(key, thisStruct);

		//push up the props
		for(String propKey : this.propStore.keySet()) {
			parentReactor.setProp(propKey, getProp(propKey));
		}
	}
	
	@Override
	public List<NounMetadata> getInputs() {
		// this is used primarily for the planner
		// we do not need to add these steps since 
		// the parent will automatically take these 
		// into consideration
		return null;
	}
	@Override
	public List<NounMetadata> getOutputs() {
		// this is used primarily for the planner
		// we do not need to add these steps since 
		// the parent will automatically take these 
		// into consideration
		return null;
	}
}
