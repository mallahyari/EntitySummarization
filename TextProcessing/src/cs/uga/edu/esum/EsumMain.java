/**
 * 
 */
package cs.uga.edu.esum;

import java.io.FileNotFoundException;
import java.io.IOException;

import cs.uga.edu.enttopicmodel.EntLDA;

/**
 * @author mehdi
 *
 */
public class EsumMain {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		EntityProc readWriteEnt =new EntityProc();
//		readWriteEnt.readWriteEntity();
		
	
		EntityProc predicCheck =new EntityProc();
		predicCheck.predicateChecker();
		predicCheck.makingPredicateList();
		predicCheck.domainRangeExtractor();
		
		
//		EntityProc corpusMaker =new EntityProc();
//		corpusMaker.corpusMaker();
		
//	SumModelParameters paramIniti =new SumModelParameters();
		
//******************************************************\\
		
		
//		EntSum entSum = new EntSum();
//		entSum.initializeMatrices();
		
		// Gibbs Sampling Initialization and Run //
//		entSum.initializeGibbsSampling();
//		entSum.run();
		

		
//      entSum.writeToCSV();
		
		
		
		
//		EntLDA2 entLDA2 = new EntLDA2();
//		entLDA2.initializeMatrices();
//		
//		// Gibbs Sampling Initialization and Run //
//		entLDA2.initializeGibbsSampling();
//		entLDA2.run();
//		
		
		
		
		
		
		
		
	}

}
