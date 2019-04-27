package detection2;

import static detection2.detector.GoalDetector.onGoal;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import detection2.data.position.AbsolutePosition;
import detection2.detector.Detector;
import detection2.detector.GoalDetector;
import detection2.detector.GoalDetector.Config;

public abstract class Game {

	protected GoalDetector.Config goalDetectorConfig;

	private static class DefaultScoreTracker implements ScoreTracker {

		private static final int MAX_BALLS = 10;

		private final DefaultScoreTracker.Listener listener;

		private DefaultScoreTracker(DefaultScoreTracker.Listener listener) {
			this.listener = listener;
		}

		private final Map<Integer, Integer> scores = new HashMap<>();

		@Override
		public int teamScored(int teamid) {
			return changeScore(teamid, +1);
		}

		@Override
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

	protected Game(Config goalDetectorConfig) {
		this.goalDetectorConfig = goalDetectorConfig;
	}

	public abstract Game update(AbsolutePosition pos);

	public static Game newGame(List<Detector> detectors, DefaultScoreTracker.Listener listener) {
		return new GameoverGame(detectors, new GoalDetector.Config(), listener);
	}

	private static class InGameGame extends Game {

		private static class GameOverScoreState implements DefaultScoreTracker.Listener {

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
		private final List<Detector> detectors;
		private final List<Detector> origDetectors;
		private ScoreTracker.Listener scoreTrackerListener;

		public InGameGame(List<Detector> detectors, Config goalDetectorConfig,
				ScoreTracker.Listener scoreTrackerListener) {
			super(goalDetectorConfig);
			this.origDetectors = detectors;
			this.detectors = addOnGoalDetector(detectors, scoreTrackerListener);
			this.goalDetectorConfig = goalDetectorConfig;
			this.scoreTrackerListener = scoreTrackerListener;
		}

		@Override
		public Game update(AbsolutePosition pos) {
			for (Detector detector : detectors) {
				detector.detect(pos);
			}
			return isGameover() ? new GameoverGame(origDetectors, goalDetectorConfig, scoreTrackerListener) : this;
		}

		private boolean isGameover() {
			return gameOverScoreState.gameover;
		}

		private List<Detector> addOnGoalDetector(List<Detector> detectors,
				DefaultScoreTracker.Listener scoreTrackerListener) {
			List<Detector> result = new ArrayList<>(detectors);
			result.add(onGoal(goalDetectorConfig,
					inform(new DefaultScoreTracker(multiplexed(scoreTrackerListener, gameOverScoreState)))));
			return result;
		}

		private DefaultScoreTracker.Listener multiplexed(ScoreTracker.Listener... listeners) {
			return new DefaultScoreTracker.Listener() {
				@Override
				public void teamScored(int teamid, int score) {
					for (DefaultScoreTracker.Listener listener : listeners) {
						listener.teamScored(teamid, score);
					}
				}

				@Override
				public void won(int teamid) {
					for (DefaultScoreTracker.Listener listener : listeners) {
						listener.won(teamid);
					}
				}

				@Override
				public void draw(int[] teamids) {
					for (DefaultScoreTracker.Listener listener : listeners) {
						listener.draw(teamids);
					}
				}
			};
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

	}

	private static class GameoverGame extends Game {

		private final List<Detector> detectors;
		private final ScoreTracker.Listener scoreTrackerListener;

		public GameoverGame(List<Detector> detectors, Config goalDetectorConfig,
				DefaultScoreTracker.Listener scoreTrackerListener) {
			super(goalDetectorConfig);
			this.detectors = detectors;
			this.scoreTrackerListener = scoreTrackerListener;
		}

		@Override
		public Game update(AbsolutePosition pos) {
			return new InGameGame(detectors.stream().map(Detector::newInstance).collect(toList()), goalDetectorConfig,
					scoreTrackerListener).update(pos);
		}

	}

	public Game withGoalConfig(Config goalDetectorConfig) {
		this.goalDetectorConfig = goalDetectorConfig;
		return this;
	}

}
