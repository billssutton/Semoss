package prerna.sablecc2.reactor.insights.recipemanagement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.google.gson.Gson;

import prerna.algorithm.api.SemossDataType;
import prerna.om.Insight;
import prerna.om.Pixel;
import prerna.om.PixelList;
import prerna.query.parsers.GenExpressionWrapper;
import prerna.query.parsers.ParamStruct;
import prerna.query.parsers.ParamStructDetails;
import prerna.query.parsers.ParamStructDetails.BASE_QS_TYPE;
import prerna.query.parsers.ParamStructDetails.QUOTE;
import prerna.query.parsers.SqlParser2;
import prerna.query.querystruct.HardSelectQueryStruct;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.query.querystruct.filters.GenRowFilters;
import prerna.query.querystruct.filters.IQueryFilter;
import prerna.query.querystruct.selectors.IQuerySelector;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.query.querystruct.transform.QSParseParamStruct;
import prerna.sablecc2.PixelPreProcessor;
import prerna.sablecc2.lexer.Lexer;
import prerna.sablecc2.lexer.LexerException;
import prerna.sablecc2.node.Start;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.parser.Parser;
import prerna.sablecc2.parser.ParserException;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.sablecc2.translations.ImportQueryTranslation;
import prerna.util.gson.GsonUtility;

public class ImportParamOptionsReactor extends AbstractReactor {

	public static final String PARAM_OPTIONS = "PARAM_OPTIONS";
	
	@Override
	public NounMetadata execute() {
		PixelList pixelList = this.insight.getPixelList();
		
		Insight tempInsight = new Insight();
		ImportQueryTranslation translation = new ImportQueryTranslation(tempInsight);
		// loop through recipe
		for(Pixel pixel : pixelList) {
			try {
				String pixelId = pixel.getId();
				String expression = pixel.getPixelString();
				translation.setPixelObj(pixel);
				expression = PixelPreProcessor.preProcessPixel(expression.trim(), new ArrayList<String>(), new HashMap<String, String>());
				Parser p = new Parser(new Lexer(new PushbackReader(new InputStreamReader(new ByteArrayInputStream(expression.getBytes("UTF-8"))), expression.length())));
				// parsing the pixel - this process also determines if expression is syntactically correct
				Start tree = p.parse();
				// apply the translation.
				tree.apply(translation);
			} catch (ParserException | LexerException | IOException e) {
				e.printStackTrace();
			}
		}
		
		Map<Pixel, SelectQueryStruct> imports = translation.getImportQsMap();
		// for each import
		// we need to get the proper param struct
		List<Map<String, Object>> params = new Vector<>();
		for(Pixel pixelStep : imports.keySet()) {
			SelectQueryStruct qs = imports.get(pixelStep);
			List<ParamStruct> paramList = getParamsForImport(imports.get(pixelStep), pixelStep);
			Map<String, Map <String, Map<String, Map<String, List<ParamStruct>>>>> paramOutput = organizeStruct(paramList);
			
			Map<String, Object> output = new HashMap<>();
			if(qs instanceof HardSelectQueryStruct) {
				output.put("baseQsType", "hqs");
			} else {
				output.put("baseQsType", "sqs");
			}
			output.put("qsType", qs.getQsType());
			output.put("appId", qs.getEngineId());
			output.put("pixelId", pixelStep.getId());
			output.put("pixelString", pixelStep.getPixelString());
			output.put("params", paramOutput);
			params.add(output);
		}
		
		NounMetadata retMap = new NounMetadata(params, PixelDataType.VECTOR);
		return retMap;
	}
	
	private List<ParamStruct> getParamsForImport(SelectQueryStruct qs, Pixel pixelObj) {
		List<ParamStruct> paramList = new Vector<>();
		
		if(qs instanceof HardSelectQueryStruct || qs.getCustomFrom() != null) {
			// do the logic of getting the params. The only issue here is
			// we assume the latest level which may not be true
			// but let us see
			String query = qs.getCustomFrom();
			if(query == null && qs instanceof HardSelectQueryStruct)
				query = ((HardSelectQueryStruct)qs).getQuery();
			SqlParser2 sqp2 = new SqlParser2();
			sqp2.parameterize = false;
			try {
				GenExpressionWrapper wrapper = sqp2.processQuery(query);
				Iterator <ParamStructDetails> structIterator = wrapper.paramToExpressionMap.keySet().iterator();
				while(structIterator.hasNext()) {
					ParamStructDetails nextStructDetails = structIterator.next();
					nextStructDetails.setBaseQsType(BASE_QS_TYPE.HQS);
					nextStructDetails.setAppId(qs.getEngineId());
					nextStructDetails.setPixelId(pixelObj.getId());
					nextStructDetails.setPixelString(pixelObj.getPixelString());
					ParamStruct nextStruct = new ParamStruct();
					nextStruct.addParamStructDetails(nextStructDetails);
					if(nextStructDetails.getOperator().equalsIgnoreCase("in")) {
						nextStruct.setMultiple(true);
					}
					paramList.add(nextStruct);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// get the filters first
			GenRowFilters importFilters = qs.getExplicitFilters();
			Set<String> filteredColumns = importFilters.getAllQsFilteredColumns();

			QSParseParamStruct parser = new QSParseParamStruct(qs, pixelObj);
			for(IQueryFilter filter : importFilters) {
				parser.parseFilter(filter, paramList);
			}
			
			// the above should be the filtered options
			// lets go through the selectors
			// and what is not filtered will be added as well
			List<String> addedQs = new Vector<>();
			List<IQuerySelector> selectors = qs.getSelectors();
			for(IQuerySelector select : selectors) {
				List<QueryColumnSelector> allColumnSelectors = select.getAllQueryColumns();
				for(QueryColumnSelector colS : allColumnSelectors) {
					
					String colQS = colS.getQueryStructName();
					if(filteredColumns.contains(colQS)) {
						// already have a filter on it
						continue;
					}
					if(addedQs.contains(colQS)) {
						// we have already added this
						continue;
					}
					
					String frameOutput = pixelObj.getFrameOutputs().iterator().next();
					Map<String, Map<String, Object>> endingHeaders = pixelObj.getEndingFrameHeaders();
					Map<String, String> aliasToType = Pixel.getFrameHeadersToDataType(endingHeaders, frameOutput);
					
					ParamStructDetails detailsStruct = new ParamStructDetails();
					detailsStruct.setBaseQsType(BASE_QS_TYPE.SQS);
					detailsStruct.setAppId(qs.getEngineId());
					detailsStruct.setPixelId(pixelObj.getId());
					detailsStruct.setPixelString(pixelObj.getPixelString());
					detailsStruct.setTableName(colS.getTable());
					detailsStruct.setColumnName(colS.getColumn());
					detailsStruct.setOperator("==");
					SemossDataType dataType = SemossDataType.convertStringToDataType(aliasToType.get(colS.getAlias()));
					detailsStruct.setType(PixelDataType.convertFromSemossDataType(dataType));
					if(dataType == SemossDataType.INT || dataType == SemossDataType.DOUBLE) {
						detailsStruct.setQuote(QUOTE.NO);
					}
					ParamStruct pStruct = new ParamStruct();
					pStruct.setMultiple(true);
					pStruct.setSearchable(true);
					pStruct.addParamStructDetails(detailsStruct);
					paramList.add(pStruct);
					// store that this qs has been added
					addedQs.add(colQS);
				}
			}
		}
		
		return paramList;
	}
	
	public Map<String, Map <String, Map<String, Map<String, List<ParamStruct>>>>> organizeStruct(List <ParamStruct> structs) {
		Map<String, Map <String, Map<String, Map<String, List<ParamStruct>>>>> columnMap = new HashMap<>();
		// level 1 - column name
		// column (key) -- List of tables (Value)
		// level 2 - column + table
		// column + table (key) - operator (value)
		// level 3 - column + table + operator
		// column + table + operator(key) - Param Struct(value)
		// level 4 - frames - dont know how to get to this but.. 
		
		for(int paramIndex = 0;paramIndex < structs.size();paramIndex++) {
			ParamStruct thisStruct = structs.get(paramIndex);
			// these structs will always only have 1 struct
			ParamStructDetails thisStructDetails = thisStruct.getDetailsList().get(0);
			String columnName = thisStructDetails.getColumnName();
			String tableName = thisStructDetails.getTableName();
			String opName = thisStructDetails.getOperator();
			String opuName = thisStructDetails.getuOperator();
			if(opuName == null) {
				opuName = opName;
			}
			
			// get the table
			Map <String, Map<String, Map<String, List<ParamStruct>>>> tableMap = null;
			if(columnMap.containsKey(columnName)) {
				tableMap = (Map <String, Map<String, Map<String, List<ParamStruct>>>>) columnMap.get(columnName);
			} else {
				tableMap = new HashMap <String, Map<String, Map<String, List<ParamStruct>>>>();
			}
			
			// get the operator from the table
			Map <String, Map<String, List <ParamStruct>>> opMap = null;
			if(tableMap.containsKey(tableName)) {
				opMap = (Map <String, Map<String, List <ParamStruct>>>)tableMap.get(tableName);
			} else {
				opMap = new HashMap<String, Map<String, List<ParamStruct>>>();
			}
			
			// get the table unique operator
			Map<String, List <ParamStruct>> opuMap = null;
			if(opMap.containsKey(opName)) {
				opuMap = (Map<String, List <ParamStruct>>)opMap.get(opName);
			} else {
				opuMap = new HashMap<String, List<ParamStruct>>();
			}
			
			// get the actual paramstruct
			List <ParamStruct> curList = null;
			if(opuMap.containsKey(opuName)) {
				curList = (List <ParamStruct>) opuMap.get(opuName);
			} else {
				curList = new ArrayList<ParamStruct>();
			}
			
			// add the paramstruct
			curList.add(thisStruct);
			// add the opumap
			opuMap.put(opuName, curList);
			// add it to the operator
			opMap.put(opName, opuMap);
			// put the table
			tableMap.put(tableName, opMap);
			// put the column
			columnMap.put(columnName, tableMap);
		}
		
		return columnMap;
	}

	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////
	
	public static void main(String[] args) {
		ImportParamOptionsReactor reactor = new ImportParamOptionsReactor();
		HardSelectQueryStruct qs = new HardSelectQueryStruct();
		// set you query into the QS for testing
		
//		qs.setQuery("SELECT Account_ID AS \"Account ID\",Master_Consumer_ID AS \"Master Consumer ID\",Recent_Month_Member_Coverage_Count AS \"Recent Month Member Coverage Count\"  FROM  (  SELECT MBRSHP.ACCT_ID AS Account_ID,MBRSHP.MCID AS Master_Consumer_ID,SUM(MBRSHP.RCNT_MNTH_MBR_CVRG_CNT) AS Recent_Month_Member_Coverage_Count,MBRSHP.TM_PRD_NM AS Time_Period  FROM  (  SELECT CII_ACCT_PRFL.ACCT_ID AS ACCT_ID,CII_FACT_MBRSHP.MCID AS MCID,SUM(CASE WHEN CII_FACT_MBRSHP.ELGBLTY_CY_MNTH_END_NBR  = TM_PRD_FNCTN.END_YEAR_MNTH THEN CII_FACT_MBRSHP.MBR_CVRG_CNT ELSE 0 END) AS RCNT_MNTH_MBR_CVRG_CNT,TM_PRD_FNCTN.TM_PRD_NM AS TM_PRD_NM  FROM CII_FACT_MBRSHP INNER JOIN (SELECT YEAR_CD_NM as TM_PRD_NM,\r\n" + 
//				"                STRT_MNTH_NBR as START_YEAR_MNTH,\r\n" + 
//				"                END_MNTH_NBR as END_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 111101\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then STRT_MNTH_NBR\r\n" + 
//				"                                else SRVC_STRT_MNTH_NBR\r\n" + 
//				"                end as STRT_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 888812\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then END_MNTH_NBR\r\n" + 
//				"                                else SRVC_END_MNTH_NBR\r\n" + 
//				"                end as END_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD <> 'Custom' then PAID_END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'PAID' then END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC2' then 888811\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC3' then dm.YEAR_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC1' then PAID_END_MNTH_NBR\r\n" + 
//				"                end as END_RPTG_PAID_YEAR_MNTH\r\n" + 
//				"from\r\n" + 
//				"                DIM_TM_PRD_ADHC dtp\r\n" + 
//				"cross join DIM_MNTH dm\r\n" + 
//				"                where (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --    AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'  \r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_CURNT_MNTH_NBR = 201910\r\n" + 
//				"                and dtp.CSTM_END_CURNT_MNTH_NBR = 202009\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009\r\n" + 
//				")\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_MNTH_NBR = 201810\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_MNTH_NBR = 201909\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2') \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_2_MNTH_NBR = 201801\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_2_MNTH_NBR = 201809\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009) ) TM_PRD_FNCTN ON CII_FACT_MBRSHP.ELGBLTY_CY_MNTH_END_NBR BETWEEN TM_PRD_FNCTN.START_YEAR_MNTH and TM_PRD_FNCTN.END_YEAR_MNTH INNER JOIN CII_ACCT_PRFL ON CII_FACT_MBRSHP.ACCT_ID = CII_ACCT_PRFL.ACCT_ID INNER JOIN (Select ACCT_ID,SGMNTN_DIM_KEY from ACIISST_SGMNTN_BRDG where ACIISST_USER_ID= 16100010 and ACCT_ID in ('W0016437') and SRC_FLTR_ID= '1ddb2347-4dbd-49c9-8cf7-931084301861' and FLTR_SRC_NM= 'User Session')SGMNTN on CII_FACT_MBRSHP.SGMNTN_DIM_KEY = SGMNTN.SGMNTN_DIM_KEY and CII_FACT_MBRSHP.ACCT_ID=SGMNTN.ACCT_ID WHERE CII_FACT_MBRSHP.ACCT_ID in ('W0016437')   GROUP BY CII_ACCT_PRFL.ACCT_ID,CII_FACT_MBRSHP.MCID,TM_PRD_FNCTN.TM_PRD_NM ) AS MBRSHP  GROUP BY MBRSHP.ACCT_ID,MBRSHP.MCID,MBRSHP.TM_PRD_NM ) AS FNL   ORDER BY Account_ID,Master_Consumer_ID,Recent_Month_Member_Coverage_Count");
//		
//		qs.setQuery("SELECT Account_ID AS \"Account ID\",Master_Consumer_ID AS \"Master Consumer ID\",Member_Coverage_Type_Description AS \"Member Coverage Type Description\",Member_Gender_Code AS \"Member Gender Code\",Reporting_Member_Relationship_Description AS \"Reporting Member Relationship Description\",Age_Group_Description AS \"Age Group Description\",Age_In_Years AS \"Age In Years\",Contract_Type_Code AS \"Contract Type Code\",Eligibility_Year_Month_Ending_Number AS \"Eligibility Year Month Ending Number\",State_Code AS \"State Code\",CBSA_Name AS \"CBSA Name\",Member_PCP_Indicator AS \"Member PCP Indicator\",Subscriber_ID AS \"Subscriber ID\",Continuous_Enrollment_for_1_Period AS \"Continuous Enrollment for 1 Period\",Member_Birth_Date AS \"Member Birth Date\",Account_Name AS \"Account Name\",Time_Period_Start AS \"Time Period Start\",Time_Period_End AS \"Time Period End\",Non_Utilizer_Indicator AS \"Non Utilizer Indicator\",Member_Coverage_Count AS \"Member Coverage Count\"  FROM  (  SELECT MBRSHP.ACCT_ID AS Account_ID,MBRSHP.MCID AS Master_Consumer_ID,MBRSHP.MBR_CVRG_TYPE_DESC AS Member_Coverage_Type_Description,MBRSHP.MBR_GNDR_CD AS Member_Gender_Code,MBRSHP.RPTG_MBR_RLTNSHP_DESC AS Reporting_Member_Relationship_Description,MBRSHP.AGE_GRP_DESC AS Age_Group_Description,MBRSHP.AGE_IN_YRS_NBR AS Age_In_Years,MBRSHP.CNTRCT_TYPE_CD AS Contract_Type_Code,MBRSHP.ELGBLTY_CY_MNTH_END_NBR AS Eligibility_Year_Month_Ending_Number,MBRSHP.ST_CD AS State_Code,MBRSHP.CBSA_NM AS CBSA_Name,MBRSHP.PCP_IND AS Member_PCP_Indicator,MBRSHP.FMBRSHP_SBSCRBR_ID AS Subscriber_ID,MBRSHP.CNTNUS_ENRLMNT_1_PRD_CD AS Continuous_Enrollment_for_1_Period,MBRSHP.MBR_BRTH_DT AS Member_Birth_Date,MBRSHP.ACCT_NM AS Account_Name,MBRSHP.TIME_PRD_STRT_NBR AS Time_Period_Start,MBRSHP.TIME_PRD_END_NBR AS Time_Period_End,MBRSHP.Non_Utilizer_Ind AS Non_Utilizer_Indicator,SUM(MBRSHP.SUM_MBR_CVRG_CNT) AS Member_Coverage_Count,MBRSHP.TM_PRD_NM AS Time_Period  FROM  (  SELECT CII_ACCT_PRFL.ACCT_ID AS ACCT_ID,CII_FACT_MBRSHP.MCID AS MCID,DIM_MBR_CVRG_TYPE.MBR_CVRG_TYPE_DESC AS MBR_CVRG_TYPE_DESC,CII_FACT_MBRSHP.MBR_GNDR_CD AS MBR_GNDR_CD,DIM_RPTG_MBR_RLTNSHP.RPTG_MBR_RLTNSHP_DESC AS RPTG_MBR_RLTNSHP_DESC,case when DIM_AGE_GRP.AGE_GRP_DESC='1-17' then '1 through 17' else  DIM_AGE_GRP.AGE_GRP_DESC end AS AGE_GRP_DESC,CII_FACT_MBRSHP.AGE_IN_YRS_NBR AS AGE_IN_YRS_NBR,CII_FACT_MBRSHP.CNTRCT_TYPE_CD AS CNTRCT_TYPE_CD,CII_FACT_MBRSHP.ELGBLTY_CY_MNTH_END_NBR AS ELGBLTY_CY_MNTH_END_NBR,CII_FACT_MBRSHP.ST_CD AS ST_CD,DIM_CBSA.CBSA_NM AS CBSA_NM,CII_FACT_MBRSHP.PCP_IND AS PCP_IND,CII_FACT_MBRSHP.SBSCRBR_ID AS FMBRSHP_SBSCRBR_ID,CE.CNTNUS_ENRLMNT_1_PRD_CD AS CNTNUS_ENRLMNT_1_PRD_CD,DIM_MCID.MBR_BRTH_DT AS MBR_BRTH_DT,CII_ACCT_PRFL.ACCT_NM AS ACCT_NM,CASE WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Current Period'  THEN 201910 WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Prior Period' THEN 201810 WHEN TM_PRD_FNCTN.TM_PRD_NM=  'Prior Period 2'  THEN 201801 end AS TIME_PRD_STRT_NBR,CASE WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Current Period'  THEN 202009 WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Prior Period' THEN 201909 WHEN TM_PRD_FNCTN.TM_PRD_NM=  'Prior Period 2'  THEN 201809 end AS TIME_PRD_END_NBR,UT.Non_Utilizer_Ind AS Non_Utilizer_Ind,SUM(MBR_CVRG_CNT) AS SUM_MBR_CVRG_CNT,TM_PRD_FNCTN.TM_PRD_NM AS TM_PRD_NM  FROM CII_FACT_MBRSHP INNER JOIN (SELECT YEAR_CD_NM as TM_PRD_NM,\r\n" + 
//				"                STRT_MNTH_NBR as START_YEAR_MNTH,\r\n" + 
//				"                END_MNTH_NBR as END_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 111101\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then STRT_MNTH_NBR\r\n" + 
//				"                                else SRVC_STRT_MNTH_NBR\r\n" + 
//				"                end as STRT_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 888812\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then END_MNTH_NBR\r\n" + 
//				"                                else SRVC_END_MNTH_NBR\r\n" + 
//				"                end as END_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD <> 'Custom' then PAID_END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'PAID' then END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC2' then 888811\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC3' then dm.YEAR_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC1' then PAID_END_MNTH_NBR\r\n" + 
//				"                end as END_RPTG_PAID_YEAR_MNTH\r\n" + 
//				"from\r\n" + 
//				"                DIM_TM_PRD_ADHC dtp\r\n" + 
//				"cross join DIM_MNTH dm\r\n" + 
//				"                where (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --    AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'  \r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_CURNT_MNTH_NBR = 201910\r\n" + 
//				"                and dtp.CSTM_END_CURNT_MNTH_NBR = 202009\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009\r\n" + 
//				")\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_MNTH_NBR = 201810\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_MNTH_NBR = 201909\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2') \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_2_MNTH_NBR = 201801\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_2_MNTH_NBR = 201809\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009) ) TM_PRD_FNCTN ON CII_FACT_MBRSHP.ELGBLTY_CY_MNTH_END_NBR BETWEEN TM_PRD_FNCTN.START_YEAR_MNTH and TM_PRD_FNCTN.END_YEAR_MNTH INNER JOIN ( Select\r\n" + 
//				"mbrshp.ACCT_ID,\r\n" + 
//				"mbrshp.MCID,\r\n" + 
//				"mbrshp.mbr_cvrg_type_cd,\r\n" + 
//				"mbrshp.tm_prd_nm,\r\n" + 
//				"Case \r\n" + 
//				"                when (mbrshp.mcid = clms.mcid \r\n" + 
//				"                and mbrshp.mbr_cvrg_type_cd = clms.mbr_cvrg_type_cd) then 'N' \r\n" + 
//				"                Else 'Y' \r\n" + 
//				"End        as Non_Utilizer_Ind\r\n" + 
//				"from\r\n" + 
//				"(\r\n" + 
//				"Select\r\n" + 
//				"fact.ACCT_ID,\r\n" + 
//				"MCID,\r\n" + 
//				"MBR_CVRG_TYPE_CD,\r\n" + 
//				"  TM_PRD_NM\r\n" + 
//				"  from\r\n" + 
//				"cii_fact_mbrshp fact\r\n" + 
//				"\r\n" + 
//				"JOIN (             \r\n" + 
//				"SELECT    YEAR_CD_NM as TM_PRD_NM,\r\n" + 
//				"                STRT_MNTH_NBR as START_YEAR_MNTH,\r\n" + 
//				"                END_MNTH_NBR as END_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 111101\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then STRT_MNTH_NBR\r\n" + 
//				"                                else SRVC_STRT_MNTH_NBR\r\n" + 
//				"                end as STRT_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 888812\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then END_MNTH_NBR\r\n" + 
//				"                                else SRVC_END_MNTH_NBR\r\n" + 
//				"                end as END_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD <> 'Custom' then PAID_END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'PAID' then END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC2' then 888811\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC3' then dm.YEAR_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC1' then PAID_END_MNTH_NBR\r\n" + 
//				"                end as END_RPTG_PAID_YEAR_MNTH\r\n" + 
//				"from\r\n" + 
//				"                DIM_TM_PRD_ADHC dtp\r\n" + 
//				"cross join DIM_MNTH dm\r\n" + 
//				"                where (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --    AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'  \r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_CURNT_MNTH_NBR = 201910\r\n" + 
//				"                and dtp.CSTM_END_CURNT_MNTH_NBR = 202009\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009\r\n" + 
//				")\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_MNTH_NBR = 201810\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_MNTH_NBR = 201909\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2') \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_2_MNTH_NBR = 201801\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_2_MNTH_NBR = 201809\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)) TM_PRD_FNCTN                 \r\n" + 
//				"    ON fact.ELGBLTY_CY_MNTH_END_NBR BETWEEN TM_PRD_FNCTN.START_YEAR_MNTH                \r\n" + 
//				"    and TM_PRD_FNCTN.END_YEAR_MNTH  \r\n" + 
//				"                INNER JOIN (Select ACCT_ID,SGMNTN_DIM_KEY from ACIISST_SGMNTN_BRDG where ACIISST_USER_ID= 16100010 and ACCT_ID in ('W0016437') and SRC_FLTR_ID= 'c83a08ad-f467-4489-81b8-4c04f59eaf34' and FLTR_SRC_NM= 'User Session')SGMNTN on fact.SGMNTN_DIM_KEY = SGMNTN.SGMNTN_DIM_KEY and fact.ACCT_ID=SGMNTN.ACCT_ID  \r\n" + 
//				"WHERE      fact.ACCT_ID = 'W0016437'                  \r\n" + 
//				"GROUP BY  fact.acct_id,  fact.MCID, TM_PRD_FNCTN.TM_PRD_NM, fact.MBR_CVRG_TYPE_CD\r\n" + 
//				") mbrshp\r\n" + 
//				"\r\n" + 
//				"left outer join\r\n" + 
//				"(\r\n" + 
//				"Select\r\n" + 
//				"clm.ACCT_ID,\r\n" + 
//				"MCID,\r\n" + 
//				"MBR_CVRG_TYPE_CD,\r\n" + 
//				"TM_PRD_NM\r\n" + 
//				"\r\n" + 
//				"from\r\n" + 
//				"cii_fact_clm_line clm\r\n" + 
//				"JOIN (             \r\n" + 
//				"SELECT     YEAR_CD_NM as TM_PRD_NM,\r\n" + 
//				"                STRT_MNTH_NBR as START_YEAR_MNTH,\r\n" + 
//				"                END_MNTH_NBR as END_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 111101\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then STRT_MNTH_NBR\r\n" + 
//				"                                else SRVC_STRT_MNTH_NBR\r\n" + 
//				"                end as STRT_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 888812\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then END_MNTH_NBR\r\n" + 
//				"                                else SRVC_END_MNTH_NBR\r\n" + 
//				"                end as END_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD <> 'Custom' then PAID_END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'PAID' then END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC2' then 888811\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC3' then dm.YEAR_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC1' then PAID_END_MNTH_NBR\r\n" + 
//				"                end as END_RPTG_PAID_YEAR_MNTH\r\n" + 
//				"from\r\n" + 
//				"                DIM_TM_PRD_ADHC dtp\r\n" + 
//				"cross join DIM_MNTH dm\r\n" + 
//				"                where (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --    AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'  \r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_CURNT_MNTH_NBR = 201910\r\n" + 
//				"                and dtp.CSTM_END_CURNT_MNTH_NBR = 202009\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009\r\n" + 
//				")\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_MNTH_NBR = 201810\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_MNTH_NBR = 201909\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2') \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_2_MNTH_NBR = 201801\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_2_MNTH_NBR = 201809\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)) TM_PRD_FNCTN                 \r\n" + 
//				"    ON clm.CLM_SRVC_YEAR_MNTH_NBR BETWEEN TM_PRD_FNCTN.START_YEAR_MNTH   \r\n" + 
//				"                and TM_PRD_FNCTN.END_YEAR_MNTH  \r\n" + 
//				"                INNER JOIN (Select ACCT_ID,SGMNTN_DIM_KEY from ACIISST_SGMNTN_BRDG where ACIISST_USER_ID= 16100010 and ACCT_ID in ('W0016437') and SRC_FLTR_ID= 'c83a08ad-f467-4489-81b8-4c04f59eaf34' and FLTR_SRC_NM= 'User Session')SGMNTN on clm.SGMNTN_DIM_KEY = SGMNTN.SGMNTN_DIM_KEY and clm.ACCT_ID=SGMNTN.ACCT_ID\r\n" + 
//				"WHERE      clm.ACCT_ID =  'W0016437'                        \r\n" + 
//				"GROUP BY  clm.acct_id,  clm.MCID, TM_PRD_FNCTN.TM_PRD_NM, clm.MBR_CVRG_TYPE_CD\r\n" + 
//				") clms\r\n" + 
//				"\r\n" + 
//				"                on\r\n" + 
//				"mbrshp.acct_id = clms.acct_id \r\n" + 
//				"                and mbrshp.mcid=clms.mcid  \r\n" + 
//				"                and mbrshp.TM_PRD_NM = clms.tm_prd_nm \r\n" + 
//				"                and mbrshp.mbr_cvrg_type_cd  = clms.mbr_cvrg_type_cd  ) UT\r\n" + 
//				"\r\n" + 
//				"                ON TM_PRD_FNCTN.TM_PRD_NM = UT.TM_PRD_NM  \r\n" + 
//				"                AND  CII_FACT_MBRSHP.ACCT_ID =UT.ACCT_ID  \r\n" + 
//				"                AND  CII_FACT_MBRSHP.MCID =UT.MCID  \r\n" + 
//				"                and CII_FACT_MBRSHP.MBR_CVRG_TYPE_CD = UT.MBR_CVRG_TYPE_CD INNER JOIN CII_ACCT_PRFL ON CII_FACT_MBRSHP.ACCT_ID = CII_ACCT_PRFL.ACCT_ID INNER JOIN DIM_MBR_CVRG_TYPE ON CII_FACT_MBRSHP.MBR_CVRG_TYPE_CD = DIM_MBR_CVRG_TYPE.MBR_CVRG_TYPE_CD INNER JOIN DIM_RPTG_MBR_RLTNSHP ON CII_FACT_MBRSHP.RPTG_MBR_RLTNSHP_CD = DIM_RPTG_MBR_RLTNSHP.RPTG_MBR_RLTNSHP_CD INNER JOIN DIM_AGE_GRP ON CII_FACT_MBRSHP.ACCT_AGE_GRP_KEY = DIM_AGE_GRP.AGE_GRP_KEY INNER JOIN DIM_CBSA ON CII_FACT_MBRSHP.CBSA_ID = DIM_CBSA.CBSA_ID INNER  JOIN ( \r\n" + 
//				"                select      m.TM_PRD_NM,  m.acct_id, m.mcid, m. MBR_CVRG_TYPE_CD ,\r\n" + 
//				"   case\r\n" + 
//				"    when m.mnths = b.mnths\r\n" + 
//				"    and m.MBR_CVRG_TYPE_CD <> 'back_fill' then 'Continuous'\r\n" + 
//				"          when strt_mnth = start_prd\r\n" + 
//				"    and end_mnth<>end_prd\r\n" + 
//				"    and m.MBR_CVRG_TYPE_CD <> 'back_fill' then 'Termed'\r\n" + 
//				"          when strt_mnth <> start_prd\r\n" + 
//				"    and end_mnth=end_prd\r\n" + 
//				"    and m.MBR_CVRG_TYPE_CD <> 'back_fill'then 'Added'\r\n" + 
//				"          when m.MBR_CVRG_TYPE_CD = 'back_fill' Then 'NA'\r\n" + 
//				"          else 'Other'\r\n" + 
//				"    end as CNTNUS_ENRLMNT_1_PRD_CD\r\n" + 
//				"                from      (\r\n" + 
//				"                                select    fact .acct_id,\r\n" + 
//				"                                                                fact .MCID,\r\n" + 
//				"                                                                TM_PRD_FNCTN.TM_PRD_NM,\r\n" + 
//				"                                                                fact.MBR_CVRG_TYPE_CD ,\r\n" + 
//				"                                                                count (distinct fact .ELGBLTY_CY_MNTH_END_NBR) mnths ,\r\n" + 
//				"                                                                min(fact .ELGBLTY_CY_MNTH_END_NBR) strt_mnth ,\r\n" + 
//				"                                                                max(fact .ELGBLTY_CY_MNTH_END_NBR) \r\n" + 
//				"                                                                end_mnth \r\n" + 
//				"                                from      cii_fact_mbrshp fact  JOIN (              \r\n" + 
//				"                                                SELECT      YEAR_CD_NM as TM_PRD_NM,\r\n" + 
//				"                STRT_MNTH_NBR as START_YEAR_MNTH,\r\n" + 
//				"                END_MNTH_NBR as END_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 111101\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then STRT_MNTH_NBR\r\n" + 
//				"                                else SRVC_STRT_MNTH_NBR\r\n" + 
//				"                end as STRT_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                                and INCRD_PAID_CD = 'PAID' then 888812\r\n" + 
//				"                                when TM_PRD_TYPE_CD = 'Custom' then END_MNTH_NBR\r\n" + 
//				"                                else SRVC_END_MNTH_NBR\r\n" + 
//				"                end as END_SRVC_YEAR_MNTH,\r\n" + 
//				"                case\r\n" + 
//				"                                when TM_PRD_TYPE_CD <> 'Custom' then PAID_END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'PAID' then END_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC2' then 888811\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC3' then dm.YEAR_MNTH_NBR\r\n" + 
//				"                                when INCRD_PAID_CD = 'INC1' then PAID_END_MNTH_NBR\r\n" + 
//				"                end as END_RPTG_PAID_YEAR_MNTH\r\n" + 
//				"from\r\n" + 
//				"                DIM_TM_PRD_ADHC dtp\r\n" + 
//				"cross join DIM_MNTH dm\r\n" + 
//				"                where (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --    AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'  \r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_CURNT_MNTH_NBR = 201910\r\n" + 
//				"                and dtp.CSTM_END_CURNT_MNTH_NBR = 202009\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009\r\n" + 
//				")\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2')  \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_MNTH_NBR = 201810\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_MNTH_NBR = 201909\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009)\r\n" + 
//				"                or (dtp.TM_PRD_TYPE_CD = 'Custom'\r\n" + 
//				"                and dtp.INCRD_PAID_CD = 'PAID'\r\n" + 
//				"                and dtp.LAG_MNTH_NBR = 0\r\n" + 
//				"                --AND DIM_TM_PRD_ADHC.TM_3061_RULE_IND = 'N'\r\n" + 
//				"                and dtp.YEAR_ID <= 1\r\n" + 
//				"                --In ('Current','Prior','Prior 2') \r\n" + 
//				"                and dtp.CSTM_STRT_PRIOR_2_MNTH_NBR = 201801\r\n" + 
//				"                and dtp.CSTM_END_PRIOR_2_MNTH_NBR = 201809\r\n" + 
//				"                and dm.YEAR_MNTH_NBR = 202009) ) TM_PRD_FNCTN                      \r\n" + 
//				"                                                ON fact .ELGBLTY_CY_MNTH_END_NBR BETWEEN TM_PRD_FNCTN.START_YEAR_MNTH  \r\n" + 
//				"                                                and TM_PRD_FNCTN. \r\n" + 
//				"                                                                END_YEAR_MNTH INNER JOIN (Select ACCT_ID,SGMNTN_DIM_KEY from ACIISST_SGMNTN_BRDG where ACIISST_USER_ID= 16100010 and ACCT_ID in ('W0016437') and SRC_FLTR_ID= 'c83a08ad-f467-4489-81b8-4c04f59eaf34' and FLTR_SRC_NM= 'User Session')SGMNTN on fact.SGMNTN_DIM_KEY = SGMNTN.SGMNTN_DIM_KEY and fact.ACCT_ID=SGMNTN.ACCT_ID   \r\n" + 
//				"                                WHERE fact .ACCT_ID = 'W0016437'                         \r\n" + 
//				"                                GROUP BY  fact .acct_id,\r\n" + 
//				"                                                                fact .MCID,\r\n" + 
//				"                                                                TM_PRD_FNCTN.TM_PRD_NM,\r\n" + 
//				"                                                                fact .MBR_CVRG_TYPE_CD ) m \r\n" + 
//				"                inner join (\r\n" + 
//				"                                select    \r\n" + 
//				"                                                                case \r\n" + 
//				"                                                                                when YEAR_ID= 1 then 'Current Period'  \r\n" + 
//				"                                                                                when YEAR_ID= 2 then 'Prior Period' \r\n" + 
//				"                                                                                when YEAR_ID= 3 then 'Prior Period 2'   \r\n" + 
//				"                                                                                ELSE    'FOO' \r\n" + 
//				"                                                                END as TM_PRD_NM,\r\n" + 
//				"                                                                max(e_abs) - max(s_abs)+ 1 mnths,\r\n" + 
//				"                                                                max(s_prd) as start_prd,\r\n" + 
//				"                                                                max(e_prd)  as \r\n" + 
//				"                                                                end_prd \r\n" + 
//				"                                from         ( \r\n" + 
//				"                                                select         \r\n" + 
//				"                                                                                CASE \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR =  201910  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 202009 then 1 \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR = 201810  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR =  201909   then 2   \r\n" + 
//				"                                                                                                WHEN  YEAR_MNTH_NBR = 201801  \r\n" + 
//				"                                                                or  YEAR_MNTH_NBR = 201809 then  3 \r\n" + 
//				"                                                                                                ELSE  4 \r\n" + 
//				"                                                                                END  as YEAR_ID,\r\n" + 
//				"                                                                                CASE \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR =  201910  \r\n" + 
//				"                                                                or  YEAR_MNTH_NBR = 201810  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 201801  then ABS_YEAR_MNTH_NBR \r\n" + 
//				"                                                                                                else null \r\n" + 
//				"                                                                                end as s_abs ,\r\n" + 
//				"                                                                                CASE \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR =  201910  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 201810  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 201801  then YEAR_MNTH_NBR \r\n" + 
//				"                                                                                                else null \r\n" + 
//				"                                                                                end as s_prd ,\r\n" + 
//				"                                                                                                 \r\n" + 
//				"                                                                                CASE  \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR = 202009  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR =  201909  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 201809  then ABS_YEAR_MNTH_NBR \r\n" + 
//				"                                                                                                else null \r\n" + 
//				"                                                                                end as e_abs,\r\n" + 
//				"                                                                                CASE \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR = 202009  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR =  201909  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 201809  then YEAR_MNTH_NBR \r\n" + 
//				"                                                                                                else null \r\n" + 
//				"                                                                                end as e_prd                 \r\n" + 
//				"                                                from      dim_mnth   \r\n" + 
//				"                                                where   \r\n" + 
//				"                                                                                CASE \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR =  201910  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR = 202009 then 1               \r\n" + 
//				"                                                                                                WHEN YEAR_MNTH_NBR = 201810  \r\n" + 
//				"                                                                or YEAR_MNTH_NBR =  201909   then 2 \r\n" + 
//				"                                                                                                WHEN  YEAR_MNTH_NBR = 201801  \r\n" + 
//				"                                                                or  YEAR_MNTH_NBR = 201809 then  3 \r\n" + 
//				"                                                                                                ELSE 0 \r\n" + 
//				"                                                                                END  > 0 ) a  \r\n" + 
//				"                                group by \r\n" + 
//				"                                                                case \r\n" + 
//				"                                                                                when YEAR_ID= 1 then 'Current Period' \r\n" + 
//				"                                                                                when YEAR_ID= 2 then 'Prior Period'  \r\n" + 
//				"                                                                                when YEAR_ID= 3 then 'Prior Period 2'   \r\n" + 
//				"                                                                                ELSE    'FOO' \r\n" + 
//				"                                                                END) b     \r\n" + 
//				"                                on  m.TM_PRD_NM = b.TM_PRD_NM) CE              \r\n" + 
//				"                ON TM_PRD_FNCTN.TM_PRD_NM = CE.TM_PRD_NM      \r\n" + 
//				"                AND  CII_FACT_MBRSHP.ACCT_ID =CE.ACCT_ID     \r\n" + 
//				"                AND  CII_FACT_MBRSHP.MCID =CE.MCID     \r\n" + 
//				"                and CII_FACT_MBRSHP.MBR_CVRG_TYPE_CD = CE.MBR_CVRG_TYPE_CD INNER JOIN DIM_MCID ON CII_FACT_MBRSHP.MCID = DIM_MCID.MCID AND CII_FACT_MBRSHP.ACCT_ID = DIM_MCID.ACCT_ID INNER JOIN (Select ACCT_ID,SGMNTN_DIM_KEY from ACIISST_SGMNTN_BRDG where ACIISST_USER_ID= 16100010 and ACCT_ID in ('W0016437') and SRC_FLTR_ID= 'c83a08ad-f467-4489-81b8-4c04f59eaf34' and FLTR_SRC_NM= 'User Session')SGMNTN on CII_FACT_MBRSHP.SGMNTN_DIM_KEY = SGMNTN.SGMNTN_DIM_KEY and CII_FACT_MBRSHP.ACCT_ID=SGMNTN.ACCT_ID WHERE CII_FACT_MBRSHP.ACCT_ID in ('W0016437')   GROUP BY CII_ACCT_PRFL.ACCT_ID,CII_FACT_MBRSHP.MCID,DIM_MBR_CVRG_TYPE.MBR_CVRG_TYPE_DESC,CII_FACT_MBRSHP.MBR_GNDR_CD,DIM_RPTG_MBR_RLTNSHP.RPTG_MBR_RLTNSHP_DESC,case when DIM_AGE_GRP.AGE_GRP_DESC='1-17' then '1 through 17' else  DIM_AGE_GRP.AGE_GRP_DESC end,CII_FACT_MBRSHP.AGE_IN_YRS_NBR,CII_FACT_MBRSHP.CNTRCT_TYPE_CD,CII_FACT_MBRSHP.ELGBLTY_CY_MNTH_END_NBR,CII_FACT_MBRSHP.ST_CD,DIM_CBSA.CBSA_NM,CII_FACT_MBRSHP.PCP_IND,CII_FACT_MBRSHP.SBSCRBR_ID,CE.CNTNUS_ENRLMNT_1_PRD_CD,DIM_MCID.MBR_BRTH_DT,CII_ACCT_PRFL.ACCT_NM,CASE WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Current Period'  THEN 201910 WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Prior Period' THEN 201810 WHEN TM_PRD_FNCTN.TM_PRD_NM=  'Prior Period 2'  THEN 201801 end,CASE WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Current Period'  THEN 202009 WHEN TM_PRD_FNCTN.TM_PRD_NM= 'Prior Period' THEN 201909 WHEN TM_PRD_FNCTN.TM_PRD_NM=  'Prior Period 2'  THEN 201809 end,UT.Non_Utilizer_Ind,TM_PRD_FNCTN.TM_PRD_NM ) AS MBRSHP  GROUP BY MBRSHP.ACCT_ID,MBRSHP.MCID,MBRSHP.MBR_CVRG_TYPE_DESC,MBRSHP.MBR_GNDR_CD,MBRSHP.RPTG_MBR_RLTNSHP_DESC,MBRSHP.AGE_GRP_DESC,MBRSHP.AGE_IN_YRS_NBR,MBRSHP.CNTRCT_TYPE_CD,MBRSHP.ELGBLTY_CY_MNTH_END_NBR,MBRSHP.ST_CD,MBRSHP.CBSA_NM,MBRSHP.PCP_IND,MBRSHP.FMBRSHP_SBSCRBR_ID,MBRSHP.CNTNUS_ENRLMNT_1_PRD_CD,MBRSHP.MBR_BRTH_DT,MBRSHP.ACCT_NM,MBRSHP.TIME_PRD_STRT_NBR,MBRSHP.TIME_PRD_END_NBR,MBRSHP.Non_Utilizer_Ind,MBRSHP.TM_PRD_NM ) AS FNL   ORDER BY Account_ID,Master_Consumer_ID,Member_Coverage_Type_Description,Member_Gender_Code,Reporting_Member_Relationship_Description,Age_Group_Description,Age_In_Years,Contract_Type_Code,Eligibility_Year_Month_Ending_Number,State_Code,CBSA_Name,Member_PCP_Indicator,Subscriber_ID,Continuous_Enrollment_for_1_Period,Member_Birth_Date,Account_Name,Time_Period_Start,Time_Period_End,Non_Utilizer_Indicator,Member_Coverage_Count");
		Pixel pixelObj = new Pixel("1", "testing");
		List<ParamStruct> retObj = reactor.getParamsForImport(qs, pixelObj);
		Gson gson = GsonUtility.getDefaultGson(true);
		System.out.println(gson.toJson(retObj));
	}
	
}
