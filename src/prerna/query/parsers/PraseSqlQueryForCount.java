package prerna.query.parsers;

import java.util.List;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Fetch;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.Top;

public class PraseSqlQueryForCount {
	private static final String ORDER_BY_REGEX = "(?i)order\\s*by";
	private static final String REPLACE_STRING = "";
	private static final String CASE_INSENSITIVE_REGEX = "(?i)";

	public String processQuery(String query) throws Exception {
		String newQuery = query;
		Pattern orderByPattern = null;
		Matcher orderByMatcher = null;

		// parse the sql
		Statement stmt = CCJSqlParserUtil.parse(query);
		Select select = ((Select)stmt);
		PlainSelect sb = (PlainSelect)select.getSelectBody();
		
		// get rid of order by
		{
			List<OrderByElement> orders = sb.getOrderByElements();
			if(orders != null && !orders.isEmpty()) {
				orderByPattern = Pattern.compile(ORDER_BY_REGEX);

				// get a matcher object
				orderByMatcher = orderByPattern.matcher(newQuery);
				newQuery = orderByMatcher.replaceAll(REPLACE_STRING);

				int counter = 0;
				int size = orders.size();
				for(OrderByElement order : orders) {
					String orderExpression = toRegex(order.toString());					

					if(size > 1 && (counter + 1) < size) {
						orderExpression += "\\s*,*";
					}

					orderByPattern = Pattern.compile(CASE_INSENSITIVE_REGEX + orderExpression);
					orderByMatcher = orderByPattern.matcher(newQuery);
					newQuery = orderByMatcher.replaceAll(REPLACE_STRING);

					counter++;
				}
			}
		}
		
		// get rid of top
		{
			Top top = sb.getTop();
			if(top != null) {
				String topExpression = toRegex(top.toString());
				newQuery = newQuery.replaceAll(CASE_INSENSITIVE_REGEX + topExpression, "");
			}
		}
		
		// get rid of limit
		{
			Limit limit = sb.getLimit();
			if(limit != null) {
				String limitExpression = toRegex(limit.toString());
				newQuery = newQuery.replaceAll(CASE_INSENSITIVE_REGEX + limitExpression, "");
			}
		}
		
		// get rid of offset
		{
			Offset offset = sb.getOffset();
			if(offset != null) {
				String offsetExpression = toRegex(offset.toString());
				newQuery = newQuery.replaceAll(CASE_INSENSITIVE_REGEX + offsetExpression, "");
			}
		}
		
		// get rid of fetch
		{
			Fetch fetch = sb.getFetch();
			if(fetch != null) {
				String fetchExpression = toRegex(fetch.toString());
				newQuery = newQuery.replaceAll(CASE_INSENSITIVE_REGEX + fetchExpression, "");
			}
		}
		
		return newQuery;
	}
	
	private static String toRegex(String s) {
		return s.replace(" ", "\\s*");
	}
	
	public static void main(String[] args) throws Exception {
		String query = "SELECT DISTINCT scored.County_Name_Proper AS \"County_Name_Proper\" , "
				+ "COUNT(DISTINCT scored.KLNK) AS \"Number_of_Individuals\" , "
				+ "scored.Treatment_Facilities_Size AS \"Treatment_Facilities_Size\" , "
				+ "scored.Drug_Death_Rate_Range AS \"Drug_Death_Rate_Range\" , "
				+ "scored.Population AS \"Population\" FROM scored scored  "
				+ "GROUP BY scored.County_Name_Proper, scored.Treatment_Facilities_Size, scored.Drug_Death_Rate_Range, scored.Population "
				+ "ORDER BY scored.County_Name_Proper desc "
				+ "LIMIT 10 OFFSET 50";
		PraseSqlQueryForCount praser = new PraseSqlQueryForCount();
		System.out.println(praser.processQuery(query));
	}
}
