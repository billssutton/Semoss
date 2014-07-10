/*******************************************************************************
 * Copyright 2013 SEMOSS.ORG
 * 
 * This file is part of SEMOSS.
 * 
 * SEMOSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SEMOSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SEMOSS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package prerna.ui.helpers;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import prerna.rdf.engine.api.IEngine;
import prerna.ui.components.ParamComboBox;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

/**
 * This gets access to the engine and runs a query with parameters, and helps appropriately process the results.
 */
public class EntityFiller implements Runnable {
	
	Logger logger = Logger.getLogger(getClass());

	public JComboBox box;
	public String type;
	public IEngine engine;
	public String engineName;
	public Vector<String> names;
	public String extQuery;
	public Vector<String> nameVector;
	
	/**
	 * Method run. Gets access to engine, gets the type query based on the type of engine, fills query parameters, and runs the query.
	 */
	@Override
	public void run() {
		logger.setLevel(Level.WARN);
		logger.info(" Engine Name is  " + engineName);
		engine = (IEngine)DIHelper.getInstance().getLocalProp(engineName);
		names = new Vector<String>();
		if (box != null && type != null) { 
			// if options for the parameter have been explicitly defined on the question sheet
			// parse and use just those
			if(DIHelper.getInstance().getProperty(
					type + "_" + Constants.OPTION) != null){
				// try to pick this from DBCM Properties table
				// this will typically be of the format
				String options = DIHelper.getInstance().getProperty(
						type + "_" + Constants.OPTION);
				// this is a string with ; delimited values
				StringTokenizer tokens = new StringTokenizer(options, ";");
				
				// sorry for the cryptic crap below
				int tknIndex = 0;
				for (; tokens.hasMoreTokens(); names.addElement(tokens
						.nextToken()), tknIndex++)
					;
				
				DefaultComboBoxModel model = new DefaultComboBoxModel(names);
				
				box.setModel(model);
				box.setEditable(false);
				//DIHelper.getInstance().setLocalProperty(type, names);
			}
			// the the param options have not been explicitly defined and the combo box has not been cached
			// time for the main processing
			else if (DIHelper.getInstance().getLocalProp(type) == null) {
				//check if URI is used in param filler
				if(!type.contains("http://"))
				{
					names.addElement("Incorrect Param Fill");
					DefaultComboBoxModel model = new DefaultComboBoxModel(names);
					box.setModel(model);
					return;
				}
				// use the type query defined on RDF Map unless external query has been defined
				String sparqlQuery = DIHelper.getInstance().getProperty(
						"TYPE" + "_" + Constants.QUERY);
				
				Hashtable paramTable = new Hashtable();
				paramTable.put(Constants.ENTITY, type);
				if (extQuery!=null)
				{
					sparqlQuery=extQuery;
				}
				else
				{
					sparqlQuery = Utility.fillParam(sparqlQuery, paramTable);	
				}
				
				// get back all of the URIs that are of that type
				names = engine.getEntityOfType(sparqlQuery);
//				if(engine instanceof AbstractEngine){
//					Vector<String> baseNames = ((AbstractEngine)engine).getBaseDataEngine().getEntityOfType(sparqlQuery);
//					for(String name: baseNames) 
//						if(!names.contains(name)) 
//							names.addAll(baseNames);
//				}
				
				// try to query for the label
				
				logger.info("Names " + names);
				Hashtable paramHash = Utility.getInstanceNameViaQuery(names);
				if (paramHash.isEmpty())
				{
					names.addElement("Concept Doesn't Exist in DB");
					DefaultComboBoxModel model = new DefaultComboBoxModel(names);
					box.setModel(model);
					return;
				}
				//keys are the labels, objects are the URIs
				Set nameC = paramHash.keySet();
				nameVector = new Vector(nameC);
				Collections.sort(nameVector);
				
				// if it is a paramcombobox, set the whole hashtable--will need to look up the URI for selected label later
				if(box instanceof ParamComboBox)
					((ParamComboBox)box).setData(paramHash, nameVector);
				// else just set the model on the box with the list
				else
				{
					DefaultComboBoxModel model = new DefaultComboBoxModel(nameVector);
					box.setModel(model);
				}
					
				box.setEditable(false);
			}
			else
			{
				names.addElement("Unknown");			
			}

		}
		// if the type is not null but their is no box to fill
		// fills the names array with all URIs of set type
		else if (type !=null)
		{
			if (DIHelper.getInstance().getLocalProp(type) == null) {
				String sparqlQuery = DIHelper.getInstance().getProperty(
						"TYPE" + "_" + Constants.QUERY);
				Hashtable paramTable = new Hashtable();
				paramTable.put(Constants.ENTITY, type);
				if (extQuery!=null)
				{
					sparqlQuery=extQuery;
				}
				else
				{
					sparqlQuery = Utility.fillParam(sparqlQuery, paramTable);	
				}	

				names = engine.getEntityOfType(sparqlQuery);
				Collections.sort(names);
				Hashtable paramHash = Utility.getInstanceNameViaQuery(names);
				if (paramHash.isEmpty())
				{
					names.addElement("Concept Doesn't Exist in DB");
					DefaultComboBoxModel model = new DefaultComboBoxModel(names);
					box.setModel(model);
					return;
				}
				//keys are the labels, objects are the URIs
				Set nameC = paramHash.keySet();
				nameVector = new Vector(nameC);
				Collections.sort(nameVector);
			}
		}

	}

	/**
	 * Method setExternalQuery.  Sets the external query to the given SPARQL query.
	 * @param query String - The SPARQL query in string form that this external query is set to.
	 */
	public void setExternalQuery(String query)
	{
		this.extQuery = query;
	}
	
	/**
	 * Method setData.  Sets the data
	 * @param uriVector Vector<String> - The URIs that this is set to.
	 */
	public void setData(Vector<String> uriVector)
	{
		
	}

}
