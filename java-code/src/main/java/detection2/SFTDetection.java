package detection2;

import static detection2.detector.FoulDetector.onFoul;
import static detection2.detector.GameStartDetector.onGameStart;
import static detection2.detector.IdleDetector.onIdle;
import static detection2.detector.MovementDetector.onMovement;
import static detection2.detector.PositionDetector.onPositionChange;

import java.io.IOException;
import java.util.function.Consumer;

import javax.management.RuntimeErrorException;

import org.eclipse.paho.client.mqttv3.MqttException;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.PositionProvider;
import detection2.mqtt.MqttConsumer;

public class SFTDetection {

	public static SFTDetection detectionOn(Table table, Consumer<Message> pub) {
		return new SFTDetection(table, pub);
	}

	private final Table table;
	private Game game;
	private boolean reset;

	private SFTDetection(Table table, Consumer<Message> consumer) {
		if (consumer instanceof MqttConsumer) {
			MqttConsumer mqttConsumer = (MqttConsumer) consumer;
			try {
				mqttConsumer.setCallback(m -> {
					if (m.getTopic().equals("game/reset")) {
						resetGame();
					}
				});
			} catch (MqttException e) {
				throw new RuntimeException(e);
			}
		}

		this.table = table;
		MessagePublisher publisher = new MessagePublisher(consumer);
		this.game = Game.newGame( //
				onGameStart(() -> publisher.gameStart()), //
				onPositionChange(p -> publisher.pos(p)), //
				onMovement(m -> publisher.movement(m)), //
				onFoul(() -> publisher.foul()), //
				onIdle(b -> publisher.idle(b)) //
		).addScoreTracker(scoreTracker(publisher));
	}

	private ScoreTracker.Listener scoreTracker(MessagePublisher sender) {
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
