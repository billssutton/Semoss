package prerna.query.querystruct;

import java.io.Serializable;
import java.util.List;

import prerna.query.querystruct.selectors.IQuerySelector;
import prerna.query.querystruct.selectors.QueryColumnSelector;

public class GenExpression extends SelectQueryStruct implements IQuerySelector, Serializable {
	
	// if composite is set then you basically have 2 sides
	// if not just one side
	boolean composite = false;
	public GenExpression parent = null;
	
	public boolean recursive = false; // ((a+b)+c)*d) - 2 values - tree out of the expression
	public boolean telescope = false; // join - <- within it join join (select a, b,c from d) a - GenExpression1(telescope = true, alias=a).body(GenExpression2(select a, b,c from d))
	
	public Object rightItem = null; // rightitem - GenExpression / Value / Scalar etc. 
	public Object leftItem = null; // left item blah blah
	
	// put the table name as well
	public String tableName = null;
	
	String rightAlias = null;
	String rightExpr = null;
	
	String leftAlias = null; // used all the time
	String leftExpr = null; // left expression - string of the sql being used
	String on = null;
	
	String expression = null; // expression
	SelectQueryStruct item = null;
	
	String alias = null;
	public String aQuery = null;
	
	public boolean paranthesis = false;
	
	public void setRightExpresion(Object rightItem)
	{
		this.rightItem = rightItem;
		if(rightItem instanceof GenExpression)
			((GenExpression)rightItem).parent = this;
	}
	
	public void setLeftExpresion(Object leftItem)
	{
		this.leftItem = leftItem;
		if(leftItem instanceof GenExpression)
			((GenExpression)leftItem).parent = this;
	}

	public void setExpression(String expression)
	{
		this.expression = expression;
	}
	
	public void setFromItem(SelectQueryStruct item)
	{
		this.item = item;
	}
	
	public void setComposite(boolean composite)
	{
		this.composite = composite;
	}
	
	public void setLeftExpr(String expr)
	{
		this.leftExpr = expr;
	}

	public void setRightExpr(String expr)
	{
		this.rightExpr = expr;
	}
	
	public void setLeftAlias(String leftAlias)
	{
		this.leftAlias = leftAlias;
	}

	public String getLeftAlias()
	{
		return leftAlias;
	}
	
	public void setOn(String on)
	{
		this.expression = on;
		this.on = on;
	}
	
	public void setOperation(String operation)
	{
		this.operation = operation;
	}
	
	public String getLeftExpr()
	{
		return this.leftExpr;
	}

	public String getOperation()
	{
		return this.operation;
	}

	@Override
	public SELECTOR_TYPE getSelectorType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAlias() {
		// TODO Auto-generated method stub
		return alias;
	}

	@Override
	public void setAlias(String alias) {
		// TODO Auto-generated method stub
		this.alias = alias;
	}

	@Override
	public boolean isDerived() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDataType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQueryStructName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<QueryColumnSelector> getAllQueryColumns() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String toString()
	{
		return aQuery;
	}

	
	public StringBuffer printQS(GenExpression qs, StringBuffer buf)
	{
		// if the type is join.. you need to do other things
		//System.err.println("Processing  " + qs.aQuery + " <>" + qs.expression + "<>" + qs.operation);
		String newLine = "";
		if(buf == null)
			buf = new StringBuffer();
		boolean processed = false;
		if(qs != null && qs.operation != null)
		{
			if(qs.operation.equalsIgnoreCase("select") || qs.operation.equalsIgnoreCase("querystruct"))
			{
				buf.append(newLine);
				buf.append("SELECT  ");
				for(int selIndex = 0;selIndex < qs.nselectors.size();selIndex++)
				{
					GenExpression sqs = qs.nselectors.get(selIndex);
					
					// need to handle telescope
					
					if(selIndex > 0)
						buf.append(", ");
					
					if(sqs.operation != null && sqs.operation.equalsIgnoreCase("querystruct"))		
					{
						buf.append("(");
					}
					printQS(sqs, buf);
					if(sqs.operation != null && sqs.operation.equalsIgnoreCase("querystruct"))		
					{
						buf.append(")");
					}
					/*// if it is a column I need to put alias too
					else if(sqs.operation.equalsIgnoreCase("column") || sqs.operation.equalsIgnoreCase("double") || sqs.operation.equalsIgnoreCase("date") || sqs.operation.equalsIgnoreCase("time") || sqs.operation.equalsIgnoreCase("string") )
					{
						buf.append(sqs.leftExpr);
						if(sqs.leftAlias != null)
							buf.append(" as ").append(sqs.leftAlias);
					}
					// need to handle the union, case, between
					
					// add the alias
					if(sqs.alias != null)
						buf.append("as " + sqs.alias);
					*/
				}
				processed = true;
			}
			else if(qs.operation.equalsIgnoreCase("column"))
			{
				String columnName = qs.leftExpr;
				//if(!columnName.startsWith("\""))
				//	columnName = "\"" + columnName + "\"";
				buf.append(qs.tableName).append(".").append(columnName);
				if(qs.leftAlias != null && qs.leftAlias.length() > 0)
					buf.append(" as ").append(qs.leftAlias);
				processed = true;
			}
			else if(qs.operation.equalsIgnoreCase("double") || qs.operation.equalsIgnoreCase("date") || qs.operation.equalsIgnoreCase("time") || qs.operation.equalsIgnoreCase("string") || qs.operation.equalsIgnoreCase("long"))
			{
				buf.append(qs.leftItem);
				if(qs.leftAlias != null && qs.leftAlias.length() > 0)
					buf.append(" as ").append(qs.leftAlias);
				processed = true;
			}
			else if(qs.operation.equalsIgnoreCase("opaque"))
			{
				buf.append(qs.getLeftExpr());
				if(qs.leftAlias != null && qs.leftAlias.length() > 0)
					buf.append(" as ").append(qs.leftAlias);
				processed = true;
			}
			else if(qs.operation.equalsIgnoreCase("from") && !qs.composite)
			{
				buf.append(qs.getLeftExpr());
				if(qs.leftAlias != null && qs.leftAlias.length() > 0)
					buf.append(" as ").append(qs.leftAlias);
				processed = true;
			}
			else if(qs.composite)
			{
				//System.err.println(" hmm.. from is composite.. but simple ?  " + qs.aQuery);
			}
		}
		if(qs.operation != null && qs.operation.contains("union"))
		{
			//System.err.println("And now we are getting the union " + qs);
			// process the left and right
			// 
			OperationExpression opex = (OperationExpression)qs;
			List <GenExpression> operands = opex.operands;
			List <String> opNames = opex.opNames;
			
			for(int opIndex = 0;opIndex < opNames.size();opIndex++)
			{
				if(opIndex == 0)
					printQS((GenExpression)operands.get(opIndex), buf);
				buf.append("  ").append(opNames.get(opIndex)).append("  ");
				printQS((GenExpression)operands.get(opIndex+1), buf);
			}
			
			processed = true;
		}
		
		if(qs.operation != null && qs.operation.equalsIgnoreCase("between"))
		{
			processed = true;
			buf.append("  "); 
			printQS((GenExpression)qs.body, buf);
			buf.append("  BETWEEN ");
			printQS((GenExpression)qs.leftItem, buf);
			buf.append("  AND  ");
			printQS((GenExpression)qs.rightItem, buf);
			processed = true;
			
		}
		if(qs.operation != null && qs.operation.equalsIgnoreCase("cast"))
		{
			// name of the function is in the left alias
			buf.append("CAST ").append("(");
			if(qs.leftItem != null && qs.leftItem instanceof GenExpression)
				printQS((GenExpression)qs.leftItem, buf);
			else
				buf.append(qs.leftItem);
			buf.append(" AS ").append(qs.leftAlias);
			buf.append(")");
			processed = true;
		}
		
		if(qs.operation != null && qs.operation.equalsIgnoreCase("function"))
		{
			// name of the function is in the left alias
			buf.append(qs.expression).append("(");
			FunctionExpression thisExpr = (FunctionExpression)qs;
			List <GenExpression> parameters = thisExpr.expressions;
			for(int paramIndex = 0;paramIndex < parameters.size();paramIndex++)
				printQS(parameters.get(paramIndex), buf);
			buf.append(")");
			if(qs.leftAlias != null)
				buf.append(" AS ").append(qs.leftAlias);
			processed = true;
		}
		if(qs.operation != null && qs.operation.equalsIgnoreCase("isnull"))
		{
			// name of the function is in the left alias
			printQS((GenExpression)qs.leftItem, buf);
			buf.append(" IS NULL ");
			if(qs.leftAlias != null)
				buf.append(" AS ").append(qs.leftAlias);
			processed = true;
		}
		// need to handle telescope

		if(!processed)
		{
			if(qs.recursive)
			{
				printQSRecursive(qs, buf);
			}
			else if(qs instanceof WhenExpression)
			{
				buf.append(((WhenExpression)qs).printOutput());
			}
			else if(qs instanceof InGenExpression)
			{
				InGenExpression ig = (InGenExpression)qs;
				if(qs.leftItem != null)
					printQS((GenExpression)qs.leftItem, buf);
				
				buf.append("  IN  ");
				if(ig.inList.size() > 0 && ig.rightItem == null)
				{
					// process the list
					// I have made this opaque
					for(int itemIndex = 0;itemIndex < ig.inList.size();itemIndex++)
					{
						if(itemIndex != 0)
							buf.append(", ");
						StringBuffer newBuf = printQS(ig.inList.get(itemIndex), null);
						if(!newBuf.toString().startsWith("("))
							buf.append("(");
						buf.append(newBuf);
						if(!newBuf.toString().endsWith(")"))
							buf.append(")");
							
					}
					//buf.append(")");
				}
				else if(qs.rightItem != null && qs.rightItem instanceof GenExpression)
				{
					buf.append("(");
					printQS((GenExpression)qs.rightItem, buf);
					buf.append(")");
				}
				processed = true;
			}
			else if(qs.leftItem != null && qs.rightItem != null) // this is expression
			{
				// dont know how to handle this yet
				// fun stuff we are going to go into a recursion again
				Object leftItem = (GenExpression)qs.leftItem;
				Object rightItem = (GenExpression)qs.rightItem;
				
				if(leftItem instanceof GenExpression && ((GenExpression)leftItem).paranthesis)
					buf.append("(");
				// this is where we need to do the paranthesis again I think
				printQS((GenExpression)qs.leftItem, buf);
				if(leftItem instanceof GenExpression && ((GenExpression)leftItem).paranthesis)
					buf.append(")");
				buf.append(qs.operation);

				if(rightItem instanceof GenExpression && ((GenExpression)rightItem).paranthesis)
					buf.append("(");
				printQS((GenExpression)qs.rightItem, buf);
				if(rightItem instanceof GenExpression && ((GenExpression)rightItem).paranthesis)
					buf.append(")");

			}
			else if(qs.telescope )
			{
				buf.append("(");
				printQS((GenExpression)qs.body, buf);
				buf.append(")");
				
				if(qs.leftAlias != null && qs.leftAlias.length() > 0)
					buf.append(" ").append(qs.leftAlias);				
			}
			else if(qs.leftExpr != null) // accomodating for the paranthesis from
			{
				buf.append(qs.leftExpr);
			}
			else if(qs.operation == null)
			{			
				//System.err.println("Dont know to handle " + qs.aQuery + "<> " + qs.operation);
			}
		}
		// like the filter

		// add the from
		if(qs.from != null)
		{
			// put the paranthesis if it is another select 
			//if(qs.from.operation != null && qs.from.operation.equalsIgnoreCase("querystruct"))
			{
				//System.err.println("From operation is set to " + qs.from.operation + " composite ?" + qs.from.composite );
				buf.append(newLine);
				buf.append("  FROM " );
				if(qs.from.composite)
					buf.append("( ");
				
				printQS(qs.from, buf);
				
				if(qs.from.composite)
				{
					buf.append(") ");
				
					if(qs.from.getLeftAlias() != null && qs.from.getLeftAlias().length() > 0)
						buf.append("AS " + qs.from.getLeftAlias());
				}
			}
			/*
			else
			{
				buf.append("  FROM " );
				if(qs.from.leftExpr != null)
					buf.append(qs.from.leftExpr).append("  ");
				if(qs.from.leftAlias != null)
					buf.append(qs.from.leftAlias).append(" ");
			}*/
		}
		

		// add the joins finally			
		for(int joinIndex = 0;joinIndex < qs.joins.size();joinIndex++)
		{
			//System.err.println("Selector Buf so far " + buf);
			GenExpression sqs = qs.joins.get(joinIndex);	
			buf.append("  ");
			String open = "";
			String close = "";
			// I also need to pick the from here
			// this is is the inner join on <from>
			// how to tell if a join is a subjoin ?
			if(sqs.from != null)
			{
				buf.append(newLine);
				buf.append(sqs.on);
				buf.append("  ");
				if(sqs.from != null && sqs.from.composite)
					buf.append("(");
				printQS(sqs.from, buf);
				if(sqs.from != null && sqs.from.composite)
					buf.append(")");
				if(sqs.from.leftAlias != null)
					buf.append("  AS ").append(sqs.from.leftAlias);
			}
			if(sqs.body != null && sqs.body.operation.equalsIgnoreCase("querystruct"))
			{
				open = "(";
				close = ")  ";
			}
			// process this as a query struct
			if(sqs.body != null)
			{
				buf.append("  on ");
				buf.append(open);
				printQS(sqs.body, buf);
				buf.append(close);
			}
		}

		// add the where
		if(qs.filter != null)
		{
			buf.append(newLine);
			buf.append("  WHERE " );
			printQS(qs.filter, buf);
		}
		
		
		// add the groupby
		if(qs.ngroupBy.size() > 0)
		{
			buf.append("  GROUP BY " );
			for(int groupIndex = 0;groupIndex < qs.ngroupBy.size();groupIndex++)
			{
				if(groupIndex != 0)
					buf = buf.append(" , ");
				GenExpression gep = qs.ngroupBy.get(groupIndex);

				if(gep.composite)
					buf.append("(");
				printQS(gep, buf);
				if(gep.composite)
					buf.append(")");
				
			}
		}

		// add the order
		if(qs.norderBy.size() > 0)
		{
			buf.append("  ORDER BY " );
			for(int orderIndex = 0;orderIndex < qs.norderBy.size();orderIndex++)
			{
				if(orderIndex != 0)
					buf = buf.append(" , ");
				GenExpression gep = qs.norderBy.get(orderIndex).body;

				if(gep.composite)
					buf.append("(");
				printQS(gep, buf);
				if(gep.composite)
					buf.append(")");
				buf.append("  ");
				String direction = ((OrderByExpression)qs.norderBy.get(orderIndex)).direction;
				if(direction.length() > 0)
					buf.append(direction);
			}
		}
		
		// limit and offset
		if(qs.limit != -1)
			buf.append(" LIMIT ").append(qs.limit);

		if(qs.offset != -1)
			buf.append(" OFFSET ").append(qs.offset);

		
		return buf;
	}
		
	public StringBuffer printQSRecursive(GenExpression qs, StringBuffer buf)
	{
		Object leftItem = qs.leftItem;
		Object rightItem = qs.rightItem;
		
		if(leftItem instanceof GenExpression)
		{
			// need to know if I need to put a paranthesis around this or not
			Object childLeftItem = ((GenExpression)leftItem).leftItem;
			Object childRightItem = ((GenExpression)leftItem).leftItem;
			
			StringBuffer leftBuf = printQS((GenExpression)leftItem, new StringBuffer());
			
			if((childLeftItem != null && childLeftItem instanceof GenExpression && ((GenExpression)childLeftItem).composite) 
					&& (childRightItem != null && childRightItem instanceof GenExpression && ((GenExpression)childRightItem).composite))
			{
				buf.append("(");
				buf.append(leftBuf);
				buf.append(")");
			}
			else
				buf.append(leftBuf).append(" ");
		}
		else 
			buf.append(qs.leftItem).append(" ");
		
		if(qs.operation != null)
			buf.append(" ").append(qs.operation).append(" ");
		
		if(rightItem instanceof GenExpression)
		{
			// need to know if I need to put a paranthesis around this or not
			Object childLeftItem = ((GenExpression)rightItem).leftItem;
			Object childRightItem = ((GenExpression)rightItem).rightItem;
			
			StringBuffer rightBuf = printQS((GenExpression)rightItem, new StringBuffer());
			
			if((childLeftItem != null && childLeftItem instanceof GenExpression && ((GenExpression)childLeftItem).composite) && (childRightItem != null && childLeftItem instanceof GenExpression && ((GenExpression)childRightItem).composite))
			{
				buf.append("(");
				buf.append(rightBuf);
				buf.append(")");
			}
			else
				buf.append(rightBuf).append(" ");
		}
		else 
			buf.append(qs.rightItem).append(" ");
		return buf;
	}
	
	public void replaceTableAlias(GenExpression gep, String oldName, String newName)
	{
		// if this is an operation
		if(gep.operation.equalsIgnoreCase("column"))
			gep.tableName = newName;
		
		// operation
		else if(gep.leftItem != null && gep.rightItem != null)
		{
			if(gep.leftItem instanceof GenExpression)
				replaceTableAlias((GenExpression)gep.leftItem, oldName, newName);
			if(gep.rightItem instanceof GenExpression)
				replaceTableAlias((GenExpression)gep.rightItem, oldName, newName);
		}
	}
	
	
	// recursively goes through and replaces column table name
	
	public void replaceTableAlias2(GenExpression qs, String oldName, String newName)
	{
		// if the type is join.. you need to do other things
		//System.err.println("Processing  " + qs.aQuery + " <>" + qs.expression + "<>" + qs.operation);
		boolean processed = false;
		if(qs != null && qs.operation != null)
		{
			if(qs.operation.equalsIgnoreCase("select") || qs.operation.equalsIgnoreCase("querystruct"))
			{
				for(int selIndex = 0;selIndex < qs.nselectors.size();selIndex++)
				{
					GenExpression sqs = qs.nselectors.get(selIndex);					
					replaceTableAlias2(sqs, oldName, newName);
				}
				processed = true;
			}
			if(qs.operation.equalsIgnoreCase("column"))
			{
				if(qs.tableName != null && oldName!= null &&qs.tableName.equalsIgnoreCase(oldName))
					qs.tableName = newName;
				else if(oldName == null)
					qs.tableName = newName;
			}
		}
		if(qs.operation != null && qs.operation.contains("union"))
		{
			//System.err.println("And now we are getting the union " + qs);
			// process the left and right
			// 
			OperationExpression opex = (OperationExpression)qs;
			List <GenExpression> operands = opex.operands;
			List <String> opNames = opex.opNames;
			
			for(int opIndex = 0;opIndex < opNames.size();opIndex++)
			{
				if(opIndex == 0)
					replaceTableAlias2((GenExpression)operands.get(opIndex), oldName, newName);
				replaceTableAlias2((GenExpression)operands.get(opIndex+1), oldName, newName);
			}
			processed = true;
		}
		
		if(qs.operation != null && qs.operation.equalsIgnoreCase("between"))
		{
			processed = true;
			replaceTableAlias2((GenExpression)qs.body, oldName, newName);
			replaceTableAlias2((GenExpression)qs.leftItem, oldName, newName);
			replaceTableAlias2((GenExpression)qs.rightItem, oldName, newName);
			processed = true;
			
		}
		if(qs.operation != null && qs.operation.equalsIgnoreCase("cast"))
		{
			// name of the function is in the left alias
			if(qs.leftItem != null && qs.leftItem instanceof GenExpression)
				replaceTableAlias2((GenExpression)qs.leftItem, oldName, newName);
			processed = true;
		}
		
		if(qs.operation != null && qs.operation.equalsIgnoreCase("function"))
		{
			// name of the function is in the left alias
			FunctionExpression thisExpr = (FunctionExpression)qs;
			List <GenExpression> parameters = thisExpr.expressions;
			for(int paramIndex = 0;paramIndex < parameters.size();paramIndex++)
			{
				replaceTableAlias2(parameters.get(paramIndex), oldName, newName);
			}
			processed = true;
		}
		if(qs.operation != null && qs.operation.equalsIgnoreCase("isnull"))
		{
			// name of the function is in the left alias
			replaceTableAlias2((GenExpression)qs.leftItem, oldName, newName);
			processed = true;
		}

		// need to handle telescope

		if(!processed)
		{
			if(qs.recursive)
			{
				Object leftItem = qs.leftItem;
				Object rightItem = qs.rightItem;
				
				if(leftItem instanceof GenExpression)
				{
					replaceTableAlias2((GenExpression)leftItem, oldName, newName);
					
				}
				if(rightItem instanceof GenExpression)
				{
					replaceTableAlias2((GenExpression)rightItem, oldName, newName);
				}
			}
			else if(qs instanceof WhenExpression)
			{
				// this shoudl already do liskov substitution
				replaceTableAlias2(qs, oldName, newName);;
			}
			else if(qs instanceof InGenExpression)
			{
				InGenExpression ig = (InGenExpression)qs;
				if(qs.leftItem != null)
					replaceTableAlias2((GenExpression)qs.leftItem, oldName, newName);

				if(qs.rightItem != null && qs.rightItem instanceof GenExpression)
				{
					replaceTableAlias2((GenExpression)qs.rightItem, oldName, newName);
				}
			}
			else if(qs.leftItem != null && qs.rightItem != null) // this is expression
			{
				// dont know how to handle this yet
				// fun stuff we are going to go into a recursion again
				Object leftItem = (GenExpression)qs.leftItem;
				Object rightItem = (GenExpression)qs.rightItem;
				// this is where we need to do the paranthesis again I think
				replaceTableAlias2((GenExpression)qs.leftItem, oldName, newName);
				replaceTableAlias2((GenExpression)qs.rightItem, oldName, newName);
			}
			else if(qs.telescope )
			{
				replaceTableAlias2((GenExpression)qs.body, oldName, newName);
			}
		}
		// like the filter

		// add the from
		if(qs.from != null)
			replaceTableAlias2((GenExpression)qs.from, oldName, newName);				
		

		// add the joins finally			
		for(int joinIndex = 0;joinIndex < qs.joins.size();joinIndex++)
		{
			//System.err.println("Selector Buf so far " + buf);
			GenExpression sqs = qs.joins.get(joinIndex);	
			// I also need to pick the from here
			// this is is the inner join on <from>
			// how to tell if a join is a subjoin ?
			if(sqs.from != null)
				replaceTableAlias2((GenExpression)sqs.from, oldName, newName);
			if(sqs.body != null)
				replaceTableAlias2((GenExpression)sqs.body, oldName, newName);
		}

		// add the where
		if(qs.filter != null)
			replaceTableAlias2((GenExpression)qs.filter, oldName, newName);
		
		
		// add the groupby
		if(qs.ngroupBy.size() > 0)
		{
			for(int groupIndex = 0;groupIndex < qs.ngroupBy.size();groupIndex++)
			{
				GenExpression gep = qs.ngroupBy.get(groupIndex);
				replaceTableAlias2(gep, oldName, newName);
			}
		}

		// add the order
		if(qs.norderBy.size() > 0)
		{
			for(int orderIndex = 0;orderIndex < qs.norderBy.size();orderIndex++)
			{
				GenExpression gep = qs.norderBy.get(orderIndex).body;
				replaceTableAlias2(gep, oldName, newName);
			}
		}
		
	}

	public void addQuoteToColumn(GenExpression qs, String quote)
	{
		// if the type is join.. you need to do other things
		//System.err.println("Processing  " + qs.aQuery + " <>" + qs.expression + "<>" + qs.operation);
		boolean processed = false;
		if(qs != null && qs.operation != null)
		{
			if(qs.operation.equalsIgnoreCase("select") || qs.operation.equalsIgnoreCase("querystruct"))
			{
				for(int selIndex = 0;selIndex < qs.nselectors.size();selIndex++)
				{
					GenExpression sqs = qs.nselectors.get(selIndex);					
					addQuoteToColumn(sqs, quote);
				}
				processed = true;
			}
			if(qs.operation.equalsIgnoreCase("column"))
			{
				qs.leftExpr = quote + qs.leftExpr + quote;
			}
		}
		if(qs.operation != null && qs.operation.contains("union"))
		{
			//System.err.println("And now we are getting the union " + qs);
			// process the left and right
			// 
			OperationExpression opex = (OperationExpression)qs;
			List <GenExpression> operands = opex.operands;
			List <String> opNames = opex.opNames;
			
			for(int opIndex = 0;opIndex < opNames.size();opIndex++)
			{
				if(opIndex == 0)
					addQuoteToColumn((GenExpression)operands.get(opIndex), quote);
				addQuoteToColumn((GenExpression)operands.get(opIndex+1), quote);
			}
			processed = true;
		}
		
		if(qs.operation != null && qs.operation.equalsIgnoreCase("between"))
		{
			processed = true;
			addQuoteToColumn((GenExpression)qs.body, quote);
			addQuoteToColumn((GenExpression)qs.leftItem, quote);
			addQuoteToColumn((GenExpression)qs.rightItem, quote);
			processed = true;
			
		}
		if(qs.operation != null && qs.operation.equalsIgnoreCase("cast"))
		{
			// name of the function is in the left alias
			if(qs.leftItem != null && qs.leftItem instanceof GenExpression)
				addQuoteToColumn((GenExpression)qs.leftItem, quote);
			processed = true;
		}
		
		if(qs.operation != null && qs.operation.equalsIgnoreCase("function"))
		{
			// name of the function is in the left alias
			FunctionExpression thisExpr = (FunctionExpression)qs;
			List <GenExpression> parameters = thisExpr.expressions;
			for(int paramIndex = 0;paramIndex < parameters.size();paramIndex++)
			{
				addQuoteToColumn(parameters.get(paramIndex),quote);
			}
			processed = true;
		}
		if(qs.operation != null && qs.operation.equalsIgnoreCase("isnull"))
		{
			// name of the function is in the left alias
			addQuoteToColumn((GenExpression)qs.leftItem,quote);
			processed = true;
		}

		// need to handle telescope

		if(!processed)
		{
			if(qs.recursive)
			{
				Object leftItem = qs.leftItem;
				Object rightItem = qs.rightItem;
				
				if(leftItem instanceof GenExpression)
				{
					addQuoteToColumn((GenExpression)leftItem, quote);
					
				}
				if(rightItem instanceof GenExpression)
				{
					addQuoteToColumn((GenExpression)rightItem,quote);
				}
			}
			else if(qs instanceof WhenExpression)
			{
				// this shoudl already do liskov substitution
				addQuoteToColumn(qs, quote);;
			}
			else if(qs.leftItem != null && qs.rightItem != null) // this is expression
			{
				// dont know how to handle this yet
				// fun stuff we are going to go into a recursion again
				Object leftItem = (GenExpression)qs.leftItem;
				Object rightItem = (GenExpression)qs.rightItem;
				// this is where we need to do the paranthesis again I think
				addQuoteToColumn((GenExpression)qs.leftItem, quote);
				addQuoteToColumn((GenExpression)qs.rightItem, quote);
			}
			else if(qs.telescope )
			{
				addQuoteToColumn((GenExpression)qs.body, quote);
			}
		}
		// like the filter

		// add the from
		if(qs.from != null)
			addQuoteToColumn((GenExpression)qs.from, quote);				
		

		// add the joins finally			
		for(int joinIndex = 0;joinIndex < qs.joins.size();joinIndex++)
		{
			//System.err.println("Selector Buf so far " + buf);
			GenExpression sqs = qs.joins.get(joinIndex);	
			// I also need to pick the from here
			// this is is the inner join on <from>
			// how to tell if a join is a subjoin ?
			if(sqs.from != null)
				addQuoteToColumn((GenExpression)sqs.from, quote);
			if(sqs.body != null)
				addQuoteToColumn((GenExpression)sqs.body, quote);
		}

		// add the where
		if(qs.filter != null)
			addQuoteToColumn((GenExpression)qs.filter, quote);
		
		
		// add the groupby
		if(qs.ngroupBy.size() > 0)
		{
			for(int groupIndex = 0;groupIndex < qs.ngroupBy.size();groupIndex++)
			{
				GenExpression gep = qs.ngroupBy.get(groupIndex);
				addQuoteToColumn(gep, quote);
			}
		}

		// add the order
		if(qs.norderBy.size() > 0)
		{
			for(int orderIndex = 0;orderIndex < qs.norderBy.size();orderIndex++)
			{
				GenExpression gep = qs.norderBy.get(orderIndex).body;
				addQuoteToColumn(gep, quote);
			}
		}
		
	}

	
}
