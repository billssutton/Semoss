package prerna.sablecc2.reactor;

import prerna.sablecc2.om.Filter;
import prerna.sablecc2.om.GenRowStruct;

public class FilterReactor extends AbstractReactor{

	// keeps the noun store
	// sets the value for comparator 
	// and for left col
	// accumulates the right column and then sets it on join
	
	@Override
	public void In()
	{
        curNoun("all");
	}
	
	@Override
	public Object Out()
	{
		// get the property called LCOL
		// get the property called comparator
		// get the nounstore to see the columns added
		// set it up in parent
		// I want to get all the columns from here
		// and then add it to the parent as a filter
		
		GenRowStruct lcol = store.getNoun("LCOL");
		GenRowStruct comparator = store.getNoun("COMPARATOR");
		GenRowStruct rcol = store.getNoun("RCOL");
		
		Filter thisFilter = new Filter(lcol, comparator.get(0).toString(), rcol);
		
		GenRowStruct thisStruct = store.makeNoun("f");
		thisStruct.addFilter(thisFilter);

		// just add this to the parent
		parentReactor.getNounStore().addNoun("f", thisStruct);
		//mergeUp();
		return parentReactor;
	}
	
	@Override
	protected void mergeUp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updatePlan() {
		// TODO Auto-generated method stub
		
	}
	
}
