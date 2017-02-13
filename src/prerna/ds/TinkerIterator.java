package prerna.ds;


import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

public class TinkerIterator implements Iterator {
	private TinkerBaseIterator it;

	public TinkerIterator(GraphTraversal gt, List<String> selectors, QueryStruct qs) {
		it = new TinkerBaseIterator(gt, selectors, qs);
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public Object[] next() {
		return it.next();
	}

}
