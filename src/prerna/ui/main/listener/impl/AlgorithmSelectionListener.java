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
package prerna.ui.main.listener.impl;

import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.ui.components.playsheets.ClassifyClusterPlaySheet;

/**
 * Controls the algorithm to use, whether clustering or classifying. Tied to the JComboBox in the ClassifyClusterPlaySheet.
 */
public class AlgorithmSelectionListener extends AbstractListener {
	static final Logger logger = LogManager.getLogger(AlgorithmSelectionListener.class.getName());
	
	//given two panels, the cluster panel and the classify panel and determines which one to show based on what is clicked.
	private ClassifyClusterPlaySheet playSheet;
	
	/**
	 * Method actionPerformed.
	 * @param e ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		JComboBox<String> bx = (JComboBox<String>)e.getSource();
		String selection = bx.getSelectedItem() + "";
		if(selection.equals("Cluster") ) {
			playSheet.showClassify(false);
			playSheet.showCluster(true);
		}
		else if(selection.equals("Classify")){
			playSheet.showCluster(false);
			playSheet.showClassify(true);
		}

	}

	/**
	 * Method setView. Sets a JComponent that the listener will access and/or modify when an action event occurs.  
	 * @param view the component that the listener will access
	 */
	@Override
	public void setView(JComponent view) {
		this.playSheet = (ClassifyClusterPlaySheet)view;
	}

}
