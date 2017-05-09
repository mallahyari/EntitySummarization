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
public class Copy_OntTopicModel_V2 extends SimpleLDA{

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(Copy_OntTopicModel_V2.class.getName());
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
	
	
	public Copy_OntTopicModel_V2(int numberOfTopics) {
		super(numberOfTopics);
		
	}
	
	public void initializeTopicsAndCocepts() {
		importDocuments();
		loadConceptsVocabulary();
//		topicIdList = wiki.loadCategories();
		conceptIdList = generateIdForConcepts();
		topicIdNameMap = wiki.loadCategoryIdNameLookupTable();
		topicNameIdMap = wiki.loadCategoryNameIdLookupTable();
		topicConceptMap = wiki.loadCategoryConceptLookupTable();
		topicIdList = getTopicIds();
		this.numTopics = topicIdList.size();
//		System.out.println("numTopics: " + numTopics);
	} // end of initializeTopicsAndCocepts
	
	
	
	/*************************************************
	 * @return
	 */
	public Set<Integer> getTopicIds() {
//		Set<String> concepts = new HashSet<String>(conceptLookupTable.values());
//		Set<String> topicnames = wiki.findCategoriesByConceptName(concepts);
		String [] topicnames = {"Information_retrieval", "Information_theory", "Machine_learning", "Statistical_natural_language_processing",
				"Data_mining"};
		Set<Integer> topicIds = new HashSet<Integer>();
		int i = 1;
		for (String topic : topicnames) {
			if (topicNameIdMap.get(topic) != null) {
				int topicId = topicNameIdMap.get(topic);
				topicIds.add(topicId);
				System.out.println("i: " + (i++) + "  topic: " + topic + " topicId: " +  topicId);
			}else {
//				System.out.println("NULL for: " + topic);
			}
		} // end of for
//		System.out.println("num of topics: " + topicIds.size());
		return topicIds;
	} // end of getTopicIds

	public void runGibbsSampling() throws Exception {
		entityManager = new PersistentLayerImpl();
		Alphabet wordLookupTable = documents.getDataAlphabet();
		int numOfIterations = Integer.valueOf(Configuration.getProperty("numOfIteration"));
		
		double alpha = 50 / (topicIdList.size());
		double beta = 0.01;
		double gamma = 0.01;
		double betaGamma = beta * gamma;
		
		/// Gibbs Sampling initialization ///
		doGibbsSamplingInitialization(wordLookupTable);

		
		/// Gibbs sampling over burn-in period and sampling period while ///
		
		int documentId = -1;
		int numOfWords = wordLookupTable.size() - lessFrequentWords.size();
		int numOfTopics = getNumTopics();
		int numOfConcepts = wikiConceptsData.size();
		double betaVocab = beta * numOfWords;
		/// q(c): Un-normilized weight for P(c|w); Sigma q(c) = s + r + q ///
		Map<Integer, Double> s = new HashMap<Integer, Double> (numOfConcepts);
		Map<Integer, Double> r = new HashMap<Integer, Double> (numOfConcepts);
		Map<Integer, Double> q = new HashMap<Integer, Double> (numOfConcepts);
		Map<Integer, Double> coff = new HashMap<Integer, Double> (numOfConcepts);
		Map<Integer, Integer> nk_c = new HashMap<Integer, Integer> (numOfConcepts);   /// n(k)_c array
		
		/// Un-normilized weight for P(z|c,w) ///
		Map<Integer, Double> z = new HashMap<Integer, Double> (numOfTopics);
		Map<Integer, Integer> zcAssignment = new HashMap<Integer, Integer> (numOfTopics);  // array to keep the concepts assignment for topics
		
		boolean add = true;
		boolean subtract = false;
		double s_i = 0;
		double r_i = 0;
		double q_i = 0;
		double sumOfS = 0;
		double sumOfR = 0;
		double sumOfQ = 0;
		double sumOfZ = 0;
		int wcCount = 0;
		int ctCount = 0;
		int tdCount = 0;
		boolean cacheFlag = false;
		String conceptName = "";
		int wordId = -1;
		int currentConcept = -1;
		int currentTopic = -1;
		int newConceptId = -1;
		int newTopicId = -1;
		double probZ = 0.0;
		double z_i = 0;
		Map<Integer, Integer> wcCountMap = null;
		FeatureVector cVocab = null;
		logger.info("=====> Gibbs Sampling iterations START <=====");
		for (int iteration = 0; iteration < numOfIterations; iteration++) {
			logger.info("iteration " + iteration);
			documentId = -1;
			for (Instance doc : documents) {
				documentId++;
				logger.info("document: " + documentId);
				Map<Integer, Integer> tdCountMap = getTopicCountByDocumentId(documentId);
				FeatureVector words = (FeatureVector) doc.getData();
				for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
					wordId = words.indexAtLocation(wordCntr); // id of the word
					if (!lessFrequentWords.containsKey(wordId)) {
						String word = wordLookupTable.lookupObject(wordId).toString(); // the word 
						String [ ] currentAssignment = doc.getProperty(wordId + "").toString().split(",");
						currentConcept = Integer.valueOf(currentAssignment [ 0 ]);
						currentTopic = Integer.valueOf(currentAssignment [ 1 ]);
//						tdCount = getTopicDocumentCount(currentTopic, documentId);  // n(d)_k
						updateSumCount(topicConceptSumCount, currentTopic, subtract);  	 // n(k) -= 1
						updateSumCount(documentTopicSumCount, documentId, subtract);   	 // n(d) -= 1
						updateConceptTopicCount(conceptTopicCountMatrix, currentConcept, currentTopic, subtract);  // n(k)_c -= 1
						updateTopicDocumentCount(topicDocumentCountMatrix, currentTopic, documentId, subtract);    // n(d)_k -= 1
						/// P(z) ///
						logger.info(".");
						for (int topicId : topicIdList) {
							Map<Integer, Integer> ctCountMap = getConceptCountByTopicId(topicId);
							tdCount = tdCountMap.get(topicId) != null ? tdCountMap.get(topicId) : 0;
							probZ = tdCount * alpha; 
							currentAssignment = doc.getProperty(wordId + "").toString().split(",");
							currentConcept = Integer.valueOf(currentAssignment [ 0 ]);
							//						currentTopic = Integer.valueOf(currentAssignment [ 1 ]);
							updateWordConceptCount(wordConceptCountMatrix, wordId, currentConcept, subtract);  // n(c)_w -= 1
							updateSumCount(conceptTermSumCount, currentConcept, subtract); // n(c) -= 1
							if (!cacheFlag) {
								cacheFlag = true;
//								for (int conceptId : conceptIdList) {
								for (Instance concept : wikiConceptsData) {
									int conceptId = Integer.parseInt(concept.getName().toString());
									cVocab = (FeatureVector) concept.getData();
									wcCountMap = getWordCountByConceptId(conceptId);
									/// calculate the P(z,c) = P(z) * P(c|z) = P(z) * P(c|z,w) = P(z) * P(c|z) * P(w|c) ///
									wcCount = wcCountMap.get(wordId) != null ? wcCountMap.get(wordId) : 0;   // n(c)_w
									ctCount = ctCountMap.get(conceptId) != null ? ctCountMap.get(conceptId) : 0;  // n(k)_c
									
//									wcCount = getWordConceptCount(wordId, conceptId);   // n(c)_w
//									ctCount = getConceptTopicCount(conceptId, topicId);  // n(k)_c
									if (conceptTermSumCount.get(conceptId) == null) {
										conceptTermSumCount.put(conceptId, 0);
									} // end of if
									coff.put(conceptId, 1 / (betaVocab + conceptTermSumCount.get(conceptId)));
									nk_c.put(conceptId, ctCount); 
									
//									conceptName = conceptLookupTable.get(conceptId);
									/// Check to see if w is a priori member of a concept in the ontology ///
									/// and also to see if c is a priori member of a category in the ontology ///
//									if (!topicConceptMap.get(topicId).contains(conceptName) || !cVocab.contains(word)) {
									if (!cVocab.contains(word)) {
										s.put(conceptId, 0.0);
										r.put(conceptId, 0.0);
										q.put(conceptId, 0.0);
										continue;
									} // end of if

									s_i = betaGamma * coff.get(conceptId) * probZ;
									s.put(conceptId, s_i);
									sumOfS += s_i;
									if (ctCount == 0) {
										r.put(conceptId, 0.0);
									}else {
										r_i = (ctCount * gamma) * coff.get(conceptId) * probZ;
										r.put(conceptId, r_i);
										sumOfR += r_i;
									} // end of if
									if (wcCount == 0) {
										q.put(conceptId, 0.0);
									}else {
										q_i = ((beta + ctCount) * wcCount) * coff.get(conceptId) * probZ;
										q.put(conceptId, q_i);
										sumOfQ += q_i;
									} // end of if
								} // end of for (conceptId)
								
								/// Sample a new concept from P(c|w) and update the counts ///
								newConceptId = doConceptSampling(s, r, q, sumOfS, sumOfR, sumOfQ);
								doc.setProperty(wordId + "", newConceptId + "," + currentTopic);
								updateWordConceptCount(wordConceptCountMatrix, wordId, newConceptId, add);  // n(c)_w += 1
								updateSumCount(conceptTermSumCount, newConceptId, add); // n(c)   += 1

								/// Update r arrayList  for new concept-Topic assignment ///
								ctCount = ctCountMap.get(newConceptId) != null ? ctCountMap.get(newConceptId) : 0;
								sumOfR -= r.get(newConceptId);
								r.put(newConceptId, (ctCount + 1) * gamma * coff.get(newConceptId));
								sumOfR += r.get(newConceptId);
								
								/// P(z|c,w) = P(z) * P(c|z) * P(w|c)
								z_i = s.get(newConceptId) + r.get(newConceptId) + q.get(newConceptId);
								z.put(topicId, z_i);
								zcAssignment.put(topicId, newConceptId);
								sumOfZ += z_i;
							}else {
								/// calculate the P(z,c) = P(z) * P(c|z) = P(z) * P(c|z,w) = P(z) * P(c|z) * P(w|c) ///
//								for (int conceptId : conceptIdList) {
								for (Instance concept : wikiConceptsData) {
									int conceptId = Integer.parseInt(concept.getName().toString());
									cVocab = (FeatureVector) concept.getData();
									wcCountMap = getWordCountByConceptId(conceptId);
//									conceptName = conceptLookupTable.get(conceptId);
									
									/// Check to see if w is a priori member of a concept in the ontology ///
									/// and also to see if c is a priori member of a category in the ontology ///
//									if (!topicConceptMap.get(topicId).contains(conceptName) || !cVocab.contains(word)) {
									if (!cVocab.contains(word)) {
										sumOfR -= r.get(conceptId);
										r.put(conceptId, 0.0);
										sumOfQ -= q.get(conceptId);
										q.put(conceptId, 0.0);
										continue;
									} // end of if
									
									wcCount = wcCountMap.get(wordId) != null ? wcCountMap.get(wordId) : 0;   // n(c)_w
									ctCount = ctCountMap.get(conceptId) != null ? ctCountMap.get(conceptId) : 0;  // n(k)_c
									
//									wcCount = getWordConceptCount(wordId, conceptId);   // n(c)_w
//									ctCount = getConceptTopicCount(conceptId, topicId);  // n(k)_c
									double coffVal = coff.get(conceptId);
									if (ctCount == 0) {
										sumOfR -= r.get(conceptId);
										r.put(conceptId, 0.0);
										sumOfQ -= q.get(conceptId);
										q.put(conceptId, 0.0);
										continue;
									}else if (ctCount != nk_c.get(conceptId)) {
										sumOfR -= r.get(conceptId);
										r_i = (ctCount * gamma) * coffVal * probZ;
										r.put(conceptId, r_i);
										sumOfR += r_i;
									} // end of if
									if (wcCount == 0) {
										if (q.get(conceptId) != 0) {
											sumOfQ -= q.get(conceptId);
											q.put(conceptId, 0.0);
										} // end of if
									}else {
										sumOfQ -= q.get(conceptId);
										q_i = ((beta + ctCount) * wcCount) * coffVal * probZ;
										q.put(conceptId, q_i);
										sumOfQ += q_i;
									} // end of if
								} // end of for (conceptId)
								/// Sample a new concept from P(c|w) and update the counts ///
								newConceptId = doConceptSampling(s, r, q, sumOfS, sumOfR, sumOfQ);
								doc.setProperty(wordId + "", newConceptId + "," + currentTopic);
								updateWordConceptCount(wordConceptCountMatrix, wordId, newConceptId, add);  // n(c)_w += 1
								updateSumCount(conceptTermSumCount, newConceptId, add); // n(c)   += 1

								/// Update r arrayList  for new concept-Topic assignment ///
								ctCount = ctCountMap.get(newConceptId) != null ? ctCountMap.get(newConceptId) : 0;
								sumOfR -= r.get(newConceptId);
								r.put(newConceptId, (ctCount + 1) * gamma * coff.get(newConceptId));
								sumOfR += r.get(newConceptId);
								
								/// P(z|c,w) = P(z) * P(c|z) * P(w|c)
								z_i = s.get(newConceptId) + r.get(newConceptId) + q.get(newConceptId);
								z.put(topicId, z_i);
								zcAssignment.put(topicId, newConceptId);
								sumOfZ += z_i;
							}// end of if (cacheFalg)
						} // end of for (topicId) 
//						cacheFlag = false;
						newTopicId = doTopicSampling(z, sumOfZ);
						newConceptId = zcAssignment.get(newTopicId);
						updateTopicDocumentCount(topicDocumentCountMatrix, newTopicId, documentId, add);  // n(d)_k += 1
						updateSumCount(documentTopicSumCount, newTopicId, add);  						  // n(d)   += 1
						updateWordConceptCount(wordConceptCountMatrix, wordId, newConceptId, add);  	  // n(c)_w += 1
						updateSumCount(conceptTermSumCount, newConceptId, add);  						  // n(c)   += 1
						updateConceptTopicCount(conceptTopicCountMatrix, newConceptId, newTopicId, add);  // n(k)_c += 1
						updateSumCount(topicConceptSumCount, newTopicId, add);   						  // n(k)   += 1
						sumOfZ = 0;
						z.clear();
						zcAssignment.clear();
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
	
	public void loadResults() {
		Map<Integer, Integer> wcsumCountMx = loadSumCountMatrix("conceptTermSumCount.ser");
		Map<Integer, Integer> ctsumCountMx = loadSumCountMatrix("topicConceptSumCount.ser");
		Map<Integer, Integer> tdsumCountMx = loadSumCountMatrix("documentTopicSumCount.ser");
		System.out.println("conceptTermSumCount size: " + wcsumCountMx.size());
		System.out.println("topicConceptSumCount size: " + ctsumCountMx.size());
		System.out.println("documentTopicSumCount size: " + tdsumCountMx.size());
		wordConceptCountMatrix = loadCountMatrix("wordConceptCountMatrix.ser");
		conceptTopicCountMatrix = loadCountMatrix("conceptTopicCountMatrix.ser");
		topicDocumentCountMatrix = loadCountMatrix("topicDocumentCountMatrix.ser");
		System.out.println("wordConceptCountMatrix size: " + wordConceptCountMatrix.size());
		System.out.println("conceptTopicCountMatrix size: " + conceptTopicCountMatrix.size());
		System.out.println("topicDocumentCountMatrix size: " + topicDocumentCountMatrix.size());
		calculateWordConceptProbability(wordConceptCountMatrix, wcsumCountMx, 0.01);
		
//		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = tdCountMx.entrySet().iterator();
//		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = ctCountMx.entrySet().iterator();
//		Iterator<Entry<Integer, Map<Integer, Integer>>> itr = wordConceptCountMatrix.entrySet().iterator();
//		while (itr.hasNext()) {
//			Map.Entry<Integer, Map<Integer, Integer>> wcRecord = itr.next();
//			System.out.println(wcRecord.getValue().size());
//		} // end of while
		
	} // end of loadResults
	
	public void calculateWordConceptProbability(Map<Integer, Map<Integer, Integer>> wcCountMx, Map<Integer, Integer> wcSumCountMx, double gamma){
		importDocuments();
		loadConceptsVocabulary();
		Map<Integer, String> conLookupTable = loadConceptLookupTable("conceptLookupTable.ser");
		try {
			Alphabet vocabulary = documents.getDataAlphabet();
			FileWriter fout = new FileWriter(SAVEDFILES_DIRECTORY_PATH + "wordConceptProb.csv");
			int vocabSize = vocabulary.size();
			String tuple = "";
			Object [ ] words = vocabulary.toArray();
			logger.info("calculating Probabilities...");
			for (Object word : words) {
				int wordId = vocabulary.lookupIndex(word);
				Map<Integer, Integer> wcAssignments = wcCountMx.get(wordId);
				if (wcAssignments != null) {
					Iterator<Entry<Integer, Integer>> itr = wcAssignments.entrySet().iterator();
					while (itr.hasNext()) {
						Entry<Integer, Integer> rec = itr.next();
						int conceptId = rec.getKey();
						int count = rec.getValue();
						double prob = (count + gamma) / (wcSumCountMx.get(conceptId) + (vocabSize * wcSumCountMx.size()));
						tuple = word + "," + conLookupTable.get(conceptId) + "," + prob + "\n";
						fout.write(tuple);
						fout.flush();
					} // end of while
				} // end of if
			} // end of for
			fout.close();
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
            FileOutputStream outputFile = new FileOutputStream(SAVEDFILES_DIRECTORY_PATH + fileName);
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
            FileOutputStream outputFile = new FileOutputStream(SAVEDFILES_DIRECTORY_PATH + fileName);
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
            FileOutputStream outputFile = new FileOutputStream(SAVEDFILES_DIRECTORY_PATH + fileName);
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
	 */
	public void doGibbsSamplingInitialization(Alphabet wordLookupTable) {
		/// initialize the Matrices ///
		wordConceptCountMatrix = new HashMap<Integer, Map<Integer, Integer>> (wordLookupTable.size());
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
				if (!lessFrequentWords.containsKey(wordId)) {
					/// Sample a Concept index and update the count///
					int conceptId = doSampleIndex(conceptMultinomial);
					int preConceptTermSumCount = conceptTermSumCount.get(conceptId) != null ? conceptTermSumCount.get(conceptId) : 0;
					conceptTermSumCount.put(conceptId, preConceptTermSumCount + 1);   /// n ( c ) += 1

					/// Sample a Topic index  and update the counts///
					int topicId = doSampleIndex(topicMultinomial);
					int preTopicConceptSumCount = topicConceptSumCount.get(topicId) != null ? topicConceptSumCount.get(topicId) : 0;
					topicConceptSumCount.put(topicId, preTopicConceptSumCount + 1);   /// n ( k ) += 1
					int preDocumentTopicSumCount = documentTopicSumCount.get(documentId) != null ? documentTopicSumCount.get(documentId) : 0;
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
		initializeCountArrays(documents.size());
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
	
	public void getIntersectionVocabulary() {
//		importDocuments();
//		loadConceptsVocabulary();
//		Alphabet cVocab = wikiConceptsData.getDataAlphabet();
//		Alphabet dVocab = documents.getDataAlphabet();
//		Object [ ] entries = dVocab.toArray();
//		int i = 1;
//		List<String> newVocab = new ArrayList<String>(dVocab.size());
//		for (Object obj : entries) {
//			if (cVocab.contains(obj)) {
//				newVocab.add(obj.toString());
//				System.out.println("i: " + i + " " + obj.toString());
//				i++;
//			}
//		} // end of for
		preprocessDocuemnts();
		
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
		Copy_OntTopicModel_V2 tm = new Copy_OntTopicModel_V2(30);
//		InstanceList instances = tm.loadDocuments();
//		Alphabet al = instances.getAlphabet();
//		tm.generateIdForConcepts(instances);
//		double m [] = tm.createMultinomialDistribution(instances.size());
//		for (int i = 0;i < m.length;i++) {
//		System.out.println("r:" + tm.doSampleIndex(m));
//		}
//		tm.runGibbsSampling(instances);
//		tm.generateIdForConcepts(instances);
		for (int i = 0; i < 20; i++) {
			logger.info("lda");
			
		} // end of for (i)
	}

}
