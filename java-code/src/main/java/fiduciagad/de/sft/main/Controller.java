package fiduciagad.de.sft.main;

import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.adjustment.AdjustmentController;
import fiduciagad.de.sft.mqtt.MqttSystem;

public class Controller {

	private boolean detectionIsAlive = false;
	private OpenCVHandler gameDetection = new OpenCVHandler();
	private OpenCVHandler colorGrabber = new OpenCVHandler();

	public void startTheDetection() throws MqttSecurityException, MqttException {

		start();

		colorGrabber.startPythonModule();

		List<String> theOutput = colorGrabber.startTheAdjustment();
		AdjustmentController.convertOutputIntoValues(theOutput);

		String pythonArgument = ConfiguratorValues.getColorHSVMinH() + "," + ConfiguratorValues.getColorHSVMinS() + ","
				+ ConfiguratorValues.getColorHSVMinV() + "," + ConfiguratorValues.getColorHSVMaxH() + ","
				+ ConfiguratorValues.getColorHSVMaxS() + "," + ConfiguratorValues.getColorHSVMaxV();

		MqttSystem mqtt = new MqttSystem("localhost", 1883);

		while (detectionIsAlive) {

			mqtt.sendIdle("false");
			mqtt.sendPostion("0-0");

			gameDetection.setPythonArguments(pythonArgument);
			gameDetection.startPythonModule();
			gameDetection.handleWithOpenCVOutput(this);

			stop();
		}

	}

	public void setGameDetection(OpenCVHandler gameDetection) {
		this.gameDetection = gameDetection;
	}

	public void setColorGrabber(OpenCVHandler colorGrabber) {
		this.colorGrabber = colorGrabber;
	}

	public Boolean isOngoing() {
		return detectionIsAlive;
	}

	public void stop() {
		detectionIsAlive = false;
	}

	public void start() {
		detectionIsAlive = true;
	}

}
