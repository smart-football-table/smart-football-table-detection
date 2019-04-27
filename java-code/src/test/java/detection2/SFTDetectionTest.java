package detection2;

import static detection2.SFTDetection.detectionOn;
import static detection2.SFTDetectionTest.StdInBuilder.ball;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.frontOfLeftGoal;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.frontOfRightGoal;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.kickoff;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.lowerRightCorner;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.offTable;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.pos;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.upperLeftCorner;
import static detection2.data.Message.message;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Test;

import detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
import detection2.input.ReaderPositionProvider;
import detection2.parser.LineParser;
import detection2.parser.RelativeValueParser;

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

			public static BallPosBuilder upperLeftCorner() {
				return pos(0.0, 0.0);
			}

			public static BallPosBuilder lowerRightCorner() {
				return pos(1.0, 1.0);
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

			public static BallPosBuilder frontOfRightGoal() {
				return kickoff().right(0.3);
			}

			public static BallPosBuilder frontOfLeftGoal() {
				return kickoff().left(0.3);
			}

			public static BallPosBuilder offTable() {
				RelativePosition noBall = RelativePosition.noPosition(0L);
				return pos(noBall.getX(), noBall.getY());
			}

		}

		private long timestamp;
		private final List<String> lines = new ArrayList<>();

		private StdInBuilder(long timestamp) {
			this.timestamp = timestamp;
		}

		public static StdInBuilder ball() {
			return ball(anyTimestamp());
		}

		public static StdInBuilder ball(long timestamp) {
			return messagesStartingAt(timestamp);
		}

		private static StdInBuilder messagesStartingAt(long timestamp) {
			return new StdInBuilder(timestamp);
		}

		private static long anyTimestamp() {
			return 1234;
		}

		private StdInBuilder then() {
			return this;
		}

		private StdInBuilder then(BallPosBuilder ballPosBuilder) {
			return at(ballPosBuilder);
		}

		private StdInBuilder at(BallPosBuilder ballPosBuilder) {
			lines.add(line(timestamp, ballPosBuilder.x, ballPosBuilder.y));
			return this;
		}

		private StdInBuilder thenAfterMillis(long duration) {
			return thenAfter(duration, MILLISECONDS);
		}

		private StdInBuilder thenAfter(long duration, TimeUnit timeUnit) {
			timestamp += timeUnit.toMillis(duration);
			return this;
		}

		private StdInBuilder invalidData() {
			lines.add(line(timestamp, "A", "B"));
			return this;
		}

		private String line(Object... objects) {
			return Arrays.stream(objects).map(String::valueOf).collect(joining(","));
		}

		private StdInBuilder prepareForLeftGoal() {
			return prepateForGoal().at(frontOfLeftGoal());
		}

		private StdInBuilder prepareForRightGoal() {
			return prepateForGoal().at(frontOfRightGoal());
		}

		private StdInBuilder prepateForGoal() {
			return at(kickoff()).thenAfter(100, MILLISECONDS);
		}

		private StdInBuilder score() {
			return offTableFor(2, SECONDS);
		}

		private StdInBuilder offTableFor(int duration, TimeUnit timeUnit) {
			return at(offTable()).thenAfter(duration, timeUnit).then(offTable());
		}

		public StdInBuilder thenCall(Consumer<Consumer<RelativePosition>> setter, Consumer<RelativePosition> c) {
			setter.accept(new Consumer<RelativePosition>() {
				long timestampNow = timestamp;
				private boolean processed;
				private boolean called;

				@Override
				public void accept(RelativePosition pos) {
					if (processed && !called) {
						c.accept(pos);
						called = true;
					}
					if (pos.getTimestamp() == timestampNow) {
						processed = true;
					}
				}
			});
			return this;
		}

		private String[] build() {
			return lines.toArray(new String[lines.size()]);
		}

	}

	private final List<Message> collectedMessages = new ArrayList<>();
	private final Consumer<Message> messageCollector = collectedMessages::add;
	private final GoalDetector.Config goalDetectorConfig = new GoalDetector.Config();
	private Consumer<RelativePosition> inProgressConsumer = p -> {
	};

	private SFTDetection sut;
	private String inputString = "";

	@Test
	public void relativeValuesGetsConvertedToAbsolutesAtKickoff() throws IOException {
		givenATableOfSize(100, 80);
		givenInputToProcessIs(ball().at(kickoff()));
		whenInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(centerX(), centerY());
		thenTheAbsolutePositionOnTheTableIsPublished(100 / 2, 80 / 2);
	}

	@Test
	public void relativeValuesGetsConvertedToAbsolutes() throws IOException {
		givenATableOfSize(100, 80);
		givenInputToProcessIs(ball().at(pos(0.0, 1.0)));
		whenInputWasProcessed();
		thenTheRelativePositionOnTheTableIsPublished(0.0, 1.0);
		thenTheAbsolutePositionOnTheTableIsPublished(0, 80);
	}

	@Test
	public void malformedMessageIsRead() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().invalidData());
		whenInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void onReadingTheNoPositionMessage_noMessageIsSent() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(offTable()));
		whenInputWasProcessed();
		thenNoMessageIsSent();
	}

	@Test
	public void whenTwoPositionsAreRead_VelocityGetsPublished() throws IOException {
		givenATableOfSize(100, 80);
		givenInputToProcessIs(ball().at(anyCorner()).thenAfter(1, SECONDS).at(lowerRightCorner()));
		whenInputWasProcessed();
		thenDistanceInCentimetersAndVelocityArePublished(128.06248474865697, 1.2806248474865697, 4.610249450951652);
	}

	@Test
	public void canDetectGoalOnRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForRightGoal().then().score());
		whenInputWasProcessed();
		thenGoalForTeamIsPublished(0);
		thenGameScoreForTeamIsPublished(0, 1);
	}

	@Test
	public void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().score());
		whenInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForRightGoal().then().at(frontOfRightGoal().left(0.01)).score());
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().at(frontOfLeftGoal().right(0.01)).score());
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void leftHandSideScoresThreeTimes() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/scored", times("1", 3));
		thenPayloadsWithTopicAre("team/score/1", "1", "2", "3");
	}

	@Test
	public void noGoalsIfBallWasNotDetectedAtMiddleLine() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().at(frontOfLeftGoal()).then().score());
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalsIfThereAreNotTwoSecondsWithoutPositions() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		long timeout = SECONDS.toMillis(2);
		givenTimeWithoutBallTilGoal(timeout, MILLISECONDS);
		long oneMsMeforeTimeout = timeout - 1;
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(offTable()).then() //
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(offTable()).then() //
				.prepareForLeftGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(kickoff()).then() //
				.prepareForRightGoal().then(offTable()).thenAfterMillis(oneMsMeforeTimeout).then(kickoff()) //
		);
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void withoutWaitTimeTheGoalDirectlyCounts() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenTimeWithoutBallTilGoal(0, MILLISECONDS);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score());
		whenInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void canRevertGoals() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball().prepareForLeftGoal().then().score().then(anyCorner()));
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("team/score/1", "1", "0");
	}

	@Test
	public void doesSendWinner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenInputWasProcessed();
		thenWinnerAre(1);
	}

	@Test
	public void doesSendDrawWinners() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().then().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score() //
		);
		whenInputWasProcessed();
		thenWinnerAre(0, 1);
	}

	@Test
	public void doesSendGameStart() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()).at(kickoff()));
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/start"), is(""));
	}

	@Test
	public void doesSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenInputToProcessIs(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(5, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/foul"), is(""));
	}

	@Test
	public void doesNotSendFoul() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenInputToProcessIs(ball().at(middlefieldRow) //
				.thenAfter(10, SECONDS).at(middlefieldRow.up(0.49)) //
				.thenAfter(4, SECONDS).at(frontOfLeftGoal()) //
				.thenAfter(1, SECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenInputWasProcessed();
		thenNoMessageWithTopicIsSent("game/foul");
	}

	@Test
	public void doesSendFoulOnlyOnceUntilFoulIsOver() throws IOException {
		givenATableOfAnySize();
		BallPosBuilder middlefieldRow = kickoff().left(0.1);
		givenInputToProcessIs(ball().at(middlefieldRow.up(0.49)) //
				.thenAfter(15, SECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
				.thenAfter(100, MILLISECONDS).at(middlefieldRow.down(0.49)) //
		);
		whenInputWasProcessed();
		assertOneMessageWithPayload(messagesWithTopic("game/foul"), is(""));
	}

	@Test
	public void doesRestartAfterGameEnd() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenInputToProcessIs(ball() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score() //
				//
				.prepareForLeftGoal().score().then() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score().then() //
				.prepareForRightGoal().score() //
		);
		whenInputWasProcessed();
		Predicate<Message> deprecatedTopic = m -> m.getTopic().startsWith("game/score/");

		assertThat("The deprecated topic no more is sent. Please remove the filtering predicate",
				collectedMessages.stream().filter(deprecatedTopic).anyMatch(deprecatedTopic), is(true));

		assertThat(
				collectedMessages(m -> !m.getTopic().startsWith("ball/")).filter(deprecatedTopic.negate())
						.collect(toList()), //
				is(asList( //
						message("game/start", ""), //
						message("team/scored", 1), //
						message("team/score/1", 1), //
						message("team/scored", 0), //
						message("team/score/0", 1), //
						message("team/scored", 1), //
						message("team/score/1", 2), //
						message("team/scored", 0), //
						message("team/score/0", 2), //
						message("team/scored", 1), //
						message("team/score/1", 3), //
						message("team/scored", 0), //
						message("team/score/0", 3), //
						message("team/scored", 1), //
						message("team/score/1", 4), //
						message("team/scored", 0), //
						message("team/score/0", 4), //
						message("team/scored", 1), //
						message("team/score/1", 5), //
						message("team/scored", 0), //
						message("team/score/0", 5), //
						message("game/gameover", winners(0, 1)), //
						message("game/start", ""), //
						message("team/scored", 1), //
						message("team/score/1", 1), //
						message("team/scored", 1), //
						message("team/score/1", 2), //
						message("team/scored", 0), //
						message("team/score/0", 1), //
						message("team/scored", 0), //
						message("team/score/0", 2))));
	}

	@Test
	public void doesSendIdleOn() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true");
	}

	@Test
	public void doesSendIdleOff() throws IOException {
		givenATableOfAnySize();
		givenInputToProcessIs(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true", "false");
	}

	@Test
	public void canResetAgameInPlay() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);

		givenInputToProcessIs(ball(MINUTES.toMillis(15)) //
				.prepareForLeftGoal().score().thenAfter(5, SECONDS) //
				.prepareForLeftGoal().score().thenCall(this::setInProgressConsumer, p -> resetGameAndClearMessages()) //
				.prepareForRightGoal().score().thenAfter(5, SECONDS) //
				.prepareForRightGoal().score() //
		);

		whenInputWasProcessed();
		thenPayloadsWithTopicAre("game/start", "");
		thenPayloadsWithTopicAre("team/score/0", "1", "2");
		thenNoMessageWithTopicIsSent("team/score/1");
	}

	public void setInProgressConsumer(Consumer<RelativePosition> inProgressConsumer) {
		this.inProgressConsumer = inProgressConsumer;
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
		this.sut = detectionOn(new Table(width, height), messageCollector);
	}

	private void givenATableOfAnySize() {
		givenATableOfSize(123, 45);
	}

	private void givenInputToProcessIs(String... messages) {
		inputString = inputString.concat(Arrays.stream(messages).collect(joining("\n")));
	}

	private void givenInputToProcessIs(StdInBuilder builder) {
		givenInputToProcessIs(builder.build());
	}

	private void givenFrontOfGoalPercentage(int frontOfGoalPercentage) {
		this.goalDetectorConfig.frontOfGoalPercentage(frontOfGoalPercentage);
	}

	private void givenTimeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
		this.goalDetectorConfig.timeWithoutBallTilGoal(duration, timeUnit);
	}

	void whenInputWasProcessed() throws IOException {
		sut = sut.withGoalConfig(goalDetectorConfig);
		sut.process(new ReaderPositionProvider(new StringReader(inputString), adapt(new RelativeValueParser())));
	}

	private LineParser adapt(RelativeValueParser delegate) {
		return new LineParser() {
			@Override
			public RelativePosition parse(String line) {
				RelativePosition pos = delegate.parse(line);
				inProgressConsumer.accept(pos);
				return pos;
			}
		};
	}

	private void resetGameAndClearMessages() {
		this.collectedMessages.clear();
		sut.resetGame();
	}

	private void thenTheRelativePositionOnTheTableIsPublished(double x, double y) {
		assertOneMessageWithPayload(messagesWithTopic("ball/position/rel"), is(makePayload(x, y)));
	}

	private void thenTheAbsolutePositionOnTheTableIsPublished(double x, double y) {
		assertOneMessageWithPayload(messagesWithTopic("ball/position/abs"), is(makePayload(x, y)));
	}

	private void thenGoalForTeamIsPublished(int teamid) {
		assertOneMessageWithPayload(messagesWithTopic("team/scored"), is(String.valueOf(teamid)));
	}

	private void thenGameScoreForTeamIsPublished(int teamid, int score) {
		assertOneMessageWithPayload(messagesWithTopic("team/score/" + teamid), is(String.valueOf(score)));
	}

	private void assertOneMessageWithPayload(Stream<Message> messagesWithTopic, Matcher<String> matcher) {
		assertThat(onlyElement(messagesWithTopic).getPayload(), matcher);
	}

	private void thenNoMessageIsSent() {
		thenNoMessageIsSent(a -> true);
	}

	private void thenNoMessageWithTopicIsSent(String topic) {
		thenNoMessageIsSent(m -> m.getTopic().equals(topic));
	}

	private void thenNoMessageIsSent(Predicate<Message> predicate) {
		assertThat(collectedMessages(predicate).collect(toList()), is(emptyList()));
	}

	private Stream<Message> collectedMessages(Predicate<Message> predicate) {
		return collectedMessages.stream().filter(predicate);
	}

	private void thenPayloadsWithTopicAre(String topic, String... payloads) {
		assertThat(payloads(messagesWithTopic(topic)), is(asList(payloads)));
	}

	private void thenWinnerAre(int... winners) {
		thenPayloadsWithTopicAre("game/gameover", winners(winners));
	}

	private String winners(int... winners) {
		return IntStream.of(winners).mapToObj(String::valueOf).collect(joining(","));
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
		return collectedMessages.stream().filter(topic(topic));
	}

	private Predicate<Message> topic(String topic) {
		return m -> m.getTopic().equals(topic);
	}

	private BallPosBuilder anyCorner() {
		return upperLeftCorner();
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
