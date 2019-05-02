package detection2.queue;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class QueueConsumer<T> implements Consumer<T> {

	private final BlockingQueue<T> blockingQueue;

	public QueueConsumer(Consumer<T> delegate) {
		this(delegate, 100);
	}

	public QueueConsumer(Consumer<T> delegate, int queueSize) {
		this.blockingQueue = new LinkedBlockingDeque<>(queueSize);
		newFixedThreadPool(1).execute(() -> {
			while (true) {
				delegate.accept(take());
			}
		});

	}

	private T take() {
		try {
			return blockingQueue.take();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void accept(T s) {
		try {
			blockingQueue.put(s);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}