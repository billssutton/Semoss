package prerna.sablecc2.reactor;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import prerna.sablecc2.om.CodeBlock;

public class PKSLPlanner {
	
	// this is primarily the tinker graph that would be used for planning the operation
	TinkerGraph g = TinkerGraph.open();
	
	// I need some way to have roots
	Vector roots = new Vector();
	
	public static final String NOUN = "NOUN";
	public static final String OPERATION = "OP";
	public static final String TINKER_ID = "_T_ID";
	public static final String TINKER_TYPE = "_T_TYPE";
	public static final String TINKER_NAME = "_T_NAME";
	
	
	
	// adds an operation with necessary inputs
	public void addInputs(String opName, Vector <String> inputs, IReactor.TYPE opType)
	{
		// find the vertex for each of the input
		// wonder if this should be a full fledged block of java ?
		Vertex opVertex = upsertVertex(OPERATION, opName);
		opVertex.property("OP_TYPE", opType);
		opVertex.property("CODE", "NONE");
		for(int inputIndex = 0;inputIndex < inputs.size();inputIndex++)
		{
			Vertex inpVertex = upsertVertex(NOUN, inputs.elementAt(inputIndex));
			String edgeID = inputs.elementAt(inputIndex)+ "_" + opName;
			Edge retEdge = inpVertex.addEdge(edgeID, opVertex, "ID", edgeID, "COUNT", 1, "INEDGE", inpVertex.property("ID"), "TYPE", "INPUT");
			
		}
	}
	
	public void addProperty(String opName, String propertyName, Object value)
	{
		Vertex opVertex = findVertex(OPERATION, opName);
		if(opVertex != null)
		{
			opVertex.property(propertyName, value);
		}
	}
	
	
	// adds an operation with outputs
	public void addOutputs(String opName, Vector <String> outputs, IReactor.TYPE opType)
	{
		// find the vertex for each of the input
		// wonder if this should be a full fledged block of java ?
		Vertex opVertex = upsertVertex(OPERATION, opName);
		opVertex.property("OP_TYPE", opType);
		opVertex.property("CODE", "NONE");
		for(int outputIndex = 0;outputIndex < outputs.size();outputIndex++)
		{
			Vertex outpVertex = upsertVertex(NOUN, outputs.elementAt(outputIndex));
			String edgeID = opName + "_" + outputs.elementAt(outputIndex);
			Edge retEdge = opVertex.addEdge(edgeID, outpVertex, "ID", edgeID, "COUNT", 1, "INEDGE", opVertex.property("ID"), "TYPE", "OUTPUT");
		}
	}
	
	// adds a java operation string as input
	public void addInputs(String opName, CodeBlock codeBlock, Vector <String> inputs, IReactor.TYPE opType)
	{
		// should this be string ?
		// it should be a map block
		// or it should be a reduce block
		// at a minimum it should say what type of block it is
		// 
		Vertex opVertex = upsertVertex(OPERATION, opName);
		opVertex.property("OP_TYPE", opType);
		opVertex.property("CODE", codeBlock);
		for(int inputIndex = 0;inputIndex < inputs.size();inputIndex++)
		{
			Vertex inpVertex = upsertVertex(NOUN, inputs.elementAt(inputIndex));
			String edgeID = inputs.elementAt(inputIndex)+ "_" + opName;
			Edge retEdge = inpVertex.addEdge(edgeID, opVertex, "ID", edgeID, "COUNT", 1, "INEDGE", inpVertex.property("ID"), "TYPE", "INPUT");			
		}
	}
	
	protected Vertex upsertVertex(String type, String name)
	{
		Vertex retVertex = null;
		// try to find the vertex
		//			GraphTraversal<Vertex, Vertex> gt = g.traversal().V().has(TINKER_TYPE, type).has(TINKER_ID, type + ":" + data);
		retVertex = findVertex(type, name);
		if(retVertex == null)
				retVertex = g.addVertex(TINKER_ID, type + ":" + name, TINKER_TYPE, type, TINKER_NAME, name);// push the actual value as well who knows when you would need it
		return retVertex; 
	}
	
	private Vertex findVertex(String type, String name)
	{
		Vertex retVertex = null;
		GraphTraversal<Vertex, Vertex> gt = g.traversal().V().has(TINKER_ID, type + ":" + name);
		if(gt.hasNext())
			retVertex = gt.next();
		return retVertex;
	}
	
	
	// this runs the plan for the final output
	public Iterator runMyPlan(String output)
	{
		// I need to take all of the stuff from gremlin and assort it into
		// blocks
		// Blocks can be progressively executed
		// I need to stack the blocks
		// everytime I find the input to be a block, I need to stack it up i.e. only the code operation
		// Output <- codeBlock <- codeblock etc. 
		// Everytime I see a reduce operation I need to chop it off
		// And So on
		
		// while output input edges is all not null
		// I need to always assimilate all the inputs
		// and plan my operation
		// I need a block keeper 
		
		// when there is a reduce which multiple thing depends on, I need to validatee so that it doesn't do it twice
		// Just as a stage has various operations and their sequence
		// I also need to keep something with all stages and its sequence
		// but at the same time ensure that it is not overwriting anything
		
		
		
		// I need to test
		// Single Stage - done
		// Multi Stage with reduce - done
		// Multi stage with reduce 
		
		Vector <Object> inputs = new Vector<Object>();
		Vertex workingVertex = findVertex(OPERATION, output);
		if(workingVertex != null)
		{
			Stack <Hashtable<String, Object>> codeStack = new Stack<Hashtable<String, Object>>();
			
			// INPUT inputVector, CODE codeVector, OUTPUT outputVector
			Hashtable <String, Object> curHash = new Hashtable<String, Object>();
			Vector <Vertex> verticesToProcess = new Vector<Vertex>();
			verticesToProcess.add(workingVertex);
			curHash.put("VERTEX_TO_PROCESS", verticesToProcess);
			codeStack.push(curHash);
			
			String code = null;
			//code = loadVertex(codeStack);
			
			//Stack <Stage> newStack = new Stack<Stage>();
			//code = loadVertex2(newStack, verticesToProcess);
			
			
			code = loadVertex3(new StageKeeper(), verticesToProcess);
			// execute the code and return the iterator
		}		
		return null;
	}
	
	private String loadVertex2(Stack <Stage> codeStack, Vector <Vertex> verticesToProcess)
	{
		
		// I need to break this into stages i.e. the codeStack
		Stage thisStage = null;
		if(codeStack.size() > 0)
			thisStage = codeStack.pop();
		else
		{
			thisStage = new Stage();
			thisStage.stageNum = 0;
		}
		Vertex thisVertex = verticesToProcess.remove(0);
		System.out.println("Processing Vertex >>>>>>>>>>>>>>>>>>>> " + thisVertex.property(TINKER_NAME));

		String operationName = thisVertex.property(TINKER_NAME).value() + "";
		
		Hashtable opHash = thisStage.addOperation(operationName);
		// add inputs to these and in the end synchronize
		// this might get the as so something to be aware of for now
		Iterator <Vertex> inputs = thisVertex.vertices(Direction.IN);

		Vector <String> opInputs = (Vector<String>)opHash.get(Stage.INPUTS);
		Vector <String> deInputs = (Vector<String>)opHash.get(Stage.DERIVED_INPUTS);
		Vector <String> depends = (Vector<String>)opHash.get(Stage.DEPENDS);
		String curCode = (String)opHash.get(Stage.CODE);
		// we will refine this code piece later

		// run through each input 
		// and 
		// the input to these are almost.. wait always nouns
		// I need a nounstore to get this assigned
		// this way I can keep the filters and everything else
		// should I just load the reactor ?
		// that way I can get what I want ?
		// the problem then is I have to worry about filters
		// let the filters be for now.. let us run through other things
		boolean close = false;

		// assimilate the inputs in a single place
		IReactor.TYPE opType = (IReactor.TYPE)thisVertex.property("OP_TYPE").value();
		
		// add the code
		Object codeBlock = thisVertex.property("CODE").value();
		
		// write the code if you see a piece of code
		if(codeBlock instanceof CodeBlock)
		{
			// I need to print this code
			String code = ((CodeBlock)codeBlock).getCode();
			curCode = curCode + "\n" + code; // building from the bottom
			opHash.put(Stage.CODE, curCode);
		}

		// find if this is a reduce operation
		while(inputs.hasNext())
		{
			// I need to account for when the input is an operation
			// vs. when the input is not an operation
			
			// collect the operations that we need to process next
			// as well as columns it is looking for
			// get this vertex
			// not accounting for parallel run yet, I will get to it
			Vertex thisNoun = inputs.next();
			String nounName = thisNoun.property(TINKER_NAME).value() + "";
			
			System.out.println("Adding noun.. " + nounName);
			
			// add if this column is not there already
			// I should do this at a later point because I dont know when it will be a reduce..
			//if(allInputCols.indexOf(nounName) < 0)
			//	allInputCols.add(thisNoun.property(TINKER_NAME).value() + "");
			
			// get the input for this to find if this is a reduce
			// this will be a an operation here
			// what is producing this noun
			Iterator <Vertex> opVertices = thisNoun.vertices(Direction.IN);
			
			Vertex thisOp = null;
			if(opVertices.hasNext()) // ok there is an operation
			{
				thisOp = opVertices.next();
				
				// also need to add the dependency
				
				// add this for the next run
				// take it out and add it to the end so it comes towards the end
				System.out.println("\t Vertex" + thisOp.property(TINKER_NAME).value());
				if(verticesToProcess.indexOf(thisOp) >= 0)
					verticesToProcess.remove(thisOp);
				verticesToProcess.add(thisOp);		
				depends.addElement(thisOp.property(TINKER_NAME).value() + "");
				// I need to find if this is reduce and if do find what do I do again ?
				// some how I need to move to next code block
				// no idea what this means hehe.. 
				// need to close this block after this is done
				codeBlock = thisOp.property("CODE").value();
				if(codeBlock instanceof String)
					opInputs.add(nounName);
				else // this is a derived call 
					deInputs.add(nounName);
			}
			else // this is the base input with no parent operation
				opInputs.add(nounName);
		}
		

		
		// I need to add the decorator on top
		// so that this will only be those fields that this particular routine needs
		// so I could be retrieving [a,b,c,d,e] and this routine may only require
		// [a,d,e] so I need to seggregate before passing it on
		// this is all sit on the input Cols
		opHash.put(Stage.INPUTS, opInputs);
		opHash.put(Stage.DERIVED_INPUTS, deInputs);
		opHash.put(Stage.CODE, curCode);	
		opHash.put(Stage.DEPENDS, depends);
		
		if(opType == IReactor.TYPE.REDUCE) // this is a reduce operation
		{
			// couple of things I need to do
			// I need to add the query to the top of it
			// which means I need sum total of all columns
			// need to take all the input columns and generate the query for it
			// this is where I convert this into a query and then let it rip from here on
			// close out the first piece
			
			// remove this operation
			// add a new stage
			// add this operation to the new stage
			// recurse
			thisStage.removeOperation(operationName);
			Stage newStage = new Stage();
			codeStack.push(thisStage);
			int newNum = thisStage.stageNum + 1;
			newStage.stageNum = newNum;
			newStage.addOperation(operationName, opHash);			
			//codeStack.push(newStage);
			thisStage = newStage;
		}
		codeStack.push(thisStage);
		thisStage.synchronizeInput(operationName);
		
		if(verticesToProcess.size() > 0)
		{
			return loadVertex2(codeStack, verticesToProcess);
		}
		
		// walk through the code stack poping one by one
		// and asking it to getcode
		for(int stageIndex = codeStack.size()-1;stageIndex >= 0;stageIndex--)
		{
			System.out.println("====================================");
			Stage stage = codeStack.elementAt(stageIndex);
			System.out.println(" STAGE " + (codeStack.size() - stageIndex) + " \n\n" + stage.getCode());
			System.out.println("====================================");
		}
		
		System.out.println("Final CODE..\n\n\n\n");
		System.out.println(curCode);	
		return curCode;
	}


	private String loadVertex3(StageKeeper keeper, Vector <Vertex> verticesToProcess)
	{
		
		// I need to break this into stages i.e. the codeStack
		Stage thisStage = null;
		if(keeper.lastStage != null)
			thisStage = keeper.lastStage;
		else
		{
			thisStage = new Stage();
			thisStage.stageNum = 0;
		}
		Vertex thisVertex = verticesToProcess.remove(0);
		System.out.println("Processing Vertex >>>>>>>>>>>>>>>>>>>> " + thisVertex.property(TINKER_NAME));

		String operationName = thisVertex.property(TINKER_NAME).value() + "";
		keeper.addStage(operationName, thisStage);
		
		Hashtable opHash = thisStage.addOperation(operationName);
		// add inputs to these and in the end synchronize
		// this might get the as so something to be aware of for now
		Iterator <Vertex> inputs = thisVertex.vertices(Direction.IN);

		Vector <String> opInputs = (Vector<String>)opHash.get(Stage.INPUTS);
		Vector <String> deInputs = (Vector<String>)opHash.get(Stage.DERIVED_INPUTS);
		Vector <String> depends = (Vector<String>)opHash.get(Stage.DEPENDS);
		String curCode = (String)opHash.get(Stage.CODE);
		// we will refine this code piece later

		// run through each input 
		// and 
		// the input to these are almost.. wait always nouns
		// I need a nounstore to get this assigned
		// this way I can keep the filters and everything else
		// should I just load the reactor ?
		// that way I can get what I want ?
		// the problem then is I have to worry about filters
		// let the filters be for now.. let us run through other things
		boolean close = false;

		// assimilate the inputs in a single place
		IReactor.TYPE opType = (IReactor.TYPE)thisVertex.property("OP_TYPE").value();
		
		// add the code
		Object codeBlock = thisVertex.property("CODE").value();
		
		// write the code if you see a piece of code
		if(codeBlock instanceof CodeBlock)
		{
			// I need to print this code
			String code = ((CodeBlock)codeBlock).getCode();
			curCode = curCode + "\n" + code; // building from the bottom
			opHash.put(Stage.CODE, curCode);
		}

		// find if this is a reduce operation
		while(inputs.hasNext())
		{
			// I need to account for when the input is an operation
			// vs. when the input is not an operation
			
			// collect the operations that we need to process next
			// as well as columns it is looking for
			// get this vertex
			// not accounting for parallel run yet, I will get to it
			Vertex thisNoun = inputs.next();
			String nounName = thisNoun.property(TINKER_NAME).value() + "";
			
			System.out.println("Adding noun.. " + nounName);
			
			// add if this column is not there already
			// I should do this at a later point because I dont know when it will be a reduce..
			//if(allInputCols.indexOf(nounName) < 0)
			//	allInputCols.add(thisNoun.property(TINKER_NAME).value() + "");
			
			// get the input for this to find if this is a reduce
			// this will be a an operation here
			// what is producing this noun
			Iterator <Vertex> opVertices = thisNoun.vertices(Direction.IN);
			
			Vertex thisOp = null;
			if(opVertices.hasNext()) // ok there is an operation
			{
				thisOp = opVertices.next();
				
				// also need to add the dependency
				
				// add this for the next run
				// take it out and add it to the end so it comes towards the end
				System.out.println("\t Vertex" + thisOp.property(TINKER_NAME).value());
				if(verticesToProcess.indexOf(thisOp) >= 0)
					verticesToProcess.remove(thisOp);
				verticesToProcess.add(thisOp);		
				depends.addElement(thisOp.property(TINKER_NAME).value() + "");
				// I need to find if this is reduce and if do find what do I do again ?
				// some how I need to move to next code block
				// no idea what this means hehe.. 
				// need to close this block after this is done
				codeBlock = thisOp.property("CODE").value();
				if(codeBlock instanceof String)
					opInputs.add(nounName);
				else // this is a derived call 
					deInputs.add(nounName);
			}
			else // this is the base input with no parent operation
				opInputs.add(nounName);
		}
		

		
		// I need to add the decorator on top
		// so that this will only be those fields that this particular routine needs
		// so I could be retrieving [a,b,c,d,e] and this routine may only require
		// [a,d,e] so I need to seggregate before passing it on
		// this is all sit on the input Cols
		opHash.put(Stage.INPUTS, opInputs);
		opHash.put(Stage.DERIVED_INPUTS, deInputs);
		opHash.put(Stage.CODE, curCode);	
		opHash.put(Stage.DEPENDS, depends);
		
		if(opType == IReactor.TYPE.REDUCE) // this is a reduce operation
		{
			// couple of things I need to do
			// I need to add the query to the top of it
			// which means I need sum total of all columns
			// need to take all the input columns and generate the query for it
			// this is where I convert this into a query and then let it rip from here on
			// close out the first piece
			
			// remove this operation
			// add a new stage
			// add this operation to the new stage
			// recurse
			thisStage.removeOperation(operationName);
			Stage newStage = new Stage();
			int newNum = thisStage.stageNum + 1;
			newStage.stageNum = newNum;
			newStage.addOperation(operationName, opHash);			
			keeper.addStage(operationName, newStage);
			//codeStack.push(newStage);
			thisStage = newStage;
		}
		//codeStack.push(thisStage);
		thisStage.synchronizeInput(operationName);
		
		if(verticesToProcess.size() > 0)
		{
			return loadVertex3(keeper, verticesToProcess);
		}

		keeper.adjustStages();
		keeper.printCode();
		// walk through the code stack poping one by one
		// and asking it to getcode
		
		System.out.println("Final CODE..\n\n\n\n");
		System.out.println(curCode);	
		return curCode;
	}

	
	
	
	// I need to keep track of
	// the code so far
	// inputs so far for this code block so I can have one query
	// that is it
	// I need a master code structure
	// which says what stage this particular processing
	// each stage should have an operation
	// query that defines this stage
	// and code block for every stage - this could quite simply be just a string
	// stage is composed each of the operation and the respective code
	// whenever there is an existing piece of code it should take the order and reinsert it to top i.e. vector
	// I also need 
	private String loadVertex(Stack <Hashtable<String, Object>> codeStack)
	{
		
		// I need to break this into stages i.e. the codeStack
		Hashtable <String, Object> curHash = codeStack.pop();
		Vector <Vertex> verticesToProcess = (Vector<Vertex>)curHash.get("VERTEX_TO_PROCESS");
		Vertex thisVertex = verticesToProcess.remove(0);
		System.out.println("Processing Vertex >>>>>>>>>>>>>>>>>>>> " + thisVertex.property(TINKER_NAME));
		// this might get the as so something to be aware of for now
		Iterator <Vertex> inputs = thisVertex.vertices(Direction.IN);
		Vector <String> allInputCols = new Vector<String>();

		String curCode = "}";
		// we will refine this code piece later
		if(curHash.containsKey("CODE"))
			curCode = (String)curHash.get("CODE");

		if(curHash.containsKey("ALL_INPUTS"))
			allInputCols = (Vector<String>)curHash.get("ALL_INPUTS");

		// run through each input 
		// and 
		// the input to these are almost.. wait always nouns
		// I need a nounstore to get this assigned
		// this way I can keep the filters and everything else
		// should I just load the reactor ?
		// that way I can get what I want ?
		// the problem then is I have to worry about filters
		// let the filters be for now.. let us run through other things
		boolean close = false;
		// assimilate the inputs in a single place
		
		Vector <String> inputCols = new Vector <String>();
		Vector <String> derivedCols = new Vector<String>();
		
		IReactor.TYPE opType = (IReactor.TYPE)thisVertex.property("OP_TYPE").value();
		
		
		// add the code
		Object codeBlock = thisVertex.property("CODE").value();
		
		// write the code if you see a piece of code
		if(codeBlock instanceof CodeBlock)
		{
			// I need to print this code
			String code = ((CodeBlock)codeBlock).getCode();
			curCode = code + curCode; // building from the bottom
		}

		String nounSoFar = "";
		
		// find if this is a reduce operation
		while(inputs.hasNext())
		{
			// I need to account for when the input is an operation
			// vs. when the input is not an operation
			
			// collect the operations that we need to process next
			// as well as columns it is looking for
			// get this vertex
			// not accounting for parallel run yet, I will get to it
			Vertex thisNoun = inputs.next();
			String nounName = thisNoun.property(TINKER_NAME).value() + ""; 
			System.out.println("Adding noun.. " + nounName);
			nounSoFar = nounSoFar + "\t" + nounName;
			
			// add if this column is not there already
			// I should do this at a later point because I dont know when it will be a reduce..
			//if(allInputCols.indexOf(nounName) < 0)
			//	allInputCols.add(thisNoun.property(TINKER_NAME).value() + "");
			
			// get the input for this to find if this is a reduce
			// this will be a an operation here
			// what is producing this noun
			Iterator <Vertex> opVertices = thisNoun.vertices(Direction.IN);
			
			Vertex thisOp = null;
			if(opVertices.hasNext()) // ok there is an operation
			{
				thisOp = opVertices.next();
				// add this for the next run
				// take it out and add it to the end so it comes towards the end
				System.out.println("\t Vertex" + thisOp.property(TINKER_NAME).value());
				if(verticesToProcess.indexOf(thisOp) >= 0)
					verticesToProcess.remove(thisOp);
				verticesToProcess.add(thisOp);					
				// I need to find if this is reduce and if do find what do I do again ?
				// some how I need to move to next code block
				// no idea what this means hehe.. 
				// need to close this block after this is done
				codeBlock = thisOp.property("CODE").value();
				if(codeBlock instanceof String)
					inputCols.add(nounName);
				else // this is a derived call 
					derivedCols.add(nounName);
			}
			else // this is the base input with no parent operation
				inputCols.add(nounName);
				
		}
		
		// I need to add the decorator on top
		// so that this will only be those fields that this particular routine needs
		// so I could be retrieving [a,b,c,d,e] and this routine may only require
		// [a,d,e] so I need to seggregate before passing it on
		// this is all sit on the input Cols
		if(opType != IReactor.TYPE.REDUCE)
		{
			// add the inputcols to the all input cols
			for(int inputIndex = 0;inputIndex <inputCols.size();inputIndex++)
				if(allInputCols.indexOf(inputCols.elementAt(inputIndex)) < 0 ) allInputCols.add(inputCols.elementAt(inputIndex));
			
			curHash.put("ALL_INPUTS", allInputCols);

			// add the curVerticesToProcess to all vertices.
			// You are good to go proceed.
			String columnFilter = "getColumns(inputCols);\n"; // this is pseudo code right now
			String mapOp = "OPERATION NAME : " + thisVertex.property(TINKER_NAME).value() + " \n COLUMN SUBSET..  " + nounSoFar + "\n";
			curCode = columnFilter + mapOp + curCode;
		}
		else // this is a reduce operation
		{
			// couple of things I need to do
			// I need to add the query to the top of it
			// which means I need sum total of all columns
			// need to take all the input columns and generate the query for it
			// this is where I convert this into a query and then let it rip from here on
			// close out the first piece
			String query = "String query = composeQuery(allInputCols);\n";
			String columns = "QUERY COLUMNS NEEDED : ";
			for(int inputIndex = 0;inputIndex < allInputCols.size();inputIndex++)
				columns = columns + " \t " + allInputCols.elementAt(inputIndex);
			String execute = "execute this query ";
			String foreach = "while(result.hasNext()) {";
			String mapLogic = "// here comes map logic :)";
			curCode = query + execute + mapLogic + foreach + curCode;
			
			// need to put this portion of the code too
			// but this is reduce
			// doesn't matter
			String columnFilter = "getColumns(inputCols);\n"; // this is pseudo code right now
			curCode = columnFilter + curCode;
			
			query = "String query = composeQuery(inputCols);";
			columns = "QUERY COLUMN NEEDED : " + nounSoFar + "\n";
			execute = "execute this query";
			String reduceLogic = "// here comes the reduce logic :) " + thisVertex.property(TINKER_NAME).value() ;
			foreach = "while(result.hasNext()) {";
			curCode = query + columns + execute + reduceLogic + foreach + "}" + curCode;

			// create a new Hash for the next cycle
			curHash = new Hashtable <String, Object>();				
		}
		if(verticesToProcess.size() > 0)
		{
			curHash.put("VERTEX_TO_PROCESS", verticesToProcess);
			curHash.put("CODE", curCode);
			curHash.put("ALL_INPUTS", allInputCols);
			codeStack.push(curHash);
			return loadVertex(codeStack);
		}
		else
		{
			// close out
			String query = "String query = composeQuery(allInputCols);\n";
			String columns = "QUERY COLUMNS NEEDED FINAL: ";
			for(int inputIndex = 0;inputIndex < allInputCols.size();inputIndex++)
				columns = columns + " \t " + allInputCols.elementAt(inputIndex);
			String execute = "\n execute this query \n";
			String foreach = "while(result.hasNext()) { \n";
			String mapLogic = "// here comes map logic :) " + thisVertex.property(TINKER_NAME).value() + "\n";
			curCode = query + columns + execute + mapLogic + foreach + curCode;			
		}
		System.out.println("Final CODE..\n\n\n\n");
		System.out.println(curCode);	
		return curCode;
	}
}
