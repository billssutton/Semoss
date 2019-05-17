package prerna.sablecc2.reactor.frame;

import java.util.Set;

import prerna.algorithm.api.ITableDataFrame;
import prerna.engine.api.IRawSelectWrapper;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.query.querystruct.filters.GenRowFilters;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.VarStore;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.imports.IImporter;
import prerna.sablecc2.reactor.imports.ImportFactory;

public class PurgeReactor extends AbstractFrameReactor {

	@Override
	public NounMetadata execute() {
		// get the frame
		ITableDataFrame frame = getFrame();
		GenRowFilters curFilters = frame.getFrameFilters().copy();
		SelectQueryStruct qs = frame.getMetaData().getFlatTableQs();
		qs.setExplicitFilters(curFilters);
		qs.setFrame(frame);
		IRawSelectWrapper it = frame.query(qs);
		
		// new frame
		ITableDataFrame newFrame = FrameFactory.getFrame(this.insight, FrameFactory.getFrameType(frame), null);
		// insert the data for the new frame
		IImporter importer = ImportFactory.getImporter(newFrame, qs, it);
		importer.insertData();		
		
		NounMetadata noun = new NounMetadata(newFrame, PixelDataType.FRAME, PixelOperationType.FRAME);
		// see if this is overriding any reference
		VarStore varStore = this.insight.getVarStore();
		// override the references
		Set<String> curReferences = varStore.getAllAliasForObjectReference(frame);
		// switch to the new frame
		for(String reference : curReferences) {
			varStore.put(reference, noun);
		}
		
		// return the noun
		return noun;
	}

}
