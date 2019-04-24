package detection2;

import static detection2.Detectors.ScoreTracker.onScoreChange;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection2.data.Message;
import detection2.data.position.Position;
import detection2.detector.Detector;
import detection2.detector.GoalDetector;
import detection2.detector.GoalDetector.Config;

public final class Detectors {

	static class ScoreTracker {

		public static interface Listener {
			void teamScored(int teamid, int score);

			void won(int teamid);

			void draw(int[] teamids);
		}

		private static final int MAX_BALLS = 10;

		public static ScoreTracker onScoreChange(ScoreTracker.Listener listener) {
			return new ScoreTracker(listener);
		}

		private final ScoreTracker.Listener listener;

		private ScoreTracker(ScoreTracker.Listener listener) {
			this.listener = listener;
		}

		private final Map<Integer, Integer> scores = new HashMap<>();

		public int teamScored(int teamid) {
			return changeScore(teamid, +1);
		}

		public int revertGoal(int teamid) {
			return changeScore(teamid, -1);
		}

		private int changeScore(int teamid, int d) {
			Integer newScore = score(teamid) + d;
			scores.put(teamid, newScore);
			listener.teamScored(teamid, newScore);
			checkState(teamid, newScore);
			return newScore;
		}

		private void checkState(int teamid, Integer newScore) {
			if (isWinningGoal(newScore)) {
				listener.won(teamid);
			} else if (isDraw()) {
				listener.draw(teamids());
			}
		}

		private Integer score(int teamid) {
			return scores.getOrDefault(teamid, 0);
		}

		private boolean isWinningGoal(int score) {
			return score > ((double) MAX_BALLS) / 2;
		}

		private boolean isDraw() {
			return scores().sum() == MAX_BALLS;
		}

		private IntStream scores() {
			return scores.values().stream().mapToInt(Integer::intValue);
		}

		private int[] teamids() {
			return scores.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
		}

	}

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

	private List<Detector> detectors;

	public List<Detector> detectors(Consumer<Message> publisher) {
		if (detectors == null || gameOverListener.isGameover()) {
			gameOverListener.gameover = false;
			detectors = create(publisher);
		}
		return detectors;
	}

	private List<Detector> create(Consumer<Message> pub) {
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

	private ScoreTracker.Listener publishScoreChanges(Consumer<Message> pub) {
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
