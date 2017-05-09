/**
 * 
 */
package cs.uga.edu.dicgenerator;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.GRAPH;
import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;
import grph.algo.bfs.BFSResult;
import grph.algo.distance.DijkstraAlgorithm;
import grph.algo.distance.SearchResult;
import grph.path.VPath;
import grph.properties.LabelProperty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import cnrs.grph.set.HashIntSet;
import cnrs.grph.set.IntSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * @author Mehdi
 * 
 */
public class PatternMatchingMachine{

	private String DIR_PATH = "/home/mehdi/textCategorization/";
	private String CONF_DIR_PATH = "/home/mehdi/textConf/";
//	private String RSS_DIR_PATH = "/home/mehdi/inputfiles/";
//	private String RSS_DIR_PATH = "/home/mehdi/400-docs-T20/";
//	private String RSS_DIR_PATH = "/home/mehdi/683-docs-T20/";
	private String RSS_DIR_PATH = "/home/mehdi/1414-docs-T20/";
//	private String RSS_DIR_PATH = "/home/mehdi/rss/reuters/sports/";
	private String EXPT_DIR_PATH = "/home/mehdi/";
	// private final int numOfKeywords = 4;
	// private final int numOfEdges = 10;
	private final int FAIL = -1;
	// private List<String> keywords = null;
	private Map<String, Integer> wordToIntMap = null;
	private Map<Integer, List<String>> outputSet = null;
	private int newstate = 0;
	private State startState = null;
	private List<State> graphNodes = null;
	private Map<Integer, Integer> failureMap = null;
	private int totalNumOfNodes = 1;
	private final int interval = 100000;
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String categoryUriPrefix = "http://dbpedia.org/resource/Category:";
	// private Map<String, Integer> wikipediaEntities = null;
	private Map<Integer, IntSet> ccVertexSets = null;
	private Map<Integer, Integer> setRedirectionMap = null;
	private Map<String, Integer> entityRepetition = null;
	private Map<Integer, Set<String>> entityCategoryMap = null;
	private Map<String, String> yagoTaxonomyMap = null;
	private Map<String, Set<String>> wikiTaxonomyMapFromChildToParent = null;
	private Map<Integer, Set<Integer>> wikiTaxonomyMapFromParentToChild = null;
	private Map<Integer, Set<Integer>> wikiTaxonomyMap_ID_FromChildToParent = null;
	private List<String> localNames = null;
	private Map<String, Integer> namesToInt = null;
	private int wikipediaSize = 5100000;
	private final int MaxNumOfLinksPerCategory = 2000;
	private final Logger logger = Logger.getLogger(PatternMatchingMachine.class.getName());

	public void main(String[] args) {
		// createPatternMatchingAutomata();
		// recognizeEntites();
	}
	
	
	
	
	
	
	public Set<String> randomWalkScores(List<String> sentenceList, Set<String> stopWords) {
		Map<String,Integer> wordToVertexMap = new HashMap<String,Integer>();
		SemanticGraph textGraph = new SemanticGraph();
		LabelProperty vlp = textGraph.getVertexLabelProperty();
		String prevWord = "";
		for (String sentc : sentenceList) {
			String[] docWords = sentc.split(" ");
			for (int wordCnt = 0; wordCnt < docWords.length; wordCnt++) {
				String currentWord = docWords[wordCnt].toLowerCase();
				currentWord = currentWord.replace("'s", "").replace("'S", "").replace("'t", "").replace("$", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace("\"", "").replace("--", "").replace("(", "").replace(")", "");
				currentWord = currentWord.replaceAll("[0-9]", "");
				if (stopWords.contains(currentWord) || currentWord.contains("[")) {
					continue;
				} // end of if
				if (currentWord.length() < 3) {
					continue;
				} // end of if
				int vid = -1;
				if (wordToVertexMap.get(currentWord) == null) {
					vid = textGraph.addVertex();
					vlp.setValue(vid, currentWord);
					wordToVertexMap.put(currentWord, vid);
				}else {
					vid = wordToVertexMap.get(currentWord);
				} // end of if
				if (!prevWord.equals("")) {
					int preVid = wordToVertexMap.get(prevWord);
					textGraph.addDirectedSimpleEdge(vid, preVid);
					textGraph.addDirectedSimpleEdge(preVid, vid);
				} // end of if
				prevWord = currentWord;
			} // end of for
		} // end of for
//		System.out.println("Creating Text Graph...");
//		try {
//			FileWriter dotFile = new FileWriter("textGraph.dot");
//			dotFile.write(textGraph.toDot());
//			dotFile.close();
//		} catch (IOException e) {
//		}
//		System.out.println("Text Graph Created successfully.");
		int [] vertices = textGraph.getVertices().toIntArray();
		Map<Integer,Double> prevScoreMap = new HashMap<Integer,Double>();
		Map<Integer,Double> curScoreMap = new HashMap<Integer,Double>();
		double convergenceThreshold = 0.0001;
		int numOfIteration = 100;
		boolean isConverged = true;
		for (int vid : vertices) {
			prevScoreMap.put(vid, 0.25);
		} // end of for
		for (int steps = 0;steps < numOfIteration; steps++) {
			for (int vid : vertices) {
				double score = runTextRank(textGraph, vid, 0.85, prevScoreMap);
				curScoreMap.put(vid, score);
			} // end of for
			for (int vid : vertices) {
				double curScore = curScoreMap.get(vid);
				double preScore = prevScoreMap.get(vid);
				double err = Math.abs(curScore - preScore);
				prevScoreMap.put(vid, curScore);
				if (err > convergenceThreshold) {
					isConverged =  false;
				} // end of if
			} // end of for
			if (isConverged) {
				break;
			} // end of if
			isConverged = true;
		} // end of for
		int topK = 10;
		List<Integer> sortedVids = sortBasedOnScore(curScoreMap);
		Set<String> topKWords = new HashSet<String>();
		for (int vid = 0;vid < sortedVids.size();vid++) {
			if (vid < topK) {
				topKWords.add(vlp.getValue(vid));
			}else {
				break;
			} // end of if
//			String word = vlp.getValue(vid);
//			double rw   = curScoreMap.get(vid);
//			System.out.println("Word: " + word + "  rw: " + rw);
		} // end of for
		return topKWords;
		
		/*
		Map<Integer,Double> tfIdfMap = new HashMap<Integer,Double>();
		double numOfSentences = Double.valueOf(sentenceList.size());
		for (int vid : sortedVids) {
			String word = vlp.getValue(vid).toLowerCase();
			double rw   = curScoreMap.get(vid);
			int frequency = 0;
			for (String sentence : sentenceList) {
				sentence = sentence.toLowerCase();
				if (sentence.contains(word)) {
					frequency++;
				} // end of if
			} // end of for
			double tfidf = rw * Math.log(numOfSentences / frequency);
			tfIdfMap.put(vid, tfidf);
			frequency = 0;
		} // end of for
		sortedVids = PatternMatchingMachine.sortBasedOnScore(tfIdfMap);
		for (int vid : sortedVids) {
			String word = vlp.getValue(vid);
			double rw   = curScoreMap.get(vid);
			System.out.println("Word: " + word + "  rw: " + rw + "  tfIdf: " + tfIdfMap.get(vid));
		} // end of for
		return curScoreMap;
		*/
	} // randomWalkScores
	
	public double runTextRank(SemanticGraph textGraph, int vid, double alpha, Map<Integer, Double> prevScoreMap) {
		double score = 0;
		int [] incomingVertices = textGraph.getInNeighbors(vid).toIntArray();
		if (incomingVertices.length == 0) {
			score += prevScoreMap.get(vid);
		}else {
			for (int v : incomingVertices) {
				int outgoingVertices = textGraph.getOutNeighbours(v).size();
				score +=  prevScoreMap.get(v) / outgoingVertices;
			} // end of for
		} // end of if
		score *= alpha;
		score += 1 - alpha;
		return score;
	} // end of runTextRank
	
	public Map<Integer, Double> categoryRandomWalkScore(SemanticGraph taxonomyGraph, Set<Integer> coreEntities) {
		Map<Integer,Double> prevScoreMap = new HashMap<Integer,Double>();
		Map<Integer,Double> curScoreMap = new HashMap<Integer,Double>();
		int [] vertices = taxonomyGraph.getVertices().toIntArray();
		double convergenceThreshold = 0.0001;
		int numOfIteration = 100;
		boolean isConverged = true;
		for (int vid : vertices) {
			prevScoreMap.put(vid, 0.25);
		} // end of for
		for (int steps = 0;steps < numOfIteration; steps++) {
			for (int vid : vertices) {
				double score = runTextRank(taxonomyGraph, vid, 0.85, prevScoreMap);
				curScoreMap.put(vid, score);
			} // end of for
			for (int vid : vertices) {
				double curScore = curScoreMap.get(vid);
				double preScore = prevScoreMap.get(vid);
				double err = Math.abs(curScore - preScore);
				prevScoreMap.put(vid, curScore);
				if (err > convergenceThreshold) {
					isConverged =  false;
				} // end of if
			} // end of for
			if (isConverged) {
				System.out.println("step: " + steps);
				break;
			} // end of if
			isConverged = true;
		} // end of for
//		List<Integer> sortedVids = sortBasedOnScore(curScoreMap);
		return curScoreMap;
	} // end of categoryRandomWalkScore
	
	public void recognizeEntites_ParseCombination() {
		long stTime = System.currentTimeMillis();
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		graphNodes = loadDeterministicGraphToMemory();
		failureMap = loadFailureFunctionMapToMemory();
		outputSet = loadOutputMapToMemory();
		Map<String, Set<Entity>> uniqueNames = DbProcessor.loadUniqueNameListToMemory();
		LocalNameMapperClass localNamesMapperClass = DbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
		localNames = localNamesMapperClass.getLocalNameList();
		namesToInt = localNamesMapperClass.getNameToInt();
		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		// yagoTaxonomyMap = DictionaryGenerator.loadYagoTaxonomyMapToMemory();
		wikiTaxonomyMapFromChildToParent = DbProcessor.loadWikiTaxonomyMapFromChildToParentToMemory();
		wikiTaxonomyMap_ID_FromChildToParent = DbProcessor.loadWikiTaxonomyMap_ID_FromChildToParentToMemory();
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
		wikiTaxonomyMapFromParentToChild = DbProcessor.loadWikiTaxonomyMapFromParentToChildToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> catNames = catNameMapper.getCatNameList();
		System.out.println("Total Load Time: " + (System.currentTimeMillis() - stTime));
		// List<String> wlist = wordMapperClass.getWordList();
		Set<String> stopWords = readStopWords();
		Set<String> timeRelatedWords = readTimeRelatedEntities();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_sportslog.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_technologylog.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_entertainment.log");
			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_business.log");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			File rssDirectory = new File(RSS_DIR_PATH);
			JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
			JsonParser jsonParser = new JsonParser();
			JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
			int numOfEntityMatchAttr = confObject.get("numOfEntityMatchAttr").getAsInt();
			JsonObject entityMatchAttrWeightObj = confObject.get("entityMatchAttrWeight").getAsJsonObject();
			String userCats = confObject.get("categoryCombination").getAsString();
			int categoryhierarchyLevel = confObject.get("categoryhierarchyLevel").getAsInt();
			double alpha = confObject.get("alpha").getAsDouble();
			JsonObject relationImportanceWeightObj = confObject.get("relationImportanceWeight").getAsJsonObject();
			double authoritativeThreshold = confObject.get("authoritativeThreshold").getAsDouble();
			int numOfSteps = confObject.get("numOfSteps").getAsInt();
			double centralityThreshold = confObject.get("centralityThreshold").getAsDouble();
			int topAuthoritativeEntities = confObject.get("topK-AuthoritativeEntities").getAsInt();
			int topCentralEntities = confObject.get("topK-CentralEntities").getAsInt();
			int fileCounter = 1;
			Map<String,Integer> catBatchResult = new HashMap<String,Integer>(); 
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(RSS_DIR_PATH + fileName);
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
				SemanticGraph semanticGraph = new SemanticGraph();
				semanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				LabelProperty vertexLabelProperty = semanticGraph.getVertexLabelProperty();
				int intialCapacity = confObject.get("initialCapacity").getAsInt();
				List<String> initialEntities = new ArrayList<String>(4 * intialCapacity);
				SentenceDetector sd = null;
				try {
					sd = new SentenceDetector();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				List<String> sentenceList = sd.getAllSentencesFromDocument(document);
				Set<String> topRWScoreWords = randomWalkScores(sentenceList, stopWords);

				//				System.out.println("============> topRWScoreWords: " +  topRWScoreWords + "\n");
				Set<String> topRWScoreConceptsMentions = new HashSet<String>();;
				Set<String> topRWScoreConcepts = new HashSet<String>();
				int state = 0;
				String preEntity = "-1";
				String curEntity = "-2";
				Set<String> foundEntities = new HashSet<String>(intialCapacity);
				entityRepetition = new HashMap<String, Integer>(intialCapacity);
				entityCategoryMap = new HashMap<Integer, Set<String>>(intialCapacity);
				stTime = System.currentTimeMillis();
				for (String sentence : sentenceList) {
					sentence = sentence.toLowerCase();
					String[] sentcWords = sentence.split(" ");
					for (int wordCnt = 0; wordCnt < sentcWords.length; wordCnt++) {
						String currentWord = sentcWords[wordCnt];
						currentWord = currentWord.replace("'s", "").replace("'S", "").replace("'t", "").replace("$", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace("\"", "").replace("--", "").replace("(", "").replace(")", "");
						currentWord = currentWord.replaceAll("[0-9]", "");
						/*
						if (stopWords.contains(currentWord) || currentWord.contains("[")) {
							continue;
						} // end of if
						if (currentWord.length() < 3) {
							continue;
						} // end of if
						 */
						int wordId = wordToIntMap.get(currentWord) != null ? wordToIntMap.get(currentWord) : -1;
						if (wordId != -1) {
							while (g(state, wordId) == FAIL) {
								state = f(state);
							} // end of while
							state = g(state, wordId);
							List<String> entities = output(state);
							if (entities != null) {
								curEntity = entities.get(0);
								String[] parts = curEntity.split(" ");
								while (true) {
									if (stopWords.contains(parts[0])) {
										curEntity = curEntity.substring(parts[0].length()).trim();
										parts = curEntity.split(" ");
									} else {
										break;
									} // end of if
								} // end of while
								if (curEntity.length() > 0) {
									if (curEntity.contains(preEntity)) {
										foundEntities.remove(preEntity);
										foundEntities.add(curEntity);
										preEntity = curEntity;
									} else {
										foundEntities.add(curEntity);
										preEntity = curEntity;
									} // end of if
									preEntity = curEntity;
								} // end of if
								int repetition = entityRepetition.get(curEntity) != null ? entityRepetition.get(curEntity) : 0;
								repetition++;
								entityRepetition.put(curEntity, repetition);
								initialEntities.addAll(entities);

								// Add all concepts of topRWWords to foundEntities
								// Start from 1 beacuse the first element is already in the foundEntities
								if (topRWScoreWords.contains(currentWord)) {
									topRWScoreConceptsMentions.add(entities.get(0));
									for (int i = 1;i < entities.size();i++) {
										String concept = entities.get(i);
										parts = concept.split(" ");
										/*
										while (true) {
											if (stopWords.contains(parts[0])) {
												concept = concept.substring(parts[0].length()).trim();
												parts = concept.split(" ");
											} else {
												break;
											} // end of if
										} // end of while
										 */
										if (parts.length == 1) {  // we ONLY add the concept correspondent to RWScoreWord from the list of concepts
											foundEntities.add(concept);
											repetition = entityRepetition.get(concept) != null ? entityRepetition.get(concept) : 0;
											repetition++;
											entityRepetition.put(concept, repetition);
											topRWScoreConceptsMentions.add(concept);
										} // end of if
									} // end of for (i)
								} // end of if
							} // end of if
						} // end of if (wordId != -1)
					} // end of for
				} // end of for
				// Set<String> foundEntities = new
				// HashSet<String>(fentities);
//				System.out.println("Total Entity Spotter time: " + (System.currentTimeMillis() - stTime));
				//				System.out.println("\n=============== Recognized Entities Start ================");
				//				System.out.println("Size: " + initialEntities.size());
				//				System.out.println(initialEntities);
				//				System.out.println("=============== Recognized Entities End   ================\n");
				// System.out.println("Number of Entities BEFORE removing Stop Words: "
				// + foundEntities.size() + "\n");
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				// System.out.println("Number of Entities AFTER removing Stop Words: "
				// + foundEntities.size() + "\n");
//				System.out.println("\n=============== Revised Recognized Entities Start ================");
//				System.out.println("Size: " + foundEntities.size());
//				System.out.println(foundEntities);
//				System.out.println("\n=============== Revised Recognized Entities End   ================\n");


				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
				int ecounter = 0;
				if (numOfEntityMatchAttr == 1) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if (enSet != null )
					} // end of while
				} else if (numOfEntityMatchAttr == 2) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} // end of if
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 3) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 4) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 5) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 6) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					double disambiguationNameShorWeight = entityMatchAttrWeightObj.get("disambiguationNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5' || et.getAttr() == '6') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '6') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameShorWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				}
//				System.out.println("\nTotal number of Wikipedia Entities including all attributes: " + ecounter);
				// System.out.println("Total number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\": "
				// + wikipediaEntityIds.size());
//				System.out.println("\nTotal number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\": " + wikipediaEntities.size());
				// System.out.println("Total number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\" Or \"Redirect Name Short\": "
				// + wikipediaEntities.size());

				//				System.out.println("Top K RW Concepts: " + topRWScoreConcepts + "\n");
//				stTime = System.currentTimeMillis();

				// Below is another approach for finding the Edges //
				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);

//				System.out.println("Total time to construct the Semantic Graph: " + (System.currentTimeMillis() - stTime));
				// semanticGraph.display();
				// semanticGraph.displayGraphvizPNG();
				// GraphvizImageWriter imgWriter = new GraphvizImageWriter();

//				System.out.println("Creating \"semanticGraph.dot\" file on disk...");
//				FileWriter dotFile = new FileWriter("semanticGraph.dot");
//				dotFile.write(semanticGraph.toDot());
//				dotFile.close();
//				System.out.println("\"semanticGraph.dot\" file Successfully created.\n");
				// FileOutputStream output = new
				// FileOutputStream("semanticGraph.png");
				// imgWriter.writeGraph(semanticGraph,
				// GraphvizImageWriter.COMMAND.dot,
				// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
				// output.flush();
				// output.close();
				SemanticGraph thematicSemanticGraph = new SemanticGraph();
				thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
//				stTime = System.currentTimeMillis();
				IntSet largestCComponent = null;
				int max = 0;
				int key = 0;
				for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
					if (entry.getValue().size() > max) {
						max = entry.getValue().size();
						key = entry.getKey();
					} // end of if
				} // end of for
				largestCComponent = ccVertexSets.get(key);
				thematicSemanticGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(largestCComponent);
				addLabelToGraphVertices(thematicSemanticGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
//				System.out.println("\nTotal time to construct the Thematic Graph: " + (System.currentTimeMillis() - stTime));
				thematicSemanticGraph.setVertexWeightProperty(semanticGraph.getVertexWeightProperty());
//				System.out.println("Creating \"thematicGraph.dot\" file on disk...");
//				dotFile = new FileWriter("thematicGraph.dot");
//				dotFile.write(thematicSemanticGraph.toDot());
//				dotFile.close();
//				System.out.println("\"thematicGraph.dot\" file Successfully created.\n");
				int[] vertices = thematicSemanticGraph.getVertices().toIntArray();
				runHITSalgorithm(thematicSemanticGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
				Set<Integer> authoritativeEntities = new HashSet<Integer>(100);
				Set<Integer> nonAuthEntities = new HashSet<Integer>(100);
				Set<Integer> allEntities = new HashSet<Integer>(100);
				//				System.out.println("============== CC Vertices Weights Start ==============\n");
				int i = 1;
				for (int vertex : vertices) {
					String ename = localNames.get(vertex);
					double vw = thematicSemanticGraph.getVertexWeight(vertex);
					if (vw >= authoritativeThreshold) {
						authoritativeEntities.add(vertex);
					} else {
						nonAuthEntities.add(vertex);
					} // end of if
					//					System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
					//					i++;
				} // end of for
				//				System.out.println("============== CC Vertices Weights End ==============\n");
				allEntities.addAll(authoritativeEntities);
				allEntities.addAll(nonAuthEntities);
				//				System.out.println("============== Authoritative Entities Start ==============\n");
				i = 1;
				List<Integer> sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicSemanticGraph.getVertexWeightProperty());
//				for (int vid : sortedAuth) {
//					String ename = localNames.get(vid);
//					double vw = thematicSemanticGraph.getVertexWeight(vid);
//					//					System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
//					//					i++;
//				}
				//				System.out.println("\n============== Authoritative Entities End ==============\n");
				SemanticGraph thematicSemanticGraphClone = (SemanticGraph) thematicSemanticGraph.clone();
				thematicSemanticGraphClone.getClass().getClassLoader().setDefaultAssertionStatus(false);
				// removeLowWeightVerticies(thematicSemanticGraph,
				// nonAuthEntities);
				removeLowWeightVerticies(thematicSemanticGraphClone, nonAuthEntities);
//				dotFile = new FileWriter("thematicGraph2.dot");
//				dotFile.write(thematicSemanticGraphClone.toDot());
//				dotFile.close();
				// largestCComponent = thematicSemanticGraph.getLargestConnectedComponent();
				largestCComponent = thematicSemanticGraphClone.getLargestConnectedComponent();
				// SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraph.getSubgraphInducedByVertices(largestCComponent);
				if (largestCComponent != null){
					SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraphClone.getSubgraphInducedByVertices(largestCComponent);
					thematicGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
					addLabelToGraphVertices(thematicGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
					thematicGraph.setVertexWeightProperty(thematicSemanticGraph.getVertexWeightProperty());
					vertices = thematicGraph.getVertices().toIntArray();
					runHITSalgorithm(thematicGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
					authoritativeEntities = new HashSet<Integer>(100);
					nonAuthEntities = new HashSet<Integer>(100);
//					System.out.println("============== CC Vertices Weights Start ==============\n");
					i = 1;
					for (int vertex : vertices) {
						String ename = localNames.get(vertex);
						double vw = thematicGraph.getVertexWeight(vertex);
						if (vw >= authoritativeThreshold) {
							authoritativeEntities.add(vertex);
						} else {
							nonAuthEntities.add(vertex);
						} // end of if
//						System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
//					System.out.println("============== CC Vertices Weights End ==============\n");
//					System.out.println("============== Authoritative Entities Start ==============\n");
					i = 1;
					sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicGraph.getVertexWeightProperty());
//					for (int vid : sortedAuth) {
//						String ename = localNames.get(vid);
//						double vw = thematicGraph.getVertexWeight(vid);
//						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
//						i++;
//					} // end of for
//					System.out.println("\n============== Authoritative Entities End ==============\n");
					
					Map<Integer, Double> centralEntities = getCentralEntities((SemanticGraph) thematicGraph.toUndirectedGraph(), authoritativeEntities, centralityThreshold);
//					System.out.println("============== Central Entities Start ==============\n");
					i = 1;
					List<Integer> sortedCentral = sortBasedOnScore(centralEntities);
					for (int vid : sortedCentral) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
//						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
//					System.out.println("\n============== Central Entities End ==============\n");

					
					Set<Integer> coreEntities = new HashSet<Integer>();
					if (topAuthoritativeEntities >= sortedAuth.size()) {
						coreEntities.addAll(sortedAuth);
					}else {
						for (int counter = 0;counter < sortedAuth.size(); counter++) {
							if (counter == topAuthoritativeEntities) {
								break;
							}else {
								coreEntities.add(sortedAuth.get(counter));
							} // end of if
						} // end of for
					} // end of if
					if (topCentralEntities >= sortedCentral.size()) {
						coreEntities.addAll(sortedCentral);
					}else {
						for (int counter = 0;counter < sortedCentral.size(); counter++) {
							if (counter == topCentralEntities) {
								break;
							}else {
								coreEntities.add(sortedCentral.get(counter));
							} // end of if
						} // end of for
					} // end of if

//					System.out.println("============== Core Entities Start ==============\n");
					i = 1;
//					for (int vid : coreEntities) {
//						String ename = localNames.get(vid);
//						double vw = thematicGraph.getVertexWeight(vid);
//						double centrality = centralEntities.get(vid);
////						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
//						i++;
//					} // end of for
//					System.out.println("\n============== Core Entities End ==============\n");
					//					IntSet vList = thematicSemanticGraph.getVertices();
					IntSet vList = semanticGraph.getVertices();
					for (String c : topRWScoreConcepts) {
						int cid = wikipediaEntities.get(c);
						if (vList.contains(cid)) {
							coreEntities.add(cid);
						} // end of if
					} // end of for
					//					show(new ArrayList<Integer> (coreEntities), entityCategoryMap);
					// Find Core and Context Entities for constructing the taxonomy Graph
					//					Set<Integer> contextEntities = findCoreContextEntities(thematicSemanticGraph, coreEntities);
					Set<Integer> contextEntities = findCoreContextEntities(semanticGraph, coreEntities);

					// It is used to create Wikipedia Taxonomy Graph for ONLY categories of core entities 
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (coreEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of ALL entities
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (allEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of Core and Context entities
					SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap, categoryhierarchyLevel);
//					dotFile = new FileWriter("taxonomyGraph.dot");
//					dotFile.write(taxonomyGraph.toDot());
//					dotFile.close();

					LabelProperty vlp = taxonomyGraph.getVertexLabelProperty();
					taxonomyGraph = (SemanticGraph) taxonomyGraph.toUndirectedGraph();
					int [ ] taxonomyVerticies = taxonomyGraph.getVertices().toIntArray();
//					System.out.println("TaxonomyVerticies size: " + taxonomyVerticies.length);
//					int [ ] vlabelIds = vlp.getIDs().toIntArray();
					Map<Integer,Integer> categoryEntityLinkMap = new HashMap<Integer,Integer>();
					for(int catId : taxonomyVerticies) {
						if (!contextEntities.contains(catId)) {
							String categoryName = vlp.getValue(catId);
							int numOfLinks = findNumberOfCategoryEntities(categoryName, virtGraph);
							categoryEntityLinkMap.put(catId, numOfLinks);
						} // end of if
					} // end of for

//					System.out.println("Processing links... done!");

					// This approach is same as previous one, but ONLY for top K categories, and ONLY for core entities 
					Map<Integer,Double> categoryScoreMap = new HashMap<Integer,Double>();
					Map<Integer,Integer> categoryCoverageMap = new HashMap<Integer,Integer>();
					for (int entId : contextEntities) {  // ONLY for core entities 
						//					for (int entId : allEntities) {   // for all entities
						BFSResult bfsTree = taxonomyGraph.bfs(entId);
						for (int catId : taxonomyVerticies) {
							if (!contextEntities.contains(catId)) {
								String category = vlp.getValue(catId);
								calculateCategoryScore(taxonomyGraph, bfsTree, entId, catId, category, categoryScoreMap, categoryCoverageMap);
							}
						} // end of for
					} // end of for
					Set<Integer> keySet = categoryScoreMap.keySet();
					for (int id : keySet) {
						double score1 = categoryScoreMap.get(id);
						double score2 = categoryCoverageMap.get(id);
						double finalScore = (alpha * score1) + (1 - alpha) * score2;
						categoryScoreMap.put(id, finalScore);
					} // end of for
					List<Integer> sortedCats = sortCategoryIdBasedOnScore(categoryScoreMap);
					List<Integer> sortedCatsSubList = new ArrayList<Integer>();
					Set<Integer> topCategories = new HashSet<Integer>();
					Set<String> topCategoriesNames = new HashSet<String>();
					int k = 1;
//					System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					for (int catId : sortedCats) {
						if (categoryEntityLinkMap.get(catId) >= MaxNumOfLinksPerCategory) {
							continue;
						}else {
							String catName = vlp.getValue(catId);
							topCategoriesNames.add(catName);
//							System.out.println("k: " + k + " Category: " + catName + "   Score: " + (categoryScoreMap.get(catId) / categoryScoreMap.get(sortedCats.get(0))) + " Coverage: " + categoryCoverageMap.get(catId) + " links: " + categoryEntityLinkMap.get(catId));
							topCategories.add(catNamesToInt.get(catName));
							sortedCatsSubList.add(catId);
							k++;
							if (k == 31) {
								break;
							}
						} // end of if
					} // end of for
//					System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");
					
					
					

					List<String> userCatNames = parseUserCategories(userCats);
					List<Integer> userCatIds = new ArrayList<Integer>();
					Map<String,Double> catBelongingnessMap = new HashMap<String,Double>();
					for (String c : userCatNames) {
						userCatIds.add(catNamesToInt.get(c));
					} // end of for
					List<Double>  finalAvgRelatedness = new ArrayList<Double>();
					List<String>  intersectionCatNames = null;
					String logMsg = "";
					for (int counter = 0;counter < userCatIds.size();counter++) {
						Set<Integer> parentGraph = CategoryProcessor.categoryTreeFromChildToParent(wikiTaxonomyMap_ID_FromChildToParent, userCatIds.get(counter));
						Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, userCatIds.get(counter), parentGraph);
						//					Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, catId1);
						Set<Integer> intersection = CategoryProcessor.getIntersection(userCategory, topCategories);
						intersectionCatNames = new ArrayList<String>();
//						System.out.println("\n=============== Intersection Categories Start ===============\n ");
						for (int id : intersection) {
							intersectionCatNames.add(catNames.get(id));
//							System.out.println("catId: " + userCatNames.get(counter) + "   CatName: " + catNames.get(id));
						} // end of for
//						System.out.println("\n=============== Intersection Categories End   =============== ");
						Map<Integer,Double> finalSRelatednessMap = new HashMap<Integer,Double>();
						Map<Integer,Set<Integer>> coreEntityLinks = new HashMap<Integer,Set<Integer>>(coreEntities.size());
						//					for (int entId : contextEntities) {
						for (int entId : coreEntities) {
							String entName = localNames.get(entId);
							coreEntityLinks.put(entId, findLinkedEntities(entName, virtGraph));
						} // end of for
						if (intersection.isEmpty()) {
							intersectionCatNames.add(userCatNames.get(counter));
//							System.out.println("catId: " + counter + "   CatName: " + userCatNames.get(counter));
						} // end of if
						for (String category : intersectionCatNames) {
							Map<Integer,Double> sRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, coreEntities, coreEntityLinks, virtGraph);
							//						Map<Integer,Double> sRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, contextEntities, coreEntityLinks, virtGraph);
//							System.out.println("\n=============== Semantic Relatedness of Core Entities and category \"" + category +"\" Start ===============\n");
							for (Map.Entry<Integer, Double> entry : sRelatednessMap.entrySet()) {
								int entId = entry.getKey();
								double sr = entry.getValue();
//								System.out.println("entId: " + entId + "   entity: " + localNames.get(entId) + "    sr: " + sr);
								double preVal = finalSRelatednessMap.get(entId) != null ? finalSRelatednessMap.get(entId) : 0;
								finalSRelatednessMap.put(entId, Math.max(preVal, sr));
								//							avgSRelatednessMap.put(entId, preVal + sr);
							} // end of for
//							System.out.println("\n=============== Semantic Relatedness of Core Entities and category \"" + category +"\" End   ===============\n");
						} // end of for
//						System.out.println("\n\n\n************ FINAL Semantic Relatedness of Core Entities ************\n ");
						logMsg += "************ FINAL Semantic Relatedness to \"" + userCatNames.get(counter) + "\" ************\n";
//						logger.log(Level.INFO, "************ FINAL Semantic Relatedness of Core Entities ************");
						List<Integer> sortedRelatedness = sortBasedOnScore(finalSRelatednessMap);

						// pick the top K semantic relatedness and get the average as the relatedness of the document to user's category of interest
						List<Integer> sortedTopKRelatedness = sortedRelatedness.size() >= 9 ? new ArrayList<Integer>(sortedRelatedness.subList(0, 9)) : sortedRelatedness;
						for (int entId : sortedRelatedness) {
							logMsg += "Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId) + "\n";
//							System.out.println("entId: " + entId + "  Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId));
//							logger.log(Level.INFO,"entId: " + entId + "  Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId));
						} // end of for
						double sumOfSr = 0;
						for (int entId : sortedTopKRelatedness) {
							sumOfSr += finalSRelatednessMap.get(entId);
						} // end of for
						double avgSR = sumOfSr /sortedTopKRelatedness.size(); 
//						System.out.println("\n\nAVERAGE Relatedness of Document to User's Category of Interest is: " + avgSR);
//						logger.log(Level.INFO,"AVERAGE Relatedness of Document to User's Category of Interest is: " + avgSR);
						finalAvgRelatedness.add(avgSR);
						double srThreshold = 0.25;
						if (avgSR >= srThreshold) {
//							System.out.println("\n*************** Document belongs to the category \"" + userCatNames.get(counter) + "\"");
							int prval = catBatchResult.get(userCatNames.get(counter)) != null ? catBatchResult.get(userCatNames.get(counter)) : 0;
							catBatchResult.put(userCatNames.get(counter), prval + 1);
//							logger.log(Level.INFO,"File: " + fileCounter + "   " +  fileName + "*************** Document belongs to the category \"" + userCatNames.get(counter) + "\"");
						}else {
//							System.out.println("\n*************** Document does NOT belong to the category \"" + userCatNames.get(counter) + "\"");
//							logger.log(Level.INFO,"File: " + fileCounter + "   " +  fileName + "*************** Document does NOT belong to the category \"" + userCatNames.get(counter) + "\"");
						} // end of if
						logger.log(Level.INFO, "\n" + logMsg);
						logMsg = "";
					} // end of for userCatIds
					for (int c = 0 ;c < userCatNames.size();c++) {
						logMsg += "File: " + fileCounter + "   " +  fileName + " SR to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c) + "\n";
						catBelongingnessMap.put(userCatNames.get(c), finalAvgRelatedness.get(c));
//						logger.log(Level.INFO,"File: " + fileCounter + "   " +  fileName + " SR to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c));
//						System.out.println("\n\n Relatedness of Document to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c));
					} // end of for
					double catComRelatedness = processCategoryCombination(userCats, catBelongingnessMap);
					logger.log(Level.INFO, "\n" + logMsg);
					logger.log(Level.INFO, "\n" + "Final Combination Relatedness: " + catComRelatedness);
					logger.log(Level.INFO, fileCounter + " files processed.");
					fileCounter++;
				} // end of if (largestCComponent != null)
				file.renameTo(new File(RSS_DIR_PATH + fileName + "_done"));
			} // end of for listFile
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING, e.getMessage());
//			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage());
//			e.printStackTrace();
		}
			
	} // end of recognizeEntites_ParseCombination
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*********************************************************
	 * @param userCats
	 * @param catBelongingnessMap
	 * @return
	 */
	public double processCategoryCombination(String userCats, Map<String, Double> catBelongingnessMap) {
		String [] tokens = userCats.split(" ");
		List<Double> maxL = new ArrayList<Double>();
		for (String s : tokens) {
			String [] inntok = s.split(","); 
			if (inntok.length > 1) {
				List<Double> minL = new ArrayList<Double>();
				for (String t : inntok) {
					double val = 0;
					if (t.indexOf('!') != -1) {
						val = catBelongingnessMap.get(t.substring(1));
						val = 1 - val;
					}else {
						val = catBelongingnessMap.get(t);
					} // end of if
					minL.add(val);
				} // end of for (String t : inntok)
				maxL.add(findMin(minL));
			} else {
				double val = 0;
				if (s.indexOf('!') != -1) {
					val = catBelongingnessMap.get(s.substring(1));
					val = 1 - val;
				}else {
					val = catBelongingnessMap.get(s);
				} // end of if
				maxL.add(val);
			} // end of if
		} // end of for (String s : tokens)
		double r = findMax(maxL);
		System.out.println("r: " + r);
		return 0;
	} // end of processCategoryCombination



	/*********************************************************
	 * @param vals
	 * @return
	 */
	private double findMax(List<Double> vals) {
		double max = vals.get(0);
		for (double n : vals) {
			if (n > max) {
				max = n;
			} // end of if
		} // end of for
		return max;
	} // end of findMax
	
	
	public double findMin(List<Double> vals) {
		double min = vals.get(0);
		for (double n : vals) {
			if (n < min) {
				min = n;
			} // end of if
		} // end of for
		return min;
	} // end of findMin



	/*********************************************************
	 * @param userCats
	 * @return
	 */
	public List<String> parseUserCategories(String userCats) {
		List<String> resultCats = new ArrayList<String>();
		String [] tokens = userCats.split(" ");
		for (String s : tokens) {
			String [] innerCats = s.split(",");
			for (String innCat : innerCats) {
				if (innCat.indexOf('!') != -1) {
					innCat = innCat.substring(1);
					if (!resultCats.contains(innCat)) {
						resultCats.add(innCat);
					}
				}else {
					if (!resultCats.contains(innCat)) {
						resultCats.add(innCat);
					}
				} // end of if
			} // end of for (String innCat)
		} // end of for (String s)
		return resultCats;
	} // end of parseUserCategories
	
	
	public void findEntityCategoriesFromFile() {
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		Map<String,String> yagoTaxanomy = DbProcessor.loadYagoTaxonomyMapToMemory();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String OUTPUT_DIR = "/home/mehdi/otm/entcatfiles/";
			RSS_DIR_PATH =  "/home/mehdi/otm/entfiles/";
			File rssDirectory = new File(RSS_DIR_PATH);
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(RSS_DIR_PATH + fileName);
				String [ ] entities = document.split(",");
				Set<String> cats = new HashSet<String>();
				Set<String> yagocats = new HashSet<String>();
				for (String entity : entities) {
					entity = entity.trim();
					cats.addAll(getEntityCategoriesList(virtGraph, entity));
				} // end of for
				for (String cat : cats) {
					String yagoCatname = "wikicategory_" + cat;
					if (yagoTaxanomy.get(yagoCatname) != null) {
						yagocats.add(yagoCatname);
					}
				} // end of for
				FileWriter fw = new FileWriter(OUTPUT_DIR + fileName.replace(".txt_c", "") + ".txt");
//				String line = cats.toString().replace("[", "").replace("]", "");
				String line = yagocats.toString().replace("[", "").replace("]", "");
				fw.write(line);
				fw.close();
			} // end of for (File file)
		}catch(Exception e) {
			e.printStackTrace();
		}
	} // end of findEntityCategoriesFromFile
	
	
	public void createConceptsReferingToEntityFromFile() {
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String OUTPUT_DIR = "/home/mehdi/otm/relatingconcepts/";
			String INPUT_DIR =  "/home/mehdi/samplefiles/";
			File inputDirectory = new File(INPUT_DIR);
			int i = 0;
			for (File file : inputDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(INPUT_DIR + fileName);
				String [ ] entities = document.split(",");
				for (String entity : entities) {
					entity = entity.trim();
					Set<String> relatingConcepts = getRelationsReferingToEntity(virtGraph, entity);
					FileWriter fw = new FileWriter(OUTPUT_DIR + entity.replace('/', '_') + ".txt");
					String line = relatingConcepts.toString().replace("[", "").replace("]", "");
					fw.write(line);
					fw.close();
				} // end of for
				System.out.println("File " + (++i) + " done.");
			} // end of for (File file)
		}catch(Exception e) {
			e.printStackTrace();
		}
	} // end of createConceptsReferingToEntityFromFile

	
	
	public void createEntityrelatedConceptsFromFile() {
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String OUTPUT_DIR = "/home/mehdi/otm/relatedconcepts/";
			RSS_DIR_PATH =  "/home/mehdi/samplefiles/";
			File rssDirectory = new File(RSS_DIR_PATH);
			int i = 0;
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(RSS_DIR_PATH + fileName);
				String [ ] entities = document.split(",");
				for (String entity : entities) {
					entity = entity.trim();
					Set<String> vocabulary = getEntityNonLiteralRelationList(virtGraph, entity);
//					Set<String> vocabulary = getEntityLiteralRelationList(virtGraph, entity);
//					vocabulary.addAll(getEntityNonLiteralRelationList(virtGraph, entity));
					FileWriter fw = new FileWriter(OUTPUT_DIR + entity.replace('/', '_') + ".txt");
					String line = vocabulary.toString().replace("[", "").replace("]", "");
					fw.write(line);
					fw.close();
				} // end of for
				System.out.println("File " + (++i) + " done.");
			} // end of for (File file)
		}catch(Exception e) {
			e.printStackTrace();
		}
	} // end of createEntityrelatedConceptsFromFile
	
	
	public void createEntityBidirectionalRelatedConceptsFromFile() {
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		
		// We would like to filter out some irrelevant entities //
		String [ ] domainEntities = readDocument("/home/mehdi/domainEntities/allConcepts.txt").split(",");
		Set<String> allDomainEntities = new HashSet<String>();
		for (String ent : domainEntities) {
			allDomainEntities.add(ent.trim());
		} // end of for
		
		try {
			String INPUT_DIR =  "/home/mehdi/samplefiles/";
			String OUTPUT_DIR = "/home/mehdi/otm/relatedconcepts/";
			File rssDirectory = new File(INPUT_DIR);
			Set<String> allCorpusEntities = new HashSet<String>();
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(INPUT_DIR + fileName);
				String [ ] entities = document.split(",");
//				allCorpusEntities.addAll(Arrays.asList(entities));
				for (String ent : entities) {
					allCorpusEntities.add(ent.trim());
				} // end of for
			} // end of for
			int i = 0;
			System.out.println("All Corpus Entities: " + allCorpusEntities.size());
			for (String entity : allCorpusEntities) {
				if (entity.indexOf('?') != -1 || entity.indexOf('.') != -1  || entity.indexOf('/') != -1 || entity.indexOf('%') != -1 || entity.indexOf('\'') != -1 || entity.indexOf(":") != -1 || entity.indexOf('\"') != -1 || entity.indexOf('-') != -1 || entity.indexOf('^') != -1 || entity.startsWith("_") || entity.startsWith(".")) {
					continue;
				}
				
				// If interesed in filtering out some of the corpus entities add following condition
//				if (!allDomainEntities.contains(entity)) {
//					continue;
//				}
				
//				entity = entity.trim();
				Set<String> relatedEntities = getEntityNonLiteralRelationList(virtGraph, entity);
				relatedEntities.addAll(getRelationsReferingToEntity(virtGraph, entity));
				// add the entity itself to its related entity list
//				relatedEntities.add(entity);
				Set<String> refinedRelatedEntities = new HashSet<String>();
				// Only pick the related entities that exist in the document corpus
				for (String e : relatedEntities) {
					if (allCorpusEntities.contains(e) && allDomainEntities.contains(e)) {
						if (e.indexOf('?') != -1 || e.indexOf('.') != -1  || e.indexOf('/') != -1 || e.indexOf('%') != -1 || e.indexOf('\'') != -1 || e.indexOf(":") != -1 || e.indexOf('\"') != -1 || e.indexOf('-') != -1 || e.indexOf('^') != -1 || e.startsWith("_") || e.startsWith(".")) {
							continue;
						}else {
							refinedRelatedEntities.add(e);
						} // end of if
					} // end of if
				} // end of for
//				String line = relatedEntities.toString().replace("[", "").replace("]", "");
				if (refinedRelatedEntities.size() > 0) {
					FileWriter fw = new FileWriter(OUTPUT_DIR + entity.replace('/', '_') + ".txt");
					String line = refinedRelatedEntities.toString().replace("[", "").replace("]", "");
					fw.write(line);
					fw.close();
				}else {
//					System.out.println("No related entities for " + entity);
				}
//				i++;
				System.out.println("Entity " + (++i) + " done.");
			} // end of for
//			System.out.println( i + " files done.");
		}catch(Exception e) {
			e.printStackTrace();
		}
	} // end of createEntityBidirectionalRelatedConceptsFromFile
	
	public void downloadWikipediaPagesForCorpusConcepts() {
		String wiki_URL = "http://en.wikipedia.org/wiki/";
		Document document = null;
		Connection conn = null;
		// We would like to filter out some irrelevant entities //
		String [ ] domainEntities = readDocument("/home/mehdi/domainEntities/allConcepts.txt").split(",");
		Set<String> allDomainEntities = new HashSet<String>();
		for (String ent : domainEntities) {
			allDomainEntities.add(ent.trim());
		} // end of for
		String CONCEPTS_DIR_PATH = "/home/mehdi/samplefiles/";
//		String CONCEPTS_DIR_PATH = "/home/mehdi/otm/relatedconcepts/";
		File conceptsDirectory = new File(CONCEPTS_DIR_PATH);
		int i = 0;
		int minNumOfWords = 200;
		Set<String> existingEntities = new HashSet<String>();
		for (File ifile : conceptsDirectory.listFiles()) {
			String fileName = ifile.getName();
			String doc = readDocument(CONCEPTS_DIR_PATH + fileName);
//			String [] entities = doc.split(",");
			Set<String> entities = new HashSet<String>(Arrays.asList(doc.split(",")));
//			entities.add(fileName.replace(".txt", ""));
			for (String entity : entities) {
				entity = entity.replace('/', '_').trim();
				if (existingEntities.contains(entity)) {
					continue;
				}
				
				// if interested in filtering out some corpus entities use following condition
//				if (existingEntities.contains(entity) || !allDomainEntities.contains(entity)) {
//					continue;
//				}
				
				existingEntities.add(entity);
				try {
					conn = Jsoup.connect(wiki_URL + entity.trim());
					document = conn.get();
					Elements content = document.select("#mw-content-text p");
					//				System.out.println(content.text());
					String [] words = content.text().split(" ");
					if (words.length > minNumOfWords){
						File file = new File("/home/mehdi/otm/wikipediaPages/" + entity.trim() + ".txt");
						FileWriter fileWriter = new FileWriter(file);
						fileWriter.write(content.text());
						fileWriter.flush();
						fileWriter.close();
					}
					conn = null;
				} catch (IOException e) {
					System.out.println(e.getMessage() + " ==> " + entity.trim());
				}
			} // end of for
			System.out.println("File " + (++i) + " done.");
		} // end of for
	} // end of downloadWikipediaPagesForCorpusConcepts

	
//	public void downloadWikipediaPagesForConcepts() {
//		String wiki_URL = "http://en.wikipedia.org/wiki/";
//		Document document = null;
//		Connection conn = null;
//		String CONCEPTS_DIR_PATH = "/home/mehdi/otm/relatingconcepts/";
//		File conceptsDirectory = new File(CONCEPTS_DIR_PATH);
//		int i = 0;
//		for (File ifile : conceptsDirectory.listFiles()) {
//			String fileName = ifile.getName();
//			String doc = readDocument(CONCEPTS_DIR_PATH + fileName);
//			Set<String> entities = new HashSet<String> (Arrays.asList(doc.split(",")));
//			entities.add(fileName.replace(".txt", ""));
//			for (String entity : entities) {
//				try {
//					conn = Jsoup.connect(wiki_URL + entity.trim());
//					document = conn.get();
//					Elements content = document.select("#mw-content-text p");
//					//				System.out.println(content.text());
//					File file = new File("/home/mehdi/otm/wikipediaRelatingPages/" + entity.trim() + ".txt");
//					FileWriter fileWriter = new FileWriter(file);
//					fileWriter.write(content.text());
//					fileWriter.flush();
//					fileWriter.close();
//					conn = null;
//				} catch (IOException e) {
//					System.out.println(e.getMessage() + " " + entity.trim());
//				}
//			} // end of for
//			System.out.println("File " + (++i) + " done.");
//		} // end of for
//	} // end of downloadWikipediaPagesForConcepts



	public void createEntityFiles() {
		graphNodes = loadDeterministicGraphToMemory();
		failureMap = loadFailureFunctionMapToMemory();
		outputSet = loadOutputMapToMemory();
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		Map<String, Set<Entity>> uniqueNames = DbProcessor.loadUniqueNameListToMemory();
		LocalNameMapperClass localNamesMapperClass = DbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
		localNames = localNamesMapperClass.getLocalNameList();
		namesToInt = localNamesMapperClass.getNameToInt();
		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		Set<String> stopWords = readStopWords();
		Set<String> timeRelatedWords = readTimeRelatedEntities();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String OUTPUT_DIR = "/home/mehdi/entfiles/";
//			String OUTPUT_DIR = "/home/mehdi/samplefiles/";
//			FileHandler fh = new FileHandler(OUTPUT_DIR + "ent.log");
//			fh.setFormatter(new SimpleFormatter());
//			logger.addHandler(fh);
//			logger.setLevel(Level.INFO);
			File rssDirectory = new File(RSS_DIR_PATH);
			JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
			JsonParser jsonParser = new JsonParser();
			JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
			int numOfEntityMatchAttr = confObject.get("numOfEntityMatchAttr").getAsInt();
			JsonObject entityMatchAttrWeightObj = confObject.get("entityMatchAttrWeight").getAsJsonObject();
			int fcounter = 0;
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(RSS_DIR_PATH + fileName);
//				System.out.println(fileName);
				if (document.equals("")) {
					continue;
				}
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
				SemanticGraph semanticGraph = new SemanticGraph();
				semanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				LabelProperty vertexLabelProperty = semanticGraph.getVertexLabelProperty();
				int intialCapacity = confObject.get("initialCapacity").getAsInt();
				List<String> initialEntities = new ArrayList<String>(4 * intialCapacity);
				SentenceDetector sd = null;
				try {
					sd = new SentenceDetector();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				List<String> sentenceList = sd.getAllSentencesFromDocument(document);

				//				System.out.println("============> topRWScoreWords: " +  topRWScoreWords + "\n");
				Set<String> topRWScoreConceptsMentions = new HashSet<String>();
				Set<String> topRWScoreConcepts = new HashSet<String>();
				int state = 0;
				String preEntity = "-1";
				String curEntity = "-2";
				Set<String> foundEntities = new HashSet<String>(intialCapacity);
				entityRepetition = new HashMap<String, Integer>(intialCapacity);
				entityCategoryMap = new HashMap<Integer, Set<String>>(intialCapacity);
				for (String sentence : sentenceList) {
					sentence = sentence.toLowerCase();
					String[] sentcWords = sentence.split(" ");
					for (int wordCnt = 0; wordCnt < sentcWords.length; wordCnt++) {
						String currentWord = sentcWords[wordCnt];
//						currentWord = currentWord.replace("'s", "").replace("'S", "").replace("'t", "").replace("$", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace("\"", "").replace("--", "").replace("(", "").replace(")", "");
						currentWord = currentWord.replace("'s", "").replace("'S", "").replace("'t", "").replaceAll("[^\\dA-Za-z ]", "").replaceAll("\\s+", "").replaceAll("[0-9]", "");
//						currentWord = currentWord.replaceAll("[0-9]", "");
						/*
						if (stopWords.contains(currentWord) || currentWord.contains("[")) {
							continue;
						} // end of if
						if (currentWord.length() < 3) {
							continue;
						} // end of if
						 */
						int wordId = wordToIntMap.get(currentWord) != null ? wordToIntMap.get(currentWord) : -1;
						if (wordId != -1) {
							while (g(state, wordId) == FAIL) {
								state = f(state);
							} // end of while
							state = g(state, wordId);
							List<String> entities = output(state);
							if (entities != null) {
								curEntity = entities.get(0);
								String[] parts = curEntity.split(" ");
								while (true) {
									if (stopWords.contains(parts[0])) {
										curEntity = curEntity.substring(parts[0].length()).trim();
										parts = curEntity.split(" ");
									} else {
										break;
									} // end of if
								} // end of while
								if (curEntity.length() > 0) {
									if (curEntity.contains(preEntity)) {
										foundEntities.remove(preEntity);
										foundEntities.add(curEntity);
										preEntity = curEntity;
									} else {
										foundEntities.add(curEntity);
										preEntity = curEntity;
									} // end of if
									preEntity = curEntity;
								} // end of if
								int repetition = entityRepetition.get(curEntity) != null ? entityRepetition.get(curEntity) : 0;
								repetition++;
								entityRepetition.put(curEntity, repetition);
								initialEntities.addAll(entities);
							} // end of if
						} // end of if (wordId != -1)
					} // end of for
				} // end of for
				// Set<String> foundEntities = new
				// HashSet<String>(fentities);
//				System.out.println("Total Entity Spotter time: " + (System.currentTimeMillis() - stTime));
				//				System.out.println("\n=============== Recognized Entities Start ================");
				//				System.out.println("Size: " + initialEntities.size());
				//				System.out.println(initialEntities);
				//				System.out.println("=============== Recognized Entities End   ================\n");
				// System.out.println("Number of Entities BEFORE removing Stop Words: "
				// + foundEntities.size() + "\n");
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
//				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				// System.out.println("Number of Entities AFTER removing Stop Words: "
				// + foundEntities.size() + "\n");
//				System.out.println("\n=============== Revised Recognized Entities Start ================");
//				System.out.println("Size: " + foundEntities.size());
//				System.out.println(foundEntities);
//				System.out.println("\n=============== Revised Recognized Entities End   ================\n");
				
				
				FileWriter fw = new FileWriter(OUTPUT_DIR + fileName + "_e");
				String line = foundEntities.toString().replace("[", "").replace("]", "");
				fw.write(line);
				fw.close();

				
				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
				int ecounter = 0;
				if (numOfEntityMatchAttr == 1) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if (enSet != null )
					} // end of while
				} else if (numOfEntityMatchAttr == 2) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} // end of if
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 3) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 4) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 5) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 6) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					double disambiguationNameShorWeight = entityMatchAttrWeightObj.get("disambiguationNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5' || et.getAttr() == '6') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '6') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameShorWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} // end of else if
				
				
				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);
				SemanticGraph thematicSemanticGraph = new SemanticGraph();
				thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				IntSet largestCComponent = null;
				int max = 0;
				int key = 0;
				for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
					if (entry.getValue().size() > max) {
						max = entry.getValue().size();
						key = entry.getKey();
					} // end of if
				} // end of for
				
				// Create entity files ONLY for Largest Connected Component concepts
				
				// ONLY considers the vertices in the Largest Connceted Component
//				largestCComponent = ccVertexSets.get(key);
//				thematicSemanticGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(largestCComponent);
//				addLabelToGraphVertices(thematicSemanticGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
//				thematicSemanticGraph.setVertexWeightProperty(semanticGraph.getVertexWeightProperty());
//				int[] vertices = thematicSemanticGraph.getVertices().toIntArray();
				
				// considers all the vertices in the semantic graph
				int[] vertices = semanticGraph.getVertices().toIntArray();
				Set<String> largestCComponentEntities = new HashSet<String>(100);
				for (int vertex : vertices) {
					largestCComponentEntities.add(localNames.get(vertex));
				} // end of for
				fw = new FileWriter(OUTPUT_DIR + fileName + "_c");
				line = largestCComponentEntities.toString().replace("[", "").replace("]", "");
				fw.write(line);
				fw.close();
				System.out.println(++fcounter);
			} // end of for (File file)
		}catch(Exception e) {
			e.printStackTrace();
		}
	} // end of createEntityFiles
	
	
	
	public void recognizeEntites_Batch() {
		long stTime = System.currentTimeMillis();
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		graphNodes = loadDeterministicGraphToMemory();
		failureMap = loadFailureFunctionMapToMemory();
		outputSet = loadOutputMapToMemory();
		Map<String, Set<Entity>> uniqueNames = DbProcessor.loadUniqueNameListToMemory();
		LocalNameMapperClass localNamesMapperClass = DbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
		localNames = localNamesMapperClass.getLocalNameList();
		namesToInt = localNamesMapperClass.getNameToInt();
		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		// yagoTaxonomyMap = DictionaryGenerator.loadYagoTaxonomyMapToMemory();
		wikiTaxonomyMapFromChildToParent = DbProcessor.loadWikiTaxonomyMapFromChildToParentToMemory();
		wikiTaxonomyMap_ID_FromChildToParent = DbProcessor.loadWikiTaxonomyMap_ID_FromChildToParentToMemory();
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
		wikiTaxonomyMapFromParentToChild = DbProcessor.loadWikiTaxonomyMapFromParentToChildToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> catNames = catNameMapper.getCatNameList();
		System.out.println("Total Load Time: " + (System.currentTimeMillis() - stTime));
		// List<String> wlist = wordMapperClass.getWordList();
		Set<String> stopWords = readStopWords();
		Set<String> timeRelatedWords = readTimeRelatedEntities();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_sportslog.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_technologylog.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_entertainment.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_business.log");
			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_politics.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_health.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_science.log");
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "reuters_arts.log");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			File rssDirectory = new File(RSS_DIR_PATH);
			JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
			JsonParser jsonParser = new JsonParser();
			JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
			int numOfEntityMatchAttr = confObject.get("numOfEntityMatchAttr").getAsInt();
			JsonObject entityMatchAttrWeightObj = confObject.get("entityMatchAttrWeight").getAsJsonObject();
			String userCats = confObject.get("categories").getAsString();
			int categoryhierarchyLevel = confObject.get("categoryhierarchyLevel").getAsInt();
			double alpha = confObject.get("alpha").getAsDouble();
			JsonObject relationImportanceWeightObj = confObject.get("relationImportanceWeight").getAsJsonObject();
			double authoritativeThreshold = confObject.get("authoritativeThreshold").getAsDouble();
			int numOfSteps = confObject.get("numOfSteps").getAsInt();
			double centralityThreshold = confObject.get("centralityThreshold").getAsDouble();
			int topAuthoritativeEntities = confObject.get("topK-AuthoritativeEntities").getAsInt();
			int topCentralEntities = confObject.get("topK-CentralEntities").getAsInt();
			int fileCounter = 1;
			Map<String,Integer> catBatchResult = new HashMap<String,Integer>(); 
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(RSS_DIR_PATH + fileName);
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
				SemanticGraph semanticGraph = new SemanticGraph();
				semanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				LabelProperty vertexLabelProperty = semanticGraph.getVertexLabelProperty();
				int intialCapacity = confObject.get("initialCapacity").getAsInt();
				List<String> initialEntities = new ArrayList<String>(4 * intialCapacity);
				SentenceDetector sd = null;
				try {
					sd = new SentenceDetector();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				List<String> sentenceList = sd.getAllSentencesFromDocument(document);
				Set<String> topRWScoreWords = randomWalkScores(sentenceList, stopWords);

				//				System.out.println("============> topRWScoreWords: " +  topRWScoreWords + "\n");
				Set<String> topRWScoreConceptsMentions = new HashSet<String>();;
				Set<String> topRWScoreConcepts = new HashSet<String>();
				int state = 0;
				String preEntity = "-1";
				String curEntity = "-2";
				Set<String> foundEntities = new HashSet<String>(intialCapacity);
				entityRepetition = new HashMap<String, Integer>(intialCapacity);
				entityCategoryMap = new HashMap<Integer, Set<String>>(intialCapacity);
				stTime = System.currentTimeMillis();
				for (String sentence : sentenceList) {
					sentence = sentence.toLowerCase();
					String[] sentcWords = sentence.split(" ");
					for (int wordCnt = 0; wordCnt < sentcWords.length; wordCnt++) {
						String currentWord = sentcWords[wordCnt];
						currentWord = currentWord.replace("'s", "").replace("'S", "").replace("'t", "").replace("$", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace("\"", "").replace("--", "").replace("(", "").replace(")", "");
						currentWord = currentWord.replaceAll("[0-9]", "");
						/*
						if (stopWords.contains(currentWord) || currentWord.contains("[")) {
							continue;
						} // end of if
						if (currentWord.length() < 3) {
							continue;
						} // end of if
						 */
						int wordId = wordToIntMap.get(currentWord) != null ? wordToIntMap.get(currentWord) : -1;
						if (wordId != -1) {
							while (g(state, wordId) == FAIL) {
								state = f(state);
							} // end of while
							state = g(state, wordId);
							List<String> entities = output(state);
							if (entities != null) {
								curEntity = entities.get(0);
								String[] parts = curEntity.split(" ");
								while (true) {
									if (stopWords.contains(parts[0])) {
										curEntity = curEntity.substring(parts[0].length()).trim();
										parts = curEntity.split(" ");
									} else {
										break;
									} // end of if
								} // end of while
								if (curEntity.length() > 0) {
									if (curEntity.contains(preEntity)) {
										foundEntities.remove(preEntity);
										foundEntities.add(curEntity);
										preEntity = curEntity;
									} else {
										foundEntities.add(curEntity);
										preEntity = curEntity;
									} // end of if
									preEntity = curEntity;
								} // end of if
								int repetition = entityRepetition.get(curEntity) != null ? entityRepetition.get(curEntity) : 0;
								repetition++;
								entityRepetition.put(curEntity, repetition);
								initialEntities.addAll(entities);

								// Add all concepts of topRWWords to foundEntities
								// Start from 1 beacuse the first element is already in the foundEntities
								if (topRWScoreWords.contains(currentWord)) {
									topRWScoreConceptsMentions.add(entities.get(0));
									for (int i = 1;i < entities.size();i++) {
										String concept = entities.get(i);
										parts = concept.split(" ");
										/*
										while (true) {
											if (stopWords.contains(parts[0])) {
												concept = concept.substring(parts[0].length()).trim();
												parts = concept.split(" ");
											} else {
												break;
											} // end of if
										} // end of while
										 */
										if (parts.length == 1) {  // we ONLY add the concept correspondent to RWScoreWord from the list of concepts
											foundEntities.add(concept);
											repetition = entityRepetition.get(concept) != null ? entityRepetition.get(concept) : 0;
											repetition++;
											entityRepetition.put(concept, repetition);
											topRWScoreConceptsMentions.add(concept);
										} // end of if
									} // end of for (i)
								} // end of if
							} // end of if
						} // end of if (wordId != -1)
					} // end of for
				} // end of for
				// Set<String> foundEntities = new
				// HashSet<String>(fentities);
//				System.out.println("Total Entity Spotter time: " + (System.currentTimeMillis() - stTime));
				//				System.out.println("\n=============== Recognized Entities Start ================");
				//				System.out.println("Size: " + initialEntities.size());
				//				System.out.println(initialEntities);
				//				System.out.println("=============== Recognized Entities End   ================\n");
				// System.out.println("Number of Entities BEFORE removing Stop Words: "
				// + foundEntities.size() + "\n");
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				// System.out.println("Number of Entities AFTER removing Stop Words: "
				// + foundEntities.size() + "\n");
//				System.out.println("\n=============== Revised Recognized Entities Start ================");
//				System.out.println("Size: " + foundEntities.size());
//				System.out.println(foundEntities);
//				System.out.println("\n=============== Revised Recognized Entities End   ================\n");


				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
				int ecounter = 0;
				if (numOfEntityMatchAttr == 1) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if (enSet != null )
					} // end of while
				} else if (numOfEntityMatchAttr == 2) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} // end of if
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 3) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 4) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 5) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 6) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					double disambiguationNameShorWeight = entityMatchAttrWeightObj.get("disambiguationNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5' || et.getAttr() == '6') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '6') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameShorWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				}
//				System.out.println("\nTotal number of Wikipedia Entities including all attributes: " + ecounter);
				// System.out.println("Total number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\": "
				// + wikipediaEntityIds.size());
//				System.out.println("\nTotal number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\": " + wikipediaEntities.size());
				// System.out.println("Total number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\" Or \"Redirect Name Short\": "
				// + wikipediaEntities.size());

				//				System.out.println("Top K RW Concepts: " + topRWScoreConcepts + "\n");
//				stTime = System.currentTimeMillis();

				// Below is another approach for finding the Edges //
				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);

//				System.out.println("Total time to construct the Semantic Graph: " + (System.currentTimeMillis() - stTime));
				// semanticGraph.display();
				// semanticGraph.displayGraphvizPNG();
				// GraphvizImageWriter imgWriter = new GraphvizImageWriter();

//				System.out.println("Creating \"semanticGraph.dot\" file on disk...");
//				FileWriter dotFile = new FileWriter("semanticGraph.dot");
//				dotFile.write(semanticGraph.toDot());
//				dotFile.close();
//				System.out.println("\"semanticGraph.dot\" file Successfully created.\n");
				// FileOutputStream output = new
				// FileOutputStream("semanticGraph.png");
				// imgWriter.writeGraph(semanticGraph,
				// GraphvizImageWriter.COMMAND.dot,
				// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
				// output.flush();
				// output.close();
				SemanticGraph thematicSemanticGraph = new SemanticGraph();
				thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
//				stTime = System.currentTimeMillis();
				IntSet largestCComponent = null;
				int max = 0;
				int key = 0;
				for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
					if (entry.getValue().size() > max) {
						max = entry.getValue().size();
						key = entry.getKey();
					} // end of if
				} // end of for
				largestCComponent = ccVertexSets.get(key);
				thematicSemanticGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(largestCComponent);
				addLabelToGraphVertices(thematicSemanticGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
//				System.out.println("\nTotal time to construct the Thematic Graph: " + (System.currentTimeMillis() - stTime));
				thematicSemanticGraph.setVertexWeightProperty(semanticGraph.getVertexWeightProperty());
//				System.out.println("Creating \"thematicGraph.dot\" file on disk...");
//				dotFile = new FileWriter("thematicGraph.dot");
//				dotFile.write(thematicSemanticGraph.toDot());
//				dotFile.close();
//				System.out.println("\"thematicGraph.dot\" file Successfully created.\n");
				int[] vertices = thematicSemanticGraph.getVertices().toIntArray();
				runHITSalgorithm(thematicSemanticGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
				Set<Integer> authoritativeEntities = new HashSet<Integer>(100);
				Set<Integer> nonAuthEntities = new HashSet<Integer>(100);
				Set<Integer> allEntities = new HashSet<Integer>(100);
				//				System.out.println("============== CC Vertices Weights Start ==============\n");
				int i = 1;
				for (int vertex : vertices) {
					String ename = localNames.get(vertex);
					double vw = thematicSemanticGraph.getVertexWeight(vertex);
					if (vw >= authoritativeThreshold) {
						authoritativeEntities.add(vertex);
					} else {
						nonAuthEntities.add(vertex);
					} // end of if
					//					System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
					//					i++;
				} // end of for
				//				System.out.println("============== CC Vertices Weights End ==============\n");
				allEntities.addAll(authoritativeEntities);
				allEntities.addAll(nonAuthEntities);
				//				System.out.println("============== Authoritative Entities Start ==============\n");
				i = 1;
				List<Integer> sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicSemanticGraph.getVertexWeightProperty());
//				for (int vid : sortedAuth) {
//					String ename = localNames.get(vid);
//					double vw = thematicSemanticGraph.getVertexWeight(vid);
//					//					System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
//					//					i++;
//				}
				//				System.out.println("\n============== Authoritative Entities End ==============\n");
				SemanticGraph thematicSemanticGraphClone = (SemanticGraph) thematicSemanticGraph.clone();
				thematicSemanticGraphClone.getClass().getClassLoader().setDefaultAssertionStatus(false);
				// removeLowWeightVerticies(thematicSemanticGraph,
				// nonAuthEntities);
				removeLowWeightVerticies(thematicSemanticGraphClone, nonAuthEntities);
//				dotFile = new FileWriter("thematicGraph2.dot");
//				dotFile.write(thematicSemanticGraphClone.toDot());
//				dotFile.close();
				// largestCComponent = thematicSemanticGraph.getLargestConnectedComponent();
				largestCComponent = thematicSemanticGraphClone.getLargestConnectedComponent();
				// SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraph.getSubgraphInducedByVertices(largestCComponent);
				if (largestCComponent != null){
					SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraphClone.getSubgraphInducedByVertices(largestCComponent);
					thematicGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
					addLabelToGraphVertices(thematicGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
					thematicGraph.setVertexWeightProperty(thematicSemanticGraph.getVertexWeightProperty());
					vertices = thematicGraph.getVertices().toIntArray();
					runHITSalgorithm(thematicGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
					authoritativeEntities = new HashSet<Integer>(100);
					nonAuthEntities = new HashSet<Integer>(100);
//					System.out.println("============== CC Vertices Weights Start ==============\n");
					i = 1;
					for (int vertex : vertices) {
						String ename = localNames.get(vertex);
						double vw = thematicGraph.getVertexWeight(vertex);
						if (vw >= authoritativeThreshold) {
							authoritativeEntities.add(vertex);
						} else {
							nonAuthEntities.add(vertex);
						} // end of if
//						System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
//					System.out.println("============== CC Vertices Weights End ==============\n");
//					System.out.println("============== Authoritative Entities Start ==============\n");
					i = 1;
					sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicGraph.getVertexWeightProperty());
//					for (int vid : sortedAuth) {
//						String ename = localNames.get(vid);
//						double vw = thematicGraph.getVertexWeight(vid);
//						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
//						i++;
//					} // end of for
//					System.out.println("\n============== Authoritative Entities End ==============\n");
					
					Map<Integer, Double> centralEntities = getCentralEntities((SemanticGraph) thematicGraph.toUndirectedGraph(), authoritativeEntities, centralityThreshold);
//					System.out.println("============== Central Entities Start ==============\n");
					i = 1;
					List<Integer> sortedCentral = sortBasedOnScore(centralEntities);
					for (int vid : sortedCentral) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
//						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
//					System.out.println("\n============== Central Entities End ==============\n");

					
					Set<Integer> coreEntities = new HashSet<Integer>();
					if (topAuthoritativeEntities >= sortedAuth.size()) {
						coreEntities.addAll(sortedAuth);
					}else {
						for (int counter = 0;counter < sortedAuth.size(); counter++) {
							if (counter == topAuthoritativeEntities) {
								break;
							}else {
								coreEntities.add(sortedAuth.get(counter));
							} // end of if
						} // end of for
					} // end of if
					if (topCentralEntities >= sortedCentral.size()) {
						coreEntities.addAll(sortedCentral);
					}else {
						for (int counter = 0;counter < sortedCentral.size(); counter++) {
							if (counter == topCentralEntities) {
								break;
							}else {
								coreEntities.add(sortedCentral.get(counter));
							} // end of if
						} // end of for
					} // end of if

//					System.out.println("============== Core Entities Start ==============\n");
					i = 1;
//					for (int vid : coreEntities) {
//						String ename = localNames.get(vid);
//						double vw = thematicGraph.getVertexWeight(vid);
//						double centrality = centralEntities.get(vid);
////						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
//						i++;
//					} // end of for
//					System.out.println("\n============== Core Entities End ==============\n");
					//					IntSet vList = thematicSemanticGraph.getVertices();
					IntSet vList = semanticGraph.getVertices();
					for (String c : topRWScoreConcepts) {
						int cid = wikipediaEntities.get(c);
						if (vList.contains(cid)) {
							coreEntities.add(cid);
						} // end of if
					} // end of for
					//					show(new ArrayList<Integer> (coreEntities), entityCategoryMap);
					// Find Core and Context Entities for constructing the taxonomy Graph
					//					Set<Integer> contextEntities = findCoreContextEntities(thematicSemanticGraph, coreEntities);
					Set<Integer> contextEntities = findCoreContextEntities(semanticGraph, coreEntities);

					// It is used to create Wikipedia Taxonomy Graph for ONLY categories of core entities 
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (coreEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of ALL entities
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (allEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of Core and Context entities
					SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap, categoryhierarchyLevel);
//					dotFile = new FileWriter("taxonomyGraph.dot");
//					dotFile.write(taxonomyGraph.toDot());
//					dotFile.close();

					LabelProperty vlp = taxonomyGraph.getVertexLabelProperty();
					taxonomyGraph = (SemanticGraph) taxonomyGraph.toUndirectedGraph();
					int [ ] taxonomyVerticies = taxonomyGraph.getVertices().toIntArray();
//					System.out.println("TaxonomyVerticies size: " + taxonomyVerticies.length);
//					int [ ] vlabelIds = vlp.getIDs().toIntArray();
					Map<Integer,Integer> categoryEntityLinkMap = new HashMap<Integer,Integer>();
					for(int catId : taxonomyVerticies) {
						if (!contextEntities.contains(catId)) {
							String categoryName = vlp.getValue(catId);
							int numOfLinks = findNumberOfCategoryEntities(categoryName, virtGraph);
							categoryEntityLinkMap.put(catId, numOfLinks);
						} // end of if
					} // end of for

//					System.out.println("Processing links... done!");

					// This approach is same as previous one, but ONLY for top K categories, and ONLY for core entities 
					Map<Integer,Double> categoryScoreMap = new HashMap<Integer,Double>();
					Map<Integer,Integer> categoryCoverageMap = new HashMap<Integer,Integer>();
					for (int entId : contextEntities) {  // ONLY for core entities 
						//					for (int entId : allEntities) {   // for all entities
						BFSResult bfsTree = taxonomyGraph.bfs(entId);
						for (int catId : taxonomyVerticies) {
							if (!contextEntities.contains(catId)) {
								String category = vlp.getValue(catId);
								calculateCategoryScore(taxonomyGraph, bfsTree, entId, catId, category, categoryScoreMap, categoryCoverageMap);
							}
						} // end of for
					} // end of for
					Set<Integer> keySet = categoryScoreMap.keySet();
					for (int id : keySet) {
						double score1 = categoryScoreMap.get(id);
						double score2 = categoryCoverageMap.get(id);
						double finalScore = (alpha * score1) + (1 - alpha) * score2;
						categoryScoreMap.put(id, finalScore);
					} // end of for
					List<Integer> sortedCats = sortCategoryIdBasedOnScore(categoryScoreMap);
					List<Integer> sortedCatsSubList = new ArrayList<Integer>();
					Set<Integer> topCategories = new HashSet<Integer>();
					Set<String> topCategoriesNames = new HashSet<String>();
					int k = 1;
//					System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					for (int catId : sortedCats) {
						if (categoryEntityLinkMap.get(catId) >= MaxNumOfLinksPerCategory) {
							continue;
						}else {
							String catName = vlp.getValue(catId);
							topCategoriesNames.add(catName);
//							System.out.println("k: " + k + " Category: " + catName + "   Score: " + (categoryScoreMap.get(catId) / categoryScoreMap.get(sortedCats.get(0))) + " Coverage: " + categoryCoverageMap.get(catId) + " links: " + categoryEntityLinkMap.get(catId));
							topCategories.add(catNamesToInt.get(catName));
							sortedCatsSubList.add(catId);
							k++;
							if (k == 31) {
								break;
							}
						} // end of if
					} // end of for
//					System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");

					List<String> userCatNames = new ArrayList<String>(Arrays.asList(userCats.split(",")));
					List<Integer> userCatIds = new ArrayList<Integer>();
					for (String c : userCatNames) {
						userCatIds.add(catNamesToInt.get(c));
					} // end of for
					List<Double>  finalAvgRelatedness = new ArrayList<Double>();
					List<String>  intersectionCatNames = null;
					String logMsg = "";
					for (int counter = 0;counter < userCatIds.size();counter++) {
						Set<Integer> parentGraph = CategoryProcessor.categoryTreeFromChildToParent(wikiTaxonomyMap_ID_FromChildToParent, userCatIds.get(counter));
						Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, userCatIds.get(counter), parentGraph);
						//					Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, catId1);
						Set<Integer> intersection = CategoryProcessor.getIntersection(userCategory, topCategories);
						intersectionCatNames = new ArrayList<String>();
//						System.out.println("\n=============== Intersection Categories Start ===============\n ");
						for (int id : intersection) {
							intersectionCatNames.add(catNames.get(id));
//							System.out.println("catId: " + userCatNames.get(counter) + "   CatName: " + catNames.get(id));
						} // end of for
//						System.out.println("\n=============== Intersection Categories End   =============== ");
						Map<Integer,Double> finalSRelatednessMap = new HashMap<Integer,Double>();
						Map<Integer,Set<Integer>> coreEntityLinks = new HashMap<Integer,Set<Integer>>(coreEntities.size());
						//					for (int entId : contextEntities) {
						for (int entId : coreEntities) {
							String entName = localNames.get(entId);
							coreEntityLinks.put(entId, findLinkedEntities(entName, virtGraph));
						} // end of for
						if (intersection.isEmpty()) {
							intersectionCatNames.add(userCatNames.get(counter));
//							System.out.println("catId: " + counter + "   CatName: " + userCatNames.get(counter));
						} // end of if
						for (String category : intersectionCatNames) {
							Map<Integer,Double> sRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, coreEntities, coreEntityLinks, virtGraph);
							//						Map<Integer,Double> sRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, contextEntities, coreEntityLinks, virtGraph);
//							System.out.println("\n=============== Semantic Relatedness of Core Entities and category \"" + category +"\" Start ===============\n");
							for (Map.Entry<Integer, Double> entry : sRelatednessMap.entrySet()) {
								int entId = entry.getKey();
								double sr = entry.getValue();
//								System.out.println("entId: " + entId + "   entity: " + localNames.get(entId) + "    sr: " + sr);
								double preVal = finalSRelatednessMap.get(entId) != null ? finalSRelatednessMap.get(entId) : 0;
								finalSRelatednessMap.put(entId, Math.max(preVal, sr));
								//							avgSRelatednessMap.put(entId, preVal + sr);
							} // end of for
//							System.out.println("\n=============== Semantic Relatedness of Core Entities and category \"" + category +"\" End   ===============\n");
						} // end of for
//						System.out.println("\n\n\n************ FINAL Semantic Relatedness of Core Entities ************\n ");
						logMsg += "************ FINAL Semantic Relatedness to \"" + userCatNames.get(counter) + "\" ************\n";
//						logger.log(Level.INFO, "************ FINAL Semantic Relatedness of Core Entities ************");
						List<Integer> sortedRelatedness = sortBasedOnScore(finalSRelatednessMap);

						// pick the top K semantic relatedness and get the average as the relatedness of the document to user's category of interest
						List<Integer> sortedTopKRelatedness = sortedRelatedness.size() >= 5 ? new ArrayList<Integer>(sortedRelatedness.subList(0, 5)) : sortedRelatedness;
						for (int entId : sortedRelatedness) {
							logMsg += "Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId) + "\n";
//							System.out.println("entId: " + entId + "  Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId));
//							logger.log(Level.INFO,"entId: " + entId + "  Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId));
						} // end of for
						double sumOfSr = 0;
						for (int entId : sortedTopKRelatedness) {
							sumOfSr += finalSRelatednessMap.get(entId);
						} // end of for
						double avgSR = sumOfSr /sortedTopKRelatedness.size(); 
//						System.out.println("\n\nAVERAGE Relatedness of Document to User's Category of Interest is: " + avgSR);
//						logger.log(Level.INFO,"AVERAGE Relatedness of Document to User's Category of Interest is: " + avgSR);
						finalAvgRelatedness.add(avgSR);
						double srThreshold = 0.2;
						if (avgSR >= srThreshold) {
//							System.out.println("\n*************** Document belongs to the category \"" + userCatNames.get(counter) + "\"");
							int prval = catBatchResult.get(userCatNames.get(counter)) != null ? catBatchResult.get(userCatNames.get(counter)) : 0;
							catBatchResult.put(userCatNames.get(counter), prval + 1);
//							logger.log(Level.INFO,"File: " + fileCounter + "   " +  fileName + "*************** Document belongs to the category \"" + userCatNames.get(counter) + "\"");
						}else {
//							System.out.println("\n*************** Document does NOT belong to the category \"" + userCatNames.get(counter) + "\"");
//							logger.log(Level.INFO,"File: " + fileCounter + "   " +  fileName + "*************** Document does NOT belong to the category \"" + userCatNames.get(counter) + "\"");
						} // end of if
						logger.log(Level.INFO, "\n" + logMsg);
						logMsg = "";
					} // end of for userCatIds
					for (int c = 0 ;c < userCatNames.size();c++) {
						logMsg += "File: " + fileCounter + "   " +  fileName + " SR to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c) + "\n";
//						logger.log(Level.INFO,"File: " + fileCounter + "   " +  fileName + " SR to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c));
//						System.out.println("\n\n Relatedness of Document to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c));
					} // end of for
					
					logger.log(Level.INFO, "\n" + logMsg);
					logger.log(Level.INFO, fileCounter + " files processed.");
					fileCounter++;
					
				} // end of if (largestCComponent != null)
				file.renameTo(new File(RSS_DIR_PATH + fileName + "_done"));
			} // end of for listFile
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING, e.getMessage());
//			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage());
//			e.printStackTrace();
		}
			
	} // end of recognizeEntites_Batch
	
	
	
	public void recognizeEntites() {
		long stTime = System.currentTimeMillis();
		graphNodes = loadDeterministicGraphToMemory();
		failureMap = loadFailureFunctionMapToMemory();
		outputSet = loadOutputMapToMemory();
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		Map<String, Set<Entity>> uniqueNames = DbProcessor.loadUniqueNameListToMemory();
		LocalNameMapperClass localNamesMapperClass = DbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
		localNames = localNamesMapperClass.getLocalNameList();
		namesToInt = localNamesMapperClass.getNameToInt();
		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		// yagoTaxonomyMap = DictionaryGenerator.loadYagoTaxonomyMapToMemory();
		wikiTaxonomyMapFromChildToParent = DbProcessor.loadWikiTaxonomyMapFromChildToParentToMemory();
		wikiTaxonomyMap_ID_FromChildToParent = DbProcessor.loadWikiTaxonomyMap_ID_FromChildToParentToMemory();
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
		wikiTaxonomyMapFromParentToChild = DbProcessor.loadWikiTaxonomyMapFromParentToChildToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> catNames = catNameMapper.getCatNameList();
		System.out.println("Total Load Time: " + (System.currentTimeMillis() - stTime));
		// List<String> wlist = wordMapperClass.getWordList();
		Scanner sc = new Scanner(System.in);
		Set<String> stopWords = readStopWords();
		Set<String> timeRelatedWords = readTimeRelatedEntities();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		String fileName = "/home/mehdi/document.txt";
		String document = readDocument(fileName);
		do {
			try {
				JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
				JsonParser jsonParser = new JsonParser();
				JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
				SemanticGraph semanticGraph = new SemanticGraph();
				semanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				LabelProperty vertexLabelProperty = semanticGraph.getVertexLabelProperty();
				int intialCapacity = confObject.get("initialCapacity").getAsInt();
				List<String> initialEntities = new ArrayList<String>(4 * intialCapacity);
				SentenceDetector sd = null;
				try {
					sd = new SentenceDetector();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				List<String> sentenceList = sd.getAllSentencesFromDocument(document);
				Set<String> topRWScoreWords = randomWalkScores(sentenceList, stopWords);
				
//				System.out.println("============> topRWScoreWords: " +  topRWScoreWords + "\n");
				Set<String> topRWScoreConceptsMentions = new HashSet<String>();;
				Set<String> topRWScoreConcepts = new HashSet<String>();
				int state = 0;
				String preEntity = "-1";
				String curEntity = "-2";
				Set<String> foundEntities = new HashSet<String>(intialCapacity);
				entityRepetition = new HashMap<String, Integer>(intialCapacity);
				entityCategoryMap = new HashMap<Integer, Set<String>>(intialCapacity);
				List<String> temp = new ArrayList<String>();
				stTime = System.currentTimeMillis();
				for (String sentence : sentenceList) {
					sentence = sentence.toLowerCase();
					String[] sentcWords = sentence.split(" ");
					for (int wordCnt = 0; wordCnt < sentcWords.length; wordCnt++) {
						String currentWord = sentcWords[wordCnt];
						currentWord = currentWord.replace("'s", "").replace("'S", "").replace("'t", "").replace("$", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace("\"", "").replace("--", "").replace("(", "").replace(")", "");
						currentWord = currentWord.replaceAll("[0-9]", "");
						/*
						if (stopWords.contains(currentWord) || currentWord.contains("[")) {
							continue;
						} // end of if
						if (currentWord.length() < 3) {
							continue;
						} // end of if
						*/
						int wordId = wordToIntMap.get(currentWord) != null ? wordToIntMap.get(currentWord) : -1;
						if (wordId != -1) {
							while (g(state, wordId) == FAIL) {
								state = f(state);
							} // end of while
							state = g(state, wordId);
							List<String> entities = output(state);
							if (entities != null) {
								curEntity = entities.get(0);
								String[] parts = curEntity.split(" ");
								while (true) {
									if (stopWords.contains(parts[0])) {
										curEntity = curEntity.substring(parts[0].length()).trim();
										parts = curEntity.split(" ");
									} else {
										break;
									} // end of if
								} // end of while
								if (curEntity.length() > 0) {
									if (curEntity.contains(preEntity)) {
										foundEntities.remove(preEntity);
										foundEntities.add(curEntity);
										temp.add(curEntity);
										preEntity = curEntity;
									} else {
										foundEntities.add(curEntity);
										temp.add(curEntity);
										preEntity = curEntity;
									} // end of if
									preEntity = curEntity;
								} // end of if
								int repetition = entityRepetition.get(curEntity) != null ? entityRepetition.get(curEntity) : 0;
								repetition++;
								entityRepetition.put(curEntity, repetition);
								initialEntities.addAll(entities);
								
								// Add all concepts of topRWWords to foundEntities
								// Start from 1 beacuse the first element is already in the foundEntities
								if (topRWScoreWords.contains(currentWord)) {
									topRWScoreConceptsMentions.add(entities.get(0));
									for (int i = 1;i < entities.size();i++) {
										String concept = entities.get(i);
										parts = concept.split(" ");
										/*
										while (true) {
											if (stopWords.contains(parts[0])) {
												concept = concept.substring(parts[0].length()).trim();
												parts = concept.split(" ");
											} else {
												break;
											} // end of if
										} // end of while
										*/
										if (parts.length == 1) {  // we ONLY add the concept corresponding to RWScoreWord from the list of concepts
											foundEntities.add(concept);
											temp.add(concept);
											repetition = entityRepetition.get(concept) != null ? entityRepetition.get(concept) : 0;
											repetition++;
											entityRepetition.put(concept, repetition);
											topRWScoreConceptsMentions.add(concept);
										} // end of if
									} // end of for (i)
								} // end of if
							} // end of if
						} // end of if (wordId != -1)
					} // end of for
				} // end of for
					// Set<String> foundEntities = new
					// HashSet<String>(fentities);
//				System.out.println("Total Entity Spotter time: " + (System.currentTimeMillis() - stTime));
//				System.out.println("\n=============== Recognized Entities Start ================");
//				System.out.println("Size: " + initialEntities.size());
//				System.out.println(initialEntities);
//				System.out.println("=============== Recognized Entities End   ================\n");
				// System.out.println("Number of Entities BEFORE removing Stop Words: "
				// + foundEntities.size() + "\n");
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				// System.out.println("Number of Entities AFTER removing Stop Words: "
				// + foundEntities.size() + "\n");
				System.out.println("\n=============== Revised Recognized Entities Start ================");
				System.out.println("Size: " + foundEntities.size());
				System.out.println(foundEntities);
				System.out.println("\n=============== Revised Recognized Entities End   ================\n");
				
				
				System.out.println("temp: " + temp);
				
				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
				int ecounter = 0;
				int numOfEntityMatchAttr = confObject.get("numOfEntityMatchAttr").getAsInt();
				JsonObject entityMatchAttrWeightObj = confObject.get("entityMatchAttrWeight").getAsJsonObject();
				if (numOfEntityMatchAttr == 1) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
//							System.out.println();
						} // end of if (enSet != null )
					} // end of while
				} else if (numOfEntityMatchAttr == 2) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} // end of if
//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								} // end of if
							} // end of for (Entity et : enSet)
//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 3) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									}
//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 4) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									}
//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 5) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									}
//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
//							System.out.println();
						} // end of if
					} // end of while
				} else if (numOfEntityMatchAttr == 6) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					double redirectNameWeight = entityMatchAttrWeightObj.get("redirectName").getAsDouble();
					double redirectNameShortWeight = entityMatchAttrWeightObj.get("redirectNameShort").getAsDouble();
					double nameShortWeight = entityMatchAttrWeightObj.get("nameShort").getAsDouble();
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					double disambiguationNameShorWeight = entityMatchAttrWeightObj.get("disambiguationNameShort").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3' || et.getAttr() == '4' || et.getAttr() == '5' || et.getAttr() == '6') {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '4') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), nameShortWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '5') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '6') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameShorWeight, entityRepetition.get(en));
									}
//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
//							System.out.println();
						} // end of if
					} // end of while
				}
				System.out.println("\nTotal number of Wikipedia Entities including all attributes: " + ecounter);
				// System.out.println("Total number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\": "
				// + wikipediaEntityIds.size());
				System.out.println("\nTotal number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\": " + wikipediaEntities.size());
				// System.out.println("Total number of Wikipedia Entities having matching \"Title\" Or \"Redirect Name\" Or \"Redirect Name Short\": "
				// + wikipediaEntities.size());
				
//				System.out.println("Top K RW Concepts: " + topRWScoreConcepts + "\n");
				stTime = System.currentTimeMillis();

				// Below is another approach for finding the Edges //
				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);

				// Below is another approach for finding the Edges //
				// for (Map.Entry<String, Integer> entry :
				// wikipediaEntities.entrySet()) {
				// String entName = entry.getKey();
				// if (entName.indexOf('"') == -1) {
				// int enId = entry.getValue();
				// Set<Integer> relations = getEntityRelationList(virtGraph,
				// wikipediaEntities, entName);
				// if (!relations.isEmpty()) {
				// for (int vertexId : relations) {
				// if (enId == vertexId) {
				// continue;
				// }
				// addEdgeToSemanticGraph(semanticGraph, enId, vertexId);
				// constructAllConnectedComponents(enId, vertexId);
				// } // end of for
				// } // end of if (!relations.isEmpty())
				// } // end of if (entName.indexOf('"')
				// } // end of for Map

				// Below is another approach for finding the Edges //
				// for (int row = 0;row < wikipediaEntityIds.size();row++) {
				// for (int col = row + 1;col < wikipediaEntityIds.size();col++)
				// {
				// String en1 = wikipediaEntityNames.get(row);
				// String en2 = wikipediaEntityNames.get(col);
				// if (en1.indexOf('"') == -1 && en2.indexOf('"') == -1) {
				// int e1Id = wikipediaEntityIds.get(row);
				// int e2Id = wikipediaEntityIds.get(col);
				// List<Boolean> relations = getEntityRelationList(virtGraph,
				// en1, en2);
				// if (relations.get(0) && !relations.get(1)) {
				// addEdgeToSemanticGraph(semanticGraph, e1Id, e2Id, 1);
				// constructAllConnectedComponents(e1Id, e2Id);
				// }else if (!relations.get(0) && relations.get(1)) {
				// addEdgeToSemanticGraph(semanticGraph, e2Id, e1Id, 1);
				// constructAllConnectedComponents(e2Id, e1Id);
				// }else if (relations.get(0) && relations.get(1)) {
				// addEdgeToSemanticGraph(semanticGraph, e1Id, e2Id, 2);
				// constructAllConnectedComponents(e1Id, e2Id);
				// constructAllConnectedComponents(e2Id, e1Id);
				// } // end of if
				// }
				// } // end of for (int col)
				// } // end of for (int row)
				System.out.println("Total time to construct the Semantic Graph: " + (System.currentTimeMillis() - stTime));
				// semanticGraph.display();
				// semanticGraph.displayGraphvizPNG();
				// GraphvizImageWriter imgWriter = new GraphvizImageWriter();

				System.out.println("Creating \"semanticGraph.dot\" file on disk...");
				FileWriter dotFile = new FileWriter("semanticGraph.dot");
				dotFile.write(semanticGraph.toDot());
				dotFile.close();
				System.out.println("\"semanticGraph.dot\" file Successfully created.\n");
				// FileOutputStream output = new
				// FileOutputStream("semanticGraph.png");
				// imgWriter.writeGraph(semanticGraph,
				// GraphvizImageWriter.COMMAND.dot,
				// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
				// output.flush();
				// output.close();
				SemanticGraph thematicSemanticGraph = new SemanticGraph();
				thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				stTime = System.currentTimeMillis();
				IntSet largestCComponent = null;
				int max = 0;
				int key = 0;
				for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
					if (entry.getValue().size() > max) {
						max = entry.getValue().size();
						key = entry.getKey();
					} // end of if
				} // end of for
				largestCComponent = ccVertexSets.get(key);
				thematicSemanticGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(largestCComponent);
				addLabelToGraphVertices(thematicSemanticGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
				System.out.println("\nTotal time to construct the Thematic Graph: " + (System.currentTimeMillis() - stTime));
				thematicSemanticGraph.setVertexWeightProperty(semanticGraph.getVertexWeightProperty());
				System.out.println("Creating \"thematicGraph.dot\" file on disk...");
				dotFile = new FileWriter("thematicGraph.dot");
				dotFile.write(thematicSemanticGraph.toDot());
				dotFile.close();
				System.out.println("\"thematicGraph.dot\" file Successfully created.\n");
				int[] vertices = thematicSemanticGraph.getVertices().toIntArray();
				int numOfSteps = confObject.get("numOfSteps").getAsInt();
				JsonObject relationImportanceWeightObj = confObject.get("relationImportanceWeight").getAsJsonObject();
				runHITSalgorithm(thematicSemanticGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
				double authoritativeThreshold = confObject.get("authoritativeThreshold").getAsDouble();
				Set<Integer> authoritativeEntities = new HashSet<Integer>(100);
				Set<Integer> nonAuthEntities = new HashSet<Integer>(100);
				Set<Integer> allEntities = new HashSet<Integer>(100);
//				System.out.println("============== CC Vertices Weights Start ==============\n");
				int i = 1;
				for (int vertex : vertices) {
					String ename = localNames.get(vertex);
					double vw = thematicSemanticGraph.getVertexWeight(vertex);
					if (vw >= authoritativeThreshold) {
						authoritativeEntities.add(vertex);
					} else {
						nonAuthEntities.add(vertex);
					} // end of if
//					System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
//					i++;
				} // end of for
//				System.out.println("============== CC Vertices Weights End ==============\n");
				allEntities.addAll(authoritativeEntities);
				allEntities.addAll(nonAuthEntities);
//				System.out.println("============== Authoritative Entities Start ==============\n");
				i = 1;
				List<Integer> sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicSemanticGraph.getVertexWeightProperty());
				for (int vid : sortedAuth) {
					String ename = localNames.get(vid);
					double vw = thematicSemanticGraph.getVertexWeight(vid);
//					System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
//					i++;
				}
//				System.out.println("\n============== Authoritative Entities End ==============\n");
				SemanticGraph thematicSemanticGraphClone = (SemanticGraph) thematicSemanticGraph.clone();
				thematicSemanticGraphClone.getClass().getClassLoader().setDefaultAssertionStatus(false);
				// removeLowWeightVerticies(thematicSemanticGraph,
				// nonAuthEntities);
				removeLowWeightVerticies(thematicSemanticGraphClone, nonAuthEntities);
				dotFile = new FileWriter("thematicGraph2.dot");
				dotFile.write(thematicSemanticGraphClone.toDot());
				dotFile.close();
				// largestCComponent = thematicSemanticGraph.getLargestConnectedComponent();
				largestCComponent = thematicSemanticGraphClone.getLargestConnectedComponent();
				// SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraph.getSubgraphInducedByVertices(largestCComponent);
				if (largestCComponent != null){
					SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraphClone.getSubgraphInducedByVertices(largestCComponent);
					thematicGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
					addLabelToGraphVertices(thematicGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
					thematicGraph.setVertexWeightProperty(thematicSemanticGraph.getVertexWeightProperty());
					vertices = thematicGraph.getVertices().toIntArray();
					runHITSalgorithm(thematicGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
					authoritativeEntities = new HashSet<Integer>(100);
					nonAuthEntities = new HashSet<Integer>(100);
					System.out.println("============== CC Vertices Weights Start ==============\n");
					i = 1;
					for (int vertex : vertices) {
						String ename = localNames.get(vertex);
						double vw = thematicGraph.getVertexWeight(vertex);
						if (vw >= authoritativeThreshold) {
							authoritativeEntities.add(vertex);
						} else {
							nonAuthEntities.add(vertex);
						} // end of if
						System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
					System.out.println("============== CC Vertices Weights End ==============\n");
					System.out.println("============== Authoritative Entities Start ==============\n");
					i = 1;
					sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicGraph.getVertexWeightProperty());
					for (int vid : sortedAuth) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
					System.out.println("\n============== Authoritative Entities End ==============\n");
					double centralityThreshold = confObject.get("centralityThreshold").getAsDouble();
					Map<Integer, Double> centralEntities = getCentralEntities((SemanticGraph) thematicGraph.toUndirectedGraph(), authoritativeEntities, centralityThreshold);
					System.out.println("============== Central Entities Start ==============\n");
					i = 1;
					List<Integer> sortedCentral = sortBasedOnScore(centralEntities);
					for (int vid : sortedCentral) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
					System.out.println("\n============== Central Entities End ==============\n");

					int topAuthoritativeEntities = confObject.get("topK-AuthoritativeEntities").getAsInt();
					int topCentralEntities = confObject.get("topK-CentralEntities").getAsInt();
					Set<Integer> coreEntities = new HashSet<Integer>();
					if (topAuthoritativeEntities >= sortedAuth.size()) {
						coreEntities.addAll(sortedAuth);
					}else {
						for (int counter = 0;counter < sortedAuth.size(); counter++) {
							if (counter == topAuthoritativeEntities) {
								break;
							}else {
								coreEntities.add(sortedAuth.get(counter));
							} // end of if
						} // end of for
					} // end of if
					if (topCentralEntities >= sortedCentral.size()) {
						coreEntities.addAll(sortedCentral);
					}else {
						for (int counter = 0;counter < sortedCentral.size(); counter++) {
							if (counter == topCentralEntities) {
								break;
							}else {
								coreEntities.add(sortedCentral.get(counter));
							} // end of if
						} // end of for
					} // end of if

					System.out.println("============== Core Entities Start ==============\n");
					i = 1;
					for (int vid : coreEntities) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
					System.out.println("\n============== Core Entities End ==============\n");
//					IntSet vList = thematicSemanticGraph.getVertices();
					IntSet vList = semanticGraph.getVertices();
					for (String c : topRWScoreConcepts) {
						int cid = wikipediaEntities.get(c);
						if (vList.contains(cid)) {
							coreEntities.add(cid);
						} // end of if
					} // end of for
//					show(new ArrayList<Integer> (coreEntities), entityCategoryMap);
					int categoryhierarchyLevel = confObject.get("categoryhierarchyLevel").getAsInt();

					// Find Core and Context Entities for constructing the taxonomy Graph
//					Set<Integer> contextEntities = findCoreContextEntities(thematicSemanticGraph, coreEntities);
					Set<Integer> contextEntities = findCoreContextEntities(semanticGraph, coreEntities);
					
					// It is used to create  YAGO Taxonomy Graph for ALL categories of Core and Context entities
//					SemanticGraph taxonomyGraph = constructYagoTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap);

					// It is used to create Wikipedia Taxonomy Graph for ONLY categories of core entities 
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (coreEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of ALL entities
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (allEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of Core and Context entities
					SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap, categoryhierarchyLevel);
					dotFile = new FileWriter("taxonomyGraph.dot");
					dotFile.write(taxonomyGraph.toDot());
					dotFile.close();

					double alpha = confObject.get("alpha").getAsDouble();
					LabelProperty vlp = taxonomyGraph.getVertexLabelProperty();
					taxonomyGraph = (SemanticGraph) taxonomyGraph.toUndirectedGraph();
					int [ ] taxonomyVerticies = taxonomyGraph.getVertices().toIntArray();
					System.out.println("TaxonomyVerticies size: " + taxonomyVerticies.length);
					int [ ] vlabelIds = vlp.getIDs().toIntArray();
					Map<Integer,Integer> categoryEntityLinkMap = new HashMap<Integer,Integer>();
					for(int catId : taxonomyVerticies) {
						if (!contextEntities.contains(catId)) {
							String categoryName = vlp.getValue(catId);
							int numOfLinks = findNumberOfCategoryEntities(categoryName, virtGraph);
							categoryEntityLinkMap.put(catId, numOfLinks);
						} // end of if
					} // end of for

					System.out.println("Processing links... done!");



					/*
				List<String> contextCategories = findContextCategories(thematicSemanticGraph, coreEntities);
				System.out.println("contextCategories: "+ contextCategories.size());
				int topKCategoryThreshold = confObject.get("topKCategoryThreshold").getAsInt();
				// This is for finding top K categories from ONLY the core entities categories
//				List<String> filteredCatNames = findTopKCategories(coreEntities, stopWords, topKCategoryThreshold, contextCategories);

				// This is for finding top K categories from all the categories of all entities
//				List<String> filteredCatNames = findTopKCategories(allEntities, entityCategoryMap, stopWords, topKCategoryThreshold);

				List<String> filteredCatNames = new ArrayList<String>(contextCategories.size());
				filteredCatNames.addAll(contextCategories);
				List<Integer> finalCatsIds = new ArrayList<Integer>(filteredCatNames.size());
				for (String category : filteredCatNames) {
					int vid = 0;
					for (int catId : vlabelIds) {
						if (vlp.getValue(catId).equals(category)) {
							vid = catId;
							break;
						} // end of if
					} // end of for
					finalCatsIds.add(vid);
				} // end of for
				System.out.println("finalCatsId: "+ finalCatsIds.size());
					 */

					// The following part is ONLY for categories of the core entities.
					// It is based on the semantic relatedness formula from Wikipedia link structure
					//				Map<String,Double> catScoreMap = new HashMap<String,Double>();
					//				double maxScore = 0;
					//				String bestCat = "";
					//				for (int eid : coreEntities) {
					//					Set<String> cats = entityCategoryMap.get(eid);
					//					int k = 1;
					//					for (String category : cats) {
					//						int vid = 0;
					//						for (int catId : vlabelIds) {
					//							if (vlp.getValue(catId).equals(category)) {
					//								vid = catId;
					//								break;
					//							} // end of if
					//						} // end of for
					//						System.out.println("k: " + k);
					//						System.out.println("category: " + category);
					//						BFSResult bfsTree = taxonomyGraph.bfs(vid);
					//						double sum = 0;
					//						Map<Integer,Double> scoreMap = calculateCategoryScore(category, coreEntities, virtGraph, alpha, bfsTree);
					//						for (int entId : coreEntities) {
					//							String entName = localNames.get(entId);
					//							System.out.println("category Score: " + scoreMap.get(entId) + " category: " + category + " entity: " + entName);
					//							sum += scoreMap.get(entId);
					//						} // end of for (int entId : coreEntities)
					//						catScoreMap.put(category, sum);
					//						System.out.println("Sum of Scores: " + sum);
					//						System.out.println();
					//						if (maxScore < sum) {
					//							maxScore = sum;
					//							bestCat  = category;
					//						} // end of if
					//					} // end of for (String category : cats)
					//					k++;
					//				} // end of for (int eid : coreEntities)
					//				System.out.println("Max Category Score: " + maxScore + " Best Category: " + bestCat);



					// The following part is for all the categories in the taxonomy Graph.
					// It is based on the semantic relatedness formula from Wikipedia link structure.

					//				double maxScore = 0;
					//				String bestCat = "";
					//				int k = 0;
					//				Map<String,Double> catScoreMap = new HashMap<String,Double>();
					//				for (int vid : taxonomyVerticies) {
					//					System.out.println("k: " + k);
					//					if (!coreEntities.contains(vid)) {
					//						String category = vlp.getValue(vid);
					//						System.out.println("category: " + category);
					//						BFSResult bfsTree = taxonomyGraph.bfs(vid);
					//						double sum = 0;
					//						Map<Integer,Double> scoreMap = calculateCategoryScore(category, coreEntities, virtGraph, alpha, bfsTree);
					//						for (int entId : coreEntities) {
					////							String entName = localNames.get(entId);
					////							System.out.println("category Score: " + scoreMap.get(entId) + " category: " + category + " entity: " + entName);
					//							sum += scoreMap.get(entId);
					//						} // end of for (int entId : coreEntities)
					//						catScoreMap.put(category, sum);
					//						System.out.println("Sum of Scores: " + sum);
					//						System.out.println();
					//						if (maxScore < sum) {
					//							maxScore = sum;
					//							bestCat  = category;
					//						} // end of if
					//					} // end of if
					//					k++;
					//				} // end of for (int vid : taxonomyVerticies)
					//				System.out.println("Max Category Score: " + maxScore + " Best Category: " + bestCat);
					//				List<String> sortedCats = CategorySortBasedOnScore(catScoreMap);
					//				System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					//				for (String cat : sortedCats) {
					//					System.out.println("Category: " + cat + "   Score: " + catScoreMap.get(cat));
					//				}
					//				System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");




					// This approach is different with two approaches above
					// It is based on the belonging mesurement which is calculated from the
					// path between entity and category.
					// It can be used for either all entities or ONLY core ones.

					//				Map<Integer,Double> categoryScoreMap = new HashMap<Integer,Double>();
					//				for (int catId : taxonomyVerticies) {
					//					if (!coreEntities.contains(catId)) {
					//						String category = vlp.getValue(catId);
					////						System.out.println("category: " + category);
					//						for (int entId : coreEntities) { // ONLY for core entities 
					////						for (int entId : allEntities) {  // for all entities
					//							BFSResult bfsTree = taxonomyGraph.bfs(entId);
					//							calculateCategoryScore(taxonomyGraph, bfsTree, entId, catId, category, categoryScoreMap);
					////							String entName = localNames.get(entId);
					////							System.out.println("category Score: " + categoryScoreMap.get(catId) + " category: " + category + " entity: " + entName);
					//						} // end of for
					////						categoryScoreMap.put(catId, categoryScoreMap.get(catId) / coreEntities.size());
					////						System.out.println();
					//					} // end of if
					//				} // end of for
					//				List<Integer> sortedCats = CategoryIdSortBasedOnScore(categoryScoreMap);
					//				System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					//				for (int catId : sortedCats) {
					//					System.out.println("Category: " + vlp.getValue(catId) + "   Score: " + categoryScoreMap.get(catId));
					//				}
					//				System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");


					// This approach is same as previous one, but ONLY for top K categories, and ONLY for core entities 
					Map<Integer,Double> categoryScoreMap = new HashMap<Integer,Double>();
					Map<Integer,Integer> categoryCoverageMap = new HashMap<Integer,Integer>();
					for (int entId : contextEntities) {  // ONLY for core entities 
						//					for (int entId : allEntities) {   // for all entities
						BFSResult bfsTree = taxonomyGraph.bfs(entId);
						for (int catId : taxonomyVerticies) {
							if (!contextEntities.contains(catId)) {
								String category = vlp.getValue(catId);
								calculateCategoryScore(taxonomyGraph, bfsTree, entId, catId, category, categoryScoreMap, categoryCoverageMap);
							}
						} // end of for
					} // end of for
					Set<Integer> keySet = categoryScoreMap.keySet();
					for (int id : keySet) {
						double score1 = categoryScoreMap.get(id);
						double score2 = categoryCoverageMap.get(id);
						double finalScore = (alpha * score1) + (1 - alpha) * score2;
						categoryScoreMap.put(id, finalScore);
					} // end of for
					List<Integer> sortedCats = sortCategoryIdBasedOnScore(categoryScoreMap);
					List<Integer> sortedCatsSubList = new ArrayList<Integer>();
					Set<Integer> topCategories = new HashSet<Integer>();
					Set<String> topCategoriesNames = new HashSet<String>();
					int k = 1;
					System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					for (int catId : sortedCats) {
						if (categoryEntityLinkMap.get(catId) >= MaxNumOfLinksPerCategory) {
							continue;
						}else {
							String catName = vlp.getValue(catId);
							topCategoriesNames.add(catName);
							System.out.println("k: " + k + " Category: " + catName + "   Score: " + (categoryScoreMap.get(catId) / categoryScoreMap.get(sortedCats.get(0))) + " Coverage: " + categoryCoverageMap.get(catId) + " links: " + categoryEntityLinkMap.get(catId));
							topCategories.add(catNamesToInt.get(catName));
							sortedCatsSubList.add(catId);
							k++;
							if (k == 31) {
								break;
							}
						} // end of if
					} // end of for
					System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");
					
					// This approach is based on the tfidf of the categories words and the top K categories
					/*
					Map<String,Integer> tfOfWords = findFrequencyOfWords(topCategoriesNames, stopWords);
					System.out.println("tfOfWords: " + tfOfWords);
					Indexer indexer = new Indexer();
					IndexReader reader = DirectoryReader.open(indexer.getIndex());
					IndexSearcher searcher = new IndexSearcher(reader);
					int totalNumOfCategories = catNames.size();
					Map<String,Double> tfIdfOfTopCategoryWords = calculateTfIdfOfCategoryWords(tfOfWords, totalNumOfCategories, indexer, searcher);
					System.out.println("\n tfIdfOfWords: " + tfIdfOfTopCategoryWords);
					String cat1 = confObject.get("category").getAsString();
					int catId1 = catNamesToInt.get(cat1);
					Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, catId1);
					System.out.println("===> user category size: " + userCategory.size());
					Set<Integer> intersection = CategoryProcessor.getIntersection(userCategory, topCategories);
					System.out.println("intersection: " + intersection);
					userCategory.clear();
					for (int cid : intersection) {
						userCategory.addAll(CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, cid));
					}
					System.out.println("===> after: user category size: " + userCategory.size());
					
					Map<String,Double> tfIdfOfUserCategoryWords = calcualteTfIdfOfUserCategory(userCategory, totalNumOfCategories, indexer, searcher);
					double semanticSimilarity = calculateCosineSimilarity(tfIdfOfTopCategoryWords, tfIdfOfUserCategoryWords);
					System.out.println("Cosine Semantic Similarity: " + semanticSimilarity);
					*/
					String cat1 = confObject.get("category1").getAsString();
					String cat2 = confObject.get("category2").getAsString();
					System.out.println("User Categories: " + cat1 + "    " + cat2);
					int cid1 = catNamesToInt.get(cat1) != null ? catNamesToInt.get(cat1) : -1;
					int cid2 = catNamesToInt.get(cat2) != null ? catNamesToInt.get(cat2) : -1;
					List<Integer> userCatIds = new ArrayList<Integer>();
					List<String> userCatNames = new ArrayList<String>();
					if (cid1 == -1) {
						System.out.println("User Category \"" + cat1 + "\" does NOT exist in Wikipedia. Please enter a valid Category.");
						continue;
					} // end of if
					userCatIds.add(cid1);
					userCatNames.add(cat1);
					if (cid2 == -1) {
						System.out.println("User Category \"" + cat2 + "\" does NOT exist in Wikipedia. Please enter a valid Category.");
					}else {
						userCatIds.add(cid2);
						userCatNames.add(cat2);
					} // end of if
					List<Double>  finalAvgRelatedness = new ArrayList<Double>();
					List<String>  intersectionCatNames = null;
					for (int counter = 0;counter < userCatIds.size();counter++) {
						Set<Integer> parentGraph = CategoryProcessor.categoryTreeFromChildToParent(wikiTaxonomyMap_ID_FromChildToParent, userCatIds.get(counter));
						Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, userCatIds.get(counter), parentGraph);
						//					Set<Integer> userCategory = CategoryProcessor.categoryTree(wikiTaxonomyMapFromParentToChild, catId1);
						Set<Integer> intersection = CategoryProcessor.getIntersection(userCategory, topCategories);
						intersectionCatNames = new ArrayList<String>();
						System.out.println("\n=============== Intersection Categories Start ===============\n ");
						for (int id : intersection) {
							intersectionCatNames.add(catNames.get(id));
							System.out.println("catId: " + userCatNames.get(counter) + "   CatName: " + catNames.get(id));
						} // end of for
						System.out.println("\n=============== Intersection Categories End   =============== ");
						Map<Integer,Double> finalSRelatednessMap = new HashMap<Integer,Double>();
						Map<Integer,Set<Integer>> coreEntityLinks = new HashMap<Integer,Set<Integer>>(coreEntities.size());
						//					for (int entId : contextEntities) {
						for (int entId : coreEntities) {
							String entName = localNames.get(entId);
							coreEntityLinks.put(entId, findLinkedEntities(entName, virtGraph));
						} // end of for
						if (intersection.isEmpty()) {
							intersectionCatNames.add(userCatNames.get(counter));
							System.out.println("catId: " + counter + "   CatName: " + userCatNames.get(counter));
						} // end of if
						for (String category : intersectionCatNames) {
							Map<Integer,Double> sRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, coreEntities, coreEntityLinks, virtGraph);
							//						Map<Integer,Double> sRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, contextEntities, coreEntityLinks, virtGraph);
							System.out.println("\n=============== Semantic Relatedness of Core Entities and category \"" + category +"\" Start ===============\n");
							for (Map.Entry<Integer, Double> entry : sRelatednessMap.entrySet()) {
								int entId = entry.getKey();
								double sr = entry.getValue();
								System.out.println("entId: " + entId + "   entity: " + localNames.get(entId) + "    sr: " + sr);
								double preVal = finalSRelatednessMap.get(entId) != null ? finalSRelatednessMap.get(entId) : 0;
								finalSRelatednessMap.put(entId, Math.max(preVal, sr));
								//							avgSRelatednessMap.put(entId, preVal + sr);
							} // end of for
							System.out.println("\n=============== Semantic Relatedness of Core Entities and category \"" + category +"\" End   ===============\n");
						} // end of for
						//					for (Map.Entry<Integer, Double> entry : avgSRelatednessMap.entrySet()) {
						//						int entId = entry.getKey();
						//						double sr = entry.getValue();
						//						sr /= intersectionCatNames.size();
						//						avgSRelatednessMap.put(entId, sr);
						//					} // end of for
						//					System.out.println("\n============> Final Relatedness of Core Entities: ");
						//					double sumOfSr = 0;
						//					for (Map.Entry<Integer, Double> entry : avgSRelatednessMap.entrySet()) {
						//						int entId = entry.getKey();
						//						double sr = entry.getValue();
						//						System.out.println("entId: " + entId + "  Name: " + localNames.get(entId) + "    sr: " + sr);
						//						sumOfSr += sr;
						//					} // end of for
						//					System.out.println("AVERAGE relatedness of Document to User's Category of Interest is: " + (sumOfSr / avgSRelatednessMap.size()));

						System.out.println("\n\n\n************ FINAL Semantic Relatedness of Core Entities ************\n ");
						List<Integer> sortedRelatedness = sortBasedOnScore(finalSRelatednessMap);

						// pick the top K semantic relatedness and get the average as the relatedness of the document to user's category of interest
						List<Integer> sortedTopKRelatedness = sortedRelatedness.size() >= 9 ? new ArrayList<Integer>(sortedRelatedness.subList(0, 9)) : sortedRelatedness;
						for (int entId : sortedRelatedness) {
							System.out.println("entId: " + entId + "  Name: " + localNames.get(entId) + "    sr: " + finalSRelatednessMap.get(entId));
						} // end of for
						double sumOfSr = 0;
						for (int entId : sortedTopKRelatedness) {
							sumOfSr += finalSRelatednessMap.get(entId);
						} // end of for
						double avgSR = sumOfSr /sortedTopKRelatedness.size(); 
						System.out.println("\n\nAVERAGE Relatedness of Document to User's Category of Interest is: " + avgSR);
						finalAvgRelatedness.add(avgSR);
						double srThreshold = 0.24;
						if (avgSR >= srThreshold) {
							System.out.println("\n*************** Document belongs to the category \"" + userCatNames.get(counter) + "\"");
						}else {
							System.out.println("\n*************** Document does NOT belongs to the category \"" + userCatNames.get(counter) + "\"");
						} // end of if
					} // end of for userCatIds
					
					for (int c = 0 ;c < userCatNames.size();c++) {
						System.out.println("\n\n Relatedness of Document to \"" + userCatNames.get(c) + "\" : " + finalAvgRelatedness.get(c));
					} // end of for

//					System.out.println("Intersection size: " + intersection.size());
////					double p = calculateProbabilityOfCategoryGivenTheDocument(intersection, catId1, userCategory, topCategories);
//					double p = calculateProbabilityOfDocumentBelongnessToCategory(intersectionCatNames, sortedCatsSubList, categoryScoreMap, vlp);
//					System.out.println("\n The belongness of document to \"" + cat1 + "\" Category is: " + p);
					
					
					




					//				
					//				// This approach is based on TextRank algorithm

					//				Map<Integer,Double> categoryScoreMap1 = categoryRandomWalkScore(taxonomyGraph, coreEntities);
					//				Map<Integer,Double> categoryScoreMap2 = new HashMap<Integer,Double>();
					//				double beta = 0.65;
					//				for (int catId : taxonomyVerticies) {
					//					if (!coreEntities.contains(catId)) {
					//						String category = vlp.getValue(catId);
					//						double score1 = categoryScoreMap1.get(catId);
					//						for (int entId : coreEntities) {  // ONLY for core entities 
					//////					for (int entId : allEntities) {   // for all entities
					//							BFSResult bfsTree = taxonomyGraph.bfs(entId);
					//							calculateCategoryScore(taxonomyGraph, bfsTree, entId, catId, category, categoryScoreMap2);
					//						} // end of for
					//						double score2 = categoryScoreMap2.get(catId);
					//						double totalScore = (beta * score1) + (1 - beta) * score2;
					//						categoryScoreMap2.put(catId, totalScore);
					//					} // end of if
					//				} // end of for
					//				List<Integer> sortedCats = sortCategoryIdBasedOnScore(categoryScoreMap2);
					//				System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					//				for (int catId : sortedCats) {
					//					System.out.println("Category: " + vlp.getValue(catId) + "   Score: " + categoryScoreMap2.get(catId));
					//				}
					//				System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");




					// output = new FileOutputStream("thematicGraph.png");
					// imgWriter.writeGraph(semanticGraph,
					// GraphvizImageWriter.COMMAND.dot,
					// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
					// output.flush();
					// output.close();
					/*
					 * System.out.println(
					 * "Finding the Second Largest Connected Component...");
					 * // SemanticGraph secondLargestCCGraph =
					 * findSecondLargestConnectedComponent(semanticGraph,
					 * largestCComponent);
					 * SemanticGraph secondLargestCCGraph =
					 * findSecondLargestConnectedComponent(semanticGraph, key);
					 * System.out.println("Finding Done!\n	");
					 * if (!secondLargestCCGraph.isNull()) {
					 * System.out.println(
					 * "Creating \"secondThematicGraph.dot\" file on disk...");
					 * dotFile = new FileWriter("secondThematicGraph.dot");
					 * dotFile.write(secondLargestCCGraph.toDot());
					 * dotFile.close();
					 * System.out.println(
					 * "\"secondThematicGraph.dot\" file Successfully created.\n");
					 * // output = new FileOutputStream("secondThematicGraph.png");
					 * // imgWriter.writeGraph(semanticGraph,
					 * GraphvizImageWriter.COMMAND.dot,
					 * GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
					 * // output.flush();
					 * // output.close();
					 * } // end of if
					 */
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("\n\nType \"quit\" to exit OR Type name of the file to continue: ");
			while (true) {
				fileName = sc.nextLine();
				if (!fileName.equalsIgnoreCase("quit")) {
					document = readDocument("/home/mehdi/" + fileName);
					if (!document.equals("")) {
						break;
					}else {
						continue;
					}
				} // end of if
				break;
			} // end of while
		} while (!fileName.equalsIgnoreCase("quit"));
		sc.close();
	} // end of recognizeEntites
	
	public void createSemanticGraphFromEntities_batch() {
		long stTime = System.currentTimeMillis();
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		Map<String, Set<Entity>> uniqueNames = DbProcessor.loadUniqueNameListToMemory();
		LocalNameMapperClass localNamesMapperClass = DbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
		localNames = localNamesMapperClass.getLocalNameList();
		namesToInt = localNamesMapperClass.getNameToInt();
		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		wikiTaxonomyMapFromChildToParent = DbProcessor.loadWikiTaxonomyMapFromChildToParentToMemory();
//		wikiTaxonomyMap_ID_FromChildToParent = DictionaryGenerator.loadWikiTaxonomyMap_ID_FromChildToParentToMemory();
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
//		wikiTaxonomyMapFromParentToChild = DictionaryGenerator.loadWikiTaxonomyMapFromParentToChildToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> catNames = catNameMapper.getCatNameList();
		System.out.println("Total Load Time: " + (System.currentTimeMillis() - stTime));
		// List<String> wlist = wordMapperClass.getWordList();
		Set<String> stopWords = readStopWords();
		Set<String> timeRelatedWords = readTimeRelatedEntities();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
//			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "400-docs-T20-result.txt");
			FileHandler fh = new FileHandler(EXPT_DIR_PATH + "1414-docs-T20-result.txt");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			File rssDirectory = new File(RSS_DIR_PATH);
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(RSS_DIR_PATH + fileName);
				List<String> docEntities = new ArrayList<String>(Arrays.asList(document.split("\n")));
				Map<String,Double> entityWeightMap = new HashMap<String,Double>();
				for (String e : docEntities) {
					String [] ew = e.split("\t");
					entityWeightMap.put(ew[0], Double.valueOf(ew[1]));
				} // end of for
				JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
				JsonParser jsonParser = new JsonParser();
				JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
				SemanticGraph semanticGraph = new SemanticGraph();
				semanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				LabelProperty vertexLabelProperty = semanticGraph.getVertexLabelProperty();
				int intialCapacity = confObject.get("initialCapacity").getAsInt();
				entityCategoryMap = new HashMap<Integer, Set<String>>(intialCapacity);
				stTime = System.currentTimeMillis();
				Set<String> foundEntities = new HashSet<String>(entityWeightMap.keySet());
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				// System.out.println("Number of Entities AFTER removing Stop Words: "
				// + foundEntities.size() + "\n");
				System.out.println("\n=============== Revised Recognized Entities Start ================");
				System.out.println("Size: " + foundEntities.size());
				System.out.println(foundEntities);
				System.out.println("\n=============== Revised Recognized Entities End   ================\n");



				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
				int ecounter = 0;
				while (it.hasNext()) {
					String enName = it.next();
					if (namesToInt.get(enName) == null) {
						continue;
					}
					int enId = namesToInt.get(enName);
					double entityWeight = entityWeightMap.get(enName);
					//						System.out.println("Entity Mention: " + en);
					addVertexToSemanticGraph(semanticGraph, enId, enName, vertexLabelProperty);
					IntSet set = new HashIntSet();
					set.add(enId);
					ccVertexSets.put(enId, set);
					wikipediaEntities.put(enName, enId);
					calculateVertexInitialWeight(semanticGraph, enId, entityWeight, 1);
					//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
					//							System.out.println();
				} // end of while
				//				System.out.println("\nTotal number of Wikipedia Entities including all attributes: " + ecounter);

				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);
				System.out.println("Total time to construct the Semantic Graph: " + (System.currentTimeMillis() - stTime));
				System.out.println("Creating \"semanticGraph.dot\" file on disk...");
				FileWriter dotFile = new FileWriter("semanticGraph.dot");
				dotFile.write(semanticGraph.toDot());
				dotFile.close();
				System.out.println("\"semanticGraph.dot\" file Successfully created.\n");
				// FileOutputStream output = new
				// FileOutputStream("semanticGraph.png");
				// imgWriter.writeGraph(semanticGraph,
				// GraphvizImageWriter.COMMAND.dot,
				// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
				// output.flush();
				// output.close();
				SemanticGraph thematicSemanticGraph = new SemanticGraph();
				thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				stTime = System.currentTimeMillis();
				IntSet largestCComponent = null;
				int max = 0;
				int key = 0;
				for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
					if (entry.getValue().size() > max) {
						max = entry.getValue().size();
						key = entry.getKey();
					} // end of if
				} // end of for
				largestCComponent = ccVertexSets.get(key);
				thematicSemanticGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(largestCComponent);
				addLabelToGraphVertices(thematicSemanticGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
				System.out.println("\nTotal time to construct the Thematic Graph: " + (System.currentTimeMillis() - stTime));
				thematicSemanticGraph.setVertexWeightProperty(semanticGraph.getVertexWeightProperty());
				System.out.println("Creating \"thematicGraph.dot\" file on disk...");
				dotFile = new FileWriter("thematicGraph.dot");
				dotFile.write(thematicSemanticGraph.toDot());
				dotFile.close();
				System.out.println("\"thematicGraph.dot\" file Successfully created.\n");
				int[] vertices = thematicSemanticGraph.getVertices().toIntArray();
				int numOfSteps = confObject.get("numOfSteps").getAsInt();
				JsonObject relationImportanceWeightObj = confObject.get("relationImportanceWeight").getAsJsonObject();
				runHITSalgorithm(thematicSemanticGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
				double authoritativeThreshold = confObject.get("authoritativeThreshold").getAsDouble();
				Set<Integer> authoritativeEntities = new HashSet<Integer>(100);
				Set<Integer> nonAuthEntities = new HashSet<Integer>(100);
				Set<Integer> allEntities = new HashSet<Integer>(100);
				//				System.out.println("============== CC Vertices Weights Start ==============\n");
				int i = 1;
				for (int vertex : vertices) {
					String ename = localNames.get(vertex);
					double vw = thematicSemanticGraph.getVertexWeight(vertex);
					if (vw >= authoritativeThreshold) {
						authoritativeEntities.add(vertex);
					} else {
						nonAuthEntities.add(vertex);
					} // end of if
					//					System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
					//					i++;
				} // end of for
				//				System.out.println("============== CC Vertices Weights End ==============\n");
				allEntities.addAll(authoritativeEntities);
				allEntities.addAll(nonAuthEntities);
				//				System.out.println("============== Authoritative Entities Start ==============\n");
				i = 1;
				List<Integer> sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicSemanticGraph.getVertexWeightProperty());
				for (int vid : sortedAuth) {
					String ename = localNames.get(vid);
					double vw = thematicSemanticGraph.getVertexWeight(vid);
					//					System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
					//					i++;
				}
				//				System.out.println("\n============== Authoritative Entities End ==============\n");
				SemanticGraph thematicSemanticGraphClone = (SemanticGraph) thematicSemanticGraph.clone();
				thematicSemanticGraphClone.getClass().getClassLoader().setDefaultAssertionStatus(false);
				removeLowWeightVerticies(thematicSemanticGraphClone, nonAuthEntities);
				dotFile = new FileWriter("thematicGraph2.dot");
				dotFile.write(thematicSemanticGraphClone.toDot());
				dotFile.close();
				largestCComponent = thematicSemanticGraphClone.getLargestConnectedComponent();
				// SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraph.getSubgraphInducedByVertices(largestCComponent);
				if (largestCComponent != null){
					SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraphClone.getSubgraphInducedByVertices(largestCComponent);
					thematicGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
					addLabelToGraphVertices(thematicGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
					thematicGraph.setVertexWeightProperty(thematicSemanticGraph.getVertexWeightProperty());
					vertices = thematicGraph.getVertices().toIntArray();
					runHITSalgorithm(thematicGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
					authoritativeEntities = new HashSet<Integer>(100);
					nonAuthEntities = new HashSet<Integer>(100);
					System.out.println("============== CC Vertices Weights Start ==============\n");
					i = 1;
					for (int vertex : vertices) {
						String ename = localNames.get(vertex);
						double vw = thematicGraph.getVertexWeight(vertex);
						if (vw >= authoritativeThreshold) {
							authoritativeEntities.add(vertex);
						} else {
							nonAuthEntities.add(vertex);
						} // end of if
						System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
					System.out.println("============== CC Vertices Weights End ==============\n");
					System.out.println("============== Authoritative Entities Start ==============\n");
					i = 1;
					sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicGraph.getVertexWeightProperty());
					for (int vid : sortedAuth) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
					System.out.println("\n============== Authoritative Entities End ==============\n");
					double centralityThreshold = confObject.get("centralityThreshold").getAsDouble();
					Map<Integer, Double> centralEntities = getCentralEntities((SemanticGraph) thematicGraph.toUndirectedGraph(), authoritativeEntities, centralityThreshold);
					System.out.println("============== Central Entities Start ==============\n");
					i = 1;
					List<Integer> sortedCentral = sortBasedOnScore(centralEntities);
					for (int vid : sortedCentral) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
					System.out.println("\n============== Central Entities End ==============\n");

					int topAuthoritativeEntities = confObject.get("topK-AuthoritativeEntities").getAsInt();
					int topCentralEntities = confObject.get("topK-CentralEntities").getAsInt();
					Set<Integer> coreEntities = new HashSet<Integer>();
					if (topAuthoritativeEntities >= sortedAuth.size()) {
						coreEntities.addAll(sortedAuth);
					}else {
						for (int counter = 0;counter < sortedAuth.size(); counter++) {
							if (counter == topAuthoritativeEntities) {
								break;
							}else {
								coreEntities.add(sortedAuth.get(counter));
							} // end of if
						} // end of for
					} // end of if
					if (topCentralEntities >= sortedCentral.size()) {
						coreEntities.addAll(sortedCentral);
					}else {
						for (int counter = 0;counter < sortedCentral.size(); counter++) {
							if (counter == topCentralEntities) {
								break;
							}else {
								coreEntities.add(sortedCentral.get(counter));
							} // end of if
						} // end of for
					} // end of if

					System.out.println("============== Core Entities Start ==============\n");
					i = 1;
					for (int vid : coreEntities) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
					System.out.println("\n============== Core Entities End ==============\n");
					//					IntSet vList = thematicSemanticGraph.getVertices();
					IntSet vList = semanticGraph.getVertices();
					int categoryhierarchyLevel = confObject.get("categoryhierarchyLevel").getAsInt();

					// Find Core and Context Entities for constructing the taxonomy Graph
					//					Set<Integer> contextEntities = findCoreContextEntities(thematicSemanticGraph, coreEntities);
					Set<Integer> contextEntities = findCoreContextEntities(semanticGraph, coreEntities);

					// It is used to create  YAGO Taxonomy Graph for ALL categories of Core and Context entities
					//					SemanticGraph taxonomyGraph = constructYagoTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap);

					// It is used to create Wikipedia Taxonomy Graph for ONLY categories of core entities 
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (coreEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of ALL entities
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (allEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of Core and Context entities
					SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap, categoryhierarchyLevel);
					dotFile = new FileWriter("taxonomyGraph.dot");
					dotFile.write(taxonomyGraph.toDot());
					dotFile.close();

					double alpha = confObject.get("alpha").getAsDouble();
					LabelProperty vlp = taxonomyGraph.getVertexLabelProperty();
					taxonomyGraph = (SemanticGraph) taxonomyGraph.toUndirectedGraph();
					int [ ] taxonomyVerticies = taxonomyGraph.getVertices().toIntArray();
					System.out.println("TaxonomyVerticies size: " + taxonomyVerticies.length);
					int [ ] vlabelIds = vlp.getIDs().toIntArray();
					Map<Integer,Integer> categoryEntityLinkMap = new HashMap<Integer,Integer>();
					for(int catId : taxonomyVerticies) {
						if (!contextEntities.contains(catId)) {
							String categoryName = vlp.getValue(catId);
							int numOfLinks = findNumberOfCategoryEntities(categoryName, virtGraph);
							categoryEntityLinkMap.put(catId, numOfLinks);
						} // end of if
					} // end of for

					System.out.println("Processing links... done!");

					// This approach is same as previous one, but ONLY for top K categories, and ONLY for core entities 
					Map<Integer,Double> categoryScoreMap = new HashMap<Integer,Double>();
					Map<Integer,Double> categoryCoverageMap = new HashMap<Integer,Double>();
					for (int entId : contextEntities) {  // ONLY for core entities 
						//					for (int entId : allEntities) {   // for all entities
						BFSResult bfsTree = taxonomyGraph.bfs(entId);
						String entame = localNames.get(entId);
						double entWeight = entityWeightMap.get(entame);
						for (int catId : taxonomyVerticies) {
							if (!contextEntities.contains(catId)) {
								String category = vlp.getValue(catId);
								calculateCategoryScoreWithWeight(taxonomyGraph, bfsTree, entId, entWeight, catId, category, categoryScoreMap, categoryCoverageMap);
							}
						} // end of for
					} // end of for
					Set<Integer> keySet = categoryScoreMap.keySet();
					double sumOfScores = 0;
					for (int id : keySet) {
						double score1 = categoryScoreMap.get(id);
						double score2 = categoryCoverageMap.get(id);
						double finalScore = ((alpha * score1) / contextEntities.size()) + (1 - alpha) * score2;
						sumOfScores += finalScore;
						categoryScoreMap.put(id, finalScore);
					} // end of for
					List<Integer> sortedCats = sortCategoryIdBasedOnScore(categoryScoreMap);
					List<Integer> sortedCatsSubList = new ArrayList<Integer>();
					Set<Integer> topCategories = new HashSet<Integer>();
					Set<String> topCategoriesNames = new HashSet<String>();
					int k = 1;
					System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					String logMsg = "";
					logMsg += "\n========== Topic " + fileName + " ============\n";
					for (int catId : sortedCats) {
						if (categoryEntityLinkMap.get(catId) >= MaxNumOfLinksPerCategory) {
							continue;
						}else {
							String catName = vlp.getValue(catId);
							topCategoriesNames.add(catName);
							System.out.println("k: " + k + " Category: " + catName + "   Score: " + (categoryScoreMap.get(catId) / sumOfScores));
							logMsg +="k: " + k + " Category: " + catName + "   Score: " + (categoryScoreMap.get(catId) / sumOfScores) + "\n";
							topCategories.add(catNamesToInt.get(catName));
							sortedCatsSubList.add(catId);
							k++;
							if (k == 31) {
								break;
							}
						} // end of if
					} // end of for
					System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");
					logger.log(Level.INFO, "\n" + logMsg);

					// output = new FileOutputStream("thematicGraph.png");
					// imgWriter.writeGraph(semanticGraph,
					// GraphvizImageWriter.COMMAND.dot,
					// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
					// output.flush();
					// output.close();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createSemanticGraphFromEntities_batch
	
	public void createSemanticGraphFromEntities() {
		long stTime = System.currentTimeMillis();
		DbpediaProcessor DbProcessor = new DbpediaProcessor();
		Map<String, Set<Entity>> uniqueNames = DbProcessor.loadUniqueNameListToMemory();
		LocalNameMapperClass localNamesMapperClass = DbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
		localNames = localNamesMapperClass.getLocalNameList();
		namesToInt = localNamesMapperClass.getNameToInt();
		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		wikiTaxonomyMapFromChildToParent = DbProcessor.loadWikiTaxonomyMapFromChildToParentToMemory();
//		wikiTaxonomyMap_ID_FromChildToParent = DictionaryGenerator.loadWikiTaxonomyMap_ID_FromChildToParentToMemory();
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
//		wikiTaxonomyMapFromParentToChild = DictionaryGenerator.loadWikiTaxonomyMapFromParentToChildToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> catNames = catNameMapper.getCatNameList();
		System.out.println("Total Load Time: " + (System.currentTimeMillis() - stTime));
		// List<String> wlist = wordMapperClass.getWordList();
		Scanner sc = new Scanner(System.in);
		Set<String> stopWords = readStopWords();
		Set<String> timeRelatedWords = readTimeRelatedEntities();
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		String fileName = "/home/mehdi/document.txt";
		String document = readDocument(fileName);
		do {
			try {
				List<String> docEntities = new ArrayList<String>(Arrays.asList(document.split("\n")));
				Map<String,Double> entityWeightMap = new HashMap<String,Double>();
				for (String e : docEntities) {
					String [] ew = e.split("\t");
					entityWeightMap.put(ew[0], Double.valueOf(ew[1]));
				} // end of for
				JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
				JsonParser jsonParser = new JsonParser();
				JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
				SemanticGraph semanticGraph = new SemanticGraph();
				semanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				LabelProperty vertexLabelProperty = semanticGraph.getVertexLabelProperty();
				int intialCapacity = confObject.get("initialCapacity").getAsInt();
				entityCategoryMap = new HashMap<Integer, Set<String>>(intialCapacity);
				stTime = System.currentTimeMillis();
				Set<String> foundEntities = new HashSet<String>(entityWeightMap.keySet());
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				// System.out.println("Number of Entities AFTER removing Stop Words: "
				// + foundEntities.size() + "\n");
				System.out.println("\n=============== Revised Recognized Entities Start ================");
				System.out.println("Size: " + foundEntities.size());
				System.out.println(foundEntities);
				System.out.println("\n=============== Revised Recognized Entities End   ================\n");
				
				
				
				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
				int ecounter = 0;
				while (it.hasNext()) {
					String enName = it.next();
					int enId = namesToInt.get(enName);
					double entityWeight = entityWeightMap.get(enName);
					//						System.out.println("Entity Mention: " + en);
					addVertexToSemanticGraph(semanticGraph, enId, enName, vertexLabelProperty);
					IntSet set = new HashIntSet();
					set.add(enId);
					ccVertexSets.put(enId, set);
					wikipediaEntities.put(enName, enId);
					calculateVertexInitialWeight(semanticGraph, enId, entityWeight, 1);
					//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
					//							System.out.println();
				} // end of while
//				System.out.println("\nTotal number of Wikipedia Entities including all attributes: " + ecounter);

				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);
				System.out.println("Total time to construct the Semantic Graph: " + (System.currentTimeMillis() - stTime));
				System.out.println("Creating \"semanticGraph.dot\" file on disk...");
				FileWriter dotFile = new FileWriter("semanticGraph.dot");
				dotFile.write(semanticGraph.toDot());
				dotFile.close();
				System.out.println("\"semanticGraph.dot\" file Successfully created.\n");
				// FileOutputStream output = new
				// FileOutputStream("semanticGraph.png");
				// imgWriter.writeGraph(semanticGraph,
				// GraphvizImageWriter.COMMAND.dot,
				// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
				// output.flush();
				// output.close();
				SemanticGraph thematicSemanticGraph = new SemanticGraph();
				thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
				stTime = System.currentTimeMillis();
				IntSet largestCComponent = null;
				int max = 0;
				int key = 0;
				for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
					if (entry.getValue().size() > max) {
						max = entry.getValue().size();
						key = entry.getKey();
					} // end of if
				} // end of for
				largestCComponent = ccVertexSets.get(key);
				thematicSemanticGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(largestCComponent);
				addLabelToGraphVertices(thematicSemanticGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
				System.out.println("\nTotal time to construct the Thematic Graph: " + (System.currentTimeMillis() - stTime));
				thematicSemanticGraph.setVertexWeightProperty(semanticGraph.getVertexWeightProperty());
				System.out.println("Creating \"thematicGraph.dot\" file on disk...");
				dotFile = new FileWriter("thematicGraph.dot");
				dotFile.write(thematicSemanticGraph.toDot());
				dotFile.close();
				System.out.println("\"thematicGraph.dot\" file Successfully created.\n");
				int[] vertices = thematicSemanticGraph.getVertices().toIntArray();
				int numOfSteps = confObject.get("numOfSteps").getAsInt();
				JsonObject relationImportanceWeightObj = confObject.get("relationImportanceWeight").getAsJsonObject();
				runHITSalgorithm(thematicSemanticGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
				double authoritativeThreshold = confObject.get("authoritativeThreshold").getAsDouble();
				Set<Integer> authoritativeEntities = new HashSet<Integer>(100);
				Set<Integer> nonAuthEntities = new HashSet<Integer>(100);
				Set<Integer> allEntities = new HashSet<Integer>(100);
//				System.out.println("============== CC Vertices Weights Start ==============\n");
				int i = 1;
				for (int vertex : vertices) {
					String ename = localNames.get(vertex);
					double vw = thematicSemanticGraph.getVertexWeight(vertex);
					if (vw >= authoritativeThreshold) {
						authoritativeEntities.add(vertex);
					} else {
						nonAuthEntities.add(vertex);
					} // end of if
//					System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
//					i++;
				} // end of for
//				System.out.println("============== CC Vertices Weights End ==============\n");
				allEntities.addAll(authoritativeEntities);
				allEntities.addAll(nonAuthEntities);
//				System.out.println("============== Authoritative Entities Start ==============\n");
				i = 1;
				List<Integer> sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicSemanticGraph.getVertexWeightProperty());
				for (int vid : sortedAuth) {
					String ename = localNames.get(vid);
					double vw = thematicSemanticGraph.getVertexWeight(vid);
//					System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
//					i++;
				}
//				System.out.println("\n============== Authoritative Entities End ==============\n");
				SemanticGraph thematicSemanticGraphClone = (SemanticGraph) thematicSemanticGraph.clone();
				thematicSemanticGraphClone.getClass().getClassLoader().setDefaultAssertionStatus(false);
				removeLowWeightVerticies(thematicSemanticGraphClone, nonAuthEntities);
				dotFile = new FileWriter("thematicGraph2.dot");
				dotFile.write(thematicSemanticGraphClone.toDot());
				dotFile.close();
				largestCComponent = thematicSemanticGraphClone.getLargestConnectedComponent();
				// SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraph.getSubgraphInducedByVertices(largestCComponent);
				if (largestCComponent != null){
					SemanticGraph thematicGraph = (SemanticGraph) thematicSemanticGraphClone.getSubgraphInducedByVertices(largestCComponent);
					thematicGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
					addLabelToGraphVertices(thematicGraph, semanticGraph.getVertexLabelProperty(), largestCComponent);
					thematicGraph.setVertexWeightProperty(thematicSemanticGraph.getVertexWeightProperty());
					vertices = thematicGraph.getVertices().toIntArray();
					runHITSalgorithm(thematicGraph, vertices, numOfSteps, relationImportanceWeightObj, virtGraph);
					authoritativeEntities = new HashSet<Integer>(100);
					nonAuthEntities = new HashSet<Integer>(100);
					System.out.println("============== CC Vertices Weights Start ==============\n");
					i = 1;
					for (int vertex : vertices) {
						String ename = localNames.get(vertex);
						double vw = thematicGraph.getVertexWeight(vertex);
						if (vw >= authoritativeThreshold) {
							authoritativeEntities.add(vertex);
						} else {
							nonAuthEntities.add(vertex);
						} // end of if
						System.out.println("i: " + i + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
					System.out.println("============== CC Vertices Weights End ==============\n");
					System.out.println("============== Authoritative Entities Start ==============\n");
					i = 1;
					sortedAuth = sortBasedOnWeight(authoritativeEntities, thematicGraph.getVertexWeightProperty());
					for (int vid : sortedAuth) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw);
						i++;
					} // end of for
					System.out.println("\n============== Authoritative Entities End ==============\n");
					double centralityThreshold = confObject.get("centralityThreshold").getAsDouble();
					Map<Integer, Double> centralEntities = getCentralEntities((SemanticGraph) thematicGraph.toUndirectedGraph(), authoritativeEntities, centralityThreshold);
					System.out.println("============== Central Entities Start ==============\n");
					i = 1;
					List<Integer> sortedCentral = sortBasedOnScore(centralEntities);
					for (int vid : sortedCentral) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
					System.out.println("\n============== Central Entities End ==============\n");

					int topAuthoritativeEntities = confObject.get("topK-AuthoritativeEntities").getAsInt();
					int topCentralEntities = confObject.get("topK-CentralEntities").getAsInt();
					Set<Integer> coreEntities = new HashSet<Integer>();
					if (topAuthoritativeEntities >= sortedAuth.size()) {
						coreEntities.addAll(sortedAuth);
					}else {
						for (int counter = 0;counter < sortedAuth.size(); counter++) {
							if (counter == topAuthoritativeEntities) {
								break;
							}else {
								coreEntities.add(sortedAuth.get(counter));
							} // end of if
						} // end of for
					} // end of if
					if (topCentralEntities >= sortedCentral.size()) {
						coreEntities.addAll(sortedCentral);
					}else {
						for (int counter = 0;counter < sortedCentral.size(); counter++) {
							if (counter == topCentralEntities) {
								break;
							}else {
								coreEntities.add(sortedCentral.get(counter));
							} // end of if
						} // end of for
					} // end of if

					System.out.println("============== Core Entities Start ==============\n");
					i = 1;
					for (int vid : coreEntities) {
						String ename = localNames.get(vid);
						double vw = thematicGraph.getVertexWeight(vid);
						double centrality = centralEntities.get(vid);
						System.out.println("i: " + i + "  vid: " + vid + "  ename: " + ename + "  weight: " + vw + "  Centrality: " + centrality);
						i++;
					} // end of for
					System.out.println("\n============== Core Entities End ==============\n");
//					IntSet vList = thematicSemanticGraph.getVertices();
					IntSet vList = semanticGraph.getVertices();
					int categoryhierarchyLevel = confObject.get("categoryhierarchyLevel").getAsInt();

					// Find Core and Context Entities for constructing the taxonomy Graph
//					Set<Integer> contextEntities = findCoreContextEntities(thematicSemanticGraph, coreEntities);
					Set<Integer> contextEntities = findCoreContextEntities(semanticGraph, coreEntities);
					
					// It is used to create  YAGO Taxonomy Graph for ALL categories of Core and Context entities
//					SemanticGraph taxonomyGraph = constructYagoTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap);

					// It is used to create Wikipedia Taxonomy Graph for ONLY categories of core entities 
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (coreEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of ALL entities
					//				SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (allEntities), entityCategoryMap, categoryhierarchyLevel);

					// It is used to create  Wikipedia Taxonomy Graph for ALL categories of Core and Context entities
					SemanticGraph taxonomyGraph = constructWikiTaxonomicGraph(new ArrayList<Integer> (contextEntities), entityCategoryMap, categoryhierarchyLevel);
					dotFile = new FileWriter("taxonomyGraph.dot");
					dotFile.write(taxonomyGraph.toDot());
					dotFile.close();

					double alpha = confObject.get("alpha").getAsDouble();
					LabelProperty vlp = taxonomyGraph.getVertexLabelProperty();
					taxonomyGraph = (SemanticGraph) taxonomyGraph.toUndirectedGraph();
					int [ ] taxonomyVerticies = taxonomyGraph.getVertices().toIntArray();
					System.out.println("TaxonomyVerticies size: " + taxonomyVerticies.length);
					int [ ] vlabelIds = vlp.getIDs().toIntArray();
					Map<Integer,Integer> categoryEntityLinkMap = new HashMap<Integer,Integer>();
					for(int catId : taxonomyVerticies) {
						if (!contextEntities.contains(catId)) {
							String categoryName = vlp.getValue(catId);
							int numOfLinks = findNumberOfCategoryEntities(categoryName, virtGraph);
							categoryEntityLinkMap.put(catId, numOfLinks);
						} // end of if
					} // end of for

					System.out.println("Processing links... done!");

					// This approach is same as previous one, but ONLY for top K categories, and ONLY for core entities 
					Map<Integer,Double> categoryScoreMap = new HashMap<Integer,Double>();
					Map<Integer,Double> categoryCoverageMap = new HashMap<Integer,Double>();
//					Map<Integer,Integer> categoryCoverageMap = new HashMap<Integer,Integer>();
					for (int entId : contextEntities) {  // ONLY for core entities 
						//					for (int entId : allEntities) {   // for all entities
						BFSResult bfsTree = taxonomyGraph.bfs(entId);
						String entame = localNames.get(entId);
						double entWeight = entityWeightMap.get(entame);
						for (int catId : taxonomyVerticies) {
							if (!contextEntities.contains(catId)) {
								String category = vlp.getValue(catId);
								calculateCategoryScoreWithWeight(taxonomyGraph, bfsTree, entId, entWeight, catId, category, categoryScoreMap, categoryCoverageMap);
							}
						} // end of for
					} // end of for
					Set<Integer> keySet = categoryScoreMap.keySet();
					double sumOfScores = 0;
					for (int id : keySet) {
						double score1 = categoryScoreMap.get(id);
						double score2 = categoryCoverageMap.get(id);
						double finalScore = ((alpha * score1) / contextEntities.size()) + (1 - alpha) * score2;
						sumOfScores += finalScore;
						categoryScoreMap.put(id, finalScore);
					} // end of for
					List<Integer> sortedCats = sortCategoryIdBasedOnScore(categoryScoreMap);
					List<Integer> sortedCatsSubList = new ArrayList<Integer>();
					Set<Integer> topCategories = new HashSet<Integer>();
					Set<String> topCategoriesNames = new HashSet<String>();
					int k = 1;
					System.out.println("\n========== List of Best Categories from Top to Buttom Start ============\n");
					for (int catId : sortedCats) {
						if (categoryEntityLinkMap.get(catId) >= MaxNumOfLinksPerCategory) {
							continue;
						}else {
							String catName = vlp.getValue(catId);
							topCategoriesNames.add(catName);
							System.out.println("k: " + k + " Category: " + catName + "   Score: " + (categoryScoreMap.get(catId) / sumOfScores));
							topCategories.add(catNamesToInt.get(catName));
							sortedCatsSubList.add(catId);
							k++;
							if (k == 31) {
								break;
							}
						} // end of if
					} // end of for
					System.out.println("\n========== List of Best Categories from Top to Buttom End ============\n");
					
					// output = new FileOutputStream("thematicGraph.png");
					// imgWriter.writeGraph(semanticGraph,
					// GraphvizImageWriter.COMMAND.dot,
					// GraphvizImageWriter.OUTPUT_FORMAT.png, false, output);
					// output.flush();
					// output.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("\n\nType \"quit\" to exit OR Type name of the file to continue: ");
			while (true) {
				fileName = sc.nextLine();
				if (!fileName.equalsIgnoreCase("quit")) {
					document = readDocument("/home/mehdi/" + fileName);
					if (!document.equals("")) {
						break;
					}else {
						continue;
					}
				} // end of if
				break;
			} // end of while
		} while (!fileName.equalsIgnoreCase("quit"));
		sc.close();
	} // end of createGraphFromEntities
	
	
	
	
	
	
	/*********************************************************
	 * @param tfIdfOfTopCategoryWords
	 * @param tfIdfOfUserCategoryWords
	 * @return
	 */
	private double calculateCosineSimilarity(Map<String, Double> tfIdfOfTopCategoryWords, Map<String, Double> tfIdfOfUserCategoryWords) {
		double dotProduct = 0;
		for (Map.Entry<String, Double> entry : tfIdfOfTopCategoryWords.entrySet()) {
			String word = entry.getKey();
			double weight = entry.getValue();
			if (tfIdfOfUserCategoryWords.containsKey(word)) {
				dotProduct += weight * tfIdfOfUserCategoryWords.get(word);
			} // end of if
		} // end of for
		
		double topCategoryLength = findLengthOfVector(new ArrayList<Double> (tfIdfOfTopCategoryWords.values()));
		double userCategoryLength = findLengthOfVector(new ArrayList<Double> (tfIdfOfUserCategoryWords.values()));
		double cosineSimilarity = (dotProduct / (topCategoryLength * userCategoryLength));
		return cosineSimilarity;
	} // end of calculateCosineSimilarity
	
	
	public double findLengthOfVector(List<Double> vector) {
		double norm = 0;
		for (double w : vector) {
			norm += Math.sqrt(w);
		} // end of for
		return norm;
	} // end of findLengthOfVector



	/*********************************************************
	 * @param userCategory
	 * @param totalNumOfCategories
	 * @param indexer
	 * @param searcher 
	 * @return
	 */
	private Map<String, Double> calcualteTfIdfOfUserCategory(Set<Integer> userCategory, int totalNumOfCategories, Indexer indexer, IndexSearcher searcher) {
//		Map<String,Integer> wordsTf = new HashMap<String, Integer>(); 
		Map<String,Double> tfIdfMap = new HashMap<String, Double>(); 
		EntityManagerImpl entityManager = new EntityManagerImpl();
		while (userCategory.size() > 0) {
			System.out.println("user Category size: " + userCategory.size());
			String catIds = ""; 
			List<Integer> part = new ArrayList<Integer>(500);
			int counter = 0;
			for (int catId : userCategory) {
				counter++;
				if (counter < 500) {
					catIds += catId + ",";
					part.add(catId);
				} else {
					break;
				} // end of if
			} // end of for
			userCategory.removeAll(part);
			System.out.println("new user Category size: " + userCategory.size());
			catIds = catIds.substring(0, catIds.length() - 1);
			String query = "SELECT word,sum(frequency) freq from invertedindex where category_id in (" + catIds + ") group by word";
			System.out.println("query: " + query);
			java.sql.ResultSet rs = entityManager.ifind(query);
			try {
				while(rs.next()) {
					String word = rs.getString("word");
					int tf = rs.getInt("freq");
//					wordsTf.put(word, tf);
//					int numOfCategoriesHavingWord = CategoryProcessor.findCategoriesByKeyword(indexer, searcher, word).size();
					int numOfCategoriesHavingWord = findCategoriesByKeywordFromDatabase(word);
					System.out.println("count: " + numOfCategoriesHavingWord);
					double idf = Math.log(Double.valueOf(totalNumOfCategories) / numOfCategoriesHavingWord); 
					double tfIdf = tfIdfMap.get(word) != null ? tfIdfMap.get(word) : 0;
					tfIdf += tf * idf;
					tfIdfMap.put(word, tfIdf);
				} // end of while
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} // end of while
		entityManager.disconnectDatabase();
		return tfIdfMap;
	} // end of calcualteTfIdfOfUserCategory
	
	
//	public int findWordFrequencyFromDatabase(String word) {
//		
//	} // end of findWordFrequencyFromDatabase



	/*********************************************************
	 * @param word
	 * @return
	 */
	public int findCategoriesByKeywordFromDatabase(String word) {
		String query = "SELECT count(*) count from invertedindex where word = '" + word + "'";
		int count = 0;
		EntityManagerImpl entityManager = new EntityManagerImpl();
		java.sql.ResultSet rs = entityManager.ifind(query);
		try {
			while(rs.next()) {
				count = rs.getInt("count");
			} // end of while
		} catch (SQLException e) {
			e.printStackTrace();
		}
		entityManager.disconnectDatabase();
		return count;
	} // end of findCategoriesByKeywordFromDatabase



	/*********************************************************
	 * @param tfOfWords
	 * @param totalNumOfCategories
	 * @param indexer
	 * @param searcher 
	 * @return
	 */
	public Map<String, Double> calculateTfIdfOfCategoryWords(Map<String, Integer> tfOfWords, int totalNumOfCategories, Indexer indexer, IndexSearcher searcher) {
		Map<String, Double> tfIdf = new HashMap<String,Double>();
		for (Map.Entry<String, Integer> entry : tfOfWords.entrySet()) {
			String word = entry.getKey();
			int wordTf = entry.getValue();
//			int numOfCatsHavingWord = CategoryProcessor.findCategoriesByKeyword(indexer, searcher, word).size();
			int numOfCatsHavingWord = findCategoriesByKeywordFromDatabase(word);
			double wordIdf = Math.log(Double.valueOf(totalNumOfCategories) / numOfCatsHavingWord);
			double wordWeight = wordTf * wordIdf;
			tfIdf.put(word, wordWeight);
		} // end of for
		return tfIdf;
	} // end of calculateTfIdfOfCategoryWords



	/*********************************************************
	 * @param categoriesNames
	 * @param stopWords
	 * @return
	 */
	public Map<String, Integer> findFrequencyOfWords(Set<String> categoriesNames, Set<String> stopWords) {
		Map<String, Integer> tf = new HashMap<String,Integer>();
		for (String cat : categoriesNames) {
			String [] catWords = cat.split("_");
			for (String word : catWords) {
				if (!stopWords.contains(word)) {
					word = word.replace("'", "").replace("(", "").replace(")", "").replace(",", "").replace("-", "").toLowerCase();
					int freq = tf.get(word) != null ? tf.get(word) : 0;
					tf.put(word, freq + 1);
				} // end of if
			} // end of for
		} // end of for
		return tf;
	} // end of findFrequencyOfWords



	/*********************************************************
	 * @param intersectionCatNames
	 * @param sortedCatsSubList
	 * @param categoryScoreMap 
	 * @param vlp 
	 * @return
	 */
	public double calculateProbabilityOfDocumentBelongnessToCategory(Set<String> intersectionCatNames, List<Integer> sortedCatsSubList, Map<Integer, Double> categoryScoreMap, LabelProperty vlp) {
		double p = 0;
		if (intersectionCatNames.isEmpty()) {
			return p;
		}else {
			int catWithMaxScore = sortedCatsSubList.get(0);
			int numOfCats = sortedCatsSubList.size();
			for (int catVertexId : sortedCatsSubList) {
				 if (intersectionCatNames.contains(vlp.getValue(catVertexId))) {
					 double normalizedScore = categoryScoreMap.get(catVertexId) / categoryScoreMap.get(catWithMaxScore);
					 p += normalizedScore / numOfCats;
				 } // end of if
			} // end of for
		} // end of if
		return p;
	} // end of calculateProbabilityOfDocumentBelongnessToCategory



	/*********************************************************
	 * @param intersection
	 * @param catId
	 * @param userCategoryTree 
	 * @param topCategories 
	 * @return
	 */
	public double calculateProbabilityOfCategoryGivenTheDocument(Set<Integer> intersection, int catId, Set<Integer> userCategoryTree, Set<Integer> topCategories) {
		int totalNumOfCategories = 1049819;
		double prioirProbabilityOfClass = Double.valueOf(userCategoryTree.size()) / totalNumOfCategories;
//		double probabilityOfCategory = (-1) * Math.log(prioirProbabilityOfClass) + calculateProbabilityOfDocumentGivenTheClass(intersection, topCategories);
		double probabilityOfCategory = calculateProbabilityOfDocumentGivenTheClass(intersection, topCategories);
		return probabilityOfCategory;
	} // end of calculateProbabilityOfCategoryGivenTheDocument



	/*********************************************************
	 * @param document
	 * @param topCategories 
	 * @return
	 */
	public double calculateProbabilityOfDocumentGivenTheClass(Set<Integer> document, Set<Integer> topCategories) {
		double probabilityOfDocument = 0;
		for (int catId : topCategories) {
			int indicator = 0;
			if (document.isEmpty()) {
				indicator = 0;
			}else {
				indicator = document.contains(catId) ? 1 : 0;
			} // end of if
			double probabilityOfWordInClass = calculateProbabilityOfWordGivenTheClass(catId, document);
			probabilityOfDocument += Math.log(((indicator * probabilityOfWordInClass) + ((1 - indicator) * (1 - probabilityOfWordInClass))));
		} // end of for
		probabilityOfDocument *= -1;
		return probabilityOfDocument;
	} // end of calculateProbabilityOfDocumentGivenTheClass



	/*********************************************************
	 * @param catId
	 * @param document
	 * @return
	 */
	public double calculateProbabilityOfWordGivenTheClass(int catId, Set<Integer> document) {
		int indicator = document.contains(catId) ? 1 : 0;
		double probabilityOfWordInClass = Double.valueOf(1 + indicator) / 3;
		return probabilityOfWordInClass;
	} // end of calculateProbabilityOfWordGivenTheClass



	/*********************************************************
	 * @param graph
	 * @param coreEntities
	 * @return
	 */
	public Set<Integer> findCoreContextEntities(SemanticGraph graph, Set<Integer> coreEntities) {
		Set<Integer> contextEntities = new HashSet<Integer>();
		for (int entId : coreEntities) {
			int [] vset = graph.getInNeighbors(entId).toIntArray();
			if (vset.length != 0) {
				for (int v : vset) {
					contextEntities.add(v);
				} // end of for
			} // end of if
			vset = graph.getOutNeighbours(entId).toIntArray();
			if (vset.length != 0) {
				for (int v : vset) {
					contextEntities.add(v);
				} // end of for
			} // end of if
		} // end of for
		contextEntities.addAll(coreEntities);
		return contextEntities;
	} // end of findCoreContextEntities


	/*********************************************************
	 * @param thematicSemanticGraph
	 * @param coreEntities
	 * @return
	 */
	public List<String> findContextCategories(SemanticGraph thematicSemanticGraph, Set<Integer> coreEntities) {
		Set<String> cats = new HashSet<String>();
		for (int entId : coreEntities) {
			int [] vset = thematicSemanticGraph.getInNeighbors(entId).toIntArray();
			if (vset.length != 0) {
				for (int v : vset) {
					if (entityCategoryMap.get(v) != null) {
						cats.addAll(entityCategoryMap.get(v));
					} // end of if
				} // end of for
			} // end of if
			vset = thematicSemanticGraph.getOutNeighbours(entId).toIntArray();
			if (vset.length != 0) {
				for (int v : vset) {
					if (entityCategoryMap.get(v) != null) {
						cats.addAll(entityCategoryMap.get(v));
					} // end of if
				} // end of for
			} // end of if
		} // end of for
		List<String> catList = new ArrayList<String>(cats);
		return catList;
	} // end of findContextCategories

	/*********************************************************
	 * @param vlp 
	 * @param entities 
	 * @param stopWords 
	 * @param topKCategoryThreshold 
	 * @param contextCategories 
	 * @return
	 */
	public List<String> findTopKCategories(Set<Integer> entities, Set<String> stopWords, int topKCategoryThreshold, List<String> contextCategories) {
		List<String> finalCats = new ArrayList<String>();
		Set<String> initialCats = new HashSet<String>();
		Set<String> vocabulary = new HashSet<String>();
//		for (Map.Entry<Integer, Set<String>> entry : entityCategoryMap.entrySet()) {
		for (int entId : entities) {
			Set<String> catList = entityCategoryMap.get(entId);
			if (catList != null) {
				for (String cat : catList) {
					initialCats.add(cat);
					String [] catWords = cat.split("_");
					for (String word : catWords) {
						if (!stopWords.contains(word)) {
							vocabulary.add(word);
						} // end of if
					} // end of for (String word : catWords) 
				} // end of for (String cat : catList)
			} // end of if
		} // end of for
		
		for (String cat : contextCategories) {
			initialCats.add(cat);
			String [] catWords = cat.split("_");
			for (String word : catWords) {
				if (!stopWords.contains(word)) {
					vocabulary.add(word);
				} // end of if
			} // end of for (String word : catWords) 
		} // end of for (String cat : catList)
		
		
		Map<String,Double> catWeightMap = new HashMap<String,Double>();
		for (String cat : initialCats) {
			String [] catWords = cat.split("_");
			int frequency = 0;
			double weight = 0.0;
			for (String word : catWords) {
				if (!stopWords.contains(word)) {
					for (String c : initialCats) {
						if (c.contains(word)) {
							frequency++;
						} // end of if
					} // end of for
					weight += Double.valueOf(frequency) / vocabulary.size();
				} // end of if
			} // end of for (String word : catWords)
			weight /= catWords.length;
			catWeightMap.put(cat, weight);
		} // end of for (String cat : initialCats)
		List<String> sortedCats = CategorySortBasedOnScore(catWeightMap);
		System.out.println("Initial Category List: ");
		for (Map.Entry<String,Double> entry : catWeightMap.entrySet()) {
			System.out.println("Category: " + entry.getKey() + "    Weight: " + entry.getValue());
		}
		System.out.println("top K Category List: ");
		topKCategoryThreshold = Math.min(topKCategoryThreshold, sortedCats.size());
		for (int i = 0;i < topKCategoryThreshold;i++) {
			finalCats.add(sortedCats.get(i));
			System.out.println(finalCats.get(i));
		} // end of for 
		return finalCats;
	} // end of findTopKCategories

//	public double calculateCategoryScoreByPageRank(SemanticGraph taxonomyGraph, int catId, double alpha, Map<Integer, Double> categoryScoreMap) {
//		double score = 0;
//		int [] incomingVertices = taxonomyGraph.getInNeighbors(catId).toIntArray();
//		if (incomingVertices.length == 0) {
//			score += prevScoreMap.get(vid);
//		}else {
//			for (int v : incomingVertices) {
//				int outgoingVertices = taxonomyGraph.getOutNeighbours(v).size();
//				score +=  prevScoreMap.get(v) / outgoingVertices;
//			} // end of for
//		} // end of if
//		score *= alpha;
//		score += 1 - alpha;
//		return score;
//	} // end of calculateCategoryScoreByPageRank
	
	public void calculateCategoryScoreWithWeight(SemanticGraph taxonomyGraph, BFSResult bfsTree, int entId, double entWeight, int catId, String category, Map<Integer,Double> categoryScoreMap, Map<Integer, Double> categoryCoverageMap) {
		VPath path = bfsTree.computePathTo(catId);
		int pathLength = path.getLength();
		if (pathLength == 0) {
			double score = categoryScoreMap.get(catId) != null ? categoryScoreMap.get(catId) : 0;
			double coverage = categoryCoverageMap.get(catId) != null ? categoryCoverageMap.get(catId) : 0;
			categoryScoreMap.put(catId, score);
			categoryCoverageMap.put(catId, coverage);
		}else if (pathLength > 0) {
			double coverage = categoryCoverageMap.get(catId) != null ? categoryCoverageMap.get(catId) : 0;
			coverage +=  entWeight * (1.0 / pathLength);
			categoryCoverageMap.put(catId, coverage);
			double score = 1;
			int [] pathVertices = path.getVerticesAsArray();
			// The first and last vertices are the source and destination
			for (int i = 0;i < pathVertices.length - 1; i++) { 
				int vid = pathVertices [ i ];
				int numOfLinks = taxonomyGraph.getOutNeighbours(vid).size();
				score *= 1.0 / numOfLinks;
			} // end of for
			score *= entWeight;
			double prevScore = categoryScoreMap.get(catId) != null ? categoryScoreMap.get(catId) : 0;
			score += prevScore;
			categoryScoreMap.put(catId, score);
		} // end of if
	} // end of calculateCategoryScoreWithWeight
	
	/*********************************************************
	 * @param taxonomyGraph
	 * @param bfsTree
	 * @param vid
	 * @param catId
	 * @param categoryCoverageMap 
	 */
	public void calculateCategoryScore(SemanticGraph taxonomyGraph, BFSResult bfsTree, int entId, int catId, String category, Map<Integer,Double> categoryScoreMap, Map<Integer, Integer> categoryCoverageMap) {
		VPath path = bfsTree.computePathTo(catId);
		int pathLength = path.getLength();
		if (pathLength == 0) {
			double score = categoryScoreMap.get(catId) != null ? categoryScoreMap.get(catId) : 0;
			int coverage = categoryCoverageMap.get(catId) != null ? categoryCoverageMap.get(catId) : 0;
			categoryScoreMap.put(catId, score);
			categoryCoverageMap.put(catId, coverage);
		}else if (pathLength > 0) {
			int coverage = categoryCoverageMap.get(catId) != null ? categoryCoverageMap.get(catId) : 0;
			coverage++;
			categoryCoverageMap.put(catId, coverage);
			double score = 1;
			int [] pathVertices = path.getVerticesAsArray();
			// The first and last vertices are the source and destination
			for (int i = 0;i < pathVertices.length - 1; i++) { 
				int vid = pathVertices [ i ];
				int numOfLinks = taxonomyGraph.getOutNeighbours(vid).size();
				score *= 1.0 / numOfLinks;
			} // end of for
			double prevScore = categoryScoreMap.get(catId) != null ? categoryScoreMap.get(catId) : 0;
			score += prevScore;
			categoryScoreMap.put(catId, score);
		} // end of if
	} // end of calculateCategoryScore

	public Map<Integer,Double> calculateCategoryScore(String category, Set<Integer> coreEntities, VirtGraph virtGraph, double alpha, BFSResult bfsTree) {
		Map<Integer,Double> semanticRelatednessMap = getSemanticRelatednessBetweenCategoryAndEntity(category, coreEntities, virtGraph);
		Map<Integer,Double> scoreMap = new HashMap<Integer,Double>();
		int pathLength = 1;
		for (int entId : coreEntities) {
			pathLength = bfsTree.computePathTo(entId).getLength();
			if (pathLength == 0) {
				pathLength = bfsTree.maxDistance;
			} // end of if
			double semanticRelatedness = semanticRelatednessMap.get(entId) != null ? semanticRelatednessMap.get(entId) : 0;
			double score = alpha * semanticRelatedness + ((1 - alpha) * (1 / (Math.log(pathLength) + 1)));
			scoreMap.put(entId, score);
		} // end of for
		return scoreMap;
	} // end of calculateCategoryScore
	
	
	public Map<Integer,Double> getSemanticRelatednessBetweenCategoryAndEntity(String category, Set<Integer> coreEntities, Map<Integer,Set<Integer>> coreEntityLinks, VirtGraph virtGraph) {
		int numOfEntities = findNumberOfCategoryEntities(category, virtGraph);
		Map<Integer,Double> semanticRelatednessMap = new HashMap<Integer,Double>();
		if (numOfEntities < 1000) {
			Set<Integer> catEntities = findAllCategoryEntities(category, virtGraph);
//			System.out.println("num of cat entities: " + catEntities.size());
			Set<Set<Integer>> catEntityLinks = new HashSet<Set<Integer>>(catEntities.size());
//			Map<Integer,Set<Integer>> coreEntityLinks = new HashMap<Integer,Set<Integer>>(coreEntities.size());
			double semanticRelatedness = 0;
			if (!catEntities.isEmpty()) {
				for (int entId : catEntities) {
					String entName = localNames.get(entId);
					catEntityLinks.add(findLinkedEntities(entName, virtGraph));
				} // end of for
//				for (int entId : coreEntities) {
//					String entName = localNames.get(entId);
//					coreEntityLinks.put(entId, findLinkedEntities(entName, virtGraph));
//				} // end of for
				for (int entId : coreEntities) {
					Set<Integer> linkingEntities1 = coreEntityLinks.get(entId);
					//				System.out.println("num of core entity links: " + linkingEntities1.size());
					for (Set<Integer> linkingEntities2 : catEntityLinks) {
						//					System.out.println("num of cat entity links: " + linkingEntities2.size());
						semanticRelatedness += calculateSemanticRelatedness(linkingEntities1, linkingEntities2);
					} // end of for
//					semanticRelatedness /= (catEntities.size() + coreEntities.size());
					semanticRelatedness /= catEntities.size();
					semanticRelatednessMap.put(entId, semanticRelatedness);
					semanticRelatedness = 0;
				} // end of for
			} // end of if
		} // end of if
		return semanticRelatednessMap;
	} // end of getSemanticRelatednessBetweenCategoryAndEntity
	
	
	
	public Map<Integer,Double> getSemanticRelatednessBetweenCategoryAndEntity(String category, Set<Integer> coreEntities, VirtGraph virtGraph) {
		int numOfEntities = findNumberOfCategoryEntities(category, virtGraph);
		Map<Integer,Double> semanticRelatednessMap = new HashMap<Integer,Double>();
		if (numOfEntities < 1000) {
			Set<Integer> catEntities = findAllCategoryEntities(category, virtGraph);
			System.out.println("num of cat entities: " + catEntities.size());
			Set<Set<Integer>> catEntityLinks = new HashSet<Set<Integer>>(catEntities.size());
			Map<Integer,Set<Integer>> coreEntityLinks = new HashMap<Integer,Set<Integer>>(coreEntities.size());
			double semanticRelatedness = 0;
			if (!catEntities.isEmpty()) {
				for (int entId : catEntities) {
					String entName = localNames.get(entId);
					catEntityLinks.add(findLinkedEntities(entName, virtGraph));
				} // end of for
				for (int entId : coreEntities) {
					String entName = localNames.get(entId);
					coreEntityLinks.put(entId, findLinkedEntities(entName, virtGraph));
				} // end of for
				for (int entId : coreEntities) {
					Set<Integer> linkingEntities1 = coreEntityLinks.get(entId);
					//				System.out.println("num of core entity links: " + linkingEntities1.size());
					for (Set<Integer> linkingEntities2 : catEntityLinks) {
						//					System.out.println("num of cat entity links: " + linkingEntities2.size());
						semanticRelatedness += calculateSemanticRelatedness(linkingEntities1, linkingEntities2);
					} // end of for
//					semanticRelatedness /= (catEntities.size() + coreEntities.size());
					semanticRelatedness /= catEntities.size();
					semanticRelatednessMap.put(entId, semanticRelatedness);
					semanticRelatedness = 0;
				} // end of for
			} // end of if
		} // end of if
		return semanticRelatednessMap;
	} // end of getSemanticRelatednessBetweenCategoryAndEntity
	
	
	/*********************************************************
	 * @param categoryName
	 * @param virtGraph 
	 * @return
	 */
	public Set<Integer> findAllCategoryEntities(String categoryName, VirtGraph virtGraph) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
		queryString.append(" ?s a <" + categoryUriPrefix + categoryName + "> . ");
		queryString.append("}LIMIT 11000");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
//		System.out.println("QUERY: " + queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		// System.out.println("Retrieved the Results.");
		// System.out.println("Iterating over the Results...");
		Set<Integer> relations = new HashSet<Integer>();
		int uriPrefixLength = uriPrefix.length();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String localname = result.getResource("s").toString().substring(uriPrefixLength);
			relations.add(namesToInt.get(localname));
		} // end of while
		vqe.close();
		return relations;
	} // end of findAllCategoryEntities
	
	
	public int findNumberOfCategoryEntities(String categoryName, VirtGraph virtGraph) {
		int counter = 0;
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
			queryString.append("SELECT (COUNT(DISTINCT ?s)) AS ?total FROM <" + GRAPH + "> WHERE { ");
			queryString.append(" ?s a <" + categoryUriPrefix + categoryName + "> . ");
			queryString.append("}");
			QueryEngineHTTP qe = new QueryEngineHTTP(endpoint, queryString.toString());
			ResultSet results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				counter = result.getLiteral("total").getInt();
			} // end of while

			/*
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT ?s FROM <" + GRAPH + "> WHERE { ");
		queryString.append(" ?s a <" + categoryUriPrefix + categoryName + "> . ");
		queryString.append("}");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
//		ResultSetMem rm = (ResultSetMem) results;
		int counter = 0;
		while (results.hasNext()) {
//			QuerySolution result = results.nextSolution();
			counter = results.getRowNumber();
			results.nextSolution();
//			counter = Integer.valueOf(result.getResource("cc").toString());
		} // end of while
			 */
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return counter;
	} // end of findNumberOfCategoryEntities
	
	
	public double calculateSemanticRelatedness(Set<Integer> linkingEntities1, Set<Integer> linkingEntities2) {
		int max = 1;
		int min = 1;
		if (linkingEntities1.size() > linkingEntities2.size()) {
			max = linkingEntities1.size();
			min = linkingEntities2.size();
		}else {
			max = linkingEntities2.size();
			min = linkingEntities1.size();
		} // end of if
//		int intersection = getIntersectionSize(linkingEntities1, linkingEntities2);
		Set<Integer> linkingEntities1Clone = new HashSet<Integer>(linkingEntities1);
		linkingEntities1Clone.retainAll(linkingEntities2);
		int intersection = linkingEntities1Clone.size();
		double semanticRelatedness = 0;
		if (intersection == 0) {
//			semanticRelatedness = 0;
			semanticRelatedness = 1 - (Math.log(max) / (Math.log(wikipediaSize) - Math.log(min)));
		}else {
			semanticRelatedness = 1 - ((Math.log(max) - Math.log(intersection)) / (Math.log(wikipediaSize) - Math.log(min)));
		}
		if (Double.isNaN(semanticRelatedness)) {
			semanticRelatedness = 0;
		}
		return semanticRelatedness;
	} // end of calculateSemanticRelatedness
	

	public double calculateSemanticRelatedness(String entName1, String entName2, VirtGraph virtGraph) {
		Set<Integer> linkingEntities1 = findLinkedEntities(entName1, virtGraph);
		Set<Integer> linkingEntities2 = findLinkedEntities(entName2, virtGraph);
		int max = 1;
		int min = 1;
		if (linkingEntities1.size() > linkingEntities2.size()) {
			max = linkingEntities1.size();
			min = linkingEntities2.size();
		}else {
			max = linkingEntities2.size();
			min = linkingEntities1.size();
		} // end of if
//		int intersection = getIntersectionSize(linkingEntities1, linkingEntities2);
		Set<Integer> linkingEntities1Clone = new HashSet<Integer>(linkingEntities1);
		linkingEntities1Clone.retainAll(linkingEntities2);
		int intersection = linkingEntities1Clone.size();
		double semanticRelatedness = 0;
		if (intersection == 0) {
			semanticRelatedness = 1 - (Math.log(max) / (Math.log(wikipediaSize) - Math.log(min)));
		}else {
			semanticRelatedness = 1 - ((Math.log(max) - Math.log(intersection)) / (Math.log(wikipediaSize) - Math.log(min)));
		}
		return semanticRelatedness;
	} // end of calculateSemanticRelatedness
	

	/*********************************************************
	 * @param entId1
	 * @param entId2
	 */
	public double calculateSemanticRelatedness(int entId1, int entId2, VirtGraph virtGraph) {
		String entName1 = localNames.get(entId1);
		String entName2 = localNames.get(entId2);
		Set<Integer> linkingEntities1 = findLinkedEntities(entName1, virtGraph);
		Set<Integer> linkingEntities2 = findLinkedEntities(entName2, virtGraph);
		int max = 1;
		int min = 1;
		if (linkingEntities1.size() > linkingEntities2.size()) {
			max = linkingEntities1.size();
			min = linkingEntities2.size();
		}else {
			max = linkingEntities2.size();
			min = linkingEntities1.size();
		} // end of if
//		int intersection = getIntersectionSize(linkingEntities1, linkingEntities2);
		Set<Integer> linkingEntities1Clone = new HashSet<Integer>(linkingEntities1);
		linkingEntities1Clone.retainAll(linkingEntities2);
		int intersection = linkingEntities1Clone.size();
		double semanticRelatedness = 0;
		if (intersection == 0) {
			semanticRelatedness = 1 - (Math.log(max) / (Math.log(wikipediaSize) - Math.log(min)));
		}else {
			semanticRelatedness = 1 - ((Math.log(max) - Math.log(intersection)) / (Math.log(wikipediaSize) - Math.log(min)));
		}
		return semanticRelatedness;
	} // end of calculateSemanticRelatedness

	/*********************************************************
	 * @param set1
	 * @param set2
	 * @return
	 */
	public int getIntersectionSize(Set<Integer> set1, Set<Integer> set2) {
		int intersection = 0;
		if (set1.size() < set2.size()) {
			for (int ent : set1) {
				if (set2.contains(ent)) {
					intersection++;
				} // end of if
			} //  end of for
		}else {
			for (int ent : set2) {
				if (set1.contains(ent)) {
					intersection++;
				} // end of if
			} //  end of for
		}
		return intersection;
	} // end of 

	/*********************************************************
	 * @param entName
	 * @return
	 */
	public Set<Integer> findLinkedEntities(String entName, VirtGraph virtGraph) {
		StringBuffer queryString = new StringBuffer();
		Set<Integer> relations = new HashSet<Integer>();
		if (entName.contains("\"") || entName.contains("\'")) {
			return relations;
		}
		try {
			queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
			queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
			queryString.append(" ?s lsdis:wiki_href <" + uriPrefix + entName + "> . ");
			queryString.append("}");
			//		System.out.println("QUERY: " + queryString.toString());
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			int uriPrefixLength = uriPrefix.length();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String localname = result.getResource("s").toString().substring(uriPrefixLength);
				relations.add(namesToInt.get(localname));
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("QUERY: " + queryString.toString());
		}
		return relations;
	} // end of findLinkedEntities

	/*********************************************************
	 * @param entities
	 * @param entityCategoryMap
	 * @param maximumHierarchyLevel
	 * @return
	 */
	private SemanticGraph constructWikiTaxonomicGraph(List<Integer> entities, Map<Integer, Set<String>> entityCategoryMap, int maximumHierarchyLevel) {
		SemanticGraph graph = new SemanticGraph();
		graph.getClass().getClassLoader().setDefaultAssertionStatus(false);
		LabelProperty vlp = graph.getVertexLabelProperty();
		Map<String, Integer> catVertexMap = new HashMap<String, Integer>();
		for (int vertex : entities) {
			graph.addVertex(vertex);
			System.out.println("entity: " + localNames.get(vertex));
			vlp.setValue(vertex, localNames.get(vertex));
			Set<String> categories = entityCategoryMap.get(vertex);
			if (categories != null) {
				for (String cat : categories) {
					int vid2 = 0;
					int hierarchyLevel = 0;
					cat = addVertexAndEdgeToTaxonomyGraph(graph, cat, catVertexMap, hierarchyLevel, maximumHierarchyLevel);
					if (catVertexMap.get(cat) != null) {
						vid2 = catVertexMap.get(cat);
					} else {
						vid2 = graph.addVertex();
						catVertexMap.put(cat, vid2);
						vlp.setValue(vid2, cat);
					} // end of if
					graph.addDirectedSimpleEdge(vertex, vid2);
				} // end of for (String cat : categories)
			} // end of if (categories != null)
		} // end of for (int vertex : coreEntities)
		return graph;
	} // end of constructWikiTaxonomicGraph

	/*********************************************************
	 * @param graph
	 * @param cat
	 * @param catVertexMap
	 * @param hierarchyLevel
	 * @param maximumHierarchyLevel
	 */
	private String addVertexAndEdgeToTaxonomyGraph(SemanticGraph graph, String cat, Map<String, Integer> catVertexMap, int hierarchyLevel, int maximumHierarchyLevel) {
		if (hierarchyLevel >= maximumHierarchyLevel) {
			return cat;
		}
		Set<String> superCategorySet = wikiTaxonomyMapFromChildToParent.get(cat);
		LabelProperty vlp = graph.getVertexLabelProperty();
		if (superCategorySet != null) {
			for (String superCategory : superCategorySet) {
				int vid1 = 0;
				// to avoid infinite loop, because of loop in wikipedia category
				// taxonomy
				if (catVertexMap.get(superCategory) != null) {
					continue;
				} else {
					vid1 = graph.addVertex();
					catVertexMap.put(superCategory, vid1);
					vlp.setValue(vid1, superCategory);
				}
				hierarchyLevel++;
				String parent = addVertexAndEdgeToTaxonomyGraph(graph, superCategory, catVertexMap, hierarchyLevel, maximumHierarchyLevel);
				int vid2 = 0;
				if (catVertexMap.get(cat) != null) {
					vid1 = catVertexMap.get(cat);
				} else {
					vid1 = graph.addVertex();
					catVertexMap.put(cat, vid1);
					vlp.setValue(vid1, cat);
				}
				if (catVertexMap.get(parent) != null) {
					vid2 = catVertexMap.get(parent);
				} else {
					vid2 = graph.addVertex();
					catVertexMap.put(parent, vid2);
					vlp.setValue(vid2, parent);
				}
				// for child -> parent relations
				if (graph.getSomeEdgeConnecting(vid1, vid2) == -1) {
					graph.addDirectedSimpleEdge(vid1, vid2);
				}

				// for parent -> child relations
				// if (graph.getSomeEdgeConnecting(vid2, vid1) == -1) {
				// graph.addDirectedSimpleEdge(vid2, vid1);
				// }
			} // end of for
		} // end of if
		return cat;
	} // end of addVertexAndEdgeToTaxonomyGraph

	/*********************************************************
	 * @param entities
	 * @param entityCategoryMap
	 * @return
	 */
	private SemanticGraph constructYagoTaxonomicGraph(List<Integer> entities, Map<Integer, Set<String>> entityCategoryMap) {
		SemanticGraph graph = new SemanticGraph();
		graph.getClass().getClassLoader().setDefaultAssertionStatus(false);
		LabelProperty vlp = graph.getVertexLabelProperty();
		Map<String, Integer> catVertexMap = new HashMap<String, Integer>();
		for (int vertex : entities) {
			Set<String> categories = entityCategoryMap.get(vertex);
			if (categories != null) {
				for (String cat : categories) {
					int vid1 = 0;
					String yagoCatname = "wikicategory_" + cat;
					if (yagoTaxonomyMap.get(yagoCatname) != null) {
						String superCategory = yagoTaxonomyMap.get(yagoCatname);
						if (catVertexMap.get(superCategory) != null) {
							vid1 = catVertexMap.get(superCategory);
							superCategory = yagoTaxonomyMap.get(superCategory);
						} else {
							vid1 = graph.addVertex();
							catVertexMap.put(superCategory, vid1);
							vlp.setValue(vid1, superCategory);
							superCategory = yagoTaxonomyMap.get(superCategory);
						} // end of if
						while (superCategory != null) {
							int vid2 = 0;
							if (catVertexMap.get(superCategory) != null) {
								vid2 = catVertexMap.get(superCategory);
							} else {
								vid2 = graph.addVertex();
								catVertexMap.put(superCategory, vid2);
								vlp.setValue(vid2, superCategory);
							}
							// for parent -> child relations
							// if (graph.getSomeEdgeConnecting(vid2, vid1) ==
							// -1) {
							// graph.addDirectedSimpleEdge(vid2, vid1);
							// }

							// for child -> parent relations
							if (graph.getSomeEdgeConnecting(vid1, vid2) == -1) {
								graph.addDirectedSimpleEdge(vid1, vid2);
							}
							vid1 = vid2;
							superCategory = yagoTaxonomyMap.get(superCategory);
						} // end of while
					} else {
						System.out.println("Cat: " + cat);
					} // end of if
				} // end of for (String cat : categories)
			}
		} // end of for (int vertex : coreEntities)
		return graph;
	} // end of constructYagoTaxonomicGraph

	/*********************************************************
	 * @param thematicSemanticGraph
	 * @param nonAuthEntities
	 */
	public void removeLowWeightVerticies(SemanticGraph thematicSemanticGraph, Set<Integer> nonAuthEntities) {
		for (int vertex : nonAuthEntities) {
			IntSet incomingEdges = thematicSemanticGraph.getInEdges(vertex);
			thematicSemanticGraph.removeEdges(incomingEdges);
			IntSet outgoingEdges = thematicSemanticGraph.getOutEdges(vertex);
			thematicSemanticGraph.removeEdges(outgoingEdges);
			thematicSemanticGraph.removeVertex(vertex);
		} // end of for
	} // end of removeLowWeightVerticies

	/*********************************************************
	 * @param entities
	 * @return
	 */
	public List<Integer> sortBasedOnScore(Map<Integer, Double> entities) {
		List<Integer> keys = new ArrayList<Integer>(entities.keySet());
		List<Double> scores = new ArrayList<Double>(entities.values());
		List<Integer> sortedKeys = new ArrayList<Integer>(entities.size());
		Collections.sort(scores, Collections.reverseOrder());
		// initialize the list, to be able to set values in different positions
		// later
		for (int i = 0; i < entities.size(); i++) {
			sortedKeys.add(i);
		}
		for (int vid : keys) {
			int index = scores.indexOf(entities.get(vid));
			scores.set(index, -1.0); // we change the value of score.get(index)
									// to some arbitrary in order to handle
									// multiple keys with the same value if
									// exist
			sortedKeys.set(index, vid);
		} // end of for
		return sortedKeys;
	} // end of sortBasedOnScore
	
	
	public List<Integer> sortCategoryIdBasedOnScore(Map<Integer, Double> items) {
		List<Integer> keys = new ArrayList<Integer>(items.keySet());
		List<Double> scores = new ArrayList<Double>(items.values());
		List<Integer> sortedKeys = new ArrayList<Integer>(items.size());
		Collections.sort(scores, Collections.reverseOrder());
		// initialize the list, to be able to set values in different positions later
		for (int i = 0; i < items.size(); i++) {
			sortedKeys.add(i);
		}
		for (int id : keys) {
			int index = scores.indexOf(items.get(id));
			scores.set(index, -1.0); // we change the value of score.get(index)
									// to some arbitrary in order to handle
									// multiple keys with the same value if
									// exist
			sortedKeys.set(index, id);
		} // end of for
		return sortedKeys;
	} // end of sortBasedOnScore
	
	
	public List<String> CategorySortBasedOnScore(Map<String, Double> items) {
		List<String> keys = new ArrayList<String>(items.keySet());
		List<Double> scores = new ArrayList<Double>(items.values());
		List<String> sortedKeys = new ArrayList<String>(items.size());
		Collections.sort(scores, Collections.reverseOrder());
		// initialize the list, to be able to set values in different positions later
		for (int i = 0; i < items.size(); i++) {
			sortedKeys.add("" + i);
		}
		for (String id : keys) {
			int index = scores.indexOf(items.get(id));
			scores.set(index, -1.0); // we change the value of score.get(index)
									// to some arbitrary in order to handle
									// multiple keys with the same value if
									// exist
			sortedKeys.set(index, id);
		} // end of for
		return sortedKeys;
	} // end of sortBasedOnScore

	/*********************************************************
	 * @param authoritativeEntities
	 * @param weightMap
	 * @return
	 */
	public List<Integer> sortBasedOnWeight(Set<Integer> entities, Map<Integer, Double> weightMap) {
		List<Integer> sortedList = new ArrayList<Integer>(entities.size());
		List<Double> weightList = new ArrayList<Double>(entities.size());
		for (int vid : entities) {
			weightList.add(weightMap.get(vid));
			sortedList.add(vid);
		} // end of for
		Collections.sort(weightList, Collections.reverseOrder());
		for (int vid : entities) {
			int index = weightList.indexOf(weightMap.get(vid));
			weightList.set(index, -1111.0);
			sortedList.set(index, vid);
		} // end of for
		return sortedList;
	} // end of sortBasedOnWeight

	/*********************************************************
	 * @param sortedCoreEntities
	 * @param mapObject
	 */
	public void show(List<Integer> sortedCoreEntities, Map<Integer, Set<String>> mapObject) {
		for (int vid : sortedCoreEntities) {
			String ename = localNames.get(vid);
			System.out.println("vid: " + vid + "  " + ename + "  " + mapObject.get(vid) + "\n");
		}
		// for (Map.Entry<Integer, Set<String>> entry : mapObject.entrySet()) {
		// int key = entry.getKey();
		// Set<String> val = entry.getValue();
		// System.out.println("Key: " + key + "  Value: " + val.toString());
		// } // end of for
	} // end of show

	/*********************************************************
	 * @param undirectedGraph
	 * @param nonAuthEntities
	 * @param centralityThreshold
	 * @return
	 */
	public Map<Integer, Double> getCentralEntities(SemanticGraph undirectedGraph, Set<Integer> authEntities, double centralityThreshold) {
		undirectedGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
		// Set<Integer> centralEntities = new HashSet<Integer>(100);
		Map<Integer, Double> centralEntities = new HashMap<Integer, Double>(100);
		List<Integer> vertices = new ArrayList<Integer>(authEntities);
		DijkstraAlgorithm shortestPathAlgorithm = new DijkstraAlgorithm();
		for (int i = 0; i < vertices.size(); i++) {
			double centrality = 0;
			double sumOfShortestPaths = 0;
			SearchResult searchResult = shortestPathAlgorithm.compute(undirectedGraph, vertices.get(i));
			for (int j = 0; j < vertices.size(); j++) {
				sumOfShortestPaths += searchResult.computePathTo(vertices.get(j)).getLength();
				// sumOfShortestPaths +=
				// undirectedGraph.getShortestPath(vertices.get(i),
				// vertices.get(j)).getLength();
			} // end of for (j)
			if (sumOfShortestPaths != 0) {
				centrality = 1 / sumOfShortestPaths;
				if (centrality >= centralityThreshold) {
					centralEntities.put(vertices.get(i), centrality);
				} // end of if
			} // end of if
		} // end of for (i)
		return centralEntities;
	} // end of getCentralEntities

	/*********************************************************
	 * @param thematicSemanticGraph
	 * @param vertices
	 * @param numOfSteps
	 * @param relationImportanceWeightObj
	 * @param virtGraph
	 * @return
	 */
	public void runHITSalgorithm(SemanticGraph graph, int[] vertices, int numOfSteps, JsonObject relationImportanceWeightObj, VirtGraph virtGraph) {
		graph.getClass().getClassLoader().setDefaultAssertionStatus(false);
		double infoboxWeight = relationImportanceWeightObj.get("infobox").getAsDouble();
		double templateWeight = relationImportanceWeightObj.get("template").getAsDouble();
		double wiki_hrefWeight = relationImportanceWeightObj.get("wiki_href").getAsDouble();
		double defaultWeight = relationImportanceWeightObj.get("default").getAsDouble();

		for (int step = 0; step < numOfSteps; step++) {
			double norm = calculateAuthorities(vertices, graph, infoboxWeight, templateWeight, wiki_hrefWeight, defaultWeight, virtGraph);
			// for (int vertex : vertices) {
			// double authorityScore = 0;
			// String en2 = localNames.get(vertex);
			// List<Integer> incomingNeighbors =
			// thematicSemanticGraph.getInNeighbors(vertex).toIntegerArrayList();
			// for (int inVertex : incomingNeighbors) {
			// double inVerHubScore =
			// thematicSemanticGraph.getVertexWeight(inVertex);
			// String en1 = localNames.get(inVertex);
			// List<String> relations =
			// getRelationsBetweenTwoEntities(virtGraph, en1, en2 );
			// for (String p : relations) {
			// double edgeWeight = 0;
			// if (p.contains("infobox")) {
			// edgeWeight = 1;
			// }else if (p.contains("template")) {
			// edgeWeight = 0.8;
			// }else if (p.contains("wiki_href")) {
			// edgeWeight = 0.6;
			// }else {
			// edgeWeight = 0.3;
			// }
			// authorityScore += edgeWeight * inVerHubScore;
			// } // end of for (String p : relations)
			// } // end of for (int inVertex : incomingNeighbors)
			// thematicSemanticGraph.setVertexWeight(vertex, authorityScore);
			// norm += Math.pow(authorityScore, 2);
			// } // end of for (int vertex : vertices)
			norm = Math.sqrt(norm);
			if (norm == 0) {
				norm = 1;
			} // end of if
			for (int vertex : vertices) {
				double authorityScore = graph.getVertexWeight(vertex);
				authorityScore /= norm; // normalise the authorities values
				graph.setVertexWeight(vertex, authorityScore);
			} // end of for (int vertex : vertices)
			norm = calculateHubs(vertices, graph, infoboxWeight, templateWeight, wiki_hrefWeight, defaultWeight, virtGraph);
			// for (int vertex : vertices) {
			// double hubScore = 0;
			// String en1 = localNames.get(vertex);
			// List<Integer> outgoingNeighbors =
			// thematicSemanticGraph.getOutNeighbours(vertex).toIntegerArrayList();
			// for (int outVertex : outgoingNeighbors) {
			// double outVerAuthorityScore =
			// thematicSemanticGraph.getVertexWeight(outVertex);
			// String en2 = localNames.get(outVertex);
			// List<String> relations =
			// getRelationsBetweenTwoEntities(virtGraph, en1, en2 );
			// for (String p : relations) {
			// double edgeWeight = 0;
			// if (p.contains("infobox")) {
			// edgeWeight = 1;
			// }else if (p.contains("template")) {
			// edgeWeight = 0.8;
			// }else if (p.contains("wiki_href")) {
			// edgeWeight = 0.6;
			// }else {
			// edgeWeight = 0.3;
			// }
			// hubScore += edgeWeight * outVerAuthorityScore;
			// } // end of for (String p : relations)
			// } // end of for (int outVertex : outgoingNeighbors)
			// thematicSemanticGraph.setVertexWeight(vertex, hubScore);
			// norm += Math.pow(hubScore, 2);
			// } // end of for (int vertex : vertices)
			norm = Math.sqrt(norm);
			if (norm == 0) {
				norm = 1;
			} // end of if
			for (int vertex : vertices) {
				double hubScore = graph.getVertexWeight(vertex);
				hubScore /= norm; // normalise the hub values
				graph.setVertexWeight(vertex, hubScore);
			} // end of for (int vertex : vertices)
		} // end of for (int step = 0;step < numOfSteps;step++)
	} // end of runHITSalgorithm

	/*********************************************************
	 * @param vertices
	 * @param thematicSemanticGraph
	 * @param defaultWeight
	 * @param wiki_hrefWeight
	 * @param templateWeight
	 * @param infoboxWeight
	 * @param virtGraph
	 */
	public double calculateHubs(int[] vertices, SemanticGraph thematicSemanticGraph, double infoboxWeight, double templateWeight, double wiki_hrefWeight, double defaultWeight, VirtGraph virtGraph) {
		thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
		double norm = 0;
		for (int vertex : vertices) {
			double hubScore = 0;
			String en1 = localNames.get(vertex);
			int[] outgoingNeighbors = thematicSemanticGraph.getOutNeighbours(vertex).toIntArray();
			for (int outVertex : outgoingNeighbors) {
				double outVerAuthorityScore = thematicSemanticGraph.getVertexWeight(outVertex);
				String en2 = localNames.get(outVertex);
				List<String> relations = getRelationsBetweenTwoEntities(virtGraph, en1, en2);
				for (String p : relations) {
					double edgeWeight = 0;
					if (p.contains("infobox")) {
						edgeWeight = infoboxWeight;
					} else if (p.contains("template")) {
						edgeWeight = templateWeight;
					} else if (p.contains("wiki_href")) {
						edgeWeight = wiki_hrefWeight;
					} else {
						edgeWeight = defaultWeight;
					}
					hubScore += edgeWeight * outVerAuthorityScore;
				} // end of for (String p : relations)
			} // end of for (int outVertex : outgoingNeighbors)
			thematicSemanticGraph.setVertexWeight(vertex, hubScore);
			norm += Math.pow(hubScore, 2);
		} // end of for (int vertex : vertices)
		return norm;
	} // end of calculateHubs

	/*********************************************************
	 * @param vertices
	 *            .
	 * @param thematicSemanticGraph
	 * @param defaultWeight
	 * @param wiki_hrefWeight
	 * @param templateWeight
	 * @param infoboxWeight
	 * @param virtGraph
	 */
	public double calculateAuthorities(int[] vertices, SemanticGraph thematicSemanticGraph, double infoboxWeight, double templateWeight, double wiki_hrefWeight, double defaultWeight, VirtGraph virtGraph) {
		thematicSemanticGraph.getClass().getClassLoader().setDefaultAssertionStatus(false);
		double norm = 0;
		for (int vertex : vertices) {
			double authorityScore = 0;
			String en2 = localNames.get(vertex);
			int[] incomingNeighbors = thematicSemanticGraph.getInNeighbors(vertex).toIntArray();
			for (int inVertex : incomingNeighbors) {
				double inVerHubScore = thematicSemanticGraph.getVertexWeight(inVertex);
				String en1 = localNames.get(inVertex);
				List<String> relations = getRelationsBetweenTwoEntities(virtGraph, en1, en2);
				for (String p : relations) {
					double edgeWeight = 0;
					if (p.contains("infobox")) {
						edgeWeight = infoboxWeight;
					} else if (p.contains("template")) {
						edgeWeight = templateWeight;
					} else if (p.contains("wiki_href")) {
						edgeWeight = wiki_hrefWeight;
					} else {
						edgeWeight = defaultWeight;
					}
					authorityScore += edgeWeight * inVerHubScore;
				} // end of for (String p : relations)
			} // end of for (int inVertex : incomingNeighbors)
			thematicSemanticGraph.setVertexWeight(vertex, authorityScore);
			norm += Math.pow(authorityScore, 2);
		} // end of for (int vertex : vertices)
		return norm;
	} // end of calculateAuthorities

	/*********************************************************
	 * @param semanticGraph
	 * @param vertexId
	 * @param relationConfidence
	 * @param numOfRepetition
	 */
	public void calculateVertexInitialWeight(SemanticGraph semanticGraph, int vertexId, double relationConfidence, Integer numOfRepetition) {
		double w = 1 - (1 / (1 + (relationConfidence * numOfRepetition)));
		semanticGraph.setVertexWeight(vertexId, w);
	} // end of calculateVertexInitialWeight

	/**
	 * *******************************************************
	 * 
	 * @param u
	 * @param v
	 */
	public void constructAllConnectedComponents(int u, int v) {
		if (ccVertexSets.get(u) == null) {
			while (setRedirectionMap.get(u) != null) {
				u = setRedirectionMap.get(u);
			} // end of while
		}
		if (ccVertexSets.get(u) != ccVertexSets.get(v)) {
			if (ccVertexSets.get(v) != null) {
				ccVertexSets.get(u).addAll(ccVertexSets.get(v));
				ccVertexSets.remove(v);
				setRedirectionMap.put(v, u);
			} else {
				while (setRedirectionMap.get(v) != null) {
					v = setRedirectionMap.get(v);
				} // end of while
				if (u != v) {
					ccVertexSets.get(u).addAll(ccVertexSets.get(v));
					ccVertexSets.remove(v);
					setRedirectionMap.put(v, u);
				} // end of if
			} // end of if
		} // end of if
	} // end of constructAllConnectedComponents

	/**
	 * *******************************************************
	 * 
	 * @param semanticGraph
	 * @param largestCCKey
	 * @return
	 */
	public SemanticGraph findSecondLargestConnectedComponent(SemanticGraph semanticGraph, int largestCCKey) {
		SemanticGraph secondlargerstCCGraph = new SemanticGraph();
		ccVertexSets.remove(largestCCKey);
		int max = 0;
		int key = 0;
		for (Map.Entry<Integer, IntSet> entry : ccVertexSets.entrySet()) {
			if (entry.getValue().size() > max) {
				max = entry.getValue().size();
				key = entry.getKey();
			} // end of if
		} // end of for
		secondlargerstCCGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(ccVertexSets.get(key));
		addLabelToGraphVertices(secondlargerstCCGraph, semanticGraph.getVertexLabelProperty(), ccVertexSets.get(key));
		return secondlargerstCCGraph;
	} // end of findSecondLargestConnectedComponent

	/*********************************************************
	 * @param semanticGraph
	 * @param largestCComponent
	 */
	public SemanticGraph findSecondLargestConnectedComponent(SemanticGraph semanticGraph, IntSet largestCComponent) {
		SemanticGraph secondlargerstCCGraph = new SemanticGraph();
		Set<IntSet> allConnectedComponents = (Set<IntSet>) semanticGraph.getConnectedComponents();
		List<Integer> ccSizes = new ArrayList<Integer>();
		for (IntSet cc : allConnectedComponents) {
			ccSizes.add(cc.size());
		} // end of for
		Collections.sort(ccSizes, Collections.reverseOrder());
		if (ccSizes.size() > 1) {
			int secondlargerstCCsSize = ccSizes.get(1);
			if (secondlargerstCCsSize < largestCComponent.size()) {
				for (IntSet cc : allConnectedComponents) {
					if (cc.size() == secondlargerstCCsSize) {
						secondlargerstCCGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(cc);
						addLabelToGraphVertices(secondlargerstCCGraph, semanticGraph.getVertexLabelProperty(), cc);
						break;
					} // end of if
				} // end of for
			} else { // there are two Or more CC with the same size as Largest
						// CC in the Graph
				allConnectedComponents.remove(largestCComponent);
				for (IntSet cc : allConnectedComponents) {
					if (cc.size() == secondlargerstCCsSize) {
						secondlargerstCCGraph = (SemanticGraph) semanticGraph.getSubgraphInducedByVertices(cc);
						addLabelToGraphVertices(secondlargerstCCGraph, semanticGraph.getVertexLabelProperty(), cc);
						break;
					} // end of if
				} // end of for
			} // end of if if (secondlargerstCCsSize < largestCC.size())
		} // end of if (ccSizes.size() > 1)
		return secondlargerstCCGraph;
	} // end of findSecondLargestConnectedComponent

	/*********************************************************
	 * @param secondlargerstCCGraph
	 * @param vertexLabelProperty
	 * @param cc
	 */
	public void addLabelToGraphVertices(SemanticGraph secondlargerstCCGraph, LabelProperty vlProperty, IntSet vlist) {
		LabelProperty vlp = secondlargerstCCGraph.getVertexLabelProperty();
		int[] vertexList = vlist.toIntArray();
		for (int i = 0; i < vertexList.length; i++) {
			vlp.setValue(vertexList[i], vlProperty.getValue(vertexList[i]));
		} // end of for
	} // end of addLabelToGraphVertices

	private void addRelationsBetweenEntities(VirtGraph virtGraph, SemanticGraph semanticGraph, Map<String, Integer> wikipediaEntities) {
		int uriPrefixLength = uriPrefix.length();
		int categoryUriPrefixLength = categoryUriPrefix.length();
		for (Map.Entry<String, Integer> entry : wikipediaEntities.entrySet()) {
			StringBuilder queryString = new StringBuilder(400);
			String entName = entry.getKey();
			if (entName.indexOf('"') == -1) {
				int entId = entry.getValue();
				queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
				queryString.append("SELECT DISTINCT ?p ?o FROM <" + GRAPH + "> WHERE { ");
				queryString.append("<" + uriPrefix + entName + ">" + " ?p ?o . ");
				queryString.append("FILTER (!isliteral(?o)) }");
				com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
				// System.out.println("QUERY: " + queryString.toString());
				VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
				ResultSet results = vqe.execSelect();
				// System.out.println("Retrieved the Results.");
				// System.out.println("Iterating over the Results...");
				Set<Integer> relations = new HashSet<Integer>();
				relations.add(entId); // the entity might have relation to
										// itself
				Set<String> categoryList = new HashSet<String>();
				while (results.hasNext()) {
					QuerySolution result = results.nextSolution();
					String predicate = result.getResource("p").toString();
					if (predicate.contains("rdf-syntax-ns#type")) {
						String category = result.getResource("o").toString();
						int index = category.indexOf("/Category:");
						if (index != -1) {
							category = category.substring(categoryUriPrefixLength);
							categoryList.add(category);
						}
					} else {
						String localname = result.getResource("o").toString().substring(uriPrefixLength);
						if (wikipediaEntities.get(localname) != null) {
							int vertexId = wikipediaEntities.get(localname);
							if (!relations.contains(vertexId)) { // the entity might have multiple relations with another entity,
															     // so only one of them is important now!
								relations.add(vertexId);
								addEdgeToSemanticGraph(semanticGraph, entId, vertexId);
								constructAllConnectedComponents(entId, vertexId);
							} // end of if
						} // end of if
					}
				} // end of while
				if (!categoryList.isEmpty()) {
					entityCategoryMap.put(entId, categoryList);
				}
				vqe.close();
			} // end of if (entName.indexOf('"')
		} // end of for Map
	} // end of addRelationsBetweenEntities

	/*********************************************************
	 * @param virtGraph
	 * @param en1
	 * @param en2
	 */
	private List<String> getRelationsBetweenTwoEntities(VirtGraph virtGraph, String en1, String en2) {
		List<String> relations = new ArrayList<String>();
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
			queryString.append("SELECT DISTINCT ?p FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + en1 + ">" + " ?p " + "<" + uriPrefix + en2 + "> . ");
			queryString.append("}");
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				relations.add(result.getResource("p").toString());
			} // end of while
			vqe.close();
		}catch(Exception e) {
			logger.log(Level.WARNING, e.getMessage());
		}	
		return relations;
	} // end of getRelationsBetweenTwoEntities

	/**
	 * *******************************************************
	 * 
	 * @param virtGraph
	 * @param semanticGraph
	 * @param entName
	 * @param entId
	 */
	private void getEntityRelationList(VirtGraph virtGraph, SemanticGraph semanticGraph, Map<String, Integer> wikipediaEntities, String entName, int entId) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?p ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + entName + ">" + " ?p ?o . ");
		queryString.append("FILTER (!isliteral(?o)) }");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		// System.out.println("QUERY: " + queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		// System.out.println("Retrieved the Results.");
		// System.out.println("Iterating over the Results...");
		int uriPrefixLength = uriPrefix.length();
		Set<Integer> relations = new HashSet<Integer>();
		relations.add(entId); // the entity might have relation to itself
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String localname = result.getResource("o").toString().substring(uriPrefixLength);
			if (wikipediaEntities.get(localname) != null) {
				int vertexId = wikipediaEntities.get(localname);
				if (!relations.contains(vertexId)) { // the entity might have
														// multiple relations
														// with another entity,
														// so only one of them
														// is important now!
					relations.add(vertexId);
					addEdgeToSemanticGraph(semanticGraph, entId, vertexId);
					constructAllConnectedComponents(entId, vertexId);
				} // end of if
			} // end of if
		} // end of while
		vqe.close();
	} // end of getEntityRelationList
	
	private Set<String> getRelationsReferingToEntity(VirtGraph virtGraph, String entName) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
		queryString.append(" ?s ?p <" + uriPrefix + entName + "> . }");
		Set<String> relations = new HashSet<String>();
		QuerySolution result = null;
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			int uriPrefixLength = uriPrefix.length();
			while (results.hasNext()) {
				result = results.nextSolution();
				String localname = result.getResource("s").toString().substring(uriPrefixLength);
				// skip the categories
				if (localname.indexOf('%') != -1 || localname.startsWith("_")) {
					continue;
				}
				relations.add(localname);
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error: " + e.getMessage() + "   " + entName);
		}
		return relations;
	} // end of getRelationsReferingToEntity
	
	
	private Set<String> getEntityNonLiteralRelationList(VirtGraph virtGraph, String entName) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?p ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + entName + ">" + " ?p ?o . ");
		queryString.append("FILTER (!isliteral(?o)) }");
		Set<String> relations = new HashSet<String>();
		QuerySolution result = null;
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			int uriPrefixLength = uriPrefix.length();
			while (results.hasNext()) {
				result = results.nextSolution();
				String localname = result.getResource("o").toString().substring(uriPrefixLength);
				// skip the categories
				if (localname.indexOf("Category:") != -1) {
					continue;
				}else if (localname.indexOf('%') != -1 || localname.startsWith("_")) {
					continue;
				}
				relations.add(localname);
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error: " + e.getMessage() + "   " + entName);
		}
		return relations;
	} // end of getEntityNonLiteralRelationList
	
	
	private Set<String> getEntityLiteralRelationList(VirtGraph virtGraph, String entName) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?p ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + entName + ">" + " ?p ?o . ");
		queryString.append("FILTER (isliteral(?o)) }");
		Set<String> relations = new HashSet<String>();
		QuerySolution result = null;
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			while (results.hasNext()) {
				result = results.nextSolution();
				String localname = result.getLiteral("o").toString();
				relations.add(localname);
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error in literal: " + e.getMessage() + "   " + entName);
		}
		return relations;
	} // end of getEntityLiteralRelationList

	
	public Set<String> getEntityCategoriesList(VirtGraph virtGraph, String entName) {
		int categoryUriPrefixLength = categoryUriPrefix.length();
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + entName + ">" + " a ?o . ");
		queryString.append(" }");
		Set<String> cats = new HashSet<String>();
		QuerySolution result = null;
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			while (results.hasNext()) {
				result = results.nextSolution();
				String category = result.getResource("o").toString();
				category = category.substring(categoryUriPrefixLength);
				cats.add(category);
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error in query: " + e.getMessage() + "   " + entName);
		}
		return cats;
	} // end of getEntityCategoriesList
	
	

	/**
	 * *******************************************************
	 * 
	 * @param virtGraph
	 * @param en
	 * @return
	 */
	private Set<Integer> getEntityRelationList(VirtGraph virtGraph, Map<String, Integer> wikipediaEntities, String en) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?p ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + en + ">" + " ?p ?o . ");
		queryString.append("FILTER (!isliteral(?o)) }");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		// System.out.println("QUERY: " + queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		// System.out.println("Retrieved the Results.");
		// System.out.println("Iterating over the Results...");
		Set<Integer> relations = new HashSet<Integer>();
		int uriPrefixLength = uriPrefix.length();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String localname = result.getResource("o").toString().substring(uriPrefixLength);
			if (wikipediaEntities.get(localname) != null) {
				relations.add(wikipediaEntities.get(localname));
			} // end of if
		} // end of while
		vqe.close();
		return relations;
	} // end of getEntityRelationList

	/*********************************************************
	 * @param virtGraph
	 * @param en1
	 * @param en2
	 */
	private List<Boolean> getEntityRelationList(VirtGraph virtGraph, String en1, String en2) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT ?p1 ?p2 FROM <" + GRAPH + "> WHERE { ");
		queryString.append("OPTIONAL { ");
		queryString.append("<" + uriPrefix + en1 + ">" + " ?p1 " + "<" + uriPrefix + en2 + "> . } ");
		queryString.append("OPTIONAL { ");
		queryString.append("<" + uriPrefix + en2 + ">" + " ?p2 " + "<" + uriPrefix + en1 + "> . } ");
		queryString.append("}");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		// System.out.println("QUERY: " + queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		// System.out.println("Retrieved the Results.");
		// System.out.println("Iterating over the Results...");
		List<Boolean> relations = new ArrayList<Boolean>();
		boolean e1ToE2HasRelation = false;
		boolean e2ToE1HasRelation = false;
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			if (result.get("p1") != null) {
				e1ToE2HasRelation = true;
			}
			if (result.get("p2") != null) {
				e2ToE1HasRelation = true;
			}
			if (e1ToE2HasRelation && e2ToE1HasRelation) {
				break;
			}
		} // end of while
		relations.add(e1ToE2HasRelation);
		relations.add(e2ToE1HasRelation);
		vqe.close();
		return relations;
	} // end of getEntityRelationList

	/**
	 * *******************************************************
	 * 
	 * @param graph
	 * @param fromVertex
	 * @param toVertex
	 */
	public void addEdgeToSemanticGraph(SemanticGraph graph, int fromVertex, int toVertex) {
		graph.addDirectedSimpleEdge(fromVertex, toVertex);
	} // end of addVertexToSemanticGraph

	/**
	 * *******************************************************
	 * 
	 * @param graph
	 * @param fromVertex
	 * @param toVertex
	 * @param mode
	 */
	public void addEdgeToSemanticGraph(SemanticGraph graph, int fromVertex, int toVertex, int mode) {
		if (mode == 1) { // from V1 => V2 ONLY
			graph.addDirectedSimpleEdge(fromVertex, toVertex);
		} else if (mode == 2) { // from V1 => V2 AND from V2 => V1
			graph.addDirectedSimpleEdge(fromVertex, toVertex);
			graph.addDirectedSimpleEdge(toVertex, fromVertex);
		} // end of if
	} // end of addVertexToSemanticGraph

	/**
	 * *******************************************************
	 * 
	 * @param graph
	 * @param vertexId
	 * @param vertexLabel
	 * @param vertexLabelProperty
	 */
	public void addVertexToSemanticGraph(SemanticGraph graph, int vertexId, String vertexLabel, LabelProperty vertexLabelProperty) {
		graph.addVertex(vertexId);
		vertexLabelProperty.setValue(vertexId, vertexLabel);
	} // end of addVertexToSemanticGraph
	
	
	/*********************************************************
	 * @param foundEntities
	 * @param timeRelatedWords
	 * @return
	 */
	public Set<String> removeTimeRelatedWordsFromEntities(Set<String> foundEntities, Set<String> timeRelatedWords) {
		Iterator<String> it = foundEntities.iterator();
		while (it.hasNext()) {
			String entity = it.next();
			if (timeRelatedWords.contains(entity)) {
				it.remove();
			} // end of if
		} // end of while
		return foundEntities;
	} // end of removeTimeRelatedWordsFromEntities
	
	/*********************************************************
	 * @param foundEntities
	 * @param stopWords
	 */
	public Set<String> removeStopWordsFromEntities(Set<String> foundEntities, Set<String> stopWords) {
		Iterator<String> it = foundEntities.iterator();
		while (it.hasNext()) {
			String entity = it.next();
			if (stopWords.contains(entity)) {
				it.remove();
				// foundEntities.remove(entity);
			} // end of if
		} // end of while
		return foundEntities;
	} // end of removeStopWordsFromEntities

	/*********************************************************
	 * 
	 */
	public List<State> loadDeterministicGraphToMemory() {
		String fileName = DIR_PATH + "deterministicGraph.ser";
		System.out.println("Loading Graph Nodes into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			List<State> gNodes = (ArrayList<State>) in.readObject();
			in.close();
			System.out.println("Graph Nodes Loaded successfully into Memory.\n");
			return gNodes;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadDeterministicGraphToMemory

	public Map<Integer, Integer> loadFailureFunctionMapToMemory() {
		String fileName = DIR_PATH + "stateFailureMap.ser";
		System.out.println("Loading Failure Function Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<Integer, Integer> failureMap = (HashMap<Integer, Integer>) in.readObject();
			in.close();
			System.out.println("Failure Function Map Loaded successfully into Memory.\n");
			return failureMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadFailureFunctionMapToMemory

	public Map<Integer, List<String>> loadOutputMapToMemory() {
		String fileName = DIR_PATH + "outputMap.ser";
		System.out.println("Loading Output Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<Integer, List<String>> outputMap = (HashMap<Integer, List<String>>) in.readObject();
			in.close();
			System.out.println("Output Map Loaded successfully into Memory.\n");
			return outputMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadOutputMapToMemory

	/*********************************************************
	 * 
	 */
//	public void createPatternMatchingAutomata() {
//		Map<String, Set<Entity>> allNames = DbProcessor.loadUniqueNameListToMemory();
//		WordToIntMapperClass wordMapperClass = DbProcessor.loadwordToIntListToMemory();
//		wordToIntMap = wordMapperClass.getWordToInt();
//		totalNumOfNodes = allNames.size();
//		// List<String> wlist = wordMapperClass.getWordList();
//		graphNodes = new ArrayList<State>(totalNumOfNodes * 2);
//		failureMap = new HashMap<Integer, Integer>(totalNumOfNodes * 2);
//		startState = new State(0);
//		graphNodes.add(startState);
//		outputSet = new HashMap<Integer, List<String>>(totalNumOfNodes);
//		Set<Integer> firstWords = new HashSet<Integer>(allNames.size());
//		int i = 1;
//		String entityName = "";
//		for (Map.Entry<String, Set<Entity>> entry : allNames.entrySet()) {
//			entityName = entry.getKey().replace("'s", "").replace("'S", "").replace("'t", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "");
//			enter(entityName);
//			firstWords.add(wordToIntMap.get(entityName.split(" ")[0]));
//			if (i % interval == 0) {
//				System.out.println(i);
//			}
//			i++;
//		} // end of for
//		System.out.println("Deterministic Automata Size: " + graphNodes.size());
//		System.out.println("First Words List Size: " + firstWords.size());
//		totalNumOfNodes = graphNodes.size();
//		String fileName = DIR_PATH + "deterministicGraph.ser";
//		System.out.println("Serializing Deterministic Automata...");
//		try {
//			FileOutputStream outputFile = new FileOutputStream(fileName);
//			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
//			ObjectOutputStream out = new ObjectOutputStream(bfout);
//			out.writeObject(graphNodes);
//			out.flush();
//			out.close();
//			System.out.println("Deterministic Automata Serialized successfully.");
//			constructFailureFunction(firstWords);
//			System.out.println("Serilizaing State Failure Map...");
//			fileName = DIR_PATH + "stateFailureMap.ser";
//			outputFile = new FileOutputStream(fileName);
//			bfout = new BufferedOutputStream(outputFile);
//			out = new ObjectOutputStream(bfout);
//			out.writeObject(failureMap);
//			out.flush();
//			out.close();
//			System.out.println("State Failure Serialized successfully.");
//			System.out.println("Serilizaing Output Map...");
//			fileName = DIR_PATH + "outputMap.ser";
//			outputFile = new FileOutputStream(fileName);
//			bfout = new BufferedOutputStream(outputFile);
//			out = new ObjectOutputStream(bfout);
//			out.writeObject(outputSet);
//			out.flush();
//			out.close();
//			System.out.println("Output Map Serialized successfully.");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//
//		}
//	} // end of createPatternMatchingAutomata

	public String readDocument(String fileName) {
//		String filename = "/home/mehdi/" + fileName;
//		String filename = "/home/mehdi/" + fileName + ".txt";
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			} // end of while
			br.close();
//			System.out.println("=============== Document Start ================");
//			System.out.println(content);
//			System.out.println("=============== Document End   ================\n\n");
		} catch (FileNotFoundException e) {
			System.out.println("\"" + fileName + "\" does NOT exits. Please enter a valid file name.");
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return content;
	} // end of readDocument

	public Set<String> readStopWords() {
		String filename = CONF_DIR_PATH + "stopwords.txt";
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line;
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Set<String> stopWords = new HashSet<String>(Arrays.asList(content.split(",")));
		return stopWords;
	} // end of readStopWords
	
	
	public Set<String> readTimeRelatedEntities() {
		String filename = CONF_DIR_PATH + "timeRelatedEntities.txt";
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line;
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Set<String> entities = new HashSet<String>(Arrays.asList(content.split(",")));
		return entities;
	} // end of readStopWords

	/*********************************************************
	 * @param state
	 * @return
	 */
	private void printOutput(int state) {
		// System.out.println("state: " + state + " " + outputSet.get(state));
		System.out.println(outputSet.get(state));
	}

	/*********************************************************
	 * @param firstWords
	 */
	public void constructFailureFunction(Set<Integer> firstWords) {
		List<Integer> queue = new ArrayList<Integer>(totalNumOfNodes);
		for (int word : firstWords) {
			int s = g(0, word);
			if (s != 0) {
				queue.add(s);
			}
			f(s, 0);
		} // end of for
		int currentItem = 0;
		System.out.println("Queue Size: " + queue.size());
		// while (!queue.isEmpty() && !flag) {
		while (currentItem != (totalNumOfNodes - 1)) {
			int qstate = queue.get(currentItem);
			// queue.remove(currentItem);
			currentItem++;
			if (currentItem % 5000 == 0) {
				System.out.println(currentItem + "      " + queue.size());
			}
			Map<Integer, Integer> outGoingEdges = getOutgoingEdges(qstate);
			if (!outGoingEdges.isEmpty()) {
				Iterator<Integer> it = outGoingEdges.keySet().iterator();
				while (it.hasNext()) {
					int edge = it.next();
					int st = g(qstate, edge);
					if (st != FAIL) {
						queue.add(st);
						// if (queue.size() > totalNumOfNodes) {
						// flag = true;
						// break;
						// }
						int state = f(qstate);
						while (g(state, edge) == FAIL) {
							state = f(state);
						} // end of while
						f(st, g(state, edge));
						outputUnion(st, f(st));
					} // end of if
				} // end of while
			} // end of if
		} // end of while
		System.out.println(queue.size());
	} // end of constructFailureFunction

	/*********************************************************
	 * @param st1
	 * @param st2
	 */
	public void outputUnion(int st1, int st2) {
		if (outputSet.containsKey(st1) && outputSet.containsKey(st2)) {
			outputSet.get(st1).addAll(outputSet.get(st2));
		} // end of if
	} // end of outputUnion

	/*********************************************************
	 * @param r
	 * @return
	 */
	public Map<Integer, Integer> getOutgoingEdges(int stId) {
		return graphNodes.get(stId).getEdges();
	}

	/*********************************************************
	 * @param s
	 * @param i
	 */
	public void f(int state, int value) {
		// if (failureMap.get(state) == null) {
		failureMap.put(state, value);
		// }
	} // end of f

	public int f(int state) {
		if (failureMap.get(state) != null) {
			return failureMap.get(state);
		} else {
			return -1;
		}
	} // end of f

	public void enter(String keyword) {
		int state = 0;
		int j = 0;
		String[] a = keyword.split(" ");
		while (gg(state, wordToIntMap.get(a[j])) != FAIL) {
			state = gg(state, wordToIntMap.get(a[j]));
			j++;
			if (j >= a.length)
				break;
		} // end of while
		for (int p = j; p < a.length; p++) {
			newstate++;
			g(state, wordToIntMap.get(a[p]), newstate);
			state = newstate;
		} // end of for
		output(state, keyword);
	} // end of enter

	/*********************************************************
	 * @param state
	 * @param keyword
	 */
	public void output(int state, String keyword) {
		if (outputSet.containsKey(state)) {
			Collections.sort(outputSet.get(state));
			int pos = Collections.binarySearch(outputSet.get(state), keyword);
			if (pos == -1)
				outputSet.get(state).add(keyword);
		} else {
			List<String> ls = new ArrayList<String>();
			ls.add(keyword);
			outputSet.put(state, ls);
		}
	}

	public void g(int state, int edge, int newstate) {
		addNode(state, edge, newstate);
	} // end of g

	/*********************************************************
	 * @param state
	 */
	public List<String> output(int state) {
		return outputSet.get(state);
		// System.out.println("state: " + state + " " + outputSet.get(state));
	}

	/*********************************************************
	 * @param state
	 * @param edge
	 * @return
	 */
	public int gg(int state, int edge) {
		return nextNode(state, edge);
	} // end of g

	public int g(int state, int edge) {
		int nnode = nextNode(state, edge);
		if (state == 0 && nnode == -1) {
			return 0;
		} else {
			return nnode;
		}
	} // end of g

	public void addNode(int from, int edge, int to) {
		if (graphNodes.get(from).findNeighbor(edge) != to) {
			State st = new State(to);
			graphNodes.add(to, st);
			graphNodes.get(from).addNeighbor(edge, st.getId());
		}
	} // end of addNode

	public int nextNode(int state, int edge) {
		return graphNodes.get(state).findNeighbor(edge);
	} // end of nextNode
	
	
}
