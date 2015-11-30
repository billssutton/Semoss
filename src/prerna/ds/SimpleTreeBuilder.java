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

package prerna.ds;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.util.ArrayUtilityMethods;

public class SimpleTreeBuilder
{
	// core set of issues I am trying to solve
	// the ability to add columns on the fly - Complete
	// ability to remove columns - Complete
	// ability to flatten it at any point and generate a table - Complete
	// Ability to add a single node  - Complete
	// ability to add columns with link to any of the existing columns - Complete
	// ability to remove a single node - Complete
	// Balance the tree automatically - need to see where to hook it in
	// stop flattening at an individual point - Done
	
	// 2 adjacent nodes from the metamodel are selected
	// The information is sent
	// Out of this, the key is from the existing node
	// The vector of all the SimpleTreeNodes are created based on where they are 
	// based on the nodes - we reach every leaf of this node then add the value node from the incoming hashtable to it
	// when a node is deleted, it is demarcated on the binary tree that this has been filtered out
	// with all the children of this node being deleted
	
	// What happens if I go with basically adding the node only to the last one
	// in this case, I dont need to worry about cloning the node multiple times
	// I need to check this out
	// and everytime there is a new one coming in
	// I need to flatten the existing structure and add to that one again
	// which means I need a way to fast search it
	// this is not difficult
	// I just need to keep the index and then add to it
	// so instead of adding a child
	// I just add to that node directly
	
	// distributed
	// What I am trying to do is
	// spray the nodes
	// so that I can load the data on a different machine if need be
	// any node can be a network node
	// SimpleNetworkNode extends SimpleTreeNode
	// TreeNetworkNode extends TreeNode
	// 
	
	private static final Logger LOGGER = LogManager.getLogger(SimpleTreeBuilder.class.getName());
	
	// actual value tree root
	private SimpleTreeNode lastAddedNode = null;
	private SimpleTreeNode filteredRoot;
	
	
	private String rootLevel;
	// see if the child can be a single node
	// this means you can only add to the last child
	// nothing before it
	boolean singleNode = false;
	// type to the root node of the type
	// type is expresed as string
	Hashtable <String, TreeNode> nodeIndexHash = new Hashtable <String, TreeNode>();
	//Hashtable <String, Integer> typeToLevel = new Hashtable <String, Integer>();
	Hashtable <String, Hashtable> nodeTypeFilters = new Hashtable <String, Hashtable>();
	
	// hosting everything else for the thread runner
	//***************************
	ISEMOSSNode [] seeds = null;
	ILinkeableEngine [] engines = null;
	String [] childs = null;
	TreePhaser phaser = null;
	ArrayList threadList = null;
	int runLevel = 0;
	ExecutorService service = null;
	String finalChildType = null;
	//****************************
//	public SimpleTreeBuilder() {
//		
//	}

	public SimpleTreeBuilder(String rootLevel) {
		this.rootLevel = rootLevel;
	}
	
	public void adjustType(String type, boolean recurse)
	{
		// the point here is to ensure that the type is being adjusted so that the levels are taken care of
		// i.e. say if you extend one side of the tree, but however, the other side is really empty
		// you want to extend this in a balanced manner
		// consider Lab - CP / AP
		// now I extend CP, however AP is empty what this basically means is 
		// when I actually extend CP further or delete a node, there is a asymmetric tree growth which I am trying to avoid
		
		// we start of by picking all the instances for this type
		// then we methodically look to see if any of them have a left child
		// if they do
		// we then pick that type and create an empty node and attach it to those that do not have a children
		// the only word of caution is at a later point
		// when we want to add a node of the same type - we will need to ensure that the left child which is empty has been taken care of
		// I am not sure if we need to have to add this to TreeNode i.e. there is not a need to index this
		// actually I do, otherwise, it wont work.. :(
		
		// we start of by picking all the instances for this type
		TreeNode rootNodeForType = nodeIndexHash.get(type);
		if(rootNodeForType != null)
		{
			Vector <SimpleTreeNode> typeInstances = new Vector<SimpleTreeNode>();
			// will need to adjust these types but also need them to be filtered
			Vector <SimpleTreeNode> filteredTypeInstances = new Vector<SimpleTreeNode>();
			IndexTreeIterator it = new IndexTreeIterator(rootNodeForType);
			while(it.hasNext()) {
				TreeNode n = it.next();
				typeInstances.addAll(n.instanceNode);
				filteredTypeInstances.addAll(n.filteredInstanceNode);
			}
			
			// if both lists are empty, return
			if(typeInstances.isEmpty() && filteredTypeInstances.isEmpty()) {
				return;
			}
			
			// then we methodically look to see if any of them have a left child 
			// if they do we then pick that type and create an empty node and attach it to those that do not have a children
			SimpleTreeNode child = null;
			for(int instanceIndex = 0; instanceIndex < typeInstances.size() && (child == null ); instanceIndex++) {
				child = typeInstances.elementAt(instanceIndex).leftChild;
			}
			for(int instanceIndex = 0; instanceIndex < filteredTypeInstances.size() && (child == null ); instanceIndex++) {
				child = filteredTypeInstances.elementAt(instanceIndex).leftChild;
			}
			if(child == null) { // nothing to do here 
				return; 
			}
			// the only word of caution is at a later point
			// when we want to add a node of the same type - we will need to ensure that the left child which is empty has been taken care of
			String childType = ((ISEMOSSNode)child.leaf).getType();
			
			// push similar processing to external method
			// method takes in the list of values and will append empty nodes based on if the instance is filtered or not filtered
			boolean newInstanceNode = adjustTypeProcessing(typeInstances, childType, false);
			boolean newFilteredNode = adjustTypeProcessing(filteredTypeInstances, childType, true);

			if(recurse && (newInstanceNode || newFilteredNode)) {
				adjustType(childType, recurse);
			}
		}
	}
	
	/**
	 * Used to process the list of simple tree nodes to adjust the type
	 * @param simpleTreeNodeList				The list of simple tree nodes
	 * @param childType							The type of the empty node to append for adjustment
	 * @param filteredValues					boolean if the empty nodes being created should be filtered
	 * @return									boolean if a new node was created (to determine recursion in adjustType method)
	 */
	private boolean adjustTypeProcessing(Vector<SimpleTreeNode> simpleTreeNodeList, String childType, boolean filteredValues) {
		// boolean if a new node was created
		boolean newNode = false;
		// loop through all simple tree nodes in list
		for(int instanceIndex = 0; instanceIndex < simpleTreeNodeList.size(); instanceIndex++)
		{
			// if their is no child, add an empty node
			if(simpleTreeNodeList.elementAt(instanceIndex).leftChild == null)
			{
				// create an empty node
				StringClass dummySEMOSSNode = new StringClass(SimpleTreeNode.EMPTY, SimpleTreeNode.EMPTY, childType);
				// if no empty node exists in the index tree, create a new one
				TreeNode dummyIndexNode = getNode(dummySEMOSSNode);
				if(dummyIndexNode == null) {
					dummyIndexNode = createNode(dummySEMOSSNode, true);
					// do the create routine here
					TreeNode root = nodeIndexHash.get(childType);
					root = root.insertData(dummyIndexNode);
					nodeIndexHash.put(childType, root);
					// adding to nodeIndexHash automatically adds to unfilterd values
					// set to filtered values if necessary
					if(filteredValues) {
						dummyIndexNode.filteredInstanceNode = dummyIndexNode.instanceNode;
						dummyIndexNode.instanceNode = new Vector<SimpleTreeNode>();
					}
				} else {
					// found an empty node, add to its filtered list of non-filtered list
					SimpleTreeNode instanceNode = new SimpleTreeNode(dummySEMOSSNode);
					if(filteredValues) {
						dummyIndexNode.addFilteredInstance(instanceNode);
					} else {
						dummyIndexNode.addInstance(instanceNode);
					}
				}
				// add the empty node as a child to the simple tree node
				if(filteredValues) {
					simpleTreeNodeList.elementAt(instanceIndex).addChild(dummyIndexNode.filteredInstanceNode.lastElement());
				} else {
					simpleTreeNodeList.elementAt(instanceIndex).addChild(dummyIndexNode.instanceNode.lastElement());
				}
				newNode = true;
			}
		}
		
		return newNode;
	}
	
	public void balanceLevel(String level) {
		TreeNode rootNode = nodeIndexHash.get(level);
		if(rootNode != null)
		{
			String parentLevel = ((ISEMOSSNode)rootNode.instanceNode.firstElement().parent.leaf).getType();
			Vector nodeGetter = new Vector();
			
			TreeNode parent = nodeIndexHash.get(parentLevel);
			nodeGetter.addElement(parent);
			Vector <SimpleTreeNode> typeInstances = parent.getInstanceNodes(nodeGetter, new Vector<SimpleTreeNode>());

			for(int i = 0; i < typeInstances.size(); i++) {
				SimpleTreeNode parentNode = typeInstances.get(i);
				
				if(parentNode.leftChild==null) {
					
				}
				StringClass dummySEMOSSNode = new StringClass(SimpleTreeNode.EMPTY, level);
				TreeNode dummyIndexNode = getNode(dummySEMOSSNode);
				if(dummyIndexNode == null) {
					dummyIndexNode = createNode(dummySEMOSSNode, true);
					TreeNode root = nodeIndexHash.get(level);
					root = root.insertData(dummyIndexNode);
					nodeIndexHash.put(level, root);
				}

				else
				{
					SimpleTreeNode instanceNode = new SimpleTreeNode(dummySEMOSSNode);
					parentNode.leftChild = instanceNode;
					instanceNode.parent = parentNode;
					dummyIndexNode.addInstance(instanceNode);
				}
			}
		}
	}

	
	// I need to introduce a routine here for multi insert
	public TreeNode createNode(ISEMOSSNode node, boolean child)
	{
		TreeNode retNode = null;
		TreeNode typeIndexRoot = nodeIndexHash.get(node.getType());
		boolean newNode = false;
		boolean hasChild = false;
		retNode = getNode(node);
		if(retNode != null)
			hasChild = hasChild(retNode);
		if(retNode == null)
		{
			retNode = new TreeNode(node);
			newNode = true;
		}
		if(newNode || (child && !hasChild && !singleNode))
		{
			//System.err.println(node.getKey()+ "    Has child is " + hasChild + "   newNode" + newNode);
			//retNode = new TreeNode(node);
			SimpleTreeNode instanceNode = new SimpleTreeNode(node);
			//System.err.println("Adding instance " + node.getKey());
			retNode.addInstance(instanceNode);
			// add it as a sibling
			// if the child is false
			// and the type index root is not null
			// and the instances do not have a parent
			// add this guy as a sibling
			if(newNode && !child &&  typeIndexRoot != null && typeIndexRoot.getInstances() != null && typeIndexRoot.getInstances().elementAt(0).parent == null)
			{
				SimpleTreeNode rightMostNode = typeIndexRoot.getInstances().elementAt(0);
				//rightMostNode = SimpleTreeNode.getRight(rightMostNode);
				SimpleTreeNode rightSibling = rightMostNode.rightSibling;
				if(rightSibling != null) {
					rightSibling.leftSibling = instanceNode;
					instanceNode.rightSibling = rightSibling;
				}
				rightMostNode.rightSibling = instanceNode;
				instanceNode.leftSibling = rightMostNode;
			}
			
			//if it is a new node and it is not a child and nodeIndexHash size is 0
			if(newNode && !child && nodeIndexHash.keySet().size()==0) {
				finalChildType = node.getType();
			}
		}
		addToNodeIndex(node.getType(), retNode);
		return retNode;
	}
	
	public boolean hasChild(TreeNode node)
	{
		boolean hasChild = false;
		Vector <SimpleTreeNode> instances = node.getInstances();
		for(int instanceIndex = 0;instanceIndex < instances.size()&& !hasChild ;instanceIndex++)
			hasChild = (instances.get(instanceIndex).leftChild != null);
		return hasChild;
	}

	
//********************* ADDITION METHODS ****************************//
	
	// this is for adding more than one pair of nodes at a time
	// the node array can be thought of as a row in a table--one node for each type
	// this assumes that the node array passed in spans the whole width of the table--every type gets a node
	public synchronized SimpleTreeNode addNodeArray(ISEMOSSNode... nodeArray)
	{
		SimpleTreeNode leafNode = null;
		// this case is when data needs to be added to every node type and the full array of nodes is important
		// I cannot consider each pair in the array individually as once I get to the second pair, I lose it's relationship to the first node
		// need to keep track of the value nodes I create as I go through the array to only append to the value node I care about
		ISEMOSSNode finalNode = nodeArray[nodeArray.length-1];
		if(finalChildType == null || !nodeIndexHash.containsKey(finalNode.getType()))
			finalChildType = finalNode.getType();		

		SimpleTreeNode parentInstanceNode = createNode(nodeArray[0], false).getInstances().lastElement();
		for(int nodeIdx = 0; nodeIdx < nodeArray.length - 1; nodeIdx++){
			// check if our relationship already exists -- only need to check the last element because we are adding a continuous row so only care about the instance we just added (or got)
			ISEMOSSNode childSEMOSSNode = nodeArray[nodeIdx+1];
			boolean childExists = parentInstanceNode.hasChild(childSEMOSSNode);
			if (childExists){
				//how do i get this child instance node???
				TreeNode childIndexNode = getNode(childSEMOSSNode);
				List<SimpleTreeNode> childInstances = childIndexNode.getInstances();
				boolean foundNode = false;
				for(int childIdx = childInstances.size() - 1; childIdx >=0 && !foundNode; childIdx -- ){ // start from the back because its likely to be the last one
					SimpleTreeNode childInst = childInstances.get(childIdx);
					if(childInst.parent.equal(parentInstanceNode)){
						parentInstanceNode.incrementCount(childInst.leaf);// increment this guy
						parentInstanceNode = childInst;
						foundNode = true;
					}
				}
				continue;
			}
			
			// if relationship doesn't exist, need to create a new child instance
			TreeNode retNode = getNode(childSEMOSSNode);
			if(retNode == null)
			{
				retNode = new TreeNode(childSEMOSSNode);
			}
			SimpleTreeNode childInstanceNode = new SimpleTreeNode(childSEMOSSNode);
			leafNode = childInstanceNode;
			retNode.addInstance(childInstanceNode);
			
			SimpleTreeNode.addLeafChild(parentInstanceNode, childInstanceNode);
			addToNodeIndex(childSEMOSSNode.getType(), retNode);

			lastAddedNode = parentInstanceNode;
			parentInstanceNode = childInstanceNode;
		}
		return leafNode;
	}

	// parent node
	// and the child node
	// adds the child node to the instances

	public synchronized void addNode(ISEMOSSNode parentNode, ISEMOSSNode node)
	{
		// this case is when data needs to be added to the lower most node
		// consider the case of Modify Referrals - Patient ID and to this I need to add Eligibility
		// I send in Modify Referrals, Eligibility
		// this needs to navigate down to the lower most and then add it to the lower most
		// set the final child type
		if(finalChildType == null || !nodeIndexHash.containsKey(node.getType()))
			finalChildType = node.getType();		
		TreeNode parentIndexNode = createNode(parentNode, false);
		
		// before all of this, I need to find if this parent already has this node
		
		
		Vector <SimpleTreeNode> parentInstances = parentIndexNode.getInstances();
		
		boolean childExists = false;	
		
		for(int instanceIndex = 0;instanceIndex < parentInstances.size() && !childExists;instanceIndex++)
		{
			SimpleTreeNode parentInstanceNode = parentInstances.elementAt(instanceIndex);
			childExists = parentInstanceNode.hasChild(node);
			if(childExists)
			{
				// update the parent instance node with this child with 1
				parentInstanceNode.incrementCount(node);
			}
		}		
		
		if(childExists)
			return;	
		
		for(int instanceIndex = 0;instanceIndex < parentInstances.size();instanceIndex++)
		{
			SimpleTreeNode parentInstanceNode = parentInstances.elementAt(instanceIndex);
			// add at the same level
			if((parentInstanceNode.leftChild == null) || (parentInstanceNode.leftChild != null && ((ISEMOSSNode)parentInstanceNode.leftChild.leaf).getType().equalsIgnoreCase(node.getType())))
			{
				// logic of adding it to every child
				TreeNode childIndexNode = createNode(node, true);
				SimpleTreeNode childInstanceNode = childIndexNode.getInstances().lastElement();
				//System.err.println("Landed into this logic " + parentInstanceNode.leaf.getKey());
				//synchronized (parentNode) 
				{
				SimpleTreeNode.addLeafChild(parentInstanceNode, childInstanceNode);
				}
				lastAddedNode = parentInstanceNode;	
			}
			// else go down one level and then add it
			else if((parentInstanceNode.leftChild != null && !((ISEMOSSNode)parentInstanceNode.leftChild.leaf).getType().equalsIgnoreCase(node.getType())))
			{
				//System.err.println("Skipping this " + parentNode.getKey() + "<<>> " + node.getKey());
				SimpleTreeNode daNode = parentInstanceNode.leftChild;
				while(daNode != null)
				{
					//System.err.println("Da Node is " + daNode.leaf.getKey());
					
					addLeafNode(daNode, node);
					daNode = daNode.rightSibling;	
				}
				//SimpleTreeNode.addLeafChild(parentInstanceNode, childInstanceNode);
			}
		}		
	}
	
	/**
	 * 
	 * */
	public void append(SimpleTreeNode node, SimpleTreeNode node2merge) {
		//Recursive Algorithm for merging
		//Use in the case node and node2merge come from structurally identical Simple Trees
		//assumes right child does not matter/get used in a SimpleTree
		
		/*
		 * Need to adapt  this to also take into account join
		 * empty node checks 
		 * null checks
		 * different types of appends
		 * append with duplicates, without duplicates, etc.
		 * */
		
		SimpleTreeNode rightNode = SimpleTreeNode.getRight(node);
		SimpleTreeNode leftNode2Merge = SimpleTreeNode.getLeft(node2merge);
		SimpleTreeNode rightMergeSibling = null;
		while(leftNode2Merge!=null) {
			//find if the node exists in the tree node
			TreeNode tn = this.getNode((ISEMOSSNode)leftNode2Merge.leaf);
			
			//If the node doesn't exist in the tree, take the node, sever connection from right sibling and add node to level, repeat for right sibling
			if(tn == null) {
				//Add to the Value Tree
				rightNode.rightSibling = leftNode2Merge;
				leftNode2Merge.leftSibling = rightNode;
				rightMergeSibling = leftNode2Merge.rightSibling;
				
				leftNode2Merge.rightSibling = null;
				rightNode = rightNode.rightSibling;
				
				//Update the Index Tree
				appendToIndexTree(leftNode2Merge);
			} 
			//If the node does exist in the tree, determine if the node exists on this branch
			else {				
				List<SimpleTreeNode> instanceList = tn.getInstances();
				SimpleTreeNode equivalentInstance = null;
				
				boolean foundNode = false;
				SimpleTreeNode instance = null;
				SimpleTreeNode mergeInstance;
				for(int i = 0; i < instanceList.size(); i++) {
					instance = instanceList.get(i);
					equivalentInstance = instance;
					mergeInstance = leftNode2Merge; 
					while(!foundNode && instance.equal(mergeInstance)){
						if(instance.parent==null) {
							foundNode = true; 
							break;
						}
						instance = instance.parent;
						mergeInstance = mergeInstance.parent;
					}
					if(foundNode) {
						break;
					}
				}
				//If the node exists on the branch, append the children
				if(foundNode) { 
//					equivalentInstance = instance; 
					// there is a child, recursively go through method with subset
					if(equivalentInstance.leftChild != null) {
						append(equivalentInstance.leftChild, leftNode2Merge.leftChild);
					} 
					// if no children, add new node children to found instance
//					else if(leftNode2Merge.leftChild != null){ 
//						equivalentInstance.leftChild = leftNode2Merge.leftChild;
//						leftNode2Merge.parent = equivalentInstance;
//					}
					// continue for right siblings
					rightMergeSibling = leftNode2Merge.rightSibling;
				} 
				//if the node doesn't exist on the branch simply add it
				else {
					//Add to the Value Tree
					rightNode.rightSibling = leftNode2Merge;
					leftNode2Merge.leftSibling = rightNode;
					rightMergeSibling = leftNode2Merge.rightSibling;
					
					leftNode2Merge.rightSibling = null;
					rightNode = rightNode.rightSibling;

					//Recursively Update the Index Tree with leftNode2Merge and it's children
					appendToIndexTree(leftNode2Merge); 
				}
				//append(equivalentInstance.leftChild, leftNode2Merge.leftChild);
			}
			leftNode2Merge = rightMergeSibling;
		}		
	}
	
	/**
	 * This method recursively adds a SimpleTreeNode and it's children (subtree) to the appropriate index trees
	 * 
	 * */
	void appendToIndexTree(SimpleTreeNode node) {

		if(node == null) {
			return;
		} else if(node.leaf == null) {
			throw new IllegalArgumentException("node has null leaf");
		}
		
		ISEMOSSNode n = (ISEMOSSNode) node.leaf;
		TreeNode rootIndexNode = nodeIndexHash.get(n.getType());
		
		//If the index tree for the level does not exist, add it
		if(rootIndexNode==null) {
			rootIndexNode = new TreeNode(n);
			rootIndexNode.instanceNode.add(node);
			nodeIndexHash.put(n.getType(), rootIndexNode);
		
			appendToIndexTree(node.leftChild);
			appendToIndexTree(node.rightSibling);
			return;

		} else {

			//loop through node and all siblings
			while(node != null) {
				TreeNode newNode = getNode( (ISEMOSSNode)node.leaf);
				// search first
				if(newNode == null) {
					// if not found 
					// create new node and set instances vector to the new value node and update the new root
					newNode = new TreeNode(node.leaf);
					newNode.addInstance(node);
					TreeNode newRoot = rootIndexNode.insertData(newNode);
					nodeIndexHash.put(n.getType(), newRoot);
					rootIndexNode = newRoot;
	
				} else {
					// if found add instance to existing TreeNode 
					newNode.getInstances().add(node);
				}
				
				if(node.leftChild != null) {
					appendToIndexTree(node.leftChild);
				}
				node = node.rightSibling;
			}
		}
	}
	
	void appendToFilteredIndexTree() {
		
	}
	
	//This method adds a 'node' vs appendToIndexTree which adds a 'tree'
	public void addToNodeIndex(String nodeType, TreeNode node)
	{
		TreeNode root = node;
		//System.err.println("Adding type " + nodeType + " <> " + node.leaf.getKey());
		if(nodeIndexHash.containsKey(nodeType))
		{
			// need to search first to make sure this node is not there
			root = nodeIndexHash.get(nodeType);
			Vector<TreeNode> searcher = new Vector<TreeNode>();
			searcher.add(root);
			if(!root.search(searcher, node, false))
			{
				root = nodeIndexHash.get(nodeType).insertData(node);
			}
		}
		nodeIndexHash.put(nodeType, root);
	}
	
	public void addLeafNode(SimpleTreeNode parentNode, ISEMOSSNode node)
	{
		if((parentNode.leftChild == null) || (parentNode.leftChild != null && ((ISEMOSSNode)parentNode.leftChild.leaf).getType().equalsIgnoreCase(node.getType())))
		{
			// logic of adding it to every child
			TreeNode childIndexNode = createNode(node, true);
			SimpleTreeNode childInstanceNode = childIndexNode.getInstances().lastElement();
			//System.err.println("Landed into this logic " + parentNode.leaf.getKey());
			//synchronized(parentNode)
			{
				SimpleTreeNode.addLeafChild(parentNode, childInstanceNode);
			}
			lastAddedNode = parentNode;
		}
		else if((parentNode.leftChild != null && !((ISEMOSSNode)parentNode.leftChild.leaf).getType().equalsIgnoreCase(node.getType())))
		{
			//System.err.println("Skipping this " + parentNode.leaf.getKey() + "<<>> " + node.getKey());
			SimpleTreeNode daNode = parentNode.leftChild;
			while(daNode != null)
			{
				//System.err.println("Da Node is " + daNode.leaf.getKey());
				addLeafNode(daNode, node);
				daNode = daNode.rightSibling;
			}
			//SimpleTreeNode.addLeafChild(parentInstanceNode, childInstanceNode);
		}
	}


//********************* END ADDITION METHODS ****************************//

	
	
	
//********************* REDUCTION METHODS ****************************//
	
	
	public void quickRefresh() {
		Set<String> levels = this.nodeIndexHash.keySet();
		for(String level: levels){
			this.quickRefresh(level);
		}
	}
	
	public void quickRefresh(String level) {
		TreeNode root = nodeIndexHash.get(level);
		if(root != null) {
			TreeNode newRoot = TreeNode.refresh(root, false);
			nodeIndexHash.put(level, newRoot);
		}
	}
	
	public void refresh() {
		Set<String> levels = this.nodeIndexHash.keySet();
		for(String level: levels){
			this.refresh(level);
		}
	}
	
	public void refresh(String level) {
		TreeNode root = nodeIndexHash.get(level);
		if(root != null) {
			TreeNode newRoot = TreeNode.refresh(root, true);
			nodeIndexHash.put(level, newRoot);
		}
	}
	
	/**
	 * 
	 * @param level
	 * @param height - distance from @param to leaf level in the simple tree
	 * 
	 * removes branches that do not reach to the leaf level
	 */
	public void removeBranchesWithoutMaxTreeHeight(String level, int height) {
		TreeNode rootNodeForType = nodeIndexHash.get(level);
		if(rootNodeForType == null) return;
		
		ValueTreeColumnIterator it = new ValueTreeColumnIterator(rootNodeForType, true);
		while(it.hasNext()) {
			SimpleTreeNode nextNode = it.next();
			removeEmptyRows(nextNode, 0, height, true);
		}
	}
	
	private void removeEmptyRows(SimpleTreeNode n, int start, int height, boolean first) {
		if(start < height-1) {
			
//			if(n.rightSibling!=null && !first) {
//				removeEmptyRows(n.rightSibling, start, height, false);
//			}
//			
			while(n != null) {
				SimpleTreeNode rightSibling = n.rightSibling;
				if (n.leftChild == null) {
					//remove this node, and go up the tree
					while(n!=null) {
						SimpleTreeNode parentNode = n.parent;
						removeFromIndexTree(n);
						SimpleTreeNode.deleteNode(n);
						n = parentNode;
						if(n != null && n.leftChild != null) {
	                        break;
						}
					}
				} else {
					SimpleTreeNode child = n.leftChild;
					removeEmptyRows(child, start+1, height, false);
				}
				
				n = rightSibling;
			}
		}
	}

	/**
	 * 
	 * @param type - the column to be removed
	 */
	public void removeType(String type)	{
		
		if(!nodeIndexHash.containsKey(type)) {
			LOGGER.debug(type  + " does not exist in node index hash");
			return;
		}
		//Determine if the column is a root column
		TreeNode typeRoot = nodeIndexHash.get(type);
		if(typeRoot==null) {
			nodeIndexHash.remove(type);
			return;
		}
		
		ValueTreeColumnIterator getFirst = new ValueTreeColumnIterator(typeRoot, true);
		SimpleTreeNode typeInstanceNode = null;
		if(getFirst.hasNext()) {
			typeInstanceNode = getFirst.next();
			getFirst = null;
		} else {
			nodeIndexHash.remove(type);
			return;
		}
		
		SimpleTreeNode parent = typeInstanceNode.parent;
		
		if(parent != null) {
			LOGGER.debug(type+" is not a root level");
			String parentType = ((ISEMOSSNode)parent.leaf).getType();
			SimpleTreeNode FilteredNode = null;
			
			TreeNode parentTypeRoot = nodeIndexHash.get(parentType);
			Iterator<SimpleTreeNode> iterator = new ValueTreeColumnIterator(parentTypeRoot, true);
			while(iterator.hasNext()) {
				SimpleTreeNode parentNode = iterator.next();
				
				//Grab left child and remove from tree
				SimpleTreeNode nodeToDelete = parentNode.leftChild;
				parentNode.leftChild = null;
				
				//for each node in the leftChild
				while(nodeToDelete != null) {
					
					SimpleTreeNode grandChildNode = nodeToDelete.leftChild;
					if(grandChildNode != null) {					
						if(parentNode.leftChild == null) {
							parentNode.leftChild = grandChildNode;
						} else {
							//TODO: optimize such that child is added to front of child list as opposed to end
							SimpleTreeNode newLeftSibling = SimpleTreeNode.getRight(parentNode.leftChild);
							newLeftSibling.rightSibling = grandChildNode;
							grandChildNode.leftSibling = newLeftSibling;
						}
					}
					
					SimpleTreeNode filteredGrandChildNode = nodeToDelete.rightChild;
					if(filteredGrandChildNode != null) {
						if(parentNode.rightChild == null) {
							parentNode.rightChild = filteredGrandChildNode;
						} else {
							//TODO: optimize such that filtered child is added to front of child list as opposed to end
							SimpleTreeNode newFilteredLeftSibling = SimpleTreeNode.getRight(parentNode.rightChild);
							newFilteredLeftSibling.rightSibling = filteredGrandChildNode;
							filteredGrandChildNode.leftSibling = newFilteredLeftSibling;
						}
					}	
					nodeToDelete = nodeToDelete.rightSibling;
				}
				SimpleTreeNode.setParent(parentNode.leftChild, parentNode);
				
				
				//DELETE THE RIGHT SIDE
				nodeToDelete = parentNode.rightChild;
				parentNode.rightChild = null;
				
				while(nodeToDelete != null) {
					
					SimpleTreeNode grandChildNode = nodeToDelete.leftChild;
					
					if(grandChildNode != null) {					
						if(parentNode.rightChild == null) {
							parentNode.rightChild = grandChildNode;
						} else {
							SimpleTreeNode newLeftSibling = SimpleTreeNode.getRight(parentNode.rightChild);
							newLeftSibling.rightSibling = grandChildNode;
							grandChildNode.leftSibling = newLeftSibling;
						}
					}
					
					SimpleTreeNode filteredGrandChildNode = nodeToDelete.rightChild;
					if(filteredGrandChildNode != null) {
						if(parentNode.rightChild == null) {
							parentNode.rightChild = filteredGrandChildNode;
						} else {
							SimpleTreeNode newFilteredLeftSibling = SimpleTreeNode.getRight(parentNode.rightChild);
							newFilteredLeftSibling.rightSibling = filteredGrandChildNode;
							filteredGrandChildNode.leftSibling = newFilteredLeftSibling;
						}
					}
					nodeToDelete = nodeToDelete.rightSibling;
				}
				SimpleTreeNode.setParent(parentNode.rightChild, parentNode);
				
			}
		}
		else // this is the case when the type is the root
		{
			typeInstanceNode = SimpleTreeNode.getLeft(typeInstanceNode);			
			if(typeInstanceNode != null) {
				
				SimpleTreeNode leftMostNode = typeInstanceNode.leftChild;
				SimpleTreeNode rightMostSibling = SimpleTreeNode.getRight(leftMostNode);
				
				while(typeInstanceNode != null) {
					// need to make sure new root are all siblings
					
					SimpleTreeNode targetNode = typeInstanceNode.leftChild;
					if(targetNode != leftMostNode) { // this occurs for the first iteration
						rightMostSibling.rightSibling = targetNode;
						targetNode.leftSibling = rightMostSibling;
					}
					rightMostSibling = SimpleTreeNode.getRight(targetNode); // update the new most right sibling
					
					while(targetNode != null) {
						targetNode.parent = null;
						targetNode = targetNode.rightSibling;
					}
					
					typeInstanceNode = typeInstanceNode.rightSibling;
				}
			}
			
//			SimpleTreeNode root = this.getRoot();
//			SimpleTreeNode filteredRoot = this.getFilteredRoot();
//			SimpleTreeNode newFilteredRoot = null;
//			
//			if(root != null) {
//				SimpleTreeNode leftMostNode = root.leftChild;
//				SimpleTreeNode rightMostSibling = SimpleTreeNode.getRight(leftMostNode);
//				
//				newFilteredRoot = root.rightChild;
//				SimpleTreeNode filteredRightMostSibling = null;
//				if(newFilteredRoot != null) {
//					filteredRightMostSibling = SimpleTreeNode.getRight(newFilteredRoot);
//				}
//				
//				while(root != null) {
//					
//					SimpleTreeNode targetNode = root.leftChild;
//					if(targetNode != leftMostNode) {
//						rightMostSibling.rightSibling = targetNode;
//						targetNode.leftSibling = rightMostSibling;
//					}
//					rightMostSibling = SimpleTreeNode.getRight(targetNode);
//					
//					//get the right children, add on the new filtered Root
//					SimpleTreeNode filteredTargetNode = root.rightChild;
//					if(filteredTargetNode != newFilteredRoot && newFilteredRoot != null) {
//						filteredRightMostSibling.rightSibling = filteredTargetNode;
//						filteredTargetNode.leftSibling = filteredRightMostSibling;
//						filteredRightMostSibling = SimpleTreeNode.getRight(filteredTargetNode);
//					} else if(newFilteredRoot == null) {
//						newFilteredRoot = filteredTargetNode;
//						if(newFilteredRoot != null) {
//							filteredRightMostSibling = SimpleTreeNode.getRight(newFilteredRoot);
//						}
//					}
//					
//					//null the parents for left child list
//					while(targetNode != null) {
//						targetNode.parent = null;
//						targetNode = targetNode.rightSibling;
//					}
//					
//					//null the parents for the right child list
//					while(filteredTargetNode != null) {
//						filteredTargetNode.parent = null;
//						filteredTargetNode = filteredTargetNode.rightSibling;
//					}
//					
//					root = root.rightSibling;
//				}
//			}
//			
//			if(filteredRoot != null) {
//				SimpleTreeNode leftMostNode = filteredRoot.leftChild;
//				SimpleTreeNode rightMostSibling = SimpleTreeNode.getRight(leftMostNode);
//				
//				if(newFilteredRoot == null) {
//					newFilteredRoot = leftMostNode;
//				} else {
//					newFilteredRoot.rightSibling = leftMostNode;
//				}
//				SimpleTreeNode filteredRightMostSibling = null;
//				
//				while(filteredRoot != null) {
//					
//					SimpleTreeNode targetNode = root.leftChild;
//					if(targetNode != leftMostNode) {
//						rightMostSibling.rightSibling = targetNode;
//						targetNode.leftSibling = rightMostSibling;
//					}
//					rightMostSibling = SimpleTreeNode.getRight(targetNode);
//					
//					//get the right children, add on the new filtered Root
//					SimpleTreeNode filteredTargetNode = root.rightChild;
//					if(filteredTargetNode != newFilteredRoot && newFilteredRoot != null) {
//						filteredRightMostSibling.rightSibling = filteredTargetNode;
//						filteredTargetNode.leftSibling = filteredRightMostSibling;
//						filteredRightMostSibling = SimpleTreeNode.getRight(filteredTargetNode);
//					} else if(newFilteredRoot == null) {
//						newFilteredRoot = filteredTargetNode;
//						if(newFilteredRoot != null) {
//							filteredRightMostSibling = SimpleTreeNode.getRight(newFilteredRoot);
//						}
//					}
//					
//					//null the parents for left child list
//					while(targetNode != null) {
//						targetNode.parent = null;
//						targetNode = targetNode.rightSibling;
//					}
//					
//					//null the parents for the right child list
//					while(filteredTargetNode != null) {
//						filteredTargetNode.parent = null;
//						filteredTargetNode = filteredTargetNode.rightSibling;
//					}
//					
//					root = root.rightSibling;
//				}
//			}
		}
		
		// if I take it off the main hashtable
		// it will get GC'ed. I am not sure I need to travel individually and do it
		nodeIndexHash.remove(type);
	}

	
	public void removeDuplicates(String type) {
		//Logic
		//for each branch, sort then compress
		if(!nodeIndexHash.containsKey(type)) return;

		SimpleTreeNode simpleTreeNode = new ValueTreeColumnIterator(nodeIndexHash.get(type), true).next();
		boolean root = (simpleTreeNode.parent == null);
		//TODO : fix for now, make better
		if(root) {
			this.unfilterColumn(type);
		}
		ISEMOSSNode parentNode;
		
		Comparator<SimpleTreeNode> simpleTreeComparator = new Comparator<SimpleTreeNode>() {
			@Override
			public int compare(SimpleTreeNode n1, SimpleTreeNode n2) {
				if(n1.equal(n2)) {
					return 0;
				}
				else if(n1.left(n2)) {
					return -1;
				}
				else {
					return 1;
				}
			}
		};
		
		if(!root) {
			parentNode = (ISEMOSSNode)simpleTreeNode.parent.leaf;
			type = parentNode.getType();
		}
		
		List<SimpleTreeNode> branchList;
		ValueTreeColumnIterator iterator = new ValueTreeColumnIterator(nodeIndexHash.get(type), true);
		if(root) {
			branchList = new ArrayList<SimpleTreeNode>();
			while(iterator.hasNext()) {
				SimpleTreeNode nextNode = iterator.next();
				nextNode.rightSibling = null;
				nextNode.leftSibling = null;
				branchList.add(nextNode);
			}
			sortBranch(branchList, simpleTreeComparator, root);
		} else {
			while(iterator.hasNext()) {
				SimpleTreeNode node = iterator.next().leftChild;
				SimpleTreeNode nextNode;
				branchList = new ArrayList<SimpleTreeNode>();
				while(node != null) {
					nextNode = node.rightSibling;
					node.rightSibling = null;
					node.leftSibling = null;
					branchList.add(node);
					node = nextNode;
				}
				sortBranch(branchList, simpleTreeComparator, root);
			}
		}
	}
	
	private void sortBranch(List<SimpleTreeNode> branchList, Comparator<SimpleTreeNode> simpleTreeComparator, boolean root) {
		Collections.sort(branchList, simpleTreeComparator);
		
		boolean keepGoing = (branchList.size() > 1);
		int i = 0;
		while(keepGoing) {
			SimpleTreeNode n1 = branchList.get(i);
			SimpleTreeNode n2 = branchList.get(i+1);
			
			if(n1.equal(n2)) {
				consolidate(n1, n2);
				branchList.remove(i+1);
			} else {
				i++;
			}
			
			keepGoing = i < branchList.size() - 1;
			if(!keepGoing && !root) {
				SimpleTreeNode newLeftChild = branchList.get(0);
				SimpleTreeNode parent = newLeftChild.parent;
				parent.leftChild = newLeftChild;
			}
		}
		
//		if(root) {
			for(i = 0; i < branchList.size()-1; i++) {	
				SimpleTreeNode n1 = branchList.get(i);
				SimpleTreeNode n2 = branchList.get(i+1);
				n1.rightSibling = n2;
				n2.leftSibling = n1;
			}
//		}
	}
	
	private void consolidate(SimpleTreeNode node, SimpleTreeNode node2consolidate) {
		SimpleTreeNode nodeLeftChild = node.leftChild;
		SimpleTreeNode nodeRightChild = node.rightChild;
		
		SimpleTreeNode cNodeLeftChild = node2consolidate.leftChild;
		SimpleTreeNode cNodeRightChild = node2consolidate.rightChild;
		
		//Combine the left side
		if(nodeLeftChild == null && cNodeLeftChild != null) {
			
			SimpleTreeNode.setParent(cNodeLeftChild, node);
			node.leftChild = cNodeLeftChild;
		
		} else if(nodeLeftChild != null && cNodeLeftChild != null){
			
			SimpleTreeNode.setParent(cNodeLeftChild, node);
			nodeLeftChild = SimpleTreeNode.getRight(nodeLeftChild);
			nodeLeftChild.rightSibling = cNodeLeftChild;
			cNodeLeftChild.leftSibling = nodeLeftChild;
			
//			cNodeLeftChild.leftSibling = null;
//			SimpleTreeNode.getRight(cNodeLeftChild).rightSibling = nodeLeftChild;
//			node.leftChild = cNodeLeftChild;
		
		}
			
		//Combine the right side
		if(nodeRightChild == null && cNodeRightChild != null) {
			
			SimpleTreeNode.setParent(cNodeRightChild, node);
			node.rightChild = cNodeRightChild;
			
		} else if(nodeRightChild != null && cNodeRightChild != null) {
		
			SimpleTreeNode.setParent(cNodeRightChild, node);
			nodeRightChild = SimpleTreeNode.getRight(nodeRightChild);
			nodeRightChild.rightSibling = cNodeRightChild;
			cNodeRightChild.leftSibling = nodeRightChild;
		
		}
		
		this.removeFromIndexTree(node2consolidate);
	}
	
	/**
	 * Finds the TreeNode in the index tree with the value of @param n, 
	 * and removes the instance of n from the index tree
	 * 
	 * @param n		The instance to be removed from the index tree
	 */
	protected void removeFromIndexTree(SimpleTreeNode n) {
		TreeNode foundNode = this.getNode((ISEMOSSNode)n.leaf);
		if(foundNode!=null) {
			if(!foundNode.getInstances().remove(n)) {
				foundNode.filteredInstanceNode.remove(n);
			}
		}
	}
	
	/**
	 * Currently rebuilds Index Tree with remaining TreeNodes after deletion. Can implement rebalance as an option to clean up Index Tree.
	 * 
	 * @param level Column header
	 * @return TreeNode root node of rebuilt/rebalanced Index Tree
	 */
	public TreeNode refreshIndexTree(String level) {
		//TODO: will uncomment this once we have deleting from tree node as opposed to recreating using unfilterd values
//		TreeNode refreshedNode = null;
//		boolean isRebuild = true;
//		
//		Vector<TreeNode> nodeList = new Vector<TreeNode>();
//		TreeNode root = this.nodeIndexHash.get(level);
//		nodeList = deleteIndexFilters(root, isRebuild);
//		
//		if(isRebuild) {
//			refreshedNode = rebuild(nodeList);
//		} else {
//			// TODO: implement rebalance under conditions where it would be faster to rebalance rather than rebuild Index Tree
//		}
		
		//New logic until deleting individual nodes from index tree is set up
		Vector<TreeNode> nodeList = new Vector<TreeNode>();
		TreeNode root = this.nodeIndexHash.get(level);
		// create iterate for index tree that gets all instance values that are not completely filtered out
		FilteredIndexTreeIterator it = new FilteredIndexTreeIterator(root);
		while(it.hasNext()) {
			// grab the next node
			// clear out the references to Simple Tree Nodes in the filtered list
			TreeNode node = it.next();
			node.filteredInstanceNode.clear();
			// add node to new list
			nodeList.addElement(node);
		}
		// rebuild method will create a new index tree using the list of nodes and return the root
		return rebuild(nodeList);
	}
	
	/**
	 * Removes all right children in the BTree: This hard deletes all filtered values. Works recursively. Also deletes filteredRoot to remove filtered root values.
	 * 
	 * @param root SimpleTreeNode from Value Tree that needs to have filtered values removed
	 */
	public void deleteFilteredValues(SimpleTreeNode node) {
		if (!node.hasChild()) {
			this.filteredRoot = null;
			return;
		}
		node.rightChild = null;
		if (node.leftChild != null) {
			if (node.leftChild.hasChild()) {
				deleteFilteredValues(node.leftChild);
			}
		}
		if (node.rightSibling != null) {
			deleteFilteredValues(node.rightSibling);
		}
		this.filteredRoot = null;
	}
	
	/**
	 * Gathers list of TreeNodes that will be used to either rebuild Index Tree or contain TreeNodes that must be deleted during rebalance
	 * 
	 * @param isRebuild
	 *            true if method is being used to rebuild index tree, false if used to rebalance index tree
	 * @return ArrayList of TreeNodes that will either be kept or deleted
	 */
	private Vector<TreeNode> deleteIndexFilters(TreeNode root, boolean isRebuild) {
		Vector<TreeNode> nodeList = new Vector<TreeNode>();
		
		CompleteIndexTreeIterator iterator = new CompleteIndexTreeIterator(root); // needs to iterate through logically deleted nodes, which IndexTreeIterator does not do currently
		while (iterator.hasNext()) {
			root = iterator.next();
			if (root.filteredInstanceNode.size() > 0) {
				root.filteredInstanceNode = new Vector<SimpleTreeNode>();
			}
			if (isRebuild) {
				if (root.instanceNode.size() > 0) {
					nodeList.add(root); // when rebuilding, we need nodes that will be kept
				}
			} else {
				if (root.instanceNode.size() == 0) {
					nodeList.add(root); // when rebalancing, we need nodes that will be deleted
				}
			}
		}
		return nodeList;
	}

	/**
	 * Rebuilds the Index Tree with the TreeNodes contained in keepList
	 * 
	 * @param keepList ArrayList containing TreeNodes that are to be kept in the new Index Tree
	 * @return TreeNode of new Index Tree
	 */
	private TreeNode rebuild(Vector<TreeNode> keepList) {
		TreeNode newRoot = keepList.get(0);
		// clean node method removes the filters of the tree node
		newRoot.cleanNode();
		keepList.remove(0);
		
		for (TreeNode node : keepList) {
			node.cleanNode();
			newRoot = newRoot.insertData(node);
		}
		
		return newRoot;
	}

	/**
	 * Removes all rows containing specified ISEMOSSNode
	 * 
	 * @param node ISEMOSSNode of value to be deleted
	 */
	public void removeNode(ISEMOSSNode node)
	{
		String type = node.getType();
		if(nodeIndexHash.containsKey(type))
		{
			TreeNode rootNode = nodeIndexHash.get(type);
			TreeNode searchNode = new TreeNode(node); // TreeNode without the references that rootNode has; is this necessary?
			Vector<TreeNode> searchVector = new Vector<TreeNode>();
			searchVector.addElement(rootNode);
			TreeNode foundNode = rootNode.getNode(searchVector, searchNode, false); // can we just feed rootNode instead of searchNode?
			
			if(foundNode != null)
			{
				// get all the instances and delete
				Vector<SimpleTreeNode> instances = foundNode.getInstances();
				Vector<SimpleTreeNode> filteredInstances = foundNode.getFilteredInstances();
				
				boolean hasInstances = instances.size() > 0;
				boolean hasFilters = filteredInstances.size() > 0;
				
				if (hasInstances) {
					while (instances.size() > 0) {
						removeInstanceFromBTree(instances.get(0));
					}
				}
				if (hasFilters) {
					while (filteredInstances.size() > 0) {
						removeInstanceFromBTree(filteredInstances.get(0));
					}
				}
			}
		}
	}
	
	/**
	 * Removes value from both Value Tree and Index Tree
	 * 
	 * @param n SimpleTreeNode of value to be deleted
	 */
	private void removeInstanceFromBTree(SimpleTreeNode n) {
		SimpleTreeNode node = findParentWithSibling(n);
		removeAllChildrenFromBTree(node, true);
	}
	
	/**
	 * Searches up the Value Tree recursively until it has found a node with siblings or is at root level. Once found, the parent is removed from the Value Tree, along with all its children.
	 * 
	 * @param n SimpleTreeNode to start searching from
	 * @return most parent SimpleTreeNode
	 */
	private SimpleTreeNode findParentWithSibling(SimpleTreeNode n) {
		SimpleTreeNode rightSibling = n.rightSibling;
		SimpleTreeNode leftSibling = n.leftSibling;
		SimpleTreeNode parent = n.parent;
		
		boolean hasParent = parent != null;
		boolean hasRightSibling = rightSibling != null;
		boolean hasLeftSibling = leftSibling != null;
		
		// rewire siblings
		if (hasParent) {
			if (!hasLeftSibling && hasRightSibling) {
				parent.leftChild = rightSibling;
				rightSibling.leftSibling = null;
			} else {
				n = findParentWithSibling(parent); // node has no siblings; find first parent node that does or root
			}
		} else if (hasLeftSibling && hasRightSibling) { // in the middle of two nodes
			rightSibling.leftSibling = leftSibling;
			leftSibling.rightSibling = rightSibling;
		} else if (hasLeftSibling && !hasRightSibling) {
			leftSibling.rightSibling = null;
		} else { // neither parent nor left sibling, but has right sibling
			rightSibling.leftSibling = null;
		}
		
		return n;
	}
	
	/**
	 * Recursively goes through entire subtree and removes each SimpleTreeNode from the Index Tree
	 * 
	 * @param n
	 * @param isFirstNode
	 */
	private void removeAllChildrenFromBTree(SimpleTreeNode n, boolean isFirstNode) {
		SimpleTreeNode instanceChild = n.leftChild;
		SimpleTreeNode filteredChild = n.rightChild;
		SimpleTreeNode rightSibling = n.rightSibling;
		
		boolean hasInstances = instanceChild != null;
		boolean hasFilters = filteredChild != null;
		boolean hasRight = rightSibling != null;
		
		this.removeFromIndexTree(n);

		if (hasRight && !isFirstNode) {
			removeAllChildrenFromBTree(rightSibling, false);
		} else if (hasFilters) {
			removeAllChildrenFromBTree(filteredChild, false);
		}
		if (hasInstances) {
			removeAllChildrenFromBTree(instanceChild, false);
		}
	}
	
	
//*********************END REDUCTION METHODS ****************************//


	

	
//******************** FILTER METHODS **************************//
	
	/**
	 * 
	 * @param objectsToFilter - list of objects to be filtered OUT of the tree
	 */
	public void filterTree(List<ITreeKeyEvaluatable> objectsToFilter) {
		for(ITreeKeyEvaluatable i: objectsToFilter) {
			filterTree(i);
		}
	}
	
	/**
	 * 
	 * @param objectToFilter - object to filter OUT of the tree
	 */
	public void filterTree(ITreeKeyEvaluatable objectToFilter) {
		//find the TreeNode associated with the object's value
		TreeNode foundNode = this.getNode((ISEMOSSNode)objectToFilter);
		if(foundNode != null) {
			
			//copy references to new list to prevent concurrent modification exceptions
			List<SimpleTreeNode> nodeList = new ArrayList<SimpleTreeNode>();
			for(SimpleTreeNode n: foundNode.instanceNode) {
				nodeList.add(n);
			}
			
			//for each SimpleTreeNode with the value of objectToFilter
			//first filter the value tree, then filter the index tree
			for(SimpleTreeNode n: nodeList) {
				filterSimpleTreeNode(n);
				filterTreeNode(n, true);
			}
		}
	}

	/**
	 * 
	 * @param node2filter
	 * 
	 * This method filters node2filter from the value tree
	 * In the case where node2filter's parent has one child (which is node2filter), node2filter's parent is filtered, etc
	 */
	private void filterSimpleTreeNode(SimpleTreeNode node2filter) {
		
		SimpleTreeNode parentNode = node2filter.parent;
		boolean root = (parentNode == null);
		
		//Grab the siblings
		SimpleTreeNode nodeRightSibling = node2filter.rightSibling;
		SimpleTreeNode nodeLeftSibling = node2filter.leftSibling;
		
		//isolate node2filter from siblings and rewire the connections
		if(node2filter.rightSibling != null && node2filter.leftSibling != null) {
			//in the middle
			nodeRightSibling.leftSibling = nodeLeftSibling;
			nodeLeftSibling.rightSibling = nodeRightSibling;	
		} 
		else if(node2filter.rightSibling == null && node2filter.leftSibling != null) {
			//right most
			nodeLeftSibling.rightSibling = null;
		} 
		else if(node2filter.rightSibling != null && node2filter.leftSibling == null) {
			//left most
			if(!root) {
				parentNode.leftChild = nodeRightSibling;
			}
			nodeRightSibling.leftSibling = null;
		} else {
			//only child
			if(!root) {
				parentNode.leftChild = null;
			}
		}
		
		//Isolate the node from the siblings
		node2filter.rightSibling = null;
		node2filter.leftSibling = null;
		
		//If node is root level, attach to filteredRoot
		if(root) {
			if(filteredRoot == null) {
				filteredRoot = node2filter;
			} else {
				filteredRoot.leftSibling = node2filter;
				node2filter.rightSibling = filteredRoot;
				filteredRoot = node2filter;
			}
		} 
		
		//otherwise put node2filter on the right side of the parent
		else {
			if(parentNode.rightChild == null) {
				parentNode.rightChild = node2filter;
			} else {
				SimpleTreeNode rightFilteredChild = parentNode.rightChild;
				node2filter.leftSibling = rightFilteredChild;
				SimpleTreeNode nextRight = rightFilteredChild.rightSibling;
				rightFilteredChild.rightSibling = node2filter;
				if(nextRight != null) {
					nextRight.leftSibling = node2filter;
					node2filter.rightSibling = nextRight;
				}
			}
		}
		
		//if parent node exists and parent node has no left children, filter that parent too but just that parent
		if(parentNode != null && SimpleTreeNode.countNodeChildren(parentNode)==0) {
			filterSimpleTreeNode(parentNode);
			//transFilterSimpleTreeNode(parentNode);
			filterTreeNode(parentNode);
			parentNode = parentNode.parent;
		}
	}
	
	/**
	 * 
	 * @param instance2filter - the root of the sub tree to filter
	 * @param firstLevel - indicates whether the call refers to the first level call
	 * 
	 * filters a subtree from the value tree with root 'instance2filter' from the index trees
	 * will not filter the siblings of instance2filter
	 */
	private void filterTreeNode(SimpleTreeNode instance2filter, boolean firstLevel) {
		
		TreeNode foundNode = this.getNode((ISEMOSSNode)instance2filter.leaf);
		
		if(foundNode != null) {
		
			//filter the node
			if(foundNode.instanceNode.remove(instance2filter)) {
				foundNode.filteredInstanceNode.add(instance2filter);
			}
			
			//filter the sibling if not in the first level
			if(!firstLevel && instance2filter.rightSibling!=null) {
				filterTreeNode(instance2filter.rightSibling, false);
			}
			
			//filter the left child
			//filtering the right child not necessary because the right child is already filtered
			if(instance2filter.leftChild!=null) {
				filterTreeNode(instance2filter.leftChild, false);
			}
		}
	}
	
	/**
	 * 
	 * @param instance2filter - the single node to filter
	 * 
	 * filters a single node from its corresponding index tree
	 */
	private void filterTreeNode(SimpleTreeNode instance2filter) {
		TreeNode foundNode = this.getNode((ISEMOSSNode)instance2filter.leaf);
		
		if(foundNode != null) {
			if(foundNode.instanceNode.remove(instance2filter)) {
				foundNode.filteredInstanceNode.add(instance2filter);
			}
		}
	}
	
	
	public void unfilterColumn(String column) {
		
		//what happens when you try to filter out everything and unfilter?
		SimpleTreeNode root = this.getRoot();
		boolean check = true;
		if(root == null) {
			root = this.getFilteredRoot();
			check = false;
			if(root == null) return;
		}
		
		
		if(((ISEMOSSNode)(root.leaf)).getType().equals(column) && check) {
			if(filteredRoot != null) {
				///SimpleTreeNode root = this.getRoot();
				root = SimpleTreeNode.getRight(root);
				
				unfilterTreeNode(filteredRoot, false);
				root.rightSibling = filteredRoot;
				filteredRoot.leftSibling = root;
				
//				unfilterTreeNode(filteredRoot);
				filteredRoot = null;
			}
		} else if(((ISEMOSSNode)(root.leaf)).getType().equals(column) && !check) {
			unfilterTreeNode(filteredRoot);
			filteredRoot = null;
		} else {
			
			TreeNode unfilterIndexTree = nodeIndexHash.get(column);
			
			ValueTreeColumnIterator it = new ValueTreeColumnIterator(unfilterIndexTree, true);
			ITreeKeyEvaluatable l;
			if(it.hasNext()) {
				l = it.next().parent.leaf;
			} else {
				return;
			}

			ISEMOSSNode parent = (ISEMOSSNode)l;
			String parentType = parent.getType();
			
			TreeNode parentIndexTree = nodeIndexHash.get(parentType);
			ValueTreeColumnIterator iterator = new ValueTreeColumnIterator(parentIndexTree);
		

			while(iterator.hasNext()) {
				SimpleTreeNode simpleTree = iterator.next();
				
				if(simpleTree.rightChild!=null) {
					SimpleTreeNode leftChild = simpleTree.leftChild;
					SimpleTreeNode rightChild = simpleTree.rightChild;
					
					unfilterTreeNode(simpleTree.rightChild, true);
					
					if(leftChild==null) {
						simpleTree.leftChild = rightChild;
					} else {
						SimpleTreeNode rightMostLeftChild = SimpleTreeNode.getRight(leftChild);
						rightMostLeftChild.rightSibling = rightChild;
						rightChild.leftSibling = rightMostLeftChild;
					}
					simpleTree.rightChild = null;
				}
			}
			
			FilteredValueTreeColumnIterator fiterator = new FilteredValueTreeColumnIterator(unfilterIndexTree);
			while(fiterator.hasNext()) {
				SimpleTreeNode unfilteredNode = fiterator.next(); 
				unfilterSimpleTreeNode(unfilteredNode);
				if(unfilteredNode != null) {
					unfilterTreeNode(unfilteredNode, true);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param objectToFilter - object to unfilter, or bring back visibility into the tree
	 */
	public void unfilterTree(ITreeKeyEvaluatable objectToFilter) {
		//find the TreeNode associated with the object's value
		TreeNode foundNode = this.getNode((ISEMOSSNode)objectToFilter);
		if(foundNode != null) {
			
			//copy references to new list to prevent concurrent modification exceptions
			List<SimpleTreeNode> nodeList = new ArrayList<SimpleTreeNode>();
			for(SimpleTreeNode n: foundNode.filteredInstanceNode) {
				nodeList.add(n);
			}
			
			for(SimpleTreeNode n: foundNode.instanceNode) {
				nodeList.add(n);
			}
			//for each SimpleTreeNode with the value of objectToFilter
			//first filter the value tree, then filter the index tree
			for(SimpleTreeNode n: nodeList) {
//				boolean unfilteredNode = unfilterSimpleTreeNode(n);
				unfilterSimpleTreeNode(n);
//				if(unfilteredNode) {
				unfilterTreeNode(n, true);
//				}
			}
		}
	}
	
	private boolean unfilterSimpleTreeNode(SimpleTreeNode node2filter) {
		SimpleTreeNode parentNode = node2filter.parent;
		boolean root = (parentNode == null);
		if(!root && isFiltered(parentNode) && parentNode.leftChild != null) {
			SimpleTreeNode leftChild = parentNode.leftChild;
			SimpleTreeNode rightChild = parentNode.rightChild;
			if(parentNode.rightChild == null) {
				parentNode.rightChild = leftChild;
			} else {
				SimpleTreeNode rightMostRightChild = SimpleTreeNode.getRight(rightChild);
				rightMostRightChild.rightSibling = leftChild;
			}
			parentNode.leftChild = null;
		}
		
		//Grab the siblings
		if((root && isFiltered(node2filter)) || (!root && parentNode.leftChild != node2filter)) {
			SimpleTreeNode nodeRightSibling = node2filter.rightSibling;
			SimpleTreeNode nodeLeftSibling = node2filter.leftSibling;
			
			//isolate node2filter from siblings and rewire the connections
			if(node2filter.rightSibling != null && node2filter.leftSibling != null) {
				//in the middle
				nodeRightSibling.leftSibling = nodeLeftSibling;
				nodeLeftSibling.rightSibling = nodeRightSibling;	
			} 
			else if(node2filter.rightSibling == null && node2filter.leftSibling != null) {
				//right most
				nodeLeftSibling.rightSibling = null;
			} 
			else if(node2filter.rightSibling != null && node2filter.leftSibling == null) {
				//left most
				if(!root) {
					parentNode.rightChild = nodeRightSibling;
				}
				nodeRightSibling.leftSibling = null;
			} else {
				//only child
				if(!root) {
					parentNode.rightChild = null;
				}
			}
			
			//Isolate the node from the siblings
			node2filter.rightSibling = null;
			node2filter.leftSibling = null;
			
			//If node is root level, attach to filteredRoot
			if(root) {
				if(filteredRoot == node2filter) {
					filteredRoot = nodeRightSibling;
				}
				
				SimpleTreeNode rootNode = this.getRoot();
				if(rootNode != null) {
					rootNode.leftSibling = node2filter;
					node2filter.rightSibling = rootNode;
				}
			} 
			
			//otherwise put node2filter on the right side of the parent
			else {
				if(parentNode.leftChild == null) {
					parentNode.leftChild = node2filter;
				} else {
					SimpleTreeNode leftChild = parentNode.leftChild;
					node2filter.leftSibling = leftChild;
					SimpleTreeNode nextRight = leftChild.rightSibling;
					leftChild.rightSibling = node2filter;
					if(nextRight != null) {
						nextRight.leftSibling = node2filter;
						node2filter.rightSibling = nextRight;
					}
				}
			}
		}
		
		//make this recursive so all children are unfiltered
		unfilterSimpleTreeNodeChildren(node2filter, true);
		
		while(!root && isFiltered(parentNode)) {
			unfilterSingleNode(parentNode);
			unfilterTreeNode(parentNode);
			parentNode = parentNode.parent;
			root = parentNode == null;
		}
		//move right children to left, repeat for each child
		
		return true;
	}
	
	private void unfilterSingleNode(SimpleTreeNode node2filter) {
		SimpleTreeNode parentNode = node2filter.parent;
		boolean root = (parentNode == null);
//		if(!root && isFiltered(parentNode)) {
//			return false;
//		}
		if(!root && isFiltered(parentNode) && parentNode.leftChild != null) {
			SimpleTreeNode leftChild = parentNode.leftChild;
			SimpleTreeNode rightChild = parentNode.rightChild;
			if(parentNode.rightChild == null) {
				parentNode.rightChild = leftChild;
			} else {
				SimpleTreeNode rightMostRightChild = SimpleTreeNode.getRight(rightChild);
				rightMostRightChild.rightSibling = leftChild;
			}
			parentNode.leftChild = null;
		}
		
		//Grab the siblings
		if((root && isFiltered(node2filter)) || (!root && parentNode.leftChild != node2filter)) {
			SimpleTreeNode nodeRightSibling = node2filter.rightSibling;
			SimpleTreeNode nodeLeftSibling = node2filter.leftSibling;
			
			//isolate node2filter from siblings and rewire the connections
			if(node2filter.rightSibling != null && node2filter.leftSibling != null) {
				//in the middle
				nodeRightSibling.leftSibling = nodeLeftSibling;
				nodeLeftSibling.rightSibling = nodeRightSibling;	
			} 
			else if(node2filter.rightSibling == null && node2filter.leftSibling != null) {
				//right most
				nodeLeftSibling.rightSibling = null;
			} 
			else if(node2filter.rightSibling != null && node2filter.leftSibling == null) {
				//left most
				if(!root) {
					parentNode.rightChild = nodeRightSibling;
				}
				nodeRightSibling.leftSibling = null;
			} else {
				//only child
				if(!root) {
					parentNode.rightChild = null;
				}
			}
			
			//Isolate the node from the siblings
			node2filter.rightSibling = null;
			node2filter.leftSibling = null;
			
			//If node is root level, attach to filteredRoot
			if(root) {				
				if(filteredRoot == node2filter) {
					filteredRoot = nodeRightSibling;
				}
				
				SimpleTreeNode rootNode = this.getRoot();
				if(rootNode != null) {
					rootNode.leftSibling = node2filter;
					node2filter.rightSibling = rootNode;
				}
			} 
			
			//otherwise put node2filter on the left side of the parent
			else {
				if(parentNode.leftChild == null) {
					parentNode.leftChild = node2filter;
				} else {
					SimpleTreeNode leftChild = parentNode.leftChild;
					node2filter.leftSibling = leftChild;
					SimpleTreeNode nextRight = leftChild.rightSibling;
					leftChild.rightSibling = node2filter;
					if(nextRight != null) {
						nextRight.leftSibling = node2filter;
						node2filter.rightSibling = nextRight;
					}
				}
			}
		}
	}
	
	private void unfilterSimpleTreeNodeChildren(SimpleTreeNode root, boolean firstLevel) {
		if(root == null) {
			return;
		}
		
		//Move right child to left
		if(root.rightChild != null) {
			
			SimpleTreeNode rightChild = root.rightChild;
			SimpleTreeNode leftChild = root.leftChild;
			
			if(root.leftChild == null) {
				root.leftChild = rightChild;
			} else {
				SimpleTreeNode rightMostLeftChild = SimpleTreeNode.getRight(leftChild);
				rightMostLeftChild.rightSibling = rightChild;
				rightChild.leftSibling = rightMostLeftChild;
			}
			root.rightChild = null;
			unfilterTreeNode(rightChild, false);
		}
		
		//Continue for other nodes
		if(!firstLevel && root.rightSibling != null) {
			unfilterSimpleTreeNodeChildren(root.rightSibling, false);
		}
		
		if(root.leftChild != null) {
			unfilterSimpleTreeNodeChildren(root.leftChild, false);
		}
	}
	
//	private SimpleTreeNode unfilterSimpleTreeNode(SimpleTreeNode node) {
//		SimpleTreeNode parentNode = node.parent;
//		//convert from recursive to iterative
////		while(parentNode != null && SimpleTreeNode.hasOneChild(parentNode)) {
////			node = parentNode;
////			parentNode = parentNode.parent;
////		}
//		boolean root = (parentNode==null);
//		SimpleTreeNode nodeRightSibling = node.rightSibling;
//		SimpleTreeNode nodeLeftSibling = node.leftSibling;
//		
//		if(!root) {
//			
//			//isolate node2filter from siblings and rewire the connections
//			if(node.rightSibling != null && node.leftSibling != null) {
//				//in the middle
//				nodeRightSibling.leftSibling = nodeLeftSibling;
//				nodeLeftSibling.rightSibling = nodeRightSibling;	
//			} 
//			else if(node.rightSibling == null && node.leftSibling != null) {
//				//right most
//				nodeLeftSibling.rightSibling = null;
//			} 
//			else if(node.rightSibling != null && node.leftSibling == null) {
//				//left most
//				if(!root) {
//					parentNode.rightChild = nodeRightSibling;
//				}
//				nodeRightSibling.leftSibling = null;
//			} else {
//				//only child
//				parentNode.leftChild = null;
//			}
//			
//			node.rightSibling = null;
//			node.leftSibling = null;
//			
//			if(parentNode.leftChild==null) {
//				parentNode.leftChild = node;
//			} else {
//				//TODO: attach to beginning of list
//				SimpleTreeNode rightMostLeftChild = SimpleTreeNode.getRight(node);
//				rightMostLeftChild.rightSibling = node;
//				node.leftSibling = rightMostLeftChild;
//			}
//			
//			if(SimpleTreeNode.hasOneChild(parentNode) && isFiltered(parentNode)) {
//				return unfilterSimpleTreeNode(parentNode);
//			} else if(isFiltered(parentNode)) {
//				return null;
//			} else {
//				return node;
//			}
//		} 
//		else {
//			//in the case the node is a root
//			
//			if(nodeRightSibling!=null && nodeLeftSibling != null) {
//				//middle
//				nodeLeftSibling.rightSibling = nodeRightSibling;
//				nodeRightSibling.leftSibling = nodeLeftSibling;
//			} else if(nodeRightSibling==null && nodeLeftSibling != null) {
//				//right most
//				nodeLeftSibling.rightSibling = null;
//			} else if(nodeLeftSibling==null && nodeRightSibling != null) {
//				//left most
//				filteredRoot = nodeRightSibling;
//				nodeRightSibling.leftSibling = null;
//			} else {
//				//only one
//				filteredRoot = null;
//			}
//			
//			node.rightSibling = null;
//			node.leftSibling = null;
//			
//			SimpleTreeNode rootNode = this.getRoot();
//			if(rootNode != null) {
//				rootNode = SimpleTreeNode.getRight(rootNode);
//				rootNode.rightSibling = node;
//				node.leftSibling = rootNode;
//			}
//			
//			return node;
//		}
//	}

	/**
	 * 
	 * @param instance
	 * @param firstLevel
	 */
	private void unfilterTreeNode(SimpleTreeNode instance, boolean firstLevel) {
		TreeNode foundNode = this.getNode((ISEMOSSNode)instance.leaf);
		
		if(foundNode != null) {
		
			if(foundNode.filteredInstanceNode.remove(instance)) {
				foundNode.instanceNode.add(instance);
			}
			
			if(!firstLevel && instance.rightSibling!=null) {
				unfilterTreeNode(instance.rightSibling, false);
			}
			
			if(instance.leftChild!=null) {
				unfilterTreeNode(instance.leftChild, false);
			}
		}
	}
	
	/**
	 * @param instance2filter - the single node to filter
	 * 
	 * unfilters a single node from its corresponding index tree
	 */
	private void unfilterTreeNode(SimpleTreeNode instance) {		
		TreeNode foundNode = this.getNode((ISEMOSSNode)instance.leaf);
		
		if(foundNode != null) {
			if(foundNode.filteredInstanceNode.remove(instance)) {
				foundNode.instanceNode.add(instance);
			}
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return true if node is filtered, false otherwise
	 */
	public boolean isFiltered(SimpleTreeNode node) {
		//find the node in the index tree
		TreeNode tnode = this.getNode(((ISEMOSSNode)node.leaf));
		
		//Value does not exist
		if(tnode == null) {
			throw new IllegalArgumentException("Node with value "+node.leaf.getValue()+" does not exist");
		}
		
		//if node is contained within its index tree's filteredInstanceNode vector then the node is filtered
		if(tnode.filteredInstanceNode.contains(node)) {
			return true;
		} 
		
		//if the node is contained within its index tree's instanceNode vector then the node is NOT filtered
		else if(tnode.instanceNode.contains(node)) {
			return false;
		} 
		
		//else node instance cannot be found
		else {
			throw new IllegalArgumentException("Node not found");
		}
	}
	
	
//******************** END FILTER METHODS ************************//
	

//******************** GETTER METHODS ****************************//
	
	//TODO: make this better, not sure if lastAddedNode is reliable
	public SimpleTreeNode getRoot() {
		TreeNode root = nodeIndexHash.get(this.rootLevel);
		
		ValueTreeColumnIterator it = new ValueTreeColumnIterator(root);
		SimpleTreeNode rootNode = null;
		if(it.hasNext()) {
			rootNode = it.next();
			return SimpleTreeNode.getLeft(rootNode);
		} else {
			return null;
		}
	}
	
	public SimpleTreeNode getFilteredRoot() {
		return filteredRoot;
	}
	
	public int getNodeTypeCount(String nodeType) {
		int count = 0;
		TreeNode typeRoot = nodeIndexHash.get(nodeType);
		IndexTreeIterator it = new IndexTreeIterator(typeRoot);
		while(it.hasNext()) {
			it.next();
			count++;
		}
		return count;
	}
	
	
	public TreeNode getNode(ISEMOSSNode node)
	{
		TreeNode typeIndexRoot = nodeIndexHash.get(node.getType());
		TreeNode retNode = null;
		if(typeIndexRoot != null)
		{
			if(typeIndexRoot.leaf.isEqual(node)) {
				return typeIndexRoot;
			}
			else {
				Vector <TreeNode> rootNodeVector = new Vector<TreeNode>();
				rootNodeVector.add(typeIndexRoot);
				// find the node which has
				retNode = typeIndexRoot.getNode(rootNodeVector, new TreeNode(node), false);
			}
		}
		return retNode;
	}
	
	public Vector <String> findLevels()	{
		
		//if(finalChildType == null) return null;

		Vector <String> retVector = new Vector<String>();
		
		TreeNode aNode = nodeIndexHash.get(rootLevel);
		SimpleTreeNode sNode = new ValueTreeColumnIterator(aNode).next();//aNode.instanceNode.elementAt(0);
		while(sNode != null)
		{
			retVector.add(((ISEMOSSNode)sNode.leaf).getType());
			sNode = sNode.parent;
		}
		Collections.reverse(retVector);
		
		sNode = new ValueTreeColumnIterator(aNode).next().leftChild;
		while(sNode != null) {
			retVector.add(((ISEMOSSNode)sNode.leaf).getType());
			sNode = sNode.leftChild;
		}
		return retVector;
	}
	
	// gets all the semoss nodes i.e. the leafs within the simple tree node
	public Vector<ISEMOSSNode> getSInstanceNodes(String type)
	{
		TreeNode typeRoot = nodeIndexHash.get(type);
		Vector <ISEMOSSNode> retVector = new Vector<>();
		
		Iterator<SimpleTreeNode> it = new ValueTreeColumnIterator(typeRoot);
		while(it.hasNext()) {
			retVector.add((ISEMOSSNode)it.next().leaf);
		}
		return retVector;
	}
	
	public void setRootLevel(String root) {
		this.rootLevel = root;
	}
	
//	public Hashtable<SimpleTreeNode, Vector<SimpleTreeNode>> getPath(String fromType, String toType)
//	{
//		TreeNode typeRoot = nodeIndexHash.get(fromType);
//		Vector <TreeNode> searchVector = new Vector<TreeNode>();
//		searchVector.add(typeRoot);
//		// I need to write the logic to walk the tree and get all instances
//		// gets all the instances
//		Vector <SimpleTreeNode> allInstanceVector = typeRoot.getInstanceNodes(searchVector, new Vector<SimpleTreeNode>());
//		
//		Hashtable<SimpleTreeNode, Vector<SimpleTreeNode>> values = new Hashtable<SimpleTreeNode, Vector<SimpleTreeNode>>();
//		for(int instanceIndex = 0;instanceIndex < allInstanceVector.size();instanceIndex++)
//		{
//			
//			Vector <SimpleTreeNode> instanceVector = new Vector<SimpleTreeNode>();
//			SimpleTreeNode fromNode = allInstanceVector.elementAt(instanceIndex);
//			instanceVector.addElement(fromNode);
//			//SimpleTreeNode instanceNode = typeRoot.getInstances().elementAt(0);
//			//instanceNode = instanceNode.getLeft(instanceNode);
//			//instanceVector.add(instanceNode);
//			//Vector output = typeRoot.getInstanceNodes(rootVector, new Vector());
//			Vector <SimpleTreeNode> output = SimpleTreeNode.getChildsOfType(instanceVector, toType, new Vector<SimpleTreeNode>());
//			values.put(fromNode, output);
//			
//			// need to remove the duplicates
//			System.out.println("Total Number of instances are " + output.size() + output.elementAt(0).leaf.getKey());
//		}
//		System.out.println("Output values is " + values);
//		return values;
//	}

//******************** UNUSED METHODS **********************//
	public void addFilter(String nodeType, String filter)
	{
		Hashtable filters = new Hashtable();
		if(nodeTypeFilters.containsKey(nodeType))
			filters = nodeTypeFilters.get(nodeType);
		
		// put it in
		filters.put(filter, filter);
		
		// set the filter back
		nodeTypeFilters.put(nodeType, filters);
	}

	// add a filter so that it is not kept when flattening is done
	public void removeFilter(String nodeType, String filter)
	{
		Hashtable filters = new Hashtable();
		if(nodeTypeFilters.containsKey(nodeType))
			filters = nodeTypeFilters.get(nodeType);
		
		// put it in
		filters.remove(filter); //, filter);
		
		// set the filter back
		nodeTypeFilters.put(nodeType, filters);
	}
	
	// flatten for a particular type
	public Vector flattenFromType(String type)
	{
		// pick the type root from the index
		// Run through every node and add the instances to a vector
		// push to flatten roots on the SimpleTreeNode and flatten it
		System.out.println("Calling Flatten");
		TreeNode typeRoot = nodeIndexHash.get(type);
		SimpleTreeNode instanceNode = typeRoot.getInstances().elementAt(0);
		instanceNode = instanceNode.getLeft(instanceNode);
		Vector <SimpleTreeNode> rootVector = new Vector<SimpleTreeNode>();
		rootVector.add(instanceNode);
		//instanceNode.printNode(rootVector, true, 1);
		// the flatten takes the following arguments
		//the node / parent to flatten from, the output vector that will have the values
		// filter hashtable - optional
		Vector outputVector = new Vector();
		if(nodeTypeFilters.size() == 0)
			instanceNode.flattenRoots(instanceNode.getLeft(instanceNode),outputVector);
		else
			instanceNode.flattenRoots(instanceNode.getLeft(instanceNode),outputVector, nodeTypeFilters);			
		
		//Vector output = typeRoot.getInstanceNodes(rootVector, new Vector());
		//System.out.println("Total Number of instances are " + output.size());
		return null;
	}

	// flatten for a particular type .. until a particular type
	public Vector flattenFromType(String type, String untilType)
	{
		// pick the type root from the index
		// Run through every node and add the instances to a vector
		// push to flatten roots on the SimpleTreeNode and flatten it
		TreeNode typeRoot = nodeIndexHash.get(type);
		SimpleTreeNode instanceNode = typeRoot.getInstances().elementAt(0);
		instanceNode = instanceNode.getLeft(instanceNode);
		Vector <SimpleTreeNode> rootVector = new Vector<SimpleTreeNode>();
		rootVector.add(instanceNode);
		//instanceNode.printNode(rootVector, true, 1);
		Vector outputVector = new Vector();
		if(nodeTypeFilters.size() == 0)
			instanceNode.flattenRoots(instanceNode, untilType, outputVector);
		else
			instanceNode.flattenRoots(instanceNode, untilType, outputVector, nodeTypeFilters);
		//Vector output = typeRoot.getInstanceNodes(rootVector, new Vector());
		//System.out.println("Total Number of instances are " + output.size());
		return null;
	}
	
	// returns whether to terminate or move forward
	public boolean triggerNext()
	{
		if((runLevel + 1) >= engines.length)
		{
			System.out.println("Ending now.. " + finalChildType);
			runLevel++;
			System.out.println("Levels... " + findLevels());

			//finalChildType = "TYPE" + runLevel;
			Vector <ISEMOSSNode> childNodes = getSInstanceNodes(finalChildType);
			System.out.println("Number of child nodes... " + childNodes.size());
			return true;
		}
		else
		{
			runLevel++;
			finalChildType = "TYPE" + runLevel;
			// need to set the new set of seeds
			// reset seeds
			System.err.println("Now at run level.... " + runLevel + finalChildType);
			Vector <ISEMOSSNode> childNodes = getSInstanceNodes(finalChildType);
			System.out.println("Number of child nodes... " + childNodes.size());
			seeds = new ISEMOSSNode[childNodes.size()];
			for(int seedIndex = 0;seedIndex < childNodes.size();seedIndex++)
				seeds[seedIndex] = childNodes.elementAt(seedIndex);
			//seeds = (StringClass[])childNodes.toArray();
			execEngine();
			return false;
		}
	}
	
	public void execEngine()
	{
		int numProc = Runtime.getRuntime().availableProcessors();
		numProc = 8;
		int seedSplitter = seeds.length / numProc;
		
		String [] childTypeToGet = new String[1];
		childTypeToGet[0] = "IGNORE FOR NOW";
		//childTypeToGet[0] = childs[runLevel];
		int lastIndex = 0;
				
		for(int threadIndex = 0;threadIndex < numProc;threadIndex++)
		{
			int nextIndex = lastIndex + seedSplitter;
			// get the thread
			ISEMOSSNode [] curParents = Arrays.copyOfRange(seeds, lastIndex, nextIndex);
			TreeThreader threader = (TreeThreader) threadList.get(threadIndex);
			threader.setChildTypes(childTypeToGet);
			((SampleHashEngine)this.engines[runLevel]).parLevel = runLevel;
			((SampleHashEngine)this.engines[runLevel]).childLevel = (runLevel + 1);
			threader.setEngine(this.engines[runLevel]);
			threader.setParents(curParents);
			if(runLevel == 0) // first time
				service.execute(threader);
			lastIndex = nextIndex;
		}		
	}
	
	public long printTime()
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		long time1 = System.nanoTime();
		long mtime1 = System.currentTimeMillis();
		//System.out.println("Start >> Nano " + dateFormat.format(date));
		System.out.println("Start >> Nano " + time1);
		return mtime1;
	}

	//TODO: a way around passing in level names
	//TODO: currently only keep track of root, nothing else
	public SimpleTreeNode createTrailOfEmptyNodes(String[] levelNames, int desiredLength) {
		// see if empty node is at root
		int currLevel = 0;
		TreeNode rootOfRoots = this.nodeIndexHash.get(levelNames[currLevel]);
		ITreeKeyEvaluatable emptyVal = new StringClass(SimpleTreeNode.EMPTY, SimpleTreeNode.EMPTY, levelNames[currLevel]);
		TreeNode emptyNode = new TreeNode(emptyVal);
		
		Vector<TreeNode> searchForEmpty = new Vector<TreeNode>();
		searchForEmpty.add(rootOfRoots);
		TreeNode foundEmpty = rootOfRoots.getNode(searchForEmpty, emptyNode, false);
		
		SimpleTreeNode lastEmptyNode = null;
		// found empty node at root
		if(foundEmpty != null) {
			currLevel++;
			// loop through instances and see if any children are blanks
			Vector<SimpleTreeNode> emptyInstances = foundEmpty.getInstances();
			for(int i = 0; i < emptyInstances.size(); i++) {
				lastEmptyNode = findLastConnectedEmptyNode(emptyInstances.get(i), levelNames, currLevel);
				if(lastEmptyNode != null) {
					break;
				}
			}
			String type = ((ISEMOSSNode) lastEmptyNode.leaf).getType();
			currLevel = ArrayUtilityMethods.arrayContainsValueAtIndex(levelNames, type);
		} 
		else { // found no empty nodes, need to construct trail from root
			// define first level node
			lastEmptyNode = new SimpleTreeNode(emptyVal);
			
			// connect via value tree
			SimpleTreeNode firstInstance = new ValueTreeColumnIterator(rootOfRoots).next();
			SimpleTreeNode previousLeftMost = SimpleTreeNode.getLeft(firstInstance);
			previousLeftMost.leftSibling = lastEmptyNode;
			lastEmptyNode.rightSibling = previousLeftMost;
			
			// connect via index tree
			emptyNode.getInstances().add(lastEmptyNode);
			TreeNode root = rootOfRoots.insertData(emptyNode);
			this.nodeIndexHash.put(levelNames[currLevel], root);
		}
		
		currLevel++;
		// build tree to desired length
		for(int i = currLevel; i < desiredLength; i++) {
			emptyVal = new StringClass(SimpleTreeNode.EMPTY, SimpleTreeNode.EMPTY, levelNames[i]);
			emptyNode = new TreeNode(emptyVal);
			SimpleTreeNode newEmpty = new SimpleTreeNode(emptyVal);
			emptyNode.getInstances().add(newEmpty);

			if(lastEmptyNode.leftChild == null) {
				lastEmptyNode.leftChild = newEmpty;
				newEmpty.parent = lastEmptyNode;
			} else {
				SimpleTreeNode joiningChild = lastEmptyNode.leftChild;
				// adjust sibling rel
				joiningChild.leftSibling = newEmpty;
				newEmpty.rightSibling = joiningChild;
				// adjust parent child rel
				lastEmptyNode.leftChild = newEmpty;
				newEmpty.parent = lastEmptyNode;
			}
			lastEmptyNode = newEmpty;
			
			// update index tree
			TreeNode previousRoot = this.nodeIndexHash.get(levelNames[i]);
			TreeNode newRoot = previousRoot.insertData(emptyNode);
			this.nodeIndexHash.put(levelNames[i], newRoot);
		}
		
		return lastEmptyNode;
	}
	
	/**
	 * Recursively finds the last empty node in a list of empty nodes
	 * SimpleTreeNode passed is assumed to be an empty node
	 * @param emptyNode				The first empty node for the start of the list
	 * @param levelNames			The names of the types for creation of the empty nodes
	 * @param currLevel				The current level of the emptyNode parameter
	 * @return						The last node found in the list of empty nodes
	 */
	private static SimpleTreeNode findLastConnectedEmptyNode(SimpleTreeNode emptyNode, String[] levelNames, int currLevel) {
		StringClass emptyVal = new StringClass(SimpleTreeNode.EMPTY, SimpleTreeNode.EMPTY, levelNames[currLevel]);
		if(!emptyNode.leaf.isEqual(emptyVal)) {
			throw new IllegalArgumentException("The node being passed in must be an empty node.");
		}
		// create a new empty value
		SimpleTreeNode joiningNode = emptyNode.leftChild;
		// loop through the child of the empty to see if a child is empty
		while(joiningNode != null && !joiningNode.leaf.isEqual(emptyVal)) {
			joiningNode = joiningNode.rightSibling;
		}
		
		// if no empty child is found, return the last empty node
		if(joiningNode == null) {
			return emptyNode;
		} else {
			// if empty child is found, re-enter method
			SimpleTreeNode retNode = findLastConnectedEmptyNode(joiningNode, levelNames, ++currLevel);
			return retNode;
		}
	}

	
	public Hashtable<String, Hashtable<String, Integer>> getPath(String fromType, String toType)
	{
		return getPath(fromType, toType, null);
	}

	// gives it in the format of
	// fromTypeKey | toType Key <> Occurences
	// 			   | to Type Key 2 <> Occurences
	public Hashtable<String, Hashtable<String, Integer>> getPath(String fromType, String toType, String paths)
	{
		Vector <String> levels = findLevels();
		boolean flip = false;
		if(levels.indexOf(fromType) > levels.indexOf(toType))
			flip = true;		
		
		TreeNode typeRoot = nodeIndexHash.get(fromType);
		Vector <TreeNode> searchVector = new Vector<TreeNode>();
		searchVector.add(typeRoot);
		// I need to write the logic to walk the tree and get all instances
		// gets all the instances
		Vector <SimpleTreeNode> allInstanceVector = typeRoot.getInstanceNodes(searchVector, new Vector<SimpleTreeNode>());
		
		Hashtable<String, Hashtable<String,Integer>> values = new Hashtable<String, Hashtable<String, Integer>>();
		for(int instanceIndex = 0;instanceIndex < allInstanceVector.size();instanceIndex++)
		{
			
			Vector <SimpleTreeNode> instanceVector = new Vector<SimpleTreeNode>();
			SimpleTreeNode fromNode = allInstanceVector.elementAt(instanceIndex);
			instanceVector.addElement(fromNode);
			String key = fromNode.leaf.getKey();
			//SimpleTreeNode instanceNode = typeRoot.getInstances().elementAt(0);
			//instanceNode = instanceNode.getLeft(instanceNode);
			//instanceVector.add(instanceNode);
			//Vector output = typeRoot.getInstanceNodes(rootVector, new Vector());
			Hashtable <String, Integer> output = new Hashtable<String, Integer>();
			if(values.containsKey(key))
				output = values.get(key);
			if(paths == null)
			{
				if(!flip)
					output = SimpleTreeNode.getChildsOfTypeCount(instanceVector, toType, output);
				else
					output = SimpleTreeNode.getParentsOfTypeCount(instanceVector, toType, output, true);
				//System.out.println(" >> " + key + "   " + output);				
				values.put(key, output);
			}
			else
			{
				// need to take care of flip
				if(!flip)
					values = SimpleTreeNode.getChildOfTypeCountMulti(instanceVector, paths, toType, values, new Hashtable <String, String>());
				else
					values = SimpleTreeNode.getParentOfTypeCountMulti(instanceVector, paths, toType, values, new Hashtable <String, String>());
			}
			
			//System.out.println("Values...  " + values);
			// need to remove the duplicates
			//System.out.println("Total Number of instances are " + output.size() + output.elementAt(0).leaf.getKey());
		}
		//System.out.println("Output values is " + values);
		
		//if(flip) // I had flipped it originally
		//	values = flipPath(values);
		return values;
	}
	
	public Hashtable<String, Hashtable<String, Integer>> flipPath(Hashtable <String, Hashtable<String, Integer>> data)
	{
		Hashtable <String, Hashtable<String, Integer>> retTable = new Hashtable <String, Hashtable<String, Integer>>();
		Enumeration <String> baseKeys = data.keys();
		
		while(baseKeys.hasMoreElements())
		{
			String baseKey = baseKeys.nextElement();
			Hashtable <String, Integer> mainData = data.get(baseKey);
			
			Hashtable <String, Integer> newData = new Hashtable<String, Integer>();
			Enumeration <String> newKeys = mainData.keys();
			
			while(newKeys.hasMoreElements())
			{
				String newKey = newKeys.nextElement();
				
				if(retTable.containsKey(newKey)) // get the table if it is already there in the flipped table
					newData = retTable.get(newKey);
				
				int count = 0;
				if(newData.containsKey(baseKey)) // baseKey is what I am flipping if the basekey value is already there on the flip
					count = newData.get(baseKey);
				
				count = count + mainData.get(newKey);
				
				newData.put(baseKey, count);
				
				retTable.put(newKey, newData);
			}
		}
		
		return retTable;
	}
	

	// need to also do it.. when there is more than one
	// i.e. this could be X1, X2 and X3 etc. 
	// so it is a list of parents and with it 1 child
	// every single time I need to do a new one
	public Hashtable<String, Hashtable<String, Integer>> getPaths(String [] fromType, String toType)
	{
		// 
		
		TreeNode typeRoot = nodeIndexHash.get(fromType);
		Vector <TreeNode> searchVector = new Vector<TreeNode>();
		searchVector.add(typeRoot);
		// I need to write the logic to walk the tree and get all instances
		// gets all the instances
		Vector <SimpleTreeNode> allInstanceVector = typeRoot.getInstanceNodes(searchVector, new Vector<SimpleTreeNode>());
		
		Hashtable<String, Hashtable<String,Integer>> values = new Hashtable<String, Hashtable<String, Integer>>();
		for(int instanceIndex = 0;instanceIndex < allInstanceVector.size();instanceIndex++)
		{
			
			Vector <SimpleTreeNode> instanceVector = new Vector<SimpleTreeNode>();
			SimpleTreeNode fromNode = allInstanceVector.elementAt(instanceIndex);
			instanceVector.addElement(fromNode);
			//SimpleTreeNode instanceNode = typeRoot.getInstances().elementAt(0);
			//instanceNode = instanceNode.getLeft(instanceNode);
			//instanceVector.add(instanceNode);
			//Vector output = typeRoot.getInstanceNodes(rootVector, new Vector());
			Hashtable <String, Integer> output = SimpleTreeNode.getChildsOfTypeCount(instanceVector, toType, new Hashtable<String, Integer>());
			values.put(fromNode.leaf.getKey(), output);
			
			// need to remove the duplicates
			//System.out.println("Total Number of instances are " + output.size() + output.elementAt(0).leaf.getKey());
		}
		System.out.println("Output values is " + values);
		return values;
	}
	
	public Hashtable<String, Integer> getNodeConfig(String type)
	{
		TreeNode typeRoot = nodeIndexHash.get(type);
		Vector <TreeNode> searchVector = new Vector<TreeNode>();
		searchVector.add(typeRoot);
		// I need to write the logic to walk the tree and get all instances
		// gets all the instances
		Vector <SimpleTreeNode> allInstanceVector = typeRoot.getInstanceNodes(searchVector, new Vector<SimpleTreeNode>());
		
		Hashtable<String,Integer> values = new Hashtable<String, Integer>();
		int grandTotal = 0;
		
		Vector <String> levels = findLevels();
		
		for(int instanceIndex = 0;instanceIndex < allInstanceVector.size();instanceIndex++)
		{
			SimpleTreeNode thisNode = allInstanceVector.elementAt(instanceIndex);
			String val = thisNode.leaf.getKey();
			// get the hashtable
			int total = 0;
			if(values.containsKey(val))
			{
				total = values.get(val);
				grandTotal = grandTotal - total;
			}
			
			// I need to check if this is the last level I am looking
			
			if(thisNode.childCount.size() > 0)
			{
				Iterator <Integer> vals = thisNode.childCount.values().iterator();
				while(vals.hasNext())
				{
					total = total + vals.next();
				}
			}
			else if(!levels.get(levels.size()-1).equals(type))
			{
				// this is not the last level
				total = 1; // atleast 1 node
			}
			else
			{
				// this is the last level run analysis for it
				SimpleTreeNode parent = thisNode.parent;
				if(parent.childCount.containsKey(val))
					total = total + parent.childCount.get(val);
				//System.out.println("To Be Determined");
			}
			grandTotal = grandTotal + total;
			values.put(val, total);
		}
		values.put("total", grandTotal);
		System.out.println("Output values is " + values);
		return values;
	}
	
	public void addSimple()
	{
		addNode(new StringClass("AnatomicPath", "BP"), new StringClass("Modify Referrals", "Activity"));
		addNode(new StringClass("AnatomicPath", "BP"), new StringClass("Modify Referrals", "Activity"));
		addNode(new StringClass("AnatomicPath", "BP"), new StringClass("Modify Referrals", "Activity"));
		addNode(new StringClass("AnatomicPath", "BP"), new StringClass("Modify Orders", "Activity"));
		addNode(new StringClass("AnatomicPath", "BP"), new StringClass("Procurement", "Activity"));
		
		addNode(new StringClass("Lab", "Cap"), new StringClass("AnatomicPath", "BP"));
		addNode(new StringClass("Lab", "Cap"), new StringClass("ClinicalPath", "BP"));
		//flattenFromType("Cap");
		
		//System.out.println("Number of nodes " + getSInstanceNodes("Activity").size());
		
		//System.err.println("-----");
		//adjustType("BP", false);
		//flattenFromType("Cap");
		
		//System.err.println("-----");
		addNode(new StringClass("ClinicalPath", "BP"), new StringClass("Procurement", "Activity"));
		//flattenFromType("Cap");
		
		//System.err.println("-----");
		
		addNode(new StringClass("Modify Referrals", "Activity"), new StringClass("Orders", "DO"));
		addNode(new StringClass("Modify Referrals", "Activity"), new StringClass("Referrals", "DO"));
		addNode(new StringClass("Modify Referrals", "Activity"), new StringClass("Patients", "DO"));
		addNode(new StringClass("ClinicalPath", "BP"), new StringClass("Procurement2", "Activity"));
		
		//adjustType("Activity", false);
		
		//addNode(new StringClass("Procurement", "Activity"), new StringClass("Eligibility", "BLU"));
		//addNode(new StringClass("Modify Referrals", "Activity"), new StringClass("Eligibility", "BLU"));
		addNode(new StringClass("Patients", "DO"), new StringClass("Eligibility32", "BLUMEAWAY"));
		adjustType("DO", false);
		//addNode(new StringClass("Eligibility", "BLU"), new StringClass("Eligibility33", "TRY"));
		addNode(new StringClass("Lab", "Cap"), new StringClass("HOLYCow", "TRY2"));
		
		//adjustType("DO", false);
		
		//flattenFromType("Cap");
		//getInstances("Cap");
		//getPath("BP", "TRY2");
		addNode(new StringClass("Lab", "Cap"), new StringClass("PK", "BP"));
		//adjustType("BP", true);
		//System.err.println("After Adjustments.... for a single node");
		//flattenFromType("Cap");
		//removeNode(new StringClass("Referrals", "DO"));
		//removeType("DO"); // not removing the last one
		//addNode(new StringClass("Modify Referrals", "Activity"), new StringClass("Eligibility", "BLU"));
		//System.out.println("Flattening ");
		//flattenFromType("Cap");
		//System.err.println("-----");
		
		//flattenFromType("Cap", "Activity");

	}
	
	public static void main(String [] args)
	{
		SimpleTreeBuilder builder = new SimpleTreeBuilder("Yo");
		//SimpleTreeBuilder builder2 = new SimpleTreeBuilder();
		
		//System.out.println(Runtime.getRuntime().availableProcessors());
		
		
		
		builder.addSimple();
		builder.append(builder.getRoot(), builder.getRoot());
		
		//builder.addStress2();
		//builder.multiEngineAdd();
	}
	
}

