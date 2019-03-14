package fiduciagad.de.sft.main;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class Application {

	public static void main(String[] args) throws MqttSecurityException, MqttException {
		Detector detector = new Detector();

		OpenCVHandler gameDetection = new OpenCVHandler();
		gameDetection.setPythonModule("playedGameDigitizer.py");

		OpenCVHandler colorHandler = new OpenCVHandler();
		colorHandler.setPythonModule("colorGrabber.py");

		ConfiguratorValues.setDefaultColorRangeYellow();

		detector.setGameDetection(gameDetection);
		detector.setColorGrabber(colorHandler);

		detector.startTheDetection();
	}

}
