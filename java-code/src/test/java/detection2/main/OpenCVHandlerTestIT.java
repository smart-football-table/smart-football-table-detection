package detection2.main;

import static detection2.data.Message.message;
import static detection2.data.position.RelativePosition.create;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Before;
import org.junit.Test;

import detection2.SFTDetection;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.PositionProvider;
import detection2.mqtt.MqttConsumer;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

public class OpenCVHandlerTestIT {

	private static final String LOCALHOST = "localhost";

	private int brokerPort;
	private Server server;
	private MqttConsumer mqttConsumer;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new ArrayList<>();

	@Before
	public void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		server = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
		mqttConsumer = new MqttConsumer(LOCALHOST, brokerPort);
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
				System.out.println(topic + " " + message);
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
	public void canReset() throws IOException {
		SFTDetection.detectionOn(new Table(120, 68), new MqttConsumer(LOCALHOST, brokerPort))
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40)).process(positionProvider());
	}

	private PositionProvider positionProvider() {
		return new PositionProvider() {
			private int i;

			@Override
			public RelativePosition next() throws IOException {
				if (i++ > 1_000) {
					return null;
				}
				try {
					MILLISECONDS.sleep(1000 / 30);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return create(currentTimeMillis(), 0.2, 0.3);
			}
		};
	}

}
