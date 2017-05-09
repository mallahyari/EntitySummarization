/**
 * 
 */
package cs.uga.edu.dicgenerator;

import java.util.Comparator;
import java.util.Map;


/**
 * @author Mehdi
 *
 */
public class MyComparable implements Comparator<Integer>{
	
	 Map<Integer, Double> items;
	public MyComparable(Map<Integer, Double> items) {
		this.items = items;
	}

	@Override
	public int compare(Integer o1, Integer o2) {
		return items.get(o1).compareTo(items.get(o2));
	}


}
