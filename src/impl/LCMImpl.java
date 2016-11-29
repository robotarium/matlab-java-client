package impl;

import java.io.IOException;

import interfaces.IPubSubbable;
import lcm.lcm.LCM;
import lcm.lcm.LCMEncodable;
import lcm.lcm.MessageAggregator;
import lcm.lcm.MessageAggregator.Message;
import parser.MessageTuple;

public class LCMImpl implements IPubSubbable<Message, LCMEncodable> {
	
	private LCM lcmInterface;
	private MessageAggregator aggr;
	
	public LCMImpl() {
		try {
			lcmInterface = new lcm.lcm.LCM("udpm://239.255.76.67:7667?ttl=1");
			aggr = new lcm.lcm.MessageAggregator();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void subscribe(String channel) {
		lcmInterface.subscribe(channel, aggr);
		
	}

	@Override
	public void unsubscribe(String channel) {
		lcmInterface.unsubscribe(channel, aggr);	
	}

	@Override
	public MessageTuple<Message> getMessage() {
		Message m = aggr.getNextMessage();
		return new MessageTuple<Message>(m.channel, m);

	}

	@Override
	public void publish(MessageTuple<LCMEncodable> message) {
		lcmInterface.publish(message.channel, message.message);	
	}

	@Override
	public void shutdown() {
		lcmInterface.close();
	}
}
