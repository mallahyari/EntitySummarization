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
public class WordToIntMapperClass implements Serializable{
	
	
	private static final long serialVersionUID = 1L;
	private Map<String, Integer> wordToInt = null;
	private ArrayList<String> wordList = null;
	private final int numOfDistinctWords    = 3000000;
	
	
	public WordToIntMapperClass() {
		setWordToInt(new HashMap<String, Integer>(numOfDistinctWords));
		setWordList(new ArrayList<String>(numOfDistinctWords));
	}


	/**
	 * @return the wordToInt
	 */
	public Map<String, Integer> getWordToInt() {
		return wordToInt;
	}


	/**
	 * @param wordToInt the wordToInt to set
	 */
	public void setWordToInt(Map<String, Integer> wordToInt) {
		this.wordToInt = wordToInt;
	}


	/**
	 * @return the wordList
	 */
	public ArrayList<String> getWordList() {
		return wordList;
	}


	/**
	 * @param wordList the wordList to set
	 */
	public void setWordList(ArrayList<String> wordList) {
		this.wordList = wordList;
	}

}
