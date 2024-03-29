/**
 * 
 */
package cs.uga.edu.dicgenerator;

import virtuoso.jena.driver.VirtGraph;

/**
 * @author mehdi
 *
 */
public abstract class VirtuosoAccess {
	
	public static String GRAPH      			 = "http://dbpedia.org";
	public static String CONNECTION_URL         = "jdbc:virtuoso://localhost:1111";
	static String USERNAME    			 = "dba";
	static String PASSOWRD    			 = "dba";
	
	
	public static VirtGraph connectToVirtuoso() {
		return new VirtGraph (GRAPH, CONNECTION_URL, USERNAME, PASSOWRD); 
	} // end of connectToVirtuoso
	
	
	public static VirtGraph connectToVirtuoso(String graph) {
		return new VirtGraph (graph, CONNECTION_URL, USERNAME, PASSOWRD); 
	} // end of connectToVirtuoso

}
