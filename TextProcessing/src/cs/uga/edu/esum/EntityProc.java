package cs.uga.edu.esum;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.*;
import grph.algo.bfs.BFSResult;
import grph.properties.LabelProperty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
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


// comment 2

public class EntityProc { 
	
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String wikiCategoryUriPrefix = "http://dbpedia.org/resource/Category:";
	private VirtGraph virtGraph = null;

	private static final String entityFile = "/home/mehdi/EntitySummarization/evaluation/faces_evaluation/instances.txt";
	private static final String entityNameOnly = "/home/mehdi/EntitySummarization/evaluation/entNameOnly.txt";
	private static final String predicateStopWords = "/home/mehdi/EntitySummarization/evaluation/predicateStopWords.txt";
	// Map each object (word) to an ID
	private static final String wordToIdFileName = "/home/mehdi/EntitySummarization/evaluation/wordToID.txt";
	//Map each entity (doc) to ID
	private static final String docToIdFileName = "/home/mehdi/EntitySummarization/evaluation/docToId.txt"; 
	private static final String rangeToIdFileName = "/home/mehdi/EntitySummarization/evaluation/rangeToId.txt";
	private static final String domainToIdFileName = "/home/mehdi/EntitySummarization/evaluation/domainToId.txt";
	
	private static final String predicateToIdFileName = "/home/mehdi/EntitySummarization/evaluation/predicateToId.txt"; 
	
	private static final String corpusFileName = "/home/mehdi/EntitySummarization/evaluation/corpus.txt"; 
	private static final String objectToTypeMapFileName = "/home/mehdi/EntitySummarization/evaluation/objToType.ser"; 
	
	private static final String predicateDomainRangeFileName = "/home/mehdi/EntitySummarization/evaluation/predicateDomainRange.txt"; 
	
	//Holding all documents (Entities) in entityDocs folder
	private static final String entityDocs = "/home/mehdi/EntitySummarization/evaluation/entityDocs/";
	private static final String predicateList = "/home/mehdi/EntitySummarization/evaluation/";
	
	////
	private Set<String> predicateSet=new HashSet<String>();
	private Set<String> objectSet=new HashSet<String>();
	private Set<String> domainSet =new HashSet<String>();
	private Set<String> rangeSet=new HashSet<String>();
	private Map<String, Integer>domainMap=new HashMap<String,Integer>();
	private Map<String, Integer>rangeMap=new HashMap<String,Integer>();
	
	
	private Vector<String> predicateVector=new Vector<String>();
	private Vector<String> objectVector =new Vector<String>();
	private Map<String, Integer> mapWordToID =new HashMap<String, Integer>();
	private Map<String, Integer> sortedMapWordToID =new HashMap<String, Integer>();
	private Map<String, Integer> mapDocToID =new HashMap<String, Integer>();
	private int wordCount=0;
	private int docCount=0;
	private int predicateNumber=0;
	private int domainNumber=0;
	private int rangeNumber=0;
	
	

	public void readWriteEntity() throws FileNotFoundException{
		/*
		 * Read entity file as input (instances.txt) write entity to name only file as output (entNameOnly.txt)
		 */
		
			//Writing to entNameOnly.txt
			File fout = new File(entityNameOnly);
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			//Reading from entityFile (instances.txt)
			BufferedReader br = null;
			FileReader fr = null;

			try {
				String entityLine, entityPart, result;
				String delStr = "http://dbpedia.org/resource/";	
				br = new BufferedReader(new FileReader(entityFile));

				while ((entityLine = br.readLine()) != null) {
					int index = entityLine.indexOf(delStr);
					int subIndex = index + delStr.length();
					entityPart=entityLine.substring(subIndex);
					result = entityPart.substring(0, entityPart.indexOf(":-:"));
					System.out.println(result.toLowerCase());
				//	bw.write(result.toLowerCase());
					bw.write(result);
					bw.newLine();
					
				}
				bw.close();

			} catch (IOException e) {

				e.printStackTrace();

			} finally {

				try {

					if (br != null)
						br.close();

					if (fr != null)
						fr.close();

				} catch (IOException ex) {

					ex.printStackTrace();

				}

			}

		
		}
	
	
/////////////////////////////////////////////////////
//	public void predicateChecker() throws IOException{
//		/*
//		 * Read entity Name only (entNameOnly.txt) as input and extract all useful predicates
//		 */
//			//Reading from entityFile
//			BufferedReader br = null;
//			FileReader fr = null;
//
//			try {
//				String entityLine;
//				br = new BufferedReader(new FileReader(entityNameOnly));
//				while ((entityLine = br.readLine()) != null) {
//					//Calling predicateExtractor to extract all predicates and Objects for all entities in entNameOnly.txt
//					predicateExtractor(entityLine);
//				}
//			} catch (IOException e) {
//
//				e.printStackTrace();
//
//			} finally {
//
//				try {
//
//					if (br != null)
//						br.close();
//
//					if (fr != null)
//						fr.close();
//				} catch (IOException ex) {
//					ex.printStackTrace();
//				}
//			}
//			// Making Word to ID file
//			File fout = new File(wordToIdFileName);
//			FileOutputStream fos = new FileOutputStream(fout);
//			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//			 sortedMapWordToID = sortByValue(mapWordToID);
//			for (Map.Entry<String,Integer> entry : sortedMapWordToID.entrySet()) {
//				  String key = entry.getKey();
//				  Integer value = entry.getValue();
//				  
//				  try {
//					bw.write(key + "  " + value);
//					bw.newLine();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				  
//				}
//				bw.close();
//				fos.close();
//		}
	
	public void predicateChecker() throws IOException{
		/*
		 * Read entity Name only (entNameOnly.txt) as input and extract all useful predicates
		 */
		//Reading from entityFile
		try {
			String entityLine;
			BufferedReader br = new BufferedReader(new FileReader(entityNameOnly));
			while ((entityLine = br.readLine()) != null) {
				//Calling predicateExtractor to extract all predicates and Objects for all entities in entNameOnly.txt
				predicateExtractor(entityLine);
			}
			br.close();
		} catch (IOException e) {

			e.printStackTrace();

		}
	} // end of predicateChecker
	
	
	public Set<String> readPredicateStopWords(String filename) {
		//Reading stop predicate from predicate Stop Words file
		Set<String> predicateStopWords = new HashSet<String>();
		try {
			String stopPredicate;
			BufferedReader br = new BufferedReader(new FileReader(filename));

			while ((stopPredicate = br.readLine()) != null) {
				predicateStopWords.add(stopPredicate);
			}
			br.close();
		} catch (IOException e) {

			e.printStackTrace();

		}
		return predicateStopWords;
	} // end of readPredicateStopWords
	
	
	public void processEntities() throws IOException {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		String entityName = "";
		BufferedReader br = new BufferedReader(new FileReader(entityNameOnly));
		FileWriter wordToIdFile = new FileWriter(wordToIdFileName);
		FileWriter docToIdFile = new FileWriter(docToIdFileName);
		FileWriter rTypesFile = new FileWriter(rangeToIdFileName);
		FileWriter sTypesFile = new FileWriter(domainToIdFileName);
		FileWriter predicateToIdFile = new FileWriter(predicateToIdFileName);
		FileWriter predicateDomainRangeFile = new FileWriter(predicateDomainRangeFileName);
		int subjectTypeIdGenerator = 0;
		int prediateIdGenerator = 0;
		int objectTypeIdGenerator = 0;
		int wordIdGenerator = 0;
		int docIdGenerator = 0;
		Map<String,Integer> objectTypesMap = new HashMap<String,Integer>();
		Map<String,Integer> subjectTypesMap = new HashMap<String,Integer>();
		Map<String, Integer> wordToIdMap = new HashMap<String,Integer>();
		Map<String, Integer> predicateToIdMap = new HashMap<String,Integer>();
		Map<Integer, Set<Integer>> objectTotypeMap = new HashMap<Integer,Set<Integer>>();
		while ((entityName = br.readLine()) != null) {
			String subjectUrl = uriPrefix + entityName;
			Set<String> subjectTypes = getEntityTypes(subjectUrl);
			for (String t : subjectTypes) {
				if (subjectTypesMap.get(t) == null) {
					sTypesFile.write(t + " " + subjectTypeIdGenerator + "\n");
					subjectTypesMap.put(t, subjectTypeIdGenerator);
					subjectTypeIdGenerator++;
				} // end of if
			} // end of for
			subjectTypes.addAll(getEntityDomain(subjectUrl));
			Set<String> predicateStopWordsSet = readPredicateStopWords(predicateStopWords);
			FileWriter docFile = new FileWriter(entityDocs + entityName +".txt");
			//Connecting to Virtuoso to extract predicates and objects
			StringBuffer queryString = new StringBuffer();
			queryString.append("SELECT ?p ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + entityName + ">" + " ?p ?o . ");
			queryString.append("}");
			Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			docToIdFile.write(entityName + " " + docIdGenerator + "\n");
			docIdGenerator++;
			while (results.hasNext()) {
				Set<String> objectTypes = new HashSet<String>();
				QuerySolution result = results.nextSolution();
				RDFNode predicate = result.get("p");
				RDFNode object = result.get("o");
				//Finding the position of the last "/"	and take only predicate name
				int index = predicate.toString().lastIndexOf("/");
				String predicateName = predicate.toString().substring(index + 1);

				/* if predicate is subject or contains http://dbpedia.org/ontology/ and Object contains http://dbpedia.org/ we will keep that predicate
				 * if predicate  contains http://dbpedia.org/property/ and Object contains http://dbpedia.org/ we will keep that predicate
				 * if predicate is http://dbpedia.org/ontology/wikiPageWikiLink we will drop it
				 */
				if (predicateStopWordsSet.contains(predicateName) || object.isLiteral()) continue;
				if (predicate.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) continue;
				if (!predicate.toString().contains("http://dbpedia.org") || !object.toString().contains("http://dbpedia.org")) continue;
				String objectName = "";
				if (predicateName.equals("subject")){
					objectName = object.toString().substring(wikiCategoryUriPrefix.length());
				}else {
					objectName = object.toString().substring(uriPrefix.length());
				} // end of if
				if (predicateToIdMap.get(predicateName) == null) {
					predicateToIdMap.put(predicateName, prediateIdGenerator);
					predicateToIdFile.write(predicateName + " " + prediateIdGenerator + "\n");
					prediateIdGenerator++;
				}
				objectTypes = getEntityTypes(object.toString());
				
				// writes the object as many times as its types size + 1 to the doc file
				for (int i = 0; i <= objectTypes.size(); i++) {
					docFile.write(objectName + "|");
				}
				if (wordToIdMap.get(objectName) == null) {
					wordToIdMap.put(objectName, wordIdGenerator);
					wordToIdFile.write(objectName + " " + wordIdGenerator + "\n");
					wordIdGenerator++;
				}
				objectTypes.addAll(getEntityRange(predicate.toString()));
				for (String t : objectTypes) {
					if (objectTypesMap.get(t) == null) {
						objectTypesMap.put(t, objectTypeIdGenerator);
						rTypesFile.write(t + " " + objectTypeIdGenerator + "\n");
						objectTypeIdGenerator++;
					} // end of if
				} // end of for
				// if this object has no type at all, attach owl#Thing type to it
				if (objectTypes.isEmpty()) {
					objectTypes.add("owl#Thing");
				}
				int objectId = wordToIdMap.get(objectName);
				if (objectTotypeMap.get(objectId) == null) {
					Set<Integer> types = new HashSet<Integer>();
					for (String t : objectTypes) {
						types.add(objectTypesMap.get(t));
					} // end of for
					objectTotypeMap.put(objectId, types);
				}else {
					Set<Integer> types = objectTotypeMap.get(objectId);
					for (String t : objectTypes) {
						types.add(objectTypesMap.get(t));
					} // end of for
					objectTotypeMap.put(objectId, types);
				} // end of if
				int predicateId = predicateToIdMap.get(predicateName);
				for (String d : subjectTypes) {
					int domainId = subjectTypesMap.get(d);
					for (String r : objectTypes) {
						int rangeId = objectTypesMap.get(r);
						predicateDomainRangeFile.write(predicateId + " " + domainId + " " + rangeId  + "\n");
					} // end of for
				} // end of for
			} // end of while
			docFile.close();
		} // end of while
		wordToIdFile.close();
		docToIdFile.close();
		rTypesFile.close();
		sTypesFile.close();
		predicateToIdFile.close();
		predicateDomainRangeFile.close();
		br.close();
		saveObjectToTypeMap(objectTotypeMap, objectToTypeMapFileName);
	} // end of processEntities
	
	
	
	
	public void saveObjectToTypeMap(Map<Integer, Set<Integer>> objectTotypeMap, String fileName) {
		try {
			File f = new File(fileName);
			if (f.exists()) {
				System.out.println(fileName + " already exists");
				f.delete();
				System.out.println(fileName + " deleted.");
			} // end of if
			FileOutputStream outputFile = new FileOutputStream(f);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(objectTotypeMap);
			out.close();
			System.out.println("Map Serialized successfully.\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of saveObjectToTypeMap
	
	@SuppressWarnings("unchecked")
	public Map<Integer, Set<Integer>> loadObjectToTypeMap(String fileName) {
		System.out.println("Loading " + fileName + " into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			Map<Integer, Set<Integer>> objToTypeMap = (Map<Integer, Set<Integer>>) in.readObject();
			in.close();
			System.out.println(fileName + " Successfuly Loaded into Memory.\n");
			return objToTypeMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadDoubleMatrix


	public void processEntities_withTypefrequency() throws IOException {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		String entityName = "";
		BufferedReader br = new BufferedReader(new FileReader(entityNameOnly));
		FileWriter wordToIdFile = new FileWriter(wordToIdFileName);
		FileWriter docToIdFile = new FileWriter(docToIdFileName);
		FileWriter rTypesFile = new FileWriter(rangeToIdFileName);
		FileWriter sTypesFile = new FileWriter(domainToIdFileName);
		FileWriter predicateToIdFile = new FileWriter(predicateToIdFileName);
		FileWriter predicateDomainRangeFile = new FileWriter(predicateDomainRangeFileName);
		int subjectTypeIdGenerator = 0;
		int prediateIdGenerator = 0;
		int objectTypeIdGenerator = 0;
		int wordIdGenerator = 0;
		int docIdGenerator = 0;
		Map<String,Integer> objectTypesMap = new HashMap<String,Integer>();
		Map<String,Integer> subjectTypesMap = new HashMap<String,Integer>();
		Map<String, Integer> wordToIdMap = new HashMap<String,Integer>();
		Map<String, Integer> predicateToIdMap = new HashMap<String,Integer>();
		while ((entityName = br.readLine()) != null) {
			String subjectUrl = uriPrefix + entityName;
			Set<String> subjectTypes = getEntityTypes(subjectUrl);
			for (String t : subjectTypes) {
				if (subjectTypesMap.get(t) == null) {
					sTypesFile.write(t + " " + subjectTypeIdGenerator + "\n");
					subjectTypesMap.put(t, subjectTypeIdGenerator);
					subjectTypeIdGenerator++;
				} // end of if
			} // end of for
			subjectTypes.addAll(getEntityDomain(subjectUrl));
			Set<String> predicateStopWordsSet = readPredicateStopWords(predicateStopWords);
			FileWriter docFile = new FileWriter(entityDocs + entityName +".txt");
			//Connecting to Virtuoso to extract predicates and objects
			StringBuffer queryString = new StringBuffer();
			queryString.append("SELECT ?p ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + entityName + ">" + " ?p ?o . ");
			queryString.append("}");
			Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			docToIdFile.write(entityName + " " + docIdGenerator + "\n");
			docIdGenerator++;
			while (results.hasNext()) {
				Set<String> objectTypes = new HashSet<String>();
				QuerySolution result = results.nextSolution();
				RDFNode predicate = result.get("p");
				RDFNode object = result.get("o");
				//Finding the position of the last "/"	and take only predicate name
				int index = predicate.toString().lastIndexOf("/");
				String predicateName = predicate.toString().substring(index + 1);

				/* if predicate is subject or contains http://dbpedia.org/ontology/ and Object contains http://dbpedia.org/ we will keep that predicate
				 * if predicate  contains http://dbpedia.org/property/ and Object contains http://dbpedia.org/ we will keep that predicate
				 * if predicate is http://dbpedia.org/ontology/wikiPageWikiLink we will drop it
				 */
				if (predicateStopWordsSet.contains(predicateName) || object.isLiteral()) continue;
				if (predicate.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) continue;
				if (!predicate.toString().contains("http://dbpedia.org") || !object.toString().contains("http://dbpedia.org")) continue;
				String objectName = "";
				if (predicateName.equals("subject")){
					objectName = object.toString().substring(wikiCategoryUriPrefix.length());
				}else {
					objectName = object.toString().substring(uriPrefix.length());
				} // end of if
				if (predicateToIdMap.get(predicateName) == null) {
					predicateToIdMap.put(predicateName, prediateIdGenerator);
					predicateToIdFile.write(predicateName + " " + prediateIdGenerator + "\n");
					prediateIdGenerator++;
				}
				objectTypes = getEntityTypes(object.toString());
				
				// writes the object and all its types to the doc file
				docFile.write(objectName + "|");
				for (String t : objectTypes) {
					docFile.write(t + "|");
				}
				if (wordToIdMap.get(objectName) == null) {
					wordToIdMap.put(objectName, wordIdGenerator);
					wordToIdFile.write(objectName + " " + wordIdGenerator + "\n");
					wordIdGenerator++;
				}
				for (String t : objectTypes) {
					if (wordToIdMap.get(t) == null) {
						wordToIdMap.put(t, wordIdGenerator);
						wordToIdFile.write(t + " " + wordIdGenerator + "\n");
						wordIdGenerator++;
					}
				} // end of for
				objectTypes.addAll(getEntityRange(object.toString()));
				for (String t : objectTypes) {
					if (objectTypesMap.get(t) == null) {
						objectTypesMap.put(t, objectTypeIdGenerator);
						rTypesFile.write(t + " " + objectTypeIdGenerator + "\n");
						objectTypeIdGenerator++;
					} // end of if
				} // end of for
				int predicateId = predicateToIdMap.get(predicateName);
				for (String d : subjectTypes) {
					int domainId = subjectTypesMap.get(d);
					for (String r : objectTypes) {
						int rangeId = objectTypesMap.get(r);
						predicateDomainRangeFile.write(predicateId + " " + domainId + " " + rangeId  + "\n");
					} // end of for
				} // end of for
			} // end of while
			docFile.close();
		} // end of while
		wordToIdFile.close();
		docToIdFile.close();
		rTypesFile.close();
		sTypesFile.close();
		predicateToIdFile.close();
		predicateDomainRangeFile.close();
		br.close();
	} // end of processEntities
	
	
	public void makeCorpus() throws IOException {
		List<String> docToId = readDocument(docToIdFileName);
		Map<String,Integer> docToIdMap = new HashMap<String,Integer>();
		for (String line : docToId) {
			String [] tokens = line.split(" ");
			String docName = tokens[0];
			String docId = tokens[1];
			docToIdMap.put(docName, Integer.parseInt(docId));
		} // end of for
		List<String> wordToId = readDocument(wordToIdFileName);
		Map<String,Integer> wordToIdMap = new HashMap<String,Integer>();
		for (String line : wordToId) {
			String [] tokens = line.split(" ");
			String wordName = tokens[0];
			String wordId = tokens[1];
			wordToIdMap.put(wordName, Integer.parseInt(wordId));
		} // end of for
		FileWriter corpusFile = new FileWriter(corpusFileName);
		File fileDirectory = new File(entityDocs);
		for (File file : fileDirectory.listFiles()) {
			String fileName = file.getName();
			String document = readDocumentAsString(entityDocs + fileName);
			fileName = fileName.replace(".txt", "");
			String [] words = document.split("\\|");
			Map<Integer, Integer> wordsFrequency = new HashMap<Integer, Integer>();
			for (String word : words) {
				System.out.println(word);
				int wordId = wordToIdMap.get(word);
				int preFreq = wordsFrequency.get(wordId) != null ? wordsFrequency.get(wordId) : 0;
				wordsFrequency.put(wordId, preFreq + 1);
			} // end of for
			for (Map.Entry<Integer, Integer> entry : wordsFrequency.entrySet()) {
				int wordId = entry.getKey();
				int wordFreq = entry.getValue();
				corpusFile.write(docToIdMap.get(fileName) + " " + wordId + " " + wordFreq + "\n");
			} // end of for
			
		} // end of for (File)
		corpusFile.close();
		
	} // end of makeCorpus
	
	public List<String> readDocument(String filename) {
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
	} // end of readDocument
	
	public String readDocumentAsString(String filename) {
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
	
	
	private Set<String> getEntityRange(String predicateUrl) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?ran FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + predicateUrl + ">" + " <http://www.w3.org/2000/01/rdf-schema#range> ?ran . } ");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		Set<String> types = new HashSet<String>();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			int index = result.getResource("ran").toString().lastIndexOf("/");
			types.add(result.getResource("ran").toString().substring(index));
		} // end of while
		return types;
	} // end of getEntityRange
	
	private Set<String> getEntityDomain(String predicateUrl) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?dom FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + predicateUrl + ">" + " <http://www.w3.org/2000/01/rdf-schema#domain> ?dom . } ");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		Set<String> types = new HashSet<String>();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			int index = result.getResource("dom").toString().lastIndexOf("/");
			types.add(result.getResource("dom").toString().substring(index));
		} // end of while
		return types;
	} // end of getEntityDomain


	private Set<String> getEntityTypes(String entiyUrl) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + entiyUrl + ">" + " a ?o . ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		Set<String> types = new HashSet<String>();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			int index = result.getResource("o").toString().lastIndexOf("/");
			types.add(result.getResource("o").toString().substring(index + 1));
		} // end of while
		return types;
	} // end of getEntityTypes


		////////////////////////////////////////////////////////
	/*
	 * taking entity name and extract all predicates (meaningful ones) 
	 */
		public void predicateExtractor(String entityName ) throws IOException{
		
			//Reading stop predicate from predicate Stop Words file
			BufferedReader br = null;
			FileReader fr = null;
			Vector<String> predicateStopWordsVec=new Vector<String>();
			try {
				String stopPredicate;
				br = new BufferedReader(new FileReader(predicateStopWords));

				while ((stopPredicate = br.readLine()) != null) {
					predicateStopWordsVec.add(stopPredicate);
					
				}
				} catch (IOException e) {

				e.printStackTrace();

			}
		//Connecting to Virtuoso to extract predicates and objects
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?p ?o FROM <" + GRAPH + "> WHERE { <http://dbpedia.org/resource/" + entityName +"> ?p ?o. }  ");

		Query sparql = QueryFactory.create(queryString.toString());

		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);

		ResultSet results = vqe.execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			RDFNode graph = result.get("graph");
		//	RDFNode s = result.get("s");
			RDFNode p = result.get("p");
			RDFNode o = result.get("o");
				//Finding the position of the last "/"	and take only predicate name
				int index = p.toString().lastIndexOf("/");
				String predicateName=p.toString().substring(index+1);
				
				/* if predicate is subject or contains http://dbpedia.org/ontology/ and Object contains http://dbpedia.org/ we will keep that predicate
				 * if predicate  contains http://dbpedia.org/property/ and Object contains http://dbpedia.org/ we will keep that predicate
				 * if predicate is http://dbpedia.org/ontology/wikiPageWikiLink we will drop it
				 */
				
				
				if (predicateName.equals("subject")){
					
						int indexO = o.toString().lastIndexOf("/");
						String objectName=o.toString().substring(indexO+10);
						objectSet.add(objectName);
						//Add to the Vector
						objectVector.add(objectName);
						
						//predicateSet.add(predicateName);
						
						predicateSet.add(p.toString());
						
						
				}else if(!(p.toString().equals("http://dbpedia.org/ontology/wikiPageWikiLink")) && p.toString().contains("http://dbpedia.org/ontology/") && o.toString().contains("http://dbpedia.org/") ){
						int indexP=p.toString().lastIndexOf("/");
						String predicateNameStr=p.toString().substring(indexP+1);
						
						int indexO = o.toString().lastIndexOf("/");
						String objectName=o.toString().substring(indexO+1);
						objectSet.add(objectName);
						//Add to the Vector
						objectVector.add(objectName);
						
						//Add predicate NAME only to predicate Set
						//predicateSet.add(predicateName);
						
						//Add predicate URL to predicate Set (Whole Predicate)
						predicateSet.add(p.toString());
						
				}else if(p.toString().contains("http://dbpedia.org/property/") && o.toString().contains("http://dbpedia.org/") && !(o.isLiteral())){
					int indexP=p.toString().lastIndexOf("/");
					String predicateNameStr=p.toString().substring(indexP+1);
					
					int indexO = o.toString().lastIndexOf("/");
					String objectName=o.toString().substring(indexO+1);
					objectSet.add(objectName);
					//Add to the Vector
					//objectVector.add(predicateNameStr+": "+objectName);
					objectVector.add(objectName);
					
					//Add to predicate set
					//predicateSet.add(predicateName);
					
					//Add predicate URL to predicate Set (Whole Predicate)
					predicateSet.add(p.toString());
			}
		

			} //End of While 

	     //	creating mapWordToID
		for (String word: objectSet){
			if (!mapWordToID.containsKey(word)){
			mapWordToID.put(word, wordCount);
			System.out.println(word + " " + wordCount);
			wordCount++;
			}
		}
		
				//making doc using object for each entity (Barack_Obama.txt)
			
				File fout = new File(entityDocs+ entityName +".txt");
				FileOutputStream fos = new FileOutputStream(fout);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
						//Adding Objects
				for(String o: objectVector){
					try {
						bw.write(o + " | ");
						//bw.newLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println(entityName +".txt has been created."  );
				objectVector.clear();
				objectSet.clear();
				bw.close();
				fos.close();
				// End of making doc
				///////////////////////////////////////////
				
				// Making entity(doc) to ID file (entity  ID) Barack_Obama  0 
				BufferedWriter bw1 = null;
				try {
			      
			         bw1 = new BufferedWriter(new FileWriter(docToIdFileName, true));
			         	bw1.write(entityName + "  " + docCount);
						bw1.newLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				 		docCount++;
				bw1.close();
	
	} // End of Entity Extractor Function
		
	//////////////////////////////////////
		//Entity
		
		
		
		
		
		
		
		
		public void corpusMaker() throws NumberFormatException, IOException{
			int countEntity=0;
			BufferedReader brObject=null;
			FileReader frObject=null;
			Map<String, Integer> mapWordToID =new HashMap<String,Integer>();
				String strObject;
				brObject = new BufferedReader(new FileReader(wordToIdFileName));
				while ((strObject = brObject.readLine()) != null) {
					 String[] kvPair = strObject.split("  ");
					    mapWordToID.put(kvPair[0], Integer.valueOf(kvPair[1]));
				
				} //End While
				BufferedReader br = null;
				FileReader fr = null;
				String entityLine;
					br = new BufferedReader(new FileReader(entityNameOnly));
					while ((entityLine = br.readLine()) != null) {
						
						BufferedReader brEntity = null;
						FileReader frEntity = null;
						String entityDoc;
						brEntity = new BufferedReader(new FileReader(entityDocs+entityLine+".txt"));
					
						String mystr=brEntity.readLine();
						
						String trimmed = mystr.trim().replaceAll(" | ", " ");
					    String[] a = trimmed.split(" ");
					    ArrayList<Integer> p = new ArrayList<>();
					    for (int i = 0; i < a.length; i++) {
					        if (p.contains(i)) {
					            continue;
					        }
					        int d = 1;
					        for (int j = i+1; j < a.length; j++) {
					            if (a[i].equals(a[j])) {
					                d += 1;
					                p.add(j);
					            }
					        }
					        int value=0;
					        if(mapWordToID.containsKey(a[i])){
					        value=mapWordToID.get(a[i]);
					        System.out.println("Count of "+value+"  "+a[i]+" is:"+d);
					     
					        // Writing Corpus
						 BufferedWriter bw1 = null;
						 	try {
						      
						         bw1 = new BufferedWriter(new FileWriter(corpusFileName, true));
						         	bw1.write(countEntity + " " + value+" "+ d);
									bw1.newLine();
									
								} catch (IOException e) {
									
									e.printStackTrace();
								}
								bw1.close();
					
					        }
					       
					    }
					    countEntity++;
					}
			
		}
		
		
		//Making Predicate list text file
		public void makingPredicateList() throws IOException{
			File foutPre = new File(predicateList+"predicateList.txt");
			FileOutputStream fosPre = new FileOutputStream(foutPre);
			BufferedWriter bwPre = new BufferedWriter(new OutputStreamWriter(fosPre));
				for(String p: predicateSet){
					try {
						bwPre.write(p + "  "+ predicateNumber );
						bwPre.newLine();
					predicateNumber++;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Predicate List has been created."  );
				predicateSet.clear();
			
				bwPre.close();
				fosPre.close();
		}
		////END OF Making Predicate list text file
		
		
		/// Extract Domain and Range for each predicate
		public void domainRangeExtractor() throws IOException{
		// Create Map for Domain <domain name , domain ID>
			BufferedReader brDomain = null;
			FileReader frDomain = null;
			String domainName=null;
			int domainID=0;
				brDomain = new BufferedReader(new FileReader(predicateList+ "domainList.txt"));
				while ((domainName = brDomain.readLine()) != null) {
					    String[] t = domainName.split("  ");
					    domainMap.put(t[0], Integer.parseInt(t[1]));
				}
				
			/*	for (String s : domainMap.keySet()) {
				    System.out.println(s + " is " + domainMap.get(s));
				}
			*/
		// Create Map for Range <Range name , Range ID>
				BufferedReader brRange = null;
				FileReader frRange = null;
				String rangeName=null;
				int rangeID=0;
					brRange = new BufferedReader(new FileReader(predicateList+ "rangeList.txt"));
					while ((rangeName = brRange.readLine()) != null) {
						    String[] t = rangeName.split("  ");
						    rangeMap.put(t[0], Integer.parseInt(t[1]));
					}
					
		//// ******\\\\\\		
				
				
				
			String predicateName;
			String exactPredicateName=null;
			String upperPredicateName=null;
			//Reading from Predicate List file
			BufferedReader br = null;
			FileReader fr = null;
			
			try {
				
				
				br = new BufferedReader(new FileReader(predicateList+ "predicateList.txt"));

				while ((predicateName = br.readLine()) != null) {
					
					
					int index = predicateName.toString().lastIndexOf(".org/");
					String purePredicateName=predicateName.toString().substring(index+5);
					
					// extract Predicate Name only for example : ontology/owningCompany , we will receive owningCompany as exactPredicateName
					exactPredicateName=purePredicateName.substring(purePredicateName.indexOf("/")+1, purePredicateName.indexOf(" "));
					
					//upperPredicateName can be property or ontology for example property/title or ontology/owningCompany
					upperPredicateName=purePredicateName.substring(0, purePredicateName.indexOf("/"));
					
					//Finding Predicate ID
					 String[] t1 = predicateName.split("  ");
					
		//Connecting to Virtuoso to extract Doamin and Range
		//System.out.println("Connecting to Virtuoso to extract domain and range ... ");
		virtGraph = connectToVirtuoso();
		//System.out.println("Successfully Connected to Virtuoso!\n");
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?dom ?ran FROM <" + GRAPH + "> WHERE { <http://dbpedia.org/"+upperPredicateName+"/"+ exactPredicateName+"> ");
		queryString.append(" <http://www.w3.org/2000/01/rdf-schema#domain> ?dom . "+ " <http://dbpedia.org/"+upperPredicateName+"/"+ exactPredicateName+">" +"  <http://www.w3.org/2000/01/rdf-schema#range> ?ran . }  ");

		Query sparql = QueryFactory.create(queryString.toString());

		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);

		ResultSet results = vqe.execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			RDFNode graph = result.get("graph");
		//	RDFNode s = result.get("s");
			RDFNode d = result.get("dom");
			RDFNode r = result.get("ran");
			
			
			String domainStr1=null;
			String rangeStr1=null;
			if(d.toString()!=null){
				int indexDomain = d.toString().lastIndexOf("/");
				domainStr1=d.toString().substring(indexDomain+1);
			//System.out.println(domainStr1);
				}
				if(r.toString()!=null){
				int indexRange = r.toString().lastIndexOf("/");
				rangeStr1=r.toString().substring(indexRange+1);
				}
			
			
			
			
			int domainID1=0;
			int rangeID1=0;
			if (domainMap.containsKey(domainStr1)){
				domainID1 =domainMap.get(domainStr1);
			}
			if (rangeMap.containsKey(rangeStr1)){
				rangeID1 =rangeMap.get(rangeStr1);
			}
			
			
		      
		     // Writing Predicate Domain Range file
			 BufferedWriter bw1 = null;
			 	try {
			      
			         bw1 = new BufferedWriter(new FileWriter(predicateDomainRangeFileName, true));
			         	bw1.write(t1[1] + " " + domainID1+" "+ rangeID1);
						bw1.newLine();
						
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					bw1.close();
			
			
				//Finding the position of the last "/"	and take only domain name
				if(d.toString()!=null){
				int indexDomain = d.toString().lastIndexOf("/");
				String domainStr=d.toString().substring(indexDomain+1);
		//		System.out.println(domainStr);
				domainSet.add(domainStr);
				}
				if(r.toString()!=null){
				int indexRange = r.toString().lastIndexOf("/");
				String rangeStr=r.toString().substring(indexRange+1);
				rangeSet.add(rangeStr);
				}
				
					
				} //end of the while loop for graph
		
		
				}
			} catch (IOException e) {

			e.printStackTrace();

		}
		
	/*	//Creating Domain List
			
			File foutDomain = new File(predicateList+"domainList.txt");
			FileOutputStream fosDomain = new FileOutputStream(foutDomain);
			BufferedWriter bwDomain = new BufferedWriter(new OutputStreamWriter(fosDomain));
				for(String d: domainSet){
					try {
						bwDomain.write(d + "  "+ domainNumber );
						bwDomain.newLine();
					domainNumber++;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Domain List has been created."  );
				predicateSet.clear();
				bwDomain.close();
				fosDomain.close();
		//END of Creating Domain List
				
				
		/// Create Range List
				File foutRange = new File(predicateList+"rangeList.txt");
				FileOutputStream fosRange = new FileOutputStream(foutRange);
				BufferedWriter bwRange = new BufferedWriter(new OutputStreamWriter(fosRange));
					for(String r: rangeSet){
						try {
							bwRange.write(r + "  "+ rangeNumber );
							bwRange.newLine();
						rangeNumber++;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					System.out.println("Range List has been created."  );
					predicateSet.clear();
					bwRange.close();
					fosRange.close();
		/// End of Create Range List
		*/
		}
		
		
		
		
		
		
		
		
		
		//Sorting Function 
		
		 public static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

		        // 1. Convert Map to List of Map
		        List<Map.Entry<String, Integer>> list =
		                new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

		        // 2. Sort list with Collections.sort(), provide a custom Comparator
		        //    Try switch the o1 o2 position for a different order
		        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
		            public int compare(Map.Entry<String, Integer> o1,
		                               Map.Entry<String, Integer> o2) {
		                return (o1.getValue()).compareTo(o2.getValue());
		            }
		        });

		        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
		        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		        for (Map.Entry<String, Integer> entry : list) {
		            sortedMap.put(entry.getKey(), entry.getValue());
		        }

		      	        return sortedMap;
		    }

		  
		
		
		
	
}
