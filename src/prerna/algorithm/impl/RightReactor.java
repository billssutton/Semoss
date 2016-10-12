package prerna.algorithm.impl;

import java.util.Iterator;
import java.util.Vector;

import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.ExpressionIterator;
import prerna.sablecc.MathReactor;
import prerna.sablecc.PKQLEnum;
import prerna.sablecc.PKQLRunner.STATUS;

public class RightReactor  extends MathReactor { // TODO create BaseMapperReactor once more mapping algorithms have been added
	
	@Override
	public Iterator process() {
		modExpression();
		Vector <String> columns = (Vector <String>)myStore.get(PKQLEnum.COL_DEF);
		String[] columnsArray = convertVectorToArray(columns);
		String script = myStore.get("MOD_" + whoAmI).toString();
		Iterator iterator = getTinkerData(columns, (ITableDataFrame)myStore.get("G"), false);
		String indx = myStore.get("MATH_PARAM").toString().replace("{\"CONDITION1\"=", "").replace("}", "");
	    String str=script.replace(" ]", "")+".reverse().take("+indx+").reverse()]";
		String nodeStr = myStore.get(whoAmI).toString();	
		ExpressionIterator expIt = new ExpressionIterator(iterator, columnsArray,str );
		myStore.put(nodeStr, expIt);
		myStore.put("STATUS",STATUS.SUCCESS);
		
		return expIt;
	}
	
}

//col.add(new,m:Right([c:Customer],{"index":3}));