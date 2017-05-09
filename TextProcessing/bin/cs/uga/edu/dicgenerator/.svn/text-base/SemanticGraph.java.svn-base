/**
 * 
 */
package cs.uga.edu.dicgenerator;

import java.util.HashMap;
import java.util.Map;

import grph.Grph;

/**
 * @author Mehdi
 *
 */
public class SemanticGraph extends Grph{

	
	private static final long serialVersionUID = 1L;
	private Map<Integer,Double> vertexWeightProperty = new HashMap<Integer,Double>();


	public SemanticGraph() {
		super();
	}
	
	public SemanticGraph(int initialVertexCapacity, int initialEdgeCapacity)  {
		super(initialVertexCapacity, initialEdgeCapacity);
	}
	
	
	public double getVertexWeight(int v) {
		return vertexWeightProperty.get(v);
	}
	
	public void setVertexWeight(int v, double val) {
		vertexWeightProperty.put(v, val);
	}
	
	
	/**
	 * @return the vertexWeightProperty
	 */
	public Map<Integer, Double> getVertexWeightProperty() {
		return vertexWeightProperty;
	}

	/**
	 * @param vertexWeightProperty the vertexWeightProperty to set
	 */
	public void setVertexWeightProperty(Map<Integer, Double> vertexWeightProperty) {
		this.vertexWeightProperty = vertexWeightProperty;
	}
	
	
//	public static void main(String[] args) {
//	}

}
