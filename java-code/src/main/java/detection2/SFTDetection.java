package detection2;

import static detection2.detector.FoulDetector.onFoul;
import static detection2.detector.GameStartDetector.onGameStart;
import static detection2.detector.IdleDetector.onIdle;
import static detection2.detector.MovementDetector.onMovement;
import static detection2.detector.PositionDetector.onPositionChange;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.Detector;
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
		MessageSender sender = new MessageSender(publisher);
		this.game = Game.newGame(detectors(sender)).withScoreTracker(scoreTracker(sender));
	}

	private ScoreTracker.Listener scoreTracker(MessageSender sender) {
		return new ScoreTracker.Listener() {

			@Override
			public void teamScored(int teamid, int score) {
				sender.teamScored(teamid, score);
			}

			@Override
			public void won(int teamid) {
				sender.gameWon(teamid);
			}

			@Override
			public void draw(int[] teamids) {
				sender.draw(teamids);
			}

		};
	}

	private List<Detector> detectors(MessageSender s) {
		return asList( //
				onGameStart(() -> s.gameStart()), //
				onPositionChange(p -> s.pos(p)), //
				onMovement(m -> s.movement(m)), //
				onFoul(() -> s.foul()), //
				onIdle(b -> s.idle(b)) //
		);
	}

	public SFTDetection withGoalConfig(GoalDetector.Config goalConfig) {
		this.game = game.withGoalConfig(goalConfig);
		return this;
	}

	public void process(PositionProvider positionProvider) throws IOException {
		RelativePosition pos;
		while ((pos = positionProvider.next()) != null) {
			if (pos.isNull() || !pos.isNull()) {
				game = game.update(table.toAbsolute(pos));
			}
		}
	}

}
