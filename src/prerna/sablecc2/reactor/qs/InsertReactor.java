package prerna.sablecc2.reactor.qs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.algorithm.api.ITableDataFrame;
import prerna.auth.User;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityAppUtils;
import prerna.cluster.util.ClusterUtil;
import prerna.date.SemossDate;
import prerna.ds.rdbms.AbstractRdbmsFrame;
import prerna.ds.util.RdbmsQueryBuilder;
import prerna.engine.api.IEngine;
import prerna.engine.api.IRDBMSEngine;
import prerna.engine.impl.rdbms.AuditDatabase;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.query.querystruct.AbstractQueryStruct;
import prerna.query.querystruct.AbstractQueryStruct.QUERY_STRUCT_TYPE;
import prerna.query.querystruct.selectors.IQuerySelector;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.query.querystruct.transform.QSAliasToPhysicalConverter;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.execptions.SemossPixelException;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.Constants;
import prerna.util.sql.AbstractSqlQueryUtil;
import prerna.util.sql.RdbmsTypeEnum;

public class InsertReactor extends AbstractReactor {
	
	private static final Logger logger = LogManager.getLogger(InsertReactor.class);

	private NounMetadata qStruct = null;
	
	public InsertReactor() {
		this.keysToGet = new String[] {"into", "values", "commit"};
	}
	
	/*
	 * Pixel can contain a <UUID> value which 
	 * will be replaced with UUID.randomUUID() 
	 * Pixel can also contain <USER_ID> value which
	 * will be replaced with the main user id
	 * upon inserting into the database
	 */
	
	@Override
	public NounMetadata execute() {
		if(qStruct == null) {
			qStruct = getQueryStruct();
		}
		
		AbstractQueryStruct qs = (AbstractQueryStruct) qStruct.getValue();
		IEngine engine = null;
		ITableDataFrame frame = null;
		AbstractSqlQueryUtil queryUtil = null;
		String userId = "user not defined";

		if(qStruct.getValue() instanceof AbstractQueryStruct) {
			qs = ((AbstractQueryStruct) qStruct.getValue());
			if(qs.getQsType() == QUERY_STRUCT_TYPE.ENGINE) {
				engine = qs.retrieveQueryStructEngine();
				if(!(engine instanceof IRDBMSEngine)) {
					throw new IllegalArgumentException("Insert query only works for rdbms databases");
				}
				queryUtil = ((IRDBMSEngine) engine).getQueryUtil();
				// If an engine and the user is defined, then grab it for the audit log
				User user = this.insight.getUser();
				if (user != null) {
					userId = user.getAccessToken(user.getLogins().get(0)).getId();
				}
				
				// If security is enabled, then check that the user can edit the engine
				if (AbstractSecurityUtils.securityEnabled() && !SecurityAppUtils.userCanEditEngine(user, engine.getEngineId())) {
					throw new IllegalArgumentException("User does not have permission to insert query for this app");
				}
			} else if(qs.getQsType() == QUERY_STRUCT_TYPE.FRAME) {
				frame = qs.getFrame();
				if(!(frame instanceof AbstractRdbmsFrame)) {
					throw new IllegalArgumentException("Insert query only works for sql frames");
				}
				queryUtil = ((AbstractRdbmsFrame) frame).getQueryUtil();
			}
		} else {
			throw new IllegalArgumentException("Input to exec query requires a query struct");
		}

		StringBuilder prefixSb = new StringBuilder("INSERT INTO ");
		
		GenRowStruct colGrs = this.store.getNoun("into");
		GenRowStruct valGrs = this.store.getNoun("values");
		GenRowStruct commitGrs = this.store.getNoun("commit");
		Boolean commit = false;
		if(commitGrs != null && !commitGrs.isEmpty()) {
			commit = Boolean.parseBoolean(commitGrs.get(0) + "");
		}
		
		List<IQuerySelector> selectors = new Vector<>();
		for(int i = 0; i < colGrs.size(); i++) {
			String s = colGrs.get(i).toString();
			selectors.add(new QueryColumnSelector (s));
		}
		
		if(frame != null) {
			qs.setSelectors(selectors);
			qs = QSAliasToPhysicalConverter.getPhysicalQs(qs, frame.getMetaData());
			selectors = qs.getSelectors();
		}
		
		// Insert table name
		QueryColumnSelector t = (QueryColumnSelector) selectors.get(0);
		prefixSb.append(t.getTable()).append(" (");
		
		// Insert columns
		for(int i = 0; i < selectors.size(); i++) {
			QueryColumnSelector c = (QueryColumnSelector) selectors.get(i);
			if(i > 0) {
				prefixSb.append(", ");
			}
			if(c.getColumn().equals(AbstractQueryStruct.PRIM_KEY_PLACEHOLDER)) {
				prefixSb.append(getPrimKey(engine, c.getTable()));
			} else {
				prefixSb.append(c.getColumn());
			}
		}
		prefixSb.append(") VALUES (");
		
		String initial = prefixSb.toString();
		List<Object[]> valueCombinations = flattenCombinations(valGrs);
		
		// h2/sqlite is file based
		// so need to do the whole sync across engines
		// only need for cloud
		if(ClusterUtil.IS_CLUSTER && engine != null) {
			RdbmsTypeEnum eType = ((RDBMSNativeEngine) engine).getQueryUtil().getDbType();
			if(eType == RdbmsTypeEnum.H2_DB || eType == RdbmsTypeEnum.SQLITE) {
				insertFileEngine(engine, queryUtil, initial, valueCombinations, selectors, userId);
				return new NounMetadata(true, PixelDataType.BOOLEAN, PixelOperationType.ALTER_DATABASE);
			}
		}
		
		// determine if we can insert booleans as true/false
		boolean allowBooleanType = queryUtil.allowBooleanDataType();
		
		// we are a frame
		// or a tcp/ip rdbms type
		for(Object[] values : valueCombinations) {
			StringBuilder valuesSb = new StringBuilder();
			// Insert values
			for(int i = 0; i < values.length; i++) {
				// add comma for next value
				if(i != 0) {
					valuesSb.append(", ");
				}
				
				// append the value
				if(values[i] == null) {
					valuesSb.append("NULL");
				} 
				else if(values[i] instanceof String) {
					if(values[i].equals("<UUID>")) {
						valuesSb.append("'" + RdbmsQueryBuilder.escapeForSQLStatement(UUID.randomUUID().toString()) + "'");
					} else if(values[i].equals("<USER_ID>")) {
						valuesSb.append("'" + RdbmsQueryBuilder.escapeForSQLStatement(userId) + "'");
					} else {
						valuesSb.append("'" + RdbmsQueryBuilder.escapeForSQLStatement(values[i] + "") + "'");
					}
				}
				else if(values[i] instanceof SemossDate) {
					String dateValue = ((SemossDate) values[i]).getFormattedDate();
					if(dateValue == null || dateValue.isEmpty() || dateValue.equals("null")) {
						valuesSb.append("NULL, ");
					} else {
						valuesSb.append("'" + ((SemossDate) values[i]).getFormattedDate() + "'");
					}
				}
				else if(values[i] instanceof Boolean) {
					if(allowBooleanType) {
						valuesSb.append(values[i]);
					} else {
						// append 1 or 0 based on true/false
						if(Boolean.parseBoolean(values[i] + "")) {
							valuesSb.append(1);
						} else {
							valuesSb.append(0);
						}
					}
				}
				else {
					valuesSb.append(values[i]);
				}
			}
			valuesSb.append(")");

			String query = initial + valuesSb.toString();
			logger.info("SQL QUERY...." + query);
			if(qs.getQsType() == QUERY_STRUCT_TYPE.ENGINE) {
				if(engine == null) {
					throw new NullPointerException("No engine passed in to insert the data");
				}
				try {
					engine.insertData(query);
					if(commit) {
						engine.commit();
					}
				} catch (Exception e) {
					logger.error(Constants.STACKTRACE, e);
					throw new SemossPixelException(
							new NounMetadata("An error occured trying to insert new records in the database", PixelDataType.CONST_STRING, PixelOperationType.ERROR));
				}

				if (engine != null) {
					AuditDatabase audit = engine.generateAudit();
					audit.auditInsertQuery(selectors, Arrays.asList(values), userId, query);
				}
			} else {
				try {
					if (frame != null) {
						((AbstractRdbmsFrame) frame).getBuilder().runQuery(query);
					}
				} catch (Exception e) {
					logger.error(Constants.STACKTRACE, e);
					throw new SemossPixelException(
							new NounMetadata("An error occured trying to insert new records in the frame", PixelDataType.CONST_STRING, PixelOperationType.ERROR));
				}
			}
		}

		return new NounMetadata(true, PixelDataType.BOOLEAN, PixelOperationType.ALTER_DATABASE, PixelOperationType.FORCE_SAVE);
	}
	
	/**
	 * Insert into the engine
	 * @param engine
	 * @param initial
	 * @param valueCombinations
	 * @param selectors
	 * @param userId
	 */
	private void insertFileEngine(IEngine engine, AbstractSqlQueryUtil queryUtil, 
			String initial, List<Object[]> valueCombinations, List<IQuerySelector> selectors, String userId) {
		synchronized(engine) {
			ClusterUtil.reactorPullApp(engine.getEngineId());
			
			// determine if we can insert booleans as true/false
			boolean allowBooleanType = queryUtil.allowBooleanDataType();

			for(Object[] values : valueCombinations) {
				StringBuilder valuesSb = new StringBuilder();
				// insert values
				for(int i = 0; i < values.length; i++) {
					// add comma for next value
					if(i != 0) {
						valuesSb.append(", ");
					}
					
					// append the value
					if(values[i] == null) {
						valuesSb.append("NULL");
					} 
					else if(values[i] instanceof String) {
						if(values[i].equals("<UUID>")) {
							valuesSb.append("'" + RdbmsQueryBuilder.escapeForSQLStatement(UUID.randomUUID().toString()) + "'");
						} else if(values[i].equals("<USER_ID>")) {
							valuesSb.append("'" + RdbmsQueryBuilder.escapeForSQLStatement(userId) + "'");
						} else {
							valuesSb.append("'" + RdbmsQueryBuilder.escapeForSQLStatement(values[i] + "") + "'");
						}
					}
					else if(values[i] instanceof SemossDate) {
						String dateValue = ((SemossDate) values[i]).getFormattedDate();
						if(dateValue == null || dateValue.isEmpty() || dateValue.equals("null")) {
							valuesSb.append("NULL, ");
						} else {
							valuesSb.append("'" + ((SemossDate) values[i]).getFormattedDate() + "'");
						}
					}
					else if(values[i] instanceof Boolean) {
						if(allowBooleanType) {
							valuesSb.append(values[i]);
						} else {
							// append 1 or 0 based on true/false
							if(Boolean.parseBoolean(values[i] + "")) {
								valuesSb.append(1);
							} else {
								valuesSb.append(0);
							}
						}
					}
					else {
						valuesSb.append(values[i]);
					}
				}
				valuesSb.append(")");
				// generate the query
				String query = initial + valuesSb.toString();

				try {
					engine.insertData(query);
					AuditDatabase audit = engine.generateAudit();
					audit.auditInsertQuery(selectors, Arrays.asList(values), userId, query);
				} catch (Exception e) {
					logger.error(Constants.STACKTRACE, e);
					throw new SemossPixelException(
							new NounMetadata("An error occured trying to insert new records in the database", PixelDataType.CONST_STRING, PixelOperationType.ERROR));
				}
			}
			// push back to the cluster
			ClusterUtil.reactorPushApp(engine.getEngineId());
		}
	}
	
	private NounMetadata getQueryStruct() {
		GenRowStruct allNouns = getNounStore().getNoun(PixelDataType.QUERY_STRUCT.getKey());
		NounMetadata queryStruct = null;
		if(allNouns != null) {
			return allNouns.getNoun(0);
		} 
		return queryStruct;
	}
	
	private List<Object[]> flattenCombinations(GenRowStruct valGrs) {
		List<Object[]> combinations = new Vector<>();
		
		Map<Integer, Integer> currIndexMap = new HashMap<>();
		
		int numInputs = valGrs.size();
		boolean moreCombinations = true;
		while(moreCombinations) {
			Object[] row = new Object[numInputs];
			for(int i = 0; i < numInputs; i++) {
				
				Object thisValue = null;
				Object result = valGrs.get(i);
				if(result instanceof List) {
					// if we know which index to grab, lets just grab it
					if(currIndexMap.containsKey(Integer.valueOf(i))) {
						Integer indexToGrab = currIndexMap.get(Integer.valueOf(i));
						thisValue = ((List) result).get(indexToGrab);
					} else {
						thisValue = ((List) result).get(0);
						currIndexMap.put(Integer.valueOf(i), Integer.valueOf(0));
					}
				} else {
					thisValue = result;
				}
				
				// set the value into the current row
				// if this is an array of more than 1 value
				// then it is a list of noun metadatas 
				// and we need to get the value
				if(thisValue instanceof NounMetadata) {
					row[i] = ((NounMetadata)thisValue).getValue();
				} else {
					row[i] = thisValue;
				}
			}
			combinations.add(row);
			
			
			// now we need to know if we should update curr index map
			// or if we are done
			boolean loopAgain = false;
			UPDATE_LOOP : for(int i = numInputs-1; i >=0 ; i--) {
				// we start at the last list
				// and see if the current index is at the end
				Object result = valGrs.get(i);
				if(result instanceof List) {
					Integer indexToGrab = currIndexMap.get(Integer.valueOf(i));
					int numIndicesToGrab = ((List) result).size();
					if( (indexToGrab + 1) == numIndicesToGrab) {
						// we are have iterated through all of this guy
						// so let us reset him
						// BUT, this doesn't mean we know we need to loop again
						// i am just preparing for the case where a list above requires us to start
						// and loop through all the last pieces
						currIndexMap.put(Integer.valueOf(i), Integer.valueOf(0));
					} else {
						// we have not looped through everything in this list
						// we need to loop again
						// after i increase the index to grab
						currIndexMap.put(Integer.valueOf(i), Integer.valueOf(indexToGrab.intValue()+1));
						loopAgain = true;
						break UPDATE_LOOP;
					}
				}
			}
			
			moreCombinations = loopAgain;
		}
		
		return combinations;
	}
	
	/**
	 * Get the primary key for a table
	 * @param engine
	 * @param tableName
	 * @return
	 */
	private String getPrimKey(IEngine engine, String tableName) {
		String physicalUri = engine.getPhysicalUriFromPixelSelector(tableName);
		return engine.getLegacyPrimKey4Table(physicalUri);
	}
	
	public static void main(String[] args) {
		GenRowStruct grs = new GenRowStruct();
		grs.add(new NounMetadata(1, PixelDataType.CONST_INT));
		List<Object> l1 = new Vector<>();
		l1.add("a");
		l1.add("b");
		l1.add("c");
		grs.add(new NounMetadata(l1, PixelDataType.VECTOR));
		List<Object> l2 = new Vector<>();
		l2.add("d");
		l2.add("e");
		grs.add(new NounMetadata(l2, PixelDataType.VECTOR));
		List<Object> l3 = new Vector<>();
		l3.add("x");
		l3.add("y");
		l3.add("z");
		grs.add(new NounMetadata(l3, PixelDataType.VECTOR));
		
		InsertReactor qir = new InsertReactor();
		List<Object[]> combinations = qir.flattenCombinations(grs);
		
		for(int i = 0; i < combinations.size(); i++) {
			logger.debug(Arrays.toString(combinations.get(i)));
		}
	}

	@Override
	public String getName()
	{
		return "Insert";
	}

}
