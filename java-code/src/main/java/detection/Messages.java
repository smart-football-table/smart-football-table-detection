package detection;

import static detection.data.Message.message;
import static detection.data.unit.SpeedUnit.KMH;
import static detection.data.unit.SpeedUnit.MPS;
import static java.util.stream.Collectors.joining;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import detection.data.Distance;
import detection.data.Message;
import detection.data.Movement;
import detection.data.position.AbsolutePosition;
import detection.data.position.Position;
import detection.data.unit.DistanceUnit;

public class Messages {

	private final Consumer<Message> consumer;
	private final DistanceUnit distanceUnit;

	public Messages(Consumer<Message> consumer, DistanceUnit distanceUnit) {
		this.consumer = consumer;
		this.distanceUnit = distanceUnit;
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

	public void movement(Movement movement, Distance overallDistance) {
		publish(message("ball/distance/" + distanceUnit.symbol(), movement.distance(distanceUnit)));
		publish(message("ball/velocity/mps", movement.velocity(MPS)));
		publish(message("ball/velocity/kmh", movement.velocity(KMH)));
		publish(message("ball/distance/overall/" + distanceUnit.symbol(), overallDistance.value(distanceUnit)));
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