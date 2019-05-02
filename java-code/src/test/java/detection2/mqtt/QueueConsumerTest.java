package detection2.mqtt;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;

import detection2.queue.QueueConsumer;

public class QueueConsumerTest {

	@Test
	public void singleElementInQueueOfSize1() throws InterruptedException {
		List<String> strings = new ArrayList<>();
		queue(addTo(strings), 1).accept("test");
		TimeUnit.MILLISECONDS.sleep(50);
		assertThat(strings, is(asList("test")));
	}

	@Test
	public void whenBlockingWillAcceptAsManyElementsAsTheQueueHasSize() throws InterruptedException {
		int queueSize = 10;
		List<String> strings = new ArrayList<>();
		Consumer<String> queued = queue(sleep().andThen(addTo(strings)), queueSize);
		fillQueue(queued, queueSize);
		TimeUnit.MILLISECONDS.sleep(50);
		assertThat(strings, is(emptyList()));
	}

	@Test
	public void whenBlockingAndTheQueueIsFullNoMoreElementsAreAccepted() throws InterruptedException {
		int queueSize = 10;
		List<String> strings = new ArrayList<>();
		Consumer<String> queued = queue(sleep().andThen(addTo(strings)), queueSize);
		fillQueue(queued, queueSize);

		Thread backgroundAdder = new Thread(() -> queued.accept("adding-last-element"));
		backgroundAdder.start();
		TimeUnit.SECONDS.sleep(5);
		assertThat(backgroundAdder.getState(), is(Thread.State.WAITING));
		backgroundAdder.interrupt();
	}

	private void fillQueue(Consumer<String> queued, int queueSize) {
		IntStream.rangeClosed(0, queueSize).mapToObj(i -> "test" + i).forEach(queued::accept);
	}

	private Consumer<String> sleep() {
		return t -> {
			try {
				TimeUnit.HOURS.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
	}

	private Consumer<String> addTo(List<String> strings) {
		return strings::add;
	}

	private <T> Consumer<T> queue(Consumer<T> consumer, int queueSize) {
		return new QueueConsumer<T>(consumer, queueSize);
	}

}
