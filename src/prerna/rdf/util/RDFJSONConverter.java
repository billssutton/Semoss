/*******************************************************************************
 * Copyright 2015 SEMOSS.ORG
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
package prerna.rdf.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import prerna.engine.api.IEngine;
import prerna.engine.api.ISelectStatement;
import prerna.engine.api.ISelectWrapper;
import prerna.engine.impl.rdf.SesameJenaConstructStatement;
import prerna.engine.impl.rdf.SesameJenaSelectCheater;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.util.Utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RDFJSONConverter {

	public static 	Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	// this does the following things
	// convert a scalar query against a database into a json
	public static Object getSelectAsJSON(String query, IEngine engine)
	{
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		
		
	ISelectWrapper wrapper = WrapperManager.getInstance().getSWrapper(engine, query);
	
	/*
	SesameJenaSelectWrapper wrapper = new SesameJenaSelectWrapper();
	wrapper.setEngine(engine);
	wrapper.setQuery(query);
	wrapper.executeQuery();
	*/
	
	// get the bindings from it
	String [] names = wrapper.getVariables();
	int count = 0;
	// now get the bindings and generate the data
	while(wrapper.hasNext())
	{
		ISelectStatement sjss = wrapper.next();
		
		Object [] values = new Object[names.length];
		for(int colIndex = 0;colIndex < names.length;colIndex++)
		{
			values[colIndex] = sjss.getRawVar(names[colIndex])+"";
		}
		list.add(count, values);
		count++;
	}
	return list;
}

	/*{
		// the engine will always return a vector
		// the variables are then plugged into a hashtable
		// which is plugged into an vector and then converts the vector into a json
		Vector output = new Vector();
		SesameJenaSelectWrapper sjsw = new SesameJenaSelectWrapper();
		sjsw.setEngine(engine);
		sjsw.setQuery(query);
		sjsw.executeQuery();
		sjsw.getVariables();
		
		while(sjsw.hasNext())
		{
			SesameJenaSelectStatement stmt = sjsw.next();
			output.addElement(stmt.getPropHash());
		}
		// return the output
		return gson.toJson(output);
	}*/	
	
	public static Hashtable getGraphAsJSON(String query, IEngine engine, Hashtable baseFilterHash)
	{
		// the engine will always return a vector
		// the variables are then plugged into a hashtable
		// which is plugged into an vector and then converts the vector into a json
		System.err.println("Called edges routine ");
		Vector output = new Vector();
		SesameJenaSelectCheater sjsw = new SesameJenaSelectCheater();
		sjsw.setEngine(engine);
		sjsw.setQuery(query);
		sjsw.execute();
		//sjsw.getVariables();
//		StringBuilder stringOutput = new StringBuilder();
		//stringOutput.append("[");
		
		Hashtable<String, Hashtable> nodesHash = new Hashtable<String, Hashtable>();
		Hashtable<String, Hashtable> edgesHash = new Hashtable<String, Hashtable>();
		
		while(sjsw.hasNext())
		{
			SesameJenaConstructStatement sct = sjsw.next();
			if(!baseFilterHash.containsKey(sct.getSubject()) && !baseFilterHash.containsKey(sct.getPredicate()) && !baseFilterHash.containsKey(sct.getObject()+""))
			{
				String sub = sct.getSubject();
				String pred = sct.getPredicate() + "";
				String obj = sct.getObject() + "";
				//add subject as node
				Hashtable subjHash = nodesHash.get(sub);
				if(subjHash==null){
					subjHash = new Hashtable();
					subjHash.put("id", sub);
					subjHash.put("label", Utility.getInstanceName(sub));
					nodesHash.put(sub, subjHash);
				}
				//add object as node
				Hashtable objectHash = nodesHash.get(obj);
				if(objectHash==null){
					objectHash = new Hashtable();
					objectHash.put("id", obj);
					objectHash.put("label", Utility.getInstanceName(obj));
					nodesHash.put(obj, objectHash);
				}
				//add pred as edge
				if(!edgesHash.contains(pred)){
					Hashtable edgeHash = new Hashtable();
					edgeHash.put("id", pred);
					edgeHash.put("source", sub);
					edgeHash.put("target", obj);
					edgeHash.put("label", Utility.getInstanceName(pred));
					edgesHash.put(pred, edgeHash);
				}
				// add this guy as source and target
//				stringOutput.append(Utility.getInstanceName(sct.getSubject()+"")+ "," + Utility.getInstanceName(sct.getObject()+"")+ ",1 newline");
				//stringOutput.app
				//output.add("{" + sct.getSubject()+ "," + sct.getObject()+",1}");
			}
		}

		Hashtable returnHash = new Hashtable();
		returnHash.put("nodes", nodesHash);
		returnHash.put("edges", edgesHash.values());
		
		//stringOutput.append("]");
		// return the output
//		return stringOutput.toString();//gson.toJson(output);
		
		System.out.println(returnHash.toString());
		return returnHash;
	}	

	
	public static Object getGraphAsJSON(IEngine engine, Hashtable baseFilterHash)
	{
		// pretty much gets the graph from a given repository connection
		// 
		String predicateSelectQuery = "SELECT DISTINCT ?Subject ?Predicate ?Object WHERE {" +
		  //"VALUES ?Subject {"  + subjects + "}"+
		  //"VALUES ?Object {"  + subjects + "}"+
		  //"VALUES ?Object {"  + objects + "}" +
		  //"VALUES ?Predicate {"  + predicates + "}" +
		  "{?Predicate " +"<http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation>;}" +
		  "{?Subject " + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  " +  " <http://semoss.org/ontologies/Concept>;}" +
		  "{?Object " + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  " +  " <http://semoss.org/ontologies/Concept>;}" +
		  "{?Subject ?Predicate ?Object.}" +
		  "}";
		
		Hashtable retHash = getGraphAsJSON(predicateSelectQuery, engine, baseFilterHash);

		String conceptSelectQuery = "SELECT DISTINCT ?Subject ?Predicate ?Object WHERE {" +
									  //"{?Predicate " +"<http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation>;}" +
									  "{?Subject " + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  " +  " <http://semoss.org/ontologies/Concept>;}" +
//									  "{?Subject ?Predicate ?Object.}" +
									  "BIND(\"\" AS ?Predicate)" + // these are only used so that I can use select cheater...
									  "BIND(\"\" AS ?Object)" +
									  "}";
		
		Hashtable fullNodesHash = getConceptsAsJSON((Hashtable)retHash.get("nodes"), conceptSelectQuery, engine, baseFilterHash);
		retHash.put("nodes", fullNodesHash);
		System.out.println("final graph hashtable: " + retHash.toString());
		return retHash;
	}
	
	public static Hashtable getConceptsAsJSON(Hashtable nodesHash, String query, IEngine engine, Hashtable baseFilterHash){
		System.err.println("Called concept routine ");
		Vector output = new Vector();
		SesameJenaSelectCheater sjsw = new SesameJenaSelectCheater();
		sjsw.setEngine(engine);
		sjsw.setQuery(query);
		sjsw.execute();
		
		while(sjsw.hasNext())
		{
			SesameJenaConstructStatement sct = sjsw.next();
			if(!baseFilterHash.containsKey(sct.getSubject()))
			{
				String sub = sct.getSubject();
				//add subject as node
				Hashtable subjHash = (Hashtable) nodesHash.get(sub);
				if(subjHash==null){
					subjHash = new Hashtable();
					subjHash.put("id", sub);
					subjHash.put("label", Utility.getInstanceName(sub));
					nodesHash.put(sub, subjHash);
				}
			}
		}

		System.out.println(nodesHash.toString());
		return nodesHash;
	}
	
}
