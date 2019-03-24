package fiduciagad.de.sft.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

		pb = new ProcessBuilder("python", "-u", path + "/" + pythonModule, pythonArguments);
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
		String lineBefore = "";
		int deleteFirstLine = 0;
		boolean itsTheSecondOne = false;

		try {
			while ((line = reader.readLine()) != null && detector.isOngoing()) {

				if (deleteFirstLine != 0) {

					if (itsTheSecondOne) {
						manager.createBallPosition(lineBefore, line);
						manager.doTheLogic();
					}
					lineBefore = line;
					itsTheSecondOne = !itsTheSecondOne;
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

	public List<String> startTheAdjustment() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		List<String> result = new ArrayList<String>();

		try {
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
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
