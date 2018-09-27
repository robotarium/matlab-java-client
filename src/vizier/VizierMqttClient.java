package vizier;

import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VizierMqttClient implements MqttCallback {

    private MqttClient client;
    private final String host;
    private final int port;
    private final String id = "java_mqtt_" +  System.currentTimeMillis();

    // Contains callbacks for particular topics.
    private final ConcurrentHashMap<String, Consumer<String>> callbacks = new ConcurrentHashMap<>();

    public VizierMqttClient(String host, int port) {

        this.host = host;
        this.port = port;

        String uri = "tcp://" + this.host + ":" + this.port;

        try {
            this.client = new MqttClient(uri, id);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        this.client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);

        if(client != null) {
            try {
                this.client.connect(options);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        // TODO Really should move connect in here
    }

    public void stop() {
        try {
            this.client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {

        var mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(0);

        try {
            this.client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            System.err.println("Unable to publish MQTT message");
            e.printStackTrace();
        }
    }

    public void subscribeWithCallback(String topic, Consumer<String> callback) {

        try {
            this.client.subscribe(topic);
            this.callbacks.put(topic, callback);
        } catch (MqttException e) {
            System.err.println("Could not subscribe to topic");
            e.printStackTrace();
        }
    }

    public BlockingQueue<String> subscribeWithQueue(String topic) {

        final BlockingQueue<String> queue;
        queue = new LinkedBlockingQueue<>();

        Consumer<String> callback = (b) -> {
            try {
                queue.put(b);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        this.subscribeWithCallback(topic, callback);

        return queue;
    }

    public void unsubscribe(String topic) {

        try {
            this.client.unsubscribe(topic);
            this.callbacks.remove(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
       //TODO implement.  Resubscribe to everything?
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

        // This series of operations should be thread safe, because the callback are contained in a concurrent
        // structure.  One the callback has been obtained, it doesn't matter if the link is subsequently unsubscribed.
        var callback = this.callbacks.getOrDefault(s, null);

        if(callback != null) {
            callback.accept(new String(mqttMessage.getPayload()));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //TODO Not used for now.
    }
}
