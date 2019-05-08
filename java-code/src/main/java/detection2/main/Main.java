package detection2.main;

import static detection2.data.position.RelativePosition.create;
import static java.lang.System.arraycopy;
import static java.util.Collections.addAll;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import detection2.SFTDetection;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.mqtt.MqttConsumer;
import detection2.queue.QueueConsumer;

public class Main {

	private String pythonModule = "src/main/resources/ballDetectorClassicOpenCV.py";

	public static void main(String[] args) throws IOException {
		new Main(args);
	}

	public Main(String... args) throws IOException {
		MqttConsumer mqtt = mqtt("localhost", 1883);
		SFTDetection detection = new SFTDetection(new Table(120, 68), new QueueConsumer<Message>(mqtt, 300))
				.receiver(mqtt).withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
		detection.process(process(pythonModule, args).map(oldPythonFormatParser()));
	}

	protected static Stream<String> process(String module, String... args) throws IOException {
		return stream(new ProcessBuilder(args(module, args)).start().getInputStream());
	}

	private static List<String> args(String module, String... args) {
		List<String> result = new ArrayList<>(Arrays.asList("python", "-u", module));
		addAll(result, args);
		return result;
	}

	private static Stream<String> stream(InputStream is) {
		return new BufferedReader(new InputStreamReader(is)).lines();
	}

	private MqttConsumer mqtt(String host, int port) throws IOException {
		return new MqttConsumer(host, port);
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
}
