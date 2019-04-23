package detection2.detector;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import detection2.data.position.AbsolutePosition;
import detection2.data.position.RelativePosition;

public class GoalDetector implements Detector {

	public static class Config {

		private long timeWithoutBallTilGoalMillisDuration = 2;
		private TimeUnit timeWithoutBallTilGoalMillisTimeUnit = SECONDS;
		private int frontOfGoalPercentage = 40;

		/**
		 * Sets where the ball has to been detected before the ball has been gone.
		 * 
		 * @param frontOfGoalPercentage 100% the whole playfield, 50% one side.
		 * @return this config
		 */
		public GoalDetector.Config frontOfGoalPercentage(int frontOfGoalPercentage) {
			this.frontOfGoalPercentage = frontOfGoalPercentage;
			return this;
		}

		public GoalDetector.Config timeWithoutBallTilGoal(long duration, TimeUnit timeUnit) {
			this.timeWithoutBallTilGoalMillisDuration = duration;
			this.timeWithoutBallTilGoalMillisTimeUnit = timeUnit;
			return this;
		}

		public int getFrontOfGoalPercentage() {
			return frontOfGoalPercentage;
		}

		public long getGoalTimeout(TimeUnit timeUnit) {
			return timeUnit.convert(timeWithoutBallTilGoalMillisDuration, timeWithoutBallTilGoalMillisTimeUnit);
		}

	}

	private final double frontOfGoal;
	private final long millisTilGoal;
	private GoalDetector.State state = new WaitForBallOnMiddleLine();
	private final GoalDetector.Listener listener;

	public static interface Listener {
		void goal(int teamid);

		void goalRevert(int teamid);
	}

	public GoalDetector(GoalDetector.Config config, GoalDetector.Listener listener) {
		this.frontOfGoal = 1 - ((double) config.getFrontOfGoalPercentage()) / 100;
		this.millisTilGoal = config.getGoalTimeout(MILLISECONDS);
		this.listener = listener;
	}

	private static interface State {
		GoalDetector.State update(AbsolutePosition pos);
	}

	private class WaitForBallAfterGoal implements GoalDetector.State {

		private final int teamid;

		public WaitForBallAfterGoal(int teamid) {
			this.teamid = teamid;
		}

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			if (pos.isNull()) {
				return this;
			}
			RelativePosition normalized = pos.getRelativePosition().normalizeX().normalizeY();
			if (normalized.getX() >= 0.99 && normalized.getY() >= 0.99) {
				return new RevertGoal(teamid);
			}
			return new WaitForBallOnMiddleLine().update(pos);
		}

	}

	private class RevertGoal implements GoalDetector.State {

		private final int teamid;

		public RevertGoal(int teamid) {
			this.teamid = teamid;
		}

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			return new BallOnTable().update(pos);
		}

		public int getTeamid() {
			return teamid;
		}

	}

	private class WaitForBallOnMiddleLine implements GoalDetector.State {

		private final double midAreasPercent = 5D;
		private final double midAreaMax = 0.5 + midAreasPercent / 100;

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			return ballAtMiddleLine(pos) ? new BallOnTable().update(pos) : this;
		}

		private boolean ballAtMiddleLine(AbsolutePosition pos) {
			return pos.getRelativePosition().normalizeX().getX() <= midAreaMax;
		}

	}

	private class BallOnTable implements GoalDetector.State {

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			RelativePosition relPos = pos.getRelativePosition();
			return isFrontOfGoal(relPos) ? new FrontOfGoal(relPos.isRightHandSide() ? 0 : 1) : this;
		}

		private boolean isFrontOfGoal(RelativePosition relPos) {
			return relPos.normalizeX().getX() >= frontOfGoal;
		}
	}

	private class FrontOfGoal implements GoalDetector.State {

		private final int teamid;

		public FrontOfGoal(int teamid) {
			this.teamid = teamid;
		}

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			return pos.isNull() ? //
					new PossibleGoal(pos.getTimestamp(), teamid).update(pos) : //
					new BallOnTable().update(pos);
		}
	}

	private class PossibleGoal implements GoalDetector.State {

		private final long timestamp;
		private final int teamid;

		public PossibleGoal(long timestamp, int teamid) {
			this.timestamp = timestamp;
			this.teamid = teamid;
		}

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			if (pos.isNull()) {
				return waitTimeElapsed(pos) ? new Goal(teamid) : this;
			}
			return new BallOnTable().update(pos);
		}

		private boolean waitTimeElapsed(AbsolutePosition pos) {
			return pos.getTimestamp() - timestamp >= millisTilGoal;
		}

	}

	private class Goal implements GoalDetector.State {

		private final int teamid;

		public Goal(int teamid) {
			this.teamid = teamid;
		}

		@Override
		public GoalDetector.State update(AbsolutePosition pos) {
			return new WaitForBallAfterGoal(teamid).update(pos);
		}

		public int getTeamid() {
			return teamid;
		}

	}

	@Override
	public void detect(AbsolutePosition pos) {
		state = state.update(pos);
		if (state instanceof Goal) {
			listener.goal(((Goal) state).getTeamid());
		} else if (state instanceof RevertGoal) {
			listener.goalRevert(((RevertGoal) state).getTeamid());
		}
	}

}