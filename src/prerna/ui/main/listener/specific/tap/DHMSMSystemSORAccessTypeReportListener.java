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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import prerna.poi.main.DHMSMDataAccessLatencyFileImporter;
import prerna.poi.specific.ReportSheetWriter;
import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.SesameJenaSelectStatement;
import prerna.rdf.engine.impl.SesameJenaSelectWrapper;
import prerna.ui.components.api.IChakraListener;
import prerna.ui.components.specific.tap.DHMSMSysDecommissionReport;
import prerna.ui.components.specific.tap.DHMSMSystemSORAccessTypeReportProcessor;
import prerna.ui.components.specific.tap.SelectRadioButtonPanel;
import prerna.ui.components.specific.tap.SourceSelectPanel;
import prerna.ui.components.specific.tap.SystemTransitionOrganizer;
import prerna.util.Constants;
import prerna.util.ConstantsTAP;
import prerna.util.DIHelper;
import prerna.util.Utility;


/**
 * Listener for sourceReportGenButton
 */
public class DHMSMSystemSORAccessTypeReportListener implements IChakraListener {

	Logger logger = Logger.getLogger(getClass());
	
	/**
	 * Method actionPerformed.
	 * @param actionevent ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent actionevent) {

		//get data objects that are checked and put them in the data object hash
		Hashtable<String,String> dataLatencyTypeHash = new Hashtable<String,String>();
		Hashtable<String,String> dataAccessTypeHash = new Hashtable<String,String>();
		
//		JTextField dataAccessLatencyFileField = (JTextField) DIHelper.getInstance().getLocalProp(Constants.SELECT_DATA_ACCESS_FILE_JFIELD);
//		if(dataAccessLatencyFileField.getText()!=null&&dataAccessLatencyFileField.getText().length()>0)
//		{
//			DHMSMDataAccessLatencyFileImporter dataLatencyFileImporter = new DHMSMDataAccessLatencyFileImporter();
//			try {
//				dataLatencyFileImporter.importFile(dataAccessLatencyFileField.getText());
//				dataLatencyTypeHash = dataLatencyFileImporter.getDataLatencyTypeHash();
//				dataAccessTypeHash = dataLatencyFileImporter.getDataAccessTypeHash();
//			} catch (Exception e) {
//				JFrame playPane = (JFrame) DIHelper.getInstance().getLocalProp(Constants.MAIN_FRAME);
//				JOptionPane.showMessageDialog(playPane, "<html>Error with Selected File.</html>");
//				return;
//			}
//		}
//		else
//		{
			SelectRadioButtonPanel selectRadioPanel = (SelectRadioButtonPanel) DIHelper.getInstance().getLocalProp(Constants.SELECT_RADIO_PANEL);
			Enumeration<String> enumKey = selectRadioPanel.radioIntegratedBoxHash.keys();
			while(enumKey.hasMoreElements()) {
				    String key = enumKey.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioIntegratedBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataAccessTypeHash.put(key, "Integrated");
					}
			}
			Enumeration<String> enumKey2 = selectRadioPanel.radioHybridBoxHash.keys();
			while(enumKey2.hasMoreElements()) {
				    String key = enumKey2.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioHybridBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataAccessTypeHash.put(key, "Hybrid");
					}
			}
			Enumeration<String> enumKey3 = selectRadioPanel.radioManualBoxHash.keys();
			while(enumKey3.hasMoreElements()) {
				    String key = enumKey3.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioManualBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataAccessTypeHash.put(key, "Manual");
					}
			}
		
			
			Enumeration<String> enumKey4 = selectRadioPanel.radioRealBoxHash.keys();
			while(enumKey4.hasMoreElements()) {
				    String key = enumKey4.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioRealBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataLatencyTypeHash.put(key, "Real");
					}
			}
			Enumeration<String> enumKey5 = selectRadioPanel.radioNearBoxHash.keys();
			while(enumKey5.hasMoreElements()) {
				    String key = enumKey5.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioNearBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataLatencyTypeHash.put(key, "NearReal");
					}
			}
			Enumeration<String> enumKey6 = selectRadioPanel.radioArchiveBoxHash.keys();
			while(enumKey6.hasMoreElements()) {
				    String key = enumKey6.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioArchiveBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataLatencyTypeHash.put(key, "Archive");
					}
			}
			Enumeration<String> enumKey7 = selectRadioPanel.radioIgnoreBoxHash.keys();
			while(enumKey7.hasMoreElements()) {
				    String key = enumKey7.nextElement();
					JRadioButton radioButton = (JRadioButton) selectRadioPanel.radioIgnoreBoxHash.get(key);
					if (radioButton.isSelected())
					{
						dataLatencyTypeHash.put(key, "Ignore");
					}
			}
//		}
		
		if(dataLatencyTypeHash.isEmpty()&&dataAccessTypeHash.isEmpty())
		{
				JFrame playPane = (JFrame) DIHelper.getInstance().getLocalProp(Constants.MAIN_FRAME);
				JOptionPane.showMessageDialog(playPane, "<html>Please select at least one data object.</html>");
				return;
		}
		logger.info("Processed Data time Hash");
		
		DHMSMSystemSORAccessTypeReportProcessor sysSORReport = new DHMSMSystemSORAccessTypeReportProcessor();
		sysSORReport.setDataLatencyTypeHash(dataLatencyTypeHash);
		sysSORReport.setDataAccessTypeHash(dataAccessTypeHash);
		sysSORReport.runReport();

	}
	
	/**
	 * Override method from IChakraListener
	 * @param view
	 */
	@Override
	public void setView(JComponent view) {
	}

}
