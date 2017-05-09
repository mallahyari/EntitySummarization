/**
 * 
 */
package cs.uga.edu.topicmodel;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;
import virtuoso.jena.driver.VirtGraph;
import cs.uga.edu.dicgenerator.CategoryNameMapper;
import cs.uga.edu.dicgenerator.DbpediaProcessor;
import cs.uga.edu.dicgenerator.VirtuosoAccess;
import cs.uga.edu.properties.Configuration;
import cs.uga.edu.wikiaccess.WikipediaAccessLayer;

/**
 * @author Mehdi
 *
 */
public class OntLDA {

	
	private static final Logger logger = LogManager.getLogger(OntLDA.class.getName());
//	private static WikipediaAccessLayer wikiOntology = null;
	
	
	public static void main(String[] args) throws FileNotFoundException {
		
//		createCorpusConcepts();
//		removeIrrelevantFiles();
		prepareData();
		
//		runStandardLDA(20);
//		computeLabelScore();
		
		
		
//		runOntoLdaModel1();
		
//		fitlerFiles();
		

		
		/*
//		topicModel.initializeTopicsAndCocepts();
//		topicModel.checkAvailabilityOfWords();
		/// Run it only once ///
//		topicModel.createWikiConceptVocabulary();
//		topicModel.importDocuments();
//		topicModel.getIntersectionVocabulary();
		
//		InstanceList documents = topicModel.loadDocuments();
		topicModel.initializeTopicsAndCocepts();
//		
		try {
			topicModel.runGibbsSampling();
		}catch (Exception e) {
			logger.catching(Level.ERROR, e);
			e.printStackTrace();
		}
		
		*/
//		calculateProbabilities();
		
	}


	

	/*************************************************
	 * 
	 */
	public static void runStandardLDA(int numberOfTopics) {
		ParallelTopicModel lda = new ParallelTopicModel(numberOfTopics);
		ImportData importer = new ImportData();
		InstanceList documents = importer.readDirectory(new File("/home/mehdi/inputfiles/"));
		lda.addInstances(documents);
		int numOfTopWords = 20;
		Object [][] topicWord = lda.getTopWords(numOfTopWords);
		for (int i = 0; i < numberOfTopics; i++) {
			System.out.println("Topic " + (i+1));
			for (int j = 0; j < numOfTopWords; j++) {
				System.out.print(topicWord[i][j] + " ");
			} // end of for (j)
			System.out.println();
		} // end of for (i)
	}


	/*************************************************
	 * 
	 */
	public static void computeLabelScore() {
		System.out.println("Indexing documents...");
		Indexer indexer = indexDocuments();
//		Directory index = indexer.getIndex(); 
//		StandardAnalyzer analyzer = indexer.getAnalyzer();
//		String kw1 = "lawmakers";
//		String kw2 = "obama administration";
//		indexer.retrieveDocumentsCountByMultipleKeywords(index, analyzer, kw1,kw2);
		
		// create topic-word matrix
		System.out.print("Creating topic-word matrix...");
		Map<Integer, Map<String, Double>> topicWordMatrix = new HashMap<Integer, Map<String, Double>>();
		System.out.println("Done.");
		// Map to keep the normalization constant for each topic
		Map<Integer, Double> normalizationConstant = new HashMap<Integer, Double>();
		String fileName = "/home/mehdi/wtweights-1414.txt";
//		String fileName = "/home/mehdi/wtweights-683.txt";
//		String fileName = "/home/mehdi/wtweights-400.txt";
//		String fileName = "/Users/Mehdi/wtweights-683.txt";
		System.out.print("Creating topic-word Probability matrix...");
		createTopicWordProbabilityMatrix(fileName, topicWordMatrix, normalizationConstant);
		System.out.println("Done.");
		Map<Integer, List<String>> topicTopWords = new HashMap<Integer, List<String>>();
		fileName = "/home/mehdi/1414_keys.txt";
//		fileName = "/home/mehdi/683_keys.txt";
//		fileName = "/home/mehdi/400_keys.txt";
//		fileName = "/Users/Mehdi/683_keys.txt";
		System.out.print("Reading topic topwords file...");
		readTopicTopWords(fileName, topicTopWords);
		System.out.println("Done.");
		fileName = "/home/mehdi/1414-labels.txt";
//		fileName = "/home/mehdi/683-labels.txt";
//		fileName = "/home/mehdi/400-labels.txt";
//		fileName = "/Users/Mehdi/683-labels.txt";
		List<String> topLabels = new ArrayList<String>();
		System.out.print("Reading label file...");
		readTopLabels(fileName, topLabels);
		System.out.println("Done.");
//		System.out.println(topLabels.size());
//		System.out.println(topLabels);
		System.out.println("Computing the Expectation of PMI for each label...");
		List<String> topicLabels = computeExpectationOfPmiForLabels(topicWordMatrix,normalizationConstant,topicTopWords,topLabels, indexer);
		System.out.println("Done.");
		System.out.println(topicLabels);
//		String searchString = "s economy";
//		indexer.retrieveDocumentsByKeyword(index, analyzer, searchString);
		
	} // end of computeLabelScore
	
	/*************************************************
	 * @param topicWordMatrix
	 * @param normalizationConstant
	 * @param topicTopWords
	 * @param topLabels
	 * @param indexer
	 */
	public static List<String> computeExpectationOfPmiForLabels(Map<Integer, Map<String, Double>> topicWordMatrix, Map<Integer, Double> normalizationConstant, Map<Integer, List<String>> topicTopWords, List<String> labels, Indexer indexer) {
		Directory index = indexer.getIndex();
		StandardAnalyzer analyzer = indexer.getAnalyzer();
		List<String> topicsLabels = new ArrayList<String>();
		int numOfTopics = topicWordMatrix.size();
		int numOfLabels = labels.size();
		double numOfDocuments = 1414;
		for (int k = 0; k < numOfTopics; k++) {
			List<Double> scoreList = new ArrayList<Double>(numOfLabels);
			for (int l = 0; l < numOfLabels; l++) {
				double score = 0;
				String label = labels.get(l);
				List<String> topWords = topicTopWords.get(k);
				for (String word : topWords) {
					// P(w|t) probability of word w given topic t
					double pr_wt = topicWordMatrix.get(k).get(word) / normalizationConstant.get(k);
					// P(w|C) probability of word w given the document collection C
					double pr_wd = indexer.retrieveDocumentsCountByKeyword(index, analyzer, word) / numOfDocuments;
					// P(l|C) probability of label l give the document collection C
					double pr_ld = indexer.retrieveDocumentsCountByKeyword(index, analyzer, label) / numOfDocuments;
					// P(w,l|C) probability of word w and label l give the document collection C
					double pr_wld = indexer.retrieveDocumentsCountByMultipleKeywords(index, analyzer, word, label) / numOfDocuments;
					// sum(P(w|t) * (P(w,l|C) / (P(w|C) * P(l|C)))
					if (pr_wt * pr_wld != 0) {
						score += (pr_wt * pr_wld) / (pr_wd * pr_ld);
					} // end of if
				} // end of for (w)
				scoreList.add(score);
			} // end of for (l)
			// get the top 5 labels for topic k
			for (int i = 0; i < 6; i++) {
				int bestLabelIndex = findMaxScoreIndex(scoreList);
				if (i == 0) {
					topicsLabels.add(labels.get(bestLabelIndex));
				} // end of if
				System.out.println("Best label for topic " + k + ": " + labels.get(bestLabelIndex));
//				System.out.println("Best label for topic " + k + ": " + topicsLabels.get(k));
				scoreList.set(bestLabelIndex, 0.0);
			} // end of for (i)
			System.out.println();
		} // end of for (k)
		return topicsLabels;
	} // end of computeExpectationOfPmiForLabels
	
	/*************************************************
	 * @param a
	 */
	public static int findMaxScoreIndex(List<Double> a) {
		int maxIndex = 0;
		double max = a.get(0);
		for (int i = 0; i < a.size(); i++) {
			if (max < a.get(i)) {
				max = a.get(i);
				maxIndex = i;
			} // end of if
		} // end of for (double1)
		return maxIndex;
	} // end of findMaxScoreIndex



	/*************************************************
	 * @param topLabelsFileName
	 * @param topLabels
	 */
	public static void readTopLabels(String topLabelsFileName, List<String> topLabels) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(topLabelsFileName));
			String line = "";
			while ((line = br.readLine()) != null && !line.equals("")) {
				String [] tokens = line.split("<>");
				if (tokens[0].length() > 1 && tokens[1].length() > 1) {
					String label = tokens[0].toLowerCase() + " " + tokens[1].toLowerCase();
					topLabels.add(label);
				} // end of if
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("\"" + topLabelsFileName + "\" does NOT exits. Please enter a valid file name.");
		} catch (IOException e) {
		}
	} // end of readTopLabels


	public static void readTopicTopWords(String topicTopWordsFileName, Map<Integer, List<String>> topicTopWords) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(topicTopWordsFileName));
			String line = "";
			while ((line = br.readLine()) != null && !line.equals("")) {
				String [] tokens = line.split("\t");
				int topicId = Integer.parseInt(tokens[0]);
				List<String> wordlist = new ArrayList<String>(Arrays.asList(tokens[2].split(" ")));
				topicTopWords.put(topicId, wordlist);
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("\"" + topicTopWordsFileName + "\" does NOT exits. Please enter a valid file name.");
		} catch (IOException e) {
		}
	} // end of readTopicTopWords
	
	public static void createTopicWordProbabilityMatrix(String wtPrFilename, Map<Integer, Map<String, Double>> topicWordMatrix, Map<Integer, Double> normalizationConstant) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(wtPrFilename));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split("\t");
				int topicId = Integer.parseInt(tokens[0]);
				String word = tokens[1];
				double wtPr = Double.valueOf(tokens[2]);
				if (topicWordMatrix.get(topicId) == null) {
					Map<String, Double> twMap = new HashMap<String, Double>();
					twMap.put(word, wtPr);
					topicWordMatrix.put(topicId, twMap);
				}else {
					topicWordMatrix.get(topicId).put(word, wtPr);
				} // end of if
				if (normalizationConstant.get(topicId) == null) {
					normalizationConstant.put(topicId, wtPr);
				}else {
					double sumOfPr = normalizationConstant.get(topicId) + wtPr;
					normalizationConstant.put(topicId, sumOfPr);
				} // end of if
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("\"" + wtPrFilename + "\" does NOT exits. Please enter a valid file name.");
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
	} // end of createTopicWordProbabilityMatrix


	/*************************************************
	 * 
	 */
	public static Indexer indexDocuments() {
		String docsPath = "/home/mehdi/1414docs-original/";
//		String docsPath = "/home/mehdi/683BAWEfiles/";
//		String docsPath = "/home/mehdi/400-docs/";
		Indexer indexer = new Indexer();
		try {
			indexer.createIndexWriter();
			indexer.indexDocumentCollection(docsPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return indexer;
	} // end of indexDocuments


	/*************************************************
	 * 
	 */
	public static void calculateProbabilities() {
		OntoLDAModel1 topicModel = new OntoLDAModel1(5);
		topicModel.loadResults();
	}
	
	
	public static void createCorpusConcepts() {
		ImportData importer = new ImportData();
		importer.createCollectionOfConcepts("/home/mehdi/domainEntities/");
	}

	
	public static void removeIrrelevantFiles() {
		ImportData importer = new ImportData();
		// delete files from relatedconcepts folder that don't exist in wikipediaPages folder
//		importer.deleteIrrelevantFiles("/Users/mehdi/corpus/", "/Users/mehdi/experiment/");
//		importer.deleteIrrelevantFiles("/home/mehdi/tcoherency/relatedconcepts/", "/home/mehdi/tcoherency/wikipediaPages/");
//		importer.deleteIrrelevantFiles("/home/mehdi/tcoherency/wikipediaPages/", "/home/mehdi/tcoherency/relatedconcepts/");
		importer.deleteIrrelevantFiles("/home/mehdi/entlda/relatedconcepts/", "/home/mehdi/entlda/wikipediaPages/");
//		importer.deleteIrrelevantFiles("/home/mehdi/entlda/wikipediaPages/", "/home/mehdi/entlda/relatedconcepts/");
	}

	/*************************************************
	 * 
	 */
	public static void prepareData() {
		ImportData importer = new ImportData();
		
		String inputFilesPath = Configuration.getProperty("inputFilesPath");
		String outputFilesPath = Configuration.getProperty("dataDirPath");
		String wikiFilesPath = Configuration.getProperty("wikipediaPagesPath");
		String entityFilesPath = Configuration.getProperty("entityFilesPath");
		String relatedConcetspath = Configuration.getProperty("relatedConceptsFilesPath");
		try {
			Map<String,Integer> corpusConceptIdMap = importer.createCorpus(inputFilesPath, wikiFilesPath, outputFilesPath, entityFilesPath, "corpus.txt", "vocabularyLookup.txt", "wikiConceptVocab.txt");
			
			importer.createRelatedConceptsMap(relatedConcetspath, outputFilesPath, corpusConceptIdMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of prepareData


	/*************************************************
	 * 
	 */
	public static void fitlerFiles() {
		String content = "";
		String fileName = "/home/mehdi/400Docs.txt";
//		String fileName = "/Users/Mehdi/Downloads/lifsciencefilenames.csv";
//		String fileName = "/Users/Mehdi/400Docs.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("\"" + fileName + "\" does NOT exits. Please enter a valid file name.");
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
//		System.out.println("line:"+content);
		Set<String> fnames = new HashSet<String>(Arrays.asList(content.split("\n")));
		String corpus_path = "/home/mehdi/400-docs/";
//		String corpus_path = "/Users/Mehdi/Downloads/CORPUS_TXT/";
		File directory = new File(corpus_path);
		for (File f : directory.listFiles()) {
//			String name = f.getName().replace(".txt", "");
			String name = f.getName();
			if (!fnames.contains(name)) {
				f.delete();
//				System.out.println(name);
			}
			
		}
		
		
	}


	/*************************************************
	 * 
	 */
	public static void runOntoLdaModel1() {
		OntoLDAModel1 topicModel = new OntoLDAModel1(5);
		// Step 1
		// Creates a vocabulary set for every entity in the ontology (corpus)
		// So, it must be run only once
//		topicModel.createWikiConceptVocabulary();
		
		// Creates a map for relating concepts of the concepts in the corpus and serializes it
		// So, it must be run only once
		topicModel.createRelatingConceptsMap();
		
//		/*
		
		// Step 2
		// Initialize Topics and Concepts
		topicModel.initializeTopicsAndCocepts();
		
		
		// Step 3
		// Run Gibbs sampling
//		try {
//			topicModel.runGibbsSampling();
//		}catch (Exception e) {
//			logger.catching(Level.ERROR, e);
//			e.printStackTrace();
//		}
//		*/
		
	} // end of runOntoLdaModel1


	/**
	 * 
	 */
	public static void populateData() {
//		CategoryNameMapper wikipediaCategories = loadWikipediaCategoryNames();
//		Map<Integer,Set<Integer>> wikiHierarchy = loadWikipediaHierarchy();
//		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
//		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
//		String catName = "Main_topic_classifications";
//		int catId = catNamesToInt.get(catName);
//		Set<Integer> mainCategories = wikiHierarchy.get(catId);
//		catName = "Computer_science";
//		catId = catNamesToInt.get(catName);
//		topics = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy);
//		Set<String> conceptList = findCategoryConceptList(topics, wikipediaCategoryNames);
//		extractWikipediaPagesForConcepts(conceptList);
	} // end of populateData

	

	

}
