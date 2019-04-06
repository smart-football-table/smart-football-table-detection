package fiduciagad.de.sft.main;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class Application {

	public static void main(String[] args) throws MqttSecurityException, MqttException, IOException {
		Controller detector = new Controller();

		OpenCVHandler gameDetection = new OpenCVHandler();
		gameDetection.setPythonModule("../../../../alexeyab/darknet/darknet_video.py");
		
		OpenCVHandler colorHandler = new OpenCVHandler();
		colorHandler.setPythonModule("adjustment.py");

		FileController.loadDataFromFile();
		
		ConfiguratorValues.setMillimeterPerPixel(20);
		ConfiguratorValues.setGameFieldSize(200, 200);
		
		detector.setGameDetection(gameDetection);
		detector.setColorGrabber(colorHandler);

		detector.startTheDetection();
	}

}
