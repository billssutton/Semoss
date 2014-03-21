package prerna.ui.components.specific.tap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.BigDataEngine;
import prerna.rdf.engine.impl.SesameJenaSelectStatement;
import prerna.rdf.engine.impl.SesameJenaSelectWrapper;
import prerna.ui.components.UpdateProcessor;
import prerna.util.Utility;

public class ServicesAggregationProcessor {

	Logger logger = Logger.getLogger(getClass());
	private IEngine servicesDB;
	private IEngine coreDB;
	private String semossBaseURI = "http://semoss.org/ontologies/Concept/";
	private String semossRelBaseURI = "http://semoss.org/ontologies/Relation/";
	private String propURI = "http://semoss.org/ontologies/Relation/Contains/";

	private Hashtable<String, Hashtable<String, Object>> dataHash = new Hashtable<String, Hashtable<String, Object>>();
	private Hashtable<String, Hashtable<String, Object>> removeDataHash = new Hashtable<String, Hashtable<String, Object>>();

	private Hashtable<String, Set<String>> allRelations = new Hashtable<String, Set<String>>();
	private Hashtable<String, Set<String>> allConcepts = new Hashtable<String, Set<String>>();

	private HashSet<String> allSoftwareModules = new HashSet<String>();
	private HashSet<String> allHardwareModules = new HashSet<String>();

	public String errorMessage = "";

	public String getErrorMessage()
	{
		return this.errorMessage;
	}

	private String TAP_SERVICES_AGGREGATE_SYSTEM_USERS_QUERY = "SELECT DISTINCT ?system ?usedBy ?user WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?user <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemUser>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?usedBy <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/UsedBy>} {?systemService ?usedBy ?user}}";
	
	private String TAP_SYSTEM_SERVICES_PROPERTY_AGGREGATION_QUERY = "SELECT DISTINCT ?system ?prop ?value ?user WHERE{{?system a <http://semoss.org/ontologies/Concept/System>} {?systemService a <http://semoss.org/ontologies/Concept/SystemService>} {?user a <http://semoss.org/ontologies/Concept/SystemUser>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?prop a <http://semoss.org/ontologies/Relation/Contains>} {?systemService ?prop ?value} {?systemService <http://semoss.org/ontologies/Relation/UsedBy> ?user}}";

	private String TAP_CORE_SYSTEM_PROPERTY_AGGREGATION_QUERY = "SELECT DISTINCT ?system ?prop ?value WHERE{{?system a <http://semoss.org/ontologies/Concept/System>} {?prop a <http://semoss.org/ontologies/Relation/Contains>} {?system ?prop ?value} }";

	private String TAP_SERVICES_AGGREGATE_PERSONNEL_QUERY = "SELECT DISTINCT ?system ?usedBy ?personnel WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>}{?usedBy <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/UsedBy>} {?personnel <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Personnel>}  {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?usedBy ?personnel}}";

	private String TAP_SERVICES_AGGREGATE_USER_INTERFACE_QUERY = "SELECT DISTINCT ?system ?utilizes ?userInterface WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?utilizes <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Utilizes>} {?userInterface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/UserInterface>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?utilizes ?userInterface}}";

	private String TAP_SERVICES_AGGREGATE_BP_QUERY = "SELECT DISTINCT ?system ?supports ?bp WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?supports <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Supports>} {?bp <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/BusinessProcess>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?supports ?bp}}";

	private String TAP_SERVICES_AGGREGATE_ACTIVITY_QUERY = "SELECT DISTINCT ?system ?supports ?activity WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?supports <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Supports>} {?activity <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Activity>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?supports ?activity}}";

	private String TAP_SERVICES_AGGREGATE_BLU_QUERY = "SELECT DISTINCT ?system ?provide ?BLU WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?provide <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>} {?BLU <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/BusinessLogicUnit>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?provide ?BLU}}";

	private String TAP_SERVICES_AGGREGATE_LIFECYCLE_QUERY = "SELECT DISTINCT ?system ?phase ?lifeCycle WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?phase <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Phase>} {?lifeCycle <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/LifeCycle>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?phase ?lifeCycle}}";

	private String TAP_SERVICES_SYSTEM_PROVIDE_ICD_QUERY = "SELECT DISTINCT ?sys ?pred ?icd WHERE{{?sys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?pred <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>} {?sys ?pred ?icd}}";
	
	private String TAP_SERVICES_ICD_CONSUME_SYS_QUERY = "SELECT DISTINCT ?icd ?pred ?sys WHERE{{?sys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?pred <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consume>} {?icd ?pred ?sys}}";
	
	private String TAP_CORE_AGGREGATE_ICD_PROP_QUERY = "SELECT DISTINCT ?Payload ?prop ?value WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?Payload <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?prop <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Relation/Contains>} {?ICD ?Payload ?Data} {?Payload ?prop ?value}}";
	
	private String TAP_SERVICES_AGGREGATE_ICD_PROP_QUERY = "SELECT DISTINCT ?Payload ?prop ?value ?user WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?Payload <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?user a <http://semoss.org/ontologies/Concept/SystemUser>} {?prop <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Relation/Contains>} {?Payload ?prop ?value} {?ICD ?Payload ?Data} {?systemService <http://semoss.org/ontologies/Relation/UsedBy> ?user} {?systemService <http://semoss.org/ontologies/Relation/Implemented_At> ?ICD}}";
	
	private String TAP_SERVICES_AGGREGATE_ICD_DATAOBJECT_QUERY = "SELECT DISTINCT ?ICD ?Payload ?data WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?Payload <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>} {?ICD ?Payload ?data}}";
	
	private String TAP_SERVICES_AGGREGATE_ICD_DFORM_QUERY = "SELECT DISTINCT ?ICD ?has ?dForm WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?dForm <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DForm>} {?has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?ICD ?has ?dForm}}";
	
	private String TAP_SERVICER_AGGREGATE_ICD_DFREQ_QUERY = "SELECT DISTINCT ?ICD ?has ?dFreq WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?dFreq <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DFreq>} {?has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?ICD ?has ?dFreq}}";
	
	private String TAP_SERVICER_AGGREGATE_ICD_DPROT_QUERY = "SELECT DISTINCT ?ICD ?has ?dProt WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?dProt <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DProt>} {?has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?ICD ?has ?dProt}}";
	
	private String TAP_SERVICER_AGGREGATE_ICD_LIFECYCLE_QUERY = "SELECT DISTINCT ?ICD ?phase ?lifeCycle WHERE{{?ICD <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?lifeCycle <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/LifeCycle>} {?phase <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Phase>} {?ICD ?phase ?lifeCycle} }";
	
	private String TAP_SERVICES_AGGREGATE_TERROR_QUERY = "SELECT DISTINCT ?system ?has ?TError ?weight WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?TError <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/TError>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?has ?TError} {?has <http://semoss.org/ontologies/Relation/Contains/weight> ?weight}}";
	
	private String TAP_CORE_AGGREGATE_TERROR_QUERY = "SELECT DISTINCT ?system ?has ?TError ?weight WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?TError <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/TError>} {?has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?system ?has ?TError} {?has <http://semoss.org/ontologies/Relation/Contains/weight> ?weight}}";

	private String TAP_SERVICES_AGGREGATE_DATAOBJECT_QUERY = "SELECT DISTINCT ?system ?provide ?data ?crm WHERE{{?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?provide <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>} {?data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?systemService ?provide ?data} {?provide <http://semoss.org/ontologies/Relation/Contains/CRM> ?crm}}";

	private String TAP_CORE_AGGREGATE_DATAOBJECT_QUERY = "SELECT DISTINCT ?system ?provide ?data ?crm WHERE{ {?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?provide <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>} {?data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?system ?provide ?data} {?provide <http://semoss.org/ontologies/Relation/Contains/CRM> ?crm}}";

	private String TAP_CORE_SOFTWARE_MODULE_LIST_QUERY = "SELECT DISTINCT ?softwareModule WHERE{{?softwareModule a <http://semoss.org/ontologies/Concept/SoftwareModule>} }";
	
	private String TAP_CORE_HARDWARE_MODULE_LIST_QUERY = "SELECT DISTINCT ?hardwareModule WHERE{{?hardwareModule a <http://semoss.org/ontologies/Concept/HardwareModule>} }";

	private String TAP_SERVICES_AGGREGATION_SOFTWARE_QUERY = "SELECT DISTINCT ?softwareModule ?prop ?value ?system ?softwareVersion ?software ?user WHERE{{?softwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SoftwareModule>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?prop <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Relation/Contains>} {?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?softwareVersion <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SoftwareVersion>} {?user <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemUser>} {?softwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SoftwareModule>} {?software <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Software>} {?serviceSoftwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ServiceSoftwareModule>} {?serviceSoftwareModule <http://semoss.org/ontologies/Relation/TypeOf> ?softwareModule} {?systemService <http://semoss.org/ontologies/Relation/Consists> ?serviceSoftwareModule} {?serviceSoftwareModule ?prop ?value} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?serviceSoftwareModule <http://semoss.org/ontologies/Relation/TypeOf> ?softwareModule} {?softwareModule <http://semoss.org/ontologies/Relation/TypeOf> ?softwareVersion} {?software <http://semoss.org/ontologies/Relation/Has> ?softwareVersion} {?systemService <http://semoss.org/ontologies/Relation/UsedBy> ?user}}";
	
	private String TAP_CORE_AGGREGATION_SOFTWARE_QUERY = "SELECT DISTINCT ?softwareModule ?prop ?value WHERE{{?softwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SoftwareModule>} {?prop <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Relation/Contains>} {?softwareModule ?prop ?value}}";
	
	private String TAP_SERVICES_AGGREGATE_HARDWARE_QUERY = "SELECT DISTINCT ?hardwareModule ?prop ?value ?system ?hardwareVersion ?hardware ?user WHERE{{?hardwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/HardwareModule>} {?serviceHardwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ServiceHardwareModule>} {?systemService <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemService>} {?hardwareVersion <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/HardwareVersion>} {?user <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemUser>} {?hardware <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Hardware>} {?prop <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Relation/Contains>} {?hardwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/HardwareModule>} {?serviceHardwareModule <http://semoss.org/ontologies/Relation/TypeOf> ?hardwareModule} {?systemService <http://semoss.org/ontologies/Relation/Has> ?serviceHardwareModule} {?serviceHardwareModule ?prop ?value} {?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?system <http://semoss.org/ontologies/Relation/ConsistsOf> ?systemService} {?serviceHardwareModule <http://semoss.org/ontologies/Relation/TypeOf> ?hardwareModule} {?hardwareModule <http://semoss.org/ontologies/Relation/TypeOf> ?hardwareVersion} {?hardware <http://semoss.org/ontologies/Relation/Has> ?hardwareVersion} {?systemService ?usedBy ?user}}";
	
	private String TAP_CORE_AGGREGATION_HARDWARE_QUERY = "SELECT DISTINCT ?hardwareModule ?prop ?value WHERE{{?hardwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/HardwareModule>} {?prop <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Relation/Contains>} {?hardwareModule ?prop ?value}}";

	private String TAP_CORE_RELATIONS_LIST_QUERY = "SELECT ?relations WHERE{{?relations <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation>} filter(regex(str(?relations),\"^http://semoss\"))}";
	
	private String TAP_CORE_CONCEPTS_LIST_QUERY = "SELECT ?concepts WHERE{{?concepts <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://semoss.org/ontologies/Concept>}}";

	public ServicesAggregationProcessor(IEngine servicesDB, IEngine coreDB){
		this.servicesDB = servicesDB;
		this.coreDB = coreDB;
	}

	public boolean runFullAggregation()
	{
		boolean success = true;
		runGetListOfModules(TAP_CORE_SOFTWARE_MODULE_LIST_QUERY, true);
		runGetListOfModules(TAP_CORE_HARDWARE_MODULE_LIST_QUERY, false);
		
		logger.info("PROCESSING SYSTEM USERS FOR SERVICE SYSTEMS AND AGGREGATING TO SYSTEMS IN TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_SYSTEM_USERS_QUERY);
		
		logger.info("PROCESSING PERSONNEL FOR SERVICE SYSTEMS AND AGGREGATING TO SYSTEMS IN TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_PERSONNEL_QUERY);
		
		logger.info("PROCESSING USER INTERFACE FOR SERVICE SYSTEMS AND AGGREGATING TO SYSTEMS IN TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_USER_INTERFACE_QUERY);
		
		logger.info("PROCESSING BP FOR SERVICE SYSTEMS AND AGGREGATING TO SYSTEMS IN TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_BP_QUERY);
		
		logger.info("PROCESSING ACTIVITY FOR SERVICE SYSTEMS AND AGGREGATING TO SYSTEMS IN TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_ACTIVITY_QUERY);
		
		logger.info("PROCESSING BLU FOR SERVICE SYSTEMS AND AGGREGATING TO SYSTEMS IN TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_BLU_QUERY);
		
		logger.info("PROCESSING SYSTEM PROVIDE ICDS AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_SYSTEM_PROVIDE_ICD_QUERY);
		
		logger.info("PROCESSING ICD CONSUME SYSTEM AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_ICD_CONSUME_SYS_QUERY);
		
		logger.info("PROCESSING ICD PAYLOAD DATA_OBJECT AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_ICD_DATAOBJECT_QUERY);
		logger.info("PROCESSING ICD HAS DFORM AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICES_AGGREGATE_ICD_DFORM_QUERY);
		logger.info("PROCESSING ICD HAS DFREQ AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICER_AGGREGATE_ICD_DFREQ_QUERY);
		logger.info("PROCESSING ICD HAS DPROT_OBJECT AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICER_AGGREGATE_ICD_DPROT_QUERY);
		logger.info("PROCESSING ICD PHASE LIFECYCLE AND PUSHING INTO TAP CORE");
		runRelationshipAggregation(TAP_SERVICER_AGGREGATE_ICD_LIFECYCLE_QUERY);

		logger.info("PROCESSING SYSTEM SERVICE LIFE AND AGGREGATING INTO TAP CORE");
		runSystemServiceLifeCylceAggregation(TAP_SERVICES_AGGREGATE_LIFECYCLE_QUERY);

		logger.info("PROCESSING SYSTEM SERVICE PROPERTIES AND AGGREGATING TO SYSTEMS IN TAP CORE");
		logger.info("QUERIES BOTH TAP SERVICES AND TAP CORE SO NO PROPERTIES ARE LOST");
		runSystemServicePropertyAggregation(TAP_SYSTEM_SERVICES_PROPERTY_AGGREGATION_QUERY, TAP_CORE_SYSTEM_PROPERTY_AGGREGATION_QUERY);
		if(!errorMessage.isEmpty())
		{
			return (success = false);
		}
		logger.info("PROCESSING ICD PAYLOAD DATA RELATIONSHIP PROPERTIES AND AGGREGATING TO PAYLOAD RELATIONSHIP IN TAP CORE");
		logger.info("QUERIES BOTH TAP SERVICES AND TAP CORE SO NO PROPERTIES ARE LOST");
		runICDPropAggregation(TAP_SERVICES_AGGREGATE_ICD_PROP_QUERY, TAP_CORE_AGGREGATE_ICD_PROP_QUERY);
		if(!errorMessage.isEmpty())
		{
			return (success = false);
		}
		logger.info("PROCESSING SYSTEM TERROR RELATIONSHIP AND WEIGHT PROPERTY AND AGGREGATING INTO TAP CORE");
		logger.info("QUERIES BOTH TAP SERVICES AND TAP CORE TO DETERMINE NEW WEIGHT OF TERROR PROPERTY VALUE");
		runTErrorAggregation(TAP_SERVICES_AGGREGATE_TERROR_QUERY, TAP_CORE_AGGREGATE_TERROR_QUERY);
		if(!errorMessage.isEmpty())
		{
			return (success = false);
		}
		logger.info("PROCESSING SYSTEM DATAOBJECT RELATIONSHIP AND CRM PROPERTY AND AGGREGATING INTO TAP CORE");
		logger.info("QUERIES BOTH TAP SERVICES AND TAP CORE TO DETERMINE CRM");
		runDataObjectAggregation(TAP_SERVICES_AGGREGATE_DATAOBJECT_QUERY, TAP_CORE_AGGREGATE_DATAOBJECT_QUERY);
		if(!errorMessage.isEmpty())
		{
			return (success = false);
		}
		logger.info("PROCESSING SERVICE SYSTEM MODULE AND DOING LOTS OF STUFF");
		runHardwareSoftwareAggregation(TAP_SERVICES_AGGREGATION_SOFTWARE_QUERY, TAP_CORE_AGGREGATION_SOFTWARE_QUERY, true);
		if(!errorMessage.isEmpty())
		{
			return (success = false);
		}
		logger.info("PROCESSING SERVICE HARDWARE MODULE AND DOING LOTS OF STUFF");
		runHardwareSoftwareAggregation(TAP_SERVICES_AGGREGATE_HARDWARE_QUERY, TAP_CORE_AGGREGATION_HARDWARE_QUERY, false);
		if(!errorMessage.isEmpty())
		{
			return (success = false);
		}
		processNewConcepts();
		processNewRelationships();
		((BigDataEngine) coreDB).infer();

		return success;
	}

	private void runRelationshipAggregation(String query)
	{
		dataHash.clear();
		SesameJenaSelectWrapper sjsw = processQuery(query, servicesDB);
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			// get the next row and see how it must be added to the insert query
			String subject = sjss.getRawVar(vars[0]).toString();
			String pred = sjss.getRawVar(vars[1]).toString();
			String object = sjss.getRawVar(vars[2]).toString();
			pred = pred.substring(0, pred.lastIndexOf("/")) + "/" + getTextAfterFinalDelimeter(subject, "/") +":" + getTextAfterFinalDelimeter(object, "/");
			logger.debug("ADDING RELATIONSHIP:     " + subject + " -----> {" + pred + " --- " + object + "}");
			addToHash(new String[]{subject, pred, object});
			// add instances to master list
			addToAllConcepts(subject);
			addToAllConcepts(object);
			addToAllRelationships(pred);
		}
		processData(dataHash);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// process system life cycle properties
	private void runSystemServiceLifeCylceAggregation(String query) 
	{
		dataHash.clear();
		SesameJenaSelectWrapper sjsw = processQuery(query, servicesDB);
		Hashtable<String, LinkedList<String>> lifeCycleHash = new Hashtable<String, LinkedList<String>>();
		lifeCycleHash = aggregateLifeCycle(sjsw, lifeCycleHash);
		String lifeCycle = "";
		for( String sys : lifeCycleHash.keySet())
		{
			LinkedList<String> lifeCycleList = lifeCycleHash.get(sys);
			String pred = lifeCycleList.get(0);
			lifeCycleList.remove(0);
			if(lifeCycleList.toString().contains("LifeCycle/Supported"))
			{
				lifeCycle = getBaseURI(sys) + "/Concept/LifeCycle/Supported";
			}
			else
			{
				lifeCycle = getBaseURI(sys) + "/Concept/LifeCycle/Retired (Not Supported)";
			}
			addToAllConcepts(sys);
			addToAllConcepts(lifeCycle);
			addToAllRelationships(pred);
			logger.debug("ADDING SYSTEM LIFECYCLE:     " + sys + " -----> {" + pred + " --- " + lifeCycle + "}");
			addToHash(new String[]{sys, pred, lifeCycle});
		}
		processData(dataHash);
	}

	private Hashtable<String, LinkedList<String>> aggregateLifeCycle(SesameJenaSelectWrapper sjsw, Hashtable<String, LinkedList<String>> lifeCycleHash) 
	{
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			// get the next row and see how it must be added to the insert query
			String sys = sjss.getRawVar(vars[0]).toString();
			String pred = sjss.getRawVar(vars[1]).toString();
			String lifeCycle = sjss.getRawVar(vars[2]).toString();
			pred = pred.substring(0, pred.lastIndexOf("/")) + "/" + getTextAfterFinalDelimeter(sys, "/") +":" + getTextAfterFinalDelimeter(lifeCycle, "/");

			if(!lifeCycle.equals("\"NA\"") && !lifeCycle.equals("\"TBD\""))
			{
				if(!lifeCycleHash.containsKey(sys))
				{
					LinkedList<String> lifeCycleList = new LinkedList<String>();
					lifeCycleList.add(pred);
					lifeCycleList.add(lifeCycle);
					lifeCycleHash.put(sys, lifeCycleList);
					logger.debug("ADDING NEW LIFECYCLE LIST:     " + sys + " -----> {" + pred + " --- " + lifeCycleList.toString() + "}");
				}
				else
				{
					LinkedList<String> lifeCycleList = lifeCycleHash.get(sys);
					lifeCycleList.add(lifeCycle);
					logger.debug("ADJUSTING LIFECYCLE LIST:     " + sys + " -----> {" + pred + " --- " + lifeCycleList.toString() + "}");
				}
			}
		}
		return lifeCycleHash;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// process system service properties

	private void runSystemServicePropertyAggregation(String propSystemServiceQuery, String propTAPCoreQuery)
	{
		dataHash.clear();
		SesameJenaSelectWrapper sjswServices = processQuery(propSystemServiceQuery, servicesDB);
		processServiceSystemProperties(sjswServices,  false);

		SesameJenaSelectWrapper sjswCore = processQuery(propTAPCoreQuery, coreDB);
		processServiceSystemProperties(sjswCore, true);

		// processing modifies class variable dataHash directly
		deleteData(removeDataHash);
		processData(dataHash);
	}

	private void processServiceSystemProperties(SesameJenaSelectWrapper sjsw, boolean TAP_Core)
	{	
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			String sub = sjss.getRawVar(vars[0]).toString();
			String prop = sjss.getRawVar(vars[1]).toString();
			Object value = sjss.getRawVar(vars[2]);
			String user = "";
			if(!TAP_Core)
			{
				user = sjss.getRawVar(vars[3]).toString();
			}

			if(dataHash.containsKey(sub) || !TAP_Core)
			{
				Object[] returnTriple = new Object[3];
				if(!value.toString().equals("\"NA\"") && !value.toString().equals("\"TBD\"") && value != null && value instanceof Literal )
				{
					if(prop.equals(propURI + "ATO_Date"))
					{
						boolean earliest = false;
						returnTriple = processMinMaxDate(sub, prop, value, earliest);
					}
					else if(prop.equals(propURI + "End_of_Support_Date"))
					{
						boolean latest = true;
						returnTriple = processMinMaxDate(sub, prop, value, latest);
					}
					else if(prop.equals(propURI + "Availability-Actual"))
					{
						boolean min = false;
						returnTriple = processMaxMinDouble(sub, prop, value, min);
					}
					else if(prop.equals(propURI + "Availability-Required"))
					{
						boolean max = true;
						returnTriple = processMaxMinDouble(sub, prop, value, max);
					}
					else if(prop.equals(propURI + "Description"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "POC"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Full_System_Name"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Number_of_Users"))
					{
						returnTriple = processSumValues(sub, prop, value);
					}
					else if(prop.equals(propURI + "Transaction_Count"))
					{
						returnTriple = processSumValues(sub, prop, value);
					}
					else if(prop.equals(propURI + "User_Consoles"))
					{
						returnTriple = processSumValues(sub, prop, value);
					}
					else if(prop.equals(propURI + "GarrisonTheater"))
					{
						returnTriple = processGarrisonTheater(sub, prop, value);
					}
					else if(prop.equals(propURI + "Transactional"))
					{
						returnTriple = processTransactional(sub, prop, value);
					}

					// if error occurs
					if(Arrays.equals(returnTriple, new String[]{""}))
					{
						return;
					}

					// returnTriple never gets a value when the property being passed in isn't in the defined list above
					if(returnTriple[0] != null)
					{
						logger.debug("ADDING SYSTEM PROPERTY:     " + returnTriple[0] + " -----> {" + returnTriple[1] + " --- " + returnTriple[2].toString() + "}");
						addToHash(returnTriple);
					}

					// sub already exists when going through TAP Core db
					if(!TAP_Core)
					{
						addToAllConcepts(sub);
					}

					// must remove existing triple in TAP Core prior to adding
					if(TAP_Core)
					{
						addToDeleteHash(new String[]{"<" + sub + ">","<" + prop + ">", value.toString()});
					}
				}
			}
		}
	}

	private void runICDPropAggregation(String servicesQuery, String coreQuery)
	{
		dataHash.clear();

		SesameJenaSelectWrapper sjswService = processQuery(servicesQuery, servicesDB);
		processICDPropAggregation(sjswService, false);

		SesameJenaSelectWrapper sjswCore = processQuery(coreQuery, coreDB);
		processICDPropAggregation(sjswCore , true);

		// processing modifies class variable dataHash directly
		deleteData(removeDataHash);
		processData(dataHash);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// process icds

	private void processICDPropAggregation(SesameJenaSelectWrapper sjsw, boolean TAP_Core) 
	{
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			String sub = sjss.getRawVar(vars[0]).toString();
			String prop = sjss.getRawVar(vars[1]).toString();
			Object value = sjss.getRawVar(vars[2]);
			String user = "";
			if(!TAP_Core)
			{
				user = sjss.getRawVar(vars[3]).toString();
			}
			else
			{
				user = "";
			}

			if(dataHash.containsKey(sub) || !TAP_Core)
			{
				Object[] returnTriple = new Object[3];
				if(!value.toString().equals("\"NA\"") && !value.toString().equals("\"TBD\"") && value != null && value instanceof Literal )
				{
					if(prop.equals(propURI + "Data"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Format"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Frequency"))
					{
						returnTriple = processDFreq(sub, prop, value);
					}
					else if(prop.equals(propURI + "Interface_Name"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Protocol"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Source"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}
					else if(prop.equals(propURI + "Type"))
					{
						returnTriple = processConcatString(sub, prop, value, user);
					}

					// if error occurs
					if(Arrays.equals(returnTriple, new String[]{""}))
					{
						return;
					}

					// returnTriple never gets a value when the property being passed in isn't in the defined list above
					if(returnTriple[0] != null)
					{
						logger.debug("ADDING ICD PROPERTY:     " + returnTriple[0] + " -----> {" + returnTriple[1] + " --- " + returnTriple[2].toString() + "}");
						addToHash(returnTriple);
					}
					// sub already exists when going through TAP Core db
					if(!TAP_Core)
					{
						addToAllConcepts(sub);
					}
					// must remove existing triple in TAP Core prior to adding
					if(TAP_Core)
					{
						addToDeleteHash(new String[]{"<" + sub + ">","<" + prop + ">", value.toString()});
					}
				}
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// process terror

	private void runTErrorAggregation(String servicesQuery, String coreQuery) 
	{
		Hashtable<String, Hashtable<String, LinkedList<Object>>> aggregatedTError = new Hashtable<String, Hashtable<String, LinkedList<Object>>>();
		dataHash.clear();

		SesameJenaSelectWrapper sjswService = processQuery(servicesQuery, servicesDB);
		aggregatedTError = runAggregateAllData(sjswService, aggregatedTError, "weight", false);

		SesameJenaSelectWrapper sjswCore = processQuery(coreQuery, coreDB);
		aggregatedTError = runAggregateAllData(sjswCore, aggregatedTError, "weight", true);

		// processing modifies class variable dataHash directly
		boolean success = processTError(aggregatedTError, "weight");
		if(!success)
		{
			return;
		}

		deleteData(removeDataHash);
		processData(dataHash);
	}

	private boolean processTError(Hashtable<String, Hashtable<String, LinkedList<Object>>> aggregatedTError, String propType) 
	{

		boolean success = true;
		String propertyURI = propURI + propType;
		for( String sub : aggregatedTError.keySet() )
		{
			Hashtable<String, LinkedList<Object>> innerHash = aggregatedTError.get(sub);
			for ( String obj : innerHash.keySet() )
			{
				LinkedList<Object> tErrList = innerHash.get(obj);
				Iterator<Object> tErrIt = tErrList.listIterator();
				int counter = 0;
				double totalTErr = 0;
				String pred = "";
				while(tErrIt.hasNext())
				{
					if(counter == 0)
					{
						pred  = tErrIt.next().toString();
						counter++;
					}
					else 
					{
						Literal valueAsObject = (Literal) tErrIt.next();
						try
						{
							valueAsObject.doubleValue();
						}
						catch(NumberFormatException e)
						{
							e.printStackTrace();
							this.errorMessage = this.errorMessage + "Error Processing TError! \n" 
									+ "Error occured processing: " + pred + ">>>>" + propertyURI + ">>>>" + valueAsObject + "\n"				
									+ "Check that value is parsable as a double";	
							return (success = false);
						}
						Double value = valueAsObject.doubleValue();
						totalTErr += value;
						counter++;
					}
				}

				Double TError = totalTErr/(counter-1);
				logger.debug("ADDING SYSTEM TO TERROR RELATIONSHIP:     " + sub + " -----> {" + pred + " --- " + obj + "}");
				addToHash(new Object[]{sub, pred, obj});
				logger.debug("ADDING TERROR WEIGHT RELATIONSHIP PROPERTY:     " + pred + " -----> {" + propertyURI + " --- " +  TError +  "}");
				addToHash(new Object[]{pred, propertyURI, TError});
				addToAllConcepts(obj);
				addToAllRelationships(pred);
			}
		}
		return success;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// process data objects

	private void runDataObjectAggregation(String servicesQuery, String coreQuery)
	{
		Hashtable<String, Hashtable<String, LinkedList<Object>>> aggregatedDataObjects = new Hashtable<String, Hashtable<String, LinkedList<Object>>>();
		dataHash.clear();

		SesameJenaSelectWrapper sjswService = processQuery(servicesQuery, servicesDB);
		aggregatedDataObjects = runAggregateAllData(sjswService , aggregatedDataObjects, "CRM", false);

		SesameJenaSelectWrapper sjswCore = processQuery(coreQuery, coreDB);
		aggregatedDataObjects = runAggregateAllData(sjswCore, aggregatedDataObjects, "CRM", true);

		// processing modifies class variable dataHash directly
		processDataObjects(aggregatedDataObjects, "CRM");

		deleteData(removeDataHash);
		processData(dataHash);
	}

	private void processDataObjects(Hashtable<String, Hashtable<String, LinkedList<Object>>> aggregatedDataObjects, String propType) 
	{
		String propertyURI = propURI + propType;
		for( String sub : aggregatedDataObjects.keySet() )
		{
			Hashtable<String, LinkedList<Object>> innerHash = aggregatedDataObjects.get(sub);
			for ( String obj : innerHash.keySet() )
			{
				LinkedList<Object> crmList = innerHash.get(obj);
				String pred  = crmList.get(0).toString();
				crmList.remove(0);
				String CRM = "";
				if(crmList.toString().contains("C"))
				{
					CRM = "\"C\"";
				}
				else if(crmList.toString().contains("M"))
				{
					CRM = "\"M\"";
				}
				else if(crmList.toString().contains("R"))
				{
					CRM = "\"R\"";
				}

				logger.debug("ADDING SYSTEM TO DATAOBJECT RELATIONSHIP:     " + sub + " -----> {" + pred + " --- " + obj + "}");
				addToHash(new Object[]{sub, pred, obj});
				logger.debug("ADDING DATAOBJECT CRM RELATIONSHIP PROPERTY:     " + pred + " -----> {" + propertyURI + " --- " +  CRM + "}");
				addToHash(new Object[]{pred, propertyURI, CRM});
				addToAllRelationships(pred);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// process software and hardware modules

	private void runGetListOfModules(String query, boolean softwareModule) 
	{
		SesameJenaSelectWrapper sjsw = processQuery(query, coreDB);
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			String sub = sjss.getRawVar(vars[0]).toString();
			if(softwareModule)
			{
				allSoftwareModules.add(sub);
			}
			else
			{
				allHardwareModules.add(sub);
			}
		}
	}

	private void runHardwareSoftwareAggregation(String servicesQuery, String coreQuery, boolean softwareModule) 
	{
		dataHash.clear();

		SesameJenaSelectWrapper sjswServices = processQuery(servicesQuery, servicesDB);
		processHardwareSoftwareProperties(sjswServices,  false, softwareModule);

		SesameJenaSelectWrapper sjswCore = processQuery(coreQuery, coreDB);
		processHardwareSoftwareProperties(sjswCore, true, softwareModule);

		// processing modifies class variable dataHash directly
		deleteData(removeDataHash);
		processData(dataHash);
	}


	private void processHardwareSoftwareProperties(SesameJenaSelectWrapper sjsw, boolean TAP_Core, boolean softwareModule)
	{
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			String module = sjss.getRawVar(vars[0]).toString();
			String prop = sjss.getRawVar(vars[1]).toString();
			Object value = sjss.getRawVar(vars[2]);
			String user = "";
			if(!TAP_Core)
			{
				user = sjss.getRawVar(vars[6]).toString();
			}

			if(dataHash.containsKey(module) || !TAP_Core)
			{
				Object[] returnTriple = new Object[3];
				if(!value.toString().equals("\"NA\"") && !value.toString().equals("\"TBD\"") && value != null && value instanceof Literal )
				{
					if(prop.equals(propURI + "Quantity"))
					{
						returnTriple = processSumValues(module, prop, value);
					}
					else if(prop.equals(propURI + "Comments"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}
					else if(prop.equals(propURI + "EOL"))
					{
						boolean max = true;
						returnTriple = processMinMaxDate(module, prop, value, max);
					}
					else if(prop.equals(propURI + "Manufacturer"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}
					else if(prop.equals(propURI + "Model"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}
					else if(prop.equals(propURI + "Product_Type"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}
					else if(prop.equals(propURI + "Master_Version"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}
					else if(prop.equals(propURI + "Major_Version"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}
					else if(prop.equals(propURI + "Vendor"))
					{
						returnTriple = processConcatString(module, prop, value, user);
					}

					// if error occurs
					if(Arrays.equals(returnTriple, new String[]{""}))
					{
						return;
					}

					// returnTriple never gets a value when the property being passed in isn't in the defined list above
					if(returnTriple[0] != null)
					{
						logger.debug("ADDING HARDWARE/SOFTWARE MODULE PROPERTY:     " + returnTriple[0] + " -----> {" + returnTriple[1] + " --- " + returnTriple[2].toString() + "}");
						addToHash(returnTriple);
					}

					// must remove existing triple in TAP Core prior to adding
					if(TAP_Core)
					{
						addToDeleteHash(new String[]{"<" + module + ">","<" + prop + ">", value.toString()});
					}

					// perform check to see if must add software/hardware version is software/hardware Module does not exist in TAP Core
					if(softwareModule)
					{
						if(!allSoftwareModules.contains(module))
						{
							String system = sjss.getRawVar(vars[3]).toString();
							String softwareV = sjss.getRawVar(vars[4]).toString();
							String software = sjss.getRawVar(vars[5]).toString();
							addToAllConcepts(system);
							addToAllConcepts(softwareV);
							addToAllConcepts(software);
							addToAllConcepts(module);

							String baseUri = getBaseURI(system);
							//relationship from system to softwareModule
							String predSysToMod = baseUri + "/Relatoin/Has/" + getTextAfterFinalDelimeter(system, "/") + ":" + getTextAfterFinalDelimeter(module, "/");
							addToAllRelationships(predSysToMod);
							logger.debug("SYSTEM TO SOFTWARE MODULE RELATIONSHIP DOES NOT EXIST IN TAP CORE");
							logger.debug("ADDING:     " + system + " -----> {" + predSysToMod + " --- " + module + "}");
							addToHash(new String[]{system, predSysToMod, module});
							//relationship from softwareModule to softwareVersion
							String predModToVer = baseUri + "/Relatoin/TypeOf/" + getTextAfterFinalDelimeter(module, "/") + ":" + getTextAfterFinalDelimeter(softwareV, "/");
							addToAllRelationships(predModToVer);
							logger.debug("SOFTWARE MODULE TO SOFTWARE VERSION RELATIONSHIP DOES NOT EXIST IN TAP CORE");
							logger.debug("ADDING:     " + module + " -----> {" + predModToVer + " --- " + softwareV + "}");
							addToHash(new String[]{module, predModToVer, softwareV});
							//relationship from software to softwareVersion
							String predSoffToVer = baseUri + "/Relatoin/Has/" + getTextAfterFinalDelimeter(software, "/") + ":" + getTextAfterFinalDelimeter(softwareV, "/");
							logger.debug("SOFTWARE TO SOFTWARE VERSION RELATIONSHIP DOES NOT EXIST IN TAP CORE");
							logger.debug("ADDING:     " + software + " -----> {" + predSoffToVer + " --- " + softwareV + "}");
							addToHash(new String[]{software, predSoffToVer, softwareV});
						}
					}
					else
					{
						if(!allHardwareModules.contains(module))
						{
							String system = sjss.getRawVar(vars[3]).toString();
							String hardwareV = sjss.getRawVar(vars[4]).toString();
							String hardware = sjss.getRawVar(vars[5]).toString();
							addToAllConcepts(system);
							addToAllConcepts(hardwareV);
							addToAllConcepts(hardware);
							addToAllConcepts(module);

							String baseUri = getBaseURI(system);
							//relationship from system to hardwareModule
							String predSysToMod = baseUri + "/Relatoin/Has/" + getTextAfterFinalDelimeter(system, "/") + ":" + getTextAfterFinalDelimeter(module, "/");
							addToAllRelationships(predSysToMod);
							logger.debug("SYSTEM TO HARDWARE MODULE RELATIONSHIP DOES NOT EXIST IN TAP CORE");
							logger.debug("ADDING:     " + system + " -----> {" + predSysToMod + " --- " + module + "}");
							addToHash(new String[]{system, predSysToMod, module});
							//relationship from hardwareModule to hardwareVersion
							String predModToVer = baseUri + "/Relatoin/TypeOf/" + getTextAfterFinalDelimeter(module, "/") + ":" + getTextAfterFinalDelimeter(hardwareV, "/");
							addToAllRelationships(predModToVer);
							logger.debug("HARDWARE MODULE TO HARDWARE VERSION RELATIONSHIP DOES NOT EXIST IN TAP CORE");
							logger.debug("ADDING:     " + module + " -----> {" + predModToVer + " --- " + hardwareV + "}");
							addToHash(new String[]{module, predModToVer, hardwareV});
							//relationship from software to softwareVersion
							String predhARDToVer = baseUri + "/Relatoin/Has/" + getTextAfterFinalDelimeter(hardware, "/") + ":" + getTextAfterFinalDelimeter(hardwareV, "/");
							logger.debug("HARDWARE TO HARDWARE VERSION RELATIONSHIP DOES NOT EXIST IN TAP CORE");
							logger.debug("ADDING:     " + hardware + " -----> {" + predhARDToVer + " --- " + hardwareV + "}");
							addToHash(new String[]{hardware, predhARDToVer, hardwareV});
						}
					}
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// methods used by all aggregation methods

	private void addToHash(Object[] returnTriple) 
	{
		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		innerHash.put(returnTriple[1].toString(), returnTriple[2]);
		if(dataHash.containsKey(returnTriple[0].toString()))
		{
			dataHash.get(returnTriple[0].toString()).putAll(innerHash);
		}
		else
		{
			dataHash.put(returnTriple[0].toString(), innerHash);
		}
	}

	private void addToDeleteHash(Object[] returnTriple)
	{
		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		innerHash.put(returnTriple[1].toString(), returnTriple[2]);
		if(removeDataHash.containsKey(returnTriple[0].toString()))
		{
			removeDataHash.get(returnTriple[0]).putAll(innerHash);
		}
		else
		{
			removeDataHash.put(returnTriple[0].toString(), innerHash);
		}
	}

	private void addToAllConcepts(String uri)
	{
		String conceptBaseURI = semossBaseURI + Utility.getClassName(uri);
		if(allConcepts.containsKey(conceptBaseURI))
		{
			allConcepts.get(conceptBaseURI).add(uri);
		}
		else
		{
			allConcepts.put(conceptBaseURI, new HashSet<String>());
			allConcepts.get(conceptBaseURI).add(uri);
		}		
	}

	private void addToAllRelationships(String uri)
	{
		String relationBaseURI = semossRelBaseURI + Utility.getClassName(uri);
		if(allRelations.containsKey(relationBaseURI))
		{
			allRelations.get(relationBaseURI).add(uri);
		}
		else
		{
			allRelations.put(relationBaseURI, new HashSet<String>());
			allRelations.get(relationBaseURI).add(uri);
		}
	}

	private void processData(Hashtable<String, Hashtable<String, Object>> data)
	{
		for( String sub : data.keySet())
		{
			for ( String pred : data.get(sub).keySet())
			{
				Object obj = data.get(sub).get(pred);
				boolean concept_triple = true;
				if( pred.contains("Relation/Contains"))
				{
					concept_triple = false;
				}
				( (BigDataEngine) coreDB).addStatement(sub, pred, obj, concept_triple);
				logger.info("ADDING INTO TAP CORE: " + sub + ">>>>>" + pred + ">>>>>" + obj + ">>>>>");
			}
		}
	}

	private void deleteData(Hashtable<String, Hashtable<String, Object>> data)
	{
		StringBuilder deleteQuery = new StringBuilder("DELETE DATA { ");
		boolean notEmpty = false;
		for ( String sub : data.keySet())
		{
			for (String pred : data.get(sub).keySet())
			{
				Object obj = data.get(sub).get(pred);
				if(!sub.equals("") && !pred.equals("") && !obj.equals(""))
				{
					notEmpty = true;
				}
				deleteQuery.append(sub + " " + pred + " " + obj + ". ");
			}
		}
		deleteQuery.append(" }");
		logger.info("DELETE QUERY: " + deleteQuery.toString());
		if(notEmpty)
		{
			UpdateProcessor proc = new UpdateProcessor();
			proc.setEngine(coreDB);
			proc.setQuery(deleteQuery.toString());
			proc.processQuery();
		}
	}

	private void processNewConcepts()
	{
		// get list of all concepts from tap core
		HashSet<String> conceptList = new HashSet<String>();
		logger.info("PROCESSING QUERY: " + TAP_CORE_CONCEPTS_LIST_QUERY);
		SesameJenaSelectWrapper sjsw = processQuery(TAP_CORE_CONCEPTS_LIST_QUERY, coreDB);
		String[] var = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			conceptList.add(sjss.getRawVar(var[0]) + "");
		}

		String pred = RDF.TYPE.toString();
		String concept = "http://semoss.org/ontologies/Concept";
		String subclassOf = RDFS.SUBCLASSOF.toString();
		for ( String obj : allConcepts.keySet())
		{
			for (String sub : allConcepts.get(obj) )
			{
				( (BigDataEngine) coreDB).addStatement(sub, pred, obj, true);
				logger.info("ADDING INSTANCE TYPEOF CONCEPT TRIPLE: " + sub + ">>>>>" + pred + ">>>>>" + obj + ">>>>>");
			}
			// add concepts that are not already in db
			if(!conceptList.contains(obj))
			{
				( (BigDataEngine) coreDB).addStatement(obj, subclassOf, concept, true);
				logger.info("ADDING NEW CONCEPT TRIPLE: " + obj + ">>>>>" + subclassOf + ">>>>>" + concept + ">>>>>");
			}
		}
	}

	private void processNewRelationships()
	{
		// get list of all relationships from tap core
		HashSet<String> relationshipList = new HashSet<String>();
		logger.info("PROCESSING QUERY: " + TAP_CORE_RELATIONS_LIST_QUERY);
		SesameJenaSelectWrapper sjsw = processQuery(TAP_CORE_RELATIONS_LIST_QUERY, coreDB);
		String[] var = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			relationshipList.add(sjss.getRawVar(var[0]) + "");
		}
		
		String relation = "http://semoss.org/ontologies/Relation";
		String subpropertyOf = RDFS.SUBPROPERTYOF.toString();
		for ( String obj : allRelations.keySet())
		{
			for (String sub : allRelations.get(obj) )
			{
				( (BigDataEngine) coreDB).addStatement(sub, subpropertyOf, obj, true);
				logger.info("ADDING RELATIONSHIP INSTANCE SUBPROPERTY TRIPLE: " + sub + ">>>>>" + subpropertyOf + ">>>>>" + obj + ">>>>>");
			}
			// add relationships that are not already in db
			if(!relationshipList.contains(obj))
			{
				( (BigDataEngine) coreDB).addStatement(obj, subpropertyOf, relation, true);
				logger.info("ADDING NEW RELATIONSHIP TRIPLE: " + obj + ">>>>>" + subpropertyOf + ">>>>>" + relation + ">>>>>");
			}
		}	
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// general methods for properties

	private Object[] processSumValues(String sub, String prop, Object value)
	{
		try
		{
			((Literal) value).doubleValue();
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
			this.errorMessage = this.errorMessage + "Error Processing Max/Min Double. Please check value of Double. \n" 
					+ "Error occured processing: " + sub + ">>>>" + prop + ">>>>" + value + "\n";	
			return new String[]{""};
		}

		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			value = ((Literal) value).doubleValue();
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		else
		{
			innerHash = dataHash.get(sub);
			Double addValue = ( (Literal) value).doubleValue();
			Double currentValue = (Double) innerHash.get(prop);
			value = addValue + currentValue;
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		return new Object[]{sub, prop, value};
	}

	private Object[] processConcatString(String sub, String prop, Object value, String user) 
	{
		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			if(!user.equals(""))
			{
				value = "\"" + getTextAfterFinalDelimeter(user, "/") + ":" + value.toString().substring(1);
			}
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		else
		{
			innerHash = dataHash.get(sub);
			Object currentString = innerHash.get(prop);
			if(!user.equals(""))
			{
				value = currentString.toString().substring(0, currentString.toString().length()-1) + ";" + getTextAfterFinalDelimeter(user, "/") + ":" + value.toString().substring(1);
			}
			else
			{
				value = currentString.toString().substring(0, currentString.toString().length()-1) + ";" + value.toString().substring(1);
			}
			logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		return new Object[]{sub, prop, value};
	}


	private Object[] processMaxMinDouble(String sub, String prop, Object value, boolean max)
	{
		try
		{
			((Literal) value).doubleValue();
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
			this.errorMessage = this.errorMessage + "Error Processing Max/Min Double. Please check value of Double. \n" 
					+ "Error occured processing: " + sub + ">>>>" + prop + ">>>>" + value + "\n";	
			return new String[]{""};
		}

		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			value = ((Literal) value).doubleValue();
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		else
		{
			innerHash = dataHash.get(sub);
			Double oldDouble = (Double) innerHash.get(prop);
			Double newDouble = ((Literal) value).doubleValue();
			if(!max)
			{
				if(newDouble < oldDouble)
				{
					// return the value being passed in
					value = ((Literal) value).doubleValue();
					logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + value + "}");
				}
				// if the new value is not to be used, return the originally value already in dataHash
				else
				{
					value = innerHash.get(prop);
				}
			}
			else
			{
				if(newDouble > oldDouble)
				{
					// return the value being passed in
					value = ((Literal) value).doubleValue();
					logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + value + "}");
				}
				// if the new value is not to be used, return the originally value already in dataHash
				else
				{
					value = innerHash.get(prop);
				}
			}
		}
		return new Object[]{sub, prop, value};
	}

	private Object[] processMinMaxDate(String sub, String prop, Object value, Boolean latest) 
	{
		try
		{
			((Literal) value).calendarValue();
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
			this.errorMessage = this.errorMessage + "Error Processing Max/Min Date. Please check value of Date. \n" 
					+ "Error occured processing: " + sub + ">>>>" + prop + ">>>>" + value + "\n";	
			return new String[]{""};
		}

		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			value = ((Literal) value).calendarValue();
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		else
		{
			innerHash = dataHash.get(sub);
			XMLGregorianCalendar oldDate = (XMLGregorianCalendar) innerHash.get(prop);
			XMLGregorianCalendar newDate = ((Literal) value).calendarValue();
			if(!latest)
			{
				if(newDate.toGregorianCalendar().getTime().before(oldDate.toGregorianCalendar().getTime()))
				{
					// return the value being passed in
					value = ((Literal) value).calendarValue();
					logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + value + "}");
				}
				// if the new value is not to be used, return the originally value already in dataHash
				else
				{
					value = innerHash.get(prop);
				}
			}
			else
			{
				if(newDate.toGregorianCalendar().getTime().after(oldDate.toGregorianCalendar().getTime()))
				{
					// return the value being passed in
					value = ((Literal) value).calendarValue();
					logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + value + "}");
				}
				// if the new value is not to be used, return the originally value already in dataHash
				else
				{
					value = innerHash.get(prop);
				}
			}
		}
		return new Object[]{sub, prop, value};
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Unique methods for properties

	private Object[] processGarrisonTheater(String sub, String prop, Object value)
	{
		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		else
		{
			innerHash = dataHash.get(sub);
			Object oldGT = innerHash.get(prop);
			if(!oldGT.toString().toString().equalsIgnoreCase(value.toString()))
			{
				value = "\"Both\"";
				logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + "\"Both\"" + "}");
			}
		}
		return new Object[]{sub, prop, value};
	}

	private Object[] processTransactional(String sub, String prop, Object value)
	{
		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		//Different SystemServices should not be sending different transactional value
		//perform check to make sure data is correct
		else
		{
			innerHash = dataHash.get(sub);
			Object currentTransactional = innerHash.get(prop);
			if(!currentTransactional.toString().toString().equalsIgnoreCase(value.toString()))
			{
				this.errorMessage = this.errorMessage + "Error Processing Transactional!  Conflicting report from systems \n" 
						+ "Error occured processing: " + sub + ">>>>" + prop + ">>>>" + value + "\n"						;				
				return new Object[]{""};
			}
		}
		return new Object[]{sub, prop, value};
	}

	private Object[] processDFreq(String sub, String prop, Object value) 
	{
		Hashtable<String, Object> innerHash = new Hashtable<String, Object>();
		if(!dataHash.containsKey(sub) || !dataHash.get(sub).containsKey(prop))
		{
			logger.info("ADDING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		else
		{
			innerHash = dataHash.get(sub);
			String[] frequencies = new String[]{value.toString().replaceAll("\"", ""), innerHash.get(prop).toString().replaceAll("\"", "")};
			Integer[] currentFreqValue = new Integer[2];

			for(int i = 0; i < frequencies.length; i++)
			{
				if(frequencies[i].equalsIgnoreCase("Real-time (user-initiated)")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (monthly)")) currentFreqValue[i] = 720;
				else if(frequencies[i].equalsIgnoreCase("Weekly")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("Monthly")) currentFreqValue[i] = 720;
				else if(frequencies[i].equalsIgnoreCase("Batch (daily)")) currentFreqValue[i] = 24;
				else if(frequencies[i].equalsIgnoreCase("Batch(Daily)")) currentFreqValue[i] = 24;
				else if(frequencies[i].equalsIgnoreCase("Real-time")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Transactional")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("On Demand")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Event Driven (seconds-minutes)")) currentFreqValue[i] = 60;
				else if(frequencies[i].equalsIgnoreCase("TheaterFramework")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Event Driven (Seconds)")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Web services")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("TF")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (12/day)")) currentFreqValue[i] = 2;
				else if(frequencies[i].equalsIgnoreCase("SFTP")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (twice monthly)")) currentFreqValue[i] = 360;
				else if(frequencies[i].equalsIgnoreCase("Daily")) currentFreqValue[i] = 24;
				else if(frequencies[i].equalsIgnoreCase("Hourly")) currentFreqValue[i] = 1;
				else if(frequencies[i].equalsIgnoreCase("Near Real-time (transaction initiated)")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (three times a week)")) currentFreqValue[i] = 56;
				else if(frequencies[i].equalsIgnoreCase("Batch (weekly)")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("Near Real-time")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Real Time")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (bi-monthly)")) currentFreqValue[i] = 1440;
				else if(frequencies[i].equalsIgnoreCase("Batch (semiannually)")) currentFreqValue[i] = 4392;
				else if(frequencies[i].equalsIgnoreCase("Event Driven (Minutes-hours)")) currentFreqValue[i] = 1;
				else if(frequencies[i].equalsIgnoreCase("Annually")) currentFreqValue[i] = 8760;
				else if(frequencies[i].equalsIgnoreCase("Batch(Monthly)")) currentFreqValue[i] = 720;
				else if(frequencies[i].equalsIgnoreCase("Bi-Weekly")) currentFreqValue[i] = 336;
				else if(frequencies[i].equalsIgnoreCase("Daily at end of day")) currentFreqValue[i] = 24;
				else if(frequencies[i].equalsIgnoreCase("TCP")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("event-driven (Minutes-hours)")) currentFreqValue[i] = 1;
				else if(frequencies[i].equalsIgnoreCase("Interactive")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Weekly Quarterly")) currentFreqValue[i] = 2184;
				else if(frequencies[i].equalsIgnoreCase("Weekly Daily Weekly Weekly Weekly Weekly Daily Daily Daily")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("Weekly Daily")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("Periodic")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (4/day)")) currentFreqValue[i] = 6;
				else if(frequencies[i].equalsIgnoreCase("Batch(Daily/Monthly)")) currentFreqValue[i] = 720;
				else if(frequencies[i].equalsIgnoreCase("Weekly; Interactive; Interactive")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("interactive")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch (quarterly)")) currentFreqValue[i] = 2184;
				else if(frequencies[i].equalsIgnoreCase("Every 8 hours (KML)/On demand (HTML)")) currentFreqValue[i] = 8;
				else if(frequencies[i].equalsIgnoreCase("Monthly at beginning of month, or as user initiated")) currentFreqValue[i] = 720;
				else if(frequencies[i].equalsIgnoreCase("On demad")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Monthly Bi-Monthly Weekly Weekly")) currentFreqValue[i] = 720;
				else if(frequencies[i].equalsIgnoreCase("Quarterly")) currentFreqValue[i] = 2184;
				else if(frequencies[i].equalsIgnoreCase("On-demand")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("user upload")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("1/hour (KML)/On demand (HTML)")) currentFreqValue[i] = 1;
				else if(frequencies[i].equalsIgnoreCase("DVD")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Real-time ")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Weekly ")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("Annual")) currentFreqValue[i] = 8760;
				else if(frequencies[i].equalsIgnoreCase("Daily Interactive")) currentFreqValue[i] = 24;
				else if(frequencies[i].equalsIgnoreCase("NFS, Oracle connection")) currentFreqValue[i] = 0;
				else if(frequencies[i].equalsIgnoreCase("Batch(Weekly)")) currentFreqValue[i] = 168;
				else if(frequencies[i].equalsIgnoreCase("Batch(Quarterly)")) currentFreqValue[i] = 2184;
				else if(frequencies[i].equalsIgnoreCase("Batch (yearly)")) currentFreqValue[i] = 8760;
				else if(frequencies[i].equalsIgnoreCase("Each user login instance")) currentFreqValue[i] = 0;
			}

			if(currentFreqValue[0] == null || currentFreqValue[1] == null)
			{
				this.errorMessage = this.errorMessage + "Error Processing DFreq!  Check frequency is predefined in list. \n" 
						+ "Error occured processing: " + sub + ">>>>" + prop + ">>>>" + value + "\n";	
				return new String[]{""};
			}

			if(currentFreqValue[0] > currentFreqValue[1])
			{
				value = innerHash.get(prop);
			}
			// else, do not change the value to keep the one being inputed
			logger.info("ADJUSTING:     " + sub + " -----> {" + prop + " --- " + value + "}");
		}
		return new Object[]{sub, prop, value};
	}


	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Utility methods 

	private String getTextAfterFinalDelimeter(String uri, String delimeter)
	{
		if(!uri.equals(""))
		{
			uri = uri.substring(uri.lastIndexOf(delimeter)+1);
		}
		return uri;
	}

	private String getBaseURI(String uri)
	{
		return uri.substring(0, uri.substring(0, uri.substring(0, uri.lastIndexOf("/")).lastIndexOf("/")).lastIndexOf("/"));
	}

	//process the query
	private SesameJenaSelectWrapper processQuery(String query, IEngine engine){
		logger.info("PROCESSING QUERY: " + query);
		SesameJenaSelectWrapper sjsw = new SesameJenaSelectWrapper();
		//run the query against the engine provided
		sjsw.setEngine(engine);
		sjsw.setQuery(query);
		sjsw.executeQuery();		
		sjsw.getVariables();
		return sjsw;
	}

	private Hashtable<String, Hashtable<String, LinkedList<Object>>> runAggregateAllData(SesameJenaSelectWrapper sjsw, Hashtable<String, Hashtable<String, LinkedList<Object>>> aggregatedData, String propType, boolean TAP_Core)
	{
		String[] vars = sjsw.getVariables();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			// get the next row and see how it must be added to the insert query
			String sys = sjss.getRawVar(vars[0]).toString();
			String pred = sjss.getRawVar(vars[1]).toString();
			String obj = sjss.getRawVar(vars[2]).toString();
			Object prop = sjss.getRawVar(vars[3]);

			if(!TAP_Core)
			{
				pred = pred.substring(0, pred.lastIndexOf("/")) + "/" + getTextAfterFinalDelimeter(sys, "/") +":" + getTextAfterFinalDelimeter(obj, "/");
			}

			if(aggregatedData.containsKey(sys) || !TAP_Core)
			{
				LinkedList<Object> dataList = new LinkedList<Object>();
				Hashtable<String, LinkedList<Object>> innerHash = new Hashtable<String, LinkedList<Object>>();
				if(!aggregatedData.containsKey(sys))
				{
					dataList.add(pred);
					dataList.add(prop);
					innerHash.put(obj, dataList);
					aggregatedData.put(sys, innerHash);
					logger.debug("ADDING NEW DATA LIST:     " + sys + " -----> {" + obj + " --- " + dataList.toString() + "}");
				}
				else
				{
					if(!aggregatedData.get(sys).containsKey(obj))
					{
						innerHash = aggregatedData.get(sys);
						dataList.add(pred);
						dataList.add(prop);
						innerHash.put(obj, dataList);
						logger.debug("ADDING NEW DATA LIST:     " + sys + " -----> {" + obj + " --- " + dataList.toString() + "}");
					}
					else
					{
						innerHash = aggregatedData.get(sys);
						dataList = innerHash.get(obj);
						dataList.add(prop);
						logger.debug("ADJUSTING DATA LIST:     " + sys + " -----> {" + obj + " --- " + dataList.toString() + "}");
					}
				}

				// add instances to master list
				addToAllConcepts(sys);
				addToAllConcepts(obj);

				if(TAP_Core)
				{
					addToDeleteHash(new Object[]{pred, propURI + propType, prop});
				}
				addToAllRelationships(pred);
			}
		}
		return aggregatedData;
	}
}
