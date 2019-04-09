package fiduciagad.de.sft.main;

import java.io.IOException;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.adjustment.AdjustmentController;
import fiduciagad.de.sft.mqtt.MqttSystem;

public class Controller {

	private boolean gameDetectionIsAlive = false;
	private boolean adjustmentWanted = false;

	private OpenCVHandler gameDetectionOpenCV = new OpenCVHandler();
	private OpenCVHandler adjustmentOpenCV = new OpenCVHandler();
	private MqttSystem mqttpub;

	public void startTheDetection() throws MqttSecurityException, MqttException, IOException {

		mqttpub = new MqttSystem("localhost", 1883);

		// SimpleMqttCallBack simpleMqttCallBack = new SimpleMqttCallBack(this);
		// MqttSystem mqttsub = new MqttSystem("localhost", 1883, simpleMqttCallBack);

		startGameDetection();

		while (true) {
			if (adjustmentWanted) {
				adjustmentOpenCV.startPythonModule();

				convertAdjustmentOutputIntoConfigValuesAndFile();
				adjustmentWanted = false;
			}

			if (gameDetectionIsAlive) {

				sendInitialMqttMessages();

				createAndSetPythonArgumentsForGameDetection();

				startThePythonProcess();

				gameDetectionOpenCV.handleWithOpenCVOutput(this);

				stopGameDetection();
			}
			break;
		}
		System.exit(0);
	}

	private void convertAdjustmentOutputIntoConfigValuesAndFile() throws IOException {
		List<String> theOutput = adjustmentOpenCV.startTheAdjustment();
		AdjustmentController.convertOutputIntoValues(theOutput);
		FileController.writeDataIntoFile();
	}

	private void sendInitialMqttMessages() throws MqttPersistenceException, MqttException {
		mqttpub.sendIdle("false");
		mqttpub.sendScore("0-0");
		mqttpub.sendGameStart();
	}

	private void startThePythonProcess() {
		do {
			gameDetectionOpenCV.startPythonModule();
		} while (!gameDetectionOpenCV.isProcessAlive());
	}

	private void createAndSetPythonArgumentsForGameDetection() {
		String pythonArgumentColor = buildPythonArgumentForColor();
		gameDetectionOpenCV.setPythonArguments(pythonArgumentColor);
	}

	private String buildPythonArgumentForColor() {
		String pythonArgument = ConfiguratorValues.getColorHSVMinH() + "," + ConfiguratorValues.getColorHSVMinS() + ","
				+ ConfiguratorValues.getColorHSVMinV() + "," + ConfiguratorValues.getColorHSVMaxH() + ","
				+ ConfiguratorValues.getColorHSVMaxS() + "," + ConfiguratorValues.getColorHSVMaxV();
		return pythonArgument;
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
