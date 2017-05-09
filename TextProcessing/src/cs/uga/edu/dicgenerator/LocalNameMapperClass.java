/**
 * 
 */
package cs.uga.edu.dicgenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mehdi
 *
 */
public class LocalNameMapperClass implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private Map<String, Integer> nameToInt = null;
	private ArrayList<String> localNameList = null;
	private final int numOfEntities    = 13000000;
	 
	 
	 public LocalNameMapperClass() {
		 nameToInt = new HashMap<String, Integer>(numOfEntities);
		 localNameList = new ArrayList<String>(numOfEntities);
	 }


	/**
	 * @return the nameToInt
	 */
	public Map<String, Integer> getNameToInt() {
		return nameToInt;
	}


	/**
	 * @param nameToInt the nameToInt to set
	 */
	public void setNameToInt(Map<String, Integer> nameToInt) {
		this.nameToInt = nameToInt;
	}


	/**
	 * @return the localNameList
	 */
	public ArrayList<String> getLocalNameList() {
		return localNameList;
	}


	/**
	 * @param localNameList the localNameList to set
	 */
	public void setLocalNameList(ArrayList<String> localNameList) {
		this.localNameList = localNameList;
	}
	 
}
