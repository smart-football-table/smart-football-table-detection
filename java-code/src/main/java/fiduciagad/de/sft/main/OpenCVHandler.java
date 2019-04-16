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
	private String pythonArgumentVideoPath = "empty";
	private String pythonArgumentColor = "0,0,0,0,0,0";
	private String pythonArgumentCamIndex = "0";
	private String pythonArgumentBufferSize = "64";
	private ProcessBuilder pb;
	private GameManager manager;
	private boolean processAlive = false;

	public void startPythonModule() {

		String path = System.getProperty("user.dir").replace('\\', '/');

		pb = new ProcessBuilder("python", "-u", path + "/" + pythonModule, "-v", pythonArgumentVideoPath, "-c",
				pythonArgumentColor, "-i", pythonArgumentCamIndex, "-b", pythonArgumentBufferSize);
		pb.redirectOutput();
		pb.redirectError();

		try {
			process = pb.start();
			processAlive = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void handleWithOpenCVOutput(Controller detector) throws MqttSecurityException, MqttException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		manager = new GameManager();
		String line = null;
		boolean itsTheSecondOne = false;

		while ((line = reader.readLine()) != null || detector.isOngoing() || processAlive) {

			if (line != null && line.contains("|")) {

				try {

					if (itsTheSecondOne) {
						manager.createBallPosition(line);
						manager.doTheLogic();
					}
					itsTheSecondOne = !itsTheSecondOne;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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

	public String getPythonArgumentVideoPath() {
		return pythonArgumentVideoPath;
	}

	public void setPythonArgumentVideoPath(String pythonArgumentVideoPath) {
		this.pythonArgumentVideoPath = pythonArgumentVideoPath;
	}

	public String getPythonArgumentColor() {
		return pythonArgumentColor;
	}

	public void setPythonArgumentColor(String pythonArgumentColor) {
		this.pythonArgumentColor = pythonArgumentColor;
	}

	public String getPythonArgumentCamIndex() {
		return pythonArgumentCamIndex;
	}

	public void setPythonArgumentCamIndex(String pythonArgumentCamIndex) {
		this.pythonArgumentCamIndex = pythonArgumentCamIndex;
	}

	public String getPythonArgumentBufferSize() {
		return pythonArgumentBufferSize;
	}

	public void setPythonArgumentBufferSize(String pythonArgumentBufferSize) {
		this.pythonArgumentBufferSize = pythonArgumentBufferSize;
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
