package prerna.algorithm.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClusterRemoveDuplicates {

	private ArrayList<Object[]> retMasterTable;
	private String[] retVarNames;
	
	public ClusterRemoveDuplicates(ArrayList<Object[]> masterTable, String[] names) {
		formatDuplicateResults(masterTable, names);
	}

	public ArrayList<Object[]> getRetMasterTable() {
		return retMasterTable;
	}

	public String[] getRetVarNames() {
		return retVarNames;
	}

	//	//TODO: make generic
	public void formatDuplicateResults(ArrayList<Object[]> masterTable, String[] names) {
		int i;
		int numRows = masterTable.size();
		int numCols = masterTable.get(0).length;
		Set<String> instancesSet = new HashSet<String>();
		Set<String> uniquePropNamesSet = new HashSet<String>();
		for(i = 0; i < numCols; i++ ) {
			int j;
			for(j = 0; j < numRows; j++) {
				Object[] row = masterTable.get(j);
				if(i == 0) {
					instancesSet.add(row[i].toString());
				} else {
					uniquePropNamesSet.add(row[i].toString());
				}
			}
		}

		int numInstances = instancesSet.size();
		if(numInstances == numRows) {
			retMasterTable = masterTable;
			retVarNames = names;
			return;
		}
		
		retMasterTable = new ArrayList<Object[]>();
		Object[] uniquePropNames = uniquePropNamesSet.toArray();
		Object[] instances = instancesSet.toArray();
		
		retVarNames = Arrays.copyOf(new String[]{names[0]}, 1 + uniquePropNames.length);
		System.arraycopy(uniquePropNames, 0, retVarNames, 1, uniquePropNames.length);
		
		int newNumCols = uniquePropNames.length;
		for(i = 0; i < numInstances; i++) {
			Object[] newRow = new Object[newNumCols + 1];
			newRow[0] = instances[i];
			retMasterTable.add(newRow);
		}

		OUTER: for(i = 0; i < numRows; i++) {	
			Object[] row = masterTable.get(i);
			int j;
			for(j = 0; j < numInstances; j++) {
				Object[] newRow = retMasterTable.get(j);
				if(row[0].equals(newRow[0])) {
					int k;
					INNERMOST: for(k = 0; k < newNumCols; k++) {
						int l;
						for(l = 1; l < row.length; l++) {
							if(uniquePropNames[k].toString().equals(row[l].toString())){
								newRow[k+1] = "Yes";
								continue INNERMOST;
							}
						}
					}
					continue OUTER;
				}
			}
		}
		
		for(i = 0; i < retMasterTable.size(); i++)
		{
			Object[] row = retMasterTable.get(i);
			int j;
			for(j = 0; j < row.length; j++) {
				if(row[j] == null) {
					row[j] = "No";
				}
			}
		}
	}
	
}
