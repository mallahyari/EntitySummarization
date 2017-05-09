package cs.uga.edu.dicgenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbAccessLayer {
	
	/*********************************************************
	 * @return
	 */
	public static Connection connect() {
		try {
            Class.forName(DbAccess.DB_DRIVE_NAME);
        } catch (ClassNotFoundException ex) {
        	
        }
		try {
			return DriverManager.getConnection(DbAccess.CONNECTION_URL,	DbAccess.USERNAME, DbAccess.PASSOWRD);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	} // end of connect
	
	/*********************************************************
	 * @param con
	 * @param query
	 * @return
	 */
	public static ResultSet retrieve (Connection con, String query) {
		ResultSet rset = null;
		try {
			Statement stmt = con.createStatement();
			rset = stmt.executeQuery(query);
			return rset;
		} catch (SQLException e) {
			e.printStackTrace();
			return rset;
		}
	}// end of retrieve
	
	/*********************************************************
	 * @param con
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public static int create(Connection con, String query) throws Exception{
	    int rows = 0;
		try {
			Statement stmt = con.createStatement();
		    rows = stmt.executeUpdate(query);
		    stmt.close();
		} catch (SQLException e) {
//			System.out.println(e.getMessage() + "query:"+query);
			//e.printStackTrace();
			throw(e);
		}
	    return rows;
	}// end of create
	
	/*********************************************************
	 * @param con
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public static int update(Connection con, String query) throws Exception{
	    int rows = 0;
		try {
			Statement stmt = con.createStatement();
		    rows = stmt.executeUpdate(query);
		    stmt.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage() + "query:"+query);
			//e.printStackTrace();
			throw(e);
		}
	    return rows;
	}// end of update
	
	/*********************************************************
	 * @param con
	 */
	public static void disconnect(Connection con) {
		try {
			if (con != null)
				con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}// end of disconnect

}
