package detection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static detection.DetectionExamples.GameSequenceBuilder.gameSequence;
import static detection.DetectionExamples.GameSituationBuilder.gameSituation;
import static detection.Topic.BALL_DISTANCE_CM;
import static detection.Topic.BALL_OVERALL_DISTANCE_CM;
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
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;

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
	// TODO could produce falls positives: random data could contain fouls
	void noIdleWithoutFoul(@ForAll("idle") List<RelativePosition> positions, @ForAll("table") Table table) {
		statistics(positions);
		List<Message> messages = process(positions, table).collect(toList());
		Map<String, Long> counts = new HashMap<>();
		counts.put("foul", messages.stream().filter(topicIs(GAME_FOUL)).count());
		counts.put("idleOn", messages.stream().filter(topicIs(GAME_IDLE).and(payloadIs("true"))).count());
		counts.put("idleOff", messages.stream().filter(topicIs(GAME_IDLE).and(payloadIs("false"))).count());
		assertThat("Amount of messages not equal" + counts, new HashSet<>(counts.values()).size() == 1, is(true));
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

	@Property
	void ballDistanceForArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(topicIs(BALL_DISTANCE_CM)).map(Message::getPayload)
				.map(Double::parseDouble).collect(toList()), everyItem(is(positive())));
	}

	@Property
	void forEachOverallDistanceThereIsASingleDistance(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		List<Message> processed = process(positions, table).collect(toList());
		assertThat(processed.stream().filter(topicIs(BALL_OVERALL_DISTANCE_CM)).count(),
				is(processed.stream().filter(topicIs(BALL_DISTANCE_CM)).count()));
	}

	@Property
	void overallDistanceIsSumOfSingleDistances(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		List<Message> processed = process(positions, table).collect(toList());
		OfDouble singles = doublePayload(processed, topicIs(BALL_DISTANCE_CM)).iterator();
		OfDouble overalls = doublePayload(processed, topicIs(BALL_OVERALL_DISTANCE_CM)).iterator();

		double sum = 0.0;
		while (singles.hasNext() && overalls.hasNext()) {
			assertThat(overalls.nextDouble(), is(sum += singles.nextDouble()));
		}
		assertThat(singles.hasNext(), is(overalls.hasNext()));
	}

	private void statistics(Collection<?> col) {
		Statistics.collect(col.size() < 50 ? "<50" : col.size() < 100 ? "<100" : ">=100");
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

	static Predicate<Message> payloadIs(String value) {
		return m -> m.getPayload().equals(value);
	}

	DoubleStream doublePayload(List<Message> messages, Predicate<Message> filter) {
		return doublePayload(messages.stream(), filter);
	}

	DoubleStream doublePayload(Stream<Message> messages, Predicate<Message> filter) {
		return messages.filter(filter).map(Message::getPayload).mapToDouble(Double::parseDouble);
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
		return gameSequence() //
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
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts) //
				.addScoreLeftSequence() //
				.addBallNotInCornerSequence().build());
	}

	@Provide
	private Arbitrary<List<RelativePosition>> goalSituationsRight() {
		return anyTimestamp(ts -> //
		gameSituation(ts) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts) //
				.addScoreRightSequence() //
				.addBallNotInCornerSequence().build());
	}

	@Provide
	Arbitrary<List<RelativePosition>> leftGoalsToReverse() {
		return anyTimestamp(ts -> {
			return a(gameSituation(ts) //
					.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts)
					.addScoreLeftSequence() //
					.addBallInCornerSequence());
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> rightGoalsToReverse() {
		return anyTimestamp(ts -> a(gameSituation(ts) //
				.aKickoffSequence().ofDuration(longs().between(1, 1_000), MILLISECONDS).add(ts).addScoreRightSequence() //
				.addBallInCornerSequence()));
	}

	@Provide
	Arbitrary<List<RelativePosition>> idle() {
		return anyTimestamp(ts -> {
			return a(gameSituation(ts) //
					.addIdleSequence());
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> idleWhereBallMaybeGone() {
		return anyTimestamp(ts -> a(gameSituation(ts).addIdleSequenceBallMaybeGone()));
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
		private static final double CORNER_DRIFT = 0.10;

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
			private Integer minSize, maxSize;
			private boolean unique;

			private Sizeable(Arbitrary<RelativePosition> arbitrary) {
				this.arbitrary = arbitrary;
			}

			private Sizeable elementsMin(int minSize) {
				this.minSize = minSize;
				return this;
			}

			private Sizeable between(int minSize, int maxSize) {
				this.minSize = minSize;
				this.maxSize = maxSize;
				return this;
			}

			private GameSituationBuilder addSequence() {
				SizableArbitrary<List<RelativePosition>> list = arbitrary.list();
				list = minSize == null ? list : list.ofMinSize(minSize);
				Arbitrary<List<RelativePosition>> seq = maxSize == null ? list : list.ofMaxSize(maxSize);
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
		private final List<Arbitrary<List<RelativePosition>>> arbitraries = new ArrayList<>();
		private Arbitrary<Long> samplingFrequency = longs().between(5, 1000);

		GameSituationBuilder(AtomicLong timestamp) {
			this.timestamp = timestamp;
		}

		GameSituationBuilder withSamplingFrequency(Arbitrary<Long> samplingFrequency, TimeUnit timeUnit) {
			this.samplingFrequency = samplingFrequency.map(timeUnit::toMillis);
			return this;
		}

		Arbitrary<List<RelativePosition>> build() {
			return join(arbitraries);
		}

		static Arbitrary<List<RelativePosition>> join(List<Arbitrary<List<RelativePosition>>> arbitraries) {
			return combine(arbitraries).as(p -> p.stream().flatMap(Collection::stream).collect(toList()));
		}

		static GameSituationBuilder gameSituation(AtomicLong timestamp) {
			return new GameSituationBuilder(timestamp);
		}

		static boolean isCorner(RelativePosition pos) {
			RelativePosition normalized = pos.normalizeX().normalizeY();
			return normalized.getX() >= (TABLE_MAX - CORNER_DRIFT) //
					&& normalized.getY() >= (TABLE_MAX - CORNER_DRIFT);
		}

		Sizeable anywhereOnTableSizeable() {
			return asSizeable(combine(samplingFrequency, wholeTable(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y)));
		}

		Sizeable asSizeable(Arbitrary<RelativePosition> as) {
			return new Sizeable(as);
		}

		GameSituationBuilder addScoreLeftSequence() {
			return addSequence(prepareLeftGoal(timestamp)).offTablePositions(timestamp);
		}

		GameSituationBuilder addScoreRightSequence() {
			return addSequence(prepareRightGoal(timestamp)).offTablePositions(timestamp);
		}

		GameSituationBuilder offTablePositions(AtomicLong timestamp) {
			return offTableSequence().ofDuration(longs().between(2, 15), SECONDS).add(timestamp);
		}

		GameSituationBuilder addBallInCornerSequence() {
			return addSequence(corner(timestamp)).anywhereOnTableSizeable().addSequence();
		}

		GameSituationBuilder addBallNotInCornerSequence() {
			return anywhereOnTableSizeable().filter(p -> !isCorner(p)).between(0, 50).addSequence();
		}

		GameSituationBuilder addIdleSequence() {
			return idleSequence(noMoveForAtLeast(1, MINUTES));
		}

		GameSituationBuilder addIdleSequenceBallMaybeGone() {
			return idleSequence(noMoveOrNoBallForAtLeast(1, MINUTES));
		}

		GameSituationBuilder idleSequence(Arbitrary<List<RelativePosition>> arbitrary) {
			// add at least two unique elements to ensure idle is over afterwards
			return anywhereOnTableSizeable().addSequence() //
					.addSequence(arbitrary).anywhereOnTableSizeable().elementsMin(2).unique().addSequence();
		}

		GameSituationBuilder addSequence(Arbitrary<List<RelativePosition>> arbitrary) {
			arbitraries.add(arbitrary);
			return this;
		}

		Sequence aKickoffSequence() {
			return new Sequence(middleLinePositions(timestamp));
		}

		Arbitrary<RelativePosition> middleLinePositions(AtomicLong timestamp) {
			return combine(samplingFrequency, middleLine(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		Sequence offTableSequence() {
			return new Sequence(samplingFrequency.map(millis -> noPosition(timestamp.addAndGet(millis))));
		}

		Arbitrary<List<RelativePosition>> prepareLeftGoal(AtomicLong timestamp) {
			return frontOfLeftGoal(timestamp).list().ofMinSize(1);
		}

		Arbitrary<List<RelativePosition>> prepareRightGoal(AtomicLong timestamp) {
			return frontOfRightGoal(timestamp).list().ofMinSize(1);
		}

		Arbitrary<RelativePosition> frontOfLeftGoal(AtomicLong timestamp) {
			return combine(samplingFrequency, frontOfLeftGoal(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		Arbitrary<RelativePosition> frontOfRightGoal(AtomicLong timestamp) {
			return combine(samplingFrequency, frontOfRightGoal(), wholeTable()) //
					.as((millis, x, y) //
					-> create(timestamp.addAndGet(millis), x, y));
		}

		Arbitrary<List<RelativePosition>> corner(AtomicLong timestamp) {
			return combine(samplingFrequency, corner(), corner(), bool(), bool()) //
					.as((millis, x, y, swapX, swapY) //
					-> create(timestamp.addAndGet(millis), possiblySwap(x, swapX), possiblySwap(y, swapY))).list()
					.ofMinSize(1);
		}

		Arbitrary<List<RelativePosition>> noMoveOrNoBallForAtLeast(int duration, TimeUnit minutes) {
			return frequency( //
					Tuple.of(90, noBallForAtLeast(duration, minutes)), //
					Tuple.of(90, noMoveForAtLeast(duration, minutes))).flatMap(identity() //
			);
		}

		Arbitrary<List<RelativePosition>> noMoveForAtLeast(int duration, TimeUnit minutes) {
			// TODO depend on duration
			IntegerArbitrary amount = integers().between(100, 1_000);
			Arbitrary<Long> xxxx = longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10));
			return combine(xxxx, amount, wholeTable(), wholeTable()) //
					.as((millis, count, x, y) //
					-> range(0, count).mapToObj(ignore -> create(timestamp.addAndGet(millis), x, y)).collect(toList()));
		}

		Arbitrary<List<RelativePosition>> noBallForAtLeast(int duration, TimeUnit minutes) {
			// TODO depend on duration
			IntegerArbitrary amount = integers().between(100, 1_000);
			Arbitrary<Long> xxxx = longs().between(SECONDS.toMillis(1), SECONDS.toMillis(10));
			return combine(xxxx, amount) //
					.as((millis, count) //
					-> range(0, count).mapToObj(ignore -> noPosition(timestamp.addAndGet(millis))).collect(toList()));
		}

		static double possiblySwap(double value, boolean swap) {
			return swap ? swap(value) : value;
		}

		static double swap(double value) {
			return TABLE_MAX - value;
		}

		static Arbitrary<Double> corner() {
			return doubles().between(TABLE_MAX - CORNER_DRIFT, TABLE_MAX);
		}

		static DoubleArbitrary wholeTable() {
			return doubles().between(TABLE_MIN, TABLE_MAX);
		}

		static DoubleArbitrary middleLine() {
			return doubles().between(CENTER - MIDDLE_LINE_DRIFT, CENTER + MIDDLE_LINE_DRIFT);
		}

		static Arbitrary<Double> frontOfLeftGoal() {
			return frontOfGoal();
		}

		static Arbitrary<Double> frontOfRightGoal() {
			return frontOfGoal().map(GameSituationBuilder::swap);
		}

		static Arbitrary<Double> frontOfGoal() {
			return doubles().between(0, FRONT_OF_GOAL_DRIFT);
		}

		static Arbitrary<Boolean> bool() {
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

	static class PositionSequenceBuilder {

		public static final long DEFAULT_DURATION = SECONDS.toMillis(1);

		private final Arbitrary<Function<Long, RelativePosition>> positionCreatorArbitrary;
		private Arbitrary<Long> durationArbitrary = Arbitraries.constant(DEFAULT_DURATION);

		public PositionSequenceBuilder(Arbitrary<Function<Long, RelativePosition>> positionCreatorArbitrary) {
			this.positionCreatorArbitrary = positionCreatorArbitrary;
		}

		public PositionSequenceBuilder forDuration(long duration, TimeUnit timeUnit) {
			return forDuration(Arbitraries.constant(duration), timeUnit);
		}

		public PositionSequenceBuilder forDuration(Arbitrary<Long> durationArbitrary, TimeUnit timeUnit) {
			this.durationArbitrary = durationArbitrary.map(timeUnit::toMillis);
			return this;
		}

		public Arbitrary<List<Tuple2<Long, Function<Long, RelativePosition>>>> build(
				Arbitrary<Long> frequencyArbitrary) {
			Arbitrary<List<Long>> timestamps = durationArbitrary
					.flatMap(durationMillis -> arbitraryCollect(frequencyArbitrary,
							base -> durationReached(base, durationMillis)));

			return timestamps.flatMap(stamps -> {
				// the list of position creators must have the same length
				Arbitrary<List<Function<Long, RelativePosition>>> positionCreators = positionCreatorArbitrary.list()
						.ofSize(stamps.size());
				return positionCreators.map(creators -> zipLists(stamps, creators));
			});

		}

		private boolean durationReached(List<Long> timestamps, long minDuration) {
			return !timestamps.isEmpty() && duration(timestamps) >= minDuration;
		}

		private long duration(List<Long> timestamps) {
			long lastTimestamp = timestamps.get(timestamps.size() - 1);
			return timestamps.stream().mapToLong(l -> l).sum() - lastTimestamp;
		}

		static <T, U> List<Tuple2<T, U>> zipLists(List<T> stamps, List<U> creators) {
			return IntStream.range(0, stamps.size()).mapToObj(i -> Tuple.of(stamps.get(i), creators.get(i)))
					.collect(toList());
		}

		static <T> Arbitrary<List<T>> arbitraryCollect(Arbitrary<T> elementArbitrary, Predicate<List<T>> until) {
			return new ArbitraryCollect<>(elementArbitrary, until);
		}

	}

}
