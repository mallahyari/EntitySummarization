/**
 * 
 */
package cs.uga.edu.topicmodel;
import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;

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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.text.StyledEditorKit.ItalicAction;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import virtuoso.jena.driver.VirtGraph;
import cs.uga.edu.dicgenerator.CategoryNameMapper;
import cs.uga.edu.dicgenerator.DbpediaProcessor;
import cs.uga.edu.dicgenerator.LocalNameMapperClass;
import cs.uga.edu.properties.Configuration;
import cs.uga.edu.wikiaccess.WikipediaAccessLayer;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.PropertyList;

/**
 * @author Mehdi
 *
 */
public class ImportData {

	Pipe pipe = null;
	
	public ImportData() {
		pipe = buildPipe();
	}
	
	/**
	 * @return
	 */
	public Pipe buildPipe() {	
		List<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add(new Input2CharSequence("UTF-8"));
//		Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+");
		Pattern tokenPattern = Pattern.compile("[\\p{L}_]+");
		pipeList.add(new CharSequence2TokenSequence(tokenPattern));
		pipeList.add(new TokenSequenceLowercase());
		pipeList.add(new TokenSequenceRemoveStopwords(new File("/home/mehdi/stopwords.txt"),"UTF-8",false,false, false));
		pipeList.add(new TokenSequence2FeatureSequence());
		// when ran standard lda through java comment the line below
//		pipeList.add(new FeatureSequence2FeatureVector());
//		pipeList.add(new PrintInputAndTarget());
		return new SerialPipes(pipeList);
	} // end of buildPipe
	
	public InstanceList readDirectory(File directory) {
        FileIterator itr = new FileIterator(directory, null, FileIterator.LAST_DIRECTORY);
        Pipe p = new MyPipe().getPipe();
        InstanceList instances = new InstanceList(p);
//        InstanceList instances = new InstanceList(pipe);
        instances.addThruPipe(itr);
        return instances;
    } // end of readDirectory
	
	public Set<Integer> getIntersectionVocabularyId(InstanceList documents, InstanceList wikiConcepts) {
//		importDocuments();
//		loadConceptsVocabulary();
		Alphabet dVocab = documents.getDataAlphabet();
		//dVocab.dump();
		Alphabet cVocab = wikiConcepts.getDataAlphabet();
		Object [ ] dvocabs = dVocab.toArray();
//		int i = 1;
		Set<Integer> newVocab = new HashSet<Integer>(dVocab.size());
		for (Object obj : dvocabs) {
			if (cVocab.contains(obj)) {
				newVocab.add(dVocab.lookupIndex(obj));
//				System.out.println("i: " + i + " " + obj.toString());
//				i++;
			}else{
				//System.out.println("word: " + obj.toString());
			} // end of if
		} // end of for
		return newVocab;
	} // end of getIntersectionVocabulary
	
	// This method is used for sOntoLDA project - Reuters dataset
	public Map<String, Integer> createCorpusFilesForReutersDataset(String inputFilesPath, String wikiFilesPath, String outputFilesPath, String corpusfile, String vocabFile, String wikiConceptFile) throws IOException {
		System.out.println("Reading Wikipedia vocabulary...");
		List<String>  vocablines = getDocumentContent(outputFilesPath + vocabFile);
		Map<String, Integer> wordToIdMap = new HashMap<String,Integer>();
		for (String line : vocablines) {
			String [] tokens = line.split("\t");
			wordToIdMap.put(tokens[1], Integer.parseInt(tokens[0]));
		} // end of for
		
		File inputDirectory = new File(inputFilesPath);
		InstanceList documents = readDirectory(inputDirectory);
		Alphabet documentAlphabet = documents.getDataAlphabet();
		
		Map<Integer, Integer> wordsFrequency = new HashMap<Integer, Integer>(documents.getDataAlphabet().size());
		System.out.println("Documents initial Vocabulary Size: " + documents.getDataAlphabet().size());
		int minFrequency = Integer.valueOf(Configuration.getProperty("wordFrequency"));
		
		// Count the frequency of words in the corpus
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
			//System.out.println(words.toString());
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
				String word = documentAlphabet.lookupObject(wordId).toString();
				if (wordToIdMap .get(word) == null) continue;
				wordId = wordToIdMap.get(word);  // id of the word in the vocabulary
				int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
				int preFreq = wordsFrequency.get(wordId) != null ? wordsFrequency.get(wordId) : 0;
				wordsFrequency.put(wordId, frequency + preFreq);
			} // end of for
		} // end of for
		// Remove less frequent words, i.e words less than minFrequency (e.g 5) times in the corpus
		Iterator<Map.Entry<Integer, Integer>> itr = wordsFrequency.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			if (wordsFrequency.get(entry.getKey()) < minFrequency) {
//				System.out.println("key:"+entry.getKey() + " f:" +entry.getValue());
				itr.remove();
			} // end of if
		} // end of while
		System.out.println("Vocabulary Size after removing less frequent words: " + wordsFrequency.size());
		System.exit(0);
		Set<Integer> vocabularyIdSet = wordsFrequency.keySet();
		int docId = 0;
		FileWriter fout = new FileWriter(outputFilesPath + corpusfile);
		FileWriter fout2 = new FileWriter(outputFilesPath + "documentLookup.txt");
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
//			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1); // this name includes the file extension ".txt"
			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1,doc.getName().toString().indexOf(".txt")); // this name excludes the file extension ".txt"
			if (words.numLocations() > 0) {
				fout2.write(docId + "\t" + docName + "\n");
				fout2.flush();
				for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
					int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
					String word = documentAlphabet.lookupObject(wordId).toString();
					if (wordToIdMap.get(word) == null) continue;
					wordId = wordToIdMap.get(word);  // id of the word in the vocabulary
					int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
					if (vocabularyIdSet.contains(wordId)) {
						String line = docId + " " + wordId + " " + frequency + "\n";
						fout.write(line);
					} // end of if
				} // end of for
				docId++;
			} // end of if
		} // end of for
		fout.flush();
		fout.close();
		fout2.close();
		System.out.println("Document Lookup file created.");
		System.out.println(corpusfile + " file created.");
//		Map<String,Integer> corpusConceptIdMap = new HashMap<String,Integer>();
		
		Map<String, Integer> corpusConceptIdMap = new HashMap<String, Integer>();
		return corpusConceptIdMap;
	} // end of createCorpusFilesForReutersDataset
	

	
	
	public Map<String, Integer> createTestCorpusFiles(String inputFilesPath, String outputFilesPath, String corpusfile, String vocabFile) throws IOException {
		System.out.println("Reading Test documents...");
		File inputDirectory = new File(inputFilesPath);
		InstanceList documents = readDirectory(inputDirectory);
		System.out.println("done.");
		System.out.println("Test Documents vocab size: " + documents.getDataAlphabet().size());
		
		String [] vocab = readDocument(outputFilesPath + vocabFile).split("\n");
		Map<String, Integer> vocabLookup = new HashMap<String, Integer>();
		for (String v : vocab) {
			Integer vid = Integer.parseInt(v.split("\t")[0]);
			String term = v.split("\t")[1];
			vocabLookup.put(term,vid);
		}
		int docId = 0;
		Map<String,Integer> documentIdMap = new HashMap<String,Integer>(documents.size());
		FileWriter fout = new FileWriter(outputFilesPath + corpusfile);
		FileWriter fout2 = new FileWriter(outputFilesPath + "testdocumentLookup.txt");
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
//			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1); // this name includes the file extension ".txt"
			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1,doc.getName().toString().indexOf(".txt")); // this name excludes the file extension ".txt"
			if (words.numLocations() > 0) {
				documentIdMap.put(docName,docId);
				fout2.write(docId + "\t" + docName + "\n");
				fout2.flush();
				for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
					int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
					int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
					String word = words.getAlphabet().lookupObject(wordId).toString();
					if (vocabLookup.get(word) != null) {
						String line = docId + " " + vocabLookup.get(word) + " " + frequency + "\n";
						fout.write(line);
					} // end of if
				} // end of for
				docId++;
			} // end of if
		} // end of for
		fout.flush();
		fout.close();
		fout2.close();
		System.out.println(" Document Lookup file created.");
		System.out.println(corpusfile + " file created.");
		Map<String,Integer> corpusConceptIdMap = new HashMap<String,Integer>();
		
		
		
		/*
		int conceptId = 0;
		FileWriter foutconceptLookup = new FileWriter(outputFilesPath + "corpusConceptsLookup.txt");
		fout = new FileWriter(outputFilesPath + wikiConceptFile);
		System.out.println("size of wiki concepts: " + wikiConcepts.size());
		for (Instance doc : wikiConcepts) {
			FeatureVector words = (FeatureVector) doc.getData();
			String conceptName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1).replace(".txt", "");
//			if (conceptName.equals("Issue_tracking_system") || conceptName.equals("Richard_Shiffrin")) {
//				continue;
//			}
			String line = conceptId + "\t" + conceptName + "\n";
			foutconceptLookup.write(line);
			corpusConceptIdMap.put(conceptName, conceptId);
//			System.out.println("cid: " +conceptId + " " + conceptName);
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word in the Wiki alphabet
//				int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the Wiki page
				String word = wikiConceptsAlphabet.lookupObject(wordId).toString();
				int idInDocAlphabet = documentAlphabet.lookupIndex(word);
				if (vocabularyIdSet.contains(idInDocAlphabet)) {
					line = conceptId + " " + oldIdToNewId.get(idInDocAlphabet) + "\n";
					fout.write(line);
				}
			} // end of for
			conceptId++;
		} // end of for
		foutconceptLookup.flush();
		foutconceptLookup.close();
		fout.flush();
		fout.close();
		System.out.println(wikiConceptFile + " file created.");
		// if semantic relatedness is needed, use the code below
		FileWriter foutConceptSr = new FileWriter(outputFilesPath + "corpusConceptsSr.txt");
		VirtGraph virtGraph  = connectToVirtuoso();
		try {
			File fileDirectory = new File(entityFilesPath);
			int i = 0;
			for (File file : fileDirectory.listFiles()) {
				String fileName = file.getName();
				docId = documentIdMap.get(fileName);
				String document = readDocument(entityFilesPath + fileName);
				String [ ] docentities = document.split(",");
				Map<String, Set<Integer>> docEntityLinks = new HashMap<String, Set<Integer>>(docentities.length);
				for (String ent1 : docentities) {
					ent1 = ent1.trim();
					docEntityLinks.put(ent1, dbProcessor.findLinkedEntities(ent1, virtGraph));
				}
				for (String ent1 : docentities) {
					ent1 = ent1.trim();
					for (String ent2 : docentities) {
						ent2 = ent2.trim();
						if (ent1.equals(ent2)) {
							continue;
						}
						if (corpusConceptIdMap.get(ent1) != null && corpusConceptIdMap.get(ent2) != null && dbProcessor.isRelationBetween(ent1, ent2, virtGraph)){
							double sr = (double) Math.round(dbProcessor.calculateSemanticRelatedness(docEntityLinks.get(ent1), docEntityLinks.get(ent2)) * 100000) / 100000;
							String line = docId + " " + corpusConceptIdMap.get(ent1) + " " + corpusConceptIdMap.get(ent2) + " " + sr + "\n";
							foutConceptSr.write(line);
							foutConceptSr.flush();
						} // end of if
					} // end of for
				} // end of for
				System.out.println("File " + (++i) + " is done.");
			} // end of for
		}catch(Exception e) {
			e.printStackTrace();
		}
		foutConceptSr.flush();
		foutConceptSr.close();
		*/
		return corpusConceptIdMap;
	} // end of createTestCorpusFiles
	
	
	// This method is used for sOntoLDA project - Wikipedia dataset
		public Map<String, Integer> createCorpusFiles(String inputFilesPath, String wikiFilesPath, String outputFilesPath, String corpusfile, String vocabFile, String wikiConceptFile) throws IOException {
			System.out.println("Reading Wikipedia pages...");
			File wikiDirectory  = new File(wikiFilesPath);
			InstanceList wikiConcepts = readDirectory(wikiDirectory);
			Alphabet wikiConceptsAlphabet = wikiConcepts.getDataAlphabet();
			System.out.println("Wikipedia initial vocab size: " + wikiConceptsAlphabet.size());
			Map<Integer, Integer> wordsFrequency = new HashMap<Integer, Integer>(wikiConcepts.getDataAlphabet().size());
			int minFrequency = Integer.valueOf(Configuration.getProperty("wordFrequency"));
			int docId = 0;
//			FileWriter fout = new FileWriter(outputFilesPath + "alldocumentsLookup.txt");
			Map<String,Integer> documentIdMap = new HashMap<String,Integer>(wikiConcepts.size());
			// Count the frequency of words in the Wikipedia articles
			for (Instance doc : wikiConcepts) {
				FeatureVector words = (FeatureVector) doc.getData();
				String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1,doc.getName().toString().indexOf(".txt")); // this name excludes the file extension ".txt"
				if (words.numLocations() > 0) {
					documentIdMap.put(docName,docId);
//					fout.write(docId + "\t" + docName + "\n");
//					fout.flush();
					docId++;
					for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
						int wordId = words.indexAtLocation(wordCntr); // id of the word
						int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
						int preFreq = wordsFrequency.get(wordId) != null ? wordsFrequency.get(wordId) : 0;
						wordsFrequency.put(wordId, frequency + preFreq);
					} // end of for
				}
			} // end of for
//			fout.close();
			// Remove less frequent words, i.e words less than minFrequency (e.g 5) times in the corpus
			Iterator<Map.Entry<Integer, Integer>> itr = wordsFrequency.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<Integer, Integer> entry = itr.next();
				if (wordsFrequency.get(entry.getKey()) < minFrequency) {
					itr.remove();
				} // end of if
			} // end of while
			System.out.println("Vocabulary Size after removing less frequent words: " + wordsFrequency.size());
//			System.out.println("alldocumentsLookup file created.");
			Set<Integer> vocabularyIdSet = wordsFrequency.keySet();
			Map<Integer, Integer> oldIdToNewId = new HashMap<Integer, Integer>(vocabularyIdSet.size());
			Map<String, Integer> wordToIdMap = new HashMap<String,Integer>(vocabularyIdSet.size());
			int id = 0;
			FileWriter fout = new FileWriter(outputFilesPath + vocabFile);
			for (int vid : vocabularyIdSet) {
				String word = wikiConceptsAlphabet.lookupObject(vid).toString();
				String line = id + "\t" + word  + "\n";
				oldIdToNewId.put(vid, id);
				wordToIdMap.put(word, id);
				fout.write(line);
				id++;
			}
			fout.flush();
			fout.close();
			System.out.println(vocabFile + " file created.");
			
			
			// This map is used to find the term-frequency (tf) of each word
			Map<Integer,Map<Integer,Integer>> docWordCountMap = new HashMap<Integer,Map<Integer,Integer>>();
			// This map is used to find the document-frequency (idf) of each word
			Map<Integer,Set<Integer>> wordCatCountMap = new HashMap<Integer,Set<Integer>>();
			for (Instance doc : wikiConcepts) {
				FeatureVector words = (FeatureVector) doc.getData();
				String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1,doc.getName().toString().indexOf(".txt")); // this name excludes the file extension ".txt"
				docId = documentIdMap.get(docName);
				if (words.numLocations() > 0) {
					// create tf map
					Map<Integer,Integer> wordCount = new HashMap<Integer,Integer>();
					for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
						int wordId = words.indexAtLocation(wordCntr); // id of the word in the wiki articles alphabet
						int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the article
						if (vocabularyIdSet.contains(wordId)) {
							// set tf map
							wordCount.put(oldIdToNewId.get(wordId), frequency);
						} // end of if
					} // end of for
					docWordCountMap.put(docId, wordCount);
				} // end of if
			} // end of for
			
			// create the categoryid,wordid,wordcount file
			fout = new FileWriter(outputFilesPath + "categoryWordMat.txt");
			WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
			Map<Integer, Set<String>> catToConMap   = wikiaccess.loadCategoryToConceptMapToMemory();
			CategoryNameMapper wikipediaCategories  = wikiaccess.loadWikipediaCategoryNames();
			//						Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
			List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
			Set<Integer> categoryIds = catToConMap.keySet();
			double [][] catwordMat  = new double [categoryIds.size()][vocabularyIdSet.size()]; 
			double [] catwordSumMat = new double [categoryIds.size()];
			Map<Integer,Integer> catNewIdToOldId = new HashMap<Integer,Integer>();

			int catId = 0;
			for (int cid : categoryIds) {
				System.out.println(catId);
				catNewIdToOldId.put(catId,cid);
				Set<String> categoryConceptSet = catToConMap.get(cid);
				for (int oldwid : oldIdToNewId.keySet()) {
					int wid = oldIdToNewId.get(oldwid);
					int wcount = 0;
					for (String conceptname : categoryConceptSet) {
						int conceptid = documentIdMap.get(conceptname) != null ? documentIdMap.get(conceptname) : -1 ;
						//									System.out.print(conceptname + " : " + conceptid);
						if (conceptid == -1) continue;
						wcount += docWordCountMap.get(conceptid).get(wid) != null ? docWordCountMap.get(conceptid).get(wid) : 0;
						//									System.out.println(" count: " + wcount);
					} // end of for
					// set idf map
					if (wcount > 0) {
						if (wordCatCountMap.get(wid) == null) {
							Set<Integer> catCount = new HashSet<Integer>();
							catCount.add(catId);
							wordCatCountMap.put(wid, catCount);
						}else {
							Set<Integer> catCount = wordCatCountMap.get(wid);
							catCount.add(catId);
							wordCatCountMap.put(wid, catCount);
						}
					} // end of if
					catwordMat[catId][wid] = wcount;
					catwordSumMat[catId]  += wcount;
				} // end of for (int wid)
				catId++;
			} // end of for

			// calculate tfIdf
			for (int i = 0; i < catwordMat.length; i++) {
				double sumTfIdf = 0;
				for (int j = 0; j < vocabularyIdSet.size(); j++) {
					double tf  = catwordMat[i][j] / catwordSumMat[i];
					if (tf > 0) {
						double idf   = Math.log((double) catwordMat.length / wordCatCountMap.get(j).size());
						double tfIdf = tf * idf;
						catwordMat[i][j] = tfIdf;
						sumTfIdf += tfIdf;
					}else {
						catwordMat[i][j] = 0;
					}
				} // end of for (j)
				catwordSumMat[i] = sumTfIdf;
			} // end of for (i)

			// normalize the catWordMat (tfIdf)
			for (int i = 0; i < catwordMat.length; i++) {
				for (int j = 0; j < vocabularyIdSet.size(); j++) {
					double tfIdf = Math.round((catwordMat[i][j] / catwordSumMat[i]) * 10000) / 10000.;
					if (tfIdf > 0.0009) {
						String line  = i + " " + j + " " + tfIdf + "\n";
						fout.write(line);
						fout.flush();
					} // end of if
				} // end of for (j)
				System.out.println("cat " + i + " done.");
			} // end of for (i)
			fout.close();
			System.out.println("categoryWordMat file created.");

			// create the categoryid,categoryname file
			fout = new FileWriter(outputFilesPath + "categoryLookup.txt");
			for (int i = 0; i < categoryIds.size(); i++) {
				int oldId = catNewIdToOldId.get(i);
				String cname = wikipediaCategoryNames.get(oldId);
				String line = i + "\t" + cname + "\n";
				fout.write(line);
				fout.flush();
			}
			fout.close();
			System.out.println("categoryLookup file created.");
			
			File inputDirectory = new File(inputFilesPath);
			InstanceList documents = readDirectory(inputDirectory);
			Alphabet documentAlphabet = documents.getDataAlphabet();
			
			wordsFrequency = new HashMap<Integer, Integer>(documents.getDataAlphabet().size());
			System.out.println("Documents initial Vocabulary Size: " + documents.getDataAlphabet().size());
			
			// Count the frequency of words in the corpus
			for (Instance doc : documents) {
				FeatureVector words = (FeatureVector) doc.getData();
				//System.out.println(words.toString());
				for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
					int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
					String word = documentAlphabet.lookupObject(wordId).toString();
					if (wordToIdMap.get(word) == null) continue;
					wordId = wordToIdMap.get(word);  // id of the word in the vocabulary
					int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
					int preFreq = wordsFrequency.get(wordId) != null ? wordsFrequency.get(wordId) : 0;
					wordsFrequency.put(wordId, frequency + preFreq);
				} // end of for
			} // end of for
			// Remove less frequent words, i.e words less than minFrequency (e.g 5) times in the corpus
			itr = wordsFrequency.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<Integer, Integer> entry = itr.next();
				if (wordsFrequency.get(entry.getKey()) < minFrequency) {
//					System.out.println("key:"+entry.getKey() + " f:" +entry.getValue());
					itr.remove();
				} // end of if
			} // end of while
			System.out.println("Vocabulary Size after removing less frequent words: " + wordsFrequency.size());
			vocabularyIdSet = wordsFrequency.keySet();
			docId = 0;
			fout = new FileWriter(outputFilesPath + corpusfile);
			FileWriter fout2 = new FileWriter(outputFilesPath + "documentLookup.txt");
			for (Instance doc : documents) {
				FeatureVector words = (FeatureVector) doc.getData();
//				String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1); // this name includes the file extension ".txt"
				String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1,doc.getName().toString().indexOf(".txt")); // this name excludes the file extension ".txt"
				if (words.numLocations() > 0) {
					documentIdMap.put(docName,docId);
					fout2.write(docId + "\t" + docName + "\n");
					fout2.flush();
					for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
						int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
						String word = documentAlphabet.lookupObject(wordId).toString();
						if (wordToIdMap.get(word) == null) continue;
						wordId = wordToIdMap.get(word);  // id of the word in the vocabulary
						int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
						if (vocabularyIdSet.contains(wordId)) {
							String line = docId + " " + wordId + " " + frequency + "\n";
							fout.write(line);
						} // end of if
					} // end of for
					docId++;
				} // end of if
			} // end of for
			fout.flush();
			fout.close();
			fout2.close();
			System.out.println(" Document Lookup file created.");
			System.out.println(corpusfile + " file created.");
//			Map<String,Integer> corpusConceptIdMap = new HashMap<String,Integer>();
			
			Map<String, Integer> corpusConceptIdMap = new HashMap<String, Integer>();
			return corpusConceptIdMap;
		} // end of createCorpusFiles
		
	/*	
	// This method is used for sOntoLDA project - Wikipedia dataset
	public Map<String, Integer> createCorpusFiles(String inputFilesPath, String wikiFilesPath, String outputFilesPath, String corpusfile, String vocabFile, String wikiConceptFile) throws IOException {
		System.out.println("Reading documents and Wikipedia pages...");
		File inputDirectory = new File(inputFilesPath);
//		File wikiDirectory  = new File(wikiFilesPath);
		InstanceList documents = readDirectory(inputDirectory);
		Alphabet documentAlphabet = documents.getDataAlphabet();
//		InstanceList wikiConcepts = readDirectory(wikiDirectory);
//		Alphabet wikiConceptsAlphabet = wikiConcepts.getDataAlphabet();
		System.out.println("done.");
		
		
		// First we should find the intersection of vocabularies between documents and Wikipedia concepts
//		wikiConceptsAlphabet.dump();
//		Set<Integer> intersectionVocab = getIntersectionVocabularyId(documents, wikiConcepts);
		System.out.println("Documents vocab size: " + documents.getDataAlphabet().size());
//		System.out.println("Wiki concepts vocab size: " + wikiConcepts.getDataAlphabet().size());
//		System.out.println("intersection vocab size: " + intersectionVocab.size());
//		 documents.getDataAlphabet().dump();
		
		Map<Integer, Integer> wordsFrequency = new HashMap<Integer, Integer>(documents.getDataAlphabet().size());
		int minFrequency = Integer.valueOf(Configuration.getProperty("wordFrequency"));
		System.out.println("initial Vocabulary Size: " + documents.getDataAlphabet().size());
		
		// Count the frequency of words in the corpus
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
			//System.out.println(words.toString());
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word
//				if (intersectionVocab.contains(wordId)) {
					int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
					int preFreq = wordsFrequency.get(wordId) != null ? wordsFrequency.get(wordId) : 0;
					wordsFrequency.put(wordId, frequency + preFreq);
//				} // end of if
			} // end of for
		} // end of for
//		System.exit(0);
		// Remove less frequent words, i.e words less than minFrequency (e.g 5) times in the corpus
		Iterator<Map.Entry<Integer, Integer>> itr = wordsFrequency.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			if (wordsFrequency.get(entry.getKey()) < minFrequency) {
//				System.out.println("key:"+entry.getKey() + " f:" +entry.getValue());
				itr.remove();
			} // end of if
		} // end of while
		System.out.println("Vocabulary Size after removing less frequent words: " + wordsFrequency.size());
//		System.exit(0);
//		System.out.println("vocab: " + wordsFrequency);
		
		Set<Integer> vocabularyIdSet = wordsFrequency.keySet();
		Map<Integer, Integer> oldIdToNewId = new HashMap<Integer, Integer>(vocabularyIdSet.size());
		int id = 0;
		FileWriter fout = new FileWriter(outputFilesPath + vocabFile);
//		FileWriter fout = new FileWriter("/Users/Mehdi/vocabulary.txt");
		for (int vid : vocabularyIdSet) {
			String word = documentAlphabet.lookupObject(vid).toString();
			String line = id + "\t" + word  + "\n";
			oldIdToNewId.put(vid, id);
			fout.write(line);
			id++;
		}
		fout.flush();
		fout.close();
		System.out.println(vocabFile + " file created.");
		int docId = 0;
		Map<String,Integer> documentIdMap = new HashMap<String,Integer>(documents.size());
		// This map is used to find the term-frequency (tf) of each word
		Map<Integer,Map<Integer,Integer>> docWordCountMap = new HashMap<Integer,Map<Integer,Integer>>();
		// This map is used to find the document-frequency (idf) of each word
		Map<Integer,Set<Integer>> wordCatCountMap = new HashMap<Integer,Set<Integer>>();
		fout = new FileWriter(outputFilesPath + corpusfile);
		FileWriter fout2 = new FileWriter(outputFilesPath + "documentLookup.txt");
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
//			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1); // this name includes the file extension ".txt"
			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1,doc.getName().toString().indexOf(".txt")); // this name excludes the file extension ".txt"
			if (words.numLocations() > 0) {
				documentIdMap.put(docName,docId);
				fout2.write(docId + "\t" + docName + "\n");
				fout2.flush();
				// create tf map
				Map<Integer,Integer> wordCount = new HashMap<Integer,Integer>();
				for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
					int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
					int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
					if (vocabularyIdSet.contains(wordId)) {
						String line = docId + " " + oldIdToNewId.get(wordId) + " " + frequency + "\n";
						fout.write(line);
						// set tf map
						wordCount.put(oldIdToNewId.get(wordId), frequency);
//						// set idf map
//						if (wordDocCountMap.get(oldIdToNewId.get(wordId)) == null) {
//							Set<Integer> docCount = new HashSet<Integer>();
//							docCount.add(docId);
//							wordDocCountMap.put(oldIdToNewId.get(wordId), docCount);
//						}else {
//							Set<Integer> docCount = wordDocCountMap.get(oldIdToNewId.get(wordId));
//							docCount.add(docId);
//							wordDocCountMap.put(oldIdToNewId.get(wordId), docCount);
//						}
					} // end of if
				} // end of for
				docWordCountMap.put(docId, wordCount);
				docId++;
			} // end of if
		} // end of for
		fout.flush();
		fout.close();
		fout2.close();
		System.out.println(" Document Lookup file created.");
		System.out.println(corpusfile + " file created.");
		Map<String,Integer> corpusConceptIdMap = new HashMap<String,Integer>();
		
		// create the categoryid,wordid,wordcount file
		fout = new FileWriter(outputFilesPath + "categoryWordMat.txt");
		WikipediaAccessLayer wikiaccess =  new WikipediaAccessLayer();
		Map<Integer, Set<String>> catToConMap   = wikiaccess.loadCategoryToConceptMapToMemory();
		CategoryNameMapper wikipediaCategories  = wikiaccess.loadWikipediaCategoryNames();
//		Map<String, Integer> catNamesToInt = wikipediaCategories.getNameToInt();
		List<String> wikipediaCategoryNames = wikipediaCategories.getCatNameList();
		Set<Integer> categoryIds = catToConMap.keySet();
		double [][] catwordMat  = new double [categoryIds.size()][vocabularyIdSet.size()]; 
		double [] catwordSumMat = new double [categoryIds.size()];
		Map<Integer,Integer> catNewIdToOldId = new HashMap<Integer,Integer>();
		
		int catId = 0;
		for (int cid : categoryIds) {
//			String catName = wikipediaCategoryNames.get(cid);
//			if (!catName.equals("Taxation")) continue;
			catNewIdToOldId.put(catId,cid);
			Set<String> categoryConceptSet = catToConMap.get(cid);
			for (int oldwid : oldIdToNewId.keySet()) {
				int wid = oldIdToNewId.get(oldwid);
//				if (wid != 473) continue;
				int wcount = 0;
				for (String conceptname : categoryConceptSet) {
					int conceptid = documentIdMap.get(conceptname) != null ? documentIdMap.get(conceptname) : -1 ;
//					System.out.print(conceptname + " : " + conceptid);
					if (conceptid == -1) continue;
					wcount += docWordCountMap.get(conceptid).get(wid) != null ? docWordCountMap.get(conceptid).get(wid) : 0;
//					System.out.println(" count: " + wcount);
				} // end of for
				// set idf map
				if (wcount > 0) {
					if (wordCatCountMap.get(wid) == null) {
						Set<Integer> catCount = new HashSet<Integer>();
						catCount.add(catId);
						wordCatCountMap.put(wid, catCount);
					}else {
						Set<Integer> catCount = wordCatCountMap.get(wid);
						catCount.add(catId);
						wordCatCountMap.put(wid, catCount);
					}
				} // end of if
				catwordMat[catId][wid] = wcount;
				catwordSumMat[catId]  += wcount;
			} // end of for (int wid)
			catId++;
		} // end of for
		
		// calculate tfIdf
		for (int i = 0; i < catwordMat.length; i++) {
			double sumTfIdf = 0;
			for (int j = 0; j < vocabularyIdSet.size(); j++) {
				double tf  = catwordMat[i][j] / catwordSumMat[i];
				if (tf > 0) {
					double idf   = Math.log((double) catwordMat.length / wordCatCountMap.get(j).size());
					double tfIdf = tf * idf;
					catwordMat[i][j] = tfIdf;
					sumTfIdf += tfIdf;
				}else {
					catwordMat[i][j] = 0;
				}
			} // end of for (j)
			catwordSumMat[i] = sumTfIdf;
		} // end of for (i)
		
		// normalize the catWordMat (tfIdf)
				for (int i = 0; i < catwordMat.length; i++) {
					for (int j = 0; j < vocabularyIdSet.size(); j++) {
						if (catwordMat[i][j] > 0) {
							String line  = i + " " + j + " " + (Math.round((catwordMat[i][j] / catwordSumMat[i]) * 1000) / 1000.) + "\n";
							fout.write(line);
							fout.flush();
						}
					} // end of for (j)
					System.out.println("cat " + i + " done.");
				} // end of for (i)
		fout.close();
		System.out.println("categoryWordMat file created.");
		
		// create the categoryid,categoryname file
		fout = new FileWriter(outputFilesPath + "categoryLookup.txt");
		for (int i = 0; i < categoryIds.size(); i++) {
			int oldId = catNewIdToOldId.get(i);
			String cname = wikipediaCategoryNames.get(oldId);
			String line = i + "\t" + cname + "\n";
			fout.write(line);
			fout.flush();
		}
		fout.close();
		System.out.println("categoryLookup file created.");
		
		/*
		int conceptId = 0;
		FileWriter foutconceptLookup = new FileWriter(outputFilesPath + "corpusConceptsLookup.txt");
		fout = new FileWriter(outputFilesPath + wikiConceptFile);
		System.out.println("size of wiki concepts: " + wikiConcepts.size());
		for (Instance doc : wikiConcepts) {
			FeatureVector words = (FeatureVector) doc.getData();
			String conceptName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1).replace(".txt", "");
//			if (conceptName.equals("Issue_tracking_system") || conceptName.equals("Richard_Shiffrin")) {
//				continue;
//			}
			String line = conceptId + "\t" + conceptName + "\n";
			foutconceptLookup.write(line);
			corpusConceptIdMap.put(conceptName, conceptId);
//			System.out.println("cid: " +conceptId + " " + conceptName);
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word in the Wiki alphabet
//				int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the Wiki page
				String word = wikiConceptsAlphabet.lookupObject(wordId).toString();
				int idInDocAlphabet = documentAlphabet.lookupIndex(word);
				if (vocabularyIdSet.contains(idInDocAlphabet)) {
					line = conceptId + " " + oldIdToNewId.get(idInDocAlphabet) + "\n";
					fout.write(line);
				}
			} // end of for
			conceptId++;
		} // end of for
		foutconceptLookup.flush();
		foutconceptLookup.close();
		fout.flush();
		fout.close();
		System.out.println(wikiConceptFile + " file created.");
		// if semantic relatedness is needed, use the code below
		FileWriter foutConceptSr = new FileWriter(outputFilesPath + "corpusConceptsSr.txt");
		VirtGraph virtGraph  = connectToVirtuoso();
		try {
			File fileDirectory = new File(entityFilesPath);
			int i = 0;
			for (File file : fileDirectory.listFiles()) {
				String fileName = file.getName();
				docId = documentIdMap.get(fileName);
				String document = readDocument(entityFilesPath + fileName);
				String [ ] docentities = document.split(",");
				Map<String, Set<Integer>> docEntityLinks = new HashMap<String, Set<Integer>>(docentities.length);
				for (String ent1 : docentities) {
					ent1 = ent1.trim();
					docEntityLinks.put(ent1, dbProcessor.findLinkedEntities(ent1, virtGraph));
				}
				for (String ent1 : docentities) {
					ent1 = ent1.trim();
					for (String ent2 : docentities) {
						ent2 = ent2.trim();
						if (ent1.equals(ent2)) {
							continue;
						}
						if (corpusConceptIdMap.get(ent1) != null && corpusConceptIdMap.get(ent2) != null && dbProcessor.isRelationBetween(ent1, ent2, virtGraph)){
							double sr = (double) Math.round(dbProcessor.calculateSemanticRelatedness(docEntityLinks.get(ent1), docEntityLinks.get(ent2)) * 100000) / 100000;
							String line = docId + " " + corpusConceptIdMap.get(ent1) + " " + corpusConceptIdMap.get(ent2) + " " + sr + "\n";
							foutConceptSr.write(line);
							foutConceptSr.flush();
						} // end of if
					} // end of for
				} // end of for
				System.out.println("File " + (++i) + " is done.");
			} // end of for
		}catch(Exception e) {
			e.printStackTrace();
		}
		foutConceptSr.flush();
		foutConceptSr.close();
		*/
//		return corpusConceptIdMap;
//	} // end of createCorpus
	
	
	// This method is used for EntLDA project
	public Map<String, Integer> createCorpus(String inputFilesPath, String wikiFilesPath, String outputFilesPath, String entityFilesPath, String corpusfile, String vocabFile, String wikiConceptFile) throws IOException {
		// if semantic relatedness between concepts is needed, use the piece of code below
		DbpediaProcessor dbProcessor = new DbpediaProcessor();
		LocalNameMapperClass localNamesMapperClass = dbProcessor.loadLocalNameListToMemory();
		// List<String> localNames = localNamesMapperClass.getLocalNameList();
//		List<String> localNames = localNamesMapperClass.getLocalNameList();
		Map<String, Integer> namesToInt = localNamesMapperClass.getNameToInt();
		dbProcessor.setNamesToInt(namesToInt);
		System.out.println("Reading documents and Wikipedia pages...");
		File inputDirectory = new File(inputFilesPath);
		File wikiDirectory  = new File(wikiFilesPath);
		InstanceList documents = readDirectory(inputDirectory);
		Alphabet documentAlphabet = documents.getDataAlphabet();
		InstanceList wikiConcepts = readDirectory(wikiDirectory);
		Alphabet wikiConceptsAlphabet = wikiConcepts.getDataAlphabet();
		System.out.println("done.");
		
		
		// First we should find the intersection of vocabularies between documents and Wikipedia concepts
//		wikiConceptsAlphabet.dump();
		Set<Integer> intersectionVocab = getIntersectionVocabularyId(documents, wikiConcepts);
		System.out.println("Documents vocab size: " + documents.getDataAlphabet().size());
		System.out.println("Wiki concepts vocab size: " + wikiConcepts.getDataAlphabet().size());
		System.out.println("intersection vocab size: " + intersectionVocab.size());
//		 documents.getDataAlphabet().dump();
		
		Map<Integer, Integer> wordsFrequency = new HashMap<Integer, Integer>(documents.getDataAlphabet().size());
		int minFrequency = Integer.valueOf(Configuration.getProperty("wordFrequency"));
		System.out.println("initial Vocabulary Size: " + documents.getDataAlphabet().size());
		
		// Count the frequency of words in the corpus
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
			//System.out.println(words.toString());
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word
//				if (intersectionVocab.contains(wordId)) {
					int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
					int preFreq = wordsFrequency.get(wordId) != null ? wordsFrequency.get(wordId) : 0;
					wordsFrequency.put(wordId, frequency + preFreq);
//				} // end of if
			} // end of for
		} // end of for
//		System.exit(0);
		// Remove less frequent words, i.e words less than minFrequency (e.g 5) times in the corpus
		Iterator<Map.Entry<Integer, Integer>> itr = wordsFrequency.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<Integer, Integer> entry = itr.next();
			if (wordsFrequency.get(entry.getKey()) < minFrequency) {
//				System.out.println("key:"+entry.getKey() + " f:" +entry.getValue());
				itr.remove();
			} // end of if
		} // end of while
		System.out.println("Vocabulary Size after removing less frequent words: " + wordsFrequency.size());
		System.exit(0);
//		System.out.println("vocab: " + wordsFrequency);
		
		Set<Integer> vocabularyIdSet = wordsFrequency.keySet();
		Map<Integer, Integer> oldIdToNewId = new HashMap<Integer, Integer>(vocabularyIdSet.size());
		int id = 0;
		FileWriter fout = new FileWriter(outputFilesPath + vocabFile);
//		FileWriter fout = new FileWriter("/Users/Mehdi/vocabulary.txt");
		for (int vid : vocabularyIdSet) {
			String word = documentAlphabet.lookupObject(vid).toString();
			String line = id + "\t" + word  + "\n";
			oldIdToNewId.put(vid, id);
			fout.write(line);
			id++;
		}
		fout.flush();
		fout.close();
		System.out.println(vocabFile + " file created.");
		int docId = 0;
		Map<String,Integer> documentIdMap = new HashMap<String,Integer>(documents.size());
		fout = new FileWriter(outputFilesPath + corpusfile);
		FileWriter fout2 = new FileWriter(outputFilesPath + "documentLookup.txt");
		for (Instance doc : documents) {
			FeatureVector words = (FeatureVector) doc.getData();
			String docName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1);
			documentIdMap.put(docName,docId);
			fout2.write(docId + "\t" + docName + "\n");
			fout2.flush();
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word in the documents alphabet
				int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the document
				if (vocabularyIdSet.contains(wordId)) {
					String line = docId + " " + oldIdToNewId.get(wordId) + " " + frequency + "\n";
					fout.write(line);
				}
			} // end of for
			docId++;
		} // end of for
		fout.flush();
		fout.close();
		fout2.close();
		System.out.println(" Document Lookup file created.");
		System.out.println(corpusfile + " file created.");
		int conceptId = 0;
		FileWriter foutconceptLookup = new FileWriter(outputFilesPath + "corpusConceptsLookup.txt");
		Map<String,Integer> corpusConceptIdMap = new HashMap<String,Integer>();
		fout = new FileWriter(outputFilesPath + wikiConceptFile);
		System.out.println("size of wiki concepts: " + wikiConcepts.size());
		for (Instance doc : wikiConcepts) {
			FeatureVector words = (FeatureVector) doc.getData();
			String conceptName = doc.getName().toString().substring(doc.getName().toString().lastIndexOf('/')+1).replace(".txt", "");
//			if (conceptName.equals("Issue_tracking_system") || conceptName.equals("Richard_Shiffrin")) {
//				continue;
//			}
			String line = conceptId + "\t" + conceptName + "\n";
			foutconceptLookup.write(line);
			corpusConceptIdMap.put(conceptName, conceptId);
//			System.out.println("cid: " +conceptId + " " + conceptName);
			for (int wordCntr = 0;wordCntr < words.numLocations();wordCntr++) {
				int wordId = words.indexAtLocation(wordCntr); // id of the word in the Wiki alphabet
//				int frequency = (int) words.valueAtLocation(wordCntr);  // frequency of the word in the Wiki page
				String word = wikiConceptsAlphabet.lookupObject(wordId).toString();
				int idInDocAlphabet = documentAlphabet.lookupIndex(word);
				if (vocabularyIdSet.contains(idInDocAlphabet)) {
					line = conceptId + " " + oldIdToNewId.get(idInDocAlphabet) + "\n";
					fout.write(line);
				}
			} // end of for
			conceptId++;
		} // end of for
		foutconceptLookup.flush();
		foutconceptLookup.close();
		fout.flush();
		fout.close();
		System.out.println(wikiConceptFile + " file created.");
		// if semantic relatedness is needed, use the code below
		FileWriter foutConceptSr = new FileWriter(outputFilesPath + "corpusConceptsSr.txt");
		VirtGraph virtGraph  = connectToVirtuoso();
		try {
			File fileDirectory = new File(entityFilesPath);
			int i = 0;
			for (File file : fileDirectory.listFiles()) {
				String fileName = file.getName();
				docId = documentIdMap.get(fileName);
				String document = readDocument(entityFilesPath + fileName);
				String [ ] docentities = document.split(",");
				Map<String, Set<Integer>> docEntityLinks = new HashMap<String, Set<Integer>>(docentities.length);
				for (String ent1 : docentities) {
					ent1 = ent1.trim();
					docEntityLinks.put(ent1, dbProcessor.findLinkedEntities(ent1, virtGraph));
				}
				for (String ent1 : docentities) {
					ent1 = ent1.trim();
					for (String ent2 : docentities) {
						ent2 = ent2.trim();
						if (ent1.equals(ent2)) {
							continue;
						}
						if (corpusConceptIdMap.get(ent1) != null && corpusConceptIdMap.get(ent2) != null && dbProcessor.isRelationBetween(ent1, ent2, virtGraph)){
							double sr = (double) Math.round(dbProcessor.calculateSemanticRelatedness(docEntityLinks.get(ent1), docEntityLinks.get(ent2)) * 100000) / 100000;
							String line = docId + " " + corpusConceptIdMap.get(ent1) + " " + corpusConceptIdMap.get(ent2) + " " + sr + "\n";
							foutConceptSr.write(line);
							foutConceptSr.flush();
						} // end of if
					} // end of for
				} // end of for
				System.out.println("File " + (++i) + " is done.");
			} // end of for
		}catch(Exception e) {
			e.printStackTrace();
		}
		foutConceptSr.flush();
		foutConceptSr.close();
		return corpusConceptIdMap;
	} // end of createCorpus
	
	// this method is used for EntLDA project
	public void createRelatedConceptsMap(String input_dir, String output_dir, Map<String,Integer> corpusConceptIdMap) {
//		WikipediaAccessLayer wikiOntology =  new WikipediaAccessLayer();
//		Set<String> initialEntities = wikiOntology.getPopulatedConcepts(4);
		
		
		// get the name of all the concepts in the corpus
		Set<String> initialEntities = new HashSet<String>();
		File sourceDir = new File(input_dir);
		String [] fileNames = sourceDir.list();
		for (String name : fileNames) {
			initialEntities.add(name.replace(".txt", ""));
		} // end of for
		
		
		// from a relating concept to a set of concepts in the corpus
		Map<String, Set<String>> rConceptsMap = new HashMap<String, Set<String>> (100000);
//		Map<String,Integer> corpusConceptIdMap = new HashMap<String,Integer>();
		File inputDirectory = new File(input_dir);
		int i = 0;
		int id = 1;
		try {
//			FileWriter fout = new FileWriter(output_dir + "corpusConceptsLookup.txt");
			for (File file : inputDirectory.listFiles()) {
				String fileName = file.getName();
				String document = readDocument(input_dir + fileName);
				String [ ] entities = document.split(",");
				String corpusConcept = fileName.replace(".txt", "");
//				corpusConceptIdMap.put(corpusConcept, id);
//				String line = id + ":" + corpusConcept + "\n";
//				fout.write(line);
//				id++;
				//			System.out.println("# of relating entities: " + entities.length);
				if (corpusConceptIdMap.get(corpusConcept) != null) {
					for (String entity : entities) {
						entity = entity.trim();
						if (entity.endsWith("_(number)") || entity.endsWith("_(disambiguation)")) {
							continue;
						}
						if (initialEntities.contains(entity)) {
							//					System.out.println("==> entity: " + entity);
							Set<String> relatedConcepts = rConceptsMap.get(entity);
							if (relatedConcepts == null) {
								relatedConcepts = new HashSet<String> ();
								relatedConcepts.add(corpusConcept);
							}else {
								relatedConcepts.add(corpusConcept);
							} // end of if
							rConceptsMap.put(entity, relatedConcepts);
						} // end of if
					} // end of for
					
					// create a relation from concept to itself
//					Set<String> relatedConcepts = new HashSet<String>();
//					relatedConcepts.add(corpusConcept);
//					rConceptsMap.put(corpusConcept, relatedConcepts);
				} // end of if
				System.out.println("File: " + (++i) + ": " + fileName +  " done.");
			} // end of for (File file)
//			fout.flush();
//			fout.close();
			
			
			
			
//			File dDirectory = new File("/home/mehdi/otm/wikipediaPages/");
//			String [] fileNames = dDirectory.list();
//			Set<String> nm = rConceptsMap.keySet();
//			for (String n : nm) {
//				boolean fl = false;
//				for(String s : fileNames) {
//					s = s.replace(".txt", "");
//					if(s.equals(n)){
//						fl = true;
//						break;
//					}
//				}
//				if(!fl) {
//					System.out.println(n);
//				}
//				
//			}
			
			

			System.out.println("The size of Related Map: " + rConceptsMap.size());
//			String fileName = "/home/mehdi/otm/relatedConceptsMap.ser";
//			System.out.println("Serializing related concepts...");
//			FileOutputStream outputFile = new FileOutputStream(fileName);
//			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
//			ObjectOutputStream out = new ObjectOutputStream(bfout);
//			out.writeObject(rConceptsMap);
//			out.close();
//			System.out.println("related concepts Serialized successfully.");
			FileWriter fout = new FileWriter(output_dir + "relatedConceptLookup.txt");
			Map<String,Integer> rConceptIdMap = new HashMap<String,Integer> ();
			
			// if related concepts are different than corpus concepts 
//			id = 1;
//			for (String e : rConceptsMap.keySet()) {
//				rConceptIdMap.put(e, id);
//				String line = id + "\t" + e + "\n";
//				fout.write(line);
//				id++;
//			} // end of for
			
			// if related concepts are the same as corpus concepts
			for (String e : rConceptsMap.keySet()) {
				if (corpusConceptIdMap.get(e) == null){
					continue;
				}
				id = corpusConceptIdMap.get(e);
				rConceptIdMap.put(e, id);
				String line = id + "\t" + e + "\n";
				fout.write(line);
			} // end of for
			
			
			
			fout.flush();
			fout.close();
			id = 1;

			fout = new FileWriter(output_dir + "c0C1Corpus.txt");
			for (Map.Entry<String, Set<String>> entry : rConceptsMap.entrySet()) {
				if (rConceptIdMap.get(entry.getKey()) == null) {
					continue;
				}
				int cid = rConceptIdMap.get(entry.getKey());
				Set<String> eset = entry.getValue();
				for (String c : eset) {
					if (corpusConceptIdMap.get(c) != null) {
						String line = cid + " " + corpusConceptIdMap.get(c) + "\n";
						fout.write(line);
					} // end of if
				} // end of for
				
			} // end of for
			fout.flush();
			fout.close();
			//			fout = new FileWriter(output_dir + "corpusConceptsLookup.txt");
			//			Object [ ] ids = corpusConceptIdMap.values().toArray();
			//			Arrays.sort(ids);
			//			for (Object cid : ids) {
			//				for (Map.Entry<String,Integer> entry : corpusConceptIdMap.entrySet()) {
			//					if (entry.getValue() == (Integer) cid) {
			//						String line = entry.getValue() + ":" + entry.getKey() + "\n";
			//						fout.write(line);
			//					} // end of if
			//				} // end of for
			//			} // end of for
			//			fout.flush();
			//			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createRelatedConceptsMap
	
	
	public void createCollectionOfConcepts(String output_dir) {
		WikipediaAccessLayer wikiOntology =  new WikipediaAccessLayer();
		int upToLevel = 4;
		Set<String> initialEntities = wikiOntology.getPopulatedConcepts(upToLevel);
		System.out.println("Size of Concepts: " + initialEntities.size());  // 9552
		Set<String> finalEntities = new HashSet<String>(initialEntities.size());
		for (String e : initialEntities) {
			if (e.indexOf('?') != -1 || e.indexOf('.') != -1 || e.indexOf('/') != -1 || e.indexOf('%') != -1 || e.indexOf('\'') != -1 || e.indexOf('^') != -1 || e.indexOf(':') != -1 || e.indexOf('\"') != -1 || e.indexOf('-') != -1 || e.startsWith("_") || e.startsWith(".")) {
				continue;
			}else {
				finalEntities.add(e);
			} // end of if
		} // end of for
		System.out.println("Size of final Concepts: " + finalEntities.size());  // 8330
		try {
			FileWriter fout = new FileWriter(output_dir + "allConcepts.txt");
			String line = finalEntities.toString().replace("[", "").replace("]", "").replace(" ", "");
			fout.write(line);
			fout.flush();
			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createCollectionOfConcepts
	
	
	public void deleteIrrelevantFiles(String sourceDir, String destDir) {
		File sDirectory = new File(sourceDir);
		File dDirectory = new File(destDir);
		String [] fileNames = dDirectory.list();
		boolean exist = false;
		for (File file : sDirectory.listFiles()) {
			String fn = file.getName();
			for (String fname : fileNames) {
				if (fname.equals(fn)) {
					exist = true;
					break;
				} // end of if
			} // end of for
			if (!exist) {
				System.out.println(fn + " does not exist.");
				file.delete();
			} // end of if
			exist = false;
		} // end of for
	} // end of deleteIrrelevantFiles
	
	public List<String> getDocumentContent(String filename) {
		List<String> content = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content.add(line);
			} // end of while
//			System.out.println(content);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	} // end of getDocumentContent

	
	
	public String readDocument(String fileName) {
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("\"" + fileName + "\" does NOT exits. Please enter a valid file name.");
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return content;
	} // end of readDocument
	
	public void splitDocumentPerLine(String sourceFile, String outputDir) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			String line = "";
			int fileName = 1;
			while ((line = br.readLine()) != null) {
				FileWriter fout = new FileWriter(outputDir + fileName + ".txt");
				line = line.replace(":", "").replace("-", " ");
				fout.write(line + "\n");
				fout.flush();
				fout.close();
				System.out.println(fileName);
				fileName++;
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("\"" + sourceFile + "\" does NOT exits. Please enter a valid file name.");
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
	} // end of splitDocumentPerLine

	public static void main(String[] args) {
		ImportData importer = new ImportData();
//		try {
//			importer.createCorpus("/Users/Mehdi/Downloads/test/","/Users/Mehdi/corpus/wikipediaPages/","/Users/Mehdi/corpus/", "corpus.txt", "vocab.txt", "wiki.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
//		importer.splitDocumentPerLine("/Users/Mehdi/Downloads/DATASET/20000-30000.txt", "/Users/Mehdi/Downloads/inputfiles/");
		
		
		InstanceList instances = importer.readDirectory(new File("/Users/Mehdi/Downloads/nn/"));
		Instance d = instances.get(0);
		Object token = d.getData();
		System.out.println(token.toString());
//		FeatureSequence tokens =  (FeatureSequence) token;
//		instances.save(new File("/Users/Mehdi/Downloads/sample-data/web/en/r.txt"));
//		InstanceList instances = InstanceList.load(new File("/Users/Mehdi/Downloads/sample-data/web/en/r.txt"));
//		Alphabet al = instances.getAlphabet();
//		System.out.println(al.size());
//		System.out.println("===> " + al.lookupObject(792));
//		System.out.println("===> " + al.lookupIndex("computer"));
//		Instance ins = instances.get(3);
//		System.out.println("name:" + ins.getName().toString().substring(ins.getName().toString().lastIndexOf('/')+1) + "  ");
//		System.out.println(ins.getName()+ " " + ins.getAlphabet().lookupIndex("mother"));
//		FeatureSequence fs = (FeatureSequence) ins.getData();
//		FeatureVector fv = (FeatureVector) ins.getData();
//		System.out.println("fv: " + fv);
//		System.out.println("NumofLoc: " + fv.numLocations());
//		System.out.println("-------------");
//		for (int i=0;i<fv.numLocations();i++) {
//			int id = fv.indexAtLocation(i);
//			System.out.print("id: " +id + " ");  // id of the word 
//			System.out.print(fv.getAlphabet().lookupObject(id) + "  ");  // the word 
//			System.out.println("val:" + fv.valueAtLocation(i));  // frequency of the word in the doc
////			ins.setProperty((String) fv.getAlphabet().lookupObject(id), id + "," + i);
//			ins.setProperty(id + "",id + "," + fv.valueAtLocation(i));
////			ins.setProperty("curId", id );
//		}
//		for(Instance ns : instances) {
//			FeatureVector fv2 = (FeatureVector) ns.getData();
//			System.out.println("data: " + fv2);
////			System.out.println(" " + fv2.contains("battle") + " ====> " + ns.getProperty("battle") );
//			if (fv2.contains("5")) {
////				System.out.println(" val: " + fv2.valueAtLocation((fv2.location(ns.getAlphabet().lookupIndex("battle")))));
////				System.out.println(" val: " + fv2.valueAtLocation((fv2.location("battle"))));
//				System.out.println("dddddd:" + ns.getName());
//			}
////			System.out.println("mm: " + fv.getAlphabet().lookupObject());
//		}
//		System.out.println("size:" + al.size());
//		Object [] ob = al.toArray();  //array of vocabulary
//		System.out.println("ob:"+ob.length);
//		for (Object o :  ob) {
//			//System.out.println("aarr:" + o.toString() );    // word in vocabulary
//		}
////		System.out.println(al.toString());
//		int [] ii = al.lookupIndices(ob, true);  // array of vocabulary ids
//		for (int j : ii) {
//		System.out.println(j + " " + ob[j]);
//		}
	
//	
//		for (int i=0;i<fv.numLocations();i++) {
//			int id = fv.indexAtLocation(i);
//			System.out.println("==>entry: " +fv.getAlphabet().lookupObject(id) + " prop val:" + ins.getProperty(id + ""));  
//		}
		
//		List<String> mm = new ArrayList<String>();
//		mm.add("mehdi");
//		mm.add("allahyari");
//		mm.add("test");
//		mm.add("java");
//		mm.add("lda");
//		Alphabet alp = new Alphabet(mm.toArray());
//		alp.dump();
//		System.out.println(alp.lookupIndex("test"));
//		System.out.println(instances.getDataAlphabet().size());
//		System.out.println(instances.getAlphabet().size());
//		al.dump();
	
	}
}
