/**
 * 
 */
package cs.uga.edu.topicmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;

/**
 * @author Mehdi
 *
 */
public class MyPipe {

	Pipe pipe = null;
	
	public MyPipe() {
		pipe = buildPipe();
	}
	
	public Pipe buildPipe() {	
		List<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add(new Input2CharSequence("UTF-8"));
//		Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+");
		Pattern tokenPattern = Pattern.compile("[\\p{L}_]+");
		pipeList.add(new CharSequence2TokenSequence(tokenPattern));
		pipeList.add(new TokenSequenceLowercase());
		pipeList.add(new TokenSequenceRemoveStopwords(false, false));
		pipeList.add(new TokenSequence2FeatureSequence());
		pipeList.add(new FeatureSequence2FeatureVector());
//		pipeList.add(new PrintInputAndTarget());
		return new SerialPipes(pipeList);
	} // end of buildPipe
	
	
	/**
	 * @return the pipe
	 */
	public Pipe getPipe() {
		return pipe;
	}

	/**
	 * @param pipe the pipe to set
	 */
	public void setPipe(Pipe pipe) {
		this.pipe = pipe;
	}

	public static void main(String[] args) {

	}

}
