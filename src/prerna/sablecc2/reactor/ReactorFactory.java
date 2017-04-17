package prerna.sablecc2.reactor;

import java.util.HashMap;
import java.util.Map;

import prerna.algorithm.api.ITableDataFrame;
import prerna.sablecc2.reactor.export.CollectReactor;
import prerna.sablecc2.reactor.export.job.AddFormatReactor;
import prerna.sablecc2.reactor.export.job.AddOptionsReactor;
import prerna.sablecc2.reactor.export.job.ExportReactor;
import prerna.sablecc2.reactor.export.job.JobReactor;
import prerna.sablecc2.reactor.export.job.SetFormatReactor;
import prerna.sablecc2.reactor.export.job.SetOptionsReactor;
import prerna.sablecc2.reactor.expression.OpMax;
import prerna.sablecc2.reactor.expression.OpMean;
import prerna.sablecc2.reactor.expression.OpMin;
import prerna.sablecc2.reactor.expression.OpSum;
import prerna.sablecc2.reactor.imports.ImportDataReactor;
import prerna.sablecc2.reactor.imports.MergeDataReactor;
import prerna.sablecc2.reactor.qs.AverageReactor;
import prerna.sablecc2.reactor.qs.DatabaseReactor;
import prerna.sablecc2.reactor.qs.FrameReactor;
import prerna.sablecc2.reactor.qs.GroupByReactor;
import prerna.sablecc2.reactor.qs.JoinReactor;
import prerna.sablecc2.reactor.qs.LimitReactor;
import prerna.sablecc2.reactor.qs.OffsetReactor;
import prerna.sablecc2.reactor.qs.QueryFilterReactor;
import prerna.sablecc2.reactor.qs.QueryReactor;
import prerna.sablecc2.reactor.qs.SelectReactor;
import prerna.sablecc2.reactor.qs.SumReactor;
import prerna.sablecc2.reactor.storage.LoadClient;
//import prerna.util.Utility;
import prerna.sablecc2.reactor.storage.MapStore;
import prerna.sablecc2.reactor.storage.RetrieveValue;
import prerna.sablecc2.reactor.storage.StoreValue;
import prerna.sablecc2.reactor.storage.UpdateValues;

public class ReactorFactory {

	// This holds the reactors that are frame agnostic and can be used by pixel
	private static Map<String, Class> reactorHash;
	
	// This holds the reactors that are expressions
	// example Sum, Max, Min
	// the reactors will handle how to execute
	// if it can be run via the frame (i.e. sql/gremlin) or needs to run external
	private static Map<String, Class> expressionHash;
	
	
	//This holds the list of expressions we have that can be used as operational formulas and reducers
	//Ex: Sum(), Max(), Min(), etc. 
//	private static Set<String> expressions;
	
	static {
		reactorHash = new HashMap<>();
		createReactorHash(reactorHash);
		
		expressionHash = new HashMap<>();
		populateExpressionSet(expressionHash);
	}
	
	private static void populateExpressionSet(Map<String, Class> expressionHash) {
		//Mathematical operations
		expressionHash.put("SUM", OpSum.class);
		expressionHash.put("AVERAGE", OpMean.class);
		expressionHash.put("AVG", OpMean.class);
		expressionHash.put("MEAN", OpMean.class);
		expressionHash.put("MIN", OpMin.class);
		expressionHash.put("MAX	", OpMax.class);
	}
	
	//populates the frame agnostic reactors used by pixel
	private static void createReactorHash(Map<String, Class> reactorHash) {
		//Import Reactors
		reactorHash.put("Import", ImportDataReactor.class); //takes in a query struct and imports data to a new frame
		reactorHash.put("Merge", MergeDataReactor.class); //takes in a query struct and merges data to an existing frame
		
		//Query Struct Reactors
		reactorHash.put("Select", SelectReactor.class); //builds the select portion of the QS
		reactorHash.put("Average", AverageReactor.class);
		reactorHash.put("Sum", SumReactor.class);
		
		reactorHash.put("Group", GroupByReactor.class);
		reactorHash.put("Limit", LimitReactor.class);
		reactorHash.put("Offset", OffsetReactor.class);
		reactorHash.put("Join", JoinReactor.class);
		reactorHash.put("Filter", QueryFilterReactor.class);
		reactorHash.put("Query", QueryReactor.class);
		
		//Data Source Reactors
		reactorHash.put("Database", DatabaseReactor.class); //specifies that our pksl operations after this point are dealing with the specified database
		reactorHash.put("Datasource", DatabaseReactor.class); //specifies that our pksl operations after this point are dealing with the specified database
		reactorHash.put("Frame", FrameReactor.class); //specifes that our pksl operations after this point are dealing with the specified frame
		reactorHash.put("CreateFrame", CreateFrame.class);
		
		//Reducers
		reactorHash.put("Iterate", IterateReactor.class); //this takes in a query struct and produces an iterator
		
		//Exporting Reactors
		reactorHash.put("Job", JobReactor.class); //defines the job
		reactorHash.put("Export", ExportReactor.class); //export
		reactorHash.put("Collect", CollectReactor.class); //collect
		reactorHash.put("Format", SetFormatReactor.class); //set formats
		reactorHash.put("SetFormat", SetFormatReactor.class); //add options
		reactorHash.put("AddFormat", AddFormatReactor.class); //add formats
		reactorHash.put("Options", SetOptionsReactor.class); //set options
		reactorHash.put("AddOptions", AddOptionsReactor.class); //add options
		reactorHash.put("SetOptions", SetOptionsReactor.class); //add options
		
		//If is in its own category
		reactorHash.put("if", IfReactor.class);
		
		// in mem storage of data
		reactorHash.put("MapStore", MapStore.class);
		reactorHash.put("StoreValue", StoreValue.class);
		reactorHash.put("RetrieveValue", RetrieveValue.class);
		reactorHash.put("LoadClient", LoadClient.class);
		reactorHash.put("RunPlan", RunPlannerReactor.class);
		reactorHash.put("UpdateValues", UpdateValues.class);
	}
	
	/**
	 * 
	 * @param reactorId - reactor name
	 * @param nodeString - pixel
	 * @param frame - frame we will be operating on
	 * @param parentReactor - the parent reactor
	 * @return
	 * 
	 * This will simply return the IReactor responsible for execution based on the reactorId
	 * 
	 * Special case:
	 * 		if we are dealing with an expression, we determine if this expression is part of a select query or should be reduced
	 * 		If it is a reducing expression we
	 * 			1. create an expr reactor
	 * 			2. grab the reducing expression reactor from the frame
	 * 			3. set that reactor to the expr reactor and return the expr reactor
	 * 		The expr reactor when executed will use that reducing expression reactor to evaluate
	 */
    public static IReactor getReactor(String reactorId, String nodeString, String inputString, ITableDataFrame frame, IReactor parentReactor) {
    	IReactor reactor;
		try {
			//is this an expression?
			//we need to determine if we are treating this expression as a reducer or as a selector
			if(expressionHash.containsKey(reactorId.toUpperCase())) {
				
				//if this expression is not a selector
				if(!(parentReactor instanceof SelectReactor)) {
					reactor = (IReactor)expressionHash.get(reactorId.toUpperCase()).newInstance();
					reactor.setPKSL(reactorId, nodeString, inputString);
					return reactor;
					
//					//this expression is a reducer not a selector
//					//so first we create an ExprReactor
//					reactor = new ExprReactor();
//					reactor.setPKSL(reactorId, nodeString, inputString);
//			        reactor.setName("OPERATION_FORMULA");
//			        
//			        //grab the expression reducer from the frame
//			        Map<String, String> scriptReactors = new H2Frame().getScriptReactors();
//			        String reactorName = scriptReactors.get(reactorId.toUpperCase());
//			        
//			        //set the name of that reducer in the reactor so it can reduce properly
//			        reactor.setProp("REACTOR_NAME", reactorName);
//			        return reactor;
				}
			}
			// if not an expression
			// search in the normal reactor hash
			if(reactorHash.containsKey(reactorId)) {
				reactor = (IReactor)reactorHash.get(reactorId).newInstance();
				reactor.setPKSL(reactorId, nodeString, inputString);
				return reactor;
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
    	
		reactor = new SampleReactor();
		reactor.setPKSL(reactorId, nodeString, inputString);
		return reactor;
    }
}
