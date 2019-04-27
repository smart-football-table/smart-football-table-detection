package detection2.main;

import java.io.IOException;
import java.util.function.Consumer;

import detection2.data.Message;
import detection2.mqtt.MqttConsumer;

public class Main {

	public Main() throws IOException {
		new OpenCVHandler(mqtt("localhost", 1883)).startPythonModule();
	}

	private Consumer<Message> mqtt(String host, int port) throws IOException {
		return new MqttConsumer(host, port);
	}

}
