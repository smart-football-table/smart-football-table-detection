package detection.main;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class MainForYoloOpenCV {

	public static void main(String[] args) throws MqttSecurityException, MqttException, IOException {
		Controller detector = new Controller();

		OpenCVHandler gameDetection = new OpenCVHandler();
		buildAndSetPythonArguments(gameDetection);
		gameDetection.setPythonModule("../../../../alexeyab/darknet/darknet_video.py");

		ConfiguratorValues.setMillimeterPerPixel(20);

		ConfiguratorValues.setGameFieldSize(810, 810);

		ConfiguratorValues.setFramesPerSecond(25);

		detector.setGameDetection(gameDetection);

		detector.startTheDetection();
	}

	private static void buildAndSetPythonArguments(OpenCVHandler gameDetection) {
		String videoArgument = "../../../../../Schreibtisch/testvideos/fullField_COM19/com19.avi";
		gameDetection.setPythonArgumentVideoPath(videoArgument);
	}

}
