package prerna.ui.components.playsheets;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyVetoException;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.learning.unsupervised.som.SelfOrganizingMap;
import prerna.algorithm.learning.unsupervised.som.SelfOrganizingMapGridViewer;
import prerna.rdf.engine.api.ISelectStatement;
import prerna.rdf.engine.api.ISelectWrapper;
import prerna.ui.components.NewScrollBarUI;
import prerna.ui.main.listener.impl.GridPlaySheetListener;
import prerna.ui.main.listener.impl.JTableExcelExportListener;
import prerna.util.Utility;

public class SelfOrganizingMapPlaySheet extends GridPlaySheet{
	
	private static final Logger LOGGER = LogManager.getLogger(SelfOrganizingMapPlaySheet.class.getName());

	private SelfOrganizingMap alg;
	private double[][] coordinates;
	
	private Double l0;
	private Double r0;
	private Double tau;
	private Integer maxIt;
	
	protected JTabbedPane jTab;
	
	private boolean includeXYPos;
	
	public SelfOrganizingMapPlaySheet() {
		super();
	}
	
	@Override
	public void createData() {
		generateData();
		runAlgorithm();
		processAlgorithm();
	}

	private void generateData() {
		if(query!=null) {
			list = new ArrayList<Object[]>();

			ISelectWrapper sjsw = Utility.processQuery(engine, query);
			names = sjsw.getVariables();
			int length = names.length;
			while(sjsw.hasNext()) {
				ISelectStatement sjss = sjsw.next();
				Object[] row = new Object[length];
				int i = 0;
				for(; i < length; i++) {
					row[i] = sjss.getVar(names[i]);
				}
				list.add(row);
			}
		}
	}

	public void runAlgorithm() {
		LOGGER.info("Creating apriori algorithm for instance: " + names[0]);
		alg = new SelfOrganizingMap(list, names);
		if(l0 != null) {
			alg.setL0(l0);
		}
		if(tau != null) {
			alg.setTau(tau);
		}
		if(r0 != null) {
			alg.setR0(r0);
		}
		if(maxIt != null) {
			alg.setMaxIt(maxIt);
		}
		boolean success = alg.execute();
		if(success == false) {
			Utility.showError("Error occured running SOM Algorithm!");
		}
	}
	
	public void processAlgorithm() {
		int[] gridAssignmentForInstance = alg.getGridAssignmentForInstances();
		
		int gridLength = alg.getLength();
		
		int i = 0;
		int numRows = list.size();
		int numColumns = list.get(0).length;
		int newSize = numColumns + 1;
		if(includeXYPos) {
			newSize += 2;
		}
		ArrayList<Object[]> retList = new ArrayList<Object[]>();
		for(; i < numRows; i++) {
			Object[] values = new Object[newSize];
			Object[] oldValues = list.get(i);
			int j = 0;
			for(; j < numColumns; j++) {
				values[j] = oldValues[j];
			}
			values[j] = gridAssignmentForInstance[i];
			if(includeXYPos) {
				j++;
				int[] cellPosition = SelfOrganizingMapGridViewer.getCoordinatesOfCell(gridAssignmentForInstance[i], gridLength);
				values[j] = cellPosition[0];
				j++;
				values[j] = cellPosition[1];
			}
			retList.add(values);
		}
		list = retList;
		
		i = 0;
		String[] retNames = new String[newSize];
		for(; i < numColumns; i++) {
			retNames[i] = names[i];
		}
		retNames[i] = "Cell";
		if(includeXYPos) {
			i++;
			retNames[i] = "X-Pos";
			i++;
			retNames[i] = "Y-Pos";
		}
		names = retNames;
	}
	
	@Override
	public void addPanel()
	{
		if(jTab==null) {
			super.addPanel();
		} else {
			String lastTabName = jTab.getTitleAt(jTab.getTabCount()-1);
			LOGGER.info("Parsing integer out of last tab name");
			int count = 1;
			if(jTab.getTabCount()>1)
				count = Integer.parseInt(lastTabName.substring(0,lastTabName.indexOf(".")))+1;
			addPanelAsTab(count+". Self Organizing Map");
		}
	}
	

	public void addPanelAsTab(String tabName) {
	//	setWindow();
		try {
			table = new JTable();
			
			//Add Excel export popup menu and menuitem
			JPopupMenu popupMenu = new JPopupMenu();
			JMenuItem menuItemAdd = new JMenuItem("Export to Excel");
			String questionTitle = this.getTitle();
			menuItemAdd.addActionListener(new JTableExcelExportListener(table, questionTitle));
			popupMenu.add(menuItemAdd);
			table.setComponentPopupMenu(popupMenu);
			
			GridPlaySheetListener gridPSListener = new GridPlaySheetListener();
			LOGGER.debug("Created the table");
			this.addInternalFrameListener(gridPSListener);
			LOGGER.debug("Added the internal frame listener ");
			//table.setAutoCreateRowSorter(true);
			
			JPanel panel = new JPanel();
			panel.add(table);
			GridBagLayout gbl_mainPanel = new GridBagLayout();
			gbl_mainPanel.columnWidths = new int[]{0, 0};
			gbl_mainPanel.rowHeights = new int[]{0, 0};
			gbl_mainPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
			gbl_mainPanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
			panel.setLayout(gbl_mainPanel);
			
			addScrollPanel(panel, table);
			
			jTab.addTab(tabName, panel);
			
			this.pack();
			this.setVisible(true);
			this.setSelected(false);
			this.setSelected(true);
			LOGGER.debug("Added new Self Organizing Map Sheet");
			
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
	}
	
	public Double getL0() {
		return l0;
	}
	public void setL0(Double l0) {
		this.l0 = l0;
	}
	public Double getR0() {
		return r0;
	}
	public void setR0(Double r0) {
		this.r0 = r0;
	}
	public Double getTau() {
		return tau;
	}
	public void setTau(Double tau) {
		this.tau = tau;
	}
	public double[][] getCoordinates() {
		return coordinates;
	}
	public void setCoordinates(double[][] coordinates) {
		this.coordinates = coordinates;
	}
	public Integer getMaxIt() {
		return maxIt;
	}
	public void setMaxIt(Integer maxIt) {
		this.maxIt = maxIt;
	}
	public SelfOrganizingMap getAlg() {
		return alg;
	}
	public void setAlg(SelfOrganizingMap alg) {
		this.alg = alg;
	}
	public boolean isIncludeXYPos() {
		return includeXYPos;
	}
	public void setIncludeXYPos(boolean includeXYPos) {
		this.includeXYPos = includeXYPos;
	}
	
	@Override
	public Object getVariable(String varName, ISelectStatement sjss){
		return sjss.getVar(varName);
	}
	public void setJTab(JTabbedPane jTab) {
		this.jTab = jTab;
	}
	public void setJBar(JProgressBar jBar) {
		this.jBar = jBar;
	}

	public void addScrollPanel(JPanel panel, JComponent obj) {
		JScrollPane scrollPane = new JScrollPane(obj);
		scrollPane.getVerticalScrollBar().setUI(new NewScrollBarUI());
		scrollPane.setAutoscrolls(true);

		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		panel.add(scrollPane, gbc_scrollPane);
	}

}
