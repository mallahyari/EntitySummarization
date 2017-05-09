/**
 * 
 */
package cs.uga.edu.topicmodel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs.uga.edu.dblayer.PersistentLayerImpl;
import cs.uga.edu.properties.Configuration;
import cs.uga.edu.wikiaccess.WikipediaAccessLayer;
import cc.mallet.topics.SimpleLDA;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

/**
 * @author Mehdi
 *
 */
public class OntTopicModel extends SimpleLDA{

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(OntTopicModel.class.getName());
	private final String DATA_DIRECTORY_PATH = Configuration.getProperty("dataDirPath");
	private final String WIKIPEDIA_DIRECTORY_PATH = Configuration.getProperty("wikipediaPagesPath");
	private final String SAVEDFILES_DIRECTORY_PATH = Configuration.getProperty("savedFilesPath");
	private Map<Integer, Map<Integer, Integer>> wordConceptCountMatrix = null;
	private Map<Integer, Map<Integer, Integer>> conceptTopicCountMatrix = null;
	private Map<Integer, Map<Integer, Integer>> topicDocumentCountMatrix = null;
	private Map<Integer, Integer> documentTopicSumCount = null;
	private Map<Integer, Integer> topicConceptSumCount = null;
	private Map<Integer, Integer> conceptTermSumCount = null;
	private Set<Integer>  conceptIdList = null; 
	private Set<Integer>  topicIdList = null; 
	private Map<Integer, String> conceptLookupTable = null;
	private PersistentLayerImpl entityManager = null;
	private Map<Integer, Integer> lessFrequentWords = null;
	private Map<Integer, String> topicIdNameMap = null;   // from topicId to topicName
	private Map<String, Integer> topicNameIdMap = null;   // from topicName to topicId
	private Map<Integer, Set<String>> topicConceptMap = null; // from topicId to list of concepts
	private WikipediaAccessLayer wiki = new WikipediaAccessLayer();
	private InstanceList documents = null;
	private InstanceList wikiConceptsData = null;
	
	
	public OntTopicModel(int numberOfTopics) {
		super(numberOfTopics);
		
	}
	
	public void initializeTopicsAndCocepts() {
		importDocuments();
		loadConceptsVocabulary();
		initializeCountArrays(documents.size());
//		topicIdList = wiki.loadCategories();
		conceptIdList = generateIdForConcepts();
//		topicIdNameMap = wiki.loadCategoryIdNameLookupTable();
//		topicNameIdMap = wiki.loadCategoryNameIdLookupTable();
//		topicConceptMap = wiki.loadCategoryConceptLookupTable();
		topicIdList = getTopicIds();
		this.numTopics = topicIdList.size();
		preprocessDocuemnts();
//		System.out.println("numTopics: " + numTopics);
	} // end of initializeTopicsAndCocepts
	
	
	
	/*************************************************
	 * @return
	 */
	public Set<Integer> getTopicIds() {
//		Set<String> concepts = new HashSet<String>(conceptLookupTable.values());
//		Set<String> topicnames = wiki.findCategoriesByConceptName(concepts);
//		String [] topicnames = {"Information_retrieval", "Information_theory", "Machine_learning", "Statistical_natural_language_processing",
//				"Data_mining"};
		Set<Integer> topicIds = new HashSet<Integer>();
		topicIds.add(0);
		topicIds.add(1);
		topicIds.add(2);
		topicIds.add(3);
		topicIds.add(4);
//		int i = 1;
//		for (String topic : topicnames) {
//			if (topicNameIdMap.get(topic) != null) {
//				int topicId = topicNameIdMap.get(topic);
//				topicIds.add(topicId);
//				logger.info("i: " + (i++) + "  topic: " + topic + " topicId: " +  topicId);
//			}else {
////				System.out.println("NULL for: " + topic);
//			}
//		} // end of for
//		System.out.println("num of topics: " + topicIds.size());
		return topicIds;
	} // end of getTopicIds

	public void runGibbsSampling() throws Exception {
//		entityManager = new PersistentLayerImpl();
		Alphabet wordLookupTable = documents.getDataAlphabet();
		int numOfIterations = Integer.valueOf(Configuration.getProperty("numOfIteration"));
		random = new Randoms();
		double alpha = 50 / (topicIdList.size());
		double beta = 0.1;
		double gamma = 0.1;
		
		Set<String> newVocab = getIntersectionVocabulary();
		
		/// Gibbs Sampling initialization ///
		doGibbsSamplingInitialization(wordLookupTable, newVocab);

		
		/// Gibbs sampling over burn-in period and sampling period while ///
		
		int documentId = -1;
		int numOfWords = newVocab.size();
//		int numOfWords = wordLookupTable.size() - lessFrequentWords.size();
		int numOfTopics = getNumTopics();
		int numOfConcepts = wikiConceptsData.size();
		
		/// Un-normilized weight for P(z|c,w) ///
//		Map<Integer, Double> z = new HashMap<Integer, Double> (numOfTopics);
//		Map<Integer, Integer> zcAssignment = new HashMap<Integer, Integer> (numOfTopics);  // array to keep the concepts assignment for topics
		
		boolean add = true;
		boolean subtract = false;
		double sumOfC = 0;
		double sumOfZ = 0;
		int wcCount = 0;
		int ctCount = 0;
		int tdCount = 0;
		int wordId = -1;
		int currentConcept = -1;
		int currentTopic = -1;
		int newConceptId = -1;
		int newTopicId = -1;
		double probZ = 0.0;
		int topicCtr = -1;
		int conceptCtr = -1;
		double [ ] probZarr = new double [numOfTopics];
		double [ ] probCarr = new double [numOfConcepts];
		double topicDenum = 1;
		double wordDenum = 1;
		double conceptDenum = 1;
		double probC = 0;
		double probW = 0;
		int [ ] zIdarr = new int [numOfTopics];
		int [ ] cIdarr = new int [numOfConcepts];
		FeatureVector cVocab = null;
		logger.info("=====> Gibbs Sampling iterations START <=====");
		for (int iteration = 0; iteration < numOfIterations; iteration++) {
			logger.info("iteration " + iteration);
			documentId = -1;
			for (Instance doc : documents) {
				documentId++;
				logger.info("document: " + documentId);
				FeatureVector words = (FeatureVector) doc.getData();
				for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
					wordId = words.indexAtLocation(wordCntr); // id of the word
					String word = wordLookupTable.lookupObject(wordId).toString(); // the word 
					if (!lessFrequentWords.containsKey(wordId) && newVocab.contains(word)) {
						String [ ] currentAssignment = doc.getProperty(wordId + "").toString().split(",");
						currentConcept = Integer.valueOf(currentAssignment [ 0 ]);
						currentTopic = Integer.valueOf(currentAssignment [ 1 ]);
						updateSumCount(conceptTermSumCount, currentConcept, subtract);  // n(c)   -= 1
						updateSumCount(topicConceptSumCount, currentTopic, subtract);  	 // n(k)  -= 1
						updateSumCount(documentTopicSumCount, documentId, subtract);   	 // n(d)  -= 1
						updateWordConceptCount(wordConceptCountMatrix, wordId, currentConcept, subtract);  	  		   // n(c)_w -= 1
						updateConceptTopicCount(conceptTopicCountMatrix, currentConcept, currentTopic, subtract);  	   // n(k)_c -= 1
						updateTopicDocumentCount(topicDocumentCountMatrix, currentTopic, documentId, subtract);   	   // n(d)_k -= 1
						/// P(z) ///
//						logger.info(".");

						topicDenum = getMapValue(documentTopicSumCount, documentId) + (topicIdList.size() * alpha);
						wordDenum = getMapValue(conceptTermSumCount, currentConcept) + (numOfWords * gamma);
						wcCount = getMapValue(wordConceptCountMatrix, wordId, currentConcept);  // n(c)_w
						probW = (wcCount + gamma) / wordDenum;

						/// Calculate P(z|c,w) ///
						topicCtr = 0;
						sumOfZ = 0;
						for (int topicId : topicIdList) {
							conceptDenum = getMapValue(topicConceptSumCount, topicId) + (numOfConcepts * beta);
							ctCount = getMapValue(conceptTopicCountMatrix, currentConcept, topicId);  // n(k)_c
							probC = (ctCount + beta) / conceptDenum;
							tdCount = getMapValue(topicDocumentCountMatrix, topicId, documentId);  // n(d)_k
							probZ = ((tdCount + alpha) * probC * probW) / topicDenum; 
							sumOfZ += probZ;
							probZarr [ topicCtr ] = probZ;
							zIdarr [ topicCtr ] = topicId;
							topicCtr++;
						} // end of for (int topicId : topicIdList)

						/// Sample a new topic from P(z|c,w) ///
						
//						int topicIndex = random.nextDiscrete(probZarr, sumOfZ);
						int topicIndex = sampleTopic(probZarr, zIdarr, sumOfZ);
						newTopicId = zIdarr [ topicIndex ];

						/// Calculate P(c|z,w) ///
						conceptCtr = 0;
						sumOfC = 0;
						conceptDenum = getMapValue(topicConceptSumCount, newTopicId) + (numOfConcepts * beta);
						tdCount = getMapValue(topicDocumentCountMatrix, newTopicId, documentId);  // n(d)_k
						probZ = (tdCount + alpha) / topicDenum;
						for (Instance concept : wikiConceptsData) {
							int conceptId = Integer.parseInt(concept.getName().toString());
							cVocab  = (FeatureVector) concept.getData();
							/// Check to see if w is a priori member of a concept in the ontology ///
							if (!cVocab.contains(word)) {
								probC   = 0;
								probCarr [ conceptCtr ] = probC;
								cIdarr [ conceptCtr ] = conceptId;
								conceptCtr++;
								continue;
							} // end of if
							wordDenum = getMapValue(conceptTermSumCount, conceptId) + (numOfWords * gamma);
							ctCount = getMapValue(conceptTopicCountMatrix, conceptId, newTopicId);  // n(k)_c
							wcCount = getMapValue(wordConceptCountMatrix, wordId, conceptId);   // n(c)_w
							probC   = ((ctCount + beta) * (wcCount + gamma) * probZ) / (conceptDenum * wordDenum);
							sumOfC += probC;
							probCarr [ conceptCtr ] = probC;
							cIdarr [ conceptCtr ] = conceptId;
							conceptCtr++;
						}  // end of for (Instance concept : wikiConceptsData)

						/// Sample a new concept from P(c|z,w) ///
//						int conceptIndex = random.nextDiscrete(probCarr, sumOfC);
						int conceptIndex = sampleConcept(probCarr, cIdarr, sumOfC);
						newConceptId = cIdarr [ conceptIndex ];

						/// Update the counts ///
						doc.setProperty(wordId + "", newConceptId + "," + newTopicId);
						updateTopicDocumentCount(topicDocumentCountMatrix, newTopicId, documentId, add);  // n(d)_k += 1
						updateSumCount(documentTopicSumCount, documentId, add);  						  // n(d)   += 1
						updateWordConceptCount(wordConceptCountMatrix, wordId, newConceptId, add);  	  // n(c)_w += 1
						updateSumCount(conceptTermSumCount, newConceptId, add);  						  // n(c)   += 1
						updateConceptTopicCount(conceptTopicCountMatrix, newConceptId, newTopicId, add);  // n(k)_c += 1
						updateSumCount(topicConceptSumCount, newTopicId, add);   						  // n(k)   += 1
					} // end of if (!lessFrequentWords.containsKey(wordId))
				} // end of for (wordCntr)
			} // end of for (Instance doc)
		} // end of for (iteration)
		logger.info("Gibbs Sampling Done!");
		saveLookupTable(conceptLookupTable, "conceptLookupTable.ser");
		saveCountMatrix(wordConceptCountMatrix, "wordConceptCountMatrix.ser");
		saveCountMatrix(conceptTopicCountMatrix, "conceptTopicCountMatrix.ser");
		saveCountMatrix(topicDocumentCountMatrix, "topicDocumentCountMatrix.ser");
		saveSumCountMatrix(conceptTermSumCount, "conceptTermSumCount.ser");
		saveSumCountMatrix(topicConceptSumCount, "topicConceptSumCount.ser");
		saveSumCountMatrix(documentTopicSumCount, "documentTopicSumCount.ser");
		
		
//		for (Instance doc : documents) {
//			FeatureVector fv = (FeatureVector) doc.getData();
//			for (int i = 0;i < fv.numLocations();i++) {
//				int id = fv.indexAtLocation(i);
//				System.out.println("==>entry: " +fv.getAlphabet().lookupObject(id) + " " + doc.getProperty(id + "")); 
//			} // end of for (i)
//		} // end of for (Instance doc : documents)
		
	} // end of runGibbsSampling
	
	/*************************************************
	 * @param probCarr
	 * @param cIdarr
	 * @param sumOfC 
	 * @return
	 */
	public int sampleConcept(double[] probCarr, int[] cIdarr, double sumOfC) {
		double [] FX = new double [cIdarr.length];
		double sum = 0.0;
		int index = -1;
		for (int i = 0; i < probCarr.length; i++) {
			probCarr [ i ] = probCarr [ i ] / sumOfC;
		} // end of for (i)
		for (int i = 0; i < cIdarr.length; i++) {
			FX [ i ] = sum + probCarr [ i ];
			sum += probCarr [ i ];
		} // end of for (i)
		Randoms r = new Randoms();
		double s = r.nextUniform();
		for (int i = 0; i < FX.length; i++) {
			if (s <= FX [ i ]) {
				index = i;
				break;
			} // end of if
		} // end of for (i)
		return index;
	} // end of sampleConcept

	/*************************************************
	 * @param probZarr
	 * @param zIdarr
	 * @param sumOfZ 
	 * @return
	 */
	public int sampleTopic(double[] probZarr, int[] zIdarr, double sumOfZ) {
		double [] FX = new double [zIdarr.length];
		double sum = 0.0;
		int index = -1;
		for (int i = 0; i < probZarr.length; i++) {
			probZarr [ i ] = probZarr [ i ] / sumOfZ;
		} // end of for (i)
		for (int i = 0; i < zIdarr.length; i++) {
			FX [ i ] = sum + probZarr [ i ];
			sum += probZarr [ i ];
		} // end of for (i)
		Randoms r = new Randoms();
		double s = r.nextUniform();
		for (int i = 0; i < FX.length; i++) {
			if (s <= FX [ i ]) {
				index = i;
				break;
			} // end of if
		} // end of for (i)
		return index;
	} // end of sampleTopic

	/*************************************************
	 * @param map
	 * @param id1
	 * @param id2
	 * @return
	 */
	private int getMapValue(Map<Integer, Map<Integer, Integer>> map, int id1, int id2) {
		Map<Integer, Integer> m = map.get(id1);
		if (m != null) {
			Integer val = m.get(id2);
			return val != null ? val.intValue() : 0;
		} // end of if
		return 0;
	} // end of getMapValue

	/*************************************************
	 * @param documentTopicSumCount2
	 * @param documentId
	 * @return
	 */
	private int getMapValue(Map<Integer, Integer> map, int id) {
		Integer val = map.get(id);
		return val != null ? val.intValue() : 0;
	} // end of getMapValue

	public void loadResults() {
		conceptTermSumCount   = loadSumCountMatrix("conceptTermSumCount.ser");
		topicConceptSumCount  = loadSumCountMatrix("topicConceptSumCount.ser");
		documentTopicSumCount = loadSumCountMatrix("documentTopicSumCount.ser");
		System.out.println("conceptTermSumCount size: " + conceptTermSumCount.size());
		System.out.println("topicConceptSumCount size: " + topicConceptSumCount.size());
		System.out.println("documentTopicSumCount size: " + documentTopicSumCount.size());
		wordConceptCountMatrix = loadCountMatrix("wordConceptCountMatrix.ser");
		conceptTopicCountMatrix = loadCountMatrix("conceptTopicCountMatrix.ser");
		topicDocumentCountMatrix = loadCountMatrix("topicDocumentCountMatrix.ser");
		System.out.println("wordConceptCountMatrix size: " + wordConceptCountMatrix.size());
		System.out.println("conceptTopicCountMatrix size: " + conceptTopicCountMatrix.size());
		System.out.println("topicDocumentCountMatrix size: " + topicDocumentCountMatrix.size());
		calculateWordConceptProbability(0.1);
		calculateConceptTopicProbability(0.1);
		
//		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = tdCountMx.entrySet().iterator();
//		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = ctCountMx.entrySet().iterator();
//		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = wordConceptCountMatrix.entrySet().iterator();
//		while (itr.hasNext()) {
//			Map.Entry<Integer, Map<Integer, Integer>> wcRecord = itr.next();
//			System.out.println(wcRecord.getValue().size());
//		} // end of while
		
	} // end of loadResults
	
	
	public void calculateConceptTopicProbability(double beta){
		importDocuments();
		loadConceptsVocabulary();
		conceptLookupTable = loadConceptLookupTable("conceptLookupTable.ser");
		try {
			File file = new File(SAVEDFILES_DIRECTORY_PATH + "conceptTopicProb.csv");
			if (file.exists()) {
				logger.info("File already exists!");
				file.delete();
				logger.info("File is deleted.");
			} // end of if
			FileWriter fout = new FileWriter(file);
			topicIdList = new HashSet<Integer>();
			for (int i = 0; i < 5; i++) {
				topicIdList.add(i);
			} // end of for (i)
			numTopics = topicIdList.size();
			logger.info("calculating Probabilities...");
			for (Instance concept : wikiConceptsData) {
				String fileName = concept.getName().toString();
				String conceptName = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".txt", "");
				System.out.println(conceptName);
				Iterator<Entry<Integer, String>> itr = conceptLookupTable.entrySet().iterator();
				int conceptId = -1;
				while(itr.hasNext()) {
					Entry<Integer, String> entry = itr.next();
					if (entry.getValue().equals(conceptName)) {
						conceptId = entry.getKey();
						break;
					} // end of if
				} // end of while
				String tuple = "";
				Map<Integer, Integer> ctAssignments = conceptTopicCountMatrix.get(conceptId);
				for (int topicId : topicIdList) {
					double prob = 0.0;
					int ctCount = 0;
					if (ctAssignments != null) {
						ctCount = getMapValue(ctAssignments, topicId);
						prob = (ctCount + beta) / (getMapValue(topicConceptSumCount, topicId) + (numTopics * beta));
						tuple += topicId + "," + prob + ",";
					}else {
						prob = 0.0;
					} // end of if
					
//					prob = (ctCount + beta) / (getMapValue(topicConceptSumCount, topicId) + (numTopics * beta));
//					tuple += topicId + "," + prob + ",";
				} // end of for
				tuple = conceptName + "," + tuple;
				tuple = tuple.substring(0, tuple.length() - 1) + "\n";
				fout.write(tuple);
				fout.flush();
			} // end of for
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Calculation is done.");
	} // end of calculateConceptTopicProbability
	
	public void calculateWordConceptProbability(double gamma){
		importDocuments();
		loadConceptsVocabulary();
		conceptLookupTable = loadConceptLookupTable("conceptLookupTable.ser");
		try {
			Alphabet wVocabulary = documents.getDataAlphabet();
//			Alphabet cVocabulary = wikiConceptsData.getDataAlphabet();
			File file = new File(SAVEDFILES_DIRECTORY_PATH + "wordConceptProb.csv");
			if (file.exists()) {
				logger.info("File already exists!");
				file.delete();
				logger.info("File is deleted.");
			} // end of if
			FileWriter fout = new FileWriter(file);
			int vocabSize = wVocabulary.size();
			Object [ ] words = wVocabulary.toArray();
			logger.info("calculating Probabilities...");
			for (Instance concept : wikiConceptsData) {
				FeatureVector cVocab = (FeatureVector) concept.getData();
				String fileName = concept.getName().toString();
				String conceptName = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".txt", "");
				Iterator<Entry<Integer, String>> itr = conceptLookupTable.entrySet().iterator();
				int conceptId = -1;
				while(itr.hasNext()) {
					Entry<Integer, String> entry = itr.next();
					if (entry.getValue() == conceptName) {
						conceptId = entry.getKey();
						break;
					} // end of if
				} // end of while
//				int conceptId = Integer.parseInt(concept.getName().toString());
//				String conceptName = conceptLookupTable.get(conceptId).trim();
				String tuple = "";
				for (Object word : words) {
					int wordId = wVocabulary.lookupIndex(word);
					double prob = 0.0;
					Map<Integer, Integer> wcAssignments = wordConceptCountMatrix.get(wordId);
					if (!cVocab.contains(word)) {
						prob = 0.0;
					}else {
						if (wcAssignments != null) {
							int wcCount = getMapValue(wcAssignments, conceptId);
							prob = (wcCount + gamma) / (getMapValue(conceptTermSumCount, conceptId) + (vocabSize * gamma));
						} // end of if
						tuple += word.toString() + "," + prob + ",";
					} // end of if
//					tuple += word.toString() + "," + prob + ",";
				} // end of for
				tuple = conceptName + "," + tuple;
				tuple = tuple.substring(0, tuple.length() - 1) + "\n";
				fout.write(tuple);
				fout.flush();
			} // end of for
			fout.close();
			
//			for (Object word : words) {
//				int wordId = vocabulary.lookupIndex(word);
//				Map<Integer, Integer> wcAssignments = wordConceptCountMatrix.get(wordId);
//				if (wcAssignments != null) {
//					logger.info("map size: " + wcAssignments.size());
//					Iterator<Entry<Integer, Integer>> itr = wcAssignments.entrySet().iterator();
//					while (itr.hasNext()) {
//						Entry<Integer, Integer> rec = itr.next();
//						int conceptId = rec.getKey();
//						int count = rec.getValue();
//						double prob = (count + gamma) / (getMapValue(conceptTermSumCount, conceptId) + (vocabSize * gamma));
//						tuple = word + "," + conceptLookupTable.get(conceptId).trim() + "," + prob + "\n";
//						fout.write(tuple);
//						fout.flush();
//					} // end of while
//				} // end of if
//			} // end of for
//			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Calculation is done.");
	} // end of calculateWordConceptProbability

	
	public Map<Integer, String> loadConceptLookupTable(String fileName) {
		logger.info("Loading " + fileName + " into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(SAVEDFILES_DIRECTORY_PATH + fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
            Map<Integer, String> clt =  (Map<Integer, String>) in.readObject();
            in.close();
            logger.info(fileName + " Successfuly Loaded into Memory.\n");
			return clt;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadConceptLookupTable
	
	
	public Map<Integer, Integer> loadSumCountMatrix(String fileName) {
		logger.info("Loading " + fileName + " into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(SAVEDFILES_DIRECTORY_PATH + fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> sumCountMatrix =  (Map<Integer, Integer>) in.readObject();
            in.close();
            logger.info(fileName + " Successfuly Loaded into Memory.\n");
			return sumCountMatrix;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadSumCountMatrix
	
	
	public Map<Integer, Map<Integer, Integer>> loadCountMatrix(String fileName) {
		logger.info("Loading " + fileName + " into Memory...");
        try {
            FileInputStream inputFile = new FileInputStream(SAVEDFILES_DIRECTORY_PATH + fileName);
            BufferedInputStream bfin = new BufferedInputStream(inputFile);
            ObjectInputStream in = new ObjectInputStream(bfin);
            @SuppressWarnings("unchecked")
            Map<Integer, Map<Integer, Integer>> countMatrix =   (Map<Integer, Map<Integer, Integer>>) in.readObject();
            in.close();
            logger.info(fileName + " Successfuly Loaded into Memory.\n");
			return countMatrix;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
	} // end of loadCountMatrix
	
	
	/*************************************************
	 * @param sumCount
	 * @param fileName
	 */
	public void saveSumCountMatrix(Map<Integer, Integer> sumCount, String fileName) {
		logger.info("Serializing " + fileName + "...");
        try {
        	File f = new File(SAVEDFILES_DIRECTORY_PATH + fileName);
        	if (f.exists()) {
        		logger.info(fileName + " already exists");
        		f.delete();
        		logger.info(fileName + " deleted.");
        	} // end of if
            FileOutputStream outputFile = new FileOutputStream(f);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(sumCount);
            out.close();
            logger.info(fileName + " Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveSumCountMatrix

	/*************************************************
	 * @param conceptLookupTable
	 * @param fileName
	 */
	public void saveLookupTable(Map<Integer, String> conceptLookupTable, String fileName) {
		logger.info("Serializing " + fileName + "...");
        try {
        	File f = new File(SAVEDFILES_DIRECTORY_PATH + fileName);
        	if (f.exists()) {
        		logger.info(fileName + " already exists");
        		f.delete();
        		logger.info(fileName + " deleted.");
        	} // end of if
            FileOutputStream outputFile = new FileOutputStream(f);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(conceptLookupTable);
            out.close();
            logger.info(fileName + " Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveLookupTable

	/*************************************************
	 * @param countMatrix
	 * @param fileName
	 */
	public void saveCountMatrix(Map<Integer, Map<Integer, Integer>> countMatrix, String fileName) {
		logger.info("Serializing " + fileName + "...");
        try {
        	File f = new File(SAVEDFILES_DIRECTORY_PATH + fileName);
        	if (f.exists()) {
        		logger.info(fileName + " already exists");
        		f.delete();
        		logger.info(fileName + " deleted.");
        	} // end of if
            FileOutputStream outputFile = new FileOutputStream(f);
            BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
            ObjectOutputStream out = new ObjectOutputStream(bfout);
            out.writeObject(countMatrix);
            out.close();
            logger.info(fileName + " Serialized successfully.\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} // end of saveCountMatrix

	/*************************************************
	 * @param z
	 * @param sumOfZ
	 * @return
	 */
	public int doTopicSampling(Map<Integer, Double> z, double sumOfZ) {
		Randoms random = new Randoms();
		double x = random.nextUniform(0, sumOfZ);
		return findIndexInSampling(z, x);
	} // end of doTopicSampling

	/*************************************************
	 * @param s
	 * @param r
	 * @param q
	 * @param sumOfS
	 * @param sumOfR
	 * @param sumOfQ
	 * @return
	 */
	private int doConceptSampling(Map<Integer, Double> s, Map<Integer, Double> r, Map<Integer, Double> q, double sumOfS, double sumOfR, double sumOfQ) {
		Randoms random = new Randoms();
		int conceptId = -1;
		try {
			double x = random.nextUniform(0, sumOfS + sumOfR + sumOfQ);
			if (x <= sumOfS) {
				conceptId = findIndexInSampling(s, x);
			}else if (x > sumOfS && x <= sumOfS + sumOfR) {
				ResultSet rs = entityManager.ifindNonZeroConceptTopicCount();
				Map<Integer, Double> tmp = new HashMap<Integer, Double>();
				while (rs.next()) {
					tmp.put(rs.getInt("concept_id"), r.get(conceptId));
				} // end of while
				conceptId = findIndexInSampling(tmp, x);
			}else if (x > sumOfR + sumOfQ) {
				ResultSet rs = entityManager.ifindNonZeroWordConceptCount();
				Map<Integer, Double> tmp = new HashMap<Integer, Double>();
				while (rs.next()) {
					tmp.put(rs.getInt("concept_id"), q.get(conceptId));
				} // end of while
				conceptId = findIndexInSampling(tmp, x);
			} // end of if

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conceptId;
	} // end of doConceptSampling
	
	public int findIndexInSampling(Map<Integer, Double> pmf, double x) {
		double sum = 0.0;
		double prob = 0.0;
		int index = -1;
		List<Double> probabilities = new ArrayList<Double>(pmf.values());
		Collections.sort(probabilities);
		for (int i = 0; i < probabilities.size(); i++) {
			sum += probabilities.get(i);
			if (sum > x) {
				prob = probabilities.get(i);
				break;
			} // end of if
		} // end of for (i)
		for(Map.Entry<Integer, Double> entry : pmf.entrySet()) {
			if (entry.getValue() == prob) {
				index = entry.getKey();
				break;
			} // end of if
		} // end of for
		return index;
	} // end of findIndexInSampling
	
	
	public Map<Integer, Integer> getWordCountByConceptId(int conceptId) {
		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = wordConceptCountMatrix.entrySet().iterator();
		Map<Integer, Integer> wcCountMap = new HashMap<Integer, Integer>(); 
		while (itr.hasNext()) {
			Map.Entry<Integer, Map<Integer, Integer>> wcRecord = itr.next();
			if (wcRecord.getValue().containsKey(conceptId)) {
				wcCountMap.put(wcRecord.getKey(), wcRecord.getValue().get(conceptId));
			} // end of if
		} // end of while
		
//		String query = "SELECT * from word_concept WHERE concept_id = " + conceptId;
//		try {
//			ResultSet rs = entityManager.ifind(query);
//			if (rs.next()) {
//				int wid = rs.getInt("word_id");
//				int count = rs.getInt("total_count");
//				wcCountMap.put(wid, count);
//			} // end of if
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		return wcCountMap;
	} // end of getWordCountByConceptId
	
	
	public Map<Integer, Integer> getConceptCountByTopicId(int topicId) {
		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = conceptTopicCountMatrix.entrySet().iterator();
		Map<Integer, Integer> ctCountMap = new HashMap<Integer, Integer>(); 
		while (itr.hasNext()) {
			Map.Entry<Integer, Map<Integer, Integer>> ctRecord = itr.next();
			if (ctRecord.getValue().containsKey(topicId)) {
				ctCountMap.put(ctRecord.getKey(), ctRecord.getValue().get(topicId));
			} // end of if
		} // end of while
		
//		String query = "SELECT * from concept_topic WHERE topic_id = " + topicId;
//		try {
//			ResultSet rs = entityManager.ifind(query);
//			if (rs.next()) {
//				int cid = rs.getInt("concept_id");
//				int count = rs.getInt("total_count");
//				ctCountMap.put(cid, count);
//			} // end of if
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		return ctCountMap;
	} // end of getConceptCountByTopicId
	
	
	public Map<Integer, Integer> getTopicCountByDocumentId(int documentId) {
		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = topicDocumentCountMatrix.entrySet().iterator();
		Map<Integer, Integer> tdCountMap = new HashMap<Integer, Integer>(); 
		while (itr.hasNext()) {
			Map.Entry<Integer, Map<Integer, Integer>> tdRecord = itr.next();
			if (tdRecord.getValue().containsKey(documentId)) {
				tdCountMap.put(tdRecord.getKey(), tdRecord.getValue().get(documentId));
			} // end of if
		} // end of while
		
//		String query = "SELECT * from topic_document WHERE document_id = " + documentId;
//		Map<Integer, Integer> tdCountMap = new HashMap<Integer, Integer>(); 
//		try {
//			ResultSet rs = entityManager.ifind(query);
//			if (rs.next()) {
//				int tid = rs.getInt("topic_id");
//				int count = rs.getInt("total_count");
//				tdCountMap.put(tid, count);
//			} // end of if
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		return tdCountMap;
	} // end of getTopicCountByDocumentId
	

	/*************************************************
	 * @param topicId
	 * @param documentId
	 */
	public int getTopicDocumentCount(int topicId, int documentId) {
		int counts = 0;
		try {
			ResultSet rs = entityManager.ifindTopicDocumentCount(topicId, documentId);
			if (rs.next()) {
				counts = rs.getInt("total_count");
			} // end of if
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return counts;
	} // end of getTopicDocumentCount

	/*************************************************
	 * @param conceptId
	 * @param topicId
	 */
	public int getConceptTopicCount(int conceptId, int topicId) {
		int counts = 0;
		try {
			ResultSet rs = entityManager.ifindConceptTopicCount(conceptId, topicId);
			if (rs.next()) {
				counts = rs.getInt("total_count");
			} // end of if
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return counts;
	} // end of getConceptTopicCount

	/*************************************************
	 * @param wordId
	 * @param cocneptId
	 */
	public int getWordConceptCount(int wordId, int cocneptId) {
		int counts = 0;
		try {
			ResultSet rs = entityManager.ifindWordConceptCount(wordId, cocneptId);
			if (rs.next()) {
				counts = rs.getInt("total_count");
			} // end of if
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return counts;
	} // end of getWordConceptCount

	/*************************************************
	 * @param documents
	 * @param wordLookupTable 
	 * @param newVocab 
	 */
	public void doGibbsSamplingInitialization(Alphabet wordLookupTable, Set<String> newVocab) {
		/// initialize the Matrices ///
		wordConceptCountMatrix = new HashMap<Integer, Map<Integer, Integer>> (newVocab.size());
		conceptTopicCountMatrix = new HashMap<Integer, Map<Integer, Integer>> (documents.size());
		topicDocumentCountMatrix = new HashMap<Integer, Map<Integer, Integer>> (topicIdList.size());
		
		boolean add = true;
		double [ ] conceptMultinomial = createMultinomialDistribution(wikiConceptsData.size());
		double [ ] topicMultinomial = createMultinomialDistribution(getNumTopics());
		logger.info("====== Gibbs Sampling initialization START ======");
		int documentId = -1;
		for (Instance doc : documents) {
			documentId++;
			logger.info("document: " + documentId);
			FeatureVector words = (FeatureVector) doc.getData();
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word
				String word = wordLookupTable.lookupObject(wordId).toString(); // the word 
				if (!lessFrequentWords.containsKey(wordId) && newVocab.contains(word)) {
					/// Sample a Concept index and update the count///
					int conceptId = doSampleIndex(conceptMultinomial);
					int preConceptTermSumCount = getMapValue(conceptTermSumCount, conceptId);
					conceptTermSumCount.put(conceptId, preConceptTermSumCount + 1);   /// n ( c ) += 1

					/// Sample a Topic index  and update the counts///
					int topicId = doSampleIndex(topicMultinomial);
					int preTopicConceptSumCount = getMapValue(topicConceptSumCount, topicId);
					topicConceptSumCount.put(topicId, preTopicConceptSumCount + 1);   /// n ( k ) += 1
					int preDocumentTopicSumCount = getMapValue(documentTopicSumCount, documentId);
					documentTopicSumCount.put(documentId, preDocumentTopicSumCount + 1);   /// n ( d ) += 1
//					String word = wordLookupTable.lookupObject(wordId).toString(); // the word 
//					System.out.print("id: " + wordId + " " + word);  
//					System.out.print(words.getAlphabet().lookupObject(wordId) + "  ");  
//					System.out.println("val:" + words.valueAtLocation(wordCntr));  // frequency of the word in the doc
					
					/// Store the word concept and topic as a property /// 
					doc.setProperty(wordId + "", conceptId + "," + topicId);

					/// Update the Concept-Word count, Topic-Concept count and Document-Topic count ///
					updateWordConceptCount(wordConceptCountMatrix, wordId, conceptId, add);  /// n ( c )_w += 1
					updateConceptTopicCount(conceptTopicCountMatrix, conceptId, topicId, add);  /// n ( k )_c += 1
					updateTopicDocumentCount(topicDocumentCountMatrix, topicId, documentId, add);   /// n ( d )_k += 1
				} // end of if (!lessFrequentWords.containsKey(wordId))
			} // end of for (wordCntr)
		} // end of for (Instance doc : documents)
		logger.info("====== Gibbs Sampling initialization END ======");
	} // end of doGibbsSamplingInitialization
	
	
	/*************************************************
	 * @param topicDocumentCountMatrix
	 * @param topicId
	 * @param documentId
	 * @param add
	 */
	private void updateTopicDocumentCount(Map<Integer, Map<Integer, Integer>> topicDocumentCountMatrix, int topicId, int documentId, boolean add) {
		if (add) {
			if (topicDocumentCountMatrix.get(topicId) != null) {
				Map<Integer, Integer> documentCounts = topicDocumentCountMatrix.get(topicId);
				if (documentCounts.get(documentId) != null) {
					int preVal = documentCounts.get(documentId);
					documentCounts.put(documentId, preVal + 1);
					topicDocumentCountMatrix.put(topicId, documentCounts);
				}else {
					documentCounts.put(documentId, 1);
					topicDocumentCountMatrix.put(topicId, documentCounts);
				} // end of if
			}else {
				Map<Integer, Integer> documentCounts = new HashMap<Integer, Integer>();
				documentCounts.put(documentId, 1);
				topicDocumentCountMatrix.put(topicId, documentCounts);
			} // end of if
		}else {
			if (topicDocumentCountMatrix.get(topicId) != null) {
				Map<Integer, Integer> documentCounts = topicDocumentCountMatrix.get(topicId);
				if (documentCounts.get(documentId) != null) {
					int preVal = documentCounts.get(documentId);
					if (preVal > 0) {
						documentCounts.put(documentId, preVal - 1);
						topicDocumentCountMatrix.put(topicId, documentCounts);
					} // end of if
				} // end of if
			} // end of if
		}  // end of if (add)
	} // end of updateTopicDocumentCount
	

	public void updateConceptTopicCount(Map<Integer, Map<Integer, Integer>> conceptTopicCountMatrix, int conceptId, int topicId, boolean add) {
		if (add) {
			if (conceptTopicCountMatrix.get(conceptId) != null) {
				Map<Integer, Integer> topicCounts = conceptTopicCountMatrix.get(conceptId);
				if (topicCounts.get(topicId) != null) {
					int preVal = topicCounts.get(topicId);
					topicCounts.put(topicId, preVal + 1);
					conceptTopicCountMatrix.put(conceptId, topicCounts);
				}else {
					topicCounts.put(topicId, 1);
					conceptTopicCountMatrix.put(conceptId, topicCounts);
				} // end of if
			}else {
				Map<Integer, Integer> topicCounts = new HashMap<Integer, Integer>();
				topicCounts.put(topicId, 1);
				conceptTopicCountMatrix.put(conceptId, topicCounts);
			} // end of if
		}else {
			if (conceptTopicCountMatrix.get(conceptId) != null) {
				Map<Integer, Integer> topicCounts = conceptTopicCountMatrix.get(conceptId);
				if (topicCounts.get(topicId) != null) {
					int preVal = topicCounts.get(topicId);
					if (preVal > 0) {
						topicCounts.put(topicId, preVal - 1);
						conceptTopicCountMatrix.put(conceptId, topicCounts);
					} // end of if
				} // end of if
			} // end of if
		}  // end of if (add)
	} // end of updateConceptTopicCount
	
	
	
	/*************************************************
	 * @param wordConceptCountMatrix
	 * @param wordId
	 * @param conceptId
	 * @param add
	 */
	public void updateWordConceptCount(Map<Integer, Map<Integer, Integer>> wordConceptCountMatrix, int wordId, int conceptId, boolean add) {
		if (add) {
			if (wordConceptCountMatrix.get(wordId) != null) {
				Map<Integer, Integer> conceptCounts = wordConceptCountMatrix.get(wordId);
				if (conceptCounts.get(conceptId) != null) {
					int preVal = conceptCounts.get(conceptId);
					conceptCounts.put(conceptId, preVal + 1);
					wordConceptCountMatrix.put(wordId, conceptCounts);
				}else {
					conceptCounts.put(conceptId, 1);
					wordConceptCountMatrix.put(wordId, conceptCounts);
				} // end of if
			}else {
				Map<Integer, Integer> conceptCounts = new HashMap<Integer, Integer>();
				conceptCounts.put(conceptId, 1);
				wordConceptCountMatrix.put(wordId, conceptCounts);
			} // end of if
		}else {
			if (wordConceptCountMatrix.get(wordId) != null) {
				Map<Integer, Integer> conceptCounts = wordConceptCountMatrix.get(wordId);
				if (conceptCounts.get(conceptId) != null) {
					int preVal = conceptCounts.get(conceptId);
					if (preVal > 0) {
						conceptCounts.put(conceptId, preVal - 1);
						wordConceptCountMatrix.put(wordId, conceptCounts);
					} // end of if
				} // end of if
			} // end of if
		}  // end of if (add)
	} // end of updateWordConceptCount

	public void updateSumCount(Map<Integer, Integer> sumCount, int id, boolean add) {
		if (add) {
			int preVal = sumCount.get(id) != null ? sumCount.get(id) : 0;
			sumCount.put(id, preVal + 1);
		}else {
			int preVal = sumCount.get(id) != null ? sumCount.get(id) : 0;
			if (preVal > 0) {
				sumCount.put(id, preVal - 1);
			} // end of if
		} // end of if
	} // end of updateSumCount

	/**
	 * @param topicId
	 * @param documentId
	 */
	public void updateTopicDocumentCount(int topicId, int documentId, String operation) {
		try {
			ResultSet rs = entityManager.ifindTopicDocumentCount(topicId, documentId);
			int totalCount = 1;
			if (operation.equals("Add")) {
				/// Check if there is any record
				if (!rs.next()) {
					entityManager.persistTopicDocumentCount(topicId, documentId, totalCount);
				}else {
					totalCount += rs.getInt("total_count");
					entityManager.updateTopicDocumentCount(totalCount, topicId, documentId);
				} // end of if
			}else if (operation.equals("Subtract")) {
				if (rs.next()) {
					totalCount = rs.getInt("total_count") - 1;
					if (totalCount < 0) {
						totalCount = 0;
					} // end of if
					entityManager.updateTopicDocumentCount(totalCount, topicId, documentId);
				} // end of if
			} // end of if
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	} // end of updateTopicDocumentCount

	/**
	 * @param conceptId
	 * @param topicId
	 */
	public void updateConceptTopicCount(int conceptId, int topicId, String operation) {
		try {
			ResultSet rs = entityManager.ifindConceptTopicCount(conceptId, topicId);
			int totalCount = 1;
			if (operation.equals("Add")) {
				/// Check if there is any record
				if (!rs.next()) {
					entityManager.persistConceptTopicCount(conceptId, topicId, totalCount);
				}else {
					totalCount += rs.getInt("total_count");
					entityManager.updateConceptTopicCount(totalCount, conceptId, topicId);
				} // end of if
			}else if (operation.equals("Subtract")) {
				if (rs.next()) {
					totalCount = rs.getInt("total_count") - 1;
					if (totalCount < 0) {
						totalCount = 0;
					} // end of if
					entityManager.updateConceptTopicCount(totalCount, conceptId, topicId);
				} // end of if
			} // end of if
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	} // end of updateConceptTopicCount

	/**
	 * @param wordId
	 * @param conceptId
	 */
	public void updateWordConceptCount(int wordId, int conceptId, String operation) {
		try {
			ResultSet rs = entityManager.ifindWordConceptCount(wordId, conceptId);
			int totalCount = 1;
			if (operation.equals("Add")) {
				/// Check if there is any record
				if (!rs.next()) {
					entityManager.persistWordConceptCount(wordId, conceptId, totalCount);
				}else {
					totalCount += rs.getInt("total_count");
					entityManager.updateWordConceptCount(totalCount, wordId, conceptId);
				} // end of if
			}else if (operation.equals("Subtract")) {
				if (rs.next()) {
					totalCount = rs.getInt("total_count") - 1;
					if (totalCount < 0) {
						totalCount = 0;
					} // end of if
					entityManager.updateWordConceptCount(totalCount, wordId, conceptId);
				} // end of if
			} // end of if
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	} // end of updateWordConceptCount
	
	

	public int doSampleIndex(double [ ] multinomial) {
		Randoms r = new Randoms();
		return r.nextDiscrete(multinomial);
	} // end of doSampleIndex
	
	
	public double [ ] createMultinomialDistribution(int parameter) {
		double [ ] multinomial = new double [ parameter ];
		Randoms r = new Randoms();
		double sum = 0;
		for (int i = 0;i < multinomial.length;i++) {
			multinomial [ i ] = r.nextUniform();
			sum += multinomial [ i ];
//			System.out.println(multinomial [i]);
		} // end of for
		for (int i = 0;i < multinomial.length;i++) {
			multinomial [ i ] /= sum;
		} // end of for
		return multinomial;
	} // end of createMultinomialDistribution
	
	public Set<Integer> generateIdForConcepts() {
		conceptLookupTable = new HashMap<Integer, String>();
		int id = -1;
		for(Instance doc : wikiConceptsData) {
			id++;
			String fileName = doc.getName().toString();
			String conceptName = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".txt", "");
			doc.unLock();
			doc.setName(id);
			doc.lock();
			conceptLookupTable.put(id, conceptName);
		} // end of for
		conceptIdList = conceptLookupTable.keySet();
		return conceptIdList;
	} // end of generateIdForConcepts
	
	public void loadConceptsVocabulary() {
		wikiConceptsData = InstanceList.load(new File(SAVEDFILES_DIRECTORY_PATH + "conceptsVocabulary.ser"));
//		InstanceList documents =  InstanceList.load(new File(DATA_DIRECTORY_PATH + "sampledata.ser"));
//		InstanceList documents =  InstanceList.load(new File("/Users/Mehdi/Downloads/sample-data/web/en/r.txt"));
//		InstanceList documents =  InstanceList.load(new File("/Users/Mehdi/Downloads/sample-data/web/en/sampledata.ser"));
//		initializeCountArrays(documents.size());
//		return InstanceList.load(new File(SAVEDFILES_DIRECTORY_PATH + "conceptsVocabulary.ser"));
	} // end of loadConceptsVocabulary


	public void initializeCountArrays(int size) {
		documentTopicSumCount  = new HashMap<Integer, Integer>(size);
		topicConceptSumCount   = new HashMap<Integer, Integer>(getNumTopics());
		conceptTermSumCount    = new HashMap<Integer, Integer>(size);
		logger.info("Topics: " + getNumTopics());
	} // end of initializeCountArrays
	
	public void preprocessDocuemnts() {
		lessFrequentWords = new HashMap<Integer, Integer>(documents.getDataAlphabet().size());
		int minFrequency = Integer.valueOf(Configuration.getProperty("wordFrequency"));
		logger.info("initial Vocabulary Size: " + documents.getDataAlphabet().size());
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word
				int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
				int preFreq = lessFrequentWords.get(wordId) != null ? lessFrequentWords.get(wordId) : 0;
				lessFrequentWords.put(wordId, frequency + preFreq);
			} // end of for
		} // end of for
		Iterator<Map.Entry<Integer, Integer>> itr = lessFrequentWords.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			if (lessFrequentWords.get(entry.getKey()) > minFrequency) {
				itr.remove();
			} // end of if
		} // end of while
		logger.info("Vocabulary Size after removing less frequent words: " + (documents.getAlphabet().size() - lessFrequentWords.size()));
	} // end of preprocessDocuemnts

	public void importDocuments() {
		ImportData importer = new ImportData();
		documents = importer.readDirectory(new File(DATA_DIRECTORY_PATH));
		logger.info("Documents Vocabulary created successfully.");
		System.out.println("Vocabulary Size: " + documents.getDataAlphabet().size());
//		documents.save(new File(SAVEDFILES_DIRECTORY_PATH + "data.ser"));
//		logger.info("Document file serialized successfully.\n");
	} // end of importDocuments
	
	public Set<String> getIntersectionVocabulary() {
//		importDocuments();
//		loadConceptsVocabulary();
		Alphabet cVocab = wikiConceptsData.getDataAlphabet();
		Alphabet dVocab = documents.getDataAlphabet();
		Object [ ] entries = dVocab.toArray();
		int i = 1;
		Set<String> newVocab = new HashSet<String>(dVocab.size());
		for (Object obj : entries) {
			if (cVocab.contains(obj)) {
				newVocab.add(obj.toString());
				System.out.println("i: " + i + " " + obj.toString());
				i++;
			} // end of if
		} // end of for
		return newVocab;
	} // end of getIntersectionVocabulary
	
	public void checkAvailabilityOfWords() {
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word
				if (!lessFrequentWords.containsKey(wordId)) {
					String word = words.getAlphabet().lookupObject(wordId).toString(); // the word
					System.out.println("***** word: " + word);
					for (Instance c : wikiConceptsData) {
						FeatureVector d = (FeatureVector) c.getData();
						if (d.contains(word)) {
							System.out.println("concept: " + c.getName());
						}
					} // end of for
					System.out.println("====");
				}
			} // end of for (int wordCntr)
		} // end of for (Instance doc)
	} // end of checkAvailabilityOfWords
	
	public void createWikiConceptVocabulary() {
		ImportData importer = new ImportData();
		wikiConceptsData = importer.readDirectory(new File(WIKIPEDIA_DIRECTORY_PATH));
		logger.info("Concepts Vocabulary created successfully.");
		wikiConceptsData.save(new File(SAVEDFILES_DIRECTORY_PATH + "conceptsVocabulary.ser"));
		logger.info("Vocabulary file serialized successfully.");
		logger.info("Vocabulary Size: " + wikiConceptsData.getDataAlphabet().size());
		wikiConceptsData.getDataAlphabet().dump();
	} // end of createWikiConceptVocabulary
	
	
	/**
	 * @return the documentTopicSumCount
	 */
	public Map<Integer, Integer> getDocumentTopicSumCount() {
		return documentTopicSumCount;
	}

	/**
	 * @param documentTopicSumCount the documentTopicSumCount to set
	 */
	public void setDocumentTopicSumCount(Map<Integer, Integer> documentTopicSumCount) {
		this.documentTopicSumCount = documentTopicSumCount;
	}

	/**
	 * @return the topicConceptSumCount
	 */
	public Map<Integer, Integer> getTopicConceptSumCount() {
		return topicConceptSumCount;
	}

	/**
	 * @param topicConceptSumCount the topicConceptSumCount to set
	 */
	public void setTopicConceptSumCount(Map<Integer, Integer> topicConceptSumCount) {
		this.topicConceptSumCount = topicConceptSumCount;
	}

	/**
	 * @return the conceptTermSumCount
	 */
	public Map<Integer, Integer> getConceptTermSumCount() {
		return conceptTermSumCount;
	}

	/**
	 * @param conceptTermSumCount the conceptTermSumCount to set
	 */
	public void setConceptTermSumCount(Map<Integer, Integer> conceptTermSumCount) {
		this.conceptTermSumCount = conceptTermSumCount;
	}
	
	
	
	/**
	 * @return the topicIdList
	 */
	public Set<Integer> getTopicIdList() {
		return topicIdList;
	}



	/**
	 * @param topicIdList the topicIdList to set
	 */
	public void setTopicIdList(Set<Integer> topicIdList) {
		this.topicIdList = topicIdList;
	}



	public static void main(String[] args) {
		OntTopicModel tm = new OntTopicModel(30);
//		InstanceList instances = tm.loadDocuments();
//		Alphabet al = instances.getAlphabet();
//		tm.generateIdForConcepts(instances);
//		double m [] = tm.createMultinomialDistribution(instances.size());
//		for (int i = 0;i < m.length;i++) {
//		System.out.println("r:" + tm.doSampleIndex(m));
//		}
//		tm.runGibbsSampling(instances);
//		tm.generateIdForConcepts(instances);
		double [] a = {0.3, 0.1,0.7,0.5};
		Randoms rr = new Randoms();
		int d = rr.nextDiscrete(a, 1.6);
		System.out.println(d);
		
	}

}
