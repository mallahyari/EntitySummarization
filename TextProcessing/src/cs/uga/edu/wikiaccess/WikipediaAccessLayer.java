/**
 * 
 */
package cs.uga.edu.wikiaccess;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.GRAPH;
import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

import cs.uga.edu.dicgenerator.CategoryNameMapper;
import cs.uga.edu.dicgenerator.VirtuosoAccess;
import cs.uga.edu.properties.Configuration;

/**
 * @author Mehdi
 *
 */
public class WikipediaAccessLayer {

	
	private final Logger logger = LogManager.getLogger(WikipediaAccessLayer.class.getName());
	private final String DIR_PATH = Configuration.getProperty("serializedFilesPath");
	private final String WIKI_PAGE_DIR_PATH = Configuration.getProperty("wikipediaPagesPath");
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String wikiCategoryUriPrefix = "http://dbpedia.org/resource/Category:";
	private final int interval = 100000;
	private final String [ ] wikipedia_internal_categories = {"wikipedia", "wikiprojects", "lists", "media wiki", "template", "user",
		 "portal", "categories", "articles", "pages"};
	private Set<Integer> categories = null;
	private Map<Integer, String> categoryIdNameLookupTable = null;   // from categoryId to categoryName
	private Map<String, Integer> categoryNameIdLookupTable = null;   // from categoryName to categoryId
	private Map<Integer, Set<String>> categoryConceptLookupTable = null; // from categoryId to list of concepts
	private VirtGraph virtGraph;
	
	
	public WikipediaAccessLayer() {
		categoryIdNameLookupTable = new HashMap<Integer, String>();
		categoryNameIdLookupTable = new HashMap<String, Integer>();
		categoryConceptLookupTable = new HashMap<Integer, Set<String>>();
	}
	
	public void createTrainAndTestSets2() {
		String dirPath = "/home/mehdi/topicdiscovery/tsfolder/";
		Map<Integer,Set<String>> catToConMap = loadCategoryToConceptMapToMemory();
		Set<Integer> keyset = catToConMap.keySet();
		Set<String> initialCorpus = new HashSet<String>();
		Set<String> testset = new HashSet<String>();
		Set<String> trainingset = new HashSet<String>();
		Random random = new Random();
		
		// create an initial corpus of 1353 * 5 = 6765 articles
		for (int cid : keyset) {
			Object[] articles = catToConMap.get(cid).toArray();
			Set<Integer> duplicate = new HashSet<Integer>();
			int ctr = 1;
			while(ctr <= 5) {
				int arIndex = random.nextInt(articles.length);
				String article = String.valueOf(articles[arIndex]);
				if (!duplicate.contains(arIndex)) {
					duplicate.add(arIndex);
					initialCorpus.add(article);
					ctr++;
				} // end of if
			} // end of while
//			System.out.println(initialCorpus.size());
		} // end of for
		
		System.out.println("Size of initial corpus: " + initialCorpus.size());
		
		// divide the set to 80% training set and 20% test set
		int nDocs = initialCorpus.size();
		Object[] articlearr = initialCorpus.toArray();
		int testsize = (nDocs * 20) / 100;
		int trainsize = nDocs - testsize;
		System.out.println("Traning set size: " + trainsize + " Test set size: " + testsize);
		
		// construct the test set (file containing article names)
		for (int i = 0; i < testsize; i++) {
			int arIndex = random.nextInt(nDocs);
			String arname = String.valueOf(articlearr[arIndex]);
			testset.add(arname);
		} // end of for
		FileWriter fw;
		try {
			fw = new FileWriter(dirPath + "test.txt");
			String line = testset.toString().replace("[", "").replace("]", "");
			fw.write(line);
			fw.close();
			
			// construct the training set (file containing article names)
			for (int i = 0; i < articlearr.length; i++) {
				String arname = String.valueOf(articlearr[i]);
				if (!testset.contains(arname)) {
					trainingset.add(arname);
				}
			} // end of for
			dirPath = "/home/mehdi/topicdiscovery/trfolder/";
			fw = new FileWriter(dirPath + "training.txt");
			line = trainingset.toString().replace("[", "").replace("]", "");
			fw.write(line);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	} // end of createTrainAndTestSets2
	
	
	
	public void createTrainAndTestSets1() {
		String dirPath = "/home/mehdi/topicdiscovery/tsfolder/";
		Map<Integer,Set<String>> catToConMap = loadCategoryToConceptMapToMemory();
		Set<Integer> keyset = catToConMap.keySet();
		Set<String> allarticles = new HashSet<String>();
		Set<String> testset = new HashSet<String>();
		Set<String> trainingset = new HashSet<String>();
		for (int cid : keyset) {
			allarticles.addAll(catToConMap.get(cid));
		}
		System.out.println("Total number of articles: " + allarticles.size());
		
		// divide the set to 90% training set and 10% test set
		int nDocs = allarticles.size();
		Object[] articlearr = allarticles.toArray();
		int testsize = (nDocs * 10) / 100;
		int trainsize = nDocs - testsize;
		System.out.println("Traning set size: " + trainsize + " Test set size: " + testsize);
		Random random = new Random();
		
		// construct the test set (file containing article names)
		for (int i = 0; i < testsize; i++) {
			int arIndex = random.nextInt(nDocs);
			String arname = String.valueOf(articlearr[arIndex]);
			testset.add(arname);
		} // end of for
		FileWriter fw;
		try {
			fw = new FileWriter(dirPath + "test.txt");
			String line = testset.toString().replace("[", "").replace("]", "");
			fw.write(line);
			fw.close();
			
			// construct the training set (file containing article names)
			for (int i = 0; i < articlearr.length; i++) {
				String arname = String.valueOf(articlearr[i]);
				if (!testset.contains(arname)) {
					trainingset.add(arname);
				}
			} // end of for
			dirPath = "/home/mehdi/topicdiscovery/trfolder/";
			fw = new FileWriter(dirPath + "training.txt");
			line = trainingset.toString().replace("[", "").replace("]", "");
			fw.write(line);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	} // end of createTrainAndTestSets1
	
	public void cleanCategoriesAndConcepts() {
		Map<Integer,Set<String>> initialcatToConMap = loadCategoryToConceptMapToMemory();
		System.out.println("initial size of the catToconceptMap: " + initialcatToConMap.size());
		Map<Integer,Set<String>> finalcatToConMap = new HashMap<Integer,Set<String>>();
		Set<Integer> keyset = initialcatToConMap.keySet();
		for (int cid : keyset) {
			if (initialcatToConMap.get(cid) == null) {
				continue;
			}
			if (initialcatToConMap.get(cid).size() == 0) {
				continue;
			}
			Set<String> articleset = initialcatToConMap.get(cid);
			Set<String> finalarticleset = new HashSet<String>();
			for (String ar : articleset) {
				ar = ar.trim();
				if (ar.indexOf('?') != -1 || ar.indexOf('/') != -1 || ar.indexOf('%') != -1 || ar.indexOf("_(disambiguation") != -1 || ar.indexOf('\'') != -1 || ar.indexOf(":") != -1 || ar.indexOf('\"') != -1 || ar.indexOf('^') != -1 || ar.startsWith("_") || ar.startsWith(".") || ar.indexOf("_(number") != -1) {
					continue;
				}
				finalarticleset.add(ar);
			}
			if (finalarticleset.size() >= 5) {
				finalcatToConMap.put(cid, finalarticleset);
			}
		} // end of for
		System.out.println("final size of the catToconceptMap: " + finalcatToConMap.size());
		serializeCatToConceptMapFile(finalcatToConMap);
	} // end of cleanCategoriesAndConcepts
	
	public Map<Integer,Set<String>> loadCategoryToConceptMapToMemory() {
		String fileName = DIR_PATH + "catToConceptMap.ser";
		System.out.println("Loading Category to Concept Map into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			@SuppressWarnings("unchecked")
			Map<Integer,Set<String>> wikiTaxonomyMap = (HashMap<Integer,Set<String>>) in.readObject();
			in.close();
			System.out.println("Category to Concept Map Loaded successfully into Memory.\n");
			return wikiTaxonomyMap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	} // end of 
	
	public void serializeCatToConceptMapFile(Map<Integer, Set<String>> catToConcept) {
		String fileName = DIR_PATH + "catToConceptMap.ser";
		try {
			System.out.println("Serializing Category to Concept Map...");
			File f = new File(fileName);
			if (f.exists()) {
				System.out.print("catToConceptMap.ser file already exists, deleting the file...");
				f.delete();
				System.out.println("done!");
			}
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(catToConcept);
			out.close();
			System.out.println("Category to Concept Map Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void createCategoryToConceptMap(int level) {
		Map<Integer, Set<String>> catToConcept = getCategoryConcepts(level);
		serializeCatToConceptMapFile(catToConcept);
	} // end of createCategoryToConceptMap
	
	public Map<Integer,Set<String>> getCategoryConcepts(int level) {
		CategoryNameMapper wikipediaCategories = loadWikipediaCategoryNames();
		Map<Integer,Set<Integer>> wikiHierarchy = loadWikipediaHierarchy();
		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		virtGraph = VirtuosoAccess.connectToVirtuoso();
//		String [] catNames = {"Business","Applied_sciences","Sports","Politics","Health"};
		String [] catNames = {"Business","Applied_sciences","Health"};
		Map<Integer, Set<String>> catToConcept = new HashMap<Integer, Set<String>>();
		for (String catName : catNames) {
//		String catName = "Politics";
			int catId = catNamesToInt.get(catName);
			Set<Integer> mainCategories = new HashSet<Integer> ();
			categories = new HashSet<Integer> ();
			categories.add(catId);
			categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
			System.out.println("number of categories: " + categories.size());
			int i = 0;
			for (int cid : categories) {
				mainCategories = new HashSet<Integer> ();
				Set<Integer> subcats = findCategoryDescendantsByCategoryId(cid, mainCategories, wikiHierarchy, level);
				Set<String> conceptList = new HashSet<String>();
				for (int scid : subcats) {
					if (categories.contains(scid)) {
						HashSet<Integer> temp = new HashSet<Integer>();
						temp.add(scid);
						conceptList.addAll(findCategoryConcepts(temp, wikipediaCategoryNames));
					}
				} // end of for
				i++;
				if (i % 1000 == 0) System.out.println(i);
//				System.out.println("conceptList size: " + conceptList.size());
				catToConcept.put(cid, conceptList);
			} // end of for
			System.out.println("size of catToConcept map: " + catToConcept.size());
		} // end of for
		virtGraph.close();
		return catToConcept;
	}
	
	public Set<String> getPopulatedConcepts(int level) {
		CategoryNameMapper wikipediaCategories = loadWikipediaCategoryNames();
		Map<Integer,Set<Integer>> wikiHierarchy = loadWikipediaHierarchy();
		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		String catName = "Politics";
//		String catName = "Applied_sciences";
		int catId = catNamesToInt.get(catName);
//		Set<Integer> mainCategories = wikiHierarchy.get(catId);
		Set<Integer> mainCategories = new HashSet<Integer> ();
		categories = new HashSet<Integer> ();
		categories.add(catId);
		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
		Set<String> conceptList = findCategoryConcepts(categories, wikipediaCategoryNames);
//		catName = "Behavioural_sciences";
		catName = "Sports_by_type";
		catId = catNamesToInt.get(catName);
		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Natural_sciences";
		catName = "Scientific_disciplines";
		catId = catNamesToInt.get(catName);
		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
		catName = "Business";
		catId = catNamesToInt.get(catName);
		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
		
		
//		logger.info("Total number of Concepts under \"" + catName + "\" :" + conceptList.size());
		logger.info("Total number of Concepts :" + conceptList.size());
		return conceptList;
	} // end of getPopulatedConcepts
	
	
	
//	public Set<String> getPopulatedConcepts(int level) {
//		CategoryNameMapper wikipediaCategories = loadWikipediaCategoryNames();
//		Map<Integer,Set<Integer>> wikiHierarchy = loadWikipediaHierarchy();
//		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
//		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
//		String catName = "Artificial_intelligence";
//		int catId = catNamesToInt.get(catName);
////		Set<Integer> mainCategories = wikiHierarchy.get(catId);
//		Set<Integer> mainCategories = new HashSet<Integer> ();
//		categories = new HashSet<Integer> ();
//		categories.add(catId);
////		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		Set<String> conceptList = findCategoryConcepts(categories, wikipediaCategoryNames);
//		catName = "Knowledge_engineering";
//		catId = catNamesToInt.get(catName);
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Knowledge_representation";
//		catId = catNamesToInt.get(catName);
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Machine_learning";
//		catId = catNamesToInt.get(catName);
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Logic_programming";
//		catId = catNamesToInt.get(catName);
//		categories.add(catId);
////		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		
//		catName = "Data_mining";
//		catId = catNamesToInt.get(catName);
////		mainCategories = wikiHierarchy.get(catId);
////		mainCategories.addAll(wikiHierarchy.get(catId));
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		
//		catName = "Information_retrieval";
//		catId = catNamesToInt.get(catName);
////		mainCategories = wikiHierarchy.get(catId);
////		mainCategories.addAll(wikiHierarchy.get(catId));
////		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		categories.add(catId);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Data_management";
//		catId = catNamesToInt.get(catName);
////		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		categories.add(catId);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Substring_indices";
//		catId = catNamesToInt.get(catName);
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Vector_space_model";
//		catId = catNamesToInt.get(catName);
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Personalized_search";
//		catId = catNamesToInt.get(catName);
////		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		categories.add(catId);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "Searching";
//		catId = catNamesToInt.get(catName);
////		mainCategories = wikiHierarchy.get(catId);
////		mainCategories.addAll(wikiHierarchy.get(catId));
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		catName = "String_similarity_measures";
//		catId = catNamesToInt.get(catName);
////		mainCategories = wikiHierarchy.get(catId);
////		mainCategories.addAll(wikiHierarchy.get(catId));
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		
//		catName = "Database_management_systems";
//		catId = catNamesToInt.get(catName);
////		mainCategories = wikiHierarchy.get(catId);
////		mainCategories.addAll(wikiHierarchy.get(catId));
//		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy, level);
//		conceptList.addAll(findCategoryConcepts(categories, wikipediaCategoryNames));
//		
//		
////		logger.info("Total number of Concepts under \"" + catName + "\" :" + conceptList.size());
//		logger.info("Total number of Concepts :" + conceptList.size());
//		return conceptList;
//	} // end of getPopulatedConcepts
	
	public void populateCategoriesAndConcepts() {
		/*
		CategoryNameMapper wikipediaCategories = loadWikipediaCategoryNames();
		Map<Integer,Set<Integer>> wikiHierarchy = loadWikipediaHierarchy();
		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		String catName = "Main_topic_classifications";
		int catId = catNamesToInt.get(catName);
		Set<Integer> mainCategories = wikiHierarchy.get(catId);
		catName = "Computer_science";
		catId = catNamesToInt.get(catName);
		categories = findCategoryDescendantsByCategoryId(catId, mainCategories, wikiHierarchy);
		Set<String> conceptList = findCategoryConcepts(categories, wikipediaCategoryNames);
//		saveCategories();
		saveCategoryIdNameLookupTable();
		saveCategoryNameIdLookupTable();
		saveCategoryConceptLookupTable();
//		downloadWikipediaPagesForConcepts(conceptList);
 */
		Set<String> conceptList = getConceptListFromFile();
		downloadWikipediaPagesForConcepts(conceptList);
		
	} // end of populateCategories
	
	public Set<String> getConceptListFromFile() {
		String dirPath = Configuration.getProperty("dataDirPath");
//		String dirPath = "/Users/Mehdi/corpus/";
		File directory = new File(dirPath);
		Set<String> conceptList = new HashSet<String>();
		for (File file : directory.listFiles()) {
			String fileName = file.getName();
			try {
				FileReader fr = new FileReader(dirPath + fileName);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while ((line = br.readLine()) != null) {
					String [ ] c = line.split(",");
					conceptList.addAll(Arrays.asList(c));
				} // end of while
				br.close();
				fr.close();
			} catch (FileNotFoundException e) {
				logger.error("\"" + fileName + "\" does NOT exits. Please enter a valid file name.");
//				e.printStackTrace();
			} catch (IOException e) {
//				e.printStackTrace();
			}
		} // end of for (file)
		logger.info("concepts size: " + conceptList.size());
		return conceptList;
	}
	
	
	public Map<Integer, Set<String>> loadCategoryConceptLookupTable() {
		String fileName = DIR_PATH + "categoryConcept.ser";
		System.out.println("Loading CategoryConceptLookupTable into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
            Map<Integer, Set<String>> catConceptLookupTable = (Map<Integer, Set<String>>) in.readObject();
            in.close();
            System.out.println("CategoryConceptLookupTable Successfuly Loaded into Memory.\n");
			return catConceptLookupTable;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadCategoryConceptLookupTable
	
	
	public Map<String, Integer> loadCategoryNameIdLookupTable() {
		String fileName = DIR_PATH + "categoryNameId.ser";
		System.out.println("Loading CategoryNameIdLookupTable into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
            Map<String, Integer> catNameIdLookupTable = (Map<String, Integer>) in.readObject();
            in.close();
            System.out.println("CategoryNameIdLookupTable Successfuly Loaded into Memory.\n");
			return catNameIdLookupTable;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadCategoryNameIdLookupTable
	
	
	public Map<Integer, String> loadCategoryIdNameLookupTable() {
		String fileName = DIR_PATH + "categoryIdName.ser";
		System.out.println("Loading CategoryIdNameLookupTable into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
            Map<Integer, String> catIdNameLookupTable = (Map<Integer, String>) in.readObject();
            in.close();
            System.out.println("CategoryIdNameLookupTable Successfuly Loaded into Memory.\n");
			return catIdNameLookupTable;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadCategoryIdNameLookupTable
	
	public Set<Integer> loadCategories() {
		String fileName = DIR_PATH + "categories.ser";
		System.out.println("Loading categories into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
			Set<Integer> cats = (Set<Integer>) in.readObject();
            in.close();
            System.out.println("Categories Successfuly Loaded into Memory.\n");
			return cats;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadCategories
	
	
	public void saveCategories() {
		System.out.println("Serializing Categories...");
        String fileName = DIR_PATH + "categories.ser";
        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(categories);
            out.close();
            System.out.println("Categories Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveCategories
	
	
	public void saveCategoryNameIdLookupTable() {
		System.out.println("Serializing CategoryNameIdLookupTable...");
        String fileName = DIR_PATH + "categoryNameId.ser";
        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(categoryNameIdLookupTable);
            out.close();
            System.out.println("CategoryNameIdLookupTable Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveCategoryNameIdLookupTable
	
	
	
	public void saveCategoryIdNameLookupTable() {
		System.out.println("Serializing CategoryIdNameLookupTable...");
        String fileName = DIR_PATH + "categoryIdName.ser";
        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(categoryIdNameLookupTable);
            out.close();
            System.out.println("CategoryIdNameLookupTable Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveCategoryIdNameLookupTable
	
	public void saveCategoryConceptLookupTable() {
		System.out.println("Serializing CategoryConceptLookupTable...");
        String fileName = DIR_PATH + "categoryConcept.ser";
        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(categoryConceptLookupTable);
            out.close();
            System.out.println("CategoryConceptLookupTable Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveCategoryConceptLookupTable
	
	
	public Set<String> findCategoryConcepts(Set<Integer> categories, List<String> wikipediaCategoryNames) {
//		VirtGraph virtGraph = VirtuosoAccess.connectToVirtuoso();
		Set<String> entities = new HashSet<String>();
//		System.out.println("Number of categories: " + categories.size());  //// Number of Descendants: 1204 for Computer Science ////
		logger.info("number of Categories: " + categories.size());
		for (int cid : categories) {
			String cname = wikipediaCategoryNames.get(cid);
			if (cname.contains("History") || cname.contains("Scientists") || cname.contains("_by_")) {
				continue;
			}
			Set<String> concepts = findEntitiesbyCategoryName(cname, virtGraph);
			// disregard categories having less than 5 and more than 2000 articles
			if (concepts.size() < 5 || concepts.size() > 2000) {
				continue;
			}
			entities.addAll(concepts);
//			logger.info("Category " + id);
		} // end of for
//		System.out.println("Total Number of Entities: " + entities.size());  //// Number of Concepts under Computer Science sub-graph: 44341 ////
//		virtGraph.close();
		return entities;
	} // end of findCategoryConcepts
	
	
	public CategoryNameMapper loadWikipediaCategoryNames() {
		return loadCatNameListToMemory();
	} // end of loadWikipediaCategoryNames
	
	
	public Map<Integer,Set<Integer>> loadWikipediaHierarchy() {
		return loadWikiTaxonomyMapFromParentToChildToMemory();
	} // end of loadWikipediaHierarchy
	
	
	public Set<Integer> findCategoryDescendantsByCategoryId(int catId, Set<Integer> mainCategories, Map<Integer, Set<Integer>> wikiHierarchy, int level) {
		return getCategoryDescendants(mainCategories, wikiHierarchy, catId, level);
	} // end of findCategoryDescendantsByCategoryId
	
	
	public void downloadWikipediaPagesForConcepts(Set<String> entities) {
		logger.info("number of entities: " + entities.size());
		String wiki_URL = "http://en.wikipedia.org/wiki/";
		Document document = null;
		Connection conn = null;
		for (String entity : entities) {
			try {
				conn = Jsoup.connect(wiki_URL + entity.trim());
				document = conn.get();
				Elements content = document.select("#mw-content-text p");
				//				System.out.println(content.text());
				File file = new File(WIKI_PAGE_DIR_PATH + entity.trim() + ".txt");
//				File file = new File("/Users/Mehdi/w/" + entity + ".txt");
				FileWriter fileWriter = new FileWriter(file);
				fileWriter.write(content.text());
				fileWriter.flush();
				fileWriter.close();
				conn = null;
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		} // end of for
	} // end of downloadWikipediaPagesForConcepts
	
	
	
	public Set<String> findCategoriesByConceptName(Set<String> wikiConcepts) {
//		System.out.println("con: " + wikiConcepts);
		VirtGraph virtGraph = connectToVirtuoso();
		int categoryUriPrefixLength = wikiCategoryUriPrefix.length();
		Set<String> categoryList = new HashSet<String>();
		for (String concept : wikiConcepts) {
			StringBuilder queryString = new StringBuilder(400);
			if (concept.indexOf('"') == -1) {
				queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
				queryString.append("SELECT DISTINCT ?p ?o FROM <" + GRAPH + "> WHERE { ");
				queryString.append("<" + uriPrefix + concept + ">" + " ?p ?o . ");
				queryString.append("FILTER (!isliteral(?o)) }");
				com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
				VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
				ResultSet results = vqe.execSelect();
//				 System.out.println("Retrieved the Results.");
//				 System.out.println("Iterating over the Results...");
				while (results.hasNext()) {
					QuerySolution result = results.nextSolution();
					String predicate = result.getResource("p").toString();
					if (predicate.contains("rdf-syntax-ns#type")) {
						String category = result.getResource("o").toString();
						int index = category.indexOf("/Category:");
						if (index != -1) {
							category = category.substring(categoryUriPrefixLength);
							categoryList.add(category);
						} // end of if
					} // end of if
				} // end of while
				vqe.close();
			} // end of if (concept.indexOf('"')
		} // end of for Map
		return categoryList;
	} // end of findCategoriesByConceptName

	
	
	
	public Set<String> findEntitiesbyCategoryName(String categoryName, VirtGraph virtGraph) {
		Set<String> relations = new HashSet<String>();
		VirtuosoQueryExecution vqe = null;
		try {
			StringBuffer queryString = new StringBuffer();
			queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
			queryString.append(" ?s <http://purl.org/dc/terms/subject> <" + wikiCategoryUriPrefix + categoryName + "> . ");
			queryString.append("}");
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			//		System.out.println("QUERY: " + queryString.toString());
			vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			// System.out.println("Retrieved the Results.");
			// System.out.println("Iterating over the Results...");
			//		Set<Integer> relations = new HashSet<Integer>();
			int uriPrefixLength = uriPrefix.length();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
//				String localname = result.getResource("s").toString().substring(uriPrefixLength).toLowerCase();
				String localname = result.getResource("s").toString().substring(uriPrefixLength);
				//			relations.add(namesToInt.get(localname));
				relations.add(localname);
			} // end of while
		}catch(Exception e) {
			logger.error(categoryName + " " + e.getMessage());
		}finally {
			vqe.close();
		}
		return relations;
	} // end of findEntitiesbyCategoryName
	
	
	
	/**
	 * Load the entire Wikipedia Category Network into memory.
	 * @return Wikipedia Category Map<superCategoryId, subCategorySet>
	 */
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
	
	
	public Set<Integer> getCategoryDescendants(Set<Integer> mainCategories, Map<Integer, Set<Integer>> wikiTaxonomyMap, int catId, int level) {
		int hierarchylevel = 1;
		Set<Integer> children = new HashSet<Integer>();
		Set<Integer> catLoop = new HashSet<Integer>();
		catLoop = mainCategories;
		Set<Integer> resultList = new HashSet<Integer>();
		resultList.add(catId);
		children = wikiTaxonomyMap.get(catId);
		if (children == null) {
			return resultList;
		}
		while(!children.isEmpty() && hierarchylevel <= level) {
			Set<Integer> nextLevelCats = new HashSet<Integer>();
			for (Integer child : children) {
				if (catLoop.contains(child)) {
					continue;
				}else {
					catLoop.add(child);
					resultList.add(child);
					if (wikiTaxonomyMap.get(child) != null) {
						nextLevelCats.addAll(wikiTaxonomyMap.get(child));
					} // end of if
				} // end of if
			} // end of for
//			System.out.println("===========================> " + hierarchylevel + "  size:   " + nextLevelCats.size());
			children = nextLevelCats;
			hierarchylevel++;
			logger.info(hierarchylevel);
		} // end of while
		return resultList;
	} // end of getCategoryDescendants
	
	
	
	/**
	 * Construct the set of all sub-categories and descendants for a category.
	 * @param wikiTaxonomyMap
	 * @param sid
	 * @param catLoopMap
	 * @return
	 */
	public Set<Integer> getCategoryDescendants(List<String> cnames, Set<Integer> mainCategories, Map<Integer, Set<Integer>> wikiTaxonomyMap, int catId) {
		int hierarchylevel = 1;
		Set<Integer> children = new HashSet<Integer>();
		Set<Integer> catLoop = new HashSet<Integer>();
		catLoop = mainCategories;
		Set<Integer> resultList = new HashSet<Integer>();
		resultList.add(catId);
		children = wikiTaxonomyMap.get(catId);
		while(!children.isEmpty() && hierarchylevel < 5) {
			Set<Integer> nextLevelCats = new HashSet<Integer>();
			for (Integer child : children) {
				if (catLoop.contains(child)) {
					continue;
				}else {
					catLoop.add(child);
					resultList.add(child);
					if (wikiTaxonomyMap.get(child) != null) {
//						System.out.println(cnames.get(child) + "  " + wikiTaxonomyMap.get(child).size());
						nextLevelCats.addAll(wikiTaxonomyMap.get(child));
					} // end of if
				} // end of if
			} // end of for
//			System.out.println("===========================> " + hierarchylevel + "  size:   " + nextLevelCats.size());
			children = nextLevelCats;
			hierarchylevel++;
			logger.info(hierarchylevel);
		} // end of while
		return resultList;
	} // end of getCategoryDescendants
	

	public Set<Integer> getCategoryDescendants(Map<Integer, Integer> categoryLevel, Map<Integer, Set<Integer>> wikiTaxonomyMap, int catId) {
		int hierarchylevel = categoryLevel.get(catId);
		Set<Integer> children = new HashSet<Integer>();
		Set<Integer> resultList = new HashSet<Integer>();
		resultList.add(catId);
		children = wikiTaxonomyMap.get(catId);
		while(true && !children.isEmpty()) {
			Set<Integer> nextLevelCats = new HashSet<Integer>();
			for (Integer child : children) {
				if (categoryLevel.containsKey(child)) {
					if (categoryLevel.get(child) == (hierarchylevel + 1)) {
						resultList.add(child);
						if (wikiTaxonomyMap.get(child) != null) {
							nextLevelCats.addAll(wikiTaxonomyMap.get(child));
						} // end of if
					} // end of if
				} // end of if
			} // end of for
//			System.out.println(hierarchylevel);
			children = nextLevelCats;
			hierarchylevel++;
			logger.info(hierarchylevel);
		} // end of while
		return resultList;
	} // end of getCategoryDescendants
	
	public Set<Integer> getCategoryDescendants(List<String> cnames, Map<Integer, Integer> categoryLevel, Map<Integer, Set<Integer>> wikiTaxonomyMap, int catId) {
		int hierarchylevel = categoryLevel.get(catId);
		Set<Integer> children = new HashSet<Integer>();
		Set<Integer> resultList = new HashSet<Integer>();
		resultList.add(catId);
		children = wikiTaxonomyMap.get(catId);
		while(true && !children.isEmpty()) {
			Set<Integer> nextLevelCats = new HashSet<Integer>();
			for (Integer child : children) {
				if (categoryLevel.containsKey(child)) {
					if (categoryLevel.get(child) == (hierarchylevel + 1)) {
						resultList.add(child);
						System.out.println(cnames.get(child));
						if (wikiTaxonomyMap.get(child) != null) {
							nextLevelCats.addAll(wikiTaxonomyMap.get(child));
						} // end of if
					} // end of if
				} // end of if
			} // end of for
//			System.out.println(hierarchylevel);
			children = nextLevelCats;
			hierarchylevel++;
			logger.info(hierarchylevel);
		} // end of while
		return resultList;
	} // end of getCategoryDescendants
	
	public void setHierarchyLevelForAllCategories(List<String> cnames,Map<Integer, Set<Integer>> wikiTaxonomyMap, int rootId) {
		Map<Integer, Integer> categoryLevel = new HashMap<Integer, Integer>();
		int hierarchylevel = 0;
		categoryLevel.put(rootId, hierarchylevel);
		Set<Integer> children = new HashSet<Integer>();
		hierarchylevel++;
		children = wikiTaxonomyMap.get(rootId);
		while(true && !children.isEmpty()) {
			Set<Integer> nextLevelCats = new HashSet<Integer>();
			for (Integer child : children) {
				if (categoryLevel.containsKey(child)) {
					continue;
				}else {
					categoryLevel.put(child, hierarchylevel);
//					System.out.println("child: " + cnames.get(child) + " level: " + hierarchylevel);
					if (wikiTaxonomyMap.get(child) != null) {
						nextLevelCats.addAll(wikiTaxonomyMap.get(child));
					} // end of if
				} // end of if
			} // end of for
			logger.info(hierarchylevel);
			System.out.println(hierarchylevel);
			children = nextLevelCats;
			hierarchylevel++;
		} // end of while
		System.out.println("Size of map: " + categoryLevel.size());
		System.out.println("Max Hierarcy Level: " + hierarchylevel);
        System.out.println("Serializing Category Level Map...");
        String fileName = DIR_PATH + "wikiCategoryLevel.ser";
        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(categoryLevel);
            out.close();
            System.out.println("Category Level Map Serialized successfully.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of setHierarchyLevelForAllCategories
	
	
	public Map<Integer, Integer> loadCatHierarchyLevelMapToMemory() {
		String fileName = DIR_PATH + "wikiCategoryLevel.ser";
		System.out.println("Loading Category Level Map into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
			Map<Integer, Integer> categoryLevel =  (Map<Integer, Integer>) in.readObject();
            in.close();
            System.out.println("Category Level Map Successfuly Loaded into Memory.\n");
			return categoryLevel;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadCatHierarchyLevelMapToMemory
	
	
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
	
	
	public void mapCategoryNamesToInt() {
		System.out.println("Connecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
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

	/**
	 * @return the categories
	 */
	public Set<Integer> getCategories() {
		return categories;
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(Set<Integer> categories) {
		this.categories = categories;
	}

	/**
	 * @return the categoryIdNameLookupTable
	 */
	public Map<Integer, String> getCategoryIdNameLookupTable() {
		return categoryIdNameLookupTable;
	}

	/**
	 * @param categoryIdNameLookupTable the categoryIdNameLookupTable to set
	 */
	public void setCategoryIdNameLookupTable(Map<Integer, String> categoryIdNameLookupTable) {
		this.categoryIdNameLookupTable = categoryIdNameLookupTable;
	}

	/**
	 * @return the categoryConceptLookupTable
	 */
	public Map<Integer, Set<String>> getCategoryConceptLookupTable() {
		return categoryConceptLookupTable;
	}

	/**
	 * @param categoryConceptLookupTable the categoryConceptLookupTable to set
	 */
	public void setCategoryConceptLookupTable(Map<Integer, Set<String>> categoryConceptLookupTable) {
		this.categoryConceptLookupTable = categoryConceptLookupTable;
	}

	
//	public static void main(String[] args) {
//		String [ ] c = {"as","asdhh", "uasdhsa"};
//		Set<String> e = new HashSet<String>();
//		e.addAll(Arrays.asList(c));
//		System.out.println(e);
//	}


}
