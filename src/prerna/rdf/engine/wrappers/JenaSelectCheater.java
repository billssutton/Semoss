package prerna.rdf.engine.wrappers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.query.BindingSet;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import prerna.rdf.engine.api.IConstructStatement;
import prerna.rdf.engine.api.IConstructWrapper;
import prerna.rdf.engine.impl.SesameJenaConstructStatement;

public class JenaSelectCheater extends AbstractWrapper implements
		IConstructWrapper {
	
	transient int count = 0;
	transient String [] var = null;
	transient int triples;
	transient int tqrCount=0;
	String queryVar[];
	transient ResultSet rs = null;


	@Override
	public IConstructStatement next() {
		
		IConstructStatement thisSt = new ConstructStatement();
	    logger.debug("Adding a JENA statement ");
	    QuerySolution row = rs.nextSolution();
	    thisSt.setSubject(row.get(var[0])+"");
	    thisSt.setPredicate(row.get(var[1])+"");
	    thisSt.setObject(row.get(var[2]));
	    
	    return thisSt;
	    
	}

	@Override
	public void execute() {
		try {
			rs = (ResultSet)engine.execSelectQuery(query);
			getVariables();
			
			processSelectVar();
			count=0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return 	rs.hasNext();

	}
	
	private String [] getVariables()
	{
		var = new String[rs.getResultVars().size()];
		List <String> names = rs.getResultVars();
		for(int colIndex = 0;
				colIndex < names.size();
				var[colIndex] = names.get(colIndex), colIndex++);
		return var;
	}
	
	public void processSelectVar()
	{
		if(query.contains("DISTINCT"))
		{
			Pattern pattern = Pattern.compile("SELECT DISTINCT(.*?)WHERE");
		    Matcher matcher = pattern.matcher(query);
		    String varString = null;
		    while (matcher.find()) 
		    {
		    	varString = matcher.group(1);
		    }
		    varString = varString.trim();
		    queryVar = varString.split(" ");
		    int num = queryVar.length+1;
		    triples = (int) Math.floor(num/3);
		}
		else
		{
			Pattern pattern = Pattern.compile("SELECT (.*?)WHERE");
		    Matcher matcher = pattern.matcher(query);
		    String varString = null;
		    while (matcher.find()) {
		        varString = matcher.group(1);
		    }
		    varString = varString.trim();
		    queryVar = varString.split(" ");
		    int num = queryVar.length+1;
		    triples = (int) Math.floor(num/3);
		}
	}


}
