package fiduciagad.de.sft.main;

import java.io.IOException;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.adjustment.AdjustmentController;
import fiduciagad.de.sft.mqtt.MqttSystem;

public class Controller {

	private boolean gameDetectionIsAlive = false;
	private boolean adjustmentWanted = true;

	private OpenCVHandler gameDetectionOpenCV = new OpenCVHandler();
	private OpenCVHandler adjustmentOpenCV = new OpenCVHandler();

	public void startTheDetection() throws MqttSecurityException, MqttException, IOException {

		MqttSystem mqttpub = new MqttSystem("localhost", 1883);

		// SimpleMqttCallBack simpleMqttCallBack = new SimpleMqttCallBack(this);
		// MqttSystem mqttsub = new MqttSystem("localhost", 1883, simpleMqttCallBack);

		startGameDetection();

		while (true) {
			if (adjustmentWanted) {
				adjustmentOpenCV.startPythonModule();

				List<String> theOutput = adjustmentOpenCV.startTheAdjustment();
				AdjustmentController.convertOutputIntoValues(theOutput);
				FileController.writeDataIntoFile();
				adjustmentWanted = false;
			}

			if (gameDetectionIsAlive) {

				mqttpub.sendIdle("false");
				mqttpub.sendPostion("0-0");

				String pythonArgument = ConfiguratorValues.getColorHSVMinH() + ","
						+ ConfiguratorValues.getColorHSVMinS() + "," + ConfiguratorValues.getColorHSVMinV() + ","
						+ ConfiguratorValues.getColorHSVMaxH() + "," + ConfiguratorValues.getColorHSVMaxS() + ","
						+ ConfiguratorValues.getColorHSVMaxV();
				gameDetectionOpenCV.setPythonArguments(pythonArgument);
				gameDetectionOpenCV.startPythonModule();
				gameDetectionOpenCV.handleWithOpenCVOutput(this);

				stopGameDetection();
			}
			break;
		}
		System.exit(0);
	}

	public void setGameDetection(OpenCVHandler gameDetection) {
		this.gameDetectionOpenCV = gameDetection;
	}

	public void setColorGrabber(OpenCVHandler colorGrabber) {
		this.adjustmentOpenCV = colorGrabber;
	}

	public Boolean isOngoing() {
		return gameDetectionIsAlive;
	}

	public void stopGameDetection() {
		gameDetectionIsAlive = false;
	}

	public void startGameDetection() {
		gameDetectionIsAlive = true;
	}

}
