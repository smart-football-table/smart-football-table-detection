package detection2.main;

import static detection2.data.position.RelativePosition.create;
import static detection2.main.OpenCVHandler.PythonArg.BUFFER_SIZE;
import static detection2.main.OpenCVHandler.PythonArg.CAM_INDEX;
import static detection2.main.OpenCVHandler.PythonArg.COLOR;
import static detection2.main.OpenCVHandler.PythonArg.RECORD_PATH;
import static detection2.main.OpenCVHandler.PythonArg.VIDEO_PATH;
import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import detection2.MessageProvider;
import detection2.SFTDetection;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;

public class OpenCVHandler {

	public enum PythonArg {
		VIDEO_PATH("-v"), RECORD_PATH("-r"), COLOR("-c"), CAM_INDEX("-i"), BUFFER_SIZE("-b");
		private String pythonSwitch;

		private PythonArg(String pythonSwitch) {
			this.pythonSwitch = pythonSwitch;
		}
	}

	private static final String PYTHON_MODULE = "darknet_video.py";

	private final Map<PythonArg, String> pythonArgs = new EnumMap<>(PythonArg.class);

	private final Consumer<Message> consumer;
	private final MessageProvider receiver;

	public OpenCVHandler(Consumer<Message> consumer, MessageProvider receiver) throws IOException {
		this.consumer = consumer;
		this.receiver = receiver;
	}

	public void startPythonModule() throws IOException {
		runDetection(startProcess("python", "-u", pythonModule()));
	}

	private void runDetection(Process process) throws IOException {
		runDetection(process.getInputStream());
	}

	private void runDetection(InputStream is) throws IOException {
		Stream<RelativePosition> stream = new BufferedReader(new InputStreamReader(is)).lines()
				.map(oldPythonFormatParser());
		SFTDetection detection = new SFTDetection(new Table(120, 68), consumer)
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
		detection = receiver == null ? detection : detection.receiver(receiver);
		detection.process(stream);
	}

	private String pythonModule() {
		// TODO do not depend on user home
		return System.getProperty("user.dir") + "/" + PYTHON_MODULE;
	}

	private Process startProcess(String... pythonCommand) throws IOException {
		return new ProcessBuilder(appendArgs(new ArrayList<>(asList(pythonCommand)))).start();
	}

	private String[] appendArgs(List<String> args) {
		for (Entry<PythonArg, String> entry : pythonArgs.entrySet()) {
			args.addAll(asList(entry.getKey().pythonSwitch, entry.getValue()));
		}
		return args.toArray(new String[args.size()]);
	}

	public static Function<String, RelativePosition> oldPythonFormatParser() {
		return new Function<String, RelativePosition>() {
			@Override
			public RelativePosition apply(String line) {
				String[] values = line.split("\\|");
				if (values.length == 3) {
					String[] secsMillis = values[0].split("\\.");
					Long timestamp = SECONDS.toMillis(toLong(secsMillis[0])) + toLong(fillRight(secsMillis[1], 2));
					Double y = toDouble(values[2]);
					Double x = toDouble(values[1]);
					return create(timestamp, x == -1 ? -1 : x / 765, y == -1 ? -1 : y / 640);
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
		addPythonArg(VIDEO_PATH, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentColor(String value) {
		addPythonArg(COLOR, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentCamIndex(String value) {
		addPythonArg(CAM_INDEX, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentBufferSize(String value) {
		addPythonArg(BUFFER_SIZE, value);
	}

	@Deprecated // use setPythonArg directly
	public void setPythonArgumentRecordPath(String value) {
		addPythonArg(RECORD_PATH, value);
	}

	public OpenCVHandler addPythonArg(PythonArg pythonArg, String value) {
		this.pythonArgs.put(pythonArg, value);
		return this;
	}

}
