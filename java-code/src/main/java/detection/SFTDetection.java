package detection;

import static detection.detector.FoulDetector.onFoul;
import static detection.detector.GameStartDetector.onGameStart;
import static detection.detector.IdleDetector.onIdle;
import static detection.detector.MovementDetector.onMovement;
import static detection.detector.PositionDetector.onPositionChange;

import java.util.function.Consumer;
import java.util.stream.Stream;

import detection.data.Message;
import detection.data.Table;
import detection.data.position.RelativePosition;
import detection.detector.GoalDetector;

public class SFTDetection {

	private final Table table;
	private Game game;
	private Messages messages;
	private volatile boolean reset;

	public SFTDetection(Table table, Consumer<Message> consumer) {
		this.table = table;
		this.messages = new Messages(consumer);
		this.game = Game.newGame( //
				onGameStart(messages::gameStart), //
				onPositionChange(messages::pos), //
				onMovement(messages::movement), //
				onFoul(messages::foul), //
				onIdle(messages::idle) //
		).addScoreTracker(scoreTracker(messages, consumer));
	}

	public SFTDetection messages(Messages messages) {
		this.messages = messages;
		return this;
	}

	public SFTDetection receiver(MessageProvider provider) {
		provider.addConsumer(m -> {
			if (messages.isReset(m)) {
				resetGame();
			}
		});
		return this;
	}

	private ScoreTracker.Listener scoreTracker(Messages messages, Consumer<Message> consumer) {
		return new ScoreTracker.Listener() {

			@Override
			public void teamScored(int teamid, int score) {
				messages.teamScored(teamid, score);
			}

			@Override
			public void won(int teamid) {
				messages.gameWon(teamid);
			}

			@Override
			public void draw(int[] teamids) {
				messages.gameDraw(teamids);
			}

		};
	}

	public SFTDetection withGoalConfig(GoalDetector.Config goalConfig) {
		this.game = game.withGoalConfig(goalConfig);
		return this;
	}

	public void process(Stream<RelativePosition> positions) {
		positions.forEach(pos -> {
			if (pos == null) {
				// TOOO log invalid line
			} else {
				if (reset) {
					game = game.reset();
					messages.gameStart();
					reset = false;
				}
				game = game.update(table.toAbsolute(pos));
			}
		});
	}

	public void resetGame() {
		this.reset = true;
	}

}
