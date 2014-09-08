package prerna.ui.components.specific.tap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.SesameJenaSelectStatement;
import prerna.rdf.engine.impl.SesameJenaSelectWrapper;
import prerna.util.DHMSMTransitionUtility;

public class LPInterfaceProcessor {

	// direct cost and indirect costs requires 
	private HashMap<String, HashMap<String, Double>> loeForSysGlItemHash = new HashMap<String, HashMap<String, Double>>();
	private HashMap<String, HashMap<String, Double>> loeForGenericGlItemHash = new HashMap<String, HashMap<String, Double>>();
	private HashMap<String, HashMap<String, Double>> avgLoeForSysGlItemHash = new HashMap<String, HashMap<String, Double>>();
	private HashMap<String, String> serviceToDataHash = new HashMap<String, String>();

	// lpni indirect cost also requires
	private HashSet<String> dhmsmSORList = new HashSet<String>();
	private HashSet<String> lpiSystemList = new HashSet<String>();
	
	private final double COST_PER_HOUR = 150.0;
	private double totalDirectCost = 0;
	private double totalIndirectCost = 0;
	
	private String query = "SELECT DISTINCT (IF(BOUND(?y),?DownstreamSys,IF(BOUND(?x),?UpstreamSys,'')) AS ?System) (IF(BOUND(?y),'Upstream',IF(BOUND(?x),'Downstream','')) AS ?InterfaceType) (IF(BOUND(?y),?UpstreamSys,IF(BOUND(?x),?DownstreamSys,'')) AS ?InterfacingSystem) (COALESCE(IF(BOUND(?y),IF((?UpstreamSysProb1 != 'High' && ?UpstreamSysProb1 != 'Question'),'Low','High'),IF(BOUND(?x),IF((?DownstreamSysProb1!='High' && ?DownstreamSysProb1!='Question'),'Low','High'),'')), '') AS ?Probability) ?Interface ?Data ?Format ?Freq ?Prot (IF((STRLEN(?DHMSMcrm)<1),'',IF((REGEX(STR(?DHMSMcrm),'C')),'Provides','Consumes')) AS ?DHMSM) ?Recommendation WHERE { {?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;} BIND('N' AS ?InterfaceYN) BIND('Y' AS ?ReceivedInformation) LET(?d := 'd') { {?UpstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> ?InterfaceYN;} {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?UpstreamSysProb;}OPTIONAL{{?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/HIE> ?HIEsys;}{?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?DownstreamSysProb1;}} OPTIONAL{ {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> ?InterfaceYN;}} {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Received_Information> ?ReceivedInformation;} {?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?Prob;}{?Interface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> ;} {?carries <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>;} {?Interface ?carries ?Data;} {?DownstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>;} {?Upstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>;}{?Downstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consume>;} {?UpstreamSys ?Upstream ?Interface ;}{?Interface ?Downstream ?DownstreamSys ;} { {?carries <http://semoss.org/ontologies/Relation/Contains/Format> ?Format ;}{?carries <http://semoss.org/ontologies/Relation/Contains/Frequency> ?Freq ;} {?carries <http://semoss.org/ontologies/Relation/Contains/Protocol> ?Prot ;} } LET(?x :=REPLACE(str(?d), 'd', 'x')) } UNION { {?DownstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> ?InterfaceYN;} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?DownstreamSysProb;}OPTIONAL{{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/HIE> ?HIEsys;}{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?UpstreamSysProb1;}} OPTIONAL{{?UpstreamSys <http://semoss.org/ontologies/Relation/Contains/Device_InterfaceYN> ?InterfaceYN;}} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Received_Information> ?ReceivedInformation;} {?DownstreamSys <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?Prob;} {?Interface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/InterfaceControlDocument> ;} {?carries <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Payload>;} {?Interface ?carries ?Data;} {?UpstreamSys <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem>;} {?Upstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Provide>;}{?Downstream <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consume>;} {?UpstreamSys ?Upstream ?Interface ;}{?Interface ?Downstream ?DownstreamSys ;} { {?carries <http://semoss.org/ontologies/Relation/Contains/Format> ?Format ;} {?carries <http://semoss.org/ontologies/Relation/Contains/Frequency> ?Freq ;}{?carries <http://semoss.org/ontologies/Relation/Contains/Protocol> ?Prot ;} } LET(?y :=REPLACE(str(?d), 'd', 'y')) } {SELECT DISTINCT ?Data (GROUP_CONCAT(DISTINCT ?Crm ; separator = ',') AS ?DHMSMcrm) WHERE {{?Data <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DataObject>;} OPTIONAL{BIND(<http://health.mil/ontologies/Concept/DHMSM/DHMSM> AS ?DHMSM ){?TaggedBy <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/TaggedBy>;}{?Capability <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Capability>;}{?DHMSM ?TaggedBy ?Capability.}{?Consists <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Consists>;}{?Task <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Task>;}{?Capability ?Consists ?Task.}{?Needs <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Needs>;}{?Needs <http://semoss.org/ontologies/Relation/Contains/CRM> ?Crm;}{?Task ?Needs ?Data.}} } GROUP BY ?Data} } ORDER BY ?System ?InterfacingSystem";	
	
	private final String SYS_KEY = "System";
	private final String INTERFACE_TYPE_KEY = "InterfaceType";
	private final String INTERFACING_SYS_KEY = "InterfacingSystem";
	private final String PROBABILITY_KEY = "Probability";
	private final String ICD_KEY = "Interface";
	private final String DATA_KEY = "Data";	
	private final String FORMAT_KEY = "Format";
	private final String FREQ_KEY = "Freq";
	private final String PROT_KEY = "Prot";
	private final String DHMSM = "DHMSM";
	private final String COMMENT_KEY = "Recommendation";

	private final String DOWNSTREAM_KEY = "Downstream";
	private final String DHMSM_PROVIDE_KEY = "Provide";
	private final String DHMSM_CONSUME_KEY = "Consumes";
	private final String LPI_KEY = "LPI";
	//	private final String lpniKey = "LPNI"; 
	private final String HPI_KEY = "HPI";
	private final String HPNI_KEY = "HPNI";

	private final String DHMSM_URI = "http://health.mil/ontologies/Concept/System/DHMSM";

	private final String provideInstanceRel = "http://health.mil/ontologies/Relation/Provide/";
	private final String consumeInstanceRel = "http://health.mil/ontologies/Relation/Consume/";
	private final String payloadInstanceRel = "http://health.mil/ontologies/Relation/Payload/";

	private final String semossPropURI = "http://semoss.org/ontologies/Relation/Contains/";
	private final String newProp = "TypeWeight";

	private IEngine engine;
	private boolean generateComments;
	private boolean generateNewTriples;
	private String[] names;
	private ArrayList<Object[]> list;

	// for future DB generation
	private ArrayList<Object[]> relList;
	private ArrayList<Object[]> relPropList;
	private ArrayList<String> addedInterfaces;
	private ArrayList<String> removedInterfaces;
	private Set<String> sysList;

	public double getTotalDirectCost() {
		return totalDirectCost;
	}

	public double getTotalIndirectCost() {
		return totalIndirectCost;
	}
	
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setEngine(IEngine engine){
		this.engine = engine;
	}

	public void setGenerateComments(boolean generateComments) {
		this.generateComments = generateComments;
	}

	public void setGenerateNewTriples(boolean generateNewTriples) {
		this.generateNewTriples = generateNewTriples;
	}

	public String[] getNames(){
		return names;
	}

	public ArrayList<Object[]> getList(){
		return list;
	}
	
	public ArrayList<Object[]> getRelList(){
		return relList;
	}
	
	public ArrayList<Object[]> getPropList(){
		return relPropList;
	}
	
	public ArrayList<String> getAddedInterfaces(){
		return addedInterfaces;
	}
	
	public ArrayList<String> getRemovedInterfaces(){
		return removedInterfaces;
	}
	
	public Set<String> getSysList(){
		return sysList;
	}
	
	public ArrayList<Object[]> generateReport() {
		list = new ArrayList<Object[]>();

		HashSet<String> sysDataSOR = DHMSMTransitionUtility.processSysDataSOR(engine);
		HashMap<String, String> sysTypeHash = DHMSMTransitionUtility.processReportTypeQuery(engine);

		//Process main query
		SesameJenaSelectWrapper wrapper = new SesameJenaSelectWrapper();
		wrapper.setQuery(query);
		wrapper.setEngine(engine);
		wrapper.executeQuery();
		// get the bindings from it
		names = wrapper.getVariables();

		list = processBusinessRules(wrapper, names, sysDataSOR, sysTypeHash);

		return list;
	}

	private ArrayList<Object[]> processBusinessRules(SesameJenaSelectWrapper sjw, String[] names, HashSet<String> sorV, HashMap<String, String> sysTypeHash){
		ArrayList<Object[]> retList = new ArrayList<Object[]>();
		relList = new ArrayList<Object[]>();
		relPropList = new ArrayList<Object[]>();
		addedInterfaces = new ArrayList<String>();
		removedInterfaces = new ArrayList<String>();
		sysList = new HashSet<String>();
		
		// becomes true if either user 
		boolean getComments = false;
		while(sjw.hasNext())
		{
			SesameJenaSelectStatement sjss = sjw.next();
			// get var's
			String sysName = "";
			String interfaceType = "";
			String interfacingSysName = "";
			String probability = "";
			String icd = "";
			String data = "";
			String format = "";
			String freq = "";
			String prot = "";
			String dhmsmSOR = "";

			if(sjss.getVar(SYS_KEY) != null) {
				sysName = sjss.getVar(SYS_KEY).toString();
			}
			if(sjss.getVar(INTERFACE_TYPE_KEY) != null) {
				interfaceType = sjss.getVar(INTERFACE_TYPE_KEY).toString();
			}
			if(sjss.getVar(INTERFACING_SYS_KEY) != null) {
				interfacingSysName = sjss.getVar(INTERFACING_SYS_KEY).toString();
			}
			if(sjss.getVar(PROBABILITY_KEY) != null) {
				probability = sjss.getVar(PROBABILITY_KEY).toString();
			}
			if(sjss.getVar(ICD_KEY) != null) {
				icd = sjss.getVar(ICD_KEY).toString();
			}
			if(sjss.getVar(DATA_KEY) != null) {
				data = sjss.getVar(DATA_KEY).toString();
			}
			if(sjss.getVar(FORMAT_KEY) != null) {
				format = sjss.getVar(FORMAT_KEY).toString();
			}
			if(sjss.getVar(FREQ_KEY) != null) {
				freq = sjss.getVar(FREQ_KEY).toString();
			}
			if(sjss.getVar(PROT_KEY) != null) {
				prot = sjss.getVar(PROT_KEY).toString();
			}
			if(sjss.getVar(DHMSM) != null) {
				dhmsmSOR = sjss.getVar(DHMSM).toString();
			}
			
			// get uri's
			String system = "";
			String interfaceTypeURI = "";
			String interfacingSystem = "";
			String probabilityURI = "";
			String icdURI = "";
			String dataURI = "";
			String formatURI = "";
			String freqURI = "";
			String protURI = "";
			String dhmsmSORURI = "";

			if(sjss.getRawVar(SYS_KEY) != null) {
				system = sjss.getRawVar(SYS_KEY).toString();
			}
			if(sjss.getVar(INTERFACE_TYPE_KEY) != null) {
				interfaceTypeURI = sjss.getVar(INTERFACE_TYPE_KEY).toString();
			}
			if(sjss.getRawVar(INTERFACING_SYS_KEY) != null) {
				interfacingSystem = sjss.getRawVar(INTERFACING_SYS_KEY).toString();
			}
			if(sjss.getRawVar(PROBABILITY_KEY) != null) {
				probabilityURI = sjss.getRawVar(PROBABILITY_KEY).toString();
			}
			if(sjss.getRawVar(ICD_KEY) != null) {
				icdURI = sjss.getRawVar(ICD_KEY).toString();
			}
			if(sjss.getRawVar(DATA_KEY) != null) {
				dataURI = sjss.getRawVar(DATA_KEY).toString();
			}
			if(sjss.getRawVar(FORMAT_KEY) != null) {
				formatURI = sjss.getRawVar(FORMAT_KEY).toString();
			}
			if(sjss.getRawVar(FREQ_KEY) != null) {
				freqURI = sjss.getRawVar(FREQ_KEY).toString();
			}
			if(sjss.getRawVar(PROT_KEY) != null) {
				protURI = sjss.getRawVar(PROT_KEY).toString();
			}
			if(sjss.getRawVar(DHMSM) != null) {
				dhmsmSORURI = sjss.getRawVar(DHMSM).toString();
			}

			String comment = "";
			Object[] values;
			if(generateComments || !generateNewTriples) {
				getComments = true;
				values = new Object[names.length];

				values[0] = sysName;
				values[1] = interfaceType;
				values[2] = interfacingSysName;
				values[3] = probability;
				values[4] = icd;
				values[5] = data;
				values[6] = format;
				values[7] = freq;
				values[8] = prot;
				values[9] = dhmsmSOR;
				values[10] = comment;
			} else {
				sysList.add(DHMSM_URI);
				values = new Object[3];
			}

			// determine which system is upstream or downstream
			String upstreamSysName = "";
			String upstreamSystemURI = "";
			String downstreamSysName = "";
			String downstreamSystemURI = "";
			if(interfaceType.contains(DOWNSTREAM_KEY)) { // lp system is providing data to interfacing system
				upstreamSystemURI = system;
				upstreamSysName = sysName;
				downstreamSystemURI = interfacingSystem;
				downstreamSysName = interfacingSysName;
			} else { // lp system is receiving data from interfacing system
				upstreamSystemURI = interfacingSystem;
				upstreamSysName = interfacingSysName;
				downstreamSystemURI = system;
				downstreamSysName = sysName;
			}
			
			String upstreamSysType = sysTypeHash.get(upstreamSysName);
			if(upstreamSysType == null) {
				upstreamSysType = "No Probability";
			}
			String downstreamSysType = sysTypeHash.get(downstreamSysName);
			if(downstreamSysType == null) {
				downstreamSysType = "No Probability";
			}
			String newICD = "";
			String payloadURI = "";

			// DHMSM is SOR of data
			if(dhmsmSOR.contains(DHMSM_PROVIDE_KEY)) {
				if(upstreamSysType.equals(LPI_KEY)) { // upstream system is LPI
					comment = comment.concat("Need to add interface DHMSM->").concat(upstreamSysName).concat(". ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMProviderOfICD(icdURI, upstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMProvider(newICD, upstreamSystemURI, upstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
					}
				} 
				// new business rule might be added - will either un-comment or remove after discussion today
				//						else if (upstreamSysType.equals(lpniKey)) { // upstream system is LPNI
				//							comment += "Recommend review of developing interface DHMSM->" + upstreamSysName + ". ";
				//						} 
				else if (downstreamSysType.equals(LPI_KEY)) { // upstream system is not LPI and downstream system is LPI
					comment = comment.concat("Need to add interface DHMSM->").concat(downstreamSysName).concat(".").concat(" Recommend review of removing interface ")
							.concat(upstreamSysName).concat("->").concat(downstreamSysName).concat(". ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMProviderOfICD(icdURI, downstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMProvider(newICD, downstreamSystemURI, downstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
						// add removed interface
						removedInterfaces.add(icdURI);
						String oldPayload = payloadInstanceRel.concat(icdURI.substring(icdURI.lastIndexOf("/")+1)).concat(":").concat(data);
						addTriples(icdURI, upstreamSystemURI, upstreamSysName, downstreamSystemURI, downstreamSysName, dataURI, data, oldPayload);
						addPropTriples(oldPayload, format, freq, prot, comment, (double) 0);
					}
				} 
				if (upstreamSysType.equals(HPI_KEY)) { // upstream is HPI
					comment = comment.concat("Provide temporary integration between DHMSM->").concat(upstreamSysName).concat(" until all deployment sites for ").concat(upstreamSysName)
							.concat(" field DHMSM (and any additional legal requirements). ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMProviderOfICD(icdURI, upstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMProvider(newICD, upstreamSystemURI, upstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
					}
				} else if(downstreamSysType.equals(HPI_KEY)) { // upstream sys is not HPI and downstream is HPI
					comment = comment.concat("Provide temporary integration between DHMSM->").concat(downstreamSysName).concat(" until all deployment sites for ").concat(downstreamSysName).concat(" field DHMSM (and any additional legal requirements).")
							.concat(" Recommend review of removing interface ").concat(upstreamSysName).concat("->").concat(downstreamSysName).concat(". ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMProviderOfICD(icdURI, downstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMProvider(newICD, downstreamSystemURI, downstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
						
						// add removed interface
						removedInterfaces.add(icdURI);
						String oldPayload = payloadInstanceRel.concat(icdURI.substring(icdURI.lastIndexOf("/")+1)).concat(":").concat(data);
						addTriples(icdURI, upstreamSystemURI, upstreamSysName, downstreamSystemURI, downstreamSysName, dataURI, data, oldPayload);
						addPropTriples(oldPayload, format, freq, prot, comment, (double) 0);
					}
				} 
				if(!upstreamSysType.equals(LPI_KEY) && !upstreamSysType.equals(HPI_KEY) && !downstreamSysType.equals(LPI_KEY) && !downstreamSysType.equals(HPI_KEY))
				{
					if(upstreamSysType.equals(HPI_KEY) || upstreamSysType.equals(HPNI_KEY) || downstreamSysType.equals(HPI_KEY) || downstreamSysType.equals(HPNI_KEY)) { //if either system is HP
						comment = "Stay as-is until all deployment sites for HP system field DHMSM (and any additional legal requirements)." ;
					} else {
						comment = "Stay as-is beyond FOC.";
					}
				}
			} else if(dhmsmSOR.contains(DHMSM_CONSUME_KEY)) {  // DHMSM is consumer of data
				boolean otherwise = true;
				if(upstreamSysType.equals(LPI_KEY) && sorV.contains(upstreamSystemURI + dataURI)) { // upstream system is LPI and SOR of data
					otherwise = false;
					comment = comment.concat("Need to add interface ").concat(upstreamSysName).concat("->DHMSM. ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMConsumerOfICD(icdURI, upstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMConsumer(newICD, upstreamSystemURI, upstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
					}
				} else if(sorV.contains(upstreamSystemURI + dataURI) && !probability.equals("null") && !probability.equals("") ) { // upstream system is SOR and has a probability
					otherwise = false;
					comment = comment.concat("Recommend review of developing interface between ").concat(upstreamSysName).concat("->DHMSM. ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMConsumerOfICD(icdURI, upstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMConsumer(newICD, upstreamSystemURI, upstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
					}
				} 
				if(downstreamSysType.equals(LPI_KEY) && sorV.contains(downstreamSystemURI + dataURI)) { // downstream system is LPI and SOR of data
					otherwise = false;
					comment = comment.concat("Need to add interface ").concat(downstreamSysName).concat("->DHMSM. ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMConsumerOfICD(icdURI, downstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMConsumer(newICD, downstreamSystemURI, downstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
					}
				} else if(sorV.contains(downstreamSystemURI + dataURI) && (!probability.equals("null") && !probability.equals("")) ) { // downstream system is SOR and has a probability
					otherwise = false;
					comment = comment.concat("Recommend review of developing interface between ").concat(downstreamSysName).concat("->DHMSM. ");
					if(generateNewTriples) {
						// add new interface
						newICD = makeDHMSMConsumerOfICD(icdURI, downstreamSysName, data);
						payloadURI = payloadInstanceRel.concat(newICD.substring(newICD.lastIndexOf("/")+1)).concat(":").concat(data);
						addedInterfaces.add(newICD);
						addTripleWithDHMSMConsumer(newICD, downstreamSystemURI, downstreamSysName, dataURI, data, payloadURI);
						addPropTriples(payloadURI, format, freq, prot, comment, (double) 5);
					}
				} 
				if(otherwise) {
					if(upstreamSysType.equals(HPI_KEY) || upstreamSysType.equals(HPNI_KEY) || downstreamSysType.equals(HPI_KEY) || downstreamSysType.equals(HPNI_KEY)) { //if either system is HP
						comment = "Stay as-is until all deployment sites for HP system field DHMSM (and any additional legal requirements)." ;
					} else {
						comment = "Stay as-is beyond FOC.";
					}
				}
			} else { // other cases DHMSM doesn't touch data object
				if(upstreamSysType.equals(HPI_KEY) || upstreamSysType.equals(HPNI_KEY) || downstreamSysType.equals(HPI_KEY) || downstreamSysType.equals(HPNI_KEY)) { //if either system is HP
					comment = "Stay as-is until all deployment sites for HP system field DHMSM (and any additional legal requirements)." ;
				} else {
					comment = "Stay as-is beyond FOC.";
				}
			}
			if(getComments) {
				values[10] = comment;
				retList.add(values);
			}
		}
		return retList;
	}

	private void addTripleWithDHMSMProvider(String icdURI, String downstreamSysURI, String downstreamSysName, String dataURI, String data, String payloadURI) {
		// change DHMSM to type System
		String upstreamSysURI = DHMSM_URI;
		String upstreamSysName = DHMSM;
		
		// dhmsm -> provide -> icd
		String provideURI = provideInstanceRel.concat(upstreamSysName).concat(":").concat(upstreamSysName).concat("-").concat(downstreamSysName).concat("-").concat(data);
		Object[] values = new Object[3];
		values[0] = upstreamSysURI;
		values[1] = provideURI;
		values[2] = icdURI;
		relList.add(values);
		
		// icd -> consume -> downstream
		String consumeURI = consumeInstanceRel.concat(upstreamSysName).concat("-").concat(downstreamSysName).concat("-").concat(data).concat(":").concat(downstreamSysName);
		values = new Object[3];
		values[0] = icdURI;
		values[1] = consumeURI;
		values[2] = downstreamSysURI;
		relList.add(values);
		
		// icd -> payload -> data 
		values = new Object[3];
		values[0] = icdURI;
		values[1] = payloadURI;
		values[2] = dataURI;
		relList.add(values);
		
		// dhmsm -> provide -> data
		values = new Object[3];
		values[0] = upstreamSysURI;
		values[1] = provideInstanceRel.concat(upstreamSysName).concat(":").concat(data);
		values[2] = dataURI;
		relList.add(values);
	}
	
	private void addTripleWithDHMSMConsumer(String icdURI, String upstreamSysURI, String upstreamSysName, String dataURI, String data, String payloadURI) {
		// change DHMSM to type System
		String downstreamSysURI = DHMSM_URI;
		String downstreamSysName = DHMSM;
		
		// upstream -> provide -> icd
		String provideURI = provideInstanceRel.concat(upstreamSysName).concat(":").concat(upstreamSysName).concat("-").concat(downstreamSysName).concat("-").concat(data);
		Object[] values = new Object[3];
		values[0] = upstreamSysURI;
		values[1] = provideURI;
		values[2] = icdURI;
		relList.add(values);
		
		// icd -> consume -> downstream
		String consumeURI = consumeInstanceRel.concat(upstreamSysName).concat("-").concat(downstreamSysName).concat("-").concat(data).concat(":").concat(downstreamSysName);
		values = new Object[3];
		values[0] = icdURI;
		values[1] = consumeURI;
		values[2] = downstreamSysURI;
		relList.add(values);
		
		// icd -> payload -> data 
		values = new Object[3];
		values[0] = icdURI;
		values[1] = payloadURI;
		values[2] = dataURI;
		relList.add(values);
	}
	
	private void addTriples(String icdURI, String upstreamSysURI, String upstreamSysName, String downstreamSysURI, String downstreamSysName, String dataURI, String data, String payloadURI) {
		sysList.add(upstreamSysURI);
		sysList.add(downstreamSysURI);
		
		// upstream -> provide -> icd
		String provideURI = provideInstanceRel.concat(upstreamSysName).concat(":").concat(upstreamSysName).concat("-").concat(downstreamSysName).concat("-").concat(data);
		Object[] values = new Object[3];
		values[0] = upstreamSysURI;
		values[1] = provideURI;
		values[2] = icdURI;
		relList.add(values);
		
		// icd -> consume -> downstream
		String consumeURI = consumeInstanceRel.concat(upstreamSysName).concat("-").concat(downstreamSysName).concat("-").concat(data).concat(":").concat(downstreamSysName);
		values = new Object[3];
		values[0] = icdURI;
		values[1] = consumeURI;
		values[2] = downstreamSysURI;
		relList.add(values);
		
		// icd -> payload -> data 
		values = new Object[3];
		values[0] = icdURI;
		values[1] = payloadURI;
		values[2] = dataURI;
		relList.add(values);
	}

	private void addPropTriples(String payloadURI, String format, String freq, String prot, String comment, double weight) {
		// payload -> contains -> prop
		Object[] values = new Object[]{payloadURI, semossPropURI.concat("Format"), format};
		relPropList.add(values);
		values = new Object[]{payloadURI, semossPropURI.concat("Frequency"), freq};
		relPropList.add(values);
		values = new Object[]{payloadURI, semossPropURI.concat("Protocol"), prot};
		relPropList.add(values);
		values = new Object[]{payloadURI, semossPropURI.concat("Recommendation"), comment};
		relPropList.add(values);
		values = new Object[]{payloadURI, semossPropURI.concat(newProp), weight};
		relPropList.add(values);
	}


	private String makeDHMSMConsumerOfICD(final String icd, final String sysProvider, final String dataObject) {
		String base = icd.substring(0, icd.lastIndexOf('/')+1);
		return base.concat(sysProvider).concat("-").concat(DHMSM).concat("-").concat(dataObject);
	}

	private String makeDHMSMProviderOfICD(final String icd, final String sysConsumer, final String dataObject) {
		String base = icd.substring(0, icd.lastIndexOf('/')+1);
		return base.concat(DHMSM).concat("-").concat(sysConsumer).concat("-").concat(dataObject);
	}
	
	
	public ArrayList<Object[]> createLPIInterfaceWithCostHash(String systemName, ArrayList<Object[]> oldData) 
	{
		ArrayList<Object[]> newData = new ArrayList<Object[]>();
		// clear the list of services already built for each system report
		HashSet<String> servicesProvideList = new HashSet<String>();
		totalDirectCost = 0;
		totalIndirectCost = 0;
		
		String interfaceType = "";
		String interfacingSystem = "";
		String dataObject = "";
		String dhmsmProvideOrConsume = "";
		
		// used to keep track of rows that have the same data object
		int rowIdx;
		ArrayList<Integer> indexArr = new ArrayList<Integer>();
		boolean deleteOtherInterfaces = false;
		boolean directCost = true;
		boolean skipFistIteration = false;
		for(rowIdx = 0; rowIdx < oldData.size(); rowIdx++)
		{
			Object[] row = oldData.get(rowIdx);
			Object[] newRow = new Object[oldData.get(0).length + 3];
			for(int i = 0; i < row.length; i++) {
				if(i == 0) {
					interfaceType = row[i].toString();
					newRow[i] = interfaceType;
				} else if(i == 1) {
					interfacingSystem = row[i].toString();
					newRow[i] = interfacingSystem;
				} else if(i == 4) {
					if(!dataObject.equals(row[i].toString()))
					{
						dataObject = row[i].toString();
						indexArr = new ArrayList<Integer>();
						deleteOtherInterfaces = false;
					}
					dataObject = row[4].toString();
					newRow[i] = dataObject;
				} else if(i == 8) {
					dhmsmProvideOrConsume = row[i].toString();
					newRow[i] = dhmsmProvideOrConsume;
				} else if (i == row.length - 1) {
					String comment = row[i].toString();
					
					String servicesList = serviceToDataHash.get(dataObject);
					if(servicesList == null) {
						servicesList = "No Services.";
					}
					newRow[i] = servicesList;
					String[] commentSplit = null;
					if(comment.contains("Stay as-is"))
					{
						newRow[i+1] = "";
						newRow[i+2] = "";
						newRow[i+3] = "";
					} else {
						commentSplit = comment.split("\\.");
						commentSplit = commentSplit[0].split("->");
						Double finalCost = null;
						if(dhmsmProvideOrConsume.equals("Consumes") && interfaceType.equals("Downstream")) { // dhmsm consumes and our system is SOR -> direct cost
							directCost = true;
							finalCost = calculateCost(dataObject, systemName, "Provider", true, servicesProvideList);
						} else if(dhmsmProvideOrConsume.equals("Provides") && !deleteOtherInterfaces) {
							if(commentSplit[1].contains(systemName)) { // dhmsm provides and our system consumes -> direct cost
								directCost = true;
								finalCost = calculateCost(dataObject, systemName, "Consume", false, servicesProvideList);
								deleteOtherInterfaces = true;
								skipFistIteration = true;
								for(Integer index : indexArr)
								{
									if(index < newData.size() && newData.get(index) != null) // case when first row is the LPI system and hasn't been added to newData yet
									{
										Object[] modifyCost = newData.get(index);
										modifyCost[i+1] = "Interface already taken into consideration.";
										modifyCost[i+2] = "";
										modifyCost[i+3] = "";
									}
								}
							} else { // dhmsm provides and other system consumes -> indirect cost
								finalCost = calculateCost(dataObject, interfacingSystem, "Consume", false, servicesProvideList);
								directCost = false;
							}
						}
	
						if(finalCost == null) {
							if(deleteOtherInterfaces && !skipFistIteration) {
								newRow[i+1] = "Interface already taken into consideration.";
								skipFistIteration = false;
							} else {
								newRow[i+1] = "Cost already taken into consideration.";
							}
							newRow[i+2] = "";
							newRow[i+3] = "";
						} else if(finalCost != (double) 0){
							if(directCost) {
								newRow[i+1] = comment;
								newRow[i+2] = finalCost;
								newRow[i+3] = "";
								totalDirectCost += finalCost;
							} else {
								newRow[i+1] = comment;
								newRow[i+2] = "";
								newRow[i+3] = finalCost;
								totalIndirectCost += finalCost;
							}
						} else {
							newRow[i+1] = "No data present to calculate loe.";
							newRow[i+2] = "";
							newRow[i+3] = "";
						}
					}
				} else {
					newRow[i] = row[i];
				}
			}
			newData.add(newRow);
		}
		
		return newData;
	}

	public ArrayList<Object[]> createLPNIInterfaceWithCostHash(String systemName, ArrayList<Object[]> oldData) 
	{
		ArrayList<Object[]> newData = new ArrayList<Object[]>();
		// clear the list of services already built for each system report
		HashSet<String> servicesProvideList = new HashSet<String>();
		totalDirectCost = 0;
		totalIndirectCost = 0;
		String dataObject = "";
		String interfacingSystem = "";
		String interfaceType = "";
		
		boolean directCost = true;
		
		for(Object[] row : oldData)
		{
			Object[] newRow = new Object[oldData.get(0).length + 3];
			for(int i = 0; i < row.length; i++)
			{
				if(i == 0) {
					interfaceType = row[i].toString();
					newRow[i] = interfaceType;
				} if(i == 1) {
					interfacingSystem= row[i].toString(); 
					newRow[i] = interfacingSystem;
				} else if(i == 4) {
					dataObject = row[i].toString(); 
					newRow[i] = dataObject;
				} else if(i == row.length - 1) {
					String comment = row[i].toString();
					
					String servicesList = serviceToDataHash.get(dataObject);
					if(servicesList == null) {
						servicesList = "No Services";
					}
					newRow[i] = servicesList;
					newRow[i+1] = comment;

					String[] commentSplit = comment.split("\\.");
					commentSplit = commentSplit[0].split("->");
					Double finalCost = null;
					// DHMSM is receiving information from LPNI which is a SOR of the data object
					if( commentSplit[0].contains(systemName) && commentSplit[1].contains("DHMSM") )
					{
						finalCost = calculateCost(dataObject, systemName, "Provide", true, servicesProvideList);
						directCost = true;
					} 
					else if( lpiSystemList.contains(interfacingSystem) && dhmsmSORList.contains(dataObject) && interfaceType.equals("Upstream"))
					{
						finalCost = calculateCost(dataObject, interfacingSystem, "Consume", false, servicesProvideList);
						directCost = false;
					} 
						
					if(finalCost == null) {
						newRow[i+2] = "";
						newRow[i+3] = "";
					} else if(finalCost != (double) 0){
						if(directCost) {
							newRow[i+1] = comment;
							newRow[i+2] = finalCost;
							newRow[i+3] = "";
							totalDirectCost += finalCost;
						} else {
							newRow[i+1] = comment;
							newRow[i+2] = "";
							newRow[i+3] = finalCost;
							totalIndirectCost += finalCost;
						}
					} else {
						newRow[i+1] = "No data present to calculate loe.";
						newRow[i+2] = "";
						newRow[i+3] = "";
					}
				} else {
					newRow[i] = row[i];
				}
			}
			newData.add(newRow);
		}

		return newData;
	}
	
	private Double calculateCost(String dataObject, String system, String tag, boolean includeGenericCost, HashSet<String> servicesProvideList)
	{
		double sysGLItemCost = 0;
		double genericCost = 0;

		ArrayList<String> sysGLItemServices = new ArrayList<String>();
		// get sysGlItem for provider lpi systems
		HashMap<String, Double> sysGLItem = loeForSysGlItemHash.get(dataObject);
		HashMap<String, Double> avgSysGLItem = avgLoeForSysGlItemHash.get(dataObject);

		boolean useAverage = true;
		boolean servicesAllUsed = false;
		if(sysGLItem != null)
		{
			for(String sysSerGLTag : sysGLItem.keySet())
			{
				String[] sysSerGLTagArr = sysSerGLTag.split("\\+\\+\\+");
				if(sysSerGLTagArr[0].equals(system))
				{
					if(sysSerGLTagArr[2].contains(tag))
					{
						useAverage = false;
						String ser = sysSerGLTagArr[1];
						if(!servicesProvideList.contains(ser)) {
							sysGLItemServices.add(ser);
							servicesProvideList.add(ser);
							sysGLItemCost += sysGLItem.get(sysSerGLTag);
						} else {
							servicesAllUsed = true;
						}
					} // else do nothing - do not care about consume loe
				}
			}
		}
		// else get the average system cost
		if(useAverage)
		{
			if(avgSysGLItem != null)
			{
				for(String serGLTag : avgSysGLItem.keySet())
				{
					String[] serGLTagArr = serGLTag.split("\\+\\+\\+");
					if(serGLTagArr[1].contains(tag))
					{
						String ser = serGLTagArr[0];
						if(!servicesProvideList.contains(ser)) {
							sysGLItemServices.add(ser);
							servicesProvideList.add(ser);
							sysGLItemCost += avgSysGLItem.get(serGLTag);
						} else {
							servicesAllUsed = true;
						}
					}
				}
			}
		}

		if(includeGenericCost)
		{
			HashMap<String, Double> genericGLItem = loeForGenericGlItemHash.get(dataObject);
			if(genericGLItem != null)
			{
				for(String ser : genericGLItem.keySet())
				{
					if(sysGLItemServices.contains(ser)) {
						genericCost += genericGLItem.get(ser);
					} 
				}
			}
		}

		Double finalCost = null;
		if(!servicesAllUsed) {
			finalCost = (double) (Math.round(sysGLItemCost + genericCost) * COST_PER_HOUR);
		}

		return finalCost;
	}
	
	public void getCostInfo(final IEngine TAP_Cost_Data){
		// get data for all systems
		if(loeForGenericGlItemHash.isEmpty()) {
			loeForGenericGlItemHash = DHMSMTransitionUtility.getGenericGLItem(TAP_Cost_Data);
		}
		if(avgLoeForSysGlItemHash.isEmpty()) {
			avgLoeForSysGlItemHash = DHMSMTransitionUtility.getAvgSysGLItem(TAP_Cost_Data);
		} 
		if(serviceToDataHash.isEmpty()) {
			serviceToDataHash = DHMSMTransitionUtility.getServiceToData(TAP_Cost_Data);
		}
		if(loeForSysGlItemHash.isEmpty()) {
			loeForSysGlItemHash = DHMSMTransitionUtility.getSysGLItem(TAP_Cost_Data);
		}
	}
	
	public void getLPNIInfo(final IEngine HR_Core) {
		if(dhmsmSORList.isEmpty()) {
			dhmsmSORList = DHMSMTransitionUtility.runVarListQuery(HR_Core, DHMSMTransitionUtility.DHMSM_SOR_QUERY);
		}
		if(lpiSystemList.isEmpty()) {
			lpiSystemList = DHMSMTransitionUtility.runVarListQuery(HR_Core, DHMSMTransitionUtility.LPI_SYS_QUERY);
		}
	}
	
}
