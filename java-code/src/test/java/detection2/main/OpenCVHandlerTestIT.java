package detection2.main;

import static detection2.data.Message.message;
import static detection2.data.position.RelativePosition.create;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.Timeout.seconds;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

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

	private Server broker;
	private int brokerPort;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new CopyOnWriteArrayList<>();

	private SFTDetection sut;

	private MqttConsumer mqttConsumer;

	@Rule
	public Timeout timeout = seconds(30);

	@Before
	public void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		broker = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
		mqttConsumer = new MqttConsumer(LOCALHOST, brokerPort);
		sut = new SFTDetection(new Table(120, 68), mqttConsumer) //
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
		client.connect(connectOptions());
		client.setCallback(new MqttCallbackExtended() {

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				messagesReceived.add(message(topic, new String(message.getPayload())));
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				try {
					subscribe(client);
				} catch (MqttException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void connectionLost(Throwable cause) {
			}
		});
		subscribe(client);
		return client;
	}

	private void subscribe(MqttClient client) throws MqttException {
		client.subscribe("#");
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	@Test
	public void canReset() throws IOException, MqttPersistenceException, MqttException, InterruptedException {
		sut.process(positionProvider(1));
		sendReset();
		sut.process(positionProvider(1));
		MILLISECONDS.sleep(50);
		assertThat(messagesReceived.stream().filter(m -> m.getTopic().equals("game/start")).count(), is(2L));
	}

	@Test
	public void doesReconnectAndResubscribe()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		sut.process(positionProvider(1));
		restartBroker();
		waitClientIsReconnected(secondClient);
		waitSutIsReconnected();
		messagesReceived.clear();
		sendReset();
		sut.process(positionProvider(1));
		MILLISECONDS.sleep(50);
		assertThat(messagesReceived.stream().filter(m -> m.getTopic().equals("game/start")).count(), is(1L));
	}

	private void restartBroker() throws IOException {
		broker.stopServer();
		broker = newMqttServer(LOCALHOST, brokerPort);
	}

	private static void waitClientIsReconnected(IMqttClient sutMqttClient) throws InterruptedException {
		while (!sutMqttClient.isConnected()) {
			MILLISECONDS.sleep(500);
		}
	}

	private void waitSutIsReconnected() throws InterruptedException {
		while (!mqttConsumer.isConnected()) {
			MILLISECONDS.sleep(500);
		}
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
