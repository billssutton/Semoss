/*******************************************************************************
 * Copyright 2015 SEMOSS.ORG
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

import prerna.auth.EnginePermission;


public final class MasterDatabaseQueries extends MasterDatabaseURIs {

	private MasterDatabaseQueries() {
		
	}
	
	public static final String MC_PARENT_CHILD_QUERY = "SELECT DISTINCT ?parentMC ?childMC WHERE { {?parentMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?parentMC <http://semoss.org/ontologies/Relation/ParentOf> ?childMC} }";
	public static final String KEYWORD_NOUN_QUERY = "SELECT DISTINCT ?keyword ?mc WHERE { {?keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?mc <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?mc} }";
	
	public static final String GET_RELATED_KEYWORDS_AND_THEIR_NOUNS = "SELECT DISTINCT ?engine ?retKeywords WHERE { BIND(<@KEYWORD@> AS ?Keyword) {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?Noun} {?Noun <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Noun <http://semoss.org/ontologies/Relation/HasTopHypernym> ?MC} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC} {?retKeywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?retKeywords <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC} {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeywords} }";
	public static final String GET_RELATED_KEYWORDS_TO_SET_AND_THEIR_NOUNS = "SELECT DISTINCT ?engine ?retKeywords WHERE { {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?Noun} {?Noun <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Noun <http://semoss.org/ontologies/Relation/HasTopHypernym> ?MC} {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC} {?retKeywords <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?retKeywords <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC} {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeywords} } BINDINGS ?Keyword {@BINDINGS@}";
	public static final String GET_NUM_KEYWORDS_ENGINE_INCLUDES = "SELECT DISTINCT ?engine (COUNT(?retKeyword) AS ?NumKeywords) WHERE {{?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?retKeyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?engine <http://semoss.org/ontologies/Relation/Has> ?retKeyword}} GROUP BY ?engine ORDER BY DESC(?NumKeywords) BINDINGS ?retKeyword {@BINDINGS@}";

	public static final String GET_INSIGHTS_FOR_KEYWORDS = "SELECT DISTINCT ?Engine ?InsightLabel ?Keyword ?PerspectiveLabel ?Viz WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?Insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?Perspective} {?Perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?Insight} {?Insight <INSIGHT:PARAM> ?Param}{?Param <PARAM:TYPE> ?Type} {?Keyword <http://semoss.org/ontologies/Relation/Has> ?Type}{?Perspective <http://semoss.org/ontologies/Relation/Contains/Label> ?PerspectiveLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Label> ?InsightLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?Viz}} BINDINGS ?Keyword {@KEYWORDS@}";
	public static final String GET_ALL_INSIGHTS = "SELECT DISTINCT ?Engine ?InsightLabel ?Keyword ?PerspectiveLabel ?Viz WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?Insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?Perspective} {?Perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?Insight} {?Insight <INSIGHT:PARAM> ?Param}{?Param <PARAM:TYPE> ?Type} {?Keyword <http://semoss.org/ontologies/Relation/Has> ?Type}{?Perspective <http://semoss.org/ontologies/Relation/Contains/Label> ?PerspectiveLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Label> ?InsightLabel}{?Insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?Viz}} ";

	public static final String GET_ENGINE_CONCEPTS_AND_SAMPLE_INSTANCE = "SELECT DISTINCT ?concept (SAMPLE(?instance) AS ?example) WHERE { {?concept <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://semoss.org/ontologies/Concept>} {?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?concept} } GROUP BY ?concept";
	
	public static final String INSTANCE_EXISTS_QUERY = "SELECT DISTINCT ?keyword ?s WHERE { {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?keyword ;} } BINDINGS ?s {@BINDINGS@}";
	public static final String ENGINE_API_QUERY = "SELECT DISTINCT ?Engine ?API WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Engine <http://semoss.org/ontologies/Relation/Contains/API> ?API}}";
	public static final String ENGINE_LIST_QUERY = "SELECT DISTINCT ?Engine WHERE { {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} }";
	
	public static final String GET_ALL_KEYWORDS = "SELECT DISTINCT ?keyword WHERE { {?keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} }";
	public static final String GET_ALL_MASTER_CONCEPTS = "SELECT DISTINCT ?mc WHERE { {?mc <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} }";
	
	public static final String GET_ALL_KEYWORDS_FROM_MC_List = "SELECT DISTINCT ?Keyword WHERE { { {?childMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC} {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC} } UNION { {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?MC} } } BINDINGS ?MC {@BINDINGS@}";
	
	public static final String GET_ALL_RELATIONSHIPS_FOR_ENGINE = "SELECT DISTINCT ?relationship ?inKeywordURI ?outKeywordURI WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> as ?engine){?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>}{?engineRelation <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/EngineRelation>}{?engine <http://semoss.org/ontologies/Relation/Has> ?engineRelation}{?engineRelation <http://semoss.org/ontologies/Relation/Contains/Name> ?relationship}{?inKeywordURI <http://semoss.org/ontologies/Relation/Provides> ?engineRelation}{?engineRelation <http://semoss.org/ontologies/Relation/Consumes> ?outKeywordURI}}";
	
	//queries for relations to delete
	public static final String KEYWORDS_QUERY = "SELECT DISTINCT ?Keyword WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Engine ?p ?Keyword}}";
	public static final String API_QUERY = "SELECT DISTINCT ?API WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Engine <http://semoss.org/ontologies/Relation/Contains/API> ?API}}";

	public static final String PERSPECTIVES_QUERY = "SELECT DISTINCT ?Perspective WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>}{?Engine ?p ?Perspective}}";
	public static final String INSIGHTS_QUERY = "SELECT DISTINCT ?Insight WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> AS ?Engine) {?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>}{?Engine ?p ?Insight}}";

	//queries to clean up the keywords after engines deleted
	public static final String KEYWORDS_WITHOUT_ENGINES_QUERY = "SELECT DISTINCT ?Keyword WHERE {{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} OPTIONAL{{?Engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?Engine <http://semoss.org/ontologies/Relation/Has> ?Keyword}}FILTER(!BOUND(?Engine))}";
	public static final String MC_KEYWORDS_QUERY = "SELECT DISTINCT ?MasterConcept ?Keyword WHERE {{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?MasterConcept <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?MasterConcept}} BINDINGS ?Keyword {@BINDINGS@}";
	public static final String KEYWORDS_TYPE_QUERY = "SELECT DISTINCT ?Keyword ?Type WHERE {{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?Keyword <http://semoss.org/ontologies/Relation/Has> ?Type}} BINDINGS ?Keyword {@BINDINGS@}";

	//queries to clean up MCs on a deep clean
	public static final String MCS_WITHOUT_KEYWORDS_QUERY = "SELECT DISTINCT ?MC WHERE { {?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}MINUS{SELECT DISTINCT ?MC WHERE{{?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {{?childMC  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>} {?MC <http://semoss.org/ontologies/Relation/ParentOf>+ ?childMC}{?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?childMC}}UNION{{?Keyword <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Keyword>}{?Keyword <http://semoss.org/ontologies/Relation/ComposedOf> ?MC}}}}}";
	public static final String PARENT_CHILD_MC_QUERY = "SELECT DISTINCT ?ParentMC ?ChildMC WHERE {{?ParentMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}{?ChildMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?ParentMC <http://semoss.org/ontologies/Relation/ParentOf> ?ChildMC}} BINDINGS ?ChildMC {@BINDINGS@}";
	public static final String MC_TOP_HYPERNYM_MC_QUERY = "SELECT DISTINCT ?MC ?TopHypernymMC WHERE {{?MC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>}{?TopHypernymMC <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/MasterConcept>} {?MC <http://semoss.org/ontologies/Relation/HasTopHypernym> ?TopHypernymMC}} BINDINGS ?MC {@BINDINGS@}";

	//Auth Queries
	public static final String GET_USER_QUERY = "SELECT DISTINCT ?user WHERE { ?user <http://www.w3.org/2000/01/rdf-schema#label> '@USER_ID@' }";
	public static final String GET_USER_ENGINES_QUERY = "SELECT DISTINCT ?engine WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + ENGINE_BASE_URI + ">} {?engine <" + ENGINE_ROLEGROUP_REL_URI + "> ?engineRoleGroup} {?engineRoleGroup <" + ROLEGROUP_USERGROUP_REL_URI + "> <" + USERGROUP_URI + "/@USER_ID@-OwnerGroup> } {<" + USERGROUP_URI + "/@USER_ID@-OwnerGroup> <" + USERGROUP_USER_REL_URI + "> <" + USER_BASE_URI + "/@USER_ID@> } }";
	public static final String GET_ACCESSIBLE_ENGINES_QUERY = "SELECT DISTINCT ?engine WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + ENGINE_BASE_URI + ">} {?engineRoleGroup <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + ENGINEROLEGROUP_URI + ">} {?userGroup <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + USERGROUP_URI + ">} {?engine <" + ENGINE_ROLEGROUP_REL_URI + "> ?engineRoleGroup} {?engineRoleGroup <" + ROLEGROUP_USERGROUP_REL_URI + "> ?userGroup} {?userGroup <" + USERGROUP_USER_REL_URI + "> <" + USER_BASE_URI + "/@USER_ID@> } }";
	public static final String GET_PERMISSIONS_FOR_ENGINE_QUERY = "SELECT (COALESCE(?explorePermission,'false') AS ?explore) (COALESCE(?traversePermission,'false') AS ?traverse) (COALESCE(?createInsightPermission,'false') AS ?createInsight) (COALESCE(?copyInsightPermission,'false') AS ?copyInsight) (COALESCE(?editInsightPermission,'false') AS ?editInsight) (COALESCE(?deleteInsightPermission,'false') AS ?deleteInsight) (COALESCE(?modifyDataPermission,'false') AS ?modifyData)  WHERE { BIND(<" + ENGINE_BASE_URI + "/@ENGINE_NAME@> as ?engine) {?engineRoleGroup <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + ENGINEROLEGROUP_URI + ">} {?userGroup <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + USERGROUP_URI + ">} {?engine <" + ENGINE_ROLEGROUP_REL_URI + "> ?engineRoleGroup} {?engineRoleGroup <" + ROLEGROUP_USERGROUP_REL_URI + "> ?userGroup} {?userGroup <" + USERGROUP_USER_REL_URI + "> <" + USER_BASE_URI + "/@USER_ID@> } {?engineRoleGroup <" + ENGINEROLEGROUP_ROLE_REL_URI + "> ?role} OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.EXPLORE_NODES.getPropertyName() + "> ?explorePermission} } OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.TRAVERSE.getPropertyName() + "> ?traversePermission} } OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.CREATE_INSIGHT.getPropertyName() + "> ?createInsightPermission} } OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.COPY_INSIGHT.getPropertyName() + "> ?copyInsightPermission} } OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.EDIT_INSIGHT.getPropertyName() + "> ?editInsightPermission} } OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.DELETE_INSIGHT.getPropertyName() + "> ?deleteInsightPermission} } OPTIONAL { {?role <" + PROP_URI + "/" + EnginePermission.MODIFY_DATA.getPropertyName() + "> ?modifyDataPermission} } }";
	public static final String GET_ENGINES_BY_PERMISSIONS_QUERY = "SELECT ?engine WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + ENGINE_BASE_URI + ">} {?engineRoleGroup <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + ENGINEROLEGROUP_URI + ">} {?userGroup <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + USERGROUP_URI + ">} {?engine <" + ENGINE_ROLEGROUP_REL_URI + "> ?engineRoleGroup} {?engineRoleGroup <" + ROLEGROUP_USERGROUP_REL_URI + "> ?userGroup} {?userGroup <" + USERGROUP_USER_REL_URI + "> <" + USER_BASE_URI + "/@USER_ID@> } {?engineRoleGroup <" + ENGINEROLEGROUP_ROLE_REL_URI + "> ?role} @PERMISSIONS@ }";
	public static final String GET_ENGINE_OWNER_QUERY = "SELECT DISTINCT ?user WHERE { BIND(<" + ENGINE_BASE_URI + "/@ENGINE_NAME@> as ?engine) {?engine <" + ENGINE_ROLEGROUP_REL_URI + "> <" + ENGINEROLEGROUP_URI + "/@ENGINE_NAME@-Owner>} {<" + ENGINEROLEGROUP_URI + "/@ENGINE_NAME@-Owner> <" + ROLEGROUP_USERGROUP_REL_URI + "> ?userGroup } {?userGroup <" + USERGROUP_USER_REL_URI + "> ?user } }";
	public static final String ENGINE_ACCESSREQUESTS_FOR_USER = "SELECT DISTINCT ?requestorName ?engine ?engineAccessRequest WHERE {BIND(<" + MasterDatabaseURIs.USER_BASE_URI + "/@USER_ID@> as ?notifiedUser) {?notifiedUser <" + MasterDatabaseURIs.USER_ENGINE_ACCESSREQUEST_REL_URI + "> ?engineAccessRequest} {?engineAccessRequest <" + MasterDatabaseURIs.ENGINE_ACCESS_REQUESTOR_PROP_URI + "> ?requestorUserId} BIND(URI(CONCAT(<" + MasterDatabaseURIs.USER_BASE_URI + "/,?requestorUserId,>)) as ?requestorUser) {?requestorUser <" + MasterDatabaseURIs.USER_NAME_PROP_URI + "> ?requestorName} {?engineAccessRequest <" + MasterDatabaseURIs.ENGINE_NAME_REQUESTED_PROP_URI + "> ?engine} }";
	public static final String GET_ENGINE_ACCESSREQUEST_USER = "SELECT ?requestorUserId ?engine WHERE {BIND(<" + MasterDatabaseURIs.USER_BASE_URI + "/@USER_ID@> as ?notifiedUser) BIND(<" + MasterDatabaseURIs.ENGINE_ACCESSREQUEST_URI + "/@REQUEST_ID@> as ?engineAccessRequest) {?notifiedUser <" + MasterDatabaseURIs.USER_ENGINE_ACCESSREQUEST_REL_URI + "> ?engineAccessRequest} {?engineAccessRequest <" + MasterDatabaseURIs.ENGINE_ACCESS_REQUESTOR_PROP_URI + "> ?requestorUserId} {?engineAccessRequest <" + MasterDatabaseURIs.ENGINE_NAME_REQUESTED_PROP_URI + "> ?engine} }";

	//Search for objects of a relationship with bindings on subject and engine
	public static final String GET_OBJECTS_OF_RELATIONSHIP = "SELECT DISTINCT ?relName ?object WHERE { BIND(<@CONCEPT@> AS ?concept) {?concept <http://semoss.org/ontologies/Relation/Provides> ?engineRelation} {?engineRelation <http://semoss.org/ontologies/Relation/Consumes> ?object} {<@ENGINE@> <http://semoss.org/ontologies/Relation/Has> ?engineRelation} {?engineRelation <http://semoss.org/ontologies/Relation/Contains/Name> ?relName} }";

	//Search for subjects of a relationship with bindings on object and engine
	public static final String GET_SUBJECTS_OF_RELATIONSHIP = "SELECT DISTINCT ?relName ?object WHERE { BIND(<@CONCEPT@> AS ?concept) {?engineRelation <http://semoss.org/ontologies/Relation/Consumes> ?concept} {?object <http://semoss.org/ontologies/Relation/Provides> ?engineRelation} {<@ENGINE@> <http://semoss.org/ontologies/Relation/Has>  ?engineRelation} {?engineRelation <http://semoss.org/ontologies/Relation/Contains/Name> ?relName} }";
	
	//User activity tracking queries
	public static final String GET_USER_INSIGHT = "SELECT ?userinsight WHERE { BIND(<" + MasterDatabaseURIs.USERINSIGHT_URI + "/@USERINSIGHT@> as ?userinsight) {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + MasterDatabaseURIs.USERINSIGHT_URI + ">} }";
	public static final String GET_USER_INSIGHT_EXECUTED_COUNT = "SELECT ?count WHERE { BIND(<" + MasterDatabaseURIs.USERINSIGHT_URI + "/@USERINSIGHT@> as ?userinsight) {?userinsight <" + MasterDatabaseURIs.USERINSIGHT_EXECUTION_COUNT_PROP_URI + "> ?count} }";
	public static final String GET_USER_INSIGHTS_FOR_ENGINE = "SELECT DISTINCT ?insight ?label ?perspective ?layout (SUM(?count) as ?total) WHERE { BIND(<http://semoss.org/ontologies/Concept/Engine/@ENGINE@> as ?engine) {?perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?perspective} {?perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?insight} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?insight <http://semoss.org/ontologies/Relation/Contains/Label> ?label} {?insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?layout} {?userinsight <http://semoss.org/ontologies/Relation/Contains/ExecutionCount> ?count} } GROUP BY ?insight ?label ?perspective ?layout ORDER BY DESC(?total)";
	public static final String GET_ALL_USER_INSIGHTS = "SELECT DISTINCT ?insight ?label ?perspective ?layout (SUM(?count) as ?total) WHERE { {?engine <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Engine>} {?perspective <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Perspective>} {?insight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Insight>} {?userinsight <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInsight>} {?engine <http://semoss.org/ontologies/Relation/Engine:Perspective> ?perspective} {?perspective <http://semoss.org/ontologies/Relation/Perspective:Insight> ?insight} {?insight <http://semoss.org/ontologies/Relation/ExecutedBy> ?userinsight} {?insight <http://semoss.org/ontologies/Relation/Contains/Label> ?label} {?insight <http://semoss.org/ontologies/Relation/Contains/Layout> ?layout} {?userinsight <http://semoss.org/ontologies/Relation/Contains/ExecutionCount> ?count} } GROUP BY ?insight ?label ?perspective ?layout ORDER BY DESC(?total)";
}
