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
	
		/* Step 1: if necessary
		 * readWriteEntity : Read entity file as input (instances.txt) write entity to name only file as output (entNameOnly.txt)
		 */
		//EntityProc readWriteEnt =new EntityProc();
		//readWriteEnt.readWriteEntity();
		//**********************\\\
	
		
		/* Step 2: 
		 * Read entity Name only (entNameOnly.txt) as input and extract all useful predicates
		 * predicateChecker will also Call predicateExtractor to extract all predicates and Objects for all entities in entNameOnly.txt
		 * 
		 * predicateExtractor taking entity name and extract all predicates (meaningful ones) . it makes doc from all extracted Objacts (Barack_Obama.txt)
		 * Also predicateExtractor makes docToID.txt contains all entities with ID (Barack_Obama  0)
		 * It also makes creating a MAP from each Word to ID (mapWordToID)
		 *
		 *making PredicateList 	//Making Predicate list text file predicateList.txt  contains full URL predicate with a unique number. This functions needs 
		 *to be ran after predicateChekcer because this function will fill out the list of perdicate in predicateSet 
		 */
		
		//EntityProc predicCheck = new EntityProc();
		//predicCheck.processEntities();
		//predicCheck.makeCorpus();
		//System.out.println("Done!"); 
		
		
//		predicCheck.predicateChecker();
//		predicCheck.makingPredicateList();
	
		/*
		 * Step 3 :  Extract Domain and Range for each predicate and make predicateDomainRange.txt
		 */
		//	predicCheck.domainRangeExtractor();
		
		/*
		 * Step 4 :  create corpus.txt (entityID, wordID, frequency)
		 */	
		//EntityProc corpusMaker =new EntityProc();
		//corpusMaker.corpusMaker();
		
//	SumModelParameters paramIniti =new SumModelParameters();
		
//******************************************************\\
		
		EntSumModel entSum = new EntSumModel();
		//entSum.initializeMatrices();
		
		// Gibbs Sampling Initialization and Run //
		//entSum.initializeGibbsSampling();
		//entSum.run();
		
		
		// Old Model 

//		EntSum entSum = new EntSum();
//		entSum.initializeMatrices();
//		
//		// Gibbs Sampling Initialization and Run //
//		entSum.initializeGibbsSampling();
//		entSum.run();

		
     entSum.writeToCSV();
		
		
		
		
//		    EntLDA2 entLDA2 = new EntLDA2();
//		entLDA2.initializeMatrices();
//		
//		// Gibbs Sampling Initialization and Run //
//		entLDA2.initializeGibbsSampling();
//		entLDA2.run();
//		
		
		
		
		
		
		
		
	}

}
