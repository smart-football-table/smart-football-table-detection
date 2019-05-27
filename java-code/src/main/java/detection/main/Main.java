package detection.main;

import static detection.data.position.RelativePosition.create;
import static detection.data.unit.DistanceUnit.CENTIMETER;
import static java.util.Collections.addAll;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import detection.SFTDetection;
import detection.data.Distance;
import detection.data.Message;
import detection.data.Table;
import detection.data.position.RelativePosition;
import detection.data.unit.DistanceUnit;
import detection.detector.GoalDetector;
import detection.mqtt.MqttConsumer;
import detection.queue.QueueConsumer;

public class Main {

	private static final Distance TABLE_WIDTH = new Distance(120, CENTIMETER);
	private static final Distance TABLE_HEIGHT = new Distance(68, CENTIMETER);

	private String pythonModule = "/home/nonroot/darknet/darknet_video.py";
//	private String pythonModule = "src/main/resources/python-files/ballDetectorClassicOpenCV.py";

	public static void main(String[] args) throws IOException {

		// runtime configuration stuff, shouldn't be in code
//		String[] arguments = { "-v", "src/main/resources/videos/testVid_ballFromLeftToRight.avi", "-c", "20,100,100,30,255,255" };

		new Main();
	}

	public Main(String... args) throws IOException {
		MqttConsumer mqtt = mqtt("localhost", 1883);
		SFTDetection detection = new SFTDetection(new Table(TABLE_WIDTH, TABLE_HEIGHT),
				new QueueConsumer<Message>(mqtt, 300)).receiver(mqtt)
						.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40));
		detection.process(process(pythonModule, args).map(fromPythonFormat()));
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

	public static Function<String, RelativePosition> fromPythonFormat() {
		return new Function<String, RelativePosition>() {
			@Override
			public RelativePosition apply(String line) {
				String[] values = line.split("\\|");
				return values.length == 3 ? create(toLong(values[0]) * 10, toDouble(values[1]), toDouble(values[2]))
						: null;
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
