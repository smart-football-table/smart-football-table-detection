package detection2;

import static java.lang.System.arraycopy;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.ReaderPositionProvider;
import detection2.parser.LineParser;
import detection2.parser.RelativeValueParser;

public class PythonOutputProcessingMain {

	private static RelativeValueParser delegate = new RelativeValueParser();

	private static class AbsValueParser implements LineParser {

		@Override
		public RelativePosition parse(String line) {
			String[] values = line.split("\\|");
			if (values.length == 3) {
				String[] secsMillis = values[0].split("\\.");
				Long timestamp = SECONDS.toMillis(toLong(secsMillis[0])) + toLong(fillRight(secsMillis[1], 2));
				Double y = toDouble(values[2]);
				Double x = toDouble(values[1]);
				return delegate.parse(timestamp + "," + (x == -1 ? -1 : (x / 765)) + "," + (y == -1 ? -1 : y / 640));
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

		SFTDetection.detectionOn(new Table(120, 68), sysout)
				.withGoalConfig(new GoalDetector.Config().frontOfGoalPercentage(40))
				.process(new ReaderPositionProvider(
						new InputStreamReader(new FileInputStream(new File("python_output_opencv.txt"))),
						new AbsValueParser()));
	}

}
