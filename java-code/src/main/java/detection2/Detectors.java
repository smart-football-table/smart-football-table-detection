package detection2;

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

	private final Listener gameOverListener;
	private Config goalDetectorConfig = new GoalDetector.Config();

	public Detectors(ScoreTracker.Listener gameOverListener) {
		this.gameOverListener = gameOverListener;
	}

	public Config getGoalDetectorConfig() {
		return goalDetectorConfig;
	}

	public void goalDetectorConfig(Config goalDetectorConfig) {
		this.goalDetectorConfig = goalDetectorConfig;
	}

	public List<Detector> createNew(Consumer<Message> pub) {
		ScoreTracker scoreTracker = newScoreTracker(pub);
		return asList( //
				onGameStart(() -> asList(message("game/start", "")).forEach(pub::accept)), //
				onPositionChange(p -> {
					asList( //
							message("ball/position/abs", posPayload(p)), //
							message("ball/position/rel", posPayload(p.getRelativePosition())) //
					).forEach(pub::accept);
				}), //
				onMovement(m -> asList( //
						message("ball/distance/cm", m.distance(CENTIMETER)), //
						message("ball/velocity/mps", m.velocity(MPS)), //
						message("ball/velocity/kmh", m.velocity(KMH) //
						)).forEach(pub::accept)), //
				onGoal(goalDetectorConfig, inform(scoreTracker)), //
				onFoul(() -> asList(message("game/foul", "")).forEach(pub::accept)),
				onIdle((s) -> asList(message("game/idle", Boolean.toString(s))).forEach(pub::accept)));
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

	private ScoreTracker newScoreTracker(Consumer<Message> pub) {
		return new ScoreTracker(multiplexed(new ScoreTracker.Listener() {

			@Override
			public void teamScored(int teamid, int score) {
				asList( //
						message("team/scored", teamid), //
						message("game/score/" + teamid, score) //
				).forEach(pub::accept);
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

		}, gameOverListener));
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
