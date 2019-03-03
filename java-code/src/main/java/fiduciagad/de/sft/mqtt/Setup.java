package fiduciagad.de.sft.mqtt;

import java.awt.Panel;
import java.io.Closeable;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static java.util.function.Predicate.isEqual;

public class Setup implements Closeable {

	public static class MqttMessage {

		private final String topic;
		private final String payload;

		public MqttMessage(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
		}

		public String getTopic() {
			return topic;
		}

		public String getPayload() {
			return payload;
		}

		public static Predicate<MqttMessage> isTopic(String topic) {
			return matches(topic, MqttMessage::getTopic);
		}

		public static Predicate<MqttMessage> isPayload(String payload) {
			return matches(payload, MqttMessage::getPayload);
		}

		public static <T> Predicate<MqttMessage> matches(T value, Function<MqttMessage, T> f) {
			return m -> isEqual(value).test(f.apply(m));
		}

	}

	private final IMqttClient mqttClient;

	public Setup(String host, int port)
			throws MqttSecurityException, MqttException {
		mqttClient = makeMqttClient(host, port);
		mqttClient.subscribe("#", (topic, message) -> {
			received(new MqttMessage(topic, new String(message.getPayload())));
		});
	}

	private final Map<Predicate<MqttMessage>, Consumer<MqttMessage>> conditions = new HashMap<>();

	private void received(MqttMessage message) {
		for (Entry<Predicate<MqttMessage>, Consumer<MqttMessage>> entry : conditions.entrySet()) {
			if (entry.getKey().test(message)) {
				try {
					entry.getValue().accept(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void whenThen(Predicate<MqttMessage> predicate, Consumer<MqttMessage> consumer) {
		conditions.put(predicate, consumer);
	}

	@Override
	public void close() {
		try {
			this.mqttClient.disconnect();
			this.mqttClient.close();
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private IMqttClient makeMqttClient(String host, int port) throws MqttException, MqttSecurityException {
		IMqttClient client = new MqttClient("tcp://" + host + ":" + port, "theSystemClient", new MemoryPersistence());
		client.connect();
		return client;
	}

}
