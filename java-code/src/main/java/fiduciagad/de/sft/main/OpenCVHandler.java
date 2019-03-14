package fiduciagad.de.sft.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OpenCVHandler {

	private Process p;
	private String pythonModule = "";
	private String pythonArguments = "";
	private ProcessBuilder pb;

	public void startPythonModule() {

		String path = System.getProperty("user.dir").replace('\\', '/');

		pb = new ProcessBuilder("python", path + "/" + pythonModule, pythonArguments);
		pb.redirectOutput();
		pb.redirectError();
		try {
			p = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public List<String> getOpenCVOutputAsList() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = null;
		List<String> result = new ArrayList<String>();

		try {
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		result.remove(0); // remove first line which is a useless testline from python
		return result;
	}

	public void setPythonModule(String string) {
		pythonModule = string;
	}

	public void startTheAdjustment() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
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

}
