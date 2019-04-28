package detection2;

import static detection2.detector.FoulDetector.onFoul;
import static detection2.detector.GameStartDetector.onGameStart;
import static detection2.detector.IdleDetector.onIdle;
import static detection2.detector.MovementDetector.onMovement;
import static detection2.detector.PositionDetector.onPositionChange;

import java.io.IOException;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.PositionProvider;

public class SFTDetection {

	private final Table table;
	private Game game;
	private Messages messages;
	private volatile boolean reset;

	public SFTDetection(Table table, Consumer<Message> consumer) {
		this.table = table;
		this.messages = new Messages(consumer);
		this.game = Game.newGame( //
				onGameStart(() -> messages.gameStart()), //
				onPositionChange(p -> messages.pos(p)), //
				onMovement(m -> messages.movement(m)), //
				onFoul(() -> messages.foul()), //
				onIdle(b -> messages.idle(b)) //
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
				messages.publishTeamScored(teamid, score);
			}

			@Override
			public void won(int teamid) {
				messages.publihGameWon(teamid);
			}

			@Override
			public void draw(int[] teamids) {
				messages.publishGameDraw(teamids);
			}

		};
	}

	public SFTDetection withGoalConfig(GoalDetector.Config goalConfig) {
		this.game = game.withGoalConfig(goalConfig);
		return this;
	}

	public void process(PositionProvider positionProvider) throws IOException {
		RelativePosition pos;
		while ((pos = positionProvider.next()) != null) {
			if (reset) {
				game = game.reset();
				reset = false;
			}
			if (pos.isNull() || !pos.isNull()) {
				game = game.update(table.toAbsolute(pos));
			}
		}
	}

	public void resetGame() {
		this.reset = true;
	}

}
