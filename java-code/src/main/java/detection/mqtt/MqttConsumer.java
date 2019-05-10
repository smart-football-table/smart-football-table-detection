package detection.mqtt;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import detection.MessageProvider;
import detection.data.Message;

public class MqttConsumer implements Consumer<Message>, MessageProvider, Closeable {

	private final MqttClient mqttClient;
	private final List<Consumer<Message>> consumers = new CopyOnWriteArrayList<>();

	public MqttConsumer(String host, int port) throws IOException {
		try {
			mqttClient = new MqttClient("tcp://" + host + ":" + port, "SFT-Detection", new MemoryPersistence());
			mqttClient.setCallback(callback());
			mqttClient.connect(connectOptions());
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}

	private MqttCallback callback() {
		return new MqttCallbackExtended() {

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				for (Consumer<Message> consumer : consumers) {
					consumer.accept(Message.message(topic, new String(message.getPayload())));
				}
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void connectionLost(Throwable cause) {
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				try {
					subscribe();
				} catch (MqttException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	private void subscribe() throws MqttException {
		mqttClient.subscribe("#");
	}

	@Override
	public void accept(Message m) {
		try {
			mqttClient.publish(m.getTopic(), new MqttMessage(m.getPayload().getBytes()));
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			if (mqttClient.isConnected()) {
				mqttClient.disconnect();
			}
			mqttClient.close();
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}

	public boolean isConnected() {
		return mqttClient.isConnected();
	}

	@Override
	public void addConsumer(Consumer<Message> consumer) {
		consumers.add(consumer);
	}

}