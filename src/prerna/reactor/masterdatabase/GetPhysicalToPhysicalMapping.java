package prerna.reactor.masterdatabase;

import prerna.ds.rdbms.h2.H2Frame;
import prerna.query.querystruct.AbstractQueryStruct.QUERY_STRUCT_TYPE;
import prerna.query.querystruct.HardSelectQueryStruct;
import prerna.reactor.AbstractReactor;
import prerna.reactor.imports.RdbmsImporter;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Constants;
import prerna.util.Utility;

public class GetPhysicalToPhysicalMapping extends AbstractReactor {

	@Override
	public NounMetadata execute() {
		String query = "SELECT "
				+ "e.engineName as \"engine1\", "
				+ "ec.physicalName as \"physicalname1\", "
				+ "c.logicalName as \"logicalname\", "
				+ "ec2.physicalName as \"physicalname2\", "
				+ "e2.engineName as \"engine2\" "
				+ "from Engine e "
				+ "INNER JOIN EngineConcept ec ON e.id=ec.engine "
				+ "INNER JOIN Concept c on ec.localConceptID = c.localConceptID "
				+ "INNER JOIN EngineConcept ec2 on ec2.localConceptID = c.localConceptID "
				+ "INNER JOIN Engine e2 ON e2.id=ec2.engine";
		
		HardSelectQueryStruct qs = new HardSelectQueryStruct();
		qs.setQuery(query);
		qs.setQsType(QUERY_STRUCT_TYPE.RAW_ENGINE_QUERY);
		qs.setEngine(Utility.getDatabase(Constants.LOCAL_MASTER_DB));
		
		H2Frame frame = new H2Frame();
		RdbmsImporter importer = new RdbmsImporter(frame, qs);
		importer.insertData();
		this.insight.setDataMaker(frame);
		return new NounMetadata(frame, PixelDataType.FRAME, PixelOperationType.FRAME);
	}

}
