package detection;

import static detection.data.Message.message;
import static detection.data.unit.DistanceUnit.CENTIMETER;
import static detection.data.unit.SpeedUnit.KMH;
import static detection.data.unit.SpeedUnit.MPS;
import static java.util.stream.Collectors.joining;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection.data.Message;
import detection.data.Movement;
import detection.data.position.AbsolutePosition;
import detection.data.position.Position;

public class Messages {

	private final Consumer<Message> consumer;

	public Messages(Consumer<Message> consumer) {
		this.consumer = consumer;
	}

	public void gameStart() {
		publish(message("game/start", ""));
	}

	public void pos(AbsolutePosition pos) {
		publish(message("ball/position/abs", posPayload(pos)));
		publish(message("ball/position/rel", posPayload(pos.getRelativePosition())));
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
		publish(message("team/score/" + teamid, score));
		publish(message("game/score/" + teamid, score)); // deprecated
	}

	public void foul() {
		publish(message("game/foul", ""));
	}

	public void gameWon(int teamid) {
		publish(message("game/gameover", teamid));
	}

	public void gameDraw(int[] teamids) {
		publish(message("game/gameover", IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
	}

	public void idle(boolean b) {
		publish(message("game/idle", Boolean.toString(b)));
	}

	private void publish(Message message) {
		consumer.accept(message);
	}

	public boolean isReset(Message message) {
		return message.getTopic().equals("game/reset");
	}

}