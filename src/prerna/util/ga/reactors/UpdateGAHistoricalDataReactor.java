package prerna.util.ga.reactors;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import prerna.sablecc2.om.NounMetadata;
import prerna.sablecc2.reactor.frame.r.AbstractRFrameReactor;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class UpdateGAHistoricalDataReactor extends AbstractRFrameReactor {

	public static final String DATE_RANGE = "range";
	
	public UpdateGAHistoricalDataReactor() {
		this.keysToGet = new String[]{DATE_RANGE};
	}
	
	@Override
	public NounMetadata execute() {
		init();
		organizeKeys();
		
		// check if packages are installed
		String[] packages = {"RGoogleAnalytics", "httr", "data.table", "jsonlite", "plyr"};
		for(int i = 0 ; i < packages.length ; i++){
			String hasPackage = this.rJavaTranslator.getString("as.character(\"" + packages[i] + "\" %in% rownames(installed.packages()))");
			if (!hasPackage.equalsIgnoreCase("true")) {
				throw new IllegalArgumentException("The " + packages[i] + " package is NOT installed");
			}	
		}
	
		// get start day from inputs, date range default is 5 days
		int dateRange = 5;
		try {
			dateRange = Integer.parseInt(this.keyValue.get(this.keysToGet[0]));
		} catch (NumberFormatException e) {
			// do nothing
		}
		
		// earliest date you can pull is 2018-01-01
		String todayDate = LocalDate.now().toString();
		String startDate;
		LocalDate earliest = LocalDate.of(2018, 01, 01);
		LocalDate start = LocalDate.now().minus(dateRange, ChronoUnit.DAYS);
		if (start.isBefore(earliest)) {
			startDate = earliest.toString();
		} else {
			startDate = start.toString();
		}
		
		// generate script to run viz_tracking.r script
		String userDf = "user_" + Utility.getRandomString(8);
		String historyDf = "hist_" + Utility.getRandomString(8);
		String rDirectory = DIHelper.getInstance().getProperty("BaseFolder") + "\\R\\Recommendations";
		String script = "source(\"" + rDirectory + "\\commons.r\");"
				+ "source(\"" + rDirectory + "\\viz_tracking.r\");"
				+ "library(RGoogleAnalytics);"
				+ "library(httr);"
				+ "library(data.table);"
				+ "library(jsonlite);"
				+ "library(plyr);"
				+ userDf + "<-get_userdata(\"" + startDate + "\",\"" + todayDate + "\", \"" + rDirectory + "\\token_file\");"
				+ historyDf + "<-viz_history(" + userDf + ");"
				+ "write.csv(" + historyDf + ",file=\"" + rDirectory + "\\historicalData\\viz_history.csv\",row.names=FALSE,na=\"\");";
		script = script.replace("\\", "\\\\");
	
		// run script
		this.rJavaTranslator.runR(script);
		
		// garbage cleanup
		String gc = "rm(" + userDf + ", " + historyDf + ")";
		this.rJavaTranslator.runR(gc);
		
		return null;
	}
	
	///////////////////////// KEYS /////////////////////////////////////
	
	@Override
	protected String getDescriptionForKey(String key) {
		if (key.equals(DATE_RANGE)) {
			return "The amount of days of hsitorical data to collect.";
		} else {
			return super.getDescriptionForKey(key);
		}
	}
}
