package detection2;

import java.util.function.Consumer;

import detection2.data.Message;

public interface MessageProvider {
	void addConsumer(Consumer<Message> consumer);
}