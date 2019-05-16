package detection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static detection.DetectionExamples.GameSituationBuilder.gameSituation;
import static detection.Topic.BALL_POSITION_ABS;
import static detection.Topic.BALL_POSITION_REL;
import static detection.Topic.BALL_VELOCITY_KMH;
import static detection.Topic.BALL_VELOCITY_MPS;
import static detection.Topic.GAME_FOUL;
import static detection.Topic.GAME_IDLE;
import static detection.Topic.TEAM_ID_LEFT;
import static detection.Topic.TEAM_ID_RIGHT;
import static detection.Topic.TEAM_SCORED;
import static detection.Topic.TEAM_SCORE_LEFT;
import static detection.Topic.TEAM_SCORE_RIGHT;
import static detection.data.position.RelativePosition.create;
import static detection.data.position.RelativePosition.noPosition;
import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static net.jqwik.api.Arbitraries.doubles;
import static net.jqwik.api.Arbitraries.integers;
import static net.jqwik.api.Arbitraries.longs;
import static net.jqwik.api.Combinators.combine;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hamcrest.Matcher;

import detection.data.Message;
import detection.data.Table;
import detection.data.position.RelativePosition;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Statistics;
import net.jqwik.api.Tuple;
import net.jqwik.api.arbitraries.DoubleArbitrary;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import net.jqwik.api.arbitraries.LongArbitrary;
import net.jqwik.api.arbitraries.SizableArbitrary;

class DetectionExamples {

	@Property
	void ballOnTableNeverWillRaiseTeamScoreOrTeamsScoredEvents(
			@ForAll("positionsOnTable") List<RelativePosition> positions, @ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(ignoreAllButNot(TEAM_SCORE_LEFT, TEAM_SCORED)).collect(toList()),
				is(empty()));
	}

	@Property
	void leftGoalProducesTeamScoredMessage(@ForAll("goalSituationsLeft") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(TEAM_SCORED)).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_LEFT)));
	}

	@Property
	void leftGoalsProducesTeamScoreMessage(@ForAll("goalSituationsLeft") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_LEFT)).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void rightGoalProducesTeamScoredMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(TEAM_SCORED)).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_RIGHT)));
	}

	@Property
	void rightGoalProducesTeamScoreMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_RIGHT)).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterALeftHandGoalTheGoalGetsRever(
			@ForAll("leftGoalsToReverse") List<RelativePosition> positions, @ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_LEFT)).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterARightHandGoalTheGoalGetsReverted(
			@ForAll("rightGoalsToReverse") List<RelativePosition> positions, @ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_RIGHT)).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
	}

	@Property
	void whenBallDoesNotMoveForMoreThanOneMinuteTheGameGoesToIdleMode(
			@ForAll("idleWhereBallMaybeGone") List<RelativePosition> positions, @ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(GAME_IDLE).and(payloadIs("true"))).count(), is(1L));
	}

	private Predicate<Message> topicIs(Topic topic) {
		return topic.getPredicate();
	}

	@Property
	// TODO could produce falls positives: some random data could contain fouls
	void noIdleWithoutFoul(@ForAll("idle") List<RelativePosition> positions, @ForAll("table") Table table) {
		statistics(positions);
		List<Message> messages = process(positions, table).collect(toList());
		Map<String, Long> data = new HashMap<>();
		data.put("foul", messages.stream().filter(topicIs(GAME_FOUL)).count());
		data.put("idleOn", messages.stream().filter(topicIs(GAME_IDLE).and(payloadIs("true"))).count());
		data.put("idleOff", messages.stream().filter(topicIs(GAME_IDLE).and(payloadIs("false"))).count());
		assertThat("Amount of messages not equal" + data, new HashSet<>(data.values()).size() == 1, is(true));
	}

	// TODO remove when generator is fixed
	@Deprecated
	private boolean ballWasOffTableForAtLeast(List<RelativePosition> positions, int duration, TimeUnit timeUnit) {
		int firstOffTable = findOffTable(positions);
		return positions.get(findNotOffTable(positions, firstOffTable) - 1).getTimestamp() //
				- positions.get(firstOffTable).getTimestamp() >= timeUnit.toMillis(duration);
	}

	private int findOffTable(List<RelativePosition> positions) {
		return findFirst(positions, 0, i -> positions.get(i).isNull());
	}

	private int findNotOffTable(List<RelativePosition> positions, int start) {
		return findFirst(positions, start, i -> !positions.get(i).isNull());
	}

	private int findFirst(List<RelativePosition> positions, int start, IntPredicate predicate) {
		int lastIdx = positions.size() - 1;
		return rangeClosed(start, lastIdx).filter(predicate).findFirst().orElse(lastIdx);
	}

	@Property
	boolean ballPositionRelForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		return process(positions, table).filter(topicIs(BALL_POSITION_REL)).count() == positions.size();
	}

	@Property
	void allRelPositionAreBetween0And1(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(BALL_POSITION_REL)).map(Message::getPayload).collect(toList()),
				everyItem( //
						allOf(hasJsonNumberBetween("x", 0, 1), hasJsonNumberBetween("y", 0, 1))));
	}

	@Property
	boolean ballPositionAbsForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		return process(positions, table).filter(topicIs(BALL_POSITION_ABS)).count() == positions.size();
	}

	@Property
	void allAbsPositionAreBetween0AndTableSize(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(BALL_POSITION_ABS)).map(Message::getPayload).collect(toList()),
				everyItem( //
						allOf(hasJsonNumberBetween("x", 0, table.getWidth()),
								hasJsonNumberBetween("y", 0, table.getHeight()))));
	}

	@Property
	boolean ballVelocityKmhForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		return process(positions, table).filter(topicIs(BALL_VELOCITY_KMH)).count() == positions.size() - 1;
	}

	@Property
	void allBallPositionVelocitiesArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(BALL_VELOCITY_KMH)).map(Message::getPayload)
				.map(Double::parseDouble).collect(toList()), everyItem(is(positive())));
	}

	@Property
	boolean ballVelocityMpsForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		return process(positions, table).filter(topicIs(BALL_VELOCITY_MPS)).count() == positions.size() - 1;
	}

	@Property
	void ballVelocityMpsForArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(BALL_VELOCITY_MPS)).map(Message::getPayload)
				.map(Double::parseDouble).collect(toList()), everyItem(is(positive())));
	}

	private void statistics(Collection<?> col) {
		Statistics.collect(col.size() < 10 ? "<10" : col.size() < 30 ? "<30" : ">=30");
	}

	private Matcher<Object> hasJsonNumberBetween(String name, int min, int max) {
		return allOf( //
				isJson(withJsonPath("$." + name, instanceOf(Number.class))), //
				isJson(withJsonPath("$[?(@." + name + " >= " + min + " && @." + name + " <= " + max + ")]")) //
		);
	}

	private Stream<Message> process(List<RelativePosition> positions, Table table) {
		List<Message> messages = new ArrayList<>();
		new SFTDetection(table, messages::add).process(positions.stream());
		return messages.stream();
	}

	private Predicate<Message> ignoreAllButNot(Topic... topics) {
		return anyOfTopic(EnumSet.allOf(Topic.class).stream().filter(f -> !Arrays.asList(topics).contains(f)));
	}

	private Predicate<Message> anyOfTopic(Stream<Topic> topics) {
		return anyOf(topics.map(Topic::getPredicate));
	}

	private Predicate<Message> anyOf(Stream<Predicate<Message>> predicates) {
		return predicates.reduce(Predicate::or).map(Predicate::negate).orElse(m -> true);
	}

	static Predicate<Message> topicStartsWith(String topic) {
		return m -> m.getTopic().startsWith(topic);
	}

	static Predicate<Message> payloadIs(String value) {
		return m -> m.getPayload().equals(value);
	}

	private Matcher<Double> positive() {
		return greaterThanOrEqualTo(0.0);
	}

	@Provide
	Arbitrary<Table> table() {
		return combine( //
				integers().greaterOrEqual(1), //
				integers().greaterOrEqual(1)) //
						.as((width, height) -> new Table(width, height));
	}

	@Provide
	Arbitrary<List<RelativePosition>> positionsOnTable() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.anywhereOnTableSizeable().elementsMin(2).addSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> goalSituationsLeft() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.addKickoffSequence() //
				.addScoreLeftSequence() //
				.addBallNotInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> goalSituationsRight() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.addKickoffSequence() //
				.addScoreRightSequence() //
				.addBallNotInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> leftGoalsToReverse() {
		return anyTimestamp(ts -> {
			return a(gameSituation(ts) //
					.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
					.addKickoffSequence() //
					.addScoreLeftSequence() //
					.addBallInCornerSequence());
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> rightGoalsToReverse() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.addKickoffSequence() //
				.addScoreRightSequence() //
				.addBallInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> idle() {
		return anyTimestamp(ts -> {
			return a(gameSituation(ts) //
					.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
					.addIdleSequence());
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> idleWhereBallMaybeGone() {
		return anyTimestamp(ts -> {
			return a(gameSituation(ts) //
					.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
					.addIdleSequenceBallMaybeGone());
		});
	}

	private LongArbitrary defaultFrequency() {
		return longs().between(5, 1000);
	}

	private Arbitrary<List<RelativePosition>> a(GameSituationBuilder builder) {
		return builder.build();
	}

	private Arbitrary<List<RelativePosition>> anyTimestamp(
			Function<AtomicLong, Arbitrary<List<RelativePosition>>> mapper) {
		return longs().map(AtomicLong::new).flatMap(mapper);
	}

	static class GameSituationBuilder {

		private static final double TABLE_MIN = 0.0;
		private static final double TABLE_MAX = 1.0;
		private static final double CENTER = TABLE_MAX / 2;

		private static final double MIDDLE_LINE_DRIFT = 0.05;
		private static final double FRONT_OF_GOAL_DRIFT = 0.3;
		private static final double CORNER_DRIFT = 0.01;

		class Sizeable {

			private Arbitrary<RelativePosition> arbitrary;
			private Integer minSize;
			private boolean unique;

			public Sizeable(Arbitrary<RelativePosition> arbitrary) {
				this.arbitrary = arbitrary;
			}

			public Sizeable elementsMin(int minSize) {
				this.minSize = minSize;
				return this;
			}

			public GameSituationBuilder addSequence() {
				SizableArbitrary<List<RelativePosition>> list = arbitrary.list();
				Arbitrary<List<RelativePosition>> seq = minSize == null ? list : list.ofMinSize(minSize);
				seq = unique ? seq.unique() : seq;
				return GameSituationBuilder.this.addSequence(seq);
			}

			public Sizeable filter(Predicate<RelativePosition> predicate) {
				arbitrary = arbitrary.filter(predicate);
				return this;
			}

			public Sizeable unique() {
				this.unique = true;
				return this;
			}

		}

		private final AtomicLong timestamp;
		private Arbitrary<Long> samplingFrequency = longs();
		private final List<Arbitrary<List<RelativePosition>>> arbitraries = new ArrayList<>();

		public GameSituationBuilder(AtomicLong timestamp) {
			this.timestamp = timestamp;
		}

		public GameSituationBuilder withSamplingFrequency(TimeUnit timeUnit, Arbitrary<Long> samplingFrequencyInMs) {
			this.samplingFrequency = samplingFrequencyInMs.map(f -> MILLISECONDS.convert(f, timeUnit));
			return this;
		}

		public Arbitrary<List<RelativePosition>> build() {
			return join(arbitraries);
		}

		private static Arbitrary<List<RelativePosition>> join(List<Arbitrary<List<RelativePosition>>> arbitraries) {
			return combine(arbitraries).as(p -> p.stream().flatMap(Collection::stream).collect(toList()));
		}

		public static GameSituationBuilder gameSituation(AtomicLong timestamp) {
			return new GameSituationBuilder(timestamp);
		}

		public GameSituationBuilder addKickoffSequence() {
			return addSequence(kickoffPositions(timestamp)).anywhereOnTableSizeable().addSequence();
		}

		private static boolean isCorner(RelativePosition pos) {
			return CENTER + abs(CENTER - pos.getX()) >= TABLE_MAX - CORNER_DRIFT
					&& CENTER + abs(CENTER - pos.getY()) >= TABLE_MAX - CORNER_DRIFT;
		}

		private Arbitrary<RelativePosition> offTablePosition(AtomicLong timestamp) {
			return samplingFrequency.map(millis -> noPosition(timestamp.addAndGet(millis)));
		}

		private Sizeable anywhereOnTableSizeable() {
			return asSizeable(combine(samplingFrequency, wholeTable(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y)));
		}

		private Sizeable asSizeable(Arbitrary<RelativePosition> as) {
			return new Sizeable(as);
		}

		public GameSituationBuilder addScoreLeftSequence() {
			return addSequence(prepareLeftGoal(timestamp)).addSequence(offTablePositions(timestamp));
		}

		public GameSituationBuilder addScoreRightSequence() {
			return addSequence(prepareRightGoal(timestamp)).addSequence(offTablePositions(timestamp));
		}

		private Arbitrary<List<RelativePosition>> offTablePositions(AtomicLong timestamp) {
			// TODO create as many positions as needed (2000ms between first and last)
			return offTablePosition(timestamp).list().ofSize(10);
		}

		public GameSituationBuilder addBallInCornerSequence() {
			return addSequence(corner(timestamp)).anywhereOnTableSizeable().addSequence();
		}

		public GameSituationBuilder addBallNotInCornerSequence() {
			return anywhereOnTableSizeable().filter(p -> !isCorner(p)).elementsMin(1).addSequence()
					.anywhereOnTableSizeable().addSequence();
		}

		private GameSituationBuilder addIdleSequence() {
			return idleSequence(noMoveForAtLeast(1, MINUTES));
		}

		private GameSituationBuilder addIdleSequenceBallMaybeGone() {
			return idleSequence(noMoveOrNoBallForAtLeast(1, MINUTES));
		}

		private GameSituationBuilder idleSequence(Arbitrary<List<RelativePosition>> arbitrary) {
			// add at least two unique elements to ensure idle is over afterwards
			return anywhereOnTableSizeable().addSequence() //
					.addSequence(arbitrary).anywhereOnTableSizeable().elementsMin(2).unique().addSequence();
		}

		public GameSituationBuilder addSequence(Arbitrary<List<RelativePosition>> arbitrary) {
			arbitraries.add(arbitrary);
			return this;
		}

		private Arbitrary<List<RelativePosition>> kickoffPositions(AtomicLong timestamp) {
			return atMiddleLine(timestamp).list().ofMinSize(1);
		}

		private Arbitrary<List<RelativePosition>> prepareLeftGoal(AtomicLong timestamp) {
			return frontOfLeftGoal(timestamp).list().ofMinSize(1);
		}

		private Arbitrary<List<RelativePosition>> prepareRightGoal(AtomicLong timestamp) {
			return frontOfRightGoal(timestamp).list().ofMinSize(1);
		}

		private Arbitrary<RelativePosition> frontOfLeftGoal(AtomicLong timestamp) {
			return combine(samplingFrequency, frontOfLeftGoal(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		private Arbitrary<RelativePosition> frontOfRightGoal(AtomicLong timestamp) {
			return combine(samplingFrequency, frontOfRightGoal(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		private Arbitrary<RelativePosition> atMiddleLine(AtomicLong timestamp) {
			return combine(samplingFrequency, middleLine(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		private Arbitrary<List<RelativePosition>> corner(AtomicLong timestamp) {
			return combine(samplingFrequency, corner(), corner(), bool(), bool()) //
					.as((millis, x, y, swapX, swapY) //
					-> create(timestamp.addAndGet(millis), possiblySwap(x, swapX), possiblySwap(y, swapY))).list()
					.ofMinSize(1);
		}

		private Arbitrary<List<RelativePosition>> noMoveOrNoBallForAtLeast(int duration, TimeUnit minutes) {
			return Arbitraries.frequency( //
					Tuple.of(90, noBallForAtLeast(duration, minutes)), //
					Tuple.of(90, noMoveForAtLeast(duration, minutes))).flatMap(identity() //
			);
		}

		private Arbitrary<List<RelativePosition>> noMoveForAtLeast(int duration, TimeUnit minutes) {
			// TODO depend on duration
			IntegerArbitrary amount = integers().between(100, 1_000);
			Arbitrary<Long> xxxx = longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10));
			return combine(xxxx, amount, wholeTable(), wholeTable()) //
					.as((millis, count, x, y) //
					-> range(0, count).mapToObj(ignore -> create(timestamp.addAndGet(millis), x, y)).collect(toList()));
		}

		private Arbitrary<List<RelativePosition>> noBallForAtLeast(int duration, TimeUnit minutes) {
			// TODO depend on duration
			IntegerArbitrary amount = integers().between(100, 1_000);
			Arbitrary<Long> xxxx = longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10));
			return combine(xxxx, amount) //
					.as((millis, count) //
					-> range(0, count).mapToObj(ignore -> noPosition(timestamp.addAndGet(millis))).collect(toList()));
		}

		private static double possiblySwap(double value, boolean swap) {
			return swap ? swap(value) : value;
		}

		private static double swap(double value) {
			return TABLE_MAX - value;
		}

		private static Arbitrary<Double> corner() {
			return doubles().between(TABLE_MAX - CORNER_DRIFT, TABLE_MAX);
		}

		private static DoubleArbitrary wholeTable() {
			return doubles().between(TABLE_MIN, TABLE_MAX);
		}

		private static DoubleArbitrary middleLine() {
			return doubles().between(CENTER - MIDDLE_LINE_DRIFT, CENTER + MIDDLE_LINE_DRIFT);
		}

		private static Arbitrary<Double> frontOfLeftGoal() {
			return frontOfGoal();
		}

		private static Arbitrary<Double> frontOfRightGoal() {
			return frontOfGoal().map(GameSituationBuilder::swap);
		}

		private static Arbitrary<Double> frontOfGoal() {
			return doubles().between(0, FRONT_OF_GOAL_DRIFT);
		}

		private static Arbitrary<Boolean> bool() {
			return Arbitraries.of(true, false);
		}

	}

}
