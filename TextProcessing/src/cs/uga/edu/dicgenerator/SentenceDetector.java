/**
 * 
 */
package cs.uga.edu.dicgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mehdi
 *
 */
public class SentenceDetector extends SentParDetector{

	/**
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public SentenceDetector() throws FileNotFoundException, UnsupportedEncodingException, IOException {
		super();
	}
	
	public List<String> getAllSentencesFromDocument(String document) {
//		File file = new File(filePath);
		List<String> sentences = new ArrayList<String>();
		//			InputStream instr = new FileInputStream(file) ;
		//			String output = getStrFromInstr(instr);
		document = markupRawText(1, document);
		String [] rawSentences = document.split("\n");
		sentences = new ArrayList<String>();
		for (String sentc : rawSentences) {
			if (sentc.startsWith("<p>") || sentc.startsWith("</p>")) {
				continue;
			}
			int beginOfSentence = sentc.indexOf('>') + 1;
			int endOfSentence   = sentc.lastIndexOf('.');
			if (beginOfSentence > -1 && endOfSentence > -1) {
				sentc = sentc.substring(beginOfSentence, endOfSentence);
				sentences.add(sentc);
			}else {
				sentences.add(sentc);
			} // end of if
		} // end of for
		return sentences;
	} // end of getAllSentencesFromFile

}
