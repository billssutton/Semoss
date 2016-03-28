package prerna.ds.H2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import prerna.ds.TinkerFrame;
import prerna.engine.api.IEngine;
import prerna.engine.api.ISelectStatement;
import prerna.engine.api.ISelectWrapper;
import prerna.om.TinkerGraphDataModel;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.ui.components.playsheets.datamakers.DataMakerComponent;
import prerna.util.Constants;

public class TinkerH2Frame extends TinkerFrame {

	private static final Logger LOGGER = LogManager.getLogger(TinkerH2Frame.class.getName());
	H2Builder builder;
	
	public TinkerH2Frame(String[] headers) {
		super(headers);
		builder = new H2Builder();
		builder.create(headers);
	}
	
	public TinkerH2Frame() {
		super();
		builder = new H2Builder();
	}
	
	/*************************** AGGREGATION METHODS *************************/
	
	public void addRow(Map<String, Object> row) {
		String[] newRow = new String[row.keySet().size()];
		
		int i = 0;
		for(String key : row.keySet()) {
			newRow[i] = (String)row.get(key);
			i++;
		}
		
		addRow(newRow);
	}
	
	public void addRow(String[] row) {
		builder.addRow(row);
	}
	
	/************************** END AGGREGATION METHODS **********************/
	
	
	@Override
	public void processDataMakerComponent(DataMakerComponent component) {
		long startTime = System.currentTimeMillis();
        LOGGER.info("Processing Component..................................");
        processPreTransformations(component, component.getPreTrans());
        long time1 = System.currentTimeMillis();
        LOGGER.info("	Processed Pretransformations: " +(time1 - startTime)+" ms");
        
        IEngine engine = component.getEngine();
        // automatically created the query if stored as metamodel
        // fills the query with selected params if required
        // params set in insightcreatrunner
        String query = component.fillQuery();
        
        String[] displayNames = null;
        if(query.trim().toUpperCase().startsWith("CONSTRUCT")){
//     	   TinkerGraphDataModel tgdm = new TinkerGraphDataModel();
//     	   tgdm.fillModel(query, engine, this);
        }
        
        else{
     	   ISelectWrapper wrapper = WrapperManager.getInstance().getSWrapper(engine, query);
            //if component has data from which we can construct a meta model then construct it and merge it
            boolean hasMetaModel = component.getBuilderData() != null;
            if(hasMetaModel) {
         	   
         	   Map<String, Set<String>> edgeHash = component.getBuilderData().getReturnConnectionsHash();
         	   this.mergeEdgeHash(edgeHash);
         	   
         	  builder.processWrapper(wrapper);
            } 
            
            //else default to primary key tinker graph
            else {
                displayNames = wrapper.getDisplayVariables();
         	   this.mergeEdgeHash(this.createPrimKeyEdgeHash(displayNames));
         	   while(wrapper.hasNext()){
         		   this.addRow(wrapper.next());
         	   }
            }
        }
        g.variables().set(Constants.HEADER_NAMES, this.headerNames); // I dont know if i even need this moving forward.. but for now I will assume it is
        redoLevels(this.headerNames);

        long time2 = System.currentTimeMillis();
        LOGGER.info("	Processed Wrapper: " +(time2 - time1)+" ms");
        
        processPostTransformations(component, component.getPostTrans());
        processActions(component, component.getActions());

        long time4 = System.currentTimeMillis();
        LOGGER.info("Component Processed: " +(time4 - startTime)+" ms");
	}
	
	@Override
	public List<Object[]> getData() {
		return builder.getData(getSelectors());
	}
	
	@Override
	public List<Object[]> getRawData() {
		return builder.getData(getSelectors());
	}
	
	
	/****************************** FILTER METHODS **********************************************/
	
	/**
	 * String columnHeader - the column on which to filter on
	 * filterValues - the values that will remain in the 
	 */
	@Override
	public void filter(String columnHeader, List<Object> filterValues) {
		//filterValues is what to keep
		builder.setFilters(columnHeader, filterValues);
	}

	@Override
	public void unfilter(String columnHeader) {
		builder.removeFilter(columnHeader);
	}

	@Override
	public void unfilter() {
		builder.clearFilters();
	}
	
	@Override
	/**
	 * This method returns the filter model for the graph in the form:
	 * 
	 * [
	 * 		{
	 * 			header_1 -> [UF_instance_01, UF_instance_02, ..., UF_instance_0N]
	 * 			header_2 -> [UF_instance_11, UF_instance_12, ..., UF_instance_1N]
	 * 			...
	 * 			header_M -> [UF_instance_M1, UF_instance_M2, ..., UF_instance_MN]
	 * 		}, 
	 * 
	 * 		{
	 * 			header_1 -> [F_instance_01, F_instance_02, ..., F_instance_0N]
	 * 			header_2 -> [F_instance_11, F_instance_12, ..., F_instance_1N]
	 * 			...
	 * 			header_M -> [F_instance_M1, F_instance_M2, ..., F_instance_MN]
	 * 		}	
	 * ]
	 * 
	 * the first object in the array is a Map<String, List<String>> where each header points to the list of UNFILTERED or VISIBLE values for that header
	 * the second object in the array is a Map<String, List<String>> where each header points to the list of FILTERED values for that header
	 */
	public Object[] getFilterModel() {

		
		Iterator<Object[]> iterator = this.iterator(true);
		
		int length = this.headerNames.length;
		
		//initialize the objects
		Map<String, List<Object>> filteredValues = new HashMap<String, List<Object>>(length);
		Map<String, List<Object>> visibleValues = new HashMap<String, List<Object>>(length);
		
		//put instances into sets to remove duplicates
		Set<Object>[] columnSets = new HashSet[length];
		for(int i = 0; i < length; i++) {
			columnSets[i] = new HashSet<Object>(length);
		}
		
		while(iterator.hasNext()) {
			Object[] nextRow = iterator.next();
			for(int i = 0; i < length; i++) {
				columnSets[i].add(nextRow[i]);
			}
		}
		
		//put the visible collected values
		for(int i = 0; i < length; i++) {
			visibleValues.put(headerNames[i], new ArrayList<Object>(columnSets[i]));
			filteredValues.put(headerNames[i], new ArrayList<Object>());
		}
		
		
		
		return new Object[]{visibleValues, builder.getFilteredValues(getSelectors())};
	}

	public Map<String, Object[]> getFilterTransformationValues() {
		Map<String, Object[]> retMap = new HashMap<String, Object[]>();
		// get meta nodes that are tagged as filtered
//		GraphTraversal<Vertex, Vertex> metaGt = g.traversal().V().has(Constants.TYPE, META).has(Constants.FILTER, true);
//		while(metaGt.hasNext()){
//			Vertex metaV = metaGt.next();
//			String vertType = metaV.value(Constants.NAME);
//			GraphTraversal<Vertex, Vertex> gt = g.traversal().V().has(Constants.TYPE, Constants.FILTER).out(Constants.FILTER+edgeLabelDelimeter+vertType).has(Constants.TYPE, vertType);
//			List<String> vertsList = new Vector<String>();
//			while(gt.hasNext()){
//				vertsList.add(gt.next().value(Constants.VALUE));
//			}
//			retMap.put(vertType, vertsList.toArray());
//		}
		
		return retMap;
	}
	
	/****************************** END FILTER METHODS ******************************************/
	
	
	@Override
	public Iterator<Object[]> iterator(boolean getRawData) {
		return builder.buildIterator(getSelectors());
	}
	
	public void applyGroupBy(String column, String newColumnName, String valueColumn, String mathType) {
		builder.processGroupBy(column, newColumnName, valueColumn, mathType);
	}
	
	@Override
	public int getNumRows() {
		Iterator<Object[]> iterator = this.iterator(false);
		int count = 0;
		while(iterator.hasNext()) {
			count++;
			iterator.next();
		}
		return count;
	}
}
