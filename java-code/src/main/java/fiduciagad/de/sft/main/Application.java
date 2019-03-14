package fiduciagad.de.sft.main;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class Application {

	public static void main(String[] args) throws MqttSecurityException, MqttException {
		Game game = new Game();

		
		OpenCVHandler opencv = new OpenCVHandler();

		opencv.setPythonModule("playedGameDigitizer.py");
		
		
		game.startTheDetection(opencv);
	}

}
