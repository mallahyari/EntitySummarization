/**
 * 
 */
package cs.uga.edu.rdflda;

import static cs.uga.edu.dicgenerator.VirtuosoAccess.GRAPH;
import static cs.uga.edu.dicgenerator.VirtuosoAccess.connectToVirtuoso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;


/**
 * @author mehdi
 *
 */
public class RdfLda {
	
	
	private VirtGraph virtGraph;
	private final String uriPrefix = "http://dbpedia.org/resource/";
	private final String catUriPrefix = "http://dbpedia.org/resource/Category:";
	private final String typeUriPrefix = "http://dbpedia.org/ontology/";
	public final int numOfRelations = 20;
	public final int numOfentitiesPerType = 50;
	private Map<String,Set<String>> dataset = null;
	private String DIR_PATH = "/home/mehdi/rdfproject/";

	public RdfLda() {
		dataset  = new HashMap<String,Set<String>>();
	}
	
	public void createDataSet() {
		// get all the DBpedia types, 683 types
		List<String> types = getDbpediaTypes();
		int tCtr = 0;
		for (String t : types) {
			// for each type get a sample of entities from DBpedia
			List<String> entities = getSampleEntitiesForSpecificType(t);
			System.out.println(t);

			if (entities.size() > 0) {
				int ctr = 0;
				// check to see each entity has at least 40 related entities
				for (String entity : entities) {
					if (hasEnoughRelatedEntities(entity) > numOfRelations) {
						addEntityToDataSet(entity,t);
						ctr++;
					}
					if (ctr == numOfentitiesPerType) {
						break;
					}
				} // end of for
			}else {
				System.out.println("type \"" + t + "\" does not have any entities in DBpedia.");
				tCtr++;
			}
		} // end of for
		System.out.println(tCtr + " of types do not have entities.");
		System.out.println("dataset size: " + dataset.size());
		String fileName = DIR_PATH + "datasetmap.ser";
		try {
			System.out.println("Serializing the dataset...");
			FileOutputStream outputFile = new FileOutputStream(fileName);
			BufferedOutputStream bfout = new BufferedOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(bfout);
			out.writeObject(dataset);
			out.close();
			System.out.println("dataset Serialized successfully.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	} // end of createDataSet
	
	@SuppressWarnings("unchecked")
	public void loadDatasetToMemory() {
		String fileName = DIR_PATH + "datasetmap.ser";
		System.out.println("Loading dataset into Memory...");
		try {
			FileInputStream inputFile = new FileInputStream(fileName);
			BufferedInputStream bfin = new BufferedInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(bfin);
			dataset =  (Map<String, Set<String>>) in.readObject();
			in.close();
			System.out.println("Dataset Successfully Loaded into Memory.\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	} // end of loadDatasetToMemory
	
	
	public List<String> getDbpediaTypes() {
		virtGraph = connectToVirtuoso();
		int uriPrefixLength = typeUriPrefix.length();
		List<String> types = new ArrayList<String>();
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class>.");
		queryString.append("}");
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String type = result.get("s").toString().substring(uriPrefixLength);
				types.add(type);
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error: " + e.getMessage() + "   " + queryString.toString());
		}
		return types;
	} // end of getDbpediaTypes
	
	public List<String> getSampleEntitiesForSpecificType(String type) {
		virtGraph = connectToVirtuoso();
		String typeUri = typeUriPrefix + type;
		List<String> entities = new ArrayList<String>();
		StringBuffer queryString = new StringBuffer(400);
		queryString.append("SELECT ?s FROM <" + GRAPH + "> WHERE { ");
		queryString.append("?s a <" + typeUri + "> .");
		queryString.append("} LIMIT 2000");
		try{
			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
			// System.out.println("QUERY: " + queryString.toString());
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
			ResultSet results = vqe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String entity = result.get("s").toString().substring(uriPrefix.length());
				if (entity.indexOf("%") != -1 || entity.matches(".*\\d.*")) {
					continue;
				}
				entities.add(entity);
			} // end of while
			vqe.close();
		}catch(Exception e) {
			System.out.println("Error: " + e.getMessage() + "   " + queryString.toString());
		}
		return entities;
	} // end of getSampleEntities


	public int hasEnoughRelatedEntities(String entity) {
//		virtGraph = connectToVirtuoso();
		int numOfEdges = 0;
		QueryEngineHTTP qe = null;
//		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			ResultSet results = null;
			queryString.append("SELECT (COUNT(?o)) AS ?total FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + entity + "> <http://dbpedia.org/ontology/wikiPageWikiLink> ?o . ");
			queryString.append("FILTER (!regex(?o,\"category\",\"i\"))");
			queryString.append("}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				numOfEdges = result.getLiteral("total").getInt();
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		
//		StringBuffer queryString = new StringBuffer(400);
//		queryString.append("SELECT COUNT(?o) AS ?total FROM <" + GRAPH + "> WHERE { ");
//		queryString.append("<" + uriPrefix + entity + "> <http://dbpedia.org/ontology/wikiPageWikiLink> ?o . ");
//		queryString.append("FILTER (!regex(?o,\"category\",\"i\"))");
//		queryString.append("}");
//		int n = 0;
//		try{
//			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
//			// System.out.println("QUERY: " + queryString.toString());
//			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
//			ResultSet results = vqe.execSelect();
//			while (results.hasNext()) {
//				QuerySolution result = results.nextSolution();
//				n = Integer.parseInt(result.get("total").toString());
//			} // end of while
//			vqe.close();
//		}catch(Exception e) {
//			System.out.println("Error: " + e.getMessage() + "   " + queryString.toString());
//		}
		
		return numOfEdges;
	} // end of hasEnoughRelatedEntities
	
	public Set<String> findRelatedEntities(String entity) {
//		virtGraph = connectToVirtuoso();
//		System.out.println("Successfully Connected to Virtuoso!\n");
		Set<String> relatedEntities = new HashSet<String>();
		QueryEngineHTTP qe = null;
//		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String endpoint = "http://localhost:8890/sparql";
			ResultSet results = null;
			StringBuffer queryString = new StringBuffer(400);
			queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + entity + "> <http://dbpedia.org/ontology/wikiPageWikiLink> ?o . ");
			queryString.append("FILTER (!regex(?o,\"category\",\"i\"))");
			queryString.append("}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String e = result.getResource("o").toString().substring(uriPrefix.length());
//				String e = result.get("o").toString().substring(uriPrefix.length());
				relatedEntities.add(e);
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		
		
		
//		VirtuosoQueryExecution vqe = null;
//		try{
//			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
//			// System.out.println("QUERY: " + queryString.toString());
//			vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
//			ResultSet results = vqe.execSelect();
//			while (results.hasNext()) {
//				QuerySolution result = results.nextSolution();
//				String e = result.get("o").toString().substring(uriPrefix.length());
//				relatedEntities.add(e);
//			} // end of while
//		}catch(Exception e) {
//			System.out.println("Error: " + e.getMessage() + "   " + queryString.toString());
//		}finally {
//			vqe.close();
//		}
		
		return relatedEntities;
	} // end of findRelatedEntities


	public void addEntityToDataSet(String entity, String type) {
		Set<String> items = dataset.get(type);
		if (items == null) {
			items = new HashSet<String>();
			items.add(entity);
			dataset.put(type, items);
		}else {
			items.add(entity);
			dataset.put(type, items);
		} // end of if
	} // end of addEntityToDataSet
	
	public void createCorpus(String outputDir) {
		int docId = 0;
		int typeId = 0;
		Set<String> duplicateEntities = new HashSet<String>();
		Set<String> duplicateTypes    = new HashSet<String>();
		Map<String,Integer> typeToIdMap = new HashMap<String,Integer>();
		try {
			FileWriter docLookupfile = new FileWriter(outputDir + "documentLookup.txt");
			FileWriter vocabfile = new FileWriter(outputDir + "vocabLookup.txt");
			for (Map.Entry<String,Set<String>> entry : dataset.entrySet()) {
				Set<String> entities = entry.getValue();
				for (String entity : entities) {
					if (!duplicateEntities.contains(entity)) {
						docLookupfile.write(docId + "\t" + entity + "\n");
//						docfile.flush();
						duplicateEntities.add(entity);
						docId++;
						System.out.println("docId = " + docId);
					} // end of if
					FileWriter dfile = new FileWriter("/home/mehdi/rdfproject/docfiles/" + entity.replace("/", "_") + ".txt");
					List<String> doc = new ArrayList<String>(10000);
					Set<String> relatedEntities = findRelatedEntities(entity);
					for (String e : relatedEntities) {
						Set<String> etypes = findEntityTypes(e);
						for (String et : etypes) {
							et = et.toLowerCase();
							if (!duplicateTypes.contains(et)) {
								vocabfile.write(typeId + "\t" + et + "\n");
//								vocabfile.flush();
								duplicateTypes.add(et);
								typeToIdMap.put(et, typeId);
								typeId++;
							}
							doc.add(et);
						} // end of for
					} // end of for
					String doctext = doc.toString().replace("[", "").replace("]", "").replace(" ", "");
					dfile.write(doctext + "\n");
					dfile.flush();
					dfile.close();
				} // end of for (String entity
			} // end of for
			docLookupfile.close();
			vocabfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createCorpus

	
	public void createLabeledCorpus(String outputDir) {
		int docId = 0;
		int typeId = 0;
		Set<String> duplicateEntities = new HashSet<String>();
		Set<String> duplicateTypes    = new HashSet<String>();
		Map<String,Integer> typeToIdMap = new HashMap<String,Integer>();
		try {
			FileWriter docfile = new FileWriter(outputDir + "documentLookup.txt");
			FileWriter corpusfile = new FileWriter(outputDir + "corpus.txt");
			FileWriter vocabfile = new FileWriter(outputDir + "vocabLookup.txt");
			for (Map.Entry<String,Set<String>> entry : dataset.entrySet()) {
				Set<String> entities = entry.getValue();
				for (String entity : entities) {
					if (!duplicateEntities.contains(entity)) {
						docfile.write(docId + "\t" + entity + "\n");
						//						docfile.flush();
						duplicateEntities.add(entity);
						System.out.println("docId = " + docId);
//						List<Integer> doc = new ArrayList<Integer>(10000);
						List<String> doc = new ArrayList<String>(10000);
						
						// find the entity types
						Set<String> docLabels = findEntityTypes(entity);
						
						// find related entities to entity
						Set<String> relatedEntities = findRelatedEntities(entity);
						for (String e : relatedEntities) {
							Set<String> etypes = findEntityTypes(e);
							for (String et : etypes) {
								et = et.toLowerCase();
								if (!duplicateTypes.contains(et)) {
									vocabfile.write(typeId + "\t" + et + "\n");
									//								vocabfile.flush();
									duplicateTypes.add(et);
									typeToIdMap.put(et, typeId);
									typeId++;
								}
								doc.add(et);
//								int wordId = typeToIdMap.get(et);
//								doc.add(wordId);
							} // end of for
						} // end of for
						String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
						String labels  = docLabels.toString().replace("[", "").replace("]", "").replace(",", "");
						corpusfile.write(docId + "\t" + labels + "\t" + doctext + "\n");
					} // end of for (String entity
					docId++;
				} // end of if
			} // end of for
			docfile.close();
			corpusfile.close();
			vocabfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createLabeledCorpus


	public void createRandomLabeledCorpus(String outputDir) {
		int docId = 0;
		int wordId = 0;
		Set<String> duplicateEntities = new HashSet<String>();
		Set<String> duplicateWords    = new HashSet<String>();
		Map<String,Integer> wordToIdMap = new HashMap<String,Integer>();
		try {
			FileWriter docfile = new FileWriter(outputDir + "documentLookup.txt");
			FileWriter corpusfile = new FileWriter(outputDir + "corpus.txt");
			FileWriter vocabfile = new FileWriter(outputDir + "vocabLookup.txt");
			
			// randomly sample 10000 entities from Dbpedia
			Set<String> entities = getSampleEntities(10000);

			for (String entity : entities) {
				if (!duplicateEntities.contains(entity)) {
					List<String> doc = new ArrayList<String>(10000);

					// find the entity types and consider them as labels for the document
					Set<String> docLabels = findEntityTypes(entity);
					
					if (docLabels.size() == 0)
						continue;
					
					docfile.write(docId + "\t" + entity + "\n");
					duplicateEntities.add(entity);
					System.out.println("docId = " + docId);
					
					// get related triples and construct bag of words
					List<String> bow = constructBowFromProperties(entity);

					// find related entities to entity
					Set<String> relatedEntities = findRelatedEntities(entity);
					
					// add entity types to the bag of words of the document
					for (String e : relatedEntities) {
						Set<String> etypes = findEntityTypes(e);
						for (String et : etypes) {
							et = et.toLowerCase();
							if (!duplicateWords.contains(et)) {
								vocabfile.write(wordId + "\t" + et + "\n");
								duplicateWords.add(et);
								wordToIdMap.put(et, wordId);
								wordId++;
							}
							doc.add(et);
						} // end of for
					} // end of for
					// also add the bag of words from the related triples
					for (String w : bow) {
						if (!duplicateWords.contains(w)) {
							vocabfile.write(wordId + "\t" + w + "\n");
							duplicateWords.add(w);
							wordToIdMap.put(w, wordId);
							wordId++;
						}
						doc.add(w);
					} // end of for
					String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
					String labels  = docLabels.toString().replace("[", "").replace("]", "").replace(",", "");
					corpusfile.write(docId + "\t" + labels + "\t" + doctext + "\n");
				} // end of for (String entity
				docId++;
			} // end of if
			docfile.close();
			corpusfile.close();
			vocabfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createRandomLabeledCorpus
	
	public Set<String> readStopWordsSet() {
		String filename = "/home/mehdi/stopwords.txt";
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				content += line;
			} // end of while
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Set<String> stopWords = new HashSet<String>(Arrays.asList(content.split(",")));
		return stopWords;
	} // end of readStopWordsSet
	
	public List<String> constructBowFromProperties(String entity) {
		QueryEngineHTTP qe = null;
		List<String> words = new ArrayList<String>(10000);
		Set<String> stopWords = readStopWordsSet();
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			ResultSet results = null;
			queryString.append("SELECT ?p ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("{ <" + uriPrefix + entity + "> ?p ?o . ");
			queryString.append("filter (contains (str(?p),\"http://dbpedia.org/\") && !contains (str(?p),\"sameAs\") ");
			queryString.append("&& !contains (str(?p),\"xmlns.com\") && !contains (str(?p),\"/abstract\") ");
			queryString.append("&& !contains (str(?p),\"wikiPage\") && !contains (str(?p),\"/image\") && !contains (str(?p),\"/thumbnail\")) ");
			queryString.append("} UNION {");
			queryString.append("<" + uriPrefix + entity + "> <http://purl.org/dc/terms/subject> ?o . }");
			queryString.append("}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			Set<String> terms = new HashSet<String>();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String term = null; 
				Resource r = result.getResource("p");
				if (r != null) {
					term = r.toString().substring(typeUriPrefix.length()).toLowerCase();
					terms.add(term);
					 RDFNode n = result.get("o");
					if (n.isResource()) {
						term = n.toString();
						String [] tokens = null;
						if (term.indexOf(uriPrefix) != -1) {
							term = term.substring(uriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "");
							tokens = term.split("_");
							for (String w : tokens) {
								if (!stopWords.contains(w) && w.indexOf("%") == -1 && !w.matches(".*\\d.*") && w.length() > 2 ) {
									words.add(w);
								}
							}
						}
					}
				}else {
					term = result.getResource("o").toString();
					term = term.substring(catUriPrefix.length()).toLowerCase().replace(",", "").replace("'s", "").replace("(", "").replace(")","").replace(".", "");
					String [] tokens = term.split("_");
					for (String w : tokens) {
						if (!stopWords.contains(w) && w.indexOf("%") == -1 && !w.matches(".*\\d.*") && w.length() > 2 ) {
							words.add(w);
						}
					}
				} // end of if
			} // end of while
			for (String t : terms) {
				words.add(t);
			}
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		return words;
	}

	public Set<String> getSampleEntities(int nEntities) {
		QueryEngineHTTP qe = null;
		Set<String> entities = new HashSet<String>();
//		System.out.println("Successfully Connected to Virtuoso!\n");
		try {
			String endpoint = "http://localhost:8890/sparql";
			StringBuffer queryString = new StringBuffer();
			ResultSet results = null;
			queryString.append("SELECT DISTINCT ?s FROM <" + GRAPH + "> WHERE { ");
			queryString.append("?s a ?o . ");
			queryString.append("?s <http://www.w3.org/2000/01/rdf-schema#label> ?l . ");
			queryString.append("?s <http://dbpedia.org/property/title> ?ti. ");
			queryString.append("} LIMIT 30000 OFFSET 10000");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String entity = result.getResource("s").toString().substring(uriPrefix.length());
				if (entity.indexOf("%") != -1 || entity.matches(".*\\d.*")) {
					continue;
				}
				entities.add(entity);
				if (entities.size() == nEntities)
					break;
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		return entities;
	} // end of getSampleEntities


	public void createCorpusInOneFile(String outputDir) {
		int docId = 0;
		int wordId = 0;
		Set<String> duplicateEntities = new HashSet<String>();
		Set<String> duplicateWords    = new HashSet<String>();
		Map<String,Integer> wordToIdMap = new HashMap<String,Integer>();
		try {
			FileWriter docfile = new FileWriter(outputDir + "documentLookup.txt");
			FileWriter corpusfile = new FileWriter(outputDir + "corpus.txt");
			FileWriter vocabfile = new FileWriter(outputDir + "vocabLookup.txt");
			for (Map.Entry<String,Set<String>> entry : dataset.entrySet()) {
				Set<String> entities = entry.getValue();
				for (String entity : entities) {
					if (!duplicateEntities.contains(entity)) {
						docfile.write(docId + "\t" + entity + "\n");
						duplicateEntities.add(entity);
						List<String> doc = new ArrayList<String>(10000);
						
						// find the entity types and consider them as words for the document
						Set<String> docLabels = findEntityTypes(entity);
						
						for (String l : docLabels) {
							doc.add(l.toLowerCase());
						}
						
						// get related triples and construct bag of words
						List<String> bow = constructBowFromProperties(entity);
						
						Set<String> relatedEntities = findRelatedEntities(entity);
						
						// when we want to create bag of integers for docs from ONLY related entity types
//						List<Integer> doc = new ArrayList<Integer>(10000);
//						for (String e : relatedEntities) {
//							Set<String> etypes = findEntityTypes(e);
//							for (String et : etypes) {
//								et = et.toLowerCase();
//								if (!duplicateTypes.contains(et)) {
//									vocabfile.write(typeId + "\t" + et + "\n");
//									//								vocabfile.flush();
//									duplicateTypes.add(et);
//									typeToIdMap.put(et, typeId);
//									typeId++;
//								}
//								int wordId = typeToIdMap.get(et);
//								doc.add(wordId);
//							} // end of for
//						} // end of for
						
						// add entity types to the bag of words of the document WITHOUT REPETITION
						Set<String> etypes = new HashSet<String>();
						for (String e : relatedEntities) {
							etypes.addAll(findEntityTypes(e));
						} // end of for
						for (String et : etypes) {
							et = et.toLowerCase();
							if (!duplicateWords.contains(et)) {
								vocabfile.write(wordId + "\t" + et + "\n");
								duplicateWords.add(et);
								wordToIdMap.put(et, wordId);
								wordId++;
							}
							doc.add(et);
						} // end of for
						
//						// add entity types to the bag of words of the document WITH REPETITION
//						for (String e : relatedEntities) {
//							Set<String> etypes = findEntityTypes(e);
//							for (String et : etypes) {
//								et = et.toLowerCase();
//								if (!duplicateWords.contains(et)) {
//									vocabfile.write(wordId + "\t" + et + "\n");
//									duplicateWords.add(et);
//									wordToIdMap.put(et, wordId);
//									wordId++;
//								}
//								doc.add(et);
//							} // end of for
//						} // end of for
						
						// also add the bag of words from the related triples
						for (String w : bow) {
							if (!duplicateWords.contains(w)) {
								vocabfile.write(wordId + "\t" + w + "\n");
								duplicateWords.add(w);
								wordToIdMap.put(w, wordId);
								wordId++;
							}
							doc.add(w);
						} // end of for
						
						String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
						corpusfile.write(docId + " " + doctext + "\n");
//						corpusfile.write(docId + " " + entity + " " + doctext + "\n");
					} // end of for (String entity
					docId++;
					System.out.println("docId = " + docId);
				} // end of if
			} // end of for
			docfile.close();
			corpusfile.close();
			vocabfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createCorpusInOneFile
	
	public void createRandomCorpus(String outputDir) {
		int docId = 0;
		int wordId = 0;
		Set<String> duplicateEntities = new HashSet<String>();
		Set<String> duplicateWords    = new HashSet<String>();
		Map<String,Integer> wordToIdMap = new HashMap<String,Integer>();
		try {
			FileWriter docfile = new FileWriter(outputDir + "documentLookup.txt");
			FileWriter corpusfile = new FileWriter(outputDir + "corpus.txt");
			FileWriter vocabfile = new FileWriter(outputDir + "vocabLookup.txt");
			
			// randomly sample 10000 entities from Dbpedia
			List<String> allentities = new ArrayList<String>(getSampleEntities(10000));
			Set<String> testEntities =  new HashSet<String>();
			Random r = new Random();
			Set<Integer> duplicates =  new HashSet<Integer>();
			int counter = 1;
			while (counter <= 2000) {
				int index = r.nextInt(allentities.size());
				if (!duplicates.contains(index)) {
					duplicates.add(index);
					testEntities.add(allentities.get(index));
					counter++;
				} // end of if
			} // end of while
			
			for (String entity : allentities) {
				if (!testEntities.contains(entity)) {
					if (!duplicateEntities.contains(entity)) {
						docfile.write(docId + "\t" + entity + "\n");
						duplicateEntities.add(entity);
						List<String> doc = new ArrayList<String>(10000);

						// find the entity types and consider them as words for the document
						Set<String> docLabels = findEntityTypes(entity);

						for (String l : docLabels) {
							doc.add(l.toLowerCase());
						}

						// get related triples and construct bag of words
						List<String> bow = constructBowFromProperties(entity);

						Set<String> relatedEntities = findRelatedEntities(entity);

						// add entity types to the bag of words of the document WITHOUT REPETITION
						Set<String> etypes = new HashSet<String>();
						for (String e : relatedEntities) {
							etypes.addAll(findEntityTypes(e));
						} // end of for
						for (String et : etypes) {
							et = et.toLowerCase();
							if (!duplicateWords.contains(et)) {
								vocabfile.write(wordId + "\t" + et + "\n");
								duplicateWords.add(et);
								wordToIdMap.put(et, wordId);
								wordId++;
							}
							doc.add(et);
						} // end of for

						// also add the bag of words from the related triples
						for (String w : bow) {
							if (!duplicateWords.contains(w)) {
								vocabfile.write(wordId + "\t" + w + "\n");
								duplicateWords.add(w);
								wordToIdMap.put(w, wordId);
								wordId++;
							}
							doc.add(w);
						} // end of for

						String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
						corpusfile.write(entity + " " + doctext + "\n");
						//					corpusfile.write(docId + " " + doctext + "\n");
					} // end of if
				} // end of if
				docId++;
				System.out.println("docId = " + docId);
			} // end of for (String entity
			docfile.close();
			corpusfile.close();
			vocabfile.close();
			FileWriter testCorpusfile = new FileWriter(outputDir + "testcorpus.txt");
			FileWriter testdocfile = new FileWriter(outputDir + "testdocumentLookup.txt");
			FileWriter testvocabfile = new FileWriter(outputDir + "testvocabLookup.txt");
			docId = 0;
			wordId = 0;
			duplicateEntities.clear();
			for (String entity : testEntities) {
				if (!duplicateEntities.contains(entity)) {
					testdocfile.write(docId + "\t" + entity + "\n");
					duplicateEntities.add(entity);
					List<String> doc = new ArrayList<String>(10000);

					// find the entity types and consider them as words for the document
					Set<String> docLabels = findEntityTypes(entity);

					for (String l : docLabels) {
						doc.add(l.toLowerCase());
					}

					// get related triples and construct bag of words
					List<String> bow = constructBowFromProperties(entity);

					Set<String> relatedEntities = findRelatedEntities(entity);

					// add entity types to the bag of words of the document WITHOUT REPETITION
					Set<String> etypes = new HashSet<String>();
					for (String e : relatedEntities) {
						etypes.addAll(findEntityTypes(e));
					} // end of for
					for (String et : etypes) {
						et = et.toLowerCase();
						if (!duplicateWords.contains(et)) {
							testvocabfile.write(wordId + "\t" + et + "\n");
							duplicateWords.add(et);
							wordId++;
						}
						doc.add(et);
					} // end of for

					// also add the bag of words from the related triples
					for (String w : bow) {
						if (!duplicateWords.contains(w)) {
							testvocabfile.write(wordId + "\t" + w + "\n");
							duplicateWords.add(w);
							wordId++;
						}
						doc.add(w);
					} // end of for

					String doctext = doc.toString().replace("[", "").replace("]", "").replace(",", "");
					testCorpusfile.write(entity + " " + doctext + "\n");
					//					corpusfile.write(docId + " " + doctext + "\n");
				} // end of if
				docId++;
				System.out.println("testdocId = " + docId);
			} // end of for (String entity
			testCorpusfile.close();
			testdocfile.close();
			testvocabfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // end of createRandomCorpus

	public Set<String> findEntityTypes(String entity) {
		QueryEngineHTTP qe = null;
		Set<String> etypes = new HashSet<String>();
		try {
			String endpoint = "http://localhost:8890/sparql";
			ResultSet results = null;
			StringBuffer queryString = new StringBuffer(400);
			queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
			queryString.append("<" + uriPrefix + entity + "> a ?o . ");
			queryString.append("}");

			qe = new QueryEngineHTTP(endpoint, queryString.toString());
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				String e = result.getResource("o").toString();
				// Only includes the types from the Dbpedia ontology
				if (e.indexOf(typeUriPrefix) == -1)
					continue;
				e = result.getResource("o").toString().substring(typeUriPrefix.length());
				
				// types such as Wikidata:Q532 are filtered out
				if (e.indexOf("Wiki") != -1)
					continue;
				etypes.add(e);
			} // end of while
		}catch(QueryException e) {
			e.printStackTrace();
		}
		finally {
			qe.close();
		}
		
		
		
		
//		virtGraph = connectToVirtuoso();
//		System.out.println("Successfully Connected to Virtuoso!\n");
//		Set<String> etypes = new HashSet<String>();
//		
//		StringBuffer queryString = new StringBuffer(400);
//		queryString.append("SELECT ?o FROM <" + GRAPH + "> WHERE { ");
//		queryString.append("<" + uriPrefix + entity + "> a ?o . ");
//		queryString.append("}");
//		VirtuosoQueryExecution vqe = null;
//		try{
//			com.hp.hpl.jena.query.Query sparql = QueryFactory.create(queryString.toString());
//			// System.out.println("QUERY: " + queryString.toString());
//			vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
//			ResultSet results = vqe.execSelect();
//			while (results.hasNext()) {
//				QuerySolution result = results.nextSolution();
//				String e = result.get("o").toString();
//				
//				// Only includes the types from the Dbpedia ontology
//				if (e.indexOf(typeUriPrefix) == -1)
//					continue;
//				e = result.get("o").toString().substring(typeUriPrefix.length());
//				
//				// types such as Wikidata:Q532 are filtered out
//				if (e.indexOf("Wiki") != -1)
//					continue;
//				etypes.add(e);
//			} // end of while
//		}catch(Exception e) {
//			System.out.println("Error: " + e.getMessage() + "   " + queryString.toString());
//		}finally {
//			vqe.close();
//		}
		return etypes;
	} // end of findEntityTypes
	
	

}
