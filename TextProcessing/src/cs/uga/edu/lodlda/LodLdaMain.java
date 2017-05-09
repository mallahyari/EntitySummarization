/**
 * 
 */
package cs.uga.edu.lodlda;

import java.util.List;

/**
 * @author mehdi
 *
 */
public class LodLdaMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LodLda lodlda = new LodLda();
		
		// run only once
//		lodlda.createCorpus();
		
		// create a user profile file
//		lodlda.createUserMovieProfile();
		
		// create a k-fold training/testing set from the user profile
//		int kfold = 5;
//		int run = 5;
//		lodlda.createTrainingTestSets(kfold, run);
		
		// create a bag of words for each movie
//		lodlda.createMovieFeatures();
		
		// train the model
//		String corpusFilename = "/home/mehdi/lodproject/trainingset1.txt";
//		lodlda.doInitialization(corpusFilename);
//		lodlda.initializeGibbsSampling();
//		lodlda.run("training");
		
		
		// test the model
//		String testFilename = "/home/mehdi/lodproject/testset5.txt";
//		lodlda.doInitializationForTest(testFilename);
//		lodlda.initializeGibbsSampling();
//		lodlda.run("testing");
		
//		 top-n recommendation
		/*
		String testFilename = "/home/mehdi/lodproject/testset5.txt";
		lodlda.loadPosteriorDistribution();
		// compute the recommendation score for each user and movie according the test set
		lodlda.computeRecommendationScore(testFilename);
		for (int n = 1; n <= 50; n++) {
			System.out.print("n = " + n + "  ");
			lodlda.findTopNRecommendation(n,testFilename);
//			String filename = "/home/mehdi/Downloads/librec/librec/Results/results-BPR-Top-50/run-5/BPR-top-50-items.txt";
//			lodlda.findTopNRecommendationForBaselineKNN(filename,n,"BPR");
//			System.out.println();
			//String filename = "/home/mehdi/Downloads/librec/librec/Results/results-ItemKNN-Top-50/run-1/ItemKNN-top-50-items.txt";
			//lodlda.findTopNRecommendationForBaselineKNN(filename,n,"ItemKNN");
			//filename 	    = "/home/mehdi/Downloads/librec/librec/Results/results-UserKNN-Top-50/run-1/UserKNN-top-50-items.txt";
			//lodlda.findTopNRecommendationForBaselineKNN(filename,n,"UserKNN");
		} // end of for
		*/
		
		
		
//		List<String> a = lodlda.constructBowFromProperties1("Back_to_the_Future");
//		for(String e : a) 
//		System.out.println(e);
	}

}
