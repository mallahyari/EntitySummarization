package cs.uga.edu.esum;

/**
 * 
 */


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
import java.util.ArrayList;
import java.util.Arrays;
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
	int[] z = null; // Array of all types corresponding to each word (object)
	int[] p = null; //Array of all predicate corresponding to each word (object)
	int[] docIds = null;
	int[] wordIds = null;
	int[] wordsCounts = null;
	int D = 0;
	int W = 0;
	int T = 0;
	int P = 0;
	int N = 0;
	int nIterations = 0;
	int burnIn = 0;
	double ALPHA = 0;
	double BETA = 0;
	double TAU = 0;
	double EPSILON = 0;
	double gamma = 0;
	SumModelParameters modelParameters = null;
	int[][] Npd = null; //doc X predicate (object X predicate)
	int[][] Ntp = null;  //predicate X type 
	int[][] Nwt = null;  //type X word
	int[] Nd = null;  //Array of all documents with the size N
	int[] Np = null; // Array of all predicates  
	int[] Nt = null; // Array of all types 
	int[][] docEntMat = null;
	int[][] entEntMat = null;
	double[] sumAlpha = null;
	double[][] alphaMat = null;
	double[][] entEntSrMat = null;
	double[][] theta = null; // entity-topic distribution
	double[][] phi = null; // topic-word distribution
	double[][] zeta = null; // document-entity distribution
	Random randomGenerator = null;
	
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
		this.T = modelParameters.T;
		this.N = modelParameters.N;
		this.P = modelParameters.P;
		this.nIterations = modelParameters.nIterations;
		this.burnIn = modelParameters.burnIn;
		this.ALPHA = modelParameters.ALPHA;
		this.BETA = modelParameters.BETA;
		this.TAU = modelParameters.TAU;
		this.EPSILON = modelParameters.EPSILON;
		this.gamma = modelParameters.gamma;
		randomGenerator = new Random();
	}

	public void run() {
		runGibbsSampling();
		computePosteriorDistribution();
		savePosteriorDistribution();
		saveCountMatrices();
	} // end of run

	public void saveCountMatrices() {
		saveMatrix(Npd, saveToDir + "document-entity.ser");
		saveMatrix(Ntp, saveToDir + "entity-topic.ser");
		saveMatrix(Nwt, saveToDir + "topic-word.ser");
		saveMatrix(alphaMat, saveToDir + "alphaMat.ser");
	} // end of saveCountMatrices

	public void runGibbsSampling() {
		FileHandler fh;
		try {
			fh = new FileHandler("/home/mehdi/mylog.txt");
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
			for (int itr = 0; itr < nIterations; itr++) {
//				logger.log(Level.INFO, "\nIteration " + (itr + 1));
				System.out.println("Iteration " + (itr + 1));
				if ((itr + 1 == burnIn)) {
					computeTheta();
					computeAlpha();
				}
				for (int w_i = 0; w_i < N; w_i++) {
					if ((w_i + 1) % showLine == 0) {
						System.out.println((w_i + 1) + " done!.");
					}
					if (docEntMat[d[w_i]].length > 0) {
						sampleEntityAndTopicAssignment(d [w_i], p [w_i], z [w_i], w [w_i], w_i);
					} // end of if
				} // end of for w_i
				if ((itr + 1) >= burnIn) {
					optimizeParameter();
					computeAlpha();
				}
			} // end of for itr
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of runGibbsSampling
	
	
	public void sampleEntityAndTopicAssignment(int did, int eid, int tid, int wid, int w_i) {
		int[] diEnts = docEntMat[did];
		double[] pr = null;
		int prSize = diEnts.length * T;
		//int [] eids = new int [diEnts.length];
		pr = allocateMemory(pr, prSize);
		updateCounts(did, eid, tid, wid, -1);
		for (int ctr = 0; ctr < diEnts.length; ctr++) {
			int e_i = diEnts[ctr];
			for (int t_i = 0; t_i < T; t_i++) {
				double pr_e = (Npd[did][e_i] + TAU) / (Nd[did] + P * TAU);
				double pr_z = (Ntp[e_i][t_i] + alphaMat[e_i][t_i]) / (Np[e_i] + sumAlpha[e_i]);
				double pr_w = (Nwt[t_i][wid] + BETA) / (Nt[t_i] + W * BETA);
				pr [ctr * T + t_i] = pr_e * pr_z * pr_w;
			} // end of for t_i
		} // end of for ctr
		int pairIndex = sample(pr, randomGenerator.nextDouble());
		int newEntity = diEnts[pairIndex / T];
		int newTopic = pairIndex % T;
		p[w_i] = newEntity;
		z[w_i] = newTopic;
		updateCounts(did, newEntity, newTopic, wid, +1);
		
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

	public void sampleEntityAndTopicAssignmentBurnIn(int d_i, int w_i) {
		int[] diEnts = docEntMat[d_i];
		double[] pr = null;
		pr = allocateMemory(pr, P * T);
		for (int ctr = 0; ctr < diEnts.length; ctr++) {
			int e_i = diEnts[ctr];
			for (int t_i = 0; t_i < T; t_i++) {
				updateCounts(d_i, e_i, t_i, w_i, -1);
				double pr_e = (Npd[d_i][e_i] + TAU) / (Nd[d_i] + P * TAU);
				double pr_z = (Ntp[e_i][t_i] + alphaMat[e_i][t_i])
						/ (Np[e_i] + sumAlpha[e_i]);
				double pr_w = (Nwt[t_i][w_i] + BETA) / (Nt[t_i] + W * BETA);
				pr[e_i * T + t_i] = pr_e * pr_z * pr_w;
			} // end of for t_i
		} // end of for ctr
		int pairIndex = sample(pr, randomGenerator.nextDouble());
		int newEntity = pairIndex / T;
		int newTopic = pairIndex % T;
		updateCounts(d_i, newEntity, newTopic, w_i, 1);
	} // end of sampleEntityAndTopicAssignmentBurnIn

	public void optimizeParameter() {
		computeTheta();
		for (int e_i = 0; e_i < P; e_i++) {
			int[] eiEnts = entEntMat [e_i];
			double[] eiEntsSr = entEntSrMat [e_i];
			for (int t_i = 0; t_i < T; t_i++) {
				double pr1_z = theta [e_i][t_i];
				double pr2_z = recomputeThetaProb(eiEnts, eiEntsSr, t_i, pr1_z);
				while (pr2_z > pr1_z) {
					pr1_z = pr2_z;
					pr2_z = recomputeThetaProb(eiEnts, eiEntsSr, t_i, pr1_z);
				} // end of while
				if (pr1_z >= theta [e_i][t_i]) {
					theta [e_i][t_i] = pr1_z;
				}
			} // end of for t_i
		} // end of for e_i
	} // end of optimizeParameter

	private double recomputeThetaProb(int[] eiEnts, double[] eiEntsSr, int t_i, double pr1_z) {
		double sumProb = 0;
		double sumSr = 0;
		for (int v = 0; v < eiEnts.length; v++) {
			sumProb += eiEntsSr[v] * theta[eiEnts[v]][t_i];
			sumSr += eiEntsSr[v];
		} // end of for v
		if (sumSr == 0)
			sumSr = 1;
		double pr2_z = ((1 - gamma) * pr1_z) + (gamma * (sumProb / sumSr));
		return Math.round(pr2_z * 10000) / 10000.;
	} // end of recomputeThetaProb

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

	public void updateCounts(int dId, int eId, int tId, int wId, int val) {
		Npd[dId][eId] = Npd[dId][eId] + val;
		Ntp[eId][tId] = Ntp[eId][tId] + val;
		Nwt[tId][wId] = Nwt[tId][wId] + val;
		Nd[dId] = Nd[dId] + val;
		Np[eId] = Np[eId] + val;
		Nt[tId] = Nt[tId] + val;
	} // end of updateCounts

	public void computeAlpha() {
		for (int e_i = 0; e_i < P; e_i++) {
			for (int t_i = 0; t_i < T; t_i++) {
				int[] neighbors = entEntMat[e_i];
				double sumProb = 0;
				for (int v = 0; v < neighbors.length; v++) {
					sumProb += theta[neighbors[v]][t_i];
					// sumProb += (Nte [v][t_i] + alphaMat [v][t_i]) / (Ne [v] +
					// sumAlpha [v]);
				} // end of for
				double oldAlpha_ei_ti = alphaMat[e_i][t_i];
				if (neighbors.length > 0) {
					alphaMat [e_i][t_i] = ((1 - EPSILON) * ALPHA) + (EPSILON * (T / neighbors.length) * sumProb);
				}else {
					alphaMat [e_i][t_i] = (1 - EPSILON) * ALPHA;
				}
				sumAlpha[e_i] = sumAlpha[e_i] - oldAlpha_ei_ti + alphaMat[e_i][t_i];
			} // end of for t_i
		} // end of for e_i
	} // end of computeAlpha

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

	public void computeTheta(int e_i, int t_i) {
		theta[e_i][t_i] = (Ntp[e_i][t_i] + alphaMat[e_i][t_i]) / (Np[e_i] + sumAlpha[e_i]);
	} // end of computeTheta

	public void computeTheta() {
		for (int e_i = 0; e_i < P; e_i++) {
			for (int t_i = 0; t_i < T; t_i++) {
				theta[e_i][t_i] = Math.round(((Ntp[e_i][t_i] + alphaMat[e_i][t_i]) / (Np[e_i] + sumAlpha[e_i])) * 10000) / 10000.;
			} // end of for t_i
		} // end of for e_i
	} // end of computeTheta

	public void computePhi() {
		for (int t_i = 0; t_i < T; t_i++) {
			for (int w_i = 0; w_i < W; w_i++) {
				phi[t_i][w_i] = Math.round(((Nwt[t_i][w_i] + BETA) / (Nt[t_i] + W * BETA)) * 10000) / 10000.;
			} // end of for w_i
		} // end of for t_i
	} // end of computePhi

	public void computeZeta() {
		for (int d_i = 0; d_i < D; d_i++) {
			for (int e_i = 0; e_i < P; e_i++) {
				zeta[d_i][e_i] = Math.round(((Npd[d_i][e_i] + TAU) / (Nd[d_i] + P * TAU)) * 10000) / 10000.;
			} // end of for e_i
		} // end of for d_i
	} // end of computeZeta
	
	public void writeToDisk() {
		Nwt = loadIntMatrix("topic-word.ser");
		try {
			FileWriter csvFile = new FileWriter("/home/mehdi/entlda/csv/Nwt.csv");
			for (int w_i = 0; w_i < W; w_i++) {
				String line = w_i + " ";
				for (int t_i = 0; t_i < T; t_i++) {
					line += Nwt [t_i][w_i] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for e_i
			csvFile.close();
		}catch (IOException e) {
			
		}
		
	}

	public void writeToCSV() {
		loadPosteriorDistribution();
		List<String> conceptNames = modelParameters.readFile("/home/mehdi/EntitySummarization/evaluation/corpusConceptsLookup.txt");
		List<String> wordNames = modelParameters.readFile("/home/mehdi/entlda/EntitySummarization/evaluation/vocabularyLookup.txt");
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, P);
		for (int e_i = 0; e_i < P; e_i++) {
			for (int t_i = 0; t_i < T; t_i++) {
				sumProb[e_i] += theta [e_i][t_i];
			} // end of for t_i
		} // end of for e_i
		try {
			FileWriter csvFile = new FileWriter("/home/mehdi/entlda/csv/theta.csv");
			for (int e_i = 0; e_i < P; e_i++) {
				String line = conceptNames.get(e_i) + " ";
				for (int t_i = 0; t_i < T; t_i++) {
					theta [e_i][t_i] = theta [e_i][t_i] / sumProb[e_i];
					line += theta [e_i][t_i] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for e_i
			csvFile.close();
			sumProb = null;
			sumProb = allocateMemory(sumProb, T);
			for (int t_i = 0; t_i < T; t_i++) {
				for (int w_i = 0; w_i < W; w_i++) {
					sumProb [t_i] += Math.round(phi [t_i][w_i] * 10000) / 10000.;
				} // end of for w_i
			} // end of for t_i
			
			csvFile = new FileWriter("/home/mehdi/entlda/csv/phi.csv");
			for (int w_i = 0; w_i < W; w_i++) {
				String line = "";
				for (int t_i = 0; t_i < T; t_i++) {
					phi [t_i][w_i] = (Math.round(phi [t_i][w_i] * 10000) / 10000.) / sumProb[t_i];
					line += wordNames.get(w_i).split("\t")[1] + " " + phi [t_i][w_i] + ",";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for w_i
			csvFile.close();
			
			sumProb = null;
			sumProb = allocateMemory(sumProb, D);
			csvFile = new FileWriter("/home/mehdi/entlda/csv/zeta.csv");
			for (int d_i = 0; d_i < D; d_i++) {
				for (int e_i = 0; e_i < P; e_i++) {
					sumProb[d_i] += zeta [d_i][e_i];
				} // end of for e_i
			} // end of for d_i
			for (int d_i = 0; d_i < D; d_i++) {
				String line = "";
				for (int e_i = 0; e_i < P; e_i++) {
					zeta [d_i][e_i] = zeta [d_i][e_i] / sumProb[d_i];
					line = conceptNames.get(e_i) + " ";
					line += zeta [d_i][e_i] + " ";
				} // end of for e_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for d_i
			csvFile.close();
			double [][] topDoc = null;
			topDoc = allocateMemory(topDoc, D, T);
			csvFile = new FileWriter("/home/mehdi/entlda/csv/topicDoc.csv");
			for (int d_i = 0; d_i < D; d_i++) {
				String line = "";
				for (int t_i = 0; t_i < T; t_i++) {
					double prob = 0;
					for (int e_i = 0; e_i < P; e_i++) {
						prob += theta[e_i][t_i] * zeta[d_i][e_i];
					} // end of for e_i
					topDoc[d_i][t_i] = prob;
					line += topDoc[d_i][t_i] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for d_i
			csvFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	} // end of writeToCSV

	public void savePosteriorDistribution() {
		saveMatrix(theta, saveToDir + "thetaProb.ser");
		saveMatrix(phi, saveToDir + "phiProb.ser");
		saveMatrix(zeta, saveToDir + "zetaProb.ser");
	} // end of savePosteriorDistribution

	public void loadPosteriorDistribution() {
		theta = loadDoubleMatrix("thetaProb.ser");
		phi   = loadDoubleMatrix("phiProb.ser");
		zeta  = loadDoubleMatrix("zetaProb.ser");
		alphaMat = loadDoubleMatrix("alphaMat.ser");
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

	public void computePosteriorDistribution() {
		//computeTheta();
		computeZeta();
		computePhi();
	} // end of computePosteriorDistribution

	public void initializeGibbsSampling() {
		
		//processCorpusEntityFile();
		System.out.println(P);
		
		System.out.print("Gibbs sampling initialization...");
		
		for (int i = 0; i < N; i++) {
			z[i] = randomGenerator.nextInt(T); // generate random number in
												// [0,T)
			p[i] = randomGenerator.nextInt(P);
		} // end of for

		for (int i = 0; i < N; i++) {
			System.out.println(d[i] + " " + p[i] + " "+z[i] + "  " + w[i]);
			Npd[d[i]][p[i]] = Npd[d[i]][p[i]] + 1;
			Ntp[p[i]][z[i]] = Ntp[p[i]][z[i]] + 1;
			Nwt[z[i]][w[i]] = Nwt[z[i]][w[i]] + 1;
			Nd[d[i]]++; //Adding one unit to 
			Np[p[i]]++;
			Nt[z[i]]++;
		} // end of for
		System.out.println("done!");
	} // end of gibbSampleInitialization

	public void processCorpusEntityFile() {
		List<String> corpusEntities = modelParameters.readFile(corpusEntitiesFile);
		int[] didTmp = new int[corpusEntities.size()];
		int[] eidTmp = new int[corpusEntities.size()];
		int[] reidTmp = new int[corpusEntities.size()];
		double[] esrTmp = new double[corpusEntities.size()];
		docEntMat = new int[D][];
		entEntMat = new int[P][];
		entEntSrMat = new double[P][];
		for (int i = 0; i < corpusEntities.size(); i++) {
			String[] tokens = corpusEntities.get(i).split(" ");
			didTmp[i] = Integer.parseInt(tokens[0]);
			eidTmp[i] = Integer.parseInt(tokens[1]);
			reidTmp[i] = Integer.parseInt(tokens[2]);
			esrTmp[i] = Double.parseDouble(tokens[3]);
		} // end of for
		for (int id = 0; id < D; id++) {
			List<Integer> dIndexList = findIndexList(didTmp, id);
			Set<Integer> unique = new HashSet<Integer>();
			for (int i = 0; i < dIndexList.size(); i++) {
				if (!unique.contains(eidTmp[dIndexList.get(i)])) {
					unique.add(eidTmp[dIndexList.get(i)]);
				}
			} // end of for i
			docEntMat[id] = new int[unique.size()];
			List<Integer> ue = new ArrayList<Integer>(unique);
			for (int j = 0; j < ue.size(); j++) {
				docEntMat[id][j] = ue.get(j);
			}
		} // end of for id

		for (int id = 0; id < P; id++) {
			List<Integer> eIndexList = findIndexList(eidTmp, id);
			Set<Integer> unique = new HashSet<Integer>();
			List<Integer> tmpEntArr = new ArrayList<Integer>();
			List<Double> tmpEntSrArr = new ArrayList<Double>();
			for (int i = 0; i < eIndexList.size(); i++) {
				if (!unique.contains(reidTmp[eIndexList.get(i)])) {
					tmpEntArr.add(reidTmp[eIndexList.get(i)]);
					tmpEntSrArr.add(esrTmp[eIndexList.get(i)]);
					unique.add(reidTmp[eIndexList.get(i)]);
				}
			} // end of for i
			entEntMat[id] = new int[unique.size()];
			entEntSrMat[id] = new double[unique.size()];
			for (int j = 0; j < tmpEntArr.size(); j++) {
				entEntMat[id][j] = tmpEntArr.get(j);
				entEntSrMat[id][j] = tmpEntSrArr.get(j);
			}
		} // end of for id

	} // end of processCorpusEntityFile

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
		Ntp = allocateMemory(Ntp, P, T);
		Nwt = allocateMemory(Nwt, T, W);
		Nd = allocateMemory(Nd, D);
		Np = allocateMemory(Np, P);
		Nt = allocateMemory(Nt, T);
		z = allocateMemory(z, N);
		p = allocateMemory(p, N);
		alphaMat = allocateMemory(alphaMat, P, T, ALPHA);
		sumAlpha = allocateMemory(sumAlpha, P, T * ALPHA);
		zeta = allocateMemory(zeta, D, P);
		theta = allocateMemory(theta, P, T);
		phi = allocateMemory(phi, T, W);
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
