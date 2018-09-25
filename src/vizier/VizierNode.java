package vizier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class VizierNode {

    private VizierMqttClient mqttClient;
    private ConcurrentHashMap<String, Descriptor> nodeDescriptor = new ConcurrentHashMap<>();
    private ArrayList<JsonObject> requests;
    private String endpoint;

    private final ThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(8);

    public VizierNode(String host, int port, JsonObject nodeDescriptor) {

        this.endpoint = nodeDescriptor.get("end_point").getAsString();
        HashMap<String, JsonObject> nodeDescriptor_ = Utils.parseNodeDescriptor(nodeDescriptor);

        for (var x:
             nodeDescriptor_.keySet()) {
            nodeDescriptor.add(x, Descriptor.class));
        }

        this.requests = Utils.parseNodeDescriptorRequests(nodeDescriptor);

        this.mqttClient = new VizierMqttClient(host, port);

        Consumer<String> requestHandler = (r) -> this.handleRequest(r);
        this.mqttClient.subscribeWithCallback(Utils.createRequestLink(this.endpoint), requestHandler);
    }

    public void verify() {

    }

    public void start() {
        this.mqttClient.start();
    }

    public void stop() {
        this.mqttClient.stop();
    }

    private void handleRequest(String msg) {

        JsonObject jsonMsg = new JsonParser().parse(msg).getAsJsonObject();

        // Verify that message contains required fields
        String id = jsonMsg.get("id").getAsString();
        String method = jsonMsg.get("method").getAsString();
        String link = jsonMsg.get("method").getAsString();

        if(id == null) {
            // TODO Handle error
            return;
        }

        if(method == null) {
            // TODO Handle error
            return;
        }

        if(link == null) {
            // TODO Handle error
            return;
        }

        // Otherwise, proceed with response



    }
}
