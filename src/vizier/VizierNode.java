package vizier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import utils.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VizierNode {

    public VizierMqttClient mqttClient;
    private ConcurrentHashMap<String, LinkDescriptor> nodeDescriptor;
    private ArrayList<LinkRequestDescriptor> requests;
    private String endpoint;

    private final ThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(8);
    private final Logger logger = Logger.getGlobal();

    private final Set<String> puttableLinks;
    private final Set<String> publishableLinks;
    private final Set<String> gettableLinks;
    private final Set<String> subscribableLinks;

    /**
     * Creates a node on a vizier network.
     *
     * @param host IP for the MQTT broker
     * @param port Port for the MQTT broker.
     * @param nodeDescriptor Node descriptor in the required Vizier format.
     */
    public VizierNode(String host, int port, JsonObject nodeDescriptor) {

        this.endpoint = nodeDescriptor.get("end_point").getAsString();
        this.nodeDescriptor = new ConcurrentHashMap<>(Utils.parseNodeDescriptor(nodeDescriptor));
        this.requests = Utils.parseNodeDescriptorRequests(nodeDescriptor);

        // Determine the 4 classes of links
        this.puttableLinks = this.nodeDescriptor.entrySet().stream()
                .filter((x) -> x.getValue().getType() == "DATA")
                .map((x) -> x.getKey())
                .collect(Collectors.toSet());

        this.publishableLinks = this.nodeDescriptor.entrySet().stream()
                .filter((x) -> x.getValue().getType() == "STREAM")
                .map((x) -> x.getKey())
                .collect(Collectors.toSet());

        this.gettableLinks = this.requests.stream()
                .filter((x) -> x.getType() == "DATA")
                .map((x) -> x.getLink())
                .collect(Collectors.toSet());

        this.subscribableLinks = this.requests.stream()
                .filter((x) -> x.getType() == "STREAM")
                .map((x) -> x.getLink())
                .collect(Collectors.toSet());

        this.mqttClient = new VizierMqttClient(host, port);

        // Set up the local request handler method to receive all requests for the node.
        Consumer<String> requestHandler = (r) -> this.handleRequest(r);
        this.mqttClient.subscribeWithCallback(Utils.createRequestLink(this.endpoint), requestHandler);

        boolean connected = this.verify(10, 5000);

        if(!connected) {
            var msg = "Could not retrieve all required links as noted in descriptor";
            logger.log(Level.SEVERE, msg, new IllegalStateException());
        }
    }

    /**
     * Verifies that all required links are currently available on the network.  Required links are indicated by the
     * node descriptor.
     *
     * @param attempts Number of times to attempt each GET request.
     * @param timeout Timeout for each of the GET requests.
     * Returns true if all required links are present.  False otherwise.
     * */
    public boolean verify(int attempts, int timeout) {

        // Names of the links to verify.  This list is only used for error-reporting purposes, if one of the required
        // links cannot be obtained.
        List<String> toVerifyNames = requests.stream()
                .filter((x) -> x.isRequired())
                .map((x) -> x.getLink())
                .collect(Collectors.toList());

        // Links from the parsed node descriptor that are tagged as required.  Executes requests for required links in
        // parallel.
        List<Response> toVerify
                = requests.stream().filter((r) -> r.isRequired())
                .parallel()
                .map((LinkRequestDescriptor x) -> this.makeGetRequest(x.getLink(), attempts, timeout))
                .collect(Collectors.toList());

        // Check that all of the required links are associated with a successful GET request.
        for(int i = 0; i < toVerify.size(); i++) {
            if(toVerify.get(i) == null) {
                // Could not GET a required request.  Report an error
                System.err.println(String.format("Could not retrieve request for topic (%s)", toVerifyNames.get(i)));
                //throw new IllegalStateException(String.format();
                return false;
            }
        }

        return true;
    }

    /**
     * Stops all thread-bound tasks associated with this object.
     */
    public void shutdown() {
        this.mqttClient.shutdown();
    }

    /**
     * Makes a vizier-style GET request on a particular link.  GET requests are a coordinated communication between
     * this node and the requested node.  In particular, a JSON-formatted GET request is sent on the link
     * requestEndpoint/link/requests.  Responses are sent on the link requestEndpoint/link/messageId.
     *
     * @param attempts
     * @param timeout
     */
    private Response makeGetRequest(String link, int attempts, int timeout) {
        //TODO should make this into a general request structure, rather than just for GET requests.

        String messageId = Utils.createMessageId();
        String request = Utils.createJsonRequest(messageId, link, "GET");

        String remoteEndpoint = link.split("/")[0];
        String remoteRequestLink = Utils.createRequestLink(remoteEndpoint);
        String remoteResponseLink = Utils.createResponseLink(remoteEndpoint, messageId);

        // Start request-response process.  In particular, the node must subscribe to the response channel before
        // sending the request to ensure that the response is not missed.
        BlockingQueue<String> incomingMessages = this.mqttClient.subscribe(remoteResponseLink);
        String message = null;

        for (int i = 0; i < attempts; i++){
            this.mqttClient.publish(remoteRequestLink, request);
            try {
                message = incomingMessages.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                this.logger.log(Level.INFO, "Could not retrieve request for attempted link.  Reattempting...");
                e.printStackTrace();
            }

            if(message != null) {
                // We successfully retrieved the request, so we can break out of the loop.
                break;
            }
        }

        this.mqttClient.unsubscribe(remoteResponseLink);

        if(message == null) {
            return null;
        }

        // Otherwise, we got the response.  Decode it and return a response.
        return new Gson().fromJson(message, Response.class);
    }

    public void subscribeWithCallback(String topic, Consumer<String> callback) {

        if(this.subscribableLinks.contains(topic)) {
            this.mqttClient.subscribeWithCallback(topic, callback);
        } else {
            this.logger.log(Level.SEVERE, "Cannot subscribe to link (%s) not in subscribable links", topic);
            throw new IllegalStateException();
        }
    }

    public BlockingQueue<String> subscribe(String topic) {

        if(this.subscribableLinks.contains(topic)) {
            return this.mqttClient.subscribe(topic);
        } else {
            this.logger.log(Level.SEVERE, "Cannot subscribe to link (%s) not in subscribable links", topic);
            throw new IllegalStateException();
        }
    }

    public void publish(String topic, String message) {

        if(this.publishableLinks.contains(topic)) {
            this.mqttClient.publish(topic, message);
        } else {
            this.logger.log(Level.SEVERE, "Cannot publish to link (%s) not in publishable links", topic);
            throw new IllegalStateException();
        }
    }

    public void put(String topic, String body) {

        if(this.puttableLinks.contains(topic)) {

            LinkDescriptor current = this.nodeDescriptor.get(topic);
            LinkDescriptor modified = new LinkDescriptor(body, current.getType());

            this.nodeDescriptor.put(topic, modified);
        } else {
            this.logger.log(Level.SEVERE, "Cannot put to link (%s) not in puttable links", topic);
            throw new IllegalStateException();
        }
    }

    public Set<String> getPuttableLinks() {
        return this.puttableLinks;
    }

    public Set<String> getPublishableLinks() {
        return this.publishableLinks;
    }

    public Set<String> getGettableLinks() {
        return this.gettableLinks;
    }

    public Set<String> getSubscribableLinks() {
        return this.subscribableLinks;
    }

    /**
     * @param msg The incoming message from the MQTT client
     */
    private void handleRequest(String msg) {

        //JsonObject jsonMsg = new JsonParser().parse(msg).getAsJsonObject();
        Request request = new Gson().fromJson(msg, Request.class);

        // Verify that message contains required fields
        String id = request.getId();
        String method = request.getMethod();
        String link = request.getLink();

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
        if(!this.nodeDescriptor.containsKey(link)) {
            // Respond with error
            this.logger.log(Level.WARNING, "Received GET request for link (%s) not contained here", link);
            return;
        }

        // Else, the key is in the data that we currently have
        LinkDescriptor ld = this.nodeDescriptor.get(link);
        String responseLink = Utils.createResponseLink(this.endpoint, id);

        String response = Utils.createJsonResponse(ld.getType(), 400, ld.getBody());
        this.mqttClient.publish(responseLink, response);
    }

//    public static void main(String[] args) {

//        try {
//            var f = new FileReader(args[0]);
//            var nodeDescriptor = new Gson().fromJson(f, JsonObject.class);

//            var result = Utils.parseNodeDescriptor(nodeDescriptor);
//            var result2 = Utils.parseNodeDescriptorRequests(nodeDescriptor);

//            var node = new VizierNode("192.168.1.24", 1883, nodeDescriptor);

//            while (true) {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
