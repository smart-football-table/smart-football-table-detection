package de.fiduciagad.de.sft.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;

public class OpenCVHandler {

	private Process p;

	public void startPythonModule(String string) {
		ProcessBuilder pb = new ProcessBuilder("python "+string);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		try {
			p = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String readConsole() {

		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String result = builder.toString();

		return "1";
	}

}
