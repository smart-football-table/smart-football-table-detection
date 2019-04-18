package detection2;

import static detection2.SFTDetectionTest.DistanceUnit.CENTIMETER;
import static detection2.SFTDetectionTest.SpeedUnit.KMH;
import static detection2.SFTDetectionTest.SpeedUnit.MPS;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import org.junit.Test;

public class SFTDetectionTest {

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

	static class RelativePosition implements Position {

		public static final RelativePosition NULL = new RelativePosition(-1, -1, -1) {
			@Override
			public boolean isNull() {
				return true;
			}
		};

		private final long timestamp;
		private final double x;
		private final double y;

		public RelativePosition(long timestamp, double x, double y) {
			this.timestamp = timestamp;
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean isNull() {
			return false;
		}

		@Override
		public long getTimestamp() {
			return timestamp;
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
		givenATableOfSize(100, 80);
		givenStdInContains(line(anyTimestamp(), "A", "B"));
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfSize(100, 80);
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
		givenATableOfSize(100, 80);
		givenStdInContains(line(anyTimestamp(), "0.99", "0.5"), anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		assertThat(onlyElement(messagesWithTopic("team/scored")).getPayload(), is("0"));
	}

	@Test
	public void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(line(anyTimestamp(), "0.01", "0.5"), anyTimestamp() + "," + noBallOnTable());
		whenStdInInputWasProcessed();
		assertThat(onlyElement(messagesWithTopic("team/scored")).getPayload(), is("1"));
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
		return new double[] { 0.5, 0.5 };
	}

	private void givenATableOfSize(int width, int height) {
		this.table = new Table(width, height);
	}

	private void givenStdInContains(String... messages) {
		is = new ByteArrayInputStream(Arrays.stream(messages).collect(joining("\n")).getBytes());
	}

	private void whenStdInInputWasProcessed() throws IOException {
		AbsolutePosition prevAbsPos = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				RelativePosition relPos = parse(line);
				if (relPos != null) {
					if (relPos.isNull()) {
						if (prevAbsPos != null) {
							sendGoal(prevAbsPos);
						}
					} else {
						AbsolutePosition absPos = table.toAbsolute(relPos);
						sendPositions(relPos, absPos);
						if (prevAbsPos != null) {
							sendMovement(new Movement(prevAbsPos, absPos));
						}
						prevAbsPos = absPos;
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
					return RelativePosition.NULL;
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

	private void sendPositions(Position relPos, Position absPos) {
		sendPosition("ball/position/abs", absPos);
		sendPosition("ball/position/rel", relPos);
	}

	private void sendPosition(String topic, Position position) {
		publisher.send(new Message(topic, "{ \"x\":" + position.getX() + ", \"y\":" + position.getY() + " }"));
	}

	private void sendMovement(Movement movement) {
		publisher.send(new Message("ball/distance/cm", String.valueOf(movement.distance(CENTIMETER))));
		publisher.send(new Message("ball/velocity/mps", String.valueOf(movement.velocity(MPS))));
		publisher.send(new Message("ball/velocity/kmh", String.valueOf(movement.velocity(KMH))));
	}

	private void sendGoal(AbsolutePosition prevAbsPos) {
		String team = prevAbsPos.getRelativePosition().getX() >= 0.5 ? "0" : "1";
		publisher.send(new Message("team/scored", team));
	}

	private void thenTheRelativePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("ball/position/rel")).getPayload(), is(makePayload(x, y)));
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("ball/position/abs")).getPayload(), is(makePayload(x, y)));
	}

	private void thenNoMessageIsSent() {
		assertThat(publisher.messages.isEmpty(), is(true));
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
