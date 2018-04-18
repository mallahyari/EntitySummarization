package cs.uga.edu.ontoPart;

/**
 * 
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mehdi
 *
 *
 *
 */
public class ModelParameters {
	
	public int fileNumOfLines = 150000;
	public int [] w = null;
	public int [] d = null;
	public int [] docIds = null;
	public int [] wordIds = null;
	public int [] wordsCounts = null;
	public int D = 0;
	public int W = 0;
//	public int T = 22;
	public int T1 = 57;
	public int T2 = 133;
	public int P = 0; 
	public int N = 0;
	public int nIterations = 2000;
	int burnIn = 200;
	public double ALPHA = 60.0 / 390;
	public final double BETA  = 0.01;
//	public final double TAU = 50.0 / T;
//	public final double EPSILON = 0.9;
	public final double gamma = 0.01	;
	public String corpusFilename = "/home/mehdi/ontoPart/evaluation/corpus.txt";
	public String entitiesFilename = "/home/mehdi/ontoPart/evaluation/entNameOnly.txt";
	public String corpusStatFilename = "/home/mehdi/ontoPart/evaluation/corpusStatistics.txt";
	public String predicateFilename = "/home/mehdi/ontoPart/evaluation/predicateToId.txt";
//	public String corpusFilename = "/home/mehdi/taxonomyProject/preprocessedFiles/corpus.txt";
//	public String entitiesFilename = "/home/mehdi/taxonomyProject/preprocessedFiles/corpusConceptsSr.txt";
//	public String corpusStatFilename = "/home/mehdi/taxonomyProject/preprocessedFiles/corpusStatistics.txt";
	
	public ModelParameters() {
		initializeParameters();
		fillArrays();
	}
	
	public void initializeParameters() {
		System.out.print("Reading corous file...");
		List<String> corpus = readFile(corpusFilename);
		System.out.println("done!");
		docIds	= new int [corpus.size()];
		wordIds = new int [corpus.size()];
		wordsCounts = new int [corpus.size()];
		Set<Integer> uniqueWordIds = new HashSet<Integer>();
		Set<Integer> uniqueDocIds = new HashSet<Integer>();
//		Set<Integer> td = new HashSet<Integer>();
//		for (int i = 0; i < 7104; i++) {
//			td.add(i);
//		}
		for (int i = 0;i < corpus.size();i++) {
			String [] tokens = corpus.get(i).split(" ");
			docIds [i] = Integer.parseInt(tokens [0]);
			wordIds [i] = Integer.parseInt(tokens [1]);
			wordsCounts [i] = Integer.parseInt(tokens [2]);
			uniqueDocIds.add(docIds [i]);
			uniqueWordIds.add(wordIds [i]);
			N += wordsCounts [i];
		} // end of for
//		for (int i: td){
//			if (!uniqueDocIds.contains(i)) System.out.println(i);
//		}
		D = uniqueDocIds.size() ;
		W = uniqueWordIds.size();

		BufferedWriter bw1 = null;
		try {

			bw1 = new BufferedWriter(new FileWriter(corpusStatFilename, true));
			bw1.write(  "numberOfEntity, sizeOfVocabulary(Object), numberOfAllObjects(Words)");
			bw1.newLine();
			bw1.write(  D + " " + W + " " + N);

		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			bw1.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("D: " + D + " W: " + W + " N: " + N);

		//Counting number of Predicate or P 

		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new FileReader(predicateFilename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			while (reader.readLine() != null) P++;
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Number of Predicate =" + P);
	} // end of initializeParameters
	
	public void fillArrays() {
		System.out.print("Filling arrays...");
//		long st = System.currentTimeMillis();
		d = new int [N];   // matrix holding all the documents of words
		w = new int [N];   // matrix keeping all the words of all the documents
		int count = 0;
		for (int j = 0; j < wordsCounts.length; j++) {
			for (int i = count; i < count + wordsCounts[j]; i++) {
				d [i] = docIds [j];
				w [i] = wordIds [j];
				//System.out.println(i);
			} // end of for i
			count += wordsCounts[j];
//			System.out.println(count);
		} // end of for j
//		double dt = (System.currentTimeMillis() - st);// / 1000.;
//		System.out.println("time: " + dt);
		System.out.println("done!");
	} // end of fillArrays
	
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

}
