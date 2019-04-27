package detection2.main;

import static detection2.data.Message.message;
import static detection2.data.position.RelativePosition.create;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.lang.System.currentTimeMillis;
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
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
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
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new ArrayList<>();

	private SFTDetection sut;

	@Before
	public void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
		MqttConsumer mqttConsumer = new MqttConsumer(LOCALHOST, brokerPort);
		sut = SFTDetection.detectionOn(new Table(120, 68), mqttConsumer) //
				.receiver(mqttConsumer) //
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));

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
			}

			@Override
			public void connectionLost(Throwable cause) {
			}

		});
		client.subscribe("#");
		return client;
	}

	@Test
	public void canReset() throws IOException, MqttPersistenceException, MqttException, InterruptedException {
		sut.process(positionProvider(1));
		sendReset();
		sut.process(positionProvider(1));
		assertThat(messagesReceived.stream().filter(m -> m.getTopic().equals("game/start")).count(), is(2L));
	}

	private void sendReset() throws MqttException, MqttPersistenceException {
		secondClient.publish("game/reset", new MqttMessage("".getBytes()));
	}

	private PositionProvider positionProvider(int count) {
		return new PositionProvider() {
			private int i;

			@Override
			public RelativePosition next() throws IOException {
				if (i++ > count) {
					return null;
				}
				try {
					MILLISECONDS.sleep(1000 / 30);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return rand();
			}

			private RelativePosition rand() {
				return create(currentTimeMillis(), 0.2, 0.3);
			}
		};
	}

}
