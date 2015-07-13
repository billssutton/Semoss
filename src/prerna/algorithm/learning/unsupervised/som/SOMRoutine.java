package prerna.algorithm.learning.unsupervised.som;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import prerna.algorithm.api.IAnalyticRoutine;
import prerna.algorithm.api.ITableDataFrame;
import prerna.algorithm.learning.util.Cluster;
import prerna.algorithm.learning.util.IClusterDistanceMode;
import prerna.algorithm.learning.util.IClusterDistanceMode.DistanceMeasure;
import prerna.ds.BTreeDataFrame;
import prerna.math.SimilarityWeighting;
import prerna.om.SEMOSSParam;
import prerna.util.ArrayUtilityMethods;

public class SOMRoutine implements IAnalyticRoutine {
	
	private static final String INITIAL_RADIUS = "initialRadius";
	private static final String LEARNING_RATE = "learningRate";
	private static final String TAU = "tau";
	private static final String MAXIMUM_ITERATIONS = "maxIterations";
	private static final String INSTANCE_INDEX_KEY = "instanceIndex";
	private static final String GRID_WIDTH = "gridWidth";
	private static final String GRID_LENGTH = "gridLength";
	
	private String somGridID = "";
	private String somHeightID = "";
	private String somXPositionID = "";
	private String somYPositionID = "";
	
	private List<SEMOSSParam> options;
	// values set from options
	private int instanceIndex;
	private double initalRadius;
	private double learningRate;
	private double tau;
	private int maxIterations;
	private Integer gridLength;
	private Integer gridWidth;
	private Map<String, IClusterDistanceMode.DistanceMeasure> distanceMeasure;

	// variables used throughout routine
	private ITableDataFrame dataFrame;
	private String[] attributeNames;
	private boolean[] isNumeric;
	private int numInstances;
	
	private SelfOrganizingMapGrid grid;
	private List<Cluster> gridCenters = new ArrayList<Cluster>();
	private int numGrids;
	
	// results of algorithm
	private Map<Object, Integer> results = new HashMap<Object, Integer>();

	// for calculation of weights
	protected Map<String, Double> numericalWeights = new HashMap<String, Double>();
	protected Map<String, Double> categoricalWeights = new HashMap<String, Double>();
	
	// in order to keep the grid size reasonable
	private int maxInstanceSize = 3000;
	
	public SOMRoutine() {
		this.options = new ArrayList<SEMOSSParam>();

		SEMOSSParam p1 = new SEMOSSParam();
		p1.setName(INSTANCE_INDEX_KEY);
		options.add(0, p1);

		SEMOSSParam p2 = new SEMOSSParam();
		p2.setName(INITIAL_RADIUS);
		options.add(1, p2);
		
		SEMOSSParam p3 = new SEMOSSParam();
		p3.setName(LEARNING_RATE);
		options.add(2, p3);
		
		SEMOSSParam p4 = new SEMOSSParam();
		p4.setName(TAU);
		options.add(3, p4);
		
		SEMOSSParam p5 = new SEMOSSParam();
		p5.setName(MAXIMUM_ITERATIONS);
		options.add(4, p5);
		
		SEMOSSParam p6 = new SEMOSSParam();
		p6.setName(GRID_WIDTH);
		options.add(5, p6);
		
		SEMOSSParam p7 = new SEMOSSParam();
		p7.setName(GRID_LENGTH);
		options.add(6, p7);
	}
	
	@Override
	public ITableDataFrame runAlgorithm(ITableDataFrame... data) {
		//TODO: this is for ease in testing
		this.instanceIndex = 0;
		this.initalRadius = 2.0;
		this.learningRate = 0.07;
		this.tau = 7.5;
		this.maxIterations = 15;
		this.somGridID = "gridID";
		this.somHeightID = "height";
		this.somXPositionID = "x";
		this.somYPositionID = "y";
		
//		this.instanceIndex = (int) options.get(0).getSelected();
//		this.initalRadius = (double) options.get(1).getSelected();
//		this.learningRate = (double) options.get(2).getSelected();
//		this.tau = (double) options.get(3).getSelected();
//		this.maxIterations = (int) options.get(4).getSelected();
//		this.gridWidth = (Integer) options.get(5).getSelected();
//		this.gridLength = (Integer) options.get(6).getSelected();
		
		this.dataFrame = data[0];
		this.attributeNames = this.dataFrame.getColumnHeaders();
		this.isNumeric = this.dataFrame.isNumeric();
		this.numInstances = this.dataFrame.getUniqueInstanceCount(attributeNames[instanceIndex]);
		
		this.grid = new SelfOrganizingMapGrid();
		if(gridLength == null || gridWidth == null || gridLength == 0 || gridWidth == 0) {
			// need to determine optimal height/width for grid
			setGridSize(numInstances);
		} 
		this.numGrids = this.gridLength * this.gridWidth;
		grid.setWidth(gridWidth);
		grid.setLength(gridLength);
		grid.setNumGrids(numGrids);
		
		// set the type of distance measure to be used for each numerical property - default is using mean
		if(this.distanceMeasure == null) {
			distanceMeasure = new HashMap<String, IClusterDistanceMode.DistanceMeasure>();
			for(int i = 0; i < attributeNames.length; i++) {
				if(isNumeric[i]) {
					distanceMeasure.put(attributeNames[i], DistanceMeasure.MEAN);
				}
			}
		} else {
			for(int i = 0; i < attributeNames.length; i++) {
				if(!distanceMeasure.containsKey(attributeNames[i])) {
					distanceMeasure.put(attributeNames[i], DistanceMeasure.MEAN);
				}
			}
		}
				
		calculateWeights();
		
		// initialize the grid
		int idx = 0;
		Iterator<List<Object[]>> it = dataFrame.scaledUniqueIterator(attributeNames[instanceIndex]);
		for(; idx < numGrids; idx++) {
			List<Object[]> instance = it.next();
			// add value to cluster
			Cluster c = new Cluster(categoricalWeights, numericalWeights);
			c.setDistanceMode(distanceMeasure);
			c.addToCluster(instance, attributeNames, isNumeric);
			// add cluster to grid
			gridCenters.add(c);
			// add instance to grid value in results map
			results.put(instance.get(0)[instanceIndex], idx);
		}
		
		int currIt = 0;
		// base radius
		boolean go = true;
		while(currIt < maxIterations && go) {
			System.out.println("Current Iteration: " + currIt);
			go = false;
			// determine radius of influence for this iteration
			double radiusOfInfluence = initalRadius * Math.exp( -1.0 * currIt / tau);
			// determine learning rate for this iteration
			double learningInfluence = learningRate * Math.exp( -1.0 * currIt / tau);
			it = dataFrame.scaledUniqueIterator(attributeNames[instanceIndex]);
			while(it.hasNext()) {
				List<Object[]> instance = it.next();
				Object instanceName = instance.get(0)[instanceIndex];
				// find best grid
				int gridIndex = determineMostSimilarGridForInstance(instance, attributeNames, isNumeric, instanceIndex, gridCenters);
				boolean change = isInstanceChangedGrid(results, instanceName, gridIndex);
				if(change) { // update the results hash and area of effect
					go = true;
					// lower instance count in other cluster
					Integer previousGridIndex = results.get(instanceName);
					if(previousGridIndex != null) {
						int currNumInstances = gridCenters.get(previousGridIndex).getNumInstances();
						gridCenters.get(previousGridIndex).setNumInstances(--currNumInstances);
					}
					results.put(instanceName, gridIndex);
					gridCenters.get(gridIndex).addToCluster(instance, attributeNames, isNumeric);
					
					// modify instance to contain appropriate numerical values when performing area-of-effect changes
					Map<String, Double> numericalChanges = gridCenters.get(gridIndex).getNumericClusterChangesForAllAttributes();
					for(String attirubte : numericalChanges.keySet()) {
						int index = ArrayUtilityMethods.arrayContainsValueAtIndex(attributeNames, attirubte);
						for(int i = 0; i < instance.size(); i++) {
							instance.get(i)[index] = numericalChanges.get(attirubte);
						}
					}
					
					// update the cells surrounding the main cell
					Map<String, List<Integer>> neighborhoodEffectHash = grid.getAdjacentCellsInRadius(gridIndex, radiusOfInfluence);
					List<Integer> adjacentCells = neighborhoodEffectHash.get(SelfOrganizingMapGrid.ADJACENT_CELLS_KEY);
					List<Integer> adjacentCellsRadius = neighborhoodEffectHash.get(SelfOrganizingMapGrid.ADJACENT_CELLS_RADIUS_KEY);
					int adjIdx = 0;
					int adjSize = adjacentCells.size();
					for(; adjIdx < adjSize; adjIdx++) {
						int effected_grid = adjacentCells.get(adjIdx);
						int effect_radius = adjacentCellsRadius.get(adjIdx);
						double adaption_effect = Math.exp( -1.0 * Math.pow(effect_radius, 2) / ( 2 * Math.pow(radiusOfInfluence, 2) ));
						gridCenters.get(effected_grid).addToCluster(instance, attributeNames, isNumeric, learningInfluence * adaption_effect);
					}
				}
			}
			currIt++;
		}
		
		ITableDataFrame returnTable = new BTreeDataFrame(new String[]{attributeNames[instanceIndex], somGridID, somHeightID, somXPositionID, somYPositionID});
		for(Object instance : results.keySet()) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put(attributeNames[instanceIndex], instance);
			int gridNum = results.get(instance);
			row.put(somGridID, gridNum);
			row.put(somHeightID, gridCenters.get(gridNum).getNumInstances());
			int[] coordinates = SelfOrganizingMapGrid.getCoordinatesOfCell(gridNum, this.gridLength);
			row.put(somXPositionID, coordinates[0]);
			row.put(somYPositionID, coordinates[1]);
			returnTable.addRow(row, row);
		}
		
		return returnTable;
	}
	
	private int determineMostSimilarGridForInstance(List<Object[]> instanceValues, String[] attributeNames, boolean[] isNumeric, int instanceIndex, List<Cluster> clusters) {
		int i = 0;
		
		double largestSimilarity = -1;
		int bestGrid = -1;
		for(; i < numGrids; i++) {
			double similarity = clusters.get(i).getSimilarityForInstance(instanceValues, attributeNames, isNumeric, instanceIndex);
			if(similarity > largestSimilarity) {
				bestGrid = i;
				largestSimilarity = similarity;
			}
			if(similarity == 1.0) {
				break;
			}
		}
		return bestGrid;
	}
	
	public boolean isInstanceChangedGrid(Map<Object, Integer> results, Object instanceName, int bestCluster) {
		if(results.containsKey(instanceName)) {
			if(results.get(instanceName) == bestCluster) {
				return false;
			} else {
				return true;
			}
		}
		return true;
	}
	
	//TODO: same method exists in clustering -- need to push into util. class
	public void calculateWeights() {
		int i = 0;
		int size = attributeNames.length;
		String instanceType = attributeNames[instanceIndex];
		
		List<Double> numericalEntropy = new ArrayList<Double>();
		List<String> numericalNames = new ArrayList<String>();
		
		List<Double> categoricalEntropy = new ArrayList<Double>();
		List<String> categoricalNames = new ArrayList<String>();
		
		for(; i < size; i++) {
			String attribute = attributeNames[i];
			if(attribute.equals(instanceType)) {
				continue;
			}
			if(isNumeric[i]) {
				numericalNames.add(attribute);
				numericalEntropy.add(dataFrame.getEntropyDensity(attribute));
			} else {
				categoricalNames.add(attribute);
				categoricalEntropy.add(dataFrame.getEntropyDensity(attribute));
			}
		}
		
		if(!numericalEntropy.isEmpty()){
			double[] numericalWeightsArr = SimilarityWeighting.generateWeighting(numericalEntropy.toArray(new Double[0]));
			i = 0;
			int numNumeric = numericalNames.size();
			for(; i < numNumeric; i++) {
				numericalWeights.put(numericalNames.get(i), numericalWeightsArr[i]);
			}
		}
		if(!categoricalEntropy.isEmpty()){
			double[] categoricalWeightsArr = SimilarityWeighting.generateWeighting(categoricalEntropy.toArray(new Double[0]));
			i = 0;
			int numCategorical = categoricalNames.size();
			for(; i < numCategorical; i++) {
				categoricalWeights.put(categoricalNames.get(i), categoricalWeightsArr[i]);
			}
		}
	}
	
	private void setGridSize(int numInstances) {
		int size = numInstances;
		if(size > maxInstanceSize) {
			size = maxInstanceSize;
		}
		double x = Math.sqrt((double) size / (6*5));
		this.gridLength = (int) Math.round(2*x);
		this.gridWidth = (int) Math.round(3*x);
	}
	
	@Override
	public String getName() {
		return "Self Organizing Map";
	}

	@Override
	public String getResultDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSelectedOptions(Map<String, Object> selected) {
		Set<String> keySet = selected.keySet();
		for(String key : keySet) {
			for(SEMOSSParam param : options) {
				if(param.getName().equals(key)){
					param.setSelected(selected.get(key));
					if(param.getName().equals(INSTANCE_INDEX_KEY)) {
						String select = selected.get(key).toString().toUpperCase();
						this.somGridID = select + "_SOM_GRID_NUM";
						this.somHeightID = select + "_SOM_GRID_HEIGHT";
						this.somXPositionID = select + "_SOM_X_POSITION";
						this.somYPositionID = select + "_SOM_Y_POSITION";
					}
					break;
				}
			}
		}
	}

	@Override
	public List<SEMOSSParam> getOptions() {
		return this.options;
	}

	@Override
	public String getDefaultViz() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getChangedColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getResultMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

}
