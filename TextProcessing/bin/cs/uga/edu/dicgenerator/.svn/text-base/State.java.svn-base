/**
 * 
 */
package cs.uga.edu.dicgenerator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mehdi
 *
 */
public class State implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private int id = -1;
	private Map<Integer, Integer> edges = null;
	
	public State(int id) {
		this.id = id;
		edges = new HashMap<Integer, Integer>();
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	public void addNeighbor(int edge, int stId) {
		edges.put(edge, stId);
	} // end of addNeighbor
	
	
	public int findNeighbor(int edge) {
		if (edges.containsKey(edge)) {
			return edges.get(edge);
		}
		return -1;
	}
	
	/**
	 * @return the edges
	 */
	public Map<Integer, Integer> getEdges() {
		return edges;
	}

	/**
	 * @param edges the edges to set
	 */
	public void setEdges(Map<Integer, Integer> edges) {
		this.edges = edges;
	}

}
