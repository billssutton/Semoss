package prerna.ds.H2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import prerna.ds.QueryStruct;
import prerna.ds.H2.H2Builder.Comparator;
import prerna.om.Dashboard;
import prerna.om.Insight;
import prerna.rdf.query.builder.SQLInterpreter;

public class H2Joiner {

	private static int counter = 0;
	private static String tableNamePrefix = "VIEWTABLE94328"; //something random...hopefully no one else makes a table named this
	
	//viewTable -> QueryStruct
	//viewTable is name of the view
	private Map<String, QueryStruct> qsMap = new HashMap<>();
		
	// this keeps the column aliases
	// contains {frame table name -> {colName -> colAliasToUse} }
	// e.g. {H2Frame -> {Title -> H2Frame_Title}}
	private Hashtable<String, Hashtable<String, String>> colAlias = new Hashtable<String, Hashtable<String, String>>();
	
	//this keeps a filterhash for each view table in the dashboard
	//viewTable -> filterhash (for details on the structure see H2Builder.java)
	private Hashtable<String, Map<String, Map<Comparator, Set<Object>>>> filterHashTable = new Hashtable<>();
	
	//pointer to the dashboard that is using this joiner
	private Dashboard dashboard;
	
	public H2Joiner() {
		qsMap = new HashMap<>();
	}
	
	public H2Joiner(Dashboard dashboard) {
		this.dashboard = dashboard;
		qsMap = new HashMap<>();
	}
	
	/**
	 * 
	 * @param frame
	 * @param viewTable
	 */
	public void refreshView(H2Frame frame, String viewTable) {
		try {
			String buildViewQuery = buildView(viewTable);
			frame.builder.runExternalQuery(buildViewQuery);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param tableName			name of the view to get filters for
	 * @return					a merge of all the filter data from the frames associated with the tableName (view) parameter
	 */
	protected Map<String, Map<Comparator, Set<Object>>> getJoinedFilterHash(String tableName) {
		if(this.filterHashTable.containsKey(tableName)) {
			return this.filterHashTable.get(tableName);
		} else {
			Map<String, Map<Comparator, Set<Object>>> newFilterHash = new HashMap<>();
			this.filterHashTable.put(tableName, newFilterHash);
			return newFilterHash;
		}
	}
	
	private List<H2Frame> getJoinedFrames(String tableName) {
		List<Insight> insights = dashboard.getInsights(tableName);
		
		List<H2Frame> frames = new ArrayList<>();
		for(Insight insight : insights) {
			frames.add((H2Frame)insight.getDataMaker());
		}
		return frames;
	}
	
	private String buildView(String viewTable) {
		QueryStruct qs = qsMap.get(viewTable);
		SQLInterpreter interpreter = new SQLInterpreter();
		interpreter.setQueryStruct(qs);
		interpreter.setColAlias(this.colAlias);
		String query = interpreter.composeQuery();
		
		query = "CREATE OR REPLACE VIEW "+viewTable+" AS ("+query+")";
		return query;
	}
	
	/**
	 * 
	 * @return		a new unique name that can be used for a view
	 */
	private static String getNewTableName() {
		return tableNamePrefix+counter++;
	}
	
	public void updateDataId(String viewTableName) {
		List<H2Frame> frames = getJoinedFrames(viewTableName);
		
		if(frames != null) {
			for(H2Frame frame : frames) {
				frame.updateDataId(1);
			}
		}
	}
	
	
	/**************************************** FOR MULTI COLUMN -- DEVELOP THIS GOING FORWARD *************************/
	
	/**
	 * 
	 * @param joinCols
	 * @param frames
	 * 
	 * use this for new multi column join
	 */
	public String joinFrames(List<List<String>> joinCols, H2Frame[] frames) {
		List<String[]> newjoinCols = convertJoinColsToJoinVals(frames, joinCols);
		
		// we need to determine if one of the h2frames is not in-memory
		// once we find one that is not in-mem, we will shift all of them to be in that schema
		// which schema is chosen if more than one is not in-mem doesn't matter
		String physicalSchema = null;
		for(H2Frame frame : frames) {
			if(!frame.isInMem()) {
				physicalSchema = frame.getSchema();
				break;
			}
		}
		
		// if physical schema is set
		// now loop through and make sure there are all in the same location
		if(physicalSchema != null) {
			for(H2Frame frame : frames) {
				if(!frame.getSchema().equals(physicalSchema)) {
					frame.convertToOnDiskFrame(physicalSchema);
				}
			}
		}
		
		if(frames.length < 2) {
			throw new IllegalArgumentException("Must Pass in at least 2 Frames!!");
		}
		else if(frames.length == 2) {
			return joinFrames(frames[0], frames[1], newjoinCols);
		}

		for(H2Frame frame : frames) {
			if(frame.isJoined()) {
				throw new IllegalArgumentException("Frame already joined");
			}
		}
		
		String retTable = "";
		for(int i = 1; i < frames.length; i++) {
			
			List<String[]> nextNewJoinCols = new ArrayList<>(2);
			for(int index = 0; index < newjoinCols.size(); index++) {
				String[] cols = newjoinCols.get(index);
				String[] nextCols = new String[]{cols[i-1], cols[i]};
				nextNewJoinCols.add(nextCols);
			}
			
			retTable = joinFrames(frames[i-1], frames[i], nextNewJoinCols);
		}
		return retTable;
	}
	
	/**
	 * 
	 * @param frame1
	 * @param frame2
	 * @param joinCols
	 */
	public String joinFrames(H2Frame frame1, H2Frame frame2, List<String[]> joinCols) {
		//make sure both frames have the same schema
		
		//if both frames are already joined, throw error
		boolean frame1joined = frame1.isJoined();
		boolean frame2joined = frame2.isJoined();
		
		if(frame1joined && frame2joined) {
			throw new IllegalArgumentException("Both Frames are already joined");
		}
		
		String firstConcept = "";
		String secondConcept = "";
		String firstJoinCol = "";
		String secondJoinCol = "";
		String relation = "inner.join";
		
		for(int i = 0; i < joinCols.size(); i++) {
			
			String[] nextJoinCols = joinCols.get(i);
			String firstFrameTable = frame1.builder.getTableName();
			firstJoinCol = nextJoinCols[0];
			firstConcept = firstFrameTable + "__"+firstJoinCol;
			
			String secondFrameTable = frame2.builder.getTableName();
			secondJoinCol = nextJoinCols[1];
			secondConcept = secondFrameTable + "__"+secondJoinCol;
			
			break; //not doing multi-column join for now...need to update sql interpreter for that
		}
		
		String viewTable = null;
		if(frame1joined) {
			//join frame2 to frame1
			try {
				//get table name from frame1
				viewTable = frame1.builder.getViewTableName();
				
				//get the query struct frame1 is a part of
				QueryStruct qs = this.qsMap.get(viewTable);
				
				//add the new relation to that query struct
				qs.addRelation(firstConcept, secondConcept, relation);
				
				//get the colAlias for the already joined frame, assign the new frame's column to have the same alias
				String frame1Table = frame1.builder.getTableName();
				String firstColAlias = colAlias.get(frame1Table).get(firstJoinCol);
				String frame2Table = frame2.builder.getTableName();
				Hashtable<String, String> newAliasMap = new Hashtable<>();
				newAliasMap.put(secondJoinCol, firstColAlias);
				this.colAlias.put(frame2Table, newAliasMap);
				
				String[] frame2Headers = getValuesHeadersFromFrame(frame2);
				
				//add all the columns from the second frame as selectors except join cols
				for(String header : frame2Headers) {
					if(!header.equals(secondJoinCol)) {
						qs.addSelector(frame2Table, header);
						newAliasMap.put(header, frame2Table+"_"+header);
					}
				}
				
				frame2.setJoin(viewTable);
				frame2.builder.setTranslationMap(newAliasMap);
				frame2.builder.setJoiner(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(frame2joined) {
			//join frame 1 to frame 2
			try {
				viewTable = frame2.builder.getViewTableName();
				QueryStruct qs = this.qsMap.get(viewTable);
				qs.addRelation(firstConcept, secondConcept, relation);
				
				String frame2Table = frame2.builder.getTableName();
				String secondColAlias = colAlias.get(frame2Table).get(secondJoinCol);
				String frame1Table = frame1.builder.getTableName();
				Hashtable<String, String> newAliasMap = new Hashtable<>();
				newAliasMap.put(secondJoinCol, secondColAlias);
				this.colAlias.put(frame1Table, newAliasMap);
				
				
				String[] frame1Headers = getValuesHeadersFromFrame(frame1);
				
				//add all the columns from the first frame as selectors except join cols
				for(String header : frame1Headers) {
					if(!header.equals(firstJoinCol)) {
						qs.addSelector(frame1Table, header);
						newAliasMap.put(header, frame1Table+"_"+header);
					}
				}		
				
				frame1.setJoin(viewTable);
				frame1.builder.setTranslationMap(newAliasMap);
				frame1.builder.setJoiner(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else {
			viewTable = getNewTableName();
			QueryStruct qs = buildNewQueryStruct(new H2Frame[]{frame1, frame2}, joinCols);
			
			String frame1Table = frame1.builder.getTableName();
			String frame2Table = frame2.builder.getTableName();
			
			String newAlias = frame1Table+"_"+firstJoinCol;
			
			Hashtable<String, String> newAliasMap1 = new Hashtable<>();
			Hashtable<String, String> newAliasMap2 = new Hashtable<>();
			newAliasMap1.put(firstJoinCol, newAlias);
			newAliasMap2.put(secondJoinCol, newAlias);
			
			this.colAlias.put(frame1Table, newAliasMap1);
			this.colAlias.put(frame2Table, newAliasMap2);
			
			String[] frame1Headers = getValuesHeadersFromFrame(frame1);
			String[] frame2Headers = getValuesHeadersFromFrame(frame2);
			
			//add all the columns from the first frame as selectors
			for(String header : frame1Headers) {
				qs.addSelector(frame1Table, header);
				newAliasMap1.put(header, frame1Table+"_"+header);
			}
			
			//add all the columns except join columns from the second frame
			for(String header : frame2Headers) {
				if(!header.equals(secondJoinCol)) {
					qs.addSelector(frame2Table, header);
					newAliasMap2.put(header, frame2Table+"_"+header);
				}
			}
			
			frame1.setJoin(viewTable);
			frame1.builder.setTranslationMap(newAliasMap1);
			frame1.builder.setJoiner(this);
			
			frame2.setJoin(viewTable);
			frame2.builder.setTranslationMap(newAliasMap2);
			frame2.builder.setJoiner(this);
			this.qsMap.put(viewTable, qs);
		}
		
		refreshView(frame1, viewTable);
		return viewTable;
	}
	
	public void addHeaders(H2Builder builder, List<String> newHeaders) {
		if(builder.getJoinMode()) {
			String viewTableName = builder.getViewTableName();
			String tableName = builder.getTableName();
			
			QueryStruct qs = this.qsMap.get(viewTableName);
			Hashtable<String, String> aliasMap = this.colAlias.get(tableName);
			for(String header : newHeaders) {
				qs.addSelector(tableName, header);
				aliasMap.put(header, tableName+"_"+header);
			}
			builder.setTranslationMap(aliasMap);
		}
	}
	
	//Currently does not account for multi column join
	private QueryStruct buildNewQueryStruct(H2Frame[] frames, List<String[]> joinCols) {
		QueryStruct qs = new QueryStruct();
		for(int i = 0; i < joinCols.size(); i++) {
			
			String[] nextJoinCols = joinCols.get(i);
			String firstFrameTable = frames[0].builder.getTableName();
			String fromConcept = firstFrameTable + "__"+nextJoinCols[0];
			
			for(int j = 1; j < frames.length; j++) {	
				String toConcept = frames[j].builder.getTableName()+"__"+nextJoinCols[j];
				qs.addRelation(fromConcept, toConcept, "inner.join");
			}
			break; //not doing multi-column join for now...need to update sql interpreter for that
		}
		return qs;
	}
	
	//convert the join columns to use the headers of the actual h2 table
	private List<String[]> convertJoinColsToJoinVals(H2Frame[] frames, List<List<String>> joinCols) {
		List<String[]> joinVals = new ArrayList<>(joinCols.size());
		
		for(List<String> nextJoinCols : joinCols) {
			String[] nextJoinVals = new String[nextJoinCols.size()];
			for(int i = 0; i < nextJoinCols.size(); i++) {
				nextJoinVals[i] = frames[i].getValueForUniqueName(nextJoinCols.get(i));
			}
			joinVals.add(nextJoinVals); 
		}
		
		return joinVals;
	}
	
	//use this method to grab the h2 headers from the frame, we need that since we are querying the table
	private String[] getValuesHeadersFromFrame(H2Frame frame) {
		
		String[] columnHeaders = frame.getColumnHeaders();
		String[] valueHeaders = new String[columnHeaders.length];
		for(int i = 0; i < columnHeaders.length; i++) {
			valueHeaders[i] = frame.getValueForUniqueName(columnHeaders[i]);
		}
		return valueHeaders;
	}

	
	/**
	 * 
	 * @param frames		This method unjoins a list of frames
	 * 							if last frame is for a view is unjoined, the view table will also be dropped
	 * 							if the second to last frame for a view, that frame will also be unjoined
	 * 							
	 * 							Any affected views with remaning frames will be refreshed to be reflective of the loss of data 
	 * 			
	 */
	public void unJoinFrame(H2Frame... frames) {
		for(H2Frame frame : frames) {
			frame.unJoin();
		}
	}
	
	public void dropView(String viewTable, H2Frame frame) {
		try {
			frame.builder.runExternalQuery("DROP VIEW "+viewTable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
