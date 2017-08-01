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

public class MQTTImpl implements IPubSubbable<String, String>, MqttCallback {
	
	//TODO: Change Double[] publish to String and do the encoding myself
	
	final int qos = 0; 
	final String broker;
	final String id = "JavaAPI" +  System.currentTimeMillis();
	final MemoryPersistence persistence = new MemoryPersistence();
	MqttClient client;
	final LinkedBlockingQueue<MessageTuple<String>> recQ = new LinkedBlockingQueue<MessageTuple<String>>();
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
		} else {
			System.out.println("Couldn't connect to server");
		}
	}


	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		try {
			client.disconnect();
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
	public MessageTuple<String> getMessage() {
		try {
			return recQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public void publish(MessageTuple<String> message) {
		
		MqttMessage m = new MqttMessage(message.message.getBytes());
		
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
		
		//Parse message into reasonable format
		try {
			recQ.put(new MessageTuple<String>(topic, msg));
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
	}	
}
