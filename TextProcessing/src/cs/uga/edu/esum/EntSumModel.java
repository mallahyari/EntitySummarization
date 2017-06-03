package cs.uga.edu.esum;

/**
 * 
 */


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
import java.util.Scanner;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import cs.uga.edu.properties.Configuration;
//
/**
 * @author mehdi
 *
 */
public class EntSumModel { 

	int[] w = null;
	int[] d = null;
//	int[] z = null; // Array of all types corresponding to each word (object)
	int[] p = null; //Array of all predicate corresponding to each word (object)
	int[] docIds = null;
	int[] wordIds = null;
	int[] wordsCounts = null;
	int D = 0;
	int W = 0;
//	int T = 0;  
	int P = 0;
	int N = 0;
	int nIterations = 0;
	int burnIn = 0;
	double ALPHA = 0;
	double BETA = 0;
//	double TAU = 0;
	ModelParameters modelParameters = null;
	int[][] Npd = null; //doc X predicate (object X predicate)
	int[][] Nwp = null;  //type X word
	int[] Nd = null;  //Array of all documents with the size N
	int[] Np = null; // Array of all predicates  
	int[][] predicateObjectWeight = null;
	int[] sumPredObjWeight = null;  
	double[] sumAlpha = null;
	double[] sumBeta = null;
	double[][] alphaMat = null;
	double[][] theta = null;
	double[][] phi = null; // topic-word distribution
	
	Random randomGenerator = null;
	EntityProc entProc = null;
	
	private static final String docPredicateDomainRange = "/home/mehdi/EntitySummarization/evaluation/predicateDomainRange.txt"; 
	private static final String predicateObjectFileName = "/home/mehdi/EntitySummarization/evaluation/predicateObject.ser"; 

	
	// Map to store each predicate with a set of corresponding Domain
	Map <Integer, Set<Integer>> predicateDomainMap= new HashMap<Integer, Set<Integer>>();
	
	// Map to store each predicate with a set of corresponding Objects
	Map<Integer, Set<Integer>> objectToPredicateMap = new HashMap<Integer, Set<Integer>>();
	
	Map<Integer, Set<Integer>> objectTotypeMap = new HashMap<Integer,Set<Integer>>();
	
	
	String corpusEntitiesFile = "/home/mehdi/EntitySummarization/evaluation/corpusConceptsSr.txt";
	
	int showLine = 400000;
	final Logger logger = Logger.getLogger(EntSumModel.class.getName());
	String saveToDir = Configuration.getProperty("savedPosteriorFiles");
	
	

	public EntSumModel() {
		entProc = new EntityProc();
		modelParameters = new ModelParameters();
		this.w = modelParameters.w;
		this.d = modelParameters.d;
		this.D = modelParameters.D;
		this.W = modelParameters.W;
		this.N = modelParameters.N;
		this.P = modelParameters.P;
		this.nIterations = modelParameters.nIterations;
		this.burnIn = modelParameters.burnIn;
		this.ALPHA = modelParameters.ALPHA;
		this.BETA = modelParameters.BETA;
		randomGenerator = new Random();
		predicateObjectWeight = entProc.loadIntMatrix(entProc.predicateObjectWeightFileName);
		sumPredObjWeight = allocateMemory(sumPredObjWeight, P);
		for (int i = 0; i < predicateObjectWeight.length; i++) {
			for (int j = 0; j < predicateObjectWeight[0].length; j++) {
				sumPredObjWeight[i] += sumPredObjWeight[i] + predicateObjectWeight[i][j];
			} // end of for (j)
		} // end of for (i)
	}

	public void run() {
		runGibbsSampling();
		computePosteriorDistribution();
		savePosteriorDistribution();
//		saveCountMatrices();
	} // end of run

	public void saveCountMatrices() {
		saveMatrix(Npd, saveToDir + "document-entity.ser");
//		saveMatrix(Ntp, saveToDir + "entity-topic.ser");
		saveMatrix(Nwp, saveToDir + "topic-word.ser");
		saveMatrix(alphaMat, saveToDir + "alphaMat.ser");
	} // end of saveCountMatrices

	public void runGibbsSampling() {
		objectToPredicateMap = entProc.loadPredicateToObjectMap(entProc.objectPredicateFileName);
		FileHandler fh;
		try {
			fh = new FileHandler("/home/mehdi/mylog.txt");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			for (int itr = 0; itr < nIterations; itr++) {
//				logger.log(Level.INFO, "\nIteration " + (itr + 1));
				System.out.println("Iteration " + (itr + 1));
//				if ((itr + 1 == burnIn)) {
//					computeTheta();
//					computeAlpha();
//				}
				for (int w_i = 0; w_i < N; w_i++) {
					if ((w_i + 1) % showLine == 0) {
						System.out.println((w_i + 1) + " done!.");
					}
					samplePredicateAssignment(d [w_i], p [w_i], w [w_i], w_i);
				} // end of for w_i
//				if ((itr + 1) >= burnIn) {
//					optimizeParameter();
//					computeAlpha();
//				}
			} // end of for itr
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of runGibbsSampling
	
	public void samplePredicateAssignment(int did, int pid, int wid, int w_i) {
		Set<Integer> wordPredicate = objectToPredicateMap.get(wid);
		double[] pr = null;
		pr = allocateMemory(pr, P);
		updateCounts(did, pid, wid, -1);
		double sum = 0;
			
		for (int ctr = 0; ctr < P; ctr++) {
			if (wordPredicate.contains(ctr)) {
				// probability of predicate
				double pr_p = (Npd[did][ctr] + ALPHA) / (Nd[did] + P * ALPHA);
				if (pr_p == 0)
					System.out.println("==== ");
				// probability of object
				double pr_w = (Nwp[ctr][wid] + predicateObjectWeight[ctr][wid] * BETA) / (Np[ctr] + sumPredObjWeight[ctr] * BETA);
				if (pr_w == 0)
					System.out.println("==== ");
				pr [ctr] = pr_p * pr_w;
				sum += pr[ctr];
			} // end of if
		} // end of for ctr
		if(sum == 0)
			System.out.println("===="); 
		int newPredicate   = sample(pr, randomGenerator.nextDouble());
		p[w_i] = newPredicate;
		updateCounts(did, newPredicate, wid, +1);
	} // end of samplePredicateAndTypeAssignment
	
	
//	public void samplePredicateAndTypeAssignment_OLD(int did, int pid, int t1id, int t2id, int wid, int w_i) {
//		Set<Integer> wordTypes = objectTotypeMap.get(wid);
//		double[] pr = null;
//		int prSize = P * T2;
//		//int [] eids = new int [diEnts.length];
//		pr = allocateMemory(pr, prSize);
//		updateCounts(did, pid, t2id, wid, -1);
//		for (int ctr = 0; ctr < P; ctr++) {
////			Set<Integer> predicateDomain = predicateDomainMap.get(ctr) != null ? predicateDomainMap.get(ctr) : new HashSet<Integer>() ;
//			Set<Integer> predicateRange  = predicateRangeMap.get(ctr)  != null ? predicateRangeMap.get(ctr) : new HashSet<Integer>();
////			System.out.println("id:" + ctr + ", wid: " + wid + " " + predicateRange);
//			// probability of predicate
//			double pr_p = (Npd[did][ctr] + ALPHA) / (Nd[did] + P * ALPHA);
//			for (int t_i = 0; t_i < T2; t_i++) {
//				if(predicateRange.contains(t_i) && wordTypes.contains(t_i)){
////				if(predicateDomain.contains(t_i) && predicateRange.contains(t_i) && wordTypes.contains(t_i)){
//					// probability of subject type
////					double pr_t1 = (Nt1p[ctr][t_i] + BETA) / (Np1[ctr] + T1 * BETA);
//					// probability of object type
//					double pr_t2 = (Nt2p[ctr][t_i] + BETA) / (Np2[ctr] + T2 * BETA);
//					// probability of object
//					double pr_w = (Nwt2[t_i][wid] + GAMMA) / (Nt2[t_i] + W * GAMMA);
//					pr [ctr * T2 + t_i] = pr_p * pr_t2 * pr_w;
////					pr [ctr * T2 + t_i] = pr_p * pr_t1 * pr_t2 * pr_w;
//				} // end of if 
//			} // end of for t_i
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		if (pairIndex == -1) {
////			System.out.println("-1");
//			pairIndex = randomGenerator.nextInt(pr.length);
//		}
//		int newPredicate = pairIndex / T2;
////		int newSubjectType = pairIndex % T2;
//		int newObjectType = pairIndex % T2;
//		p[w_i] = newPredicate;
////		z1[w_i] = newSubjectType;
//		z2[w_i] = newObjectType;
//		updateCounts(did, newPredicate, newObjectType, wid, +1);
////		updateCounts(did, newPredicate, newSubjectType, newObjectType, wid, +1);
//		
////		if (pairIndex != -1) {
////			int newEntity = diEnts[pairIndex / T];
////			int newTopic = pairIndex % T;
////			e[w_i] = newEntity;
////			z[w_i] = newTopic;
////			updateCounts(did, newEntity, newTopic, wid, +1);
////		}else {
////			updateCounts(did, eid, tid, wid, +1);
////		}
//	} // end of samplePredicateAndTypeAssignment_OLD
//	
	



//	public void sampleEntityAndTopicAssignmentBurnIn(int d_i, int w_i) {
//		int[] diEnts = docEntMat[d_i];
//		double[] pr = null;
//		pr = allocateMemory(pr, P * T);
//		for (int ctr = 0; ctr < diEnts.length; ctr++) {
//			int e_i = diEnts[ctr];
//			for (int t_i = 0; t_i < T; t_i++) {
//				updateCounts(d_i, e_i, t_i, w_i, -1);
//				double pr_e = (Npd[d_i][e_i] + TAU) / (Nd[d_i] + P * TAU);
//				double pr_z = (Ntp[e_i][t_i] + alphaMat[e_i][t_i])
//						/ (Np[e_i] + sumAlpha[e_i]);
//				double pr_w = (Nwt2[t_i][w_i] + BETA) / (Nt2[t_i] + W * BETA);
//				pr[e_i * T + t_i] = pr_e * pr_z * pr_w;
//			} // end of for t_i
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		int newEntity = pairIndex / T;
//		int newTopic = pairIndex % T;
//		updateCounts(d_i, newEntity, newTopic, w_i, 1);
//	} // end of sampleEntityAndTopicAssignmentBurnIn


	
	public void updateCounts(int dId, int pId, int wId, int val) {
		Npd[dId][pId] = Npd[dId][pId] + val;
		Nwp[pId][wId] = Nwp[pId][wId] + val;
		Nd[dId] = Nd[dId] + val;
		Np[pId] = Np[pId] + val;
	} // end of updateCounts


//	public void computeAlpha() {
//		for (int e_i = 0; e_i < P; e_i++) {
//			for (int t_i = 0; t_i < T; t_i++) {
//				int[] neighbors = entEntMat[e_i];
//				double sumProb = 0;
//				for (int v = 0; v < neighbors.length; v++) {
//					sumProb += theta[neighbors[v]][t_i];
//					// sumProb += (Nte [v][t_i] + alphaMat [v][t_i]) / (Ne [v] +
//					// sumAlpha [v]);
//				} // end of for
//				double oldAlpha_ei_ti = alphaMat[e_i][t_i];
//				if (neighbors.length > 0) {
//					alphaMat [e_i][t_i] = ((1 - EPSILON) * ALPHA) + (EPSILON * (T / neighbors.length) * sumProb);
//				}else {
//					alphaMat [e_i][t_i] = (1 - EPSILON) * ALPHA;
//				}
//				sumAlpha[e_i] = sumAlpha[e_i] - oldAlpha_ei_ti + alphaMat[e_i][t_i];
//			} // end of for t_i
//		} // end of for e_i
//	} // end of computeAlpha

//	public double computeAlpha(int e_i, int t_i) {
//		int[] neighbors = entEntMat[e_i];
//		double sumProb = 0;
//		for (int v = 0; v < neighbors.length; v++) {
//			sumProb += theta[v][t_i];
//			// sumProb += (Nte [v][t_i] + alphaMat [v][t_i]) / (Ne [v] +
//			// sumAlpha [v]);
//		} // end of for
//		return ((EPSILON * ALPHA) + ((1 - EPSILON) * (T / neighbors.length) * sumProb));
//	} // end of computeAlpha

//	public void computeTheta(int e_i, int t_i) {
//		theta[e_i][t_i] = (Ntp[e_i][t_i] + alphaMat[e_i][t_i]) / (Np[e_i] + sumAlpha[e_i]);
//	} // end of computeTheta

	
//	public void writeToDisk() {
//		Nwt = loadIntMatrix("topic-word.ser");
//		try {
//			FileWriter csvFile = new FileWriter("/home/mehdi/entlda/csv/Nwt.csv");
//			for (int w_i = 0; w_i < W; w_i++) {
//				String line = w_i + " ";
//				for (int t_i = 0; t_i < T; t_i++) {
//					line += Nwt [t_i][w_i] + " ";
//				} // end of for t_i
//				csvFile.write(line + "\n");
//				csvFile.flush();
//			} // end of for e_i
//			csvFile.close();
//		}catch (IOException e) {
//			
//		}
//		
//	}

	public void writeToCSV() {
		Map <String, Map<String, Double>> probabilitySearch = new HashMap <String, Map<String, Double>>();
		
		loadPosteriorDistribution();
		List<String> entityNames = modelParameters.readFile("/home/mehdi/EntitySummarization/evaluation/docToId.txt");
		List<String> predicateNames = modelParameters.readFile("/home/mehdi/EntitySummarization/evaluation/predicateToId.txt");
		List<String> wordNames = modelParameters.readFile("/home/mehdi/EntitySummarization/evaluation/wordToID.txt");
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, D);
		for (int d_i = 0; d_i < D; d_i++) {
			for (int p_i = 0; p_i < P; p_i++) {
				sumProb[d_i] += theta [d_i][p_i];
			} // end of for t_i
		} // end of for e_i
		try {
			FileWriter csvFile = new FileWriter("/home/mehdi/EntitySummarization/evaluation/csv/theta.csv");
			for (int d_i = 0; d_i < D; d_i++) {
				String line = entityNames.get(d_i) + " ";
				for (int p_i = 0; p_i < P; p_i++) {
					theta [d_i][p_i] = theta [d_i][p_i] / sumProb[d_i];
					line += theta [d_i][p_i] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for e_i
			csvFile.close();
			sumProb = null;
			sumProb = allocateMemory(sumProb, P);
			for (int p_i = 0; p_i < P; p_i++) {
				for (int w_i = 0; w_i < W; w_i++) {
					sumProb [p_i] += Math.round(phi [p_i][w_i] * 10000) / 10000.;
				} // end of for w_i
			} // end of for p_i
			csvFile = new FileWriter("/home/mehdi/EntitySummarization/evaluation/csv/phi.csv");
			for (int w_i = 0; w_i < W; w_i++) {
				String line = "";
				for (int p_i = 0; p_i < P; p_i++) {
					phi [p_i][w_i] = (Math.round(phi [p_i][w_i] * 10000) / 10000.) / sumProb[p_i];
					String word = wordNames.get(w_i).split(" ")[0];
					line += word + " " + predicateNames.get(p_i) + " " + phi [p_i][w_i] + ",";
					if (probabilitySearch.get(word) == null) {
						Map<String,Double> preToProb = new HashMap<String,Double>();
						preToProb.put(predicateNames.get(p_i), phi [p_i][w_i]);
						probabilitySearch.put(word, preToProb);
					}else {
						Map<String,Double> preToProb = probabilitySearch.get(word);
						preToProb.put(predicateNames.get(p_i), phi [p_i][w_i]);
						probabilitySearch.put(word, preToProb);
					}
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for w_i
			csvFile.close();
			boolean flag = true;
			System.out.println("Enter a word and predicate:");
			Scanner sc = new Scanner(System.in);
			while(flag) {
				String word = sc.nextLine();
				String predicate = sc.nextLine();
				System.out.println("Probability = " + probabilitySearch.get(word).get(predicate));
				if (word.equals("QUIT")) flag = false;
			}
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	} // end of writeToCSV

	
//	public void samplePredicateAndTypeAssignment_OLD(int did, int pid, int t1id, int t2id, int wid, int w_i) {
//		Set<Integer> wordTypes = objectTotypeMap.get(wid);
//		double[] pr = null;
//		int prSize = P * T2;
//		//int [] eids = new int [diEnts.length];
//		pr = allocateMemory(pr, prSize);
//		updateCounts(did, pid, t2id, wid, -1);
//		for (int ctr = 0; ctr < P; ctr++) {
////			Set<Integer> predicateDomain = predicateDomainMap.get(ctr) != null ? predicateDomainMap.get(ctr) : new HashSet<Integer>() ;
//			Set<Integer> predicateRange  = predicateRangeMap.get(ctr)  != null ? predicateRangeMap.get(ctr) : new HashSet<Integer>();
////			System.out.println("id:" + ctr + ", wid: " + wid + " " + predicateRange);
//			// probability of predicate
//			double pr_p = (Npd[did][ctr] + ALPHA) / (Nd[did] + P * ALPHA);
//			for (int t_i = 0; t_i < T2; t_i++) {
//				if(predicateRange.contains(t_i) && wordTypes.contains(t_i)){
////				if(predicateDomain.contains(t_i) && predicateRange.contains(t_i) && wordTypes.contains(t_i)){
//					// probability of subject type
////					double pr_t1 = (Nt1p[ctr][t_i] + BETA) / (Np1[ctr] + T1 * BETA);
//					// probability of object type
//					double pr_t2 = (Nt2p[ctr][t_i] + BETA) / (Np2[ctr] + T2 * BETA);
//					// probability of object
//					double pr_w = (Nwt2[t_i][wid] + GAMMA) / (Nt2[t_i] + W * GAMMA);
//					pr [ctr * T2 + t_i] = pr_p * pr_t2 * pr_w;
////					pr [ctr * T2 + t_i] = pr_p * pr_t1 * pr_t2 * pr_w;
//				} // end of if 
//			} // end of for t_i
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		if (pairIndex == -1) {
////			System.out.println("-1");
//			pairIndex = randomGenerator.nextInt(pr.length);
//		}
//		int newPredicate = pairIndex / T2;
////		int newSubjectType = pairIndex % T2;
//		int newObjectType = pairIndex % T2;
//		p[w_i] = newPredicate;
////		z1[w_i] = newSubjectType;
//		z2[w_i] = newObjectType;
//		updateCounts(did, newPredicate, newObjectType, wid, +1);
////		updateCounts(did, newPredicate, newSubjectType, newObjectType, wid, +1);
//		
////		if (pairIndex != -1) {
////			int newEntity = diEnts[pairIndex / T];
////			int newTopic = pairIndex % T;
////			e[w_i] = newEntity;
////			z[w_i] = newTopic;
////			updateCounts(did, newEntity, newTopic, wid, +1);
////		}else {
////			updateCounts(did, eid, tid, wid, +1);
////		}
//	} // end of samplePredicateAndTypeAssignment_OLD
//	
	

public boolean hasValue(int[] arr, int val) {
		for (int v : arr) {
			if (v ==  val)
				return true;
		} // end of for
		return false;
	} // end of hasValue


//	public void sampleEntityAndTopicAssignmentBurnIn(int d_i, int w_i) {
//		int[] diEnts = docEntMat[d_i];
//		double[] pr = null;
//		pr = allocateMemory(pr, P * T);
//		for (int ctr = 0; ctr < diEnts.length; ctr++) {
//			int e_i = diEnts[ctr];
//			for (int t_i = 0; t_i < T; t_i++) {
//				updateCounts(d_i, e_i, t_i, w_i, -1);
//				double pr_e = (Npd[d_i][e_i] + TAU) / (Nd[d_i] + P * TAU);
//				double pr_z = (Ntp[e_i][t_i] + alphaMat[e_i][t_i])
//						/ (Np[e_i] + sumAlpha[e_i]);
//				double pr_w = (Nwt2[t_i][w_i] + BETA) / (Nt2[t_i] + W * BETA);
//				pr[e_i * T + t_i] = pr_e * pr_z * pr_w;
//			} // end of for t_i
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		int newEntity = pairIndex / T;
//		int newTopic = pairIndex % T;
//		updateCounts(d_i, newEntity, newTopic, w_i, 1);
//	} // end of sampleEntityAndTopicAssignmentBurnIn


	public int sample(double[] pr, double randSeed) {
		int l = pr.length;
		double[] cdf = new double[l];
		for (int i = 0; i < l; i++) {
			cdf[i] = pr[i];
		} // end of for
		for (int i = 1; i < l; i++) {
			cdf[i] += cdf[i - 1];
		} // end of for
		double u = randSeed * cdf[l - 1];
		for (int i = 0; i < l; i++) {
			if (cdf[i] > u) {
				return i;
			} // end of if
		} // end of for
		return -1;
	} // end of sample
	


//	public void computeAlpha() {
//		for (int e_i = 0; e_i < P; e_i++) {
//			for (int t_i = 0; t_i < T; t_i++) {
//				int[] neighbors = entEntMat[e_i];
//				double sumProb = 0;
//				for (int v = 0; v < neighbors.length; v++) {
//					sumProb += theta[neighbors[v]][t_i];
//					// sumProb += (Nte [v][t_i] + alphaMat [v][t_i]) / (Ne [v] +
//					// sumAlpha [v]);
//				} // end of for
//				double oldAlpha_ei_ti = alphaMat[e_i][t_i];
//				if (neighbors.length > 0) {
//					alphaMat [e_i][t_i] = ((1 - EPSILON) * ALPHA) + (EPSILON * (T / neighbors.length) * sumProb);
//				}else {
//					alphaMat [e_i][t_i] = (1 - EPSILON) * ALPHA;
//				}
//				sumAlpha[e_i] = sumAlpha[e_i] - oldAlpha_ei_ti + alphaMat[e_i][t_i];
//			} // end of for t_i
//		} // end of for e_i
//	} // end of computeAlpha

//	public double computeAlpha(int e_i, int t_i) {
//		int[] neighbors = entEntMat[e_i];
//		double sumProb = 0;
//		for (int v = 0; v < neighbors.length; v++) {
//			sumProb += theta[v][t_i];
//			// sumProb += (Nte [v][t_i] + alphaMat [v][t_i]) / (Ne [v] +
//			// sumAlpha [v]);
//		} // end of for
//		return ((EPSILON * ALPHA) + ((1 - EPSILON) * (T / neighbors.length) * sumProb));
//	} // end of computeAlpha

//	public void computeTheta(int e_i, int t_i) {
//		theta[e_i][t_i] = (Ntp[e_i][t_i] + alphaMat[e_i][t_i]) / (Np[e_i] + sumAlpha[e_i]);
//	} // end of computeTheta

	public void computeTheta() {
		for (int d_i = 0; d_i < D; d_i++) {
			for (int p_i = 0; p_i < P; p_i++) {
				theta[d_i][p_i] = Math.round(((Npd[d_i][p_i] + ALPHA) / (Nd[d_i] + P * ALPHA)) * 10000) / 10000.;
//				theta[d_i][p_i] = Math.round(((Npd[d_i][p_i] + alphaMat[d_i][p_i]) / (Np[d_i] + sumAlpha[d_i])) * 10000) / 10000.;
			} // end of for t_i
		} // end of for e_i
	} // end of computeTheta

	
	public void computePhi() {
		for (int p_i = 0; p_i < P; p_i++) {
			for (int w_i = 0; w_i < W; w_i++) {
				phi[p_i][w_i] = Math.round(((Nwp[p_i][w_i] + predicateObjectWeight[p_i][w_i] * BETA) / (Np[p_i] + sumPredObjWeight[p_i] * BETA)) * 10000) / 10000.;
			} // end of for e_i
		} // end of for d_i
	} // end of computeZeta
	
//	public void writeToDisk() {
//		Nwt = loadIntMatrix("topic-word.ser");
//		try {
//			FileWriter csvFile = new FileWriter("/home/mehdi/entlda/csv/Nwt.csv");
//			for (int w_i = 0; w_i < W; w_i++) {
//				String line = w_i + " ";
//				for (int t_i = 0; t_i < T; t_i++) {
//					line += Nwt [t_i][w_i] + " ";
//				} // end of for t_i
//				csvFile.write(line + "\n");
//				csvFile.flush();
//			} // end of for e_i
//			csvFile.close();
//		}catch (IOException e) {
//			
//		}
//		
//	}


	public void savePosteriorDistribution() {
		saveMatrix(theta, saveToDir + "thetaProb.ser");
		saveMatrix(phi, saveToDir + "phiProb.ser");
	} // end of savePosteriorDistribution

	public void loadPosteriorDistribution() {
		theta = loadDoubleMatrix("thetaProb.ser");
		phi   = loadDoubleMatrix("phiProb.ser");
	} // end of loadPosteriorDistribution

	public int[][] loadIntMatrix(String fileName) {
		System.out.println("Loading " + fileName + " into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(saveToDir + fileName);
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
	
	public double[][] loadDoubleMatrix(String fileName) {
		System.out.println("Loading " + fileName + " into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(saveToDir + fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			double[][] z = (double[][]) in.readObject();
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
	} // end of loadDoubleMatrix
	
	public void saveMatrix(int [][] mat, String fileName) {
		logger.info("Serializing Count Matrix...");
		try {
			File f = new File(fileName);
			if (f.exists()) {
				logger.info(fileName + " already exists");
				f.delete();
				logger.info(fileName + " deleted.");
			} // end of if
			FileOutputStream outputFile = new FileOutputStream(f);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(mat);
			out.close();
			logger.info("Matrix Serialized successfully.\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of saveCountMatrix
	
	public void saveMatrix(double [][] mat, String fileName) {
		logger.info("Serializing Matrix...");
		try {
			File f = new File(fileName);
			if (f.exists()) {
				logger.info(fileName + " already exists");
				f.delete();
				logger.info(fileName + " deleted.");
			} // end of if
			FileOutputStream outputFile = new FileOutputStream(f);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(mat);
			out.close();
			logger.info("Matrix Serialized successfully.\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of saveMatrix
	
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
	} // end of loadObjectToTypeMap


	public void computePosteriorDistribution() {
		computeTheta();
		computePhi();
	} // end of computePosteriorDistribution

	public void initializeGibbsSampling() {
		
		//processCorpusEntityFile();
		System.out.println(P);
		
		System.out.print("Gibbs sampling initialization...");
		
		for (int i = 0; i < N; i++) {
			p[i] = randomGenerator.nextInt(P);
		} // end of for

		for (int i = 0; i < N; i++) {
			Npd[d[i]][p[i]] = Npd[d[i]][p[i]] + 1;
			Nwp[p[i]][w[i]] = Nwp[p[i]][w[i]] + 1;
			Nd[d[i]]++; //Adding one unit to 
			Np[p[i]]++;
		} // end of for
		System.out.println("done!");
	} // end of gibbSampleInitialization

	public List<Integer> findIndexList(int[] arr, int id) {
		List<Integer> indexList = new ArrayList<Integer>(arr.length);
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == id) {
				indexList.add(i);
			}
		} // end of for
		return indexList;
	} // end of findIndexList

	public List<Integer> findIndexList(double[] arr, int id) {
		List<Integer> indexList = new ArrayList<Integer>(arr.length);
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == id) {
				indexList.add(i);
			}
		} // end of for
		return indexList;
	} // end of findIndexList

	public void initializeMatrices() {
		Npd = allocateMemory(Npd, D, P);
		Nwp = allocateMemory(Nwp, P, W);
		Nd = allocateMemory(Nd, D);
		Np = allocateMemory(Np, P);
//		z = allocateMemory(z, N);
		p = allocateMemory(p, N);
		theta = allocateMemory(theta, D, P);
 		phi = allocateMemory(phi, P, W);
	} // end of initializeMatrices

	public int[][] allocateMemory(int[][] ar, int r, int c) {
		ar = new int[r][c];
		for (int rCtr = 0; rCtr < r; rCtr++) {
			for (int cCtr = 0; cCtr < c; cCtr++) {
				ar[rCtr][cCtr] = 0;
			} // end of for cCtr
		} // end of for rCtr
		return ar;
	} // end of allocateMemory

	public int[] allocateMemory(int[] ar, int r) {
		ar = new int[r];
		for (int rCtr = 0; rCtr < r; rCtr++) {
			ar[rCtr] = 0;
		} // end of for rCtr
		return ar;
	} // end of allocateMemory

	public double[][] allocateMemory(double[][] ar, int r, int c) {
		ar = new double[r][c];
		for (int rCtr = 0; rCtr < r; rCtr++) {
			for (int cCtr = 0; cCtr < c; cCtr++) {
				ar[rCtr][cCtr] = 0;
			} // end of for cCtr
		} // end of for rCtr
		return ar;
	} // end of allocateMemory

	public double[][] allocateMemory(double[][] ar, int r, int c, double val) {
		ar = new double[r][c];
		for (int rCtr = 0; rCtr < r; rCtr++) {
			for (int cCtr = 0; cCtr < c; cCtr++) {
				ar[rCtr][cCtr] = val;
			} // end of for cCtr
		} // end of for rCtr
		return ar;
	} // end of allocateMemory

	public double[] allocateMemory(double[] ar, int r) {
		ar = new double[r];
		for (int rCtr = 0; rCtr < r; rCtr++) {
			ar[rCtr] = 0;
		} // end of for rCtr
		return ar;
	} // end of allocateMemory

	public double[] allocateMemory(double[] ar, int r, double val) {
		ar = new double[r];
		for (int rCtr = 0; rCtr < r; rCtr++) {
			ar[rCtr] = val;
		} // end of for rCtr
		return ar;
	} // end of allocateMemory
}
