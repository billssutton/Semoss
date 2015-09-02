package prerna.ui.components.specific.ousd;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import prerna.ds.OrderedBTreeDataFrame;
import prerna.ui.components.playsheets.GridPlaySheet;

public class RoadmapCleanTableComparisonPlaySheet extends GridPlaySheet{

	OUSDTimeline timeline;
	OUSDTimeline comparatorTimeline;
	String roadmapName;
	String comparatorName;

	@Override
	public void setQuery(String query){
		String delimiters = "[;]";
		String[] insights = query.split(delimiters);
		roadmapName = insights[0];
		comparatorName = insights[1];
	}

	@Override
	public void createData(){

		try{
			timeline = OUSDPlaysheetHelper.buildTimeline(this.engine, roadmapName);
		}catch (ClassNotFoundException | InstantiationException | IllegalAccessException e){
			e.printStackTrace();
		}

		try{
			comparatorTimeline = OUSDPlaysheetHelper.buildTimeline(this.engine, comparatorName);
		}catch (ClassNotFoundException | InstantiationException | IllegalAccessException e){
			e.printStackTrace();
		}

		List<Integer> fyList = timeline.getFiscalYears();

		List<Integer> comparatorFyList = comparatorTimeline.getFiscalYears();

		List<Integer> yearList = new ArrayList<Integer>();
		yearList.addAll(fyList);

		for(Integer year: comparatorFyList){
			if(!yearList.contains(year)){
				yearList.add(year);
			}
		}
		Collections.sort(yearList);

		yearList.add(yearList.get(yearList.size()-1)+1);

		String[] columns = new String[yearList.size() + 1];
		columns[0] = "System";
		int count = 1;
		for (Integer year : yearList){
			columns[count] = year+"";
			count++;
		}

		this.dataFrame = new OrderedBTreeDataFrame(columns);

		createTable(yearList, timeline, fyList, comparatorTimeline, comparatorFyList);
	}

	@Override
	public Hashtable getData(){
		Hashtable<String, Object> map = (Hashtable<String, Object>) super.getData();
		//		map.put("data", this.timeline.getGanttData());
		//		map.put("headers", this.timeline.getGanttHeaders());
		map.put("data", this.timeline.getDashboardData());
		map.put("headers", new System[0]);
		return map;
	}

	private void createTable(List<Integer> yearList, OUSDTimeline timeline, List<Integer> fyList, OUSDTimeline comparatorTimeline, List<Integer> comparatorFyList){

		//		this.list = new ArrayList<Object[]>();
		String[] names = this.dataFrame.getColumnHeaders();
		List<String> processedSystems = new ArrayList<String>();
		List<String> comparatorProcessedSystems = new ArrayList<String>();

		List<Map<String, List<String>>> systemYears = timeline.getTimeData();
		Map<String, Double> budgets = timeline.getBudgetMap();
		Object[] row = new Object[names.length];
		row[0] = this.roadmapName + " New Savings this year";
		Object[] rowBuildCost = new Object[names.length];
		rowBuildCost[0] = this.roadmapName + " Build Cost";
		Object[] rowSustainCost = new Object[names.length];
		rowSustainCost[0] = this.roadmapName + " Sustainment Cost";

		List<Map<String, List<String>>> comparatorYears = comparatorTimeline.getTimeData();
		Map<String, Double> comparatorBudgets = comparatorTimeline.getBudgetMap();
		Object[] comparatorRow = new Object[names.length];
		comparatorRow[0] = this.comparatorName + " New Savings this year";
		Object[] compRowBuildCost = new Object[names.length];
		compRowBuildCost[0] = this.comparatorName + " Build Cost";
		Object[] compRowSustainCost = new Object[names.length];
		compRowSustainCost[0] = this.comparatorName + " Sustainment Cost";

		NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
		NumberFormat percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setMinimumFractionDigits(1);

		List<Map<String, Double>> invest = timeline.getSystemInvestmentMap();
		List<Map<String, Double>> investComparator = comparatorTimeline.getSystemInvestmentMap();

		List<Map<String, Double>> sustainMap = timeline.getInterfaceSustainmentMap();
		List<Map<String, Double>> sustainComparatorMap = comparatorTimeline.getInterfaceSustainmentMap();

		for(int i = names.length-1; i>0; i--){
			int year = yearList.get(i-1);
			if(fyList.contains(year)){
				int yearIdx = timeline.getFyIndexFiscalYear(year);	
				double yearTotal = 0.0;

				Map<String, List<String>> yearMap = systemYears.get(yearIdx);
				for(String system: yearMap.keySet()){
					if(!processedSystems.contains(system)){
						yearTotal = yearTotal + budgets.get(system);
						processedSystems.add(system);
					}
				}
				String formattedTotal = formatter.format(yearTotal);
				if(year == fyList.get(0)){
					row[i] = formatter.format(0.0);
				}
				row[i+1] = formattedTotal;

				if(invest!=null){
					Map<String, Double> yearInvest = invest.get(yearIdx);
					double totalInvest = 0.;
					for(Double val : yearInvest.values()){
						totalInvest = totalInvest + val;
					}
					rowBuildCost[i] = formatter.format(totalInvest);
				}
				if(sustainMap!=null){
					Map<String, Double> yearSustain = sustainMap.get(yearIdx);
					double totalSustain = 0.;
					for(Double val : yearSustain.values()){
						totalSustain = totalSustain + val;
					}
					rowSustainCost[i] = formatter.format(totalSustain);
				}
			}else{				
				if(year>fyList.get(fyList.size()-1)){
					if(sustainMap!=null){
						Map<String, Double> yearSustain = sustainMap.get(sustainMap.size()-1);
						double totalSustain = 0.;
						for(Double val : yearSustain.values()){
							totalSustain = totalSustain + val;
						}
						rowSustainCost[i] = formatter.format(totalSustain);
					}
				}
			}
			if(comparatorFyList.contains(year)){
				int yearIdx = comparatorTimeline.getFyIndexFiscalYear(year);	
				double yearTotal = 0.0;

				Map<String, List<String>> yearMap = comparatorYears.get(yearIdx);
				for(String system: yearMap.keySet()){
					if(!comparatorProcessedSystems.contains(system)){
						yearTotal = yearTotal + comparatorBudgets.get(system);
						comparatorProcessedSystems.add(system);
					}
				}
				if(year == comparatorFyList.get(0)){
					comparatorRow[i] = formatter.format(0.0);
				}
				comparatorRow[i+1] = formatter.format(yearTotal);

				if(investComparator!=null){
					Map<String, Double> yearInvest = investComparator.get(yearIdx);
					double totalInvest = 0.;
					for(Double val : yearInvest.values()){
						totalInvest = totalInvest + val;
					}
					compRowBuildCost[i] = formatter.format(totalInvest);
				}
				if(sustainComparatorMap!=null){
					Map<String, Double> yearSustain = sustainComparatorMap.get(yearIdx);
					double totalSustain = 0.;
					for(Double val : yearSustain.values()){
						totalSustain = totalSustain + val;
					}
					compRowSustainCost[i] = formatter.format(totalSustain);
				}
			}else{				
				if(year>comparatorFyList.get(comparatorFyList.size()-1)){
					if(sustainComparatorMap!=null){
						Map<String, Double> yearSustain = sustainComparatorMap.get(sustainComparatorMap.size()-1);
						double totalSustain = 0.;
						for(Double val : yearSustain.values()){
							totalSustain = totalSustain + val;
						}
						compRowSustainCost[i] = formatter.format(totalSustain);
					}
				}
			}			
		}
		this.dataFrame.addRow(row, row);
		this.dataFrame.addRow(rowBuildCost, rowBuildCost);
		this.dataFrame.addRow(rowSustainCost, rowSustainCost);

		additionalRowBuilder(fyList, row, rowSustainCost, rowBuildCost, names, formatter, percentFormat, this.roadmapName);

		this.dataFrame.addRow(comparatorRow, comparatorRow);
		this.dataFrame.addRow(compRowBuildCost, compRowBuildCost);
		this.dataFrame.addRow(compRowSustainCost, compRowSustainCost);

		additionalRowBuilder(comparatorFyList, comparatorRow, compRowSustainCost, compRowBuildCost, names, formatter, percentFormat, this.comparatorName);
	}

	private void additionalRowBuilder(List<Integer> yearList, Object[] baseRow, Object[] sustainRow, Object[] buildCostRow, String[] names, NumberFormat formatter, NumberFormat percentFormat, String name){

		double cumulativeCost=0.0;
		double cumulativeSavings=0.0;

		List<Double> annualSavings = new ArrayList<Double>();

		Object[] cumulativeSavingsRow = new Object[names.length];
		cumulativeSavingsRow[0] = name + " Cumulative Savings";
		Object[] savingsRow = new Object[names.length];
		savingsRow[0] = name + " Previous Decommissioning Savings";
		Object[] cumulativeTotalCostRow = new Object[names.length];
		cumulativeTotalCostRow[0] = name + " Cumulative Cost";
		Object[] roiRow = new Object[names.length];
		roiRow[0] = name + " ROI";

		for(int i=1; i<names.length; i++){

			double year = Double.parseDouble(names[i].toString());
			if(year<yearList.get(0)){
				continue;
			}

			//cumulative savings
			if(baseRow[i] != null){
				String savings = baseRow[i].toString().replace("$", "").replace(",", "");
				annualSavings.add(Double.parseDouble(savings));				
			}
			for(Double value: annualSavings){
				cumulativeSavings = cumulativeSavings + value;
			}
			cumulativeSavingsRow[i] = formatter.format(cumulativeSavings);							

			if(cumulativeSavingsRow[i-1] != null && cumulativeSavingsRow[i-1].toString().contains("$") && baseRow[i] != null){
				savingsRow[i] = formatter.format(Double.parseDouble(cumulativeSavingsRow[i].toString().replace("$", "").replace(",", ""))
						-Double.parseDouble(cumulativeSavingsRow[i-1].toString().replace("$", "").replace(",", ""))
						-Double.parseDouble(baseRow[i].toString().replace("$", "").replace(",", "")));				
			}

			//row sustainment cost and cumulative total cost
			if(sustainRow[i] != null){
				String sustainmentCost = sustainRow[i].toString().replace("$", "").replace(",", "");
				cumulativeCost = cumulativeCost + Double.parseDouble(sustainmentCost);
			}
			if(buildCostRow[i] != null){
				String cost = buildCostRow[i].toString().replace("$", "").replace(",", "");
				cumulativeCost = cumulativeCost + Double.parseDouble(cost);
			}
			cumulativeTotalCostRow[i] = formatter.format(cumulativeCost);


			//ROI value for each year
			double netSavings = 0.0;
			double investment = 0.0;
			if(cumulativeTotalCostRow[i] != null){
				String cost = cumulativeTotalCostRow[i].toString().replace("$", "").replace(",", "");
				investment = Double.parseDouble(cost);
			}
			if(cumulativeSavingsRow[i] != null){
				String savings = cumulativeSavingsRow[i].toString().replace("$", "").replace(",", "");
				netSavings = Double.parseDouble(savings) - investment; 
			}
			if(investment != 0){
				double roi = netSavings/investment;
				roiRow[i] = percentFormat.format(roi);
			}else{
				roiRow[i] = percentFormat.format(0);
			}
		}
		this.dataFrame.addRow(savingsRow, savingsRow);
		this.dataFrame.addRow(cumulativeSavingsRow, cumulativeSavingsRow);
		this.dataFrame.addRow(cumulativeTotalCostRow, cumulativeTotalCostRow);
		this.dataFrame.addRow(roiRow, roiRow);
	}
}
