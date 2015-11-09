package prerna.ui.components.playsheets;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.ui.components.GridScrollPane;
import prerna.ui.components.NewScrollBarUI;
import prerna.util.Constants;
import prerna.util.DIHelper;

public class ScatterPlotMatrixPlaySheet extends BrowserPlaySheet{

	private static final Logger LOGGER = LogManager.getLogger(ScatterPlotMatrixPlaySheet.class.getName());	

	/**
	 * Constructor for ScatterPlotMatrixPlaySheet.
	 */
	public ScatterPlotMatrixPlaySheet() {
		super();
		this.setPreferredSize(new Dimension(800,600));
		String workingDir = DIHelper.getInstance().getProperty(Constants.BASE_FOLDER);
		fileName = "file://" + workingDir + "/html/MHS-RDFSemossCharts/app/scatter-plot-matrix.html";
	}
	
	public void processQueryData()
	{		
		Hashtable<String, Object> allHash = new Hashtable<String, Object>();
		allHash.put("one-row", false);
		allHash.put("dataTableAlign", getDataTableAlign());
		allHash.put("names", getNames());
		allHash.put("dataSeries", getList());

		this.dataHash = allHash;
	}
	
	@Override
	public Hashtable<String, String> getDataTableAlign() {
		Hashtable<String, String> alignHash = new Hashtable<String, String>();
		String[] names = dataFrame.getColumnHeaders();
		for(int i = 0; i <names.length; i++){
			alignHash.put("dim " + i, names[i]);
		}
		return alignHash;
	}

	@Override
	public void createData() {
		if(dataFrame == null || dataFrame.isEmpty()) {
			super.createData();
		}
		
		String[] names = dataFrame.getColumnHeaders();
		boolean[] numeric = dataFrame.isNumeric();
		List<String> ignoreCols = new ArrayList<String>();
		for(int i = 0; i < names.length; i++) {
			if(!numeric[i]) {
				ignoreCols.add(names[i]);
			}
		}
		dataFrame.setColumnsToSkip(ignoreCols);
	}

	/////////////////////////////SWING DEPENDENT CODE/////////////////////////////
	@Override
	public void addPanel() {
		if (jTab == null) {
			super.addPanel();
		} else {
			String lastTabName = jTab.getTitleAt(jTab.getTabCount() - 1);
			LOGGER.info("Parsing integer out of last tab name");
			int count = 1;
			if (jTab.getTabCount() > 1)
				count = Integer.parseInt(lastTabName.substring(0, lastTabName.indexOf("."))) + 1;
			addPanelAsTab(count + ". Scatter Plot Matrix Viz Data");
			addGridTab(count + ". Scatter Plot Matrix Raw Data");
		}
	}

	public void addGridTab(String tabName) {
		table = new JTable();
		GridScrollPane gsp = null;
		gsp = new GridScrollPane(getNames(), dataFrame.getData());
		gsp.addHorizontalScroll();
		jTab.addTab(tabName, gsp);
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

	public void setJTab(JTabbedPane jTab) {
		this.jTab = jTab;
	}

	public void setJBar(JProgressBar jBar) {
		this.jBar = jBar;
	}
}
