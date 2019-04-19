package detection.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;

public class GameManagerTest {

	private String brokerHost = "localhost";
	private int brokerPort;
	private Server server;

	@Before
	public void setup() throws IOException {
		brokerPort = randomPort();
		server = newMqttServer(brokerHost, brokerPort);
	}

	@After
	public void tearDown() {
		server.stopServer();
	}

	private static int randomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	private Server newMqttServer(String host, int port) throws IOException {
		Server server = new Server();
		server.startServer(config(host, port));
		return server;
	}

	private IConfig config(String host, int port) {
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		return new MemoryConfig(properties);
	}

	@Test
	public void canSetScore() throws MqttSecurityException, MqttException {

		GameManager game = new GameManager(brokerHost, brokerPort);

		game.setGoalForTeamWhenGoalHappend("on the right");
		game.setGoalForTeamWhenGoalHappend("on the right");
		game.setGoalForTeamWhenGoalHappend("on the left");

		assertThat(game.getScoreAsString(), is("2-1"));
	}

}
