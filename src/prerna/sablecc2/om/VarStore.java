package prerna.sablecc2.om;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import prerna.algorithm.api.ITableDataFrame;
import prerna.engine.api.IRawSelectWrapper;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.IReactor;
import prerna.sablecc2.reactor.storage.MapHeaderDataRowIterator;

public class VarStore implements InMemStore<String, NounMetadata> {

	public static final String PARAM_STRUCT_PREFIX = "$PARAM_STRUCT_";
	
	// the main object where all the nouns are stored
	private Map<String, NounMetadata> varMap;
	
	// for quick searching
	// storing the varNames for all frames
	private Set<String> frameSet;
	
	// for quick searching
	// storing the varnames for all insight parameters
	private Set<String> insightParametersSet;
	
	public VarStore() {
		varMap = new ConcurrentHashMap<>();
		frameSet = new LinkedHashSet<>();
		insightParametersSet = new LinkedHashSet<>();
	}
	
	@Override
	public void put(String varName, NounMetadata variable) {
		varName = cleanVarName(varName);
		if(variable.getNounType() == PixelDataType.COLUMN) {
			if(varName.equals(variable.getValue().toString())) {
				return;
			}
		}
		varMap.put(varName, variable);
		// keep quick reference to frames
		if(variable.getNounType() == PixelDataType.FRAME) {
			frameSet.add(varName);
		} else if(variable.getNounType() == PixelDataType.PARAM_STRUCT) {
			insightParametersSet.add(varName);
		}
	}
	
	public void putAll(VarStore otherStore) {
		varMap.putAll(otherStore.varMap);
		frameSet.addAll(otherStore.frameSet);
		insightParametersSet.addAll(otherStore.insightParametersSet);
	}
	
	@Override
	public NounMetadata get(String varName) {
		varName = cleanVarName(varName);
		return varMap.get(varName);
	}
	
	@Override
	public NounMetadata getEvaluatedValue(String varName) {
		varName = cleanVarName(varName);
		NounMetadata valueNoun = varMap.get(varName);
		if(valueNoun != null) {
			PixelDataType valType = valueNoun.getNounType();
			if(valType == PixelDataType.COLUMN) {
				String valName = valueNoun.getValue().toString();
				// got to make sure it is not a variable
				// pointing to another variable
				if(containsKey(valName)) {
					return getEvaluatedValue(valName);
				}
			}
			else if(valType == PixelDataType.LAMBDA) {
				NounMetadata retNoun = ((IReactor) valueNoun.getValue()).execute();
				return retNoun;
			}
		}
		// once we are done with the whole recursive
		// part above, just return the noun
		return valueNoun;
	}
	
	@Override
	public boolean containsKey(String varName) {
		varName = cleanVarName(varName);
		return varMap.containsKey(varName);
	}
	
	@Override
	public NounMetadata remove(String varName) {
		// also try to remove from frameSet if its a frame
		this.frameSet.remove(varName);
		this.insightParametersSet.remove(varName);
		return varMap.remove(varName);
	}
	
	/**
	 * Remove all keys 
	 * @param keys
	 */
	public void removeAll(Collection<String> keys) {
		// also try to remove from frameSet if its a frame
		this.frameSet.removeAll(keys);
		this.insightParametersSet.removeAll(keys);
		this.varMap.keySet().removeAll(keys);
	}
	
	@Override
	public void clear() {
		this.frameSet.clear();
		this.insightParametersSet.clear();
		this.varMap.clear();
	}
	
	@Override
	public IRawSelectWrapper getIterator() {
		return new MapHeaderDataRowIterator(this);
	}

	@Override
	public IRawSelectWrapper getIterator(SelectQueryStruct qs) {
		//TODO: figure out how to use a qs with this
		return new MapHeaderDataRowIterator(this);
	}

	@Override
	public Set<String> getKeys() {
		return varMap.keySet();
	}
	
	public Set<String> getFrameKeys() {
		return frameSet;
	}
	
	/**
	 * Get all the references for a specific frame
	 * @param frame
	 * @return
	 */
	public Set<String> findAllVarReferencesForFrame(ITableDataFrame frame) {
		Set<String> referenceSet = new HashSet<>();
		for(String frameKey : frameSet) {
			NounMetadata possibleFrameVar = this.varMap.get(frameKey);
			if(possibleFrameVar.getValue() == frame) {
				referenceSet.add(frameKey);
			}
		}
		return referenceSet;
	}
	
	public Set<String> getInsightParameterKeys() {
		return insightParametersSet;
	}
	
	/**
	 * Used to get all keys that point to the same object
	 * @param obj
	 */
	public Set<String> getAllAliasForObjectReference(Object obj) {
		Set<String> alias = new HashSet<String>();
		for(String key : varMap.keySet()) {
			NounMetadata noun = varMap.get(key);
			if(noun.getValue() == obj) {
				alias.add(key);
			}
		}
		return alias;
	}
	
	private String cleanVarName(String varName) {
		return varName.trim();
	}
	
	/**
	 * Pull the insight parameters
	 * @return
	 */
	public Map<String, NounMetadata> pullParameters() {
		Map<String, NounMetadata> retMap = new HashMap<>();
		for(String paramKey : this.insightParametersSet) {
			retMap.put(paramKey, this.varMap.get(paramKey));
		}
		
		return retMap;
	}
	
}
