package prerna.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

import prerna.algorithm.api.IMetaData;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.api.IRawSelectWrapper;
import prerna.engine.impl.rdf.InMemorySesameEngine;
import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.util.Utility;

public class OwlTemporalEngineMeta {

	private InMemorySesameEngine myEng;
	
	private static final String SEMOSS_CONCEPT_PREFIX = "http://semoss.org/ontologies/Concept";
	private static final String SEMOSS_PROPERTY_PREFIX = "http://semoss.org/ontologies/Relation/Contains";
	private static final String SEMOSS_RELATION_PREFIX = "http://semoss.org/ontologies/Relation";

	// specific uri's for in memory databases
	private static final String IS_PRIM_KEY_PRED = "http://semoss.org/ontologies/Relation/Contains/IsPrimKey";
	private static final String IS_DERIVED_PRED = "http://semoss.org/ontologies/Relation/Contains/IsDerived";
	private static final String QUERY_STRUCT_PRED = "http://semoss.org/ontologies/Relation/Contains/QueryStructName";
	private static final String ALIAS_PRED = "http://semoss.org/ontologies/Relation/Contains/Alias";
	private static final String PHYSICAL_PRED = "http://semoss.org/ontologies/Relation/Contains/Physical";

	/**
	 * Constructor
	 */
	public OwlTemporalEngineMeta() {
		// generate the in memory rc 
		RepositoryConnection rc = null;
		try {
			Repository myRepository = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));
			myRepository.initialize();
			rc = myRepository.getConnection();
		} catch(RuntimeException ignored) {
			ignored.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		
		// set the rc in the in-memory engine
		this.myEng = new InMemorySesameEngine();
		this.myEng.setRepositoryConnection(rc);
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	
	/*
	 * METHODS PERTAINING TO A VERTEX
	 */

	public void addVertex(String uniqueName) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = RDFS.SUBCLASSOF.toString();
		obj = SEMOSS_CONCEPT_PREFIX;
		this.myEng.addStatement(new Object[]{sub, pred, obj, true});
	}

	public void setDataTypeToVertex(String uniqueName, String dataType) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = OWL.DATATYPEPROPERTY.toString();
		obj = dataType;
		if(obj == null) {
			obj = "STRING";
		} else {
			if(Utility.isNumericType(dataType)) {
				obj = "NUMBER";
			} else if(Utility.isDateType(dataType)) {
				obj = "DATE";
			} else {
				obj = "STRING";
			}
		}	
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setPrimKeyToVertex(String uniqueName, boolean isPrimKey) {
		String sub = "";
		String pred = "";
		boolean obj = false;
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = IS_PRIM_KEY_PRED;
		obj = isPrimKey;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setDerivedToVertex(String uniqueName, boolean isDerived) {
		String sub = "";
		String pred = "";
		boolean obj = false;
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = IS_DERIVED_PRED;
		obj = isDerived;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setQueryStructNameToVertex(String uniqueName, String engineName, String qsName) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = QUERY_STRUCT_PRED;
		obj = engineName + ":::" + qsName;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setAliasToVertex(String uniqueName, String alias) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = ALIAS_PRED;
		obj = alias;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setPhysicalNameToVertex(String uniqueName, String physical) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = PHYSICAL_PRED;
		obj = physical;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	
	/*
	 * METHODS PERTAINING TO A PROPERTY
	 */

	public void addProperty(String conceptUniqueName, String propertyUniqueName) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the property as a property
		sub = SEMOSS_PROPERTY_PREFIX+ "/" + propertyUniqueName;
		pred = RDF.TYPE.toString();
		obj = SEMOSS_PROPERTY_PREFIX;
		this.myEng.addStatement(new Object[]{sub, pred, obj, true});
		
		// store the property for the provided concept
		sub = SEMOSS_CONCEPT_PREFIX+ "/" + conceptUniqueName;
		pred = SEMOSS_PROPERTY_PREFIX;
		obj = SEMOSS_PROPERTY_PREFIX + "/" + propertyUniqueName;
		this.myEng.addStatement(new Object[]{sub, pred, obj, true});
	}
	
	public void setDataTypeToProperty(String uniqueName, String dataType) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_PROPERTY_PREFIX + "/" + uniqueName;
		pred = OWL.DATATYPEPROPERTY.toString();
		obj = dataType;
		if(obj == null) {
			obj = "STRING";
		} else {
			if(Utility.isNumericType(dataType)) {
				obj = "NUMBER";
			} else if(Utility.isDateType(dataType)) {
				obj = "DATE";
			} else {
				obj = "STRING";
			}
		}
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setPrimKeyToProperty(String uniqueName, boolean isPrimKey) {
		String sub = "";
		String pred = "";
		boolean obj = false;
		
		// store the unique name as a concept
		sub = SEMOSS_PROPERTY_PREFIX + "/" + uniqueName;
		pred = IS_PRIM_KEY_PRED;
		obj = isPrimKey;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setDerivedToProperty(String uniqueName, boolean isDerived) {
		String sub = "";
		String pred = "";
		boolean obj = false;
		
		// store the unique name as a concept
		sub = SEMOSS_PROPERTY_PREFIX + "/" + uniqueName;
		pred = IS_DERIVED_PRED;
		obj = isDerived;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setQueryStructNameToProperty(String uniqueName, String engineName, String qsName) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_PROPERTY_PREFIX + "/" + uniqueName;
		pred = QUERY_STRUCT_PRED;
		obj = engineName + ":::" + qsName;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	public void setAliasToProperty(String uniqueName, String alias) {
		String sub = "";
		String pred = "";
		String obj = "";
		
		// store the unique name as a concept
		sub = SEMOSS_PROPERTY_PREFIX + "/" + uniqueName;
		pred = ALIAS_PRED;
		obj = alias;
		this.myEng.addStatement(new Object[]{sub, pred, obj, false});
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	
	/*
	 * METHODS PERTAINING TO ADDING RELATIONSHIPS
	 */
	
	public void addRelationship(String fromUniqueName, String toUniqueName, String relType) {
		String sub = "";
		String pred = "";
		String obj = "";

		// store the specific relation as a relation
		sub = SEMOSS_RELATION_PREFIX + "/" + fromUniqueName + ":" + toUniqueName  + ":" + relType;
		pred = RDFS.SUBPROPERTYOF.toString();
		obj = SEMOSS_RELATION_PREFIX;
		this.myEng.addStatement(new Object[]{sub, pred, obj, true});
		
		// store the relation between the two vertices
		sub = SEMOSS_CONCEPT_PREFIX + "/" + fromUniqueName;
		pred = SEMOSS_RELATION_PREFIX + "/" + fromUniqueName + ":" + toUniqueName + ":" + relType;
		obj = SEMOSS_CONCEPT_PREFIX + "/" + toUniqueName;
		this.myEng.addStatement(new Object[]{sub, pred, obj, true});
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////

	/*
	 * Flush out the data from the OWL to a POJO
	 * This is the return we want from the FE so we know Table and Column
	 */
	public List<String> getFrameSelectors() {
		String query = "select distinct "
				+ "?header (coalesce(?prim, 'false') as ?isPrim)"
				+ "where {"
				+ "{"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "}"
				+ "union"
				+ "{"
				+ "{?header <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "}"
				+ "filter(?header != <" + SEMOSS_CONCEPT_PREFIX + "> && "
					+ "?header != <" + SEMOSS_PROPERTY_PREFIX + ">)"
				+ "} order by ?header";
		
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		List<String> headers = new Vector<String>();
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			if(row[1].equals("false")) {
				headers.add(row[0].toString());
			}
		}
		
		return headers;
	}
	
	/**
	 * This returns the alias for the names in the frame
	 * @return
	 */
	public List<String> getFrameColumnNames() {
		String query = "select distinct "
				+ "(coalesce(?alias, ?header) as ?frameName) (coalesce(?prim, 'false') as ?isPrim) "
				+ "where {"
				+ "{"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?header <" + ALIAS_PRED + "> ?alias}"
				+ "}"
				+ "union"
				+ "{"
				+ "{?header <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?header <" + ALIAS_PRED + "> ?alias}"
				+ "}"
				+ "filter(?header != <" + SEMOSS_CONCEPT_PREFIX + "> && "
					+ "?header != <" + SEMOSS_PROPERTY_PREFIX + ">)"
				+ "} order by ?header";
		
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		List<String> headers = new Vector<String>();
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			if(row[1].equals("false")) {
				headers.add(row[0].toString());
			}
		}
		
		return headers;
	}
	
	/**
	 * Get the physical name for a unique name
	 * @param uniqueName
	 * @return
	 */
	public String getPhysicalName(String uniqueName) {
		String query = "select distinct "
				+ "?header ?physical "
				+ "where {"
				+ "bind(<" + SEMOSS_CONCEPT_PREFIX + "/" + uniqueName + "> as ?header)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?header <" + PHYSICAL_PRED + "> ?physical}"
				+ "}";
		
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			return row[1].toString();
		}
		
		return uniqueName;
	}
	
	/*
	 * Flush out the relationships from the OWL to a POJO
	 */
	public List<String[]> getAllRelationships() {
		String query = "select ?fromNode ?toNode ?rel where {"
				+ "{?fromNode <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?toNode <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?fromNode ?rel ?toNode}"
				+ "filter(?rel != <" + SEMOSS_RELATION_PREFIX + ">)"
				+ "}";
		
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		List<String[]> relationships = new Vector<String[]>();
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			relationships.add(new String[]{row[0].toString(), row[1].toString(), row[2].toString().split(":")[2]});
		}
		return relationships;
	}
	
	public Map<String, IMetaData.DATA_TYPES> getHeaderToTypeMap() {
		String query = "select distinct ?header ?datatype where {"
				+ "{"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?header <" + OWL.DATATYPEPROPERTY + "> ?datatype}"
				+ "}"
				+ "union"
				+ "{"
				+ "{?header <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
				+ "{?header <" + OWL.DATATYPEPROPERTY + "> ?datatype}"
				+ "}"
				+ "}";
		
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		Map<String, IMetaData.DATA_TYPES> returnMap = new HashMap<String, IMetaData.DATA_TYPES>();
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			returnMap.put(row[0].toString(), IMetaData.convertToDataTypeEnum(row[1].toString()));
		}
		
		return returnMap;
	}
	
	public IMetaData.DATA_TYPES getHeaderTypeAsEnum(String uniqueName) {
		String parent = null;
		if(uniqueName.contains("__")) {
			parent = uniqueName.split("__")[1];
		}
		return getHeaderTypeAsEnum(uniqueName, parent);
	}
	
	public IMetaData.DATA_TYPES getHeaderTypeAsEnum(String uniqueName, String parentUniqueName) {
		String query = null;
		if(parentUniqueName == null || parentUniqueName.isEmpty()) {
			// we have a concept
			query = "select distinct ?header ?datatype where {"
					+ "bind(<" + SEMOSS_CONCEPT_PREFIX + "/" + uniqueName + "> as ?header)"
					+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
					+ "{?header <" + OWL.DATATYPEPROPERTY + "> ?datatype}"
					+ "}";
		} else {
			// we have a property
			query = "select distinct ?header ?datatype where {"
					+ "bind(<" + SEMOSS_PROPERTY_PREFIX + "/" + uniqueName + "> as ?header)"
					+ "{?header <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
					+ "{?header <" + OWL.DATATYPEPROPERTY + "> ?datatype}"
					+ "bind(<" + SEMOSS_CONCEPT_PREFIX + "/" + parentUniqueName + "> as ?parent)"
					+ "{?parent <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
					+ "{?parent <" + SEMOSS_PROPERTY_PREFIX + "> ?header}"
					+ "}";
		}
	
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			return IMetaData.convertToDataTypeEnum(row[1].toString());
		}
		
		return null;
	}
	
	public String getHeaderTypeAsString(String uniqueName) {
		String parent = null;
		if(uniqueName.contains("__")) {
			parent = uniqueName.split("__")[1];
		}
		return getHeaderTypeAsString(uniqueName, parent);
	}
	
	public String getHeaderTypeAsString(String uniqueName, String parentUniqueName) {
		String query = null;
		if(parentUniqueName == null || parentUniqueName.isEmpty()) {
			// we have a concept
			query = "select distinct ?header ?datatype where {"
					+ "bind(<" + SEMOSS_CONCEPT_PREFIX + "/" + uniqueName + "> as ?header)"
					+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
					+ "{?header <" + OWL.DATATYPEPROPERTY + "> ?datatype}"
					+ "}";
		} else {
			// we have a property
			query = "select distinct ?header ?datatype where {"
					+ "bind(<" + SEMOSS_PROPERTY_PREFIX + "/" + uniqueName + "> as ?header)"
					+ "{?header <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
					+ "bind(<" + SEMOSS_CONCEPT_PREFIX + "/" + parentUniqueName + "> as ?parent)"
					+ "{?parent <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
					+ "{?parent <" + SEMOSS_PROPERTY_PREFIX + "> ?header}"
					+ "{?header <" + OWL.DATATYPEPROPERTY + "> ?datatype}"
					+ "}";
		}
	
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			return row[1].toString();
		}
		
		return null;
	}
	
	public Map<String, Object> getTableHeaderObjects(boolean onlyNumeric) {
		String query = "select distinct "
				+ "?header "
				+ "(coalesce(?prim, 'false') as ?isPrim) "
				+ "(coalesce(?dt, 'unknown') as ?dataType) "
				+ "(coalesce(?qs, 'unknown') as ?qsName) "
				+ "(coalesce(?parent, 'none') as ?parentNode) "
				+ "(coalesce(?alias, 'none') as ?displayName) "
				+ "(coalesce(?derived, 'false') as ?isDerived) "
				+ "where {"
				+ "{" 
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ (onlyNumeric ? "bind(\"NUMBER\" as ?dt){?header <" + OWL.DATATYPEPROPERTY + "> \"NUMBER\"}" : "optional{?header <" + OWL.DATATYPEPROPERTY + "> ?dt}")
				+ "optional{?header <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?header <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?header <" + IS_DERIVED_PRED + "> ?derived}"
				+ "bind('none' as ?parent)"
				+ "}"
				+ "union"
				+ "{"
				+ "{?header <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
				+ "{?parent <" + SEMOSS_PROPERTY_PREFIX + "> ?header}"
				+ (onlyNumeric ? "bind(\"NUMBER\" as ?dt){?header <" + OWL.DATATYPEPROPERTY + "> \"NUMBER\"}" : "optional{?header <" + OWL.DATATYPEPROPERTY + "> ?dt}")
				+ "optional{?header <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?header <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?header <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}"
				+ "filter(?header != <" + SEMOSS_CONCEPT_PREFIX + "> && "
					+ "?header != <" + SEMOSS_PROPERTY_PREFIX + ">)"
				+ "} ORDER BY ?displayName";
		
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		
		Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
		
		List<Map<String, Object>> headersList = new ArrayList<Map<String, Object>>();
		while(it.hasNext()) {
			IHeadersDataRow dataRow = it.next();
			
			String[] headers = dataRow.getHeaders();
			Object[] values = dataRow.getValues();
			int numReturns = headers.length;
			
			String uniqueName = values[0].toString();
			String isPrim = values[1].toString();
			// we will ignore primary key headers as they cannot be used for calculations
			// they are only used as structural MM constructs for how data is connected
			if(isPrim.equals("true")) {
				continue;
			}
			
			boolean append = false;
			Map<String, Object> rowMap = null;
			if(nameToIndex.containsKey(uniqueName)) {
				append = true;
				rowMap = headersList.get(nameToIndex.get(uniqueName));
			} else {
				rowMap = new HashMap<String, Object>();
			}
			
			if(!append) {
				for(int i = 0; i < numReturns; i++) {
					if(headers[i].equals("qsName") && !values[i].toString().equals("unknown")) {
						String[] split = values[i].toString().split(":::");
						Map<String, List<String>> engineQsMap = new HashMap<String, List<String>>();
						List<String> qsNamesList = new Vector<String>();
						qsNamesList.add(split[1]);
						engineQsMap.put(split[0], qsNamesList);
						rowMap.put(headers[i], engineQsMap);
					} else if(headers[i].equals("displayName")) {
						rowMap.put(headers[i], values[i].toString());
					} else {
						rowMap.put(headers[i], values[i]);
					}
				}
				// add the new map into the list
				headersList.add(rowMap);
				// store its index if we come back to it
				nameToIndex.put(uniqueName, headersList.size()-1);
			} else {
				// there are a few things that we need to merge
				for(int i = 0; i < numReturns; i++) {
					// right now, only merge the qsName in case this column is used as a join
					// from one engine to another
					if(headers[i].equals("qsName") && !values[i].toString().equals("unknown")) {
						Map<String, List<String>> engineQsMap = (Map<String, List<String>>) rowMap.get("qsName");
						String[] split = values[i].toString().split(":::");
						if(engineQsMap.containsKey(split[0])) {
							List<String> qsNamesList = engineQsMap.get(split[0]);
							qsNamesList.add(split[1]);
						} else {
							List<String> qsNamesList = new Vector<String>();
							qsNamesList.add(split[1]);
							engineQsMap.put(split[0], qsNamesList);
						}
					}
				}
			}
		}
		
		String relQuery = "select ?fromNode ?toNode ?rel where {"
				+ "{?fromNode <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?toNode <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?fromNode ?rel ?toNode}"
				+ "filter(?rel != <" + SEMOSS_RELATION_PREFIX + ">)"
				+ "}";
		
		List<Map<String, String>> relList = new ArrayList<Map<String, String>>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, relQuery);
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			Map<String, String> rel = new HashMap<String, String>();
			rel.put("fromNode", row[0].toString());
			rel.put("toNode", row[1].toString());
			rel.put("joinType", row[2].toString().split(":")[2]);
			relList.add(rel);
		}
		
		Map<String, Object> headersMap = new HashMap<String, Object>();
		headersMap.put("headers", headersList);
		headersMap.put("joins", relList);
		
		return headersMap;
	}
	
	public Map<String, Object> getMetamodel() {
		Map<String, Object> metamodel = new HashMap<String, Object>();
		// get the nodes
		String nodesQuery 	= "select distinct "
				+ "?concept "
				+ "(coalesce(?qs, 'unknown') as ?qsName) "
				+ "(coalesce(?alias, ?concept) as ?displayName) "
				+ "(coalesce(?prim, 'false') as ?primKey) "
				+ "(coalesce(?prop, 'noprops') as ?property) "
				+ "(coalesce(?property_qs, 'unknown') as ?propQsName) "
				+ "(coalesce(?property_alias, ?property) as ?propAlias) "
				+ "(coalesce(?property_prim, 'false') as ?propPrimKey) "
				+ "where {"
				+ "{?concept <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "optional{?concept <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?concept <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?concept <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional "
				+ "{"
				+ "{?prop <" + RDF.TYPE + "> <" + SEMOSS_PROPERTY_PREFIX + ">}"
				+ "{?concept <" + SEMOSS_PROPERTY_PREFIX + "> ?prop}"
				+ "optional{?prop <" + QUERY_STRUCT_PRED + "> ?property_qs}"
				+ "optional{?prop <" + ALIAS_PRED + "> ?property_alias}"
				+ "optional{?prop <" + IS_PRIM_KEY_PRED + "> ?property_prim}"
				+ "}"
				+ "filter(?concept != <" + SEMOSS_CONCEPT_PREFIX + ">)"
//				+ "filter(?prop != <" + SEMOSS_PROPERTY_PREFIX + ">)"
				+ "}";
		
		Map<String, Map<String, Object>> nodesMap = new HashMap<String, Map<String, Object>>();
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, nodesQuery);
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			// concept values
			String conceptName = row[0].toString();
			String conceptQs = row[1].toString();
			String conceptAlias = row[2].toString();
			String conceptPrim = row[3].toString();
			
			// property values for concept
			String propertyName = row[4].toString();
			String propertyQs = row[5].toString();
			String propertyAlias = row[6].toString();
			String propertyPrim = row[7].toString();

			
			Map<String, Object> node = null;
			if(nodesMap.containsKey(conceptName)) {
				// we have seen this node before
				node = nodesMap.get(conceptName);
			} else {
				// new node
				// add the node info that can only appear once
				node = new HashMap<String, Object>();
				node.put("conceptualName", conceptName);
				node.put("displayName", conceptAlias);
				node.put("primKey", Boolean.parseBoolean(conceptPrim));

				// also add in an empty property map so
				// we dont have to check for this
				node.put("properties", new HashMap<String, Map<String, Object>>());
				
				nodesMap.put(conceptName, node);
			}

			if(!conceptQs.equals("unknown")) {
				Map<String, List<String>> conceptEngineQsMap = null;
				String[] conceptQsSplit = conceptQs.split(":::");
				if(node.containsKey("engineQs")) {
					conceptEngineQsMap = (Map<String, List<String>>) node.get("engineQs");
					if(conceptEngineQsMap.containsKey(conceptQsSplit[0])) {
						List<String> qsNamesList = conceptEngineQsMap.get(conceptQsSplit[0]);
						qsNamesList.add(conceptQsSplit[1]);
					} else {
						List<String> qsNamesList = new Vector<String>();
						qsNamesList.add(conceptQsSplit[1]);
						conceptEngineQsMap.put(conceptQsSplit[0], qsNamesList);
					}
				} else {
					conceptEngineQsMap = new HashMap<String, List<String>>();
					List<String> qsNamesList = new Vector<String>();
					qsNamesList.add(conceptQsSplit[1]);
					conceptEngineQsMap.put(conceptQsSplit[0], qsNamesList);
					node.put("engineQs", conceptEngineQsMap);
				}
			}

			// check if there are properties to add
			if(propertyName.equals("noprops")) {
				continue;
			}
			
			Map<String, Map<String, Object>> propertiesMap = (Map<String, Map<String, Object>>) node.get("properties");
			Map<String, Object> prop = null;
			if(propertiesMap.containsKey(propertyName)) {
				// we have seen this property before 
				prop = propertiesMap.get(propertyName);
			} else {
				// new property
				// add the property info that will only appear once
				prop = new HashMap<String, Object>();
				prop.put("conceptualName", propertyName);
				prop.put("displayName", propertyAlias);
				prop.put("primKey", Boolean.parseBoolean(propertyPrim));

				propertiesMap.put(propertyName, prop);
			}
			
			if(!propertyQs.equals("unknown")) {
				Map<String, List<String>> propEngineQsMap = null;
				String[] propertyQsSplit = propertyQs.split(":::");
				if(prop.containsKey("engineQs")) {
					propEngineQsMap = (Map<String, List<String>>) prop.get("engineQs");
					if(propEngineQsMap.containsKey(propertyQsSplit[0])) {
						List<String> qsNamesList = propEngineQsMap.get(propertyQsSplit[0]);
						qsNamesList.add(propertyQsSplit[1]);
					} else {
						List<String> qsNamesList = new Vector<String>();
						qsNamesList.add(propertyQsSplit[1]);
						propEngineQsMap.put(propertyQsSplit[0], qsNamesList);
					}
				} else {
					propEngineQsMap = new HashMap<String, List<String>>();
					List<String> qsNamesList = new Vector<String>();
					qsNamesList.add(propertyQsSplit[1]);
					propEngineQsMap.put(propertyQsSplit[0], qsNamesList);
					prop.put("engineQs", propEngineQsMap);
				}
			}
		}
		
		// flatten out the properties information
		for(String key : nodesMap.keySet()) {
			Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) nodesMap.get(key).get("properties");
			nodesMap.get(key).put("properties", properties.values());
		}
		
		// flatten out the nodes as well and store in return object
		metamodel.put("nodes", nodesMap.values());
		
		String relQuery = "select ?fromNode ?toNode where {"
				+ "{?fromNode <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?toNode <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?fromNode ?rel ?toNode}"
				+ "}";
		
		List<Map<String, String>> relList = new ArrayList<Map<String, String>>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, relQuery);
		while(it.hasNext()) {
			Object[] row = it.next().getValues();
			Map<String, String> rel = new HashMap<String, String>();
			rel.put("fromNode", row[0].toString());
			rel.put("toNode", row[1].toString());
			relList.add(rel);
		}
		metamodel.put("edges", relList);
		
		return metamodel;
	}
	
	public QueryStruct2 getFlatTableQs() {
		QueryStruct2 qs = new QueryStruct2();
		
		// set all the headers
		List<String> allHeaders = this.getFrameSelectors();
		for(String header : allHeaders) {
			if(header.contains("__")) {
				String[] split = header.split("__");
				QueryColumnSelector qsSelector = new QueryColumnSelector();
				qsSelector.setTable(split[0]);
				qsSelector.setColumn(split[1]);
				qsSelector.setAlias(split[1]);
				qs.addSelector(qsSelector);
			} else {
				QueryColumnSelector qsSelector = new QueryColumnSelector();
				qsSelector.setTable(header);
				qsSelector.setColumn(QueryStruct.PRIM_KEY_PLACEHOLDER);
				qsSelector.setAlias(header);
				qs.addSelector(qsSelector);
			}
		}
		
		// set all the relationships
		// ASSUMPTION ::: EVERYTHING IS INNER JOIN!
		List<String[]> allRelationships = this.getAllRelationships();
		for(String[] rel : allRelationships) {
			qs.addRelation(rel[0], rel[1], rel[2]);
		}
		
		return qs;
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////

	/*
	 * METHODS TO MODIFY EXISTING META INFORMATION
	 */

	public void modifyDataTypeToVertex(String uniqueName, String newDataType) {
		// need to get the current value
		String curType = getHeaderTypeAsString(uniqueName, null);

		String sub = "";
		String pred = "";
		String obj = "";
		
		// remove the existing type
		sub = SEMOSS_CONCEPT_PREFIX + "/" + uniqueName;
		pred = OWL.DATATYPEPROPERTY.toString();
		obj = curType;
		this.myEng.removeStatement(new Object[]{sub, pred, obj, false});

		// add the new type
		setDataTypeToVertex(uniqueName, newDataType);
	}
	
	public void modifyDataTypeToProperty(String uniqueName, String parent, String newDataType) {
		// need to get the current value
		String curType = getHeaderTypeAsString(uniqueName, parent);

		String sub = "";
		String pred = "";
		String obj = "";
		
		// remove the existing type
		sub = SEMOSS_PROPERTY_PREFIX + "/" + uniqueName;
		pred = OWL.DATATYPEPROPERTY.toString();
		obj = curType;
		this.myEng.removeStatement(new Object[]{sub, pred, obj, false});

		// add the new type
		setDataTypeToProperty(uniqueName, newDataType);
	}
	
	public void modifyPropertyName(String propertyName, String tableName, String newPropertyName) {
		/*
		 * This method is used in instances when we must modify the column name
		 */
		
		final String EMPTY_VALUE = "not_present";
		
		String headerPropertiesQuery = "select distinct "
				+ "?parent "
				+ "?property "
				+ "(coalesce(?prim, '" + EMPTY_VALUE + "') as ?isPrim) "
				+ "(coalesce(?dt, '" + EMPTY_VALUE + "') as ?dataType) "
				+ "(coalesce(?qs, '" + EMPTY_VALUE + "') as ?qsName) "
				+ "(coalesce(?alias, '" + EMPTY_VALUE + "') as ?displayName) "
				+ "(coalesce(?derived, '" + EMPTY_VALUE + "') as ?isDerived) "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + tableName + "> as ?parent)"
				+ "BIND(<" + SEMOSS_PROPERTY_PREFIX + "/" + propertyName + "> as ?property)"
				+ "{?parent <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?parent <" + SEMOSS_PROPERTY_PREFIX + "> ?property}"
				+ "optional{?property <" + OWL.DATATYPEPROPERTY + "> ?dt}"
				+ "optional{?property <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?property <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?property <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?property <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}";
		
		List<Object[]> propertyInfo = new Vector<Object[]>();
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, headerPropertiesQuery);
		while(it.hasNext()) {
			propertyInfo.add(it.next().getValues());
		}
		
		for(Object[] headerPropRow : propertyInfo) {
			String tableUri = SEMOSS_CONCEPT_PREFIX + "/" + headerPropRow[0].toString();
			String propertyUri = SEMOSS_PROPERTY_PREFIX + "/" + headerPropRow[1].toString();
			String propertyIsPrimKey = headerPropRow[2].toString();
			String propertyDataType = headerPropRow[3].toString();
			String propertyQsInfo = headerPropRow[4].toString();
			String propertyAlias = headerPropRow[5].toString();
			String propertyDerived = headerPropRow[6].toString();
			
			String newPropertyUri = SEMOSS_PROPERTY_PREFIX + "/" + newPropertyName;
			
			this.myEng.removeStatement(new Object[]{tableUri, SEMOSS_PROPERTY_PREFIX, propertyUri, true});
			this.myEng.removeStatement(new Object[]{propertyUri, RDF.TYPE, SEMOSS_PROPERTY_PREFIX, true});
			
			this.myEng.addStatement(new Object[]{tableUri, SEMOSS_PROPERTY_PREFIX, newPropertyUri, true});
			this.myEng.addStatement(new Object[]{newPropertyUri, RDF.TYPE, SEMOSS_PROPERTY_PREFIX, true});

			if(!propertyIsPrimKey.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_PRIM_KEY_PRED, propertyIsPrimKey, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, IS_PRIM_KEY_PRED, propertyIsPrimKey, false});
			}
			if(!propertyDataType.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, OWL.DATATYPEPROPERTY, propertyDataType, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, OWL.DATATYPEPROPERTY, propertyDataType, false});
			}
			if(!propertyQsInfo.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, QUERY_STRUCT_PRED, propertyQsInfo, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, QUERY_STRUCT_PRED, propertyQsInfo, false});
			}
			if(!propertyAlias.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, ALIAS_PRED, propertyAlias, false});
			}
			// always add the new alias since this is what he want to display
			this.myEng.addStatement(new Object[]{newPropertyUri, ALIAS_PRED, newPropertyName.split("__")[1], false});
			if(!propertyDerived.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_DERIVED_PRED, propertyDerived, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, IS_DERIVED_PRED, propertyDerived, false});
			}
		}
	}
	
	public void modifyVertexName(String oldTableName, String newTableName) {
		/*
		 * This method is used in instances when we must modify the table name
		 * Mostly used when pushing a frame from H2 into R and vice versa
		 * Since the column names stay the same, but tablename/rvarname get modified
		 */
		
		final String EMPTY_VALUE = "not_present";
		
		// first query to get all the information on the header
		String headerInfoQuery = "select distinct "
				+ "?header "
				+ "(coalesce(?prim, '" + EMPTY_VALUE + "') as ?isPrim) "
				+ "(coalesce(?dt, '" + EMPTY_VALUE + "') as ?dataType) "
				+ "(coalesce(?qs, '" + EMPTY_VALUE + "') as ?qsName) "
				+ "(coalesce(?alias, '" + EMPTY_VALUE + "') as ?displayName) "
				+ "(coalesce(?derived, '" + EMPTY_VALUE + "') as ?isDerived) "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + oldTableName + "> as ?header)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "optional{?header <" + OWL.DATATYPEPROPERTY + "> ?dt}"
				+ "optional{?header <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?header <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?header <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}";
		
		
		List<Object[]> headerInfo = new Vector<Object[]>();
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, headerInfoQuery);
		while(it.hasNext()) {
			headerInfo.add(it.next().getValues());
		}
		
		// second query to get all the header to properties triples
		String headerPropertiesQuery = "select distinct "
				+ "?header "
				+ "?property "
				+ "(coalesce(?prim, '" + EMPTY_VALUE + "') as ?isPrim) "
				+ "(coalesce(?dt, '" + EMPTY_VALUE + "') as ?dataType) "
				+ "(coalesce(?qs, '" + EMPTY_VALUE + "') as ?qsName) "
				+ "(coalesce(?alias, '" + EMPTY_VALUE + "') as ?displayName) "
				+ "(coalesce(?derived, '" + EMPTY_VALUE + "') as ?isDerived) "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + oldTableName + "> as ?header)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?header <" + SEMOSS_PROPERTY_PREFIX + "> ?property}"
				+ "optional{?property <" + OWL.DATATYPEPROPERTY + "> ?dt}"
				+ "optional{?property <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?property <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?property <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?property <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}";
		
		List<Object[]> headerProperties = new Vector<Object[]>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, headerPropertiesQuery);
		while(it.hasNext()) {
			headerProperties.add(it.next().getValues());
		}
		
		// third query to get all the upstream relationships
		String upstreamRelsQuery = "select distinct "
				+ "?myHeader "
				+ "?rel "
				+ "?otherHeader "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + oldTableName + "> as ?myHeader)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?otherHeader <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?myHeader ?rel ?otherHeader}"
				+ "}";
		
		List<Object[]> upstreamRels = new Vector<Object[]>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, upstreamRelsQuery);
		while(it.hasNext()) {
			upstreamRels.add(it.next().getValues());
		}
		
		// fourth query to get all the downstream relationships
		String downstreamRelsQuery = "select distinct "
				+ "?otherHeader "
				+ "?rel "
				+ "?myHeader "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + oldTableName + "> as ?myHeader)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?otherHeader <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?otherHeader ?rel ?myHeader}"
				+ "}";
		
		List<Object[]> downstreamRels = new Vector<Object[]>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, downstreamRelsQuery);
		while(it.hasNext()) {
			downstreamRels.add(it.next().getValues());
		}
		
		// the new triple we will be using for replacement
		String newTableUri = SEMOSS_CONCEPT_PREFIX + "/" + newTableName;
		
		// now that we have all the info, we remove and insert new triples
		for(Object[] headerRow : headerInfo) {
			String tableUri = SEMOSS_CONCEPT_PREFIX + "/" + headerRow[0].toString();
			String isPrimKey = headerRow[1].toString();
			String dataType = headerRow[2].toString();
			String qsInfo = headerRow[3].toString();
			String alias = headerRow[4].toString();
			String derived = headerRow[5].toString();
			
			// triples to delete and insert
			this.myEng.removeStatement(new Object[]{tableUri, RDFS.SUBCLASSOF, SEMOSS_CONCEPT_PREFIX, true});
			this.myEng.addStatement(new Object[]{newTableUri, RDFS.SUBCLASSOF, SEMOSS_CONCEPT_PREFIX, true});
			if(!isPrimKey.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, IS_PRIM_KEY_PRED, isPrimKey, false});
				this.myEng.addStatement(new Object[]{newTableUri, IS_PRIM_KEY_PRED, isPrimKey, false});
			}
			if(!dataType.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, OWL.DATATYPEPROPERTY, dataType, false});
				this.myEng.addStatement(new Object[]{newTableUri, OWL.DATATYPEPROPERTY, dataType, false});
			}
			if(!qsInfo.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, QUERY_STRUCT_PRED, qsInfo, false});
				this.myEng.addStatement(new Object[]{newTableUri, QUERY_STRUCT_PRED, qsInfo, false});
			}
			if(!alias.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, ALIAS_PRED, alias, false});
			}
			// always add the new alias since this is what he want to display
			this.myEng.addStatement(new Object[]{newTableUri, ALIAS_PRED, newTableName, false});
			if(!derived.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, IS_DERIVED_PRED, derived, false});
				this.myEng.addStatement(new Object[]{newTableUri, IS_DERIVED_PRED, derived, false});
			}
		}
		
		for(Object[] headerPropRow : headerProperties) {
			String tableUri = SEMOSS_CONCEPT_PREFIX + "/" + headerPropRow[0].toString();
			String propertyUri = SEMOSS_PROPERTY_PREFIX + "/" + headerPropRow[1].toString();
			String propertyIsPrimKey = headerPropRow[2].toString();
			String propertyDataType = headerPropRow[3].toString();
			String propertyQsInfo = headerPropRow[4].toString();
			String propertyAlias = headerPropRow[5].toString();
			String propertyDerived = headerPropRow[6].toString();
			
			String newPropertyUri = SEMOSS_PROPERTY_PREFIX + "/" + newTableName + "__" + headerPropRow[1].toString().split("__")[1];
			
			this.myEng.removeStatement(new Object[]{tableUri, SEMOSS_PROPERTY_PREFIX, propertyUri, true});
			this.myEng.removeStatement(new Object[]{propertyUri, RDF.TYPE, SEMOSS_PROPERTY_PREFIX, true});
			
			this.myEng.addStatement(new Object[]{newTableUri, SEMOSS_PROPERTY_PREFIX, newPropertyUri, true});
			this.myEng.addStatement(new Object[]{newPropertyUri, RDF.TYPE, SEMOSS_PROPERTY_PREFIX, true});

			if(!propertyIsPrimKey.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_PRIM_KEY_PRED, propertyIsPrimKey, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, IS_PRIM_KEY_PRED, propertyIsPrimKey, false});
			}
			if(!propertyDataType.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, OWL.DATATYPEPROPERTY, propertyDataType, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, OWL.DATATYPEPROPERTY, propertyDataType, false});
			}
			if(!propertyQsInfo.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, QUERY_STRUCT_PRED, propertyQsInfo, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, QUERY_STRUCT_PRED, propertyQsInfo, false});
			}
			if(!propertyAlias.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, ALIAS_PRED, propertyAlias, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, ALIAS_PRED, propertyAlias, false});
			}
			if(!propertyDerived.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_DERIVED_PRED, propertyDerived, false});
				this.myEng.addStatement(new Object[]{newPropertyUri, IS_DERIVED_PRED, propertyDerived, false});
			}
		}
		
		for(Object[] upRel : upstreamRels) {
			String myTableUri = SEMOSS_CONCEPT_PREFIX + "/" + upRel[0].toString();
			String relUri = SEMOSS_RELATION_PREFIX + "/" + upRel[1].toString();
			String otherTableUri = SEMOSS_CONCEPT_PREFIX + "/" + upRel[2].toString();

			String newRelUri = newTableName + ":" + upRel[2].toString();
			
			this.myEng.removeStatement(new Object[]{relUri, RDFS.SUBPROPERTYOF, SEMOSS_RELATION_PREFIX, true});
			this.myEng.addStatement(new Object[]{newRelUri, RDFS.SUBPROPERTYOF, SEMOSS_RELATION_PREFIX, true});

			this.myEng.removeStatement(new Object[]{myTableUri, relUri, otherTableUri, true});
			this.myEng.addStatement(new Object[]{newTableUri, newRelUri, otherTableUri, true});
		}
		
		
		for(Object[] downRel : downstreamRels) {
			String otherTableUri = SEMOSS_CONCEPT_PREFIX + "/" + downRel[0].toString();
			String relUri = SEMOSS_RELATION_PREFIX + "/" + downRel[1].toString();
			String myTableUri = SEMOSS_CONCEPT_PREFIX + "/" + downRel[2].toString();

			String newRelUri = downRel[0].toString() + ":" + newTableName;
			
			this.myEng.removeStatement(new Object[]{relUri, RDFS.SUBPROPERTYOF, SEMOSS_RELATION_PREFIX, true});
			this.myEng.addStatement(new Object[]{newRelUri, RDFS.SUBPROPERTYOF, SEMOSS_RELATION_PREFIX, true});

			this.myEng.removeStatement(new Object[]{otherTableUri, relUri, myTableUri, true});
			this.myEng.addStatement(new Object[]{otherTableUri, newRelUri, myTableUri, true});
		}
	}
	
	public void dropVertex(String tableName) {
		/*
		 * This method is used when we want to drop an entire table + all its properties
		 */
		
		final String EMPTY_VALUE = "not_present";
		
		// first query to get all the information on the header
		String headerInfoQuery = "select distinct "
				+ "?header "
				+ "(coalesce(?prim, '" + EMPTY_VALUE + "') as ?isPrim) "
				+ "(coalesce(?dt, '" + EMPTY_VALUE + "') as ?dataType) "
				+ "(coalesce(?qs, '" + EMPTY_VALUE + "') as ?qsName) "
				+ "(coalesce(?alias, '" + EMPTY_VALUE + "') as ?displayName) "
				+ "(coalesce(?derived, '" + EMPTY_VALUE + "') as ?isDerived) "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + tableName + "> as ?header)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "optional{?header <" + OWL.DATATYPEPROPERTY + "> ?dt}"
				+ "optional{?header <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?header <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?header <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?header <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}";
		
		
		List<Object[]> headerInfo = new Vector<Object[]>();
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, headerInfoQuery);
		while(it.hasNext()) {
			headerInfo.add(it.next().getValues());
		}
		
		// second query to get all the header to properties triples
		String headerPropertiesQuery = "select distinct "
				+ "?header "
				+ "?property "
				+ "(coalesce(?prim, '" + EMPTY_VALUE + "') as ?isPrim) "
				+ "(coalesce(?dt, '" + EMPTY_VALUE + "') as ?dataType) "
				+ "(coalesce(?qs, '" + EMPTY_VALUE + "') as ?qsName) "
				+ "(coalesce(?alias, '" + EMPTY_VALUE + "') as ?displayName) "
				+ "(coalesce(?derived, '" + EMPTY_VALUE + "') as ?isDerived) "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + tableName + "> as ?header)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?header <" + SEMOSS_PROPERTY_PREFIX + "> ?property}"
				+ "optional{?property <" + OWL.DATATYPEPROPERTY + "> ?dt}"
				+ "optional{?property <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?property <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?property <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?property <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}";
		
		List<Object[]> headerProperties = new Vector<Object[]>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, headerPropertiesQuery);
		while(it.hasNext()) {
			headerProperties.add(it.next().getValues());
		}
		
		// third query to get all the upstream relationships
		String upstreamRelsQuery = "select distinct "
				+ "?myHeader "
				+ "?rel "
				+ "?otherHeader "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + tableName + "> as ?myHeader)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?otherHeader <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?myHeader ?rel ?otherHeader}"
				+ "}";
		
		List<Object[]> upstreamRels = new Vector<Object[]>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, upstreamRelsQuery);
		while(it.hasNext()) {
			upstreamRels.add(it.next().getValues());
		}
		
		// fourth query to get all the downstream relationships
		String downstreamRelsQuery = "select distinct "
				+ "?otherHeader "
				+ "?rel "
				+ "?myHeader "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + tableName + "> as ?myHeader)"
				+ "{?header <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?otherHeader <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?rel <" + RDFS.SUBPROPERTYOF + "> <" + SEMOSS_RELATION_PREFIX + ">}"
				+ "{?otherHeader ?rel ?myHeader}"
				+ "}";
		
		List<Object[]> downstreamRels = new Vector<Object[]>();
		it = WrapperManager.getInstance().getRawWrapper(this.myEng, downstreamRelsQuery);
		while(it.hasNext()) {
			downstreamRels.add(it.next().getValues());
		}
		
		// now that we have all the info, we remove and insert new triples
		for(Object[] headerRow : headerInfo) {
			String tableUri = SEMOSS_CONCEPT_PREFIX + "/" + headerRow[0].toString();
			String isPrimKey = headerRow[1].toString();
			String dataType = headerRow[2].toString();
			String qsInfo = headerRow[3].toString();
			String alias = headerRow[4].toString();
			String derived = headerRow[5].toString();
			
			// triples to delete and insert
			this.myEng.removeStatement(new Object[]{tableUri, RDFS.SUBCLASSOF, SEMOSS_CONCEPT_PREFIX, true});
			if(!isPrimKey.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, IS_PRIM_KEY_PRED, isPrimKey, false});
			}
			if(!dataType.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, OWL.DATATYPEPROPERTY, dataType, false});
			}
			if(!qsInfo.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, QUERY_STRUCT_PRED, qsInfo, false});
			}
			if(!alias.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, ALIAS_PRED, alias, false});
			}
			if(!derived.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{tableUri, IS_DERIVED_PRED, derived, false});
			}
		}
		
		for(Object[] headerPropRow : headerProperties) {
			String tableUri = SEMOSS_CONCEPT_PREFIX + "/" + headerPropRow[0].toString();
			String propertyUri = SEMOSS_PROPERTY_PREFIX + "/" + headerPropRow[1].toString();
			String propertyIsPrimKey = headerPropRow[2].toString();
			String propertyDataType = headerPropRow[3].toString();
			String propertyQsInfo = headerPropRow[4].toString();
			String propertyAlias = headerPropRow[5].toString();
			String propertyDerived = headerPropRow[6].toString();
			
			this.myEng.removeStatement(new Object[]{tableUri, SEMOSS_PROPERTY_PREFIX, propertyUri, true});
			this.myEng.removeStatement(new Object[]{propertyUri, RDF.TYPE, SEMOSS_PROPERTY_PREFIX, true});
			
			if(!propertyIsPrimKey.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_PRIM_KEY_PRED, propertyIsPrimKey, false});
			}
			if(!propertyDataType.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, OWL.DATATYPEPROPERTY, propertyDataType, false});
			}
			if(!propertyQsInfo.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, QUERY_STRUCT_PRED, propertyQsInfo, false});
			}
			if(!propertyAlias.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, ALIAS_PRED, propertyAlias, false});
			}
			if(!propertyDerived.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_DERIVED_PRED, propertyDerived, false});
			}
		}
		
		for(Object[] upRel : upstreamRels) {
			String myTableUri = SEMOSS_CONCEPT_PREFIX + "/" + upRel[0].toString();
			String relUri = SEMOSS_RELATION_PREFIX + "/" + upRel[1].toString();
			String otherTableUri = SEMOSS_CONCEPT_PREFIX + "/" + upRel[2].toString();

			this.myEng.removeStatement(new Object[]{relUri, RDFS.SUBPROPERTYOF, SEMOSS_RELATION_PREFIX, true});
			this.myEng.removeStatement(new Object[]{myTableUri, relUri, otherTableUri, true});
		}
		
		for(Object[] downRel : downstreamRels) {
			String otherTableUri = SEMOSS_CONCEPT_PREFIX + "/" + downRel[0].toString();
			String relUri = SEMOSS_RELATION_PREFIX + "/" + downRel[1].toString();
			String myTableUri = SEMOSS_CONCEPT_PREFIX + "/" + downRel[2].toString();

			this.myEng.removeStatement(new Object[]{relUri, RDFS.SUBPROPERTYOF, SEMOSS_RELATION_PREFIX, true});
			this.myEng.removeStatement(new Object[]{otherTableUri, relUri, myTableUri, true});
		}
	}
	
	public void dropProperty(String propertyName, String tableName) {
		/*
		 * This method is used in instances when we want to drop a column from a given table
		 */
		
		final String EMPTY_VALUE = "not_present";
		
		String headerPropertiesQuery = "select distinct "
				+ "?parent "
				+ "?property "
				+ "(coalesce(?prim, '" + EMPTY_VALUE + "') as ?isPrim) "
				+ "(coalesce(?dt, '" + EMPTY_VALUE + "') as ?dataType) "
				+ "(coalesce(?qs, '" + EMPTY_VALUE + "') as ?qsName) "
				+ "(coalesce(?alias, '" + EMPTY_VALUE + "') as ?displayName) "
				+ "(coalesce(?derived, '" + EMPTY_VALUE + "') as ?isDerived) "
				+ "where {"
				+ "BIND(<" + SEMOSS_CONCEPT_PREFIX + "/" + tableName + "> as ?parent)"
				+ "BIND(<" + SEMOSS_PROPERTY_PREFIX + "/" + propertyName + "> as ?property)"
				+ "{?parent <" + RDFS.SUBCLASSOF + "> <" + SEMOSS_CONCEPT_PREFIX + ">}"
				+ "{?parent <" + SEMOSS_PROPERTY_PREFIX + "> ?property}"
				+ "optional{?property <" + OWL.DATATYPEPROPERTY + "> ?dt}"
				+ "optional{?property <" + QUERY_STRUCT_PRED + "> ?qs}"
				+ "optional{?property <" + IS_PRIM_KEY_PRED + "> ?prim}"
				+ "optional{?property <" + ALIAS_PRED + "> ?alias}"
				+ "optional{?property <" + IS_DERIVED_PRED + "> ?derived}"
				+ "}";
		
		List<Object[]> propertyInfo = new Vector<Object[]>();
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, headerPropertiesQuery);
		while(it.hasNext()) {
			propertyInfo.add(it.next().getValues());
		}
		
		for(Object[] headerPropRow : propertyInfo) {
			String tableUri = SEMOSS_CONCEPT_PREFIX + "/" + headerPropRow[0].toString();
			String propertyUri = SEMOSS_PROPERTY_PREFIX + "/" + headerPropRow[1].toString();
			String propertyIsPrimKey = headerPropRow[2].toString();
			String propertyDataType = headerPropRow[3].toString();
			String propertyQsInfo = headerPropRow[4].toString();
			String propertyAlias = headerPropRow[5].toString();
			String propertyDerived = headerPropRow[6].toString();
			
			this.myEng.removeStatement(new Object[]{tableUri, SEMOSS_PROPERTY_PREFIX, propertyUri, true});
			this.myEng.removeStatement(new Object[]{propertyUri, RDF.TYPE, SEMOSS_PROPERTY_PREFIX, true});
			
			if(!propertyIsPrimKey.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_PRIM_KEY_PRED, propertyIsPrimKey, false});
			}
			if(!propertyDataType.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, OWL.DATATYPEPROPERTY, propertyDataType, false});
			}
			if(!propertyQsInfo.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, QUERY_STRUCT_PRED, propertyQsInfo, false});
			}
			if(!propertyAlias.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, ALIAS_PRED, propertyAlias, false});
			}
			if(!propertyDerived.equals(EMPTY_VALUE)) {
				this.myEng.removeStatement(new Object[]{propertyUri, IS_DERIVED_PRED, propertyDerived, false});
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	
	public OwlTemporalEngineMeta copy() {
		OwlTemporalEngineMeta newMeta = new OwlTemporalEngineMeta();
		
		String query = "select distinct ?s ?p ?o where {?s ?p ?o}";
		IRawSelectWrapper it = WrapperManager.getInstance().getRawWrapper(this.myEng, query);
		while(it.hasNext()) {
			IHeadersDataRow row = it.next();
			Object[] rawRow = row.getRawValues();
			Object[] cleanRow = row.getValues();
			
			String subUri = rawRow[0].toString();
			String predUri = rawRow[1].toString();
			
			Object obj = null;
			Boolean isConcept = false;
			
			if(predUri.equals(IS_PRIM_KEY_PRED) || predUri.equals(IS_DERIVED_PRED) ||
					predUri.equals(QUERY_STRUCT_PRED) || predUri.equals(ALIAS_PRED) ||
					predUri.equals(OWL.DATATYPEPROPERTY.toString())) 
			{
				obj = cleanRow[2].toString();
				isConcept = false;
			} else {
				obj = rawRow[2].toString();
				isConcept = true;
			}
			
			if(obj.toString().equals("\"STRING\"")) {
				System.out.println("here");
			}
			
			newMeta.myEng.addStatement(new Object[]{subUri, predUri, obj, isConcept});
		}
		
		return newMeta;
	}
}
