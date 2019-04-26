package detection2.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import detection2.data.position.RelativePosition;
import detection2.parser.LineParser;

public final class InputStreamPositionProvider implements PositionProvider {

	private final LineParser lineParser;
	private final BufferedReader reader;

	public InputStreamPositionProvider(InputStream is, LineParser lineParser) {
		this.lineParser = lineParser;
		this.reader = new BufferedReader(new InputStreamReader(is));
	}

	@Override
	public RelativePosition next() throws IOException {
		String line = reader.readLine();
		return line == null ? null : parse(lineParser, line);
	}

	private RelativePosition parse(LineParser lineParser, String line) {
		RelativePosition pos = lineParser.parse(line);
		if (pos == null) {
			// TODO log invalid line
		}
		return pos;
	}
}