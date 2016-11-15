package interfaces;

import parser.MessageTuple;

public interface IPubSubbable<ReceiveType, PublishType> extends IShutdownable {
	
	public void subscribe(String channel); 
	public void unsubscribe(String channel);
	public MessageTuple<ReceiveType> getMessage();
	public void publish(MessageTuple<PublishType> message);
}
