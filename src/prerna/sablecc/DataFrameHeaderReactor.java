package prerna.sablecc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import prerna.algorithm.api.ITableDataFrame;
import prerna.sablecc.PKQLRunner.STATUS;
import prerna.sablecc.meta.DataframeHeaderMetadata;
import prerna.sablecc.meta.IPkqlMetadata;

public class DataFrameHeaderReactor extends AbstractReactor {

	public static final String ADDITIONAL_INFO_BOOL = "additionalInfoBool";
	
	public DataFrameHeaderReactor() {
		String[] thisReacts = {};
		super.whatIReactTo = thisReacts;
		super.whoAmI = PKQLEnum.DATA_FRAME_HEADER;
	}

	@Override
	public Iterator process() {
		ITableDataFrame table = (ITableDataFrame) myStore.get("G");
		Boolean includeAdditionalInfo = (Boolean) myStore.get(ADDITIONAL_INFO_BOOL);
		if(includeAdditionalInfo == null || includeAdditionalInfo.equals(false)) {
			Set<String> orderHeaders = new TreeSet<String>();
			orderHeaders.addAll(Arrays.asList(table.getColumnHeaders()));
			myStore.put("tableHeaders", orderHeaders);
		} else {
			myStore.put("tableHeaders", table.getTableHeaderObjects());
		}
		myStore.put("RESPONSE", STATUS.SUCCESS.toString());
		myStore.put("STATUS", STATUS.SUCCESS);

		return null;
	}

	public IPkqlMetadata getPkqlMetadata() {
		DataframeHeaderMetadata metadata = new DataframeHeaderMetadata();
		metadata.setPkqlStr((String) myStore.get(PKQLEnum.DATA_FRAME_HEADER));
		return metadata;
	}

}
