package prerna.nameserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import prerna.algorithm.nlp.PartOfSpeechHelper;
import prerna.util.Utility;
import rita.RiWordNet;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TypedDependency;

public class WordnetComparison {

	private static final Logger LOGGER = LogManager.getLogger(WordnetComparison.class.getName());
	
	private RiWordNet wordnet;
	private LexicalizedParser lp;

	private double mainNounWeight = 0.8;
	private double otherNounWeight = 0.2;
	
	private double cutOffSimScore = 0.20;
	
	/**
	 * Constructor for the class
	 * Defines the wordnet library
	 */
	public WordnetComparison(String wordNetDir, String lpDir) {
		wordnet = new RiWordNet(wordNetDir, false, true); // params: wordnetInstallDir, ignoreCompoundWords, ignoreUppercaseWords
		lp = LexicalizedParser.loadModel(lpDir);
		lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
	}

	public boolean isSimilar(Set<String> firstNounList, Set<String> secondNounList) {
		double comparissonVal = compareKeywords(firstNounList, secondNounList);
		return isSimilar(comparissonVal);
	}
	
	public boolean isSimilar(double comparissonVal) {
		if(comparissonVal <= cutOffSimScore) {
			return true;
		}
		return false;
	}
	
	public double compareKeywords(Set<String> firstNounList, Set<String> secondNounList) {
		System.err.println(">>>>>>>>>>>>>>>>>COMPARING: " + firstNounList + " to " + secondNounList);
		// need to iterate through and break apart the URIs
		firstNounList = getInstanceForSet(firstNounList);
		secondNounList = getInstanceForSet(secondNounList);
		System.err.println(">>>>>>>>>>>>>>>>> " + firstNounList);
		System.err.println(">>>>>>>>>>>>>>>>> " + secondNounList);
		
		Set<String> firstMainNouns = getMainNouns(firstNounList);
		Set<String> secondMainNouns = getMainNouns(secondNounList);
		System.err.println(">>>>>>>>>>>>>>>>>FOUND MAIN NOUNS IN SET 1: " + firstMainNouns);
		System.err.println(">>>>>>>>>>>>>>>>>FOUND MAIN NOUNS IN SET 2: " + secondMainNouns);

		Set<String> firstOtherNouns = new HashSet<String>();
		Set<String> secondOtherNouns = new HashSet<String>();

		Iterator<String> iterator = firstNounList.iterator();
		while(iterator.hasNext()) {
			String val = iterator.next();
			if(!firstMainNouns.contains(val)) {
				firstOtherNouns.add(val);
			}
		}
		
		iterator = secondNounList.iterator();
		while(iterator.hasNext()) {
			String val = iterator.next();
			if(!secondMainNouns.contains(val)) {
				secondOtherNouns.add(val);
			}
		}
		
		double[][] mainComparisonMatrix = getComparisonMatrix(firstMainNouns, secondMainNouns);
		double[][] otherComparisonMatrix = getComparisonMatrix(firstOtherNouns, secondOtherNouns);

		double mainCompVal = calculateBestComparison(mainComparisonMatrix) * mainNounWeight;
		double otherCompVal = calculateBestComparison(otherComparisonMatrix) * otherNounWeight;
		
		System.err.println(">>>>>>>>>>>>>>>>>COMPARING: " + firstMainNouns + " to " + secondMainNouns + " gives value = " + mainCompVal);
		System.err.println(">>>>>>>>>>>>>>>>>COMPARING: " + firstOtherNouns + " to " + secondOtherNouns + " gives value = " + otherCompVal);

		return mainCompVal + otherCompVal;
	}
	
	private double calculateBestComparison(double[][] comparisonMatrix) {
		int size = comparisonMatrix.length;
		
		if(size == 0) {
			return 0;
		}
		
		double bestCombination = 1.1;
		
		int counter = 0;
		for(; counter < size; counter++) {
			int i = counter;
			double newCombination = 0;
			Set<Integer> usedIndices = new HashSet<Integer>();
			for(;i < size; i++) {
				double currComp = 1.1;
				int indexToRemove = 0;
				int j = 0;
				for(; j < size; j++) {
					double newComp = comparisonMatrix[i][j];
					if(!usedIndices.contains(j) && newComp < currComp) {
						currComp = newComp;
						indexToRemove = j;
					} 
				}
				usedIndices.add(indexToRemove);
				newCombination += currComp;
			}
			if(bestCombination > newCombination) {
				bestCombination = newCombination;
			}
		}
		
		return bestCombination;
	}
	
	private double[][] getComparisonMatrix(Set<String> nounList1, Set<String> nounList2) {
		
		int sizeNounList1 = nounList1.size();
		int sizeNounList2 = nounList2.size();
		int maxSize = Math.max(sizeNounList1, sizeNounList2);
		double[][] comparisonMatrix = new double[maxSize][maxSize];
		
		int i = 0;
		Iterator<String> iterator1 = nounList1.iterator();
		for(; i < maxSize; i++) {
			int j = 0;
			Iterator<String> iterator2 = nounList2.iterator();
			for(; j < maxSize; j++) {
				if(iterator1.hasNext()) {
					String noun1 = iterator1.next();
					if(iterator2.hasNext()) {
						String noun2 = iterator2.next();
						comparisonMatrix[i][j] = wordnet.getDistance(noun1.toLowerCase(), noun2.toLowerCase(), "n");
					} else {
						comparisonMatrix[i][j] = 0.5;
					}
				} else {
					if(iterator2.hasNext()) {
						iterator2.next();
						comparisonMatrix[i][j] = 0.5;
					} else {
						comparisonMatrix[i][j] = 0.5;
					}
				}
			}
		}
		
		return comparisonMatrix;
	}

	private Set<String> getMainNouns(Set<String> nounList) {
		if(nounList.size() == 1) {
			return nounList;
		}
		
		String sentence = "";
		Iterator<String> nounIt = nounList.iterator();
		while(nounIt.hasNext()) {
			sentence = sentence.concat(nounIt.next()).concat(" ");
		}
		sentence = sentence.trim();
		
		List<TypedDependency> tdl = new ArrayList<TypedDependency>();
		PartOfSpeechHelper.createDepList(lp, sentence, tdl, new ArrayList<TaggedWord>()); //create dependencies
		Hashtable<GrammaticalRelation, Vector<TypedDependency>> nodeHash = new Hashtable<GrammaticalRelation, Vector<TypedDependency>>();
		PartOfSpeechHelper.setTypeDependencyHash(tdl, nodeHash);

		Vector<TypedDependency> tdlArr = nodeHash.get(EnglishGrammaticalRelations.NOUN_COMPOUND_MODIFIER);
		if(tdlArr != null) {
			return getTopGov(tdlArr);
		}
		
		// if NLP determines something isn't a noun, look for adj/adv
		tdlArr = nodeHash.get(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER);
		if(tdlArr != null) {
			return getTopGov(tdlArr);
		}
		
		tdlArr = nodeHash.get(EnglishGrammaticalRelations.ADVERBIAL_MODIFIER);
		if(tdlArr != null) {
			return getTopGov(tdlArr);
		}
		
		tdlArr = nodeHash.get(EnglishGrammaticalRelations.NP_ADVERBIAL_MODIFIER);
		if(tdlArr != null) {
			return getTopDep(tdlArr);
		}
		
		tdlArr = nodeHash.get(EnglishGrammaticalRelations.NOMINAL_SUBJECT);
		if(tdlArr != null) {
			return getTopDep(tdlArr);
		}
		
		tdlArr = nodeHash.get(EnglishGrammaticalRelations.DIRECT_OBJECT);
		if(tdlArr != null) {
			return getTopDep(tdlArr);
		}
		
		tdlArr = nodeHash.get(EnglishGrammaticalRelations.NUMERIC_MODIFIER);
		if(tdlArr != null) {
			return getTopGov(tdlArr);
		}
		
		// if cannot determine what type of grammatical relationship, just grab the dependent and get the gov
		tdlArr = nodeHash.get(GrammaticalRelation.DEPENDENT);
		if(tdlArr != null) {
			return getTopDep(tdlArr);
		}
		
		return null;
	}
	
	private Set<String> getTopGov(Vector<TypedDependency> subV) {
		Set<String> govList = new HashSet<String>();
		
		int i = 0;
		int size = subV.size();
		
		for(; i < size; i++) {
			TypedDependency td = subV.get(i);
			govList.add(td.gov().value());
		}
		
		return govList;
	}
	
	private Set<String> getTopDep(Vector<TypedDependency> subV) {
		Set<String> govList = new HashSet<String>();
		
		int i = 0;
		int size = subV.size();
		
		for(; i < size; i++) {
			TypedDependency td = subV.get(i);
			govList.add(td.dep().value());
		}
		
		return govList;
	}
	
	private Set<String> getInstanceForSet(Set<String> uriList) {
		Set<String> retSet = new HashSet<String>();
		Iterator<String> it = uriList.iterator();
		while(it.hasNext()) {
			String uri = it.next();
			String instance = Utility.getInstanceName(uri);
			retSet.add(instance);
		}
		
		return retSet;
	}
	
}
