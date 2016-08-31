package prerna.algorithm.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class KMeansModel {
	
	private List<DataPoint> points;
	private List<Cluster> clusters ;
	
	public KMeansModel(Iterator itr, int numClusters, int numIterations){
		points = new ArrayList<>();
		while(itr.hasNext()){
			Object[] row = (Object[]) itr.next();
			
			double[] dim  = new double[row.length-1];
			for(int col=1; col<row.length;col++){
				dim[col-1] = (double)row[col];
			}
			DataPoint point = new DataPoint(row[0].toString(),dim);
			points.add(point);				
		}
		Collections.sort(points);
		clusters = new ArrayList<>(numClusters);
		InitClusters(numClusters);
		Expectation();
		while(numIterations > 0){
			Maximization();
			Expectation();
			numIterations--;
		}
	}
	
	void InitClusters(int numClusters){
		for(int clusterNum = 0; clusterNum < numClusters; clusterNum++){
			Cluster cluster = new Cluster(points.get((points.size()-1) * clusterNum/numClusters));
			clusters.add(cluster);
		}
	}
	
	public Map<Object,Integer> clusterResult(){
		Map<Object, Integer> result = new HashMap<Object,Integer>();
		/*try {
			PrintWriter pw = new PrintWriter(new File("C:\\Users\\shantasingh\\Desktop\\clusterResult.txt"));
			for(DataPoint p : points){
				StringBuilder sb = new StringBuilder();
				for(double d : p.dimensions){
					sb.append(d+",");
				}
				sb.append(p.clusterNum + "\n");
				pw.write(sb.toString());
				result.put(p.id, p.clusterNum);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		for(DataPoint p : points){
			result.put(p.id, p.clusterNum);
		}
		return result;
	}
	
	void Expectation(){
		for(DataPoint p : points){
			p.clusterNum = -1;
			double minDist = Double.MAX_VALUE;
			for(Cluster c : clusters){
				double distance = DataPoint.EucledianDistance(c.centre, p);
				if(distance < minDist){
					minDist = distance;
					p.clusterNum = clusters.indexOf(c);
				}
			}
		}
	}
	
	void Maximization(){
		double[][] sumDataPointDimPerCluster = new double[clusters.size()][points.get(0).dimensions.length];
		int[] numDataPointsPerCluster = new int[clusters.size()];
		for(DataPoint point : points){
			for(int dim=0; dim<point.dimensions.length; dim++){
				sumDataPointDimPerCluster[point.clusterNum][dim] += point.dimensions[dim];
			}
			numDataPointsPerCluster[point.clusterNum]++;
		}
		for(Cluster cluster : clusters){
			int index = clusters.indexOf(cluster);
			double[] newCentreDim = new double[sumDataPointDimPerCluster[index].length];
			for(int dim=0;dim<newCentreDim.length;dim++){
				newCentreDim[dim] = sumDataPointDimPerCluster[index][dim]/numDataPointsPerCluster[index];
			}
			DataPoint newCentre = new DataPoint(null,newCentreDim);
			newCentre.clusterNum = cluster.centre.clusterNum;
			cluster.centre = newCentre;
		}
	}

}

class DataPoint implements Comparable<DataPoint>,Comparator<DataPoint> {
	final String id;
	final double[] dimensions;
	int clusterNum;
	
	public DataPoint(String id, double[] dimensions){
		this.id = id;
		this.dimensions = dimensions;
		this.clusterNum = -1;
	}
	
	public static double EucledianDistance(DataPoint p1, DataPoint p2){
		double sqDist = 0.0;
		for(int dim=0; dim<p1.dimensions.length;dim++){
			sqDist += Math.pow((p1.dimensions[dim] - p2.dimensions[dim]),2);
		}
		return Math.sqrt(sqDist);
	}

	@Override
	public int compare(DataPoint arg0, DataPoint arg1) {
		// TODO Auto-generated method stub
		if(arg0 == null)
			return arg1 == null? 0 : 1;
		if (arg1 == null)
			return -1;
		return arg0.compareTo(arg1);
	}

	@Override
	public int compareTo(DataPoint arg0) {
		return id.compareTo(arg0.id);
	}
}

class Cluster{
	
	DataPoint centre;
	
	public Cluster(DataPoint centre){
		this.centre = centre;
	}
}