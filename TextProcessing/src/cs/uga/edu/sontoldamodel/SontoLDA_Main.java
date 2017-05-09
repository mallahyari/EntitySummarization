/**
 * 
 */
package cs.uga.edu.sontoldamodel;

/**
 * @author mehdi
 *
 */
public class SontoLDA_Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		SontoLDA solda = new SontoLDA();
		solda.initializeMatrices();
		
		// Gibbs Sampling Initialization and Run //
		solda.initializeGibbsSampling();
//		solda.run();
//		
		
//		solda.writeToCSV();
		
		
		
		
//		EntLDA2 entLDA2 = new EntLDA2();
//		entLDA2.initializeMatrices();
//		
//		// Gibbs Sampling Initialization and Run //
//		entLDA2.initializeGibbsSampling();
//		entLDA2.run();
//		
		
		
		
		
		
//		
		
		// Load Posterior to memory and construct the Taxonomy //
//		entLDA.loadPosteriorDistribution();
		
		
//		int [] a =new int [0];
//		int t =20;
//		System.out.println(Math.round(0.9128732*1000) / 1000.);

	}

}
