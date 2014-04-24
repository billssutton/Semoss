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
package prerna.ui.main.listener.specific.tap;

import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import prerna.ui.components.BooleanProcessor;
import prerna.ui.components.specific.tap.SysBPCapInsertProcessor;
import prerna.ui.main.listener.impl.AbstractListener;
import prerna.util.Constants;
import prerna.util.ConstantsTAP;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class SysBPCapInsertListener extends AbstractListener {

	Logger logger = Logger.getLogger(getClass());
	
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		//get the selected engine name
		JList list = (JList) DIHelper.getInstance().getLocalProp(Constants.REPO_LIST);
		String engineName = (String)list.getSelectedValue();
		
		//get the selected logic type
		JComboBox logicTypeBox = (JComboBox) DIHelper.getInstance().getLocalProp(ConstantsTAP.LOGIC_TYPE);
		String logicType = (String) logicTypeBox.getSelectedItem();
		
		//get selected threshold values and parse as a double
		JTextField dataObjectThresholdTextField = (JTextField) DIHelper.getInstance().getLocalProp(ConstantsTAP.DATA_OBJECT_THRESHOLD_VALUE_TEXT_BOX);
		String dataObjectThresholdTextValue = dataObjectThresholdTextField.getText();
		Double dataObjectThresholdValue = 0.0;
		JTextField bluThresholdTextField = (JTextField) DIHelper.getInstance().getLocalProp(ConstantsTAP.BLU_THRESHOLD_VALUE_TEXT_BOX);
		String bluThresholdTextValue = bluThresholdTextField.getText();
		Double bluThresholdValue = 0.0;	
		
		try{
			dataObjectThresholdValue = Double.parseDouble(dataObjectThresholdTextValue);
			bluThresholdValue = Double.parseDouble(bluThresholdTextValue);
			if ((dataObjectThresholdValue > 1.0) || (bluThresholdValue > 1.0) || (dataObjectThresholdValue < 0) || (bluThresholdValue < 0))  {
				throw new IllegalArgumentException("Threshold value must be between 1 and 0.");
			}
		}catch(Exception e){
			if (e instanceof NumberFormatException) {Utility.showError("All text values must be numbers."); }
			else Utility.showError(e.getMessage());
			return;
		}
				
		//send to processor
		logger.info("Inserting System-BP and System-Activity for Central Systems into " + engineName + "...");
		logger.info("Insert logic type " + logicType + " selected.");
		boolean success = false;
		String errorMessage = "";
		String isCalculatedQuery = "ASK WHERE { {?o <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/BusinessProcess> ;} {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System> ;} {?s ?p ?o ;} BIND(<http://semoss.org/ontologies/Relation/Contains/Calculated> AS ?contains) {?p ?contains ?prop ;} {?p <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Supports>} }";
		BooleanProcessor proc = new BooleanProcessor();
		proc.setQuery(isCalculatedQuery);
		JFrame playPane = (JFrame) DIHelper.getInstance().getLocalProp(Constants.MAIN_FRAME);
		boolean isCalculated = proc.processQuery();
				
		if(isCalculated){		
			Object[] buttons = {"Cancel Calculation", "Continue With Calculation"};
			int response = JOptionPane.showOptionDialog(playPane, "The selected RDF store (" + engineName + ") already " +
					"contains calculated relationships.  Would you like to recalculate?", 
					"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
			
			if (response == 1) {
				SysBPCapInsertProcessor insertProcessor = new SysBPCapInsertProcessor(dataObjectThresholdValue, bluThresholdValue, logicType);
				insertProcessor.setInsertCoreDB(engineName);
				insertProcessor.runDeleteQueries();
				success = insertProcessor.runCoreInsert();
				errorMessage = insertProcessor.getErrorMessage();
				if (!(errorMessage == "")) {
					Utility.showError(errorMessage);
				}
			}
			else return;
		}
		else {
			SysBPCapInsertProcessor insertProcessor = new SysBPCapInsertProcessor(dataObjectThresholdValue, bluThresholdValue, logicType);
			insertProcessor.setInsertCoreDB(engineName);
			success = insertProcessor.runCoreInsert();
			errorMessage = insertProcessor.getErrorMessage();
			if (!(errorMessage == "")) {
				Utility.showError(errorMessage);
			}
		}
		
		if(success)	{
			logger.info("Completed Insert.");
			Utility.showMessage("Insert Completed!");			
		}
	}
	
	@Override
	public void setView(JComponent view) {
			
	}
}
