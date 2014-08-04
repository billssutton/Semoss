/*******************************************************************************
 * Copyright 2013 SEMOSS.ORG
 * 
 * This file is part of SEMOSS.
 * 
 * SEMOSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SEMOSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SEMOSS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package prerna.ui.components.specific.tap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JList;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import prerna.algorithm.api.IAlgorithm;
import prerna.rdf.engine.api.IEngine;
import prerna.rdf.engine.impl.SesameJenaSelectStatement;
import prerna.rdf.engine.impl.SesameJenaSelectWrapper;
import prerna.ui.components.UpdateProcessor;
import prerna.ui.components.api.IPlaySheet;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

/**
 * Calculates necessary values for TM Vendor calculation.
 */
public class TMVendorCalculationPerformer implements IAlgorithm {

	Hashtable<String, Object> TMhash = new Hashtable<String, Object>();
	Hashtable<String, Hashtable> combined = new Hashtable<String, Hashtable>();
	Hashtable<String, Double> fulfillment = new Hashtable<String,Double>();
	Logger logger = Logger.getLogger(getClass());

	/**
	 * Constructor for TMVendorCalculationPerformer.
	 */
	public TMVendorCalculationPerformer() {
		
	}
	
	/**
	 * Updates DB with TM Values.
	 * 
	 * @return Hashtable	TM Values
	 */
	public Hashtable tmCalculate() {
		if (DIHelper.getInstance().getLocalProp(Constants.TECH_MATURITY)!=null) 
			TMhash = (Hashtable<String, Object>) DIHelper.getInstance().getLocalProp(Constants.TECH_MATURITY);
		logger.info("Graph PlaySheet Beginning TECH MATURITY Calculation");

		try {
			calculateTechMaturity();
			DIHelper.getInstance().setLocalProperty(Constants.TECH_MATURITY, TMhash);
			
		} catch (RuntimeException e) {
			// TODO: Specify exception
			e.printStackTrace();
		}
		if(TMhash.size()>0)
		{
//			ArrayList<Double> techStdValues = (ArrayList<Double>) TMhash.get(Constants.TECH_MATURITY+Constants.CALC_MATRIX_TECH_STD);
			ArrayList<Double> extStabValues = (ArrayList<Double>) TMhash.get(Constants.TECH_MATURITY+Constants.CALC_MATRIX_EXT_STAB);
			ArrayList<String> technicalNames = (ArrayList<String>) TMhash.get(Constants.TECH_MATURITY+Constants.CALC_NAMES_LIST);
			
			String extStabQuery = prepareInsert(technicalNames, extStabValues, "ExternalStabilityTM");
	//		String techStdQuery = prepareInsert(technicalNames, techStdValues,"TechnicalStandardTM");
			
			UpdateProcessor pro = new UpdateProcessor();
	//		pro.setQuery(techStdQuery);
	//		pro.processQuery();
			pro = new UpdateProcessor();
			pro.setQuery(extStabQuery);
			pro.processQuery();
		}
		return TMhash;
	}

	/**
	 * Integrate system hardware into calculations.
	 */
	private void combineWithSysHard(){
		String[][] hardwareMatrix = (String[][]) TMhash.get("Vendor/Hardware"+Constants.TM_LIFECYCLE);
		ArrayList<String> hardwareCol = (ArrayList<String>) TMhash.get("Vendor/Hardware"+Constants.CALC_COLUMN_LABELS);
		ArrayList<String> hardwareRow = (ArrayList<String>) TMhash.get("Vendor/Hardware"+Constants.CALC_ROW_LABELS);
		for (int row = 0; row<hardwareRow.size();row++){
			String sys = hardwareRow.get(row);
			double sum = 0;
			int count = 0;
			for (int col =0; col<hardwareCol.size(); col++){
				if(hardwareMatrix[row][col] instanceof String){
					String lifecycle = getLifeCycle(hardwareMatrix[row][col]);
					double additional = textToNum(lifecycle)*textToNum("Infrastructure");
					if(additional>0){
						sum=sum+additional;
						count = count +1;
					}
				}
			}
			if (count>0){
				Hashtable sums;
				Hashtable counts;
				if (combined.containsKey(sys)){
					sums = combined.get(sys+"/sums");
					counts = combined.get(sys+"/counts");
					if(sums.containsKey("Infrastructure")){
						double newSum = sum + (Double)sums.get("Infrastructure");
						int newCount = count + (Integer)counts.get("Infrastructure");
						sums.put("Infrastructure", newSum);
						counts.put("Infrastructure", newCount);
					}
					else{
						sums.put("Infrastructure", sum);
						counts.put("Infrastructure", count);
					}
				}
				else {
					sums = new Hashtable();
					counts = new Hashtable();
					sums.put("Infrastructure", sum);
					counts.put("Infrastructure", count);
				}
				combined.put(sys+"/counts", counts);
				combined.put(sys, new Hashtable());
				combined.put(sys+"/sums", sums);
			}
		}
	}
	
	/**
	 * Integrate system software into calculations.
	 */
	private void prepareSysSoft(){
		String[][] softwareMatrix = (String[][]) TMhash.get("Vendor/Software"+Constants.TM_LIFECYCLE);
		String[][] softwareCatMatrix = (String[][]) TMhash.get("Vendor/Software"+Constants.TM_CATEGORY);
		ArrayList<String> softwareCol = (ArrayList<String>) TMhash.get("Vendor/Software"+Constants.CALC_COLUMN_LABELS);
		ArrayList<String> softwareRow = (ArrayList<String>) TMhash.get("Vendor/Software"+Constants.CALC_ROW_LABELS);
		for (int row = 0; row<softwareRow.size();row++){
			String sysName = softwareRow.get(row);
			Hashtable<String, Integer> counts = new Hashtable<String, Integer>();
			Hashtable<String, Double> sums = new Hashtable<String, Double>();
			boolean addToCombined = false;
			for (int col =0; col<softwareCol.size(); col++){
				if(softwareMatrix[row][col] instanceof String && softwareCatMatrix[row][col] instanceof String){
					String lifecycle = getLifeCycle(softwareMatrix[row][col]);
					double additional = textToNum(lifecycle)*textToNum(softwareCatMatrix[row][col]);
					int count=1;
					if(additional>0)
					{
						double sum;
						if(counts.containsKey(softwareCatMatrix[row][col])){
							sum=additional + sums.get(softwareCatMatrix[row][col]);
							count = 1 + counts.get(softwareCatMatrix[row][col]);
						}
						else{
							sum = additional;
							count = 1;
							addToCombined = true;
						}
						sums.put(softwareCatMatrix[row][col], sum);
						counts.put(softwareCatMatrix[row][col], count);
					}
				}
			}
			if(addToCombined){
				combined.put(sysName+"/counts", counts);
				combined.put(sysName, new Hashtable());
				combined.put(sysName+"/sums", sums);
			}
		}
	}
	
	/**
	 * Returns lifecycle value based on end date vs. current date.
	 * 
	 * @param date String	End of support date
	 * @return String		Current lifecycle value
	 */
	private String getLifeCycle(String date){
		if(date.contains("TBD"))
			return "Date_TBD";
		date=date.substring(1,date.substring(1).indexOf("T"));
		int year=Integer.parseInt(date.substring(0,date.indexOf("-")));
		int month=Integer.parseInt(date.substring(date.indexOf("-")+1,date.lastIndexOf("-")));
		
		Calendar now = Calendar.getInstance();
		int currYear = now.get(Calendar.YEAR);
		int currMonth = now.get(Calendar.MONTH);
		String lifeCycleType = "Supported";
		if((year<currYear)||(year==currYear && month<=currMonth+6)||(year==currYear+1&&month<=currMonth+6-12))
			lifeCycleType="Retired_(Not_Supported)";
		else if(year<=currYear||(year==currYear+1&&month<=currMonth))
			lifeCycleType="Sunset_(End_of_Life)";
		else if(year<=currYear+2||(year==currYear+3&&month<=currMonth))
			lifeCycleType="Generally_Available";
		return lifeCycleType;
	}
	
//	/**
//	 * Populates fulfillment values based on vendor/tech standards.
//	 */
//	private void prepareTechStandardFulfillment()
//	{
//		String query = "SELECT DISTINCT ?Vendor ?TechStandard ?FulfillLevel WHERE {{?Techconcat <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/VendorSystemCapabilityTaskTechRequirement>;}{?AssociatedTo <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/AssociatedTo> ;}{?concat <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/VendorSystemCapabilityTask>;}{?Techconcat ?AssociatedTo ?concat;}{?Vendor <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/Vendor> ;}{?HasVendor <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Has> ;}{?concat ?HasVendor ?Vendor;}{?TechStandard <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/TechStandard> ;}{?SatisfiesTechStandard <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Satisfies> ;}{?Techconcat ?SatisfiesTechStandard ?TechStandard;}{?FulfillLevel <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semoss.org/ontologies/Concept/SupportLevel> ;}{?Supports <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> <http://semoss.org/ontologies/Relation/Supports> ;}{?Techconcat ?Supports ?FulfillLevel;}}";// ORDER BY ?Vendor ?TechStandard ?FulfillLevel";
//		SesameJenaSelectWrapper wrapper = new SesameJenaSelectWrapper();
//		JList list = (JList)DIHelper.getInstance().getLocalProp(Constants.REPO_LIST);
//		Object[] repos = (Object [])list.getSelectedValues();
//		IEngine engine = (IEngine)DIHelper.getInstance().getLocalProp(repos[0]+"");
//		logger.info("Repository is " + repos);
//		
//		Hashtable<String,Double> options = new Hashtable<String,Double>();
//		options.put("Supports_out_of_box", 10.0);
//		options.put("Supports_with_configuration", 7.0);
//		options.put("Supports_with_customization", 3.0);
//		options.put("Does_not_support", 0.0);
//		
//		wrapper.setQuery(query);
//		wrapper.setEngine(engine);
//		wrapper.executeQuery();
//		
//		Hashtable<String,Double> vendorNumOfTechStandards = new Hashtable<String,Double>();
//		
//		String[] names = wrapper.getVariables();
//		while(wrapper.hasNext()) {
//			// make a hashtable with system as the key mapped to number of tech standards for each
//			SesameJenaSelectStatement sjss = wrapper.BVnext();
//			String vendor = (String) Utility.getInstanceName(sjss.getVar(names[0])+"");
//			String standard = (String) Utility.getInstanceName(sjss.getVar(names[1])+""); 
//			String fulfill = (String)Utility.getInstanceName(sjss.getVar(names[2])+"");
//			Double value=options.get(fulfill);
//			Double count=1.0;
//			if (fulfillment.containsKey(vendor))
//			{
//				value+=fulfillment.get(vendor);
//				count+=vendorNumOfTechStandards.get(vendor);
//			}
//			fulfillment.put(vendor, value);
//			vendorNumOfTechStandards.put(vendor,count);
//		}
//		
//		for (String ven: fulfillment.keySet()) {
//			double score = fulfillment.get(ven)/(vendorNumOfTechStandards.get(ven));
//			fulfillment.put(ven, score);
//		}
//	}
	
	/**
	 * Performs technical maturity calculations, inserting them into TMhash.
	 */
	private void calculateTechMaturity(){
//		prepareTechStandardFulfillment();
		prepareSysSoft();
		combineWithSysHard();
		ArrayList<String> allSysNames = new ArrayList<String>();
		ArrayList<String> tempSysNames = new ArrayList<String>();
		ArrayList<Double> sysExtStabMat = new ArrayList<Double>();
		ArrayList<Double> sysTechStdMat = new ArrayList<Double>();
		
		Set<String> combinedKeyIt = combined.keySet();
		for(String fullKey: combinedKeyIt) 
			if(!fullKey.contains("/")) tempSysNames.add(fullKey);
		
		Iterator sysNameIt = tempSysNames.iterator();
		while(sysNameIt.hasNext()){
			String sys = (String) sysNameIt.next();
			Hashtable sums = combined.get(sys+"/sums");
			Hashtable counts = combined.get(sys + "/counts");
			//Iterate through every type of hard/soft in each system's counts and sums matrix
			Iterator sumsIt = sums.keySet().iterator();
			double techMatValue = 0;
			while(sumsIt.hasNext()){
				String type = (String) sumsIt.next();
				double finalTotal = (Double) sums.get(type);
				int finalCount = (Integer) counts.get(type);
				techMatValue = techMatValue + finalTotal/finalCount;
			}
			
			double techStdValue = 0;
			if(fulfillment.containsKey(sys)) {
				techStdValue = fulfillment.get(sys);
			}
			allSysNames.add(sys);
			sysExtStabMat.add(techMatValue);
			sysTechStdMat.add(techStdValue);
		}
		TMhash.put(Constants.TECH_MATURITY + Constants.CALC_MATRIX_EXT_STAB, sysExtStabMat);
//		TMhash.put(Constants.TECH_MATURITY + Constants.CALC_MATRIX_TECH_STD, sysTechStdMat);
		TMhash.put(Constants.TECH_MATURITY + Constants.CALC_NAMES_LIST, allSysNames);

	}
	
	//TODO: Delete unused method?
	/**
	 * Method testPrint.
	 * 
	 * @param key String
	 */
	public void testPrint(String key){
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(key +" testPrint.xls");
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet worksheet = workbook.createSheet("Data");
			ArrayList<Double> values = (ArrayList<Double>) TMhash.get(key+Constants.CALC_MATRIX);
			ArrayList<String> names = (ArrayList<String>) TMhash.get(key+Constants.CALC_NAMES_LIST);
			for (int row=0; row<names.size(); row++){
				HSSFRow row1 = worksheet.createRow((short) row);
				HSSFCell cell = row1.createCell(0);
				cell.setCellValue((String) names.get(row));
				HSSFCell cell1 = row1.createCell(1);
				cell1.setCellValue((double) values.get(row));
			}
		workbook.write(fileOut);
		fileOut.flush();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try{
				if(fileOut!=null)
					fileOut.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("Printed: " +key +" testPrint.xls");
	}	
	
	//TODO: Delete unused method?
	/**
	 * Method printMatrix.
	 * 
	 * @param keyPart1 String
	 * @param keyPart2 String
	 */
	public void printMatrix(String keyPart1, String keyPart2){
		//mprint(activitySystem);
		String key = keyPart1+"/"+keyPart2;
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(keyPart1+"-"+keyPart2 +"_Matrix.xls");
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet worksheet = workbook.createSheet("Data");
			double[][] matrix = (double[][]) TMhash.get(key+Constants.CALC_MATRIX);
			ArrayList<String> rowNames = (ArrayList<String>) TMhash.get(key+Constants.CALC_ROW_LABELS);
			ArrayList<String> colNames = (ArrayList<String>) TMhash.get(key+Constants.CALC_COLUMN_LABELS);
			HSSFRow row0 = worksheet.createRow((short) 0);
			for (int colTitle = 1; colTitle<colNames.size()+1; colTitle++){
				HSSFCell cell = row0.createCell(colTitle);
				cell.setCellValue((String) colNames.get(colTitle-1));
			}
			for (int row=1; row<rowNames.size()+1; row++){
				HSSFRow row1 = worksheet.createRow((short) row);
				HSSFCell cell = row1.createCell(0);
				cell.setCellValue((String) rowNames.get(row-1));
				for(int col = 1; col<colNames.size()+1; col++){
					HSSFCell cell1 = row1.createCell(col);
					cell1.setCellValue((double) matrix[row-1][col-1]);
				}
			}
		workbook.write(fileOut);
		fileOut.flush();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try{
				if(fileOut!=null)
					fileOut.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("Printed: " + keyPart1+"-"+keyPart2 +"_Matrix.xls");
	}	
	
	/**
	 * Returns appropriate numerical value based upon String value
	 * 
	 * @param s1 String		Text to be parsed
	 * @return double		Value representation of text
	 */
	public static double textToNum(String s1) {
		if(s1.contains("Infrastructure")) return .1;
		else if(s1.contains("Platform")) return .3;
		else if(s1.contains("Software")) return .6;
		else if(s1.contains("Application")) return 0;
		else if(s1.contains("Beta")) return 3;
		else if(s1.contains("Generally_Available")) return 7;
		else if(s1.contains("Retired_(Not_Supported)")) return 1;
		else if(s1.contains("Supported")) return 10;
		else if(s1.contains("Sunset_(End_of_Life)")) return 5;
		else return 0;
	  }

	
	/**
	 * Prepares insert query based on property triples
	 * 
	 * @param names ArrayList<String>	List of vendor system names
	 * @param values ArrayList<Double>	List of vendor-specific values
	 * @param propName String			Name of property
	 * 
	 * @return String	Query to be inserted
	 */
	private String prepareInsert(ArrayList<String> names, ArrayList<Double> values, String propName){
		String predUri = "<http://semoss.org/ontologies/Relation/Contains/"+propName+">";
		
		//add start with type triple
		String insertQuery = "INSERT DATA { " +predUri + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " +
				"<http://semoss.org/ontologies/Relation/Contains>. ";
		
		//add other type triple
		insertQuery = insertQuery + predUri +" <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " +
				"<http://www.w3.org/1999/02/22-rdf-syntax-ns#Property>. ";
		
		//add sub property triple -->>>>>>> should probably look into this.... very strange how other properties are set up
		insertQuery = insertQuery + predUri +" <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> " +
				predUri + ". ";
		
		for(int sysIdx = 0; sysIdx<names.size(); sysIdx++){
			String subjectUri = "<http://health.mil/ontologies/Concept/Vendor/"+names.get(sysIdx) +">";
			String objectUri = "\"" + values.get(sysIdx) + "\"" + "^^<http://www.w3.org/2001/XMLSchema#double>";

			insertQuery = insertQuery + subjectUri + " " + predUri + " " + objectUri + ". ";
		}
		insertQuery = insertQuery + "}";
		
		return insertQuery;
	}

	/**
	 * Inherited method.
	 * 
	 * @param graphPlaySheet IPlaySheet
	 */
	@Override
	public void setPlaySheet(IPlaySheet graphPlaySheet) {
		
	}

	/**
	 * Inherited method.
	 * 
	 * @return String[]
	 */
	@Override
	public String[] getVariables() {
		// TODO: Don't return null
		return null;
	}

	/**
	 * Inherited method.
	 */
	@Override
	public void execute() {
		
	}

	/**
	 * Inherited method.
	 * 
	 * @return String
	 */
	@Override
	public String getAlgoName() {
		// TODO: Don't return null
		return null;
	}
}
