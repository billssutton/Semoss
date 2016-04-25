package prerna.sablecc;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

import prerna.algorithm.api.IAction;
import prerna.algorithm.api.ITableDataFrame;
import prerna.algorithm.impl.ImportAction;
import prerna.ds.TinkerFrame;
import prerna.engine.api.IScriptReactor;
import prerna.sablecc.PKQLEnum.PKQLReactor;
import prerna.sablecc.PKQLEnum.PKQLToken;
import prerna.sablecc.analysis.DepthFirstAdapter;
import prerna.sablecc.node.AAddColumn;
import prerna.sablecc.node.AApiBlock;
import prerna.sablecc.node.AColCsv;
import prerna.sablecc.node.AColDef;
import prerna.sablecc.node.AColWhere;
import prerna.sablecc.node.ACsvRow;
import prerna.sablecc.node.ADecimal;
import prerna.sablecc.node.ADivExpr;
import prerna.sablecc.node.AEExprExpr;
import prerna.sablecc.node.AExprGroup;
import prerna.sablecc.node.AExprRow;
import prerna.sablecc.node.AExprScript;
import prerna.sablecc.node.AFilterColumn;
import prerna.sablecc.node.AHelpScript;
import prerna.sablecc.node.AImportColumn;
import prerna.sablecc.node.AImportData;
import prerna.sablecc.node.AMathFun;
import prerna.sablecc.node.AMathFunTerm;
import prerna.sablecc.node.AMinusExpr;
import prerna.sablecc.node.AModExpr;
import prerna.sablecc.node.AMultExpr;
import prerna.sablecc.node.ANumWordOrNum;
import prerna.sablecc.node.ANumberTerm;
import prerna.sablecc.node.APlusExpr;
import prerna.sablecc.node.AROp;
import prerna.sablecc.node.ARelationDef;
import prerna.sablecc.node.ASetColumn;
import prerna.sablecc.node.ATermExpr;
import prerna.sablecc.node.AUnfilterColumn;
import prerna.sablecc.node.AVarop;
import prerna.sablecc.node.AVizChange;
import prerna.sablecc.node.AVizopScript;
import prerna.sablecc.node.AWord;

public class Translation2 extends DepthFirstAdapter {
	// this is the third version of this shit I am building
	// I need some way of having logical points for me to know when to start another reactor
	// for instance I could have an expr term within a math function which itself could be within another expr term
	// the question then is do I have 2 expr terms etc. 
	// so I start the first expr term
	// I start assimilating
	// get to a point where I start a new one
	// my vector tells me that
	// the init and deinit should take care of it I bet ?
	// how do I know when I am done ?
	// it has to be invoked at the last level
	Hashtable <String, Object> reactorHash = null;
	IScriptReactor curReactor = null;
	
	// reactor vector
	Vector <Hashtable<String, Object>> reactorHashHistory = new Vector <Hashtable<String, Object>>(); 
	Vector <Hashtable <String, Object>> reactorStack = new Vector<Hashtable <String, Object>>();
	
	// set of reactors
	// which serves 2 purposes 
	// a. Where to initiate
	// b. What is the name of the reactor
	
	Hashtable <String, String> reactorNames = new Hashtable<String, String>();
	ITableDataFrame frame = null;
	PKQLRunner runner = null;
	
	public Translation2() { // Test Constructor
		frame = new TinkerFrame();
		((TinkerFrame)frame).tryCustomGraph();
		this.runner = new PKQLRunner();
		fillReactors();
	}
	/**
	 * Constructor that takes in the dataframe that it will perform its calculations off of and the runner that invoked the translation
	 * @param frame IDataMaker: either TinkerFrame or TinkerH2Frame
	 * @param runner PKQLRunner: holds response from PKQL script and the status of whether the script errored or not
	 */
	public Translation2(ITableDataFrame frame, PKQLRunner runner) {
		// now get the data from tinker
		this.frame = frame;
		this.runner = runner;
		fillReactors();
	}
	
	private void fillReactors() { // TODO: use PKQLReactor enum
		reactorNames.put(PKQLEnum.EXPR_TERM, "prerna.sablecc.ExprReactor");
		reactorNames.put(PKQLEnum.EXPR_SCRIPT, "prerna.sablecc.ExprReactor");
		reactorNames.put(PKQLEnum.MATH_FUN, "prerna.sablecc.MathReactor");
		reactorNames.put(PKQLEnum.COL_CSV, "prerna.sablecc.ColCsvReactor"); // it almost feels like I need a way to tell when to do this and when not but let me see
		reactorNames.put(PKQLEnum.ROW_CSV, "prerna.sablecc.RowCsvReactor");
		reactorNames.put(PKQLEnum.API, "prerna.sablecc.ApiReactor");
		reactorNames.put(PKQLEnum.WHERE, "prerna.sablecc.ColWhereReactor");
		reactorNames.put(PKQLEnum.REL_DEF, "prerna.sablecc.RelReactor");
		reactorNames.put(PKQLEnum.COL_ADD, "prerna.sablecc.ColAddReactor");
		reactorNames.put(PKQLEnum.IMPORT_DATA, "prerna.sablecc.ImportDataReactor");
		reactorNames.put(PKQLEnum.FILTER_DATA, "prerna.sablecc.ColFilterReactor");
		reactorNames.put(PKQLReactor.R_OP.toString(), "prerna.sablecc.RReactor");
		reactorNames.put(PKQLEnum.VIZ, "prerna.sablecc.VizReactor");
		reactorNames.put(PKQLEnum.UNFILTER_DATA, "prerna.sablecc.ColUnfilterReactor");
	}
	
	public void initReactor(String myName) {
		String parentName = null;
		if(reactorHash != null)
			// I am not sure I need to add element here
			// I need 2 things in here
			// I need the name of a parent i.e. what is my name and my parent name
			// actually I just need my name
			parentName = (String)reactorHash.get("SELF");
		reactorHash = new Hashtable<String, Object>();
		if(parentName != null)
			reactorHash.put("PARENT_NAME", parentName);
		reactorHash.put("SELF", myName);
		reactorStack.addElement(reactorHash);
		
		// I should also possibly initialize the reactor here
		try {
			String reactorName = reactorNames.get(myName);
			curReactor = (IScriptReactor)Class.forName(reactorName).newInstance();
			curReactor.put(PKQLEnum.G, frame);
			// this is how I can get access to the parent when that happens
			reactorHash.put(myName, curReactor);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}
	
	public Hashtable <String, Object> deinitReactor(String myName, String input, String output, boolean put) {
		Hashtable <String, Object> thisReactorHash = reactorStack.lastElement();
		reactorStack.remove(thisReactorHash);
		IScriptReactor thisReactor = (IScriptReactor)thisReactorHash.get(myName);
		// this is still one level up
		thisReactor.process();
		Object value = 	thisReactor.getValue(input);		
		System.out.println("Value is .. " + value);		

		if(reactorStack.size() > 0) {
			reactorHash = reactorStack.lastElement();
			// also set the cur reactor
			String parent = (String)thisReactorHash.get("PARENT_NAME");
			
			// if the parent is not null
			if(parent != null && reactorHash.containsKey(parent)) {
				// I need to make some decisions here
				curReactor = (IScriptReactor)reactorHash.get(parent);
				if(put)
					curReactor.put(output, value);
				else
					curReactor.set(output, value);
			}
		}
		return thisReactorHash;
	}		
	
	public Hashtable <String, Object> deinitReactor(String myName, String input, String output) {
		return deinitReactor(myName, input, output, true);
	}
	
	private void synchronizeValues(String input, String[] values2Sync, IScriptReactor thisReactor) {
		for(int valIndex = 0;valIndex < values2Sync.length;valIndex++) {
			Object value = thisReactor.getValue(values2Sync[valIndex]);
			if(value != null) {
				curReactor.put(input + "_" + values2Sync[valIndex], value);
			}
		}		
	}
	
	private String getCol(String colName) {
		colName = colName.substring(colName.indexOf(":") + 1);
		colName = colName.trim();

		return colName;
	}

	@Override
	public void inAApiBlock(AApiBlock node) {
		if(reactorNames.containsKey(PKQLEnum.API)) {
			initReactor(PKQLEnum.API);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.API, nodeStr.trim());
			curReactor.put("ENGINE", node.getEngineName() + "");
			curReactor.put("INSIGHT", node.getInsight() + "");
		}		
	}
	
	@Override
	public void outAApiBlock(AApiBlock node) {
		String nodeStr = node + "";
		nodeStr = nodeStr.trim();
		IScriptReactor thisReactor = curReactor;
		
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.API, nodeStr, PKQLEnum.API); // I need to make this into a string
		// I also need the various columns to join
		// infact I need a way to say what to pass
		// this should sit on the parent and not the child
		// the curReactor is the API
		if(curReactor != null) {
			String [] values2Sync = curReactor.getValues2Sync(PKQLEnum.API);
			synchronizeValues(PKQLEnum.API, values2Sync, thisReactor);
		}
	}

	@Override
	public void inAExprScript(AExprScript node) {
		// everytime I need to open it
		if(reactorNames.containsKey(PKQLEnum.EXPR_SCRIPT)) {
			// simplify baby simplify baby simplify
			initReactor(PKQLEnum.EXPR_SCRIPT);
			String nodeStr = node.getExpr() + "";
			curReactor.put(PKQLEnum.EXPR_TERM, nodeStr.trim());
		}		
	}

	@Override
	public void outAExprScript(AExprScript node) {
		String nodeStr = node.getExpr() + "";
		nodeStr = nodeStr.trim();
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.EXPR_SCRIPT, nodeStr, (node + "").trim());
	}

	@Override
	public void inAVizChange(AVizChange node) {
		System.out.println("in a viz change");
		initReactor(PKQLEnum.VIZ);
		String layout = node.getLayout().toString().trim();
		String alignment = node.getDatatablealign().toString().trim();
		runner.addFeData("layout", layout);
		runner.addFeData("dataTableAlign", alignment);
		runner.setResponse("Successfully set layout to " + layout + " with alignment " + alignment);//
		runner.setStatus("SUCCESS");
	}

	@Override
	public void outAVizChange(AVizChange node) {
		System.out.println("out a viz change");
		deinitReactor(PKQLEnum.VIZ, "", "");
//		String nodeStr = node.getExpr() + "";
//		nodeStr = nodeStr.trim();
//		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.EXPR_SCRIPT, nodeStr, (node + "").trim());
	}
	
	@Override 
	public void inAVizopScript(AVizopScript node){
		runner.addFeData("type", "visual");
	}

	@Override
	public void inATermExpr(ATermExpr node) {
		if(reactorNames.containsKey(PKQLEnum.EXPR_TERM)) {
				// get the appropriate reactor
				initReactor(PKQLEnum.EXPR_TERM);
				// get the name of reactor
				String nodeStr = node.getTerm() + "";
				curReactor.put("G", frame);
				curReactor.put(PKQLEnum.EXPR_TERM, nodeStr.trim());
		}	
		// I need to find if there is a parent to this
		// which is also an expr term
		// I need some way to see if the parent is the same
		// then I should just assimilate
		// instead of trying to redo it
		// I need some way to figure out
		// && whatICallThisInMyWorld.containsKey("PARENT") && whatICallThisInMyWorld.get("PARENT").equalsIgnoreCase("EXPR_TERM")					
		// this is the one that has paranthesis
	}

	@Override
	public void outATermExpr(ATermExpr node) {
		//System.out.println("Successful in retrieving the data for expr term ?+ node.getExpr() + 	+ dataKeeper.containsKey((node.getExpr() + "").trim()));
		// get the value of it
		// I am not goiong to do anything here
		System.out.println("Printing expression.. " + node.getTerm());
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.EXPR_TERM, node.getTerm().toString().trim(),  node.toString().trim());

		if (thisReactorHash.get(PKQLEnum.EXPR_TERM) instanceof ExprReactor) {
			ExprReactor thisReactor = (ExprReactor)thisReactorHash.get(PKQLEnum.EXPR_TERM);
			String expr = (String)thisReactor.getValue(PKQLEnum.EXPR_TERM);
			curReactor.put("COL_DEF", thisReactor.getValue(PKQLEnum.COL_DEF));
			
			// see if parent exists
			String parent = (String)thisReactorHash.get("PARENT_NAME");
			// if the parent is not null
			if(parent != null && reactorHash.containsKey(parent)) {
				IScriptReactor parentReactor = (IScriptReactor)reactorHash.get(parent);
				Collection<? extends Object> values = (Collection<? extends Object>) thisReactor.getValue(PKQLEnum.COL_DEF);
				for(Object obj : values) {
					parentReactor.set("COL_DEF", obj);
				}
			}
			
			curReactor.addReplacer(expr, thisReactor.getValue(expr));
			runner.setResponse(thisReactor.getValue(expr));
			runner.setStatus((String)thisReactor.getValue("STATUS"));
		}
	}
	
	@Override
    public void inAEExprExpr(AEExprExpr node) {
        System.out.println("In The EXPRESSION .. " + node);
    }
	
	@Override
	public void outAPlusExpr(APlusExpr node) {
		String leftKeyName = node.getLeft() + "";
		String rightKeyName = node.getRight() + "";

		Object leftObj = curReactor.getValue(leftKeyName.trim());
		Object rightObj = curReactor.getValue(rightKeyName.trim());
		//System.out.println(node.getLeft() + " [][] " + node.getRight());
		Object result = null;
		if (rightObj instanceof Double && leftObj instanceof Double) {
			result = (Double)(leftObj)
					+ (Double)(rightObj);
			// remove the left and right key
			curReactor.addReplacer((node + "").trim(), result);
			curReactor.removeReplacer(leftKeyName.trim());
			curReactor.removeReplacer(rightKeyName.trim());
		}
	}

	@Override
	public void inAMinusExpr(AMinusExpr node) {
		//System.out.println("MINUS... " + node);
	}

	@Override
	public void outAMinusExpr(AMinusExpr node) {
		String leftKeyName = node.getLeft() + "";
		String rightKeyName = node.getRight() + "";

		Object leftObj = curReactor.getValue(leftKeyName.trim());
		Object rightObj = curReactor.getValue(rightKeyName.trim());
		//System.out.println(node.getLeft() + " [][] " + node.getRight());
		Object result = null;
		if (rightObj instanceof Double && leftObj instanceof Double) {
			result = (Double)(leftObj)
					- (Double)(rightObj);
			// remove the left and right key
			curReactor.addReplacer((node + "").trim(), result);
			curReactor.removeReplacer(leftKeyName);
			curReactor.removeReplacer(rightKeyName);
		}
	}
	
	@Override
	public void outAMultExpr(AMultExpr node) {
		String leftKeyName = node.getLeft() + "";
		String rightKeyName = node.getRight() + "";

		Object leftObj = curReactor.getValue(leftKeyName.trim());
		Object rightObj = curReactor.getValue(rightKeyName.trim());
		//System.out.println(node.getLeft() + " [][] " + node.getRight());
		Object result = null;
		if (rightObj instanceof Double && leftObj instanceof Double) {
			result = (Double)(leftObj)
					* (Double)(rightObj);
			// remove the left and right key
			curReactor.addReplacer((node + "").trim(), result);
			curReactor.removeReplacer(leftKeyName);
			curReactor.removeReplacer(rightKeyName);
		}
	}

	@Override
	public void outADivExpr(ADivExpr node) {
		String leftKeyName = node.getLeft() + "";
		String rightKeyName = node.getRight() + "";

		Object leftObj = curReactor.getValue(leftKeyName.trim());
		Object rightObj = curReactor.getValue(rightKeyName.trim());
		//System.out.println(node.getLeft() + " [][] " + node.getRight());
		Object result = null;
		if (rightObj instanceof Double && leftObj instanceof Double) {
			result = (Double)(leftObj)
					/ (Double)(rightObj);
			// remove the left and right key
			curReactor.addReplacer((node + "").trim(), result);
			curReactor.removeReplacer(leftKeyName);
			curReactor.removeReplacer(rightKeyName);
		}
	}

	@Override
	public void outAModExpr(AModExpr node) {
	}

	@Override
	public void inAColDef(AColDef node) {
		String colName = node.getColname().toString().trim();
		// adding to the reactor
		curReactor.set("COL_DEF", colName);
		curReactor.addReplacer((node + "").trim(), colName);
	}

	@Override
	public void inAAddColumn(AAddColumn node) {
		if(reactorNames.containsKey(PKQLEnum.COL_ADD)) {
			initReactor(PKQLEnum.COL_ADD);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.COL_ADD, nodeStr.trim());
		}		
	}
	
	@Override
	public void outAAddColumn(AAddColumn node) {
		String nodeExpr = node.getExpr().toString().trim();
		curReactor.put(PKQLEnum.EXPR_TERM, nodeExpr);
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.COL_ADD, nodeExpr, node.toString().trim());
	}
	
	@Override
	public void inAFilterColumn(AFilterColumn node) {
		if(reactorNames.containsKey(PKQLEnum.FILTER_DATA)) {
			initReactor(PKQLEnum.FILTER_DATA);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.FILTER_DATA, nodeStr.trim());
		}
	}
//	
	@Override
	public void outAFilterColumn(AFilterColumn node) {
		String nodeExpr = node.getWhere().toString().trim();
//		curReactor.put(PKQLEnum.WHERE, nodeExpr);
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.FILTER_DATA, nodeExpr, node.toString().trim());
		IScriptReactor previousReactor = (IScriptReactor)thisReactorHash.get(PKQLEnum.FILTER_DATA.toString());
		runner.setStatus((String)previousReactor.getValue("STATUS"));
		runner.setResponse("Filtered Column: " + (String)previousReactor.getValue("FILTER_COLUMN"));
	}
	
	@Override
    public void inAUnfilterColumn(AUnfilterColumn node) {
		if(reactorNames.containsKey(PKQLEnum.UNFILTER_DATA)) {
			initReactor(PKQLEnum.UNFILTER_DATA);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.UNFILTER_DATA, nodeStr.trim());
		}
    }

	@Override
    public void outAUnfilterColumn(AUnfilterColumn node) {
		String nodeExpr = node.getColDef().toString().trim();
//		curReactor.put(PKQLEnum.WHERE, nodeExpr);
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.UNFILTER_DATA, nodeExpr, node.toString().trim());
		IScriptReactor previousReactor = (IScriptReactor)thisReactorHash.get(PKQLEnum.UNFILTER_DATA.toString());
		runner.setStatus((String)previousReactor.getValue("STATUS"));
		runner.setResponse("Unfiltered Column: " + (String)previousReactor.getValue("FILTER_COLUMN"));
    }

	@Override
	public void outAExprGroup(AExprGroup node) {
	}

	@Override
    public void inAImportData(AImportData node){
		if(reactorNames.containsKey(PKQLEnum.IMPORT_DATA)) {
			// simplify baby simplify baby simplify
			initReactor(PKQLEnum.IMPORT_DATA);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.IMPORT_DATA, nodeStr.trim());
		}		
    }
    
    @Override
    public void outAImportData(AImportData node){
		String nodeStr = node.getApi() + "";
		nodeStr = nodeStr.trim();
		curReactor.put(PKQLEnum.EXPR_TERM, nodeStr);
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.IMPORT_DATA, nodeStr, (node + "").trim());
    	IScriptReactor previousReactor = (IScriptReactor)thisReactorHash.get(PKQLReactor.IMPORT_DATA.toString());
		runner.setResponse(previousReactor.getValue(node.toString().trim()));//
		runner.setStatus((String)previousReactor.getValue("STATUS"));
    }

    @Override
	public void outASetColumn(ASetColumn node) {
		//System.out.println("Set.. [" + (node.getExpr() + "").trim() + "]");
	}

	@Override
	public void outAVarop(AVarop node) {
		String varName = getCol(node.getName() + "");
		String expr = getCol(node.getExpr() + "");
		//System.out.println("Variable declaration " + varName + " =  " + expr);
	}

	@Override
	public void inANumberTerm(ANumberTerm node) {
		//System.out.println("Number term.. >>> " + node.getDecimal());
		String number = node.getDecimal() + "";
	}
	
	@Override
    public void inADecimal(ADecimal node) {
		//System.out.println("DECIMAL VALUE.. >>> " + node);
		String fraction = node.getFraction() +"";
		String number = (node.getWhole() + "").trim();
		if(node.getFraction() != null)
			number = number + "." + (node.getFraction() + "").trim();
		
    	// I also need to add this into mystore - need a cleaner way to do this
    	//curReactor.set( node.toString().trim(), Double.parseDouble(number));
    	curReactor.addReplacer(node.toString().trim(), Double.parseDouble(number));
	}
    
    @Override
    public void inAWord(AWord node) {
    }
    
    @Override
    public void outAWord(AWord node) {
        //System.out.println("In a word.. " + node); // need to find a way to clean up information puts a space after the quote
        curReactor.set(PKQLEnum.WORD_OR_NUM, (node + "").trim());
        //thisRow.addElement((node + "").trim());        
    }

    @Override
    public void outANumWordOrNum(ANumWordOrNum node) {
    }

	@Override
    public void inAExprRow(AExprRow node) {
    }

    @Override
    public void outAExprRow(AExprRow node) {
    }

    @Override
	public void inAMathFunTerm(AMathFunTerm node) {
	}

	@Override
	public void inAMathFun(AMathFun node) {
		if(reactorNames.containsKey(PKQLEnum.MATH_FUN)) {
			// get the appropriate reactor
			String procedureName = node.getId().toString().trim();
			String nodeStr = node.getExpr().toString().trim();
			
			initReactor(PKQLReactor.MATH_FUN.toString());
			// get the name of reactor
			curReactor.put(PKQLEnum.G, frame);
			curReactor.put(PKQLEnum.MATH_FUN, nodeStr.trim());
			curReactor.put(PKQLEnum.PROC_NAME, procedureName);
		}	
	}

	@Override
	public void outAMathFun(AMathFun node) {
		// function would usually get
		/*
		 * a. Expression - what to compute b. List of columns to pull c.
		 * Iterator which has all of these different columns pulled d.
		 * getValue() method which will actually return an object e.
		 */
		// need to accomodate for the array that is there
		// I need to do something to find if I am at the right level
		// how no Shit works
		// I need to set some stuff
		// like the tinker frame etc.. 
		String nodeStr = node.toString().trim();
		String expr = node.getExpr().toString().trim();
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLReactor.MATH_FUN.toString(), expr, nodeStr);
		IScriptReactor previousReactor = (IScriptReactor)thisReactorHash.get(PKQLReactor.MATH_FUN.toString());
//		if (thisReactorHash.get(PKQLReactor.MATH_FUN.toString()) instanceof MathReactor) {
//			MathReactor thisReactor = (MathReactor)thisReactorHash.get(PKQLReactor.MATH_FUN.toString());
			curReactor.put("COL_DEF", previousReactor.getValue(PKQLEnum.COL_DEF)); //TODO: use syncronize instead
			curReactor.addReplacer(nodeStr, previousReactor.getValue(expr));
			runner.setResponse(previousReactor.getValue(expr));
			runner.setStatus((String)previousReactor.getValue("STATUS"));
//		}
	}
	
	
    public void inAImportColumn(AImportColumn node) {
        //System.out.println("In the import col operation");
        //System.out.println("DATA ..... " + node.getData());
        //System.out.println("DATA ..... " + node.getCols());
        
        IAction thisAction = new ImportAction();
        thisAction.set("TF", frame); // will need to change to constants afterwards
    }
	
    @Override
    public void inAColWhere(AColWhere node) {
		if(reactorNames.containsKey(PKQLEnum.WHERE)) {
			initReactor(PKQLEnum.WHERE);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.WHERE, nodeStr.trim());
			curReactor.put(PKQLEnum.COMPARATOR, (node.getComparator()+"").trim());
		}		
    }

    @Override
    public void outAColWhere(AColWhere node) {
        // I need to do some kind of action and pop out the last one on everything
		String nodeStr = node + "";
		nodeStr = nodeStr.trim();
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.WHERE, nodeStr, PKQLEnum.FILTER, false);
    }

    @Override
    public void inARelationDef(ARelationDef node) {
		if(reactorNames.containsKey(PKQLEnum.REL_DEF)) {
			initReactor(PKQLEnum.REL_DEF);
			String nodeStr = node.toString().trim();
			curReactor.put(PKQLEnum.REL_DEF, nodeStr);
			curReactor.put(PKQLEnum.REL_TYPE, (node.getRelType().toString()).trim());
		}		
    }

    @Override
    public void outARelationDef(ARelationDef node) {
		String nodeStr = node + "";
		nodeStr = nodeStr.trim();
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.REL_DEF, nodeStr, PKQLEnum.JOINS, false);
    }
    
    @Override
    public void inAColCsv(AColCsv node) {
    	System.out.println("Directly lands into col csv " + node);
		if(reactorNames.containsKey(PKQLEnum.COL_CSV))
		{
			initReactor(PKQLEnum.COL_CSV);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.COL_CSV, nodeStr.trim());
		}
    }

    @Override
    public void outAColCsv(AColCsv node) {
    	String thisNode = node.toString().trim();
		Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLEnum.COL_CSV, thisNode, PKQLEnum.COL_CSV);
		
    }

    @Override
    public void inACsvRow(ACsvRow node) {
    	System.out.println("Directly lands into col csv " + node);
		if(reactorNames.containsKey(PKQLEnum.ROW_CSV)) {
			initReactor(PKQLEnum.ROW_CSV);
			String nodeStr = node + "";
			curReactor.put(PKQLEnum.ROW_CSV, nodeStr.trim());
		}
    }
    
    @Override
    public void outACsvRow(ACsvRow node) {
    	// I need to do an action here
    	// get the action
    	// call to say this has happened and then reset it to null;
    	String thisNode = node.toString().trim();
		deinitReactor(PKQLEnum.ROW_CSV, thisNode, PKQLEnum.ROW_CSV);
    }
    
    @Override
    public void inAROp(AROp node) {
    	String script = node.getCodeblock().toString().trim();
    	script = script.substring(1, script.length()-1); // have to exclude curly braces
    	String nodeStr = node.toString().trim();
    	initReactor(PKQLReactor.R_OP.toString());
    	curReactor.put(PKQLEnum.G, frame);
    	curReactor.put(PKQLReactor.R_OP.toString(), nodeStr);
    	curReactor.put(PKQLToken.CODE.toString(), script);
    }
    
    @Override
    public void outAROp(AROp node) {
    	String nodeStr = node.toString().trim();
    	Hashtable <String, Object> thisReactorHash = deinitReactor(PKQLReactor.R_OP.toString(), nodeStr, nodeStr); // Should 2nd argument be codeblock?
    	IScriptReactor previousReactor = (IScriptReactor)thisReactorHash.get(PKQLReactor.R_OP.toString());
		runner.setResponse(previousReactor.getValue(nodeStr));
		runner.setStatus((String)previousReactor.getValue("STATUS"));
    }
    
    @Override
    public void inAHelpScript(AHelpScript node) {
    	//TODO: build out a String that explains PKQL and the commands
    	runner.setResponse("Welcome to PKQL. Please look through documentation to find available functions.");
    	runner.setStatus("success");
    }
	
}