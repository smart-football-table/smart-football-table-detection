package detection2;

import static java.lang.Math.abs;
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

	private static class RelativePosition {

		private long timestamp;
		private double x;
		private double y;

		public RelativePosition(long timestamp, double x, double y) {
			this.timestamp = timestamp;
			this.x = x;
			this.y = y;
		}

		public static RelativePosition parse(String line) {
			String[] values = line.split("\\,");

			if (values.length == 3) {
				Long timestamp = toLong(values[0]);
				Double x = toDouble(values[1]);
				Double y = toDouble(values[2]);
				if (timestamp != null && isValidPosition(x, y)) {
					return new RelativePosition(timestamp, x, y);
				}
			}
			return null;

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

		public double convertY(double y) {
			return height * y;
		}

		public double convertX(double x) {
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
		givenStdInContains("0,0.0,0.0", SECONDS.toMillis(1) + ",1.0,1.0");
		whenStdInInputWasProcessed();
		thenDistanceInCentimetersAndVelocityInMetersPerSecondArePublished(128.06248474865697, 1.2806248474865697);
	}

	private void thenDistanceInCentimetersAndVelocityInMetersPerSecondArePublished(double meters, double mps) {
		assertThat(onlyElement(messagesWithTopic("game/ball/distance/cm")).getPayload(), is(String.valueOf(meters)));
		assertThat(onlyElement(messagesWithTopic("game/ball/velocity/mps")).getPayload(), is(String.valueOf(mps)));
	}

	private void thenNoMessageIsSent() {
		assertThat(publisher.messages.isEmpty(), is(true));
	}

	private String malformedMessage() {
		return "A,B";
	}

	private String anyTimestamp() {
		return "0";
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
		RelativePosition prevPos = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				RelativePosition pos = RelativePosition.parse(line);
				if (pos != null) {
					long timestamp = pos.timestamp;
					String baseTopic = "game/ball/position/";
					sendXY(baseTopic + "abs", table.convertX(pos.x), table.convertY(pos.y));
					sendXY(baseTopic + "rel", pos.x, pos.y);

					// calculate distance and velocity
					if (prevPos != null) {
						double diffX = abs((table.convertX(prevPos.x) - table.convertX(pos.x)));
						double diffY = abs((table.convertY(prevPos.y) - table.convertY(pos.y)));
						double cm = Math.sqrt(diffX * diffX + diffY * diffY);
						double mps = 10 * cm / (timestamp - prevPos.timestamp);
						publisher.send(new Message("game/ball/distance/cm", String.valueOf(cm)));
						publisher.send(new Message("game/ball/velocity/mps", String.valueOf(mps)));
					}
					prevPos = pos;
				}
			}

		}
	}

	private void sendXY(String topic, Double x, Double y) {
		publisher.send(new Message(topic, "{ \"x\":" + x + ", \"y\":" + y + " }"));
	}

	private static boolean isValidPosition(Double x, Double y) {
		// TODO test x/y > 1.0?
		return x != null && y != null && x >= 0.0 && y >= 0.0;
	}

	private void thenTheRelativePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("game/ball/position/rel")).getPayload(), is(makePayload(x, y)));
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(double x, double y) {
		assertThat(onlyElement(messagesWithTopic("game/ball/position/abs")).getPayload(), is(makePayload(x, y)));
	}

	private String makePayload(double x, double y) {
		return "{ \"x\":" + x + ", \"y\":" + y + " }";
	}

	private Stream<Message> messagesWithTopic(String topic) {
		return publisher.messages.stream().filter(m -> m.getTopic().equals(topic));
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

	private static <T> T onlyElement(Stream<T> stream) {
		return stream.reduce(toOnlyElement()).orElseThrow(() -> new NoSuchElementException("empty stream"));
	}

	private static <T> BinaryOperator<T> toOnlyElement() {
		return (t1, t2) -> {
			throw new IllegalStateException("more than one element");
		};
	}

}
