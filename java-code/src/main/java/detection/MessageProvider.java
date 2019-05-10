package detection;

import java.util.function.Consumer;

import detection.data.Message;

public interface MessageProvider {
	void addConsumer(Consumer<Message> consumer);
}