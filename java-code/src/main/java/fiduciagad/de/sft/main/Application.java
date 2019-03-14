package fiduciagad.de.sft.main;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class Application {

	public static void main(String[] args) throws MqttSecurityException, MqttException {
		Game game = new Game();

		OpenCVHandler gameDetection = new OpenCVHandler();
		gameDetection.setPythonModule("playedGameDigitizer.py");

		OpenCVHandler colorHandler = new OpenCVHandler();
		colorHandler.setPythonModule("colorGrabber.py");

		ConfiguratorValues.setDefaultColorRangeYellow();

		game.setGameDetection(gameDetection);
		game.setColorHandler(colorHandler);

		game.startTheDetection();
	}

}
