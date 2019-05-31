package detection.main;

import static detection.data.Message.message;
import static detection.data.position.RelativePosition.create;
import static detection.data.position.RelativePosition.noPosition;
import static detection.data.unit.DistanceUnit.CENTIMETER;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import detection.SFTDetection;
import detection.data.Message;
import detection.data.Table;
import detection.data.position.RelativePosition;
import detection.detector.GoalDetector;
import detection.mqtt.MqttConsumer;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;

class SFTDetectionIT {

	private static Duration timeout = ofSeconds(30);

	private static final String LOCALHOST = "localhost";

	private Server broker;
	private int brokerPort;
	private IMqttClient secondClient;
	private List<Message> messagesReceived = new CopyOnWriteArrayList<>();

	private SFTDetection sut;

	private MqttConsumer mqttConsumer;

	@BeforeEach
	void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		broker = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
		mqttConsumer = new MqttConsumer(LOCALHOST, brokerPort);
		sut = new SFTDetection(new Table(120, 68, CENTIMETER), mqttConsumer) //
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
	void onResetTheNewGameIsStartedImmediatelyAndWithoutTableInteraction()
			throws IOException, MqttPersistenceException, MqttException, InterruptedException {
		assertTimeoutPreemptively(timeout, () -> {
			sut.process(positions(42));
			messagesReceived.clear();
			sendReset();
			sut.process(provider(3, () -> noPosition(currentTimeMillis())));
			MILLISECONDS.sleep(50);
			assertThat(messagesWithTopic("game/start").count(), is(1L));
		});
	}

	@Test
	void doesReconnectAndResubscribe()
			throws IOException, InterruptedException, MqttPersistenceException, MqttException {
		assertTimeoutPreemptively(timeout, () -> {
			sut.process(positions(42));
			restartBroker();
			waitUntil(secondClient, IMqttClient::isConnected);
			waitUntil(mqttConsumer, MqttConsumer::isConnected);
			messagesReceived.clear();
			sendReset();
			sut.process(provider(3, () -> noPosition(currentTimeMillis())));
			MILLISECONDS.sleep(50);
			assertThat(messagesWithTopic("game/start").count(), is(1L));
		});
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return messagesReceived.stream().filter(m -> m.getTopic().equals(topic));
	}

	private void restartBroker() throws IOException {
		broker.stopServer();
		broker = newMqttServer(LOCALHOST, brokerPort);
	}

	private static <T> void waitUntil(T object, Predicate<T> predicate) throws InterruptedException {
		while (!predicate.test(object)) {
			MILLISECONDS.sleep(500);
		}
	}

	private void sendReset() throws MqttException, MqttPersistenceException {
		secondClient.publish("game/reset", new MqttMessage("".getBytes()));
	}

	private Stream<RelativePosition> positions(int count) {
		return provider(count, () -> create(currentTimeMillis(), 0.2, 0.3));
	}

	private Stream<RelativePosition> provider(int count, Supplier<RelativePosition> supplier) {
		return range(0, count).peek(i -> sleep()).mapToObj(i -> supplier.get());
	}

	private void sleep() {
		try {
			MILLISECONDS.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
