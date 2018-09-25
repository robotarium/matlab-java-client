package vizier;

import org.eclipse.paho.client.mqttv3.*;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;
import java.util.function.Function;

public class VizierMqttClient implements MqttCallback {

    private MqttClient client;
    private final String host;
    private final int port;
    private final String id = "java_mqtt_" +  System.currentTimeMillis();

    // For handling messages / callbacks
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

    }

    public void stop() {

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

    public void subscribeWithQueue(String topic, Optional<BlockingQueue<String>> optionalQueue) {

        final BlockingQueue<String> queue;

        if(optionalQueue.isPresent()) {
                queue = optionalQueue.get();
        } else {
            queue = new LinkedBlockingQueue<>();
        }

        Consumer<String> callback = b -> {
            try {
                queue.put(b);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        this.subscribeWithCallback(topic, callback);
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

        var callback = this.callbacks.getOrDefault(s, null);

        if(callback != null) {
            callback.accept(new String(mqttMessage.getPayload()));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //TODO Implement
    }

    public static void main(String[] args) {
       var client = new VizierMqttClient("192.168.1.24", 1883);
       client.subscribeWithCallback("matlab_api/1", (a) -> System.out.println(new String(a)));
    }
}
