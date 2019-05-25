package detection;

import java.util.function.Predicate;

import detection.data.Message;

public enum Topic {

	BALL_POSITION_ABS(topicStartsWith("ball/position/abs")), //
	BALL_POSITION_REL(topicStartsWith("ball/position/rel")), //
	BALL_DISTANCE_CM(topicStartsWith("ball/distance/cm")), //
	BALL_OVERALL_DISTANCE_CM(topicStartsWith("ball/distance/overall/cm")), //
	BALL_VELOCITY_KMH(topicStartsWith("ball/velocity/kmh")), //
	BALL_VELOCITY_MPS(topicStartsWith("ball/velocity/mps")), //
	GAME_START(topicStartsWith("game/start")), //
	GAME_FOUL(topicStartsWith("game/foul")), //
	GAME_IDLE(topicStartsWith("game/idle")), //
	TEAM_SCORE_LEFT(topicStartsWith("team/score/" + Topic.TEAM_ID_LEFT)), //
	TEAM_SCORE_RIGHT(topicStartsWith("team/score/" + Topic.TEAM_ID_RIGHT)), //
	TEAM_SCORED(topicStartsWith("team/scored")); //

	public static final String TEAM_ID_LEFT = "0";
	public static final String TEAM_ID_RIGHT = "1";

	private final Predicate<Message> predicate;

	static Predicate<Message> topicStartsWith(String topic) {
		return m -> m.getTopic().startsWith(topic);
	}

	Topic(Predicate<Message> predicate) {
		this.predicate = predicate;
	}

	Predicate<Message> getPredicate() {
		return predicate;
	}

}