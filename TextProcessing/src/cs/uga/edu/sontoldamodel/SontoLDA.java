/**
 * 
 */
package cs.uga.edu.sontoldamodel;

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

/**
 * @author mehdi
 *
 */
public class SontoLDA {

	int[] w = null;
	int[] d = null;
	int[] z = null;
	int[] docIds = null;
	int[] wordIds = null;
	int[] wordsCounts = null;
	int D = 0;
	int W = 0;
	int T = 0;
	int N = 0;
	int nIterations = 0;
	int burnIn = 0;
	double ALPHA = 0;
	double BETA = 0;
	ModelParameters modelParameters = null;
	int[][] Ntd = null;
	int[][] Nwt = null;
	int[] Nd = null;
	int[] Nt = null;
	int[][] catWordMat = null;
	double[][] tfIdfMat = null;
	double[] tfIdfSumMat;
	double[] sumAlpha = null;
	double[][] alphaMat = null;
	double[][] theta = null; // document-topic distribution
	double[][] phi = null; // topic-word distribution
	Random randomGenerator = null;
	String corpusFolder    = null;
	String categoryWordFile = null;
	int showLine = 400000;
	final Logger logger = Logger.getLogger(SontoLDA.class.getName());
	String saveToDir = Configuration.getProperty("savedPosteriorFiles");

	public SontoLDA() {
		modelParameters = new ModelParameters();
		this.w = modelParameters.w;
		this.d = modelParameters.d;
		this.D = modelParameters.D;
		this.W = modelParameters.W;
		this.T = modelParameters.T;
		this.N = modelParameters.N;
		this.nIterations = modelParameters.nIterations;
		this.burnIn = modelParameters.burnIn;
		this.ALPHA = modelParameters.ALPHA;
		this.BETA = modelParameters.BETA;
		this.corpusFolder = modelParameters.corpusFolder;
		categoryWordFile = corpusFolder + "categoryWordMat.txt";
		randomGenerator = new Random();
	}

	public void run() {
		runGibbsSampling();
		computePosteriorDistribution();
		savePosteriorDistribution();
		saveCountMatrices();
	} // end of run

	public void saveCountMatrices() {
		saveMatrix(Ntd, saveToDir + "document-topic.ser");
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
//				if ((itr + 1 == burnIn)) {
//					computeAlpha();
//				}
				for (int w_i = 0; w_i < N; w_i++) {
					if ((w_i + 1) % showLine == 0) {
						System.out.println((w_i + 1) + " done!.");
					}
					sampleTopicAssignment(d [w_i], z [w_i], w [w_i], w_i);
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
	
	public void sampleTopicAssignment(int did, int tid, int wid, int w_i) {
//		double[] pr = null;
//		int prSize = T;
		//int [] eids = new int [diEnts.length];
//		pr = allocateMemory(pr, prSize);
		double[] pr = new double[T];
		updateCounts(did, tid, wid, -1);
		for (int t_i = 0; t_i < T; t_i++) {
			double pr_z = (Ntd[did][t_i] + alphaMat[did][t_i]) / (Nd[did] + sumAlpha[did]);
			double pr_w = (Nwt[t_i][wid] + tfIdfMat[t_i][wid] * BETA) / (Nt[t_i] + tfIdfSumMat[t_i] * BETA);
			pr [t_i] = pr_z * pr_w;
		} // end of for t_i
		int newTopic = sample(pr, randomGenerator.nextDouble());
		z[w_i] = newTopic;
		updateCounts(did, newTopic, wid, +1);
	} // end of sampleTopicAssignment
	
	
//	public void sampleEntityAndTopicAssignment(int did, int tid, int wid, int w_i) {
//		int[] ciWords = catWordMat[tid];
//		double[] tfIdfArr = tfIdfMat[tid];
//		double[] pr = null;
//		int prSize = ciWords.length * T;
//		//int [] eids = new int [diEnts.length];
//		pr = allocateMemory(pr, prSize);
//		updateCounts(did, tid, wid, -1);
//		for (int ctr = 0; ctr < ciWords.length; ctr++) {
//			int wordId = ciWords[ctr];
//			for (int t_i = 0; t_i < T; t_i++) {
//				double pr_z = (Ntd[did][t_i] + alphaMat[did][t_i]) / (Nd[did] + sumAlpha[did]);
//				double pr_w = (Nwt[t_i][wid] + tfIdfArr[wordId] * BETA) / (Nt[t_i] + tfIdfSumMat[t_i] * BETA);
//				pr [ctr * T + t_i] = pr_z * pr_w;
//			} // end of for t_i
//		} // end of for ctr
//		int pairIndex = sample(pr, randomGenerator.nextDouble());
//		int newEntity = ciWords[pairIndex / T];
//		int newTopic = pairIndex % T;
//		z[w_i] = newTopic;
//		updateCounts(did, newTopic, wid, +1);
//	} // end of sampleEntityAndTopicAssignment
	
	
	public boolean hasValue(int[] arr, int val) {
		for (int v : arr) {
			if (v ==  val)
				return true;
		} // end of for
		return false;
	} // end of hasValue

//	public void sampleEntityAndTopicAssignmentBurnIn(int d_i, int w_i) {
//		int[] diEnts = catWordMat[d_i];
//		double[] pr = null;
//		pr = allocateMemory(pr, E * T);
//		for (int ctr = 0; ctr < diEnts.length; ctr++) {
//			int e_i = diEnts[ctr];
//			for (int t_i = 0; t_i < T; t_i++) {
//				updateCounts(d_i, e_i, t_i, w_i, -1);
//				double pr_e = (Ned[d_i][e_i] + TAU) / (Nd[d_i] + E * TAU);
//				double pr_z = (Ntd[e_i][t_i] + alphaMat[e_i][t_i])
//						/ (Ne[e_i] + sumAlpha[e_i]);
//				double pr_w = (Nwt[t_i][w_i] + BETA) / (Nt[t_i] + W * BETA);
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
//		for (int e_i = 0; e_i < E; e_i++) {
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
//		double pr2_z = ((1 - gamma) * pr1_z) + (gamma * (sumProb / sumSr));
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

	public void updateCounts(int dId, int tId, int wId, int val) {
		Ntd[dId][tId] = Ntd[dId][tId] + val;
		Nwt[tId][wId] = Nwt[tId][wId] + val;
		Nd[dId] = Nd[dId] + val;
		Nt[tId] = Nt[tId] + val;
	} // end of updateCounts

//	public void computeAlpha() {
//		for (int e_i = 0; e_i < E; e_i++) {
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


	public void computeTheta() {
		for (int d_i = 0; d_i < D; d_i++) {
			for (int t_i = 0; t_i < T; t_i++) {
				theta[d_i][t_i] = Math.round(((Ntd[d_i][t_i] + alphaMat[d_i][t_i]) / (Nd[d_i] + sumAlpha[d_i])) * 10000) / 10000.;
			} // end of for t_i
		} // end of for d_i
	} // end of computeTheta

	public void computePhi() {
		for (int t_i = 0; t_i < T; t_i++) {
			for (int w_i = 0; w_i < W; w_i++) {
				phi[t_i][w_i] = Math.round(((Nwt[t_i][w_i] + tfIdfMat[t_i][w_i] * BETA) / (Nt[t_i] + tfIdfSumMat[t_i] * BETA)) * 10000) / 10000.;
			} // end of for w_i
		} // end of for t_i
	} // end of computePhi

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
		List<String> documentNames = modelParameters.readFile(corpusFolder + "documentLookup.txt");
		List<String> categoryNames = modelParameters.readFile(corpusFolder + "categoryLookup.txt");
		List<String> wordNames = modelParameters.readFile(corpusFolder + "vocabularyLookup.txt");
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, T);
		for (int t_i = 0; t_i < T; t_i++) {
			for (int w_i = 0; w_i < W; w_i++) {
				sumProb[t_i] += Math.round(phi [t_i][w_i] * 10000) / 10000.;
			} // end of for t_i
		} // end of for e_i
		
		// normalize the probabilities
		for (int t_i = 0; t_i < T; t_i++) {
			for (int w_i = 0; w_i < W; w_i++) {
				phi [t_i][w_i] = (Math.round(phi [t_i][w_i] * 10000) / 10000.) / sumProb[t_i];
			} // end of for w_i
		} // end of for t_i
		
		// sort the probabilities in descending order and write them into the file
		int maxNumOfWords = 50;
		try {
			FileWriter csvFile = new FileWriter("/home/mehdi/topicdiscovery/csv/phi.csv");
			for (int t_i = 0; t_i < T; t_i++) {
				int [] sortedIndex = getSortedIndexArray(phi[t_i]);
				String line = categoryNames.get(t_i).split("\t")[1] + " ";
				for (int w_i = 0; w_i < maxNumOfWords; w_i++) {
					int wordId = sortedIndex[w_i];
					line += wordNames.get(wordId).split("\t")[1] + ":" + phi [t_i][wordId] + " ";
				} // end of for w_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for t_i
			csvFile.close();
			
			// normalize the probabilities
			sumProb = null;
			sumProb = allocateMemory(sumProb, D);
			csvFile = new FileWriter("/home/mehdi/topicdiscovery/csv/theta.csv");
			for (int d_i = 0; d_i < D; d_i++) {
				for (int t_i = 0; t_i < T; t_i++) {
					sumProb[d_i] +=  Math.round(theta [d_i][t_i] * 10000) / 10000.;
				} // end of for t_i
			} // end of for d_i
			for (int d_i = 0; d_i < D; d_i++) {
				for (int t_i = 0; t_i < T; t_i++) {
					theta [d_i][t_i] = (Math.round(theta [d_i][t_i] * 10000) / 10000.) / sumProb[d_i];
				} // end of for t_i
			} // end of for d_i
			
			int maxNumOfTopics = 30;
			for (int d_i = 0; d_i < D; d_i++) {
				int [] sortedIndex = getSortedIndexArray(theta[d_i]);
				String line = documentNames.get(d_i).split("\t")[1] + " ";
				for (int t_i = 0; t_i < maxNumOfTopics; t_i++) {
					int topicId = sortedIndex[t_i];
					line += categoryNames.get(topicId).split("\t")[1] + ":" + theta [d_i][topicId] + " ";
				} // end of for t_i
				csvFile.write(line + "\n");
				csvFile.flush();
			} // end of for d_i
			csvFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of writeToCSV

	public int[] getSortedIndexArray(double[] arr) {
		// be careful about reference by value
		// TODO
		int [] result = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			result[i] = i;
		}
		for (int i = 1; i < arr.length; i++) {
			double x = arr[i];
			int index = result[i];
			int j = i;
			while (j > 0 && arr[j - 1] > x) {
				arr[j]    = arr[j - 1];
				result[j] = result[j - 1];
				j--;
			} // end of while
			arr[j] = x;
			result[j] = index;
		} // end of for
		return result;
	} // end of getSortedIndexArray

	public void savePosteriorDistribution() {
		saveMatrix(theta, saveToDir + "thetaProb.ser");
		saveMatrix(phi, saveToDir + "phiProb.ser");
	} // end of savePosteriorDistribution

	public void loadPosteriorDistribution() {
		theta = loadDoubleMatrix("thetaProb.ser");
		phi   = loadDoubleMatrix("phiProb.ser");
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
		computeTheta();
		computePhi();
	} // end of computePosteriorDistribution

	public void initializeGibbsSampling() {
		processCategoryWordMatFile();
		System.out.print("Gibbs sampling initialization...");
		for (int i = 0; i < N; i++) {
			z[i] = randomGenerator.nextInt(T); // generate random number in [0,T)
		} // end of for

		for (int i = 0; i < N; i++) {
			Ntd[d[i]][z[i]] = Ntd[d[i]][z[i]] + 1;
			Nwt[z[i]][w[i]] = Nwt[z[i]][w[i]] + 1;
			Nd[d[i]]++;
			Nt[z[i]]++;
		} // end of for
		System.out.println("done!");
	} // end of gibbSampleInitialization
	
	public void processCategoryWordMatFile() {
		List<String> catWordLines = modelParameters.readFile(categoryWordFile);
//		catWordMat  = new int[T][W];
		tfIdfMat    = new double[T][W];
		tfIdfSumMat = new double[T];
		for (int i = 0; i < catWordLines.size(); i++) {
			String[] tokens = catWordLines.get(i).split(" ");
			int cid = Integer.parseInt(tokens[0]);
			int wid = Integer.parseInt(tokens[1]);
			double tfIdfVal = Double.parseDouble(tokens[2]);
			tfIdfMat[cid][wid] = tfIdfVal;
		} // end of for
		for (int i = 0; i < T; i++) {
			for (int j = 0; j < W; j++) {
				tfIdfSumMat[i] += tfIdfMat[i][j];
			} // end of for j
		} // end of for i
	} // end of processCategoryWordMatFile

//	public void processCategoryWordMatFile() {
//		List<String> catWordLines = modelParameters.readFile(categoryWordFile);
//		int[] cidTmp = new int[catWordLines.size()];
//		int[] widTmp = new int[catWordLines.size()];
//		double[] tfIdfTmp = new double[catWordLines.size()];
//		catWordMat  = new int[T][];
//		tfIdfMat    = new double[T][];
//		tfIdfSumMat = new double[T];
//		for (int i = 0; i < catWordLines.size(); i++) {
//			String[] tokens = catWordLines.get(i).split(" ");
//			cidTmp[i] = Integer.parseInt(tokens[0]);
//			widTmp[i] = Integer.parseInt(tokens[1]);
//			tfIdfTmp[i] = Double.parseDouble(tokens[3]);
//		} // end of for
//		for (int id = 0; id < T; id++) {
//			List<Integer> cIndexList = findIndexList(cidTmp, id);
//			Set<Integer> unique = new HashSet<Integer>();
//			List<Integer> tmpcatWordArr = new ArrayList<Integer>();
//			List<Double> tmpTfidfArr = new ArrayList<Double>();
//			for (int i = 0; i < cIndexList.size(); i++) {
//				if (!unique.contains(widTmp[cIndexList.get(i)])) {
//					tmpcatWordArr.add(widTmp[cIndexList.get(i)]);
//					tmpTfidfArr.add(tfIdfTmp[cIndexList.get(i)]);
//					unique.add(widTmp[cIndexList.get(i)]);
//				}
//			} // end of for i
//			catWordMat[id] = new int[unique.size()];
//			tfIdfMat[id]   = new double[unique.size()];
//			for (int j = 0; j < tmpTfidfArr.size(); j++) {
//				catWordMat[id][j] = tmpcatWordArr.get(j);
//				tfIdfMat[id][j] = tmpTfidfArr.get(j);
//				tfIdfSumMat[id] += tmpTfidfArr.get(j);
//			}
//		} // end of for id
//	} // end of processCategoryWordMatFile

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
		Ntd = allocateMemory(Ntd, D, T);
		Nwt = allocateMemory(Nwt, T, W);
		Nd = allocateMemory(Nd, D);
		Nt = allocateMemory(Nt, T);
		z = allocateMemory(z, N);
		alphaMat = allocateMemory(alphaMat, D, T, ALPHA);
		sumAlpha = allocateMemory(sumAlpha, D, T * ALPHA);
		theta = allocateMemory(theta, D, T);
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
