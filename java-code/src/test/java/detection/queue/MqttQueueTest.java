package detection.queue;

import static detection.data.Message.message;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import detection.data.Message;
import detection.mqtt.MqttConsumer;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

class MqttQueueTest {

	private static final String LOCALHOST = "localhost";

	private int brokerPort;
	private Server server;
	private MqttConsumer mqttConsumer;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new ArrayList<>();

	@BeforeEach
	void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		server = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "second-client-for-test");
		mqttConsumer = new MqttConsumer(LOCALHOST, brokerPort);
	}

	@AfterEach
	void tearDown() throws MqttException, IOException {
		secondClient.disconnect();
		secondClient.close();
		mqttConsumer.close();
		server.stopServer();
	}

	private int randomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	private Server newMqttServer(String host, int port) throws IOException {
		Server server = new Server();
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		server.startServer(new MemoryConfig(properties));
		return server;
	}

	private MqttClient newMqttClient(String host, int port, String id) throws MqttException, MqttSecurityException {
		MqttClient client = new MqttClient("tcp://" + host + ":" + port, id, new MemoryPersistence());
		client.connect();
		client.setCallback(new MqttCallback() {

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				messagesReceived.add(message(topic, new String(message.getPayload())));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub

			}

			@Override
			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub

			}
		});
		client.subscribe("#");
		return client;
	}

	@Test
	void sendSomeMessages() throws InterruptedException {
		List<Message> messages = asList(message("topic1", "payload1"), message("topic2", "payload2"));
		messages.forEach(mqttConsumer::accept);
		MILLISECONDS.sleep(10);
		assertThat(messagesReceived, is(messages));
	}

}
