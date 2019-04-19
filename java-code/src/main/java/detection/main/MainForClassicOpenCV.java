package detection.main;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class MainForClassicOpenCV {

	public static void main(String[] args) throws MqttSecurityException, MqttException, IOException {
		Controller detector = new Controller();

		ConfiguratorValues.setMillimeterPerPixel(20);

		ConfiguratorValues.setGameFieldSize(800, 600);

		ConfiguratorValues.setDefaultColorRangeYellow();
		// FileController.loadDataFromFile();

		OpenCVHandler gameDetection = new OpenCVHandler();

		buildAndSetPythonArguments(gameDetection);

		gameDetection.setPythonModule("src/main/resources/python-files/playedGameDigitizer.py");

		OpenCVHandler colorHandler = new OpenCVHandler();
		colorHandler.setPythonModule("adjustment.py");

		detector.setGameDetection(gameDetection);
		detector.setColorGrabber(colorHandler);

		detector.startTheDetection();
	}

	private static void buildAndSetPythonArguments(OpenCVHandler gameDetection) {
		String videoArgument = "../../../../../Schreibtisch/testvideos/fullField_COM19/com19.avi";
		gameDetection.setPythonArgumentVideoPath(videoArgument);
		String pythonArgumentColor = buildPythonArgumentForColor();
		gameDetection.setPythonArgumentColor(pythonArgumentColor);
		
	}

	private static String buildPythonArgumentForColor() {
		String pythonArgumentColor = ConfiguratorValues.getColorHSVMinH() + "," + ConfiguratorValues.getColorHSVMinS()
				+ "," + ConfiguratorValues.getColorHSVMinV() + "," + ConfiguratorValues.getColorHSVMaxH() + ","
				+ ConfiguratorValues.getColorHSVMaxS() + "," + ConfiguratorValues.getColorHSVMaxV();
		return pythonArgumentColor;
	}

}
