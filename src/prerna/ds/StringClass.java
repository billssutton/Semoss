package prerna.ds;

import java.lang.reflect.Field;
import java.util.StringTokenizer;

public class StringClass implements ISEMOSSNode {
	
	String innerString = null;
	String type = null;
	String rawValue = null;
	
	public StringClass(String string, boolean serialized){
		if(!serialized){
			this.innerString = innerString;
		}
		else{
			fromString(string);
		}
	}
	
	public StringClass(String innerString)
	{
		this.innerString = innerString;
	}
	
	public StringClass(String innerString, String type)
	{
		this.innerString = innerString;
		this.type = type;
	}
	
	public StringClass(String innerString, String rawValue, String type) {
		this.innerString = innerString;
		this.type = type;
		this.rawValue = rawValue;
	}
	
	@Override
	public String toString(){
		String ret = "innerString==="+this.innerString +
				"&&&type==="+this.type +
				"&&&rawValue==="+this.rawValue;
		return ret;
	}
	
	private void fromString(String serializedString){
//		System.err.println(serializedString);
		String[] mainParts = serializedString.split("&{3}");
		for(int idx = 0; idx < mainParts.length; idx++){
			String element = mainParts[idx];
			String[] parts = element.split("={3}");
			String name = parts[0];
			String value = parts[1];
			if(!value.equals("null")){
				if(name.equals("innerString")){
					this.innerString = value;
				}
				else if(name.equals("type")){
					this.type = value;
				}
				else if(name.equals("rawValue")){
					this.rawValue = value;
				}
			}
		}
	}
	
	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return innerString;
	}

	@Override
	public boolean isLeft(ITreeKeyEvaluatable object) {
		// TODO Auto-generated method stub
		return ((((StringClass)object).innerString.compareTo(innerString)) > 0);
	}

	@Override
	public boolean isEqual(ITreeKeyEvaluatable object) {
		// TODO Auto-generated method stub
		return ((StringClass)object).innerString.compareTo(innerString) == 0;
	}


	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return this.innerString;
	}


	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return this.type;
	}


	@Override
	public VALUE_TYPE getValueType() {
		// TODO Auto-generated method stub
		return ISEMOSSNode.VALUE_TYPE.NOMINAL;
	}

	@Override
	public String getRawValue() {
		// TODO Auto-generated method stub
		return this.rawValue;
	}
}
