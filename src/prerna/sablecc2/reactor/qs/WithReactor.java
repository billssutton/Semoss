package prerna.sablecc2.reactor.qs;

import java.util.List;

import prerna.om.InsightPanel;
import prerna.query.querystruct.QueryStruct2;
import prerna.query.querystruct.filters.GenRowFilters;
import prerna.query.querystruct.selectors.QueryColumnOrderBySelector;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;

public class WithReactor extends QueryStructReactor {

	protected static final String PANEL_KEY = "panel";

	@Override
	QueryStruct2 createQueryStruct() {
		InsightPanel panel = getPanel();
		GenRowFilters panelFilters = panel.getPanelFilters();
		qs.mergeFilters(panelFilters);
		List<QueryColumnOrderBySelector> orderBys = panel.getPanelOrderBys();
		qs.mergeOrderBy(orderBys);
		return qs;
	}

	/**
	 * Get the panel filter grs
	 * @return
	 */
	private InsightPanel getPanel() {
		InsightPanel panel = null;

		// see if panel was passed via generic reactor
		GenRowStruct genericGrs = this.store.getNoun(PANEL_KEY);
		if(genericGrs != null && !genericGrs.isEmpty()) {
			String panelId = genericGrs.get(0).toString();
			panel = this.insight.getInsightPanel(panelId);
		}

		if(panel == null) {
			// if not, see if it was passed in the grs
			List<Object> panelNouns = this.curRow.getValuesOfType(PixelDataType.PANEL);
			if(panelNouns != null && !panelNouns.isEmpty()) {
				panel = (InsightPanel) panelNouns.get(0);
			}
		}

		if(panel == null) {
			throw new IllegalArgumentException("Invalid panel id passed into With reactor");
		}

		return panel;
	}
}
