package detection2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.parser.LineParser;

public class SFTDetection {

	public static SFTDetection detectionOn(Table table, Consumer<Message> pub) {
		return new SFTDetection(table, pub);
	}

	private final Table table;
	private Game game;

	private SFTDetection(Table table, Consumer<Message> publisher) {
		this.table = table;
		this.game = new Game(publisher);
	}

	@Deprecated
	public Game getGame() {
		return game;
	}

	public void process(LineParser lineParser, InputStream is) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				RelativePosition relPos = lineParser.parse(line);
				if (relPos == null) {
					// TODO log invalid line
				} else {
					game = game.update(table.toAbsolute(relPos));
				}
			}

		}
	}

}
