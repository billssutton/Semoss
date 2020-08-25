package prerna.sablecc2.reactor.scheduler;

import java.util.List;

import org.quartz.Scheduler;

import prerna.engine.api.IRawSelectWrapper;
import prerna.engine.impl.rdbms.RDBMSNativeEngine;
import prerna.query.querystruct.SelectQueryStruct;
import prerna.query.querystruct.filters.GenRowFilters;
import prerna.query.querystruct.selectors.IQuerySort;
import prerna.query.querystruct.selectors.QueryColumnOrderBySelector;
import prerna.query.querystruct.selectors.QueryColumnSelector;
import prerna.rdf.engine.wrappers.WrapperManager;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.om.task.BasicIteratorTask;
import prerna.sablecc2.reactor.AbstractReactor;

public class SchedulerHistoryReactor extends AbstractReactor {

	public SchedulerHistoryReactor() {
		this.keysToGet = new String[] {ReactorKeysEnum.FILTERS.getKey(), ReactorKeysEnum.SORT.getKey(), ReactorKeysEnum.LIMIT.getKey(), ReactorKeysEnum.OFFSET.getKey()};
	}

	@Override
	public NounMetadata execute() {

		Scheduler scheduler = SchedulerFactorySingleton.getInstance().getScheduler();
		// start up scheduler if it isn't on
		SchedulerH2DatabaseUtility.startScheduler(scheduler);
		RDBMSNativeEngine schedulerDb = SchedulerH2DatabaseUtility.getSchedulerDB();

		SelectQueryStruct qs = new SelectQueryStruct();
		qs.addSelector(new QueryColumnSelector(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.JOB_NAME));
		qs.addSelector(new QueryColumnSelector(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.JOB_GROUP));
		qs.addSelector(new QueryColumnSelector(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.EXECUTION_START));
		qs.addSelector(new QueryColumnSelector(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.EXECUTION_END));
		qs.addSelector(new QueryColumnSelector(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.EXECUTION_DELTA));
		qs.addSelector(new QueryColumnSelector(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.SUCCESS));

		GenRowFilters additionalFilters = getFilters();
		if(additionalFilters != null) {
			qs.mergeExplicitFilters(additionalFilters);
		}
		List<IQuerySort> sorts = getSort();
		if(sorts != null) {
			qs.setOrderBy(sorts);
		} else {
			// set default
			qs.addOrderBy(SchedulerConstants.SMSS_AUDIT_TRAIL + "__" + SchedulerConstants.EXECUTION_START, 
					QueryColumnOrderBySelector.ORDER_BY_DIRECTION.DESC.toString());
		}
		qs.setLimit(getLimit());
		qs.setOffSet(getOffset());

		IRawSelectWrapper iterator = null;
		try {
			iterator = WrapperManager.getInstance().getRawWrapper(schedulerDb, qs);
		} catch (Exception e) {
			e.printStackTrace();
			String message = e.getMessage();
			if(message == null || message.isEmpty()) {
				throw new IllegalArgumentException(message);
			} else {
				throw new IllegalArgumentException("An error occured attemping to get your requests");
			}
		}
		BasicIteratorTask task = new BasicIteratorTask(qs, iterator);
		task.setNumCollect(-1);
		return new NounMetadata(task, PixelDataType.FORMATTED_DATA_SET);
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	/*
	 * Additional inputs for filtering
	 */
	
	protected GenRowFilters getFilters() {
		GenRowStruct inputsGRS = this.store.getNoun(ReactorKeysEnum.FILTERS.getKey());
		if (inputsGRS != null && !inputsGRS.isEmpty()) {
			NounMetadata filterNoun = inputsGRS.getNoun(0);
			SelectQueryStruct qs = (SelectQueryStruct) filterNoun.getValue();
			GenRowFilters filters = qs.getCombinedFilters();
			return filters;
		}
		return null;
	}

	protected List<IQuerySort> getSort() {
		GenRowStruct inputsGRS = this.store.getNoun(ReactorKeysEnum.SORT.getKey());
		if (inputsGRS != null && !inputsGRS.isEmpty()) {
			NounMetadata sortNoun = inputsGRS.getNoun(0);
			SelectQueryStruct qs = (SelectQueryStruct) sortNoun.getValue();
			List<IQuerySort> orderBy = qs.getOrderBy();
			return orderBy;
		}
		return null;
	}

	protected int getLimit() {
		GenRowStruct inputsGRS = this.store.getNoun(ReactorKeysEnum.LIMIT.getKey());
		if (inputsGRS != null && !inputsGRS.isEmpty()) {
			NounMetadata limitNoun = inputsGRS.getNoun(0);
			return ((Number) limitNoun.getValue()).intValue();
		}
		return -1;
	}

	protected int getOffset() {
		GenRowStruct inputsGRS = this.store.getNoun(ReactorKeysEnum.OFFSET.getKey());
		if (inputsGRS != null && !inputsGRS.isEmpty()) {
			NounMetadata offsetNoun = inputsGRS.getNoun(0);
			return ((Number) offsetNoun.getValue()).intValue();
		}
		return -1;
	}

}
