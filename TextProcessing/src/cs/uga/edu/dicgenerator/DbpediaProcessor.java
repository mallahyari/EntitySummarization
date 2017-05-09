/**
 *
 */
package cs.uga.edu.dicgenerator;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.*;
import grph.algo.bfs.BFSResult;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.python.core.util.FileUtil;

import cnrs.grph.set.HashIntSet;
import cnrs.grph.set.IntSet;

import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import cs.uga.edu.wikiaccess.WikipediaAccessLayer;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

/**
 * @author mehdi
 *
 */
public class DbpediaProcessor{

	private VirtGraph virtGraph = null;
	private final int interval = 100000;
	private final String CONF_DIR_PATH = "/home/mehdi/";
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String wikiCategoryUriPrefix = "http://dbpedia.org/resource/Category:";
	private final String yagoUriPrefix = "http://yago-knowledge.org/resource/";
	private final int UNIQUE_ENTITY_NAMES = 10000000;
	private String DIR_PATH = "/home/mehdi/serializedFiles/";
	private int idGenerator = 0;
	private List<String> localNameList = null;
//	private List<Map<Integer, Entity_Old_Version>> dicContainer = null; 
	private Map<String, Integer> wordToIntMap = null;
	private Map<Integer, List<String>> outputSet = null;
	private Map<String, Integer> entityRepetition = null;
	private Map<Integer, Set<String>> entityCategoryMap = null;
	private Map<Integer, IntSet> ccVertexSets = null;
	private Map<Integer, Integer> setRedirectionMap = null;
	private int newstate = 0;
	private State startState = null;
	private List<State> graphNodes = null;
	private Map<Integer, Integer> failureMap = null;
	private int totalNumOfNodes = 1;
	private final int FAIL = -1;
	private final int maxNumOfAcceptableEntities = 5000;
	private final Logger logger = Logger.getLogger(DbpediaProcessor.class.getName());
	private final String [ ] wikipedia_internal_categories = {"wikipedia", "wikiprojects", "lists", "media wiki", "template", "user",
			"portal", "categories", "articles", "pages"};
	private List<String> localNames = null;
	private Map<String, Integer> namesToInt = null;
	

	private double wikipediaSize = 4017666;


	public DbpediaProcessor() {

	}
	
	public void downloadDbpediaPagesForCorpusConcepts(String concepts_dir, String output_dir) {
		virtGraph = connectToVirtuoso();
		Set<String> existingEntities = new HashSet<String>();
		File conceptsDirectory = new File(concepts_dir);
		for (File ifile : conceptsDirectory.listFiles()) {
			String fileName = ifile.getName();
			String doc = readDocument(concepts_dir + fileName);
			Set<String> entities = new HashSet<String>(Arrays.asList(doc.split(",")));
			System.out.println("number of entities: " + entities.size());
			int i = 0;
			for (String entName : entities) {
				entName = entName.trim();
				if (existingEntities.contains(entName)) {
					continue;
				}
				existingEntities.add(entName);
				StringBuffer queryString = new StringBuffer(400);
				queryString.append("SELECT (str(?abs) as ?abstract) (str(?lbl) as ?label) FROM <" + GRAPH + "> WHERE { ");
				queryString.append("<" + uriPrefix + entName + ">" + " <http://dbpedia.org/ontology/abstract> ?abs . ");
				queryString.append("<" + uriPrefix + entName + ">" + " <http://www.w3.org/2000/01/rdf-schema#label> ?lbl . ");
				queryString.append("}");
				try{
					com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
					// System.out.println("QUERY: " + queryString.toString());
					VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
					ResultSet results = vqe.execSelect();
					i++;
					while (results.hasNext()) {
						QuerySolution result = results.nextSolution();
						String abs = result.get("abstract").toString();
//						String label = result.get("label").toString();
						File file = new File(output_dir + entName + ".txt");
						FileWriter fileWriter = new FileWriter(file);
						fileWriter.write(abs);
						fileWriter.flush();
						fileWriter.close();
					} // end of while
					vqe.close();
					if (i % 1000 == 0) System.out.println(i);
				}catch(Exception e) {
					System.out.println("Error: " + e.getMessage() + "   " + entName);
				}
			}
		}
	
	}
	
	
	public void downloadWikipediaPagesForCorpusConcepts(String concepts_dir, String output_dir) {
		String wiki_URL = "http://en.wikipedia.org/wiki/";
		Document document = null;
		Connection conn = null;
		// We would like to filter out some irrelevant entities //
//		String [ ] domainEntities = readDocument("/home/mehdi/domainEntities/allConcepts.txt").split(",");
//		Set<String> allDomainEntities = new HashSet<String>();
//		for (String ent : domainEntities) {
//			allDomainEntities.add(ent.trim());
//		} // end of for
//		String CONCEPTS_DIR_PATH = "/home/mehdi/otm/relatedconcepts/";
		File conceptsDirectory = new File(concepts_dir);
		int i = 0;
		int minNumOfWords = 300;
		Set<String> existingEntities = new HashSet<String>();
		for (File ifile : conceptsDirectory.listFiles()) {
			String fileName = ifile.getName();
			String doc = readDocument(concepts_dir + fileName);
//			String [] entities = doc.split(",");
			Set<String> entities = new HashSet<String>(Arrays.asList(doc.split(",")));
//			entities.add(fileName.replace(".txt", ""));
			for (String entity : entities) {
				entity = entity.trim();
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
					String [] words = content.text().split(" ");
					if (words.length > minNumOfWords){
						File file = new File(output_dir + entity.trim() + ".txt");
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
		System.out.println("Total number of corpus Wikipedia concepts: " + existingEntities.size());
	} // end of downloadWikipediaPagesForCorpusConcepts
	
	public void downloadWikipediaPagesForCorpusConcepts(Set<String> articles, String output_dir) {
		String wiki_URL = "http://en.wikipedia.org/wiki/";
		Document document = null;
		Connection conn = null;
		// We would like to filter out some irrelevant entities //
//		String [ ] domainEntities = readDocument("/home/mehdi/domainEntities/allConcepts.txt").split(",");
//		Set<String> allDomainEntities = new HashSet<String>();
//		for (String ent : domainEntities) {
//			allDomainEntities.add(ent.trim());
//		} // end of for
//		String CONCEPTS_DIR_PATH = "/home/mehdi/otm/relatedconcepts/";
		int i = 0;
		int minNumOfWords = 200;
		Set<String> existingEntities = new HashSet<String>();
			for (String entity : articles) {
				entity = entity.trim();
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
					String [] words = content.text().split(" ");
					if (words.length > minNumOfWords){
						File file = new File(output_dir + entity.trim() + ".txt");
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
		System.out.println("Total number of corpus Wikipedia concepts: " + existingEntities.size());
	} // end of downloadWikipediaPagesForCorpusConcepts
	
	
	
	public void FilteNonRelevantEntitiesFromFile(String input_dir, String output_dir) {
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		Set<String> timeRelatedEntities = readTimeRelatedEntities();
		Set<String> totalEntities = new HashSet<String>();
		try {
			File fileDirectory = new File(input_dir);
			int i = 0;
			for (File file : fileDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(input_dir + fileName);
				Set<String> refinedEntities = new HashSet<String>();
				String [ ] entities = document.split(",");
//				allCorpusEntities.addAll(Arrays.asList(entities));
				for (String entity : entities) {
					entity = entity.trim();
					if (entity.indexOf('?') != -1 || entity.indexOf('/') != -1 || entity.indexOf('%') != -1 || entity.indexOf("_(disambiguation") != -1 || entity.indexOf('\'') != -1 || entity.indexOf(":") != -1 || entity.indexOf('\"') != -1 || entity.indexOf('^') != -1 || entity.startsWith("_") || entity.startsWith(".") || entity.indexOf("_(number") != -1) {
						continue;
					}
					if (timeRelatedEntities.contains(entity.toLowerCase())) {
						continue;
					}
					if (isWikipediaArticle(virtGraph, entity)) {
						refinedEntities.add(entity);
						totalEntities.add(entity);
					}
				} // end of for
				if (refinedEntities.size() > 0) {
					FileWriter fw = new FileWriter(output_dir + fileName);
					String line = refinedEntities.toString().replace("[", "").replace("]", "");
					fw.write(line);
					fw.flush();
					fw.close();
				}
				System.out.println("File " + (++i) + " is done.");
			} // end of for
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("Total number of entities in the corpus which have \"Abstract\": " + totalEntities.size());
		virtGraph.close();
	} // end of FilteNonRelevantEntitiesFromFile
	
	private boolean isWikipediaArticle(VirtGraph virtGraph, String entName) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + entName + ">" + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o . ");
		queryString.append("<" + uriPrefix + entName + ">" + " <http://dbpedia.org/ontology/abstract> ?o1 . ");
		queryString.append("}");
		boolean isArticle = false;
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			while (results.hasNext()) {
				isArticle = true;
				results.nextSolution();
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error: " + e.getMessage() + "   " + entName);
		}
		return isArticle;
	} // end of isWikipediaArticle
	
	
	public void identifyAndCreateEntityFiles(String input_dir, String emention_dir, String docentity_dir) {
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
		Set<String> stopWords = readStopWordsSet();
//		Set<String> timeRelatedWords = readTimeRelatedEntities();
//		System.out.println("\nConnecting to Virtuoso ... ");
//		VirtGraph virtGraph = connectToVirtuoso();
//		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
//			FileHandler fh = new FileHandler(OUTPUT_DIR + "ent.log");
//			fh.setFormatter(new SimpleFormatter());
//			logger.addHandler(fh);
//			logger.setLevel(Level.INFO);
			File inputDirectory = new File(input_dir);
			JsonReader jsonReader = new JsonReader(new FileReader(CONF_DIR_PATH + "configurations.json"));
			JsonParser jsonParser = new JsonParser();
			JsonObject confObject = jsonParser.parse(jsonReader).getAsJsonObject();
			int numOfEntityMatchAttr = confObject.get("numOfEntityMatchAttr").getAsInt();
			JsonObject entityMatchAttrWeightObj = confObject.get("entityMatchAttrWeight").getAsJsonObject();
			int fcounter = 0;
			for (File file : inputDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(input_dir + fileName);
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
								int repetition = entityRepetition.get(curEntity.toLowerCase()) != null ? entityRepetition.get(curEntity.toLowerCase()) : 0;
								repetition++;
								entityRepetition.put(curEntity.toLowerCase(), repetition);
								initialEntities.addAll(entities);
							} // end of if
						} // end of if (wordId != -1)
					} // end of for
				} // end of for
				foundEntities = removeStopWordsFromEntities(foundEntities, stopWords);
//				foundEntities = removeTimeRelatedWordsFromEntities(foundEntities, timeRelatedWords);
				FileWriter fw = new FileWriter(emention_dir + fileName);
				String line = foundEntities.toString().replace("[", "").replace("]", "");
				fw.write(line);
				fw.close();

				Set<String> duplicateMentions = new HashSet<String> (foundEntities.size());
				Map<String, Integer> wikipediaEntities = new HashMap<String, Integer>(foundEntities.size() * 5);
				ccVertexSets = new HashMap<Integer, IntSet>(foundEntities.size() * 5);
				setRedirectionMap = new HashMap<Integer, Integer>(foundEntities.size() * 5);
				Iterator<String> it = foundEntities.iterator();
//				int ecounter = 0;
				if (numOfEntityMatchAttr == 1) {
					double titleWeight = entityMatchAttrWeightObj.get("title").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
//								ecounter++;
								String enName = localNames.get(et.geteId());
								if (et.getAttr() == '1' && !duplicateMentions.contains(enName.toLowerCase())) {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									duplicateMentions.add(enName.toLowerCase());
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
//								ecounter++;
								String enName = localNames.get(et.geteId());
								if ((et.getAttr() == '1' || et.getAttr() == '2') && !duplicateMentions.contains(enName.toLowerCase())) {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									duplicateMentions.add(enName.toLowerCase());
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
					double disambiguationNameWeight = entityMatchAttrWeightObj.get("disambiguationName").getAsDouble();
					while (it.hasNext()) {
						String en = it.next();
						Set<Entity> enSet = uniqueNames.get(en);
						//						System.out.println("Entity Mention: " + en);
						if (enSet != null) {
							for (Entity et : enSet) {
//								ecounter++;
								String enName = localNames.get(et.geteId());
								if ((et.getAttr() == '1' || et.getAttr() == '2' || et.getAttr() == '3') && !duplicateMentions.contains(enName.toLowerCase())) {
									addVertexToSemanticGraph(semanticGraph, et.geteId(), enName, vertexLabelProperty);
									IntSet set = new HashIntSet();
									set.add(et.geteId());
									ccVertexSets.put(et.geteId(), set);
									wikipediaEntities.put(enName, et.geteId());
									duplicateMentions.add(enName.toLowerCase());
									if (topRWScoreConceptsMentions.contains(en)) {
										topRWScoreConcepts.add(enName);
									} // end of if
									if (et.getAttr() == '1') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), titleWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '2') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), redirectNameWeight, entityRepetition.get(en));
									} else if (et.getAttr() == '3') {
										calculateVertexInitialWeight(semanticGraph, et.geteId(), disambiguationNameWeight, entityRepetition.get(en));
									}
									//									System.out.println("Entity: " + enName + "  Attr: " + et.getAttr());
								}// end of if
							} // end of for (Entity et : enSet)
							//							System.out.println();
						} // end of if
					} // end of while
				} 				
//				addRelationsBetweenEntities(virtGraph, semanticGraph, wikipediaEntities);
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
				fw = new FileWriter(docentity_dir + fileName);
				line = largestCComponentEntities.toString().replace("[", "").replace("]", "");
				fw.write(line);
				fw.close();
				System.out.println(++fcounter);
			} // end of for (File file)
		}catch(Exception e) {
			e.printStackTrace();
		}
	} // end of identifyAndCreateEntityFiles
	
	public void addVertexToSemanticGraph(SemanticGraph graph, int vertexId, String vertexLabel, LabelProperty vertexLabelProperty) {
		graph.addVertex(vertexId);
		vertexLabelProperty.setValue(vertexId, vertexLabel);
	} // end of addVertexToSemanticGraph
	
	public void calculateVertexInitialWeight(SemanticGraph semanticGraph, int vertexId, double relationConfidence, Integer numOfRepetition) {
		double w = 1 - (1 / (1 + (relationConfidence * numOfRepetition)));
		semanticGraph.setVertexWeight(vertexId, w);
	} // end of calculateVertexInitialWeight
	
	private void addRelationsBetweenEntities(VirtGraph virtGraph, SemanticGraph semanticGraph, Map<String, Integer> wikipediaEntities) {
		int uriPrefixLength = uriPrefix.length();
		int categoryUriPrefixLength = wikiCategoryUriPrefix.length();
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
	
	
	public void createPatternMatchingAutomata() {
		Map<String, Set<Entity>> allNames = loadUniqueNameListToMemory();
		WordToIntMapperClass wordMapperClass = loadwordToIntListToMemory();
		wordToIntMap = wordMapperClass.getWordToInt();
		totalNumOfNodes = allNames.size();
		// List<String> wlist = wordMapperClass.getWordList();
		graphNodes = new ArrayList<State>(totalNumOfNodes * 2);
		failureMap = new HashMap<Integer, Integer>(totalNumOfNodes * 2);
		startState = new State(0);
		graphNodes.add(startState);
		outputSet = new HashMap<Integer, List<String>>(totalNumOfNodes);
		Set<Integer> firstWords = new HashSet<Integer>(allNames.size());
		int i = 1;
		String entityName = "";
		for (Map.Entry<String, Set<Entity>> entry : allNames.entrySet()) {
			entityName = entry.getKey().replace("'s", "").replace("'S", "").replace("'t", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace(".", "");
			if (isValid(entityName)) {
//				System.out.println(entityName);
				boolean flag = true;
				for(String word : entityName.split(" ")) {
					if (wordToIntMap.get(word) == null){
						flag = false;
						break;
					}
				} // end of for
				if (flag) {
					enter(entityName);
					firstWords.add(wordToIntMap.get(entityName.split(" ")[0]));
				}
				if (i % interval == 0) {
					System.out.println(i);
				}
				i++;
			} // end of if
		} // end of for
		System.out.println("Deterministic Automata Size: " + graphNodes.size());
		System.out.println("First Words List Size: " + firstWords.size());
		totalNumOfNodes = graphNodes.size();
		String fileName = DIR_PATH + "deterministicGraph.ser";
		System.out.println("Serializing Deterministic Automata...");
		try {
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(graphNodes);
			out.flush();
			out.close();
			System.out.println("Deterministic Automata Serialized successfully.");
			constructFailureFunction(firstWords);
			System.out.println("Serilizaing State Failure Map...");
			fileName = DIR_PATH + "stateFailureMap.ser";
			outputFile = new FileOutputStream(fileName);
			bfout = new BufferedOutputStream(outputFile);
			out = new ObjectOutputStream(bfout);
			out.writeObject(failureMap);
			out.flush();
			out.close();
			System.out.println("State Failure Serialized successfully.");
			System.out.println("Serilizaing Output Map...");
			fileName = DIR_PATH + "outputMap.ser";
			outputFile = new FileOutputStream(fileName);
			bfout = new BufferedOutputStream(outputFile);
			out = new ObjectOutputStream(bfout);
			out.writeObject(outputSet);
			out.flush();
			out.close();
			System.out.println("Output Map Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {

		}
	} // end of createPatternMatchingAutomata
	
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
			if (currentItem % 100000 == 0) {
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
	






	public Map<String, Set<Entity>> loadUniqueNameListToMemory() {
		String fileName = DIR_PATH + "allUniqueNames.ser";
		System.out.println("Loading All Unique Names into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<String, Set<Entity>> allNames = (Map<String, Set<Entity>>) in.readObject();
			in.close();
			System.out.println("Unique Names List Successfully Loaded into Memory.\n");
			return allNames;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadUniqueNameListToMemory



	public void mapAllUniqueEntityNamesToEntityIds() {
		LocalNameMapperClass lnMapperClass = loadLocalNameListToMemory();
		Map<String, Integer> localNameMap = lnMapperClass.getNameToInt();
		//  	List<String> localNames      = lnMapperClass.getLocalNameList();
		StringBuffer queryString = new StringBuffer();
		Set<String> stopWords = new HashSet<String>(Arrays.asList(readStopWords().split(",")));
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		queryString.append("SELECT ?uri ?label ?redirectEntity FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label .");
		queryString.append("FILTER NOT EXISTS { ");
		queryString.append("?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label .");
		queryString.append("FILTER ( REGEX(?uri,\"%\",\"i\") || REGEX(?uri,\"Category:\",\"i\") ) } ");
		queryString.append("OPTIONAL {");
		queryString.append("?uri <http://dbpedia.org/ontology/wikiPageRedirects> ?redirectEntity . ");
		queryString.append("FILTER NOT EXISTS { ");
		queryString.append("?redirectEntity <http://www.w3.org/2000/01/rdf-schema#label> ?rlabel .");
		queryString.append("FILTER ( REGEX(?redirectEntity,\"%\",\"i\") || REGEX(?redirectEntity,\"Category:\",\"i\") ) }} ");
		queryString.append("}");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		RDFNode uri = null;
		RDFNode label = null;
		RDFNode redirectEntity = null;
		String entityLocalName = "";
		String title = "";
		int numOfTriples = 0;
		int uriPrefixLength = uriPrefix.length();
		Map<String, Set<Entity>> allNames = new HashMap<String, Set<Entity>>(UNIQUE_ENTITY_NAMES);
		Entity e = null;
		System.out.println("Iterating over the Results...");
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples + " " + allNames.size());
			}
			uri = result.get("uri");
			label = result.get("label");
			redirectEntity = result.get("redirectEntity");
			title = label.toString().toLowerCase().replace("@en", "");
			if (redirectEntity != null) {
				entityLocalName = redirectEntity.toString();
				entityLocalName = entityLocalName.substring(uriPrefixLength);
				if (!stopWords.contains(title)) {
					if (isValid(title)) {
						if (localNameMap.get(entityLocalName) == null) {
							continue;
						}
						int eid = localNameMap.get(entityLocalName);
						e = new Entity(eid, '2');
						if (allNames.containsKey(title)) {
							allNames.get(title).add(e);
						}else {
							Set<Entity> elist = new HashSet<Entity>();
							elist.add(e);
							allNames.put(title, elist);
						} // end of if
					} // end of if
				} // end of if
			}else {
				entityLocalName = uri.toString();
				entityLocalName = entityLocalName.substring(uriPrefixLength);
				if (!stopWords.contains(title)) {
					if (isValid(title)) {
						if (localNameMap.get(entityLocalName) == null) {
							continue;
						}
						int eid = localNameMap.get(entityLocalName);
						e = new Entity(eid, '1');
						if (allNames.containsKey(title)) {
							allNames.get(title).add(e);
						}else {
							Set<Entity> elist = new HashSet<Entity>();
							elist.add(e);
							allNames.put(title, elist);
						} // end of if
					} // end of if
				} // end of if
			} // end of if
		} // end of while
		System.out.println("Mapping of Redirected Names is done for : " + allNames.size() + " Entities.");
		System.out.println("Mapping of Disambiguated Names begins...");
		vqe.close();
		queryString.setLength(0);
		queryString.append("SELECT ?uri ?label ?disambiguateEntity FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label .");
		queryString.append("FILTER NOT EXISTS { ");
		queryString.append("?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label .");
		queryString.append("FILTER ( REGEX(?uri,\"%\",\"i\") || REGEX(?uri,\"Category:\",\"i\") ) } ");
		queryString.append("?uri <http://dbpedia.org/ontology/wikiPageDisambiguates> ?disambiguateEntity . ");
		queryString.append("FILTER NOT EXISTS { ");
		queryString.append("?disambiguateEntity <http://www.w3.org/2000/01/rdf-schema#label> ?dlabel .");
		queryString.append("FILTER ( REGEX(?disambiguateEntity,\"%\",\"i\") || REGEX(?disambiguateEntity,\"Category:\",\"i\") ) } ");
		queryString.append("}");
		sparql = QueryFactory.create(queryString.toString());
		vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		uri = null;
		label = null;
		RDFNode disambiguateEntity = null;
		numOfTriples = 0;
		System.out.println("Iterating over the Results...");
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples + " " + allNames.size());
			}
			uri = result.get("uri");
			label = result.get("label");
			disambiguateEntity = result.get("disambiguateEntity");
			title = label.toString().toLowerCase().replace("@en", "").replace(" (disambiguation)","");
			entityLocalName = disambiguateEntity.toString();
			entityLocalName = entityLocalName.substring(uriPrefixLength);
			if (localNameMap.get(entityLocalName) == null) {
				continue;
			}
			int eid = localNameMap.get(entityLocalName);
			if (!stopWords.contains(title)) {
				if (isValid(title)) {
					e = new Entity(eid, '3');
					if (allNames.containsKey(title)) {
						allNames.get(title).add(e);
					}else {
						Set<Entity> elist = new HashSet<Entity>();
						elist.add(e);
						allNames.put(title, elist);
					} // end of if
				} // end of if
			} // end of if
		} // end of while
		vqe.close();
		System.out.println("Mapping of Disambiguated Names Ends.");
		System.out.println("Total number of unique names: " + allNames.size());
		System.out.println("Serializing All Names...");
		String fileName = DIR_PATH + "allUniqueNames.ser";
		try {
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(allNames);
			out.flush();
			out.close();
			System.out.println("All Names Serialized successfully.");
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {

		}
	} // end of mapAllUniqueEntityNamesToEntityIds





	/*********************************************************
	 * 
	 */
	public void mapAllUniqueEntityNamesToEntityIds1() {
		LocalNameMapperClass lnMapperClass = loadLocalNameListToMemory();
		Map<String, Integer> localNameMap = lnMapperClass.getNameToInt();
		//  	List<String> localNames      = lnMapperClass.getLocalNameList();
		StringBuffer queryString = new StringBuffer();
		Set<String> stopWords = new HashSet<String>(Arrays.asList(readStopWords().split(",")));
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		queryString.append("PREFIX lsdis: <http://lsdis.cs.uga.edu/wiki#>");
		queryString.append("SELECT ?s ?name ?redirect_name ?redirect_name_short ?name_short ?disambiguation_name ?disambiguation_name_short FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s lsdis:wiki_name ?name .");
		queryString.append("OPTIONAL {");
		queryString.append("?s lsdis:wiki_redirect_name ?redirect_name . }");
		queryString.append("OPTIONAL {");
		queryString.append("?s lsdis:wiki_redirect_name_short ?redirect_name_short . }");
		queryString.append("OPTIONAL {");
		queryString.append("?s lsdis:wiki_name_short ?name_short . }");
		queryString.append("OPTIONAL {");
		queryString.append("?s lsdis:wiki_disambiguation_name ?disambiguation_name . }");
		queryString.append("OPTIONAL {");
		queryString.append("?s lsdis:wiki_disambiguation_name_short ?disambiguation_name_short . }");
		queryString.append("}");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		RDFNode uri = null;
		RDFNode wikiname = null;
		RDFNode redirect_name = null;
		RDFNode redirect_name_short = null;
		RDFNode name_short = null;
		RDFNode disambiguation_name = null;
		RDFNode disambiguation_name_short = null;
		String entityURI = "";
		String title = "";
		String redirectName = "";
		String redirectNameShort = "";
		String nameShort = "";
		String disambiguationName = "";
		String disambiguationNameShort = "";
		int numOfTriples = 0;
		int uriPrefixLength = uriPrefix.length();
		Map<String, Set<Integer>> allNames = new HashMap<String, Set<Integer>>(UNIQUE_ENTITY_NAMES);
		System.out.println("Iterating over the Results...");
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples + " " + allNames.size());
			}
			uri = result.get("s");
			wikiname = result.get("name");
			redirect_name = result.get("redirect_name");
			redirect_name_short = result.get("redirect_name_short");
			name_short = result.get("name_short");
			disambiguation_name = result.get("disambiguation_name");
			disambiguation_name_short = result.get("disambiguation_name_short");
			entityURI = uri.toString();
			entityURI = entityURI.substring(uriPrefixLength);
			int eid = localNameMap.get(entityURI);
			title = wikiname.toString().toLowerCase();
			redirectName = redirect_name != null ? redirect_name.toString().toLowerCase() : " ";
			redirectNameShort = redirect_name_short != null ? redirect_name_short.toString().toLowerCase() : " ";
			nameShort = name_short != null ? name_short.toString().toLowerCase() : " ";
			disambiguationName = disambiguation_name != null ? disambiguation_name.toString().toLowerCase() : " ";
			disambiguationNameShort = disambiguation_name_short != null ? disambiguation_name_short.toString().toLowerCase() : " ";
			if (!stopWords.contains(title)) {
				if (isValid(title)) {
					if (allNames.containsKey(title)) {
						allNames.get(title).add(eid);
					}else {
						Set<Integer> elist = new HashSet<Integer>();
						elist.add(eid);
						allNames.put(title, elist);
					}
				}
			} // end of if
			if (!stopWords.contains(redirectName)) {
				if (isValid(redirectName)) {
					if (allNames.containsKey(redirectName)) {
						allNames.get(redirectName).add(eid);
					}else {
						Set<Integer> elist = new HashSet<Integer>();
						elist.add(eid);
						allNames.put(redirectName, elist);
					}
				}
			} // end of if
			if (!stopWords.contains(redirectNameShort)) {
				if (isValid(redirectNameShort)) {
					if (allNames.containsKey(redirectNameShort)) {
						allNames.get(redirectNameShort).add(eid);
					}else {
						Set<Integer> elist = new HashSet<Integer>();
						elist.add(eid);
						allNames.put(redirectNameShort, elist);
					}
				}
			} // end of if
			if (!stopWords.contains(nameShort)) {
				if (isValid(nameShort)) {
					if (allNames.containsKey(nameShort)) {
						allNames.get(nameShort).add(eid); 
					}else {
						Set<Integer> elist = new HashSet<Integer>();
						elist.add(eid);
						allNames.put(nameShort, elist);
					}
				}
			} // end of if
			if (!stopWords.contains(disambiguationName)) {
				if (isValid(disambiguationName)) {
					if (allNames.containsKey(disambiguationName)) {
						allNames.get(disambiguationName).add(eid);
					}else {
						Set<Integer> elist = new HashSet<Integer>();
						elist.add(eid);
						allNames.put(disambiguationName, elist);
					}
				}
			} // end of if
			if (!stopWords.contains(disambiguationNameShort)) {
				if (isValid(disambiguationNameShort)) {
					if (allNames.containsKey(disambiguationNameShort)) {
						allNames.get(disambiguationNameShort).add(eid);
					}else {
						Set<Integer> elist = new HashSet<Integer>();
						elist.add(eid);
						allNames.put(disambiguationNameShort, elist);
					}
				}
			} // end of if
		} // end of while
		System.out.println("Total number of unique names: " + allNames.size());
		//    System.out.println("Please hit \"return\" to continue...");
		//    Scanner sc = new Scanner(System.in);
		//    sc.nextLine();
		System.out.println("Serializing All Names...");
		String fileName = DIR_PATH + "allUniqueNames.ser";
		try {
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(allNames);
			out.flush();
			out.close();
			System.out.println("All Names Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {

		}
	} // end of mapAllUniqueEntityNamesToEntityIds
	
	
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
	
	public Set<Integer> findLinkedEntities(String entName, VirtGraph virtGraph) {
		StringBuffer queryString = new StringBuffer();
		Set<Integer> relations = new HashSet<Integer>();
		if (entName.contains("\"") || entName.contains("\'")) {
			return relations;
		}
		try {
			queryString.append("SELECT DISTINCT ?s1 ?s2 FROM <" + GRAPH + "> WHERE { ");
			queryString.append("{ ?s1 <http://dbpedia.org/ontology/wikiPageWikiLink> <" + uriPrefix + entName + "> . } ");
			queryString.append("UNION");
			queryString.append("{ <" + uriPrefix + entName + "> <http://dbpedia.org/ontology/wikiPageWikiLink> ?s2 . } ");
			queryString.append("}");
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			int uriPrefixLength = uriPrefix.length();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String localname1 = result.getResource("s1") != null ? result.getResource("s1").toString() : "";
				if (!localname1.equals("")) {
					localname1 = localname1.substring(uriPrefixLength);
					if (namesToInt.get(localname1) != null) {
						relations.add(namesToInt.get(localname1));
					}
				}else {
					String localname2 = result.getResource("s2") != null ? result.getResource("s2").toString() : "";
					if (!localname2.equals("")) {
						localname2 = localname2.substring(uriPrefixLength);
						if (namesToInt.get(localname2) != null) {
							relations.add(namesToInt.get(localname2));
						}
					}
				} // end of if
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error: " + e.getMessage() + " QUERY: " + queryString.toString());
		}
		return relations;
	} // end of findLinkedEntities
	

	public String readDocument() {
		String filename = "/home/mehdi/document.txt";
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line;
			} // end of while
			System.out.println(content);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	} // end of readDocument
	
	public String readDocument(String filename) {
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line;
			} // end of while
//			System.out.println(content);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	} // end of readDocument
	
	public List<String> getDocumentContent(String filename) {
		List<String> content = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content.add(line);
			} // end of while
//			System.out.println(content);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	} // end of getDocumentContent


	public ArrayList<Integer> getIntersection(ArrayList<Integer> l1, ArrayList<Integer> l2) {
		if (l1 == null) {
			return l2;
		}else if (l2 == null) {
			return l1;
		}else {
			ArrayList<Integer> intersection = new ArrayList<Integer>();
			Collections.sort(l2);
			Iterator<Integer> it = l1.iterator();
			while (it.hasNext()) {
				int eid = it.next();
				int pos = Collections.binarySearch(l2, eid);
				if (pos < 0){
					continue;
				}else {
					if (l2.get(pos) == eid) {
						intersection.add(eid);
					}
				}
				//				if (l2.contains(eid)) {
				//					intersection.add(eid);
				//				} // end of if
			} // end of while
			return intersection;
		} // end of if
	} // end of getIntersection

	public LocalNameMapperClass loadLocalNameListToMemory() {
		String fileName = DIR_PATH + "entityLocalNames.ser";
		System.out.println("Loading Entities Local Names into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			LocalNameMapperClass lnmapper = (LocalNameMapperClass) in.readObject();
			in.close();
			System.out.println("Entities Local Names Successfuly Loaded into Memory.\n");
			return lnmapper;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadLocalNameListToMemory

	public WordToIntMapperClass loadwordToIntListToMemory() {
		String fileName = DIR_PATH + "wordToInt.ser";
		System.out.println("Loading Word To Integer Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			WordToIntMapperClass wtoint = (WordToIntMapperClass) in.readObject();
			in.close();
			System.out.println("Word To Integer List Successfully Loaded into Memory.\n");
			return wtoint;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadwordToIntListToMemory

	public void mapEntityLocalNamesToInt() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		LocalNameMapperClass lnMapper = new LocalNameMapperClass();
		Map<String, Integer> nToInt = lnMapper.getNameToInt();
		ArrayList<String> lNameList = lnMapper.getLocalNameList();
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT DISTINCT ?uri FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?uri <http://www.w3.org/2000/01/rdf-schema#label> ?name .");
		queryString.append("FILTER NOT EXISTS { ");
		queryString.append("?uri <http://www.w3.org/2000/01/rdf-schema#label> ?name .");
		queryString.append("FILTER ( REGEX(?uri,\"%\",\"i\") || REGEX(?uri,\"Category:\",\"i\") ) } ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		System.out.println("Iterating over the results...");
		QuerySolution result = null;
		int numOfTriples = 0;
		RDFNode uri = null;
		String entityURI = "";
		String localName = "";
		int uriPrefixLength = uriPrefix.length();
		while (results.hasNext()) {
			result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			uri = result.get("uri");
			entityURI = uri.toString();
			localName = entityURI.substring(uriPrefixLength);
			lNameList.add(localName);
			nToInt.put(localName, numOfTriples - 1); // because numOfTriples begins with 1
		} // end of while
		lNameList.trimToSize();
		String fileName = DIR_PATH + "entityLocalNames.ser";
		try {
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(lnMapper);
			out.close();
			System.out.println("Entities Local Names Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of mapEntityLocalNamesToInt

	public void mapWordToInt() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		WordToIntMapperClass wToIntMapper = new WordToIntMapperClass();
		Map<String,Integer> wordToInt = wToIntMapper.getWordToInt();
		ArrayList<String> wlist    = wToIntMapper.getWordList();
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?s ?label FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s <http://www.w3.org/2000/01/rdf-schema#label> ?label .");
		queryString.append("FILTER NOT EXISTS { ");
		queryString.append("?s <http://www.w3.org/2000/01/rdf-schema#label> ?label .");
		queryString.append("FILTER ( REGEX(?s,\"%\",\"i\") || REGEX(?s,\"Category:\",\"i\") ) } ");
		queryString.append("}");
		com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		RDFNode wikiname = null;
		String title = "";
		int numOfTriples = 0;
		System.out.println("Iterating over the results...");
		try{
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			wikiname = result.get("label");
			title = wikiname.toString().toLowerCase().replace("@en", "");
			fillInVocabulary(wordToInt, wlist, title);
		} // end of while
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		wlist.trimToSize();
		System.out.println("Number of Triples: " + numOfTriples);
		System.out.println("wtoint size: " + wordToInt.size() + " wlist size: " + wlist.size());
		String fileName = DIR_PATH + "wordToInt.ser";
		//    String[] wToInt = new ArrayList<String>(wordToIntMapper.keySet()).toArray(new String[wordToIntMapper.size()]);
		try {
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(wToIntMapper);
			out.close();
			System.out.println("Word To Int Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} //catch (ClassNotFoundException e) {
		// e.printStackTrace();
		//}
	} // end of mapWordToInt


	public void persistWikipediaEntitiesIncomingLinks() {
		LocalNameMapperClass localNamesMapperClass = loadLocalNameListToMemory();
		Map<String, Integer> namesToInt = localNamesMapperClass.getNameToInt();
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		int uriPrefixLength = uriPrefix.length();
		EntityManagerImpl entityManager = new EntityManagerImpl();
		String sqlQuery = "";
		FileHandler fh = null;
		StringBuffer queryString = new StringBuffer();
		VirtuosoQueryExecution vqe = null;
		com.hp.hpl.jena.query.Query sparql = null;
		ResultSet results = null;
		try {
			fh = new FileHandler("entity_incominglink_tablelog.log");
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		fh.setFormatter(new SimpleFormatter());
		logger.addHandler(fh);
		logger.setLevel(Level.INFO);
		int rowCounter = 0;
		long st = System.currentTimeMillis();
		for (Map.Entry<String, Integer> entry : namesToInt.entrySet()) {
			queryString.setLength(0);
			String entityName = entry.getKey();
			int entityId = entry.getValue();
			queryString.append("PREFIX lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
			queryString.append("SELECT DISTINCT ?ent FROM <" + GRAPH + "> WHERE { ");
			queryString.append(" ?ent ?p <" + uriPrefix + entityName + "> . ");
			queryString.append("}");
			try {
				sparql = QueryFactory.create(queryString.toString());
				vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
				results = vqe.execSelect();
				QuerySolution result = null;
				String localname = "";
				while (results.hasNext()) {
					result = results.nextSolution();
					localname = result.getResource("ent").toString().substring(uriPrefixLength);
					int entId = namesToInt.get(localname) != null ? namesToInt.get(localname) : -1;
					if (entId != -1) {
						sqlQuery = "INSERT INTO entity_incominglink (incoming_entity_id,entity_id) VALUES (" + entId + "," + entityId + ")";
						entityManager.persist(sqlQuery);
					} // end of if
				} // end of while
				rowCounter++;
				if (rowCounter % interval == 0) {
					logger.log(Level.INFO, rowCounter + " of Entities processed.");
				} // end of if
			} catch(Exception e) {
				logger.log(Level.WARNING, entityName);
				logger.log(Level.WARNING, queryString.toString());
				logger.log(Level.WARNING, e.getMessage());
			}
		} // end of for
		double duration = Double.valueOf((System.currentTimeMillis() - st)) / 3600000;
		logger.log(Level.INFO, "Elapsed time: " + duration);
		logger.log(Level.INFO, "Total number of " + rowCounter + " Entities processed.");
		vqe.close();
	} // end of persistWikipediaEntitiesIncomingLinks


	public void persistWikipediaCategoryEntities() {
		CategoryNameMapper catNameMapper = loadCatNameListToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		LocalNameMapperClass localNamesMapperClass = loadLocalNameListToMemory();
		Map<String, Integer> namesToInt = localNamesMapperClass.getNameToInt();
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		VirtuosoQueryExecution vqe = null;
		int uriPrefixLength = uriPrefix.length();
		EntityManagerImpl entityManager = new EntityManagerImpl();
		String sqlQuery = "";
		FileHandler fh = null;
		String endpoint = "http://localhost:8890/sparql";
		QueryEngineHTTP qe = null;
		StringBuffer queryString = new StringBuffer();
		ResultSet results = null;
		try {
			fh = new FileHandler("category_entity_tablelog.log");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			int rowCounter = 0;
			long st = System.currentTimeMillis();
			for (Map.Entry<String, Integer> entry : catNamesToInt.entrySet()) {
				queryString.setLength(0);
				String categoryName = entry.getKey();
				int categoryId = entry.getValue();
				queryString.append("PREFIX lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
				queryString.append("SELECT (COUNT(DISTINCT ?s)) AS ?total FROM <" + GRAPH + "> WHERE { ");
				queryString.append(" ?s a <" + wikiCategoryUriPrefix + categoryName + "> . ");
				queryString.append("}");
				qe = new QueryEngineHTTP(endpoint, queryString.toString());

				//				com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
				com.hp.hpl.jena.query.Query sparql = null;
				//		System.out.println("QUERY: " + queryString.toString());
				//				vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
				//				ResultSet results = vqe.execSelect();
				try {
					results = qe.execSelect();
					// System.out.println("Retrieved the Results.");
					// System.out.println("Iterating over the Results...");
					int total = 0;
					while (results.hasNext()) {
						QuerySolution result = results.nextSolution();
						total = result.getLiteral("total").getInt();
					} // end of while
					if (total > maxNumOfAcceptableEntities) {
						continue;
					} // end of if
					qe.close();
					queryString.setLength(0);
					queryString.append("PREFIX lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
					queryString.append("SELECT DISTINCT ?entity FROM <" + GRAPH + "> WHERE { ");
					queryString.append(" ?entity a <" + wikiCategoryUriPrefix + categoryName + "> . ");
					queryString.append("}");
					sparql = QueryFactory.create(queryString.toString());
					vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
					results = vqe.execSelect();
					QuerySolution result = null;
					String localname = "";
					while (results.hasNext()) {
						result = results.nextSolution();
						localname = result.getResource("entity").toString().substring(uriPrefixLength);
						int entityId = namesToInt.get(localname);
						sqlQuery = "INSERT INTO category_entity (category_id,entity_id) VALUES (" + categoryId + "," + entityId + ")";
						entityManager.persist(sqlQuery);
					} // end of while
					rowCounter++;
					if (rowCounter % interval == 0) {
						logger.log(Level.INFO, rowCounter + " of categories processed.");
					} // end of if
				} catch(Exception e) {
					logger.log(Level.WARNING, categoryName);
					logger.log(Level.WARNING, queryString.toString());
					logger.log(Level.WARNING, e.getMessage());
				}
			} // end of for
			double duration = Double.valueOf((System.currentTimeMillis() - st)) / 3600000;
			logger.log(Level.INFO, "Elapsed time: " + duration);
			logger.log(Level.INFO, "Total number of " + rowCounter + " categories processed.");
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage());
		}
		vqe.close();
	} // end of persistWikipediaCategoryEntities


	public void createWikipediaTaxanomyMapFromParentToChild() {
		CategoryNameMapper catNameMapper = loadCatNameListToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<Integer, Set<Integer>> wikiTaxanomy = new HashMap<Integer, Set<Integer>>(2100000);
		StringBuffer queryString = new StringBuffer();
		queryString.append("PREFIX  skos: <http://www.w3.org/2004/02/skos/core#> ");
		queryString.append("SELECT ?s ?sc FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s a <http://www.w3.org/2004/02/skos/core#Concept> . ");
		queryString.append("?sc skos:broader ?s . ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		System.out.println("Iterating over the results...");
		QuerySolution result = null;
		int numOfTriples = 0;
		String superClass = "";
		String subClass = "";
		int superClassId = 0;
		int subClassId = 0;
		int uriPrefixLength = wikiCategoryUriPrefix.length();
		while (results.hasNext()) {
			result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			superClass = result.get("s").toString();
			if (superClass.length() > uriPrefixLength) {
				superClass = superClass.substring(uriPrefixLength);
				subClass = result.get("sc").toString();
				subClass = subClass.substring(uriPrefixLength);
				if (!isInternalCategory(superClass) && !isInternalCategory(subClass)) {
					superClassId = catNamesToInt.get(superClass);
					subClassId  = catNamesToInt.get(subClass);
					if (wikiTaxanomy.get(superClassId) != null) {
						wikiTaxanomy.get(superClassId).add(subClassId);
					}else {
						Set<Integer> catSet = new HashSet<Integer>();
						catSet.add(subClassId);
						wikiTaxanomy.put(superClassId, catSet); 
					} // end of if
				} // end of if
			} // end of if
		} // end of while
		vqe.close();
		System.out.println("number of wiki parents: " + wikiTaxanomy.size());  // # 393625                                                                                                                                                                                                                                          
		String fileName = DIR_PATH + "wikiTaxanomyMapFromParentToChild.ser";
		try {
			System.out.println("Serializing Wiki Taxanomy...");
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(wikiTaxanomy);
			out.close();
			System.out.println("Wiki Taxanomy Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createWikipediaTaxanomyMapFromParentToChild


	public Map<Integer,Set<Integer>> loadWikiTaxonomyMapFromParentToChildToMemory() {
		String fileName = DIR_PATH + "wikiTaxanomyMapFromParentToChild.ser";
		System.out.println("Loading Wiki Taxonomy Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<Integer,Set<Integer>> wikiTaxonomyMap = (HashMap<Integer,Set<Integer>>) in.readObject();
			in.close();
			System.out.println("Wiki Taxonomy Map from Parent to Child Loaded successfully into Memory.\n");
			return wikiTaxonomyMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadWikiTaxonomyMapFromParentToChildToMemory


	public void createWikipediaTaxanomyMap_ID_FromChildToParent() {
		CategoryNameMapper catNameMapper = loadCatNameListToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<Integer, Set<Integer>> wikiTaxanomy = new HashMap<Integer, Set<Integer>>(2100000);
		StringBuffer queryString = new StringBuffer();
		queryString.append("PREFIX lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?s ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s lsdis:wiki_category_parent ?o . ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		System.out.println("Iterating over the results...");
		QuerySolution result = null;
		int numOfTriples = 0;
		String subClass = "";
		String superClass = "";
		int subClassId = 0;
		int superClassId = 0;
		int uriPrefixLength = wikiCategoryUriPrefix.length();
		while (results.hasNext()) {
			result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			subClass = result.get("s").toString();
			if (subClass.length() > uriPrefixLength) {
				subClass = subClass.substring(uriPrefixLength);
				superClass = result.get("o").toString();
				superClass = superClass.substring(uriPrefixLength);
				if (!isInternalCategory(subClass) && !isInternalCategory(superClass)) {
					subClassId  = catNamesToInt.get(subClass);
					superClassId = catNamesToInt.get(superClass);
					if (wikiTaxanomy.get(subClassId) != null) {
						wikiTaxanomy.get(subClassId).add(superClassId);
					}else {
						Set<Integer> catSet = new HashSet<Integer>();
						catSet.add(superClassId);
						wikiTaxanomy.put(subClassId, catSet); 
					} // end of if
				} // end of if
			} // end of if
		} // end of while
		vqe.close();
		System.out.println("Wiki Taxanomy Size: " + wikiTaxanomy.size()); // # 786825
		String fileName = DIR_PATH + "wikiTaxanomyMap_ID_FromChildToParent.ser";
		try {
			System.out.println("Serializing Wiki Taxanomy...");
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(wikiTaxanomy);
			out.close();
			System.out.println("Wiki Taxanomy Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createWikipediaTaxanomyMap_ID_FromChildToParent


	public Map<Integer,Set<Integer>> loadWikiTaxonomyMap_ID_FromChildToParentToMemory() {
		String fileName = DIR_PATH + "wikiTaxanomyMap_ID_FromChildToParent.ser";
		System.out.println("Loading Wiki Taxonomy Map from Child to Parent into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<Integer,Set<Integer>> wikiTaxonomyMap = (HashMap<Integer,Set<Integer>>) in.readObject();
			in.close();
			System.out.println("Wiki Taxonomy Map Loaded successfully into Memory.\n");
			return wikiTaxonomyMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadWikiTaxonomyMap_ID_FromChildToParentToMemory





	public void createWikipediaTaxanomyMapFromChildToParent() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<String, Set<String>> wikiTaxanomy = new HashMap<String, Set<String>>(2100000);
		StringBuffer queryString = new StringBuffer();
		queryString.append("PREFIX lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
		queryString.append("SELECT DISTINCT ?s ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s lsdis:wiki_category_parent ?o . ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		System.out.println("Iterating over the results...");
		QuerySolution result = null;
		int numOfTriples = 0;
		String subClass = "";
		String superClass = "";
		int uriPrefixLength = wikiCategoryUriPrefix.length();
		while (results.hasNext()) {
			result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			subClass = result.get("s").toString();
			if (subClass.length() > uriPrefixLength) {
				subClass = subClass.substring(uriPrefixLength);
				superClass = result.get("o").toString();
				superClass = superClass.substring(uriPrefixLength);
				if (wikiTaxanomy.get(subClass) != null) {
					wikiTaxanomy.get(subClass).add(superClass);
				}else {
					Set<String> catSet = new HashSet<String>();
					catSet.add(superClass);
					wikiTaxanomy.put(subClass, catSet); 
				} // end of if
			} // end of if
		} // end of while
		vqe.close();
		System.out.println("Wiki Taxanomy Size: " + wikiTaxanomy.size());
		String fileName = DIR_PATH + "wikiTaxanomyMapFromChildToParent.ser";
		try {
			System.out.println("Serializing Wiki Taxanomy...");
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(wikiTaxanomy);
			out.close();
			System.out.println("Wiki Taxanomy Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createWikipediaTaxanomyMapFromChildToParent

	public Map<String,Set<String>> loadWikiTaxonomyMapFromChildToParentToMemory() {
		String fileName = DIR_PATH + "wikiTaxanomyMapFromChildToParent.ser";
		System.out.println("Loading Wiki Taxonomy Map from Child to Parent into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<String,Set<String>> wikiTaxonomyMap = (HashMap<String,Set<String>>) in.readObject();
			in.close();
			System.out.println("Wiki Taxonomy Map Loaded successfully into Memory.\n");
			return wikiTaxonomyMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadWikiTaxonomyMapFromChildToParentToMemory


	public void mapCategoryNamesToInt() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		CategoryNameMapper catNameMapper = new CategoryNameMapper();
		Map<String, Integer> catNameToInt = catNameMapper.getNameToInt();
		ArrayList<String> catNameList = catNameMapper.getCatNameList();
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?cat FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?cat a <http://www.w3.org/2004/02/skos/core#Concept> . ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		System.out.println("Iterating over the results...");
		QuerySolution result = null;
		int numOfTriples = 0;
		RDFNode uri = null;
		String catURI = "";
		String catlocalName = "";
		int uriPrefixLength = wikiCategoryUriPrefix.length();
		int id = 0;
		while (results.hasNext()) {
			result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			uri = result.get("cat");
			catURI = uri.toString();
			catlocalName = catURI.substring(uriPrefixLength);
			boolean internalCatFlag = isInternalCategory(catlocalName);
			if (!internalCatFlag) {
				if (!catNameToInt.containsKey(catlocalName)) {
					catNameToInt.put(catlocalName, id); 
					catNameList.add(catlocalName);
					id++;
				} // end of if
			} // end of if
		} // end of while
		catNameList.trimToSize(); // total number of categories: 997300
		System.out.println("Number of Categories: " + catNameList.size());
		System.out.println("Serializing Category Names...");
		String fileName = DIR_PATH + "wikiCategoryNames.ser";
		try {
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(catNameMapper);
			out.close();
			System.out.println("Category Names Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of mapCategoryNamesToInt
	public boolean isInternalCategory(String category) {
		boolean internalCatFlag = false;
		for (String internalCat : wikipedia_internal_categories) {
			if (category.toLowerCase().contains(internalCat)) {
				internalCatFlag = true;
				break;
			} // end of if
		} // end of for
		return internalCatFlag;
	} // end of isInternalCategory


	public CategoryNameMapper loadCatNameListToMemory() {
		String fileName = DIR_PATH + "wikiCategoryNames.ser";
		System.out.println("Loading Category Names into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			CategoryNameMapper catNameMapper = (CategoryNameMapper) in.readObject();
			in.close();
			System.out.println("Category Names Successfuly Loaded into Memory.\n");
			return catNameMapper;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadCatNameListToMemory



	public Map<String,String> loadYagoTaxonomyMapToMemory() {
		String fileName = DIR_PATH + "yagoTaxanomyMap.ser";
		System.out.println("Loading Yago Taxonomy Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<String,String> yagoTaxonomyMap = (HashMap<String,String>) in.readObject();
			in.close();
			System.out.println("Yago Taxonomy Map Loaded successfully into Memory.\n");
			return yagoTaxonomyMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadYagoTaxonomyMapToMemory


	public void createYagoTaxanomyMap() {
		System.out.println("Connecting to Virtuoso ... ");
		String yagoGraph = "http://yago.com#";
		VirtGraph yagoVirtGraph = connectToVirtuoso(yagoGraph);
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<String, String> yagoTaxanomy = new HashMap<String, String>(451000);
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT DISTINCT ?s ?o FROM <" + yagoGraph + "> WHERE { ");
		queryString.append("?s ?p ?o . ");
		queryString.append("FILTER regex (?o,\"http://yago-knowledge.org\", \"i\") ");
		//    queryString.append("FILTER NOT EXISTS { ?s ?p <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> . } ");
		//    queryString.append("FILTER NOT EXISTS { ?s ?p <http://www.w3.org/2002/07/owl#Thing> . } ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, yagoVirtGraph);
		ResultSet results = vqe.execSelect();
		System.out.println("Retrieved the Results.");
		System.out.println("Iterating over the results...");
		QuerySolution result = null;
		int numOfTriples = 0;
		String subClass = "";
		String superClass = "";
		int uriPrefixLength = yagoUriPrefix.length();
		while (results.hasNext()) {
			result = results.nextSolution();
			numOfTriples = results.getRowNumber();
			if (numOfTriples % interval == 0) {
				System.out.println(numOfTriples);
			}
			subClass = result.get("s").toString();
			if (subClass.length() > uriPrefixLength) {
				subClass = subClass.substring(uriPrefixLength);
				superClass = result.get("o").toString();
				superClass = superClass.substring(uriPrefixLength);
				yagoTaxanomy.put(subClass, superClass); 
			} // end of if
		} // end of while
		queryString.setLength(0);
		queryString.append("SELECT DISTINCT ?s FROM <" + yagoGraph + "> WHERE { ");
		queryString.append("?s ?p <http://www.w3.org/2002/07/owl#Thing> . ");
		queryString.append("}");
		sparql = QueryFactory.create(queryString.toString());
		vqe = VirtuosoQueryExecutionFactory.create(sparql, yagoVirtGraph);
		results = vqe.execSelect();
		superClass = "Thing";
		while (results.hasNext()) {
			result = results.nextSolution();
			subClass = result.get("s").toString();
			subClass = subClass.substring(uriPrefixLength);
			yagoTaxanomy.put(subClass, superClass); 
		} // end of while
		vqe.close();
		System.out.println("YAGO Taxanomy Size: " + yagoTaxanomy.size());
		String fileName = DIR_PATH + "yagoTaxanomyMap.ser";
		try {
			System.out.println("Serializing Yago Taxanomy...");
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(yagoTaxanomy);
			out.close();
			System.out.println("Yago Taxanomy Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createYagoTaxanomyMap



	public void fillInVocabulary(Map<String,Integer> vocabulary, ArrayList<String> wlist, String name) {
		name = name.replace("'s", "").replace("'S", "").replace("'t", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "").replace(".", "");
		if (isValid(name)) {
			String[] tokens = name.split(" ");
			for (String token : tokens) {
				if (!vocabulary.containsKey(token)) {
					vocabulary.put(token, idGenerator);
					wlist.add(token);
					idGenerator++;
				} // end of if
			} // end of for
		} // end of if
	} // end of fillInVocabulary

	public boolean isValid(String name) {
		int index = name.indexOf(" (");
		if (index != -1) {
			name = name.substring(0, index);
		}
		if (name.equals(" ")) {
			return false;
		}
		if (name.length() < 3) {
			return false;
		}
		if (name.startsWith("'")) {
			return false;
		}
		if (name.contains("?")) {
			return false;
		}
		if (name.startsWith("!")) {
			return false;
		}
		if (name.startsWith("&")) {
			return false;
		}
		if (name.contains("%")) {
			return false;
		}
		if (name.contains("+")) {
			return false;
		}
		if (name.contains("/")) {
			return false;
		}
		try {
			Number n = NumberFormat.getInstance().parse(name);
			if ( n != null) {
				if (n.toString().equals(name)) {
					return false;
				}else {
					return true;
				}
			}
		} catch (ParseException e) {
			return true;
		}
		return true;
	} // end of isValid

	public String readStopWords() {
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
		return content;
	} // end of readStopWords
	
	public Set<String> readStopWordsSet() {
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
	} // end of readStopWordsSet
	
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
	
	public Map<String, Integer> getNamesToInt() {
		return namesToInt;
	}


	public void setNamesToInt(Map<String, Integer> namesToInt) {
		this.namesToInt = namesToInt;
	}


	public boolean isRelationBetween(String ent1, String ent2, VirtGraph virtGraph) {
		StringBuffer queryString = new StringBuffer();
		boolean related = false;
		try {
			queryString.append("SELECT ?p FROM <" + GRAPH + "> WHERE { ");
			queryString.append("{ <" + uriPrefix + ent1 + "> ?p <" + uriPrefix + ent2 + "> . } ");
			queryString.append("UNION { <" + uriPrefix + ent2 + "> ?p <" + uriPrefix + ent1 + "> . } ");
			queryString.append("}");
			//		System.out.println("QUERY: " + queryString.toString());
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			while (results.hasNext()) {
				related = true;
				break;
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("QUERY: " + queryString.toString());
		}
		return related;
	} // end of isRelationBetween


	public void createEntityBidirectionalRelatedConceptsFromFile(String input_dir, String output_dir) {
		System.out.println("\nConnecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		
		// We would like to filter out some irrelevant entities //
//		String [ ] domainEntities = readDocument("/home/mehdi/domainEntities/allConcepts.txt").split(",");
//		Set<String> allDomainEntities = new HashSet<String>();
//		for (String ent : domainEntities) {
//			allDomainEntities.add(ent.trim());
//		} // end of for
		
		try {
//			String input_dir =  "/home/mehdi/taxonomyProject/finaldocEntities/";
//			String output_dir = "/home/mehdi/taxonomyProject/relatedconcepts/";
			File rssDirectory = new File(input_dir);
			Set<String> allCorpusEntities = new HashSet<String>();
			for (File file : rssDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(input_dir + fileName);
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
				
				// If interested in filtering out some of the corpus entities add following condition
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
					if (allCorpusEntities.contains(e)) {
//					if (allCorpusEntities.contains(e) && allDomainEntities.contains(e)) {
						if (e.indexOf('?') != -1 || e.indexOf('.') != -1  || e.indexOf('/') != -1 || e.indexOf('%') != -1 || e.indexOf('\'') != -1 || e.indexOf(":") != -1 || e.indexOf('\"') != -1 || e.indexOf('-') != -1 || e.indexOf('^') != -1 || e.startsWith("_") || e.startsWith(".")) {
							continue;
						}else {
							refinedRelatedEntities.add(e);
						} // end of if
					} // end of if
				} // end of for
//				String line = relatedEntities.toString().replace("[", "").replace("]", "");
				if (refinedRelatedEntities.size() > 0) {
					FileWriter fw = new FileWriter(output_dir + entity.replace('/', '_') + ".txt");
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
	
	private Set<String> getEntityNonLiteralRelationList(VirtGraph virtGraph, String entName) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("SELECT DISTINCT ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + uriPrefix + entName + ">" + " <http://dbpedia.org/ontology/wikiPageWikiLink> ?o . ");
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
				}else if (localname.indexOf("File:") != -1) {
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
	
	private Set<String> getRelationsReferingToEntity(VirtGraph virtGraph, String entName) {
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
		queryString.append(" ?s <http://dbpedia.org/ontology/wikiPageWikiLink> <" + uriPrefix + entName + "> . ");
		queryString.append(" FILTER NOT EXISTS {");
		queryString.append(" ?s <http://dbpedia.org/ontology/wikiPageRedirects> <" + uriPrefix + entName + "> . }");
		queryString.append(" }");
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

	public void getSampleArticles(String input_dir, String output_dir, int sampleSize) {
		File inFolder = new File(input_dir);
		File [] fileList = inFolder.listFiles();
		Random r = new Random();
		Set<Integer> duplicates =  new HashSet<Integer>();
		int counter = 1;
		while (counter <= sampleSize) {
			int fileNo = r.nextInt(fileList.length);
			if (!duplicates.contains(fileNo)) {
				duplicates.add(fileNo);
				String fileName = fileList[fileNo].getName();
				String document = readDocument(input_dir + fileName);
				try {
					FileWriter fout = new FileWriter(output_dir + fileName);
					fout.write(document);
					fout.flush();
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				counter++;
			} // end of if
		} // end of while
	} // end of getSampleArticles

	public void getSampleTestArticles(String input_dir, String output_dir, String training_dir, int sampleSize) {
		File trFolder = new File(training_dir);
		File [] trFileList = trFolder.listFiles();
		Set<String> trFilenames = new HashSet<String>();
		for (File f : trFileList) {
			trFilenames.add(f.getName());
		}
		File inFolder = new File(input_dir);
		File [] fileList = inFolder.listFiles();
		Random r = new Random();
		Set<Integer> duplicates =  new HashSet<Integer>();
		int counter = 1;
		while (counter <= sampleSize) {
			int fileNo = r.nextInt(fileList.length);
			if (!duplicates.contains(fileNo)) {
				duplicates.add(fileNo);
				String fileName = fileList[fileNo].getName();
				if (trFilenames.contains(fileName)) {
					System.out.println("invalid");
					continue;
				}
				String document = readDocument(input_dir + fileName);
				try {
					FileWriter fout = new FileWriter(output_dir + fileName);
					fout.write(document);
					fout.flush();
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				counter++;
			} // end of if
		} // end of while
	} // end of getSampleTestArticles
	
	public void measurePrecision(String prFile, int prK, boolean isRelaxed) {
		WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
		Map<Integer,Set<Integer>> wikiHierarchy = wikiaccess.loadWikipediaHierarchy();
		CategoryNameMapper wikipediaCategories = wikiaccess.loadWikipediaCategoryNames();
		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		int level = 2;
		
		List<String> categoryLookupFile = getDocumentContent("/home/mehdi/topicdiscovery/preprocessedFiles/categoryLookup.txt");
		List<String> content = getDocumentContent("/home/mehdi/topicdiscovery/posteriorFiles-wikipediaDataset/" + prFile);
		Set<String> corpusCategories = new HashSet<String>();
		int nCoverage = 0;
		for (String line : categoryLookupFile) {
			corpusCategories.add(line.split("\t")[1]);
		}
		int nDocs = content.size();
		double sumPrecision = 0;
		int docCtr = 1;
		int i = 1;
		for (String doc : content) {
			String [] items = doc.split(",");
			String articleTitle = items[0];
			Set<String> cats = findCategoriesByConceptName(articleTitle); // initial categories assigned to article
			Set<String> finalCats =  new HashSet<String>(); // final categories assigned to article after filtering non-existing ones
			for (String c : cats) {
				if (corpusCategories.contains(c)) {
					finalCats.add(c);
				}
			} // end of if
			double precision = 0;
			if (isRelaxed) {
				precision = computeRelaxedPrecisionAtK(items, finalCats, prK, wikiaccess, wikiHierarchy, catNamesToInt, wikipediaCategoryNames, level);
			}else {
				precision = computePrecisionAtK(items, finalCats, prK);
			}
			if (precision > 0) {
//				System.out.println(i++);
				nCoverage++;
			}
			if (precision != -1) {
				sumPrecision += precision;
			}else {
				nDocs--;
			}
//			System.out.println(docCtr++ + " docs done.");
		} // end of for
		double finalPrecisonAtK = sumPrecision / nDocs;
		double coverageAtK = (double) nCoverage / nDocs;
		System.out.println("Precision@" + prK + ": " + Math.round(finalPrecisonAtK * 1000) / 1000.);
		System.out.println("Coverage@" + prK + ": " + Math.round(coverageAtK * 1000) / 1000.);
	} // end of measurePrecision

	private double computeRelaxedPrecisionAtK(String[] items,	Set<String> cats, int prK, WikipediaAccessLayer wikiaccess, Map<Integer, Set<Integer>> wikiHierarchy, Map<String, Integer> catNamesToInt, List<String> wikipediaCategoryNames, int level) {
		int docRelCtr = 0;  // counts the number of relevant topics
		if (cats.size() > 0) {
			for (int i = 1; i <= prK; i++) {
				String predictedCategory = items[i].split(":")[0];
				if (catNamesToInt.get(predictedCategory) == null) {
					continue;
				}
				int catId = catNamesToInt.get(predictedCategory);
				Set<Integer> mainCategories = new HashSet<Integer>();
				Set<Integer> categories = new HashSet<Integer>();
				Set<String> categoryNames = new HashSet<String>();
				categories.add(catId);
				categories.addAll(wikiaccess.findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level));
				for (int cid : categories) {
					categoryNames.add(wikipediaCategoryNames.get(cid));
				}
				for (String cname : cats) {
					if (categoryNames.contains(cname)) {
						docRelCtr++;
						break;
					} // end of if
				}
			} // end of for
			docRelCtr = Math.min(docRelCtr, cats.size());
			return (double) docRelCtr / Math.min(prK,cats.size());
//			return (double) docRelCtr / prK;
		}else {
			return -1.0;
		}
	} // end of computeRelaxedPrecisionAtK

	public double computePrecisionAtK(String[] items, Set<String> cats, int prK) {
//		String [] items = doc.split(",");
//		String articleTitle = items[0];
//		Set<String> cats = findCategoriesByConceptName(articleTitle);
		int docRelCtr = 0;  // counts the number of relevant topics
		if (cats.size() > 0) {
			for (int i = 1; i <= prK; i++) {
				String predictedCategory = items[i].split(":")[0];
				if (cats.contains(predictedCategory)) {
					docRelCtr++;
				} // end of if
			} // end of for
//			docRelCtr = Math.min(docRelCtr, cats.size());
			return (double) docRelCtr / Math.min(prK,cats.size()) ;
		}else {
			return -1.0;
		}
	} // end of computePrecisionAtK
	
	public Set<String> findCategoriesByConceptName(String concept) {
//		System.out.println("con: " + wikiConcepts);
		VirtGraph virtGraph = connectToVirtuoso();
		int categoryUriPrefixLength = wikiCategoryUriPrefix.length();
		Set<String> categoryList = new HashSet<String>();
		StringBuilder queryString = new StringBuilder(400);
		if (concept.indexOf('"') == -1) {
			queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + concept + ">" + " <http://purl.org/dc/terms/subject> ?o . ");
			queryString.append(" }");
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			//				 System.out.println("Retrieved the Results.");
			//				 System.out.println("Iterating over the Results...");
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String category = result.getResource("o").toString();
				int index = category.indexOf("/Category:");
				if (index != -1) {
					category = category.substring(categoryUriPrefixLength);
					categoryList.add(category);
				} // end of if
			} // end of while
			vqe.close();
		} // end of if (concept.indexOf('"')
		virtGraph.close();
		return categoryList;
	} // end of findCategoriesByConceptName
	
	public Set<String> findSuperCategoriesByCatName(String cat) {
//		System.out.println("con: " + wikiConcepts);
		VirtGraph virtGraph = connectToVirtuoso();
		int categoryUriPrefixLength = wikiCategoryUriPrefix.length();
		Set<String> categoryList = new HashSet<String>();
		StringBuilder queryString = new StringBuilder(400);
		if (cat.indexOf('"') == -1) {
			queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + wikiCategoryUriPrefix + cat + ">" + " <http://www.w3.org/2004/02/skos/core#broader> ?o . ");
			queryString.append(" }");
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			//				 System.out.println("Retrieved the Results.");
			//				 System.out.println("Iterating over the Results...");
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String category = result.getResource("o").toString();
				int index = category.indexOf("/Category:");
				if (index != -1) {
					category = category.substring(categoryUriPrefixLength);
					categoryList.add(category);
				} // end of if
			} // end of while
			vqe.close();
		} // end of if (concept.indexOf('"')
		virtGraph.close();
		return categoryList;
	} // end of findSuperCategoriesByCatName

	public void measureMeanAveragePrecision(String prFile, int prK, boolean isRelaxed) {
		WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
		Map<Integer,Set<Integer>> wikiHierarchy = wikiaccess.loadWikipediaHierarchy();
		CategoryNameMapper wikipediaCategories = wikiaccess.loadWikipediaCategoryNames();
		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		int level = 2;
		
		List<String> categoryLookupFile = getDocumentContent("/home/mehdi/topicdiscovery/preprocessedFiles/categoryLookup.txt");
		List<String> content = getDocumentContent("/home/mehdi/topicdiscovery/posteriorFiles-wikipediaDataset/" + prFile);
		Set<String> corpusCategories = new HashSet<String>();
		for (String line : categoryLookupFile) {
			corpusCategories.add(line.split("\t")[1]);
		}
		int nDocs = content.size();
		int docCtr = 1;
		double map = 0;
		for (String doc : content) {
			String [] items = doc.split(",");
			String articleTitle = items[0];
			Set<String> cats = findCategoriesByConceptName(articleTitle);
			int relTopics = 0; // initial relevant categories, some of them may not be available in the corpus categories and must NOT be counted!
			Set<String> finalCats =  new HashSet<String>(); // final categories assigned to article after filtering non-existing ones
			if (cats.size() > 0) {
				for (String c : cats) {
					if (corpusCategories.contains(c)) {
						relTopics++;
						finalCats.add(c);
					}
				} // end of for
				if (relTopics > 0) {
					double prAtK = 0.;
//					for (int j = 1; j <= relTopics; j++) {
					if (isRelaxed) {
						for (int j = 1; j <= prK; j++) {
							prAtK += computeRelaxedPrecisionAtK(items, finalCats, j, wikiaccess, wikiHierarchy, catNamesToInt, wikipediaCategoryNames, level);
						}
					}else {
						for (int j = 1; j <= prK; j++) {
							prAtK += computePrecisionAtK(items, finalCats, j);
						}
					}
					map += prAtK / prK;
//					map += prAtK / relTopics;
				}else {
					nDocs--;
				}
			}else {
				nDocs--;
			}
		} // end of for
		double finalMap = map / nDocs;
		System.out.println("Mean Average Precision: " + Math.round(finalMap * 1000) / 1000.);
		
	}

	public void measurePrecisionForReutersDataset(String prFile, int prK) {
		String [] catNames = {"Business","Applied_sciences","Health"};
		Map<String,Set<String>> docToCatMap    = getDocumentsPredefinedCategories();
		Map<String,Set<String>> catToSubcatMap = getWikipediaSubCategories(catNames);
		List<String> content = getDocumentContent("/home/mehdi/topicdiscovery/posteriorFiles/" + prFile);
		
		int nDocs = content.size();
		double sumPrecision = 0;
		int nCoverage = 0;
		int docCtr = 1;
		int i = 1;
		for (String doc : content) {
			String [] items = doc.split(",");
			String articleTitle = items[0];
			Set<String> predefinedCats = docToCatMap.get(articleTitle);
			if (predefinedCats == null) {
				nDocs--;
				continue;
			}
			Set<String> finalCats = new HashSet<String>();
//			System.out.println(articleTitle);
			for (String cat : predefinedCats) {
				finalCats.addAll(catToSubcatMap.get(cat));
			}
			double precision = computePrecisionAtKForReutersDataset(items, finalCats, prK, predefinedCats.size());
			if (precision > 0) {
//				System.out.println(i++);
				nCoverage++;
			}
			if (precision != -1) {
				sumPrecision += precision;
			}else {
				nDocs--;
			}
//			System.out.println(docCtr++ + " docs done.");
		} // end of for
		double finalPrecisonAtK = sumPrecision / nDocs;
		double coverageAtK = (double) nCoverage / nDocs;
		System.out.println("Precision@" + prK + ": " + Math.round(finalPrecisonAtK * 1000) / 1000.);
		System.out.println("Coverage@" + prK + ": " + Math.round(coverageAtK * 1000) / 1000.);
	} // end of measurePrecisionForReutersDataset

	public double computePrecisionAtKForReutersDataset(String[] items, Set<String> cats, int prK, int nCats) {
		int docRelCtr = 0;  // counts the number of relevant topics
		if (cats.size() > 0) {
			for (int i = 1; i <= prK; i++) {
				String predictedCategory = items[i].split(":")[0];
				if (cats.contains(predictedCategory)) {
					docRelCtr++;
				} // end of if
			} // end of for
			docRelCtr = Math.min(docRelCtr, nCats);
			return (double) docRelCtr / Math.min(prK,nCats) ;
		}else {
			return -1.0;
		}
	} // end of computePrecisionAtKForReutersDataset

	public Map<String, Set<String>> getWikipediaSubCategories(String[] catNames) {
		Map<String,Set<String>> catToSubcatMap = new HashMap<String,Set<String>>();
		WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
		Map<Integer,Set<Integer>> wikiHierarchy = wikiaccess.loadWikipediaHierarchy();
		CategoryNameMapper wikipediaCategories = wikiaccess.loadWikipediaCategoryNames();
		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		int level = 2;
		for (String cn : catNames) {
			int catId = catNamesToInt.get(cn);
			Set<Integer> mainCategories = new HashSet<Integer>();
			Set<Integer> categories = new HashSet<Integer>();
			Set<String> cnames = new HashSet<String>();
			categories.add(catId);
			categories.addAll(wikiaccess.findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level));
			for (int cid : categories) {
				cnames.add(wikipediaCategoryNames.get(cid));
			}
			if (cn.equals("Applied_sciences")) {
				catToSubcatMap.put("Science", cnames);
			}else {
				catToSubcatMap.put(cn, cnames);
			}
		} // end of for
//		System.out.println(catToSubcatMap.get("Science").size());
		return catToSubcatMap;
	} // end of getWikipediaSubCategories

	public Map<String, Set<String>> getDocumentsPredefinedCategories() {
		Map<String, Set<String>> docNameToCatMap = new HashMap<String, Set<String>>();
		File trFolder = new File("/home/mehdi/topicdiscovery/reutersDocs/");
		File [] trFileList = trFolder.listFiles();
		Set<String> trFilenames = new HashSet<String>();
		for (File f : trFileList) {
			trFilenames.add(f.getName());
		}
		// find the predefined categories assigned to documents by Reuters website
		// we should check the categories for each document
		String [] catLabels       = {"Business", "Health", "Science", "Science"};
		String [] catlabelFolders = {"/home/mehdi/rss/reuters-copy/businessNews/", "/home/mehdi/rss/reuters-copy/healthNews/", "/home/mehdi/rss/reuters-copy/science/", "/home/mehdi/rss/reuters-copy/technology/" };
		for (int i = 0;i < catlabelFolders.length;i++) {
			String folder = catlabelFolders[i];
			File dir = new File(folder);
			File [] flist = dir.listFiles();
			Set<String> fnames = new HashSet<String>();
			for (File f : flist) {
				fnames.add(f.getName());
			}
			for (String nme : trFilenames) {
				if (fnames.contains(nme)) {
					nme = nme.substring(0, nme.indexOf(".txt"));
					Set<String> cats = docNameToCatMap.get(nme);
					if (cats == null) {
						cats = new HashSet<String>();
						cats.add(catLabels[i].toLowerCase());
					}else {
						cats.add(catLabels[i].toLowerCase());
					}
					docNameToCatMap.put(nme, cats);
//					System.out.println(docNameToCatMap.get(nme).toString());
				} // end of if
			} // end of for
		} // end of for i
		return docNameToCatMap;
	} // end of getDocumentsPredefinedCategories

	public void measureMeanAveragePrecisionForReutersDataset(String prFile,	int prK) {
		String [] catNames = {"Business","Applied_sciences","Health"};
		Map<String,Set<String>> docToCatMap    = getDocumentsPredefinedCategories();
		Map<String,Set<String>> catToSubcatMap = getWikipediaSubCategories(catNames);
		List<String> content = getDocumentContent("/home/mehdi/topicdiscovery/posteriorFiles/" + prFile);
		int nDocs = content.size();
		int docCtr = 1;
		double map = 0;
		for (String doc : content) {
			String [] items = doc.split(",");
			String articleTitle = items[0];
			
			Set<String> predefinedCats = docToCatMap.get(articleTitle);
			if (predefinedCats == null) {
				nDocs--;
				continue;
			}
			Set<String> finalCats = new HashSet<String>();
//			System.out.println(articleTitle);
			for (String cat : predefinedCats) {
				finalCats.addAll(catToSubcatMap.get(cat));
			}
			double prAtK = 0.;
			//					for (int j = 1; j <= relTopics; j++) {
			for (int j = 1; j <= prK; j++) {
				prAtK += computePrecisionAtKForReutersDataset(items, finalCats, j, predefinedCats.size());
			}
			map += prAtK / prK;
			//					map += prAtK / relTopics;
			//					System.out.println(docCtr++ + " docs done.");
		} // end of for
		double finalMap = map / nDocs;
		System.out.println("Mean Average Precision: " + Math.round(finalMap * 1000) / 1000.);
	} // end of measureMeanAveragePrecisionForReutersDataset
	
	public void computeTfIdf(String filePath, String filename) {
		try {
			List<String> docs = getDocumentContent(filePath + filename);
			Map<String,Map<String,Integer>> docTermMap = new HashMap<String,Map<String,Integer>>();
			Map<String,Integer> wordIdfMap = new HashMap<String,Integer>();
			Set<String> vocab = new HashSet<String>();
			Set<String> stopWords = readStopWordsSet();
			for (String doc : docs) {
				Map<String,Integer> wordTfMap = new HashMap<String,Integer>();
				String [] tokens = doc.split(",");
				String docId = tokens[0]; 
				for (int i = 1; i < tokens.length; i++) {
					String token = tokens[i];
					String [] labelTerms = token.split(":")[0].split("_");
					for (String term : labelTerms) {
						term = term.replaceAll("[^a-zA-Z]", "").toLowerCase();
						if (!stopWords.contains(term) && term.length() > 3) {
							vocab.add(term);
							if (wordTfMap.get(term) != null) {
								int tf = wordTfMap.get(term) + 1;
								wordTfMap.put(term, tf);
							}else {
								wordTfMap.put(term, 1);
							}
						} // end of if
					} // end of for (term)
				} // end of for (token)
				docTermMap.put(docId, wordTfMap);
				Set<String> keys = wordTfMap.keySet();
				for(String key : keys) {
					if (wordIdfMap.get(key) != null) {
						int prevFreq = wordIdfMap.get(key);
						wordIdfMap.put(key, prevFreq + 1);
					}else {
						wordIdfMap.put(key, 1);
					}
				} // end of for (key)
			} // end of for (doc)
			List<String> vocabList = new ArrayList<String>(vocab);
//			System.out.println(docTermMap.size() + " " + vocab.size());
			FileWriter vocabFile = new FileWriter("/home/mehdi/topicdiscovery/clustering/vocab.txt");
			vocabFile.write(vocabList.toString().replace("[", "").replace("]", ""));
			vocabFile.flush();
			vocabFile.close();
			int numOfDocs = docTermMap.size();
			List<String> docIds = new ArrayList<String>(docTermMap.keySet());
			// creating a file compatible with Weka
			FileWriter corpusFile = new FileWriter("/home/mehdi/topicdiscovery/clustering/corpus.arff");
			corpusFile.write("@relation doc-cluster\n");
			corpusFile.write("@attribute docId string\n");
			for (int wid = 0; wid < vocabList.size(); wid++) {
				corpusFile.write("@attribute tfidf" + (wid + 1) + " numeric\n");
			} // end of for (word)
			corpusFile.write("\n\n");
			corpusFile.write("@data\n");
			System.out.println("Computing tf-idf...");
			for (String docId : docIds) {
				Map<String,Integer> docTerms = docTermMap.get(docId);
				double tfIdfSum = 0;
				double [] docTfIdfVec = new double[vocabList.size()];
				String line = docId + ",";
				for (int wid = 0; wid < vocabList.size(); wid++) {
					String word = vocabList.get(wid);
					int tf = docTerms.get(word) != null ? docTerms.get(word) : 0;
					double idf = Math.log(numOfDocs / wordIdfMap.get(word));
					double tfIdf = tf * idf;
					tfIdfSum += tfIdf;
					docTfIdfVec[wid] = tfIdf;
				} // end of for (word)
				// normalize the tfidf values
				for (int wid = 0; wid < vocabList.size(); wid++) {
					double normalziedTfIdf = Math.round((docTfIdfVec[wid] / tfIdfSum) * 100000) / 100000.;
					line += normalziedTfIdf + ",";
				}
				line = line.substring(0, line.length() - 1) + "\n";
				corpusFile.write(line);
			}
			corpusFile.flush();
			corpusFile.close();
			System.out.println("done!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of computeTfIdf
	
	public void filterDocumentsByCategory(String cat) {
		Map<String, Set<String>> docNameToCatMap = getDocumentsPredefinedCategories();
//		File folder = new File("/home/mehdi/topicdiscovery/classification/business/");
//		File folder = new File("/home/mehdi/topicdiscovery/classification/health/");
		File folder = new File("/home/mehdi/topicdiscovery/classification/science/");
		File [] fileList = folder.listFiles();
		for (File f : fileList) {
			String fname = f.getName().substring(0, f.getName().indexOf(".txt"));
			System.out.println(fname);
			if (!docNameToCatMap.get(fname).contains(cat)) {
				File source = new File(folder.getPath() + "/" + f.getName());
//				File dest   = new File("/home/mehdi/topicdiscovery/classification/non-business/" + f.getName());
//				File dest   = new File("/home/mehdi/topicdiscovery/classification/non-health/" + f.getName());
				File dest   = new File("/home/mehdi/topicdiscovery/classification/non-science/" + f.getName());
				try {
					FileUtils.moveFile(source, dest);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	} // end of filterDocumentsbyCategory
	
	/*
	 * This method creates documents represented by top-k tags
	 */
	public void createTaggedDocs(String filePath, String filename) {
		try {
			List<String> docs = getDocumentContent(filePath + filename);
			Set<String> stopWords = readStopWordsSet();
			for (String doc : docs) {
				String [] tokens = doc.split(",");
				String docId = tokens[0]; 
				FileWriter docfile = new FileWriter("/home/mehdi/topicdiscovery/classification/tagdocs/" + docId + ".txt");
				String bow = "";
				for (int i = 1; i < tokens.length; i++) {
					String token = tokens[i];
					String [] labelTerms = token.split(":")[0].split("_");
					for (String term : labelTerms) {
						term = term.replaceAll("[^a-zA-Z]", "").toLowerCase();
						if (!stopWords.contains(term) && term.length() > 3) {
							bow += term + " ";
						} // end of if
					} // end of for (term)
				} // end of for (token)
				docfile.write(bow + "\n");
				docfile.flush();
				docfile.close();
			} // end of for (doc)
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createTaggedDocs
	
	/*
	 * This method filters the documents that are represented by tags (i.e. bag of tag words) and creates folders for different class labels. (e.g. business, non-business)
	 */
	public void filterTaggedDocumentsByCategory(String cat) {
		Map<String, Set<String>> docNameToCatMap = getDocumentsPredefinedCategories();
//		File folder = new File("/home/mehdi/topicdiscovery/classification/tagdocs/bus/business/");
//		File folder = new File("/home/mehdi/topicdiscovery/classification/tagdocs/heal/health/");
		File folder = new File("/home/mehdi/topicdiscovery/classification/tagdocs/sci/science/");
		File [] fileList = folder.listFiles();
		for (File f : fileList) {
			String fname = f.getName().substring(0, f.getName().indexOf(".txt"));
			System.out.println(fname);
			if (!docNameToCatMap.get(fname).contains(cat)) {
				File source = new File(folder.getPath() + "/" + f.getName());
//				File dest   = new File("/home/mehdi/topicdiscovery/classification/tagdocs/bus/non-business/" + f.getName());
//				File dest   = new File("/home/mehdi/topicdiscovery/classification/tagdocs/heal/non-health/" + f.getName());
				File dest   = new File("/home/mehdi/topicdiscovery/classification/tagdocs/sci/non-science/" + f.getName());
				try {
					FileUtils.moveFile(source, dest);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	} // end of filterDocumentsbyCategory
	
	/**
	 * 
	 */


}
