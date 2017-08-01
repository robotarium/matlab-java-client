package parsers;

import mqttimpl.MQTTImpl;
import parser.Parser;
import parser.Parser.ParserBuilder;

public class MQTTParser {
		
	// Base channel for robots.  The channel for a robot with tag ID 6 would be "matlab_api/6 ... "
	public final String baseRobotChannel = "matlab_api";
	// Hard-coded channel for the tracker data
	public final String trackerChannel = "overhead_tracker/all_robot_pose_data";

	public Parser<String, String> mqttParser; 
	
	public MQTTParser(String host, int port) {
		
		// Make sure we attach to a specific host/port		
		mqttParser = new ParserBuilder<String, String>(new MQTTImpl(host, port))
				.withSubscriptionTo(trackerChannel)
				.build();
	}
}
