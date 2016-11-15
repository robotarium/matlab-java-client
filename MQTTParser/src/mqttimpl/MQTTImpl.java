package mqttimpl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import interfaces.IPubSubbable;
import parser.MessageTuple;

public class MQTTImpl implements IPubSubbable<Double[][], Double[]>, MqttCallback {
	
	//TODO: Change Double[] publish to String and do the encoding myself
	
	final int qos = 0; 
	final String broker;
	final String id = "JavaAPI" +  System.currentTimeMillis();
	final MemoryPersistence persistence = new MemoryPersistence();
	MqttClient client;
	final LinkedBlockingQueue<MessageTuple<Double[][]>> recQ = new LinkedBlockingQueue<MessageTuple<Double[][]>>();
	final JsonParser parser = new JsonParser();
	final Gson g = new Gson();
	final MqttConnectOptions connOpts = new MqttConnectOptions();
	
	public MQTTImpl(String host, int port) {
		broker = "tcp://" + host + ":" + port;
	    connOpts.setCleanSession(true);
		try {
			client = new MqttClient(broker, id, persistence);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			client = null;
			e.printStackTrace();
		}
		
		if(client != null) {
			client.setCallback(this);
			try {
				client.connect(connOpts);
			} catch (MqttSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		try {
			client.disconnectForcibly();
		} catch (MqttException e) {
			System.err.println("Didn't shut down Java MQTT client correctly...");
			e.printStackTrace();
		}
	}

	@Override
	public void subscribe(String channel) {				
		try {
			client.subscribe(channel);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void unsubscribe(String channel) {
		try {
			client.unsubscribe(channel);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@Override
	public MessageTuple<Double[][]> getMessage() {
		try {
			return recQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public void publish(MessageTuple<Double[]> message) {
				
		if(message.message.length != 2) {
			System.err.println("Received message sent to " + message.channel + " that is not of size 2.");
			return;
		}
		
		RobotInput ri = new RobotInput(message.message[0], message.message[1]);
		
		String msg = g.toJson(ri, RobotInput.class);
		
		MqttMessage m = new MqttMessage(msg.getBytes());
		
		m.setQos(this.qos);
		
		try {
			client.publish(message.channel, m);
		} catch (MqttException e) {
			e.printStackTrace();
		} 
	}


	@Override
	public void connectionLost(Throwable arg0) {
		try {
			client.connect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void messageArrived(String topic, MqttMessage message) {	
		
		String msg = new String(message.getPayload());
		
		//System.out.println(msg);
		
		if(msg.equals("null")) {
			return;
		}
			
		JsonElement element = parser.parse(msg);

		JsonObject obj = element.getAsJsonObject();
		Set<Map.Entry<String, JsonElement>> entries = obj.entrySet();
		
		if(entries.isEmpty()) {
			return;
		}
		
		Double[][] data = new Double[5][entries.size()];
		int i = 0;
		
		for (Map.Entry<String, JsonElement> entry: entries) {
			
		    //System.out.println(entry.getKey());
		    RobotStateInfo s = g.fromJson(entry.getValue(), RobotStateInfo.class);
		    data[0][i] = Double.valueOf(entry.getKey());
		    data[1][i] = s.x; 
		    data[2][i]  = s.y; 
		    data[3][i] = s.theta;
		    data[4][i] = s.powerData;
		    i++;
		}
		
		//Parse message into reasonable format
		try {
			recQ.put(new MessageTuple<Double[][]>(topic, data));
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
	}
	
	private class RobotInput {
		public final double v; 
		public final double w; 
		
		public RobotInput(double v, double w) {
			this.v = v; 
			this.w = w;
		}
	}
	
	private class RobotStateInfo {
		
		public final double x; 
		public final double y; 
		public final double theta; 
		public final double powerData;
		
		public RobotStateInfo(double x, double y, double theta, double powerData) {
			this.x = x; 
			this.y = y; 
			this.theta = theta;
			this.powerData = powerData;
		}
	}
}
