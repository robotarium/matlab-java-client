package parser;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Wrapper class to allow for "easier" access
 * @author robotarium
 *
 */
public class Receiver<T> {
	public final LinkedBlockingQueue<MessageTuple<T>> receiver = new LinkedBlockingQueue<MessageTuple<T>>(1); 
}
