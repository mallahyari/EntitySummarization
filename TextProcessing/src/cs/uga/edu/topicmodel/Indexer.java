package cs.uga.edu.topicmodel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
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
	private int hitsPerPage = 10000;
	private int resultsFound = 0;
	
	public Indexer() {
		/* when index is not huge */
		index = new RAMDirectory();
//		try {
//			index = FSDirectory.open(new File(indexDir));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
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
	
	public void indexDocumentCollection(String dirPath) throws IOException {
		ImportData im = new ImportData();
		File docsDirectory = new File(dirPath);
		for (File file : docsDirectory.listFiles()) {
			String fileName = file.getName();
			String document = im.readDocument(dirPath + fileName);
			Document doc = new Document();
			doc.add(new StringField("fileName", fileName, Field.Store.YES));
			doc.add(new TextField("content", document, Field.Store.YES));
			indexWriter.addDocument(doc);
		} // end of for
		System.out.println(indexWriter.numDocs() + " records indexed.");
		closeIndexWriter();
	} // end of indexWikipediaCategories
	
	public int retrieveDocumentsCountByKeyword(Directory index, StandardAnalyzer analyzer, String searchString) {
//		Query query;
		PhraseQuery query = new PhraseQuery();
		String [] terms = searchString.split(" ");
		for (String term : terms) {
			query.add(new Term("content", term));
		}
//		System.out.println("keyword: "+ searchString);
		int numOfHits = 0;
		try {
//			query = new QueryParser(Version.LUCENE_44, "content", analyzer).parse(searchString);

			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			searcher.search(query, collector);
			ScoreDoc [] hits = collector.topDocs().scoreDocs;
			numOfHits = hits.length;
//			for(int cnt = 0;cnt < hits.length;cnt++) {
//				int docId = hits[cnt].doc;
//				Document d = searcher.doc(docId);
//				String filename = d.get("fileName");
//				System.out.println("file: " + filename);
//			} // end of for
//			System.out.println("Number of hits: " + hits.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return numOfHits;
	} // end of retrieveDocumentsCountByKeyword
	
	public int retrieveDocumentsCountByMultipleKeywords(Directory index, StandardAnalyzer analyzer, String keyword1, String keyword2) {
		int numOfHits = 0;
		try {
			BooleanQuery bq   = new BooleanQuery();
			PhraseQuery query = new PhraseQuery();
			String [] terms = keyword2.split(" ");
			for (String term : terms) {
				query.add(new Term("content", term));
			}
			bq.add(query, BooleanClause.Occur.MUST);
			bq.add(new TermQuery(new Term("content",keyword1)), BooleanClause.Occur.MUST);
//			System.out.println("keywords: "+ keyword1 + " " + keyword2);

			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
			searcher.search(bq, collector);
			ScoreDoc [] hits = collector.topDocs().scoreDocs;
			numOfHits = hits.length;
//			for(int cnt = 0;cnt < hits.length;cnt++) {
//				int docId = hits[cnt].doc;
//				Document d = searcher.doc(docId);
//				String filename = d.get("fileName");
//				System.out.println("file: " + filename);
//			} // end of for
//			System.out.println("Number of hits: " + hits.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return numOfHits;
	} // end of retrieveDocumentsCountByMultipleKeywords


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
