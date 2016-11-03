package prerna.sablecc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import prerna.ds.r.RDataTable;
import prerna.sablecc.expressions.IExpressionSelector;
import prerna.sablecc.expressions.r.builder.RColumnSelector;
import prerna.sablecc.expressions.r.builder.RConstantSelector;
import prerna.sablecc.expressions.r.builder.RExpressionBuilder;
import prerna.sablecc.expressions.r.builder.RExpressionIterator;
import prerna.util.ArrayUtilityMethods;

public class RVizReactor extends AbstractVizReactor{

	@Override
	public Iterator process() {
		RDataTable frame = (RDataTable) getValue("G");
		String[] headers = frame.getColumnHeaders();
		
		Vector<Object> selectors = (Vector<Object>) getValue("VIZ_SELECTOR");
		Vector<String> vizTypes = (Vector<String>) getValue("VIZ_TYPE");
		Vector<String> vizFormula = (Vector<String>) getValue("VIZ_FORMULA");

		if(selectors == null || selectors.size() == 0) {
			// this is the case when user wants a grid of everything
			// we do not send back any data through the pkql
			// they get it in the getNextTableData call
			return null;
		}
		
		List<Map> mergeMaps = new Vector<Map>();
		List<String> mergeVizTypes = new Vector<String>();
		List<String> mergeVizFormula = new Vector<String>();

		RExpressionBuilder mainBuilder = new RExpressionBuilder(frame);
		// we have at least one selector
		int size = selectors.size();
		for(int i = 0; i < size; i++) {
			
			Object term = selectors.get(i);
			if(term instanceof RExpressionBuilder) {
				List<IExpressionSelector> mainGroups = mainBuilder.getGroupBySelectors();
				
				RExpressionBuilder builder = (RExpressionBuilder) term;
				List<IExpressionSelector> builderSelectors = builder.getSelectors();
				List<IExpressionSelector> builderGroups = builder.getGroupBySelectors();
				
				// add all the information together here
				if(mainGroups == null || mainGroups.size() == 0) {
					if(builderGroups != null && builderGroups.size()>0) {
						// we have no selectors to start with
						// just add everything
						for(IExpressionSelector group : builderGroups) {
							mainBuilder.addGroupBy(group);
						}
					}
				} else {
					// we need to compare the values here and make sure they are right
					
					Set<String> groups = new HashSet<String>();
					for(IExpressionSelector group : mainGroups) {
						groups.addAll(group.getTableColumns());
					}
					int startSize = groups.size();
					
					for(IExpressionSelector group : builderGroups) {
						groups.addAll(group.getTableColumns());
					}
					
					int endSize = groups.size();
					
					if(startSize != endSize) {
						throw new IllegalArgumentException("Expression contains group bys that are not the same.  Unable to process.");
					}
				}
				
				for(IExpressionSelector selector : builderSelectors) {
					mainBuilder.addSelector(selector);
				}
				
			} else if(term instanceof Map){
				// store this in a list, will use the old map merge logic on it after
				mergeMaps.add((Map) term);
				mergeVizTypes.add(vizTypes.get(i));
				mergeVizFormula.add(vizFormula.get(i));
				
			} else {
				IExpressionSelector selector = null;
				String val = term.toString().trim();
				if(ArrayUtilityMethods.arrayContainsValue(headers, val)) {
					// we have a header to return
					selector = new RColumnSelector(val);
				} else {
					// we have a constant to return
					selector = new RConstantSelector(val);
				}
				mainBuilder.addSelector(selector);
			}
		}
		
		
		if(mergeMaps.size() == 0) {
			// yay, we can just run the query and return it
			RExpressionIterator it = new RExpressionIterator(mainBuilder);
			List<Object[]> data = new Vector<Object[]>();
			while(it.hasNext()) {
				data.add(it.next());
			}
			
			myStore.put("VizTableKeys", it.getHeaderInformation(vizTypes, vizFormula));
			myStore.put("VizTableValues", data);
		} else {
			// need to merge with existing iterator
			// need to flush iterator into a map :(
			// figure out the columns that were grabbed
			
			// this is all the selectors
			List<IExpressionSelector> querySelectors = mainBuilder.getSelectors();
			int selectorSize = querySelectors.size();
			// get all the headers
			List<String> queryHeaders = new Vector<String>();
			// we need all the selectors that are columns
			List<String> tableCols = new Vector<String>();
			for(int i = 0; i < selectorSize; i++) {
				if(querySelectors.get(i) instanceof RColumnSelector) {
					// while this is an add all
					// it will only return a single value
					tableCols.addAll(querySelectors.get(i).getTableColumns());
				}
				queryHeaders.add(querySelectors.get(i).toString());
			}
			
			RExpressionIterator it = new RExpressionIterator(mainBuilder);
			
			Map<Map<String, Object>, Object> map = convertIteratorDataToMap(it, queryHeaders, tableCols);
			
			// we need to merge some results here
			// consolidate all the terms into a single map
			for(int i = 0; i < mergeMaps.size(); i++) {
				map = mergeMap(map, mergeMaps.get(i));
			}
			
			List<Object[]> data = convertMapToGrid(map, tableCols.toArray(new String[]{}));
			myStore.put("VizTableValues", data);
			
			List<Map<String, Object>> tableKeys = it.getHeaderInformation(vizTypes, vizFormula);
			Vector<Map<String, Object>> mergeTableKeys = (Vector<Map<String, Object>>) getValue("MERGE_HEADER_INFO");
			for(int i = 0; i < mergeMaps.size(); i++) {
				// map to store the info
				Map<String, Object> headMap = mergeTableKeys.get(i);
				headMap.put("vizType", mergeVizTypes.get(i).replace("=", ""));
				
				tableKeys.add(headMap);
			}
			
			myStore.put("VizTableKeys", tableKeys);
		}
			
		return null;
	}
	
}
