package vizier;

import com.google.gson.JsonObject;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class VizierNode {

    private VizierMqttClient mqttClient;

    private final JsonObject nodeDescriptor;
    private final ThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(8);

    public VizierNode(String host, int port, JsonObject nodeDescriptor) {

        this.nodeDescriptor = nodeDescriptor;
        this.mqttClient = new VizierMqttClient(host, port);
    }

    public void verify() {

    }

    public void start() {

    }

    public void stop() {

    }

    //private void handleRequest(String )
}
