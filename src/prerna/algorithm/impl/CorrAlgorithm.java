package prerna.algorithm.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import prerna.util.Constants;

public class CorrAlgorithm extends BaseReducer {

	@Override
	public void set(Iterator inputIterator, String[] ids, String script) {
		super.set(inputIterator, ids, script, null);
	}

	@Override
	public Object reduce() {
		// current correlation algorithm requires the full amount of data
		// can later look at algorithms to stream and compute an approximate
		// correlation value
		List<double[]> values = new Vector<double[]>();
		
		while(this.inputIterator.hasNext() && !errored) {
			Object data = this.inputIterator.next();
			double[] retObject = new double[ids.length];
			if(data instanceof Map) {
				for(int colIndex = 0;colIndex < ids.length; colIndex++) {
					Map<String, Object> mapData = (Map<String, Object>)data; //cast to map
					retObject[colIndex] = (double) ((Vertex)mapData.get(ids[colIndex])).property(Constants.NAME).value();
				}
			} else {
				for(int colIndex = 0; colIndex < ids.length; colIndex++) {
					retObject[colIndex] = ((Number) ((Object[]) data)[colIndex]).doubleValue();
				}
			}
			values.add(retObject);
		}
		
		//TODO: need to test that this conversion will work
		double[][] A = (double[][]) values.toArray(new double[][]{});
		PearsonsCorrelation correlation = new PearsonsCorrelation(A);
		double[][] correlationArray = correlation.getCorrelationMatrix().getData();	
		
		return correlationArray;
	}
	
	@Override
	public void setData(Iterator inputIterator, String[] ids, String script) {
		super.set(inputIterator, ids, script, null);
	}

	@Override
	public Object execute() {
		return reduce();
	}

}
