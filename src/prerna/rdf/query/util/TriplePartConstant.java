package prerna.rdf.query.util;

public class TriplePartConstant {

	String constant;
	
	//object here because if I put string, anyone method that requires TriplePart, coder can put in string without error
	public TriplePartConstant (Object constant)
	{
		this.constant = (String)constant;
	}
	
	public String getConstant()
	{
		return constant;
	}
	
	public void setConstant(String constant)
	{
		this.constant = constant;
	}
}
