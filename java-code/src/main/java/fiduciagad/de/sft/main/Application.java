package fiduciagad.de.sft.main;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class Application {

	public static void main(String[] args) throws MqttSecurityException, MqttException {
		Controller detector = new Controller();

		OpenCVHandler gameDetection = new OpenCVHandler();
		gameDetection.setPythonModule("playedGameDigitizerTwoCams.py");

		OpenCVHandler colorHandler = new OpenCVHandler();
		colorHandler.setPythonModule("adjustment.py");

		ConfiguratorValues.setDefaultColorRangeYellow();
		ConfiguratorValues.setMillimeterPerPixel(1);

		detector.setGameDetection(gameDetection);
		detector.setColorGrabber(colorHandler);

		detector.startTheDetection();
	}

}
