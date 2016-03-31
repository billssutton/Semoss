/*******************************************************************************
 * Copyright 2015 Defense Health Agency (DHA)
 *
 * If your use of this software does not include any GPLv2 components:
 * 	Licensed under the Apache License, Version 2.0 (the "License");
 * 	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 *
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 * ----------------------------------------------------------------------------
 * If your use of this software includes any GPLv2 components:
 * 	This program is free software; you can redistribute it and/or
 * 	modify it under the terms of the GNU General Public License
 * 	as published by the Free Software Foundation; either version 2
 * 	of the License, or (at your option) any later version.
 *
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *******************************************************************************/
package prerna.rdf.engine.wrappers;

import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.evaluation.util.QueryEvaluationUtil;

import prerna.algorithm.api.ITableDataFrame;
import prerna.ds.BTreeDataFrame;
import prerna.engine.api.ISelectStatement;
import prerna.engine.api.ISelectWrapper;
import prerna.util.Constants;
import prerna.util.Utility;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class JenaSelectWrapper extends AbstractWrapper implements ISelectWrapper {
	
	transient ResultSet rs = null;
	
	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return rs.hasNext();
	}

	@Override
	public ISelectStatement next() 
	{
		ISelectStatement thisSt = new SelectStatement();
	    QuerySolution row = rs.nextSolution();
		for(int colIndex = 0;colIndex < displayVar.length;colIndex++)
		{
			String value = row.get(var[colIndex])+"";
			RDFNode node = row.get(var[colIndex]);
			
			thisSt.setVar(displayVar[colIndex], getRealValue(node));
			thisSt.setRawVar(displayVar[colIndex], value);
			logger.debug("Binding Name " + var[colIndex]);
			logger.debug("Binding Value " + value);
		}	
		return thisSt;
	}

	@Override
	public String[] getVariables() {
		return getDisplayVariables();
	}
	
	@Override
	public String[] getDisplayVariables() {
		displayVar = new String[rs.getResultVars().size()];
		List <String> names = rs.getResultVars();
		for (int colIndex = 0; colIndex < names.size(); colIndex++){
			String columnLabel = names.get(colIndex);
			String tableLabel = names.get(colIndex);
			boolean columnIsProperty = false;
			String tableLabelURI = Constants.CONCEPT_URI;
			String columnLabelURI = Constants.PROPERTY_URI;
			if(columnLabel.contains("__")){
				columnIsProperty = true;
				String[] splitColAndTable = columnLabel.split("__");
				tableLabel = splitColAndTable[0];
				columnLabel = splitColAndTable[1];
			}
		
			tableLabelURI += tableLabel;
			columnLabelURI += columnLabel;
			//now get the display name 
			tableLabelURI = engine.getTransformedNodeName(tableLabelURI, true);
			columnLabelURI = engine.getTransformedNodeName(columnLabelURI, true);
			tableLabel = Utility.getInstanceName(tableLabelURI);
			columnLabel = Utility.getInstanceName(columnLabelURI);
			if(columnIsProperty){
				columnLabel = tableLabel + "__" + columnLabel;
			} else {
				columnLabel = tableLabel;
			}
			
			displayVar[colIndex] = columnLabel;
		}
		return displayVar;
	}
	
	@Override
	public String[] getPhysicalVariables() {
		// get the result set metadata to get the column names
		var = new String[rs.getResultVars().size()];
		List <String> names = rs.getResultVars();
		for(int colIndex = 0;colIndex < names.size();var[colIndex] = names.get(colIndex), colIndex++);
		return var;
	}

	@Override
	public void execute() {
		rs = (ResultSet) engine.execQuery(query);		
	}
	
	private Object getRealValue(RDFNode node){
		if(node.isAnon())
		{
			logger.debug("Ok.. an anon node");
			return Utility.getNextID();
		}
		else
		{
			logger.debug("Raw data JENA For Column ");
			return Utility.getInstanceName(node + "");
		}
	}


	@Override
	public ITableDataFrame getTableDataFrame() {
		BTreeDataFrame dataFrame = new BTreeDataFrame(this.displayVar);
		while (hasNext()){
			logger.debug("Adding a jena statement ");
			QuerySolution row = rs.nextSolution();
			
			Object[] clean = new Object[this.displayVar.length];
			Object[] raw = new Object[this.displayVar.length];
			for(int colIndex = 0;colIndex < displayVar.length;colIndex++)
			{
				raw[colIndex] = row.get(var[colIndex] + "");
				clean[colIndex] = getRealValue(row.get(var[colIndex]));
			}
			dataFrame.addRow(clean, raw);
		}
		
		return dataFrame;
	}

}
