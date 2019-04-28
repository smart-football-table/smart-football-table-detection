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

public final class Messages {

	private Messages() {
		super();
	}

	public static final class OutgoingMessages {

		private OutgoingMessages() {
			super();
		}

		public static void publishGameStart(Consumer<Message> p) {
			publish(p, message("game/start", ""));
		}

		public static void publishPos(Consumer<Message> p, AbsolutePosition pos) {
			publish(p, message("ball/position/abs", posPayload(pos)));
			publish(p, message("ball/position/rel", posPayload(pos.getRelativePosition())));
		}

		private static String posPayload(Position pos) {
			return "{ \"x\":" + pos.getX() + ", \"y\":" + pos.getY() + " }";
		}

		public static void publishMovement(Consumer<Message> p, Movement m) {
			publish(p, message("ball/distance/cm", m.distance(CENTIMETER)));
			publish(p, message("ball/velocity/mps", m.velocity(MPS)));
			publish(p, message("ball/velocity/kmh", m.velocity(KMH)));
		}

		public static void publishTeamScored(Consumer<Message> p, int teamid, int score) {
			publish(p, message("team/scored", teamid));
			publish(p, message("team/score/" + teamid, score));
			publish(p, message("game/score/" + teamid, score)); // deprecated
		}

		public static void publishFoul(Consumer<Message> p) {
			publish(p, message("game/foul", ""));
		}

		public static void publihGameWon(Consumer<Message> p, int teamid) {
			publish(p, message("game/gameover", teamid));
		}

		public static void publishGameDraw(Consumer<Message> p, int[] teamids) {
			publish(p, message("game/gameover", IntStream.of(teamids).mapToObj(String::valueOf).collect(joining(","))));
		}

		public static void pusblishIdle(Consumer<Message> p, boolean b) {
			publish(p, message("game/idle", Boolean.toString(b)));
		}

		private static void publish(Consumer<Message> p, Message message) {
			p.accept(message);
		}
	}

	public static final class IncomingMessages {

		private IncomingMessages() {
			super();
		}

		public static boolean isReset(Message message) {
			return message.getTopic().equals("game/reset");
		}
	}

}