package detection2;

import static detection2.SFTDetectionTest.DistanceUnit.CENTIMETER;
import static detection2.SFTDetectionTest.SpeedUnit.KMH;
import static detection2.SFTDetectionTest.SpeedUnit.MPS;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Test;

public class SFTDetectionTest {

	private static interface MessageGenerator {
		Collection<Message> update(AbsolutePosition absPos);
	}

	private static class MovementMessageGenerator implements MessageGenerator {

		private AbsolutePosition prevAbsPos;

		@Override
		public Collection<Message> update(AbsolutePosition absPos) {
			RelativePosition relPos = absPos.getRelativePosition();
			List<Message> messages = Collections.emptyList();
			if (!relPos.isNull()) {
				if (prevAbsPos != null) {
					messages = messages(new Movement(prevAbsPos, absPos));
				}
				prevAbsPos = absPos;
			}
			return messages;
		}

		private List<Message> messages(Movement movement) {
			return asList( //
					new Message("ball/distance/cm", String.valueOf(movement.distance(CENTIMETER))), //
					new Message("ball/velocity/mps", String.valueOf(movement.velocity(MPS))), //
					new Message("ball/velocity/kmh", String.valueOf(movement.velocity(KMH)) //
					));
		}

	}

	private static class PositionMessageGenerator implements MessageGenerator {

		@Override
		public Collection<Message> update(AbsolutePosition absPos) {
			return absPos.getRelativePosition().isNull() ? Collections.emptyList() : messages(absPos);
		}

		private List<Message> messages(AbsolutePosition position) {
			return asList( //
					positionMessage("ball/position/abs", position), //
					positionMessage("ball/position/rel", position.getRelativePosition()) //
			);
		}

		private Message positionMessage(String topic, Position position) {
			return new Message(topic, "{ \"x\":" + position.getX() + ", \"y\":" + position.getY() + " }");
		}

	}

	private static class GoalMessageGenerator implements MessageGenerator {

		private final Map<Integer, Integer> scores = new HashMap<>();
		private int frontOfGoalPercentage = 40;
		private AbsolutePosition frontOfGoalPos;

		@Override
		public Collection<Message> update(AbsolutePosition absPos) {
			RelativePosition relPos = absPos.getRelativePosition();
			List<Message> messages = Collections.emptyList();
			if (!ballOnTable(relPos) && frontOfGoalPos != null) {
				messages = goalMessage();
			}
			frontOfGoalPos = isFrontOfGoal(relPos) ? absPos : null;
			return messages;
		}

		private boolean isFrontOfGoal(RelativePosition relPos) {
			return relPos.normalizeX().getX() >= 1 - ((double) frontOfGoalPercentage) / 100;
		}

		private boolean ballOnTable(RelativePosition relPos) {
			return !relPos.isNull();
		}

		private List<Message> goalMessage() {
			RelativePosition prevRelPos = frontOfGoalPos.getRelativePosition();
			int teamid = prevRelPos.isRightHandSide() ? 0 : 1;
			return asList( //
					new Message("team/scored", String.valueOf(teamid)), //
					new Message("game/score/" + teamid, String.valueOf(increaseScore(teamid))) //
			);
		}

		private int increaseScore(int teamid) {
			Integer newScore = scores.getOrDefault(teamid, 0) + 1;
			scores.put(teamid, newScore);
			return newScore;
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

	}

	private static class Message {

		private final String topic;
		private final String payload;

		public Message(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
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
		double[] xy = kickoffPosition();
		givenStdInContains(line(anyTimestamp(), xy[0], xy[1]));
		whenStdInInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(xy[0], xy[1]);
		thenTheAbsolutePositionOnTheTableIsPublished(100 / 2, 80 / 2);
	}

	@Test
	public void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(line(anyTimestamp(), "0.0", "1.0"));
		whenStdInInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(0.0, 1.0);
		thenTheAbsolutePositionOnTheTableIsPublished(0, 80);
	}

	@Test
	public void malformedMessageIsRead() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(line(anyTimestamp(), "A", "B"));
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void whenTwoPositionsAreRead_VelocityGetsPublished() throws IOException {
		givenATableOfSize(100, 80);
		int startTimestamp = anyTimestamp();
		long endTimestamp = startTimestamp + SECONDS.toMillis(1);
		givenStdInContains(line(startTimestamp, "0.0", "0.0"), line(endTimestamp, "1.0", "1.0"));
		whenStdInInputWasProcessed();
		thenDistanceInCentimetersAndVelocityArePublished(128.06248474865697, 1.2806248474865697, 4.610249450951652);
	}

	@Test
	public void canDetectGoalOnRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(line(anyTimestamp(), 1.0 - 0.20, centerY()), anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(0);
		thenGameScoreForTeamIsPublished(0, 1);
	}

	@Test
	public void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(line(anyTimestamp(), 0.0 + 0.20, centerY()), anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(line(anyTimestamp(), 1.0 - 0.21, centerY()), anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(line(anyTimestamp(), 0.0 + 0.21, centerY()), anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void leftHandSideScoresThreeTimes() throws IOException {
		givenATableOfAnySize();
		String frontOfLeftGoal = line(anyTimestamp(), 0.0 + 0.20, centerY());
		String ballGone = anyTimestamp() + "," + noBallOnTable();
		givenStdInContains( //
				frontOfLeftGoal, ballGone, //
				frontOfLeftGoal, ballGone, //
				frontOfLeftGoal, ballGone //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("team/scored", times("1", 3));
		thenPayloadsWithTopicAre("game/score/1", "1", "2", "3");
	}

	private String noBallOnTable() {
		return "-1,-1";
	}

	private void thenDistanceInCentimetersAndVelocityArePublished(double centimeters, double mps, double kmh) {
		assertThat(onlyElement(messagesWithTopic("ball/distance/cm")).getPayload(), is(String.valueOf(centimeters)));
		assertThat(onlyElement(messagesWithTopic("ball/velocity/mps")).getPayload(), is(String.valueOf(mps)));
		assertThat(onlyElement(messagesWithTopic("ball/velocity/kmh")).getPayload(), is(String.valueOf(kmh)));
	}

	private String line(Object... objects) {
		return Arrays.stream(objects).map(String::valueOf).collect(joining(","));
	}

	private int anyTimestamp() {
		return 1234;
	}

	private double[] kickoffPosition() {
		return new double[] { centerX(), centerY() };
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

	private void givenFrontOfGoalPercentage(int percentage) {
		goalMessageGenerator.setFrontOfGoalPercentage(percentage);
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
						for (Message message : messageGenerator.update(absPos)) {
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
