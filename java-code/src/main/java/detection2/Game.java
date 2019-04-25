package detection2;

import static detection2.Game.ScoreTracker.onScoreChange;
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import detection2.data.Message;
import detection2.data.position.AbsolutePosition;
import detection2.data.position.Position;
import detection2.detector.Detector;
import detection2.detector.GoalDetector;
import detection2.detector.GoalDetector.Config;

public class Game {

	private static class DetectorSuppliers {

		private final List<Supplier<Detector>> detectors = new ArrayList<>();

		public DetectorSuppliers add(Supplier<Detector> detector) {
			this.detectors.add(detector);
			return this;
		}

		public List<Detector> build() {
			return detectors.stream().map(Supplier::get).collect(toList());
		}

	}

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

	private static class GameOverScoreState implements ScoreTracker.Listener {

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

	}

	private final GameOverScoreState gameOverScoreState = new GameOverScoreState();
	private final Consumer<Message> publisher;
	private Config goalDetectorConfig;

	private final List<Detector> detectors;

	public Game(Consumer<Message> pub) {
		this(pub, new GoalDetector.Config());
	}

	public Game(Consumer<Message> publisher, Config goalDetectorConfig) {
		this.publisher = publisher;
		this.goalDetectorConfig = goalDetectorConfig;
		this.detectors = create();
	}

	public Game withGoalConfig(Config goalConfig) {
		return new Game(publisher, goalConfig);
	}

	public Game update(AbsolutePosition pos) {
		for (Detector detector : detectors) {
			detector.detect(pos);
		}
		return isGameover() ? new Game(publisher, goalDetectorConfig) : this;
	}

	private boolean isGameover() {
		return gameOverScoreState.gameover;
	}

	private List<Detector> create() {
		ScoreTracker scoreTracker = onScoreChange(multiplexed(publishScoreChanges(publisher), gameOverScoreState));
		return new DetectorSuppliers() //
				.add(() -> onGameStart(() -> publisher.accept(message("game/start", "")))) //
				.add(() -> onPositionChange(p -> {
					publisher.accept(message("ball/position/abs", posPayload(p)));
					publisher.accept(message("ball/position/rel", posPayload(p.getRelativePosition())));
				})) //
				.add(() -> onMovement(m -> {
					publisher.accept(message("ball/distance/cm", m.distance(CENTIMETER)));
					publisher.accept(message("ball/velocity/mps", m.velocity(MPS)));
					publisher.accept(message("ball/velocity/kmh", m.velocity(KMH)));
				})) //
				.add(() -> onGoal(goalDetectorConfig, inform(scoreTracker))) //
				.add(() -> onFoul(() -> publisher.accept(message("game/foul", "")))) //
				.add(() -> onIdle(s -> publisher.accept(message("game/idle", Boolean.toString(s))))) //
				.build();
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
