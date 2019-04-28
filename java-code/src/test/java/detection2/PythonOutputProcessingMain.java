package detection2;

import static detection2.data.position.RelativePosition.create;
import static java.lang.System.arraycopy;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.parser.LineParser;
import detection2.parser.RelativeValueParser;

public class PythonOutputProcessingMain {

	private static class AbsValueParser implements LineParser {

		public RelativePosition parse(String line) {
			String[] values = line.split("\\|");
			if (values.length == 3) {
				String[] secsMillis = values[0].split("\\.");
				Long timestamp = SECONDS.toMillis(toLong(secsMillis[0])) + toLong(fillRight(secsMillis[1], 2));
				Double y = toDouble(values[2]);
				Double x = toDouble(values[1]);
				return create(timestamp, x, y);
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

		private static Double toDouble(String val) {
			try {
				return Double.valueOf(val);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		private static Long toLong(String val) {
			try {
				return Long.valueOf(val);
			} catch (NumberFormatException e) {
				return null;
			}
		}

	}

	public static void main(String[] args) throws IOException {
		Consumer<Message> sysout = System.out::println;
		// Consumer<Message> publisher = t -> {
		// if (t.getTopic().contains("score")) {
		// sysout.accept(t);
		// }
		// };

		AbsValueParser parser = new AbsValueParser();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File("python_output_opencv.txt"))))) {
			Function<String, RelativePosition> mapper = parser::parse;
			LineParser delegate = new RelativeValueParser();
			new SFTDetection(new Table(120, 68), sysout)
					.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40)).process(reader.lines()
							.map(mapper.andThen(PythonOutputProcessingMain::toString).andThen(delegate::parse)));
		}
	}

	private static String toString(RelativePosition p) {
		return p.getTimestamp() + "," + (p.getX() == -1 ? -1 : (p.getX() / 765)) + ","
				+ (p.getY() == -1 ? -1 : p.getY() / 640);
	}

}
