package parsers;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;

public final class Parsers {
	
	// This class really just holds the singletons
	
	public static MQTTParser mqttParser; 
	
	public static final MQTTParser getParserSingleton(String host, int port) {
		
		if (mqttParser == null) {
			mqttParser = new MQTTParser(host, port);
		}
		
		return mqttParser;		
	}
			
	public static void main(String[] args) throws InterruptedException, IOException, MqttException {
		
		Parsers.getParserSingleton("localhost", 1883);

		while(true) {
						
			String data = mqttParser.mqttParser.getMessageTimeout("overhead_tracker/all_robot_pose_data", 15000).message;
			
			System.out.println(data);
			
//			for (int i = 0; i < data[0].length; i++) {
//												
//				System.out.println(data[0][i] + " "  + data[1][i] + " "  + data[2][i] + " " + data[3][i] + " " + data[4][i]);
//				
//				Double[] input = {(double) 0.1, (double) 3};
//				
//				System.out.println(data[0][i].intValue());
//				
//				String specificRobotChannel = mqttParser.baseRobotChannel + "/" + data[0][i].intValue();
//				
//				System.out.println(specificRobotChannel);
//				
//			}					
		}
	}
}
