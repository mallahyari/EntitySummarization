/**
 * 
 */
package cs.uga.edu.dicgenerator;


import static cs.uga.edu.dicgenerator.VirtuosoAccess.GRAPH;
import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * @author Mehdi
 *
 */
public class CategoryProcessor {

	
	private static Map<Integer, Set<Integer>> wikiTaxonomyMap = null;
	private static int hitsPerPage = 1000000;
	private static final int interval = 10000;
	private static final Logger logger = Logger.getLogger(CategoryProcessor.class.getName());
	static DbpediaProcessor DbProcessor = new DbpediaProcessor();
	
	
	
	public static void main(String[] args) throws SecurityException, IOException {
		operateOnCategories("History", "Arab_history");
	
	}
	
	
	public static void fillCategoryEntityTable() {
		DbProcessor.persistWikipediaCategoryEntities();
	} // end of fillCategoryEntityTable
	
	
	public static void createCategoryInvertedIndex(){
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
//		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> catNames = catNameMapper.getCatNameList();
//		System.out.println(catNames.get(700475));
		FileHandler fh = null;
		try {
			fh = new FileHandler("invertedIndexlog.log");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			//		System.out.println("nametoint: " + catNamesToInt.size() + " catnames: " + catNames.size());
			Indexer indexer = new Indexer();
			IndexReader reader = DirectoryReader.open(indexer.getIndex());
			IndexSearcher searcher = new IndexSearcher(reader);
			Set<String> stopWords = null;//DbProcessor.readStopWords();
			Set<String> uniqueWords = new HashSet<String>(100000);
			EntityManagerImpl entityManager =  new EntityManagerImpl();
			String query = "";
			int counter = 0;
			int frequency = 1;
			long st = System.currentTimeMillis();
			for (String category : catNames) {
				String [] categoryWords = category.split("_");
				for (String word : categoryWords) {
					if (!stopWords.contains(word)) {
						word = word.replace("'", "").replace("(", "").replace(")", "").replace(",", "").replace("-", "").toLowerCase();
						if (!uniqueWords.contains(word)) {
							uniqueWords.add(word);
							Set<Integer> hits;
							hits = findCategoriesByKeyword(indexer, searcher, word);
							for (int catId : hits) {
								query = "INSERT INTO invertedindex (word,category_id,frequency) VALUES ('" + word + "'," + catId + "," + frequency + ")";
								entityManager.persist(query);
							} // end of for
						} // end of if (!uniqueWords.contains(word))
					} // end of if (!stopWords.contains(word))
				} // end of for (String word : categoryWords)
				counter++;
				if (counter % interval == 0) {
					logger.log(Level.INFO, counter + " records processed.");
				}
			} // end of for (String category : catNames)
			double duration = Double.valueOf((System.currentTimeMillis() - st)) / 3600000;
			logger.log(Level.INFO, "Total number of " + counter + " records processed.");
			logger.log(Level.INFO, "Number of Unique Category Words: " + uniqueWords.size());
			logger.log(Level.INFO, "Elapsed time: " + duration);
			entityManager.disconnectDatabase();
		} catch (SecurityException e) {
			logger.log(Level.WARNING, e.getMessage());
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage());
		} 
	} // end of fillCategoryTable

	

	public static Set<Integer> findCategoriesByKeyword(Indexer indexer, IndexSearcher searcher, String keyword) {
//		CategoryNameMapperClass catNameMapper = DictionaryGenerator.loadCatNameListToMemory();
//		List<String> catNames = catNameMapper.getCatNameList();
//		KeywordAnalyzer searchAnalyser = new KeywordAnalyzer();
		org.apache.lucene.search.Query query;
		Set<Integer> categories = new HashSet<Integer>();
		//		System.out.println("keyword: "+ keyword);
		QueryParser parser = new QueryParser(Version.LUCENE_44, "catName", indexer.getAnalyzer());
//		QueryParser parser = new QueryParser(Version.LUCENE_44, "catName", searchAnalyser);
		//		long startTimeMS = System.currentTimeMillis();
		try {
			query = parser.parse(keyword);
//			IndexReader reader = DirectoryReader.open(indexer.getIndex());
//			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			searcher.search(query, collector);
			//		long duarationMS = System.currentTimeMillis() - startTimeMS;
			//		System.out.println("Total Time for Search Query: " + duarationMS);
			ScoreDoc [] hits = collector.topDocs().scoreDocs;
			//		System.out.println("Found " + hits.length + " hits.");
			if (hits.length > 0){
				//			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
				//			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
				for(int cnt = 0;cnt < hits.length;cnt++) {
					int docId = hits[cnt].doc;
					Document d = searcher.doc(docId);
					//				String entityURI = d.get("entityURI");
					//				TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), docId, "allFieldsTogether", indexer.getAnalyzer());
					//				TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
					//				String entitySegments = "";
					//				for (int i = 0; i < frag.length; i++) {
					//					if ((frag[i] != null) && (frag[i].getScore() > 0)) {
					//						entitySegments += frag[i].toString() + "...";
					//					}
					//				} // end of for i
					int catId = Integer.valueOf(d.get("catId"));
//					String catName = d.get("catName");
//					System.out.println("catId: " + catId + " catName: " +  catName.replace(' ', '_'));
					categories.add(catId);
				} // end of for cnt
			} // end of if
					System.out.println("number of categories: " + categories.size());
		} catch (ParseException e) {
			logger.log(Level.WARNING, e.getMessage());
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		}
		return categories;
	} // end of findCategoriesByKeyword
	
	
	
	
	public static void createIndexForWikipediaCategories() {
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
//		List<String> catNames = catNameMapper.getCatNameList();
		Indexer indexer = new Indexer();
		try {
			indexer.createIndexWriter();
			indexer.indexWikipediaCategories(catNamesToInt);
			indexer.closeIndexWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	} // end of createIndexForWikipediaCategories
	
	public static void operateOnCategories (String cat1, String cat2) {
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
		wikiTaxonomyMap = DbProcessor.loadWikiTaxonomyMapFromParentToChildToMemory();
		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> cnames = catNameMapper.getCatNameList();
		System.out.println("size: " + catNamesToInt.size());
		int catId1 = catNamesToInt.get(cat1);
		int catId2 = catNamesToInt.get(cat2);
		Set<Integer> set1 = categoryTree(wikiTaxonomyMap, catId1);
		Set<Integer> set2 = categoryTree(wikiTaxonomyMap, catId2);
		Set<Integer> result = getIntersection(set1, set2);
		System.out.println("set1 size: " + set1.size() + "  set2 size: " + set2.size());
		if (result != null) {
			System.out.println("Intersection result size: " + result.size());
//			for (int c : result) {
//				System.out.println("Cat: " + cnames.get(c));
//			} // end of for
		} // end of if
		System.out.println("\nset1 size: " + set1.size() + "  set2 size: " + set2.size());
		result = getUnion(set1, set2);
		System.out.println("Union result size: " + result.size());
		System.out.println("\nset1 size: " + set1.size() + "  set2 size: " + set2.size());
		result = getDifference(set1, set2);
		System.out.println("\nDifference result size: " + result.size());
		System.out.println("set1 size: " + set1.size() + "  set2 size: " + set2.size());
		result = getSymetricDifference(set1, set2);
		System.out.println("\nSymetric Difference result size: " + result.size());
	} // operateOnCategories
	
	
	/*********************************************************
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static Set<Integer> getUnion(Set<Integer> set1, Set<Integer> set2) {
		if (set1 == null) {
			return set2; 
		}else if (set2 == null) {
			return set1;
		}
		Set<Integer> union = new HashSet<Integer> (set1);
		union.addAll(set2);
		return union;
	} // end of getUnion
	
	
	
	/*********************************************************
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static Set<Integer> getIntersection(Set<Integer> set1, Set<Integer> set2) {
		if (set1 == null || set2 == null) {
			return null; 
		}
		Set<Integer> intersection = new HashSet<Integer>(set1);
		intersection.retainAll(set2);
		return intersection;
	} // end of getIntersection
	
	
	/*********************************************************
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static Set<Integer> getDifference(Set<Integer> set1, Set<Integer> set2) {
		Set<Integer> difference = new HashSet<Integer>(set1);
		difference.removeAll(set2);
		return difference;
	} // end of getSymetricDifference
	
	
	
	/*********************************************************
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static Set<Integer> getSymetricDifference(Set<Integer> set1, Set<Integer> set2) {
		Set<Integer> union = new HashSet<Integer> (set1);
		union.addAll(set2);   // union now contains the Union of two sets
		Set<Integer> intersection = new HashSet<Integer> (set1);
		intersection.retainAll(set2);   // intersection contains the Intersection of two sets
		union.removeAll(intersection);
		return union;
	} // end of getSymetricDifference
	
	public static Set<Integer> categoryTree(Map<Integer, Set<Integer>> wikiTaxonomyMap, int sid, Set<Integer> catLoopMap) {
//		CategoryNameMapperClass catNameMapper = DictionaryGenerator.loadCatNameListToMemory();
//		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
//		List<String> cnames = catNameMapper.getCatNameList();
//		System.out.println("size: " + catNamesToInt.size());
//		wikiTaxonomyMap = DictionaryGenerator.loadWikiTaxonomyMapFromParentToChildToMemory();
		Set<Integer> children = wikiTaxonomyMap.get(sid);
		Set<Integer> resultList = new HashSet<Integer>();
//		Set<Integer> catLoopMap = new HashSet<Integer>();
		resultList.add(sid);
		catLoopMap.add(sid);
		if (children != null) {
			resultList.addAll(children);
		} // end of if
//		int i = 1;
//		long st = System.currentTimeMillis();
		while(true && children != null) {
			Set<Integer> travList = new HashSet<Integer>();
			for (Integer child : children) {
//				System.out.println("child: "  + child + " name: " + cnames.get(child));
				if (catLoopMap.contains(child)) {
					continue;
				}
				catLoopMap.add(child);
				if (wikiTaxonomyMap.get(child) != null) {
					travList.addAll(wikiTaxonomyMap.get(child));
				}
//				System.out.println();
			} // end of for
//			System.out.println();
//			System.out.println("========> Level " + i);
//			i++;
			if (travList.isEmpty()) {
				break;
			}else {
//				System.out.println("num of cats: " + travList.size());
				resultList.addAll(travList);
				children = travList;
			}
		} // end of while
		
//		System.out.println("total Time: " + (System.currentTimeMillis() - st));
//		System.out.println("size of category graph: " + resultList.size());
		return resultList;
	} // end of categoryTree
	

	public static Set<Integer> categoryTree(Map<Integer, Set<Integer>> wikiTaxonomyMap, int sid) {
//		CategoryNameMapperClass catNameMapper = DictionaryGenerator.loadCatNameListToMemory();
//		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
//		List<String> cnames = catNameMapper.getCatNameList();
//		System.out.println("size: " + catNamesToInt.size());
//		wikiTaxonomyMap = DictionaryGenerator.loadWikiTaxonomyMapFromParentToChildToMemory();
		Set<Integer> children = wikiTaxonomyMap.get(sid);
		Set<Integer> resultList = new HashSet<Integer>();
		Set<Integer> catLoopMap = new HashSet<Integer>();
		resultList.add(sid);
		catLoopMap.add(sid);
		if (children != null) {
			resultList.addAll(children);
		} // end of if
//		int i = 1;
//		long st = System.currentTimeMillis();
		while(true && children != null) {
			Set<Integer> travList = new HashSet<Integer>();
			for (Integer child : children) {
//				System.out.println("child: "  + child + " name: " + cnames.get(child));
				if (catLoopMap.contains(child)) {
					continue;
				}
				catLoopMap.add(child);
				if (wikiTaxonomyMap.get(child) != null) {
					travList.addAll(wikiTaxonomyMap.get(child));
				}
//				System.out.println();
			} // end of for
//			System.out.println();
//			System.out.println("========> Level " + i);
//			i++;
			if (travList.isEmpty()) {
				break;
			}else {
//				System.out.println("num of cats: " + travList.size());
				resultList.addAll(travList);
				children = travList;
			}
		} // end of while
		
//		System.out.println("total Time: " + (System.currentTimeMillis() - st));
//		System.out.println("size of category graph: " + resultList.size());
		return resultList;
	} // end of categoryTree
	
	
	public static Set<Integer> categoryTreeFromChildToParent(Map<Integer, Set<Integer>> wikiTaxMap, int sid) {
//		CategoryNameMapperClass catNameMapper = DictionaryGenerator.loadCatNameListToMemory();
//		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
//		List<String> cnames = catNameMapper.getCatNameList();
//		System.out.println("size: " + catNamesToInt.size());
//		wikiTaxonomyMap = DictionaryGenerator.loadWikiTaxonomyMapFromParentToChildToMemory();
		Set<Integer> parents = wikiTaxMap.get(sid);
		Set<Integer> resultList = new HashSet<Integer>();
		Set<Integer> catLoopMap = new HashSet<Integer>();
		catLoopMap.add(sid);
		if (parents != null) {
			resultList.addAll(parents);
		}else {
			resultList.add(sid);
		} // end of if
		int level = 1;
//		long st = System.currentTimeMillis();
		while(true && parents != null) {
			Set<Integer> travList = new HashSet<Integer>();
			for (Integer parent : parents) {
//				System.out.println("parent: "  + parent + " name: " + cnames.get(parent));
				if (catLoopMap.contains(parent)) {
					continue;
				}
				catLoopMap.add(parent);
				if (wikiTaxMap.get(parent) != null) {
					travList.addAll(wikiTaxMap.get(parent));
//					System.out.println(wikiTaxMap.get(parent).size());
				}
//				System.out.println();
			} // end of for
//			System.out.println("level: " + level + "      num of Parents: " + parents.size());
//			System.out.println("========> Level " + i);
			level++;
			if (travList.isEmpty() || level == 25) {
//			if (travList.isEmpty()) {
				break;
			}else {
//				System.out.println("num of cats: " + travList.size());
				resultList.addAll(travList);
				parents = travList;
			}
		} // end of while
		
//		System.out.println("total Time: " + (System.currentTimeMillis() - st));
//		System.out.println("size of parent category graph: " + resultList.size());
		return resultList;
	} // end of categoryTree
	
	
	public static void computeCategorySubgraphWordCount() {
		Map<Integer, Set<Integer>> wikiTaxonomyMapFromParentToChild = DbProcessor.loadWikiTaxonomyMapFromParentToChildToMemory();
		CategoryNameMapper catNameMapper = DbProcessor.loadCatNameListToMemory();
//		Map<String, Integer> catNamesToInt = catNameMapper.getNameToInt();
		List<String> cnames = catNameMapper.getCatNameList();
		Set<String> stopWords = null; //DbProcessor.readStopWords();
		Map<Integer, Integer> catWordCountMap = new HashMap<Integer, Integer>();
		int counter = 0;
		for (Map.Entry<Integer, Set<Integer>> entry : wikiTaxonomyMapFromParentToChild.entrySet()) {
			int ancestorId = entry.getKey();
			String ancestor = cnames.get(ancestorId);
			int numOfWords = 0;
			numOfWords += countWords(ancestor, stopWords);
			Set<Integer> descendants = categoryTree(wikiTaxonomyMapFromParentToChild, ancestorId);
			for (int childId : descendants) {
				String childCat = cnames.get(childId);
				numOfWords += countWords(childCat, stopWords);
			} // end of for
			catWordCountMap.put(ancestorId, numOfWords);
			counter++;
//			if (counter % interval == 0) {
//			System.out.println(counter + " of categories processed.");
//			}
		} // end of for
		System.out.println("Serializing Category Subgraph Word Count...");
        String fileName = null;//DbProcessor.DIR_PATH + "wikiCategorySubgraphWordCount.ser";
        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(catWordCountMap);
            out.close();
            System.out.println("Category Subgraph Word Count Serialized successfully.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		
	} // end of computeCategorySubgraphWordCount
	
	public static int countWords(String category, Set<String> stopWords) {
		String [] categoryWords = category.split("_");
		int wordCounter = 0;
		for (String word : categoryWords) {
			if (!stopWords.contains(word)) {
				wordCounter++;
			} // end of if
		} // end of for
		return wordCounter;
	} // end of countWords

	
	
	
	public static void fillCategoryTableWithWikipediaTaxanomy() {
		EntityManagerImpl entityManager =  new EntityManagerImpl();
		System.out.println("Connecting to Virtuoso ... ");
		VirtGraph virtGraph = connectToVirtuoso();
		System.out.println("Successfully Connected to Virtuoso!\n");
        StringBuffer queryString = new StringBuffer();
        queryString.append("PREFIX  lsdis: <http://lsdis.cs.uga.edu/wiki#> ");
        queryString.append("SELECT ?s ?sc FROM <" + GRAPH + "> WHERE { ");
        queryString.append("?s a <http://www.w3.org/2000/01/rdf-schema#Class> . ");
        queryString.append("?sc lsdis:wiki_category_parent ?s . ");
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
        int uriPrefixLength = 10;//DbProcessor.wikiCategoryUriPrefix.length();
        int rowCounter = 0;
        while (results.hasNext()) {
            result = results.nextSolution();
            numOfTriples = results.getRowNumber();
            if (numOfTriples % interval == 0) {
                System.out.println(numOfTriples);
            }
            superClass = result.get("s").toString();
            if (superClass.length() > uriPrefixLength) {
            	superClass = superClass.substring(uriPrefixLength).replace("'", "\\'");
            	subClass = result.get("sc").toString();
            	subClass = subClass.substring(uriPrefixLength).replace("'", "\\'");
            	String query = "INSERT INTO category (category_name,child_name) VALUES ('" 
						+ superClass + "','" + subClass + "')";
				try {
					rowCounter += entityManager.persist(query);
				} catch (Exception e) {
					System.out.println("error in insert: " + e.getMessage());
				}
            	// end of if
            } // end of if
        } // end of while
        vqe.close();
        System.out.println("number of records: " + rowCounter);
    } // end of fillCategoryTableWithWikipediaTaxanomy
	
	
	
	/*********************************************************
	 * @param child
	 * @param catLoopMap
	 * @return
	 */
	/*
	private static Set<String> traverseCategory(String child, Set<String> catLoopMap) {
		Set<String> subCategorySet = wikiTaxonomyMap.get(child);
		Set<String> traversedList = new HashSet<String>();
		if (subCategorySet != null) {
			for (String subCategory : subCategorySet) {
				if (catLoopMap.contains(subCategory)) {
					continue;
				} else {
					System.out.println(subCategory);
					catLoopMap.add(subCategory);
					traversedList.add(subCategory);
					traversedList.addAll(traverseCategory(subCategory, catLoopMap));
				} // end of if
			} // end of for
		} // end of if
		return traversedList;
	}

	public static void fillCategoryTable() {
		wikiTaxonomyMap = DictionaryGenerator.loadWikiTaxonomyMapFromParentToChildToMemory();
		EntityManagerImpl entityManager =  new EntityManagerImpl();
		System.out.println("size: " + wikiTaxonomyMap.size());
		Iterator<String> it = wikiTaxonomyMap.keySet().iterator();
		int rowCounter = 0;
		while(it.hasNext()) {
			String subCat = it.next();
			Set<String> parentCat = wikiTaxonomyMap.get(subCat);
			subCat = subCat.replace("'", "\\'");
			for (String parent : parentCat) {
				Set<String> catLoopMap = new HashSet<String>();
				catLoopMap.add(parent);
				Set<String> traversedList = traverseCategoryDirectory(parent, catLoopMap,entityManager);
				parent = parent.replace("'", "\\'");
				String query = "INSERT INTO category (category_name,parent_name,relation) VALUES ('" 
						+ subCat + "','" + parent + "','D')";
				try {
					rowCounter += entityManager.persist(query);
				} catch (Exception e) {
					System.out.println(".");
//					System.out.println("error in insert: " + e.getMessage());
				}
				if (rowCounter % 100000 == 0) {
					System.out.println(rowCounter + " INSERTED!");
				} // end of if
				for (String c : traversedList) {
					c = c.replace("'", "\\'");
					query = "INSERT INTO category (category_name,parent_name,relation) VALUES ('" 
							+ subCat + "','" + c + "','I')";
					try {
						rowCounter += entityManager.persist(query);
					} catch (Exception e) {
						System.out.println(".");
//						System.out.println("error in insert: " + e.getMessage());
					}
					if (rowCounter % 100000 == 0) {
						System.out.println(rowCounter + " INSERTED!");
					} // end of if
				} // end of for
			} // end of for
		} // end of while
		System.out.println("total rows: " + rowCounter);
		entityManager.disconnectDatabase();
	} // end of fillCategoryTable
	
	public static Set<String> traverseCategoryDirectory(String category, Set<String> catLoopMap, EntityManagerImpl entityManager) {
		Set<String> superCategorySet = wikiTaxonomyMap.get(category);
		Set<String> traversedList = new HashSet<String>();
		if (superCategorySet != null) {
			for (String superCategory : superCategorySet) {
				if (catLoopMap.contains(superCategory)) {
					continue;
				} else {
					catLoopMap.add(superCategory);
					traversedList.add(superCategory);
					traversedList.addAll(traverseCategoryDirectory(superCategory, catLoopMap, entityManager));
				} // end of if
			} // end of for
		} // end of if
		return traversedList;
	} // end of traverseCategoryDirectory
	
	
	
	
*/
}
