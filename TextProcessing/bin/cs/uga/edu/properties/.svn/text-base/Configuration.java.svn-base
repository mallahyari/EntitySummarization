/**
 * 
 */
package cs.uga.edu.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Mehdi
 *
 */
public abstract class Configuration {
	private static Properties prop = null;
	private static InputStream fileInput = null;
	private static final String FILE_PATH = "config.properties";
	
	
	public static String getProperty(String key) {
		if (prop == null) {
			LoadPropertyFile();
		}
		return prop.getProperty(key);
	} // end of getProperty
	
	public static void LoadPropertyFile() {
		prop = new Properties();
		try {
			fileInput = new FileInputStream(FILE_PATH);
			prop.load(fileInput);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (fileInput != null) {
				try {
					fileInput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	} // end of LoadPropertyFile
}
