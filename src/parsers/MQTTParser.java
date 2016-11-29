package parsers;

import mqttimpl.MQTTImpl;
import parser.Parser;
import parser.Parser.ParserBuilder;

public class MQTTParser {
		
	// Base channel for robots.  The channel for a robot with tag ID 6 would be "matlab_api/6 ... "
	public final String baseRobotChannel = "matlab_api";
	// Hard-coded channel for the tracker data
	public final String trackerChannel = "overhead_tracker/all_robot_pose_data";

	public Parser<Double[][], Double[]> mqttParser; 
	
	public MQTTParser() {
		
		// Make sure we attach to a specific host/port		
		mqttParser = new ParserBuilder<Double[][],Double[]>(new MQTTImpl("192.168.1.2", 1884))
				.withSubscriptionTo(trackerChannel)
				.build();
	}
}
