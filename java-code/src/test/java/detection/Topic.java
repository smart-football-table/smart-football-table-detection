package detection;

import java.util.function.Predicate;

import detection.data.Message;

public enum Topic {

	BALL_POSITION_ABS(DetectionExamples.topicStartsWith("ball/position/abs")), //
	BALL_POSITION_REL(DetectionExamples.topicStartsWith("ball/position/rel")), //
	BALL_DISTANCE_CM(DetectionExamples.topicStartsWith("ball/distance/cm")), //
	BALL_VELOCITY_KMH(DetectionExamples.topicStartsWith("ball/velocity/kmh")), //
	BALL_VELOCITY_MPS(DetectionExamples.topicStartsWith("ball/velocity/mps")), //
	GAME_START(DetectionExamples.topicStartsWith("game/start")), //
	GAME_FOUL(DetectionExamples.topicStartsWith("game/foul")), //
	GAME_IDLE(DetectionExamples.topicStartsWith("game/idle")), //
	TEAM_SCORE_LEFT(DetectionExamples.topicStartsWith("team/score/" + Topic.TEAM_ID_LEFT)), //
	TEAM_SCORE_RIGHT(DetectionExamples.topicStartsWith("team/score/" + Topic.TEAM_ID_RIGHT)), //
	TEAM_SCORED(DetectionExamples.topicStartsWith("team/scored")); //

	public static final String TEAM_ID_LEFT = "0";
	public static final String TEAM_ID_RIGHT = "1";

	private final Predicate<Message> predicate;

	Topic(Predicate<Message> predicate) {
		this.predicate = predicate;
	}

	Predicate<Message> getPredicate() {
		return predicate;
	}

}