package detection2;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static net.jqwik.api.Arbitraries.doubles;
import static net.jqwik.api.Arbitraries.integers;
import static net.jqwik.api.Arbitraries.longs;
import static net.jqwik.api.Combinators.combine;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import detection2.data.Message;
import detection2.data.Table;
import detection2.data.position.RelativePosition;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class DetectionExamples {

	@Property
	void ballsOnTableNeverWillRaiseAGoalEvent(@ForAll("positionsOnTable") List<RelativePosition> positions,
			@ForAll("table") Table table) {
		List<Message> messages = new ArrayList<>();
		new SFTDetection(table, messages::add).process(positions.stream());
		assertThat(messages.stream().filter(anyOf( //
				asList( //
						topicStartsWith("ball/position/"), //
						topicStartsWith("ball/distance/"), //
						topicStartsWith("ball/velocity/"), //
						topicStartsWith("game/start"), //
						topicStartsWith("game/foul"), //
						topicStartsWith("game/idle") //
				))).collect(toList()), is(emptyList()));
	}

	private <T> Predicate<T> anyOf(Collection<Predicate<T>> predicates) {
		return predicates.stream().reduce(Predicate::or).map(Predicate::negate).orElse(m -> true);
	}

	private Predicate<Message> topicStartsWith(String topic) {
		return m -> m.getTopic().startsWith(topic);
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
		AtomicLong now = new AtomicLong();
		return combine(//
				longs().between(1, SECONDS.toMillis(5)), //
				doubles().between(0, 1), //
				doubles().between(0, 1)) //
						.as((timestamp, x, y) //
						-> RelativePosition.create(now.addAndGet(timestamp), x, y)) //
						.list();
	}

}
