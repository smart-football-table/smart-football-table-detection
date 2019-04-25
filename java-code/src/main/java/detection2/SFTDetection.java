package detection2;

import java.io.IOException;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.PositionProvider;

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

	public SFTDetection withGoalConfig(GoalDetector.Config goalConfig) {
		this.game = game.withGoalConfig(goalConfig);
		return this;
	}

	public void process(PositionProvider positionProvider) throws IOException {
		RelativePosition relPos;
		while ((relPos = positionProvider.next()) != null) {
			game = game.update(table.toAbsolute(relPos));
		}
	}

}
