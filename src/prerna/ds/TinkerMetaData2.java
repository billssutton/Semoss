package prerna.ds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import prerna.algorithm.api.IMetaData;
import prerna.engine.api.IEngine;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class TinkerMetaData2 implements IMetaData {

	private static final Logger LOGGER = LogManager.getLogger(TinkerMetaData2.class.getName());

	protected GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
	protected TinkerGraph g = null;

	public static final String ALIAS = "ALIAS";
	public static final String DB_NAME = "DB_NAME";
	public static final String ALIAS_TYPE = "ALIAS_TYPE";
	public static final String PHYSICAL_NAME = "PHYSICAL_NAME";
	public static final String PHYSICAL_URI = "PHYSICAL_URI";
	public static final String LOGICAL_NAME = "LOGICAL_NAME";
	public static final String PARENT = "PARENT";
	public static final String DATATYPE = "DATATYPE";
	
	public static final String PRIM_KEY = "PRIM_KEY";
	public static final String META = "META";
	public static final String edgeLabelDelimeter = "+++";
	
	/**
	 * CURRENT PROPERTY STRUCTURE::::::::::::::::::
	 * 		Key	Value	Description
			TYPE	META	Has type META to differentiate from instance nodes
			NAME	System_1	What the unique name for this column is
			VALUE	System	This aligns with the TYPE for the instance nodes
			ALIAS	[System_1, System, Application, etc]	This is a list for all different aliases that have been given to this node (can be user defined, db physical name, db logical name, etc)
			DERIVED	m:Sum(�)	String representing how to calc if it is derived (PKQL?) IF IT IS DERIVED
			PROPERTY	Interface	States what node (unique name) this node is a property of (IF IT IS A PROPERTY)
			System_1	"{
				Type: DB Logical Name or DB Physical Name or User Defined, etc
				DB: [TAP_Core, TAP_Site]
				}"	Holds all meta data around that alias -- where did it come from etc.
			System	"{
				Type: DB Logical Name or DB Physical Name or User Defined, etc
				DB: [TAP_Core, TAP_Site]
				}"	
			Application	"{
				Type: DB Logical Name or DB Physical Name or User Defined, etc
				DB: [TAP_Core, TAP_Site]
				}"	
			DATABASE	[TAP_Core, TAP_Site, etc]	List for all different databases that contributed to this column
			TAP_Core	"{
				Physical Name: System
				Physical URI: http://semoss.org/ontologies/Concept/System
				Logical Name: System
				Query Struct: ???
				Stringified OWL: ???
				}"	Holds all meta data around that database
			TAP_Site	"{
				Physical Name: System
				Physical URI: http://semoss.org/ontologies/Concept/System
				Logical Name: System
				Query Struct: ???
				Stringified OWL: ???
				}"	

	 * @param g
	 */
	public TinkerMetaData2(TinkerGraph g) {
		this.g = g;
	}
	
	public TinkerMetaData2() {
		TinkerGraph g = TinkerGraph.open();
		g.createIndex(Constants.TYPE, Vertex.class);
		g.createIndex(Constants.ID, Vertex.class);
		g.createIndex(T.label.toString(), Edge.class);
		g.createIndex(Constants.ID, Edge.class);
		this.g = g;
	}
	
	/*
	 * Return's nodes Contrant.Value/Physical logical name from the inputted physical name
	 */
	@Override
	public String getValueForUniqueName (String metaNodeName) {
		String metaNodeValue = null;
		// get metamodel info for metaModeName
		GraphTraversal<Vertex, Vertex> metaT = g.traversal().V().has(Constants.TYPE, TinkerFrame.META).has(Constants.NAME, metaNodeName);
		
		// if metaT has metaNodeName then find the value else return metaNodeName
		if (metaT.hasNext()) {
			Vertex startNode = metaT.next();
			metaNodeValue = startNode.property(Constants.VALUE).value() + "";
		}

		return metaNodeValue;
	}
	
	/**
	 * 
	 * @param vert
	 * @param engineName
	 * @param logicalName
	 * @param instancesType
	 * @param physicalUri
	 * @param dataType
	 */
	public void addEngineMeta(Vertex vert, String engineName, String logicalName, String instancesType, String physicalUri, String dataType) {
		
		// now add the meta object if it doesn't already exist
		Map<Object, Object> metaData = null;
		if(!vert.property(engineName).isPresent()){
			metaData = new HashMap<Object, Object>();
			vert.property(engineName, metaData);
		}
		else{
			metaData = vert.value(engineName);
		}
		
		metaData.put(IMetaData.NAME_TYPE.DB_LOGICAL_NAME, logicalName);
		metaData.put(IMetaData.NAME_TYPE.DB_PHYSICAL_NAME, instancesType);
		metaData.put(IMetaData.NAME_TYPE.DB_PHYSICAL_URI, physicalUri);
		
		if(dataType != null) {
			metaData.put(DATATYPE, dataType);
		}
	}
	
	@Override
	public void addDataType(String uniqueName, String dataType) {
		Vertex vert = getExistingVertex(uniqueName);
		if(dataType == null || dataType.isEmpty()) {
			return;
		}
		
		dataType = dataType.toUpperCase();
		
		String currType = null;
		if(vert.property(DATATYPE).isPresent()){
			currType = vert.value(DATATYPE);
		}
		
		if(currType == null) {
			if(dataType.contains("STRING") || dataType.contains("TEXT") || dataType.contains("VARCHAR")) {
				vert.property(DATATYPE, "STRING");
			} 
			else if(dataType.contains("INT") || dataType.contains("DECIMAL") || dataType.contains("DOUBLE") || dataType.contains("FLOAT") || dataType.contains("LONG") || dataType.contains("BIGINT")
					|| dataType.contains("TINYINT") || dataType.contains("SMALLINT") || dataType.contains("NUMBER")){
				vert.property(DATATYPE, "NUMBER");
			} 
			else if(dataType.contains("DATE")) {
				vert.property(DATATYPE, "DATE");
			}
		} else {
			// if current is string or new col is string
			// column must now be a string
			if(currType.contains("STRING") || dataType.contains("STRING") || dataType.contains("TEXT") || dataType.contains("VARCHAR")) {
				vert.property(DATATYPE, "STRING");
			}
			// if current is a number and new is a number
			// column is still number
			else if(currType.equals("NUMBER") && ( dataType.contains("INT") || dataType.contains("DECIMAL") || dataType.contains("DOUBLE") || dataType.contains("FLOAT") || dataType.contains("LONG") || dataType.contains("BIGINT")
					|| dataType.contains("TINYINT") || dataType.contains("SMALLINT") )){
				// no change
				// vert.property(DATATYPE, "NUMBER");
			}
			// if current is date and new is date
			// column is still date
			else if(currType.equals("DATE") && dataType.contains("DATE")) {
				// no change
				// vert.property(DATATYPE, "DATE");
			}
			// any other situation, you have mixed types or numbers and dates... declare it a string for now //TODO
			else {
				vert.property(DATATYPE, "STRING");
			}
			
		}
		
	}
	
	/**
	 * 
	 * @param vert
	 * @param propKey - property to add to
	 * @param newValue - value to add to propkey
	 * @param newVal - optional additional values to add to propKey
	 */
	private void addToMultiProperty(Vertex vert, String propKey, String newValue, String... newVal){
//		printNode(vert);
		vert.property(VertexProperty.Cardinality.set, propKey, newValue);
		for(String val : newVal){
			vert.property(VertexProperty.Cardinality.set, propKey, val);
		}
//		printNode(vert);
	}
	
	/**
	 * 
	 * @param vert - meta vertex to modify
	 * @param name - alias being added
	 * @param type - how the alias is defined e.g. DB_LOGICAL
	 * @param engineName - if db defined name, what db defined it
	 */
	private void addAliasMeta(Vertex vert, String name, NAME_TYPE type, String engineName){
		// now add the meta object if it doesn't already exist
		Map<String, Object> metaData = null;
		if(!vert.property(name).isPresent()){
			metaData = new HashMap<String, Object>();
			metaData.put(DB_NAME, new Vector<String>());
			metaData.put(ALIAS_TYPE, new Vector<IMetaData.NAME_TYPE>());
			vert.property(name, metaData);
		}
		else{
			metaData = vert.value(name);
		}
		
		if(engineName!=null){
			((Vector<String>)metaData.get(DB_NAME)).add(engineName);
		}
		
		((Vector<IMetaData.NAME_TYPE>)metaData.get(ALIAS_TYPE)).add(type);
	}

	// create or add vertex
	private Vertex upsertVertex(Object uniqueName, Object howItsCalledInDataFrame)
	{
		String type = META;
		// checks to see if the vertex is there already
		// if so retrieves the vertex
		// if not inserts the vertex and then returns that vertex
		Vertex retVertex = getExistingVertex(uniqueName);
		// if we were unable to get the existing vertex... time to create a new one
		if (retVertex == null){
			LOGGER.debug(" adding vertex ::: " + Constants.ID + " = " + type + ":" + uniqueName+ " & " + Constants.VALUE+ " = " + howItsCalledInDataFrame+ " & " + Constants.TYPE+ " = " + type+ " & " + Constants.NAME+ " = " + uniqueName);
			retVertex = g.addVertex(Constants.ID, type + ":" + uniqueName, Constants.VALUE, howItsCalledInDataFrame, Constants.TYPE, type, Constants.NAME, uniqueName);// push the actual value as well who knows when you would need it
			// all new meta nodes are defaulted as unfiltered and not prim keys
			retVertex.property(Constants.FILTER, false);
			retVertex.property(PRIM_KEY, false);
		}
		return retVertex;
	}

	private Edge upsertEdge(Vertex fromVertex, Vertex toVertex)
	{
		Edge retEdge = null;
		String type = META + edgeLabelDelimeter + META;
		String edgeID = type + "/" + fromVertex.value(Constants.NAME) + ":" + toVertex.value(Constants.NAME);
		// try to find the vertex
		GraphTraversal<Edge, Edge> gt = g.traversal().E().has(Constants.ID, edgeID);
		if(gt.hasNext()) {
			retEdge = gt.next(); // COUNTS HAVE NO MEANING IN META. REMOVED.
		}
		else {
			retEdge = fromVertex.addEdge(type, toVertex, Constants.ID, edgeID);
		}

		return retEdge;
	}
	
	private Vertex getExistingVertex(Object uniqueName){
		Vertex retVertex = null;
		// try to find the vertex
		GraphTraversal<Vertex, Vertex> gt = g.traversal().V().has(Constants.ID, META + ":" + uniqueName);
		if(gt.hasNext()) {
			retVertex = gt.next();
		}
		return retVertex;
	}

	public Map<String, String> getNodeTypesForUniqueAlias() {
		GraphTraversal<Vertex, Vertex> gt = g.traversal().V().has(Constants.TYPE, META);
		Map<String, String> retMap = new Hashtable<String, String>();
		while(gt.hasNext()) {
			Vertex vert = gt.next();
			String uniqueName = vert.value(Constants.NAME);
			if(vert.property(DATATYPE).isPresent()) {
				String type = vert.value(DATATYPE);
				retMap.put(uniqueName, type);
			} else {
				retMap.put(uniqueName, "TYPE NOT STORED IN OWL, NEED TO UPDATE DB");
			}
		}
		
		return retMap;
	}

	@Override
	public Map<String, String> getProperties() {
		Map<String, String> retMap = new HashMap<String, String>();
		GraphTraversal<Vertex, Vertex> trav = g.traversal().V().has(Constants.TYPE, META).has(PARENT);
		while (trav.hasNext()){
			Vertex vert = trav.next();
			String prop = vert.property(Constants.NAME).value() +"";
			String parent = vert.property(PARENT).value()+"";
			retMap.put(prop, parent);
		}
		return retMap;
	}


	@Override
	public String getPhysicalUriForNode(String nodeUniqueName, String engineName) {
		GraphTraversal<Vertex, Vertex> trav = g.traversal().V().has(Constants.TYPE, META).has(Constants.NAME, nodeUniqueName);
		if(trav.hasNext()){
			Vertex node = trav.next();
			Map<Object, Object> engineMap = (Map<Object, Object>) node.property(engineName).value();
			String physUri = (String) engineMap.get(IMetaData.NAME_TYPE.DB_PHYSICAL_URI);
			return physUri;
		}
		else return null;
	}

	@Override
	public Set<String> getEnginesForUniqueName(String nodeUniqueName) {
		GraphTraversal<Vertex, Vertex> trav = g.traversal().V().has(Constants.TYPE, META).has(Constants.NAME, nodeUniqueName);
		if(trav.hasNext()){
			Vertex node = trav.next();
			Iterator<VertexProperty<Object>> dbSet = node.properties(DB_NAME);
			Set<String> engines = new HashSet<String>();
			while(dbSet.hasNext()){
				engines.add(dbSet.next().value().toString());
			}
			return engines;
		}
		else return null;
	}
	
	
	private void printNode(Vertex v){
		System.out.println(v.toString());
		Iterator<VertexProperty<Object>> props = v.properties();
		while(props.hasNext()){
			VertexProperty<Object> prop = props.next();
			Iterator<Object> vals = prop.values();
			while(vals.hasNext()){
				System.out.println(vals.next());
			}
		}
	}

//	@Override
//	public void storeVertex(Object uniqueName, Object howItsCalledInDataFrame){
//		upsertVertex(uniqueName, howItsCalledInDataFrame);
//	}
//
//	@Override
//	public void storeProperty(Object uniqueName, Object howItsCalledInDataFrame, Object parentUniqueName){
//		Vertex vert = upsertVertex(uniqueName, howItsCalledInDataFrame);
//		vert.property(PARENT, parentUniqueName);
//	}

	@Override
	public void storeRelation(String uniqueName1, String uniqueName2) {
		Vertex outVert = getExistingVertex(uniqueName1);
		Vertex inVert = getExistingVertex(uniqueName2);
		upsertEdge(outVert, inVert);
	}

	@Override
	public void storeUserDefinedAlias(String uniqueName, String aliasName) {
		Vertex vert = getExistingVertex(uniqueName);
		addToMultiProperty(vert, ALIAS, aliasName);
		addAliasMeta(vert, aliasName, NAME_TYPE.USER_DEFINED, null);
	}

	@Override
	public void storeEngineDefinedVertex(String uniqueName, String uniqueParentNameIfProperty, String engineName, String queryStructName) {
		
		//get the rest of the needed information off the owl
		IEngine engine = (IEngine) DIHelper.getInstance().getLocalProp(engineName);
		
		String physicalName = queryStructName;
		String physicalUri = null;
		//check if property
		if(physicalName.contains("__")){
			physicalName = physicalName.substring(physicalName.indexOf("__")+2);
			physicalUri = Constants.PROPERTY_URI + physicalName;
		}
		else{
			physicalUri = engine.getConceptUri4PhysicalName(queryStructName);
		}
		
		String logicalName = Utility.getInstanceName(engine.getTransformedNodeName(physicalUri, true));
//		String physicalName = Utility.getInstanceName(physicalUri);
		String dataType = engine.getDataTypes(physicalUri);

		Vertex vert = upsertVertex(uniqueName, logicalName);
		addToMultiProperty(vert, DB_NAME, engineName);
		
		//store it to the vertex
		addEngineMeta(vert, engineName, logicalName, physicalName, physicalUri, dataType);
		
		// add all of that information in terms of aliases as well
		String[] curAl = new String[]{physicalName, physicalUri};
		addToMultiProperty(vert, ALIAS, logicalName, curAl);
		
		addAliasMeta(vert, logicalName, NAME_TYPE.DB_LOGICAL_NAME, engineName);
		addAliasMeta(vert, physicalName, NAME_TYPE.DB_PHYSICAL_NAME, engineName);
		addAliasMeta(vert, queryStructName, NAME_TYPE.DB_QUERY_STRUCT_NAME, engineName);
		addAliasMeta(vert, physicalUri, NAME_TYPE.DB_PHYSICAL_URI, engineName);
		
		if(uniqueParentNameIfProperty != null){
			vert.property(PARENT, uniqueParentNameIfProperty);
		}
	}

	@Override
	public void storeDataType(String uniqueName, String dataType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFiltered(String uniqueName, boolean filtered) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPrimKey(String uniqueName, boolean primKey) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
//////////////////::::::::::::::::::::::: GETTER METHODS :::::::::::::::::::::::::::::::://////////////////////////////

	@Override
	public Set<String> getAlias(String uniqueName) {
		Vertex vert = getExistingVertex(uniqueName);
		if(vert.properties(ALIAS).hasNext()) {
			Set<String> aliasSet = new TreeSet<String>();
			Iterator<VertexProperty<Object>> props = vert.properties(ALIAS);
			while(props.hasNext()) {
				aliasSet.add(props.next().value() + "");
			}
			return aliasSet;
		}
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> getAliasMetaData(String uniqueName) {
		Vertex vert = getExistingVertex(uniqueName);
		if(vert.properties(ALIAS).hasNext()) {
			Map<String, Map<String, Object>> aliasMetaData = new TreeMap<String, Map<String, Object>>();
			Iterator<VertexProperty<Object>> props = vert.properties(ALIAS);
			while(props.hasNext()) {
				String prop = props.next().value() + "";
				if(vert.property(prop).isPresent()) {
					Map<String, Object> metaData = vert.value(prop);
					aliasMetaData.put(prop, metaData);
				}
			}
			return aliasMetaData;
		}
		return null;
	}

	@Override
	public String getDataType(String uniqueName) {
		Vertex vert = getExistingVertex(uniqueName);
		if(vert.property(DATATYPE).isPresent()) {
			return vert.value(DATATYPE);
		}
		return "STRING";
	}

	@Override
	public boolean isFiltered(String uniqueName) {
		Vertex vert = getExistingVertex(uniqueName);
		if(vert.property(Constants.FILTER).isPresent()) {
			return vert.value(Constants.FILTER);
		}
		return false;
	}

	@Override
	public boolean isPrimKey(String uniqueName) {
		Vertex vert = getExistingVertex(uniqueName);
		if(vert.property(PRIM_KEY).isPresent()) {
			return vert.value(PRIM_KEY);
		}
		return false;
	}
	
	public QueryStruct getQueryStruct(String startName) {
		QueryStruct qs = new QueryStruct();
		List<String> travelledEdges = new Vector<String>();

		Vertex startVert = getExistingVertex(startName);
		if(startVert != null) {
			visitNode(startVert, travelledEdges, qs);
			return qs;
		}
		return null;
	}
	
	public void visitNode(Vertex vert, List<String> travelledEdges, QueryStruct qs) {
		String origName = vert.value(Constants.NAME);
		String vertParent = null;
		if(vert.property(PARENT).isPresent()) {
			vertParent = vert.property(PARENT).value() + "";
		}

		GraphTraversal<Vertex, Vertex> downstreamIt = g.traversal().V().has(Constants.TYPE, TinkerFrame.META).has(Constants.ID, vert.property(Constants.ID).value()).out(TinkerFrame.META + TinkerFrame.edgeLabelDelimeter + TinkerFrame.META);
		while (downstreamIt.hasNext()) {
			Vertex nodeV = downstreamIt.next();
			if(nodeV.property(PRIM_KEY).isPresent()) {
				if((boolean) nodeV.property(PRIM_KEY).value()) {
					visitNode(nodeV, travelledEdges, qs);
					continue;
				}
			}
			String nameNode = nodeV.property(Constants.NAME).value() + "";
			
			String edgeKey = origName + TinkerFrame.edgeLabelDelimeter + nameNode;
			if(!travelledEdges.contains(edgeKey)) {
				travelledEdges.add(edgeKey);
				
				String nodeVParent = null;
				if(nodeV.property(PARENT).isPresent()) {
					nodeVParent = nodeV.property(PARENT).value() + "";
				}
				
				if(vert.value(Constants.NAME).equals(nodeVParent)) {
					qs.addSelector(vert.property(Constants.VALUE).value() + "", nodeV.property(Constants.VALUE).value() + "");
				} else if(nodeV.value(Constants.NAME).equals(vertParent)) {
					qs.addSelector(nodeV.property(Constants.VALUE).value() + "", vert.property(Constants.VALUE).value() + "");
				} else {
//					qs.addSelector(nodeVParent, nodeV.property(Constants.VALUE).value() + "");
//					qs.addSelector(vertParent, vert.property(Constants.VALUE).value() + "");
					qs.addRelation(vert.property(Constants.VALUE).value() + "", nodeV.property(Constants.VALUE).value() + "", "inner.join");
				}
				visitNode(nodeV, travelledEdges, qs);
			}
		}

		GraphTraversal<Vertex, Vertex> upstreamIt = g.traversal().V().has(Constants.TYPE, TinkerFrame.META).has(Constants.ID, vert.property(Constants.ID).value()).in(TinkerFrame.META+TinkerFrame.edgeLabelDelimeter+TinkerFrame.META);
		while(upstreamIt.hasNext()) {
			Vertex nodeV = upstreamIt.next();
			if(nodeV.property(PRIM_KEY).isPresent()) {
				if((boolean) nodeV.property(PRIM_KEY).value()) {
					visitNode(nodeV, travelledEdges, qs);
					continue;
				}
			}
			String nameNode = nodeV.property(Constants.NAME).value() + "";
			
			String edgeKey = nameNode + TinkerFrame.edgeLabelDelimeter + origName;
			if (!travelledEdges.contains(edgeKey)) {
				travelledEdges.add(edgeKey);
				
				String nodeVParent = null;
				if(nodeV.property(PARENT).isPresent()) {
					nodeVParent = nodeV.property(PARENT).value() + "";
				}
				if(vert.value(Constants.NAME).equals(nodeVParent)) {
					qs.addSelector(vert.property(Constants.VALUE).value() + "", nodeV.property(Constants.VALUE).value() + "");
				} else if(nodeV.value(Constants.NAME).equals(vertParent)) {
					qs.addSelector(nodeV.property(Constants.VALUE).value() + "", vert.property(Constants.VALUE).value() + "");
				} else {
//					qs.addSelector(nodeVParent, nodeV.property(Constants.VALUE).value() + "");
//					qs.addSelector(vertParent, vert.property(Constants.VALUE).value() + "");
					qs.addRelation(nodeV.property(Constants.VALUE).value() + "", vert.property(Constants.VALUE).value() + "", "inner.join");
				}
			}
		}
	}
	
	public List<String> getUniqueNames(){
		List<String> uniqueList = new Vector<String>();
		GraphTraversal<Vertex, Object> trav = g.traversal().V().has(Constants.TYPE, META).values(Constants.NAME);
		while(trav.hasNext()){
			uniqueList.add(trav.next().toString());
		}
		return uniqueList;
	}

	/**
	 * edgeHash is all query struct names of things getting added this go
	 * e.g.
	 * {
	 * 		Title -> [Title__Budget, Studio]
	 * }
	 * 
	 * joins is list of all joins getting added this go
	 * e.g.
	 * {
	 * 		Title -> Title
	 * 		System -> System_1
	 * }
	 * 
	 * return is the unique name and unique parent name (if property) to be associated with each physical name in edgeHash
	 * e.g.
	 * {
	 * 		System -> [System_1, null]
	 * 		Budget -> [MovieBudget, Title_1]
	 * }
	 */
	@Override
	public Map<String, String[]> getPhysical2LogicalTranslations(Map<String, Set<String>> edgeHash,
			List<Map<String, String>> joins) {
		Map<String, String[]> retMap = new HashMap<String, String[]>();
		
		List<String> uniqueNames = getUniqueNames();
		
		// create master set
		// master set is all of the query struct names of the things getting added this go
		Set<String> masterSet = new HashSet<String>();
		masterSet.addAll(edgeHash.keySet());
		for(Set<String> edgeHashSet : edgeHash.values()){
			masterSet.addAll(edgeHashSet);
		}
		
		// first go through just for the concepts
		for(String key: masterSet){
			String myParentsUniqueName = getUniqueName(key.contains("__")? key.substring(0, key.indexOf("__")): null, uniqueNames, joins);
			String myUniqueName = getUniqueName(key.contains("__")? key.substring(key.indexOf("__")+2): key, uniqueNames, joins);
            retMap.put(key, new String[]{myUniqueName, myParentsUniqueName});
		}
		return retMap;
	}
	
	private String getUniqueName(String name, List<String> uniqueNames, List<Map<String, String>> joins){
		if (name == null) return null;
		for(Map<String, String> join: joins) {
			if(join.containsKey(name)){
				return join.get(name);
			}
		}
		String correctName = name;
		int counter = 1;
        while (uniqueNames.contains(correctName)) {
        	correctName = name + "_" + counter;
            counter++;
        }
        return correctName;
	}

	@Override
	public String getLogicalNameForUniqueName(String uniqueName, String engineName) {
		Vertex vert = getExistingVertex(uniqueName);
		Map<Object, Object> engineProps = (Map<Object, Object>) vert.property(engineName).value();
		return engineProps.get(NAME_TYPE.DB_LOGICAL_NAME) + "";
	}

	@Override
	public void storeVertex(String uniqueName, String howItsCalledInDataFrame, String uniqueParentNameIfProperty) {
		Vertex vertex = this.upsertVertex(uniqueName, howItsCalledInDataFrame);
		if(uniqueParentNameIfProperty != null && !uniqueParentNameIfProperty.isEmpty()) {
			vertex.property(PARENT, uniqueParentNameIfProperty);
		}
	}
	
	@Override
	public Map<String, Set<String>> getEdgeHash() {
		Map<String, Set<String>> retMap = new HashMap<String, Set<String>>();
		GraphTraversal<Vertex, Vertex> metaT = g.traversal().V().has(Constants.TYPE, TinkerFrame.META);
		while(metaT.hasNext()) {
			Vertex startNode = metaT.next();
			String startType = startNode.property(Constants.NAME).value()+"";
			Iterator<Vertex> downNodes = startNode.vertices(Direction.OUT);
			Set<String> downSet = new HashSet<String>();
			while(downNodes.hasNext()){
				Vertex downNode = downNodes.next();
				String downType = downNode.property(Constants.NAME).value()+"";
				downSet.add(downType);
			}
			retMap.put(startType, downSet);
		}
		return retMap;
	}

	
	
//	public List<String> getSelectors(String aliasKey) {
//		GraphTraversal<Vertex, Vertex> traversal = g.traversal().V().has(Constants.TYPE, META);
//		List<String> selectors = new ArrayList<String>();
//		while(traversal.hasNext()) {
//			Vertex nextVert = traversal.next();
//			
//			//if nextVert not a prim key
//			if(!nextVert.value(Constants.NAME).equals(PRIM_KEY)) {
//				selectors.add(nextVert.value(aliasKey));
//			}
//		}
//		return selectors;
//	}
	
//////////////////::::::::::::::::::::::: TESTING :::::::::::::::::::::::::::::::://////////////////////////////

	public static void main(String[] args) {
		TinkerMetaData2 meta = new TinkerMetaData2();

		Vertex pkV = meta.upsertVertex("TABLE1","TABLE1");
		pkV.property(PRIM_KEY, true);

		Vertex v1 = meta.upsertVertex("TITLE","TITLE");
		v1.property(PARENT, "TABLE1");
		meta.upsertEdge(pkV, v1);

		Vertex v2 = meta.upsertVertex("STUDIO","STUDIO");
		v2.property(PARENT, "TABLE1");
		meta.upsertEdge(pkV, v2);
		
		Vertex v3 = meta.upsertVertex("DIRECTOR","DIRECTOR");
		v3.property(PARENT, "TABLE1");
		meta.upsertEdge(pkV, v3);
		
		// new sheet
		
		pkV = meta.upsertVertex("TABLE2","TABLE2");
		pkV.property(PRIM_KEY, true);

		Vertex newv1 = meta.upsertVertex("TABLE2__TITLE","TABLE2__TITLE");
		newv1.property(PARENT, "TABLE2");
		meta.upsertEdge(pkV, newv1);
		meta.upsertEdge(v1, newv1);

		v2 = meta.upsertVertex("BUDGET","BUDGET");
		v2.property(PARENT, "TABLE2");
		meta.upsertEdge(pkV, v2);
		
		v3 = meta.upsertVertex("REVENUE","REVENUE");
		v3.property(PARENT, "TABLE2");
		meta.upsertEdge(pkV, v3);

		QueryStruct qs = meta.getQueryStruct("DIRECTOR");
		qs.print();
	}

}
