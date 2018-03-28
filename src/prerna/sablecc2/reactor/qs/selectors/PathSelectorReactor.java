package prerna.sablecc2.reactor.qs.selectors;

import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;

public class PathSelectorReactor extends AbstractReactor {

	/**
	 * This class is meant to be used for getting a query column selector
	 * When we have a path that we are trying to get 
	 * Example : JSON parsing
	 */
	
	public PathSelectorReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.COLUMN.getKey()};
	}
	
	@Override
	public NounMetadata execute() {
		organizeKeys();
		String pathInput = this.keyValue.get(this.keysToGet[0]);
		QueryColumnSelector pSelector = new QueryColumnSelector();
		pSelector.setTable("PATH");
		pSelector.setColumn(pathInput);
		return new NounMetadata(pSelector, PixelDataType.COLUMN);
	}

}
