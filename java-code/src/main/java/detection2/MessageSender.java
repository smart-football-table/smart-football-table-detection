package detection2;

import static detection2.data.Message.message;
import static detection2.data.unit.DistanceUnit.CENTIMETER;
import static detection2.data.unit.SpeedUnit.KMH;
import static detection2.data.unit.SpeedUnit.MPS;
import static java.util.stream.Collectors.joining;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection2.data.Message;
import detection2.data.Movement;
import detection2.data.position.AbsolutePosition;
import detection2.data.position.Position;

public class MessageSender {

	private final Consumer<Message> publisher;

	public MessageSender(Consumer<Message> publisher) {
		this.publisher = publisher;
	}

	public void gameStart() {
		publish(message("game/start", ""));
	}

	public void pos(AbsolutePosition p) {
		publish(message("ball/position/abs", posPayload(p)));
		publish(message("ball/position/rel", posPayload(p.getRelativePosition())));
	}

	private String posPayload(Position pos) {
		return "{ \"x\":" + pos.getX() + ", \"y\":" + pos.getY() + " }";
	}

	public void movement(Movement m) {
		publish(message("ball/distance/cm", m.distance(CENTIMETER)));
		publish(message("ball/velocity/mps", m.velocity(MPS)));
		publish(message("ball/velocity/kmh", m.velocity(KMH)));
	}

	public void teamScored(int teamid, int score) {
		publish(message("team/scored", teamid));
		publish(message("game/score/" + teamid, score));
	}

	public void foul() {
		publish(message("game/foul", ""));
	}

	public void gameWon(int teamid) {
		publish(message("game/gameover", teamid));
	}

	public void draw(int[] teamids) {
		publish(message("game/gameover", IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
	}

	public void idle(boolean b) {
		publish(message("game/idle", Boolean.toString(b)));
	}

	private void publish(Message message) {
		publisher.accept(message);
	}

}