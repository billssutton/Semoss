package prerna.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.hp.hpl.jena.vocabulary.OWL;

import prerna.engine.api.IEngine;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.impl.rdf.RDFFileSesameEngine;
import prerna.query.querystruct.AbstractQueryStruct;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.query.querystruct.selectors.QueryFunctionHelper;
import prerna.query.querystruct.selectors.QueryFunctionSelector;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.test.TestUtilityMethods;

public class Owler extends AbstractOwler {

	/**
	 * Constructor for the class when we are creating a brand new OWL file
	 * @param fileName				The location of the new OWL file
	 * @param type					The type of the engine the OWL file is being created for
	 */
	public Owler(String owlPath, IEngine.ENGINE_TYPE type) {
		super(owlPath, type);
	}
	
	/**
	 * Constructor for the class when we are adding to an existing OWL file
	 * @param existingEngine		The engine we are adding to
	 */
	public Owler(IEngine existingEngine) {
		super(existingEngine);
	}
	
	/////////////////// ADDING CONCEPTS INTO THE OWL /////////////////////////////////
	/*
	 * This method is overloaded to make it easier to add concepts into the owl file
	 * The overloading exists since some fields are not required as a result of the difference
	 * between the RDF concept URI (semoss:Concept/Concept_Name) and RDBMS concept URI (semoss:Concept/ColName/TableName)
	 * This is why we keep track of the type of engine when the OWLER is created
	 * 
	 * Lets look at what triples will be added
	 * NOTE: for simplicity, I will use 'semoss:' to denote the prefix 'http://semoss.org/ontologies'
	 * 
	 * For a RDF concept, call it 'C-X', the following triples will be added:
	 * 1) { <semoss:Concept/C-X> <rdfs:subClassOf> <semoss:Concept> }
	 * 2) { <semoss:Concept/C-X> <rdfs:Class> 'TYPE:STRING' } <- note that all RDF concepts are type STRING since they are URIs
	 * 3) { <semoss:Concept/C-X> <semoss:Relation/Conceptual> <semoss:Concept/CX> }
 	 * 
	 * NOTE: IF THE CONCEPT WAS 'CX', #3 would be a self-loop and that's okay
	 * 
	 * For a RDBMS concept, say it is 'C1' for column name and sits on 'T1' table name, the following tiples will be added:
	 * 1) { <semoss:Concept/C1/T1> <rdfs:subClassOf> <semoss:Concept> }
	 * 2) { <semoss:Concept/C1/T1> <rdfs:Class> 'TYPE:DATA_TYPE_HERE' }
	 * 3) { <semoss:Concept/C1/T1> <rdf:type> <semoss:Composite> } Depending on whether the concept is composite
	 * 4) { <semoss:Concept/C1/T1> <URI:KEY> <semoss:Relation/Contains/P/X> } If composite,
	 *    then one for each property that makes up the composite key
	 * 5) { <semoss:Concept/C1/T1> <semoss:Relation/Conceptual> <semoss:Concept/T1> }
	 */

	/**
	 * Wrapper around the main addConcept method.  Simplifies the call to use only one column name.
	 * @param tableName				For RDF: This is the name of the concept
	 * 								For RDBMS: This is the name of the table where the concept exists
	 * @param colName				For RDF: This is NOT used
	 * 								For RDBMS: This is the name of the column that contain the concept instances
	 * @param dataType				The dataType for the concept
	 * @return						Returns the physical URI for the node
	 * 								The return is only used in RDF databases where the instance URI must be linked
	 * 								to the meta URI
	 */	
	public String addConcept(String tableName, String colName, String dataType, String conceptual) {
		// since the column name is sometimes null or empty,
		// need to construct the key in a similar manner to how the subject is created
		String key = tableName + colName;
		if(type.equals(IEngine.ENGINE_TYPE.RDBMS)) {
			// if the column is null or empty, assume table name and col name are the same
			if(colName == null || colName.isEmpty()) {
				key = tableName + tableName;
			} else {
				// they might be the same, might be diff, don't matter
				// create it appropriately
				key = colName + tableName;
			}
		}

		// since RDF uses this multiple times, don't create it each time and just store it in a hash to send back
		if(!conceptHash.containsKey(key)) {
			// here is the logic to create the physical uri for the concept
			// the base URI for the concept will be the baseNodeURI
			String subject = BASE_NODE_URI + "/";
			
			// we also want to keep track of what the conceptual name is
			// jk, this will always be the table name now
			// if it is an RDBMS engine, we need to account when the table name and column name are not the same
			if(type.equals(IEngine.ENGINE_TYPE.RDBMS)) {
				// if the column is null or empty, assume table name and col name are the same
				if(colName == null || colName.isEmpty()) {
					subject += tableName + "/" + tableName;
				} else {
					// they might be the same, might be diff, don't matter
					// create it appropriately
					subject += colName + "/" + tableName;
				}
			} else {
				// here, it must be RDF
				// just add the table name since that is supposed to hold the concept name
				subject += tableName;
			}

			// now lets start to add the triples
			// lets add the triples pertaining to those numbered above

			// 1) adding the physical URI concept as a subClassOf the baseNodeURI
			engine.addToBaseEngine(subject, RDFS.SUBCLASSOF.stringValue(), BASE_NODE_URI);

			// 2) now lets add the dataType of the concept
			// determine the dataType
			if(dataType == null || dataType.isEmpty()) {
				// otherwise default to string
				dataType = "STRING";
			}
			String typeObject = "TYPE:" + dataType;
			engine.addToBaseEngine(subject, RDFS.CLASS.stringValue(), typeObject);

			// store it in the hash for future use
			conceptHash.put(key, subject);

			// 3) now lets add the physical URI concept to the conceptual concept URI
			// one of the advantages of the conceptual URI is that we can specify a concept in a database even
			// if it contains any of the special characters that are not allowed in pixel
			// the conceptual name is the clean version of the name ... for now
			String conceptualNode = conceptual;
			if(conceptualNode == null) {
				conceptualNode = Utility.cleanVariableString(tableName);
			}
			conceptualNames.add(conceptualNode);
			String conceptualSubject = BASE_NODE_URI + "/" + conceptualNode;
			engine.addToBaseEngine(subject, CONCEPTUAL_RELATION_URI, conceptualSubject);
		}
		return conceptHash.get(key);
	}
	
	public String addConcept(String tableName, String colName, String dataType) {
		return addConcept(tableName, colName, dataType, null);
	}

	/**
	 * Wrapper around the main addConcept method.  Simplifies the call as the column name is not required 
	 * and it defaults to use the default base SEMOSS_URI
	 * This version passes in an empty string as the column name.  If the engine is RDBMS, this means the 
	 * column name for the concept is the same as the table name
	 * @param tableName				For RDF: This is the name of the concept
	 * 								For RDBMS: This is the name of the table AND column name (they are the same) for the concept
	 * @param dataType				The dataType for the concept
	 * @return						Returns the physical URI for the node
	 */
	public String addConcept(String tableName, String dataType) {
		return addConcept(tableName, "", dataType);
	}
	
	/**
	 * Wrapper around the main addConcept method.  Simplifies the call as the column name is not required 
	 * and it defaults to use the default base SEMOSS_URI and the data type as string
	 * This version is really only intended for RDF databases
	 * Since concepts are URIs, the data type assumption of string is valid when an engine is RDF
	 * @param concept				The name of the concept to a
	 * @return						Returns the physical URI for the node
	 */
	public String addConcept(String concept) {
		return addConcept(concept, "", "STRING");
	}
	
	/////////////////// END ADDING CONCEPTS INTO THE OWL /////////////////////////////////

	
	/////////////////// ADDING RELATIONSHIP INTO THE OWL /////////////////////////////////
	/*
	 * This method is used to make it easier to add relationships into the owl file
	 * 
	 * Lets look at what triples will be added
	 * NOTE: for simplicity, I will use 'semoss:' to denote the prefix 'http://semoss.org/ontologies'
	 * 
	 * When a predicate is defined, lets call it 'ABC' the following triple is added:
	 * 1) { <semoss:Relation/ABC> <rdfs:subPropertyOf> <semoss:Relation> }
	 * but if the predicate is not defined, and the fromConcept is 'X' and toConcept is 'Y', the following
	 * triple is added: 
	 * 1) { <semoss:Relation/X.X.Y.X_FK> <rdfs:subPropertyOf> <semoss:Relation> }
	 * TODO: For RDBMS databases, this is not accurate, we need to overload this method...
	 * 		The relationship above always assumes the position of the FK on the second node
	 * 		and the method assumes the table name and column name are the name and the connecting column 
	 * 		is the first table with "_FK" on it needs to be overloaded to allow for proper concept creation
	 * 
	 * 
	 * Let's just continue with the relationship URI being <semoss:Relation/ABC>
	 * 
	 * For a RDF relationship between two concept, call each concept 'X' and 'Y', the following triples will be added:
	 * 2) { <semoss:Concept/X> <semoss:Relation/ABC> <semoss:Concept/Y> }
	 * For a RDBMS relationship between two concept, call each concept 'X' and 'Y', the following triples will be added:
	 * 2) { <semoss:Concept/X/X> <semoss:Relation/ABC> <semoss:Concept/Y/Y> }
	 * 
	 * NOTE: RDBMS section #2, if the concept is predefined using the addConcept method with a table and column name
	 * 		already defined, then the URIs for the concepts will not necessarily have the table name and column name
	 * 		as being the same
	 */
	
	/**
	 * Adds a relationship between two concepts within the OWL
	 * @param fromTable				For RDF: This is the name of the start concept
	 * 								For RDBMS: This is the name of the table for the start concept
	 * @param fromColumn			For RDF: This is not used
	 * 								For RDBMS: This is the name of the column for the start concept
	 * 								If empty, the logic assumes it is the fromTable
	 * @param toTable				For RDF: This is the name of the end concept
	 * 								For RDBMS: This is the name of the table for the end concept
	 * @param toColumn				For RDF: This is not used
	 * 								For RDBMS: This is the name of the column for the end concept
	 * 								If empty, the logic assumes it is the toTable
	 * @param predicate				The predicate for the relationship.  If this is null, the code
	 * 								will generate a predicate based on the fromConcept and toConcept.
	 * 								The format will be created based on the default loading of a RDBMS
	 * 								predicate where each concept is its own table and the column name 
	 * 								matches the table name.
	 * @return						Returns the physical URI for the relationship
	 */
	public String addRelation(String fromTable, String fromColumn, String toTable, String toColumn, String predicate) {
		// since RDF uses this multiple times, don't create it each time and just store it in a hash to send back
		if(!relationHash.containsKey(fromTable + fromColumn + toTable + toColumn + predicate)) {
			
			// need to make sure both the fromConcept and the toConcept are already defined as concepts
			// TODO: this works for RDBMS even though it only takes in the concept names because we usually perform
			// 		the addConcept call before... this is really just intended to retrieve the concept URIs from the
			// 		conceptHash as they should already there
			String fromConceptURI = addConcept(fromTable, fromColumn, null);
			String toConceptURI = addConcept(toTable, toColumn, null);
			
			// if a predicate is not defined, we create it based on the connection
			// TODO: this makes a lot of assumptions regarding the structure of the database
			// 		the assumption is only accurate for databases that we create within the tool through upload
			if(predicate == null) {
				// assume the foreign key is on the toTable
				// this will be the foreign key column in the toTable, which is the from column name + "_FK"
				// however, if from column is empty, it will be the assumption that from column is the same as from table
				
				// store the foreign key column
				String fk_Col = null;
				// this will be the column name in case it is empty and the same as the table name
				String fromCol = null;
				if(!fromColumn.isEmpty()) {
					fk_Col = fromColumn;
					fromCol = fromColumn;
				} else {
					// if the fromColumn is empty, assume it is the same as the table
					fk_Col = fromTable;
					fromCol = fromTable;
				}
				fk_Col += "_FK";
				
				// generate the predicate string
				predicate = fromTable + "." + fromCol + "." + toTable + "." + fk_Col;
			}
			
			// TODO: this always assumes the baseURI for the relationship is the default semoss uri
			// create the base relationship uri
			String baseRelationURI = SEMOSS_URI_PREFIX + DEFAULT_RELATION_CLASS;
			String predicateSubject = baseRelationURI + "/" + predicate;
			
			// now lets start to add the triples
			// lets add the triples pertaining to those numbered above
			
			// 1) now add the physical relationship URI
			engine.addToBaseEngine(predicateSubject, RDFS.SUBPROPERTYOF.stringValue(), baseRelationURI);
			
			// 2) now add the relationship between the two nodes
			engine.addToBaseEngine(fromConceptURI, predicateSubject, toConceptURI);
			
			// lastly, store it in the hash for future use
			relationHash.put(fromTable + fromColumn + toTable + toColumn + predicate, predicateSubject);
		}
		return relationHash.get(fromTable + fromColumn + toTable + toColumn + predicate);
	}
	
	/**
	 * This does the same thing as addRelationship but removes instead of add statements
	 * @param fromTable
	 * @param fromColumn
	 * @param toTable
	 * @param toColumn
	 * @param predicate
	 */
	public void removeRelation(String fromTable, String fromColumn, String toTable, String toColumn, String predicate) {
		// need to make sure both the fromConcept and the toConcept are already defined as concepts
		// TODO: this works for RDBMS even though it only takes in the concept names because we usually perform
		// 		the addConcept call before... this is really just intended to retrieve the concept URIs from the
		// 		conceptHash as they should already there
		String fromConceptURI = addConcept(fromTable, fromColumn, null);
		String toConceptURI = addConcept(toTable, toColumn, null);
		
		// if a predicate is not defined, we create it based on the connection
		// TODO: this makes a lot of assumptions regarding the structure of the database
		// 		the assumption is only accurate for databases that we create within the tool through upload
		if(predicate == null) {
			// assume the foreign key is on the toTable
			// this will be the foreign key column in the toTable, which is the from column name + "_FK"
			// however, if from column is empty, it will be the assumption that from column is the same as from table
			
			// store the foreign key column
			String fk_Col = null;
			// this will be the column name in case it is empty and the same as the table name
			String fromCol = null;
			if(!fromColumn.isEmpty()) {
				fk_Col = fromColumn;
				fromCol = fromColumn;
			} else {
				// if the fromColumn is empty, assume it is the same as the table
				fk_Col = fromTable;
				fromCol = fromTable;
			}
			fk_Col += "_FK";
			
			// generate the predicate string
			predicate = fromTable + "." + fromCol + "." + toTable + "." + fk_Col;
		}
		
		// TODO: this always assumes the baseURI for the relationship is the default semoss uri
		// create the base relationship uri
		String baseRelationURI = SEMOSS_URI_PREFIX + DEFAULT_RELATION_CLASS;
		String predicateSubject = baseRelationURI + "/" + predicate;
		
		// now lets start to add the triples
		// lets add the triples pertaining to those numbered above
		
		// 1) now add the physical relationship URI
		engine.removeFromBaseEngine(predicateSubject, RDFS.SUBPROPERTYOF.stringValue(), baseRelationURI);
		
		// 2) now add the relationship between the two nodes
		engine.removeFromBaseEngine(fromConceptURI, predicateSubject, toConceptURI);
		
		// lastly, store it in the hash for future use
		relationHash.remove(fromTable + fromColumn + toTable + toColumn + predicate);
	}
	
	/**
	 * Adds a relationship between two concepts within the OWL
	 * @param fromConcept			For RDF: This is the name of the start concept
	 * 								For RDBMS: This is the name of the table AND column name (they are the same) for
	 * 								the start concept
	 * @param toConcept				For RDF: This is the name of the end concept
	 * 								For RDBMS: This is the name of the table AND column name (they are the same) for 
	 * 								the end concept
	 * @param predicate				The predicate for the relationship.  If this is null, the code
	 * 								will generate a predicate based on the fromConcept and toConcept.
	 * 								The format will be created based on the default loading of a RDBMS
	 * 								predicate where each concept is its own table and the column name 
	 * 								matches the table name.
	 * @return						Returns the physical URI for the relationship
	 */
	public String addRelation(String fromConcept, String toConcept, String predicate) {
		return addRelation(fromConcept, "", toConcept, "", predicate);
	}
	
	/////////////////// END ADDING RELATIONSHIP INTO THE OWL /////////////////////////////////

	
	/////////////////// ADDING PROPERTIES TO CONCEPTS IN THE OWL /////////////////////////////////
	/*
	 * This method is overloaded to make it easier to add properties for concepts into the owl file
	 * 
	 * Lets look at what triples will be added
	 * NOTE: for simplicity, I will use 'semoss:' to denote the prefix 'http://semoss.org/ontologies'
	 * 
	 * For a RDF property, call it 'P-P' on concept 'X' the following triples will be added:
	 * 1) { <semoss:Relation/Contains/P-P> <rdf:type> <semoss:Relation/Contains> }
	 * 2) { <semoss:Concept/X> <semoss:Relation/Contains> <semoss:Relation/Contains/P-P> }
	 * 3) { <semoss:Relation/Contains/P-P> <rdfs:Class> TYPE:DATA_TYPE_HERE }
	 * 4) { <semoss:Relation/Contains/P-P> <semoss:Relation/Conceptual> <semoss:Relation/Contains/PP/X> }
	 * 
	 * NOTE: IF THE PROPERTY WAS 'PP', #4 would be a self-loop and that's okay
	 * 
	 * For a RDBMS property, call it 'P' on concept with table name 'X' and column name 'Y' the following triples will be added:
	 * 1) { <semoss:Relation/Contains/P/X> <rdf:type> <semoss:Relation/Contains> }
	 * 2) { <semoss:Concept/Y/X> <semoss:Relation/Contains> <semoss:Relation/Contains/P/X> }
	 * 3) { <semoss:Relation/Contains/P/X> <rdfs:Class> TYPE:DATA_TYPE_HERE }
	 * 4) { <semoss:Relation/Contains/P/X> <semoss:Relation/Conceptual> <semoss:Relation/Contains/P/X> }
	 */

	/**
	 * This method will add a property onto a concept in the OWL file
	 * There are some differences based on how the information is used based on if it is a 
	 * RDF engine or a RDBMS engine
	 * @param tableName				For RDF: This is the name of the concept
	 * 								For RDBMS: This is the name of the table where the concept exists
	 * @param colName				For RDF: This is NOT used
	 * 								For RDBMS: This is the name of the column which contains the concept instances
	 * @param propertyCol			This will be the name of the property
	 * @param dataType				The dataType for the property
	 * @param adtlDataType			Additional data type for the property 
	 * @return						Returns the physical URI for the node
	 * 								TODO: RDF databases end up constructing the prop URI regardless.. need them to take this return
	 */
	public String addProp(String tableName, String colName, String propertyCol, String dataType, String adtlDataType, String conceptual) {
		// since RDF uses this multiple times, don't create it each time and just store it in a hash to send back
		if(!propHash.containsKey(tableName + "%" + propertyCol)) {
			// add the concept to get the physical URI
			// TODO: this also makes the assumption that the concept is already added since the dataType being passed is null
			//		is inaccurate... this is really just intended to retrieve the concept URIs from the conceptHash
			String conceptURI = addConcept(tableName, colName, null); 
			
			// create the property URI
			String property = BASE_PROPERTY_URI + "/";
			// if it is an RDBMS engine, we need to account when the table name and column name are not the same
			if(type.equals(IEngine.ENGINE_TYPE.RDBMS)) {
				// need to append the table name onto the URI
				property += propertyCol + "/" + tableName;
			} else {
				// here, it must be RDF
				// just add the property col 
				property += propertyCol;
			}
			
			// now lets start to add the triples
			// lets add the triples pertaining to those numbered above
			
			// 1) adding the property as type of base property URI
			engine.addToBaseEngine(property, RDF.TYPE.stringValue(), BASE_PROPERTY_URI);
			
			// 2) adding the property to the concept
			engine.addToBaseEngine(conceptURI, OWL.DatatypeProperty.toString(), property);
			
			// 3) adding the property data type
			String typeObject = "TYPE:" + dataType;
			engine.addToBaseEngine(property, RDFS.CLASS.stringValue(), typeObject);
			
			// 4) adding the property additional data type, if available
			if (adtlDataType != null && !adtlDataType.isEmpty()) {
				String adtlTypeObject = "ADTLTYPE:" + adtlDataType.replace("/", "{{REPLACEMENT_TOKEN}}").replace("'", "((SINGLE_QUOTE))").replace(" ", "((SPACE))");
				String adtlTypeUri = SEMOSS_URI_PREFIX + DEFAULT_PROP_CLASS + "/AdtlDataType";
				engine.addToBaseEngine(property, adtlTypeUri, adtlTypeObject);
			}
						
			// 4) now lets add the physical property to the conceptual property
			// one of the advantages of the conceptual URI is that we can specify a property in a database even 
			// if it contains any of the special characters that are not allowed in pixel
			// the conceptual property name is the clean version of the property name ... for now
			
			// get the conceptual name
			// always has the parent name so we only require properties to be unique 
			// on a given concept and not throughout the entire db
			String conceptualProp = conceptual;
			if(conceptualProp == null) {
				conceptualProp = Utility.cleanVariableString(propertyCol);
			}
			String conceptualPropertyName = conceptualProp + "/" +  Utility.cleanVariableString(tableName);
			String conceptualProperty = BASE_PROPERTY_URI + "/" + conceptualPropertyName;
			engine.addToBaseEngine(property, CONCEPTUAL_RELATION_URI, conceptualProperty);
			
			// lastly, store it in the hash for future use
			propHash.put(tableName + "%" + propertyCol, property);
		}
		
		return propHash.get(tableName + "%" + propertyCol);
	}

	public String addProp(String tableName, String colName, String propertyCol, String dataType, String adtlDataType) {
		return addProp(tableName, colName, propertyCol, dataType, adtlDataType, null);
	}
		
	/**
	 * This method will add a property onto a concept in the OWL file
	 * There are some differences based on how the information is used based on if it is a 
	 * RDF engine or a RDBMS engine
	 * @param tableName				For RDF: This is the name of the concept
	 * 								For RDBMS: This is the name of the table where the concept exists. If the concept
	 * 									doesn't exist, it is assumed the column name of the concept is the same as the table name
	 * @param propertyCol			This will be the name of the property
	 * @param dataType				The dataType for the property
	 * @param adtlDataType			Additional data type for the property 
	 * @return						Returns the physical URI for the node
	 */
	public String addProp(String tableName, String propertyCol, String dataType, String adtlDataType) {
		return addProp(tableName, "", propertyCol, dataType, adtlDataType);
	}
	
	public String addProp(String tableName, String propertyCol, String dataType) {
		return addProp(tableName, "", propertyCol, dataType, null);
	}
	
	/**
	 * This method will calculate the unique values in each column/property
	 * and add it to the owl file.
	 * 
	 * @param queryEngine
	 */
	public void addUniqueCounts(IEngine queryEngine) {
		String uniqueCountProp = SEMOSS_URI_PREFIX + DEFAULT_PROP_CLASS + "/UNIQUE";

		Vector<String> concepts = queryEngine.getConcepts(false);
		for (String conceptPhysicalUri : concepts){
			String conceptConceptualUri = queryEngine.getConceptualUriFromPhysicalUri(conceptPhysicalUri);
			String conceptualName = Utility.getInstanceName(conceptConceptualUri);
			
			// query for unique column values and dont 
			// store it if it returns null
			SelectQueryStruct qs = new SelectQueryStruct();
			QueryFunctionSelector newSelector = new QueryFunctionSelector();
			newSelector.setFunction(QueryFunctionHelper.UNIQUE_COUNT);
			newSelector.setDistinct(true);
			QueryColumnSelector innerSelector = new QueryColumnSelector();
			innerSelector.setTable(conceptualName);
			innerSelector.setColumn(SelectQueryStruct.PRIM_KEY_PLACEHOLDER);
			newSelector.addInnerSelector(innerSelector);
			qs.addSelector(newSelector);
			qs.setQsType(AbstractQueryStruct.QUERY_STRUCT_TYPE.ENGINE);
			
			Iterator<IHeadersDataRow> it = WrapperManager.getInstance().getRawWrapper(queryEngine, qs);
			if (!it.hasNext()) {
				continue;
			}
			long uniqueRows = ((Number) it.next().getValues()[0]).longValue();
			this.engine.addToBaseEngine(conceptPhysicalUri, uniqueCountProp, uniqueRows, false);
			
			List<String> properties = queryEngine.getProperties4Concept(conceptPhysicalUri, false);
			for(String propertyPhysicalUri : properties) {
				// so grab the conceptual name
				String propertyConceptualUri = queryEngine.getConceptualUriFromPhysicalUri(propertyPhysicalUri);
				// property conceptual uris are always /Column/Table
				String propertyConceptualName = Utility.getClassName(propertyConceptualUri);
				
				// query for unique column values and dont 
				// store it if it returns null
				qs = new SelectQueryStruct();
				newSelector = new QueryFunctionSelector();
				newSelector.setFunction(QueryFunctionHelper.UNIQUE_COUNT);
				newSelector.setDistinct(true);
				innerSelector = new QueryColumnSelector();
				innerSelector.setTable(conceptualName);
				innerSelector.setColumn(propertyConceptualName);
				newSelector.addInnerSelector(innerSelector);
				qs.addSelector(newSelector);
				qs.setQsType(AbstractQueryStruct.QUERY_STRUCT_TYPE.ENGINE);
				
				it = WrapperManager.getInstance().getRawWrapper(queryEngine, qs);
				if (!it.hasNext()) {
					continue;
				}
				uniqueRows = ((Number) it.next().getValues()[0]).longValue();
				this.engine.addToBaseEngine(propertyPhysicalUri, uniqueCountProp, uniqueRows, false);
			}
		}
		
		this.engine.commit();
		try {
			this.engine.exportBaseEng(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/////////////////// END ADDING PROPERTIES TO CONCEPTS INTO THE OWL /////////////////////////////////

	/////////////////// ADDITIONAL METHODS TO INSERT INTO THE OWL /////////////////////////////////
	
	/**
	 * Have one class a subclass of another class
	 * This code is really intended for RDF databases... not sure what use it will have
	 * to utilize this within an RDBMS
	 * @param childType					The child concept node
	 * @param parentType				The parent concept node
	 */
	public void addSubclass(String childType, String parentType) {
		String childURI = addConcept(childType);
		String parentURI = addConcept(parentType);
		engine.addToBaseEngine(childURI, RDFS.SUBCLASSOF.stringValue(), parentURI);
	}
	
	/////////////////// END ADDITIONAL METHODS TO INSERT INTO THE OWL /////////////////////////////////

	///////////////////// TESTING /////////////////////
	public static void main(String [] args) {
		TestUtilityMethods.loadDIHelper("C:\\workspace\\Semoss_Dev\\RDF_Map.prop");
		Owler owler = new Owler( "C:\\workspace\\Semoss_Dev\\themes_OWL.OWL", IEngine.ENGINE_TYPE.RDBMS);
		
		owler.addConcept("ADMIN_THEME ", "id", "VARCHAR(255)");
		owler.addProp("ADMIN_THEME ", "id", "theme_name", "VARCHAR(255)", null);
		owler.addProp("ADMIN_THEME ", "id", "theme_map", "CLOB", null);
		owler.addProp("ADMIN_THEME ", "id", "is_active", "BOOLEAN", null);

		// load the owl into a rfse
		RDFFileSesameEngine rfse = new RDFFileSesameEngine();
		try {
			owler.export();
		} catch (IOException e) {
			e.printStackTrace();
		}
		owler.getOwlAsString();
		rfse.openFile(owler.getOwlPath(), "RDF/XML", SEMOSS_URI_PREFIX);
	}
		
	public String getOwlAsString() {
		// this will both write the owl to a file and print it onto the console
		String owl = null;
		try {
			owl = engine.exportBaseEngAsString(true);
			System.out.println("OWL.. " + owl);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return owl;
	}
	///////////////////// END TESTING /////////////////////
}
