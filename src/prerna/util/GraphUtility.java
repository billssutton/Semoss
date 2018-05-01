package prerna.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import prerna.algorithm.api.SemossDataType;

public class GraphUtility {

	public static HashMap<String, Object> getMetamodel(GraphTraversalSource gts, String graphTypeId) {
		HashMap<String, Object> retMap = new HashMap<String, Object>();
		Map<String, ArrayList<String>> edgeMap = new HashMap<>();
		Map<String, Map<String, String>> nodes = new HashMap<>();
		
		GraphTraversal<Vertex, Map<Object, Object>> gtTest = gts.V().has(graphTypeId).group().by(__.values(graphTypeId));
		// get the types from the specified prop key
		Set<Object> types = null;
		while (gtTest.hasNext()) {
			Map<Object, Object> v = gtTest.next();
			types = v.keySet();
		}
		if (types != null) {
			for (Object t : types) {
				// get the properties for each type
				GraphTraversal<Vertex, String> x = gts.V().has(graphTypeId, t).properties().key().dedup();
				Map<String, String> propMap = new HashMap<>();
				while (x.hasNext()) {
					String nodeProp = x.next();
					// determine data types
					GraphTraversal<Vertex, Object> testType = gts.V().has(graphTypeId, t).has(nodeProp).values(nodeProp);
					int i = 0;
					int limit = 50;
					SemossDataType[] smssTypes = new SemossDataType[limit];
					// might need to default to string
					boolean isString = false;
					boolean next = true;
					while (testType.hasNext() && next) {
						Object value = testType.next();
						Object[] valueType = Utility.findTypes(value.toString());
						SemossDataType smssType = SemossDataType
								.convertStringToDataType(valueType[0].toString().toUpperCase());
						if (smssType == SemossDataType.STRING) {
							isString = true;
							break;
						}
						smssTypes[i] = smssType;
						i++;
						if (i <= limit) {
							if (!testType.hasNext()) {
								next = false;
							}
						}
						if (i == limit) {
							next = false;
						}
					}
					if (isString) {
						propMap.put(nodeProp, SemossDataType.STRING.toString());
					} else {
						SemossDataType defaultType = smssTypes[0];
						boolean useDefault = true;
						// check type array if all types are the same
						for (SemossDataType tempType : smssTypes) {
							if (tempType != null) {
								if (tempType != defaultType) {
									// if different types treat all as String
									propMap.put(nodeProp, SemossDataType.STRING.toString());
									useDefault = false;
									break;
								}
							}
						}
						if (useDefault) {
							propMap.put(nodeProp, defaultType.toString());
						}
					}
				}
				nodes.put(t.toString(), propMap);
			}
		}
		// get edges
		Iterator<Edge> edges = gts.E();
		while (edges.hasNext()) {
			Edge e = edges.next();
			String edgeLabel = e.label();
			Vertex outV = e.outVertex();
			Set<String> outVKeys = outV.keys();
			Vertex inV = e.inVertex();
			Set<String> inVKeys = inV.keys();
			if (outVKeys.contains(graphTypeId) && inVKeys.contains(graphTypeId)) {
				Object outVLabel = outV.value(graphTypeId);
				Object inVLabel = inV.value(graphTypeId);
				if (!edgeMap.containsKey(edgeLabel)) {
					ArrayList<String> vertices = new ArrayList<>();
					vertices.add(outVLabel.toString());
					vertices.add(inVLabel.toString());
					edgeMap.put(edgeLabel, vertices);
				}
			}
		}
		if (!nodes.isEmpty()) {
			retMap.put("nodes", nodes);
		}
		if (!edgeMap.isEmpty()) {
			retMap.put("edges", edgeMap);
		}
		return retMap;

	}
	public static List<String> getAllNodeProperties(GraphTraversalSource gts) {
		ArrayList<String> properties = new ArrayList<>();
		GraphTraversal<Vertex, String> x = gts.V().properties().key().dedup();
		while (x.hasNext()) {
			String prop = x.next();
			properties.add(prop);
		}
		return properties;
	}

}
