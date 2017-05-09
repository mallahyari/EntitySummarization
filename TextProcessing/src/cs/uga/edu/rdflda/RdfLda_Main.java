/**
 * 
 */
package cs.uga.edu.rdflda;


/**
 * @author mehdi
 *
 */
public class RdfLda_Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RdfLda rdflda = new RdfLda();
		
		// run it only once
//		rdflda.createDataSet();
		
		// load the dataset
		rdflda.loadDatasetToMemory();
		String outputDir = "/home/mehdi/rdfproject/preprocessedFiles/";
//		rdflda.createCorpus(outputDir);
		
		// create a corpus from all the types in the Dbpedia
//		rdflda.createLabeledCorpus(outputDir);
		
		// create a corpus from Dbpedia in a random order
//		rdflda.createRandomLabeledCorpus(outputDir);
		
		// create a corpus from related properties and types
//		rdflda.createCorpusInOneFile(outputDir);
		
		rdflda.createRandomCorpus(outputDir);
		
	}

	

}
