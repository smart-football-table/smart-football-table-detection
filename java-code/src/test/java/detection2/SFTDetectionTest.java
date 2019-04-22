package detection2;

import static detection2.SFTDetectionTest.DistanceUnit.CENTIMETER;
import static detection2.SFTDetectionTest.SpeedUnit.KMH;
import static detection2.SFTDetectionTest.SpeedUnit.MPS;
import static detection2.SFTDetectionTest.StdInBuilder.ball;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.kickoff;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.offTable;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.pos;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Test;

import detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder;

public class SFTDetectionTest {

	public static class StdInBuilder {

		public static class BallPosBuilder {

			private double x;
			private double y;

			public BallPosBuilder(double x, double y) {
				this.x = x;
				this.y = y;
			}

			public static BallPosBuilder kickoff() {
				return pos(centerX(), centerY());
			}

			public static BallPosBuilder pos(double x, double y) {
				return new BallPosBuilder(x, y);
			}

			private static double centerX() {
				return 0.5;
			}

			private static double centerY() {
				return 0.5;
			}

			public BallPosBuilder left(double adjX) {
				x -= adjX;
				return this;
			}

			public BallPosBuilder right(double adjX) {
				x += adjX;
				return this;
			}

			public BallPosBuilder up(double adjY) {
				y -= adjY;
				return this;
			}

			public BallPosBuilder down(double adjY) {
				y += adjY;
				return this;
			}

			public static BallPosBuilder offTable() {
				RelativePosition noBall = RelativePosition.noPosition(0L);
				return pos(noBall.getX(), noBall.getY());
			}

		}

		private long timestamp;
		private final List<String> lines = new ArrayList<>();

		public StdInBuilder(long timestamp) {
			this.timestamp = timestamp;
		}

		public static StdInBuilder ball() {
			return messagesStartingAt(anyTimestamp());
		}

		private static StdInBuilder messagesStartingAt(long timestamp) {
			return new StdInBuilder(timestamp);
		}

		private static long anyTimestamp() {
			return 1234;
		}

		public StdInBuilder then() {
			return this;
		}

		public StdInBuilder then(BallPosBuilder ballPosBuilder) {
			return at(ballPosBuilder);
		}

		private StdInBuilder at(BallPosBuilder ballPosBuilder) {
			lines.add(line(timestamp, 0.0 + ballPosBuilder.x, ballPosBuilder.y));
			return this;
		}

		StdInBuilder thenAfter(long adjustment, TimeUnit timeUnit) {
			timestamp += timeUnit.toMillis(adjustment);
			return this;
		}

		public StdInBuilder invalidData() {
			lines.add(line(timestamp, "A", "B"));
			return this;
		}

		private String line(Object... objects) {
			return Arrays.stream(objects).map(String::valueOf).collect(joining(","));
		}

		public StdInBuilder prepareForGoal() {
			return at(kickoff());
		}

		public StdInBuilder makeGoal() {
			return then(offTable()).thenAfter(2, SECONDS).then(offTable());
		}

		public String[] build() {
			return lines.toArray(new String[lines.size()]);
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

		private static class Config {

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

		private final int frontOfGoalPercentage;
		private final long millisTilGoal;
		private State state = new WaitForBallOnMiddleLine();
		private final Listener listener;

		public static interface Listener {
			void goal(int teamid);
		}

		public GoalDetector(Config config, Listener listener) {
			this.frontOfGoalPercentage = config.getFrontOfGoalPercentage();
			this.millisTilGoal = config.getGoalTimeout(MILLISECONDS);
			this.listener = listener;
		}

		private static interface State {
			State update(AbsolutePosition pos);
		}

		private class WaitForBallOnMiddleLine implements State {

			@Override
			public State update(AbsolutePosition pos) {
				return ballAtMiddleLine(pos) ? new BallOnTable().update(pos) : this;
			}

			private boolean ballAtMiddleLine(AbsolutePosition pos) {
				double normalizeX = pos.getRelativePosition().normalizeX().getX();
				return normalizeX >= 0.5 && normalizeX <= 0.55;
			}

		}

		private class BallOnTable implements State {

			@Override
			public State update(AbsolutePosition pos) {
				RelativePosition relPos = pos.getRelativePosition();
				return isFrontOfGoal(relPos) ? new FrontOfGoal(relPos.isRightHandSide() ? 0 : 1) : this;
			}

			private boolean isFrontOfGoal(RelativePosition relPos) {
				return relPos.normalizeX().getX() >= 1 - ((double) frontOfGoalPercentage) / 100;
			}
		}

		private class FrontOfGoal implements State {

			private final int teamid;
			private final BallOnTable ballOnTable = new BallOnTable();

			public FrontOfGoal(int teamid) {
				this.teamid = teamid;
			}

			@Override
			public State update(AbsolutePosition pos) {
				if (pos.isNull()) {
					return millisTilGoal == 0 //
							? new Goal(teamid) //
							: new PossibleGoal(pos.getTimestamp(), teamid);
				}
				return ballOnTable.update(pos);
			}
		}

		private class PossibleGoal implements State {

			private final BallOnTable ballOnTable = new BallOnTable();
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
				return ballOnTable.update(pos);
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

	private static class AbsolutePosition implements Position {

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

	private static class Message {

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

	private final class MessagePublisherForTest implements MessagePublisher {

		private final List<Message> messages = new ArrayList<>();

		@Override
		public void send(Message message) {
			messages.add(message);
		}
	}

	private interface MessagePublisher {
		void send(Message message);
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

	private final MessagePublisherForTest publisher = new MessagePublisherForTest();
	private GoalDetector.Config goalDetectorConfig = new GoalDetector.Config();

	private Table table;
	private InputStream is;

	@Test
	public void relativeValuesGetsConvertedToAbsolutesAtKickoff() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(ball().at(kickoff()));
		whenStdInInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(centerX(), centerY());
		thenTheAbsolutePositionOnTheTableIsPublished(100 / 2, 80 / 2);
	}

	@Test
	public void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(ball().at(pos(0.0, 1.0)));
		whenStdInInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(0.0, 1.0);
		thenTheAbsolutePositionOnTheTableIsPublished(0, 80);
	}

	@Test
	public void malformedMessageIsRead() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball().invalidData());
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball().at(offTable()));
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void whenTwoPositionsAreRead_VelocityGetsPublished() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(ball().at(pos(0.0, 0.0)).thenAfter(1, SECONDS).at(pos(1.0, 1.0)));
		whenStdInInputWasProcessed();
		thenDistanceInCentimetersAndVelocityArePublished(128.06248474865697, 1.2806248474865697, 4.610249450951652);
	}

	@Test
	public void canDetectGoalOnRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForGoal().then().at(frontOfRightGoal()).makeGoal());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(0);
		thenGameScoreForTeamIsPublished(0, 1);
	}

	@Test
	public void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForGoal().then().at(frontOfLeftGoal()).makeGoal());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForGoal().then().at(frontOfRightGoal().left(0.01)).makeGoal());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForGoal().then().at(frontOfLeftGoal().right(0.01)).makeGoal());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void leftHandSideScoresThreeTimes() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal() //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("team/scored", times("1", 3));
		thenPayloadsWithTopicAre("game/score/1", "1", "2", "3");
	}

	@Test
	public void noGoalsIfBallWasNotDetectedAtMiddleLine() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().at(frontOfLeftGoal()).then(offTable()).makeGoal());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalsIfThereAreNotTwoSecondsWithoutPositions() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		long timeout = SECONDS.toMillis(2);
		givenTimeWithoutBallTilGoal(timeout, MILLISECONDS);
		givenStdInContains(ball().prepareForGoal().then().at(frontOfLeftGoal()).then(offTable())
				.thenAfter(timeout - 1, MILLISECONDS).then(offTable()));
		whenStdInInputWasProcessed();
		givenStdInContains(ball().prepareForGoal().then().at(frontOfRightGoal()).then(offTable())
				.thenAfter(timeout - 1, MILLISECONDS).then(offTable()));
		whenStdInInputWasProcessed();
		givenStdInContains(ball().prepareForGoal().then().at(frontOfLeftGoal()).then(offTable())
				.thenAfter(timeout - 1, MILLISECONDS).then(kickoff()));
		whenStdInInputWasProcessed();
		givenStdInContains(ball().prepareForGoal().then().at(frontOfRightGoal()).then(offTable())
				.thenAfter(timeout - 1, MILLISECONDS).then(kickoff()));
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void withoutWaitTimeTheGoalDirectlyCounts() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenTimeWithoutBallTilGoal(0, MILLISECONDS);
		givenStdInContains(ball().prepareForGoal().then().at(frontOfLeftGoal()).then(offTable()));
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void doesSendWinner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal() //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/gameover", "1");
	}

	@Test
	public void doesSendDrawWinners() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal() //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/gameover", "0,1");
	}

	@Test
	public void doesSendGameStart() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball().at(kickoff()).at(kickoff()));
		whenStdInInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/start"), is(""));
	}

	@Test
	public void doesSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenStdInContains(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(5, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenStdInInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/foul"), is(""));
	}

	@Test
	public void doesNotSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenStdInContains(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(4, SECONDS).at(frontOfLeftGoal()) //
				.thenAfter(1, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("game/foul");
	}

	@Test
	public void doesSendFoulOnlyOnceUntilFoulIsOver() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenStdInContains(ball().at(middlefieldRow.up(0.49)) //
				.thenAfter(15, SECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenStdInInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/foul"), is(""));
	}

	@Test
	public void doesRestartAfterGameEnd() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfRightGoal()).makeGoal() //
		);
		whenStdInInputWasProcessed();
		givenStdInContains(ball() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal().then() //
				.prepareForGoal().then().at(frontOfLeftGoal()).makeGoal() //
		);
		whenStdInInputWasProcessed();
		assertThat(publisher.messages.stream().filter(m -> !m.getTopic().startsWith("ball/")).collect(toList()),
				is(asList( //
						new Message("game/start", ""), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 1), //
						new Message("team/scored", 0), //
						new Message("game/score/0", 1), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 2), //
						new Message("team/scored", 0), //
						new Message("game/score/0", 2), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 3), //
						new Message("team/scored", 0), //
						new Message("game/score/0", 3), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 4), //
						new Message("team/scored", 0), //
						new Message("game/score/0", 4), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 5), //
						new Message("team/scored", 0), //
						new Message("game/score/0", 5), //
						new Message("game/gameover", "0,1"), //

						new Message("game/start", ""), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 1), //
						new Message("team/scored", 1), //
						new Message("game/score/1", 2) //
				)));
	}

	private BallPosBuilder frontOfLeftGoal() {
		return kickoff().left(0.3);
	}

	private BallPosBuilder frontOfRightGoal() {
		return kickoff().right(0.3);
	}

	private void thenDistanceInCentimetersAndVelocityArePublished(double centimeters, double mps, double kmh) {
		assertOneMessageWithPayload(messagesWithTopic("ball/distance/cm"), is(String.valueOf(centimeters)));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/mps"), is(String.valueOf(mps)));
		assertOneMessageWithPayload(messagesWithTopic("ball/velocity/kmh"), is(String.valueOf(kmh)));
	}

	private double centerX() {
		return 0.5;
	}

	private double centerY() {
		return 0.5;
	}

	private void givenATableOfSize(int width, int height) {
		this.table = new Table(width, height);
	}

	private void givenATableOfAnySize() {
		givenATableOfSize(123, 45);
	}

	private void givenStdInContains(String... messages) {
		is = new ByteArrayInputStream(Arrays.stream(messages).collect(joining("\n")).getBytes());
	}

	private void givenStdInContains(StdInBuilder builder) {
		givenStdInContains(builder.build());
	}

	private void givenFrontOfGoalPercentage(int frontOfGoalPercentage) {
		this.goalDetectorConfig.frontOfGoalPercentage(frontOfGoalPercentage);
	}

	private void givenTimeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
		this.goalDetectorConfig.timeWithoutBallTilGoal(duration, timeUnit);
	}

	private void whenStdInInputWasProcessed() throws IOException {
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
				RelativePosition relPos = parse(line);
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
		return asList( //
				new GameStartDetector(() -> asList(new Message("game/start", "")).forEach(publisher::send)), //
				new PositionDetector(new PositionDetector.Listener() {
					@Override
					public void position(AbsolutePosition pos) {
						Position rel = pos.getRelativePosition();
						asList( //
								new Message("ball/position/abs", payload(pos)), //
								new Message("ball/position/rel", payload(rel)) //
						).forEach(publisher::send);
					}

					private String payload(Position pos) {
						return "{ \"x\":" + pos.getX() + ", \"y\":" + pos.getY() + " }";
					}

				}), //
				new MovementDetector(movement -> asList( //
						new Message("ball/distance/cm", movement.distance(CENTIMETER)), //
						new Message("ball/velocity/mps", movement.velocity(MPS)), //
						new Message("ball/velocity/kmh", movement.velocity(KMH) //
						)).forEach(publisher::send)), //
				goalDetector(new ScoreTracker(multiplexed(new ScoreTracker.Listener() {
					@Override
					public void teamScored(int teamid, int score) {
						asList(new Message("team/scored", teamid), new Message("game/score/" + teamid, score))
								.forEach(publisher::send);
					}

					@Override
					public void won(int teamid) {
						publisher.send(new Message("game/gameover", teamid));
					}

					@Override
					public void draw(int[] teamids) {
						publisher.send(new Message("game/gameover",
								IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
					}

				}, gameOverListener))), //
				new FoulDetector(() -> asList(new Message("game/foul", "")).forEach(publisher::send)));
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

	private GoalDetector goalDetector(ScoreTracker scoreTracker) {
		return new GoalDetector(goalDetectorConfig, teamid -> scoreTracker.teamScored(teamid));
	}

	private static RelativePosition parse(String line) {
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

	private void thenTheRelativePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("ball/position/rel")).getPayload(), is(makePayload(x, y)));
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("ball/position/abs")).getPayload(), is(makePayload(x, y)));
	}

	private void thenGoalForTeamIsPublished(int teamid) {
		assertThat(onlyElement(messagesWithTopic("team/scored")).getPayload(), is(String.valueOf(teamid)));
	}

	private void thenGameScoreForTeamIsPublished(int teamid, int score) {
		assertOneMessageWithPayload(messagesWithTopic("game/score/" + teamid), is(String.valueOf(score)));
	}

	private void assertOneMessageWithPayload(Stream<Message> messagesWithTopic, Matcher<String> matcher) {
		assertThat(onlyElement(messagesWithTopic).getPayload(), matcher);
	}

	private void thenNoMessageIsSent() {
		assertThat(String.valueOf(publisher.messages), publisher.messages.isEmpty(), is(true));
	}

	private void thenNoMessageWithTopicIsSent(String topic) {
		thenNoMessageIsSent(m -> m.getTopic().equals(topic));
	}

	private void thenNoMessageIsSent(Predicate<Message> predicate) {
		List<Message> mWithTopic = publisher.messages.stream().filter(predicate).collect(toList());
		assertThat(String.valueOf(mWithTopic), mWithTopic.isEmpty(), is(true));
	}

	private void thenPayloadsWithTopicAre(String topic, String... payloads) {
		assertThat(payloads(messagesWithTopic(topic)), is(asList(payloads)));
	}

	private List<String> payloads(Stream<Message> messages) {
		return messages.map(Message::getPayload).collect(toList());
	}

	private static String[] times(String value, int times) {
		return range(0, times).mapToObj(i -> value).toArray(String[]::new);
	}

	private String makePayload(double x, double y) {
		return "{ \"x\":" + x + ", \"y\":" + y + " }";
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return publisher.messages.stream().filter(m -> m.getTopic().equals(topic));
	}

	private static <T> T onlyElement(Stream<T> stream) {
		return stream.reduce(toOnlyElement()).orElseThrow(() -> new NoSuchElementException("empty stream"));
	}

	private static <T> BinaryOperator<T> toOnlyElement() {
		return (t1, t2) -> {
			throw new IllegalStateException("more than one element");
		};
	}

}
