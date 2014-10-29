package prerna.ui.components.specific.tap;

import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.error.EngineException;
import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.BigDataEngine;
import prerna.util.DHMSMTransitionUtility;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class InsertInterfaceModernizationProperty {

	static final Logger LOGGER = LogManager.getLogger(InsertInterfaceModernizationProperty.class.getName());

	private final String sysURIPrefix = "http://health.mil/ontologies/Concept/System/";
	private final String costPropertyURI = "http://semoss.org/ontologies/Relation/Contains/InterfaceModernizationCost";

	private IEngine HR_Core;

	public void insert() throws EngineException
	{
		try{
			HR_Core = (IEngine) DIHelper.getInstance().getLocalProp("HR_Core");
			if(HR_Core==null)
				throw new EngineException("Database not found");
		} catch(EngineException e) {
			Utility.showError("Could not find necessary database: HR_Core. Cannot generate report.");
			return;
		}
		getCostFromInterfaceReport();
	}

	private void getCostFromInterfaceReport() throws EngineException 
	{
		HashMap<String,String> reportTypeHash = DHMSMTransitionUtility.processReportTypeQuery(HR_Core);
		LPInterfaceProcessor processor = new LPInterfaceProcessor();

		IEngine TAP_Cost_Data = (IEngine) DIHelper.getInstance().getLocalProp("TAP_Cost_Data");
		if(TAP_Cost_Data == null) {
			throw new EngineException("TAP_Cost_Data Database not found");
		}
		
		processor.setEngine(HR_Core);
		processor.getCostInfo(TAP_Cost_Data);
		for(String sysName : reportTypeHash.keySet()){
			sysName = sysName.replaceAll("\\(", "\\\\\\\\\\(").replaceAll("\\)", "\\\\\\\\\\)");
			processor.setQuery(DHMSMTransitionUtility.lpSystemInterfacesQuery.replace("@SYSTEMNAME@", sysName));
			processor.isGenerateCost(true);
			processor.generateReport();
			
			Object cost = (Double) processor.getTotalDirectCost();
			if(cost == null) {
				cost = "NA";
			}
			addProperty(sysURIPrefix.concat(sysName), costPropertyURI, cost, false);
		}
	}

	private void addProperty(String sub, String pred, Object obj, boolean concept_triple) 
	{
		( (BigDataEngine) HR_Core).addStatement(sub, pred, obj, concept_triple);
		( (BigDataEngine) HR_Core).commit();
		System.out.println(sub + " >>> " + pred + " >>> " + obj);
	}
}
