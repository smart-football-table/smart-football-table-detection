package fiduciagad.de.sft.goaldetector;

import java.util.List;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.ConfiguratorValues;

public class GoalDetector {

	private boolean ballWasNotInFrontOfAGoal(BallPosition ballPosition_tMinus1) {

		boolean ballWasNotInFrontOfAGoal;

		double twentyPercentOfXMaxOfGameField = ConfiguratorValues.getXMaxOfGameField() * 0.2;
		double xStartOfAreaWhereBallIsntBeforeAGoal = twentyPercentOfXMaxOfGameField;
		double xEndOfAreaWhereBallIsntBeforeAGoal = ConfiguratorValues.getXMaxOfGameField()
				- twentyPercentOfXMaxOfGameField;

		ballWasNotInFrontOfAGoal = xStartOfAreaWhereBallIsntBeforeAGoal < ballPosition_tMinus1.getXCoordinate()
				&& ballPosition_tMinus1.getXCoordinate() < xEndOfAreaWhereBallIsntBeforeAGoal;

		boolean ballWasNotInField = noPositionDetected(ballPosition_tMinus1);

		return ballWasNotInFrontOfAGoal || ballWasNotInField;
	}

	public String whereHappendTheGoal(BallPosition ballPosition_tMinus1, BallPosition ballPosition_tMinus2) {

		boolean ballMovedLeft = ballPosition_tMinus2.getXCoordinate() > ballPosition_tMinus1.getXCoordinate();
		if (ballMovedLeft) {
			return "on the left";
		} else {
			return "on the right";
		}

	}

	public boolean isThereAGoal(List<BallPosition> ballPositions) {

		boolean thereIsAGoal = false;
		int counter = 0;

		for (int i = 0; i < 50; i++) {

			// iterate over the last 50 positions
			BallPosition position = ballPositions.get((ballPositions.size() - 50) + i);

			if (noPositionDetected(position)) {
				counter++;
			} else {
				counter = 0;
			}

			if (counter == 50) {

				BallPosition lastBallPositionOnField = ballPositions.get(ballPositions.size() - 51);
				if (ballWasNotInFrontOfAGoal(lastBallPositionOnField)) {
					thereIsAGoal = false;
				} else {
					thereIsAGoal = true;
				}
			}

		}

		return thereIsAGoal;
	}

	private boolean noPositionDetected(BallPosition position) {
		return position.getXCoordinate() == -1 && position.getYCoordinate() == -1;
	}

}
