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
public class EntSum {

	int[] w = null;
	int[] d = null;
//	int[] z = null; // Array of all types corresponding to each word (object)
	int[] z1 = null; // Array of all types corresponding to each subject 
	int[] z2 = null; // Array of all types corresponding to each object 
	int[] p = null; //Array of all predicate corresponding to each word (object)
	int[] docIds = null;
	int[] wordIds = null;
	int[] wordsCounts = null;
	int D = 0;
	int W = 0;
//	int T = 0;  
	int T1 = 0;
	int T2 = 0;
	int P = 0;
	int N = 0;
	int nIterations = 0;
	int burnIn = 0;
	double ALPHA = 0;
	double BETA = 0;
//	double TAU = 0;
	double GAMMA = 0;
	SumModelParameters modelParameters = null;
	int[][] Npd = null; //doc X predicate (object X predicate)
//	int[][] Ntp = null;  //predicate X type 
	int[][] Nt1p = null;  //predicate X type 
	int[][] Nt2p = null;  //predicate X type 
	int[][] Nwt2 = null;  //type X word
	int[] Nd = null;  //Array of all documents with the size N
//	int[] Np = null; // Array of all predicates  
	int[] Np1 = null; // Array of all predicates  
	int[] Np2 = null; // Array of all predicates  
	int[] Nt2 = null; // Array of all types 
//	int[][] docEntMat = null;
//	int[][] entEntMat = null;
	double[] sumAlpha = null;
	double[][] alphaMat = null;
//	double[][] entEntSrMat = null;
	double[][] theta = null; // entity-topic distribution
//	double[][] phi = null; // topic-word distribution
	double[][] phi1 = null; // topic-word distribution
	double[][] phi2 = null; // topic-word distribution
	double[][] zeta = null; // document-entity distribution
	Random randomGenerator = null;
	
	
	private static final String docPredicateDomainRange = "/home/mehdi/EntitySummarization/evaluation/predicateDomainRange.txt"; 
	private static final String objectToTypeMapFileName = "/home/mehdi/EntitySummarization/evaluation/objToType.ser"; 
	
	// Map to store each predicate with a set of corresponding Domain
	Map <Integer, Set<Integer>> predicateDomainMap= new HashMap<Integer, Set<Integer>>();
	
	// Map to store each predicate with a set of corresponding Range
	Map<Integer, Set<Integer>> predicateRangeMap=new HashMap<Integer, Set<Integer>>();
	
	Map<Integer, Set<Integer>> objectTotypeMap = new HashMap<Integer,Set<Integer>>();
	
	
	String corpusEntitiesFile = "/home/mehdi/EntitySummarization/evaluation/corpusConceptsSr.txt";
	
	int showLine = 400000;
	final Logger logger = Logger.getLogger(EntSum.class.getName());
	String saveToDir = Configuration.getProperty("savedPosteriorFiles");
	
	

	public EntSum() {
		modelParameters = new SumModelParameters();
		this.w = modelParameters.w;
		this.d = modelParameters.d;
		this.D = modelParameters.D;
		this.W = modelParameters.W;
//		this.T = modelParameters.T;
		this.T1 = modelParameters.T1;
		this.T2 = modelParameters.T2;
		this.N = modelParameters.N;
		this.P = modelParameters.P;
		this.nIterations = modelParameters.nIterations;
		this.burnIn = modelParameters.burnIn;
		this.ALPHA = modelParameters.ALPHA;
		this.BETA = modelParameters.BETA;
		this.GAMMA = modelParameters.gamma;
		randomGenerator = new Random();
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
		saveMatrix(Nwt2, saveToDir + "topic-word.ser");
		saveMatrix(alphaMat, saveToDir + "alphaMat.ser");
	} // end of saveCountMatrices

	public void runGibbsSampling() {
		createPredicateDomainRangeMap();
		objectTotypeMap = loadObjectToTypeMap(objectToTypeMapFileName);
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
					samplePredicateAndTypeAssignment(d [w_i], p [w_i], z1 [w_i], z2 [w_i], w [w_i], w_i);
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
	
	
	public void samplePredicateAndTypeAssignment(int did, int pid, int t1id, int t2id, int wid, int w_i) {
		Set<Integer> wordTypes = objectTotypeMap.get(wid);
		double[] pr = null;
		int prSize = P * T1;
		//int [] eids = new int [diEnts.length];
		pr = allocateMemory(pr, prSize);
		updateCounts(did, pid, t1id, t2id, wid, -1);
		for (int ctr = 0; ctr < P; ctr++) {
			Set<Integer> predicateDomain = predicateDomainMap.get(ctr) != null ? predicateDomainMap.get(ctr) : new HashSet<Integer>() ;
			Set<Integer> predicateRange  = predicateRangeMap.get(ctr)  != null ? predicateRangeMap.get(ctr) : new HashSet<Integer>();
			// probability of predicate
			double pr_p = (Npd[did][ctr] + ALPHA) / (Nd[did] + P * ALPHA);
			for (int t_i = 0; t_i < T1; t_i++) {
				if(predicateDomain.contains(t_i) && predicateRange.contains(t_i) && wordTypes.contains(t_i)){
					// probability of subject type
					double pr_t1 = (Nt1p[ctr][t_i] + BETA) / (Np1[ctr] + T1 * BETA);
					// probability of object type
					double pr_t2 = (Nt2p[ctr][t_i] + BETA) / (Np2[ctr] + T2 * BETA);
					// probability of object
					double pr_w = (Nwt2[t_i][wid] + GAMMA) / (Nt2[t_i] + W * GAMMA);
					pr [ctr * T1 + t_i] = pr_p * pr_t1 * pr_t2 * pr_w;
				} // end of if 
			} // end of for t_i
		} // end of for ctr
		int pairIndex = sample(pr, randomGenerator.nextDouble());
		int newPredicate = pairIndex / T1;
		int newSubjectType = pairIndex % T1;
		int newObjectType = pairIndex % T1;
		p[w_i] = newPredicate;
		z1[w_i] = newSubjectType;
		z2[w_i] = newObjectType;
		updateCounts(did, newPredicate, newSubjectType, newObjectType, wid, +1);
		
//		if (pairIndex != -1) {
//			int newEntity = diEnts[pairIndex / T];
//			int newTopic = pairIndex % T;
//			e[w_i] = newEntity;
//			z[w_i] = newTopic;
//			updateCounts(did, newEntity, newTopic, wid, +1);
//		}else {
//			updateCounts(did, eid, tid, wid, +1);
//		}
		
		
	} // end of sampleEntityAndTopicAssignment
	
	
//	public void sampleEntityAndTopicAssignment(int did, int eid, int tid, int wid, int w_i) {
//		int[] diEnts = docEntMat[did];
//		double[] pr = null;
//		pr = allocateMemory(pr, P * T);
//		updateCounts(did, eid, tid, wid, -1);
//		for (int ctr = 0; ctr < diEnts.length; ctr++) {
//			int e_i = diEnts[ctr];
//			for (int t_i = 0; t_i < T; t_i++) {
//				double pr_e = (Ned[did][e_i] + TAU) / (Nd[did] + P * TAU);
//				double pr_z = (Nte[e_i][t_i] + alphaMat[e_i][t_i]) / (Ne[e_i] + sumAlpha[e_i]);
//				double pr_w = (Nwt[t_i][wid] + BETA) / (Nt[t_i] + W * BETA);
//				pr [e_i * T + t_i] = pr_e * pr_z * pr_w;
//			} // end of for t_i
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		int newEntity = pairIndex / T;
//		int newTopic = pairIndex % T;
//		e[w_i] = newEntity;
//		z[w_i] = newTopic;
//		updateCounts(did, newEntity, newTopic, wid, +1);
//	} // end of sampleEntityAndTopicAssignment
//	

public boolean hasValue(int[] arr, int val) {
		for (int v : arr) {
			if (v ==  val)
				return true;
		} // end of for
		return false;
	} // end of hasValue

//	public void sampleEntityAndTopicAssignment(int d_i, int w_i) {
////		computeTheta();
//		int[] diEnts = docEntMat[d_i];
//		double[] pr = null;
//		pr = allocateMemory(pr, P * T);
//		for (int ctr = 0; ctr < diEnts.length; ctr++) {
//			int e_i = diEnts[ctr];
//			// double newSumAlpha_e_i = 0;
//			for (int t_i = 0; t_i < T; t_i++) {
////				double oldAlpha_ei_ti = alphaMat[e_i][t_i];
////				alphaMat[e_i][t_i] = computeAlpha(e_i, t_i);
////				sumAlpha[e_i] = sumAlpha[e_i] - oldAlpha_ei_ti + alphaMat[e_i][t_i];
//				// newSumAlpha_e_i += alphaMat [e_i][t_i];
//				updateCounts(d_i, e_i, t_i, w_i, -1);
//				double pr_e = (Ned[d_i][e_i] + TAU) / (Nd[d_i] + P * TAU);
//				double pr_z = (Nte[e_i][t_i] + alphaMat[e_i][t_i]) / (Ne[e_i] + sumAlpha[e_i]);
//				double pr_w = (Nwt[t_i][w_i] + BETA) / (Nt[t_i] + W * BETA);
//				pr [e_i * T + t_i] = pr_e * pr_z * pr_w;
//			} // end of for t_i
//			// sumAlpha [e_i] = newSumAlpha_e_i;
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		int newEntity = pairIndex / T;
//		int newTopic = pairIndex % T;
//		updateCounts(d_i, newEntity, newTopic, w_i, 1);
//	} // end of sampleEntityAndTopicAssignment

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

//	public void optimizeParameter() {
//		computeTheta();
//		for (int e_i = 0; e_i < P; e_i++) {
//			int[] eiEnts = entEntMat [e_i];
//			double[] eiEntsSr = entEntSrMat [e_i];
//			for (int t_i = 0; t_i < T; t_i++) {
//				double pr1_z = theta [e_i][t_i];
//				double pr2_z = recomputeThetaProb(eiEnts, eiEntsSr, t_i, pr1_z);
//				while (pr2_z > pr1_z) {
//					pr1_z = pr2_z;
//					pr2_z = recomputeThetaProb(eiEnts, eiEntsSr, t_i, pr1_z);
//				} // end of while
//				if (pr1_z >= theta [e_i][t_i]) {
//					theta [e_i][t_i] = pr1_z;
//				}
//			} // end of for t_i
//		} // end of for e_i
//	} // end of optimizeParameter

//	private double recomputeThetaProb(int[] eiEnts, double[] eiEntsSr, int t_i, double pr1_z) {
//		double sumProb = 0;
//		double sumSr = 0;
//		for (int v = 0; v < eiEnts.length; v++) {
//			sumProb += eiEntsSr[v] * theta[eiEnts[v]][t_i];
//			sumSr += eiEntsSr[v];
//		} // end of for v
//		if (sumSr == 0)
//			sumSr = 1;
//		double pr2_z = ((1 - GAMMA) * pr1_z) + (GAMMA * (sumProb / sumSr));
//		return Math.round(pr2_z * 10000) / 10000.;
//	} // end of recomputeThetaProb

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

	public void updateCounts(int dId, int pId, int t1Id, int t2Id, int wId, int val) {
		Npd[dId][pId] = Npd[dId][pId] + val;
		Nt1p[pId][t1Id] = Nt1p[pId][t1Id] + val;
		Nt2p[pId][t2Id] = Nt2p[pId][t2Id] + val;
		Nwt2[t2Id][wId] = Nwt2[t2Id][wId] + val;
		Nd[dId] = Nd[dId] + val;
//		Np[pId] = Np[pId] + val;
		Np1[pId] = Np1[pId] + val;
		Np2[pId] = Np2[pId] + val;
//		Nt[tId] = Nt[tId] + val;
		Nt2[t2Id] = Nt2[t2Id] + val;
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

	public void computeTheta() {
		for (int d_i = 0; d_i < D; d_i++) {
			for (int p_i = 0; p_i < P; p_i++) {
				theta[d_i][p_i] = Math.round(((Npd[d_i][p_i] + ALPHA) / (Nd[d_i] + P * ALPHA)) * 10000) / 10000.;
//				theta[d_i][p_i] = Math.round(((Npd[d_i][p_i] + alphaMat[d_i][p_i]) / (Np[d_i] + sumAlpha[d_i])) * 10000) / 10000.;
			} // end of for t_i
		} // end of for e_i
	} // end of computeTheta

	public void computePhi1() {
		for (int p_i = 0; p_i < P; p_i++) {
			for (int t_i = 0; t_i < T1; t_i++) {
				phi1[p_i][t_i] = Math.round(((Nt1p[p_i][t_i] + BETA) / (Np1[p_i] + T1 * BETA)) * 10000) / 10000.;
			} // end of for w_i
		} // end of for t_i
	} // end of computePhi1
	
	public void computePhi2() {
		for (int p_i = 0; p_i < P; p_i++) {
			for (int t_i = 0; t_i < T2; t_i++) {
				phi1[p_i][t_i] = Math.round(((Nt2p[p_i][t_i] + BETA) / (Np2[p_i] + T2 * BETA)) * 10000) / 10000.;
			} // end of for w_i
		} // end of for t_i
	} // end of computePhi2
	
	
	public void computeZeta() {
		for (int t_i = 0; t_i < T2; t_i++) {
			for (int w_i = 0; w_i < W; w_i++) {
				zeta[t_i][w_i] = Math.round(((Nwt2[t_i][w_i] + GAMMA) / (Nd[t_i] + W * GAMMA)) * 10000) / 10000.;
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

	public void writeToCSV() {
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
				for (int t_i = 0; t_i < T1; t_i++) {
					sumProb [p_i] += Math.round(phi1 [p_i][t_i] * 10000) / 10000.;
				} // end of for t_i
			} // end of for p_i
			
			csvFile = new FileWriter("/home/mehdi/EntitySummarization/evaluation/csv/phi1.csv");
			for (int p_i = 0; p_i < P; p_i++) {
				String line = predicateNames.get(p_i) + " ";
				for (int t_i = 0; t_i < T1; t_i++) {
					phi1 [p_i][t_i] = (Math.round(phi1 [p_i][t_i] * 10000) / 10000.) / sumProb[p_i];
					line += phi1 [p_i][t_i] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for p_i
			csvFile.close();
			
			sumProb = null;
			sumProb = allocateMemory(sumProb, P);
			for (int p_i = 0; p_i < P; p_i++) {
				for (int t_i = 0; t_i < T2; t_i++) {
					sumProb [p_i] += Math.round(phi2 [p_i][t_i] * 10000) / 10000.;
				} // end of for t_i
			} // end of for p_i
			
			csvFile = new FileWriter("/home/mehdi/EntitySummarization/evaluation/csv/phi2.csv");
			for (int p_i = 0; p_i < P; p_i++) {
				String line = predicateNames.get(p_i) + " ";
				for (int t_i = 0; t_i < T2; t_i++) {
					phi2 [p_i][t_i] = (Math.round(phi2 [p_i][t_i] * 10000) / 10000.) / sumProb[p_i];
					line += phi2 [p_i][t_i] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for p_i
			csvFile.close();
			
			sumProb = null;
			sumProb = allocateMemory(sumProb, T2);
			for (int t_i = 0; t_i < T2; t_i++) {
				for (int w_i = 0; w_i < W; w_i++) {
					sumProb [t_i] += Math.round(zeta [t_i][w_i] * 10000) / 10000.;
				} // end of for w_i
			} // end of for t_i
			
			csvFile = new FileWriter("/home/mehdi/EntitySummarization/evaluation/csv/zeta.csv");
			for (int w_i = 0; w_i < W; w_i++) {
				String line = "";
				for (int t_i = 0; t_i < T2; t_i++) {
					zeta [t_i][w_i] = (Math.round(zeta [t_i][w_i] * 10000) / 10000.) / sumProb[t_i];
					line += wordNames.get(w_i).split(" ")[0] + " " + zeta [t_i][w_i] + ",";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for w_i
			csvFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	} // end of writeToCSV

	public void savePosteriorDistribution() {
		saveMatrix(theta, saveToDir + "thetaProb.ser");
		saveMatrix(phi1, saveToDir + "phi1Prob.ser");
		saveMatrix(phi2, saveToDir + "phi2Prob.ser");
		saveMatrix(zeta, saveToDir + "zetaProb.ser");
	} // end of savePosteriorDistribution

	public void loadPosteriorDistribution() {
		theta = loadDoubleMatrix("thetaProb.ser");
//		phi   = loadDoubleMatrix("phiProb.ser");
		phi1   = loadDoubleMatrix("phi1Prob.ser");
		phi2   = loadDoubleMatrix("phi2Prob.ser");
		zeta  = loadDoubleMatrix("zetaProb.ser");
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
	} // end of loadDoubleMatrix


	public void computePosteriorDistribution() {
		computeTheta();
		computePhi1();
		computePhi2();
		computeZeta();
	} // end of computePosteriorDistribution

	public void initializeGibbsSampling() {
		
		//processCorpusEntityFile();
		System.out.println(P);
		
		System.out.print("Gibbs sampling initialization...");
		
		for (int i = 0; i < N; i++) {
//			z[i] = randomGenerator.nextInt(T1); // generate random number in
			z1[i] = randomGenerator.nextInt(T1); // generate random number in
			z2[i] = randomGenerator.nextInt(T2); // generate random number in
												// [0,T)
			p[i] = randomGenerator.nextInt(P);
		} // end of for

		for (int i = 0; i < N; i++) {
			Npd[d[i]][p[i]] = Npd[d[i]][p[i]] + 1;
//			Ntp[p[i]][z[i]] = Ntp[p[i]][z[i]] + 1;
			Nt1p[p[i]][z1[i]] = Nt1p[p[i]][z1[i]] + 1;
			Nt2p[p[i]][z2[i]] = Nt2p[p[i]][z2[i]] + 1;
			Nwt2[z2[i]][w[i]] = Nwt2[z2[i]][w[i]] + 1;
			Nd[d[i]]++; //Adding one unit to 
//			Np[p[i]]++;
			Np1[p[i]]++;
			Np2[p[i]]++;
			Nt2[z2[i]]++;
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
//		Ntp = allocateMemory(Ntp, P, T);
		Nt1p = allocateMemory(Nt1p, P, T1);
		Nt2p = allocateMemory(Nt2p, P, T2);
		Nwt2 = allocateMemory(Nwt2, T2, W);
		Nd = allocateMemory(Nd, D);
//		Np = allocateMemory(Np, P);
		Np1 = allocateMemory(Np1, P);
		Np2 = allocateMemory(Np2, P);
		Nt2 = allocateMemory(Nt2, T2);
//		z = allocateMemory(z, N);
		z1 = allocateMemory(z1, N);
		z2 = allocateMemory(z2, N);
		p = allocateMemory(p, N);
//		alphaMat = allocateMemory(alphaMat, P, T, ALPHA);
//		sumAlpha = allocateMemory(sumAlpha, P, T * ALPHA);
		zeta = allocateMemory(zeta, T2, W);
		theta = allocateMemory(theta, D, P);
// 		phi = allocateMemory(phi, T, W);
		phi1 = allocateMemory(phi1, P, T1);
		phi2 = allocateMemory(phi2, P, T2);
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
	
	
public void createPredicateDomainRangeMap(){
	//Reading from PredicateDomainRange  file (PredicateID , DomainID, RangeID)
	BufferedReader br = null;
	FileReader fr = null;

	try {
		String str;
		br = new BufferedReader(new FileReader(docPredicateDomainRange));
		while ((str = br.readLine()) != null) {
			String[] a = str.split(" ");
			if (predicateDomainMap.get(Integer.valueOf(a[0]))==null){
				Set<Integer> domainSet =new HashSet<Integer>();
				domainSet.add(Integer.valueOf(a[1]));
				predicateDomainMap.put(Integer.valueOf(a[0]), domainSet);
				}else{
					Set<Integer> domainSet= predicateDomainMap.get(Integer.valueOf(a[0]));
					domainSet.add(Integer.valueOf(a[1]));
					predicateDomainMap.put(Integer.valueOf(a[0]), domainSet);
				}
			
			if (predicateRangeMap.get(Integer.valueOf(a[0]))==null){
				Set<Integer> rangeSet =new HashSet<Integer>();
				rangeSet.add(Integer.valueOf(a[2]));
				predicateRangeMap.put(Integer.valueOf(a[0]), rangeSet);
				}else{
					Set<Integer> rangeSet= predicateRangeMap.get(Integer.valueOf(a[0]));
					rangeSet.add(Integer.valueOf(a[2]));
					predicateRangeMap.put(Integer.valueOf(a[0]), rangeSet);
				}
		
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
	
	
	
	
	
	}
	
	
	
	
	
	
	

}
