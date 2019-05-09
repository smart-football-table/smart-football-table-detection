package detection.mqtt;

import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

public class MqttSystemTest {

	private static final String LOCALHOST = "localhost";

	private int brokerPort;

	private Server server;
	private MqttSystem setup;
	private IMqttClient secondClient;

	@Before
	public void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		server = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
		setup = new MqttSystem(LOCALHOST, brokerPort);
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
		return client;
	}

	@Ignore
	@Test
	public void sendIdleMessage() throws MqttSecurityException, MqttException {

		MqttSystem mqtt = new MqttSystem(LOCALHOST, brokerPort);

		mqtt.sendIdle("true");

		// TODO:

		// assertThat(there is idle, matcher);

	}

	@Ignore
	@Test
	public void sendScoreMessage() throws MqttSecurityException, MqttException {

		MqttSystem mqtt = new MqttSystem(LOCALHOST, brokerPort);

		mqtt.sendScore("1-0");

		// TODO:

		// assertThat(there is score, matcher);

	}

	@After
	public void tearDown() throws MqttException {
		secondClient.disconnect();
		secondClient.close();
		setup.close();
		server.stopServer();
	}

}
