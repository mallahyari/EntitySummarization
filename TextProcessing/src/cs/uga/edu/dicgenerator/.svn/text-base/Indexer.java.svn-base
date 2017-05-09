package cs.uga.edu.dicgenerator;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * 
 */

/**
 * @author Mehdi
 *
 */
public class Indexer {

	private Directory index     = null;
	private StandardAnalyzer analyzer = null;
	private IndexWriterConfig indexConfig = null;
	private IndexWriter indexWriter   = null;
	private String indexDir = "/home/mehdi/luceneIndex";
	
	public Indexer() {
		/* when index is not huge */
//		index = new RAMDirectory();
		try {
			index = FSDirectory.open(new File(indexDir));
		} catch (IOException e) {
			e.printStackTrace();
		}
		analyzer = new StandardAnalyzer(Version.LUCENE_44);
		indexConfig = new IndexWriterConfig(Version.LUCENE_44, analyzer);
	}
	
	
	public void createIndexWriter() throws IOException {
		if (indexWriter == null) {
			indexWriter = new IndexWriter(index, indexConfig);
		}
	} // end of createIndexWriter
	
	
	public void closeIndexWriter() throws IOException {
		if (indexWriter != null) {
			indexWriter.close();
		}
	} // end of closeIndexWriter
	
	public void indexWikipediaCategories(Map<String, Integer> catNamesToInt) throws IOException {
		int counter = 1;
		for (Map.Entry<String, Integer> entry : catNamesToInt.entrySet()) {	
			String catNameWithUnderScore = entry.getKey();
			String catName = catNameWithUnderScore.replace('_', ' ');
			catName = catName.replace("-", "");
//			String catName = entry.getKey();
			int catId      = entry.getValue();
			Document doc = new Document();
			IntField catIdField = new IntField("catId", catId, Field.Store.YES);
			doc.add(catIdField);
			doc.add(new TextField("catName", catName, Field.Store.YES));
//			doc.add(new StringField("catName", catName, Field.Store.YES));
			indexWriter.addDocument(doc);
			if (counter % 100000 == 0) {
				System.out.println(counter + " records indexed.");
			}
			counter++;
		} // end of for
		System.out.println(counter + " records indexed.");
	} // end of indexWikipediaCategories


	/**
	 * @return the index
	 */
	public Directory getIndex() {
		return index;
	}


	/**
	 * @return the analyzer
	 */
	public StandardAnalyzer getAnalyzer() {
		return analyzer;
	}

	
}
