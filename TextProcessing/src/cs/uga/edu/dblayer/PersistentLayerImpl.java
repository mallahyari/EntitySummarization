package cs.uga.edu.dblayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import static cs.uga.edu.dblayer.DbAccessLayer.*;

/**
 * @author Mehdi
 *
 */
public class PersistentLayerImpl {
	
	private Connection conn = null;
	private PreparedStatement retrieveWordConcept = null;
	private PreparedStatement retrieveConceptTopic = null;
	private PreparedStatement retrieveTopicDocument = null;
	private PreparedStatement retrieveNonZeroWordConcept = null;
	private PreparedStatement retrieveNonZeroConceptTopic = null;
	private PreparedStatement retrieveNonZeroTopicDocument = null;
	private PreparedStatement updateWordConcept = null;
	private PreparedStatement updateConceptTopic = null;
	private PreparedStatement updateTopicDocument = null;
	private PreparedStatement insertWordConcept = null;
	private PreparedStatement insertConceptTopic = null;
	private PreparedStatement insertTopicDocument = null;
	private String retrieveWordConceptQuery = null;
	private String retrieveConceptTopicQuery = null;
	private String retrieveTopicDocumentQuery = null;
	private String retrieveNonZeroWordConceptQuery = null;
	private String retrieveNonZeroConceptTopicQuery = null;
	private String retrieveNonZeroTopicDocumentQuery = null;
	private String updateWordConceptQuery = null;
	private String updateConceptTopicQuery = null;
	private String updateTopicDocumentQuery = null;
	private String insertWordConceptQuery = null;
	private String insertConceptTopicQuery = null;
	private String insertTopicDocumentQuery = null;
	
	private Savepoint spt = null;
	
	public PersistentLayerImpl() {
		conn = connect();
		try {
			initializePreparedStatements();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * @throws SQLException 
	 * 
	 */
	public void initializePreparedStatements() throws SQLException {
		retrieveWordConceptQuery = "SELECT total_count FROM word_concept WHERE word_id = ? AND concept_id = ?";
		retrieveConceptTopicQuery = "SELECT total_count FROM concept_topic WHERE concept_id = ? AND topic_id = ?";
		retrieveTopicDocumentQuery = "SELECT total_count FROM topic_document WHERE topic_id = ? AND document_id = ?";
		retrieveNonZeroWordConceptQuery = "SELECT * FROM word_concept WHERE total_count > 0 ORDER BY concept_id";
		retrieveNonZeroConceptTopicQuery = "SELECT * FROM concept_topic WHERE total_count > 0 ORDER BY concept_id";
		retrieveNonZeroTopicDocumentQuery = "SELECT * FROM topic_document WHERE total_count > 0 ORDER BY topic_id";
		updateWordConceptQuery = "UPDATE word_concept SET total_count = ? WHERE word_id = ? AND concept_id = ?";
		updateConceptTopicQuery = "UPDATE concept_topic SET total_count = ? WHERE concept_id = ? AND topic_id = ?";
		updateTopicDocumentQuery = "UPDATE topic_document SET total_count = ? WHERE topic_id = ? AND document_id = ?";
		insertWordConceptQuery = "INSERT INTO word_concept (word_id,concept_id,total_count) VALUES (?,?,?)";
		insertConceptTopicQuery = "INSERT INTO concept_topic (concept_id,topic_id,total_count) VALUES (?,?,?)";
		insertTopicDocumentQuery = "INSERT INTO topic_document (topic_id,document_id,total_count) VALUES (?,?,?)";
		retrieveWordConcept = conn.prepareStatement(retrieveWordConceptQuery);
		retrieveConceptTopic = conn.prepareStatement(retrieveConceptTopicQuery);
		retrieveTopicDocument = conn.prepareStatement(retrieveTopicDocumentQuery);
		retrieveNonZeroWordConcept = conn.prepareStatement(retrieveNonZeroWordConceptQuery);
		retrieveNonZeroConceptTopic = conn.prepareStatement(retrieveNonZeroConceptTopicQuery);
		retrieveNonZeroTopicDocument = conn.prepareStatement(retrieveNonZeroTopicDocumentQuery);
		updateWordConcept = conn.prepareStatement(updateWordConceptQuery);
		updateConceptTopic = conn.prepareStatement(updateConceptTopicQuery);
		updateTopicDocument = conn.prepareStatement(updateTopicDocumentQuery);
		insertWordConcept = conn.prepareStatement(insertWordConceptQuery);
		insertConceptTopic = conn.prepareStatement(insertConceptTopicQuery);
		insertTopicDocument = conn.prepareStatement(insertTopicDocumentQuery);
	} // end of initializePreparedStatements
	
	
	
	
	
	/*********************************************************
	 * 
	 */
	public void makeSavePoint() {
		try {
			if (conn == null)
				conn = connect();
			conn.setAutoCommit(false);
			spt = conn.setSavepoint("svpt");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	} // end of makeSavePoint
	
	/*********************************************************
	 * 
	 */
	public void runRollback() {
		try {
			conn.rollback(spt);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	} // end of runRollback
	/*********************************************************
	 * 
	 */
	public void doCommit() {
		try {
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	} // end of doCommit
	
	/*********************************************************
	 * @param stmt
	 * @param wordId
	 * @param conceptId
	 * @return
	 * @throws SQLException 
	 */
	public ResultSet ifindWordConceptCount (int wordId, int conceptId) throws SQLException {
		if (conn == null)
			conn = connect();
		retrieveWordConcept.setInt(1, wordId);
		retrieveWordConcept.setInt(2, conceptId);
		return retrieve(conn, retrieveWordConcept);
	} // end of ifindWordConceptCount
	
	
	public ResultSet ifindNonZeroWordConceptCount () throws SQLException {
		if (conn == null)
			conn = connect();
		return retrieve(conn, retrieveNonZeroWordConcept);
	} // end of ifindNonZeroWordConceptCount
	
	
	/*************************************************
	 * @param conceptId
	 * @param topicId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet ifindConceptTopicCount (int conceptId, int topicId) throws SQLException {
		if (conn == null)
			conn = connect();
		retrieveConceptTopic.setInt(1, conceptId);
		retrieveConceptTopic.setInt(2, topicId);
		return retrieve(conn, retrieveConceptTopic);
	} // end of ifindConceptTopicCount
	
	
	public ResultSet ifindNonZeroConceptTopicCount () throws SQLException {
		if (conn == null)
			conn = connect();
		return retrieve(conn, retrieveNonZeroConceptTopic);
	} // end of ifindNonZeroConceptTopicCount
	
	
	public ResultSet ifindTopicDocumentCount (int topicId, int documentId) throws SQLException {
		if (conn == null)
			conn = connect();
		retrieveTopicDocument.setInt(1, topicId);
		retrieveTopicDocument.setInt(2, documentId);
		return retrieve(conn, retrieveTopicDocument);
	} // end of ifindTopicDocumentCount
	
	
	
	public ResultSet ifindNonZeroTopicDocumentCount () throws SQLException {
		if (conn == null)
			conn = connect();
		return retrieve(conn, retrieveNonZeroTopicDocument);
	} // end of ifindNonZeroTopicDocumentCount
	
	
	/*********************************************************
	 * @param sqlCmd
	 * @return
	 */
	public ResultSet ifind (String sqlCmd) {
		if (conn == null)
			conn = connect();
		return retrieve(conn, sqlCmd);
	}// end of ifind
	
	
	public int persistWordConceptCount(int wordId, int conceptId, int totalCount) throws SQLException{
		if (conn == null)
			conn = connect();
		insertWordConcept.setInt(1, wordId);
		insertWordConcept.setInt(2, conceptId);
		insertWordConcept.setInt(3, totalCount);
		return create(conn, insertWordConcept);
	} //end of persistWordConceptCount
	
	
	public int persistConceptTopicCount(int conceptId, int topicId, int totalCount) throws SQLException{
		if (conn == null)
			conn = connect();
		insertConceptTopic.setInt(1, conceptId);
		insertConceptTopic.setInt(2, topicId);
		insertConceptTopic.setInt(3, totalCount);
		return create(conn, insertConceptTopic);
	} //end of persistConceptTopicCount
	
	
	public int persistTopicDocumentCount(int topicId, int documentId, int totalCount) throws SQLException{
		if (conn == null)
			conn = connect();
		insertTopicDocument.setInt(1, topicId);
		insertTopicDocument.setInt(2, documentId);
		insertTopicDocument.setInt(3, totalCount);
		return create(conn, insertTopicDocument);
	} //end of persistTopicDocumentCount
	
	
	/*********************************************************
	 * @param sqlCmd
	 * @return
	 * @throws Exception
	 */
	public int persist(String sqlCmd) throws Exception {
		if (conn == null)
			conn = connect();
		return create(conn, sqlCmd);
	} //end of persist
	
	
	public int updateWordConceptCount (int totalCount, int wordId, int conceptId) throws SQLException {
		if (conn == null)
			conn = connect();
		updateWordConcept.setInt(1, totalCount);
		updateWordConcept.setInt(2, wordId);
		updateWordConcept.setInt(3, conceptId);
		return update(conn, updateWordConcept);
	} // end of updateWordConceptCount
	
	
	public int updateConceptTopicCount (int totalCount, int conceptId, int topicId) throws SQLException {
		if (conn == null)
			conn = connect();
		updateConceptTopic.setInt(1, totalCount);
		updateConceptTopic.setInt(2, conceptId);
		updateConceptTopic.setInt(3, topicId);
		return update(conn, updateConceptTopic);
	} // end of updateConceptTopicCount
	
	
	public int updateTopicDocumentCount (int totalCount, int topicId, int documentId) throws SQLException {
		if (conn == null)
			conn = connect();
		updateTopicDocument.setInt(1, totalCount);
		updateTopicDocument.setInt(2, topicId);
		updateTopicDocument.setInt(3, documentId);
		return update(conn, updateTopicDocument);
	} // end of updateTopicDocumentCount
	
	
	/*********************************************************
	 * @param sqlCmd
	 * @return
	 * @throws Exception
	 */
	public int updateObject(String sqlCmd) throws Exception{
		if (conn == null)
			conn = connect();
		return update(conn, sqlCmd);
	}//end of updateObject
	
	/*********************************************************
	 * 
	 */
	public void disconnectDatabase() {
		disconnect(conn);
		conn = null;
	}// end of disconnectDatabase

}
