package prerna.ds.shared;

import prerna.algorithm.api.SemossDataType;
import prerna.engine.api.IEngine;
import prerna.engine.api.IHeadersDataRow;
import prerna.engine.api.IRawSelectWrapper;

public class RawCachedWrapper implements IRawSelectWrapper {

	CachedIterator iterator = null;
	
	@Override
	public void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQuery(String query) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getQuery() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cleanUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEngine(IEngine engine) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return iterator.hasNext();
	}

	@Override
	public IHeadersDataRow next() {
		// TODO Auto-generated method stub
		return iterator.next();
	}

	@Override
	public String[] getHeaders() {
		// TODO Auto-generated method stub
		return iterator.getHeaders();
	}

	@Override
	public SemossDataType[] getTypes() {
		// TODO Auto-generated method stub
		return iterator.colTypes;
	}

	@Override
	public long getNumRows() {
		return iterator.getInitSize();
	}

	@Override
	public long getNumRecords() {
		return iterator.getInitSize() * getHeaders().length;
	}
	
	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	public void setIterator(CachedIterator pi) {
		// TODO Auto-generated method stub
		this.iterator = pi;
	}

	@Override
	public IEngine getEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public boolean first()
	{
		return iterator.first;
	}
	
	public CachedIterator getIterator()
	{
		if(this.iterator == null)
			iterator = new CachedIterator();
		return iterator;
	}
	
}
