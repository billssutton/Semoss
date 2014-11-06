package prerna.ui.components.specific.tap;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.rdf.engine.api.IEngine;
import prerna.ui.components.playsheets.DualEngineGridPlaySheet;
import prerna.util.DIHelper;

public class DHMSMIntegrationSavingsPerFiscalYearBySiteProcessor {

	private static final Logger LOGGER = LogManager.getLogger(DHMSMIntegrationSavingsPerFiscalYearBySiteProcessor.class.getName());

	private final String masterQuery = "TAP_Site_Data&HR_Core&SELECT DISTINCT ?Wave ?HostSiteAndFloater ?System WHERE { {?Wave <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Wave>} { {?HostSiteAndFloater <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DCSite>} {?Wave <http://semoss.org/ontologies/Relation/Contains> ?HostSiteAndFloater} {?SystemDCSite <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemDCSite>} {?SystemDCSite <http://semoss.org/ontologies/Relation/DeployedAt> ?HostSiteAndFloater} {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?System <http://semoss.org/ontologies/Relation/DeployedAt> ?SystemDCSite} } UNION { {?HostSiteAndFloater <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Floater>} {?HostSiteAndFloater <http://semoss.org/ontologies/Relation/Supports> ?Wave} {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?HostSiteAndFloater <http://semoss.org/ontologies/Relation/Supports> ?System} } } &SELECT DISTINCT ?System WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem> }{?System <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?Probability}}BINDINGS ?Probability {('High') ('Question')}&false&false";
	private String masterQueryForSingleSystem = "TAP_Site_Data&HR_Core&SELECT DISTINCT ?Wave ?HostSiteAndFloater ?System WHERE { BIND(@SYSTEM@ AS ?System) {?Wave <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Wave>} { {?HostSiteAndFloater <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/DCSite>} {?Wave <http://semoss.org/ontologies/Relation/Contains> ?HostSiteAndFloater} {?SystemDCSite <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SystemDCSite>} {?SystemDCSite <http://semoss.org/ontologies/Relation/DeployedAt> ?HostSiteAndFloater} {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?System <http://semoss.org/ontologies/Relation/DeployedAt> ?SystemDCSite} } UNION { {?HostSiteAndFloater <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Floater>} {?HostSiteAndFloater <http://semoss.org/ontologies/Relation/Supports> ?Wave} {?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/System>} {?HostSiteAndFloater <http://semoss.org/ontologies/Relation/Supports> ?System} } } &SELECT DISTINCT ?System WHERE {{?System <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/ActiveSystem> }{?System <http://semoss.org/ontologies/Relation/Contains/Probability_of_Included_BoS_Enterprise_EHRS> ?Probability}}BINDINGS ?Probability {('High') ('Question')}&false&false";
	
	private final String TAP_PORTFOLIO = "TAP_Portfolio";
	private final String TAP_SITE = "TAP_Site_Data";
	private IEngine tapPortfolio;
	private IEngine tapSite;

	private DualEngineGridPlaySheet dualQueries = new DualEngineGridPlaySheet();	

	private HashMap<String, Double[]> sysSustainmentInfoHash;
	private HashMap<String, Double> numSitesForSysHash;
	private HashMap<String, HashMap<String, Double>> sysSiteSupportAndFloaterCostHash;
	private HashMap<String, String[]> waveStartEndDate;
	private HashMap<String, String> lastWaveForSitesAndFloatersInMultipleWavesHash;
	private HashMap<String, String> lastWaveForEachSystem;
	private HashMap<String, HashMap<String, ArrayList<String>>> masterHash;
	private HashMap<String, double[]> savingsData = new HashMap<String, double[]>();

	private ArrayList<Object[]> list;
	private String[] names;

	public ArrayList<Object[]> getList() {
		return list;
	}

	public String[] getNames() {
		return names;
	}

	public void processData() {
		Integer minYear = 3000; // arbitrarily large year
		Integer maxYear = 0;
		for(String wave : waveStartEndDate.keySet()) {
			String[] startDate = waveStartEndDate.get(wave);
			String startTime[] = startDate[0].split("FY");
			String endTime[] = startDate[1].split("FY");
			String startYear = startTime[1];
			String endYear = endTime[1];
			int startYearAsNum = Integer.parseInt(startYear);
			int endYearAsNum = Integer.parseInt(endYear);
			if(endYearAsNum > maxYear) {
				maxYear = endYearAsNum;
			} else if(startYearAsNum < minYear) {
				minYear = startYearAsNum;
			}
		}
		int numColumns = maxYear - minYear + 2; // costs gains are realized a year after
		double[] inflationArr = new double[numColumns+1];
		int i;
		for(i = 0; i < numColumns+1; i++) {
			if(i <= 1) {
				inflationArr[i] = 1;
			} else {
				// only add inflation for years we don't have O&M budget info for
				inflationArr[i] = Math.pow(1.03, i-1);
			}
		}
		HashMap<String, Double> sysSavings = new HashMap<String, Double>();
		

		for(String wave : waveStartEndDate.keySet()) 
		{
			String[] startDate = waveStartEndDate.get(wave);
			String endTime[] = startDate[1].split("FY");
			String endYear = endTime[1];

			int sustainmentIndex = 0;
			switch(endYear) {
			case "2017" : sustainmentIndex = 2; break;
			case "2018" : sustainmentIndex = 3; break;
			default : sustainmentIndex = 4;
			}

			int outputYear = Integer.parseInt(endYear) - minYear + 1;

			HashMap<String, ArrayList<String>> sites = masterHash.get(wave);
			if(sites != null) {
				for(String site : sites.keySet()) {
					if(site.equals("NAVBASE_KITSAP_BREMERTON")) {
						System.out.println("");
					}
					boolean addSite = false;
					if(!lastWaveForSitesAndFloatersInMultipleWavesHash.containsKey(site)) {
						addSite = true;
					} else {
						String lastWave = lastWaveForSitesAndFloatersInMultipleWavesHash.get(site); 
						if(lastWave.equals(wave)) {
							addSite = true;
						} else {
							addSite = false;
						}
					}
					if(addSite) {
						ArrayList<String> systems = sites.get(site);
						double[] yearlySavings = new double[numColumns];
	
						int counter = 0;
						for(String system : systems) {
							boolean notAdded = true;
							
							for(int index = outputYear; index < numColumns; index++) {
								double savings = 0.0;
								if(sysSiteSupportAndFloaterCostHash.containsKey(system)) {
									// if we have cost information at the site lvl
									HashMap<String, Double> siteSupportCostForSystem = sysSiteSupportAndFloaterCostHash.get(system);
									if(siteSupportCostForSystem.containsKey(site)) {
										savings += siteSupportCostForSystem.get(site);
									} else {
										savings += 0;
									}
									// store amount saved each individual time a sytem is decommissioned
									if(notAdded) {
										if(sysSavings.containsKey(system)) {
											double currSiteSavings = sysSavings.get(system);
											currSiteSavings += savings * inflationArr[index];
											sysSavings.put(system, currSiteSavings);
										} else {
											sysSavings.put(system, savings *inflationArr[index]);
										}
									}
								} else {
									// if we do not have cost information at the site lvl
									Double[] costs = sysSustainmentInfoHash.get(system);
									// assume cost for a specific site is total cost / num sites
									double numSites = numSitesForSysHash.get(system);
									if(costs != null){
										if(costs[sustainmentIndex + counter] == null) {
											savings += 0;
										} else {
											savings += costs[sustainmentIndex + counter] / numSites;
										}
									}
								}
								
								if(sustainmentIndex+counter < 4) {
									counter++;
								}
								yearlySavings[index] = savings * inflationArr[index+1];
							}
						}
	
						if(savingsData.containsKey(site)) {
							double[] currSavings = savingsData.get(site);
							for(int index = 0; index < currSavings.length; index++) {
								currSavings[index] += yearlySavings[index];
							}
							savingsData.put(site, currSavings);
						} else {
							savingsData.put(site, yearlySavings);
						}
					}
				}
			}
		}

		list = new ArrayList<Object[]>();
		int numCols = 0;
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
		symbols.setGroupingSeparator(',');
		NumberFormat formatter = new DecimalFormat("'$' ###,##0.00", symbols);
		double[] totalCol = null; 
		for(String site : savingsData.keySet()) {
			double[] values = savingsData.get(site);

			if(list.isEmpty()) {
				numCols = values.length+2;
				totalCol = new double[numCols-2];
				names = new String[numCols];
				int fy = minYear; // pass min year to start table
				int index;
				for(index = 0; index < numCols - 1; index++) {
					if(index == 0) {
						names[0] = "HostSite/Floater";
					}
					String fyString = "" + fy;
					fyString = "FY" + fyString.substring(2,4);
					names[index+1] = fyString;
					fy++;				
				}
				names[index] = "Total";
			}

			Object[] row = new Object[numCols];
			double totalRow = 0;
			row[0] = site;
			int index;
			for(index = 0; index < numCols - 2; index++) {
				double value = values[index];
				totalRow+=value;
				row[index + 1] = formatter.format(values[index]);
				totalCol[index] += value;
			}
			row[index+1] = formatter.format(totalRow);
			list.add(row);
		}
		
		//add fixed cost and column totals
		Object[] row = new Object[numCols];
		row[0] = "Total";
		double combinedTotal = 0;
		int index;
		for(index = 0; index < numCols - 2; index++) {
			combinedTotal += totalCol[index];
		}
		
		// need to determine when last wave for each system we have information for was decommissioned
		Object[] sustainmentRow = new Object[numCols];
		double[] yearlySavings = new double[numCols - 2];
		for(String system : sysSiteSupportAndFloaterCostHash.keySet()) {
			Double currSiteSavings = sysSavings.get(system);
			if(currSiteSavings != null) {
				String wave = lastWaveForEachSystem.get(system);
				String[] startDate = waveStartEndDate.get(wave);
				String endTime[] = startDate[1].split("FY");
				String endYear = endTime[1];
				int outputYear = Integer.parseInt(endYear) - minYear + 1;
				
				// find last non-null cost information
				Double[] costArr = sysSustainmentInfoHash.get(system);
				int position = costArr.length - 1;
				boolean loop = true;
				while(loop) {
					if(costArr[position] == null) {
						position = position - 1;
					} else {
						loop = false;
					}
				}
				double savings = costArr[position]; 
				
				for(index = outputYear; index < numCols - 2; index++) {
					double inflatedSavings = savings * inflationArr[index-position];
					yearlySavings[index] += inflatedSavings - currSiteSavings;
				}
			}
		}
		sustainmentRow[0] = "Fixed_Sustainment_Cost";
		double totalSustainment = 0;
		for(index = 1; index < numCols - 1; index++) {
			double fixedAmount = yearlySavings[index-1];
			sustainmentRow[index] = formatter.format(fixedAmount);
			row[index] = formatter.format(totalCol[index-1] + fixedAmount);
			totalSustainment += fixedAmount;
		}
		sustainmentRow[numCols - 1] = formatter.format(totalSustainment);
		row[numCols - 1] = formatter.format(combinedTotal + totalSustainment);
		list.add(sustainmentRow);
		list.add(row);
	}

	public void runSupportQueries() {
		this.tapPortfolio = (IEngine) DIHelper.getInstance().getLocalProp(TAP_PORTFOLIO);
		this.tapSite = (IEngine) DIHelper.getInstance().getLocalProp(TAP_SITE);

		sysSustainmentInfoHash = DHMSMDeploymentHelper.getSysSustainmentBudget(tapPortfolio);
		
		HashMap<String, HashMap<String, Double>> sysSiteSupportCostHash = DHMSMDeploymentHelper.getSysSiteSupportCost(tapPortfolio);
		HashMap<String, HashMap<String, Double>> sysFloaterCostHash = DHMSMDeploymentHelper.getSysFloaterCost(tapPortfolio);
		sysSiteSupportAndFloaterCostHash = addAllCostInfo(sysSiteSupportCostHash, sysFloaterCostHash);
		
		numSitesForSysHash = DHMSMDeploymentHelper.getNumSitesSysDeployedAt(tapSite);
		
		ArrayList<String> waveOrder = DHMSMDeploymentHelper.getWaveOrder(tapSite);
		HashMap<String, List<String>> sitesInMultipleWavesHash = DHMSMDeploymentHelper.getSitesAndMultipleWaves(tapSite);
		lastWaveForSitesAndFloatersInMultipleWavesHash = DHMSMDeploymentHelper.determineLastWaveForInput(waveOrder, sitesInMultipleWavesHash);
		
		HashMap<String, List<String>> floaterWaveList = DHMSMDeploymentHelper.getFloatersAndWaves(tapSite);
		lastWaveForSitesAndFloatersInMultipleWavesHash.putAll(DHMSMDeploymentHelper.determineLastWaveForInput(waveOrder, floaterWaveList));
		
		waveStartEndDate = DHMSMDeploymentHelper.getWaveStartAndEndDate(tapSite);
		
		lastWaveForEachSystem = DHMSMDeploymentHelper.getLastWaveForEachSystem(tapSite, waveOrder);
	}

	private HashMap<String, HashMap<String, Double>> addAllCostInfo(HashMap<String, HashMap<String, Double>> sysSiteSupportCostHash, HashMap<String, HashMap<String, Double>> sysFloaterCostHash) {
		HashMap<String, HashMap<String, Double>> retHash = new HashMap<String, HashMap<String, Double>>();
		retHash.putAll(sysSiteSupportCostHash);
		
		for(String key : sysFloaterCostHash.keySet()) {
			if(retHash.containsKey(key)) {
				HashMap<String, Double> innerHash = retHash.get(key);
				innerHash.putAll(sysFloaterCostHash.get(key));
			} else {
				retHash.put(key, sysFloaterCostHash.get(key));
			}
		}
		
		return retHash;
	}

	public void runMainQuery(String sysURI) {
		String query = masterQuery;
		if(!sysURI.isEmpty()) {
			query = masterQueryForSingleSystem.replace("@SYSTEM@", sysURI);
		}

		LOGGER.info(query);

		if(masterHash == null) {
			masterHash = new HashMap<String, HashMap<String, ArrayList<String>>>();
			//Use Dual Engine Grid to process the dual query that gets cost info
			dualQueries.setQuery(query);
			dualQueries.createData();
			ArrayList<Object[]> deploymentInfo = dualQueries.getList();
			int i;
			int size = deploymentInfo.size();
			for(i = 0; i < size; i++) {
				Object[] info = deploymentInfo.get(i);
				// wave is first index in Object[]
				// site is second index in Object[]
				// system is third index in Object[]
				String wave = info[0].toString();
				String site = info[1].toString();
				String system = info[2].toString();
				if(masterHash.containsKey(wave)) {
					HashMap<String, ArrayList<String>> waveInfo = masterHash.get(wave);
					if(waveInfo.containsKey(site)) {
						ArrayList<String> systemList = waveInfo.get(site);
						systemList.add(system);
					} else {
						// put site info
						ArrayList<String> systemList = new ArrayList<String>();
						systemList.add(system);
						waveInfo.put(site, systemList);
					}
				} else {
					// put from wave info
					ArrayList<String> systemList = new ArrayList<String>();
					systemList.add(system);
					HashMap<String, ArrayList<String>> newWaveInfo = new HashMap<String, ArrayList<String>>();
					newWaveInfo.put(site, systemList);
					masterHash.put(wave, newWaveInfo);
				}
			} 
		}
	}


}