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
package prerna.ui.components.playsheets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.jgrapht.Graph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.SimpleGraph;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

import prerna.om.SEMOSSEdge;
import prerna.om.GraphDataModel;
import prerna.om.SEMOSSVertex;
import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.AbstractEngine;
import prerna.rdf.engine.impl.InMemoryJenaEngine;
import prerna.rdf.engine.impl.InMemorySesameEngine;
import prerna.rdf.engine.impl.RDFFileSesameEngine;
import prerna.rdf.engine.impl.SesameJenaConstructStatement;
import prerna.rdf.engine.impl.SesameJenaConstructWrapper;
import prerna.rdf.engine.impl.SesameJenaSelectCheater;
import prerna.rdf.engine.impl.SesameJenaUpdateWrapper;
import prerna.ui.components.ControlData;
import prerna.ui.components.ControlPanel;
import prerna.ui.components.GraphOWLHelper;
import prerna.ui.components.LegendPanel2;
import prerna.ui.components.NewHoriScrollBarUI;
import prerna.ui.components.NewScrollBarUI;
import prerna.ui.components.PropertySpecData;
import prerna.ui.components.RDFEngineHelper;
import prerna.ui.components.VertexColorShapeData;
import prerna.ui.components.VertexFilterData;
import prerna.ui.components.specific.tap.DataLatencyPlayPopup;
import prerna.ui.main.listener.impl.GraphNodeListener;
import prerna.ui.main.listener.impl.GraphPlaySheetListener;
import prerna.ui.main.listener.impl.PickedStateListener;
import prerna.ui.main.listener.impl.PlaySheetColorShapeListener;
import prerna.ui.main.listener.impl.PlaySheetControlListener;
import prerna.ui.main.listener.impl.PlaySheetOWLListener;
import prerna.ui.transformer.ArrowDrawPaintTransformer;
import prerna.ui.transformer.ArrowFillPaintTransformer;
import prerna.ui.transformer.EdgeArrowStrokeTransformer;
import prerna.ui.transformer.EdgeLabelFontTransformer;
import prerna.ui.transformer.EdgeLabelTransformer;
import prerna.ui.transformer.EdgeStrokeTransformer;
import prerna.ui.transformer.EdgeTooltipTransformer;
import prerna.ui.transformer.VertexIconTransformer;
import prerna.ui.transformer.VertexLabelFontTransformer;
import prerna.ui.transformer.VertexLabelTransformer;
import prerna.ui.transformer.VertexPaintTransformer;
import prerna.ui.transformer.VertexShapeTransformer;
import prerna.ui.transformer.VertexStrokeTransformer;
import prerna.ui.transformer.VertexTooltipTransformer;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.JenaSesameUtils;
import prerna.util.QuestionPlaySheetStore;
import prerna.util.Utility;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.BasicRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;

/**
 */
public class GraphPlaySheet extends AbstractRDFPlaySheet {

	/*
	 * this will have references to the following a. Internal Frame that needs to be displayed b. The panel of
	 * parameters c. The composed SPARQL Query d. Perspective selected e. The question selected by the user f. Filter
	 * criterias including slider values
	 */
	GraphDataModel gdm = new GraphDataModel();
	public DelegateForest forest = null;
	public VisualizationViewer <SEMOSSVertex, SEMOSSEdge> view = null;
	protected String layoutName = Constants.FR;
	Layout layout2Use = null;
	public LegendPanel2 legendPanel = null;
	public JPanel cheaterPanel = new JPanel();
	public JTabbedPane jTab = new JTabbedPane();
//	public Vector edgeVector = new Vector();
	public JInternalFrame dataLatencyPopUp = null;
	public DataLatencyPlayPopup dataLatencyPlayPopUp = null;
	public ControlData controlData = new ControlData();
	public PropertySpecData predData = new PropertySpecData();
	SimpleGraph <SEMOSSVertex, SEMOSSEdge> graph = new SimpleGraph<SEMOSSVertex, SEMOSSEdge>(SEMOSSEdge.class);

	public VertexColorShapeData colorShapeData = new VertexColorShapeData();
	public VertexFilterData filterData = new VertexFilterData();
	
	boolean sudowl, search, prop;
	
	//So that it doesn't get reset on extend and overlay etc. it must be stored
	VertexLabelFontTransformer vlft;
	EdgeLabelFontTransformer elft;
	VertexShapeTransformer vsht;
	

	public ControlPanel searchPanel;
	
	public JSplitPane graphSplitPane;

	/**
	 * Constructor for GraphPlaySheet.
	 */
	public GraphPlaySheet()
	{
		logger.info("Graph PlaySheet " + query);
	}
	
	/**
	 * Method setAppend.
	 * @param append boolean
	 */
	@Override
	public void setAppend(boolean append) {
		logger.debug("Append set to " + append);
		//writeStatus("Append set to  : " + append);
		this.overlay = append;
		gdm.setOverlay(append);
	}
	
	public boolean getSudowl(){
		return sudowl;
	}
	
	public GraphDataModel getGraphData(){
		return gdm;
	}
	
	public void setGraphData(GraphDataModel gdm){
		this.gdm = gdm;
	}
	
	private void setPropSudowlSearch(){
		sudowl = Boolean.parseBoolean(DIHelper.getInstance().getProperty(Constants.GPSSudowl));
		prop = Boolean.parseBoolean(DIHelper.getInstance().getProperty(Constants.GPSProp));
		search = Boolean.parseBoolean(DIHelper.getInstance().getProperty(Constants.GPSSearch));
		gdm.setPropSudowlSearch(prop, sudowl, search);
	}

	/**
	 * Method createView.
	 */
	public void createView() {
		if(gdm.rc==null){
			String questionID = getQuestionID();
			// fill the nodetype list so that they can choose from
			// remove from store
			// this will also clear out active sheet
			QuestionPlaySheetStore.getInstance().remove(questionID);
			if(QuestionPlaySheetStore.getInstance().isEmpty())
			{
				JButton btnShowPlaySheetsList = (JButton) DIHelper.getInstance().getLocalProp(
						Constants.SHOW_PLAYSHEETS_LIST);
				btnShowPlaySheetsList.setEnabled(false);
			}
			Utility.showError("Query returned no results.");
			return;
		}
		super.createView();
		
		this.setPreferredSize(new Dimension(1000,750));

		searchPanel=new ControlPanel(search);

		try {
			// get the graph query result and paint it
			// need to get all the vertex transformers here

			// create initial panel
			// addInitialPanel();
			// execute the query now
			setAppend(false);
			
			//writeStatus(" Starting create view");
			getForest();
			
			
			addInitialPanel();

			addToMainPane(pane);
			showAll();
				
			updateProgressBar("60%...Processing RDF Statements	", 60);
			
			logger.debug("Executed the select");
			createForest();
			createLayout();
			processView();
			updateProgressBar("100%...Graph Generation Complete", 100);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.fatal(e.getStackTrace());
		}
	}
	
	/**
	 * Method processView.
	 */
	public void processView()
	{
		
		createVisualizer();
		updateProgressBar("80%...Creating Visualization", 80);
		
		addPanel();
		try {
			this.setSelected(false);
			this.setSelected(true);
			printConnectedNodes();
			printSpanningTree();
			//logger.debug("model size: " +rc.size());
		} catch (Exception e) {
			// TODO: Specify exception
			e.printStackTrace();
		}
	}
	
	/**
	 * Method undoView.
	 */
	public void undoView()
	{
		// get the latest and undo it
		// Need to find a way to keep the base relationships
		try {
			if(gdm.modelCounter > 1)
			{
				updateProgressBar("30%...Getting Previous Model", 30);
				gdm.undoView();
				filterData = new VertexFilterData();
				controlData = new ControlData();
				predData = new PropertySpecData();
				updateProgressBar("50%...Graph Undo in Progress", 50);
				
				refineView();
				logger.info("model size: " + gdm.rc.size());
			}
			this.setSelected(false);
			this.setSelected(true);
			printConnectedNodes();
			printSpanningTree();

			genAllData();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		updateProgressBar("100%...Graph Undo Complete", 100);
	}

	
    /**
     * Method redoView.
     */
    public void redoView() {
        try {
               if(gdm.rcStore.size() > gdm.modelCounter-1)
               {
                     updateProgressBar("30%...Getting Previous Model", 30);
                     gdm.redoView();
                     updateProgressBar("50%...Graph Redo in Progress", 50);
                     refineView();
                     
               }
               this.setSelected(false);
               this.setSelected(true);
               printConnectedNodes();
               printSpanningTree();
        }catch (PropertyVetoException e) {
        	e.printStackTrace();
        }
        updateProgressBar("100%...Graph Redo Complete", 100);
    }

    public void overlayView(){
		try
		{
//			semossGraph.rc.commit();
			createForest();
			
			//add to overall modelstore
			
			boolean successfulLayout = createLayout();
			if(!successfulLayout){
				Utility.showMessage("Current layout cannot handle the extend. Resetting to " + Constants.FR + " layout...");
				layoutName = Constants.FR;
				createLayout();
			}
			
			processView();
			gdm.processTraverseCourse();
			setUndoRedoBtn();
			updateProgressBar("100%...Graph Extension Complete", 100);
		}catch(Exception ex)
		{
			ex.printStackTrace();
			logger.fatal(ex);
		}
    }
	
	/**
	 * Method removeView.
	 */
	public void removeView()
	{
		gdm.removeView(query, engine);
		//sc.addStatement(vf.createURI("<http://semoss.org/ontologies/Concept/Service/tom2>"),vf.createURI("<http://semoss.org/ontologies/Relation/Exposes>"),vf.createURI("<http://semoss.org/ontologies/Concept/BusinessLogicUnit/tom1>"));
		logger.debug("\nSPARQL: " + query);
		//tq.setIncludeInferred(true /* includeInferred */);
		//tq.evaluate();

		gdm.fillStoresFromModel();
		updateProgressBar("80%...Creating Visualization", 80);

		refineView();
		logger.debug("Removing Forest Complete >>>>>> ");
		updateProgressBar("100%...Graph Remove Complete", 100);
	}
	

	
	/**
	 * Method refineView.
	 */
	public void refineView()
	{
		try {
			getForest();
			gdm.fillStoresFromModel();
			createForest();
			logger.info("Refining Forest Complete >>>>>");
			
			// create the specified layout
			createLayout();
			// identify the layout specified for this perspective
			// now create the visualization viewer and we are done
			createVisualizer();
			
			// add the panel
			addPanel();
			// addpane
			// addpane
			legendPanel.drawLegend();
			//showAll();
			//progressBarUpdate("100%...Graph Refine Complete", 100);
			setUndoRedoBtn();
		} catch (Exception e) {
			// TODO: Specify exception
			e.printStackTrace();
		}
	}
	

	/**
	 * Method refreshView.
	 */
	public void refreshView(){
		createVisualizer();
		// add the panel
		addPanel();
		try {
			this.setSelected(false);
			this.setSelected(true);
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		
		//showAll();
	}

	/**
	 * Method addInitialPanel.
	 */
	public void addInitialPanel()
	{
		setWindow();
		// create the listener and add the frame
		// JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
		// frame.setPreferredSize(new Dimension(400,600));
		// if there is a view remove it
		// get
		GraphPlaySheetListener gpListener = new GraphPlaySheetListener();
		PlaySheetControlListener gpControlListener = new PlaySheetControlListener();
		PlaySheetOWLListener gpOWLListener = new PlaySheetOWLListener();
		PlaySheetColorShapeListener gpColorShapeListener = new PlaySheetColorShapeListener();
		
		this.addInternalFrameListener(gpListener);
		this.addInternalFrameListener(gpControlListener);
		this.addInternalFrameListener(gpOWLListener);
		this.addInternalFrameListener(gpColorShapeListener);

		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{728, 0};
		gridBagLayout.rowHeights = new int[]{557, 70, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);

	
		cheaterPanel.setPreferredSize(new Dimension(800, 70));
		GridBagLayout gbl_cheaterPanel = new GridBagLayout();
		gbl_cheaterPanel.columnWidths = new int[]{0, 0};
		gbl_cheaterPanel.rowHeights = new int[]{60, 0};
		gbl_cheaterPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_cheaterPanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		cheaterPanel.setLayout(gbl_cheaterPanel);

		legendPanel = new LegendPanel2();
		legendPanel.setPreferredSize(new Dimension(800,50));
		GridBagConstraints gbc_legendPanel = new GridBagConstraints();
		gbc_legendPanel.fill = GridBagConstraints.BOTH;
		gbc_legendPanel.gridx = 0;
		gbc_legendPanel.gridy = 0;
		cheaterPanel.add(legendPanel, gbc_legendPanel);
		
		jBar.setStringPainted(true);
		jBar.setString("0%...Preprocessing");
		jBar.setValue(0);
		resetProgressBar();
       
       // SwingUtilities.updateComponentTreeUI(jBar);
		GridBagConstraints gbc_jBar = new GridBagConstraints();
		gbc_jBar.anchor = GridBagConstraints.NORTH;
		gbc_jBar.fill = GridBagConstraints.HORIZONTAL;
		gbc_jBar.gridx = 0;
		gbc_jBar.gridy = 1;
		cheaterPanel.add(jBar, gbc_jBar);
		GridBagConstraints gbc_cheaterPanel = new GridBagConstraints();
		gbc_cheaterPanel.anchor = GridBagConstraints.NORTH;
		gbc_cheaterPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_cheaterPanel.gridx = 0;
		gbc_cheaterPanel.gridy = 1;
		this.getContentPane().add(cheaterPanel, gbc_cheaterPanel);
		
		GridBagConstraints gbc_jTab = new GridBagConstraints();
		gbc_jTab.anchor = GridBagConstraints.NORTH;
		gbc_jTab.fill = GridBagConstraints.BOTH;
		gbc_jTab.gridx = 0;
		gbc_jTab.gridy = 0;
		this.getContentPane().add(jTab, gbc_jTab);
		graphSplitPane = new JSplitPane();

		graphSplitPane.setEnabled(false);
		graphSplitPane.setOneTouchExpandable(true);
		graphSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		searchPanel.setPlaySheet(this);

		

	}
	
	/**
	 * Method addPanel.
	 */
	protected void addPanel() {
		try
		{
			// add the model to search panel
			if (search)
			{
				searchPanel.searchCon.indexStatements(gdm.getJenaModel());
			}
			//graphSplitPane.removeAll();
			//graphPanel.setLayout(new BorderLayout());
			GraphZoomScrollPane gzPane = new GraphZoomScrollPane(view);
			gzPane.getVerticalScrollBar().setUI(new NewScrollBarUI());
			gzPane.getHorizontalScrollBar().setUI(new NewHoriScrollBarUI());
//			GridBagLayout gbl_graphPanel = new GridBagLayout();
//			gbl_graphPanel.columnWidths = new int[]{0, 0};
//			gbl_graphPanel.rowHeights = new int[]{0, 0, 0};
//			gbl_graphPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
//			gbl_graphPanel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
//			graphPanel.setLayout(gbl_graphPanel);
//			
//			GridBagConstraints gbc_search = new GridBagConstraints();
//			gbc_search.insets = new Insets(0, 0, 5, 0);
//			gbc_search.fill = GridBagConstraints.BOTH;
//			gbc_search.gridx = 0;
//			gbc_search.gridy = 0;
			graphSplitPane.setLeftComponent(searchPanel);
			
//			GridBagConstraints gbc_panel_2 = new GridBagConstraints();
//			gbc_panel_2.fill = GridBagConstraints.BOTH;
//			gbc_panel_2.gridx = 0;
//			gbc_panel_2.gridy = 1;
			graphSplitPane.setRightComponent(gzPane);	
			
			this.addComponentListener(
					new ComponentAdapter(){
						public void componentResized(ComponentEvent e){
							logger.info(((JInternalFrame)e.getComponent()).isMaximum());
							GraphPlaySheet gps = (GraphPlaySheet) e.getSource();
							if(!layoutName.equals(Constants.TREE_LAYOUT))
								layout2Use.setSize(view.getSize());
							logger.info("Size: " + gps.view.getSize());
							
						}
					});
	
			legendPanel.data = filterData;
			legendPanel.drawLegend();
			logger.info("Adding graph tab");
			boolean setSelected = jTab.getSelectedIndex()==0;
			jTab.insertTab("Graph", null, graphSplitPane, null, 0);
			if(setSelected) jTab.setSelectedIndex(0);
			logger.info("Add Panel Complete >>>>>");
		}catch(Exception ex)
		{
			
		}
	}

	/**
	 * Method addToMainPane.
	 * @param pane JComponent
	 */
	protected void addToMainPane(JComponent pane) {

		pane.add((Component)this);

		logger.info("Adding Main Panel Complete");
	}

	/**
	 * Method showAll.
	 */
	public void showAll() {
		this.pack();
		this.setVisible(true);
		//JFrame frame2 = (JFrame) DIHelper.getInstance().getLocalProp(
	//			Constants.MAIN_FRAME);
		//frame2.repaint();

	}

	/**
	 * Method createVisualizer.
	 */
	protected void createVisualizer() {
		//tree layout cannot set size
		if(!layoutName.equals(Constants.TREE_LAYOUT))
			this.layout2Use.setSize(new Dimension(this.getContentPane().getWidth()-15, this.getContentPane().getHeight()-cheaterPanel.getHeight()-(int)searchPanel.getPreferredSize().getHeight()-50));
		view = new VisualizationViewer(this.layout2Use);
		view.setPreferredSize(this.layout2Use.getSize());
		view.setBounds(10000000, 10000000, 10000000, 100000000);

		Renderer r = new BasicRenderer();
		
		view.setRenderer(r);
		//view.getRenderer().setVertexRenderer(new MyRenderer());

		GraphNodeListener gl = new GraphNodeListener();
		view.setGraphMouse(new GraphNodeListener());
		// DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
		gl.setMode(ModalGraphMouse.Mode.PICKING);
		view.setGraphMouse(gl);
		VertexLabelTransformer vlt = new VertexLabelTransformer(controlData);
		VertexPaintTransformer vpt = new VertexPaintTransformer();
		VertexTooltipTransformer vtt = new VertexTooltipTransformer(controlData);
		EdgeLabelTransformer elt = new EdgeLabelTransformer(controlData);
		EdgeTooltipTransformer ett = new EdgeTooltipTransformer(controlData);
		EdgeStrokeTransformer est = new EdgeStrokeTransformer();
		VertexStrokeTransformer vst = new VertexStrokeTransformer();
		ArrowDrawPaintTransformer adpt = new ArrowDrawPaintTransformer();
		EdgeArrowStrokeTransformer east = new EdgeArrowStrokeTransformer();
		ArrowFillPaintTransformer aft = new ArrowFillPaintTransformer();
		PickedStateListener psl = new PickedStateListener(view);
		//keep the stored one if possible
		if(vlft==null)
			vlft = new VertexLabelFontTransformer();
		if(elft==null)
			elft = new EdgeLabelFontTransformer();
		if(vsht==null)
			vsht = new VertexShapeTransformer();
		else vsht.emptySelected();
		VertexIconTransformer vit = new VertexIconTransformer();
		
		//view.getRenderContext().getGraphicsContext().setStroke(s);

		Color color = view.getBackground();
		view.setBackground(Color.WHITE);
		color = view.getBackground();
		
		//view.setGraphMouse(mc);
		view.getRenderContext().setVertexLabelTransformer(
							vlt);
		view.getRenderContext().setEdgeLabelTransformer(
				elt);
		view.getRenderContext().setVertexStrokeTransformer(vst);
		view.getRenderContext().setVertexShapeTransformer(vsht);
		view.getRenderContext().setVertexFillPaintTransformer(
				vpt);
		view.getRenderContext().setEdgeStrokeTransformer(est);
		view.getRenderContext().setArrowDrawPaintTransformer(adpt);
		view.getRenderContext().setEdgeArrowStrokeTransformer(east);
		view.getRenderContext().setArrowFillPaintTransformer(aft);
		view.getRenderContext().setVertexFontTransformer(vlft);
		view.getRenderContext().setEdgeFontTransformer(elft);
		view.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
		view.getRenderContext().setLabelOffset(0);
		//view.getRenderContext().set;
		// view.getRenderContext().setVertexIconTransformer(new DBCMVertexIconTransformer());
		view.setVertexToolTipTransformer(vtt);
		view.setEdgeToolTipTransformer(ett);
		//view.getRenderContext().setVertexIconTransformer(vit);
		PickedState ps = view.getPickedVertexState();
		ps.addItemListener(psl);
		controlData.setViewer(view);

		searchPanel.setViewer(view);
		logger.info("Completed Visualization >>>> ");
	}

	/**
	 * Method createLayout.
	 * @return boolean
	 */
	public boolean createLayout() {
		int fail = 0;
		// creates the layout
		// Constructor cons = Class.forName(layoutName).getConstructor(this.forest.class);
		// layout2Use = (Layout)cons.newInstance(forest);
		logger.info("Create layout >>>>>> ");
		Class layoutClass = (Class)DIHelper.getInstance().getLocalProp(layoutName);
		//layoutClass.getConstructors()
		Constructor constructor=null;
		try{
			constructor = layoutClass.getConstructor(edu.uci.ics.jung.graph.Forest.class);
			layout2Use  = (Layout)constructor.newInstance(forest);
		}catch(Exception e){
			fail++;
			logger.info(e);
		}
		try{
			constructor = layoutClass.getConstructor(edu.uci.ics.jung.graph.Graph.class);
			layout2Use  = (Layout)constructor.newInstance(forest);
		}catch(Exception e){
			fail++;
			logger.info(e);
		}
		searchPanel.setGraphLayout(layout2Use);
		//= (Layout) new FRLayout((forest));
		logger.info("Create layout Complete >>>>>> ");
		if(fail==2) {
			return false;
		}
		else return true;
	}
	
	/**
	 * Method getLayoutName.
	 * @return String
	 */
	public String getLayoutName(){
		return layoutName;
	}
	
	
	/**
	 * Method createForest.
	 */
	protected void createForest() throws Exception
	{
		// need to take the base information from the base query and insert it into the jena model
		// this is based on EXTERNAL ontology
		// then take the ontology and insert it into the jena model
		// (may be eventually we can run this through a reasoner too)
		// Now insert our base model into the same ontology
		// Now query the model for 
		// Relations - Paint the basic graph
		// Now find a way to get all the predicate properties from them
		// Hopefully the property is done using subproperty of
		// predicates - Pick all the predicates but for the properties
		// paint them
		// properties
		// and then paint it appropriately
		logger.debug("creating the in memory jena model");


		Hashtable<String, String> filteredNodes = filterData.filterNodes;
		logger.warn("Filtered Nodes " + filteredNodes);
		
		//use edge store to add all edges to forest
		logger.info("Adding edges from edgeStore to forest");

		Hashtable<String, SEMOSSVertex> vertStore = gdm.getVertStore();
		Hashtable<String, SEMOSSEdge> edgeStore = gdm.getEdgeStore();
		Iterator<String> edgeIt = edgeStore.keySet().iterator();
		while(edgeIt.hasNext()){
			String edgeURI = edgeIt.next();
			SEMOSSEdge edge = edgeStore.get(edgeURI);
			SEMOSSVertex outVert = edge.outVertex;
			SEMOSSVertex inVert = edge.inVertex;
				if ((filteredNodes == null) || (filteredNodes != null && !filteredNodes.containsKey(inVert.getURI())
						&& !filteredNodes.containsKey(outVert.getURI()) && !filterData.edgeFilterNodes.containsKey(edge.getURI()))){
				//add to forest
				forest.addEdge(edge, outVert, inVert);
				processControlData(edge);
				
				//add to filter data
				filterData.addEdge(edge);
				
				//add to pred data
				predData.addPredicateAvailable(edge.getURI());
				predData.addConceptAvailable(inVert.getURI());
				predData.addConceptAvailable(outVert.getURI());
				
				//add to simple graph
				graph.addVertex(outVert);
				graph.addVertex(inVert);
				graph.addEdge(outVert, inVert, edge);
			}
		}
		logger.info("Done with edges... checking for isolated nodes");
		//now for vertices--process control data and add what is necessary to the graph
		//use vert store to check for any isolated nodes and add to forest
		Collection<SEMOSSVertex> verts = vertStore.values();
		for(SEMOSSVertex vert : verts)
		{
			if((filteredNodes == null) || (filteredNodes != null && !filteredNodes.containsKey(vert.getURI()))){
				processControlData(vert);
				filterData.addVertex(vert);
				if(!forest.containsVertex(vert)){
					forest.addVertex(vert);
					graph.addVertex(vert);
				}
			}
		}
		logger.info("Done with forest creation");

		genAllData();
		
		// first execute all the predicate selectors
		// Backdoor entry
		Thread thread = new Thread(){
			public void run()
			{
				printAllRelationship();				
			}
		};
		thread.start();
//		modelCounter++;
//shouldn't this be in create data?
		logger.info("Creating Forest Complete >>>>>> ");										
	}
	
	/**
	 * Method exportDB.
	 */
	public void exportDB() 
	{
		try {
			gdm.baseRelEngine.exportDB();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}	
    /**
     * Method printAllRelationship.
     */
    public void printAllRelationship()
    {
          String conceptHierarchyForSubject = "SELECT DISTINCT ?Subject ?Predicate ?Object WHERE " +
          "{" +
          "{?Subject ?Predicate ?Object}" + 
          "}";
          logger.debug(conceptHierarchyForSubject);
          
          IEngine jenaEngine = new InMemorySesameEngine();
          ((InMemorySesameEngine)jenaEngine).setRepositoryConnection(gdm.rc);
          
          SesameJenaConstructWrapper sjsc;
          
          if(query.toUpperCase().contains("CONSTRUCT"))
                sjsc = new SesameJenaConstructWrapper();
          else
                sjsc = new SesameJenaSelectCheater();

          // = new SesameJenaSelectCheater();
          sjsc.setEngine(jenaEngine);
          logger.warn("<<<<");
          String end = "";
          
                while(!end.equalsIgnoreCase("end"))
                {
                      try {
                      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                      logger.debug("Enter Query");
                      String query2 = reader.readLine();                    
                      end = query2;
                      logger.debug("Query is " + query2);
                      if(query2.toUpperCase().contains("CONSTRUCT"))
                            sjsc = new SesameJenaConstructWrapper();
                      else
                            sjsc = new SesameJenaSelectCheater();

                      // = new SesameJenaSelectCheater();
                      sjsc.setEngine(jenaEngine);
                      sjsc.setQuery(query);//conceptHierarchyForSubject);
                      sjsc.setQuery(query2);
                      sjsc.execute();
                      while(sjsc.hasNext())
                      {
                            // read the subject predicate object
                            // add it to the in memory jena model
                            // get the properties
                            // add it to the in memory jena model
                            SesameJenaConstructStatement st = sjsc.next();
                            logger.warn(st.getSubject() + "<<>>" + st.getPredicate() + "<<>>" + st.getObject());
                            //addToJenaModel(st);
                      }
                      } catch (Exception e) {
                            // TODO: Specify exception
                            e.printStackTrace();
                      }
                }

          
    }

	
	/**
	 * Method setUndoRedoBtn.
	 */
	private void setUndoRedoBtn()
	{
		if(gdm.modelCounter>1)
		{
			searchPanel.undoBtn.setEnabled(true);
		}
		else
		{
			searchPanel.undoBtn.setEnabled(false);
		}
		if(gdm.rcStore.size()>=gdm.modelCounter)
		{
			searchPanel.redoBtn.setEnabled(true);
		}
		else
		{
			searchPanel.redoBtn.setEnabled(false);
		}
	}
	
	/**
	 * Method setDataLatencyPopUp.
	 * @param dataLate JInternalFrame
	 */
	public void setDataLatencyPopUp(JInternalFrame dataLate){
		dataLatencyPopUp = dataLate;
	}

	/**
	 * Method setDataLatencyPlayPopUp.
	 * @param dataLate DataLatencyPlayPopup
	 */
	public void setDataLatencyPlayPopUp(DataLatencyPlayPopup dataLate){
		dataLatencyPlayPopUp = dataLate;
	}
	
	/**
	 * Method getFilterData.
	 * @return VertexFilterData
	 */
	public VertexFilterData getFilterData() {
		return filterData;
	}

	/**
	 * Method getColorShapeData.
	 * @return VertexColorShapeData
	 */
	public VertexColorShapeData getColorShapeData() {
		return colorShapeData;
	}

	/**
	 * Method getControlData.
	 * @return ControlData
	 */
	public ControlData getControlData() {
		return controlData;
	}

	

	/**
	 * Method getForest.
	 * @return DelegateForest
	 */
	public DelegateForest getForest() {
		forest = new DelegateForest();
//		semossGraph.graph = new SimpleGraph<SEMOSSVertex, SEMOSSEdge>(SEMOSSEdge.class);
		return forest;
	}

	/**
	 * Method setForest.
	 * @param forest DelegateForest
	 */
	public void setForest(DelegateForest forest) {
		this.forest = forest;
	}
	

	/**
	 * Method setLayout.
	 * @param layout String
	 */
	public void setLayout(String layout) {
		this.layoutName = layout;
	}

	/**
	 * Method getGraph.
	 * @return Graph
	 */
	public Graph getGraph()
	{
		return graph;
	}
	
	/**
	 * Method getView.
	 * @return VisualizationViewer
	 */
	public VisualizationViewer getView()
	{
		return view;
	}
	
	/**
	 * Method printConnectedNodes.
	 */
	protected void printConnectedNodes()
	{
		logger.info("In print connected Nodes routine " );
		ConnectivityInspector ins = new ConnectivityInspector(graph);
		logger.info("Number of vertices " + graph.vertexSet().size() + "<>" + graph.edgeSet().size());
		logger.info(" Graph Connected ? " + ins.isGraphConnected());
		//writeStatus("Graph Connected ? " + ins.isGraphConnected());
		logger.info("Number of connected sets are " + ins.connectedSets().size());
		Iterator <Set<SEMOSSVertex>> csIterator = ins.connectedSets().iterator();
		while(csIterator.hasNext())
		{
			Set <SEMOSSVertex> vertSet = csIterator.next();
			Iterator <SEMOSSVertex> si = vertSet.iterator();
			while(si.hasNext())
			{
				SEMOSSVertex vert = si.next();
				//logger.info("Set " + count + ">>>> " + vert.getProperty(Constants.VERTEX_NAME));
			}
		}	
	}	
	
	/**
	 * Method printSpanningTree.
	 */
	protected void printSpanningTree()
	{
		logger.info("In Spanning Tree " );
		KruskalMinimumSpanningTree<SEMOSSVertex, SEMOSSEdge> ins = new KruskalMinimumSpanningTree<SEMOSSVertex, SEMOSSEdge>(graph);
		
		logger.info("Number of vertices " + graph.vertexSet().size());
		logger.info(" Edges  " + ins.getEdgeSet().size());
		Iterator <SEMOSSEdge> csIterator = ins.getEdgeSet().iterator();
		int count = 0;
		while(csIterator.hasNext())
		{
				SEMOSSEdge vert = csIterator.next();
				logger.info("Set " + count + ">>>> " + vert.getProperty(Constants.EDGE_NAME));
		}
		count++;
	}	
	
	/**
	 * Method setRC.
	 * @param rc RepositoryConnection
	 */
	public void setRC(RepositoryConnection rc)
	{
		this.gdm.rc=rc;
	}
	
	/**
	 * Method getRC.
	 * @param rc RepositoryConnection
	 */
	public RepositoryConnection getRC()
	{
		return gdm.rc;
	}
	
	/**
	 * Method getEdgeLabelFontTransformer.
	 * @return EdgeLabelFontTransformer
	 */
	public EdgeLabelFontTransformer getEdgeLabelFontTransformer(){
		return elft;
	}

	/**
	 * Method getVertexLabelFontTransformer.
	 * @return VertexLabelFontTransformer
	 */
	public VertexLabelFontTransformer getVertexLabelFontTransformer(){
		return vlft;
	}
	
	/**
	 * Method resetTransformers.
	 */
	public void resetTransformers(){

		EdgeStrokeTransformer tx = (EdgeStrokeTransformer)view.getRenderContext().getEdgeStrokeTransformer();
		tx.setEdges(null);
		ArrowDrawPaintTransformer atx = (ArrowDrawPaintTransformer)view.getRenderContext().getArrowDrawPaintTransformer();
		atx.setEdges(null);
		EdgeArrowStrokeTransformer east = (EdgeArrowStrokeTransformer)view.getRenderContext().getEdgeArrowStrokeTransformer();
		east.setEdges(null);
		VertexShapeTransformer vst = (VertexShapeTransformer)view.getRenderContext().getVertexShapeTransformer();
		vst.setVertexSizeHash(new Hashtable());
		
		if(searchPanel.btnHighlight.isSelected()){
			VertexPaintTransformer ptx = (VertexPaintTransformer)view.getRenderContext().getVertexFillPaintTransformer();
			Hashtable searchVertices = new Hashtable();
			searchVertices.putAll(searchPanel.searchCon.cleanResHash);
			ptx.setVertHash(searchVertices);
			VertexLabelFontTransformer vfl = (VertexLabelFontTransformer)view.getRenderContext().getVertexFontTransformer();
			vfl.setVertHash(searchVertices);
		}
		else{
			VertexPaintTransformer ptx = (VertexPaintTransformer)view.getRenderContext().getVertexFillPaintTransformer();
			ptx.setVertHash(null);
			VertexLabelFontTransformer vfl = (VertexLabelFontTransformer)view.getRenderContext().getVertexFontTransformer();
			vfl.setVertHash(null);
		}
	}
	
	// removes existing concepts 
	/**
	 * Method removeExistingConcepts.
	 * @param subVector Vector<String>
	 */
	public void removeExistingConcepts(Vector <String> subVector)
	{

		for(int remIndex = 0;remIndex < subVector.size();remIndex++)
		{
			try {
				String remQuery = subVector.elementAt(remIndex);
				logger.warn("Removing query " + remQuery);
				
				Update update = gdm.rc.prepareUpdate(QueryLanguage.SPARQL, remQuery);
				update.execute();
				this.gdm.baseRelEngine.execInsertQuery(remQuery);
			
			} catch (Exception e) {
				// TODO: Specify exception
				e.printStackTrace();
			}
		}
	}
	
	// adds existing concepts 
	/**
	 * Method addNewConcepts.
	 * @param subjects String
	 * @param baseObject String
	 * @param predicate String
	 * @return String
	 */
	public String addNewConcepts(String subjects, String baseObject, String predicate)
	{
		
		StringTokenizer tokenz = new StringTokenizer(subjects, ";");
		
		String listOfChilds = null;
		
		while(tokenz.hasMoreTokens())
		{
			String adder = tokenz.nextToken();
			
			String parent = adder.substring(0,adder.indexOf("@@"));
			String child = adder.substring(adder.indexOf("@@") + 2);
			
			if(listOfChilds == null)
				listOfChilds = child;
			else
			listOfChilds = listOfChilds + ";" + child;
			
			SesameJenaConstructStatement st = new SesameJenaConstructStatement();
			st.setSubject(child);
			st.setPredicate(predicate);
			st.setObject(baseObject);
			gdm.addToSesame(st,true, true);
			
			logger.info(" Query....  " + parent + "<>" + child);	
		}
		return listOfChilds;
	}

	@Override
	public Object getData() {
		Hashtable returnHash = new Hashtable();
		returnHash.put("nodes", gdm.getVertStore());
		returnHash.put("edges", gdm.getEdgeStore().values());
		
		return returnHash;
	}

	/**
	 * Method genAllData.
	 */
	public void genAllData()
	{
		filterData.fillRows();
		filterData.fillEdgeRows();
		controlData.generateAllRows();
		if(sudowl)
			predData.genPredList();
		colorShapeData.setTypeHash(filterData.typeHash);
		colorShapeData.setCount(filterData.count);
		colorShapeData.fillRows();
	}

	@Override
	public void runAnalytics() {
		// TODO Auto-generated method stub
		
	}
	
	private void processControlData(SEMOSSEdge edge){
		String edgeType = edge.getProperty(Constants.EDGE_TYPE)+"";
		for(String prop : edge.getPropertyKeys()){
			controlData.addProperty(edgeType, prop);
		}
	}
	
	private void processControlData(SEMOSSVertex vert){
		String vertType = vert.getProperty(Constants.VERTEX_TYPE)+"";
		for(String prop : vert.getPropertyKeys()){
			controlData.addProperty(vertType, prop);
		}
	}

	@Override
	public void createData() {
		setPropSudowlSearch();
		gdm.createModel(query, engine);

		logger.info("Creating the base Graph");
		gdm.fillStoresFromModel();
	}

	/**
	 * Method getPredicateData.
	 * @return PropertySpecData
	 */
	public PropertySpecData getPredicateData() {
		return predData;
	}
	
}
