package prerna.algorithm.learning.moa;

import gov.sandia.cognition.learning.algorithm.perceptron.KernelizableBinaryCategorizerOnlineLearner;
import gov.sandia.cognition.learning.algorithm.perceptron.OnlinePerceptron;
import gov.sandia.cognition.learning.algorithm.perceptron.kernel.KernelBinaryCategorizerOnlineLearnerAdapter;
import gov.sandia.cognition.learning.data.DefaultInputOutputPair;
import gov.sandia.cognition.learning.data.InputOutputPair;
import gov.sandia.cognition.learning.function.categorization.DefaultKernelBinaryCategorizer;
import gov.sandia.cognition.learning.function.categorization.LinearBinaryCategorizer;
import gov.sandia.cognition.learning.function.kernel.Kernel;
import gov.sandia.cognition.learning.function.kernel.LinearKernel;
import gov.sandia.cognition.learning.function.kernel.PolynomialKernel;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.VectorFactory;
import gov.sandia.cognition.math.matrix.mtj.DenseVector;
import gov.sandia.cognition.math.matrix.mtj.Vector2;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import moa.classifiers.Classifier;
import moa.classifiers.functions.Perceptron;
import moa.core.InstancesHeader;
import moa.options.FloatOption;
import moa.streams.ArffFileStream;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import prerna.algorithm.api.IAnalyticRoutine;
import prerna.algorithm.api.ITableDataFrame;
import prerna.algorithm.learning.weka.WekaUtilityMethods;
import prerna.ds.BTreeDataFrame;
import prerna.ds.BTreeIterator;
import prerna.ds.ValueTreeColumnIterator;
import prerna.error.FileReaderException;
import prerna.math.BarChart;
import prerna.om.SEMOSSParam;
import prerna.util.Utility;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

public class MOAPerceptronRunner implements IAnalyticRoutine {
	protected List<SEMOSSParam> options;
	private Perceptron learner;
	private String className;
	private double[][] weights;
	private double accuracy;
	private int classIndex;
	//private double[][] weightAttribute;
	//private FloatOption learningRatioOption = new FloatOption("learningRatio", 'r', "Learning ratio", 1);

	public MOAPerceptronRunner() {
		learner = new MOAPerceptron();

		this.options = new ArrayList<SEMOSSParam>();

		SEMOSSParam p1 = new SEMOSSParam();
		p1.setName("className");
		options.add(0, p1);

		SEMOSSParam p2 = new SEMOSSParam();
		p2.setName("skipAttributes");
		options.add(1, p2);
		
		SEMOSSParam p3 = new SEMOSSParam();
		p3.setName("KernelType");
		options.add(2, p3);

		SEMOSSParam p4 = new SEMOSSParam();
		p4.setName("degree");
		options.add(3, p4);
		//add weights option
	}

	public double[][] getWeights() {
		return ((MOAPerceptron)learner).getWeights();
	}

	@Override
	public String getName() {
		return "MOA Perceptron";
	}

	@Override
	public String getResultDescription() {
		return null;
	}


	@Override
	public void setSelectedOptions(Map<String, Object> selected) {
		Set<String> keySet = selected.keySet();
		for(String key : keySet) {
			for(SEMOSSParam param : options) {
				if(param.getName().equals(key)){
					param.setSelected(selected.get(key));
					break;
				}
			}
		}
	}


	@Override
	public List<SEMOSSParam> getOptions() {
		return options;
	}

	@Override
	public ITableDataFrame runAlgorithm(ITableDataFrame... data) {
		className = (String)this.options.get(0).getSelected();
		String kernelType = (String)this.options.get(2).getSelected();
		Integer degree = (Integer)this.options.get(3).getSelected();
		//Double constant = (Double)this.options.get(4).getSelected();
		
		ITableDataFrame table = data[0];
		ArrayList<String> skip = new ArrayList<>();
		skip.add(table.getColumnHeaders()[0]);
		table.setColumnsToSkip(skip);
		int numAttributes = table.getNumCols();
		String[] names = table.getColumnHeaders();

		List<Object[]> dataTable = table.getData();
		Collections.shuffle(dataTable);
		
		
		for(classIndex = 0; classIndex < names.length; classIndex++) {
			if(names[classIndex].equals(className)) {
				break;
			}
		}

		boolean[] isCategorical = table.isNumeric();
		for(int i = 0; i < isCategorical.length; i++) {
			isCategorical[i] = !isCategorical[i]; 
		}

		Instances instanceData = WekaUtilityMethods.createInstancesFromQuery("DataSet", dataTable, names, classIndex);
		instanceData.setClassIndex(classIndex);
	
		String[] correctArray;
		if(kernelType.equalsIgnoreCase("MOA Linear")) {
			correctArray = this.runMOALinearPerceptron(dataTable, instanceData, isCategorical, numAttributes);
		} else {
			correctArray = this.runPolynomialKernelPerceptron(dataTable, instanceData, isCategorical, numAttributes, degree, 1.0);
		}
		
		String columnName = "Correctly_Classified -- "+(int)accuracy+"%";
		String[] newNames = {names[0], columnName};
		ITableDataFrame newTable = new BTreeDataFrame(newNames);
		for(int i = 0; i < dataTable.size(); i++) {
			Map<String, Object> nextRow = new HashMap<>();
			nextRow.put(newNames[0], dataTable.get(i)[0]);
			nextRow.put(columnName, correctArray[i]);
			newTable.addRow(nextRow, nextRow);
		}
		
		return newTable;
	}

	private void trainClassifier(ITableDataFrame trainingSet, int classIndex) {
		boolean[] isCategorical = trainingSet.isNumeric();

		for(int i = 0; i < isCategorical.length; i++) {
			isCategorical[i] = !isCategorical[i];
		}

		Instances instanceData = WekaUtilityMethods.createInstancesFromQuery("TrainingSet", (ArrayList<Object[]>)trainingSet.getScaledData(), trainingSet.getColumnHeaders(), classIndex);
		instanceData.setClassIndex(classIndex);

		InstancesHeader header = new InstancesHeader(instanceData);
		learner.setModelContext(header);
		learner.prepareForUse();

		Iterator<Object[]> iterator = trainingSet.scaledIterator(false);
		while(iterator.hasNext()) {
			Object[] row = iterator.next();
			Instance trainInst = WekaUtilityMethods.createInstance(instanceData, row, isCategorical, trainingSet.getColumnHeaders().length - 1);
			System.out.println(trainInst.toDoubleArray());
			System.out.println(row[classIndex]);
			learner.trainOnInstanceImpl(trainInst);
		}
	}

	private String[] runMOALinearPerceptron(List<Object[]> dataTable, Instances instanceData, boolean[] isCategorical, int numAttributes) {
		//make a header
		InstancesHeader header = new InstancesHeader(instanceData);
		learner.setModelContext(header);
		learner.prepareForUse();

		String[] correctArray = new String[dataTable.size()];
		
		int correct = 0;
		int total = dataTable.size();

		for(int i = 0; i < dataTable.size(); i++) {

			Object[] newRow = dataTable.get(i);
			Instance nextInst = WekaUtilityMethods.createInstance(instanceData, newRow, isCategorical, numAttributes);	
			
			Boolean correctlyClassified = learner.correctlyClassifies(nextInst);
			if(correctlyClassified) {
				correctArray[i] = correctlyClassified.toString();
				correct++;
			} else {
				int index = Utils.maxIndex(learner.getVotesForInstance(nextInst));
				String className = learner.getClassLabelString(index);
				className = className.substring(1, className.length()-1);
				int beginIndex = className.indexOf(":");
				className = className.substring(beginIndex+1);
				correctArray[i] = className;
			}

			learner.trainOnInstance(nextInst);
		}
		accuracy = 100.0*(double)correct/(double)total;
		
		return correctArray;
	}
    private String[] runPolynomialKernelPerceptron(List<Object[]> allData, Instances instanceData, boolean[] isCategorical, int numAttributes, int degree, double constant) {
    	
        KernelBinaryCategorizerOnlineLearnerAdapter<Vector> instance = new KernelBinaryCategorizerOnlineLearnerAdapter<Vector>(new PolynomialKernel(degree, constant), new OnlinePerceptron());
        DefaultKernelBinaryCategorizer<Vector> learned = instance.createInitialLearnedObject();    
        int correctCount = 0;
        String[] correctArray = new String[allData.size()];
        
        String binaryClassifier = instanceData.attribute(classIndex).value(0);
		for(int z = 0; z < allData.size(); z++) {
			
			Object[] newRow = allData.get(z);
			
			Instance nextInst = WekaUtilityMethods.createInstance(instanceData, newRow, isCategorical, numAttributes);
	    	double[] array = nextInst.toDoubleArray();
	    	double[] array2 = new double[array.length-1];
	    	int b = 0;
	    	for(int a = 0; a < array.length; a++) {
	    		if(a!=3) {
	    			array2[b] = array[a];
	    			b++;
	    		}
	    	}
	
	    	Vector v = VectorFactory.getDenseDefault().copyArray(array2);
	    	String attribute = newRow[classIndex].toString();
	    	boolean bool = attribute.equalsIgnoreCase(binaryClassifier);
	    	
	    	InputOutputPair<Vector, Boolean> example = DefaultInputOutputPair.create(v, bool);
	        	
	        boolean actual = example.getOutput();
	        Boolean predicted = learned.evaluate(example.getInput());
	        Boolean result = actual==predicted;
	        if (result) {
	            correctCount++;
	        }
	        correctArray[z] = result.toString();//(actual==predicted).toString();
	        this.applyUpdate(instance, learned, example);
	        
		}
		accuracy = (double) correctCount / (allData.size())*100;
		return correctArray;
    }
	
    private void applyUpdate(final KernelizableBinaryCategorizerOnlineLearner learner, final LinearBinaryCategorizer target, final InputOutputPair<Vector, Boolean> example) {
    	learner.update(target, example);
    }

    private void applyUpdate(final KernelBinaryCategorizerOnlineLearnerAdapter<Vector> learner, final DefaultKernelBinaryCategorizer<Vector> target, final InputOutputPair<Vector, Boolean> example) {
    	learner.update(target, example);
    }
	@Override
	public String getDefaultViz() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getChangedColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getResultMetadata() {
		// TODO Auto-generated method stub
		return null;
	}
}
