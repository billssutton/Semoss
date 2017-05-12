package prerna.sablecc2.reactor.planner.graph;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.engine.api.IHeadersDataRow;
import prerna.engine.api.IRawSelectWrapper;
import prerna.sablecc2.PkslUtility;
import prerna.sablecc2.PlannerTranslation;
import prerna.sablecc2.reactor.PKSLPlanner;
import prerna.sablecc2.reactor.planner.AbstractLoadClient;

public class LoadGraphClient extends AbstractLoadClient {

	private static final Logger LOGGER = LogManager.getLogger(LoadGraphClient.class.getName());

	private int total = 0;
	private int error = 0;
	

	protected PKSLPlanner createPlanner() {
		long start = System.currentTimeMillis();

		// generate our lazy translation
		// which only ingests the routines
		// without executing
		PlannerTranslation plannerT = new PlannerTranslation();
		
		// get the iterator we are loading
		IRawSelectWrapper iterator = (IRawSelectWrapper) getIterator();
		String[] headers = iterator.getDisplayVariables();
		
		int[] assignmentIndices = getAssignmentIndices(headers);
		int valIndex = getValueIndex(headers);
		int typeIndex = getTypeIndex(headers);
		String separator = getSeparator();

		while(iterator.hasNext()) {
			IHeadersDataRow nextData = iterator.next();
			Object[] values = nextData.getValues();
			
			//grab the assignment variable, or the alias
			String assignment = getAssignment(values, assignmentIndices, separator);
			
			//grab the value we are assigning to that variable/alias
			String value = getValue(values, valIndex);
			
			//if the value is a formula add to the pksl planner
			if(isFormula(values, typeIndex)) {
				String pkslString = generatePKSLString(assignment, value);
				PkslUtility.addPkslToTranslation(plannerT, pkslString);
			} 
			//else we just want to add the value of the constant/decimal directly to the planner
			else {
				addVariable(plannerT.planner, assignment, value);
			}
		}
		
		// grab the planner from the new translation
		LOGGER.info("****************    "+total+"      *************************");
		LOGGER.info("****************    "+error+"      *************************");

		long end = System.currentTimeMillis();
		System.out.println("****************    END LOAD CLIENT "+(end - start)+"ms      *************************");

		return plannerT.planner;
	}
}
