package parser;


/**
 * 
 * @author robotarium
 *
 * @param <T>
 */
public class MessageTuple<T> {
	
	public final String channel; 
	public final T message; 
	
	public MessageTuple(String channel, T message) {
		this.channel = channel; 
		this.message = message;
	}
}

