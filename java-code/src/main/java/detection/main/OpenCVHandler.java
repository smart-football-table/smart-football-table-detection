package detection.main;

import static detection.main.OpenCVHandler.PythonArg.BUFFER_SIZE;
import static detection.main.OpenCVHandler.PythonArg.CAM_INDEX;
import static detection.main.OpenCVHandler.PythonArg.COLOR;
import static detection.main.OpenCVHandler.PythonArg.RECORD_PATH;
import static detection.main.OpenCVHandler.PythonArg.VIDEO_PATH;
import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

	public enum PythonArg {
		VIDEO_PATH("-v"), RECORD_PATH("-r"), COLOR("-c"), CAM_INDEX("-i"), BUFFER_SIZE("-b");
		private String pythonSwitch;

		private PythonArg(String pythonSwitch) {
			this.pythonSwitch = pythonSwitch;
		}
	}

	private Map<PythonArg, String> pythonArgs = new EnumMap<>(PythonArg.class);

	public void startPythonModule(String pythonModule) throws IOException {
		String path = System.getProperty("user.dir").replace('\\', '/');

		List<String> pythonCommand = new ArrayList<>(asList("python", "-u", path + "/" + pythonModule));
		ProcessBuilder pb = new ProcessBuilder(appendArgs(pythonCommand));
		pb.redirectOutput();
		pb.redirectError();
		pb.start();
	}

	private String[] appendArgs(List<String> args) {
		for (Entry<PythonArg, String> entry : pythonArgs.entrySet()) {
			args.addAll(asList(entry.getKey().pythonSwitch, entry.getValue()));
		}
		return args.toArray(new String[args.size()]);
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

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentVideoPath(String value) {
		setPythonArg(VIDEO_PATH, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentColor(String value) {
		setPythonArg(COLOR, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentCamIndex(String value) {
		setPythonArg(CAM_INDEX, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentBufferSize(String value) {
		setPythonArg(BUFFER_SIZE, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentRecordPath(String value) {
		setPythonArg(RECORD_PATH, value);
	}

	private void setPythonArg(PythonArg pythonArg, String value) {
		this.pythonArgs.put(pythonArg, value);
	}

}
