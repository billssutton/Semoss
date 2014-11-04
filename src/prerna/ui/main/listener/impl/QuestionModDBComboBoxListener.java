package prerna.ui.main.listener.impl;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.QuestionAdministrator;
import prerna.ui.components.ParamComboBox;
import prerna.ui.components.api.IChakraListener;
import prerna.ui.helpers.EntityFillerForSubClass;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.PlaySheetEnum;

public class QuestionModDBComboBoxListener implements IChakraListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		JComboBox<String> questionModDBComboBox = (JComboBox<String>)e.getSource();
		String engineName = (String) questionModDBComboBox.getSelectedItem();
		IEngine engine = (IEngine)DIHelper.getInstance().getLocalProp(engineName);
		JRadioButton addQuestionModType = (JRadioButton) DIHelper.getInstance().getLocalProp(Constants.ADD_QUESTION_BUTTON);

		//populate layout combobox
		JComboBox playSheetComboBox = (JComboBox)DIHelper.getInstance().getLocalProp(Constants.QUESTION_MOD_PLAYSHEET_COMBOBOXLIST);
		playSheetComboBox.removeAllItems();
		
		playSheetComboBox.insertItemAt("*Custom_PlaySheet", 0);
		
		ArrayList playsheetArray = PlaySheetEnum.getAllSheetNames();
		for(int i = 0; i < playsheetArray.size(); i++){
			playSheetComboBox.addItem(playsheetArray.get(i));
		}
		
		//entity filler; this will populate the parameter combobox for users to select a parameter they want to bind
		ArrayList<JComboBox> boxes = new ArrayList<JComboBox>();
		ParamComboBox addParameterComboBox = (ParamComboBox) DIHelper.getInstance().getLocalProp(Constants.QUESTION_ADD_PARAMETER_COMBO_BOX);
		boxes.add(addParameterComboBox);
		
		EntityFillerForSubClass entityFillerSC = new EntityFillerForSubClass();
		entityFillerSC.boxes = boxes;
		entityFillerSC.engine = engine;
		entityFillerSC.parent = "Concept";
		Thread aThread = new Thread(entityFillerSC);
		aThread.start();

		QuestionAdministrator.selectedEngine = engineName;
		
		//get the perspectives and store it
		Vector<String> perspectives = engine.getPerspectives();
		Collections.sort(perspectives);
		
		JComboBox<String> box = (JComboBox<String>)DIHelper.getInstance().getLocalProp(Constants.QUESTION_PERSPECTIVE_SELECTOR);
		box.removeAllItems();
		
		//populates perspectives based on selected db
		for(int itemIndex = 0;itemIndex < perspectives.size(); itemIndex++) {
			box.addItem(perspectives.get(itemIndex).toString());
		}
		if (addQuestionModType.isSelected()){
			box.insertItemAt("*NEW Perspective", 0);
		}
		box.setSelectedIndex(0);
	}

	@Override
	public void setView(JComponent view) {
		// TODO Auto-generated method stub
		
	}

}
