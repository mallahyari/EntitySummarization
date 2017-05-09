package cs.uga.edu.dicgenerator;

/**
*
*/

import static cs.uga.edu.dicgenerator.VirtuosoAccess.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.Scanner;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.common.collect.HashBiMap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import cs.uga.edu.dicgenerator.CategoryNameMapper;

/**
* @author mehdi
*
*/
public class DictionaryGenerator {
	
	public DictionaryGenerator() {
		
	}

   private static VirtGraph virtGraph = null;
   protected static final int interval = 100000;
   private static final int numOfEntities = 5050000;
   private static final String uriPrefix = "http://dbpedia.org/resource/";
   protected static final String wikiCategoryUriPrefix = "http://dbpedia.org/resource/Category:";
   private static final String yagoUriPrefix = "http://yago-knowledge.org/resource/";
	private static final int UNIQUE_ENTITY_NAMES = 10000000;
   protected static String DIR_PATH = "/home/mehdi/textCategorization/";
   private static int idGenerator = 0;
   private static List<String> localNameList = null;
//   private static List<Map<Integer, Entity_Old_Version>> dicContainer = null; 
   private static int longestWikipediaTitle = 53;
   private static final int maxNumOfAcceptableEntities = 5000;
   private static final Logger logger = Logger.getLogger(DictionaryGenerator.class.getName());
   private static final String [ ] wikipedia_internal_categories = {"wikipedia", "wikiprojects", "lists", "media wiki", "template", "user",
   															 "portal", "categories", "articles", "pages"};
   
   /**
    * @param args
    */
   public static void main(String[] args) {
//   	dicContainer = new ArrayList<Map<Integer, Entity>>();
//   	System.out.println("Connecting to Virtuoso ... ");
//   	virtGraph = connectToVirtuoso();
//   	System.out.println("Successfully Connected to Virtuoso!\n");
//   	mapEntityLocalNamesToInt();
//   	mapWordToInt();
//   	createPatternMatchingDictionary();
//   	loadDictionaryToMemory();
//   	recognizeWikipediaEntities();
//   	mapAllUniqueEntityNamesToEntityIds();
   	
   }
   
   /*
   public static void test() {
       System.out.println("Connecting to Virtuoso ... ");
       virtGraph = connectToVirtuoso();
       System.out.println("Successfully Connected to Virtuoso!\n");
       StringBuffer queryString = new StringBuffer();
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#>");
       queryString.append("SELECT distinct ?p ?o FROM <" + GRAPH + "> WHERE { ");
       queryString.append("<http://dbpedia.org/resource/Supply_chain> ?p ?o . ");
       queryString.append("filter (!isliteral(?o)) }");
       Query sparql = QueryFactory.create(queryString.toString());
       VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
       ResultSet results = vqe.execSelect();
       System.out.println("Retrieved the Results.");
       System.out.println("Iterating over the results...");
       QuerySolution result = null;
       RDFNode uri = null;
       //        int uriPrefixLength = uriPrefix.length();                                                                                                                   
       int i = 0;
       while (results.hasNext()) {
           result = results.nextSolution();
           i = results.getRowNumber();
           System.out.println(result.getResource("o").toString());
           System.out.println("rdf Node:"+result.get("o").toString());
           //System.out.println(result.getResource("o").toString() + "   " + result.getResource("o").getURI());                                                              
       } // end of while                                                                                                                                                     
       System.out.println("size:" +i);
   }
   */
   
   public static Map<String, Set<Entity>> loadUniqueNameListToMemory() {
		String fileName = DIR_PATH + "allUniqueNames.ser";
		System.out.println("Loading All Unique Names into Memory...");
       try {
           FileInputStream inputFile = new FileInputStream(fileName);
           BufferedInputStream bfin = new BufferedInputStream(inputFile);
           ObjectInputStream in = new ObjectInputStream(bfin);
           @SuppressWarnings("unchecked")
           Map<String, Set<Entity>> allNames =  (Map<String, Set<Entity>>) in.readObject();
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
   
   
   
   public static void mapAllUniqueEntityNamesToEntityIds() {
		LocalNameMapperClass lnMapperClass = loadLocalNameListToMemory();
   	Map<String, Integer> localNameMap  = lnMapperClass.getNameToInt();
//   	List<String> localNames            = lnMapperClass.getLocalNameList();
   	StringBuffer queryString = new StringBuffer();
   	Set<String> stopWords = new HashSet<String>(Arrays.asList(readStopWords().split(",")));
   	System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#>");
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
       Map<String, Set<Entity>> allNames = new HashMap<String, Set<Entity>>(UNIQUE_ENTITY_NAMES);
       Entity e = null;
       System.out.println("Iterating over the Results...");
       while (results.hasNext()) {
           QuerySolution result = results.nextSolution();
           numOfTriples = results.getRowNumber();
           if (numOfTriples % interval == 0) {
               System.out.println(numOfTriples + "  " + allNames.size());
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
           		e = new Entity(eid, '1');
           		if (allNames.containsKey(title)) {
           			allNames.get(title).add(e);
           		}else {
           			Set<Entity> elist = new HashSet<Entity>();
           			elist.add(e);
           			allNames.put(title, elist);
           		}
           	}
           } // end of if
           if (!stopWords.contains(redirectName)) {
           	if (isValid(redirectName)) {
           		e = new Entity(eid, '2');
           		if (allNames.containsKey(redirectName)) {
           			allNames.get(redirectName).add(e);
           		}else {
           			Set<Entity> elist = new HashSet<Entity>();
           			elist.add(e);
           			allNames.put(redirectName, elist);
           		}
           	}
           } // end of if
           if (!stopWords.contains(redirectNameShort)) {
           	if (isValid(redirectNameShort)) {
           		e = new Entity(eid, '3');
           		if (allNames.containsKey(redirectNameShort)) {
           			allNames.get(redirectNameShort).add(e);
           		}else {
           			Set<Entity> elist = new HashSet<Entity>();
           			elist.add(e);
           			allNames.put(redirectNameShort, elist);
           		}
           	}
           } // end of if
           if (!stopWords.contains(nameShort)) {
           	if (isValid(nameShort)) {
           		e = new Entity(eid, '4');
           		if (allNames.containsKey(nameShort)) {
           			allNames.get(nameShort).add(e); 
           		}else {
           			Set<Entity> elist = new HashSet<Entity>();
           			elist.add(e);
           			allNames.put(nameShort, elist);
           		}
           	}
           } // end of if
           if (!stopWords.contains(disambiguationName)) {
           	if (isValid(disambiguationName)) {
           		e = new Entity(eid, '5');
           		if (allNames.containsKey(disambiguationName)) {
           			allNames.get(disambiguationName).add(e);
           		}else {
           			Set<Entity> elist = new HashSet<Entity>();
           			elist.add(e);
           			allNames.put(disambiguationName, elist);
           		}
           	}
           } // end of if
           if (!stopWords.contains(disambiguationNameShort)) {
           	if (isValid(disambiguationNameShort)) {
           		e = new Entity(eid, '6');
           		if (allNames.containsKey(disambiguationNameShort)) {
           			allNames.get(disambiguationNameShort).add(e);
           		}else {
           			Set<Entity> elist = new HashSet<Entity>();
           			elist.add(e);
           			allNames.put(disambiguationNameShort, elist);
           		}
           	}
           } // end of if
       } // end of while
       System.out.println("Total number of unique names: " + allNames.size());
//       System.out.println("Please hit \"return\" to continue...");
//       Scanner sc = new Scanner(System.in);
//       sc.nextLine();
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
	public static void mapAllUniqueEntityNamesToEntityIds1() {
		LocalNameMapperClass lnMapperClass = loadLocalNameListToMemory();
   	Map<String, Integer> localNameMap  = lnMapperClass.getNameToInt();
//   	List<String> localNames            = lnMapperClass.getLocalNameList();
   	StringBuffer queryString = new StringBuffer();
   	Set<String> stopWords = new HashSet<String>(Arrays.asList(readStopWords().split(",")));
   	System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#>");
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
               System.out.println(numOfTriples + "  " + allNames.size());
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
//       System.out.println("Please hit \"return\" to continue...");
//       Scanner sc = new Scanner(System.in);
//       sc.nextLine();
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

	public static String readDocument() {
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
   }
   

	public static ArrayList<Integer> getIntersection(ArrayList<Integer> l1, ArrayList<Integer> l2) {
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
   
   
  

	public static LocalNameMapperClass loadLocalNameListToMemory() {
		String fileName = DIR_PATH + "entityLocalNames.ser";
		System.out.println("Loading Entities Local Names into Memory...");
       try {
           FileInputStream inputFile = new FileInputStream(fileName);
           BufferedInputStream bfin = new BufferedInputStream(inputFile);
           ObjectInputStream in = new ObjectInputStream(bfin);
           LocalNameMapperClass lnmapper =  (LocalNameMapperClass) in.readObject();
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

	public static WordToIntMapperClass loadwordToIntListToMemory() {
		String fileName = DIR_PATH + "wordToInt.ser";
		System.out.println("Loading Word To Integer Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
	    	ObjectInputStream in = new ObjectInputStream(bfin);
	    	WordToIntMapperClass wtoint =  (WordToIntMapperClass) in.readObject();
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

	public static void mapEntityLocalNamesToInt() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		LocalNameMapperClass lnMapper = new LocalNameMapperClass();
		Map<String, Integer> nToInt = lnMapper.getNameToInt();
       ArrayList<String> lNameList = lnMapper.getLocalNameList();
       StringBuffer queryString = new StringBuffer();
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#>");
       queryString.append("SELECT DISTINCT ?uri FROM <" + GRAPH + "> WHERE { ");
       queryString.append("?uri lsdis:wiki_name ?name .");
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
           nToInt.put(localName, numOfTriples - 1);  // because numOfTriples begins with 1
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

   public static void mapWordToInt() {
   	System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
   	WordToIntMapperClass wToIntMapper =  new WordToIntMapperClass();
       Map<String,Integer> wordToInt = wToIntMapper.getWordToInt();
       ArrayList<String> wlist       = wToIntMapper.getWordList();
       StringBuffer queryString = new StringBuffer();
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#>");
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
       RDFNode wikiname = null;
       RDFNode redirect_name = null;
       RDFNode redirect_name_short = null;
       RDFNode name_short = null;
       RDFNode disambiguation_name = null;
       RDFNode disambiguation_name_short = null;
       String title = "";
       String redirectName = "";
       String redirectNameShort = "";
       String nameShort = "";
       String disambiguationName = "";
       String disambiguationNameShort = "";
       int numOfTriples = 0;
       System.out.println("Iterating over the results...");
       while (results.hasNext()) {
           QuerySolution result = results.nextSolution();
           numOfTriples = results.getRowNumber();
           if (numOfTriples % interval == 0) {
               System.out.println(numOfTriples);
           }
           wikiname = result.get("name");
           redirect_name = result.get("redirect_name");
           redirect_name_short = result.get("redirect_name_short");
           name_short = result.get("name_short");
           disambiguation_name = result.get("disambiguation_name");
           disambiguation_name_short = result.get("disambiguation_name_short");
           title = wikiname.toString().toLowerCase();
           redirectName = redirect_name != null ? redirect_name.toString().toLowerCase() : " ";
           redirectNameShort = redirect_name_short != null ? redirect_name_short.toString().toLowerCase() : " ";
           nameShort = name_short != null ? name_short.toString().toLowerCase() : " ";
           disambiguationName = disambiguation_name != null ? disambiguation_name.toString().toLowerCase() : " ";
           disambiguationNameShort = disambiguation_name_short != null ? disambiguation_name_short.toString().toLowerCase() : " ";
           fillInVocabulary(wordToInt, wlist, title);
           if (!redirectName.equals(" ")) {
               fillInVocabulary(wordToInt, wlist, redirectName);
           }
           if (!redirectNameShort.equals(" ")) {
               fillInVocabulary(wordToInt, wlist, redirectNameShort);
           }
           if (!nameShort.equals(" ")) {
               fillInVocabulary(wordToInt, wlist, nameShort);
           }
           if (!disambiguationName.equals(" ")) {
               fillInVocabulary(wordToInt, wlist, disambiguationName);
           }
           if (!disambiguationNameShort.equals(" ")) {
               fillInVocabulary(wordToInt, wlist, disambiguationNameShort);
           }
       } // end of while
       wlist.trimToSize();
       System.out.println("Number of Triples: " + numOfTriples);
       System.out.println("wtoint size: " + wordToInt.size() + "  wlist size: " + wlist.size());
       String fileName = DIR_PATH + "wordToInt.ser";
//       String[] wToInt = new ArrayList<String>(wordToIntMapper.keySet()).toArray(new String[wordToIntMapper.size()]);
       try {
           FileOutputStream outputFile = new FileOutputStream(fileName);
           BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
           ObjectOutputStream out = new ObjectOutputStream(bfout);
           out.writeObject(wToIntMapper);
           out.close();
           System.out.println("Word To Int Serialized successfully.");
//           fileName = "/home/mehdi/wtoint2.ser";
//           outputFile = new FileOutputStream(fileName);
//           bfout = new BufferedOutputStream(outputFile);
//           out = new ObjectOutputStream(bfout);
//           out.writeObject(wordToIntMapper);
//           out.close();
//           System.out.println("Word To Int2 Serialized successfully.");
       } catch (FileNotFoundException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       } //catch (ClassNotFoundException e) {
          // e.printStackTrace();
       //}
   } // end of mapWordToInt
   
   
   public static void persistWikipediaEntitiesIncomingLinks() {
		LocalNameMapperClass localNamesMapperClass = loadLocalNameListToMemory();
		Map<String, Integer> namesToInt = localNamesMapperClass.getNameToInt();
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		int uriPrefixLength = uriPrefix.length();
		EntityManagerImpl entityManager =  new EntityManagerImpl();
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
			queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
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
   
   
   public void createWikipediaTaxanomyMapFromParentToChild() {
	   CategoryNameMapper catNameMapper = loadCatNameListToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<Integer, Set<Integer>> wikiTaxanomy = new HashMap<Integer, Set<Integer>>(2100000);
       StringBuffer queryString = new StringBuffer();
//     queryString.append("SELECT ?s ?sc FROM <" + GRAPH + "> WHERE { ");
//     queryString.append("?s a <http://www.w3.org/2000/01/rdf-schema#Class> . ");
//     queryString.append("?sc lsdis:wiki_category_parent ?s . ");
       
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
           		subClassId   = catNamesToInt.get(subClass);
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
       System.out.println("number of wiki parents: " + wikiTaxanomy.size());   // # 416622
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
	   queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
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
           		subClassId   = catNamesToInt.get(subClass);
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
       System.out.println("Wiki Taxanomy Size: " + wikiTaxanomy.size());  // # 786825
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
   
   
   public static Map<Integer,Set<Integer>> loadWikiTaxonomyMap_ID_FromChildToParentToMemory() {
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
   
   
   
   
   
   public static void createWikipediaTaxanomyMapFromChildToParent() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<String, Set<String>> wikiTaxanomy = new HashMap<String, Set<String>>(2100000);
       StringBuffer queryString = new StringBuffer();
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
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
   
   public static Map<String,Set<String>> loadWikiTaxonomyMapFromChildToParentToMemory() {
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
   
   
   public static void mapCategoryNamesToInt() {
		System.out.println("Connecting to Virtuoso ... ");
		virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
		CategoryNameMapper catNameMapper = new CategoryNameMapper();
		Map<String, Integer> catNameToInt = catNameMapper.getNameToInt();
       ArrayList<String> catNameList = catNameMapper.getCatNameList();
       StringBuffer queryString = new StringBuffer();
       queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
       queryString.append("SELECT ?cat FROM <" + GRAPH + "> WHERE { ");
       queryString.append("?cat a <http://www.w3.org/2000/01/rdf-schema#Class> .");
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
       catNameList.trimToSize();  // total number of categories: 930472
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
   public static boolean isInternalCategory(String category) {
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
           CategoryNameMapper catNameMapper =  (CategoryNameMapper) in.readObject();
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
	
   
   
   public static Map<String,String> loadYagoTaxonomyMapToMemory() {
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
   
   
   public static void createYagoTaxanomyMap() {
		System.out.println("Connecting to Virtuoso ... ");
		String yagoGraph = "http://yago.com#";
		VirtGraph yagoVirtGraph = connectToVirtuoso(yagoGraph);
		System.out.println("Successfully Connected to Virtuoso!\n");
		Map<String, String> yagoTaxanomy = new HashMap<String, String>(451000);
       StringBuffer queryString = new StringBuffer();
       queryString.append("SELECT DISTINCT ?s ?o FROM <" + yagoGraph + "> WHERE { ");
       queryString.append("?s ?p ?o . ");
       queryString.append("FILTER regex (?o,\"http://yago-knowledge.org\", \"i\") ");
//       queryString.append("FILTER NOT EXISTS { ?s ?p <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> . } ");
//       queryString.append("FILTER NOT EXISTS { ?s ?p <http://www.w3.org/2002/07/owl#Thing> . } ");
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
   
   
   
   public static void fillInVocabulary(Map<String,Integer> vocabulary, ArrayList<String> wlist, String name) {
       if (isValid(name)) {
           name = name.replace("'s", "").replace("'S", "").replace("'t", "").replace("'", "").replace(",", "").replace("?", "").replace("!", "");
           String[] tokens = name.split(" ");
           for (int wordCnt = 0; wordCnt < tokens.length; wordCnt++) {
               if (!vocabulary.containsKey(tokens [wordCnt])) {
                   vocabulary.put(tokens [wordCnt], idGenerator);
                   wlist.add(tokens [wordCnt]);
                   idGenerator++;
               } // end of if
           } // end of for
       } // end of if

   } // end of fillInVocabulary

   public static boolean isValid(String name) {
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
       try {
           if (NumberFormat.getInstance().parse(name) != null) {
               return false;
           }
       } catch (ParseException e) {
           return true;
       }
       return true;
   } // end of isValid
   
   public static String readStopWords() {
   	String filename = "/home/mehdi/stopwords.txt";
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
   
}
