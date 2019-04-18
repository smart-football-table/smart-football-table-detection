package detection2;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
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
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import org.junit.Test;

public class SFTDetectionTest {

	private static class Position {

		private long timestamp;
		private double x;
		private double y;

		public Position(long timestamp, double x, double y) {
			this.timestamp = timestamp;
			this.x = x;
			this.y = y;
		}

		public static Position parse(String line) {
			String[] values = line.split("\\,");
			if (values.length == 3) {
				Long timestamp = toLong(values[0]);
				Double x = toDouble(values[1]);
				Double y = toDouble(values[2]);
				if (isValidTimestamp(timestamp) && isValidPosition(x, y)) {
					return new Position(timestamp, x, y);
				}
			}
			return null;
		}

		private static boolean isValidTimestamp(Long timestamp) {
			return timestamp != null && timestamp >= 0;
		}

		private static boolean isValidPosition(Double x, Double y) {
			// TODO test x/y > 1.0?
			return x != null && y != null && x >= 0.0 && y >= 0.0;
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

		public Position toAbsolute(Position pos) {
			return new Position(pos.timestamp, convertX(pos.x), convertY(pos.y));
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

	private MessagePublisherForTest publisher = new MessagePublisherForTest();
	private Table table;
	private InputStream is;

	@Test
	public void relativeValuesGetsConvertedToAbsolutesAtKickoff() throws IOException {
		givenATableOfSize(100, 80);
		double[] xy = kickoffPosition();
		givenStdInContains(anyTimestamp() + "," + xy[0] + "," + xy[1]);
		whenStdInInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(xy[0], xy[1]);
		thenTheAbsolutePositionOnTheTableIsPublished(100 / 2, 80 / 2);
	}

	@Test
	public void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(anyTimestamp() + "," + "0.0,1.0");
		whenStdInInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(0.0, 1.0);
		thenTheAbsolutePositionOnTheTableIsPublished(0, 80);
	}

	@Test
	public void malformedMessageIsRead() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(anyTimestamp() + "," + malformedMessage());
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfSize(100, 80);
		givenStdInContains(anyTimestamp() + "," + "-1.0,-1.0");
		whenStdInInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void whenTwoPositionsAreRead_VelocityGetsPublished() throws IOException {
		givenATableOfSize(100, 80);
		int startTimestamp = anyTimestamp();
		long endTimestamp = startTimestamp + SECONDS.toMillis(1);
		givenStdInContains(startTimestamp + ",0.0,0.0", endTimestamp + ",1.0,1.0");
		whenStdInInputWasProcessed();
		thenDistanceInCentimetersAndVelocityArePublished(128.06248474865697, 1.2806248474865697);
	}

	private void thenDistanceInCentimetersAndVelocityArePublished(double meters, double mps) {
		assertThat(onlyElement(messagesWithTopic("ball/distance/cm")).getPayload(), is(String.valueOf(meters)));
		assertThat(onlyElement(messagesWithTopic("ball/velocity/mps")).getPayload(), is(String.valueOf(mps)));
		assertThat(onlyElement(messagesWithTopic("ball/velocity/kmh")).getPayload(), is(String.valueOf(mps * 3.6)));
	}

	private void thenNoMessageIsSent() {
		assertThat(publisher.messages.isEmpty(), is(true));
	}

	private String malformedMessage() {
		return "A,B";
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
		Position prevRelPos = null;
		Position prevAbsPos = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Position relPos = Position.parse(line);
				if (relPos != null) {
					String baseTopic = "ball/position/";
					Position absPos = table.toAbsolute(relPos);
					sendPosition(baseTopic + "abs", absPos);
					sendPosition(baseTopic + "rel", relPos);

					// calculate distance and velocity
					if (prevRelPos != null) {
						double cm = sqrt(pow2(absDiffX(prevAbsPos, absPos)) + pow2(absDiffY(prevAbsPos, absPos)));
						publisher.send(new Message("ball/distance/cm", String.valueOf(cm)));
						double mps = 10 * cm / (relPos.timestamp - prevRelPos.timestamp);
						publisher.send(new Message("ball/velocity/mps", String.valueOf(mps)));
						publisher.send(new Message("ball/velocity/kmh", String.valueOf(mps * 3.6)));
					}
					prevRelPos = relPos;
					prevAbsPos = absPos;
				}
			}

		}
	}

	private double absDiffX(Position p1, Position p2) {
		return abs(p1.x - p2.x);
	}

	private double absDiffY(Position p1, Position p2) {
		return abs(p1.y - p2.y);
	}

	private double pow2(double d) {
		return pow(d, 2);
	}

	private void sendPosition(String topic, Position position) {
		publisher.send(new Message(topic, "{ \"x\":" + position.x + ", \"y\":" + position.y + " }"));
	}

	private void thenTheRelativePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("ball/position/rel")).getPayload(), is(makePayload(x, y)));
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("ball/position/abs")).getPayload(), is(makePayload(x, y)));
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
