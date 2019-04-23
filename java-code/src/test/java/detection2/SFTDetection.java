package detection2;

import static detection2.SFTDetection.DistanceUnit.CENTIMETER;
import static detection2.SFTDetection.SpeedUnit.KMH;
import static detection2.SFTDetection.SpeedUnit.MPS;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection2.SFTDetection.GoalDetector.Config;

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

	private static interface Detector {
		void detect(AbsolutePosition pos);
	}

	private static class MovementDetector implements Detector {

		private final Listener listener;

		public static interface Listener {
			void movement(Movement movement);
		}

		public MovementDetector(Listener listener) {
			this.listener = listener;
		}

		private AbsolutePosition prevPos;

		@Override
		public void detect(AbsolutePosition pos) {
			RelativePosition relPos = pos.getRelativePosition();
			if (!relPos.isNull()) {
				if (prevPos != null) {
					listener.movement(new Movement(prevPos, pos));
				}
				prevPos = pos;
			}
		}

	}

	private static class GameStartDetector implements Detector {

		public static interface Listener {
			void gameStarted();
		}

		private final Listener listener;

		public GameStartDetector(Listener listener) {
			this.listener = listener;
		}

		private boolean gameStartSend;

		@Override
		public void detect(AbsolutePosition pos) {
			if (!gameStartSend && !pos.getRelativePosition().isNull()) {
				gameStartSend = true;
				listener.gameStarted();
			}
		}

	}

	private static class PositionDetector implements Detector {

		private final Listener listener;

		public static interface Listener {
			void position(AbsolutePosition pos);
		}

		public PositionDetector(Listener listener) {
			this.listener = listener;
		}

		@Override
		public void detect(AbsolutePosition pos) {
			if (!pos.getRelativePosition().isNull()) {
				listener.position(pos);
			}
		}

	}

	public static class GoalDetector implements Detector {

		public static class Config {

			private long timeWithoutBallTilGoalMillisDuration = 2;
			private TimeUnit timeWithoutBallTilGoalMillisTimeUnit = SECONDS;
			private int frontOfGoalPercentage = 40;

			/**
			 * Sets where the ball has to been detected before the ball has been gone.
			 * 
			 * @param frontOfGoalPercentage 100% the whole playfield, 50% one side.
			 * @return this config
			 */
			public Config frontOfGoalPercentage(int frontOfGoalPercentage) {
				this.frontOfGoalPercentage = frontOfGoalPercentage;
				return this;
			}

			public Config timeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
				this.timeWithoutBallTilGoalMillisDuration = duration;
				this.timeWithoutBallTilGoalMillisTimeUnit = timeUnit;
				return this;
			}

			public int getFrontOfGoalPercentage() {
				return frontOfGoalPercentage;
			}

			public long getGoalTimeout(TimeUnit timeUnit) {
				return timeUnit.convert(timeWithoutBallTilGoalMillisDuration, timeWithoutBallTilGoalMillisTimeUnit);
			}

		}

		private final double frontOfGoal;
		private final long millisTilGoal;
		private State state = new WaitForBallOnMiddleLine();
		private final Listener listener;

		public static interface Listener {
			void goal(int teamid);
		}

		public GoalDetector(Config config, Listener listener) {
			this.frontOfGoal = 1 - ((double) config.getFrontOfGoalPercentage()) / 100;
			this.millisTilGoal = config.getGoalTimeout(MILLISECONDS);
			this.listener = listener;
		}

		private static interface State {
			State update(AbsolutePosition pos);
		}

		private class WaitForBallOnMiddleLine implements State {

			private final double midAreasPercent = 5D;
			private final double midAreaMax = 0.5 + midAreasPercent / 100;

			@Override
			public State update(AbsolutePosition pos) {
				return ballAtMiddleLine(pos) ? new BallOnTable().update(pos) : this;
			}

			private boolean ballAtMiddleLine(AbsolutePosition pos) {
				return pos.getRelativePosition().normalizeX().getX() <= midAreaMax;
			}

		}

		private class BallOnTable implements State {

			@Override
			public State update(AbsolutePosition pos) {
				RelativePosition relPos = pos.getRelativePosition();
				return isFrontOfGoal(relPos) ? new FrontOfGoal(relPos.isRightHandSide() ? 0 : 1) : this;
			}

			private boolean isFrontOfGoal(RelativePosition relPos) {
				return relPos.normalizeX().getX() >= frontOfGoal;
			}
		}

		private class FrontOfGoal implements State {

			private final int teamid;

			public FrontOfGoal(int teamid) {
				this.teamid = teamid;
			}

			@Override
			public State update(AbsolutePosition pos) {
				return pos.isNull() ? //
						new PossibleGoal(pos.getTimestamp(), teamid).update(pos) : //
						new BallOnTable().update(pos);
			}
		}

		private class PossibleGoal implements State {

			private final long timestamp;
			private final int teamid;

			public PossibleGoal(long timestamp, int teamid) {
				this.timestamp = timestamp;
				this.teamid = teamid;
			}

			@Override
			public State update(AbsolutePosition pos) {
				if (pos.isNull()) {
					return waitTimeElapsed(pos) ? new Goal(teamid) : this;
				}
				return new BallOnTable().update(pos);
			}

			private boolean waitTimeElapsed(AbsolutePosition pos) {
				return pos.getTimestamp() - timestamp >= millisTilGoal;
			}

		}

		private class Goal implements State {

			private final int teamid;

			public Goal(int teamid) {
				this.teamid = teamid;
			}

			@Override
			public State update(AbsolutePosition pos) {
				return new WaitForBallOnMiddleLine().update(pos);
			}

			public int getTeamid() {
				return teamid;
			}

		}

		@Override
		public void detect(AbsolutePosition pos) {
			state = state.update(pos);
			if (state instanceof Goal) {
				listener.goal(((Goal) state).getTeamid());
			}
		}

	}

	public static class IdleDetector implements Detector {

		private final long idleWhen = MINUTES.toMillis(1);

		public static interface Listener {
			void idle(boolean state);
		}

		private final Listener listener;
		private AbsolutePosition offTableSince;
		private boolean idle;

		public IdleDetector(Listener listener) {
			this.listener = listener;
		}

		@Override
		public void detect(AbsolutePosition pos) {
			if (offTable(pos)) {
				if (offTableSince == null) {
					offTableSince = pos;
				} else {
					long diff = pos.getTimestamp() - offTableSince.getTimestamp();
					if (diff >= idleWhen) {
						if (!idle) {
							listener.idle(true);
							idle = true;
						}
					}
				}
			} else {
				if (idle) {
					listener.idle(false);
					idle = false;
				}
			}
		}

		private boolean offTable(AbsolutePosition pos) {
			boolean offTable = pos.isNull();
			return offTable;
		}

	}

	public static class FoulDetector implements Detector {

		public static interface Listener {
			void foulHappenend();
		}

		private static final long TIMEOUT = SECONDS.toMillis(15);
		private static final double MOVEMENT_GREATER_THAN = 0.05;

		private final Listener listener;

		private RelativePosition noMovementSince;
		private boolean foulInProgress;

		public FoulDetector(Listener listener) {
			this.listener = listener;
		}

		@Override
		public void detect(AbsolutePosition pos) {
			if (noMovementSince != null && xChanged(pos)) {
				noMovementSince = null;
				foulInProgress = false;
			} else {
				if (noMovementSince == null) {
					noMovementSince = pos.getRelativePosition().normalizeX();
				} else if (noMovementDurationInMillis(pos) >= TIMEOUT) {
					if (!foulInProgress) {
						listener.foulHappenend();
					}
					foulInProgress = true;
				}
			}
		}

		private long noMovementDurationInMillis(AbsolutePosition pos) {
			return pos.getTimestamp() - noMovementSince.getTimestamp();
		}

		private boolean xChanged(AbsolutePosition pos) {
			return pos.isNull() || xDiff(pos) > MOVEMENT_GREATER_THAN;
		}

		private double xDiff(AbsolutePosition pos) {
			return pos.getRelativePosition().normalizeX().getX() - noMovementSince.getX();
		}

	}

	static enum DistanceUnit {
		CENTIMETER {
			@Override
			public double toCentimeter(double value) {
				return value;
			}

			@Override
			protected double convert(double value, DistanceUnit target) {
				return toCentimeter(value);
			}
		};

		public abstract double toCentimeter(double value);

		protected abstract double convert(double value, DistanceUnit target);
	}

	private static class Distance {

		private final double value;
		private final DistanceUnit distanceUnit;

		public Distance(double value, DistanceUnit distanceUnit) {
			this.value = value;
			this.distanceUnit = distanceUnit;
		}

		public double value(DistanceUnit target) {
			return distanceUnit.convert(value, target);
		}

	}

	enum SpeedUnit {
		MPS {
			@Override
			public double toMps(double value) {
				return value;
			}
		},
		KMH {
			@Override
			public double toMps(double value) {
				return value * 3.6;
			}
		};

		public abstract double toMps(double metersPerSecond);
	}

	public static class Velocity {

		private final double metersPerSecond;

		public Velocity(Distance distance, long millis) {
			this.metersPerSecond = mps(distance.value(CENTIMETER), millis);
		}

		private double mps(double cm, long millis) {
			return 10 * cm / millis;
		}

		private double value(SpeedUnit speedUnit) {
			return speedUnit.toMps(metersPerSecond);
		}

	}

	public static class Movement {

		private final long durationInMillis;
		private final Velocity velocity;
		private final Distance distance;

		public Movement(Position pos1, Position pos2) {
			this.distance = new Distance(sqrt(pow2(absDiffX(pos1, pos2)) + pow2(absDiffY(pos1, pos2))), CENTIMETER);
			this.durationInMillis = pos2.getTimestamp() - pos1.getTimestamp();
			this.velocity = new Velocity(distance, this.durationInMillis);
		}

		public double distance(DistanceUnit target) {
			return distance.value(target);
		}

		public long duration(TimeUnit target) {
			return target.convert(durationInMillis, MILLISECONDS);
		}

		public double velocity(SpeedUnit speedUnit) {
			return velocity.value(speedUnit);
		}

		private double absDiffX(Position p1, Position p2) {
			return abs(p1.getX() - p2.getX());
		}

		private double absDiffY(Position p1, Position p2) {
			return abs(p1.getY() - p2.getY());
		}

		private double pow2(double d) {
			return pow(d, 2);
		}

	}

	public static class AbsolutePosition implements Position {

		private final RelativePosition relativePosition;
		private final double x;
		private final double y;

		public AbsolutePosition(RelativePosition relativePosition, double x, double y) {
			this.relativePosition = relativePosition;
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean isNull() {
			return relativePosition.isNull();
		}

		public RelativePosition getRelativePosition() {
			return relativePosition;
		}

		@Override
		public long getTimestamp() {
			return relativePosition.getTimestamp();
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public String toString() {
			return "AbsolutePosition [relativePosition=" + relativePosition + ", x=" + x + ", y=" + y + "]";
		}

	}

	public static class Message {

		private final String topic;
		private final String payload;

		public Message(String topic, Object payload) {
			this.topic = topic;
			this.payload = payload == null ? null : String.valueOf(payload);
		}

		public String getPayload() {
			return payload;
		}

		public String getTopic() {
			return topic;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((payload == null) ? 0 : payload.hashCode());
			result = prime * result + ((topic == null) ? 0 : topic.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Message other = (Message) obj;
			if (payload == null) {
				if (other.payload != null)
					return false;
			} else if (!payload.equals(other.payload))
				return false;
			if (topic == null) {
				if (other.topic != null)
					return false;
			} else if (!topic.equals(other.topic))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Message [topic=" + topic + ", payload=" + payload + "]";
		}

	}

	public static class Table {

		private final int width;
		private final int height;

		public Table(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public AbsolutePosition toAbsolute(RelativePosition pos) {
			return new AbsolutePosition(pos, convertX(pos.getX()), convertY(pos.getY()));
		}

		private double convertY(double y) {
			return height * y;
		}

		private double convertX(double x) {
			return width * x;
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
			Integer newScore = score(teamid) + 1;
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
			if (detectors == null || gameOverListener.isGameover()) {
				detectors = detectors(gameOverListener = new GameOverListener());
			}
			String line;
			while ((line = reader.readLine()) != null) {
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
				new GoalDetector(goalDetectorConfig, teamid1 -> scoreTracker.teamScored(teamid1)), //
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
