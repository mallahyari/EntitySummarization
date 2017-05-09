/**
 * 
 */
package cs.uga.edu.lodlda;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.GRAPH;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;


/**
 * @author mehdi
 *
 */
public class LodLda {
	
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String catUriPrefix = "http://dbpedia.org/resource/Category:";
	private int [] u = null;
	private int [] m = null;
	private int [] z = null;
	private int [] c1 = null;
	private int [] c2 = null;
	private int [] c3 = null;
	private int [] c4 = null;
	private int [] c5 = null;
	private int [] userIds = null;
	private int [] movieIds = null;
	int[][] Ntu = null;
	int[][] Nmt = null;
	int[][] Nct = null;
	int[] Nu = null;
	int[] Nt_m = null;
	int[] Nt_c = null;
	private int nUsers = 0;
	private int nMovies = 3131;
	private int nTopics = 20;
	private int nContexts = 5;
	private int nFeatures = 10;
	private int nRecords = 0;
	private int nIterations = 1000;
//	private int burnIn = 200;
	private double ALPHA = 50.0 / nTopics;
	private final double BETA  = 0.01;
	private final double GAMMA  = 0.01;
	private final int fileNumOfLines = 1000000;
	double[][] theta = null; // user-topic distribution
	double[][] phi = null; // topic-movie distribution
	double[][] zeta = null; // topic-feature distribution
	double[][] userMovieRank = null; // user-movie rank matrix
	
	private Map<Integer,List<Integer>> movieContextMap = null;
	private Set<Integer> testUsers = null;
	private Set<Integer> testMovies = null;
	
	Random randomGenerator = null;
	int showLine = 500000;
	final Logger logger = Logger.getLogger(LodLda.class.getName());
	String saveToDir = "/home/mehdi/lodproject/posteriorFiles/";
	
	
	public LodLda() {
		randomGenerator = new Random();
		testUsers = new HashSet<Integer>();
		testMovies = new HashSet<Integer>();
	}
	
	public void computeRecommendationScore(String testFilename) {
//		loadPosteriorDistribution();
		userMovieRank = allocateMemory(userMovieRank, nUsers, nMovies);
		
		// load movie to features map
		movieContextMap = readMovieContextFile(nContexts);
		
		// find unique users and unique movies in test set
		findTestUsersAndMovies(testFilename);
		for (int uId : testUsers) {
			for (int mId : testMovies) {
				// P(m|c,u) = P(m|u) * P(c|m,u)
				double pr_m = findProbabilityOfMovieGivenUser(mId,uId);
				double pr_c = findProbabilityOfContextGivenUserAndMovie(mId,uId,movieContextMap.get(mId));
				userMovieRank[uId][mId] = pr_m * pr_c;
			} // end of for
		} // end of for
		
		// normalize the user-movie matrix
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, nUsers);
		for (int u_i = 0; u_i < nUsers; u_i++) {
			for (int m_i = 0; m_i < nMovies; m_i++) {
				sumProb[u_i] += userMovieRank[u_i][m_i];
			}
		} // end of for
		
		for (int u_i = 0; u_i < nUsers; u_i++) {
			for (int m_i = 0; m_i < nMovies; m_i++) {
				if (sumProb[u_i] == 0) {
					sumProb[u_i] = 1;
				}
				userMovieRank[u_i][m_i] = Math.round((userMovieRank[u_i][m_i] / sumProb[u_i]) * 10000) / 10000.;
			}
		} // end of for
	} // end of computeRecommendationScore
	
	public void findTopNRecommendation(int n, String testFilename) {
		// compute the recommendation score for each user and movie according the test set
//		computeRecommendationScore(testFilename);
		
		// create a map for each user and their movies from test set
		Map<Integer,Set<Integer>> userMovieMap = createUserMovieMapForTestSet(testFilename);
		
		// for each user and movie in the test set, compute the top-N recommendation
		double hit = 0;
		for (int uId : testUsers) {
				double [] userVec = userMovieRank[uId];
				// sort the recommendation in descending order using insertion sort
				int [] topMoviesVec = getSortedIndexArray(userVec);
				Set<Integer> umovies = userMovieMap.get(uId);
				for (int i = 0; i < n; i++) {
					int m = topMoviesVec[i];
					if (umovies.contains(m)) {
						hit++;
//						System.out.println("user: " + uId + ", Movies: " + umovies.toString() + ", m: " + m + " prob: " + userVec[m]);
						break;
					}
				} // end of for
		} // end of for
//		System.out.println("No of Hits: " + hit);
		System.out.println("SCRM: " + hit / testUsers.size());
	} // end of findTopNRecommendation
	
	public void findTopNRecommendationForBaselineKNN(String filename, int n, String method) {
//		findTestUsersAndMovies();
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			// skip the first line which is header
			String line = br.readLine();
			double hits = 0;
			int ucounter = 0;
			while ((line = br.readLine()) != null) {
				ucounter++;
				line = line.replace(" ", "").replace("),","-").replace("(", "").replace("(", "");
				String [] tokens = line.split("-");
				String uId = tokens[0].split(":")[0];
				for (int i = 0; i < n; i++) {
					String movie = tokens[i];
					if (movie.indexOf("*") != -1) {
						hits++;
//						System.out.println("user: " + uId + ", Movies: " + movie);
						break;
					}
				}
			} // end of while
			br.close();
//			System.out.println("No of Hits for KNN: " + hits);
//			System.out.println(hits / testUsers.size());
			System.out.print(method + ": " + hits / ucounter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		// create a map for each user and their movies from test set
//		Map<Integer,Set<Integer>> userMovieMap = createUserMovieMapForTestSet();
//				Set<Integer> uKnnUsers = getUsersFromFile(filename);
//				System.out.println("KNN size:" + uKnnUsers.size());
//				Set<Integer> usrs = userMovieMap.keySet();
//				for (int uid : usrs) {
//					if (!uKnnUsers.contains(uid)) {
//				System.out.println("uid: " + uid);
//			}
//		}
	} // end of findTopNRecommendationForBaselineKNN
	
	public Set<Integer> getUsersFromFile(String filename) {
		Set<Integer> users = new HashSet<Integer>();
		try {
			int counter = 0;
			BufferedReader br = new BufferedReader(new FileReader(filename));
			// skip the first line which is header
//			String line = br.readLine();
			String line = "";
			counter++;
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split(" ");
//				String [] tokens = line.split(":");
				users.add(Integer.parseInt(tokens[0]));
				counter++;
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return users;
	} // getUsersFromFile

	public int[] getSortedIndexArray(double[] arr) {
		int [] result = new int[arr.length];
		double [] arrCopy = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			result[i] = i;
			arrCopy[i] = arr[i];
		}
		for (int i = 1; i < arr.length; i++) {
			double x = arrCopy[i];
			int index = result[i];
			int j = i;
			while (j > 0 && arrCopy[j - 1] < x) {
				arrCopy[j]    = arrCopy[j - 1];
				result[j] = result[j - 1];
				j--;
			} // end of while
			arrCopy[j] = x;
			result[j] = index;
		} // end of for
		return result;
	} // end of getSortedIndexArray

	public Map<Integer, Set<Integer>> createUserMovieMapForTestSet(String testFilename) {
//		String inputfilename = "/home/mehdi/lodproject/testset1.txt";
//		String inputfilename = "/home/mehdi/lodproject/userprofile.txt";
		Map<Integer, Set<Integer>> mu = new HashMap<Integer, Set<Integer>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(testFilename));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split(" ");
				int userId = Integer.parseInt(tokens[0]);
				int movieId = Integer.parseInt(tokens[1]);
				if (mu.get(userId) == null) {
					Set<Integer> m = new HashSet<Integer>();
					m.add(movieId);
					mu.put(userId, m);
				}else {
					Set<Integer> m = mu.get(userId);
					m.add(movieId);
					mu.put(userId, m);
				} // end of if
			} // end of while
			br.close();
//			System.out.println("done.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mu;
	} // end of createUserMovieMapForTestSet

	public double findProbabilityOfContextGivenUserAndMovie(int mId, int uId, List<Integer> ctext) {
		double normalizationConstant = 0;
		for (int t_i = 0; t_i < nTopics; t_i++) {
			normalizationConstant += phi[t_i][mId] * theta[uId][t_i];
		} // end of for
		
		if (normalizationConstant == 0) {
			normalizationConstant = 1;
		}
		double prob = 1;
		for (int f : ctext) {
			double pr_f = 0;
			for (int t_i = 0; t_i < nTopics; t_i++) {
				pr_f += (zeta[t_i][f] * phi[t_i][mId] * theta[uId][t_i]) / normalizationConstant;
			}
			prob *= pr_f;
		} // end of for
		return prob;
	} // end of findProbabilityOfContextGivenUserAndMovie

	public double findProbabilityOfMovieGivenUser(int mId, int uId) {
		double prob = 0;
		for (int t_i = 0; t_i < nTopics; t_i++) {
			// P(m|u) = Sum(P(m|z) * P(z|u))
			prob += phi[t_i][mId] * theta[uId][t_i];
		}
		return prob;
	} // end of findProbabilityOfMovieGivenUser

	public void findTestUsersAndMovies(String testFilename) {
//		String movieLookupFile = "/home/mehdi/lodproject/" + testFilename;
		try {
			BufferedReader br = new BufferedReader(new FileReader(testFilename));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split(" ");
				testUsers.add(Integer.parseInt(tokens[0]));
				testMovies.add(Integer.parseInt(tokens[1]));
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of findTestUsersAndMovies

	public void run(String mode) {
		if (mode.equals("testing")) {
			nIterations = 50;
		}
		runGibbsSampling();
		computePosteriorDistribution();
		savePosteriorDistribution(mode);
		saveCountMatrices(mode);
	} // end of run
	
	public void loadPosteriorDistribution() {
		theta = loadDoubleMatrix("thetaProb_test.ser");
		phi   = loadDoubleMatrix("phiProb_test.ser");
		zeta  = loadDoubleMatrix("zetaProb_test.ser");
		nUsers = theta.length;
		nTopics = phi.length;
		nMovies = phi[0].length;
	} // end of loadPosteriorDistribution
	
	public void loadCountMatrices() {
		Ntu = loadIntMatrix("user-topic_train.ser");
		Nmt = loadIntMatrix("topic-movie_train.ser");
		Nct = loadIntMatrix("topic-feature_train.ser");
		nUsers = Ntu.length;
		nTopics = Nmt.length;
		nMovies = Nmt[0].length;
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
	
	public void saveCountMatrices(String mode) {
		if (mode.equals("training")) {
			// for training
			saveMatrix(Ntu, saveToDir + "user-topic_train.ser");
			saveMatrix(Nmt, saveToDir + "topic-movie_train.ser");
			saveMatrix(Nct, saveToDir + "topic-feature_train.ser");
		}else {
			//for testing
			saveMatrix(Ntu, saveToDir + "user-topic_test.ser");
			saveMatrix(Nmt, saveToDir + "topic-movie_test.ser");
			saveMatrix(Nct, saveToDir + "topic-feature_test.ser");
		}
	} // end of saveCountMatrices
	
	private void savePosteriorDistribution(String mode) {
		if (mode.equals("training")) {
			// for training
			saveMatrix(theta, saveToDir + "thetaProb_train.ser");
			saveMatrix(phi, saveToDir + "phiProb_train.ser");
			saveMatrix(zeta, saveToDir + "zetaProb_train.ser");
		}else {
			// for testing
		saveMatrix(theta, saveToDir + "thetaProb_test.ser");
		saveMatrix(phi, saveToDir + "phiProb_test.ser");
		saveMatrix(zeta, saveToDir + "zetaProb_test.ser");
		}
	} // end of savePosteriorDistribution
	
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

	public void computePosteriorDistribution() {
		computeTheta();
		computePhi();
		computeZeta();
	} // end of computePosteriorDistribution
	
	public void computeTheta() {
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, nUsers);
		for (int u_i = 0; u_i < nUsers; u_i++) {
			for (int t_i = 0; t_i < nTopics; t_i++) {
				theta[u_i][t_i] = Math.round(((Ntu[u_i][t_i] + ALPHA) / (Nu[u_i] + (nTopics * ALPHA))) * 10000) / 10000.;
				sumProb[u_i] += Math.round(theta[u_i][t_i] * 10000) / 10000.;
			} // end of for t_i
		} // end of for u_i
		
		// normalize the probabilities
		for (int u_i = 0; u_i < nUsers; u_i++) {
			for (int t_i = 0; t_i < nTopics; t_i++) {
				theta[u_i][t_i] = Math.round((theta[u_i][t_i] / sumProb[u_i]) * 10000) / 10000.;
			} // end of for t_i
		} // end of for u_i
	} // end of computeTheta

	public void computePhi() {
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, nTopics);
		for (int t_i = 0; t_i < nTopics; t_i++) {
			for (int m_i = 0; m_i < nMovies; m_i++) {
				phi[t_i][m_i] = Math.round(((Nmt[t_i][m_i] + BETA) / (Nt_m[t_i] + (nMovies * BETA))) * 10000) / 10000.;
				sumProb[t_i] += Math.round(phi[t_i][m_i] * 10000) / 10000.;
			} // end of for m_i
		} // end of for t_i
		
		// normalize the probabilities
		for (int t_i = 0; t_i < nTopics; t_i++) {
			for (int m_i = 0; m_i < nMovies; m_i++) {
				phi[t_i][m_i] = Math.round((phi[t_i][m_i] / sumProb[t_i]) * 10000) / 10000.;
			} // end of for m_i
		} // end of for t_i
		
	} // end of computePhi
	
	public void computeZeta() {
		double [] sumProb = null;
		sumProb = allocateMemory(sumProb, nTopics);
		for (int t_i = 0; t_i < nTopics; t_i++) {
			for (int f_i = 0; f_i < nFeatures; f_i++) {
				zeta[t_i][f_i] = Math.round(((Nct[t_i][f_i] + GAMMA) / (Nt_c[t_i] + (nFeatures * GAMMA))) * 10000) / 10000.;
				sumProb[t_i] += Math.round(zeta[t_i][f_i] * 10000) / 10000.;
			} // end of for f_i
		} // end of for t_i
		
		// normalize the probabilities
		for (int t_i = 0; t_i < nTopics; t_i++) {
			for (int f_i = 0; f_i < nFeatures; f_i++) {
				zeta[t_i][f_i] = Math.round((zeta[t_i][f_i] / sumProb[t_i]) * 10000) / 10000. ;
			} // end of for f_i
		} // end of for t_i
	} // end of computeZeta
	
	public void runGibbsSampling() {
		movieContextMap = readMovieContextFile(nContexts);
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
				for (int ctr = 0; ctr < nRecords; ctr++) {
					if ((ctr + 1) % showLine == 0) {
						System.out.println((ctr + 1) + " done.");
					}
					// call this function for context with 1 feature 
//					sampleTopicAssignment(u[ctr], z[ctr], m[ctr], c1[ctr], ctr);
//					sampleTopicAssignment(u[ctr], z[ctr], m[ctr], c1[ctr], c2[ctr], ctr);
					//sampleTopicAssignment(u[ctr], z[ctr], m[ctr], c1[ctr], c2[ctr], c3[ctr], ctr);
					sampleTopicAssignment(u[ctr], z[ctr], m[ctr], c1[ctr], c2[ctr], c3[ctr], c4[ctr], c5[ctr], ctr);
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
	
	// This function is ONLY for context with 1 feature
		public void sampleTopicAssignment(int uid, int tid, int mid, int c1id, int index) {
			double[] pr = new double[nTopics];
			List<Integer> currMcontext = new ArrayList<Integer>();
			currMcontext.add(c1id);
			List<Integer> mcontexts = movieContextMap.get(mid);
			updateCounts(uid, tid, mid, currMcontext, -1);
			double sumPr = 0;
			for (int t_i = 0; t_i < nTopics; t_i++) {
				double pr_z = (Ntu[uid][t_i] + ALPHA) / (Nu[uid] + (nTopics * ALPHA));
				double pr_m = (Nmt[t_i][mid] + BETA) / (Nt_m[t_i] + (nMovies * BETA));
				double pr_c = 1;
				for (int con : mcontexts) {
					pr_c *= (Nct[t_i][con] + GAMMA) / (Nt_c[t_i] + (nFeatures * GAMMA));
				}
				pr[t_i] = pr_z * pr_m * pr_c;
				sumPr += pr [t_i];
			} // end of for t_i
			int newTopic = sample(pr, randomGenerator.nextDouble(), sumPr);
			z[index] = newTopic;
			c1[index]  = mcontexts.get(0);
			updateCounts(uid, newTopic, mid, mcontexts, +1);
		} // end of sampleTopicAssignment
		
	
	// This function is ONLY for context with 2 features
	public void sampleTopicAssignment(int uid, int tid, int mid, int c1id, int c2id, int index) {
		double[] pr = new double[nTopics];
		List<Integer> mcontexts = movieContextMap.get(mid);
		updateCounts(uid, tid, mid, c1id, c2id, -1);
		double sumPr = 0;
		for (int t_i = 0; t_i < nTopics; t_i++) {
			double pr_z = (Ntu[uid][t_i] + ALPHA) / (Nu[uid] + (nTopics * ALPHA));
			double pr_m = (Nmt[t_i][mid] + BETA) / (Nt_m[t_i] + (nMovies * BETA));
			double pr_c = 1;
			for (int con : mcontexts) {
				pr_c *= (Nct[t_i][con] + GAMMA) / (Nt_c[t_i] + (nFeatures * GAMMA));
			}
			pr[t_i] = pr_z * pr_m * pr_c;
			sumPr += pr [t_i];
		} // end of for t_i
		int newTopic = sample(pr, randomGenerator.nextDouble(), sumPr);
		z[index] = newTopic;
		c1[index]  = mcontexts.get(0);
		c2[index]  = mcontexts.get(1);
		updateCounts(uid, newTopic, mid, mcontexts, +1);
	} // end of sampleTopicAssignment
	
	// This function is ONLY for context with 3 features
	public void sampleTopicAssignment(int uid, int tid, int mid, int c1id, int c2id,int c3id, int index) {
		double[] pr = new double[nTopics];
		List<Integer> currMcontext = new ArrayList<Integer>();
		currMcontext.add(c1id);
		currMcontext.add(c2id);
		currMcontext.add(c3id);
		List<Integer> mcontexts = movieContextMap.get(mid);
		updateCounts(uid, tid, mid, currMcontext, -1);
		double sumPr = 0;
		for (int t_i = 0; t_i < nTopics; t_i++) {
			double pr_z = (Ntu[uid][t_i] + ALPHA) / (Nu[uid] + (nTopics * ALPHA));
			double pr_m = (Nmt[t_i][mid] + BETA) / (Nt_m[t_i] + (nMovies * BETA));
			double pr_c = 1;
			for (int con : mcontexts) {
				pr_c *= (Nct[t_i][con] + GAMMA) / (Nt_c[t_i] + (nFeatures * GAMMA));
			}
			pr[t_i] = pr_z * pr_m * pr_c;
			sumPr += pr [t_i];
		} // end of for t_i
		int newTopic = sample(pr, randomGenerator.nextDouble(), sumPr);
		z[index] = newTopic;
		c1[index]  = mcontexts.get(0);
		c2[index]  = mcontexts.get(1);
		c3[index]  = mcontexts.get(2);
		updateCounts(uid, newTopic, mid, mcontexts, +1);
	} // end of sampleTopicAssignment

	// This function is ONLY for context with 5 features
	public void sampleTopicAssignment(int uid, int tid, int mid, int c1id, int c2id, int c3id, int c4id, int c5id, int index) {
		double[] pr = new double[nTopics];
		List<Integer> currMcontext = new ArrayList<Integer>();
		currMcontext.add(c1id);
		currMcontext.add(c2id);
		currMcontext.add(c3id);
		currMcontext.add(c4id);
		currMcontext.add(c5id);
		List<Integer> mcontexts = movieContextMap.get(mid);
		updateCounts(uid, tid, mid, currMcontext, -1);
		double sumPr = 0;
		for (int t_i = 0; t_i < nTopics; t_i++) {
			double pr_z = (Ntu[uid][t_i] + ALPHA) / (Nu[uid] + (nTopics * ALPHA));
			double pr_m = (Nmt[t_i][mid] + BETA) / (Nt_m[t_i] + (nMovies * BETA));
			double pr_c = 1;
			for (int con : mcontexts) {
				pr_c *= (Nct[t_i][con] + GAMMA) / (Nt_c[t_i] + (nFeatures * GAMMA));
			}
			pr[t_i] = pr_z * pr_m * pr_c;
			sumPr += pr [t_i];
		} // end of for t_i
		int newTopic = sample(pr, randomGenerator.nextDouble(), sumPr);
		if (newTopic < 0)
			System.out.println();
		z[index] = newTopic;
		c1[index]  = mcontexts.get(0);
		c2[index]  = mcontexts.get(1);
		c3[index]  = mcontexts.get(2);
		c4[index]  = mcontexts.get(3);
		c5[index]  = mcontexts.get(4);
		updateCounts(uid, newTopic, mid, mcontexts, +1);
	} // end of sampleTopicAssignment

	private void updateCounts(int uId, int tId, int mId, int c1Id, int c2Id, int val) {
		Ntu[uId][tId] = Ntu[uId][tId] + val;
		Nmt[tId][mId] = Nmt[tId][mId] + val;
		Nct[tId][c1Id] = Nct[tId][c1Id] + val;
		Nct[tId][c2Id] = Nct[tId][c2Id] + val;
		Nu[uId] = Nu[uId] + val;
		Nt_m[tId] = Nt_m[tId] + val;
		Nt_c[tId] = Nt_c[tId] + (2 * val);
		if (Nct[tId][c1Id] < 0 || Nct[tId][c2Id] < 0)
			System.out.println("---");
	} // end of updateCounts

	public void updateCounts(int uId, int tId, int mId, List<Integer> ctext, int val) {
		Ntu[uId][tId] = Ntu[uId][tId] + val;
		Nmt[tId][mId] = Nmt[tId][mId] + val;
		for (int cId : ctext) {
			Nct[tId][cId] = Nct[tId][cId] + val;
		}
		Nu[uId] = Nu[uId] + val;
		Nt_m[tId] = Nt_m[tId] + val;
		Nt_c[tId] = Nt_c[tId] + ctext.size();
	} // end of updateCounts
	
	public int sample(double[] pr, double randSeed, double sumPr) {
		int l = pr.length;
		double[] cdf = new double[l];
		for (int i = 0; i < l; i++) {
			cdf[i] = pr[i];
//			cdf[i] = pr[i] / sumPr;
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
	
	public void initializeGibbsSampling() {
		System.out.print("Gibbs sampling initialization...");
		for (int i = 0; i < nRecords; i++) {
			z[i] = randomGenerator.nextInt(nTopics); // generate random number in [0,nTopics)
			c1[i] = randomGenerator.nextInt(nFeatures); // generate random number in [0,nFeatures)
			c2[i] = randomGenerator.nextInt(nFeatures); // generate random number in [0,nFeatures)
			c3[i] = randomGenerator.nextInt(nFeatures); // generate random number in [0,nFeatures)
			c4[i] = randomGenerator.nextInt(nFeatures); // generate random number in [0,nFeatures)
			c5[i] = randomGenerator.nextInt(nFeatures); // generate random number in [0,nFeatures)
		} // end of for

		for (int i = 0; i < nRecords; i++) {
			Ntu[u[i]][z[i]] = Ntu[u[i]][z[i]] + 1;
			Nmt[z[i]][m[i]] = Nmt[z[i]][m[i]] + 1;
			Nct[z[i]][c1[i]] = Nct[z[i]][c1[i]] + 1;
			Nct[z[i]][c2[i]] = Nct[z[i]][c2[i]] + 1;
			Nct[z[i]][c3[i]] = Nct[z[i]][c3[i]] + 1;
			Nct[z[i]][c4[i]] = Nct[z[i]][c4[i]] + 1;
			Nct[z[i]][c5[i]] = Nct[z[i]][c5[i]] + 1;
			Nu[u[i]]++;
			Nt_m[z[i]]++;
			Nt_c[z[i]]+= nContexts;
		} // end of for
		System.out.println("done!");
	} // end of gibbSampleInitialization
	
	public void initializeParameters(String corpusFilename) {
		System.out.print("Reading Training file...");
//		String corpusFilename = "/home/mehdi/lodproject/trainingset1.txt";
		List<String> corpus = readFile(corpusFilename);
		System.out.println("done!");
		userIds	= new int [corpus.size()];
		movieIds = new int [corpus.size()];
		Set<Integer> uniqueMovieIds = new HashSet<Integer>();
		Set<Integer> uniqueUserIds = new HashSet<Integer>();
		for (int i = 0;i < corpus.size();i++) {
			String [] tokens = corpus.get(i).split(" ");
			userIds [i] = Integer.parseInt(tokens [0]);
			movieIds [i] = Integer.parseInt(tokens [1]);
			uniqueUserIds.add(userIds [i]);
			uniqueMovieIds.add(movieIds [i]);
			nRecords++;
		} // end of for
		nUsers = uniqueUserIds.size();
//		nMovies = uniqueMovieIds.size();
		
//		for (int i = 0;i < nUsers;i++) {
//			if (!uniqueUserIds.contains(i))
//				System.out.println(i);
//		}
//		
//		for (int i = 0;i < nMovies;i++) {
//			if (!uniqueMovieIds.contains(i))
//				System.out.println(i);
//		}
		
	} // end of initializeParameters
	
	public void initializeParametersForTest(String testFilename) {
		System.out.print("Reading Training file...");
//		String testFilename = "/home/mehdi/lodproject/testset1.txt";
		List<String> corpus = readFile(testFilename);
		System.out.println("done!");
		userIds	= new int [corpus.size()];
		movieIds = new int [corpus.size()];
		Set<Integer> uniqueMovieIds = new HashSet<Integer>();
		Set<Integer> uniqueUserIds = new HashSet<Integer>();
		for (int i = 0;i < corpus.size();i++) {
			String [] tokens = corpus.get(i).split(" ");
			userIds [i] = Integer.parseInt(tokens [0]);
			movieIds [i] = Integer.parseInt(tokens [1]);
			uniqueUserIds.add(userIds [i]);
			uniqueMovieIds.add(movieIds [i]);
			nRecords++;
		} // end of for
		nUsers = uniqueUserIds.size();
//		nMovies = uniqueMovieIds.size();
		
//		for (int i = 0;i < nUsers;i++) {
//			if (!uniqueUserIds.contains(i))
//				System.out.println(i);
//		}
//		
//		for (int i = 0;i < nMovies;i++) {
//			if (!uniqueMovieIds.contains(i))
//				System.out.println(i);
//		}
		
	} // end of initializeParametersForTest
	
	public void fillArrays() {
		System.out.print("Filling arrays...");
//		long st = System.currentTimeMillis();
		u = new int [nRecords];
		m = new int [nRecords];
		int count = 0;
		for (int j = 0; j < userIds.length; j++) {
			for (int i = count; i < count + 1; i++) {
				u [i] = userIds [j];
				m [i] = movieIds [j];
				//System.out.println(i);
			} // end of for i
			count++;
//			System.out.println(count);
		} // end of for j
//		double dt = (System.currentTimeMillis() - st);// / 1000.;
//		System.out.println("time: " + dt);
		System.out.println("done!");
	} // end of fillArrays
	
	private Map<Integer, List<Integer>> readMovieContextFile(int nFeatures) {
		Map<Integer, List<Integer>> m = new HashMap<Integer, List<Integer>>();
		String movieLookupFile = "/home/mehdi/lodproject/lod_compostion_T10.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(movieLookupFile));
			String line = "";
			line = br.readLine(); // skip the first line which is header
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split("\t");
				String mId = tokens[1];
				List<Integer> features = new ArrayList<Integer>();
				for (int i = 2; i < 2 * nFeatures + 2; i++) {
					if (i % 2 != 0) continue;
					features.add(Integer.parseInt(tokens[i]));
				}
				m.put(Integer.parseInt(mId), features);
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return m;
	} // end of readMovieContextFile
	
	public void createCorpus() {
		// read the movielens2Dbpedia map file
		Map<String,String> movielens2Dbpedia = readMovieLensFile();
		
		System.out.print("Creating corpus files...");
		int userId = 0;
		int movieId = 0;
		Set<String> duplicateUsers = new HashSet<String>();
		Set<String> duplicateMovies = new HashSet<String>();
		Map<String,Integer> userToIdMap = new HashMap<String,Integer>();
		Map<String,Integer> movieToIdMap = new HashMap<String,Integer>();
		String ratingfile = "/home/mehdi/lodproject/movielens-1M/ratings.dat";
		try {
//			FileWriter userLookupfile = new FileWriter("/home/mehdi/lodproject/userLookup.txt");
//			FileWriter movieLookupfile = new FileWriter("/home/mehdi/lodproject/movieLookup.txt");
			FileWriter corpusfile = new FileWriter("/home/mehdi/lodproject/corpus.txt");
			BufferedReader br = new BufferedReader(new FileReader(ratingfile));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split("::");
				String mId = tokens[1];
				if (movielens2Dbpedia.get(mId) != null) {
					String uId = tokens[0];
					String rate = tokens[2];
					if (!duplicateUsers.contains(uId)) {
						duplicateUsers.add(uId);
//						userLookupfile.write(userId + "\t" + uId + "\n");
						userToIdMap.put(uId, userId);
						userId++;
					}
					if (!duplicateMovies.contains(mId)) {
						duplicateMovies.add(mId);
//						movieLookupfile.write(mId + "\t" + movielens2Dbpedia.get(mId) + "\n");
//						movieLookupfile.write(movieId + "\t" + movielens2Dbpedia.get(mId) + "\n");
						movieToIdMap.put(mId, movieId);
						movieId++;
					}
					corpusfile.write(uId + " " + mId + " " + rate + "\n");
//					corpusfile.write(userToIdMap.get(uId) + " " + movieToIdMap.get(mId) + " " + rate + "\n");
				} // end of if
			} // end of while
			br.close();
//			userLookupfile.close();
//			movieLookupfile.close();
			corpusfile.close();
			System.out.println("done!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createCorpus
	
	public Map<String,String> readMovieLensFile() {
		String filename = "/home/mehdi/lodproject/movielens-1M/Movielens2Lookup.csv";
		Map<String,String> m = new HashMap<String,String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split(",");
//				System.out.println(line);
				String mname =  tokens[tokens.length - 1].replace("\"", "");
				mname = line.substring(line.indexOf(uriPrefix) + uriPrefix.length()).replace("\"", "");
				m.put(tokens[0],mname);
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return m;
	} // end of readMovieLensFile
	
	public Map<String,String> readMovieLookupFile() {
		String filename = "/home/mehdi/lodproject/movieLookup.txt";
		Map<String,String> m = new HashMap<String,String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split("\t");
				m.put(tokens[0],tokens[2]);
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return m;
	} // end of readMovieLensFile
	
//	public void createMovieFeatures() {
//		System.out.println("Creating movie features...");
//		Map<String,String> movieLookup = readMovieLookupFile();
//		String movieLookupFile = "/home/mehdi/lodproject/movieLookup.txt";
//		Map<String,Integer> wordToIdMap = new HashMap<String,Integer>();
//		int wordId = 0;
//		try {
//			FileWriter movieFeaturefile = new FileWriter("/home/mehdi/lodproject/movieFeatures.txt");
////			FileWriter vocabfile = new FileWriter("/home/mehdi/lodproject/vocabulary.txt");
//			BufferedReader br = new BufferedReader(new FileReader(movieLookupFile));
//			String line = "";
//			while ((line = br.readLine()) != null) {
//				String [] tokens = line.split("\t");
//				String mId = tokens[0];
//				String mname = tokens[2];
//				List<String> bow = null;
//				// check to make sure name is the title of the entity
//				String redirectname = getRedirectName(mname);
//				if (redirectname.equals("")) {
//					// get related triples and construct bag of words from categories
////					bow = constructBowFromCategories(mname);
//					
//					bow = constructBowFromProperties(mname);
//				}else {
////					bow = constructBowFromCategories(redirectname);
//	
//					bow = constructBowFromProperties(redirectname);
//				}
//				
//				// needed if docs are going to be integer words
////				List<Integer> doc = new ArrayList<Integer>(bow.size()); 
////				for (String word : bow) {
////					if(wordToIdMap.get(word) == null) {
////						wordToIdMap.put(word, wordId);
////						vocabfile.write(wordId + "\t" + word + "\n");
////						wordId++;
////					}
////					doc.add(wordToIdMap.get(word));
////				} // end of for
//				// if docs are going to be string words
//				String doctext = bow.toString().replace("[", "").replace("]", "").replace(",", "");
//				
//				// if docs are going to be integer words
////				String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
//				if (doctext.length() > 0)
//				movieFeaturefile.write(mId + " " + doctext + "\n");
//				// end of if
//			} // end of while
//			br.close();
//			movieFeaturefile.close();
////			vocabfile.close();
//			System.out.println("done!");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	} // end of createMovieFeatures
//	
//
//
	public void createMovieFeatures() {
		System.out.println("Creating movie features...");
		String movieLookupFile = "/home/mehdi/lodproject/movieLookup.txt";
		Map<String,Integer> wordToIdMap = new HashMap<String,Integer>();
		int wordId = 0;
		try {
			FileWriter movieFeaturefile = new FileWriter("/home/mehdi/lodproject/movieFeatures.txt");
//			FileWriter vocabfile = new FileWriter("/home/mehdi/lodproject/vocabulary.txt");
			BufferedReader br = new BufferedReader(new FileReader(movieLookupFile));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split("\t");
				String mId = tokens[0];
				String mname = tokens[2];
				List<String> bow = null;
				// check to make sure name is the title of the entity
				String redirectname = getRedirectName(mname);
				if (mname.equals("Contact_(1997_American_film)")) {
					bow = readDbpediaConceptFile(mname);
					String doctext = bow.toString().replace("[", "").replace("]", "").replace(",", "");
					movieFeaturefile.write(mId + " " + doctext + "\n");
					continue;
				}
				if (redirectname.equals("")) {
					// get related triples and construct bag of words from categories
//					bow = constructBowFromCategories(mname);
					
					bow = constructBowFromProperties(mname);
				}else {
//					bow = constructBowFromCategories(redirectname);
	
					bow = constructBowFromProperties(redirectname);
				}
				
				// needed if docs are going to be integer words
//				List<Integer> doc = new ArrayList<Integer>(bow.size()); 
//				for (String word : bow) {
//					if(wordToIdMap.get(word) == null) {
//						wordToIdMap.put(word, wordId);
//						vocabfile.write(wordId + "\t" + word + "\n");
//						wordId++;
//					}
//					doc.add(wordToIdMap.get(word));
//				} // end of for
				// if docs are going to be string words
				String doctext = bow.toString().replace("[", "").replace("]", "").replace(",", "");
				
				// if docs are going to be integer words
//				String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
				if (doctext.length() > 0) {
					movieFeaturefile.write(mId + " " + doctext + "\n");
				}else {
					System.out.println(mname + " : " + redirectname);
				}
				// end of if
			} // end of while
			br.close();
			movieFeaturefile.close();
//			vocabfile.close();
			System.out.println("done!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createMovieFeatures
	
	public List<String> constructBowFromCategories(String entity) {
		QueryEngineHTTP qe = null;
		List<String> words = new ArrayList<String>(10000);
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			ResultSet results = null;
			queryString.append("SELECT ?cat ?supcat FROM <" + GRAPH + "> WHERE ");
			queryString.append("{ <" + uriPrefix + entity + "> <http://purl.org/dc/terms/subject> ?cat . ");
			queryString.append("?cat <http://www.w3.org/2004/02/skos/core#broader> ?supcat . ");
			queryString.append("}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String term = result.getResource("cat").toString().substring(catUriPrefix.length()).replace("_", "").replace(".", "").replace("-", "").replace("'", "").replace("/", "").toLowerCase();
				
				if (term.indexOf("by") == -1 && term.indexOf("language") == -1 && term.indexOf("%") == -1 && term.indexOf("(") == -1 && term.indexOf("*") == -1 && !term.matches(".*\\d.*")) {
					words.add(term);
				}
				term = result.getResource("supcat").toString().substring(catUriPrefix.length()).replace("_", "").replace(".", "").replace("-", "").replace("'", "").replace("/", "").toLowerCase();
				if (term.indexOf("by") == -1 && term.indexOf("language") == -1 && term.indexOf("%") == -1 && term.indexOf("(") == -1 && term.indexOf("*") == -1 && !term.matches(".*\\d.*")) {
					words.add(term);
				}
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		return words;
	}
	
	public String getRedirectName(String entity) {
		String etitle = "";
		QueryEngineHTTP qe = null;
		try {
			String endpoint = "http://localhost:8890/sparql";
			ResultSet results = null;
			StringBuffer queryString = new StringBuffer(400);
			queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + entity + "> <http://dbpedia.org/ontology/wikiPageRedirects> ?o . ");
			queryString.append("}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				RDFNode r = result.get("o");
				if (r.isResource()) {
					etitle = r.toString().substring(uriPrefix.length());
				}
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		return etitle;
	} // end of getRedirectName
	
	public Set<String> readStopWordsSet() {
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
		Set<String> stopWords = new HashSet<String>(Arrays.asList(content.split(",")));
		return stopWords;
	} // end of readStopWordsSet
	
	public List<String> readDbpediaConceptFile(String filename) {
		filename = "/home/mehdi/lodproject/" + "Contact_(1997_American_film)" + ".csv";
		List<String> words = new ArrayList<String>(10000);
		Set<String> stopWords = readStopWordsSet();
		stopWords.add("films");
		stopWords.add("film");
		stopWords.add("filmed");
		stopWords.add("english");
		stopWords.add("based");
		stopWords.add("set");
		stopWords.add("language");
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				line = line.replace("\"", "");
				if (line.indexOf(catUriPrefix) != -1 && line.indexOf("%") == -1) {
					line = line.substring(catUriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "").replace("-", "_").replace("&", "").replace("!", "").replace("'", "");
					String [] tokens = line.split("_");
					for (String w : tokens) {
						if (!stopWords.contains(w) && w.indexOf("%") == -1 && w.indexOf("*") == -1 && !w.matches(".*\\d.*") && w.length() > 2 ) {
							words.add(w);
						}
					}
				}else if (line.indexOf("%") == -1 && line.indexOf("http://dbpedia.org") != -1) {
//					System.out.println(term);
					line = line.substring(uriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "").replace("-", "_").replace("&", "").replace("!", "").replace("'", "");
					String [] tokens = line.split("_");
					for (String w : tokens) {
						if (!stopWords.contains(w) && w.indexOf("%") == -1 && w.indexOf("*") == -1 && !w.matches(".*\\d.*") && w.length() > 2 ) {
							words.add(w);
						}
					}
				}
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return words;
	} // end of readDbpediaConceptFile
	
	public List<String> constructBowFromProperties(String entity) {
		QueryEngineHTTP qe = null;
		List<String> words = new ArrayList<String>(10000);
		Set<String> stopWords = readStopWordsSet();
		stopWords.add("films");
		stopWords.add("film");
		stopWords.add("filmed");
		stopWords.add("english");
		stopWords.add("based");
		stopWords.add("set");
		stopWords.add("language");
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			ResultSet results = null;
			queryString.append("SELECT DISTINCT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("{ <" + uriPrefix + entity + "> ?p ?o . ");
			queryString.append("FILTER (!isLiteral(?o) && contains (str(?p),\"http://dbpedia.org/\") && !contains (str(?p),\"sameAs\") ");
			queryString.append("&& !contains (str(?p),\"xmlns.com\") && !contains (str(?p),\"/abstract\") ");
			queryString.append("&& !contains (str(?p),\"wikiPage\") && !contains (str(?p),\"/image\") && !contains (str(?p),\"/thumbnail\")) ");
			queryString.append("} UNION {");
			queryString.append("<" + uriPrefix + entity + "> <http://purl.org/dc/terms/subject> ?o . ");
			queryString.append("}}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				 String term =result.getResource("o").toString();
				if (term.indexOf(catUriPrefix) != -1 && term.indexOf("%") == -1) {
					term = term.substring(catUriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "").replace("-", "_").replace("&", "").replace("!", "").replace("'", "");
					String [] tokens = term.split("_");
					for (String w : tokens) {
						if (!stopWords.contains(w) && w.indexOf("%") == -1 && w.indexOf("*") == -1 && !w.matches(".*\\d.*") && w.length() > 2 ) {
							words.add(w);
						}
					}
				}else if (term.indexOf("%") == -1 && term.indexOf("http://dbpedia.org") != -1) {
//					System.out.println(term);
					term = term.substring(uriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "").replace("-", "_").replace("&", "").replace("!", "").replace("'", "");
					String [] tokens = term.split("_");
					for (String w : tokens) {
						if (!stopWords.contains(w) && w.indexOf("%") == -1 && w.indexOf("*") == -1 && !w.matches(".*\\d.*") && w.length() > 2 ) {
							words.add(w);
						}
					}
				}
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		return words;
	} // end of constructBowFromProperties
	
	public List<String> constructBowFromProperties1(String entity) {
		QueryEngineHTTP qe = null;
		List<String> words = new ArrayList<String>(10000);
		Set<String> stopWords = readStopWordsSet();
		stopWords.add("films");
		stopWords.add("film");
		stopWords.add("filmed");
		stopWords.add("english");
		stopWords.add("based");
		stopWords.add("set");
		stopWords.add("language");
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			ResultSet results = null;
			queryString.append("SELECT DISTINCT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("{ <" + uriPrefix + entity + "> ?p ?o . ");
			queryString.append("FILTER (!isLiteral(?o) && contains (str(?p),\"http://dbpedia.org/\") && !contains (str(?p),\"sameAs\") ");
			queryString.append("&& !contains (str(?p),\"xmlns.com\") && !contains (str(?p),\"/abstract\") ");
			queryString.append("&& !contains (str(?p),\"wikiPage\") && !contains (str(?p),\"/image\") && !contains (str(?p),\"/thumbnail\")) ");
			queryString.append("} UNION {");
			queryString.append("<" + uriPrefix + entity + "> <http://purl.org/dc/terms/subject> ?o . ");
			queryString.append("}}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				 String term =result.getResource("o").toString();
				if (term.indexOf(catUriPrefix) != -1 && term.indexOf("%") == -1) {
					term = term.substring(catUriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "").replace("-", "_").replace("&", "").replace("!", "").replace("'", "");
					words.add(term);
				}else if (term.indexOf("%") == -1 && term.indexOf("http://dbpedia.org") != -1) {
//					System.out.println(term);
					term = term.substring(uriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "").replace("-", "_").replace("&", "").replace("!", "").replace("'", "");
					words.add(term);
				}
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		return words;
	} // end of constructBowFromProperties

	public void createUserMovieProfile() {
		System.out.println("Creating user profile...");
		String inputfilename = "/home/mehdi/lodproject/corpus.txt";
		// read the movielens2Dbpedia map file
		Map<String,String> movielens2Dbpedia = readMovieLensFile();
		int userId = 0;
		int movieId = 0;
		Set<String> duplicateUsers = new HashSet<String>();
		Set<String> duplicateMovies = new HashSet<String>();
		Map<String,Integer> userToIdMap = new HashMap<String,Integer>();
		Map<String,Integer> movieToIdMap = new HashMap<String,Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputfilename));
			FileWriter umfile = new FileWriter("/home/mehdi/lodproject/userprofile.txt");
			FileWriter userLookupfile = new FileWriter("/home/mehdi/lodproject/userLookup.txt");
			FileWriter movieLookupfile = new FileWriter("/home/mehdi/lodproject/movieLookup.txt");
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split(" ");
				String uId = tokens[0];
				String mId = tokens[1];
				int rate = Integer.valueOf(tokens[2]);
				if (rate > 3) {
					if (!duplicateUsers.contains(uId)) {
						duplicateUsers.add(uId);
						userLookupfile.write(userId + "\t" + uId + "\n");
						userToIdMap.put(uId, userId);
						userId++;
					}
					if (!duplicateMovies.contains(mId)) {
						duplicateMovies.add(mId);
						movieLookupfile.write(movieId + "\t" + mId + "\t" + movielens2Dbpedia.get(mId) + "\n");
						movieToIdMap.put(mId, movieId);
						movieId++;
					}
					umfile.write(userToIdMap.get(uId) + " " + movieToIdMap.get(mId) + " " + rate + "\n"); 
				}
			} // end of while
			br.close();
			umfile.close();
			userLookupfile.close();
			movieLookupfile.close();
			System.out.println("done.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createUserMovieProfile
	
	public Map<String, List<String>> createUserProfileMap() {
		String inputfilename = "/home/mehdi/lodproject/userprofile.txt";
		Map<String,List<String>> userProfile = new TreeMap<String,List<String>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputfilename));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split(" ");
				String uId = tokens[0];
				String mId = tokens[1];
				String rate = tokens[2];

				if (userProfile.get(uId) == null) {
					List<String> p = new ArrayList<String>();
					p.add(mId + " " + rate);
					userProfile.put(uId, p);
				}else {
					List<String> p = userProfile.get(uId);
					p.add(mId + " " + rate);
					userProfile.put(uId, p);
				}
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userProfile;
	} // end of createUserProfileMap

	public void createTrainingTestSets(int kFold, int slice) {
		
		Map<String,List<String>> userProfile = createUserProfileMap();
		try {
			FileWriter trainingfile = new FileWriter("/home/mehdi/lodproject/trainingset" + slice + ".txt");
			FileWriter testfile = new FileWriter("/home/mehdi/lodproject/testset" + slice + ".txt");
			// create a k-fold training/test sets
			for(Map.Entry<String, List<String>> entry : userProfile.entrySet()) {
				String uId = entry.getKey();
				List<String> p = entry.getValue();
				int r = p.size() / kFold;
				if (r == 0) {
					System.out.println("UserId = " + uId + " does not have test record!");
				}
				Set<Integer> heldoutIndices = new HashSet<Integer>();
				int stIndex = (slice - 1) * r;
				int endIndex = stIndex + r;
				for (int i = stIndex; i < endIndex; i++) {
					heldoutIndices.add(i);
				}
				for (int i = 0; i < p.size(); i++) {
					if (heldoutIndices.contains(i)) {
						testfile.write(uId + " " + p.get(i) + "\n");
					}else {
						trainingfile.write(uId + " " + p.get(i) + "\n");
					}
				} // end of for
			} // end of for
			trainingfile.close();
			testfile.close();
			System.out.println("done.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createTrainingTestSets
	
	
	public List<String> readFile(String filename) {
		List<String> corpus = new ArrayList<String>(fileNumOfLines);
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				corpus.add(line);
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return corpus;
	} // end of readFile
	
	public void initializeMatrices() {
		Ntu = allocateMemory(Ntu, nUsers, nTopics);
		Nmt = allocateMemory(Nmt, nTopics, nMovies);
		Nct = allocateMemory(Nct, nTopics, nFeatures);
		Nu = allocateMemory(Nu, nUsers);
		Nt_m = allocateMemory(Nt_m, nTopics);
		Nt_c = allocateMemory(Nt_c, nTopics);
		z = allocateMemory(z, nRecords);
		c1 = allocateMemory(c1, nRecords);
		c2 = allocateMemory(c2, nRecords);
		c3 = allocateMemory(c3, nRecords);
		c4 = allocateMemory(c4, nRecords);
		c5 = allocateMemory(c5, nRecords);
		theta = allocateMemory(theta, nUsers, nTopics);
		phi = allocateMemory(phi, nTopics, nMovies);
		zeta = allocateMemory(zeta, nTopics, nFeatures);
	} // end of initializeMatrices
	
	public void initializeMatricesForTest() {
		loadCountMatrices();
		Nu = allocateMemory(Nu, nUsers);
		Nt_m = allocateMemory(Nt_m, nTopics);
		Nt_c = allocateMemory(Nt_c, nTopics);
		Nu = fillMatrixFromMatrix(Nu, Ntu);
		Nt_m = fillMatrixFromMatrix(Nt_m, Nmt);
		Nt_c = fillMatrixFromMatrix(Nt_c, Nct);
		z = allocateMemory(z, nRecords);
		c1 = allocateMemory(c1, nRecords);
		c2 = allocateMemory(c2, nRecords);
		c3 = allocateMemory(c3, nRecords);
		theta = allocateMemory(theta, nUsers, nTopics);
		phi = allocateMemory(phi, nTopics, nMovies);
		zeta = allocateMemory(zeta, nTopics, nFeatures);
	} // end of initializeMatricesForTest
	
	public int[] fillMatrixFromMatrix(int[] toArr, int[][] fromArr) {
		int rows = fromArr.length;
		int cols = fromArr[0].length;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				toArr[i] += fromArr[i][j];
			}
		} // end of for
		return toArr;
	} // end of fillMatrixFromMatrix

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

	public void doInitialization(String corpusFilename) {
		initializeParameters(corpusFilename);
		fillArrays();
		initializeMatrices();
	} // end of doInitialization
	
	public void doInitializationForTest(String testFilename) {
		initializeParametersForTest(testFilename);
		fillArrays();
		initializeMatricesForTest();
	} // end of doInitializationForTest
	
	
	public void extractFeaturesForMovies() {
		String movieLookupFile = "/home/mehdi/lodproject/movieLookup.txt";
		try {
			FileWriter movieFeaturefile = new FileWriter("/home/mehdi/lodproject/movieFeatures.txt");
			BufferedReader br = new BufferedReader(new FileReader(movieLookupFile));
			String line = "";
			while ((line = br.readLine()) != null) {
				String [] tokens = line.split("\t");
				String mId = tokens[0];
				String mname = tokens[1];
//				String doctext = bow.toString().replace("[", "").replace("]", "").replace(",", "");
//				
//				// if docs are going to be integer words
////				String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
//				if (doctext.length() > 0)
//				movieFeaturefile.write(mId + " " + doctext + "\n");
				// end of if
			} // end of while
			br.close();
			movieFeaturefile.close();
			System.out.println("done!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of extractFeaturesForMovies



}
