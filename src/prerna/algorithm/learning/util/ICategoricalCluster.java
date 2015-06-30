package prerna.algorithm.learning.util;

import java.util.List;

public interface ICategoricalCluster {

	/**
	 * Update when adding to the cluster for the given attribute and value
	 * @param attributeName					The name of the attribute
	 * @param attributeInstance				The name of the instance
	 * @param value							The value of the instance
	 */
	void addToCluster(String attributeName, String attributeInstance, Double value);
	
	/**
	 * 
	 * @param attributeNames
	 * @param attributeInstances
	 * @param values
	 */
	void addToCluster(List<String> attributeNames, List<String> attributeInstances, List<Double> values);
	
	/**
	 * Update when removing from the cluster for the given attribute and value
	 * @param attributeName					The name of the attribute
 	 * @param attributeInstance				The name of the instance
	 * @param value							The value of the attribute
	 */
	void removeFromCluster(String attributeName, String attributeInstance, Double value);
	
	/**
	 * 
	 * @param attributeNames
	 * @param attributeInstances
	 * @param values
	 */
	void removeFromCluster(List<String> attributeNames, List<String> attributeInstances, List<Double> values);
	
	/**
	 * 
	 * @param attributeName
	 * @param attributeInstance
	 * @param value
	 */
	Double getSimilarity(String attributeName, String attributeInstance);
	
//	/**
//	 * 
//	 * @param attributeNames
//	 * @param attributeInstances
//	 */
//	Double getSimilarity(List<String> attributeNames, List<String> attributeInstances);
	
	/**
	 * 
	 * @param attributeNames
	 * @param attributeInstances
	 * @param indexToSkip
	 * @return
	 */
	Double getSimilarity(List<String> attributeNames, List<String> attributeInstances, int indexToSkip);
	
	/**
	 * Determine if the cluster is empty
	 * @return								boolean true if object is empty, false otherwise
	 */
	boolean isEmpty();

	/**
	 * Resets the clusters
	 */
	void reset();
//	/**
//	 * 
//	 * @param categoricalWeights
//	 */
//	void setWeights(Map<String, Double> categoricalWeights);
	
}
