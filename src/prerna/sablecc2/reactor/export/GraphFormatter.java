package prerna.sablecc2.reactor.export;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import prerna.engine.api.IHeadersDataRow;
import prerna.ui.helpers.TypeColorShapeTable;
import prerna.util.ArrayUtilityMethods;

public class GraphFormatter extends AbstractFormatter {

	// the nodes list to return
	private List<Map<String, Object>> nodesMapList;
	// the edges list to return
	private List<Map<String, Object>> edgesMapList;

	/*
	 * These are the options that the FE can define for graph output
	 * 1) connectionsMap
	 * 		Example : {upstreamNode1 -> [downstreamNode1 , downstreamnode2, .. etc]
	 * 		This defines the up node to its list of downnodes
	 * 		The same node can be an up node in some situations and down nodes in orders
	 * 
	 * 2) nodePropertiesMap
	 * 		Example : {node -> [prop1, prop2, ... propN]
	 * 		This defines the properties to add into the prop hash for a given node
	 * 
	 * 3) edgePropertiesMap
	 * 		Example: {upNode1.downNode1 -> [prop1, prop2, ... propN]
	 * 		This defines the properties to put inbetween a given relationship that 
	 * 		must already be defined in teh connectionsMap
	 * 
	 */
	private Map<String, List<String>> connectionsMap;
	private Map<String, List<String>> nodePropertiesMap;
	private Map<String, List<String>> edgePropertiesMap;

	// this is used to make sure we do not add vertices twice
	protected Map<String, Set<String>> vertLabelUniqueValues;

	private List<Integer[]> indexConnections;

	// used for edge map
	private static final String EDGES = "edges";
	private static final String SOURCE = "source";
	private static final String TARGET = "target";

	// used for node map
	private static final String NODES = "nodes";
	private static final String VERTEX_TYPE_PROPERTY = "VERTEX_TYPE_PROPERTY";
	private static final String VERTEX_COLOR_PROPERTY = "VERTEX_COLOR_PROPERTY";
	private static final String VERTEX_LABEL_PROPERTY = "VERTEX_LABEL_PROPERTY";

	private static final String URI = "uri";
	private static final String PROP_HASH = "propHash";
	private static final String GRAPH_META = "graphMeta";

	public GraphFormatter() {
		this.nodesMapList = new ArrayList<Map<String, Object>>();
		this.edgesMapList = new ArrayList<Map<String, Object>>();
		this.vertLabelUniqueValues = new HashMap<String, Set<String>>();
	}

	@Override
	public void addData(IHeadersDataRow nextData) {
		String[] headers = nextData.getHeaders();
		// also get the raw headers and see if you can try it
		String[] rawHeaders = nextData.getRawHeaders();
		Object[] values = nextData.getValues();
		if (this.indexConnections == null) {
			determineConnectionsIndex(headers, rawHeaders);
		}

		// process the nodes
		processNodes(headers, values);
		// process the relationships
		processRelationships(headers, values);
	}

	private void processNodes(String[] headers, Object[] values) {
		// add the node information
		for(int i = 0; i < headers.length; i++) {
			String vertexType = headers[i];
			Object vertexLabel = values[i];
			if(vertexLabel == null) {
				continue;
			}
			String uri = vertexType + "/" + vertexLabel;

			// store the meta data around each node
			// and also ensure we do not add nodes twice unnecessarily
			boolean isNewNode = false;
			if (this.vertLabelUniqueValues.containsKey(vertexType)) {
				Set<String> processedNodes = (Set<String>) this.vertLabelUniqueValues.get(vertexType);
				if (!processedNodes.contains(vertexLabel.toString())) {
					processedNodes.add(vertexLabel.toString());
					isNewNode = true;
				}
			} else {
				Set<String> processedNodes = new HashSet<String>();
				processedNodes.add(vertexLabel.toString());
				this.vertLabelUniqueValues.put(vertexType, processedNodes);
				isNewNode = true;
			}

			// only process new nodes once
			if(!isNewNode) {
				continue;
			}

			Map<String, Object> nodeMap = new HashMap<String, Object>();
			Color color = TypeColorShapeTable.getInstance().getColor(vertexType, vertexLabel.toString());
			nodeMap.put(VERTEX_COLOR_PROPERTY, getRgb(color));
			nodeMap.put(VERTEX_TYPE_PROPERTY, vertexType);
			nodeMap.put(VERTEX_LABEL_PROPERTY, vertexLabel);
			nodeMap.put(URI, uri);

			Map<String, Object> propHash = new HashMap<String, Object>();
			if (this.nodePropertiesMap != null && !this.nodePropertiesMap.isEmpty()) {
				if (nodePropertiesMap.containsKey(vertexType)) {
					List<String> propertyTypes = nodePropertiesMap.get(vertexType);
					for (String property : propertyTypes) {
						int propertyIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(headers, property);
						if (propertyIndex < values.length) {
							propHash.put(property, values[propertyIndex]);
						}
					}

				}
			}
			nodeMap.put(PROP_HASH, propHash);
			this.nodesMapList.add(nodeMap);
		}
	}

	private void processRelationships(String[] headers, Object[] values) {
		// add the relationship information

		// we will use the index connections to determine the header locations
		// instead of calculating this every time
		if (this.indexConnections != null && !this.indexConnections.isEmpty()) {
			for (Integer[] index : indexConnections) {
				Map<String, Object> edgeMap = new HashMap<String, Object>();
				int upHeaderIndex = index[0];
				int downHeaderIndex = index[1];
				if (upHeaderIndex >= 0 && downHeaderIndex >= 0) {
					Object sValue = values[upHeaderIndex];
					if(sValue == null) {
						continue;
					}
					Object tValue = values[downHeaderIndex];
					if(tValue == null) {
						continue;
					}
					String source = headers[upHeaderIndex] + "/" + sValue;
					String target = headers[downHeaderIndex] + "/" + tValue;
					String uri = source + ":" + target;
					edgeMap.put(SOURCE, source);
					edgeMap.put(TARGET, target);
					edgeMap.put(URI, uri);

					// Add relationship properties col.col = ["col"]
					Map<String, Object> propHash = new HashMap<String, Object>();
					if (edgePropertiesMap != null && !edgePropertiesMap.isEmpty()) {
						for (String edgeLabel : edgePropertiesMap.keySet()) {
							// validate syntax col.col
							if (edgeLabel.contains(".")) {
								String[] split = edgeLabel.split("\\.");
								if (split.length > 0) {
									String startNode = split[0];
									String endNode = split[1];
									// check if edge exists in connections
									int startNodeIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(headers, startNode);
									int endNodeIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(headers, endNode);
									if (validEdgeLabel(startNodeIndex, endNodeIndex)) {
										List<String> properties = edgePropertiesMap.get(edgeLabel);
										for (String property : properties) {
											int propertyIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(headers, property);
											propHash.put(property, values[propertyIndex]);
										}
									}

								}
							}

						}
					}

					edgeMap.put(PROP_HASH, propHash);
					this.edgesMapList.add(edgeMap);
				}
			}
		}
	}

	private boolean validEdgeLabel(int startNodeIndex, int endNodeIndex) {
		if (startNodeIndex >= 0 && endNodeIndex >= 0) {
			for (Integer[] edge : indexConnections) {
				int edgeStartIndex = edge[0].intValue();
				int edgeEndIndex = edge[1].intValue();
				if (edgeStartIndex == startNodeIndex && edgeEndIndex == endNodeIndex) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Gets graph metadata
	 * "col" : unique instance count
	 */
	protected HashMap<String, Object> getGraphMeta() {
		HashMap<String, Object> meta = new HashMap<String, Object>();
		for(String vertexType : vertLabelUniqueValues.keySet()) {
			Set<String> values = vertLabelUniqueValues.get(vertexType);
			meta.put(vertexType, values.size());
		}
		return meta;
	}

	private void determineConnectionsIndex(String[] headers, String [] rawHeaders) {
		// loop through and find the indices to grab for each connection we want
		this.indexConnections = new ArrayList<Integer[]>();
		if (connectionsMap != null && !this.connectionsMap.isEmpty()) {
			for (String upstreamHeader : this.connectionsMap.keySet()) {
				// find the up header index
				int upHeaderIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(headers, upstreamHeader);
				if(upHeaderIndex < 0)
					upHeaderIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(rawHeaders, upstreamHeader);
				List<String> downstreamHeaderList = this.connectionsMap.get(upstreamHeader);
				for (String downstreamHeader : downstreamHeaderList) {
					// find the down header index
					int downHeaderIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(headers, downstreamHeader);
					// try it in raw headers as well
					if(downHeaderIndex < 0)
						downHeaderIndex = ArrayUtilityMethods.arrayContainsValueAtIndex(rawHeaders, downstreamHeader);

					this.indexConnections.add(new Integer[] { upHeaderIndex, downHeaderIndex });
				}
			}
		}
	}

	@Override
	public Object getFormattedData() {
		Map<String, Object> formattedData = new HashMap<String, Object>();
		formattedData.put(NODES, nodesMapList);
		formattedData.put(EDGES, edgesMapList);
		formattedData.put(GRAPH_META, getGraphMeta());
		return formattedData;
	}

	@Override
	public void clear() {
		this.nodesMapList.clear();
		this.edgesMapList.clear();
	}

	@Override
	public String getFormatType() {
		return "GRAPH";
	}

	@Override
	public void setOptionsMap(Map<String, Object> optionsMap) {
		super.setOptionsMap(optionsMap);
		String connections = (String) this.optionsMap.get("connections");
		if (connections != null && connections.length() > 0) {
			this.connectionsMap = generateEdgeHashFromStr(connections);
		}
		String nodeProperties = (String) this.optionsMap.get("nodeProperties");
		if (nodeProperties != null && nodeProperties.length() > 0) {
			this.nodePropertiesMap = generateEdgeHashFromStr(nodeProperties);
		}
		String edgeProperties = (String) this.optionsMap.get("edgeProperties");
		if (edgeProperties != null && edgeProperties.length() > 0) {
			this.edgePropertiesMap = generateEdgeHashFromStr(edgeProperties);
		}
	}
	
	public static Map<String, List<String>> generateEdgeHashFromStr(String edgeHashStr) {
		Map<String, List<String>> edgeHash = new Hashtable<String, List<String>>();
		// each path is separated by a semicolon
		String[] paths = edgeHashStr.split(";");
		for(String path : paths) {
			if(path.contains(".")) {
				String[] pathVertex = path.split("\\.");
				// we start at index 1 and take the index prior for ease of looping
				for(int i = 1; i < pathVertex.length; i++) {
					String startNode = pathVertex[i-1];
					//TODO: need to figure out passing of alias!!!!
					if(startNode.contains("__")) {
						startNode = startNode.split("__")[1];
					}
					String endNode = pathVertex[i];
					if(endNode.contains("__")) {
						endNode = endNode.split("__")[1];
					}
					
					// update the edge hash correctly
					Set<String> downstreamNodes = null;
					Vector<String> list = new Vector<String>();

					if (edgeHash.containsKey(startNode)) {
						downstreamNodes = new HashSet<String>(edgeHash.get(startNode));
						downstreamNodes.add(endNode);
					} else {
						downstreamNodes = new HashSet<String>();
						downstreamNodes.add(endNode);
					}
					list.addAll(downstreamNodes);
					edgeHash.put(startNode, list);
				}
			} else {
				// ugh... when would this happen?
			}
		}
		return edgeHash;
	}
	
	private String getRgb(Color c) {
		return c.getRed() + "," + c.getGreen() + "," +c.getBlue();
	}
}
