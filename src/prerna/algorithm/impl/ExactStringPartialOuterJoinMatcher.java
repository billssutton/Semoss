package prerna.algorithm.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.BTreeDataFrame;
import prerna.ds.SimpleTreeNode;

public class ExactStringPartialOuterJoinMatcher extends ExactStringMatcher {

	private static final Logger LOGGER = LogManager.getLogger(ExactStringPartialOuterJoinMatcher.class.getName());

	/**
	 * Assumes the partial outer join is keeping all values in table1Col for performMatch method
	 */
	public ExactStringPartialOuterJoinMatcher() {
		super();
	}
	
	@Override
	protected ITableDataFrame performMatch(Object[] table1Col, Object[] table2Col) {
		String table1ValueKey = "Table1Value";
		String table2ValueKey = "Table2Value";

		ITableDataFrame bTree = new BTreeDataFrame(new String[]{table1ValueKey, table2ValueKey});

		int success = 0;
		int total = 0;
		for(int i = 0; i < table1Col.length; i++) {
			boolean matchFound = false;
			for(int j = 0; j < table2Col.length; j++) {
				if(match(table1Col[i],table2Col[j])) {
					// update boolean for partial outer join
					matchFound = true;
					
					System.out.println("MATCHED::::::::::::::::: " + table1Col[i] + "      " +   table2Col[j]  );
					Map<String, Object> row = new HashMap<String, Object>();
					row.put(table1ValueKey , table1Col[i]);
					row.put(table2ValueKey, table2Col[j]);
					bTree.addRow(row, row); //TODO: adding values as both raw and clean
					success++;
				}
				total++;
			}

			// if no match found, add to a blank node
			if(!matchFound) {
				System.out.println("OUTER JOIN MATCHED::::::::::::::::: " + table1Col[i] + "       NULL ");
				Map<String, Object> row = new HashMap<String, Object>();
				row.put(table1ValueKey , table1Col[i]);
				row.put(table2ValueKey, SimpleTreeNode.EMPTY);
				bTree.addRow(row, row); //TODO: adding values as both raw and clean
			}
		}

		this.resultMetadata.put("success", success);
		this.resultMetadata.put("total", total);

		return bTree;
	}

}

