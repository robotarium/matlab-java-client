package parser;

public class Metrics {

	public double startTime = System.currentTimeMillis();
	public double receiveMessageCount;
	public double sentMessageCount;
	
	public double getReceiveRate() {
		return (receiveMessageCount / (System.currentTimeMillis() - startTime));
	}
	
	public double getSendRate() {
		return (sentMessageCount / (System.currentTimeMillis() - startTime));
	}
	
	public void incrementReceived() {
		receiveMessageCount++;
	}
	
	public void incrementSent() {
		sentMessageCount++;
	}
}
