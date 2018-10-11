package vizier;

import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VizierMqttClient implements MqttCallback {

    private static final int MAX_OUTGOING = 100;
    private MqttClient client;
    private final String host;
    private final int port;
    private final String id = "java_mqtt_" +  System.currentTimeMillis();
    private final Logger logger = Logger.getGlobal();

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    // Set up publishing thread
    private final LinkedBlockingQueue<MessagePair> toPublish = new LinkedBlockingQueue<>();
    Runnable r = () -> {
        while(true) {
            MessagePair msg = null;

            if(toPublish.size() >= VizierMqttClient.MAX_OUTGOING) {
                this.logger.log(Level.SEVERE, "Too many message in outgoing queue!");
            }

            try {
                msg = toPublish.take();
            } catch (InterruptedException e) {
                this.logger.log(Level.WARNING, "Publish task interrupted.");
                e.printStackTrace();
                // Continue in the loop
                continue;
            }

            if (msg == null) {
                this.logger.log(Level.INFO, "Got null message. Terminating publish task");
                return;
            }

            // Else, if we have a good message
            try {
                this.client.publish(msg.topic, msg.message.getBytes(), 0, false);
            } catch (MqttException e) {
                this.logger.log(Level.WARNING, "Could not publish MQTT message.");
                e.printStackTrace();
            }
        }
    };
    private final Future<?>  taskFuture = executor.submit(r);

    // Contains callbacks for particular topics.
    private final ConcurrentHashMap<String, Consumer<String>> callbacks = new ConcurrentHashMap<>();

    public VizierMqttClient(String host, int port) {

        this.host = host;
        this.port = port;

        String uri = "tcp://" + this.host + ":" + this.port;

        try {
            this.client = new MqttClient(uri, id);
        } catch (MqttException e) {
            String msg = String.format("Could not instantiate MQTT client to host (%s) at port (%i)", host, port);
            this.logger.log(Level.SEVERE, msg, e);
            e.printStackTrace();
            throw new IllegalStateException();
        }

        this.client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);

        if(client != null) {
            try {
                this.client.connect(options);
            } catch (MqttException e) {
                String msg = String.format("Could not connect to broker on host (%s) port (%d)", this.host, this.port);
                this.logger.log(Level.SEVERE, msg);
                e.printStackTrace();
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Shutdown the MQTT client, ending all background threads.
     *
     * Disconnects the MQTT client, shutdown the executor, and stops the publishing thread.
     */
    public void shutdown() {
        boolean failed = true;
        try {
            this.client.disconnect(1000);
        } catch (MqttException e) {
            e.printStackTrace();
            this.logger.log(Level.WARNING, "Could not disconnect MQTT client.");
            failed = false;
        }

        if(failed) {
            // Else, we couldn't disconnect from the server.
            try {
                this.client.disconnectForcibly(1000);
            } catch (MqttException e) {
                this.logger.log(Level.SEVERE, "Could not forcibly disconnect MQTT client.");
                e.printStackTrace();
            }
        }

        // Putting null in this queue should cause the task to cease
        try {
            this.toPublish.put(null);
        } catch (InterruptedException e) {
            this.logger.log(Level.SEVERE, "Interrupted while cancelling publish task");
            e.printStackTrace();
        }
        // Cancel the future that we're running
        this.taskFuture.cancel(true);
        // Finally, shutdown the executor
        this.executor.shutdown();
    }

    public void publish(final String topic, final String message) {
        try {
            this.toPublish.put(new MessagePair(topic, message));
        } catch (InterruptedException e) {
            this.logger.log(Level.WARNING, "Interrupted");
            e.printStackTrace();
        }
    }

    public void subscribeWithCallback(String topic, Consumer<String> callback) {
        // TODO Make concurrent!!! Add synchronized to callbacks?
        try {
            this.client.subscribe(topic);
            this.callbacks.put(topic, callback);
        } catch (MqttException e) {
            this.logger.log(Level.SEVERE, "Could not subscribe to topic.");
            e.printStackTrace();
        }
    }

    public BlockingQueue<String> subscribe(String topic) {
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
        //TODO Make concurrent!! Add synchronized?
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
        Consumer<String> callback = this.callbacks.getOrDefault(s, null);

        if(callback != null) {
            callback.accept(new String(mqttMessage.getPayload()));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //TODO Not used for now.
    }

    private class MessagePair {

        public final String topic;
        public final String message;

        public MessagePair(String topic, String message) {
            this.topic = topic;
            this.message = message;
        }
    }
}
