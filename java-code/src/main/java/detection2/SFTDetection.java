package detection2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.AbsolutePosition;
import detection2.data.position.RelativePosition;
import detection2.detector.Detector;
import detection2.parser.LineParser;

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

	public void process(LineParser lineParser, InputStream is) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				RelativePosition relPos = lineParser.parse(line);
				if (relPos == null) {
					// TODO log invalid line
				} else {
					AbsolutePosition absPos = table.toAbsolute(relPos);
					for (Detector detector : detectorFactory.detectors(publisher)) {
						detector.detect(absPos);
					}
				}
			}

		}
	}

}
