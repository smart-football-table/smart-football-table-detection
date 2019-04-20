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
import static java.util.Collections.emptyList;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

	private static interface MessageGenerator {
		Collection<Message> messages(AbsolutePosition pos);
	}

	private static class MovementMessageGenerator implements MessageGenerator {

		private AbsolutePosition prevPos;

		@Override
		public Collection<Message> messages(AbsolutePosition pos) {
			RelativePosition relPos = pos.getRelativePosition();
			List<Message> messages = emptyList();
			if (!relPos.isNull()) {
				if (prevPos != null) {
					messages = messages(new Movement(prevPos, pos));
				}
				prevPos = pos;
			}
			return messages;
		}

		private List<Message> messages(Movement movement) {
			return asList( //
					new Message("ball/distance/cm", movement.distance(CENTIMETER)), //
					new Message("ball/velocity/mps", movement.velocity(MPS)), //
					new Message("ball/velocity/kmh", movement.velocity(KMH) //
					));
		}

	}

	private static class PositionMessageGenerator implements MessageGenerator {

		@Override
		public Collection<Message> messages(AbsolutePosition pos) {
			return pos.getRelativePosition().isNull() ? emptyList() : positions(pos);
		}

		private List<Message> positions(AbsolutePosition pos) {
			return asList( //
					positionMessage("ball/position/abs", pos), //
					positionMessage("ball/position/rel", pos.getRelativePosition()) //
			);
		}

		private Message positionMessage(String topic, Position position) {
			return new Message(topic, "{ \"x\":" + position.getX() + ", \"y\":" + position.getY() + " }");
		}

	}

	private static class GoalMessageGenerator implements MessageGenerator {

		private static final int MAX_BALLS = 10;

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
				return isFrontOfGoal(relPos) ? new FrontOfGoal(relPos.isRightHandSide()) : this;
			}

			private boolean isFrontOfGoal(RelativePosition relPos) {
				return relPos.normalizeX().getX() >= 1 - ((double) frontOfGoalPercentage) / 100;
			}
		}

		private class FrontOfGoal implements State {

			private final boolean rightHandSide;
			private final BallOnTable ballOnTable = new BallOnTable();

			public FrontOfGoal(boolean rightHandSide) {
				this.rightHandSide = rightHandSide;
			}

			@Override
			public State update(AbsolutePosition pos) {
				if (pos.isNull()) {
					return millisTilGoal == 0 //
							? new Goal(rightHandSide) //
							: new PossibleGoal(pos.getTimestamp(), rightHandSide);
				}
				return ballOnTable.update(pos);
			}
		}

		private class PossibleGoal implements State {

			private final BallOnTable ballOnTable = new BallOnTable();
			private final long timestamp;
			private final boolean rightHandSide;

			public PossibleGoal(long timestamp, boolean rightHandSide) {
				this.timestamp = timestamp;
				this.rightHandSide = rightHandSide;
			}

			@Override
			public State update(AbsolutePosition pos) {
				if (pos.isNull()) {
					return waitTimeElapsed(pos) ? new Goal(rightHandSide) : this;
				}
				return ballOnTable.update(pos);
			}

			private boolean waitTimeElapsed(AbsolutePosition pos) {
				return pos.getTimestamp() - timestamp >= millisTilGoal;
			}

		}

		private class Goal implements State {

			private final boolean rightHandSide;

			public Goal(boolean rightHandSide) {
				this.rightHandSide = rightHandSide;
			}

			@Override
			public State update(AbsolutePosition pos) {
				return new WaitForBallOnMiddleLine().update(pos);
			}

			public boolean isRightHandSide() {
				return rightHandSide;
			}

		}

		private final Map<Integer, Integer> scores = new HashMap<>();
		private int frontOfGoalPercentage = 40;
		private long millisTilGoal = SECONDS.toMillis(2);
		private State state = new WaitForBallOnMiddleLine();

		@Override
		public Collection<Message> messages(AbsolutePosition pos) {
			state = state.update(pos);
			return state instanceof Goal ? goalMessage(((Goal) state).isRightHandSide()) : emptyList();
		}

		private List<Message> goalMessage(boolean isRightHandSide) {
			int teamid = isRightHandSide ? 0 : 1;
			List<Message> messages = new ArrayList<>();
			messages.add(new Message("team/scored", teamid));
			messages.add(new Message("game/score/" + teamid, increaseScore(teamid)));
			if (hasWon(teamid)) {
				messages.add(new Message("game/gameover", teamid));
			} else if (isDraw()) {
				messages.add(new Message("game/gameover", teamids()));
			}

			return messages;
		}

		private int increaseScore(int teamid) {
			Integer newScore = scores.getOrDefault(teamid, 0) + 1;
			scores.put(teamid, newScore);
			return newScore;
		}

		private boolean hasWon(int teamid) {
			return scores.get(teamid) > MAX_BALLS / 2;
		}

		private boolean isDraw() {
			return scoresSum() == MAX_BALLS;
		}

		private int scoresSum() {
			return scores.values().stream().mapToInt(Integer::intValue).sum();
		}

		private String teamids() {
			return scores.keySet().stream().sorted().map(String::valueOf).collect(joining(","));
		}

		/**
		 * Sets where the ball has to been detected before the ball has been gone.
		 * 
		 * @param i 100% the whole playfield, 50% one side.
		 * @return
		 */
		public GoalMessageGenerator setFrontOfGoalPercentage(int frontOfGoalPercentage) {
			this.frontOfGoalPercentage = frontOfGoalPercentage;
			return this;
		}

		public void setTimeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
			this.millisTilGoal = timeUnit.toMillis(duration);
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
		private Distance distance;

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

	private final GoalMessageGenerator goalMessageGenerator = new GoalMessageGenerator();
	private final MessagePublisherForTest publisher = new MessagePublisherForTest();
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

	private BallPosBuilder frontOfLeftGoal() {
		return kickoff().left(0.3);
	}

	private BallPosBuilder frontOfRightGoal() {
		return kickoff().right(0.3);
	}

	private void thenDistanceInCentimetersAndVelocityArePublished(double centimeters, double mps, double kmh) {
		assertThat(onlyElement(messagesWithTopic("ball/distance/cm")).getPayload(), is(String.valueOf(centimeters)));
		assertThat(onlyElement(messagesWithTopic("ball/velocity/mps")).getPayload(), is(String.valueOf(mps)));
		assertThat(onlyElement(messagesWithTopic("ball/velocity/kmh")).getPayload(), is(String.valueOf(kmh)));
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

	private void givenFrontOfGoalPercentage(int percentage) {
		goalMessageGenerator.setFrontOfGoalPercentage(percentage);
	}

	private void givenTimeWithoutBallTilGoal(long millis, TimeUnit timeUnit) {
		goalMessageGenerator.setTimeWithoutBallTilGoal(millis, timeUnit);
	}

	private void whenStdInInputWasProcessed() throws IOException {
		List<MessageGenerator> generators = asList( //
				goalMessageGenerator, //
				new PositionMessageGenerator(), //
				new MovementMessageGenerator() //
		);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				RelativePosition relPos = parse(line);
				if (relPos == null) {
					// TODO log invalid line
				} else {
					AbsolutePosition absPos = table.toAbsolute(relPos);
					for (MessageGenerator messageGenerator : generators) {
						for (Message message : messageGenerator.messages(absPos)) {
							publisher.send(message);
						}
					}
				}
			}

		}
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
		assertThat(onlyElement(messagesWithTopic("game/score/" + teamid)).getPayload(), is(String.valueOf(score)));
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
