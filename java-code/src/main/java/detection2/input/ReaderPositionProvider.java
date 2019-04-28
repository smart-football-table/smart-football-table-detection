package detection2.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import detection2.data.position.RelativePosition;
import detection2.parser.LineParser;

public final class ReaderPositionProvider implements Iterator<RelativePosition> {

	private final LineParser lineParser;
	private final BufferedReader reader;
	private RelativePosition next;

	public ReaderPositionProvider(Reader reader, LineParser lineParser) {
		this.lineParser = lineParser;
		this.reader = new BufferedReader(reader);
		this.next = readNext();
	}

	@Override
	public boolean hasNext() {
		return this.next != null;
	}

	@Override
	public RelativePosition next() {
		RelativePosition tmp = this.next;
		this.next = readNext();
		return tmp;
	}

	private RelativePosition readNext() {
		try {
			String line = reader.readLine();
			return line == null ? null : parse(lineParser, line);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private RelativePosition parse(LineParser lineParser, String line) {
		RelativePosition pos = lineParser.parse(line);
		if (pos == null) {
			// TODO log invalid line
		}
		return pos;
	}

}