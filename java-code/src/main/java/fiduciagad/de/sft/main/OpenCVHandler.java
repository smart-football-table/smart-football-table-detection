package fiduciagad.de.sft.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

public class OpenCVHandler {

	private Process process;
	private String pythonModule = "";
	private String pythonArguments = "";
	private ProcessBuilder pb;
	private GameManager manager;
	private boolean processAlive = false;

	public void startPythonModule() {

		String path = System.getProperty("user.dir").replace('\\', '/');

		pb = new ProcessBuilder("python", path + "/" + pythonModule, pythonArguments);
		pb.redirectOutput();
		pb.redirectError();
		try {
			process = pb.start();
			processAlive = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void handleWithOpenCVOutput(Controller detector) throws MqttSecurityException, MqttException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		manager = new GameManager();
		String line = null;
		int deleteFirstLine = 0;

		try {
			while ((line = reader.readLine()) != null) {

				if (deleteFirstLine != 0) {
					manager.createBallPosition(line);
					manager.doTheLogic();
				}

				deleteFirstLine++;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setPythonModule(String string) {
		pythonModule = string;
	}

	public void startTheAdjustment() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;

		try {
			while ((line = reader.readLine()) != null) {

				if (line.contains(";")) {
					String[] colorValues = line.split(";");

					ConfiguratorValues.setColorHSVMinH(Integer.parseInt(colorValues[0]));
					ConfiguratorValues.setColorHSVMinS(Integer.parseInt(colorValues[1]));
					ConfiguratorValues.setColorHSVMinV(Integer.parseInt(colorValues[2]));
					ConfiguratorValues.setColorHSVMaxH(Integer.parseInt(colorValues[3]));
					ConfiguratorValues.setColorHSVMaxS(Integer.parseInt(colorValues[4]));
					ConfiguratorValues.setColorHSVMaxV(Integer.parseInt(colorValues[5]));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void setPythonArguments(String pythonArguments) {
		this.pythonArguments = pythonArguments;
	}

	public GameManager getManager() {
		return manager;
	}

	public void setManager(GameManager manager) {
		this.manager = manager;
	}

	public boolean isProcessAlive() {
		return processAlive;
	}

}
