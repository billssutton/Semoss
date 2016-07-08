package prerna.rdf.query.builder;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import prerna.ds.QueryStruct;
import prerna.engine.api.IEngine;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.test.TestUtilityMethods;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class SQLInterpreter implements IQueryInterpreter{
	
	// core class to convert the query struct into a sql query
	QueryStruct qs = null;
	
	private static Hashtable <String,String> aliases = new Hashtable<String,String>();
	
	// keep track of processed tables used to ensure we don't re-add tables into the from string
	private Hashtable <String, String> tableProcessed = new Hashtable<String, String>();
	
	// we will keep track of the conceptual names to physical names so we don't re-query the owl multiple times
	private transient Hashtable <String, String> conceptualConceptToPhysicalMap = new Hashtable<String, String>();
	// need to also keep track of the properties
	private transient Hashtable <String, String> conceptualPropertyToPhysicalMap = new Hashtable<String, String>();
	// need to keep track of the primary key for tables
	private transient Map<String, String> primaryKeyCache = new HashMap<String, String>();

	
	// we can create a statement without an engine... but everything needs to be the physical
	// we currently only use it when the engine is null, but we could use this to query on 
	// an in-memory rdbms like an H2Frame which is not an engine
	private IEngine engine; 
	
	// where the wheres are all kept
	// key is always a combination of concept and comparator
	// and the values are values
	private Hashtable <String, String> whereHash = new Hashtable<String, String>();

	private Hashtable <String, String> relationHash = new Hashtable<String, String>();

	private transient Map<String, String[]> relationshipConceptPropertiesMap = new HashMap<String, String[]>();
	
	private String selectors = "";
	private String froms = "";
	
	
	public SQLInterpreter() {
		
	}

	public SQLInterpreter(IEngine engine) {
		this.engine = engine;
	}

	@Override
	public void setQueryStruct(QueryStruct qs) {
		this.qs = qs;
	}
	
	/**
	 * Main method to invoke to take the QueryStruct to compose the appropriate SQL query
	 */
	public String composeQuery()
	{
		/*
		 * Need to create the query... 
		 * This to consider:
		 * 1) the user is going to be using the conceptual names as defined by the OWL (if present
		 * and OWL is the improved version). This has a few consequences:
		 * 1.a) when a user enters a table name, we need to determine what the primary key is
		 * 		for that table
		 * 1.b) need to consider what tables are used within joins and which are not. this will
		 * 		determine when we add it to the from clause or if the table will be defined via 
		 * 		the join 
		 */
		
		String query = null;
		addSelectors();
		addJoins();
		addFilters();
		
		// the final step where the equation is balanced and the anamoly revealed
		query = "SELECT  DISTINCT " + selectors + "  FROM " + froms;
		boolean firstTime = true;

		Enumeration joins = relationHash.keys();
		while(joins.hasMoreElements())
		{
			String value = relationHash.get(joins.nextElement());

			if(firstTime)
			{
				query = query + " " + value;
				firstTime = false;
			}
			else
				query = query + " " + value;
		}
		
		firstTime = true;
		for (String key : whereHash.keySet())
		{
			String value = whereHash.get(key);
			
			String[] conceptKey = key.split(":::");
			String concept = conceptKey[0];
			String property = conceptKey[1];
			String comparator = conceptKey[2];
			
			String conceptString = "";
			if(comparator.trim().equals("=")) {
				conceptString = getAlias(concept) + "." + property +" IN ";
			}
			
			if(comparator.trim().equals("=") || value.contains(" OR ")) {
				value = " ( " + value + " ) ";
			}
			
			if(firstTime)
			{
				query = query + " WHERE " + conceptString + value;
				firstTime = false;
			}
			else
				query = query + " AND " + conceptString + value;
		}

		System.out.println("QUERY....  " + query);
		return query;
	}

	//////////////////////////// adding selectors //////////////////////////////////////////
	
	/**
	 * Loops through the selectors defined in the QS to add them to the selector string
	 * and considers if the table should be added to the from string
	 */
	public void addSelectors() {
		Hashtable<String, Vector<String>> selectorData = qs.selectors;
		
		// loop through every table name
		for(String tableName : selectorData.keySet()) {
			// now get all the column names for the table
			Vector<String> columns = selectorData.get(tableName);
			// now loop through and add all the column data
			for(String col : columns) {
				// now actually do the column add into the selector string
				addSelector(tableName, col);
				// adds the from if it isn't part of a join
				if(notUsedInJoin(tableName)){
					addFrom(tableName);
				}
			}
		}
	}
	
	/**
	 * Adds the selector required for a table and column name
	 * @param table				The name of the table
	 * @param colName			The column in the table
	 */
	public void addSelector(String table, String colName)
	{
		String selectorAddition = colName;
		// not sure how we get to the point where table would be null..
		// but this was here previously so i will just keep it I guess
		if(table != null) {
			String tableAlias = getAlias(table);
			// will be getting the physical column name
			String physicalColName = colName;
			// TODO: currently assuming the display name is the conceptual
			//		once we have this in the OWL, we need to add this
			String displayName = colName; 

			// if engine is not null, get the info from the engine
			if(engine != null) {
				// if the colName is the primary key placeholder
				// we will go ahead and grab the primary key from the table
				if(colName.equals(QueryStruct.PRIM_KEY_PLACEHOLDER)){
					physicalColName = getPrimKey4Table(table);
					// the display name is defaulted to the table name
					displayName = table;
				} else {
					// default assumption is the info being passed is the conceptual name
					// get the physical from the conceptual
					physicalColName = getPhysicalPropertyNameFromConceptualName(colName);
					displayName = colName;

				}
			}

			selectorAddition = tableAlias + "." + physicalColName + " AS " + displayName;
		}

		if(selectors.length() == 0) {
			selectors = selectorAddition;
		} else {
			selectors = selectors + " , " + selectorAddition;
		}
	}
	
	//////////////////////////////////// end adding selectors /////////////////////////////////////
	
	
	
	
	/////////////////////////////////// adding from ////////////////////////////////////////////////
	
	
	/**
	 * Adds the form statement for each table
	 * @param conceptualTableName			The name of the table
	 */
	private void addFrom(String conceptualTableName)
	{
		String alias = getAlias(conceptualTableName);
		// we don't want to add the from table multiple times as this is invalid in sql
		if(!tableProcessed.containsKey(conceptualTableName)) {
			tableProcessed.put(conceptualTableName, "true");
			
			// we want to use the physical table name
			String physicalTableName = getPhysicalTableNameFromConceptualName(conceptualTableName);
			
			// add the physical table name and define its unique alias into the from statement
			String fromText =  physicalTableName + "  " + alias;
			if(froms.length() > 0){
				froms = froms + " , " + fromText;
			} else {
				froms = fromText;
			}
		}
	}

	/**
	 * Determine if a table is used within a join
	 * @param tableName				The name of the table
	 * @return						Boolean true if not used in join, false if used in join
	 */
	private boolean notUsedInJoin(String tableName) {
		// need to get the physical name
		String physicalTableName = getPhysicalTableNameFromConceptualName(tableName);
		
		// need to look a the table name and compare it to all the different relationships defined via the joins
		Hashtable<String, Hashtable<String, Vector>> relationsData = qs.relations;
		for(String startConceptProperty : relationsData.keySet() ) {
			// at this point, we do not care about the type of join
			// just want to see if it will be used in a join
			Hashtable<String, Vector> joinMap = relationsData.get(startConceptProperty);
			
			for(String comparator : joinMap.keySet()) {
				Vector<String> joinColumns = joinMap.get(comparator);
				for(String endConceptProperty : joinColumns) {
					
					// get the actual join information
					// this also returns the physical names
					String[] relationships = getRelationshipConceptProperties(startConceptProperty, endConceptProperty);
					// based on the determination used for joins
					// the end table is used within the join and not the start table
					String toConcept = relationships[2];
					// so if the table entered
					if(toConcept.equals(physicalTableName)){
						return false;
					}
				}
			}
		}
		
		// if we found don't return during the above loop
		// this table is not used in a join
		return true;
	}
	
	////////////////////////////////////// end adding from ///////////////////////////////////////
	
	
	////////////////////////////////////// adding joins /////////////////////////////////////////////
	
	/**
	 * Adds the joins for the query
	 */
	public void addJoins() {
		Hashtable<String, Hashtable<String, Vector>> relationsData = qs.relations;
		// loop through all the relationships
		// realize we can be joining on properties within a table
		for(String startConceptProperty : relationsData.keySet() ) {
			// the key for this object is the specific type of join to be used
			// between this instance and all the other ones
			Hashtable<String, Vector> joinMap = relationsData.get(startConceptProperty);
			for(String comparator : joinMap.keySet()) {
				Vector<String> joinColumns = joinMap.get(comparator);
				for(String endConceptProperty : joinColumns) {
					// go through and perform the actual join
					addJoin(startConceptProperty, comparator, endConceptProperty);
				}
			}
		}
	}

	/**
	 * Adds the join to the relationHash which gets added to the query in composeQuery
	 * @param fromCol					The starting column, this can be just a table
	 * 									or table__column
	 * @param thisComparator			The comparator for the type of join
	 * @param toCol						The ending column, this can be just a table
	 * 									or table__column
	 */
	private void addJoin(String fromCol, String thisComparator, String toCol) {
		String thisWhere = "";
		
		// get the parts of the join
		String[] relConProp = getRelationshipConceptProperties(fromCol, toCol);
		String concept = relConProp[0];
		String property = relConProp[1];
		String toConcept = relConProp[2];
		String toProperty = relConProp[3];
		
		// the unique key for the join will be the concept and type of join
		// this is so we append the joins property
		String key = toConcept + thisComparator;
		
		// based on the linear approach of creating the sql
		// based on the position of the relationships
		// if the toConcept isn't defined in the from selection
		// we need to add it or the query will break
		if(notUsedInJoin(concept)){
			addFrom(concept);
		}
		
		if(!relationHash.containsKey(key))
		{
			String compName = thisComparator.replace(".", "  ");	
			thisWhere = compName + "  " + toConcept+ " " + getAlias(toConcept) + " ON " + getAlias(concept) + "." + property + " = " + getAlias(toConcept) + "." + toProperty;			
		}
		else
		{
			thisWhere = relationHash.get(key);
			thisWhere = thisWhere + " AND " + getAlias(concept) + "." + property + " = " + getAlias(toConcept) + "." + toProperty;			
		}
		relationHash.put(key, thisWhere);
	}
	
	////////////////////////////////////////// end adding joins ///////////////////////////////////////
	
	
	////////////////////////////////////////// adding filters ////////////////////////////////////////////
	
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter
	//TODO: should go back and add comments for filter

	public void addFilters()
	{
		Enumeration <String> concepts = qs.andfilters.keys();
		
		while(concepts.hasMoreElements())
		{
			String concept_property = concepts.nextElement();
			
			// inside this is a hashtable of all the comparators
			Hashtable <String, Vector> compHash = qs.andfilters.get(concept_property);
			Enumeration <String> comps = compHash.keys();
			
			// when adding implicit filtering from the dataframe as a pretrans that gets appended into the QS
			// we store the value without the parent__, so need to check here if it is stored as a prop in the engine
			if(engine != null) {
				String parent = engine.getParentOfProperty(concept_property);
				if(parent != null) {
					concept_property = Utility.getClassName(parent) + "__" + concept_property;
				}
			}
			String[] conProp = getConceptProperty(concept_property);
			String concept = conProp[0];
			String property = conProp[1];
			
			// the comparator between the concept is an and so block it that way
			// I need to specify to it that I am doing something new here
			// ok.. what I mean is this
			// say I have > 50
			// and then  < 80
			// I need someway to tell the adder that this is an end 
			while(comps.hasMoreElements())
			{
				String thisComparator = comps.nextElement();
				
				Vector options = compHash.get(thisComparator);
				
				// and the final one goes here					
				
				// now I get all of them and I start adding them
				// usually these are or ?
				// so I am saying if something is

				if(thisComparator == null || thisComparator.trim().equals("=")) {
					for(int optIndex = 0;optIndex < options.size(); optIndex++){
						addEqualsFilter(concept, property, thisComparator, options.get(optIndex) + "");
					}
				} else {
					for(int optIndex = 0;optIndex < options.size(); optIndex++){
						addFilter(concept, property, thisComparator, options.get(optIndex)+"");
					}
				}
			}
		}
	}
	
	
	private void addFilter(String concept, String property, String thisComparator, String object) {
		String thisWhere = "";
		String key = concept +":::"+ property +":::"+ thisComparator;
		if(!whereHash.containsKey(key))
		{
			if(object instanceof String) // ok this is a string
			{
				object = object.replace("\"", ""); // get rid of the space
				object = object.replaceAll("'", "''");
				object = object.trim();
				object = "\'" + object + "\'";
				//thisWhere = getAlias(concept) + "." + property + " " + thisComparator + " " + object;
			}
			thisWhere = getAlias(concept) + "." + property + " " + thisComparator + " " + object;		
			
		}
		else
		{
			thisWhere = whereHash.get(key);
			if(object instanceof String) // ok this is a string
			{
				object = object.replaceAll("\"", ""); // get rid of the space
				object = object.replaceAll("'", "''");
				object = object.trim();
				object = "\'" + object + "\'";
				//thisWhere = getAlias(concept) + "." + property + " " + thisComparator + " " + object;
			}
			thisWhere = thisWhere + " OR " + getAlias(concept) + "." + property + " " + thisComparator + " " + object;						
		}
	//	System.out.println("WHERE " + thisWhere);
		whereHash.put(key, thisWhere);
	}
	
	//we want the filter query to be: "... where table.column in ('value1', 'value2', ...) when the comparator is '='
	private void addEqualsFilter(String concept, String property, String thisComparator, String object) {
		String thisWhere = "";
		String key = concept +":::"+ property +":::"+ thisComparator;
		if(!whereHash.containsKey(key))
		{
			if(object instanceof String) // ok this is a string
			{
				object = object.replace("\"", ""); // get rid of the space
				object = object.replaceAll("'", "''");
				object = object.trim();
				object = "\'" + object + "\'";
				//thisWhere = getAlias(concept) + "." + property + " " + thisComparator + " " + object;
			}
			thisWhere = object;		
			
		}
		else
		{
			thisWhere = whereHash.get(key);
			if(object instanceof String) // ok this is a string
			{
				object = object.replaceAll("\"", ""); // get rid of the space
				object = object.replaceAll("'", "''");
				object = object.trim();
				object = "\'" + object + "\'";
				//thisWhere = getAlias(concept) + "." + property + " " + thisComparator + " " + object;
			}
			thisWhere = thisWhere + ", " +object;						
		}
	//	System.out.println("WHERE " + thisWhere);
		whereHash.put(key, thisWhere);
	}
	
	////////////////////////////////////// end adding filters ////////////////////////////////////////////

	
	//////////////////////////////////// caching utility methods /////////////////////////////////////////
	
	/**
	 * Get the physical name of the 
	 * @param conceptualTableName
	 * @return
	 */
	private String getPhysicalTableNameFromConceptualName(String conceptualTableName) {
		// if engine present
		// get the appropriate physical storage name for the table
		if(engine != null) {
			// if we already have it, just grab from hash
			if(conceptualConceptToPhysicalMap.containsKey(conceptualTableName)) {
				return conceptualConceptToPhysicalMap.get(conceptualTableName);
			}
			
			// we dont have it.. so query for it
			String conceptualURI = "http://semoss.org/ontologies/Concept/" + conceptualTableName;
			String tableURI = this.engine.getPhysicalUriFromConceptualUri(conceptualURI);
			
			// table name is the instance name of the URI
			String tableName = Utility.getInstanceName(tableURI);
			
			// since we also have the URI, just store the primary key as well if we haven't already
			if(!primaryKeyCache.containsKey(conceptualTableName)) {
				// will most likely be used
				String primKey = "";
				// TODO: this is the old OWL
				// the conceptual nodes do not exist so it just returns the same thing
				// in this case, the primKey is the same as the table name
				if(tableURI.equals(conceptualURI)) {
					primKey = tableName;
				} else {
					// default in new OWL is class name is the prim key
					primKey = Utility.getClassName(tableURI);
				}
				
				primaryKeyCache.put(conceptualTableName, primKey);
			}
			
			// store the physical name as well in case we get it later
			conceptualConceptToPhysicalMap.put(conceptualTableName, tableName);
			return tableName;
		} else {
			// no engine is defined, just return the value
			return conceptualTableName;
		}
	}
	
	/**
	 * Get the physical name for a property
	 * @param columnConceptualName					The conceptual name of the property
	 * @return										The physical name of the property
	 */
	private String getPhysicalPropertyNameFromConceptualName(String columnConceptualName) {
		if(engine != null) {
			// if we already have it, just grab from hash
			if(conceptualPropertyToPhysicalMap.containsKey(columnConceptualName)) {
				return conceptualPropertyToPhysicalMap.get(columnConceptualName);
			}
			
			// we don't have it... so query for it
			String propertyConceptualURI = "http://semoss.org/ontologies/Relation/Contains/" + columnConceptualName;
			String colURI = this.engine.getPhysicalUriFromConceptualUri(propertyConceptualURI);
			String colName = null;
			
			// TODO: this occurs when we have the old version of the OWL file
			if(propertyConceptualURI.equals(colURI)) {
				// in this case, just use the instance name
				colName = Utility.getInstanceName(colURI);
			} else {
				// this should be the default case once new OWL is only possibility
				colName = Utility.getClassName(colURI);
			}
			
			conceptualPropertyToPhysicalMap.put(columnConceptualName, colName);
			return colName;
		} else {
			// no engine is defined, just return the value
			return columnConceptualName;
		}
	}
	
	/**
	 * Get the primary key from the conceptual table name
	 * @param table						The conceptual table name
	 * @return							The physical table name
	 */
	private String getPrimKey4Table(String conceptualTableName){
		if(primaryKeyCache.containsKey(conceptualTableName)){
			return primaryKeyCache.get(conceptualTableName);
		}
		else if (engine != null){
			// we dont have it.. so query for it
			String conceptualURI = "http://semoss.org/ontologies/Concept/" + conceptualTableName;
			String tableURI = this.engine.getPhysicalUriFromConceptualUri(conceptualURI);
			
			// since we also have the URI, just store the primary key as well
			// will most likely be used
			String primKey = "";
			// TODO: this is the old OWL
			// the conceptual nodes do not exist so it just returns the same thing
			// in this case, the primKey is the same as the table name
			if(tableURI.equals(conceptualURI)) {
				primKey = Utility.getInstanceName(tableURI);
			} else {
				// default in new OWL is class name is the prim key
				primKey = Utility.getClassName(tableURI);
			}
			
			primaryKeyCache.put(conceptualTableName, primKey);
			
			return primKey;
		}
		return conceptualTableName;
	}
	
	/**
	 * Get the alias for each table name
	 * @param tableName				The table name
	 * @return						The alias for the table name
	 */
	public String getAlias(String tableName)
	{
		// alias already exists
		if(aliases.containsKey(tableName)) {
			return aliases.get(tableName);
		} else {
			boolean aliasComplete = false;
			int count = 0;
			String tryAlias = "";
			while(!aliasComplete)
			{
				if(tryAlias.length()>0){
					tryAlias+="_"; //prevent an error where you may create an alias that is a reserved word (ie, we did this with "as")
				}
				tryAlias = (tryAlias + tableName.charAt(count)).toUpperCase();
				aliasComplete = !aliases.containsValue(tryAlias);
				count++;
			}
			aliases.put(tableName, tryAlias);
			return tryAlias;
		}
	}
	////////////////////////////// end caching utility methods //////////////////////////////////////
	
	
	/////////////////////////////// other utility methods /////////////////////////////////////////
	
	/**
	 * Gets the 4 parts needed to define a relationship
	 * 1) the start table
	 * 2) the start tables column
	 * 3) the end table
	 * 4) the end tables column
	 * 
	 * We have 3 situations
	 * 1) If all 4 parts are defined within the fromString and toString parameters by utilizing
	 * a "__", then it just converts to the physical names and is done
	 * 2) If startTable and start column is defined but endTable/endColumn is not, it assumes the input
	 * for endString is a concept and should bind on its primary key.  This is analogous for when endTable
	 * and end column are defined but the startString is not.
	 * 3) Neither are defined, so we must use the OWL to define the relationship between the 2 tables
	 * 
	 * @param fromString				The start string defining the table/column
	 * @param toString					The end string defining the table/column
	 * @return							String[] of length 4 where the indices are
	 * 									[startTable, startCol, endTable, endCol]
	 */
	private String[] getRelationshipConceptProperties(String fromString, String toString){
		if(relationshipConceptPropertiesMap.containsKey(fromString + "__" + toString)) {
			return relationshipConceptPropertiesMap.get(fromString + "__" + toString);
		}
		
		String fromTable = null;
		String fromCol = null;
		String toTable = null;
		String toCol = null;
		
		// see if both the table name and column name are specified for the fromString
		if(fromString.contains("__")){
			fromTable = fromString.substring(0, fromString.indexOf("__"));
			fromCol = fromString.substring(fromString.indexOf("__")+2);
			
			// need to make these the physical names
			if(engine != null) {
				fromTable = getPhysicalTableNameFromConceptualName(fromTable);
				fromCol = getPhysicalPropertyNameFromConceptualName(fromCol);
			}
		}
		
		// see if both the table name and column name are specified for the toString
		if(toString.contains("__")){
			toTable = toString.substring(0, toString.indexOf("__"));
			toCol = toString.substring(toString.indexOf("__")+2);
			
			// need to make these the physical names
			if(engine != null) {
				toTable = getPhysicalTableNameFromConceptualName(toTable);
				toCol = getPhysicalPropertyNameFromConceptualName(toCol);
			}
		}
		
		// if both have table and property defined, then we know exactly what we need to do
		// for the join... so we are done!
		
		// however, if one has a property specified and the other doesn't
		// then we want to connect the one table with col specified to the other table 
		// using the primary key of that table
		// lets try this for both cases of either toTable or fromTable not being specified 
		if(fromTable != null && toTable == null){
			String[] toConProp = getConceptProperty(toString);
			toTable = toConProp[0];
			toCol = toConProp[1];
		}
		
		else if(fromTable == null && toTable != null){
			String[] fromConProp = getConceptProperty(fromString);
			fromTable = fromConProp[0];
			fromCol = fromConProp[1];
		}
		
		// if neither has a property specified, use owl to look up foreign key relationship
		else if(engine != null && (fromCol == null && toCol == null)) // in this case neither has a property specified. time to go to owl to get fk relationship
		{
			String fromURI = null;
			String toURI = null;
			
			String fromConceptual = "http://semoss.org/ontologies/Concept/" + fromString;
			String toConceptual = "http://semoss.org/ontologies/Concept/" + toString;
			
			fromURI = this.engine.getPhysicalUriFromConceptualUri(fromConceptual);
			toURI = this.engine.getPhysicalUriFromConceptualUri(toConceptual);

			// TODO: this occurs for old OWL
			// should not have to perform this in the future
			if(fromURI.equals(fromConceptual) && toURI.equals(toConceptual)) {
				fromURI = this.engine.getConceptUri4PhysicalName(fromString);
				toURI = this.engine.getConceptUri4PhysicalName(toString);

			}
			
			// need to figure out what the predicate is from the owl
			// also need to determine the direction of the relationship -- if it is forward or backward
			String query = "SELECT ?relationship WHERE {<" + fromURI + "> ?relationship <" + toURI + "> } ORDER BY DESC(?relationship)";
			System.out.println(query);
			TupleQueryResult res = (TupleQueryResult) this.engine.execOntoSelectQuery(query);
			String predURI = " unable to get pred from owl for " + fromURI + " and " + toURI;
			try {
				if(res.hasNext()){
					predURI = res.next().getBinding(res.getBindingNames().get(0)).getValue().toString();
				}
				else {
					query = "SELECT ?relationship WHERE {<" + toURI + "> ?relationship <" + fromURI + "> } ORDER BY DESC(?relationship)";
					System.out.println(query);
					res = (TupleQueryResult) this.engine.execOntoSelectQuery(query);
					if(res.hasNext()){
						predURI = res.next().getBinding(res.getBindingNames().get(0)).getValue().toString();
					}
				}
			} catch (QueryEvaluationException e) {
				System.out.println(predURI);
			}
			String[] predPieces = Utility.getInstanceName(predURI).split("[.]");
			fromTable = predPieces[0];
			fromCol = predPieces[1];
			toTable = predPieces[2];
			toCol = predPieces[3];
		}
		
		String[] retArr = new String[]{fromTable, fromCol, toTable, toCol};
		relationshipConceptPropertiesMap.put(fromString + "__" + toString, retArr);
		
		return retArr;
	}
	
	
	/**
	 * Returns the physical concept name and property name for a given input
	 * If the input contains a "__" it returns the physical from both the 
	 * the concept and the property
	 * If the input doesn't contain a "__", get the concept and the primary key 
	 * @param concept_property				The input string
	 * @return								String[] containing the concept physical
	 * 										at index 0 and property physical at index 1
	 */
	private String[] getConceptProperty(String concept_property) {
		String conceptPhysical = null;
		String propertyPhysical = null;
		
		// if it contains a "__"
		// break the string and get the physical for both parts
		if(concept_property.contains("__")) {
			String concept = concept_property.substring(0, concept_property.indexOf("__"));
			String property = concept_property.substring(concept_property.indexOf("__")+2);
			
			conceptPhysical = getPhysicalTableNameFromConceptualName(concept);
			propertyPhysical = getPhysicalPropertyNameFromConceptualName(property);
		} else {
			// if it doesn't contain a "__", then it is just a concept
			// get the physical and the prim key
			conceptPhysical = getPhysicalTableNameFromConceptualName(concept_property);
			propertyPhysical = getPrimKey4Table(concept_property);
		}
		
		return new String[]{conceptPhysical, propertyPhysical};
	}
	
	////////////////////////////////////////// end other utility methods ///////////////////////////////////////////
	
	
	///////////////////////////////////////// test method /////////////////////////////////////////////////
	
	public static void main(String[] args) {
		
		// load in the engine
		TestUtilityMethods.loadDIHelper();

		//TODO: put in correct path for your database
		String engineProp = "C:\\workspace\\Semoss_Dev\\db\\test_this.smss";
		RDBMSNativeEngine coreEngine = new RDBMSNativeEngine();
		coreEngine.setEngineName("test_this");
		coreEngine.openDB(engineProp);
		DIHelper.getInstance().setLocalProperty("test_this", coreEngine);
		
		
		QueryStruct qs = new QueryStruct();
		qs.addSelector("MOVIE_RESULTS", null);
		qs.addSelector("MOVIE_RESULTS", "Title");
		qs.addSelector("MOVIE_RESULTS", "Movie_Budget");
		qs.addSelector("MOVIE_CHARACTERISTICS", null);
		qs.addSelector("MOVIE_CHARACTERISTICS", "Year");
		qs.addRelation("MOVIE_RESULTS__Title", "MOVIE_CHARACTERISTICS__Title", "inner.join");
		
		IQueryInterpreter qi = coreEngine.getQueryInterpreter();
		qi.setQueryStruct(qs);
		String query = qi.composeQuery();
		
		System.out.println(query);
	}
	
	
	
	
	///////////////////////////////////////// end test methods //////////////////////////////////////////////
	

}