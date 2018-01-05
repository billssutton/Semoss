package prerna.sablecc2.reactor.qs.selectors;

import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.selectors.IQuerySelector;
import prerna.query.querystruct.selectors.QueryAggregationEnum;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;

public class MinReactor extends QuerySelectReactor {
	
	@Override
	protected QueryStruct2 createQueryStruct() {
		QueryAggregationEnum aggregationFunction = QueryAggregationEnum.MIN;
		GenRowStruct qsInputs = this.getCurRow();
		if(qsInputs != null && !qsInputs.isEmpty()) {
			for(int selectIndex = 0;selectIndex < qsInputs.size();selectIndex++) {
				NounMetadata input = qsInputs.getNoun(selectIndex);
				IQuerySelector innerSelector = getSelector(input);
				qs.addSelector(genFunctionSelector(aggregationFunction, innerSelector));
			}
		}
		return qs;
	}
}