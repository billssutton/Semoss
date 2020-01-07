package prerna.sablecc2.reactor.frame.rdbms;

import java.util.List;
import java.util.Vector;

import prerna.ds.OwlTemporalEngineMeta;
import prerna.ds.rdbms.AbstractRdbmsFrame;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.frame.AbstractFrameReactor;

public class ExtractNumbersReactor extends AbstractFrameReactor {
	public static final String COLUMNS = "columns";
	public static final String OVERRIDE = "override";
	public static final String NUMERIC_COLUMN_NAME = "_NUMERIC";

	@Override
	public NounMetadata execute() {
		AbstractRdbmsFrame frame = (AbstractRdbmsFrame) getFrame();
		// get table name
		String table = frame.getName();
		// get columns to extract numeric characters
		List<String> columns = getColumns();
		// check if user want to override the column or create new columns
		boolean overrideColumn = getOverride();
		// update existing columns
		if (overrideColumn) {
			String update = "";
			for (int i = 0; i < columns.size(); i++) {
				String column = columns.get(i);
				update += "UPDATE " + table + " SET " + column + "= REGEXP_REPLACE(" + column + ", '[^0-9]', ''); ";
			}
			try {
				frame.getBuilder().runQuery(update);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// create new columns
		else {
			for (int i = 0; i < columns.size(); i++) {
				String column = columns.get(i);
				String newColumn = getCleanNewColName(frame, column + NUMERIC_COLUMN_NAME);
				// add new column
				String update = "ALTER TABLE " + table + " ADD " + newColumn + " varchar(800);";
				// update extract alpha characters and underscores
				update += "UPDATE " + table + " SET " + newColumn + " = REGEXP_REPLACE(" + column + ", '[^0-9]', '');";
				try {
					frame.getBuilder().runQuery(update);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// if query runs successfully add new column metadata
				OwlTemporalEngineMeta metaData = frame.getMetaData();
				metaData.addProperty(table, table + "__" + newColumn);
				metaData.setAliasToProperty(table + "__" + newColumn, newColumn);
				metaData.setDataTypeToProperty(table + "__" + newColumn, "String");
			}
		}
		return new NounMetadata(frame, PixelDataType.FRAME, PixelOperationType.FRAME_DATA_CHANGE);
	}

	private List<String> getColumns() {
		GenRowStruct grs = this.store.getNoun(COLUMNS);
		Vector<String> columns = new Vector<String>();
		NounMetadata noun;
		if (grs != null) {
			for (int i = 0; i < grs.size(); i++) {
				noun = grs.getNoun(i);
				if (noun != null) {
					String column = noun.getValue() + "";
					if (column.length() > 0) {
						columns.add(column);
					}
				}
			}
		}
		return columns;
	}

	private boolean getOverride() {
		GenRowStruct grs = this.store.getNoun(OVERRIDE);
		boolean override = false;
		NounMetadata noun;
		if (grs != null) {
			noun = grs.getNoun(0);
			override = (Boolean) noun.getValue();
		}
		return override;
	}

}
