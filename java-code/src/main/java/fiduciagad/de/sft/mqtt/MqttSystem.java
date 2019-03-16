package fiduciagad.de.sft.mqtt;

import java.io.Closeable;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import fiduciagad.de.sft.main.BallPosition;

public class MqttSystem implements Closeable {

	private final IMqttClient mqttClient;

	public MqttSystem(String host, int port) throws MqttSecurityException, MqttException {
		mqttClient = makeMqttClient(host, port);

	}

	public void close() {
		try {
			this.mqttClient.disconnect();
			this.mqttClient.close();
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private IMqttClient makeMqttClient(String host, int port) throws MqttException, MqttSecurityException {
		IMqttClient client = new MqttClient("tcp://" + host + ":" + port, "SetupClient", new MemoryPersistence());
		client.connect();
		return client;
	}

	public void sendIdle(String string) throws MqttPersistenceException, MqttException {

		String finalString = "{\"idle\":" + string + "}";
		mqttClient.publish("game/idle", finalString.getBytes(), 0, false);

	}

	public void sendPostion(String string) throws MqttPersistenceException, MqttException {

		String finalString = "{\"score\":[" + string.split("-")[0] + "," + string.split("-")[1] + "]}";

		mqttClient.publish("game/score", finalString.getBytes(), 0, false);

	}

	public void sendFoul() throws MqttPersistenceException, MqttException {

		String empty = "";
		mqttClient.publish("game/foul", empty.getBytes(), 0, false);

	}

	public void sendGameOver(String string) throws MqttPersistenceException, MqttException {

		String finalString = "{\"winners\":[" + string + "]}";

		mqttClient.publish("game/gameover", finalString.getBytes(), 0, false);

	}

	public void sendPostion(BallPosition ballPosition) throws MqttPersistenceException, MqttException {

		String finalString = "{\"x\":" + ballPosition.getXCoordinate() + ",\"y\":" + ballPosition.getYCoordinate()
				+ "}";

		mqttClient.publish("game/position", finalString.getBytes(), 0, false);

	}

}
