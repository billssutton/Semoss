package prerna.sablecc2.reactor.qs.selectors;

import java.util.ArrayList;
import java.util.List;

import prerna.algorithm.api.ITableDataFrame;
import prerna.query.querystruct.AbstractQueryStruct;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.query.querystruct.selectors.QueryFunctionHelper;
import prerna.query.querystruct.selectors.QueryFunctionSelector;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.qs.AbstractQueryStructReactor;

public class AggregateAllReactor extends AbstractQueryStructReactor {

	private static final String MATH_KEY = "math";
	private static final String IGNORE_COLUMN_KEY = "ignoreCols";

	// Frame(df) | AggregateAll(math='sum', ignoreCols=['groupby']) | Collect() ;
	public AggregateAllReactor() {
		this.keysToGet = new String[] { MATH_KEY, IGNORE_COLUMN_KEY };
	}

	@Override
	protected AbstractQueryStruct createQueryStruct() {
		String mathFunction = getKey(MATH_KEY);

		List<String> ignoreCols = getIgnoreCols(IGNORE_COLUMN_KEY);
		//parse the math operation to a valid query function
		mathFunction = QueryFunctionHelper.getPrettyName(mathFunction);
		
		// get the frame
		ITableDataFrame dataFrame = this.qs.getFrame();
		String tableName = dataFrame.getName();

		// get columns
		String[] cols = dataFrame.getColumnHeaders();
		// iterate through the columns
		for (String col : cols) {
			//check for ignored cols
			if(!ignoreCols.contains(col)) {
				// perform the aggregation
				QueryFunctionSelector fun = new QueryFunctionSelector();
				fun.setFunction(mathFunction);
				fun.addInnerSelector(new QueryColumnSelector(tableName +"__"+ col));
				fun.setAlias(col);
				this.qs.addSelector(fun);
			} else {
				this.qs.addSelector(new QueryColumnSelector(tableName +"__"+ col));
			}
		}
		return qs;
	}
	
	private String getKey(String key) {
		GenRowStruct keyGrs = this.store.getNoun(key);
		if(keyGrs != null && !keyGrs.isEmpty()) {
			List<NounMetadata> inputs = keyGrs.getNounsOfType(PixelDataType.CONST_STRING);
			if(inputs != null && !inputs.isEmpty()) {
				return (String) inputs.get(0).getValue();
			}
		}

		List<NounMetadata> inputs = this.curRow.getNounsOfType(PixelDataType.CONST_STRING);
		if(inputs != null && !inputs.isEmpty()) {
			return (String) inputs.get(0).getValue();
		}

		throw new IllegalArgumentException("Invalid key value");
	}
	
	private List<String> getIgnoreCols(String key) {
		List<String> cols = new ArrayList<>();
		GenRowStruct keyGrs = this.store.getNoun(key);
		if (keyGrs != null && !keyGrs.isEmpty()) {
			List<NounMetadata> inputs = keyGrs.getNounsOfType(PixelDataType.CONST_STRING);
			if (inputs != null && !inputs.isEmpty()) {
				for (int i = 0; i < inputs.size(); i++) {
					cols.add((String) inputs.get(i).getValue());
				}
				return cols;
			}
		}

		List<NounMetadata> inputs = this.curRow.getNounsOfType(PixelDataType.CONST_STRING);
		if (inputs != null && !inputs.isEmpty()) {
			for (int i = 0; i < inputs.size(); i++) {
				cols.add((String) inputs.get(i).getValue());
			}
			return cols;
		}

		return cols;
	}	
}
