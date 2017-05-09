/**
 * 
 */
package cs.uga.edu.enttopicmodel;

/**
 * @author mehdi
 *
 */
public class EntLDA_Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		EntLDA entLDA = new EntLDA();
		entLDA.initializeMatrices();
		
		// Gibbs Sampling Initialization and Run //
	//	entLDA.initializeGibbsSampling();
	//	entLDA.run();
		
//      entLDA.writeToCSV();
		
		
		
		
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
