package prerna.ui.components.specific.tap;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import prerna.poi.specific.IndividualSystemTransitionReportWriter;
import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.SesameJenaSelectStatement;
import prerna.rdf.engine.impl.SesameJenaSelectWrapper;
import prerna.ui.components.playsheets.AbstractRDFPlaySheet;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class IndividualLPISystemTransitionReport extends AbstractRDFPlaySheet{

	// list of queries to run
	private String sysInfoQuery = "SELECT DISTINCT ?Description (COALESCE(?GT, 'Garrison') AS ?GarrisonTheater) (IF(BOUND(?MU),'Yes','No') AS ?MHS_Specific) (COALESCE(IF(DATATYPE(?TC) = xsd:double,?TC,-1),-1) AS ?Transaction_Count) (COALESCE(SUBSTR(STR(?ATO),0,10),'NA') AS ?ATO_Date) (COALESCE(SUBSTR(STR(?ES),0,10),'NA') AS ?End_Of_Support) (COALESCE(IF(DATATYPE(?NU) = xsd:double,?NU,-1),-1) AS ?Num_Users) WHERE { BIND(@SYSTEM@ AS ?System){?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/MHS_Specific> ?MU}} OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/Description> ?Description}}OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/GarrisonTheater> ?GT}} OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/Transaction_Count> ?TC}} OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/ATO_Date> ?ATO}} OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/End_of_Support_Date> ?ES}} OPTIONAL{{?System <http://semoss.org/ontologies/Relation/Contains/Number_of_Users> ?NU}} }";
	private String sysDataConsumeProvideQuery = "SELECT DISTINCT ?Data ?ProvideOrConsume (GROUP_CONCAT(?Ser; SEPARATOR = ', ') AS ?Services) WHERE { {SELECT DISTINCT ?System ?Data ?ProvideOrConsume (SUBSTR(STR(?Service), 46) AS ?Ser) WHERE {BIND(@SYSTEM@ AS ?System){?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?otherSystem <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?Service <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Service>} {?provideData <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>}{?provideData <http://semoss.org/ontologies/Relation/Contains/CRM> ?crm} {?System <http://semoss.org/ontologies/Relation/Provide> ?icd} {?icd <http://semoss.org/ontologies/Relation/Consume> ?otherSystem} {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data} {?Service <http://semoss.org/ontologies/Relation/Exposes> ?Data} {?System ?provideData ?Data} FILTER( !regex(str(?crm),'R')) BIND('Provide' AS ?ProvideOrConsume)}} UNION {SELECT DISTINCT ?System ?Data ?ProvideOrConsume (SUBSTR(STR(?Service), 46) AS ?Ser) WHERE {BIND(@SYSTEM@ AS ?System){?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?otherSystem <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?Service <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Service>} OPTIONAL {{?icd2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?icd2 <http://semoss.org/ontologies/Relation/Consume> ?System} {?icd2 <http://semoss.org/ontologies/Relation/Payload> ?Data}} {?System <http://semoss.org/ontologies/Relation/Provide> ?icd} {?icd <http://semoss.org/ontologies/Relation/Consume> ?otherSystem} {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data} {?Service <http://semoss.org/ontologies/Relation/Exposes> ?Data}  FILTER(!BOUND(?icd2)) BIND('Provide' AS ?ProvideOrConsume)}} UNION {SELECT DISTINCT ?System ?Data ?ProvideOrConsume (SUBSTR(STR(?Service), 46) AS ?Ser) WHERE {BIND(@SYSTEM@ AS ?System){?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?otherSystem <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?Service <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Service>}   {?otherSystem <http://semoss.org/ontologies/Relation/Provide> ?icd} {?icd <http://semoss.org/ontologies/Relation/Consume> ?System} {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data} {?Service <http://semoss.org/ontologies/Relation/Exposes> ?Data} BIND('Consume' AS ?ProvideOrConsume)}} } GROUP BY ?System ?Data ?ProvideOrConsume ORDER BY ?System";
	private String sysSORDataWithDHMSMQuery = "SELECT DISTINCT ?Data ?DHMSM_SOR WHERE { {SELECT DISTINCT ?System ?Data (IF(BOUND(?Needs),'Yes','No') AS ?DHMSM_SOR) WHERE { BIND(@SYSTEM@ AS ?System){?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>}  {?System <http://semoss.org/ontologies/Relation/Provide> ?icd} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?provideData <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>} {?provideData <http://semoss.org/ontologies/Relation/Contains/CRM> ?crm} {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data} {?System ?provideData ?Data} FILTER( !regex(str(?crm),'R')) OPTIONAL { BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM) {?DHMSM <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DHMSM>} {?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>} {?DHMSM <http://semoss.org/ontologies/Relation/TaggedBy> ?Capability} {?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>;} {?Capability <http://semoss.org/ontologies/Relation/Consists> ?Task} {?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>} {?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> 'C'} {?Task ?Needs ?Data} } } } UNION {SELECT DISTINCT ?System ?Data (IF(BOUND(?Needs),'Yes','No') AS ?DHMSM_SOR) WHERE {BIND(@SYSTEM@ AS ?System) {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>}{?System <http://semoss.org/ontologies/Relation/Provide> ?icd} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data} OPTIONAL { {?icd2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument>} {?icd2 <http://semoss.org/ontologies/Relation/Consume> ?System} {?icd2 <http://semoss.org/ontologies/Relation/Payload> ?Data} } FILTER(!BOUND(?icd2)) OPTIONAL { BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM) {?DHMSM <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DHMSM>} {?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>} {?DHMSM <http://semoss.org/ontologies/Relation/TaggedBy> ?Capability} {?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>;} {?Capability <http://semoss.org/ontologies/Relation/Consists> ?Task} {?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>} {?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> 'C'} {?Task ?Needs ?Data} } }} } ORDER BY ?System";
	private String sysSORDataWithDHMSMCapQuery = "SELECT DISTINCT ?Capability ?Data (GROUP_CONCAT(DISTINCT ?Comment ; Separator = ' and ') AS ?Comments) WHERE { { SELECT DISTINCT ?Capability ?Data ?System ?Comment ?DHMSMcrm WHERE { BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM) {?DHMSM <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DHMSM>} {?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>} {?DHMSM <http://semoss.org/ontologies/Relation/TaggedBy> ?Capability} {?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} BIND(@SYSTEM@ AS ?System) {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem> } {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> } {?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>} {?provideData <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide> }{?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> 'R'} {?provideData <http://semoss.org/ontologies/Relation/Contains/CRM> ?SystemProvideData_CRM} FILTER( !regex(str(?SystemProvideData_CRM),'R')) {?Capability <http://semoss.org/ontologies/Relation/Consists> ?Task} {?System <http://semoss.org/ontologies/Relation/Provide> ?icd} {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data} {?System ?provideData ?Data} {?Task ?Needs ?Data} BIND('ICD validated through downstream interfaces' AS ?Comment) { SELECT DISTINCT ?Data (GROUP_CONCAT(DISTINCT ?Crm ; separator = ',') AS ?DHMSMcrm) WHERE { {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;} BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM ) {?DHMSM <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DHMSM>} {?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>;} {?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>;} {?DHMSM <http://semoss.org/ontologies/Relation/TaggedBy> ?Capability.} {?Capability <http://semoss.org/ontologies/Relation/Consists> ?Task.} {?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>;} {?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> ?Crm;} {?Task ?Needs ?Data.} } GROUP BY ?Data } BIND('R' AS ?DHMSMcrm) } } UNION { SELECT DISTINCT ?Capability ?Data ?System ?Comment ?DHMSMcrm WHERE { BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM) {?DHMSM <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DHMSM>} {?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>} {?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>} {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>} BIND(@SYSTEM@ AS ?System) {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem> } {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> } {?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>} {?DHMSM <http://semoss.org/ontologies/Relation/TaggedBy> ?Capability} {?Capability <http://semoss.org/ontologies/Relation/Consists> ?Task} {?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> 'R'} {?Task ?Needs ?Data} OPTIONAL { {?icd2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> } {?icd2 <http://semoss.org/ontologies/Relation/Consume> ?System} {?icd2 <http://semoss.org/ontologies/Relation/Payload> ?Data} } {?System <http://semoss.org/ontologies/Relation/Provide> ?icd } {?icd <http://semoss.org/ontologies/Relation/Payload> ?Data } FILTER(!BOUND(?icd2)) BIND('ICD implied through downstream and no upstream interfaces' AS ?Comment) { SELECT DISTINCT ?Data (GROUP_CONCAT(DISTINCT ?Crm ; separator = ',') AS ?DHMSMcrm) WHERE { {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;} BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM ) {?DHMSM <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DHMSM>} {?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>;} {?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>;} {?DHMSM <http://semoss.org/ontologies/Relation/TaggedBy> ?Capability.} {?Capability <http://semoss.org/ontologies/Relation/Consists> ?Task.} {?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>;} {?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> ?Crm;} {?Task ?Needs ?Data.} } GROUP BY ?Data } BIND('R' AS ?DHMSMcrm) } } } GROUP BY ?Capability ?Data ?System ORDER BY ?Capability ?Data ?System";
	private String softwareLifeCycleQuery = "SELECT DISTINCT ?System ?SoftwareVersion (COALESCE(xsd:dateTime(?SoftMEOL), COALESCE(xsd:dateTime(?SoftVEOL),'TBD')) AS ?SupportDate) (COALESCE(xsd:dateTime(?SoftMEOL), COALESCE(xsd:dateTime(?SoftVEOL),'TBD')) AS ?LifeCycle) (COALESCE(?unitcost,0) AS ?UnitCost) (COALESCE(?quantity,0) AS ?Quantity) (COALESCE(?UnitCost*?Quantity,0) AS ?TotalCost) (COALESCE(?budget,0) AS ?SystemSWBudget) WHERE { BIND(@SYSTEM@ AS ?System){?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?SoftwareModule <http://semoss.org/ontologies/Relation/Contains/Quantity> ?Quantity} {?Has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consists>} {?SoftwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SoftwareModule>} {?TypeOf <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/TypeOf>} {?SoftwareVersion <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SoftwareVersion>} {?System ?Has ?SoftwareModule} {?SoftwareModule ?TypeOf ?SoftwareVersion} OPTIONAL {?SoftwareVersion <http://semoss.org/ontologies/Relation/Contains/EOL> ?SoftVEOL} OPTIONAL {?SoftwareModule <http://semoss.org/ontologies/Relation/Contains/EOL> ?SoftMEOL} OPTIONAL {?SoftwareVersion <http://semoss.org/ontologies/Relation/Contains/Price> ?unitcost} } ORDER BY ?System ?SoftwareVersion";
	private String hardwareLifeCycleQuery = "SELECT DISTINCT ?System ?HardwareVersion (COALESCE(xsd:dateTime(?HardMEOL), COALESCE(xsd:dateTime(?HardVEOL),'TBD')) AS ?SupportDate) (COALESCE(xsd:dateTime(?HardMEOL), COALESCE(xsd:dateTime(?HardVEOL),'TBD')) AS ?LifeCycle) (COALESCE(?unitcost,0) AS ?UnitCost) (COALESCE(?quantity,0) AS ?Quantity) (COALESCE(?UnitCost*?Quantity,0) AS ?TotalCost) (COALESCE(?budget,0) AS ?SystemHWBudget) WHERE { BIND(@SYSTEM@ AS ?System) {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?HardwareModule <http://semoss.org/ontologies/Relation/Contains/Quantity> ?Quantity} {?Has <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has>} {?HardwareModule <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/HardwareModule>} {?TypeOf <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/TypeOf>} {?HardwareVersion <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/HardwareVersion>} {?System ?Has ?HardwareModule} {?HardwareModule ?TypeOf ?HardwareVersion } OPTIONAL {?HardwareVersion <http://semoss.org/ontologies/Relation/Contains/EOL> ?HardVEOL} OPTIONAL {?HardwareModule <http://semoss.org/ontologies/Relation/Contains/EOL> ?HardMEOL} OPTIONAL {?HardwareVersion <http://semoss.org/ontologies/Relation/Contains/Price> ?unitcost} } ORDER BY ?System ?HardwareVersion";
	private String sysInterfacesQuery = "HR_Core$HR_Core$HR_Core$HR_Core$SELECT DISTINCT ?LPISystem ?InterfaceType ?InterfacingSystem ?Probability (COALESCE(?interface,'') AS ?Interface) ?Data (COALESCE(?format,'') AS ?Format) (COALESCE(?Freq,'') AS ?Frequency) (COALESCE(?Prot,'') AS ?Protocol) ?DHMSM ?Comment WHERE { {SELECT DISTINCT (IF(BOUND(?y),?DownstreamSys,IF(BOUND(?x),?UpstreamSys,'')) AS ?LPISystem)  (IF(BOUND(?y),'Upstream',IF(BOUND(?x),'Downstream','')) AS ?InterfaceType)  (IF(BOUND(?y),?UpstreamSys,IF(BOUND(?x),?DownstreamSys,'')) AS ?InterfacingSystem) (COALESCE(IF(BOUND(?y),IF(!REGEX(STR(?UpstreamSysProb1),'High'),'Low','High'),IF(BOUND(?x),IF(!REGEX(STR(?DownstreamSysProb1),'High'),'Low','High'),'')), '') AS ?Probability) ?interface ?Data ?format ?Freq ?Prot (IF((STRLEN(?DHMSMcrm)<1),'',IF((REGEX(STR(?DHMSMcrm),'C')),'Provides','Consumes')) AS ?DHMSM) (COALESCE(?HIEsys, '') AS ?HIE) ?DHMSMcrm WHERE { {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;} BIND('N' AS ?InterfaceYN) BIND('Y' AS ?InterfaceDHMSM) BIND('Y' AS ?ReceivedInformation) BIND('High' AS ?Prob) LET(?d := 'd') OPTIONAL{ { {?UpstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>}  {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> ?InterfaceYN;} {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Interface_Needed_w_DHMSM> ?InterfaceDHMSM;}{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?UpstreamSysProb;}OPTIONAL{{?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/HIE> ?HIEsys;}{?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?DownstreamSysProb1;}} {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Received_Information> ?ReceivedInformation;}  MINUS {{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?Prob;}}{?interface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> ;} {?carries <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>;}  {?interface ?carries ?Data;} {?DownstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>;} {?Upstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>;}{?Downstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consume>;} {?UpstreamSys ?Upstream ?interface ;}{?interface ?Downstream ?DownstreamSys ;} { {?carries <http://semoss.org/ontologies/Relation/Contains/Format> ?format ;}{?carries <http://semoss.org/ontologies/Relation/Contains/Frequency> ?Freq ;} {?carries <http://semoss.org/ontologies/Relation/Contains/Protocol> ?Prot ;} } LET(?x :=REPLACE(str(?d), 'd', 'x')) } UNION {{?DownstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> ?InterfaceYN;} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Interface_Needed_w_DHMSM> ?InterfaceDHMSM;}{?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?DownstreamSysProb;}OPTIONAL{{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/HIE> ?HIEsys;}{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?UpstreamSysProb1;}} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Received_Information> ?ReceivedInformation;} MINUS {{?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?Prob;} }{?interface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> ;} {?carries <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>;}  {?interface ?carries ?Data;} {?UpstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>;} {?Upstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>;}{?Downstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consume>;} {?UpstreamSys ?Upstream ?interface ;}{?interface ?Downstream ?DownstreamSys ;} { {?carries <http://semoss.org/ontologies/Relation/Contains/Format> ?format ;} {?carries <http://semoss.org/ontologies/Relation/Contains/Frequency> ?Freq ;}{?carries <http://semoss.org/ontologies/Relation/Contains/Protocol> ?Prot ;} } LET(?y :=REPLACE(str(?d), 'd', 'y')) } } {SELECT DISTINCT ?Data (GROUP_CONCAT(DISTINCT ?Crm ; separator = ',') AS ?DHMSMcrm) WHERE {{?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;} OPTIONAL{BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM ){?TaggedBy <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/TaggedBy>;}{?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>;}{?DHMSM ?TaggedBy ?Capability.}{?Consists <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consists>;}{?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>;}{?Capability ?Consists ?Task.}{?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>;}{?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> ?Crm;}{?Task ?Needs ?Data.}} } GROUP BY ?Data} }} FILTER(REGEX(STR(?LPISystem), '@SYSTEMNAME@')) }$SELECT DISTINCT (CONCAT(STR(?system), STR(?data)) AS ?sysDataKey) WHERE { { {?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem> } {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> } {?provideData <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>} {?system <http://semoss.org/ontologies/Relation/Provide> ?icd} {?provideData <http://semoss.org/ontologies/Relation/Contains/CRM> ?crm} filter( !regex(str(?crm),'R')) {?icd <http://semoss.org/ontologies/Relation/Payload> ?data} {?system ?provideData ?data} } UNION { {?system <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem> ;} {?icd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> ;} {?system <http://semoss.org/ontologies/Relation/Provide> ?icd } {?icd <http://semoss.org/ontologies/Relation/Payload> ?data} OPTIONAL{ {?icd2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> ;} {?icd2 <http://semoss.org/ontologies/Relation/Consume> ?system} {?icd2 <http://semoss.org/ontologies/Relation/Payload> ?data} } FILTER(!BOUND(?icd2)) } } ORDER BY ?data ?system$SELECT DISTINCT ?System WHERE { BIND(@SYSTEM@ AS ?System) {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?System <http://semoss.org/ontologies/Relation/Contains/Received_Information> 'Y'} {?System <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> 'N'}}$SELECT DISTINCT ?System WHERE { {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?System <http://semoss.org/ontologies/Relation/Contains/HIE> ?HIEsys;} } BINDINGS ?HIEsys {('Y')}";
	
	private String headerKey = "headers";
	private String dataKey = "data";

	private IEngine hr_Core;
	private IEngine TAP_Cost_Data;
	
	private String[] dates = new String[]{"2016&June","2017&April","2022&July"}; 
	
	Logger logger = Logger.getLogger(getClass());

	@Override
	public void createData() {
		try{
			hr_Core = (IEngine) DIHelper.getInstance().getLocalProp("HR_Core");
			TAP_Cost_Data = (IEngine) DIHelper.getInstance().getLocalProp("TAP_Cost_Data");
		} catch(Exception e) {
			e.printStackTrace();
			Utility.showError("Could not find necessary databases:\nHR_Core, TAP_Cost_Data");
		}

		String systemURI = this.query;

		sysInfoQuery = sysInfoQuery.replace("@SYSTEM@", systemURI);
		sysDataConsumeProvideQuery = sysDataConsumeProvideQuery.replace("@SYSTEM@", systemURI);
		sysSORDataWithDHMSMQuery = sysSORDataWithDHMSMQuery.replace("@SYSTEM@", systemURI);
		sysSORDataWithDHMSMCapQuery = sysSORDataWithDHMSMCapQuery.replace("@SYSTEM@", systemURI);
		softwareLifeCycleQuery = softwareLifeCycleQuery.replace("@SYSTEM@", systemURI);
		hardwareLifeCycleQuery = hardwareLifeCycleQuery.replace("@SYSTEM@", systemURI);
		sysInterfacesQuery = sysInterfacesQuery.replaceAll("@SYSTEM@", systemURI);
		sysInterfacesQuery = sysInterfacesQuery.replaceAll("@SYSTEMNAME@", systemURI.substring(systemURI.lastIndexOf("/")+1,systemURI.lastIndexOf(">")));
		
		HashMap<String, Object> sysInfoHash = getQueryData(hr_Core, sysInfoQuery);
		HashMap<String, Object> sysDataConsumeProvideHash = getQueryData(hr_Core, sysDataConsumeProvideQuery);
		HashMap<String, Object> sysSORDataWithDHMSMHash = getQueryData(hr_Core, sysSORDataWithDHMSMQuery);
		HashMap<String, Object> sysSORDataWithDHMSMCapHash = getQueryData(hr_Core, sysSORDataWithDHMSMCapQuery);
		
		HashMap<Integer, HashMap<String, Object>> storeSoftwareData = new HashMap<Integer, HashMap<String, Object>>();
		HashMap<Integer, HashMap<String, Object>> storeHardwareData = new HashMap<Integer, HashMap<String, Object>>();

		LifeCycleGridPlaySheet getSoftwareHardwareData = new LifeCycleGridPlaySheet();
		for(int i = 0; i < dates.length; i++)
		{
			getSoftwareHardwareData.engine = hr_Core;
			getSoftwareHardwareData.setQuery(dates[i] + "&" + softwareLifeCycleQuery);
			ArrayList<Object[]> dataRow = getSoftwareHardwareData.processQuery(getSoftwareHardwareData.getQuery());
			String[] names = getSoftwareHardwareData.getNames();
			dataRow  = removeSystemFromArrayList(dataRow);
			names = removeSystemFromStringArray(names);
			HashMap<String, Object> innerMap = new HashMap<String, Object>();
			innerMap.put(headerKey, names);
			innerMap.put(dataKey, dataRow);
			storeSoftwareData.put(i, innerMap);
		}
		for(int i = 0; i < dates.length; i++)
		{
			getSoftwareHardwareData.setQuery(dates[i] + "&" + softwareLifeCycleQuery);
			String[] names = getSoftwareHardwareData.getNames();
			ArrayList<Object[]> dataRow = getSoftwareHardwareData.processQuery(getSoftwareHardwareData.getQuery());
			dataRow  = removeSystemFromArrayList(dataRow);
			names = removeSystemFromStringArray(names);
			HashMap<String, Object> innerMap = new HashMap<String, Object>();
			innerMap.put(headerKey, names);
			innerMap.put(dataKey, dataRow);
			storeHardwareData.put(i, innerMap);
		}
		
		LPIInterfaceReportGenerator getSysLPIInterfaceData = new LPIInterfaceReportGenerator();
		getSysLPIInterfaceData.setQuery(sysInterfacesQuery);
		getSysLPIInterfaceData.createData();
		HashMap<String, Object> sysLPIInterfaceHash = new HashMap<String, Object>();
		sysLPIInterfaceHash.put(headerKey, getSysLPIInterfaceData.getNames());
		sysLPIInterfaceHash.put(dataKey, removeSystemFromArrayList(getSysLPIInterfaceData.getList()));

		IndividualSystemTransitionReportWriter writer = new IndividualSystemTransitionReportWriter();
		writer.makeWorkbook(Utility.getInstanceName(systemURI.replace(">", "").replace("<", "")));
		writer.writeSystemInfoSheet("System Overview",sysInfoHash);
		writer.writeHWSWSheet("Software Lifecycles", storeSoftwareData.get(0), storeSoftwareData.get(1), storeSoftwareData.get(2));
		writer.writeHWSWSheet("Hardware Lifecycles", storeHardwareData.get(0), storeHardwareData.get(1), storeHardwareData.get(2));
		writer.writeListSheet("System Data", sysDataConsumeProvideHash);
		writer.writeListSheet("Data Provenance", sysSORDataWithDHMSMHash);
		writer.writeListSheet("DHMSM Data Requirements", sysSORDataWithDHMSMCapHash);
		writer.writeListSheet("System Interfaces", sysLPIInterfaceHash);
		boolean success = writer.writeWorkbook();
		if(success){
			Utility.showMessage("System Export Finished! File located in:\n" + IndividualSystemTransitionReportWriter.getFileLoc() );
		} else {
			Utility.showError("Error Creating Report!");
		}
	}

	public HashMap<String, Object> getQueryData(IEngine engine, String query){
		HashMap<String, Object> dataHash = new HashMap<String, Object>();
		
		SesameJenaSelectWrapper sjsw = processQuery(engine, query);
		String[] names = sjsw.getVariables();
		dataHash.put(headerKey, names);

		ArrayList<Object[]> dataToAddArr = new ArrayList<Object[]>();
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjsw.next();
			Object[] dataRow = new Object[names.length];
			for(int i = 0; i < names.length; i++)
			{
				Object dataElem = sjss.getVar(names[i]);
				if(dataElem.toString().startsWith("\"") && dataElem.toString().endsWith("\""))
				{
					dataElem = dataElem.toString().substring(1, dataElem.toString().length()-1); // remove annoying quotes
				}
				dataRow[i] = dataElem;
			}
			dataToAddArr.add(dataRow);
		}

		dataHash.put(dataKey, dataToAddArr);

		return dataHash;
	}
	
	public ArrayList<Object[]> removeSystemFromArrayList(ArrayList<Object[]> dataRow)
	{
		ArrayList<Object[]> retList = new ArrayList<Object[]>();
		for(int i=0;i<dataRow.size();i++)
		{
			Object[] row = new Object [dataRow.get(i).length-1];
			for(int j=0;j<row.length;j++)
				row[j] = dataRow.get(i)[j+1];
			retList.add(row);
		}
		return retList;
	}
	
	public String[] removeSystemFromStringArray(String[] names)
	{
		String[] retArray = new String[names.length-1];
		for(int j=0;j<retArray.length;j++)
			retArray[j] = names[j+1];
		return retArray;
	}

	public SesameJenaSelectWrapper processQuery(IEngine engine, String query){
		logger.info("PROCESSING QUERY: " + query);
		SesameJenaSelectWrapper sjsw = new SesameJenaSelectWrapper();
		//run the query against the engine provided
		sjsw.setEngine(engine);
		sjsw.setQuery(query);
		sjsw.executeQuery();	
		return sjsw;
	}

	@Override
	public void refineView() {
		// TODO Auto-generated method stub
	}

	@Override
	public void overlayView() {
		// TODO Auto-generated method stub
	}

	@Override
	public void runAnalytics() {
		// TODO Auto-generated method stub
	}

}
