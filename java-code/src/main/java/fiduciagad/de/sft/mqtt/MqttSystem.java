package fiduciagad.de.sft.mqtt;

import java.io.Closeable;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.SimpleMqttCallBack;

public class MqttSystem implements Closeable {

	private final IMqttClient mqttClient;

	public MqttSystem(String host, int port) throws MqttSecurityException, MqttException {
		mqttClient = new MqttClient("tcp://" + host + ":" + port, "SetupClient", new MemoryPersistence());
		mqttClient.connect();

	}

	public MqttSystem(String host, int port, SimpleMqttCallBack simpleMqttCallBack) throws MqttException {
		mqttClient = new MqttClient("tcp://" + host + ":" + port, "SetupClient", new MemoryPersistence());
		mqttClient.setCallback(simpleMqttCallBack);
		mqttClient.connect();
	}

	public void close() {
		try {
			this.mqttClient.disconnect();
			this.mqttClient.close();
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
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

		String finalString = "{\"x\":" + ballPosition.normedXPosition() + ",\"y\":" + ballPosition.normedYPosition()
				+ "}";

		mqttClient.publish("game/ball/position", finalString.getBytes(), 0, false);

	}

	public void sendVelocity(double velocity) throws MqttPersistenceException, MqttException {

		String finalString = "{\"velocity\":[" + velocity + "]}";
		mqttClient.publish("game/ball/velocity", finalString.getBytes(), 0, false);

	}

}
