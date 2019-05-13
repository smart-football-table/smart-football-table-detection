package detection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static detection.Topic.BALL_POSITION_ABS;
import static detection.Topic.BALL_POSITION_REL;
import static detection.Topic.BALL_VELOCITY_KMH;
import static detection.Topic.BALL_VELOCITY_MPS;
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
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static net.jqwik.api.Arbitraries.doubles;
import static net.jqwik.api.Arbitraries.integers;
import static net.jqwik.api.Arbitraries.longs;
import static net.jqwik.api.Combinators.combine;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import net.jqwik.api.arbitraries.DoubleArbitrary;
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
		assertThat(process(positions, table).filter(TEAM_SCORED).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_LEFT)));
	}

	@Property
	void leftGoalsProducesTeamScoreMessage(@ForAll("goalSituationsLeft") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(TEAM_SCORE_LEFT).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void rightGoalProducesTeamScoredMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(TEAM_SCORED).map(Message::getPayload).collect(toList()),
				is(asList(TEAM_ID_RIGHT)));
	}

	@Property
	void rightGoalProducesTeamScoreMessage(@ForAll("goalSituationsRight") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(TEAM_SCORE_RIGHT).map(Message::getPayload).collect(toList()),
				is(asList("1")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterALeftHandGoalTheGoalGetsRever(
			@ForAll("leftGoalsToReverse") List<RelativePosition> positions, @ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(TEAM_SCORE_LEFT).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
	}

	@Property
	void whenBallIsDetectedInAnyCornerAfterARightHandGoalTheGoalGetsReverted(
			@ForAll("rightGoalsToReverse") List<RelativePosition> positions, @ForAll("table") Table table) {
		// TODO remove when generator is fixed
		Assume.that(ballWasOffTableForAtLeast(positions, 2, SECONDS));
		statistics(positions);
		assertThat(process(positions, table).filter(TEAM_SCORE_RIGHT).map(Message::getPayload).collect(toList()),
				is(asList("1", "0")));
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
	void ballPositionRelForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_POSITION_REL).collect(toList()), hasSize(positions.size()));
	}

	@Property
	void allRelPositionAreBetween0And1(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_POSITION_REL).map(Message::getPayload).collect(toList()),
				everyItem( //
						allOf(hasNumberBetween("x", 0, 1), hasNumberBetween("y", 0, 1))));
	}

	@Property
	void ballPositionAbsForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_POSITION_ABS).collect(toList()), hasSize(positions.size()));
	}

	@Property
	void allAbsPositionAreBetween0AndTableSize(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_POSITION_ABS).map(Message::getPayload).collect(toList()),
				everyItem( //
						allOf(hasNumberBetween("x", 0, table.getWidth()),
								hasNumberBetween("y", 0, table.getHeight()))));
	}

	@Property
	void ballVelocityKmhForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_VELOCITY_KMH).collect(toList()),
				hasSize(positions.size() - 1));
	}

	@Property
	void allBallPositionVelocitiesArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_VELOCITY_KMH).map(Message::getPayload).map(Double::parseDouble)
				.collect(toList()), everyItem(is(positive())));
	}

	@Property
	void ballVelocityMpsForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_VELOCITY_MPS).collect(toList()),
				hasSize(positions.size() - 1));
	}

	@Property
	void ballVelocityMpsForArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table).filter(BALL_VELOCITY_MPS).map(Message::getPayload).map(Double::parseDouble)
				.collect(toList()), everyItem(is(positive())));
	}

	private void statistics(Collection<?> col) {
		Statistics.collect(col.size() < 10 ? "<10" : col.size() < 30 ? "<30" : ">=30");
	}

	private Matcher<Object> hasNumberBetween(String name, int min, int max) {
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
		return longs().map(AtomicLong::new).flatMap(ts -> anywhereOnTable(ts).list().ofMinSize(2));
	}

	@Provide
	Arbitrary<List<RelativePosition>> goalSituationsLeft() {
		return longs().map(AtomicLong::new).flatMap(ts -> {
			return join(asList( //
					kickoffPositions(ts), //
					anywhereOnTable(ts).list(), //
					prepareLeftGoal(ts), //
					offTablePositions(ts), //
					notInCorner(ts) //
			));
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> goalSituationsRight() {
		return longs().map(AtomicLong::new).flatMap(ts -> {
			return join(asList( //
					kickoffPositions(ts), //
					anywhereOnTable(ts).list(), //
					prepareRightGoal(ts), //
					offTablePositions(ts), //
					notInCorner(ts) //
			));
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> leftGoalsToReverse() {
		return longs().map(AtomicLong::new).flatMap(ts -> {
			return join(asList( //
					kickoffPositions(ts), //
					anywhereOnTable(ts).list(), //
					prepareLeftGoal(ts), //
					offTablePositions(ts), //
					corner(ts) //
			));
		});
	}

	@Provide
	Arbitrary<List<RelativePosition>> rightGoalsToReverse() {
		return longs().map(AtomicLong::new).flatMap(ts -> {
			return join(asList( //
					kickoffPositions(ts), //
					anywhereOnTable(ts).list(), //
					prepareRightGoal(ts), //
					offTablePositions(ts), //
					corner(ts) //
			));
		});
	}

	private Arbitrary<List<RelativePosition>> notInCorner(AtomicLong ts) {
		return combine(diffInMillis(), wholeTable(), wholeTable()) //
				.as((millis, x, y) //
				-> create(ts.addAndGet(millis), x, y)).list().ofMinSize(1)
				.filter(c -> c.isEmpty() || !isCorner(c.get(0)));
	}

	private Arbitrary<List<RelativePosition>> corner(AtomicLong ts) {
		return combine(diffInMillis(), corner(), corner(), bool(), bool()) //
				.as((millis, x, y, swapX, swapY) //
				-> create(ts.addAndGet(millis), swapX ? 1.0 - x : x, swapY ? 1.0 - y : y)).list().ofMinSize(1);
	}

	private Arbitrary<Boolean> bool() {
		return Arbitraries.integers().between(0, 1).map(i -> i == 0);
	}

	private Arbitrary<Double> corner() {
		return doubles().between(0.99, 1);
	}

	private boolean isCorner(RelativePosition pos) {
		return 0.5 + abs(0.5 - pos.getX()) >= 0.99 && 0.5 + abs(0.5 - pos.getY()) >= 0.99;
	}

	private SizableArbitrary<List<RelativePosition>> kickoffPositions(AtomicLong ts) {
		return atMiddleLine(ts).list().ofMinSize(1);
	}

	private SizableArbitrary<List<RelativePosition>> prepareLeftGoal(AtomicLong ts) {
		return frontOfLeftGoal(ts).list().ofMinSize(1);
	}

	private SizableArbitrary<List<RelativePosition>> prepareRightGoal(AtomicLong ts) {
		return frontOfRightGoal(ts).list().ofMinSize(1);
	}

	private Arbitrary<List<RelativePosition>> offTablePositions(AtomicLong timestamp) {
		// TODO create as many positions as needed (2000ms between first and last)
		return offTablePosition(timestamp).list().ofSize(10);
	}

	private static Arbitrary<RelativePosition> anywhereOnTable(AtomicLong timestamp) {
		return combine(diffInMillis(), wholeTable(), wholeTable()) //
				.as((millis, x, y) //
				-> create(timestamp.addAndGet(millis), x, y));
	}

	private static Arbitrary<RelativePosition> offTablePosition(AtomicLong timestamp) {
		return diffInMillis().map(millis -> noPosition(timestamp.addAndGet(millis)));
	}

	private static Arbitrary<RelativePosition> atMiddleLine(AtomicLong timestamp) {
		return combine(diffInMillis(), middleLine(), wholeTable()) //
				.as((millis, x, y) //
				-> create(timestamp.addAndGet(millis), x, y));
	}

	private static Arbitrary<RelativePosition> frontOfLeftGoal(AtomicLong timestamp) {
		return combine(diffInMillis(), frontOfLeftGoal(), wholeTable()) //
				.as((millis, x, y) //
				-> create(timestamp.addAndGet(millis), x, y));
	}

	private static Arbitrary<RelativePosition> frontOfRightGoal(AtomicLong timestamp) {
		return combine(diffInMillis(), frontOfRightGoal(), wholeTable()) //
				.as((millis, x, y) //
				-> create(timestamp.addAndGet(millis), x, y));
	}

	private static LongArbitrary diffInMillis() {
		return longs().between(MILLISECONDS.toMillis(1), SECONDS.toMillis(10));
	}

	private static DoubleArbitrary wholeTable() {
		return doubles().between(0, 1);
	}

	private static DoubleArbitrary middleLine() {
		return doubles().between(0.45, 0.55);
	}

	private static DoubleArbitrary frontOfLeftGoal() {
		return doubles().between(0, 0.3);
	}

	private static DoubleArbitrary frontOfRightGoal() {
		return doubles().between(0.7, 1);
	}

	private static Arbitrary<List<RelativePosition>> join(List<Arbitrary<List<RelativePosition>>> arbitraries) {
		return combine(arbitraries).as(p -> p.stream().flatMap(Collection::stream).collect(toList()));
	}

}
