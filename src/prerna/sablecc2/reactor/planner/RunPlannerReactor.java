package prerna.sablecc2.reactor.planner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import prerna.sablecc2.Translation;
import prerna.sablecc2.lexer.Lexer;
import prerna.sablecc2.lexer.LexerException;
import prerna.sablecc2.node.Start;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PkslDataTypes;
import prerna.sablecc2.parser.Parser;
import prerna.sablecc2.parser.ParserException;
import prerna.sablecc2.reactor.PKSLPlanner;
import prerna.sablecc2.reactor.storage.InMemStore;
import prerna.sablecc2.reactor.storage.MapStore;

public class RunPlannerReactor extends AbstractPlannerReactor {

	private static final Logger LOGGER = LogManager.getLogger(LoadClient.class.getName());

	@Override
	public void In() {
		curNoun("all");
	}

	@Override
	public Object Out() {
		return parentReactor;
	}
	
	@Override
	public NounMetadata execute()
	{
		long start = System.currentTimeMillis();
		
		PKSLPlanner planner = getPlanner();
		// we use this just for debugging
		// this will get undefined variables and set them to 0
		// ideally, we should never have to do this...
//		List<String> pksls = getUndefinedVariablesPksls(planner);
		List<String> pksls = new Vector<String>();

		// get the list of the root vertices
		// these are the vertices we can run right away
		// and are the starting point for the plan execution
		Set<Vertex> rootVertices = getRootPksls(planner);
		// using the root vertices
		// iterate down all the other vertices and add the signatures
		// for the desired travels in the appropriate order
		// note: this is adding to the list of undefined variables
		// calculated at beginning of class 
		traverseDownstreamVertsAndOrderProcessing(rootVertices, pksls);
		
		Translation translation = new Translation();
		
//		String fileName = "C:\\Workspace\\Semoss_Dev\\failedpksls.txt";
//		BufferedWriter bw = null;
//		FileWriter fw = null;
//		
//		try {
//			fw = new FileWriter(fileName);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		bw = new BufferedWriter(fw);
		int count = 0;
		int total = 0;
		int numPksls = pksls.size();
		for(int pkslIndex = 0; pkslIndex < numPksls; pkslIndex++) {
			String pkslString = pksls.get(pkslIndex);
			try {
				Parser p = new Parser(new Lexer(new PushbackReader(new InputStreamReader(new ByteArrayInputStream(pkslString.getBytes("UTF-8"))))));
				Start tree = p.parse();
				tree.apply(translation);
//				bw.write("COMPLETE::: "+pkslString+"\n");
			} catch (ParserException | LexerException | IOException e) {
				count++;
//				e.printStackTrace();
//				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				System.out.println(pkslString);
//				try {
//					bw.write("PARSE ERROR::::   "+pkslString+"\n");
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			} catch(Exception e) {
				count++;
				e.printStackTrace();
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				System.out.println(e.getMessage());
				System.out.println(pkslString);
//				try {
//					bw.write("EVAL ERROR:::: " + e.getMessage()+"\n");
//					bw.write("EVAL ERROR:::: "+pkslString+"\n");
//					bw.write("\n");
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			}
			total++;
		}
		
//		try {
//			bw.close();
//			fw.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		LOGGER.info("****************    "+total+"      *************************");
		LOGGER.info("****************    "+count+"      *************************");
		
		InMemStore mapStore = getInMemoryStore();
		Set<String> variables = translation.planner.getVariables();
		for(String variable : variables) {
			mapStore.put(variable, translation.planner.getVariableValue(variable));
		}
		
		long end = System.currentTimeMillis();
		System.out.println("****************    "+(end - start)+"      *************************");
		
		return new NounMetadata(mapStore, PkslDataTypes.IN_MEM_STORE);
	}
	
	private InMemStore getInMemoryStore() {
		InMemStore inMemStore = null;
		GenRowStruct grs = getNounStore().getNoun(PkslDataTypes.IN_MEM_STORE.toString());
		if(grs != null) {
			inMemStore = (InMemStore) grs.get(0);
		}
		
		if(inMemStore == null) {
			return new MapStore();
		}
		return inMemStore;
	}
	
	private PKSLPlanner getPlanner() {
		GenRowStruct allNouns = getNounStore().getNoun(PkslDataTypes.PLANNER.toString());
		PKSLPlanner planner = null;
		if(allNouns != null) {
			planner = (PKSLPlanner) allNouns.get(0);
			return planner;
		} else {
			return this.planner;
		}
	}
}
