package parsers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.google.gson.Gson;

import mqttimpl.MQTTImpl;
import mqttimpl.SetupMessage;
import parser.Parser;
import parser.Parser.ParserBuilder;

public class MQTTParser {
		
		public Map<String, String> alias;
		public String robotChannel;
	
		public String readFile(String path, Charset encoding) throws IOException {
		  byte[] encoded = Files.readAllBytes(Paths.get(path));
		  return new String(encoded, encoding);
		}
	
		//Pattern is MAC to alias

		public Parser<Double[][], Double[]> mqttParser; 
		
		public MQTTParser() {
			
			String json = null;
			
			try {
				json = readFile("/home/robotarium/Git/RobotariumRepositories/robotarium_nodes/api/config.json", Charset.defaultCharset());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println(json);
			
			ParserBuilder<Double[][], Double[]> builder = new ParserBuilder<Double[][],Double[]>(new MQTTImpl("192.168.1.2", 1884));
			
			Gson gson = new Gson();
			SetupMessage response = gson.fromJson(json, SetupMessage.class);
			
			// Input channel for the overhead tracker
			String channel = "overhead_tracker/all_robot_pose_data";
			
			robotChannel = "matlab_api";
			
			builder.withSubscriptionTo(channel);
			
			System.out.println("Subscribing to channel: " + channel);
			
			json = null;
			
			try {
				json = readFile("/home/robotarium/Git/RobotariumRepositories/robotarium_nodes/api/api_alias.json", Charset.defaultCharset());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
										
			alias = gson.fromJson(json, Map.class);
			
			mqttParser = builder.build();
		}
}
