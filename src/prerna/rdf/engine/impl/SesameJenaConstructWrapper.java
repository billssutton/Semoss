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
package prerna.rdf.engine.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;

import prerna.rdf.engine.api.IEngine;
import prerna.util.Utility;

import com.google.gson.Gson;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * The wrapper helps takes care of selection of the type of engine you are using (Jena/Sesame).  This wrapper processes CONSTRUCT statements. 
 */
public class SesameJenaConstructWrapper extends AbstractWrapper{
	
	public transient GraphQueryResult gqr = null;	
	transient Model model = null;
	transient StmtIterator si = null;	
	public transient IEngine engine = null;
	transient Enum engineType = IEngine.ENGINE_TYPE.SESAME;
	transient String query = null;
	transient com.hp.hpl.jena.rdf.model.Statement curSt = null;
	transient SesameJenaConstructStatement retSt = null;
	public transient boolean queryBoolean = true;
	transient Logger logger = Logger.getLogger(getClass());
	transient SesameJenaConstructWrapper remoteWrapperProxy = null;
	transient ObjectInputStream ris = null;// remote input stream
	
	
	
	/**
	 * Method setGqr. - Sets the Graph query result.
	 * @param gqr GraphQueryResult - The graph query result that this is being set to.
	 */
	public void setGqr(GraphQueryResult gqr)
	{
		this.gqr = gqr;
	}
	
	/**
	 * Method setEngine. Sets the engine.
	 * @param engine IEngine - The engine that this is being set to.
	 */
	public void setEngine(IEngine engine)
	{
		this.engine = engine;
		engineType = engine.getEngineType();
	}
	
	/**
	 * Method setQuery. - Sets the SPARQL query statement.
	 * @param query String - The string version of the SPARQL query.
	 */
	public void setQuery(String query)
	{
		this.query = query;
	}
	
	/**
	 * Method execute.  Executes the SPARQL query based on the type of engine selected.
	 */
	public void execute()
	{
		try {
			if(engineType == IEngine.ENGINE_TYPE.SESAME)
			{
				gqr = (GraphQueryResult)engine.execGraphQuery(this.query);
			}
			else if (engineType == IEngine.ENGINE_TYPE.JENA)
			{
				model = (Model)engine.execGraphQuery(query);
				setModel(model);
			}
			else if(engineType == IEngine.ENGINE_TYPE.SEMOSS_SESAME_REMOTE)
			{
				// get the actual SesameJenaConstructWrapper from the engine
				// this is json output
				//System.out.println("Trying to get the wrapper remotely now");
				// get the input stream directly here
				remoteWrapperProxy = (SesameJenaConstructWrapper)engine.execGraphQuery(query);
			}
		} catch (RuntimeException e) {
			// TODO: Specify exception
			e.printStackTrace();
		}
	}
	
	/**
	 * Method setModel. Sets the type of model being used.
	 * @param model Model - The model type.
	 */
	public void setModel(Model model)
	{
		this.model = model;
		si = model.listStatements();
	}
	
	/**
	 * Method hasNext.  Checks to see if the tuple query result has additional results.
	
	 * @return boolean - True if the Tuple Query result has additional results.
	 * */
	public boolean hasNext() 
	{
		boolean retBool = false;
		try
		{
			logger.debug("Checking for next " );
			if(engineType == IEngine.ENGINE_TYPE.SESAME)
			{
				retBool = gqr.hasNext();
				if(!retBool)
					gqr.close();
			}
			else if(engineType == IEngine.ENGINE_TYPE.JENA)
			{
				retBool = si.hasNext();
				if(!retBool)
					si.close();
			}
			// need to include an engine type remote so that it can pull it through REST API
			else if(engineType == IEngine.ENGINE_TYPE.SEMOSS_SESAME_REMOTE)
			{
				if(retSt != null) // they have not picked it up yet
					return true;
				retSt = new SesameJenaConstructStatement();
				// I need to pull from remote
				// this is just so stupid to call its own
				if(ris == null)
				{
					Hashtable params = new Hashtable<String,String>();
					params.put("id", remoteWrapperProxy.getRemoteID());
					ris = new ObjectInputStream(Utility.getStream(remoteWrapperProxy.getRemoteAPI() + "/next", params));
				}					
				try {
					Object myObject = ris.readObject();
					
					if(!myObject.toString().equalsIgnoreCase("null"))
					{
						Statement stmt = (Statement)myObject;
						retSt.setSubject(stmt.getSubject()+"");
						retSt.setObject(stmt.getObject());
						retSt.setPredicate(stmt.getPredicate() + "");
						//System.out.println("Abile to get the object appropriately here " + retSt.getSubject());
						retBool = true;
					}
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					retSt = null;
					retBool = false;
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					retSt = null;
					retBool = false;
				}

				
				/*Hashtable params = new Hashtable<String,String>();
				params.put("id", remoteWrapperProxy.getRemoteID());
				System.out.println("ID for remote is " + remoteWrapperProxy.getRemoteID());
				String output = Utility.retrieveResult(remoteWrapperProxy.getRemoteAPI() + "/hasNext", params);
				Gson gson = new Gson();
				retBool = gson.fromJson(output, Boolean.class); // cleans up automatically at the remote end
				*/
				
			}
			
			
		}catch(RuntimeException ex)
		{
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug(" Next " + retBool);
		return retBool;
	}
	
	/**
	 * Method next.  Processes the select statement for either Sesame or Jena.
	
	 * @return SesameJenaConstructStatement - returns the construct statement.
	 */
	public SesameJenaConstructStatement next()
	{
		SesameJenaConstructStatement thisSt = null;
		try
		{
			if(engineType == IEngine.ENGINE_TYPE.SESAME)
			{
				thisSt = new SesameJenaConstructStatement();
				logger.debug("Adding a sesame statement ");
				Statement stmt = gqr.next();
				thisSt.setSubject(stmt.getSubject()+"");
				thisSt.setObject(stmt.getObject());
				thisSt.setPredicate(stmt.getPredicate() + "");
				
			}
			else if(engineType == IEngine.ENGINE_TYPE.JENA)
			{
				thisSt = new SesameJenaConstructStatement();
				com.hp.hpl.jena.rdf.model.Statement stmt = si.next();
				logger.debug("Adding a JENA statement ");
				curSt = stmt;
				Resource sub = stmt.getSubject();
				Property pred = stmt.getPredicate();
				RDFNode node = stmt.getObject();
				if(node.isAnon())
					thisSt.setPredicate(Utility.getNextID());
				else 	
					thisSt.setPredicate(stmt.getPredicate() + "");

				if(sub.isAnon())
					thisSt.setSubject(Utility.getNextID());
				else
					thisSt.setSubject(stmt.getSubject()+"");
				
				if(node.isAnon())
					thisSt.setObject(Utility.getNextID());
				else
					thisSt.setObject(stmt.getObject());
			}
			else if(engineType == IEngine.ENGINE_TYPE.SEMOSS_SESAME_REMOTE)
			{
				thisSt = retSt;
				retSt = null;
			}

		}catch(RuntimeException ex)
		{
			ex.printStackTrace();
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return thisSt;
	}

	/**
	 * Method getJenaStatement.  Gets the query solution for a JENA model.
	
	 * @return com.hp.hpl.jena.rdf.model.Statement */
	public com.hp.hpl.jena.rdf.model.Statement getJenaStatement()
	{
		return curSt;
	}
	
	/**
	 * Method setEngineType. Sets the engine type.
	 * @param engineType Enum - The type engine that this is being set to.
	 */
	public void setEngineType(Enum engineType)
	{
		this.engineType = engineType;
	}

	public static void main(String [] args)
	{
		RemoteSemossSesameEngine engine = new RemoteSemossSesameEngine();
		engine.setAPI("http://localhost:9080/Monolith/api/engine");
		engine.setDatabase("Movie_DB");
		engine.setEngineName("Movie_DB");
		
		engine.openDB(null);
		
		System.out.println("Perspectives is .... " + engine.getPerspectives());
		
		System.out.println("Trying.. ");
		SesameJenaConstructWrapper sjcw = new SesameJenaConstructWrapper(); //(SesameJenaSelectWrapper) engine.execSelectQuery("SELECT ?S ?P ?O WHERE {{?S ?P ?O}.} LIMIT 1");
		sjcw.setEngine(engine);
		sjcw.setEngineType(engine.getEngineType());
		sjcw.setQuery("CONSTRUCT {?subject ?predicate ?object} WHERE {{?subject ?predicate ?object.}}");
		
		sjcw.execute();
		
		System.out.println(" has next " + sjcw.hasNext());
		SesameJenaConstructStatement st = sjcw.next();
		
		System.out.println(st.getSubject());
		
		//System.out.println(" var " + sjcw.getVariables());
		
	}
	

}
