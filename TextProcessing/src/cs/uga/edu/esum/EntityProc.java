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



public class EntityProc { 
	
	private final String uriPrefix = "http://dbpedia.org/resource/"; 
	private VirtGraph virtGraph = null;

	private static final String entityFile = "/home/mehdi/EntitySummarization/evaluation/faces_evaluation/instances.txt";
	private static final String entityNameOnly = "/home/mehdi/EntitySummarization/evaluation/entNameOnly.txt";
	private static final String predicateStopWords = "/home/mehdi/EntitySummarization/evaluation/predicateStopWords.txt";
	// Map each object (word) to an ID
	private static final String wordToID = "/home/mehdi/EntitySummarization/evaluation/wordToID.txt";
	//Map each entity (doc) to ID
	private static final String docToID = "/home/mehdi/EntitySummarization/evaluation/docToID.txt"; 
	
	private static final String corpus = "/home/mehdi/EntitySummarization/evaluation/corpus.txt"; 
	
	//Holding all documents (Entities) in entityDocs folder
	private static final String entityDocs = "/home/mehdi/EntitySummarization/evaluation/entityDocs/";
	private static final String predicateList = "/home/mehdi/EntitySummarization/evaluation/";
	
	
	private Set<String> predicateSet=new HashSet<String>();
	private Set<String> objectSet=new HashSet<String>();
	private Set<String> domainSet =new HashSet<String>();
	private Set<String> rangeSet=new HashSet<String>();
	
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
	public void predicateChecker() throws IOException{
		/*
		 * Read entity Name only (entNameOnly.txt) as input and extract all useful predicates
		 */
			
			//Reading from entityFile
			BufferedReader br = null;
			FileReader fr = null;

			try {
				String entityLine;
				br = new BufferedReader(new FileReader(entityNameOnly));
				while ((entityLine = br.readLine()) != null) {
					//Calling predicateExtractor to extract all predicates and Objects for all entities in entNameOnly.txt
					predicateExtractor(entityLine);
					
				}
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
			
			
			// Making Word to ID file
			
			File fout = new File(wordToID);
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			 sortedMapWordToID = sortByValue(mapWordToID);
		
			for (Map.Entry<String,Integer> entry : sortedMapWordToID.entrySet()) {
				  String key = entry.getKey();
				  Integer value = entry.getValue();
				  
				  try {
					bw.write(key + "  " + value);
					bw.newLine();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  
				}
			
				bw.close();
				fos.close();
	
		}
	
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
		
//				//Check if the predicate is stop predicate drop it
//				
//				if (!predicateStopWordsVec.contains(predicateName)){
//			
//					//System.out.println(predicateName.toLowerCase());
//					//Add to the set
//					predicateSet.add(predicateName.toLowerCase());
//					//Add to the Vector
//					predicateVector.add(predicateName);
//					
//					
//					
//				}
			} //End of While 

		for (String word: objectSet){
			if (!mapWordToID.containsKey(word)){
			mapWordToID.put(word, wordCount);
			System.out.println(word + " " + wordCount);
			wordCount++;
			}
		}
		
	
		//making doc using object for each entity
		
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
				
				// Making entity(doc) to ID file
				BufferedWriter bw1 = null;

			      try {
			      
			         bw1 = new BufferedWriter(new FileWriter(docToID, true));
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
				brObject = new BufferedReader(new FileReader(wordToID));
				while ((strObject = brObject.readLine()) != null) {
					 String[] kvPair = strObject.split("  ");
					    mapWordToID.put(kvPair[0], Integer.valueOf(kvPair[1]));
				
				
				} //End While
				
				
				BufferedReader br = null;
				FileReader fr = null;

			
					String entityLine;
					br = new BufferedReader(new FileReader(entityNameOnly));
					while ((entityLine = br.readLine()) != null) {
						countEntity++;
						BufferedReader brEntity = null;
						FileReader frEntity = null;
						String entityDoc;
						brEntity = new BufferedReader(new FileReader(entityDocs+entityLine+".txt"));
					
						System.out.println(countEntity + "HHHHHHHHHHHHHH");
						
						
					//	////////////////////
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
						      
						         bw1 = new BufferedWriter(new FileWriter(corpus, true));
						         	bw1.write(countEntity + " " + value+" "+ d);
									bw1.newLine();
									
								} catch (IOException e) {
									
									e.printStackTrace();
								}
								bw1.close();
					
					        }
					       
					    }
						
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
		//
		
		/// Extract Domain and Range for each predicate
		public void domainRangeExtractor() throws IOException{
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
					System.out.println(purePredicateName);
					
					// extract Predicate Name only for example : ontology/owningCompany , we will receive owningCompany as exactPredicateName
					exactPredicateName=purePredicateName.substring(purePredicateName.indexOf("/")+1, purePredicateName.indexOf(" "));
					System.out.println(exactPredicateName);
					
					//upperPredicateName can be property or ontology for example property/title or ontology/owningCompany
					upperPredicateName=purePredicateName.substring(0, purePredicateName.indexOf("/"));
					System.out.println(upperPredicateName);
					
					
				
		//Connecting to Virtuoso to extract Doamin and Range
		System.out.println("Connecting to Virtuoso to extract domain and range ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
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
			
			System.out.println("Domain of  "+ exactPredicateName+ " is "+ d.toString());
			
			System.out.println("Range of  "+ exactPredicateName+ " is "+ r.toString());
			
		
			
				//Finding the position of the last "/"	and take only domain name
				if(d.toString()!=null){
				int indexDomain = d.toString().lastIndexOf("/");
				String domainStr=d.toString().substring(indexDomain+1);
				System.out.println(domainStr);
				domainSet.add(domainStr);
				}
				if(r.toString()!=null){
				int indexRange = r.toString().lastIndexOf("/");
				String rangeStr=r.toString().substring(indexRange+1);
				rangeSet.add(rangeStr);
				}
				
					
				} ///end of the while loop for graph
		
		
				}
			} catch (IOException e) {

			e.printStackTrace();

		}
		
		//Creating Domain List
			
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
