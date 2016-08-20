/*******************************************************************************
 * Copyright 2015 Defense Health Agency (DHA)
 *
 * If your use of this software does not include any GPLv2 components:
 * 	Licensed under the Apache License, Version 2.0 (the "License");
 * 	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 *
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 * ----------------------------------------------------------------------------
 * If your use of this software includes any GPLv2 components:
 * 	This program is free software; you can redistribute it and/or
 * 	modify it under the terms of the GNU General Public License
 * 	as published by the Free Software Foundation; either version 2
 * 	of the License, or (at your option) any later version.
 *
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *******************************************************************************/
package prerna.nameserver;

import org.openrdf.model.vocabulary.RDF;

import prerna.util.Constants;

public final class MasterDatabaseQueries extends MasterDatabaseURIs {

	private MasterDatabaseQueries() {
		
	}
	
	// USED IN MasterDBHelper.java, AddToMasterDB.java, NameServer.java
	
	public static final String MC_PARENT_CHILD_QUERY = "SELECT DISTINCT ?parentMC ?childMC WHERE { {?parentMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?parentMC <http://semoss.org/ontologies/Relation/ParentOf> ?childMC} }";
	
	public static final String KEYWORD_NOUN_QUERY = "SELECT DISTINCT ?keyword ?mc WHERE { {?keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?mc <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?mc} }";
	
	public static final String GET_RELATED_KEYWORDS_AND_THEIR_NOUNS = "SELECT DISTINCT ?engine ?retKeywords WHERE { BIND(<@KEYWORD@> AS ?Keyword) {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?Noun} {?Noun <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Noun <http://semoss.org/ontologies/Relation/HasTopHypernym> ?MC} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC} {?retKeywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?retKeywords <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC} {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeywords} }";
	public static final String GET_RELATED_KEYWORD_AND_THEIR_NOUNS_NO_RECURSION = "SELECT DISTINCT ?engine ?retKeywords WHERE { BIND(<@KEYWORD@> AS ?Keyword) {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?Noun} {?Noun <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Noun <http://semoss.org/ontologies/Relation/HasTopHypernym> ?MC} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?retKeywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?retKeywords <http://semoss.org/ontologies/Relation/ComposedOf> ?MC} {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeywords} }"; 
	public static final String GET_RELATED_KEYWORDS_TO_SET_AND_THEIR_NOUNS = "SELECT DISTINCT ?engine ?retKeywords WHERE { {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?Noun} {?Noun <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Noun <http://semoss.org/ontologies/Relation/HasTopHypernym> ?MC} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC} {?retKeywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?retKeywords <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC} {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeywords} } BINDINGS ?Keyword {@BINDINGS@}";
	public static final String GET_RELATED_KEYWORDS_TO_SET_AND_THEIR_NOUNS_NO_RECURSION = "SELECT DISTINCT ?engine ?retKeywords WHERE { {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?Noun} {?Noun <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Noun <http://semoss.org/ontologies/Relation/HasTopHypernym> ?MC} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?retKeywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?retKeywords <http://semoss.org/ontologies/Relation/ComposedOf> ?MC} {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeywords} } BINDINGS ?Keyword {@BINDINGS@}";

	public static final String ENGINE_API_QUERY = "SELECT DISTINCT ?Engine ?API WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Engine <http://semoss.org/ontologies/Relation/Contains/API> ?API}}";
	public static final String ENGINE_LIST_QUERY = "SELECT DISTINCT ?Engine WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/meta/engine>} }";
	public static final String ENGINE_TIMESTAMP_QUERY = "SELECT DISTINCT ?engine ?time WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Contains/TimeStamp> ?time } }";
	
	public static final String GET_ALL_KEYWORDS = "SELECT DISTINCT ?keyword WHERE { {?keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} }";
	public static final String GET_ALL_KEYWORDS2 = "SELECT ?conceptLogical WHERE {{?conceptComposite <http://semoss.org/ontologies/Relation/presentin> ?engine}"
														//+ "{?conceptComposite ?rel2 <" + Constants.CONCEPT_URI + ">}"
														+ "{?conceptComposite <" + RDF.TYPE + "> ?concept}"
														+ "{?concept <http://semoss.org/ontologies/Relation/logical> ?conceptLogical}"
														+ "}";
	public static final String GET_ALL_KEYWORDS_AND_ENGINES = "SELECT DISTINCT ?engine ?keywords WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>}  {?keywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?engine <http://semoss.org/ontologies/Relation/Has> ?keywords} } ORDER BY ?engine";
	public static final String GET_ALL_KEYWORDS_AND_ENGINES2 = "SELECT ?engine ?conceptLogical ?concept WHERE {{?conceptComposite <http://semoss.org/ontologies/Relation/presentin> ?engine}"
																//+ "{?conceptComposite ?rel2 <" + Constants.CONCEPT_URI + ">}"
																+ "{?conceptComposite <" + RDF.TYPE + "> ?concept}"
																+ "{?concept <http://semoss.org/ontologies/Relation/conceptual> ?conceptLogical}"
																+ "}";
														
	
	// ONLY USED IN DeleteFromMasterDB.java class
	//queries for relations to delete
	public static final String KEYWORDS_QUERY = "SELECT DISTINCT ?Keyword WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Engine ?p ?Keyword}}";
	public static final String API_QUERY = "SELECT DISTINCT ?API WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Engine <http://semoss.org/ontologies/Relation/Contains/API> ?API}}";
	//queries to clean up the keywords after engines deleted
	public static final String KEYWORDS_WITHOUT_ENGINES_QUERY = "SELECT DISTINCT ?Keyword WHERE {{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} OPTIONAL{{?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Engine <http://semoss.org/ontologies/Relation/Has> ?Keyword}}FILTER(!BOUND(?Engine))}";
	public static final String MC_KEYWORDS_QUERY = "SELECT DISTINCT ?MasterConcept ?Keyword WHERE {{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?MasterConcept <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?MasterConcept}} BINDINGS ?Keyword {@BINDINGS@}";
	public static final String KEYWORDS_TYPE_QUERY = "SELECT DISTINCT ?Keyword ?Type WHERE {{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Keyword <http://semoss.org/ontologies/Relation/Has> ?Type}} BINDINGS ?Keyword {@BINDINGS@}";
	//queries to clean up MCs on a deep clean
	public static final String MCS_WITHOUT_KEYWORDS_QUERY = "SELECT DISTINCT ?MC WHERE { {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}MINUS{SELECT DISTINCT ?MC WHERE{{?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {{?childMC  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC}{?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC}}UNION{{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>}{?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?MC}}}}}";
	public static final String PARENT_CHILD_MC_QUERY = "SELECT DISTINCT ?ParentMC ?ChildMC WHERE {{?ParentMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}{?ChildMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?ParentMC <http://semoss.org/ontologies/Relation/ParentOf> ?ChildMC}} BINDINGS ?ChildMC {@BINDINGS@}";
	public static final String MC_TOP_HYPERNYM_MC_QUERY = "SELECT DISTINCT ?MC ?TopHypernymMC WHERE {{?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}{?TopHypernymMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://semoss.org/ontologies/Relation/HasTopHypernym> ?TopHypernymMC}} BINDINGS ?MC {@BINDINGS@}";
	public static final String ENGINE_RELATIONS = "SELECT DISTINCT ?Engine ?Has ?EngineRelations WHERE { {?EngineRelations <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/EngineRelation>} BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?Engine ?Has ?EngineRelations} {?Has <http://www.w3.org/2000/01/rdf-schema#label> ?label } }";
	// END USED IN DELETE FROM MASTER DB CLASS

	
	//Search for objects of a relationship with bindings on subject and engine
	public static final String GET_OBJECTS_OF_RELATIONSHIP = "SELECT DISTINCT ?relName ?object WHERE { BIND(<@CONCEPT@> AS ?concept) {?concept <http://semoss.org/ontologies/Relation/Provides> ?engineRelation} {?engineRelation <http://semoss.org/ontologies/Relation/Consumes> ?object} {<@ENGINE@> <http://semoss.org/ontologies/Relation/Has> ?engineRelation} {?engineRelation <http://semoss.org/ontologies/Relation/Contains/Name> ?relName} }";

	//Search for subjects of a relationship with bindings on object and engine
	public static final String GET_SUBJECTS_OF_RELATIONSHIP = "SELECT DISTINCT ?relName ?object WHERE { BIND(<@CONCEPT@> AS ?concept) {?engineRelation <http://semoss.org/ontologies/Relation/Consumes> ?concept} {?object <http://semoss.org/ontologies/Relation/Provides> ?engineRelation} {<@ENGINE@> <http://semoss.org/ontologies/Relation/Has>  ?engineRelation} {?engineRelation <http://semoss.org/ontologies/Relation/Contains/Name> ?relName} }";
	
	
	
	//////// EVERYTHING BELOW IS LEGACY CODE AND NOT ACTIVELY USED ////////
	
	// USED WHEN BE WAS DETERMINING THE METAMODEL... is used in DataStructureFromCSV which exists in DatabaseUploader.java but the call is never invoked by FE
	public static final String GET_NUM_KEYWORDS_ENGINE_INCLUDES = "SELECT DISTINCT ?engine (COUNT(?retKeyword) AS ?NumKeywords) WHERE {{?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?retKeyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeyword}} GROUP BY ?engine ORDER BY DESC(?NumKeywords) BINDINGS ?retKeyword {@BINDINGS@}";
	public static final String GET_ALL_RELATIONSHIPS_FOR_ENGINE = "SELECT DISTINCT ?relationship ?inKeywordURI ?outKeywordURI WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> as ?engine){?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>}{?engineRelation <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/EngineRelation>}{?engine <http://semoss.org/ontologies/Relation/Has> ?engineRelation}{?engineRelation <http://semoss.org/ontologies/Relation/Contains/Name> ?relationship}{?inKeywordURI <http://semoss.org/ontologies/Relation/Provides> ?engineRelation}{?engineRelation <http://semoss.org/ontologies/Relation/Consumes> ?outKeywordURI}}";
	
	// USED IN SEARCH ENGINE MASTER DB... class is no longer used... but class still exists so not commenting out
//	public static final String GET_INSIGHTS_FOR_KEYWORDS = "SELECT DISTINCT ?Engine ?InsightLabel ?Keyword ?PerspectiveLabel ?Viz WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?Insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?Perspective} {?Perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?Insight} {?Insight <INSIGHT:PARAM> ?Param}{?Param <PARAM:TYPE> ?Type} {?Keyword <http://semoss.org/ontologies/Relation/Has> ?Type}{?Perspective <http://semoss.org/ontologies/Relation/Contains/Label> ?PerspectiveLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Label> ?InsightLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?Viz}} BINDINGS ?Keyword {@KEYWORDS@}";
//	public static final String GET_ALL_INSIGHTS = "SELECT DISTINCT ?Engine ?InsightLabel ?Keyword ?PerspectiveLabel ?Viz WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?Insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?Perspective} {?Perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?Insight} {?Insight <INSIGHT:PARAM> ?Param}{?Param <PARAM:TYPE> ?Type} {?Keyword <http://semoss.org/ontologies/Relation/Has> ?Type}{?Perspective <http://semoss.org/ontologies/Relation/Contains/Label> ?PerspectiveLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Label> ?InsightLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?Viz}} ";
//	public static final String GET_ENGINE_CONCEPTS_AND_SAMPLE_INSTANCE = "SELECT DISTINCT ?concept (SAMPLE(?instance) AS ?example) WHERE { {?concept <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://semoss.org/ontologies/Concept>} {?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?concept} } GROUP BY ?concept";
//	public static final String INSTANCE_EXISTS_QUERY = "SELECT DISTINCT ?keyword ?s WHERE { {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?keyword ;} } BINDINGS ?s {@BINDINGS@}";
//	public static final String GET_ALL_MASTER_CONCEPTS = "SELECT DISTINCT ?mc WHERE { {?mc <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} }";
//	public static final String GET_ALL_KEYWORDS_FROM_MC_LIST = "SELECT DISTINCT ?Keyword WHERE { { {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC} {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC} } UNION { {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?MC} } } BINDINGS ?MC {@BINDINGS@}";

	// NO LONGER USED... INSIGHTS DO NOT EXIST IN LOCAL MASTER ANYMORE
//	public static final String PERSPECTIVES_QUERY = "SELECT DISTINCT ?Perspective WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>}{?Engine ?p ?Perspective}}";
//	public static final String INSIGHTS_QUERY = "SELECT DISTINCT ?Insight WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>}{?Engine ?p ?Insight}}";

	//Auth Queries
//	public static final String GET_USER_QUERY = "SELECT ID, NAME, EMAIL, TYPE FROM USER WHERE ID=@ID@";
	
	//User activity tracking queries
//	public static final String GET_USER_INSIGHT = "SELECT ?userinsight WHERE { BIND(<" + MasterDatabaseURIs.USERINSIGHT_URI + "/@USERINSIGHT@> as ?userinsight) {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + MasterDatabaseURIs.USERINSIGHT_URI + ">} }";
//	public static final String GET_USER_INSIGHT_EXECUTED_COUNT = "SELECT ?count ?date WHERE { BIND(<" + MasterDatabaseURIs.USERINSIGHT_URI + "/@USERINSIGHT@> as ?userinsight) {?userinsight <" + MasterDatabaseURIs.USERINSIGHT_EXECUTION_COUNT_PROP_URI + "> ?count} OPTIONAL { {?userinsight <" + MasterDatabaseURIs.USERINSIGHT_LAST_EXECUTED_DATE_PROP_URI + "> ?date} } }";
//	public static final String GET_USER_INSIGHT_LAST_EXECUTED_DATE = "SELECT ?date WHERE { BIND(<" + MasterDatabaseURIs.USERINSIGHT_URI + "/@USERINSIGHT@> as ?userinsight) {?userinsight <" + MasterDatabaseURIs.USERINSIGHT_LAST_EXECUTED_DATE_PROP_URI + "> ?date} }";
//	public static final String GET_USER_INSIGHTS_FOR_ENGINE = "SELECT DISTINCT ?insight ?label ?perspective ?layout (SUM(?count) as ?total) WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> as ?engine) {?perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?perspective} {?perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?insight} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?insight <http://semoss.org/ontologies/Relation/Contains/Label> ?label} {?insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?layout} {?userinsight <http://semoss.org/ontologies/Relation/Contains/ExecutionCount> ?count} } GROUP BY ?insight ?label ?perspective ?layout ORDER BY DESC(?total) LIMIT @LIMIT@";
//	public static final String GET_ALL_USER_INSIGHTS = "SELECT DISTINCT ?insight ?label ?perspective ?layout (SUM(?count) as ?total) WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?perspective} {?perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?insight} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?insight <http://semoss.org/ontologies/Relation/Contains/Label> ?label} {?insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?layout} {?userinsight <http://semoss.org/ontologies/Relation/Contains/ExecutionCount> ?count} } GROUP BY ?insight ?label ?perspective ?layout ORDER BY DESC(?total) LIMIT @LIMIT@";
//	public static final String GET_USER_INSIGHTS_FOR_FEED = "SELECT DISTINCT ?insight ?label ?perspective ?layout (SUM(?count) as ?total) WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?perspective} {?perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?insight} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?insight <http://semoss.org/ontologies/Relation/Contains/Label> ?label} {?insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?layout} {?userinsight <http://semoss.org/ontologies/Relation/Contains/ExecutionCount> ?count} {?userinsight <http://semoss.org/ontologies/Relation/Contains/PublishVisibility> '@VISIBILITY@'} } GROUP BY ?insight ?label ?perspective ?layout ORDER BY DESC(?total) LIMIT @LIMIT@";
//	public static final String GET_VISIBILITY_FOR_USERINSIGHT = "SELECT ?visibility (COALESCE(?pubdate, '') as ?pubdate) WHERE { BIND(<http://semoss.org/ontologies/Concept/UserInsight/@USERINSIGHT@> as ?userinsight) {?userinsight <http://semoss.org/ontologies/Relation/Contains/PublishVisibility> ?visibility} OPTIONAL { {?userinsight <http://semoss.org/ontologies/Relation/Contains/PublishDate> ?pubdate} } }";
//	public static final String GET_ALL_INSIGHTS_FOR_BROWSE = "SELECT DISTINCT ?insight ?label ?layout ?visibility (SUM(?total) as ?total) ?engine WHERE { SELECT DISTINCT ?insight ?label ?layout (COALESCE(?vis, 'me') as ?visibility) (COALESCE(?count, \"0\"^^xsd:double) as ?total) ?engine WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>}  {?insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?perspective} {?perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?insight} {?insight <http://semoss.org/ontologies/Relation/Contains/Label> ?label} {?insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?layout} OPTIONAL { {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?userinsight <http://semoss.org/ontologies/Relation/Contains/ExecutionCount> ?count} } OPTIONAL { {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?userinsight <http://semoss.org/ontologies/Relation/Contains/PublishVisibility> ?vis} } } }  GROUP BY ?insight ?label ?layout ?visibility ?engine ORDER BY ?engine DESC(?total)";
//	public static final String GET_INSIGHT_DETAILS = "SELECT DISTINCT ?description ?tags ?lastViewed WHERE { BIND(<http://semoss.org/ontologies/Concept/Insight/@INSIGHT@> as ?insight) OPTIONAL { {?insight <http://semoss.org/ontologies/Relation/Contains/Description> ?description} } OPTIONAL { {?insight <http://semoss.org/ontologies/Relation/Contains/Tags> ?tags} } OPTIONAL { BIND(<http://semoss.org/ontologies/Concept/UserInsight/@USER@-@INSIGHT@> as ?userinsight) {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?userinsight <http://semoss.org/ontologies/Relation/Contains/LastExecutedDate> ?lastViewed} } }";
}
	
