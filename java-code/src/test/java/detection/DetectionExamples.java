package detection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static detection.DetectionExamples.GameSequenceBuilder.gameSequence;
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
import static net.jqwik.api.Arbitraries.constant;
import static net.jqwik.api.Arbitraries.doubles;
import static net.jqwik.api.Arbitraries.frequency;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hamcrest.Matcher;

import detection.GameSituationExamples.PositionSequenceBuilder;
import detection.data.Message;
import detection.data.Table;
import detection.data.position.RelativePosition;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Statistics;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;
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
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(TEAM_SCORED)).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_LEFT)));
	}

	@Property
	void leftGoalsProducesTeamScoreMessage(@ForAll("goalSituationsLeft") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_LEFT)).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void rightGoalProducesTeamScoredMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(TEAM_SCORED)).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_RIGHT)));
	}

	@Property
	void rightGoalProducesTeamScoreMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_RIGHT)).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterALeftHandGoalTheGoalGetsRever(
			@ForAll("leftGoalsToReverse") List<RelativePosition> positions, @ForAll("table") Table table) {
		statistics(positions);
		assertThat(
				process(positions, table).filter(topicIs(TEAM_SCORE_LEFT)).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterARightHandGoalTheGoalGetsReverted(
			@ForAll("rightGoalsToReverse") List<RelativePosition> positions, @ForAll("table") Table table) {
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
		return gameSequence().withSamplingFrequency(defaultFrequency()) //
				.addSequence(anyPosition().forDuration(longs().between(1, 20), SECONDS)) //
				.build();
	}

	private PositionSequenceBuilder anyPosition() {
		return position(wholeTable(), wholeTable());
	}

	private PositionSequenceBuilder position(DoubleArbitrary xPosition, DoubleArbitrary yPosition) {
		return new PositionSequenceBuilder(
				combine(xPosition, yPosition).as((x, y) -> ts -> RelativePosition.create(ts, x, y)));
	}

	private static DoubleArbitrary wholeTable() {
		return doubles().between(0, 1);
	}

	@Provide
	private Arbitrary<List<RelativePosition>> goalSituationsLeft() {
		return anyTimestamp(ts -> //
		gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts).addScoreLeftSequence() //
				.addBallNotInCornerSequence().build());
	}

	@Provide
	private Arbitrary<List<RelativePosition>> goalSituationsRight() {
		return anyTimestamp(ts -> //
		gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts)
				.addScoreRightSequence() //
				.addBallNotInCornerSequence().build());
	}

	@Provide
	Arbitrary<List<RelativePosition>> leftGoalsToReverse() {
		return anyTimestamp(ts -> {
			return a(gameSituation(ts) //
					.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
					.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts)
					.addScoreLeftSequence() //
					.addBallInCornerSequence());
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> rightGoalsToReverse() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.withSamplingFrequency(MILLISECONDS, defaultFrequency()) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts)
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
		return longs().between(0, Long.MAX_VALUE / 2).map(AtomicLong::new).flatMap(mapper);
	}

	static class GameSituationBuilder {

		private static final double TABLE_MIN = 0.0;
		private static final double TABLE_MAX = 1.0;
		private static final double CENTER = TABLE_MAX / 2;

		private static final double MIDDLE_LINE_DRIFT = 0.05;
		private static final double FRONT_OF_GOAL_DRIFT = 0.3;
		private static final double CORNER_DRIFT = 0.01;

		private class DurationSequence extends Sequence {

			private final Arbitrary<Long> forDuration;

			public DurationSequence(Arbitrary<RelativePosition> base, Arbitrary<Long> between, TimeUnit timeUnit) {
				super(base);
				forDuration = between.map(timeUnit::toMillis);
			}

			@Override
			GameSituationBuilder add(AtomicLong timestamp) {
				return addSequence(collect(base));
			}

			private Arbitrary<List<RelativePosition>> collect(Arbitrary<RelativePosition> positionArbitrary) {
				return forDuration.flatMap(minDuration -> arbitraryCollect(positionArbitrary,
						positions -> durationReached(positions, minDuration)));
			}

			private boolean durationReached(List<RelativePosition> positions, long minDuration) {
				return duration(positions) >= minDuration;
			}

			private long duration(List<RelativePosition> positions) {
				if (positions.isEmpty()) {
					return 0;
				}
				RelativePosition first = positions.get(0);
				RelativePosition last = positions.get(positions.size() - 1);
				return last.getTimestamp() - first.getTimestamp();
			}

			private <T> Arbitrary<List<T>> arbitraryCollect(Arbitrary<T> elementArbitrary, Predicate<List<T>> until) {
				return new ArbitraryCollect<>(elementArbitrary, until);
			}

		}

		private class Sequence {

			final Arbitrary<RelativePosition> base;

			public Sequence(Arbitrary<RelativePosition> base) {
				this.base = base;
			}

			GameSituationBuilder add(AtomicLong timestamp) {
				return addSequence(base.list().ofMinSize(1));
			}

			private Sequence ofDuration(Arbitrary<Long> between, TimeUnit timeUnit) {
				return new DurationSequence(base, between, timeUnit);
			}

		}

		class Sizeable {

			private Arbitrary<RelativePosition> arbitrary;
			private Integer minSize;
			private boolean unique;

			private Sizeable(Arbitrary<RelativePosition> arbitrary) {
				this.arbitrary = arbitrary;
			}

			private Sizeable elementsMin(int minSize) {
				this.minSize = minSize;
				return this;
			}

			private GameSituationBuilder addSequence() {
				SizableArbitrary<List<RelativePosition>> list = arbitrary.list();
				Arbitrary<List<RelativePosition>> seq = minSize == null ? list : list.ofMinSize(minSize);
				seq = unique ? seq.unique() : seq;
				return GameSituationBuilder.this.addSequence(seq);
			}

			private Sizeable filter(Predicate<RelativePosition> predicate) {
				arbitrary = arbitrary.filter(predicate);
				return this;
			}

			private Sizeable unique() {
				this.unique = true;
				return this;
			}

		}

		private final AtomicLong timestamp;
		private Arbitrary<Long> samplingFrequency = longs();
		private final List<Arbitrary<List<RelativePosition>>> arbitraries = new ArrayList<>();

		private GameSituationBuilder(AtomicLong timestamp) {
			this.timestamp = timestamp;
		}

		private GameSituationBuilder withSamplingFrequency(TimeUnit timeUnit, Arbitrary<Long> samplingFrequency) {
			this.samplingFrequency = samplingFrequency.map(f -> MILLISECONDS.convert(f, timeUnit));
			return this;
		}

		private Arbitrary<List<RelativePosition>> build() {
			return join(arbitraries);
		}

		private static Arbitrary<List<RelativePosition>> join(List<Arbitrary<List<RelativePosition>>> arbitraries) {
			return combine(arbitraries).as(p -> p.stream().flatMap(Collection::stream).collect(toList()));
		}

		static GameSituationBuilder gameSituation(AtomicLong timestamp) {
			return new GameSituationBuilder(timestamp);
		}

		private static boolean isCorner(RelativePosition pos) {
			return CENTER + abs(CENTER - pos.getX()) >= TABLE_MAX - CORNER_DRIFT
					&& CENTER + abs(CENTER - pos.getY()) >= TABLE_MAX - CORNER_DRIFT;
		}

		private Sizeable anywhereOnTableSizeable() {
			return asSizeable(combine(samplingFrequency, wholeTable(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y)));
		}

		private Sizeable asSizeable(Arbitrary<RelativePosition> as) {
			return new Sizeable(as);
		}

		private GameSituationBuilder addScoreLeftSequence() {
			return addSequence(prepareLeftGoal(timestamp)).offTablePositions(timestamp);
		}

		private GameSituationBuilder addScoreRightSequence() {
			return addSequence(prepareRightGoal(timestamp)).offTablePositions(timestamp);
		}

		private GameSituationBuilder offTablePositions(AtomicLong timestamp) {
			return offTableSequence().ofDuration(longs().between(2, 15), SECONDS).add(timestamp);
		}

		private GameSituationBuilder addBallInCornerSequence() {
			return addSequence(corner(timestamp)).anywhereOnTableSizeable().addSequence();
		}

		private GameSituationBuilder addBallNotInCornerSequence() {
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

		private GameSituationBuilder addSequence(Arbitrary<List<RelativePosition>> arbitrary) {
			arbitraries.add(arbitrary);
			return this;
		}

		private Sequence aKickoffSequence() {
			return new Sequence(middleLinePositions(timestamp));
		}

		private Arbitrary<RelativePosition> middleLinePositions(AtomicLong timestamp) {
			return combine(samplingFrequency, middleLine(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		private Sequence offTableSequence() {
			return new Sequence(samplingFrequency.map(millis -> noPosition(timestamp.addAndGet(millis))));
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

		private Arbitrary<List<RelativePosition>> corner(AtomicLong timestamp) {
			return combine(samplingFrequency, corner(), corner(), bool(), bool()) //
					.as((millis, x, y, swapX, swapY) //
					-> create(timestamp.addAndGet(millis), possiblySwap(x, swapX), possiblySwap(y, swapY))).list()
					.ofMinSize(1);
		}

		private Arbitrary<List<RelativePosition>> noMoveOrNoBallForAtLeast(int duration, TimeUnit minutes) {
			return frequency( //
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

	static class GameSequenceBuilder {

		private Arbitrary<Long> samplingFrequency = constant(10L);
		private List<PositionSequenceBuilder> sequences = new ArrayList<>();

		static GameSequenceBuilder gameSequence() {
			return new GameSequenceBuilder();
		}

		GameSequenceBuilder withSamplingFrequency(TimeUnit timeUnit, Arbitrary<Long> samplingFrequency) {
			return withSamplingFrequency(samplingFrequency.map(f -> MILLISECONDS.convert(f, timeUnit)));
		}

		GameSequenceBuilder withSamplingFrequency(long samplingFrequencyMillis) {
			return withSamplingFrequency(constant(samplingFrequencyMillis));
		}

		GameSequenceBuilder withSamplingFrequency(Arbitrary<Long> samplingFrequency) {
			this.samplingFrequency = samplingFrequency;
			return this;
		}

		private GameSequenceBuilder addSequence(PositionSequenceBuilder positionSequence) {
			sequences.add(positionSequence);
			return this;
		}

		private Arbitrary<List<RelativePosition>> build() {
			return build(constant(0L));
		}

		private Arbitrary<List<RelativePosition>> build(Arbitrary<Long> initialTimestampArbitrary) {
			List<Arbitrary<List<Tuple2<Long, Function<Long, RelativePosition>>>>> sequenceArbitraries = sequences
					.stream().map(sequence -> sequence.build(samplingFrequency)).collect(toList());

			return initialTimestampArbitrary.flatMap(initialTimestamp -> combine(sequenceArbitraries)
					.as(tuplesLists -> generateSituation(initialTimestamp, tuplesLists)));
		}

		private List<RelativePosition> generateSituation(Long initialTimestamp,
				List<List<Tuple2<Long, Function<Long, RelativePosition>>>> listOfTupleLists) {
			List<Tuple2<Long, Function<Long, RelativePosition>>> flattenedTupleList = flatten(listOfTupleLists);

			AtomicLong timestamp = new AtomicLong(initialTimestamp);
			return flattenedTupleList.stream().map(tuple -> tuple.get2().apply(timestamp.getAndAdd(tuple.get1())))
					.collect(toList());
		}

		static <T> List<T> flatten(List<List<T>> listOfLists) {
			return listOfLists.stream().flatMap(List<T>::stream).collect(toList());
		}

	}

}
