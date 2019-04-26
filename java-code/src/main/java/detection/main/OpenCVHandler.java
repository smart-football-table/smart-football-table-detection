package detection.main;

import static java.lang.System.arraycopy;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import detection2.SFTDetection;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.InputStreamPositionProvider;
import detection2.parser.LineParser;
import detection2.parser.RelativeValueParser;

public class OpenCVHandler {

	private String pythonModule = "";
	private String pythonArgumentVideoPath = "empty";
	private String pythonArgumentRecordPath = "empty";
	private String pythonArgumentColor = "0,0,0,0,0,0";
	private String pythonArgumentCamIndex = "0";
	private String pythonArgumentBufferSize = "64";

	public void startPythonModule() throws IOException {
		String path = System.getProperty("user.dir").replace('\\', '/');
		ProcessBuilder pb = new ProcessBuilder("python", "-u", path + "/" + pythonModule, "-v", pythonArgumentVideoPath,
				"-c", pythonArgumentColor, "-i", pythonArgumentCamIndex, "-b", pythonArgumentBufferSize, "-r",
				pythonArgumentRecordPath);
		pb.redirectOutput();
		pb.redirectError();
		pb.start();
	}

	public void handleWithOpenCVOutput() throws MqttSecurityException, MqttException, IOException {
		SFTDetection.detectionOn(new Table(160, 80), mqtt())
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40))
				.process(new InputStreamPositionProvider(new FileInputStream(new File("python_output_opencv.txt")),
						parser()));

	}

	private Consumer<Message> mqtt() {
		// TODO PF
		return m -> {
		};
	}

	private LineParser parser() {
		RelativeValueParser delegate = new RelativeValueParser();
		return new LineParser() {
			@Override
			public RelativePosition parse(String line) {
				String[] values = line.split("\\|");
				if (values.length == 3) {
					String[] secsMillis = values[0].split("\\.");
					Long timestamp = SECONDS.toMillis(toLong(secsMillis[0])) + toLong(fillRight(secsMillis[1], 2));
					Double y = toDouble(values[2]);
					Double x = toDouble(values[1]);
					return delegate
							.parse(timestamp + "," + (x == -1 ? -1 : (x / 765)) + "," + (y == -1 ? -1 : y / 640));
				}
				return null;
			}

			private String fillRight(String string, int len) {
				char[] result = new char[len];
				Arrays.fill(result, '0');
				char[] in = string.toCharArray();
				arraycopy(in, 0, result, 0, in.length);
				return new String(result);
			}

			private Double toDouble(String val) {
				try {
					return Double.valueOf(val);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			private Long toLong(String val) {
				try {
					return Long.valueOf(val);
				} catch (NumberFormatException e) {
					return null;
				}
			}

		};
	}

	public void setPythonModule(String string) {
		pythonModule = string;
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

	public String getPythonArgumentRecordPath() {
		return pythonArgumentRecordPath;
	}

	public void setPythonArgumentRecordPath(String pythonArgumentRecordPath) {
		this.pythonArgumentRecordPath = pythonArgumentRecordPath;
	}

}
