/**
 * 
 */
package cs.uga.edu.dicgenerator;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.GRAPH;
import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;
import grph.algo.bfs.BFSResult;
import grph.algo.distance.DijkstraAlgorithm;
import grph.algo.distance.PageRank;
import grph.properties.IntProperty;
import grph.properties.LabelProperty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import cnrs.grph.set.HashIntSet;
import cnrs.grph.set.IntSet;

import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import cs.uga.edu.esum.EntityProc;
import cs.uga.edu.topicmodel.ImportData;
import cs.uga.edu.wikiaccess.WikipediaAccessLayer;



/**
 * @author Mehdi
 *
 */
public class TextProcessor {

	
	
	public static void main(String[] args) throws Exception {
		
		
		
		// This method is used for clustering evaluation for sOntoLDA project
//		runClassificationEvaluationOnReutersDataset();
		
		
		// this is used to for "Discovering topics using Wikipedia categories" project, sOntoLDA
//		runExperimentForsOntoLdaForWikipediaDataset();
		// this method is used for tagging Reuters documents, sOntoLDA
//		runExperimentForsOntoLdaForReutersDataset();
		
		// this is used for EntLDA project
//		doPreprocessForEntLDA();
		
	}
	public static void runClassificationEvaluationOnReutersDataset() {
		DbpediaProcessor dbpediaProcessor = new DbpediaProcessor();
		
		// the following lines will move documents to binary folders (e.g. business, non-business) to be used later for binary classification
//		dbpediaProcessor.filterDocumentsByCategory("business");
//		dbpediaProcessor.filterDocumentsByCategory("health");
//		dbpediaProcessor.filterDocumentsByCategory("science");
		
		// creates a corpus of documents represented as bag of words from tags
//		dbpediaProcessor.createTaggedDocs("/home/mehdi/topicdiscovery/posteriorFiles/", "dtPr.csv");
		
		// the following lines will move documents to binary folders (e.g. business, non-business) to be used later for binary classification
//		dbpediaProcessor.filterTaggedDocumentsByCategory("business");
//		dbpediaProcessor.filterTaggedDocumentsByCategory("health");
		dbpediaProcessor.filterTaggedDocumentsByCategory("science");
		
		// this method is used for clustering which is not used any more!
//		dbpediaProcessor.computeTfIdf("/home/mehdi/topicdiscovery/posteriorFiles/", "dtPr.csv");
	} // end of runClassificationEvaluationOnReutersDataset
	
	public static void runExperimentForsOntoLdaForReutersDataset() {
		DbpediaProcessor dbpediaProcessor = new DbpediaProcessor();
		
		// get training articles from all the articles
//		String input_dir = "/home/mehdi/topicdiscovery/reutersDocs/";
//		String output_dir = "/home/mehdi/topicdiscovery/trainingdocs/";
//		int sampleSize = 2331;  // 80% of 2914 docs
//		dbpediaProcessor.getSampleArticles(input_dir, output_dir, sampleSize);
		
		// get sample articles for test set
//		String input_dir = "/home/mehdi/topicdiscovery/reutersDocs/";
//		String output_dir = "/home/mehdi/topicdiscovery/testdocs/";
//		String training_dir = "/home/mehdi/topicdiscovery/trainingdocs/";
//		int sampleSize = 583;  //20% for 2914 docs
//		dbpediaProcessor.getSampleTestArticles(input_dir, output_dir, training_dir, sampleSize);
		
		// create corpus-related files for training set
//		ImportData importer = new ImportData();
//		String inputFilesPath = "/home/mehdi/topicdiscovery/reutersDocs/";
//		String wikiFilesPath = "/home/mehdi/topicdiscovery/wikipediaPages/";
//		String outputFilesPath = "/home/mehdi/topicdiscovery/preprocessedFiles/";
//		try {
//			importer.createCorpusFilesForReutersDataset(inputFilesPath, wikiFilesPath, outputFilesPath, "corpus.txt", "vocabularyLookup.txt", "wikiConceptVocab.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		// evaluate the test documents
		System.out.println("======== Evaluation Result for Reuters Dataset ========");
		int prK = 5;
		dbpediaProcessor.measurePrecisionForReutersDataset("dtPr.csv", prK);
		dbpediaProcessor.measureMeanAveragePrecisionForReutersDataset("dtPr.csv",prK);
		
		
	} // end of runExperimentForsOntoLdaForReutersDataset

	public static void runExperimentForsOntoLdaForWikipediaDataset() {
		DbpediaProcessor dbpediaProcessor = new DbpediaProcessor();
//		WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
		 // the next two lines are run ONLY once to create the DBpedia Taxanomy
//		dbpediaProcessor.mapCategoryNamesToInt();
//		dbpediaProcessor.createWikipediaTaxanomyMapFromParentToChild();
		
		// create a map between user-defined categories and their corresponding Wikipedia articles and serialize it
//		wikiaccess.createCategoryToConceptMap(2);
		
		
		// remove useless categories and concepts
//		wikiaccess.cleanCategoriesAndConcepts();
		
		// create tfIdf for each category and term in the Wikipedia, (sub-ontology)
//		Map<Integer,Set<String>> catToConMap = wikiaccess.loadCategoryToConceptMapToMemory();
//		Set<Integer> keyset = catToConMap.keySet();
//		Set<String> allarticles = new HashSet<String>();
//		for (int cid : keyset) {
//			allarticles.addAll(catToConMap.get(cid));
//		}
//		String output_dir = "/home/mehdi/topicdiscovery/wikipediaPages/";
//		dbpediaProcessor.downloadWikipediaPagesForCorpusConcepts(allarticles, output_dir);
		
		
		// create training and test set documents
//		wikiaccess.createTrainAndTestSets1();  // different approach
//		wikiaccess.createTrainAndTestSets2();
		
		// download articles for training and test set
//		String concepts_dir  = "/home/mehdi/topicdiscovery/trfolder/";
//		String output_dir = "/home/mehdi/topicdiscovery/trainingdocs/";
//		dbpediaProcessor.downloadWikipediaPagesForCorpusConcepts(concepts_dir, output_dir);
////		dbpediaProcessor.downloadDbpediaPagesForCorpusConcepts(concepts_dir, output_dir);
//		concepts_dir  = "/home/mehdi/topicdiscovery/tsfolder/";
//		output_dir = "/home/mehdi/topicdiscovery/testdocs/";
//		dbpediaProcessor.downloadWikipediaPagesForCorpusConcepts(concepts_dir, output_dir);
////		dbpediaProcessor.downloadDbpediaPagesForCorpusConcepts(concepts_dir, output_dir);
		
		// get a set of sample articles from all the articles
//		String input_dir = "/home/mehdi/topicdiscovery/wikipediaArticles/";
//		String output_dir = "/home/mehdi/topicdiscovery/trainingdocs/";
//		int sampleSize = 3000;
//		dbpediaProcessor.getSampleArticles(input_dir, output_dir, sampleSize);
		
		// get sample articles for test set
//		String input_dir = "/home/mehdi/topicdiscovery/wikipediaArticles/";
//		String output_dir = "/home/mehdi/topicdiscovery/testdocs/";
//		String training_dir = "/home/mehdi/topicdiscovery/trainingdocs/";
//		int sampleSize = 1000;
//		dbpediaProcessor.getSampleTestArticles(input_dir, output_dir, training_dir, sampleSize);
		
		// create corpus-related files for training set
//		ImportData importer = new ImportData();
//		String inputFilesPath = "/home/mehdi/topicdiscovery/trainingdocs/";
//		String wikiFilesPath = "/home/mehdi/topicdiscovery/wikipediaPages/";
//		String outputFilesPath = "/home/mehdi/topicdiscovery/preprocessedFiles/";
//		try {
//			importer.createCorpusFiles(inputFilesPath, wikiFilesPath, outputFilesPath, "corpus.txt", "vocabularyLookup.txt", "wikiConceptVocab.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
//		// create corpus-related files for test set
//		ImportData importer = new ImportData();
//		String inputFilesPath = "/home/mehdi/topicdiscovery/testdocs/";
//		String outputFilesPath = "/home/mehdi/topicdiscovery/preprocessedFiles/";
//		try {
//			importer.createTestCorpusFiles(inputFilesPath, outputFilesPath, "testcorpus.txt", "vocabularyLookup.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
		// evaluate the test documents
		System.out.println("======== Evaluation Result for Wikipedia Dataset ========");
		int prK = 10;
		boolean isRelaxed = true;
//		dbpediaProcessor.measurePrecision("dtPr-test-wikipediaDataset.csv", prK, isRelaxed);
		dbpediaProcessor.measureMeanAveragePrecision("dtPr-test-wikipediaDataset.csv",prK, isRelaxed);
		
//		test();
		
	} // end of runExperimentForsOntoLdaForWikipediaDataset
	
	

	public static void test() {
		WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
		Map<Integer, Set<String>> catToConMap   = wikiaccess.loadCategoryToConceptMapToMemory();
		CategoryNameMapper wikipediaCategories  = wikiaccess.loadWikipediaCategoryNames();
//		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		Set<Integer> categoryIds = catToConMap.keySet();
		for (int cid : categoryIds) {
			String cname = wikipediaCategoryNames.get(cid);
			if (cname.equals("Taxation")) {
				Set<String> categoryConceptSet = catToConMap.get(cid);
				System.out.println("concept Size: " + categoryConceptSet.size());
				Set<String> tset = new TreeSet<String>();
				for (String conceptname : categoryConceptSet) {
					tset.add(conceptname);
				}
				for (String conceptname : tset) {
					System.out.println(conceptname);
				}
			}
		}
	}

	public static void doPreprocessForEntLDA() {
		DbpediaProcessor dbpediaProcessor = new DbpediaProcessor();
		// Maps all the DBpedia entity names (whole name) to integers and serializes the result //
//		dbpediaProcessor.mapEntityLocalNamesToInt();
		
		// Maps all the DBpedia entity name word tokens to integers and serializes the result //
//		dbpediaProcessor.mapWordToInt();
		
		// Maps all the unique entity labels to corresponding entities //
//		dbpediaProcessor.mapAllUniqueEntityNamesToEntityIds();
		
		// Creates a Finite State Machine from the surface names of the all the entities in DBpedia //
//		dbpediaProcessor.createPatternMatchingAutomata();
		
		// Loads the DBpedia category names into memory //
//		dbpediaProcessor.loadCatNameListToMemory();
		
		// Identifies Entity mentions as well as Initial Wikipedia Entities in documents and create files
//		String rootFolder = "/home/mehdi/taxonomyProject/";
//		String rootFolder = "/home/mehdi/tcoherency/";
		String rootFolder = "/home/mehdi/entlda/";
		String input_dir = rootFolder + "documents/";
		String emention_dir = rootFolder + "entityMentions/";
		String initdocentity_dir =  rootFolder + "initdocEntities/";
//		dbpediaProcessor.identifyAndCreateEntityFiles(input_dir, emention_dir, initdocentity_dir); 
		
		// Filter Non-relevant entities from initial entities //
		String finalEntities_dir = rootFolder + "finaldocEntities/";
		dbpediaProcessor.FilteNonRelevantEntitiesFromFile(initdocentity_dir, finalEntities_dir);
		
		// Download Wikipedia Pages for the corpus concepts //
		String wikiPageDir = rootFolder + "wikipediaPages/";
		dbpediaProcessor.downloadWikipediaPagesForCorpusConcepts(finalEntities_dir, wikiPageDir); 
		
		// Create related concepts to concepts occurring in documents
		String relatedConceptsDir = rootFolder + "relatedconcepts/";
		dbpediaProcessor.createEntityBidirectionalRelatedConceptsFromFile(finalEntities_dir, relatedConceptsDir); 
		
//		PatternMatchingMachine.recognizeEntites_Batch();
		
		
//		dbpediaProcessor.createYagoTaxanomyMap();
//		dbpediaProcessor.createWikipediaTaxanomyMapFromChildToParent();
//		dbpediaProcessor.createWikipediaTaxanomyMap_ID_FromChildToParent();
//		dbpediaProcessor.mapCategoryNamesToInt();
//		CategoryProcessor.computeCategorySubgraphWordCount();
//		CategoryProcessor.createIndexForWikipediaCategories();
		
		
//		PatternMatchingMachine.recognizeEntites();
//		PatternMatchingMachine.createSemanticGraphFromEntities();
//		PatternMatchingMachine.createSemanticGraphFromEntities_batch();
		
//		PatternMatchingMachine.createConceptsReferingToEntityFromFile();
//		PatternMatchingMachine.createEntityrelatedConceptsFromFile();
		
		
//		PatternMatchingMachine.findEntityCategoriesFromFile();
//		CategoryProcessor.fillCategoryEntityTable();
//		dbpediaProcessor.persistWikipediaEntitiesIncomingLinks();
		
	}
	
	

} // end of TextProcessor
