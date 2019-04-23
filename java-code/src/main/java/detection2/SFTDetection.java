package detection2;

import static detection2.data.unit.DistanceUnit.CENTIMETER;
import static detection2.data.unit.SpeedUnit.KMH;
import static detection2.data.unit.SpeedUnit.MPS;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.AbsolutePosition;
import detection2.data.position.Position;
import detection2.data.position.RelativePosition;
import detection2.detector.Detector;
import detection2.detector.FoulDetector;
import detection2.detector.GameStartDetector;
import detection2.detector.GoalDetector;
import detection2.detector.GoalDetector.Config;
import detection2.detector.IdleDetector;
import detection2.detector.MovementDetector;
import detection2.detector.PositionDetector;

public class SFTDetection {

	public static SFTDetection detectionOn(Table table) {
		return new SFTDetection(table);
	}

	private final Table table;
	private Consumer<Message> publisher = m -> {
	};
	private GoalDetector.Config goalDetectorConfig = new GoalDetector.Config();

	private SFTDetection(Table table) {
		this.table = table;
	}

	public SFTDetection publishTo(Consumer<Message> publisher) {
		this.publisher = publisher;
		return this;
	}

	public SFTDetection usingGoalDetectorConfig(Config goalDetectorConfig) {
		this.goalDetectorConfig = goalDetectorConfig;
		return this;
	}

	public static interface LineParser {
		RelativePosition parse(String line);
	}

	static class RelativeValueParser implements LineParser {

		@Override
		public RelativePosition parse(String line) {
			String[] values = line.split("\\,");
			if (values.length == 3) {
				Long timestamp = toLong(values[0]);
				Double x = toDouble(values[1]);
				Double y = toDouble(values[2]);

				// TODO test x/y > 1.0?
				if (isValidTimestamp(timestamp) && !isNull(x, y)) {
					if (x == -1 && y == -1) {
						return RelativePosition.noPosition(timestamp);
					} else if (isValidPosition(x, y)) {
						return new RelativePosition(timestamp, x, y);
					}
				}
			}
			return null;
		}

		private static boolean isValidTimestamp(Long timestamp) {
			return timestamp != null && timestamp >= 0;
		}

		private static boolean isValidPosition(Double x, Double y) {
			return x >= 0.0 && y >= 0.0;
		}

		private static boolean isNull(Double x, Double y) {
			return x == null || y == null;

		}

		private static Double toDouble(String val) {
			try {
				return Double.valueOf(val);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		private static Long toLong(String val) {
			try {
				return Long.valueOf(val);
			} catch (NumberFormatException e) {
				return null;
			}
		}

	}

	private static class ScoreTracker {

		public static interface Listener {
			void teamScored(int teamid, int score);

			void won(int teamid);

			void draw(int[] teamids);
		}

		private static final int MAX_BALLS = 10;
		private final Listener listener;

		public ScoreTracker(Listener listener) {
			this.listener = listener;
		}

		private final Map<Integer, Integer> scores = new HashMap<>();

		private int teamScored(int teamid) {
			return changeScore(teamid, +1);
		}

		private int revertGoal(int teamid) {
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

	public void process(LineParser lineParser, InputStream is) throws IOException {
		class GameOverListener implements ScoreTracker.Listener {

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

			public boolean isGameover() {
				return gameover;
			}
		}

		GameOverListener gameOverListener = null;
		List<Detector> detectors = null;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (detectors == null || gameOverListener.isGameover()) {
					detectors = detectors(gameOverListener = new GameOverListener());
				}
				RelativePosition relPos = lineParser.parse(line);
				if (relPos == null) {
					// TODO log invalid line
				} else {
					AbsolutePosition absPos = table.toAbsolute(relPos);
					for (Detector detector : detectors) {
						detector.detect(absPos);
					}
				}
			}

		}
	}

	private List<Detector> detectors(ScoreTracker.Listener gameOverListener) {
		ScoreTracker scoreTracker = new ScoreTracker(multiplexed(new ScoreTracker.Listener() {

			@Override
			public void teamScored(int teamid, int score) {
				asList(new Message("team/scored", teamid), new Message("game/score/" + teamid, score))
						.forEach(publisher::accept);
			}

			@Override
			public void won(int teamid) {
				publisher.accept(new Message("game/gameover", teamid));
			}

			@Override
			public void draw(int[] teamids) {
				publisher.accept(new Message("game/gameover",
						IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
			}

		}, gameOverListener));
		return asList( //
				new GameStartDetector(() -> asList(new Message("game/start", "")).forEach(publisher::accept)), //
				new PositionDetector(new PositionDetector.Listener() {
					@Override
					public void position(AbsolutePosition pos) {
						Position rel = pos.getRelativePosition();
						asList( //
								new Message("ball/position/abs", payload(pos)), //
								new Message("ball/position/rel", payload(rel)) //
						).forEach(publisher::accept);
					}

					private String payload(Position pos) {
						return "{ \"x\":" + pos.getX() + ", \"y\":" + pos.getY() + " }";
					}

				}), //
				new MovementDetector(movement -> asList( //
						new Message("ball/distance/cm", movement.distance(CENTIMETER)), //
						new Message("ball/velocity/mps", movement.velocity(MPS)), //
						new Message("ball/velocity/kmh", movement.velocity(KMH) //
						)).forEach(publisher::accept)), //
				new GoalDetector(goalDetectorConfig, new GoalDetector.Listener() {
					@Override
					public void goal(int teamid) {
						scoreTracker.teamScored(teamid);
					}

					@Override
					public void goalRevert(int teamid) {
						scoreTracker.revertGoal(teamid);
					}
				}), //
				new FoulDetector(() -> asList(new Message("game/foul", "")).forEach(publisher::accept)),
				new IdleDetector(
						(s) -> asList(new Message("game/idle", Boolean.toString(s))).forEach(publisher::accept)));
	}

	private ScoreTracker.Listener multiplexed(ScoreTracker.Listener... listeners) {
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
