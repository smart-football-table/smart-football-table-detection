package detection2;

import static detection2.SFTDetection.detectionOn;
import static detection2.SFTDetectionTest.StdInBuilder.ball;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.frontOfLeftGoal;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.frontOfRightGoal;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.kickoff;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.offTable;
import static detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder.pos;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

import detection2.SFTDetectionTest.StdInBuilder.BallPosBuilder;
import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import detection2.detector.GoalDetector;
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

		public StdInBuilder prepareForLeftGoal() {
			return at(kickoff()).then().at(frontOfLeftGoal());
		}

		public StdInBuilder prepareForRightGoal() {
			return at(kickoff()).then().at(frontOfRightGoal());
		}

		public StdInBuilder score() {
			return offTableFor(2, SECONDS);
		}

		private StdInBuilder offTableFor(int duration, TimeUnit timeUnit) {
			return at(offTable()).thenAfter(duration, timeUnit).then(offTable());
		}

		public String[] build() {
			return lines.toArray(new String[lines.size()]);
		}

	}

	private final List<Message> collectedMessages = new ArrayList<>();
	private final Consumer<Message> messageCollector = collectedMessages::add;
	private final GoalDetector.Config goalDetectorConfig = new GoalDetector.Config();

	private SFTDetection sut;
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
		givenStdInContains(ball().prepareForRightGoal().then().score());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(0);
		thenGameScoreForTeamIsPublished(0, 1);
	}

	@Test
	public void canDetectGoalOnLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForLeftGoal().score());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalRightHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForRightGoal().then().at(frontOfRightGoal().left(0.01)).score());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalIfBallWasNotInFrontOfGoalLeftHandSide() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForLeftGoal().then().at(frontOfLeftGoal().right(0.01)).score());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void leftHandSideScoresThreeTimes() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("team/scored", times("1", 3));
		thenPayloadsWithTopicAre("game/score/1", "1", "2", "3");
	}

	@Test
	public void noGoalsIfBallWasNotDetectedAtMiddleLine() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().at(frontOfLeftGoal()).then().score());
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void noGoalsIfThereAreNotTwoSecondsWithoutPositions() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		long timeout = SECONDS.toMillis(2);
		givenTimeWithoutBallTilGoal(timeout, MILLISECONDS);
		long justNotTimeout = timeout - 1;
		givenStdInContains(ball() //
				.prepareForLeftGoal().then(offTable()).thenAfter(justNotTimeout, MILLISECONDS).then(offTable()).then() //
				.prepareForRightGoal().then(offTable()).thenAfter(justNotTimeout, MILLISECONDS).then(offTable()).then() //
				.prepareForLeftGoal().then(offTable()).thenAfter(justNotTimeout, MILLISECONDS).then(kickoff()).then() //
				.prepareForRightGoal().then(offTable()).thenAfter(justNotTimeout, MILLISECONDS).then(kickoff()) //
		);
		whenStdInInputWasProcessed();
		thenNoMessageWithTopicIsSent("team/scored");
	}

	@Test
	public void withoutWaitTimeTheGoalDirectlyCounts() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenTimeWithoutBallTilGoal(0, MILLISECONDS);
		givenStdInContains(ball().prepareForLeftGoal().then().score());
		whenStdInInputWasProcessed();
		thenGoalForTeamIsPublished(1);
	}

	@Test
	public void canRevertGoals() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball().prepareForLeftGoal().then().score().then(pos(0.0, 0.0)));
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/score/1", "1", "0");
	}

	@Test
	public void doesSendWinner() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score().then() //
				.prepareForLeftGoal().then().score() //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/gameover", "1");
	}

	@Test
	public void doesSendDrawWinners() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball() //
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
		whenStdInInputWasProcessed();
		assertThat(collectedMessages.stream().filter(m -> !m.getTopic().startsWith("ball/")).collect(toList()),
				is(asList( //
						Message.message("game/start", ""), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 1), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 1), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 2), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 2), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 3), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 3), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 4), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 4), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 5), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 5), //
						Message.message("game/gameover", "0,1"), //
						Message.message("game/start", ""), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 1), //
						Message.message("team/scored", 1), //
						Message.message("game/score/1", 2), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 1), //
						Message.message("team/scored", 0), //
						Message.message("game/score/0", 2))));
	}

	@Test
	public void doesSendIdleOn() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, SECONDS).at(offTable()) //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true");
	}

	@Test
	public void doesSendIdleOff() throws IOException {
		givenATableOfAnySize();
		givenStdInContains(ball().at(kickoff()) //
				.thenAfter(1, SECONDS).at(offTable()) //
				.thenAfter(1, MINUTES).at(offTable()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
				.thenAfter(1, SECONDS).at(kickoff()) //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/idle", "true", "false");
	}

	@Test
	@Ignore
	public void canResetAgameInPlay() throws IOException {
		givenATableOfAnySize();
		givenFrontOfGoalPercentage(20);
		givenStdInContains(ball() //
				.prepareForLeftGoal().score().then() //
				.prepareForRightGoal().score() //
		);
		whenStdInInputWasProcessed();
		thenPayloadsWithTopicAre("game/start", "", "");
		thenPayloadsWithTopicAre("game/gameover", "");
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

	void whenStdInInputWasProcessed() throws IOException {
		sut.getGame().goalDetectorConfig(goalDetectorConfig);
		sut.process(new RelativeValueParser(), is);
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
		assertOneMessageWithPayload(messagesWithTopic("game/score/" + teamid), is(String.valueOf(score)));
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
		assertThat(collectedMessages.stream().filter(predicate).collect(toList()), is(emptyList()));
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
		return collectedMessages.stream().filter(m -> m.getTopic().equals(topic));
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
