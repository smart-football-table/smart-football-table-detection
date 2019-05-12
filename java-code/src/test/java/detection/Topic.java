package detection;

import java.util.function.Predicate;

import detection.data.Message;

public enum Topic implements Predicate<Message> {

	BALL_POSITION_ABS(DetectionExamples.topicStartsWith("ball/position/abs")), //
	BALL_POSITION_REL(DetectionExamples.topicStartsWith("ball/position/rel")), //
	BALL_DISTANCE_CM(DetectionExamples.topicStartsWith("ball/distance/cm")), //
	BALL_VELOCITY_KMH(DetectionExamples.topicStartsWith("ball/velocity/kmh")), //
	BALL_VELOCITY_MPS(DetectionExamples.topicStartsWith("ball/velocity/mps")), //
	GAME_START(DetectionExamples.topicStartsWith("game/start")), //
	GAME_FOUL(DetectionExamples.topicStartsWith("game/foul")), //
	GAME_IDLE(DetectionExamples.topicStartsWith("game/idle")), //
	TEAM_SCORE_LEFT(DetectionExamples.topicStartsWith("team/score/0")), //
	TEAM_SCORE_RIGHT(DetectionExamples.topicStartsWith("team/score/1")), //
	TEAM_SCORED(DetectionExamples.topicStartsWith("team/scored")); //

	private final Predicate<Message> predicate;

	Topic(Predicate<Message> predicate) {
		this.predicate = predicate;
	}

	Predicate<Message> getPredicate() {
		return predicate;
	}

	@Override
	public boolean test(Message message) {
		return predicate.test(message);
	}

}