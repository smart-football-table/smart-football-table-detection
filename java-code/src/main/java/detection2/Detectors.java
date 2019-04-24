package detection2;

import static detection2.ScoreTracker.onScoreChange;
import static detection2.data.Message.message;
import static detection2.data.unit.DistanceUnit.CENTIMETER;
import static detection2.data.unit.SpeedUnit.KMH;
import static detection2.data.unit.SpeedUnit.MPS;
import static detection2.detector.FoulDetector.onFoul;
import static detection2.detector.GameStartDetector.onGameStart;
import static detection2.detector.GoalDetector.onGoal;
import static detection2.detector.IdleDetector.onIdle;
import static detection2.detector.MovementDetector.onMovement;
import static detection2.detector.PositionDetector.onPositionChange;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection2.ScoreTracker.Listener;
import detection2.data.Message;
import detection2.data.position.Position;
import detection2.detector.Detector;
import detection2.detector.GoalDetector;
import detection2.detector.GoalDetector.Config;

public final class Detectors {

	public interface GameOverListener {
		boolean isGameover();
	}

	private static class GameOverScoreTrackerListener implements ScoreTracker.Listener, GameOverListener {

		private boolean gameover;

		@Override
		public void teamScored(int teamid, int score) {
		}

		@Override
		public void won(int teamid) {
			gameover = true;
		}

		@Override
		public void draw(int[] teamids) {
			gameover = true;
		}

		@Override
		public boolean isGameover() {
			return gameover;
		}

	}

	private final GameOverScoreTrackerListener gameOverListener = new GameOverScoreTrackerListener();
	private Config goalDetectorConfig = new GoalDetector.Config();

	public GameOverListener getGameOverListener() {
		return gameOverListener;
	}

	public void goalDetectorConfig(Config goalDetectorConfig) {
		this.goalDetectorConfig = goalDetectorConfig;
	}

	public List<Detector> createNew(Consumer<Message> pub) {
		gameOverListener.gameover = false;
		ScoreTracker scoreTracker = onScoreChange(multiplexed(publishScoreChanges(pub), gameOverListener));
		return asList( //
				onGameStart(() -> pub.accept(message("game/start", ""))), //
				onPositionChange(p -> {
					pub.accept(message("ball/position/abs", posPayload(p)));
					pub.accept(message("ball/position/rel", posPayload(p.getRelativePosition())));
				}), //
				onMovement(m -> {
					pub.accept(message("ball/distance/cm", m.distance(CENTIMETER)));
					pub.accept(message("ball/velocity/mps", m.velocity(MPS)));
					pub.accept(message("ball/velocity/kmh", m.velocity(KMH)));
				}), //
				onGoal(goalDetectorConfig, inform(scoreTracker)), //
				onFoul(() -> pub.accept(message("game/foul", ""))),
				onIdle(s -> pub.accept(message("game/idle", Boolean.toString(s)))));
	}

	private String posPayload(Position pos) {
		return "{ \"x\":" + pos.getX() + ", \"y\":" + pos.getY() + " }";
	}

	private GoalDetector.Listener inform(ScoreTracker scoreTracker) {
		return new GoalDetector.Listener() {
			@Override
			public void goal(int teamid) {
				scoreTracker.teamScored(teamid);
			}

			@Override
			public void goalRevert(int teamid) {
				scoreTracker.revertGoal(teamid);
			}
		};
	}

	private Listener publishScoreChanges(Consumer<Message> pub) {
		return new ScoreTracker.Listener() {

			@Override
			public void teamScored(int teamid, int score) {
				pub.accept(message("team/scored", teamid));
				pub.accept(message("game/score/" + teamid, score));
			}

			@Override
			public void won(int teamid) {
				pub.accept(message("game/gameover", teamid));
			}

			@Override
			public void draw(int[] teamids) {
				pub.accept(message("game/gameover",
						IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
			}

		};
	}

	private static ScoreTracker.Listener multiplexed(ScoreTracker.Listener... listeners) {
		return new ScoreTracker.Listener() {
			@Override
			public void teamScored(int teamid, int score) {
				for (ScoreTracker.Listener listener : listeners) {
					listener.teamScored(teamid, score);
				}
			}

			@Override
			public void won(int teamid) {
				for (ScoreTracker.Listener listener : listeners) {
					listener.won(teamid);
				}
			}

			@Override
			public void draw(int[] teamids) {
				for (ScoreTracker.Listener listener : listeners) {
					listener.draw(teamids);
				}
			}
		};
	}

}
