package detection2.mqtt;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import detection2.data.Message;

public class MqttConsumer implements Consumer<Message>, Closeable {

	private final MqttClient mqttClient;

	public MqttConsumer(String host, int port) throws IOException {
		try {
			mqttClient = new MqttClient("tcp://" + host + ":" + port, "SFT-Detection", new MemoryPersistence());
			mqttClient.connect();
		} catch (MqttException e) {
			throw new IOException(e);
		}
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

}