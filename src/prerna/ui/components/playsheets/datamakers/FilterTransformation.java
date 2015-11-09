package prerna.ui.components.playsheets.datamakers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.internal.StringMap;

import prerna.rdf.query.builder.AbstractQueryBuilder;
import prerna.util.Utility;

public class FilterTransformation extends AbstractTransformation {

	private static final Logger LOGGER = LogManager.getLogger(FilterTransformation.class.getName());
	public static final String METHOD_NAME = "filter";						// name of the method in all data makers to perform a filter
	public static final String UNDO_METHOD_NAME = "unfilter";				// name of the method in all data makers to perform undo filtering
	public static final String COLUMN_HEADER_KEY = "colHeader";				// key in properties map for the type to apply the filter for
	public static final String VALUES_KEY = "values";						// key in properties map for the list of values to filter

	private DataMakerComponent dmc;
	private Boolean preTrans;
	private IDataMaker dm;

	@Override
	public void setDataMakers(IDataMaker... dm){
		this.dm = dm[0];
	}
	
	@Override
	public void setDataMakerComponent(DataMakerComponent dmc){
		this.dmc = dmc;
	}
	
	@Override
	public void setTransformationType(Boolean preTransformation){
		this.preTrans = preTransformation;
	}

	@Override
	public void runMethod() {
		String colHeader = this.props.get(COLUMN_HEADER_KEY) +"";
		List<Object> values = (List<Object>) this.props.get(VALUES_KEY);
		
		// if this is a pre-transformation
		if(preTrans){
			// if there is metamodel data, add this as a filter and let query builder do its thing
			Map<String, Object> metamodelData = this.dmc.getMetamodelData();
			if(metamodelData != null) {
				addFilterToComponentData(colHeader, values, metamodelData);
			} else {
				// there is no metamodel data
				// need to fill the query with the selected value
				// this doesn't allow for multiselect from the UI 
				String query = this.dmc.getQuery();
				query = Utility.normalizeParam(query);
				Map<String, List<Object>> paramHash = new Hashtable<String, List<Object>>();
				paramHash.put(colHeader, values);
				query = Utility.fillParam(query, paramHash);
				this.dmc.setQuery(query);
			}
		}
		// if it is post trans
		// we need to call filter by reflection on the data maker
		else{
			runFilterMethod(colHeader, values);
		}
	}
	
	/**
	 * Appends the filtering into the metamodel data within the component
	 * Used when the filtering transformation is a preTransformation
	 * @param colHeader						The name of type to filter
	 * @param values						The list of values for the filter
	 * @param metamodelData					The metamodel data
	 */
	private void addFilterToComponentData(String colHeader, List<Object> values, Map<String, Object> metamodelData){
        StringMap<List<Object>> stringMap;
        if(((StringMap) metamodelData.get("QueryData")).containsKey(AbstractQueryBuilder.filterKey)) {
               stringMap = (StringMap<List<Object>>) ((StringMap) metamodelData.get("QueryData")).get(AbstractQueryBuilder.filterKey);
        } else {
               stringMap = new StringMap<List<Object>>();
        }
        stringMap.put(colHeader, values);
        ((StringMap) metamodelData.get("QueryData")).put(AbstractQueryBuilder.filterKey, stringMap);
	}
		
	/**
	 * Runs the postTransformation filtering routine
	 * @param colHeader					The name of the type to filter
	 * @param values					The list of values to filter
	 */
	private void runFilterMethod(String colHeader, List<Object> values){
		Method method = null;
		try {
			method = dm.getClass().getMethod(METHOD_NAME, String.class, List.class);
			LOGGER.info("Successfully got method : " + METHOD_NAME);
			
			method.invoke(dm, colHeader, values);
			LOGGER.info("Successfully invoked method : " + METHOD_NAME);

		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return;
	}

	@Override
	public Map<String, Object> getProperties() {
		props.put(TYPE, METHOD_NAME);
		return this.props;
	}

	@Override
	public void undoTransformation() {
		String colHeader = this.props.get(COLUMN_HEADER_KEY) +"";
		List<Object> values = (List<Object>) this.props.get(VALUES_KEY);
		
		Method method = null;
		try {
			method = dm.getClass().getMethod(UNDO_METHOD_NAME, String.class, List.class);
			LOGGER.info("Successfully got method : " + UNDO_METHOD_NAME);
			
			method.invoke(dm, colHeader, values);
			LOGGER.info("Successfully invoked method : " + UNDO_METHOD_NAME);

		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return;
	}
	
}
