package prerna.engine.impl.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prerna.engine.api.IModelEngine;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.Utility;


public class LLMReactor extends AbstractReactor
{
	public LLMReactor()
	{
		this.keysToGet = new String[] {ReactorKeysEnum.ENGINE.getKey(), 
				ReactorKeysEnum.COMMAND.getKey(), 
				ReactorKeysEnum.CONTEXT.getKey(), 
				ReactorKeysEnum.PARAM_VALUES_MAP.getKey()};
		this.keyRequired = new int[] {1, 1, 0, 0};
	}
	
	// execute method - GREEDY translation
	public NounMetadata execute()
	{
		organizeKeys();
		
		String modelId = this.getNounStore().getNoun(keysToGet[0]).get(0) + "";
		String question = this.getNounStore().getNoun(keysToGet[1]).get(0) + "";
		String context = getContext();
		Map paramMap = getMap();
		IModelEngine eng = Utility.getModel(modelId);
		//Map <String, Object> params = new HashMap<String, Object>();
		if(paramMap == null)
			paramMap = new HashMap<String, Object>();
		
		Map<String, String> output = eng.ask(question, context, this.insight, paramMap);
		
		return new NounMetadata(output, PixelDataType.MAP, PixelOperationType.OPERATION);
	}	
	
	private Map<String, Object> getMap() 
	{
        GenRowStruct mapGrs = this.store.getNoun(keysToGet[3]);
        if(mapGrs != null && !mapGrs.isEmpty()) {
            List<NounMetadata> mapInputs = mapGrs.getNounsOfType(PixelDataType.MAP);
            if(mapInputs != null && !mapInputs.isEmpty()) {
                return (Map<String, Object>) mapInputs.get(0).getValue();
            }
        }
        List<NounMetadata> mapInputs = this.curRow.getNounsOfType(PixelDataType.MAP);
        if(mapInputs != null && !mapInputs.isEmpty()) {
            return (Map<String, Object>) mapInputs.get(0).getValue();
        }
        return null;
    }
	
	private String getContext() {
		GenRowStruct contextGrs = this.store.getNoun(keysToGet[2]);
		if(contextGrs != null && !contextGrs.isEmpty()) {
            List<NounMetadata> contextInput = contextGrs.getNounsOfType(PixelDataType.CONST_STRING);
            if(contextInput != null && !contextInput.isEmpty()) {
                return (String) contextInput.get(0).getValue();
            }
        }
		return null;
	}
}