package detection2.main;

import java.io.IOException;

import detection2.data.Message;
import detection2.mqtt.MqttConsumer;
import detection2.queue.QueueConsumer;

public class Main {

	public static void main(String[] args) throws IOException {
		new Main(args);
	}

	public Main(String... args) throws IOException {
		MqttConsumer mqtt = mqtt("localhost", 1883);
		new OpenCVHandler(new QueueConsumer<Message>(mqtt, 300)) //
				.withReceiver(mqtt) //
				.withPythonArgs(args) //
				.startPythonModule();
	}

	private MqttConsumer mqtt(String host, int port) throws IOException {
		return new MqttConsumer(host, port);
	}

}
