package prerna.sablecc2.reactor.qs.selectors;

import java.util.List;

import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.selectors.IQuerySelector;
import prerna.query.querystruct.selectors.QueryArithmeticSelector;
import prerna.query.querystruct.selectors.QueryConstantSelector;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.reactor.AbstractReactor;

public class QuerySelectorExpressionAssimilator extends AbstractReactor {

	private String mathExpr;
	
	@Override
	public NounMetadata execute() {
		GenRowStruct qsInputs = this.getCurRow();
		IQuerySelector leftSelector = getSelector(qsInputs.getNoun(0));
		IQuerySelector rightSelector = getSelector(qsInputs.getNoun(1));
		return composeNewSelector(combineSelectorsModingForOOO(this.mathExpr, leftSelector, rightSelector));
	}
	
	private NounMetadata composeNewSelector(IQuerySelector selector) {
		return new NounMetadata(selector, PixelDataType.COLUMN);
	}
	
	public void setMathExpr(String mathExpr) {
		this.mathExpr = mathExpr;
	}
	
	private IQuerySelector getSelector(NounMetadata input) {
		PixelDataType nounType = input.getNounType();
		if(nounType == PixelDataType.QUERY_STRUCT) {
			// remember, if it is an embedded selector
			// we return a full QueryStruct even if it has just one selector
			// inside of it
			QueryStruct2 qs = (QueryStruct2) input.getValue();
			List<IQuerySelector> selectors = qs.getSelectors();
			if(selectors.isEmpty()) {
				// umm... merge the other QS stuff
				qs.merge(qs);
				return null;
			}
			return selectors.get(0);
		} else if(nounType == PixelDataType.COLUMN) {
			return (IQuerySelector) input.getValue();
		} else {
			// we have a constant...
			QueryConstantSelector cSelect = new QueryConstantSelector();
			cSelect.setConstant(input.getValue());
			return cSelect;
		}
	}
	
	/**
	 * Method to correctly set up left and right selectors based on order of operations
	 * @param mathExpression
	 * @param origSelector
	 * @param leftSelector
	 * @param rightSelector
	 * @return
	 */
	public static QueryArithmeticSelector combineSelectorsModingForOOO(String mathExpression, IQuerySelector leftSelector, IQuerySelector rightSelector) {
		// so i gotta do some fun stuff here
		// lets say you do the following
		// 2 + 5 * 6 / 4
		// since the expression is parsed left to right
		// we will end up creating ( (2+5) * 6 ) / 4;
		// but we actually want 2 + ((5*6) / 4)
		// so i will do some processing based on the type of expression
		// to get the correct math order of operations
		if(mathExpression.equals("*") || mathExpression.equals("/")) {
			// this is only the case when we have a mult/div operation
			// and the expression must be a plus/minus
			if(leftSelector.getSelectorType() == IQuerySelector.SELECTOR_TYPE.ARITHMETIC) {
				QueryArithmeticSelector leftArithSelector = (QueryArithmeticSelector) leftSelector;
				String leftSideMathExpression = leftArithSelector.getMathExpr();
				if(leftSideMathExpression.equals("-") || leftSideMathExpression.equals("+")) {
					// we gotta break this apart to form correctly
					IQuerySelector leftOfLeftSelector = leftArithSelector.getLeftSelector();
					IQuerySelector rightOfLeftSelector = leftArithSelector.getRightSelector();
					
					// we will do a nice switch
					// (leftOfLeftSelector + rightOfLeftSelector) * rightSelector -> leftOfLeftSelector + (rightOfLeftSelector * rightSelector)
					QueryArithmeticSelector newRightSelector = new QueryArithmeticSelector();
					newRightSelector.setLeftSelector(rightOfLeftSelector);
					newRightSelector.setRightSelector(rightSelector);
					newRightSelector.setMathExpr(mathExpression);
					
					QueryArithmeticSelector newSelector = new QueryArithmeticSelector();
					newSelector.setLeftSelector(leftOfLeftSelector);
					newSelector.setRightSelector(newRightSelector);
					newSelector.setMathExpr(leftSideMathExpression);
					return newSelector;
				}
			} else if(rightSelector.getSelectorType() == IQuerySelector.SELECTOR_TYPE.ARITHMETIC) {
				QueryArithmeticSelector rightArithSelector = (QueryArithmeticSelector) rightSelector;
				String rightSideMathExpression = rightArithSelector.getMathExpr();
				if(rightSideMathExpression.equals("-") || rightSideMathExpression.equals("+")) {
					// we gotta break this apart to form correctly
					IQuerySelector leftOfRightSelector = rightArithSelector.getLeftSelector();
					IQuerySelector rightOfRightSelector = rightArithSelector.getRightSelector();
					
					// we will do a nice switch
					// leftSelector * (leftOfRightSelector + rightOfRightSelector) -> (leftSelector * leftOfRightSelector) + rightOfRightSelector
					QueryArithmeticSelector newLeftSelector = new QueryArithmeticSelector();
					newLeftSelector.setLeftSelector(leftSelector);
					newLeftSelector.setRightSelector(leftOfRightSelector);
					newLeftSelector.setMathExpr(mathExpression);
					
					QueryArithmeticSelector newSelector = new QueryArithmeticSelector();
					newSelector.setLeftSelector(newLeftSelector);
					newSelector.setRightSelector(rightOfRightSelector);
					newSelector.setMathExpr(rightSideMathExpression);
					return newSelector;
				}
			}
		}
		
		QueryArithmeticSelector newSelector = new QueryArithmeticSelector();
		newSelector.setLeftSelector(leftSelector);
		newSelector.setRightSelector(rightSelector);
		newSelector.setMathExpr(mathExpression);
		return newSelector;
	}
	
}
