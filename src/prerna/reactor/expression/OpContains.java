package prerna.reactor.expression;

import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;

public class OpContains extends OpBasic {

	public OpContains() {
		this.operation="contains";
		this.keysToGet = new String[]{ReactorKeysEnum.ARRAY.getKey(), ReactorKeysEnum.VALUE.getKey()};

	}

	@Override
	protected NounMetadata evaluate(Object[] values) {
		if(values.length < 2) {
			return new NounMetadata(false, PixelDataType.BOOLEAN);
		}
		Object[] list = null;
		if(values[0] instanceof Object[]) {
			list = (Object[]) values[0];
		} else {
			list = new Object[]{values[0]};
		}
		Object value = values[1];
		int length = list.length;
		boolean contains = false;
		for(int i = 0; i < length; i++) {
			if(list[i].equals(value)) {
				contains = true;
				break;
			}
		}

		NounMetadata noun = new NounMetadata(contains, PixelDataType.BOOLEAN);
		return noun;
	}

	@Override
	public String getReturnType() {
		// TODO Auto-generated method stub
		return "double";
	}
}
