package detection2;

import static detection2.Messages.IncomingMessages.isReset;
import static detection2.Messages.OutgoingMessages.publihGameWon;
import static detection2.Messages.OutgoingMessages.publishFoul;
import static detection2.Messages.OutgoingMessages.publishGameDraw;
import static detection2.Messages.OutgoingMessages.publishGameStart;
import static detection2.Messages.OutgoingMessages.publishMovement;
import static detection2.Messages.OutgoingMessages.publishPos;
import static detection2.Messages.OutgoingMessages.publishTeamScored;
import static detection2.Messages.OutgoingMessages.pusblishIdle;
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

	public static SFTDetection detectionOn(Table table, Consumer<Message> consumer) {
		return new SFTDetection(table, consumer);
	}

	private final Table table;
	private Game game;
	private volatile boolean reset;

	private SFTDetection(Table table, Consumer<Message> consumer) {
		this.table = table;
		this.game = Game.newGame( //
				onGameStart(() -> publishGameStart(consumer)), //
				onPositionChange(p -> publishPos(consumer, p)), //
				onMovement(m -> publishMovement(consumer, m)), //
				onFoul(() -> publishFoul(consumer)), //
				onIdle(b -> pusblishIdle(consumer, b)) //
		).addScoreTracker(scoreTracker(consumer));
	}

	public SFTDetection receiver(MessageProvider provider) {
		provider.addConsumer(m -> {
			if (isReset(m)) {
				resetGame();
			}
		});
		return this;
	}

	private ScoreTracker.Listener scoreTracker(Consumer<Message> consumer) {
		return new ScoreTracker.Listener() {

			@Override
			public void teamScored(int teamid, int score) {
				publishTeamScored(consumer, teamid, score);
			}

			@Override
			public void won(int teamid) {
				publihGameWon(consumer, teamid);
			}

			@Override
			public void draw(int[] teamids) {
				publishGameDraw(consumer, teamids);
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
