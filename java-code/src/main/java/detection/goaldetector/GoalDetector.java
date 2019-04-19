package detection.goaldetector;

import java.util.List;

import detection.main.BallPosition;
import detection.main.ConfiguratorValues;

public class GoalDetector {

	public boolean isBallWasInMidArea() {
		return ballWasInMidArea;
	}

	private boolean ballWasInMidArea;

	private boolean ballWasNotInFrontOfAGoal(BallPosition ballPosition_tMinus1) {

		boolean ballWasNotInFrontOfAGoal;

		double fourtyPercentOfXMaxOfGameFiel = ConfiguratorValues.getXMaxOfGameField() * 0.4;
		double xStartOfAreaWhereBallIsntBeforeAGoal = fourtyPercentOfXMaxOfGameFiel;
		double xEndOfAreaWhereBallIsntBeforeAGoal = ConfiguratorValues.getXMaxOfGameField()
				- fourtyPercentOfXMaxOfGameFiel;

		ballWasNotInFrontOfAGoal = xStartOfAreaWhereBallIsntBeforeAGoal < ballPosition_tMinus1.getXCoordinate()
				&& ballPosition_tMinus1.getXCoordinate() < xEndOfAreaWhereBallIsntBeforeAGoal;

		boolean ballWasNotInField = noPositionDetected(ballPosition_tMinus1);

		return ballWasNotInFrontOfAGoal || ballWasNotInField;
	}

	public String whereHappendTheGoal(BallPosition ballPosition_tMinus1, BallPosition ballPosition_tMinus2) {

		// boolean ballMovedLeft = ballPosition_tMinus2.getXCoordinate() >
		// ballPosition_tMinus1.getXCoordinate();

		boolean ballIsOnRightSite = ballPosition_tMinus1.getXCoordinate() < ConfiguratorValues.getXMaxOfGameField()
				* 0.5;

		if (ballIsOnRightSite) {
			return "on the left";
		} else {
			return "on the right";
		}

	}

	public boolean isThereAGoal(List<BallPosition> ballPositions) {

		boolean thereIsAGoal = false;
		int counter = 0;
		int countForLastTwoSeconds = ConfiguratorValues.getFramesPerSecond() * 2;

		for (int i = 0; i < countForLastTwoSeconds; i++) {

			// iterate over the last two seconds
			BallPosition position = ballPositions.get((ballPositions.size() - countForLastTwoSeconds) + i);

			if (noPositionDetected(position)) {
				counter++;
			} else {
				counter = 0;
			}

			if (counter == countForLastTwoSeconds) {

				BallPosition lastBallPositionOnField = ballPositions
						.get(ballPositions.size() - (countForLastTwoSeconds + 1));
				if (!ballWasNotInFrontOfAGoal(lastBallPositionOnField) && ballWasInMidArea) {
					thereIsAGoal = true;
					setBallWasInMidArea(false);
				}
			}

		}

		return thereIsAGoal;
	}

	private boolean noPositionDetected(BallPosition position) {
		return position.getXCoordinate() == -1 && position.getYCoordinate() == -1;
	}

	public void setBallWasInMidArea(boolean b) {
		this.ballWasInMidArea = b;

	}

}
