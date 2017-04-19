package prerna.sablecc2.reactor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;

import prerna.algorithm.api.ITableDataFrame;
import prerna.engine.api.IHeadersDataRow;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.NounStore;
import prerna.sablecc2.om.PkslDataTypes;

public abstract class AbstractReactor implements IReactor {

	private static final Logger LOGGER = LogManager.getLogger(AbstractReactor.class.getName());
	
	protected String operationName = null;
	protected String signature = null;
	protected String inputString = null;
	
	protected String curNoun = null;
	protected IReactor parentReactor = null;
	protected Vector<IReactor> childReactor = new Vector<IReactor>();
	protected NounStore store = null;
	protected IReactor.TYPE type = IReactor.TYPE.FLATMAP;
	protected IReactor.STATUS status = null;
	protected GenRowStruct curRow = null;
	
	protected String reactorName = "";
	protected String [] asName = null;
	protected Vector <String> outputFields = null;
	protected Vector <String> outputTypes = null;
	
	protected Hashtable <String, Object> propStore = new Hashtable<String, Object>();
	protected ITableDataFrame frame = null;
	
	public PKSLPlanner planner = null;
	protected Lambda runner = null;
	
	protected String[] defaultOutputAlias;
	boolean evaluate = false;
	
	@Override
	public void setPKSL(String operation, String fullOperation, String inputString) {
		this.operationName = operation;
		this.signature = fullOperation;
		this.inputString = inputString;
	}
	
	@Override
	public void setFrame(ITableDataFrame frame)
	{
		this.frame = frame;
	}

	@Override
	public String[] getPKSL() {
		String [] output = new String[3];
		output[0] = operationName;
		output[1] = signature;
		output[2] = inputString;
		return output;
	}
	
	@Override
	public void setParentReactor(IReactor parentReactor) {
		this.parentReactor = parentReactor;
	}
	
	@Override
	public IReactor getParentReactor() {
		return this.parentReactor;
	}
	
	@Override
	public void setName(String reactorName) {
		this.reactorName = reactorName;
	}
	
	@Override
	public String getName() {
		return this.reactorName;
	}
	
	@Override
	public void setProp(String key, Object value) {
		propStore.put(key, value);
		
	}

	@Override
	public Object getProp(String key) {
		return propStore.get(key);
	}
	
	@Override
	public boolean hasProp(String key) {
		return propStore.containsKey(key);
	}
	
	@Override
	public void setPKSLPlanner(PKSLPlanner planner) {
		this.planner = planner;
	}
	
	@Override
	public void getErrorMessage() {

	}

	@Override
	public STATUS getStatus() {
		return this.status;
	}
	
	@Override
	public void setChildReactor(IReactor childReactor) {
		this.childReactor.add(childReactor);
	}
	
	@Override
	public void curNoun(String noun) { 
		this.curNoun = noun;
		curRow = makeNoun(noun);
	}
	
	// gets the current noun
	@Override
	public GenRowStruct getCurRow() {
		return curRow;
	}
	

	@Override
	public void closeNoun(String noun) {
		curRow = store.getNoun("all");
	}

	@Override
	public NounStore getNounStore() {
		// I need to see if I have a child
		// if the child
		if(this.store == null)
			store = new NounStore(operationName);
		return store;
	}

	@Override
	public TYPE getType() {
		Object typeProp = getProp("type");
		if(typeProp != null && ((String)typeProp).contains("reduce"))
			this.type = TYPE.REDUCE;
		return this.type;
	}

	@Override
	public void setAs(String [] asName) {
		this.asName = asName;
		// need to set this up on planner as well
		//if(outputFields == null)
			outputFields = new Vector();
		outputFields.addAll(Arrays.asList(asName));
		// re add this to output
		planner.addOutputs(signature, outputFields, type);
	}
	
	
	// get noun
	GenRowStruct makeNoun(String noun) {
		GenRowStruct newRow = null;
		getNounStore();
		newRow = store.makeNoun(noun);
		return newRow;
	}
	
	// execute it
	public NounMetadata execute()
	{
		List <NounMetadata> lamList = curRow.getNounsOfType(PkslDataTypes.LAMBDA);
		// replace all the values that is inside this. this could be a recursive call
		for(int lamIndex = 0;lamIndex < lamList.size();lamIndex++)
		{
			NounMetadata thisLambdaMeta = lamList.get(lamIndex);
			IReactor thisReactor = (IReactor)thisLambdaMeta.getValue();
			String rSignature = thisReactor.getSignature();
			NounMetadata result = thisReactor.execute();// this might further trigger other things

			// for compilation reasons
			// if we have a double
			// we dont want it to print with the exponential
			Object replaceValue = result.getValue();
			PkslDataTypes replaceType = result.getNounName();
			if(replaceType == PkslDataTypes.CONST_DECIMAL) {
				// big decimal is easiest way i have seen to do this formatting
				replaceValue = new BigDecimal((double) replaceValue).toPlainString();
			} else {
				replaceValue = replaceValue + "";
			}
			modifySignature(rSignature, replaceValue.toString());
		}
		return null;
	}
	
	@Override
	public void modifySignature(String stringToFind, String stringReplacement) {
		LOGGER.debug("Original signature value = " + this.signature);
		this.signature = StringUtils.replaceOnce( this.signature, stringToFind, stringReplacement);
		LOGGER.debug("New signature value = " + this.signature);
	}

	// call for map
	public IHeadersDataRow map(IHeadersDataRow row)
	{
		return null;
	}
	
	// call for reeduce
	public Object reduce(Iterator it)
	{
		return null;
	}
	
	@Override
	public void mergeUp() {
		// need to figure out the best way to do this
		// i dont want to always just push everything
		// ... not sure if that is avoidable
		
		
		// but i know for filter statements
		// children definitely need to be pushed up
		// based on the curnoun the filter has set
		// we definitely need to define based on the type
		
		// TODO: how do i accurately determine the data type?
		if(this.parentReactor instanceof FilterReactor || this.parentReactor instanceof AssignmentReactor ) {
			NounMetadata data = new NounMetadata(this, PkslDataTypes.LAMBDA);
			this.parentReactor.getCurRow().add(data);
		}
	}
	
	@Override
	public List<NounMetadata> getInputs() {
		List<NounMetadata> inputs = new Vector<NounMetadata>();
		// grab all the nouns in the noun store
		Set<String> nounKeys = this.getNounStore().nounRow.keySet();
		for(String nounKey : nounKeys) {
			// grab the genrowstruct for the noun
			// and add its vector to the inputs list
			GenRowStruct struct = this.getNounStore().getNoun(nounKey);
			inputs.addAll(struct.vector);
		}
		
		// we also need to account for some special cases
		// when we have a filter reactor
		// it doesn't get added as an op
		// so we need to go through the child of this reactor
		// and if it has a filter
		// add its nouns to the inputs for this reactor
		for(IReactor child : childReactor) {
			List<NounMetadata> childInputs = child.getInputs();
			if(childInputs != null) {
				inputs.addAll(childInputs);
			}
		}

		return inputs;
	}
	
	@Override
	public List<NounMetadata> getOutputs() {
		//Default operation for the abstract is to return the asName aliases as the outputs
		if(this.asName != null) {
			List<NounMetadata> outputs = new Vector<>();
			for(String alias : this.asName) {
				NounMetadata aliasNoun = new NounMetadata(alias, PkslDataTypes.ALIAS);
				outputs.add(aliasNoun);
			}
			return outputs;
		}
		return null;
	}
	
	@Override
	public void updatePlan() {
		// at this point, the inner child is still just part of a 
		// larger parent
		// we only need to update the plan when the full pksl
		// is loaded
		if(this.parentReactor != null) {
			return;
		}
		// get the inputs and outputs
		// and add this to the plan using the signature
		// of the reactor
		List<NounMetadata> inputs = this.getInputs();
		List<NounMetadata> outputs = this.getOutputs();

		if(inputs != null) {
			List<String> strInputs = new Vector<String>();
			for(int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
				strInputs.add(inputs.get(inputIndex).getValue() + "");
			}
			
			this.planner.addInputs(this.signature, strInputs, TYPE.FLATMAP);
		}
		
		if(outputs != null) {
			List<String> strOutputs = new Vector<String>();
			for(int outputIndex = 0; outputIndex < outputs.size(); outputIndex++) {
				strOutputs.add(outputs.get(outputIndex).getValue() + "");
			}
			
			this.planner.addOutputs(this.signature, strOutputs, TYPE.FLATMAP);
		}
		
		// if no input or outputs
		// no need to add this to the plan
		if(inputs != null || outputs != null) {
			// store the reactor itself onto the planner
			this.planner.addProperty(this.signature, PKSLPlanner.REACTOR_CLASS, this);
		}
	}
	
	@Override
	public String getSignature()
	{
		return signature;
	}
}