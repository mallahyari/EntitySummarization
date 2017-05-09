package cs.uga.edu.dicgenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

import static cs.uga.edu.dicgenerator.DbAccessLayer.*;

public class EntityManagerImpl {
	
	private Connection conn = null;
	Savepoint spt = null;
	
	public EntityManagerImpl() {
		conn = connect();
	}
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
	 * @param sqlCmd
	 * @return
	 */
	public ResultSet ifind (String sqlCmd) {
		if (conn == null)
			conn = connect();
		ResultSet rs = retrieve(conn, sqlCmd);
		return rs;
	}// end of ifind
	
	/*********************************************************
	 * @param sqlCmd
	 * @return
	 * @throws Exception
	 */
	public int persist(String sqlCmd) throws Exception {
		if (conn == null)
			conn = connect();
		int rows = create(conn, sqlCmd);
		return rows;
	}//end of persist
	
	/*********************************************************
	 * @param sqlCmd
	 * @return
	 * @throws Exception
	 */
	public int updateObject(String sqlCmd) throws Exception{
		if (conn == null)
			conn = connect();
		int rows = update(conn, sqlCmd);
		return rows;
	}//end of persist
	
	/*********************************************************
	 * 
	 */
	public void disconnectDatabase() {
		disconnect(conn);
		conn = null;
	}// end of disconnectDatabase

}
