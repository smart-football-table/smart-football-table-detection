package detection2.data;

public class Message {

	private final String topic;
	private final String payload;

	public Message(String topic, Object payload) {
		this.topic = topic;
		this.payload = payload == null ? null : String.valueOf(payload);
	}

	public String getPayload() {
		return payload;
	}

	public String getTopic() {
		return topic;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (payload == null) {
			if (other.payload != null)
				return false;
		} else if (!payload.equals(other.payload))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Message [topic=" + topic + ", payload=" + payload + "]";
	}

}