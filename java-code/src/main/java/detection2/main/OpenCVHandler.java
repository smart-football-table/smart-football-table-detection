package detection2.main;

import static detection2.data.position.RelativePosition.create;
import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	private static final String PYTHON_MODULE = "src/main/resources/ballDetectorClassicOpenCV.py";

	private List<String> pythonArgs;

	private SFTDetection detection;

	public OpenCVHandler(Consumer<Message> consumer) throws IOException {
		this.detection = new SFTDetection(new Table(120, 68), consumer)
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
	}

	public OpenCVHandler withReceiver(MessageProvider receiver) {
		detection = detection.receiver(receiver);
		return this;
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
		this.detection.process(stream);
	}

	private String pythonModule() {
		return PYTHON_MODULE;
	}

	private Process startProcess(String... pythonCommand) throws IOException {
		List<String> args = new ArrayList<>(asList(pythonCommand));
		args.addAll(pythonArgs);
		return new ProcessBuilder(args).start();
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

	public OpenCVHandler withPythonArgs(String... values) {
		this.pythonArgs = Arrays.asList(values);
		return this;
	}

}
