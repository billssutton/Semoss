package prerna.sablecc2.om;

import java.util.Hashtable;

import prerna.sablecc2.reactor.IReactor;

public class CodeBlock {

	// primary building block for the code to be assimilated
	// couple of things here
	// imports what are the things we need to import
	// The Language of code - is this java / R / Python
	// What are the selectors it is using for operation
	// What type of code is it - Map / Reduce or something else
	// What are the options for the code
	
	
	public enum LANG{JAVA, R, PYTHON};
	
	Hashtable <String, Object> options = new Hashtable <String, Object>();
	String imports = null;
	String code = null;// this is the code that will go into the map block or reduce block or something else
	LANG language = LANG.JAVA; // default yeah baby
	IReactor.TYPE type = IReactor.TYPE.MAP;
	
	public void setLanguage(LANG language)
	{
		this.language = language;
	}
	
	public LANG getLanguage()
	{
		return this.language;
	}
	
	public String getCode()
	{
		return this.code;
	}
	
	public void setType(IReactor.TYPE type)
	{
		this.type = type;
	}
	
	public void addImport(String importPackage)
	{
		if(imports == null)
			imports = importPackage;
		else
			imports = imports + importPackage;
	}
	
	public void setImports(String imports)
	{
		this.imports = imports;
	}
	
	public void addCode(String lineOfCode)
	{
		if(code == null)
			code = lineOfCode;
		else
			code = code + lineOfCode;
	}
	
	public void addOption(String key, Object value)
	{
		options.put(key, value);
	}

	public void setOptions(Hashtable <String, Object> options)
	{
		this.options = options;
	}
	
	public Class makeCode()
	{
		// this is the final call that makes the code to be executed
		return null;
	}
}
