package detection.mqtt;

import java.io.Closeable;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import detection.main.BallPosition;

public class MqttSystem implements Closeable {

	private final IMqttClient mqttClient;

	public MqttSystem(String host, int port) throws MqttSecurityException, MqttException {
		mqttClient = new MqttClient("tcp://" + host + ":" + port, "SetupClient", new MemoryPersistence());
		mqttClient.connect();

	}

	public MqttSystem(String host, int port, MqttCallback mqttCallback) throws MqttException {
		mqttClient = new MqttClient("tcp://" + host + ":" + port, "SetupClient", new MemoryPersistence());
		mqttClient.connect();
		mqttClient.setCallback(mqttCallback);
		mqttClient.subscribe("game/reset");
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

	public void sendScore(String string) throws MqttPersistenceException, MqttException {

		String[] scores = string.split("-");
		String score0 = scores[0];
		String score1 = scores[1];

		mqttClient.publish("game/score/team/0", score0.getBytes(), 0, true);
		mqttClient.publish("game/score/team/1", score1.getBytes(), 0, true);

	}

	public void sendFoul() throws MqttPersistenceException, MqttException {

		String empty = "";
		mqttClient.publish("game/foul", empty.getBytes(), 0, false);

	}

	public void sendGameOver(String string) throws MqttPersistenceException, MqttException {

		String finalString = "{\"winners\":[" + string + "]}";

		if (string.contains(",")) {
			String[] strings = string.split(",");
			finalString = "{\"winners\":[" + strings[0] + "," + strings[1] + "]}";
		}

		mqttClient.publish("game/gameover", finalString.getBytes(), 0, false);

	}

	public void sendPostion(BallPosition ballPosition) throws MqttPersistenceException, MqttException {

		String finalString = "{\"x\":" + ballPosition.normedXPosition() + ",\"y\":" + ballPosition.normedYPosition()
				+ "}";

		mqttClient.publish("ball/position", finalString.getBytes(), 0, false);

	}

	public void sendVelocity(double velocity) throws MqttPersistenceException, MqttException {

		mqttClient.publish("ball/velocity/kmh", String.valueOf(velocity).getBytes(), 0, false);

	}

	public void sendGameStart() throws MqttPersistenceException, MqttException {
		String empty = "";
		mqttClient.publish("game/start", empty.getBytes(), 0, false);

	}

	public void sendTeamThatScored(int i) throws MqttPersistenceException, MqttException {
		String empty = "" + i;
		mqttClient.publish("team/scored", empty.getBytes(), 0, false);

	}

	public void sendVelocityMS(double velocityms) throws MqttPersistenceException, MqttException {
		mqttClient.publish("ball/velocity/ms", String.valueOf(velocityms).getBytes(), 0, false);

	}

}
