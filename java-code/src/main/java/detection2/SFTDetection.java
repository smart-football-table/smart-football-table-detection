package detection2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;

import detection2.Detectors.GameOverListener;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.AbsolutePosition;
import detection2.data.position.RelativePosition;
import detection2.detector.Detector;

public class SFTDetection {

	public static SFTDetection detectionOn(Table table) {
		return new SFTDetection(table);
	}

	private final Table table;
	private Detectors detectorFactory = new Detectors();
	private Consumer<Message> publisher = m -> {
	};

	private SFTDetection(Table table) {
		this.table = table;
	}

	public SFTDetection publishTo(Consumer<Message> publisher) {
		this.publisher = publisher;
		return this;
	}

	public Detectors getDetectors() {
		return detectorFactory;
	}

	public static interface LineParser {
		RelativePosition parse(String line);
	}

	static class RelativeValueParser implements LineParser {

		@Override
		public RelativePosition parse(String line) {
			String[] values = line.split("\\,");
			if (values.length == 3) {
				Long timestamp = toLong(values[0]);
				Double x = toDouble(values[1]);
				Double y = toDouble(values[2]);

				// TODO test x/y > 1.0?
				if (isValidTimestamp(timestamp) && !isNull(x, y)) {
					if (x == -1 && y == -1) {
						return RelativePosition.noPosition(timestamp);
					} else if (isValidPosition(x, y)) {
						return new RelativePosition(timestamp, x, y);
					}
				}
			}
			return null;
		}

		private static boolean isValidTimestamp(Long timestamp) {
			return timestamp != null && timestamp >= 0;
		}

		private static boolean isValidPosition(Double x, Double y) {
			return x >= 0.0 && y >= 0.0;
		}

		private static boolean isNull(Double x, Double y) {
			return x == null || y == null;

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

	public void process(LineParser lineParser, InputStream is) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			List<Detector> detectors = null;
			GameOverListener gameOverListener = detectorFactory.getGameOverListener();
			String line;
			while ((line = reader.readLine()) != null) {
				if (detectors == null || gameOverListener.isGameover()) {
					detectors = detectorFactory.createNew(publisher);
				}
				RelativePosition relPos = lineParser.parse(line);
				if (relPos == null) {
					// TODO log invalid line
				} else {
					AbsolutePosition absPos = table.toAbsolute(relPos);
					for (Detector detector : detectors) {
						detector.detect(absPos);
					}
				}
			}

		}
	}

}
