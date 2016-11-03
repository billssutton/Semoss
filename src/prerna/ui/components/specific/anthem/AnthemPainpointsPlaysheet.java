package prerna.ui.components.specific.anthem;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.ds.h2.H2Frame;
import prerna.ui.components.playsheets.TablePlaySheet;
import prerna.ui.components.playsheets.datamakers.DataMakerComponent;
import prerna.ui.components.playsheets.datamakers.IDataMaker;

public class AnthemPainpointsPlaysheet extends TablePlaySheet implements IDataMaker {

	private static final Logger logger = LogManager.getLogger(TablePlaySheet.class.getName());
	private DataMakerComponent dmComponent;
////	static String masterQuery = SELECT DISTINCT OBA_L1.OBA_L0_FK AS OBA_L0, OBA_L1.OBA_L1 AS OBA_L1, OBA_L2.OBA_L2 AS OBA_L2, PAIN_POINT.PAIN_POINT_DESCRIPTION AS PAIN_POINT, SOURCE_DOMAIN.SOURCE_DOMAIN AS SOURCE_DOMAIN  FROM OBA_L1, PAIN_POINT, SOURCE_DOMAIN INNER JOIN OBA_L2 ON OBA_L1.OBA_L1 = OBA_L2.OBA_L1_FK AND PAIN_POINT.PAIN_POINT = OBA_L2.PAIN_POINT_FK  and SOURCE_DOMAIN.SOURCE_DOMAIN = PAIN_POINT.SOURCE_DOMAIN_FK where OBA_L2.OBA_TYPE = 'PD' "where OBA_L2 is not null";"

	//OBA_L0.OBA_L0.OBA_L1.OBA_L0_FK
	private final String OBA_L0 = "OBA_L0_FK";
	private final String OBA_L1 = "OBA_L1";
	private final String OBA_L2 = "OBA_L2";
	private final String SOURCE = "SOURCE_DOMAIN";
	private final String PAIN_POINT = "PAIN_POINT";

	public static String instanceOfPlaysheet = "prerna.ui.components.specific.anthem.AnthemPainpointsPlaysheet";

	//create a datamaker
	@Override
	public void createData() {
		if (this.dmComponent == null) {
			this.dmComponent = new DataMakerComponent(this.engine, this.query);
		}

		if(this.dataFrame == null) {
			this.dataFrame = new H2Frame();
		}
		this.dataFrame.processDataMakerComponent(this.dmComponent);
	}


	@Override
	public void setUserId(String userId) {
		if(this.dataFrame == null) {
			this.dataFrame = new H2Frame();
		}
		this.dataFrame.setUserId(userId);
	}

	@Override
	public Map getDataMakerOutput(String... selectors) {
		Map<String, Object> returnHashMap = super.getDataMakerOutput(selectors);
		returnHashMap.put("styling", "Anthem");
		returnHashMap.put("dataTableAlign", getDataTableAlign());
		return returnHashMap;
	}     

	public Map getDataTableAlign (){
		Map<String, String> dataTableAlign = new HashMap <String, String> ();
		dataTableAlign.put("levelOne", OBA_L0);
		dataTableAlign.put("levelTwo", OBA_L1);
		dataTableAlign.put("levelThree", OBA_L2);
		
		return dataTableAlign;
	}

}
