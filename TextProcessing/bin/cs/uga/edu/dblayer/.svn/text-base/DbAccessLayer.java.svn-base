package cs.uga.edu.dblayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
	
	
	public static ResultSet retrieve(Connection con, PreparedStatement stmt) {
		ResultSet rset = null;
		try {
			rset = stmt.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rset;
	} // end of retrieve
	
	
	
	
	/*********************************************************
	 * @param con
	 * @param query
	 * @return
	 */
	public static ResultSet retrieve(Connection con, String query) {
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
	
	
	public static int create(Connection con, PreparedStatement stmt) {
	    int rows = 0;
		try {
		    rows = stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	    return rows;
	} // end of create
	
	
	/*********************************************************
	 * @param con
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public static int create(Connection con, String query) {
	    int rows = 0;
		try {
			Statement stmt = con.createStatement();
		    rows = stmt.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	    return rows;
	} // end of create
	
	
	public static int update(Connection con, PreparedStatement stmt) {
	    int rows = 0;
		try {
		    rows = stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	    return rows;
	}// end of update
	
	
	/*********************************************************
	 * @param con
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public static int update(Connection con, String query) {
	    int rows = 0;
		try {
			Statement stmt = con.createStatement();
		    rows = stmt.executeUpdate(query);
		} catch (SQLException e) {
			System.out.println(e.getMessage() + "query:"+query);
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
