package cs.uga.edu.simpleOntoPart;

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
import java.io.InputStreamReader;
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

import book.set.ObjectBilinkable;
import cs.uga.edu.wikiaccess.WikipediaAccessLayer;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;


// comment 2


public class entityProcessing { 
	
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String uriClassPrefix = "http://dbpedia.org/ontology/";
	private final String wikiCategoryUriPrefix = "http://dbpedia.org/resource/Category:";
	private VirtGraph virtGraph = null;

	private static final String entityFile = "/home/mehdi/simpleOntoPart/evaluation/instances.txt";
	private static final String entityNameOnly = "/home/mehdi/simpleOntoPart/evaluation/entNameOnly.txt";
	private static final String classNameOnly = "/home/mehdi/simpleOntoPart/evaluation/className/classNameOnly.txt";
	private static final String predicateStopWords = "/home/mehdi/simpleOntoPart/evaluation/predicateStopWords.txt";
	// Map each object (word) to an ID
	private static final String wordToIdFileName = "/home/mehdi/simpleOntoPart/evaluation/wordToID.txt";
	//Map each entity (doc) to ID
	private static final String docToIdFileName = "/home/mehdi/simpleOntoPart/evaluation/docToId.txt"; 
	
	private static final String predicateObjectPairToIdFileName = "/home/mehdi/simpleOntoPart/evaluation/predicateObjectPairToID.txt";
	
	private static final String rangeToIdFileName = "/home/mehdi/simpleOntoPart/evaluation/rangeToId.txt";
	private static final String domainToIdFileName = "/home/mehdi/simpleOntoPart/evaluation/domainToId.txt";
	
	private static final String predicateToIdFileName = "/home/mehdi/simpleOntoPart/evaluation/predicateToId.txt"; 
	
	private static final String corpusFileName = "/home/mehdi/simpleOntoPart/evaluation/corpus.txt"; 
	private static final String objectToTypeMapFileName = "/home/mehdi/simpleOntoPart/evaluation/objToType.ser"; 
	
	private static final String predicateDomainRangeFileName = "/home/mehdi/simpleOntoPart/evaluation/predicateDomainRange.txt"; 
	private static final String predicateObjectFileName = "/home/mehdi/simpleOntoPart/evaluation/predicateObject.ser";
	private static final String literalObjectFileName = "/home/mehdi/simpleOntoPart/evaluation/literalObjectName.txt";
	private static final String realObjectFileName = "/home/mehdi/simpleOntoPart/evaluation/realObjectName.txt";
	private static final String classIdFileName = "/home/mehdi/simpleOntoPart/evaluation/classListandID.txt";
	//private static final String subjectIdFileName = "/home/mehdi/simpleOntoPart/evaluation/entityListandID.txt";
	private static final String predicateObjectIdFileName = "/home/mehdi/simpleOntoPart/evaluation/predicateObjectID.txt";
	
	private static final String subjectIdCatIdFileName = "/home/mehdi/simpleOntoPart/evaluation/subjectIdCatId.txt";
	private static final String categoryIdFileName = "/home/mehdi/simpleOntoPart/evaluation/CategoryId.txt";
	
	
	protected String objectPredicateFileName = "/home/mehdi/simpleOntoPart/evaluation/objectPredicate.ser";
	protected final String predicateObjectWeightFileName = "/home/mehdi/simpleOntoPart/evaluation/predicateObjectWeight.ser";
	
	//Holding all documents (Entities) in entityDocs folder
	private static final String entityList = "/home/mehdi/simpleOntoPart/evaluation/";
	private static final String entityDocs = "/home/mehdi/simpleOntoPart/evaluation/entityDocs/"; 
	private static final String predicateList = "/home/mehdi/simpleOntoPart/evaluation/";
	



	
	////
	private Set<String> predicateSet=new HashSet<String>();
	private Set<String> objectSet=new HashSet<String>();
	private Set<String> domainSet =new HashSet<String>();
	private Set<String> rangeSet=new HashSet<String>();
	private Map<String, Integer>domainMap=new HashMap<String,Integer>();
	private Map<String, Integer>rangeMap=new HashMap<String,Integer>();
	
	
	private Vector<String> predicateVector=new Vector<String>();
	private Vector<String> objectVector = new Vector<String>();
	private Vector<String> objectVector1 = new Vector<String>();
	private Map<String, Integer> mapWordToID = new HashMap<String, Integer>();
	private Map<String, Integer> sortedMapWordToID = new HashMap<String, Integer>();
	private Map<String, Integer> mapDocToID = new HashMap<String, Integer>();
	private int wordCount=0;
	private int docCount=0;
	private int predicateNumber=0;
	private int domainNumber=0;
	private int rangeNumber=0;
	protected int[][] predicateObjectWeight = null;
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	//Create list of subject name (entity name) given a list of class Name
	public void createEntityList() throws IOException {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		FileWriter classToIdFile = new FileWriter(classIdFileName); //"/home/mehdi/simpleOntoPart/evaluation/classListandID.txt";
		FileWriter subjectToIdFile = new FileWriter(docToIdFileName); // "/home/mehdi/simpleOntoPart/evaluation/docToId.txt";
		FileWriter subjectIdCatIdFile = new FileWriter(subjectIdCatIdFileName); //"/home/mehdi/simpleOntoPart/evaluation/subjectIdCatId.txt";
		FileWriter CategoryIdFile = new FileWriter(categoryIdFileName); //"/home/mehdi/simpleOntoPart/evaluation/subjectIdCatId.txt";
		FileWriter entNameOnlyFile = new FileWriter(entityNameOnly); //"/home/mehdi/simpleOntoPart/evaluation/entNameOnly.txt";
		FileWriter wordToIdFile = new FileWriter(wordToIdFileName); //"/home/mehdi/simpleOntoPart/evaluation/wordToID.txt";
		FileWriter predicateObjectPair = new FileWriter(predicateObjectPairToIdFileName); //"/home/mehdi/simpleOntoPart/evaluation/predicateObjectPairToID.txt";
		FileWriter predicateToIdFile = new FileWriter(predicateToIdFileName); //"/home/mehdi/simpleOntoPart/evaluation/predicateToId.txt"; 
		FileWriter predicateObjectFile = new FileWriter(predicateObjectFileName);
		
		Set<String> predicateObjectPairSet = new HashSet<String>();
		Set<String> predicateObjectSet = new HashSet<String>();
		Set<String> subjectNames = new HashSet<String>();
		Map<String, Integer> subjectNameToIdMap = new HashMap<String,Integer>();
		Map<String, Integer> classNameToIdMap = new HashMap<String,Integer>();
		Vector<String> predicateObjectVec = new Vector<String>();
		Map<String, Integer> CategoryNameToIdMap = new HashMap<String,Integer>();
		Map<Integer, Integer> subjectIdCatIdMap = new HashMap<Integer,Integer>();
		Map<String, Integer> wordToIdMap = new HashMap<String,Integer>();
		Map<String, Integer> predicateToIdMap = new HashMap<String,Integer>();
		Map<String, Integer> predicateObjectIdMap = new HashMap<String,Integer>();
		
		Map<Integer, Set<Integer>> predicateToObjectMap = new HashMap<Integer,Set<Integer>>();
		Map<Integer, Set<String>> objectToCategoryMap = new HashMap<Integer,Set<String>>();
		Map<Integer, Set<Integer>> objectToPredicateMap = new HashMap<Integer,Set<Integer>>();
		Set<String> literalObject=new HashSet<>();
		Set<String> realObject=new HashSet<>();

		int CategoryIdGenerator=0;
		int subjectIdGenerator = 0;
		int classIdGenerator=0;
		int prediateIdGenerator = 0;
		int prediateObjectIdGenerator = 0;
		int wordIdGenerator = 0;
		int docIdGenerator = 0;
		
		String className = "";
		BufferedReader br = new BufferedReader(new FileReader(classNameOnly));

		//Read list of classes from a text file  and extract entity from that class
		while ((className = br.readLine()) != null) {
			//Connecting to Virtuoso to extract predicates and objects
			StringBuffer queryString = new StringBuffer();
			queryString.append("SELECT ?s FROM <" + GRAPH + "> WHERE { ");
			queryString.append(" ?s a <" + uriClassPrefix + className + ">   " );
			queryString.append("}   Limit 1");
			//System.out.println(queryString);
			Query sparql = QueryFactory.create(queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			
		int numberOfPredicate=0;
		// For each subject (entity) extract number of useful predicates
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				RDFNode subject = result.get("s");
				
				//Finding the position of the last "/"	and take only predicate name
				int index = subject.toString().lastIndexOf("/");
				String subjectName = subject.toString().substring(index + 1);
				
				StringBuffer queryString1 = new StringBuffer();
				queryString1.append("select   (count(?p)as ?pTotal) FROM <" + GRAPH + "> WHERE { ");
				queryString1.append("<http://dbpedia.org/resource/" + subjectName +"> ?p ?o.  " );
				queryString1.append("FILTER (?p NOT IN (<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://www.w3.org/2002/07/owl#sameAs> ) )");
				queryString1.append("FILTER (?p NOT IN (<http://purl.org/dc/terms/subject> ) )");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/wikiPageWikiLink> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/wikiPageExternalLink> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/abstract> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://www.w3.org/2000/01/rdf-schema#comment> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/thumbnail> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://xmlns.com/foaf/0.1/depiction> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/image> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/mapImage> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/percentage> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/seats> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/width> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/map> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/length> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://xmlns.com/foaf/0.1/homepage> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/d> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/b> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/washpo> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://www.w3.org/2000/01/rdf-schema#label> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/property/imageSize> ) ) ");
				queryString1.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/height> ) ) ");
				queryString1.append("FILTER(!isLiteral(?o) ) ");
				queryString1.append("}  ");
				
				//System.out.println(queryString1);
				Query sparql1 = QueryFactory.create(queryString1.toString());
				VirtuosoQueryExecution vqe1 = VirtuosoQueryExecutionFactory.create (sparql1, virtGraph);
				ResultSet results1 = vqe1.execSelect();
				if (results1.hasNext()) {
					QuerySolution result1 = results1.nextSolution();
					RDFNode subject1 = result1.get("pTotal");
					int index1 = subject1.toString().lastIndexOf("^^");
					String myPredicateNum = subject1.toString().substring(0,index1 );
					numberOfPredicate=Integer.parseInt(myPredicateNum);
					
				}
				//Only extract/select entities where the number of its predicate is greater that 30 and 
				//extract their predicate and objects
				predicateObjectVec.clear();
				
				if (numberOfPredicate>40 && subjectName.length()>5){
					System.out.println(className + "      subjectName:"+ subjectName);
					StringBuffer queryString2 = new StringBuffer();
					queryString2.append("SELECT ?p ?o FROM <" + GRAPH + "> WHERE { ");
					queryString2.append("<" + uriPrefix + subjectName + ">" + " ?p ?o . ");
					queryString2.append("FILTER (?p NOT IN (<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://www.w3.org/2002/07/owl#sameAs> ) )");
					queryString2.append("FILTER (?p NOT IN (<http://purl.org/dc/terms/subject> ) )");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/wikiPageWikiLink> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/wikiPageExternalLink> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/abstract> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://www.w3.org/2000/01/rdf-schema#comment> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/thumbnail> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://xmlns.com/foaf/0.1/depiction> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/image> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/mapImage> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/percentage> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/seats> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/width> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/map> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/length> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://xmlns.com/foaf/0.1/homepage> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/d> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/b> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/washpo> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://www.w3.org/2000/01/rdf-schema#label> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/property/imageSize> ) ) ");
					queryString2.append("FILTER (?p NOT IN (<http://dbpedia.org/ontology/height> ) ) ");
					queryString2.append("FILTER(!isLiteral(?o) ) ");
					
					
					
					queryString2.append("}  ");
					Query sparql2 = QueryFactory.create(queryString2.toString());
					VirtuosoQueryExecution vqe2 = VirtuosoQueryExecutionFactory.create (sparql2, virtGraph);
					ResultSet results2 = vqe2.execSelect();
							
					while (results2.hasNext()) {
							QuerySolution result2 = results2.nextSolution();
							RDFNode predicate = result2.get("p");
							RDFNode object = result2.get("o");
							
							int index2 = predicate.toString().lastIndexOf("/");
							String predicateName = predicate.toString().substring(index2 + 1);
							
							String objectNameRaw=object.toString();
							
							if (object.toString().contains("^^")){
								int index4 = object.toString().lastIndexOf("^");
								 objectNameRaw = object.toString().substring(0,index4-1);
							}
							
							
							int index3 = objectNameRaw.toString().lastIndexOf("/");
							String objectName = objectNameRaw.toString().substring(index3 + 1);
							//System.out.println(subjectName+"  Predicate "+predicateName + "    "+ objectName);
							
							objectName=objectName.replace("*","");
							objectName=objectName.replace("\n", "");
							objectName=objectName.replace("@en", "");
							objectName=objectName.replace(")", "");
							objectName=objectName.replace("(", "");
							objectName=objectName.replaceAll("\\s+"," ");
							objectName=objectName.trim();
							objectName=objectName.replaceAll(" ","_");
							
							if (objectName.length()>20){
							objectName=objectName.substring(0, 20);
							}
							
							
							//Vector of predicate object pair to add into bag of word for each entity (doc)
							predicateObjectVec.add(predicateName + "*"+ objectName);
							predicateObjectSet.add(predicateName + "*"+ objectName);
							
							
							//Store ONLY predicate with ID (unique pair)
							if (predicateToIdMap.get(predicateName) == null) {
								predicateToIdMap.put(predicateName, prediateIdGenerator);
								predicateToIdFile.write(predicateName + " " + prediateIdGenerator + "\n");
								prediateIdGenerator++;
							}
							//Store ONLY object with ID (unique pair)
							if (wordToIdMap.get(objectName) == null) {
								wordToIdMap.put(objectName, wordIdGenerator);
								wordToIdFile.write(objectName + " " + wordIdGenerator + "\n");
								wordIdGenerator++;
							}
							//Store pair of predicate*object with ID (unique pair)
							if (predicateObjectIdMap.get(predicateName+" || "+objectName) == null) {
								predicateObjectIdMap.put(predicateName+" || "+objectName, prediateObjectIdGenerator);
								predicateObjectPair.write(predicateName+" || "+objectName + "    " + prediateObjectIdGenerator + "\n");
								prediateObjectIdGenerator++;
								predicateObjectPairSet.add(predicateName+"||"+objectName);
							}
							
							
							
							} //end while
					
					//Create bag of words for each subject, entity (doc). write into file.
					FileWriter entityFileDocs = new FileWriter(entityDocs+ subjectName+".txt");
					for (String myUnit: predicateObjectVec){
						entityFileDocs.write(myUnit+" | ");
					}
					entityFileDocs.close();
					
					
					//if a class has an entity then it will be added into classNametoID file 
					if ( classNameToIdMap.get(className) == null) {
						classNameToIdMap.put(className, classIdGenerator);
						classToIdFile.write(className + " " + classIdGenerator + "\n");
						classIdGenerator++;
					}
					
					
					if ( subjectNameToIdMap.get(subjectName) == null) {
						subjectNameToIdMap.put(subjectName, subjectIdGenerator);
						subjectToIdFile.write(subjectName + " " + subjectIdGenerator + "\n");
						entNameOnlyFile.write(subjectName + "\n");
						
						subjectIdGenerator++;
						//System.out.println(subjectName+ "  predicateNume " +numberOfPredicate);
					}
				
				}// end if predicate number
					
			} // end of while
				
		}// end of while for class list
		
//		int kk=0;
//		FileWriter POIdFile = new FileWriter("/home/mehdi/simpleOntoPart/evaluation/POID.txt"); //"/home/mehdi/simpleOntoPart/evaluation/predicateToId.txt"; 
//		for (String K: predicateObjectSet){
//			POIdFile.write(K +"     "+kk+"\n");
//			kk++;
//		}
//		POIdFile.close();
		System.out.println("Size of predicate-object map"+ predicateObjectIdMap.size() +  "\n  Class Map " +classNameToIdMap.size());
		Set<String> domains = new HashSet<String>();
	for (String mypair: predicateObjectPairSet){
		String v =mypair.toString();
		System.out.println(v.toString().split("||"));
//		String [] tokens1 = mypair.split("||");
//		String part1=tokens1[0];
//		System.out.println(part1);
//		domains=getPredicateDomain("http://dbpedia.org/ontology/"+"starring");
//		for(String mydom : domains){
//			System.out.println("Domain forsssss  " +mydom);
//		}
		
	}
			
		

	//Set<Integer> st = predicateObjectIdMap.keySet();
	// create the lambda matrix
	int numOfPredicates = predicateToIdMap.size();
	int numOfObjects    = wordToIdMap.size();
	predicateObjectWeight = new int[numOfPredicates][numOfObjects];
	
	for (int i = 0; i < numOfPredicates; i++) {
		for (int j = 0; j < numOfObjects; j++) {
			Set<String> cats = objectToCategoryMap.get(j) != null ? objectToCategoryMap.get(j) : new HashSet<String>();
			if (objectToPredicateMap.get(j) != null && objectToPredicateMap.get(j).contains(i) && !cats.isEmpty()) {
				predicateObjectWeight[i][j] = cats.size(); 
			}else {
				predicateObjectWeight[i][j] = 1; 
			}
		} // end of for (j)
	} // end of for (i)
		
		
		
		
		
		
		
		
		
		br.close();
		subjectToIdFile.close();
		classToIdFile.close();
		predicateObjectPair.close();
		entNameOnlyFile.close();
		
		
		//************* extract Category ***************\\
		//entityNameOnly = "/home/mehdi/simpleOntoPart/evaluation/entNameOnly.txt";
		String subjectName1 = "";
		BufferedReader br1 = new BufferedReader(new FileReader(entityNameOnly));
		//Read list of subjects from a text file  
		while ((subjectName1 = br1.readLine()) != null) {
		StringBuffer queryString4 = new StringBuffer();
		queryString4.append("SELECT ?o FROM <" + GRAPH + "> WHERE { "); // uriPrefix = "http://dbpedia.org/resource/"
		queryString4.append("<" + uriPrefix + subjectName1 + ">" + " <http://purl.org/dc/terms/subject> ?o . ");
		queryString4.append("}  ");
		Query sparql4 = QueryFactory.create(queryString4.toString());
		VirtuosoQueryExecution vqe4 = VirtuosoQueryExecutionFactory.create (sparql4, virtGraph);
		ResultSet results4 = vqe4.execSelect();
				
		while (results4.hasNext()) {
			    QuerySolution result4 = results4.nextSolution();
			    RDFNode object4 = result4.get("o");
				int index4 = object4.toString().lastIndexOf(":");
				String objectCategoryName = object4.toString().substring(index4 + 1);
				if ( CategoryNameToIdMap.get(objectCategoryName) == null) {
					CategoryNameToIdMap.put(objectCategoryName, CategoryIdGenerator);
					CategoryIdFile.write(objectCategoryName + " " + CategoryIdGenerator + "\n");
					CategoryIdGenerator++;
					}
				//System.out.println(subjectName1 + "&&&&&&&&&&&&&&&&&&& "+objectCategoryName+"\n");
				subjectIdCatIdFile.write(subjectNameToIdMap.get(subjectName1) + " "+ CategoryNameToIdMap.get(objectCategoryName)+"\n");
				
             }//end while
		}//end while
		br1.close();
		subjectIdCatIdFile.close();
		
		//************* END extract Category ***************\\
		
		
		
		
	} // end of createEntityList
	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void corpusMaker() throws NumberFormatException, IOException{
		int countEntity=0;
		BufferedReader brObject=null;
		FileReader frObject=null;
		Map<String, Integer> mapWordToID =new HashMap<String,Integer>();
			String strObject;
			
			//predicateObjectPairToIdFileName word=pair of predicate and object
			brObject = new BufferedReader(new FileReader(predicateObjectPairToIdFileName));
			while ((strObject = brObject.readLine()) != null) {
				 String[] kvPair = strObject.split("    ");
				    mapWordToID.put(kvPair[0], Integer.valueOf(kvPair[1].trim()));
			
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
				       // System.out.println("Count of "+value+"  "+a[i]+" is:"+d);
				     
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
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
//	//************* extract Predicate-Object X Class Matrix ***************\\
//			//entityNameOnly = "/home/mehdi/simpleOntoPart/evaluation/predicateObjectPairToID.txt";
//	public void predicateObjectClassMatrix() throws IOException{
//
//		Map<String, Integer> predicateObjectIdMap = new HashMap<String,Integer>();
//		Map<String, Integer> classIdMap = new HashMap<String,Integer>();
//		String predicateObjectpair = "";
//		BufferedReader br1 = new BufferedReader(new FileReader(predicateObjectPairToIdFileName));
//		//Read list of subjects from a text file  
//		while ((predicateObjectpair = br1.readLine()) != null) {
//			String [] tokens = predicateObjectpair.split(" ");
//			tokens [0] = Integer.parseInt(tokens [0]);
//		
//		}
//		
//		
////		savePredicateToObjectMap(predicateToObjectMap, predicateObjectFileName);
//		saveObjectToPredicateMap(objectToPredicateMap, objectPredicateFileName);
//		System.out.println("predicates: " + predicateToIdMap.size() + "    " + objectToPredicateMap.size());
//		Set<Integer> st = objectToPredicateMap.keySet();
//		// create the lambda matrix
//		int numOfPredicates = predicateToIdMap.size();
//		int numOfObjects    = wordToIdMap.size();
//		predicateObjectWeight = new int[numOfPredicates][numOfObjects];
//		
//		for (int i = 0; i < numOfPredicates; i++) {
//			for (int j = 0; j < numOfObjects; j++) {
//				Set<String> cats = objectToCategoryMap.get(j) != null ? objectToCategoryMap.get(j) : new HashSet<String>();
//				if (objectToPredicateMap.get(j) != null && objectToPredicateMap.get(j).contains(i) && !cats.isEmpty()) {
//					predicateObjectWeight[i][j] = cats.size(); 
//				}else {
//					predicateObjectWeight[i][j] = 1; 
//				}
//			} // end of for (j)
//		} // end of for (i)
		
		
		
		
	//}
	
	
	private Set<String> getPredicateRange(String predicateUrl) {
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
			types.add(result.getResource("ran").toString().substring(index + 1));
		} // end of while
		return types;
	} // end of getPredicateRange
	
	private Set<String> getPredicateDomain(String predicateUrl) {
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
	} // end of getPredicateDomain

	
/////////////////////////////////////////////////////////	
	public void processEntities() throws IOException {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		String entityName = "";
		BufferedReader br = new BufferedReader(new FileReader(entityNameOnly));
		FileWriter wordToIdFile = new FileWriter(wordToIdFileName);
		FileWriter docToIdFile = new FileWriter(docToIdFileName);
		FileWriter predicateToIdFile = new FileWriter(predicateToIdFileName);
		FileWriter predicateObjectFile = new FileWriter(predicateObjectFileName);
		FileWriter literalObjectFile=new FileWriter(literalObjectFileName);
		FileWriter realObjectFile=new FileWriter(realObjectFileName);
		int prediateIdGenerator = 0;
		int wordIdGenerator = 0;
		int docIdGenerator = 0;
		Map<String, Integer> wordToIdMap = new HashMap<String,Integer>();
		Map<String, Integer> predicateToIdMap = new HashMap<String,Integer>();
		Map<Integer, Set<Integer>> predicateToObjectMap = new HashMap<Integer,Set<Integer>>();
		Map<Integer, Set<String>> objectToCategoryMap = new HashMap<Integer,Set<String>>();
		Map<Integer, Set<Integer>> objectToPredicateMap = new HashMap<Integer,Set<Integer>>();
		Set<String> literalObject=new HashSet<>();
		Set<String> realObject=new HashSet<>();
		
		while ((entityName = br.readLine()) != null) {
//			String subjectUrl = uriPrefix + entityName;
//			Set<String> subjectTypes = getEntityTypes(subjectUrl);
			Set<String> predicateStopWordsSet = readPredicateStopWords(predicateStopWords);
			//FileWriter docFile = new FileWriter(entityDocs + entityName +".txt");
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
			
			objectVector1.clear();
			
			while (results.hasNext()) {
				Set<String> objectCategories = new HashSet<String>();
				//to keep literal similar words extracted from ENR
				Set<String> literalCategories = new HashSet<String>();
				
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
				//drop literal objects
				//if (predicateStopWordsSet.contains(predicateName) || object.isLiteral()) continue;
				if (predicateStopWordsSet.contains(predicateName)) continue;
				if (predicate.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") || predicate.toString().equals("http://dbpedia.org/property/website")) continue;
				// check literal if contains digit
				if(object.isLiteral() && object.toString().matches(".*\\d+.*")) continue;
				if(object.isLiteral() &&(object.toString().length()<4||object.toString().contains(".jpg")||object.toString().contains(".png")||object.toString().contains(".svg")||object.toString().contains("yes")||object.toString().contains("no")))		continue;
				if(object.toString().length()<2){
					System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXX");
					continue;
				}
				
				
				if (predicateName.equals("thumbnail")||predicateName.equals("homepage")||predicateName.equals("url")||predicateName.length()<4) continue;
				
				
				//Literals do not have http://dbpedia.org so if you do not want to consider literal uncomment line below
			//	if (!predicate.toString().contains("http://dbpedia.org") || !object.toString().contains("http://dbpedia.org")) continue;
				if (!predicate.toString().contains("http://dbpedia.org") ) continue;
				
				
				
				
				String objectName = "";
				//check for literal abstract
				if (predicate.toString().contains("http://dbpedia.org/ontology/abstract") ) {
					objectName = object.toString();//.substring("http://dbpedia.org/ontology/abstract".length());
					objectVector1.add(predicateName+ " "+objectName + "|");
				}
				objectName = "";
				
				
				if (predicateName.equals("subject")){
					//objectName = object.toString().substring(wikiCategoryUriPrefix.length());
				}else {
					
					
					
					
					if(object.toString().contains("http://dbpedia.org")){
						objectName = object.toString().substring(uriPrefix.length());
						System.out.println(entityName+ "  "+ objectName);
						realObject.add(objectName);
						
						
					}else{
						//Real literal
						String myLiteral=object.toString();
						myLiteral=myLiteral.replace("@en", "");
						myLiteral=myLiteral.replace("( )", "");
						myLiteral=myLiteral.replace("\"", "");
						myLiteral=myLiteral.replace("*", "_");
						myLiteral=myLiteral.replace("\n", " ");
						literalObject.add(myLiteral);
						myLiteral=myLiteral.replace(" ", "_");
						//if(myLiteral.contains("http://")||myLiteral.contains("https://")||myLiteral.length()<4||myLiteral.contains(".jpg")||myLiteral.contains(".png")||myLiteral.contains(".svg"))continue;

						//objectName=object.toString();
						//System.out.println("Literal: "+entityName+"  "+predicateName+"  "+ myLiteral);
						objectName=myLiteral;
						

					}
				} // end of if
				
//			if(object.isLiteral()){
//				System.out.println("Literal: "+entityName+"  "+ object.toString());
//				}
				if (predicateToIdMap.get(predicateName) == null) {
					predicateToIdMap.put(predicateName, prediateIdGenerator);
					predicateToIdFile.write(predicateName + " " + prediateIdGenerator + "\n");
					prediateIdGenerator++;
				}
				if (wordToIdMap.get(objectName) == null) {
					wordToIdMap.put(objectName, wordIdGenerator);
					wordToIdFile.write(objectName + " " + wordIdGenerator + "\n");
					wordIdGenerator++;
				}
			//	docFile.write(objectName + "|");
			objectVector1.add(predicateName+ " "+objectName + "|");
				
				
				
				
				
				
			//	objectCategories = getEntityCategories(object.toString());
				
//				// if you want to increase object frequency in the document
//				for (int i = 0; i < objectCategories.size(); i++) {
//					docFile.write(objectName + "|");
//				}
//				
				
				// if you want to include object categories in the document, write the object categories to the doc file
//				for (String c : objectCategories) {
//					docFile.write(c + "|");
//					if (wordToIdMap.get(c) == null) {
//						wordToIdMap.put(c, wordIdGenerator);
//						wordToIdFile.write(c + " " + wordIdGenerator + "\n");
//						wordIdGenerator++;
//					}
//				} // end of for
				
				
			
				
				// Extract Similar words
				// if you want to include similar words in the document, write the similar words to the doc file
			//	Set <String> similarWords=new Vector<String>();
//				objectCategories=extractSimilarWords(objectName);
//						
//				for (String c : objectCategories) {
//					docFile.write(c + "|");
//					if (wordToIdMap.get(c) == null) {
//						wordToIdMap.put(c, wordIdGenerator);
//						wordToIdFile.write(c + " " + wordIdGenerator + "\n");
//						wordIdGenerator++;
//					}
//				} // end of for
//				
				
				
//				//literalCategories is a set of similar words to each literal, which are adding to bag of words.
//				literalCategories=extractSimilarWordsforLiteral(objectName);
//				
//				for (String cl : literalCategories) {
//					docFile.write(cl + "|");
//					if (wordToIdMap.get(cl) == null) {
//						wordToIdMap.put(cl, wordIdGenerator);
//						wordToIdFile.write(cl + " " + wordIdGenerator + "\n");
//						wordIdGenerator++;
//					}
//				} // end of for
				
//				
//				
//				int objectId = wordToIdMap.get(objectName);
//				int predicateId = predicateToIdMap.get(predicateName);
//				objectToCategoryMap.put(objectId, objectCategories);
//				
//				if (objectToPredicateMap.get(objectId) == null) {
//					Set<Integer> preds = new HashSet<Integer>();
//					preds.add(predicateId);
//					objectToPredicateMap.put(objectId, preds);
//					
//					// uncomment the block below ONLY if you want to include object categories in the documents
//					for (String c : objectCategories) {
//						int catId = wordToIdMap.get(c);
//						Set<Integer> catpreds = new HashSet<Integer>();
//						if (objectToPredicateMap.get(catId) == null) {
//							catpreds.add(predicateId);
//						}else {
//							catpreds = objectToPredicateMap.get(catId);
//							catpreds.add(predicateId);
//						} // end of if
//						objectToPredicateMap.put(catId, catpreds);
//					} // end of for
//				}else {
//					Set<Integer> preds = objectToPredicateMap.get(objectId);
//					preds.add(predicateId);
//					objectToPredicateMap.put(objectId, preds);
//					
//					// uncomment the block below ONLY if you want to include object categories in the documents
//					for (String c : objectCategories) {
//						int catId = wordToIdMap.get(c);
//						Set<Integer> catpreds = new HashSet<Integer>();
//						if (objectToPredicateMap.get(catId) == null) {
//							catpreds.add(predicateId);
//						}else {
//							catpreds = objectToPredicateMap.get(catId);
//							catpreds.add(predicateId);
//						} // end of if
//						objectToPredicateMap.put(catId, catpreds);
//					} // end of for
//				} // end of if
//				
//				
//				
//				////// LITERAL
//				
//				if (objectToPredicateMap.get(objectId) == null) {
//					Set<Integer> preds = new HashSet<Integer>();
//					preds.add(predicateId);
//					objectToPredicateMap.put(objectId, preds);
//					
//					// uncomment the block below ONLY if you want to include object categories in the documents
//					for (String c : literalCategories) {
//						int catId = wordToIdMap.get(c);
//						Set<Integer> catpreds = new HashSet<Integer>();
//						if (objectToPredicateMap.get(catId) == null) {
//							catpreds.add(predicateId);
//						}else {
//							catpreds = objectToPredicateMap.get(catId);
//							catpreds.add(predicateId);
//						} // end of if
//						objectToPredicateMap.put(catId, catpreds);
//					} // end of for
//				}else {
//					Set<Integer> preds = objectToPredicateMap.get(objectId);
//					preds.add(predicateId);
//					objectToPredicateMap.put(objectId, preds);
//					
//					// uncomment the block below ONLY if you want to include object categories in the documents
//					for (String c : literalCategories) {
//						int catId = wordToIdMap.get(c);
//						Set<Integer> catpreds = new HashSet<Integer>();
//						if (objectToPredicateMap.get(catId) == null) {
//							catpreds.add(predicateId);
//						}else {
//							catpreds = objectToPredicateMap.get(catId);
//							catpreds.add(predicateId);
//						} // end of if
//						objectToPredicateMap.put(catId, catpreds);
//					} // end of for
//				} // end of if
//				
//				
//				
//				
//				
				
				
//				int predicateId = predicateToIdMap.get(predicateName);
//				if (predicateToObjectMap.get(predicateId) == null) {
//					Set<Integer> objs = new HashSet<Integer>();
//					objs.add(wordToIdMap.get(objectName));
//					for (String c : objectCategories) {
//						objs.add(wordToIdMap.get(c));
//					} // end of for
//					predicateToObjectMap.put(predicateId, objs);
//				}else {
//					Set<Integer> objs = predicateToObjectMap.get(predicateId);
//					for (String c : objectCategories) {
//						objs.add(wordToIdMap.get(c));
//					} // end of for
//					predicateToObjectMap.put(predicateId, objs);
//				} // end of if
				
				
//				int predicateId = predicateToIdMap.get(predicateName);
//				for (String c : objectCategories) {
//					int objId = wordToIdMap.get(c);
//					predicateObjectFile.write(predicateId + " " + objId  + "\n");
//				} // end of for
			} // end of while
			//docFile.close();
			
			System.out.println(objectVector1.size());
			if (objectVector1.size() > 15) {
			System.out.println("AAAA");
			FileWriter docFile = new FileWriter(entityDocs + entityName +".txt");
		
			for (String myObject1 : objectVector1){
				docFile.write(myObject1);
			}
			docFile.close();
			}
			
			
			
		} // end of while
		wordToIdFile.close();
		docToIdFile.close();
		predicateToIdFile.close();
		predicateObjectFile.close();
		
		//Write Set of Literals into File
		for (String myliteral : literalObject){
			literalObjectFile.write(myliteral + "\n");
		}
		literalObjectFile.close();
	
		
		
		
		
		
		
		
		//Write Set of realObjects into File
				for (String myObject : realObject){
					realObjectFile.write(myObject + "\n");
				}
				realObjectFile.close();
		
		
		br.close();
////		savePredicateToObjectMap(predicateToObjectMap, predicateObjectFileName);
//		saveObjectToPredicateMap(objectToPredicateMap, objectPredicateFileName);
//		System.out.println("predicates: " + predicateToIdMap.size() + "    " + objectToPredicateMap.size());
//		Set<Integer> st = objectToPredicateMap.keySet();
//		// create the lambda matrix
//		int numOfPredicates = predicateToIdMap.size();
//		int numOfObjects    = wordToIdMap.size();
//		predicateObjectWeight = new int[numOfPredicates][numOfObjects];
//		
//		for (int i = 0; i < numOfPredicates; i++) {
//			for (int j = 0; j < numOfObjects; j++) {
//				Set<String> cats = objectToCategoryMap.get(j) != null ? objectToCategoryMap.get(j) : new HashSet<String>();
//				if (objectToPredicateMap.get(j) != null && objectToPredicateMap.get(j).contains(i) && !cats.isEmpty()) {
//					predicateObjectWeight[i][j] = cats.size(); 
//				}else {
//					predicateObjectWeight[i][j] = 1; 
//				}
//			} // end of for (j)
//		} // end of for (i)
//		
		
//		for (int i = 0; i < numOfPredicates; i++) {
//			for (int j = 0; j < numOfObjects; j++) {
//				Set<String> cats = objectToCategoryMap.get(j) != null ? objectToCategoryMap.get(j) : new HashSet<String>();
//				if (predicateToObjectMap.get(i).contains(j) && !cats.isEmpty()) {
//					predicateObjectWeight[i][j] = cats.size(); 
//				}else {
//					predicateObjectWeight[i][j] = 1; 
//				}
//				if (predicateObjectWeight[i][j] == 0)
//					System.out.println("=======");
//			} // end of for (j)
//		} // end of for (i)
		//saveMatrix(predicateObjectWeight, predicateObjectWeightFileName);
	} // end of processEntities


	
	public static Set<String> extractSimilarWords(String keyword) throws IOException{
		// Open the file
		FileInputStream fstream = new FileInputStream("W2Voutput.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		Map<String, String> datamap=new HashMap<>();
		//Read File Line By Line and extract object name and similar words
		//for example Columbia_University : new_york_university 0.75,cornell_university 0.68,university_of_chicago 0.67
		while ((strLine = br.readLine()) != null)   {
		      String[] items = strLine.split(" : ");
		      datamap.put(items[0], items[1]);
		}
		//items[1]= new_york_university 0.75,cornell_university 0.68,university_of_chicago 0.67
		//creating vector of similar words upper than a threshold
		Set <String> myOutput=new HashSet<>();
		if (datamap.containsKey(keyword)){
			String [] mydata=datamap.get(keyword).split(",");
			List<String> itemList = Arrays.asList(mydata);
		     for (String item : itemList) {
		    	  String [] mydata1=item.split(" ");
		    	  //defining the threshold 0.50
		         if (Double.parseDouble(mydata1[1])>0.50){
		        	 myOutput.add(mydata1[0]);
		        	// System.out.println(mydata1[0]);
		         }
		       }
		}
		//Close the input stream
		br.close();
		return myOutput;
	}
	
	
	/////////////////////////////////////////////////////
	
	
	
		public static Set<String> extractSimilarWordsforLiteral(String keyword) throws IOException{
			// Open the file
			FileInputStream fstream = new FileInputStream("finalresult.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String strLine;
			Map<String, String> datamap=new HashMap<>();
			//Read File Line By Line and extract object name and similar words
			//for example Columbia_University : new_york_university 0.75,cornell_university 0.68,university_of_chicago 0.67
			while ((strLine = br.readLine()) != null)   {
			      String[] items = strLine.split(" :: ");
			      if (items[1].equals(",")) continue;
			   
			      
			   //   System.out.println(items[0]+"      "+ items[1]);
			      datamap.put(items[0], items[1]);
			}
			//items[1]= new_york_university 0.75,cornell_university 0.68,university_of_chicago 0.67
			//creating vector of similar words upper than a threshold
			Set <String> myOutput=new HashSet<>();
			if (datamap.containsKey(keyword)){
				String [] mydata=datamap.get(keyword).split(",");
				List<String> itemList = Arrays.asList(mydata);
			     for (String item : itemList) {
			    	 // String [] mydata1=item.split(" ");
			    	  //defining the threshold 0.50
			         //if (Double.parseDouble(mydata1[1])>0.50){
			        	 myOutput.add(item.toLowerCase());
			        	// System.out.println(mydata1[0]);
			        // }
			       }
			}
			//Close the input stream
			br.close();
			return myOutput;
		}
	/////////////////////////////////////////////////////
	

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
	
	
	/* This method processes entities based on their Wikipedia categories.
	 * 
	 */
	
	private void saveObjectToPredicateMap(Map<Integer, Set<Integer>> objectToPredicateMap, String fileName) {
		saveMap(objectToPredicateMap, fileName);
	} // end of saveObjectToPredicateMap


	public void saveMatrix(int [][] mat, String fileName) {
		System.out.println("Serializing Matrix...");
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
			out.writeObject(mat);
			out.close();
			System.out.println("Matrix Serialized successfully.\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of saveMatrix
	
	public int[][] loadIntMatrix(String fileName) {
		System.out.println("Loading " + fileName + " into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			int[][] z = (int[][]) in.readObject();
			in.close();
			System.out.println(fileName + " Successfuly Loaded into Memory.\n");
			return z;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadIntMatrix
	
	private void savePredicateToObjectMap(Map<Integer, Set<Integer>> predicateToObjectMap, String fileName) {
		saveMap(predicateToObjectMap, fileName);
	} // end of savePredicateToObjectMap
	
	@SuppressWarnings("unchecked")
	public Map<Integer, Set<Integer>> loadPredicateToObjectMap(String fileName) {
		System.out.println("Loading " + fileName + " into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			Map<Integer, Set<Integer>> predToObjMap = (Map<Integer, Set<Integer>>) in.readObject();
			in.close();
			System.out.println(fileName + " Successfuly Loaded into Memory.\n");
			return predToObjMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of loadPredicateToObjectMap


	private Set<String> getEntityCategories(String entiyUrl) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("SELECT ?cat FROM <" + GRAPH + "> WHERE { ");
		queryString.append("<" + entiyUrl + ">" + " <http://purl.org/dc/terms/subject> ?cat. ");
		queryString.append("}");
		Query sparql = QueryFactory.create(queryString.toString());
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, virtGraph);
		ResultSet results = vqe.execSelect();
		Set<String> types = new HashSet<String>();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			types.add(result.getResource("cat").toString().substring(wikiCategoryUriPrefix.length()));
		} // end of while
		return types;
	} // end of getEntityCategories


	/*
	 * This method is to process the entities and create related files using objects and their types.
	 * It will supplement the doc files by repeating each object as many times as its types. 
	 */
	public void processEntities_WithObjectFrequency() throws IOException {
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
				objectTypes.addAll(getPredicateRange(predicate.toString()));
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
		saveMap(objectTotypeMap, fileName);
	} // end of saveObjectToTypeMap
	
	
	
	public void saveMap(Map<Integer, Set<Integer>> map, String fileName) {
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
			out.writeObject(map);
			out.close();
			System.out.println("Map Serialized successfully.\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of saveMap
	
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
