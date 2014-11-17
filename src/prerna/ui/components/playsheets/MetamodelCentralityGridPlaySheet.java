package prerna.ui.components.playsheets;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.impl.CentralityCalculator;
import prerna.algorithm.impl.PageRankCalculator;
import prerna.om.SEMOSSEdge;
import prerna.om.SEMOSSVertex;
import prerna.rdf.engine.impl.AbstractEngine;
import prerna.util.Constants;
import edu.uci.ics.jung.graph.DelegateForest;

@SuppressWarnings("serial")
public class MetamodelCentralityGridPlaySheet extends GridPlaySheet {

	private static final Logger logger = LogManager.getLogger(MetamodelCentralityGridPlaySheet.class.getName());
		
	@Override
	public void createData() {
		GraphPlaySheet graphPS = CentralityCalculator.createMetamodel(((AbstractEngine)engine).getBaseDataEngine().getRC(), query);

		Hashtable<String, SEMOSSVertex> vertStore  = graphPS.getGraphData().getVertStore();

		names = new String[]{"Type","Undirected Closeness Centrality","Undirected Betweeness Centrality","Undirected Eccentricity Centrality","Undirected Page Rank"};

		list = new ArrayList<Object[]>();
		
		Hashtable<SEMOSSVertex, Double> unDirCloseness = CentralityCalculator.calculateCloseness(vertStore, false);
		Hashtable<SEMOSSVertex, Double> unDirBetweenness = CentralityCalculator.calculateBetweenness(vertStore, false);
		Hashtable<SEMOSSVertex, Double> unDirEccentricity = CentralityCalculator.calculateEccentricity(vertStore,false);
		
		DelegateForest<SEMOSSVertex,SEMOSSEdge> forest = CentralityCalculator.makeForestUndirected(graphPS.getGraphData().getEdgeStore(), graphPS.forest);
		PageRankCalculator pCalc = new PageRankCalculator();
		Hashtable<SEMOSSVertex, Double> ranks = pCalc.calculatePageRank(forest);
		
		Hashtable<SEMOSSVertex, Double> ranksTimesNodes = new Hashtable<SEMOSSVertex, Double>();
		for(SEMOSSVertex vert : ranks.keySet()) {
			ranksTimesNodes.put(vert, ranks.get(vert)*ranks.keySet().size());
		}
		
		for(String node : vertStore.keySet()) {
			SEMOSSVertex vert = vertStore.get(node);
			String type = (String)vert.propHash.get(Constants.VERTEX_NAME);
			Object[] row = new Object[5];
			row[0] = type;
			row[1] = unDirCloseness.get(vert);
			row[2] = unDirBetweenness.get(vert);
			row[3] = unDirEccentricity.get(vert);
			row[4] = ranksTimesNodes.get(vert);
			list.add(row);
		}
	}
}
