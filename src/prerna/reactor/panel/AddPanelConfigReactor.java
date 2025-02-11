package prerna.reactor.panel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prerna.om.InsightPanel;
import prerna.sablecc2.om.GenRowStruct;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;

public class AddPanelConfigReactor extends AbstractInsightPanelReactor {
	
	// input keys for the map
	@Deprecated
	private static final String CONFIG = "config";

	public AddPanelConfigReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.PANEL.getKey(), ReactorKeysEnum.CONFIG.getKey()};
	}

	@Override
	public NounMetadata execute() {
		// get the insight panel
		InsightPanel insightPanel = getInsightPanel();
		if(insightPanel == null) {
			throw new IllegalArgumentException("Cannot find the insight panel");
		}
		// get the map
		Map<String, Object> mapInput = getMapInput();
		if(mapInput == null) {
			throw new IllegalArgumentException("Need to define the config input");
		}
		Map<String, Object> config = null;
		// deprecated logic
		if(mapInput.containsKey(CONFIG)) {
			config = (Map<String, Object>) mapInput.get(CONFIG);
			if (config == null) {
				config = new HashMap<String, Object>();
			}
		}
		// the input should always be exactly what we want to input
		else {
			config = mapInput;
		}
		// merge the map options
		insightPanel.addConfig(config);
		return new NounMetadata(insightPanel, PixelDataType.PANEL, PixelOperationType.PANEL_CONFIG);
	}

	private Map<String, Object> getMapInput() {
		// see if it was passed directly in with the lower case key config
		GenRowStruct genericReactorGrs = this.store.getNoun(keysToGet[1]);
		if(genericReactorGrs != null && !genericReactorGrs.isEmpty()) {
			return (Map<String, Object>) genericReactorGrs.get(0);
		}
		
		// see if it is in the curRow
		// if it was passed directly in as a variable
		List<NounMetadata> panelNouns = this.curRow.getNounsOfType(PixelDataType.MAP);
		if(panelNouns != null && !panelNouns.isEmpty()) {
			return (Map<String, Object>) panelNouns.get(0).getValue();
		}
		
		// well, you are out of luck
		return null;
	}
}
