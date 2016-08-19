package prerna.sablecc;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import prerna.algorithm.api.ITableDataFrame;
import prerna.engine.api.IEngine;
import prerna.util.DIHelper;

public class NativeImportDataReactor extends ImportDataReactor {

	public Iterator process() {
		
		String query = "";
		
		if(myStore.containsKey(PKQLEnum.API)) {	
			ITableDataFrame frame = null;
			Map<String, Set<String>> edgeHash = (Map<String, Set<String>>) this.getValue(PKQLEnum.API + "_EDGE_HASH");
			IEngine engine = (IEngine) DIHelper.getInstance().getLocalProp((this.getValue(PKQLEnum.API + "_ENGINE")+"").trim());
			query  = (String) myStore.get(PKQLEnum.API);

			// 4 & 5) if engine is not null, merge the data into the frame
			// the engine is null when the api is actually a csv file
			if(engine != null) {
				// put the edge hash and the logicalToValue maps within the myStore
				// will be used when the data is actually imported
//				Map[] mergedMaps = frame.mergeQSEdgeHash(edgeHash, engine, joinCols);
//				myStore.put("edgeHash", mergedMaps[0]);
//				myStore.put("logicalToValue", mergedMaps[1]);
			} 
		}
		return null;
	}
}
