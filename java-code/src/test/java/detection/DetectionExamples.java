package detection;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static detection.data.position.RelativePosition.create;
import static java.lang.Double.parseDouble;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static net.jqwik.api.Arbitraries.doubles;
import static net.jqwik.api.Arbitraries.integers;
import static net.jqwik.api.Arbitraries.longs;
import static net.jqwik.api.Combinators.combine;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hamcrest.Matcher;

import detection.data.Message;
import detection.data.Table;
import detection.data.position.AbsolutePosition;
import detection.data.position.Position;
import detection.data.position.RelativePosition;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Statistics;

class DetectionExamples {

	private static final Predicate<Message> ballPositionAbs = topicStartsWith("ball/position/abs");
	private static final Predicate<Message> ballPositionRel = topicStartsWith("ball/position/rel");
	private static final Predicate<Message> ballDistanceCm = topicStartsWith("ball/distance/cm");
	private static final Predicate<Message> ballVelocityKmh = topicStartsWith("ball/velocity/kmh");
	private static final Predicate<Message> ballVelocityMps = topicStartsWith("ball/velocity/mps");
	private static final Predicate<Message> gameStart = topicStartsWith("game/start");
	private static final Predicate<Message> gameFoul = topicStartsWith("game/foul");
	private static final Predicate<Message> gameIdle = topicStartsWith("game/idle");

	private static final List<Predicate<Message>> topics = asList(ballPositionAbs, ballPositionRel, ballDistanceCm,
			ballVelocityKmh, ballVelocityMps, gameStart, gameFoul, gameIdle);

	@Property
	void ballsOnTableNeverWillRaiseEventsOtherThan(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, anyOf(topics)), is(empty()));
	}

	@Property
	void ballPositionRelForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballPositionRel), hasSize(positions.size()));
	}

	@Property
	void ballPositionRelIsJson(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballPositionRel).stream().map(Message::getPayload).collect(toList()),
				everyItem(isJson()));
	}

	@Property
	void ballPositionAbsForEveryPosition(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballPositionAbs), hasSize(positions.size()));
	}

	@Property
	void ballPositionAbsIsJson(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballPositionAbs).stream().map(Message::getPayload).collect(toList()),
				everyItem(isJson()));
	}

	@Property
	void ballVelocityKmhForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballVelocityKmh), hasSize(positions.size() - 1));
	}

	@Property
	void allBallPositionAbsArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballVelocityKmh).stream().map(Message::getPayload).map(Double::parseDouble)
				.collect(toList()), everyItem(is(positive())));
	}

	@Property
	void ballVelocityMpsForEveryPositionChange(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballVelocityMps), hasSize(positions.size() - 1));
	}

	@Property
	void ballVelocityMpsForArePositive(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		statistics(positions);
		assertThat(process(positions, table, ballVelocityMps).stream().map(Message::getPayload).map(Double::parseDouble)
				.collect(toList()), everyItem(is(positive())));
	}

	private void statistics(Collection<?> col) {
		Statistics.collect(col.size() < 10 ? "<10" : col.size() < 30 ? "<30" : ">=30");
	}

	private List<Message> process(List<RelativePosition> positions, Table table, Predicate<Message> predicate) {
		List<Message> messages = new ArrayList<>();
		new SFTDetection(table, messages::add).process(positions.stream());
		return messages.stream().filter(predicate).collect(toList());
	}

	private <T> Predicate<T> anyOf(Collection<Predicate<T>> predicates) {
		return predicates.stream().reduce(Predicate::or).map(Predicate::negate).orElse(m -> true);
	}

	private static Predicate<Message> topicStartsWith(String topic) {
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
		return longs().map(AtomicLong::new).flatMap( //
				base -> position(base).list().ofMinSize(2));
	}

	private Arbitrary<RelativePosition> position(AtomicLong base) {
		return combine( //
				longs().between(1, SECONDS.toMillis(10)), //
				doubles().between(0, 1), //
				doubles().between(0, 1)) //
						.as((timestamp, x, y) //
						-> create(base.addAndGet(timestamp), x, y));
	}

}
