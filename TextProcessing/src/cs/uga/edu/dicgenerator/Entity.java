/**
 * 
 */
package cs.uga.edu.dicgenerator;

import java.io.Serializable;

/**
 * @author Mehdi
 *
 */
public class Entity implements Serializable{

	private static final long serialVersionUID = 1L;
	private int eId = 0;
	private char attr = '0';
	
	public Entity() {
		
	}
	
	public Entity( int eId, char attr) {
		this.eId  = eId;
		this.attr = attr;
	}
	
	
	public int hashCode() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.eId);
		return sb.toString().hashCode();
	}
	
	public boolean equals(Object o){
		if (o == null) return false;
		if (o == this) return true;	
		if (this.getClass() != o.getClass()) return false;
		Entity e = (Entity) o;
		if (this.hashCode() == e.hashCode()) return true;
		return false;
		
	}
	
	public String toString() {
		return this.eId + "  " + this.attr;
		
	}

	/**
	 * @return the eId
	 */
	public int geteId() {
		return eId;
	}

	/**
	 * @param eId the eId to set
	 */
	public void seteId(int eId) {
		this.eId = eId;
	}

	/**
	 * @return the attr
	 */
	public char getAttr() {
		return attr;
	}

	/**
	 * @param attr the attr to set
	 */
	public void setAttr(char attr) {
		this.attr = attr;
	}
	
	

}
